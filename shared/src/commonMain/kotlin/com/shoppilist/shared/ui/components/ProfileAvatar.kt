package com.shoppilist.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/** A stable, pleasant palette for seeded avatars (readable with white text). */
private val AVATAR_COLORS = listOf(
    Color(0xFF2ECC71), Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFFEF4444),
    Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899), Color(0xFF6366F1)
)

private fun avatarColorFor(seed: String): Color {
    if (seed.isEmpty()) return AVATAR_COLORS[0]
    val idx = (seed.hashCode().mod(AVATAR_COLORS.size))
    return AVATAR_COLORS[idx]
}

/**
 * A circular user avatar showing the initial on a seed-derived color, or a neutral person icon
 * when there's no name yet (screenshot b: "user's avatar or blank picture"). Shared by the
 * dashboard top bar, list-card collaborator rows, and the Profile header.
 */
@Composable
fun ProfileAvatar(
    initial: String?,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val letter = initial?.trim()?.takeIf { it.isNotEmpty() }?.take(1)?.uppercase()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (letter != null) avatarColorFor(seed) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.42f).sp
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
