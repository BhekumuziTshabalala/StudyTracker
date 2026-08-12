package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One of the two modules studied in a given month.
 *
 * Each module belongs to a [MonthPlan] and is identified within that
 * plan by its [orderIndex] (0 = first module, 1 = second module).
 * The foreign key cascades deletes so removing a MonthPlan also
 * removes its modules.
 */
@Entity(
    tableName = "modules",
    foreignKeys = [
        ForeignKey(
            entity = MonthPlan::class,
            parentColumns = ["id"],
            childColumns = ["monthPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["monthPlanId"])]
)
data class Module(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK → month_plans.id */
    val monthPlanId: Long,

    /** Display name, e.g. "Data Structures & Algorithms" */
    val name: String,

    /** 0 = first module, 1 = second module within the month */
    val orderIndex: Int
)
