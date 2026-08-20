package com.example.ui.screens.exams

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.network.Content
import com.example.data.network.GenerateContentRequest
import com.example.data.network.Part
import com.example.data.network.RetrofitClient
import com.example.ui.viewmodel.BcsAdminViewModel
import com.example.ui.viewmodel.QuestionDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiExamEditorDialog(
    viewModel: BcsAdminViewModel,
    onDismiss: () -> Unit,
    onOpenManualEditor: (String) -> Unit = {}
) {
    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("MEDIUM") }
    var numberOfQuestions by remember { mutableStateOf("10") }
    var instructions by remember { mutableStateOf("") }
    
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var documentName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        documentUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        documentName = c.getString(displayNameIndex)
                    }
                }
            }
        }
    }
    
    var isGenerating by remember { mutableStateOf(false) }
    var generatedText by remember { mutableStateOf("") }
    var parsedQuestions by remember { mutableStateOf<List<QuestionDraft>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    fun saveExamWithStatus(status: String, openEditor: Boolean = false) {
        val examId = UUID.randomUUID().toString()
        val numQ = numberOfQuestions.toIntOrNull() ?: (if (parsedQuestions.isNotEmpty()) parsedQuestions.size else 10)
        val duration = numQ * 2

        viewModel.saveFullExam(
            id = examId,
            title = "$subject - $topic",
            description = "AI-generated exam based on $topic. ${if (documentName.isNotBlank()) "Source document: $documentName" else ""}",
            durationMinutes = duration,
            difficulty = difficulty,
            status = status,
            questions = parsedQuestions
        ) {
            if (openEditor) {
                onOpenManualEditor(examId)
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("AI Exam Generator", fontWeight = FontWeight.Bold)
                            Text(
                                "Powered by Gemini 3.5 Flash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Generation Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject * (e.g., Bangladesh Affairs, General Science)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                        )
                        
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text("Topic / Chapter * (e.g., Constitution, Liberation War)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Topic, contentDescription = null) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = numberOfQuestions,
                                onValueChange = { numberOfQuestions = it },
                                label = { Text("No. of Questions") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Quiz, contentDescription = null) }
                            )
                            
                            OutlinedTextField(
                                value = difficulty,
                                onValueChange = { difficulty = it },
                                label = { Text("Difficulty") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Special Prompt / Instructions (Optional)") },
                            placeholder = { Text("e.g., Focus on articles 27 to 44 of the Constitution, include chronological questions...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        // Document Upload Box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            if (documentName.isNotBlank()) documentName else "Attach PDF / Document (Optional)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (documentName.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                        Text(
                                            if (documentName.isNotBlank()) "Document attached for context" else "AI will extract exam questions from this file",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (documentName.isNotBlank()) {
                                    IconButton(onClick = {
                                        documentUri = null
                                        documentName = ""
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    Button(onClick = { documentPicker.launch("*/*") }) {
                                        Text("Browse")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isGenerating = true
                                coroutineScope.launch {
                                    val result = generateQuestions(
                                        subject = subject,
                                        topic = topic,
                                        numQuestions = numberOfQuestions,
                                        difficulty = difficulty,
                                        instructions = instructions,
                                        documentUri = documentUri,
                                        contentResolver = context.contentResolver
                                    )
                                    generatedText = result
                                    parsedQuestions = parseGeneratedQuestions(result)
                                    isGenerating = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGenerating && subject.isNotBlank() && topic.isNotBlank()
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Questions with Gemini...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Questions")
                            }
                        }
                    }
                }

                if (generatedText.isNotBlank()) {
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
                                    "Generated Questions (${if (parsedQuestions.isNotEmpty()) parsedQuestions.size else numberOfQuestions})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Ready to Save",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = generatedText,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Publish, Draft, or Open in Editor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { saveExamWithStatus("DRAFT") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Draft", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = { saveExamWithStatus("PUBLISHED") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Publish Live", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            OutlinedButton(
                                onClick = { saveExamWithStatus("DRAFT", openEditor = true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in Full Editor (Review & Tweak)")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseGeneratedQuestions(text: String): List<QuestionDraft> {
    val questions = mutableListOf<QuestionDraft>()
    val blocks = text.split(Regex("(?m)^(?=\\s*(?:Question\\s*\\d+|Q\\d+|\\d+[.)]))"))
    for (block in blocks) {
        val trimmed = block.trim()
        if (trimmed.isBlank() || trimmed.length < 15) continue
        
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        var qText = ""
        var optA = ""
        var optB = ""
        var optC = ""
        var optD = ""
        var correct = "A"
        var expl = ""
        
        for (line in lines) {
            when {
                line.matches(Regex("^(?:Question\\s*\\d*[:.]?|Q\\d*[:.]?|\\d+[.)])\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    qText = line.replace(Regex("^(?:Question\\s*\\d*[:.]?|Q\\d*[:.]?|\\d+[.)])\\s*", RegexOption.IGNORE_CASE), "").trim()
                }
                line.matches(Regex("^[Aa][.)\\-]\\s*.*")) -> optA = line.substring(2).trim()
                line.matches(Regex("^[Bb][.)\\-]\\s*.*")) -> optB = line.substring(2).trim()
                line.matches(Regex("^[Cc][.)\\-]\\s*.*")) -> optC = line.substring(2).trim()
                line.matches(Regex("^[Dd][.)\\-]\\s*.*")) -> optD = line.substring(2).trim()
                line.contains("Answer", ignoreCase = true) || line.contains("Correct", ignoreCase = true) -> {
                    val match = Regex("[ABCDabcd]").find(line.substringAfter(":")) ?: Regex("[ABCDabcd]").find(line)
                    if (match != null) correct = match.value.uppercase()
                }
                line.contains("Explanation", ignoreCase = true) -> {
                    expl = line.substringAfter(":").trim()
                }
                qText.isBlank() -> qText = line
            }
        }
        
        if (qText.isNotBlank()) {
            questions.add(
                QuestionDraft(
                    questionText = qText,
                    optionA = optA.ifBlank { "Option A" },
                    optionB = optB.ifBlank { "Option B" },
                    optionC = optC.ifBlank { "Option C" },
                    optionD = optD.ifBlank { "Option D" },
                    correctOption = correct,
                    explanation = expl
                )
            )
        }
    }
    return questions
}

private suspend fun generateQuestions(
    subject: String,
    topic: String,
    numQuestions: String,
    difficulty: String,
    instructions: String,
    documentUri: Uri?,
    contentResolver: android.content.ContentResolver
): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank()) {
        return@withContext "Error: Gemini API Key is missing. Please add it to the Secrets panel in AI Studio."
    }
    
    val prompt = "Generate $numQuestions multiple choice questions for a BCS (Bangladesh Civil Service) exam on the subject '$subject' and topic '$topic'. Difficulty level: $difficulty. Format every question clearly with:\n" +
            "Question <Number>: <Question Text>\n" +
            "A) <Option A>\n" +
            "B) <Option B>\n" +
            "C) <Option C>\n" +
            "D) <Option D>\n" +
            "Answer: <A/B/C/D>\n" +
            "Explanation: <Brief Explanation>\n" +
            (if (instructions.isNotBlank()) "\nAdditional instructions: $instructions" else "")
    
    val parts = mutableListOf<Part>()
    parts.add(Part(text = prompt))
    
    documentUri?.let { uri ->
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val mimeType = contentResolver.getType(uri) ?: "application/pdf"
                parts.add(Part(inlineData = com.example.data.network.InlineData(mimeType, base64Data)))
            }
        } catch (e: Exception) {
            return@withContext "Error reading document: ${e.message}"
        }
    }
    
    val request = GenerateContentRequest(
        contents = listOf(Content(parts = parts))
    )
    
    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini API"
    } catch (e: Exception) {
        "Error generating content: ${e.message}"
    }
}
