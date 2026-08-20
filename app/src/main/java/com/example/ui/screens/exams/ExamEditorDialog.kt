package com.example.ui.screens.exams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.BcsAdminViewModel
import com.example.ui.viewmodel.QuestionDraft
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamEditorDialog(
    viewModel: BcsAdminViewModel,
    onDismiss: () -> Unit,
    initialExamId: String? = null
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle(initialValue = emptyList())
    val topics by viewModel.topics.collectAsStateWithLifecycle(initialValue = emptyList())

    val examId = remember { initialExamId ?: UUID.randomUUID().toString() }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf("") }
    var selectedTopicId by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("30") }
    var questionTimerSeconds by remember { mutableStateOf("0") }
    var negativeMarking by remember { mutableStateOf("0.5") }
    var difficulty by remember { mutableStateOf("MEDIUM") }
    var status by remember { mutableStateOf("DRAFT") }
    
    val questions = remember { mutableStateListOf<QuestionDraft>() }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }

    // Question form fields
    var qText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctOpt by remember { mutableStateOf("A") }
    var explanation by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("1") }

    var isLoaded by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load initial data if editing
    LaunchedEffect(initialExamId) {
        if (initialExamId != null && !isLoaded) {
            viewModel.loadExamForEditing(initialExamId) { exam, loadedQuestions ->
                title = exam.title
                description = exam.description
                selectedSubjectId = exam.subjectId
                selectedTopicId = exam.topicId
                durationMinutes = if (exam.durationMinutes > 0) exam.durationMinutes.toString() else "30"
                questionTimerSeconds = exam.questionTimerSeconds.toString()
                negativeMarking = exam.negativeMarking.toString()
                difficulty = exam.difficulty.ifBlank { "MEDIUM" }
                status = exam.status.ifBlank { "DRAFT" }
                questions.clear()
                questions.addAll(loadedQuestions)
                isLoaded = true
            }
        } else {
            isLoaded = true
        }
    }

    fun saveExam(publish: Boolean) {
        if (title.isBlank()) {
            errorMessage = "Please enter an exam title"
            return
        }

        val finalStatus = if (publish) "PUBLISHED" else "DRAFT"
        val dur = durationMinutes.toIntOrNull() ?: 30
        val qTimer = questionTimerSeconds.toIntOrNull() ?: 0
        val neg = negativeMarking.toDoubleOrNull() ?: 0.5

        viewModel.saveFullExam(
            id = examId,
            title = title,
            description = description,
            subjectId = selectedSubjectId,
            topicId = selectedTopicId,
            durationMinutes = dur,
            questionTimerSeconds = qTimer,
            negativeMarking = neg,
            difficulty = difficulty,
            status = finalStatus,
            questions = questions.toList()
        ) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (title.isNotBlank() || questions.isNotEmpty()) {
                showDiscardConfirm = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (initialExamId == null) "Create Exam" else "Edit Exam",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (status == "PUBLISHED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (status == "PUBLISHED") "PUBLISHED" else "DRAFT",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (status == "PUBLISHED") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${questions.size} Questions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { saveExam(publish = false) },
                            enabled = title.isNotBlank()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Draft")
                        }
                        Button(
                            onClick = { saveExam(publish = true) },
                            enabled = title.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publish")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Section 1: Basic Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "1. Basic Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                errorMessage = null
                            },
                            label = { Text("Exam Title *") },
                            placeholder = { Text("e.g., 46th BCS Preliminary Model Test 01") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Syllabus") },
                            placeholder = { Text("Outline the topics covered, syllabus, or instructions for candidates...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Subject & Topic selectors
                        if (subjects.isNotEmpty()) {
                            Text("Subject (Optional)", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subjects.take(4).forEach { sub ->
                                    FilterChip(
                                        selected = selectedSubjectId == sub.id,
                                        onClick = {
                                            selectedSubjectId = if (selectedSubjectId == sub.id) "" else sub.id
                                        },
                                        label = { Text(sub.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Exam Settings & Rules Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "2. Parameters & Scoring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it },
                                label = { Text("Duration (Mins)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) }
                            )

                            OutlinedTextField(
                                value = negativeMarking,
                                onValueChange = { negativeMarking = it },
                                label = { Text("Negative Mark") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) }
                            )
                        }

                        Text("Difficulty Level", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
                                FilterChip(
                                    selected = difficulty.equals(diff, ignoreCase = true),
                                    onClick = { difficulty = diff },
                                    label = { Text(diff) }
                                )
                            }
                        }

                        // Publish Status Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Publish Immediately",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (status == "PUBLISHED") "Live for all registered candidates" else "Hidden in drafts (Admin only)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = status == "PUBLISHED",
                                onCheckedChange = { isChecked ->
                                    status = if (isChecked) "PUBLISHED" else "DRAFT"
                                }
                            )
                        }
                    }
                }

                // Section 3: Questions List Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "3. Questions (${questions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (questions.isNotEmpty()) {
                                Text(
                                    "Total Marks: ${questions.sumOf { it.marks }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        if (questions.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Quiz,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No questions added yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Use the form below to append multiple questions to this exam.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            questions.forEachIndexed { index, q ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (editingQuestionIndex == index)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (editingQuestionIndex == index)
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    else null
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            "${index + 1}",
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Correct: Option ${q.correctOption}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "(${q.marks} Mark)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row {
                                                // Move up / down
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            val item = questions.removeAt(index)
                                                            questions.add(index - 1, item)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (index < questions.size - 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val item = questions.removeAt(index)
                                                            questions.add(index + 1, item)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                // Edit
                                                IconButton(
                                                    onClick = {
                                                        editingQuestionIndex = index
                                                        qText = q.questionText
                                                        optA = q.optionA
                                                        optB = q.optionB
                                                        optC = q.optionC
                                                        optD = q.optionD
                                                        correctOpt = q.correctOption
                                                        explanation = q.explanation
                                                        marks = q.marks.toString()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                                // Delete
                                                IconButton(
                                                    onClick = {
                                                        if (editingQuestionIndex == index) editingQuestionIndex = null
                                                        questions.removeAt(index)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            q.questionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                OptionSnippet("A", q.optionA, q.correctOption == "A")
                                                OptionSnippet("B", q.optionB, q.correctOption == "B")
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                OptionSnippet("C", q.optionC, q.correctOption == "C")
                                                OptionSnippet("D", q.optionD, q.correctOption == "D")
                                            }
                                        }

                                        if (q.explanation.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Explanation: ${q.explanation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Question Input Form (for adding or updating a question)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (editingQuestionIndex == null) "➕ Add New Question" else "✏️ Edit Question #${editingQuestionIndex!! + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (editingQuestionIndex != null) {
                                        TextButton(onClick = {
                                            editingQuestionIndex = null
                                            qText = ""
                                            optA = ""
                                            optB = ""
                                            optC = ""
                                            optD = ""
                                            correctOpt = "A"
                                            explanation = ""
                                            marks = "1"
                                        }) {
                                            Text("Cancel Edit")
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = qText,
                                    onValueChange = { qText = it },
                                    label = { Text("Question Prompt *") },
                                    placeholder = { Text("Type the question in Bangla or English...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = optA,
                                        onValueChange = { optA = it },
                                        label = { Text("Option A *") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = optB,
                                        onValueChange = { optB = it },
                                        label = { Text("Option B *") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = optC,
                                        onValueChange = { optC = it },
                                        label = { Text("Option C *") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = optD,
                                        onValueChange = { optD = it },
                                        label = { Text("Option D *") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Correct Option", style = MaterialTheme.typography.labelMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("A", "B", "C", "D").forEach { opt ->
                                                FilterChip(
                                                    selected = correctOpt == opt,
                                                    onClick = { correctOpt = opt },
                                                    label = { Text(opt, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = marks,
                                        onValueChange = { marks = it },
                                        label = { Text("Marks") },
                                        modifier = Modifier.width(80.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }

                                OutlinedTextField(
                                    value = explanation,
                                    onValueChange = { explanation = it },
                                    label = { Text("Explanation (Optional)") },
                                    placeholder = { Text("Why is this answer correct? Provide context...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (qText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()) {
                                            val draft = QuestionDraft(
                                                id = if (editingQuestionIndex != null) questions[editingQuestionIndex!!].id else UUID.randomUUID().toString(),
                                                questionText = qText,
                                                optionA = optA,
                                                optionB = optB,
                                                optionC = optC,
                                                optionD = optD,
                                                correctOption = correctOpt,
                                                explanation = explanation,
                                                marks = marks.toIntOrNull() ?: 1
                                            )

                                            if (editingQuestionIndex != null) {
                                                questions[editingQuestionIndex!!] = draft
                                                editingQuestionIndex = null
                                            } else {
                                                questions.add(draft)
                                            }

                                            // Clear form
                                            qText = ""
                                            optA = ""
                                            optB = ""
                                            optC = ""
                                            optD = ""
                                            correctOpt = "A"
                                            explanation = ""
                                            marks = "1"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = qText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()
                                ) {
                                    Icon(
                                        if (editingQuestionIndex == null) Icons.Default.Add else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (editingQuestionIndex == null) "Append Question to Exam" else "Update Question")
                                }
                            }
                        }
                    }
                }

                // Bottom Save/Publish Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { saveExam(publish = false) },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save as Draft")
                    }

                    Button(
                        onClick = { saveExam(publish = true) },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish Exam")
                    }
                }
            }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have unsaved edits in this exam. Closing now will discard them.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
private fun OptionSnippet(label: String, text: String, isCorrect: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
            color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
            color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
