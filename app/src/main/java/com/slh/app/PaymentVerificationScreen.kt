package com.slh.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentVerificationScreen(
    coachingId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

    var selectedTab by remember {
        mutableStateOf(0)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var selectedPayment by remember {
        mutableStateOf<PaymentRecord?>(null)
    }

    var paymentToVerify by remember {
        mutableStateOf<PaymentRecord?>(null)
    }

    var paymentToCancel by remember {
        mutableStateOf<PaymentRecord?>(null)
    }

    var receiptFile by remember {
        mutableStateOf<File?>(null)
    }

    LaunchedEffect(refreshKey) {
        PaymentStore.initialize()
    }

    val allPayments = remember(
        coachingId,
        refreshKey,
        PaymentStore.payments
    ) {
        PaymentStore
            .findByCoaching(coachingId)
            .sortedByDescending {
                it.createdAt
            }
    }

    val pendingPayments = remember(
        allPayments,
        searchText
    ) {
        filterPayments(
            payments = allPayments.filter {
                it.status == PaymentStatus.PENDING
            },
            searchText = searchText
        )
    }

    val verifiedPayments = remember(
        allPayments,
        searchText
    ) {
        filterPayments(
            payments = allPayments.filter {
                it.status == PaymentStatus.VERIFIED
            },
            searchText = searchText
        )
    }

    val cancelledPayments = remember(
        allPayments,
        searchText
    ) {
        filterPayments(
            payments = allPayments.filter {
                it.status == PaymentStatus.CANCELLED
            },
            searchText = searchText
        )
    }

    val displayedPayments = when (selectedTab) {
        0 -> pendingPayments
        1 -> verifiedPayments
        else -> cancelledPayments
    }

    val pendingAmount = pendingPayments.sumOf {
        it.amount
    }

    val verifiedAmount = verifiedPayments.sumOf {
        it.amount
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment Verification",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            refreshKey++
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            SummarySection(
                pendingCount = pendingPayments.size,
                pendingAmount = pendingAmount,
                verifiedCount = verifiedPayments.size,
                verifiedAmount = verifiedAmount
            )

            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                searchText = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                label = {
                    Text("Search payment")
                },
                placeholder = {
                    Text(
                        "Student, receipt, reference..."
                    )
                }
            )

            TabRow(
                selectedTabIndex = selectedTab
            ) {

                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            "Pending (${pendingPayments.size})"
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            "Verified (${verifiedPayments.size})"
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    text = {
                        Text(
                            "Cancelled (${cancelledPayments.size})"
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null
                        )
                    }
                )
            }

            if (displayedPayments.isEmpty()) {

                EmptyPaymentState(
                    selectedTab = selectedTab
                )

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {

                    items(
                        items = displayedPayments,
                        key = {
                            it.id
                        }
                    ) { payment ->

                        PaymentVerificationCard(
                            payment = payment,
                            onView = {
                                selectedPayment = payment
                            },
                            onVerify = {
                                paymentToVerify = payment
                            },
                            onCancel = {
                                paymentToCancel = payment
                            },
                            onViewReceipt = {

                                val file =
                                    findReceiptFile(
                                        context = context,
                                        payment = payment
                                    )

                                if (file != null) {
                                    receiptFile = file
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Receipt file not found.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onShareReceipt = {
                                shareReceipt(
                                    context = context,
                                    payment = payment
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    selectedPayment?.let { payment ->

        PaymentDetailsDialog(
            payment = payment,
            onDismiss = {
                selectedPayment = null
            },
            onVerify = {
                selectedPayment = null
                paymentToVerify = payment
            },
            onCancel = {
                selectedPayment = null
                paymentToCancel = payment
            }
        )
    }

    paymentToVerify?.let { payment ->

        AlertDialog(
            onDismissRequest = {
                paymentToVerify = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null
                )
            },
            title = {
                Text("Verify Payment?")
            },
            text = {
                Text(
                    "Are you sure you want to verify payment " +
                            "${payment.receiptNumber}?\n\n" +
                            "Amount: ${paymentMoney(payment.amount)}"
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        val verified =
                            PaymentStore.verifyPayment(
                                payment.id
                            )

                        if (verified) {

                            val updatedPayment =
                                PaymentStore.getById(
                                    payment.id
                                )

                            if (
                                updatedPayment != null &&
                                updatedPayment.status ==
                                PaymentStatus.VERIFIED
                            ) {

                                // Clear "pending admin verify" note on installment
                                if (updatedPayment.installmentId.isNotBlank()) {
                                    val inst =
                                        FeeStore.getInstallmentById(
                                            updatedPayment.installmentId
                                        )
                                    if (inst != null) {
                                        FeeStore.updatePayment(
                                            installmentId = inst.id,
                                            // Older student submissions already counted the amount
                                            // on this installment; newer ones did not (they are only
                                            // applied here, at verification).
                                            paidAmount =
                                                if (
                                                    inst.paymentNote.contains(
                                                        "pending admin verify",
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    inst.paidAmount
                                                } else {
                                                    inst.paidAmount +
                                                            updatedPayment.amount
                                                },
                                            paidDate = inst.paidDate.ifBlank {
                                                updatedPayment.paymentDate
                                            },
                                            paymentNote = "Verified • Receipt ${updatedPayment.receiptNumber}"
                                        )
                                    }
                                }

                                try {

                                    val file =
                                        ReceiptGenerator.generateReceipt(
                                            context = context,
                                            payment = updatedPayment
                                        )

                                    receiptFile = file

                                    Toast.makeText(
                                        context,
                                        "Payment verified and receipt generated.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } catch (e: Exception) {

                                    Toast.makeText(
                                        context,
                                        "Payment verified, but receipt generation failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } else {

                                Toast.makeText(
                                    context,
                                    "Payment verified.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            refreshKey++

                        } else {

                            Toast.makeText(
                                context,
                                "Payment verification failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        paymentToVerify = null
                    }
                ) {
                    Text("Verify & Generate Receipt")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        paymentToVerify = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    paymentToCancel?.let { payment ->

        AlertDialog(
            onDismissRequest = {
                paymentToCancel = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null
                )
            },
            title = {
                Text("Cancel Payment?")
            },
            text = {
                Text(
                    "Are you sure you want to cancel payment " +
                            "${payment.receiptNumber}?"
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        val cancelled =
                            PaymentStore.cancelPayment(
                                payment.id
                            )

                        if (cancelled) {

                            var reversalMessage =
                                ""

                            if (
                                payment.installmentId
                                    .isNotBlank()
                            ) {

                                val installment =
                                    FeeStore.getInstallmentById(
                                        payment.installmentId
                                    )

                                if (installment != null) {

                                    /*
                                     * Reverse the amount only if the installment actually
                                     * includes this payment: a verified payment, or an older
                                     * student submission that was already counted locally.
                                     * A PENDING payment submitted by a student was never
                                     * added to the installment.
                                     */
                                    val includesPayment =
                                        payment.status == PaymentStatus.VERIFIED ||
                                                installment.paymentNote.contains(
                                                    "pending admin verify",
                                                    ignoreCase = true
                                                )

                                    if (includesPayment) {

                                        val reversedAmount =
                                            (
                                                    installment.paidAmount -
                                                            payment.amount
                                                    ).coerceAtLeast(
                                                    0.0
                                                )

                                        FeeStore.updatePayment(
                                            installmentId =
                                                payment.installmentId,
                                            paidAmount =
                                                reversedAmount,
                                            paymentNote =
                                                "Reversed: payment ${payment.receiptNumber} cancelled"
                                        )
                                    }

                                } else {

                                    reversalMessage =
                                        " (installment not found, please check fee manually)"
                                }

                            } else {

                                reversalMessage =
                                    " (old record, please adjust student's fee manually)"
                            }

                            Toast.makeText(
                                context,
                                "Payment cancelled.$reversalMessage",
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            Toast.makeText(
                                context,
                                "Unable to cancel payment.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        refreshKey++
                        paymentToCancel = null
                    }
                ) {
                    Text("Cancel Payment")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        paymentToCancel = null
                    }
                ) {
                    Text("Keep Payment")
                }
            }
        )
    }

    receiptFile?.let { file ->

        ReceiptReadyDialog(
            file = file,
            onDismiss = {
                receiptFile = null
            },
            onOpen = {
                openReceipt(
                    context = context,
                    file = file
                )
            },
            onShare = {
                shareReceiptFile(
                    context = context,
                    file = file
                )
            }
        )
    }
}


private fun filterPayments(
    payments: List<PaymentRecord>,
    searchText: String
): List<PaymentRecord> {

    val query = searchText
        .trim()
        .lowercase(Locale.getDefault())

    if (query.isBlank()) {
        return payments
    }

    return payments.filter { payment ->

        payment.studentName
            .lowercase(Locale.getDefault())
            .contains(query) ||

                payment.studentCode
                    .lowercase(Locale.getDefault())
                    .contains(query) ||

                payment.receiptNumber
                    .lowercase(Locale.getDefault())
                    .contains(query) ||

                payment.transactionReference
                    .lowercase(Locale.getDefault())
                    .contains(query)
    }
}


@Composable
private fun SummarySection(
    pendingCount: Int,
    pendingAmount: Double,
    verifiedCount: Int,
    verifiedAmount: Double
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(
            10.dp
        )
    ) {

        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Pending",
            value = pendingCount.toString(),
            subtitle = paymentMoney(
                pendingAmount
            )
        )

        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Verified",
            value = verifiedCount.toString(),
            subtitle = paymentMoney(
                verifiedAmount
            )
        )
    }
}


@Composable
private fun SummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun PaymentVerificationCard(
    payment: PaymentRecord,
    onView: () -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
    onViewReceipt: () -> Unit,
    onShareReceipt: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = payment.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = payment.studentCode,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                PaymentStatusChip(
                    status = payment.status
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "Receipt: ${payment.receiptNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            PaymentInfoRow(
                label = "Amount",
                value = paymentMoney(payment.amount)
            )

            PaymentInfoRow(
                label = "Date",
                value = payment.paymentDate
            )

            PaymentInfoRow(
                label = "Mode",
                value = paymentModeText(
                    payment.paymentMode
                )
            )

            if (
                payment.transactionReference.isNotBlank()
            ) {

                PaymentInfoRow(
                    label = "Reference",
                    value = payment.transactionReference
                )
            }

            PaymentInfoRow(
                label = "Previous Paid",
                value = paymentMoney(
                    payment.previousPaidAmount
                )
            )

            PaymentInfoRow(
                label = "Remaining",
                value = paymentMoney(
                    payment.remainingAmount
                )
            )

            if (payment.remark.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Remark: ${payment.remark}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Divider()

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp
                )
            ) {

                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f)
                ) {

                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(5.dp)
                    )

                    Text("Details")
                }

                if (
                    payment.status ==
                    PaymentStatus.PENDING
                ) {

                    Button(
                        onClick = onVerify,
                        modifier = Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.CheckCircle,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text("Verify")
                    }
                }
            }

            if (
                payment.status ==
                PaymentStatus.PENDING
            ) {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(5.dp)
                    )

                    Text("Cancel Payment")
                }
            }

            if (
                payment.status ==
                PaymentStatus.VERIFIED
            ) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp
                    )
                ) {

                    OutlinedButton(
                        onClick = onViewReceipt,
                        modifier = Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Description,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text("Receipt")
                    }

                    OutlinedButton(
                        onClick = onShareReceipt,
                        modifier = Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Share,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text("Share")
                    }
                }
            }
        }
    }
}


@Composable
private fun PaymentStatusChip(
    status: PaymentStatus
) {

    val text = when (status) {

        PaymentStatus.PENDING ->
            "PENDING"

        PaymentStatus.VERIFIED ->
            "VERIFIED"

        PaymentStatus.CANCELLED ->
            "CANCELLED"
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = text,
                maxLines = 1
            )
        }
    )
}


@Composable
private fun PaymentInfoRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 3.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun EmptyPaymentState(
    selectedTab: Int
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = when (selectedTab) {
                    0 -> "No pending payments"
                    1 -> "No verified payments"
                    else -> "No cancelled payments"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Payment records will appear here.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
private fun PaymentDetailsDialog(
    payment: PaymentRecord,
    onDismiss: () -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Payment Details")
        },
        text = {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(
                    7.dp
                )
            ) {

                item {
                    DetailRow(
                        "Receipt Number",
                        payment.receiptNumber
                    )
                }

                item {
                    DetailRow(
                        "Student",
                        payment.studentName
                    )
                }

                item {
                    DetailRow(
                        "Student ID",
                        payment.studentCode
                    )
                }

                item {
                    DetailRow(
                        "Course",
                        payment.course.ifBlank {
                            "-"
                        }
                    )
                }

                item {
                    DetailRow(
                        "Batch",
                        payment.batchName.ifBlank {
                            payment.batchId.ifBlank {
                                "-"
                            }
                        }
                    )
                }

                item {
                    DetailRow(
                        "Total Fee",
                        paymentMoney(
                            payment.totalFee
                        )
                    )
                }

                item {
                    DetailRow(
                        "Previous Paid",
                        paymentMoney(
                            payment.previousPaidAmount
                        )
                    )
                }

                item {
                    DetailRow(
                        "Current Payment",
                        paymentMoney(
                            payment.amount
                        )
                    )
                }

                item {
                    DetailRow(
                        "Remaining Balance",
                        paymentMoney(
                            payment.remainingAmount
                        )
                    )
                }

                item {
                    DetailRow(
                        "Payment Date",
                        payment.paymentDate
                    )
                }

                item {
                    DetailRow(
                        "Payment Mode",
                        paymentModeText(
                            payment.paymentMode
                        )
                    )
                }

                item {
                    DetailRow(
                        "Reference",
                        payment.transactionReference.ifBlank {
                            "-"
                        }
                    )
                }

                item {
                    DetailRow(
                        "Status",
                        payment.status.name
                    )
                }

                if (payment.remark.isNotBlank()) {

                    item {
                        DetailRow(
                            "Remark",
                            payment.remark
                        )
                    }
                }
            }
        },
        confirmButton = {

            if (
                payment.status ==
                PaymentStatus.PENDING
            ) {

                Button(
                    onClick = onVerify
                ) {
                    Text("Verify")
                }

            } else {

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {

            if (
                payment.status ==
                PaymentStatus.PENDING
            ) {

                TextButton(
                    onClick = onCancel
                ) {
                    Text("Cancel Payment")
                }
            }
        }
    )
}


@Composable
private fun DetailRow(
    label: String,
    value: String
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
private fun ReceiptReadyDialog(
    file: File,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null
            )
        },
        title = {
            Text("Receipt Ready")
        },
        text = {

            Column {

                Text(
                    "Payment has been verified and the PDF receipt has been generated."
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {

                    Text(
                        text = file.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {

            Button(
                onClick = onOpen
            ) {

                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text("Open")
            }
        },
        dismissButton = {

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    2.dp
                )
            ) {

                TextButton(
                    onClick = onShare
                ) {

                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text("Share")
                }

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Close")
                }
            }
        }
    )
}


private fun findReceiptFile(
    context: Context,
    payment: PaymentRecord
): File? {

    val directory = File(
        context.filesDir,
        "Documents/Receipts"
    )

    val safeReceiptNumber =
        payment.receiptNumber
            .replace(
                Regex("[^A-Za-z0-9._-]"),
                "_"
            )

    val expectedFile = File(
        directory,
        "Receipt_$safeReceiptNumber.pdf"
    )

    if (expectedFile.exists()) {
        return expectedFile
    }

    return directory
        .listFiles()
        ?.firstOrNull { file ->

            file.isFile &&
                    file.extension.equals(
                        "pdf",
                        ignoreCase = true
                    ) &&
                    file.name.contains(
                        safeReceiptNumber,
                        ignoreCase = true
                    )
        }
}


private fun openReceipt(
    context: Context,
    file: File
) {

    try {

        val uri: Uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        val intent = Intent(
            Intent.ACTION_VIEW
        ).apply {

            setDataAndType(
                uri,
                "application/pdf"
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

        context.startActivity(intent)

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "No PDF viewer found or receipt could not be opened.",
            Toast.LENGTH_LONG
        ).show()
    }
}


private fun shareReceipt(
    context: Context,
    payment: PaymentRecord
) {

    val file =
        findReceiptFile(
            context = context,
            payment = payment
        )

    if (file == null) {

        Toast.makeText(
            context,
            "Receipt file not found.",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    shareReceiptFile(
        context = context,
        file = file
    )
}


private fun shareReceiptFile(
    context: Context,
    file: File
) {

    try {

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        val shareIntent = Intent(
            Intent.ACTION_SEND
        ).apply {

            type = "application/pdf"

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                "Share Payment Receipt"
            )
        )

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "Unable to share receipt: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}


private fun paymentMoney(
    amount: Double
): String {

    val formatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    return formatter.format(amount)
}


private fun paymentModeText(
    mode: PaymentMode
): String {

    return when (mode) {

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