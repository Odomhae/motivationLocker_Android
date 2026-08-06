package com.odom.motivationlocker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailyNotificationScheduler {

    private const val UNIQUE_WORK_NAME = "daily_quote_notification"
    const val DEFAULT_HOUR = 10
    const val DEFAULT_MINUTE = 0
    const val PREF_HOUR = "dailyNotificationHour"
    const val PREF_MINUTE = "dailyNotificationMinute"

    fun schedule(context: Context) {
        scheduleNext(context)
    }

    fun scheduleNext(context: Context) {
        val request = OneTimeWorkRequestBuilder<QuoteNotificationWorker>()
            .setInitialDelay(calculateInitialDelayMillis(context), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    // 사용자가 고른 다음 알림 시각까지 남은 시간(ms). 이미 지난 시각이면 내일로 계산한다.
    private fun calculateInitialDelayMillis(context: Context): Long {
        val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.getInt(PREF_HOUR, DEFAULT_HOUR))
            set(Calendar.MINUTE, prefs.getInt(PREF_MINUTE, DEFAULT_MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return nextRun.timeInMillis - now.timeInMillis
    }
}
