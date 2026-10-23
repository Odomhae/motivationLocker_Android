package com.odom.motivationlocker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import java.util.Calendar

object DailyNotificationScheduler {

    // 예전 버전(WorkManager 기반)이 등록해둔 작업 이름. 업데이트한 사용자의 잔여 작업을 지우는 용도로만 쓴다.
    private const val LEGACY_UNIQUE_WORK_NAME = "daily_quote_notification"
    private const val REQUEST_CODE = 8888
    const val ACTION_DAILY_ALARM = "com.odom.motivationlocker.ACTION_DAILY_QUOTE_ALARM"
    const val EXTRA_TARGET_TIME = "targetTime"
    const val DEFAULT_HOUR = 10
    const val DEFAULT_MINUTE = 0
    const val PREF_HOUR = "dailyNotificationHour"
    const val PREF_MINUTE = "dailyNotificationMinute"
    const val PREF_ENABLED = "dailyNotificationEnabled"

    // 목표 시각까지 이 시간 이하로 남으면 목표 시각 자체로 알람을 건다
    internal const val FINAL_APPROACH_MILLIS = 10 * 60 * 1000L

    // 권한 없는 알람(setAndAllowWhileIdle)은 요청 시각 + (남은 시간 × 0.75)까지 늦게 울릴 수 있다
    private const val INEXACT_DELAY_FACTOR = 1.75

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, false)

    // 지금 기준 다음 목표 시각으로 새로 예약한다 (시간 변경, 부팅, 업데이트, 알림 후 재예약)
    fun schedule(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME)
        val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
        val target = nextTargetMillis(
            System.currentTimeMillis(),
            prefs.getInt(PREF_HOUR, DEFAULT_HOUR),
            prefs.getInt(PREF_MINUTE, DEFAULT_MINUTE)
        )
        setAlarm(context, target)
    }

    // 앱 실행 시 호출. 걸려 있는 알람이 없을 때(강제 종료 등으로 지워진 경우)만 다시 예약한다.
    // 이미 걸려 있으면 건드리지 않아야 목표 시각 직후 늦게 울릴 예정인 오늘 알림이 내일로 밀리지 않는다.
    fun ensureScheduled(context: Context) {
        val existing = PendingIntent.getBroadcast(
            context, REQUEST_CODE, alarmIntent(context, 0L),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing == null) {
            schedule(context)
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, alarmIntent(context, 0L),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // target(목표 시각)을 향해 다음 알람을 건다. 남은 시간이 길면 중간 알람을 걸어 점점 좁혀간다.
    fun setAlarm(context: Context, target: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, alarmIntent(context, target),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = nextAlarmMillis(System.currentTimeMillis(), target)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun alarmIntent(context: Context, target: Long): Intent =
        Intent(context, DailyNotificationReceiver::class.java)
            .setAction(ACTION_DAILY_ALARM)
            .putExtra(EXTRA_TARGET_TIME, target)

    // 사용자가 고른 시각의 다음 발생 시점. 이미 지난 시각이면 내일로 계산한다.
    internal fun nextTargetMillis(now: Long, hour: Int, minute: Int): Long {
        val nextRun = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return nextRun.timeInMillis
    }

    // 이번에 걸 알람 시각. 목표가 가까우면 목표 시각 그대로,
    // 멀면 가장 늦게 울려도 목표 FINAL_APPROACH_MILLIS 전에는 울리는 중간 시각을 돌려준다.
    internal fun nextAlarmMillis(now: Long, target: Long): Long {
        val remaining = target - now
        if (remaining <= FINAL_APPROACH_MILLIS) {
            return target
        }
        return now + ((remaining - FINAL_APPROACH_MILLIS) / INEXACT_DELAY_FACTOR).toLong()
    }
}
