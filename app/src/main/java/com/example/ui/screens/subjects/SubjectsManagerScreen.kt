package com.example.ui.screens.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.ui.components.BcsIconHelper
import com.example.ui.components.DriveLinkButton
import com.example.ui.components.EmptyState
import com.example.ui.components.parseHexColor
import com.example.ui.viewmodel.BcsAdminViewModel

@Composable
fun SubjectsManagerScreen(
    viewModel: BcsAdminViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    var showSubjectDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    var showTopicDialog by remember { mutableStateOf(false) }
    var defaultSubjectIdForTopic by remember { mutableStateOf<String?>(null) }
    var topicToEdit by remember { mutableStateOf<TopicEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

    val expandedSubjects = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    subjectToEdit = null
                    showSubjectDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Subject", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar & Quick Add Topic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BCS Syllabus Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${subjects.size} Subjects • ${topics.size} Topics configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        topicToEdit = null
                        defaultSubjectIdForTopic = subjects.firstOrNull()?.id
                        showTopicDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("quick_add_topic_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Topic", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (subjects.isEmpty()) {
                EmptyState(
                    title = "No Subjects Configured",
                    message = "You haven't added any BCS subjects yet. Tap below to create your first subject.",
                    icon = Icons.Default.School,
                    actionLabel = "Add Subject",
                    onAction = {
                        subjectToEdit = null
                        showSubjectDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        val subjectTopics = topics.filter { it.subjectId == subject.id }.sortedBy { it.orderIndex }
                        val isExpanded = expandedSubjects[subject.id] ?: true

                        SubjectCard(
                            subject = subject,
                            topics = subjectTopics,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedSubjects[subject.id] = !isExpanded
                            },
                            onEditSubject = {
                                subjectToEdit = subject
                                showSubjectDialog = true
                            },
                            onDeleteSubject = {
                                subjectToDelete = subject
                            },
                            onAddTopic = {
                                topicToEdit = null
                                defaultSubjectIdForTopic = subject.id
                                showTopicDialog = true
                            },
                            onEditTopic = { topic ->
                                topicToEdit = topic
                                showTopicDialog = true
                            },
                            onDeleteTopic = { topic ->
                                topicToDelete = topic
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSubjectDialog) {
        SubjectEditorDialog(
            initialSubject = subjectToEdit,
            onDismiss = {
                showSubjectDialog = false
                subjectToEdit = null
            },
            onSaveSubject = { id, name, code, colorHex, iconName, driveUrl, desc ->
                viewModel.saveSubject(
                    id = id,
                    name = name,
                    code = code,
                    colorHex = colorHex,
                    iconName = iconName,
                    driveFolderUrl = driveUrl,
                    description = desc,
                    onSuccess = {
                        showSubjectDialog = false
                        subjectToEdit = null
                    }
                )
            }
        )
    }

    if (showTopicDialog) {
        TopicEditorDialog(
            initialTopic = topicToEdit,
            defaultSubjectId = defaultSubjectIdForTopic,
            subjects = subjects,
            onDismiss = {
                showTopicDialog = false
                topicToEdit = null
            },
            onSaveTopic = { id, subjId, name, desc, driveDocUrl, order ->
                viewModel.saveTopic(
                    id = id,
                    subjectId = subjId,
                    name = name,
                    description = desc,
                    driveDocUrl = driveDocUrl,
                    orderIndex = order,
                    onSuccess = {
                        showTopicDialog = false
                        topicToEdit = null
                    }
                )
            }
        )
    }

    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete '${subjectToDelete?.name}' and all associated syllabus topics?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        subjectToDelete?.let { viewModel.deleteSubject(it.id) }
                        subjectToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (topicToDelete != null) {
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = { Text("Delete Topic?") },
            text = { Text("Are you sure you want to delete '${topicToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        topicToDelete?.let { viewModel.deleteTopic(it.id) }
                        topicToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubjectCard(
    subject: SubjectEntity,
    topics: List<TopicEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditSubject: () -> Unit,
    onDeleteSubject: () -> Unit,
    onAddTopic: () -> Unit,
    onEditTopic: (TopicEntity) -> Unit,
    onDeleteTopic: (TopicEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = parseHexColor(subject.colorHex)
    val subjectIcon = BcsIconHelper.getIcon(subject.iconName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.code}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subject Main Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(subjectColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = subjectIcon,
                        contentDescription = null,
                        tint = subjectColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Subject Name & Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(subjectColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = subject.code,
                                fontSize = 10.sp,
                                color = subjectColor,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    if (subject.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subject.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${topics.size} syllabus topics",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditSubject,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Subject",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteSubject,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Subject",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Google Drive Resource folder launcher if provided
            if (subject.driveFolderUrl.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DriveLinkButton(
                        url = subject.driveFolderUrl,
                        label = "${subject.code} Google Drive Folder",
                        isFolder = true
                    )
                }
            }

            // Expandable Topics Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYLLABUS SUB-TOPICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        TextButton(
                            onClick = onAddTopic,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Topic", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (topics.isEmpty()) {
                        Text(
                            text = "No sub-topics added yet for ${subject.name}.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        topics.forEachIndexed { index, topic ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                            TopicRowItem(
                                topic = topic,
                                onEdit = { onEditTopic(topic) },
                                onDelete = { onDeleteTopic(topic) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicRowItem(
    topic: TopicEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("topic_row_${topic.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Order Badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${topic.orderIndex}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Topic info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (topic.description.isNotBlank()) {
                Text(
                    text = topic.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            if (topic.driveDocUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                DriveLinkButton(
                    url = topic.driveDocUrl,
                    label = "Study Note Doc"
                )
            }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Topic",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Topic",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
