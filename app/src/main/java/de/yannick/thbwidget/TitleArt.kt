package de.yannick.thbwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.ceil

/**
 * Malt den App-Namen als Neon-Schriftzug in ein Bitmap:
 * kursive Exo-2-Schrift mit durchgehendem Regenbogen-Neonverlauf, Leuchten,
 * Glanz oben und einer futuristischen Zierlinie mit Schrägstrichen darunter.
 * (Widgets können keine Verlaufs-Schrift – darum als Bild.)
 */
object TitleArt {

    private val COLORS = intArrayOf(
        0xFF00E5FF.toInt(), // Cyan
        0xFF4D7CFF.toInt(), // Blau
        0xFFB04DFF.toInt(), // Lila
        0xFFFF3DD8.toInt(), // Magenta
        0xFFFF7A45.toInt(), // Orange
        0xFFFFD23F.toInt(), // Gelb
        0xFF9CFF3A.toInt(), // Giftgrün
        0xFF3DFFB0.toInt(), // Mint
        0xFF00E5FF.toInt()  // Cyan
    )
    private val STOPS = FloatArray(COLORS.size) { it / (COLORS.size - 1f) }

    private var cache: Bitmap? = null
    private var cacheKey: String? = null

    private fun typeface(ctx: Context): Typeface =
        try { ctx.resources.getFont(R.font.exo2_semibold_italic) }
        catch (_: Exception) { Typeface.create("sans-serif", Typeface.BOLD_ITALIC) }

    private fun rainbow(x0: Float, x1: Float) =
        LinearGradient(x0, 0f, x1, 0f, COLORS, STOPS, Shader.TileMode.CLAMP)

    fun render(ctx: Context, text: String, heightDp: Float = 50f): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val key = "$text|$heightDp|$density"
        cache?.let { if (cacheKey == key && !it.isRecycled) return it }

        val h = (heightDp * density).coerceAtLeast(24f)
        val ts = h * 0.66f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = ts
            typeface = typeface(ctx)
        }
        val tw = textPaint.measureText(text)
        val lp = h * 0.32f
        val tail = h * 0.62f
        val w = ceil(lp + tw + tail + h * 0.12f).toInt()

        val bmp = Bitmap.createBitmap(w, ceil(h).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val base = h * 0.64f
        val x0 = lp
        val x1 = lp + tw

        // ── Zierlinie ──────────────────────────────────────────────
        val y1 = h * 0.86f
        val step = h * 0.035f
        val xs = x0 + tw * 0.36f
        val xu = x0 + tw * 0.97f
        val yt = h * 0.70f
        val sw = h * 0.028f

        val line = Path().apply {
            moveTo(x0 + h * 0.04f, y1)
            lineTo(xs, y1)
            lineTo(xs + h * 0.05f, y1 - step)
            lineTo(xu, y1 - step)
            lineTo(xu + h * 0.16f, yt)
            lineTo(w - h * 0.08f, yt)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sw
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            shader = rainbow(x0, w.toFloat())
        }
        // Leuchten der Linie, dann scharfe Linie
        linePaint.maskFilter = BlurMaskFilter(h * 0.10f, BlurMaskFilter.Blur.NORMAL)
        c.drawPath(line, linePaint)
        linePaint.maskFilter = null
        c.drawPath(line, linePaint)

        // Linker Akzent-Strich
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sw * 0.8f
            strokeCap = Paint.Cap.ROUND
            color = 0xBF00E5FF.toInt()
        }
        c.drawLine(lp - h * 0.26f, h * 0.97f, lp - h * 0.02f, h * 0.80f, accent)

        // Drei Schrägstriche „///“
        val slashColors = intArrayOf(0xFF00E5FF.toInt(), 0xFF3DFFB0.toInt(), 0xFF9CFF3A.toInt())
        accent.strokeWidth = sw * 0.75f
        slashColors.forEachIndexed { i, col ->
            val sx = xu - h * 0.34f + i * h * 0.11f
            accent.color = col
            c.drawLine(sx, y1 - step - h * 0.03f, sx + h * 0.13f, yt - h * 0.03f, accent)
        }

        // ── Schriftzug ─────────────────────────────────────────────
        textPaint.shader = rainbow(x0, x1)
        // Leuchten
        textPaint.maskFilter = BlurMaskFilter(h * 0.19f, BlurMaskFilter.Blur.NORMAL)
        textPaint.alpha = 230
        c.drawText(text, x0, base, textPaint)
        // Scharfer Text
        textPaint.maskFilter = null
        textPaint.alpha = 255
        c.drawText(text, x0, base, textPaint)
        // Glanz oben
        textPaint.shader = LinearGradient(
            0f, base - ts * 0.75f, 0f, base,
            intArrayOf(0x73FFFFFF, 0x00FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawText(text, x0, base, textPaint)

        cache = bmp
        cacheKey = key
        return bmp
    }
}
