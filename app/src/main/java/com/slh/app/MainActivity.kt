package com.slh.app

import android.os.Bundle

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.Surface

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier

import com.slh.app.ui.theme.SLHTheme



class MainActivity : FragmentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * =====================================================
         * LOCAL STORAGE INITIALIZATION
         * =====================================================
         */

        SLHLocalStorage.init(
            applicationContext
        )

        SLHFirebase.init(
            applicationContext
        )

        SessionManager.init(
            applicationContext
        )

        AppLocale.init(
            applicationContext
        )

        StudentStore.initialize()

        TeacherStore.initialize()

        BatchStore.initialize()

        UserAccountStore.initialize()

        CoachingProfileStore.initialize()

        PaymentStore.initialize()

        FeeStore.initialize()

        AttendanceStatusStore.initialize()

        StudentGpsAttendanceStore.initialize()

        NoticeStore.initialize()

        QuestionStore.initialize()

        TestStore.initialize()



        /*
         * =====================================================
         * EDGE TO EDGE
         * =====================================================
         */

        enableEdgeToEdge()


        /*
         * =====================================================
         * COMPOSE CONTENT
         * =====================================================
         */

        setContent {


            /*
             * =================================================
             * THEME MODE
             * =================================================
             */

            val (
                themeMode,
                setThemeMode
            ) = rememberThemeMode()


            /*
             * =================================================
             * DARK THEME
             * =================================================
             */

            val darkTheme =
                when (themeMode) {

                    ThemeMode.DARK ->
                        true

                    ThemeMode.LIGHT ->
                        false

                    ThemeMode.SYSTEM ->
                        isSystemInDarkTheme()
                }


            SLHTheme(
                darkTheme = darkTheme
            ) {


                Surface(
                    modifier =
                        Modifier.fillMaxSize()
                ) {


                    /*
                     * =========================================
                     * CURRENT SCREEN
                     * =========================================
                     */

                    var currentScreen by remember {

                        mutableStateOf(
                            "login"
                        )
                    }


                    /*
                     * =========================================
                     * LOGGED-IN USER
                     * =========================================
                     */

                    var loggedInUser by remember {

                        mutableStateOf<DemoUser?>(
                            null
                        )
                    }


                    /*
                     * =========================================
                     * SLH GITHUB UPDATE
                     * =========================================
                     *
                     * App startup पर GitHub से latest version
                     * check किया जाएगा.
                     */

                    var updateInfo by remember {

                        mutableStateOf<SLHUpdateInfo?>(
                            null
                        )
                    }


                    /*
                     * =========================================
                     * CHECK FOR APP UPDATE
                     * =========================================
                     *
                     * यह check background में होगा.
                     *
                     * Internet unavailable होने पर app normal
                     * तरीके से चलता रहेगा.
                     */

                    LaunchedEffect(Unit) {

                        updateInfo =
                            AppUpdateManager.checkForUpdate(
                                this@MainActivity
                            )
                    }


                    /*
                     * =========================================
                     * NAVIGATION
                     * =========================================
                     */

                    when (currentScreen) {


                        // =====================================================
                        // LOGIN
                        // =====================================================

                        "login" -> {


                            /*
                             * Every visit to login screen ends
                             * previous Firebase session.
                             */

                            LaunchedEffect(Unit) {

                                SLHFirebase.signOutAll()
                            }


                            LoginScreen(

                                onLoginSuccess = { user ->


                                    loggedInUser =
                                        user


                                    /*
                                     * Biometric
                                     */

                                    if (
                                        BiometricHelper.isEnabled(
                                            this@MainActivity
                                        )
                                    ) {

                                        BiometricHelper.enable(

                                            this@MainActivity,

                                            user.username,

                                            user.role
                                        )
                                    }


                                    /*
                                     * =====================================
                                     * FIREBASE COACHING DATA SYNC
                                     * =====================================
                                     */

                                    user.coachingId
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?.let { coachingId ->


                                            when (user.role) {


                                                // =================================================
                                                // COACHING ADMIN
                                                // =================================================

                                                UserRole.COACHING_ADMIN -> {


                                                    BatchStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    StudentStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    TeacherStore.syncFromFirebase(

                                                        coachingId,

                                                        asAdmin = true
                                                    )


                                                    FeeStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    PaymentStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    NoticeStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    TestStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    QuestionStore.syncFromFirebase(

                                                        coachingId,

                                                        canWrite = true
                                                    )


                                                    AttendanceStatusStore
                                                        .syncFromFirebase(

                                                            coachingId,

                                                            canWrite = true
                                                        )


                                                    StudentGpsAttendanceStore
                                                        .syncFromFirebase(

                                                            coachingId,

                                                            canWrite = true
                                                        )


                                                    SLHFirebase
                                                        .backfillStudentAuthEmail(

                                                            coachingId
                                                        )
                                                }


                                                // =================================================
                                                // TEACHER
                                                // =================================================

                                                UserRole.TEACHER -> {


                                                    BatchStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    StudentStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    TeacherStore.syncFromFirebase(

                                                        coachingId,

                                                        asAdmin = false,

                                                        ownUsername =
                                                            user.username
                                                    )


                                                    NoticeStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    TestStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    QuestionStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    AttendanceStatusStore
                                                        .syncFromFirebase(

                                                            coachingId
                                                        )


                                                    StudentGpsAttendanceStore
                                                        .syncFromFirebase(

                                                            coachingId
                                                        )
                                                }


                                                // =================================================
                                                // STUDENT
                                                // =================================================

                                                UserRole.STUDENT -> {


                                                    BatchStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    StudentStore
                                                        .syncOwnFromFirebase(

                                                            user.id
                                                        )


                                                    FeeStore.syncOwnFromFirebase(

                                                        coachingId,

                                                        user.id
                                                    )


                                                    PaymentStore.syncOwnFromFirebase(

                                                        coachingId,

                                                        user.id
                                                    )


                                                    TeacherStore.syncFromFirebase(

                                                        coachingId,

                                                        asAdmin = false
                                                    )


                                                    NoticeStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    TestStore.syncFromFirebase(

                                                        coachingId,

                                                        studentId =
                                                            user.id
                                                    )


                                                    QuestionStore.syncFromFirebase(

                                                        coachingId
                                                    )


                                                    AttendanceStatusStore
                                                        .syncFromFirebase(

                                                            coachingId
                                                        )


                                                    StudentGpsAttendanceStore
                                                        .syncFromFirebase(

                                                            coachingId
                                                        )
                                                }


                                                else -> Unit
                                            }
                                        }


                                    /*
                                     * =====================================
                                     * NEXT SCREEN
                                     * =====================================
                                     */

                                    currentScreen =
                                        when {


                                            // ---------------------------------
                                            // PRINCIPAL ADMIN
                                            // ---------------------------------

                                            user.role ==
                                                    UserRole.PRINCIPAL_ADMIN -> {

                                                "principal_admin"
                                            }


                                            // ---------------------------------
                                            // PENDING STUDENT
                                            // ---------------------------------

                                            user.role ==
                                                    UserRole.STUDENT &&
                                                    user.status ==
                                                    AccountStatus.PENDING -> {

                                                "student_activation_pending"
                                            }


                                            // ---------------------------------
                                            // NORMAL USERS
                                            // ---------------------------------

                                            else -> {

                                                "dashboard"
                                            }
                                        }
                                }
                            )
                        }


                        // =====================================================
                        // STUDENT ACTIVATION PENDING
                        // =====================================================

                        "student_activation_pending" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.STUDENT &&
                                    user.status ==
                                    AccountStatus.PENDING
                                ) {


                                    StudentActivationPendingScreen(

                                        user =
                                            user,


                                        onLogout = {

                                            loggedInUser =
                                                null

                                            currentScreen =
                                                "login"
                                        }
                                    )


                                } else {


                                    loggedInUser =
                                        null

                                    currentScreen =
                                        "login"
                                }
                            }
                        }


                        // =====================================================
                        // PRINCIPAL ADMIN
                        // =====================================================

                        "principal_admin" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.PRINCIPAL_ADMIN
                                ) {


                                    PrincipalAdminScreen(

                                        onBack = {

                                            loggedInUser =
                                                null

                                            currentScreen =
                                                "login"
                                        },


                                        onLogout = {

                                            loggedInUser =
                                                null

                                            currentScreen =
                                                "login"
                                        }
                                    )


                                } else {


                                    loggedInUser =
                                        null

                                    currentScreen =
                                        "login"
                                }
                            }
                        }


                        // =====================================================
                        // DASHBOARD
                        // =====================================================

                        "dashboard" -> {


                            loggedInUser?.let { user ->


                                AppDashboardScreen(

                                    user =
                                        user,


                                    onLogout = {

                                        loggedInUser =
                                            null

                                        currentScreen =
                                            "login"
                                    },


                                    onCoachingProfile = {

                                        currentScreen =
                                            "coaching_profile"
                                    },


                                    onStudentManagement = {

                                        if (
                                            user.role ==
                                            UserRole.COACHING_ADMIN
                                        ) {

                                            currentScreen =
                                                "student_management"
                                        }
                                    },


                                    onResultAnalytics = {

                                        if (
                                            user.role ==
                                            UserRole.COACHING_ADMIN
                                        ) {

                                            currentScreen =
                                                "result_analytics"
                                        }
                                    }
                                )
                            }
                        }


                        // =====================================================
                        // STUDENT MANAGEMENT
                        // =====================================================

                        "student_management" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.COACHING_ADMIN &&
                                    user.coachingId != null
                                ) {


                                    StudentManagementScreen(

                                        coachingId =
                                            user.coachingId,


                                        onBack = {

                                            currentScreen =
                                                "dashboard"
                                        }
                                    )


                                } else {


                                    currentScreen =
                                        "dashboard"
                                }
                            }
                        }


                        // =====================================================
                        // COACHING PROFILE
                        // =====================================================

                        "coaching_profile" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.COACHING_ADMIN &&
                                    user.coachingId != null
                                ) {


                                    CoachingProfileScreen(

                                        coachingId =
                                            user.coachingId,


                                        onBack = {

                                            currentScreen =
                                                "dashboard"
                                        }
                                    )


                                } else {


                                    currentScreen =
                                        "dashboard"
                                }
                            }
                        }


                        // =====================================================
                        // ADMIN ATTENDANCE
                        // =====================================================

                        "admin_attendance" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.COACHING_ADMIN &&
                                    user.coachingId != null
                                ) {


                                    AdminAttendanceScreen(

                                        coachingId =
                                            user.coachingId,


                                        onBack = {

                                            currentScreen =
                                                "dashboard"
                                        }
                                    )


                                } else {


                                    currentScreen =
                                        "dashboard"
                                }
                            }
                        }


                        // =====================================================
                        // TEST MANAGEMENT
                        // =====================================================

                        "test_management" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.COACHING_ADMIN &&
                                    user.coachingId != null
                                ) {


                                    TestManagementScreen(

                                        coachingId =
                                            user.coachingId,


                                        onBack = {

                                            currentScreen =
                                                "dashboard"
                                        }
                                    )


                                } else {


                                    currentScreen =
                                        "dashboard"
                                }
                            }
                        }


                        // =====================================================
                        // QUESTION MANAGEMENT
                        // =====================================================

                        "question_management" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.COACHING_ADMIN ||
                                    user.role ==
                                    UserRole.TEACHER
                                ) {


                                    QuestionManagementScreen(

                                        user =
                                            user,


                                        onBack = {

                                            currentScreen =
                                                "dashboard"
                                        }
                                    )


                                } else {


                                    currentScreen =
                                        "dashboard"
                                }
                            }
                        }


                        // =====================================================
                        // RESULT ANALYTICS
                        // =====================================================

                        "result_analytics" -> {


                            loggedInUser?.let { user ->


                                if (
                                    user.role ==
                                    UserRole.COACHING_ADMIN &&
                                    user.coachingId != null
                                ) {


                                    ResultAnalyticsScreen(

                                        coachingId =
                                            user.coachingId,


                                        onBack = {

                                            currentScreen =
                                                "dashboard"
                                        }
                                    )


                                } else {


                                    currentScreen =
                                        "dashboard"
                                }
                            }
                        }


                        // =====================================================
                        // FALLBACK
                        // =====================================================

                        else -> {


                            loggedInUser =
                                null

                            currentScreen =
                                "login"
                        }
                    }


                    /*
                     * =====================================================
                     * SLH UPDATE DIALOG
                     * =====================================================
                     *
                     * यह dialog current screen के ऊपर दिखाई देगा.
                     */

                    updateInfo?.let { info ->


                        SLHUpdateDialog(

                            updateInfo =
                                info,


                            onUpdate = {

                                AppUpdateManager
                                    .downloadAndInstall(

                                        this@MainActivity,

                                        info
                                    )


                                /*
                                 * Dialog बंद कर देते हैं.
                                 *
                                 * Download notification Android दिखाएगा.
                                 */

                                updateInfo =
                                    null
                            },


                            onLater = {

                                updateInfo =
                                    null
                            }
                        )
                    }
                }
            }
        }
    }
}