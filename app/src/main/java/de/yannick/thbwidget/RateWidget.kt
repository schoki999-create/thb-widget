package de.yannick.thbwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class RateWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        // Sofort mit gespeicherten Werten zeichnen, dann Kurs im Hintergrund holen
        renderAll(ctx)
        refreshAsync(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) {
            renderAll(ctx, loading = true)
            refreshAsync(ctx)
        }
    }

    private fun refreshAsync(ctx: Context) {
        val pending = goAsync()
        val app = ctx.applicationContext
        Thread {
            try {
                RateStore.refresh(app)
                renderAll(app)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_REFRESH = "de.yannick.thbwidget.REFRESH"

        fun renderAll(ctx: Context, loading: Boolean = false) {
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, RateWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(ctx, loading)
            for (id in ids) manager.updateAppWidget(id, views)
        }

        private fun build(ctx: Context, loading: Boolean): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            val rate = RateStore.rate(ctx)
            val amount = RateStore.amount(ctx)

            if (rate != null) {
                v.setTextViewText(R.id.rate, "1 € = ${RateStore.formatRate(rate)} ฿")
                v.setTextViewText(R.id.amount_eur, RateStore.formatEur(amount))
                v.setTextViewText(R.id.amount_thb, RateStore.formatThb(amount * rate))
            } else {
                v.setTextViewText(R.id.rate, "Kurs wird geladen …")
                v.setTextViewText(R.id.amount_eur, RateStore.formatEur(amount))
                v.setTextViewText(R.id.amount_thb, "– ฿")
            }
            v.setTextViewText(
                R.id.status,
                if (loading) "Aktualisiere …" else RateStore.statusLine(ctx)
            )

            // Tippen aufs Widget → Eingabefenster
            val open = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, ConvertActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.root, open)

            // ⟳ → Kurs neu laden
            val refresh = PendingIntent.getBroadcast(
                ctx, 1,
                Intent(ctx, RateWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.refresh, refresh)
            return v
        }
    }
}
