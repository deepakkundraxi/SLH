package com.slh.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.slh.app.ui.theme.ChipColors
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    var selectedStudentId by remember {
        mutableStateOf<String?>(null)
    }

    var showForm by remember {
        mutableStateOf(false)
    }

    var showProfile by remember {
        mutableStateOf(false)
    }

    var showAttendance by remember {
        mutableStateOf(false)
    }

    var studentPendingApproval by remember {
        mutableStateOf<Student?>(null)
    }

    var showApprovalDialog by remember {
        mutableStateOf(false)
    }

    var studentToDelete by remember {
        mutableStateOf<Student?>(null)
    }

    BackHandler {

        when {

            showForm -> {
                showForm = false
                selectedStudentId = null
            }

            showAttendance -> {
                showAttendance = false
                showProfile = true
            }

            showProfile -> {
                showProfile = false
                selectedStudentId = null
            }

            else -> {
                onBack()
            }
        }
    }


    // ================================================================
    // ATTENDANCE SCREEN
    // ================================================================

    if (showAttendance) {

        val studentId = selectedStudentId

        if (studentId != null) {

            StudentAttendanceScreen(
                studentId = studentId,
                onBack = {
                    showAttendance = false
                    showProfile = true
                }
            )

        } else {

            showAttendance = false
        }

        return
    }


    // ================================================================
    // PROFILE SCREEN
    // ================================================================

    if (showProfile) {

        val studentId = selectedStudentId

        if (studentId != null) {

            StudentProfileScreen(
                studentId = studentId,

                onBack = {
                    showProfile = false
                    selectedStudentId = null
                },

                onEdit = { id ->
                    selectedStudentId = id
                    showProfile = false
                    showForm = true
                },

                onAttendance = { id ->
                    selectedStudentId = id
                    showProfile = false
                    showAttendance = true
                }
            )

        } else {

            showProfile = false
        }

        return
    }


    // ================================================================
    // STUDENT FORM
    // ================================================================

    if (showForm) {

        StudentFormScreen(
            coachingId = coachingId,
            studentId = selectedStudentId,

            onBack = {
                showForm = false
                selectedStudentId = null
            },

            onSaved = {
                showForm = false
                selectedStudentId = null
            }
        )

        return
    }


    // ================================================================
    // STUDENTS
    // ================================================================

    val allStudents =
        StudentStore.findStudentsByCoaching(
            coachingId
        )

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val students = remember(allStudents, searchQuery, statusFilter) {
        allStudents.filter { s ->
            val matchesQuery = matchesSearch(
                searchQuery,
                s.name, s.studentId, s.mobile, s.username, s.course
            )
            val matchesStatus = statusFilter == null ||
                s.status.name == statusFilter
            matchesQuery && matchesStatus
        }
    }


    Scaffold(

        modifier = Modifier.fillMaxSize(),

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = AppStrings.students,
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
                },

                actions = {

                    IconButton(
                        onClick = {
                            try {
                                val file = DataExportUtils.exportStudentsCsv(
                                    context, coachingId
                                )
                                DataExportUtils.shareFile(context, file)
                            } catch (_: Exception) {
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = AppStrings.exportStudents
                        )
                    }

                    IconButton(
                        onClick = {
                            selectedStudentId = null
                            showForm = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Add,
                            contentDescription =
                                "Add Student"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
        ) {

            // =========================================================
            // HEADER CARD
            // =========================================================

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            ChipColors.blue.container
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
                                .clip(CircleShape)
                                .background(
                                    ChipColors.blue.icon
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.People,

                            contentDescription =
                                null,

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(29.dp)
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
                                "Student Management",

                            style =
                                MaterialTheme.typography.titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "${students.size} student(s)",

                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }


                    Box(

                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(12.dp)
                                )
                                .background(
                                    ChipColors.blue.container
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                )
                    ) {

                        Text(
                            text = "ADMIN",

                            style =
                                MaterialTheme.typography.labelSmall,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                ChipColors.blue.icon
                        )
                    }
                }
            }


            // =========================================================
            // SEARCH + STATUS FILTER
            // =========================================================

            SearchFilterBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = AppStrings.searchPlaceholder,
                statusOptions = listOf(
                    AccountStatus.APPROVED.name,
                    AccountStatus.PENDING.name,
                    AccountStatus.REJECTED.name,
                    AccountStatus.SUSPENDED.name
                ),
                selectedStatus = statusFilter,
                onStatusSelected = { statusFilter = it }
            )


            // =========================================================
            // EMPTY / LIST
            // =========================================================

            if (students.isEmpty()) {

                EmptyStudentsView(
                    onAddStudent = {
                        selectedStudentId = null
                        showForm = true
                    }
                )

            } else {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = students,
                        key = {
                            it.id
                        }
                    ) { student ->

                        StudentManagementCard(
                            student = student,

                            onClick = {
                                selectedStudentId =
                                    student.id
                                showProfile = true
                            },

                            onEdit = {
                                selectedStudentId =
                                    student.id
                                showForm = true
                            },

                            onApprove = {
                                studentPendingApproval = student
                                showApprovalDialog = true
                            },

                            onDelete = {
                                studentToDelete = student
                            }
                        )
                    }
                }
            }
        }
    }


    // ================================================================
    // APPROVAL DIALOG
    // ================================================================

    if (
        showApprovalDialog &&
        studentPendingApproval != null
    ) {

        val pendingStudent =
            studentPendingApproval!!

        AlertDialog(

            onDismissRequest = {

                showApprovalDialog = false
                studentPendingApproval = null
            },

            icon = {

                Icon(
                    imageVector =
                        Icons.Default.CheckCircle,

                    contentDescription =
                        null,

                    tint =
                        ChipColors.green.icon
                )
            },

            title = {

                Text(
                    text =
                        "Approve & Activate?"
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "Student: ${pendingStudent.name}",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Student ID: ${pendingStudent.studentId}"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    ChipColors.amber.container
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(12.dp)
                        ) {

                            Text(
                                text =
                                    "Payment Verification",

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    ChipColors.amber.icon
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Please confirm that the student's payment has been verified."
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "After approval, the student will be able to log in and access the Student Dashboard."
                    )
                }
            },

            confirmButton = {

                Button(

                    onClick = {

                        /*
                         * =====================================================
                         * FIREBASE-COMPATIBLE APPROVAL
                         * =====================================================
                         *
                         * StudentStore.updateStudent() performs:
                         *
                         * 1. Local StudentStore update
                         * 2. Local persistence
                         * 3. Firestore synchronization
                         *
                         * The Firebase Auth account was already created
                         * during registration, so we DO NOT create another
                         * Firebase Auth account here.
                         */
                        val approvedStudent =
                            pendingStudent.copy(
                                status =
                                    AccountStatus.APPROVED
                            )

                        StudentStore.updateStudent(
                            approvedStudent
                        )

                        showApprovalDialog =
                            false

                        studentPendingApproval =
                            null
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                ChipColors.green.icon
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.CheckCircle,

                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text(
                        text =
                            "Approve & Activate"
                    )
                }
            },

            dismissButton = {

                OutlinedButton(

                    onClick = {

                        showApprovalDialog =
                            false

                        studentPendingApproval =
                            null
                    }
                ) {

                    Text(
                        text =
                            "Cancel"
                    )
                }
            }
        )
    }


    // ================================================================
    // DELETE DIALOG
    // ================================================================

    studentToDelete?.let { student ->

        AlertDialog(

            onDismissRequest = {
                studentToDelete = null
            },

            title = {
                Text("Delete Student?")
            },

            text = {

                Text(
                    "Are you sure you want to permanently delete ${student.name}?\n\n" +
                            "Fee plan and payment records will also be removed."
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        StudentStore.deleteStudent(
                            student.id
                        )

                        if (
                            student.username.isNotBlank()
                        ) {

                            UserAccountStore
                                .findByUsername(
                                    student.username
                                )
                                ?.let { account ->

                                    UserAccountStore.delete(
                                        account.id
                                    )
                                }
                        }

                        studentToDelete = null

                        if (
                            selectedStudentId ==
                            student.id
                        ) {

                            selectedStudentId = null
                            showProfile = false
                            showForm = false
                        }
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFD32F2F)
                        )
                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        studentToDelete = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}


// =====================================================================
// STUDENT MANAGEMENT CARD
// =====================================================================

@Composable
private fun StudentManagementCard(
    student: Student,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onDelete: () -> Unit
) {

    val batch =
        if (student.batchId.isBlank()) {
            null
        } else {
            BatchStore.findBatch(
                student.batchId
            )
        }


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(20.dp),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 14.dp,
                            top = 14.dp,
                            end = 14.dp,
                            bottom = 10.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                StudentPhoto(
                    photoUri = student.photoUri,
                    size = 64.dp
                )


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
                            student.name,

                        style =
                            MaterialTheme.typography.titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            "ID: ${student.studentId}",

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )


                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )


                    if (batch != null) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.School,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(16.dp),

                                tint =
                                    ChipColors.purple.icon
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(4.dp)
                            )

                            Text(
                                text =
                                    batch.name,

                                style =
                                    MaterialTheme.typography.bodySmall,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    ChipColors.purple.icon
                            )
                        }

                    } else {

                        Text(
                            text =
                                "Batch: Not assigned",

                            style =
                                MaterialTheme.typography.bodySmall,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            if (student.course.isBlank()) {
                                "Course not assigned"
                            } else {
                                student.course
                            },

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            student.mobile,

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,

                        contentDescription =
                            "Edit Student",

                        tint =
                            ChipColors.blue.icon
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Delete Student",

                        tint =
                            Color(0xFFD32F2F)
                    )
                }
            }


            StudentAccountStatusSection(
                student = student,
                onApprove = onApprove
            )
        }
    }
}


// =====================================================================
// ACCOUNT STATUS
// =====================================================================

@Composable
private fun StudentAccountStatusSection(
    student: Student,
    onApprove: () -> Unit
) {

    val isPending =
        student.status == AccountStatus.PENDING


    val statusText =
        when (student.status) {

            AccountStatus.PENDING ->
                "PENDING"

            AccountStatus.APPROVED ->
                "APPROVED"

            AccountStatus.REJECTED ->
                "REJECTED"

            AccountStatus.SUSPENDED ->
                "SUSPENDED"
        }


    val statusContainer =
        when (student.status) {

            AccountStatus.PENDING ->
                ChipColors.amber.container

            AccountStatus.APPROVED ->
                ChipColors.green.container

            AccountStatus.REJECTED ->
                ChipColors.red.container

            AccountStatus.SUSPENDED ->
                ChipColors.red.container
        }


    val statusColor =
        when (student.status) {

            AccountStatus.PENDING ->
                ChipColors.amber.icon

            AccountStatus.APPROVED ->
                ChipColors.green.icon

            AccountStatus.REJECTED ->
                ChipColors.red.icon

            AccountStatus.SUSPENDED ->
                ChipColors.red.icon
        }


    Column(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(

                shape =
                    RoundedCornerShape(20.dp),

                color =
                    statusContainer,

                contentColor =
                    statusColor
            ) {

                Text(

                    text =
                        statusText,

                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.weight(1f)
            )


            if (isPending) {

                Text(
                    text =
                        "Payment Pending",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ChipColors.amber.icon
                )
            }
        }


        if (isPending) {

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(14.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            ChipColors.amber.container
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {

                    Text(
                        text =
                            "Student account is waiting for payment verification.",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            ChipColors.amber.icon
                    )


                    Spacer(
                        modifier =
                            Modifier.height(9.dp)
                    )


                    Button(

                        onClick =
                            onApprove,

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    ChipColors.green.icon
                            )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.CheckCircle,

                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(7.dp)
                        )

                        Text(
                            text =
                                "Approve & Activate",

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// =====================================================================
// STUDENT PHOTO
// =====================================================================

@Composable
private fun StudentPhoto(
    photoUri: String?,
    size: Dp
) {

    if (!photoUri.isNullOrBlank()) {

        AsyncImage(

            model =
                Uri.parse(photoUri),

            contentDescription =
                "Student Photo",

            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape),

            contentScale =
                ContentScale.Crop
        )

    } else {

        Box(

            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        ChipColors.blue.container
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(

                imageVector =
                    Icons.Default.Person,

                contentDescription =
                    "Student",

                modifier =
                    Modifier.size(
                        size * 0.55f
                    ),

                tint =
                    ChipColors.blue.icon
            )
        }
    }
}


// =====================================================================
// EMPTY STUDENTS
// =====================================================================

@Composable
private fun EmptyStudentsView(
    onAddStudent: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(28.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Box(

            modifier =
                Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        ChipColors.blue.container
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(

                imageVector =
                    Icons.Default.People,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(50.dp),

                tint =
                    ChipColors.blue.icon
            )
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        Text(
            text =
                "No Students Found",

            style =
                MaterialTheme.typography.titleLarge,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(6.dp)
        )


        Text(

            text =
                "Add your first student to start managing student records.",

            style =
                MaterialTheme.typography.bodyMedium,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(18.dp)
        )


        Button(

            onClick =
                onAddStudent,

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        ChipColors.blue.icon
                )
        ) {

            Icon(
                imageVector =
                    Icons.Default.Add,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                "Add Student"
            )
        }
    }
}


// =====================================================================
// STUDENT FORM
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormScreen(
    coachingId: String,
    studentId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {

    val context =
        LocalContext.current


    val existingStudent =
        remember(studentId) {
            studentId?.let {
                StudentStore.findStudent(it)
            }
        }


    val batches =
        BatchStore.findBatchesByCoaching(
            coachingId
        )


    var name by remember {
        mutableStateOf(
            existingStudent?.name ?: ""
        )
    }


    var mobile by remember {
        mutableStateOf(
            existingStudent?.mobile ?: ""
        )
    }


    var address by remember {
        mutableStateOf(
            existingStudent?.address ?: ""
        )
    }


    var username by remember {
        mutableStateOf(
            existingStudent?.username ?: ""
        )
    }


    /*
     * NEW student: plain password typed by Admin until Save.
     * EXISTING student: starts EMPTY. Leave blank to keep the
     * current (unknown/hashed) password; type a new value to
     * RESET — Admin never sees the old password (one-way hash).
     */
    var password by remember {
        mutableStateOf("")
    }

    // After a successful reset, show the plain password once
    // so Admin can tell the student (not stored in plain later).
    var lastResetPasswordShown by remember {
        mutableStateOf<String?>(null)
    }


    var course by remember {
        mutableStateOf(
            existingStudent?.course ?: ""
        )
    }


    var admissionDate by remember {
        mutableStateOf(
            existingStudent?.admissionDate ?: ""
        )
    }


    var showAdmissionDatePicker by remember {
        mutableStateOf(false)
    }


    var photoUri by remember {
        mutableStateOf(
            existingStudent?.photoUri
        )
    }


    var selectedBatchId by remember {
        mutableStateOf(
            existingStudent?.batchId ?: ""
        )
    }


    var errorMessage by remember {
        mutableStateOf("")
    }


    var showBatchMenu by remember {
        mutableStateOf(false)
    }


    /*
     * Prevent double tapping Save while Firebase
     * account provisioning is running.
     */
    var isSaving by remember {
        mutableStateOf(false)
    }


    val selectedBatch =
        batches.firstOrNull {
            it.id == selectedBatchId
        }


    val galleryLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri ->

            if (uri != null) {
                // Compress before storing so local photos stay small
                val compressed = ImageCompressor.compress(
                    context = context,
                    sourceUri = uri,
                    filePrefix = "student"
                )
                photoUri = compressed ?: uri.toString()
            }
        }


    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text =
                            if (studentId == null) {
                                "Add Student"
                            } else {
                                "Edit Student"
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                },


                navigationIcon = {

                    IconButton(
                        onClick = {
                            if (!isSaving) {
                                onBack()
                            }
                        }
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

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            // =========================================================
            // PHOTO
            // =========================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            ChipColors.purple.container
                    )
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    StudentPhoto(
                        photoUri = photoUri,
                        size = 110.dp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )


                    OutlinedButton(

                        onClick = {

                            if (!isSaving) {

                                galleryLauncher.launch(
                                    "image/*"
                                )
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Image,

                            contentDescription =
                                null
                        )


                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )


                        Text(

                            text =
                                if (
                                    photoUri.isNullOrBlank()
                                ) {
                                    "Select Photo"
                                } else {
                                    "Change Photo"
                                }
                        )
                    }
                }
            }


            // =========================================================
            // BATCH
            // =========================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            ChipColors.green.container
                    )
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        ChipColors.green.icon
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.School,

                                contentDescription =
                                    null,

                                tint =
                                    Color.White
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    "Student Batch",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Select the batch for this student",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )


                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Card(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        if (
                                            !isSaving &&
                                            batches.isNotEmpty()
                                        ) {

                                            showBatchMenu =
                                                true
                                        }
                                    },

                            shape =
                                RoundedCornerShape(16.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (
                                            selectedBatch != null
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .surface
                                        } else {
                                            ChipColors
                                                .amber
                                                .container
                                        }
                                ),

                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation =
                                        2.dp
                                )
                        ) {

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(15.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Box(

                                    modifier =
                                        Modifier
                                            .size(42.dp)
                                            .clip(
                                                CircleShape
                                            )
                                            .background(
                                                if (
                                                    selectedBatch != null
                                                ) {
                                                    ChipColors
                                                        .green
                                                        .icon
                                                } else {
                                                    ChipColors
                                                        .amber
                                                        .icon
                                                }
                                            ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            if (
                                                selectedBatch != null
                                            ) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Default.School
                                            },

                                        contentDescription =
                                            null,

                                        tint =
                                            Color.White
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
                                            selectedBatch?.name
                                                ?: "Select Batch",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium,

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            if (
                                                selectedBatch != null
                                            ) {
                                                ChipColors
                                                    .green
                                                    .icon
                                            } else {
                                                ChipColors
                                                    .orange
                                                    .icon
                                            }
                                    )


                                    if (
                                        selectedBatch != null
                                    ) {

                                        val details =
                                            buildString {

                                                if (
                                                    selectedBatch.code
                                                        .isNotBlank()
                                                ) {

                                                    append(
                                                        selectedBatch.code
                                                    )
                                                }

                                                if (
                                                    selectedBatch.course
                                                        .isNotBlank()
                                                ) {

                                                    if (
                                                        isNotEmpty()
                                                    ) {
                                                        append(" • ")
                                                    }

                                                    append(
                                                        selectedBatch.course
                                                    )
                                                }

                                                if (
                                                    selectedBatch.timing
                                                        .isNotBlank()
                                                ) {

                                                    if (
                                                        isNotEmpty()
                                                    ) {
                                                        append(" • ")
                                                    }

                                                    append(
                                                        selectedBatch.timing
                                                    )
                                                }
                                            }


                                        if (
                                            details.isNotBlank()
                                        ) {

                                            Spacer(
                                                modifier =
                                                    Modifier.height(3.dp)
                                            )

                                            Text(
                                                text =
                                                    details,

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall,

                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            )
                                        }

                                    } else {

                                        Text(
                                            text =
                                                if (
                                                    batches.isEmpty()
                                                ) {
                                                    "No batch created for this coaching"
                                                } else {
                                                    "Tap here to select a batch"
                                                },

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall,

                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )
                                    }
                                }


                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowDropDown,

                                    contentDescription =
                                        "Select Batch",

                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }


                        DropdownMenu(

                            expanded =
                                showBatchMenu,

                            onDismissRequest = {
                                showBatchMenu = false
                            },

                            modifier =
                                Modifier.fillMaxWidth(0.92f)
                        ) {

                            if (batches.isEmpty()) {

                                DropdownMenuItem(

                                    text = {

                                        Column {

                                            Text(
                                                text =
                                                    "No Batch Available",

                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            Text(
                                                text =
                                                    "Please create a batch first.",

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall
                                            )
                                        }
                                    },

                                    onClick = {
                                        showBatchMenu = false
                                    }
                                )

                            } else {

                                Text(

                                    text =
                                        "SELECT BATCH",

                                    modifier =
                                        Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 10.dp
                                        ),

                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        ChipColors.green.icon
                                )


                                batches.forEach { batch ->

                                    DropdownMenuItem(

                                        text = {

                                            Column {

                                                Row(
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {

                                                    Box(

                                                        modifier =
                                                            Modifier
                                                                .size(34.dp)
                                                                .clip(
                                                                    CircleShape
                                                                )
                                                                .background(
                                                                    Color(
                                                                        0xFFE8F5E9
                                                                    )
                                                                ),

                                                        contentAlignment =
                                                            Alignment.Center
                                                    ) {

                                                        Icon(
                                                            imageVector =
                                                                Icons.Default.School,

                                                            contentDescription =
                                                                null,

                                                            modifier =
                                                                Modifier.size(
                                                                    19.dp
                                                                ),

                                                            tint =
                                                                Color(
                                                                    0xFF2E7D32
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
                                                                batch.name,

                                                            fontWeight =
                                                                FontWeight.Bold
                                                        )


                                                        val subText =
                                                            buildString {

                                                                if (
                                                                    batch.code
                                                                        .isNotBlank()
                                                                ) {

                                                                    append(
                                                                        batch.code
                                                                    )
                                                                }

                                                                if (
                                                                    batch.course
                                                                        .isNotBlank()
                                                                ) {

                                                                    if (
                                                                        isNotEmpty()
                                                                    ) {
                                                                        append(
                                                                            " • "
                                                                        )
                                                                    }

                                                                    append(
                                                                        batch.course
                                                                    )
                                                                }
                                                            }


                                                        if (
                                                            subText.isNotBlank()
                                                        ) {

                                                            Text(
                                                                text =
                                                                    subText,

                                                                style =
                                                                    MaterialTheme
                                                                        .typography
                                                                        .bodySmall,

                                                                color =
                                                                    Color(
                                                                        0xFF607D8B
                                                                    )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },

                                        onClick = {

                                            selectedBatchId =
                                                batch.id

                                            errorMessage =
                                                ""

                                            showBatchMenu =
                                                false
                                        }
                                    )
                                }
                            }
                        }
                    }


                    if (
                        batches.isNotEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        Text(
                            text =
                                "${batches.size} batch(es) available",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                ChipColors.green.icon,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }


            // =========================================================
            // STUDENT INFORMATION
            // =========================================================

            SectionHeader(
                title = "Student Information",
                color = ChipColors.blue.icon,
                background = ChipColors.blue.container
            )


            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = ""
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Student Name")
                },
                singleLine = true
            )


            OutlinedTextField(
                value = mobile,
                onValueChange = {
                    mobile = it
                    errorMessage = ""
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Mobile Number")
                },
                singleLine = true
            )


            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    errorMessage = ""
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Address")
                },
                minLines = 2,
                maxLines = 4
            )


            OutlinedTextField(
                value = course,
                onValueChange = {
                    course = it
                    errorMessage = ""
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Course")
                },
                placeholder = {
                    Text(
                        "e.g. NEET, JEE, Foundation"
                    )
                },
                singleLine = true
            )


            // =========================================================
            // LOGIN INFORMATION
            // =========================================================

            SectionHeader(
                title = "Login Information",
                color = ChipColors.purple.icon,
                background = ChipColors.purple.container
            )


            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = ""
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Username")
                },
                singleLine = true
            )


            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                    lastResetPasswordShown = null
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (existingStudent != null) {
                            "New password (reset)"
                        } else {
                            "Password"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (existingStudent != null) {
                            "Leave blank to keep current"
                        } else {
                            "Enter password"
                        }
                    )
                },
                supportingText = {
                    if (existingStudent != null) {
                        Text(
                            "Student password is hashed — Admin cannot view it. " +
                                "Type a new password here to reset, then tell the student."
                        )
                    }
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true
            )

            if (lastResetPasswordShown != null) {
                Text(
                    text = "Password reset. Share with student: ${lastResetPasswordShown}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }


            // =========================================================
            // ADMISSION & ACCOUNT
            // =========================================================

            SectionHeader(
                title = "Admission & Account",
                color = ChipColors.orange.icon,
                background = ChipColors.orange.container
            )


            OutlinedButton(

                onClick = {

                    if (!isSaving) {

                        showAdmissionDatePicker =
                            true

                        errorMessage =
                            ""
                    }
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector =
                        Icons.Default.CalendarToday,

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text =
                        if (admissionDate.isBlank()) {
                            "Select Admission Date"
                        } else {
                            "Admission Date: $admissionDate"
                        }
                )
            }


            if (showAdmissionDatePicker) {

                StudentAdmissionDatePickerDialog(

                    initialDate =
                        admissionDate,

                    onDismiss = {
                        showAdmissionDatePicker = false
                    },

                    onDateSelected = { selected ->

                        admissionDate =
                            selected

                        showAdmissionDatePicker =
                            false

                        errorMessage =
                            ""
                    }
                )
            }


            // =========================================================
            // ACCOUNT STATUS CARD
            // =========================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (
                                existingStudent == null ||
                                existingStudent.status ==
                                AccountStatus.PENDING
                            ) {
                                ChipColors.amber.container
                            } else {
                                ChipColors.green.container
                            }
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(

                        imageVector =
                            if (
                                existingStudent?.status ==
                                AccountStatus.APPROVED
                            ) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Warning
                            },

                        contentDescription =
                            null,

                        tint =
                            if (
                                existingStudent?.status ==
                                AccountStatus.APPROVED
                            ) {
                                ChipColors.green.icon
                            } else {
                                ChipColors.amber.icon
                            },

                        modifier =
                            Modifier.size(27.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Account Status",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(
                            text =
                                if (
                                    existingStudent == null
                                ) {
                                    "PENDING"
                                } else {
                                    existingStudent.status
                                        .name
                                        .lowercase()
                                        .replaceFirstChar {
                                            it.uppercase()
                                        }
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                if (
                                    existingStudent?.status ==
                                    AccountStatus.APPROVED
                                ) {
                                    ChipColors.green.icon
                                } else {
                                    ChipColors.amber.icon
                                }
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(
                            text =
                                if (
                                    existingStudent == null
                                ) {

                                    "Payment pending. Admin must verify payment and activate this account."

                                } else if (
                                    existingStudent.status ==
                                    AccountStatus.PENDING
                                ) {

                                    "Payment pending. Use Approve & Activate after payment verification."

                                } else {

                                    "Status is managed from Student Management."
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }


            // =========================================================
            // ERROR
            // =========================================================

            if (errorMessage.isNotBlank()) {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(14.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                ChipColors.red.container
                        )
                ) {

                    Text(

                        text =
                            errorMessage,

                        modifier =
                            Modifier.padding(14.dp),

                        color =
                            ChipColors.red.icon,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            // =========================================================
            // SAVE
            // =========================================================

            Button(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                enabled =
                    !isSaving,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            ChipColors.blue.icon
                    ),

                onClick = {

                    if (isSaving) {
                        return@Button
                    }


                    // -------------------------------------------------
                    // VALIDATION
                    // -------------------------------------------------

                    if (name.trim().isBlank()) {

                        errorMessage =
                            "Please enter student name."

                    } else if (
                        mobile.trim().isBlank()
                    ) {

                        errorMessage =
                            "Please enter mobile number."

                    } else if (
                        course.trim().isBlank()
                    ) {

                        errorMessage =
                            "Please enter course."

                    } else if (
                        username.trim().isBlank()
                    ) {

                        errorMessage =
                            "Please enter username."

                    } else if (
                        StudentStore.students.any { other ->

                            other.username
                                .trim()
                                .equals(
                                    username.trim(),
                                    ignoreCase = true
                                ) &&
                                    other.id !=
                                    existingStudent?.id
                        }
                    ) {

                        errorMessage =
                            "This username is already taken. Please choose a different one."

                    } else if (
                        existingStudent == null &&
                        password.isBlank()
                    ) {

                        errorMessage =
                            "Please enter password."

                    } else if (
                        existingStudent != null &&
                        password.isNotBlank() &&
                        password.length < 6
                    ) {

                        errorMessage =
                            "New password must be at least 6 characters."

                    } else if (
                        admissionDate.trim().isBlank()
                    ) {

                        errorMessage =
                            "Please enter admission date."

                    } else if (
                        selectedBatchId.isBlank()
                    ) {

                        errorMessage =
                            "Please select a batch."

                    } else {

                        // =================================================
                        // EXISTING STUDENT
                        // =================================================

                        if (
                            existingStudent != null
                        ) {

                            // Blank = keep current hashed password.
                            // Non-blank = Admin RESET to this new plain password.
                            val passwordToStore =
                                if (password.isBlank()) {
                                    existingStudent.password
                                } else {
                                    PasswordHasher.hash(password)
                                }

                            StudentStore.updateStudent(

                                existingStudent.copy(

                                    name =
                                        name.trim(),

                                    mobile =
                                        mobile.trim(),

                                    address =
                                        address.trim(),

                                    username =
                                        username.trim(),

                                    password =
                                        passwordToStore,

                                    course =
                                        course.trim(),

                                    admissionDate =
                                        admissionDate.trim(),

                                    /*
                                     * Existing account status is preserved.
                                     */
                                    status =
                                        existingStudent.status,

                                    coachingId =
                                        coachingId,

                                    batchId =
                                        selectedBatchId,

                                    photoUri =
                                        photoUri
                                )
                            )

                            if (password.isNotBlank()) {
                                // Show once so Admin can tell the student.
                                lastResetPasswordShown = password

                                // Best-effort: update Firebase Auth when possible.
                                // Client SDK cannot force-set another user's
                                // password without the old one; local login
                                // works immediately with the new hash.
                                isSaving = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        FirebaseStudentAuthRepository
                                            .adminResetStudentPasswordBestEffort(
                                                context = context,
                                                username = username.trim(),
                                                newPassword = password
                                            )
                                    } catch (_: Exception) {
                                    }
                                    isSaving = false
                                    onSaved()
                                }
                            } else {
                                onSaved()
                            }

                        } else {

                            // =================================================
                            // NEW STUDENT
                            // =================================================

                            /*
                             * IMPORTANT:
                             *
                             * originalPassword = plaintext entered by Admin.
                             *
                             * StudentStore receives only the hash.
                             * Firebase Auth receives the original password.
                             * Firestore does NOT receive the password.
                             */
                            val originalPassword =
                                password


                            val newStudent =
                                Student(

                                    id =
                                        StudentStore.nextId(),

                                    studentId =
                                        StudentStore.nextStudentId(),

                                    name =
                                        name.trim(),

                                    mobile =
                                        mobile.trim(),

                                    address =
                                        address.trim(),

                                    username =
                                        username.trim(),

                                    password =
                                        PasswordHasher.hash(
                                            originalPassword
                                        ),

                                    course =
                                        course.trim(),

                                    admissionDate =
                                        admissionDate.trim(),

                                    /*
                                     * Every newly created student starts
                                     * in PENDING state.
                                     */
                                    status =
                                        AccountStatus.PENDING,

                                    coachingId =
                                        coachingId,

                                    batchId =
                                        selectedBatchId,

                                    photoUri =
                                        photoUri
                                )


                            /*
                             * =================================================
                             * LOCAL + FIRESTORE SAVE
                             * =================================================
                             *
                             * StudentStore.addStudent() performs the
                             * existing local save and starts Firestore
                             * synchronization.
                             */
                            StudentStore.addStudent(
                                newStudent
                            )


                            /*
                             * =================================================
                             * FIREBASE AUTH ACCOUNT
                             * =================================================
                             *
                             * This uses the secondary Firebase application
                             * through FirebaseStudentAuthRepository.
                             *
                             * Therefore the currently logged-in Admin/
                             * Principal Firebase session is not replaced.
                             */
                            isSaving =
                                true


                            CoroutineScope(Dispatchers.Main).launch {

                                try {

                                    FirebaseStudentAuthRepository.createStudentAccount(
                                        context = context,
                                        username = username.trim(),
                                        password = originalPassword
                                    )

                                    isSaving = false
                                    onSaved()

                                } catch (_: Exception) {

                                    isSaving = false

                                    errorMessage =
                                        "Student saved locally, but Firebase account could not be created. " +
                                                "Please check internet/Firebase settings and try again."
                                }
                            }
                        }
                    }
                }
            ) {

                if (isSaving) {

                    Text(
                        text =
                            "Creating Firebase Account...",

                        fontWeight =
                            FontWeight.Bold
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Default.CheckCircle,

                        contentDescription =
                            null
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    Text(

                        text =
                            if (studentId == null) {
                                "Save Student"
                            } else {
                                "Update Student"
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            // =========================================================
            // EXISTING STUDENT ID
            // =========================================================

            if (existingStudent != null) {

                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 4.dp
                        )
                )


                Text(

                    text =
                        "Student ID: ${existingStudent.studentId}",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }
}


// =====================================================================
// SECTION HEADER
// =====================================================================

@Composable
private fun SectionHeader(
    title: String,
    color: Color,
    background: Color
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(14.dp)
                )
                .background(background)
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(

            modifier =
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
        )


        Spacer(
            modifier =
                Modifier.width(9.dp)
        )


        Text(

            text =
                title,

            style =
                MaterialTheme
                    .typography
                    .titleSmall,

            fontWeight =
                FontWeight.Bold,

            color =
                color
        )
    }
}


// =====================================================================
// ADMISSION DATE PICKER
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentAdmissionDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {

    val initialMillis =
        try {

            if (initialDate.isNotBlank()) {

                SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.getDefault()
                )
                    .parse(initialDate)
                    ?.time

            } else {
                null
            }

        } catch (_: Exception) {
            null
        }


    val datePickerState =
        rememberDatePickerState(

            initialSelectedDateMillis =
                initialMillis
                    ?: System.currentTimeMillis()
        )


    DatePickerDialog(

        onDismissRequest =
            onDismiss,

        confirmButton = {

            TextButton(

                onClick = {

                    val millis =
                        datePickerState
                            .selectedDateMillis

                    if (millis != null) {

                        val formatted =
                            SimpleDateFormat(
                                "dd-MM-yyyy",
                                Locale.getDefault()
                            )
                                .format(
                                    Date(millis)
                                )

                        onDateSelected(
                            formatted
                        )

                    } else {

                        onDismiss()
                    }
                }
            ) {

                Text("OK")
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("Cancel")
            }
        }

    ) {

        DatePicker(
            state =
                datePickerState
        )
    }
}