package de.yannick.thbwidget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Speichert Kurs + Betrag lokal und holt den aktuellen EUR→THB-Kurs (EZB via Frankfurter). */
object RateStore {
    private const val PREFS = "thb_widget"
    private const val KEY_RATE = "rate"
    private const val KEY_RATE_DATE = "rate_date"
    private const val KEY_FETCHED_AT = "fetched_at"
    private const val KEY_AMOUNT = "amount_eur" // Eingabebetrag in der jeweiligen Eingabewährung
    private const val KEY_DIR = "thb_to_eur"

    private val ENDPOINTS = listOf(
        "https://api.frankfurter.dev/v1/latest?base=EUR&symbols=THB",
        "https://api.frankfurter.app/latest?from=EUR&to=THB"
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun rate(ctx: Context): Double? =
        prefs(ctx).getString(KEY_RATE, null)?.toDoubleOrNull()

    fun rateDate(ctx: Context): String? = prefs(ctx).getString(KEY_RATE_DATE, null)

    fun fetchedAt(ctx: Context): Long = prefs(ctx).getLong(KEY_FETCHED_AT, 0L)

    fun amount(ctx: Context): Double =
        prefs(ctx).getString(KEY_AMOUNT, null)?.toDoubleOrNull() ?: 100.0

    fun setAmount(ctx: Context, value: Double) {
        prefs(ctx).edit().putString(KEY_AMOUNT, value.toString()).apply()
    }

    /** false = EUR eingeben → THB, true = THB eingeben → EUR */
    fun thbToEur(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DIR, false)

    fun setThbToEur(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_DIR, value).apply()
    }

    /** Rechnet einen Betrag in der aktuellen Richtung um. */
    fun convert(amount: Double, rate: Double, thbToEur: Boolean): Double =
        if (thbToEur) amount / rate else amount * rate

    fun formatIn(v: Double, thbToEur: Boolean) = if (thbToEur) formatThb(v) else formatEur(v)
    fun formatOut(v: Double, thbToEur: Boolean) = if (thbToEur) formatEur(v) else formatThb(v)

    /** Richtung umdrehen; das bisherige Ergebnis wird zum neuen Eingabebetrag. */
    fun toggleDirection(ctx: Context) {
        val dir = thbToEur(ctx)
        val rate = rate(ctx)
        if (rate != null) setAmount(ctx, roundNice(convert(amount(ctx), rate, dir)))
        setThbToEur(ctx, !dir)
    }

    fun roundNice(v: Double): Double = Math.round(v * 100.0) / 100.0

    /** Blockierend – nur im Hintergrund-Thread aufrufen. Gibt true zurück, wenn ein Kurs geladen wurde. */
    fun refresh(ctx: Context): Boolean {
        for (endpoint in ENDPOINTS) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (conn.responseCode != 200) continue
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val rate = json.getJSONObject("rates").getDouble("THB")
                    prefs(ctx).edit()
                        .putString(KEY_RATE, rate.toString())
                        .putString(KEY_RATE_DATE, json.optString("date"))
                        .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
                        .apply()
                    return true
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // nächsten Endpoint versuchen
            }
        }
        return false
    }

    /** Eingabe wie "1.234,50" oder "12.5" in eine Zahl umwandeln. */
    fun parseAmount(input: String): Double? {
        var s = input.trim().replace(" ", "")
        if (s.isEmpty()) return null
        if (s.contains(',')) s = s.replace(".", "").replace(',', '.')
        return s.toDoubleOrNull()
    }

    fun formatEur(v: Double): String = fmt(v, 2) + " €"
    fun formatThb(v: Double): String = fmt(v, 2) + " ฿"
    fun formatRate(v: Double): String = fmt(v, 2)

    private fun fmt(v: Double, digits: Int): String =
        NumberFormat.getNumberInstance(Locale.GERMANY).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = digits
            if (v >= 1000) maximumFractionDigits = 0
        }.format(v)

    /** z. B. "Kurs vom 06.11. · geladen 11:20" */
    fun statusLine(ctx: Context): String {
        val date = rateDate(ctx)
        val fetched = fetchedAt(ctx)
        val parts = mutableListOf<String>()
        if (!date.isNullOrBlank()) {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                if (d != null) parts += "Kurs vom " + SimpleDateFormat("dd.MM.", Locale.GERMANY).format(d)
            } catch (_: Exception) { parts += "Kurs vom $date" }
        }
        if (fetched > 0) parts += "geladen " + SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(fetched))
        return if (parts.isEmpty()) "Noch kein Kurs geladen" else parts.joinToString(" · ")
    }
}
