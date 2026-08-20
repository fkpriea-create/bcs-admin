package com.example.ui.screens.tasks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.ui.components.BcsIconHelper
import com.example.ui.components.openExternalLink
import com.example.ui.components.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
    initialTask: TaskEntity? = null,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    onDismiss: () -> Unit,
    onSaveTask: (
        id: String?,
        title: String,
        description: String,
        subjectId: String,
        topicId: String,
        dueDate: String,
        dueTime: String,
        repeatSchedule: String,
        googleDriveUrl: String,
        googleDriveLabel: String,
        priority: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Default dates
    val calendar = Calendar.getInstance()
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val defaultDate = initialTask?.dueDate ?: run {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        sdfDate.format(calendar.time)
    }

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }

    var selectedSubjectId by remember {
        mutableStateOf(initialTask?.subjectId ?: subjects.firstOrNull()?.id ?: "")
    }

    val availableTopics = remember(selectedSubjectId, topics) {
        topics.filter { it.subjectId == selectedSubjectId }
    }

    var selectedTopicId by remember(selectedSubjectId) {
        mutableStateOf(
            if (initialTask != null && initialTask.subjectId == selectedSubjectId) initialTask.topicId
            else availableTopics.firstOrNull()?.id ?: ""
        )
    }

    var dueDate by remember { mutableStateOf(defaultDate) }
    var dueTime by remember { mutableStateOf(initialTask?.dueTime ?: "23:59") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "HIGH") }
    var repeatSchedule by remember { mutableStateOf(initialTask?.repeatSchedule ?: "NONE") }
    var googleDriveUrl by remember { mutableStateOf(initialTask?.googleDriveUrl ?: "") }
    var googleDriveLabel by remember { mutableStateOf(initialTask?.googleDriveLabel ?: "BCS Study Material PDF") }

    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var topicDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialTask == null) "Publish New BCS Task" else "Edit Published Task",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Syncs automatically to BCS Student App",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Read 7th March Speech & Solve 100 MCQs") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detailed Instructions / Syllabus Target") },
                    placeholder = { Text("e.g. Cover pages 45-60 from lecture guide. Memorize sector commanders.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input"),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Subject Selector
                Text(
                    text = "BCS Subject Category *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val currentSubject = subjects.find { it.id == selectedSubjectId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { subjectDropdownExpanded = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentSubject != null) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(currentSubject.colorHex))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${currentSubject.code} - ${currentSubject.name}",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "Select a Subject",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        subjects.forEach { subj ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(subj.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${subj.code} - ${subj.name}")
                                    }
                                },
                                onClick = {
                                    selectedSubjectId = subj.id
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Topic Selector
                Text(
                    text = "Syllabus Sub-Topic",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val currentTopic = topics.find { it.id == selectedTopicId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { topicDropdownExpanded = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentTopic?.name ?: if (availableTopics.isEmpty()) "No sub-topics created yet" else "Select Sub-Topic",
                            fontWeight = FontWeight.Normal,
                            color = if (currentTopic != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = topicDropdownExpanded,
                        onDismissRequest = { topicDropdownExpanded = false }
                    ) {
                        if (availableTopics.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No topics for this subject") },
                                onClick = { topicDropdownExpanded = false }
                            )
                        } else {
                            availableTopics.forEach { top ->
                                DropdownMenuItem(
                                    text = { Text("${top.orderIndex}. ${top.name}") },
                                    onClick = {
                                        selectedTopicId = top.id
                                        topicDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Due Date & Due Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Due Time") },
                        placeholder = { Text("23:59") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority Level
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        FilterChip(
                            selected = priority.equals(p, ignoreCase = true),
                            onClick = { priority = p },
                            label = { Text(p, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Repeat Schedule
                Text(
                    text = "Repeat Schedule",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("NONE", "DAILY", "WEEKDAYS", "WEEKLY").forEach { rep ->
                        FilterChip(
                            selected = repeatSchedule.equals(rep, ignoreCase = true),
                            onClick = { repeatSchedule = rep },
                            label = { Text(rep) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Google Drive Resource Section
                Text(
                    text = "Google Drive Study Material",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = googleDriveUrl,
                    onValueChange = { googleDriveUrl = it },
                    label = { Text("Google Drive URL (PDF / Doc / Sheet)") },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null)
                    },
                    trailingIcon = {
                        if (googleDriveUrl.isNotBlank()) {
                            TextButton(
                                onClick = { openExternalLink(context, googleDriveUrl) }
                            ) {
                                Text("Test Link", fontSize = 11.sp)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_drive_url_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = googleDriveLabel,
                    onValueChange = { googleDriveLabel = it },
                    label = { Text("Drive Button Label") },
                    placeholder = { Text("e.g. BCS Lecture PDF & MCQs") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter a Task Title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedSubjectId.isBlank()) {
                                Toast.makeText(context, "Please select a Subject", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSaveTask(
                                initialTask?.id,
                                title,
                                description,
                                selectedSubjectId,
                                selectedTopicId,
                                dueDate,
                                dueTime,
                                repeatSchedule,
                                googleDriveUrl,
                                googleDriveLabel,
                                priority
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("publish_task_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialTask == null) "Publish to Firestore" else "Update Task",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
