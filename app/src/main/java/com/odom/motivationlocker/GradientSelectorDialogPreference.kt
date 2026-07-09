package com.odom.motivationlocker

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
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
        dialog.selectedPresetIndex = presetIndex
        dialog.onDialogGradientClickListener = this
        dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "gradientPicker")
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        presetIndex = getPersistedInt(defaultValue as? Int ?: 0)
    }

    override fun getSummary(): CharSequence = ""

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val colorPreview = holder.itemView?.findViewById<android.widget.ImageView>(R.id.color_preview)
        val preset = GradientPresets.ALL.getOrNull(presetIndex) ?: GradientPresets.ALL[0]
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                ContextCompat.getColor(context, preset.startColor),
                ContextCompat.getColor(context, preset.endColor)
            )
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
