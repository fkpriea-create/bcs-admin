package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_completions",
    indices = [Index(value = ["taskId"]), Index(value = ["userId"])]
)
data class TaskCompletionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val userId: String,
    val studentName: String = "",
    val completedAt: Long = System.currentTimeMillis(),
    val completionDate: String = "",
    val isCompleted: Boolean = true
)
