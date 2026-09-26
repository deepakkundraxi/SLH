package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * BATCH FIRESTORE REPOSITORY
 * ============================================================
 *
 * Firebase Firestore layer for coaching batches.
 *
 * Collection:
 *
 * batches/{batch.id}
 *
 * Every document carries "coachingId" so that one coaching
 * only ever reads its own batches.
 *
 * ============================================================
 */
object BatchFirestoreRepository {

    private const val COLLECTION_BATCHES = "batches"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    private fun batchToMap(
        batch: CoachingBatch
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to batch.id,
            "coachingId" to batch.coachingId,
            "name" to batch.name,
            "code" to batch.code,
            "course" to batch.course,
            "subject" to batch.subject,
            "teacherId" to batch.teacherId,
            "startDate" to batch.startDate,
            "endDate" to batch.endDate,
            "timing" to batch.timing,
            "room" to batch.room,
            "description" to batch.description,
            "status" to batch.status
        )
    }


    /**
     * Create or overwrite a batch document.
     */
    suspend fun saveBatch(
        batch: CoachingBatch
    ) {

        db.collection(COLLECTION_BATCHES)
            .document(batch.id)
            .set(batchToMap(batch))
            .await()
    }


    suspend fun deleteBatch(
        batchId: String
    ) {

        db.collection(COLLECTION_BATCHES)
            .document(batchId)
            .delete()
            .await()
    }


    /**
     * All batches of one coaching.
     *
     * Source.SERVER is used on purpose: when the phone is offline
     * this throws instead of returning an empty/stale cache, so a
     * sync can never wipe or overwrite good local data by mistake.
     */
    suspend fun findBatchesByCoaching(
        coachingId: String
    ): List<CoachingBatch> {

        val snapshot =
            db.collection(COLLECTION_BATCHES)
                .whereEqualTo(
                    "coachingId",
                    coachingId
                )
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToBatch(it)
        }
    }


    private fun documentToBatch(
        document: DocumentSnapshot
    ): CoachingBatch? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        return CoachingBatch(
            id = document.getString("id") ?: document.id,
            coachingId = coachingId,
            name = document.getString("name") ?: "",
            code = document.getString("code") ?: "",
            course = document.getString("course") ?: "",
            subject = document.getString("subject") ?: "",
            teacherId = document.getString("teacherId") ?: "",
            startDate = document.getString("startDate") ?: "",
            endDate = document.getString("endDate") ?: "",
            timing = document.getString("timing") ?: "",
            room = document.getString("room") ?: "",
            description = document.getString("description") ?: "",
            status = document.getString("status") ?: "ACTIVE"
        )
    }
}
