package com.mohammadkk.myaudioplayer.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Size
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.util.Util.isOnMainThread
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.extensions.getAlbumArt
import com.mohammadkk.myaudioplayer.extensions.getTrackArt
import com.mohammadkk.myaudioplayer.extensions.hasPermission
import com.mohammadkk.myaudioplayer.extensions.stopForegroundNotRemoved
import com.mohammadkk.myaudioplayer.extensions.stopForegroundRemoved
import com.mohammadkk.myaudioplayer.extensions.toContentUri
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.utils.NotificationUtils
import com.mohammadkk.myaudioplayer.utils.OreoAudioFocus
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private val playbackStateManager = PlaybackStateManager.getInstance()
    private val settings = BaseSettings.getInstance()
    var mPlayer: MediaPlayer? = null
    private var mCurrSongCover: Bitmap? = null
    private var mPlaceholder: Bitmap? = null
    private var mHeadsetPlugReceiver: HeadsetPlugReceiver? = null
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekBarPositionUpdateTask: Runnable? = null
    private var mAudioManager: AudioManager? = null
    private var mOreoAudioFocus: OreoAudioFocus? = null
    private var wasPlayingFocusLost = false
    private var playOnPrepare = true
    private var mMediaSession: MediaSessionCompat? = null
    private var retriedSongCount = 0
    private var prevAudioFocusState = 0
    private var setProgressOnPrepare = 0
    private var mClicksCount = 0
    private val mButtonControlHandler = Handler(Looper.getMainLooper())
    private val mRunnable = Runnable {
        if (mClicksCount == 0) return@Runnable
        when (mClicksCount) {
            1 -> onHandlePlayPause()
            2 -> onHandleNext()
            else -> onHandlePrevious()
        }
        mClicksCount = 0
    }
    private var notificationUtils: NotificationUtils? = null
    private var isForeground = false
    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            onHandleMediaButton(mediaButtonEvent)
            return true
        }
        override fun onPlay() {
            onResumeSong()
        }
        override fun onPause() {
            onPauseSong()
        }
        override fun onStop() {
            if (isServiceInit)
                onPauseSong()
        }
        override fun onSkipToNext() {
            onHandleNext()
        }
        override fun onSkipToPrevious() {
            onHandlePrevious()
        }
        override fun onSeekTo(pos: Long) {
            updateProgress(pos.toInt())
        }
        override fun onCustomAction(action: String?, extras: Bundle?) {
            if (action == Constant.DISMISS)
                onHandleDismiss()
        }
    }
    private val mAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_GAIN -> audioFocusGained()
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duckAudio(true)
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> audioFocusLost()
        }
        prevAudioFocusState = focusChange
    }
    override fun onCreate() {
        super.onCreate()
        onCreateMediaSession()

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Constant.isOreoPlus()) mOreoAudioFocus = OreoAudioFocus(applicationContext)

        if (!Constant.isQPlus() && hasPermission(Constant.storagePermissionApi())) {
            playbackStateManager.noStoragePermission()
        }
        notificationUtils = NotificationUtils.createInstance(this, mMediaSession!!)
    }
    override fun onDestroy() {
        super.onDestroy()
        destroyPlayer()
        mMediaSession?.isActive = false
        mMediaSession = null
    }
    private fun audioFocusGained() {
        if (wasPlayingFocusLost) {
            if (prevAudioFocusState == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                duckAudio(false)
            } else {
                onResumeSong()
            }
        }
    }
    private fun duckAudio(isDuck: Boolean) {
        if (isDuck) {
            mPlayer?.setVolume(0.3f, 0.3f)
            wasPlayingFocusLost = isPlaying()
        } else {
            mPlayer?.setVolume(1f, 1f)
        }
    }
    private fun audioFocusLost() {
        if (isPlaying()) {
            wasPlayingFocusLost = true
            onPauseSong()
        } else {
            wasPlayingFocusLost = false
        }
    }
    private fun onCreateMediaSession() {
        mMediaSession = MediaSessionCompat(this, "MusicService")
        @Suppress("DEPRECATION")
        mMediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mMediaSession!!.setCallback(mMediaSessionCallback)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Constant.isQPlus() && !hasPermission(Constant.storagePermissionApi())) {
            return START_NOT_STICKY
        }
        notifyFocusGained()
        val action = intent?.action
        when (action) {
            Constant.INIT -> onHandleInit(intent)
            Constant.PREVIOUS -> onHandlePrevious()
            Constant.PAUSE -> onPauseSong()
            Constant.PLAY_PAUSE -> onHandlePlayPause()
            Constant.NEXT -> onHandleNext()
            Constant.FINISH -> onHandleFinish()
            Constant.DISMISS -> onHandleDismiss()
            Constant.SET_PROGRESS -> onHandleSetProgress(intent)
            Constant.SKIP_BACKWARD -> onSkip(false)
            Constant.SKIP_FORWARD -> onSkip(true)
            Constant.BROADCAST_STATUS -> {
                broadcastSongStateChange(mPlayer?.isPlaying ?: false)
                broadcastSongChange()
                broadcastSongProgress(mPlayer?.currentPosition ?: 0)
            }
        }
        MediaButtonReceiver.handleIntent(mMediaSession!!, intent)
        if (action != Constant.DISMISS && action != Constant.FINISH) {
            startForegroundOrNotify()
        }
        return START_NOT_STICKY
    }
    private fun notifyFocusGained() {
        isServiceInit = true
        wasPlayingFocusLost = false
        prevAudioFocusState = AUDIOFOCUS_GAIN
    }
    private fun isPlaying(): Boolean {
        return try {
            mPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }
    private fun initializeService(intent: Intent?) {
        var wantedId = intent?.getLongExtra(Constant.SONG_ID, -1L) ?: -1L
        if (wantedId == -1L) {
            val lastPlaying = settings.getLastPlaying()
            wantedId = lastPlaying?.first?.id ?: -1L
            setProgressOnPrepare = lastPlaying?.second ?: 0
        }
        for (i in mSongs.indices) {
            val track = mSongs[i]
            if (track.id == wantedId) {
                mCurrSong = track
                break
            }
        }
        wasPlayingFocusLost = false
        initMediaPlayerIfNeeded()
        startForegroundOrNotify()
        isServiceInit = true
    }
    private fun onHandleInit(intent: Intent? = null) {
        Constant.ensureBackgroundThread {
            initializeService(intent)
            playOnPrepare = true
            val wantedId = mCurrSong?.id ?: -1L
            setSong(wantedId)
        }
    }
    private fun onHandlePrevious() {
        if (mSongs.isEmpty()) {
            onHandleEmptyList()
            return
        }
        initMediaPlayerIfNeeded()
        if (mPlayer!!.currentPosition < 4500) {
            restartSong()
        } else {
            mCurrIndex -= 1
            if (mCurrIndex < 0) mCurrIndex = mSongs.lastIndex
            mCurrSong = mSongs[mCurrIndex]
            setSong(mCurrSong!!.id)
        }
    }
    private fun onResumeSong() {
        if (mSongs.isEmpty()) {
            onHandleEmptyList()
            return
        }
        initMediaPlayerIfNeeded()
        if (mCurrSong == null) {
            setupNextSong()
        } else {
            mPlayer!!.start()
            requestAudioFocus()
        }
        songStateChanged(true)
    }
    private fun onPauseSong() {
        initMediaPlayerIfNeeded()
        mPlayer!!.pause()
        songStateChanged(false)
        updateMediaSessionState()
        saveSongProgress()
        if (!Constant.isSPlus()) {
            stopForegroundNotRemoved()
            isForeground = false
        }
    }
    private fun onHandlePlayPause() {
        playOnPrepare = true
        if (isPlaying())
            onPauseSong()
        else
            onResumeSong()
    }
    private fun onHandleNext() {
        playOnPrepare = true
        setupNextSong()
    }
    private fun onHandleFinish() {
        broadcastSongProgress(0)
        stopForegroundOrNotification(false)
        stopSelf()
    }
    private fun onHandleDismiss() {
        if (isPlaying()) onPauseSong()
        stopForegroundOrNotification(true)
    }
    private fun restartSong() {
        if (mCurrSong != null)
            setSong(mCurrSong!!.id)
    }
    private fun setupNextSong() {
        val songId = when (mSongs.size) {
            0 -> -1L
            1 -> mSongs.first().id
            else -> {
                mCurrIndex = (mCurrIndex + 1) % mSongs.size
                val currSong = mSongs[mCurrIndex]
                mCurrSong = currSong
                currSong.id
            }
        }
        setSong(songId)
    }
    private fun onHandleSetProgress(intent: Intent) {
        if (mPlayer != null) {
            val progress = intent.getIntExtra(Constant.PROGRESS, mPlayer!!.currentPosition)
            updateProgress(progress)
        }
    }
    private fun startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            if (mSeekBarPositionUpdateTask == null) {
                mSeekBarPositionUpdateTask = Runnable { updateProgressCallbackTask() }
            }
            mExecutor = Executors.newSingleThreadScheduledExecutor()
            mExecutor?.scheduleAtFixedRate(
                mSeekBarPositionUpdateTask,
                0, 1000,
                TimeUnit.MILLISECONDS
            )
        }
    }
    private fun stopUpdatingCallbackWithPosition() {
        mExecutor?.shutdownNow()
        mExecutor = null
        mSeekBarPositionUpdateTask = null
    }
    private fun updateProgressCallbackTask() {
        if (isPlaying()) {
            val mTime = mPlayer!!.currentPosition
            broadcastSongProgress(mTime)
        }
    }
    private fun onHandleProgressHandler(playing: Boolean) {
        if (playing) {
            startUpdatingCallbackWithPosition()
        } else {
            stopUpdatingCallbackWithPosition()
        }
    }
    private fun onHandleMediaButton(mediaButtonEvent: Intent?) {
        if (mediaButtonEvent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val swapBtn = settings.swapPrevNext
            val event: KeyEvent = IntentCompat.getParcelableExtra(
                mediaButtonEvent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java
            ) ?: return
            if (event.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> onResumeSong()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> onPauseSong()
                    KeyEvent.KEYCODE_MEDIA_STOP -> onPauseSong()
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> onHandlePlayPause()
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if (swapBtn) onHandleNext() else onHandlePrevious()
                    KeyEvent.KEYCODE_MEDIA_NEXT -> if (swapBtn) onHandlePrevious() else onHandleNext()
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        mClicksCount++
                        mButtonControlHandler.removeCallbacks(mRunnable)
                        if (mClicksCount >= 3) {
                            mButtonControlHandler.post(mRunnable)
                        } else {
                            mButtonControlHandler.postDelayed(mRunnable, 700)
                        }
                    }
                }
            }
        }
    }
    private fun onHandleEmptyList() {
        mPlayer?.pause()
        abandonAudioFocus()
        songChanged()
        songStateChanged(false)
        if (!isServiceInit) onHandleInit()
    }
    private fun onSkip(forward: Boolean) {
        val curr = mPlayer?.currentPosition ?: return
        val newProgress = if (forward) min(curr + 1000, mPlayer!!.duration) else max(curr - 1000, 0)
        mPlayer!!.seekTo(newProgress)
        onResumeSong()
    }
    private fun updateProgress(progress: Int) {
        mPlayer!!.seekTo(progress)
        saveSongProgress()
        onResumeSong()
    }
    private fun songChanged() {
        broadcastSongChange()
        updateMediaSession()
        updateMediaSessionState()
    }
    private fun songStateChanged(playing: Boolean, notify: Boolean = true) {
        onHandleProgressHandler(playing)
        broadcastSongStateChange(playing)
        if (notify) startForegroundOrNotify()
        if (mHeadsetPlugReceiver == null) {
            mHeadsetPlugReceiver = HeadsetPlugReceiver()
        }
        if (playing) {
            val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(mHeadsetPlugReceiver, filter)
        } else {
            try {
                unregisterReceiver(mHeadsetPlugReceiver)
            } catch (ignored: IllegalArgumentException) {
            }
        }
    }
    private fun setSong(wantedId: Long) {
        if (mSongs.isEmpty()) {
            onHandleEmptyList()
            return
        }
        initMediaPlayerIfNeeded()
        mPlayer?.reset() ?: return
        if (mCurrSong == null) {
            mCurrSong = mSongs.firstOrNull { it.id == wantedId } ?: return
        }
        try {
            val songUri = if (mCurrSong!!.id == 0L) {
                File(mCurrSong!!.path).toUri()
            } else mCurrSong!!.id.toContentUri()

            mPlayer!!.setDataSource(applicationContext, songUri)
            mPlayer!!.prepareAsync()
            songChanged()
        } catch (e: IOException) {
            if (retriedSongCount < 3) {
                retriedSongCount++
                setupNextSong()
            }
        } catch (ignored: Exception) {
        }
    }
    private fun broadcastSongProgress(progress: Int) {
        playbackStateManager.progressUpdated(progress)
        updateMediaSessionState()
    }
    private fun broadcastSongChange() {
        if (isOnMainThread()) {
            playbackStateManager.songChanged(mCurrSong)
        } else {
            Handler(Looper.getMainLooper()).post {
                playbackStateManager.songChanged(mCurrSong)
            }
        }
        saveSongProgress()
    }
    private fun broadcastSongStateChange(playing: Boolean) {
        playbackStateManager.songStateChanged(playing)
    }
    private fun updateMediaSession() {
        val songCover = getSongCoverImage()
        mCurrSongCover = songCover.first
        var imageScreen = if (songCover.second) songCover.first else null
        if (imageScreen == null || imageScreen.isRecycled) imageScreen = mPlaceholder

        val metadata = MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, imageScreen)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mCurrSong?.album ?: "")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mCurrSong?.artist ?: "")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mCurrSong?.title ?: "")
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mCurrSong?.id?.toString())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (mCurrSong?.duration ?: 0).toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (mCurrIndex + 1).toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mSongs.size.toLong())
            .build()

        mMediaSession?.setMetadata(metadata)
    }
    private fun updateMediaSessionState() {
        val builder = PlaybackStateCompat.Builder()
        val playbackState = if (isPlaying()) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val dismissAction = PlaybackStateCompat.CustomAction.Builder(
            Constant.DISMISS,
            getString(R.string.dismiss),
            R.drawable.ic_close
        ).build()

        builder.setActions(mMediaSessionActions)
            .setState(playbackState, (mPlayer?.currentPosition ?: 0).toLong(), 1f)
            .addCustomAction(dismissAction)
        try {
            mMediaSession?.setPlaybackState(builder.build())
        } catch (ignored: Exception) {
        }
    }
    private fun getSongCoverImage(): Pair<Bitmap, Boolean> {
        if (mPlaceholder == null) {
            mPlaceholder = BitmapFactory.decodeResource(resources, R.drawable.ic_music_large)
        }
        if (File(mCurrSong?.path ?: "").exists()) {
            val rawArt = mCurrSong?.getTrackArt(baseContext ?: applicationContext)
            if (rawArt != null) return Pair(rawArt, true)
        }
        val albumArt = mCurrSong?.getAlbumArt(baseContext ?: applicationContext)
        if (albumArt != null) return Pair(albumArt, true)
        if (Constant.isQPlus()) {
            if (mCurrSong?.path?.startsWith("content://") == true) {
                try {
                    val size = Size(512, 512)
                    val thumbnail = contentResolver.loadThumbnail(mCurrSong!!.path.toUri(), size, null)
                    return Pair(thumbnail, true)
                } catch (ignored: Exception) {
                }
            }
        }
        return Pair(mPlaceholder!!, false)
    }
    private fun destroyPlayer() {
        saveSongProgress()
        mCurrSong = null
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
        songStateChanged(playing = false, notify = false)
        songChanged()
        isServiceInit = false
        abandonAudioFocus()
    }
    override fun onPrepared(mp: MediaPlayer?) {
        retriedSongCount = 0
        if (playOnPrepare) {
            mp?.start()
            requestAudioFocus()
        }
        if (setProgressOnPrepare > 0) {
            mp?.seekTo(setProgressOnPrepare)
            broadcastSongProgress(setProgressOnPrepare)
            setProgressOnPrepare = 0
        }
        songStateChanged(isPlaying())
    }
    private fun isEndedPlaylist(): Boolean {
        return when (mSongs.size) {
            0, 1 -> true
            else -> mCurrSong?.id == mSongs.last().id
        }
    }
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mPlayer!!.reset()
        return false
    }
    override fun onCompletion(mp: MediaPlayer?) {
        if (!settings.autoplay) return
        if (isEndedPlaylist()) {
            broadcastSongProgress(0)
            setupNextSong()
        } else setupNextSong()
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private fun initMediaPlayerIfNeeded() {
        if (mPlayer != null) {
            return
        }
        mPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            if (Constant.isOreoPlus()) {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                setAudioStreamType(STREAM_MUSIC)
            }
            setOnPreparedListener(this@MusicService)
            setOnCompletionListener(this@MusicService)
            setOnErrorListener(this@MusicService)
        }
    }
    private fun startForegroundOrNotify() {
        if (mCurrSongCover?.isRecycled == true) {
            mCurrSongCover = BitmapFactory.decodeResource(resources, R.drawable.ic_music_large)
        }
        notificationUtils?.createMusicNotification(
            song = mCurrSong,
            playing = isPlaying(),
            largeIcon = mCurrSongCover
        ) {
            if (!isForeground) {
                try {
                    if (Constant.isQPlus()) {
                        startForeground(
                            NotificationUtils.NOTIFICATION_ID, it,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(NotificationUtils.NOTIFICATION_ID, it)
                    }
                    isForeground = true
                } catch (ignored: IllegalStateException) {
                }
            } else {
                notificationUtils?.notify(NotificationUtils.NOTIFICATION_ID, it)
            }
        }
    }
    private fun stopForegroundOrNotification(isReset: Boolean) {
        try {
            if (isForeground) {
                stopForegroundRemoved()
                isForeground = false
            } else {
                notificationUtils?.cancel(NotificationUtils.NOTIFICATION_ID)
            }
            if (isReset) isServiceInit = false
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
    private fun requestAudioFocus() {
        if (Constant.isQPlus()) {
            mOreoAudioFocus?.requestAudioFocus(mAudioFocusChange)
        } else {
            @Suppress("DEPRECATION")
            mAudioManager?.requestAudioFocus(mAudioFocusChange, STREAM_MUSIC, AUDIOFOCUS_GAIN)
        }
    }
    private fun abandonAudioFocus() {
        if (Constant.isOreoPlus()) {
            mOreoAudioFocus?.abandonAudioFocus()
        } else {
            @Suppress("DEPRECATION")
            mAudioManager?.abandonAudioFocus(mAudioFocusChange)
        }
    }
    private fun saveSongProgress() {
        val currPosition = mPlayer?.currentPosition ?: 0
        if (mCurrSong != null && currPosition != 0) {
            settings.putLastPlaying(
                Pair(mCurrSong, mPlayer!!.currentPosition)
            )
        }
    }
    companion object {
        private const val mMediaSessionActions = PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_PAUSE

        private var isServiceInit = false
        var mCurrSong: Song? = null
        var mSongs = listOf<Song>()
        var mCurrIndex = 0

        fun isInitialize() = isServiceInit
    }
}