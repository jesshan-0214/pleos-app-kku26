package com.pleos.familyjourney.feature.vote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.familyjourney.data.JourneyRepository
import com.pleos.familyjourney.data.Restaurant
import com.pleos.familyjourney.data.VoteKind
import com.pleos.familyjourney.data.VoteRoom
import com.pleos.familyjourney.pleos.NaviClient
import com.pleos.familyjourney.pleos.TtsClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoteUiState(
    val waypointRoom: VoteRoom? = null,
    val waypointResult: String? = null,
    val menuRoom: VoteRoom? = null,
    val restaurants: List<Restaurant> = emptyList(),
    val searching: Boolean = false,
    val restaurantResult: String? = null,
    val upcomingWaypoints: List<String> = emptyList(),
)

/** 3. 경유지: 들를까 투표(과반이면 경유지 추가) + 점심 메뉴 익명 투표 → 주변 식당 검색 */
class VoteViewModel(
    private val repo: JourneyRepository,
    private val navi: NaviClient,
    private val tts: TtsClient,
) : ViewModel() {

    private val _ui = MutableStateFlow(VoteUiState())
    val ui: StateFlow<VoteUiState> = _ui.asStateFlow()

    companion object {
        /** 결과 처리를 이미 끝낸 투표방 id (화면을 다시 열어도 경유지를 두 번 넣지 않게) */
        private val handled = mutableSetOf<String>()
    }

    init {
        viewModelScope.launch {
            repo.voteRooms.collect { rooms ->
                val wp = rooms[VoteKind.WAYPOINT]
                val menu = rooms[VoteKind.MENU]
                _ui.update { it.copy(waypointRoom = wp, menuRoom = menu) }
                if (wp != null && wp.closed && handled.add(wp.id)) onWaypointVoteClosed(wp)
                if (menu != null && menu.closed && handled.add(menu.id)) onMenuVoteClosed(menu)
            }
        }
        viewModelScope.launch {
            navi.state.collect { n -> _ui.update { it.copy(upcomingWaypoints = n.upcomingWaypoints) } }
        }
    }

    fun openWaypointVote() {
        _ui.update { it.copy(waypointResult = null) }
        val room = repo.openWaypointVote()
        tts.speak("${room.question} 화면의 QR을 찍고 폰으로 투표해 줘.")
    }

    fun openMenuVote(spoken: String) {
        _ui.update { it.copy(restaurants = emptyList(), restaurantResult = null) }
        val room = repo.openMenuVote(spoken)
        tts.speak("점심 메뉴 투표! ${room.options.joinToString { it.label }} 중에 골라 줘. 누가 뭘 골랐는지는 비밀이야.")
    }

    /** 차량 화면에서 누른 한 표 (조수석/정차 중) */
    fun vote(kind: VoteKind, optionId: String) = repo.castVote(kind, optionId)

    fun close(kind: VoteKind) = repo.closeVote(kind)

    private fun onWaypointVoteClosed(room: VoteRoom) {
        val winner = room.majority
        val place = winner?.place
        val message = when {
            place == null -> "과반이 안 돼서 그냥 원래 길로 갈게요."
            navi.addWaypoint(place) -> "과반 찬성! ${place.name}을 경유지로 추가하고 경로를 다시 찾았어요. 약 ${winner.extraMinutes}분 더 걸려요."
            else -> "경유지가 이미 ${NaviClient.MAX_WAYPOINTS}곳이라 더 넣을 수 없어요. 앞 경유지를 지난 뒤 다시 해 보세요."
        }
        _ui.update { it.copy(waypointResult = message) }
        tts.speak(message)
    }

    private fun onMenuVoteClosed(room: VoteRoom) {
        val menu = room.winner?.label ?: return
        _ui.update { it.copy(searching = true) }
        tts.speak("투표 결과 점심은 $menu! 근처 $menu 집을 찾아볼게요.")
        viewModelScope.launch {
            val list = repo.findRestaurants(menu, navi.state.value.location)
            _ui.update { it.copy(restaurants = list, searching = false) }
        }
    }

    fun chooseRestaurant(r: Restaurant) {
        val ok = navi.addWaypoint(r.place)
        val message = if (ok) "${r.name}을 경유지로 넣었어요. 맛있게 먹고 가요!"
        else "경유지가 꽉 찼어요(최대 ${NaviClient.MAX_WAYPOINTS}곳). 앞 경유지를 지난 뒤 다시 눌러 주세요."
        _ui.update { it.copy(restaurantResult = message) }
        tts.speak(message)
    }
}
