package dev.daniil.iskra.pet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class Mood { HAPPY, NORMAL, HUNGRY, SMOLDER }

/**
 * Procedural pixel-art fire spirit. Everything is drawn on a GxG grid so the
 * same code renders the animated pet in the app and the static one in the widget.
 */
object PetRenderer {
    const val G = 24

    private class Pal(val edge: Int, val c0: Int, val c1: Int, val c2: Int, val c3: Int)

    private val ASH = Pal(0xFF2A221F.toInt(), 0xFF5A4038.toInt(), 0xFF7A5040.toInt(), 0xFF9A6448.toInt(), 0xFFB07850.toInt())
    private val SILHOUETTE = 0xFF3A2C27.toInt()

    private fun pal(i: Int): Pal {
        val f = Palettes.get(i)
        return Pal(f.edge, f.c0, f.c1, f.c2, f.c3)
    }

    private fun wing(i: Int): Pal {
        val f = Palettes.get(i)
        return Pal(f.edge, f.edge, f.c0, f.c1, f.c2)
    }
    private val EYE = 0xFF2B0F06.toInt()
    private val SHINE = 0xFFFFFFFF.toInt()
    private val BLUSH = 0xFFFF7A9A.toInt()
    private val SMOKE = 0x998A8580.toInt()

    private fun shade(p: Pal, heat: Float): Int = when {
        heat < 0.28f -> p.c0
        heat < 0.52f -> p.c1
        heat < 0.76f -> p.c2
        else -> p.c3
    }

    /** Heat 0..1 inside a teardrop flame, or -1 outside. */
    private fun flameHeat(
        px: Float, py: Float,
        cx: Float, cy: Float, r: Float, h: Float, amp: Float, phase: Float,
    ): Float {
        val top = cy - h
        if (py < top || py > cy + r) return -1f
        val w = if (py >= cy) {
            val dy = py - cy
            sqrt(max(0f, r * r - dy * dy))
        } else {
            val k = (py - top) / h
            r * k.pow(0.75f)
        }
        val lift = if (py < cy) (cy - py) / h else 0f
        val off = sin(phase + py * 0.45f) * amp * lift
        val dx = abs(px - cx - off)
        if (dx > w) return -1f
        val nx = if (w > 0f) dx / w else 1f
        val vy = if (py >= cy) (py - cy) / r else (cy - py) / h
        return (1f - max(nx, vy * 0.9f)).coerceIn(0f, 1f)
    }

    fun buildGrid(stage: Int, mood: Mood, phase: Float, palette: Int = 0, silhouette: Boolean = false): IntArray {
        val g = IntArray(G * G)
        val smolder = mood == Mood.SMOLDER
        val fire = pal(palette)
        val body = if (smolder) ASH else fire
        val wingPal = wing(palette)
        val cx = G / 2f
        val r: Float
        val h0: Float
        val cy: Float
        when (stage) {
            0 -> { r = 4f; h0 = 6f; cy = 18f }
            1 -> { r = 5.5f; h0 = 9f; cy = 17f }
            2 -> { r = 7f; h0 = 11f; cy = 16f }
            else -> { r = 6.5f; h0 = 10f; cy = 15.5f }
        }
        val h = when (mood) {
            Mood.SMOLDER -> h0 * 0.5f
            Mood.HUNGRY -> h0 * 0.8f
            Mood.HAPPY -> h0 * 1.08f
            else -> h0
        }
        val amp = when (mood) {
            Mood.SMOLDER -> 0.3f
            Mood.HAPPY -> 1.6f
            else -> 1.1f
        }

        // Phoenix wings (drawn first, behind the body)
        if (stage >= 3 && !smolder) {
            val flap = sin(phase * 2f)
            val len = 5.5f
            for (y in 0 until G) for (x in 0 until G) {
                val px = x + 0.5f
                val py = y + 0.5f
                val dist = abs(px - cx) - (r - 1f)
                if (dist <= 0f || dist > len) continue
                val t = dist / len
                val top = cy - 1.5f - t * 6f - flap * t * 1.6f
                val feather = if (x % 2 == 0) 1.2f else 0f
                val bottom = cy + 3f - t * 3.5f + feather - flap * t * 1.2f
                if (py in top..bottom) g[y * G + x] = shade(wingPal, 1f - t)
            }
        }

        // Body + extra tongues
        val parts = mutableListOf(floatArrayOf(cx, cy, r, h, amp, phase))
        if (stage == 2 && !smolder) {
            parts += floatArrayOf(cx - r - 0.5f, cy + 2f, 2.2f, 5f, 0.8f, phase + 1.7f)
            parts += floatArrayOf(cx + r + 0.5f, cy + 2f, 2.2f, 5f, 0.8f, phase + 3.1f)
        }
        if (stage >= 3 && !smolder) {
            parts += floatArrayOf(cx - 2.6f, cy - h * 0.45f, 1.6f, 4.5f, 0.6f, phase + 1f)
            parts += floatArrayOf(cx + 2.6f, cy - h * 0.45f, 1.6f, 4.5f, 0.6f, phase + 2.2f)
        }
        for (y in 0 until G) for (x in 0 until G) {
            var best = -1f
            for (p in parts) {
                val hh = flameHeat(x + 0.5f, y + 0.5f, p[0], p[1], p[2], p[3], p[4], p[5])
                if (hh > best) best = hh
            }
            if (best >= 0f) g[y * G + x] = shade(body, best)
        }

        // Outline
        val out = g.copyOf()
        for (y in 0 until G) for (x in 0 until G) {
            if (g[y * G + x] != 0) continue
            val n = (x > 0 && g[y * G + x - 1] != 0) ||
                (x < G - 1 && g[y * G + x + 1] != 0) ||
                (y > 0 && g[(y - 1) * G + x] != 0) ||
                (y < G - 1 && g[(y + 1) * G + x] != 0)
            if (n) out[y * G + x] = body.edge
        }

        fun put(x: Int, y: Int, c: Int) {
            if (x in 0 until G && y in 0 until G) out[y * G + x] = c
        }
        fun sym(x: Int, y: Int, c: Int) {
            put(x, y, c)
            put(G - 1 - x, y, c)
        }

        // Face
        val e = if (r >= 5f) 2 else 1
        val off = max(1.5f, r * 0.5f)
        val ey = (cy - r * 0.2f).toInt()
        val lx = (cx - off - e / 2f).roundToInt()
        val my = ey + e + 1
        val blink = ((phase / (2f * PI.toFloat())) % 3f) > 2.82f

        when {
            mood == Mood.SMOLDER || blink -> {
                for (i in 0 until e) sym(lx + i, ey + e - 1, EYE)
            }
            mood == Mood.HAPPY -> {
                for (i in 0 until e) sym(lx + i, ey, EYE)
                sym(lx - 1, ey + 1, EYE)
                sym(lx + e, ey + 1, EYE)
            }
            else -> {
                for (i in 0 until e) for (j in 0 until e) sym(lx + i, ey + j, EYE)
                if (e == 2) sym(lx + 1, ey, SHINE)
            }
        }

        when (mood) {
            Mood.HAPPY -> {
                if (e == 2) {
                    sym(10, my, EYE)
                    sym(11, my + 1, EYE)
                    sym(lx - 1, my, BLUSH)
                } else {
                    sym(11, my, EYE)
                }
            }
            Mood.HUNGRY -> {
                sym(11, my, EYE)
                sym(11, my + 1, EYE)
            }
            Mood.SMOLDER -> sym(11, my + 1, EYE)
            Mood.NORMAL -> sym(11, my, EYE)
        }

        // Particles
        val top = cy - h
        if (smolder) {
            for (i in 0 until 3) {
                val t = ((phase * 0.5f + i * 2.1f) % 6.283f) / 6.283f
                val sy = (top - 1f - t * 8f).toInt()
                val sx = (cx + sin(phase + i * 2f) * 2f).toInt()
                put(sx, sy, SMOKE)
                put(sx + 1, sy, SMOKE)
            }
        } else if (mood == Mood.HAPPY) {
            for (i in 0 until 3) {
                val t = ((phase * 0.7f + i * 2.1f) % 6.283f) / 6.283f
                val sy = (top - t * 6f).toInt()
                val sx = (cx + sin(phase * 1.3f + i * 2.4f) * (r + 1f)).toInt()
                put(sx, sy, fire.c3)
            }
        }
        if (silhouette) {
            for (i in out.indices) if (out[i] != 0) out[i] = SILHOUETTE
        }
        return out
    }

    fun draw(
        canvas: Canvas, left: Float, top: Float, size: Float,
        stage: Int, mood: Mood, phase: Float, glow: Boolean = false,
        palette: Int = 0, silhouette: Boolean = false,
    ) {
        if (glow) {
            val gx = left + size / 2f
            val gy = top + size * 0.62f
            val rad = size * 0.55f
            val base = Palettes.get(palette).c1 and 0x00FFFFFF
            val col = (if (mood == Mood.SMOLDER) 0x30000000 else 0x60000000) or base
            val gp = Paint().apply {
                shader = RadialGradient(gx, gy, rad, col, 0, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(gx, gy, rad, gp)
        }
        val grid = buildGrid(stage, mood, phase, palette, silhouette)
        val s = size / G
        val p = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
        for (y in 0 until G) for (x in 0 until G) {
            val c = grid[y * G + x]
            if (c == 0) continue
            p.color = c
            canvas.drawRect(left + x * s, top + y * s, left + (x + 1) * s, top + (y + 1) * s, p)
        }
    }

    fun bitmap(px: Int, stage: Int, mood: Mood, palette: Int = 0, phase: Float = 0.6f): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        draw(Canvas(b), 0f, 0f, px.toFloat(), stage, mood, phase, palette = palette)
        return b
    }
}
