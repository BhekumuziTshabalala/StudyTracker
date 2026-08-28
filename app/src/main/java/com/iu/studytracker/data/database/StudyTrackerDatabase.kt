package com.iu.studytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iu.studytracker.data.database.dao.TaskDao
import com.iu.studytracker.data.database.dao.MonthPlanDao
import com.iu.studytracker.data.database.dao.ModuleDao
import com.iu.studytracker.data.database.dao.TopicDao
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.MonthPlan
import com.iu.studytracker.data.database.entity.Task
import com.iu.studytracker.data.database.entity.Topic
import com.iu.studytracker.data.database.entity.DegreePlan
import com.iu.studytracker.data.database.dao.DegreePlanDao
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.CurriculumTopic
import com.iu.studytracker.data.database.dao.CurriculumDao
import com.iu.studytracker.data.database.dao.ModuleDetailsDao
import com.iu.studytracker.data.database.entity.ModuleTask
import com.iu.studytracker.data.database.entity.ModuleScheduleEvent
import com.iu.studytracker.data.database.dao.TaskTemplateDao
import com.iu.studytracker.data.database.entity.TaskTemplate

@Database(
    entities = [
        MonthPlan::class,
        Module::class,
        Topic::class,
        Task::class,
        DegreePlan::class,
        CurriculumModule::class,
        CurriculumTopic::class,
        ModuleTask::class,
        ModuleScheduleEvent::class,
        TaskTemplate::class
    ],
    version = 8,
    exportSchema = false
)
abstract class StudyTrackerDatabase : RoomDatabase() {

    abstract fun monthPlanDao(): MonthPlanDao
    abstract fun moduleDao(): ModuleDao
    abstract fun topicDao(): TopicDao
    abstract fun taskDao(): TaskDao
    abstract fun degreePlanDao(): DegreePlanDao
    abstract fun curriculumDao(): CurriculumDao
    abstract fun moduleDetailsDao(): ModuleDetailsDao
    abstract fun taskTemplateDao(): TaskTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: StudyTrackerDatabase? = null

        fun getInstance(context: Context): StudyTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyTrackerDatabase::class.java,
                    "study_tracker.db"
                )
                    .addMigrations(MIGRATION_1_4, MIGRATION_4_8)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
