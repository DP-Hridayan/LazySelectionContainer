package `in`.hridayan.lazyselectioncontainer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult

/**
 * Internal representation of a line's layout information used for selection hit-testing
 * and rendering highlights.
 */
internal data class LineInfo(val boundsInWindow: Rect, val layoutResult: TextLayoutResult)

@Composable
fun rememberLazySelectionTextLayout(index: Int): (TextLayoutResult) -> Unit {
    val selectionState = LocalLazySelectionState.current
        ?: error("rememberLazySelectionTextLayout must be used inside a LazySelectionContainer")
    return remember(selectionState, index) { selectionState.onTextLayout(index) }
}

/**
 * Registers an item's layout for text selection and renders its selection highlight.
 * Must be attached to each selectable [Text] component.
 *
 * @param index The index of the item in the list.
 */
@Composable
fun Modifier.lazySelectionItem(index: Int): Modifier = composed {

    val selectionState = LocalLazySelectionState.current
        ?: error("Modifier.lazySelectionItem must be used inside a LazySelectionContainer")

    val colors = LocalLazySelectionColors.current ?: LazySelectionDefaults.colors()

    DisposableEffect(index) {
        onDispose { selectionState.releaseItem(index) }
    }

    this
        .onGloballyPositioned { coords ->
            selectionState.updateBounds(index, coords.boundsInWindow())
        }
        .drawBehind {
            val info = selectionState.lineInfos[index] ?: return@drawBehind
            val result = info.layoutResult
            val len = result.layoutInput.text.text.length
            val sel = selectionState.selection?.normalized() ?: return@drawBehind
            if (index < sel.startLine || index > sel.endLine) return@drawBehind

            val start = (if (index == sel.startLine) sel.startOffset else 0).coerceIn(0, len)
            val end = (if (index == sel.endLine) sel.endOffset else len).coerceIn(0, len)
            if (start == end) return@drawBehind

            drawPath(
                result.getPathForRange(minOf(start, end), maxOf(start, end)),
                color = colors.highlightColor
            )
        }
}