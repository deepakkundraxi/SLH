@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.slh.app
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


/*
 * =================================================================
 * TEACHER <-> LOGIN ACCOUNT SYNC
 *
 * Teacher records (name, subject, mobile, etc.) live in TeacherStore.
 * Actual login is validated against UserAccountStore (see
 * LoginScreen.kt). These are two separate stores, so a Teacher
 * saved here must be mirrored into UserAccountStore or the teacher
 * will never be able to log in.
 *
 * The DemoUser id is derived from the Teacher id ("U" + teacher.id)
 * so add/update/delete can always find the matching account.
 * =================================================================
 */

private fun teacherAccountId(
    teacherId: String
): String {

    return "U$teacherId"
}

private fun syncTeacherLoginAccount(
    teacher: Teacher,
    plainPassword: String? = null,
    context: android.content.Context? = null
) {

    val accountId =
        teacherAccountId(
            teacher.id
        )

    if (
        teacher.username.isBlank()
    ) {

        // No username set (or cleared) — remove any login access.
        UserAccountStore.delete(
            accountId
        )

        return
    }

    val demoUser =
        DemoUser(
            id = accountId,
            username = teacher.username,
            password = teacher.password,
            role = UserRole.TEACHER,
            status =
                if (
                    teacher.accountStatus
                        .equals(
                            "INACTIVE",
                            ignoreCase = true
                        )
                ) {
                    AccountStatus.SUSPENDED
                } else {
                    AccountStatus.APPROVED
                },
            coachingId = teacher.coachingId,
            displayName = teacher.name
        )

    val existingAccount =
        UserAccountStore.getById(
            accountId
        )

    /*
     * A teacher record that came from the cloud has no password
     * hash (Firestore never stores it). Never overwrite an existing
     * local login with a blank password, and never create a local
     * login without one - the cloud login covers that case.
     */
    if (existingAccount == null) {

        if (teacher.password.isNotBlank()) {

            UserAccountStore.add(
                demoUser
            )
        }

    } else {

        UserAccountStore.update(
            if (teacher.password.isBlank()) {
                demoUser.copy(
                    password =
                        existingAccount.password
                )
            } else {
                demoUser
            }
        )
    }

    /*
     * The plain password is only known while the admin is typing
     * it. Use it to create the cloud login so this teacher can
     * also sign in from their own phone.
     */
    if (
        !plainPassword.isNullOrBlank() &&
        context != null
    ) {

        AccountCloudSync.provision(
            context,
            demoUser,
            plainPassword
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManagementScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var showForm by remember {
        mutableStateOf(false)
    }

    var editingTeacher by remember {
        mutableStateOf<Teacher?>(null)
    }

    var selectedTeacher by remember {
        mutableStateOf<Teacher?>(null)
    }

    var deleteTeacher by remember {
        mutableStateOf<Teacher?>(null)
    }

    BackHandler {
        when {
            selectedTeacher != null -> {
                selectedTeacher = null
            }

            showForm -> {
                showForm = false
                editingTeacher = null
            }

            else -> {
                onBack()
            }
        }
    }

    if (selectedTeacher != null) {

        TeacherProfileScreen(
            teacher = selectedTeacher!!,
            coachingId = coachingId,
            onBack = {
                selectedTeacher = null
            },
            onEdit = {
                editingTeacher = selectedTeacher
                selectedTeacher = null
                showForm = true
            }
        )

        return
    }

    if (showForm) {

        TeacherFormScreen(
            coachingId = coachingId,
            teacher = editingTeacher,
            onBack = {
                showForm = false
                editingTeacher = null
            },
            onSaved = {
                showForm = false
                editingTeacher = null
            }
        )

        return
    }

    val teachers =
        TeacherStore.searchTeachers(
            coachingId = coachingId,
            query = searchQuery
        )

    val totalTeachers =
        TeacherStore.getTeacherCount(
            coachingId
        )

    val activeTeachers =
        TeacherStore.getActiveTeacherCount(
            coachingId
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Teacher Management",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Manage your teaching team",
                            style = MaterialTheme.typography.labelSmall
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
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                TeacherSummaryHeader(
                    totalTeachers = totalTeachers,
                    activeTeachers = activeTeachers
                )
            }

            item {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    placeholder = {
                        Text(
                            "Search teacher, mobile or subject"
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {

                Button(
                    onClick = {
                        editingTeacher = null
                        showForm = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00AFA9)
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "Add New Teacher",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (teachers.isEmpty()) {

                item {

                    EmptyTeacherCard()
                }

            } else {

                items(
                    items = teachers,
                    key = {
                        it.id
                    }
                ) { teacher ->

                    TeacherCard(
                        teacher = teacher,
                        coachingId = coachingId,
                        onClick = {
                            selectedTeacher = teacher
                        },
                        onEdit = {
                            editingTeacher = teacher
                            showForm = true
                        },
                        onDelete = {
                            deleteTeacher = teacher
                        }
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }

    deleteTeacher?.let { teacher ->

        AlertDialog(
            onDismissRequest = {
                deleteTeacher = null
            },
            title = {
                Text(
                    "Delete Teacher?"
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete ${teacher.name}?"
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        TeacherStore.deleteTeacher(
                            teacher.id
                        )

                        UserAccountStore.delete(
                            teacherAccountId(
                                teacher.id
                            )
                        )

                        deleteTeacher = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                Button(
                    onClick = {
                        deleteTeacher = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun TeacherSummaryHeader(
    totalTeachers: Int,
    activeTeachers: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF3155D9),
                            Color(0xFF673AB7)
                        )
                    )
                )
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Teaching Team",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Keep your teachers, subjects and batches organized.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "$totalTeachers",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Teachers",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )

                    Text(
                        text = "$activeTeachers Active",
                        color = Color(0xFFB9F6CA),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun TeacherCard(
    teacher: Teacher,
    coachingId: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val assignedBatches =
        BatchStore.findBatchesByCoaching(
            coachingId
        ).filter {
            it.teacherId == teacher.id
        }

    val cardBrush =
        if (
            teacher.accountStatus.equals(
                "ACTIVE",
                ignoreCase = true
            )
        ) {
            Brush.linearGradient(
                listOf(
                    Color(0xFFE8F5E9),
                    Color(0xFFE3F2FD)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFF3E0),
                    Color(0xFFFFEBEE)
                )
            )
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(16.dp)
        ) {

            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF3155D9)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = teacher.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                if (teacher.subject.isBlank())
                                    "Subject not assigned"
                                else
                                    teacher.subject,
                            color = Color(0xFF3155D9),
                            fontWeight = FontWeight.SemiBold
                        )

                        if (teacher.mobile.isNotBlank()) {

                            Text(
                                text = teacher.mobile,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    StatusPill(
                        active =
                            teacher.accountStatus.equals(
                                "ACTIVE",
                                ignoreCase = true
                            )
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    InfoPill(
                        text =
                            if (assignedBatches.isEmpty())
                                "No Batch"
                            else
                                "${assignedBatches.size} Batch"
                    )

                    if (teacher.qualification.isNotBlank()) {

                        InfoPill(
                            text = teacher.qualification
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    IconButton(
                        onClick = onEdit
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF3155D9)
                        )
                    }

                    IconButton(
                        onClick = onDelete
                    ) {

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun StatusPill(
    active: Boolean
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (active)
                    Color(0xFF2E7D32)
                else
                    Color(0xFFE65100)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text =
                if (active)
                    "ACTIVE"
                else
                    "INACTIVE",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun InfoPill(
    text: String
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Color.White.copy(alpha = 0.8f)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun EmptyTeacherCard() {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "👨‍🏫",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "No teachers found",
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Add your first teacher to get started.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherFormScreen(
    coachingId: String,
    teacher: Teacher?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {

    val accountContext =
        LocalContext.current

    var name by remember {
        mutableStateOf(teacher?.name ?: "")
    }

    var mobile by remember {
        mutableStateOf(teacher?.mobile ?: "")
    }

    var email by remember {
        mutableStateOf(teacher?.email ?: "")
    }

    var address by remember {
        mutableStateOf(teacher?.address ?: "")
    }

    var subject by remember {
        mutableStateOf(teacher?.subject ?: "")
    }

    var qualification by remember {
        mutableStateOf(teacher?.qualification ?: "")
    }

    var username by remember {
        mutableStateOf(teacher?.username ?: "")
    }

    var password by remember {
        mutableStateOf(teacher?.password ?: "")
    }

    var joiningDate by remember {
        mutableStateOf(teacher?.joiningDate ?: "")
    }

    var status by remember {
        mutableStateOf(
            teacher?.accountStatus ?: "ACTIVE"
        )
    }

    var selectedBatchId by remember {

        mutableStateOf(
            BatchStore.findBatchesByCoaching(
                coachingId
            ).firstOrNull {
                it.teacherId == teacher?.id
            }?.id ?: ""
        )
    }

    var statusExpanded by remember {
        mutableStateOf(false)
    }

    var batchExpanded by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val batches =
        BatchStore.findActiveBatchesByCoaching(
            coachingId
        )

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        if (teacher == null)
                            "Add Teacher"
                        else
                            "Edit Teacher",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00AFA9),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                FormSectionCard(
                    title = "👨‍🏫 Basic Information",
                    description = "Teacher personal details"
                ) {

                    TeacherField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = "Teacher Name *"
                    )

                    TeacherField(
                        value = mobile,
                        onValueChange = {
                            mobile = it
                        },
                        label = "Mobile Number"
                    )

                    TeacherField(
                        value = email,
                        onValueChange = {
                            email = it
                        },
                        label = "Email"
                    )

                    TeacherField(
                        value = address,
                        onValueChange = {
                            address = it
                        },
                        label = "Address",
                        singleLine = false
                    )
                }
            }

            item {

                FormSectionCard(
                    title = "📚 Teaching Details",
                    description = "Subject and qualification"
                ) {

                    TeacherField(
                        value = subject,
                        onValueChange = {
                            subject = it
                        },
                        label = "Subject"
                    )

                    TeacherField(
                        value = qualification,
                        onValueChange = {
                            qualification = it
                        },
                        label = "Qualification"
                    )

                    TeacherField(
                        value = joiningDate,
                        onValueChange = {
                            joiningDate = it
                        },
                        label = "Joining Date"
                    )
                }
            }

            item {

                FormSectionCard(
                    title = "🎓 Batch Assignment",
                    description = "Assign teacher to a batch"
                ) {

                    ExposedDropdownMenuBox(
                        expanded = batchExpanded,
                        onExpandedChange = {
                            batchExpanded = !batchExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value =
                                batches.firstOrNull {
                                    it.id == selectedBatchId
                                }?.name
                                    ?: "Select Batch",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = {
                                Text("Batch")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = batchExpanded
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = batchExpanded,
                            onDismissRequest = {
                                batchExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("No Batch")
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
                                            "${batch.name} • ${batch.course}"
                                        )
                                    },
                                    onClick = {
                                        selectedBatchId = batch.id
                                        batchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {

                FormSectionCard(
                    title = "🔐 Login & Account",
                    description = "Teacher login credentials"
                ) {

                    TeacherField(
                        value = username,
                        onValueChange = {
                            username = it
                        },
                        label = "Username"
                    )

                    TeacherField(
                        value = password,
                        onValueChange = {
                            password = it
                        },
                        label = "Password"
                    )

                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = {
                            statusExpanded = !statusExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = {
                                Text("Account Status")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = statusExpanded
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = {
                                statusExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("ACTIVE")
                                },
                                onClick = {
                                    status = "ACTIVE"
                                    statusExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("INACTIVE")
                                },
                                onClick = {
                                    status = "INACTIVE"
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (errorMessage.isNotBlank()) {

                item {

                    Text(
                        text = errorMessage,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 4.dp
                        )
                    )
                }
            }

            item {

                Button(
                    onClick = {

                        if (name.isBlank()) {
                            errorMessage =
                                "Please enter teacher name."
                            return@Button
                        }

                        if (teacher == null) {

                            val newTeacher =
                                Teacher(
                                    id =
                                        "T${System.currentTimeMillis()}",
                                    coachingId = coachingId,
                                    name = name.trim(),
                                    mobile = mobile.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    subject = subject.trim(),
                                    qualification = qualification.trim(),
                                    username = username.trim(),
                                    password = PasswordHasher.hash(password),
                                    joiningDate = joiningDate.trim(),
                                    accountStatus = status
                                )

                            val added =
                                TeacherStore.addTeacher(
                                    newTeacher
                                )

                            if (!added) {

                                errorMessage =
                                    "Teacher could not be added. Username may already exist."

                                return@Button
                            }

                            syncTeacherLoginAccount(
                                newTeacher,
                                plainPassword = password,
                                context = accountContext
                            )

                            if (
                                selectedBatchId.isNotBlank()
                            ) {

                                BatchStore.findBatch(
                                    selectedBatchId
                                )?.let { batch ->

                                    BatchStore.updateBatch(
                                        batch.copy(
                                            teacherId =
                                                newTeacher.id
                                        )
                                    )
                                }
                            }

                        } else {

                            val updatedTeacher =
                                teacher.copy(
                                    coachingId = coachingId,
                                    name = name.trim(),
                                    mobile = mobile.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    subject = subject.trim(),
                                    qualification = qualification.trim(),
                                    username = username.trim(),
                                    password =
                                        if (
                                            password ==
                                            teacher.password
                                        ) {
                                            teacher.password
                                        } else {
                                            PasswordHasher.hash(
                                                password
                                            )
                                        },
                                    joiningDate = joiningDate.trim(),
                                    accountStatus = status
                                )

                            val updated =
                                TeacherStore.updateTeacher(
                                    updatedTeacher
                                )

                            if (!updated) {

                                errorMessage =
                                    "Teacher could not be updated. Username may already exist."

                                return@Button
                            }

                            syncTeacherLoginAccount(
                                updatedTeacher,
                                plainPassword =
                                    if (
                                        password !=
                                        teacher.password
                                    ) {
                                        password
                                    } else {
                                        null
                                    },
                                context = accountContext
                            )

                            BatchStore.findBatchesByCoaching(
                                coachingId
                            )
                                .filter {
                                    it.teacherId == teacher.id
                                }
                                .forEach { batch ->

                                    BatchStore.updateBatch(
                                        batch.copy(
                                            teacherId = ""
                                        )
                                    )
                                }

                            if (
                                selectedBatchId.isNotBlank()
                            ) {

                                BatchStore.findBatch(
                                    selectedBatchId
                                )?.let { batch ->

                                    BatchStore.updateBatch(
                                        batch.copy(
                                            teacherId =
                                                teacher.id
                                        )
                                    )
                                }
                            }
                        }

                        onSaved()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3155D9)
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            if (teacher == null)
                                "Save Teacher"
                            else
                                "Update Teacher",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }
        }
    }
}


@Composable
private fun TeacherProfileScreen(
    teacher: Teacher,
    coachingId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {

    val assignedBatches =
        BatchStore.findBatchesByCoaching(
            coachingId
        ).filter {
            it.teacherId == teacher.id
        }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        "Teacher Profile",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF673AB7),
                                        Color(0xFF3155D9),
                                        Color(0xFF00AFA9)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(82.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF3155D9),
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = teacher.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Text(
                                text =
                                    if (teacher.subject.isBlank())
                                        "Teacher"
                                    else
                                        teacher.subject,
                                color = Color.White.copy(
                                    alpha = 0.9f
                                )
                            )
                        }
                    }
                }
            }

            item {

                ProfileInfoCard(
                    title = "Personal Information",
                    values = listOf(
                        "Mobile" to teacher.mobile,
                        "Email" to teacher.email,
                        "Address" to teacher.address,
                        "Joining Date" to teacher.joiningDate
                    )
                )
            }

            item {

                ProfileInfoCard(
                    title = "Teaching Information",
                    values = listOf(
                        "Subject" to teacher.subject,
                        "Qualification" to teacher.qualification
                    )
                )
            }

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "🎓 Assigned Batches",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        if (assignedBatches.isEmpty()) {

                            Text(
                                text = "No batch assigned.",
                                style = MaterialTheme.typography.bodySmall
                            )

                        } else {

                            assignedBatches.forEach { batch ->

                                Text(
                                    text =
                                        "• ${batch.name}" +
                                                if (
                                                    batch.course.isNotBlank()
                                                )
                                                    " — ${batch.course}"
                                                else
                                                    "",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8EAF6)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "🔐 Login Information",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Username: ${teacher.username.ifBlank { "Not set" }}"
                        )

                        Text(
                            text =
                                "Account: ${teacher.accountStatus}"
                        )
                    }
                }
            }

            item {

                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3155D9)
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        "Edit Teacher",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun ProfileInfoCard(
    title: String,
    values: List<Pair<String, String>>
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7FF)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            values.forEach { (label, value) ->

                if (value.isNotBlank()) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 5.dp
                            )
                    ) {

                        Text(
                            text = "$label:",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(105.dp)
                        )

                        Text(
                            text = value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun FormSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            content()
        }
    }
}


@Composable
private fun TeacherField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        singleLine = singleLine,
        minLines =
            if (singleLine) 1 else 3,
        shape = RoundedCornerShape(14.dp)
    )
}