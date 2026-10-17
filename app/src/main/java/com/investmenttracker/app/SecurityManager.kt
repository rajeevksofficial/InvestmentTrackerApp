package com.investmenttracker.app

import android.content.Context
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityManager {

    private const val PREFS_AUTH = "investment_tracker_auth"
    private const val PREF_PIN_HASH = "master_pin_hash"
    private const val PREF_PIN_SALT = "master_pin_salt"
    private const val PREF_IS_PIN_SET = "is_pin_set"

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_LENGTH = 256

    fun isMasterPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_IS_PIN_SET, false)
    }

    fun setMasterPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        val saltBase64 = SafeBase64.encode(salt)

        context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_PIN_HASH, hash)
            .putString(PREF_PIN_SALT, saltBase64)
            .putBoolean(PREF_IS_PIN_SET, true)
            .apply()
    }

    fun verifyMasterPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(PREF_PIN_HASH, null) ?: return false
        val savedSaltBase64 = prefs.getString(PREF_PIN_SALT, null) ?: return false

        val salt = SafeBase64.decode(savedSaltBase64)
        val calculatedHash = hashPin(pin, salt)

        return slowEquals(savedHash, calculatedHash)
    }

    fun changeMasterPin(context: Context, currentPin: String, newPin: String): Boolean {
        if (!verifyMasterPin(context, currentPin)) {
            return false
        }
        setMasterPin(context, newPin)
        return true
    }

    fun resetMasterPin(context: Context) {
        context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    internal fun hashPinForTest(pin: String, salt: ByteArray): String = hashPin(pin, salt)
    internal fun slowEqualsForTest(a: String, b: String): Boolean = slowEquals(a, b)

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return SafeBase64.encode(hashBytes)
    }

    private fun slowEquals(a: String, b: String): Boolean {
        var diff = a.length xor b.length
        val minLen = minOf(a.length, b.length)
        for (i in 0 until minLen) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
