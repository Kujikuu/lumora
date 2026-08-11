package com.iptvcinema.tv.core.catalog

import com.iptvcinema.tv.core.util.AppStrings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogSyncMessageFormatter @Inject constructor(
    private val appStrings: AppStrings,
) {
    fun failure(result: CatalogSyncCoordinatorResult.Failed): String =
        appStrings.get(failureMessageResource(result.reason))
}
