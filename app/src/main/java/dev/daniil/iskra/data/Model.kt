package dev.daniil.iskra.data

import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.pet.PetLook
import java.time.LocalDate

/** [remind] is a minute of the day for a reminder, or -1. */
data class Task(val id: Long, val title: String, val emoji: String, val remind: Int = -1)

data class Settings(
    val petName: String = "Искра",
    val userName: String = "",
    val palette: Int = 0,
    val vibration: Boolean = true,
    /** colours follow the time of day */
    val autoTheme: Boolean = true,
    val morningOn: Boolean = true,
    val morningAt: Int = 9 * 60,
    val eveningOn: Boolean = true,
    val eveningAt: Int = 20 * 60,
    val lastOn: Boolean = true,
    val lastAt: Int = 22 * 60 + 30,
)

data class IskraState(
    /** the date [doneIds] belong to (yyyy-MM-dd) */
    val day: String = "",
    val tasks: List<Task> = emptyList(),
    val doneIds: Set<Long> = emptySet(),
    /** days when every task was done */
    val history: Set<String> = emptySet(),
    /** days saved by an ember */
    val frozen: Set<String> = emptySet(),
    val freezes: Int = 0,
    val milestone: Int = 0,
    val best: Int = 0,
    val totalDone: Int = 0,
    val achievements: Set<String> = emptySet(),
    val settings: Settings = Settings(),
    /** the hatching scene has been shown */
    val hatchSeen: Boolean = false,
) {
    val effective: Set<String> get() = history + frozen
}

/** state: 0 nothing, 1 all done, 2 saved by an ember */
data class DayCell(val date: LocalDate, val state: Int)

data class Summary(
    val streak: Int,
    val best: Int,
    /** days with everything done — the pet's age */
    val lit: Int,
    val form: Int,
    val growth: Float,
    val warmth: Float,
    val mood: Mood,
    val done: Int,
    val total: Int,
    val complete: Boolean,
    val week: List<DayCell>,
    val freezes: Int,
    val totalDone: Int,
    val palette: Int,
    val petName: String,
) {
    val look: PetLook get() = PetLook(form, growth, mood, palette, warmth)
    val hatched: Boolean get() = form >= 1
    val left: Int get() = total - done
}

/** Egg → Огонёк → Пламя → Феникс, by days with everything done. */
object Growth {
    const val HATCH = 3
    val starts = intArrayOf(0, 3, 10, 21)
    private const val FULL = 40
    val names = listOf("Яйцо", "Огонёк", "Пламя", "Феникс")

    fun form(lit: Int): Int = starts.indexOfLast { lit >= it }.coerceAtLeast(0)

    fun growth(lit: Int): Float {
        val f = form(lit)
        val start = starts[f]
        val end = if (f < starts.lastIndex) starts[f + 1] else FULL
        return ((lit - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }

    /** Next form name and days left, or null when fully grown. */
    fun next(lit: Int): Pair<String, Int>? {
        val f = form(lit)
        if (f >= starts.lastIndex) return null
        return names[f + 1] to (starts[f + 1] - lit)
    }

    /** Progress 0..1 towards the next form (or to full size for the last one). */
    fun progress(lit: Int): Float = growth(lit)
}
