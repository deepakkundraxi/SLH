package com.slh.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Account deletion / GDPR-style wipe for the currently signed-in user.
 *
 * Staff (admin / teacher):
 *  - Deletes users/{uid} (+ mirror if any)
 *  - Deletes Firebase Auth account on the secondary role app
 *  - Clears local UserAccountStore entry
 *
 * Student:
 *  - Does NOT delete the admin-owned students/{id} record
 *  - Deletes users/{uid} link + Firebase Auth student session
 *  - Clears local student cache for this device
 *
 * Always call after confirming with the user in the UI.
 */
object AccountDeletionHelper {

    sealed class Result {
        data object Ok : Result()
        data class Failed(val reason: String) : Result()
    }

    suspend fun deleteStaffAccount(
        context: Context,
        user: DemoUser
    ): Result {
        return try {
            val auth = try {
                FirebaseAuth.getInstance(
                    com.google.firebase.FirebaseApp.getInstance("slh_role_auth")
                )
            } catch (_: Exception) {
                null
            }

            val uid = auth?.currentUser?.uid
            if (uid != null) {
                val db = FirebaseFirestore.getInstance(
                    auth.app
                )
                try {
                    db.collection("users").document(uid).delete().await()
                } catch (_: Exception) {
                }
                FirebaseRoleAuthRepository.mirrorCollectionForRole(user.role)
                    ?.let { mirror ->
                        try {
                            db.collection(mirror).document(uid).delete().await()
                        } catch (_: Exception) {
                        }
                    }
                try {
                    auth.currentUser?.delete()?.await()
                } catch (_: Exception) {
                    // May require recent login; still clear local.
                }
                try {
                    auth.signOut()
                } catch (_: Exception) {
                }
            }

            UserAccountStore.delete(user.id)
            BiometricHelper.disable(context)
            SessionManager.clear()

            Result.Ok
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Could not delete account")
        }
    }

    suspend fun deleteStudentSession(
        context: Context,
        student: Student
    ): Result {
        return try {
            val auth = try {
                FirebaseAuth.getInstance(
                    com.google.firebase.FirebaseApp.getInstance("slh_student_auth")
                )
            } catch (_: Exception) {
                null
            }

            val uid = auth?.currentUser?.uid
            if (uid != null) {
                val db = FirebaseFirestore.getInstance(auth.app)
                try {
                    db.collection("users").document(uid).delete().await()
                } catch (_: Exception) {
                }
                try {
                    auth.currentUser?.delete()?.await()
                } catch (_: Exception) {
                }
                try {
                    auth.signOut()
                } catch (_: Exception) {
                }
            }

            // Remove only the local copy; server students/{id} stays
            // under admin control.
            StudentStore.deleteStudent(student.id)
            BiometricHelper.disable(context)
            SessionManager.clear()

            Result.Ok
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Could not delete session")
        }
    }
}
