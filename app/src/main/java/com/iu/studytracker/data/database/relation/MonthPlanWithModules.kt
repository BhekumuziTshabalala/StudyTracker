package com.iu.studytracker.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.MonthPlan

/**
 * One-to-many relationship: a [MonthPlan] with its two [Module] records.
 */
data class MonthPlanWithModules(
    @Embedded
    val monthPlan: MonthPlan,

    @Relation(
        parentColumn = "id",
        entityColumn = "monthPlanId"
    )
    val modules: List<Module>
)
