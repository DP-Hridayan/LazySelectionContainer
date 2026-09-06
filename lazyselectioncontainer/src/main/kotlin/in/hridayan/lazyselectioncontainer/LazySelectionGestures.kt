package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adds text selection gesture support to a [Modifier].
 * This includes long-press to initiate selection and drag to extend it with auto-scroll support.
 *
 * @param selectionState The state holder for the selection.
 * @param items The list of items in the container.
 * @param textOf A function to extract selectable text from an item.
 * @param listState The state of the [LazyColumn].
 * @param autoScrollEdgeThreshold The distance from the edge that triggers auto-scrolling.
 * @param autoScrollSpeed The speed of auto-scrolling.
 */
fun <T> Modifier.textSelectionGestures(
    selectionState: LazySelectionState,
    items: List<T>,
    textOf: (T) -> String,
    listState: LazyListState,
    autoScrollEdgeThreshold: Dp = 48.dp,
    autoScrollSpeed: Dp = 4.dp,
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val hapticsConfig = LocalLazySelectionHaptics.current ?: LazySelectionDefaults.haptics()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val edgeThresholdPx = with(density) { autoScrollEdgeThreshold.toPx() }
    val scrollSpeedPx = with(density) { autoScrollSpeed.toPx() }

    val currentItems by rememberUpdatedState(items)
    val currentTextOf by rememberUpdatedState(textOf)

    val performSelectionHaptic = {
        if (hapticsConfig.enabled) {
            hapticsConfig.hapticFeedbackType?.let {
                haptic.performHapticFeedback(it)
            }
        }
    }

    this
        .onGloballyPositioned { coords ->
            selectionState.containerWindowOrigin = coords.boundsInWindow().topLeft
            selectionState.containerHeightPx = coords.size.height.toFloat()
            selectionState.containerWidthPx = coords.size.width.toFloat()
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.isConsumed) return@awaitEachGesture

                val longPress = awaitLongPressOrCancellation(down.id)

                if (longPress == null) {
                    val isUp = currentEvent.changes.none { it.pressed }
                    if (isUp && selectionState.selection != null) {
                        selectionState.clear()
                    }
                    return@awaitEachGesture
                }

                val startGlobal = longPress.position + selectionState.containerWindowOrigin
                val start = findLineOffset(selectionState, startGlobal) ?: return@awaitEachGesture

                if (hapticsConfig.enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                selectionState.handlesVisible = false
                selectionState.toolbarVisible = false

                val lineText = currentTextOf(currentItems[start.first])
                val wordRange = wordBoundsAt(lineText, start.second)

                selectionState.selection =
                    LineSelection(start.first, wordRange.first, start.first, wordRange.last)

                val pointerPosRef = Ref(longPress.position)
                val magResult1 = computeMagnifierOffset(
                    selectionState, start.first, start.second, longPress.position.x
                )

                if (magResult1 != null) {
                    selectionState.magnifierOffset = magResult1.first
                    selectionState.magnifierLineHeightPx = magResult1.second
                } else {
                    selectionState.magnifierOffset = null
                }

                val initialLine = start.first
                val initialWordRange = wordRange

                val loopJob = coroutineScope.launch {
                    while (isActive) {
                        autoScrollIfNearEdge(
                            listState, pointerPosRef.value.y, selectionState.containerHeightPx,
                            edgeThresholdPx, scrollSpeedPx
                        )

                        val globalPos = pointerPosRef.value + selectionState.containerWindowOrigin
                        val end = findLineOffsetOrNearestEdge(selectionState, globalPos)

                        if (end != null) {
                            val isInsideInitialWord =
                                end.first == initialLine && end.second in initialWordRange

                            val newSel = if (isInsideInitialWord) {
                                LineSelection(
                                    startLine = initialLine, startOffset = initialWordRange.first,
                                    endLine = initialLine, endOffset = initialWordRange.last
                                )
                            } else {
                                val isBefore =
                                    end.first < initialLine || (end.first == initialLine && end.second < initialWordRange.first)

                                if (isBefore) {
                                    LineSelection(
                                        startLine = initialLine,
                                        startOffset = initialWordRange.last,
                                        endLine = end.first,
                                        endOffset = end.second
                                    )
                                } else {
                                    LineSelection(
                                        startLine = initialLine,
                                        startOffset = initialWordRange.first,
                                        endLine = end.first,
                                        endOffset = end.second
                                    )
                                }
                            }
                            if (selectionState.selection != newSel) {
                                performSelectionHaptic()
                                selectionState.selection = newSel
                            }

                            val magResult2 = computeMagnifierOffset(
                                selectionState, end.first, end.second, pointerPosRef.value.x
                            )
                            if (magResult2 != null) {
                                selectionState.magnifierOffset = magResult2.first
                                selectionState.magnifierLineHeightPx = magResult2.second
                            } else {
                                selectionState.magnifierOffset = null
                            }
                        }
                        delay(16.milliseconds)
                    }
                }

                drag(longPress.id) { change ->
                    change.consume()
                    pointerPosRef.value = change.position
                }

                loopJob.cancel()
                selectionState.magnifierOffset = null

                val finalSelection = selectionState.selection
                val hasSelection = finalSelection != null

                selectionState.handlesVisible = hasSelection
                selectionState.toolbarVisible = hasSelection

                if (finalSelection != null) {
                    selectionState.toolbarPinnedEnd =
                        if (finalSelection == finalSelection.normalized()) SelectionEnd.END else SelectionEnd.START
                }
            }
        }
}
