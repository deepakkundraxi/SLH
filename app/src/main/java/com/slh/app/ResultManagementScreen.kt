package com.slh.app

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun ResultManagementScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    androidx.activity.compose.BackHandler {
        onBack()
    }

    val batches =
        BatchStore.findBatchesByCoaching(coachingId)

    var selectedBatch by remember {
        mutableStateOf<CoachingBatch?>(null)
    }

    var selectedTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    var message by remember {
        mutableStateOf("")
    }

    val tests =
        selectedBatch?.let { batch ->
            TestStore.findTestsByBatchAndCoaching(
                batchId = batch.id,
                coachingId = coachingId
            )
        } ?: emptyList()

    val students =
        selectedBatch?.let { batch ->
            StudentStore.students.filter { student ->
                student.coachingId == coachingId &&
                        student.batchId == batch.id
            }.sortedBy {
                it.name.lowercase()
            }
        } ?: emptyList()

    val obtainedMarks =
        remember {
            mutableStateMapOf<String, String>()
        }

    val remarks =
        remember {
            mutableStateMapOf<String, String>()
        }

    val publishState =
        remember {
            mutableStateMapOf<String, Boolean>()
        }

    fun loadExistingResults(
        test: CoachingTest?
    ) {

        obtainedMarks.clear()
        remarks.clear()
        publishState.clear()

        if (test == null) {
            return
        }

        students.forEach { student ->

            val existing =
                TestStore.findResult(
                    testId = test.id,
                    studentId = student.id
                )

            if (existing != null) {

                obtainedMarks[student.id] =
                    formatNumber(existing.obtainedMarks)

                remarks[student.id] =
                    existing.remarks

                publishState[student.id] =
                    existing.status.equals(
                        "PUBLISHED",
                        ignoreCase = true
                    )
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * SUMMARY
     * ---------------------------------------------------------
     */

    val selectedTestResults =
        selectedTest?.let { test ->
            TestStore.getTestResults(test.id)
        } ?: emptyList()

    val enteredCount =
        selectedTestResults.size

    val publishedCount =
        selectedTestResults.count {
            it.status.equals(
                "PUBLISHED",
                ignoreCase = true
            )
        }

    val passedCount =
        selectedTest?.let { test ->
            selectedTestResults.count {
                it.obtainedMarks >= test.passingMarks
            }
        } ?: 0

    val failedCount =
        enteredCount - passedCount

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        /*
         * -----------------------------------------------------
         * HEADER
         * -----------------------------------------------------
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column {
                Text(
                    text = "Result Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage marks & results",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        /*
         * -----------------------------------------------------
         * BATCH SELECTION
         * -----------------------------------------------------
         */

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.secondaryContainer
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Groups,
                        contentDescription = null,
                        tint =
                            Color(0xFF5E35B1),
                        modifier =
                            Modifier.size(28.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Text(
                        text = "Select Batch",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (batches.isEmpty()) {

                    Text(
                        text =
                            "No batches found.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                } else {

                    batches.forEach { batch ->

                        val isSelected =
                            selectedBatch?.id == batch.id

                        Button(
                            onClick = {

                                selectedBatch =
                                    batch

                                selectedTest =
                                    null

                                obtainedMarks.clear()
                                remarks.clear()
                                publishState.clear()

                                message = ""
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        if (isSelected) {
                                            "✓ ${batch.name}"
                                        } else {
                                            batch.name
                                        },
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                if (
                                    batch.code.isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            batch.code,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
         * -----------------------------------------------------
         * TEST SELECTION
         * -----------------------------------------------------
         */

        if (selectedBatch != null) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Assessment,
                            contentDescription = null,
                            tint =
                                Color(0xFF1976D2),
                            modifier =
                                Modifier.size(28.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Text(
                            text = "Select Test",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    if (tests.isEmpty()) {

                        Text(
                            text =
                                "No tests found for this batch.",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                    } else {

                        tests.forEach { test ->

                            val isSelected =
                                selectedTest?.id == test.id

                            Button(
                                onClick = {

                                    selectedTest =
                                        test

                                    loadExistingResults(
                                        test
                                    )

                                    message = ""
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        text =
                                            if (isSelected) {
                                                "✓ ${test.title}"
                                            } else {
                                                test.title
                                            },
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        text =
                                            "Total: ${
                                                formatNumber(
                                                    test.totalMarks
                                                )
                                            } | Passing: ${
                                                formatNumber(
                                                    test.passingMarks
                                                )
                                            }",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
         * -----------------------------------------------------
         * TEST INFORMATION
         * -----------------------------------------------------
         */

        if (selectedTest != null) {

            val test =
                selectedTest!!

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.School,
                            contentDescription = null,
                            tint =
                                Color(0xFF2E7D32),
                            modifier =
                                Modifier.size(28.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Text(
                            text =
                                test.title,
                            fontWeight =
                                FontWeight.Bold,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )
                    }

                    Text(
                        text =
                            "Total Marks: ${
                                formatNumber(
                                    test.totalMarks
                                )
                            }"
                    )

                    Text(
                        text =
                            "Passing Marks: ${
                                formatNumber(
                                    test.passingMarks
                                )
                            }"
                    )

                    if (
                        test.subject.isNotBlank()
                    ) {

                        Text(
                            text =
                                "Subject: ${test.subject}"
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
                                    Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(6.dp)
                            )

                            Text(
                                text =
                                    "Test Date: ${test.testDate}"
                            )
                        }
                    }
                }
            }

            /*
             * -------------------------------------------------
             * RESULT SUMMARY
             * -------------------------------------------------
             */

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(14.dp)
                ) {

                    Text(
                        text =
                            "Result Summary",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        ResultSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            title = "Students",
                            value =
                                students.size.toString()
                        )

                        ResultSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            title = "Entered",
                            value =
                                enteredCount.toString()
                        )

                        ResultSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            title = "Published",
                            value =
                                publishedCount.toString()
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

                        ResultSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            title = "Passed",
                            value =
                                passedCount.toString()
                        )

                        ResultSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            title = "Failed",
                            value =
                                failedCount.toString()
                        )
                    }
                }
            }

            /*
             * -------------------------------------------------
             * STUDENTS
             * -------------------------------------------------
             */

            Text(
                text =
                    "Student Results",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            if (students.isEmpty()) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "No students found in this batch.",
                        modifier =
                            Modifier.padding(16.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

            } else {

                students.forEach { student ->

                    val currentMarks =
                        obtainedMarks[
                            student.id
                        ] ?: ""

                    val currentRemarks =
                        remarks[
                            student.id
                        ] ?: ""

                    val isPublished =
                        publishState[
                            student.id
                        ] ?: false

                    val marksValue =
                        currentMarks.toDoubleOrNull()

                    val percentage =
                        if (
                            marksValue != null &&
                            test.totalMarks > 0
                        ) {
                            (
                                    marksValue /
                                            test.totalMarks
                                    ) * 100.0
                        } else {
                            null
                        }

                    val resultStatus =
                        when {

                            marksValue == null ->
                                ""

                            marksValue >=
                                    test.passingMarks ->
                                "PASS"

                            else ->
                                "FAIL"
                        }

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                    MaterialTheme.colorScheme.surface
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            /*
                             * STUDENT NAME
                             */

                            Text(
                                text =
                                    student.name,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            if (
                                student.studentId
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        "Student ID: ${student.studentId}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium
                                )
                            }

                            /*
                             * MARKS
                             */

                            OutlinedTextField(

                                value =
                                    currentMarks,

                                onValueChange = { value ->

                                    if (
                                        value.isEmpty() ||
                                        value.toDoubleOrNull() != null
                                    ) {

                                        obtainedMarks[
                                            student.id
                                        ] = value
                                    }
                                },

                                label = {
                                    Text(
                                        "Obtained Marks"
                                    )
                                },

                                placeholder = {
                                    Text(
                                        "0 - ${
                                            formatNumber(
                                                test.totalMarks
                                            )
                                        }"
                                    )
                                },

                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType =
                                            KeyboardType.Decimal
                                    ),

                                singleLine = true,

                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            /*
                             * PERCENTAGE
                             */

                            if (
                                percentage != null
                            ) {

                                Text(
                                    text =
                                        "Percentage: ${
                                            formatPercentage(
                                                percentage
                                            )
                                        }%",
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }

                            /*
                             * PASS / FAIL
                             */

                            if (
                                resultStatus.isNotBlank()
                            ) {

                                val isPass =
                                    resultStatus ==
                                            "PASS"

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isPass) {
                                                    Color(
                                                        0xFFE8F5E9
                                                    )
                                                } else {
                                                    Color(
                                                        0xFFFFEBEE
                                                    )
                                                },
                                                RoundedCornerShape(
                                                    10.dp
                                                )
                                            )
                                            .padding(
                                                10.dp
                                            )
                                ) {

                                    Text(
                                        text =
                                            resultStatus,
                                        fontWeight =
                                            FontWeight.Bold,
                                        color =
                                            if (isPass) {
                                                Color(
                                                    0xFF2E7D32
                                                )
                                            } else {
                                                Color(
                                                    0xFFC62828
                                                )
                                            }
                                    )
                                }
                            }

                            /*
                             * REMARKS
                             */

                            OutlinedTextField(

                                value =
                                    currentRemarks,

                                onValueChange = { value ->

                                    remarks[
                                        student.id
                                    ] = value
                                },

                                label = {
                                    Text(
                                        "Remarks"
                                    )
                                },

                                minLines = 2,

                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            /*
                             * PUBLISH STATUS
                             */

                            Surface(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                color =
                                    if (isPublished) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                shape =
                                    RoundedCornerShape(
                                        10.dp
                                    )
                            ) {

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector =
                                            if (isPublished) {
                                                Icons.Default.Visibility
                                            } else {
                                                Icons.Default.VisibilityOff
                                            },
                                        contentDescription =
                                            null,
                                        modifier =
                                            Modifier.size(20.dp)
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(8.dp)
                                    )

                                    Text(
                                        text =
                                            if (isPublished) {
                                                "Published — Student can see result"
                                            } else {
                                                "Unpublished — Student cannot see result"
                                            },
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        modifier =
                                            Modifier.weight(1f)
                                    )
                                }
                            }

                            /*
                             * SAVE
                             */

                            Button(

                                onClick = {

                                    val marks =
                                        obtainedMarks[
                                            student.id
                                        ]?.toDoubleOrNull()

                                    if (marks == null) {

                                        message =
                                            "Enter marks for ${student.name}"

                                        return@Button
                                    }

                                    if (
                                        marks < 0 ||
                                        marks >
                                        test.totalMarks
                                    ) {

                                        message =
                                            "Marks must be between 0 and ${
                                                formatNumber(
                                                    test.totalMarks
                                                )
                                            }."

                                        return@Button
                                    }

                                    TestStore.addOrUpdateResult(

                                        testId =
                                            test.id,

                                        studentId =
                                            student.id,

                                        coachingId =
                                            coachingId,

                                        obtainedMarks =
                                            marks,

                                        remarks =
                                            remarks[
                                                student.id
                                            ] ?: "",

                                        resultDate =
                                            AttendanceStatusStore
                                                .todayDate(),

                                        status =
                                            if (
                                                publishState[
                                                    student.id
                                                ] == true
                                            ) {
                                                "PUBLISHED"
                                            } else {
                                                "UNPUBLISHED"
                                            }
                                    )

                                    message =
                                        if (
                                            publishState[
                                                student.id
                                            ] == true
                                        ) {
                                            "Result saved and published for ${student.name}"
                                        } else {
                                            "Result saved as unpublished for ${student.name}"
                                        }
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Save,
                                    contentDescription =
                                        null
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        "Save Result"
                                )
                            }

                            /*
                             * PUBLISH / UNPUBLISH
                             */

                            Button(

                                onClick = {

                                    val marks =
                                        obtainedMarks[
                                            student.id
                                        ]?.toDoubleOrNull()

                                    if (marks == null) {

                                        message =
                                            "Enter and save marks first."

                                        return@Button
                                    }

                                    if (
                                        marks < 0 ||
                                        marks >
                                        test.totalMarks
                                    ) {

                                        message =
                                            "Invalid marks."

                                        return@Button
                                    }

                                    val newStatus =
                                        if (
                                            isPublished
                                        ) {
                                            "UNPUBLISHED"
                                        } else {
                                            "PUBLISHED"
                                        }

                                    TestStore.addOrUpdateResult(

                                        testId =
                                            test.id,

                                        studentId =
                                            student.id,

                                        coachingId =
                                            coachingId,

                                        obtainedMarks =
                                            marks,

                                        remarks =
                                            remarks[
                                                student.id
                                            ] ?: "",

                                        resultDate =
                                            AttendanceStatusStore
                                                .todayDate(),

                                        status =
                                            newStatus
                                    )

                                    publishState[
                                        student.id
                                    ] =
                                        newStatus ==
                                                "PUBLISHED"

                                    message =
                                        if (
                                            newStatus ==
                                            "PUBLISHED"
                                        ) {
                                            "Result published for ${student.name}"
                                        } else {
                                            "Result unpublished for ${student.name}"
                                        }
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    imageVector =
                                        if (isPublished) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                    contentDescription =
                                        null
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        if (isPublished) {
                                            "Unpublish Result"
                                        } else {
                                            "Publish Result"
                                        }
                                )
                            }

                            /*
                             * DELETE
                             */

                            TextButton(

                                onClick = {

                                    val existing =
                                        TestStore.findResult(
                                            testId =
                                                test.id,
                                            studentId =
                                                student.id
                                        )

                                    if (existing == null) {

                                        message =
                                            "No saved result found."

                                        return@TextButton
                                    }

                                    val deleted =
                                        TestStore.deleteResult(
                                            testId =
                                                test.id,
                                            studentId =
                                                student.id
                                        )

                                    if (deleted) {

                                        obtainedMarks.remove(
                                            student.id
                                        )

                                        remarks.remove(
                                            student.id
                                        )

                                        publishState.remove(
                                            student.id
                                        )

                                        message =
                                            "Result deleted for ${student.name}"
                                    }
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Delete,
                                    contentDescription =
                                        null,
                                    tint =
                                        Color(0xFFC62828)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(6.dp)
                                )

                                Text(
                                    text =
                                        "Delete Result",
                                    color =
                                        Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }

        /*
         * -----------------------------------------------------
         * MESSAGE
         * -----------------------------------------------------
         */

        if (
            message.isNotBlank()
        ) {

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                color =
                    MaterialTheme.colorScheme.tertiaryContainer,
                shape =
                    RoundedCornerShape(10.dp)
            ) {

                Text(
                    text =
                        message,
                    modifier =
                        Modifier.padding(12.dp),
                    color =
                        Color(0xFF00695C),
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )
    }
}


/*
 * =============================================================
 * RESULT SUMMARY BOX
 * =============================================================
 */

@Composable
private fun ResultSummaryBox(
    modifier: Modifier,
    title: String,
    value: String
) {

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    value,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    title,
                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )
        }
    }
}


/*
 * =============================================================
 * NUMBER FORMAT
 * =============================================================
 */

private fun formatNumber(
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


/*
 * =============================================================
 * PERCENTAGE FORMAT
 * =============================================================
 */

private fun formatPercentage(
    value: Double
): String {

    return String.format(
        java.util.Locale.US,
        "%.1f",
        value
    )
}
