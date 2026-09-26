package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * NOTICE FIRESTORE REPOSITORY
 * ============================================================
 *
 * Collection:
 *
 *   notices/{notice.id}
 *
 * Every document carries "coachingId" so one coaching only
 * ever reads its own notices.
 *
 * Soft-delete is used (status = DELETED). Documents are not
 * hard-deleted so that read history stays intact.
 *
 * ============================================================
 */
object NoticeFirestoreRepository {

    private const val COLLECTION_NOTICES = "notices"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    private fun noticeToMap(
        notice: CoachingNotice
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to notice.id,
            "coachingId" to notice.coachingId,
            "batchId" to notice.batchId,
            "title" to notice.title,
            "message" to notice.message,
            "noticeDate" to notice.noticeDate,
            "expiryDate" to notice.expiryDate,
            "createdById" to notice.createdById,
            "createdByName" to notice.createdByName,
            "status" to notice.status.name,
            "readByUserIds" to notice.readByUserIds
        )
    }


    /**
     * Create or overwrite a notice document.
     */
    suspend fun saveNotice(
        notice: CoachingNotice
    ) {

        db.collection(COLLECTION_NOTICES)
            .document(notice.id)
            .set(noticeToMap(notice))
            .await()
    }


    /**
     * Soft-delete is preferred (status = DELETED via saveNotice).
     * Hard delete is only for permanent cleanup.
     */
    suspend fun deleteNotice(
        noticeId: String
    ) {

        db.collection(COLLECTION_NOTICES)
            .document(noticeId)
            .delete()
            .await()
    }


    /**
     * Atomically append one userId to readByUserIds.
     * Safe when multiple students mark the same notice as read.
     */
    suspend fun markRead(
        noticeId: String,
        userId: String
    ) {

        if (userId.isBlank()) return

        db.collection(COLLECTION_NOTICES)
            .document(noticeId)
            .update(
                "readByUserIds",
                FieldValue.arrayUnion(userId)
            )
            .await()
    }


    /**
     * Atomically remove one userId from readByUserIds.
     */
    suspend fun markUnread(
        noticeId: String,
        userId: String
    ) {

        if (userId.isBlank()) return

        db.collection(COLLECTION_NOTICES)
            .document(noticeId)
            .update(
                "readByUserIds",
                FieldValue.arrayRemove(userId)
            )
            .await()
    }


    /**
     * All notices of one coaching (including DELETED so the
     * soft-delete state can be restored on other devices).
     *
     * Source.SERVER is used on purpose: when the phone is offline
     * this throws instead of returning an empty/stale cache, so a
     * sync can never wipe good local data by mistake.
     */
    suspend fun findNoticesByCoaching(
        coachingId: String
    ): List<CoachingNotice> {

        val snapshot =
            db.collection(COLLECTION_NOTICES)
                .whereEqualTo(
                    "coachingId",
                    coachingId
                )
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToNotice(it)
        }
    }


    private fun documentToNotice(
        document: DocumentSnapshot
    ): CoachingNotice? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        val statusName =
            document.getString("status")
                ?: NoticeStatus.ACTIVE.name

        val status =
            try {
                NoticeStatus.valueOf(statusName)
            } catch (_: Exception) {
                NoticeStatus.ACTIVE
            }

        @Suppress("UNCHECKED_CAST")
        val readBy =
            (document.get("readByUserIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

        return CoachingNotice(
            id = document.getString("id") ?: document.id,
            coachingId = coachingId,
            batchId = document.getString("batchId") ?: "",
            title = document.getString("title") ?: "",
            message = document.getString("message") ?: "",
            noticeDate = document.getString("noticeDate") ?: "",
            expiryDate = document.getString("expiryDate") ?: "",
            createdById = document.getString("createdById") ?: "",
            createdByName = document.getString("createdByName") ?: "",
            status = status,
            readByUserIds = readBy
        )
    }
}
