package com.odom.motivationlocker

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

// summary에 "#FFFFFF" 같은 hex 문자열 대신, 항목 오른쪽에 실제 색상 원형 칩을 보여주는 Preference.
// 단색(1개)·그라데이션(2개) 모두 지원하고, 사진 배경처럼 색이 없는 경우엔 칩을 숨긴다.
class ColorIndicatorPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    private var indicatorColors: IntArray? = null

    init {
        widgetLayoutResource = R.layout.preference_color_indicator
    }

    fun setIndicatorColor(color: Int) {
        indicatorColors = intArrayOf(color, color)
        notifyChanged()
    }

    fun setIndicatorGradient(startColor: Int, endColor: Int) {
        indicatorColors = intArrayOf(startColor, endColor)
        notifyChanged()
    }

    fun clearIndicator() {
        indicatorColors = null
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val indicator = holder.findViewById(R.id.colorIndicator) ?: return
        val colors = indicatorColors
        if (colors == null) {
            indicator.visibility = View.GONE
        } else {
            indicator.visibility = View.VISIBLE
            // 흰색처럼 배경과 구분이 안 되는 색도 보이도록 옅은 테두리를 함께 그린다
            val strokeWidth = (context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            indicator.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(strokeWidth, 0x33000000)
            }
        }
    }
}
