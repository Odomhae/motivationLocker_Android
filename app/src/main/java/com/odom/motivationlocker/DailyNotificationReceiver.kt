package com.odom.motivationlocker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// DailyNotificationScheduler가 건 알람을 받는 리시버 (외부 노출 안 함).
// 목표 시각 전에 울린 중간 알람이면 다시 좁혀서 걸고, 목표 시각이 지났으면 알림을 띄운 뒤 다음 날로 재예약한다.
class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "com.odom.motivationlocker.daily_quote"
        private const val NOTIFICATION_ID = 8888
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyNotificationScheduler.ACTION_DAILY_ALARM) return
        if (!DailyNotificationScheduler.isEnabled(context)) return

        val target = intent.getLongExtra(DailyNotificationScheduler.EXTRA_TARGET_TIME, 0L)
        if (target > System.currentTimeMillis()) {
            DailyNotificationScheduler.setAlarm(context, target)
            return
        }

        showNotification(context)
        DailyNotificationScheduler.schedule(context)
    }

    private fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prefs = context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
        val language = prefs.getInt("language", 0)

        val quote = QuoteRepository.getRandomQuote(context, language)

        createChannelIfNeeded(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.android_chrome_192x192)
            .setContentTitle(quote.quote)
            .setContentText(quote.writer)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${quote.quote}\n\n${quote.writer}"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.daily_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
}
