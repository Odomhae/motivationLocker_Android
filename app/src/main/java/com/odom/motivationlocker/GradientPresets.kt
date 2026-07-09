package com.odom.motivationlocker

import android.graphics.Color

// startColor/endColor는 실제 색상 값(Int)이다 — 리소스 id가 아니라 선택된 배경색을 기준으로
// 그때그때 계산된 색이기 때문.
data class GradientPreset(val startColor: Int, val endColor: Int)

object GradientPresets {

    // 사용자가 고른 배경색(baseColor)을 기준으로, 서로 뚜렷이 구별되는 그라데이션 변형들을 생성한다.
    // 인덱스는 SharedPreferences에 "backgroundGradientPreset"으로 영속되므로
    // 이 리스트의 순서나 개수를 바꾸면 기존 사용자가 골라둔 그라데이션이 달라질 수 있다 — append만 허용.
    fun forBaseColor(baseColor: Int): List<GradientPreset> {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        return listOf(
            // 밝게 -> 어둡게 (같은 색조)
            GradientPreset(shade(hue, sat, value, 1.3f), shade(hue, sat, value, 0.5f)),
            // 흰색 -> 원색
            GradientPreset(Color.WHITE, baseColor),
            // 원색 -> 검정
            GradientPreset(baseColor, Color.BLACK),
            // 보색(반대 색조)
            GradientPreset(baseColor, hueShift(hue, sat, value, 180f)),
            // 인접 색조 +40도
            GradientPreset(baseColor, hueShift(hue, sat, value, 40f)),
            // 인접 색조 -40도
            GradientPreset(baseColor, hueShift(hue, sat, value, -40f)),
            // 채도를 낮춘 회색조 -> 원색
            GradientPreset(desaturate(hue, sat, value), baseColor),
            // 파스텔 톤 -> 원색
            GradientPreset(pastel(hue, sat, value), baseColor)
        )
    }

    private fun shade(hue: Float, sat: Float, value: Float, valueScale: Float): Int =
        Color.HSVToColor(floatArrayOf(hue, sat, (value * valueScale).coerceIn(0f, 1f)))

    private fun hueShift(hue: Float, sat: Float, value: Float, degrees: Float): Int {
        val newHue = (hue + degrees + 360f) % 360f
        return Color.HSVToColor(floatArrayOf(newHue, sat, value))
    }

    private fun desaturate(hue: Float, sat: Float, value: Float): Int =
        Color.HSVToColor(floatArrayOf(hue, sat * 0.15f, value))

    private fun pastel(hue: Float, sat: Float, value: Float): Int =
        Color.HSVToColor(floatArrayOf(hue, (sat * 0.4f).coerceIn(0f, 1f), (value * 1.15f).coerceIn(0f, 1f)))
}
