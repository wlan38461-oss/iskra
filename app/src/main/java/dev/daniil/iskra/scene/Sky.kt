package dev.daniil.iskra.scene

enum class DayPhase(val title: String, val emoji: String, val light: Boolean) {
    NIGHT("Ночь", "🌙", false),
    DAWN("Рассвет", "🌅", false),
    MORNING("Утро", "🌤️", true),
    DAY("День", "☀️", true),
    SUNSET("Закат", "🌇", false),
    DUSK("Вечер", "🌆", false),
}

/** Colours and celestial positions for one minute of the day. All colours are ARGB ints. */
data class SkyState(
    val minute: Int,
    val phase: DayPhase,
    val top: Int,
    val mid: Int,
    val bottom: Int,
    val far: Int,
    val near: Int,
    val ground: Int,
    val cloud: Int,
    /** 0..1 star visibility */
    val stars: Float,
    /** 0..1 how dark it is (fire glow, fireflies) */
    val night: Float,
    /** position along the sun arc 0..1, or -1 when the sun is down */
    val sunT: Float,
    /** position along the moon arc 0..1, or -1 when the moon is down */
    val moonT: Float,
)

object Sky {
    private class K(
        val m: Int, val top: Long, val mid: Long, val bottom: Long,
        val far: Long, val near: Long, val ground: Long, val cloud: Long,
        val stars: Float, val night: Float,
    )

    private val keys = listOf(
        K(0, 0xFF060A1E, 0xFF0E1638, 0xFF1D2350, 0xFF171D44, 0xFF0F1433, 0xFF0C1129, 0x18FFFFFF, 1f, 1f),
        K(270, 0xFF0A0F2B, 0xFF1B1F4D, 0xFF3A2E66, 0xFF201F4C, 0xFF16173A, 0xFF111330, 0x1CFFFFFF, 0.9f, 1f),
        K(330, 0xFF1E2552, 0xFF5A4C8C, 0xFFF0A08C, 0xFF4B3F79, 0xFF2F2A56, 0xFF262043, 0x50FFC8D2, 0.4f, 0.7f),
        K(405, 0xFF3E64A8, 0xFFD69AB5, 0xFFFFD0A0, 0xFF8E7DB0, 0xFF5B5088, 0xFF433C6A, 0xA6FFE0E6, 0.05f, 0.35f),
        K(480, 0xFF63B0F0, 0xFFA8D8F8, 0xFFFFF0D2, 0xFF9DBEDC, 0xFF79AE6E, 0xFF64A05B, 0xEBFFFFFF, 0f, 0f),
        K(690, 0xFF3D97E8, 0xFF86C8F6, 0xFFDDF1FF, 0xFFA3C6E3, 0xFF62A85C, 0xFF52A04E, 0xF5FFFFFF, 0f, 0f),
        K(960, 0xFF4A95DE, 0xFF95C9F0, 0xFFF3EFD8, 0xFFAAC3D8, 0xFF6AA65C, 0xFF5A9852, 0xF0FFFFFF, 0f, 0f),
        K(1065, 0xFF4B6FB8, 0xFFE7A97C, 0xFFFFD08A, 0xFFB58C8E, 0xFF7A7150, 0xFF62603F, 0xE0FFE1C2, 0f, 0.15f),
        K(1140, 0xFF2D2F6E, 0xFFC2577A, 0xFFFF9150, 0xFF7C4069, 0xFF412849, 0xFF30203A, 0xBFFFB4A0, 0.1f, 0.5f),
        K(1215, 0xFF171A45, 0xFF3E2F6E, 0xFF8E4A7A, 0xFF372B5C, 0xFF211B40, 0xFF1A1631, 0x48C8B4E6, 0.5f, 0.85f),
        K(1320, 0xFF0A0F2C, 0xFF151D48, 0xFF262A5C, 0xFF1A2046, 0xFF11163B, 0xFF0D112A, 0x1CFFFFFF, 0.95f, 1f),
        K(1440, 0xFF060A1E, 0xFF0E1638, 0xFF1D2350, 0xFF171D44, 0xFF0F1433, 0xFF0C1129, 0x18FFFFFF, 1f, 1f),
    )

    /** Minutes where the UI phase changes (used to refresh widgets). */
    val phaseStarts = listOf(300, 450, 660, 1020, 1170, 1290)

    fun phaseAt(minute: Int): DayPhase = when (((minute % 1440) + 1440) % 1440) {
        in 300 until 450 -> DayPhase.DAWN
        in 450 until 660 -> DayPhase.MORNING
        in 660 until 1020 -> DayPhase.DAY
        in 1020 until 1170 -> DayPhase.SUNSET
        in 1170 until 1290 -> DayPhase.DUSK
        else -> DayPhase.NIGHT
    }

    fun lerpColor(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        fun ch(c: Int, s: Int) = (c ushr s) and 0xFF
        fun mix(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * tt).toInt().coerceIn(0, 255)
        return (mix(24) shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    fun withAlpha(c: Int, a: Float): Int =
        (((c ushr 24) * a.coerceIn(0f, 1f)).toInt() shl 24) or (c and 0x00FFFFFF)

    fun darken(c: Int, k: Float): Int = lerpColor(c, (c and 0xFF000000.toInt()), k)

    fun lighten(c: Int, k: Float): Int = lerpColor(c, (c and 0xFF000000.toInt()) or 0x00FFFFFF, k)

    /** Perceived luminance 0..1. */
    fun luminance(c: Int): Float {
        val r = ((c shr 16) and 0xFF) / 255f
        val g = ((c shr 8) and 0xFF) / 255f
        val b = (c and 0xFF) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    fun sunT(minute: Int): Float {
        val m = ((minute % 1440) + 1440) % 1440
        return if (m in 320..1180) (m - 320) / 860f else -1f
    }

    fun moonT(minute: Int): Float {
        val m = ((minute % 1440) + 1440) % 1440
        val since = (m - 1170 + 1440) % 1440
        return if (since <= 660) since / 660f else -1f
    }

    fun at(minute: Int): SkyState {
        val m = ((minute % 1440) + 1440) % 1440
        var i = 0
        while (i < keys.size - 2 && keys[i + 1].m <= m) i++
        val a = keys[i]
        val b = keys[i + 1]
        val t = (m - a.m).toFloat() / (b.m - a.m)
        fun c(x: Long, y: Long) = lerpColor(x.toInt(), y.toInt(), t)
        return SkyState(
            minute = m,
            phase = phaseAt(m),
            top = c(a.top, b.top),
            mid = c(a.mid, b.mid),
            bottom = c(a.bottom, b.bottom),
            far = c(a.far, b.far),
            near = c(a.near, b.near),
            ground = c(a.ground, b.ground),
            cloud = c(a.cloud, b.cloud),
            stars = a.stars + (b.stars - a.stars) * t,
            night = a.night + (b.night - a.night) * t,
            sunT = sunT(m),
            moonT = moonT(m),
        )
    }
}
