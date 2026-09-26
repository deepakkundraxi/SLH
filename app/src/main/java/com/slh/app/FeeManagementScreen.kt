package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// MAIN FEE MANAGEMENT SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeManagementScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    var searchText by remember {
        mutableStateOf("")
    }

    // all | pending | overdue
    var feeFilter by remember {
        mutableStateOf("all")
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var showFeeDialog by remember {
        mutableStateOf(false)
    }

    var selectedInstallment by remember {
        mutableStateOf<FeeInstallment?>(null)
    }

    var showPaymentDialog by remember {
        mutableStateOf(false)
    }

    var deleteStudent by remember {
        mutableStateOf<Student?>(null)
    }

    // Which student card is expanded (shows installments + Add Payment)
    var expandedStudentId by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(refreshKey) {
        FeeStore.refreshStatuses()
    }

    val students =
        StudentStore
            .findStudentsByCoaching(
                coachingId
            )
            .filter { student ->

                val matchesSearch =
                    if (searchText.isBlank()) {
                        true
                    } else {
                        student.name.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                                student.studentId.contains(
                                    searchText,
                                    ignoreCase = true
                                ) ||
                                student.mobile.contains(
                                    searchText,
                                    ignoreCase = true
                                )
                    }

                if (!matchesSearch) {
                    return@filter false
                }

                when (feeFilter) {
                    "pending" -> {
                        val summary =
                            FeeStore.getSummary(
                                studentId = student.id,
                                coachingId = coachingId
                            )
                        summary != null &&
                                summary.remaining > 0.0 &&
                                summary.installments.none {
                                    it.status == FeeInstallmentStatus.OVERDUE
                                }
                    }
                    "overdue" -> {
                        val summary =
                            FeeStore.getSummary(
                                studentId = student.id,
                                coachingId = coachingId
                            )
                        summary != null &&
                                summary.installments.any {
                                    it.status == FeeInstallmentStatus.OVERDUE
                                }
                    }
                    else -> true
                }
            }

    val totalFee =
        FeeStore.getTotalFee(
            coachingId
        )

    val totalCollected =
        FeeStore.getTotalCollected(
            coachingId
        )

    val totalRemaining =
        FeeStore.getTotalRemaining(
            coachingId
        )

    val overdueCount =
        FeeStore.getOverdueStudentCount(
            coachingId
        )

    val pendingCount =
        FeeStore.getPendingInstallmentCount(
            coachingId
        )

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "Fee Management",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Fees & payments",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                },

                navigationIcon = {

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
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(

                            containerColor =
                                MaterialTheme.colorScheme.surface,

                            titleContentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,

                            navigationIconContentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal = 12.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                OutlinedTextField(

                    value =
                        searchText,

                    onValueChange = {

                        searchText =
                            it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                "Search"
                        )
                    },

                    label = {

                        Text(
                            "Search Student"
                        )
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "all" to "All",
                        "pending" to "Pending",
                        "overdue" to "Overdue"
                    ).forEach { (key, label) ->
                        val selected = feeFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { feeFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {

                FeeSummaryCards(

                    totalFee =
                        totalFee,

                    totalCollected =
                        totalCollected,

                    totalRemaining =
                        totalRemaining,

                    overdueCount =
                        overdueCount,

                    pendingCount =
                        pendingCount
                )
            }

            items(

                items =
                    students,

                key = {
                    it.id
                }

            ) { student ->

                FeeStudentCard(

                    student =
                        student,

                    coachingId =
                        coachingId,

                    expanded =
                        expandedStudentId == student.id,

                    onToggleExpand = {
                        expandedStudentId =
                            if (expandedStudentId == student.id) {
                                null
                            } else {
                                student.id
                            }
                    },

                    onAddFee = {

                        selectedStudent =
                            student

                        showFeeDialog =
                            true
                    },

                    onPayment = { installment ->

                        /*
                         * IMPORTANT:
                         * Select both student and installment.
                         */
                        selectedStudent =
                            student

                        selectedInstallment =
                            installment

                        showPaymentDialog =
                            true
                    },

                    onEdit = {

                        selectedStudent =
                            student

                        showFeeDialog =
                            true
                    },

                    onDelete = {

                        deleteStudent =
                            student
                    }
                )
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            30.dp
                        )
                )
            }
        }
    }

    // ========================================================
    // FEE PLAN DIALOG
    // ========================================================

    if (
        showFeeDialog &&
        selectedStudent != null
    ) {

        FeePlanDialog(

            student =
                selectedStudent!!,

            coachingId =
                coachingId,

            onDismiss = {

                showFeeDialog =
                    false

                selectedStudent =
                    null
            },

            onSaved = {

                showFeeDialog =
                    false

                selectedStudent =
                    null

                refreshKey++
            }
        )
    }

    // ========================================================
    // PAYMENT DIALOG
    // ========================================================

    if (
        showPaymentDialog &&
        selectedInstallment != null &&
        selectedStudent != null
    ) {

        PaymentDialog(

            installment =
                selectedInstallment!!,

            student =
                selectedStudent!!,

            coachingId =
                coachingId,

            onDismiss = {

                showPaymentDialog =
                    false

                selectedInstallment =
                    null

                selectedStudent =
                    null
            },

            onSaved = {

                showPaymentDialog =
                    false

                selectedInstallment =
                    null

                selectedStudent =
                    null

                refreshKey++
            }
        )
    }

    // ========================================================
    // DELETE DIALOG
    // ========================================================

    if (
        deleteStudent != null
    ) {

        AlertDialog(

            onDismissRequest = {

                deleteStudent =
                    null
            },

            title = {

                Text(
                    "Delete Fee Plan?"
                )
            },

            text = {

                Text(
                    "Student fee plan and payment records will be deleted."
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        FeeStore.deleteFeePlan(

                            studentId =
                                deleteStudent!!.id,

                            coachingId =
                                coachingId
                        )

                        deleteStudent =
                            null

                        refreshKey++
                    }
                ) {

                    Text(
                        "Delete"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        deleteStudent =
                            null
                    }
                ) {

                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }
}

// ============================================================
// SUMMARY CARDS
// ============================================================

@Composable
private fun FeeSummaryCards(
    totalFee: Double,
    totalCollected: Double,
    totalRemaining: Double,
    overdueCount: Int,
    pendingCount: Int
) {

    Column(

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            FeeSummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Total Fee",

                value =
                    money(
                        totalFee
                    ),

                icon =
                    Icons.Default.AccountBalance
            )

            FeeSummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Collected",

                value =
                    money(
                        totalCollected
                    ),

                icon =
                    Icons.Default.AttachMoney
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

            FeeSummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Remaining",

                value =
                    money(
                        totalRemaining
                    ),

                icon =
                    Icons.Default.People
            )

            FeeSummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Overdue Students",

                value =
                    overdueCount.toString(),

                icon =
                    Icons.Default.CalendarToday
            )
        }

        Text(

            text =
                "Pending Payments: $pendingCount",

            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun FeeSummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                14.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    12.dp
                )
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(
                        24.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    title
            )

            Text(

                text =
                    value,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

// ============================================================
// STUDENT CARD
// ============================================================

@Composable
private fun FeeStudentCard(
    student: Student,
    coachingId: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddFee: () -> Unit,
    onPayment: (FeeInstallment) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val summary =
        FeeStore.getSummary(

            studentId =
                student.id,

            coachingId =
                coachingId
        )

    val statusLabel =
        when {
            summary == null -> "No fee plan"
            summary.remaining <= 0.0 -> "Fully Paid"
            summary.installments.any {
                it.status == FeeInstallmentStatus.OVERDUE
            } -> "Overdue"
            summary.totalPaid > 0.0 -> "Partially Paid"
            else -> "Pending"
        }

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onToggleExpand()
                },

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    if (expanded) {
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.35f
                        )
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    14.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(

                        text =
                            student.name,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            student.studentId
                    )

                    if (
                        student.mobile.isNotBlank()
                    ) {

                        Text(
                            text =
                                student.mobile
                        )
                    }

                    if (
                        student.course.isNotBlank()
                    ) {

                        Text(

                            text =
                                student.course,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    Text(
                        text = statusLabel,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!expanded) {
                        Text(
                            text = "Tap to view installments & payment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick =
                        onEdit
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Edit,

                        contentDescription =
                            "Edit Fee"
                    )
                }

                IconButton(
                    onClick =
                        onDelete
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Delete Fee"
                    )
                }
            }

            // Detail (installments / create plan) only after student is selected (expanded)
            if (expanded) {

                if (
                    summary == null
                ) {

                    Text(
                        text =
                            "No fee plan created."
                    )

                    Button(
                        onClick =
                            onAddFee
                    ) {

                        Text(
                            "Create Fee Plan"
                        )
                    }

                } else {

                    AdminFeeSummary(
                        summary =
                            summary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(
                        text = "Installments",
                        fontWeight = FontWeight.Bold
                    )

                    summary.installments
                        .forEach { installment ->

                            AdminInstallmentCard(

                                installment =
                                    installment,

                                student =
                                    student,

                                onPayment =
                                    onPayment
                            )
                        }
                }
            }
        }
    }
}

// ============================================================
// ADMIN FEE SUMMARY
// ============================================================

@Composable
private fun AdminFeeSummary(
    summary: StudentFeeSummary
) {

    Column(

        verticalArrangement =
            Arrangement.spacedBy(
                5.dp
            )
    ) {

        Text(

            text =
                "Fee Summary",

            fontWeight =
                FontWeight.Bold
        )

        FeeInfoRow(

            title =
                "Original Fee",

            value =
                money(
                    summary.originalFee
                )
        )

        FeeInfoRow(

            title =
                "Concession",

            value =
                money(
                    summary.concessionAmount
                )
        )

        FeeInfoRow(

            title =
                "Final Payable Fee",

            value =
                money(
                    summary.totalFee
                )
        )

        FeeInfoRow(

            title =
                "Paid",

            value =
                money(
                    summary.totalPaid
                )
        )

        FeeInfoRow(

            title =
                "Remaining",

            value =
                money(
                    summary.remaining
                )
        )

        FeeInfoRow(

            title =
                "Payment Mode",

            value =
                paymentModeText(
                    summary.paymentMode
                )
        )
    }
}

@Composable
private fun FeeInfoRow(
    title: String,
    value: String
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(

            text =
                title,

            modifier =
                Modifier.weight(
                    1f
                )
        )

        Spacer(
            modifier =
                Modifier.width(
                    8.dp
                )
        )

        Text(

            text =
                value,

            fontWeight =
                FontWeight.Bold
        )
    }
}

// ============================================================
// INSTALLMENT CARD
// ============================================================

@Composable
private fun AdminInstallmentCard(
    installment: FeeInstallment,
    student: Student,
    onPayment: (FeeInstallment) -> Unit
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    val remaining =
        (
                installment.amount -
                        installment.paidAmount
                ).coerceAtLeast(
                0.0
            )

    val status =
        getInstallmentDisplayStatus(
            installment
        )

    val title =
        if (
            installment.installmentNumber == 0
        ) {

            "Full Payment"

        } else {

            "Installment ${installment.installmentNumber}"
        }

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                12.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    12.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(

                    text =
                        title,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(

                    text =
                        installmentStatusText(
                            status
                        ),

                    fontWeight =
                        FontWeight.Bold
                )
            }

            FeeInfoRow(

                title =
                    "Amount",

                value =
                    money(
                        installment.amount
                    )
            )

            FeeInfoRow(

                title =
                    "Paid",

                value =
                    money(
                        installment.paidAmount
                    )
            )

            FeeInfoRow(

                title =
                    "Remaining",

                value =
                    money(
                        remaining
                    )
            )

            FeeInfoRow(

                title =
                    "Due Date",

                value =
                    installment.dueDate.ifBlank {
                        "-"
                    }
            )

            if (
                installment.paidDate.isNotBlank()
            ) {

                FeeInfoRow(

                    title =
                        "Paid Date",

                    value =
                        installment.paidDate
                )
            }

            if (
                installment.paymentNote.isNotBlank()
            ) {

                FeeInfoRow(

                    title =
                        "Note",

                    value =
                        installment.paymentNote
                )
            }

            if (
                remaining > 0.0
            ) {

                Button(

                    onClick = {

                        onPayment(
                            installment
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Add Payment"
                    )
                }

                OutlinedButton(
                    onClick = {
                        sendFeeWhatsAppReminder(
                            context = context,
                            student = student,
                            installment = installment,
                            remaining = remaining
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("WhatsApp Remind")
                }
            }
        }
    }
}

private fun sendFeeWhatsAppReminder(
    context: android.content.Context,
    student: Student,
    installment: FeeInstallment,
    remaining: Double
) {
    val label =
        if (installment.installmentNumber == 0) {
            "Full Payment"
        } else {
            "Installment ${installment.installmentNumber}"
        }

    val amountText =
        "₹" + String.format(java.util.Locale.US, "%.2f", remaining)

    val message =
        "Namaste ${student.name},\n\n" +
                "Aapki coaching fee pending hai.\n" +
                "$label\n" +
                "Amount due: $amountText\n" +
                "Due date: ${installment.dueDate.ifBlank { "-" }}\n\n" +
                "Kripya jald fee jama karwa dein. Dhanyavad."

    val phoneDigits =
        student.mobile.filter { it.isDigit() }

    try {
        val intent =
            if (phoneDigits.length >= 10) {
                val withCountry =
                    if (phoneDigits.length == 10) {
                        "91$phoneDigits"
                    } else {
                        phoneDigits
                    }
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://wa.me/$withCountry?text=" +
                                java.net.URLEncoder.encode(message, "UTF-8")
                    )
                )
            } else {
                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, message)
                }
            }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Send fee reminder")
        )
    } catch (_: Exception) {
        android.widget.Toast.makeText(
            context,
            "WhatsApp open nahi ho saka",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

// ============================================================
// FEE PLAN DIALOG
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeePlanDialog(
    student: Student,
    coachingId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    val existing =
        FeeStore.findPlan(

            studentId =
                student.id,

            coachingId =
                coachingId
        )

    var totalFeeText by remember(existing) {

        mutableStateOf(

            if (
                existing != null
            ) {

                existing.originalFee.toString()

            } else {

                ""
            }
        )
    }

    var concessionType by remember(existing) {

        mutableStateOf(

            existing?.concessionType
                ?: FeeConcessionType.NONE
        )
    }

    var concessionValueText by remember(existing) {

        mutableStateOf(

            if (
                existing != null &&
                existing.concessionValue > 0
            ) {

                existing.concessionValue.toString()

            } else {

                ""
            }
        )
    }

    var paymentMode by remember(existing) {

        mutableStateOf(

            existing?.paymentMode
                ?: FeePaymentMode.INSTALLMENTS
        )
    }

    val existingInstallments =
        remember(existing) {

            FeeStore.getStudentInstallments(

                studentId =
                    student.id,

                coachingId =
                    coachingId
            )
        }

    var date1 by remember(existingInstallments) {

        mutableStateOf(

            existingInstallments
                .firstOrNull {
                    it.installmentNumber == 1
                }
                ?.dueDate
                ?: ""
        )
    }

    var date2 by remember(existingInstallments) {

        mutableStateOf(

            existingInstallments
                .firstOrNull {
                    it.installmentNumber == 2
                }
                ?.dueDate
                ?: ""
        )
    }

    var date3 by remember(existingInstallments) {

        mutableStateOf(

            existingInstallments
                .firstOrNull {
                    it.installmentNumber == 3
                }
                ?.dueDate
                ?: ""
        )
    }

    var fullPaymentDate by remember(existingInstallments) {

        mutableStateOf(

            existingInstallments
                .firstOrNull {
                    it.installmentNumber == 0
                }
                ?.dueDate
                ?: ""
        )
    }

    var selectedDateField by remember {
        mutableStateOf(0)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val originalFee =
        totalFeeText
            .toDoubleOrNull()
            ?.coerceAtLeast(
                0.0
            )
            ?: 0.0

    val concessionValue =
        concessionValueText
            .toDoubleOrNull()
            ?.coerceAtLeast(
                0.0
            )
            ?: 0.0

    val concessionAmount =
        FeeStore.calculateConcessionAmount(

            originalFee =
                originalFee,

            concessionType =
                concessionType,

            concessionValue =
                concessionValue
        )

    val finalFee =
        FeeStore.calculateFinalFee(

            originalFee =
                originalFee,

            concessionType =
                concessionType,

            concessionValue =
                concessionValue
        )

    val preview =
        FeeStore.calculateInstallmentAmounts(
            finalFee
        )

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(

                text =
                    if (
                        existing == null
                    ) {

                        "Create Fee Plan"

                    } else {

                        "Edit Fee Plan"
                    }
            )
        },

        text = {

            Column(

                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Text(

                    text =
                        "Student: ${student.name}",

                    fontWeight =
                        FontWeight.Bold
                )

                OutlinedTextField(

                    value =
                        totalFeeText,

                    onValueChange = {

                        totalFeeText =
                            it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {

                        Text(
                            "Total Course Fee"
                        )
                    },

                    singleLine = true
                )

                Text(

                    text =
                        "Concession",

                    fontWeight =
                        FontWeight.Bold
                )

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    RadioButton(

                        selected =
                            concessionType ==
                                    FeeConcessionType.NONE,

                        onClick = {

                            concessionType =
                                FeeConcessionType.NONE

                            concessionValueText =
                                ""
                        }
                    )

                    Text(

                        text =
                            "None",

                        modifier =
                            Modifier.clickable {

                                concessionType =
                                    FeeConcessionType.NONE

                                concessionValueText =
                                    ""
                            }
                    )
                }

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    RadioButton(

                        selected =
                            concessionType ==
                                    FeeConcessionType.FIXED,

                        onClick = {

                            concessionType =
                                FeeConcessionType.FIXED
                        }
                    )

                    Text(

                        text =
                            "Fixed ₹",

                        modifier =
                            Modifier.clickable {

                                concessionType =
                                    FeeConcessionType.FIXED
                            }
                    )
                }

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    RadioButton(

                        selected =
                            concessionType ==
                                    FeeConcessionType.PERCENTAGE,

                        onClick = {

                            concessionType =
                                FeeConcessionType.PERCENTAGE
                        }
                    )

                    Text(

                        text =
                            "Percentage %",

                        modifier =
                            Modifier.clickable {

                                concessionType =
                                    FeeConcessionType.PERCENTAGE
                            }
                    )
                }

                if (
                    concessionType !=
                    FeeConcessionType.NONE
                ) {

                    OutlinedTextField(

                        value =
                            concessionValueText,

                        onValueChange = {

                            concessionValueText =
                                it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {

                            Text(

                                text =
                                    if (
                                        concessionType ==
                                        FeeConcessionType.FIXED
                                    ) {

                                        "Concession Amount ₹"

                                    } else {

                                        "Concession Percentage %"
                                    }
                            )
                        },

                        singleLine = true
                    )
                }

                Card(

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(

                        modifier =
                            Modifier.padding(
                                12.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                5.dp
                            )
                    ) {

                        Text(

                            text =
                                "Fee Preview",

                            fontWeight =
                                FontWeight.Bold
                        )

                        FeeInfoRow(

                            title =
                                "Original Fee",

                            value =
                                money(
                                    originalFee
                                )
                        )

                        FeeInfoRow(

                            title =
                                "Concession",

                            value =
                                money(
                                    concessionAmount
                                )
                        )

                        FeeInfoRow(

                            title =
                                "Final Payable",

                            value =
                                money(
                                    finalFee
                                )
                        )
                    }
                }

                Text(

                    text =
                        "Payment Mode",

                    fontWeight =
                        FontWeight.Bold
                )

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    RadioButton(

                        selected =
                            paymentMode ==
                                    FeePaymentMode.INSTALLMENTS,

                        onClick = {

                            paymentMode =
                                FeePaymentMode.INSTALLMENTS
                        }
                    )

                    Text(

                        text =
                            "Installments",

                        modifier =
                            Modifier.clickable {

                                paymentMode =
                                    FeePaymentMode.INSTALLMENTS
                            }
                    )
                }

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    RadioButton(

                        selected =
                            paymentMode ==
                                    FeePaymentMode.FULL_PAYMENT,

                        onClick = {

                            paymentMode =
                                FeePaymentMode.FULL_PAYMENT
                        }
                    )

                    Text(

                        text =
                            "Full Payment",

                        modifier =
                            Modifier.clickable {

                                paymentMode =
                                    FeePaymentMode.FULL_PAYMENT
                            }
                    )
                }

                if (
                    paymentMode ==
                    FeePaymentMode.INSTALLMENTS
                ) {

                    Text(

                        text =
                            "Installment Schedule",

                        fontWeight =
                            FontWeight.Bold
                    )

                    PreviewInstallment(

                        number =
                            1,

                        amount =
                            preview.getOrElse(
                                0
                            ) {
                                0.0
                            },

                        dueDate =
                            date1,

                        onDateClick = {

                            selectedDateField =
                                1

                            showDatePicker =
                                true
                        }
                    )

                    PreviewInstallment(

                        number =
                            2,

                        amount =
                            preview.getOrElse(
                                1
                            ) {
                                0.0
                            },

                        dueDate =
                            date2,

                        onDateClick = {

                            selectedDateField =
                                2

                            showDatePicker =
                                true
                        }
                    )

                    PreviewInstallment(

                        number =
                            3,

                        amount =
                            preview.getOrElse(
                                2
                            ) {
                                0.0
                            },

                        dueDate =
                            date3,

                        onDateClick = {

                            selectedDateField =
                                3

                            showDatePicker =
                                true
                        }
                    )

                } else {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),

                            verticalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {

                            Text(

                                text =
                                    "Full Payment",

                                fontWeight =
                                    FontWeight.Bold
                            )

                            FeeInfoRow(

                                title =
                                    "Amount",

                                value =
                                    money(
                                        finalFee
                                    )
                            )

                            OutlinedButton(

                                onClick = {

                                    selectedDateField =
                                        4

                                    showDatePicker =
                                        true
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.CalendarToday,

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

                                    text =
                                        if (
                                            fullPaymentDate.isBlank()
                                        ) {

                                            "Select Full Payment Due Date"

                                        } else {

                                            "Due Date: $fullPaymentDate"
                                        }
                                )
                            }
                        }
                    }
                }

                if (
                    errorMessage.isNotBlank()
                ) {

                    Text(

                        text =
                            errorMessage,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    errorMessage =
                        ""

                    if (
                        originalFee <= 0.0
                    ) {

                        errorMessage =
                            "Please enter total course fee."

                        return@Button
                    }

                    if (
                        concessionType !=
                        FeeConcessionType.NONE &&
                        concessionValue <= 0.0
                    ) {

                        errorMessage =
                            "Please enter a valid concession."

                        return@Button
                    }

                    if (
                        concessionType ==
                        FeeConcessionType.PERCENTAGE &&
                        concessionValue > 100.0
                    ) {

                        errorMessage =
                            "Percentage concession cannot exceed 100%."

                        return@Button
                    }

                    if (
                        concessionAmount >
                        originalFee
                    ) {

                        errorMessage =
                            "Concession cannot exceed total fee."

                        return@Button
                    }

                    if (
                        finalFee <= 0.0
                    ) {

                        errorMessage =
                            "Final payable fee must be greater than zero."

                        return@Button
                    }

                    if (
                        paymentMode ==
                        FeePaymentMode.INSTALLMENTS
                    ) {

                        if (
                            date1.isBlank() ||
                            date2.isBlank() ||
                            date3.isBlank()
                        ) {

                            errorMessage =
                                "Please select all 3 installment due dates."

                            return@Button
                        }

                    } else {

                        if (
                            fullPaymentDate.isBlank()
                        ) {

                            errorMessage =
                                "Please select Full Payment due date."

                            return@Button
                        }
                    }

                    try {

                        if (
                            existing == null
                        ) {

                            FeeStore.createAutomaticFeePlan(

                                studentId =
                                    student.id,

                                coachingId =
                                    coachingId,

                                originalFee =
                                    originalFee,

                                concessionType =
                                    concessionType,

                                concessionValue =
                                    concessionValue,

                                paymentMode =
                                    paymentMode,

                                installment1DueDate =
                                    date1,

                                installment2DueDate =
                                    date2,

                                installment3DueDate =
                                    date3,

                                fullPaymentDueDate =
                                    fullPaymentDate
                            )

                        } else {

                            FeeStore.updateAutomaticFeePlan(

                                studentId =
                                    student.id,

                                coachingId =
                                    coachingId,

                                originalFee =
                                    originalFee,

                                concessionType =
                                    concessionType,

                                concessionValue =
                                    concessionValue,

                                paymentMode =
                                    paymentMode,

                                installment1DueDate =
                                    date1,

                                installment2DueDate =
                                    date2,

                                installment3DueDate =
                                    date3,

                                fullPaymentDueDate =
                                    fullPaymentDate
                            )
                        }

                        onSaved()

                    } catch (
                        exception: Exception
                    ) {

                        errorMessage =
                            exception.message
                                ?: "Fee plan could not be saved."
                    }
                }
            ) {

                Text(
                    "Save"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )

    if (
        showDatePicker
    ) {

        val currentDate =

            when (
                selectedDateField
            ) {

                1 ->
                    date1

                2 ->
                    date2

                3 ->
                    date3

                4 ->
                    fullPaymentDate

                else ->
                    ""
            }

        FeeDatePickerDialog(

            initialDate =
                currentDate,

            onDismiss = {

                showDatePicker =
                    false
            },

            onDateSelected = {
                    selectedDate ->

                when (
                    selectedDateField
                ) {

                    1 ->
                        date1 =
                            selectedDate

                    2 ->
                        date2 =
                            selectedDate

                    3 ->
                        date3 =
                            selectedDate

                    4 ->
                        fullPaymentDate =
                            selectedDate
                }

                showDatePicker =
                    false
            }
        )
    }
}

// ============================================================
// PREVIEW INSTALLMENT
// ============================================================

@Composable
private fun PreviewInstallment(
    number: Int,
    amount: Double,
    dueDate: String,
    onDateClick: () -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                12.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    12.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
        ) {

            Text(

                text =
                    "Installment $number",

                fontWeight =
                    FontWeight.Bold
            )

            FeeInfoRow(

                title =
                    "Amount",

                value =
                    money(
                        amount
                    )
            )

            OutlinedButton(

                onClick =
                    onDateClick,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Icon(

                    imageVector =
                        Icons.Default.CalendarToday,

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

                    text =
                        if (
                            dueDate.isBlank()
                        ) {

                            "Select Due Date"

                        } else {

                            "Due Date: $dueDate"
                        }
                )
            }
        }
    }
}

// ============================================================
// PAYMENT DIALOG
// ============================================================

@Composable
private fun PaymentDialog(
    installment: FeeInstallment,
    student: Student,
    coachingId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {

    val remaining =
        (
                installment.amount -
                        installment.paidAmount
                ).coerceAtLeast(
                0.0
            )

    val feeSummary =
        FeeStore.getSummary(

            studentId =
                student.id,

            coachingId =
                coachingId
        )

    val previousPaid =
        feeSummary?.totalPaid
            ?: 0.0

    val totalFee =
        feeSummary?.totalFee
            ?: installment.amount

    var amountText by remember(

        installment.id,
        installment.paidAmount
    ) {

        mutableStateOf(
            formatInputAmount(
                remaining
            )
        )
    }

    var paidDate by remember {

        mutableStateOf(

            if (
                installment.paidDate.isNotBlank()
            ) {

                installment.paidDate

            } else {

                FeeStore.todayDate()
            }
        )
    }

    var note by remember {

        mutableStateOf("")
    }

    var paymentMode by remember {

        mutableStateOf(
            PaymentMode.CASH
        )
    }

    var transactionReference by remember {

        mutableStateOf("")
    }

    var paymentModeExpanded by remember {

        mutableStateOf(false)
    }

    var showDatePicker by remember {

        mutableStateOf(false)
    }

    var errorMessage by remember {

        mutableStateOf("")
    }

    var saving by remember {

        mutableStateOf(false)
    }

    val title =
        if (
            installment.installmentNumber == 0
        ) {

            "Full Payment"

        } else {

            "Installment ${installment.installmentNumber}"
        }

    AlertDialog(

        onDismissRequest = {

            if (!saving) {
                onDismiss()
            }
        },

        title = {

            Column {

                Text(

                    text =
                        "Add Payment",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(

                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        },

        text = {

            Column(

                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                // ====================================================
                // STUDENT
                // ====================================================

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                ) {

                    Column(

                        modifier =
                            Modifier.padding(
                                12.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {

                        Text(

                            text =
                                student.name,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(

                            text =
                                "Student ID: ${student.studentId}"
                        )

                        if (
                            student.course.isNotBlank()
                        ) {

                            Text(

                                text =
                                    "Course: ${student.course}"
                            )
                        }

                        Text(

                            text =
                                "Payment status: PENDING",

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

                // ====================================================
                // SUMMARY
                // ====================================================

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                ) {

                    Column(

                        modifier =
                            Modifier.padding(
                                12.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                5.dp
                            )
                    ) {

                        Text(

                            text =
                                "Payment Summary",

                            fontWeight =
                                FontWeight.Bold
                        )

                        FeeInfoRow(

                            title =
                                "Total Fee",

                            value =
                                money(
                                    totalFee
                                )
                        )

                        FeeInfoRow(

                            title =
                                "Previous Paid",

                            value =
                                money(
                                    previousPaid
                                )
                        )

                        FeeInfoRow(

                            title =
                                "Installment Amount",

                            value =
                                money(
                                    installment.amount
                                )
                        )

                        FeeInfoRow(

                            title =
                                "Already Paid",

                            value =
                                money(
                                    installment.paidAmount
                                )
                        )

                        FeeInfoRow(

                            title =
                                "Installment Remaining",

                            value =
                                money(
                                    remaining
                                )
                        )
                    }
                }

                // ====================================================
                // CURRENT PAYMENT
                // ====================================================

                OutlinedTextField(

                    value =
                        amountText,

                    onValueChange = {

                        amountText =
                            sanitizeMoneyInput(
                                it
                            )
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {

                        Text(
                            "Current Payment Amount"
                        )
                    },

                    singleLine = true
                )

                // ====================================================
                // PAYMENT MODE
                // ====================================================

                Box(

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    OutlinedButton(

                        onClick = {

                            paymentModeExpanded =
                                true
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.AttachMoney,

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

                            text =
                                "Payment Mode: ${
                                    paymentMode.displayName()
                                }"
                        )
                    }

                    DropdownMenu(

                        expanded =
                            paymentModeExpanded,

                        onDismissRequest = {

                            paymentModeExpanded =
                                false
                        }
                    ) {

                        PaymentMode.values()
                            .forEach { mode ->

                                DropdownMenuItem(

                                    text = {

                                        Text(
                                            mode.displayName()
                                        )
                                    },

                                    onClick = {

                                        paymentMode =
                                            mode

                                        paymentModeExpanded =
                                            false
                                    }
                                )
                            }
                    }
                }

                // ====================================================
                // TRANSACTION REFERENCE
                // ====================================================

                OutlinedTextField(

                    value =
                        transactionReference,

                    onValueChange = {

                        transactionReference =
                            it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {

                        Text(
                            "Transaction / Reference No."
                        )
                    },

                    placeholder = {

                        Text(
                            "UPI / UTR / Cheque No."
                        )
                    },

                    singleLine = true
                )

                // ====================================================
                // PAYMENT DATE
                // ====================================================

                OutlinedButton(

                    onClick = {

                        showDatePicker =
                            true
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.CalendarToday,

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

                        text =
                            "Payment Date: $paidDate"
                    )
                }

                // ====================================================
                // NOTE
                // ====================================================

                OutlinedTextField(

                    value =
                        note,

                    onValueChange = {

                        note =
                            it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {

                        Text(
                            "Payment Note"
                        )
                    },

                    minLines =
                        2,

                    maxLines =
                        4
                )

                if (
                    errorMessage.isNotBlank()
                ) {

                    Text(

                        text =
                            errorMessage,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },

        confirmButton = {

            Button(

                enabled =
                    !saving,

                onClick = {

                    errorMessage =
                        ""

                    val amount =
                        amountText
                            .toDoubleOrNull()

                    if (
                        amount == null ||
                        amount <= 0.0
                    ) {

                        errorMessage =
                            "Please enter a valid payment amount."

                        return@Button
                    }

                    if (
                        amount > remaining
                    ) {

                        errorMessage =
                            "Payment cannot be greater than remaining installment amount."

                        return@Button
                    }

                    if (
                        paidDate.isBlank()
                    ) {

                        errorMessage =
                            "Please select payment date."

                        return@Button
                    }

                    saving =
                        true

                    try {

                        // =================================================
                        // CALCULATE PAYMENT VALUES
                        // =================================================

                        val newInstallmentPaid =
                            installment.paidAmount +
                                    amount

                        val newOverallPaid =
                            previousPaid +
                                    amount

                        val newRemaining =
                            (
                                    totalFee -
                                            newOverallPaid
                                    ).coerceAtLeast(
                                    0.0
                                )

                        // =================================================
                        // RECEIPT NUMBER
                        // =================================================

                        val receiptNumber =
                            PaymentStore.nextReceiptNumber(
                                coachingId
                            )

                        // =================================================
                        // PAYMENT RECORD
                        // =================================================

                        val batchNameResolved =
                            if (student.batchId.isNotBlank()) {
                                BatchStore.findBatch(student.batchId)?.name
                                    ?: ""
                            } else {
                                ""
                            }

                        val paymentRecord =
                            PaymentRecord(

                                id =
                                    PaymentStore.nextId(),

                                receiptNumber =
                                    receiptNumber,

                                coachingId =
                                    coachingId,

                                studentId =
                                    student.id,

                                batchId =
                                    student.batchId,

                                installmentId =
                                    installment.id,

                                studentName =
                                    student.name,

                                studentCode =
                                    student.studentId,

                                course =
                                    student.course,

                                batchName =
                                    batchNameResolved,

                                amount =
                                    amount,

                                paymentDate =
                                    paidDate,

                                paymentMode =
                                    paymentMode,

                                transactionReference =
                                    transactionReference
                                        .trim(),

                                previousPaidAmount =
                                    previousPaid,

                                totalFee =
                                    totalFee,

                                remainingAmount =
                                    newRemaining,

                                remark =
                                    note.trim(),

                                status =
                                    PaymentStatus.PENDING,

                                createdAt =
                                    System.currentTimeMillis()
                            )

                        // =================================================
                        // SAVE TRANSACTION
                        // =================================================

                        PaymentStore.addPayment(
                            paymentRecord
                        )

                        // =================================================
                        // UPDATE FEE INSTALLMENT
                        // =================================================

                        val success =
                            FeeStore.updatePayment(

                                installmentId =
                                    installment.id,

                                paidAmount =
                                    newInstallmentPaid,

                                paidDate =
                                    paidDate,

                                paymentNote =
                                    note.trim()
                            )

                        if (!success) {

                            errorMessage =
                                "Payment transaction was recorded, but fee installment could not be updated."

                            saving =
                                false

                            return@Button
                        }

                        onSaved()

                    } catch (
                        exception: Exception
                    ) {

                        errorMessage =
                            exception.message
                                ?: "Payment could not be saved."

                        saving =
                            false
                    }
                }
            ) {

                Text(

                    text =
                        if (saving) {

                            "Saving..."

                        } else {

                            "Save Payment"
                        }
                )
            }
        },

        dismissButton = {

            TextButton(

                enabled =
                    !saving,

                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )

    // ========================================================
    // PAYMENT DATE PICKER
    // ========================================================

    if (
        showDatePicker
    ) {

        FeeDatePickerDialog(

            initialDate =
                paidDate,

            onDismiss = {

                showDatePicker =
                    false
            },

            onDateSelected = {
                    selectedDate ->

                paidDate =
                    selectedDate

                showDatePicker =
                    false
            }
        )
    }
}

// ============================================================
// DATE PICKER
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeeDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {

    val initialMillis =

        try {

            if (
                initialDate.isNotBlank()
            ) {

                SimpleDateFormat(

                    "dd-MM-yyyy",

                    Locale.getDefault()
                )
                    .parse(
                        initialDate
                    )
                    ?.time

            } else {

                null
            }

        } catch (
            exception: Exception
        ) {

            null
        }

    val datePickerState =
        rememberDatePickerState(

            initialSelectedDateMillis =
                initialMillis
        )

    DatePickerDialog(

        onDismissRequest =
            onDismiss,

        confirmButton = {

            TextButton(

                onClick = {

                    val millis =
                        datePickerState
                            .selectedDateMillis

                    if (
                        millis != null
                    ) {

                        val formatted =
                            SimpleDateFormat(

                                "dd-MM-yyyy",

                                Locale.getDefault()
                            )
                                .format(
                                    Date(
                                        millis
                                    )
                                )

                        onDateSelected(
                            formatted
                        )
                    }
                }
            ) {

                Text(
                    "OK"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }

    ) {

        DatePicker(
            state =
                datePickerState
        )
    }
}

// ============================================================
// INSTALLMENT STATUS
// ============================================================

private fun getInstallmentDisplayStatus(
    installment: FeeInstallment
): FeeInstallmentStatus {

    return when {

        installment.paidAmount >=
                installment.amount -> {

            FeeInstallmentStatus.PAID
        }

        installment.status ==
                FeeInstallmentStatus.OVERDUE -> {

            FeeInstallmentStatus.OVERDUE
        }

        else -> {

            FeeInstallmentStatus.PENDING
        }
    }
}

private fun installmentStatusText(
    status: FeeInstallmentStatus
): String {

    return when (status) {

        FeeInstallmentStatus.PAID ->
            "PAID"

        FeeInstallmentStatus.PENDING ->
            "PENDING"

        FeeInstallmentStatus.OVERDUE ->
            "OVERDUE"
    }
}

// ============================================================
// PAYMENT MODE
// ============================================================

private fun PaymentMode.displayName(): String {

    return when (this) {

        PaymentMode.CASH ->
            "Cash"

        PaymentMode.UPI ->
            "UPI"

        PaymentMode.BANK_TRANSFER ->
            "Bank Transfer"

        PaymentMode.CHEQUE ->
            "Cheque"

        PaymentMode.OTHER ->
            "Other"
    }
}

// ============================================================
// FEE PAYMENT MODE
// ============================================================

private fun paymentModeText(
    mode: FeePaymentMode
): String {

    return when (mode) {

        FeePaymentMode.INSTALLMENTS ->
            "Installments"

        FeePaymentMode.FULL_PAYMENT ->
            "Full Payment"
    }
}

// ============================================================
// MONEY
// ============================================================

private fun money(
    value: Double
): String {

    return "₹" +
            String.format(
                Locale.getDefault(),
                "%.2f",
                value
            )
}

private fun formatInputAmount(
    value: Double
): String {

    return String.format(
        Locale.US,
        "%.2f",
        value
    )
}

private fun sanitizeMoneyInput(
    value: String
): String {

    var result =
        value.filter {

            it.isDigit() ||
                    it == '.'
        }

    val firstDot =
        result.indexOf('.')

    if (
        firstDot >= 0
    ) {

        result =
            result.substring(
                0,
                firstDot + 1
            ) +
                    result
                        .substring(
                            firstDot + 1
                        )
                        .replace(
                            ".",
                            ""
                        )
    }

    if (
        result.contains('.')
    ) {

        val parts =
            result.split(
                ".",
                limit = 2
            )

        val decimals =
            parts
                .getOrElse(1) {
                    ""
                }
                .take(2)

        result =
            parts[0] +
                    "." +
                    decimals
    }

    return result
}