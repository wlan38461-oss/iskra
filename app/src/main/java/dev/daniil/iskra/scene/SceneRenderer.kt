package dev.daniil.iskra.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.pet.PetDraw
import dev.daniil.iskra.pet.PetGrid
import dev.daniil.iskra.pet.PetLook
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The landscape behind the pet: sky for the time of day, sun or moon, stars, clouds,
 * mountains, hills, a meadow and the campfire. Plain android.graphics so the app
 * screens and the home-screen widgets share one look.
 */
object SceneRenderer {
    const val FULL = 0
    const val HERO = 1

    class Opts(
        val mode: Int = FULL,
        /** seconds, drives twinkling, clouds and sparks */
        val time: Float = 0f,
        /** px per dp */
        val density: Float = 3f,
        val look: PetLook? = null,
        /** tasks done today = logs on the fire */
        val logs: Int = 0,
        val petScale: Float = 1f,
        val petPhase: Float = 0.6f,
        val fireX: Float = 0.5f,
        /** where the far mountains stand, fraction of height */
        val horizon: Float = 0.64f,
        /** where the meadow begins, fraction of height */
        val groundY: Float = 0.80f,
        /** pet size, fraction of height */
        val petSize: Float = 0.42f,
        /** HERO: colour of the foreground wave (the screen's sheet) */
        val sheet: Int = 0,
        val clouds: Boolean = true,
        val trees: Boolean = true,
        val sparks: Boolean = true,
        /** top of the sun/moon arc, fraction of height */
        val arcTop: Float = 0.12f,
        val stars: Int = 70,
        /** horizontal span of the sun/moon arc */
        val arcFrom: Float = 0.08f,
        val arcTo: Float = 0.92f,
        val celestialSize: Float = 1f,
    )

    fun draw(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        drawBack(c, w, h, sky, o)
        drawAtmos(c, w, h, sky, o)
        drawLand(c, w, h, sky, o)
        if (o.mode == FULL) drawFire(c, w, h, sky, o)
    }

    // ------------------------------------------------------------------ sky

    fun drawBack(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        val p = Paint()
        val hz = h * o.horizon
        p.shader = LinearGradient(
            0f, 0f, 0f, hz,
            intArrayOf(sky.top, sky.mid, sky.bottom), floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, w, h, p)
        drawCelestial(c, w, h, sky, o)
    }

    private fun arcPos(t: Float, w: Float, h: Float, o: Opts): FloatArray {
        val x = w * (o.arcFrom + (o.arcTo - o.arcFrom) * t)
        val e = sin(t * PI.toFloat())
        val base = h * o.horizon
        val peak = h * o.arcTop
        return floatArrayOf(x, base - (base - peak) * e, e)
    }

    private fun drawCelestial(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        val r = min(w, h) * 0.055f * o.celestialSize
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (sky.moonT >= 0f) {
            val (x, y, _) = arcPos(sky.moonT, w, h, o).let { Triple(it[0], it[1], it[2]) }
            p.shader = RadialGradient(
                x, y, r * 4.5f,
                intArrayOf(0x40D8E0FF, 0x14D8E0FF, 0x00D8E0FF), floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            c.drawCircle(x, y, r * 4.5f, p)
            p.shader = null
            val moon = Path()
            moon.addCircle(x, y, r, Path.Direction.CW)
            val bite = Path()
            bite.addCircle(x + r * 0.48f, y - r * 0.28f, r * 0.9f, Path.Direction.CW)
            moon.op(bite, Path.Op.DIFFERENCE)
            p.color = 0xFFF6EED6.toInt()
            c.drawPath(moon, p)
            p.color = 0x33B9AE95
            c.drawCircle(x - r * 0.55f, y + r * 0.15f, r * 0.16f, p)
            c.drawCircle(x - r * 0.3f, y + r * 0.55f, r * 0.11f, p)
        }
        if (sky.sunT >= 0f) {
            val pos = arcPos(sky.sunT, w, h, o)
            val x = pos[0]
            val y = pos[1]
            val e = pos[2]
            val col = Sky.lerpColor(0xFFFF6A3D.toInt(), 0xFFFFDD6E.toInt(), e.pow(0.5f))
            val glowR = r * (7f - 2.5f * e)
            p.shader = RadialGradient(
                x, y, glowR,
                intArrayOf(Sky.withAlpha(col, 0.55f), Sky.withAlpha(col, 0.16f), Sky.withAlpha(col, 0f)),
                floatArrayOf(0f, 0.3f, 1f),
                Shader.TileMode.CLAMP,
            )
            c.drawCircle(x, y, glowR, p)
            p.shader = null
            p.color = col
            c.drawCircle(x, y, r, p)
            p.color = Sky.withAlpha(0xFFFFF8E0.toInt(), 0.45f + 0.35f * e)
            c.drawCircle(x - r * 0.1f, y - r * 0.1f, r * 0.6f, p)
        }
    }

    // --------------------------------------------------------- stars, clouds

    fun drawAtmos(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        drawStars(c, w, h, sky, o)
        if (o.clouds) drawClouds(c, w, h, sky, o)
    }

    private class Rnd(seed: Int) {
        private var s = seed.toLong() * 2654435761L + 12345L
        fun next(): Float {
            s = (s * 6364136223846793005L + 1442695040888963407L)
            return ((s ushr 33) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
        }
    }

    private fun drawStars(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        if (sky.stars <= 0.02f) return
        val d = o.density
        val rnd = Rnd(7)
        val p = Paint()
        val limit = h * o.horizon * 0.9f
        for (i in 0 until o.stars) {
            val x = rnd.next() * w
            val y = rnd.next() * limit
            val big = rnd.next() > 0.88f
            val s = (if (big) 2.2f else 1f + rnd.next() * 0.9f) * d
            val speed = 0.6f + rnd.next() * 1.6f
            val tw = 0.5f + 0.5f * sin(o.time * speed + i * 1.7f)
            val warm = rnd.next() > 0.8f
            val base = if (warm) 0xFFFFE6C2.toInt() else 0xFFFFFFFF.toInt()
            p.color = Sky.withAlpha(base, sky.stars * (0.35f + 0.65f * tw))
            c.drawRect(x, y, x + s, y + s, p)
            if (big && tw > 0.6f) {
                p.color = Sky.withAlpha(base, sky.stars * (tw - 0.5f))
                c.drawRect(x - s, y, x, y + s, p)
                c.drawRect(x + s, y, x + 2 * s, y + s, p)
                c.drawRect(x, y - s, x + s, y, p)
                c.drawRect(x, y + s, x + s, y + 2 * s, p)
            }
        }
    }

    private val cloudDefs = listOf(
        floatArrayOf(0.13f, 1.0f, 5f, 0.10f),
        floatArrayOf(0.30f, 0.72f, 3.4f, 0.58f),
        floatArrayOf(0.07f, 0.55f, 7f, 0.82f),
        floatArrayOf(0.44f, 0.86f, 4.2f, 0.33f),
    )

    private fun drawClouds(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        if ((sky.cloud ushr 24) < 10) return
        val d = o.density
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val skyH = h * o.horizon
        for (cd in cloudDefs) {
            val cw = max(w, skyH) * 0.30f * cd[1]
            val ch = cw * 0.34f
            val span = w + cw
            val x = ((cd[3] * span + o.time * cd[2] * d) % span) - cw
            val y = skyH * cd[0] + skyH * 0.08f
            val path = Path()
            path.addCircle(x + cw * 0.30f, y + ch * 0.56f, ch * 0.30f, Path.Direction.CW)
            path.addCircle(x + cw * 0.49f, y + ch * 0.42f, ch * 0.42f, Path.Direction.CW)
            path.addCircle(x + cw * 0.69f, y + ch * 0.56f, ch * 0.32f, Path.Direction.CW)
            path.addRoundRect(x + cw * 0.12f, y + ch * 0.52f, x + cw * 0.88f, y + ch * 0.9f, ch * 0.19f, ch * 0.19f, Path.Direction.CW)
            p.shader = LinearGradient(
                0f, y, 0f, y + ch * 0.9f,
                sky.cloud, Sky.lerpColor(sky.cloud, Sky.withAlpha(sky.mid, (sky.cloud ushr 24) / 255f), 0.12f),
                Shader.TileMode.CLAMP,
            )
            val save = c.save()
            c.clipRect(x - 2f, y - ch, x + cw + 2f, y + ch * 0.9f)
            c.drawPath(path, p)
            c.restoreToCount(save)
        }
    }

    // ------------------------------------------------------------------ land

    private fun tri(u: Float, f: Float, ph: Float): Float {
        val v = ((u * f + ph) % 1f + 1f) % 1f
        return 1f - abs(v * 2f - 1f)
    }

    fun drawLand(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val baseY = h * o.horizon
        val n = 72

        // far mountains
        val far = Path()
        far.moveTo(0f, h)
        for (i in 0..n) {
            val u = i / n.toFloat()
            val ridge = 0.55f * tri(u, 2.2f, 0.15f).pow(1.5f) + 0.3f * tri(u, 4.7f, 0.6f) + 0.15f * tri(u, 10.3f, 0.1f)
            far.lineTo(w * u, baseY - ridge * h * 0.15f)
        }
        far.lineTo(w, h)
        far.close()
        p.shader = LinearGradient(
            0f, baseY - h * 0.15f, 0f, baseY + h * 0.05f,
            Sky.lerpColor(sky.far, sky.bottom, 0.15f), sky.far, Shader.TileMode.CLAMP,
        )
        c.drawPath(far, p)
        p.shader = null

        if (o.mode == HERO) {
            drawHeroFront(c, w, h, sky, o, p)
            return
        }

        // rolling hills
        val hillTop = baseY + h * 0.035f
        val hills = Path()
        hills.moveTo(0f, h)
        for (i in 0..n) {
            val u = i / n.toFloat()
            val y = hillTop + sin(u * 6.283f * 1.25f + 1.1f) * h * 0.028f + sin(u * 6.283f * 3.1f + 0.4f) * h * 0.012f
            hills.lineTo(w * u, y)
        }
        hills.lineTo(w, h)
        hills.close()
        p.color = sky.near
        c.drawPath(hills, p)

        if (o.trees) {
            val treeCol = Sky.darken(sky.near, 0.28f)
            val spots = floatArrayOf(0.05f, 0.11f, 0.17f, 0.80f, 0.87f, 0.94f)
            val sizes = floatArrayOf(1f, 0.75f, 0.9f, 0.85f, 1.05f, 0.7f)
            for (i in spots.indices) {
                val u = spots[i]
                val tx = w * u
                val ty = hillTop + sin(u * 6.283f * 1.25f + 1.1f) * h * 0.028f + sin(u * 6.283f * 3.1f + 0.4f) * h * 0.012f + h * 0.01f
                drawPine(c, tx, ty, h * 0.10f * sizes[i], treeCol)
            }
        }

        // meadow
        val gy = h * o.groundY
        val ground = Path()
        ground.moveTo(0f, gy + h * 0.03f)
        ground.quadTo(w * 0.5f, gy - h * 0.035f, w, gy + h * 0.03f)
        ground.lineTo(w, h)
        ground.lineTo(0f, h)
        ground.close()
        p.shader = LinearGradient(
            0f, gy - h * 0.03f, 0f, h,
            Sky.lighten(sky.ground, 0.06f), Sky.darken(sky.ground, 0.32f), Shader.TileMode.CLAMP,
        )
        c.drawPath(ground, p)
        p.shader = null

        // grass tufts
        val tuft = Paint(Paint.ANTI_ALIAS_FLAG)
        tuft.style = Paint.Style.STROKE
        tuft.strokeWidth = 1.6f * o.density
        tuft.strokeCap = Paint.Cap.ROUND
        tuft.color = Sky.lighten(sky.ground, 0.12f)
        val rnd = Rnd(3)
        val fx = w * o.fireX
        for (i in 0 until 16) {
            val x = rnd.next() * w
            val y = gy + h * 0.04f + rnd.next() * (h - gy - h * 0.06f)
            if (abs(x - fx) < w * 0.18f && y < gy + h * 0.12f) continue
            val s = (3f + rnd.next() * 3f) * o.density
            c.drawLine(x, y, x - s * 0.5f, y - s, tuft)
            c.drawLine(x, y, x, y - s * 1.2f, tuft)
            c.drawLine(x, y, x + s * 0.5f, y - s, tuft)
        }
    }

    private fun drawHeroFront(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts, p: Paint) {
        val n = 60
        // middle hills, halfway between the mountains and the sheet
        val mid = Path()
        mid.moveTo(0f, h)
        for (i in 0..n) {
            val u = i / n.toFloat()
            val y = h * 0.80f + sin(u * 6.283f * 1.1f + 2.2f) * h * 0.035f + sin(u * 6.283f * 2.6f) * h * 0.015f
            mid.lineTo(w * u, y)
        }
        mid.lineTo(w, h)
        mid.close()
        p.color = Sky.lerpColor(sky.far, o.sheet, 0.45f)
        c.drawPath(mid, p)
        if (o.trees) {
            val treeCol = Sky.lerpColor(sky.far, o.sheet, 0.25f)
            val spots = floatArrayOf(0.62f, 0.68f, 0.73f, 0.9f, 0.95f)
            val sizes = floatArrayOf(0.8f, 1f, 0.7f, 0.9f, 0.75f)
            for (i in spots.indices) {
                val u = spots[i]
                val ty = h * 0.80f + sin(u * 6.283f * 1.1f + 2.2f) * h * 0.035f + sin(u * 6.283f * 2.6f) * h * 0.015f + h * 0.01f
                drawPine(c, w * u, ty, h * 0.11f * sizes[i], treeCol)
            }
        }
        // the sheet wave
        val wave = Path()
        wave.moveTo(0f, h)
        for (i in 0..n) {
            val u = i / n.toFloat()
            val y = h * 0.905f + sin(u * 6.283f * 1.2f + 0.7f) * h * 0.022f + sin(u * 6.283f * 2.7f) * h * 0.01f
            wave.lineTo(w * u, y)
        }
        wave.lineTo(w, h + 2f)
        wave.lineTo(0f, h + 2f)
        wave.close()
        p.color = o.sheet
        c.drawPath(wave, p)
    }

    private fun drawPine(c: Canvas, x: Float, base: Float, size: Float, col: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = col
        c.drawRect(x - size * 0.05f, base - size * 0.18f, x + size * 0.05f, base, p)
        for (k in 0 until 3) {
            val top = base - size * (0.42f + k * 0.26f)
            val wid = size * (0.36f - k * 0.08f)
            val bot = base - size * (0.12f + k * 0.24f)
            val t = Path()
            t.moveTo(x, top - size * 0.12f)
            t.lineTo(x + wid, bot)
            t.lineTo(x - wid, bot)
            t.close()
            c.drawPath(t, p)
        }
    }

    // ------------------------------------------------------------------ fire

    fun drawFire(c: Canvas, w: Float, h: Float, sky: SkyState, o: Opts) {
        val look = o.look
        val d = o.density
        val gy = h * o.groundY
        val petPx = h * o.petSize
        val fx = w * o.fireX
        val baseY = gy + h * 0.045f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val pal = Palettes.get(look?.palette ?: 0)
        val dim = look == null || look.mood == Mood.SMOLDER || (look.form == 0 && look.warmth < 0.5f)

        // warm light on the ground
        val night = sky.night
        val glowR = w * 0.40f * (0.75f + 0.25f * night)
        val a = (0.10f + 0.30f * night) * (if (dim) 0.45f else 1f)
        p.shader = RadialGradient(
            fx, baseY - petPx * 0.2f, glowR,
            intArrayOf(Sky.withAlpha(pal.c1, a), Sky.withAlpha(pal.c1, a * 0.35f), Sky.withAlpha(pal.c1, 0f)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawCircle(fx, baseY - petPx * 0.2f, glowR, p)
        p.shader = null

        // stone ring
        val stoneTop = Sky.lerpColor(0xFFA7A9AF.toInt(), 0xFF454A66.toInt(), night)
        val stoneLow = Sky.lerpColor(0xFF77797F.toInt(), 0xFF2B2F47.toInt(), night)
        val sw = petPx * 0.085f
        val sh = petPx * 0.055f
        for (i in 0 until 7) {
            val ang = PI.toFloat() * (0.06f + 0.88f * i / 6f)
            val sx = fx + cos(ang) * petPx * 0.5f
            val sy = baseY + sin(ang) * petPx * 0.06f
            p.color = stoneLow
            c.drawOval(sx - sw, sy - sh, sx + sw, sy + sh, p)
            p.color = stoneTop
            c.drawOval(sx - sw * 0.8f, sy - sh, sx + sw * 0.7f, sy + sh * 0.2f, p)
        }

        // logs: one per task done today
        val logs = o.logs.coerceIn(0, 6)
        val logW = petPx * 0.44f
        val logH = petPx * 0.115f
        val rows = when {
            logs == 0 -> 0
            logs <= 3 -> 1
            logs <= 5 -> 2
            else -> 3
        }
        if (logs == 0) {
            p.color = Sky.lerpColor(0xFF4A3B36.toInt(), 0xFF241D24.toInt(), night)
            c.drawOval(fx - petPx * 0.3f, baseY - logH * 0.5f, fx + petPx * 0.3f, baseY + logH * 0.35f, p)
            p.color = Sky.withAlpha(pal.c0, 0.7f)
            c.drawOval(fx - petPx * 0.12f, baseY - logH * 0.3f, fx + petPx * 0.12f, baseY + logH * 0.1f, p)
        } else {
            val layout = intArrayOf(0, 0, 0, 1, 1, 2)
            for (row in 0 until rows) {
                val inRow = (0 until logs).count { layout[it] == row }
                val y = baseY - (row + 1) * logH * 0.9f
                val total = inRow * logW * 0.86f
                for (k in 0 until inRow) {
                    val x = fx - total / 2f + k * logW * 0.86f
                    drawLog(c, x, y, logW * 0.82f, logH)
                }
            }
        }
        val pileTop = baseY - rows * logH * 0.9f + (if (rows == 0) logH * 0.1f else logH * 0.25f)

        // the pet
        if (look != null) {
            val g = PetGrid.G.toFloat()
            val petTop = pileTop - petPx * (PetGrid.BASE / g)
            val save = c.save()
            c.scale(o.petScale, o.petScale, fx, pileTop)
            PetDraw.draw(c, fx - petPx / 2f, petTop, petPx, look, o.petPhase, glow = 0.35f + 0.65f * night)
            c.restoreToCount(save)

            if (o.sparks && look.form >= 1 && look.mood != Mood.SMOLDER) {
                val sp = Paint()
                for (i in 0 until 12) {
                    val t = ((o.time * 0.22f + i * 0.0833f) % 1f)
                    val sx = fx + sin(i * 12.9f + o.time * 0.7f) * petPx * 0.32f * (1f - t * 0.3f)
                    val sy = petTop + petPx * 0.35f - t * h * 0.45f
                    val s = (2f + (i % 3)) * d
                    sp.color = Sky.withAlpha(if (i % 2 == 0) pal.c3 else pal.c2, (1f - t) * 0.9f)
                    c.drawRect(sx, sy, sx + s, sy + s, sp)
                }
            }
        }

        // fireflies after dark
        if (night > 0.45f) {
            val ff = Paint(Paint.ANTI_ALIAS_FLAG)
            val rnd = Rnd(11)
            for (i in 0 until 9) {
                val bx = rnd.next() * w
                val by = h * o.horizon + rnd.next() * (h - h * o.horizon) * 0.8f
                val x = bx + sin(o.time * (0.3f + rnd.next() * 0.4f) + i) * 18f * d
                val y = by + cos(o.time * (0.25f + rnd.next() * 0.3f) + i * 2f) * 10f * d
                if (abs(x - fx) < petPx * 0.45f && y > baseY - petPx) continue
                val pulse = 0.5f + 0.5f * sin(o.time * 2f + i * 1.3f)
                val alpha = (night - 0.45f) / 0.55f * pulse
                ff.color = Sky.withAlpha(0xFFE6FF8A.toInt(), alpha * 0.35f)
                c.drawCircle(x, y, 4.5f * d, ff)
                ff.color = Sky.withAlpha(0xFFF4FFC4.toInt(), alpha)
                c.drawCircle(x, y, 1.6f * d, ff)
            }
        }
    }

    private fun drawLog(c: Canvas, x: Float, y: Float, lw: Float, lh: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF6B3D24.toInt()
        c.drawRoundRect(x, y, x + lw, y + lh, lh / 2f, lh / 2f, p)
        p.color = 0xFF8A5332.toInt()
        c.drawRoundRect(x + lh * 0.3f, y + lh * 0.18f, x + lw - lh * 0.7f, y + lh * 0.4f, lh * 0.1f, lh * 0.1f, p)
        p.color = 0xFFD9A066.toInt()
        c.drawCircle(x + lw - lh / 2f, y + lh / 2f, lh / 2f, p)
        p.color = 0xFFA9703F.toInt()
        c.drawCircle(x + lw - lh / 2f, y + lh / 2f, lh * 0.28f, p)
        p.color = 0xFFD9A066.toInt()
        c.drawCircle(x + lw - lh / 2f, y + lh / 2f, lh * 0.12f, p)
    }
}
