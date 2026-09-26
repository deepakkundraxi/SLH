package com.slh.app

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * STUDENT FIRESTORE REPOSITORY
 * ============================================================
 *
 * Firebase Firestore layer for Student records.
 *
 * IMPORTANT:
 * - Password is NEVER stored in Firestore.
 * - Existing local StudentStore remains available.
 * - This repository only handles cloud student data.
 *
 * Collection:
 *
 * students/{student.id}
 *
 * ============================================================
 */
object StudentFirestoreRepository {

    private const val COLLECTION_STUDENTS = "students"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    /*
     * ========================================================
     * CONVERT STUDENT -> FIRESTORE MAP
     * ========================================================
     */
    private fun studentToMap(
        student: Student
    ): Map<String, Any?> {

        return hashMapOf(

            "id" to student.id,

            "studentId" to student.studentId,

            "name" to student.name,

            "mobile" to student.mobile,

            "address" to student.address,

            "username" to student.username,

            /*
             * NEVER store password in Firestore.
             */
            "course" to student.course,

            "admissionDate" to student.admissionDate,

            "status" to student.status.name,

            "coachingId" to student.coachingId,

            "batchId" to student.batchId,

            "photoUri" to student.photoUri,

            /*
             * Firebase Auth email of this student. Security rules
             * compare it with the signed-in student's email.
             */
            "authEmail" to authEmailFor(student)
        )
    }


    private fun authEmailFor(
        student: Student
    ): String {

        return try {

            FirebaseStudentAuthRepository
                .usernameToFirebaseEmail(
                    student.username
                )

        } catch (_: Exception) {

            ""
        }
    }


    /**
     * Adds ONLY the "authEmail" field to existing student
     * documents. Documents that no longer exist are skipped.
     * Any other error (e.g. permission denied) is thrown so the
     * caller can retry later.
     */
    suspend fun backfillAuthEmail(
        students: List<Student>
    ) {

        for (student in students) {

            val email =
                authEmailFor(student)

            if (email.isBlank()) {
                continue
            }

            try {

                db.collection(COLLECTION_STUDENTS)
                    .document(student.id)
                    .update("authEmail", email)
                    .await()

            } catch (exception: FirebaseFirestoreException) {

                if (
                    exception.code !=
                    FirebaseFirestoreException.Code.NOT_FOUND
                ) {
                    throw exception
                }
            }
        }
    }


    /*
     * ========================================================
     * ADD / CREATE STUDENT
     * ========================================================
     */
    suspend fun addStudent(
        student: Student
    ) {

        db.collection(COLLECTION_STUDENTS)
            .document(student.id)
            .set(studentToMap(student))
            .await()
    }


    /*
     * ========================================================
     * UPDATE STUDENT
     * ========================================================
     */
    suspend fun updateStudent(
        student: Student
    ) {

        db.collection(COLLECTION_STUDENTS)
            .document(student.id)
            .set(studentToMap(student))
            .await()
    }


    /*
     * ========================================================
     * DELETE STUDENT
     * ========================================================
     */
    suspend fun deleteStudent(
        studentId: String
    ) {

        db.collection(COLLECTION_STUDENTS)
            .document(studentId)
            .delete()
            .await()
    }


    /*
     * ========================================================
     * GET STUDENT
     * ========================================================
     *
     * Password cannot be reconstructed from Firestore.
     *
     * Therefore this method returns a Student with an empty
     * password field. Authentication will be handled by
     * Firebase Authentication in the next migration step.
     *
     */
    suspend fun findStudent(
        studentId: String
    ): Student? {

        val snapshot =
            db.collection(COLLECTION_STUDENTS)
                .document(studentId)
                .get()
                .await()

        if (!snapshot.exists()) {
            return null
        }

        return documentToStudent(snapshot)
    }


    /*
     * ========================================================
     * GET ALL STUDENTS FOR COACHING
     * ========================================================
     */
    suspend fun findStudentsByCoaching(
        coachingId: String
    ): List<Student> {

        val snapshot =
            db.collection(COLLECTION_STUDENTS)
                .whereEqualTo(
                    "coachingId",
                    coachingId
                )
                .get(Source.SERVER)
                .await()

        return snapshot.documents.mapNotNull {
            documentToStudent(it)
        }
    }


    /**
     * The student whose Firebase account has this email.
     * Used by the cloud login on a phone that has no local copy.
     */
    suspend fun findStudentByAuthEmail(
        authEmail: String
    ): Student? {

        return db.collection(COLLECTION_STUDENTS)
            .whereEqualTo("authEmail", authEmail)
            .limit(1)
            .get(Source.SERVER)
            .await()
            .documents
            .firstNotNullOfOrNull {
                documentToStudent(it)
            }
    }


    /*
     * ========================================================
     * GET STUDENTS BY BATCH
     * ========================================================
     */
    suspend fun findStudentsByBatch(
        coachingId: String,
        batchId: String
    ): List<Student> {

        val snapshot =
            db.collection(COLLECTION_STUDENTS)
                .whereEqualTo(
                    "coachingId",
                    coachingId
                )
                .whereEqualTo(
                    "batchId",
                    batchId
                )
                .get()
                .await()

        return snapshot.documents.mapNotNull {
            documentToStudent(it)
        }
    }


    /*
     * ========================================================
     * FIND BY USERNAME
     * ========================================================
     */
    suspend fun findStudentByUsername(
        username: String
    ): Student? {

        val snapshot =
            db.collection(COLLECTION_STUDENTS)
                .whereEqualTo(
                    "username",
                    username.trim()
                )
                .limit(1)
                .get()
                .await()

        return snapshot.documents
            .firstOrNull()
            ?.let {
                documentToStudent(it)
            }
    }


    /*
     * ========================================================
     * CONVERT FIRESTORE DOCUMENT -> STUDENT
     * ========================================================
     */
    fun documentToStudent(
        document: com.google.firebase.firestore.DocumentSnapshot
    ): Student? {

        val id =
            document.getString("id")
                ?: document.id

        val studentId =
            document.getString("studentId")
                ?: return null

        val name =
            document.getString("name")
                ?: ""

        val mobile =
            document.getString("mobile")
                ?: ""

        val address =
            document.getString("address")
                ?: ""

        val username =
            document.getString("username")
                ?: ""

        val course =
            document.getString("course")
                ?: ""

        val admissionDate =
            document.getString("admissionDate")
                ?: ""

        val statusName =
            document.getString("status")
                ?: AccountStatus.PENDING.name

        val status =
            try {

                AccountStatus.valueOf(
                    statusName
                )

            } catch (_: Exception) {

                AccountStatus.PENDING
            }

        val coachingId =
            document.getString("coachingId")
                ?: "C001"

        val batchId =
            document.getString("batchId")
                ?: ""

        val photoUri =
            document.getString("photoUri")

        return Student(

            id = id,

            studentId = studentId,

            name = name,

            mobile = mobile,

            address = address,

            username = username,

            /*
             * Password intentionally empty.
             */
            password = "",

            course = course,

            admissionDate = admissionDate,

            status = status,

            coachingId = coachingId,

            batchId = batchId,

            photoUri = photoUri
        )
    }
}