package com.odom.motivationlocker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.ads.*
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.odom.motivationlocker.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    // 뒤로가기 2번 종료
    var backPressTime = 0L
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // 뒤로가기 클릭 시 실행시킬 코드
            if (System.currentTimeMillis() - backPressTime > 2000){
                backPressTime = System.currentTimeMillis()
                Toast.makeText(this@MainActivity, "한번 더 누르면 앱이 종료됩니다" , Toast.LENGTH_SHORT).show()
                reviewApp()
            } else {
                reviewApp()
                finish()
            }
        }
    }

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

    private lateinit var binding: ActivityMainBinding // 자동 생성된 바인딩 클래스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = resources.getColor(R.color.colorGray)
        window.setTitleColor(resources.getColor(R.color.colorGray))
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val contentView: View = this.findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(contentView, object : OnApplyWindowInsetsListener {
            override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                val innerPadding = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                v.setPadding(0, innerPadding.top, 0, innerPadding.bottom)

                return insets
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val isLightStatusBars =
                AppCompatDelegate.getDefaultNightMode() !== AppCompatDelegate.MODE_NIGHT_YES
            val compat: WindowInsetsControllerCompat = WindowInsetsControllerCompat(
                this.getWindow(), this.getWindow().getDecorView()
            )
            compat.setAppearanceLightStatusBars(isLightStatusBars)
            compat.setAppearanceLightNavigationBars(isLightStatusBars)
        }

        this.onBackPressedDispatcher.addCallback(this, callback)

        supportFragmentManager.beginTransaction().replace(R.id.preferenceContent, SettingPreferencesFragment()).commit()

        checkPermission()
    }

    private fun checkPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent =  Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, PermissionsCode)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }
    }

    //  앱 리뷰
    private fun reviewApp() {
        val manager = ReviewManagerFactory.create(this@MainActivity)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                manager.launchReviewFlow(this@MainActivity, reviewInfo)
                    .addOnCompleteListener { task1: Task<Void?> ->
                        if (task1.isSuccessful) {
                            Log.d("TAG", "Review Success")
                        }
                    }
            } else {
                Log.d("TAG", "Review Error")
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

        // 배너 광고
        MobileAds.initialize(this) {}
        mAdView = AdView(this)
        binding.adMobView.addView(mAdView)
        loadBanner()
    }

    private fun loadBanner() {
        mAdView.adUnitId = resources.getString(R.string.REAL_banner_ad_unit_id)
        mAdView.setAdSize(adSize)

        val adRequest = AdRequest
            .Builder()
            .build()
           // .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest)
    }


    class SettingPreferencesFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref, rootKey)

            val switchPreference = findPreference<SwitchPreferenceCompat>("useLockScreen")
            val languagePreference = findPreference<androidx.preference.ListPreference>("languageCategory")
            val backGroundColorPreference = findPreference<androidx.preference.ListPreference>("backgroundColorCategory")
            val textColorPreference = findPreference<androidx.preference.ListPreference>("textColorCategory")
            val textSizePreference = findPreference<androidx.preference.ListPreference>("textSizeCategory")
            val switchSourcePreference = findPreference<SwitchPreferenceCompat>("showSourcePref")


            // 앱이 시작됬을대 이미 퀴즈잠금화면 사용이 체크되어있으면 서비스 실행
            if (switchPreference?.isChecked!!) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(Intent(requireActivity(), LockScreenService::class.java))
                } else {
                    requireContext().startService(Intent(requireActivity(), LockScreenService::class.java))
                }

            }

            languagePreference?.summary = languagePreference?.entries?.get(getInt("language"))
            backGroundColorPreference?.summary = backGroundColorPreference?.entries?.get(getInt("backgroundColor"))
            textColorPreference?.summary = textColorPreference?.entries?.get(getInt("textColor"))
            textSizePreference?.summary = textSizePreference?.entries?.get(getInt("textSize"))


            // 사용여부
            switchPreference.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    // 잠금화면 사용이 체크된 경우 lockScreenService 실행
                    Log.d("checked", "체크")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireActivity().startForegroundService(Intent(requireActivity(), LockScreenService::class.java))
                    } else {
                        requireContext().startService(Intent(requireActivity(), LockScreenService::class.java))
                    }
                } else {
                    // 사용 체크 안됬으면 서비스 중단
                    requireContext().stopService(Intent(requireActivity(), LockScreenService::class.java))
                }
                true
            }

            // 언어
            languagePreference?.setOnPreferenceChangeListener { _, newValue ->
                languagePreference.summary = newValue.toString()

                val index = languagePreference.findIndexOfValue(newValue.toString())
                setInts(requireContext(), "language", index)

                true
            }

            // 배경색
            backGroundColorPreference?.setOnPreferenceChangeListener { _, newValue ->
                backGroundColorPreference.summary = newValue.toString()

                val index = backGroundColorPreference.findIndexOfValue(newValue.toString())
                setInts(requireContext(), "backgroundColor", index)

                true
            }

            // 글자색
            textColorPreference?.setOnPreferenceChangeListener { _, newValue ->
                textColorPreference.summary = newValue.toString()

                val index = textColorPreference.findIndexOfValue(newValue.toString())
                setInts(requireContext(), "textColor", index)

                true
            }

            // 글자 크기
            textSizePreference?.setOnPreferenceChangeListener { _, newValue ->
                textSizePreference.summary = newValue.toString()

                val index = textSizePreference.findIndexOfValue(newValue.toString())
                setInts(requireContext(), "textSize", index)

                true
            }

            // 출처 표시
            switchSourcePreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    //출처 표기시 0
                    setInts(requireContext(), "showSource", 0)
                } else {
                    //출처 미표기시 1
                    setInts(requireContext(), "showSource", 1)
                }

                true
            }

        }

        private fun setInts(context: Context, key : String, value : Int) {
            val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            val editor = prefs!!.edit()
            editor.putInt(key, value).apply()
        }

        // 설정에 따라
        private fun getInt( key : String) : Int{
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            return prefs.getInt(key, 0)
        }

    }

}
