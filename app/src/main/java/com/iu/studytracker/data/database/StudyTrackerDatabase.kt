package com.iu.studytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iu.studytracker.data.database.dao.DailyTaskDao
import com.iu.studytracker.data.database.dao.ModuleDao
import com.iu.studytracker.data.database.dao.MonthPlanDao
import com.iu.studytracker.data.database.dao.TopicDao
import com.iu.studytracker.data.database.entity.DailyTask
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.MonthPlan
import com.iu.studytracker.data.database.entity.Topic

@Database(
    entities = [
        MonthPlan::class,
        Module::class,
        Topic::class,
        DailyTask::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyTrackerDatabase : RoomDatabase() {

    abstract fun monthPlanDao(): MonthPlanDao
    abstract fun moduleDao(): ModuleDao
    abstract fun topicDao(): TopicDao
    abstract fun dailyTaskDao(): DailyTaskDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
