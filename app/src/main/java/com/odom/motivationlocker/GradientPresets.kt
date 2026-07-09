package com.odom.motivationlocker

data class GradientPreset(val startColor: Int, val endColor: Int)

object GradientPresets {

    // 기존 20색(colors.xml) 팔레트를 조합한 프리셋. 인덱스는 SharedPreferences에
    // "backgroundGradientPreset"으로 영속되므로 append-only여야 한다(재배치 금지).
    val ALL: List<GradientPreset> = listOf(
        GradientPreset(R.color.colorBlue, R.color.colorMalibu),
        GradientPreset(R.color.colorPurple, R.color.colorMagenta),
        GradientPreset(R.color.colorPink, R.color.colorViolet),
        GradientPreset(R.color.colorOrange, R.color.colorYellow),
        GradientPreset(R.color.colorRed, R.color.colorCrimson),
        GradientPreset(R.color.colorGreen, R.color.colorYellowGreen),
        GradientPreset(R.color.colorMint, R.color.colorMalibu),
        GradientPreset(R.color.colorBrown, R.color.colorWalnut),
        GradientPreset(R.color.colorBeige, R.color.colorSalmon),
        GradientPreset(R.color.colorBlack, R.color.colorGray),
        GradientPreset(R.color.colorWhite, R.color.colorGray),
        GradientPreset(R.color.colorBlue, R.color.colorPurple),
        GradientPreset(R.color.colorPink, R.color.colorSalmon),
        GradientPreset(R.color.colorYellow, R.color.colorGreen),
        GradientPreset(R.color.colorViolet, R.color.colorMagenta),
        GradientPreset(R.color.colorWalnut, R.color.colorBlack)
    )
}
