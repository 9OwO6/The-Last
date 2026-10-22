package com.thelast.app.data.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CloudCredential(val server: String, val token: String, val code: String, val approved: Boolean)

/** Keystore-encrypted credential stored outside Android backup/device transfer. */
class CloudCredentialStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "thelast-cloud.enc")
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("thelast-cloud-v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("thelast-cloud-v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun load(): CloudCredential? = runCatching {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        CloudCredential(json.getString("server"), json.getString("token"), json.getString("code"), json.getBoolean("approved"))
    }.getOrNull()
    fun save(value: CloudCredential) {
        val json = JSONObject().put("server", value.server).put("token", value.token).put("code", value.code).put("approved", value.approved)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val temp = File(file.parentFile, "thelast-cloud.tmp")
        temp.writeBytes(cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8)))
        check(temp.renameTo(file)) { "无法保存设备授权" }
    }
    fun clear() { file.delete() }
}

class CloudHttpError(val status: Int, message: String) : Exception(message)
interface CloudTransport {
    suspend fun post(server: String, path: String, body: JSONObject, token: String? = null): JSONObject
}
class HttpCloudTransport : CloudTransport {
    override suspend fun post(server: String, path: String, body: JSONObject, token: String?): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(server + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 15000; connection.readTimeout = 30000
            connection.instanceFollowRedirects = false
            connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { throw CloudHttpError(status, "云端响应异常，请检查网站地址") }
            if (status !in 200..299) throw CloudHttpError(status, json.optString("error", "云端暂时不可用"))
            json
        } finally { connection.disconnect() }
    }
}
