package dev.daniil.iskra.pet

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class Mood { HAPPY, NORMAL, HUNGRY, SMOLDER }

/**
 * Everything needed to draw the pet.
 * [form]: 0 egg, 1 Огонёк, 2 Пламя, 3 Феникс. [growth]: 0..1 inside the form.
 * [warmth]: egg only — lit days plus today's progress (0..3).
 */
data class PetLook(
    val form: Int,
    val growth: Float,
    val mood: Mood,
    val palette: Int = 0,
    val warmth: Float = 0f,
)

/** Procedural pixel art on a GxG grid. Pure Kotlin, so it renders anywhere. */
object PetGrid {
    const val G = 32
    private const val CX = 16f
    /** y (in grid cells) where the pet stands. */
    const val BASE = 29.5f

    class Pal(val edge: Int, val c0: Int, val c1: Int, val c2: Int, val c3: Int)

    private val ASH = Pal(0xFF2A221F.toInt(), 0xFF5A4038.toInt(), 0xFF7A5040.toInt(), 0xFF9A6448.toInt(), 0xFFB07850.toInt())
    private val SHELL = Pal(0xFF1C1218.toInt(), 0xFF35242C.toInt(), 0xFF4E333A.toInt(), 0xFF6B4549.toInt(), 0xFF8F5E5C.toInt())
    private val SHELL_COLD = Pal(0xFF22201F.toInt(), 0xFF3B3533.toInt(), 0xFF514846.toInt(), 0xFF6A5F5B.toInt(), 0xFF8B7F7A.toInt())
    val SILHOUETTE = 0xFF3A2C27.toInt()
    private val EYE = 0xFF2B0F06.toInt()
    private val SHINE = 0xFFFFFFFF.toInt()
    private val BLUSH = 0xFFFF7A9A.toInt()
    private val SMOKE = 0x998A8580.toInt()

    fun pal(i: Int): Pal {
        val f = Palettes.get(i)
        return Pal(f.edge, f.c0, f.c1, f.c2, f.c3)
    }

    private fun wingPal(i: Int): Pal {
        val f = Palettes.get(i)
        return Pal(f.edge, f.edge, f.c0, f.c1, f.c2)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun shade(p: Pal, heat: Float): Int = when {
        heat < 0.28f -> p.c0
        heat < 0.52f -> p.c1
        heat < 0.76f -> p.c2
        else -> p.c3
    }

    /** Egg wobble in degrees: short bursts every few seconds, stronger when about to hatch. */
    fun eggWobble(look: PetLook, phase: Float): Float {
        if (look.form != 0 || look.mood == Mood.SMOLDER) return 0f
        val burst = max(0f, sin(phase * 0.2f)).pow(12)
        val amp = 2.5f + 2.5f * look.warmth.coerceIn(0f, 3f)
        return sin(phase * 2.6f) * amp * burst
    }

    fun build(look: PetLook, phase: Float, silhouette: Boolean = false): IntArray {
        val out = if (look.form <= 0) egg(look, phase) else flame(look, phase)
        if (silhouette) {
            for (i in out.indices) if (out[i] != 0) out[i] = SILHOUETTE
        }
        return out
    }

    // ---------------------------------------------------------------- flame

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
        val off = sin(phase + py * 0.42f) * amp * lift
        val dx = abs(px - cx - off)
        if (dx > w) return -1f
        val nx = if (w > 0f) dx / w else 1f
        val vy = if (py >= cy) (py - cy) / r else (cy - py) / h
        return (1f - max(nx, vy * 0.9f)).coerceIn(0f, 1f)
    }

    private fun flame(look: PetLook, phase: Float): IntArray {
        val g = IntArray(G * G)
        val mood = look.mood
        val smolder = mood == Mood.SMOLDER
        val fire = pal(look.palette)
        val body = if (smolder) ASH else fire
        val gr = look.growth.coerceIn(0f, 1f)
        val form = look.form.coerceIn(1, 3)

        val r: Float
        val h0: Float
        when (form) {
            1 -> { r = lerp(4.2f, 5.8f, gr); h0 = lerp(6.5f, 10f, gr) }
            2 -> { r = lerp(6.0f, 7.2f, gr); h0 = lerp(10f, 12.5f, gr) }
            else -> { r = lerp(6.6f, 7.6f, gr); h0 = lerp(11f, 13f, gr) }
        }
        val h = h0 * when (mood) {
            Mood.SMOLDER -> 0.55f
            Mood.HUNGRY -> 0.85f
            Mood.HAPPY -> 1.07f
            else -> 1f
        }
        val amp = when (mood) {
            Mood.SMOLDER -> 0.35f
            Mood.HAPPY -> 1.9f
            else -> 1.35f
        }
        val cy = BASE - r
        val cx = CX

        // Phoenix wings, behind the body
        if (form >= 3 && !smolder) {
            val wp = wingPal(look.palette)
            val flap = sin(phase * 2f)
            val len = lerp(4.8f, 7.6f, gr)
            for (y in 0 until G) for (x in 0 until G) {
                val px = x + 0.5f
                val py = y + 0.5f
                val dist = abs(px - cx) - (r - 1.2f)
                if (dist <= 0f || dist > len) continue
                val t = dist / len
                val top = cy - 2f - t * 8f - flap * t * 2f
                val feather = if (x % 2 == 0) 1.6f else 0f
                val bottom = cy + 4f - t * 5f + feather - flap * t * 1.6f
                if (py in top..bottom) g[y * G + x] = shade(wp, 1f - t)
            }
        }

        // Body and extra tongues
        val parts = mutableListOf(floatArrayOf(cx, cy, r, h, amp, phase))
        if (form == 2 && !smolder) {
            val tr = lerp(2.0f, 2.9f, gr)
            val th = lerp(4.5f, 7f, gr)
            parts += floatArrayOf(cx - r - 0.4f, cy + r * 0.3f, tr, th, 0.8f, phase + 1.7f)
            parts += floatArrayOf(cx + r + 0.4f, cy + r * 0.3f, tr, th, 0.8f, phase + 3.1f)
        }
        if (form >= 3 && !smolder) {
            parts += floatArrayOf(cx - 3.4f, cy - h * 0.45f, 2.0f, 6f, 0.6f, phase + 1f)
            parts += floatArrayOf(cx + 3.4f, cy - h * 0.45f, 2.0f, 6f, 0.6f, phase + 2.2f)
        }
        for (y in 0 until G) for (x in 0 until G) {
            var best = -1f
            for (p in parts) {
                val hh = flameHeat(x + 0.5f, y + 0.5f, p[0], p[1], p[2], p[3], p[4], p[5])
                if (hh > best) best = hh
            }
            if (best >= 0f) g[y * G + x] = shade(body, best)
        }

        val out = outline(g, body.edge)

        fun put(x: Int, y: Int, c: Int) {
            if (x in 0 until G && y in 0 until G) out[y * G + x] = c
        }
        fun sym(x: Int, y: Int, c: Int) {
            put(x, y, c)
            put(G - 1 - x, y, c)
        }

        // Face
        val eh = if (r >= 5.2f) 3 else 2
        val gap = (r * 0.30f).coerceIn(1f, 2.6f)
        val lx = (cx - gap - 2f).roundToInt() // left eye: columns lx, lx+1
        val rx = G - 2 - lx // right eye: columns rx, rx+1 (mirror)
        val ey = (cy - r * (if (r < 5.2f) 0.22f else 0.36f) - eh / 2f).roundToInt()
        val my = ey + eh + 1
        val blink = ((phase / (2f * PI.toFloat())) % 3f) > 2.84f

        when {
            mood == Mood.SMOLDER || blink -> {
                for (i in 0..1) sym(lx + i, ey + eh - 1, EYE)
            }
            mood == Mood.HAPPY -> {
                // ^ ^
                sym(lx, ey + 1, EYE)
                sym(lx + 1, ey + 1, EYE)
                sym(lx - 1, ey + 2, EYE)
                sym(lx + 2, ey + 2, EYE)
            }
            else -> {
                for (i in 0..1) for (j in 0 until eh) sym(lx + i, ey + j, EYE)
                put(lx, ey, SHINE)
                put(rx, ey, SHINE)
            }
        }

        when (mood) {
            Mood.HAPPY -> {
                sym(14, my, EYE)
                sym(15, my + 1, EYE)
                if (eh == 3) {
                    sym(lx - 1, my, BLUSH)
                    sym(lx, my, BLUSH)
                }
            }
            Mood.HUNGRY -> {
                sym(15, my, EYE)
                sym(15, my + 1, EYE)
            }
            Mood.SMOLDER -> sym(15, my + 1, EYE)
            Mood.NORMAL -> {
                sym(15, my, EYE)
                if (eh == 3) sym(14, my - 1, EYE)
            }
        }

        // Particles
        val top = cy - h
        if (smolder) {
            for (i in 0 until 3) {
                val t = ((phase * 0.5f + i * 2.1f) % 6.283f) / 6.283f
                val sy = (top - 1f - t * 9f).toInt()
                val sx = (cx + sin(phase + i * 2f) * 2.4f).toInt()
                put(sx, sy, SMOKE)
                put(sx + 1, sy, SMOKE)
            }
        } else if (mood == Mood.HAPPY) {
            for (i in 0 until 4) {
                val t = ((phase * 0.7f + i * 1.7f) % 6.283f) / 6.283f
                val sy = (top - t * 7f).toInt()
                val sx = (cx + sin(phase * 1.3f + i * 2.4f) * (r + 1.5f)).toInt()
                put(sx, sy, fire.c3)
            }
        }
        return out
    }

    // ---------------------------------------------------------------- egg

    private const val EGG_CY = 21.0f
    private const val EGG_RX = 7.7f
    private const val EGG_RYT = 10.0f
    private const val EGG_RYB = 8.2f

    /** Crack polylines (grid coordinates). 0 and 1 appear on days 1 and 2, 2 grows on hatch day. */
    private val crackLines = listOf(
        intArrayOf(19, 15, 18, 17, 20, 19, 18, 21, 19, 23),
        intArrayOf(12, 16, 11, 18, 13, 20, 11, 22, 12, 24),
        intArrayOf(10, 25, 12, 26, 14, 25, 16, 26, 18, 25, 20, 26, 22, 25),
    )
    private val crackPixels: List<List<Int>> = crackLines.map { pts ->
        val res = mutableListOf<Int>()
        var i = 0
        while (i + 3 < pts.size) {
            line(pts[i], pts[i + 1], pts[i + 2], pts[i + 3]) { x, y ->
                val k = y * G + x
                if (res.isEmpty() || res.last() != k) res += k
            }
            i += 2
        }
        res
    }

    private inline fun line(x0: Int, y0: Int, x1: Int, y1: Int, plot: (Int, Int) -> Unit) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            plot(x, y)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    private fun egg(look: PetLook, phase: Float): IntArray {
        val g = IntArray(G * G)
        val inside = BooleanArray(G * G)
        val cold = look.mood == Mood.SMOLDER
        val shell = if (cold) SHELL_COLD else SHELL
        val fire = if (cold) ASH else pal(look.palette)
        val warm = look.warmth.coerceIn(0f, 3f)

        for (y in 0 until G) for (x in 0 until G) {
            val dx = x + 0.5f - CX
            val dy = y + 0.5f - EGG_CY
            val ry = if (dy < 0f) EGG_RYT else EGG_RYB
            val rxx = if (dy < 0f) EGG_RX * (1f - 0.1f * (-dy / EGG_RYT)) else EGG_RX
            val d = (dx / rxx) * (dx / rxx) + (dy / ry) * (dy / ry)
            if (d > 1f) continue
            val k = y * G + x
            inside[k] = true
            val edge = sqrt(d)
            val light = (-dx / rxx) * 0.55f + (-dy / ry) * 0.45f
            val tone = 0.52f + 0.36f * light - 0.5f * edge.pow(5)
            g[k] = when {
                tone < 0.2f -> shell.c0
                tone < 0.46f -> shell.c1
                tone < 0.7f -> shell.c2
                else -> shell.c3
            }
            // warm speckles
            val hsh = ((x * 73856093) xor (y * 19349663)) and 0xFF
            if (hsh < 16 && edge < 0.82f && tone < 0.7f) g[k] = if (cold) shell.c0 else fire.c0
        }
        // glossy highlight
        for ((x, y) in listOf(12 to 15, 12 to 16, 13 to 14, 11 to 17)) {
            val k = y * G + x
            if (inside[k]) g[k] = if (cold) shell.c3 else 0xFFC99A86.toInt()
        }

        // Cracks: glow halo first, then bright cores
        val cores = mutableListOf<Int>()
        for (c in 0 until 3) {
            val amount = (warm - c).coerceIn(0f, 1f)
            if (amount <= 0f) break
            val pix = crackPixels[c]
            val n = if (amount >= 1f) pix.size else (pix.size * amount).roundToInt().coerceAtLeast(1)
            for (i in 0 until n) cores += pix[i]
        }
        for (k in cores) {
            val x = k % G
            val y = k / G
            for ((ox, oy) in NEIGH) {
                val nx = x + ox
                val ny = y + oy
                if (nx !in 0 until G || ny !in 0 until G) continue
                val nk = ny * G + nx
                if (inside[nk]) g[nk] = fire.c0
            }
        }
        for ((i, k) in cores.withIndex()) {
            val pulse = sin(phase * 0.9f + i * 0.55f) > 0.35f
            g[k] = if (pulse) fire.c3 else fire.c2
        }

        // Peeking eyes through a hole near the top on the last day
        if (warm >= 2f) {
            val hole = listOf(
                14 to 14, 15 to 14, 16 to 14, 17 to 14,
                13 to 15, 14 to 15, 15 to 15, 16 to 15, 17 to 15, 18 to 15,
                13 to 16, 14 to 16, 15 to 16, 16 to 16, 17 to 16, 18 to 16,
                14 to 17, 15 to 17, 16 to 17, 17 to 17,
            )
            for ((x, y) in hole) g[y * G + x] = fire.c2
            for ((x, y) in listOf(15 to 15, 16 to 15, 15 to 16, 16 to 16)) g[y * G + x] = fire.c3
            val blink = ((phase / (2f * PI.toFloat())) % 3f) > 2.8f
            if (!cold) {
                if (blink) {
                    g[16 * G + 14] = EYE
                    g[16 * G + 17] = EYE
                } else {
                    for ((x, y) in listOf(14 to 15, 14 to 16, 17 to 15, 17 to 16)) g[y * G + x] = EYE
                }
            }
        }

        val out = outline(g, shell.edge)

        // Sparks drifting up from a warm egg
        if (!cold && warm >= 1f) {
            for (i in 0 until 2) {
                val t = ((phase * 0.45f + i * 3.1f) % 6.283f) / 6.283f
                val sy = (12f - t * 9f).toInt()
                val sx = (CX + sin(phase * 0.8f + i * 2.7f) * 5f).toInt()
                if (sx in 0 until G && sy in 0 until G && out[sy * G + sx] == 0) out[sy * G + sx] = fire.c3
            }
        }
        return out
    }

    private val NEIGH = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    private fun outline(g: IntArray, edge: Int): IntArray {
        val out = g.copyOf()
        for (y in 0 until G) for (x in 0 until G) {
            if (g[y * G + x] != 0) continue
            val n = (x > 0 && g[y * G + x - 1] != 0) ||
                (x < G - 1 && g[y * G + x + 1] != 0) ||
                (y > 0 && g[(y - 1) * G + x] != 0) ||
                (y < G - 1 && g[(y + 1) * G + x] != 0)
            if (n) out[y * G + x] = edge
        }
        return out
    }
}
