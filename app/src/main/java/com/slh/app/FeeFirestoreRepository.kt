package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * FEE + PAYMENT FIRESTORE REPOSITORIES
 * ============================================================
 *
 * Collections (document id = "<coachingId>__<local id>" so two
 * coachings can never overwrite each other):
 *
 * feePlans/{id}         full plan incl. concession (admin only)
 * feeSummaries/{id}     final fee + payment mode (admin + that
 *                       student). Concession details are NOT here.
 * feeInstallments/{id}  installments (admin + that student)
 * payments/{id}         payment records (admin + that student;
 *                       a student may only create PENDING ones)
 *
 * ============================================================
 */
object FeeFirestoreRepository {

    const val PLANS = "feePlans"

    const val SUMMARIES = "feeSummaries"

    const val INSTALLMENTS = "feeInstallments"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    fun docId(
        coachingId: String,
        id: String
    ): String {

        return "${coachingId}__$id"
    }


    fun planDocId(
        plan: StudentFeePlan
    ): String {

        return docId(
            plan.coachingId,
            plan.studentId
        )
    }


    // ---------------- plans ----------------

    suspend fun savePlan(
        plan: StudentFeePlan
    ) {

        val id =
            planDocId(plan)

        val full =
            hashMapOf<String, Any?>(
                "studentId" to plan.studentId,
                "coachingId" to plan.coachingId,
                "originalFee" to plan.originalFee,
                "concessionType" to plan.concessionType.name,
                "concessionValue" to plan.concessionValue,
                "concessionAmount" to plan.concessionAmount,
                "finalFee" to plan.finalFee,
                "paymentMode" to plan.paymentMode.name
            )

        val summary =
            hashMapOf<String, Any?>(
                "studentId" to plan.studentId,
                "coachingId" to plan.coachingId,
                "finalFee" to plan.finalFee,
                "paymentMode" to plan.paymentMode.name
            )

        val batch =
            db.batch()

        batch.set(
            db.collection(PLANS).document(id),
            full
        )

        batch.set(
            db.collection(SUMMARIES).document(id),
            summary
        )

        batch.commit().await()
    }


    suspend fun deletePlanDoc(
        docId: String
    ) {

        val batch =
            db.batch()

        batch.delete(
            db.collection(PLANS).document(docId)
        )

        batch.delete(
            db.collection(SUMMARIES).document(docId)
        )

        batch.commit().await()
    }


    suspend fun findPlansByCoaching(
        coachingId: String
    ): List<StudentFeePlan> {

        return db.collection(PLANS)
            .whereEqualTo("coachingId", coachingId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToPlan(it) }
    }


    /**
     * A student's own plan, from the summary document (no
     * concession details). originalFee = finalFee here.
     */
    suspend fun findOwnPlan(
        coachingId: String,
        studentId: String
    ): StudentFeePlan? {

        val document =
            db.collection(SUMMARIES)
                .document(docId(coachingId, studentId))
                .get(Source.SERVER)
                .await()

        if (!document.exists()) {
            return null
        }

        val finalFee =
            document.getDouble("finalFee")
                ?: return null

        return StudentFeePlan(
            studentId = studentId,
            coachingId = coachingId,
            originalFee = finalFee,
            concessionType = FeeConcessionType.NONE,
            concessionValue = 0.0,
            concessionAmount = 0.0,
            finalFee = finalFee,
            paymentMode =
                enumOr(
                    document.getString("paymentMode"),
                    FeePaymentMode.INSTALLMENTS
                )
        )
    }


    private fun documentToPlan(
        document: DocumentSnapshot
    ): StudentFeePlan? {

        val studentId =
            document.getString("studentId")
                ?: return null

        val coachingId =
            document.getString("coachingId")
                ?: return null

        return StudentFeePlan(
            studentId = studentId,
            coachingId = coachingId,
            originalFee =
                document.getDouble("originalFee") ?: 0.0,
            concessionType =
                enumOr(
                    document.getString("concessionType"),
                    FeeConcessionType.NONE
                ),
            concessionValue =
                document.getDouble("concessionValue") ?: 0.0,
            concessionAmount =
                document.getDouble("concessionAmount") ?: 0.0,
            finalFee =
                document.getDouble("finalFee") ?: 0.0,
            paymentMode =
                enumOr(
                    document.getString("paymentMode"),
                    FeePaymentMode.INSTALLMENTS
                )
        )
    }


    // ---------------- installments ----------------

    suspend fun saveInstallment(
        installment: FeeInstallment
    ) {

        db.collection(INSTALLMENTS)
            .document(
                docId(
                    installment.coachingId,
                    installment.id
                )
            )
            .set(
                hashMapOf<String, Any?>(
                    "id" to installment.id,
                    "studentId" to installment.studentId,
                    "coachingId" to installment.coachingId,
                    "installmentNumber" to
                            installment.installmentNumber,
                    "amount" to installment.amount,
                    "paidAmount" to installment.paidAmount,
                    "dueDate" to installment.dueDate,
                    "paidDate" to installment.paidDate,
                    "status" to installment.status.name,
                    "paymentNote" to installment.paymentNote
                )
            )
            .await()
    }


    suspend fun deleteDoc(
        collection: String,
        docId: String
    ) {

        db.collection(collection)
            .document(docId)
            .delete()
            .await()
    }


    suspend fun findInstallmentsByCoaching(
        coachingId: String
    ): List<FeeInstallment> {

        return db.collection(INSTALLMENTS)
            .whereEqualTo("coachingId", coachingId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToInstallment(it) }
    }


    suspend fun findInstallmentsByStudent(
        coachingId: String,
        studentId: String
    ): List<FeeInstallment> {

        return db.collection(INSTALLMENTS)
            .whereEqualTo("coachingId", coachingId)
            .whereEqualTo("studentId", studentId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToInstallment(it) }
    }


    private fun documentToInstallment(
        document: DocumentSnapshot
    ): FeeInstallment? {

        val studentId =
            document.getString("studentId")
                ?: return null

        val coachingId =
            document.getString("coachingId")
                ?: return null

        return FeeInstallment(
            id = document.getString("id") ?: return null,
            studentId = studentId,
            coachingId = coachingId,
            installmentNumber =
                (document.getLong("installmentNumber") ?: 0L)
                    .toInt(),
            amount = document.getDouble("amount") ?: 0.0,
            paidAmount =
                document.getDouble("paidAmount") ?: 0.0,
            dueDate = document.getString("dueDate") ?: "",
            paidDate = document.getString("paidDate") ?: "",
            status =
                enumOr(
                    document.getString("status"),
                    FeeInstallmentStatus.PENDING
                ),
            paymentNote =
                document.getString("paymentNote") ?: ""
        )
    }


    private inline fun <reified E : Enum<E>> enumOr(
        name: String?,
        default: E
    ): E {

        return try {

            enumValueOf<E>(name ?: "")

        } catch (_: Exception) {

            default
        }
    }
}


object PaymentFirestoreRepository {

    const val PAYMENTS = "payments"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    fun docId(
        payment: PaymentRecord
    ): String {

        return FeeFirestoreRepository.docId(
            payment.coachingId,
            payment.id
        )
    }


    suspend fun savePayment(
        payment: PaymentRecord
    ) {

        db.collection(PAYMENTS)
            .document(docId(payment))
            .set(
                hashMapOf<String, Any?>(
                    "id" to payment.id,
                    "receiptNumber" to payment.receiptNumber,
                    "coachingId" to payment.coachingId,
                    "studentId" to payment.studentId,
                    "batchId" to payment.batchId,
                    "installmentId" to payment.installmentId,
                    "studentName" to payment.studentName,
                    "studentCode" to payment.studentCode,
                    "course" to payment.course,
                    "batchName" to payment.batchName,
                    "amount" to payment.amount,
                    "paymentDate" to payment.paymentDate,
                    "paymentMode" to payment.paymentMode.name,
                    "transactionReference" to
                            payment.transactionReference,
                    "previousPaidAmount" to
                            payment.previousPaidAmount,
                    "totalFee" to payment.totalFee,
                    "remainingAmount" to payment.remainingAmount,
                    "remark" to payment.remark,
                    "status" to payment.status.name,
                    "createdAt" to payment.createdAt
                )
            )
            .await()
    }


    suspend fun deletePaymentDoc(
        docId: String
    ) {

        db.collection(PAYMENTS)
            .document(docId)
            .delete()
            .await()
    }


    suspend fun findByCoaching(
        coachingId: String
    ): List<PaymentRecord> {

        return db.collection(PAYMENTS)
            .whereEqualTo("coachingId", coachingId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToPayment(it) }
    }


    suspend fun findByStudent(
        coachingId: String,
        studentId: String
    ): List<PaymentRecord> {

        return db.collection(PAYMENTS)
            .whereEqualTo("coachingId", coachingId)
            .whereEqualTo("studentId", studentId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToPayment(it) }
    }


    private fun documentToPayment(
        document: DocumentSnapshot
    ): PaymentRecord? {

        return try {

            PaymentRecord(
                id = document.getString("id") ?: return null,
                receiptNumber =
                    document.getString("receiptNumber") ?: "",
                coachingId =
                    document.getString("coachingId")
                        ?: return null,
                studentId =
                    document.getString("studentId")
                        ?: return null,
                batchId = document.getString("batchId") ?: "",
                installmentId =
                    document.getString("installmentId") ?: "",
                studentName =
                    document.getString("studentName") ?: "",
                studentCode =
                    document.getString("studentCode") ?: "",
                course = document.getString("course") ?: "",
                batchName =
                    document.getString("batchName") ?: "",
                amount = document.getDouble("amount") ?: 0.0,
                paymentDate =
                    document.getString("paymentDate") ?: "",
                paymentMode =
                    try {
                        PaymentMode.valueOf(
                            document.getString("paymentMode")
                                ?: ""
                        )
                    } catch (_: Exception) {
                        PaymentMode.OTHER
                    },
                transactionReference =
                    document.getString("transactionReference")
                        ?: "",
                previousPaidAmount =
                    document.getDouble("previousPaidAmount")
                        ?: 0.0,
                totalFee = document.getDouble("totalFee") ?: 0.0,
                remainingAmount =
                    document.getDouble("remainingAmount") ?: 0.0,
                remark = document.getString("remark") ?: "",
                status =
                    try {
                        PaymentStatus.valueOf(
                            document.getString("status") ?: ""
                        )
                    } catch (_: Exception) {
                        PaymentStatus.PENDING
                    },
                createdAt =
                    document.getLong("createdAt")
                        ?: System.currentTimeMillis()
            )

        } catch (_: Exception) {

            null
        }
    }
}
