package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculumModule(module: CurriculumModule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySession>)

    @Query("SELECT * FROM curriculum_modules ORDER BY semester ASC, id ASC")
    fun getAllCurriculumModules(): Flow<List<CurriculumModule>>

    @Query("SELECT * FROM curriculum_modules ORDER BY semester ASC, id ASC")
    suspend fun getAllCurriculumModulesSync(): List<CurriculumModule>

    @Query("UPDATE curriculum_modules SET isCompleted = :isCompleted WHERE id = :moduleId")
    suspend fun updateModuleCompletion(moduleId: String, isCompleted: Boolean)
    
    @Query("UPDATE study_sessions SET isCompleted = :isCompleted WHERE id = :sessionId")
    suspend fun updateSessionCompletion(sessionId: String, isCompleted: Boolean)
    
    @Query("UPDATE curriculum_modules SET examPassed = :examPassed, finalGrade = :finalGrade WHERE id = :moduleId")
    suspend fun updateExamResult(moduleId: String, examPassed: Boolean?, finalGrade: String?)

    @Query("SELECT * FROM curriculum_modules WHERE id = :moduleId")
    fun observeModuleById(moduleId: String): Flow<CurriculumModule?>

    @Query("SELECT * FROM study_sessions WHERE curriculumModuleId = :moduleId ORDER BY id ASC")
    suspend fun getSessionsForModule(moduleId: String): List<StudySession>
    
    @Query("SELECT * FROM study_sessions WHERE curriculumModuleId = :moduleId ORDER BY id ASC")
    fun observeSessionsForModule(moduleId: String): Flow<List<StudySession>>
    
    @Query("SELECT * FROM study_sessions WHERE curriculumModuleId IN (:moduleIds) ORDER BY id ASC")
    suspend fun getSessionsForModules(moduleIds: List<String>): List<StudySession>


    @Query("SELECT * FROM study_sessions")
    suspend fun getAllStudySessions(): List<StudySession>
    
    @Query("SELECT * FROM study_sessions")
    fun observeAllStudySessions(): Flow<List<StudySession>>

    @Query("DELETE FROM curriculum_modules")
    suspend fun clearCurriculum()

    @Query("DELETE FROM curriculum_modules WHERE id = :moduleId")
    suspend fun deleteCurriculumModule(moduleId: String)

    @Query("""
        SELECT ct.* FROM study_sessions ct
        INNER JOIN curriculum_modules cm ON ct.curriculumModuleId = cm.id
        WHERE ct.curriculumModuleId IN (:moduleIds)
        ORDER BY cm.semester ASC, ct.id ASC
    """)
    fun observeSessionsForModules(moduleIds: List<String>): Flow<List<StudySession>>

    /** Update the day-of-week assignment for a single topic (1=Mon..7=Sun, null=unscheduled) */
    @Query("UPDATE study_sessions SET scheduledDay = :day, scheduledTime = :time, timeSlotCategory = :category WHERE id = :sessionId")
    suspend fun updateSessionSchedule(sessionId: String, day: Int?, time: String?, category: String?)

    
    

    /** All sessions whose scheduledDay matches the given weekday (1=Mon..7=Sun) */
    @Query("SELECT * FROM study_sessions WHERE scheduledDay = :dayOfWeek ORDER BY id ASC")
    suspend fun getSessionsForDay(dayOfWeek: Int): List<StudySession>

    /** Observe sessions for a given weekday */
    @Query("SELECT * FROM study_sessions WHERE scheduledDay = :dayOfWeek ORDER BY id ASC")
    fun observeSessionsForDay(dayOfWeek: Int): Flow<List<StudySession>>

    @Transaction
    suspend fun insertModuleWithSessions(module: CurriculumModule, sessions: List<StudySession>) {
        insertCurriculumModule(module)
        val sessionsWithIds = sessions.map { it.copy(curriculumModuleId = module.id) }
        insertStudySessions(sessionsWithIds)
    }
}
