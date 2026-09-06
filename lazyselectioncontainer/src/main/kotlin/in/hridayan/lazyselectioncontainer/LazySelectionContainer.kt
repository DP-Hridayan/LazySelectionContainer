package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset


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
 * @param itemToText A function to extract the string representation of an item. Because lazy lists discard off-screen composables, this function is strictly required so the library can bypass the UI and instantly extract the underlying data when copying large off-screen selections to the clipboard.
 * @param listState The [LazyListState] of the [LazyColumn] inside [content].
 *   Must be the **same** instance passed to the inner [LazyColumn] so that
 *   auto-scroll and handle positioning work correctly.
 * @param modifier Modifier applied to the outer container.
 * @param selectionState The state holder for the selection.
 * @param colors Configuration for colors used in text selection.
 * @param haptics Configuration for haptics used during text selection.
 * @param autoScroll Configuration for edge auto-scrolling during text selection drag.
 * @param magnifier Configuration for the magnifier loupe used during text selection drag.
 * @param toolbar Configuration for the selection toolbar used during text selection.
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
    haptics: LazySelectionHaptics = LazySelectionDefaults.haptics(),
    autoScroll: LazySelectionAutoScroll = LazySelectionDefaults.autoScroll(),
    magnifier: LazySelectionMagnifier = LazySelectionDefaults.magnifier(),
    toolbar: LazySelectionToolbar = LazySelectionDefaults.toolbar(),
    onCopy: ((success: Boolean) -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLazySelectionState provides selectionState,
        LocalLazySelectionColors provides colors,
        LocalLazySelectionHaptics provides haptics
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
                toolbar = toolbar,
                onCopy = onCopy,
                onSelectAll = onSelectAll,
            )
        }
    }
}