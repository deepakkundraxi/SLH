package com.slh.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class FeeInstallmentStatus {
    PENDING,
    PAID,
    OVERDUE
}

enum class FeeConcessionType {
    NONE,
    FIXED,
    PERCENTAGE
}

enum class FeePaymentMode {
    INSTALLMENTS,
    FULL_PAYMENT
}

data class FeeInstallment(
    val id: String,
    val studentId: String,
    val coachingId: String,

    /**
     * 0 = Full Payment
     * 1 = Installment 1
     * 2 = Installment 2
     * 3 = Installment 3
     */
    val installmentNumber: Int,

    val amount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: String,
    val paidDate: String = "",
    val status: FeeInstallmentStatus = FeeInstallmentStatus.PENDING,
    val paymentNote: String = ""
)

data class StudentFeePlan(
    val studentId: String,
    val coachingId: String,

    // Admin-only information
    val originalFee: Double,

    val concessionType: FeeConcessionType = FeeConcessionType.NONE,
    val concessionValue: Double = 0.0,
    val concessionAmount: Double = 0.0,

    // Final payable amount
    val finalFee: Double = 0.0,

    val paymentMode: FeePaymentMode = FeePaymentMode.INSTALLMENTS
)

data class StudentFeeSummary(
    val studentId: String,
    val coachingId: String,

    // Admin side information
    val originalFee: Double,
    val concessionType: FeeConcessionType,
    val concessionValue: Double,
    val concessionAmount: Double,

    // Student-visible final amounts
    val totalFee: Double,
    val totalPaid: Double,
    val remaining: Double,

    val installments: List<FeeInstallment>,
    val paymentMode: FeePaymentMode
)

object FeeStore {

    private val installmentsState =
        mutableStateListOf<FeeInstallment>()

    private val plansState =
        mutableStateListOf<StudentFeePlan>()

    private var initialized = false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /*
     * Every persist() pushes the difference to Firestore
     * (see CloudDiffSync). The installment STATUS is derived
     * from dates, so it is ignored when comparing.
     */
    private val planSync =
        CloudDiffSync<StudentFeePlan>(
            idOf = { FeeFirestoreRepository.planDocId(it) },
            upsert = { FeeFirestoreRepository.savePlan(it) },
            delete = { FeeFirestoreRepository.deletePlanDoc(it) }
        )

    private val installmentSync =
        CloudDiffSync<FeeInstallment>(
            idOf = {
                FeeFirestoreRepository.docId(
                    it.coachingId,
                    it.id
                )
            },
            comparable = {
                it.copy(status = FeeInstallmentStatus.PENDING)
            },
            upsert = {
                FeeFirestoreRepository.saveInstallment(it)
            },
            delete = {
                FeeFirestoreRepository.deleteDoc(
                    FeeFirestoreRepository.INSTALLMENTS,
                    it
                )
            }
        )

    val installments: List<FeeInstallment>
        get() = installmentsState

    val plans: List<StudentFeePlan>
        get() = plansState

    // ---------------------------------------------------------
    // INITIALIZE + PERSIST
    // ---------------------------------------------------------

    fun initialize() {
        if (initialized) {
            return
        }
        initialized = true

        val savedPlans = SLHLocalStorage.loadFeePlans()
        val savedInstallments = SLHLocalStorage.loadFeeInstallments()

        if (savedPlans != null) {
            plansState.clear()
            plansState.addAll(savedPlans)
        }

        if (savedInstallments != null) {
            installmentsState.clear()
            installmentsState.addAll(savedInstallments)
        }

        // Baseline for cloud sync: what is on disk is not "new".
        planSync.reset(plansState.toList())
        installmentSync.reset(installmentsState.toList())

        // Refresh overdue/pending statuses after load.
        refreshStatusesInternal()
    }

    private fun persist() {
        SLHLocalStorage.saveFeePlans(plansState.toList())
        SLHLocalStorage.saveFeeInstallments(installmentsState.toList())

        planSync.onPersist(plansState.toList())
        installmentSync.onPersist(installmentsState.toList())
    }


    // ---------------------------------------------------------
    // FIREBASE -> LOCAL SYNC
    // ---------------------------------------------------------

    /**
     * Replaces the local fee data of one coaching (or of one
     * student when studentId is given) with the cloud copy.
     */
    private fun applyCloudSlice(
        coachingId: String,
        studentId: String?,
        plans: List<StudentFeePlan>,
        installments: List<FeeInstallment>
    ) {

        Snapshot.withMutableSnapshot {

            plansState.removeAll {
                it.coachingId == coachingId &&
                        (studentId == null ||
                                it.studentId == studentId)
            }

            plansState.addAll(plans)

            installmentsState.removeAll {
                it.coachingId == coachingId &&
                        (studentId == null ||
                                it.studentId == studentId)
            }

            installmentsState.addAll(installments)
        }

        // The pulled data is now the baseline: nothing to push.
        planSync.reset(plansState.toList())
        installmentSync.reset(installmentsState.toList())

        persist()

        Snapshot.withMutableSnapshot {
            refreshStatusesInternal()
        }
    }


    /**
     * Admin / principal phones: the whole coaching.
     * canWrite: the first time, local plans and installments that
     * are missing in the cloud are uploaded once.
     * Offline / error: nothing changes locally.
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

                var cloudPlans =
                    FeeFirestoreRepository
                        .findPlansByCoaching(coachingId)

                var cloudInstallments =
                    FeeFirestoreRepository
                        .findInstallmentsByCoaching(coachingId)

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "fees",
                        coachingId
                    )
                ) {

                    val planIds =
                        cloudPlans
                            .map {
                                FeeFirestoreRepository
                                    .planDocId(it)
                            }
                            .toSet()

                    val installmentIds =
                        cloudInstallments
                            .map { it.id }
                            .toSet()

                    val missingPlans =
                        plansState.filter {
                            it.coachingId == coachingId &&
                                    FeeFirestoreRepository
                                        .planDocId(it) !in planIds
                        }

                    val missingInstallments =
                        installmentsState.filter {
                            it.coachingId == coachingId &&
                                    it.id !in installmentIds
                        }

                    missingPlans.forEach {
                        FeeFirestoreRepository.savePlan(it)
                    }

                    missingInstallments.forEach {
                        FeeFirestoreRepository
                            .saveInstallment(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "fees",
                        coachingId
                    )

                    cloudPlans =
                        cloudPlans + missingPlans

                    cloudInstallments =
                        cloudInstallments + missingInstallments
                }

                applyCloudSlice(
                    coachingId,
                    null,
                    cloudPlans,
                    cloudInstallments
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
     * Student phones: only the student's own fee data. The plan
     * comes from the summary document, so concession details are
     * never downloaded.
     */
    fun syncOwnFromFirebase(
        coachingId: String,
        studentId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {

        cloudScope.launch {

            try {

                val ownPlan =
                    FeeFirestoreRepository
                        .findOwnPlan(coachingId, studentId)

                val installments =
                    FeeFirestoreRepository
                        .findInstallmentsByStudent(
                            coachingId,
                            studentId
                        )

                applyCloudSlice(
                    coachingId,
                    studentId,
                    listOfNotNull(ownPlan),
                    installments
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

    // ---------------------------------------------------------
    // BASIC HELPERS
    // ---------------------------------------------------------

    fun todayDate(): String {
        return SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    private fun parseDate(date: String): Date? {

        if (date.isBlank()) {
            return null
        }

        return try {

            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }.parse(date)

        } catch (_: Exception) {
            null
        }
    }

    private fun roundMoney(value: Double): Double {
        return round(value * 100.0) / 100.0
    }

    fun calculateConcessionAmount(
        originalFee: Double,
        concessionType: FeeConcessionType,
        concessionValue: Double
    ): Double {

        val safeFee =
            max(0.0, originalFee)

        val safeValue =
            max(0.0, concessionValue)

        return when (concessionType) {

            FeeConcessionType.NONE -> {
                0.0
            }

            FeeConcessionType.FIXED -> {
                min(
                    safeFee,
                    safeValue
                )
            }

            FeeConcessionType.PERCENTAGE -> {
                val percentage =
                    min(
                        100.0,
                        safeValue
                    )

                safeFee * percentage / 100.0
            }
        }.let(::roundMoney)
    }

    fun calculateFinalFee(
        originalFee: Double,
        concessionType: FeeConcessionType,
        concessionValue: Double
    ): Double {

        val concession =
            calculateConcessionAmount(
                originalFee = originalFee,
                concessionType = concessionType,
                concessionValue = concessionValue
            )

        return roundMoney(
            max(
                0.0,
                originalFee - concession
            )
        )
    }

    fun calculateInstallmentAmounts(
        finalFee: Double
    ): List<Double> {

        val fee =
            max(
                0.0,
                finalFee
            )

        val first =
            roundMoney(
                fee * 0.40
            )

        val second =
            roundMoney(
                fee * 0.30
            )

        val third =
            roundMoney(
                fee - first - second
            )

        return listOf(
            first,
            second,
            third
        )
    }

    // ---------------------------------------------------------
    // FIND / GET
    // ---------------------------------------------------------

    fun findPlan(
        studentId: String,
        coachingId: String
    ): StudentFeePlan? {

        return plansState.firstOrNull {
            it.studentId == studentId &&
                    it.coachingId == coachingId
        }
    }

    fun findPlan(
        studentId: String
    ): StudentFeePlan? {

        return plansState.firstOrNull {
            it.studentId == studentId
        }
    }

    fun getStudentInstallments(
        studentId: String,
        coachingId: String
    ): List<FeeInstallment> {

        return installmentsState
            .filter {
                it.studentId == studentId &&
                        it.coachingId == coachingId
            }
            .sortedBy {
                it.installmentNumber
            }
    }

    fun getStudentInstallments(
        studentId: String
    ): List<FeeInstallment> {

        return installmentsState
            .filter {
                it.studentId == studentId
            }
            .sortedBy {
                it.installmentNumber
            }
    }

    fun getInstallmentById(
        installmentId: String
    ): FeeInstallment? {

        return installmentsState
            .firstOrNull {
                it.id == installmentId
            }
    }

    fun getSummary(
        studentId: String,
        coachingId: String
    ): StudentFeeSummary? {

        val plan =
            findPlan(
                studentId = studentId,
                coachingId = coachingId
            )
                ?: return null

        val studentInstallments =
            getStudentInstallments(
                studentId = studentId,
                coachingId = coachingId
            )

        val totalPaid =
            roundMoney(
                studentInstallments.sumOf {
                    it.paidAmount
                }
            )

        val remaining =
            roundMoney(
                max(
                    0.0,
                    plan.finalFee - totalPaid
                )
            )

        return StudentFeeSummary(
            studentId = plan.studentId,
            coachingId = plan.coachingId,
            originalFee = plan.originalFee,
            concessionType = plan.concessionType,
            concessionValue = plan.concessionValue,
            concessionAmount = plan.concessionAmount,
            totalFee = plan.finalFee,
            totalPaid = totalPaid,
            remaining = remaining,
            installments = studentInstallments,
            paymentMode = plan.paymentMode
        )
    }

    fun getSummary(
        studentId: String
    ): StudentFeeSummary? {

        val plan =
            findPlan(
                studentId = studentId
            )
                ?: return null

        return getSummary(
            studentId = plan.studentId,
            coachingId = plan.coachingId
        )
    }

    // ---------------------------------------------------------
    // STATUS
    // ---------------------------------------------------------

    private fun calculateStatus(
        installment: FeeInstallment,
        today: String = todayDate()
    ): FeeInstallmentStatus {

        if (
            installment.paidAmount >=
            installment.amount
        ) {
            return FeeInstallmentStatus.PAID
        }

        val due =
            parseDate(
                installment.dueDate
            )

        val current =
            parseDate(
                today
            )

        if (
            due != null &&
            current != null &&
            due.before(current)
        ) {
            return FeeInstallmentStatus.OVERDUE
        }

        return FeeInstallmentStatus.PENDING
    }

    private fun refreshStatusesInternal() {

        var changed = false

        for (index in installmentsState.indices) {

            val current =
                installmentsState[index]

            val newStatus =
                calculateStatus(
                    installment = current
                )

            if (
                newStatus !=
                current.status
            ) {

                installmentsState[index] =
                    current.copy(
                        status = newStatus
                    )
                changed = true
            }
        }

        if (changed) {
            persist()
        }
    }

    fun refreshStatuses() {
        refreshStatusesInternal()
    }

    fun getOverdueInstallments(
        coachingId: String
    ): List<FeeInstallment> {

        refreshStatusesInternal()

        return installmentsState
            .filter {
                it.coachingId == coachingId &&
                        it.status ==
                        FeeInstallmentStatus.OVERDUE
            }
            .sortedBy {
                parseDate(it.dueDate)
            }
    }

    fun getOverdueStudentIds(
        coachingId: String
    ): List<String> {

        refreshStatusesInternal()

        return installmentsState
            .filter {
                it.coachingId == coachingId &&
                        it.status ==
                        FeeInstallmentStatus.OVERDUE
            }
            .map {
                it.studentId
            }
            .distinct()
    }

    /**
     * Number of unique students having at least
     * one overdue fee.
     *
     * Important:
     * If one student has 2 overdue installments,
     * that student is counted only once.
     */
    fun getOverdueStudentCount(
        coachingId: String
    ): Int {

        refreshStatusesInternal()

        return installmentsState
            .filter {
                it.coachingId == coachingId &&
                        it.status ==
                        FeeInstallmentStatus.OVERDUE
            }
            .map {
                it.studentId
            }
            .distinct()
            .size
    }

    // ---------------------------------------------------------
    // CREATE AUTOMATIC FEE PLAN
    // ---------------------------------------------------------

    fun createAutomaticFeePlan(
        studentId: String,
        coachingId: String,
        originalFee: Double,
        concessionType: FeeConcessionType,
        concessionValue: Double,
        paymentMode: FeePaymentMode,
        installment1DueDate: String = "",
        installment2DueDate: String = "",
        installment3DueDate: String = "",
        fullPaymentDueDate: String = ""
    ): StudentFeePlan {

        // Remove existing plan first (keep payment history)
        deleteFeePlan(
            studentId = studentId,
            coachingId = coachingId,
            cascadePayments = false
        )

        val safeOriginalFee =
            roundMoney(
                max(
                    0.0,
                    originalFee
                )
            )

        val concessionAmount =
            calculateConcessionAmount(
                originalFee = safeOriginalFee,
                concessionType = concessionType,
                concessionValue = concessionValue
            )

        val finalFee =
            roundMoney(
                max(
                    0.0,
                    safeOriginalFee -
                            concessionAmount
                )
            )

        val plan =
            StudentFeePlan(
                studentId = studentId,
                coachingId = coachingId,
                originalFee = safeOriginalFee,
                concessionType = concessionType,
                concessionValue =
                    max(
                        0.0,
                        concessionValue
                    ),
                concessionAmount = concessionAmount,
                finalFee = finalFee,
                paymentMode = paymentMode
            )

        plansState.add(plan)

        if (
            paymentMode ==
            FeePaymentMode.FULL_PAYMENT
        ) {

            // FULL PAYMENT:
            // Exactly ONE payment record.
            installmentsState.add(
                FeeInstallment(
                    id =
                        "${studentId}_FEE_FULL",
                    studentId =
                        studentId,
                    coachingId =
                        coachingId,
                    installmentNumber = 0,
                    amount =
                        finalFee,
                    paidAmount = 0.0,
                    dueDate =
                        fullPaymentDueDate,
                    paidDate = "",
                    status =
                        FeeInstallmentStatus.PENDING,
                    paymentNote = ""
                )
            )

        } else {

            // INSTALLMENTS:
            // 40% + 30% + 30%
            val amounts =
                calculateInstallmentAmounts(
                    finalFee
                )

            installmentsState.add(
                FeeInstallment(
                    id =
                        "${studentId}_FEE_1",
                    studentId =
                        studentId,
                    coachingId =
                        coachingId,
                    installmentNumber = 1,
                    amount =
                        amounts[0],
                    paidAmount = 0.0,
                    dueDate =
                        installment1DueDate,
                    status =
                        FeeInstallmentStatus.PENDING
                )
            )

            installmentsState.add(
                FeeInstallment(
                    id =
                        "${studentId}_FEE_2",
                    studentId =
                        studentId,
                    coachingId =
                        coachingId,
                    installmentNumber = 2,
                    amount =
                        amounts[1],
                    paidAmount = 0.0,
                    dueDate =
                        installment2DueDate,
                    status =
                        FeeInstallmentStatus.PENDING
                )
            )

            installmentsState.add(
                FeeInstallment(
                    id =
                        "${studentId}_FEE_3",
                    studentId =
                        studentId,
                    coachingId =
                        coachingId,
                    installmentNumber = 3,
                    amount =
                        amounts[2],
                    paidAmount = 0.0,
                    dueDate =
                        installment3DueDate,
                    status =
                        FeeInstallmentStatus.PENDING
                )
            )
        }

        refreshStatusesInternal()
        persist()

        return plan
    }

    // ---------------------------------------------------------
    // UPDATE AUTOMATIC FEE PLAN
    // ---------------------------------------------------------

    fun updateAutomaticFeePlan(
        studentId: String,
        coachingId: String,
        originalFee: Double,
        concessionType: FeeConcessionType,
        concessionValue: Double,
        paymentMode: FeePaymentMode,
        installment1DueDate: String = "",
        installment2DueDate: String = "",
        installment3DueDate: String = "",
        fullPaymentDueDate: String = ""
    ): StudentFeePlan {

        val oldInstallments =
            installmentsState
                .filter {
                    it.studentId == studentId &&
                            it.coachingId == coachingId
                }
                .sortedBy {
                    it.installmentNumber
                }

        // Preserve already-paid amount
        val totalPreviouslyPaid =
            roundMoney(
                oldInstallments.sumOf {
                    it.paidAmount
                }
            )

        val safeOriginalFee =
            roundMoney(
                max(
                    0.0,
                    originalFee
                )
            )

        val concessionAmount =
            calculateConcessionAmount(
                originalFee = safeOriginalFee,
                concessionType = concessionType,
                concessionValue = concessionValue
            )

        val finalFee =
            roundMoney(
                max(
                    0.0,
                    safeOriginalFee -
                            concessionAmount
                )
            )

        val plan =
            StudentFeePlan(
                studentId = studentId,
                coachingId = coachingId,
                originalFee = safeOriginalFee,
                concessionType = concessionType,
                concessionValue =
                    max(
                        0.0,
                        concessionValue
                    ),
                concessionAmount =
                    concessionAmount,
                finalFee = finalFee,
                paymentMode = paymentMode
            )

        val existingPlanIndex =
            plansState.indexOfFirst {
                it.studentId == studentId &&
                        it.coachingId == coachingId
            }

        if (existingPlanIndex >= 0) {

            plansState[existingPlanIndex] =
                plan

        } else {

            plansState.add(plan)
        }

        // Remove old installment records
        installmentsState.removeAll {
            it.studentId == studentId &&
                    it.coachingId == coachingId
        }

        // Paid amount cannot exceed new final fee.
        var remainingPreviouslyPaid =
            min(
                totalPreviouslyPaid,
                finalFee
            )

        if (
            paymentMode ==
            FeePaymentMode.FULL_PAYMENT
        ) {

            val paidForFullPayment =
                min(
                    remainingPreviouslyPaid,
                    finalFee
                )

            val status =
                if (
                    paidForFullPayment >=
                    finalFee
                ) {
                    FeeInstallmentStatus.PAID
                } else {
                    FeeInstallmentStatus.PENDING
                }

            installmentsState.add(
                FeeInstallment(
                    id =
                        "${studentId}_FEE_FULL",
                    studentId =
                        studentId,
                    coachingId =
                        coachingId,
                    installmentNumber = 0,
                    amount =
                        finalFee,
                    paidAmount =
                        roundMoney(
                            paidForFullPayment
                        ),
                    dueDate =
                        fullPaymentDueDate,
                    paidDate =
                        if (
                            paidForFullPayment >=
                            finalFee
                        ) {
                            todayDate()
                        } else {
                            ""
                        },
                    status =
                        status
                )
            )

        } else {

            val amounts =
                calculateInstallmentAmounts(
                    finalFee
                )

            val dueDates =
                listOf(
                    installment1DueDate,
                    installment2DueDate,
                    installment3DueDate
                )

            for (index in 0..2) {

                val installmentAmount =
                    amounts[index]

                val paidForThis =
                    min(
                        remainingPreviouslyPaid,
                        installmentAmount
                    )

                remainingPreviouslyPaid =
                    roundMoney(
                        max(
                            0.0,
                            remainingPreviouslyPaid -
                                    paidForThis
                        )
                    )

                val number =
                    index + 1

                val status =
                    if (
                        paidForThis >=
                        installmentAmount
                    ) {
                        FeeInstallmentStatus.PAID
                    } else {
                        FeeInstallmentStatus.PENDING
                    }

                installmentsState.add(
                    FeeInstallment(
                        id =
                            "${studentId}_FEE_$number",
                        studentId =
                            studentId,
                        coachingId =
                            coachingId,
                        installmentNumber =
                            number,
                        amount =
                            installmentAmount,
                        paidAmount =
                            roundMoney(
                                paidForThis
                            ),
                        dueDate =
                            dueDates[index],
                        paidDate =
                            if (
                                paidForThis >=
                                installmentAmount
                            ) {
                                todayDate()
                            } else {
                                ""
                            },
                        status =
                            status
                    )
                )
            }
        }

        refreshStatusesInternal()
        persist()

        return plan
    }

    // ---------------------------------------------------------
    // LEGACY CREATE FEE PLAN
    // ---------------------------------------------------------

    fun createFeePlan(
        studentId: String,
        coachingId: String,
        originalFee: Double,
        concessionType: FeeConcessionType = FeeConcessionType.NONE,
        concessionValue: Double = 0.0,
        paymentMode: FeePaymentMode = FeePaymentMode.INSTALLMENTS,
        installment1DueDate: String = "",
        installment2DueDate: String = "",
        installment3DueDate: String = ""
    ): StudentFeePlan {

        return createAutomaticFeePlan(
            studentId = studentId,
            coachingId = coachingId,
            originalFee = originalFee,
            concessionType = concessionType,
            concessionValue = concessionValue,
            paymentMode = paymentMode,
            installment1DueDate =
                installment1DueDate,
            installment2DueDate =
                installment2DueDate,
            installment3DueDate =
                installment3DueDate,

            // Legacy method has no separate
            // full-payment date field.
            fullPaymentDueDate =
                installment1DueDate
        )
    }

    // ---------------------------------------------------------
    // UPDATE INSTALLMENT
    // ---------------------------------------------------------

    fun updateInstallment(
        installmentId: String,
        dueDate: String? = null,
        amount: Double? = null,
        paymentNote: String? = null
    ): Boolean {

        val index =
            installmentsState.indexOfFirst {
                it.id == installmentId
            }

        if (index < 0) {
            return false
        }

        val current =
            installmentsState[index]

        val newAmount =
            roundMoney(
                max(
                    0.0,
                    amount ?: current.amount
                )
            )

        val newPaidAmount =
            min(
                current.paidAmount,
                newAmount
            )

        val updated =
            current.copy(
                amount = newAmount,
                paidAmount =
                    roundMoney(
                        newPaidAmount
                    ),
                dueDate =
                    dueDate ?: current.dueDate,
                paymentNote =
                    paymentNote
                        ?: current.paymentNote,
                status =
                    calculateStatus(
                        current.copy(
                            amount =
                                newAmount,
                            paidAmount =
                                roundMoney(
                                    newPaidAmount
                                ),
                            dueDate =
                                dueDate
                                    ?: current.dueDate
                        )
                    )
            )

        installmentsState[index] =
            updated

        persist()
        return true
    }

    // ---------------------------------------------------------
    // MARK AS PAID
    // ---------------------------------------------------------

    fun markAsPaid(
        installmentId: String,
        paymentNote: String = ""
    ): Boolean {

        val index =
            installmentsState.indexOfFirst {
                it.id == installmentId
            }

        if (index < 0) {
            return false
        }

        val current =
            installmentsState[index]

        val updated =
            current.copy(
                paidAmount =
                    current.amount,
                paidDate =
                    todayDate(),
                status =
                    FeeInstallmentStatus.PAID,
                paymentNote =
                    paymentNote
            )

        installmentsState[index] =
            updated

        persist()
        return true
    }

    // ---------------------------------------------------------
    // PARTIAL / CUSTOM PAYMENT
    // ---------------------------------------------------------

    fun updatePayment(
        installmentId: String,
        paidAmount: Double,
        paidDate: String = todayDate(),
        paymentNote: String = ""
    ): Boolean {

        val index =
            installmentsState.indexOfFirst {
                it.id == installmentId
            }

        if (index < 0) {
            return false
        }

        val current =
            installmentsState[index]

        val safePaidAmount =
            roundMoney(
                min(
                    current.amount,
                    max(
                        0.0,
                        paidAmount
                    )
                )
            )

        val newStatus =
            if (
                safePaidAmount >=
                current.amount
            ) {
                FeeInstallmentStatus.PAID
            } else {

                val due =
                    parseDate(
                        current.dueDate
                    )

                val currentDate =
                    parseDate(
                        todayDate()
                    )

                if (
                    due != null &&
                    currentDate != null &&
                    due.before(currentDate)
                ) {
                    FeeInstallmentStatus.OVERDUE
                } else {
                    FeeInstallmentStatus.PENDING
                }
            }

        installmentsState[index] =
            current.copy(
                paidAmount =
                    safePaidAmount,
                paidDate =
                    if (
                        safePaidAmount > 0.0
                    ) {
                        paidDate
                    } else {
                        ""
                    },
                status =
                    newStatus,
                paymentNote =
                    paymentNote
            )

        persist()
        return true
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    fun deleteInstallment(
        installmentId: String
    ): Boolean {

        val before =
            installmentsState.size

        installmentsState.removeAll {
            it.id == installmentId
        }

        val removed = installmentsState.size < before
        if (removed) {
            persist()
        }
        return removed
    }

    /**
     * @param cascadePayments when true (default), also deletes PaymentStore
     * records for this student. Pass false when recreating a fee plan
     * so existing payment history is preserved.
     */
    fun deleteFeePlan(
        studentId: String,
        coachingId: String,
        cascadePayments: Boolean = true
    ) {

        plansState.removeAll {
            it.studentId == studentId &&
                    it.coachingId == coachingId
        }

        installmentsState.removeAll {
            it.studentId == studentId &&
                    it.coachingId == coachingId
        }

        if (cascadePayments) {
            // Remove payment records so pending payments
            // do not keep showing after fee plan is deleted.
            PaymentStore.deleteByStudent(
                studentId = studentId,
                coachingId = coachingId
            )
        }

        persist()
    }

    fun deleteFeePlan(
        studentId: String,
        cascadePayments: Boolean = true
    ) {

        plansState.removeAll {
            it.studentId == studentId
        }

        installmentsState.removeAll {
            it.studentId == studentId
        }

        if (cascadePayments) {
            PaymentStore.deleteByStudent(
                studentId = studentId
            )
        }

        persist()
    }

    // ---------------------------------------------------------
    // COUNTS
    // ---------------------------------------------------------

    fun getPendingInstallmentCount(
        coachingId: String
    ): Int {

        refreshStatusesInternal()

        return installmentsState.count {
            it.coachingId == coachingId &&
                    it.status ==
                    FeeInstallmentStatus.PENDING &&
                    StudentStore.findStudent(it.studentId) != null
        }
    }

    fun getPaidInstallmentCount(
        coachingId: String
    ): Int {

        refreshStatusesInternal()

        return installmentsState.count {
            it.coachingId == coachingId &&
                    it.status ==
                    FeeInstallmentStatus.PAID &&
                    StudentStore.findStudent(it.studentId) != null
        }
    }

    fun getOverdueInstallmentCount(
        coachingId: String
    ): Int {

        refreshStatusesInternal()

        return installmentsState.count {
            it.coachingId == coachingId &&
                    it.status ==
                    FeeInstallmentStatus.OVERDUE &&
                    StudentStore.findStudent(it.studentId) != null
        }
    }

    fun getStudentFeePlanCount(
        coachingId: String
    ): Int {

        return plansState.count {
            it.coachingId == coachingId
        }
    }

    // ---------------------------------------------------------
    // TOTALS
    // ---------------------------------------------------------

    fun getTotalFee(
        coachingId: String
    ): Double {

        return roundMoney(
            plansState
                .filter {
                    it.coachingId ==
                            coachingId
                }
                .sumOf {
                    it.finalFee
                }
        )
    }

    fun getTotalCollected(
        coachingId: String
    ): Double {

        return roundMoney(
            installmentsState
                .filter {
                    it.coachingId ==
                            coachingId
                }
                .sumOf {
                    it.paidAmount
                }
        )
    }

    fun getTotalRemaining(
        coachingId: String
    ): Double {

        val totalFee =
            getTotalFee(
                coachingId
            )

        val totalCollected =
            getTotalCollected(
                coachingId
            )

        return roundMoney(
            max(
                0.0,
                totalFee -
                        totalCollected
            )
        )
    }

    fun getTotalOverdueAmount(
        coachingId: String
    ): Double {

        refreshStatusesInternal()

        return roundMoney(
            installmentsState
                .filter {
                    it.coachingId == coachingId &&
                            it.status ==
                            FeeInstallmentStatus.OVERDUE
                }
                .sumOf {
                    max(
                        0.0,
                        it.amount -
                                it.paidAmount
                    )
                }
        )
    }

    // ---------------------------------------------------------
    // STUDENT-SPECIFIC TOTALS
    // ---------------------------------------------------------

    fun getStudentTotalPaid(
        studentId: String,
        coachingId: String
    ): Double {

        return roundMoney(
            installmentsState
                .filter {
                    it.studentId == studentId &&
                            it.coachingId == coachingId
                }
                .sumOf {
                    it.paidAmount
                }
        )
    }

    fun getStudentRemaining(
        studentId: String,
        coachingId: String
    ): Double {

        val plan =
            findPlan(
                studentId = studentId,
                coachingId = coachingId
            )
                ?: return 0.0

        val paid =
            getStudentTotalPaid(
                studentId = studentId,
                coachingId = coachingId
            )

        return roundMoney(
            max(
                0.0,
                plan.finalFee - paid
            )
        )
    }

    // ---------------------------------------------------------
    // OVERDUE / UPCOMING HELPERS
    // ---------------------------------------------------------

    fun getStudentOverdueInstallments(
        studentId: String,
        coachingId: String
    ): List<FeeInstallment> {

        refreshStatusesInternal()

        return installmentsState
            .filter {
                it.studentId == studentId &&
                        it.coachingId == coachingId &&
                        it.status ==
                        FeeInstallmentStatus.OVERDUE
            }
            .sortedBy {
                parseDate(it.dueDate)
            }
    }

    fun getStudentPendingInstallments(
        studentId: String,
        coachingId: String
    ): List<FeeInstallment> {

        refreshStatusesInternal()

        return installmentsState
            .filter {
                it.studentId == studentId &&
                        it.coachingId == coachingId &&
                        it.status ==
                        FeeInstallmentStatus.PENDING
            }
            .sortedBy {
                it.installmentNumber
            }
    }

    // ---------------------------------------------------------
    // CLEAR
    // ---------------------------------------------------------

    fun clear() {

        installmentsState.clear()
        plansState.clear()

        // Local reset only - never delete the cloud copy.
        planSync.reset(emptyList())
        installmentSync.reset(emptyList())

        persist()
    }
}