package dev.daniil.iskra.pet

/** Flame colour themes. [unlockAt] is the best streak needed to unlock. */
class FlamePalette(
    val name: String,
    val unlockAt: Int,
    val edge: Int,
    val c0: Int,
    val c1: Int,
    val c2: Int,
    val c3: Int,
    /** UI accent (ARGB) */
    val accent: Long,
    /** UI second accent (ARGB) */
    val accent2: Long,
)

object Palettes {
    val all = listOf(
        FlamePalette(
            "Костёр", 0,
            0xFF7A1C10.toInt(), 0xFFE8521C.toInt(), 0xFFFF8C2A.toInt(), 0xFFFFD23F.toInt(), 0xFFFFF6C8.toInt(),
            0xFFFF8C2A, 0xFFFFD23F,
        ),
        FlamePalette(
            "Газовая горелка", 3,
            0xFF0D2A6B.toInt(), 0xFF1E5BD8.toInt(), 0xFF3D9BFF.toInt(), 0xFF8FD3FF.toInt(), 0xFFE8F8FF.toInt(),
            0xFF4AA3FF, 0xFF9AD8FF,
        ),
        FlamePalette(
            "Изумруд", 7,
            0xFF0B4A2A.toInt(), 0xFF159947.toInt(), 0xFF2FD06A.toInt(), 0xFF9DF27A.toInt(), 0xFFF0FFD8.toInt(),
            0xFF34D27A, 0xFFB4F28A,
        ),
        FlamePalette(
            "Аметист", 14,
            0xFF3A0F5E.toInt(), 0xFF7A2BD0.toInt(), 0xFFB05CFF.toInt(), 0xFFE0A6FF.toInt(), 0xFFFFF0FF.toInt(),
            0xFFB86BFF, 0xFFE8B4FF,
        ),
    )

    fun get(i: Int): FlamePalette = all.getOrElse(i) { all[0] }
}
