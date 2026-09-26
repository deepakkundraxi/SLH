package com.slh.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Result of a [FirebaseRoleAuthRepository.cloudLogin] /
 * [FirebaseStudentAuthRepository.cloudLogin] attempt.
 *
 * Login used to collapse every failure (wrong password, no
 * internet, a Firestore permission error, ...) into the same
 * generic "invalid username or password" message, which made it
 * impossible to tell a real typo apart from a phone with no
 * internet or a blocked cloud request. This makes the reason
 * explicit so the login screen can show the right message.
 */
sealed class CloudLoginOutcome<out T> {

    data class Success<T>(val value: T) : CloudLoginOutcome<T>()

    /** Firebase Authentication rejected the password. */
    data object WrongPassword : CloudLoginOutcome<Nothing>()

    /**
     * Authentication succeeded but there is no matching, approved
     * profile for this username/role - or no account exists at
     * all (Firebase Auth itself does not tell those two apart).
     */
    data object NotFound : CloudLoginOutcome<Nothing>()

    /** The phone could not reach Firebase at all. */
    data object NoInternet : CloudLoginOutcome<Nothing>()

    /** Firebase reachable but the request itself failed/was denied. */
    data object ServerBlocked : CloudLoginOutcome<Nothing>()
}

/**
 * ============================================================
 * FIREBASE ROLE AUTH REPOSITORY
 * ============================================================
 *
 * Purpose:
 *
 * Migrates existing local:
 *
 *      Principal Admin
 *      Coaching Admin
 *      Teacher
 *
 * accounts toward Firebase Authentication.
 *
 * IMPORTANT:
 * - Existing local authentication remains the fallback.
 * - Password is NEVER stored in Firestore.
 * - Role metadata is stored in Firestore.
 * - A secondary Firebase App is used so that the existing
 *   Firebase session used elsewhere is not replaced.
 *
 * Firestore:
 *
 * users/{firebaseUid}
 *
 * Example:
 *
 * {
 *   username: "admin01",
 *   role: "COACHING_ADMIN",
 *   status: "APPROVED",
 *   coachingId: "C001",
 *   displayName: "Coaching Admin"
 * }
 *
 * ============================================================
 */
object FirebaseRoleAuthRepository {

    private const val SECONDARY_APP_NAME =
        "slh_role_auth"

    private const val USERS_COLLECTION =
        "users"

    /*
     * "users/{uid}" stays the SINGLE source of truth for login and
     * security rules - nothing here changes that. These two extra
     * collections are a best-effort MIRROR of the same document,
     * written purely so the Firebase console shows admins and
     * teachers separately from students/developer instead of all
     * mixed together in "users". A failure to write the mirror is
     * swallowed; it must never affect login or account creation.
     */
    private const val ADMINS_MIRROR_COLLECTION =
        "admins"

    private const val TEACHERS_MIRROR_COLLECTION =
        "teacherLogins"

    /**
     * Mirror collection for a role, or null when that role has
     * no mirror (PRINCIPAL_ADMIN / developer stays only in
     * "users"; STUDENT has its own file, see
     * FirebaseStudentAuthRepository).
     */
    fun mirrorCollectionForRole(
        role: UserRole
    ): String? {

        return when (role) {

            UserRole.COACHING_ADMIN ->
                ADMINS_MIRROR_COLLECTION

            UserRole.TEACHER ->
                TEACHERS_MIRROR_COLLECTION

            else ->
                null
        }
    }

    private const val PROVISION_APP_NAME =
        "slh_provision"

    private var secondaryAuth: FirebaseAuth? = null


    /**
     * --------------------------------------------------------
     * GET / CREATE SECONDARY FIREBASE AUTH INSTANCE
     * --------------------------------------------------------
     */
    private fun getSecondaryAuth(
        context: Context
    ): FirebaseAuth {

        secondaryAuth?.let {
            return it
        }

        val app = try {

            FirebaseApp.getInstance(
                SECONDARY_APP_NAME
            )

        } catch (_: Exception) {

            val defaultApp =
                FirebaseApp.getInstance()

            val options =
                FirebaseOptions.Builder()
                    .setApplicationId(
                        defaultApp.options.applicationId
                    )
                    .setApiKey(
                        defaultApp.options.apiKey
                    )
                    .setProjectId(
                        defaultApp.options.projectId
                    )
                    .setStorageBucket(
                        defaultApp.options.storageBucket
                    )
                    .build()

            FirebaseApp.initializeApp(
                context.applicationContext,
                options,
                SECONDARY_APP_NAME
            )!!
        }

        return FirebaseAuth
            .getInstance(app)
            .also {
                secondaryAuth = it
            }
    }


    /**
     * --------------------------------------------------------
     * USERNAME -> SYNTHETIC EMAIL
     * --------------------------------------------------------
     *
     * Firebase Email/Password authentication requires
     * an email-style identifier.
     *
     * We do NOT use a real personal email address.
     *
     * Example:
     *
     * admin01
     *
     * becomes something like:
     *
     * 61646d696e3031@users.slh.app
     *
     * --------------------------------------------------------
     */
    fun usernameToFirebaseEmail(
        username: String
    ): String {

        val normalized =
            username
                .trim()
                .lowercase(Locale.US)

        val hex =
            normalized
                .toByteArray(Charsets.UTF_8)
                .joinToString("") {
                    "%02x".format(
                        it.toInt() and 0xFF
                    )
                }

        return "$hex@users.slh.app"
    }


    /**
     * --------------------------------------------------------
     * ENSURE FIREBASE ACCOUNT
     * --------------------------------------------------------
     *
     * This method is intentionally called only after the
     * existing local password has already been verified.
     *
     * If the Firebase account already exists:
     *
     *     sign in
     *
     * If it does not exist:
     *
     *     create account
     *
     * Existing Firebase Authentication account is therefore
     * never overwritten.
     *
     * --------------------------------------------------------
     */
    suspend fun ensureFirebaseAccount(
        context: Context,
        username: String,
        password: String
    ): String? {

        if (
            username
                .trim()
                .isBlank() ||
            password.isBlank()
        ) {
            return null
        }

        val auth =
            getSecondaryAuth(context)

        val email =
            usernameToFirebaseEmail(username)

        return try {

            val result =
                auth
                    .signInWithEmailAndPassword(
                        email,
                        password
                    )
                    .await()

            result.user?.uid

        } catch (signInException: Exception) {

            try {

                val result =
                    auth
                        .createUserWithEmailAndPassword(
                            email,
                            password
                        )
                        .await()

                result.user?.uid

            } catch (createException: Exception) {

                /*
                 * The account may already exist but the password
                 * could have changed.
                 *
                 * Do not modify or delete the existing account.
                 *
                 * Returning null allows the existing local
                 * authentication flow to continue.
                 */
                null
            }
        }
    }


    /**
     * --------------------------------------------------------
     * SYNC ROLE PROFILE
     * --------------------------------------------------------
     *
     * Password is deliberately absent.
     * --------------------------------------------------------
     */
    suspend fun syncUserProfile(
        uid: String,
        user: DemoUser,
        firestore: FirebaseFirestore? = null
    ) {

        val data =
            hashMapOf<String, Any?>(
                "id" to user.id,
                "uid" to uid,
                "username" to user.username.trim(),
                "role" to user.role.name,
                "status" to user.status.name,
                "coachingId" to user.coachingId,
                "displayName" to user.displayName
            )

        /*
         * Use whichever Firestore instance carries the RIGHT auth
         * token for this write:
         *
         * - Caller passes one explicitly (provisionAccount does
         *   this, using the just-created user's own still-active
         *   session, before it gets signed out) - needed because
         *   "users/{uid}" can usually only be written by that same
         *   uid under the security rules.
         * - Otherwise fall back to the secondary role-auth session
         *   that just signed in (migrateCurrentUser's case), or
         *   finally the main app as a last resort.
         */
        val db =
            firestore
                ?: secondaryAuth?.let {
                    FirebaseFirestore.getInstance(it.app)
                }
                ?: SLHFirebase.db

        db
            .collection(USERS_COLLECTION)
            .document(uid)
            .set(data)
            .await()

        // Best-effort mirror - never allowed to fail the real write.
        mirrorCollectionForRole(user.role)?.let { mirrorCollection ->

            try {

                db
                    .collection(mirrorCollection)
                    .document(uid)
                    .set(data)
                    .await()

            } catch (e: Exception) {

                SLHFirebase.logSyncFailure(
                    "mirror write $mirrorCollection/$uid",
                    e
                )
            }
        }
    }


    /**
     * --------------------------------------------------------
     * MIGRATE CURRENT USER
     * --------------------------------------------------------
     *
     * Existing local password is verified BEFORE this method
     * is called.
     *
     * The plaintext password exists only in memory for the
     * duration of the authentication operation.
     * --------------------------------------------------------
     */
    suspend fun migrateCurrentUser(
        context: Context,
        user: DemoUser,
        password: String
    ): Boolean {

        /*
         * Only non-student roles are handled here.
         *
         * Student already has its own Firebase authentication
         * repository and flow.
         */
        if (
            user.role == UserRole.STUDENT
        ) {
            return false
        }

        if (
            user.status !=
            AccountStatus.APPROVED
        ) {
            return false
        }
        if (DemoData.isSeedUser(user)) {
            return false
        }

        val uid =
            ensureFirebaseAccount(
                context = context,
                username = user.username,
                password = password
            )
                ?: return false

        return try {

            syncUserProfile(
                uid = uid,
                user = user.copy(
                    password = ""
                )
            )

            true

        } catch (_: Exception) {

            false
        }
    }


    /**
     * --------------------------------------------------------
     * SIGN OUT SECONDARY AUTH
     * --------------------------------------------------------
     *
     * This does NOT affect the main/default FirebaseAuth
     * instance.
     * --------------------------------------------------------
     */
    fun signOut() {

        secondaryAuth?.signOut()
    }


    /**
     * --------------------------------------------------------
     * CLOUD LOGIN (account created on another phone)
     * --------------------------------------------------------
     *
     * Used only when NO local account exists for the typed
     * username + role. Firebase Authentication checks the
     * password; the role, status and coaching come from
     * users/{uid}. The role session stays signed in on success
     * and is signed out again on any failure.
     *
     * Returns a DemoUser with an EMPTY password (the caller stores
     * a hash of what the user typed).
     * --------------------------------------------------------
     */
    suspend fun cloudLogin(
        context: Context,
        username: String,
        password: String,
        role: UserRole
    ): CloudLoginOutcome<DemoUser> {

        if (
            username.trim().isBlank() ||
            password.isBlank()
        ) {
            return CloudLoginOutcome.WrongPassword
        }

        val auth =
            getSecondaryAuth(context)

        val uid =
            try {

                auth
                    .signInWithEmailAndPassword(
                        usernameToFirebaseEmail(username),
                        password
                    )
                    .await()
                    .user
                    ?.uid
                    ?: return CloudLoginOutcome.NotFound

            } catch (e: FirebaseNetworkException) {

                return CloudLoginOutcome.NoInternet

            } catch (e: FirebaseAuthInvalidUserException) {

                // No account with this (synthetic) email.
                return CloudLoginOutcome.NotFound

            } catch (e: FirebaseAuthInvalidCredentialsException) {

                // Account exists, password is wrong.
                return CloudLoginOutcome.WrongPassword

            } catch (e: Exception) {

                return CloudLoginOutcome.ServerBlocked
            }

        val document =
            try {

                FirebaseFirestore
                    .getInstance(auth.app)
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .get()
                    .await()

            } catch (e: FirebaseFirestoreException) {

                try {
                    auth.signOut()
                } catch (_: Exception) {
                }

                return if (
                    e.code ==
                    FirebaseFirestoreException.Code.UNAVAILABLE
                ) {
                    CloudLoginOutcome.NoInternet
                } else {
                    // Most commonly PERMISSION_DENIED - the
                    // Firestore security rules blocked the read.
                    CloudLoginOutcome.ServerBlocked
                }

            } catch (e: Exception) {

                try {
                    auth.signOut()
                } catch (_: Exception) {
                }

                return CloudLoginOutcome.NoInternet
            }

        if (
            !document.exists() ||
            document.getString("role") != role.name
        ) {

            auth.signOut()

            return CloudLoginOutcome.NotFound
        }

        val status =
            try {

                AccountStatus.valueOf(
                    document.getString("status") ?: ""
                )

            } catch (_: Exception) {

                AccountStatus.PENDING
            }

        return CloudLoginOutcome.Success(
            DemoUser(
                id = document.getString("id") ?: uid,
                username =
                    document.getString("username")
                        ?: username.trim(),
                password = "",
                role = role,
                status = status,
                coachingId = document.getString("coachingId"),
                displayName =
                    document.getString("displayName")
                        ?: username.trim()
            )
        )
    }


    /**
     * Status stored in the cloud for the currently signed-in role
     * user, or null when there is no session / no profile /
     * the request fails.
     */
    suspend fun currentCloudStatus(): AccountStatus? {

        val auth =
            secondaryAuth
                ?: return null

        val uid =
            auth.currentUser?.uid
                ?: return null

        return try {

            val document =
                FirebaseFirestore
                    .getInstance(auth.app)
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .get()
                    .await()

            if (!document.exists()) {

                null

            } else {

                AccountStatus.valueOf(
                    document.getString("status") ?: ""
                )
            }

        } catch (_: Exception) {

            null
        }
    }


    /**
     * --------------------------------------------------------
     * PROVISION A NEW STAFF ACCOUNT
     * --------------------------------------------------------
     *
     * Called by the principal (coaching admins) or a coaching
     * admin (teachers) at the moment the plain password is typed.
     *
     * A THIRD Firebase app is used to create the Auth account so
     * that the creator's own session is never replaced. users/{uid}
     * is then written with the creator's session.
     * --------------------------------------------------------
     */
    private fun getProvisionAuth(
        context: Context
    ): FirebaseAuth {

        val app =
            try {

                FirebaseApp.getInstance(
                    PROVISION_APP_NAME
                )

            } catch (_: Exception) {

                FirebaseApp.initializeApp(
                    context.applicationContext,
                    FirebaseApp.getInstance().options,
                    PROVISION_APP_NAME
                )!!
            }

        return FirebaseAuth.getInstance(app)
    }


    suspend fun provisionAccount(
        context: Context,
        user: DemoUser,
        plainPassword: String
    ): ProvisionResult {

        if (
            user.username.trim().isBlank() ||
            plainPassword.isBlank()
        ) {
            return ProvisionResult.FAILED
        }

        val auth =
            getProvisionAuth(context)

        val email =
            usernameToFirebaseEmail(user.username)

        /*
         * Set only if the users/{uid} write below actually fails.
         * The Auth account can still exist and be usable (uid !=
         * null) even when this happens, so it is checked after -
         * it must not be silently treated as full success.
         */
        var profileFailure = false

        val uid: String? =
            try {

                val createdUid =
                    try {

                        auth
                            .createUserWithEmailAndPassword(
                                email,
                                plainPassword
                            )
                            .await()
                            .user
                            ?.uid

                    } catch (exception: Exception) {

                        val alreadyExists =
                            exception is
                                    FirebaseAuthUserCollisionException ||
                                    exception.message
                                        ?.contains(
                                            "already in use",
                                            ignoreCase = true
                                        ) == true

                        if (!alreadyExists) {

                            throw exception
                        }

                        /*
                         * Account exists already (for example the
                         * person logged in once before). If the
                         * password matches we can reuse it.
                         */
                        try {

                            auth
                                .signInWithEmailAndPassword(
                                    email,
                                    plainPassword
                                )
                                .await()
                                .user
                                ?.uid

                        } catch (_: Exception) {

                            null
                        }
                    }

                /*
                 * IMPORTANT:
                 *
                 * The users/{uid} document MUST be written here,
                 * while this account's own session (auth) is still
                 * signed in - "users/{uid}" can normally only be
                 * written by that same uid under the security
                 * rules. Writing it AFTER auth.signOut() (as this
                 * used to do, down in the finally block below)
                 * means the write happens under the CREATOR's own
                 * session instead, which the rules reject - and
                 * because Firestore's offline cache reports the
                 * write as "successful" immediately regardless,
                 * that failure was invisible: the Auth account got
                 * created (so the password looked right), but the
                 * profile document never existed, so login from
                 * any other phone always failed with "no account
                 * found".
                 */
                if (createdUid != null) {

                    try {

                        syncUserProfile(
                            uid = createdUid,
                            user = user.copy(
                                password = ""
                            ),
                            firestore =
                                FirebaseFirestore.getInstance(
                                    auth.app
                                )
                        )

                    } catch (_: Exception) {

                        profileFailure = true
                    }
                }

                createdUid

            } catch (exception: FirebaseAuthWeakPasswordException) {

                return ProvisionResult.WEAK_PASSWORD

            } catch (exception: Exception) {

                return ProvisionResult.FAILED

            } finally {

                try {
                    auth.signOut()
                } catch (_: Exception) {
                }
            }

        if (uid == null) {

            return ProvisionResult.ACCOUNT_CONFLICT
        }

        return if (profileFailure) {

            ProvisionResult.PROFILE_FAILED

        } else {

            ProvisionResult.OK
        }
    }
}


enum class ProvisionResult {
    OK,
    WEAK_PASSWORD,
    ACCOUNT_CONFLICT,
    PROFILE_FAILED,
    FAILED
}