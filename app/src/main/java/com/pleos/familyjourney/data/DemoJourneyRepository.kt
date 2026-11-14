package com.pleos.familyjourney.data

import com.pleos.familyjourney.pleos.FakeNaviClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 하드코딩 데이터 전부. 시나리오: "한 가족, 한 여정" — 아이 둘과 함께 서울 → 강릉 경포해변 (약 2시간 30분).
 * 좌표는 시연용 근사값이다.
 */
class DemoJourneyRepository(private val scope: CoroutineScope) : JourneyRepository {

    // =========================================================================================
    // 하드코딩 데이터
    // =========================================================================================

    private object Places {
        val SEOUL = Place("start", "서울시청 (출발)", "서울 중구 세종대로 110", GeoPoint(37.5663, 126.9779), StopType.START)
        val HOENGSEONG_REST = Place("rest_hs", "횡성휴게소", "강원 횡성군 영동고속도로 (강릉방향)", GeoPoint(37.4866, 128.0425), StopType.REST)
        val PYEONGCHANG_REST = Place("rest_pc", "평창휴게소", "강원 평창군 영동고속도로 (강릉방향)", GeoPoint(37.5640, 128.3910), StopType.REST)
        val SHEEP_FARM = Place("sheep", "대관령 양떼목장", "강원 평창군 대관령면", GeoPoint(37.6879, 128.7524), StopType.SIGHT)
        val GYEONGPO = Place("gyeongpo", "경포해변", "강원 강릉시 창해로", GeoPoint(37.8055, 128.9080), StopType.DESTINATION)
    }

    private val savedPassengers = listOf(
        Passenger(1, "아빠", AgeGroup.ADULT, Seat.DRIVER),
        Passenger(2, "엄마", AgeGroup.ADULT, Seat.PASSENGER),
        Passenger(3, "지우", AgeGroup.CHILD, Seat.REAR_LEFT, "8살, 멀미 조금"),
        Passenger(4, "서준", AgeGroup.INFANT, Seat.REAR_RIGHT, "4살, 카시트, 화장실 자주"),
    )

    private val regions = listOf(
        Region("seoul", "서울", 0.0, 127.18),
        Region("gyeonggi", "경기 하남·양평", 127.18, 127.80),
        Region("wonju", "강원 원주", 127.80, 127.98),
        Region("hoengseong", "강원 횡성", 127.98, 128.33),
        Region("pyeongchang", "강원 평창", 128.33, 128.76),
        Region("gangneung", "강원 강릉", 128.76, 180.0),
    )

    private val quizzes = listOf(
        QuizQuestion("seoul1", "seoul", "서울 한가운데를 가로지르는 큰 강의 이름은?", listOf("낙동강", "한강", "금강"), 1,
            "한강은 서울을 남쪽과 북쪽으로 나누며 흘러요."),
        QuizQuestion("seoul2", "seoul", "남산 꼭대기에서 서울을 내려다볼 수 있는 탑은?", listOf("N서울타워", "에펠탑", "피사의 사탑"), 0,
            "N서울타워는 남산 위에 있어요. 에펠탑은 프랑스 파리에 있어요."),
        QuizQuestion("gg1", "gyeonggi", "양평 두물머리는 두 강물이 만나는 곳이에요. 어떤 두 강일까요?",
            listOf("남한강과 북한강", "낙동강과 영산강", "섬진강과 금강"), 0,
            "남한강과 북한강이 만나 한강이 돼요. 그래서 '두 물이 만나는 머리'예요."),
        QuizQuestion("gg2", "gyeonggi", "경기도는 어느 도시를 빙 둘러싸고 있을까요?", listOf("부산", "서울", "제주"), 1,
            "경기도는 서울을 동그랗게 감싸고 있어요."),
        QuizQuestion("wj1", "wonju", "원주 치악산에는 어떤 동물이 은혜를 갚은 옛이야기가 있을까요?", listOf("꿩", "호랑이", "토끼"), 0,
            "구해 준 선비를 위해 꿩이 종을 울려 은혜를 갚았다는 이야기가 있어요."),
        QuizQuestion("wj2", "wonju", "강원도의 도청이 있는 도시는?", listOf("원주", "춘천", "강릉"), 1,
            "강원특별자치도청은 춘천에 있어요."),
        QuizQuestion("hs1", "hoengseong", "횡성은 어떤 음식으로 유명할까요?", listOf("한우", "귤", "굴비"), 0,
            "횡성 한우는 전국에서 유명해요."),
        QuizQuestion("hs2", "hoengseong", "소의 위(밥통)는 몇 개로 나뉘어 있을까요?", listOf("1개", "2개", "4개"), 2,
            "소는 위가 4개라서 먹은 풀을 되새김질해요."),
        QuizQuestion("pc1", "pyeongchang", "2018년에 평창에서 열린 세계 스포츠 대회는?", listOf("동계올림픽", "월드컵", "하계올림픽"), 0,
            "평창 동계올림픽에서는 눈과 얼음 위에서 경기를 했어요."),
        QuizQuestion("pc2", "pyeongchang", "대관령 양떼목장의 양은 어떻게 울까요?", listOf("음매~", "메에~", "꼬끼오"), 1,
            "양은 '메에~' 하고 울어요. '음매'는 소예요."),
        QuizQuestion("gn1", "gangneung", "강릉에 있는 유명한 바다 해변은?", listOf("경포해변", "해운대", "협재해변"), 0,
            "경포해변은 강릉에 있어요. 해운대는 부산, 협재는 제주예요."),
        QuizQuestion("gn2", "gangneung", "강릉 초당마을에서 바닷물로 만드는 유명한 음식은?", listOf("초당순두부", "떡볶이", "짜장면"), 0,
            "바닷물을 간수로 써서 두부를 만들어요."),
    )

    private val restaurantData = listOf(
        Restaurant("r1", "초당 손두부집", "순두부", 1.2,
            Place("r1", "초당 손두부집", "강원 강릉시 초당동", GeoPoint(37.7917, 128.9147), StopType.MEAL)),
        Restaurant("r2", "경포 순두부마을", "순두부", 2.0,
            Place("r2", "경포 순두부마을", "강원 강릉시 초당동", GeoPoint(37.7893, 128.9152), StopType.MEAL)),
        Restaurant("r3", "대관령 막국수집", "막국수", 3.4,
            Place("r3", "대관령 막국수집", "강원 평창군 대관령면", GeoPoint(37.6820, 128.7400), StopType.MEAL)),
        Restaurant("r4", "강릉 감자옹심이집", "감자옹심이", 2.7,
            Place("r4", "강릉 감자옹심이집", "강원 강릉시 임당동", GeoPoint(37.7560, 128.8990), StopType.MEAL)),
    )

    private val initialBadges = listOf(
        Badge("seoul", "서울", "🏙️", "N서울타워", 0xFF5C6BC0, earnedAt = 1_730_000_000_000), // 지난 여행에서 획득
        Badge("gyeonggi", "양평", "🚲", "두물머리", 0xFF66BB6A),
        Badge("wonju", "원주", "⛰️", "치악산", 0xFF8D6E63),
        Badge("hoengseong", "횡성", "🐂", "횡성 한우", 0xFFEF5350),
        Badge("pyeongchang", "평창", "🐑", "양떼목장", 0xFF9CCC65),
        Badge("gangneung", "강릉", "🌊", "경포 바다", 0xFF29B6F6),
        Badge("busan", "부산", "🌉", "광안대교", 0xFF26A69A),
        Badge("gyeongju", "경주", "🏛️", "첨성대", 0xFFFFA726),
        Badge("jeonju", "전주", "🥢", "비빔밥", 0xFFAB47BC),
        Badge("jeju", "제주", "🍊", "감귤", 0xFFFF7043),
    )

    private fun initialChecklist() = listOf(
        ChecklistItem(1, "엄마", "선크림이랑 모자"),
        ChecklistItem(2, "아빠", "트렁크에 있는 돗자리"),
        ChecklistItem(3, "지우", "물놀이 가방"),
        ChecklistItem(4, "서준", "애착 인형"),
        ChecklistItem(5, "공통", "휴대폰 충전기"),
    )

    // =========================================================================================
    // 상태
    // =========================================================================================

    private val _passengers = MutableStateFlow<List<Passenger>>(emptyList())
    override val passengers: StateFlow<List<Passenger>> = _passengers.asStateFlow()

    private val _plan = MutableStateFlow<TripPlan?>(null)
    override val plan: StateFlow<TripPlan?> = _plan.asStateFlow()

    private val _voteRooms = MutableStateFlow<Map<VoteKind, VoteRoom>>(emptyMap())
    override val voteRooms: StateFlow<Map<VoteKind, VoteRoom>> = _voteRooms.asStateFlow()

    private val _badges = MutableStateFlow(initialBadges)
    override val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    private val _checklist = MutableStateFlow(initialChecklist())
    override val checklist: StateFlow<List<ChecklistItem>> = _checklist.asStateFlow()

    private var nextId = 100L
    private val voteJobs = mutableMapOf<VoteKind, Job>()

    // =========================================================================================
    // 1. 출발
    // =========================================================================================

    override fun loadSavedPassengers() {
        _passengers.value = savedPassengers
    }

    override fun addPassenger(name: String, ageGroup: AgeGroup, seat: Seat, note: String) {
        _passengers.update { it + Passenger(nextId++, name.trim(), ageGroup, seat, note.trim()) }
    }

    override fun removePassenger(id: Long) {
        _passengers.update { list -> list.filterNot { it.id == id } }
    }

    override fun clearPassengers() {
        _passengers.value = emptyList()
    }

    override fun modeFor(passengers: List<Passenger>): JourneyMode = when {
        passengers.any { it.ageGroup.isKid } -> JourneyMode.KIDS
        passengers.any { it.ageGroup == AgeGroup.SENIOR } -> JourneyMode.SENIOR
        else -> JourneyMode.ADULTS
    }

    /** 가짜 LLM: 탑승자 구성에 따라 휴식 간격과 경유지를 바꾼다. */
    override suspend fun createPlan(passengers: List<Passenger>): TripPlan {
        delay(1200) // LLM 응답 대기 흉내
        val mode = modeFor(passengers)
        val kids = passengers.filter { it.ageGroup.isKid }
        val (rest, reason) = when (mode) {
            JourneyMode.KIDS -> Places.HOENGSEONG_REST to
                "${kids.joinToString { it.name }} 동승 → 1시간 10분 안에 화장실·간식 휴식"
            JourneyMode.SENIOR -> Places.HOENGSEONG_REST to "고령 동승자 → 1시간마다 스트레칭 휴식"
            JourneyMode.ADULTS -> Places.PYEONGCHANG_REST to "2시간 연속 운전 전 졸음 예방 휴식"
        }
        val restStay = if (mode == JourneyMode.ADULTS) 10 else 20
        val toRest = driveMin(Places.SEOUL.point, rest.point)
        val toDest = driveMin(rest.point, Places.GYEONGPO.point)
        val stops = listOf(
            PlanStop(Places.SEOUL, 0, 0, "${passengers.size}명 출발"),
            PlanStop(rest, toRest, restStay, reason),
            PlanStop(Places.GYEONGPO, toRest + restStay + toDest, 0, "목적지 · 도착하면 강릉 배지"),
        )
        val summary = when (mode) {
            JourneyMode.KIDS ->
                "아이 ${kids.size}명 기준으로 짰어요. 이동 중엔 지역 퀴즈, 점심은 메뉴 투표로 정하고, " +
                    "평창 근처 볼거리는 '들를까 투표'로 추가할 수 있어요."
            JourneyMode.SENIOR -> "고령자 모드는 로드맵 기능이에요. 휴식을 자주 넣었어요."
            JourneyMode.ADULTS -> "어른만 탔으니 휴식은 한 번만 넣었어요."
        }
        return TripPlan("서울 → 강릉 가족 여행", mode, stops, toRest + toDest, summary).also { _plan.value = it }
    }

    private fun driveMin(a: GeoPoint, b: GeoPoint) = (FakeNaviClient.roadKm(a, b) / 85.0 * 60).toInt()

    // =========================================================================================
    // 2. 이동 중
    // =========================================================================================

    override fun regionAt(point: GeoPoint): Region? = regions.firstOrNull { point.lng >= it.minLng && point.lng < it.maxLng }

    override suspend fun quizFor(region: Region, mode: JourneyMode): List<QuizQuestion> {
        delay(400)
        return quizzes.filter { it.regionCode == region.code }
    }

    override suspend fun collectPhoneAnswers(question: QuizQuestion): Map<String, Int> {
        delay(3000) // 뒷좌석 아이들이 폰으로 답하는 시간
        val riders = _passengers.value.filter { it.seat != Seat.DRIVER && it.seat != Seat.PASSENGER }
            .ifEmpty { _passengers.value.filter { it.seat != Seat.DRIVER } }
        return riders.mapIndexed { i, p ->
            // 첫째는 정답, 나머지는 문제마다 맞았다 틀렸다
            val correct = i == 0 || (question.id.hashCode() + i) % 2 == 0
            p.name to if (correct) question.answerIndex else (question.answerIndex + 1) % question.choices.size
        }.toMap()
    }

    // =========================================================================================
    // 3. 경유지 투표
    // =========================================================================================

    override fun openWaypointVote(): VoteRoom {
        val place = Places.SHEEP_FARM
        val room = VoteRoom(
            id = "wp-${System.currentTimeMillis() % 100000}",
            kind = VoteKind.WAYPOINT,
            question = "근처에 ${place.name}이 있어요. 25분 더 걸려요. 들를까요?",
            options = listOf(
                VoteOption("yes", "들르자! 🐑", "+25분 (이동 10분 + 구경 15분)", place, 25),
                VoteOption("no", "그냥 가자", "도착 시간 그대로"),
            ),
            voterCount = _passengers.value.size.coerceAtLeast(1),
        )
        startRoom(room, pattern = listOf(0, 0, 1, 0)) // 지우 찬성, 서준 찬성, 엄마 반대, 아빠(음성) 찬성
        return room
    }

    override fun openMenuVote(spokenCandidates: String): VoteRoom {
        // 가짜 LLM: "막국수랑 순두부, 아니면 감자옹심이" → [막국수, 순두부, 감자옹심이]
        val menus = spokenCandidates
            .split(",", "랑 ", "이랑 ", "아니면", "하고 ", "또는", "/")
            .map { it.trim().removeSuffix("이요").removeSuffix("요").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("막국수", "순두부", "감자옹심이") }
        val room = VoteRoom(
            id = "menu-${System.currentTimeMillis() % 100000}",
            kind = VoteKind.MENU,
            question = "점심 뭐 먹을까? (익명 투표)",
            options = menus.mapIndexed { i, m -> VoteOption("m$i", m) },
            voterCount = _passengers.value.size.coerceAtLeast(1),
        )
        startRoom(room, pattern = listOf(1, 2, 1, 0))
        return room
    }

    /** 동승자 폰 투표가 몇 초 간격으로 들어오는 것을 흉내 낸다. 실제: Firebase 실시간 리스너 */
    private fun startRoom(room: VoteRoom, pattern: List<Int>) {
        voteJobs[room.kind]?.cancel()
        _voteRooms.update { it + (room.kind to room) }
        voteJobs[room.kind] = scope.launch {
            repeat(room.voterCount) { i ->
                delay(1200L + (i % 2) * 600L) // 한 표씩 1~2초 간격
                val current = _voteRooms.value[room.kind] ?: return@launch
                if (current.closed || current.id != room.id) return@launch
                val option = room.options[pattern[i % pattern.size] % room.options.size]
                castVote(room.kind, option.id)
            }
        }
    }

    override fun castVote(kind: VoteKind, optionId: String) {
        _voteRooms.update { rooms ->
            val room = rooms[kind] ?: return@update rooms
            if (room.closed) return@update rooms
            val tally = room.tally + (optionId to (room.tally[optionId] ?: 0) + 1)
            val updated = room.copy(tally = tally, closed = tally.values.sum() >= room.voterCount)
            rooms + (kind to updated)
        }
    }

    override fun closeVote(kind: VoteKind) {
        voteJobs[kind]?.cancel()
        _voteRooms.update { rooms -> rooms[kind]?.let { rooms + (kind to it.copy(closed = true)) } ?: rooms }
    }

    override suspend fun findRestaurants(menu: String, near: GeoPoint?): List<Restaurant> {
        delay(800) // 카카오 로컬 키워드 검색 흉내
        val hits = restaurantData.filter { menu.contains(it.menu) || it.menu.contains(menu) }
        if (hits.isNotEmpty()) return hits
        val p = near ?: Places.GYEONGPO.point
        return listOf(
            Restaurant("g1", "근처 $menu 맛집", menu, 1.5,
                Place("g1", "근처 $menu 맛집", "현재 위치 주변", GeoPoint(p.lat + 0.01, p.lng + 0.01), StopType.MEAL)),
        )
    }

    // =========================================================================================
    // 4. 도착
    // =========================================================================================

    override fun awardBadge(regionCode: String): Badge? {
        var awarded: Badge? = null
        _badges.update { list ->
            list.map {
                if (it.regionCode == regionCode && !it.earned) it.copy(earnedAt = System.currentTimeMillis()).also { b -> awarded = b }
                else it
            }
        }
        return awarded
    }

    override fun addChecklistFromSpeech(sentence: String) {
        val text = sentence.trim()
        if (text.isEmpty()) return
        // 가짜 LLM: 문장에 들어 있는 탑승자 이름으로 분류
        val owner = _passengers.value.firstOrNull { text.contains(it.name) }?.name ?: "공통"
        val item = if (owner == "공통") text else text
            .replaceFirst(Regex("^${Regex.escape(owner)}(이는|이가|이|는|가|도)?\\s*"), "")
            .ifBlank { text }
        _checklist.update { it + ChecklistItem(nextId++, owner, item) }
    }

    override fun toggleChecklist(id: Long) {
        _checklist.update { list -> list.map { if (it.id == id) it.copy(checked = !it.checked) else it } }
    }

    override fun removeChecklist(id: Long) {
        _checklist.update { list -> list.filterNot { it.id == id } }
    }

    override fun resetDemo() {
        voteJobs.values.forEach { it.cancel() }
        voteJobs.clear()
        _plan.value = null
        _voteRooms.value = emptyMap()
        _badges.value = initialBadges
        _checklist.value = initialChecklist()
    }
}
