@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.slh.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StudentFeeHistoryScreen(
    studentId: String,
    coachingId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var selectedPayment by remember {
        mutableStateOf<PaymentRecord?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val payments = remember(
        studentId,
        refreshKey
    ) {
        PaymentStore
            .findByStudent(studentId)
            .sortedByDescending {
                it.paymentDate
            }
    }

    val verifiedPayments = payments.filter {
        it.status == PaymentStatus.VERIFIED
    }

    val pendingPayments = payments.filter {
        it.status == PaymentStatus.PENDING
    }

    val totalFee = payments
        .map {
            it.totalFee
        }
        .maxOrNull()
        ?: 0.0

    val totalPaid = verifiedPayments.sumOf {
        it.amount
    }

    val remainingBalance = when {
        verifiedPayments.isNotEmpty() -> {
            verifiedPayments
                .maxByOrNull {
                    it.createdAt
                }
                ?.remainingAmount
                ?: 0.0
        }

        payments.isNotEmpty() -> {
            payments
                .maxByOrNull {
                    it.createdAt
                }
                ?.remainingAmount
                ?: totalFee
        }

        else -> {
            totalFee
        }
    }

    val student = remember(studentId) {
        StudentStore.findStudent(studentId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fees & Payments",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            }

            item {
                StudentFeeHeaderCard(
                    studentName = student?.name ?: "Student",
                    studentCode = student?.studentId ?: studentId,
                    course = student?.course ?: "",
                    batchId = student?.batchId ?: ""
                )
            }

            item {
                FeeSummaryCard(
                    totalFee = totalFee,
                    totalPaid = totalPaid,
                    remaining = remainingBalance
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Verified",
                        value = verifiedPayments.size.toString(),
                        icon = Icons.Default.Verified
                    )

                    SmallSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending",
                        value = pendingPayments.size.toString(),
                        icon = Icons.Default.ErrorOutline
                    )
                }
            }

            item {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        top = 6.dp,
                        bottom = 2.dp
                    )
                )
            }

            if (payments.isEmpty()) {
                item {
                    EmptyPaymentCard()
                }
            } else {
                items(
                    items = payments,
                    key = {
                        it.id
                    }
                ) { payment ->

                    StudentPaymentCard(
                        payment = payment,
                        onViewDetails = {
                            selectedPayment = payment
                        },
                        onOpenReceipt = {
                            val file = getOrCreateReceiptFile(
                                context = context,
                                payment = payment
                            )

                            if (file != null) {
                                openReceipt(
                                    context = context,
                                    file = file,
                                    onError = {
                                        message = it
                                    }
                                )
                            } else {
                                message =
                                    "Receipt PDF ban nahi paya. Try again."
                            }
                        },
                        onShareReceipt = {
                            val file = getOrCreateReceiptFile(
                                context = context,
                                payment = payment
                            )

                            if (file != null) {
                                shareReceipt(
                                    context = context,
                                    file = file,
                                    onError = {
                                        message = it
                                    }
                                )
                            } else {
                                message =
                                    "Receipt PDF अभी उपलब्ध नहीं है।"
                            }
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

    selectedPayment?.let { payment ->

        PaymentDetailsDialog(
            payment = payment,
            onDismiss = {
                selectedPayment = null
            },
            onOpenReceipt = {
                selectedPayment = null

                val file = getOrCreateReceiptFile(
                    context = context,
                    payment = payment
                )

                if (file != null) {
                    openReceipt(
                        context = context,
                        file = file,
                        onError = {
                            message = it
                        }
                    )
                } else {
                    message =
                        "Receipt PDF ban nahi paya. Try again."
                }
            }
        )
    }

    message?.let { text ->

        AlertDialog(
            onDismissRequest = {
                message = null
            },
            title = {
                Text("Information")
            },
            text = {
                Text(text)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        message = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun StudentFeeHeaderCard(
    studentName: String,
    studentCode: String,
    course: String,
    batchId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = studentName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Student ID: $studentCode",
                style = MaterialTheme.typography.bodyMedium
            )

            if (course.isNotBlank()) {
                Text(
                    text = "Course: $course",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (batchId.isNotBlank()) {
                Text(
                    text = "Batch: $batchId",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun FeeSummaryCard(
    totalFee: Double,
    totalPaid: Double,
    remaining: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "Fee Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            FeeAmountRow(
                label = "Total Fee",
                amount = totalFee
            )

            FeeAmountRow(
                label = "Paid Amount",
                amount = totalPaid
            )

            FeeAmountRow(
                label = "Remaining Balance",
                amount = remaining,
                bold = true
            )

            if (totalFee > 0.0) {

                val progress = (
                        totalPaid / totalFee
                        )
                    .coerceIn(0.0, 1.0)
                    .toFloat()

                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${(progress * 100).toInt()}% fee paid",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FeeAmountRow(
    label: String,
    amount: Double,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (bold) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )

        Text(
            text = money(amount),
            fontWeight = if (bold) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            }
        )
    }
}

@Composable
private fun SmallSummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StudentPaymentCard(
    payment: PaymentRecord,
    onViewDetails: () -> Unit,
    onOpenReceipt: () -> Unit,
    onShareReceipt: () -> Unit
) {
    val isVerified =
        payment.status == PaymentStatus.VERIFIED

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = money(payment.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Show official receipt number only after payment is verified.
                    // While PENDING, do not expose receipt number as "paid".
                    Text(
                        text = if (isVerified) {
                            "Receipt: ${payment.receiptNumber}"
                        } else {
                            "Receipt will be issued after verification"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                PaymentStatusChip(
                    status = payment.status
                )
            }

            Divider()

            PaymentInfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Date",
                value = payment.paymentDate
            )

            PaymentInfoRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Mode",
                value = paymentModeText(
                    payment.paymentMode
                )
            )

            if (
                payment.transactionReference
                    .isNotBlank()
            ) {
                PaymentInfoRow(
                    icon = Icons.Default.Description,
                    label = "Reference",
                    value = payment.transactionReference
                )
            }

            PaymentInfoRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Remaining",
                value = money(
                    payment.remainingAmount
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }

                if (isVerified) {

                    OutlinedButton(
                        onClick = onOpenReceipt,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text("Receipt")
                    }

                    IconButton(
                        onClick = onShareReceipt
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Share,
                            contentDescription =
                                "Share receipt"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp)
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PaymentStatusChip(
    status: PaymentStatus
) {
    val text = when (status) {
        PaymentStatus.PENDING -> "Pending"
        PaymentStatus.VERIFIED -> "Verified"
        PaymentStatus.CANCELLED -> "Cancelled"
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 5.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyPaymentCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = "No payment records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "आपकी payment history यहाँ दिखाई देगी।",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PaymentDetailsDialog(
    payment: PaymentRecord,
    onDismiss: () -> Unit,
    onOpenReceipt: () -> Unit
) {
    val isVerified =
        payment.status == PaymentStatus.VERIFIED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Payment Details",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                DetailLine(
                    "Receipt Number",
                    if (isVerified) {
                        payment.receiptNumber
                    } else {
                        "Will be issued after verification"
                    }
                )

                DetailLine(
                    "Amount",
                    money(payment.amount)
                )

                DetailLine(
                    "Payment Date",
                    payment.paymentDate
                )

                DetailLine(
                    "Payment Mode",
                    paymentModeText(
                        payment.paymentMode
                    )
                )

                DetailLine(
                    "Status",
                    when (payment.status) {
                        PaymentStatus.PENDING ->
                            "Pending"

                        PaymentStatus.VERIFIED ->
                            "Verified"

                        PaymentStatus.CANCELLED ->
                            "Cancelled"
                    }
                )

                DetailLine(
                    "Previous Paid",
                    money(
                        payment.previousPaidAmount
                    )
                )

                DetailLine(
                    "Remaining Balance",
                    money(
                        payment.remainingAmount
                    )
                )

                if (
                    payment.transactionReference
                        .isNotBlank()
                ) {
                    DetailLine(
                        "Transaction / Reference",
                        payment.transactionReference
                    )
                }

                if (payment.remark.isNotBlank()) {
                    DetailLine(
                        "Remark",
                        payment.remark
                    )
                }
            }
        },
        confirmButton = {
            if (isVerified) {

                TextButton(
                    onClick = onOpenReceipt
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text("Open Receipt")
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
            if (isVerified) {

                TextButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun money(
    amount: Double
): String {
    return NumberFormat
        .getCurrencyInstance(
            Locale("en", "IN")
        )
        .format(amount)
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

private fun findReceiptFile(
    context: Context,
    payment: PaymentRecord
): File? {

    if (
        payment.status !=
        PaymentStatus.VERIFIED
    ) {
        return null
    }

    val directory = File(
        context.filesDir,
        "Documents/Receipts"
    )

    if (!directory.exists()) {
        return null
    }

    val safeReceiptNumber =
        payment.receiptNumber
            .replace(
                Regex("[^A-Za-z0-9._-]"),
                "_"
            )

    val exactFile = File(
        directory,
        "Receipt_$safeReceiptNumber.pdf"
    )

    if (exactFile.exists()) {
        return exactFile
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

/**
 * Student device pe admin-generated PDF nahi hota.
 * Isliye VERIFIED payment ke liye local pe PDF generate kar lo.
 */
private fun getOrCreateReceiptFile(
    context: Context,
    payment: PaymentRecord
): File? {

    if (payment.status != PaymentStatus.VERIFIED) {
        return null
    }

    findReceiptFile(context, payment)?.let {
        return it
    }

    return try {
        ReceiptGenerator.generateReceipt(
            context = context,
            payment = payment
        )
    } catch (_: Exception) {
        null
    }
}

private fun openReceipt(
    context: Context,
    file: File,
    onError: (String) -> Unit
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

    } catch (
        _: ActivityNotFoundException
    ) {

        onError(
            "PDF खोलने के लिए कोई PDF viewer app उपलब्ध नहीं है।"
        )

    } catch (
        _: Exception
    ) {

        onError(
            "Receipt खोलते समय समस्या हुई।"
        )
    }
}

private fun shareReceipt(
    context: Context,
    file: File,
    onError: (String) -> Unit
) {
    try {

        val uri: Uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        val intent = Intent(
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
                intent,
                "Share Receipt"
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
        )

    } catch (
        _: ActivityNotFoundException
    ) {

        onError(
            "Receipt share करने के लिए कोई compatible app उपलब्ध नहीं है।"
        )

    } catch (
        _: Exception
    ) {

        onError(
            "Receipt share करते समय समस्या हुई।"
        )
    }
}
