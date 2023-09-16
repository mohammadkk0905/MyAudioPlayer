package com.mohammadkk.myaudioplayer.utils

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

@TargetApi(26)
class OreoAudioFocus(context: Context) {
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun abandonAudioFocus() {
        if (audioFocusRequest != null) {
            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
        }
    }
    fun requestAudioFocus(listener: AudioManager.OnAudioFocusChangeListener) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(listener)
            .setAudioAttributes(audioAttributes)
            .build()

        audioManager?.requestAudioFocus(audioFocusRequest!!)
    }
}