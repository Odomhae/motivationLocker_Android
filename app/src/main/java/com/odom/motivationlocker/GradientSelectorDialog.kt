package com.odom.motivationlocker

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

// ColorSelectorDialog와 동일한 원형 그리드 구조를 재사용하되, 각 스와치를 2색 그라데이션으로 그린다.
// 프리셋이 고정 리스트라 ColorSelectorDialogBuilder 같은 별도 빌더는 두지 않는다.
class GradientSelectorDialog : DialogFragment(), View.OnClickListener {
    private lateinit var tagDialog: String
    lateinit var onDialogGradientClickListener: OnDialogGradientClickListener

    var selectedPresetIndex: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_color_picker_adapter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildView(view)
    }

    private fun buildView(view: View) {
        (view.findViewById(R.id.rootLayoutColorSelector) as LinearLayout).removeAllViews()
        view.setBackgroundResource(androidx.appcompat.R.color.material_blue_grey_800)

        val layoutParamsContainer = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val scale = requireContext().resources.displayMetrics.density
        val pixels = (50 * scale + 0.5f).toInt()

        val layoutParamsCircles = LinearLayout.LayoutParams(pixels, pixels)
        val margin = (4 * scale + 0.5f).toInt()
        layoutParamsCircles.setMargins(margin, margin, margin, margin)

        val presets = GradientPresets.ALL
        var rows = presets.size / 4
        if (presets.size % 4 != 0) rows += 1

        var index = 0

        for (i in 0 until rows) {
            val lineLayout = LinearLayout(context)
            lineLayout.gravity = Gravity.CENTER
            lineLayout.orientation = LinearLayout.HORIZONTAL
            lineLayout.layoutParams = layoutParamsContainer

            for (y in 0..3) {
                val imageButton = ImageButton(context)
                imageButton.layoutParams = layoutParamsCircles

                if (index < presets.size) {
                    val preset = presets[index]
                    imageButton.tag = index
                    imageButton.contentDescription = "Gradient $index"
                    imageButton.setOnClickListener(this)

                    imageButton.setBackgroundResource(R.drawable.circle)
                    val startColor = ContextCompat.getColor(requireContext(), preset.startColor)
                    val endColor = ContextCompat.getColor(requireContext(), preset.endColor)
                    (imageButton.background as GradientDrawable).apply {
                        orientation = GradientDrawable.Orientation.TL_BR
                        setColors(intArrayOf(startColor, endColor))
                    }
                } else {
                    imageButton.background = null
                }

                lineLayout.addView(imageButton)
                index += 1
            }

            (view.findViewById(R.id.rootLayoutColorSelector) as LinearLayout).addView(lineLayout)
        }
    }

    override fun onClick(v: View) {
        selectedPresetIndex = v.tag as Int
        onDialogGradientClickListener.onGradientClick(tagDialog, selectedPresetIndex)
        this.dismiss()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        this.tagDialog = tag!!
        super.show(manager, tag)
    }

    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        this.tagDialog = tag!!
        return super.show(transaction, tag)
    }

    interface OnDialogGradientClickListener {
        fun onGradientClick(tagDialog: String, selectedPresetIndex: Int)
    }
}
