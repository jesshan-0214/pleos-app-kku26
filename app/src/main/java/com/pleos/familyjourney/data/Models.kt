package com.pleos.familyjourney.data

/** 데이터 형식. 화면과 Repository, SDK 래퍼가 함께 쓰는 값들. */

enum class AgeGroup(val label: String) {
    INFANT("영유아(0~4)"),
    CHILD("어린이(5~12)"),
    TEEN("청소년"),
    ADULT("성인"),
    SENIOR("고령자");

    val isKid get() = this == INFANT || this == CHILD
}

enum class Seat(val label: String) {
    DRIVER("운전석"),
    PASSENGER("조수석"),
    REAR_LEFT("뒷좌석 왼쪽"),
    REAR_RIGHT("뒷좌석 오른쪽"),
    REAR_CENTER("뒷좌석 가운데"),
}

data class Passenger(
    val id: Long,
    val name: String,
    val ageGroup: AgeGroup,
    val seat: Seat,
    val note: String = "",
)

/** 탑승자 구성이 이후 모든 기능의 모드를 결정한다. 데모는 KIDS 모드 하나만 시연. */
enum class JourneyMode(val label: String) {
    KIDS("어린이 모드"),
    SENIOR("고령자 모드 (로드맵)"),
    ADULTS("어른 모드"),
}

data class GeoPoint(val lat: Double, val lng: Double)

enum class StopType(val label: String, val emoji: String) {
    START("출발", "🏠"),
    REST("휴식", "☕"),
    MEAL("식사", "🍽️"),
    SIGHT("관광", "📸"),
    DESTINATION("도착", "🏁"),
}

data class Place(
    val id: String,
    val name: String,
    val address: String,
    val point: GeoPoint,
    val type: StopType,
)

data class PlanStop(
    val place: Place,
    /** 출발 후 몇 분 뒤에 도착하는지 */
    val etaMin: Int,
    val stayMin: Int,
    val reason: String,
)

data class TripPlan(
    val title: String,
    val mode: JourneyMode,
    val stops: List<PlanStop>,
    val totalDriveMin: Int,
    val summary: String,
) {
    val destination get() = stops.last().place
    val waypoints get() = stops.drop(1).dropLast(1).map { it.place }
}

/**
 * 경로 위 지역. 데모에서는 서울→강릉이 서→동 방향이라 경도 범위로 지역을 판정한다.
 * (실제 구현: FusedLocation 좌표 → 카카오 로컬 coord2regioncode)
 */
data class Region(
    val code: String,
    val name: String,
    val minLng: Double,
    val maxLng: Double,
)

data class QuizQuestion(
    val id: String,
    val regionCode: String,
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
)

enum class VoteKind { WAYPOINT, MENU }

data class VoteOption(
    val id: String,
    val label: String,
    val detail: String = "",
    val place: Place? = null,
    val extraMinutes: Int = 0,
)

data class VoteRoom(
    val id: String,
    val kind: VoteKind,
    val question: String,
    val options: List<VoteOption>,
    val voterCount: Int,
    /** 옵션 id -> 득표 수 */
    val tally: Map<String, Int> = emptyMap(),
    val closed: Boolean = false,
) {
    val votedCount get() = tally.values.sum()
    val winner: VoteOption?
        get() {
            val best = tally.maxByOrNull { it.value } ?: return null
            return options.firstOrNull { it.id == best.key }
        }
    /** 과반 득표한 옵션 (없으면 null) */
    val majority: VoteOption?
        get() {
            val best = tally.maxByOrNull { it.value } ?: return null
            return if (best.value * 2 > voterCount) options.firstOrNull { it.id == best.key } else null
        }
    /** 동승자 폰 웹이 여는 주소 (QR 내용) */
    val joinUrl get() = "https://ridemate.web.app/vote.html?room=$id"
}

data class Restaurant(
    val id: String,
    val name: String,
    val menu: String,
    val distanceKm: Double,
    val place: Place,
)

data class Badge(
    val regionCode: String,
    val regionName: String,
    val emoji: String,
    val feature: String,
    /** 배지 배경색 (ARGB) */
    val color: Long,
    val earnedAt: Long? = null,
) {
    val earned get() = earnedAt != null
}

data class ChecklistItem(
    val id: Long,
    val owner: String,
    val text: String,
    val checked: Boolean = false,
)

/** 받침에 맞는 조사: "서준".josa("은", "는") → "서준은", "지우".josa("은", "는") → "지우는" */
fun String.josa(withBatchim: String, withoutBatchim: String): String {
    val last = lastOrNull() ?: return this
    val hasBatchim = last in '가'..'힣' && (last - '가') % 28 != 0
    return this + if (hasBatchim) withBatchim else withoutBatchim
}
