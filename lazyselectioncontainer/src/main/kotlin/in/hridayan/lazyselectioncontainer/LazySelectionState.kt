package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult

/**
 * A data class representing a selection across lines in a [LazyColumn].
 *
 * @property startLine The index of the item where the selection begins.
 * @property startOffset The character offset within the start item.
 * @property endLine The index of the item where the selection ends.
 * @property endOffset The character offset within the end item.
 */
data class LineSelection(
    val startLine: Int,
    val startOffset: Int,
    val endLine: Int,
    val endOffset: Int,
) {
    /**
     * Returns a normalized version of this selection where the start position
     * is guaranteed to precede or equal the end position in reading order.
     */
    fun normalized(): LineSelection =
        if (startLine < endLine || (startLine == endLine && startOffset <= endOffset)) this
        else LineSelection(endLine, endOffset, startLine, startOffset)
}

/**
 * State holder for text selection within a [LazySelectionContainer].
 * Manages selection coordinates, visibility of handles, and the selection toolbar.
 */
@Stable
class LazySelectionState {
    /**
     * The current selection range, or null if no text is selected.
     */
    var selection by mutableStateOf<LineSelection?>(null)

    /**
     * Controls the visibility of the selection handles.
     */
    var handlesVisible by mutableStateOf(false)

    /**
     * Controls the visibility of the selection toolbar (Copy/Select All).
     */
    var toolbarVisible by mutableStateOf(false)

    /**
     * Specifies which end of the selection the toolbar is currently anchored to.
     */
    var toolbarPinnedEnd by mutableStateOf(SelectionEnd.END)

    /**
     * Position for the magnifier loupe, in container-local coordinates.
     * Non-null only while a drag is in progress (initial selection or handle drag).
     */
    var magnifierOffset by mutableStateOf<Offset?>(null)
    internal var magnifierLineHeightPx by mutableFloatStateOf(0f)

    /**
     * Map of currently composed line indices to their layout information.
     */
    internal val lineInfos = mutableMapOf<Int, LineInfo>()

    /**
     * A version counter used to trigger recomposition of the selection overlay
     * when line layout information changes.
     */
    internal var lineInfoVersion by mutableIntStateOf(0)
    internal var containerWindowOrigin: Offset = Offset.Zero
    internal var containerHeightPx: Float = 0f
    internal var containerWidthPx: Float = 0f

    // --- Layout capture, sourced directly from each item's real Text/onTextLayout ---
    // windowBoundsOf and layoutResultOf are filled independently (onGloballyPositioned
    // vs onTextLayout fire in no guaranteed order on the first frame), and merged into
    // lineInfos once both halves for a given index are available.
    private val windowBoundsOf = mutableMapOf<Int, Rect>()
    private val layoutResultOf = mutableMapOf<Int, TextLayoutResult>()

    /**
     * Records the real [TextLayoutResult] produced by an item's own Text composable.
     * Call this from that Text's `onTextLayout` (see [onTextLayout]) — never re-measure
     * text independently, or the captured layout can drift from what's actually rendered.
     */
    internal fun updateLayout(index: Int, result: TextLayoutResult) {
        layoutResultOf[index] = result
        windowBoundsOf[index]?.let { bounds ->
            lineInfos[index] = LineInfo(bounds, result)
            lineInfoVersion++
        }
    }

    /**
     * Records an item's current on-screen bounds, from `onGloballyPositioned`.
     */
    internal fun updateBounds(index: Int, bounds: Rect) {
        windowBoundsOf[index] = bounds
        layoutResultOf[index]?.let { result ->
            lineInfos[index] = LineInfo(bounds, result)
            lineInfoVersion++
        }
    }

    /**
     * Releases all cached state for an item once it leaves composition
     * (e.g. scrolled out of the Lazy list).
     */
    internal fun releaseItem(index: Int) {
        lineInfos.remove(index)
        windowBoundsOf.remove(index)
        layoutResultOf.remove(index)
        lineInfoVersion++
    }

    /**
     * Returns a callback to pass as `onTextLayout` on the item's real Text composable
     * at [index], so this state captures the exact layout Compose used to render it —
     * no separate `style`/`text` parameters required.
     */
    fun onTextLayout(index: Int): (TextLayoutResult) -> Unit = { result ->
        updateLayout(index, result)
    }

    /**
     * Clears the current selection and hides all selection UI components.
     */
    fun clear() {
        selection = null
        handlesVisible = false
        toolbarVisible = false
        magnifierOffset = null
    }
}

/**
 * Creates and remembers a [LazySelectionState] instance.
 */
@Composable
fun rememberLazySelectionState(): LazySelectionState = remember { LazySelectionState() }

/**
 * Creates and remembers a [LazySelectionState] instance.
 */
@Deprecated(
    message = "Use rememberLazySelectionState() instead.",
    replaceWith = ReplaceWith("rememberLazySelectionState()", "in.hridayan.lazyselectioncontainer.rememberLazySelectionState")
)
@Composable
fun rememberSelectionState(): LazySelectionState = rememberLazySelectionState()

