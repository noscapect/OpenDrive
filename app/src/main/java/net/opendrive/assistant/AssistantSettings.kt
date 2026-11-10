package net.opendrive.assistant

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** OpenDrive-only settings. The personal API credential never enters the APK, logs or backups. */
class AssistantSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("opendrive_assistant", Context.MODE_PRIVATE)
    private val keyFile = File(context.applicationContext.noBackupFilesDir, "opendrive-openai-key")

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit { putBoolean("enabled", value) }
    var model: String
        get() = prefs.getString("model", "gpt-6.1-sol") ?: "gpt-6.1-sol"
        set(value) = prefs.edit { putString("model", value.trim().ifBlank { "gpt-6.1-sol" }) }
    var speakReplies: Boolean
        get() = prefs.getBoolean("speak", true)
        set(value) = prefs.edit { putBoolean("speak", value) }
    var preferences: String
        get() = prefs.getString("preferences", "") ?: ""
        set(value) = prefs.edit { putString("preferences", value.trim().take(1000)) }

    val hasKey: Boolean get() = keyFile.isFile

    fun observe(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun unobserve(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun saveKey(value: String) {
        val key = value.trim()
        require(key.isNotBlank() && key.length <= 1024 && key.none { it.isWhitespace() })
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
        val contents = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val temporary = File(keyFile.parentFile, "${keyFile.name}.tmp")
        temporary.writeText(contents)
        check(temporary.renameTo(keyFile)) { "Could not save credential" }
    }

    fun readKey(): String {
        val parts = keyFile.readText().split(':', limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun forgetKey() {
        check(!keyFile.exists() || keyFile.delete()) { "Could not remove credential" }
        enabled = false
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object { const val ALIAS = "opendrive.personal.openai" }
}
