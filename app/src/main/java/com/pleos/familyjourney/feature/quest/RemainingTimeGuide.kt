package com.pleos.familyjourney.feature.quest

import com.pleos.familyjourney.data.josa
import kotlin.math.roundToInt

/**
 * 남은 시간을 아이가 이해하는 단위로 바꾼다.
 * 실제 재생 중인 곡과 어긋날 수 있어서 "약 n곡 분량" 으로 말한다.
 */
object RemainingTimeGuide {
    const val SONG_MIN = 3.5
    const val STORY_MIN = 5.0

    data class Guide(val songs: Int, val stories: Int, val sentence: String)

    fun describe(remainingSec: Int, childName: String? = null): Guide {
        val min = remainingSec / 60.0
        val songs = (min / SONG_MIN).roundToInt()
        val stories = (min / STORY_MIN).roundToInt()
        val who = childName?.let { "${it.josa("아", "야")}, " } ?: ""
        val sentence = when {
            min < 2 -> "${who}거의 다 왔어! 이제 곧 도착이야."
            songs <= 1 -> "${who}노래 한 곡만 들으면 도착이야!"
            else -> "${who}도착까지 노래 약 ${songs}곡 분량 남았어. 이야기로 치면 ${stories}편이야."
        }
        return Guide(songs, stories, sentence)
    }
}
