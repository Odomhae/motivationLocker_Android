package com.odom.motivationlocker

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
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
import androidx.core.net.toUri
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showExitConfirmDialog()
        }
    }

    // 광고
    lateinit var mAdView : AdView
    // 뒤로가기 종료 확인 다이얼로그에 표시할, 미리 로드해 둔 배너 광고
    private lateinit var exitAdView: AdView
    private lateinit var updateAdView: AdView
    private val adSize: AdSize
        get() {
            val density = resources.displayMetrics.density
            val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.width().toFloat()
            } else {
                @Suppress("DEPRECATION")
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(metrics)
                metrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private lateinit var binding: ActivityMainBinding // 자동 생성된 바인딩 클래스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLightBars = AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightBars
            isAppearanceLightNavigationBars = isLightBars
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // 툴바 top padding = 상태바 높이: 툴바 배경이 상태바 뒤까지 연장되어 색상이 자연스럽게 이어짐
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        // 하단 네비게이션 바 영역이 광고 뷰와 겹치지 않도록
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        this.onBackPressedDispatcher.addCallback(this, callback)

        supportFragmentManager.beginTransaction().replace(R.id.preferenceContent, SettingPreferencesFragment()).commit()

        checkAppUpdate()
    }

    private fun checkAppUpdate() {
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        }
        RemoteConfigManager.checkForUpdate(versionCode) { status ->
            runOnUiThread {
                when (status) {
                    RemoteConfigManager.UpdateStatus.ForceUpdate -> showForceUpdateDialog()
                    RemoteConfigManager.UpdateStatus.RecommendedUpdate -> showRecommendedUpdateDialog()
                    RemoteConfigManager.UpdateStatus.UpToDate -> Unit
                }
            }
        }
    }

    private fun buildUpdateDialogView(title: String, message: String): android.view.View {
        val view = layoutInflater.inflate(R.layout.dialog_update, null)
        view.findViewById<TextView>(R.id.updateDialogTitle).text = title
        view.findViewById<TextView>(R.id.updateDialogMessage).text = message
        val adContainer = view.findViewById<FrameLayout>(R.id.updateAdContainer)
        (updateAdView.parent as? ViewGroup)?.removeView(updateAdView)
        adContainer.addView(updateAdView)
        return view
    }

    private fun showForceUpdateDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(buildUpdateDialogView(
                getString(R.string.update_force_title),
                getString(R.string.update_force_message)
            ))
            .setPositiveButton(getString(R.string.update_go_to_store)) { _, _ -> openPlayStore() }
            .setCancelable(false)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(ContextCompat.getColor(context, R.color.colorRed))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    private fun showRecommendedUpdateDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(buildUpdateDialogView(
                getString(R.string.update_recommended_title),
                getString(R.string.update_recommended_message)
            ))
            .setPositiveButton(getString(R.string.update_go_to_store)) { _, _ -> openPlayStore() }
            .setNegativeButton(getString(R.string.update_later), null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(ContextCompat.getColor(context, R.color.colorRed))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
            setTextColor(ContextCompat.getColor(context, R.color.settingsSecondary))
        }
    }

    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri())
        startActivity(intent)
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

    override fun onStart() {
        super.onStart()

        // 배너 광고
        MobileAds.initialize(this) {}
        mAdView = AdView(this)
        binding.adMobView.addView(mAdView)
        loadBanner()

        // 뒤로가기 종료 확인 다이얼로그용 배너 광고를 미리 로드(다이얼로그가 뜰 때 바로 보이도록)
        exitAdView = AdView(this)
        exitAdView.adUnitId = resources.getString(R.string.TEST_banner_ad_unit_id)
        exitAdView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        exitAdView.loadAd(AdRequest.Builder().build())

        // 업데이트 다이얼로그용 배너 광고 미리 로드
        updateAdView = AdView(this)
        updateAdView.adUnitId = resources.getString(R.string.TEST_banner_ad_unit_id)
        updateAdView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        updateAdView.loadAd(AdRequest.Builder().build())
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

        // "배경 설정" 다이얼로그가 열려있는 동안, 사진 선택 완료 콜백에서 썸네일을 즉시 갱신하기 위한 참조
        private var backgroundDialog: BackgroundSettingsDialogFragment? = null

        // Photo Picker는 Fragment가 STARTED 상태가 되기 전에 등록되어야 하므로 필드로 선언
        private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
                prefs.edit().putString("backgroundPhotoUri", uri.toString()).apply()

                backgroundDialog?.updatePhotoPreview(uri)
                refreshBackgroundSettingsSummary()

                trackColorChange()
            }
        }

        // "다른 앱 위에 표시" 설정 화면에서 돌아왔을 때 결과 처리
        // (이 설정 화면은 resultCode를 돌려주지 않으므로 canDrawOverlays로 직접 재확인)
        private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val switchPreference = findPreference<SwitchPreferenceCompat>("useLockScreen") ?: return@registerForActivityResult
            if (Settings.canDrawOverlays(requireContext())) {
                Toast.makeText(requireContext(), R.string.permission_set_message, Toast.LENGTH_SHORT).show()
                switchPreference.isChecked = true
                startLockScreenService()
            } else {
                Toast.makeText(requireContext(), R.string.permission_denied_message, Toast.LENGTH_SHORT).show()
            }
        }

        private fun startLockScreenService() {
            val intent = Intent(requireContext(), LockScreenService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        }

        private fun requestOverlayPermission() {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${requireContext().packageName}".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref, rootKey)

            adManager = AdManager(requireContext())

            // SharedPreferences에서 colorChangeCount 초기화
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            colorChangeCount = prefs.getInt("colorChangeCount", 0)

            val switchPreference = findPreference<SwitchPreferenceCompat>("useLockScreen")
            val languagePreference = findPreference<androidx.preference.ListPreference>("languageCategory")
            val switchSourcePreference = findPreference<SwitchPreferenceCompat>("showSourcePref")
            val dailyNotificationPreference = findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled")
            val backgroundSettingsPreference = findPreference<androidx.preference.Preference>("backgroundSettingsEntry")
            val textSettingsPreference = findPreference<androidx.preference.Preference>("textSettingsEntry")


            // 앱이 시작됬을대 이미 퀴즈잠금화면 사용이 체크되어있으면 서비스 실행
            // 단, 사용자가 시스템 설정에서 "다른 앱 위에 표시" 권한을 회수했다면 잠금화면이 뜰 수 없으므로 스위치를 끈다
            if (switchPreference?.isChecked!!) {
                if (Settings.canDrawOverlays(requireContext())) {
                    startLockScreenService()
                } else {
                    switchPreference.isChecked = false
                    requireContext().stopService(Intent(requireActivity(), LockScreenService::class.java))
                }
            }

            languagePreference?.summary = languagePreference?.entries?.get(getInt("language"))
            refreshBackgroundSettingsSummary()
            refreshTextSettingsSummary()
            refreshDailyNotificationSummary()

            // 강제 종료 등으로 데일리 알림 알람이 지워졌으면 다시 건다
            if (dailyNotificationPreference?.isChecked == true) {
                DailyNotificationScheduler.ensureScheduled(requireContext())
            }


            // 사용여부
            switchPreference.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    // 잠금화면 사용이 체크된 경우 lockScreenService 실행
                    Log.d("checked", "체크")
                    if (!Settings.canDrawOverlays(requireContext())) {
                        // 권한이 없으면 스위치는 그대로 꺼둔 채 설정 화면으로 보내고,
                        // 허용하고 돌아오면 overlayPermissionLauncher에서 스위치를 켠다
                        requestOverlayPermission()
                        return@setOnPreferenceChangeListener false
                    }
                    startLockScreenService()
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
                    } else {
                        // 시간 선택을 확정해야 스위치가 켜지므로 여기서는 false를 반환
                        showNotificationTimePicker()
                    }
                    false
                } else {
                    // 이미 켜진 상태에서 탭하면 바로 끄지 않고 시간 변경 다이얼로그를 띄운다.
                    // 끄기는 다이얼로그의 "알림 끄기" 버튼으로 처리한다.
                    showNotificationTimePicker(allowDisable = true)
                    false
                }
            }

            // 배경 설정 (단색/그라데이션/사진 통합 팝업)
            backgroundSettingsPreference?.setOnPreferenceClickListener {
                val dialog = BackgroundSettingsDialogFragment()
                dialog.onSummaryRefreshNeeded = { refreshBackgroundSettingsSummary() }
                dialog.onColorChangeTracked = { trackColorChange() }
                dialog.onPhotoPickRequested = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                dialog.onDialogDismissed = { backgroundDialog = null }
                backgroundDialog = dialog
                dialog.show(parentFragmentManager, "backgroundSettings")
                true
            }

            // 글자 설정 (글자색/글자크기 통합 팝업)
            textSettingsPreference?.setOnPreferenceClickListener {
                val dialog = TextSettingsDialogFragment()
                dialog.onSummaryRefreshNeeded = { refreshTextSettingsSummary() }
                dialog.onColorChangeTracked = { trackColorChange() }
                dialog.show(parentFragmentManager, "textSettings")
                true
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            if (requestCode == DAILY_NOTIFICATION_PERMISSION_CODE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 시간 선택을 확정하면 스위치가 켜지고 스케줄된다
                    showNotificationTimePicker()
                } else {
                    findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled")?.isChecked = false
                    Toast.makeText(requireContext(), R.string.daily_notification_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 알림 받을 시간을 고르는 다이얼로그. 확정 시에만 저장/스케줄/스위치 ON 처리한다.
        // allowDisable=true(이미 켜진 상태에서 탭)면 "알림 끄기" 버튼을 함께 보여준다.
        private fun showNotificationTimePicker(allowDisable: Boolean = false) {
            val context = requireContext()
            val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            val hour = prefs.getInt(DailyNotificationScheduler.PREF_HOUR, DailyNotificationScheduler.DEFAULT_HOUR)
            val minute = prefs.getInt(DailyNotificationScheduler.PREF_MINUTE, DailyNotificationScheduler.DEFAULT_MINUTE)

            val dialog = TimePickerDialog(context, { _, pickedHour, pickedMinute ->
                prefs.edit()
                    .putInt(DailyNotificationScheduler.PREF_HOUR, pickedHour)
                    .putInt(DailyNotificationScheduler.PREF_MINUTE, pickedMinute)
                    .apply()
                DailyNotificationScheduler.schedule(context)
                findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled")?.isChecked = true
                refreshDailyNotificationSummary(enabled = true)
            }, hour, minute, android.text.format.DateFormat.is24HourFormat(context))

            if (allowDisable) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.daily_notification_turn_off)) { _, _ ->
                    DailyNotificationScheduler.cancel(context)
                    findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled")?.isChecked = false
                    refreshDailyNotificationSummary(enabled = false)
                }
            }
            dialog.show()
        }

        // 알림이 켜져 있으면 "매일 HH:mm"을 summary로 표시하고, 꺼져 있으면 지운다
        private fun refreshDailyNotificationSummary(enabled: Boolean? = null) {
            val pref = findPreference<SwitchPreferenceCompat>("dailyNotificationEnabled") ?: return
            if (!(enabled ?: pref.isChecked)) {
                pref.summary = null
                return
            }
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            val time = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prefs.getInt(DailyNotificationScheduler.PREF_HOUR, DailyNotificationScheduler.DEFAULT_HOUR))
                set(Calendar.MINUTE, prefs.getInt(DailyNotificationScheduler.PREF_MINUTE, DailyNotificationScheduler.DEFAULT_MINUTE))
            }
            val timeText = android.text.format.DateFormat.getTimeFormat(requireContext()).format(time.time)
            pref.summary = getString(R.string.daily_notification_time_summary, timeText)
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

        // "배경 설정" 1줄의 summary와 색상 칩을 현재 배경 종류에 맞춰 갱신.
        // 색은 hex 문자열 대신 항목 오른쪽 원형 칩(ColorIndicatorPreference)으로 보여준다.
        private fun refreshBackgroundSettingsSummary() {
            val pref = findPreference<ColorIndicatorPreference>("backgroundSettingsEntry") ?: return
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            when (getInt("backgroundType")) {
                1 -> {
                    val presetIndex = prefs.getInt("backgroundGradientPreset", 0)
                    val presets = GradientPresets.forBaseColor(getInt("backgroundColor"))
                    val preset = presets.getOrNull(presetIndex) ?: presets[0]
                    pref.setIndicatorGradient(preset.startColor, preset.endColor)
                    pref.summary = getString(R.string.backgroundTypeGradient)
                }
                2 -> {
                    val hasPhoto = prefs.getString("backgroundPhotoUri", null) != null
                    val photoState = getString(if (hasPhoto) R.string.background_photo_selected else R.string.background_photo_not_selected)
                    pref.clearIndicator()
                    pref.summary = "${getString(R.string.backgroundTypePhoto)} · $photoState"
                }
                else -> {
                    pref.setIndicatorColor(getInt("backgroundColor"))
                    pref.summary = getString(R.string.backgroundTypeSolid)
                }
            }
        }

        // "글자 설정" 1줄의 summary(글자 크기)와 글자색 칩을 갱신
        private fun refreshTextSettingsSummary() {
            val pref = findPreference<ColorIndicatorPreference>("textSettingsEntry") ?: return
            val prefs = requireContext().getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
            val sizeIndex = prefs.getInt("textSize", 0)
            pref.setIndicatorColor(getInt("textColor"))
            pref.summary = resources.getStringArray(R.array.textSizeCategory).getOrElse(sizeIndex) { "" }
        }

    }

}
