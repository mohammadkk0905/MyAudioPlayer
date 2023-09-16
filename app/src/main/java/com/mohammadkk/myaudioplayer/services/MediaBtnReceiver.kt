package com.mohammadkk.myaudioplayer.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.extensions.sendIntent

class MediaBtnReceiver : BroadcastReceiver() {
    private var mHandler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val settings = BaseSettings.getInstance()
        onCreateRunnable(context)
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val swapButton = settings.swapPrevNext
            val event: KeyEvent = IntentCompat.getParcelableExtra(
                intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java
            ) ?: return
            if (event.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        context?.sendIntent(Constant.PLAY_PAUSE)
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        context?.sendIntent(if (swapButton) Constant.NEXT else Constant.PREVIOUS)
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        context?.sendIntent(if (swapButton) Constant.PREVIOUS else Constant.NEXT)
                    }
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        mClickCount++
                        mHandler.removeCallbacks(mRunnable!!)
                        if (mClickCount >= 3) {
                            mHandler.post(mRunnable!!)
                        } else {
                            mHandler.postDelayed(mRunnable!!, 700)
                        }
                    }
                }
            }
        }
    }
    private fun onCreateRunnable(context: Context?) {
        if (mRunnable == null) {
            mRunnable = Runnable {
                if (mClickCount == 0) {
                    return@Runnable
                }
                context?.sendIntent(
                    when (mClickCount) {
                        1 -> Constant.PLAY_PAUSE
                        2 -> Constant.NEXT
                        else -> Constant.PREVIOUS
                    }
                )
                mClickCount = 0
            }
        }
    }
    companion object {
        private var mClickCount = 0
    }
}