package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    studentId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAttendance: (String) -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {

    val student =
        StudentStore.findStudent(
            studentId
        )

    // ---- NEW: theme mode for the settings toggle below ----
    val (themeMode, setThemeMode) = rememberThemeMode()

    BackHandler {
        onBack()
    }

    if (student == null) {

        Scaffold(

            modifier =
                Modifier.navigationBarsPadding(),

            topBar = {

                TopAppBar(

                    title = {

                        Text(
                            text =
                                "Student Profile",

                            fontWeight =
                                FontWeight.Bold
                        )
                    },

                    navigationIcon = {

                        IconButton(
                            onClick =
                                onBack
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

        ) { paddingValues ->

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "Student not found",

                    color =
                        MaterialTheme.colorScheme
                            .error
                )
            }
        }

        return
    }

    val coaching =
        CoachingProfileStore.get(
            student.coachingId
        )

    val isActive =
        student.status ==
                AccountStatus.APPROVED

    /*
     * REAL ATTENDANCE DATA
     */

    val presentDays =
        StudentGpsAttendanceStore
            .getStudentPresentDays(
                studentId = student.id
            )

    val absentDays =
        AttendanceStatusStore
            .getStudentAbsentDays(
                studentId = student.id
            )

    val workingDays =
        AttendanceStatusStore
            .getStudentWorkingDays(
                studentId = student.id
            )

    val attendancePercentage =
        AttendanceStatusStore
            .getStudentAttendancePercentage(
                studentId = student.id
            )

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        bottomBar = {

            SLHBottomNavBar(
                items = BottomNavItems.student,
                currentRoute = "profile",
                onNavigate = { route ->

                    if (route == "profile") {
                        // already here
                    } else if (route == "dashboard") {
                        onBack()
                    } else {
                        onNavigateTab(route)
                    }
                }
            )
        },

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text =
                            "Student Profile",

                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },

                actions = {

                    IconButton(

                        onClick = {

                            onEdit(
                                student.id
                            )
                        }

                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Edit,

                            contentDescription =
                                "Edit Student"
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                MaterialTheme.colorScheme
                                    .surface
                        )
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    )
        ) {

            /*
             * PROFILE HEADER
             */

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(22.dp),

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            MaterialTheme.colorScheme
                                .primaryContainer
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

                    if (
                        !student.photoUri
                            .isNullOrBlank()
                    ) {

                        AsyncImage(

                            model =
                                student.photoUri,

                            contentDescription =
                                "Student Photo",

                            modifier =
                                Modifier
                                    .size(88.dp)
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
                                    .size(88.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Person,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        48.dp
                                    ),

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(

                        text =
                            student.name,

                        style =
                            MaterialTheme.typography
                                .headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(

                        text =
                            "${student.studentId} • ${
                                if (
                                    student.course
                                        .isBlank()
                                ) {
                                    "Course not assigned"
                                } else {
                                    student.course
                                }
                            }",

                        style =
                            MaterialTheme.typography
                                .bodyMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Box(

                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        50.dp
                                    )
                                )
                                .background(

                                    if (isActive) {

                                        Color(
                                            0xFFDFF7E5
                                        )

                                    } else {

                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer
                                    }
                                )
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 6.dp
                                )
                    ) {

                        Text(

                            text =
                                if (isActive)
                                    "ACTIVE"
                                else
                                    "INACTIVE",

                            fontWeight =
                                FontWeight.Bold,

                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,

                            color =
                                if (isActive) {

                                    Color(
                                        0xFF15803D
                                    )

                                } else {

                                    MaterialTheme
                                        .colorScheme
                                        .error
                                }
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            /*
             * BASIC INFORMATION
             */

            ProfileSectionTitle(
                title =
                    "Basic Information"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column {

                    ProfileInfoRow(
                        icon =
                            Icons.Default.Badge,

                        title =
                            "Student ID",

                        value =
                            student.studentId
                    )

                    ProfileInfoRow(
                        icon =
                            Icons.Default.Phone,

                        title =
                            "Mobile Number",

                        value =
                            student.mobile
                    )

                    ProfileInfoRow(
                        icon =
                            Icons.Default.AccountCircle,

                        title =
                            "Username",

                        value =
                            student.username
                    )

                    ProfileInfoRow(
                        icon =
                            Icons.Default.CalendarMonth,

                        title =
                            "Admission Date",

                        value =
                            student.admissionDate
                    )

                    ProfileInfoRow(
                        icon =
                            Icons.Default.AccountCircle,

                        title =
                            "Address",

                        value =
                            student.address
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            /*
             * ACADEMIC INFORMATION
             */

            ProfileSectionTitle(
                title =
                    "Academic Information"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column {

                    ProfileInfoRow(
                        icon =
                            Icons.Default.School,

                        title =
                            "Course",

                        value =
                            student.course
                    )

                    ProfileInfoRow(
                        icon =
                            Icons.Default.Badge,

                        title =
                            "Account Status",

                        value =
                            if (isActive)
                                "Active"
                            else
                                "Inactive"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            /*
             * ATTENDANCE SUMMARY
             */

            ProfileSectionTitle(
                title =
                    "Attendance Summary"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAttendance(
                                student.id
                            )
                        },

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Today,

                                contentDescription =
                                    "Attendance",

                                tint =
                                    Color(0xFF2E7D32),

                                modifier =
                                    Modifier.size(27.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(

                                text =
                                    "Attendance",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(

                                text =
                                    "Overall attendance",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,

                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(

                            text =
                                "$attendancePercentage%",

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineMedium,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Color(0xFF2E7D32)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        ProfileAttendanceStat(
                            modifier =
                                Modifier.weight(1f),

                            title =
                                "Present",

                            value =
                                presentDays.toString(),

                            valueColor =
                                Color(0xFF2E7D32)
                        )

                        ProfileAttendanceStat(
                            modifier =
                                Modifier.weight(1f),

                            title =
                                "Absent",

                            value =
                                absentDays.toString(),

                            valueColor =
                                Color(0xFFC62828)
                        )

                        ProfileAttendanceStat(
                            modifier =
                                Modifier.weight(1f),

                            title =
                                "Working Days",

                            value =
                                workingDays.toString(),

                            valueColor =
                                Color(0xFF1565C0)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(

                        text =
                            "Tap to view detailed attendance",

                        style =
                            MaterialTheme.typography
                                .labelSmall,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            /*
             * COACHING INFORMATION
             */

            ProfileSectionTitle(
                title =
                    "Coaching Information"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            MaterialTheme.colorScheme
                                .secondaryContainer
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(

                        text =
                            coaching.name,

                        style =
                            MaterialTheme.typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        coaching.tagline
                            .isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(

                            text =
                                coaching.tagline,

                            style =
                                MaterialTheme.typography
                                    .bodyMedium,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        text =
                            "Coaching ID: ${student.coachingId}",

                        style =
                            MaterialTheme.typography
                                .bodySmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            /*
             * CHANGE PASSWORD (student self-service)
             * Updates local Student record (Admin sees new password)
             * and Firebase Auth when online.
             */
            ProfileSectionTitle(
                title = "Security"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            StudentChangePasswordSection(
                student = student
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            /*
             * SETTINGS — appearance
             */

            ProfileSectionTitle(
                title =
                    "Appearance"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            ThemeModeSelector(
                current = themeMode,
                onSelect = setThemeMode
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            AccountSettingsSection(
                student = student
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun StudentChangePasswordSection(
    student: Student
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var successText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Change your login password. Admin student list will stay in sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (successText.isNotBlank()) {
                Text(
                    text = successText,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                onClick = {
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    errorText = ""
                    successText = ""
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Password")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showDialog = false
            },
            title = { Text("Change Password") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )
                    if (errorText.isNotBlank()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        when {
                            currentPassword.isBlank() -> {
                                errorText = "Enter current password."
                            }
                            newPassword.length < 6 -> {
                                errorText = "New password must be at least 6 characters."
                            }
                            newPassword != confirmPassword -> {
                                errorText = "New password and confirm do not match."
                            }
                            !PasswordHasher.verify(
                                currentPassword,
                                student.password
                            ) -> {
                                errorText = "Current password is incorrect."
                            }
                            else -> {
                                isSaving = true
                                errorText = ""
                                scope.launch {
                                    val cloudError =
                                        FirebaseStudentAuthRepository
                                            .changeStudentPassword(
                                                context = context,
                                                username = student.username,
                                                currentPassword = currentPassword,
                                                newPassword = newPassword
                                            )

                                    // Always update local store so Admin
                                    // student management sees the new password
                                    // (hashed). Cloud failure still allows
                                    // offline-first update; next online login
                                    // may need re-sync.
                                    val updated = student.copy(
                                        password = PasswordHasher.hash(newPassword)
                                    )
                                    StudentStore.updateStudent(updated)

                                    isSaving = false
                                    if (cloudError != null) {
                                        // Local updated; warn about cloud
                                        successText =
                                            "Password updated on this device. Cloud: $cloudError"
                                        showDialog = false
                                    } else {
                                        successText =
                                            "Password changed successfully."
                                        showDialog = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isSaving) "Saving…" else "Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isSaving,
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun ProfileAttendanceStat(
    modifier: Modifier,
    title: String,
    value: String,
    valueColor: Color
) {

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(14.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.72f
                    )
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 10.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(

                text =
                    value,

                style =
                    MaterialTheme.typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold,

                color =
                    valueColor
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(

                text =
                    title,

                style =
                    MaterialTheme.typography
                        .labelSmall,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ProfileSectionTitle(
    title: String
) {

    Text(

        text =
            title,

        style =
            MaterialTheme.typography
                .titleMedium,

        fontWeight =
            FontWeight.Bold
    )
}


@Composable
private fun ProfileInfoRow(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    value: String
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 13.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(

            modifier =
                Modifier
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

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(21.dp),

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(

                text =
                    title,

                style =
                    MaterialTheme.typography
                        .labelMedium,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(

                text =
                    value.ifBlank {
                        "Not available"
                    },

                style =
                    MaterialTheme.typography
                        .bodyLarge,

                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}


@Composable
private fun ProfileModuleCard(
    title: String,

    subtitle: String,

    clickable: Boolean = false,

    onClick: (() -> Unit)? = null
) {

    val modifier =

        if (
            clickable &&
            onClick != null
        ) {

            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 8.dp
                )
                .clickable(
                    onClick = onClick
                )

        } else {

            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 8.dp
                )
        }

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(16.dp),

        colors =

            if (clickable) {

                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                )

            } else {

                CardDefaults.cardColors()
            }
    ) {

        Row(

            modifier =
                Modifier.padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(
                            CircleShape
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                        )
            )

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        title,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(

                    text =
                        subtitle,

                    style =
                        MaterialTheme.typography
                            .bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}