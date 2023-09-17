package com.mohammadkk.myaudioplayer.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.Constant.SET_PROGRESS
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.databinding.ActivityPlayerBinding
import com.mohammadkk.myaudioplayer.extensions.applyColor
import com.mohammadkk.myaudioplayer.extensions.errorToast
import com.mohammadkk.myaudioplayer.extensions.getColorCompat
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.getTrackArt
import com.mohammadkk.myaudioplayer.extensions.sendIntent
import com.mohammadkk.myaudioplayer.extensions.toFormattedDuration
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.viewmodels.PlaybackViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private val playbackViewModel: PlaybackViewModel by viewModels()
    private var mPlaceholder: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(mBackPressedCallback)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.actionTop)
        supportActionBar?.title = null
        binding.actionTop.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.trackImage.scaleType = ImageView.ScaleType.CENTER_CROP
        initializeViewModel()
        initMediaButtons()
        initTrackSlider()
        val song = resolveSong()
        if (song == null) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }
        initSongInfo(song)
        if (intent.getBooleanExtra(Constant.RESTART_PLAYER, false)) {
            intent.removeExtra(Constant.RESTART_PLAYER)
            Intent(this, MusicService::class.java).apply {
                putExtra(Constant.SONG_ID, song.id)
                action = Constant.INIT
                try {
                    startService(this)
                    binding.fabPlayPause.setImageResource(R.drawable.ic_pause)
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
            onSongChanged(it)
        }
        playbackViewModel.isPlaying.observe(this) {
            onSongStateChanged(it)
        }
        playbackViewModel.isPermission.observe(this) {
            if (!it) onNoStoragePermission()
        }
        playbackViewModel.position.observe(this) {
            setProgressSlider(it)
        }
    }
    private val mBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
    private fun getPlaceholder(): Drawable? {
        if (mPlaceholder == null) {
            mPlaceholder = getDrawableCompat(R.drawable.ic_audiotrack)?.applyColor(
                getColorCompat(R.color.pink_400)
            )
        }
        return mPlaceholder
    }
    private fun initSongInfo(song: Song) {
        val placeholder = getPlaceholder()
        CoroutineScope(Dispatchers.IO).launch {
            val songCover = song.getTrackArt(applicationContext)
            withContext(Dispatchers.Main) {
                if (songCover != null) {
                    binding.trackImage.setImageBitmap(songCover)
                } else {
                    binding.trackImage.setImageDrawable(placeholder)
                }
            }
        }
        binding.tvTitleTrack.text = song.title
        binding.tvAlbumTrack.text = song.album
        binding.tvArtistTrack.text = song.artist
        binding.tvCountTrack.text = String.format(
            Locale.getDefault(), "%d / %d",
            MusicService.mCurrIndex.plus(1),
            MusicService.mSongs.size
        )
        setMaxSlider(song.duration)
    }
    private fun getProgressSlider(): Int {
        return binding.trackSlider.value.toInt()
    }
    private fun getMaxSlider(): Int {
        return binding.trackSlider.valueTo.toInt()
    }
    private fun setMaxSlider(value: Int) {
        val toValue = max(value, 1)
        binding.trackSlider.isEnabled = value > 0
        if (getProgressSlider() > toValue) {
            binding.trackSlider.value = toValue.toFloat()
        }
        binding.trackSlider.valueTo = toValue.toFloat()
        binding.tvTotalTimeTrack.text = value.toFormattedDuration(true)
    }
    private fun setProgressSlider(value: Int) {
        val from = max(value, 0)
        if (from <= getMaxSlider() && !binding.trackSlider.isActivated) {
            binding.trackSlider.value = from.toFloat()
            binding.tvPlayedTimeTrack.text = from.toFormattedDuration(true)
        }
    }
    private fun initMediaButtons() {
        binding.btnPreviousTrack.setOnClickListener {
            setProgressSlider(0)
            sendIntent(Constant.PREVIOUS)
        }
        binding.fabPlayPause.setOnClickListener {
            sendIntent(Constant.PLAY_PAUSE)
        }
        binding.btnNextTrack.setOnClickListener {
            setProgressSlider(0)
            sendIntent(Constant.NEXT)
        }
        binding.tvPlayedTimeTrack.setOnClickListener {
            sendIntent(Constant.SKIP_BACKWARD)
        }
        binding.tvTotalTimeTrack.setOnClickListener {
            sendIntent(Constant.SKIP_FORWARD)
        }
    }
    private fun initTrackSlider() {
        binding.trackSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                binding.trackSlider.isActivated = true
            }
            override fun onStopTrackingTouch(slider: Slider) {
                binding.trackSlider.isActivated = false
                Intent(this@PlayerActivity, MusicService::class.java).apply {
                    putExtra(Constant.PROGRESS, slider.value.toInt())
                    action = SET_PROGRESS
                    startService(this)
                }
            }
        })
        binding.trackSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val from = max(value.toInt(), 0)
                val time = if (from <= getMaxSlider()) {
                    from.toFormattedDuration(true)
                } else  "--:--"
                binding.tvPlayedTimeTrack.text = time
            }
        }
        binding.trackSlider.setLabelFormatter {
            val from = max(it.toInt(), 0)
            val time = if (from <= getMaxSlider()) {
                from.toFormattedDuration(true)
            } else  "--:--"
            return@setLabelFormatter time
        }
    }
    private fun resolveSong(): Song? {
        if (intent.hasExtra(Constant.SONG)) {
            intent.removeExtra(Constant.SONG)
            val json = intent.getStringExtra(Constant.SONG)
            val songType = object : TypeToken<Song>() {}.type
            return GsonBuilder().create().fromJson(json, songType) ?: MusicService.mCurrSong
        }
        return MusicService.mCurrSong
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
    private fun onNoStoragePermission() {
        toast(R.string.permission_storage_denied)
        finish()
    }
    private fun onSongChanged(song: Song?) {
        if (song == null) {
            finish()
        } else {
            initSongInfo(song)
        }
    }
    private fun onSongStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            binding.fabPlayPause.setImageResource(R.drawable.ic_pause)
        } else {
            binding.fabPlayPause.setImageResource(R.drawable.ic_play)
        }
    }
}