package com.pleos.familyjourney.feature.vote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.familyjourney.data.VoteKind
import com.pleos.familyjourney.data.VoteRoom
import com.pleos.familyjourney.ui.Chip
import com.pleos.familyjourney.ui.QrCode
import com.pleos.familyjourney.ui.RatioBar
import com.pleos.familyjourney.ui.SectionCard
import com.pleos.familyjourney.ui.Success

@Composable
fun VoteScreen(vm: VoteViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var spoken by rememberSaveable { mutableStateOf("막국수랑 순두부, 아니면 감자옹심이") }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- 들를까 투표 ---
        SectionCard(
            "들를까 투표",
            Modifier.weight(1f),
            trailing = { if (ui.upcomingWaypoints.isNotEmpty()) Chip("경유 ${ui.upcomingWaypoints.size}/2") },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val room = ui.waypointRoom
                if (room == null) {
                    Text("경로 근처에 볼거리가 잡혔어요: 대관령 양떼목장 🐑", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = vm::openWaypointVote, modifier = Modifier.fillMaxWidth()) { Text("동승자 투표 열기") }
                } else {
                    VoteRoomView(room, onVote = { vm.vote(VoteKind.WAYPOINT, it) }, onClose = { vm.close(VoteKind.WAYPOINT) })
                    ui.waypointResult?.let { Text(it, color = Success, fontWeight = FontWeight.Bold) }
                    if (room.closed) TextButton(onClick = vm::openWaypointVote) { Text("다시 투표") }
                }
            }
        }

        // --- 메뉴 투표 ---
        SectionCard("점심 메뉴 익명 투표", Modifier.weight(1f)) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val room = ui.menuRoom
                if (room == null || room.closed && ui.restaurants.isEmpty() && !ui.searching) {
                    OutlinedTextField(
                        spoken, { spoken = it },
                        label = { Text("🎤 \"글레오, 점심 후보는…\" (음성 인식 결과)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { vm.openMenuVote(spoken) }, modifier = Modifier.fillMaxWidth()) { Text("메뉴 투표 열기") }
                }
                if (room != null) {
                    VoteRoomView(room, onVote = { vm.vote(VoteKind.MENU, it) }, onClose = { vm.close(VoteKind.MENU) })
                }
                if (ui.searching) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Text("  주변 식당 찾는 중…")
                }
                ui.restaurants.forEach { r ->
                    OutlinedButton(onClick = { vm.chooseRestaurant(r) }, modifier = Modifier.fillMaxWidth()) {
                        Text("🍽️ ${r.name} · ${r.menu} · ${"%.1f".format(r.distanceKm)}km  → 경유지로")
                    }
                }
                ui.restaurantResult?.let { Text(it, color = Success, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun VoteRoomView(room: VoteRoom, onVote: (String) -> Unit, onClose: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(room.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${room.votedCount} / ${room.voterCount}명 투표" + if (room.closed) " · 마감" else "")
            val leader = room.winner?.id
            room.options.forEach { o ->
                RatioBar(o.label + if (o.detail.isNotBlank()) "  (${o.detail})" else "", room.tally[o.id] ?: 0, room.voterCount,
                    highlight = room.closed && o.id == leader)
            }
            if (!room.closed) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                room.options.forEach { o -> OutlinedButton(onClick = { onVote(o.id) }) { Text(o.label) } }
                TextButton(onClick = onClose) { Text("마감") }
            }
        }
        if (!room.closed) QrCode(room.joinUrl, size = 130.dp)
    }
}
