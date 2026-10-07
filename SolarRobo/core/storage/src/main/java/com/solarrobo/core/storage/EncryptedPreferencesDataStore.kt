package com.solarrobo.core.storage

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private data class SecurePreferences(
    val values: Map<String, String> = emptyMap()
)

@Singleton
class EncryptedPreferencesDataStore @Inject constructor(
    @ApplicationContext context: Context
) : SecurePreferencesStore {
    private val dataStoreDirectory = File(context.filesDir, "datastore").also { directory ->
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create the preferences directory.")
        }
    }
    private val dataStore: DataStore<SecurePreferences> = DataStoreFactory.create(
        serializer = EncryptedPreferencesSerializer(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) {
        File(dataStoreDirectory, FILE_NAME)
    }

    override val preferences: Flow<Map<String, String>> =
        dataStore.data.map { it.values.toMap() }

    override suspend fun update(transform: (Map<String, String>) -> Map<String, String>) {
        dataStore.updateData { current ->
            SecurePreferences(transform(current.values.toMap()).toMap())
        }
    }

    private companion object {
        const val FILE_NAME = "secure_user_preferences.bin"
    }
}

private class EncryptedPreferencesSerializer : Serializer<SecurePreferences> {
    override val defaultValue = SecurePreferences()

    override suspend fun readFrom(input: InputStream): SecurePreferences {
        val encrypted = input.readBytes()
        if (encrypted.isEmpty()) return defaultValue
        if (encrypted.size <= GCM_IV_LENGTH_BYTES) {
            throw CorruptionException(
                "Encrypted preferences are truncated.",
                IllegalArgumentException("Encrypted preferences are truncated.")
            )
        }

        val plainText = try {
            decrypt(encrypted)
        } catch (error: GeneralSecurityException) {
            throw CorruptionException("Unable to decrypt user preferences.", error)
        }
        return try {
            DataInputStream(ByteArrayInputStream(plainText)).use { data ->
                val count = data.readInt()
                if (count < 0 || count > MAX_ENTRIES) {
                    throw IllegalArgumentException("Invalid preference count.")
                }
                val values = buildMap {
                    repeat(count) {
                        put(data.readUTF(), data.readUTF())
                    }
                    if (data.available() != 0) {
                        throw IllegalArgumentException("Unexpected data after preferences.")
                    }
                }
                SecurePreferences(values)
            }
        } catch (error: IOException) {
            throw CorruptionException("Unable to read user preferences.", error)
        } catch (error: IllegalArgumentException) {
            throw CorruptionException("Unable to read user preferences.", error)
        }
    }

    override suspend fun writeTo(t: SecurePreferences, output: OutputStream) {
        val plainText = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { data ->
                require(t.values.size <= MAX_ENTRIES) { "Too many preferences." }
                data.writeInt(t.values.size)
                t.values.toSortedMap().forEach { (key, value) ->
                    data.writeUTF(key)
                    data.writeUTF(value)
                }
            }
            buffer.toByteArray()
        }
        val encrypted = try {
            encrypt(plainText)
        } catch (error: GeneralSecurityException) {
            throw IOException("Unable to encrypt user preferences.", error)
        }
        output.write(encrypted)
    }

    private fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText)
        return cipher.iv + encrypted
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val payload = encrypted.copyOfRange(GCM_IV_LENGTH_BYTES, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        return cipher.doFinal(payload)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEY_STORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "solarrobo.user.preferences"
        const val KEY_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val MAX_ENTRIES = 128
    }
}
