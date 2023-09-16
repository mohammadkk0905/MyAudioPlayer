package com.mohammadkk.myaudioplayer.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.extensions.sendIntent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (val action = intent?.action) {
            Constant.PREVIOUS, Constant.PLAY_PAUSE,
            Constant.NEXT, Constant.FINISH, Constant.DISMISS -> {
                context?.sendIntent(action)
            }
        }
    }
}