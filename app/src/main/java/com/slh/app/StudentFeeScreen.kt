package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.slh.app.ui.theme.ChipColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeeScreen(
    studentId: String,
    onBack: () -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {

    BackHandler {
        onBack()
    }

    val student =
        StudentStore.findStudent(studentId)

    if (student == null) {

        Scaffold(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),

            topBar = {

                TopAppBar(
                    title = {
                        Text(
                            text = "My Fees",
                            fontWeight =
                                FontWeight.Bold
                        )
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

                    colors =
                        TopAppBarDefaults
                            .topAppBarColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary,

                                titleContentColor =
                                    MaterialTheme.colorScheme.onPrimary,

                                navigationIconContentColor =
                                    MaterialTheme.colorScheme.onPrimary
                            )
                )
            }
        ) { paddingValues ->

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "Student information not found.",

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        return
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(refreshKey) {
        FeeStore.refreshStatuses()
    }

    /*
     * Current FeeStore API:
     * getSummary() returns StudentFeeSummary?
     */
    val summary =
        FeeStore.getSummary(
            studentId = student.id,
            coachingId = student.coachingId
        )

    /*
     * If no fee plan exists, show an empty state.
     */
    if (summary == null) {

        Scaffold(
            modifier =
                Modifier
                    .fillMaxSize(),

            bottomBar = {

                SLHBottomNavBar(
                    items = BottomNavItems.student,
                    currentRoute = "fees",
                    onNavigate = { route ->

                        if (route == "fees") {
                            // already here
                        } else if (route == "dashboard") {
                            onBack()
                        } else {
                            onNavigateTab(route)
                        }
                    }
                )
            },

            topBar = {

                TopAppBar(
                    title = {
                        Text(
                            text = "My Fees",
                            fontWeight =
                                FontWeight.Bold
                        )
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

                    colors =
                        TopAppBarDefaults
                            .topAppBarColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary,

                                titleContentColor =
                                    MaterialTheme.colorScheme.onPrimary,

                                navigationIconContentColor =
                                    MaterialTheme.colorScheme.onPrimary
                            )
                )
            }
        ) { paddingValues ->

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                NoFeePlanCard()
            }
        }

        return
    }

    val installments =
        summary.installments

    val batch =
        if (student.batchId.isNotBlank()) {
            BatchStore.findBatch(
                student.batchId
            )
        } else {
            null
        }

    /*
     * First unpaid installment is the next payment.
     *
     * This works for both:
     * installment mode:
     * 1, 2, 3
     *
     * and full payment mode:
     * 0 = Full Payment
     */
    val nextInstallment =
        installments
            .firstOrNull {
                it.paidAmount < it.amount
            }

    val overdueInstallments =
        installments.filter {
            it.status ==
                    FeeInstallmentStatus.OVERDUE
        }

    val hasOverdue =
        overdueInstallments.isNotEmpty()

    var showSubmitPaymentDialog by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    Scaffold(

        modifier =
            Modifier
                .fillMaxSize(),

        bottomBar = {

            SLHBottomNavBar(
                items = BottomNavItems.student,
                currentRoute = "fees",
                onNavigate = { route ->

                    if (route == "fees") {
                        // already here
                    } else if (route == "dashboard") {
                        onBack()
                    } else {
                        onNavigateTab(route)
                    }
                }
            )
        },

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "My Fees",
                        fontWeight =
                            FontWeight.Bold
                    )
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

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(

                            containerColor =
                                MaterialTheme.colorScheme.primary,

                            titleContentColor =
                                MaterialTheme.colorScheme.onPrimary,

                            navigationIconContentColor =
                                MaterialTheme.colorScheme.onPrimary
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
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),

            contentPadding =
                PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 28.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            item {

                StudentHeaderCard(
                    student = student,
                    batchName = batch?.name
                )
            }

            item {

                StudentFeeSummary(
                    totalFee =
                        summary.totalFee,

                    totalPaid =
                        summary.totalPaid,

                    remaining =
                        summary.remaining
                )
            }

            /*
             * Show next payment only when something
             * is still unpaid.
             */
            if (nextInstallment != null) {

                item {

                    NextPaymentCard(
                        installment =
                            nextInstallment,
                        onSubmitForVerification = {
                            showSubmitPaymentDialog = true
                        }
                    )
                }
            }

            if (hasOverdue) {

                item {

                    StudentOverdueCard(
                        installments =
                            overdueInstallments
                    )
                }
            }

            item {

                Text(
                    text =
                        "Payment Schedule",

                    fontSize = 20.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme.colorScheme.onSurface
                )
            }

            if (installments.isEmpty()) {

                item {

                    NoFeePlanCard()
                }

            } else {

                items(
                    items =
                        installments,

                    key = {
                            installment ->
                        installment.id
                    }
                ) { installment ->

                    StudentInstallmentCard(
                        installment =
                            installment,
                        studentId =
                            student.id
                    )
                }
            }

            item {
                Text(
                    text = "Payments & Receipts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val studentPayments =
                PaymentStore.findByStudent(student.id)

            if (studentPayments.isEmpty()) {
                item {
                    Text(
                        text = "Abhi koi payment record nahi.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = studentPayments,
                    key = { it.id }
                ) { payment ->
                    StudentPaymentReceiptCard(
                        payment = payment,
                        context = context
                    )
                }
            }
        }
    }

    if (showSubmitPaymentDialog && nextInstallment != null) {
        AlertDialog(
            onDismissRequest = {
                showSubmitPaymentDialog = false
            },
            title = {
                Text("Payment submit karein?")
            },
            text = {
                val remaining =
                    (nextInstallment.amount - nextInstallment.paidAmount)
                        .coerceAtLeast(0.0)
                Text(
                    "Aap bata rahe ho ki aapne fee jama kar di hai.\n\n" +
                            "Amount: ₹${String.format(Locale.US, "%.2f", remaining)}\n\n" +
                            "Admin Verify Payments se approve karega. " +
                            "Tabhi official receipt milegi."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inst = nextInstallment
                        val remaining =
                            (inst.amount - inst.paidAmount).coerceAtLeast(0.0)
                        if (remaining <= 0.0) {
                            showSubmitPaymentDialog = false
                            return@Button
                        }

                        if (
                            PaymentStore
                                .findByStudent(student.id)
                                .any {
                                    it.installmentId == inst.id &&
                                            it.status ==
                                            PaymentStatus.PENDING
                                }
                        ) {
                            Toast.makeText(
                                context,
                                "Is installment ka payment pehle se verification ke liye pending hai.",
                                Toast.LENGTH_LONG
                            ).show()
                            showSubmitPaymentDialog = false
                            return@Button
                        }
                        val summaryNow =
                            FeeStore.getSummary(
                                studentId = student.id,
                                coachingId = student.coachingId
                            )
                        val previousPaid = summaryNow?.totalPaid ?: 0.0
                        val totalFee = summaryNow?.totalFee ?: inst.amount
                        val newOverall = previousPaid + remaining
                        val newRemaining =
                            (totalFee - newOverall).coerceAtLeast(0.0)

                        val payment = PaymentRecord(
                            id = PaymentStore.nextId(),
                            receiptNumber = PaymentStore.nextReceiptNumber(
                                student.coachingId
                            ),
                            coachingId = student.coachingId,
                            studentId = student.id,
                            batchId = student.batchId,
                            installmentId = inst.id,
                            studentName = student.name,
                            studentCode = student.studentId,
                            course = student.course,
                            batchName = batch?.name ?: "",
                            amount = remaining,
                            paymentDate = FeeStore.todayDate(),
                            paymentMode = PaymentMode.OTHER,
                            transactionReference = "",
                            previousPaidAmount = previousPaid,
                            totalFee = totalFee,
                            remainingAmount = newRemaining,
                            remark = "Submitted by student for verification",
                            status = PaymentStatus.PENDING
                        )

                        /*
                         * The installment is NOT changed here. The payment stays
                         * PENDING until the admin verifies it; only then is the
                         * installment updated (on the admin's phone).
                         */
                        PaymentStore.addPayment(payment)

                        showSubmitPaymentDialog = false
                        refreshKey++
                        Toast.makeText(
                            context,
                            "Payment verification ke liye bhej diya. Admin approve karega.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) {
                    Text("Haan, submit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSubmitPaymentDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun StudentHeaderCard(
    student: Student,
    batchName: String?
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    3.dp
            )
    ) {

        Row(

            modifier =
                Modifier.padding(
                    16.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(58.dp)
                        .clip(
                            RoundedCornerShape(
                                18.dp
                            )
                        )
                        .background(
                            ChipColors.indigo.container
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Groups,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            30.dp
                        ),

                    tint =
                        ChipColors.indigo.icon
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        student.name,

                    fontSize = 19.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Student ID: ${student.studentId}",

                    fontSize = 13.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (
                    batchName != null
                ) {

                    Text(
                        text =
                            "Batch: $batchName",

                        fontSize = 13.sp,

                        color =
                            MaterialTheme.colorScheme.primary,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
private fun StudentFeeSummary(
    totalFee: Double,
    totalPaid: Double,
    remaining: Double
) {

    Column {

        Text(
            text =
                "Fee Summary",

            fontSize = 20.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            StudentSummaryCard(
                title =
                    "Total Fee",

                value =
                    money(totalFee),

                icon =
                    Icons.Default.AccountBalance,

                background =
                    ChipColors.blue.container,

                iconColor =
                    ChipColors.blue.icon,

                modifier =
                    Modifier.weight(1f)
            )

            StudentSummaryCard(
                title =
                    "Paid",

                value =
                    money(totalPaid),

                icon =
                    Icons.Default.AttachMoney,

                background =
                    ChipColors.green.container,

                iconColor =
                    ChipColors.green.icon,

                modifier =
                    Modifier.weight(1f)
            )

            StudentSummaryCard(
                title =
                    "Remaining",

                value =
                    money(remaining),

                icon =
                    Icons.Default.AttachMoney,

                background =
                    ChipColors.orange.container,

                iconColor =
                    ChipColors.orange.icon,

                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun StudentSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    background: Color,
    iconColor: Color,
    modifier: Modifier
) {

    Card(

        modifier = modifier,

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    10.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(

                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(
                            RoundedCornerShape(
                                11.dp
                            )
                        )
                        .background(
                            iconColor.copy(
                                alpha = 0.15f
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    tint =
                        iconColor
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    title,

                fontSize = 11.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text =
                    value,

                fontSize = 14.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
private fun NextPaymentCard(
    installment: FeeInstallment,
    onSubmitForVerification: () -> Unit = {}
) {

    val isOverdue =
        installment.status ==
                FeeInstallmentStatus.OVERDUE

    val background =
        if (isOverdue) {
            ChipColors.red.container
        } else {
            ChipColors.blue.container
        }

    val iconColor =
        if (isOverdue) {
            ChipColors.red.icon
        } else {
            ChipColors.blue.icon
        }

    val paymentTitle =
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
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            )
    ) {

        Column {

            Row(

                modifier =
                    Modifier.padding(
                        16.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(

                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .background(
                                iconColor.copy(
                                    alpha = 0.15f
                                )
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Today,

                        contentDescription =
                            null,

                        tint =
                            iconColor
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (isOverdue) {
                                "Payment Overdue"
                            } else {
                                "Next Payment"
                            },

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 17.sp,

                        color =
                            iconColor
                    )

                    Text(
                        text =
                            paymentTitle,

                        fontSize = 13.sp
                    )

                    Text(
                        text =
                            "Due Date: ${installment.dueDate}",

                        fontSize = 13.sp,

                        color =
                            if (isOverdue) {
                                ChipColors.red.icon
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            money(
                                (
                                        installment.amount -
                                                installment.paidAmount
                                        ).coerceAtLeast(
                                        0.0
                                    )
                            ),

                        fontSize = 17.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            iconColor
                    )

                    Text(
                        text =
                            "Remaining",

                        fontSize = 11.sp,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onSubmitForVerification,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Text("Maine fee jama kar di (Verify ke liye)")
            }
        }
    }
}


@Composable
private fun StudentOverdueCard(
    installments:
    List<FeeInstallment>
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    ChipColors.red.container
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    16.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Today,

                    contentDescription =
                        null,

                    tint =
                        ChipColors.red.icon
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    text =
                        "Overdue Payment",

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ChipColors.red.icon
                )
            }

            installments.forEach {
                    installment: FeeInstallment ->

                val paymentTitle =
                    if (
                        installment.installmentNumber == 0
                    ) {
                        "Full Payment"
                    } else {
                        "Installment ${installment.installmentNumber}"
                    }

                Text(
                    text =
                        "$paymentTitle: " +
                                "${money(
                                    (
                                            installment.amount -
                                                    installment.paidAmount
                                            ).coerceAtLeast(
                                            0.0
                                        )
                                )} remaining • " +
                                "Due ${installment.dueDate}",

                    fontSize = 13.sp,

                    color =
                        ChipColors.red.icon
                )
            }
        }
    }
}


@Composable
private fun StudentInstallmentCard(
    installment: FeeInstallment,
    studentId: String = ""
) {

    // Agar admin verify kar chuka hai to purana "pending admin verify" note mat dikhao
    val relatedVerified =
        studentId.isNotBlank() &&
                PaymentStore.findByStudent(studentId).any { p ->
                    p.installmentId == installment.id &&
                            p.status == PaymentStatus.VERIFIED
                }

    val displayNote =
        when {
            relatedVerified ->
                "Verified by admin"
            installment.paymentNote.contains(
                "pending admin verify",
                ignoreCase = true
            ) ->
                "Admin verification pending"
            else ->
                installment.paymentNote
        }

    val isPaid =
        installment.status ==
                FeeInstallmentStatus.PAID

    val isOverdue =
        installment.status ==
                FeeInstallmentStatus.OVERDUE

    val background =
        when {

            isPaid ->
                ChipColors.green.container

            isOverdue ->
                ChipColors.red.container

            else ->
                ChipColors.orange.container
        }

    val accent =
        when {

            isPaid ->
                ChipColors.green.icon

            isOverdue ->
                ChipColors.red.icon

            else ->
                ChipColors.orange.icon
        }

    val paymentTitle =
        if (
            installment.installmentNumber == 0
        ) {
            "Full Payment"
        } else {
            "Installment ${installment.installmentNumber}"
        }

    val statusText =
        when {

            isPaid ->
                "PAID"

            isOverdue ->
                "OVERDUE"

            else ->
                "PENDING"
        }

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    16.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(

                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .background(
                                accent.copy(
                                    alpha =
                                        0.15f
                                )
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.AttachMoney,

                        contentDescription =
                            null,

                        tint =
                            accent
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            paymentTitle,

                        fontSize = 17.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Due: ${installment.dueDate}",

                        fontSize = 12.sp,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StudentStatusChip(
                    text =
                        statusText,

                    color =
                        accent
                )
            }

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                FeeAmountColumn(
                    title =
                        if (
                            installment.installmentNumber == 0
                        ) {
                            "Full Fee"
                        } else {
                            "Installment"
                        },

                    amount =
                        installment.amount
                )

                FeeAmountColumn(
                    title =
                        "Paid",

                    amount =
                        installment.paidAmount
                )

                FeeAmountColumn(
                    title =
                        "Remaining",

                    amount =
                        (
                                installment.amount -
                                        installment.paidAmount
                                ).coerceAtLeast(
                                0.0
                            )
                )
            }

            if (
                installment.paidDate.isNotBlank()
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Today,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                17.dp
                            ),

                        tint =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            "Payment Date: ${installment.paidDate}",

                        fontSize = 12.sp,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (displayNote.isNotBlank()) {

                Text(
                    text =
                        "Note: $displayNote",

                    fontSize = 12.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StudentPaymentReceiptCard(
    payment: PaymentRecord,
    context: android.content.Context
) {
    val isVerified = payment.status == PaymentStatus.VERIFIED
    val isPending = payment.status == PaymentStatus.PENDING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = money(payment.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                text = "Date: ${payment.paymentDate}",
                fontSize = 13.sp
            )
            Text(
                text = when {
                    isVerified -> "Status: VERIFIED • Receipt ${payment.receiptNumber}"
                    isPending -> "Status: PENDING (admin verify ka wait)"
                    else -> "Status: ${payment.status.name}"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isVerified -> Color(0xFF2E7D32)
                    isPending -> Color(0xFFEF6C00)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (isVerified) {
                Button(
                    onClick = {
                        try {
                            val file = ReceiptGenerator.generateReceipt(
                                context = context,
                                payment = payment
                            )
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW
                            ).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(intent, "Open Receipt")
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Receipt open nahi ho paya: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Receipt dekho / download")
                }
            }
        }
    }
}


@Composable
private fun FeeAmountColumn(
    title: String,
    amount: Double
) {

    Column {

        Text(
            text =
                title,

            fontSize = 11.sp,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text =
                money(amount),

            fontSize = 14.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun StudentStatusChip(
    text: String,
    color: Color
) {

    Surface(

        color =
            color.copy(
                alpha = 0.12f
            ),

        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Text(
            text =
                text,

            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),

            fontSize = 10.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                color
        )
    }
}


@Composable
private fun NoFeePlanCard() {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        28.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.AccountBalance,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(
                        50.dp
                    ),

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    "Fee Plan Not Available",

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Your fee details have not been added yet.",

                fontSize = 13.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


private fun money(
    amount: Double
): String {

    return "₹" +
            String.format(
                Locale.getDefault(),
                "%.0f",
                amount
            )
}