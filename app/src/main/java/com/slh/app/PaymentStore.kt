package com.slh.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================
// PAYMENT MODE
// =========================================================

enum class PaymentMode(
    val title: String
) {
    CASH("Cash"),
    UPI("UPI"),
    BANK_TRANSFER("Bank Transfer"),
    CHEQUE("Cheque"),
    OTHER("Other")
}

// =========================================================
// PAYMENT STATUS
// =========================================================

enum class PaymentStatus(
    val title: String
) {
    PENDING("Pending"),
    VERIFIED("Verified"),
    CANCELLED("Cancelled")
}

// =========================================================
// PAYMENT DATA MODEL
// =========================================================

data class PaymentRecord(

    val id: String,

    val receiptNumber: String,

    val coachingId: String,

    val studentId: String,

    val batchId: String = "",

    val installmentId: String = "",

    val studentName: String,

    val studentCode: String = "",

    val course: String = "",

    val batchName: String = "",

    val amount: Double,

    val paymentDate: String,

    val paymentMode: PaymentMode,

    val transactionReference: String = "",

    val previousPaidAmount: Double = 0.0,

    val totalFee: Double = 0.0,

    val remainingAmount: Double = 0.0,

    val remark: String = "",

    val status: PaymentStatus = PaymentStatus.VERIFIED,

    val createdAt: Long = System.currentTimeMillis()
)

// =========================================================
// PAYMENT STORE
// =========================================================

object PaymentStore {

    private val paymentState =
        androidx.compose.runtime.mutableStateOf(
            emptyList<PaymentRecord>()
        )

    private var initialized =
        false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /*
     * Every persist() pushes the difference to Firestore
     * (see CloudDiffSync).
     */
    private val paymentSync =
        CloudDiffSync<PaymentRecord>(
            idOf = { PaymentFirestoreRepository.docId(it) },
            upsert = {
                PaymentFirestoreRepository.savePayment(it)
            },
            delete = {
                PaymentFirestoreRepository.deletePaymentDoc(it)
            }
        )

    // =====================================================
    // INITIALIZE
    // =====================================================

    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        val saved =
            SLHLocalStorage.loadPayments()

        if (saved != null) {

            paymentState.value =
                saved

            // Baseline for cloud sync: what is on disk is not "new".
            paymentSync.reset(saved)

        } else {

            paymentState.value =
                emptyList()

            persist()
        }
    }

    // =====================================================
    // ALL PAYMENTS
    // =====================================================

    val payments: List<PaymentRecord>
        get() =
            paymentState.value

    fun getAll(): List<PaymentRecord> {

        return paymentState.value
    }

    // =====================================================
    // FIND PAYMENT
    // =====================================================

    fun getById(
        id: String
    ): PaymentRecord? {

        return paymentState.value
            .find {
                it.id == id
            }
    }

    // =====================================================
    // STUDENT PAYMENTS
    // =====================================================

    fun findByStudent(
        studentId: String
    ): List<PaymentRecord> {

        return paymentState.value
            .filter {
                it.studentId == studentId
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    // =====================================================
    // COACHING PAYMENTS
    // =====================================================

    fun findByCoaching(
        coachingId: String
    ): List<PaymentRecord> {

        return paymentState.value
            .filter {
                it.coachingId == coachingId
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    // =====================================================
    // BATCH PAYMENTS
    // =====================================================

    fun findByBatch(
        batchId: String
    ): List<PaymentRecord> {

        return paymentState.value
            .filter {
                it.batchId == batchId
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    // =====================================================
    // DATE FILTER
    // =====================================================

    fun findByDateRange(
        coachingId: String,
        fromDate: String,
        toDate: String
    ): List<PaymentRecord> {

        return paymentState.value
            .filter { payment ->

                payment.coachingId == coachingId &&
                        payment.paymentDate >= fromDate &&
                        payment.paymentDate <= toDate
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    // =====================================================
    // STATUS FILTER
    // =====================================================

    fun findByStatus(
        coachingId: String,
        status: PaymentStatus
    ): List<PaymentRecord> {

        return paymentState.value
            .filter {
                it.coachingId == coachingId &&
                        it.status == status
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    // =====================================================
    // PAYMENT MODE FILTER
    // =====================================================

    fun findByPaymentMode(
        coachingId: String,
        paymentMode: PaymentMode
    ): List<PaymentRecord> {

        return paymentState.value
            .filter {
                it.coachingId == coachingId &&
                        it.paymentMode == paymentMode
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    // =====================================================
    // ADD PAYMENT
    // =====================================================

    fun addPayment(
        payment: PaymentRecord
    ): Boolean {

        if (
            payment.id.isBlank()
        ) {
            return false
        }

        if (
            payment.receiptNumber.isBlank()
        ) {
            return false
        }

        if (
            payment.amount <= 0.0
        ) {
            return false
        }

        if (
            paymentState.value.any {
                it.id == payment.id
            }
        ) {
            return false
        }

        if (
            paymentState.value.any {
                it.receiptNumber.equals(
                    payment.receiptNumber,
                    ignoreCase = true
                )
            }
        ) {
            return false
        }

        paymentState.value =
            paymentState.value +
                    payment

        persist()

        return true
    }

    // =====================================================
    // UPDATE PAYMENT
    // =====================================================

    fun updatePayment(
        payment: PaymentRecord
    ): Boolean {

        val index =
            paymentState.value.indexOfFirst {
                it.id == payment.id
            }

        if (
            index < 0
        ) {
            return false
        }

        val updated =
            paymentState.value
                .toMutableList()

        updated[index] =
            payment

        paymentState.value =
            updated

        persist()

        return true
    }

    // =====================================================
    // VERIFY PAYMENT
    // =====================================================

    fun verifyPayment(
        paymentId: String
    ): Boolean {

        val payment =
            getById(
                paymentId
            )
                ?: return false

        /*
         * A payment submitted by a student carries a temporary
         * receipt number generated on the student's phone. The
         * admin's list is complete, so the OFFICIAL number is
         * assigned here, at verification.
         */
        val officialReceipt =
            if (payment.status == PaymentStatus.PENDING) {
                nextReceiptNumber(payment.coachingId)
            } else {
                payment.receiptNumber
            }

        return updatePayment(
            payment.copy(
                status =
                    PaymentStatus.VERIFIED,
                receiptNumber =
                    officialReceipt
            )
        )
    }

    // =====================================================
    // CANCEL PAYMENT
    // =====================================================

    fun cancelPayment(
        paymentId: String
    ): Boolean {

        val payment =
            getById(
                paymentId
            )
                ?: return false

        return updatePayment(
            payment.copy(
                status =
                    PaymentStatus.CANCELLED
            )
        )
    }

    // =====================================================
    // DELETE PAYMENT
    // =====================================================

    fun deletePayment(
        paymentId: String
    ): Boolean {

        val oldSize =
            paymentState.value.size

        paymentState.value =
            paymentState.value
                .filterNot {
                    it.id == paymentId
                }

        if (
            paymentState.value.size ==
            oldSize
        ) {
            return false
        }

        persist()

        return true
    }

    /**
     * Delete all payment records for a student (optionally scoped to a coaching).
     * Used when student or fee plan is deleted so pending payments do not linger.
     */
    fun deleteByStudent(
        studentId: String,
        coachingId: String? = null
    ): Int {

        val before = paymentState.value.size

        paymentState.value =
            paymentState.value.filterNot { payment ->
                payment.studentId == studentId &&
                        (coachingId == null || payment.coachingId == coachingId)
            }

        val removed = before - paymentState.value.size

        if (removed > 0) {
            persist()
        }

        return removed
    }

    // =====================================================
    // STUDENT TOTAL PAID
    // =====================================================

    fun getTotalPaid(
        studentId: String
    ): Double {

        return paymentState.value
            .filter {
                it.studentId == studentId &&
                        it.status ==
                        PaymentStatus.VERIFIED
            }
            .sumOf {
                it.amount
            }
    }

    // =====================================================
    // STUDENT TOTAL PAYMENTS
    // =====================================================

    fun getPaymentCount(
        studentId: String
    ): Int {

        return paymentState.value
            .count {
                it.studentId == studentId &&
                        it.status ==
                        PaymentStatus.VERIFIED
            }
    }

    // =====================================================
    // COACHING COLLECTION
    // =====================================================

    fun getTotalCollection(
        coachingId: String
    ): Double {

        return paymentState.value
            .filter {
                it.coachingId == coachingId &&
                        it.status ==
                        PaymentStatus.VERIFIED
            }
            .sumOf {
                it.amount
            }
    }

    // =====================================================
    // COLLECTION BY DATE
    // =====================================================

    fun getCollectionByDateRange(
        coachingId: String,
        fromDate: String,
        toDate: String
    ): Double {

        return findByDateRange(
            coachingId = coachingId,
            fromDate = fromDate,
            toDate = toDate
        )
            .filter {
                it.status ==
                        PaymentStatus.VERIFIED
            }
            .sumOf {
                it.amount
            }
    }

    // =====================================================
    // RECEIPT NUMBER
    // =====================================================

    fun nextReceiptNumber(
        coachingId: String
    ): String {

        val year =
            SimpleDateFormat(
                "yyyy",
                Locale.getDefault()
            )
                .format(
                    Date()
                )

        // Prefer coaching short name for branding; fallback to SLH.
        val branding =
            CoachingProfileStore.get(coachingId)
                ?.shortName
                ?.trim()
                ?.replace(Regex("[^A-Za-z0-9]"), "")
                ?.uppercase(Locale.getDefault())
                ?.takeIf { it.isNotBlank() }
                ?: "SLH"

        val prefix =
            "$branding-$year-"

        val highest =
            paymentState.value
                .mapNotNull { payment ->

                    if (
                        payment.receiptNumber
                            .startsWith(
                                prefix,
                                ignoreCase = true
                            )
                    ) {

                        payment.receiptNumber
                            .removePrefix(
                                prefix
                            )
                            .toIntOrNull()

                    } else {

                        null
                    }
                }
                .maxOrNull()
                ?: 0

        return prefix +
                String.format(
                    Locale.getDefault(),
                    "%05d",
                    highest + 1
                )
    }

    // =====================================================
    // PAYMENT ID
    // =====================================================

    fun nextId(): String {

        /*
         * Time based: sequential ids collide when the admin's phone
         * and a student's phone create payments independently.
         */
        var stamp =
            System.currentTimeMillis()

        while (
            paymentState.value.any {
                it.id == "PAY$stamp"
            }
        ) {
            stamp++
        }

        return "PAY$stamp"
    }

    // =====================================================
    // PERSIST
    // =====================================================

    private fun persist() {

        SLHLocalStorage.savePayments(
            paymentState.value
        )

        paymentSync.onPersist(
            paymentState.value
        )
    }


    // =====================================================
    // FIREBASE -> LOCAL SYNC
    // =====================================================

    private fun applyCloudSlice(
        coachingId: String,
        studentId: String?,
        payments: List<PaymentRecord>
    ) {

        paymentState.value =
            paymentState.value.filterNot {
                it.coachingId == coachingId &&
                        (studentId == null ||
                                it.studentId == studentId)
            } + payments

        // The pulled data is now the baseline: nothing to push.
        paymentSync.reset(paymentState.value)

        persist()
    }


    /**
     * Admin phones: every payment of the coaching. canWrite: the
     * first time, local payments missing in the cloud are
     * uploaded once. Offline / error: nothing changes locally.
     */
    fun syncFromFirebase(
        coachingId: String,
        canWrite: Boolean = false,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {

        if (coachingId.isBlank()) {

            onComplete?.invoke(false, "coachingId is blank")

            return
        }

        cloudScope.launch {

            try {

                var cloudPayments =
                    PaymentFirestoreRepository
                        .findByCoaching(coachingId)

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "payments",
                        coachingId
                    )
                ) {

                    val cloudIds =
                        cloudPayments
                            .map { it.id }
                            .toSet()

                    val missing =
                        paymentState.value.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudIds
                        }

                    missing.forEach {
                        PaymentFirestoreRepository
                            .savePayment(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "payments",
                        coachingId
                    )

                    cloudPayments =
                        cloudPayments + missing
                }

                applyCloudSlice(
                    coachingId,
                    null,
                    cloudPayments
                )

                onComplete?.invoke(true, null)

            } catch (exception: Exception) {

                onComplete?.invoke(
                    false,
                    exception.message
                )
            }
        }
    }


    /**
     * Student phones: only the student's own payments.
     */
    fun syncOwnFromFirebase(
        coachingId: String,
        studentId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {

        cloudScope.launch {

            try {

                applyCloudSlice(
                    coachingId,
                    studentId,
                    PaymentFirestoreRepository
                        .findByStudent(
                            coachingId,
                            studentId
                        )
                )

                onComplete?.invoke(true, null)

            } catch (exception: Exception) {

                onComplete?.invoke(
                    false,
                    exception.message
                )
            }
        }
    }

    // =====================================================
    // CLEAR
    // =====================================================

    fun clear() {

        paymentState.value =
            emptyList()

        // Local reset only - never delete the cloud copy.
        paymentSync.reset(emptyList())

        persist()
    }
}