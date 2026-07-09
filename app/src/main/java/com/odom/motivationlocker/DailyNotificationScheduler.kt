package com.odom.motivationlocker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailyNotificationScheduler {

    private const val UNIQUE_WORK_NAME = "daily_quote_notification"
    private const val NOTIFICATION_HOUR = 10

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<QuoteNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelayMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    // 다음 오전 10시까지 남은 시간(ms). 이미 지난 시각이면 내일 오전 10시로 계산한다.
    private fun calculateInitialDelayMillis(): Long {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, NOTIFICATION_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return nextRun.timeInMillis - now.timeInMillis
    }
}
