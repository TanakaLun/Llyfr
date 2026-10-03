package io.github.tanakalun.llyfr.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class NoteCipher {

    private val getKey = getOrCreateKey()

    fun encrypt(plaintext: String, enabled: Boolean): String {
        if (plaintext.isEmpty() || !enabled) return plaintext
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(GCM_IV_SIZE + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, GCM_IV_SIZE)
        System.arraycopy(encrypted, 0, combined, GCM_IV_SIZE, encrypted.size)
        return PREFIX_V1 + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        val body = when {
            stored.startsWith(PREFIX_V1) -> stored.removePrefix(PREFIX_V1)
            stored.startsWith(PREFIX_V0) -> stored.removePrefix(PREFIX_V0)
            else -> stored
        }
        val combined = Base64.decode(body, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_SIZE)
        val encrypted = combined.copyOfRange(GCM_IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKey, GCMParameterSpec(GCM_TAG_SIZE, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun isEncrypted(stored: String): Boolean =
        stored.startsWith(PREFIX_V1) || stored.startsWith(PREFIX_V0)

    /**
     * 将明文规范化写入格式：若为明文直接返回；若为 v0 旧密文则重新加密为 v1；
     * 若解密失败返回 null 表示该字段已不可读。
     */
    fun normalizeStoredField(stored: String): String? {
        if (!isEncrypted(stored)) return stored
        return try {
            encrypt(decrypt(stored), enabled = true)
        } catch (_: Exception) {
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        keyStore.getEntry(KEY_ALIAS, null)?.let {
            return (it as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGen.generateKey()
    }

    companion object {
        const val PREFIX_V1 = "enc:v1:"
        const val PREFIX_V0 = "enc:v0:"

        private const val KEY_ALIAS = "llyfr_note_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
    }
}
