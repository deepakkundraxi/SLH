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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale

private enum class TeacherResultPage {
    BATCHES,
    TESTS,
    STUDENTS
}

@Composable
fun TeacherResultManagementScreen(
    user: DemoUser,
    onBack: () -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {
    var page by remember {
        mutableStateOf(TeacherResultPage.BATCHES)
    }

    var selectedBatch by remember {
        mutableStateOf<CoachingBatch?>(null)
    }

    var selectedTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    val coachingId = user.coachingId ?: ""

    // All ACTIVE batches of this coaching — shared across all teachers.
    val assignedBatches = remember(
        user.id,
        coachingId
    ) {
        BatchStore
            .findBatchesByCoaching(coachingId)
            .filter {
                it.status.equals("ACTIVE", ignoreCase = true)
            }
    }

    BackHandler {
        when (page) {

            TeacherResultPage.BATCHES -> {
                onBack()
            }

            TeacherResultPage.TESTS -> {
                selectedBatch = null
                page = TeacherResultPage.BATCHES
            }

            TeacherResultPage.STUDENTS -> {
                selectedTest = null
                page = TeacherResultPage.TESTS
            }
        }
    }

    // No nested Scaffold — same layout as fixed Teacher Dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            TeacherResultHeader(
                title = when (page) {
                    TeacherResultPage.BATCHES ->
                        "Marks & Results"

                    TeacherResultPage.TESTS ->
                        selectedBatch?.name ?: "Tests"

                    TeacherResultPage.STUDENTS ->
                        selectedTest?.title ?: "Student Results"
                },
                onBack = {
                    when (page) {

                        TeacherResultPage.BATCHES -> {
                            onBack()
                        }

                        TeacherResultPage.TESTS -> {
                            selectedBatch = null
                            page = TeacherResultPage.BATCHES
                        }

                        TeacherResultPage.STUDENTS -> {
                            selectedTest = null
                            page = TeacherResultPage.TESTS
                        }
                    }
                }
            )

            when (page) {

                TeacherResultPage.BATCHES -> {

                    TeacherBatchResultSelection(
                        batches = assignedBatches,
                        onBatchSelected = { batch ->
                            selectedBatch = batch
                            page = TeacherResultPage.TESTS
                        }
                    )
                }

                TeacherResultPage.TESTS -> {

                    selectedBatch?.let { batch ->

                        TeacherTestResultSelection(
                            batch = batch,
                            coachingId = coachingId,
                            onTestSelected = { test ->
                                selectedTest = test
                                page = TeacherResultPage.STUDENTS
                            }
                        )
                    }
                }

                TeacherResultPage.STUDENTS -> {

                    if (
                        selectedBatch != null &&
                        selectedTest != null
                    ) {

                        TeacherStudentResultList(
                            user = user,
                            batch = selectedBatch!!,
                            test = selectedTest!!
                        )
                    }
                }
            }
        }

        SLHBottomNavBar(
            items = BottomNavItems.teacher,
            currentRoute = "results",
            onNavigate = { route ->
                if (route == "results") {
                    // already here
                } else if (route == "dashboard") {
                    onBack()
                } else {
                    onNavigateTab(route)
                }
            }
        )
    }
}

/* -------------------------------------------------------------------------- */
/* HEADER                                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherResultHeader(
    title: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* BATCH SELECTION                                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherBatchResultSelection(
    batches: List<CoachingBatch>,
    onBatchSelected: (CoachingBatch) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Column {

                            Text(
                                text = "Select Batch",
                                style =
                                    MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Choose a batch to view student results."
                            )
                        }
                    }
                }
            }
        }

        if (batches.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "No batches found",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "No active batch in this coaching yet."
                        )
                    }
                }
            }

        } else {

            items(
                items = batches,
                key = { it.id }
            ) { batch ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint =
                                    MaterialTheme.colorScheme.primary
                            )

                            Spacer(
                                modifier = Modifier.width(10.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = batch.name,
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (batch.code.isNotBlank()) {

                                    Text(
                                        text =
                                            "Code: ${batch.code}",
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (batch.course.isNotBlank()) {

                                    Text(
                                        text =
                                            batch.course,
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {
                                onBatchSelected(batch)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Tests")
                        }
                    }
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* TEST SELECTION                                                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherTestResultSelection(
    batch: CoachingBatch,
    coachingId: String,
    onTestSelected: (CoachingTest) -> Unit
) {
    val tests = remember(
        batch.id,
        coachingId
    ) {
        TestStore.findTestsByBatchAndCoaching(
            batchId = batch.id,
            coachingId = coachingId
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = batch.name,
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "${tests.size} test(s)"
                    )
                }
            }
        }

        if (tests.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "No tests found",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "No test has been created for this batch."
                        )
                    }
                }
            }

        } else {

            items(
                items = tests,
                key = { it.id }
            ) { test ->

                TeacherTestResultCard(
                    test = test,
                    onSelected = {
                        onTestSelected(test)
                    }
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
}

@Composable
private fun TeacherTestResultCard(
    test: CoachingTest,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = test.title,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (test.subject.isNotBlank()) {

                        Text(
                            text = test.subject,
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            TeacherResultInfoRow(
                label = "Date",
                value =
                    test.testDate.ifBlank {
                        "Not specified"
                    }
            )

            TeacherResultInfoRow(
                label = "Total Marks",
                value =
                    formatTeacherResultNumber(
                        test.totalMarks
                    )
            )

            TeacherResultInfoRow(
                label = "Passing Marks",
                value =
                    formatTeacherResultNumber(
                        test.passingMarks
                    )
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onSelected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Student Results")
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* STUDENT RESULT LIST                                                        */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherStudentResultList(
    user: DemoUser,
    batch: CoachingBatch,
    test: CoachingTest
) {
    val students = remember(
        batch.id,
        batch.coachingId
    ) {
        StudentStore.searchStudentsByBatch(
            coachingId = batch.coachingId,
            batchId = batch.id,
            query = ""
        )
    }

    // Rank map from auto-generated / published results only
    // (same source as student role — no manual entry by teacher).
    val rankByStudentId = remember(test.id, students) {
        TestStore.getStudentPerformance(
            testId = test.id,
            includeUnpublished = false
        ).associate { it.studentId to it.position }
    }

    val enteredCount = remember(test.id, students) {
        students.count {
            val r = TestStore.findResult(test.id, it.id)
            r != null && r.status.equals("PUBLISHED", ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = test.title,
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (test.subject.isNotBlank()) {

                        Text(
                            text = test.subject,
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "View only — same results as student app (auto from tests)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        TeacherSummaryItem(
                            label = "Students",
                            value = students.size.toString(),
                            modifier =
                                Modifier.weight(1f)
                        )

                        TeacherSummaryItem(
                            label = "Published",
                            value = enteredCount.toString(),
                            modifier =
                                Modifier.weight(1f)
                        )

                        TeacherSummaryItem(
                            label = "Total",
                            value =
                                formatTeacherResultNumber(
                                    test.totalMarks
                                ),
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (students.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "No students found",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        } else {

            items(
                items = students,
                key = { it.id }
            ) { student ->

                TeacherStudentResultCard(
                    student = student,
                    test = test,
                    rank = rankByStudentId[student.id]
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(18.dp)
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* STUDENT CARD                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherStudentResultCard(
    student: Student,
    test: CoachingTest,
    rank: Int? = null
) {
    val result = TestStore.findResult(
        test.id,
        student.id
    )
    val published =
        result != null &&
                result.status.equals("PUBLISHED", ignoreCase = true)

    val percentage =
        if (
            published &&
            test.totalMarks > 0.0
        ) {
            result!!.obtainedMarks /
                    test.totalMarks *
                    100.0
        } else {
            0.0
        }

    val passed =
        published &&
                result!!.obtainedMarks >= test.passingMarks

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        if (published) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Groups
                        },
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint =
                        if (published) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = student.name,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "ID: ${student.studentId}",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (published) {
                            if (passed) {
                                MaterialTheme.colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme.colorScheme
                                    .errorContainer
                            }
                        } else {
                            MaterialTheme.colorScheme
                                .surfaceVariant
                        }
                ) {

                    Text(
                        text =
                            when {
                                !published ->
                                    "NO RESULT"

                                passed ->
                                    "PASS"

                                else ->
                                    "FAIL"
                            },
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        style =
                            MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            if (published) {

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TeacherSummaryItem(
                        label = "Marks",
                        value =
                            "${formatTeacherResultNumber(
                                result!!.obtainedMarks
                            )} / ${
                                formatTeacherResultNumber(
                                    test.totalMarks
                                )
                            }",
                        modifier =
                            Modifier.weight(1f)
                    )

                    TeacherSummaryItem(
                        label = "Percentage",
                        value =
                            "${String.format(
                                Locale.US,
                                "%.1f",
                                percentage
                            )}%",
                        modifier =
                            Modifier.weight(1f)
                    )

                    TeacherSummaryItem(
                        label = "Rank",
                        value =
                            if (rank != null && rank > 0) {
                                "#$rank"
                            } else {
                                "—"
                            },
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                if (result.remarks.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Remarks: ${result.remarks}",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

            } else {

                Text(
                    text = "Result not published yet (same as student view).",
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* SUMMARY ITEM                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherSummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(
            horizontal = 4.dp
        )
    ) {

        Text(
            text = label,
            style =
                MaterialTheme.typography.labelSmall
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/* -------------------------------------------------------------------------- */
/* INFO ROW                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun TeacherResultInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(115.dp)
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f)
        )
    }
}

/* -------------------------------------------------------------------------- */
/* HELPERS                                                                    */
/* -------------------------------------------------------------------------- */

private fun formatTeacherResultNumber(
    value: Double
): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    }
}