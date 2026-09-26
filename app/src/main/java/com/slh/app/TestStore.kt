package com.slh.app

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Test lifecycle:
 *
 * Teacher creates  → DRAFT
 * Teacher submits  → PENDING_APPROVAL
 * Admin approves   → PUBLISHED (student-visible)
 * Admin rejects    → REJECTED (teacher edits + resubmits)
 *
 * Students must only ever see APPROVED / PUBLISHED / legacy ACTIVE.
 */
enum class TestStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    PUBLISHED,
    REJECTED,
    COMPLETED,
    CANCELLED,

    /** Legacy status from older builds — treated as student-visible. */
    ACTIVE
}

enum class TestType {
    LIVE,
    QUIZ
}

fun TestStatus.isVisibleToStudent(): Boolean =
    this == TestStatus.APPROVED ||
            this == TestStatus.PUBLISHED ||
            this == TestStatus.ACTIVE

fun TestStatus.canTeacherEdit(): Boolean =
    this == TestStatus.DRAFT ||
            this == TestStatus.REJECTED

fun TestStatus.canSubmitForApproval(): Boolean =
    this == TestStatus.DRAFT ||
            this == TestStatus.REJECTED

fun parseTestStatus(raw: String?): TestStatus {
    if (raw.isNullOrBlank()) return TestStatus.DRAFT
    return try {
        TestStatus.valueOf(raw)
    } catch (_: Exception) {
        TestStatus.ACTIVE
    }
}

fun parseTestType(raw: String?): TestType {
    if (raw.isNullOrBlank()) return TestType.QUIZ
    return try {
        TestType.valueOf(raw)
    } catch (_: Exception) {
        TestType.QUIZ
    }
}

data class CoachingTest(
    val id: String,
    val coachingId: String,
    val batchId: String,
    val title: String,
    val subject: String = "",
    val testDate: String = "",
    val totalMarks: Double = 0.0,
    val passingMarks: Double = 0.0,
    val description: String = "",
    val createdById: String = "",
    val createdByName: String = "",
    val status: TestStatus = TestStatus.DRAFT,
    val testType: TestType = TestType.QUIZ,
    /** Duration in minutes (mainly for LIVE tests). */
    val durationMinutes: Int = 0,
    /** Optional schedule window, e.g. "10:00" */
    val startTime: String = "",
    /** Optional schedule window, e.g. "12:00" */
    val endTime: String = "",
    val rejectionReason: String = "",
    val submittedAt: String = "",
    val approvedAt: String = "",
    val approvedById: String = "",
    val approvedByName: String = ""
)

data class TestResult(
    val id: String,
    val testId: String,
    val studentId: String,
    val coachingId: String,
    val obtainedMarks: Double = 0.0,
    val remarks: String = "",
    val resultDate: String = "",
    val status: String = "PUBLISHED"
)

/*
 * -------------------------------------------------------------
 * RESULT ANALYTICS MODEL
 * -------------------------------------------------------------
 */

data class TestResultSummary(
    val testId: String,
    val coachingId: String,
    val batchId: String,
    val totalStudents: Int,
    val enteredResults: Int,
    val publishedResults: Int,
    val passedResults: Int,
    val failedResults: Int,
    val averagePercentage: Double,
    val highestMarks: Double,
    val lowestMarks: Double
)

/*
 * Student-wise performance information.
 */
data class StudentTestPerformance(
    val studentId: String,
    val testId: String,
    val obtainedMarks: Double,
    val totalMarks: Double,
    val percentage: Double,
    val passed: Boolean,
    val published: Boolean,
    val position: Int = 0
)

object TestStore {

    private val testsState =
        mutableStateListOf<CoachingTest>()

    private val resultsState =
        mutableStateListOf<TestResult>()

    private var initialized = false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /*
     * ========================================================
     * FIREBASE WRITE-THROUGH
     * ========================================================
     *
     * Local data is saved first (app keeps working offline),
     * then the change is pushed to Firestore in the background.
     */

    private const val KIND_TESTS = "tests"
    private const val KIND_RESULTS = "testResults"

    private fun pendingDeleteKey(
        coachingId: String,
        testId: String
    ) = "$coachingId|$testId"


    private fun syncTestToFirebase(
        test: CoachingTest
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_TESTS, test.id)

        cloudScope.launch {

            try {

                TestFirestoreRepository
                    .saveTest(test)

                SLHFirebase.clearPendingUpsert(
                    KIND_TESTS,
                    test.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveTest ${test.id}",
                    e
                )
            }
        }
    }


    private fun deleteTestFromFirebase(
        testId: String,
        coachingId: String
    ) {

        val key = pendingDeleteKey(coachingId, testId)

        SLHFirebase.addPendingDelete(KIND_TESTS, key)

        cloudScope.launch {

            try {

                TestFirestoreRepository
                    .deleteTest(testId)

                TestFirestoreRepository
                    .deleteResultsForTest(testId, coachingId)

                SLHFirebase.clearPendingDelete(
                    KIND_TESTS,
                    key
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteTest $testId",
                    e
                )
            }
        }
    }


    private fun syncResultToFirebase(
        result: TestResult
    ) {

        SLHFirebase.addPendingUpsert(KIND_RESULTS, result.id)

        cloudScope.launch {

            try {

                TestFirestoreRepository
                    .saveResult(result)

                SLHFirebase.clearPendingUpsert(
                    KIND_RESULTS,
                    result.id
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "saveResult ${result.id}",
                    e
                )
            }
        }
    }


    private fun deleteResultFromFirebase(
        resultId: String
    ) {

        SLHFirebase.addPendingDelete(KIND_RESULTS, resultId)

        cloudScope.launch {

            try {

                TestFirestoreRepository
                    .deleteResult(resultId)

                SLHFirebase.clearPendingDelete(
                    KIND_RESULTS,
                    resultId
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteResult $resultId",
                    e
                )
            }
        }
    }


    /**
     * Retries everything that failed to reach Firestore earlier.
     * Runs at the start of every syncFromFirebase().
     */
    private suspend fun flushPending(
        coachingId: String
    ) {

        for (key in SLHFirebase.pendingDeletes(KIND_TESTS)) {

            val parts = key.split("|", limit = 2)

            if (parts.size != 2 || parts[0] != coachingId) {
                continue
            }

            try {

                TestFirestoreRepository.deleteTest(parts[1])

                TestFirestoreRepository
                    .deleteResultsForTest(parts[1], coachingId)

                SLHFirebase.clearPendingDelete(KIND_TESTS, key)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteTest ${parts[1]}", e)
            }
        }

        for (id in SLHFirebase.pendingDeletes(KIND_RESULTS)) {

            try {

                TestFirestoreRepository.deleteResult(id)

                SLHFirebase.clearPendingDelete(KIND_RESULTS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteResult $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_TESTS)) {

            val test =
                testsState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                TestFirestoreRepository.saveTest(test)

                SLHFirebase.clearPendingUpsert(KIND_TESTS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveTest $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_RESULTS)) {

            val result =
                resultsState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                TestFirestoreRepository.saveResult(result)

                SLHFirebase.clearPendingUpsert(KIND_RESULTS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveResult $id", e)
            }
        }
    }


    /*
     * ========================================================
     * FIREBASE -> LOCAL SYNC
     * ========================================================
     *
     * Call after login with the user's coachingId.
     *
     * - Cloud is the source of truth for this coaching.
     * - canWrite (admin): first time, local tests/results
     *   missing in the cloud are uploaded once.
     * - Offline / error: nothing changes locally.
     */
    fun syncFromFirebase(
        coachingId: String,
        canWrite: Boolean = false,
        onComplete: ((Boolean, String?) -> Unit)? = null,
        studentId: String? = null
    ) {

        if (coachingId.isBlank()) {

            onComplete?.invoke(false, "coachingId is blank")

            return
        }

        cloudScope.launch {

            try {

                // 1. Push whatever failed to upload earlier.
                flushPending(coachingId)

                var cloudTests =
                    TestFirestoreRepository
                        .findTestsByCoaching(
                            coachingId
                        )

                // 2. Results are fetched on their own: if this query is
                //    rejected (security rules) the tests must still load.
                var cloudResults: List<TestResult>? =
                    try {

                        TestFirestoreRepository
                            .findResultsByCoaching(
                                coachingId
                            )

                    } catch (e: Exception) {

                        SLHFirebase.logSyncFailure(
                            "findResultsByCoaching",
                            e
                        )

                        if (studentId != null) {

                            try {

                                TestFirestoreRepository
                                    .findResultsByStudent(
                                        coachingId,
                                        studentId
                                    )

                            } catch (e2: Exception) {

                                SLHFirebase.logSyncFailure(
                                    "findResultsByStudent",
                                    e2
                                )

                                null
                            }

                        } else {

                            null
                        }
                    }

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "tests",
                        coachingId
                    )
                ) {

                    val cloudTestIds =
                        cloudTests.map { it.id }.toSet()

                    val missingTests =
                        testsState.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudTestIds
                        }

                    missingTests.forEach {
                        TestFirestoreRepository.saveTest(it)
                    }

                    cloudTests = cloudTests + missingTests

                    // Only when results could be read, otherwise we
                    // cannot know which ones are missing.
                    if (cloudResults != null) {

                        val cloudResultIds =
                            cloudResults.map { it.id }.toSet()

                        val missingResults =
                            resultsState.filter {
                                it.coachingId == coachingId &&
                                        it.id !in cloudResultIds
                            }

                        missingResults.forEach {
                            TestFirestoreRepository.saveResult(it)
                        }

                        cloudResults = cloudResults + missingResults

                        SLHFirebase.markFirstUploadDone(
                            "tests",
                            coachingId
                        )
                    }
                }

                // 3. Replace only this coaching's slice - but keep local
                //    records that could not be uploaded yet, and do not
                //    resurrect records whose delete is still pending.
                val pendingTestIds =
                    SLHFirebase.pendingUpserts(KIND_TESTS)

                val pendingDeletedTests =
                    SLHFirebase.pendingDeletes(KIND_TESTS)

                val keepTests =
                    testsState.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingTestIds
                    }

                val keepTestIds =
                    keepTests.map { it.id }.toSet()

                testsState.removeAll {
                    it.coachingId == coachingId
                }

                testsState.addAll(
                    cloudTests.filter {
                        it.id !in keepTestIds &&
                                pendingDeleteKey(coachingId, it.id) !in pendingDeletedTests
                    }
                )

                testsState.addAll(keepTests)

                if (cloudResults != null) {

                    val pendingResultIds =
                        SLHFirebase.pendingUpserts(KIND_RESULTS)

                    val pendingDeletedResults =
                        SLHFirebase.pendingDeletes(KIND_RESULTS)

                    val keepResults =
                        resultsState.filter {
                            it.coachingId == coachingId &&
                                    it.id in pendingResultIds
                        }

                    val keepResultIds =
                        keepResults.map { it.id }.toSet()

                    resultsState.removeAll {
                        it.coachingId == coachingId
                    }

                    resultsState.addAll(
                        cloudResults.filter {
                            it.id !in keepResultIds &&
                                    it.id !in pendingDeletedResults
                        }
                    )

                    resultsState.addAll(keepResults)
                }

                persistTests()
                persistResults()

                onComplete?.invoke(true, null)

            } catch (exception: Exception) {

                SLHFirebase.logSyncFailure(
                    "TestStore.syncFromFirebase",
                    exception
                )

                onComplete?.invoke(
                    false,
                    exception.message
                )
            }
        }
    }


    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        SLHLocalStorage.loadTests()?.let {
            testsState.addAll(it)
        }

        SLHLocalStorage.loadTestResults()?.let {
            resultsState.addAll(it)
        }
    }

    private fun persistTests() {

        SLHLocalStorage.saveTests(
            testsState.toList()
        )
    }

    private fun persistResults() {

        SLHLocalStorage.saveTestResults(
            resultsState.toList()
        )
    }

    val tests: List<CoachingTest>
        get() = testsState

    val results: List<TestResult>
        get() = resultsState

    /*
     * ---------------------------------------------------------
     * TEST FIND / LIST
     * ---------------------------------------------------------
     */

    fun findTest(
        testId: String
    ): CoachingTest? =
        testsState.firstOrNull {
            it.id == testId
        }

    fun findTestsByCoaching(
        coachingId: String
    ): List<CoachingTest> =
        testsState
            .filter {
                it.coachingId == coachingId
            }
            .sortedByDescending {
                it.id
            }

    fun findTestsByBatch(
        batchId: String
    ): List<CoachingTest> =
        testsState
            .filter {
                it.batchId == batchId
            }
            .sortedByDescending {
                it.id
            }

    fun findTestsByBatchAndCoaching(
        batchId: String,
        coachingId: String
    ): List<CoachingTest> =
        testsState
            .filter {
                it.batchId == batchId &&
                        it.coachingId == coachingId
            }
            .sortedByDescending {
                it.id
            }

    /**
     * Student-visible tests only (APPROVED / PUBLISHED / legacy ACTIVE).
     * Never returns DRAFT, PENDING_APPROVAL, or REJECTED.
     */
    fun getActiveTestsByBatch(
        batchId: String,
        coachingId: String
    ): List<CoachingTest> =
        testsState
            .filter {
                it.batchId == batchId &&
                        it.coachingId == coachingId &&
                        it.status.isVisibleToStudent()
            }
            .sortedByDescending {
                it.id
            }

    fun getPendingApprovalTests(
        coachingId: String
    ): List<CoachingTest> =
        testsState
            .filter {
                it.coachingId == coachingId &&
                        it.status == TestStatus.PENDING_APPROVAL
            }
            .sortedByDescending {
                it.submittedAt.ifBlank { it.id }
            }

    fun getPendingLiveCount(coachingId: String): Int =
        testsState.count {
            it.coachingId == coachingId &&
                    it.status == TestStatus.PENDING_APPROVAL &&
                    it.testType == TestType.LIVE
        }

    fun getPendingQuizCount(coachingId: String): Int =
        testsState.count {
            it.coachingId == coachingId &&
                    it.status == TestStatus.PENDING_APPROVAL &&
                    it.testType == TestType.QUIZ
        }

    /*
     * ---------------------------------------------------------
     * TEST CREATE / UPDATE
     * ---------------------------------------------------------
     */

    fun addTest(
        test: CoachingTest
    ): Boolean {

        if (test.id.isBlank()) {
            return false
        }

        if (
            testsState.any {
                it.id == test.id
            }
        ) {
            return false
        }

        testsState.add(test)

        persistTests()

        syncTestToFirebase(test)

        return true
    }

    fun createTest(
        coachingId: String,
        batchId: String,
        title: String,
        subject: String = "",
        testDate: String = "",
        totalMarks: Double = 0.0,
        passingMarks: Double = 0.0,
        description: String = "",
        createdById: String = "",
        createdByName: String = "",
        testType: TestType = TestType.QUIZ,
        durationMinutes: Int = 0,
        startTime: String = "",
        endTime: String = ""
    ): CoachingTest {

        val safeTotalMarks =
            maxOf(
                0.0,
                totalMarks
            )

        val safePassingMarks =
            passingMarks
                .coerceAtLeast(0.0)
                .coerceAtMost(
                    safeTotalMarks
                )

        val test =
            CoachingTest(
                id =
                    "TEST_${System.currentTimeMillis()}",
                coachingId =
                    coachingId,
                batchId =
                    batchId,
                title =
                    title.trim(),
                subject =
                    subject.trim(),
                testDate =
                    testDate,
                totalMarks =
                    safeTotalMarks,
                passingMarks =
                    safePassingMarks,
                description =
                    description.trim(),
                createdById =
                    createdById,
                createdByName =
                    createdByName,
                status =
                    TestStatus.DRAFT,
                testType =
                    testType,
                durationMinutes =
                    durationMinutes.coerceAtLeast(0),
                startTime =
                    startTime.trim(),
                endTime =
                    endTime.trim()
            )

        testsState.add(test)

        persistTests()

        syncTestToFirebase(test)

        return test
    }

    fun updateTest(
        testId: String,
        title: String,
        subject: String = "",
        testDate: String = "",
        totalMarks: Double = 0.0,
        passingMarks: Double = 0.0,
        description: String = "",
        testType: TestType? = null,
        durationMinutes: Int? = null,
        startTime: String? = null,
        endTime: String? = null
    ): Boolean {

        val index =
            testsState.indexOfFirst {
                it.id == testId
            }

        if (index < 0) {
            return false
        }

        val current =
            testsState[index]

        // Only allow content edits while DRAFT or REJECTED.
        if (!current.status.canTeacherEdit() &&
            testType == null &&
            durationMinutes == null
        ) {
            // Still allow metadata update for approved tests via setTestStatus path.
        }

        val safeTotalMarks =
            maxOf(
                0.0,
                totalMarks
            )

        val safePassingMarks =
            passingMarks
                .coerceAtLeast(0.0)
                .coerceAtMost(
                    safeTotalMarks
                )

        testsState[index] =
            current.copy(
                title =
                    title.trim(),
                subject =
                    subject.trim(),
                testDate =
                    testDate,
                totalMarks =
                    safeTotalMarks,
                passingMarks =
                    safePassingMarks,
                description =
                    description.trim(),
                testType =
                    testType ?: current.testType,
                durationMinutes =
                    durationMinutes?.coerceAtLeast(0)
                        ?: current.durationMinutes,
                startTime =
                    startTime?.trim() ?: current.startTime,
                endTime =
                    endTime?.trim() ?: current.endTime
            )

        persistTests()

        syncTestToFirebase(testsState[index])

        return true
    }

    fun setTestStatus(
        testId: String,
        status: TestStatus
    ): Boolean {

        val index =
            testsState.indexOfFirst {
                it.id == testId
            }

        if (index < 0) {
            return false
        }

        testsState[index] =
            testsState[index].copy(
                status = status
            )

        persistTests()

        syncTestToFirebase(testsState[index])

        return true
    }

    /*
     * ---------------------------------------------------------
     * APPROVAL WORKFLOW
     * ---------------------------------------------------------
     */

    private fun nowStamp(): String =
        java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            java.util.Locale.US
        ).format(java.util.Date())

    /**
     * Teacher: DRAFT / REJECTED → PENDING_APPROVAL
     * Requires at least one question.
     */
    fun submitForApproval(
        testId: String
    ): Boolean {

        val index =
            testsState.indexOfFirst {
                it.id == testId
            }

        if (index < 0) return false

        val current = testsState[index]

        if (!current.status.canSubmitForApproval()) {
            return false
        }

        val questionCount =
            QuestionStore.findQuestionsByTest(testId).size

        if (questionCount <= 0) {
            return false
        }

        testsState[index] =
            current.copy(
                status = TestStatus.PENDING_APPROVAL,
                submittedAt = nowStamp(),
                rejectionReason = ""
            )

        persistTests()
        syncTestToFirebase(testsState[index])

        return true
    }

    /**
     * Admin: PENDING_APPROVAL → PUBLISHED (student-visible).
     * Also sets APPROVED intermediate fields for audit.
     */
    fun approveTest(
        testId: String,
        adminId: String,
        adminName: String
    ): Boolean {

        val index =
            testsState.indexOfFirst {
                it.id == testId
            }

        if (index < 0) return false

        val current = testsState[index]

        if (current.status != TestStatus.PENDING_APPROVAL) {
            return false
        }

        val stamp = nowStamp()

        testsState[index] =
            current.copy(
                status = TestStatus.PUBLISHED,
                approvedAt = stamp,
                approvedById = adminId,
                approvedByName = adminName,
                rejectionReason = ""
            )

        persistTests()
        syncTestToFirebase(testsState[index])

        return true
    }

    /**
     * Admin: PENDING_APPROVAL → REJECTED with reason.
     */
    fun rejectTest(
        testId: String,
        reason: String,
        adminId: String = "",
        adminName: String = ""
    ): Boolean {

        val index =
            testsState.indexOfFirst {
                it.id == testId
            }

        if (index < 0) return false

        val current = testsState[index]

        if (current.status != TestStatus.PENDING_APPROVAL) {
            return false
        }

        val safeReason = reason.trim()
        if (safeReason.isBlank()) return false

        testsState[index] =
            current.copy(
                status = TestStatus.REJECTED,
                rejectionReason = safeReason,
                approvedAt = "",
                approvedById = adminId,
                approvedByName = adminName
            )

        persistTests()
        syncTestToFirebase(testsState[index])

        return true
    }

    /**
     * Teacher after fix: REJECTED → PENDING_APPROVAL again.
     */
    fun resubmitTest(
        testId: String
    ): Boolean = submitForApproval(testId)

    fun deleteTest(
        testId: String
    ): Boolean {

        val before =
            testsState.size

        val coachingId =
            testsState
                .firstOrNull { it.id == testId }
                ?.coachingId

        testsState.removeAll {
            it.id == testId
        }

        resultsState.removeAll {
            it.testId == testId
        }

        persistTests()
        persistResults()

        if (testsState.size < before) {

            if (coachingId != null) {

                deleteTestFromFirebase(testId, coachingId)
            }

            // Questions of a deleted test used to stay behind
            // (locally and in Firestore) forever.
            QuestionStore.deleteQuestionsByTest(testId)
        }

        return testsState.size < before
    }

    /*
     * ---------------------------------------------------------
     * TEST SEARCH / COUNTS
     * ---------------------------------------------------------
     */

    fun searchTests(
        coachingId: String,
        query: String
    ): List<CoachingTest> {

        val q =
            query.trim()

        if (q.isBlank()) {
            return findTestsByCoaching(
                coachingId
            )
        }

        return testsState
            .filter {
                it.coachingId == coachingId &&
                        (
                                it.title.contains(
                                    q,
                                    ignoreCase = true
                                ) ||
                                        it.subject.contains(
                                            q,
                                            ignoreCase = true
                                        ) ||
                                        it.description.contains(
                                            q,
                                            ignoreCase = true
                                        )
                                )
            }
            .sortedByDescending {
                it.id
            }
    }

    fun getTestCount(
        coachingId: String
    ): Int =
        testsState.count {
            it.coachingId == coachingId
        }

    fun getActiveTestCount(
        coachingId: String
    ): Int =
        testsState.count {
            it.coachingId == coachingId &&
                    it.status.isVisibleToStudent()
        }

    fun getPendingApprovalCount(
        coachingId: String
    ): Int =
        testsState.count {
            it.coachingId == coachingId &&
                    it.status == TestStatus.PENDING_APPROVAL
        }

    fun getBatchTestCount(
        batchId: String,
        coachingId: String
    ): Int =
        testsState.count {
            it.batchId == batchId &&
                    it.coachingId == coachingId
        }

    /*
     * ---------------------------------------------------------
     * RESULT FIND
     * ---------------------------------------------------------
     */

    fun findResult(
        testId: String,
        studentId: String
    ): TestResult? =
        resultsState.firstOrNull {
            it.testId == testId &&
                    it.studentId == studentId
        }

    fun findResultById(
        resultId: String
    ): TestResult? =
        resultsState.firstOrNull {
            it.id == resultId
        }

    fun getTestResults(
        testId: String
    ): List<TestResult> =
        resultsState
            .filter {
                it.testId == testId
            }
            .sortedBy {
                it.studentId
            }

    fun getStudentResults(
        studentId: String,
        coachingId: String
    ): List<TestResult> =
        resultsState
            .filter {
                it.studentId == studentId &&
                        it.coachingId == coachingId
            }
            .sortedByDescending {
                it.resultDate
            }

    fun getBatchResults(
        batchId: String,
        coachingId: String
    ): List<TestResult> {

        val testIds =
            testsState
                .filter {
                    it.batchId == batchId &&
                            it.coachingId == coachingId
                }
                .map {
                    it.id
                }
                .toSet()

        return resultsState.filter {
            it.testId in testIds &&
                    it.coachingId == coachingId
        }
    }

    /*
     * ---------------------------------------------------------
     * ADD / UPDATE RESULT
     * ---------------------------------------------------------
     */

    fun addOrUpdateResult(
        testId: String,
        studentId: String,
        coachingId: String,
        obtainedMarks: Double,
        remarks: String = "",
        resultDate: String = "",
        status: String = "PUBLISHED"
    ): TestResult? {

        val test =
            findTest(testId)
                ?: return null

        if (
            test.coachingId != coachingId
        ) {
            return null
        }

        val safeMarks =
            obtainedMarks
                .coerceAtLeast(0.0)
                .coerceAtMost(
                    test.totalMarks
                )

        val existingIndex =
            resultsState.indexOfFirst {
                it.testId == testId &&
                        it.studentId == studentId
            }

        val result =
            TestResult(
                id =
                    if (existingIndex >= 0) {
                        resultsState[
                            existingIndex
                        ].id
                    } else {
                        // One document per (test, student): a resubmit
                        // from another phone, or two students finishing in
                        // the same millisecond, can no longer collide or
                        // create duplicates.
                        "RESULT_${testId}_${studentId}".replace("/", "_")
                    },

                testId =
                    testId,

                studentId =
                    studentId,

                coachingId =
                    coachingId,

                obtainedMarks =
                    safeMarks,

                remarks =
                    remarks.trim(),

                resultDate =
                    resultDate,

                status =
                    status
            )

        if (
            existingIndex >= 0
        ) {

            resultsState[
                existingIndex
            ] = result

        } else {

            resultsState.add(
                result
            )
        }

        persistResults()

        syncResultToFirebase(result)

        return result
    }

    /*
     * ---------------------------------------------------------
     * DELETE RESULT
     * ---------------------------------------------------------
     */

    fun deleteResult(
        testId: String,
        studentId: String
    ): Boolean {

        val toDelete =
            resultsState.filter {
                it.testId == testId &&
                        it.studentId == studentId
            }

        if (toDelete.isEmpty()) {
            return false
        }

        resultsState.removeAll {
            it.testId == testId &&
                    it.studentId == studentId
        }

        persistResults()

        toDelete.forEach {
            deleteResultFromFirebase(it.id)
        }

        return true
    }

    /*
     * ---------------------------------------------------------
     * RESULT COUNTS
     * ---------------------------------------------------------
     */

    fun getResultCount(
        testId: String
    ): Int =
        resultsState.count {
            it.testId == testId
        }

    fun getPublishedResultCount(
        testId: String
    ): Int =
        resultsState.count {
            it.testId == testId &&
                    it.status.equals(
                        "PUBLISHED",
                        ignoreCase = true
                    )
        }

    fun getUnpublishedResultCount(
        testId: String
    ): Int =
        resultsState.count {
            it.testId == testId &&
                    !it.status.equals(
                        "PUBLISHED",
                        ignoreCase = true
                    )
        }

    fun getPassedResultCount(
        testId: String
    ): Int {

        val test =
            findTest(testId)
                ?: return 0

        return resultsState.count {
            it.testId == testId &&
                    it.obtainedMarks >=
                    test.passingMarks
        }
    }

    fun getFailedResultCount(
        testId: String
    ): Int {

        val test =
            findTest(testId)
                ?: return 0

        return resultsState.count {
            it.testId == testId &&
                    it.obtainedMarks <
                    test.passingMarks
        }
    }

    /*
     * ---------------------------------------------------------
     * PERCENTAGE
     * ---------------------------------------------------------
     */

    fun getResultPercentage(
        testId: String,
        studentId: String
    ): Double? {

        val test =
            findTest(testId)
                ?: return null

        val result =
            findResult(
                testId,
                studentId
            ) ?: return null

        if (
            test.totalMarks <= 0.0
        ) {
            return 0.0
        }

        return (
                result.obtainedMarks /
                        test.totalMarks
                ) * 100.0
    }

    /*
     * ---------------------------------------------------------
     * AVERAGE PERCENTAGE
     * ---------------------------------------------------------
     */

    fun getAveragePercentage(
        testId: String
    ): Double {

        val test =
            findTest(testId)
                ?: return 0.0

        if (
            test.totalMarks <= 0.0
        ) {
            return 0.0
        }

        val results =
            getTestResults(testId)

        if (results.isEmpty()) {
            return 0.0
        }

        return results
            .map {
                (
                        it.obtainedMarks /
                                test.totalMarks
                        ) * 100.0
            }
            .average()
    }

    /*
     * ---------------------------------------------------------
     * HIGHEST / LOWEST
     * ---------------------------------------------------------
     */

    fun getHighestMarks(
        testId: String
    ): Double {

        return getTestResults(testId)
            .maxOfOrNull {
                it.obtainedMarks
            } ?: 0.0
    }

    fun getLowestMarks(
        testId: String
    ): Double {

        return getTestResults(testId)
            .minOfOrNull {
                it.obtainedMarks
            } ?: 0.0
    }

    /*
     * ---------------------------------------------------------
     * TEST RESULT SUMMARY
     * ---------------------------------------------------------
     */

    fun getTestResultSummary(
        testId: String
    ): TestResultSummary? {

        val test =
            findTest(testId)
                ?: return null

        val results =
            getTestResults(testId)

        val passed =
            results.count {
                it.obtainedMarks >=
                        test.passingMarks
            }

        val failed =
            results.count {
                it.obtainedMarks <
                        test.passingMarks
            }

        return TestResultSummary(

            testId =
                test.id,

            coachingId =
                test.coachingId,

            batchId =
                test.batchId,

            totalStudents =
                StudentStore.students.count {
                    it.coachingId ==
                            test.coachingId &&
                            it.batchId ==
                            test.batchId
                },

            enteredResults =
                results.size,

            publishedResults =
                results.count {
                    it.status.equals(
                        "PUBLISHED",
                        ignoreCase = true
                    )
                },

            passedResults =
                passed,

            failedResults =
                failed,

            averagePercentage =
                getAveragePercentage(
                    testId
                ),

            highestMarks =
                getHighestMarks(
                    testId
                ),

            lowestMarks =
                getLowestMarks(
                    testId
                )
        )
    }

    /*
     * ---------------------------------------------------------
     * STUDENT PERFORMANCE LIST
     * ---------------------------------------------------------
     *
     * Sorted by obtained marks descending.
     */

    fun getStudentPerformance(
        testId: String,
        includeUnpublished: Boolean = true
    ): List<StudentTestPerformance> {

        val test =
            findTest(testId)
                ?: return emptyList()

        val results =
            getTestResults(testId)
                .filter {
                    includeUnpublished ||
                            it.status.equals(
                                "PUBLISHED",
                                ignoreCase = true
                            )
                }
                .sortedByDescending {
                    it.obtainedMarks
                }

        return results.mapIndexed { index, result ->

            val percentage =
                if (
                    test.totalMarks > 0.0
                ) {
                    (
                            result.obtainedMarks /
                                    test.totalMarks
                            ) * 100.0
                } else {
                    0.0
                }

            StudentTestPerformance(

                studentId =
                    result.studentId,

                testId =
                    test.id,

                obtainedMarks =
                    result.obtainedMarks,

                totalMarks =
                    test.totalMarks,

                percentage =
                    percentage,

                passed =
                    result.obtainedMarks >=
                            test.passingMarks,

                published =
                    result.status.equals(
                        "PUBLISHED",
                        ignoreCase = true
                    ),

                position =
                    index + 1
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * TOP STUDENTS
     * ---------------------------------------------------------
     */

    fun getTopStudentPerformances(
        testId: String,
        limit: Int = 3,
        publishedOnly: Boolean = false
    ): List<StudentTestPerformance> {

        if (limit <= 0) {
            return emptyList()
        }

        return getStudentPerformance(
            testId = testId,
            includeUnpublished = !publishedOnly
        ).take(limit)
    }

    /*
     * ---------------------------------------------------------
     * STUDENT POSITION
     * ---------------------------------------------------------
     */

    fun getStudentPosition(
        testId: String,
        studentId: String,
        publishedOnly: Boolean = false
    ): Int {

        val list =
            getStudentPerformance(
                testId = testId,
                includeUnpublished =
                    !publishedOnly
            )

        return list
            .firstOrNull {
                it.studentId ==
                        studentId
            }
            ?.position ?: 0
    }

    /*
     * ---------------------------------------------------------
     * STUDENT AVERAGE
     * ---------------------------------------------------------
     */

    fun getStudentAveragePercentage(
        studentId: String,
        coachingId: String,
        publishedOnly: Boolean = true
    ): Double {

        val studentResults =
            getStudentResults(
                studentId =
                    studentId,
                coachingId =
                    coachingId
            ).filter {

                !publishedOnly ||
                        it.status.equals(
                            "PUBLISHED",
                            ignoreCase = true
                        )
            }

        if (
            studentResults.isEmpty()
        ) {
            return 0.0
        }

        val percentages =
            studentResults.mapNotNull { result ->

                val test =
                    findTest(
                        result.testId
                    ) ?: return@mapNotNull null

                if (
                    test.totalMarks <= 0.0
                ) {
                    return@mapNotNull null
                }

                (
                        result.obtainedMarks /
                                test.totalMarks
                        ) * 100.0
            }

        if (
            percentages.isEmpty()
        ) {
            return 0.0
        }

        return percentages.average()
    }

    /*
     * ---------------------------------------------------------
     * BATCH RESULT TOTAL
     * ---------------------------------------------------------
     */

    fun getBatchResultCount(
        batchId: String,
        coachingId: String
    ): Int {

        val testIds =
            testsState
                .filter {
                    it.batchId == batchId &&
                            it.coachingId == coachingId
                }
                .map {
                    it.id
                }
                .toSet()

        return resultsState.count {
            it.testId in testIds &&
                    it.coachingId == coachingId
        }
    }

    fun getBatchPublishedResultCount(
        batchId: String,
        coachingId: String
    ): Int {

        val testIds =
            testsState
                .filter {
                    it.batchId == batchId &&
                            it.coachingId == coachingId
                }
                .map {
                    it.id
                }
                .toSet()

        return resultsState.count {
            it.testId in testIds &&
                    it.coachingId == coachingId &&
                    it.status.equals(
                        "PUBLISHED",
                        ignoreCase = true
                    )
        }
    }

    /*
     * ---------------------------------------------------------
     * CLEAR
     * ---------------------------------------------------------
     */

    fun clear() {

        testsState.clear()

        resultsState.clear()

        persistTests()
        persistResults()
    }
}