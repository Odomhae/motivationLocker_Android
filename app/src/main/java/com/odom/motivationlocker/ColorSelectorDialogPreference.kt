package com.odom.motivationlocker

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.odom.ledscreen.ColorSelectorDialogBuilder

class ColorSelectorDialogPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr), ColorSelectorDialog.OnDialogColorClickListener {

    private var color: Int = Color.WHITE

    init {
        widgetLayoutResource = R.layout.preference_color
    }

    fun setColor(color: Int) {
        this.color = color
        persistInt(color)
        notifyChanged()
    }

    fun getColor(): Int = color

    override fun onClick() {
        super.onClick()
        showColorPicker()
    }

    private fun showColorPicker() {
        val colorList = listOf(
            R.color.colorWhite,
            R.color.colorGray,
            R.color.colorBlack,
            R.color.colorRed,
            R.color.colorCrimson,
            R.color.colorSalmon,
            R.color.colorBeige,
            R.color.colorOrange,
            R.color.colorBrown,
            R.color.colorWalnut,
            R.color.colorBlue,
            R.color.colorMalibu,
            R.color.colorGreen,
            R.color.colorYellowGreen,
            R.color.colorMint,
            R.color.colorYellow,
            R.color.colorPink,
            R.color.colorViolet,
            R.color.colorMagenta,
            R.color.colorPurple
        )

        val dialog = ColorSelectorDialogBuilder()
            .setColorList(colorList)
            .setFigureType(FigureType.CIRCLE)
            .setSelectedColor(getColorResourceId(color))
            .setOnDialogColorClickListener(this)
            .build()

        dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "colorPicker")
    }

    private fun getColorResourceId(color: Int): Int {
        // Try to find matching resource ID for the color
        val colorMap = mapOf(
            Color.WHITE to R.color.colorWhite,
            Color.BLACK to R.color.colorBlack,
            Color.RED to R.color.colorRed,
            Color.GREEN to R.color.colorGreen,
            Color.BLUE to R.color.colorBlue
        )
        return colorMap[color] ?: R.color.colorWhite
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        color = getPersistedInt(defaultValue as? Int ?: Color.WHITE)
    }

    override fun getSummary(): CharSequence {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        
        // Update color preview to show selected color
        val colorPreview = holder.itemView?.findViewById<android.widget.ImageView>(R.id.color_preview)
        colorPreview?.setBackgroundColor(color)
    }

    override fun onColorClick(tagDialog: String, selectedColor: Int?) {
        selectedColor?.let { colorResId ->
            val actualColor = try {
                val hexColor = getColorHex(colorResId)
                android.graphics.Color.parseColor(hexColor)
            } catch (e: Exception) {
                // Fallback to predefined colors
                when (colorResId) {
                    R.color.colorWhite -> Color.WHITE
                    R.color.colorBlack -> Color.BLACK
                    R.color.colorRed -> Color.RED
                    R.color.colorGreen -> Color.GREEN
                    R.color.colorBlue -> Color.BLUE
                    else -> Color.WHITE
                }
            }
            
            // Save the color and notify listeners
            setColor(actualColor)
            
            // Call the preference change listener manually
            callChangeListener(actualColor)
        }
    }

    private fun getColorHex(colorResId: Int): String {
        return when (colorResId) {
            R.color.colorWhite -> "#FFFFFF"
            R.color.colorBlack -> "#000000"
            R.color.colorRed -> "#FF0000"
            R.color.colorGreen -> "#00FF00"
            R.color.colorBlue -> "#0000FF"
            R.color.colorGray -> "#808080"
            R.color.colorCrimson -> "#DC143C"
            R.color.colorSalmon -> "#FA8072"
            R.color.colorBeige -> "#F5F5DC"
            R.color.colorOrange -> "#FFA500"
            R.color.colorBrown -> "#A52A2A"
            R.color.colorWalnut -> "#8B4513"
            R.color.colorMalibu -> "#87CEEB"
            R.color.colorYellowGreen -> "#9ACD32"
            R.color.colorMint -> "#98FF98"
            R.color.colorYellow -> "#FFFF00"
            R.color.colorPink -> "#FFC0CB"
            R.color.colorViolet -> "#EE82EE"
            R.color.colorMagenta -> "#FF00FF"
            R.color.colorPurple -> "#800080"
            else -> "#FFFFFF"
        }
    }
}
