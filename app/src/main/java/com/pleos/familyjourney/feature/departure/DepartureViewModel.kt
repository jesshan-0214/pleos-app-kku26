package com.pleos.familyjourney.feature.departure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.familyjourney.data.AgeGroup
import com.pleos.familyjourney.data.JourneyMode
import com.pleos.familyjourney.data.JourneyRepository
import com.pleos.familyjourney.data.Passenger
import com.pleos.familyjourney.data.Seat
import com.pleos.familyjourney.data.TripPlan
import com.pleos.familyjourney.pleos.NaviClient
import com.pleos.familyjourney.pleos.TtsClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DepartureUiState(
    val passengers: List<Passenger> = emptyList(),
    val mode: JourneyMode? = null,
    val plan: TripPlan? = null,
    val planning: Boolean = false,
    val routeSent: Boolean = false,
)

/** 1. 출발: 탑승자 설정과 일정 작성. 화면 → ViewModel → Repository / NaviClient */
class DepartureViewModel(
    private val repo: JourneyRepository,
    private val navi: NaviClient,
    private val tts: TtsClient,
) : ViewModel() {

    private val planning = MutableStateFlow(false)

    val ui: StateFlow<DepartureUiState> =
        combine(repo.passengers, repo.plan, planning, navi.state) { people, plan, busy, naviState ->
            DepartureUiState(
                passengers = people,
                mode = if (people.isEmpty()) null else repo.modeFor(people),
                plan = plan,
                planning = busy,
                routeSent = naviState.routing || naviState.arrived,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DepartureUiState())

    fun loadSaved() = repo.loadSavedPassengers()

    fun add(name: String, age: AgeGroup, seat: Seat, note: String) {
        if (name.isNotBlank()) repo.addPassenger(name, age, seat, note)
    }

    fun remove(id: Long) = repo.removePassenger(id)

    fun clear() = repo.clearPassengers()

    fun createPlan() {
        val people = repo.passengers.value
        if (people.isEmpty() || planning.value) return
        viewModelScope.launch {
            planning.value = true
            try {
                val plan = repo.createPlan(people)
                tts.speak("${plan.mode.label}로 일정을 짰어요. ${plan.summary}")
            } finally {
                planning.value = false
            }
        }
    }

    fun sendToNavi() {
        val plan = repo.plan.value ?: return
        navi.requestRoute(plan.destination, plan.waypoints)
        val via = plan.waypoints.joinToString { it.name }
        tts.speak("${plan.destination.name}까지 안내를 시작할게요." + if (via.isNotEmpty()) " $via 에서 쉬어 가요." else "")
    }
}
