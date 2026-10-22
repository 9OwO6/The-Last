package com.thelast.app.data.sync

import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import com.thelast.app.data.database.TheLastDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class CloudUiState(
    val server: String = "", val connected: Boolean = false, val pairingCode: String = "",
    val message: String = "未连接云端，记录保存在本机", val busy: Boolean = false,
)

class CloudSyncManager(context: Context, private val db: TheLastDatabase) {
    private val credentials = CloudCredentialStore(context)
    private val transport = HttpCloudTransport()
    private val engine = CloudSyncEngine(db, transport)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(credentials.load().let { c ->
        CloudUiState(server = c?.server.orEmpty(), connected = c?.approved == true,
            pairingCode = if (c?.approved == false) c.code else "",
            message = if (c?.approved == true) "等待同步" else "未连接云端，记录保存在本机")
    })
    val state = mutableState.asStateFlow()

    fun schedule() { scope.launch { delay(400); syncNow() } }
    suspend fun syncNow() = mutex.withLock {
        val credential = credentials.load() ?: return@withLock
        if (!credential.approved) return@withLock
        mutableState.value = mutableState.value.copy(busy = true, message = "正在同步…")
        try {
            engine.sync(credential)
            val all = db.syncDao().all(); val conflicts = all.count { it.conflict != null }; val pending = all.count { it.dirty && it.conflict == null }
            mutableState.value = mutableState.value.copy(message = when {
                conflicts > 0 -> "有 $conflicts 项修改冲突，本机草稿已保留"
                pending > 0 -> "仍有 $pending 项等待关联事项同步"
                else -> "云端已同步"
            })
        } catch (error: Exception) {
            mutableState.value = mutableState.value.copy(message = if (error is CloudHttpError) error.message ?: "云端连接失败" else "网络暂不可用，记录已保存本机，联网后重试")
        } finally { mutableState.value = mutableState.value.copy(busy = false) }
    }

    fun startPair(serverInput: String) { scope.launch { mutex.withLock {
        try {
            val server = serverInput.trim().trimEnd('/')
            val uri = URI(server)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty()) { "请输入 MyJournal 的 HTTPS 网站首页地址" }
            val existing = credentials.load()
            require(existing == null || existing.server == server) { "请先断开现有连接；为避免混入另一套数据库，此安装只能连接原网站" }
            mutableState.value = mutableState.value.copy(busy = true, message = "正在生成连接码…")
            val result = transport.post(server, "/api/thelast/pair", JSONObject().put("action", "start").put("name", "${Build.MANUFACTURER} ${Build.MODEL}"))
            val credential = CloudCredential(server, result.getString("token"), result.getString("code"), false)
            credentials.save(credential)
            mutableState.value = CloudUiState(server, false, credential.code, "连接码 10 分钟内有效。请在浏览器登录 MyJournal 并授权")
        } catch (error: Exception) { mutableState.value = mutableState.value.copy(busy = false, message = error.message ?: "无法发起连接") }
    } } }

    fun checkPair() { scope.launch {
        mutex.withLock {
            val credential = credentials.load() ?: return@launch
            if (credential.approved || credential.code.isEmpty()) return@launch
            try {
                val result = transport.post(credential.server, "/api/thelast/pair", JSONObject().put("action", "status").put("token", credential.token))
                when (result.getString("status")) {
                    "approved" -> { credentials.save(credential.copy(approved = true)); mutableState.value = CloudUiState(credential.server, true, message = "连接成功，正在同步原有记录…") }
                    "pending" -> mutableState.value = mutableState.value.copy(message = "还在等待浏览器授权，请核对连接码后确认")
                    else -> mutableState.value = mutableState.value.copy(pairingCode = "", message = "连接码已过期或授权已撤销，请重新连接")
                }
            } catch (error: Exception) { mutableState.value = mutableState.value.copy(message = error.message ?: "连接检查失败") }
        }
        syncNow()
    } }

    fun disconnect() { scope.launch { mutex.withLock {
        // Keep original server binding to prevent accidental cross-project merges.
        credentials.load()?.let { credentials.save(it.copy(approved = false, code = "", token = "")) }
        mutableState.value = CloudUiState(server = mutableState.value.server, message = "已停止本机同步。可在网页撤销设备授权；本机记录保留")
    } } }
    fun useCloudVersion() { scope.launch {
        mutex.withLock { engine.discardConflicts() }; syncNow()
    } }
    suspend fun exportSyncBackup(): String = db.withTransaction {
        JSONArray().apply { db.syncDao().all().forEach { row -> put(JSONObject().put("cloudId", row.cloudId).put("kind", row.kind)
            .put("data", JSONObject(row.payload)).put("deleted", row.deleted).put("dirty", row.dirty)
            .put("conflict", row.conflict ?: JSONObject.NULL).put("frozenOperation", row.frozenOperation ?: JSONObject.NULL)) } }.toString(2)
    }
}

