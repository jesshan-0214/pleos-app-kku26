package com.pleos.familyjourney.data

import kotlinx.coroutines.flow.StateFlow

/**
 * 화면(ViewModel)이 데이터를 받는 유일한 창구.
 * 지금은 DemoJourneyRepository(하드코딩)가 구현하고, 나중에 TourAPI·Gleo LLM·카카오 로컬·
 * Firebase·Room을 쓰는 실제 구현으로 바꿔 끼운다. 화면 코드는 바뀌지 않는다.
 */
interface JourneyRepository {

    // --- 1. 출발: 탑승자 설정 (실제: Room) ---
    val passengers: StateFlow<List<Passenger>>
    fun loadSavedPassengers()
    fun addPassenger(name: String, ageGroup: AgeGroup, seat: Seat, note: String)
    fun removePassenger(id: Long)
    fun clearPassengers()
    fun modeFor(passengers: List<Passenger>): JourneyMode

    // --- 1. 출발: 일정 작성 (실제: TourAPI 후보 + Gleo LLM) ---
    val plan: StateFlow<TripPlan?>
    suspend fun createPlan(passengers: List<Passenger>): TripPlan

    // --- 2. 이동 중: 지역 감지와 퀴즈 (실제: 카카오 로컬 + TourAPI + LLM) ---
    fun regionAt(point: GeoPoint): Region?
    suspend fun quizFor(region: Region, mode: JourneyMode): List<QuizQuestion>
    /** 동승자 폰에서 들어오는 퀴즈 답 (이름 → 고른 보기). 실제: Firebase quizAnswers */
    suspend fun collectPhoneAnswers(question: QuizQuestion): Map<String, Int>

    // --- 3. 경유지: 투표 (실제: Firebase 투표방 + 폰 웹) ---
    val voteRooms: StateFlow<Map<VoteKind, VoteRoom>>
    fun openWaypointVote(): VoteRoom
    /** 음성으로 말한 메뉴 후보 문장 → 목록 정리(실제: STT + LLM) → 투표방 */
    fun openMenuVote(spokenCandidates: String): VoteRoom
    fun castVote(kind: VoteKind, optionId: String)
    fun closeVote(kind: VoteKind)
    suspend fun findRestaurants(menu: String, near: GeoPoint?): List<Restaurant>

    // --- 4. 도착: 배지와 체크리스트 (실제: Room + Firebase) ---
    val badges: StateFlow<List<Badge>>
    fun awardBadge(regionCode: String): Badge?
    val checklist: StateFlow<List<ChecklistItem>>
    /** "서준이 애착 인형 챙기기" 같은 말 → 구성원별 분류 (실제: STT + LLM) */
    fun addChecklistFromSpeech(sentence: String)
    fun toggleChecklist(id: Long)
    fun removeChecklist(id: Long)

    /** 시연을 처음부터 다시 */
    fun resetDemo()
}
