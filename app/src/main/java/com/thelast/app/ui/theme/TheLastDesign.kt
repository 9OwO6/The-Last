package com.thelast.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * thelast 的品牌层：纸张、墨色、印章和生活节奏色。
 * 业务页面只消费这组语义色，避免各页面各自维护一套暖色常量。
 */
@Immutable
data class TheLastColors(
    val background: Color,
    val surface: Color,
    val panel: Color,
    val elevated: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val peach: Color,
    val green: Color,
    val blue: Color,
    val yellow: Color,
    val lavender: Color,
    val shadow: Color,
)

@Composable
fun theLastColors(): TheLastColors {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.4f
    return if (dark) {
        TheLastColors(
            background = scheme.background,
            surface = scheme.surface,
            panel = scheme.surfaceContainer,
            elevated = scheme.surfaceContainerHigh,
            ink = scheme.onBackground,
            muted = scheme.onSurfaceVariant,
            line = scheme.outlineVariant,
            peach = scheme.primary,
            green = scheme.secondary,
            blue = Color(0xFFA9C8DF),
            yellow = scheme.tertiary,
            lavender = Color(0xFFD8BCE5),
            shadow = Color.Black.copy(alpha = 0.32f),
        )
    } else {
        TheLastColors(
            background = scheme.background,
            surface = scheme.surface,
            panel = scheme.surfaceContainer,
            elevated = scheme.surfaceContainerHigh,
            ink = scheme.onBackground,
            muted = scheme.onSurfaceVariant,
            line = scheme.outlineVariant,
            peach = scheme.primary,
            green = scheme.secondary,
            blue = Color(0xFF8EB9D7),
            yellow = scheme.tertiary,
            lavender = Color(0xFFC8A7D8),
            shadow = Color(0xFF5A3726).copy(alpha = 0.10f),
        )
    }
}

object TheLastShape {
    val Hero = 32.dp
    val Card = 24.dp
    val SmallCard = 18.dp
    val Pill = 999.dp
}

object TheLastMotion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
    fun <T> settle() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    fun <T> reveal(durationMillis: Int = 420) = tween<T>(durationMillis, easing = EmphasizedEasing)
}
