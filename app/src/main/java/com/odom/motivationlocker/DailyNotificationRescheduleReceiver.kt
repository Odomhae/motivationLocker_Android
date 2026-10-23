package com.odom.motivationlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 재부팅, 앱 업데이트, 시간/시간대 변경 시 데일리 알림 알람을 다시 건다.
// AlarmManager 알람은 재부팅하면 사라지고, 벽시계 기준 목표 시각은 시간 변경 시 다시 계산해야 하기 때문.
class DailyNotificationRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                if (DailyNotificationScheduler.isEnabled(context)) {
                    DailyNotificationScheduler.schedule(context)
                }
            }
        }
    }
}
