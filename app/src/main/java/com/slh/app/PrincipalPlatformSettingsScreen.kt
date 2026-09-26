package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalPlatformSettingsScreen(
    onBack: () -> Unit,
    onOpenBranding: () -> Unit
) {
    BackHandler { onBack() }

    val allowNewRegistrations =
        PlatformSettingsStore.allowNewRegistrations

    val requireAdminApproval =
        PlatformSettingsStore.requireAdminApproval

    val maintenanceMode =
        PlatformSettingsStore.maintenanceMode

    val branding =
        MainLoginBrandingStore.branding

    val coachingCount =
        CoachingProfileStore.getAll().size

    val adminCount =
        UserAccountStore.coachingAdmins().size

    val approvedAdmins =
        UserAccountStore.coachingAdmins().count {
            it.status == AccountStatus.APPROVED
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Platform Settings",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "System controls",
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
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor =
                            MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor =
                            MaterialTheme.colorScheme.onSurface
                    )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .primaryContainer
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Platform Overview",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Coachings: $coachingCount"
                    )

                    Text(
                        text =
                            "Coaching Admins: $adminCount " +
                                    "($approvedAdmins approved)"
                    )

                    Text(
                        text =
                            "Login brand: ${branding.name}"
                    )
                }
            }

            Text(
                text = "Controls",
                fontWeight = FontWeight.Bold
            )

            SettingsSwitchRow(
                title =
                    "Allow new coaching registrations",

                subtitle =
                    "When off, new coachings cannot be added from public flows",

                checked =
                    allowNewRegistrations,

                onCheckedChange = {
                    PlatformSettingsStore
                        .updateAllowNewRegistrations(it)
                }
            )

            SettingsSwitchRow(
                title =
                    "Require admin approval",

                subtitle =
                    "New coaching admins stay pending until Principal approves",

                checked =
                    requireAdminApproval,

                onCheckedChange = {
                    PlatformSettingsStore
                        .updateRequireAdminApproval(it)
                }
            )

            SettingsSwitchRow(
                title =
                    "Maintenance mode",

                subtitle =
                    "Blocks non-principal logins & shows a banner on the login screen",

                checked =
                    maintenanceMode,

                onCheckedChange = {
                    PlatformSettingsStore
                        .updateMaintenanceMode(it)
                }
            )

            Text(
                text = "Branding",
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = onOpenBranding,

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp)
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
                        Icons.Default.Info,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.size(12.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Login Branding",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "${branding.name} — ${branding.tagline}",

                            style =
                                MaterialTheme.typography.labelSmall,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Edit",
                        color =
                            MaterialTheme.colorScheme.primary,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = subtitle,

                    style =
                        MaterialTheme.typography.labelSmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}