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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LEAVE
}

data class StudentAttendanceRecord(
    val id: String,
    val studentId: String,
    val date: String,
    val status: AttendanceStatus
)

object StudentAttendanceStore {


    private val records =
        mutableStateOf(
            listOf<StudentAttendanceRecord>()
        )

    val attendance: List<StudentAttendanceRecord>
        get() = records.value

    fun getStudentAttendance(
        studentId: String
    ): List<StudentAttendanceRecord> {

        return records.value
            .filter {
                it.studentId == studentId
            }
            .sortedByDescending {
                parseDate(it.date)
            }
    }

    fun setAttendance(
        studentId: String,
        date: String,
        status: AttendanceStatus
    ) {

        val existing =
            records.value.firstOrNull {
                it.studentId == studentId &&
                        it.date == date
            }

        if (existing != null) {

            records.value =
                records.value.map {

                    if (it.id == existing.id) {

                        it.copy(
                            status = status
                        )

                    } else {
                        it
                    }
                }

        } else {

            val newRecord =
                StudentAttendanceRecord(

                    id =
                        "${studentId}_${date}",

                    studentId =
                        studentId,

                    date =
                        date,

                    status =
                        status
                )

            records.value =
                records.value + newRecord
        }
    }

    private fun parseDate(
        date: String
    ): Long {

        return try {

            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            )
                .parse(date)
                ?.time ?: 0L

        } catch (
            e: Exception
        ) {

            0L
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendanceScreen(
    studentId: String,
    onBack: () -> Unit
) {


    val student =
        StudentStore.findStudent(
            studentId
        )

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
                                "Attendance",
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

    var selectedDate by remember {
        mutableStateOf(
            todayDate()
        )
    }

    var selectedStatus by remember {
        mutableStateOf<AttendanceStatus?>(
            null
        )
    }

    val allRecords =
        StudentAttendanceStore
            .getStudentAttendance(
                studentId
            )

    val presentCount =
        allRecords.count {
            it.status ==
                    AttendanceStatus.PRESENT
        }

    val absentCount =
        allRecords.count {
            it.status ==
                    AttendanceStatus.ABSENT
        }

    val leaveCount =
        allRecords.count {
            it.status ==
                    AttendanceStatus.LEAVE
        }

    val totalCount =
        allRecords.size

    val percentage =
        if (totalCount > 0) {

            (presentCount * 100) /
                    totalCount

        } else {
            0
        }

    val selectedRecord =
        allRecords.firstOrNull {
            it.date == selectedDate
        }

    val currentSelectedStatus =
        selectedRecord?.status

    Scaffold(

        modifier =
            Modifier.navigationBarsPadding(),

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
                                student.name,

                            style =
                                MaterialTheme.typography
                                    .labelSmall,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
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

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal = 16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                ),

            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(
                        top = 4.dp,
                        bottom = 24.dp
                    )
        ) {

            item {

                StudentAttendanceHeader(
                    student =
                        student
                )
            }

            item {

                AttendanceSummaryCard(

                    total =
                        totalCount,

                    present =
                        presentCount,

                    absent =
                        absentCount,

                    leave =
                        leaveCount,

                    percentage =
                        percentage
                )
            }

            item {

                AttendanceMarkCard(

                    selectedDate =
                        selectedDate,

                    selectedStatus =
                        currentSelectedStatus,

                    onDateChange = {
                        selectedDate = it
                    },

                    onStatusChange = {
                        selectedStatus = it

                        StudentAttendanceStore
                            .setAttendance(

                                studentId =
                                    student.id,

                                date =
                                    selectedDate,

                                status =
                                    it
                            )
                    }
                )
            }

            item {

                Text(
                    text =
                        "Attendance History",

                    style =
                        MaterialTheme.typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (allRecords.isEmpty()) {

                item {

                    EmptyAttendanceView()
                }

            } else {

                items(

                    items =
                        allRecords,

                    key = {
                        it.id
                    }

                ) { record ->

                    AttendanceHistoryCard(
                        record =
                            record
                    )
                }
            }
        }
    }


}

@Composable
private fun StudentAttendanceHeader(
    student: Student
) {


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme.colorScheme
                        .primaryContainer
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

            if (
                !student.photoUri.isNullOrBlank()
            ) {

                AsyncImage(

                    model =
                        student.photoUri,

                    contentDescription =
                        "Student Photo",

                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape),

                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Box(

                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme
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

                        tint =
                            MaterialTheme.colorScheme
                                .onPrimary,

                        modifier =
                            Modifier.size(30.dp)
                    )
                }
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
                        student.name,

                    style =
                        MaterialTheme.typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        "${student.studentId} • ${
                            if (
                                student.course.isBlank()
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
            }
        }
    }


}

@Composable
private fun AttendanceSummaryCard(
    total: Int,
    present: Int,
    absent: Int,
    leave: Int,
    percentage: Int
) {


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp)
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(

                text =
                    "Attendance Summary",

                style =
                    MaterialTheme.typography
                        .titleMedium,

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
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                AttendanceStat(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Present",

                    value =
                        present,

                    icon =
                        Icons.Default.CheckCircle,

                    iconColor =
                        Color(0xFF15803D)
                )

                AttendanceStat(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Absent",

                    value =
                        absent,

                    icon =
                        Icons.Default.Close,

                    iconColor =
                        Color(0xFFDC2626)
                )

                AttendanceStat(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Leave",

                    value =
                        leave,

                    icon =
                        Icons.Default.EventAvailable,

                    iconColor =
                        Color(0xFFD97706)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Attendance Percentage",

                        style =
                            MaterialTheme.typography
                                .bodyMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text =
                            "$percentage%",

                        style =
                            MaterialTheme.typography
                                .headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Text(
                    text =
                        "$total total days",

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

@Composable
private fun AttendanceStat(
    modifier: Modifier,
    title: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {


    Column(

        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    MaterialTheme.colorScheme
                        .surfaceVariant
                )
                .padding(10.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                null,

            tint =
                iconColor,

            modifier =
                Modifier.size(22.dp)
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(

            text =
                value.toString(),

            style =
                MaterialTheme.typography
                    .titleLarge,

            fontWeight =
                FontWeight.Bold
        )

        Text(

            text =
                title,

            style =
                MaterialTheme.typography
                    .labelSmall
        )
    }


}

@Composable
private fun AttendanceMarkCard(
    selectedDate: String,
    selectedStatus: AttendanceStatus?,
    onDateChange: (String) -> Unit,
    onStatusChange: (AttendanceStatus) -> Unit
) {


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

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
                    "Mark Attendance",

                style =
                    MaterialTheme.typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            MaterialTheme.colorScheme
                                .surface
                        )
                        .clickable {

                            onDateChange(
                                previousDate(
                                    selectedDate
                                )
                            )
                        }
                        .padding(14.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Default.CalendarMonth,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme.colorScheme
                            .primary
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Column {

                    Text(
                        text =
                            "Selected Date",

                        style =
                            MaterialTheme.typography
                                .labelSmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text =
                            selectedDate,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            AttendanceChoice(

                title =
                    "Present",

                selected =
                    selectedStatus ==
                            AttendanceStatus.PRESENT,

                icon =
                    Icons.Default.CheckCircle,

                iconColor =
                    Color(0xFF15803D),

                onClick = {

                    onStatusChange(
                        AttendanceStatus.PRESENT
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            AttendanceChoice(

                title =
                    "Absent",

                selected =
                    selectedStatus ==
                            AttendanceStatus.ABSENT,

                icon =
                    Icons.Default.Close,

                iconColor =
                    Color(0xFFDC2626),

                onClick = {

                    onStatusChange(
                        AttendanceStatus.ABSENT
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            AttendanceChoice(

                title =
                    "Leave",

                selected =
                    selectedStatus ==
                            AttendanceStatus.LEAVE,

                icon =
                    Icons.Default.EventAvailable,

                iconColor =
                    Color(0xFFD97706),

                onClick = {

                    onStatusChange(
                        AttendanceStatus.LEAVE
                    )
                }
            )
        }
    }


}

@Composable
private fun AttendanceChoice(
    title: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {


    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(

                    if (selected) {

                        MaterialTheme.colorScheme
                            .primaryContainer

                    } else {

                        MaterialTheme.colorScheme
                            .surface
                    }
                )
                .clickable(
                    onClick = onClick
                )
                .padding(13.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                null,

            tint =
                iconColor,

            modifier =
                Modifier.size(24.dp)
        )

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Text(

            text =
                title,

            modifier =
                Modifier.weight(1f),

            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )

        if (selected) {

            Text(

                text =
                    "Selected",

                style =
                    MaterialTheme.typography
                        .labelSmall,

                color =
                    MaterialTheme.colorScheme
                        .primary,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }


}

@Composable
private fun AttendanceHistoryCard(
    record: StudentAttendanceRecord
) {


    val (
        title,
        color,
        icon
    ) = when (record.status) {

        AttendanceStatus.PRESENT ->

            Triple(
                "Present",
                Color(0xFF15803D),
                Icons.Default.CheckCircle
            )

        AttendanceStatus.ABSENT ->

            Triple(
                "Absent",
                Color(0xFFDC2626),
                Icons.Default.Close
            )

        AttendanceStatus.LEAVE ->

            Triple(
                "Leave",
                Color(0xFFD97706),
                Icons.Default.EventAvailable
            )
    }

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp)
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            color.copy(
                                alpha = 0.12f
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
                        color,

                    modifier =
                        Modifier.size(23.dp)
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

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        color
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(

                    text =
                        record.date,

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

@Composable
private fun EmptyAttendanceView() {


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp)
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
                    Icons.Default.Schedule,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(42.dp),

                tint =
                    MaterialTheme.colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(

                text =
                    "No attendance records",

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(

                text =
                    "Attendance records will appear here.",

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

private fun todayDate(): String {


    return SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.getDefault()
    ).format(
        Calendar.getInstance().time
    )


}

private fun previousDate(
    currentDate: String
): String {


    val calendar =
        Calendar.getInstance()

    try {

        val parsedDate =
            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).parse(
                currentDate
            )

        if (parsedDate != null) {
            calendar.time = parsedDate
        }

    } catch (
        e: Exception
    ) {
        // Keep today's date if parsing fails.
    }

    calendar.add(
        Calendar.DAY_OF_MONTH,
        -1
    )

    return SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.getDefault()
    ).format(
        calendar.time
    )


}
