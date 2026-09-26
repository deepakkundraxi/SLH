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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchManagementScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var showAddEditDialog by remember {
        mutableStateOf(false)
    }

    var editingBatch by remember {
        mutableStateOf<CoachingBatch?>(null)
    }

    var deleteBatch by remember {
        mutableStateOf<CoachingBatch?>(null)
    }

    var selectedBatch by remember {
        mutableStateOf<CoachingBatch?>(null)
    }

    /*
     * =========================================================
     * BATCH STUDENTS SCREEN
     * =========================================================
     */

    if (selectedBatch != null) {

        BackHandler {
            selectedBatch = null
        }

        BatchStudentListScreen(
            batch = selectedBatch!!,
            coachingId = coachingId,
            onBack = {
                selectedBatch = null
            }
        )

        return
    }

    /*
     * =========================================================
     * MAIN BATCH MANAGEMENT
     * =========================================================
     */

    BackHandler {
        onBack()
    }

    val batches =
        BatchStore.searchBatches(
            coachingId = coachingId,
            query = searchQuery
        )

    val allBatches =
        BatchStore.findBatchesByCoaching(
            coachingId
        )

    val activeCount =
        allBatches.count {
            it.status.equals(
                "ACTIVE",
                ignoreCase = true
            )
        }

    Scaffold(

        modifier = Modifier.fillMaxSize(),

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Batch Management",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${allBatches.size} Batches",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    editingBatch = null
                    showAddEditDialog = true
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,
                    contentDescription =
                        "Add Batch"
                )
            }
        }

    ) { paddingValues ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    SummaryCard(
                        title = "Total",
                        value =
                            allBatches.size.toString(),
                        icon =
                            Icons.Default.Groups,
                        modifier =
                            Modifier.weight(1f),
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer
                    )

                    SummaryCard(
                        title = "Active",
                        value =
                            activeCount.toString(),
                        icon =
                            Icons.Default.CheckCircle,
                        modifier =
                            Modifier.weight(1f),
                        containerColor =
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                OutlinedTextField(

                    value = searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription =
                                null
                        )
                    },

                    label = {
                        Text("Search Batch")
                    },

                    placeholder = {
                        Text(
                            "Name, code, course, subject..."
                        )
                    },

                    shape =
                        RoundedCornerShape(16.dp)
                )
            }

            if (batches.isEmpty()) {

                item {

                    EmptyBatchCard(
                        hasSearch =
                            searchQuery.isNotBlank()
                    )
                }

            } else {

                items(
                    items = batches,
                    key = {
                        it.id
                    }
                ) { batch ->

                    BatchCard(

                        batch = batch,

                        onClick = {
                            selectedBatch = batch
                        },

                        onEdit = {

                            editingBatch = batch
                            showAddEditDialog = true
                        },

                        onDelete = {

                            deleteBatch = batch
                        },

                        onToggleStatus = {

                            val newStatus =
                                if (
                                    batch.status.equals(
                                        "ACTIVE",
                                        ignoreCase = true
                                    )
                                ) {
                                    "INACTIVE"
                                } else {
                                    "ACTIVE"
                                }

                            BatchStore.setBatchStatus(
                                batchId = batch.id,
                                status = newStatus
                            )
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(90.dp)
                )
            }
        }
    }

    /*
     * =========================================================
     * ADD / EDIT
     * =========================================================
     */

    if (showAddEditDialog) {

        BatchAddEditDialog(

            coachingId = coachingId,

            existingBatch =
                editingBatch,

            onDismiss = {

                showAddEditDialog = false
                editingBatch = null
            },

            onSaved = {

                showAddEditDialog = false
                editingBatch = null
            }
        )
    }

    /*
     * =========================================================
     * DELETE
     * =========================================================
     */

    if (deleteBatch != null) {

        val batch =
            deleteBatch!!

        val studentCount =
            StudentStore.getBatchStudentCount(
                coachingId =
                    batch.coachingId,
                batchId =
                    batch.id
            )

        AlertDialog(

            onDismissRequest = {
                deleteBatch = null
            },

            title = {
                Text("Delete Batch?")
            },

            text = {

                if (studentCount > 0) {

                    Column {

                        Text(
                            text =
                                "\"${batch.name}\" mein $studentCount student assigned hain."
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Delete karne par ye students batch se unassign ho jayenge (batch khali)."
                        )
                    }

                } else {

                    Text(
                        text =
                            "Kya aap \"${batch.name}\" delete karna chahte hain?"
                    )
                }
            },

            confirmButton = {

                Button(

                    onClick = {

                        BatchStore.deleteBatch(
                            batch.id
                        )

                        deleteBatch = null
                    }
                ) {

                    Text(
                        if (studentCount > 0) {
                            "Unassign & Delete"
                        } else {
                            "Delete"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteBatch = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}


/*
 * =============================================================
 * SUMMARY CARD
 * =============================================================
 */

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    containerColor: Color
) {

    Card(

        modifier = modifier,

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    containerColor
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier =
                    Modifier.size(28.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = title,
                style =
                    MaterialTheme.typography
                        .labelMedium
            )
        }
    }
}


/*
 * =============================================================
 * BATCH CARD
 * =============================================================
 */

@Composable
private fun BatchCard(
    batch: CoachingBatch,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {

    val studentCount =
        StudentStore.getBatchStudentCount(
            coachingId =
                batch.coachingId,
            batchId =
                batch.id
        )

    val isActive =
        batch.status.equals(
            "ACTIVE",
            ignoreCase = true
        )

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(22.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.surfaceVariant
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(

                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.School,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(28.dp)
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
                            batch.name,
                        fontWeight =
                            FontWeight.Bold,
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    if (
                        batch.code.isNotBlank()
                    ) {

                        Text(
                            text =
                                "Code: ${batch.code}",
                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )
                    }
                }

                StatusChip(
                    active = isActive
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                if (
                    batch.course.isNotBlank()
                ) {

                    InfoChip(
                        text =
                            batch.course,
                        modifier =
                            Modifier.weight(1f),
                        background =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                if (
                    batch.subject.isNotBlank()
                ) {

                    InfoChip(
                        text =
                            batch.subject,
                        modifier =
                            Modifier.weight(1f),
                        background =
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClick()
                        },

                shape =
                    RoundedCornerShape(14.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Groups,
                        contentDescription =
                            null
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
                                "$studentCount Students",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Tap to view students",
                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )
                    }

                    Text(
                        text = "View →",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            if (
                batch.timing.isNotBlank() ||
                batch.room.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    if (
                        batch.timing.isNotBlank()
                    ) {

                        InfoChip(
                            text =
                                batch.timing,
                            modifier =
                                Modifier.weight(1f),
                            background =
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    }

                    if (
                        batch.room.isNotBlank()
                    ) {

                        InfoChip(
                            text =
                                "Room ${batch.room}",
                            modifier =
                                Modifier.weight(1f),
                            background =
                                MaterialTheme.colorScheme.errorContainer
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onToggleStatus
                ) {

                    Icon(
                        imageVector =
                            if (isActive)
                                Icons.Default.ToggleOn
                            else
                                Icons.Default.ToggleOff,
                        contentDescription =
                            "Toggle Status"
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,
                        contentDescription =
                            "Edit"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            "Delete"
                    )
                }
            }
        }
    }
}


/*
 * =============================================================
 * STATUS CHIP
 * =============================================================
 */

@Composable
private fun StatusChip(
    active: Boolean
) {

    Box(

        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(50)
                )
                .background(
                    if (active)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                )
    ) {

        Text(
            text =
                if (active)
                    "ACTIVE"
                else
                    "INACTIVE",

            fontWeight =
                FontWeight.Bold,

            style =
                MaterialTheme.typography
                    .labelSmall
        )
    }
}


/*
 * =============================================================
 * INFO CHIP
 * =============================================================
 */

@Composable
private fun InfoChip(
    text: String,
    modifier: Modifier,
    background: Color
) {

    Box(

        modifier =
            modifier
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(background)
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                )
    ) {

        Text(
            text = text,
            maxLines = 2,
            overflow =
                TextOverflow.Ellipsis,
            style =
                MaterialTheme.typography
                    .bodySmall
        )
    }
}


/*
 * =============================================================
 * EMPTY BATCH
 * =============================================================
 */

@Composable
private fun EmptyBatchCard(
    hasSearch: Boolean
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(22.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(28.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.Groups,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(52.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    if (hasSearch)
                        "No Batch Found"
                    else
                        "No Batch Available",

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme.typography
                        .titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    if (hasSearch)
                        "Search बदलकर फिर try करें।"
                    else
                        "पहले एक batch create करें।"
            )
        }
    }
}


/*
 * =============================================================
 * ADD / EDIT BATCH DIALOG
 * =============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchAddEditDialog(
    coachingId: String,
    existingBatch: CoachingBatch?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    var name by remember {
        mutableStateOf(
            existingBatch?.name ?: ""
        )
    }

    var code by remember {
        mutableStateOf(
            existingBatch?.code ?: ""
        )
    }

    var course by remember {
        mutableStateOf(
            existingBatch?.course ?: ""
        )
    }

    var subject by remember {
        mutableStateOf(
            existingBatch?.subject ?: ""
        )
    }

    var startDate by remember {
        mutableStateOf(
            existingBatch?.startDate ?: ""
        )
    }

    var endDate by remember {
        mutableStateOf(
            existingBatch?.endDate ?: ""
        )
    }

    var timing by remember {
        mutableStateOf(
            existingBatch?.timing ?: ""
        )
    }

    var room by remember {
        mutableStateOf(
            existingBatch?.room ?: ""
        )
    }

    var description by remember {
        mutableStateOf(
            existingBatch?.description ?: ""
        )
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Card(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),

            shape =
                RoundedCornerShape(24.dp)
        ) {

            Column {

                Box(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF3155D9)
                            )
                            .padding(18.dp)
                ) {

                    Column {

                        Text(
                            text =
                                if (existingBatch == null)
                                    "Create New Batch"
                                else
                                    "Edit Batch",

                            color =
                                Color.White,

                            fontWeight =
                                FontWeight.Bold,

                            style =
                                MaterialTheme.typography
                                    .titleLarge
                        )

                        Text(
                            text =
                                "Batch की details भरें",

                            color =
                                Color.White,

                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )
                    }
                }

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(580.dp)
                            .padding(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    item {

                        OutlinedTextField(

                            value = name,

                            onValueChange = {
                                name = it
                                errorMessage = ""
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            label = {
                                Text("Batch Name *")
                            },

                            singleLine = true
                        )
                    }

                    item {

                        OutlinedTextField(

                            value = code,

                            onValueChange = {
                                code = it
                                errorMessage = ""
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            label = {
                                Text("Batch Code")
                            },

                            singleLine = true
                        )
                    }

                    item {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedTextField(

                                value = course,

                                onValueChange = {
                                    course = it
                                },

                                modifier =
                                    Modifier.weight(1f),

                                label = {
                                    Text("Course")
                                },

                                singleLine = true
                            )

                            OutlinedTextField(

                                value = subject,

                                onValueChange = {
                                    subject = it
                                },

                                modifier =
                                    Modifier.weight(1f),

                                label = {
                                    Text("Subject")
                                },

                                singleLine = true
                            )
                        }
                    }

                    item {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedTextField(

                                value = startDate,

                                onValueChange = {
                                    startDate = it
                                },

                                modifier =
                                    Modifier.weight(1f),

                                label = {
                                    Text("Start Date")
                                },

                                placeholder = {
                                    Text("dd-MM-yyyy")
                                },

                                singleLine = true
                            )

                            OutlinedTextField(

                                value = endDate,

                                onValueChange = {
                                    endDate = it
                                },

                                modifier =
                                    Modifier.weight(1f),

                                label = {
                                    Text("End Date")
                                },

                                placeholder = {
                                    Text("dd-MM-yyyy")
                                },

                                singleLine = true
                            )
                        }
                    }

                    item {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedTextField(

                                value = timing,

                                onValueChange = {
                                    timing = it
                                },

                                modifier =
                                    Modifier.weight(1f),

                                label = {
                                    Text("Timing")
                                },

                                placeholder = {
                                    Text(
                                        "5:00 PM - 7:00 PM"
                                    )
                                },

                                singleLine = true
                            )

                            OutlinedTextField(

                                value = room,

                                onValueChange = {
                                    room = it
                                },

                                modifier =
                                    Modifier.weight(1f),

                                label = {
                                    Text("Room")
                                },

                                singleLine = true
                            )
                        }
                    }

                    item {

                        OutlinedTextField(

                            value = description,

                            onValueChange = {
                                description = it
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            label = {
                                Text("Description")
                            },

                            minLines = 3,

                            maxLines = 5
                        )
                    }

                    if (
                        errorMessage.isNotBlank()
                    ) {

                        item {

                            Card(

                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                MaterialTheme.colorScheme.errorContainer
                                        ),

                                shape =
                                    RoundedCornerShape(12.dp)
                            ) {

                                Text(
                                    text =
                                        errorMessage,

                                    modifier =
                                        Modifier
                                            .padding(12.dp),

                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.End
                        ) {

                            TextButton(
                                onClick =
                                    onDismiss
                            ) {

                                Text("Cancel")
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Button(

                                onClick = {

                                    val cleanName =
                                        name.trim()

                                    val cleanCode =
                                        code.trim()

                                    if (
                                        cleanName.isBlank()
                                    ) {

                                        errorMessage =
                                            "Please enter batch name."

                                        return@Button
                                    }

                                    val duplicateCode =
                                        if (
                                            cleanCode.isBlank()
                                        ) {

                                            false

                                        } else {

                                            BatchStore
                                                .findBatchesByCoaching(
                                                    coachingId
                                                )
                                                .any {

                                                    it.id !=
                                                            (
                                                                    existingBatch
                                                                        ?.id
                                                                        ?: ""
                                                                    ) &&

                                                            it.code.equals(
                                                                cleanCode,
                                                                ignoreCase = true
                                                            )
                                                }
                                        }

                                    if (
                                        duplicateCode
                                    ) {

                                        errorMessage =
                                            "This batch code already exists."

                                        return@Button
                                    }

                                    if (
                                        existingBatch == null
                                    ) {

                                        val batch =
                                            CoachingBatch(

                                                id =
                                                    generateBatchId(),

                                                coachingId =
                                                    coachingId,

                                                name =
                                                    cleanName,

                                                code =
                                                    cleanCode,

                                                course =
                                                    course.trim(),

                                                subject =
                                                    subject.trim(),

                                                teacherId =
                                                    "",

                                                startDate =
                                                    startDate.trim(),

                                                endDate =
                                                    endDate.trim(),

                                                timing =
                                                    timing.trim(),

                                                room =
                                                    room.trim(),

                                                description =
                                                    description.trim(),

                                                status =
                                                    "ACTIVE"
                                            )

                                        val added =
                                            BatchStore.addBatch(
                                                batch
                                            )

                                        if (!added) {

                                            errorMessage =
                                                "Batch could not be created."

                                        } else {

                                            onSaved()
                                        }

                                    } else {

                                        val updated =
                                            existingBatch.copy(

                                                name =
                                                    cleanName,

                                                code =
                                                    cleanCode,

                                                course =
                                                    course.trim(),

                                                subject =
                                                    subject.trim(),

                                                startDate =
                                                    startDate.trim(),

                                                endDate =
                                                    endDate.trim(),

                                                timing =
                                                    timing.trim(),

                                                room =
                                                    room.trim(),

                                                description =
                                                    description.trim()
                                            )

                                        val success =
                                            BatchStore.updateBatch(
                                                updated
                                            )

                                        if (!success) {

                                            errorMessage =
                                                "Batch could not be updated."

                                        } else {

                                            onSaved()
                                        }
                                    }
                                }
                            ) {

                                Text(
                                    if (
                                        existingBatch == null
                                    )
                                        "Create"
                                    else
                                        "Save"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/*
 * =============================================================
 * BATCH STUDENT LIST
 * =============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchStudentListScreen(
    batch: CoachingBatch,
    coachingId: String,
    onBack: () -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var transferStudent by remember {
        mutableStateOf<Student?>(null)
    }

    val students =
        StudentStore.searchStudentsByBatch(
            coachingId = coachingId,
            batchId = batch.id,
            query = searchQuery
        )

    val allBatches =
        BatchStore.findBatchesByCoaching(
            coachingId
        ).filter {
            it.id != batch.id
        }

    BackHandler {
        onBack()
    }

    Scaffold(

        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                batch.name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "${students.size} Students",
                            style =
                                MaterialTheme.typography
                                    .labelSmall
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

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(20.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.tertiaryContainer
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "Batch Students",
                            fontWeight =
                                FontWeight.Bold,
                            style =
                                MaterialTheme.typography
                                    .titleMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                buildBatchSummary(batch)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                OutlinedTextField(

                    value = searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription =
                                null
                        )
                    },

                    label = {
                        Text("Search Student")
                    },

                    placeholder = {
                        Text(
                            "Name, student ID, mobile..."
                        )
                    },

                    shape =
                        RoundedCornerShape(16.dp)
                )
            }

            if (students.isEmpty()) {

                item {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(20.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Groups,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(48.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    if (
                                        searchQuery.isBlank()
                                    )
                                        "No Students in this Batch"
                                    else
                                        "No Student Found",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

            } else {

                items(
                    items = students,
                    key = {
                        it.id
                    }
                ) { student ->

                    BatchStudentCard(

                        student = student,

                        onTransfer = {

                            if (
                                allBatches.isNotEmpty()
                            ) {

                                transferStudent =
                                    student
                            }
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(30.dp)
                )
            }
        }
    }

    if (transferStudent != null) {

        StudentBatchTransferDialog(

            student =
                transferStudent!!,

            currentBatch =
                batch,

            availableBatches =
                allBatches,

            onDismiss = {
                transferStudent = null
            },

            onTransferred = {
                transferStudent = null
            }
        )
    }
}


/*
 * =============================================================
 * STUDENT CARD
 * =============================================================
 */

@Composable
private fun BatchStudentCard(
    student: Student,
    onTransfer: () -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
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
                        .size(46.dp)
                        .clip(
                            RoundedCornerShape(14.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.School,
                    contentDescription =
                        null
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
                        student.name,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text =
                        "ID: ${student.studentId}",
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )

                if (
                    student.course.isNotBlank()
                ) {

                    Text(
                        text =
                            student.course,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }

                if (
                    student.mobile.isNotBlank()
                ) {

                    Text(
                        text =
                            student.mobile,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }
            }

            IconButton(
                onClick = onTransfer
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SwapHoriz,
                    contentDescription =
                        "Transfer Student"
                )
            }
        }
    }
}


/*
 * =============================================================
 * TRANSFER DIALOG
 * =============================================================
 */

@Composable
private fun StudentBatchTransferDialog(
    student: Student,
    currentBatch: CoachingBatch,
    availableBatches: List<CoachingBatch>,
    onDismiss: () -> Unit,
    onTransferred: () -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    var selectedBatchId by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val selectedBatch =
        availableBatches.firstOrNull {
            it.id == selectedBatchId
        }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Column {

                Text(
                    text =
                        "Transfer Student",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        student.name,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        },

        text = {

            Column {

                Card(

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.secondaryContainer
                        ),

                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(12.dp)
                    ) {

                        Text(
                            text =
                                "Current Batch",
                            style =
                                MaterialTheme.typography
                                    .labelSmall
                        )

                        Text(
                            text =
                                currentBatch.name,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Box {

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = true
                                },

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiaryContainer
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {

                            Text(
                                text =
                                    "New Batch",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall
                            )

                            Text(
                                text =
                                    selectedBatch?.name
                                        ?: "Select Batch",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(

                        expanded = expanded,

                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        availableBatches.forEach { batch ->

                            DropdownMenuItem(

                                text = {

                                    Column {

                                        Text(
                                            text =
                                                batch.name,
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        if (
                                            batch.code.isNotBlank()
                                        ) {

                                            Text(
                                                text =
                                                    batch.code,
                                                style =
                                                    MaterialTheme.typography
                                                        .labelSmall
                                            )
                                        }
                                    }
                                },

                                onClick = {

                                    selectedBatchId =
                                        batch.id

                                    expanded = false

                                    errorMessage = ""
                                }
                            )
                        }
                    }
                }

                if (
                    errorMessage.isNotBlank()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            errorMessage,
                        color =
                            MaterialTheme.colorScheme
                                .error
                    )
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    if (
                        selectedBatchId.isBlank()
                    ) {

                        errorMessage =
                            "Please select a new batch."

                        return@Button
                    }

                    if (
                        selectedBatchId ==
                        currentBatch.id
                    ) {

                        errorMessage =
                            "Please select a different batch."

                        return@Button
                    }

                    val success =
                        StudentStore.moveStudentToBatch(
                            studentId =
                                student.id,
                            batchId =
                                selectedBatchId
                        )

                    if (!success) {

                        errorMessage =
                            "Student transfer failed."

                    } else {

                        onTransferred()
                    }
                }
            ) {

                Text("Transfer")
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


/*
 * =============================================================
 * HELPERS
 * =============================================================
 */

private fun generateBatchId(): String {

    return "BATCH_" +
            System.currentTimeMillis()
}


private fun buildBatchSummary(
    batch: CoachingBatch
): String {

    val parts =
        mutableListOf<String>()

    if (
        batch.course.isNotBlank()
    ) {
        parts.add(batch.course)
    }

    if (
        batch.subject.isNotBlank()
    ) {
        parts.add(batch.subject)
    }

    if (
        batch.timing.isNotBlank()
    ) {
        parts.add(batch.timing)
    }

    return if (
        parts.isEmpty()
    ) {
        "Batch details"
    } else {
        parts.joinToString(" • ")
    }
}