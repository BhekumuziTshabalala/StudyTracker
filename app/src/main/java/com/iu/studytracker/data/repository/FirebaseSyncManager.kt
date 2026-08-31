package com.iu.studytracker.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow

class FirebaseSyncManager(
    private val context: Context,
    private val database: com.iu.studytracker.data.database.StudyTrackerDatabase,
    private val userPreferences: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "FirebaseSyncManager"
    }

    private var db: FirebaseFirestore? = null
    private val syncScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSyncing

    private val _lastSyncResult = kotlinx.coroutines.flow.MutableStateFlow<Result<Unit>?>(null)
    val lastSyncResult: kotlinx.coroutines.flow.StateFlow<Result<Unit>?> = _lastSyncResult

    suspend fun initialize() {
        val isEnabled = userPreferences.isFirebaseSyncEnabled.first()
        if (!isEnabled) {
            Log.d(TAG, "Firebase sync is disabled")
            return
        }

        val projectId = userPreferences.firebaseProjectId.first()
        val appId = userPreferences.firebaseAppId.first()
        val apiKey = userPreferences.firebaseApiKey.first()

        if (projectId.isNullOrEmpty() || appId.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            Log.e(TAG, "Firebase credentials are not fully configured")
            return
        }

        val options = FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .build()

        try {
            var app = FirebaseApp.getApps(context).firstOrNull { it.name == "StudyTrackerSync" }
            if (app == null) {
                app = FirebaseApp.initializeApp(context, options, "StudyTrackerSync")
            }
            db = FirebaseFirestore.getInstance(app)
            Log.d(TAG, "Firebase initialized successfully")
            // Register device
            registerDevice()

            // Setup invalidation tracker to sync on local changes
            database.invalidationTracker.addObserver(object : androidx.room.InvalidationTracker.Observer(
                "degree_plans", "curriculum_modules", "curriculum_topics", "month_plans", 
                "modules", "topics", "tasks", "module_tasks", "module_schedule_events", "task_templates"
            ) {
                override fun onInvalidated(tables: Set<String>) {
                    syncScope.launch {
                        try {
                            startSync()
                        } catch (e: Exception) {
                            Log.e(TAG, "Auto-sync failed", e)
                        }
                    }
                }
            })

            // Run initial sync
            syncScope.launch {
                try {
                    startSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Initial sync failed", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }
    }

    private suspend fun registerDevice() {
        if (db == null) return
        val deviceId = userPreferences.getOrCreateDeviceId()
        
        try {
            val deviceData = mapOf(
                "deviceId" to deviceId,
                "lastSeen" to System.currentTimeMillis()
            )
            db!!.collection("devices").document(deviceId)
                .set(deviceData, SetOptions.merge())
                .await()
            Log.d(TAG, "Device registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
        }
    }

    suspend fun attemptLink(projectId: String, appId: String, apiKey: String): Result<Unit> {
        val options = FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .build()

        return try {
            var app = FirebaseApp.getApps(context).firstOrNull { it.name == "StudyTrackerSync" }
            if (app != null && (app.options.projectId != projectId || app.options.applicationId != appId || app.options.apiKey != apiKey)) {
                app.delete()
                app = null
            }
            if (app == null) {
                app = FirebaseApp.initializeApp(context, options, "StudyTrackerSync")
            }
            
            val tempDb = FirebaseFirestore.getInstance(app)
            val deviceId = userPreferences.getOrCreateDeviceId()
            val deviceData = mapOf(
                "deviceId" to deviceId,
                "lastSeen" to System.currentTimeMillis()
            )
            
            try {
                kotlinx.coroutines.withTimeout(5000L) {
                    tempDb.collection("devices").document(deviceId)
                        .set(deviceData, SetOptions.merge())
                        .await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw Exception("Connection timed out. Please ensure your device has internet access and that you have created a Firestore Database in your Firebase Console.")
            } catch (e: Exception) {
                if (e.message?.contains("PERMISSION_DENIED") == true || e.message?.contains("Missing or insufficient permissions") == true) {
                    Log.w(TAG, "Device registration denied by rules, but connection succeeded")
                } else {
                    throw e
                }
            }

            db = tempDb
            
            syncScope.launch {
                try {
                    startSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Link sync failed", e)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to link Firebase", e)
            Result.failure(Exception(e.localizedMessage ?: "Unknown error occurred"))
        }
    }

    suspend fun triggerManualSync() {
        if (_isSyncing.value) return
        syncScope.launch {
            _isSyncing.value = true
            try {
                startSync()
                _lastSyncResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _lastSyncResult.value = Result.failure(e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    suspend fun startSync() {
        if (db == null) return
        try {
            syncTable("degree_plans", com.iu.studytracker.data.database.entity.DegreePlan::class.java, database.degreePlanDao().getAllDegreePlans()) { items -> items.forEach { database.degreePlanDao().insert(it) } }
            syncTable("curriculum_modules", com.iu.studytracker.data.database.entity.CurriculumModule::class.java, database.curriculumDao().getAllCurriculumModulesSync()) { items -> items.forEach { database.curriculumDao().insertCurriculumModule(it) } }
            syncTable("curriculum_topics", com.iu.studytracker.data.database.entity.CurriculumTopic::class.java, database.curriculumDao().getAllCurriculumTopics()) { items -> database.curriculumDao().insertCurriculumTopics(items) }
            syncTable("month_plans", com.iu.studytracker.data.database.entity.MonthPlan::class.java, database.monthPlanDao().getAllMonthPlans()) { items -> items.forEach { database.monthPlanDao().insert(it) } }
            syncTable("modules", com.iu.studytracker.data.database.entity.Module::class.java, database.moduleDao().getAllModules()) { items -> items.forEach { database.moduleDao().insert(it) } }
            syncTable("topics", com.iu.studytracker.data.database.entity.Topic::class.java, database.topicDao().getAllTopics()) { items -> items.forEach { database.topicDao().insert(it) } }
            syncTable("tasks", com.iu.studytracker.data.database.entity.Task::class.java, database.taskDao().getAllTasks()) { items -> 
                val remaining = items.toMutableList()
                val sortedTasks = mutableListOf<com.iu.studytracker.data.database.entity.Task>()
                while (remaining.isNotEmpty()) {
                    val startSize = remaining.size
                    val it = remaining.iterator()
                    while (it.hasNext()) {
                        val task = it.next()
                        // Insert if parent is not in the remaining list (meaning it's already in DB or already in sortedTasks)
                        if (task.parentTaskId == null || !remaining.any { it.id == task.parentTaskId }) {
                            sortedTasks.add(task)
                            it.remove()
                        }
                    }
                    if (remaining.size == startSize) {
                        // Circular dependency, just add the rest
                        sortedTasks.addAll(remaining)
                        break
                    }
                }
                sortedTasks.forEach { database.taskDao().insert(it) } 
            }
            syncTable("module_tasks", com.iu.studytracker.data.database.entity.ModuleTask::class.java, database.moduleDetailsDao().getAllModuleTasks()) { items -> items.forEach { database.moduleDetailsDao().insertTask(it) } }
            syncTable("module_schedule_events", com.iu.studytracker.data.database.entity.ModuleScheduleEvent::class.java, database.moduleDetailsDao().getAllModuleScheduleEvents()) { items -> items.forEach { database.moduleDetailsDao().insertScheduleEvent(it) } }
            syncTable("task_templates", com.iu.studytracker.data.database.entity.TaskTemplate::class.java, database.taskTemplateDao().getAllTaskTemplates()) { items -> items.forEach { database.taskTemplateDao().insert(it) } }
            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            throw e
        }
    }

    private suspend fun <T : Any> syncTable(
        collectionName: String,
        clazz: Class<T>,
        localItems: List<T>,
        insertLocal: suspend (List<T>) -> Unit
    ) {
        val remoteSnapshot = db!!.collection("data").document("shared").collection(collectionName).get().await()
        val remoteItems = remoteSnapshot.toObjects(clazz)
        
        val localMap = localItems.associateBy { getId(it) }
        val remoteMap = remoteItems.associateBy { getId(it) }
        
        val toPush = mutableListOf<T>()
        val toPull = mutableListOf<T>()
        
        for (local in localItems) {
            val id = getId(local) ?: continue
            val remote = remoteMap[id]
            if (remote == null) {
                toPush.add(local)
            } else {
                val localTime = getUpdatedAt(local)
                val remoteTime = getUpdatedAt(remote)
                if (localTime > remoteTime) {
                    toPush.add(local)
                } else if (remoteTime > localTime) {
                    toPull.add(remote)
                }
            }
        }
        
        for (remote in remoteItems) {
            val id = getId(remote) ?: continue
            if (!localMap.containsKey(id)) {
                toPull.add(remote)
            }
        }
        
        if (toPush.isNotEmpty()) {
            val batch = db!!.batch()
            val collRef = db!!.collection("data").document("shared").collection(collectionName)
            for (item in toPush) {
                val id = getId(item)
                if (id != null) {
                    batch.set(collRef.document(id), item)
                }
            }
            batch.commit().await()
            Log.d(TAG, "Pushed ${toPush.size} items to $collectionName")
        }
        
        if (toPull.isNotEmpty()) {
            insertLocal(toPull)
            Log.d(TAG, "Pulled ${toPull.size} items from $collectionName")
        }
    }
    
    private fun <T : Any> getId(item: T): String? {
        return try {
            val method = item.javaClass.getMethod("getId")
            method.invoke(item) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    private fun <T : Any> getUpdatedAt(item: T): Long {
        return try {
            val method = item.javaClass.getMethod("getUpdatedAt")
            method.invoke(item) as? Long ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getLinkedDevices(): kotlinx.coroutines.flow.Flow<List<Map<String, Any>>> = kotlinx.coroutines.flow.callbackFlow {
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = db!!.collection("devices").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed.", error)
                return@addSnapshotListener
            }
            
            val devices = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            trySend(devices)
        }
        
        awaitClose { listener.remove() }
    }

    suspend fun removeDevice(deviceId: String) {
        if (db == null) return
        try {
            db!!.collection("devices").document(deviceId).delete().await()
            Log.d(TAG, "Device $deviceId removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove device $deviceId", e)
        }
    }
}
