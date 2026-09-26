package com.slh.app

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * ============================================================
 * STUDENT MODEL
 * ============================================================
 */

data class Student(

    val id: String,

    val studentId: String,

    val name: String,

    val mobile: String,

    val address: String = "",

    val username: String,

    val password: String,

    val course: String = "",

    val admissionDate: String,

    val status: AccountStatus =
        AccountStatus.APPROVED,

    val coachingId: String = "C001",

    val batchId: String = "",

    val photoUri: String? = null
)


/*
 * ============================================================
 * STUDENT STORE
 * ============================================================
 */
object StudentStore {

    private val studentsState: MutableState<List<Student>> =
        mutableStateOf(emptyList())

    private val cloudScope =
        CoroutineScope(Dispatchers.IO)


    /*
     * ========================================================
     * INITIAL DATA
     * ========================================================
     */
    private fun defaultStudents(): List<Student> = emptyList()


    /*
     * ========================================================
     * INITIALIZE
     * ========================================================
     */
    fun initialize() {

        val saved = SLHLocalStorage.loadStudents()

        // No built-in/demo student is created in any build.
        // Remove the old seeded student left by earlier installations.
        val cleaned = (saved ?: emptyList()).filterNot {
            it.id == "ST001" &&
                    it.username.equals("student01", ignoreCase = true)
        }

        studentsState.value = cleaned

        if (saved == null || cleaned.size != saved.size) {
            SLHLocalStorage.saveStudents(cleaned)
        }
    }


    /*
     * ========================================================
     * READ-ONLY LIST
     * ========================================================
     */
    val students: List<Student>
        get() = studentsState.value


    /*
     * ========================================================
     * LOCAL PERSIST
     * ========================================================
     */
    private fun persist() {

        SLHLocalStorage.saveStudents(
            studentsState.value
        )
    }


    /*
     * ========================================================
     * ADD STUDENT
     * ========================================================
     *
     * Existing UI continues working immediately.
     *
     * Firebase sync happens in background.
     */
    fun addStudent(
        student: Student
    ) {

        studentsState.value =
            studentsState.value + student

        persist()

        syncStudentToFirebase(
            student
        )
    }


    /*
     * ========================================================
     * UPDATE STUDENT
     * ========================================================
     */
    fun updateStudent(
        student: Student
    ) {

        studentsState.value =
            studentsState.value.map {

                    existingStudent ->

                if (
                    existingStudent.id ==
                    student.id
                ) {
                    student
                } else {
                    existingStudent
                }
            }

        persist()

        syncStudentToFirebase(
            student
        )
    }


    /*
     * ========================================================
     * FIREBASE ADD/UPDATE
     * ========================================================
     */
    private const val KIND_STUDENTS = "students"

    private fun syncStudentToFirebase(
        student: Student
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_STUDENTS, student.id)

        cloudScope.launch {

            try {

                StudentFirestoreRepository
                    .updateStudent(
                        student
                    )

                SLHFirebase.clearPendingUpsert(
                    KIND_STUDENTS,
                    student.id
                )

            } catch (e: Exception) {

                /*
                 * Do not break the existing app if Firebase
                 * is temporarily unavailable.
                 *
                 * Local data remains available; retried on
                 * next sync.
                 */
                SLHFirebase.logSyncFailure(
                    "updateStudent ${student.id}",
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

        for (id in SLHFirebase.pendingDeletes(KIND_STUDENTS)) {

            try {

                StudentFirestoreRepository.deleteStudent(id)

                SLHFirebase.clearPendingDelete(KIND_STUDENTS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteStudent $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_STUDENTS)) {

            val student =
                studentsState.value.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                StudentFirestoreRepository.updateStudent(student)

                SLHFirebase.clearPendingUpsert(KIND_STUDENTS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry updateStudent $id", e)
            }
        }
    }


    /*
     * ========================================================
     * DELETE STUDENT
     * ========================================================
     */
    fun deleteStudent(
        studentId: String
    ) {

        studentsState.value =
            studentsState.value.filter {

                it.id != studentId
            }

        /*
         * Existing cascade cleanup.
         */
        FeeStore.deleteFeePlan(
            studentId
        )

        persist()

        /*
         * Firebase delete.
         */
        SLHFirebase.addPendingDelete(KIND_STUDENTS, studentId)

        cloudScope.launch {

            try {

                StudentFirestoreRepository
                    .deleteStudent(
                        studentId
                    )

                SLHFirebase.clearPendingDelete(
                    KIND_STUDENTS,
                    studentId
                )

            } catch (e: Exception) {

                /*
                 * Keep local functionality working.
                 */
                SLHFirebase.logSyncFailure(
                    "deleteStudent $studentId",
                    e
                )
            }
        }
    }


    /*
     * ========================================================
     * FIND STUDENT
     * ========================================================
     */
    fun findStudent(
        id: String
    ): Student? {

        return studentsState.value
            .firstOrNull {

                it.id == id
            }
    }


    /*
     * ========================================================
     * FIND BY USERNAME
     * ========================================================
     */
    fun findStudentByUsername(
        username: String
    ): Student? {

        return studentsState.value
            .firstOrNull {

                it.username.equals(
                    username,
                    ignoreCase = true
                )
            }
    }


    /*
     * ========================================================
     * FIND BY COACHING
     * ========================================================
     */
    fun findStudentsByCoaching(
        coachingId: String
    ): List<Student> {

        return studentsState.value.filter {

            it.coachingId == coachingId
        }
    }


    /*
     * ========================================================
     * FIND BY BATCH
     * ========================================================
     */
    fun findStudentsByBatch(
        coachingId: String,
        batchId: String
    ): List<Student> {

        return studentsState.value.filter {

            it.coachingId == coachingId &&
                    it.batchId == batchId
        }
    }


    /*
     * ========================================================
     * BATCH STUDENT COUNT
     * ========================================================
     */
    fun getBatchStudentCount(
        coachingId: String,
        batchId: String
    ): Int {

        return studentsState.value.count {

            it.coachingId == coachingId &&
                    it.batchId == batchId
        }
    }


    /*
     * ========================================================
     * COACHING STUDENT COUNT
     * ========================================================
     */
    fun getStudentCount(
        coachingId: String
    ): Int {

        return studentsState.value.count {

            it.coachingId == coachingId
        }
    }


    /*
     * ========================================================
     * SEARCH STUDENTS
     * ========================================================
     */
    fun searchStudents(
        coachingId: String,
        query: String
    ): List<Student> {

        val search =
            query.trim()

        return studentsState.value.filter { student ->

            student.coachingId == coachingId &&
                    (
                            search.isBlank() ||

                                    student.name.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.studentId.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.mobile.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.course.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.username.contains(
                                        search,
                                        ignoreCase = true
                                    )
                            )
        }
    }


    /*
     * ========================================================
     * SEARCH BY BATCH
     * ========================================================
     */
    fun searchStudentsByBatch(
        coachingId: String,
        batchId: String,
        query: String
    ): List<Student> {

        val search =
            query.trim()

        return studentsState.value.filter { student ->

            student.coachingId == coachingId &&
                    student.batchId == batchId &&
                    (
                            search.isBlank() ||

                                    student.name.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.studentId.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.mobile.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.course.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||

                                    student.username.contains(
                                        search,
                                        ignoreCase = true
                                    )
                            )
        }
    }


    /*
     * ========================================================
     * MOVE STUDENT TO BATCH
     * ========================================================
     */
    fun moveStudentToBatch(
        studentId: String,
        batchId: String
    ): Boolean {

        val student =
            findStudent(studentId)
                ?: return false

        updateStudent(

            student.copy(
                batchId = batchId
            )
        )

        return true
    }


    /*
     * ========================================================
     * REMOVE STUDENT FROM BATCH
     * ========================================================
     */
    fun removeStudentFromBatch(
        studentId: String
    ): Boolean {

        val student =
            findStudent(studentId)
                ?: return false

        updateStudent(

            student.copy(
                batchId = ""
            )
        )

        return true
    }


    /*
     * ========================================================
     * NEXT INTERNAL ID
     * ========================================================
     */
    fun nextId(): String {

        /*
         * Time based: sequential ids (ST001, ST002 ...) collide
         * when two phones or two coachings add students
         * independently, and the cloud document id is this id.
         */
        var stamp =
            System.currentTimeMillis()

        while (
            studentsState.value.any {
                it.id == "ST$stamp"
            }
        ) {
            stamp++
        }

        return "ST$stamp"
    }


    /*
     * ========================================================
     * NEXT STUDENT ID
     * ========================================================
     */
    fun nextStudentId(): String {

        var number =
            studentsState.value.size + 1

        var studentId =
            "S" +
                    String.format(
                        "%03d",
                        number
                    )

        while (
            studentsState.value.any {
                it.studentId == studentId
            }
        ) {

            number++

            studentId =
                "S" +
                        String.format(
                            "%03d",
                            number
                        )
        }

        return studentId
    }


    /*
     * ========================================================
     * FIREBASE -> LOCAL SYNC
     * ========================================================
     *
     * Downloads the students of one coaching; the cloud copy is
     * the source of truth for that coaching (other coachings'
     * local data is never touched).
     *
     * canWrite (coaching admin phones): the first time, local
     * students that are missing in the cloud are uploaded once,
     * so students created before the migration are not lost.
     *
     * Passwords are NOT replaced because Firebase Firestore
     * intentionally does not contain passwords.
     * Offline / error: nothing changes locally.
     */
    fun syncFromFirebase(
        coachingId: String,
        canWrite: Boolean = false,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {

        cloudScope.launch {

            try {

                // Push whatever failed to upload earlier.
                flushPending(coachingId)

                var cloudStudents =
                    StudentFirestoreRepository
                        .findStudentsByCoaching(
                            coachingId
                        )

                if (
                    canWrite &&
                    !SLHFirebase.isFirstUploadDone(
                        "students",
                        coachingId
                    )
                ) {

                    val cloudIds =
                        cloudStudents
                            .map { it.id }
                            .toSet()

                    val missing =
                        studentsState.value.filter {
                            it.coachingId == coachingId &&
                                    it.id !in cloudIds
                        }

                    missing.forEach {
                        StudentFirestoreRepository
                            .addStudent(it)
                    }

                    SLHFirebase.markFirstUploadDone(
                        "students",
                        coachingId
                    )

                    cloudStudents =
                        cloudStudents + missing
                }

                val localStudents =
                    studentsState.value

                val mergedStudents =
                    cloudStudents.map { cloudStudent ->

                        val localStudent =
                            localStudents.firstOrNull {
                                it.id == cloudStudent.id
                            }

                        if (localStudent != null) {

                            cloudStudent.copy(
                                password =
                                    localStudent.password
                            )

                        } else {

                            cloudStudent
                        }
                    }

                // Keep local students that could not be uploaded yet,
                // and do not resurrect ones whose delete is pending.
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_STUDENTS)

                val pendingDeletedIds =
                    SLHFirebase.pendingDeletes(KIND_STUDENTS)

                val keep =
                    studentsState.value.filter {
                        it.coachingId == coachingId &&
                                it.id in pendingIds
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                val filteredMerged =
                    mergedStudents.filter {
                        it.id !in keepIds &&
                                it.id !in pendingDeletedIds
                    }

                studentsState.value =
                    studentsState.value.filter {
                        it.coachingId != coachingId
                    } + filteredMerged + keep

                persist()

                onComplete?.invoke(
                    true,
                    null
                )

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
     * STUDENT PHONE: OWN RECORD
     * ========================================================
     *
     * A student may only read their own document, so the
     * coaching-wide sync above is not used on student phones.
     */
    fun syncOwnFromFirebase(
        studentId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {

        cloudScope.launch {

            try {

                val cloudStudent =
                    StudentFirestoreRepository
                        .findStudent(studentId)

                if (cloudStudent != null) {

                    val localStudent =
                        studentsState.value.firstOrNull {
                            it.id == cloudStudent.id
                        }

                    cacheFromCloud(
                        if (localStudent != null) {
                            cloudStudent.copy(
                                password =
                                    localStudent.password
                            )
                        } else {
                            cloudStudent
                        }
                    )
                }

                onComplete?.invoke(true, null)

            } catch (exception: Exception) {

                onComplete?.invoke(
                    false,
                    exception.message
                )
            }
        }
    }


    /**
     * Stores a student that came from the cloud, on this phone
     * only (nothing is written back to Firestore).
     */
    fun cacheFromCloud(
        student: Student
    ) {

        studentsState.value =
            if (
                studentsState.value.any {
                    it.id == student.id
                }
            ) {

                studentsState.value.map {
                    if (it.id == student.id) {
                        student
                    } else {
                        it
                    }
                }

            } else {

                studentsState.value + student
            }

        persist()
    }


    /*
     * ========================================================
     * UPLOAD ALL LOCAL STUDENTS
     * ========================================================
     *
     * Useful for first migration.
     *
     * Existing local students are uploaded to Firestore.
     */
    fun uploadAllToFirebase(
        coachingId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {

        cloudScope.launch {

            try {

                val coachingStudents =
                    studentsState.value.filter {

                        it.coachingId ==
                                coachingId
                    }

                for (student in coachingStudents) {

                    StudentFirestoreRepository
                        .addStudent(
                            student
                        )
                }

                onComplete?.invoke(
                    true,
                    null
                )

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
     * CLEAR
     * ========================================================
     */
    fun clear() {

        studentsState.value =
            emptyList()

        persist()
    }
}