package com.iu.studytracker.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_4 = object : Migration(1, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `degree_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `targetGraduation` TEXT NOT NULL, `totalCreditsRequired` INTEGER NOT NULL, `completedCredits` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `curriculum_modules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester` INTEGER NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `assessment` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `curriculum_topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `curriculumModuleId` INTEGER NOT NULL, `title` TEXT NOT NULL, FOREIGN KEY(`curriculumModuleId`) REFERENCES `curriculum_modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_topics_curriculumModuleId` ON `curriculum_topics` (`curriculumModuleId`)")
        db.execSQL("ALTER TABLE `month_plans` ADD COLUMN `degreePlanId` INTEGER")
    }
}

val MIGRATION_4_8 = object : Migration(4, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop daily_tasks
        db.execSQL("DROP TABLE IF EXISTS `daily_tasks`")

        // 1. month_plans
        db.execSQL("CREATE TABLE IF NOT EXISTS `month_plans_new` (`id` TEXT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `isSetupComplete` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `degreePlanId` TEXT, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO `month_plans_new` (`id`, `year`, `month`, `isSetupComplete`, `createdAt`, `degreePlanId`, `updatedAt`) SELECT CAST(`id` AS TEXT), `year`, `month`, `isSetupComplete`, `createdAt`, CAST(`degreePlanId` AS TEXT), `createdAt` FROM `month_plans`")
        db.execSQL("DROP TABLE `month_plans`")
        db.execSQL("ALTER TABLE `month_plans_new` RENAME TO `month_plans`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_month_plans_year_month` ON `month_plans` (`year`, `month`)")

        // 2. modules
        db.execSQL("CREATE TABLE IF NOT EXISTS `modules_new` (`id` TEXT NOT NULL, `monthPlanId` TEXT NOT NULL, `name` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`), FOREIGN KEY(`monthPlanId`) REFERENCES `month_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO `modules_new` (`id`, `monthPlanId`, `name`, `orderIndex`, `updatedAt`) SELECT CAST(`id` AS TEXT), CAST(`monthPlanId` AS TEXT), `name`, `orderIndex`, 0 FROM `modules`")
        db.execSQL("DROP TABLE `modules`")
        db.execSQL("ALTER TABLE `modules_new` RENAME TO `modules`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_modules_monthPlanId` ON `modules` (`monthPlanId`)")

        // 3. topics
        db.execSQL("CREATE TABLE IF NOT EXISTS `topics_new` (`id` TEXT NOT NULL, `moduleId` TEXT NOT NULL, `title` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `resourceUri` TEXT, `pageRange` TEXT, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`), FOREIGN KEY(`moduleId`) REFERENCES `modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO `topics_new` (`id`, `moduleId`, `title`, `orderIndex`, `resourceUri`, `pageRange`, `updatedAt`) SELECT CAST(`id` AS TEXT), CAST(`moduleId` AS TEXT), `title`, `orderIndex`, `resourceUri`, `pageRange`, 0 FROM `topics`")
        db.execSQL("DROP TABLE `topics`")
        db.execSQL("ALTER TABLE `topics_new` RENAME TO `topics`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_topics_moduleId` ON `topics` (`moduleId`)")

        // 4. degree_plans
        db.execSQL("CREATE TABLE IF NOT EXISTS `degree_plans_new` (`id` TEXT NOT NULL, `targetGraduation` TEXT NOT NULL, `totalCreditsRequired` INTEGER NOT NULL, `completedCredits` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO `degree_plans_new` (`id`, `targetGraduation`, `totalCreditsRequired`, `completedCredits`, `updatedAt`) SELECT CAST(`id` AS TEXT), `targetGraduation`, `totalCreditsRequired`, `completedCredits`, 0 FROM `degree_plans`")
        db.execSQL("DROP TABLE `degree_plans`")
        db.execSQL("ALTER TABLE `degree_plans_new` RENAME TO `degree_plans`")

        // 5. curriculum_modules
        db.execSQL("CREATE TABLE IF NOT EXISTS `curriculum_modules_new` (`id` TEXT NOT NULL, `semester` INTEGER NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `assessment` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO `curriculum_modules_new` (`id`, `semester`, `code`, `name`, `assessment`, `isCompleted`, `updatedAt`) SELECT CAST(`id` AS TEXT), `semester`, `code`, `name`, `assessment`, `isCompleted`, 0 FROM `curriculum_modules`")
        db.execSQL("DROP TABLE `curriculum_modules`")
        db.execSQL("ALTER TABLE `curriculum_modules_new` RENAME TO `curriculum_modules`")

        // 6. curriculum_topics
        db.execSQL("CREATE TABLE IF NOT EXISTS `curriculum_topics_new` (`id` TEXT NOT NULL, `curriculumModuleId` TEXT NOT NULL, `title` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`), FOREIGN KEY(`curriculumModuleId`) REFERENCES `curriculum_modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT INTO `curriculum_topics_new` (`id`, `curriculumModuleId`, `title`, `updatedAt`) SELECT CAST(`id` AS TEXT), CAST(`curriculumModuleId` AS TEXT), `title`, 0 FROM `curriculum_topics`")
        db.execSQL("DROP TABLE `curriculum_topics`")
        db.execSQL("ALTER TABLE `curriculum_topics_new` RENAME TO `curriculum_topics`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_topics_curriculumModuleId` ON `curriculum_topics` (`curriculumModuleId`)")

        // Create new tables
        db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `parentTaskId` TEXT, `monthPlanId` TEXT, `topicId` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `status` TEXT NOT NULL, `priority` TEXT NOT NULL, `scheduledDate` TEXT, `startDate` INTEGER, `endDate` INTEGER, `completedAt` INTEGER, `recurrenceRule` TEXT, `estimatedMinutes` INTEGER NOT NULL, `actualMinutesSpent` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`monthPlanId`) REFERENCES `month_plans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`topicId`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`parentTaskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_monthPlanId` ON `tasks` (`monthPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_topicId` ON `tasks` (`topicId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentTaskId` ON `tasks` (`parentTaskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_scheduledDate` ON `tasks` (`scheduledDate`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `module_tasks` (`id` TEXT NOT NULL, `curriculumModuleId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `type` TEXT NOT NULL, `dueDate` INTEGER, `isCompleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`curriculumModuleId`) REFERENCES `curriculum_modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_module_tasks_curriculumModuleId` ON `module_tasks` (`curriculumModuleId`)")
        
        db.execSQL("CREATE TABLE IF NOT EXISTS `module_schedule_events` (`id` TEXT NOT NULL, `curriculumModuleId` TEXT NOT NULL, `title` TEXT NOT NULL, `eventType` TEXT NOT NULL, `date` INTEGER NOT NULL, `durationMinutes` INTEGER, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`curriculumModuleId`) REFERENCES `curriculum_modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_module_schedule_events_curriculumModuleId` ON `module_schedule_events` (`curriculumModuleId`)")
        
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_templates` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `defaultPriority` TEXT NOT NULL, `defaultModuleId` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `curriculum_modules` ADD COLUMN `examPassed` INTEGER")
        db.execSQL("ALTER TABLE `curriculum_modules` ADD COLUMN `finalGrade` TEXT")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `curriculum_topics` ADD COLUMN `scheduledDay` INTEGER DEFAULT NULL")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `curriculum_topics` ADD COLUMN `scheduledTime` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `curriculum_topics` ADD COLUMN `timeSlotCategory` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `curriculum_topics` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `curriculum_modules` ADD COLUMN `syllabus` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `curriculum_modules` ADD COLUMN `totalUnits` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `study_sessions` (
                `id` TEXT NOT NULL,
                `curriculumModuleId` TEXT NOT NULL,
                `unitNumber` INTEGER NOT NULL,
                `scheduledDay` INTEGER,
                `scheduledTime` TEXT,
                `timeSlotCategory` TEXT,
                `isCompleted` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`curriculumModuleId`) REFERENCES `curriculum_modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_curriculumModuleId` ON `study_sessions` (`curriculumModuleId`)")

        val cursor = db.query("SELECT id, curriculumModuleId, scheduledDay, scheduledTime, timeSlotCategory, isCompleted, updatedAt FROM curriculum_topics ORDER BY curriculumModuleId, id")
        var currentModuleId = ""
        var unitCounter = 1
        
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val moduleId = cursor.getString(1)
            
            if (moduleId != currentModuleId) {
                currentModuleId = moduleId
                unitCounter = 1
            }
            
            val day = if (cursor.isNull(2)) "NULL" else cursor.getInt(2).toString()
            val time = if (cursor.isNull(3)) "NULL" else "'${cursor.getString(3)}'"
            val cat = if (cursor.isNull(4)) "NULL" else "'${cursor.getString(4)}'"
            val completed = cursor.getInt(5)
            val updated = cursor.getLong(6)

            db.execSQL("INSERT INTO `study_sessions` (id, curriculumModuleId, unitNumber, scheduledDay, scheduledTime, timeSlotCategory, isCompleted, updatedAt) VALUES ('$id', '$moduleId', $unitCounter, $day, $time, $cat, $completed, $updated)")
            unitCounter++
        }
        cursor.close()

        db.execSQL("""
            UPDATE `curriculum_modules`
            SET `totalUnits` = (
                SELECT COUNT(*) FROM `study_sessions` 
                WHERE `study_sessions`.curriculumModuleId = `curriculum_modules`.id
            )
        """)

        db.execSQL("DROP TABLE IF EXISTS `curriculum_topics`")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `modules` ADD COLUMN `syllabus` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `modules` ADD COLUMN `totalUnits` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `unitNumber` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `moduleId` TEXT DEFAULT NULL")
    }
}
