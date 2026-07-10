package com.shoppilist.shared.ui.theme

import androidx.compose.ui.graphics.Color

// ShoppiList "Sage & Gold" palette (from the approved design mockup).
// Identifier names are kept stable across the codebase; only the values changed to the new theme.
val ShoppiListPrimary = Color(0xFF6B8F71)        // Sage green (Primary)
val ShoppiListPrimaryDark = Color(0xFF4F7A5A)    // Deeper green (Success)
val ShoppiListSurface = Color(0xFFFFFFFF)        // Cards / rows
val ShoppiListBackground = Color(0xFFFAF9F6)     // App background (cream)
val ShoppiListMuted = Color(0xFFF7F5F2)          // Secondary muted surface (chips, search)
val ShoppiListTextPrimary = Color(0xFF2F3437)    // Charcoal text
val ShoppiListTextSecondary = Color(0xFF6B7280)
val ShoppiListStrikethrough = Color(0xFF9CA3AF)
val ShoppiListAccentBlue = Color(0xFFC6A15B)     // Gold accent (kept name; now gold, e.g. the + FAB)
val ShoppiListWarning = Color(0xFFC6A15B)        // Gold

// Dark-theme counterparts — the mockup defines a light palette; these keep the sage/gold hue
// relationships at Material3-appropriate elevation.
val ShoppiListSurfaceDark = Color(0xFF23282A)
val ShoppiListBackgroundDark = Color(0xFF181B1D)

val ListColorSwatches = listOf(
    ShoppiListPrimary,
    ShoppiListAccentBlue,
    ShoppiListPrimaryDark,
    Color(0xFFB07C4A),
    Color(0xFF7E8D6B),
    ShoppiListTextSecondary
)
