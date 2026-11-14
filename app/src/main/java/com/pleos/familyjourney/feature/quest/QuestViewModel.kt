package com.pleos.familyjourney.feature.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleos.familyjourney.data.JourneyMode
import com.pleos.familyjourney.data.JourneyRepository
import com.pleos.familyjourney.data.QuizQuestion
import com.pleos.familyjourney.data.Region
import com.pleos.familyjourney.pleos.NaviClient
import com.pleos.familyjourney.pleos.TtsClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestUiState(
    val routing: Boolean = false,
    val remainingSec: Int? = null,
    val guide: RemainingTimeGuide.Guide? = null,
    val region: Region? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    /** 이름 → 고른 보기 */
    val answers: Map<String, Int> = emptyMap(),
    val waitingPhones: Boolean = false,
    val revealed: Boolean = false,
    val scores: Map<String, Int> = emptyMap(),
    /** 안전 규칙: 주행 중이면 차량 화면 터치 퀴즈 잠금 → 음성/폰으로 참여 */
    val touchLocked: Boolean = false,
    val speedKmh: Int? = null,
) {
    val question get() = questions.getOrNull(index)
}

/** 2. 이동 중: 남은 시간 비유 안내 + 지나가는 지역 퀴즈 */
class QuestViewModel(
    private val repo: JourneyRepository,
    private val navi: NaviClient,
    private val tts: TtsClient,
) : ViewModel() {

    private val _ui = MutableStateFlow(QuestUiState())
    val ui: StateFlow<QuestUiState> = _ui.asStateFlow()

    private var lastAnnouncedHalfHour: Int? = null
    private var phoneJob: Job? = null

    private val mode: JourneyMode get() = repo.modeFor(repo.passengers.value)
    private val firstChild get() = repo.passengers.value.firstOrNull { it.ageGroup.isKid }?.name

    init {
        viewModelScope.launch {
            navi.state.collect { n ->
                val speed = n.speedKmh
                _ui.update {
                    it.copy(
                        routing = n.routing,
                        remainingSec = n.remainingSec,
                        guide = n.remainingSec?.let { s -> RemainingTimeGuide.describe(s, firstChild) },
                        speedKmh = speed,
                        touchLocked = speed != null && speed >= DRIVING_LOCK_KMH,
                    )
                }
                n.location?.let { loc -> repo.regionAt(loc) }?.let { region ->
                    if (region.code != _ui.value.region?.code) onRegionChanged(region, announce = n.routing)
                }
                // 30분마다 남은 시간 자동 안내
                val remain = n.remainingSec
                if (n.routing && remain != null) {
                    val half = remain / 1800
                    if (lastAnnouncedHalfHour != null && half != lastAnnouncedHalfHour) announceRemaining()
                    lastAnnouncedHalfHour = half
                }
            }
        }
    }

    private suspend fun onRegionChanged(region: Region, announce: Boolean) {
        phoneJob?.cancel()
        val qs = repo.quizFor(region, mode)
        _ui.update {
            it.copy(region = region, questions = qs, index = 0, answers = emptyMap(), revealed = false, waitingPhones = false)
        }
        if (announce) tts.speak("${region.name}에 들어왔어! ${region.name.substringAfterLast(" ")} 퀴즈 ${qs.size}문제 준비됐어.")
    }

    fun announceRemaining() {
        _ui.value.guide?.let { tts.speak(it.sentence) }
    }

    /** 문제를 음성으로 낸다. 주행 중이면 화면 대신 동승자 폰으로 답을 받는다. */
    fun ask() {
        val q = _ui.value.question ?: return
        val choices = q.choices.mapIndexed { i, c -> "${i + 1}번 $c" }.joinToString(", ")
        tts.speak("퀴즈! ${q.question} $choices")
        _ui.update { it.copy(answers = emptyMap(), revealed = false) }
        if (_ui.value.touchLocked) waitPhoneAnswers(q)
    }

    private fun waitPhoneAnswers(q: QuizQuestion) {
        phoneJob?.cancel()
        _ui.update { it.copy(waitingPhones = true) }
        phoneJob = viewModelScope.launch {
            val answers = repo.collectPhoneAnswers(q)
            _ui.update { it.copy(answers = it.answers + answers, waitingPhones = false) }
            reveal()
        }
    }

    /** 정차 중에만: 차량 화면을 직접 눌러 답하기 */
    fun answerOnScreen(choice: Int) {
        if (_ui.value.touchLocked || _ui.value.revealed) return
        _ui.update { it.copy(answers = it.answers + ("차량 화면" to choice)) }
        reveal()
    }

    private fun reveal() {
        val s = _ui.value
        val q = s.question ?: return
        val winners = s.answers.filterValues { it == q.answerIndex }.keys
        val scores = s.scores.toMutableMap().apply { winners.forEach { put(it, (get(it) ?: 0) + 1) } }
        _ui.update { it.copy(revealed = true, scores = scores) }
        val who = if (winners.isEmpty()) "아쉽게 아무도 못 맞혔어." else "${winners.joinToString()} 정답!"
        tts.speak("정답은 ${q.answerIndex + 1}번 ${q.choices[q.answerIndex]}. $who ${q.explanation}")
    }

    fun next() {
        phoneJob?.cancel()
        _ui.update {
            val nextIndex = if (it.questions.isEmpty()) 0 else (it.index + 1) % it.questions.size
            it.copy(index = nextIndex, answers = emptyMap(), revealed = false, waitingPhones = false)
        }
    }

    companion object {
        /** 이 속도 이상이면 터치 게임 잠금 (실제: Vehicle SDK 차속) */
        const val DRIVING_LOCK_KMH = 10
    }
}
