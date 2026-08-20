package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskCompletionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

enum class LogStatus {
    SUCCESS,
    QUEUED_OFFLINE,
    PERMISSION_DENIED,
    ERROR
}

data class FirestoreLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    val collection: String,
    val documentId: String,
    val status: LogStatus,
    val details: String,
    val errorCode: String? = null
)

class FirestoreManager(private val context: Context? = null) {
    private val TAG = "FirestoreManager"

    private val _operationLogs = MutableStateFlow<List<FirestoreLogEntry>>(emptyList())
    val operationLogs: StateFlow<List<FirestoreLogEntry>> = _operationLogs.asStateFlow()

    private fun addLog(entry: FirestoreLogEntry) {
        val updated = (_operationLogs.value + entry).takeLast(100)
        _operationLogs.value = updated
    }

    private fun safeLong(doc: DocumentSnapshot, fieldName: String, defaultVal: Long = 0L): Long {
        return try {
            val raw = doc.get(fieldName) ?: return defaultVal
            when (raw) {
                is Number -> raw.toLong()
                is com.google.firebase.Timestamp -> raw.toDate().time
                is String -> raw.toLongOrNull() ?: defaultVal
                is Date -> raw.time
                else -> defaultVal
            }
        } catch (e: Throwable) {
            defaultVal
        }
    }

    private fun safeInt(doc: DocumentSnapshot, fieldName: String, defaultVal: Int = 0): Int {
        return try {
            val raw = doc.get(fieldName) ?: return defaultVal
            when (raw) {
                is Number -> raw.toInt()
                is com.google.firebase.Timestamp -> (raw.toDate().time / 1000).toInt()
                is String -> raw.toIntOrNull() ?: defaultVal
                else -> defaultVal
            }
        } catch (e: Throwable) {
            defaultVal
        }
    }

    private fun safeString(doc: DocumentSnapshot, primaryField: String, vararg fallbackFields: String, defaultVal: String = ""): String {
        try {
            val primary = doc.getString(primaryField)
            if (!primary.isNullOrBlank()) return primary
            for (fallback in fallbackFields) {
                val str = doc.getString(fallback)
                if (!str.isNullOrBlank()) return str
            }
            val raw = doc.get(primaryField)
            if (raw != null) return raw.toString()
        } catch (e: Throwable) {}
        return defaultVal
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val app = if (context != null) {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    try {
                        FirebaseApp.initializeApp(context)
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    FirebaseApp.getInstance()
                }
            } else {
                try {
                    FirebaseApp.getInstance()
                } catch (e: Throwable) {
                    null
                }
            }
            if (app != null) {
                val db = FirebaseFirestore.getInstance(app)
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build()
                    db.firestoreSettings = settings
                } catch (ignored: Throwable) {}
                db
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Firestore initialization check: ${e.message}")
            null
        }
    }

    val isAvailable: Boolean
        get() = firestore != null

    // Real-time subjects flow
    fun observeSubjects(): Flow<List<SubjectEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration: ListenerRegistration = db.collection("subjects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "subjects",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for subjects: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            SubjectEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                name = safeString(doc, "name", "title", "subjectName"),
                                code = safeString(doc, "code"),
                                colorHex = safeString(doc, "colorHex", "color", defaultVal = "#3B82F6"),
                                iconName = safeString(doc, "iconName", "icon", defaultVal = "menu_book"),
                                driveFolderUrl = safeString(doc, "driveFolderUrl", "driveUrl"),
                                description = safeString(doc, "description"),
                                updatedAt = safeLong(doc, "updatedAt", System.currentTimeMillis())
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping subject doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    // Real-time topics flow
    fun observeTopics(): Flow<List<TopicEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("topics")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "topics",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for topics: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            TopicEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                subjectId = safeString(doc, "subjectId"),
                                name = safeString(doc, "name", "title", "topicName"),
                                description = safeString(doc, "description"),
                                driveDocUrl = safeString(doc, "driveDocUrl", "driveUrl", "driveFolderUrl"),
                                orderIndex = safeInt(doc, "orderIndex", safeInt(doc, "order", 0)),
                                updatedAt = safeLong(doc, "updatedAt", System.currentTimeMillis())
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping topic doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    // Real-time tasks flow
    fun observeTasks(): Flow<List<TaskEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "tasks",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for tasks: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            TaskEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                title = safeString(doc, "title", "name"),
                                description = safeString(doc, "description"),
                                subjectId = safeString(doc, "subjectId"),
                                topicId = safeString(doc, "topicId"),
                                dueDate = safeString(doc, "dueDate"),
                                dueTime = safeString(doc, "dueTime"),
                                repeatSchedule = safeString(doc, "repeatSchedule", defaultVal = "NONE"),
                                googleDriveUrl = safeString(doc, "googleDriveUrl", "driveUrl"),
                                googleDriveLabel = safeString(doc, "googleDriveLabel", defaultVal = "BCS Study Material"),
                                priority = safeString(doc, "priority", defaultVal = "MEDIUM"),
                                createdAt = safeLong(doc, "createdAt", System.currentTimeMillis())
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping task doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    // Real-time exams flow
    fun observeExams(): Flow<List<ExamEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("exams")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "exams",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for exams: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            ExamEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                title = safeString(doc, "title", "name"),
                                description = safeString(doc, "description"),
                                subjectId = safeString(doc, "subjectId"),
                                topicId = safeString(doc, "topicId"),
                                durationMinutes = safeInt(doc, "durationMinutes"),
                                questionTimerSeconds = safeInt(doc, "questionTimerSeconds"),
                                negativeMarking = doc.getDouble("negativeMarking") ?: 0.0,
                                difficulty = safeString(doc, "difficulty", defaultVal = "MEDIUM"),
                                status = safeString(doc, "status", defaultVal = "DRAFT"),
                                availableFrom = safeLong(doc, "availableFrom"),
                                availableTo = safeLong(doc, "availableTo"),
                                createdAt = safeLong(doc, "createdAt", System.currentTimeMillis())
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping exam doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeExamResults(examId: String): Flow<List<com.example.data.local.entity.ExamResultEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("exam_results")
            .whereEqualTo("examId", examId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "exam_results",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for exam_results: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val score = doc.getDouble("score") ?: doc.getDouble("marks") ?: doc.getDouble("totalMarks") ?: 0.0
                            val totalMarks = doc.getDouble("totalMarks") ?: doc.getDouble("total_marks") ?: 0.0
                            val timeTaken = doc.getLong("timeTakenSeconds")?.toInt() ?: doc.getLong("time_taken_seconds")?.toInt() ?: 0
                            val submittedAt = doc.getLong("submittedAt") ?: doc.getLong("timestamp") ?: doc.getLong("createdAt") ?: doc.getDate("timestamp")?.time ?: 0L
                            com.example.data.local.entity.ExamResultEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                examId = safeString(doc, "examId", "exam_id"),
                                userId = safeString(doc, "userId", "user_id"),
                                studentName = safeString(doc, "studentName", "student_name", "userName", defaultVal = "Unknown"),
                                score = score,
                                totalMarks = totalMarks,
                                timeTakenSeconds = timeTaken,
                                submittedAt = submittedAt
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping exam_result doc ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedWith(compareByDescending<com.example.data.local.entity.ExamResultEntity> { it.score }.thenBy { it.timeTakenSeconds })
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }
    // Real-time questions flow
    fun observeQuestions(): Flow<List<QuestionEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("questions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "questions",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for questions: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            QuestionEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                examId = safeString(doc, "examId"),
                                questionText = safeString(doc, "questionText"),
                                optionA = safeString(doc, "optionA"),
                                optionB = safeString(doc, "optionB"),
                                optionC = safeString(doc, "optionC"),
                                optionD = safeString(doc, "optionD"),
                                correctOption = safeString(doc, "correctOption"),
                                explanation = safeString(doc, "explanation"),
                                marks = safeInt(doc, "marks", 1),
                                orderIndex = safeInt(doc, "orderIndex")
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping question doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    // Real-time users flow (Students)
    fun observeUsers(): Flow<List<UserEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "users",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for users: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            UserEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                name = safeString(doc, "name", defaultVal = "BCS Aspirant"),
                                email = safeString(doc, "email"),
                                role = safeString(doc, "role", defaultVal = "STUDENT"),
                                streakDays = safeInt(doc, "streakDays", 0),
                                lastActiveDate = safeString(doc, "lastActiveDate")
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping user doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    // Real-time task completions flow
    fun observeTaskCompletions(): Flow<List<TaskCompletionEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("task_completions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isPerm = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
                    addLog(
                        FirestoreLogEntry(
                            operation = "OBSERVE",
                            collection = "task_completions",
                            documentId = "*",
                            status = status,
                            details = "Listener error: ${error.message}",
                            errorCode = error.code.name
                        )
                    )
                    Log.w(TAG, "[OBSERVE ${status.name}] Listen failed for task_completions: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            TaskCompletionEntity(
                                id = safeString(doc, "id", defaultVal = doc.id),
                                taskId = safeString(doc, "taskId"),
                                userId = safeString(doc, "userId"),
                                studentName = safeString(doc, "studentName"),
                                completedAt = safeLong(doc, "completedAt", System.currentTimeMillis()),
                                completionDate = safeString(doc, "completionDate"),
                                isCompleted = doc.getBoolean("isCompleted") ?: true
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error mapping completion doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    // Firestore Writes using non-blocking local-first queueing with robust logging
    private suspend fun safeSet(collectionName: String, docId: String, map: Map<String, Any?>): Boolean {
        val db = firestore
        if (db == null) {
            val entry = FirestoreLogEntry(
                operation = "WRITE_SET",
                collection = collectionName,
                documentId = docId,
                status = LogStatus.ERROR,
                details = "Firestore instance is null / uninitialized",
                errorCode = "CLIENT_UNINITIALIZED"
            )
            addLog(entry)
            Log.e(TAG, "[WRITE ERROR] Firestore unavailable for $collectionName/$docId")
            return false
        }

        val startTime = System.currentTimeMillis()
        return try {
            val task = db.collection(collectionName).document(docId).set(map, SetOptions.merge())
            val completed = kotlinx.coroutines.withTimeoutOrNull(3500) {
                task.await()
                true
            } ?: false

            val duration = System.currentTimeMillis() - startTime
            val status = if (completed) LogStatus.SUCCESS else LogStatus.QUEUED_OFFLINE
            val details = if (completed) {
                "Confirmed in cloud (${duration}ms, fields: ${map.keys})"
            } else {
                "Queued in local cache, syncing in background (${duration}ms)"
            }

            addLog(
                FirestoreLogEntry(
                    operation = "WRITE_SET",
                    collection = collectionName,
                    documentId = docId,
                    status = status,
                    details = details
                )
            )
            Log.i(TAG, "[WRITE ${status.name}] $collectionName/$docId: $details")
            true
        } catch (e: FirebaseFirestoreException) {
            val isPerm = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
            val details = if (isPerm) {
                "Write rejected by Firestore Security Rules for $collectionName/$docId: ${e.message}"
            } else {
                "FirebaseFirestoreException (${e.code}): ${e.message}"
            }
            addLog(
                FirestoreLogEntry(
                    operation = "WRITE_SET",
                    collection = collectionName,
                    documentId = docId,
                    status = status,
                    details = details,
                    errorCode = e.code.name
                )
            )
            Log.e(TAG, "[WRITE ${status.name}] $collectionName/$docId: $details", e)
            !isPerm
        } catch (e: Throwable) {
            addLog(
                FirestoreLogEntry(
                    operation = "WRITE_SET",
                    collection = collectionName,
                    documentId = docId,
                    status = LogStatus.QUEUED_OFFLINE,
                    details = "Write queued in local cache: ${e.message}",
                    errorCode = e.javaClass.simpleName
                )
            )
            Log.w(TAG, "[WRITE QUEUED] $collectionName/$docId cached locally: ${e.message}")
            true
        }
    }

    private suspend fun safeDelete(collectionName: String, docId: String): Boolean {
        val db = firestore
        if (db == null) {
            val entry = FirestoreLogEntry(
                operation = "DELETE",
                collection = collectionName,
                documentId = docId,
                status = LogStatus.ERROR,
                details = "Firestore instance is null / uninitialized",
                errorCode = "CLIENT_UNINITIALIZED"
            )
            addLog(entry)
            Log.e(TAG, "[DELETE ERROR] Firestore unavailable for $collectionName/$docId")
            return false
        }

        val startTime = System.currentTimeMillis()
        return try {
            val task = db.collection(collectionName).document(docId).delete()
            val completed = kotlinx.coroutines.withTimeoutOrNull(3500) {
                task.await()
                true
            } ?: false

            val duration = System.currentTimeMillis() - startTime
            val status = if (completed) LogStatus.SUCCESS else LogStatus.QUEUED_OFFLINE
            val details = if (completed) {
                "Deleted from cloud (${duration}ms)"
            } else {
                "Delete queued in offline cache, syncing in background (${duration}ms)"
            }

            addLog(
                FirestoreLogEntry(
                    operation = "DELETE",
                    collection = collectionName,
                    documentId = docId,
                    status = status,
                    details = details
                )
            )
            Log.i(TAG, "[DELETE ${status.name}] $collectionName/$docId: $details")
            true
        } catch (e: FirebaseFirestoreException) {
            val isPerm = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            val status = if (isPerm) LogStatus.PERMISSION_DENIED else LogStatus.ERROR
            val details = if (isPerm) {
                "Delete rejected by Firestore Security Rules for $collectionName/$docId (Requires Admin permissions): ${e.message}"
            } else {
                "FirebaseFirestoreException (${e.code}): ${e.message}"
            }
            addLog(
                FirestoreLogEntry(
                    operation = "DELETE",
                    collection = collectionName,
                    documentId = docId,
                    status = status,
                    details = details,
                    errorCode = e.code.name
                )
            )
            Log.e(TAG, "[DELETE ${status.name}] $collectionName/$docId: $details", e)
            !isPerm
        } catch (e: Throwable) {
            addLog(
                FirestoreLogEntry(
                    operation = "DELETE",
                    collection = collectionName,
                    documentId = docId,
                    status = LogStatus.QUEUED_OFFLINE,
                    details = "Delete queued in local cache: ${e.message}",
                    errorCode = e.javaClass.simpleName
                )
            )
            Log.w(TAG, "[DELETE QUEUED] $collectionName/$docId cached locally: ${e.message}")
            true
        }
    }

    suspend fun saveSubject(subject: SubjectEntity): Boolean {
        val map = hashMapOf(
            "id" to subject.id,
            "subjectId" to subject.id,
            "subject_id" to subject.id,
            "name" to subject.name,
            "title" to subject.name,
            "subjectName" to subject.name,
            "code" to subject.code,
            "subjectCode" to subject.code,
            "colorHex" to subject.colorHex,
            "color" to subject.colorHex,
            "colorCode" to subject.colorHex,
            "iconName" to subject.iconName,
            "icon" to subject.iconName,
            "driveFolderUrl" to subject.driveFolderUrl,
            "driveUrl" to subject.driveFolderUrl,
            "driveLink" to subject.driveFolderUrl,
            "folderUrl" to subject.driveFolderUrl,
            "description" to subject.description,
            "desc" to subject.description,
            "isActive" to true,
            "active" to true,
            "isPublished" to true,
            "published" to true,
            "updatedAt" to subject.updatedAt,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "createdTimestamp" to com.google.firebase.Timestamp(Date(subject.updatedAt))
        )
        return safeSet("subjects", subject.id, map)
    }

    suspend fun deleteSubject(subjectId: String): Boolean {
        return safeDelete("subjects", subjectId)
    }

    suspend fun saveTopic(topic: TopicEntity): Boolean {
        val map = hashMapOf(
            "id" to topic.id,
            "topicId" to topic.id,
            "topic_id" to topic.id,
            "subjectId" to topic.subjectId,
            "subject_id" to topic.subjectId,
            "subject" to topic.subjectId,
            "name" to topic.name,
            "title" to topic.name,
            "topicName" to topic.name,
            "description" to topic.description,
            "desc" to topic.description,
            "driveDocUrl" to topic.driveDocUrl,
            "driveFolderUrl" to topic.driveDocUrl,
            "driveUrl" to topic.driveDocUrl,
            "driveLink" to topic.driveDocUrl,
            "docUrl" to topic.driveDocUrl,
            "orderIndex" to topic.orderIndex,
            "order" to topic.orderIndex,
            "index" to topic.orderIndex,
            "isActive" to true,
            "active" to true,
            "isPublished" to true,
            "published" to true,
            "updatedAt" to topic.updatedAt,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "createdTimestamp" to com.google.firebase.Timestamp(Date(topic.updatedAt))
        )
        return safeSet("topics", topic.id, map)
    }

    suspend fun deleteTopic(topicId: String): Boolean {
        return safeDelete("topics", topicId)
    }

    suspend fun saveTask(task: TaskEntity): Boolean {
        val map = hashMapOf(
            // Primary Identifiers
            "id" to task.id,
            "taskId" to task.id,
            "task_id" to task.id,

            // Titles & Descriptions
            "title" to task.title,
            "name" to task.title,
            "taskTitle" to task.title,
            "taskName" to task.title,
            "heading" to task.title,
            "description" to task.description,
            "desc" to task.description,
            "details" to task.description,

            // Relational IDs
            "subjectId" to task.subjectId,
            "subject_id" to task.subjectId,
            "subject" to task.subjectId,
            "topicId" to task.topicId,
            "topic_id" to task.topicId,
            "topic" to task.topicId,

            // Dates & Times
            "dueDate" to task.dueDate,
            "due_date" to task.dueDate,
            "date" to task.dueDate,
            "deadline" to task.dueDate,
            "dueTime" to task.dueTime,
            "due_time" to task.dueTime,
            "time" to task.dueTime,

            // Schedule & Priority
            "repeatSchedule" to task.repeatSchedule,
            "repeat" to task.repeatSchedule,
            "schedule" to task.repeatSchedule,
            "frequency" to task.repeatSchedule,
            "priority" to task.priority,
            "priorityLevel" to task.priority,
            "taskPriority" to task.priority,

            // Drive Resource Links
            "googleDriveUrl" to task.googleDriveUrl,
            "driveUrl" to task.googleDriveUrl,
            "driveLink" to task.googleDriveUrl,
            "googleDriveLink" to task.googleDriveUrl,
            "pdfUrl" to task.googleDriveUrl,
            "materialUrl" to task.googleDriveUrl,
            "link" to task.googleDriveUrl,
            "url" to task.googleDriveUrl,
            "googleDriveLabel" to task.googleDriveLabel,
            "driveLabel" to task.googleDriveLabel,
            "linkLabel" to task.googleDriveLabel,
            "materialLabel" to task.googleDriveLabel,

            // Status & Filter Attributes (Essential for User App queries)
            "isCompleted" to false,
            "completed" to false,
            "isDone" to false,
            "done" to false,
            "status" to "ACTIVE",
            "taskStatus" to "PENDING",
            "isPublished" to true,
            "published" to true,
            "isActive" to true,
            "active" to true,
            "visible" to true,

            // Timestamps
            "createdAt" to task.createdAt,
            "updatedAt" to task.createdAt,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "createdTimestamp" to com.google.firebase.Timestamp(Date(task.createdAt))
        )
        return safeSet("tasks", task.id, map)
    }

    suspend fun deleteTask(taskId: String): Boolean {
        return safeDelete("tasks", taskId)
    }

    suspend fun saveUser(user: UserEntity): Boolean {
        val map = hashMapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role,
            "streakDays" to user.streakDays,
            "lastActiveDate" to user.lastActiveDate
        )
        return safeSet("users", user.id, map)
    }

    suspend fun deleteUser(userId: String): Boolean {
        return safeDelete("users", userId)
    }

    suspend fun saveTaskCompletion(completion: TaskCompletionEntity): Boolean {
        val map = hashMapOf(
            "id" to completion.id,
            "taskId" to completion.taskId,
            "userId" to completion.userId,
            "studentName" to completion.studentName,
            "completedAt" to completion.completedAt,
            "completionDate" to completion.completionDate,
            "isCompleted" to completion.isCompleted
        )
        return safeSet("task_completions", completion.id, map)
    }

    suspend fun saveExam(exam: ExamEntity): Boolean {
        val isPublished = exam.status == "PUBLISHED"
        val map = hashMapOf(
            "id" to exam.id,
            "examId" to exam.id,
            "exam_id" to exam.id,
            "title" to exam.title,
            "name" to exam.title,
            "examTitle" to exam.title,
            "examName" to exam.title,
            "description" to exam.description,
            "desc" to exam.description,
            "subjectId" to exam.subjectId,
            "subject_id" to exam.subjectId,
            "subject" to exam.subjectId,
            "topicId" to exam.topicId,
            "topic_id" to exam.topicId,
            "topic" to exam.topicId,
            "durationMinutes" to exam.durationMinutes,
            "duration" to exam.durationMinutes,
            "time" to exam.durationMinutes,
            "questionTimerSeconds" to exam.questionTimerSeconds,
            "negativeMarking" to exam.negativeMarking,
            "negative_marking" to exam.negativeMarking,
            "negativeMark" to exam.negativeMarking,
            "difficulty" to exam.difficulty,
            "status" to exam.status,
            "examStatus" to exam.status,
            "isPublished" to isPublished,
            "published" to isPublished,
            "isActive" to isPublished,
            "active" to isPublished,
            "visible" to isPublished,
            "availableFrom" to exam.availableFrom,
            "availableTo" to exam.availableTo,
            "createdAt" to exam.createdAt,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "createdTimestamp" to com.google.firebase.Timestamp(Date(exam.createdAt))
        )
        return safeSet("exams", exam.id, map)
    }

    suspend fun deleteExam(examId: String): Boolean {
        return safeDelete("exams", examId)
    }

    suspend fun saveQuestion(question: QuestionEntity): Boolean {
        val map = hashMapOf(
            "id" to question.id,
            "questionId" to question.id,
            "question_id" to question.id,
            "examId" to question.examId,
            "exam_id" to question.examId,
            "exam" to question.examId,
            "questionText" to question.questionText,
            "text" to question.questionText,
            "question" to question.questionText,
            "optionA" to question.optionA,
            "option_a" to question.optionA,
            "a" to question.optionA,
            "optionB" to question.optionB,
            "option_b" to question.optionB,
            "b" to question.optionB,
            "optionC" to question.optionC,
            "option_c" to question.optionC,
            "c" to question.optionC,
            "optionD" to question.optionD,
            "option_d" to question.optionD,
            "d" to question.optionD,
            "correctOption" to question.correctOption,
            "correct_option" to question.correctOption,
            "answer" to question.correctOption,
            "correctAnswer" to question.correctOption,
            "explanation" to question.explanation,
            "marks" to question.marks,
            "orderIndex" to question.orderIndex,
            "index" to question.orderIndex,
            "order" to question.orderIndex
        )
        return safeSet("questions", question.id, map)
    }

    suspend fun deleteQuestion(questionId: String): Boolean {
        return safeDelete("questions", questionId)
    }
}
