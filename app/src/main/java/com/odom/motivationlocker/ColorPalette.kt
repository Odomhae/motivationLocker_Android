package com.odom.motivationlocker

import android.content.Context
import androidx.core.content.ContextCompat

// 색상/그라데이션 선택 다이얼로그가 공유하는 20색 팔레트.
// colors.xml이 유일한 진실 소스이므로 하드코딩된 hex 값은 여기 두지 않는다.
object ColorPalette {
    val ALL = listOf(
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

    // 현재 저장된 실제 색상(actualColor)과 일치하는 팔레트의 리소스 id를 찾는다(체크 표시 위치 결정용).
    fun resIdForColor(context: Context, actualColor: Int): Int {
        return ALL.firstOrNull { ContextCompat.getColor(context, it) == actualColor } ?: R.color.colorWhite
    }
}
