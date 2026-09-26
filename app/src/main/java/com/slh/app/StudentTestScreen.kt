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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StudentTestPage {
    SUBJECTS,
    TESTS,
    TAKE_TEST,
    SUBMITTED
}

@Composable
fun StudentTestScreen(
    student: Student,
    onBack: () -> Unit
) {
    var page by remember {
        mutableStateOf(StudentTestPage.SUBJECTS)
    }

    var selectedSubject by remember {
        mutableStateOf("")
    }

    var selectedTest by remember {
        mutableStateOf<CoachingTest?>(null)
    }

    var obtainedMarks by remember {
        mutableStateOf(0.0)
    }

    BackHandler {
        when (page) {
            StudentTestPage.SUBJECTS -> {
                onBack()
            }

            StudentTestPage.TESTS -> {
                selectedSubject = ""
                page = StudentTestPage.SUBJECTS
            }

            StudentTestPage.TAKE_TEST -> {
                // Take-test screen handles its own exit confirmation.
            }

            StudentTestPage.SUBMITTED -> {
                selectedTest = null
                page = StudentTestPage.TESTS
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {

        StudentTestHeader(
            title = when (page) {
                StudentTestPage.SUBJECTS -> "Tests & Quiz"
                StudentTestPage.TESTS -> selectedSubject
                StudentTestPage.TAKE_TEST ->
                    selectedTest?.title ?: "Test"
                StudentTestPage.SUBMITTED -> "Test Submitted"
            },
            onBack = {
                when (page) {
                    StudentTestPage.SUBJECTS -> onBack()

                    StudentTestPage.TESTS -> {
                        selectedSubject = ""
                        page = StudentTestPage.SUBJECTS
                    }

                    StudentTestPage.TAKE_TEST -> {
                        // Test screen owns the back action.
                    }

                    StudentTestPage.SUBMITTED -> {
                        selectedTest = null
                        page = StudentTestPage.TESTS
                    }
                }
            },
            enableBack = page != StudentTestPage.TAKE_TEST
        )

        when (page) {

            StudentTestPage.SUBJECTS -> {
                StudentSubjectSelection(
                    student = student,
                    onSubjectSelected = { subject ->
                        selectedSubject = subject
                        page = StudentTestPage.TESTS
                    }
                )
            }

            StudentTestPage.TESTS -> {
                StudentSubjectTests(
                    student = student,
                    subject = selectedSubject,
                    onTestSelected = { test ->
                        selectedTest = test
                        page = StudentTestPage.TAKE_TEST
                    }
                )
            }

            StudentTestPage.TAKE_TEST -> {
                selectedTest?.let { test ->

                    StudentTakeTest(
                        student = student,
                        test = test,
                        onSubmitted = { marks ->
                            obtainedMarks = marks
                            page = StudentTestPage.SUBMITTED
                        },
                        onExitTest = {
                            selectedTest = null
                            page = StudentTestPage.TESTS
                        }
                    )
                }
            }

            StudentTestPage.SUBMITTED -> {
                selectedTest?.let { test ->

                    StudentTestSubmitted(
                        test = test,
                        obtainedMarks = obtainedMarks,
                        onDone = {
                            selectedTest = null
                            page = StudentTestPage.TESTS
                        }
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* HEADER                                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun StudentTestHeader(
    title: String,
    onBack: () -> Unit,
    enableBack: Boolean = true
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
                onClick = onBack,
                enabled = enableBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

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
/* SUBJECT SELECTION                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun StudentSubjectSelection(
    student: Student,
    onSubjectSelected: (String) -> Unit
) {
    val subjects = remember(
        student.id,
        student.batchId
    ) {
        TestStore
            .getActiveTestsByBatch(
                batchId = student.batchId,
                coachingId = student.coachingId
            )
            .map { it.subject.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {

            Spacer(modifier = Modifier.height(10.dp))

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Subject,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {

                            Text(
                                text = "Select Subject",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Choose a subject to see available tests.",
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        if (subjects.isEmpty()) {

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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No tests available",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text =
                                "There are currently no active tests for your batch."
                        )
                    }
                }
            }

        } else {

            items(subjects) { subject ->

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
                                imageVector = Icons.Default.Subject,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint =
                                    MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = subject,
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onSubjectSelected(subject)
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
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/* -------------------------------------------------------------------------- */
/* TEST LIST                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun StudentSubjectTests(
    student: Student,
    subject: String,
    onTestSelected: (CoachingTest) -> Unit
) {
    val tests = remember(
        student.id,
        student.batchId,
        subject
    ) {
        TestStore
            .getActiveTestsByBatch(
                batchId = student.batchId,
                coachingId = student.coachingId
            )
            .filter {
                it.subject.equals(
                    subject,
                    ignoreCase = true
                )
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$subject Tests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${tests.size} active test(s)",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))
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
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No active tests",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "No test is currently available for this subject."
                        )
                    }
                }
            }

        } else {

            items(
                items = tests,
                key = { it.id }
            ) { test ->

                StudentTestListCard(
                    test = test,
                    onStart = {
                        onTestSelected(test)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StudentTestListCard(
    test: CoachingTest,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.Top
            ) {

                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = test.title.ifBlank {
                            "Untitled Test"
                        },
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

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color =
                        MaterialTheme.colorScheme.primaryContainer
                ) {

                    Text(
                        text = if (test.testType == TestType.LIVE) "LIVE" else "QUIZ",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        style =
                            MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (test.testDate.isNotBlank()) {

                TestInfoRow(
                    label = "Test Date",
                    value = test.testDate
                )
            }

            TestInfoRow(
                label = "Total Marks",
                value = formatStudentNumber(
                    test.totalMarks
                )
            )

            TestInfoRow(
                label = "Passing Marks",
                value = formatStudentNumber(
                    test.passingMarks
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (test.description.isNotBlank()) {

                Text(
                    text = "Instructions",
                    style =
                        MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = test.description,
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Test")
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* TAKE TEST                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun StudentTakeTest(
    student: Student,
    test: CoachingTest,
    onSubmitted: (Double) -> Unit,
    onExitTest: () -> Unit
) {
    /*
     * Do NOT remember the question list forever.
     * remember(test.id) froze an empty list when questions had not
     * synced yet → every answer scored 0.
     * Reading QuestionStore each composition stays reactive.
     */
    val questions = run {
        val byCoaching =
            QuestionStore.findQuestionsByTestAndCoaching(
                testId = test.id,
                coachingId = student.coachingId
            )
        if (byCoaching.isNotEmpty()) {
            byCoaching
        } else {
            // Fallback if coachingId mismatch on older records
            QuestionStore.findQuestionsByTest(test.id)
        }
    }

    val answers = remember(test.id) {
        mutableStateMapOf<String, String>()
    }

    var currentQuestion by remember(test.id) {
        mutableIntStateOf(0)
    }

    var showSubmitDialog by remember {
        mutableStateOf(false)
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }

    if (showExitDialog) {

        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },
            title = {
                Text("Exit Test?")
            },
            text = {
                Text(
                    "Your current answers will be lost if you leave this test."
                )
            },
            confirmButton = {

                Button(
                    onClick = {
                        showExitDialog = false
                        onExitTest()
                    }
                ) {
                    Text("Exit Test")
                }
            },
            dismissButton = {

                OutlinedButton(
                    onClick = {
                        showExitDialog = false
                    }
                ) {
                    Text("Continue Test")
                }
            }
        )
    }

    BackHandler {
        showExitDialog = true
    }

    if (showSubmitDialog) {

        val answeredCount = answers.size
        val unansweredCount =
            (questions.size - answeredCount)
                .coerceAtLeast(0)

        AlertDialog(
            onDismissRequest = {
                showSubmitDialog = false
            },
            title = {
                Text(
                    if (unansweredCount > 0) {
                        "Some Questions Are Unanswered"
                    } else {
                        "Submit Test?"
                    }
                )
            },
            text = {

                Column {

                    Text(
                        text =
                            "Answered: $answeredCount / ${questions.size}"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (unansweredCount > 0) {

                        Text(
                            text =
                                "Unanswered: $unansweredCount"
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "You can still submit the test, but unanswered questions will receive 0 marks."
                        )

                    } else {

                        Text(
                            text =
                                "All questions have been answered. Are you sure you want to submit?"
                        )
                    }
                }
            },
            confirmButton = {

                Button(
                    onClick = {

                        showSubmitDialog = false

                        val marks =
                            calculateStudentMarks(
                                questions = questions,
                                answers = answers,
                                totalMarks = test.totalMarks
                            )

                        TestStore.addOrUpdateResult(
                            testId = test.id,
                            studentId = student.id,
                            coachingId = student.coachingId,
                            obtainedMarks = marks,
                            remarks = "",
                            resultDate = currentStudentDate(),
                            status = "PUBLISHED"
                        )

                        onSubmitted(marks)
                    }
                ) {
                    Text("Submit Now")
                }
            },
            dismissButton = {

                OutlinedButton(
                    onClick = {
                        showSubmitDialog = false
                    }
                ) {
                    Text("Review")
                }
            }
        )
    }

    if (questions.isEmpty()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {

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
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Questions are not available",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text =
                            "The teacher has not added questions to this test yet."
                    )
                }
            }
        }

        return
    }

    val question = questions[currentQuestion]

    val answeredCount = answers.size

    val unansweredCount =
        (questions.size - answeredCount)
            .coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        /* -------------------------------------------------------------- */
        /* TEST SUMMARY                                                    */
        /* -------------------------------------------------------------- */

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(12.dp)
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
                            text = test.title,
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )

                        Text(
                            text =
                                "Question ${currentQuestion + 1} of ${questions.size}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(
                        horizontalAlignment =
                            Alignment.End
                    ) {

                        Text(
                            text = "Answered $answeredCount",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Remaining $unansweredCount",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        /* -------------------------------------------------------------- */
        /* QUESTION NAVIGATOR                                             */
        /* -------------------------------------------------------------- */

        QuestionNavigator(
            questions = questions,
            currentQuestion = currentQuestion,
            answers = answers,
            onQuestionSelected = { index ->
                currentQuestion = index
            }
        )

        /* -------------------------------------------------------------- */
        /* CURRENT QUESTION                                                */
        /* -------------------------------------------------------------- */

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.Top
                        ) {

                            Text(
                                text =
                                    "${currentQuestion + 1}.",
                                style =
                                    MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    question.questionText,
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight =
                                    FontWeight.SemiBold,
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Marks: ${
                                    formatStudentNumber(
                                        question.marks
                                    )
                                }",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {

                StudentAnswerOption(
                    optionKey = "A",
                    text = question.optionA,
                    selected =
                        answers[question.id] == "A",
                    onSelected = {
                        answers[question.id] = "A"
                    }
                )
            }

            item {

                StudentAnswerOption(
                    optionKey = "B",
                    text = question.optionB,
                    selected =
                        answers[question.id] == "B",
                    onSelected = {
                        answers[question.id] = "B"
                    }
                )
            }

            item {

                StudentAnswerOption(
                    optionKey = "C",
                    text = question.optionC,
                    selected =
                        answers[question.id] == "C",
                    onSelected = {
                        answers[question.id] = "C"
                    }
                )
            }

            item {

                StudentAnswerOption(
                    optionKey = "D",
                    text = question.optionD,
                    selected =
                        answers[question.id] == "D",
                    onSelected = {
                        answers[question.id] = "D"
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        /* -------------------------------------------------------------- */
        /* BOTTOM NAVIGATION                                               */
        /* -------------------------------------------------------------- */

        Surface(
            tonalElevation = 5.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = {

                        if (currentQuestion > 0) {
                            currentQuestion--
                        }
                    },
                    enabled = currentQuestion > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }

                if (currentQuestion < questions.lastIndex) {

                    Button(
                        onClick = {
                            currentQuestion++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }

                } else {

                    Button(
                        onClick = {
                            showSubmitDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit Test")
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* QUESTION NAVIGATOR                                                         */
/* -------------------------------------------------------------------------- */

@Composable
private fun QuestionNavigator(
    questions: List<TestQuestion>,
    currentQuestion: Int,
    answers: Map<String, String>,
    onQuestionSelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 12.dp)
    ) {

        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 4.dp,
                        bottom = 4.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {

                questions.forEachIndexed { index, question ->

                    QuestionNumberButton(
                        number = index + 1,
                        selected = index == currentQuestion,
                        answered =
                            answers.containsKey(question.id),
                        onClick = {
                            onQuestionSelected(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionNumberButton(
    number: Int,
    selected: Boolean,
    answered: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor: Color =
        when {
            selected ->
                MaterialTheme.colorScheme.primary

            answered ->
                MaterialTheme.colorScheme.secondaryContainer

            else ->
                MaterialTheme.colorScheme.surfaceVariant
        }

    val textColor: Color =
        when {
            selected ->
                MaterialTheme.colorScheme.onPrimary

            else ->
                MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = Modifier
            .size(42.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Button
            ),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        tonalElevation = if (selected) 4.dp else 1.dp
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = number.toString(),
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* ANSWER OPTION                                                              */
/* -------------------------------------------------------------------------- */

@Composable
private fun StudentAnswerOption(
    optionKey: String,
    text: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    if (text.isBlank()) {
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
        )
    ) {

        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    if (selected) {
                        Icons.Default.RadioButtonChecked
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "$optionKey. $text",
                style =
                    MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* SUBMITTED                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun StudentTestSubmitted(
    test: CoachingTest,
    obtainedMarks: Double,
    onDone: () -> Unit
) {
    val percentage =
        if (test.totalMarks > 0.0) {
            (obtainedMarks / test.totalMarks) * 100.0
        } else {
            0.0
        }

    val passed =
        obtainedMarks >= test.passingMarks

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    if (passed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
            )
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Test Submitted",
                    style =
                        MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = test.title,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text =
                        "${formatStudentNumber(obtainedMarks)} / ${
                            formatStudentNumber(
                                test.totalMarks
                            )
                        }",
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text =
                        "${String.format(
                            Locale.US,
                            "%.1f",
                            percentage
                        )}%"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Text(
                        text =
                            if (passed) "PASS" else "FAIL",
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Tests")
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* INFO ROW                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun TestInfoRow(
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
            modifier = Modifier.width(125.dp)
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f)
        )
    }
}

/* -------------------------------------------------------------------------- */
/* MARK CALCULATION                                                           */
/* -------------------------------------------------------------------------- */

private fun calculateStudentMarks(
    questions: List<TestQuestion>,
    answers: Map<String, String>,
    totalMarks: Double
): Double {
    var marks = 0.0

    questions.forEach { question ->

        val selectedRaw = answers[question.id] ?: return@forEach
        val selected = selectedRaw.trim().uppercase()
        val correct = question.correctOption.trim().uppercase()

        if (correct.isBlank()) return@forEach

        val letterMatch = selected == correct

        // Also accept if teacher saved full option text as correct answer
        val optionText = when (selected) {
            "A" -> question.optionA
            "B" -> question.optionB
            "C" -> question.optionC
            "D" -> question.optionD
            else -> ""
        }.trim()

        val textMatch =
            optionText.isNotBlank() &&
                    (optionText.equals(question.correctOption.trim(), ignoreCase = true) ||
                            selected.equals(optionText, ignoreCase = true))

        if (letterMatch || textMatch) {
            val qMarks = if (question.marks > 0.0) question.marks else 1.0
            marks += qMarks
        }
    }

    val maxAllowed =
        if (totalMarks > 0.0) {
            totalMarks
        } else {
            // If test totalMarks was left 0, use sum of question marks
            questions.sumOf {
                if (it.marks > 0.0) it.marks else 1.0
            }.coerceAtLeast(marks)
        }

    return marks
        .coerceAtLeast(0.0)
        .coerceAtMost(maxAllowed)
}

/* -------------------------------------------------------------------------- */
/* HELPERS                                                                    */
/* -------------------------------------------------------------------------- */

private fun formatStudentNumber(
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

private fun currentStudentDate(): String {
    return SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.US
    ).format(Date())
}