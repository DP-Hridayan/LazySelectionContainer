package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
}

/**
 * Wrapper composable that adds cross-line text selection to any [LazyColumn]
 * placed inside [content], analogous to how [SelectionContainer] wraps
 * ordinary composables.
 *
 * Place your own [LazyColumn] (or any scrollable list) inside [content],
 * and apply [lazySelectionItem] to each selectable item. The wrapper
 * automatically provides gesture detection, selection handles, a magnifier
 * loupe, and a copy/select-all toolbar.
 *
 * Example usage:
 * ```kotlin
 * val listState = rememberLazyListState()
 *
 * LazySelectionContainer(
 *     items = messages,
 *     itemToText = { it.content },
 *     listState = listState,
 * ) { selectionState ->
 *     LazyColumn(state = listState) {
 *         itemsIndexed(messages) { index, message ->
 *             Text(
 *                 text = message.content,
 *                 modifier = Modifier.lazySelectionItem(
 *                     selectionState = selectionState,
 *                     index = index,
 *                     text = message.content
 *                 )
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @param items The list of data items backing the selectable content.
 * @param itemToText A function to extract selectable text from an item.
 * @param listState The [LazyListState] of the [LazyColumn] inside [content].
 *   Must be the **same** instance passed to the inner [LazyColumn] so that
 *   auto-scroll and handle positioning work correctly.
 * @param modifier Modifier applied to the outer container.
 * @param selectionState The state holder for the selection.
 * @param colors Configuration for colors used in text selection.
 * @param autoScroll Configuration for edge auto-scrolling during text selection drag.
 * @param magnifier Configuration for the magnifier loupe used during text selection drag.
 * @param onCopy Callback invoked when the user taps "Copy" in the toolbar.
 * @param onSelectAll Callback invoked when the user taps "Select All" in the toolbar.
 * @param content Your composable content – typically a [LazyColumn]. The selection state is provided via LocalLazySelectionState.
 */
@Composable
fun <T> LazySelectionContainer(
    modifier: Modifier = Modifier,
    items: List<T>,
    itemToText: (T) -> String,
    listState: LazyListState,
    selectionState: LazySelectionState = rememberLazySelectionState(),
    colors: LazySelectionColors = LazySelectionDefaults.colors(),
    autoScroll: LazySelectionAutoScroll = LazySelectionDefaults.autoScroll(),
    magnifier: LazySelectionMagnifier = LazySelectionDefaults.magnifier(),
    onCopy: ((success: Boolean) -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLazySelectionState provides selectionState,
        LocalLazySelectionColors provides colors
    ) {
        Box(
            modifier = modifier
                .magnifier(
                    sourceCenter = { selectionState.magnifierOffset ?: Offset.Unspecified },
                    magnifierCenter = {
                        val offset = selectionState.magnifierOffset
                            ?: return@magnifier Offset.Unspecified
                        val shift =
                            (selectionState.magnifierLineHeightPx / 2f) + magnifier.verticalOffset.toPx()
                        offset.copy(y = offset.y - shift)
                    },
                    cornerRadius = magnifier.cornerRadius
                )
                .textSelectionGestures(
                    selectionState = selectionState,
                    items = items,
                    textOf = itemToText,
                    listState = listState,
                    autoScrollEdgeThreshold = autoScroll.scrollThreshold,
                    autoScrollSpeed = autoScroll.scrollSpeed,
                )
        ) {
            content()

            SelectionHandlesOverlay(
                selectionState = selectionState,
                items = items,
                itemToText = itemToText,
                listState = listState,
                handleColor = colors.dragHandleColor,
                autoScrollEdgeThreshold = autoScroll.scrollThreshold,
                autoScrollSpeed = autoScroll.scrollSpeed,
                onCopy = onCopy,
                onSelectAll = onSelectAll
            )
        }
    }
}