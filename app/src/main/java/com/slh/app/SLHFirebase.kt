package com.slh.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ============================================================
 * SLH FIREBASE SESSION HELPER
 * ============================================================
 *
 * Role users (principal / admin / teacher) and students sign in
 * on SECONDARY FirebaseApp instances, so that they never replace
 * each other's session.
 *
 * Firestore, however, was always used through the DEFAULT app,
 * which nobody signs in to. Every request therefore reached
 * Firestore WITHOUT an auth token, so security rules could not
 * tell users apart.
 *
 * [db] fixes that: it returns the Firestore instance that belongs
 * to whichever secondary app currently has a signed-in user.
 *
 * Priority: role session -> student session -> default (anonymous).
 *
 * ============================================================
 */
object SLHFirebase {

    /*
     * Must match SECONDARY_APP_NAME in
     * FirebaseRoleAuthRepository / FirebaseStudentAuthRepository.
     */
    private const val ROLE_APP = "slh_role_auth"

    private const val STUDENT_APP = "slh_student_auth"

    private var appContext: Context? = null

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /**
     * True for debug builds only. Demo accounts (admin01 / 123456 ...)
     * are created and kept ONLY when this is true; a release build
     * never has them.
     */
    var isDebuggable: Boolean = false
        private set

    fun init(context: Context) {

        appContext = context.applicationContext

        isDebuggable =
            (
                    context.applicationInfo.flags and
                            ApplicationInfo.FLAG_DEBUGGABLE
                    ) != 0
    }


    private fun signedInApp(
        name: String
    ): FirebaseApp? {

        return try {

            val app =
                FirebaseApp.getInstance(name)

            if (
                FirebaseAuth
                    .getInstance(app)
                    .currentUser != null
            ) {
                app
            } else {
                null
            }

        } catch (_: Exception) {

            null
        }
    }


    /**
     * Firestore instance carrying the current user's auth token.
     */
    val db: FirebaseFirestore
        get() {

            val app =
                signedInApp(ROLE_APP)
                    ?: signedInApp(STUDENT_APP)

            return if (app != null) {

                FirebaseFirestore.getInstance(app)

            } else {

                FirebaseFirestore.getInstance()
            }
        }


    /**
     * Ends every Firebase session. Called whenever the login
     * screen is shown so a previous user's session can never be
     * reused by the next person on the same phone.
     */
    fun signOutAll() {

        for (name in listOf(ROLE_APP, STUDENT_APP)) {

            try {

                FirebaseAuth
                    .getInstance(
                        FirebaseApp.getInstance(name)
                    )
                    .signOut()

            } catch (_: Exception) {

                // App not initialised yet -> nothing to sign out.
            }
        }
    }


    /*
     * ========================================================
     * FIRST-UPLOAD FLAGS
     * ========================================================
     *
     * The first time an ADMIN phone syncs a kind of data
     * ("batches", "students", "teachers") for a coaching, local
     * records that are missing in the cloud are uploaded once,
     * so nothing that existed before the Firebase migration is
     * lost when the cloud copy becomes the source of truth.
     * The flag is only set after that upload succeeded.
     */
    private fun migrationPrefs() =
        appContext?.getSharedPreferences(
            "slh_cloud_migration",
            Context.MODE_PRIVATE
        )

    fun isFirstUploadDone(
        kind: String,
        coachingId: String
    ): Boolean {

        return migrationPrefs()
            ?.getBoolean(
                "first_upload_${kind}_$coachingId",
                false
            )
            ?: true
    }

    fun markFirstUploadDone(
        kind: String,
        coachingId: String
    ) {

        migrationPrefs()
            ?.edit()
            ?.putBoolean(
                "first_upload_${kind}_$coachingId",
                true
            )
            ?.apply()
    }


    /*
     * ========================================================
     * PENDING (NOT-YET-SYNCED) TRACKING
     * ========================================================
     *
     * Every write-through to Firestore first records the record id
     * here and removes it again only after Firestore confirmed the
     * write. If the write fails (rules, auth not ready, app killed
     * mid-write ...) the id stays, so the next syncFromFirebase()
     * can retry it and will NOT overwrite that local record with the
     * older cloud copy.
     *
     *   kind = "tests", "testResults", "questions", ...
     */
    private const val PENDING_PREFS = "slh_cloud_pending"

    private val pendingLock = Any()

    private fun pendingPrefs() =
        appContext?.getSharedPreferences(
            PENDING_PREFS,
            Context.MODE_PRIVATE
        )

    private fun readPending(key: String): Set<String> =
        pendingPrefs()
            ?.getStringSet(key, null)
            ?.toSet()
            ?: emptySet()

    private fun writePending(
        key: String,
        values: Set<String>
    ) {

        pendingPrefs()
            ?.edit()
            ?.putStringSet(key, HashSet(values))
            ?.apply()
    }

    fun pendingUpserts(kind: String): Set<String> =
        synchronized(pendingLock) {
            readPending("up_$kind")
        }

    fun pendingDeletes(kind: String): Set<String> =
        synchronized(pendingLock) {
            readPending("del_$kind")
        }

    fun addPendingUpsert(kind: String, id: String) {

        synchronized(pendingLock) {
            writePending("up_$kind", readPending("up_$kind") + id)
            writePending("del_$kind", readPending("del_$kind") - id)
        }
    }

    fun clearPendingUpsert(kind: String, id: String) {

        synchronized(pendingLock) {
            writePending("up_$kind", readPending("up_$kind") - id)
        }
    }

    fun addPendingDelete(kind: String, id: String) {

        synchronized(pendingLock) {
            writePending("del_$kind", readPending("del_$kind") + id)
            writePending("up_$kind", readPending("up_$kind") - id)
        }
    }

    fun clearPendingDelete(kind: String, id: String) {

        synchronized(pendingLock) {
            writePending("del_$kind", readPending("del_$kind") - id)
        }
    }

    /**
     * Firestore failures used to be swallowed silently. They are now
     * visible in Logcat (filter by tag "SLHSync"), e.g.
     * PERMISSION_DENIED means the security rules blocked the request.
     */
    fun logSyncFailure(what: String, error: Throwable) {

        Log.w("SLHSync", "$what failed: ${error.message}", error)
    }


    /**
     * One-time backfill: adds "authEmail" to the Firestore student
     * documents of a coaching (needed by the security rules to
     * recognise a student). Only that one field is touched, so no
     * other data can be overwritten. Runs once per coaching per
     * phone; retried on the next admin login if it failed.
     */
    fun backfillStudentAuthEmail(
        coachingId: String
    ) {

        val context =
            appContext
                ?: return

        val prefs =
            context.getSharedPreferences(
                "slh_cloud_migration",
                Context.MODE_PRIVATE
            )

        val key =
            "student_auth_email_v1_$coachingId"

        if (prefs.getBoolean(key, false)) {

            return
        }

        scope.launch {

            try {

                StudentFirestoreRepository
                    .backfillAuthEmail(
                        StudentStore.students.filter {
                            it.coachingId == coachingId
                        }
                    )

                prefs.edit()
                    .putBoolean(key, true)
                    .apply()

            } catch (_: Exception) {

                // Will be retried on next admin login.
            }
        }
    }
}
