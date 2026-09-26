package com.slh.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudentDashboardScreen(
    user: DemoUser,
    coaching: CoachingProfile?,
    onAttendance: () -> Unit,
    onFees: () -> Unit,
    onProfile: () -> Unit = {},
    onNotices: () -> Unit = {}
) {

    var showResultScreen by remember {
        mutableStateOf(false)
    }

    var showTestScreen by remember {
        mutableStateOf(false)
    }

    val student =
        StudentStore.findStudentByUsername(
            user.username
        )

    // =========================================================
    // STUDENT TEST SCREEN
    // =========================================================

    if (
        showTestScreen &&
        student != null
    ) {

        StudentTestScreen(
            student = student,
            onBack = {
                showTestScreen = false
            }
        )

        return
    }

    // =========================================================
    // STUDENT RESULT SCREEN
    // =========================================================

    if (
        showResultScreen &&
        student != null
    ) {

        StudentResultScreen(
            student = student,
            onBack = {
                showResultScreen = false
            }
        )

        return
    }



    // =========================================================
    // FEE SUMMARY
    // =========================================================

    val feeSummary =
        student?.let { currentStudent ->

            FeeStore.getSummary(
                studentId =
                    currentStudent.id,

                coachingId =
                    currentStudent.coachingId
            )
        }

    // =========================================================
    // OVERDUE FEES
    // =========================================================

    val overdueCount =
        feeSummary
            ?.installments
            ?.count {
                    installment: FeeInstallment ->

                installment.status ==
                        FeeInstallmentStatus.OVERDUE
            }
            ?: 0

    // =========================================================
    // UNREAD NOTICES
    // =========================================================

    val unreadNoticeCount =
        student?.let { currentStudent ->

            NoticeStore.getUnreadNoticeCount(
                coachingId =
                    currentStudent.coachingId,

                userId =
                    currentStudent.id
            )
        }
            ?: 0

    // =========================================================
    // MAIN DASHBOARD
    // =========================================================

    Column(

        modifier =
            Modifier
                .fillMaxSize()

                // Scroll first
                .verticalScroll(
                    rememberScrollState()
                )

                // Horizontal and top spacing
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        // =====================================================
        // WELCOME
        // =====================================================

        StudentWelcomeCard(

            name =
                user.displayName,

            coachingName =
                coaching?.name
                    ?: "Your learning space"
        )

        // =====================================================
        // MY LEARNING
        // =====================================================

        StudentSectionTitle(
            text = "My Learning"
        )

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            StudentStatCard(

                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.Today,

                title =
                    "Attendance",

                value =
                    student?.let {
                            currentStudent ->

                        AttendanceStatusStore
                            .getStudentAttendancePercentage(
                                currentStudent.id
                            )
                            .toString() + "%"
                    }
                        ?: "0%",

                backgroundColor =
                    ChipColors.green.container,

                iconColor =
                    ChipColors.green.icon
            )

            StudentStatCard(

                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.Assessment,

                title =
                    "Average",

                value =
                    "—",

                backgroundColor =
                    ChipColors.blue.container,

                iconColor =
                    ChipColors.blue.icon
            )
        }

        // =====================================================
        // QUICK ACCESS
        // =====================================================

        StudentSectionTitle(
            text = "Quick Access"
        )

        // -----------------------------------------------------
        // ROW 1
        // Attendance / Tests
        // -----------------------------------------------------

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            StudentFeatureCard(

                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.Today,

                title =
                    "Attendance",

                subtitle =
                    "Today's attendance",

                iconBackground =
                    ChipColors.green.container,

                iconColor =
                    ChipColors.green.icon,

                onClick =
                    onAttendance
            )

            StudentFeatureCard(

                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.Quiz,

                title =
                    "Tests & Quiz",

                subtitle =
                    "Available tests",

                iconBackground =
                    ChipColors.purple.container,

                iconColor =
                    ChipColors.purple.icon,

                onClick = {

                    if (
                        student != null
                    ) {

                        showTestScreen =
                            true
                    }
                }
            )
        }

        // -----------------------------------------------------
        // ROW 2
        // Results / Fees
        // -----------------------------------------------------

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            StudentFeatureCard(

                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Default.Assessment,

                title =
                    "Results",

                subtitle =
                    "Marks & results",

                iconBackground =
                    ChipColors.teal.container,

                iconColor =
                    ChipColors.teal.icon,

                onClick = {

                    if (
                        student != null
                    ) {

                        showResultScreen =
                            true
                    }
                }
            )

            Box(

                modifier =
                    Modifier
                        .weight(1f)
                        .height(116.dp)
            ) {

                DashboardStudentFeeCard(

                    summary =
                        feeSummary,

                    overdueCount =
                        overdueCount,

                    onClick =
                        onFees
                )
            }
        }

        // -----------------------------------------------------
        // ROW 3
        // Notices / Information
        // -----------------------------------------------------

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            StudentNoticeFeatureCard(

                modifier =
                    Modifier.weight(1f),

                unreadNoticeCount =
                    unreadNoticeCount,

                onClick = {
                    if (student != null) {
                        onNotices()
                    }
                }
            )

            StudentInfoCard(
                modifier = Modifier.weight(1f),
                onClick = onProfile
            )
        }

        // =====================================================
        // BOTTOM LOGO AREA
        // =====================================================

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        StudentBottomLogo()

        // Important:
        // Extra scrollable bottom space prevents the logo
        // from being hidden behind the system navigation area.
        Spacer(
            modifier =
                Modifier.height(
                    48.dp
                )
        )
    }
}


// =============================================================
// BOTTOM SLH LOGO
// =============================================================

@Composable
private fun StudentBottomLogo() {

    val isDark =
        isSystemInDarkTheme()

    val logoRes =
        if (isDark) {
            R.drawable.slh_word_logo_dark
        } else {
            R.drawable.slh_word_logo_light
        }

    androidx.compose.foundation.Image(

        painter =
            painterResource(
                id = logoRes
            ),

        contentDescription =
            "Sohan's Learning Hub",

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),

        contentScale =
            ContentScale.Fit
    )
}


// =============================================================
// WELCOME CARD
// =============================================================

@Composable
private fun StudentWelcomeCard(
    name: String,
    coachingName: String
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    ChipColors.blue.container
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(

                text =
                    "Welcome, $name",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ChipColors.blue.icon,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(

                text =
                    coachingName,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    ChipColors.blue.icon,

                maxLines =
                    2,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================
// SECTION TITLE
// =============================================================

@Composable
private fun StudentSectionTitle(
    text: String
) {

    Text(

        text =
            text,

        style =
            MaterialTheme
                .typography
                .titleSmall,

        fontWeight =
            FontWeight.Bold,

        color =
            MaterialTheme
                .colorScheme
                .onSurface
    )
}


// =============================================================
// STAT CARD
// =============================================================

@Composable
private fun StudentStatCard(

    modifier: Modifier,

    icon: ImageVector,

    title: String,

    value: String,

    backgroundColor: Color,

    iconColor: Color
) {

    Card(

        modifier =
            modifier
                .height(96.dp),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            color =
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.75f
                                ),
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
                        icon,

                    contentDescription =
                        null,

                    tint =
                        iconColor,

                    modifier =
                        Modifier.size(
                            22.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        10.dp
                    )
            )

            Column {

                Text(

                    text =
                        title,

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(

                    text =
                        value,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        iconColor,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}


// =============================================================
// FEATURE CARD
// =============================================================

@Composable
private fun StudentFeatureCard(

    modifier: Modifier,

    icon: ImageVector,

    title: String,

    subtitle: String,

    iconBackground: Color,

    iconColor: Color,

    onClick: (() -> Unit)? = null
) {

    Card(

        modifier =
            modifier
                .height(116.dp)
                .then(

                    if (
                        onClick != null
                    ) {

                        Modifier.clickable {
                            onClick()
                        }

                    } else {

                        Modifier
                    }
                ),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        12.dp
                    ),

            horizontalAlignment =
                Alignment.Start,

            verticalArrangement =
                Arrangement.Center
        ) {

            Box(

                modifier =
                    Modifier
                        .size(42.dp)
                        .background(
                            color =
                                iconBackground,
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
                        icon,

                    contentDescription =
                        null,

                    tint =
                        iconColor,

                    modifier =
                        Modifier.size(
                            23.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(

                text =
                    title,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Text(

                text =
                    subtitle,

                fontSize =
                    10.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================
// NOTICE FEATURE CARD
// =============================================================

@Composable
private fun StudentNoticeFeatureCard(

    modifier: Modifier,

    unreadNoticeCount: Int,

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
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        unreadNoticeCount > 0
                    ) {

                        ChipColors.orange.container

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        12.dp
                    ),

            verticalArrangement =
                Arrangement.Center
        ) {

            Box {

                Box(

                    modifier =
                        Modifier
                            .size(42.dp)
                            .background(
                                color =
                                    ChipColors.amber.container,
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
                            Icons.Default.Campaign,

                        contentDescription =
                            "Notices",

                        tint =
                            ChipColors.orange.icon,

                        modifier =
                            Modifier.size(
                                23.dp
                            )
                    )
                }

                if (
                    unreadNoticeCount > 0
                ) {

                    Box(

                        modifier =
                            Modifier
                                .size(10.dp)
                                .background(
                                    color =
                                        ChipColors.red.icon,
                                    shape =
                                        androidx.compose.foundation
                                            .shape
                                            .CircleShape
                                )
                                .align(
                                    Alignment.TopEnd
                                )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(

                text =
                    "Notices",

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Text(

                text =
                    if (
                        unreadNoticeCount > 0
                    ) {

                        "$unreadNoticeCount unread notice"

                    } else {

                        "Latest announcements"
                    },

                fontSize =
                    10.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================
// INFORMATION CARD
// =============================================================

@Composable
private fun StudentInfoCard(
    modifier: Modifier,
    onClick: () -> Unit = {}
) {

    Card(

        modifier =
            modifier
                .height(116.dp)
                .clickable(onClick = onClick),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    ChipColors.purple.container
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        12.dp
                    ),

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(

                text =
                    "My Profile",

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    ChipColors.purple.icon,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(

                text =
                    "Student account",

                fontSize =
                    10.sp,

                color =
                    ChipColors.purple.icon,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(

                text =
                    "Learning Dashboard",

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Medium,

                color =
                    ChipColors.purple.icon,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}