package com.mohammadkk.myaudioplayer.extensions

import android.annotation.SuppressLint
import android.app.Service
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.app.ServiceCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R

fun ViewPager2.reduceDragSensitivity() {
    try {
        val recycler = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recycler.isAccessible = true
        val recyclerView = recycler.get(this) as RecyclerView
        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * 3) // 3x seems to be the best fit here
    } catch (e: Exception) {
        Log.e("MainActivity", e.stackTraceToString())
    }
}
fun ImageView.updateIconTint(@ColorInt color: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
}
@SuppressLint("NotifyDataSetChanged")
fun <VH : RecyclerView.ViewHolder> RecyclerView.Adapter<VH>.notifyOnDataChanged() {
    notifyDataSetChanged()
}
fun Drawable.applyColor(@ColorInt color: Int): Drawable {
    val copy = mutate()
    if (Constant.isQPlus()) {
        copy.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        @Suppress("DEPRECATION")
        copy.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
    return copy
}
fun Boolean.getPlayingIcon(anim: Boolean): Int {
    return if (!anim) {
        if (this) R.drawable.ic_pause else R.drawable.ic_play
    } else {
        if (this) R.drawable.ic_play_to_pause else R.drawable.ic_pause_to_play
    }
}
fun MenuItem.setTitleColor(color: Int) {
    if (title.isNullOrEmpty()) return
    SpannableString(title).apply {
        setSpan(ForegroundColorSpan(color), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        title = this
    }
}
fun Service.stopForegroundCompat(isRemoved: Boolean) {
    if (isRemoved) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    } else {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    }
}