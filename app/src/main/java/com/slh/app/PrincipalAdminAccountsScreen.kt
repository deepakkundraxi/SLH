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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalAdminAccountsScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val accountContext = LocalContext.current

    val version = UserAccountStore.version
    val admins = remember(version) {
        UserAccountStore.coachingAdmins()
    }
    val coachings = remember(CoachingProfileStore.version) {
        CoachingProfileStore.getAll()
    }

    var showAdd by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var coachingId by remember { mutableStateOf(coachings.firstOrNull()?.id ?: "") }
    var error by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Coaching Admins",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create & approve admin accounts",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        showAdd = !showAdd
                        error = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (showAdd) "Cancel" else "Create Coaching Admin")
                }
            }

            if (showAdd) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("New Admin", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = coachingId,
                                onValueChange = { coachingId = it },
                                label = { Text("Coaching ID (e.g. C001)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (coachings.isNotEmpty()) {
                                Text(
                                    "Available: " + coachings.joinToString { "${it.id}=${it.shortName}" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (error.isNotBlank()) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = {
                                    when {
                                        displayName.isBlank() || username.isBlank() || password.isBlank() ->
                                            error = "All fields are required."
                                        coachingId.isBlank() ->
                                            error = "Coaching ID is required."
                                        UserAccountStore.findByUsername(username.trim()) != null ->
                                            error = "Username already exists."
                                        else -> {
                                            val status =
                                                if (PlatformSettingsStore.requireAdminApproval)
                                                    AccountStatus.PENDING
                                                else
                                                    AccountStatus.APPROVED
                                            val newAdmin = DemoUser(
                                                id = "A${System.currentTimeMillis() % 100000}",
                                                username = username.trim(),
                                                password = PasswordHasher.hash(password),
                                                role = UserRole.COACHING_ADMIN,
                                                status = status,
                                                coachingId = coachingId.trim(),
                                                displayName = displayName.trim()
                                            )
                                            UserAccountStore.add(newAdmin)

                                            // Cloud login so this admin can sign in from their own phone.
                                            AccountCloudSync.provision(
                                                accountContext,
                                                newAdmin,
                                                password
                                            )
                                            displayName = ""
                                            username = ""
                                            password = ""
                                            error = ""
                                            showAdd = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create (Pending Approval)")
                            }
                        }
                    }
                }
            }

            items(admins, key = { it.id }) { admin ->
                val coachingName = admin.coachingId?.let {
                    try { CoachingProfileStore.get(it).name } catch (_: Exception) { it }
                } ?: "—"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(admin.displayName, fontWeight = FontWeight.Bold)
                                Text(
                                    "@${admin.username} · $coachingName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Status: ${admin.status.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (admin.status) {
                                        AccountStatus.APPROVED -> Color(0xFF2E7D32)
                                        AccountStatus.PENDING -> Color(0xFFF9A825)
                                        AccountStatus.REJECTED -> Color(0xFFC62828)
                                        AccountStatus.SUSPENDED -> Color(0xFF6A1B9A)
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (admin.status != AccountStatus.APPROVED) {
                                Button(
                                    onClick = {
                                        UserAccountStore.updateStatus(admin.id, AccountStatus.APPROVED)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve")
                                }
                            }
                            if (admin.status == AccountStatus.PENDING) {
                                OutlinedButton(
                                    onClick = {
                                        UserAccountStore.updateStatus(admin.id, AccountStatus.REJECTED)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject")
                                }
                            }
                            if (admin.status == AccountStatus.APPROVED) {
                                OutlinedButton(
                                    onClick = {
                                        UserAccountStore.updateStatus(admin.id, AccountStatus.SUSPENDED)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Suspend")
                                }
                            }
                            if (admin.status == AccountStatus.SUSPENDED) {
                                Button(
                                    onClick = {
                                        UserAccountStore.updateStatus(admin.id, AccountStatus.APPROVED)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reactivate")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}