package me.rerere.rikkahub.ui.pages.health

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.gadgetbridge.GadgetbridgeDbPath
import me.rerere.rikkahub.data.gadgetbridge.GadgetbridgeReader
import me.rerere.rikkahub.data.gadgetbridge.HealthUiState
import me.rerere.rikkahub.data.gadgetbridge.StepsRange
import java.io.File

class HealthVM(
    application: Application,
    private val settingsStore: SettingsStore,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(HealthUiState())
    val state = _state.asStateFlow()

    init {
        checkAndLoad()
    }

    fun checkAndLoad() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Android 11+ 必须先有 MANAGE_EXTERNAL_STORAGE 权限
            if (!hasManageStoragePermission()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    dbFileExists = false,
                    error = "需要存储权限才能读取健康数据"
                )
                return@launch
            }

            val customPath = settingsStore.settingsFlow.value.systemToolsSetting.gadgetbridgeDbPath

            // 有权限后再检查文件
            if (!GadgetbridgeReader.dbFileExists(customPath)) {
                // 构建诊断信息，帮助用户排查
                val paths = GadgetbridgeDbPath.getPossiblePaths(customPath)
                val diagInfo = buildString {
                    append("未找到数据库文件。请确认 Gadgetbridge 已开启自动导出。\n")
                    append("预期路径: ${GadgetbridgeDbPath.DB_PATH}\n")
                    append("外部存储目录: ${Environment.getExternalStorageDirectory().absolutePath}\n")
                    // 检查 Download 目录是否存在
                    val downloadDir = File(Environment.getExternalStorageDirectory(), "Download")
                    append("Download目录存在: ${downloadDir.exists()}\n")
                    if (downloadDir.exists()) {
                        val subDirs = downloadDir.listFiles()?.map { it.name } ?: emptyList()
                        append("Download子目录: ${subDirs.joinToString(", ")}\n")
                        val shouhuanDir = File(downloadDir, "手环")
                        if (shouhuanDir.exists()) {
                            val files = shouhuanDir.listFiles()?.map { it.name } ?: emptyList()
                            append("手环目录文件: ${files.joinToString(", ")}\n")
                        } else {
                            append("手环目录不存在\n")
                        }
                    }
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    dbFileExists = false,
                    error = diagInfo
                )
                return@launch
            }

            loadHealthData(customPath)
        }
    }

    fun setStepsRange(range: StepsRange) {
        _state.value = _state.value.copy(stepsRange = range)
        viewModelScope.launch { loadStepsData(range) }
    }

    private suspend fun loadHealthData(customPath: String = "") {
        withContext(Dispatchers.IO) {
            try {
                val latestActivity = GadgetbridgeReader.readLatestActivitySample(customPath)
                val dailySummaries7 = GadgetbridgeReader.readDailySummaries(7, customPath)
                val dailySummaries30 = GadgetbridgeReader.readDailySummaries(30, customPath)
                val sleepStages = GadgetbridgeReader.readLastNightSleepStages(customPath)
                val (spo2, stress) = GadgetbridgeReader.readLatestSpo2AndStress(customPath)
                val todaySummary = dailySummaries7.lastOrNull()
                _state.value = _state.value.copy(
                    isLoading = false, dbFileExists = true,
                    currentHeartRate = latestActivity?.heartRate,
                    dailySummaries7 = dailySummaries7,
                    dailySummaries30 = dailySummaries30,
                    lastNightSleepStages = sleepStages,
                    latestSpo2 = spo2, latestStress = stress,
                    todaySteps = todaySummary?.steps ?: 0,
                    todayCalories = todaySummary?.calories,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "读取健康数据失败: ${e.message}")
            }
        }
    }

    private suspend fun loadStepsData(range: StepsRange) {
        withContext(Dispatchers.IO) {
            try {
                val customPath = settingsStore.settingsFlow.value.systemToolsSetting.gadgetbridgeDbPath
                val days = if (range == StepsRange.SEVEN_DAYS) 7 else 30
                val summaries = GadgetbridgeReader.readDailySummaries(days, customPath)
                if (range == StepsRange.SEVEN_DAYS) {
                    _state.value = _state.value.copy(dailySummaries7 = summaries)
                } else {
                    _state.value = _state.value.copy(dailySummaries30 = summaries)
                }
            } catch (_: Exception) {}
        }
    }

    fun openGadgetbridgeExportSettings() {
        val context = getApplication<Application>()
        try {
            val intent = Intent("nodomain.freeyourgadget.gadgetbridge.activity.SettingsActivity")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Android 10 及以下，检查 READ_EXTERNAL_STORAGE
            androidx.core.content.ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${getApplication<Application>().packageName}")
            }
        } else null
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) checkAndLoad()
    }
}