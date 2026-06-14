package com.brainsmooch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * "Charm lover Salubra" inspired palette: deep void blue, warm Salubra pink,
 * pale soul-blue and charm-gold accents.
 */
private val SalubraColorScheme = darkColorScheme(
    primary = Color(0xFFF27EB4),            // Salubra pink
    onPrimary = Color(0xFF33101F),
    primaryContainer = Color(0xFF5A2440),
    onPrimaryContainer = Color(0xFFFFD9E6),
    secondary = Color(0xFF9DB2E8),          // pale soul-blue
    onSecondary = Color(0xFF141B33),
    secondaryContainer = Color(0xFF2A3354),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFE8C97E),           // charm-gold glow
    onTertiary = Color(0xFF2E2410),
    tertiaryContainer = Color(0xFF4A3D1E),
    onTertiaryContainer = Color(0xFFFFEFC9),
    background = Color(0xFF0D1020),         // deep void blue-charcoal
    onBackground = Color(0xFFE8E4F0),
    surface = Color(0xFF161B30),            // raised stone-blue
    onSurface = Color(0xFFE8E4F0),
    surfaceVariant = Color(0xFF232A45),
    onSurfaceVariant = Color(0xFFB8B3CC),
    outline = Color(0xFF6E5A78),
    error = Color(0xFFFF8FA3),
    onError = Color(0xFF40101C),
    errorContainer = Color(0xFF4A1F2E),
    onErrorContainer = Color(0xFFFFD9DE)
)

@Composable
fun BrainSmoochTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SalubraColorScheme,
        typography = SalubraTypography,
        shapes = SalubraShapes,
        content = content
    )
}
