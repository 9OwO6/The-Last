package com.thelast.app.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.thelast.app.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UpdateState(
    val repository: String = "", val automatic: Boolean = true,
    val busy: Boolean = false, val message: String = "连接发布仓库，及时收到新版本",
    val release: ReleaseInfo? = null, val progress: Float? = null, val ready: Boolean = false,
)

class AppUpdateManager(
    private val context: Context,
    private val connectionFactory: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection },
) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutable = MutableStateFlow(UpdateState(prefs.getString("repository", "") ?: "", prefs.getBoolean("automatic", true)))
    val state = mutable.asStateFlow()
    private var lastAttempt = 0L
    private val apk get() = File(context.cacheDir, "updates/thelast.apk")

    fun configure(value: String): String? {
        if (state.value.busy) return "请等待当前操作完成"
        val repo = try { ReleaseInfo.repository(value) } catch (e: IllegalArgumentException) { return e.message }
        prefs.edit().putString("repository", repo).remove("lastCheck").apply()
        apk.delete()
        lastAttempt = 0
        mutable.value = UpdateState(repository = repo, automatic = state.value.automatic)
        check(true)
        return null
    }

    fun setAutomatic(enabled: Boolean) {
        prefs.edit().putBoolean("automatic", enabled).apply()
        mutable.value = state.value.copy(automatic = enabled)
        if (enabled) check()
    }

    fun check(force: Boolean = false) {
        val old = state.value
        if (old.busy || old.repository.isEmpty() || (!force && !old.automatic)) return
        val now = System.currentTimeMillis()
        if (!force && now - maxOf(lastAttempt, prefs.getLong("lastCheck", 0)) < 6 * 60 * 60 * 1000) return
        lastAttempt = now
        mutable.value = old.copy(busy = true, message = "正在检查新版本…")
        scope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    val connection = connect("https://api.github.com/repos/${old.repository}/releases/latest")
                    try {
                        when (connection.responseCode) {
                            404 -> error("未找到公开正式版本，请检查仓库地址和 Release")
                            403, 429 -> error("检查过于频繁，请稍后重试")
                        }
                        require(connection.responseCode == 200) { "检查失败（HTTP ${connection.responseCode}）" }
                        val bytes = connection.inputStream.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                require(output.size() + read <= 1024 * 1024) { "发布信息过大" }
                                output.write(buffer, 0, read)
                            }
                            output.toByteArray()
                        }
                        require(bytes.size <= 1024 * 1024) { "发布信息过大" }
                        ReleaseInfo.parse(bytes.toString(Charsets.UTF_8), old.repository)
                    } finally { connection.disconnect() }
                }
                val newer = ReleaseInfo.isNewer(release.version, BuildConfig.VERSION_NAME)
                prefs.edit().putLong("lastCheck", now).apply()
                val same = old.release == release
                mutable.value = old.copy(busy = false, release = release.takeIf { newer }, ready = same && old.ready && apk.exists(),
                    message = if (newer) "发现新版本 ${release.version}" else "已经是最新版本")
            } catch (e: Exception) {
                mutable.value = old.copy(busy = false, message = e.message?.take(180) ?: "网络连接失败，请稍后重试")
            }
        }
    }

    fun download() {
        val old = state.value
        val release = old.release ?: return
        if (old.busy) return
        mutable.value = old.copy(busy = true, progress = 0f, ready = false, message = "正在下载安装包…")
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apk.parentFile!!.mkdirs()
                    val partial = File(apk.parentFile, "download.part")
                    try {
                        val connection = connect(release.url)
                        try {
                            require(connection.responseCode == 200) { "下载失败（HTTP ${connection.responseCode}）" }
                            var count = 0L
                            val digest = MessageDigest.getInstance("SHA-256")
                            connection.inputStream.use { input -> partial.outputStream().use { output ->
                                val buffer = ByteArray(65536)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    count += read
                                    require(count <= release.size) { "下载大小异常" }
                                    digest.update(buffer, 0, read)
                                    output.write(buffer, 0, read)
                                    mutable.value = mutable.value.copy(progress = count.toFloat() / release.size)
                                }
                            } }
                            require(count == release.size) { "下载不完整，请重试" }
                            require(digest.digest().joinToString("") { "%02x".format(it) }.equals(release.sha256, true)) { "安装包校验失败，请重新下载" }
                            validatePackage(partial)
                            apk.delete()
                            check(partial.renameTo(apk)) { "无法保存安装包" }
                        } finally { connection.disconnect() }
                    } finally { partial.delete() }
                }
                mutable.value = old.copy(ready = true, message = "校验通过，可以安装")
            } catch (e: Exception) {
                apk.delete()
                mutable.value = old.copy(ready = false, message = e.message?.take(180) ?: "下载失败，请重试")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun validatePackage(file: File) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = context.packageManager.getPackageArchiveInfo(file.path, flags) ?: error("安装包无法识别")
        val current = context.packageManager.getPackageInfo(context.packageName, flags)
        require(candidate.packageName == context.packageName) { "安装包应用标识不匹配" }
        val code = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        require(code > BuildConfig.VERSION_CODE) { "安装包版本未高于当前版本" }
        require(candidate.versionName == state.value.release?.version) { "发布版本和安装包版本不匹配" }
        fun signatures(info: android.content.pm.PackageInfo) =
            (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
                ?.map { it.toCharsString() }?.toSet().orEmpty()
        require(signatures(current).isNotEmpty() && signatures(candidate) == signatures(current)) { "安装包签名不同，无法保留数据更新" }
    }

    fun install() {
        try {
            require(state.value.ready && apk.exists()) { "请先下载安装包" }
            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                mutable.value = state.value.copy(message = "允许此来源后，返回并再次点击安装")
                return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { mutable.value = state.value.copy(message = e.message ?: "无法打开安装程序") }
    }

    private fun connect(url: String): HttpURLConnection = connectionFactory(url).apply {
        connectTimeout = 15000; readTimeout = 30000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "TheLast/${BuildConfig.VERSION_NAME}")
    }
}
