package com.pleos.familyjourney.navigation

/** 화면 경로 이름. 순서 = 시연 순서 (어린이 모드 4단계) */
object Routes {
    const val HOME = "home"
    const val PASSENGERS = "departure/passengers"
    const val PLANNER = "departure/planner"
    const val QUEST = "quest"
    const val VOTE = "vote"
    const val ARRIVAL = "arrival"

    /** 각 화면의 "다음 단계" 버튼이 가는 곳 */
    val demoOrder = listOf(HOME, PASSENGERS, PLANNER, QUEST, VOTE, ARRIVAL)

    fun next(route: String): String? = demoOrder.getOrNull(demoOrder.indexOf(route) + 1)
}
