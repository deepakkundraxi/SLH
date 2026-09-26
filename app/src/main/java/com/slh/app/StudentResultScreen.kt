package com.slh.app

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentResultScreen(
    student: Student,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }


    /*
     * ---------------------------------------------------------
     * STUDENT BATCH TESTS
     * ---------------------------------------------------------
     */
    val tests =
        remember(
            student.id,
            student.batchId
        ) {

            if (student.batchId.isBlank()) {
                emptyList()
            } else {

                TestStore.findTestsByBatch(
                    batchId = student.batchId
                )
            }
        }


    /*
     * ---------------------------------------------------------
     * PUBLISHED RESULTS
     * ---------------------------------------------------------
     */
    val publishedResults =
        remember(
            student.id,
            tests
        ) {

            tests.mapNotNull { test ->

                val result =
                    TestStore.findResult(
                        testId = test.id,
                        studentId = student.id
                    )

                if (
                    result != null &&
                    result.status == "PUBLISHED"
                ) {

                    test to result

                } else {
                    null
                }
            }
        }


    /*
     * ---------------------------------------------------------
     * PASSED / FAILED
     * ---------------------------------------------------------
     */
    val passedCount =
        publishedResults.count { (test, result) ->

            result.obtainedMarks >=
                    test.passingMarks
        }


    val failedCount =
        publishedResults.count { (test, result) ->

            result.obtainedMarks <
                    test.passingMarks
        }


    /*
     * ---------------------------------------------------------
     * AVERAGE PERCENTAGE
     * ---------------------------------------------------------
     *
     * Sirf published results ka average niklega.
     */
    val averagePercentage =
        if (publishedResults.isNotEmpty()) {

            publishedResults
                .mapNotNull { (test, result) ->

                    if (test.totalMarks > 0) {

                        (
                                result.obtainedMarks /
                                        test.totalMarks
                                ) * 100.0

                    } else {
                        null
                    }
                }
                .average()

        } else {
            0.0
        }


    /*
     * ---------------------------------------------------------
     * SCREEN
     * ---------------------------------------------------------
     */
    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "My Results",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
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

    ) { innerPadding ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = 16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp),

            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(
                        top = 12.dp,
                        bottom = 28.dp
                    )
        ) {

            /*
             * -------------------------------------------------
             * STUDENT HEADER
             * -------------------------------------------------
             */
            item {

                StudentResultHeader(
                    student = student
                )
            }


            /*
             * -------------------------------------------------
             * SUMMARY
             * -------------------------------------------------
             */
            item {

                StudentResultSummaryCard(
                    totalResults =
                        publishedResults.size,

                    passed =
                        passedCount,

                    failed =
                        failedCount,

                    average =
                        averagePercentage
                )
            }


            /*
             * -------------------------------------------------
             * EMPTY / TEST LIST
             * -------------------------------------------------
             */
            if (tests.isEmpty()) {

                item {

                    StudentResultEmptyCard(

                        message =
                            if (
                                student.batchId.isBlank()
                            ) {

                                "You are not assigned to any batch yet."

                            } else {

                                "No tests are available for your batch."
                            }
                    )
                }

            } else {

                item {

                    Text(
                        text = "Test Results",

                        fontSize = 20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        modifier =
                            Modifier.padding(
                                top = 4.dp,
                                bottom = 2.dp
                            )
                    )
                }


                items(
                    items = tests,

                    key = { test ->
                        test.id
                    }
                ) { test ->

                    val result =
                        TestStore.findResult(
                            testId = test.id,
                            studentId = student.id
                        )

                    val rank =
                        if (result != null &&
                            result.status == "PUBLISHED"
                        ) {
                            TestStore.getStudentPerformance(
                                testId = test.id,
                                includeUnpublished = false
                            ).firstOrNull {
                                it.studentId == student.id
                            }?.position
                        } else {
                            null
                        }

                    StudentResultItem(
                        test = test,
                        result = result,
                        rank = rank
                    )
                }
            }
        }
    }
}


/*
 * =============================================================
 * STUDENT HEADER
 * =============================================================
 */
@Composable
private fun StudentResultHeader(
    student: Student
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(54.dp)
                        .background(
                            Color(0xFF1976D2),
                            RoundedCornerShape(16.dp)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Assessment,

                    contentDescription =
                        "Results",

                    tint =
                        Color.White,

                    modifier =
                        Modifier.size(30.dp)
                )
            }


            Spacer(
                modifier =
                    Modifier.size(14.dp)
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        student.name,

                    fontSize = 19.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Student ID: ${student.studentId}",

                    fontSize = 13.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )


                if (
                    student.batchId.isNotBlank()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "Batch: ${student.batchId}",

                        fontSize = 13.sp,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


/*
 * =============================================================
 * PERFORMANCE SUMMARY
 * =============================================================
 */
@Composable
private fun StudentResultSummaryCard(
    totalResults: Int,
    passed: Int,
    failed: Int,
    average: Double
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

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
                Modifier.padding(16.dp)
        ) {

            Text(
                text =
                    "Performance Summary",

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
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

                    title =
                        "Tests",

                    value =
                        totalResults.toString(),

                    background =
                        MaterialTheme.colorScheme.primaryContainer,

                    textColor =
                        Color(0xFF1976D2)
                )


                ResultSummaryBox(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Passed",

                    value =
                        passed.toString(),

                    background =
                        MaterialTheme.colorScheme.tertiaryContainer,

                    textColor =
                        Color(0xFF2E7D32)
                )


                ResultSummaryBox(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Failed",

                    value =
                        failed.toString(),

                    background =
                        MaterialTheme.colorScheme.errorContainer,

                    textColor =
                        Color(0xFFC62828)
                )
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(12.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Average Percentage",

                        fontWeight =
                            FontWeight.Medium
                    )


                    Text(
                        text =
                            "${formatStudentPercentage(
                                average
                            )}%",

                        fontSize = 20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color(0xFF7B1FA2)
                    )
                }
            }
        }
    }
}


/*
 * =============================================================
 * SUMMARY BOX
 * =============================================================
 */
@Composable
private fun ResultSummaryBox(
    modifier: Modifier,
    title: String,
    value: String,
    background: Color,
    textColor: Color
) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(12.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 12.dp,
                        horizontal = 6.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    value,

                fontSize = 21.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    textColor
            )


            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )


            Text(
                text =
                    title,

                fontSize = 12.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/*
 * =============================================================
 * INDIVIDUAL RESULT
 * =============================================================
 */
@Composable
private fun StudentResultItem(
    test: CoachingTest,
    result: TestResult?,
    rank: Int? = null
) {

    val hasPublishedResult =
        result != null &&
                result.status == "PUBLISHED"


    val passed =
        hasPublishedResult &&
                result!!.obtainedMarks >=
                test.passingMarks


    val percentage =
        if (
            hasPublishedResult &&
            test.totalMarks > 0
        ) {

            (
                    result!!.obtainedMarks /
                            test.totalMarks
                    ) * 100.0

        } else {
            0.0
        }


    val statusText =
        when {

            !hasPublishedResult ->
                "Result Not Published"

            passed ->
                "PASS"

            else ->
                "FAIL"
        }


    val statusColor =
        when {

            !hasPublishedResult ->
                Color(0xFF1976D2)

            passed ->
                Color(0xFF2E7D32)

            else ->
                Color(0xFFC62828)
        }


    val statusBackground =
        when {

            !hasPublishedResult ->
                MaterialTheme.colorScheme.primaryContainer

            passed ->
                MaterialTheme.colorScheme.tertiaryContainer

            else ->
                MaterialTheme.colorScheme.errorContainer
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

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
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(14.dp)
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Assessment,

                        contentDescription =
                            "Test",

                        tint =
                            Color(0xFF5E35B1),

                        modifier =
                            Modifier.size(25.dp)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.size(12.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            test.title,

                        fontSize = 17.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    if (
                        test.subject.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )

                        Text(
                            text =
                                test.subject,

                            fontSize = 13.sp,

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }


                Box(
                    modifier =
                        Modifier
                            .background(
                                statusBackground,
                                RoundedCornerShape(50.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            )
                ) {

                    Text(
                        text =
                            statusText,

                        color =
                            statusColor,

                        fontSize = 12.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
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

                ResultInfoBox(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.CalendarMonth,

                    label =
                        "Test Date",

                    value =
                        if (
                            test.testDate.isBlank()
                        ) {

                            "—"

                        } else {

                            test.testDate
                        }
                )


                ResultInfoBox(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.School,

                    label =
                        "Total Marks",

                    value =
                        formatStudentNumber(
                            test.totalMarks
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            if (hasPublishedResult) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    ResultInfoBox(
                        modifier =
                            Modifier.weight(1f),

                        icon =
                            Icons.Default.Assessment,

                        label =
                            "Obtained",

                        value =
                            formatStudentNumber(
                                result!!.obtainedMarks
                            )
                    )


                    ResultInfoBox(
                        modifier =
                            Modifier.weight(1f),

                        icon =
                            Icons.Default.Assessment,

                        label =
                            "Percentage",

                        value =
                            "${formatStudentPercentage(
                                percentage
                            )}%"
                    )

                    ResultInfoBox(
                        modifier =
                            Modifier.weight(1f),

                        icon =
                            Icons.Default.Assessment,

                        label =
                            "Rank",

                        value =
                            if (rank != null && rank > 0) {
                                "#$rank"
                            } else {
                                "—"
                            }
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(12.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                statusBackground
                        )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(13.dp),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                if (passed) {

                                    "Congratulations! You passed this test."

                                } else {

                                    "You did not reach the passing marks."
                                },

                            fontSize = 13.sp,

                            color =
                                statusColor,

                            modifier =
                                Modifier.weight(1f)
                        )


                        Text(
                            text =
                                "Pass: ${
                                    formatStudentNumber(
                                        test.passingMarks
                                    )
                                }",

                            fontSize = 12.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                statusColor
                        )
                    }
                }

            } else {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(12.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer
                        )
                ) {

                    Text(
                        text =
                            "Your result has not been published yet.",

                        modifier =
                            Modifier.padding(13.dp),

                        fontSize = 13.sp,

                        color =
                            Color(0xFF1565C0),

                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }
        }
    }
}


/*
 * =============================================================
 * RESULT INFO BOX
 * =============================================================
 */
@Composable
private fun ResultInfoBox(
    modifier: Modifier,
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(12.dp),

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
                    .padding(10.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        label,

                    tint =
                        Color(0xFF546E7A),

                    modifier =
                        Modifier.size(17.dp)
                )


                Spacer(
                    modifier =
                        Modifier.size(5.dp)
                )


                Text(
                    text =
                        label,

                    fontSize = 11.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Text(
                text =
                    value,

                fontSize = 14.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


/*
 * =============================================================
 * EMPTY RESULT CARD
 * =============================================================
 */
@Composable
private fun StudentResultEmptyCard(
    message: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
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
                    "No results",

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                modifier =
                    Modifier.size(42.dp)
            )


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Text(
                text =
                    message,

                fontSize = 15.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/*
 * =============================================================
 * NUMBER FORMAT
 * =============================================================
 */
private fun formatStudentNumber(
    value: Double
): String {

    return if (
        value % 1.0 == 0.0
    ) {

        value.roundToInt().toString()

    } else {

        String.format(
            java.util.Locale.US,
            "%.2f",
            value
        )
            .trimEnd('0')
            .trimEnd('.')
    }
}


/*
 * =============================================================
 * PERCENTAGE FORMAT
 * =============================================================
 */
private fun formatStudentPercentage(
    value: Double
): String {

    return String.format(
        java.util.Locale.US,
        "%.1f",
        value
    )
        .trimEnd('0')
        .trimEnd('.')
}