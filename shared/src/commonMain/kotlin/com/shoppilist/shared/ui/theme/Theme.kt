package com.shoppilist.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ShoppiListPrimary,
    onPrimary = ShoppiListSurface,
    primaryContainer = Color(0xFFE3EBE0),
    onPrimaryContainer = ShoppiListPrimaryDark,
    secondary = ShoppiListAccentBlue,           // gold
    onSecondary = ShoppiListSurface,
    tertiary = ShoppiListAccentBlue,             // gold accent (e.g. + FAB)
    onTertiary = ShoppiListSurface,
    background = ShoppiListBackground,
    surface = ShoppiListSurface,
    onBackground = ShoppiListTextPrimary,
    onSurface = ShoppiListTextPrimary,
    surfaceVariant = ShoppiListMuted,
    onSurfaceVariant = ShoppiListTextSecondary,
    secondaryContainer = Color(0xFFEDE7DA),      // soft gold tint (selected chip / banner)
    onSecondaryContainer = ShoppiListTextPrimary,
    outline = Color(0xFFD6D3CC),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = ShoppiListPrimary,
    onPrimary = ShoppiListTextPrimary,
    secondary = ShoppiListAccentBlue,
    tertiary = ShoppiListAccentBlue,
    onTertiary = ShoppiListTextPrimary,
    background = ShoppiListBackgroundDark,
    surface = ShoppiListSurfaceDark,
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = ShoppiListSurfaceDark,
    onSurfaceVariant = ShoppiListTextSecondary,
    error = Color(0xFFEF4444)
)

@Composable
fun ShoppiListTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ShoppiListTypography,
        content = content
    )
}
