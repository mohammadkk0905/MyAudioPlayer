package com.mohammadkk.myaudioplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDivider
import com.mohammadkk.myaudioplayer.R

class DialogRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    private val topDivider = MaterialDivider(context)
    private val bottomDivider = MaterialDivider(context)
    private val spacingMedium = context.resources.getDimensionPixelSize(R.dimen.spacing_mid_medium)

    init {
        updatePadding(top = spacingMedium)
        overScrollMode = OVER_SCROLL_NEVER
        overlay.apply {
            add(topDivider)
            add(bottomDivider)
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        measureDivider(topDivider)
        measureDivider(bottomDivider)
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        topDivider.layout(l, spacingMedium, r, spacingMedium + topDivider.measuredHeight)
        bottomDivider.layout(l, measuredHeight - bottomDivider.measuredHeight, r, b)
        invalidateDividers()
    }
    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        invalidateDividers()
    }
    private fun measureDivider(divider: MaterialDivider) {
        val widthMeasureSpec = ViewGroup.getChildMeasureSpec(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            0, divider.layoutParams.width
        )
        val heightMeasureSpec = ViewGroup.getChildMeasureSpec(
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            0, divider.layoutParams.height
        )
        divider.measure(widthMeasureSpec, heightMeasureSpec)
    }
    private fun invalidateDividers() {
        val lmm = layoutManager as LinearLayoutManager
        topDivider.isInvisible = lmm.findFirstCompletelyVisibleItemPosition() < 1
        bottomDivider.isInvisible = lmm.findLastCompletelyVisibleItemPosition() == (lmm.itemCount - 1)
    }
}