package com.pleos.familyjourney.feature.departure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.familyjourney.data.PlanStop
import com.pleos.familyjourney.ui.Chip
import com.pleos.familyjourney.ui.SectionCard
import com.pleos.familyjourney.ui.Success
import com.pleos.familyjourney.ui.formatDuration

private const val START_HOUR = 10 // 시연 시나리오: 오전 10시 출발

/** 일정 작성: 탑승자 구성 → (TourAPI 후보 + LLM) 일정 → 내비에 목적지·경유지 설정 */
@Composable
fun PlannerScreen(vm: DepartureViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val plan = ui.plan

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard("일정", Modifier.weight(1.4f), trailing = { plan?.let { Chip(it.mode.label) } }) {
            when {
                ui.planning -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text("  Gleo AI가 탑승자 구성에 맞춰 일정을 짜는 중…")
                }
                plan == null -> Text(
                    if (ui.passengers.isEmpty()) "먼저 탑승자를 설정하세요." else "오른쪽 [맞춤 일정 만들기]를 눌러 보세요.",
                    color = MaterialTheme.colorScheme.outline,
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(plan.stops) { StopRow(it) }
                }
            }
        }
        SectionCard("여정 플래너", Modifier.weight(1f)) {
            Text("탑승자 ${ui.passengers.size}명 · ${ui.mode?.label ?: "-"}")
            Text(
                ui.passengers.joinToString("\n") { "· ${it.name} (${it.ageGroup.label}${if (it.note.isNotBlank()) ", ${it.note}" else ""})" },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = vm::createPlan,
                enabled = ui.passengers.isNotEmpty() && !ui.planning,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (plan == null) "맞춤 일정 만들기" else "일정 다시 만들기") }

            if (plan != null) {
                Text(plan.summary, style = MaterialTheme.typography.bodyMedium)
                Text("총 운전 ${formatDuration(plan.totalDriveMin * 60)}", fontWeight = FontWeight.Bold)
                Button(onClick = vm::sendToNavi, modifier = Modifier.fillMaxWidth()) {
                    Text(if (ui.routeSent) "다시 보내기" else "내비에 경로 보내기")
                }
                if (ui.routeSent) Text("✓ 내비 안내 시작 (requestRouteWithWaypoints)", color = Success)
            }
        }
    }
}

@Composable
private fun StopRow(stop: PlanStop) {
    val total = START_HOUR * 60 + stop.etaMin
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("%02d:%02d".format(total / 60 % 24, total % 60), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(72.dp))
        Text(stop.place.type.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Text(stop.place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stop.reason + if (stop.stayMin > 0) " · ${stop.stayMin}분 머무름" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Chip(stop.place.type.label)
    }
}
