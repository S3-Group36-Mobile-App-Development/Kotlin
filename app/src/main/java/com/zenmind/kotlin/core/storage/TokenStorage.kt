package com.zenmind.kotlin.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TokenStorage(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        "zenmind_auth_storage",
        Context.MODE_PRIVATE
    )

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val KEY_ALIAS = "zenmind_auth_key"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    fun saveTokens(
        accessToken: String,
        refreshToken: String
    ) {
        preferences.edit()
            .putString(
                ACCESS_TOKEN_KEY,
                encrypt(accessToken)
            )
            .putString(
                REFRESH_TOKEN_KEY,
                encrypt(refreshToken)
            )
            .apply()
    }

    fun getAccessToken(): String? {
        val encryptedToken =
            preferences.getString(ACCESS_TOKEN_KEY, null)
                ?: return null

        return decrypt(encryptedToken)
    }

    fun getRefreshToken(): String? {
        val encryptedToken =
            preferences.getString(REFRESH_TOKEN_KEY, null)
                ?: return null

        return decrypt(encryptedToken)
    }

    fun hasSession(): Boolean {
        return !getAccessToken().isNullOrBlank() &&
                !getRefreshToken().isNullOrBlank()
    }

    fun clearTokens() {
        preferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {

        val existingKey = keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    private fun encrypt(value: String): String {

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encryptedBytes = cipher.doFinal(
            value.toByteArray(Charsets.UTF_8)
        )

        val iv = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        val encrypted = Base64.encodeToString(
            encryptedBytes,
            Base64.NO_WRAP
        )

        return "$iv:$encrypted"
    }

    private fun decrypt(value: String): String? {

        return try {

            val parts = value.split(
                ":",
                limit = 2
            )

            if (parts.size != 2) {
                return null
            }

            val iv = Base64.decode(
                parts[0],
                Base64.NO_WRAP
            )

            val encryptedBytes = Base64.decode(
                parts[1],
                Base64.NO_WRAP
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            val spec = GCMParameterSpec(
                128,
                iv
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                spec
            )

            val decrypted = cipher.doFinal(
                encryptedBytes
            )

            String(
                decrypted,
                Charsets.UTF_8
            )

        } catch (exception: Exception) {
            null
        }
    }
}