package com.odom.motivationlocker

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

// Firebase Remote Config로 앱 최소/최신 버전을 원격 관리한다.
//
// Remote Config 콘솔에서 설정할 키:
//   min_version_code   (Long, 기본값 0) — 이 값보다 낮은 versionCode는 강제 업데이트
//   latest_version_code (Long, 기본값 0) — 이 값보다 낮으면 권장 업데이트 (사용자가 무시 가능)
//
// 사용 전 준비:
//   1. Firebase 콘솔(https://console.firebase.google.com)에서 프로젝트 생성
//   2. Android 앱 등록 (패키지명: com.odom.motivationlocker)
//   3. google-services.json 다운로드 후 app/ 디렉토리에 배치
//   4. Remote Config 콘솔 → 매개변수 추가:
//      - min_version_code    : 숫자 (예: 14)
//      - latest_version_code : 숫자 (예: 15)
object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"
    private const val FETCH_INTERVAL_SECONDS = 3600L  // 1시간

    private const val KEY_MIN_VERSION_CODE = "min_version_code"
    private const val KEY_LATEST_VERSION_CODE = "latest_version_code"

    sealed class UpdateStatus {
        object UpToDate : UpdateStatus()
        object RecommendedUpdate : UpdateStatus()
        object ForceUpdate : UpdateStatus()
    }

    // 앱 시작 시 호출. fetch 완료 후 onResult로 업데이트 상태를 전달한다.
    // Firebase 미설정이거나 네트워크 오류 시 UpToDate로 처리해 앱이 정상 동작한다.
    fun checkForUpdate(currentVersionCode: Long, onResult: (UpdateStatus) -> Unit) {
        try {
            val remoteConfig = Firebase.remoteConfig
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS
                }
            )
            remoteConfig.setDefaultsAsync(
                mapOf(
                    KEY_MIN_VERSION_CODE to 0L,
                    KEY_LATEST_VERSION_CODE to 0L
                )
            )
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Remote Config fetch 실패, 업데이트 체크 건너뜀", task.exception)
                    onResult(UpdateStatus.UpToDate)
                    return@addOnCompleteListener
                }

                val minVersionCode = remoteConfig.getLong(KEY_MIN_VERSION_CODE)
                val latestVersionCode = remoteConfig.getLong(KEY_LATEST_VERSION_CODE)

                Log.d(TAG, "버전 체크: 현재=$currentVersionCode, 최소=$minVersionCode, 최신=$latestVersionCode")

                val status = when {
                    currentVersionCode < minVersionCode -> UpdateStatus.ForceUpdate
                    currentVersionCode < latestVersionCode -> UpdateStatus.RecommendedUpdate
                    else -> UpdateStatus.UpToDate
                }
                onResult(status)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase 초기화 안 됨, 업데이트 체크 건너뜀", e)
            onResult(UpdateStatus.UpToDate)
        }
    }
}
