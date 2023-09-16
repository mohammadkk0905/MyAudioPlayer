package com.mohammadkk.myaudioplayer.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.extensions.sendIntent

class HeadsetPlugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!isInitialStickyBroadcast && intent?.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", -1)
            if (state == 0) context?.sendIntent(Constant.PAUSE)
        } else if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            context?.sendIntent(Constant.PAUSE)
        }
    }
}