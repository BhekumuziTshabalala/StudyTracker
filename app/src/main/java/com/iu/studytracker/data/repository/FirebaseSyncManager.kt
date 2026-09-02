package com.iu.studytracker.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
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

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
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

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.d(TAG, "Cannot sync: user not signed in")
            return
        }

        try {
            Log.d(TAG, "Firebase initialized successfully")
            // Register device
            registerDevice()

            // Setup invalidation tracker to sync on local changes
            database.invalidationTracker.addObserver(object : androidx.room.InvalidationTracker.Observer(
                "degree_plans", "curriculum_modules", "study_sessions", "month_plans", 
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
            Log.e(TAG, "Failed to initialize Firebase sync", e)
        }
    }

    private suspend fun registerDevice() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = userPreferences.getOrCreateDeviceId()
        
        try {
            val deviceData = mapOf(
                "deviceId" to deviceId,
                "lastSeen" to System.currentTimeMillis()
            )
            db.collection("users").document(uid)
                .collection("devices").document(deviceId)
                .set(deviceData, SetOptions.merge())
                .await()
            Log.d(TAG, "Device registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "Cannot sync: user not signed in")
            return
        }

        try {
            syncTable(uid, "degree_plans", com.iu.studytracker.data.database.entity.DegreePlan::class.java, database.degreePlanDao().getAllDegreePlans()) { items -> items.forEach { database.degreePlanDao().insert(it) } }
            syncTable(uid, "curriculum_modules", com.iu.studytracker.data.database.entity.CurriculumModule::class.java, database.curriculumDao().getAllCurriculumModulesSync()) { items -> items.forEach { database.curriculumDao().insertCurriculumModule(it) } }
            syncTable(uid, "study_sessions", com.iu.studytracker.data.database.entity.StudySession::class.java, database.curriculumDao().getAllStudySessions()) { items -> database.curriculumDao().insertStudySessions(items) }
            syncTable(uid, "month_plans", com.iu.studytracker.data.database.entity.MonthPlan::class.java, database.monthPlanDao().getAllMonthPlans()) { items -> items.forEach { database.monthPlanDao().insert(it) } }
            syncTable(uid, "modules", com.iu.studytracker.data.database.entity.Module::class.java, database.moduleDao().getAllModules()) { items -> items.forEach { database.moduleDao().insert(it) } }
            syncTable(uid, "topics", com.iu.studytracker.data.database.entity.Topic::class.java, database.topicDao().getAllTopics()) { items -> items.forEach { database.topicDao().insert(it) } }
            syncTable(uid, "tasks", com.iu.studytracker.data.database.entity.Task::class.java, database.taskDao().getAllTasks()) { items -> 
                val remaining = items.toMutableList()
                val sortedTasks = mutableListOf<com.iu.studytracker.data.database.entity.Task>()
                while (remaining.isNotEmpty()) {
                    val startSize = remaining.size
                    val it = remaining.iterator()
                    while (it.hasNext()) {
                        val task = it.next()
                        if (task.parentTaskId == null || !remaining.any { it.id == task.parentTaskId }) {
                            sortedTasks.add(task)
                            it.remove()
                        }
                    }
                    if (remaining.size == startSize) {
                        sortedTasks.addAll(remaining)
                        break
                    }
                }
                sortedTasks.forEach { database.taskDao().insert(it) } 
            }
            syncTable(uid, "module_tasks", com.iu.studytracker.data.database.entity.ModuleTask::class.java, database.moduleDetailsDao().getAllModuleTasks()) { items -> items.forEach { database.moduleDetailsDao().insertTask(it) } }
            syncTable(uid, "module_schedule_events", com.iu.studytracker.data.database.entity.ModuleScheduleEvent::class.java, database.moduleDetailsDao().getAllModuleScheduleEvents()) { items -> items.forEach { database.moduleDetailsDao().insertScheduleEvent(it) } }
            syncTable(uid, "task_templates", com.iu.studytracker.data.database.entity.TaskTemplate::class.java, database.taskTemplateDao().getAllTaskTemplates()) { items -> items.forEach { database.taskTemplateDao().insert(it) } }
            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            throw e
        }
    }

    private suspend fun <T : Any> syncTable(
        uid: String,
        collectionName: String,
        clazz: Class<T>,
        localItems: List<T>,
        insertLocal: suspend (List<T>) -> Unit
    ) {
        val collRef = db.collection("users").document(uid).collection(collectionName)
        val remoteSnapshot = collRef.get().await()
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
            val batch = db.batch()
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = db.collection("users").document(uid).collection("devices").addSnapshotListener { snapshot, error ->
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            db.collection("users").document(uid).collection("devices").document(deviceId).delete().await()
            Log.d(TAG, "Device $deviceId removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove device $deviceId", e)
        }
    }
}
