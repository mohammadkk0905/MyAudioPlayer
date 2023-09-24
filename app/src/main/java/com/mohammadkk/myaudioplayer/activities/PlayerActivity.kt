package com.mohammadkk.myaudioplayer.activities

import android.content.Intent
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.databinding.ActivityPlayerBinding
import com.mohammadkk.myaudioplayer.extensions.applyColor
import com.mohammadkk.myaudioplayer.extensions.errorToast
import com.mohammadkk.myaudioplayer.extensions.getColorCompat
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.getPlayingIcon
import com.mohammadkk.myaudioplayer.extensions.getPrimaryColor
import com.mohammadkk.myaudioplayer.extensions.getTrackArt
import com.mohammadkk.myaudioplayer.extensions.sendIntent
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.extensions.updateIconTint
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.ui.MusicSeekBar
import com.mohammadkk.myaudioplayer.utils.PlaybackRepeat
import com.mohammadkk.myaudioplayer.viewmodels.PlaybackViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var isAnimPlay = true
    private val baseSettings get() = BaseSettings.getInstance()
    private val playbackViewModel: PlaybackViewModel by viewModels()
    private var isRtlLocal = false
    private var mPlaceholder: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(mBackPressedCallback)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.playbackToolbar)
        isRtlLocal = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL
        supportActionBar?.title = getString(R.string.playing)
        binding.playbackToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        initializeSlider()
        initializeButtons()
        val song = findSongByIntent()
        if (song == null) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }
        initializeViewModel()
        initializeSongInfo(song)
        if (intent.hasExtra(Constant.RESTART_PLAYER)) {
            intent.removeExtra(Constant.RESTART_PLAYER)
            Intent(this, MusicService::class.java).apply {
                putExtra(Constant.SONG_ID, song.id)
                action = Constant.INIT
                try {
                    startService(this)
                    setPlayPause(isAnim = false, playing = true)
                } catch (e: Exception) {
                    errorToast(e)
                }
            }
        } else {
            isAnimPlay = false
            sendIntent(Constant.BROADCAST_STATUS)
        }
    }
    private fun initializeViewModel() {
        playbackViewModel.song.observe(this) {
            if (it == null) {
                finish()
            } else {
                initializeSongInfo(it)
            }
        }
        playbackViewModel.isPlaying.observe(this) {
            setPlayPause(true, it)
        }
        playbackViewModel.isPermission.observe(this) {
            if (!it) {
                toast(R.string.permission_storage_denied)
                finish()
            }
        }
        playbackViewModel.position.observe(this) {
            binding.playbackSeekBar.positionMills = it
        }
    }
    private fun initializeSongInfo(song: Song) {
        CoroutineScope(Dispatchers.IO).launch {
            val coverArt = song.getTrackArt(applicationContext)
            withContext(Dispatchers.Main) {
                if (coverArt != null) {
                    binding.playbackCover.setImageBitmap(coverArt)
                } else {
                    binding.playbackCover.setImageDrawable(createPlaceholder())
                }
            }
        }
        binding.playbackSong.text = song.title
        binding.playbackAlbum.text = song.album
        binding.playbackArtist.text = song.artist
        binding.playbackCount.text = String.format(
            Locale.getDefault(), "%d / %d",
            MusicService.findIndex().plus(1),
            MusicService.mSongs.size
        )
        binding.playbackSeekBar.durationMills = song.duration
    }
    private fun createPlaceholder(): Drawable? {
        if (mPlaceholder == null) {
            mPlaceholder = object : Drawable() {
                val src = getDrawableCompat(R.drawable.ic_audiotrack)!!.applyColor(
                    getColorCompat(R.color.pink_400)
                )
                override fun draw(canvas: Canvas) {
                    src.bounds.set(canvas.clipBounds)
                    val adjustWidth = src.bounds.width() / 4
                    val adjustHeight = src.bounds.height() / 4
                    src.bounds.set(
                        adjustWidth, adjustHeight,
                        src.bounds.width() - adjustWidth,
                        src.bounds.height() - adjustHeight
                    )
                    src.draw(canvas)
                }
                override fun setAlpha(alpha: Int) {
                    src.alpha = alpha
                }
                override fun setColorFilter(colorFilter: ColorFilter?) {
                    src.colorFilter = colorFilter
                }
                @Suppress("OVERRIDE_DEPRECATION")
                override fun getOpacity(): Int {
                    return PixelFormat.TRANSLUCENT
                }
            }
        }
        return mPlaceholder
    }
    private fun findSongByIntent(): Song? {
        if (intent.hasExtra(Constant.SONG)) {
            intent.removeExtra(Constant.SONG)
            val gson = GsonBuilder().create()
            val type = object : TypeToken<Song>() {}.type
            return gson.fromJson(
                intent.getStringExtra(Constant.SONG), type
            ) ?: MusicService.mCurrSong
        }
        return MusicService.mCurrSong
    }
    private val mBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
    private fun initializeSlider() {
        binding.playbackSeekBar.setOnCallback(object : MusicSeekBar.Callback {
            override fun onSeekTo(position: Int) {
                isAnimPlay = false
                Intent(this@PlayerActivity, MusicService::class.java).apply {
                    putExtra(Constant.PROGRESS, position)
                    action = Constant.SET_PROGRESS
                    startService(this)
                }
            }
        })
        binding.playbackSeekBar.setOnSkipBackward { sendIntent(Constant.SKIP_BACKWARD) }
        binding.playbackSeekBar.setOnSkipForward { sendIntent(Constant.SKIP_FORWARD) }
    }
    private fun initializeButtons() {
        binding.playbackSkipPrev.rotation = if (isRtlLocal) 180f else 0f
        binding.playbackSkipNext.rotation = if (isRtlLocal) 180f else 0f
        binding.playbackShuffle.setOnClickListener { toggleShuffle() }
        binding.playbackSkipPrev.setOnClickListener {
            if (MusicService.isPlaying()) isAnimPlay = false
            if (binding.playbackSeekBar.positionMills >= 5) {
                binding.playbackSeekBar.positionMills = 0
            }
            if (isRtlLocal) {
                sendIntent(Constant.NEXT)
            } else sendIntent(Constant.PREVIOUS)
        }
        binding.playbackPlayPause.setOnClickListener {
            sendIntent(Constant.PLAY_PAUSE)
        }
        binding.playbackSkipNext.setOnClickListener {
            if (MusicService.isPlaying()) isAnimPlay = false
            if (binding.playbackSeekBar.positionMills >= 5) {
                binding.playbackSeekBar.positionMills = 0
            }
            if (isRtlLocal) {
                sendIntent(Constant.PREVIOUS)
            } else sendIntent(Constant.NEXT)
        }
        binding.playbackRepeat.setOnClickListener { togglePlaybackRepeat() }
        initializeBtnShuffle()
        initializeBtnRepeat()
    }
    private fun toggleShuffle() {
        val isShuffleEnabled = !baseSettings.isShuffleEnabled
        baseSettings.isShuffleEnabled = isShuffleEnabled
        toast(if (isShuffleEnabled) R.string.shuffle_enabled else R.string.shuffle_disabled)
        initializeBtnShuffle()
        sendIntent(Constant.REFRESH_LIST)
    }
    private fun initializeBtnShuffle() {
        val isShuffle = baseSettings.isShuffleEnabled
        binding.playbackShuffle.apply {
            alpha = if (isShuffle) 1f else 0.9f
            updateIconTint(if (isShuffle) getPrimaryColor() else getColorCompat(R.color.grey_800))
            contentDescription = getString(if (isShuffle) R.string.shuffle_enabled else R.string.shuffle_disabled)
        }
    }
    private fun togglePlaybackRepeat() {
        val newPlaybackRepeat = baseSettings.playbackRepeat.nextPlayBackRepeat
        baseSettings.playbackRepeat = newPlaybackRepeat
        toast(newPlaybackRepeat.descriptionRes)
        initializeBtnRepeat()
    }
    private fun initializeBtnRepeat() {
        val playbackRepeat = baseSettings.playbackRepeat
        binding.playbackRepeat.apply {
            contentDescription = getString(playbackRepeat.nextPlayBackRepeat.descriptionRes)
            setImageResource(playbackRepeat.iconRes)
            val isRepeatOff = playbackRepeat == PlaybackRepeat.REPEAT_OFF
            alpha = if (isRepeatOff) 0.9f else 1f
            updateIconTint(if (isRepeatOff) getColorCompat(R.color.grey_800) else getPrimaryColor())
        }
    }
    private fun setPlayPause(isAnim: Boolean, playing: Boolean) {
        val icon = playing.getPlayingIcon(false)
        if (!isAnim || !isAnimPlay) {
            binding.playbackPlayPause.setImageResource(icon)
            isAnimPlay = true
        } else {
            binding.playbackPlayPause.setImageResource(playing.getPlayingIcon(true))
            val drawable = binding.playbackPlayPause.drawable
            if (drawable != null) {
                when (drawable) {
                    is AnimatedVectorDrawable -> drawable.start()
                    is AnimatedVectorDrawableCompat -> drawable.start()
                    else -> binding.playbackPlayPause.setImageResource(icon)
                }
            } else {
                binding.playbackPlayPause.setImageResource(icon)
            }
        }
    }
    private fun isFadeAnim(): Boolean {
        return intent?.getBooleanExtra("fade_anim", false) ?: false
    }
    override fun onPause() {
        super.onPause()
        if (isFadeAnim()) {
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
}