package com.slh.app

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeReportScreen(
    coachingId: String,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedStatus by remember {
        mutableStateOf("ALL")
    }

    var selectedMode by remember {
        mutableStateOf("ALL")
    }

    var fromDate by remember {
        mutableStateOf("")
    }

    var toDate by remember {
        mutableStateOf("")
    }

    var showStatusMenu by remember {
        mutableStateOf(false)
    }

    var showModeMenu by remember {
        mutableStateOf(false)
    }

    var generatedFile by remember {
        mutableStateOf<File?>(null)
    }

    var showExportDialog by remember {
        mutableStateOf(false)
    }

    var exportMessage by remember {
        mutableStateOf("")
    }

    BackHandler {
        onBack()
    }

    val allPayments = remember(refreshKey) {
        PaymentStore.findByCoaching(coachingId)
            .sortedByDescending {
                it.createdAt
            }
    }

    val filteredPayments = remember(
        allPayments,
        searchText,
        selectedStatus,
        selectedMode,
        fromDate,
        toDate
    ) {

        allPayments.filter { payment ->

            val searchMatch =
                searchText.isBlank() ||
                        payment.studentName.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                        payment.studentCode.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                        payment.receiptNumber.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                        payment.transactionReference.contains(
                            searchText,
                            ignoreCase = true
                        )

            val statusMatch =
                selectedStatus == "ALL" ||
                        payment.status.name == selectedStatus

            val modeMatch =
                selectedMode == "ALL" ||
                        payment.paymentMode.name == selectedMode

            val dateMatch =
                isPaymentInsideDateRange(
                    paymentDate = payment.paymentDate,
                    fromDate = fromDate,
                    toDate = toDate
                )

            searchMatch &&
                    statusMatch &&
                    modeMatch &&
                    dateMatch
        }
    }

    val verifiedPayments =
        filteredPayments.filter {
            it.status == PaymentStatus.VERIFIED
        }

    val pendingPayments =
        filteredPayments.filter {
            it.status == PaymentStatus.PENDING
        }

    val cancelledPayments =
        filteredPayments.filter {
            it.status == PaymentStatus.CANCELLED
        }

    val totalCollection =
        verifiedPayments.sumOf {
            it.amount
        }

    val pendingCollection =
        pendingPayments.sumOf {
            it.amount
        }

    val cancelledCollection =
        cancelledPayments.sumOf {
            it.amount
        }

    val totalStudents =
        filteredPayments
            .map {
                if (it.studentId.isNotBlank()) {
                    it.studentId
                } else {
                    it.studentCode
                }
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .size

    val reportPeriod =
        buildReportPeriod(
            fromDate = fromDate,
            toDate = toDate
        )

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Fee Report",
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        if (
                            fromDate.isNotBlank() ||
                            toDate.isNotBlank()
                        ) {

                            Text(
                                text = reportPeriod,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
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
                },

                actions = {

                    IconButton(
                        onClick = {
                            refreshKey++
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Refresh,
                            contentDescription =
                                "Refresh"
                        )
                    }

                    IconButton(
                        onClick = {

                            generatedFile =
                                FeeReportExporter.generatePdf(
                                    context = context,
                                    coachingId = coachingId,
                                    payments = filteredPayments,
                                    allPayments = allPayments,
                                    reportPeriod = reportPeriod
                                )

                            exportMessage =
                                "Professional PDF fee report generated successfully."

                            showExportDialog = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.PictureAsPdf,
                            contentDescription =
                                "Export PDF"
                        )
                    }

                    IconButton(
                        onClick = {

                            generatedFile =
                                FeeReportExporter.generateExcel(
                                    context = context,
                                    coachingId = coachingId,
                                    payments = filteredPayments,
                                    allPayments = allPayments,
                                    reportPeriod = reportPeriod
                                )

                            exportMessage =
                                "Excel fee report generated successfully."

                            showExportDialog = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Download,
                            contentDescription =
                                "Export Excel"
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors()
            )
        }

    ) { innerPadding ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                OutlinedTextField(

                    value = searchText,

                    onValueChange = {
                        searchText = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Search payment")
                    },

                    placeholder = {
                        Text(
                            "Student, ID, receipt or transaction no."
                        )
                    },

                    singleLine = true,

                    shape =
                        RoundedCornerShape(12.dp)
                )
            }

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        OutlinedButton(

                            onClick = {
                                showStatusMenu = true
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(

                                when (selectedStatus) {

                                    "ALL" ->
                                        "Status: All"

                                    "PENDING" ->
                                        "Pending"

                                    "VERIFIED" ->
                                        "Verified"

                                    "CANCELLED" ->
                                        "Cancelled"

                                    else ->
                                        selectedStatus
                                },

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }

                        DropdownMenu(

                            expanded =
                                showStatusMenu,

                            onDismissRequest = {
                                showStatusMenu = false
                            }
                        ) {

                            listOf(
                                "ALL",
                                "PENDING",
                                "VERIFIED",
                                "CANCELLED"
                            ).forEach { status ->

                                DropdownMenuItem(

                                    text = {

                                        Text(

                                            when (status) {

                                                "ALL" ->
                                                    "All"

                                                "PENDING" ->
                                                    "Pending"

                                                "VERIFIED" ->
                                                    "Verified"

                                                "CANCELLED" ->
                                                    "Cancelled"

                                                else ->
                                                    status
                                            }
                                        )
                                    },

                                    onClick = {

                                        selectedStatus =
                                            status

                                        showStatusMenu =
                                            false
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        OutlinedButton(

                            onClick = {
                                showModeMenu = true
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(

                                if (
                                    selectedMode == "ALL"
                                ) {
                                    "Mode: All"
                                } else {
                                    paymentModeText(
                                        selectedMode
                                    )
                                },

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }

                        DropdownMenu(

                            expanded =
                                showModeMenu,

                            onDismissRequest = {
                                showModeMenu = false
                            }
                        ) {

                            listOf(
                                "ALL",
                                "CASH",
                                "UPI",
                                "BANK_TRANSFER",
                                "CHEQUE",
                                "OTHER"
                            ).forEach { mode ->

                                DropdownMenuItem(

                                    text = {

                                        Text(

                                            if (
                                                mode == "ALL"
                                            ) {
                                                "All"
                                            } else {
                                                paymentModeText(
                                                    mode
                                                )
                                            }
                                        )
                                    },

                                    onClick = {

                                        selectedMode =
                                            mode

                                        showModeMenu =
                                            false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                    ) {

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.CalendarMonth,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(22.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Report Period",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.Bold,
                                modifier =
                                    Modifier.weight(1f)
                            )

                            if (
                                fromDate.isNotBlank() ||
                                toDate.isNotBlank()
                            ) {

                                IconButton(
                                    onClick = {

                                        fromDate = ""
                                        toDate = ""
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Clear,
                                        contentDescription =
                                            "Clear dates"
                                    )
                                }
                            }
                        }

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

                            OutlinedButton(

                                onClick = {

                                    showDatePicker(
                                        context = context,
                                        initialDate =
                                            parseReportDate(
                                                fromDate
                                            )
                                                ?: Calendar
                                                    .getInstance(),
                                        onDateSelected = {
                                                selected ->

                                            fromDate =
                                                formatReportDate(
                                                    selected
                                                )

                                            if (
                                                toDate.isNotBlank() &&
                                                compareReportDates(
                                                    fromDate,
                                                    toDate
                                                ) > 0
                                            ) {
                                                toDate =
                                                    fromDate
                                            }
                                        }
                                    )
                                },

                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.CalendarMonth,
                                    contentDescription =
                                        null,
                                    modifier =
                                        Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(5.dp)
                                )

                                Text(
                                    text =
                                        if (
                                            fromDate.isBlank()
                                        ) {
                                            "From Date"
                                        } else {
                                            fromDate
                                        },
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(

                                onClick = {

                                    showDatePicker(
                                        context = context,
                                        initialDate =
                                            parseReportDate(
                                                toDate
                                            )
                                                ?: parseReportDate(
                                                    fromDate
                                                )
                                                ?: Calendar
                                                    .getInstance(),
                                        onDateSelected = {
                                                selected ->

                                            val newToDate =
                                                formatReportDate(
                                                    selected
                                                )

                                            if (
                                                fromDate.isBlank() ||
                                                compareReportDates(
                                                    fromDate,
                                                    newToDate
                                                ) <= 0
                                            ) {
                                                toDate =
                                                    newToDate
                                            }
                                        }
                                    )
                                },

                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.CalendarMonth,
                                    contentDescription =
                                        null,
                                    modifier =
                                        Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(5.dp)
                                )

                                Text(
                                    text =
                                        if (
                                            toDate.isBlank()
                                        ) {
                                            "To Date"
                                        } else {
                                            toDate
                                        },
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                reportPeriod,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }

            item {

                SummaryCard(
                    title =
                        "Verified Collection",
                    amount =
                        money(totalCollection),
                    subtitle =
                        "${verifiedPayments.size} verified payments"
                )
            }

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    SmallSummaryCard(
                        modifier =
                            Modifier.weight(1f),
                        title =
                            "Pending",
                        value =
                            money(pendingCollection),
                        count =
                            pendingPayments.size
                    )

                    SmallSummaryCard(
                        modifier =
                            Modifier.weight(1f),
                        title =
                            "Cancelled",
                        value =
                            money(cancelledCollection),
                        count =
                            cancelledPayments.size
                    )
                }
            }

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                    ) {

                        Text(
                            text =
                                "Report Overview",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        ReportRow(
                            label =
                                "Period",
                            value =
                                reportPeriod,
                            bold = true
                        )

                        ReportRow(
                            label =
                                "Students",
                            value =
                                totalStudents.toString()
                        )

                        ReportRow(
                            label =
                                "Payments",
                            value =
                                filteredPayments.size.toString()
                        )

                        ReportRow(
                            label =
                                "Verified",
                            value =
                                verifiedPayments.size.toString()
                        )

                        ReportRow(
                            label =
                                "Pending",
                            value =
                                pendingPayments.size.toString()
                        )

                        ReportRow(
                            label =
                                "Cancelled",
                            value =
                                cancelledPayments.size.toString()
                        )

                        ReportRow(
                            label =
                                "Total Fee",
                            value =
                                money(
                                    FeeReportExporter.calculateTotalFee(
                                        allPayments
                                    )
                                ),
                            bold = true
                        )

                        ReportRow(
                            label =
                                "Remaining",
                            value =
                                money(
                                    FeeReportExporter.calculateRemainingBalance(
                                        allPayments
                                    )
                                ),
                            bold = true
                        )
                    }
                }
            }

            item {

                Text(
                    text =
                        "Showing ${filteredPayments.size} payment(s)",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold,

                    modifier =
                        Modifier.padding(
                            top = 4.dp,
                            bottom = 2.dp
                        )
                )
            }

            if (filteredPayments.isEmpty()) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "No payment records found.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    "Try changing the search, date or other filters.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }
                }

            } else {

                items(

                    items =
                        filteredPayments,

                    key = {
                        it.id
                    }

                ) { payment ->

                    PaymentReportCard(

                        payment =
                            payment,

                        onOpenReceipt = {

                            if (
                                payment.status ==
                                PaymentStatus.VERIFIED
                            ) {

                                val file =
                                    FeeReportExporter
                                        .findReceiptFile(
                                            context,
                                            payment
                                        )

                                if (file != null) {

                                    FeeReportExporter.openFile(
                                        context,
                                        file
                                    )

                                } else {

                                    exportMessage =
                                        "Receipt file not found."

                                    generatedFile =
                                        null

                                    showExportDialog =
                                        true
                                }
                            }
                        },

                        onShareReceipt = {

                            if (
                                payment.status ==
                                PaymentStatus.VERIFIED
                            ) {

                                val file =
                                    FeeReportExporter
                                        .findReceiptFile(
                                            context,
                                            payment
                                        )

                                if (file != null) {

                                    FeeReportExporter.shareFile(
                                        context,
                                        file,
                                        "Share Payment Receipt"
                                    )

                                } else {

                                    exportMessage =
                                        "Receipt file not found."

                                    generatedFile =
                                        null

                                    showExportDialog =
                                        true
                                }
                            }
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }

    if (showExportDialog) {

        AlertDialog(

            onDismissRequest = {
                showExportDialog = false
            },

            title = {
                Text("Export Complete")
            },

            text = {

                Column {

                    Text(exportMessage)

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Period: $reportPeriod",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    generatedFile?.let { file ->

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                file.name,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            },

            confirmButton = {

                generatedFile?.let { file ->

                    Button(

                        onClick = {

                            FeeReportExporter.shareFile(
                                context =
                                    context,
                                file =
                                    file,
                                chooserTitle =
                                    "Share Fee Report"
                            )

                            showExportDialog =
                                false
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Share,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text("Share")
                    }
                }
            },

            dismissButton = {

                OutlinedButton(
                    onClick = {
                        showExportDialog =
                            false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

/* -------------------------------------------------------------------------- */
/* PAYMENT CARD                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun PaymentReportCard(
    payment: PaymentRecord,
    onOpenReceipt: () -> Unit,
    onShareReceipt: () -> Unit
) {

    val statusText =
        when (payment.status) {

            PaymentStatus.PENDING ->
                "PENDING"

            PaymentStatus.VERIFIED ->
                "VERIFIED"

            PaymentStatus.CANCELLED ->
                "CANCELLED"
        }

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(14.dp),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            payment.studentName,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        payment.studentCode.isNotBlank()
                    ) {

                        Text(
                            text =
                                payment.studentCode,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }

                StatusBadge(
                    status =
                        statusText
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            ReportRow(
                label =
                    "Amount",
                value =
                    money(payment.amount),
                bold = true
            )

            ReportRow(
                label =
                    "Date",
                value =
                    payment.paymentDate
            )

            ReportRow(
                label =
                    "Mode",
                value =
                    paymentModeText(
                        payment.paymentMode.name
                    )
            )

            if (
                payment.receiptNumber.isNotBlank()
            ) {

                ReportRow(
                    label =
                        "Receipt",
                    value =
                        payment.receiptNumber
                )
            }

            if (
                payment.transactionReference
                    .isNotBlank()
            ) {

                ReportRow(
                    label =
                        "Reference",
                    value =
                        payment.transactionReference
                )
            }

            ReportRow(
                label =
                    "Previous Paid",
                value =
                    money(
                        payment.previousPaidAmount
                    )
            )

            ReportRow(
                label =
                    "Remaining",
                value =
                    money(
                        payment.remainingAmount
                    )
            )

            if (
                payment.remark.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        "Remark: ${payment.remark}",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            if (
                payment.status ==
                PaymentStatus.VERIFIED
            ) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedButton(

                        onClick =
                            onOpenReceipt,

                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.PictureAsPdf,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text("Receipt")
                    }

                    OutlinedButton(

                        onClick =
                            onShareReceipt,

                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Share,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text("Share")
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* SUMMARY UI                                                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    subtitle: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            Text(
                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    amount,

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    subtitle,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}

@Composable
private fun SmallSummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    count: Int
) {

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(14.dp)
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
        ) {

            Text(
                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .titleSmall,

                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "$count payment(s)",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: String
) {

    val background =
        when (status) {

            "VERIFIED" ->
                MaterialTheme
                    .colorScheme
                    .primaryContainer

            "PENDING" ->
                MaterialTheme
                    .colorScheme
                    .secondaryContainer

            else ->
                MaterialTheme
                    .colorScheme
                    .errorContainer
        }

    Box(

        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(50.dp)
                )
                .background(background)
                .padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                )
    ) {

        Text(

            text =
                status,

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    bold: Boolean = false
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),

        verticalAlignment =
            Alignment.Top
    ) {

        Text(

            text =
                label,

            modifier =
                Modifier.width(110.dp),

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        Text(

            text =
                value,

            modifier =
                Modifier.weight(1f),

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}

/* -------------------------------------------------------------------------- */
/* EXPORTER                                                                   */
/* -------------------------------------------------------------------------- */

object FeeReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val LEFT = 24f
    private const val RIGHT = 571f

    private const val TOP = 30f
    private const val BOTTOM = 38f

    private const val HEADER_HEIGHT = 165f
    private const val FOOTER_HEIGHT = 30f

    private const val ROW_HEIGHT = 26f

    private const val X_STUDENT = 24f
    private const val X_ID = 98f
    private const val X_COURSE = 145f
    private const val X_BATCH = 205f
    private const val X_RECEIPT = 260f
    private const val X_DATE = 330f
    private const val X_MODE = 380f
    private const val X_AMOUNT = 430f
    private const val X_PREVIOUS = 475f
    private const val X_REMAINING = 525f

    /* ---------------------------------------------------------------------- */
    /* TOTAL FEE                                                              */
    /* ---------------------------------------------------------------------- */

    fun calculateTotalFee(
        payments: List<PaymentRecord>
    ): Double {

        return payments
            .groupBy {

                if (
                    it.studentId.isNotBlank()
                ) {
                    it.studentId
                } else {
                    it.studentCode
                }
            }
            .values
            .sumOf { studentPayments ->

                studentPayments.maxOfOrNull {
                    it.totalFee
                } ?: 0.0
            }
    }

    /* ---------------------------------------------------------------------- */
    /* REMAINING BALANCE                                                      */
    /* ---------------------------------------------------------------------- */

    fun calculateRemainingBalance(
        payments: List<PaymentRecord>
    ): Double {

        return payments
            .groupBy {

                if (
                    it.studentId.isNotBlank()
                ) {
                    it.studentId
                } else {
                    it.studentCode
                }
            }
            .values
            .sumOf { studentPayments ->

                val latest =
                    studentPayments.maxByOrNull {
                        it.createdAt
                    }

                latest?.remainingAmount ?: 0.0
            }
    }

    /* ---------------------------------------------------------------------- */
    /* PDF                                                                    */
    /* ---------------------------------------------------------------------- */

    fun generatePdf(
        context: Context,
        coachingId: String,
        payments: List<PaymentRecord>,
        allPayments: List<PaymentRecord> = payments,
        reportPeriod: String = "All Dates"
    ): File {

        val document =
            PdfDocument()

        val profile =
            CoachingProfileStore.get(
                coachingId
            )

        val coachingName =
            profile?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Sohan's Learning Hub"

        val shortName =
            profile?.shortName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "SLH"

        val tagline =
            profile?.tagline
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Learn Today. Succeed Tomorrow."

        val verifiedPayments =
            payments.filter {
                it.status ==
                        PaymentStatus.VERIFIED
            }

        val pendingPayments =
            payments.filter {
                it.status ==
                        PaymentStatus.PENDING
            }

        val cancelledPayments =
            payments.filter {
                it.status ==
                        PaymentStatus.CANCELLED
            }

        val verifiedAmount =
            verifiedPayments.sumOf {
                it.amount
            }

        val pendingAmount =
            pendingPayments.sumOf {
                it.amount
            }

        val cancelledAmount =
            cancelledPayments.sumOf {
                it.amount
            }

        val totalFee =
            calculateTotalFee(
                allPayments
            )

        val remainingBalance =
            calculateRemainingBalance(
                allPayments
            )

        val uniqueStudents =
            payments
                .map {

                    if (
                        it.studentId.isNotBlank()
                    ) {
                        it.studentId
                    } else {
                        it.studentCode
                    }
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .size

        val modeSummary =
            calculateModeSummary(
                verifiedPayments
            )

        val dateSummary =
            calculateDateSummary(
                verifiedPayments
            )

        val rowsPerPage =
            calculateRowsPerPage()

        val detailPageCount =
            if (payments.isEmpty()) {
                1
            } else {
                (
                        payments.size +
                                rowsPerPage -
                                1
                        ) / rowsPerPage
            }

        val totalPages =
            detailPageCount + 1

        var currentPage =
            1

        var page =
            createPage(
                document,
                currentPage
            )

        var canvas =
            page.canvas

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        drawPageHeader(
            canvas = canvas,
            paint = paint,
            shortName = shortName,
            coachingName = coachingName,
            tagline = tagline,
            verifiedAmount = verifiedAmount,
            pendingAmount = pendingAmount,
            cancelledAmount = cancelledAmount,
            reportPeriod = reportPeriod,
            pageNumber = currentPage,
            totalPages = totalPages
        )

        var y =
            TOP + HEADER_HEIGHT

        if (payments.isEmpty()) {

            paint.textSize =
                11f

            canvas.drawText(
                "No payment records found.",
                LEFT,
                y,
                paint
            )

        } else {

            payments.forEach { payment ->

                if (
                    y + ROW_HEIGHT >
                    PAGE_HEIGHT -
                    BOTTOM -
                    FOOTER_HEIGHT
                ) {

                    drawPageFooter(
                        canvas = canvas,
                        paint = paint,
                        pageNumber = currentPage,
                        totalPages = totalPages
                    )

                    document.finishPage(
                        page
                    )

                    currentPage++

                    page =
                        createPage(
                            document,
                            currentPage
                        )

                    canvas =
                        page.canvas

                    drawPageHeader(
                        canvas = canvas,
                        paint = paint,
                        shortName = shortName,
                        coachingName = coachingName,
                        tagline = tagline,
                        verifiedAmount = verifiedAmount,
                        pendingAmount = pendingAmount,
                        cancelledAmount = cancelledAmount,
                        reportPeriod = reportPeriod,
                        pageNumber = currentPage,
                        totalPages = totalPages
                    )

                    y =
                        TOP + HEADER_HEIGHT
                }

                drawPaymentRow(
                    canvas = canvas,
                    paint = paint,
                    payment = payment,
                    y = y
                )

                y += ROW_HEIGHT
            }
        }

        drawPageFooter(
            canvas = canvas,
            paint = paint,
            pageNumber = currentPage,
            totalPages = totalPages
        )

        document.finishPage(
            page
        )

        currentPage++

        val summaryPage =
            createPage(
                document,
                currentPage
            )

        drawFinalSummaryPage(
            canvas = summaryPage.canvas,
            paint = paint,
            shortName = shortName,
            coachingName = coachingName,
            tagline = tagline,
            reportPeriod = reportPeriod,
            totalFee = totalFee,
            verifiedAmount = verifiedAmount,
            pendingAmount = pendingAmount,
            cancelledAmount = cancelledAmount,
            remainingBalance = remainingBalance,
            uniqueStudents = uniqueStudents,
            totalPayments = payments.size,
            verifiedCount = verifiedPayments.size,
            pendingCount = pendingPayments.size,
            cancelledCount = cancelledPayments.size,
            modeSummary = modeSummary,
            dateSummary = dateSummary,
            pageNumber = currentPage,
            totalPages = totalPages
        )

        drawPageFooter(
            canvas = summaryPage.canvas,
            paint = paint,
            pageNumber = currentPage,
            totalPages = totalPages
        )

        document.finishPage(
            summaryPage
        )

        val directory =
            File(
                context.filesDir,
                "Documents/FeeReports"
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file =
            File(
                directory,
                "Fee_Report_${fileTimestamp()}.pdf"
            )

        file.outputStream().use { output ->

            document.writeTo(
                output
            )
        }

        document.close()

        return file
    }

    private fun calculateRowsPerPage(): Int {

        val usableHeight =
            PAGE_HEIGHT -
                    TOP -
                    HEADER_HEIGHT -
                    BOTTOM -
                    FOOTER_HEIGHT

        return (
                usableHeight /
                        ROW_HEIGHT
                )
            .toInt()
            .coerceAtLeast(1)
    }

    /* ---------------------------------------------------------------------- */
    /* MODE SUMMARY                                                            */
    /* ---------------------------------------------------------------------- */

    private fun calculateModeSummary(
        payments: List<PaymentRecord>
    ): Map<String, Double> {

        return payments
            .groupBy {
                it.paymentMode.name
            }
            .mapValues { entry ->

                entry.value.sumOf {
                    it.amount
                }
            }
    }

    /* ---------------------------------------------------------------------- */
    /* DATE SUMMARY                                                            */
    /* ---------------------------------------------------------------------- */

    private fun calculateDateSummary(
        payments: List<PaymentRecord>
    ): List<Pair<String, Double>> {

        return payments
            .groupBy {
                it.paymentDate
            }
            .map { entry ->

                entry.key to
                        entry.value.sumOf {
                            it.amount
                        }
            }
            .sortedByDescending {
                parseAnyPaymentDate(
                    it.first
                )?.time ?: 0L
            }
    }

    /* ---------------------------------------------------------------------- */
    /* CREATE PAGE                                                             */
    /* ---------------------------------------------------------------------- */

    private fun createPage(
        document: PdfDocument,
        pageNumber: Int
    ): PdfDocument.Page {

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageNumber
            ).create()

        return document.startPage(
            pageInfo
        )
    }

    /* ---------------------------------------------------------------------- */
    /* PAGE HEADER                                                             */
    /* ---------------------------------------------------------------------- */

    private fun drawPageHeader(
        canvas: android.graphics.Canvas,
        paint: Paint,
        shortName: String,
        coachingName: String,
        tagline: String,
        verifiedAmount: Double,
        pendingAmount: Double,
        cancelledAmount: Double,
        reportPeriod: String,
        pageNumber: Int,
        totalPages: Int
    ) {

        var y =
            TOP + 5f

        paint.textSize =
            18f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            shortName.take(28),
            LEFT,
            y,
            paint
        )

        y += 21f

        paint.textSize =
            13f

        canvas.drawText(
            coachingName.take(60),
            LEFT,
            y,
            paint
        )

        y += 17f

        paint.textSize =
            8f

        paint.isFakeBoldText =
            false

        canvas.drawText(
            tagline.take(90),
            LEFT,
            y,
            paint
        )

        y += 22f

        paint.textSize =
            14f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "FEE PAYMENT REPORT",
            LEFT,
            y,
            paint
        )

        paint.textSize =
            8f

        paint.isFakeBoldText =
            false

        canvas.drawText(
            "Page $pageNumber of $totalPages",
            505f,
            y,
            paint
        )

        y += 18f

        paint.textSize =
            8f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "Report Period: $reportPeriod",
            LEFT,
            y,
            paint
        )

        paint.isFakeBoldText =
            false

        y += 15f

        canvas.drawText(
            "Generated: ${currentDateTime()}",
            LEFT,
            y,
            paint
        )

        y += 16f

        canvas.drawText(
            "Verified: ${money(verifiedAmount)}",
            LEFT,
            y,
            paint
        )

        canvas.drawText(
            "Pending: ${money(pendingAmount)}",
            195f,
            y,
            paint
        )

        canvas.drawText(
            "Cancelled: ${money(cancelledAmount)}",
            390f,
            y,
            paint
        )

        y += 18f

        drawTableHeader(
            canvas = canvas,
            paint = paint,
            y = y
        )
    }

    /* ---------------------------------------------------------------------- */
    /* TABLE HEADER                                                            */
    /* ---------------------------------------------------------------------- */

    private fun drawTableHeader(
        canvas: android.graphics.Canvas,
        paint: Paint,
        y: Float
    ) {

        paint.textSize =
            6.7f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "Student",
            X_STUDENT,
            y,
            paint
        )

        canvas.drawText(
            "ID",
            X_ID,
            y,
            paint
        )

        canvas.drawText(
            "Course",
            X_COURSE,
            y,
            paint
        )

        canvas.drawText(
            "Batch",
            X_BATCH,
            y,
            paint
        )

        canvas.drawText(
            "Receipt",
            X_RECEIPT,
            y,
            paint
        )

        canvas.drawText(
            "Date",
            X_DATE,
            y,
            paint
        )

        canvas.drawText(
            "Mode",
            X_MODE,
            y,
            paint
        )

        canvas.drawText(
            "Amount",
            X_AMOUNT,
            y,
            paint
        )

        canvas.drawText(
            "Prev.",
            X_PREVIOUS,
            y,
            paint
        )

        canvas.drawText(
            "Remain.",
            X_REMAINING,
            y,
            paint
        )

        paint.isFakeBoldText =
            false

        canvas.drawLine(
            LEFT,
            y + 7f,
            RIGHT,
            y + 7f,
            paint
        )
    }

    /* ---------------------------------------------------------------------- */
    /* PAYMENT ROW                                                             */
    /* ---------------------------------------------------------------------- */

    private fun drawPaymentRow(
        canvas: android.graphics.Canvas,
        paint: Paint,
        payment: PaymentRecord,
        y: Float
    ) {

        paint.textSize =
            6.4f

        paint.isFakeBoldText =
            false

        canvas.drawText(
            compactText(
                payment.studentName,
                13
            ),
            X_STUDENT,
            y,
            paint
        )

        canvas.drawText(
            compactText(
                payment.studentCode,
                8
            ),
            X_ID,
            y,
            paint
        )

        canvas.drawText(
            compactText(
                payment.course,
                10
            ),
            X_COURSE,
            y,
            paint
        )

        canvas.drawText(
            compactText(
                payment.batchName
                    .ifBlank {
                        payment.batchId
                    },
                9
            ),
            X_BATCH,
            y,
            paint
        )

        canvas.drawText(
            compactText(
                payment.receiptNumber,
                11
            ),
            X_RECEIPT,
            y,
            paint
        )

        canvas.drawText(
            compactText(
                payment.paymentDate,
                9
            ),
            X_DATE,
            y,
            paint
        )

        canvas.drawText(
            compactText(
                paymentModeText(
                    payment.paymentMode.name
                ),
                8
            ),
            X_MODE,
            y,
            paint
        )

        canvas.drawText(
            money(payment.amount),
            X_AMOUNT,
            y,
            paint
        )

        canvas.drawText(
            money(
                payment.previousPaidAmount
            ),
            X_PREVIOUS,
            y,
            paint
        )

        canvas.drawText(
            money(
                payment.remainingAmount
            ),
            X_REMAINING,
            y,
            paint
        )

        paint.textSize =
            5f

        canvas.drawLine(
            LEFT,
            y + 8f,
            RIGHT,
            y + 8f,
            paint
        )
    }

    /* ---------------------------------------------------------------------- */
    /* FINAL ACCOUNTING SUMMARY PAGE                                          */
    /* ---------------------------------------------------------------------- */

    private fun drawFinalSummaryPage(
        canvas: android.graphics.Canvas,
        paint: Paint,
        shortName: String,
        coachingName: String,
        tagline: String,
        reportPeriod: String,
        totalFee: Double,
        verifiedAmount: Double,
        pendingAmount: Double,
        cancelledAmount: Double,
        remainingBalance: Double,
        uniqueStudents: Int,
        totalPayments: Int,
        verifiedCount: Int,
        pendingCount: Int,
        cancelledCount: Int,
        modeSummary: Map<String, Double>,
        dateSummary: List<Pair<String, Double>>,
        pageNumber: Int,
        totalPages: Int
    ) {

        var y =
            TOP + 5f

        paint.textSize =
            18f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            shortName.take(28),
            LEFT,
            y,
            paint
        )

        y += 21f

        paint.textSize =
            13f

        canvas.drawText(
            coachingName.take(60),
            LEFT,
            y,
            paint
        )

        y += 17f

        paint.textSize =
            8f

        paint.isFakeBoldText =
            false

        canvas.drawText(
            tagline.take(90),
            LEFT,
            y,
            paint
        )

        y += 30f

        paint.textSize =
            16f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "ACCOUNTING SUMMARY",
            LEFT,
            y,
            paint
        )

        paint.textSize =
            8f

        paint.isFakeBoldText =
            false

        canvas.drawText(
            "Page $pageNumber of $totalPages",
            505f,
            y,
            paint
        )

        y += 22f

        paint.textSize =
            8f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "Report Period: $reportPeriod",
            LEFT,
            y,
            paint
        )

        y += 28f

        paint.textSize =
            11f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "Overall Summary",
            LEFT,
            y,
            paint
        )

        y += 22f

        paint.textSize =
            9f

        paint.isFakeBoldText =
            false

        drawSummaryLine(
            canvas,
            paint,
            "Total Fee",
            money(totalFee),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Verified Collection",
            money(verifiedAmount),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Pending Amount",
            money(pendingAmount),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Cancelled Amount",
            money(cancelledAmount),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Remaining Balance",
            money(remainingBalance),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Total Students",
            uniqueStudents.toString(),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Total Payments",
            totalPayments.toString(),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Verified Payments",
            verifiedCount.toString(),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Pending Payments",
            pendingCount.toString(),
            y
        )

        y += 20f

        drawSummaryLine(
            canvas,
            paint,
            "Cancelled Payments",
            cancelledCount.toString(),
            y
        )

        y += 32f

        paint.textSize =
            11f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "Payment Mode Summary",
            LEFT,
            y,
            paint
        )

        y += 22f

        paint.textSize =
            9f

        paint.isFakeBoldText =
            false

        val modeOrder =
            listOf(
                "CASH",
                "UPI",
                "BANK_TRANSFER",
                "CHEQUE",
                "OTHER"
            )

        modeOrder.forEach { mode ->

            val amount =
                modeSummary[mode] ?: 0.0

            drawSummaryLine(
                canvas,
                paint,
                paymentModeText(mode),
                money(amount),
                y
            )

            y += 19f
        }

        y += 18f

        paint.textSize =
            11f

        paint.isFakeBoldText =
            true

        canvas.drawText(
            "Date-wise Collection",
            LEFT,
            y,
            paint
        )

        y += 22f

        paint.textSize =
            8f

        paint.isFakeBoldText =
            false

        val maxDateRows =
            10

        dateSummary
            .take(maxDateRows)
            .forEach { item ->

                drawSummaryLine(
                    canvas,
                    paint,
                    item.first,
                    money(item.second),
                    y
                )

                y += 17f
            }

        if (
            dateSummary.size >
            maxDateRows
        ) {

            paint.textSize =
                7f

            canvas.drawText(
                "... additional dates are included in the detailed report.",
                LEFT,
                y + 2f,
                paint
            )

            y += 18f
        }

        y += 18f

        paint.textSize =
            8f

        paint.isFakeBoldText =
            false

        canvas.drawLine(
            LEFT,
            y,
            RIGHT,
            y,
            paint
        )

        y += 18f

        canvas.drawText(
            "This report is computer generated.",
            LEFT,
            y,
            paint
        )

        y += 15f

        canvas.drawText(
            "Prepared from the payment records stored in the SLH application.",
            LEFT,
            y,
            paint
        )
    }

    private fun drawSummaryLine(
        canvas: android.graphics.Canvas,
        paint: Paint,
        label: String,
        value: String,
        y: Float
    ) {

        canvas.drawText(
            label,
            LEFT,
            y,
            paint
        )

        canvas.drawText(
            value,
            430f,
            y,
            paint
        )
    }

    /* ---------------------------------------------------------------------- */
    /* FOOTER                                                                  */
    /* ---------------------------------------------------------------------- */

    private fun drawPageFooter(
        canvas: android.graphics.Canvas,
        paint: Paint,
        pageNumber: Int,
        totalPages: Int
    ) {

        paint.textSize =
            7f

        paint.isFakeBoldText =
            false

        canvas.drawLine(
            LEFT,
            PAGE_HEIGHT - 31f,
            RIGHT,
            PAGE_HEIGHT - 31f,
            paint
        )

        canvas.drawText(
            "Computer generated fee report",
            LEFT,
            PAGE_HEIGHT - 17f,
            paint
        )

        canvas.drawText(
            "Page $pageNumber of $totalPages",
            500f,
            PAGE_HEIGHT - 17f,
            paint
        )
    }

    /* ---------------------------------------------------------------------- */
    /* EXCEL                                                                    */
    /* ---------------------------------------------------------------------- */

    fun generateExcel(
        context: Context,
        coachingId: String,
        payments: List<PaymentRecord>,
        allPayments: List<PaymentRecord> = payments,
        reportPeriod: String = "All Dates"
    ): File {

        val profile =
            CoachingProfileStore.get(
                coachingId
            )

        val coachingName =
            profile?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Sohan's Learning Hub"

        val verifiedAmount =
            payments
                .filter {
                    it.status ==
                            PaymentStatus.VERIFIED
                }
                .sumOf {
                    it.amount
                }

        val pendingAmount =
            payments
                .filter {
                    it.status ==
                            PaymentStatus.PENDING
                }
                .sumOf {
                    it.amount
                }

        val cancelledAmount =
            payments
                .filter {
                    it.status ==
                            PaymentStatus.CANCELLED
                }
                .sumOf {
                    it.amount
                }

        val totalFee =
            calculateTotalFee(
                allPayments
            )

        val remainingBalance =
            calculateRemainingBalance(
                allPayments
            )

        val html =
            buildString {

                append(
                    """
                    <html>
                    <head>
                    <meta charset="UTF-8">
                    <style>
                    body {
                        font-family: Arial;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                    }
                    th, td {
                        border: 1px solid #999999;
                        padding: 6px;
                    }
                    th {
                        font-weight: bold;
                    }
                    .summary {
                        font-weight: bold;
                    }
                    </style>
                    </head>
                    <body>

                    <h2>${escapeHtml(coachingName)}</h2>

                    <h3>Fee Payment Report</h3>

                    <p class="summary">
                    Report Period:
                    ${escapeHtml(reportPeriod)}
                    </p>

                    <p>
                    Generated:
                    ${escapeHtml(currentDateTime())}
                    </p>

                    <h3>Accounting Summary</h3>

                    <p>Total Fee: $totalFee</p>
                    <p>Verified Collection: $verifiedAmount</p>
                    <p>Pending Amount: $pendingAmount</p>
                    <p>Cancelled Amount: $cancelledAmount</p>
                    <p>Remaining Balance: $remainingBalance</p>

                    <table>

                    <tr>
                        <th>Student</th>
                        <th>Student ID</th>
                        <th>Course</th>
                        <th>Batch</th>
                        <th>Receipt No.</th>
                        <th>Date</th>
                        <th>Amount</th>
                        <th>Mode</th>
                        <th>Transaction Reference</th>
                        <th>Previous Paid</th>
                        <th>Remaining</th>
                        <th>Status</th>
                        <th>Remark</th>
                    </tr>
                    """.trimIndent()
                )

                payments.forEach { payment ->

                    append(
                        """
                        <tr>
                            <td>${escapeHtml(payment.studentName)}</td>
                            <td>${escapeHtml(payment.studentCode)}</td>
                            <td>${escapeHtml(payment.course)}</td>
                            <td>${escapeHtml(payment.batchName.ifBlank { payment.batchId })}</td>
                            <td>${escapeHtml(payment.receiptNumber)}</td>
                            <td>${escapeHtml(payment.paymentDate)}</td>
                            <td>${payment.amount}</td>
                            <td>${escapeHtml(paymentModeText(payment.paymentMode.name))}</td>
                            <td>${escapeHtml(payment.transactionReference)}</td>
                            <td>${payment.previousPaidAmount}</td>
                            <td>${payment.remainingAmount}</td>
                            <td>${escapeHtml(payment.status.name)}</td>
                            <td>${escapeHtml(payment.remark)}</td>
                        </tr>
                        """.trimIndent()
                    )
                }

                append(
                    """
                    </table>

                    <h3>Payment Mode Summary</h3>

                    <table>
                    <tr>
                        <th>Mode</th>
                        <th>Amount</th>
                    </tr>
                    """.trimIndent()
                )

                calculateModeSummary(
                    payments.filter {
                        it.status ==
                                PaymentStatus.VERIFIED
                    }
                ).forEach { entry ->

                    append(
                        """
                        <tr>
                            <td>${escapeHtml(paymentModeText(entry.key))}</td>
                            <td>${entry.value}</td>
                        </tr>
                        """.trimIndent()
                    )
                }

                append(
                    """
                    </table>

                    <h3>Date-wise Collection</h3>

                    <table>
                    <tr>
                        <th>Date</th>
                        <th>Collection</th>
                    </tr>
                    """.trimIndent()
                )

                calculateDateSummary(
                    payments.filter {
                        it.status ==
                                PaymentStatus.VERIFIED
                    }
                ).forEach { entry ->

                    append(
                        """
                        <tr>
                            <td>${escapeHtml(entry.first)}</td>
                            <td>${entry.second}</td>
                        </tr>
                        """.trimIndent()
                    )
                }

                append(
                    """
                    </table>

                    </body>
                    </html>
                    """.trimIndent()
                )
            }

        val directory =
            File(
                context.filesDir,
                "Documents/FeeReports"
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file =
            File(
                directory,
                "Fee_Report_${fileTimestamp()}.xls"
            )

        file.writeText(
            html,
            Charsets.UTF_8
        )

        return file
    }

    /* ---------------------------------------------------------------------- */
    /* RECEIPT FILE                                                            */
    /* ---------------------------------------------------------------------- */

    fun findReceiptFile(
        context: Context,
        payment: PaymentRecord
    ): File? {

        if (
            payment.receiptNumber.isBlank()
        ) {
            return null
        }

        val directory =
            File(
                context.filesDir,
                "Documents/Receipts"
            )

        val file =
            File(
                directory,
                "Receipt_${safeFileName(payment.receiptNumber)}.pdf"
            )

        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    /* ---------------------------------------------------------------------- */
    /* OPEN FILE                                                               */
    /* ---------------------------------------------------------------------- */

    fun openFile(
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

            val mimeType =
                if (
                    file.extension.equals(
                        "pdf",
                        ignoreCase = true
                    )
                ) {
                    "application/pdf"
                } else {
                    "application/vnd.ms-excel"
                }

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        mimeType
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            context.startActivity(
                intent
            )

        } catch (_: Exception) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /* SHARE FILE                                                              */
    /* ---------------------------------------------------------------------- */

    fun shareFile(
        context: Context,
        file: File,
        chooserTitle: String
    ) {

        try {

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

            val mimeType =
                if (
                    file.extension.equals(
                        "pdf",
                        ignoreCase = true
                    )
                ) {
                    "application/pdf"
                } else {
                    "application/vnd.ms-excel"
                }

            val intent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        mimeType

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
                    chooserTitle
                )
            )

        } catch (_: Exception) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /* TEXT HELPERS                                                            */
    /* ---------------------------------------------------------------------- */

    private fun compactText(
        value: String,
        maxLength: Int
    ): String {

        val clean =
            value
                .replace(
                    "\n",
                    " "
                )
                .replace(
                    "\r",
                    " "
                )
                .trim()

        if (
            clean.length <= maxLength
        ) {
            return clean
        }

        if (
            maxLength <= 1
        ) {
            return clean.take(
                maxLength
            )
        }

        return clean
            .take(
                maxLength - 1
            )
            .trimEnd() +
                "…"
    }

    private fun escapeHtml(
        value: String
    ): String {

        return value
            .replace(
                "&",
                "&amp;"
            )
            .replace(
                "<",
                "&lt;"
            )
            .replace(
                ">",
                "&gt;"
            )
            .replace(
                "\"",
                "&quot;"
            )
            .replace(
                "'",
                "&#39;"
            )
    }
}

/* -------------------------------------------------------------------------- */
/* DATE RANGE HELPERS                                                         */
/* -------------------------------------------------------------------------- */

private fun showDatePicker(
    context: Context,
    initialDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {

    val year =
        initialDate.get(
            Calendar.YEAR
        )

    val month =
        initialDate.get(
            Calendar.MONTH
        )

    val day =
        initialDate.get(
            Calendar.DAY_OF_MONTH
        )

    DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->

            val selected =
                Calendar.getInstance()

            selected.set(
                Calendar.YEAR,
                selectedYear
            )

            selected.set(
                Calendar.MONTH,
                selectedMonth
            )

            selected.set(
                Calendar.DAY_OF_MONTH,
                selectedDay
            )

            selected.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            selected.set(
                Calendar.MINUTE,
                0
            )

            selected.set(
                Calendar.SECOND,
                0
            )

            selected.set(
                Calendar.MILLISECOND,
                0
            )

            onDateSelected(
                selected
            )
        },
        year,
        month,
        day
    ).show()
}

private fun formatReportDate(
    calendar: Calendar
): String {

    return SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.getDefault()
    ).format(
        calendar.time
    )
}

private fun parseReportDate(
    value: String
): Calendar? {

    if (
        value.isBlank()
    ) {
        return null
    }

    val date =
        parseAnyPaymentDate(
            value
        )
            ?: return null

    return Calendar.getInstance().apply {

        time =
            date

        set(
            Calendar.HOUR_OF_DAY,
            0
        )

        set(
            Calendar.MINUTE,
            0
        )

        set(
            Calendar.SECOND,
            0
        )

        set(
            Calendar.MILLISECOND,
            0
        )
    }
}

private fun parseAnyPaymentDate(
    value: String
): Date? {

    if (
        value.isBlank()
    ) {
        return null
    }

    val patterns =
        listOf(
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy HH:mm",
            "yyyy-MM-dd HH:mm"
        )

    for (pattern in patterns) {

        try {

            return SimpleDateFormat(
                pattern,
                Locale.getDefault()
            ).apply {
                isLenient = false
            }.parse(
                value
            )

        } catch (_: Exception) {
        }
    }

    return null
}

private fun compareReportDates(
    first: String,
    second: String
): Int {

    val firstDate =
        parseAnyPaymentDate(
            first
        )

    val secondDate =
        parseAnyPaymentDate(
            second
        )

    if (
        firstDate == null ||
        secondDate == null
    ) {
        return first.compareTo(
            second
        )
    }

    return firstDate.compareTo(
        secondDate
    )
}

private fun isPaymentInsideDateRange(
    paymentDate: String,
    fromDate: String,
    toDate: String
): Boolean {

    if (
        fromDate.isBlank() &&
        toDate.isBlank()
    ) {
        return true
    }

    val payment =
        parseAnyPaymentDate(
            paymentDate
        )
            ?: return true

    val start =
        parseAnyPaymentDate(
            fromDate
        )

    val end =
        parseAnyPaymentDate(
            toDate
        )

    val paymentTime =
        payment.time

    val startTime =
        start?.time

    val endTime =
        end?.time?.plus(
            24L * 60L * 60L * 1000L - 1L
        )

    if (
        startTime != null &&
        paymentTime < startTime
    ) {
        return false
    }

    if (
        endTime != null &&
        paymentTime > endTime
    ) {
        return false
    }

    return true
}

private fun buildReportPeriod(
    fromDate: String,
    toDate: String
): String {

    return when {

        fromDate.isBlank() &&
                toDate.isBlank() ->
            "All Dates"

        fromDate.isNotBlank() &&
                toDate.isBlank() ->
            "From $fromDate"

        fromDate.isBlank() &&
                toDate.isNotBlank() ->
            "Up to $toDate"

        else ->
            "$fromDate to $toDate"
    }
}

/* -------------------------------------------------------------------------- */
/* GLOBAL HELPERS                                                             */
/* -------------------------------------------------------------------------- */

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

private fun paymentModeText(
    mode: String
): String {

    return when (mode) {

        "CASH" ->
            "Cash"

        "UPI" ->
            "UPI"

        "BANK_TRANSFER" ->
            "Bank Transfer"

        "CHEQUE" ->
            "Cheque"

        "OTHER" ->
            "Other"

        else ->
            mode.replace(
                "_",
                " "
            )
    }
}

private fun currentDateTime(): String {

    return SimpleDateFormat(
        "dd-MM-yyyy HH:mm",
        Locale.getDefault()
    ).format(
        Date()
    )
}

private fun fileTimestamp(): String {

    return SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.getDefault()
    ).format(
        Date()
    )
}

private fun safeFileName(
    value: String
): String {

    return value.replace(
        Regex(
            "[^A-Za-z0-9._-]"
        ),
        "_"
    )
}