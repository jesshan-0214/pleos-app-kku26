package com.pleos.familyjourney.feature.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.familyjourney.ui.Chip
import com.pleos.familyjourney.ui.Danger
import com.pleos.familyjourney.ui.QrCode
import com.pleos.familyjourney.ui.SectionCard
import com.pleos.familyjourney.ui.Success
import com.pleos.familyjourney.ui.formatDuration

@Composable
fun QuestScreen(vm: QuestViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- 남은 시간 비유 안내 ---
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("도착까지") {
                val g = ui.guide
                if (!ui.routing || g == null) {
                    Text("내비 안내가 시작되면 남은 시간을 알려 줘요.", color = MaterialTheme.colorScheme.outline)
                } else {
                    Text("🎵 노래 약 ${g.songs}곡", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("📖 이야기 ${g.stories}편", fontSize = 26.sp)
                    Text("(실제 남은 시간 ${formatDuration(ui.remainingSec ?: 0)})", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = vm::announceRemaining, modifier = Modifier.fillMaxWidth()) { Text("🔊 아이에게 말해 주기") }
                }
            }
            SectionCard("지금 지나는 곳") {
                Text(ui.region?.name ?: "위치 확인 중…", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("지역이 바뀌면 그 지역 퀴즈가 자동으로 바뀌어요.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // --- 지역 퀴즈 ---
        SectionCard(
            "지역 퀴즈",
            Modifier.weight(1.6f),
            trailing = {
                if (ui.touchLocked) Chip("🔒 주행 중 · 음성/폰으로 참여", Danger) else Chip("정차 중 · 화면 터치 가능", Success)
            },
        ) {
            val q = ui.question
            if (q == null) {
                Text("이 지역 퀴즈를 불러오는 중…", color = MaterialTheme.colorScheme.outline)
                return@SectionCard
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Q${ui.index + 1}/${ui.questions.size}. ${q.question}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    q.choices.forEachIndexed { i, c ->
                        val isAnswer = ui.revealed && i == q.answerIndex
                        Button(
                            onClick = { vm.answerOnScreen(i) },
                            enabled = !ui.touchLocked && !ui.revealed || isAnswer,
                            colors = if (isAnswer) ButtonDefaults.buttonColors(containerColor = Success) else ButtonDefaults.buttonColors(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("${i + 1}. $c", fontSize = 20.sp) }
                    }
                    if (ui.waitingPhones) Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Text("  뒷좌석 폰에서 답을 기다리는 중…")
                    }
                    if (ui.revealed) {
                        Text(
                            ui.answers.entries.joinToString("   ") { (who, a) -> "$who: ${a + 1}번 ${if (a == q.answerIndex) "⭕" else "❌"}" },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::ask) { Text("🔊 문제 내기") }
                        OutlinedButton(onClick = vm::next) { Text("다음 문제") }
                    }
                    if (ui.scores.isNotEmpty()) {
                        Text("점수  " + ui.scores.entries.sortedByDescending { it.value }.joinToString("  ") { "${it.key} ${it.value}점" },
                            fontWeight = FontWeight.Bold)
                    }
                }
                if (ui.touchLocked) QrCode("https://ridemate.web.app/quiz.html?q=${q.id}", size = 140.dp)
            }
        }
    }
}
