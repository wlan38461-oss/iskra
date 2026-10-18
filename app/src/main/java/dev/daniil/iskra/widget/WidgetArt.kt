package dev.daniil.iskra.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Texts
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.scene.SceneRenderer
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.scene.SkyState

class WidgetFonts(val black: Typeface, val bold: Typeface)

/** Pre-rendered widget pictures: the scene, the pet and the numbers in one bitmap. */
object WidgetArt {

    private fun rounded(c: Canvas, w: Float, h: Float, r: Float) {
        val clip = Path()
        clip.addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)
        c.clipPath(clip)
    }

    /** Text colour that reads on this sky. */
    fun lightSky(sky: SkyState): Boolean = Sky.luminance(Sky.lerpColor(sky.top, sky.mid, 0.5f)) > 0.5f

    private fun textPaint(font: Typeface, sizePx: Float, color: Int, light: Boolean): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = font
        p.textSize = sizePx
        p.color = color
        p.setShadowLayer(sizePx * 0.18f, 0f, sizePx * 0.05f, if (light) 0x66FFFFFF else 0x80000000.toInt())
        return p
    }

    /** Small flame drawn with paths (no emoji font needed). */
    fun flameIcon(c: Canvas, cx: Float, bottom: Float, size: Float, outer: Int, inner: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = size * 0.62f
        val path = Path()
        path.moveTo(cx, bottom - size)
        path.cubicTo(cx + w * 0.15f, bottom - size * 0.62f, cx + w * 0.62f, bottom - size * 0.55f, cx + w * 0.55f, bottom - size * 0.28f)
        path.cubicTo(cx + w * 0.5f, bottom - size * 0.05f, cx + w * 0.2f, bottom, cx, bottom)
        path.cubicTo(cx - w * 0.2f, bottom, cx - w * 0.5f, bottom - size * 0.05f, cx - w * 0.55f, bottom - size * 0.28f)
        path.cubicTo(cx - w * 0.6f, bottom - size * 0.5f, cx - w * 0.25f, bottom - size * 0.55f, cx, bottom - size)
        path.close()
        p.color = outer
        c.drawPath(path, p)
        val k = 0.55f
        val ip = Path()
        val iw = w * k
        val ib = bottom - size * 0.06f
        val isz = size * k
        ip.moveTo(cx, ib - isz)
        ip.cubicTo(cx + iw * 0.4f, ib - isz * 0.55f, cx + iw * 0.6f, ib - isz * 0.25f, cx + iw * 0.45f, ib - isz * 0.12f)
        ip.cubicTo(cx + iw * 0.3f, ib, cx - iw * 0.3f, ib, cx - iw * 0.45f, ib - isz * 0.12f)
        ip.cubicTo(cx - iw * 0.6f, ib - isz * 0.3f, cx - iw * 0.3f, ib - isz * 0.5f, cx, ib - isz)
        ip.close()
        p.color = inner
        c.drawPath(ip, p)
    }

    private fun checkMark(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.16f
        p.strokeCap = Paint.Cap.ROUND
        p.color = color
        val path = Path()
        path.moveTo(cx - s * 0.32f, cy + s * 0.02f)
        path.lineTo(cx - s * 0.08f, cy + s * 0.26f)
        path.lineTo(cx + s * 0.36f, cy - s * 0.24f)
        c.drawPath(path, p)
    }

    private fun ring(c: Canvas, cx: Float, cy: Float, r: Float, stroke: Float, progress: Float, accent: Int, track: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = stroke
        p.color = track
        c.drawCircle(cx, cy, r, p)
        if (progress > 0f) {
            p.strokeCap = Paint.Cap.ROUND
            p.color = accent
            c.drawArc(cx - r, cy - r, cx + r, cy + r, -90f, 360f * progress.coerceIn(0f, 1f), false, p)
        }
    }

    /** Small 2×2 widget: the campfire scene with the pet, streak and today's ring. */
    fun pet(w: Int, h: Int, d: Float, sky: SkyState, sm: Summary, fonts: WidgetFonts, time: Float = 2f): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val wf = w.toFloat()
        val hf = h.toFloat()
        rounded(c, wf, hf, 22f * d)
        SceneRenderer.draw(
            c, wf, hf, sky,
            SceneRenderer.Opts(
                mode = SceneRenderer.FULL, time = time, density = d, look = sm.look, logs = sm.done,
                horizon = 0.56f, groundY = 0.74f, petSize = 0.40f, arcTop = 0.2f, arcFrom = 0.55f, arcTo = 0.95f,
                stars = 30, celestialSize = 0.8f, trees = wf > 150f * d, sparks = true,
            ),
        )
        val light = lightSky(sky)
        val fg = if (light) 0xFF14263A.toInt() else 0xFFFFFFFF.toInt()
        val dim = if (light) 0xCC14263A.toInt() else 0xD9FFFFFF.toInt()
        val pad = 12f * d
        val big = textPaint(fonts.black, 30f * d, fg, light)
        c.drawText("${sm.streak}", pad, pad + 26f * d, big)
        val numW = big.measureText("${sm.streak}")
        val pal0 = Palettes.get(sm.palette)
        flameIcon(c, pad + numW + 11f * d, pad + 24f * d, 19f * d, pal0.c1, pal0.c3)
        val small = textPaint(fonts.bold, 11f * d, dim, light)
        c.drawText(Texts.plural(sm.streak, "день подряд", "дня подряд", "дней подряд"), pad, pad + 41f * d, small)

        val pal = Palettes.get(sm.palette)
        val r = 15f * d
        val cx = wf - pad - r
        val cy = pad + r
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.color = if (light) 0x8CFFFFFF.toInt() else 0x59000000
        c.drawCircle(cx, cy, r + 3f * d, bg)
        ring(
            c, cx, cy, r, 4f * d,
            if (sm.total == 0) 0f else sm.done.toFloat() / sm.total,
            pal.accent.toInt(), if (light) 0x33000000 else 0x40FFFFFF,
        )
        if (sm.complete) {
            checkMark(c, cx, cy, r * 1.1f, fg)
        } else {
            val label = if (sm.total == 0) "+" else "${sm.done}/${sm.total}"
            val lp = textPaint(fonts.black, (if (label.length > 3) 9.5f else 11f) * d, fg, light)
            lp.textAlign = Paint.Align.CENTER
            lp.clearShadowLayer()
            c.drawText(label, cx, cy + lp.textSize * 0.36f, lp)
        }
        return b
    }

    /** 4×1 strip: pet on the left, streak, the week and today's bar. */
    fun strip(w: Int, h: Int, d: Float, sky: SkyState, sm: Summary, fonts: WidgetFonts, time: Float = 2f): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val wf = w.toFloat()
        val hf = h.toFloat()
        rounded(c, wf, hf, 22f * d)
        val petArea = hf * 1.15f
        SceneRenderer.draw(
            c, wf, hf, sky,
            SceneRenderer.Opts(
                mode = SceneRenderer.FULL, time = time, density = d, look = sm.look, logs = sm.done,
                horizon = 0.5f, groundY = 0.70f, petSize = 0.62f, fireX = (petArea / 2f) / wf,
                arcTop = 0.18f, arcFrom = 0.5f, arcTo = 0.98f, stars = 40, celestialSize = 0.55f,
                trees = false, sparks = false,
            ),
        )
        val light = lightSky(sky)
        // soft scrim behind the text
        val scrim = Paint()
        val sc = if (light) 0x00FFFFFF else 0x00000000
        val sa = if (light) 0x73FFFFFF else 0x73000000
        scrim.shader = LinearGradient(petArea * 0.8f, 0f, petArea * 1.5f, 0f, sc, sa, Shader.TileMode.CLAMP)
        c.drawRect(petArea * 0.8f, 0f, wf, hf, scrim)

        val fg = if (light) 0xFF14263A.toInt() else 0xFFFFFFFF.toInt()
        val dim = if (light) 0xCC14263A.toInt() else 0xD9FFFFFF.toInt()
        val x0 = petArea
        val big = textPaint(fonts.black, 28f * d, fg, light)
        val baseY = hf / 2f + 2f * d
        c.drawText("${sm.streak}", x0, baseY, big)
        val nw = big.measureText("${sm.streak}")
        val small = textPaint(fonts.bold, 11f * d, dim, light)
        val pal0 = Palettes.get(sm.palette)
        flameIcon(c, x0 + nw + 10f * d, baseY - 1f * d, 17f * d, pal0.c1, pal0.c3)
        c.drawText(Texts.plural(sm.streak, "день подряд", "дня подряд", "дней подряд"), x0, baseY + 15f * d, small)

        // week dots
        val pal = Palettes.get(sm.palette)
        val dotR = 5.5f * d
        val gap = 6f * d
        val right = wf - 14f * d
        val weekW = 7 * dotR * 2 + 6 * gap
        val wx = right - weekW
        val wy = hf / 2f - 8f * d
        val dp = Paint(Paint.ANTI_ALIAS_FLAG)
        sm.week.forEachIndexed { i, cell ->
            val cx = wx + dotR + i * (dotR * 2 + gap)
            dp.style = Paint.Style.FILL
            dp.color = when (cell.state) {
                1 -> pal.accent.toInt()
                2 -> 0xFF8A8494.toInt()
                else -> if (light) 0x33000000 else 0x40FFFFFF
            }
            c.drawCircle(cx, wy, dotR, dp)
            if (i == sm.week.lastIndex) {
                dp.style = Paint.Style.STROKE
                dp.strokeWidth = 1.6f * d
                dp.color = pal.accent2.toInt()
                c.drawCircle(cx, wy, dotR + 2.5f * d, dp)
            }
        }
        // today bar
        val by = wy + dotR + 12f * d
        val bh = 5f * d
        val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        bar.color = if (light) 0x26000000 else 0x33FFFFFF
        c.drawRoundRect(wx, by, right, by + bh, bh / 2, bh / 2, bar)
        if (sm.total > 0 && sm.done > 0) {
            bar.color = pal.accent.toInt()
            c.drawRoundRect(wx, by, wx + (right - wx) * sm.done / sm.total, by + bh, bh / 2, bh / 2, bar)
        }
        val cap = textPaint(fonts.bold, 10f * d, dim, light)
        val capText = when {
            sm.total == 0 -> "добавь дела"
            sm.complete -> "день засчитан!"
            else -> "сегодня ${sm.done} из ${sm.total}"
        }
        c.drawText(capText, wx, by + bh + 12f * d, cap)
        return b
    }

    /** 4×2 list widget background: scene with the pet on the left, glass panel on the right. */
    fun today(w: Int, h: Int, d: Float, sky: SkyState, sm: Summary, leftW: Float, panelLight: Boolean, time: Float = 2f): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val wf = w.toFloat()
        val hf = h.toFloat()
        rounded(c, wf, hf, 22f * d)
        SceneRenderer.draw(
            c, wf, hf, sky,
            SceneRenderer.Opts(
                mode = SceneRenderer.FULL, time = time, density = d, look = sm.look, logs = sm.done,
                horizon = 0.56f, groundY = 0.74f, petSize = 0.36f, fireX = (leftW / 2f) / wf,
                arcTop = 0.14f, arcFrom = 0.04f, arcTo = (leftW / wf) * 0.95f, stars = 50, celestialSize = 0.7f,
                trees = false, sparks = true,
            ),
        )
        val m = 8f * d
        val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        panel.color = if (panelLight) 0xE0FFFFFF.toInt() else 0xC70E1229.toInt()
        c.drawRoundRect(leftW, m, wf - m, hf - m, 16f * d, 16f * d, panel)
        return b
    }

    /** Round check box for the list widget. */
    fun check(px: Int, done: Boolean, accent: Int, light: Boolean): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = px / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (done) {
            p.color = accent
            c.drawCircle(r, r, r * 0.92f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = px * 0.12f
            p.strokeCap = Paint.Cap.ROUND
            p.color = 0xFF2A1206.toInt()
            val path = Path()
            path.moveTo(px * 0.30f, px * 0.52f)
            path.lineTo(px * 0.45f, px * 0.66f)
            path.lineTo(px * 0.71f, px * 0.37f)
            c.drawPath(path, p)
        } else {
            p.style = Paint.Style.STROKE
            p.strokeWidth = px * 0.1f
            p.color = if (light) 0x66000000 else 0x80FFFFFF.toInt()
            c.drawCircle(r, r, r * 0.8f, p)
        }
        return b
    }
}
