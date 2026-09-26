package com.slh.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.util.Locale

private enum class GpsAttendanceState {
    IDLE,
    CHECKING,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGpsAttendanceScreen(
    studentId: String,
    onBack: () -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val student =
        StudentStore.findStudent(studentId)

    var showAttendanceHistory by remember {
        mutableStateOf(false)
    }

    /*
     * Student not found
     */

    if (student == null) {

        BackHandler {
            onBack()
        }

        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "GPS Attendance",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Student not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        return
    }

    /*
     * Attendance History
     */

    if (showAttendanceHistory) {

        StudentGpsAttendanceHistoryScreen(
            studentId = student.id,
            onBack = {
                showAttendanceHistory = false
            }
        )

        return
    }

    val coaching =
        CoachingProfileStore.get(student.coachingId)

    /*
     * Android Back
     */

    BackHandler {
        onBack()
    }

    var attendanceState by remember {
        mutableStateOf(
            GpsAttendanceState.IDLE
        )
    }

    var message by remember {
        mutableStateOf(
            "Tap the button below to verify your location."
        )
    }

    var currentDistance by remember {
        mutableStateOf<Float?>(null)
    }

    /*
     * Permission launcher
     */

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseGranted =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            if (fineGranted || coarseGranted) {

                checkGpsAttendance(
                    context = context,
                    coaching = coaching,
                    studentId = student.id,
                    onStateChange = {
                        attendanceState = it
                    },
                    onMessageChange = {
                        message = it
                    },
                    onDistanceChange = {
                        currentDistance = it
                    }
                )

            } else {

                attendanceState =
                    GpsAttendanceState.ERROR

                message =
                    "Location permission is required for attendance."
            }
        }

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        bottomBar = {

            SLHBottomNavBar(
                items = BottomNavItems.student,
                currentRoute = "attendance",
                onNavigate = { route ->

                    if (route == "attendance") {
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

                    Column {

                        Text(
                            text = "GPS Attendance",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = student.name,
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme
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
                    .padding(paddingValues)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            /*
             * STUDENT CARD
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

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
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
                                    .size(64.dp)
                                    .clip(CircleShape),

                            contentScale =
                                ContentScale.Crop
                        )

                    } else {

                        Box(

                            modifier =
                                Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
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

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary,

                                modifier =
                                    Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.width(14.dp)
                    )

                    Column {

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
                                student.studentId,

                            style =
                                MaterialTheme.typography
                                    .bodyMedium
                        )

                        Text(

                            text =
                                if (
                                    student.course.isBlank()
                                ) {
                                    "Course not assigned"
                                } else {
                                    student.course
                                },

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

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            /*
             * GPS ICON
             */

            Box(

                modifier =
                    Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(

                            when (attendanceState) {

                                GpsAttendanceState.SUCCESS ->
                                    Color(0xFFDFF7E5)

                                GpsAttendanceState.ERROR ->
                                    MaterialTheme
                                        .colorScheme
                                        .errorContainer

                                else ->
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                            }
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(

                    imageVector =

                        when (attendanceState) {

                            GpsAttendanceState.SUCCESS ->
                                Icons.Default.CheckCircle

                            GpsAttendanceState.ERROR ->
                                Icons.Default.LocationDisabled

                            else ->
                                Icons.Default.GpsFixed
                        },

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(62.dp),

                    tint =

                        when (attendanceState) {

                            GpsAttendanceState.SUCCESS ->
                                Color(0xFF15803D)

                            GpsAttendanceState.ERROR ->
                                MaterialTheme
                                    .colorScheme
                                    .error

                            else ->
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(

                text =
                    when (attendanceState) {

                        GpsAttendanceState.SUCCESS ->
                            "Attendance Marked"

                        GpsAttendanceState.CHECKING ->
                            "Checking Location..."

                        GpsAttendanceState.ERROR ->
                            "Attendance Not Marked"

                        GpsAttendanceState.IDLE ->
                            "Mark Today's Attendance"
                    },

                style =
                    MaterialTheme.typography
                        .headlineSmall,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(

                text =
                    message,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),

                style =
                    MaterialTheme.typography
                        .bodyMedium,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            /*
             * DISTANCE
             */

            currentDistance?.let { distance ->

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.LocationOn,

                            contentDescription =
                                null,

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
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
                                    "Distance from coaching",

                                style =
                                    MaterialTheme.typography
                                        .labelMedium,

                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )

                            Text(

                                text =
                                    formatDistance(distance),

                                style =
                                    MaterialTheme.typography
                                        .titleMedium,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )
            }

            /*
             * TODAY STATUS
             */

            val alreadyMarked =
                StudentGpsAttendanceStore
                    .hasAttendanceToday(student.id)

            if (alreadyMarked) {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFDFF7E5)
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

                        Icon(

                            imageVector =
                                Icons.Default.CheckCircle,

                            contentDescription =
                                null,

                            tint =
                                Color(0xFF15803D)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Column {

                            Text(

                                text =
                                    "Today's attendance is already marked.",

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(

                                text =
                                    "You cannot mark attendance again today.",

                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )
                        }
                    }
                }

            } else {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Button(

                    onClick = {

                        val fineGranted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission
                                    .ACCESS_FINE_LOCATION
                            ) ==
                                    PackageManager
                                        .PERMISSION_GRANTED

                        val coarseGranted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission
                                    .ACCESS_COARSE_LOCATION
                            ) ==
                                    PackageManager
                                        .PERMISSION_GRANTED

                        if (
                            fineGranted ||
                            coarseGranted
                        ) {

                            checkGpsAttendance(

                                context =
                                    context,

                                coaching =
                                    coaching,

                                studentId =
                                    student.id,

                                onStateChange = {
                                    attendanceState = it
                                },

                                onMessageChange = {
                                    message = it
                                },

                                onDistanceChange = {
                                    currentDistance = it
                                }
                            )

                        } else {

                            permissionLauncher.launch(

                                arrayOf(

                                    Manifest.permission
                                        .ACCESS_FINE_LOCATION,

                                    Manifest.permission
                                        .ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },

                    enabled =
                        attendanceState !=
                                GpsAttendanceState.CHECKING,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.GpsFixed,

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
                                attendanceState ==
                                GpsAttendanceState.CHECKING
                            ) {

                                "Checking GPS..."

                            } else {

                                "Mark Attendance"
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            /*
             * ATTENDANCE HISTORY BUTTON
             */

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedButton(

                onClick = {
                    showAttendanceHistory = true
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                shape =
                    RoundedCornerShape(16.dp)
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
                    text = "Attendance History",
                    fontWeight = FontWeight.SemiBold
                )
            }

            /*
             * TRY AGAIN
             */

            if (
                attendanceState ==
                GpsAttendanceState.ERROR
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedButton(

                    onClick = {

                        attendanceState =
                            GpsAttendanceState.IDLE

                        message =
                            "Tap the button below to try again."

                        currentDistance =
                            null
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Refresh,

                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Try Again"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(

                text =
                    "Attendance is verified using your current GPS location.",

                style =
                    MaterialTheme.typography
                        .labelSmall,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

private fun checkGpsAttendance(
    context: Context,
    coaching: CoachingProfile,
    studentId: String,
    onStateChange:
        (GpsAttendanceState) -> Unit,
    onMessageChange:
        (String) -> Unit,
    onDistanceChange:
        (Float) -> Unit
) {

    /*
     * Coaching GPS location must be configured.
     */

    val coachingLatitude =
        coaching.latitude

    val coachingLongitude =
        coaching.longitude

    if (
        coachingLatitude == null ||
        coachingLongitude == null
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Coaching GPS location is not configured yet."
        )

        return
    }

    /*
     * Permission check.
     */

    val fineGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    if (
        !fineGranted &&
        !coarseGranted
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Please allow location permission."
        )

        return
    }

    val locationManager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

    /*
     * GPS must be enabled.
     */

    val gpsEnabled =
        try {

            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

        } catch (
            e: Exception
        ) {

            false
        }

    if (!gpsEnabled) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Please turn ON GPS/location and try again."
        )

        return
    }

    onStateChange(
        GpsAttendanceState.CHECKING
    )

    onMessageChange(
        "Getting your current GPS location..."
    )

    /*
     * Last known location.
     */

    try {

        val lastLocation: Location? =

            if (fineGranted) {

                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission
                            .ACCESS_FINE_LOCATION
                    ) != PackageManager
                        .PERMISSION_GRANTED
                ) {
                    null
                } else {

                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )
                }

            } else {

                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission
                            .ACCESS_COARSE_LOCATION
                    ) != PackageManager
                        .PERMISSION_GRANTED
                ) {
                    null
                } else {

                    locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                }
            }

        if (
            lastLocation != null &&
            isLocationRecent(lastLocation)
        ) {

            processAttendanceLocation(

                location =
                    lastLocation,

                coaching =
                    coaching,

                studentId =
                    studentId,

                onStateChange =
                    onStateChange,

                onMessageChange =
                    onMessageChange,

                onDistanceChange =
                    onDistanceChange
            )

            return
        }

    } catch (
        e: SecurityException
    ) {

        // Continue with fresh GPS location.

    } catch (
        e: Exception
    ) {

        // Continue with fresh GPS location.
    }

    /*
     * Fresh GPS location.
     */

    val handler =
        Handler(
            Looper.getMainLooper()
        )

    var completed =
        false

    var locationListener:
            android.location.LocationListener? =
        null

    val timeoutRunnable =
        Runnable {

            if (!completed) {

                completed = true

                try {

                    locationListener?.let {

                        locationManager.removeUpdates(
                            it
                        )
                    }

                } catch (
                    e: Exception
                ) {
                    // Ignore.
                }

                onStateChange(
                    GpsAttendanceState.ERROR
                )

                onMessageChange(
                    "Could not get a fresh GPS location. Please move to an open area and try again."
                )
            }
        }

    locationListener =
        object :
            android.location.LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                if (completed) {
                    return
                }

                completed = true

                handler.removeCallbacks(
                    timeoutRunnable
                )

                try {

                    locationManager.removeUpdates(
                        this
                    )

                } catch (
                    e: Exception
                ) {
                    // Ignore.
                }

                processAttendanceLocation(

                    location =
                        location,

                    coaching =
                        coaching,

                    studentId =
                        studentId,

                    onStateChange =
                        onStateChange,

                    onMessageChange =
                        onMessageChange,

                    onDistanceChange =
                        onDistanceChange
                )
            }

            override fun onProviderEnabled(
                provider: String
            ) {
            }

            override fun onProviderDisabled(
                provider: String
            ) {
            }
        }

    /*
     * Explicit permission guard.
     */

    val canRequestLocation =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    if (!canRequestLocation) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Location permission was denied."
        )

        return
    }

    try {

        locationManager.requestLocationUpdates(

            LocationManager.GPS_PROVIDER,

            0L,

            0f,

            locationListener!!,

            Looper.getMainLooper()
        )

        handler.postDelayed(
            timeoutRunnable,
            15_000L
        )

    } catch (
        e: SecurityException
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Location permission was denied."
        )

    } catch (
        e: Exception
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Unable to access GPS location."
        )
    }
}

private fun processAttendanceLocation(
    location: Location,
    coaching: CoachingProfile,
    studentId: String,
    onStateChange:
        (GpsAttendanceState) -> Unit,
    onMessageChange:
        (String) -> Unit,
    onDistanceChange:
        (Float) -> Unit
) {

    val coachingLatitude =
        coaching.latitude ?: return

    val coachingLongitude =
        coaching.longitude ?: return

    val distance =
        FloatArray(1)

    Location.distanceBetween(

        location.latitude,
        location.longitude,

        coachingLatitude,
        coachingLongitude,

        distance
    )

    val distanceMeters =
        distance[0]

    onDistanceChange(
        distanceMeters
    )

    /*
     * Reject spoofed / fake-GPS locations. Without this check,
     * a student can use a "Fake GPS" app to report themselves
     * as being inside the coaching's radius without actually
     * being there.
     */

    if (
        location.isFromMockProvider
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "Mock/fake location detected. Please disable any fake GPS app and try again with your real location."
        )

        return
    }

    /*
     * Accuracy check.
     */

    if (
        location.hasAccuracy() &&
        location.accuracy > 150f
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "GPS accuracy is too low (${location.accuracy.toInt()} m). Please try again in an open area."
        )

        return
    }

    /*
     * Coaching radius check.
     */

    val radius =
        coaching.attendanceRadiusMeters
            .coerceAtLeast(20f)

    if (
        distanceMeters > radius
    ) {

        onStateChange(
            GpsAttendanceState.ERROR
        )

        onMessageChange(
            "You are outside the coaching attendance area. Required within ${radius.toInt()} m."
        )

        return
    }

    /*
     * Duplicate attendance protection.
     */

    if (
        StudentGpsAttendanceStore
            .hasAttendanceToday(studentId)
    ) {

        onStateChange(
            GpsAttendanceState.SUCCESS
        )

        onMessageChange(
            "Today's attendance has already been marked."
        )

        return
    }

    /*
     * Save attendance.
     */

    val success =
        StudentGpsAttendanceStore
            .addAttendance(

                studentId =
                    studentId,

                coachingId =
                    coaching.id,

                latitude =
                    location.latitude,

                longitude =
                    location.longitude,

                distanceMeters =
                    distanceMeters
            )

    if (success) {

        onStateChange(
            GpsAttendanceState.SUCCESS
        )

        onMessageChange(
            "Attendance marked successfully. You are ${formatDistance(distanceMeters)} from the coaching."
        )

    } else {

        onStateChange(
            GpsAttendanceState.SUCCESS
        )

        onMessageChange(
            "Today's attendance has already been marked."
        )
    }
}

private fun isLocationRecent(
    location: Location
): Boolean {

    val age =
        System.currentTimeMillis() -
                location.time

    return age in 0..120_000
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