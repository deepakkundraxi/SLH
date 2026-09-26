package com.slh.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ManualAttendanceStatus {
    PRESENT,
    ABSENT
}

data class ManualAttendanceRecord(
    val id: String,
    val studentId: String,
    val coachingId: String,
    val date: String,
    val status: ManualAttendanceStatus
)

object AttendanceStatusStore {

    private var recordsState by mutableStateOf(
        emptyList<ManualAttendanceRecord>()
    )

    private var initialized = false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /*
     * ========================================================
     * FIREBASE WRITE-THROUGH
     * ========================================================
     */

    private const val KIND_MANUAL_ATTENDANCE = "manualAttendance"

    private fun syncManualToFirebase(
        record: ManualAttendanceRecord
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_MANUAL_ATTENDANCE, record.id)

        cloudScope.launch {

            try {

                AttendanceFirestoreRepository
                    .saveManual(record)

                SLHFirebase.clearPendingUpsert(
                    KIND_MANUAL_ATTENDANCE,
                    record.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveManual ${record.id}",
                    e
                )
            }
        }
    }


    private fun deleteManualFromFirebase(
        recordId: String
    ) {

        SLHFirebase.addPendingDelete(KIND_MANUAL_ATTENDANCE, recordId)

        cloudScope.launch {

            try {

                AttendanceFirestoreRepository
                    .deleteManual(recordId)

                SLHFirebase.clearPendingDelete(
                    KIND_MANUAL_ATTENDANCE,
                    recordId
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteManual $recordId",
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

        for (id in SLHFirebase.pendingDeletes(KIND_MANUAL_ATTENDANCE)) {

            try {

                AttendanceFirestoreRepository.deleteManual(id)

                SLHFirebase.clearPendingDelete(KIND_MANUAL_ATTENDANCE, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteManual $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_MANUAL_ATTENDANCE)) {

            val record =
                recordsState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                AttendanceFirestoreRepository.saveManual(record)

                SLHFirebase.clearPendingUpsert(KIND_MANUAL_ATTENDANCE, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveManual $id", e)
            }
        }
    }


    /*
     * ========================================================
     * FIREBASE -> LOCAL SYNC
     * ========================================================
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

                var cloudRecords =
                    AttendanceFirestoreRepository
                        .findManualByCoaching(
                            coachingId
                        )

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "manualAttendance",
                        coachingId
                    )
                ) {

                    val cloudIds =
                        cloudRecords
                            .map { it.id }
                            .toSet()

                    val missing =
                        recordsState.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudIds
                        }

                    missing.forEach {
                        AttendanceFirestoreRepository
                            .saveManual(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "manualAttendance",
                        coachingId
                    )

                    cloudRecords =
                        cloudRecords + missing
                }

                // Keep local records that could not be uploaded yet,
                // and do not resurrect ones whose delete is pending.
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_MANUAL_ATTENDANCE)

                val pendingDeletedIds =
                    SLHFirebase.pendingDeletes(KIND_MANUAL_ATTENDANCE)

                val keep =
                    recordsState.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingIds
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                val filteredCloud =
                    cloudRecords.filter {
                        it.id !in keepIds &&
                                it.id !in pendingDeletedIds
                    }

                recordsState =
                    recordsState.filter {
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


    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        val saved =
            SLHLocalStorage.loadAttendanceRecords()

        recordsState =
            saved ?: emptyList()
    }

    private fun persist() {

        SLHLocalStorage.saveAttendanceRecords(
            recordsState
        )
    }

    val records: List<ManualAttendanceRecord>
        get() = recordsState

    private fun dateFormat(): SimpleDateFormat =
        SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        )

    fun todayDate(): String =
        dateFormat().format(Date())

    fun isAbsent(
        studentId: String,
        date: String
    ): Boolean {

        return recordsState.any {
            it.studentId == studentId &&
                    it.date == date &&
                    it.status ==
                    ManualAttendanceStatus.ABSENT
        }
    }

    fun getRecord(
        studentId: String,
        date: String
    ): ManualAttendanceRecord? {

        return recordsState.firstOrNull {
            it.studentId == studentId &&
                    it.date == date
        }
    }

    fun markAbsent(
        studentId: String,
        coachingId: String,
        date: String
    ): Boolean {

        /*
         * GPS Present attendance has priority.
         */
        val gpsPresent =
            StudentGpsAttendanceStore.records.any {

                it.studentId == studentId &&
                        it.coachingId == coachingId &&
                        it.date == date
            }

        if (gpsPresent) {
            return false
        }

        /*
         * Already marked manually.
         */
        val existing =
            recordsState.firstOrNull {

                it.studentId == studentId &&
                        it.date == date
            }

        if (existing != null) {
            return false
        }

        val record =
            ManualAttendanceRecord(

                id =
                    "${studentId}_${coachingId}_${date}",

                studentId =
                    studentId,

                coachingId =
                    coachingId,

                date =
                    date,

                status =
                    ManualAttendanceStatus.ABSENT
            )

        recordsState =
            recordsState + record

        persist()

        syncManualToFirebase(record)

        return true
    }

    fun removeAbsent(
        studentId: String,
        date: String
    ) {

        val toRemove =
            recordsState.filter {

                it.studentId == studentId &&
                        it.date == date
            }

        recordsState =
            recordsState.filterNot {

                it.studentId == studentId &&
                        it.date == date
            }

        persist()

        toRemove.forEach {
            deleteManualFromFirebase(it.id)
        }
    }

    fun getStudentAbsentDays(
        studentId: String
    ): Int {

        return recordsState.count {

            it.studentId == studentId &&
                    it.status ==
                    ManualAttendanceStatus.ABSENT
        }
    }

    fun getStudentWorkingDays(
        studentId: String
    ): Int {

        val presentDates =
            StudentGpsAttendanceStore.records
                .filter {
                    it.studentId == studentId
                }
                .map {
                    it.date
                }
                .toSet()

        val absentDates =
            recordsState
                .filter {

                    it.studentId == studentId &&
                            it.status ==
                            ManualAttendanceStatus.ABSENT
                }
                .map {
                    it.date
                }
                .toSet()

        return (
                presentDates + absentDates
                ).size
    }

    fun getStudentAttendancePercentage(
        studentId: String
    ): Int {

        val workingDays =
            getStudentWorkingDays(
                studentId
            )

        if (workingDays <= 0) {
            return 0
        }

        val presentDays =
            StudentGpsAttendanceStore
                .getStudentPresentDays(
                    studentId
                )

        return (
                presentDays
                    .toFloat()
                    .div(workingDays)
                    .times(100f)
                    .toInt()
                ).coerceIn(
                0,
                100
            )
    }

    fun getTodayAbsentCount(
        coachingId: String
    ): Int {

        val today =
            todayDate()

        return recordsState.count {

            it.coachingId == coachingId &&
                    it.date == today &&
                    it.status ==
                    ManualAttendanceStatus.ABSENT
        }
    }

    fun getAbsentStudentIds(
        coachingId: String,
        date: String
    ): Set<String> {

        return recordsState
            .filter {

                it.coachingId == coachingId &&
                        it.date == date &&
                        it.status ==
                        ManualAttendanceStatus.ABSENT
            }
            .map {
                it.studentId
            }
            .toSet()
    }
}
