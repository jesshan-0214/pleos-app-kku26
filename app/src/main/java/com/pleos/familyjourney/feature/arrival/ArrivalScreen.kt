package com.pleos.familyjourney.feature.arrival

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.familyjourney.ui.BadgeMedal
import com.pleos.familyjourney.ui.Chip
import com.pleos.familyjourney.ui.Gap
import com.pleos.familyjourney.ui.QrCode
import com.pleos.familyjourney.ui.SectionCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArrivalScreen(vm: ArrivalViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var spoken by rememberSaveable { mutableStateOf("") }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- 배지 ---
        Column(Modifier.weight(1.2f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard(if (ui.arrived) "🏁 ${ui.destinationName ?: "목적지"} 도착!" else "아직 이동 중") {
                val badge = ui.newBadge
                if (badge != null) {
                    var shown by remember(badge.regionCode) { mutableStateOf(false) }
                    LaunchedEffect(badge.regionCode) { shown = true }
                    val scale by animateFloatAsState(
                        if (shown) 1f else 0.2f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "badge",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.scale(scale)) {
                            BadgeMedal(badge.emoji, badge.regionName, Color(badge.color), earned = true, size = 120.dp)
                        }
                        Gap(24.dp)
                        Column(Modifier.weight(1f)) {
                            Text("새 배지 획득!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("${badge.regionName} · ${badge.feature}")
                        }
                    }
                } else {
                    Text(
                        if (ui.arrived) "이 지역 배지는 이미 모았어요." else "목적지에 도착하면 그 지역 배지를 받아요.",
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            SectionCard("배지 도감", trailing = { Chip("${ui.badges.count { it.earned }} / ${ui.badges.size}") }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ui.badges.forEach { BadgeMedal(it.emoji, it.regionName, Color(it.color), it.earned) }
                }
            }
        }

        // --- 체크리스트 ---
        SectionCard("내리기 전 체크리스트", Modifier.weight(1f)) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ui.byOwner.forEach { (owner, items) ->
                    Text(owner, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(item.checked, { vm.toggle(item.id) })
                            Text(
                                item.text,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            )
                            TextButton(onClick = { vm.remove(item.id) }) { Text("삭제") }
                        }
                    }
                }
                OutlinedTextField(
                    spoken, { spoken = it },
                    label = { Text("🎤 \"서준이 모래놀이 세트\" 처럼 말하기") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { vm.addFromSpeech(spoken); spoken = "" }, enabled = spoken.isNotBlank()) { Text("추가") }
                    Button(onClick = vm::remind) { Text("🔊 다시 안내") }
                }
                QrCode("https://ridemate.web.app/checklist.html", size = 110.dp)
            }
        }
    }
}
