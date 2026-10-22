package com.thelast.app

import android.app.Application
import com.thelast.app.data.update.ReleaseInfo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ReleaseInfoTest {
    private fun release() = JSONObject("""{
        "tag_name":"v0.12.0", "body":"新图标与体验优化", "draft":false, "prerelease":false,
        "assets":[{"name":"thelast.apk","size":2048,
        "browser_download_url":"https://github.com/owner/The-Last/releases/download/v0.12.0/thelast.apk",
        "digest":"sha256:${"ab".repeat(32)}"}]
    }""")
    private fun parse(value: JSONObject) = ReleaseInfo.parse(value.toString(), "owner/The-Last")
    private fun rejected(value: JSONObject) {
        assertThrows(IllegalArgumentException::class.java) { parse(value) }
    }
    @Test fun readsReleaseNotesAndIntegrityMetadata() {
        val result = parse(release())
        assertEquals("0.12.0", result.version)
        assertEquals("新图标与体验优化", result.notes)
        assertEquals(2048L, result.size)
        assertEquals("ab".repeat(32), result.sha256)
    }
    @Test fun comparesVersionsNumericallyAndRejectsDowngrades() {
        assertTrue(ReleaseInfo.isNewer("0.11.0", "0.9.9"))
        assertTrue(ReleaseInfo.isNewer("1.0.0", "0.99.99"))
        assertFalse(ReleaseInfo.isNewer("0.11.0", "0.11.0"))
        assertFalse(ReleaseInfo.isNewer("0.10.0", "0.11.0"))
        assertFalse(ReleaseInfo.isNewer("0.12.0-beta", "0.11.0"))
        assertFalse(ReleaseInfo.isNewer("999999999999999999999.0.0", "0.11.0"))
    }
    @Test fun acceptsOnlyRepositoryNamesAndOfficialGithubUrls() {
        assertEquals("owner/The-Last", ReleaseInfo.repository(" https://github.com/owner/The-Last.git/ "))
        listOf("https://evil.test/owner/repo", "owner/repo/releases", "owner/..", "", "owner/repo?token=x").forEach {
            assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.repository(it) }
        }
    }
    @Test fun rejectsDraftAndPrerelease() {
        rejected(release().put("draft", true))
        rejected(release().put("prerelease", true))
    }
    @Test fun rejectsNonSemanticTags() { rejected(release().put("tag_name", "latest")) }
    @Test fun requiresSha256() {
        val data = release()
        data.getJSONArray("assets").getJSONObject(0).remove("digest")
        rejected(data)
    }
    @Test fun rejectsOtherRepositoryOrHost() {
        for (url in listOf("https://evil.test/thelast.apk", "https://github.com/other/repo/releases/download/v0.12.0/app.apk")) {
            val data = release()
            data.getJSONArray("assets").getJSONObject(0).put("browser_download_url", url)
            rejected(data)
        }
    }
    @Test fun rejectsEmptyOrOversizedApk() {
        for (size in listOf(0L, 201L * 1024 * 1024)) {
            val data = release()
            data.getJSONArray("assets").getJSONObject(0).put("size", size)
            rejected(data)
        }
    }
    @Test fun rejectsMissingAndAmbiguousApks() {
        val data = release()
        data.getJSONArray("assets").put(JSONObject(data.getJSONArray("assets").getJSONObject(0).toString()))
        rejected(data)
        rejected(release().put("assets", org.json.JSONArray()))
    }
}
