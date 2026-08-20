package com.example.ui.screens.subjects

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import com.example.ui.components.ColorPicker
import com.example.ui.components.IconPicker
import com.example.ui.components.openExternalLink
import com.example.ui.theme.SubjectPalette

@Composable
fun SubjectEditorDialog(
    initialSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onSaveSubject: (
        id: String?,
        name: String,
        code: String,
        colorHex: String,
        iconName: String,
        driveFolderUrl: String,
        description: String
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var code by remember { mutableStateOf(initialSubject?.code ?: "") }
    var description by remember { mutableStateOf(initialSubject?.description ?: "") }
    var colorHex by remember { mutableStateOf(initialSubject?.colorHex ?: SubjectPalette.first()) }
    var iconName by remember { mutableStateOf(initialSubject?.iconName ?: "menu_book") }
    var driveFolderUrl by remember { mutableStateOf(initialSubject?.driveFolderUrl ?: "") }

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
                        text = if (initialSubject == null) "Add BCS Subject" else "Edit BCS Subject",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Bangladesh Affairs") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Code
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Subject Code *") },
                    placeholder = { Text("e.g. BCS-BD, BCS-ENG, BCS-MATH") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_code_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Syllabus Scope / Marks Weightage") },
                    placeholder = { Text("e.g. 35th-46th BCS Preliminary Syllabus (30 Marks)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Picker
                ColorPicker(
                    selectedHex = colorHex,
                    onColorSelected = { colorHex = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Icon Picker
                IconPicker(
                    selectedIconName = iconName,
                    onIconSelected = { iconName = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Google Drive Folder URL
                OutlinedTextField(
                    value = driveFolderUrl,
                    onValueChange = { driveFolderUrl = it },
                    label = { Text("Google Drive Folder URL (Lectures/Sheets)") },
                    placeholder = { Text("https://drive.google.com/drive/folders/...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.FolderShared, contentDescription = null)
                    },
                    trailingIcon = {
                        if (driveFolderUrl.isNotBlank()) {
                            TextButton(onClick = { openExternalLink(context, driveFolderUrl) }) {
                                Text("Test Link", fontSize = 11.sp)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_drive_input"),
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
                            if (name.isBlank() || code.isBlank()) {
                                Toast.makeText(context, "Subject Name and Code are required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSaveSubject(
                                initialSubject?.id,
                                name,
                                code,
                                colorHex,
                                iconName,
                                driveFolderUrl,
                                description
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_subject_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Subject", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
