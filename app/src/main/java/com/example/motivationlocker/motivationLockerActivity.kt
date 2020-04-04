package com.example.motivationlocker

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.TypedValue
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

        // 글 데이터 가져옴
        // asset 폴더의 capital.json 파일
        val json = assets.open("korean.json").reader().readText()
        val sayingArray = JSONArray(json)

        // 랜덤으로 퀴즈 선택
        saying = sayingArray.getJSONObject(Random.nextInt(sayingArray.length()))
        //글 보여줌
        sayingTextView.text = saying?.getString("quote")
        // 작가? 보여줌

        // 설정에 따라
        sayingTextView.setTextColor(resources.getColor(R.color.colorRed))
        sayingTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36f)
        myLayout.setBackgroundColor(resources.getColor(R.color.colorBeige))

    }

}