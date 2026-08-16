package com.iu.studytracker.scheduler

import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.Topic
import com.iu.studytracker.data.database.relation.ModuleWithTopics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [TopicScheduler].
 *
 * These tests validate the three stages of the algorithm:
 * interleaving, date range calculation, and distribution —
 * plus the end-to-end schedule generation.
 */
class TopicSchedulerTest {

    // ── Helpers ─────────────────────────────────────────────────

    private fun makeTopic(id: Long, moduleId: Long, title: String, order: Int) =
        Topic(id = id.toString(), moduleId = moduleId.toString(), title = title, orderIndex = order)

    private fun makeModule(id: Long, planId: Long, name: String, order: Int) =
        Module(id = id.toString(), monthPlanId = planId.toString(), name = name, orderIndex = order)

    private fun makeModuleWithTopics(
        moduleId: Long,
        planId: Long,
        name: String,
        order: Int,
        topicTitles: List<String>
    ): ModuleWithTopics {
        val module = makeModule(moduleId, planId, name, order)
        val topics = topicTitles.mapIndexed { i, title ->
            makeTopic(id = moduleId * 100 + i.toLong(), moduleId = moduleId, title = title, order = i)
        }
        return ModuleWithTopics(module = module, topics = topics)
    }

    // ── Interleave Tests ────────────────────────────────────────

    @Test
    fun `interleave with equal-sized modules produces alternating pattern`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, listOf("A1", "A2", "A3"))
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, listOf("B1", "B2", "B3"))

        val result = TopicScheduler.interleaveTopics(listOf(modA, modB))

        assertEquals(6, result.size)
        assertEquals(listOf("A1", "B1", "A2", "B2", "A3", "B3"), result.map { it.title })
    }

    @Test
    fun `interleave with unequal modules appends extras at end`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, listOf("A1", "A2", "A3", "A4"))
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, listOf("B1", "B2"))

        val result = TopicScheduler.interleaveTopics(listOf(modA, modB))

        assertEquals(6, result.size)
        assertEquals(listOf("A1", "B1", "A2", "B2", "A3", "A4"), result.map { it.title })
    }

    @Test
    fun `interleave with one empty module returns only the other`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, listOf("A1", "A2"))
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, emptyList())

        val result = TopicScheduler.interleaveTopics(listOf(modA, modB))

        assertEquals(2, result.size)
        assertEquals(listOf("A1", "A2"), result.map { it.title })
    }

    @Test
    fun `interleave with both modules empty returns empty`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, emptyList())
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, emptyList())

        val result = TopicScheduler.interleaveTopics(listOf(modA, modB))

        assertTrue(result.isEmpty())
    }

    // ── Date Range Tests ────────────────────────────────────────

    @Test
    fun `dates from start of month when startFrom is before the month`() {
        // Setting up September from August
        val dates = TopicScheduler.getAvailableDates(2026, 9, LocalDate.of(2026, 8, 15))

        assertEquals(30, dates.size) // September has 30 days
        assertEquals(LocalDate.of(2026, 9, 1), dates.first())
        assertEquals(LocalDate.of(2026, 9, 30), dates.last())
    }

    @Test
    fun `dates from today when startFrom is mid-month`() {
        val dates = TopicScheduler.getAvailableDates(2026, 8, LocalDate.of(2026, 8, 11))

        assertEquals(21, dates.size) // Aug 11-31 = 21 days
        assertEquals(LocalDate.of(2026, 8, 11), dates.first())
        assertEquals(LocalDate.of(2026, 8, 31), dates.last())
    }

    @Test
    fun `dates empty when month is in the past`() {
        val dates = TopicScheduler.getAvailableDates(2026, 7, LocalDate.of(2026, 8, 1))

        assertTrue(dates.isEmpty())
    }

    @Test
    fun `dates for February leap year`() {
        val dates = TopicScheduler.getAvailableDates(2028, 2, LocalDate.of(2028, 1, 15))

        assertEquals(29, dates.size) // 2028 is a leap year
        assertEquals(LocalDate.of(2028, 2, 1), dates.first())
        assertEquals(LocalDate.of(2028, 2, 29), dates.last())
    }

    @Test
    fun `single day when startFrom is last day of month`() {
        val dates = TopicScheduler.getAvailableDates(2026, 8, LocalDate.of(2026, 8, 31))

        assertEquals(1, dates.size)
        assertEquals(LocalDate.of(2026, 8, 31), dates.first())
    }

    // ── Distribution Tests ──────────────────────────────────────

    @Test
    fun `sparse schedule - fewer topics than days`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, listOf("A1", "A2", "A3"))
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, listOf("B1", "B2", "B3"))
        val interleaved = TopicScheduler.interleaveTopics(listOf(modA, modB))

        // 6 topics across 30 days
        val dates = (1..30).map { LocalDate.of(2026, 9, it) }
        val tasks = TopicScheduler.distributeTasks(interleaved, dates, "1")

        assertEquals(6, tasks.size)

        // Each topic should be on a different day
        val uniqueDates = tasks.map { it.scheduledDate }.toSet()
        assertEquals(6, uniqueDates.size)

        // Topics should be evenly spaced (every 5th day: 0, 5, 10, 15, 20, 25)
        val dayIndices = tasks.map { task ->
            dates.indexOfFirst { it.toString() == task.scheduledDate }
        }
        // Verify spacing is roughly 5 days apart
        for (i in 1 until dayIndices.size) {
            assertEquals(5, dayIndices[i] - dayIndices[i - 1])
        }
    }

    @Test
    fun `dense schedule - more topics than days`() {
        val topics = (1..10).map { makeTopic(it.toLong(), 1, "T$it", it - 1) }
        val dates = (1..3).map { LocalDate.of(2026, 9, it) }

        val tasks = TopicScheduler.distributeTasks(topics, dates, "1")

        assertEquals(10, tasks.size)

        // Distribution should be balanced: 4, 3, 3
        val grouped = tasks.groupBy { it.scheduledDate }
        val counts = grouped.values.map { it.size }.sortedDescending()
        assertEquals(listOf(4, 3, 3), counts)
    }

    @Test
    fun `exact fit - topics equal days`() {
        val topics = (1..5).map { makeTopic(it.toLong(), 1, "T$it", it - 1) }
        val dates = (1..5).map { LocalDate.of(2026, 9, it) }

        val tasks = TopicScheduler.distributeTasks(topics, dates, "1")

        assertEquals(5, tasks.size)

        // Each day should have exactly 1 topic
        val grouped = tasks.groupBy { it.scheduledDate }
        assertEquals(5, grouped.size)
        grouped.values.forEach { assertEquals(1, it.size) }
    }

    @Test
    fun `single topic single day`() {
        val topics = listOf(makeTopic(1, 1, "Only Topic", 0))
        val dates = listOf(LocalDate.of(2026, 9, 1))

        val tasks = TopicScheduler.distributeTasks(topics, dates, "1")

        assertEquals(1, tasks.size)
        assertEquals("2026-09-01", tasks[0].scheduledDate)
    }

    // ── End-to-End Schedule Generation ───────────────────────────

    @Test
    fun `full schedule generation for a typical month`() {
        val modA = makeModuleWithTopics(
            1, 1, "Data Structures", 0,
            listOf("Arrays", "Linked Lists", "Stacks", "Queues", "Trees")
        )
        val modB = makeModuleWithTopics(
            2, 1, "Algorithms", 1,
            listOf("Sorting", "Searching", "Graph BFS", "Graph DFS", "Dynamic Programming")
        )

        // Schedule for September 2026, starting day 1
        val result = TopicScheduler.generateSchedule(
            monthPlanId = "1",
            modulesWithTopics = listOf(modA, modB),
            year = 2026,
            month = 9,
            startFrom = LocalDate.of(2026, 8, 1) // before the month → uses Sept 1
        )

        // 10 topics across 30 days
        assertEquals(10, result.tasks.size)
        assertEquals(30, result.availableDates.size)

        // All tasks should have monthPlanId = 1
        assertTrue(result.tasks.all { it.monthPlanId == "1" })

        // All dates should be in September 2026
        assertTrue(result.tasks.all { it.scheduledDate?.startsWith("2026-09") == true })

        // Verify summary
        val summary = result.summary()
        assertEquals(10, summary.totalTopics)
        assertEquals(30, summary.totalDays)
        assertEquals(1, summary.maxTopicsPerDay)
        assertEquals(20, summary.restDays) // 30 - 10 = 20 rest days
    }

    @Test
    fun `mid-month setup uses fewer days`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, listOf("A1", "A2"))
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, listOf("B1", "B2"))

        // Setup on August 20 → only 12 days left (Aug 20-31)
        val result = TopicScheduler.generateSchedule(
            monthPlanId = "1",
            modulesWithTopics = listOf(modA, modB),
            year = 2026,
            month = 8,
            startFrom = LocalDate.of(2026, 8, 20)
        )

        assertEquals(4, result.tasks.size)
        assertEquals(12, result.availableDates.size)

        // First task should be on Aug 20 or later
        assertTrue((result.tasks.first().scheduledDate ?: "") >= "2026-08-20")
    }

    @Test
    fun `empty modules returns empty schedule`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, emptyList())
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, emptyList())

        val result = TopicScheduler.generateSchedule(
            monthPlanId = "1",
            modulesWithTopics = listOf(modA, modB),
            year = 2026,
            month = 9,
            startFrom = LocalDate.of(2026, 9, 1)
        )

        assertTrue(result.isEmpty)
    }

    // ── Summary Tests ───────────────────────────────────────────

    @Test
    fun `summary computes correct stats`() {
        val modA = makeModuleWithTopics(1, 1, "Mod A", 0, listOf("A1", "A2", "A3"))
        val modB = makeModuleWithTopics(2, 1, "Mod B", 1, listOf("B1", "B2", "B3"))

        val result = TopicScheduler.generateSchedule(
            monthPlanId = "1",
            modulesWithTopics = listOf(modA, modB),
            year = 2026,
            month = 9,
            startFrom = LocalDate.of(2026, 9, 1)
        )

        val summary = result.summary()

        assertEquals(6, summary.totalTopics)
        assertEquals(30, summary.totalDays)
        assertEquals(1, summary.maxTopicsPerDay)
        assertEquals(24, summary.restDays)
        assertEquals("2026-09-01", summary.startDate)
        assertEquals("2026-09-30", summary.endDate)
    }
}
