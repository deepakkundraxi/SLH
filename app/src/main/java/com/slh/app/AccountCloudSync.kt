package com.slh.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ============================================================
 * ACCOUNT CLOUD SYNC
 * ============================================================
 *
 * Keeps the Firestore side of staff accounts (users/{uid}) in
 * step with the local UserAccountStore, so that admins and
 * teachers can log in from a different phone.
 *
 * - provision : creates the Firebase Auth account + users/{uid}
 *               (needs the plain password, so it is called from
 *               the screens where the password is typed)
 * - pushProfile / deleteProfile : status, name and removal
 *               (called by UserAccountStore, no password needed)
 *
 * Everything runs in the background. A failure never breaks the
 * local flow; only provisioning problems are shown as a Toast
 * because the person creating the account can fix them.
 *
 * ============================================================
 */
object AccountCloudSync {

    private const val USERS_COLLECTION = "users"

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    private fun toast(
        context: Context,
        message: String
    ) {

        Handler(Looper.getMainLooper()).post {

            Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /**
     * Create the cloud login for a newly created (or re-saved with
     * a new password) staff account.
     */
    fun provision(
        context: Context,
        user: DemoUser,
        plainPassword: String
    ) {

        val appContext =
            context.applicationContext

        scope.launch {

            val result =
                try {

                    FirebaseRoleAuthRepository
                        .provisionAccount(
                            appContext,
                            user,
                            plainPassword
                        )

                } catch (_: Exception) {

                    ProvisionResult.FAILED
                }

            val message =
                when (result) {

                    ProvisionResult.OK ->
                        null

                    ProvisionResult.WEAK_PASSWORD ->
                        "Saved on this phone. Use at least 6 characters " +
                                "in the password to enable login from other phones."

                    ProvisionResult.ACCOUNT_CONFLICT ->
                        "Saved on this phone, but this username already " +
                                "exists in the cloud with a different password, " +
                                "so it cannot log in from other phones."

                    ProvisionResult.PROFILE_FAILED ->
                        "Saved on this phone, but the cloud profile could " +
                                "not be written. Check internet and try again."

                    ProvisionResult.FAILED ->
                        "Saved on this phone, but the cloud account could " +
                                "not be created. Check internet and try again."
                }

            if (message != null) {

                toast(appContext, message)
            }
        }
    }


    private suspend fun findProfileDocs(
        username: String,
        coachingId: String?
    ): List<DocumentSnapshot> {

        /*
         * coachingId is part of the query so that it satisfies the
         * security rules used by coaching admins.
         */
        var query: Query =
            SLHFirebase.db
                .collection(USERS_COLLECTION)
                .whereEqualTo(
                    "username",
                    username.trim()
                )

        if (!coachingId.isNullOrBlank()) {

            query =
                query.whereEqualTo(
                    "coachingId",
                    coachingId
                )
        }

        return query
            .get()
            .await()
            .documents
    }


    /**
     * Push status + display name of an existing account.
     */
    fun pushProfile(
        user: DemoUser
    ) {

        if (user.username.isBlank()) {
            return
        }

        scope.launch {

            val update =
                mapOf(
                    "status" to user.status.name,
                    "displayName" to user.displayName
                )

            val mirrorCollection =
                FirebaseRoleAuthRepository
                    .mirrorCollectionForRole(user.role)

            try {

                for (
                doc in
                findProfileDocs(
                    user.username,
                    user.coachingId
                )
                ) {

                    doc.reference
                        .update(update)
                        .await()

                    // Best-effort: keep the console mirror in step.
                    if (mirrorCollection != null) {

                        try {

                            SLHFirebase.db
                                .collection(mirrorCollection)
                                .document(doc.id)
                                .update(update)
                                .await()

                        } catch (e: Exception) {

                            SLHFirebase.logSyncFailure(
                                "mirror update $mirrorCollection/${doc.id}",
                                e
                            )
                        }
                    }
                }

            } catch (_: Exception) {

                // Not allowed / offline: local data stays valid.
            }
        }
    }


    /**
     * Remove the cloud profile (the account can no longer be
     * used for cloud access). The Firebase Auth entry itself can
     * only be removed from the Firebase Console.
     */
    fun deleteProfile(
        user: DemoUser
    ) {

        if (user.username.isBlank()) {
            return
        }

        scope.launch {

            val mirrorCollection =
                FirebaseRoleAuthRepository
                    .mirrorCollectionForRole(user.role)

            try {

                for (
                doc in
                findProfileDocs(
                    user.username,
                    user.coachingId
                )
                ) {

                    doc.reference
                        .delete()
                        .await()

                    // Best-effort: remove the console mirror too.
                    if (mirrorCollection != null) {

                        try {

                            SLHFirebase.db
                                .collection(mirrorCollection)
                                .document(doc.id)
                                .delete()
                                .await()

                        } catch (e: Exception) {

                            SLHFirebase.logSyncFailure(
                                "mirror delete $mirrorCollection/${doc.id}",
                                e
                            )
                        }
                    }
                }

            } catch (_: Exception) {

                // Not allowed / offline: local data stays valid.
            }
        }
    }
}
