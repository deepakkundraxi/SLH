package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeManagementScreen(
    coachingId: String,
    adminUser: DemoUser,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var showForm by remember {
        mutableStateOf(false)
    }

    var editingNotice by remember {
        mutableStateOf<CoachingNotice?>(null)
    }

    var deletingNotice by remember {
        mutableStateOf<CoachingNotice?>(null)
    }

    val notices = remember(refreshKey) {
        NoticeStore.findNoticesByCoaching(
            coachingId = coachingId
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notice Management",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Announcements for students & teachers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Button(
                onClick = {
                    editingNotice = null
                    showForm = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text("Create New Notice")
            }

            if (notices.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "No notices available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "Create a notice for your students and teachers.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = notices,
                        key = {
                            it.id
                        }
                    ) { notice ->

                        NoticeAdminCard(
                            notice = notice,
                            onEdit = {
                                editingNotice = notice
                                showForm = true
                            },
                            onDelete = {
                                deletingNotice = notice
                            },
                            onToggleStatus = {

                                val newStatus =
                                    if (
                                        notice.status ==
                                        NoticeStatus.ACTIVE
                                    ) {
                                        NoticeStatus.EXPIRED
                                    } else {
                                        NoticeStatus.ACTIVE
                                    }

                                NoticeStore.setNoticeStatus(
                                    noticeId = notice.id,
                                    status = newStatus
                                )

                                refreshKey++
                            }
                        )
                    }
                }
            }
        }
    }

    if (showForm) {

        NoticeFormDialog(
            coachingId = coachingId,
            adminUser = adminUser,
            existingNotice = editingNotice,
            onDismiss = {
                showForm = false
                editingNotice = null
            },
            onSaved = {
                showForm = false
                editingNotice = null
                refreshKey++
            }
        )
    }

    deletingNotice?.let { notice ->

        AlertDialog(
            onDismissRequest = {
                deletingNotice = null
            },
            title = {
                Text("Delete Notice?")
            },
            text = {
                Text(
                    "This notice will no longer be visible to students and teachers."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        NoticeStore.deleteNotice(
                            noticeId = notice.id
                        )

                        deletingNotice = null
                        refreshKey++
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        deletingNotice = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun NoticeAdminCard(
    notice: CoachingNotice,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {

    val targetText =
        if (notice.batchId.isBlank()) {

            "Entire Coaching"

        } else {

            val batch =
                BatchStore.findBatch(
                    notice.batchId
                )

            if (batch != null) {
                "Batch: ${batch.name}"
            } else {
                "Specific Batch"
            }
        }

    val active =
        notice.status == NoticeStatus.ACTIVE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {

                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(26.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                if (notice.batchId.isBlank()) {
                                    Icons.Default.School
                                } else {
                                    Icons.Default.Groups
                                },
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = notice.message,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Notice Date: ${
                        notice.noticeDate.ifBlank {
                            "Not set"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (notice.expiryDate.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Expiry Date: ${notice.expiryDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(50),
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                ) {

                    Text(
                        text =
                            if (active) {
                                "ACTIVE"
                            } else {
                                "EXPIRED"
                            },
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onToggleStatus
                ) {

                    Text(
                        text =
                            if (active) {
                                "Mark Expired"
                            } else {
                                "Activate"
                            }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeFormDialog(
    coachingId: String,
    adminUser: DemoUser,
    existingNotice: CoachingNotice?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    var title by remember(existingNotice?.id) {
        mutableStateOf(
            existingNotice?.title ?: ""
        )
    }

    var message by remember(existingNotice?.id) {
        mutableStateOf(
            existingNotice?.message ?: ""
        )
    }

    var noticeDate by remember(existingNotice?.id) {
        mutableStateOf(
            existingNotice?.noticeDate
                ?: AttendanceStatusStore.todayDate()
        )
    }

    var expiryDate by remember(existingNotice?.id) {
        mutableStateOf(
            existingNotice?.expiryDate ?: ""
        )
    }

    var selectedBatchId by remember(existingNotice?.id) {
        mutableStateOf(
            existingNotice?.batchId ?: ""
        )
    }

    var batchExpanded by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val batches =
        remember(coachingId) {
            BatchStore.findActiveBatchesByCoaching(
                coachingId
            )
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text =
                        if (existingNotice == null) {
                            "Create Notice"
                        } else {
                            "Edit Notice"
                        },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Notice Title")
                        },
                        placeholder = {
                            Text("Enter notice title")
                        }
                    )
                }

                item {

                    OutlinedTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Notice Message")
                        },
                        placeholder = {
                            Text("Write your notice here...")
                        },
                        minLines = 4,
                        maxLines = 8
                    )
                }

                item {

                    Text(
                        text = "Notice For",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = batchExpanded,
                        onExpandedChange = {
                            batchExpanded = !batchExpanded
                        }
                    ) {

                        val selectedText =
                            if (selectedBatchId.isBlank()) {
                                "Entire Coaching"
                            } else {
                                batches
                                    .find {
                                        it.id == selectedBatchId
                                    }
                                    ?.name
                                    ?: "Specific Batch"
                            }

                        OutlinedTextField(
                            value = selectedText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            leadingIcon = {

                                Icon(
                                    imageVector =
                                        if (
                                            selectedBatchId.isBlank()
                                        ) {
                                            Icons.Default.School
                                        } else {
                                            Icons.Default.Groups
                                        },
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = batchExpanded
                                )
                            },
                            label = {
                                Text("Audience")
                            }
                        )

                        DropdownMenu(
                            expanded = batchExpanded,
                            onDismissRequest = {
                                batchExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Entire Coaching")
                                },
                                onClick = {
                                    selectedBatchId = ""
                                    batchExpanded = false
                                }
                            )

                            batches.forEach { batch ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Batch: ${batch.name}"
                                        )
                                    },
                                    onClick = {
                                        selectedBatchId =
                                            batch.id
                                        batchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {

                    OutlinedTextField(
                        value = noticeDate,
                        onValueChange = {
                            noticeDate = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Notice Date")
                        },
                        leadingIcon = {

                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null
                            )
                        },
                        placeholder = {
                            Text("YYYY-MM-DD")
                        },
                        singleLine = true
                    )
                }

                item {

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = {
                            expiryDate = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Expiry Date (Optional)")
                        },
                        placeholder = {
                            Text("YYYY-MM-DD")
                        },
                        singleLine = true
                    )
                }

                if (errorMessage.isNotBlank()) {

                    item {

                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {

            Button(
                onClick = {

                    when {

                        title.isBlank() -> {
                            errorMessage =
                                "Please enter notice title."
                        }

                        message.isBlank() -> {
                            errorMessage =
                                "Please enter notice message."
                        }

                        noticeDate.isBlank() -> {
                            errorMessage =
                                "Please enter notice date."
                        }

                        else -> {

                            if (existingNotice == null) {

                                NoticeStore.createNotice(
                                    coachingId = coachingId,
                                    batchId = selectedBatchId,
                                    title = title.trim(),
                                    message = message.trim(),
                                    noticeDate = noticeDate.trim(),
                                    expiryDate = expiryDate.trim(),
                                    createdById = adminUser.id,
                                    createdByName =
                                        adminUser.displayName
                                )

                            } else {

                                val updatedNotice =
                                    existingNotice.copy(
                                        batchId =
                                            selectedBatchId,
                                        title =
                                            title.trim(),
                                        message =
                                            message.trim(),
                                        noticeDate =
                                            noticeDate.trim(),
                                        expiryDate =
                                            expiryDate.trim()
                                    )

                                NoticeStore.updateNotice(
                                    notice = updatedNotice
                                )
                            }

                            onSaved()
                        }
                    }
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text =
                        if (existingNotice == null) {
                            "Create"
                        } else {
                            "Save Changes"
                        }
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}