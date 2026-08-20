package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector

object BcsIconHelper {
    val AVAILABLE_ICONS = listOf(
        "menu_book" to "Book",
        "public" to "International",
        "translate" to "Language",
        "calculate" to "Math",
        "science" to "Science",
        "computer" to "Computer",
        "history_edu" to "Literature",
        "psychology" to "Mental Ability",
        "school" to "Education",
        "assignment" to "Task"
    )

    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "menu_book", "book" -> Icons.Default.MenuBook
            "public", "globe" -> Icons.Default.Public
            "translate", "language" -> Icons.Default.Translate
            "calculate", "math" -> Icons.Default.Calculate
            "science", "lab" -> Icons.Default.Science
            "computer", "it" -> Icons.Default.Computer
            "history_edu", "literature" -> Icons.Default.HistoryEdu
            "psychology", "mental" -> Icons.Default.Psychology
            "school" -> Icons.Default.School
            "assignment" -> Icons.Default.Assignment
            else -> Icons.Default.Description
        }
    }
}
