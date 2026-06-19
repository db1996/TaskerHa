package com.github.db1996.taskerha.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import kotlinx.serialization.json.*
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Backup format:
 *  - Unencrypted: raw UTF-8 JSON bytes
 *  - Encrypted:   0x01 (version byte) + 16-byte salt + 12-byte IV + AES-256-GCM ciphertext
 *
 * JSON structure:
 *  { "version": 1, "ts": <ms>, "prefs": { "<prefsName>": { "<key>": { "t": "<type>", "v": <value> } } } }
 *
 * Type codes: "s" = String, "b" = Boolean, "i" = Int, "l" = Long, "f" = Float, "ss" = StringSet
 */
object BackupManager {

    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITER = 100_000
    private const val KEY_BITS = 256
    private const val ENC_MARKER = 0x01.toByte()

    val PREFS_NAMES = listOf("ha_settings", "ha_instances", "TriggerStatePrefs")

    fun createBackupJson(context: Context): String {
        val prefsData = buildJsonObject {
            for (name in PREFS_NAMES) {
                val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                put(name, buildJsonObject {
                    for ((k, v) in sp.all) {
                        put(k, encodeEntry(v))
                    }
                })
            }
        }
        return buildJsonObject {
            put("version", 1)
            put("ts", System.currentTimeMillis())
            put("prefs", prefsData)
        }.toString()
    }

    fun writeAndShareBackup(context: Context, json: String, password: String?) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val finalBytes = if (!password.isNullOrBlank()) encrypt(bytes, password) else bytes

        // Remove any leftover backup files
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("taskerha_backup_") }
            ?.forEach { it.delete() }

        val filename = "taskerha_backup_${System.currentTimeMillis()}.taskerha_backup"
        val file = File(context.filesDir, filename)
        file.writeBytes(finalBytes)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-taskerha-backup"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TaskerHA Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Save backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun restoreBackup(context: Context, data: ByteArray, password: String?): RestoreResult {
        return try {
            val json = if (isEncrypted(data)) {
                if (password.isNullOrBlank()) return RestoreResult.NeedsPassword
                val plain = tryDecrypt(data, password) ?: return RestoreResult.WrongPassword
                plain.toString(Charsets.UTF_8)
            } else {
                data.toString(Charsets.UTF_8)
            }

            val root = Json.parseToJsonElement(json).jsonObject
            val prefs = root["prefs"]?.jsonObject ?: return RestoreResult.InvalidFormat

            for ((prefsName, entriesEl) in prefs) {
                val sp = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val editor = sp.edit()
                editor.clear()
                for ((key, entryEl) in entriesEl.jsonObject) {
                    applyEntry(editor, key, entryEl.jsonObject)
                }
                editor.apply()
            }

            HaInstanceRepository.init(context)
            PrefsJsonStore.reload("TriggerStatePrefs")

            RestoreResult.Success
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun encodeEntry(value: Any?): JsonObject = when (value) {
        is String -> buildJsonObject { put("t", "s"); put("v", value) }
        is Boolean -> buildJsonObject { put("t", "b"); put("v", value) }
        is Int -> buildJsonObject { put("t", "i"); put("v", value) }
        is Long -> buildJsonObject { put("t", "l"); put("v", value) }
        is Float -> buildJsonObject { put("t", "f"); put("v", value) }
        is Set<*> -> buildJsonObject {
            put("t", "ss")
            put("v", JsonArray(value.map { JsonPrimitive(it.toString()) }))
        }
        else -> buildJsonObject { put("t", "s"); put("v", value?.toString() ?: "") }
    }

    private fun applyEntry(editor: SharedPreferences.Editor, key: String, obj: JsonObject) {
        val t = obj["t"]?.jsonPrimitive?.content ?: return
        val v = obj["v"] ?: return
        when (t) {
            "s" -> editor.putString(key, v.jsonPrimitive.content)
            "b" -> editor.putBoolean(key, v.jsonPrimitive.boolean)
            "i" -> editor.putInt(key, v.jsonPrimitive.int)
            "l" -> editor.putLong(key, v.jsonPrimitive.long)
            "f" -> editor.putFloat(key, v.jsonPrimitive.float)
            "ss" -> editor.putStringSet(key, v.jsonArray.map { it.jsonPrimitive.content }.toSet())
        }
    }

    private fun isEncrypted(data: ByteArray) = data.isNotEmpty() && data[0] == ENC_MARKER

    private fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plain)
        return byteArrayOf(ENC_MARKER) + salt + iv + ct
    }

    private fun tryDecrypt(data: ByteArray, password: String): ByteArray? = try {
        val salt = data.copyOfRange(1, 1 + SALT_LEN)
        val iv = data.copyOfRange(1 + SALT_LEN, 1 + SALT_LEN + IV_LEN)
        val ct = data.copyOfRange(1 + SALT_LEN + IV_LEN, data.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(ct)
    } catch (_: Exception) { null }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITER, KEY_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    sealed class RestoreResult {
        object Success : RestoreResult()
        object NeedsPassword : RestoreResult()
        object WrongPassword : RestoreResult()
        object InvalidFormat : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }
}
