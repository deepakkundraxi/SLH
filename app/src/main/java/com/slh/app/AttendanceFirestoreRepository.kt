package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * ATTENDANCE FIRESTORE REPOSITORY
 * ============================================================
 *
 * Collections:
 *
 *   manualAttendance/{record.id}   ABSENT marks by staff
 *   gpsAttendance/{record.id}      PRESENT marks by student GPS
 *
 * Document ids are deterministic:
 *   manual: "${studentId}_${coachingId}_${date}"
 *   gps:    "${studentId}_${date}"
 *
 * ============================================================
 */
object AttendanceFirestoreRepository {

    private const val MANUAL = "manualAttendance"
    private const val GPS = "gpsAttendance"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    // ---------------- manual (ABSENT) ----------------

    private fun manualToMap(
        record: ManualAttendanceRecord
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to record.id,
            "studentId" to record.studentId,
            "coachingId" to record.coachingId,
            "date" to record.date,
            "status" to record.status.name
        )
    }


    suspend fun saveManual(
        record: ManualAttendanceRecord
    ) {

        db.collection(MANUAL)
            .document(record.id)
            .set(manualToMap(record))
            .await()
    }


    suspend fun deleteManual(
        recordId: String
    ) {

        db.collection(MANUAL)
            .document(recordId)
            .delete()
            .await()
    }


    suspend fun findManualByCoaching(
        coachingId: String
    ): List<ManualAttendanceRecord> {

        val snapshot =
            db.collection(MANUAL)
                .whereEqualTo("coachingId", coachingId)
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToManual(it)
        }
    }


    private fun documentToManual(
        document: DocumentSnapshot
    ): ManualAttendanceRecord? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        val studentId =
            document.getString("studentId")
                ?: return null

        val statusName =
            document.getString("status")
                ?: ManualAttendanceStatus.ABSENT.name

        val status =
            try {
                ManualAttendanceStatus.valueOf(statusName)
            } catch (_: Exception) {
                ManualAttendanceStatus.ABSENT
            }

        return ManualAttendanceRecord(
            id = document.getString("id") ?: document.id,
            studentId = studentId,
            coachingId = coachingId,
            date = document.getString("date") ?: "",
            status = status
        )
    }


    // ---------------- gps (PRESENT) ----------------

    private fun gpsToMap(
        record: GpsAttendanceRecord
    ): Map<String, Any?> {

        return hashMapOf(
            "id" to record.id,
            "studentId" to record.studentId,
            "coachingId" to record.coachingId,
            "date" to record.date,
            "time" to record.time,
            "latitude" to record.latitude,
            "longitude" to record.longitude,
            "distanceMeters" to record.distanceMeters.toDouble()
        )
    }


    suspend fun saveGps(
        record: GpsAttendanceRecord
    ) {

        db.collection(GPS)
            .document(record.id)
            .set(gpsToMap(record))
            .await()
    }


    suspend fun deleteGps(
        recordId: String
    ) {

        db.collection(GPS)
            .document(recordId)
            .delete()
            .await()
    }


    suspend fun findGpsByCoaching(
        coachingId: String
    ): List<GpsAttendanceRecord> {

        val snapshot =
            db.collection(GPS)
                .whereEqualTo("coachingId", coachingId)
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToGps(it)
        }
    }


    private fun documentToGps(
        document: DocumentSnapshot
    ): GpsAttendanceRecord? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        val studentId =
            document.getString("studentId")
                ?: return null

        val distance =
            when (val raw = document.get("distanceMeters")) {
                is Double -> raw.toFloat()
                is Long -> raw.toFloat()
                is Int -> raw.toFloat()
                is Float -> raw
                else -> 0f
            }

        return GpsAttendanceRecord(
            id = document.getString("id") ?: document.id,
            studentId = studentId,
            coachingId = coachingId,
            date = document.getString("date") ?: "",
            time = document.getString("time") ?: "",
            latitude = document.getDouble("latitude") ?: 0.0,
            longitude = document.getDouble("longitude") ?: 0.0,
            distanceMeters = distance
        )
    }
}
