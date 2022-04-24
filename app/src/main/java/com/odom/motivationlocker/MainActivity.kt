package com.odom.motivationlocker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val fragment = MyPreferenceFragment()
    //퍼미션 응답 처리 코드
    private val PermissionsCode = 100
    // 광고
    lateinit var mAdView : AdView
    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density
            val adWidthPixels = outMetrics.widthPixels.toFloat()
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = resources.getColor(R.color.colorGray)
        window.setTitleColor(resources.getColor(R.color.colorGray))
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_main)
        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent =  Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivityForResult(intent, PermissionsCode)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionsCode) {
            if (Settings.canDrawOverlays(this)) {
                Log.d("TAG", "권한 설정됨")
                val toast = Toast.makeText(applicationContext, R.string.permission_set_message, Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP,  Gravity.CENTER, 550)
                toast.show()
                onResume()
            }else{
                Log.d("TAG", "권한 거절됨")
                finish()
                val toast = Toast.makeText(applicationContext, R.string.permission_denied_message, Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP, Gravity.CENTER, 550)
                toast.show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        window.statusBarColor = resources.getColor(R.color.colorGray)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // preferenceContent 아이디 부분에 fragment 넣기
        fragmentManager.beginTransaction().replace(R.id.preferenceContent, fragment).commit()

        // 배너 광고
        MobileAds.initialize(this) {}
        mAdView = AdView(this)
        adMobView.addView(mAdView)
        loadBanner()
    }

    private fun loadBanner() {
        mAdView.adUnitId = resources.getString(R.string.REAL_banner_ad_unit_id)
        mAdView.adSize = adSize

        val adRequest = AdRequest
            .Builder()
            .build()
           // .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest)
    }

    class MyPreferenceFragment : PreferenceFragment(){

        fun setInts(context: Context, key : String, value : Int) {
            val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            val editor = prefs!!.edit()
            editor.putInt(key, value).apply()
        }

        // 설정에 따라
        private fun getInt( key : String) : Int{
            val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            return prefs.getInt(key, 0)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            //환경설정 리소스 파일
            addPreferencesFromResource(R.xml.pref)

            // 언어 종류 요약정보에 현재 선택된 항목 보여줌
            val languageCategoryPref = findPreference("languageCategory") as ListPreference
            languageCategoryPref.summary = languageCategoryPref.entries[getInt("language")]
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
            backgroundColorCategoryPref.summary = backgroundColorCategoryPref.entries[getInt("backgroundColor")]
            backgroundColorCategoryPref.setOnPreferenceChangeListener { preference, newValue ->
                val index = backgroundColorCategoryPref.findIndexOfValue(newValue.toString())
                setInts(context, "backgroundColor", index)
                backgroundColorCategoryPref.summary = backgroundColorCategoryPref.entries[index]
                Log.d("선택한 배경색", backgroundColorCategoryPref.summary.toString())
                true
            }

            // 글자색
            val textColorCategoryPref = findPreference("textColorCategory") as ListPreference
            textColorCategoryPref.summary = textColorCategoryPref.entries[getInt("textColor")]
            textColorCategoryPref.setOnPreferenceChangeListener { preference, newValue ->
                val index = textColorCategoryPref.findIndexOfValue(newValue.toString())
                setInts(context, "textColor", index)
                textColorCategoryPref.summary = textColorCategoryPref.entries[index]
                Log.d("선택한 글자색", textColorCategoryPref.summary.toString())
                true
            }

            //글자크기
            val textSizeCategoryPref = findPreference("textSizeCategory") as ListPreference
            textSizeCategoryPref.summary = textSizeCategoryPref.entries[getInt("textSize")]
            textSizeCategoryPref.setOnPreferenceChangeListener{preference, newValue ->
                val index = textSizeCategoryPref.findIndexOfValue(newValue.toString())
                setInts(context, "textSize", index)
                textSizeCategoryPref.summary = textSizeCategoryPref.entries[index]
                Log.d("선택한 글자크기", textSizeCategoryPref.summary.toString())
                true
            }

            // 출처 표시
            val showSourcePref = findPreference("showSourcePref") as SwitchPreference
            showSourcePref.setOnPreferenceClickListener {
                when{
                    //출처 표기시 0
                    showSourcePref.isChecked -> setInts(context, "showSource", 0)
                    //출처 미표기시 1
                    else -> setInts(context, "showSource", 1)
                }
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
