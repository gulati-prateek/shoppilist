package com.shoppilist.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Recognizable brand-colored monogram for a quick-commerce / e-commerce vendor. We don't bundle
 * trademarked logo images, so this shows the vendor's initial on a brand-associated background —
 * far more distinguishable than a generic emoji. Known vendors get their signature color; anything
 * else gets a stable seeded color.
 */
private val VENDOR_COLORS: List<Pair<String, Color>> = listOf(
    "blinkit" to Color(0xFFF8CB46),
    "bigbasket" to Color(0xFF84C225),
    "jiomart" to Color(0xFF0C5AAB),
    "zepto" to Color(0xFF6A1B9A),
    "swiggy" to Color(0xFFFC8019),
    "instamart" to Color(0xFFFC8019),
    "dunzo" to Color(0xFF00D290),
    "instacart" to Color(0xFF0AAD0A),
    "amazon" to Color(0xFF232F3E),
    "flipkart" to Color(0xFF2874F0),
    "walmart" to Color(0xFF0071CE),
    "target" to Color(0xFFCC0000),
    "tesco" to Color(0xFF00539F),
    "sainsbury" to Color(0xFFF06C00),
    "asda" to Color(0xFF68A51C),
    "ocado" to Color(0xFF6D2C91),
    "carrefour" to Color(0xFF004E9F),
    "rewe" to Color(0xFFCC071E),
    "aldi" to Color(0xFF00437B),
    "lidl" to Color(0xFF0050AA),
    "getir" to Color(0xFF5D3EBC),
    "gorillas" to Color(0xFF111111),
    "noon" to Color(0xFFFEEE00),
    "talabat" to Color(0xFFFF5A00),
    "carrefour" to Color(0xFF004E9F),
    "ebay" to Color(0xFFE53238),
    "shopee" to Color(0xFFEE4D2D),
    "rakuten" to Color(0xFFBF0000),
    "mercado" to Color(0xFFFFE600),
    "coupang" to Color(0xFFE60012)
)

private val FALLBACK_COLORS = listOf(
    Color(0xFF6B8F71), Color(0xFF3B82F6), Color(0xFFC6A15B), Color(0xFFEF4444),
    Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899), Color(0xFF6366F1)
)

private fun vendorColor(name: String): Color {
    val lower = name.lowercase()
    VENDOR_COLORS.firstOrNull { lower.contains(it.first) }?.let { return it.second }
    return FALLBACK_COLORS[name.hashCode().mod(FALLBACK_COLORS.size)]
}

/** White or dark text depending on the background's luminance, so the initial stays legible. */
private fun onColorFor(bg: Color): Color {
    val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (luminance > 0.6f) Color(0xFF2F3437) else Color.White
}

@Composable
fun VendorIcon(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val bg = vendorColor(name)
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = onColorFor(bg),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp
        )
    }
}
