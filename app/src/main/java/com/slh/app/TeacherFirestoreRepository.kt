package com.slh.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * TEACHER FIRESTORE REPOSITORY
 * ============================================================
 *
 * Two collections are written together:
 *
 * teachers/{id}          full record (mobile, email, address, ...)
 *                        readable only by admins and the teacher
 *                        themself. The password (hash) is NEVER
 *                        stored in Firestore.
 *
 * teacherDirectory/{id}  name + subject only. Readable by every
 *                        member of the coaching, including
 *                        students, so that a batch can show its
 *                        teacher without exposing contact details.
 *
 * ============================================================
 */
object TeacherFirestoreRepository {

    private const val COLLECTION_TEACHERS = "teachers"

    private const val COLLECTION_DIRECTORY = "teacherDirectory"

    private val db: FirebaseFirestore
        get() = SLHFirebase.db


    private fun authEmailFor(
        teacher: Teacher
    ): String {

        if (teacher.username.isBlank()) {
            return ""
        }

        return try {

            FirebaseRoleAuthRepository
                .usernameToFirebaseEmail(
                    teacher.username
                )

        } catch (_: Exception) {

            ""
        }
    }


    /**
     * Create or overwrite both documents in one atomic batch.
     */
    suspend fun saveTeacher(
        teacher: Teacher
    ) {

        val full =
            hashMapOf<String, Any?>(
                "id" to teacher.id,
                "coachingId" to teacher.coachingId,
                "name" to teacher.name,
                "mobile" to teacher.mobile,
                "email" to teacher.email,
                "address" to teacher.address,
                "subject" to teacher.subject,
                "qualification" to teacher.qualification,
                "username" to teacher.username,
                "joiningDate" to teacher.joiningDate,
                "accountStatus" to teacher.accountStatus,
                "authEmail" to authEmailFor(teacher)
            )

        val directory =
            hashMapOf<String, Any?>(
                "id" to teacher.id,
                "coachingId" to teacher.coachingId,
                "name" to teacher.name,
                "subject" to teacher.subject,
                "qualification" to teacher.qualification,
                "accountStatus" to teacher.accountStatus
            )

        val batch =
            db.batch()

        batch.set(
            db.collection(COLLECTION_TEACHERS)
                .document(teacher.id),
            full
        )

        batch.set(
            db.collection(COLLECTION_DIRECTORY)
                .document(teacher.id),
            directory
        )

        batch.commit().await()
    }


    suspend fun deleteTeacher(
        teacherId: String
    ) {

        val batch =
            db.batch()

        batch.delete(
            db.collection(COLLECTION_TEACHERS)
                .document(teacherId)
        )

        batch.delete(
            db.collection(COLLECTION_DIRECTORY)
                .document(teacherId)
        )

        batch.commit().await()
    }


    /**
     * Full records of one coaching (admins only).
     * Source.SERVER: fails when offline instead of returning a
     * stale/empty cache.
     */
    suspend fun findTeachersByCoaching(
        coachingId: String
    ): List<Teacher> {

        return db.collection(COLLECTION_TEACHERS)
            .whereEqualTo("coachingId", coachingId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToTeacher(it) }
    }


    /**
     * Name + subject of every teacher of one coaching.
     * Contact fields come back empty.
     */
    suspend fun findDirectoryByCoaching(
        coachingId: String
    ): List<Teacher> {

        return db.collection(COLLECTION_DIRECTORY)
            .whereEqualTo("coachingId", coachingId)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { documentToTeacher(it) }
    }


    /**
     * The signed-in teacher's own full record.
     */
    suspend fun findOwnTeacher(
        username: String
    ): Teacher? {

        if (username.isBlank()) {
            return null
        }

        val authEmail =
            FirebaseRoleAuthRepository
                .usernameToFirebaseEmail(
                    username
                )

        return db.collection(COLLECTION_TEACHERS)
            .whereEqualTo("authEmail", authEmail)
            .limit(1)
            .get(Source.SERVER)
            .await()
            .documents
            .firstNotNullOfOrNull {
                documentToTeacher(it)
            }
    }


    private fun documentToTeacher(
        document: DocumentSnapshot
    ): Teacher? {

        val coachingId =
            document.getString("coachingId")
                ?: return null

        return Teacher(
            id = document.getString("id") ?: document.id,
            coachingId = coachingId,
            name = document.getString("name") ?: "",
            mobile = document.getString("mobile") ?: "",
            email = document.getString("email") ?: "",
            address = document.getString("address") ?: "",
            subject = document.getString("subject") ?: "",
            qualification =
                document.getString("qualification") ?: "",
            username = document.getString("username") ?: "",
            password = "",
            joiningDate =
                document.getString("joiningDate") ?: "",
            accountStatus =
                document.getString("accountStatus") ?: "ACTIVE"
        )
    }
}
