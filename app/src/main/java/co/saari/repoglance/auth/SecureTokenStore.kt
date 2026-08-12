package co.saari.repoglance.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface TokenStore {
    fun read(): GitHubUserToken?
    fun write(token: GitHubUserToken)
    fun clear()
}

class SecureTokenStore(context: Context) : TokenStore {
    private val tokenFile = AtomicFile(File(context.noBackupFilesDir, TOKEN_FILE_NAME))

    override fun read(): GitHubUserToken? {
        if (!tokenFile.baseFile.exists()) return null
        return runCatching {
            val encrypted = tokenFile.openRead().use { it.readBytes() }
            decode(decrypt(encrypted))
        }.getOrElse {
            clear()
            null
        }
    }

    override fun write(token: GitHubUserToken) {
        val encrypted = encrypt(encode(token))
        val stream = tokenFile.startWrite()
        try {
            stream.write(encrypted)
            tokenFile.finishWrite(stream)
        } catch (failure: Throwable) {
            tokenFile.failWrite(stream)
            throw failure
        }
    }

    override fun clear() {
        tokenFile.delete()
    }

    private fun encrypt(cleartext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(cleartext)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(FORMAT_VERSION)
                output.writeInt(cipher.iv.size)
                output.write(cipher.iv)
                output.writeInt(ciphertext.size)
                output.write(ciphertext)
            }
            bytes.toByteArray()
        }
    }

    private fun decrypt(payload: ByteArray): ByteArray = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        require(input.readInt() == FORMAT_VERSION) { "Unsupported token format" }
        val ivSize = input.readInt()
        require(ivSize in 12..32) { "Invalid token IV" }
        val iv = ByteArray(ivSize).also(input::readFully)
        val ciphertextSize = input.readInt()
        require(ciphertextSize in 1..MAX_TOKEN_PAYLOAD_BYTES) { "Invalid token payload" }
        val ciphertext = ByteArray(ciphertextSize).also(input::readFully)
        require(input.available() == 0) { "Unexpected token bytes" }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        cipher.doFinal(ciphertext)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun encode(token: GitHubUserToken): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(FORMAT_VERSION)
            output.writeUTF(token.accessToken)
            output.writeOptionalString(token.refreshToken)
            output.writeOptionalInstant(token.accessTokenExpiresAt)
            output.writeOptionalInstant(token.refreshTokenExpiresAt)
            output.writeUTF(token.tokenType)
        }
        bytes.toByteArray()
    }

    private fun decode(payload: ByteArray): GitHubUserToken = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        require(input.readInt() == FORMAT_VERSION) { "Unsupported token format" }
        GitHubUserToken(
            accessToken = input.readUTF(),
            refreshToken = input.readOptionalString(),
            accessTokenExpiresAt = input.readOptionalInstant(),
            refreshTokenExpiresAt = input.readOptionalInstant(),
            tokenType = input.readUTF(),
        ).also { require(input.available() == 0) { "Unexpected token fields" } }
    }

    private fun DataOutputStream.writeOptionalString(value: String?) {
        writeBoolean(value != null)
        value?.let(::writeUTF)
    }

    private fun DataInputStream.readOptionalString(): String? = if (readBoolean()) readUTF() else null

    private fun DataOutputStream.writeOptionalInstant(value: Instant?) {
        writeBoolean(value != null)
        value?.let { writeLong(it.epochSecond) }
    }

    private fun DataInputStream.readOptionalInstant(): Instant? =
        if (readBoolean()) Instant.ofEpochSecond(readLong()) else null

    private companion object {
        const val TOKEN_FILE_NAME = "github-user-token.enc"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "co.saari.repoglance.github-user-token.v1"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        // v2 deliberately invalidates v1 web-flow sessions: only tokens minted
        // by device flow are eligible for secret-free refresh.
        const val FORMAT_VERSION = 2
        const val MAX_TOKEN_PAYLOAD_BYTES = 128 * 1024
    }
}
