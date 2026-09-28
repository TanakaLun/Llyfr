package io.github.tanakalun.mynotes.data.crypto

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongBackupPasswordException(message: String) : Exception(message)

/**
 * 备份 v3 加密：用户口令 + PBKDF2WithHmacSHA256 派生密钥 + 随机 salt/IV。
 *
 * 文件布局：
 *   "MNBU"(4B) | version(1B)=3 | iterations(4B BE) | salt(16B) | iv(12B) | ciphertext+tag
 */
object BackupCrypto {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val MAGIC = "MNBU"
    private const val VERSION = 3
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private const val SALT_SIZE = 16
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256

    fun hasV3Header(data: ByteArray): Boolean {
        if (data.size < MAGIC.length) return false
        return String(data, 0, MAGIC.length, Charsets.UTF_8) == MAGIC
    }

    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt, ITERATIONS)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(data)

        val out = ByteBuffer.allocate(MAGIC.length + 1 + 4 + SALT_SIZE + IV_SIZE + ciphertext.size)
        out.put(MAGIC.toByteArray(Charsets.UTF_8))
        out.put(VERSION.toByte())
        out.putInt(ITERATIONS)
        out.put(salt)
        out.put(iv)
        out.put(ciphertext)
        return out.array()
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        val buf = ByteBuffer.wrap(data)
        val magic = ByteArray(MAGIC.length)
        buf.get(magic)
        if (String(magic, Charsets.UTF_8) != MAGIC) {
            throw WrongBackupPasswordException("Not a password-encrypted backup")
        }
        val version = buf.get().toInt()
        if (version != VERSION) {
            throw WrongBackupPasswordException("Unsupported backup version $version")
        }
        val iterations = buf.int
        val salt = ByteArray(SALT_SIZE).also { buf.get(it) }
        val iv = ByteArray(IV_SIZE).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }

        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return try {
            cipher.doFinal(ciphertext)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw WrongBackupPasswordException("Wrong password or corrupted backup")
        } catch (e: Exception) {
            throw WrongBackupPasswordException("Failed to decrypt backup")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun passwordStrength(password: CharArray): Int {
        if (password.isEmpty()) return 0
        var score = 1
        if (password.size >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        return score
    }
}
