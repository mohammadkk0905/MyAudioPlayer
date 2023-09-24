package com.mohammadkk.myaudioplayer

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.models.StateMode
import com.mohammadkk.myaudioplayer.utils.PlaybackRepeat
import java.lang.reflect.Type

class BaseSettings(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = GsonBuilder().create()
    private val songType = object : TypeToken<Pair<Song, Int>>() {}.type
    private val stateType = object : TypeToken<StateMode>() {}.type

    var songsSorting: Int
        get() = prefs.getInt("songs_sorting", Constant.SORT_BY_TITLE)
        set(value) = prefs.edit { putInt("songs_sorting", value) }

    var albumsSorting: Int
        get() = prefs.getInt("albums_sorting", Constant.SORT_BY_TITLE)
        set(value) = prefs.edit { putInt("albums_sorting", value) }

    var artistsSorting: Int
        get() = prefs.getInt("artists_sorting", Constant.SORT_BY_TITLE)
        set(value) = prefs.edit { putInt("artists_sorting", value) }

    var swapPrevNext: Boolean
        get() = prefs.getBoolean("swap_prev_next", false)
        set(value) = prefs.edit { putBoolean("swap_prev_next", value) }

    var gaplessPlayback: Boolean
        get() = prefs.getBoolean("gapless_playback", false)
        set(value) = prefs.edit { putBoolean("gapless_playback", value) }

    var autoplay: Boolean
        get() = prefs.getBoolean("autoplay", true)
        set(value) = prefs.edit { putBoolean("autoplay", value) }

    var playbackRepeat: PlaybackRepeat
        get() {
            val index = prefs.getInt("playback_repeat", PlaybackRepeat.REPEAT_OFF.ordinal)
            return PlaybackRepeat.values().getOrNull(index) ?: PlaybackRepeat.REPEAT_OFF
        }
        set(value) = prefs.edit { putInt("playback_repeat", value.ordinal) }

    var isShuffleEnabled: Boolean
        get() = prefs.getBoolean("shuffle", false)
        set(value) = prefs.edit {putBoolean("shuffle", value) }

    var lastPlaying:  Pair<Song, Int>?
        get() = getObject("last_playing", songType)
        set(value) = putObject("last_playing", value)

    var lastStateMode:  StateMode
        get() = getObject("last_state_mode", stateType) ?: StateMode("MAIN", -1L)
        set(value) = putObject("last_state_mode", value)

    private fun <T> getObject(key: String, type: Type): T? {
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, type)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
    private fun <T> putObject(key: String, value: T?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
            return
        }
        val json = gson.toJson(value)
        prefs.edit { putString(key, json) }
    }
    companion object {
        @Volatile
        private var INSTANCE: BaseSettings? = null

        fun initialize(context: Context): BaseSettings {
            return INSTANCE ?: synchronized(this) {
                val instance = BaseSettings(context)
                INSTANCE = instance
                instance
            }
        }
        fun getInstance(): BaseSettings {
            return INSTANCE ?: error("Not initialize settings!")
        }
    }
}