package com.slh.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class QuestionType {
    MCQ
}

data class TestQuestion(
    val id: String,
    val testId: String,
    val coachingId: String,
    val batchId: String,
    val questionText: String,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOption: String = "",
    val marks: Double = 1.0,
    val questionType: QuestionType = QuestionType.MCQ,
    val questionOrder: Int = 0
)

object QuestionStore {

    private var questionsState by mutableStateOf(
        emptyList<TestQuestion>()
    )

    private var initialized = false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /*
     * ========================================================
     * FIREBASE WRITE-THROUGH
     * ========================================================
     */

    private const val KIND_QUESTIONS = "questions"
    private const val KIND_QUESTIONS_OF_TEST = "questionsOfTest"

    private fun pendingDeleteKey(
        coachingId: String,
        testId: String
    ) = "$coachingId|$testId"


    private fun syncQuestionToFirebase(
        question: TestQuestion
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_QUESTIONS, question.id)

        cloudScope.launch {

            try {

                QuestionFirestoreRepository
                    .saveQuestion(question)

                SLHFirebase.clearPendingUpsert(
                    KIND_QUESTIONS,
                    question.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveQuestion ${question.id}",
                    e
                )
            }
        }
    }


    private fun deleteQuestionFromFirebase(
        questionId: String
    ) {

        SLHFirebase.addPendingDelete(KIND_QUESTIONS, questionId)

        cloudScope.launch {

            try {

                QuestionFirestoreRepository
                    .deleteQuestion(questionId)

                SLHFirebase.clearPendingDelete(
                    KIND_QUESTIONS,
                    questionId
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteQuestion $questionId",
                    e
                )
            }
        }
    }


    private fun deleteQuestionsForTestFromFirebase(
        testId: String,
        coachingId: String
    ) {

        val key = pendingDeleteKey(coachingId, testId)

        SLHFirebase.addPendingDelete(KIND_QUESTIONS_OF_TEST, key)

        cloudScope.launch {

            try {

                QuestionFirestoreRepository
                    .deleteQuestionsForTest(testId, coachingId)

                SLHFirebase.clearPendingDelete(
                    KIND_QUESTIONS_OF_TEST,
                    key
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteQuestionsForTest $testId",
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

        for (key in SLHFirebase.pendingDeletes(KIND_QUESTIONS_OF_TEST)) {

            val parts = key.split("|", limit = 2)

            if (parts.size != 2 || parts[0] != coachingId) {
                continue
            }

            try {

                QuestionFirestoreRepository
                    .deleteQuestionsForTest(parts[1], coachingId)

                SLHFirebase.clearPendingDelete(
                    KIND_QUESTIONS_OF_TEST,
                    key
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "retry deleteQuestionsForTest ${parts[1]}",
                    e
                )
            }
        }

        for (id in SLHFirebase.pendingDeletes(KIND_QUESTIONS)) {

            try {

                QuestionFirestoreRepository.deleteQuestion(id)

                SLHFirebase.clearPendingDelete(KIND_QUESTIONS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteQuestion $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_QUESTIONS)) {

            val question =
                questionsState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                QuestionFirestoreRepository.saveQuestion(question)

                SLHFirebase.clearPendingUpsert(KIND_QUESTIONS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveQuestion $id", e)
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
     * - canWrite (admin): first time, local questions missing
     *   in the cloud are uploaded once.
     * - Offline / error: nothing changes locally.
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

                // Push whatever failed to upload earlier.
                flushPending(coachingId)

                var cloudQuestions =
                    QuestionFirestoreRepository
                        .findQuestionsByCoaching(
                            coachingId
                        )

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "questions",
                        coachingId
                    )
                ) {

                    val cloudIds =
                        cloudQuestions
                            .map { it.id }
                            .toSet()

                    val missing =
                        questionsState.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudIds
                        }

                    missing.forEach {
                        QuestionFirestoreRepository
                            .saveQuestion(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "questions",
                        coachingId
                    )

                    cloudQuestions =
                        cloudQuestions + missing
                }

                // Keep local questions that could not be uploaded yet,
                // and do not resurrect ones whose delete is pending.
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_QUESTIONS)

                val pendingDeletedIds =
                    SLHFirebase.pendingDeletes(KIND_QUESTIONS)

                val pendingDeletedTests =
                    SLHFirebase.pendingDeletes(KIND_QUESTIONS_OF_TEST)

                val keep =
                    questionsState.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingIds
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                questionsState =
                    questionsState.filter {
                        it.coachingId != coachingId
                    } + cloudQuestions.filter {
                        it.id !in keepIds &&
                                it.id !in pendingDeletedIds &&
                                pendingDeleteKey(coachingId, it.testId) !in pendingDeletedTests
                    } + keep

                persist()

                onComplete?.invoke(true, null)

            } catch (exception: Exception) {

                SLHFirebase.logSyncFailure(
                    "QuestionStore.syncFromFirebase",
                    exception
                )

                onComplete?.invoke(
                    false,
                    exception.message
                )
            }
        }
    }


    // =====================================================
    // INITIALIZE
    // =====================================================

    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        val saved =
            SLHLocalStorage.loadQuestions()

        questionsState =
            saved ?: emptyList()
    }

    private fun persist() {

        SLHLocalStorage.saveQuestions(
            questionsState
        )
    }

    private val questions: List<TestQuestion>
        get() = questionsState

    // ------------------------------------------------------------
    // FIND
    // ------------------------------------------------------------

    fun findQuestion(questionId: String): TestQuestion? {
        return questions.find {
            it.id == questionId
        }
    }

    fun findQuestionsByTest(
        testId: String
    ): List<TestQuestion> {
        return questions
            .filter {
                it.testId == testId
            }
            .sortedBy {
                it.questionOrder
            }
    }

    fun findQuestionsByTestAndCoaching(
        testId: String,
        coachingId: String
    ): List<TestQuestion> {
        return questions
            .filter {
                it.testId == testId &&
                        it.coachingId == coachingId
            }
            .sortedBy {
                it.questionOrder
            }
    }

    fun findQuestionsByBatch(
        batchId: String
    ): List<TestQuestion> {
        return questions
            .filter {
                it.batchId == batchId
            }
            .sortedBy {
                it.questionOrder
            }
    }

    // ------------------------------------------------------------
    // ADD
    // ------------------------------------------------------------

    fun addQuestion(
        question: TestQuestion
    ): TestQuestion {

        questionsState =
            questionsState + question

        persist()

        syncQuestionToFirebase(question)

        return question
    }

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    fun createQuestion(
        testId: String,
        coachingId: String,
        batchId: String,
        questionText: String,
        optionA: String,
        optionB: String,
        optionC: String,
        optionD: String,
        correctOption: String,
        marks: Double = 1.0,
        questionType: QuestionType = QuestionType.MCQ,
        questionOrder: Int? = null
    ): TestQuestion {

        val nextOrder =
            questionOrder
                ?: (
                        questions
                            .filter {
                                it.testId == testId
                            }
                            .maxOfOrNull {
                                it.questionOrder
                            }
                            ?.plus(1)
                            ?: 1
                        )

        val question = TestQuestion(
            id = "QUESTION_${System.currentTimeMillis()}_${questions.size}",
            testId = testId,
            coachingId = coachingId,
            batchId = batchId,
            questionText = questionText,
            optionA = optionA,
            optionB = optionB,
            optionC = optionC,
            optionD = optionD,
            correctOption = correctOption,
            marks = marks,
            questionType = questionType,
            questionOrder = nextOrder
        )

        questionsState =
            questionsState + question

        persist()

        syncQuestionToFirebase(question)

        return question
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    fun updateQuestion(
        question: TestQuestion
    ): Boolean {

        val exists =
            questionsState.any {
                it.id == question.id
            }

        if (!exists) {
            return false
        }

        questionsState =
            questionsState.map {
                if (it.id == question.id) {
                    question
                } else {
                    it
                }
            }

        persist()

        syncQuestionToFirebase(question)

        return true
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    fun deleteQuestion(
        questionId: String
    ): Boolean {

        val exists =
            questionsState.any {
                it.id == questionId
            }

        if (!exists) {
            return false
        }

        questionsState =
            questionsState.filterNot {
                it.id == questionId
            }

        persist()

        deleteQuestionFromFirebase(questionId)

        return true
    }

    // ------------------------------------------------------------
    // DELETE ALL QUESTIONS OF A TEST
    // ------------------------------------------------------------

    fun deleteQuestionsByTest(
        testId: String
    ): Int {

        val before = questionsState.size

        val coachingIds =
            questionsState
                .filter { it.testId == testId }
                .map { it.coachingId }
                .toSet()

        questionsState =
            questionsState.filterNot {
                it.testId == testId
            }

        val removed =
            before - questionsState.size

        if (removed > 0) {

            persist()

            coachingIds.forEach {
                deleteQuestionsForTestFromFirebase(testId, it)
            }
        }

        return removed
    }

    // ------------------------------------------------------------
    // COUNT
    // ------------------------------------------------------------

    fun getQuestionCount(
        testId: String
    ): Int {

        return questions.count {
            it.testId == testId
        }
    }

    fun getQuestionCountByBatch(
        batchId: String
    ): Int {

        return questions.count {
            it.batchId == batchId
        }
    }

    // ------------------------------------------------------------
    // TOTAL MARKS
    // ------------------------------------------------------------

    fun getTotalQuestionMarks(
        testId: String
    ): Double {

        return questions
            .filter {
                it.testId == testId
            }
            .sumOf {
                it.marks
            }
    }

    // ------------------------------------------------------------
    // SEARCH
    // ------------------------------------------------------------

    fun searchQuestions(
        testId: String,
        query: String
    ): List<TestQuestion> {

        val q = query.trim()

        if (q.isBlank()) {
            return findQuestionsByTest(testId)
        }

        return questions
            .filter {
                it.testId == testId &&
                        (
                                it.questionText.contains(
                                    q,
                                    ignoreCase = true
                                ) ||
                                        it.optionA.contains(
                                            q,
                                            ignoreCase = true
                                        ) ||
                                        it.optionB.contains(
                                            q,
                                            ignoreCase = true
                                        ) ||
                                        it.optionC.contains(
                                            q,
                                            ignoreCase = true
                                        ) ||
                                        it.optionD.contains(
                                            q,
                                            ignoreCase = true
                                        )
                                )
            }
            .sortedBy {
                it.questionOrder
            }
    }

    // ------------------------------------------------------------
    // REORDER
    // ------------------------------------------------------------

    fun reorderQuestions(
        testId: String,
        orderedQuestionIds: List<String>
    ) {

        var updated = questionsState

        orderedQuestionIds.forEachIndexed { index, questionId ->

            updated = updated.map {
                if (
                    it.id == questionId &&
                    it.testId == testId
                ) {
                    it.copy(
                        questionOrder = index + 1
                    )
                } else {
                    it
                }
            }
        }

        questionsState = updated

        persist()

        // Push each reordered question of this test
        questionsState
            .filter { it.testId == testId }
            .forEach { syncQuestionToFirebase(it) }
    }

    // ------------------------------------------------------------
    // CLEAR
    // ------------------------------------------------------------

    fun clear() {
        questionsState = emptyList()
        persist()
    }
}
