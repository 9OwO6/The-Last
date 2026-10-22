package com.thelast.app.ui.illustration

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thelast.app.ui.theme.theLastColors

/** One story illustration per page; controls and dense records keep simple semantic icons. */
@Composable
fun StoryBanner(
    eyebrow: String,
    title: String,
    subtitle: String,
    @DrawableRes illustration: Int,
    modifier: Modifier = Modifier,
) {
    val colors = theLastColors()
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 10.dp)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 340.dp
        Row(
            modifier = Modifier.fillMaxWidth().clip(shape)
                .background(Brush.linearGradient(listOf(colors.panel, colors.surface)))
                .border(1.dp, colors.line.copy(alpha = 0.6f), shape)
                .padding(start = 20.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = colors.green,
                    fontWeight = FontWeight.Bold)
                Text(title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, color = colors.ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            Image(painterResource(illustration), contentDescription = null,
                modifier = Modifier.weight(0.95f).height(156.dp), contentScale = ContentScale.Fit)
        }
    }
}
