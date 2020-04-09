package com.odom.motivationlocker

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginTop
import com.odom.motivationlocker.R
import kotlinx.android.synthetic.main.activity_motivation_locker.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class motivationLockerActivity : AppCompatActivity() {

    var saying : JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_motivation_locker)
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

        setContentView(R.layout.activity_motivation_locker)

        // 설정에 따라
        fun getInt( key : String) : Int{
            val prefs = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            return prefs.getInt(key, 0)
        }

        // 언어, 배경색, 글자색, 글자 크기, 출처표기 여부
        val language = getInt("language")
        val backgroundColor = getInt("backgroundColor")
        val textColor = getInt("textColor")
        val textSize = getInt("textSize")
        val showSource = getInt("showSource")

        // 언어
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
                sayingTextView.text = saying?.getString("quote")
                // 작가 보여줌
                writerTextView.text = saying?.getString("writer")
            }
            //
            1-> {
                val json = assets.open("korean.json").reader().readText()
                val sayingArray = JSONArray(json)
                saying = sayingArray.getJSONObject(Random.nextInt(sayingArray.length()))
                sayingTextView.text = saying?.getString("quote")
                // 작가?
                writerTextView.text = saying?.getString("writer")

            }
        }

        // 배경색
        when(backgroundColor){
            0 -> {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorWhite))
                // 삳태바도 같은 색으로 api 21 이상
                window.statusBarColor = resources.getColor(R.color.colorWhite)
                //상태바 글씨 보이게
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            1 -> {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorGray))
                window.statusBarColor = resources.getColor(R.color.colorGray)
            }
            2 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBlack))
                window.statusBarColor = resources.getColor(R.color.colorBlack)
            }
            3 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorRed))
                window.statusBarColor = resources.getColor(R.color.colorRed)
            }
            4 -> {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorCrimson))
                window.statusBarColor = resources.getColor(R.color.colorCrimson)
            }
            5 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorSalmon))
                window.statusBarColor = resources.getColor(R.color.colorSalmon)
            }
            6 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBeige))
                window.statusBarColor = resources.getColor(R.color.colorBeige)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            7 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorOrange))
                window.statusBarColor = resources.getColor(R.color.colorOrange)
            }
            8 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBrown))
                window.statusBarColor = resources.getColor(R.color.colorBrown)
            }
            9 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorWalnut))
                window.statusBarColor = resources.getColor(R.color.colorWalnut)
            }
            10 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBlue))
                window.statusBarColor = resources.getColor(R.color.colorBlue)
            }
            11 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorMalibu))
                window.statusBarColor = resources.getColor(R.color.colorMalibu)
            }
            12 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorGreen))
                window.statusBarColor = resources.getColor(R.color.colorGreen)
            }
            13 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorYellowGreen))
                window.statusBarColor = resources.getColor(R.color.colorYellowGreen)
            }
            14 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorMint))
                window.statusBarColor = resources.getColor(R.color.colorMint)
            }
            15 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorYellow))
                window.statusBarColor = resources.getColor(R.color.colorYellow)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

            }
            16 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorPink))
                window.statusBarColor = resources.getColor(R.color.colorPink)
            }
            17 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorViolet))
                window.statusBarColor = resources.getColor(R.color.colorViolet)
            }
            18 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorMagenta))
                window.statusBarColor = resources.getColor(R.color.colorMagenta)
            }
            19 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorPurple))
                window.statusBarColor = resources.getColor(R.color.colorPurple)
            }

        }
        // 글자색
        when(textColor){
            0 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorWhite))
                writerTextView.setTextColor(resources.getColor(R.color.colorWhite))
            }
            1 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorGray))
                writerTextView.setTextColor(resources.getColor(R.color.colorGray))
            }
            2 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorBlack))
                writerTextView.setTextColor(resources.getColor(R.color.colorBlack))
            }
            3 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorRed))
                writerTextView.setTextColor(resources.getColor(R.color.colorRed))
            }
            4 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorCrimson))
                writerTextView.setTextColor(resources.getColor(R.color.colorCrimson))
            }
            5 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorSalmon))
                writerTextView.setTextColor(resources.getColor(R.color.colorSalmon))
            }
            6 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorBeige))
                writerTextView.setTextColor(resources.getColor(R.color.colorBeige))
            }
            7 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorOrange))
                writerTextView.setTextColor(resources.getColor(R.color.colorOrange))
            }
            8 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorBrown))
                writerTextView.setTextColor(resources.getColor(R.color.colorBrown))
            }
            9 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorWalnut))
                writerTextView.setTextColor(resources.getColor(R.color.colorWalnut))
            }
            10 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorBlue))
                writerTextView.setTextColor(resources.getColor(R.color.colorBlue))
            }
            11 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorMalibu))
                writerTextView.setTextColor(resources.getColor(R.color.colorMalibu))
            }
            12 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorGreen))
                writerTextView.setTextColor(resources.getColor(R.color.colorGreen))
            }
            13 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorYellowGreen))
                writerTextView.setTextColor(resources.getColor(R.color.colorYellowGreen))
            }
            14 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorMint))
                writerTextView.setTextColor(resources.getColor(R.color.colorMint))
            }
            15 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorYellow))
                writerTextView.setTextColor(resources.getColor(R.color.colorYellow))
            }
            16 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorPink))
                writerTextView.setTextColor(resources.getColor(R.color.colorPink))
            }
            17 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorViolet))
                writerTextView.setTextColor(resources.getColor(R.color.colorViolet))
            }
            18 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorMagenta))
                writerTextView.setTextColor(resources.getColor(R.color.colorMagenta))
            }
            19 ->  {
                sayingTextView.setTextColor(resources.getColor(R.color.colorPurple))
                writerTextView.setTextColor(resources.getColor(R.color.colorPurple))
            }
        }
        // 글자크기
        when(textSize){
            0 ->  {
                sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            }
            1 ->  {
                sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36f)
                blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36f)
                writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25f)
            }
            2 ->  {
                sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)
                blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)
                writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35f)

            }
            3 ->  {
                sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 75f)
                blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 75f)
                writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)

            }
            4 ->  {
                sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 100f)
                blankTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 100f)
                writerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 70f)

            }
        }

        // 출처 표기 안하면 투명하게
        if(showSource == 1)
            writerTextView.setTextColor(resources.getColor(R.color.colorTransparent))

        //시작, 끝점 계산해서 잠금해제
        var startX = 0
        var startY = 0

        var endX = 0
        var endY = 0

        myLayout.setOnTouchListener { v, event ->
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
                        Log.d("gazaa", (((endX-startX)*(endX-startX)) + ((endY-startY)*(endY-startY))).toString())
                        if( ((endX- startX)*(endX - startX)) + ((endY - startY)*(endY- startY)) >= 80000 )
                            finish()
                    }
            }

            true
        }

    }

}