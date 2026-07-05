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
    secondary = ShoppiListAccentBlue,
    background = ShoppiListBackground,
    surface = ShoppiListSurface,
    onBackground = ShoppiListTextPrimary,
    onSurface = ShoppiListTextPrimary,
    surfaceVariant = ShoppiListBackground,
    onSurfaceVariant = ShoppiListTextSecondary,
    error = Color(0xFFEF4444),
    tertiary = ShoppiListWarning
)

private val DarkColors = darkColorScheme(
    primary = ShoppiListPrimary,
    onPrimary = ShoppiListTextPrimary,
    secondary = ShoppiListAccentBlue,
    background = ShoppiListBackgroundDark,
    surface = ShoppiListSurfaceDark,
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = ShoppiListSurfaceDark,
    onSurfaceVariant = ShoppiListTextSecondary,
    error = Color(0xFFEF4444),
    tertiary = ShoppiListWarning
)

@Composable
fun ShoppiListTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ShoppiListTypography,
        content = content
    )
}
