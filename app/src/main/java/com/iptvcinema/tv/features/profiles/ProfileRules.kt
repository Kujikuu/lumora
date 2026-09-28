package com.iptvcinema.tv.features.profiles

import com.iptvcinema.tv.core.model.UserProfile

enum class ProfileNameError { Empty, TooLong, Duplicate }

object ProfileRules {
    const val MAX_PROFILES = 5
    const val MAX_NAME_LENGTH = 20

    fun validateName(raw: String, existing: List<UserProfile>, editingId: String?): ProfileNameError? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> ProfileNameError.Empty
            name.length > MAX_NAME_LENGTH -> ProfileNameError.TooLong
            existing.any { it.id != editingId && it.name.trim().equals(name, ignoreCase = true) } ->
                ProfileNameError.Duplicate
            else -> null
        }
    }

    fun canAddProfile(existing: List<UserProfile>): Boolean = existing.size < MAX_PROFILES

    /** The last profile and the one in use cannot be deleted. */
    fun canDelete(existing: List<UserProfile>, profileId: String, activeProfileId: String?): Boolean =
        existing.size > 1 && profileId != activeProfileId
}
