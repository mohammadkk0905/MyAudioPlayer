package com.mohammadkk.myaudioplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.android.material.slider.Slider
import com.mohammadkk.myaudioplayer.databinding.MusicSeekBarBinding
import com.mohammadkk.myaudioplayer.extensions.toFormattedDuration
import kotlin.math.max
class MusicSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    Slider.OnSliderTouchListener, Slider.OnChangeListener {
    private val binding = MusicSeekBarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.musicSlider.addOnSliderTouchListener(this)
        binding.musicSlider.addOnChangeListener(this)
    }

    private var mCallback: Callback? = null

    var positionMills: Int
        get() = binding.musicSlider.value.toInt()
        set(value) {
            if (value <= durationMills && !isActivated) {
                binding.musicSlider.value = value.toFloat()
                binding.sliderPosition.text = value.toFormattedDuration(true)
            }
        }

    var durationMills: Int
        get() = binding.musicSlider.valueTo.toInt()
        set(value) {
            val valueTo = max(value, 1)
            isEnabled = value > 0
            if (positionMills > value) {
                binding.musicSlider.value = valueTo.toFloat()
            }
            binding.musicSlider.valueTo = valueTo.toFloat()
            binding.sliderDuration.text = value.toFormattedDuration(true)
        }

    fun setOnCallback(callback: Callback?) {
        mCallback = callback
    }
    fun setOnSkipBackward(listener: OnClickListener?) {
        binding.sliderPosition.setOnClickListener(listener)
    }
    fun setOnSkipForward(listener: OnClickListener?) {
        binding.sliderDuration.setOnClickListener(listener)
    }
    override fun onStartTrackingTouch(slider: Slider) {
        isActivated = true
    }
    override fun onStopTrackingTouch(slider: Slider) {
        isActivated = false
        mCallback?.onSeekTo(slider.value.toInt())
    }
    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        if (fromUser) {
            binding.sliderPosition.text = value.toInt().toFormattedDuration(true)
        }
    }
    interface Callback {
        fun onSeekTo(position: Int)
    }
}