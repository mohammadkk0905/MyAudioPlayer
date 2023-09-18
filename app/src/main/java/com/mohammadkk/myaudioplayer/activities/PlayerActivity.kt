package com.mohammadkk.myaudioplayer.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
            binding.trackSlider.positionMills = it
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
        binding.trackSlider.durationMills = song.duration
    }

    private fun initMediaButtons() {
        binding.btnPreviousTrack.setOnClickListener {
            binding.trackSlider.positionMills = 0
            sendIntent(Constant.PREVIOUS)
        }
        binding.fabPlayPause.setOnClickListener {
            sendIntent(Constant.PLAY_PAUSE)
        }
        binding.btnNextTrack.setOnClickListener {
            binding.trackSlider.positionMills = 0
            sendIntent(Constant.NEXT)
        }
        binding.trackSlider.setOnSkipBackward {
            sendIntent(Constant.SKIP_BACKWARD)
        }
        binding.trackSlider.setOnSkipForward {
            sendIntent(Constant.SKIP_FORWARD)
        }
    }
    private fun initTrackSlider() {
        binding.trackSlider.setOnCallback(object : MusicSeekBar.Callback {
            override fun onSeekTo(position: Int) {
                Intent(this@PlayerActivity, MusicService::class.java).apply {
                    putExtra(Constant.PROGRESS, position)
                    action = SET_PROGRESS
                    startService(this)
                }
            }
        })
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