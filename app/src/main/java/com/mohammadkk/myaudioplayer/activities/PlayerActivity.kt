package com.mohammadkk.myaudioplayer.activities

import android.content.Intent
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.databinding.ActivityPlayerBinding
import com.mohammadkk.myaudioplayer.extensions.applyColor
import com.mohammadkk.myaudioplayer.extensions.errorToast
import com.mohammadkk.myaudioplayer.extensions.getColorCompat
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.getTrackArt
import com.mohammadkk.myaudioplayer.extensions.sendIntent
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.ui.MusicSeekBar
import com.mohammadkk.myaudioplayer.viewmodels.PlaybackViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
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
                    binding.playbackPlayPause.setImageResource(R.drawable.ic_pause)
                } catch (e: Exception) {
                    errorToast(e)
                }
            }
        } else {
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
            binding.playbackPlayPause.setImageResource(
                if (it) R.drawable.ic_pause else R.drawable.ic_play
            )
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
        supportActionBar?.subtitle = String.format(
            Locale.getDefault(), "%d / %d",
            MusicService.mCurrIndex.plus(1),
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
        binding.playbackShuffle.setOnClickListener {  }
        binding.playbackSkipPrev.setOnClickListener {
            if (binding.playbackSeekBar.positionMills >= 5) {
                onPrevSong()
            } else {
                binding.playbackSeekBar.positionMills = 0
                onPrevSong()
            }
        }
        binding.playbackPlayPause.setOnClickListener {
            sendIntent(Constant.PLAY_PAUSE)
        }
        binding.playbackSkipNext.setOnClickListener {
            if (binding.playbackSeekBar.positionMills >= 5) {
                onNextSong()
            } else {
                binding.playbackSeekBar.positionMills = 0
                onNextSong()
            }
        }
        binding.playbackRepeat.setOnClickListener {  }
    }
    private fun onPrevSong() {
        if (isRtlLocal) {
            sendIntent(Constant.NEXT)
        } else sendIntent(Constant.PREVIOUS)
    }
    private fun onNextSong() {
        if (isRtlLocal) {
            sendIntent(Constant.PREVIOUS)
        } else sendIntent(Constant.NEXT)
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