package com.slh.app


import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


// =============================================================
// PRINCIPAL ADMIN DASHBOARD
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalAdminScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {

    BackHandler {
        onBack()
    }

    var showBrandingEditor by remember {
        mutableStateOf(false)
    }

    var showCoachingManagement by remember {
        mutableStateOf(false)
    }

    var showAdminAccounts by remember {
        mutableStateOf(false)
    }

    var showPlatformSettings by remember {
        mutableStateOf(false)
    }

    var showUsersOverview by remember {
        mutableStateOf(false)
    }

    var showPendingApprovals by remember {
        mutableStateOf(false)
    }

    var showAnalytics by remember {
        mutableStateOf(false)
    }

    val (themeMode, setThemeMode) =
        rememberThemeMode()

    // =========================================================
    // SUB-SCREENS
    // =========================================================

    if (showBrandingEditor) {
        PrincipalLoginBrandingScreen(
            onBack = { showBrandingEditor = false }
        )
        return
    }

    if (showCoachingManagement) {
        PrincipalCoachingManagementScreen(
            onBack = { showCoachingManagement = false }
        )
        return
    }

    if (showAdminAccounts) {
        PrincipalAdminAccountsScreen(
            onBack = { showAdminAccounts = false }
        )
        return
    }

    if (showPlatformSettings) {
        PrincipalPlatformSettingsScreen(
            onBack = { showPlatformSettings = false },
            onOpenBranding = {
                showPlatformSettings = false
                showBrandingEditor = true
            }
        )
        return
    }

    if (showUsersOverview) {
        PrincipalUsersScreen(
            onBack = { showUsersOverview = false },
            pendingOnly = false
        )
        return
    }

    if (showPendingApprovals) {
        PrincipalUsersScreen(
            onBack = { showPendingApprovals = false },
            pendingOnly = true
        )
        return
    }

    if (showAnalytics) {
        PrincipalAnalyticsScreen(
            onBack = { showAnalytics = false }
        )
        return
    }

    // =========================================================
    // PLATFORM COUNTS
    // =========================================================

    // version triggers refresh when principal changes accounts


    val users = UserAccountStore.getAll()
    val allUsers = UserAccountStore.getAll()

    val totalUsers = allUsers.size

    val totalAdmins =
        allUsers.count { it.role == UserRole.COACHING_ADMIN }

    val totalTeachers =
        allUsers.count { it.role == UserRole.TEACHER }

    val totalStudents =
        StudentStore.students.size

    val totalCoachings =
        CoachingProfileStore.getAll().size

    // =========================================================
    // MAIN DASHBOARD
    // =========================================================

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        topBar = {

            // =================================================
            // ADMIN STYLE HEADER
            // =================================================

            TopAppBar(

                title = {

                    Column {

                        Text(

                            text =
                                "Principal Dashboard",

                            fontWeight =
                                FontWeight.Bold,

                            maxLines =
                                1,

                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(

                            text =
                                "Platform Control",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

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
                        onClick = { showPlatformSettings = true }
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Settings,

                            contentDescription =
                                "Platform Settings"
                        )
                    }

                    IconButton(
                        onClick = onLogout
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
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                    .navigationBarsPadding()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            // =================================================
            // WELCOME CARD
            // =================================================

            PrincipalWelcomeCard()

            // =================================================
            // PLATFORM OVERVIEW
            // =================================================

            PrincipalSectionTitle(
                text = "Platform Overview"
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                PrincipalStatCard(

                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.People,

                    title =
                        "Users",

                    value =
                        totalUsers.toString(),

                    backgroundColor =
                        Color(0xFFE3F2FD),

                    iconColor =
                        Color(0xFF1565C0)
                )

                PrincipalStatCard(

                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.School,

                    title =
                        "Students",

                    value =
                        totalStudents.toString(),

                    backgroundColor =
                        Color(0xFFE8F5E9),

                    iconColor =
                        Color(0xFF2E7D32)
                )

                PrincipalStatCard(

                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.SupervisorAccount,

                    title =
                        "Teachers",

                    value =
                        totalTeachers.toString(),

                    backgroundColor =
                        Color(0xFFFFF3E0),

                    iconColor =
                        Color(0xFFEF6C00)
                )
            }

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                PrincipalStatCard(

                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.AdminPanelSettings,

                    title =
                        "Admins",

                    value =
                        totalAdmins.toString(),

                    backgroundColor =
                        Color(0xFFF3E5F5),

                    iconColor =
                        Color(0xFF7B1FA2)
                )

                PrincipalStatCard(

                    modifier =
                        Modifier.weight(2f),

                    icon =
                        Icons.Default.Dashboard,

                    title =
                        "Platform Status",

                    value =
                        "Active",

                    backgroundColor =
                        Color(0xFFE0F2F1),

                    iconColor =
                        Color(0xFF00897B)
                )
            }

            // =================================================
            // MANAGEMENT
            // =================================================

            PrincipalSectionTitle(
                text = "Core Management"
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                PrincipalManagementCard(

                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.People,

                    title =
                        "Coaching Admins",

                    subtitle =
                        "Create & approve",

                    backgroundColor =
                        Color(0xFFFCE4EC),

                    iconColor =
                        Color(0xFFC2185B),

                    onClick = {
                        showAdminAccounts = true
                    }
                )

                PrincipalManagementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    title = "All Users",
                    subtitle = "Search & manage",
                    backgroundColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32),
                    onClick = { showUsersOverview = true }
                )
            }

            // =================================================
            // MORE TOOLS (secondary features, kept lightweight)
            // =================================================

            PrincipalSectionTitle(
                text = "More Tools"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrincipalManagementCard(

                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Default.Dashboard,

                    title =
                        "Coachings",

                    subtitle =
                        "Institutions list",

                    backgroundColor =
                        Color(0xFFFFF8E1),

                    iconColor =
                        Color(0xFFF9A825),

                    onClick = {
                        showCoachingManagement = true
                    }
                )

                PrincipalManagementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Notifications,
                    title = "Pending",
                    subtitle = "Approvals center",
                    backgroundColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFEF6C00),
                    onClick = { showPendingApprovals = true }
                )

                PrincipalManagementCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Dashboard,
                    title = "Analytics",
                    subtitle = "Platform overview",
                    backgroundColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1565C0),
                    onClick = { showAnalytics = true }
                )
            }

            // =================================================
            // CURRENT LOGIN BRANDING
            // =================================================

            PrincipalSectionTitle(
                text = "Current Login Branding"
            )

            PrincipalBrandingPreviewCard()

            // =================================================
            // PRINCIPAL ACCESS
            // =================================================

            PrincipalSectionTitle(
                text = "Principal Access"
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
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
                            "Platform-level access",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )

                    Text(

                        text =
                            "Principal Admin controls the main login branding and platform-level settings.",

                        fontSize =
                            11.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        maxLines =
                            3,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }

            // =================================================
            // APPEARANCE
            // =================================================

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

            // =================================================
            // BOTTOM LOGO
            // =================================================

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            PrincipalBottomLogo()

            Spacer(
                modifier =
                    Modifier.height(
                        48.dp
                    )
            )
        }
    }
}


// =============================================================
// WELCOME CARD
// =============================================================

@Composable
private fun PrincipalWelcomeCard() {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFE8EAF6)
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF3949AB)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(

                    imageVector =
                        Icons.Default.AdminPanelSettings,

                    contentDescription =
                        null,

                    tint =
                        Color.White,

                    modifier =
                        Modifier.size(
                            28.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        "Welcome, Principal Admin",

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF283593),

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(

                    text =
                        "Manage your SLH platform from one place.",

                    fontSize =
                        11.sp,

                    color =
                        Color(0xFF5C6BC0),

                    maxLines =
                        2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}


// =============================================================
// SECTION TITLE
// =============================================================

@Composable
private fun PrincipalSectionTitle(
    text: String
) {

    Text(

        text =
            text,

        fontSize =
            15.sp,

        fontWeight =
            FontWeight.Bold,

        color =
            MaterialTheme
                .colorScheme
                .onSurface,

        maxLines =
            1,

        overflow =
            TextOverflow.Ellipsis
    )
}


// =============================================================
// STAT CARD
// =============================================================

@Composable
private fun PrincipalStatCard(

    modifier: Modifier,

    icon: ImageVector,

    title: String,

    value: String,

    backgroundColor: Color,

    iconColor: Color
) {

    Card(

        modifier =
            modifier.height(
                92.dp
            ),

        shape =
            RoundedCornerShape(
                15.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        10.dp
                    ),

            verticalArrangement =
                Arrangement.Center
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

                    tint =
                        iconColor,

                    modifier =
                        Modifier.size(
                            20.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )

                Text(

                    text =
                        title,

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Medium,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(

                text =
                    value,

                fontSize =
                    19.sp,

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


// =============================================================
// MANAGEMENT CARD
// =============================================================

@Composable
private fun PrincipalManagementCard(

    modifier: Modifier,

    icon: ImageVector,

    title: String,

    subtitle: String,

    backgroundColor: Color,

    iconColor: Color,

    onClick: (() -> Unit)?
) {

    Card(

        modifier =
            modifier
                .height(108.dp)
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
                    backgroundColor
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

            Box(

                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(
                            RoundedCornerShape(
                                11.dp
                            )
                        )
                        .background(
                            iconColor
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
                        Color.White,

                    modifier =
                        Modifier.size(
                            21.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )

            Text(

                text =
                    title,

                fontSize =
                    13.sp,

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
// BRANDING PREVIEW
// =============================================================

@Composable
private fun PrincipalBrandingPreviewCard() {

    val branding =
        MainLoginBrandingStore.branding

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

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (
                branding.logoUri != null
            ) {

                AsyncImage(

                    model =
                        branding.logoUri,

                    contentDescription =
                        "Login Logo",

                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            ),

                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Box(

                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
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
                            branding.shortName
                                .take(3)
                                .uppercase(),

                        fontSize =
                            17.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        branding.name.ifBlank {
                            "Coaching Name"
                        },

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(

                    text =
                        branding.tagline.ifBlank {
                            "Your tagline"
                        },

                    fontSize =
                        10.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines =
                        2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}


// =============================================================
// BOTTOM SLH LOGO
// =============================================================

@Composable
private fun PrincipalBottomLogo() {

    val isDark =
        isSystemInDarkTheme()

    val logoRes =
        if (isDark) {
            R.drawable.slh_word_logo_dark
        } else {
            R.drawable.slh_word_logo_light
        }

    Image(
        painter = painterResource(id = logoRes),
        contentDescription = "Sohan's Learning Hub",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(48.dp),
        contentScale = ContentScale.Fit
    )
}


// =============================================================
// PRINCIPAL LOGIN BRANDING SCREEN
// =============================================================

@Composable
private fun PrincipalLoginBrandingScreen(
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val currentBranding =
        MainLoginBrandingStore.branding

    var name by remember {
        mutableStateOf(
            currentBranding.name
        )
    }

    var shortName by remember {
        mutableStateOf(
            currentBranding.shortName
        )
    }

    var tagline by remember {
        mutableStateOf(
            currentBranding.tagline
        )
    }

    var logoUri by remember {

        mutableStateOf<Uri?>(
            currentBranding.logoUri?.let {
                Uri.parse(it)
            }
        )
    }

    var saved by remember {
        mutableStateOf(false)
    }

    val logoPicker =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri ->

            if (uri != null) {

                logoUri =
                    uri

                saved =
                    false
            }
        }

    Surface(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
        ) {

            // =================================================
            // BRANDING HEADER
            // =================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

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

                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )

                Column {

                    Text(

                        text =
                            "Login Branding",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(

                        text =
                            "Shown on main login page",

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

            // =================================================
            // BRANDING CONTENT
            // =================================================

            Column(

                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                // =============================================
                // LOGO CARD
                // =============================================

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                ) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    18.dp
                                ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        if (
                            logoUri != null
                        ) {

                            AsyncImage(

                                model =
                                    logoUri,

                                contentDescription =
                                    "Coaching Logo",

                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                18.dp
                                            )
                                        ),

                                contentScale =
                                    ContentScale.Crop
                            )

                        } else {

                            Box(

                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                18.dp
                                            )
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
                                        Icons.Default.Image,

                                    contentDescription =
                                        null,

                                    modifier =
                                        Modifier.size(
                                            42.dp
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
                                Modifier.height(
                                    12.dp
                                )
                        )

                        OutlinedButton(

                            onClick = {

                                logoPicker.launch(
                                    "image/*"
                                )
                            }
                        ) {

                            Text(

                                text =
                                    if (
                                        logoUri == null
                                    ) {
                                        "Choose Logo"
                                    } else {
                                        "Change Logo"
                                    }
                            )
                        }
                    }
                }

                // =============================================
                // NAME
                // =============================================

                OutlinedTextField(

                    value =
                        name,

                    onValueChange = {

                        name =
                            it

                        saved =
                            false
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "Coaching Name"
                        )
                    },

                    singleLine =
                        true
                )

                // =============================================
                // SHORT NAME
                // =============================================

                OutlinedTextField(

                    value =
                        shortName,

                    onValueChange = {

                        shortName =
                            it

                        saved =
                            false
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "Short Name"
                        )
                    },

                    singleLine =
                        true
                )

                // =============================================
                // TAGLINE
                // =============================================

                OutlinedTextField(

                    value =
                        tagline,

                    onValueChange = {

                        tagline =
                            it

                        saved =
                            false
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "Tagline"
                        )
                    },

                    minLines =
                        2,

                    maxLines =
                        3
                )

                // =============================================
                // PREVIEW
                // =============================================

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                ) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    18.dp
                                ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(

                            text =
                                "Login Page Preview",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )

                        if (
                            logoUri != null
                        ) {

                            AsyncImage(

                                model =
                                    logoUri,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier
                                        .size(78.dp)
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
                                        .size(78.dp)
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
                                        shortName
                                            .take(3)
                                            .uppercase(),

                                    fontSize =
                                        20.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )

                        Text(

                            text =
                                name.ifBlank {
                                    "Coaching Name"
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold,

                            maxLines =
                                2,

                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(

                            text =
                                tagline.ifBlank {
                                    "Your tagline"
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            maxLines =
                                3,

                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }

                // =============================================
                // SAVE MESSAGE
                // =============================================

                if (saved) {

                    Text(

                        text =
                            "Branding saved successfully.",

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        fontWeight =
                            FontWeight.SemiBold,

                        fontSize =
                            12.sp
                    )
                }

                // =============================================
                // BOTTOM LOGO
                // =============================================

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                PrincipalBottomLogo()

                Spacer(
                    modifier =
                        Modifier.height(
                            40.dp
                        )
                )
            }

            // =================================================
            // SAVE BUTTON
            // =================================================

            Button(

                onClick = {

                    MainLoginBrandingStore.update(

                        MainLoginBranding(

                            name =
                                name.trim(),

                            shortName =
                                shortName.trim(),

                            tagline =
                                tagline.trim(),

                            logoUri =
                                logoUri?.toString()
                        )
                    )

                    saved =
                        true
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Save,

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    "Save Branding"
                )
            }
        }
    }
}