package com.pleos.familyjourney.feature.departure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.pleos.familyjourney.data.AgeGroup
import com.pleos.familyjourney.data.Passenger
import com.pleos.familyjourney.data.Seat
import com.pleos.familyjourney.ui.Chip
import com.pleos.familyjourney.ui.SectionCard

/** 탑승자 설정: 이름, 나이대, 좌석, 특이사항. 탑승자 구성이 이후 모든 기능의 모드를 결정한다. */
@Composable
fun PassengerSetupScreen(vm: DepartureViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(
            "탑승자 ${ui.passengers.size}명",
            Modifier.weight(1.2f),
            trailing = { ui.mode?.let { Chip("→ ${it.label}") } },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::loadSaved) { Text("지난번 탑승자 불러오기") }
                TextButton(onClick = vm::clear) { Text("모두 지우기") }
            }
            if (ui.passengers.isEmpty()) {
                Text("탑승자를 추가하거나 지난번 탑승자를 불러오세요.", color = MaterialTheme.colorScheme.outline)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.passengers, key = { it.id }) { PassengerRow(it, onRemove = { vm.remove(it.id) }) }
            }
        }
        AddPassengerForm(Modifier.weight(1f), onAdd = vm::add)
    }
}

@Composable
private fun PassengerRow(p: Passenger, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(if (p.ageGroup.isKid) "🧒" else if (p.ageGroup.name == "SENIOR") "👵" else "🧑", style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                listOf(p.ageGroup.label, p.seat.label, p.note).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onRemove) { Text("삭제") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPassengerForm(modifier: Modifier, onAdd: (String, AgeGroup, Seat, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf(AgeGroup.CHILD) }
    var seat by rememberSaveable { mutableStateOf(Seat.REAR_LEFT) }

    SectionCard("탑승자 추가", modifier) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("나이대", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeGroup.entries.forEach { FilterChip(age == it, { age = it }, label = { Text(it.label) }) }
            }
            Text("좌석", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Seat.entries.forEach { FilterChip(seat == it, { seat = it }, label = { Text(it.label) }) }
            }
            OutlinedTextField(note, { note = it }, label = { Text("특이사항 (멀미, 카시트 등)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onAdd(name, age, seat, note); name = ""; note = "" },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("추가") }
        }
    }
}
