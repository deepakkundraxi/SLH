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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalCoachingManagementScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // Search query
    var searchQuery by remember { mutableStateOf("") }

    // Read version so list refreshes when active flag / add changes
    val version = CoachingProfileStore.version
    val allCoachings = remember(version) {
        CoachingProfileStore.getAll()
    }

    val coachings = remember(allCoachings, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) allCoachings
        else allCoachings.filter {
            it.name.contains(q, true) ||
                    it.shortName.contains(q, true) ||
                    it.id.contains(q, true)
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newShort by remember { mutableStateOf("") }
    var newTagline by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var selectedCoachingId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Coaching Management",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${coachings.size} institutions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
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

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search coaching") }
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        showAdd = !showAdd
                        error = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.size(8.dp))

                    Text(
                        if (showAdd) "Cancel"
                        else "Add Coaching"
                    )
                }
            }

            if (showAdd) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "New Coaching",
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Coaching Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newShort,
                                onValueChange = { newShort = it },
                                label = { Text("Short Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newTagline,
                                onValueChange = { newTagline = it },
                                label = { Text("Tagline") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            if (error.isNotBlank()) {
                                Text(
                                    error,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Button(
                                onClick = {
                                    if (!PlatformSettingsStore.allowNewRegistrations) {
                                        error =
                                            "New coaching registrations are disabled."
                                        return@Button
                                    }

                                    if (
                                        newName.isBlank() ||
                                        newShort.isBlank()
                                    ) {
                                        error =
                                            "Name and short name are required."
                                        return@Button
                                    }

                                    val id =
                                        "C${System.currentTimeMillis() % 100000}"

                                    CoachingProfileStore.add(
                                        CoachingProfile(
                                            id = id,
                                            name = newName.trim(),
                                            shortName = newShort
                                                .trim()
                                                .take(6)
                                                .uppercase(),
                                            tagline = newTagline.trim()
                                        )
                                    )

                                    newName = ""
                                    newShort = ""
                                    newTagline = ""
                                    error = ""
                                    showAdd = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Coaching")
                            }
                        }
                    }
                }
            }

            if (coachings.isEmpty()) {
                item {
                    Text(
                        "No coaching institutions yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(
                coachings,
                key = { it.id }
            ) { coaching ->

                val active =
                    CoachingProfileStore.isActive(coaching.id)

                Card(
                    onClick = {
                        selectedCoachingId =
                            if (selectedCoachingId == coaching.id) {
                                null
                            } else {
                                coaching.id
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (active) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(Modifier.size(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    coaching.name,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "${coaching.shortName} · ${
                                        coaching.tagline.ifBlank {
                                            "No tagline"
                                        }
                                    }",
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )

                                Text(
                                    if (active) "Active" else "Disabled",
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    color =
                                        if (active) {
                                            Color(0xFF2E7D32)
                                        } else {
                                            Color(0xFFC62828)
                                        }
                                )
                            }

                            IconButton(
                                onClick = {
                                    CoachingProfileStore.setActive(
                                        coaching.id,
                                        !active
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (active) {
                                            Icons.Default.ToggleOn
                                        } else {
                                            Icons.Default.ToggleOff
                                        },
                                    contentDescription = "Toggle active",
                                    tint =
                                        if (active) {
                                            Color(0xFF2E7D32)
                                        } else {
                                            Color(0xFF9E9E9E)
                                        },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        if (selectedCoachingId == coaching.id) {
                            Spacer(Modifier.height(10.dp))

                            val linkedUsers =
                                UserAccountStore.getAll()
                                    .filter {
                                        it.coachingId == coaching.id
                                    }

                            val studentRecords =
                                StudentStore.students.count {
                                    it.coachingId == coaching.id
                                }

                            Text(
                                "ID: ${coaching.id}",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )

                            Text(
                                "Phone: ${
                                    coaching.phone.ifBlank { "—" }
                                }",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )

                            Text(
                                "Email: ${
                                    coaching.email.ifBlank { "—" }
                                }",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )

                            Text(
                                "Linked accounts: ${linkedUsers.size}",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )

                            Text(
                                "Student records: $studentRecords",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )

                            Text(
                                "Admins: ${
                                    linkedUsers.count {
                                        it.role ==
                                                UserRole.COACHING_ADMIN
                                    }
                                } · " +
                                        "Teachers: ${
                                            linkedUsers.count {
                                                it.role ==
                                                        UserRole.TEACHER
                                            }
                                        } · " +
                                        "Students: ${
                                            linkedUsers.count {
                                                it.role ==
                                                        UserRole.STUDENT
                                            }
                                        }",
                                style =
                                    MaterialTheme.typography.labelSmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}