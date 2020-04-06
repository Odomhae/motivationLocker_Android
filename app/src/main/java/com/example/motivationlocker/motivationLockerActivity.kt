package com.example.motivationlocker

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

        // 언어, 배경색, 글자색, 글자 크기
        val language = getInt("language")
        val backgroundColor = getInt("backgroundColor")
        val textColor = getInt("textColor")
        val textSize = getInt("textSize")

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
                // 작가? 보여줌
                //    writerTextView.text = saying?.getString("writer")
            }
            //
            1-> {
                val json = assets.open("korean.json").reader().readText()
                val sayingArray = JSONArray(json)
                saying = sayingArray.getJSONObject(Random.nextInt(sayingArray.length()))
                sayingTextView.text = saying?.getString("quote")
                // 작가?
                //    writerTextView.text = saying?.getString("writer")

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
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBlack))
                window.statusBarColor = resources.getColor(R.color.colorBlack)
            }
            2 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorRed))
                window.statusBarColor = resources.getColor(R.color.colorRed)
            }
            3 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBeige))
                window.statusBarColor = resources.getColor(R.color.colorBeige)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            4 -> {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorBlue))
                window.statusBarColor = resources.getColor(R.color.colorBlue)
            }
            5 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorGreen))
                window.statusBarColor = resources.getColor(R.color.colorGreen)
            }
            6 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorOrange))
                window.statusBarColor = resources.getColor(R.color.colorOrange)
            }
            7 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorYellow))
                window.statusBarColor = resources.getColor(R.color.colorYellow)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            8 ->  {
                myLayout.setBackgroundColor(resources.getColor(R.color.colorPink))
                window.statusBarColor = resources.getColor(R.color.colorPink)
            }
        }
        // 글자색
        when(textColor){
            0 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorWhite))
            1 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorBlack))
            2 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorRed))
            3 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorBeige))
            4 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorBlue))
            5 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorGreen))
            6 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorOrange))
            7 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorYellow))
            8 ->  sayingTextView.setTextColor(resources.getColor(R.color.colorPink))
        }
        // 글자크기
        when(textSize){
            0 ->  sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            1 ->  sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36f)
            2 ->  sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50f)
        }


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