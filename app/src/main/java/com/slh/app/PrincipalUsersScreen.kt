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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class UserFilter {
    ALL, PENDING, STUDENT, TEACHER, ADMIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalUsersScreen(
    onBack: () -> Unit,
    pendingOnly: Boolean = false
) {
    BackHandler { onBack() }

    val version = UserAccountStore.version
    val allUsers = remember(version) { UserAccountStore.getAll() }

    var query by remember { mutableStateOf("") }
    var filter by remember {
        mutableStateOf(if (pendingOnly) UserFilter.PENDING else UserFilter.ALL)
    }

    val filtered = remember(allUsers, query, filter) {
        allUsers.filter { user ->
            val matchesFilter = when (filter) {
                UserFilter.ALL -> true
                UserFilter.PENDING -> user.status == AccountStatus.PENDING
                UserFilter.STUDENT -> user.role == UserRole.STUDENT
                UserFilter.TEACHER -> user.role == UserRole.TEACHER
                UserFilter.ADMIN -> user.role == UserRole.COACHING_ADMIN
            }
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                user.displayName.contains(q, true) ||
                user.username.contains(q, true) ||
                (user.coachingId?.contains(q, true) == true)
            matchesFilter && matchesQuery
        }.sortedWith(
            compareBy<DemoUser> { it.status != AccountStatus.PENDING }
                .thenBy { it.role.title }
                .thenBy { it.displayName.lowercase() }
        )
    }

    val pendingCount = allUsers.count { it.status == AccountStatus.PENDING }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (pendingOnly) "Pending Approvals" else "Users Overview",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pendingOnly)
                                "$pendingCount pending accounts"
                            else
                                "${allUsers.size} platform accounts",
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
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                label = { Text("Search name, username, coaching") }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    UserFilter.ALL to "All",
                    UserFilter.PENDING to "Pending",
                    UserFilter.ADMIN to "Admins",
                    UserFilter.TEACHER to "Teachers",
                    UserFilter.STUDENT to "Students"
                ).forEach { (f, label) ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "No users match this filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }

                items(filtered, key = { it.id }) { user ->
                    UserAccountCard(user = user)
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun UserAccountCard(user: DemoUser) {
    val coachingName = user.coachingId?.let {
        runCatching { CoachingProfileStore.get(it).name }.getOrDefault(it)
    } ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName, fontWeight = FontWeight.Bold)
                    Text(
                        "@${user.username} · ${user.role.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Coaching: $coachingName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Status: ${user.status.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = when (user.status) {
                            AccountStatus.APPROVED -> Color(0xFF2E7D32)
                            AccountStatus.PENDING -> Color(0xFFF9A825)
                            AccountStatus.REJECTED -> Color(0xFFC62828)
                            AccountStatus.SUSPENDED -> Color(0xFF6A1B9A)
                        }
                    )
                }
            }

            if (user.role != UserRole.PRINCIPAL_ADMIN) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user.status != AccountStatus.APPROVED) {
                        Button(
                            onClick = {
                                UserAccountStore.updateStatus(user.id, AccountStatus.APPROVED)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve") }
                    }
                    if (user.status == AccountStatus.PENDING) {
                        OutlinedButton(
                            onClick = {
                                UserAccountStore.updateStatus(user.id, AccountStatus.REJECTED)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject") }
                    }
                    if (user.status == AccountStatus.APPROVED) {
                        OutlinedButton(
                            onClick = {
                                UserAccountStore.updateStatus(user.id, AccountStatus.SUSPENDED)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Suspend") }
                    }
                    if (user.status == AccountStatus.SUSPENDED) {
                        Button(
                            onClick = {
                                UserAccountStore.updateStatus(user.id, AccountStatus.APPROVED)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reactivate") }
                    }
                }
            }
        }
    }
}
