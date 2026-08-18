package br.com.redesurftank.havalshisuku.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.redesurftank.havalshisuku.managers.TripConsistencyManager
import br.com.redesurftank.havalshisuku.managers.TripConsistencyConfig
import br.com.redesurftank.havalshisuku.models.TripConsistencyClassification
import br.com.redesurftank.havalshisuku.models.TripConsistencyMetrics
import br.com.redesurftank.havalshisuku.models.TripConsistencyReport
import br.com.redesurftank.havalshisuku.models.TripConsistencySession
import br.com.redesurftank.havalshisuku.models.TripConsistencyStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TripConsistencyScreen(onBackToFeatures: (() -> Unit)? = null) {
    val manager = remember { TripConsistencyManager.getInstance() }
    LaunchedEffect(Unit) { manager.initialize() }

    val session = manager.currentSession
    val reportHistory = manager.reportHistory
    var selectedReport by remember { mutableStateOf<TripConsistencyReport?>(null) }
    var showRules by remember { mutableStateOf(false) }
    var note by remember(session?.id) { mutableStateOf(session?.notes.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TripScoreHeader(
            onShowRules = { showRules = true },
            onBackToFeatures = onBackToFeatures
        )

        when {
            showRules -> ScoreRulesView(onBack = { showRules = false })
            selectedReport != null -> TripReportView(
                report = selectedReport,
                onBack = { selectedReport = null },
                onDelete = {
                    selectedReport?.let { manager.deleteReport(it.id) }
                    selectedReport = null
                }
            )
            session == null -> IdleTripScoreView(
                reports = reportHistory,
                onStart = {
                    manager.startTrip()
                    selectedReport = null
                    showRules = false
                },
                onOpenReport = { selectedReport = it },
                onShowRules = { showRules = true }
            )
            session.status in setOf(
                TripConsistencyStatus.PAUSED_AFTER_IGNITION_OFF,
                TripConsistencyStatus.WAITING_USER_CONFIRMATION
            ) -> PausedTripView(
                session = session,
                onContinue = { manager.continueTrip() },
                onFinish = {
                    manager.addNote(note)
                    selectedReport = manager.finishTrip()
                },
                onLater = { manager.viewLater() }
            )
            else -> ActiveTripView(
                session = session,
                note = note,
                onNoteChange = {
                    note = it
                    manager.addNote(it)
                },
                onFinish = {
                    manager.addNote(note)
                    selectedReport = manager.finishTrip()
                }
            )
        }
    }
}

@Composable
private fun TripScoreHeader(
    onShowRules: () -> Unit,
    onBackToFeatures: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackToFeatures != null) {
                    Surface(
                        modifier = Modifier
                            .width(78.dp)
                            .height(52.dp)
                            .clickable(onClick = onBackToFeatures),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
                        color = AppColors.ButtonSecondary
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("<", color = AppColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    "行程一致评分",
                    color = AppColors.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            SecondaryButton(onClick = onShowRules, text = "原理说明")
        }
        Text(
            "基于 Impulse 已监控的车辆遥测数据评估平顺性、稳定性和效率。",
            color = AppColors.TextSecondary,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun IdleTripScoreView(
    reports: List<TripConsistencyReport>,
    onStart: () -> Unit,
    onOpenReport: (TripConsistencyReport) -> Unit,
    onShowRules: () -> Unit
) {
    StyledCard {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("当前没有分析中的行程", color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "开始行程即可实时查看评分。分析期间仪表只显示一个低调的指示标记。",
                color = AppColors.TextSecondary,
                fontSize = 17.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(onClick = onStart, text = "开始行程分析")
                SecondaryButton(onClick = onShowRules, text = "了解评分")
                if (reports.isNotEmpty()) {
                    SecondaryButton(onClick = { onOpenReport(reports.first()) }, text = "查看最近报告")
                }
            }
        }
    }

    TripHistoryView(reports = reports, onOpenReport = onOpenReport)
}

@Composable
private fun ActiveTripView(
    session: TripConsistencySession,
    note: String,
    onNoteChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    val scoreReady = session.metrics.samplesCount >= TripConsistencyConfig.MIN_VALID_SAMPLES_FOR_SCORE
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        StyledCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TripScoreGauge(
                    score = session.currentScore.takeIf { scoreReady },
                    classification = session.currentClassification.takeIf { scoreReady }
                )
                ClassificationChips(session.currentClassification, enabled = scoreReady)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TripMetricPill(Icons.Default.Route, "距离", formatDistance(session.distanceKm))
                    TripMetricPill(Icons.Default.Timeline, "时长", formatDuration(session.elapsedSeconds))
                }
                if (!scoreReady || session.telemetryWarning) {
                    Text(
                        "等待遥测数据后开始评分。",
                        color = Color(0xFFFFC857),
                        fontSize = 14.sp
                    )
                }
            }
        }

        StyledCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Prévia em tempo real", color = AppColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                MetricsGrid(session.metrics, scoreReady = scoreReady)
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("行程备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                PrimaryButton(onClick = onFinish, text = "结束行程并生成报告", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PausedTripView(
    session: TripConsistencySession,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onLater: () -> Unit
) {
    val scoreReady = session.metrics.samplesCount >= TripConsistencyConfig.MIN_VALID_SAMPLES_FOR_SCORE
    StyledCard {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("还在行程中吗？", color = AppColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("分析进行中车辆已断电。", color = AppColors.TextSecondary, fontSize = 17.sp)
                }
            }
            TripScoreGauge(
                score = session.currentScore.takeIf { scoreReady },
                classification = session.currentClassification.takeIf { scoreReady },
                compact = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(onClick = onContinue, text = "继续行程")
                SecondaryButton(onClick = onFinish, text = "结束并生成报告")
                SecondaryButton(onClick = onLater, text = "稍后")
            }
        }
    }
}

@Composable
private fun TripReportView(
    report: TripConsistencyReport?,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    if (report == null) return

    StyledCard {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("行程报告", color = AppColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("${formatInstant(report.startedAt)} 到 ${formatInstant(report.endedAt)}", color = AppColors.TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(onClick = onBack, text = "返回")
                    SecondaryButton(onClick = onDelete, text = "删除报告")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TripScoreGauge(report.score, report.classification, compact = true)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(report.classificationLabel, color = Color(0xFF00D8FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(report.summaryText, color = AppColors.TextSecondary, fontSize = 17.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TripMetricPill(Icons.Default.Route, "距离", formatDistance(report.distanceKm))
                        TripMetricPill(Icons.Default.Timeline, "时长", formatDuration(report.durationSeconds))
                        TripMetricPill(Icons.Default.Flag, "事件", report.events.size.toString())
                    }
                    MetricsGrid(report.metrics, scoreReady = true)
                }
            }
        }
    }
}

@Composable
private fun TripHistoryView(
    reports: List<TripConsistencyReport>,
    onOpenReport: (TripConsistencyReport) -> Unit
) {
    StyledCard {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF00D8FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("行程历史", color = AppColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("最近 ${reports.size}/10 次已结束行程。", color = AppColors.TextSecondary, fontSize = 14.sp)
                }
            }

            if (reports.isEmpty()) {
                Text("还没有已结束的行程。", color = AppColors.TextSecondary, fontSize = 16.sp)
            } else {
                reports.forEach { report ->
                    TripHistoryRow(report = report, onClick = { onOpenReport(report) })
                }
            }
        }
    }
}

@Composable
private fun TripHistoryRow(report: TripConsistencyReport, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111B27),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TripScoreMiniBadge(report.score)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(report.classificationLabel, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${formatInstant(report.startedAt)} - ${formatDistance(report.distanceKm)} - ${formatDuration(report.durationSeconds)}", color = AppColors.TextSecondary, fontSize = 14.sp)
                }
            }
            Text("打开", color = Color(0xFF00D8FF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TripScoreMiniBadge(score: Int) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Color(0xFF0C3044), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(score.toString(), color = Color(0xFF00D8FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreRulesView(onBack: () -> Unit) {
    StyledCard {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00D8FF), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("评分原理", color = AppColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("规则基于 Impulse 已读取的真实数据实现。", color = AppColors.TextSecondary, fontSize = 16.sp)
                    }
                }
                SecondaryButton(onClick = onBack, text = "返回")
            }

            ScoreRuleSection(
                title = "使用数据",
                lines = listOf(
                    "车速、里程和车辆状态。",
                    "可用的瞬时油耗与能耗。",
                    "通过能量回收电平/电流识别相关事件。",
                    "不使用 GPS：评分不依赖路线、定位或地图。"
                )
            )
            ScoreRuleSection(
                title = "评分权重",
                lines = listOf(
                    "速度变化：25%。",
                    "平顺加速：20%。",
                    "制动/能量回收：20%。",
                    "能耗：15%。",
                    "稳定性：15%。",
                    "路况判断：5%。"
                )
            )
            ScoreRuleSection(
                title = "分级",
                lines = listOf(
                    "平稳行程：评分高、少急变、驾驶可预测。",
                    "运动行程：多次急加速/急刹车或大量速度波动。",
                    "拥堵路况：平均车速低、最高车速低且走走停停。"
                )
            )
            ScoreRuleSection(
                title = "持续保存",
                lines = listOf(
                    "车辆断电时，进行中的行程会自动保存。",
                    "12 小时内再次通电可继续行程，不会丢失数据。",
                    "历史记录最多保留最近 10 次行程。"
                )
            )
        }
    }
}

@Composable
private fun ScoreRuleSection(title: String, lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color(0xFF00D8FF), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        lines.forEach { line ->
            Text("- $line", color = AppColors.TextSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TripScoreGauge(
    score: Int?,
    classification: TripConsistencyClassification?,
    compact: Boolean = false
) {
    val size = if (compact) 190.dp else 260.dp
    val stroke = if (compact) 12.dp else 18.dp
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val arcSize = Size(size.toPx() - strokePx, size.toPx() - strokePx)
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            drawArc(
                color = Color(0xFF263244),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFF00D8FF), Color(0xFF4A9EFF), Color(0xFF00D8FF))),
                startAngle = 135f,
                sweepAngle = 270f * ((score ?: 0).coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score?.toString() ?: "--", color = AppColors.TextPrimary, fontSize = if (compact) 54.sp else 82.sp, fontWeight = FontWeight.Bold)
            Text("/100", color = Color(0xFF4A9EFF), fontSize = if (compact) 18.sp else 24.sp)
            Text(
                formatGaugeClassificationLabel(classification),
                color = Color(0xFF00D8FF),
                fontSize = if (compact) 14.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = if (compact) 16.sp else 20.sp
            )
        }
    }
}

private fun formatGaugeClassificationLabel(classification: TripConsistencyClassification?): String =
    when (classification) {
        TripConsistencyClassification.SMOOTH -> "平稳\n行程"
        TripConsistencyClassification.SPORTY -> "运动\n行程"
        TripConsistencyClassification.HEAVY_TRAFFIC -> classification.label.uppercase(Locale.getDefault())
        null -> "等待中"
    }

@Composable
private fun ClassificationChips(current: TripConsistencyClassification, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TripConsistencyClassification.values().forEach { item ->
            val selected = enabled && item == current
            AssistChip(
                onClick = {},
                label = { Text(item.label) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) Color(0xFF0C3044) else AppColors.SurfaceVariant,
                    labelColor = if (selected) Color(0xFF00D8FF) else AppColors.TextSecondary,
                    leadingIconContentColor = Color(0xFF00D8FF)
                )
            )
        }
    }
}

@Composable
private fun MetricsGrid(metrics: TripConsistencyMetrics, scoreReady: Boolean, compact: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp)) {
        MetricProgress("速度变化", metrics.speedVariationScore.takeIf { scoreReady })
        MetricProgress("平顺加速", metrics.accelerationScore.takeIf { scoreReady })
        MetricProgress("制动/能量回收", metrics.brakeRegenScore.takeIf { scoreReady })
        MetricProgress("能耗", (metrics.consumptionScore ?: metrics.energyEfficiencyScore).takeIf { scoreReady })
        MetricProgress("稳定性", metrics.stabilityScore.takeIf { scoreReady })
        Divider(color = AppColors.BorderColor)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TripMetricPill(Icons.Default.Speed, "平均", formatKmh(metrics.averageSpeedKmh), compact = compact)
            TripMetricPill(Icons.Default.Bolt, "急加速", metrics.strongAccelerationCount.toString(), compact = compact)
            TripMetricPill(Icons.Default.LocalGasStation, "停车", metrics.stopCount.toString(), compact = compact)
        }
    }
}

@Composable
private fun MetricProgress(label: String, score: Int?) {
    val progress = (score ?: 0).coerceIn(0, 100) / 100f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AppColors.TextSecondary, fontSize = 14.sp)
            Text(score?.toString() ?: "--", color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF243044), RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(Color(0xFF00D8FF), RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun TripMetricPill(icon: ImageVector, label: String, value: String, compact: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111B27),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 7.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00D8FF), modifier = Modifier.size(if (compact) 19.dp else 22.dp))
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
            Column {
                Text(label, color = AppColors.TextSecondary, fontSize = if (compact) 11.sp else 12.sp)
                Text(value, color = AppColors.TextPrimary, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatDistance(value: Double?): String = value?.let { String.format(Locale.US, "%.1f km", it) } ?: "-- km"

private fun formatKmh(value: Double?): String = value?.let { String.format(Locale.US, "%.0f km/h", it) } ?: "-- km/h"

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}

private fun formatInstant(value: String): String =
    runCatching {
        DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(value))
    }.getOrDefault("--")
