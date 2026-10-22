package com.thelast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import com.thelast.app.navigation.TheLastNavHost
import com.thelast.app.ui.theme.TheLastTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TheLastApplication::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test(timeout = 60000) fun homeNavigationAndUpdateCenterRender() {
        compose.setContent { TheLastTheme { TheLastNavHost() } }
        compose.waitForIdle()
        compose.onNodeWithText("我的生活抽屉").assertIsDisplayed()
        capture("home")
        compose.onNodeWithText("我的", useUnmergedTree = true).performClick()
        compose.onNodeWithText("让小日常，常用常新").assertIsDisplayed()
        capture("profile")
        compose.onNodeWithText("设置来源").performClick()
        compose.onNodeWithText("owner/repository").performTextInput("invalid")
        compose.onNodeWithText("保存并检查").performScrollTo().performClick()
        compose.onNodeWithText("请输入 GitHub 仓库地址或 owner/repository").assertExists()
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.onNodeWithText("日历", useUnmergedTree = true).performClick()
        capture("calendar")
        compose.onNodeWithText("统计", useUnmergedTree = true).performClick()
        capture("stats")
    }

    @Test(timeout = 60000) fun recordedEventAndNightThemeRender() {
        val app = org.robolectric.RuntimeEnvironment.getApplication() as TheLastApplication
        kotlinx.coroutines.runBlocking {
            val id = app.eventRepository.createEventType("加油", "gas", 0xFFB96145, "出行", 0)
            app.eventRepository.quickRecord(id)
        }
        compose.setContent { TheLastTheme(themeMode = com.thelast.app.data.datastore.ThemeMode.Night) { TheLastNavHost() } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("加油").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("加油").assertIsDisplayed()
        compose.onNodeWithText("记一下").assertIsDisplayed()
        capture("home-night-recorded")
    }

    private fun capture(name: String) {
        val output = File("build/reports/ui/$name.png").also { it.parentFile!!.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
