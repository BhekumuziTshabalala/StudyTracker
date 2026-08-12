package com.iu.studytracker.scheduler

import com.iu.studytracker.data.database.entity.DailyTask
import com.iu.studytracker.data.database.entity.Topic
import com.iu.studytracker.data.database.relation.ModuleWithTopics
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Generates a day-by-day study schedule by distributing topics
 * from two modules evenly across the available days of the month.
 *
 * The algorithm works in three stages:
 * 1. **Interleave** — round-robin merge of topics from both modules
 *    so the user alternates between subjects for variety.
 * 2. **Date range** — determine which calendar days are available
 *    (from today or day 1, whichever applies, through month end).
 * 3. **Distribute** — map topics onto dates, handling both sparse
 *    schedules (rest days) and dense schedules (multiple topics/day).
 */
object TopicScheduler {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Public API ──────────────────────────────────────────────

    /**
     * Generates a list of [DailyTask] records representing the study schedule.
     *
     * @param monthPlanId  The ID of the month plan these tasks belong to.
     * @param modulesWithTopics  The two modules with their topics.
     * @param year  Calendar year (e.g. 2026).
     * @param month  Calendar month 1–12.
     * @param startFrom  First eligible scheduling date. Defaults to today.
     *                   If this date is before the month, day 1 is used.
     *                   If after the month, an empty list is returned.
     * @return List of [DailyTask] records ready for database insertion.
     */
    fun generateSchedule(
        monthPlanId: Long,
        modulesWithTopics: List<ModuleWithTopics>,
        year: Int,
        month: Int,
        startFrom: LocalDate = LocalDate.now()
    ): ScheduleResult {
        // 1. Interleave topics from both modules for study variety
        val interleaved = interleaveTopics(modulesWithTopics)
        if (interleaved.isEmpty()) {
            return ScheduleResult(emptyList(), emptyList())
        }

        // 2. Determine available dates within the month
        val availableDates = getAvailableDates(year, month, startFrom)
        if (availableDates.isEmpty()) {
            return ScheduleResult(emptyList(), emptyList())
        }

        // 3. Distribute topics across dates
        val tasks = distributeTasks(interleaved, availableDates, monthPlanId)
        return ScheduleResult(tasks, availableDates)
    }

    /**
     * Re-distributes past, incomplete tasks across the remaining days of the month.
     */
    fun rebalanceSchedule(
        incompleteTasks: List<DailyTask>,
        year: Int,
        month: Int,
        today: LocalDate = LocalDate.now()
    ): List<DailyTask> {
        if (incompleteTasks.isEmpty()) return emptyList()

        val availableDates = getAvailableDates(year, month, today)
        if (availableDates.isEmpty()) return emptyList()

        val updatedTasks = mutableListOf<DailyTask>()
        val baseCount = incompleteTasks.size / availableDates.size
        val extraDays = incompleteTasks.size % availableDates.size
        var taskIndex = 0

        for (dayIndex in availableDates.indices) {
            val tasksToday = if (dayIndex < extraDays) baseCount + 1 else baseCount
            repeat(tasksToday) {
                if (taskIndex < incompleteTasks.size) {
                    val oldTask = incompleteTasks[taskIndex]
                    updatedTasks.add(
                        oldTask.copy(scheduledDate = availableDates[dayIndex].format(dateFormatter))
                    )
                    taskIndex++
                }
            }
        }
        return updatedTasks
    }

    // ── Stage 1: Interleave ─────────────────────────────────────

    /**
     * Interleaves topics from multiple modules in round-robin order.
     *
     * Given modules A[a1, a2, a3] and B[b1, b2], produces:
     * [a1, b1, a2, b2, a3]
     *
     * This ensures the user alternates between modules each day
     * rather than front-loading one module. If one module has more
     * topics, its extras naturally trail at the end.
     */
    internal fun interleaveTopics(modulesWithTopics: List<ModuleWithTopics>): List<Topic> {
        val sortedModules = modulesWithTopics
            .sortedBy { it.module.orderIndex }
            .map { mwt -> mwt.topics.sortedBy { it.orderIndex } }

        val result = mutableListOf<Topic>()
        val maxSize = sortedModules.maxOfOrNull { it.size } ?: 0

        for (i in 0 until maxSize) {
            for (moduleTopics in sortedModules) {
                if (i < moduleTopics.size) {
                    result.add(moduleTopics[i])
                }
            }
        }

        return result
    }

    // ── Stage 2: Date Range ─────────────────────────────────────

    /**
     * Returns the list of calendar dates available for scheduling.
     *
     * The range is [effectiveStart, lastDayOfMonth]. The effective
     * start is the later of [startFrom] and the first day of the
     * target month. If the entire month is in the past relative
     * to [startFrom], an empty list is returned.
     */
    internal fun getAvailableDates(
        year: Int,
        month: Int,
        startFrom: LocalDate
    ): List<LocalDate> {
        val yearMonth = YearMonth.of(year, month)
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        val effectiveStart = when {
            // startFrom is within the month — use it
            !startFrom.isBefore(firstDay) && !startFrom.isAfter(lastDay) -> startFrom
            // startFrom is after the month — month is past, nothing to schedule
            startFrom.isAfter(lastDay) -> return emptyList()
            // startFrom is before the month — use first day
            else -> firstDay
        }

        val dates = mutableListOf<LocalDate>()
        var current = effectiveStart
        while (!current.isAfter(lastDay)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        return dates
    }

    // ── Stage 3: Distribute ─────────────────────────────────────

    /**
     * Distributes topics evenly across available dates.
     *
     * **Sparse schedule** (topics ≤ dates):
     * Topics are spaced out evenly across the date range. The
     * formula `dayIndex = (i × dateCount) / topicCount` ensures
     * uniform spacing. Days without topics become implicit rest days.
     *
     * Example: 6 topics across 30 days → topics land on days
     * 0, 5, 10, 15, 20, 25 (every 5th day).
     *
     * **Dense schedule** (topics > dates):
     * Each day gets `floor(topics/dates)` topics, with the first
     * `topics % dates` days receiving one extra. This guarantees
     * no day has more than one extra topic compared to any other.
     *
     * Example: 10 topics across 3 days → days get 4, 3, 3 topics.
     */
    internal fun distributeTasks(
        topics: List<Topic>,
        dates: List<LocalDate>,
        monthPlanId: Long
    ): List<DailyTask> {
        val tasks = mutableListOf<DailyTask>()

        if (topics.size <= dates.size) {
            // ── Sparse: spread topics evenly, leaving rest days ──
            for (i in topics.indices) {
                val dayIndex = (i * dates.size) / topics.size
                tasks.add(
                    DailyTask(
                        monthPlanId = monthPlanId,
                        topicId = topics[i].id,
                        scheduledDate = dates[dayIndex].format(dateFormatter)
                    )
                )
            }
        } else {
            // ── Dense: stack multiple topics per day, balanced ──
            val baseCount = topics.size / dates.size
            val extraDays = topics.size % dates.size
            var topicIndex = 0

            for (dayIndex in dates.indices) {
                val topicsToday = if (dayIndex < extraDays) baseCount + 1 else baseCount
                repeat(topicsToday) {
                    if (topicIndex < topics.size) {
                        tasks.add(
                            DailyTask(
                                monthPlanId = monthPlanId,
                                topicId = topics[topicIndex].id,
                                scheduledDate = dates[dayIndex].format(dateFormatter)
                            )
                        )
                        topicIndex++
                    }
                }
            }
        }

        return tasks
    }

    // ── Schedule Summary ────────────────────────────────────────

    /**
     * Human-readable statistics about a generated schedule.
     */
    data class ScheduleSummary(
        /** Total number of topics to study */
        val totalTopics: Int,
        /** Total calendar days in the scheduling window */
        val totalDays: Int,
        /** Average topics assigned per study day (excludes rest days) */
        val avgTopicsPerStudyDay: Float,
        /** Maximum topics assigned to any single day */
        val maxTopicsPerDay: Int,
        /** Number of days with no topics (rest days) */
        val restDays: Int,
        /** First scheduled date ("yyyy-MM-dd") */
        val startDate: String,
        /** Last scheduled date ("yyyy-MM-dd") */
        val endDate: String
    )

    /**
     * Computes summary statistics for a generated schedule.
     */
    fun summarize(tasks: List<DailyTask>, availableDates: List<LocalDate>): ScheduleSummary {
        if (tasks.isEmpty() || availableDates.isEmpty()) {
            return ScheduleSummary(0, 0, 0f, 0, 0, "", "")
        }

        val grouped = tasks.groupBy { it.scheduledDate }
        val studyDays = grouped.keys.size
        val maxPerDay = grouped.values.maxOf { it.size }
        val restDays = availableDates.size - studyDays

        return ScheduleSummary(
            totalTopics = tasks.size,
            totalDays = availableDates.size,
            avgTopicsPerStudyDay = if (studyDays > 0) tasks.size.toFloat() / studyDays else 0f,
            maxTopicsPerDay = maxPerDay,
            restDays = restDays,
            startDate = availableDates.first().format(dateFormatter),
            endDate = availableDates.last().format(dateFormatter)
        )
    }

    // ── Result wrapper ──────────────────────────────────────────

    /**
     * Wraps the generated tasks together with the date range
     * used, so callers can compute summaries without re-deriving dates.
     */
    data class ScheduleResult(
        val tasks: List<DailyTask>,
        val availableDates: List<LocalDate>
    ) {
        val isEmpty: Boolean get() = tasks.isEmpty()

        fun summary(): ScheduleSummary = summarize(tasks, availableDates)
    }
}
