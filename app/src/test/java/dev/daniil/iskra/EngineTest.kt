package dev.daniil.iskra

import dev.daniil.iskra.data.Engine
import dev.daniil.iskra.data.Growth
import dev.daniil.iskra.data.IskraState
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.pet.Mood
import dev.daniil.iskra.scene.DayPhase
import dev.daniil.iskra.scene.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EngineTest {
    private val d0 = LocalDate.of(2026, 10, 1)
    private fun at(day: Int, hour: Int = 12): LocalDateTime = d0.plusDays(day.toLong()).atTime(hour, 0)

    private fun base(): IskraState {
        var s = IskraState()
        s = Engine.rollover(s, d0).first
        s = Engine.add(s, Task(1, "Вода", "💧"), d0)
        s = Engine.add(s, Task(2, "Книга", "📖"), d0)
        return s
    }

    /** Completes every task on day [day] (after rolling over to it). */
    private fun completeDay(s0: IskraState, day: Int): IskraState {
        val now = at(day)
        var s = Engine.rollover(s0, now.toLocalDate()).first
        for (t in s.tasks) if (t.id !in s.doneIds) s = Engine.toggle(s, t.id, now)
        return s
    }

    @Test
    fun eggHatchesAfterThreeDays() {
        var s = base()
        assertEquals(0, Engine.summary(s, at(0)).form)
        s = completeDay(s, 0)
        assertEquals(0, Engine.summary(s, at(0)).form)
        assertEquals(1f, Engine.summary(s, at(0)).warmth, 0.001f)
        s = completeDay(s, 1)
        s = Engine.rollover(s, at(2).toLocalDate()).first
        s = Engine.toggle(s, 1, at(2))
        val half = Engine.summary(s, at(2))
        assertEquals(0, half.form)
        assertEquals(2.5f, half.warmth, 0.001f)
        s = Engine.toggle(s, 2, at(2))
        val sm = Engine.summary(s, at(2))
        assertEquals(1, sm.form)
        assertEquals(0f, sm.growth, 0.001f)
        assertEquals(3, sm.streak)
        assertTrue("hatch" in s.achievements)
    }

    @Test
    fun growsGraduallyAndEvolves() {
        assertEquals(0, Growth.form(2))
        assertEquals(1, Growth.form(3))
        assertEquals(1, Growth.form(9))
        assertEquals(2, Growth.form(10))
        assertEquals(3, Growth.form(21))
        assertTrue(Growth.growth(5) > Growth.growth(4))
        assertEquals(Pair("Пламя", 6), Growth.next(4))
    }

    @Test
    fun missedDayBreaksStreakButKeepsAge() {
        var s = base()
        for (d in 0..4) s = completeDay(s, d)
        s = Engine.rollover(s, at(6).toLocalDate()).first
        val sm = Engine.summary(s, at(6))
        assertEquals(0, sm.streak)
        assertEquals(5, sm.lit)
        assertEquals(1, sm.form)
        assertEquals(Mood.SMOLDER, sm.mood)
    }

    @Test
    fun emberSavesStreak() {
        var s = base()
        for (d in 0..6) s = completeDay(s, d)
        s = Engine.rollover(s, at(7).toLocalDate()).first
        assertEquals(1, s.freezes)
        // day 7 is missed, the ember covers it on day 8
        val (after, notes) = Engine.rollover(s, at(8).toLocalDate())
        assertEquals(0, after.freezes)
        assertTrue(notes.isNotEmpty())
        assertEquals(8, Engine.summary(after, at(8)).streak)
    }

    @Test
    fun newDayClearsTicks() {
        var s = base()
        s = Engine.toggle(s, 1, at(0))
        assertTrue(1L in s.doneIds)
        s = Engine.rollover(s, at(1).toLocalDate()).first
        assertFalse(1L in s.doneIds)
    }

    @Test
    fun hungryInTheEvening() {
        val s = base()
        assertEquals(Mood.HUNGRY, Engine.summary(s, at(0, 19)).mood)
        assertEquals(Mood.NORMAL, Engine.summary(s, at(0, 10)).mood)
    }

    @Test
    fun phases() {
        assertEquals(DayPhase.NIGHT, Sky.phaseAt(2 * 60))
        assertEquals(DayPhase.DAWN, Sky.phaseAt(6 * 60))
        assertEquals(DayPhase.MORNING, Sky.phaseAt(9 * 60))
        assertEquals(DayPhase.DAY, Sky.phaseAt(13 * 60))
        assertEquals(DayPhase.SUNSET, Sky.phaseAt(18 * 60))
        assertEquals(DayPhase.DUSK, Sky.phaseAt(20 * 60 + 30))
        assertEquals(DayPhase.NIGHT, Sky.phaseAt(23 * 60))
        assertTrue(Sky.at(13 * 60).sunT in 0f..1f)
        assertTrue(Sky.at(2 * 60).moonT in 0f..1f)
        assertEquals(-1f, Sky.at(2 * 60).sunT, 0f)
    }

    @Test
    fun moveTask() {
        val s = Engine.move(base(), 2, -1)
        assertEquals(listOf(2L, 1L), s.tasks.map { it.id })
    }
}
