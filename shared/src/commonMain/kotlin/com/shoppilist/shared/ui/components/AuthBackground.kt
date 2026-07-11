package com.shoppilist.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Decorative backdrop for the launch/auth screens: a soft sage→cream→gold vertical gradient with
 * large, faded shopping-category emojis scattered around the edges — echoing the Sage & Gold theme
 * and the app's grocery→fashion→electronics catalog breadth. Content stays centered and readable
 * because the watermarks hug the corners at very low alpha.
 */
@Composable
fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),      // sage wash at the top
            MaterialTheme.colorScheme.background,                        // cream middle
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)       // gold wash at the bottom
        )
    )
    Box(modifier = Modifier.fillMaxSize().background(gradient)) {
        WatermarkEmoji("🛒", Alignment.TopStart, x = 8.dp, y = 40.dp, rotation = -18f)
        WatermarkEmoji("🥦", Alignment.TopEnd, x = (-20).dp, y = 96.dp, rotation = 14f)
        WatermarkEmoji("👗", Alignment.CenterStart, x = (-14).dp, y = (-60).dp, rotation = -10f)
        WatermarkEmoji("📱", Alignment.CenterEnd, x = 16.dp, y = 30.dp, rotation = 12f)
        WatermarkEmoji("🍞", Alignment.BottomStart, x = 20.dp, y = (-110).dp, rotation = 10f)
        WatermarkEmoji("🎁", Alignment.BottomEnd, x = (-10).dp, y = (-48).dp, rotation = -14f)
        WatermarkEmoji("🥛", Alignment.BottomCenter, x = (-70).dp, y = (-16).dp, rotation = 8f)
        content()
    }
}

@Composable
private fun BoxScope.WatermarkEmoji(
    emoji: String,
    alignment: Alignment,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    rotation: Float
) {
    Text(
        emoji,
        fontSize = 64.sp,
        modifier = Modifier
            .align(alignment)
            .offset(x = x, y = y)
            .rotate(rotation)
            .alpha(0.14f)
    )
}
