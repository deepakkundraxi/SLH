package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.slh.app.ui.theme.ChipColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionManagementScreen(
    user: DemoUser,
    onBack: () -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {
    BackHandler {
        onBack()
    }

    val coachingId = user.coachingId ?: ""

    var selectedBatchId by remember {
        mutableStateOf("")
    }

    var selectedTestId by remember {
        mutableStateOf("")
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var showQuestionDialog by remember {
        mutableStateOf(false)
    }

    var editingQuestion by remember {
        mutableStateOf<TestQuestion?>(null)
    }

    /*
     * Admin and Teacher: all batches of this coaching
     * (batches are shared — not locked to one teacher).
     */
    val availableBatches =
        BatchStore.findBatchesByCoaching(
            coachingId = coachingId
        )

    LaunchedEffect(availableBatches) {
        if (
            selectedBatchId.isBlank() &&
            availableBatches.isNotEmpty()
        ) {
            selectedBatchId = availableBatches.first().id
        }

        if (
            selectedBatchId.isNotBlank() &&
            availableBatches.none {
                it.id == selectedBatchId
            }
        ) {
            selectedBatchId =
                availableBatches.firstOrNull()?.id ?: ""
        }
    }

    val selectedBatch =
        availableBatches.find {
            it.id == selectedBatchId
        }

    val availableTests =
        if (selectedBatchId.isBlank()) {
            emptyList()
        } else {
            TestStore.findTestsByBatchAndCoaching(
                batchId = selectedBatchId,
                coachingId = coachingId
            )
        }

    LaunchedEffect(selectedBatchId, availableTests) {
        if (
            selectedTestId.isNotBlank() &&
            availableTests.none {
                it.id == selectedTestId
            }
        ) {
            selectedTestId = ""
        }

        if (
            selectedTestId.isBlank() &&
            availableTests.isNotEmpty()
        ) {
            selectedTestId = availableTests.first().id
        }
    }

    val selectedTest =
        availableTests.find {
            it.id == selectedTestId
        }

    /*
     * refreshKey intentionally used to force UI refresh
     * after add/edit/delete operations.
     */
    val questions =
        remember(
            selectedTestId,
            refreshKey
        ) {
            if (selectedTestId.isBlank()) {
                emptyList()
            } else {
                QuestionStore.findQuestionsByTestAndCoaching(
                    testId = selectedTestId,
                    coachingId = coachingId
                )
            }
        }

    val totalMarks =
        questions.sumOf {
            it.marks
        }

    // No nested Scaffold — parent AppDashboard already has TopAppBar
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ChipColors.blue.container
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = ChipColors.blue.icon,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "Test Questions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (
                                        user.role == UserRole.TEACHER
                                    ) {
                                        "Manage questions for your assigned batches."
                                    } else {
                                        "Create and manage questions for tests."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item {

                BatchDropdown(
                    batches = availableBatches,
                    selectedBatch = selectedBatch,
                    onBatchSelected = {
                        selectedBatchId = it.id
                        selectedTestId = ""
                    }
                )
            }

            item {

                TestDropdown(
                    tests = availableTests,
                    selectedTest = selectedTest,
                    onTestSelected = {
                        selectedTestId = it.id
                    }
                )
            }

            item {

                if (selectedTest != null) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ChipColors.purple.container
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Text(
                                text = selectedTest.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = if (
                                    selectedTest.subject.isBlank()
                                ) {
                                    "Subject not specified"
                                } else {
                                    "Subject: ${selectedTest.subject}"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {

                                SummaryBox(
                                    modifier = Modifier.weight(1f),
                                    title = "Questions",
                                    value = questions.size.toString(),
                                    background = ChipColors.indigo.container
                                )

                                SummaryBox(
                                    modifier = Modifier.weight(1f),
                                    title = "Question Marks",
                                    value = formatQuestionNumber(
                                        totalMarks
                                    ),
                                    background = ChipColors.teal.container
                                )

                                SummaryBox(
                                    modifier = Modifier.weight(1f),
                                    title = "Test Marks",
                                    value = formatQuestionNumber(
                                        selectedTest.totalMarks
                                    ),
                                    background = ChipColors.orange.container
                                )
                            }
                        }
                    }
                }
            }

            item {

                if (selectedTest != null) {

                    Button(
                        onClick = {
                            editingQuestion = null
                            showQuestionDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "Add New Question"
                        )
                    }
                }
            }

            if (
                selectedTest != null &&
                questions.isEmpty()
            ) {

                item {

                    EmptyQuestionsCard()
                }
            }

            items(
                items = questions,
                key = {
                    it.id
                }
            ) { question ->

                QuestionCard(
                    question = question,
                    onEdit = {
                        editingQuestion = question
                        showQuestionDialog = true
                    },
                    onDelete = {

                        QuestionStore.deleteQuestion(
                            questionId = question.id
                        )

                        refreshKey++
                    }
                )
            }

            item {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }

        SLHBottomNavBar(
            items = BottomNavItems.teacher,
            currentRoute = "questions",
            onNavigate = { route ->
                if (route == "questions") {
                    // already here
                } else if (route == "dashboard") {
                    onBack()
                } else {
                    onNavigateTab(route)
                }
            }
        )
    }

    if (showQuestionDialog && selectedTest != null) {

        QuestionEditorDialog(
            test = selectedTest,
            coachingId = coachingId,
            existingQuestion = editingQuestion,
            nextOrder = questions.size + 1,
            onDismiss = {
                showQuestionDialog = false
                editingQuestion = null
            },
            onSaved = {

                showQuestionDialog = false
                editingQuestion = null
                refreshKey++
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchDropdown(
    batches: List<CoachingBatch>,
    selectedBatch: CoachingBatch?,
    onBatchSelected: (CoachingBatch) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = selectedBatch?.let {
                if (it.code.isBlank()) {
                    it.name
                } else {
                    "${it.name} (${it.code})"
                }
            } ?: "",
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Select Batch")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            if (batches.isEmpty()) {

                DropdownMenuItem(
                    text = {
                        Text(
                            "No assigned batch available"
                        )
                    },
                    onClick = {
                        expanded = false
                    }
                )

            } else {

                batches.forEach { batch ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (batch.code.isBlank()) {
                                    batch.name
                                } else {
                                    "${batch.name} (${batch.code})"
                                }
                            )
                        },
                        onClick = {
                            onBatchSelected(batch)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestDropdown(
    tests: List<CoachingTest>,
    selectedTest: CoachingTest?,
    onTestSelected: (CoachingTest) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = selectedTest?.let {
                if (it.subject.isBlank()) {
                    it.title
                } else {
                    "${it.title} • ${it.subject}"
                }
            } ?: "",
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Select Test")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            if (tests.isEmpty()) {

                DropdownMenuItem(
                    text = {
                        Text(
                            "No tests available for this batch"
                        )
                    },
                    onClick = {
                        expanded = false
                    }
                )

            } else {

                tests.forEach { test ->

                    DropdownMenuItem(
                        text = {

                            Column {

                                Text(
                                    text = test.title,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (test.subject.isNotBlank()) {

                                    Text(
                                        text = test.subject,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        onClick = {
                            onTestSelected(test)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBox(
    modifier: Modifier,
    title: String,
    value: String,
    background: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = background
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: TestQuestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.Top
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = ChipColors.indigo.container,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = question.questionOrder.toString(),
                        fontWeight = FontWeight.Bold,
                        color = ChipColors.indigo.icon
                    )
                }

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Marks: ${formatQuestionNumber(question.marks)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Question",
                        tint = ChipColors.blue.icon
                    )
                }

                IconButton(
                    onClick = {
                        showDeleteDialog = true
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Question",
                        tint = ChipColors.red.icon
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OptionRow(
                label = "A",
                text = question.optionA,
                isCorrect = question.correctOption.equals(
                    "A",
                    ignoreCase = true
                )
            )

            OptionRow(
                label = "B",
                text = question.optionB,
                isCorrect = question.correctOption.equals(
                    "B",
                    ignoreCase = true
                )
            )

            OptionRow(
                label = "C",
                text = question.optionC,
                isCorrect = question.correctOption.equals(
                    "C",
                    ignoreCase = true
                )
            )

            OptionRow(
                label = "D",
                text = question.optionD,
                isCorrect = question.correctOption.equals(
                    "D",
                    ignoreCase = true
                )
            )
        }
    }

    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Delete Question?")
            },
            text = {
                Text(
                    "This question will be permanently removed from this test."
                )
            },
            confirmButton = {

                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    text: String,
    isCorrect: Boolean
) {

    val background =
        if (isCorrect) {
            ChipColors.green.container
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    val borderColor =
        if (isCorrect) {
            ChipColors.green.icon
        } else {
            MaterialTheme.colorScheme.outline
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                color = background,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "$label.",
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = text.ifBlank {
                "Option not entered"
            },
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        if (isCorrect) {

            Text(
                text = "✓",
                fontWeight = FontWeight.Bold,
                color = ChipColors.green.icon
            )
        }
    }
}

@Composable
private fun EmptyQuestionsCard() {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ChipColors.amber.container
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = ChipColors.orange.icon
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "No questions yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Add questions to this test to prepare it for students.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditorDialog(
    test: CoachingTest,
    coachingId: String,
    existingQuestion: TestQuestion?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    var questionText by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.questionText ?: ""
        )
    }

    var optionA by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.optionA ?: ""
        )
    }

    var optionB by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.optionB ?: ""
        )
    }

    var optionC by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.optionC ?: ""
        )
    }

    var optionD by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.optionD ?: ""
        )
    }

    var correctOption by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.correctOption ?: ""
        )
    }

    var marksText by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.marks?.let {
                formatQuestionNumber(it)
            } ?: "1"
        )
    }

    var orderText by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.questionOrder?.toString()
                ?: nextOrder.toString()
        )
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(22.dp)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {

                    Text(
                        text = if (
                            existingQuestion == null
                        ) {
                            "Add Question"
                        } else {
                            "Edit Question"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = test.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {

                    OutlinedTextField(
                        value = questionText,
                        onValueChange = {
                            questionText = it
                            errorMessage = ""
                        },
                        label = {
                            Text("Question")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                item {

                    OutlinedTextField(
                        value = optionA,
                        onValueChange = {
                            optionA = it
                            errorMessage = ""
                        },
                        label = {
                            Text("Option A")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {

                    OutlinedTextField(
                        value = optionB,
                        onValueChange = {
                            optionB = it
                            errorMessage = ""
                        },
                        label = {
                            Text("Option B")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {

                    OutlinedTextField(
                        value = optionC,
                        onValueChange = {
                            optionC = it
                            errorMessage = ""
                        },
                        label = {
                            Text("Option C")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {

                    OutlinedTextField(
                        value = optionD,
                        onValueChange = {
                            optionD = it
                            errorMessage = ""
                        },
                        label = {
                            Text("Option D")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {

                    Text(
                        text = "Correct Answer",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        CorrectOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "A",
                            selected =
                                correctOption.equals(
                                    "A",
                                    ignoreCase = true
                                ),
                            onClick = {
                                correctOption = "A"
                                errorMessage = ""
                            }
                        )

                        CorrectOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "B",
                            selected =
                                correctOption.equals(
                                    "B",
                                    ignoreCase = true
                                ),
                            onClick = {
                                correctOption = "B"
                                errorMessage = ""
                            }
                        )

                        CorrectOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "C",
                            selected =
                                correctOption.equals(
                                    "C",
                                    ignoreCase = true
                                ),
                            onClick = {
                                correctOption = "C"
                                errorMessage = ""
                            }
                        )

                        CorrectOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "D",
                            selected =
                                correctOption.equals(
                                    "D",
                                    ignoreCase = true
                                ),
                            onClick = {
                                correctOption = "D"
                                errorMessage = ""
                            }
                        )
                    }
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        OutlinedTextField(
                            value = marksText,
                            onValueChange = {
                                marksText = it
                                errorMessage = ""
                            },
                            label = {
                                Text("Marks")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = orderText,
                            onValueChange = {
                                orderText = it
                                errorMessage = ""
                            },
                            label = {
                                Text("Question No.")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {

                    item {

                        Text(
                            text = errorMessage,
                            color = ChipColors.red.icon,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {

                                val cleanQuestion =
                                    questionText.trim()

                                val cleanA =
                                    optionA.trim()

                                val cleanB =
                                    optionB.trim()

                                val cleanC =
                                    optionC.trim()

                                val cleanD =
                                    optionD.trim()

                                val marks =
                                    marksText
                                        .trim()
                                        .toDoubleOrNull()

                                val order =
                                    orderText
                                        .trim()
                                        .toIntOrNull()

                                when {

                                    cleanQuestion.isBlank() -> {
                                        errorMessage =
                                            "Please enter the question."
                                    }

                                    cleanA.isBlank() -> {
                                        errorMessage =
                                            "Please enter Option A."
                                    }

                                    cleanB.isBlank() -> {
                                        errorMessage =
                                            "Please enter Option B."
                                    }

                                    cleanC.isBlank() -> {
                                        errorMessage =
                                            "Please enter Option C."
                                    }

                                    cleanD.isBlank() -> {
                                        errorMessage =
                                            "Please enter Option D."
                                    }

                                    correctOption !in
                                            listOf(
                                                "A",
                                                "B",
                                                "C",
                                                "D"
                                            ) -> {
                                        errorMessage =
                                            "Please select the correct answer."
                                    }

                                    marks == null ||
                                            marks <= 0 -> {
                                        errorMessage =
                                            "Marks must be greater than 0."
                                    }

                                    order == null ||
                                            order <= 0 -> {
                                        errorMessage =
                                            "Question number must be greater than 0."
                                    }

                                    else -> {

                                        if (
                                            existingQuestion == null
                                        ) {

                                            QuestionStore.createQuestion(
                                                testId = test.id,
                                                coachingId = coachingId,
                                                batchId = test.batchId,
                                                questionText = cleanQuestion,
                                                optionA = cleanA,
                                                optionB = cleanB,
                                                optionC = cleanC,
                                                optionD = cleanD,
                                                correctOption = correctOption,
                                                marks = marks,
                                                questionType = QuestionType.MCQ,
                                                questionOrder = order
                                            )

                                        } else {

                                            QuestionStore.updateQuestion(
                                                existingQuestion.copy(
                                                    questionText = cleanQuestion,
                                                    optionA = cleanA,
                                                    optionB = cleanB,
                                                    optionC = cleanC,
                                                    optionD = cleanD,
                                                    correctOption = correctOption,
                                                    marks = marks,
                                                    questionOrder = order
                                                )
                                            )
                                        }

                                        onSaved()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {

                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.width(6.dp)
                            )

                            Text(
                                text = if (
                                    existingQuestion == null
                                ) {
                                    "Save"
                                } else {
                                    "Update"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectOptionButton(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    if (selected) {

        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }

    } else {

        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    }
}

private fun formatQuestionNumber(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {
        String.format(
            Locale.US,
            "%.0f",
            value
        )
    } else {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    }
}