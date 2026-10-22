package com.thelast.app

import android.app.Application
import android.content.Context
import com.thelast.app.data.update.AppUpdateManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppUpdateManagerTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val json = """{"tag_name":"v0.13.0","assets":[{"name":"app.apk","size":3,
        "browser_download_url":"https://github.com/owner/repo/releases/download/v0.13.0/app.apk",
        "digest":"sha256:${"ab".repeat(32)}"}]}"""
    private fun prepare() {
        context.getSharedPreferences("app_updates", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun response(url: String, code: Int, body: String) = object : HttpURLConnection(URL(url)) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getResponseCode() = code
        override fun getInputStream() = body.byteInputStream()
    }
    private fun await(manager: AppUpdateManager) {
        val deadline = System.currentTimeMillis() + 5000
        while (manager.state.value.busy && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(10)
        }
        ShadowLooper.idleMainLooper()
        assertFalse("更新操作未完成", manager.state.value.busy)
    }
    @Test fun detectsNewReleaseAndThrottlesAutomaticChecks() {
        prepare()
        val calls = AtomicInteger()
        val manager = AppUpdateManager(context) { calls.incrementAndGet(); response(it, 200, json) }
        assertNull(manager.configure("owner/repo"))
        await(manager)
        assertEquals("0.13.0", manager.state.value.release?.version)
        manager.check()
        assertEquals(1, calls.get())
        manager.check(true)
        await(manager)
        assertEquals(2, calls.get())
    }
    @Test fun disabledAutomaticCheckStillAllowsManualCheck() {
        prepare()
        context.getSharedPreferences("app_updates", Context.MODE_PRIVATE).edit()
            .putString("repository", "owner/repo").putBoolean("automatic", false).commit()
        val calls = AtomicInteger()
        val manager = AppUpdateManager(context) { calls.incrementAndGet(); response(it, 404, "") }
        manager.check()
        assertEquals(0, calls.get())
        manager.check(true)
        await(manager)
        assertEquals(1, calls.get())
        assertTrue(manager.state.value.message.contains("未找到公开正式版本"))
    }
    @Test fun corruptDownloadIsDeletedAndCannotBeInstalled() {
        prepare()
        val manager = AppUpdateManager(context) { response(it, 200, if (it.contains("api.github.com")) json else "bad") }
        manager.configure("owner/repo")
        await(manager)
        manager.download()
        await(manager)
        assertFalse(manager.state.value.ready)
        assertTrue(manager.state.value.message.contains("校验失败"))
        assertFalse(java.io.File(context.cacheDir, "updates/thelast.apk").exists())
        assertFalse(java.io.File(context.cacheDir, "updates/download.part").exists())
    }
    @Test fun rateLimitAndInvalidSourceAreRecoverable() {
        prepare()
        val manager = AppUpdateManager(context) { response(it, 403, "") }
        assertNotNull(manager.configure("https://evil.example/repo"))
        assertEquals("9OwO6/The-Last", manager.state.value.repository)
        manager.configure("owner/repo")
        await(manager)
        assertTrue(manager.state.value.message.contains("过于频繁"))
        assertFalse(manager.state.value.busy)
    }
}
