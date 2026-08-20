package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TaskCompletionDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.ExamDao
import com.example.data.local.dao.QuestionDao
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskCompletionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.QuestionEntity

@Database(
    entities = [
        SubjectEntity::class,
        TopicEntity::class,
        TaskEntity::class,
        UserEntity::class,
        TaskCompletionEntity::class,
        ExamEntity::class,
        QuestionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun examDao(): ExamDao
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bcs_admin_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
