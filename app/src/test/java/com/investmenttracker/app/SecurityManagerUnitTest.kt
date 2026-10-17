package com.investmenttracker.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class SecurityManagerUnitTest {

    @Test
    fun testSafeBase64EncodingAndDecoding() {
        val original = "InvestTrack-Security-MasterPin-2026!@#"
        val bytes = original.toByteArray(Charsets.UTF_8)
        val encoded = SafeBase64.encode(bytes)
        val decodedBytes = SafeBase64.decode(encoded)
        val result = String(decodedBytes, Charsets.UTF_8)

        assertEquals(original, result)
    }

    @Test
    fun testPinHashingDeterministicWithSameSalt() {
        val pin = "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val hash1 = SecurityManager.hashPinForTest(pin, salt)
        val hash2 = SecurityManager.hashPinForTest(pin, salt)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testPinHashingDifferentForDifferentPins() {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val hash1 = SecurityManager.hashPinForTest("1234", salt)
        val hash2 = SecurityManager.hashPinForTest("5678", salt)

        assertFalse(hash1 == hash2)
    }

    @Test
    fun testSlowEquals() {
        assertTrue(SecurityManager.slowEqualsForTest("correct-hash-string", "correct-hash-string"))
        assertFalse(SecurityManager.slowEqualsForTest("correct-hash-string", "wrong-hash-string"))
        assertFalse(SecurityManager.slowEqualsForTest("correct-hash-string", "correct-hash"))
    }
}
