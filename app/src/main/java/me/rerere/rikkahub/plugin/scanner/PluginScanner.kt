/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.plugin.scanner

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.security.SecurityAuditRepository
import me.rerere.rikkahub.plugin.model.PluginInfo
import me.rerere.rikkahub.plugin.model.PluginManifest
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * 插件扫描器
 * 负责扫描、导入和管理插件目录
 */
class PluginScanner(
    private val context: Context,
    private val auditRepo: SecurityAuditRepository? = null,
) {
    companion object {
        private const val TAG = "PluginScanner"
        const val PLUGINS_DIR = "Orangechat/plugins"
        const val MANIFEST_FILE = "manifest.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 获取插件根目录
     * 使用应用专属外部存储，避免 Android 11+ 的 EACCES 权限问题
     * 路径: /storage/emulated/0/Android/data/<pkg>/files/plugins/
     */
    val pluginsDir: File
        get() {
            val dir = context.getExternalFilesDir("plugins")
            dir?.mkdirs()
            return dir ?: File(context.filesDir, "plugins").apply { mkdirs() }
        }

    /**
     * 确保插件目录存在
     */
    fun ensurePluginsDir(): File = pluginsDir

    /**
     * 一次性迁移：将旧路径 /storage/emulated/0/Orangechat/plugins/ 下的插件
     * 复制到新路径。仅在首次发现旧路径有插件且新路径还没有时执行。
     */
    private fun migrateFromOldPathIfNeeded() {
        val oldDir = File("/storage/emulated/0/Orangechat/plugins")
        if (!oldDir.isDirectory) return

        val newDir = pluginsDir
        if (!newDir.isDirectory) {
            newDir.mkdirs()
        }

        val newHasContent = newDir.listFiles()?.any { it.isDirectory } == true
        if (newHasContent) return

        val migrationFlag = File(newDir, ".migrated_from_old")
        if (migrationFlag.exists()) return

        val plugins = oldDir.listFiles { file -> file.isDirectory } ?: return
        if (plugins.isEmpty()) return

        Log.i(TAG, "Migrating ${plugins.size} plugin(s) from old path: ${oldDir.absolutePath}")
        var migrated = 0

        plugins.forEach { pluginDir ->
            try {
                val targetDir = File(newDir, pluginDir.name)
                if (!targetDir.exists()) {
                    pluginDir.copyRecursively(targetDir, overwrite = false)
                    migrated++
                    Log.i(TAG, "Migrated plugin: ${pluginDir.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to migrate plugin ${pluginDir.name}", e)
            }
        }

        try {
            migrationFlag.writeText("migrated_at_${System.currentTimeMillis()}")
        } catch (_: Exception) {}

        Log.i(TAG, "Migration complete: $migrated plugins moved to ${newDir.absolutePath}")
    }

    /**
     * 扫描所有插件
     * 首次扫描时自动将旧路径的插件迁移到新路径
     */
    fun scanPlugins(): List<PluginInfo> {
        migrateFromOldPathIfNeeded()
        val dir = ensurePluginsDir()
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        return dir.listFiles { file -> file.isDirectory }
            ?.mapNotNull { pluginDir -> loadPluginInfo(pluginDir) }
            ?: emptyList()
    }

    /**
     * 加载单个插件信息
     */
    fun loadPluginInfo(pluginDir: File): PluginInfo? {
        val manifestFile = File(pluginDir, MANIFEST_FILE)
        if (!manifestFile.exists()) {
            return null
        }

        return try {
            val content = manifestFile.readText()
            val manifest = json.decodeFromString(PluginManifest.serializer(), content)

            // 完整性校验：若存在 .integrity 文件则验证，失败则禁用并标记错误
            val integrityFile = File(pluginDir, ".integrity")
            if (integrityFile.exists()) {
                val stored = integrityFile.readText().trim()
                val current = computePluginChecksum(pluginDir)
                if (stored != current) {
                    return PluginInfo(
                        manifest = manifest,
                        directory = pluginDir,
                        isEnabled = false,
                        loadError = "完整性校验失败：插件文件已被篡改或损坏"
                    )
                }
            }

            PluginInfo(
                manifest = manifest,
                directory = pluginDir,
                isEnabled = true // 默认启用
            )
        } catch (e: Exception) {
            // 解析失败，返回错误状态的插件
            PluginInfo(
                manifest = PluginManifest(
                    id = pluginDir.name,
                    name = pluginDir.name,
                    description = "加载失败: ${e.message}",
                    version = "error",
                    author = "unknown",
                    icon = "⚠️",
                    entry = "",
                    tools = emptyList(),
                    config = emptyList()
                ),
                directory = pluginDir,
                isEnabled = false,
                loadError = e.message
            )
        }
    }

    /**
     * 从ZIP文件预览插件（不解压到插件目录，仅解析 manifest）
     * 返回 manifest 和临时目录，供调用方展示权限确认对话框。
     * 调用方确认后应调用 [completeImport] 完成导入；取消时应清理临时目录。
     */
    suspend fun previewFromZip(uri: Uri): Result<Pair<PluginManifest, File>> {
        return try {
            val tempFile = File(context.cacheDir, "plugin_preview_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("无法读取文件"))

            val tempDir = File(context.cacheDir, "plugin_preview_${System.currentTimeMillis()}")
            unzip(tempFile, tempDir)

            val manifestFile = findManifest(tempDir)
                ?: run {
                    tempFile.delete()
                    tempDir.deleteRecursively()
                    return Result.failure(IllegalArgumentException("找不到 manifest.json"))
                }

            val manifest = json.decodeFromString(PluginManifest.serializer(), manifestFile.readText())

            // 预览阶段保留 tempFile 和 tempDir，供后续 completeImport 使用
            Result.success(manifest to tempDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 完成插件导入（在 previewFromZip 后调用）
     * @param manifest 预览阶段解析的 manifest
     * @param tempDir 预览阶段解压的临时目录
     */
    suspend fun completeImport(manifest: PluginManifest, tempDir: File): Result<PluginInfo> {
        return try {
            val existingPlugins = scanPlugins()
            if (existingPlugins.any { it.manifest.id == manifest.id }) {
                tempDir.deleteRecursively()
                return Result.failure(IllegalArgumentException("插件 ${manifest.id} 已存在"))
            }

            val manifestFile = findManifest(tempDir)
                ?: run {
                    tempDir.deleteRecursively()
                    return Result.failure(IllegalArgumentException("找不到 manifest.json"))
                }

            val entryFile = File(manifestFile.parentFile, manifest.entry)
            if (!entryFile.exists()) {
                tempDir.deleteRecursively()
                return Result.failure(IllegalArgumentException("找不到入口文件: ${manifest.entry}"))
            }

            val pluginDir = File(pluginsDir, manifest.id)
            if (pluginDir.exists()) {
                pluginDir.deleteRecursively()
            }
            manifestFile.parentFile?.copyRecursively(pluginDir, overwrite = true)
            tempDir.deleteRecursively()

            // 写入完整性校验和
            runCatching {
                val checksum = computePluginChecksum(pluginDir)
                File(pluginDir, ".integrity").writeText(checksum)
            }

            loadPluginInfo(pluginDir)?.let {
                auditRepo?.log(
                    category = "plugin",
                    action = "installed",
                    target = manifest.id,
                    detail = "插件 ${manifest.name} (${manifest.id}) v${manifest.version} 已安装，作者: ${manifest.author}",
                    status = "success",
                )
                Result.success(it)
            } ?: Result.failure(IllegalStateException("无法加载插件信息"))
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            Result.failure(e)
        }
    }

    /**
     * 从ZIP文件导入插件（旧版一次性导入，保留用于兼容）
     */
    suspend fun importFromZip(uri: Uri): Result<PluginInfo> {
        return try {
            // 1. 复制到临时文件
            val tempFile = File(context.cacheDir, "plugin_import_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("无法读取文件"))

            // 2. 解压到临时目录
            val tempDir = File(context.cacheDir, "plugin_import_${System.currentTimeMillis()}")
            unzip(tempFile, tempDir)

            // 3. 查找manifest.json
            val manifestFile = findManifest(tempDir)
                ?: return Result.failure(IllegalArgumentException("找不到 manifest.json"))

            // 4. 解析manifest
            val content = manifestFile.readText()
            val manifest = json.decodeFromString(PluginManifest.serializer(), content)

            // 5. 检查ID是否重复
            val existingPlugins = scanPlugins()
            if (existingPlugins.any { it.manifest.id == manifest.id }) {
                // 清理临时文件
                tempFile.delete()
                tempDir.deleteRecursively()
                return Result.failure(IllegalArgumentException("插件 ${manifest.id} 已存在"))
            }

            // 6. 验证入口文件
            val entryFile = File(manifestFile.parentFile, manifest.entry)
            if (!entryFile.exists()) {
                tempFile.delete()
                tempDir.deleteRecursively()
                return Result.failure(IllegalArgumentException("找不到入口文件: ${manifest.entry}"))
            }

            // 7. 移动到插件目录
            val pluginDir = File(pluginsDir, manifest.id)
            if (pluginDir.exists()) {
                pluginDir.deleteRecursively()
            }
            manifestFile.parentFile?.copyRecursively(pluginDir, overwrite = true)

            // 8. 清理临时文件
            tempFile.delete()
            tempDir.deleteRecursively()

            // 9. 返回插件信息
            loadPluginInfo(pluginDir)?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("无法加载插件信息"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除插件
     */
    fun deletePlugin(pluginId: String): Boolean {
        val pluginDir = File(pluginsDir, pluginId)
        return if (pluginDir.exists()) {
            pluginDir.deleteRecursively()
        } else {
            false
        }
    }

    /**
     * 获取插件目录
     */
    fun getPluginDir(pluginId: String): File {
        return File(pluginsDir, pluginId)
    }

    /**
     * 解压ZIP文件
     */
    private fun unzip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry: java.util.zip.ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    /**
     * 查找manifest.json文件
     * 优先在根目录查找，然后在子目录中查找
     */
    private fun findManifest(dir: File): File? {
        // 优先在根目录找
        val rootManifest = File(dir, MANIFEST_FILE)
        if (rootManifest.exists()) {
            return rootManifest
        }

        // 在子目录中查找
        return dir.listFiles { file -> file.isDirectory }
            ?.asSequence()
            ?.map { subdir -> File(subdir, MANIFEST_FILE) }
            ?.firstOrNull { it.exists() }
    }

    /**
     * 计算插件目录的 SHA-256 校验和（排除 .integrity 文件本身，稳定排序）
     */
    private fun computePluginChecksum(dir: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        dir.walkTopDown()
            .filter { it.isFile && it.name != ".integrity" }
            .sortedBy { it.relativeTo(dir).path.replace('\\', '/') }
            .forEach { file ->
                digest.update(file.relativeTo(dir).path.replace('\\', '/').toByteArray(Charsets.UTF_8))
                digest.update(file.readBytes())
            }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}