package com.brainsmooch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.brainsmooch.R

val PoppinsBold = FontFamily(Font(R.font.poppins_bold))
val PoppinsRegular = FontFamily(Font(R.font.poppins_regular))

val SalubraTypography = Typography(
    // Titles - Bold
    displayLarge = TextStyle(
        fontFamily = PoppinsBold,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsBold,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PoppinsBold,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PoppinsBold,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsBold,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsBold,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    // Body - Regular (lighter)
    bodyLarge = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    // Labels
    labelLarge = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)
