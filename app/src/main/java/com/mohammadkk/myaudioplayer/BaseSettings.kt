package com.mohammadkk.myaudioplayer

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohammadkk.myaudioplayer.models.Song

class BaseSettings(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = GsonBuilder().create()
    private val songType = object : TypeToken<Pair<Song, Int>>() {}.type

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

    fun getLastPlaying(): Pair<Song, Int>? {
        val json = prefs.getString("last_playing", null) ?: return null
        return gson.fromJson(json, songType)
    }
    fun putLastPlaying(value: Pair<Song?, Int>) {
        if (value.first == null) return
        val json = gson.toJson(value)
        prefs.edit { putString("last_playing", json) }
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