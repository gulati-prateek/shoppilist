package com.shoppilist.shared.ui.theme

import androidx.compose.ui.graphics.Color

// ShoppiList brand palette (product spec §4)
val ShoppiListPrimary = Color(0xFF2ECC71)
val ShoppiListPrimaryDark = Color(0xFF1A9E56)
val ShoppiListSurface = Color(0xFFFFFFFF)
val ShoppiListBackground = Color(0xFFF6F8FA)
val ShoppiListTextPrimary = Color(0xFF1A1D23)
val ShoppiListTextSecondary = Color(0xFF6B7280)
val ShoppiListStrikethrough = Color(0xFF9CA3AF)
val ShoppiListAccentBlue = Color(0xFF3B82F6)
val ShoppiListWarning = Color(0xFFF59E0B)

// Dark-theme surface/background counterparts — not specified by the doc (which only defines a
// light palette), chosen to keep the same hue relationships at Material3-appropriate elevation.
val ShoppiListSurfaceDark = Color(0xFF1E2126)
val ShoppiListBackgroundDark = Color(0xFF14161A)

val ListColorSwatches = listOf(
    ShoppiListPrimary,
    ShoppiListAccentBlue,
    ShoppiListWarning,
    Color(0xFFEF4444),
    Color(0xFF8B5CF6),
    ShoppiListTextSecondary
)
