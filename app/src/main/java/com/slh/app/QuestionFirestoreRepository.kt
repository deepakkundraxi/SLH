package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * QUESTION FIRESTORE REPOSITORY
 * ============================================================
 *
 * Collection:
 *
 *   questions/{question.id}
 *
 * Every document carries "coachingId" and "testId".
 *
 * ============================================================
 */
object QuestionFirestoreRepository {

    private const val COLLECTION_QUESTIONS = "questions"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    private fun questionToMap(
        question: TestQuestion
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to question.id,
            "testId" to question.testId,
            "coachingId" to question.coachingId,
            "batchId" to question.batchId,
            "questionText" to question.questionText,
            "optionA" to question.optionA,
            "optionB" to question.optionB,
            "optionC" to question.optionC,
            "optionD" to question.optionD,
            "correctOption" to question.correctOption,
            "marks" to question.marks,
            "questionType" to question.questionType.name,
            "questionOrder" to question.questionOrder
        )
    }


    suspend fun saveQuestion(
        question: TestQuestion
    ) {

        db.collection(COLLECTION_QUESTIONS)
            .document(question.id)
            .set(questionToMap(question))
            .await()
    }


    suspend fun deleteQuestion(
        questionId: String
    ) {

        db.collection(COLLECTION_QUESTIONS)
            .document(questionId)
            .delete()
            .await()
    }


    /**
     * Delete every question that belongs to one test.
     */
    suspend fun deleteQuestionsForTest(
        testId: String,
        coachingId: String
    ) {

        val snapshot =
            db.collection(COLLECTION_QUESTIONS)
                .whereEqualTo("coachingId", coachingId)
                .whereEqualTo("testId", testId)
                .get()
                .await()

        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }


    /**
     * Source.SERVER so offline never returns an empty cache
     * that would wipe good local data on sync.
     */
    suspend fun findQuestionsByCoaching(
        coachingId: String
    ): List<TestQuestion> {

        val snapshot =
            db.collection(COLLECTION_QUESTIONS)
                .whereEqualTo("coachingId", coachingId)
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToQuestion(it)
        }
    }


    private fun documentToQuestion(
        document: DocumentSnapshot
    ): TestQuestion? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        val testId =
            document.getString("testId")
                ?: return null

        val typeName =
            document.getString("questionType")
                ?: QuestionType.MCQ.name

        val questionType =
            try {
                QuestionType.valueOf(typeName)
            } catch (_: Exception) {
                QuestionType.MCQ
            }

        val order =
            when (val raw = document.get("questionOrder")) {
                is Long -> raw.toInt()
                is Int -> raw
                is Double -> raw.toInt()
                else -> 0
            }

        return TestQuestion(
            id = document.getString("id") ?: document.id,
            testId = testId,
            coachingId = coachingId,
            batchId = document.getString("batchId") ?: "",
            questionText = document.getString("questionText") ?: "",
            optionA = document.getString("optionA") ?: "",
            optionB = document.getString("optionB") ?: "",
            optionC = document.getString("optionC") ?: "",
            optionD = document.getString("optionD") ?: "",
            correctOption = document.getString("correctOption") ?: "",
            marks = document.getDouble("marks") ?: 1.0,
            questionType = questionType,
            questionOrder = order
        )
    }
}
