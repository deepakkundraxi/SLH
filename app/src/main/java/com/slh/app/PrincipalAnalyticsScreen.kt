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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalAnalyticsScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val users = UserAccountStore.getAll()
    val coachings = CoachingProfileStore.getAll()
    val activeCoachings = coachings.count { CoachingProfileStore.isActive(it.id) }
    val pending = users.count { it.status == AccountStatus.PENDING }
    val approved = users.count { it.status == AccountStatus.APPROVED }
    val suspended = users.count { it.status == AccountStatus.SUSPENDED }
    val students = users.count { it.role == UserRole.STUDENT }
    val teachers = users.count { it.role == UserRole.TEACHER }
    val admins = users.count { it.role == UserRole.COACHING_ADMIN }
    val storeStudents = StudentStore.students.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Platform Analytics", fontWeight = FontWeight.Bold)
                        Text(
                            "Overview of SLH platform",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsRow(
                listOf(
                    "Total Users" to users.size.toString(),
                    "Approved" to approved.toString()
                )
            )
            AnalyticsRow(
                listOf(
                    "Pending" to pending.toString(),
                    "Suspended" to suspended.toString()
                )
            )
            AnalyticsRow(
                listOf(
                    "Students (accounts)" to students.toString(),
                    "Students (records)" to storeStudents.toString()
                )
            )
            AnalyticsRow(
                listOf(
                    "Teachers" to teachers.toString(),
                    "Coaching Admins" to admins.toString()
                )
            )
            AnalyticsRow(
                listOf(
                    "Coachings" to coachings.size.toString(),
                    "Active coachings" to activeCoachings.toString()
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Login branding", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    val b = MainLoginBrandingStore.branding
                    Text(b.name)
                    Text(b.tagline, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (PlatformSettingsStore.maintenanceMode)
                            "Maintenance mode: ON"
                        else
                            "Maintenance mode: OFF",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnalyticsRow(pairs: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pairs.forEach { (title, value) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
