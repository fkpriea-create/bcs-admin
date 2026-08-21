package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdminTopBar
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.subjects.SubjectEditorDialog
import com.example.ui.screens.subjects.SubjectsManagerScreen
import com.example.ui.screens.subjects.TopicEditorDialog
import com.example.ui.screens.tasks.TaskEditorDialog
import com.example.ui.screens.tasks.TaskStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BcsAdminViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.auth.LoginScreen

import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.Quiz

enum class AdminTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    TASKS("Tasks", Icons.Filled.Assignment, Icons.Outlined.Assignment, "nav_tasks"),
    SUBJECTS("Syllabus", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_subjects"),
    EXAMS("Exams", Icons.Filled.Quiz, Icons.Outlined.Quiz, "nav_exams"),
    ANALYTICS("Analytics", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_analytics")
}

class MainActivity : ComponentActivity() {
    private val viewModel: BcsAdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
                val authError by viewModel.authError.collectAsStateWithLifecycle()
                val context = LocalContext.current

                if (currentUser == null) {
                    LoginScreen(
                        isLoading = isAuthLoading,
                        errorMessage = authError,
                        onGoogleSignInClick = { viewModel.signInWithGoogle(context) },
                        onDismissError = { viewModel.clearAuthError() }
                    )
                } else {
                    BcsAdminApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BcsAdminApp(viewModel: BcsAdminViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    // Global quick action modals
    var showQuickTaskEditor by remember { mutableStateOf(false) }
    var showQuickSubjectEditor by remember { mutableStateOf(false) }
    var showQuickTopicEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AdminTopBar(
                syncState = syncState,
                currentUser = currentUser,
                onResetWorkspace = { viewModel.resetWorkspace() },
                onSignOut = { viewModel.signOut() }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("admin_bottom_navigation")
            ) {
                AdminTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            AdminTab.DASHBOARD -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { selectedTab = AdminTab.TASKS },
                    onNavigateToSubjects = { selectedTab = AdminTab.SUBJECTS },
                    onNavigateToAnalytics = { selectedTab = AdminTab.ANALYTICS },
                    onOpenTaskEditor = { showQuickTaskEditor = true },
                    onOpenSubjectEditor = { showQuickSubjectEditor = true },
                    onOpenTopicEditor = { showQuickTopicEditor = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AdminTab.TASKS -> {
                TaskStudioScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AdminTab.SUBJECTS -> {
                SubjectsManagerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AdminTab.EXAMS -> {
                com.example.ui.screens.exams.ExamManagerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AdminTab.ANALYTICS -> {
                AnalyticsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Global Modal Dialogs from Dashboard Quick Actions
    if (showQuickTaskEditor) {
        TaskEditorDialog(
            subjects = subjects,
            topics = topics,
            onDismiss = { showQuickTaskEditor = false },
            onSaveTask = { id, title, desc, subjId, topId, dueDate, dueTime, rep, driveUrl, driveLabel, priority ->
                viewModel.publishTask(
                    id = id,
                    title = title,
                    description = desc,
                    subjectId = subjId,
                    topicId = topId,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    repeatSchedule = rep,
                    googleDriveUrl = driveUrl,
                    googleDriveLabel = driveLabel,
                    priority = priority,
                    onSuccess = { showQuickTaskEditor = false }
                )
            }
        )
    }

    if (showQuickSubjectEditor) {
        SubjectEditorDialog(
            onDismiss = { showQuickSubjectEditor = false },
            onSaveSubject = { id, name, code, colorHex, iconName, driveUrl, desc ->
                viewModel.saveSubject(
                    id = id,
                    name = name,
                    code = code,
                    colorHex = colorHex,
                    iconName = iconName,
                    driveFolderUrl = driveUrl,
                    description = desc,
                    onSuccess = { showQuickSubjectEditor = false }
                )
            }
        )
    }

    if (showQuickTopicEditor) {
        TopicEditorDialog(
            subjects = subjects,
            onDismiss = { showQuickTopicEditor = false },
            onSaveTopic = { id, subjId, name, desc, driveDocUrl, order ->
                viewModel.saveTopic(
                    id = id,
                    subjectId = subjId,
                    name = name,
                    description = desc,
                    driveDocUrl = driveDocUrl,
                    orderIndex = order,
                    onSuccess = { showQuickTopicEditor = false }
                )
            }
        )
    }
}
