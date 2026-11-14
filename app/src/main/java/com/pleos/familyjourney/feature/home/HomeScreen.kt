package com.pleos.familyjourney.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pleos.familyjourney.data.JourneyMode
import com.pleos.familyjourney.data.JourneyRepository
import com.pleos.familyjourney.navigation.Routes
import com.pleos.familyjourney.pleos.NaviState
import com.pleos.familyjourney.ui.Chip
import com.pleos.familyjourney.ui.SectionCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val passengerSummary: String = "아직 탑승자를 정하지 않았어요",
    val mode: JourneyMode? = null,
    val planTitle: String? = null,
    val earnedBadges: Int = 0,
    val totalBadges: Int = 0,
)

class HomeViewModel(repo: JourneyRepository) : ViewModel() {
    val ui: StateFlow<HomeUiState> = combine(repo.passengers, repo.plan, repo.badges) { people, plan, badges ->
        HomeUiState(
            passengerSummary = if (people.isEmpty()) "아직 탑승자를 정하지 않았어요"
            else people.joinToString(" · ") { "${it.name}(${it.ageGroup.label.substringBefore("(")})" },
            mode = if (people.isEmpty()) null else repo.modeFor(people),
            planTitle = plan?.title,
            earnedBadges = badges.count { it.earned },
            totalBadges = badges.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

private data class Stage(val route: String, val number: String, val emoji: String, val title: String, val desc: String)

private val stages = listOf(
    Stage(Routes.PASSENGERS, "1", "👨‍👩‍👧‍👦", "출발", "탑승자 설정 → 맞춤 일정 → 내비로 전송"),
    Stage(Routes.QUEST, "2", "🎯", "이동 중", "남은 시간 '노래 n곡' 안내 · 지역 퀴즈"),
    Stage(Routes.VOTE, "3", "🗳️", "경유지", "들를까 투표 · 점심 메뉴 익명 투표"),
    Stage(Routes.ARRIVAL, "4", "🏅", "도착", "지역 배지 · 구성원별 체크리스트"),
)

/** 메인메뉴: 여정 4단계로 들어가는 입구 */
@Composable
fun HomeScreen(vm: HomeViewModel, navi: NaviState, onOpen: (String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "함께 탄 사람 기준으로 여정을 짜고, 그 여정을 퀘스트처럼 즐겨요",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            stages.forEach { s ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpen(s.route) },
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.emoji, fontSize = 40.sp)
                        Text("${s.number}. ${s.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(s.desc, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("오늘의 여정", Modifier.weight(2f)) {
                Text("탑승자: ${ui.passengerSummary}")
                ui.mode?.let { Chip(it.label) }
                Text("일정: ${ui.planTitle ?: "아직 없음"}")
                Text("내비: ${if (navi.routing) "${navi.destinationName} 안내 중" else if (navi.arrived) "도착" else "대기"}")
            }
            SectionCard("배지 도감", Modifier.weight(1f)) {
                Text("${ui.earnedBadges} / ${ui.totalBadges}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("지역에 도착하면 배지를 모아요")
            }
        }
        Text(
            "데모 모드 · 가짜 내비 + 하드코딩 데이터 · 아래 [▶ 주행] [+10분] 으로 여행을 빨리 감을 수 있어요",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
