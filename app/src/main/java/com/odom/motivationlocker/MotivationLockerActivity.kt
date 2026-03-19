package com.odom.motivationlocker

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.odom.motivationlocker.databinding.ActivityMotivationLockerBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class MotivationLockerActivity : AppCompatActivity() {

    var saying : JSONObject? = null

    private lateinit var binding: ActivityMotivationLockerBinding // 자동 생성된 바인딩 클래스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMotivationLockerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 기존 잠금화면보다 먼저 나타나도록
        // 버전별로
        if(Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1){
            // 잠금화면에서 보여지게
            setShowWhenLocked(true)
            // 기존 잠금화면 해제
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)

        }else{
            // 잠금화면에서 보여지게
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            // 기존 잠금화면 해제
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }

        // 언어, 배경색, 글자색, 글자 크기, 출처표기 여부
        val language = getInt("language")
        val backgroundColor = getInt("backgroundColor")
        val textColor = getInt("textColor")
        val textSize = getInt("textSize")
        val showSource = getInt("showSource")

        setLanguage(language)
        setBackgroundColor(backgroundColor)
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
        when(language){
            // 영어
            0 -> {
                // 글 데이터 가져옴
                // asset 폴더의 korean.json 파일
                val json = assets.open("English.json").reader().readText()
                val sayingArray = JSONArray(json)
                // 랜덤으로 퀴즈 선택
                saying = sayingArray.getJSONObject(Random.nextInt(sayingArray.length()))
                //글 보여줌
                binding.sayingTextView.text = saying?.getString("quote")
                // 작가 보여줌
                binding.writerTextView.text = saying?.getString("writer")
            }
            //
            1-> {
                val json = assets.open("korean.json").reader().readText()
                val sayingArray = JSONArray(json)
                saying = sayingArray.getJSONObject(Random.nextInt(sayingArray.length()))
                binding.sayingTextView.text = saying?.getString("quote")
                binding.writerTextView.text = saying?.getString("writer")
            }
        }

    }

    // 배경색
    private fun setBackgroundColor(backgroundColor : Int){
        Log.d("Background Color Hex", String.format("#%06X", 0xFFFFFF and backgroundColor))
        
        // Handle -1 (transparent) case by using default white
        val actualColor = if (backgroundColor == -1) {
            android.graphics.Color.WHITE
        } else {
            backgroundColor
        }
        
        // Apply the actual stored color
        binding.myLayout.setBackgroundColor(actualColor)
        window.statusBarColor = actualColor
        
        // 상태바 글씨 색상 결정 (배경색이 밝으면 어두운 글씨, 어두우면 밝은 글씨)
        val brightness = (android.graphics.Color.red(actualColor) + android.graphics.Color.green(actualColor) + android.graphics.Color.blue(actualColor)) / 3
        if (brightness > 128) {
            // 밝은 배경색 -> 어두운 상태바 글씨
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            // 어두운 배경색 -> 밝은 상태바 글씨
            window.decorView.systemUiVisibility = 0
        }
    }

    // 글자색
    private fun setTextColor(textColor : Int ){
        Log.d("Selected Text Color ", textColor.toString() + "")
        Log.d("Text Color Hex", String.format("#%06X", 0xFFFFFF and textColor))
        
        // Handle -1 (transparent) case by using default black
        val actualColor = if (textColor == -1) {
            android.graphics.Color.BLACK
        } else {
            textColor
        }

        binding.sayingTextView.setTextColor(actualColor)
        binding.writerTextView.setTextColor(actualColor)
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