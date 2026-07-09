package com.odom.motivationlocker

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.odom.motivationlocker.databinding.ActivityMotivationLockerBinding

class MotivationLockerActivity : AppCompatActivity() {

    companion object {
        // 다른 앱의 알람/푸시로 화면이 켜졌을 때, 기기의 화면 절전시간 내내 이 화면이
        // 켜져있지 않도록 일정 시간 뒤 자동으로 닫는다(배터리 소모 방지).
        private const val AUTO_DISMISS_DELAY_MS = 8000L
    }

    var saying : Quote? = null

    private lateinit var binding: ActivityMotivationLockerBinding // 자동 생성된 바인딩 클래스

    private val autoDismissHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMotivationLockerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 기존 잠금화면보다 먼저 나타나도록
        // 버전별로
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        // 핀/패턴/비밀번호가 설정된 기기에서는 잠금을 해제하지 않는다.
        // 해제 요청을 하지 않아야 스와이프 후 finish()될 때 시스템 잠금화면(PIN 입력)이 자연스럽게 나타난다.
        val isKeyguardSecure = keyguardManager.isKeyguardSecure

        if(Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1){
            // 잠금화면에서 보여지게
            setShowWhenLocked(true)
            // 잠금이 없는 기기에서만 기존 잠금화면 해제
            if (!isKeyguardSecure) {
                keyguardManager.requestDismissKeyguard(this, null)
            }

        }else{
            // 잠금화면에서 보여지게
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            // 잠금이 없는 기기에서만 기존 잠금화면 해제
            if (!isKeyguardSecure) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }
        }

        // 언어, 배경, 글자색, 글자 크기, 출처표기 여부
        val language = getInt("language")
        val backgroundType = getInt("backgroundType")
        val backgroundColor = getInt("backgroundColor")
        val backgroundGradientPreset = getInt("backgroundGradientPreset")
        val backgroundPhotoUri = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE).getString("backgroundPhotoUri", null)
        val textColor = getInt("textColor")
        val textSize = getInt("textSize")
        val showSource = getInt("showSource")

        setLanguage(language)
        setBackground(backgroundType, backgroundColor, backgroundGradientPreset, backgroundPhotoUri)
        setTextColor(textColor)
        setTextSize(textSize)

        // 출처 표기 안하면 투명하게
        if(showSource == 1)
            binding.writerTextView.setTextColor(getColor(R.color.colorTransparent))

        //시작, 끝점 계산해서 잠금해제
        var startX = 0
        var startY = 0

        var endX = 0
        var endY = 0

        binding.myLayout.setOnTouchListener { v, event ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    // 초기값
                    startX =  event.x.toInt()
                    startY =  event.y.toInt()

                    Log.d("start x", startX.toString() )
                    Log.d("start y", startY.toString() )
                }

                MotionEvent.ACTION_MOVE -> {
                    // 이동 값
                    endX = event.x.toInt()
                    endY = event.y.toInt()

                    Log.d("end x", endX.toString() )
                    Log.d("end y", endY.toString() )
                }

                   // 이동 끝내고 조건 맞으면 헤제
                    else -> {
                       // Log.d("gazaa", (((endX-startX)*(endX-startX)) + ((endY-startY)*(endY-startY))).toString())
                        if( ((endX- startX)*(endX - startX)) + ((endY - startY)*(endY- startY)) >= 80000 )
                            finish()
                    }
            }

            true
        }

        autoDismissHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY_MS)
    }

    override fun onDestroy() {
        super.onDestroy()
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
    }

    // 설정에 따라
    private fun getInt( key : String) : Int{
        val prefs = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
        val result = when (key) {
            "backgroundColor" -> {
                val bgColor = prefs.getInt("backgroundColorCategory", android.graphics.Color.WHITE)
                bgColor
            }
            "textColor" -> {
                val txtColor = prefs.getInt("textColorCategory", android.graphics.Color.BLACK)
                txtColor
            }
            else -> {
                val otherValue = prefs.getInt(key, 0)
                otherValue
            }
        }

        return result
    }

    // 언어
    private fun setLanguage(language: Int){
        // 랜덤으로 명언 선택 (QuoteRepository가 언어별 assets 로딩/캐싱을 담당)
        saying = QuoteRepository.getRandomQuote(this, language)
        // 글 보여줌
        binding.sayingTextView.text = saying?.quote
        // 작가 보여줌
        binding.writerTextView.text = saying?.writer
    }

    // 배경 (단색 / 그라데이션 / 사진)
    private fun setBackground(backgroundType: Int, backgroundColor: Int, gradientPresetIndex: Int, photoUriString: String?) {
        when (backgroundType) {
            1 -> setGradientBackground(gradientPresetIndex, backgroundColor)
            2 -> setPhotoBackground(photoUriString)
            else -> setSolidBackground(backgroundColor)
        }
    }

    // 단색
    private fun setSolidBackground(backgroundColor: Int) {
        binding.scrimView.visibility = View.GONE
        binding.photoBackgroundView.visibility = View.GONE
        Log.d("Background Color Hex", String.format("#%06X", 0xFFFFFF and backgroundColor))

        // Handle -1 (transparent) case by using default white
        val actualColor = if (backgroundColor == -1) {
            android.graphics.Color.WHITE
        } else {
            backgroundColor
        }

        binding.myLayout.setBackgroundColor(actualColor)
        applyStatusBarColor(actualColor)
    }

    // 그라데이션 (선택된 배경색을 기준으로 계산된 프리셋)
    private fun setGradientBackground(presetIndex: Int, baseColor: Int) {
        binding.scrimView.visibility = View.GONE
        binding.photoBackgroundView.visibility = View.GONE

        val presets = GradientPresets.forBaseColor(baseColor)
        val preset = presets.getOrNull(presetIndex) ?: presets[0]

        binding.myLayout.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(preset.startColor, preset.endColor)
        )

        val averageColor = android.graphics.Color.rgb(
            (android.graphics.Color.red(preset.startColor) + android.graphics.Color.red(preset.endColor)) / 2,
            (android.graphics.Color.green(preset.startColor) + android.graphics.Color.green(preset.endColor)) / 2,
            (android.graphics.Color.blue(preset.startColor) + android.graphics.Color.blue(preset.endColor)) / 2
        )
        applyStatusBarColor(averageColor)
    }

    // 사용자 사진 (URI가 없거나 더 이상 열 수 없으면 단색 흰 배경으로 안전하게 대체)
    private fun setPhotoBackground(photoUriString: String?) {
        if (photoUriString == null) {
            setSolidBackground(android.graphics.Color.WHITE)
            return
        }

        try {
            val uri = Uri.parse(photoUriString)
            val bitmap = decodeOrientedBitmap(uri) ?: throw java.io.IOException("사진을 디코딩하지 못함")

            binding.myLayout.background = null
            binding.photoBackgroundView.setImageBitmap(bitmap)
            binding.photoBackgroundView.visibility = View.VISIBLE
            binding.scrimView.visibility = View.VISIBLE
            // 스크림이 어두운 반투명이라 상태바 아이콘은 밝게 고정
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.decorView.systemUiVisibility = 0
        } catch (e: Exception) {
            Log.w("MotivationLockerActivity", "배경 사진을 불러오지 못함, 기본 배경으로 대체", e)
            setSolidBackground(android.graphics.Color.WHITE)
        }
    }

    // EXIF Orientation을 반영해 올바른 방향의 비트맵을 반환한다.
    // BitmapFactory.decodeStream()은 EXIF 회전 정보를 무시하므로, 가로로 찍힌 사진이
    // 그대로 세로로 디코딩되는 경우(카메라 회전 저장 방식)를 여기서 보정한다.
    private fun decodeOrientedBitmap(uri: Uri): Bitmap? {
        val orientation = contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun applyStatusBarColor(color: Int) {
        window.statusBarColor = color

        // 상태바 글씨 색상 결정 (배경색이 밝으면 어두운 글씨, 어두우면 밝은 글씨)
        val brightness = (android.graphics.Color.red(color) + android.graphics.Color.green(color) + android.graphics.Color.blue(color)) / 3
        if (brightness > 128) {
            // 밝은 배경색 -> 어두운 상태바 글씨
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            // 어두운 배경색 -> 밝은 상태바 글씨
            window.decorView.systemUiVisibility = 0
        }
    }

    // 글자색
    // Color.WHITE(0xFFFFFFFF)는 부호있는 Int로 -1과 같은 값이라, 여기서 -1을 "미설정"으로
    // 취급해 검정으로 강제하면 사용자가 고른 흰색 글자색이 항상 검정으로 보이는 버그가 생긴다.
    private fun setTextColor(textColor : Int ){
        Log.d("Selected Text Color ", textColor.toString() + "")
        Log.d("Text Color Hex", String.format("#%06X", 0xFFFFFF and textColor))

        binding.sayingTextView.setTextColor(textColor)
        binding.writerTextView.setTextColor(textColor)
    }

    // 글자크기
    private fun setTextSize(textSize : Int){
        when(textSize){
            0 ->  {
                binding.sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                binding.blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                binding.writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            }
            1 ->  {
                binding.sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36f)
                binding.blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36f)
                binding.writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25f)
            }
            2 ->  {
                binding.sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)
                binding.blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)
                binding.writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35f)

            }
            3 ->  {
                binding.sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 75f)
                binding.blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 75f)
                binding.writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)

            }
            4 ->  {
                binding.sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 100f)
                binding.blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 100f)
                binding.writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 70f)

            }
        }

    }

}