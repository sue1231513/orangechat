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

fun createStorageInfoTool(context: Context): Tool = Tool(
    name = "get_storage_info",
    description = "Get storage usage info: device-level free/total space, plus a per-directory " +
        "breakdown of this app's own private storage (files/, cache/, databases/ and each " +
        "subfolder), with file counts. Use this to find what is consuming app data space.",
    needsApproval = true,
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {}
        )
    },
    execute = { _ ->
        try {
            val result = buildJsonObject {
                put("success", true)

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
