package com.pleos.familyjourney

import android.app.Application
import com.pleos.familyjourney.data.DemoJourneyRepository
import com.pleos.familyjourney.data.JourneyRepository
import com.pleos.familyjourney.pleos.DemoTtsClient
import com.pleos.familyjourney.pleos.FakeNaviClient
import com.pleos.familyjourney.pleos.NaviClient
import com.pleos.familyjourney.pleos.TripSimulator
import com.pleos.familyjourney.pleos.TtsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 데모 ↔ 실제 구현을 바꾸는 유일한 곳.
 * 화면/ViewModel은 여기서 만든 공용 객체(인터페이스)만 받아 쓴다. (Hilt 없이 직접 생성)
 * 실제 구현(Pleos SDK, TourAPI·LLM·Firebase·Room)이 생기면 onCreate 의 세 줄만 바꾼다.
 */
class FamilyJourneyApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var navi: NaviClient
        private set
    lateinit var tts: TtsClient
        private set
    lateinit var repository: JourneyRepository
        private set

    /** 시연용 빨리감기. 실제 내비로 바꾸면 null */
    var simulator: TripSimulator? = null
        private set

    override fun onCreate() {
        super.onCreate()
        val fakeNavi = FakeNaviClient(appScope)
        navi = fakeNavi                              // 실제: NaviHelper 래퍼
        tts = DemoTtsClient(this)                    // 실제: Pleos TextToSpeech 래퍼
        repository = DemoJourneyRepository(appScope) // 실제: TourAPI·LLM·Firebase·Room
        simulator = fakeNavi

        navi.initialize()
        tts.initialize()
    }

    override fun onTerminate() {
        navi.release()
        tts.release()
        super.onTerminate()
    }

    /** 시연을 처음 상태로 */
    fun resetDemo() {
        tts.stop()
        navi.cancelRoute()
        simulator?.reset()
        repository.resetDemo()
    }
}
