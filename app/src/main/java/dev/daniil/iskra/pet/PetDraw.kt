package dev.daniil.iskra.pet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/** Draws [PetGrid] pixel art onto an android.graphics.Canvas. */
object PetDraw {

    fun draw(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float,
        look: PetLook,
        phase: Float,
        glow: Float = 0f,
        silhouette: Boolean = false,
        silColor: Int = PetGrid.SILHOUETTE,
    ) {
        if (glow > 0f && !silhouette) drawGlow(canvas, left, top, size, look, glow)
        val grid = PetGrid.build(look, phase, silhouette, silColor)
        val g = PetGrid.G
        val s = size / g
        val angle = if (silhouette) 0f else PetGrid.eggWobble(look, phase)
        val save = canvas.save()
        if (angle != 0f) canvas.rotate(angle, left + size / 2f, top + size * (PetGrid.BASE / g))
        val p = Paint()
        p.isAntiAlias = false
        p.style = Paint.Style.FILL
        for (y in 0 until g) for (x in 0 until g) {
            val c = grid[y * g + x]
            if (c == 0) continue
            p.color = c
            canvas.drawRect(left + x * s, top + y * s, left + (x + 1) * s, top + (y + 1) * s, p)
        }
        canvas.restoreToCount(save)
    }

    private fun drawGlow(canvas: Canvas, left: Float, top: Float, size: Float, look: PetLook, k: Float) {
        val f = Palettes.get(look.palette)
        val base = f.c1 and 0x00FFFFFF
        val strength = when {
            look.form == 0 && look.mood == Mood.SMOLDER -> 0.0f
            look.form == 0 -> 0.10f + 0.09f * look.warmth.coerceIn(0f, 3f)
            look.mood == Mood.SMOLDER -> 0.16f
            else -> 0.38f
        }
        if (strength <= 0f) return
        val a = (strength * k * 255).toInt().coerceIn(0, 255)
        val gx = left + size / 2f
        val gy = top + size * 0.66f
        val rad = size * 0.58f
        val gp = Paint()
        gp.isAntiAlias = true
        gp.shader = RadialGradient(gx, gy, rad, (a shl 24) or base, base, Shader.TileMode.CLAMP)
        canvas.drawCircle(gx, gy, rad, gp)
    }

    fun bitmap(px: Int, look: PetLook, phase: Float = 0.6f, glow: Float = 0f): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        draw(Canvas(b), 0f, 0f, px.toFloat(), look, phase, glow)
        return b
    }
}
