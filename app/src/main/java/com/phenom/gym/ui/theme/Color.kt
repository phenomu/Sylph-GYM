package com.phenom.gym.ui.theme

import androidx.compose.ui.graphics.Color

// Keep legacy for compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ===== Modern Dark Palette =====
// Smooth, warm-tinted dark with vibrant accents
val BgDark        = Color(0xFF0D0F14)  // main background — near-black with slight blue
val BgCard        = Color(0xFF151821)  // elevated card surface
val BgCardAlt     = Color(0xFF1C2029)  // alt card for contrast
val BgInput       = Color(0xFF11141B)  // input fields

// Accent colors — vivid but not harsh
val AccentPrimary  = Color(0xFF6C5CE7)  // indigo-purple (main brand)
val AccentCyan     = Color(0xFF00CEC9)  // teal-cyan (secondary)
val AccentPink     = Color(0xFFFD79A8)  // warm pink (tertiary)
val AccentGreen    = Color(0xFF00B894)  // mint green (success)
val AccentYellow   = Color(0xFFFDCB6E)  // soft amber (warning)
val AccentOrange   = Color(0xFFE17055)  // coral (danger)

// Text
val TextPrimary    = Color(0xFFF0F3F7)  // headings — off-white
val TextSecondary  = Color(0xFF8B95A7)  // labels — muted gray-blue
val TextTertiary   = Color(0xFF5A6377)  // hints — darker gray

// Borders & dividers
val BorderSubtle   = Color(0xFF252932)  // card borders
val BorderActive   = Color(0xFF3A3F4B)  // input focus borders

// ===== Legacy aliases (for gradual migration) =====
val CyberBg          = BgDark
val CyberBgElevated  = BgCard
val CyberSurface     = BgCardAlt
val CyberPrimary     = AccentPrimary
val CyberSecondary   = AccentPink
val CyberAccent      = AccentCyan
val CyberYellow      = AccentYellow
val CyberGreen       = AccentGreen
val CyberTextPrimary = TextPrimary
val CyberTextSecondary = TextSecondary
val CyberBorder      = BorderSubtle
