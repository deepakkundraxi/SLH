package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDashboardScreen(
    user: DemoUser,
    onLogout: () -> Unit,
    onCoachingProfile: () -> Unit,
    onStudentManagement: () -> Unit,
    onResultAnalytics: () -> Unit = {}
) {

    var showStudentGpsAttendance by remember {
        mutableStateOf(false)
    }

    var showStudentFees by remember {
        mutableStateOf(false)
    }

    var showTeacherManagement by remember {
        mutableStateOf(false)
    }

    var showBatchManagement by remember {
        mutableStateOf(false)
    }

    var showFeeManagement by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // NEW: FEE REPORT
    // =========================================================

    var showFeeReport by remember {
        mutableStateOf(false)
    }

    var showPaymentVerification by remember {
        mutableStateOf(false)
    }

    var showAdminAttendance by remember {
        mutableStateOf(false)
    }

    var showTestManagement by remember {
        mutableStateOf(false)
    }

    var showTestApproval by remember {
        mutableStateOf(false)
    }

    var showResultManagement by remember {
        mutableStateOf(false)
    }

    var showNoticeManagement by remember {
        mutableStateOf(false)
    }

    var showCoachingProfile by remember {
        mutableStateOf(false)
    }

    var showResultAnalytics by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // STUDENT NOTICE + PROFILE
    // =========================================================

    var showStudentNotice by remember {
        mutableStateOf(false)
    }

    var showStudentProfile by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // STUDENT LOOKUP
    // =========================================================

    val student =
        if (user.role == UserRole.STUDENT) {

            StudentStore.findStudentByUsername(
                user.username
            )

        } else {
            null
        }

    // =========================================================
    // STUDENT BOTTOM NAVIGATION
    // =========================================================

    val navigateToStudentTab: (String) -> Unit = { route ->

        showStudentGpsAttendance = false
        showStudentFees = false
        showStudentNotice = false
        showStudentProfile = false

        when (route) {

            "attendance" -> {
                showStudentGpsAttendance = true
            }

            "fees" -> {
                showStudentFees = true
            }

            "notices" -> {
                showStudentNotice = true
            }

            "profile" -> {
                showStudentProfile = true
            }

            "dashboard" -> {
                // All sections remain closed.
            }
        }
    }

    // =========================================================
    // STUDENT ATTENDANCE
    // =========================================================

    if (showStudentGpsAttendance) {

        if (student == null) {

            showStudentGpsAttendance = false

        } else {

            StudentGpsAttendanceScreen(
                studentId = student.id,

                onBack = {
                    showStudentGpsAttendance = false
                },

                onNavigateTab = navigateToStudentTab
            )

            return
        }
    }

    // =========================================================
    // STUDENT FEES & PAYMENTS
    // =========================================================

    if (showStudentFees) {

        if (student == null) {

            showStudentFees = false

        } else {

            // StudentFeeScreen = pending + "Maine fee jama kar di"
            // (History screen only shows past payments, no submit button)
            StudentFeeScreen(
                studentId = student.id,
                onBack = {
                    showStudentFees = false
                },
                onNavigateTab = navigateToStudentTab
            )

            return
        }
    }

    // =========================================================
    // STUDENT NOTICES
    // =========================================================

    if (showStudentNotice) {

        if (student == null) {

            showStudentNotice = false

        } else {

            StudentNoticeScreen(
                student = student,

                onBack = {
                    showStudentNotice = false
                },

                onNavigateTab =
                    navigateToStudentTab
            )

            return
        }
    }

    // =========================================================
    // STUDENT PROFILE
    // =========================================================

    if (showStudentProfile) {

        if (student == null) {

            showStudentProfile = false

        } else {

            StudentProfileScreen(
                studentId = student.id,

                onBack = {
                    showStudentProfile = false
                },

                onEdit = { },

                onAttendance = {

                    showStudentProfile = false
                    showStudentGpsAttendance = true
                },

                onNavigateTab =
                    navigateToStudentTab
            )

            return
        }
    }

    // =========================================================
    // DASHBOARD BACK
    // Sub-screen open ho to pehle woh band; warna logout.
    // Pehle hamesha logout ho jata tha — isliye back = logout lagta tha.
    // =========================================================

    BackHandler {
        when {
            showTeacherManagement -> showTeacherManagement = false
            showBatchManagement -> showBatchManagement = false
            showFeeManagement -> showFeeManagement = false
            showFeeReport -> showFeeReport = false
            showPaymentVerification -> showPaymentVerification = false
            showAdminAttendance -> showAdminAttendance = false
            showTestManagement -> showTestManagement = false
            showTestApproval -> showTestApproval = false
            showResultManagement -> showResultManagement = false
            showNoticeManagement -> showNoticeManagement = false
            showCoachingProfile -> showCoachingProfile = false
            showResultAnalytics -> showResultAnalytics = false
            showStudentGpsAttendance -> showStudentGpsAttendance = false
            showStudentFees -> showStudentFees = false
            showStudentNotice -> showStudentNotice = false
            showStudentProfile -> showStudentProfile = false
            else -> onLogout()
        }
    }

    // =========================================================
    // COACHING PROFILE
    // =========================================================

    val coaching =
        user.coachingId?.let {
            CoachingProfileStore.get(it)
        }

    // =========================================================
    // MAIN DASHBOARD
    // =========================================================

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        contentWindowInsets =
            WindowInsets(0, 0, 0, 0),

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomBar = {

            if (user.role == UserRole.STUDENT) {

                val studentRoute = when {

                    showStudentGpsAttendance ->
                        "attendance"

                    showStudentFees ->
                        "fees"

                    showStudentNotice ->
                        "notices"

                    showStudentProfile ->
                        "profile"

                    else ->
                        "dashboard"
                }

                SLHBottomNavBar(

                    items =
                        BottomNavItems.student,

                    currentRoute =
                        studentRoute,

                    onNavigate = { route ->

                        navigateToStudentTab(
                            route
                        )
                    }
                )

            } else if (
                user.role ==
                UserRole.COACHING_ADMIN
            ) {

                val adminRoute = when {

                    showBatchManagement ->
                        "batches"

                    showFeeManagement ->
                        "fees"

                    showTeacherManagement ->
                        "teachers"

                    else ->
                        "dashboard"
                }

                SLHBottomNavBar(

                    items =
                        BottomNavItems.admin,

                    currentRoute =
                        adminRoute,

                    onNavigate = { route ->

                        when (route) {

                            "dashboard" -> {

                                showBatchManagement =
                                    false

                                showFeeManagement =
                                    false

                                showTeacherManagement =
                                    false

                                showTestManagement =
                                    false

                                showResultManagement =
                                    false

                                showNoticeManagement =
                                    false

                                showCoachingProfile =
                                    false

                                showResultAnalytics =
                                    false

                                showFeeReport =
                                    false

                                showPaymentVerification =
                                    false

                                showAdminAttendance =
                                    false
                            }

                            "batches" -> {

                                showBatchManagement =
                                    true

                                showFeeManagement =
                                    false

                                showTeacherManagement =
                                    false

                                showFeeReport =
                                    false

                                showPaymentVerification =
                                    false

                                showAdminAttendance =
                                    false
                            }

                            "fees" -> {

                                showFeeManagement =
                                    true

                                showBatchManagement =
                                    false

                                showTeacherManagement =
                                    false

                                showFeeReport =
                                    false

                                showPaymentVerification =
                                    false

                                showAdminAttendance =
                                    false
                            }

                            "teachers" -> {

                                showTeacherManagement =
                                    true

                                showBatchManagement =
                                    false

                                showFeeManagement =
                                    false

                                showFeeReport =
                                    false

                                showPaymentVerification =
                                    false

                                showAdminAttendance =
                                    false
                            }
                        }
                    }
                )
            }
        },

        // =====================================================
        // TOP BAR
        // =====================================================

        topBar = {

            if (
                user.role !=
                UserRole.COACHING_ADMIN ||

                (
                        !showBatchManagement &&
                                !showFeeManagement &&
                                !showTeacherManagement &&
                                !showTestManagement &&
                                !showTestApproval &&
                                !showResultManagement &&
                                !showNoticeManagement &&
                                !showCoachingProfile &&
                                !showResultAnalytics &&
                                !showFeeReport &&
                                !showPaymentVerification &&
                                !showAdminAttendance
                        )
            ) {

                TopAppBar(

                    title = {

                        Column {

                            Text(
                                text =
                                    when (user.role) {

                                        UserRole.COACHING_ADMIN ->
                                            "Admin Dashboard"

                                        UserRole.TEACHER ->
                                            "Teacher Dashboard"

                                        UserRole.STUDENT ->
                                            "Student Dashboard"

                                        UserRole.PRINCIPAL_ADMIN ->
                                            "Principal Admin"
                                    },

                                fontWeight =
                                    FontWeight.Bold
                            )

                            coaching?.let {

                                Text(
                                    text =
                                        it.name,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    },

                    navigationIcon = {

                        if (
                            coaching?.logoUri != null
                        ) {

                            AsyncImage(

                                model =
                                    coaching.logoUri,

                                contentDescription =
                                    "Coaching Logo",

                                modifier =
                                    Modifier
                                        .padding(
                                            start = 12.dp
                                        )
                                        .size(40.dp)
                                        .clip(
                                            CircleShape
                                        ),

                                contentScale =
                                    ContentScale.Crop
                            )

                        } else {

                            Box(

                                modifier =
                                    Modifier
                                        .padding(
                                            start = 12.dp
                                        )
                                        .size(40.dp)
                                        .clip(
                                            CircleShape
                                        )
                                        .background(
                                            MaterialTheme
                                                .colorScheme
                                                .primaryContainer
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(

                                    text =
                                        coaching
                                            ?.shortName
                                            ?.take(2)
                                            ?.uppercase()
                                            ?: "SLH",

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        }
                    },

                    actions = {

                        IconButton(
                            onClick =
                                onLogout
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Logout,

                                contentDescription =
                                    "Logout"
                            )
                        }
                    },

                    colors =
                        TopAppBarDefaults
                            .topAppBarColors()
                )
            }
        }

    ) { paddingValues ->

        /*
         * Do not add verticalScroll() here.
         *
         * Child screens already manage their own
         * LazyColumn / scrolling.
         */

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    ),

            verticalArrangement =
                Arrangement.spacedBy(0.dp)
        ) {

            when (user.role) {

                // =================================================
                // ADMIN
                // =================================================

                UserRole.COACHING_ADMIN -> {

                    val coachingId =
                        user.coachingId

                    when {

                        showTeacherManagement &&
                                coachingId != null -> {

                            TeacherManagementScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showTeacherManagement =
                                        false
                                }
                            )
                        }

                        showBatchManagement &&
                                coachingId != null -> {

                            BatchManagementScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showBatchManagement =
                                        false
                                }
                            )
                        }

                        showFeeManagement &&
                                coachingId != null -> {

                            FeeManagementScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showFeeManagement =
                                        false
                                }
                            )
                        }

                        // =================================================
                        // NEW: FEE REPORT
                        // =================================================

                        showFeeReport &&
                                coachingId != null -> {

                            FeeReportScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showFeeReport =
                                        false
                                }
                            )
                        }

                        showPaymentVerification &&
                                coachingId != null -> {

                            PaymentVerificationScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showPaymentVerification =
                                        false
                                }
                            )
                        }

                        showAdminAttendance &&
                                coachingId != null -> {

                            AdminAttendanceScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showAdminAttendance =
                                        false
                                }
                            )
                        }

                        showTestManagement &&
                                coachingId != null -> {

                            TestManagementScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showTestManagement =
                                        false
                                }
                            )
                        }

                        showTestApproval -> {

                            AdminTestApprovalScreen(

                                user = user,

                                onBack = {
                                    showTestApproval =
                                        false
                                }
                            )
                        }

                        showResultManagement &&
                                coachingId != null -> {

                            ResultManagementScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showResultManagement =
                                        false
                                }
                            )
                        }

                        showNoticeManagement &&
                                coachingId != null -> {

                            NoticeManagementScreen(

                                coachingId =
                                    coachingId,

                                adminUser =
                                    user,

                                onBack = {
                                    showNoticeManagement =
                                        false
                                }
                            )
                        }

                        showCoachingProfile &&
                                coachingId != null -> {

                            CoachingProfileScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showCoachingProfile =
                                        false
                                }
                            )
                        }

                        showResultAnalytics &&
                                coachingId != null -> {

                            ResultAnalyticsScreen(

                                coachingId =
                                    coachingId,

                                onBack = {
                                    showResultAnalytics =
                                        false
                                }
                            )
                        }

                        else -> {

                            AdminDashboardScreen(

                                user =
                                    user,

                                coaching =
                                    coaching,

                                onCoachingProfile = {
                                    showCoachingProfile =
                                        true
                                },

                                onStudentManagement =
                                    onStudentManagement,

                                onTeacherManagement = {
                                    showTeacherManagement =
                                        true
                                },

                                onBatchManagement = {
                                    showBatchManagement =
                                        true
                                },

                                onFeeManagement = {
                                    showFeeManagement =
                                        true
                                },

                                // =================================================
                                // NEW: FEE REPORT CALLBACK
                                // =================================================

                                onFeeReport = {
                                    showFeeReport =
                                        true
                                },

                                onPaymentVerification = {
                                    showPaymentVerification =
                                        true
                                },

                                onAttendance = {
                                    showAdminAttendance =
                                        true
                                },

                                onTestManagement = {
                                    showTestManagement =
                                        true
                                },

                                onTestApproval = {
                                    showTestApproval =
                                        true
                                },

                                onResultManagement = {
                                    showResultManagement =
                                        true
                                },

                                onNoticeManagement = {
                                    showNoticeManagement =
                                        true
                                },

                                onResultAnalytics = {
                                    showResultAnalytics =
                                        true
                                }
                            )
                        }
                    }
                }

                // =================================================
                // TEACHER
                // =================================================

                UserRole.TEACHER -> {

                    TeacherDashboardScreen(

                        user =
                            user,

                        coaching =
                            coaching
                    )
                }

                // =================================================
                // STUDENT
                // =================================================

                UserRole.STUDENT -> {

                    StudentDashboardScreen(

                        user =
                            user,

                        coaching =
                            coaching,

                        onAttendance = {

                            if (student != null) {

                                showStudentGpsAttendance =
                                    true
                            }
                        },

                        onFees = {

                            if (student != null) {

                                showStudentFees =
                                    true
                            }
                        },

                        onProfile = {

                            if (student != null) {

                                showStudentProfile =
                                    true
                            }
                        },

                        onNotices = {

                            if (student != null) {

                                showStudentNotice =
                                    true
                            }
                        }
                    )
                }

                // =================================================
                // PRINCIPAL ADMIN
                // =================================================

                UserRole.PRINCIPAL_ADMIN -> {

                    PrincipalMiniDashboard(
                        user =
                            user
                    )
                }
            }
        }
    }
}