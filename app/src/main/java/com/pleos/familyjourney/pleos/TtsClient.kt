package com.pleos.familyjourney.pleos

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import android.speech.tts.TextToSpeech as AndroidTts

/** TextToSpeech 래퍼. lastSpoken은 화면 자막용(에뮬레이터 소리가 안 나와도 시연 가능). */
interface TtsClient {
    val lastSpoken: StateFlow<String>
    fun initialize()
    fun speak(text: String)
    fun stop()
    fun release()
}

// ---------------------------------------------------------------------------------------------
// 데모 구현: Android 기본 TTS 엔진. Pleos SDK 없이도 소리 + 자막으로 시연된다.
// ---------------------------------------------------------------------------------------------

class DemoTtsClient(private val context: Context) : TtsClient {

    private val _lastSpoken = MutableStateFlow("")
    override val lastSpoken: StateFlow<String> = _lastSpoken.asStateFlow()

    private var tts: AndroidTts? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    override fun initialize() {
        if (tts != null) return
        tts = AndroidTts(context) { status ->
            ready = status == AndroidTts.SUCCESS
            if (ready) {
                tts?.language = Locale.KOREAN
                pending.forEach { say(it) }
            }
            pending.clear()
        }
    }

    override fun speak(text: String) {
        _lastSpoken.value = text
        if (ready) say(text) else pending += text
    }

    private fun say(text: String) {
        // QUEUE_ADD: 지역 안내와 투표 결과가 겹쳐도 앞 문장을 끊지 않고 이어서 읽는다
        tts?.speak(text, AndroidTts.QUEUE_ADD, null, "ridemate-${text.hashCode()}")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun release() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
