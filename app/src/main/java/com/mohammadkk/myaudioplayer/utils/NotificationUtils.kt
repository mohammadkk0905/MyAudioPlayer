package com.mohammadkk.myaudioplayer.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.activities.MainActivity
import com.mohammadkk.myaudioplayer.extensions.notificationManager
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.services.NotificationDismissedReceiver
import com.mohammadkk.myaudioplayer.services.NotificationReceiver

class NotificationUtils(private val context: Context, private val mediaSessionToken: MediaSessionCompat.Token?) {
    private val notificationManager = context.notificationManager

    fun createMusicNotification(song: Song?, playing: Boolean, largeIcon: Bitmap?, onCreate: (Notification) -> Unit) {
        val title = song?.title.orEmpty()
        val artist = song?.artist.orEmpty()
        var postTime = 0L
        var multiBoolean = false
        if (playing) {
            postTime = System.currentTimeMillis() - (MusicService.mPlayer?.position() ?: 0)
            multiBoolean = true
        }
        val previousAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous,
            context.getString(R.string.previous),
            getIntent(Constant.PREVIOUS)
        ).build()
        val playPauseAction = NotificationCompat.Action.Builder(
            if (playing) R.drawable.ic_pause else R.drawable.ic_play,
            context.getString(R.string.play_pause),
            getIntent(Constant.PLAY_PAUSE)
        ).build()
        val nextAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next,
            context.getString(R.string.previous),
            getIntent(Constant.NEXT)
        ).build()
        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.ic_close,
            context.getString(R.string.dismiss),
            getIntent(Constant.DISMISS)
        ).build()
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_audiotrack)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setWhen(postTime)
            .setShowWhen(multiBoolean)
            .setUsesChronometer(multiBoolean)
            .setContentIntent(getContentIntent())
            .setOngoing(multiBoolean)
            .setChannelId(NOTIFICATION_CHANNEL)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSessionToken!!)
            )
            .setDeleteIntent(getDismissedIntent())
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(dismissAction)

        try {
            builder.setLargeIcon(largeIcon)
        } catch (ignored: OutOfMemoryError) {
        }
        onCreate(builder.build())
    }
    fun createMediaScanNotification(text: String, progress: Int, max: Int): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(context.getString(R.string.scanning))
            .setSmallIcon(R.drawable.ic_audiotrack)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(getContentIntent())
            .setChannelId(NOTIFICATION_CHANNEL)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setDeleteIntent(getDismissedIntent())
            .setOngoing(true)
            .setProgress(max, progress, progress == 0)
            .apply {
                if (text.isNotEmpty()) {
                    setContentText(text)
                }
            }.build()
    }
    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }
    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }
    private fun getContentIntent(): PendingIntent {
        val cIntent = Intent(context, MainActivity::class.java)
        cIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        var flags = 0
        if (Constant.isMarshmallowPlus()) flags = PendingIntent.FLAG_IMMUTABLE or 0
        return PendingIntent.getActivity(context, 0, cIntent, flags)
    }
    private fun getDismissedIntent(): PendingIntent {
        val nDismissedIntent = Intent(context, NotificationDismissedReceiver::class.java).setAction(
            Constant.NOTIFICATION_DISMISSED
        )
        return PendingIntentCompat.getBroadcast(
            context, 0, nDismissedIntent, PendingIntent.FLAG_CANCEL_CURRENT, false
        )
    }
    private fun getIntent(actionName: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.action = actionName
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Constant.isMarshmallowPlus()) {
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
    companion object {
        private const val NOTIFICATION_CHANNEL = "audio_player_channel"
        const val SCANNER_NOTIFICATION_ID = 43
        const val NOTIFICATION_ID = 57

        @RequiresApi(26)
        private fun createNotificationChannel(context: Context) {
            with(context.notificationManager) {
                if (getNotificationChannel(NOTIFICATION_CHANNEL) == null) {
                    val nChannel = NotificationChannel(
                        NOTIFICATION_CHANNEL,
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                    nChannel.enableLights(false)
                    nChannel.enableVibration(false)
                    nChannel.setShowBadge(false)

                    createNotificationChannel(nChannel)
                }
            }
        }
        fun createInstance(context: Context, mediaSession: MediaSessionCompat? = null): NotificationUtils {
            if (Constant.isOreoPlus()) createNotificationChannel(context)
            return NotificationUtils(context, mediaSession?.sessionToken)
        }
    }
}