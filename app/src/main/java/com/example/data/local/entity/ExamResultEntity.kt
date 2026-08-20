package com.example.data.local.entity

data class ExamResultEntity(
    val id: String = "",
    val examId: String = "",
    val userId: String = "",
    val studentName: String = "",
    val score: Double = 0.0,
    val totalMarks: Double = 0.0,
    val timeTakenSeconds: Int = 0,
    val submittedAt: Long = 0L
)
