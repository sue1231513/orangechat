/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.tools

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.common.android.Logging
import java.io.File

/** 递归统计目录占用；符号链接不跟随，避免绕圈。 */
private fun dirSize(file: File): Long {
    if (!file.exists()) return 0L
    if (file.isFile) return file.length()
    val children = file.listFiles() ?: return 0L
    var total = 0L
    for (child in children) {
        total += if (child.isDirectory) dirSize(child) else child.length()
    }
    return total
}

private fun countFiles(file: File): Int {
    if (!file.exists()) return 0
    if (file.isFile) return 1
    val children = file.listFiles() ?: return 0
    var count = 0
    for (child in children) {
        count += if (child.isDirectory) countFiles(child) else 1
    }
    return count
}

private fun clearDirectoryContents(file: File): Pair<Long, Int> {
    if (!file.isDirectory) return 0L to 0
    var freedBytes = 0L
    var deletedEntries = 0
    file.listFiles()?.forEach { child ->
        val size = dirSize(child)
        if (child.deleteRecursively()) {
            freedBytes += size
            deletedEntries++
        }
    }
    return freedBytes to deletedEntries
}

/**
 * Rootfs 里可安全再生的缓存目录。这里故意不包含 .gradle / .local：
 * 它们可能含构建环境和工作区运行时数据，清掉会让离线编译或工具环境断掉。
 */
private fun rootfsSafeCacheDirs(filesDir: File): List<File> {
    val rootfs = File(filesDir, "workspaces")
        .listFiles()
        ?.firstOrNull { File(it, "linux").isDirectory }
        ?.resolve("linux")
        ?: return emptyList()
    return listOf(
        File(rootfs, "tmp"),
        File(rootfs, "var/cache"),
        File(rootfs, "root/.npm"),
        File(rootfs, "root/.cache"),
    )
}

fun createStorageInfoTool(context: Context): Tool = Tool(
    name = "get_storage_info",
    description = "Get storage usage info, or clear only the app workspace rootfs's safe, " +
        "regenerable caches (tmp, package caches). Never clears projects, chat data, Gradle, " +
        "or runtime data. action: inspect (default) or clear_safe_rootfs_caches.",
    needsApproval = true,
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                putJsonObject("action") {
                    put("type", "string")
                    put("description", "inspect (default) or clear_safe_rootfs_caches")
                }
            }
        )
    },
    execute = { input ->
        try {
            val action = input.toString()
                .let { raw -> Regex("\\\"action\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.getOrNull(1) }
                ?: "inspect"
            val clearSafeCaches = action == "clear_safe_rootfs_caches"
            val result = buildJsonObject {
                put("success", true)
                put("action", action)

                // ── 设备整体空间 ──
                try {
                    val internalPath = Environment.getDataDirectory().path
                    val stat = StatFs(internalPath)
                    val totalBytes = stat.totalBytes
                    val freeBytes = stat.freeBytes
                    putJsonObject("internal") {
                        put("total_bytes", totalBytes)
                        put("free_bytes", freeBytes)
                        put("used_bytes", totalBytes - freeBytes)
                    }
                } catch (e: Exception) {
                    Logging.log("StorageInfoTool", "Error reading internal storage: ${e.message}")
                    putJsonObject("internal") {
                        put("error", e.message ?: "Failed to read internal storage")
                    }
                }

                try {
                    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                        val externalPath = Environment.getExternalStorageDirectory().path
                        val stat = StatFs(externalPath)
                        val totalBytes = stat.totalBytes
                        val freeBytes = stat.freeBytes
                        putJsonObject("external") {
                            put("total_bytes", totalBytes)
                            put("free_bytes", freeBytes)
                            put("used_bytes", totalBytes - freeBytes)
                        }
                    } else {
                        put("external", kotlinx.serialization.json.JsonNull)
                    }
                } catch (e: Exception) {
                    Logging.log("StorageInfoTool", "Error reading external storage: ${e.message}")
                    putJsonObject("external") {
                        put("error", e.message ?: "Failed to read external storage")
                    }
                }

                // ── 本应用私有空间的分目录明细 ──
                // 系统设置里的「数据」是这几块之和，光看总数无法判断谁在堆。
                try {
                    val filesDir = context.filesDir
                    if (clearSafeCaches) {
                        var freedBytes = 0L
                        var deletedEntries = 0
                        putJsonObject("cleared_rootfs_caches") {
                            rootfsSafeCacheDirs(filesDir).forEach { dir ->
                                val (freed, entries) = clearDirectoryContents(dir)
                                freedBytes += freed
                                deletedEntries += entries
                                putJsonObject(dir.path.substringAfter("/linux/")) {
                                    put("freed_bytes", freed)
                                    put("deleted_top_level_entries", entries)
                                }
                            }
                            put("total_freed_bytes", freedBytes)
                            put("total_deleted_top_level_entries", deletedEntries)
                            put("note", "Only tmp, var/cache, root/.npm and root/.cache were cleared. .gradle, .local, projects and chat data were preserved.")
                        }
                    }
                    val cacheDir = context.cacheDir
                    // databases 与 files 同级
                    val dataRoot = filesDir.parentFile
                    val databasesDir = dataRoot?.let { File(it, "databases") }
                    val sharedPrefsDir = dataRoot?.let { File(it, "shared_prefs") }
                    val noBackupDir = runCatching { context.noBackupFilesDir }.getOrNull()

                    putJsonObject("app_data") {
                        put("files_bytes", dirSize(filesDir))
                        put("cache_bytes", dirSize(cacheDir))
                        databasesDir?.let { put("databases_bytes", dirSize(it)) }
                        sharedPrefsDir?.let { put("shared_prefs_bytes", dirSize(it)) }
                        noBackupDir?.let { put("no_backup_bytes", dirSize(it)) }

                        // files/ 下逐个子目录，找出真正的大头
                        putJsonObject("files_children") {
                            filesDir.listFiles()?.sortedByDescending { dirSize(it) }?.forEach { child ->
                                putJsonObject(child.name) {
                                    put("bytes", dirSize(child))
                                    put("is_directory", child.isDirectory)
                                    if (child.isDirectory) put("file_count", countFiles(child))
                                }
                            }
                        }

                        // workspaces/ 再展开一层：rootfs 的 .git、构建缓存、依赖目录
                        // 往往比整个 workspace 目录更能说明问题。
                        val workspacesDir = File(filesDir, "workspaces")
                        if (workspacesDir.isDirectory) {
                            putJsonObject("workspace_roots") {
                                workspacesDir.listFiles()
                                    ?.sortedByDescending { dirSize(it) }
                                    ?.forEach { root ->
                                        putJsonObject(root.name) {
                                            put("bytes", dirSize(root))
                                            put("is_directory", root.isDirectory)
                                            if (root.isDirectory) {
                                                put("file_count", countFiles(root))
                                                // rootfs 内再拆一层：这里才能区分系统层、项目、.gradle 与缓存。
                                                putJsonObject("children") {
                                                    root.listFiles()
                                                        ?.sortedByDescending { dirSize(it) }
                                                        ?.forEach { child ->
                                                            putJsonObject(child.name) {
                                                                put("bytes", dirSize(child))
                                                                put("is_directory", child.isDirectory)
                                                                if (child.isDirectory) {
                                                                    put("file_count", countFiles(child))
                                                                    // linux 是 rootfs 本体，一级目录（usr/var/root…）仍过粗；
                                                                    // 继续展开一层，只做诊断，绝不在统计时删除。
                                                                    if (child.name == "linux") {
                                                                        putJsonObject("children") {
                                                                            child.listFiles()
                                                                                ?.sortedByDescending { dirSize(it) }
                                                                                ?.forEach { linuxChild ->
                                                                                    putJsonObject(linuxChild.name) {
                                                                                        put("bytes", dirSize(linuxChild))
                                                                                        put("is_directory", linuxChild.isDirectory)
                                                                                        if (linuxChild.isDirectory) {
                                                                                            put("file_count", countFiles(linuxChild))
                                                                                            // root 是包管理/构建缓存最常驻的地方；var 常放日志与 apt 缓存。
                                                                                            // 只展开这两处，避免诊断 JSON 膨胀成另一份大文件。
                                                                                            if (linuxChild.name == "root" || linuxChild.name == "var") {
                                                                                                putJsonObject("children") {
                                                                                                    linuxChild.listFiles()
                                                                                                        ?.sortedByDescending { dirSize(it) }
                                                                                                        ?.forEach { leaf ->
                                                                                                            putJsonObject(leaf.name) {
                                                                                                                put("bytes", dirSize(leaf))
                                                                                                                put("is_directory", leaf.isDirectory)
                                                                                                                if (leaf.isDirectory) put("file_count", countFiles(leaf))
                                                                                                            }
                                                                                                        }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    }
                            }
                        }

                        // cache/ 下逐个子目录
                        putJsonObject("cache_children") {
                            cacheDir.listFiles()?.sortedByDescending { dirSize(it) }?.forEach { child ->
                                putJsonObject(child.name) {
                                    put("bytes", dirSize(child))
                                    put("is_directory", child.isDirectory)
                                    if (child.isDirectory) put("file_count", countFiles(child))
                                }
                            }
                        }

                        // databases/ 里逐个文件：主库、-wal、-shm 分开看
                        // WAL 没做 checkpoint 时能长到比主库还大。
                        databasesDir?.let { dir ->
                            putJsonObject("database_files") {
                                dir.listFiles()?.sortedByDescending { it.length() }?.forEach { f ->
                                    put(f.name, f.length())
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logging.log("StorageInfoTool", "Error reading app data breakdown: ${e.message}")
                    putJsonObject("app_data") {
                        put("error", e.message ?: "Failed to read app data breakdown")
                    }
                }
            }

            listOf(UIMessagePart.Text(result.toString()))
        } catch (e: Exception) {
            Logging.log("StorageInfoTool", "Unexpected error: ${e.message}\n${e.stackTraceToString()}")
            listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("success", false)
                        put("error", e.message ?: "Unknown error")
                    }.toString()
                )
            )
        }
    }
)
