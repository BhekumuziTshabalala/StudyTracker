package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single topic or chapter within a [Module].
 *
 * Topics are ordered by [orderIndex] to preserve the sequence the user
 * entered them in. The scheduling algorithm later assigns each topic
 * to a specific calendar day.
 */
@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Module::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moduleId"])]
)
data class Topic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK → modules.id */
    val moduleId: Long,

    /** Topic title, e.g. "Binary Search Trees" */
    val title: String,

    /** Ordering within the module (0-based) */
    val orderIndex: Int,

    /** Optional URI to a local resource (PDF) or web URL */
    val resourceUri: String? = null,

    /** Specific pages or sections to cover (e.g. "Pages 20-45") */
    val pageRange: String? = null
)
