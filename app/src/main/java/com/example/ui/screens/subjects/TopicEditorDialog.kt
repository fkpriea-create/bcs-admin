package com.example.ui.screens.subjects

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.ui.components.openExternalLink
import com.example.ui.components.parseHexColor

@Composable
fun TopicEditorDialog(
    initialTopic: TopicEntity? = null,
    defaultSubjectId: String? = null,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSaveTopic: (
        id: String?,
        subjectId: String,
        name: String,
        description: String,
        driveDocUrl: String,
        orderIndex: Int
    ) -> Unit
) {
    val context = LocalContext.current

    var selectedSubjectId by remember {
        mutableStateOf(
            initialTopic?.subjectId ?: defaultSubjectId ?: subjects.firstOrNull()?.id ?: ""
        )
    }
    var name by remember { mutableStateOf(initialTopic?.name ?: "") }
    var description by remember { mutableStateOf(initialTopic?.description ?: "") }
    var driveDocUrl by remember { mutableStateOf(initialTopic?.driveDocUrl ?: "") }
    var orderIndexStr by remember { mutableStateOf(initialTopic?.orderIndex?.toString() ?: "1") }

    var subjectDropdownExpanded by remember { mutableStateOf(false) }

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
                    Text(
                        text = if (initialTopic == null) "Add Syllabus Topic" else "Edit Syllabus Topic",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Selector
                Text(
                    text = "Assign to Subject *",
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
                        if (currentSubject != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(currentSubject.colorHex))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${currentSubject.code} - ${currentSubject.name}",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text("Select Subject", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        subjects.forEach { subj ->
                            DropdownMenuItem(
                                text = { Text("${subj.code} - ${subj.name}") },
                                onClick = {
                                    selectedSubjectId = subj.id
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Topic Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic Name *") },
                    placeholder = { Text("e.g. Liberation War 1971 & 7th March Speech") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_name_input"),
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topic Scope & Key Notes") },
                    placeholder = { Text("Key events, sector commanders, and constitutional references") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Order Index
                OutlinedTextField(
                    value = orderIndexStr,
                    onValueChange = { orderIndexStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Topic Order Index") },
                    placeholder = { Text("1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Drive Doc Link
                OutlinedTextField(
                    value = driveDocUrl,
                    onValueChange = { driveDocUrl = it },
                    label = { Text("Google Drive Note / Doc URL") },
                    placeholder = { Text("https://docs.google.com/document/d/...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null)
                    },
                    trailingIcon = {
                        if (driveDocUrl.isNotBlank()) {
                            TextButton(onClick = { openExternalLink(context, driveDocUrl) }) {
                                Text("Test Link", fontSize = 11.sp)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_drive_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
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
                            if (name.isBlank()) {
                                Toast.makeText(context, "Topic Name is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedSubjectId.isBlank()) {
                                Toast.makeText(context, "Please assign a Subject", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val order = orderIndexStr.toIntOrNull() ?: 1
                            onSaveTopic(
                                initialTopic?.id,
                                selectedSubjectId,
                                name,
                                description,
                                driveDocUrl,
                                order
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_topic_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Topic", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
