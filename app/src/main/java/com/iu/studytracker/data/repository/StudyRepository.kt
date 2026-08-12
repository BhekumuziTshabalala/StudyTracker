package com.iu.studytracker.data.repository

import com.iu.studytracker.data.database.dao.DailyTaskDao
import com.iu.studytracker.data.database.dao.ModuleDao
import com.iu.studytracker.data.database.dao.MonthPlanDao
import com.iu.studytracker.data.database.dao.TopicDao
import com.iu.studytracker.data.database.dao.DegreePlanDao
import com.iu.studytracker.data.database.dao.CurriculumDao
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.CurriculumTopic
import com.iu.studytracker.data.database.entity.DailyTask
import com.iu.studytracker.data.database.entity.DegreePlan
import com.iu.studytracker.data.model.DailyTaskWithDetails
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.entity.MonthPlan
import com.iu.studytracker.data.database.entity.Topic
import com.iu.studytracker.data.model.CurriculumJson
import com.google.gson.Gson
import com.iu.studytracker.data.database.relation.ModuleWithTopics
import com.iu.studytracker.data.database.relation.MonthPlanFull
import com.iu.studytracker.data.database.relation.MonthPlanWithModules
import com.iu.studytracker.scheduler.TopicScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Single source of truth for all study data.
 *
 * Aggregates the four DAOs and provides higher-level operations
 * used by ViewModels. All suspend functions run on the caller's
 * coroutine context (typically Dispatchers.IO via viewModelScope).
 */
class StudyRepository(
    private val monthPlanDao: MonthPlanDao,
    private val moduleDao: ModuleDao,
    private val topicDao: TopicDao,
    private val dailyTaskDao: DailyTaskDao,
    private val degreePlanDao: DegreePlanDao,
    private val curriculumDao: CurriculumDao
) {

    // ── Curriculum Management ───────────────────────────────────

    suspend fun importCurriculumFromJson(jsonString: String): String? {
        return try {
            val gson = Gson()
            val curriculumData = gson.fromJson(jsonString, CurriculumJson::class.java)

            // Clear old curriculum
            curriculumDao.clearCurriculum()

            // Update Degree Plan
            val currentPlan = degreePlanDao.getCurrentPlan()
            // Extract only the first sequence of digits (e.g., "180" from "180 CP[cite: 1]")
            val creditString = Regex("\\d+").find(curriculumData.totalCreditPoints)?.value
            val credits = creditString?.toIntOrNull() ?: 180

            if (currentPlan != null) {
                degreePlanDao.insert(currentPlan.copy(totalCreditsRequired = credits))
            } else {
                degreePlanDao.insert(DegreePlan(totalCreditsRequired = credits))
            }

            // Insert new curriculum
            curriculumData.curriculum.forEach { semesterJson ->
                semesterJson.allModules.forEach { moduleJson ->
                    val module = CurriculumModule(
                        semester = semesterJson.effectiveSemester,
                        code = moduleJson.code.replace("\\[cite:.*\\]".toRegex(), ""),
                        name = moduleJson.name.replace("\\[cite:.*\\]".toRegex(), ""),
                        assessment = moduleJson.assessment.replace("\\[cite:.*\\]".toRegex(), "")
                    )
                    val topics = moduleJson.coreTopics.map { topicString ->
                        CurriculumTopic(title = topicString.replace("\\[cite:.*\\]".toRegex(), ""))
                    }
                    curriculumDao.insertModuleWithTopics(module, topics)
                }
            }
            curriculumData.programme.replace("\\[cite:.*\\]".toRegex(), "")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCurriculumModuleCompletion(moduleId: Long, isCompleted: Boolean) {
        curriculumDao.updateModuleCompletion(moduleId, isCompleted)
    }

    suspend fun insertCurriculumModuleManually(module: CurriculumModule, topics: List<CurriculumTopic>) {
        curriculumDao.insertModuleWithTopics(module, topics)
    }

    fun observeAllCurriculumModules(): Flow<List<CurriculumModule>> {
        return curriculumDao.getAllCurriculumModules()
    }

    suspend fun getAllCurriculumModulesSync(): List<CurriculumModule> {
        return curriculumDao.getAllCurriculumModulesSync()
    }

    suspend fun deleteCurriculumModule(moduleId: Long) {
        curriculumDao.deleteCurriculumModule(moduleId)
    }

    // ── Date formatting ─────────────────────────────────────────

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayString(): String = LocalDate.now().format(dateFormatter)

    // ── MonthPlan ───────────────────────────────────────────────

    suspend fun getOrCreateCurrentMonthPlan(): MonthPlan {
        val now = LocalDate.now()
        val existing = monthPlanDao.getByYearAndMonth(now.year, now.monthValue)
        if (existing != null) return existing

        val newPlan = MonthPlan(year = now.year, month = now.monthValue)
        val id = monthPlanDao.insert(newPlan)
        return newPlan.copy(id = id)
    }

    suspend fun getMonthPlan(year: Int, month: Int): MonthPlan? {
        return monthPlanDao.getByYearAndMonth(year, month)
    }

    suspend fun getMonthPlanById(id: Long): MonthPlan? {
        return monthPlanDao.getById(id)
    }

    fun observeMonthPlan(year: Int, month: Int): Flow<MonthPlan?> {
        return monthPlanDao.observeByYearAndMonth(year, month)
    }

    fun observeAllMonthPlans(): Flow<List<MonthPlan>> {
        return monthPlanDao.observeAll()
    }

    suspend fun markSetupComplete(monthPlanId: Long) {
        monthPlanDao.markSetupComplete(monthPlanId)
    }

    suspend fun deleteMonthPlan(monthPlanId: Long) {
        monthPlanDao.deleteById(monthPlanId)
    }

    // ── Modules ─────────────────────────────────────────────────

    suspend fun insertModule(module: Module): Long {
        return moduleDao.insert(module)
    }

    suspend fun insertModules(modules: List<Module>): List<Long> {
        return moduleDao.insertAll(modules)
    }

    suspend fun getModulesForMonth(monthPlanId: Long): List<Module> {
        return moduleDao.getModulesForMonth(monthPlanId)
    }

    fun observeModulesForMonth(monthPlanId: Long): Flow<List<Module>> {
        return moduleDao.observeModulesForMonth(monthPlanId)
    }

    suspend fun getModulesWithTopics(monthPlanId: Long): List<ModuleWithTopics> {
        return moduleDao.getModulesWithTopicsForMonth(monthPlanId)
    }

    // ── Topics ───────────────────────────────────────────────────

    suspend fun insertTopics(topics: List<Topic>): List<Long> {
        return topicDao.insertAll(topics)
    }

    suspend fun getTopicsForModule(moduleId: Long): List<Topic> {
        return topicDao.getTopicsForModule(moduleId)
    }

    suspend fun getAllTopicsForMonth(monthPlanId: Long): List<Topic> {
        return topicDao.getAllTopicsForMonth(monthPlanId)
    }

    suspend fun countTopicsForMonth(monthPlanId: Long): Int {
        return topicDao.countTopicsForMonth(monthPlanId)
    }

    // ── Daily Tasks ─────────────────────────────────────────────

    suspend fun insertDailyTasks(tasks: List<DailyTask>) {
        dailyTaskDao.insertAll(tasks)
    }

    suspend fun getTasksForDate(date: String): List<DailyTask> {
        return dailyTaskDao.getTasksForDate(date)
    }

    fun observeTasksForDate(date: String): Flow<List<DailyTask>> {
        return dailyTaskDao.observeTasksForDate(date)
    }

    fun observeTasksForMonth(monthPlanId: Long): Flow<List<DailyTask>> {
        return dailyTaskDao.observeTasksForMonth(monthPlanId)
    }

    fun observeTodaysTasks(): Flow<List<DailyTask>> {
        return dailyTaskDao.observeTasksForDate(todayString())
    }

    fun observeTasksWithDetailsForDate(date: String): Flow<List<DailyTaskWithDetails>> {
        return dailyTaskDao.observeTasksWithDetailsForDate(date)
    }

    fun observeTodaysTasksWithDetails(): Flow<List<DailyTaskWithDetails>> {
        return dailyTaskDao.observeTasksWithDetailsForDate(todayString())
    }

    fun observeAllTasksWithDetailsForMonth(monthPlanId: Long): Flow<List<DailyTaskWithDetails>> {
        return dailyTaskDao.observeAllTasksWithDetailsForMonth(monthPlanId)
    }

    suspend fun toggleTaskCompletion(taskId: Long, isCurrentlyCompleted: Boolean) {
        if (isCurrentlyCompleted) {
            dailyTaskDao.markIncomplete(taskId)
        } else {
            dailyTaskDao.markComplete(taskId)
        }
    }

    suspend fun deleteTasksForMonth(monthPlanId: Long) {
        dailyTaskDao.deleteTasksForMonth(monthPlanId)
    }

    suspend fun incrementTimeSpent(taskId: Long, minutes: Int) {
        dailyTaskDao.incrementTimeSpent(taskId, minutes)
    }

    suspend fun rebalanceSchedule(monthPlanId: Long): Boolean {
        val plan = getMonthPlanById(monthPlanId) ?: return false
        val today = todayString()
        val incomplete = dailyTaskDao.getIncompleteTasksBeforeDate(monthPlanId, today)
        if (incomplete.isEmpty()) return false

        val updated = TopicScheduler.rebalanceSchedule(
            incompleteTasks = incomplete,
            year = plan.year,
            month = plan.month,
            today = LocalDate.now()
        )
        if (updated.isNotEmpty()) {
            dailyTaskDao.insertAll(updated)
        }
        return true
    }

    // ── Degree Plan ──────────────────────────────────────────────

    suspend fun insertDegreePlan(plan: DegreePlan): Long {
        return degreePlanDao.insert(plan)
    }

    fun observeCurrentDegreePlan(): Flow<DegreePlan?> {
        return degreePlanDao.observeCurrentPlan()
    }

    suspend fun getCurrentDegreePlan(): DegreePlan? {
        return degreePlanDao.getCurrentPlan()
    }

    // ── Stats ────────────────────────────────────────────────────

    suspend fun getCompletionStats(monthPlanId: Long): Pair<Int, Int> {
        val total = dailyTaskDao.getTotalTaskCount(monthPlanId)
        val completed = dailyTaskDao.getCompletedTaskCount(monthPlanId)
        return Pair(completed, total)
    }

    suspend fun getIncompleteCountForToday(): Int {
        return dailyTaskDao.getIncompleteCountForDate(todayString())
    }

    suspend fun getScheduledDatesForMonth(monthPlanId: Long): List<String> {
        return dailyTaskDao.getScheduledDatesForMonth(monthPlanId)
    }

    // ── Full Plan Assembly ───────────────────────────────────────

    /**
     * Assembles a complete snapshot of a month's study plan.
     * Returns null if the month plan doesn't exist.
     */
    suspend fun getFullMonthPlan(monthPlanId: Long): MonthPlanFull? {
        val planWithModules = monthPlanDao.getWithModules(monthPlanId) ?: return null
        val modulesWithTopics = moduleDao.getModulesWithTopicsForMonth(monthPlanId)
        val tasks = dailyTaskDao.getTasksForDate(todayString()) // just today for dashboard
        return MonthPlanFull(
            monthPlanWithModules = planWithModules,
            modulesWithTopics = modulesWithTopics,
            dailyTasks = tasks
        )
    }

    // ── Combined Setup Operation ────────────────────────────────

    /**
     * Full monthly setup: creates a plan, two modules, and their topics.
     * Returns the MonthPlan id.
     *
     * @param year Calendar year
     * @param month Calendar month (1–12)
     * @param module1Name Name of the first module
     * @param module1Topics List of topic titles for the first module
     * @param module2Name Name of the second module
     * @param module2Topics List of topic titles for the second module
     */
    suspend fun performMonthlySetup(
        year: Int,
        month: Int,
        module1Name: String,
        module1Topics: List<String>,
        module2Name: String,
        module2Topics: List<String>
    ): Long {
        // 1. Create or get the month plan
        val existingPlan = monthPlanDao.getByYearAndMonth(year, month)
        val monthPlanId = if (existingPlan != null) {
            // Clear old data for re-setup
            dailyTaskDao.deleteTasksForMonth(existingPlan.id)
            monthPlanDao.deleteById(existingPlan.id)
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
        } else {
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
        }

        // 2. Create the two modules
        val mod1Id = moduleDao.insert(
            Module(monthPlanId = monthPlanId, name = module1Name, orderIndex = 0)
        )
        val mod2Id = moduleDao.insert(
            Module(monthPlanId = monthPlanId, name = module2Name, orderIndex = 1)
        )

        // 3. Create topics for module 1
        val topics1 = module1Topics.mapIndexed { index, title ->
            Topic(moduleId = mod1Id, title = title, orderIndex = index)
        }
        topicDao.insertAll(topics1)

        // 4. Create topics for module 2
        val topics2 = module2Topics.mapIndexed { index, title ->
            Topic(moduleId = mod2Id, title = title, orderIndex = index)
        }
        topicDao.insertAll(topics2)

        return monthPlanId
    }

    // ── Schedule Generation ─────────────────────────────────────

    /**
     * Generates a study schedule for a month plan and saves it to the database.
     *
     * Uses [TopicScheduler] to distribute topics across available days,
     * then batch-inserts the resulting [DailyTask] records.
     *
     * @param monthPlanId The month plan to generate a schedule for.
     * @param startFrom First eligible day (defaults to today).
     * @return Schedule result with tasks and summary, or null if plan not found.
     */
    suspend fun generateAndSaveSchedule(
        monthPlanId: Long,
        startFrom: LocalDate = LocalDate.now()
    ): TopicScheduler.ScheduleResult? {
        val plan = monthPlanDao.getById(monthPlanId) ?: return null
        val modulesWithTopics = moduleDao.getModulesWithTopicsForMonth(monthPlanId)

        if (modulesWithTopics.isEmpty()) return null

        // Clear any previously generated schedule
        dailyTaskDao.deleteTasksForMonth(monthPlanId)

        // Generate the schedule
        val result = TopicScheduler.generateSchedule(
            monthPlanId = monthPlanId,
            modulesWithTopics = modulesWithTopics,
            year = plan.year,
            month = plan.month,
            startFrom = startFrom
        )

        // Persist tasks to database
        if (result.tasks.isNotEmpty()) {
            dailyTaskDao.insertAll(result.tasks)
        }

        // Mark setup as complete
        monthPlanDao.markSetupComplete(monthPlanId)

        return result
    }

    /**
     * One-shot convenience: performs monthly setup AND generates the schedule.
     *
     * This is the main entry point called from the Setup screen's ViewModel.
     *
     * @return Pair of (monthPlanId, ScheduleResult) for confirmation UI.
     */
    suspend fun setupMonthAndGenerateSchedule(
        year: Int,
        month: Int,
        module1Name: String,
        module1Topics: List<String>,
        module2Name: String,
        module2Topics: List<String>,
        startFrom: LocalDate = LocalDate.now()
    ): Pair<Long, TopicScheduler.ScheduleResult?> {
        val monthPlanId = performMonthlySetup(
            year = year,
            month = month,
            module1Name = module1Name,
            module1Topics = module1Topics,
            module2Name = module2Name,
            module2Topics = module2Topics
        )

        val result = generateAndSaveSchedule(monthPlanId, startFrom)
        return Pair(monthPlanId, result)
    }

    suspend fun setupMonthWithCurriculumModules(
        year: Int,
        month: Int,
        moduleIds: List<Long>,
        startFrom: LocalDate = LocalDate.now()
    ): Pair<Long, TopicScheduler.ScheduleResult?> {
        // 1. Create or get the month plan
        val existingPlan = monthPlanDao.getByYearAndMonth(year, month)
        val monthPlanId = if (existingPlan != null) {
            // Clear old data for re-setup
            dailyTaskDao.deleteTasksForMonth(existingPlan.id)
            monthPlanDao.deleteById(existingPlan.id)
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
        } else {
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
        }

        // 2. Fetch the selected curriculum modules and topics
        val curriculumModules = curriculumDao.getAllCurriculumModulesSync().filter { moduleIds.contains(it.id) }
        
        // 3. Insert as Active Modules
        curriculumModules.forEachIndexed { index, currModule ->
            val modId = moduleDao.insert(
                Module(monthPlanId = monthPlanId, name = currModule.name, orderIndex = index)
            )
            
            // Fetch topics for this curriculum module
            val currTopics = curriculumDao.getTopicsForModule(currModule.id)
            val topics = currTopics.mapIndexed { tIndex, currTopic ->
                Topic(moduleId = modId, title = currTopic.title, orderIndex = tIndex)
            }
            topicDao.insertAll(topics)
        }

        val result = generateAndSaveSchedule(monthPlanId, startFrom)
        return Pair(monthPlanId, result)
    }
}
