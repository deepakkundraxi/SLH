package com.slh.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestManagementScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(refreshKey) {
        // Forces recomposition when local Store data changes.
    }

    val batches =
        BatchStore.findBatchesByCoaching(
            coachingId
        )

    var selectedBatchId by remember {
        mutableStateOf("")
    }

    var batchExpanded by remember {
        mutableStateOf(false)
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var editingTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    var deleteTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    var statusTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    val selectedBatch =
        batches.firstOrNull {
            it.id == selectedBatchId
        }

    val tests =
        if (selectedBatchId.isBlank()) {
            TestStore.findTestsByCoaching(
                coachingId
            )
        } else {
            TestStore.findTestsByBatchAndCoaching(
                batchId = selectedBatchId,
                coachingId = coachingId
            )
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Test Management",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tests & question bank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text = "Select Batch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = batchExpanded,
                    onExpandedChange = {
                        batchExpanded = !batchExpanded
                    }
                ) {

                    OutlinedTextField(
                        value =
                            selectedBatch?.name
                                ?: "All Batches",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = {
                            Text("Batch")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = batchExpanded
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = batchExpanded,
                        onDismissRequest = {
                            batchExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("All Batches")
                            },
                            onClick = {
                                selectedBatchId = ""
                                batchExpanded = false
                            }
                        )

                        batches.forEach { batch ->

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        batch.name
                                    )
                                },
                                onClick = {
                                    selectedBatchId =
                                        batch.id

                                    batchExpanded =
                                        false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    onClick = {
                        if (selectedBatchId.isNotBlank()) {
                            showAddDialog = true
                        }
                    },
                    enabled = selectedBatchId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "Create New Test"
                    )
                }

                if (selectedBatchId.isBlank()) {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Select a batch to create a new test.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tests.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "No Tests Found",
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                if (selectedBatchId.isBlank())
                                    "No tests have been created yet."
                                else
                                    "No tests found for this batch.",
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout
                        .PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    item {

                        Text(
                            text =
                                "Tests (${tests.size})",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        items = tests,
                        key = {
                            it.id
                        }
                    ) { test ->

                        TestManagementCard(
                            test = test,
                            onEdit = {
                                editingTest = test
                            },
                            onDelete = {
                                deleteTest = test
                            },
                            onStatusClick = {
                                statusTest = test
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {

        TestFormDialog(
            title = "Create Test",
            batchName =
                selectedBatch?.name
                    ?: "",
            initialTest = null,
            onDismiss = {
                showAddDialog = false
            },
            onSave = { title,
                       subject,
                       testDate,
                       totalMarks,
                       passingMarks,
                       description ->

                TestStore.createTest(
                    coachingId = coachingId,
                    batchId = selectedBatchId,
                    title = title,
                    subject = subject,
                    testDate = testDate,
                    totalMarks = totalMarks,
                    passingMarks = passingMarks,
                    description = description
                )

                showAddDialog = false
                refreshKey++
            }
        )
    }

    editingTest?.let { test ->

        TestFormDialog(
            title = "Edit Test",
            batchName =
                batches.firstOrNull {
                    it.id == test.batchId
                }?.name ?: "",
            initialTest = test,
            onDismiss = {
                editingTest = null
            },
            onSave = { title,
                       subject,
                       testDate,
                       totalMarks,
                       passingMarks,
                       description ->

                TestStore.updateTest(
                    testId = test.id,
                    title = title,
                    subject = subject,
                    testDate = testDate,
                    totalMarks = totalMarks,
                    passingMarks = passingMarks,
                    description = description
                )

                editingTest = null
                refreshKey++
            }
        )
    }

    deleteTest?.let { test ->

        AlertDialog(
            onDismissRequest = {
                deleteTest = null
            },
            title = {
                Text("Delete Test?")
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${test.title}\"? Its saved results will also be removed."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        TestStore.deleteTest(
                            test.id
                        )

                        deleteTest = null
                        refreshKey++
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        deleteTest = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    statusTest?.let { test ->

        AlertDialog(
            onDismissRequest = {
                statusTest = null
            },
            title = {
                Text("Change Test Status")
            },
            text = {

                Column {

                    Text(
                        text = test.title,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    TestStatusButton(
                        text = "Published",
                        onClick = {
                            TestStore.setTestStatus(
                                test.id,
                                TestStatus.PUBLISHED
                            )
                            statusTest = null
                            refreshKey++
                        }
                    )

                    TestStatusButton(
                        text = "Completed",
                        onClick = {
                            TestStore.setTestStatus(
                                test.id,
                                TestStatus.COMPLETED
                            )
                            statusTest = null
                            refreshKey++
                        }
                    )

                    TestStatusButton(
                        text = "Cancelled",
                        onClick = {
                            TestStore.setTestStatus(
                                test.id,
                                TestStatus.CANCELLED
                            )
                            statusTest = null
                            refreshKey++
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {

                TextButton(
                    onClick = {
                        statusTest = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}


@Composable
private fun TestManagementCard(
    test: CoachingTest,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusClick: () -> Unit
) {

    val statusText =
        when (test.status) {
            TestStatus.DRAFT -> "DRAFT"
            TestStatus.PENDING_APPROVAL -> "PENDING"
            TestStatus.APPROVED -> "APPROVED"
            TestStatus.PUBLISHED -> "PUBLISHED"
            TestStatus.REJECTED -> "REJECTED"
            TestStatus.COMPLETED -> "COMPLETED"
            TestStatus.CANCELLED -> "CANCELLED"
            TestStatus.ACTIVE -> "ACTIVE"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            MaterialTheme.colorScheme
                                .primaryContainer,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Quiz,
                        contentDescription = null
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = test.title,
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (test.subject.isNotBlank()) {

                        Text(
                            text = test.subject,
                            style =
                                MaterialTheme.typography
                                    .bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onStatusClick
                ) {
                    Text(statusText)
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                TestInfoBox(
                    title = "Test Date",
                    value =
                        test.testDate.ifBlank {
                            "Not set"
                        },
                    modifier =
                        Modifier.weight(1f)
                )

                TestInfoBox(
                    title = "Total Marks",
                    value =
                        formatMarks(
                            test.totalMarks
                        ),
                    modifier =
                        Modifier.weight(1f)
                )

                TestInfoBox(
                    title = "Pass Marks",
                    value =
                        formatMarks(
                            test.passingMarks
                        ),
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (test.description.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = test.description,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End
            ) {

                OutlinedButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text("Edit")
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            "Delete Test"
                    )
                }
            }
        }
    }
}


@Composable
private fun TestInfoBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme
                        .surfaceVariant
            )
    ) {

        Column(
            modifier = Modifier.padding(10.dp)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography
                        .labelSmall
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun TestStatusButton(
    text: String,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }

    Spacer(
        modifier = Modifier.height(6.dp)
    )
}


@Composable
private fun TestFormDialog(
    title: String,
    batchName: String,
    initialTest: CoachingTest?,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        Double,
        Double,
        String
    ) -> Unit
) {

    var testTitle by remember(initialTest) {
        mutableStateOf(
            initialTest?.title ?: ""
        )
    }

    var subject by remember(initialTest) {
        mutableStateOf(
            initialTest?.subject ?: ""
        )
    }

    var testDate by remember(initialTest) {
        mutableStateOf(
            initialTest?.testDate ?: ""
        )
    }

    var totalMarks by remember(initialTest) {
        mutableStateOf(
            if (initialTest != null)
                formatMarks(initialTest.totalMarks)
            else
                ""
        )
    }

    var passingMarks by remember(initialTest) {
        mutableStateOf(
            if (initialTest != null)
                formatMarks(initialTest.passingMarks)
            else
                ""
        )
    }

    var description by remember(initialTest) {
        mutableStateOf(
            initialTest?.description ?: ""
        )
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Text(
                    text = "Batch: $batchName",
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = testTitle,
                    onValueChange = {
                        testTitle = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Test Title")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = testDate,
                    onValueChange = {
                        testDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Test Date")
                    },
                    placeholder = {
                        Text("dd-MM-yyyy")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                Icons.Default.Today,
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = totalMarks,
                    onValueChange = {
                        totalMarks = it
                            .filter { char ->
                                char.isDigit() ||
                                        char == '.'
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Total Marks")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = passingMarks,
                    onValueChange = {
                        passingMarks = it
                            .filter { char ->
                                char.isDigit() ||
                                        char == '.'
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Passing Marks")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Description")
                    },
                    minLines = 3,
                    maxLines = 5
                )

                if (errorMessage.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = errorMessage,
                        color =
                            MaterialTheme.colorScheme
                                .error,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }
            }
        },
        confirmButton = {

            Button(
                onClick = {

                    val parsedTotal =
                        totalMarks.toDoubleOrNull()

                    val parsedPassing =
                        passingMarks.toDoubleOrNull()

                    when {
                        testTitle.isBlank() -> {
                            errorMessage =
                                "Please enter test title."
                        }

                        parsedTotal == null ||
                                parsedTotal <= 0.0 -> {
                            errorMessage =
                                "Please enter valid total marks."
                        }

                        parsedPassing == null ||
                                parsedPassing < 0.0 -> {
                            errorMessage =
                                "Please enter valid passing marks."
                        }

                        parsedPassing > parsedTotal -> {
                            errorMessage =
                                "Passing marks cannot be greater than total marks."
                        }

                        else -> {

                            onSave(
                                testTitle,
                                subject,
                                testDate,
                                parsedTotal,
                                parsedPassing,
                                description
                            )
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}


private fun formatMarks(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}