
package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTestManagementScreen(
    user: DemoUser,
    onBack: () -> Unit
) {

    val coachingId = user.coachingId ?: ""

    // All ACTIVE batches of this coaching — shared across all teachers.
    val assignedBatches =
        BatchStore.findBatchesByCoaching(
            coachingId = coachingId
        ).filter {
            it.status.equals("ACTIVE", ignoreCase = true)
        }

    var selectedBatch by remember {
        mutableStateOf<CoachingBatch?>(null)
    }

    var batchSelectorOpen by remember {
        mutableStateOf(false)
    }

    var showForm by remember {
        mutableStateOf(false)
    }

    var editingTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    var showQuestionManagement by remember {
        mutableStateOf(false)
    }

    if (showQuestionManagement) {

        QuestionManagementScreen(
            user = user,
            onBack = {
                showQuestionManagement = false
            }
        )

        BackHandler {
            showQuestionManagement = false
        }

        return
    }

    if (showForm) {

        TeacherTestForm(
            user = user,
            batch = selectedBatch,
            existingTest = editingTest,
            onCancel = {
                showForm = false
                editingTest = null
            },
            onSaved = {
                showForm = false
                editingTest = null
            }
        )

        BackHandler {
            showForm = false
            editingTest = null
        }

        return
    }

    BackHandler {
        onBack()
    }

    val tests =
        if (selectedBatch != null) {
            TestStore.findTestsByBatchAndCoaching(
                batchId = selectedBatch!!.id,
                coachingId = coachingId
            )
        } else {
            emptyList()
        }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text("Tests & Questions")
                },
                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
        ) {

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                item {

                    Column(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                    ) {

                        Text(
                            text = "My Tests",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Create tests and manage questions for your assigned batches.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                item {

                    TeacherBatchSelector(
                        batches = assignedBatches,
                        selectedBatch = selectedBatch,
                        expanded = batchSelectorOpen,
                        onExpand = {
                            batchSelectorOpen =
                                !batchSelectorOpen
                        },
                        onSelect = { batch ->

                            selectedBatch = batch
                            batchSelectorOpen = false
                        }
                    )
                }

                if (selectedBatch == null) {

                    item {

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp
                                    ),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color(0xFFE8EAF6)
                                ),
                            shape =
                                RoundedCornerShape(18.dp)
                        ) {

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Groups,
                                    contentDescription =
                                        null,
                                    modifier =
                                        Modifier.size(56.dp),
                                    tint =
                                        Color(0xFF3949AB)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                Text(
                                    text =
                                        "Select a batch",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "All active batches of your coaching are available.",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }

                } else {

                    item {

                        TeacherSelectedBatchCard(
                            batch = selectedBatch!!
                        )
                    }

                    item {

                        Button(
                            onClick = {

                                editingTest = null
                                showForm = true
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp
                                    )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Add,
                                contentDescription =
                                    null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text("Create New Test")
                        }
                    }

                    if (tests.isEmpty()) {

                        item {

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 16.dp
                                        ),
                                shape =
                                    RoundedCornerShape(16.dp)
                            ) {

                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Assessment,
                                        contentDescription =
                                            null,
                                        modifier =
                                            Modifier.size(52.dp),
                                        tint =
                                            Color(0xFF5E35B1)
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(10.dp)
                                    )

                                    Text(
                                        text =
                                            "No tests created yet",
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        text =
                                            "Create your first test for this batch.",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }
                        }

                    } else {

                        items(
                            items = tests,
                            key = {
                                it.id
                            }
                        ) { test ->

                            TeacherTestCard(
                                test = test,

                                onEdit = {
                                    if (test.status.canTeacherEdit()) {
                                        editingTest = test
                                        showForm = true
                                    }
                                },

                                onDelete = {

                                    TestStore.deleteTest(
                                        testId = test.id
                                    )
                                },

                                onQuestions = {

                                    showQuestionManagement =
                                        true
                                },

                                onSubmitForApproval = {

                                    TestStore.submitForApproval(
                                        testId = test.id
                                    )
                                },

                                onMarkCompleted = {

                                    if (test.status.isVisibleToStudent()) {
                                        TestStore.setTestStatus(
                                            testId = test.id,
                                            status = TestStatus.COMPLETED
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherBatchSelector(
    batches: List<CoachingBatch>,
    selectedBatch: CoachingBatch?,
    expanded: Boolean,
    onExpand: () -> Unit,
    onSelect: (CoachingBatch) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
    ) {

        Text(
            text = "Select Batch",
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(14.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFFF5F7FF)
                ),
            onClick = onExpand
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Groups,
                    contentDescription =
                        null,
                    tint =
                        Color(0xFF3155D9)
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            selectedBatch?.name
                                ?: "Choose your batch",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    if (
                        selectedBatch != null &&
                        selectedBatch.course.isNotBlank()
                    ) {

                        Text(
                            text =
                                selectedBatch.course,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                Text(
                    text =
                        if (expanded) "▲" else "▼",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        if (expanded) {

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            if (batches.isEmpty()) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "No active batch assigned to you.",
                        modifier =
                            Modifier.padding(16.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

            } else {

                batches.forEach { batch ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 3.dp
                                ),
                        shape =
                            RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (
                                        selectedBatch?.id ==
                                        batch.id
                                    ) {
                                        Color(0xFFE3F2FD)
                                    } else {
                                        Color.White
                                    }
                            ),
                        onClick = {
                            onSelect(batch)
                        }
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {

                            Text(
                                text =
                                    batch.name,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            if (
                                batch.course.isNotBlank()
                            ) {

                                Text(
                                    text =
                                        batch.course,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }

                            if (
                                batch.subject.isNotBlank()
                            ) {

                                Text(
                                    text =
                                        "Subject: ${batch.subject}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        Color(0xFF3155D9)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherSelectedBatchCard(
    batch: CoachingBatch
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFE8F5E9)
            ),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier =
                Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            color =
                                Color(0xFF43A047),
                            shape =
                                RoundedCornerShape(12.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.School,
                    contentDescription =
                        null,
                    tint =
                        Color.White
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column {

                Text(
                    text =
                        batch.name,
                    fontWeight =
                        FontWeight.Bold
                )

                if (
                    batch.course.isNotBlank()
                ) {

                    Text(
                        text =
                            batch.course,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                if (
                    batch.subject.isNotBlank()
                ) {

                    Text(
                        text =
                            "Subject: ${batch.subject}",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherTestForm(
    user: DemoUser,
    batch: CoachingBatch?,
    existingTest: CoachingTest?,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {

    if (batch == null) {
        onCancel()
        return
    }

    val coachingId =
        user.coachingId ?: ""

    val teacher =
        TeacherStore.resolveTeacherForUser(user)

    val teacherSubject =
        teacher?.subject?.trim() ?: ""

    val batchSubject =
        batch.subject.trim()

    val existingSubjects =
        TestStore.findTestsByBatchAndCoaching(
            batchId = batch.id,
            coachingId = coachingId
        )
            .map {
                it.subject.trim()
            }
            .filter {
                it.isNotBlank()
            }

    val subjectOptions =
        buildList {

            if (
                teacherSubject.isNotBlank()
            ) {
                add(teacherSubject)
            }

            if (
                batchSubject.isNotBlank()
            ) {
                add(batchSubject)
            }

            existingSubjects.forEach {
                add(it)
            }

        }.distinct()

    var title by remember(existingTest) {
        mutableStateOf(
            existingTest?.title ?: ""
        )
    }

    var subject by remember(existingTest) {
        mutableStateOf(
            existingTest?.subject
                ?: teacherSubject.ifBlank {
                    batchSubject
                }
        )
    }

    var testDate by remember(existingTest) {
        mutableStateOf(
            existingTest?.testDate
                ?: AttendanceStatusStore.todayDate()
        )
    }

    var totalMarks by remember(existingTest) {
        mutableStateOf(
            if (existingTest != null) {
                formatTeacherNumber(
                    existingTest.totalMarks
                )
            } else {
                ""
            }
        )
    }

    var passingMarks by remember(existingTest) {
        mutableStateOf(
            if (existingTest != null) {
                formatTeacherNumber(
                    existingTest.passingMarks
                )
            } else {
                ""
            }
        )
    }

    var description by remember(existingTest) {
        mutableStateOf(
            existingTest?.description ?: ""
        )
    }

    var testType by remember(existingTest) {
        mutableStateOf(
            existingTest?.testType ?: TestType.QUIZ
        )
    }

    var durationMinutes by remember(existingTest) {
        mutableStateOf(
            if (existingTest != null && existingTest.durationMinutes > 0) {
                existingTest.durationMinutes.toString()
            } else {
                ""
            }
        )
    }

    var subjectSelectorOpen by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        if (existingTest == null) {
                            "Create Test"
                        } else {
                            "Edit Test"
                        }
                    )
                },
                navigationIcon = {

                    IconButton(
                        onClick = onCancel
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 16.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                TeacherSelectedBatchCard(
                    batch = batch
                )
            }

            item {

                OutlinedTextField(
                    value = title,
                    onValueChange = {

                        title = it
                        errorMessage = ""
                    },
                    label = {
                        Text("Test Title")
                    },
                    placeholder = {
                        Text("e.g. Unit Test 1")
                    },
                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Quiz,
                            contentDescription =
                                null
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Subject",
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(14.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFF5F7FF)
                            ),
                        onClick = {

                            if (
                                subjectOptions.isNotEmpty()
                            ) {
                                subjectSelectorOpen =
                                    !subjectSelectorOpen
                            }
                        }
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        subject.ifBlank {
                                            "Select Subject"
                                        },
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    color =
                                        if (
                                            subject.isBlank()
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface
                                        }
                                )
                            }

                            Text(
                                text =
                                    if (
                                        subjectSelectorOpen
                                    ) {
                                        "▲"
                                    } else {
                                        "▼"
                                    },
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    if (
                        subjectSelectorOpen
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        subjectOptions.forEach { option ->

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 2.dp
                                        ),
                                shape =
                                    RoundedCornerShape(10.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            if (
                                                subject ==
                                                option
                                            ) {
                                                Color(0xFFE3F2FD)
                                            } else {
                                                Color.White
                                            }
                                    ),
                                onClick = {

                                    subject = option
                                    subjectSelectorOpen =
                                        false
                                    errorMessage = ""
                                }
                            ) {

                                Text(
                                    text =
                                        option,
                                    modifier =
                                        Modifier.padding(
                                            14.dp
                                        ),
                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item {

                OutlinedTextField(
                    value = testDate,
                    onValueChange = {
                        testDate = it
                    },
                    label = {
                        Text("Test Date")
                    },
                    placeholder = {
                        Text("YYYY-MM-DD")
                    },
                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.CalendarMonth,
                            contentDescription =
                                null
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {

                OutlinedTextField(
                    value = totalMarks,
                    onValueChange = {

                        totalMarks =
                            it.filter { character ->
                                character.isDigit() ||
                                        character == '.'
                            }
                    },
                    label = {
                        Text("Total Marks")
                    },
                    placeholder = {
                        Text("e.g. 100")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {

                OutlinedTextField(
                    value = passingMarks,
                    onValueChange = {

                        passingMarks =
                            it.filter { character ->
                                character.isDigit() ||
                                        character == '.'
                            }
                    },
                    label = {
                        Text("Passing Marks")
                    },
                    placeholder = {
                        Text("e.g. 40")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                    },
                    label = {
                        Text("Instructions / Description")
                    },
                    placeholder = {
                        Text(
                            "Enter test instructions"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6
                )
            }

            item {
                Text(
                    text = "Test Type",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { testType = TestType.LIVE },
                        modifier = Modifier.weight(1f),
                        colors = if (testType == TestType.LIVE) {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text("Live Test")
                    }
                    OutlinedButton(
                        onClick = { testType = TestType.QUIZ },
                        modifier = Modifier.weight(1f),
                        colors = if (testType == TestType.QUIZ) {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text("Quiz")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    placeholder = { Text("e.g. 30") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }

            item {

                if (
                    errorMessage.isNotBlank()
                ) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFFFEBEE)
                            )
                    ) {

                        Text(
                            text = errorMessage,
                            color =
                                Color(0xFFC62828),
                            modifier =
                                Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedButton(
                        onClick = onCancel,
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text("Cancel")
                    }

                    Button(
                        onClick = {

                            val total =
                                totalMarks.toDoubleOrNull()

                            val passing =
                                passingMarks.toDoubleOrNull()

                            when {

                                title.trim().isBlank() -> {

                                    errorMessage =
                                        "Please enter test title."
                                }

                                subject.trim().isBlank() -> {

                                    errorMessage =
                                        "Please select or enter subject."
                                }

                                testDate.trim().isBlank() -> {

                                    errorMessage =
                                        "Please enter test date."
                                }

                                total == null ||
                                        total <= 0.0 -> {

                                    errorMessage =
                                        "Please enter valid total marks."
                                }

                                passing == null ||
                                        passing < 0.0 -> {

                                    errorMessage =
                                        "Please enter valid passing marks."
                                }

                                passing > total -> {

                                    errorMessage =
                                        "Passing marks cannot be greater than total marks."
                                }

                                else -> {

                                    if (
                                        existingTest == null
                                    ) {

                                        TestStore.createTest(

                                            coachingId =
                                                coachingId,

                                            batchId =
                                                batch.id,

                                            title =
                                                title.trim(),

                                            subject =
                                                subject.trim(),

                                            testDate =
                                                testDate.trim(),

                                            totalMarks =
                                                total,

                                            passingMarks =
                                                passing,

                                            description =
                                                description.trim(),

                                            createdById =
                                                user.id,

                                            createdByName =
                                                user.displayName,

                                            testType =
                                                testType,

                                            durationMinutes =
                                                durationMinutes.toIntOrNull() ?: 0
                                        )

                                    } else {

                                        TestStore.updateTest(

                                            testId =
                                                existingTest.id,

                                            title =
                                                title.trim(),

                                            subject =
                                                subject.trim(),

                                            testDate =
                                                testDate.trim(),

                                            totalMarks =
                                                total,

                                            passingMarks =
                                                passing,

                                            description =
                                                description.trim(),

                                            testType =
                                                testType,

                                            durationMinutes =
                                                durationMinutes.toIntOrNull()
                                        )
                                    }

                                    onSaved()
                                }
                            }
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Save,
                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(
                            if (
                                existingTest == null
                            ) {
                                "Create Test"
                            } else {
                                "Save Changes"
                            }
                        )
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TeacherTestCard(
    test: CoachingTest,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuestions: () -> Unit,
    onSubmitForApproval: () -> Unit,
    onMarkCompleted: () -> Unit
) {

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Delete Test?")
            },
            text = {
                Text(
                    "This test and its saved results will be deleted."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        showDeleteDialog = false
                        onDelete()
                    }
                ) {

                    Text(
                        "Delete",
                        color =
                            Color(0xFFC62828)
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(18.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(
                                color =
                                    Color(0xFFEDE7F6),
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Assessment,
                        contentDescription =
                            null,
                        tint =
                            Color(0xFF5E35B1)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            test.title,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        test.subject.isNotBlank()
                    ) {

                        Text(
                            text =
                                test.subject,
                            color =
                                Color(0xFF3155D9),
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    if (
                        test.testDate.isNotBlank()
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Today,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(16.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(4.dp)
                            )

                            Text(
                                text =
                                    test.testDate,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }

                Text(
                    text =
                        when (test.status) {
                            TestStatus.DRAFT -> "DRAFT"
                            TestStatus.PENDING_APPROVAL -> "PENDING"
                            TestStatus.APPROVED -> "APPROVED"
                            TestStatus.PUBLISHED -> "PUBLISHED"
                            TestStatus.REJECTED -> "REJECTED"
                            TestStatus.COMPLETED -> "COMPLETED"
                            TestStatus.CANCELLED -> "CANCELLED"
                            TestStatus.ACTIVE -> "ACTIVE"
                        },
                    color =
                        when (test.status) {
                            TestStatus.DRAFT -> Color(0xFF757575)
                            TestStatus.PENDING_APPROVAL -> Color(0xFFF57C00)
                            TestStatus.APPROVED,
                            TestStatus.PUBLISHED,
                            TestStatus.ACTIVE -> Color(0xFF2E7D32)
                            TestStatus.REJECTED -> Color(0xFFC62828)
                            TestStatus.COMPLETED -> Color(0xFF1565C0)
                            TestStatus.CANCELLED -> Color(0xFF757575)
                        },
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                TeacherInfoBox(
                    label = "Total",
                    value =
                        formatTeacherNumber(
                            test.totalMarks
                        ),
                    modifier =
                        Modifier.weight(1f)
                )

                TeacherInfoBox(
                    label = "Pass",
                    value =
                        formatTeacherNumber(
                            test.passingMarks
                        ),
                    modifier =
                        Modifier.weight(1f)
                )

                TeacherInfoBox(
                    label = "Questions",
                    value =
                        QuestionStore
                            .getQuestionCount(
                                test.id
                            )
                            .toString(),
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (
                test.description.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        test.description,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            // Type + duration chip
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildString {
                    append(
                        if (test.testType == TestType.LIVE) "Live Test"
                        else "Quiz"
                    )
                    if (test.durationMinutes > 0) {
                        append(" · ${test.durationMinutes} min")
                    }
                    if (test.startTime.isNotBlank() || test.endTime.isNotBlank()) {
                        append(" · ${test.startTime}")
                        if (test.endTime.isNotBlank()) {
                            append("–${test.endTime}")
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5E35B1),
                fontWeight = FontWeight.SemiBold
            )

            if (
                test.status == TestStatus.REJECTED &&
                test.rejectionReason.isNotBlank()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rejected: ${test.rejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Button(
                onClick = onQuestions,
                modifier =
                    Modifier.fillMaxWidth(),
                enabled = test.status.canTeacherEdit() ||
                        test.status == TestStatus.PENDING_APPROVAL
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Quiz,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )

                Text("Manage Questions")
            }

            // Submit / Resubmit for Admin Approval
            if (test.status.canSubmitForApproval()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSubmitForApproval,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0)
                    )
                ) {
                    Text(
                        if (test.status == TestStatus.REJECTED)
                            "Resubmit for Approval"
                        else
                            "Submit for Admin Approval"
                    )
                }
            }

            if (test.status == TestStatus.PENDING_APPROVAL) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Waiting for Admin approval…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                if (test.status.canTeacherEdit()) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Edit,
                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )

                        Text("Edit")
                    }
                }

                if (test.status.isVisibleToStudent()) {
                    OutlinedButton(
                        onClick = onMarkCompleted,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("Mark Completed")
                    }
                }

                IconButton(
                    onClick = {
                        showDeleteDialog = true
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            "Delete",
                        tint =
                            Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherInfoBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFF5F5F5)
            ),
        shape =
            RoundedCornerShape(10.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(8.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = label,
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text = value,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

private fun formatTeacherNumber(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {

        value.toInt().toString()

    } else {

        String.format(
            java.util.Locale.US,
            "%.2f",
            value
        )
    }
}