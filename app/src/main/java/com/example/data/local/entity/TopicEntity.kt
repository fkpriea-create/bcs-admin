package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    indices = [Index(value = ["subjectId"])]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val name: String,
    val description: String = "",
    val driveDocUrl: String = "",
    val orderIndex: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
