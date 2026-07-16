package com.odom.motivationlocker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews

// 홈 화면 위젯: 명언 텍스트만 보여주고, 탭하면 새 명언으로 갱신된다(자동 주기 갱신은 없음).
// 배경/글자색/글자크기/출처표시/언어는 앱의 "SETTINGS" 값을 그대로 재사용한다(단, 사진 배경은
// 위젯에서 지원하지 않고 단색으로 대체 — RemoteViews 제약과 메모리 부담 때문).
class QuoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.odom.motivationlocker.ACTION_REFRESH_WIDGET"
        private const val SETTINGS_PREFS = "SETTINGS"
        private const val WIDGET_QUOTES_PREFS = "WIDGET_QUOTES"

        // 그라데이션을 비트맵으로 렌더링할 기준 크기(dp). RemoteViews는 GradientDrawable 같은 커스텀
        // Drawable을 직접 받지 못하므로, 적당한 해상도의 비트맵을 그려서 배경 ImageView에 넣는다.
        private const val GRADIENT_BITMAP_SIZE_DP = 300
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                pickNewQuote(context, appWidgetId)
                AppWidgetManager.getInstance(context)
                    .updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetId))
            }
            return
        }
        // 그 외 액션(APPWIDGET_UPDATE/DELETED/ENABLED 등)은 기본 동작에 위임
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            // 저장된 명언이 없을 때(최초 배치)만 새로 뽑는다 — 리사이즈 등으로 onUpdate가
            // 다시 불려도 이미 표시 중인 명언이 임의로 바뀌지 않도록.
            if (!hasStoredQuote(context, appWidgetId)) {
                pickNewQuote(context, appWidgetId)
            }
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetId))
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(WIDGET_QUOTES_PREFS, Context.MODE_PRIVATE).edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(quoteKey(appWidgetId))
            editor.remove(writerKey(appWidgetId))
        }
        editor.apply()
    }

    private fun quoteKey(appWidgetId: Int) = "quote_$appWidgetId"
    private fun writerKey(appWidgetId: Int) = "writer_$appWidgetId"

    private fun hasStoredQuote(context: Context, appWidgetId: Int): Boolean {
        return context.getSharedPreferences(WIDGET_QUOTES_PREFS, Context.MODE_PRIVATE)
            .contains(quoteKey(appWidgetId))
    }

    private fun pickNewQuote(context: Context, appWidgetId: Int) {
        val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val language = settings.getInt("language", 0)
        val quote = QuoteRepository.getRandomQuote(context, language)

        context.getSharedPreferences(WIDGET_QUOTES_PREFS, Context.MODE_PRIVATE).edit()
            .putString(quoteKey(appWidgetId), quote.quote)
            .putString(writerKey(appWidgetId), quote.writer)
            .apply()
    }

    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quote)

        val widgetQuotes = context.getSharedPreferences(WIDGET_QUOTES_PREFS, Context.MODE_PRIVATE)
        views.setTextViewText(R.id.widgetQuoteText, widgetQuotes.getString(quoteKey(appWidgetId), ""))
        views.setTextViewText(R.id.widgetWriterText, widgetQuotes.getString(writerKey(appWidgetId), ""))

        applyStyle(context, views)

        // 위젯을 탭하면 이 브로드캐스트가 와서 해당 위젯 ID만 새 명언으로 갱신한다.
        val refreshIntent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        // requestCode를 appWidgetId로 다르게 주어야 위젯마다 별개의 PendingIntent가 되어
        // 탭했을 때 그 위젯만 갱신된다(같으면 마지막 위젯의 PendingIntent로 덮어써짐).
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

        // 우측 상단 작은 아이콘만 따로 앱 실행으로 연결(위젯 전체 탭은 여전히 명언 갱신).
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetOpenAppIcon, openAppPendingIntent)

        return views
    }

    // 앱 설정("SETTINGS")의 배경/글자색/글자크기/출처표시를 위젯에도 동일하게 반영한다.
    private fun applyStyle(context: Context, views: RemoteViews) {
        val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val backgroundType = settings.getInt("backgroundType", 0)
        val backgroundColor = settings.getInt("backgroundColorCategory", Color.WHITE)
        val gradientPreset = settings.getInt("backgroundGradientPreset", 0)
        val textColor = settings.getInt("textColorCategory", Color.BLACK)
        val textSize = settings.getInt("textSize", 0)
        // MotivationLockerActivity와 동일한 의미: 0=출처 표기, 1=출처 미표기.
        val showSource = settings.getInt("showSource", 0)

        if (backgroundType == 1) {
            // 그라데이션
            views.setInt(R.id.widgetRoot, "setBackgroundColor", Color.TRANSPARENT)
            views.setImageViewBitmap(
                R.id.widgetBackgroundImage,
                renderGradientBitmap(context, backgroundColor, gradientPreset)
            )
            views.setViewVisibility(R.id.widgetBackgroundImage, View.VISIBLE)
        } else {
            // 단색(backgroundType==0), 또는 사진(2) — 위젯은 사진을 지원하지 않으므로 단색으로 대체
            views.setViewVisibility(R.id.widgetBackgroundImage, View.GONE)
            val actualColor = if (backgroundColor == -1) Color.WHITE else backgroundColor
            views.setInt(R.id.widgetRoot, "setBackgroundColor", actualColor)
        }

        views.setTextColor(R.id.widgetQuoteText, textColor)
        views.setTextColor(R.id.widgetWriterText, textColor)

        val (quoteSp, writerSp) = widgetTextSizes(textSize)
        views.setTextViewTextSize(R.id.widgetQuoteText, TypedValue.COMPLEX_UNIT_SP, quoteSp)
        views.setTextViewTextSize(R.id.widgetWriterText, TypedValue.COMPLEX_UNIT_SP, writerSp)

        views.setViewVisibility(
            R.id.widgetWriterText,
            if (showSource == 1) View.GONE else View.VISIBLE
        )
    }

    // MotivationLockerActivity.setTextSize()의 인덱스(0~4)와 순서는 같지만, 그 함수의 dp 값은
    // 전체 화면 기준(최대 100dp)이라 위젯의 훨씬 작은 표시 영역에 그대로 쓰면 잘리거나 넘친다.
    // 같은 상대적 크기 순서를 유지하면서 위젯에 맞게 축소한 sp 값을 쓴다.
    private fun widgetTextSizes(textSize: Int): Pair<Float, Float> = when (textSize) {
        0 -> 12f to 10f
        1 -> 16f to 12f
        2 -> 20f to 14f
        3 -> 26f to 18f
        4 -> 32f to 22f
        else -> 12f to 10f
    }

    private fun renderGradientBitmap(context: Context, baseColor: Int, presetIndex: Int): Bitmap {
        val presets = GradientPresets.forBaseColor(baseColor)
        val preset = presets.getOrNull(presetIndex) ?: presets[0]

        val density = context.resources.displayMetrics.density
        val sizePx = (GRADIENT_BITMAP_SIZE_DP * density).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(preset.startColor, preset.endColor)
        )
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        return bitmap
    }
}
