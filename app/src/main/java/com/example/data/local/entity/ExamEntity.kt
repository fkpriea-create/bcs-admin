package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exams",
    indices = [Index(value = ["subjectId"]), Index(value = ["topicId"])]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val subjectId: String = "",
    val topicId: String = "",
    val durationMinutes: Int = 0,
    val questionTimerSeconds: Int = 0,
    val negativeMarking: Double = 0.0,
    val difficulty: String = "MEDIUM",
    val status: String = "DRAFT",
    val availableFrom: Long = 0L,
    val availableTo: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
