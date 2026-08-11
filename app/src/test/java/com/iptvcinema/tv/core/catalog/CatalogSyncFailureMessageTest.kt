package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSyncFailureMessageTest {
    @Test
    fun everyFailureReasonMapsToAUserFacingLocalizedResource() {
        val expected = mapOf(
            CatalogSyncFailureReason.NO_SOURCE to R.string.refresh_source_missing,
            CatalogSyncFailureReason.DEMO_UNAVAILABLE to R.string.refresh_demo_unavailable,
            CatalogSyncFailureReason.CREDENTIALS_MISSING to R.string.refresh_credentials_missing,
            CatalogSyncFailureReason.AUTHENTICATION to R.string.refresh_authentication_failed,
            CatalogSyncFailureReason.NETWORK to R.string.refresh_network_failed,
            CatalogSyncFailureReason.PROVIDER to R.string.refresh_provider_failed,
            CatalogSyncFailureReason.UNKNOWN to R.string.refresh_failed,
        )

        assertEquals(expected, CatalogSyncFailureReason.entries.associateWith(::failureMessageResource))
    }
}
