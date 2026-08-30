/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.health

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Alert01
import me.rerere.hugeicons.stroke.Pulse01
import me.rerere.hugeicons.stroke.Zap
import me.rerere.rikkahub.data.gadgetbridge.DailySummary
import me.rerere.rikkahub.data.gadgetbridge.HealthUiState
import me.rerere.rikkahub.data.gadgetbridge.SleepSummary
import me.rerere.rikkahub.data.gadgetbridge.StepsRange
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HealthPage(vm: HealthVM = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.onPermissionResult(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else true
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("健康数据") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            !vm.hasManageStoragePermission() -> {
                PermissionRequiredContent(
                    modifier = Modifier.padding(padding),
                    onRequestPermission = {
                        vm.requestStoragePermissionIntent()?.let { permissionLauncher.launch(it) }
                    }
                )
            }
            !state.dbFileExists -> {
                DbNotFoundContent(
                    modifier = Modifier.padding(padding),
                    diagnosticInfo = state.error,
                    onRetry = { vm.checkAndLoad() }
                )
            }
            state.error != null -> {
                ErrorContent(
                    modifier = Modifier.padding(padding),
                    error = state.error!!,
                    onRetry = { vm.checkAndLoad() }
                )
            }
            else -> {
                HealthContent(
                    state = state,
                    padding = padding,
                    onStepsRangeChange = { vm.setStepsRange(it) },
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("需要存储权限读取健康数据", style = MaterialTheme.typography.titleMedium)
            Text(
                "用于读取Gadgetbridge导出的数据库文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRequestPermission) { Text("授予权限") }
        }
    }
}

@Composable
private fun DbNotFoundContent(
    modifier: Modifier = Modifier,
    diagnosticInfo: String? = null,
    onRetry: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = HugeIcons.Alert01,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text("未找到Gadgetbridge数据库", style = MaterialTheme.typography.titleMedium)
            Text(
                "请在Gadgetbridge设置中开启自动导出数据库",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                "预期路径: /sdcard/Download/手环/Gadgetbridge.db",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (diagnosticInfo != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        diagnosticInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun ErrorContent(
    modifier: Modifier = Modifier,
    error: String,
    onRetry: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun HealthContent(
    state: HealthUiState,
    padding: PaddingValues,
    onStepsRangeChange: (StepsRange) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RealTimeHeartRateCard(
                heartRate = state.currentHeartRate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            TodayOverviewCard(
                steps = state.todaySteps,
                calories = state.todayCalories,
                spo2 = state.latestSpo2,
                stress = state.latestStress,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            StepsBarChartCard(
                summaries = if (state.stepsRange == StepsRange.SEVEN_DAYS) state.dailySummaries7 else state.dailySummaries30,
                range = state.stepsRange,
                onRangeChange = onStepsRangeChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            HeartRateLineChartCard(
                summaries = state.dailySummaries7,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SleepSummaryCard(
                summaries = state.sleepSummaries,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Spo2AndStressCard(
                spo2 = state.latestSpo2,
                stress = state.latestStress,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ============ Real-time Heart Rate Card ============
@Composable
private fun RealTimeHeartRateCard(
    heartRate: Int?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CustomColors.cardColorsOnSurfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = HugeIcons.Pulse01,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("实时心率", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (heartRate != null && heartRate > 0) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$heartRate", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.error)
                        Text("BPM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                    }
                } else {
                    Text("暂无数据", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ============ Today Overview Card ============
@Composable
private fun TodayOverviewCard(
    steps: Int,
    calories: Int?,
    spo2: Int?,
    stress: Int?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("今日概览", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OverviewItem(icon = HugeIcons.Zap, label = "步数", value = steps.toString(), tint = MaterialTheme.colorScheme.primary)
                OverviewItem(icon = HugeIcons.Pulse01, label = "卡路里", value = calories?.toString() ?: "--", tint = Color(0xFFFF6B35))
                OverviewItem(icon = HugeIcons.Zap, label = "血氧", value = if (spo2 != null) "${spo2}%" else "--", tint = Color(0xFF4FC3F7))
                OverviewItem(icon = HugeIcons.Alert01, label = "压力", value = stress?.toString() ?: "--", tint = Color(0xFFAB47BC))
            }
        }
    }
}

@Composable
private fun OverviewItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============ Steps Bar Chart Card ============
@Composable
private fun StepsBarChartCard(
    summaries: List<DailySummary>,
    range: StepsRange,
    onRangeChange: (StepsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = HugeIcons.Zap, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("步数统计", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = range == StepsRange.SEVEN_DAYS, onClick = { onRangeChange(StepsRange.SEVEN_DAYS) }, label = { Text("7天") })
                    FilterChip(selected = range == StepsRange.THIRTY_DAYS, onClick = { onRangeChange(StepsRange.THIRTY_DAYS) }, label = { Text("30天") })
                }
            }
            if (summaries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                StepsBarChart(summaries = summaries, modifier = Modifier.fillMaxWidth().height(180.dp))
            }
        }
    }
}

@Composable
private fun StepsBarChart(
    summaries: List<DailySummary>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val maxSteps = summaries.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1

    Canvas(modifier = modifier) {
        val barAreaHeight = size.height - 24f
        val barWidth = (size.width / summaries.size) * 0.6f
        val gapWidth = (size.width / summaries.size) * 0.4f

        summaries.forEachIndexed { index, summary ->
            val x = index * (barWidth + gapWidth) + gapWidth / 2
            val barHeight = (summary.steps.toFloat() / maxSteps) * barAreaHeight * 0.9f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, barAreaHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f),
            )

            val dateText = summary.date.format(dateFormatter)
            val textLayout = textMeasurer.measure(
                dateText,
                style = androidx.compose.ui.text.TextStyle(color = labelColor, fontSize = with(density) { 9.dp.toSp() }),
            )
            drawText(
                textLayout,
                topLeft = Offset(x + barWidth / 2 - textLayout.size.width / 2, barAreaHeight + 4f),
            )

            if (barHeight > 20f && summaries.size <= 15) {
                val stepText = if (summary.steps >= 1000) "${(summary.steps / 100f).roundToInt() / 10f}k" else summary.steps.toString()
                val stepLayout = textMeasurer.measure(
                    stepText,
                    style = androidx.compose.ui.text.TextStyle(color = labelColor, fontSize = with(density) { 8.dp.toSp() }),
                )
                drawText(
                    stepLayout,
                    topLeft = Offset(x + barWidth / 2 - stepLayout.size.width / 2, barAreaHeight - barHeight - 14f),
                )
            }
        }
    }
}

// ============ Heart Rate Line Chart Card ============
@Composable
private fun HeartRateLineChartCard(
    summaries: List<DailySummary>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = HugeIcons.Pulse01, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                Text("心率趋势 (近7天)", style = MaterialTheme.typography.titleMedium)
            }
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendItem(color = MaterialTheme.colorScheme.error, label = "最高")
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "平均")
                LegendItem(color = Color(0xFF4FC3F7), label = "最低")
            }
            val hasData = summaries.any { it.hrMax != null || it.hrAvg != null || it.hrMin != null }
            if (!hasData) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                HeartRateLineChart(summaries = summaries, modifier = Modifier.fillMaxWidth().height(180.dp))
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeartRateLineChart(
    summaries: List<DailySummary>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxColor = MaterialTheme.colorScheme.error
    val avgColor = MaterialTheme.colorScheme.primary
    val minColor = Color(0xFF4FC3F7)

    val hrValues = summaries.mapNotNull { listOfNotNull(it.hrMax, it.hrAvg, it.hrMin) }.flatten()
    val minHr = hrValues.minOrNull()?.coerceAtMost(40) ?: 40
    val maxHr = hrValues.maxOrNull()?.coerceAtLeast(100) ?: 100
    val hrRange = (maxHr - minHr).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val chartHeight = size.height - 24f
        val chartWidth = size.width
        val stepX = if (summaries.size > 1) chartWidth / (summaries.size - 1) else chartWidth / 2f

        // Helper: map HR to Y coordinate
        fun hrToY(hr: Int): Float {
            return chartHeight - ((hr - minHr).toFloat() / hrRange) * chartHeight * 0.9f - chartHeight * 0.05f
        }

        // Draw horizontal grid lines
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = chartHeight * i / gridSteps
            drawLine(
                color = labelColor.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f,
            )
        }

        // Draw lines for max, avg, min
        fun drawLineForData(
            values: List<Int?>,
            color: Color,
        ) {
            val points = values.mapIndexedNotNull { index, hr ->
                if (hr != null && hr > 0) {
                    Offset(index * stepX, hrToY(hr))
                } else null
            }
            if (points.size < 2) return

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Draw dots
            points.forEach { point ->
                drawCircle(color, radius = 4f, center = point)
            }
        }

        drawLineForData(summaries.map { it.hrMax }, maxColor)
        drawLineForData(summaries.map { it.hrAvg }, avgColor)
        drawLineForData(summaries.map { it.hrMin }, minColor)

        // Draw date labels
        summaries.forEachIndexed { index, summary ->
            val dateText = summary.date.format(dateFormatter)
            val textLayout = textMeasurer.measure(
                dateText,
                style = androidx.compose.ui.text.TextStyle(color = labelColor, fontSize = with(density) { 9.dp.toSp() }),
            )
            val x = if (summaries.size > 1) index * stepX else chartWidth / 2f
            drawText(
                textLayout,
                topLeft = Offset(x - textLayout.size.width / 2, chartHeight + 4f),
            )
        }
    }
}

// ============ Sleep Summary Card ============
@Composable
private fun SleepSummaryCard(
    summaries: List<SleepSummary>,
    modifier: Modifier = Modifier,
) {
    if (summaries.isEmpty()) return

    Card(modifier = modifier, colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = HugeIcons.Alert01, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF7E57C2))
                Text("最近睡眠", style = MaterialTheme.typography.titleMedium)
            }

            summaries.take(7).forEachIndexed { index, summary ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
                    val startDate = Instant.ofEpochMilli(summary.timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(dateFormatter)
                    val startTime = Instant.ofEpochMilli(summary.timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalTime()
                        .format(timeFormatter)
                    val endTime = Instant.ofEpochMilli(summary.wakeupTime)
                        .atZone(ZoneId.systemDefault()).toLocalTime()
                        .format(timeFormatter)
                    val hours = summary.totalDuration / 60
                    val mins = summary.totalDuration % 60

                    Column {
                        Text(
                            if (summary.isNap) "小憩" else "睡眠",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$startDate $startTime - $endTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${hours}h ${mins}min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!summary.isNap && summary.totalDuration > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("深${summary.deepSleep}m", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0))
                                Text("浅${summary.lightSleep}m", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64B5F6))
                                Text("REM${summary.remSleep}m", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAB47BC))
                            }
                        }
                    }
                }
                if (index < summaries.take(7).size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

// ============ SpO2 and Stress Card ============
@Composable
private fun Spo2AndStressCard(
    spo2: Int?,
    stress: Int?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("血氧 & 压力", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // SpO2 circular indicator
                CircularIndicator(
                    value = spo2,
                    maxValue = 100,
                    minValue = 90,
                    label = "血氧",
                    unit = "%",
                    color = Color(0xFF4FC3F7),
                    modifier = Modifier.size(100.dp),
                )
                // Stress circular indicator
                CircularIndicator(
                    value = stress,
                    maxValue = 100,
                    minValue = 0,
                    label = "压力",
                    unit = "",
                    color = Color(0xFFAB47BC),
                    modifier = Modifier.size(100.dp),
                )
            }
        }
    }
}

@Composable
private fun CircularIndicator(
    value: Int?,
    maxValue: Int,
    minValue: Int,
    label: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = modifier) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )

            // Progress arc
            if (value != null) {
                val progress = ((value - minValue).toFloat() / (maxValue - minValue).coerceAtLeast(1)).coerceIn(0f, 1f)
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (value != null) {
                Text(
                    text = "$value$unit",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                )
            } else {
                Text("--", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}