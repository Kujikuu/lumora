package com.iptvcinema.tv.features.activation

import io.github.jan.supabase.exceptions.HttpRequestException
import java.io.IOException

internal fun Throwable.toActivationError(): ActivationError = when (this) {
    is IOException, is HttpRequestException -> ActivationError.Network
    else -> ActivationError.Server
}
