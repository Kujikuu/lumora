package com.iptvcinema.tv.core.parental

import com.iptvcinema.tv.core.model.ParentalControls
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinHasher @Inject constructor() {
    fun hashPin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256(salt + pin.toByteArray(Charsets.UTF_8))
        return salt.toHex() + ":" + hash.toHex()
    }

    fun verifyPin(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytes() ?: return false
        val expectedHash = parts[1]
        val actualHash = sha256(salt + pin.toByteArray(Charsets.UTF_8)).toHex()
        return actualHash == expectedHash
    }

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? = runCatching {
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }.getOrNull()
}

@Singleton
class ParentalSession @Inject constructor() {
    private var verifiedProfileId: String? = null
    private var verifiedAtMs: Long = 0L

    fun markVerified(profileId: String) {
        verifiedProfileId = profileId
        verifiedAtMs = System.currentTimeMillis()
    }

    fun clear() {
        verifiedProfileId = null
        verifiedAtMs = 0L
    }

    fun isVerified(profileId: String): Boolean {
        if (verifiedProfileId != profileId) return false
        val elapsed = System.currentTimeMillis() - verifiedAtMs
        return elapsed < SESSION_TIMEOUT_MS
    }

    companion object {
        private const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L
    }
}

@Singleton
class ParentalGate @Inject constructor(
    private val pinHasher: PinHasher,
    private val parentalSession: ParentalSession,
) {
    val pinEnabled: (ParentalControls) -> Boolean = { controls -> !controls.pinHash.isNullOrBlank() }

    fun verifyPin(controls: ParentalControls, pin: String): Boolean {
        val valid = pinHasher.verifyPin(pin, controls.pinHash)
        if (valid) {
            parentalSession.markVerified(controls.profileId)
        }
        return valid
    }

    fun isPinVerified(profileId: String): Boolean = parentalSession.isVerified(profileId)

    fun clearSession() = parentalSession.clear()

    fun filterCategoryNames(categories: List<String>, controls: ParentalControls): List<String> {
        val blocked = blockedCategorySet(controls)
        return categories.filter { name ->
            val normalized = name.trim()
            !blocked.any { blockedName -> normalized.equals(blockedName, ignoreCase = true) }
        }
    }

    fun isCategoryBlocked(categoryName: String, controls: ParentalControls): Boolean {
        val normalized = categoryName.trim()
        return blockedCategorySet(controls).any { blockedName ->
            normalized.equals(blockedName, ignoreCase = true) ||
                (controls.hideAdultCategories && isAdultCategory(normalized))
        }
    }

    fun requiresPinForSettings(controls: ParentalControls): Boolean =
        controls.lockPlaylistSettings && pinEnabled(controls)

    fun requiresPinForLiveCategories(controls: ParentalControls): Boolean =
        controls.lockLiveCategories && pinEnabled(controls)

    fun isRatingAllowed(contentRating: String?, controls: ParentalControls): Boolean =
        RatingPolicy.isAllowed(contentRating, controls.maxRating)

    fun isContentBlocked(
        categoryName: String?,
        contentRating: String?,
        controls: ParentalControls,
    ): Boolean =
        isCategoryBlocked(categoryName.orEmpty(), controls) ||
            !isRatingAllowed(contentRating, controls)

    private fun blockedCategorySet(controls: ParentalControls): Set<String> {
        val blocked = controls.blockedCategories.map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        if (controls.hideAdultCategories) {
            blocked.addAll(ADULT_CATEGORY_KEYWORDS)
        }
        return blocked
    }

    private fun isAdultCategory(name: String): Boolean =
        ADULT_CATEGORY_KEYWORDS.any { keyword -> name.contains(keyword, ignoreCase = true) }

    companion object {
        private val ADULT_CATEGORY_KEYWORDS = setOf("Adult", "XXX", "18+", "Porn", "Erotic")
    }
}
