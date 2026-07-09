package com.odom.motivationlocker

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.odom.ledscreen.ColorSelectorDialogBuilder

// "배경 설정" 1줄을 눌렀을 때 뜨는 팝업. 단색/그라데이션/사진 세부 설정을 기존 피커
// (ColorSelectorDialog, GradientSelectorDialog)를 재사용해 한 화면에서 고르게 한다.
// 실제 사진 선택(PickVisualMedia)은 Fragment STARTED 이전 등록 제약 때문에 호스트가 대신 실행한다.
class BackgroundSettingsDialogFragment : DialogFragment(),
    ColorSelectorDialog.OnDialogColorClickListener,
    GradientSelectorDialog.OnDialogGradientClickListener {

    var onSummaryRefreshNeeded: (() -> Unit)? = null
    var onColorChangeTracked: (() -> Unit)? = null
    var onPhotoPickRequested: (() -> Unit)? = null
    var onDialogDismissed: (() -> Unit)? = null

    private var photoUri: String? = null

    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var rowColor: View
    private lateinit var rowGradient: View
    private lateinit var rowPhoto: View
    private lateinit var colorPreview: ImageView
    private lateinit var gradientPreview: ImageView
    private lateinit var photoPreview: ImageView

    private fun prefs() = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_background_settings, null)

        typeRadioGroup = view.findViewById(R.id.typeRadioGroup)
        rowColor = view.findViewById(R.id.rowColor)
        rowGradient = view.findViewById(R.id.rowGradient)
        rowPhoto = view.findViewById(R.id.rowPhoto)
        colorPreview = view.findViewById(R.id.colorPreview)
        gradientPreview = view.findViewById(R.id.gradientPreview)
        photoPreview = view.findViewById(R.id.photoPreview)

        photoUri = prefs().getString("backgroundPhotoUri", null)

        val backgroundType = prefs().getInt("backgroundType", 0)
        typeRadioGroup.check(
            when (backgroundType) {
                1 -> R.id.radioGradient
                2 -> R.id.radioPhoto
                else -> R.id.radioSolid
            }
        )
        updateRowVisibility(backgroundType)
        refreshColorPreview()
        refreshGradientPreview()
        refreshPhotoPreview()

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val index = when (checkedId) {
                R.id.radioGradient -> 1
                R.id.radioPhoto -> 2
                else -> 0
            }
            prefs().edit().putInt("backgroundType", index).apply()
            updateRowVisibility(index)
            onSummaryRefreshNeeded?.invoke()
        }

        rowColor.setOnClickListener { showColorPicker() }
        rowGradient.setOnClickListener { showGradientPicker() }
        rowPhoto.setOnClickListener { onPhotoPickRequested?.invoke() }

        return AlertDialog.Builder(context)
            .setTitle(R.string.background_settings_title)
            .setView(view)
            .setPositiveButton(R.string.settings_dialog_close, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }

    // 호스트가 사진 선택 완료 후 호출해 다이얼로그가 열려있는 동안 썸네일만 갱신한다.
    // 요약 갱신/광고 카운트는 호스트(photoPickerLauncher 콜백)가 한 번만 책임진다 - 여기서
    // 다시 콜백을 부르면 중복 카운트될 수 있다.
    fun updatePhotoPreview(uri: Uri?) {
        photoUri = uri?.toString()
        refreshPhotoPreview()
    }

    private fun updateRowVisibility(backgroundType: Int) {
        rowColor.visibility = if (backgroundType == 0) View.VISIBLE else View.GONE
        rowGradient.visibility = if (backgroundType == 1) View.VISIBLE else View.GONE
        rowPhoto.visibility = if (backgroundType == 2) View.VISIBLE else View.GONE
    }

    private fun currentColor(): Int = prefs().getInt("backgroundColorCategory", Color.WHITE)

    private fun refreshColorPreview() {
        colorPreview.setBackgroundColor(currentColor())
    }

    private fun refreshGradientPreview() {
        val presetIndex = prefs().getInt("backgroundGradientPreset", 0)
        val presets = GradientPresets.forBaseColor(currentColor())
        val preset = presets.getOrNull(presetIndex) ?: presets[0]
        gradientPreview.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(preset.startColor, preset.endColor)
        )
    }

    private fun refreshPhotoPreview() {
        val uriString = photoUri
        val bitmap = uriString?.let {
            PhotoThumbnailLoader.loadThumbnail(requireContext(), Uri.parse(it), photoPreview.layoutParams.width)
        }
        if (bitmap != null) {
            photoPreview.setImageBitmap(bitmap)
            photoPreview.visibility = View.VISIBLE
        } else {
            photoPreview.visibility = View.GONE
        }
    }

    private fun showColorPicker() {
        val dialog = ColorSelectorDialogBuilder()
            .setColorList(ColorPalette.ALL)
            .setFigureType(FigureType.CIRCLE)
            .setSelectedColor(ColorPalette.resIdForColor(requireContext(), currentColor()))
            .setOnDialogColorClickListener(this)
            .build()
        dialog.show(childFragmentManager, "backgroundColorPicker")
    }

    private fun showGradientPicker() {
        val dialog = GradientSelectorDialog()
        dialog.baseColor = currentColor()
        dialog.selectedPresetIndex = prefs().getInt("backgroundGradientPreset", 0)
        dialog.onDialogGradientClickListener = this
        dialog.show(childFragmentManager, "backgroundGradientPicker")
    }

    override fun onColorClick(tagDialog: String, selectedColor: Int?) {
        selectedColor?.let { colorResId ->
            val actualColor = ContextCompat.getColor(requireContext(), colorResId)
            prefs().edit().putInt("backgroundColorCategory", actualColor).apply()
            refreshColorPreview()
            refreshGradientPreview()
            onSummaryRefreshNeeded?.invoke()
            onColorChangeTracked?.invoke()
        }
    }

    override fun onGradientClick(tagDialog: String, selectedPresetIndex: Int) {
        prefs().edit().putInt("backgroundGradientPreset", selectedPresetIndex).apply()
        refreshGradientPreview()
        onSummaryRefreshNeeded?.invoke()
        onColorChangeTracked?.invoke()
    }
}
