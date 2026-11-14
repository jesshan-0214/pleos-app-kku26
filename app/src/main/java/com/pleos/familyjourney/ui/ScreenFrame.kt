package com.pleos.familyjourney.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pleos.familyjourney.pleos.NaviState

/** 시연용 빨리감기 조작 (가짜 내비일 때만) */
class DemoControls(
    val running: Boolean,
    val onToggleRun: () -> Unit,
    val onSkip10: () -> Unit,
    val onArrive: () -> Unit,
    val onReset: () -> Unit,
)

/**
 * 모든 화면의 공통 틀.
 * 위: 단계 표시와 이전/다음, 가운데: 기능 화면, 아래: 내비 상태 · TTS 자막 · 시연 조작.
 */
@Composable
fun ScreenFrame(
    title: String,
    step: String?,
    navi: NaviState,
    subtitle: String,
    demo: DemoControls?,
    onBack: (() -> Unit)?,
    onNext: (() -> Unit)?,
    nextLabel: String = "다음 단계 →",
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        // --- 상단 ---
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) TextButton(onClick = onBack) { Text("← 메인") }
            step?.let { Chip(it) ; Gap(12.dp) }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onNext != null) Button(onClick = onNext) { Text(nextLabel) }
        }

        // --- 기능 화면 ---
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) { content() }

        // --- 하단 상태바 ---
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (navi.routing || navi.arrived) {
                LinearProgressIndicator(progress = { navi.progress }, modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(naviSummary(navi), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (demo != null) {
                    Text("시연", style = MaterialTheme.typography.labelMedium)
                    Gap(8.dp)
                    OutlinedButton(onClick = demo.onToggleRun) { Text(if (demo.running) "⏸ 정차" else "▶ 주행") }
                    Gap(6.dp)
                    OutlinedButton(onClick = demo.onSkip10) { Text("+10분") }
                    Gap(6.dp)
                    OutlinedButton(onClick = demo.onArrive) { Text("도착") }
                    Gap(6.dp)
                    TextButton(onClick = demo.onReset) { Text("처음부터") }
                }
            }
            if (subtitle.isNotBlank()) {
                Row {
                    Text("🔊", modifier = Modifier.width(28.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun naviSummary(n: NaviState): String {
    if (!n.connected) return "내비 연결 안 됨"
    if (n.arrived) return "🏁 ${n.destinationName ?: "목적지"} 도착 · ${n.lastEvent}"
    if (!n.routing) return "경로 없음 · 출발 단계에서 일정을 내비로 보내세요"
    val remain = n.remainingSec?.let { formatDuration(it) } ?: "-"
    val km = n.remainingMeters?.let { "%.0fkm".format(it / 1000.0) } ?: ""
    val speed = n.speedKmh?.let { " · ${it}km/h" } ?: ""
    val via = if (n.upcomingWaypoints.isNotEmpty()) " · 경유 ${n.upcomingWaypoints.joinToString()}" else ""
    return "🚗 ${n.destinationName} 까지 $remain $km$speed$via · ${n.lastEvent}"
}

fun formatDuration(sec: Int): String {
    val m = (sec + 59) / 60
    return if (m >= 60) "${m / 60}시간 ${m % 60}분" else "${m}분"
}
