package `in`.hridayan.lazyselectioncontainer

import android.content.ClipData
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

/**
 * A generic mutable reference holder for values that need to be accessed across scopes.
 */
internal class Ref<T>(var value: T)

/** Returns the word bounds around the given character offset. */
internal fun wordBoundsAt(text: String, offset: Int): IntRange {
    if (text.isEmpty()) return 0..0
    val clamped = offset.coerceIn(0, text.length)
    val probeIndex = when {
        clamped < text.length && text[clamped].isLetterOrDigit() -> clamped
        clamped > 0 && text[clamped - 1].isLetterOrDigit() -> clamped - 1
        else -> -1
    }
    if (probeIndex == -1) {
        val end = (clamped + 1).coerceAtMost(text.length)
        return clamped.coerceAtMost(end)..end
    }
    var start = probeIndex
    while (start > 0 && text[start - 1].isLetterOrDigit()) start--
    var end = probeIndex + 1
    while (end < text.length && text[end].isLetterOrDigit()) end++
    return start..end
}

/** Triggers list scrolling when the pointer is near the top or bottom edges. */
internal suspend fun autoScrollIfNearEdge(
    listState: LazyListState,
    pointerYInContainer: Float,
    containerHeightPx: Float,
    edgeThresholdPx: Float,
    scrollSpeedPx: Float,
) {
    if (containerHeightPx <= 0f || edgeThresholdPx <= 0f) return
    val distanceFromTop = pointerYInContainer
    val distanceFromBottom = containerHeightPx - pointerYInContainer
    when {
        distanceFromTop < edgeThresholdPx -> {
            val factor = 1f - (distanceFromTop / edgeThresholdPx).coerceIn(0f, 1f)
            listState.scrollBy(-scrollSpeedPx * factor)
        }

        distanceFromBottom < edgeThresholdPx -> {
            val factor = 1f - (distanceFromBottom / edgeThresholdPx).coerceIn(0f, 1f)
            listState.scrollBy(scrollSpeedPx * factor)
        }
    }
}

internal fun updateSelectionForVisualHandle(
    selectionState: LazySelectionState,
    isStartVisual: Boolean,
    line: Int,
    offset: Int,
) {
    val current = selectionState.selection ?: return
    val swapped = current != current.normalized()
    val writeRawStart = if (isStartVisual) !swapped else swapped
    val newSel = if (writeRawStart) current.copy(startLine = line, startOffset = offset)
    else current.copy(endLine = line, endOffset = offset)
    selectionState.selection = newSel
}

internal fun computeHandleOffset(
    selectionState: LazySelectionState,
    line: Int,
    charOffset: Int,
): Offset? {
    val info = selectionState.lineInfos[line] ?: return null
    val textLength = info.layoutResult.layoutInput.text.length
    val cursorRect = info.layoutResult.getCursorRect(charOffset.coerceIn(0, textLength))
    val windowPoint = info.boundsInWindow.topLeft + Offset(cursorRect.left, cursorRect.bottom)
    val local = windowPoint - selectionState.containerWindowOrigin
    if (local.y < 0f || local.y > selectionState.containerHeightPx) {
        return null
    }
    return local
}

internal fun <T> selectAll(selectionState: LazySelectionState, items: List<T>) {
    if (items.isEmpty()) return
    selectionState.selection = LineSelection(0, 0, items.lastIndex, Int.MAX_VALUE)
    selectionState.handlesVisible = true
}

internal suspend fun <T> copySelection(
    clipboard: Clipboard,
    items: List<T>,
    textOf: (T) -> String,
    selection: LineSelection,
    onResult: ((success: Boolean) -> Unit)?,
) {
    val text = buildSelectedText(items, textOf, selection)
    val success = if (text.isEmpty()) {
        false
    } else {
        try {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Selected text", text)))
            true
        } catch (e: Exception) {
            false
        }
    }
    onResult?.invoke(success)
}

internal fun findLineOffset(state: LazySelectionState, globalPos: Offset): Pair<Int, Int>? {
    for ((index, info) in state.lineInfos) {
        val b = info.boundsInWindow
        if (globalPos.y in b.top..b.bottom) {
            val local = Offset(
                x = (globalPos.x - b.left).coerceIn(0f, b.width),
                y = (globalPos.y - b.top).coerceIn(0f, b.height)
            )
            val offset = info.layoutResult.getOffsetForPosition(local)
            return index to offset
        }
    }
    return null
}

internal fun findLineOffsetOrNearestEdge(
    state: LazySelectionState,
    globalPos: Offset
): Pair<Int, Int>? {
    findLineOffset(state, globalPos)?.let { return it }

    if (state.lineInfos.isEmpty()) {
        return null
    }

    val closestEntry = state.lineInfos.entries.minByOrNull { entry ->
        val b = entry.value.boundsInWindow
        when {
            globalPos.y < b.top -> b.top - globalPos.y
            globalPos.y > b.bottom -> globalPos.y - b.bottom
            else -> 0f
        }
    } ?: return null

    val b = closestEntry.value.boundsInWindow
    val local = Offset(
        x = (globalPos.x - b.left).coerceIn(0f, b.width),
        y = (globalPos.y - b.top).coerceIn(0f, b.height)
    )

    val offset = closestEntry.value.layoutResult.getOffsetForPosition(local)
    return closestEntry.key to offset
}

internal fun <T> buildSelectedText(
    items: List<T>,
    textOf: (T) -> String,
    sel: LineSelection
): String {
    val norm = sel.normalized()
    val sb = StringBuilder()
    for (i in norm.startLine..norm.endLine) {
        val line = items.getOrNull(i)?.let(textOf) ?: continue
        val piece = when {
            norm.startLine == norm.endLine -> {
                val s = norm.startOffset.coerceIn(0, line.length)
                val e = norm.endOffset.coerceIn(0, line.length)
                line.substring(minOf(s, e), maxOf(s, e))
            }

            i == norm.startLine -> line.substring(norm.startOffset.coerceIn(0, line.length))
            i == norm.endLine -> line.substring(0, norm.endOffset.coerceIn(0, line.length))
            else -> line
        }
        sb.append(piece)
        if (i != norm.endLine) sb.append("\n")
    }
    return sb.toString()
}

/**
 * Computes the magnifier source-centre for a resolved [lineIndex] + [charOffset].
 * X is set to [fingerX] so the loupe tracks the finger horizontally, while
 * Y is snapped to the vertical centre of the text line � the magnifier only
 * jumps vertically when the selection moves to a different line, matching
 * native Android behaviour.
 */
internal fun computeMagnifierOffset(
    selectionState: LazySelectionState,
    lineIndex: Int,
    charOffset: Int,
    fingerX: Float,
): Pair<Offset, Float>? {
    val info = selectionState.lineInfos[lineIndex] ?: return null
    val textLength = info.layoutResult.layoutInput.text.length
    val safeOffset = charOffset.coerceIn(0, textLength)
    val cursorRect = info.layoutResult.getCursorRect(safeOffset)
    val lineCenterY = (cursorRect.top + cursorRect.bottom) / 2f
    val yInContainer =
        info.boundsInWindow.top + lineCenterY - selectionState.containerWindowOrigin.y
    val lineHeight = cursorRect.bottom - cursorRect.top
    return Pair(Offset(fingerX, yInContainer), lineHeight)
}

/**
 * Computes the native floating anchor for the selection toolbar by finding the topmost
 * visible selected line and returning its vertical center and line height.
 */
internal fun computeToolbarAnchor(
    selectionState: LazySelectionState
): Pair<Offset, Float>? {
    val sel = selectionState.selection?.normalized() ?: return null

    val visibleLines = selectionState.lineInfos.keys.filter { it in sel.startLine..sel.endLine }
    if (visibleLines.isEmpty()) return null
    val topmostVisibleLine = visibleLines.minOrNull() ?: return null

    val info = selectionState.lineInfos[topmostVisibleLine] ?: return null
    val textLength = info.layoutResult.layoutInput.text.length

    val charOffset = if (topmostVisibleLine == sel.startLine) sel.startOffset else 0
    val safeOffset = charOffset.coerceIn(0, textLength)

    val cursorRect = info.layoutResult.getCursorRect(safeOffset)

    val windowPoint = info.boundsInWindow.topLeft + Offset(
        cursorRect.left,
        cursorRect.top + cursorRect.height / 2f
    )
    val local = windowPoint - selectionState.containerWindowOrigin

    val clampedY = local.y.coerceIn(0f, selectionState.containerHeightPx)
    val clampedX = local.x.coerceIn(0f, selectionState.containerWidthPx)

    return Offset(clampedX, clampedY) to cursorRect.height
}
