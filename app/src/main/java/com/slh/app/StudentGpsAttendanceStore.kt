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

data class GpsAttendanceRecord(
    val id: String,
    val studentId: String,
    val coachingId: String,
    val date: String,
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float
)

object StudentGpsAttendanceStore {

    private var recordsState by mutableStateOf(
        emptyList<GpsAttendanceRecord>()
    )

    private var initialized = false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /*
     * ========================================================
     * FIREBASE WRITE-THROUGH
     * ========================================================
     */

    private const val KIND_GPS_ATTENDANCE = "gpsAttendance"

    private fun syncGpsToFirebase(
        record: GpsAttendanceRecord
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_GPS_ATTENDANCE, record.id)

        cloudScope.launch {

            try {

                AttendanceFirestoreRepository
                    .saveGps(record)

                SLHFirebase.clearPendingUpsert(
                    KIND_GPS_ATTENDANCE,
                    record.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveGps ${record.id}",
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

        for (id in SLHFirebase.pendingUpserts(KIND_GPS_ATTENDANCE)) {

            val record =
                recordsState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                AttendanceFirestoreRepository.saveGps(record)

                SLHFirebase.clearPendingUpsert(KIND_GPS_ATTENDANCE, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveGps $id", e)
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
                        .findGpsByCoaching(
                            coachingId
                        )

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "gpsAttendance",
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
                            .saveGps(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "gpsAttendance",
                        coachingId
                    )

                    cloudRecords =
                        cloudRecords + missing
                }

                // Keep local records that could not be uploaded yet.
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_GPS_ATTENDANCE)

                val keep =
                    recordsState.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingIds
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                val filteredCloud =
                    cloudRecords.filter {
                        it.id !in keepIds
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
            SLHLocalStorage.loadGpsAttendanceRecords()

        recordsState =
            saved ?: emptyList()
    }

    private fun persist() {

        SLHLocalStorage.saveGpsAttendanceRecords(
            recordsState
        )
    }

    val records: List<GpsAttendanceRecord>
        get() = recordsState

    fun getStudentAttendance(
        studentId: String
    ): List<GpsAttendanceRecord> =
        recordsState
            .filter {
                it.studentId == studentId
            }
            .sortedByDescending {
                "${it.date} ${it.time}"
            }

    fun getStudentPresentDays(
        studentId: String
    ): Int =
        recordsState
            .count {
                it.studentId == studentId
            }

    fun hasAttendanceToday(
        studentId: String
    ): Boolean {
        val today = todayDate()

        return recordsState.any {
            it.studentId == studentId &&
                    it.date == today
        }
    }

    fun getTodayPresentCount(
        coachingId: String
    ): Int {
        val today = todayDate()

        return recordsState
            .count {
                it.coachingId == coachingId &&
                        it.date == today
            }
    }

    fun getTodayPresentStudentIds(
        coachingId: String
    ): Set<String> {
        val today = todayDate()

        return recordsState
            .filter {
                it.coachingId == coachingId &&
                        it.date == today
            }
            .map {
                it.studentId
            }
            .toSet()
    }

    fun addAttendance(
        studentId: String,
        coachingId: String,
        latitude: Double,
        longitude: Double,
        distanceMeters: Float
    ): Boolean {

        val today = todayDate()

        if (
            recordsState.any {
                it.studentId == studentId &&
                        it.date == today
            }
        ) {
            return false
        }

        val record = GpsAttendanceRecord(
            id = "${studentId}_${today}",
            studentId = studentId,
            coachingId = coachingId,
            date = today,
            time = currentTime(),
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters
        )

        recordsState =
            recordsState + record

        persist()

        syncGpsToFirebase(record)

        return true
    }

    private fun todayDate(): String =
        SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        ).format(Date())

    private fun currentTime(): String =
        SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(Date())
}
