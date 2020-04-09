package com.odom.motivationlocker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import com.odom.motivationlocker.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val fragment = MyPreferenceFragment()
    // 광고
    lateinit var mAdView : AdView
    private lateinit var mInterstitialAd: InterstitialAd
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
        setContentView(R.layout.activity_main)

//        // 초기실행시 전면 광고
//        mInterstitialAd = InterstitialAd(this)
//        //전면 광고 테스트 전용 광고 단위 ID
//        mInterstitialAd.adUnitId = resources.getString(R.string.TEST_fullscreen_ad_unit_id)
//        mInterstitialAd.loadAd(AdRequest.Builder().build())
//
//        mInterstitialAd.adListener = object : AdListener() {
//            override fun onAdLoaded() {
//                if (mInterstitialAd.isLoaded) {
//                    mInterstitialAd.show()
//                }else {
//                    Log.d("Msg ", "The interstitial wasn't loaded yet.")
//                }
//            }
//
//            override fun onAdOpened() {}
//            override fun onAdFailedToLoad(errorCode: Int) {}
//        }
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
        adView.addView(mAdView)
        loadBanner()
    }

    private fun loadBanner() {
        mAdView.adUnitId = resources.getString(R.string.TEST_banner_ad_unit_id)
        mAdView.adSize = adSize

        val adRequest = AdRequest
            .Builder()
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest)
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
            languageCategoryPref.summary = preferenceScreen.sharedPreferences.getString("language", "")
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
