package com.iptvcinema.tv.core.util

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDisplayNameJsonTest {
    @Test
    fun jsonMetadataName_hasNoQuotes() {
        val metadata = mapOf("full_name" to JsonPrimitive("Ahmed Afifi"))

        assertEquals("Ahmed Afifi", AccountDisplayNameResolver.resolve(email = null, metadata = metadata))
    }
}
