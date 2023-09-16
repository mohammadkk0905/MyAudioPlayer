package com.mohammadkk.myaudioplayer

import android.Manifest
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.bumptech.glide.util.Util.isOnMainThread
import kotlin.math.abs

object Constant {
    private var lastClickTime: Long = 0
    const val PERMISSION_REQUEST_STORAGE = 1001
    const val PERMISSION_REQUEST_NOTIFICATION = 1005

    const val SORT_BY_TITLE = 1
    const val SORT_BY_ALBUM = 2
    const val SORT_BY_ARTIST = 4
    const val SORT_BY_DURATION = 8
    const val SORT_BY_DATE_ADDED = 16
    const val SORT_BY_YEAR = 32
    const val SORT_BY_SONGS = 64

    const val SONG_ID = "song_id"
    const val ALBUM_ID = "album_id"
    const val ARTIST_ID = "artist_id"
    const val SONG = "song"
    const val RESTART_PLAYER = "restart_player"
    const val PROGRESS = "progress"

    // Notification
    private const val PATH = "com.mohammadkk.myaudioplayer.action."
    const val INIT = PATH + "INIT"
    const val PREVIOUS = PATH + "PREVIOUS"
    const val PAUSE = PATH + "PAUSE"
    const val PLAY_PAUSE = PATH + "PLAY_PAUSE"
    const val NEXT = PATH + "NEXT"
    const val FINISH = PATH + "FINISH"
    const val DISMISS = PATH + "DISMISS"
    const val SET_PROGRESS = PATH + "SET_PROGRESS"
    const val SKIP_BACKWARD = PATH + "SKIP_BACKWARD"
    const val SKIP_FORWARD = PATH + "SKIP_FORWARD"
    const val BROADCAST_STATUS = PATH + "BROADCAST_STATUS"
    const val NOTIFICATION_DISMISSED = PATH + "NOTIFICATION_DISMISSED"

    fun isBlockingClick(): Boolean {
        val isBlocking: Boolean
        val currentTime = System.currentTimeMillis()
        isBlocking = abs(currentTime - lastClickTime) < 500L
        if (!isBlocking) lastClickTime = currentTime
        return isBlocking
    }

    fun storagePermissionApi() = when {
        isTiramisuPlus() -> Manifest.permission.READ_MEDIA_AUDIO
        isRPlus() -> Manifest.permission.READ_EXTERNAL_STORAGE
        else -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    fun ensureBackgroundThread(callback: () -> Unit) {
        if (isOnMainThread()) {
            Thread {
                callback()
            }.start()
        } else {
            callback()
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun isTiramisuPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isSPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun isQPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
    fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}