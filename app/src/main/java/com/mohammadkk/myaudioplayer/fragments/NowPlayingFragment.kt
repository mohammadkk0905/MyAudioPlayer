package com.mohammadkk.myaudioplayer.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.activities.PlayerActivity
import com.mohammadkk.myaudioplayer.databinding.FragmentNowPlayingBinding
import com.mohammadkk.myaudioplayer.extensions.applyColor
import com.mohammadkk.myaudioplayer.extensions.getColorCompat
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.getPlayingIcon
import com.mohammadkk.myaudioplayer.extensions.getTrackArt
import com.mohammadkk.myaudioplayer.extensions.sendIntent
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.viewmodels.PlaybackViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class NowPlayingFragment : Fragment(R.layout.fragment_now_playing) {
    private lateinit var binding: FragmentNowPlayingBinding
    private val playbackViewModel: PlaybackViewModel by activityViewModels()
    private var mPlaceholder: Drawable? = null
    private var isAnimPlay = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNowPlayingBinding.bind(view)
        initializeViewModel()
        binding.root.setOnClickListener {
            with(requireActivity()) {
                val mIntent = Intent(this, PlayerActivity::class.java)
                mIntent.putExtra("fade_anim", true)
                startActivity(mIntent)
                overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
            }
        }
        binding.btnPlayPause.setOnClickListener {
            isAnimPlay = true
            requireContext().sendIntent(Constant.PLAY_PAUSE)
        }
    }
    private fun initializeViewModel() {
        playbackViewModel.song.observe(requireActivity()) {
            onSongChanged(it)
        }
        playbackViewModel.isPlaying.observe(requireActivity()) {
            setPlayPause(it)
        }
        playbackViewModel.isPermission.observe(requireActivity()) {
            if (!it) onNoStoragePermission()
        }
        playbackViewModel.position.observe(requireActivity()) {
            onProgressUpdated(it)
        }
    }
    private fun initSongInfo(song: Song) {
        val placeholder = getPlaceholder()
        CoroutineScope(Dispatchers.IO).launch {
            val songCover = song.getTrackArt(requireContext())
            withContext(Dispatchers.Main) {
                if (songCover != null) {
                    binding.trackImage.setImageBitmap(songCover)
                    binding.trackImage.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    binding.trackImage.setImageDrawable(placeholder)
                    binding.trackImage.scaleType = ImageView.ScaleType.CENTER
                }
            }
        }
        val gravity = getGravity()
        if (binding.tvInfoTrack.gravity != gravity) binding.tvInfoTrack.gravity = gravity
        binding.tvInfoTrack.text = HtmlCompat.fromHtml(
            "<b>${song.title}</b><br>${song.album}",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.songProgress.max = song.duration / 1000
    }
    @SuppressLint("RtlHardcoded")
    private fun getGravity(): Int {
        val isRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL
        return if (isRtl) Gravity.RIGHT else Gravity.LEFT
    }
    private fun getPlaceholder(): Drawable? {
        if (mPlaceholder == null) {
            mPlaceholder = requireContext().getDrawableCompat(
                R.drawable.ic_audiotrack
            )?.applyColor(
                requireContext().getColorCompat(R.color.blue_500)
            )
        }
        return mPlaceholder
    }
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (MusicService.isMusicPlayer()) {
            isAnimPlay = false
            context?.sendIntent(Constant.BROADCAST_STATUS)
        }
    }
    private fun onNoStoragePermission() {
        Log.d(javaClass.simpleName, "Not permission storage")
    }
    private fun onProgressUpdated(progress: Int) {
        binding.songProgress.progress = progress / 1000
    }
    private fun onSongChanged(song: Song?) {
        if (song == null) {
            binding.trackImage.setImageDrawable(getPlaceholder())
            val gravity = getGravity()
            if (binding.tvInfoTrack.gravity != gravity) binding.tvInfoTrack.gravity = gravity
            binding.tvInfoTrack.text = HtmlCompat.fromHtml(
                "<b>${MediaStore.UNKNOWN_STRING}</b><br>${MediaStore.UNKNOWN_STRING}",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            initSongInfo(song)
        }
    }
    private fun setPlayPause(playing: Boolean) {
        val icon = playing.getPlayingIcon(false)
        if (!isAnimPlay) {
            binding.btnPlayPause.setImageResource(icon)
            isAnimPlay = true
        } else {
            binding.btnPlayPause.setImageResource(playing.getPlayingIcon(true))
            val drawable = binding.btnPlayPause.drawable
            if (drawable != null) {
                when (drawable) {
                    is AnimatedVectorDrawable -> drawable.start()
                    is AnimatedVectorDrawableCompat -> drawable.start()
                    else -> binding.btnPlayPause.setImageResource(icon)
                }
            } else {
                binding.btnPlayPause.setImageResource(icon)
            }
        }
    }
}