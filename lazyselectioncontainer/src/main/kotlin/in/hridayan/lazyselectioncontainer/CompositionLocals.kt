package `in`.hridayan.lazyselectioncontainer

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalLazySelectionState = staticCompositionLocalOf<LazySelectionState?> { null }
internal val LocalLazySelectionColors = staticCompositionLocalOf<LazySelectionColors?> { null }
internal val LocalLazySelectionHaptics = staticCompositionLocalOf<LazySelectionHaptics?> { null }