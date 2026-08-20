package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    indices = [Index(value = ["examId"])]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String, // "A", "B", "C", "D"
    val explanation: String = "",
    val marks: Int = 1,
    val orderIndex: Int = 0
)
