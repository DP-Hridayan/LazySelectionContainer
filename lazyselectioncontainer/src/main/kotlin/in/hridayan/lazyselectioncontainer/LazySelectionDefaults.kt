package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for colors used in text selection.
 */
data class LazySelectionColors(
    val highlightColor: Color,
    val dragHandleColor: Color
)

/**
 * Configuration for edge auto-scrolling during text selection drag.
 */
data class LazySelectionAutoScroll(
    val scrollThreshold: Dp,
    val scrollSpeed: Dp
)

/**
 * Configuration for the magnifier loupe used during text selection.
 */
data class LazySelectionMagnifier(
    val verticalOffset: Dp,
    val cornerRadius: Dp
)

/**
 * Defines the positioning behavior of the selection toolbar.
 */
enum class LazySelectionToolbarBehavior {
    /**
     * Pins the toolbar near the active drag handle (start or end).
     */
    PinnedToDragHandle,

    /**
     * Floats the toolbar natively above the topmost visible line of the selection.
     */
    FloatingAboveSelection
}

/**
 * Configuration for the selection toolbar (Copy / Select All).
 */
data class LazySelectionToolbar(
    val behavior: LazySelectionToolbarBehavior,
    val shape: Shape,
    val containerColor: Color,
    val contentColor: Color,
    val textStyle: TextStyle
)

/**
 * Default configurations for [LazySelectionContainer].
 */
object LazySelectionDefaults {
    /**
     * Creates a [LazySelectionColors] with the default text selection highlight and handle colors.
     */
    @Composable
    fun colors(
        highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        dragHandleColor: Color = MaterialTheme.colorScheme.primary
    ): LazySelectionColors = LazySelectionColors(
        highlightColor = highlightColor,
        dragHandleColor = dragHandleColor
    )

    /**
     * Creates a [LazySelectionAutoScroll] with the default auto-scroll behavior parameters.
     */
    fun autoScroll(
        scrollThreshold: Dp = 48.dp,
        scrollSpeed: Dp = 6.dp
    ): LazySelectionAutoScroll = LazySelectionAutoScroll(
        scrollThreshold = scrollThreshold,
        scrollSpeed = scrollSpeed
    )

    /**
     * Creates a [LazySelectionMagnifier] with the default magnifier behavior parameters.
     */
    fun magnifier(
        verticalOffset: Dp = 36.dp,
        cornerRadius: Dp = 24.dp
    ): LazySelectionMagnifier = LazySelectionMagnifier(
        verticalOffset = verticalOffset,
        cornerRadius = cornerRadius
    )

    /**
     * Creates a [LazySelectionToolbar] with the default toolbar behavior parameters.
     */
    @Composable
    fun toolbar(
        behavior: LazySelectionToolbarBehavior = LazySelectionToolbarBehavior.FloatingAboveSelection,
        shape: Shape = RoundedCornerShape(50),
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        textStyle: TextStyle = MaterialTheme.typography.bodyMedium
    ): LazySelectionToolbar = LazySelectionToolbar(
        behavior = behavior,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        textStyle = textStyle
    )
}