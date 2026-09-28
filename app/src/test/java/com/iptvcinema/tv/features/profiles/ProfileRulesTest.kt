package com.iptvcinema.tv.features.profiles

import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRulesTest {
    private val existing = listOf(
        UserProfile(id = "1", name = "Main", type = ProfileType.MAIN, avatarInitial = "M"),
        UserProfile(id = "2", name = "Kids", type = ProfileType.KIDS, avatarInitial = "K"),
    )

    @Test
    fun blankName_isRejected() {
        assertEquals(ProfileNameError.Empty, ProfileRules.validateName("   ", existing, editingId = null))
    }

    @Test
    fun tooLongName_isRejected() {
        assertEquals(ProfileNameError.TooLong, ProfileRules.validateName("A".repeat(21), existing, editingId = null))
    }

    @Test
    fun duplicateName_ignoringCaseAndSpaces_isRejected() {
        assertEquals(ProfileNameError.Duplicate, ProfileRules.validateName("  kids ", existing, editingId = null))
    }

    @Test
    fun renamingAProfileToItsOwnName_isAllowed() {
        assertEquals(null, ProfileRules.validateName("Kids", existing, editingId = "2"))
    }

    @Test
    fun validName_isAccepted() {
        assertEquals(null, ProfileRules.validateName("Grandma", existing, editingId = null))
    }

    @Test
    fun canAddProfile_untilLimit() {
        assertTrue(ProfileRules.canAddProfile(existing))
        val full = (1..ProfileRules.MAX_PROFILES).map {
            UserProfile(id = "$it", name = "P$it", type = ProfileType.FAMILY, avatarInitial = "P")
        }
        assertFalse(ProfileRules.canAddProfile(full))
    }

    @Test
    fun lastProfile_cannotBeDeleted() {
        assertFalse(ProfileRules.canDelete(existing.take(1), profileId = "1", activeProfileId = null))
    }

    @Test
    fun activeProfile_cannotBeDeleted() {
        assertFalse(ProfileRules.canDelete(existing, profileId = "1", activeProfileId = "1"))
        assertTrue(ProfileRules.canDelete(existing, profileId = "2", activeProfileId = "1"))
    }
}
