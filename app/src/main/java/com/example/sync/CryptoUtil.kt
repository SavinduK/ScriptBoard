package com.example.sync

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {

    private val secureRandom = SecureRandom()

    fun generateNonce(bytesCount: Int = 16): String {
        val bytes = ByteArray(bytesCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun computeHmac(secret: String, data: String): String {
        return try {
            val hmacKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(hmacKey)
            val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    fun verifyHmac(secret: String, data: String, expectedHmac: String): Boolean {
        if (secret.isBlank() || data.isBlank() || expectedHmac.isBlank()) return false
        val computed = computeHmac(secret, data)
        return computed.equals(expectedHmac, ignoreCase = true)
    }
}
