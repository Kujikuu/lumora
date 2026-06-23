package com.iptvcinema.tv.core.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStrings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun get(@StringRes id: Int, vararg formatArgs: Any): String =
        context.getString(id, *formatArgs)

    fun getQuantity(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String =
        context.resources.getQuantityString(id, quantity, *formatArgs)
}
