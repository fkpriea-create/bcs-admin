package com.example.ui.screens.exams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ExamEntity
import com.example.ui.viewmodel.BcsAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamManagerScreen(
    viewModel: BcsAdminViewModel,
    modifier: Modifier = Modifier
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle(initialValue = emptyList())
    val subjects by viewModel.subjects.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Published, 2: Drafts

    var showCreateOptions by remember { mutableStateOf(false) }
    var showManualEditor by remember { mutableStateOf(false) }
    var editingExamId by remember { mutableStateOf<String?>(null) }
    var showAiEditor by remember { mutableStateOf(false) }
    
    var previewExam by remember { mutableStateOf<ExamEntity?>(null) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }
    var examToPublishToggle by remember { mutableStateOf<ExamEntity?>(null) }
    var examForLeaderboard by remember { mutableStateOf<ExamEntity?>(null) }

    val publishedCount = exams.count { it.status.equals("PUBLISHED", ignoreCase = true) }
    val draftCount = exams.size - publishedCount

    val filteredExams = remember(exams, searchQuery, selectedFilterTab) {
        exams.filter { exam ->
            val matchesSearch = searchQuery.isBlank() ||
                    exam.title.contains(searchQuery, ignoreCase = true) ||
                    exam.description.contains(searchQuery, ignoreCase = true) ||
                    exam.difficulty.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilterTab) {
                1 -> exam.status.equals("PUBLISHED", ignoreCase = true)
                2 -> !exam.status.equals("PUBLISHED", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateOptions = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Exam")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Exam", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBox(label = "Total Exams", count = exams.size.toString(), color = MaterialTheme.colorScheme.primary)
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    StatBox(label = "Published", count = publishedCount.toString(), color = MaterialTheme.colorScheme.primary)
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    StatBox(label = "Drafts", count = draftCount.toString(), color = MaterialTheme.colorScheme.tertiary)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search exams by title, topic, difficulty...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedFilterTab,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    text = { Text("All (${exams.size})", fontWeight = if (selectedFilterTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    text = { Text("Published ($publishedCount)", fontWeight = if (selectedFilterTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedFilterTab == 2,
                    onClick = { selectedFilterTab = 2 },
                    text = { Text("Drafts ($draftCount)", fontWeight = if (selectedFilterTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Exams List
            if (filteredExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "No exams match '$searchQuery'" else "No exams in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tap '+ New Exam' below to create a new model test manually or with Gemini AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredExams, key = { it.id }) { exam ->
                        val questionsList by viewModel.getQuestionsForExam(exam.id).collectAsStateWithLifecycle(initialValue = emptyList())
                        val subjectName = subjects.firstOrNull { it.id == exam.subjectId }?.name

                        ExamManagementCard(
                            exam = exam,
                            questionCount = questionsList.size,
                            subjectName = subjectName,
                            onPreview = { previewExam = exam },
                            onEdit = {
                                editingExamId = exam.id
                                showManualEditor = true
                            },
                            onTogglePublish = {
                                examToPublishToggle = exam
                            },
                            onDuplicate = {
                                viewModel.duplicateExam(exam.id)
                            },
                            onDelete = {
                                examToDelete = exam
                            },
                            onLeaderboard = {
                                examForLeaderboard = exam
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Create Options (Manual vs AI)
    if (showCreateOptions) {
        AlertDialog(
            onDismissRequest = { showCreateOptions = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Exam", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Select how you want to build this exam:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCreateOptions = false
                                editingExamId = null
                                showManualEditor = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Manual Exam Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Draft title, options, marks, and custom questions manually.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCreateOptions = false
                                showAiEditor = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AI-Powered Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Generate questions from document upload or topic prompts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual / Full Exam Editor Dialog
    if (showManualEditor) {
        ExamEditorDialog(
            viewModel = viewModel,
            initialExamId = editingExamId,
            onDismiss = {
                showManualEditor = false
                editingExamId = null
            }
        )
    }

    // AI Exam Generator Dialog
    examForLeaderboard?.let { exam ->
        ExamLeaderboardDialog(
            exam = exam,
            viewModel = viewModel,
            onDismiss = { examForLeaderboard = null }
        )
    }
    if (showAiEditor) {
        AiExamEditorDialog(
            viewModel = viewModel,
            onDismiss = { showAiEditor = false },
            onOpenManualEditor = { createdId ->
                showAiEditor = false
                editingExamId = createdId
                showManualEditor = true
            }
        )
    }

    // Live Preview Dialog
    previewExam?.let { exam ->
        ExamPreviewDialog(
            exam = exam,
            viewModel = viewModel,
            onDismiss = { previewExam = null },
            onEdit = {
                val id = exam.id
                previewExam = null
                editingExamId = id
                showManualEditor = true
            }
        )
    }

    // Publish / Unpublish Confirmation Alert
    examToPublishToggle?.let { exam ->
        val isPublished = exam.status.equals("PUBLISHED", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { examToPublishToggle = null },
            icon = {
                Icon(
                    if (isPublished) Icons.Default.Unpublished else Icons.Default.Publish,
                    contentDescription = null,
                    tint = if (isPublished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(if (isPublished) "Unpublish Exam?" else "Publish Exam?")
            },
            text = {
                Text(
                    if (isPublished)
                        "This will convert '${exam.title}' back to DRAFT mode. Aspirants will no longer be able to take it until republished."
                    else
                        "This will make '${exam.title}' immediately live for all registered aspirants and BCS students."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.togglePublishStatus(exam.id, exam.status)
                        examToPublishToggle = null
                    },
                    colors = if (isPublished)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isPublished) "Unpublish to Draft" else "Publish Live Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToPublishToggle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Exam?") },
            text = { Text("Are you sure you want to permanently delete '${exam.title}' and all its questions? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExam(exam.id)
                        examToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExamManagementCard(
    exam: ExamEntity,
    questionCount: Int,
    subjectName: String?,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onLeaderboard: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isPublished = exam.status.equals("PUBLISHED", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status Badge & Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isPublished) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isPublished) Icons.Default.CheckCircle else Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isPublished) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPublished) "PUBLISHED" else "DRAFT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPublished) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (subjectName != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = subjectName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Preview Exam") },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onPreview()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Exam") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isPublished) "Unpublish (Move to Draft)" else "Publish Live") },
                            leadingIcon = { Icon(if (isPublished) Icons.Default.Unpublished else Icons.Default.Publish, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onTogglePublish()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Exam") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Exam", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Description
            Text(
                text = exam.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (exam.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exam.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgeTag(icon = Icons.Default.Quiz, text = "$questionCount Qs")
                BadgeTag(icon = Icons.Default.Timer, text = "${exam.durationMinutes}m")
                BadgeTag(icon = Icons.Default.Speed, text = exam.difficulty)
                if (exam.negativeMarking > 0) {
                    BadgeTag(icon = Icons.Default.RemoveCircleOutline, text = "-${exam.negativeMarking}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }

                // Publish / Unpublish Toggle Button
                if (isPublished) {
                    OutlinedButton(
                        onClick = onTogglePublish,
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Unpublished, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unpublish", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = onTogglePublish,
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publish Live", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Preview Button
                FilledTonalIconButton(
                    onClick = onPreview,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Preview", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BadgeTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
