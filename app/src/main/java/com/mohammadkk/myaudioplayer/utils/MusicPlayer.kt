package com.mohammadkk.myaudioplayer.utils

import android.app.Application
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.core.content.getSystemService
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.services.HeadsetPlugReceiver

class MusicPlayer(private val app: Application, private val listener: PlaybackListener) : MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private val settings get() = BaseSettings.getInstance()
    private var mCurrMediaPlayer = MediaPlayer()
    private var mNextMediaPlayer: MediaPlayer? = null
    var isInitialized = false
    private var mNoisyReceiverRegistered = false
    private val mNoisyReceiver = HeadsetPlugReceiver()
    private val mNoisyReceiverIntentFilter = IntentFilter(AudioManager.ACTION_HEADSET_PLUG).apply {
        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    }
    private val audioManager: AudioManager? = app.getSystemService()
    private var isPausedByTransientLossOfFocus = false
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!isPlaying() && isPausedByTransientLossOfFocus) {
                    start()
                    listener.onPlayStateChanged()
                    isPausedByTransientLossOfFocus = false
                }
                setVolume(Volume.NORMAL)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
                listener.onPlayStateChanged()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val wasPlaying = isPlaying()
                pause()
                listener.onPlayStateChanged()
                isPausedByTransientLossOfFocus = wasPlaying
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                setVolume(Volume.DUCK)
            }
        }
    }
    private var audioFocusRequest: AudioFocusRequestCompat? = null

    init {
        mCurrMediaPlayer.setWakeMode(app, PowerManager.PARTIAL_WAKE_LOCK)
    }

    fun isPlaying() : Boolean {
        return isInitialized && mCurrMediaPlayer.isPlaying
    }
    fun setDataSource(songUri: Uri) {
        isInitialized = true
        setDataSourceImpl(mCurrMediaPlayer, songUri) { result ->
            if (result.isFailure) {
                throw result.exceptionOrNull()!!
            }
            isInitialized = true
            setNextDataSource(null)
            listener.onPrepared()
        }
    }
    fun setNextDataSource(songUri: Uri?, onPrepared: (() -> Unit)? = null) {
        try {
            mCurrMediaPlayer.setNextMediaPlayer(null)
        } catch (e: IllegalArgumentException) {
            // Next media player is current one, continuing
        } catch (e: IllegalStateException) {
            return
        }
        releaseNextPlayer()
        if (songUri == null) return
        if (settings.gaplessPlayback) {
            mNextMediaPlayer = MediaPlayer()
            mNextMediaPlayer!!.setWakeMode(app, PowerManager.PARTIAL_WAKE_LOCK)
            mNextMediaPlayer!!.audioSessionId = mCurrMediaPlayer.audioSessionId
        }
        setDataSourceImpl(mNextMediaPlayer!!, songUri) { result ->
            if (result.isSuccess) {
                try {
                    mCurrMediaPlayer.setNextMediaPlayer(mNextMediaPlayer)
                    onPrepared?.invoke()
                } catch (e: IllegalArgumentException) {
                    releaseNextPlayer()
                } catch (e: IllegalStateException) {
                    releaseNextPlayer()
                }
            } else {
                releaseNextPlayer()
                throw result.exceptionOrNull()!!
            }
        }
    }
    fun start(): Boolean {
        requestFocus()
        registerBecomingNoisyReceiver()
        return try {
            mCurrMediaPlayer.start()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    private fun stop() {
        abandonFocus()
        unregisterBecomingNoisyReceiver()
        mCurrMediaPlayer.reset()
        isInitialized = false
    }
    fun release() {
        stop()
        mCurrMediaPlayer.release()
        mNextMediaPlayer?.release()
    }
    fun pause(): Boolean {
        if (!isPlaying()) return false

        unregisterBecomingNoisyReceiver()
        return try {
            mCurrMediaPlayer.pause()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    fun duration(): Int {
        return if (!this.isInitialized) {
            -1
        } else try {
            mCurrMediaPlayer.duration
        } catch (e: IllegalStateException) {
            -1
        }
    }
    fun position(): Int {
        return if (!this.isInitialized) {
            -1
        } else try {
            mCurrMediaPlayer.currentPosition
        } catch (e: IllegalStateException) {
            -1
        }
    }
    fun seekTo(whereto: Int): Int {
        return try {
            mCurrMediaPlayer.seekTo(whereto)
            whereto
        } catch (e: IllegalStateException) {
            -1
        }
    }
    private fun setVolume(vol: Float): Boolean {
        return try {
            mCurrMediaPlayer.setVolume(vol, vol)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        isInitialized = false
        mCurrMediaPlayer.reset()
        return false
    }
    override fun onCompletion(mp: MediaPlayer?) {
        if (settings.gaplessPlayback && mp == mCurrMediaPlayer && mNextMediaPlayer != null) {
            isInitialized = false
            mCurrMediaPlayer.reset()
            mCurrMediaPlayer.release()
            mCurrMediaPlayer = mNextMediaPlayer!!
            isInitialized = true
            mNextMediaPlayer = null
            listener.onTrackWentToNext()
        } else {
            listener.onTrackEnded()
        }
    }
    private fun setDataSourceImpl(player: MediaPlayer, songUri: Uri, onPrepared: (success: Result<Boolean>) -> Unit) {
        player.reset()
        try {
            player.setDataSource(app, songUri)
            player.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            player.setOnPreparedListener {
                player.setOnPreparedListener(null)
                onPrepared(Result.success(true))
            }
            player.prepareAsync()
        } catch (e: Exception) {
            onPrepared(Result.failure(e))
            e.printStackTrace()
        }
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }
    private fun releaseNextPlayer() {
        mNextMediaPlayer?.release()
        mNextMediaPlayer = null
    }
    private fun unregisterBecomingNoisyReceiver() {
        if (mNoisyReceiverRegistered) {
            app.unregisterReceiver(mNoisyReceiver)
            mNoisyReceiverRegistered = false
        }
    }
    private fun registerBecomingNoisyReceiver() {
        if (!mNoisyReceiverRegistered) {
            app.registerReceiver(
                mNoisyReceiver, mNoisyReceiverIntentFilter
            )
            mNoisyReceiverRegistered = true
        }
    }
    private fun getAudioFocusRequest(): AudioFocusRequestCompat {
        if (audioFocusRequest == null) {
            val audioAttributes = AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setAudioAttributes(audioAttributes)
                .build()
        }
        return audioFocusRequest!!
    }
    private fun requestFocus(): Boolean {
        return AudioManagerCompat.requestAudioFocus(audioManager!!, getAudioFocusRequest()) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    private fun abandonFocus() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager!!, getAudioFocusRequest())
    }
    interface PlaybackListener {
        fun onPrepared()
        fun onTrackEnded()
        fun onTrackWentToNext()
        fun onPlayStateChanged()
    }
    object Volume {
        const val DUCK = 0.2f
        const val NORMAL = 1.0f
    }
}