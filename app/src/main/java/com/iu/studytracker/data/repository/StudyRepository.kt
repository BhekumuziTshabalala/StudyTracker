package com.iu.studytracker.data.repository

import com.iu.studytracker.data.database.dao.TaskDao
import com.iu.studytracker.data.database.entity.Task
import com.iu.studytracker.data.model.TaskWithDetails
import com.iu.studytracker.data.database.entity.TaskStatus
import com.iu.studytracker.data.database.entity.TaskPriority
import com.iu.studytracker.data.database.dao.ModuleDao
import com.iu.studytracker.data.database.dao.MonthPlanDao
import com.iu.studytracker.data.database.dao.TopicDao
import com.iu.studytracker.data.database.dao.DegreePlanDao
import com.iu.studytracker.data.database.dao.CurriculumDao
import com.iu.studytracker.data.database.dao.ModuleDetailsDao
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.CurriculumTopic
import com.iu.studytracker.data.database.entity.ModuleTask
import com.iu.studytracker.data.database.entity.ModuleScheduleEvent
import com.iu.studytracker.data.database.entity.DegreePlan
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
import kotlinx.coroutines.flow.first
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
    private val taskDao: TaskDao,
    private val degreePlanDao: DegreePlanDao,
    private val curriculumDao: CurriculumDao,
    private val moduleDetailsDao: ModuleDetailsDao,
    private val taskTemplateDao: com.iu.studytracker.data.database.dao.TaskTemplateDao
) {

    // ── Module Details (Tasks & Events) ─────────────────────────

    fun observeTasksForModule(moduleId: String): Flow<List<ModuleTask>> {
        return moduleDetailsDao.getTasksForModule(moduleId)
    }

    suspend fun insertModuleTask(task: ModuleTask) {
        moduleDetailsDao.insertTask(task)
    }

    suspend fun updateModuleTask(task: ModuleTask) {
        moduleDetailsDao.updateTask(task)
    }

    suspend fun deleteModuleTask(task: ModuleTask) {
        moduleDetailsDao.deleteTask(task)
    }

    suspend fun updateModuleTaskCompletion(taskId: String, isCompleted: Boolean) {
        moduleDetailsDao.updateTaskCompletion(taskId, isCompleted)
    }

    fun observeScheduleEventsForModule(moduleId: String): Flow<List<ModuleScheduleEvent>> {
        return moduleDetailsDao.getScheduleEventsForModule(moduleId)
    }

    fun observeTaskCountForModule(moduleId: String): Flow<Int> {
        return moduleDetailsDao.observeTaskCountForModule(moduleId)
    }

    fun observeCompletedTaskCountForModule(moduleId: String): Flow<Int> {
        return moduleDetailsDao.observeCompletedTaskCountForModule(moduleId)
    }

    suspend fun insertScheduleEvent(event: ModuleScheduleEvent) {
        moduleDetailsDao.insertScheduleEvent(event)
    }

    suspend fun updateScheduleEvent(event: ModuleScheduleEvent) {
        moduleDetailsDao.updateScheduleEvent(event)
    }

    suspend fun deleteScheduleEvent(event: ModuleScheduleEvent) {
        moduleDetailsDao.deleteScheduleEvent(event)
    }

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

    suspend fun updateCurriculumModuleCompletion(moduleId: String, isCompleted: Boolean) {
        curriculumDao.updateModuleCompletion(moduleId, isCompleted)
    }

    suspend fun updateExamResult(moduleId: String, passed: Boolean?, grade: String?) {
        curriculumDao.updateExamResult(moduleId, passed, grade)
    }

    fun observeModuleById(moduleId: String): Flow<CurriculumModule?> {
        return curriculumDao.observeModuleById(moduleId)
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

    suspend fun deleteCurriculumModule(moduleId: String) {
        curriculumDao.deleteCurriculumModule(moduleId)
    }

    suspend fun updateCurriculumTopicScheduledDay(topicId: String, dayOfWeek: Int?) {
        curriculumDao.updateTopicScheduledDay(topicId, dayOfWeek)
    }

    fun observeCurriculumTopicsForModules(moduleIds: List<String>): Flow<List<CurriculumTopic>> {
        return curriculumDao.observeTopicsForModules(moduleIds)
    }

    fun observeCurriculumTopicsForDay(dayOfWeek: Int): Flow<List<CurriculumTopic>> {
        return curriculumDao.observeTopicsForDay(dayOfWeek)
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
        monthPlanDao.insert(newPlan)
        return newPlan
    }

    suspend fun getMonthPlan(year: Int, month: Int): MonthPlan? {
        return monthPlanDao.getByYearAndMonth(year, month)
    }

    suspend fun getMonthPlanById(id: String): MonthPlan? {
        return monthPlanDao.getById(id)
    }

    fun observeMonthPlan(year: Int, month: Int): Flow<MonthPlan?> {
        return monthPlanDao.observeByYearAndMonth(year, month)
    }

    fun observeAllMonthPlans(): Flow<List<MonthPlan>> {
        return monthPlanDao.observeAll()
    }

    suspend fun markSetupComplete(monthPlanId: String) {
        monthPlanDao.markSetupComplete(monthPlanId)
    }

    suspend fun deleteMonthPlan(monthPlanId: String) {
        monthPlanDao.deleteById(monthPlanId)
    }

    // ── Modules ─────────────────────────────────────────────────

    suspend fun insertModule(module: Module): String {
        moduleDao.insert(module)
        return module.id
    }

    suspend fun insertModules(modules: List<Module>): List<String> {
        moduleDao.insertAll(modules)
        return modules.map { it.id }
    }

    suspend fun getModulesForMonth(monthPlanId: String): List<Module> {
        return moduleDao.getModulesForMonth(monthPlanId)
    }

    fun observeModulesForMonth(monthPlanId: String): Flow<List<Module>> {
        return moduleDao.observeModulesForMonth(monthPlanId)
    }

    suspend fun getModulesWithTopics(monthPlanId: String): List<ModuleWithTopics> {
        return moduleDao.getModulesWithTopicsForMonth(monthPlanId)
    }

    // ── Topics ───────────────────────────────────────────────────

    suspend fun insertTopics(topics: List<Topic>): List<String> {
        topicDao.insertAll(topics)
        return topics.map { it.id }
    }

    suspend fun getTopicsForModule(moduleId: String): List<Topic> {
        return topicDao.getTopicsForModule(moduleId)
    }

    suspend fun getAllTopicsForMonth(monthPlanId: String): List<Topic> {
        return topicDao.getAllTopicsForMonth(monthPlanId)
    }

    suspend fun countTopicsForMonth(monthPlanId: String): Int {
        return topicDao.countTopicsForMonth(monthPlanId)
    }

    // ── Tasks ─────────────────────────────────────────────

    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
    }

    suspend fun insertTasks(tasks: List<Task>) {
        taskDao.insertAll(tasks)
    }

    suspend fun getTasksForDate(date: String): List<Task> {
        return taskDao.getTasksForDate(date)
    }

    fun observeTasksForDate(date: String): Flow<List<Task>> {
        return taskDao.observeTasksForDate(date)
    }

    fun observeOverdueTasks(): Flow<List<Task>> {
        return taskDao.observeOverdueTasks(todayString())
    }

    suspend fun rescheduleOverdueTasksToToday() {
        val overdueTasks = taskDao.getOverdueTasksSync(todayString())
        val updatedTasks = overdueTasks.map { it.copy(scheduledDate = todayString()) }
        if (updatedTasks.isNotEmpty()) {
            taskDao.insertAll(updatedTasks) // insertAll with REPLACE strategy updates them
        }
    }

    // ── Task Templates ──────────────────────────────────────────

    suspend fun insertTaskTemplate(template: com.iu.studytracker.data.database.entity.TaskTemplate) {
        taskTemplateDao.insert(template)
    }

    suspend fun updateTaskTemplate(template: com.iu.studytracker.data.database.entity.TaskTemplate) {
        taskTemplateDao.update(template)
    }

    suspend fun deleteTaskTemplate(template: com.iu.studytracker.data.database.entity.TaskTemplate) {
        taskTemplateDao.delete(template)
    }

    fun observeAllTaskTemplates(): Flow<List<com.iu.studytracker.data.database.entity.TaskTemplate>> {
        return taskTemplateDao.observeAll()
    }

    fun observeTasksForMonth(monthPlanId: String): Flow<List<Task>> {
        return taskDao.observeTasksForMonth(monthPlanId)
    }

    fun observeTodaysTasks(): Flow<List<Task>> {
        return taskDao.observeTasksForDate(todayString())
    }

    fun observeTasksWithDetailsForDate(date: String): Flow<List<TaskWithDetails>> {
        return taskDao.observeTasksWithDetailsForDate(date)
    }

    fun observeTodaysTasksWithDetails(): Flow<List<TaskWithDetails>> {
        return taskDao.observeTasksWithDetailsForDate(todayString())
    }

    fun observeAllTasksWithDetailsForMonth(monthPlanId: String): Flow<List<TaskWithDetails>> {
        return taskDao.observeAllTasksWithDetailsForMonth(monthPlanId)
    }

    fun observeIncompleteTasksWithDetails(): Flow<List<TaskWithDetails>> {
        return taskDao.observeIncompleteTasksWithDetails()
    }

    fun observeSubTasksWithDetails(parentId: String): Flow<List<TaskWithDetails>> {
        return taskDao.observeSubTasksWithDetails(parentId)
    }

    suspend fun updateTaskPriority(taskId: String, priority: TaskPriority) {
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            taskDao.update(task.copy(priority = priority))
        }
    }

    suspend fun getTasksWithRecurrenceSync(): List<Task> {
        return taskDao.getTasksWithRecurrenceSync()
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }

    suspend fun incrementTaskMinutes(taskId: String, minutes: Int) {
        taskDao.incrementTaskMinutes(taskId, minutes)
    }

    suspend fun toggleTaskCompletion(taskId: String, isCurrentlyCompleted: Boolean) {
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            val newStatus = if (isCurrentlyCompleted) TaskStatus.TODO else TaskStatus.DONE
            val completedAt = if (isCurrentlyCompleted) null else System.currentTimeMillis()
            taskDao.update(task.copy(status = newStatus, completedAt = completedAt))
        }
    }

    suspend fun deleteTasksForMonth(monthPlanId: String) {
        taskDao.deleteTasksForMonth(monthPlanId)
    }

    suspend fun incrementTimeSpent(taskId: String, minutes: Int) {
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            taskDao.update(task.copy(actualMinutesSpent = task.actualMinutesSpent + minutes))
        }
    }

    suspend fun rebalanceSchedule(monthPlanId: String): Boolean {
        val plan = getMonthPlanById(monthPlanId) ?: return false
        val today = todayString()
        val incomplete = taskDao.getIncompleteTasksBeforeDate(monthPlanId, today)
        if (incomplete.isEmpty()) return false

        val updated = TopicScheduler.rebalanceSchedule(
            incompleteTasks = incomplete,
            year = plan.year,
            month = plan.month,
            today = LocalDate.now()
        )
        if (updated.isNotEmpty()) {
            taskDao.insertAll(updated)
        }
        return true
    }

    // ── Degree Plan ──────────────────────────────────────────────

    suspend fun insertDegreePlan(plan: DegreePlan): String {
        degreePlanDao.insert(plan)
        return plan.id
    }

    fun observeCurrentDegreePlan(): Flow<DegreePlan?> {
        return degreePlanDao.observeCurrentPlan()
    }

    suspend fun getCurrentDegreePlan(): DegreePlan? {
        return degreePlanDao.getCurrentPlan()
    }

    // ── Stats ────────────────────────────────────────────────────

    suspend fun getCompletionStats(monthPlanId: String): Pair<Int, Int> {
        // We will need to write custom methods in TaskDao or count manually
        val allTasks = taskDao.observeTasksForMonth(monthPlanId).first()
        val total = allTasks.size
        val completed = allTasks.count { it.status == TaskStatus.DONE }
        return Pair(completed, total)
    }

    suspend fun getIncompleteCountForToday(): Int {
        val allTasks = taskDao.getTasksForDate(todayString())
        return allTasks.count { it.status != TaskStatus.DONE }
    }

    suspend fun getScheduledDatesForMonth(monthPlanId: String): List<String> {
        val allTasks = taskDao.observeTasksForMonth(monthPlanId).first()
        return allTasks.mapNotNull { it.scheduledDate }.distinct()
    }

    fun observeTasksCompletedBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<Task>> {
        return taskDao.observeTasksCompletedBetween(startTimestamp, endTimestamp)
    }

    fun observeTasksScheduledBetween(startDate: String, endDate: String): Flow<List<Task>> {
        return taskDao.observeTasksScheduledBetween(startDate, endDate)
    }

    fun observeTotalFocusTime(): Flow<Int> {
        return taskDao.observeTotalFocusTime()
            .let { flow -> 
                kotlinx.coroutines.flow.flow {
                    flow.collect { value -> emit(value ?: 0) }
                }
            }
    }

    // ── Full Plan Assembly ───────────────────────────────────────

    /**
     * Assembles a complete snapshot of a month's study plan.
     * Returns null if the month plan doesn't exist.
     */
    suspend fun getFullMonthPlan(monthPlanId: String): MonthPlanFull? {
        val planWithModules = monthPlanDao.getWithModules(monthPlanId) ?: return null
        val modulesWithTopics = moduleDao.getModulesWithTopicsForMonth(monthPlanId)
        val tasks = taskDao.getTasksForDate(todayString()) // just today for dashboard
        return MonthPlanFull(
            monthPlanWithModules = planWithModules,
            modulesWithTopics = modulesWithTopics,
            tasks = tasks
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
    ): String {
        // 1. Create or get the month plan
        val existingPlan = monthPlanDao.getByYearAndMonth(year, month)
        val monthPlanId = if (existingPlan != null) {
            // Clear old data for re-setup
            taskDao.deleteTasksForMonth(existingPlan.id)
            monthPlanDao.deleteById(existingPlan.id)
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
            newPlan.id
        } else {
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
            newPlan.id
        }

        // 2. Create the two modules
        val mod1 = Module(monthPlanId = monthPlanId, name = module1Name, orderIndex = 0)
        moduleDao.insert(mod1)
        val mod1Id = mod1.id

        val mod2 = Module(monthPlanId = monthPlanId, name = module2Name, orderIndex = 1)
        moduleDao.insert(mod2)
        val mod2Id = mod2.id

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
     * then batch-inserts the resulting [Task] records.
     *
     * @param monthPlanId The month plan to generate a schedule for.
     * @param startFrom First eligible day (defaults to today).
     * @return Schedule result with tasks and summary, or null if plan not found.
     */
    suspend fun generateAndSaveSchedule(
        monthPlanId: String,
        startFrom: LocalDate = LocalDate.now()
    ): TopicScheduler.ScheduleResult? {
        val plan = monthPlanDao.getById(monthPlanId) ?: return null
        val modulesWithTopics = moduleDao.getModulesWithTopicsForMonth(monthPlanId)

        if (modulesWithTopics.isEmpty()) return null

        // Clear any previously generated schedule
        taskDao.deleteTasksForMonth(monthPlanId)

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
            taskDao.insertAll(result.tasks)
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
    ): Pair<String, TopicScheduler.ScheduleResult?> {
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
        moduleIds: List<String>,
        startFrom: LocalDate = LocalDate.now()
    ): Pair<String, TopicScheduler.ScheduleResult?> {
        // 1. Create or get the month plan
        val existingPlan = monthPlanDao.getByYearAndMonth(year, month)
        val monthPlanId = if (existingPlan != null) {
            // Clear old data for re-setup
            taskDao.deleteTasksForMonth(existingPlan.id)
            monthPlanDao.deleteById(existingPlan.id)
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
            newPlan.id
        } else {
            val newPlan = MonthPlan(year = year, month = month)
            monthPlanDao.insert(newPlan)
            newPlan.id
        }

        // 2. Fetch the selected curriculum modules and topics
        val curriculumModules = curriculumDao.getAllCurriculumModulesSync().filter { moduleIds.contains(it.id) }
        
        // 3. Insert as Active Modules
        curriculumModules.forEachIndexed { index, currModule ->
            val mod = Module(monthPlanId = monthPlanId, name = currModule.name, orderIndex = index)
            moduleDao.insert(mod)
            val modId = mod.id
            
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
