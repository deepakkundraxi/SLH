package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.slh.app.ui.theme.ChipColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TeacherDashboardScreen(
    user: DemoUser,
    coaching: CoachingProfile?
) {

    var showNoticeScreen by remember {
        mutableStateOf(false)
    }

    var showQuestionScreen by remember {
        mutableStateOf(false)
    }

    /** Create / manage tests (paper) — primary entry for teachers. */
    var showTestScreen by remember {
        mutableStateOf(false)
    }

    var showResultScreen by remember {
        mutableStateOf(false)
    }

    // ---- NEW: shared tab-switch helper for the bottom nav ----
    val navigateToTeacherTab: (String) -> Unit = { route ->

        showQuestionScreen = false
        showTestScreen = false
        showNoticeScreen = false
        showResultScreen = false

        when (route) {
            "questions" -> showQuestionScreen = true
            "tests" -> showTestScreen = true
            "notices" -> showNoticeScreen = true
            "results" -> showResultScreen = true
            // "dashboard" -> all already false, falls through
        }
    }

    /*
     * ---------------------------------------------------------
     * RESULT MANAGEMENT
     * ---------------------------------------------------------
     */

    if (showResultScreen) {

        BackHandler {
            showResultScreen = false
        }

        TeacherResultManagementScreen(
            user = user,
            onBack = {
                showResultScreen = false
            },
            onNavigateTab = navigateToTeacherTab
        )

        return
    }

    /*
     * ---------------------------------------------------------
     * TEST MANAGEMENT (create paper, submit for approval)
     * ---------------------------------------------------------
     */

    if (showTestScreen) {

        BackHandler {
            showTestScreen = false
        }

        TeacherTestManagementScreen(
            user = user,
            onBack = {
                showTestScreen = false
            }
        )

        return
    }

    /*
     * ---------------------------------------------------------
     * QUESTION MANAGEMENT
     * ---------------------------------------------------------
     */

    if (showQuestionScreen) {

        BackHandler {
            showQuestionScreen = false
        }

        QuestionManagementScreen(
            user = user,
            onBack = {
                showQuestionScreen = false
            },
            onNavigateTab = navigateToTeacherTab
        )

        return
    }

    /*
     * ---------------------------------------------------------
     * NOTICE SCREEN
     * ---------------------------------------------------------
     */

    if (showNoticeScreen) {

        BackHandler {
            showNoticeScreen = false
        }

        TeacherNoticeScreen(
            user = user,
            onBack = {
                showNoticeScreen = false
            },
            onNavigateTab = navigateToTeacherTab
        )

        return
    }

    /*
     * ---------------------------------------------------------
     * DASHBOARD DATA
     * ---------------------------------------------------------
     */

    val coachingId =
        user.coachingId ?: ""

    // All batches of this coaching are available to every teacher
    // (not limited to a single assigned teacherId).
    val assignedBatches =
        BatchStore
            .findBatchesByCoaching(coachingId)

    val assignedBatchIds =
        assignedBatches
            .map { it.id }
            .toSet()

    val studentCount =
        StudentStore.students.count {
            it.coachingId == coachingId &&
                    it.batchId in assignedBatchIds
        }

    val activeBatchCount =
        assignedBatches.count {
            it.status.equals(
                "ACTIVE",
                ignoreCase = true
            )
        }

    val unreadNoticeCount =
        NoticeStore.getUnreadNoticeCount(
            coachingId = coachingId,
            userId = user.id
        )

    val activeTestCount =
        assignedBatches.sumOf { batch ->

            TestStore.getActiveTestsByBatch(
                batchId = batch.id,
                coachingId = coachingId
            ).size
        }

    val coachingName =
        coaching?.name
            ?: "Sohan's Learning Hub"

    val (themeMode, setThemeMode) =
        rememberThemeMode()

    /*
     * ---------------------------------------------------------
     * MAIN DASHBOARD
     * ---------------------------------------------------------
     */

    // Content-only: parent AppDashboard provides TopAppBar.
    // No statusBarsPadding here (that was creating the top gap).
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            /*
             * -----------------------------------------------------
             * WELCOME CARD
             * -----------------------------------------------------
             */

            TeacherWelcomeCard(
                name = user.displayName,
                coachingName = coachingName
            )

            /*
             * -----------------------------------------------------
             * OVERVIEW
             * -----------------------------------------------------
             */

            DashboardSectionTitle(
                text = "My Teaching Overview"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                TeacherStatCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.Group,
                    title = "Students",
                    value =
                        studentCount.toString(),
                    containerColor =
                        ChipColors.green.container,
                    iconColor =
                        ChipColors.green.icon
                )

                TeacherStatCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.MenuBook,
                    title = "Batches",
                    value =
                        activeBatchCount.toString(),
                    containerColor =
                        ChipColors.blue.container,
                    iconColor =
                        ChipColors.blue.icon
                )

                TeacherStatCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.Quiz,
                    title = "Tests",
                    value =
                        activeTestCount.toString(),
                    containerColor =
                        ChipColors.orange.container,
                    iconColor =
                        ChipColors.orange.icon
                )
            }

            /*
             * -----------------------------------------------------
             * QUICK ACCESS
             * -----------------------------------------------------
             */

            DashboardSectionTitle(
                text = "Quick Access"
            )

            /*
             * -----------------------------------------------------
             * ROW 1
             * -----------------------------------------------------
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                TeacherCompactFeatureCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.EventAvailable,
                    title = "Attendance",
                    subtitle = "Manage attendance",
                    iconBackground =
                        ChipColors.green.container,
                    iconColor =
                        ChipColors.green.icon,
                    onClick = {
                        // Existing attendance flow
                        // remains unchanged.
                    }
                )

                TeacherCompactFeatureCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.Quiz,
                    title = "Tests & Questions",
                    subtitle = "Create tests & MCQs",
                    iconBackground =
                        ChipColors.purple.container,
                    iconColor =
                        ChipColors.purple.icon,
                    onClick = {
                        // Open test list first — create paper, then questions, then submit for approval
                        showTestScreen = true
                    }
                )
            }

            /*
             * -----------------------------------------------------
             * ROW 2
             * -----------------------------------------------------
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                TeacherCompactFeatureCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.AssignmentTurnedIn,
                    title = "Marks & Results",
                    subtitle = "Manage student results",
                    iconBackground =
                        ChipColors.teal.container,
                    iconColor =
                        ChipColors.teal.icon,
                    onClick = {
                        showResultScreen = true
                    }
                )

                TeacherCompactNoticeCard(
                    modifier =
                        Modifier.weight(1f),
                    unreadCount =
                        unreadNoticeCount,
                    onClick = {
                        showNoticeScreen = true
                    }
                )
            }

            /*
             * -----------------------------------------------------
             * ROW 3
             * -----------------------------------------------------
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                TeacherCompactFeatureCard(
                    modifier =
                        Modifier.weight(1f),
                    icon =
                        Icons.Default.MenuBook,
                    title = "My Batches",
                    subtitle =
                        if (assignedBatches.isEmpty()) {
                            "No batches yet"
                        } else {
                            "$activeBatchCount active batch" +
                                    if (activeBatchCount == 1) {
                                        ""
                                    } else {
                                        "es"
                                    }
                        },
                    iconBackground =
                        ChipColors.indigo.container,
                    iconColor =
                        ChipColors.indigo.icon,
                    onClick = {
                        // My Batches section
                        // is shown below.
                    }
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )
            }

            /*
             * -----------------------------------------------------
             * MY BATCHES
             * -----------------------------------------------------
             */

            DashboardSectionTitle(
                text = "My Batches"
            )

            if (assignedBatches.isEmpty()) {

                TeacherNoBatchCard()

            } else {

                assignedBatches.forEach { batch ->

                    val batchStudents =
                        StudentStore
                            .searchStudentsByBatch(
                                coachingId =
                                    coachingId,
                                batchId =
                                    batch.id,
                                query = ""
                            )

                    val batchTests =
                        TestStore
                            .findTestsByBatchAndCoaching(
                                batchId =
                                    batch.id,
                                coachingId =
                                    coachingId
                            )

                    TeacherBatchCard(
                        batchName =
                            batch.name,
                        batchCode =
                            batch.code,
                        course =
                            batch.course,
                        studentCount =
                            batchStudents.size,
                        testCount =
                            batchTests.size,
                        isActive =
                            batch.status.equals(
                                "ACTIVE",
                                ignoreCase = true
                            )
                    )
                }
            }

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(14.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text =
                            "Appearance",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    ThemeModeSelector(
                        current =
                            themeMode,

                        onSelect =
                            setThemeMode
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )
        }

        SLHBottomNavBar(
            items = BottomNavItems.teacher,
            currentRoute = "dashboard",
            onNavigate = { route ->
                when (route) {
                    // Tests first (create paper), questions managed from inside test screen
                    "questions" -> showTestScreen = true
                    "tests" -> showTestScreen = true
                    "notices" -> showNoticeScreen = true
                    "results" -> showResultScreen = true
                }
            }
        )
    }
}


/*
 * =============================================================
 * WELCOME CARD
 * =============================================================
 */

@Composable
private fun TeacherWelcomeCard(
    name: String,
    coachingName: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    ChipColors.blue.container
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(54.dp)
                        .background(
                            ChipColors.blue.icon,
                            RoundedCornerShape(18.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Group,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(30.dp)
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
                        "Welcome, $name",
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        ChipColors.blue.icon,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        "Teacher Dashboard",
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        ChipColors.blue.icon
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        coachingName,
                    fontSize = 12.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}


/*
 * =============================================================
 * STAT CARD
 * =============================================================
 */

@Composable
private fun TeacherStatCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    containerColor: Color,
    iconColor: Color
) {

    Card(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    containerColor
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 11.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(
                            Color.White.copy(
                                alpha = 0.75f
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        iconColor,
                    modifier =
                        Modifier.size(21.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    iconColor
            )

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Medium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


/*
 * =============================================================
 * COMPACT FEATURE CARD
 * =============================================================
 */

@Composable
private fun TeacherCompactFeatureCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    Card(
        modifier =
            modifier
                .height(116.dp)
                .clickable {
                    onClick()
                },
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
                Modifier
                    .fillMaxSize()
                    .padding(11.dp),
            verticalArrangement =
                Arrangement.Center
        ) {

            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .background(
                            iconBackground,
                            RoundedCornerShape(12.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        iconColor,
                    modifier =
                        Modifier.size(22.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


/*
 * =============================================================
 * NOTICE CARD
 * =============================================================
 */

@Composable
private fun TeacherCompactNoticeCard(
    modifier: Modifier,
    unreadCount: Int,
    onClick: () -> Unit
) {

    val hasUnread =
        unreadCount > 0

    val cardColor =
        if (hasUnread) {
            ChipColors.red.container
        } else {
            MaterialTheme.colorScheme.surface
        }

    val iconBackground =
        if (hasUnread) {
            ChipColors.red.container
        } else {
            ChipColors.amber.container
        }

    val iconColor =
        if (hasUnread) {
            ChipColors.red.icon
        } else {
            ChipColors.amber.icon
        }

    Card(
        modifier =
            modifier
                .height(116.dp)
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(11.dp),
                verticalArrangement =
                    Arrangement.Center
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .background(
                                iconBackground,
                                RoundedCornerShape(12.dp)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Notifications,
                        contentDescription =
                            null,
                        tint =
                            iconColor,
                        modifier =
                            Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                Text(
                    text = "Notices",
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        if (hasUnread) {
                            if (unreadCount == 1) {
                                "1 unread notice"
                            } else {
                                "$unreadCount unread notices"
                            }
                        } else {
                            "View coaching notices"
                        },
                    fontSize = 12.sp,
                    color =
                        if (hasUnread) {
                            ChipColors.red.icon
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            if (hasUnread) {

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.TopEnd
                            )
                            .padding(
                                top = 9.dp,
                                end = 9.dp
                            )
                            .size(10.dp)
                            .background(
                                ChipColors.red.icon,
                                RoundedCornerShape(50)
                            )
                )
            }
        }
    }
}


/*
 * =============================================================
 * BATCH CARD
 * =============================================================
 */

@Composable
private fun TeacherBatchCard(
    batchName: String,
    batchCode: String,
    course: String,
    studentCount: Int,
    testCount: Int,
    isActive: Boolean
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
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                            .size(44.dp)
                            .background(
                                ChipColors.indigo.container,
                                RoundedCornerShape(14.dp)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.MenuBook,
                        contentDescription =
                            null,
                        tint =
                            ChipColors.indigo.icon,
                        modifier =
                            Modifier.size(25.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.size(11.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = batchName,
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    if (batchCode.isNotBlank()) {

                        Text(
                            text =
                                "Code: $batchCode",
                            fontSize = 12.sp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }

                    if (course.isNotBlank()) {

                        Text(
                            text = course,
                            fontSize = 10.sp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .background(
                                if (isActive) {
                                    ChipColors.green.container
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(50)
                            )
                            .padding(
                                horizontal = 9.dp,
                                vertical = 5.dp
                            )
                ) {

                    Text(
                        text =
                            if (isActive) {
                                "ACTIVE"
                            } else {
                                "INACTIVE"
                            },
                        fontSize = 10.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            if (isActive) {
                                ChipColors.green.icon
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                    )
                }
            }

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

                TeacherInfoChip(
                    text =
                        "$studentCount Students",
                    background =
                        ChipColors.blue.container,
                    textColor =
                        ChipColors.blue.icon
                )

                TeacherInfoChip(
                    text =
                        "$testCount Tests",
                    background =
                        ChipColors.orange.container,
                    textColor =
                        ChipColors.orange.icon
                )
            }
        }
    }
}


/*
 * =============================================================
 * NO BATCH CARD
 * =============================================================
 */

@Composable
private fun TeacherNoBatchCard() {

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
                    .padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .background(
                            ChipColors.indigo.container,
                            RoundedCornerShape(14.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.MenuBook,
                    contentDescription =
                        null,
                    tint =
                        ChipColors.indigo.icon,
                    modifier =
                        Modifier.size(26.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = "No batch assigned",
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    "No batch is currently assigned to you.",
                fontSize = 12.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


/*
 * =============================================================
 * INFO CHIP
 * =============================================================
 */

@Composable
private fun TeacherInfoChip(
    text: String,
    background: Color,
    textColor: Color
) {

    Box(
        modifier =
            Modifier
                .background(
                    background,
                    RoundedCornerShape(50)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                )
    ) {

        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight =
                FontWeight.SemiBold,
            color =
                textColor,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}