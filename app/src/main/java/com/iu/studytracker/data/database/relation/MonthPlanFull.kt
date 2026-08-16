package com.iu.studytracker.data.database.relation

import com.iu.studytracker.data.database.entity.Task

/**
 * Full snapshot of a month's study plan.
 *
 * This is NOT a Room relation class — it is assembled in the Repository
 * layer by combining multiple DAO queries. This gives us full control
 * over loading order and avoids deeply nested Room relations.
 */
data class MonthPlanFull(
    val monthPlanWithModules: MonthPlanWithModules,
    val modulesWithTopics: List<ModuleWithTopics>,
    val tasks: List<Task>
)
