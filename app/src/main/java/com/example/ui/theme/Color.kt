package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Frosted Glass Palette (Core Material 3 Brand Colors matching HTML)
val GlassPrimary = Color(0xFF6750A4)      // Rich primary violet
val GlassSecondary = Color(0xFFEADDFF)    // Soft lavender
val GlassTertiary = Color(0xFF625B71)     // Soft gray-violet

// Foreground Texts
val FrostedDarkText = Color(0xFF1D1B20)    // High contrast screen text (almost black)
val FrostedLightText = Color(0xFFF4EFF4)   // High contrast screen text for dark mode

// Light Frosted Glass Gradient Elements
val GlassLightBgStart = Color(0xFFD0BCFF)  // Pale Indigo-Lavender
val GlassLightBgCenter = Color(0xFFF3EDF7) // Lighter violet wash
val GlassLightBgEnd = Color(0xFFB69DF8)    // Soft magenta-lavender

// Dark Frosted Glass Gradient Elements (Deeper, high contrast)
val GlassDarkBgStart = Color(0xFF2E1065)   // Deep Indigo
val GlassDarkBgCenter = Color(0xFF1E1B4B)  // Dark Slate-Violet
val GlassDarkBgEnd = Color(0xFF0F172A)     // Ink slate dark

// Bubble specific color states
val BubbleOutgoingLight = Color(0xFF6750A4)
val BubbleOutgoingDark = Color(0xFF7F67BE)

val BubbleIncomingLight = Color(0xB2FFFFFF) // 70% opacity white for glass refraction
val BubbleIncomingDark = Color(0x33FFFFFF)  // Translucent white-wash list-view borders for dark mode

// Legacy color mappings for seamless backwards compatibility and compilation unit safety
val JetBlack = Color(0xFF0F172A)
val BubbleFaceToFaceUser1 = Color(0xFF6750A4) // Elegant Frosted Purple
val BubbleFaceToFaceUser2 = Color(0xFF0EA5E9) // Elegant Soft Sky Blue

val BubbleUser = Color(0xFF6750A4)
val BubbleFriend = Color(0xE6FFFFFF) // Light mode beautiful milky-white glass
