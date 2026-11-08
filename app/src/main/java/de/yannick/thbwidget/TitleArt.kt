package de.yannick.thbwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.sin

/**
 * Malt den App-Namen als Psytrance-Neon-Schriftzug in ein Bitmap:
 * jeder Buchstabe eigener Neon-Verlauf, Leuchten, dunkle Kontur, leicht schräg und hüpfend.
 * (Widgets können keine Verlaufs-Schrift – darum als Bild.)
 */
object TitleArt {

    private val NEON = intArrayOf(
        0xFFFF00E5.toInt(), // Magenta
        0xFF00F0FF.toInt(), // Cyan
        0xFFB6FF00.toInt(), // Giftgrün
        0xFFFFE600.toInt(), // Gelb
        0xFFFF6A00.toInt(), // Orange
        0xFF9D4DFF.toInt()  // UV-Lila
    )

    private var cache: Bitmap? = null
    private var cacheKey: String? = null

    fun render(ctx: Context, text: String, heightDp: Float = 50f): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val key = "$text|$heightDp|$density"
        cache?.let { if (cacheKey == key && !it.isRecycled) return it }

        val h = (heightDp * density).toInt().coerceAtLeast(24)
        val textSize = h * 0.62f
        val glow = h * 0.14f
        val pad = glow * 1.6f

        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC)
        }

        val spacing = textSize * 0.03f
        val advances = text.map { base.measureText(it.toString()) + spacing }
        val width = (advances.sum() + pad * 2).toInt().coerceAtLeast(1)

        val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val fm = base.fontMetrics
        val baseline = h / 2f - (fm.ascent + fm.descent) / 2f

        val glowPaint = Paint(base).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
        }
        val outline = Paint(base).apply {
            style = Paint.Style.STROKE
            strokeWidth = h * 0.07f
            strokeJoin = Paint.Join.ROUND
            color = 0xFF14001F.toInt()
        }
        val fill = Paint(base).apply { style = Paint.Style.FILL }
        val shine = Paint(base).apply {
            style = Paint.Style.STROKE
            strokeWidth = h * 0.018f
            color = 0x99FFFFFF.toInt()
        }

        var x = pad
        var colorIdx = 0
        text.forEachIndexed { i, ch ->
            val adv = advances[i]
            if (ch == ' ') { x += adv; return@forEachIndexed }

            val c1 = NEON[colorIdx % NEON.size]
            val c2 = NEON[(colorIdx + 1) % NEON.size]
            colorIdx++

            val y = baseline + sin(i * 1.4).toFloat() * h * 0.06f
            val angle = if (i % 2 == 0) -7f else 6f
            val s = ch.toString()

            canvas.save()
            canvas.rotate(angle, x + adv / 2f, y - textSize / 3f)

            glowPaint.color = c1
            canvas.drawText(s, x, y, glowPaint)
            canvas.drawText(s, x, y, glowPaint)

            canvas.drawText(s, x, y, outline)

            fill.shader = LinearGradient(
                0f, y - textSize * 0.8f, 0f, y,
                c1, c2, Shader.TileMode.CLAMP
            )
            canvas.drawText(s, x, y, fill)
            canvas.drawText(s, x, y, shine)

            canvas.restore()
            x += adv
        }

        cache = bmp
        cacheKey = key
        return bmp
    }
}
