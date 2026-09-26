package com.slh.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


/*
 * ============================================================
 * BATCH MODEL
 * ============================================================
 */

data class CoachingBatch(

    val id: String,

    val coachingId: String,

    val name: String,

    val code: String = "",

    val course: String = "",

    val subject: String = "",

    val teacherId: String = "",

    val startDate: String = "",

    val endDate: String = "",

    val timing: String = "",

    val room: String = "",

    val description: String = "",

    val status: String = "ACTIVE"
)


/*
 * ============================================================
 * BATCH STORE
 * ============================================================
 */

object BatchStore {

    private var batchesState by mutableStateOf(
        emptyList<CoachingBatch>()
    )

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

    private const val KIND_BATCHES = "batches"

    private fun syncBatchToFirebase(
        batch: CoachingBatch
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_BATCHES, batch.id)

        cloudScope.launch {

            try {

                BatchFirestoreRepository
                    .saveBatch(batch)

                SLHFirebase.clearPendingUpsert(
                    KIND_BATCHES,
                    batch.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveBatch ${batch.id}",
                    e
                )
            }
        }
    }

    private fun deleteBatchFromFirebase(
        batchId: String
    ) {

        SLHFirebase.addPendingDelete(KIND_BATCHES, batchId)

        cloudScope.launch {

            try {

                BatchFirestoreRepository
                    .deleteBatch(batchId)

                SLHFirebase.clearPendingDelete(
                    KIND_BATCHES,
                    batchId
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteBatch $batchId",
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

        for (id in SLHFirebase.pendingDeletes(KIND_BATCHES)) {

            try {

                BatchFirestoreRepository.deleteBatch(id)

                SLHFirebase.clearPendingDelete(KIND_BATCHES, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteBatch $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_BATCHES)) {

            val batch =
                batchesState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                BatchFirestoreRepository.saveBatch(batch)

                SLHFirebase.clearPendingUpsert(KIND_BATCHES, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveBatch $id", e)
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
     *   batches that are missing in the cloud are uploaded once,
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

                var cloudBatches =
                    BatchFirestoreRepository
                        .findBatchesByCoaching(
                            coachingId
                        )

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "batches",
                        coachingId
                    )
                ) {

                    val cloudIds =
                        cloudBatches
                            .map { it.id }
                            .toSet()

                    val missing =
                        batchesState.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudIds
                        }

                    missing.forEach {
                        BatchFirestoreRepository
                            .saveBatch(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "batches",
                        coachingId
                    )

                    cloudBatches =
                        cloudBatches + missing
                }

                // Keep local batches that could not be uploaded yet,
                // and do not resurrect ones whose delete is pending.
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_BATCHES)

                val pendingDeletedIds =
                    SLHFirebase.pendingDeletes(KIND_BATCHES)

                val keep =
                    batchesState.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingIds
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                val filteredCloud =
                    cloudBatches.filter {
                        it.id !in keepIds &&
                                it.id !in pendingDeletedIds
                    }

                batchesState =
                    batchesState.filter {
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


    /*
     * ========================================================
     * INITIALIZE
     * ========================================================
     */

    fun initialize() {

        val saved =
            SLHLocalStorage.loadBatches()

        if (saved != null) {

            batchesState =
                saved

        } else {

            batchesState =
                emptyList()

            SLHLocalStorage.saveBatches(
                batchesState
            )
        }
    }


    /*
     * ========================================================
     * PERSIST
     * ========================================================
     */

    private fun persist() {

        SLHLocalStorage.saveBatches(
            batchesState
        )
    }


    /*
     * ========================================================
     * READ-ONLY LIST
     * ========================================================
     */

    val batches: List<CoachingBatch>
        get() = batchesState


    /*
     * ========================================================
     * FIND BATCH
     * ========================================================
     */

    fun findBatch(
        batchId: String
    ): CoachingBatch? {

        return batchesState.firstOrNull {

            it.id == batchId
        }
    }


    /*
     * ========================================================
     * FIND ALL BATCHES OF COACHING
     * ========================================================
     */

    fun findBatchesByCoaching(
        coachingId: String
    ): List<CoachingBatch> {

        return batchesState.filter {

            it.coachingId == coachingId
        }
    }


    /*
     * ========================================================
     * ACTIVE BATCHES
     * ========================================================
     */

    fun findActiveBatchesByCoaching(
        coachingId: String
    ): List<CoachingBatch> {

        return batchesState.filter {

            it.coachingId == coachingId &&
                    it.status.equals(
                        "ACTIVE",
                        ignoreCase = true
                    )
        }
    }


    /*
     * ========================================================
     * ADD BATCH
     * ========================================================
     */

    fun addBatch(
        batch: CoachingBatch
    ): Boolean {

        if (
            batchesState.any {
                it.id == batch.id
            }
        ) {

            return false
        }


        if (
            batch.code.isNotBlank() &&
            batchesState.any {

                it.coachingId ==
                        batch.coachingId &&

                        it.code.equals(
                            batch.code,
                            ignoreCase = true
                        )
            }
        ) {

            return false
        }


        batchesState =
            batchesState + batch

        persist()

        syncBatchToFirebase(batch)

        return true
    }


    /*
     * ========================================================
     * UPDATE BATCH
     * ========================================================
     */

    fun updateBatch(
        batch: CoachingBatch
    ): Boolean {

        val exists =
            batchesState.any {

                it.id == batch.id
            }

        if (!exists) {

            return false
        }


        if (
            batch.code.isNotBlank() &&
            batchesState.any {

                it.id != batch.id &&
                        it.coachingId ==
                        batch.coachingId &&

                        it.code.equals(
                            batch.code,
                            ignoreCase = true
                        )
            }
        ) {

            return false
        }


        batchesState =
            batchesState.map {

                if (
                    it.id == batch.id
                ) {

                    batch

                } else {

                    it
                }
            }

        persist()

        syncBatchToFirebase(batch)

        return true
    }


    /*
     * ========================================================
     * DELETE BATCH
     * ========================================================
     */

    fun deleteBatch(
        batchId: String
    ): Boolean {

        // Capture coachingId before removing the batch.
        val coachingId =
            findBatch(batchId)?.coachingId

        val oldSize =
            batchesState.size

        batchesState =
            batchesState.filterNot {

                it.id == batchId
            }

        val deleted =
            batchesState.size < oldSize

        if (deleted) {
            // Unassign all students from this batch so they
            // are not left pointing at a deleted batch.
            if (coachingId != null) {
                StudentStore.findStudentsByBatch(
                    coachingId = coachingId,
                    batchId = batchId
                ).forEach { student ->
                    StudentStore.removeStudentFromBatch(student.id)
                }
            } else {
                // Fallback: match by batchId only
                StudentStore.students
                    .filter { it.batchId == batchId }
                    .forEach { student ->
                        StudentStore.removeStudentFromBatch(student.id)
                    }
            }

            persist()

            deleteBatchFromFirebase(batchId)
        }

        return deleted
    }


    /*
     * ========================================================
     * SEARCH
     * ========================================================
     */

    fun searchBatches(
        coachingId: String,
        query: String
    ): List<CoachingBatch> {

        val search =
            query.trim()

        return batchesState.filter { batch ->

            batch.coachingId == coachingId &&
                    (
                            search.isBlank() ||

                                    batch.name.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    batch.code.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    batch.course.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    batch.subject.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    batch.timing.contains(
                                        search,
                                        ignoreCase = true
                                    )
                            )
        }
    }


    /*
     * ========================================================
     * TOTAL BATCH COUNT
     * ========================================================
     */

    fun getBatchCount(
        coachingId: String
    ): Int {

        return batchesState.count {

            it.coachingId == coachingId
        }
    }


    /*
     * ========================================================
     * ACTIVE BATCH COUNT
     * ========================================================
     */

    fun getActiveBatchCount(
        coachingId: String
    ): Int {

        return batchesState.count {

            it.coachingId == coachingId &&
                    it.status.equals(
                        "ACTIVE",
                        ignoreCase = true
                    )
        }
    }


    /*
     * ========================================================
     * CHANGE STATUS
     * ========================================================
     */

    fun setBatchStatus(
        batchId: String,
        status: String
    ): Boolean {

        val batch =
            findBatch(
                batchId
            )
                ?: return false

        return updateBatch(

            batch.copy(
                status = status
            )
        )
    }


    /*
     * ========================================================
     * CLEAR
     * ========================================================
     */

    fun clear() {

        batchesState =
            emptyList()

        persist()
    }
}