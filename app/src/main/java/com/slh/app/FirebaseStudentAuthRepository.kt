package com.slh.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * FirebaseStudentAuthRepository
 *
 * Student Firebase Authentication is handled through a SECONDARY
 * FirebaseApp so that creating/logging-in a student does not replace
 * the currently logged-in Admin/Teacher/Principal Admin Firebase session.
 *
 * IMPORTANT:
 * - Student passwords are never stored in Firestore.
 * - The password sent here is the ORIGINAL password entered during
 *   student registration.
 * - Student username is converted into a deterministic Firebase email.
 */
object FirebaseStudentAuthRepository {

    private const val SECONDARY_APP_NAME = "slh_student_auth"

    /*
     * Best-effort mirror of users/{uid}, written purely so the
     * Firebase console shows students separately from staff instead
     * of all mixed together in "users". "users/{uid}" itself stays
     * untouched - it is still what login and security rules use.
     */
    private const val STUDENTS_MIRROR_COLLECTION = "studentLogins"

    /**
     * Returns the secondary FirebaseApp used only for student
     * authentication operations.
     */
    private fun getSecondaryFirebaseApp(
        context: Context
    ): FirebaseApp {

        return try {

            FirebaseApp.getInstance(
                SECONDARY_APP_NAME
            )

        } catch (_: IllegalStateException) {

            FirebaseApp.initializeApp(
                context,
                FirebaseApp.getInstance().options,
                SECONDARY_APP_NAME
            )
                ?: throw IllegalStateException(
                    "Unable to initialize secondary Firebase app."
                )
        }
    }


    /**
     * Returns FirebaseAuth attached to the secondary FirebaseApp.
     */
    private fun getSecondaryAuth(
        context: Context
    ): FirebaseAuth {

        return FirebaseAuth.getInstance(
            getSecondaryFirebaseApp(context)
        )
    }


    /**
     * Converts an SLH student username into a deterministic
     * Firebase Authentication email address.
     *
     * Example:
     *
     * student01
     *      ↓
     * 73747564656e743031@students.slh.app
     *
     * Hex encoding prevents collisions caused by special characters
     * or unusual usernames.
     */
    fun usernameToFirebaseEmail(
        username: String
    ): String {

        val normalized =
            username
                .trim()
                .lowercase(Locale.US)

        require(normalized.isNotBlank()) {
            "Username cannot be blank."
        }

        val hex =
            normalized
                .toByteArray(StandardCharsets.UTF_8)
                .joinToString("") {
                    "%02x".format(
                        it.toInt() and 0xFF
                    )
                }

        return "$hex@students.slh.app"
    }


    /**
     * Creates the Firebase Authentication account for a new student.
     *
     * If the account already exists, this method treats that condition
     * as non-fatal so that the existing local SLH student record is not
     * damaged.
     */
    suspend fun createStudentAccount(
        context: Context,
        username: String,
        password: String
    ) {

        require(username.trim().isNotBlank()) {
            "Username cannot be blank."
        }

        require(password.isNotBlank()) {
            "Password cannot be blank."
        }

        val email =
            usernameToFirebaseEmail(username)

        val auth =
            getSecondaryAuth(context)

        try {

            auth.createUserWithEmailAndPassword(
                email,
                password
            ).await()

        } catch (exception: Exception) {

            val message =
                exception.message
                    ?.lowercase(Locale.US)
                    ?: ""

            val errorCode =
                when {
                    message.contains(
                        "email address is already in use"
                    ) -> "ERROR_EMAIL_ALREADY_IN_USE"

                    message.contains(
                        "already exists"
                    ) -> "ERROR_EMAIL_ALREADY_IN_USE"

                    else -> ""
                }

            if (
                errorCode !=
                "ERROR_EMAIL_ALREADY_IN_USE"
            ) {
                throw exception
            }
        } finally {

            /*
             * Very important:
             *
             * The secondary student Auth must never remain signed in.
             * Otherwise later Firebase operations could accidentally
             * use the student's session.
             */
            try {
                auth.signOut()
            } catch (_: Exception) {
            }
        }
    }


    /**
     * Firebase Student Login.
     *
     * Returns true when Firebase Authentication succeeds.
     *
     * The secondary Auth instance is used so the main Admin/Teacher/
     * Principal Admin Firebase session remains untouched.
     */
    suspend fun authenticateStudent(
        context: Context,
        username: String,
        password: String,
        keepSignedIn: Boolean = false
    ): Boolean {

        if (
            username.trim().isBlank() ||
            password.isBlank()
        ) {
            return false
        }

        val email =
            usernameToFirebaseEmail(username)

        val auth =
            getSecondaryAuth(context)

        return try {

            auth.signInWithEmailAndPassword(
                email,
                password
            ).await()

            true

        } catch (_: Exception) {

            false

        } finally {

            /*
             * Pure "does this account exist" checks sign out again.
             * The real student login passes keepSignedIn = true so
             * Firestore requests carry the student's token; the
             * session is ended when the login screen is shown again
             * (see SLHFirebase.signOutAll).
             */
            if (!keepSignedIn) {

                try {
                    auth.signOut()
                } catch (_: Exception) {
                }
            }
        }
    }


    /**
     * Login on a phone that has no local copy of the student.
     * Firebase Authentication checks the password; the student
     * record comes from Firestore (matched by the account email).
     * The student session stays signed in on success.
     */
    suspend fun cloudLogin(
        context: Context,
        username: String,
        password: String
    ): CloudLoginOutcome<Student> {

        if (
            username.trim().isBlank() ||
            password.isBlank()
        ) {
            return CloudLoginOutcome.WrongPassword
        }

        val email =
            usernameToFirebaseEmail(username)

        val auth =
            getSecondaryAuth(context)

        try {

            auth
                .signInWithEmailAndPassword(
                    email,
                    password
                )
                .await()

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

        val student =
            try {

                StudentFirestoreRepository
                    .findStudentByAuthEmail(
                        email
                    )

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

        if (student == null) {

            try {
                FirebaseAuth
                    .getInstance(
                        getSecondaryFirebaseApp(context)
                    )
                    .signOut()
            } catch (_: Exception) {
            }

            return CloudLoginOutcome.NotFound
        }

        return CloudLoginOutcome.Success(student)
    }


    /**
     * Writes users/{uid} for the signed-in student so the
     * security rules can map the Firebase account to the
     * students/{id} document. Role and coaching are NOT trusted
     * from this document - the rules re-check them against the
     * student document created by the admin.
     */
    suspend fun syncStudentProfile(
        context: Context,
        student: Student
    ) {

        val app =
            getSecondaryFirebaseApp(context)

        val uid =
            FirebaseAuth
                .getInstance(app)
                .currentUser
                ?.uid
                ?: return

        val data =
            hashMapOf<String, Any?>(
                "id" to student.id,
                "uid" to uid,
                "username" to student.username.trim(),
                "role" to UserRole.STUDENT.name,
                "status" to student.status.name,
                "coachingId" to student.coachingId,
                "displayName" to student.name,
                "studentDocId" to student.id
            )

        val firestore =
            com.google.firebase.firestore.FirebaseFirestore
                .getInstance(app)

        firestore
            .collection("users")
            .document(uid)
            .set(data)
            .await()

        // Best-effort mirror - never allowed to fail the real write.
        try {

            firestore
                .collection(STUDENTS_MIRROR_COLLECTION)
                .document(uid)
                .set(data)
                .await()

        } catch (e: Exception) {

            SLHFirebase.logSyncFailure(
                "mirror write $STUDENTS_MIRROR_COLLECTION/$uid",
                e
            )
        }
    }


    /**
     * Checks whether the Firebase Authentication account exists.
     *
     * This method intentionally does NOT expose any password.
     */
    suspend fun checkStudentAccountExists(
        context: Context,
        username: String,
        password: String
    ): Boolean {

        return authenticateStudent(
            context = context,
            username = username,
            password = password
        )
    }

    /**
     * Admin password reset (best-effort for Firebase Auth).
     *
     * Client SDK cannot force-change another user's password
     * without the old password or Admin SDK. Local StudentStore
     * hash is the source of truth for login on synced devices.
     * This method tries createUser (no-op if account already
     * exists) so brand-new cloud accounts still get a password.
     *
     * @return null always (local reset is already applied by caller).
     */
    suspend fun adminResetStudentPasswordBestEffort(
        context: Context,
        username: String,
        newPassword: String
    ): String? {
        if (username.isBlank() || newPassword.length < 6) {
            return null
        }
        return try {
            // If no Firebase Auth user exists yet, create with the
            // new password. If it already exists, create fails
            // silently (email in use) — local hash still works.
            createStudentAccount(
                context = context,
                username = username,
                password = newPassword
            )
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Changes the student's Firebase Auth password.
     * Requires the current (old) password for re-authentication.
     * On success, local Student.password should also be updated
     * (hashed) so Admin student management stays in sync.
     *
     * @return null on success, or an error message string on failure.
     */
    suspend fun changeStudentPassword(
        context: Context,
        username: String,
        currentPassword: String,
        newPassword: String
    ): String? {

        if (username.trim().isBlank()) {
            return "Username is missing."
        }
        if (currentPassword.isBlank()) {
            return "Current password is required."
        }
        if (newPassword.length < 6) {
            return "New password must be at least 6 characters."
        }
        if (currentPassword == newPassword) {
            return "New password must be different from current password."
        }

        val email = usernameToFirebaseEmail(username)
        val auth = getSecondaryAuth(context)

        return try {
            val result = auth
                .signInWithEmailAndPassword(email, currentPassword)
                .await()

            val user = result.user
                ?: return "Could not verify account."

            user.updatePassword(newPassword).await()

            // Keep secondary session clean for other checks.
            try {
                auth.signOut()
            } catch (_: Exception) {
            }

            null
        } catch (e: FirebaseNetworkException) {
            "No internet connection. Try again later."
        } catch (e: FirebaseAuthInvalidUserException) {
            "Account not found on cloud."
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            "Current password is incorrect."
        } catch (e: Exception) {
            e.message?.takeIf { it.isNotBlank() }
                ?: "Password change failed."
        }
    }
}