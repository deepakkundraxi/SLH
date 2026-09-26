package com.slh.app

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun ResultAnalyticsScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    androidx.activity.compose.BackHandler {
        onBack()
    }

    val context = LocalContext.current

    val allBatches =
        remember(coachingId) {
            BatchStore
                .findBatchesByCoaching(coachingId)
                .sortedBy {
                    it.name.lowercase(Locale.getDefault())
                }
        }

    var selectedBatchId by remember {
        mutableStateOf(
            allBatches.firstOrNull()?.id ?: ""
        )
    }

    var showBatchSelector by remember {
        mutableStateOf(false)
    }

    var showShareSelector by remember {
        mutableStateOf(false)
    }

    var selectedExportUri by remember {
        mutableStateOf<android.net.Uri?>(null)
    }

    var selectedExportMimeType by remember {
        mutableStateOf("")
    }

    var selectedExportTitle by remember {
        mutableStateOf("")
    }

    val selectedBatch =
        allBatches.firstOrNull {
            it.id == selectedBatchId
        }

    val students =
        if (selectedBatch != null) {
            StudentStore.searchStudentsByBatch(
                coachingId = coachingId,
                batchId = selectedBatch.id,
                query = ""
            )
        } else {
            emptyList()
        }

    val tests =
        if (selectedBatch != null) {
            TestStore
                .findTestsByBatchAndCoaching(
                    batchId = selectedBatch.id,
                    coachingId = coachingId
                )
                .sortedBy {
                    it.testDate
                }
        } else {
            emptyList()
        }

    val testSummaries =
        tests.mapNotNull {
            TestStore.getTestResultSummary(it.id)
        }

    val totalStudents =
        students.size

    val totalTests =
        tests.size

    val enteredResults =
        testSummaries.sumOf {
            it.enteredResults
        }

    val publishedResults =
        testSummaries.sumOf {
            it.publishedResults
        }

    val passedResults =
        testSummaries.sumOf {
            it.passedResults
        }

    val failedResults =
        testSummaries.sumOf {
            it.failedResults
        }

    val overallAverage =
        if (testSummaries.isNotEmpty()) {
            testSummaries
                .map {
                    it.averagePercentage
                }
                .average()
        } else {
            0.0
        }

    val studentOverallAnalytics =
        remember(
            selectedBatchId,
            students,
            tests
        ) {

            students
                .map { student ->

                    val performances =
                        tests.mapNotNull { test ->

                            TestStore
                                .getStudentPerformance(
                                    testId = test.id,
                                    includeUnpublished = false
                                )
                                .firstOrNull {
                                    it.studentId == student.id &&
                                            it.published
                                }
                        }

                    val attempted =
                        performances.size

                    val passed =
                        performances.count {
                            it.passed
                        }

                    val failed =
                        performances.count {
                            !it.passed
                        }

                    val average =
                        if (attempted > 0) {
                            performances
                                .map {
                                    it.percentage
                                }
                                .average()
                        } else {
                            0.0
                        }

                    val highest =
                        performances.maxOfOrNull {
                            it.percentage
                        } ?: 0.0

                    val obtained =
                        performances.sumOf {
                            it.obtainedMarks
                        }

                    val possible =
                        performances.sumOf {
                            it.totalMarks
                        }

                    StudentOverallAnalytics(
                        student = student,
                        testsAttempted = attempted,
                        passedTests = passed,
                        failedTests = failed,
                        averagePercentage = average,
                        highestPercentage = highest,
                        totalObtainedMarks = obtained,
                        totalPossibleMarks = possible,
                        rank = 0
                    )
                }
                .sortedWith(
                    compareByDescending<StudentOverallAnalytics> {
                        it.averagePercentage
                    }
                        .thenByDescending {
                            it.highestPercentage
                        }
                        .thenByDescending {
                            it.testsAttempted
                        }
                        .thenBy {
                            it.student.name.lowercase(
                                Locale.getDefault()
                            )
                        }
                )
                .mapIndexed { index, item ->

                    if (item.testsAttempted > 0) {
                        item.copy(
                            rank = index + 1
                        )
                    } else {
                        item.copy(
                            rank = 0
                        )
                    }
                }
        }

    val attemptedStudents =
        studentOverallAnalytics.count {
            it.testsAttempted > 0
        }

    val studentsWithAllTestsPassed =
        studentOverallAnalytics.count { item ->

            item.testsAttempted > 0 &&
                    item.failedTests == 0
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Result Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Performance & Result Reports",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                if (allBatches.isEmpty()) {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "No batches available",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = "Create a batch first to view result analytics.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                } else {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Text(
                                text = "Selected Batch",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = selectedBatch?.name
                                    ?: "Select Batch",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (
                                selectedBatch?.code?.isNotBlank() == true
                            ) {

                                Spacer(
                                    modifier = Modifier.height(3.dp)
                                )

                                Text(
                                    text = "Code: ${selectedBatch.code}",
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Button(
                                onClick = {
                                    showBatchSelector = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Change Batch")
                            }
                        }
                    }
                }
            }

            if (selectedBatch != null) {

                item {

                    ExportAnalyticsCard(
                        onPdf = {

                            val uri =
                                ResultAnalyticsExport.exportPdf(
                                    context = context,
                                    coachingId = coachingId,
                                    batch = selectedBatch,
                                    students = students,
                                    tests = tests
                                )

                            if (uri != null) {

                                selectedExportUri = uri
                                selectedExportMimeType =
                                    "application/pdf"
                                selectedExportTitle =
                                    "Share Result Analytics PDF"

                                Toast.makeText(
                                    context,
                                    "PDF exported successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "PDF export failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },

                        onExcel = {

                            val uri =
                                ResultAnalyticsExport.exportExcel(
                                    context = context,
                                    coachingId = coachingId,
                                    batch = selectedBatch,
                                    students = students,
                                    tests = tests
                                )

                            if (uri != null) {

                                selectedExportUri = uri
                                selectedExportMimeType =
                                    "application/vnd.ms-excel"
                                selectedExportTitle =
                                    "Share Result Analytics Excel"

                                Toast.makeText(
                                    context,
                                    "Excel exported successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Excel export failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },

                        onShare = {
                            showShareSelector = true
                        }
                    )
                }

                item {

                    BatchOverviewCard(
                        totalStudents = totalStudents,
                        totalTests = totalTests,
                        enteredResults = enteredResults,
                        publishedResults = publishedResults,
                        passedResults = passedResults,
                        failedResults = failedResults,
                        averagePercentage = overallAverage
                    )
                }

                item {

                    StudentOverallSummaryCard(
                        totalStudents = totalStudents,
                        attemptedStudents = attemptedStudents,
                        passedStudents = studentsWithAllTestsPassed,
                        totalTests = totalTests,
                        averagePercentage =
                            if (
                                studentOverallAnalytics
                                    .any {
                                        it.testsAttempted > 0
                                    }
                            ) {

                                studentOverallAnalytics
                                    .filter {
                                        it.testsAttempted > 0
                                    }
                                    .map {
                                        it.averagePercentage
                                    }
                                    .average()

                            } else {
                                0.0
                            }
                    )
                }

                item {

                    SectionTitle(
                        title = "Student Overall Performance"
                    )
                }

                if (
                    studentOverallAnalytics.isEmpty() ||
                    attemptedStudents == 0
                ) {

                    item {

                        EmptyAnalyticsCard(
                            message =
                                "No published student results available."
                        )
                    }

                } else {

                    items(
                        items =
                            studentOverallAnalytics.filter {
                                it.testsAttempted > 0
                            },
                        key = {
                            it.student.id
                        }
                    ) { item ->

                        StudentOverallPerformanceCard(
                            analytics = item,
                            totalTests = totalTests
                        )
                    }
                }

                item {

                    SectionTitle(
                        title = "Test-wise Analytics"
                    )
                }

                if (tests.isEmpty()) {

                    item {

                        EmptyAnalyticsCard(
                            message =
                                "No tests available for this batch."
                        )
                    }

                } else {

                    items(
                        items = tests,
                        key = {
                            it.id
                        }
                    ) { test ->

                        TestAnalyticsCard(
                            test = test
                        )
                    }
                }

                item {

                    SectionTitle(
                        title = "Top 3 Performers"
                    )
                }

                val topPerformers =
                    tests
                        .flatMap { test ->

                            TestStore
                                .getTopStudentPerformances(
                                    testId = test.id,
                                    limit = 3,
                                    publishedOnly = true
                                )
                                .map { performance ->

                                    val student =
                                        students.firstOrNull {
                                            it.id ==
                                                    performance.studentId
                                        }

                                    TopPerformerRow(
                                        student = student,
                                        test = test,
                                        performance =
                                            performance
                                    )
                                }
                        }
                        .sortedByDescending {
                            it.performance.percentage
                        }
                        .take(3)

                if (topPerformers.isEmpty()) {

                    item {

                        EmptyAnalyticsCard(
                            message =
                                "No published top performers available."
                        )
                    }

                } else {

                    items(
                        items = topPerformers
                    ) { performer ->

                        TopPerformerCard(
                            performer = performer
                        )
                    }
                }

                item {

                    SectionTitle(
                        title = "Test Performance Summary"
                    )
                }

                if (testSummaries.isEmpty()) {

                    item {

                        EmptyAnalyticsCard(
                            message =
                                "No result summary available."
                        )
                    }

                } else {

                    items(
                        items = testSummaries,
                        key = {
                            it.testId
                        }
                    ) { summary ->

                        ResultSummaryCard(
                            summary = summary,
                            test =
                                tests.firstOrNull {
                                    it.id ==
                                            summary.testId
                                }
                        )
                    }
                }

                item {

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
    }

    if (showBatchSelector) {

        AlertDialog(
            onDismissRequest = {
                showBatchSelector = false
            },

            title = {
                Text(
                    text = "Select Batch",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    allBatches.forEach { batch ->

                        OutlinedButton(
                            onClick = {

                                selectedBatchId =
                                    batch.id

                                showBatchSelector =
                                    false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 4.dp
                                )
                        ) {

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text = batch.name,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                if (
                                    batch.code.isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            batch.code,
                                        fontSize = 12.sp,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchSelector = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showShareSelector && selectedBatch != null) {

        AlertDialog(
            onDismissRequest = {
                showShareSelector = false
            },

            title = {
                Text(
                    text = "Share Result Analytics",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "Choose the format you want to share.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        onClick = {

                            val uri =
                                ResultAnalyticsExport.exportPdf(
                                    context = context,
                                    coachingId = coachingId,
                                    batch = selectedBatch,
                                    students = students,
                                    tests = tests
                                )

                            showShareSelector = false

                            if (uri != null) {

                                ResultAnalyticsExport.shareFile(
                                    context = context,
                                    uri = uri,
                                    mimeType =
                                        "application/pdf",
                                    title =
                                        "Share Result Analytics PDF"
                                )

                            } else {

                                Toast.makeText(
                                    context,
                                    "PDF export failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text("📄 Share PDF")
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Button(
                        onClick = {

                            val uri =
                                ResultAnalyticsExport.exportExcel(
                                    context = context,
                                    coachingId = coachingId,
                                    batch = selectedBatch,
                                    students = students,
                                    tests = tests
                                )

                            showShareSelector = false

                            if (uri != null) {

                                ResultAnalyticsExport.shareFile(
                                    context = context,
                                    uri = uri,
                                    mimeType =
                                        "application/vnd.ms-excel",
                                    title =
                                        "Share Result Analytics Excel"
                                )

                            } else {

                                Toast.makeText(
                                    context,
                                    "Excel export failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text("📊 Share Excel")
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showShareSelector = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (
        selectedExportUri != null &&
        selectedExportMimeType.isNotBlank()
    ) {

        AlertDialog(
            onDismissRequest = {
                selectedExportUri = null
                selectedExportMimeType = ""
                selectedExportTitle = ""
            },

            title = {
                Text(
                    text = "Export Complete",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text =
                        "The file has been generated successfully. " +
                                "Would you like to share it now?"
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        val uri =
                            selectedExportUri

                        val mimeType =
                            selectedExportMimeType

                        val title =
                            selectedExportTitle

                        selectedExportUri = null
                        selectedExportMimeType = ""
                        selectedExportTitle = ""

                        if (uri != null) {

                            ResultAnalyticsExport.shareFile(
                                context = context,
                                uri = uri,
                                mimeType = mimeType,
                                title = title
                            )
                        }
                    }
                ) {

                    Text("Share")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        selectedExportUri = null
                        selectedExportMimeType = ""
                        selectedExportTitle = ""
                    }
                ) {

                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ExportAnalyticsCard(
    onPdf: () -> Unit,
    onExcel: () -> Unit,
    onShare: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Export Result Analytics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    "Save or share the complete analytics report.",
                fontSize = 13.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onPdf,
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "📄 PDF",
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onExcel,
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "📊 Excel",
                        maxLines = 1
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("📤 Share")
            }
        }
    }
}

@Composable
private fun BatchOverviewCard(
    totalStudents: Int,
    totalTests: Int,
    enteredResults: Int,
    publishedResults: Int,
    passedResults: Int,
    failedResults: Int,
    averagePercentage: Double
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Batch Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AnalyticsStatRow(
                leftTitle = "Students",
                leftValue = totalStudents.toString(),
                rightTitle = "Tests",
                rightValue = totalTests.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Entered",
                leftValue = enteredResults.toString(),
                rightTitle = "Published",
                rightValue = publishedResults.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Passed",
                leftValue = passedResults.toString(),
                rightTitle = "Failed",
                rightValue = failedResults.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Average",
                leftValue =
                    "${formatAnalyticsNumber(averagePercentage)}%",
                rightTitle = "",
                rightValue = ""
            )
        }
    }
}

@Composable
private fun StudentOverallSummaryCard(
    totalStudents: Int,
    attemptedStudents: Int,
    passedStudents: Int,
    totalTests: Int,
    averagePercentage: Double
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Student Overall Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AnalyticsStatRow(
                leftTitle = "Total Students",
                leftValue =
                    totalStudents.toString(),
                rightTitle = "Attempted",
                rightValue =
                    attemptedStudents.toString()
            )

            AnalyticsStatRow(
                leftTitle = "All Tests Passed",
                leftValue =
                    passedStudents.toString(),
                rightTitle = "Total Tests",
                rightValue =
                    totalTests.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Average",
                leftValue =
                    "${formatAnalyticsNumber(averagePercentage)}%",
                rightTitle = "",
                rightValue = ""
            )
        }
    }
}

@Composable
private fun StudentOverallPerformanceCard(
    analytics: StudentOverallAnalytics,
    totalTests: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth()
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

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (analytics.rank > 0) {
                                "#${analytics.rank}  ${analytics.student.name}"
                            } else {
                                analytics.student.name
                            },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (
                        analytics.student.studentId.isNotBlank()
                    ) {

                        Text(
                            text =
                                "ID: ${analytics.student.studentId}",
                            fontSize = 12.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                if (analytics.averagePercentage > 0.0) {

                    Text(
                        text =
                            "${formatAnalyticsNumber(analytics.averagePercentage)}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AnalyticsStatRow(
                leftTitle = "Tests",
                leftValue =
                    "${analytics.testsAttempted}/$totalTests",
                rightTitle = "Highest",
                rightValue =
                    "${formatAnalyticsNumber(analytics.highestPercentage)}%"
            )

            AnalyticsStatRow(
                leftTitle = "PASS",
                leftValue =
                    analytics.passedTests.toString(),
                rightTitle = "FAIL",
                rightValue =
                    analytics.failedTests.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Marks",
                leftValue =
                    "${formatAnalyticsNumber(analytics.totalObtainedMarks)} / " +
                            formatAnalyticsNumber(
                                analytics.totalPossibleMarks
                            ),
                rightTitle = "Average",
                rightValue =
                    "${formatAnalyticsNumber(analytics.averagePercentage)}%"
            )
        }
    }
}

@Composable
private fun TestAnalyticsCard(
    test: CoachingTest
) {

    val summary =
        TestStore.getTestResultSummary(test.id)

    val topPerformers =
        TestStore.getTopStudentPerformances(
            testId = test.id,
            limit = 3,
            publishedOnly = true
        )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = test.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            if (test.subject.isNotBlank()) {

                Text(
                    text = "Subject: ${test.subject}",
                    fontSize = 13.sp
                )
            }

            if (test.testDate.isNotBlank()) {

                Text(
                    text = "Date: ${test.testDate}",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            AnalyticsStatRow(
                leftTitle = "Total Marks",
                leftValue =
                    formatAnalyticsNumber(
                        test.totalMarks
                    ),
                rightTitle = "Passing",
                rightValue =
                    formatAnalyticsNumber(
                        test.passingMarks
                    )
            )

            if (summary != null) {

                AnalyticsStatRow(
                    leftTitle = "Students",
                    leftValue =
                        summary.totalStudents.toString(),
                    rightTitle = "Entered",
                    rightValue =
                        summary.enteredResults.toString()
                )

                AnalyticsStatRow(
                    leftTitle = "Published",
                    leftValue =
                        summary.publishedResults.toString(),
                    rightTitle = "Passed",
                    rightValue =
                        summary.passedResults.toString()
                )

                AnalyticsStatRow(
                    leftTitle = "Failed",
                    leftValue =
                        summary.failedResults.toString(),
                    rightTitle = "Average",
                    rightValue =
                        "${formatAnalyticsNumber(summary.averagePercentage)}%"
                )

                AnalyticsStatRow(
                    leftTitle = "Highest",
                    leftValue =
                        formatAnalyticsNumber(
                            summary.highestMarks
                        ),
                    rightTitle = "Lowest",
                    rightValue =
                        formatAnalyticsNumber(
                            summary.lowestMarks
                        )
                )
            } else {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "No results entered.",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            if (topPerformers.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Top Performers",
                    fontWeight = FontWeight.SemiBold
                )

                topPerformers.forEachIndexed { index, performance ->

                    val position =
                        index + 1

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "$position. " +
                                    "${formatAnalyticsNumber(performance.percentage)}% " +
                                    "• ${formatAnalyticsNumber(performance.obtainedMarks)}/" +
                                    formatAnalyticsNumber(performance.totalMarks),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(
    summary: TestResultSummary,
    test: CoachingTest?
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text =
                    test?.title
                        ?: "Test Result Summary",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            AnalyticsStatRow(
                leftTitle = "Students",
                leftValue =
                    summary.totalStudents.toString(),
                rightTitle = "Entered",
                rightValue =
                    summary.enteredResults.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Published",
                leftValue =
                    summary.publishedResults.toString(),
                rightTitle = "Passed",
                rightValue =
                    summary.passedResults.toString()
            )

            AnalyticsStatRow(
                leftTitle = "Failed",
                leftValue =
                    summary.failedResults.toString(),
                rightTitle = "Average",
                rightValue =
                    "${formatAnalyticsNumber(summary.averagePercentage)}%"
            )

            AnalyticsStatRow(
                leftTitle = "Highest",
                leftValue =
                    formatAnalyticsNumber(
                        summary.highestMarks
                    ),
                rightTitle = "Lowest",
                rightValue =
                    formatAnalyticsNumber(
                        summary.lowestMarks
                    )
            )
        }
    }
}

private data class TopPerformerRow(
    val student: Student?,
    val test: CoachingTest,
    val performance: StudentTestPerformance
)

@Composable
private fun TopPerformerCard(
    performer: TopPerformerRow
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    when (performer.performance.position) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "#${performer.performance.position}"
                    },
                fontSize = 26.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        performer.student?.name
                            ?: "Student",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text =
                        performer.test.title,
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {

                Text(
                    text =
                        "${formatAnalyticsNumber(performer.performance.percentage)}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text =
                        "${formatAnalyticsNumber(performer.performance.obtainedMarks)}/" +
                                formatAnalyticsNumber(
                                    performer.performance.totalMarks
                                ),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AnalyticsStatRow(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            if (leftTitle.isNotBlank()) {

                Text(
                    text = leftTitle,
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text = leftValue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {

            if (rightTitle.isNotBlank()) {

                Text(
                    text = rightTitle,
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text = rightValue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String
) {

    Text(
        text = title,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            top = 4.dp,
            bottom = 2.dp
        )
    )
}

@Composable
private fun EmptyAnalyticsCard(
    message: String
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = message,
                textAlign = TextAlign.Center,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

private data class StudentOverallAnalytics(
    val student: Student,
    val testsAttempted: Int,
    val passedTests: Int,
    val failedTests: Int,
    val averagePercentage: Double,
    val highestPercentage: Double,
    val totalObtainedMarks: Double,
    val totalPossibleMarks: Double,
    val rank: Int
)

private fun formatAnalyticsNumber(
    value: Double
): String {

    return if (value % 1.0 == 0.0) {

        value
            .toInt()
            .toString()

    } else {

        String.format(
            Locale.US,
            "%.2f",
            value
        )
    }
}