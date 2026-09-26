package com.slh.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

@Composable
fun CoachingProfileScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val context =
        androidx.compose.ui.platform.LocalContext.current

    /*
     * CoachingProfileStore.get() returns CoachingProfile?
     *
     * Therefore we first create a safe non-null profile.
     */
    val currentProfile =
        remember(coachingId) {
            CoachingProfileStore.get(coachingId)
        }

    val profile =
        currentProfile
            ?: CoachingProfile(
                id = coachingId,
                name = "SLH",
                 shortName = "SLH",
                tagline = "Learn Today. Succeed Tomorrow.",
                logoUri = null,
                phone = "",
                email = "",
                address = "",
                latitude = null,
                longitude = null,
                attendanceRadiusMeters = 100f
            )

    var coachingName by remember(coachingId) {
        mutableStateOf(profile.name)
    }

    var shortName by remember(coachingId) {
        mutableStateOf(profile.shortName)
    }

    var tagline by remember(coachingId) {
        mutableStateOf(profile.tagline)
    }

    var phone by remember(coachingId) {
        mutableStateOf(profile.phone)
    }

    var email by remember(coachingId) {
        mutableStateOf(profile.email)
    }

    var address by remember(coachingId) {
        mutableStateOf(profile.address)
    }

    var logoUri by remember(coachingId) {
        mutableStateOf(
            profile.logoUri?.let {
                Uri.parse(it)
            }
        )
    }

    var latitude by remember(coachingId) {
        mutableStateOf(
            profile.latitude?.toString() ?: ""
        )
    }

    var longitude by remember(coachingId) {
        mutableStateOf(
            profile.longitude?.toString() ?: ""
        )
    }

    var radiusText by remember(coachingId) {
        mutableStateOf(
            profile.attendanceRadiusMeters
                .toInt()
                .toString()
        )
    }

    var locationMessage by remember {
        mutableStateOf("")
    }

    var savedMessage by remember {
        mutableStateOf("")
    }

    var isGettingLocation by remember {
        mutableStateOf(false)
    }

    var locationListener: LocationListener? = null

    val locationManager =
        remember {
            context.getSystemService(
                android.content.Context.LOCATION_SERVICE
            ) as LocationManager
        }

    val locationHandler =
        remember {
            Handler(Looper.getMainLooper())
        }

    val stopLocationUpdates: () -> Unit = {

        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (_: Exception) {
            }
        }

        locationListener = null
        isGettingLocation = false

        locationHandler.removeCallbacksAndMessages(null)
    }

    val handleLocation: (Location) -> Unit = { location ->

        latitude =
            location.latitude.toString()

        longitude =
            location.longitude.toString()

        locationMessage =
            "Current coaching location detected successfully."

        savedMessage = ""

        stopLocationUpdates()
    }

    val startLocationDetection: () -> Unit = {

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

        if (!fineGranted && !coarseGranted) {

            locationMessage =
                "Please allow location permission."

            isGettingLocation = false

        } else {

            isGettingLocation = true

            locationMessage =
                "Getting current location..."

            val lastLocation: Location? =

                if (fineGranted) {

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        null
                    } else {

                        try {
                            locationManager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER
                            )
                        } catch (_: SecurityException) {
                            null
                        }
                    }

                } else {

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        null
                    } else {

                        try {
                            locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER
                            )
                        } catch (_: SecurityException) {
                            null
                        }
                    }
                }

            if (lastLocation != null) {

                handleLocation(lastLocation)

            } else {

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

                    isGettingLocation = false

                    locationMessage =
                        "Location permission was denied."

                } else {

                    val listener =
                        object : LocationListener {

                            override fun onLocationChanged(
                                location: Location
                            ) {
                                handleLocation(location)
                            }

                            override fun onProviderEnabled(
                                provider: String
                            ) {
                                locationMessage =
                                    "Location service enabled. Getting location..."
                            }

                            override fun onProviderDisabled(
                                provider: String
                            ) {
                                stopLocationUpdates()

                                locationMessage =
                                    "Please turn on GPS/location services."
                            }

                            @Deprecated("Deprecated in Android API")
                            override fun onStatusChanged(
                                provider: String?,
                                status: Int,
                                extras: Bundle?
                            ) {
                            }
                        }

                    locationListener = listener

                    try {

                        if (fineGranted) {

                            locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                1000L,
                                1f,
                                listener,
                                Looper.getMainLooper()
                            )

                        } else {

                            locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                1000L,
                                1f,
                                listener,
                                Looper.getMainLooper()
                            )
                        }

                        locationHandler.postDelayed(
                            {

                                if (isGettingLocation) {

                                    stopLocationUpdates()

                                    locationMessage =
                                        "Could not get a fresh location. Please try again."
                                }

                            },
                            15000L
                        )

                    } catch (
                        securityException: SecurityException
                    ) {

                        stopLocationUpdates()

                        locationMessage =
                            "Location permission is required."
                    } catch (
                        exception: Exception
                    ) {

                        stopLocationUpdates()

                        locationMessage =
                            "Unable to get location. Please try again."
                    }
                }
            }
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
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

                startLocationDetection()

            } else {

                isGettingLocation = false

                locationMessage =
                    "Location permission was denied."
            }
        }

    val getCurrentLocation: () -> Unit = {

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

        if (fineGranted || coarseGranted) {

            startLocationDetection()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                logoUri = uri

                savedMessage = ""
            }
        }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {

            /* ---------------- HEADER ---------------- */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

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

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Coaching Profile",
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Name, logo & tagline",
                        style =
                            MaterialTheme.typography.labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            /* ---------------- CONTENT ---------------- */

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            horizontal = 20.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Manage your coaching information",
                    fontSize =
                        14.sp,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /* ---------------- LOGO ---------------- */

                Box(
                    modifier =
                        Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme
                                    .primaryContainer
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    if (logoUri != null) {

                        AsyncImage(
                            model = logoUri,
                            contentDescription =
                                "Coaching Logo",
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            contentScale =
                                ContentScale.Crop
                        )

                    } else {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Business,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(36.dp),
                                tint =
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    shortName
                                        .ifBlank {
                                            "LOGO"
                                        }
                                        .take(8),
                                fontSize =
                                    13.sp,
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

                OutlinedButton(
                    onClick = {
                        imagePicker.launch("image/*")
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
                            "Choose Coaching Logo"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                /* ---------------- BASIC INFO ---------------- */

                ProfileSectionTitle(
                    text =
                        "Basic Information"
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                ProfileField(
                    value =
                        coachingName,
                    onValueChange = {
                        coachingName = it
                        savedMessage = ""
                    },
                    label =
                        "Coaching Name"
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                ProfileField(
                    value =
                        shortName,
                    onValueChange = {
                        shortName = it
                        savedMessage = ""
                    },
                    label =
                        "Short Name",
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                ProfileField(
                    value =
                        tagline,
                    onValueChange = {
                        tagline = it
                        savedMessage = ""
                    },
                    label =
                        "Tagline"
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                /* ---------------- CONTACT ---------------- */

                ProfileSectionTitle(
                    text =
                        "Contact Information"
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                ProfileField(
                    value =
                        phone,
                    onValueChange = {
                        phone = it
                        savedMessage = ""
                    },
                    label =
                        "Phone Number",
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                ProfileField(
                    value =
                        email,
                    onValueChange = {
                        email = it
                        savedMessage = ""
                    },
                    label =
                        "Email",
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                ProfileField(
                    value =
                        address,
                    onValueChange = {
                        address = it
                        savedMessage = ""
                    },
                    label =
                        "Address"
                )

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                /* ---------------- GPS ATTENDANCE ---------------- */

                ProfileSectionTitle(
                    text =
                        "GPS Attendance Settings"
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Set the coaching location and the maximum distance from the coaching where students can mark attendance.",
                    modifier =
                        Modifier.fillMaxWidth(),
                    fontSize =
                        13.sp,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
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
                                    .surfaceVariant
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.LocationOn,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(30.dp),
                                tint =
                                    MaterialTheme.colorScheme
                                        .primary
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
                                        "Coaching Location",
                                    fontSize =
                                        17.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    text =
                                        if (
                                            latitude.isNotBlank() &&
                                            longitude.isNotBlank()
                                        ) {
                                            "Location configured"
                                        } else {
                                            "Location not configured"
                                        },
                                    fontSize =
                                        13.sp,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        Button(
                            onClick =
                                getCurrentLocation,
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(12.dp),
                            enabled =
                                !isGettingLocation
                        ) {

                            Icon(
                                imageVector =
                                    if (isGettingLocation) {
                                        Icons.Default.Refresh
                                    } else {
                                        Icons.Default.MyLocation
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
                                    if (isGettingLocation) {
                                        "Getting Location..."
                                    } else {
                                        "Use Current Location"
                                    },
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        ProfileField(
                            value =
                                latitude,
                            onValueChange = {
                                latitude = it
                                savedMessage = ""
                            },
                            label =
                                "Latitude",
                            singleLine =
                                true
                        )

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        ProfileField(
                            value =
                                longitude,
                            onValueChange = {
                                longitude = it
                                savedMessage = ""
                            },
                            label =
                                "Longitude",
                            singleLine =
                                true
                        )

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        ProfileField(
                            value =
                                radiusText,
                            onValueChange = { value ->

                                if (
                                    value.all {
                                        it.isDigit()
                                    } &&
                                    value.length <= 5
                                ) {

                                    radiusText = value
                                    savedMessage = ""
                                }
                            },
                            label =
                                "Attendance Radius (meters)",
                            singleLine =
                                true
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Recommended: 100 meters",
                            fontSize =
                                12.sp,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )

                        if (
                            latitude.isNotBlank() &&
                            longitude.isNotBlank()
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.CheckCircle,
                                    contentDescription =
                                        null,
                                    modifier =
                                        Modifier.size(20.dp),
                                    tint =
                                        MaterialTheme.colorScheme
                                            .primary
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        "GPS location is ready for student attendance.",
                                    fontSize =
                                        13.sp,
                                    color =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                            }
                        }

                        if (
                            locationMessage.isNotBlank()
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text =
                                    locationMessage,
                                modifier =
                                    Modifier.fillMaxWidth(),
                                fontSize =
                                    13.sp,
                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                /* ---------------- PREVIEW ---------------- */

                ProfileSectionTitle(
                    text =
                        "Branding Preview"
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
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
                                    .surface
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        if (logoUri != null) {

                            AsyncImage(
                                model =
                                    logoUri,
                                contentDescription =
                                    "Logo Preview",
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(CircleShape),
                                contentScale =
                                    ContentScale.Crop
                            )

                        } else {

                            Box(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
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
                                        shortName
                                            .ifBlank {
                                                "CO"
                                            }
                                            .take(8),
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                coachingName.ifBlank {
                                    "Coaching Name"
                                },
                            fontSize =
                                20.sp,
                            fontWeight =
                                FontWeight.Bold,
                            textAlign =
                                TextAlign.Center
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                tagline.ifBlank {
                                    "Your coaching tagline"
                                },
                            fontSize =
                                13.sp,
                            textAlign =
                                TextAlign.Center,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                /* ---------------- SAVE ---------------- */

                Button(
                    onClick = {

                        val parsedLatitude =
                            latitude
                                .trim()
                                .toDoubleOrNull()

                        val parsedLongitude =
                            longitude
                                .trim()
                                .toDoubleOrNull()

                        val parsedRadius =
                            radiusText
                                .trim()
                                .toFloatOrNull()
                                ?.coerceIn(
                                    20f,
                                    1000f
                                )
                                ?: 100f

                        CoachingProfileStore.update(
                            CoachingProfile(
                                id =
                                    coachingId,

                                name =
                                    coachingName
                                        .trim()
                                        .ifBlank {
                                            "My Coaching"
                                        },

                                shortName =
                                    shortName
                                        .trim()
                                        .ifBlank {
                                            "MC"
                                        },

                                tagline =
                                    tagline.trim(),

                                logoUri =
                                    logoUri?.toString(),

                                phone =
                                    phone.trim(),

                                email =
                                    email.trim(),

                                address =
                                    address.trim(),

                                latitude =
                                    parsedLatitude,

                                longitude =
                                    parsedLongitude,

                                attendanceRadiusMeters =
                                    parsedRadius
                            )
                        )

                        savedMessage =
                            "Coaching profile and GPS settings saved successfully."
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),

                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Text(
                        text =
                            "Save Profile",
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (
                    savedMessage.isNotEmpty()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            savedMessage,
                        fontSize =
                            13.sp,
                        color =
                            MaterialTheme.colorScheme
                                .primary,
                        textAlign =
                            TextAlign.Center,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )
            }
        }
    }
}


/* ------------------------------------------------ */
/* SECTION TITLE */
/* ------------------------------------------------ */

@Composable
private fun ProfileSectionTitle(
    text: String
) {

    Text(
        text =
            text,
        modifier =
            Modifier.fillMaxWidth(),
        fontSize =
            17.sp,
        fontWeight =
            FontWeight.Bold
    )
}


/* ------------------------------------------------ */
/* FIELD */
/* ------------------------------------------------ */

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false
) {

    OutlinedTextField(
        value =
            value,

        onValueChange =
            onValueChange,

        modifier =
            Modifier.fillMaxWidth(),

        label = {
            Text(label)
        },

        singleLine =
            singleLine,

        minLines =
            if (singleLine) {
                1
            } else {
                2
            },

        maxLines =
            if (singleLine) {
                1
            } else {
                4
            }
    )
}