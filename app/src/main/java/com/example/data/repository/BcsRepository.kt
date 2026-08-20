package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskCompletionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Synced(val timestamp: Long = System.currentTimeMillis()) : SyncState()
    data class Offline(val reason: String = "Local Mode") : SyncState()
    data class Error(val message: String) : SyncState()
}

class BcsRepository(
    private val database: AppDatabase,
    private val firestoreManager: FirestoreManager,
    private val scope: CoroutineScope
) {
    private val TAG = "BcsRepository"

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Room reactive streams
    val subjects: Flow<List<SubjectEntity>> = database.subjectDao().getAllSubjects()
    val topics: Flow<List<TopicEntity>> = database.topicDao().getAllTopics()
    val tasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()
    val exams: Flow<List<ExamEntity>> = database.examDao().getAllExams()
    val users: Flow<List<UserEntity>> = database.userDao().getAllUsers()
    val completions: Flow<List<TaskCompletionEntity>> = database.taskCompletionDao().getAllCompletions()
    val operationLogs = firestoreManager.operationLogs

    fun getQuestionsForExam(examId: String): Flow<List<QuestionEntity>> = database.questionDao().getQuestionsForExam(examId)

    init {
        startSyncListeners()
    }

    private fun startSyncListeners() {
        if (!firestoreManager.isAvailable) {
            _syncState.value = SyncState.Offline("Running with local Room Cache")
            return
        }

        _syncState.value = SyncState.Syncing

        // Listen to Firestore Subjects and write into Room Cache
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeSubjects().collect { list ->
                    if (list.isNotEmpty()) {
                        database.subjectDao().insertSubjects(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Subjects listener issue: ${e.message}")
            }
        }

        // Listen to Firestore Topics
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeTopics().collect { list ->
                    if (list.isNotEmpty()) {
                        database.topicDao().insertTopics(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Topics listener issue: ${e.message}")
            }
        }

        // Listen to Firestore Tasks
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeTasks().collect { list ->
                    if (list.isNotEmpty()) {
                        database.taskDao().insertTasks(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Tasks listener issue: ${e.message}")
            }
        }

        // Listen to Firestore Users
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeUsers().collect { list ->
                    if (list.isNotEmpty()) {
                        database.userDao().insertUsers(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Users listener issue: ${e.message}")
            }
        }

        // Listen to Firestore Task Completions
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeTaskCompletions().collect { list ->
                    if (list.isNotEmpty()) {
                        database.taskCompletionDao().insertCompletions(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Completions listener issue: ${e.message}")
            }
        }

        // Listen to Firestore Exams
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeExams().collect { list ->
                    if (list.isNotEmpty()) {
                        database.examDao().insertExams(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Exams listener issue: ${e.message}")
            }
        }

        // Listen to Firestore Questions
        scope.launch(Dispatchers.IO) {
            try {
                firestoreManager.observeQuestions().collect { list ->
                    if (list.isNotEmpty()) {
                        database.questionDao().insertQuestions(list)
                        _syncState.value = SyncState.Synced()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Questions listener issue: ${e.message}")
            }
        }
    }

    // Task Operations
    suspend fun saveTask(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        // Save to Room Cache immediately
        database.taskDao().insertTask(task)
        // Push to Firestore
        val remoteOk = firestoreManager.saveTask(task)
        if (remoteOk) {
            _syncState.value = SyncState.Synced()
        }
        true
    }

    suspend fun deleteTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        database.taskDao().deleteTaskById(taskId)
        firestoreManager.deleteTask(taskId)
        true
    }

    // Subject Operations
    suspend fun saveSubject(subject: SubjectEntity): Boolean = withContext(Dispatchers.IO) {
        database.subjectDao().insertSubject(subject)
        val remoteOk = firestoreManager.saveSubject(subject)
        if (remoteOk) {
            _syncState.value = SyncState.Synced()
        }
        true
    }

    suspend fun deleteSubject(subjectId: String): Boolean = withContext(Dispatchers.IO) {
        database.subjectDao().deleteSubjectById(subjectId)
        database.topicDao().deleteTopicsBySubjectId(subjectId)
        firestoreManager.deleteSubject(subjectId)
        true
    }

    // Topic Operations
    suspend fun saveTopic(topic: TopicEntity): Boolean = withContext(Dispatchers.IO) {
        database.topicDao().insertTopic(topic)
        val remoteOk = firestoreManager.saveTopic(topic)
        if (remoteOk) {
            _syncState.value = SyncState.Synced()
        }
        true
    }

    suspend fun deleteTopic(topicId: String): Boolean = withContext(Dispatchers.IO) {
        database.topicDao().deleteTopicById(topicId)
        firestoreManager.deleteTopic(topicId)
        true
    }

    // User Operations
    suspend fun deleteUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        database.userDao().deleteUserById(userId)
        firestoreManager.deleteUser(userId)
        true
    }

    suspend fun getExamById(id: String): ExamEntity? = withContext(Dispatchers.IO) {
        database.examDao().getExamById(id)
    }

    suspend fun saveFullExam(exam: ExamEntity, questions: List<QuestionEntity>): Boolean = withContext(Dispatchers.IO) {
        database.examDao().insertExam(exam)
        database.questionDao().deleteQuestionsByExamId(exam.id)
        database.questionDao().insertQuestions(questions)
        
        val remoteExamOk = firestoreManager.saveExam(exam)
        questions.forEach { q -> firestoreManager.saveQuestion(q) }
        
        if (remoteExamOk) _syncState.value = SyncState.Synced()
        true
    }

    suspend fun updateExamStatus(examId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val exam = database.examDao().getExamById(examId)
        if (exam != null) {
            val updated = exam.copy(status = newStatus)
            database.examDao().updateExam(updated)
            firestoreManager.saveExam(updated)
            
            // Also push questions to Firestore if we are publishing, just to ensure they are available
            if (newStatus == "PUBLISHED") {
                val questions = database.questionDao().getQuestionsForExamSync(examId)
                questions.forEach { firestoreManager.saveQuestion(it) }
            }
            
            _syncState.value = SyncState.Synced()
            return@withContext true
        }
        false
    }
    
    suspend fun duplicateExam(examId: String): String? = withContext(Dispatchers.IO) {
        val exam = database.examDao().getExamById(examId) ?: return@withContext null
        val newExamId = java.util.UUID.randomUUID().toString()
        val duplicatedExam = exam.copy(
            id = newExamId,
            title = "${exam.title} (Copy)",
            status = "DRAFT",
            createdAt = System.currentTimeMillis()
        )
        val questions = database.questionDao().getQuestionsForExamSync(examId).map {
            it.copy(
                id = java.util.UUID.randomUUID().toString(),
                examId = newExamId
            )
        }
        
        saveFullExam(duplicatedExam, questions)
        newExamId
    }
    
    // Exam Operations
    suspend fun saveExam(exam: ExamEntity): Boolean = withContext(Dispatchers.IO) {
        database.examDao().insertExam(exam)
        val remoteOk = firestoreManager.saveExam(exam)
        if (remoteOk) _syncState.value = SyncState.Synced()
        true
    }

    suspend fun deleteExam(examId: String): Boolean = withContext(Dispatchers.IO) {
        database.examDao().deleteExamById(examId)
        database.questionDao().deleteQuestionsByExamId(examId)
        firestoreManager.deleteExam(examId)
        // Note: we should ideally delete remote questions too
        true
    }

    // Question Operations
    fun observeExamResults(examId: String) = firestoreManager.observeExamResults(examId)
    suspend fun getQuestionsForExamSync(examId: String): List<QuestionEntity> = withContext(Dispatchers.IO) {
        database.questionDao().getQuestionsForExamSync(examId)
    }

    suspend fun saveQuestion(question: QuestionEntity): Boolean = withContext(Dispatchers.IO) {
        database.questionDao().insertQuestion(question)
        val remoteOk = firestoreManager.saveQuestion(question)
        if (remoteOk) _syncState.value = SyncState.Synced()
        true
    }

    suspend fun deleteQuestion(questionId: String): Boolean = withContext(Dispatchers.IO) {
        database.questionDao().deleteQuestionById(questionId)
        firestoreManager.deleteQuestion(questionId)
        true
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        database.subjectDao().clearAll()
        database.topicDao().clearAll()
        database.taskDao().clearAll()
        database.userDao().clearAll()
        database.taskCompletionDao().clearAll()
    }
}
