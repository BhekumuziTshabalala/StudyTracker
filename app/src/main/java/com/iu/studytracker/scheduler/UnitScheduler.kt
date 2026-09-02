package com.iu.studytracker.scheduler

import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.Task
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object UnitScheduler {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun generateSchedule(
        monthPlanId: String,
        modules: List<Module>,
        year: Int,
        month: Int,
        startFrom: LocalDate = LocalDate.now()
    ): ScheduleResult {
        val availableDates = getAvailableDates(year, month, startFrom)
        if (modules.isEmpty() || availableDates.isEmpty()) return ScheduleResult(emptyList(), availableDates)

        val tasks = mutableListOf<Task>()
        val maxUnits = modules.maxOfOrNull { it.totalUnits } ?: 0
        val interleavedUnits = mutableListOf<Pair<Module, Int>>()

        // Interleave units
        for (unitNum in 1..maxUnits) {
            for (module in modules) {
                if (unitNum <= module.totalUnits) {
                    interleavedUnits.add(module to unitNum)
                }
            }
        }

        if (interleavedUnits.isEmpty()) return ScheduleResult(emptyList(), availableDates)

        // Distribute interleaved units across available dates
        if (interleavedUnits.size <= availableDates.size) {
            for (i in interleavedUnits.indices) {
                val dayIndex = (i * availableDates.size) / interleavedUnits.size
                val (module, unit) = interleavedUnits[i]
                tasks.add(createTask(monthPlanId, module, unit, availableDates[dayIndex]))
            }
        } else {
            val baseCount = interleavedUnits.size / availableDates.size
            val extraDays = interleavedUnits.size % availableDates.size
            var unitIndex = 0

            for (dayIndex in availableDates.indices) {
                val tasksToday = if (dayIndex < extraDays) baseCount + 1 else baseCount
                repeat(tasksToday) {
                    if (unitIndex < interleavedUnits.size) {
                        val (module, unit) = interleavedUnits[unitIndex]
                        tasks.add(createTask(monthPlanId, module, unit, availableDates[dayIndex]))
                        unitIndex++
                    }
                }
            }
        }
        return ScheduleResult(tasks, availableDates)
    }

    fun rebalanceSchedule(
        incompleteTasks: List<Task>,
        year: Int,
        month: Int,
        today: LocalDate = LocalDate.now()
    ): List<Task> {
        val availableDates = getAvailableDates(year, month, today)
        if (availableDates.isEmpty() || incompleteTasks.isEmpty()) return incompleteTasks

        val updated = mutableListOf<Task>()
        val tasksToDistribute = incompleteTasks.sortedBy { it.scheduledDate }
        
        if (tasksToDistribute.size <= availableDates.size) {
            for (i in tasksToDistribute.indices) {
                val dayIndex = (i * availableDates.size) / tasksToDistribute.size
                updated.add(tasksToDistribute[i].copy(scheduledDate = availableDates[dayIndex].format(dateFormatter)))
            }
        } else {
            val baseCount = tasksToDistribute.size / availableDates.size
            val extraDays = tasksToDistribute.size % availableDates.size
            var taskIdx = 0
            
            for (dayIndex in availableDates.indices) {
                val tasksToday = if (dayIndex < extraDays) baseCount + 1 else baseCount
                repeat(tasksToday) {
                    if (taskIdx < tasksToDistribute.size) {
                        updated.add(tasksToDistribute[taskIdx].copy(scheduledDate = availableDates[dayIndex].format(dateFormatter)))
                        taskIdx++
                    }
                }
            }
        }
        return updated
    }

    private fun createTask(planId: String, module: Module, unit: Int, date: LocalDate): Task {
        return Task(
            monthPlanId = planId,
            moduleId = module.id,
            unitNumber = unit,
            title = "${module.name} - Unit $unit",
            scheduledDate = date.format(dateFormatter)
        )
    }

    internal fun getAvailableDates(year: Int, month: Int, startFrom: LocalDate): List<LocalDate> {
        val yearMonth = YearMonth.of(year, month)
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()
        val effectiveStart = when {
            !startFrom.isBefore(firstDay) && !startFrom.isAfter(lastDay) -> startFrom
            startFrom.isAfter(lastDay) -> return emptyList()
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

    data class ScheduleSummary(
        val totalTopics: Int,
        val totalDays: Int,
        val avgTopicsPerStudyDay: Float,
        val maxTopicsPerDay: Int,
        val restDays: Int,
        val startDate: String,
        val endDate: String
    )

    data class ScheduleResult(
        val tasks: List<Task>,
        val availableDates: List<LocalDate>
    ) {
        val isEmpty: Boolean get() = tasks.isEmpty()

        fun summary(): ScheduleSummary {
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
    }
}
