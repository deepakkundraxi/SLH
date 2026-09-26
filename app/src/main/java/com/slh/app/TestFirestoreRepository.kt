package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * TEST + RESULT FIRESTORE REPOSITORY
 * ============================================================
 *
 * Collections:
 *
 *   tests/{test.id}
 *   testResults/{result.id}
 *
 * Every document carries "coachingId" so one coaching only
 * ever reads its own tests and results.
 *
 * ============================================================
 */
object TestFirestoreRepository {

    private const val COLLECTION_TESTS = "tests"
    private const val COLLECTION_RESULTS = "testResults"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    // ---------------- tests ----------------

    private fun testToMap(
        test: CoachingTest
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to test.id,
            "coachingId" to test.coachingId,
            "batchId" to test.batchId,
            "title" to test.title,
            "subject" to test.subject,
            "testDate" to test.testDate,
            "totalMarks" to test.totalMarks,
            "passingMarks" to test.passingMarks,
            "description" to test.description,
            "createdById" to test.createdById,
            "createdByName" to test.createdByName,
            "status" to test.status.name,
            "testType" to test.testType.name,
            "durationMinutes" to test.durationMinutes,
            "startTime" to test.startTime,
            "endTime" to test.endTime,
            "rejectionReason" to test.rejectionReason,
            "submittedAt" to test.submittedAt,
            "approvedAt" to test.approvedAt,
            "approvedById" to test.approvedById,
            "approvedByName" to test.approvedByName
        )
    }


    suspend fun saveTest(
        test: CoachingTest
    ) {

        db.collection(COLLECTION_TESTS)
            .document(test.id)
            .set(testToMap(test))
            .await()
    }


    suspend fun deleteTest(
        testId: String
    ) {

        db.collection(COLLECTION_TESTS)
            .document(testId)
            .delete()
            .await()
    }


    /**
     * Source.SERVER so offline never returns an empty cache
     * that would wipe good local data on sync.
     */
    suspend fun findTestsByCoaching(
        coachingId: String
    ): List<CoachingTest> {

        val snapshot =
            db.collection(COLLECTION_TESTS)
                .whereEqualTo("coachingId", coachingId)
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToTest(it)
        }
    }


    private fun documentToTest(
        document: DocumentSnapshot
    ): CoachingTest? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        val status =
            parseTestStatus(document.getString("status"))

        val duration =
            (document.getLong("durationMinutes")
                ?: document.getDouble("durationMinutes")?.toLong()
                ?: 0L).toInt()

        return CoachingTest(
            id = document.getString("id") ?: document.id,
            coachingId = coachingId,
            batchId = document.getString("batchId") ?: "",
            title = document.getString("title") ?: "",
            subject = document.getString("subject") ?: "",
            testDate = document.getString("testDate") ?: "",
            totalMarks = document.getDouble("totalMarks") ?: 0.0,
            passingMarks = document.getDouble("passingMarks") ?: 0.0,
            description = document.getString("description") ?: "",
            createdById = document.getString("createdById") ?: "",
            createdByName = document.getString("createdByName") ?: "",
            status = status,
            testType = parseTestType(document.getString("testType")),
            durationMinutes = duration,
            startTime = document.getString("startTime") ?: "",
            endTime = document.getString("endTime") ?: "",
            rejectionReason = document.getString("rejectionReason") ?: "",
            submittedAt = document.getString("submittedAt") ?: "",
            approvedAt = document.getString("approvedAt") ?: "",
            approvedById = document.getString("approvedById") ?: "",
            approvedByName = document.getString("approvedByName") ?: ""
        )
    }


    // ---------------- results ----------------

    private fun resultToMap(
        result: TestResult
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to result.id,
            "testId" to result.testId,
            "studentId" to result.studentId,
            "coachingId" to result.coachingId,
            "obtainedMarks" to result.obtainedMarks,
            "remarks" to result.remarks,
            "resultDate" to result.resultDate,
            "status" to result.status
        )
    }


    suspend fun saveResult(
        result: TestResult
    ) {

        db.collection(COLLECTION_RESULTS)
            .document(result.id)
            .set(resultToMap(result))
            .await()
    }


    suspend fun deleteResult(
        resultId: String
    ) {

        db.collection(COLLECTION_RESULTS)
            .document(resultId)
            .delete()
            .await()
    }


    /**
     * Delete every result that belongs to one test
     * (used when a test itself is deleted).
     *
     * The query also filters on coachingId so it can satisfy
     * security rules that only allow reading own-coaching data.
     */
    suspend fun deleteResultsForTest(
        testId: String,
        coachingId: String
    ) {

        val snapshot =
            db.collection(COLLECTION_RESULTS)
                .whereEqualTo("coachingId", coachingId)
                .whereEqualTo("testId", testId)
                .get()
                .await()

        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }


    suspend fun findResultsByCoaching(
        coachingId: String
    ): List<TestResult> {

        val snapshot =
            db.collection(COLLECTION_RESULTS)
                .whereEqualTo("coachingId", coachingId)
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToResult(it)
        }
    }


    /**
     * Fallback for students whose security rules only allow reading
     * their OWN results (the coaching-wide query is then rejected).
     */
    suspend fun findResultsByStudent(
        coachingId: String,
        studentId: String
    ): List<TestResult> {

        val snapshot =
            db.collection(COLLECTION_RESULTS)
                .whereEqualTo("coachingId", coachingId)
                .whereEqualTo("studentId", studentId)
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToResult(it)
        }
    }


    private fun documentToResult(
        document: DocumentSnapshot
    ): TestResult? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        val testId =
            document.getString("testId")
                ?: return null

        val studentId =
            document.getString("studentId")
                ?: return null

        return TestResult(
            id = document.getString("id") ?: document.id,
            testId = testId,
            studentId = studentId,
            coachingId = coachingId,
            obtainedMarks = document.getDouble("obtainedMarks") ?: 0.0,
            remarks = document.getString("remarks") ?: "",
            resultDate = document.getString("resultDate") ?: "",
            status = document.getString("status") ?: "PUBLISHED"
        )
    }
}
