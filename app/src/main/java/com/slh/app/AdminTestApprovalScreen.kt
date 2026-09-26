package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp

/**
 * Admin reviews teacher-submitted tests before students can see them.
 *
 * Pending list is split into Live Tests and Quizzes.
 * Admin can open full paper (questions + correct answers) then Approve or Reject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTestApprovalScreen(
    user: DemoUser,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coachingId = user.coachingId ?: ""

    var refreshKey by remember { mutableStateOf(0) }
    val pending = remember(refreshKey, coachingId) {
        TestStore.getPendingApprovalTests(coachingId)
    }
    val livePending = pending.filter { it.testType == TestType.LIVE }
    val quizPending = pending.filter { it.testType == TestType.QUIZ }

    var viewingTest by remember { mutableStateOf<CoachingTest?>(null) }
    var rejectTarget by remember { mutableStateOf<CoachingTest?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    if (viewingTest != null) {
        AdminTestPaperReview(
            test = viewingTest!!,
            admin = user,
            onBack = { viewingTest = null },
            onApproved = {
                viewingTest = null
                refreshKey++
            },
            onRejectRequest = { test ->
                rejectTarget = test
                rejectReason = ""
            }
        )
        return
    }

    if (rejectTarget != null) {
        AlertDialog(
            onDismissRequest = {
                rejectTarget = null
                rejectReason = ""
            },
            title = { Text("Reject Test") },
            text = {
                Column {
                    Text(
                        text = rejectTarget!!.title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason") },
                        placeholder = { Text("e.g. Question 7 correct answer is wrong") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = TestStore.rejectTest(
                            testId = rejectTarget!!.id,
                            reason = rejectReason,
                            adminId = user.id,
                            adminName = user.displayName
                        )
                        if (ok) {
                            rejectTarget = null
                            rejectReason = ""
                            viewingTest = null
                            refreshKey++
                        }
                    },
                    enabled = rejectReason.trim().isNotBlank()
                ) {
                    Text("Reject Test", color = Color(0xFFC62828))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rejectTarget = null
                        rejectReason = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Approvals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    text = "Pending Tests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusChip(
                        label = "Live Tests",
                        count = livePending.size,
                        color = Color(0xFFC62828)
                    )
                    StatusChip(
                        label = "Quiz",
                        count = quizPending.size,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            if (pending.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
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
                                tint = Color(0xFF5E35B1),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                "No pending approvals",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Teacher-submitted papers will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (livePending.isNotEmpty()) {
                item {
                    Text(
                        "🔴 Live Tests",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }
                items(livePending, key = { it.id }) { test ->
                    PendingTestCard(
                        test = test,
                        onView = { viewingTest = test }
                    )
                }
            }

            if (quizPending.isNotEmpty()) {
                item {
                    Text(
                        "🟢 Quiz",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                items(quizPending, key = { it.id }) { test ->
                    PendingTestCard(
                        test = test,
                        onView = { viewingTest = test }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    count: Int,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Text(
            text = "$label · $count Pending",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PendingTestCard(
    test: CoachingTest,
    onView: () -> Unit
) {
    val batch = BatchStore.findBatch(test.batchId)
    val questionCount = QuestionStore.findQuestionsByTest(test.id).size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onView
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = test.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Teacher: ${test.createdByName.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Batch: ${batch?.name ?: test.batchId}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Type: ${if (test.testType == TestType.LIVE) "Live Test" else "Quiz"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    append("Questions: $questionCount")
                    append(" · Total Marks: ${test.totalMarks.toInt()}")
                    if (test.durationMinutes > 0) {
                        append(" · Duration: ${test.durationMinutes} min")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (test.submittedAt.isNotBlank()) {
                Text(
                    text = "Submitted: ${test.submittedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onView,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Full Paper")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTestPaperReview(
    test: CoachingTest,
    admin: DemoUser,
    onBack: () -> Unit,
    onApproved: () -> Unit,
    onRejectRequest: (CoachingTest) -> Unit
) {
    BackHandler { onBack() }

    val questions = remember(test.id) {
        QuestionStore.findQuestionsByTest(test.id)
            .sortedBy { it.questionOrder }
    }
    val batch = BatchStore.findBatch(test.batchId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Approval") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onRejectRequest(test) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFC62828)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REJECT")
                }
                Button(
                    onClick = {
                        val ok = TestStore.approveTest(
                            testId = test.id,
                            adminId = admin.id,
                            adminName = admin.displayName
                        )
                        if (ok) onApproved()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("APPROVE")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    test.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Teacher: ${test.createdByName.ifBlank { "—" }}")
                Text("Batch: ${batch?.name ?: test.batchId}")
                Text("Type: ${if (test.testType == TestType.LIVE) "Live Test" else "Quiz"}")
                if (test.subject.isNotBlank()) Text("Subject: ${test.subject}")
                Text(
                    buildString {
                        append("Questions: ${questions.size}")
                        append(" · Total Marks: ${test.totalMarks.toInt()}")
                        if (test.durationMinutes > 0) {
                            append(" · Duration: ${test.durationMinutes} min")
                        }
                    }
                )
                if (test.startTime.isNotBlank() || test.endTime.isNotBlank()) {
                    Text("Schedule: ${test.startTime} – ${test.endTime}")
                }
            }

            items(questions, key = { it.id }) { q ->
                QuestionReviewCard(question = q)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun QuestionReviewCard(
    question: TestQuestion
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Q${question.questionOrder}. ${question.questionText}",
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OptionLine("A", question.optionA, question.correctOption)
            OptionLine("B", question.optionB, question.correctOption)
            OptionLine("C", question.optionC, question.correctOption)
            OptionLine("D", question.optionD, question.correctOption)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Correct Answer: ${question.correctOption}  ·  Marks: ${question.marks}",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OptionLine(
    letter: String,
    text: String,
    correct: String
) {
    val isCorrect = letter.equals(correct, ignoreCase = true)
    Text(
        text = "$letter. $text",
        color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
