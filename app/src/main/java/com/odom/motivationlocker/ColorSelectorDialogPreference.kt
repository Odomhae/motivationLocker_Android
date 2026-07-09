package com.odom.motivationlocker

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.odom.ledscreen.ColorSelectorDialogBuilder

class ColorSelectorDialogPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr), ColorSelectorDialog.OnDialogColorClickListener {

    private var color: Int = Color.WHITE

    private val colorList = listOf(
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
        val dialog = ColorSelectorDialogBuilder()
            .setColorList(colorList)
            .setFigureType(FigureType.CIRCLE)
            .setSelectedColor(findColorResId(color))
            .setOnDialogColorClickListener(this)
            .build()

        dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "colorPicker")
    }

    // 현재 저장된 실제 색상(color)과 일치하는 팔레트의 리소스 id를 찾는다(체크 표시 위치 결정용).
    // colors.xml이 유일한 진실 소스이므로 하드코딩된 hex 테이블을 별도로 유지하지 않는다.
    private fun findColorResId(actualColor: Int): Int {
        return colorList.firstOrNull { ContextCompat.getColor(context, it) == actualColor } ?: R.color.colorWhite
    }

    // pref.xml의 android:defaultValue="@color/..."를 올바르게 읽기 위해 오버라이드.
    // 이게 없으면 defaultValue가 항상 null로 전달되어 onSetInitialValue가 무조건 흰색으로 폴백한다.
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getColor(index, Color.WHITE)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        color = getPersistedInt(defaultValue as? Int ?: Color.WHITE)
    }

    override fun getSummary(): CharSequence {
        return ""//String.format("#%06X", 0xFFFFFF and color)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // Update color preview to show selected color
        val colorPreview = holder.itemView?.findViewById<android.widget.ImageView>(R.id.color_preview)
        colorPreview?.setBackgroundColor(color)
    }

    override fun onColorClick(tagDialog: String, selectedColor: Int?) {
        selectedColor?.let { colorResId ->
            val actualColor = ContextCompat.getColor(context, colorResId)

            // Save the color and notify listeners
            setColor(actualColor)

            // Call the preference change listener manually
            callChangeListener(actualColor)
        }
    }
}
