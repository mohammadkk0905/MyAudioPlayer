package com.mohammadkk.myaudioplayer.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.extensions.sendIntent

class NotificationDismissedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.sendIntent(Constant.DISMISS)
    }
}