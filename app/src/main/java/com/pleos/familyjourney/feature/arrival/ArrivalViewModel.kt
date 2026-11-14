package com.pleos.familyjourney.feature.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.familyjourney.data.Badge
import com.pleos.familyjourney.data.ChecklistItem
import com.pleos.familyjourney.data.JourneyRepository
import com.pleos.familyjourney.data.josa
import com.pleos.familyjourney.pleos.NaviClient
import com.pleos.familyjourney.pleos.TtsClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArrivalUiState(
    val arrived: Boolean = false,
    val destinationName: String? = null,
    val newBadge: Badge? = null,
    val badges: List<Badge> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
) {
    val byOwner: Map<String, List<ChecklistItem>> get() = checklist.groupBy { it.owner }
}

/** 4. 도착: 도착 판정 → 지역 배지 지급 → 구성원별 체크리스트 음성 안내 */
class ArrivalViewModel(
    private val repo: JourneyRepository,
    private val navi: NaviClient,
    private val tts: TtsClient,
) : ViewModel() {

    private val newBadge = MutableStateFlow(lastAwarded)

    val ui: StateFlow<ArrivalUiState> =
        combine(navi.state, newBadge, repo.badges, repo.checklist) { n, badge, badges, items ->
            ArrivalUiState(n.arrived, n.destinationName, badge, badges, items)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArrivalUiState())

    init {
        viewModelScope.launch {
            navi.state.collect { n ->
                if (!n.arrived) {
                    arrivalHandled = false
                    lastAwarded = null
                    newBadge.value = null
                    return@collect
                }
                if (arrivalHandled) return@collect
                arrivalHandled = true
                // 도착 판정은 내비의 onDestinationArrived 이벤트를 그대로 쓴다 (거리 계산 불필요)
                val badge = n.location?.let { repo.regionAt(it) }?.let { repo.awardBadge(it.code) }
                lastAwarded = badge
                newBadge.value = badge
                val head = if (badge != null) "도착! ${badge.regionName} 배지를 얻었어요. " else "도착했어요! "
                tts.speak(head + checklistSentence())
            }
        }
    }

    private fun checklistSentence(): String {
        val left = repo.checklist.value.filterNot { it.checked }
        if (left.isEmpty()) return "챙길 물건은 다 확인했어요."
        return "내리기 전에 확인해요. " + left.joinToString(", ") {
            if (it.owner == "공통") it.text else "${it.owner.josa("은", "는")} ${it.text}"
        } + "."
    }

    fun remind() = tts.speak(checklistSentence())

    fun addFromSpeech(sentence: String) = repo.addChecklistFromSpeech(sentence)

    fun toggle(id: Long) = repo.toggleChecklist(id)

    fun remove(id: Long) = repo.removeChecklist(id)

    companion object {
        // 화면을 나갔다 와도 같은 도착을 두 번 처리하지 않게
        private var arrivalHandled = false
        private var lastAwarded: Badge? = null
    }
}
