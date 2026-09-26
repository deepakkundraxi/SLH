package com.slh.app

import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedDate by remember {
        mutableStateOf(
            AttendanceStatusStore.todayDate()
        )
    }

    var showDateDialog by remember {
        mutableStateOf(false)
    }

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    /*
     * Force Compose to observe changes after
     * marking Absent.
     */
    @Suppress("UNUSED_VARIABLE")
    val currentRefreshKey = refreshKey

    val students =
        StudentStore
            .findStudentsByCoaching(
                coachingId
            )
            .filter { student ->

                if (
                    searchText.isBlank()
                ) {
                    true
                } else {

                    student.name
                        .contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                            student.studentId
                                .contains(
                                    searchText,
                                    ignoreCase = true
                                ) ||
                            student.mobile
                                .contains(
                                    searchText,
                                    ignoreCase = true
                                )
                }
            }
            .sortedBy {
                it.name.lowercase()
            }

    val todayPresentIds =
        StudentGpsAttendanceStore
            .records
            .filter {
                it.coachingId == coachingId &&
                        it.date == selectedDate
            }
            .map {
                it.studentId
            }
            .toSet()

    val absentIds =
        AttendanceStatusStore
            .getAbsentStudentIds(
                coachingId = coachingId,
                date = selectedDate
            )

    val presentCount =
        students.count {
            todayPresentIds.contains(
                it.id
            )
        }

    val absentCount =
        students.count {
            absentIds.contains(
                it.id
            )
        }

    val notMarkedCount =
        (
                students.size -
                        presentCount -
                        absentCount
                ).coerceAtLeast(0)

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "Attendance",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                selectedDate,
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

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
        ) {

            /*
             * DATE + SEARCH
             */

            OutlinedButton(
                onClick = {
                    showDateDialog = true
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector =
                        Icons.Default.CalendarMonth,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text =
                        "Date: $selectedDate"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(

                value =
                    searchText,

                onValueChange = {
                    searchText = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true,

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,
                        contentDescription =
                            null
                    )
                },

                placeholder = {
                    Text(
                        "Search student..."
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            /*
             * SUMMARY
             */

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                AttendanceSummaryCard(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Students",

                    value =
                        students.size.toString(),

                    background =
                        Color(0xFFE3F2FD),

                    iconColor =
                        Color(0xFF1976D2)
                )

                AttendanceSummaryCard(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Present",

                    value =
                        presentCount.toString(),

                    background =
                        Color(0xFFE8F5E9),

                    iconColor =
                        Color(0xFF2E7D32)
                )

                AttendanceSummaryCard(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Absent",

                    value =
                        absentCount.toString(),

                    background =
                        Color(0xFFFFEBEE),

                    iconColor =
                        Color(0xFFC62828)
                )

                AttendanceSummaryCard(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Not Marked",

                    value =
                        notMarkedCount.toString(),

                    background =
                        Color(0xFFFFF3E0),

                    iconColor =
                        Color(0xFFEF6C00)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            if (students.isEmpty()) {

                EmptyAttendanceState()

            } else {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = students,
                        key = {
                            it.id
                        }
                    ) { student ->

                        AdminStudentAttendanceCard(

                            student =
                                student,

                            selectedDate =
                                selectedDate,

                            isPresent =
                                todayPresentIds
                                    .contains(
                                        student.id
                                    ),

                            isAbsent =
                                absentIds
                                    .contains(
                                        student.id
                                    ),

                            onMarkAbsent = {

                                selectedStudent =
                                    student
                            }
                        )
                    }

                    item {
                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }

    /*
     * DATE DIALOG
     */

    if (showDateDialog) {

        AttendanceDateDialog(

            currentDate =
                selectedDate,

            onDismiss = {
                showDateDialog = false
            },

            onDateSelected = { date ->

                selectedDate = date
                showDateDialog = false
            }
        )
    }

    /*
     * ABSENT CONFIRMATION
     */

    selectedStudent?.let { student ->

        AlertDialog(

            onDismissRequest = {
                selectedStudent = null
            },

            icon = {

                Icon(
                    imageVector =
                        Icons.Default.Close,
                    contentDescription =
                        null,
                    tint =
                        Color(0xFFC62828)
                )
            },

            title = {

                Text(
                    text =
                        "Mark Absent?"
                )
            },

            text = {

                Text(
                    text =
                        "${student.name} will be marked absent for $selectedDate."
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        AttendanceStatusStore
                            .markAbsent(
                                studentId =
                                    student.id,

                                coachingId =
                                    coachingId,

                                date =
                                    selectedDate
                            )

                        selectedStudent = null

                        refreshKey++
                    }
                ) {

                    Text(
                        text =
                            "Mark Absent"
                    )
                }
            },

            dismissButton = {

                OutlinedButton(
                    onClick = {
                        selectedStudent = null
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
}


/* ============================================================
   STUDENT ATTENDANCE CARD
   ============================================================ */

@Composable
private fun AdminStudentAttendanceCard(
    student: Student,
    selectedDate: String,
    isPresent: Boolean,
    isAbsent: Boolean,
    onMarkAbsent: () -> Unit
) {

    val gpsRecord =
        StudentGpsAttendanceStore
            .records
            .firstOrNull {

                it.studentId == student.id &&
                        it.date == selectedDate
            }

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
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

                if (
                    student.photoUri != null
                ) {

                    AsyncImage(

                        model =
                            student.photoUri,

                        contentDescription =
                            student.name,

                        modifier =
                            Modifier
                                .width(50.dp)
                                .height(50.dp),

                        contentScale =
                            androidx.compose.ui.layout
                                .ContentScale.Crop
                    )

                } else {

                    Surface(

                        modifier =
                            Modifier
                                .width(50.dp)
                                .height(50.dp),

                        shape =
                            androidx.compose.foundation
                                .shape.CircleShape,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Person,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }
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
                            student.name,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "${student.studentId} • ${student.course.ifBlank { "Course not assigned" }}",

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

                when {

                    isPresent -> {

                        AssistChip(

                            onClick = {},

                            label = {
                                Text(
                                    "Present"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    imageVector =
                                        Icons.Default.CheckCircle,

                                    contentDescription =
                                        null
                                )
                            }
                        )
                    }

                    isAbsent -> {

                        AssistChip(

                            onClick = {},

                            label = {
                                Text(
                                    "Absent"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    imageVector =
                                        Icons.Default.Close,

                                    contentDescription =
                                        null
                                )
                            }
                        )
                    }

                    else -> {

                        AssistChip(

                            onClick =
                                onMarkAbsent,

                            label = {
                                Text(
                                    "Mark Absent"
                                )
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            when {

                isPresent &&
                        gpsRecord != null -> {

                    Column {

                        AttendanceInfoRow(
                            icon =
                                Icons.Default.Today,

                            text =
                                "Time: ${gpsRecord.time}"
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        AttendanceInfoRow(
                            icon =
                                Icons.Default.LocationOn,

                            text =
                                "Distance: ${formatDistance(gpsRecord.distanceMeters)}"
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        AttendanceInfoRow(
                            icon =
                                Icons.Default.LocationOn,

                            text =
                                "GPS: ${
                                    formatCoordinate(
                                        gpsRecord.latitude
                                    )
                                }, ${
                                    formatCoordinate(
                                        gpsRecord.longitude
                                    )
                                }"
                        )
                    }
                }

                isAbsent -> {

                    Text(
                        text =
                            "Student was marked absent by Admin.",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            Color(0xFFC62828)
                    )
                }

                else -> {

                    Text(
                        text =
                            "Attendance not marked yet.",

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
    }
}


/* ============================================================
   SUMMARY CARD
   ============================================================ */

@Composable
private fun AttendanceSummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    background: Color,
    iconColor: Color
) {

    Card(

        modifier =
            modifier,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),

        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {

            Text(
                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

                color =
                    iconColor
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold,

                color =
                    iconColor
            )
        }
    }
}


/* ============================================================
   DATE DIALOG
   ============================================================ */

@Composable
private fun AttendanceDateDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {

    val calendar =
        Calendar.getInstance()

    val formatter =
        SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        )

    val dates =
        (0..30).map { daysAgo ->

            val dateCalendar =
                calendar.clone() as Calendar

            dateCalendar.add(
                Calendar.DAY_OF_YEAR,
                -daysAgo
            )

            formatter.format(
                dateCalendar.time
            )
        }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        icon = {

            Icon(
                imageVector =
                    Icons.Default.CalendarMonth,
                contentDescription =
                    null
            )
        },

        title = {

            Text(
                text =
                    "Select Attendance Date"
            )
        },

        text = {

            Column {

                dates.forEach { date ->

                    OutlinedButton(

                        onClick = {
                            onDateSelected(
                                date
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Icon(
                            imageVector =
                                if (
                                    date ==
                                    currentDate
                                ) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.CalendarMonth
                                },

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
                                    date ==
                                    currentDate
                                ) {
                                    "$date  •  Selected"
                                } else {
                                    date
                                }
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )
                }
            }
        },

        confirmButton = {

            OutlinedButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    text =
                        "Close"
                )
            }
        }
    )
}


/* ============================================================
   EMPTY STATE
   ============================================================ */

@Composable
private fun EmptyAttendanceState() {

    Column(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(40.dp)
        )

        Icon(
            imageVector =
                Icons.Default.Person,

            contentDescription =
                null,

            modifier =
                Modifier.size(48.dp),

            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "No students found",

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "Try another search.",
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


/* ============================================================
   HELPERS
   ============================================================ */

@Composable
private fun AttendanceInfoRow(
    icon:
    androidx.compose.ui.graphics.vector
    .ImageVector,
    text: String
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                icon,

            contentDescription =
                null,

            modifier =
                Modifier.size(16.dp),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )

        Spacer(
            modifier =
                Modifier.width(6.dp)
        )

        Text(
            text =
                text,

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

private fun formatDistance(
    distance: Float
): String {

    return if (
        distance < 1000f
    ) {

        "${distance.toInt()} m"

    } else {

        String.format(
            Locale.getDefault(),
            "%.2f km",
            distance / 1000f
        )
    }
}

private fun formatCoordinate(
    value: Double
): String {

    return String.format(
        Locale.getDefault(),
        "%.6f",
        value
    )
}