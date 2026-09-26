package com.slh.app

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class UserRole(
    val title: String,
    val subtitle: String
) {
    STUDENT(
        "Student",
        "Student Login"
    ),

    TEACHER(
        "Teacher",
        "Teacher Login"
    ),

    COACHING_ADMIN(
        "Admin",
        "Coaching Admin"
    ),

    PRINCIPAL_ADMIN(
        "Principal Admin",
        "Principal Admin"
    )
}

enum class AccountStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}

data class DemoUser(
    val id: String,
    val username: String,
    val password: String,
    val role: UserRole,
    val status: AccountStatus,
    val coachingId: String? = null,
    val displayName: String
)

data class CoachingProfile(
    val id: String,
    val name: String,
    val shortName: String,
    val tagline: String,
    val logoUri: String? = null,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val attendanceRadiusMeters: Float = 100f
)

data class MainLoginBranding(
    val name: String,
    val shortName: String,
    val tagline: String,
    val logoUri: String? = null
)

object MainLoginBrandingStore {

    var branding by mutableStateOf(
        MainLoginBranding(
            name = "SLH",
            shortName = "SLH",
            tagline = "Learn Today. Succeed Tomorrow.",
            logoUri = null
        )
    )
        private set

    fun update(
        newBranding: MainLoginBranding
    ) {
        branding = newBranding
    }
}

object DemoData {

    // No built-in/demo accounts or coaching profiles are shipped with the app.
    val coachingProfiles = emptyList<CoachingProfile>()
    val users = emptyList<DemoUser>()

    fun isSeedUser(user: DemoUser): Boolean = false

    fun isLegacyPrincipalSeed(user: DemoUser): Boolean {
        return user.id == "P001" &&
                user.username.equals("developeradmin", ignoreCase = true) &&
                !PasswordHasher.isHashed(user.password)
    }

    fun findCoaching(
        coachingId: String?
    ): CoachingProfile? {

        if (coachingId == null) {
            return null
        }

        return CoachingProfileStore.get(
            coachingId
        )
    }
}

object CoachingProfileStore {

    private val profileStates =
        mutableMapOf<
                String,
                MutableState<CoachingProfile>
                >()

    private val profiles =
        mutableMapOf<String, CoachingProfile>()

    var version by mutableStateOf(0)
        private set

    private val activeFlags =
        mutableMapOf<String, Boolean>()

    private fun stateFor(
        coachingId: String
    ): MutableState<CoachingProfile> {

        return profileStates.getOrPut(
            coachingId
        ) {

            mutableStateOf(
                profiles[coachingId]
                    ?: CoachingProfile(
                        id = coachingId,
                        name = "My Coaching",
                        shortName = "MC",
                        tagline = ""
                    )
            )
        }
    }

    /**
     * Loads coaching profiles from encrypted
     * local storage.
     *
     * If no saved data exists, demo profiles
     * are loaded and persisted.
     */
    fun initialize() {

        val saved =
            SLHLocalStorage.loadCoachingProfiles()

        if (saved != null) {

            profiles.clear()

            saved.forEach { profile ->

                profiles[
                    profile.id
                ] = profile

                stateFor(
                    profile.id
                ).value = profile
            }

        } else {

            profiles.clear()

            DemoData.coachingProfiles.forEach {
                    profile ->

                profiles[
                    profile.id
                ] = profile

                stateFor(
                    profile.id
                ).value = profile
            }

            persist()
        }

        version++
    }

    /**
     * Persists all coaching profiles.
     */
    private fun persist() {

        SLHLocalStorage.saveCoachingProfiles(
            profiles.values.toList()
        )
    }

    fun get(
        coachingId: String
    ): CoachingProfile {

        return stateFor(
            coachingId
        ).value
    }

    /**
     * Updates an existing coaching profile.
     */
    fun update(
        profile: CoachingProfile
    ) {

        profiles[
            profile.id
        ] = profile

        stateFor(
            profile.id
        ).value = profile

        version++

        persist()
    }

    /**
     * Returns all coaching profiles.
     */
    fun getAll():
            List<CoachingProfile> {

        version

        return profiles.values
            .sortedBy {
                it.name.lowercase()
            }
    }

    /**
     * Adds a new coaching profile.
     */
    fun add(
        profile: CoachingProfile
    ) {

        if (
            profiles.containsKey(
                profile.id
            )
        ) {
            return
        }

        profiles[
            profile.id
        ] = profile

        stateFor(
            profile.id
        ).value = profile

        version++

        persist()
    }

    /**
     * Checks whether a coaching is active.
     */
    fun isActive(
        coachingId: String
    ): Boolean {

        return activeFlags[
            coachingId
        ] ?: true
    }

    /**
     * Changes coaching active state.
     */
    fun setActive(
        coachingId: String,
        active: Boolean
    ) {

        activeFlags[
            coachingId
        ] = active

        version++
    }

    /**
     * Deletes a coaching profile.
     */
    fun delete(
        coachingId: String
    ): Boolean {

        val removed =
            profiles.remove(
                coachingId
            ) != null

        profileStates.remove(
            coachingId
        )

        activeFlags.remove(
            coachingId
        )

        if (removed) {

            version++

            persist()
        }

        return removed
    }

    /**
     * Clears all coaching profiles.
     */
    fun clear() {

        profiles.clear()
        profileStates.clear()
        activeFlags.clear()

        version++

        persist()
    }
}

object UserAccountStore {

    private val userState:
            MutableState<List<DemoUser>> =
        mutableStateOf(
            emptyList()
        )

    var version by mutableStateOf(0)
        private set

    fun initialize() {

        val savedUsers = SLHLocalStorage.loadUserAccounts()

        // No built-in/demo accounts are created in any build.
        // Remove old seeded accounts left by earlier installations.
        val cleaned = (savedUsers ?: emptyList()).filterNot { user ->
            DemoData.isSeedUser(user) ||
                    DemoData.isLegacyPrincipalSeed(user)
        }

        userState.value = cleaned

        if (savedUsers == null || cleaned.size != savedUsers.size) {
            SLHLocalStorage.saveUserAccounts(cleaned)
        }

        version++
    }

    val users: List<DemoUser>
        get() = userState.value

    private fun persist() {

        SLHLocalStorage.saveUserAccounts(
            userState.value
        )
    }

    fun getAll(): List<DemoUser> {

        version

        return userState.value.toList()
    }

    fun getById(
        id: String
    ): DemoUser? {

        version

        return userState.value.find {
            it.id == id
        }
    }

    fun findByUsername(
        username: String
    ): DemoUser? {

        version

        return userState.value.find {
            it.username.equals(
                username,
                ignoreCase = true
            )
        }
    }

    fun coachingAdmins(): List<DemoUser> {

        version

        return userState.value
            .filter {
                it.role ==
                        UserRole.COACHING_ADMIN
            }
            .sortedBy {
                it.displayName.lowercase()
            }
    }

    fun updateStatus(
        userId: String,
        status: AccountStatus
    ) {

        val index =
            userState.value.indexOfFirst {
                it.id == userId
            }

        if (index >= 0) {

            val updated =
                userState.value.toMutableList()

            updated[index] =
                updated[index].copy(
                    status = status
                )

            userState.value =
                updated

            version++

            persist()

            AccountCloudSync.pushProfile(
                updated[index]
            )
        }
    }

    fun add(
        user: DemoUser
    ) {

        val duplicate =
            userState.value.any {
                it.id == user.id ||
                        it.username.equals(
                            user.username,
                            ignoreCase = true
                        )
            }

        if (!duplicate) {

            userState.value =
                userState.value + user

            version++

            persist()
        }
    }

    fun update(
        user: DemoUser
    ): Boolean {

        val index =
            userState.value.indexOfFirst {
                it.id == user.id
            }

        if (index < 0) {
            return false
        }

        val duplicateUsername =
            userState.value.any {
                it.id != user.id &&
                        it.username.equals(
                            user.username,
                            ignoreCase = true
                        )
            }

        if (duplicateUsername) {
            return false
        }

        val updated =
            userState.value.toMutableList()

        updated[index] =
            user

        userState.value =
            updated

        version++

        persist()

        AccountCloudSync.pushProfile(
            user
        )

        return true
    }

    fun delete(
        userId: String
    ): Boolean {

        val target =
            userState.value.firstOrNull {
                it.id == userId
            }

        val oldSize =
            userState.value.size

        userState.value =
            userState.value.filterNot {
                it.id == userId
            }

        val deleted =
            userState.value.size < oldSize

        if (deleted) {

            version++

            persist()

            target?.let {
                AccountCloudSync.deleteProfile(it)
            }
        }

        return deleted
    }

    fun usernameExists(
        username: String,
        exceptUserId: String? = null
    ): Boolean {

        return userState.value.any {

            it.id != exceptUserId &&
                    it.username.equals(
                        username.trim(),
                        ignoreCase = true
                    )
        }
    }

    fun nextId(): String {

        var number =
            userState.value.size + 1

        var id =
            "U" +
                    String.format(
                        "%03d",
                        number
                    )

        while (
            userState.value.any {
                it.id == id
            }
        ) {

            number++

            id =
                "U" +
                        String.format(
                            "%03d",
                            number
                        )
        }

        return id
    }

    fun clear() {

        userState.value =
            emptyList()

        version++

        persist()
    }
}

object PlatformSettingsStore {

    var allowNewRegistrations by
    mutableStateOf(true)
        private set

    var requireAdminApproval by
    mutableStateOf(true)
        private set

    var maintenanceMode by
    mutableStateOf(false)
        private set

    fun updateAllowNewRegistrations(
        value: Boolean
    ) {

        allowNewRegistrations =
            value
    }

    fun updateRequireAdminApproval(
        value: Boolean
    ) {

        requireAdminApproval =
            value
    }

    fun updateMaintenanceMode(
        value: Boolean
    ) {

        maintenanceMode =
            value
    }
}