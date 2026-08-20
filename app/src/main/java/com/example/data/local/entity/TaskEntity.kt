package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["subjectId"]), Index(value = ["topicId"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val subjectId: String,
    val topicId: String,
    val dueDate: String,
    val dueTime: String,
    val repeatSchedule: String = "NONE",
    val googleDriveUrl: String = "",
    val googleDriveLabel: String = "BCS Study Material",
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis()
)
