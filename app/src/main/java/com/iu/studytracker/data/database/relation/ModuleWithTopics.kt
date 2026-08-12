package com.iu.studytracker.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.Topic

/**
 * One-to-many relationship: a [Module] with all its [Topic] records.
 */
data class ModuleWithTopics(
    @Embedded
    val module: Module,

    @Relation(
        parentColumn = "id",
        entityColumn = "moduleId"
    )
    val topics: List<Topic>
)
