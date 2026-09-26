package com.slh.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class NoticeStatus {
    ACTIVE,
    EXPIRED,
    DELETED
}

data class CoachingNotice(
    val id: String,
    val coachingId: String,

    // Empty batchId = notice for entire coaching
    val batchId: String = "",

    val title: String,
    val message: String,

    val noticeDate: String = "",
    val expiryDate: String = "",

    val createdById: String = "",
    val createdByName: String = "",

    val status: NoticeStatus = NoticeStatus.ACTIVE,

    /*
     * Users who have opened/read this notice.
     *
     * Every Student and Teacher has an independent
     * read status.
     */
    val readByUserIds: List<String> = emptyList()
)

object NoticeStore {

    private var noticesState by mutableStateOf(
        emptyList<CoachingNotice>()
    )

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
     * Firestore's own offline queue delivers it once the phone
     * is back online.
     */

    private const val KIND_NOTICES = "notices"

    private fun syncNoticeToFirebase(
        notice: CoachingNotice
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_NOTICES, notice.id)

        cloudScope.launch {

            try {

                NoticeFirestoreRepository
                    .saveNotice(notice)

                SLHFirebase.clearPendingUpsert(
                    KIND_NOTICES,
                    notice.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveNotice ${notice.id}",
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

        for (id in SLHFirebase.pendingUpserts(KIND_NOTICES)) {

            val notice =
                noticesState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                NoticeFirestoreRepository.saveNotice(notice)

                SLHFirebase.clearPendingUpsert(KIND_NOTICES, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveNotice $id", e)
            }
        }
    }


    private fun markReadOnFirebase(
        noticeId: String,
        userId: String
    ) {

        cloudScope.launch {

            try {

                NoticeFirestoreRepository
                    .markRead(noticeId, userId)

            } catch (e: Exception) {

                // Local data stays valid; never crash the UI.
                SLHFirebase.logSyncFailure(
                    "markRead $noticeId/$userId",
                    e
                )
            }
        }
    }


    private fun markUnreadOnFirebase(
        noticeId: String,
        userId: String
    ) {

        cloudScope.launch {

            try {

                NoticeFirestoreRepository
                    .markUnread(noticeId, userId)

            } catch (e: Exception) {

                // Local data stays valid; never crash the UI.
                SLHFirebase.logSyncFailure(
                    "markUnread $noticeId/$userId",
                    e
                )
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
     * - The cloud copy is the source of truth for this coaching
     *   (other coachings' local data is never touched).
     * - canWrite (coaching admin phones): the first time, local
     *   notices that are missing in the cloud are uploaded once,
     *   so nothing created before the migration is lost.
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

                var cloudNotices =
                    NoticeFirestoreRepository
                        .findNoticesByCoaching(
                            coachingId
                        )

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "notices",
                        coachingId
                    )
                ) {

                    val cloudIds =
                        cloudNotices
                            .map { it.id }
                            .toSet()

                    val missing =
                        noticesState.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudIds
                        }

                    missing.forEach {
                        NoticeFirestoreRepository
                            .saveNotice(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "notices",
                        coachingId
                    )

                    cloudNotices =
                        cloudNotices + missing
                }

                // Keep local notices that could not be uploaded yet.
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_NOTICES)

                val keep =
                    noticesState.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingIds
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                val filteredCloud =
                    cloudNotices.filter {
                        it.id !in keepIds
                    }

                noticesState =
                    noticesState.filter {
                        it.coachingId != coachingId
                    } + filteredCloud + keep

                persist()

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
    // INITIALIZE
    // =====================================================

    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        val saved =
            SLHLocalStorage.loadNotices()

        noticesState =
            saved ?: emptyList()
    }

    private fun persist() {

        SLHLocalStorage.saveNotices(
            noticesState
        )
    }

    private val notices: List<CoachingNotice>
        get() = noticesState

    /*
     * ---------------------------------------------------------
     * Basic Find
     * ---------------------------------------------------------
     */

    fun findNotice(
        noticeId: String
    ): CoachingNotice? {

        return notices.find {
            it.id == noticeId
        }
    }

    fun findNoticesByCoaching(
        coachingId: String
    ): List<CoachingNotice> {

        return notices
            .filter {
                it.coachingId == coachingId &&
                        it.status != NoticeStatus.DELETED
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    fun findNoticesByBatch(
        coachingId: String,
        batchId: String
    ): List<CoachingNotice> {

        return notices
            .filter {
                it.coachingId == coachingId &&
                        (
                                it.batchId.isBlank() ||
                                        it.batchId == batchId
                                ) &&
                        it.status != NoticeStatus.DELETED
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    fun findActiveNoticesByCoaching(
        coachingId: String
    ): List<CoachingNotice> {

        return notices
            .filter {
                it.coachingId == coachingId &&
                        it.status == NoticeStatus.ACTIVE
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    fun findActiveNoticesByBatch(
        coachingId: String,
        batchId: String
    ): List<CoachingNotice> {

        return notices
            .filter {
                it.coachingId == coachingId &&
                        (
                                it.batchId.isBlank() ||
                                        it.batchId == batchId
                                ) &&
                        it.status == NoticeStatus.ACTIVE
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    /*
     * ---------------------------------------------------------
     * Add
     * ---------------------------------------------------------
     */

    fun addNotice(
        notice: CoachingNotice
    ): CoachingNotice {

        noticesState =
            noticesState + notice

        persist()

        syncNoticeToFirebase(notice)

        return notice
    }

    /*
     * ---------------------------------------------------------
     * Create
     * ---------------------------------------------------------
     */

    fun createNotice(
        coachingId: String,
        batchId: String = "",
        title: String,
        message: String,
        noticeDate: String = "",
        expiryDate: String = "",
        createdById: String = "",
        createdByName: String = ""
    ): CoachingNotice {

        val notice =
            CoachingNotice(
                id =
                    "NOTICE_${System.currentTimeMillis()}",
                coachingId =
                    coachingId,
                batchId =
                    batchId,
                title =
                    title,
                message =
                    message,
                noticeDate =
                    noticeDate,
                expiryDate =
                    expiryDate,
                createdById =
                    createdById,
                createdByName =
                    createdByName,
                status =
                    NoticeStatus.ACTIVE,
                readByUserIds =
                    emptyList()
            )

        noticesState =
            noticesState + notice

        persist()

        syncNoticeToFirebase(notice)

        return notice
    }

    /*
     * ---------------------------------------------------------
     * Update
     * ---------------------------------------------------------
     */

    fun updateNotice(
        notice: CoachingNotice
    ): Boolean {

        val exists =
            noticesState.any {
                it.id == notice.id
            }

        if (!exists) {
            return false
        }

        noticesState =
            noticesState.map {
                if (it.id == notice.id) {
                    notice
                } else {
                    it
                }
            }

        persist()

        syncNoticeToFirebase(notice)

        return true
    }

    /*
     * ---------------------------------------------------------
     * Delete (soft)
     * ---------------------------------------------------------
     */

    fun deleteNotice(
        noticeId: String
    ): Boolean {

        val exists =
            noticesState.any {
                it.id == noticeId
            }

        if (!exists) {
            return false
        }

        var updated: CoachingNotice? = null

        noticesState =
            noticesState.map {
                if (it.id == noticeId) {
                    it.copy(
                        status =
                            NoticeStatus.DELETED
                    ).also { n -> updated = n }
                } else {
                    it
                }
            }

        persist()

        updated?.let { syncNoticeToFirebase(it) }

        return true
    }

    /*
     * ---------------------------------------------------------
     * Status
     * ---------------------------------------------------------
     */

    fun setNoticeStatus(
        noticeId: String,
        status: NoticeStatus
    ): Boolean {

        val exists =
            noticesState.any {
                it.id == noticeId
            }

        if (!exists) {
            return false
        }

        var updated: CoachingNotice? = null

        noticesState =
            noticesState.map {
                if (it.id == noticeId) {
                    it.copy(status = status)
                        .also { n -> updated = n }
                } else {
                    it
                }
            }

        persist()

        updated?.let { syncNoticeToFirebase(it) }

        return true
    }

    /*
     * ---------------------------------------------------------
     * READ / UNREAD
     * ---------------------------------------------------------
     */

    /**
     * Returns true when this particular user
     * has already opened the notice.
     */
    fun hasUserReadNotice(
        noticeId: String,
        userId: String
    ): Boolean {

        val notice =
            findNotice(noticeId)
                ?: return false

        return userId in notice.readByUserIds
    }

    /**
     * Mark notice as read for one specific user.
     *
     * Other students/teachers remain unread
     * until they open the notice themselves.
     *
     * Cloud update uses arrayUnion so concurrent
     * readers do not overwrite each other.
     */
    fun markNoticeAsRead(
        noticeId: String,
        userId: String
    ): Boolean {

        if (userId.isBlank()) {
            return false
        }

        val notice =
            noticesState.firstOrNull {
                it.id == noticeId
            }
                ?: return false

        if (
            userId in
            notice.readByUserIds
        ) {
            return true
        }

        noticesState =
            noticesState.map {
                if (it.id == noticeId) {
                    it.copy(
                        readByUserIds =
                            it.readByUserIds + userId
                    )
                } else {
                    it
                }
            }

        persist()

        markReadOnFirebase(noticeId, userId)

        return true
    }

    /**
     * Mark notice unread for a specific user.
     *
     * Mainly useful if Admin later wants
     * to reset a user's read state.
     */
    fun markNoticeAsUnread(
        noticeId: String,
        userId: String
    ): Boolean {

        val notice =
            noticesState.firstOrNull {
                it.id == noticeId
            }
                ?: return false

        if (
            userId !in
            notice.readByUserIds
        ) {
            return true
        }

        noticesState =
            noticesState.map {
                if (it.id == noticeId) {
                    it.copy(
                        readByUserIds =
                            it.readByUserIds.filter { uid ->
                                uid != userId
                            }
                    )
                } else {
                    it
                }
            }

        persist()

        markUnreadOnFirebase(noticeId, userId)

        return true
    }

    /**
     * Returns notices which are still unread
     * for a particular user.
     */
    fun findUnreadNoticesForUser(
        coachingId: String,
        userId: String
    ): List<CoachingNotice> {

        return notices
            .filter {
                it.coachingId == coachingId &&
                        it.status != NoticeStatus.DELETED &&
                        userId !in it.readByUserIds
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    fun findUnreadActiveNoticesForUser(
        coachingId: String,
        userId: String
    ): List<CoachingNotice> {

        return notices
            .filter {
                it.coachingId == coachingId &&
                        it.status == NoticeStatus.ACTIVE &&
                        userId !in it.readByUserIds
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    /**
     * Unread active notices for a user.
     */
    fun getUnreadNoticeCount(
        coachingId: String,
        userId: String
    ): Int {

        return findUnreadNoticesForUser(
            coachingId = coachingId,
            userId = userId
        ).size
    }

    /*
     * ---------------------------------------------------------
     * Search
     * ---------------------------------------------------------
     */

    fun searchNotices(
        coachingId: String,
        query: String
    ): List<CoachingNotice> {

        val q =
            query.trim()

        if (q.isBlank()) {

            return findNoticesByCoaching(
                coachingId
            )
        }

        return notices
            .filter {

                it.coachingId == coachingId &&

                        it.status !=
                        NoticeStatus.DELETED &&

                        (
                                it.title.contains(
                                    q,
                                    ignoreCase = true
                                ) ||

                                        it.message.contains(
                                            q,
                                            ignoreCase = true
                                        ) ||

                                        it.createdByName.contains(
                                            q,
                                            ignoreCase = true
                                        )
                                )
            }
            .sortedByDescending {
                it.noticeDate
            }
    }

    /*
     * ---------------------------------------------------------
     * Counts
     * ---------------------------------------------------------
     */

    fun getNoticeCount(
        coachingId: String
    ): Int {

        return notices.count {

            it.coachingId == coachingId &&

                    it.status !=
                    NoticeStatus.DELETED
        }
    }

    fun getActiveNoticeCount(
        coachingId: String
    ): Int {

        return notices.count {

            it.coachingId == coachingId &&

                    it.status ==
                    NoticeStatus.ACTIVE
        }
    }

    /*
     * ---------------------------------------------------------
     * Clear
     * ---------------------------------------------------------
     */

    fun clear() {
        noticesState = emptyList()
        persist()
    }
}
