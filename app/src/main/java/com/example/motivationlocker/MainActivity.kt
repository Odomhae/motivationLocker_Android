package com.example.motivationlocker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.*
import android.util.Log


class MainActivity : AppCompatActivity() {

    val fragment = MyPreferenceFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // preferenceContent 아이디 부분에 fragment 넣기
        fragmentManager.beginTransaction().replace(R.id.preferenceContent, fragment).commit()
    }

    class MyPreferenceFragment : PreferenceFragment(){

        fun setInts(context: Context, key : String, value : Int) {
            val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            val editor = prefs!!.edit()
            editor.putInt(key, value).apply()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            //환경설정 리소스 파일
            addPreferencesFromResource(R.xml.pref)

            // 언어 종류 요약정보에 현재 선택된 항목 보여줌
            val languageCategoryPref = findPreference("languageCategory") as ListPreference
            // 요약정보도 같이 변경
            languageCategoryPref.setOnPreferenceChangeListener { preference, newValue ->
                val index = languageCategoryPref.findIndexOfValue(newValue.toString())
                // 언어 정보 저장
                setInts(context, "language", index)
                languageCategoryPref.summary = languageCategoryPref.entries[index]
                Log.d("선택한 언어", languageCategoryPref.summary.toString())
                true
            }

            // 배경색
            val backgroundColorCategoryPref = findPreference("backgroundColorCategory") as ListPreference
            backgroundColorCategoryPref.setOnPreferenceChangeListener { preference, newValue ->
                val index = backgroundColorCategoryPref.findIndexOfValue(newValue.toString())
                setInts(context, "backgroundColor", index)
                backgroundColorCategoryPref.summary = backgroundColorCategoryPref.entries[index]
                Log.d("선택한 배경색", backgroundColorCategoryPref.summary.toString())
                true
            }

            // 글자색
            val textColorCategoryPref = findPreference("textColorCategory") as ListPreference
            textColorCategoryPref.setOnPreferenceChangeListener { preference, newValue ->
                val index = textColorCategoryPref.findIndexOfValue(newValue.toString())
                setInts(context, "textColor", index)
                textColorCategoryPref.summary = textColorCategoryPref.entries[index]
                Log.d("선택한 글자색", textColorCategoryPref.summary.toString())
                true
            }

            //글자크기
            val textSizeCategoryPref = findPreference("textSizeCategory") as ListPreference
            textSizeCategoryPref.setOnPreferenceChangeListener{preference, newValue ->
                val index = textSizeCategoryPref.findIndexOfValue(newValue.toString())
                setInts(context, "textSize", index)
                textSizeCategoryPref.summary = textSizeCategoryPref.entries[index]
                Log.d("선택한 글자크기", textSizeCategoryPref.summary.toString())
                true
            }

            // 잠금화면 사용 스위치 객체 사용
            val useLockScreenPref = findPreference("useLockScreen") as SwitchPreference
            // 클릭됬을때
            useLockScreenPref.setOnPreferenceClickListener {
                when{
                    // 퀴즈 잠금화면 사용이 체크된 경우 lockScreenService 실행
                    useLockScreenPref.isChecked ->{
                        Log.d("checked", "체크")
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            activity.startForegroundService(Intent(activity, LockScreenService::class.java))
                        }else{
                            activity.startService(Intent(activity, LockScreenService::class.java))
                        }
                    }
                    // 사용 체크 안됬으면 서비스 중단
                    else -> activity.stopService(Intent(activity, LockScreenService::class.java))
                }
                true
            }

            // 앱이 시작됬을대 이미 퀴즈잠금화면 사용이 체크되어있으면 서비스 실행
            if(useLockScreenPref.isChecked){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    activity.startForegroundService(Intent(activity, LockScreenService::class.java))
                }else{
                    activity.startService(Intent(activity, LockScreenService::class.java))
                }

            }

        }
    }
}
