package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskCompletionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.repository.BcsRepository
import com.example.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AdminAnalytics(
    val totalStudents: Int = 0,
    val activeStreaksCount: Int = 0,
    val maxStreak: Int = 0,
    val publishedTasksCount: Int = 0,
    val totalCompletionsCount: Int = 0,
    val completionRatePercentage: Int = 0,
    val totalSubjectsCount: Int = 0,
    val totalTopicsCount: Int = 0
)

data class QuestionDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "A",
    val explanation: String = "",
    val marks: Int = 1
)

class BcsAdminViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val firestoreManager = FirestoreManager(application)
    private val repository = BcsRepository(database, firestoreManager, viewModelScope)
    val authManager = com.example.data.auth.AuthManager(application)

    val currentUser = authManager.currentUser
    val isAuthLoading = authManager.isLoading
    val authError = authManager.authError

    val syncState: StateFlow<SyncState> = repository.syncState

    val subjects: StateFlow<List<SubjectEntity>> = repository.subjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<TopicEntity>> = repository.topics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = repository.exams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completions: StateFlow<List<TaskCompletionEntity>> = repository.completions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val operationLogs = repository.operationLogs

    // Filtering & Search
    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery = _taskSearchQuery.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow<String?>(null)
    val selectedSubjectFilter = _selectedSubjectFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<String?>(null)
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        tasks,
        taskSearchQuery,
        selectedSubjectFilter,
        selectedPriorityFilter
    ) { allTasks, query, subjectId, priority ->
        allTasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.googleDriveLabel.contains(query, ignoreCase = true)

            val matchesSubject = subjectId == null || task.subjectId == subjectId
            val matchesPriority = priority == null || task.priority.equals(priority, ignoreCase = true)

            matchesQuery && matchesSubject && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics calculations
    val analytics: StateFlow<AdminAnalytics> = combine(
        users,
        tasks,
        completions,
        subjects,
        topics
    ) { studentList, taskList, completionList, subjectList, topicList ->
        val totalStudents = studentList.size
        val activeStreaks = studentList.count { it.streakDays > 0 }
        val maxStreak = studentList.maxOfOrNull { it.streakDays } ?: 0
        val publishedTasks = taskList.size
        val totalCompletions = completionList.size

        val totalPossibleCompletions = (totalStudents * publishedTasks).coerceAtLeast(1)
        val completionRate = if (totalStudents > 0 && publishedTasks > 0) {
            ((totalCompletions.toDouble() / (totalStudents * publishedTasks)) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        AdminAnalytics(
            totalStudents = totalStudents,
            activeStreaksCount = activeStreaks,
            maxStreak = maxStreak,
            publishedTasksCount = publishedTasks,
            totalCompletionsCount = totalCompletions,
            completionRatePercentage = completionRate,
            totalSubjectsCount = subjectList.size,
            totalTopicsCount = topicList.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminAnalytics())

    // Search & Filter setters
    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun setSubjectFilter(subjectId: String?) {
        _selectedSubjectFilter.value = subjectId
    }

    fun setPriorityFilter(priority: String?) {
        _selectedPriorityFilter.value = priority
    }

    // Task Actions
    fun publishTask(
        id: String? = null,
        title: String,
        description: String,
        subjectId: String,
        topicId: String,
        dueDate: String,
        dueTime: String,
        repeatSchedule: String,
        googleDriveUrl: String,
        googleDriveLabel: String,
        priority: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val taskId = id ?: "task_bcs_${System.currentTimeMillis() % 100000}"
            val task = TaskEntity(
                id = taskId,
                title = title.trim(),
                description = description.trim(),
                subjectId = subjectId,
                topicId = topicId,
                dueDate = dueDate,
                dueTime = dueTime,
                repeatSchedule = repeatSchedule,
                googleDriveUrl = googleDriveUrl.trim(),
                googleDriveLabel = if (googleDriveLabel.isNotBlank()) googleDriveLabel.trim() else "BCS Study Material",
                priority = priority,
                createdAt = System.currentTimeMillis()
            )
            repository.saveTask(task)
            onSuccess()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    // Subject Actions
    fun saveSubject(
        id: String? = null,
        name: String,
        code: String,
        colorHex: String,
        iconName: String,
        driveFolderUrl: String,
        description: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanCode = code.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")
            val subjectId = id ?: "subj_${if (cleanCode.isNotBlank()) cleanCode else UUID.randomUUID().toString().take(8)}"
            val subject = SubjectEntity(
                id = subjectId,
                name = name.trim(),
                code = code.trim().uppercase(),
                colorHex = colorHex,
                iconName = iconName,
                driveFolderUrl = driveFolderUrl.trim(),
                description = description.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveSubject(subject)
            onSuccess()
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    // Topic Actions
    fun saveTopic(
        id: String? = null,
        subjectId: String,
        name: String,
        description: String,
        driveDocUrl: String,
        orderIndex: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanName = name.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").take(15)
            val topicId = id ?: "topic_${subjectId}_${if (cleanName.isNotBlank()) cleanName else UUID.randomUUID().toString().take(8)}"
            val topic = TopicEntity(
                id = topicId,
                subjectId = subjectId,
                name = name.trim(),
                description = description.trim(),
                driveDocUrl = driveDocUrl.trim(),
                orderIndex = orderIndex,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveTopic(topic)
            onSuccess()
        }
    }

    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            repository.deleteTopic(topicId)
        }
    }

    // Exam Actions
    fun getQuestionsForExam(examId: String) = repository.getQuestionsForExam(examId)
    fun observeExamResults(examId: String) = repository.observeExamResults(examId)

    fun loadExamForEditing(examId: String, onLoaded: (ExamEntity, List<QuestionDraft>) -> Unit) {
        viewModelScope.launch {
            val exam = repository.getExamById(examId) ?: return@launch
            val questions = repository.getQuestionsForExamSync(examId).map { q ->
                QuestionDraft(
                    id = q.id,
                    questionText = q.questionText,
                    optionA = q.optionA,
                    optionB = q.optionB,
                    optionC = q.optionC,
                    optionD = q.optionD,
                    correctOption = q.correctOption,
                    explanation = q.explanation,
                    marks = q.marks
                )
            }
            onLoaded(exam, questions)
        }
    }

    fun saveExam(
        id: String,
        title: String,
        description: String,
        durationMinutes: Int,
        difficulty: String,
        status: String = "DRAFT"
    ) {
        viewModelScope.launch {
            val exam = ExamEntity(
                id = id,
                title = title,
                description = description,
                durationMinutes = durationMinutes,
                difficulty = difficulty,
                status = status,
                createdAt = System.currentTimeMillis()
            )
            repository.saveExam(exam)
        }
    }

    fun saveFullExam(
        id: String,
        title: String,
        description: String,
        subjectId: String = "",
        topicId: String = "",
        durationMinutes: Int = 30,
        questionTimerSeconds: Int = 0,
        negativeMarking: Double = 0.5,
        difficulty: String = "MEDIUM",
        status: String = "DRAFT",
        questions: List<QuestionDraft>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val exam = ExamEntity(
                id = id,
                title = title.trim(),
                description = description.trim(),
                subjectId = subjectId,
                topicId = topicId,
                durationMinutes = durationMinutes,
                questionTimerSeconds = questionTimerSeconds,
                negativeMarking = negativeMarking,
                difficulty = difficulty,
                status = status,
                createdAt = System.currentTimeMillis()
            )
            
            val questionEntities = questions.mapIndexed { index, draft ->
                com.example.data.local.entity.QuestionEntity(
                    id = if (draft.id.isNotBlank()) draft.id else java.util.UUID.randomUUID().toString(),
                    examId = id,
                    questionText = draft.questionText.trim(),
                    optionA = draft.optionA.trim(),
                    optionB = draft.optionB.trim(),
                    optionC = draft.optionC.trim(),
                    optionD = draft.optionD.trim(),
                    correctOption = draft.correctOption.trim().uppercase(),
                    explanation = draft.explanation.trim(),
                    marks = draft.marks,
                    orderIndex = index
                )
            }
            
            repository.saveFullExam(exam, questionEntities)
            onComplete()
        }
    }

    fun setExamStatus(examId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateExamStatus(examId, newStatus)
        }
    }

    fun togglePublishStatus(examId: String, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = if (currentStatus.equals("PUBLISHED", ignoreCase = true)) "DRAFT" else "PUBLISHED"
            repository.updateExamStatus(examId, newStatus)
        }
    }

    fun duplicateExam(examId: String, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val newId = repository.duplicateExam(examId)
            onComplete(newId)
        }
    }
    
    fun deleteExam(examId: String) {
        viewModelScope.launch {
            repository.deleteExam(examId)
        }
    }

    // Student Roster Actions
    fun deleteStudent(userId: String) {
        viewModelScope.launch {
            repository.deleteUser(userId)
        }
    }

    // Reset workspace
    fun resetWorkspace() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }

    // Auth Actions
    fun signInWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            authManager.signInWithGoogle(context)
        }
    }

    fun signInAsAdmin() {
        authManager.signInAsAdmin()
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearAuthError() {
        authManager.clearError()
    }
}
