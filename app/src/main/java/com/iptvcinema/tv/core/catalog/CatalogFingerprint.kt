package com.iptvcinema.tv.core.catalog

import java.security.MessageDigest

object CatalogFingerprint {
    fun of(items: Iterable<Any?>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        items.forEach { item ->
            val bytes = item.toString().toByteArray(Charsets.UTF_8)
            digest.update(bytes.size.toString().toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(bytes)
            digest.update(0)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
