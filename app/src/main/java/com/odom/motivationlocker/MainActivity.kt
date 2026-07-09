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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showExitConfirmDialog()
        }
    }

    //퍼미션 응답 처리 코드
    private val PermissionsCode = 100
    // 광고
    lateinit var mAdView : AdView
    // 뒤로가기 종료 확인 다이얼로그에 표시할, 미리 로드해 둔 배너 광고
    private lateinit var exitAdView: AdView
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

        window.statusBarColor = resources.getColor(R.color.settingsPrimary)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

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
        // POST_NOTIFICATIONS(Android 13+)는 데일리 알림 스위치를 켤 때만 맥락 있게 요청한다.
        // (SettingPreferencesFragment의 dailyNotificationEnabled 리스너 참고)
    }

    // 뒤로가기 시 미리 로드해 둔 배너 광고가 포함된 종료 확인 다이얼로그 표시
    private fun showExitConfirmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_confirm, null)
        val adContainer = dialogView.findViewById<FrameLayout>(R.id.exitAdContainer)

        // 다이얼로그를 여러 번 열어도 안전하도록, 이전에 붙어 있던 부모가 있으면 먼저 제거
        (exitAdView.parent as? ViewGroup)?.removeView(exitAdView)
        adContainer.addView(exitAdView)

        AlertDialog.Builder(this)
            .setMessage(R.string.exit_confirm_message)
            .setView(dialogView)
            .setPositiveButton(R.string.exit_confirm_positive) { _, _ ->
                reviewApp()
                finish()
            }
            .setNegativeButton(R.string.exit_confirm_negative, null)
            .show()
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

        window.statusBarColor = resources.getColor(R.color.settingsPrimary)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // 배너 광고
        MobileAds.initialize(this) {}
        mAdView = AdView(this)
        binding.adMobView.addView(mAdView)
        loadBanner()

        // 뒤로가기 종료 확인 다이얼로그용 배너 광고를 미리 로드(다이얼로그가 뜰 때 바로 보이도록)
        exitAdView = AdView(this)
        exitAdView.adUnitId = resources.getString(R.string.TEST_banner_ad_unit_id)
        exitAdView.setAdSize(AdSize.BANNER)
        exitAdView.loadAd(AdRequest.Builder().build())
    }

    private fun loadBanner() {
        mAdView.adUnitId = resources.getString(R.string.TEST_banner_ad_unit_id)
        mAdView.setAdSize(adSize)

        val adRequest = AdRequest
            .Builder()
            .build()
           // .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build()

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest)
    }


    class SettingPreferencesFragment : PreferenceFragmentCompat() {

        companion object {
            private const val DAILY_NOTIFICATION_PERMISSION_CODE = 1001
        }

        private lateinit var adManager: AdManager

        // 색상 변경 카운트 (3번 변경 시 광고 표시)
        private var colorChangeCount = 0

        // Photo Picker는 Fragment가 STARTED 상태가 되기 전에 등록되어야 하므로 필드로 선언
        private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
                prefs.edit().putString("backgroundPhotoUri", uri.toString()).apply()

                findPreference<androidx.preference.Preference>("backgroundPhotoPicker")?.summary =
                    getString(R.string.background_photo_selected)

                trackColorChange()
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref, rootKey)

            adManager = AdManager(requireContext())

            // SharedPreferences에서 colorChangeCount 초기화
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            colorChangeCount = prefs.getInt("colorChangeCount", 0)

            val switchPreference = findPreference<SwitchPreferenceCompat>("useLockScreen")
            val languagePreference = findPreference<androidx.preference.ListPreference>("languageCategory")
            val backGroundColorPreference = findPreference<ColorSelectorDialogPreference>("backgroundColorCategory")
            val textColorPreference = findPreference<ColorSelectorDialogPreference>("textColorCategory")
            val textSizePreference = findPreference<androidx.preference.ListPreference>("textSizeCategory")
            val switchSourcePreference = findPreference<SwitchPreferenceCompat>("showSourcePref")
            val dailyNotificationPreference = findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled")
            val backgroundTypePreference = findPreference<androidx.preference.ListPreference>("backgroundTypeCategory")
            val backgroundGradientPreference = findPreference<GradientSelectorDialogPreference>("backgroundGradientCategory")
            val backgroundPhotoPickerPreference = findPreference<androidx.preference.Preference>("backgroundPhotoPicker")


            // 앱이 시작됬을대 이미 퀴즈잠금화면 사용이 체크되어있으면 서비스 실행
            if (switchPreference?.isChecked!!) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(Intent(requireActivity(), LockScreenService::class.java))
                } else {
                    requireContext().startService(Intent(requireActivity(), LockScreenService::class.java))
                }

            }

            languagePreference?.summary = languagePreference?.entries?.get(getInt("language"))
          //  backGroundColorPreference?.summary = String.format("#%06X", 0xFFFFFF and getInt("backgroundColor"))
          //  textColorPreference?.summary = String.format("#%06X", 0xFFFFFF and getInt("textColor"))
            textSizePreference?.summary = textSizePreference?.entries?.get(getInt("textSize"))

            // 배경 종류 - 저장된 값에 따라 그라데이션/사진 설정 항목만 노출
            val backgroundType = getInt("backgroundType")
            backgroundTypePreference?.summary = backgroundTypePreference?.entries?.get(backgroundType)
            backgroundGradientPreference?.isVisible = backgroundType == 1
            backgroundPhotoPickerPreference?.isVisible = backgroundType == 2
            backgroundPhotoPickerPreference?.summary = if (prefs.getString("backgroundPhotoUri", null) != null) {
                getString(R.string.background_photo_selected)
            } else {
                getString(R.string.background_photo_not_selected)
            }


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
                val color = newValue as Int
                Log.d("==ttMainActivity", "Background color hex: ${String.format("#%06X", 0xFFFFFF and color)}")
               // backGroundColorPreference.summary = String.format("#%06X", 0xFFFFFF and color)
                setInts(requireContext(), "backgroundColorCategory", color)
                
                // 색상 변경 카운트 증가 및 광고 처리
                trackColorChange()
                
                true
            }

            // 글자색
            textColorPreference?.setOnPreferenceChangeListener { _, newValue ->
                val color = newValue as Int
                Log.d("==ttMainActivity", "Text color hex: ${String.format("#%06X", 0xFFFFFF and color)}")
              //  textColorPreference.summary = String.format("#%06X", 0xFFFFFF and color)
                setInts(requireContext(), "textColorCategory", color)
                
                // 색상 변경 카운트 증가 및 광고 처리
                trackColorChange()
                
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

            // 데일리 명언 알림
            dailyNotificationPreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // 권한 응답 전까지는 스위치를 켜지 않고, 응답 콜백에서 결과에 따라 처리
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), DAILY_NOTIFICATION_PERMISSION_CODE)
                        false
                    } else {
                        DailyNotificationScheduler.schedule(requireContext())
                        true
                    }
                } else {
                    DailyNotificationScheduler.cancel(requireContext())
                    true
                }
            }

            // 배경 종류
            backgroundTypePreference?.setOnPreferenceChangeListener { _, newValue ->
                backgroundTypePreference.summary = newValue.toString()

                val index = backgroundTypePreference.findIndexOfValue(newValue.toString())
                setInts(requireContext(), "backgroundType", index)

                backgroundGradientPreference?.isVisible = index == 1
                backgroundPhotoPickerPreference?.isVisible = index == 2

                true
            }

            // 그라데이션 프리셋
            backgroundGradientPreference?.setOnPreferenceChangeListener { _, newValue ->
                val presetIndex = newValue as Int
                setInts(requireContext(), "backgroundGradientPreset", presetIndex)

                // 색상 변경 카운트 증가 및 광고 처리
                trackColorChange()

                true
            }

            // 배경 사진 선택
            backgroundPhotoPickerPreference?.setOnPreferenceClickListener {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                true
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            if (requestCode == DAILY_NOTIFICATION_PERMISSION_CODE) {
                val dailyNotificationPreference = findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled")
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dailyNotificationPreference?.isChecked = true
                    DailyNotificationScheduler.schedule(requireContext())
                } else {
                    dailyNotificationPreference?.isChecked = false
                    Toast.makeText(requireContext(), R.string.daily_notification_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 색상 변경 카운트 및 광고 처리
        private fun trackColorChange() {
            
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            colorChangeCount = prefs.getInt("colorChangeCount", 0)
            colorChangeCount++
            
            // SharedPreferences에 colorChangeCount 저장
            val editor = prefs.edit()
            editor.putInt("colorChangeCount", colorChangeCount)
            editor.apply()
            
            if (colorChangeCount % 3 == 0) {
                // 광고 표시 메소드
                adManager.showInterstitialAd(requireActivity())
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
            return when (key) {
                "backgroundColor" -> prefs.getInt("backgroundColorCategory", android.graphics.Color.WHITE)
                "textColor" -> prefs.getInt("textColorCategory", android.graphics.Color.BLACK)
                else -> prefs.getInt(key, 0)
            }
        }

    }

}
