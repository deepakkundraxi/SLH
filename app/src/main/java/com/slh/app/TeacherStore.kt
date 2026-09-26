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
 * TEACHER MODEL
 * ============================================================
 */

data class Teacher(

    val id: String,

    val coachingId: String,

    val name: String,

    val mobile: String = "",

    val email: String = "",

    val address: String = "",

    val subject: String = "",

    val qualification: String = "",

    val username: String = "",

    val password: String = "",

    val joiningDate: String = "",

    val accountStatus: String = "ACTIVE"
)


/*
 * ============================================================
 * TEACHER STORE
 * ============================================================
 */

object TeacherStore {

    private var teachersState by mutableStateOf(
        emptyList<Teacher>()
    )

    private var initialized = false

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /*
     * ========================================================
     * FIREBASE WRITE-THROUGH
     * ========================================================
     *
     * Local data is saved first, then pushed to Firestore in the
     * background (Firestore queues it while offline). The password
     * hash is never sent.
     */
    private const val KIND_TEACHERS = "teachers"

    private fun syncTeacherToFirebase(
        teacher: Teacher
    ) {

        // Stays "pending" until Firestore confirms the write.
        SLHFirebase.addPendingUpsert(KIND_TEACHERS, teacher.id)

        cloudScope.launch {

            try {

                TeacherFirestoreRepository
                    .saveTeacher(teacher)

                SLHFirebase.clearPendingUpsert(
                    KIND_TEACHERS,
                    teacher.id
                )

            } catch (e: Exception) {

                // Local data stays valid; retried on next sync.
                SLHFirebase.logSyncFailure(
                    "saveTeacher ${teacher.id}",
                    e
                )
            }
        }
    }

    private fun deleteTeacherFromFirebase(
        teacherId: String
    ) {

        SLHFirebase.addPendingDelete(KIND_TEACHERS, teacherId)

        cloudScope.launch {

            try {

                TeacherFirestoreRepository
                    .deleteTeacher(teacherId)

                SLHFirebase.clearPendingDelete(
                    KIND_TEACHERS,
                    teacherId
                )

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "deleteTeacher $teacherId",
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

        for (id in SLHFirebase.pendingDeletes(KIND_TEACHERS)) {

            try {

                TeacherFirestoreRepository.deleteTeacher(id)

                SLHFirebase.clearPendingDelete(KIND_TEACHERS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry deleteTeacher $id", e)
            }
        }

        for (id in SLHFirebase.pendingUpserts(KIND_TEACHERS)) {

            val teacher =
                teachersState.firstOrNull {
                    it.id == id && it.coachingId == coachingId
                } ?: continue

            try {

                TeacherFirestoreRepository.saveTeacher(teacher)

                SLHFirebase.clearPendingUpsert(KIND_TEACHERS, id)

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure("retry saveTeacher $id", e)
            }
        }
    }


    /*
     * ========================================================
     * FIREBASE -> LOCAL SYNC
     * ========================================================
     *
     * asAdmin = true  : full records (admin phones). The first
     *                   time, local teachers missing in the cloud
     *                   are uploaded once.
     * asAdmin = false : directory only (name + subject) for
     *                   teachers and students; a teacher also
     *                   receives their own full record
     *                   (ownUsername).
     *
     * Local passwords (hashes) are kept; the cloud never has them.
     * Offline / error: nothing changes locally.
     */
    fun syncFromFirebase(
        coachingId: String,
        asAdmin: Boolean,
        ownUsername: String? = null,
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

                var cloudTeachers: List<Teacher>

                if (asAdmin) {

                    cloudTeachers =
                        TeacherFirestoreRepository
                            .findTeachersByCoaching(
                                coachingId
                            )

                    if (
                        !SLHFirebase.isFirstUploadDone(
                            "teachers",
                            coachingId
                        )
                    ) {

                        val cloudIds =
                            cloudTeachers
                                .map { it.id }
                                .toSet()

                        val missing =
                            teachersState.filter {
                                it.coachingId == coachingId &&
                                        it.id !in cloudIds
                            }

                        missing.forEach {
                            TeacherFirestoreRepository
                                .saveTeacher(it)
                        }

                        SLHFirebase.markFirstUploadDone(
                            "teachers",
                            coachingId
                        )

                        cloudTeachers =
                            cloudTeachers + missing
                    }

                } else {

                    cloudTeachers =
                        TeacherFirestoreRepository
                            .findDirectoryByCoaching(
                                coachingId
                            )

                    val own =
                        ownUsername?.let {
                            TeacherFirestoreRepository
                                .findOwnTeacher(it)
                        }

                    if (own != null) {

                        cloudTeachers =
                            cloudTeachers.filter {
                                it.id != own.id
                            } + own
                    }
                }

                val localTeachers =
                    teachersState

                val merged =
                    cloudTeachers.map { cloud ->

                        val local =
                            localTeachers.firstOrNull {
                                it.id == cloud.id
                            }

                        if (local != null) {

                            cloud.copy(
                                password = local.password
                            )

                        } else {

                            cloud
                        }
                    }

                // Keep local teachers that could not be uploaded yet,
                // and do not resurrect ones whose delete is pending
                // (both only matter for the admin's own coaching).
                val pendingIds =
                    SLHFirebase.pendingUpserts(KIND_TEACHERS)

                val pendingDeletedIds =
                    SLHFirebase.pendingDeletes(KIND_TEACHERS)

                val keep =
                    if (asAdmin) {
                        teachersState.filter {
                            it.coachingId == coachingId &&
                                    it.id in pendingIds
                        }
                    } else {
                        emptyList()
                    }

                val keepIds =
                    keep.map { it.id }.toSet()

                val filteredMerged =
                    if (asAdmin) {
                        merged.filter {
                            it.id !in keepIds &&
                                    it.id !in pendingDeletedIds
                        }
                    } else {
                        merged
                    }

                teachersState =
                    teachersState.filter {
                        it.coachingId != coachingId
                    } + filteredMerged + keep

                persist()

                onComplete?.invoke(true, null)

            } catch (exception: Exception) {

                SLHFirebase.logSyncFailure(
                    "TeacherStore.syncFromFirebase",
                    exception
                )

                onComplete?.invoke(
                    false,
                    exception.message
                )
            }
        }
    }


    /*
     * Load previously saved teachers from disk.
     * If none exist yet, starts with an empty list
     * (no demo teachers needed).
     *
     * Must be called once, e.g. from MainActivity.onCreate,
     * before any screen reads TeacherStore.teachers — otherwise
     * every teacher added is only kept in memory and is lost
     * the moment the app process is killed.
     */

    fun initialize() {

        if (initialized) {
            return
        }

        initialized = true

        val saved =
            SLHLocalStorage.loadTeachers()

        teachersState =
            saved ?: emptyList()
    }

    private fun persist() {

        SLHLocalStorage.saveTeachers(
            teachersState
        )
    }


    /*
     * Read-only teacher list
     */

    val teachers: List<Teacher>
        get() = teachersState


    /*
     * Find teacher by ID
     */

    fun findTeacher(
        teacherId: String
    ): Teacher? {

        return teachersState.firstOrNull {

            it.id == teacherId
        }
    }


    /*
     * Find teachers belonging to one coaching
     */

    fun findTeachersByCoaching(
        coachingId: String
    ): List<Teacher> {

        return teachersState.filter {

            it.coachingId == coachingId
        }
    }


    /*
     * Find teacher by username
     */

    fun findTeacherByUsername(
        username: String
    ): Teacher? {

        return teachersState.firstOrNull {

            it.username.equals(
                username,
                ignoreCase = true
            )
        }
    }

    /**
     * Login DemoUser.id is "U" + Teacher.id (see TeacherManagementScreen).
     * Batch.teacherId stores the real Teacher.id.
     * Always resolve through this helper so assigned batches match.
     */
    fun resolveTeacherIdForUser(user: DemoUser): String {

        findTeacherByUsername(user.username)?.id?.let {
            return it
        }

        if (user.id.startsWith("U") && user.id.length > 1) {
            val withoutPrefix = user.id.removePrefix("U")
            if (findTeacher(withoutPrefix) != null) {
                return withoutPrefix
            }
        }

        if (findTeacher(user.id) != null) {
            return user.id
        }

        return user.id
    }

    fun resolveTeacherForUser(user: DemoUser): Teacher? {
        val id = resolveTeacherIdForUser(user)
        return findTeacher(id) ?: findTeacherByUsername(user.username)
    }


    /*
     * Add teacher
     */

    fun addTeacher(
        teacher: Teacher
    ): Boolean {

        /*
         * Prevent duplicate teacher ID.
         */

        if (
            teachersState.any {
                it.id == teacher.id
            }
        ) {

            return false
        }


        /*
         * Prevent duplicate username.
         */

        if (
            teacher.username.isNotBlank() &&
            teachersState.any {

                it.username.equals(
                    teacher.username,
                    ignoreCase = true
                )
            }
        ) {

            return false
        }


        teachersState =
            teachersState + teacher

        persist()

        syncTeacherToFirebase(teacher)

        return true
    }


    /*
     * Update teacher
     */

    fun updateTeacher(
        teacher: Teacher
    ): Boolean {

        val exists =
            teachersState.any {

                it.id == teacher.id
            }

        if (!exists) {
            return false
        }


        /*
         * Do not allow another teacher
         * to use the same username.
         */

        if (
            teacher.username.isNotBlank() &&
            teachersState.any {

                it.id != teacher.id &&
                        it.username.equals(
                            teacher.username,
                            ignoreCase = true
                        )
            }
        ) {

            return false
        }


        teachersState =
            teachersState.map {

                if (
                    it.id == teacher.id
                ) {

                    teacher

                } else {

                    it
                }
            }

        persist()

        syncTeacherToFirebase(teacher)

        return true
    }


    /*
     * Delete teacher
     */

    fun deleteTeacher(
        teacherId: String
    ): Boolean {

        val oldSize =
            teachersState.size

        teachersState =
            teachersState.filterNot {

                it.id == teacherId
            }

        val deleted =
            teachersState.size < oldSize

        if (deleted) {
            persist()

            deleteTeacherFromFirebase(teacherId)
        }

        return deleted
    }


    /*
     * Search teachers
     */

    fun searchTeachers(
        coachingId: String,
        query: String
    ): List<Teacher> {

        val search =
            query.trim()

        return teachersState.filter { teacher ->

            teacher.coachingId == coachingId &&
                    (
                            search.isBlank() ||
                                    teacher.name.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||
                                    teacher.mobile.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||
                                    teacher.subject.contains(
                                        search,
                                        ignoreCase = true
                                    ) ||
                                    teacher.username.contains(
                                        search,
                                        ignoreCase = true
                                    )
                            )
        }
    }


    /*
     * Teacher count for one coaching
     */

    fun getTeacherCount(
        coachingId: String
    ): Int {

        return teachersState.count {

            it.coachingId == coachingId
        }
    }


    /*
     * Active teacher count
     */

    fun getActiveTeacherCount(
        coachingId: String
    ): Int {

        return teachersState.count {

            it.coachingId == coachingId &&
                    it.accountStatus.equals(
                        "ACTIVE",
                        ignoreCase = true
                    )
        }
    }


    /*
     * Clear all teachers
     */

    fun clear() {

        teachersState =
            emptyList()
    }
}