package com.pleos.familyjourney.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pleos.familyjourney.FamilyJourneyApp
import com.pleos.familyjourney.feature.arrival.ArrivalScreen
import com.pleos.familyjourney.feature.arrival.ArrivalViewModel
import com.pleos.familyjourney.feature.departure.DepartureViewModel
import com.pleos.familyjourney.feature.departure.PassengerSetupScreen
import com.pleos.familyjourney.feature.departure.PlannerScreen
import com.pleos.familyjourney.feature.home.HomeScreen
import com.pleos.familyjourney.feature.home.HomeViewModel
import com.pleos.familyjourney.feature.quest.QuestScreen
import com.pleos.familyjourney.feature.quest.QuestViewModel
import com.pleos.familyjourney.feature.vote.VoteScreen
import com.pleos.familyjourney.feature.vote.VoteViewModel
import com.pleos.familyjourney.ui.DemoControls
import com.pleos.familyjourney.ui.ScreenFrame

/** 메인메뉴와 기능 화면 연결. 시연 순서: 메인 → 탑승자 → 일정 → 이동 중 → 투표 → 도착 */
@Composable
fun AppNavGraph(app: FamilyJourneyApp) {
    val nav = rememberNavController()
    val navi by app.navi.state.collectAsStateWithLifecycle()
    val subtitle by app.tts.lastSpoken.collectAsStateWithLifecycle()
    val sim = app.simulator
    val running = sim?.running?.collectAsStateWithLifecycle()?.value ?: false

    // 내비가 목적지 도착을 알리면 어느 화면에 있든 도착 화면으로 간다.
    LaunchedEffect(navi.arrived) {
        if (navi.arrived && nav.currentDestination?.route != Routes.ARRIVAL) {
            nav.navigate(Routes.ARRIVAL) { launchSingleTop = true }
        }
    }

    val demo = sim?.let {
        DemoControls(
            running = running,
            onToggleRun = { it.setRunning(!running) },
            onSkip10 = { it.skipMinutes(10) },
            onArrive = { it.jumpToArrival() },
            onReset = {
                app.resetDemo()
                nav.navigate(Routes.HOME) { popUpTo(0) }
            },
        )
    }

    @Composable
    fun Frame(route: String, title: String, step: String?, content: @Composable () -> Unit) {
        val next = Routes.next(route)
        ScreenFrame(
            title = title,
            step = step,
            navi = navi,
            subtitle = subtitle,
            demo = demo,
            onBack = if (route == Routes.HOME) null else ({ nav.popBackStack(Routes.HOME, inclusive = false) }),
            onNext = next?.takeIf { route != Routes.HOME }?.let { { nav.go(it) } },
            content = content,
        )
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel { HomeViewModel(app.repository) }
            Frame(Routes.HOME, "라이드메이트", null) {
                HomeScreen(vm, navi, onOpen = { nav.go(it) })
            }
        }
        composable(Routes.PASSENGERS) {
            val vm: DepartureViewModel = viewModel { DepartureViewModel(app.repository, app.navi, app.tts) }
            Frame(Routes.PASSENGERS, "누가 탔나요?", "1. 출발") { PassengerSetupScreen(vm) }
        }
        composable(Routes.PLANNER) {
            val vm: DepartureViewModel = viewModel { DepartureViewModel(app.repository, app.navi, app.tts) }
            Frame(Routes.PLANNER, "맞춤 여정 플래너", "1. 출발") { PlannerScreen(vm) }
        }
        composable(Routes.QUEST) {
            val vm: QuestViewModel = viewModel { QuestViewModel(app.repository, app.navi, app.tts) }
            Frame(Routes.QUEST, "이동 중 퀘스트", "2. 이동 중") { QuestScreen(vm) }
        }
        composable(Routes.VOTE) {
            val vm: VoteViewModel = viewModel { VoteViewModel(app.repository, app.navi, app.tts) }
            Frame(Routes.VOTE, "들를까 투표", "3. 경유지") { VoteScreen(vm) }
        }
        composable(Routes.ARRIVAL) {
            val vm: ArrivalViewModel = viewModel { ArrivalViewModel(app.repository, app.navi, app.tts) }
            Frame(Routes.ARRIVAL, "도착! 보상과 체크리스트", "4. 도착") { ArrivalScreen(vm) }
        }
    }
}

private fun NavHostController.go(route: String) = navigate(route) { launchSingleTop = true }
