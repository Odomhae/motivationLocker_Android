package com.odom.motivationlocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class QuoteNotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val CHANNEL_ID = "com.odom.motivationlocker.daily_quote"
        private const val NOTIFICATION_ID = 8888
    }

    override fun doWork(): Result {
        val context = applicationContext
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

        DailyNotificationScheduler.scheduleNext(context)

        return Result.success()
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
