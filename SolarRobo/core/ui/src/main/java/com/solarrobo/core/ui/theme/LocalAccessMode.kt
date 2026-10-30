package com.solarrobo.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.solarrobo.core.contracts.AccessMode

/** Read by UI controls for presentation only; physical authorization is enforced again in TrackerSafety. */
val LocalAccessMode = staticCompositionLocalOf { AccessMode.VIEWER }
