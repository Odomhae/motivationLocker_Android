package com.odom.motivationlocker

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.odom.ledscreen.ColorSelectorDialogBuilder

// "글자 설정" 1줄을 눌렀을 때 뜨는 팝업. 글자색(기존 ColorSelectorDialog 재사용)과
// 글자 크기를 한 다이얼로그에서 고르게 한다.
class TextSettingsDialogFragment : DialogFragment(), ColorSelectorDialog.OnDialogColorClickListener {

    var onSummaryRefreshNeeded: (() -> Unit)? = null
    var onColorChangeTracked: (() -> Unit)? = null

    private lateinit var rowTextColor: View
    private lateinit var textColorPreview: ImageView
    private lateinit var sizeRadioGroup: RadioGroup

    private fun prefs() = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_text_settings, null)

        rowTextColor = view.findViewById(R.id.rowTextColor)
        textColorPreview = view.findViewById(R.id.textColorPreview)
        sizeRadioGroup = view.findViewById(R.id.sizeRadioGroup)

        refreshTextColorPreview()
        buildSizeRadioButtons()

        rowTextColor.setOnClickListener { showColorPicker() }

        return AlertDialog.Builder(context)
            .setTitle(R.string.text_settings_title)
            .setView(view)
            .setPositiveButton(R.string.settings_dialog_close, null)
            .create()
    }

    private fun currentTextColor(): Int = prefs().getInt("textColorCategory", Color.BLACK)

    private fun refreshTextColorPreview() {
        textColorPreview.setBackgroundColor(currentTextColor())
    }

    // MotivationLockerActivity/기존 요약 표시 모두 "textSize" 미설정 시 0(=S)을 기본값으로
    // 취급해 왔으므로(펜딩되던 pref.xml ListPreference의 defaultValue="1"은 실제로 읽히지 않던
    // 죽은 값), 여기서도 0을 기본값으로 맞춘다.
    private fun buildSizeRadioButtons() {
        val labels = resources.getStringArray(R.array.textSizeCategory)
        val currentSize = prefs().getInt("textSize", 0)
        sizeRadioGroup.removeAllViews()
        labels.forEachIndexed { index, label ->
            val radioButton = RadioButton(requireContext()).apply {
                text = label
                id = View.generateViewId()
                layoutParams = RadioGroup.LayoutParams(0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f)
                isChecked = index == currentSize
                setOnClickListener {
                    prefs().edit().putInt("textSize", index).apply()
                    onSummaryRefreshNeeded?.invoke()
                }
            }
            sizeRadioGroup.addView(radioButton)
        }
    }

    private fun showColorPicker() {
        val dialog = ColorSelectorDialogBuilder()
            .setColorList(ColorPalette.ALL)
            .setFigureType(FigureType.CIRCLE)
            .setSelectedColor(ColorPalette.resIdForColor(requireContext(), currentTextColor()))
            .setOnDialogColorClickListener(this)
            .build()
        dialog.show(childFragmentManager, "textColorPicker")
    }

    override fun onColorClick(tagDialog: String, selectedColor: Int?) {
        selectedColor?.let { colorResId ->
            val actualColor = ContextCompat.getColor(requireContext(), colorResId)
            prefs().edit().putInt("textColorCategory", actualColor).apply()
            refreshTextColorPreview()
            onSummaryRefreshNeeded?.invoke()
            onColorChangeTracked?.invoke()
        }
    }
}
