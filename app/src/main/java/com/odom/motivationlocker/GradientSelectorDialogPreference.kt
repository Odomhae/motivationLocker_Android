package com.odom.motivationlocker

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class GradientSelectorDialogPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr), GradientSelectorDialog.OnDialogGradientClickListener {

    private var presetIndex: Int = 0

    init {
        widgetLayoutResource = R.layout.preference_color
    }

    override fun onClick() {
        super.onClick()
        showGradientPicker()
    }

    private fun showGradientPicker() {
        val dialog = GradientSelectorDialog()
        dialog.baseColor = currentBackgroundColor()
        dialog.selectedPresetIndex = presetIndex
        dialog.onDialogGradientClickListener = this
        dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "gradientPicker")
    }

    // 그라데이션은 사용자가 고른 단색 배경(backgroundColorCategory)을 기준으로 계산되므로 직접 읽어온다.
    private fun currentBackgroundColor(): Int {
        val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
        return prefs.getInt("backgroundColorCategory", Color.WHITE)
    }

    // pref.xml의 android:defaultValue="0"을 올바르게 읽기 위해 오버라이드(관례상 명시적으로 통일).
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, 0)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        presetIndex = getPersistedInt(defaultValue as? Int ?: 0)
    }

    override fun getSummary(): CharSequence = ""

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val colorPreview = holder.itemView?.findViewById<android.widget.ImageView>(R.id.color_preview)
        val presets = GradientPresets.forBaseColor(currentBackgroundColor())
        val preset = presets.getOrNull(presetIndex) ?: presets[0]
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(preset.startColor, preset.endColor)
        )
        colorPreview?.background = drawable
    }

    override fun onGradientClick(tagDialog: String, selectedPresetIndex: Int) {
        presetIndex = selectedPresetIndex
        persistInt(presetIndex)
        notifyChanged()
        callChangeListener(presetIndex)
    }
}
