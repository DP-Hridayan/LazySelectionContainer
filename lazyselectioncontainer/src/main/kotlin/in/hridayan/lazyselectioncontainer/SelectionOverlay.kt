package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the end of the selection that the toolbar should be anchored to.
 */
enum class SelectionEnd { START, END }

/**
 * Overlay that renders selection handles and the selection toolbar.
 */
@Composable
internal fun <T> SelectionHandlesOverlay(
    modifier: Modifier = Modifier,
    selectionState: LazySelectionState,
    items: List<T>,
    itemToText: (T) -> String,
    listState: LazyListState,
    handleColor: Color,
    autoScrollEdgeThreshold: Dp,
    autoScrollSpeed: Dp,
    onCopy: ((success: Boolean) -> Unit)? = null,
) {
    if (!selectionState.handlesVisible) return

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val edgeThresholdPx = with(density) { autoScrollEdgeThreshold.toPx() }
    val scrollSpeedPx = with(density) { autoScrollSpeed.toPx() }

    // The non-dragged handle's (line, offset) � captured on drag-start,
    // read by onDragTo so the anchor stays fixed even when handles cross.
    val dragAnchor = remember { Ref<Pair<Int, Int>?>(null) }

    fun doCopy() {
        scope.launch {
            val sel = selectionState.selection?.normalized() ?: return@launch
            copySelection(clipboard, items, itemToText, sel, onCopy)
        }
        selectionState.toolbarVisible = false
    }

    fun doSelectAll() {
        selectAll(selectionState, items)
        selectionState.toolbarVisible = false
        selectionState.toolbarPinnedEnd = SelectionEnd.START
        scope.launch { listState.scrollToItem(0) }
    }

    val startPosProvider = remember(selectionState) {
        {
            selectionState.lineInfoVersion
            val sel = selectionState.selection?.normalized()
            if (sel != null) computeHandleOffset(
                selectionState,
                sel.startLine,
                sel.startOffset
            ) else null
        }
    }

    val endPosProvider = remember(selectionState) {
        {
            selectionState.lineInfoVersion
            val sel = selectionState.selection?.normalized()
            if (sel != null) computeHandleOffset(
                selectionState,
                sel.endLine,
                sel.endOffset
            ) else null
        }
    }

    Box(
        modifier = modifier.zIndex(10f)
    ) {
        SelectionHandle(
            positionProvider = startPosProvider,
            isStartVisual = true,
            color = handleColor,
            selectionState = selectionState,
            listState = listState,
            edgeThresholdPx = edgeThresholdPx,
            scrollSpeedPx = scrollSpeedPx,
            onTap = {
                selectionState.toolbarVisible =
                    !(selectionState.toolbarVisible && selectionState.toolbarPinnedEnd == SelectionEnd.START)
                selectionState.toolbarPinnedEnd = SelectionEnd.START
            },
            onDragStart = {
                val norm = selectionState.selection?.normalized()
                dragAnchor.value = norm?.let { it.endLine to it.endOffset }
                selectionState.toolbarVisible = false
            },
            onDragEnd = {
                val finalSel = selectionState.selection
                // After crossover the dragged position is the normalized
                // end, so pin the toolbar there; otherwise keep it at START.
                selectionState.toolbarPinnedEnd =
                    if (finalSel != null && finalSel != finalSel.normalized()) SelectionEnd.END
                    else SelectionEnd.START
                selectionState.toolbarVisible = true
                selectionState.magnifierOffset = null
                dragAnchor.value = null
            },
            onDragTo = { rawPos ->
                val global = rawPos + selectionState.containerWindowOrigin
                val result = findLineOffsetOrNearestEdge(selectionState, global)
                val anchor = dragAnchor.value
                if (result != null && anchor != null) {
                    val magResult = computeMagnifierOffset(
                        selectionState, result.first, result.second, rawPos.x
                    )
                    if (magResult != null) {
                        selectionState.magnifierOffset = magResult.first
                        selectionState.magnifierLineHeightPx = magResult.second
                    } else {
                        selectionState.magnifierOffset = null
                    }
                    // Dragged position = start, anchor = end.
                    // Normalization swaps them for display if they cross.
                    selectionState.selection = LineSelection(
                        startLine = result.first,
                        startOffset = result.second,
                        endLine = anchor.first,
                        endOffset = anchor.second,
                    )
                } else if (result != null) {
                    // Fallback before anchor is captured (loopJob fires
                    // before onDragStart on the very first frame).
                    updateSelectionForVisualHandle(
                        selectionState,
                        isStartVisual = true,
                        line = result.first,
                        offset = result.second
                    )
                }
            },
        )

        SelectionHandle(
            positionProvider = endPosProvider,
            isStartVisual = false,
            color = handleColor,
            selectionState = selectionState,
            listState = listState,
            edgeThresholdPx = edgeThresholdPx,
            scrollSpeedPx = scrollSpeedPx,
            onTap = {
                selectionState.toolbarVisible =
                    !(selectionState.toolbarVisible && selectionState.toolbarPinnedEnd == SelectionEnd.END)
                selectionState.toolbarPinnedEnd = SelectionEnd.END
            },
            onDragStart = {
                val norm = selectionState.selection?.normalized()
                dragAnchor.value = norm?.let { it.startLine to it.startOffset }
                selectionState.toolbarVisible = false
            },
            onDragEnd = {
                val finalSel = selectionState.selection
                // After crossover the dragged position is the normalized
                // start, so pin the toolbar there; otherwise keep it at END.
                selectionState.toolbarPinnedEnd =
                    if (finalSel != null && finalSel != finalSel.normalized()) SelectionEnd.START
                    else SelectionEnd.END
                selectionState.toolbarVisible = true
                selectionState.magnifierOffset = null
                dragAnchor.value = null
            },
            onDragTo = { rawPos ->
                val global = rawPos + selectionState.containerWindowOrigin
                val result = findLineOffsetOrNearestEdge(selectionState, global)
                val anchor = dragAnchor.value
                if (result != null && anchor != null) {
                    val magResult = computeMagnifierOffset(
                        selectionState, result.first, result.second, rawPos.x
                    )
                    if (magResult != null) {
                        selectionState.magnifierOffset = magResult.first
                        selectionState.magnifierLineHeightPx = magResult.second
                    } else {
                        selectionState.magnifierOffset = null
                    }
                    // Anchor = start, dragged position = end.
                    // Normalization swaps them for display if they cross.
                    selectionState.selection = LineSelection(
                        startLine = anchor.first,
                        startOffset = anchor.second,
                        endLine = result.first,
                        endOffset = result.second,
                    )
                } else if (result != null) {
                    // Fallback before anchor is captured.
                    updateSelectionForVisualHandle(
                        selectionState,
                        isStartVisual = false,
                        line = result.first,
                        offset = result.second
                    )
                }
            },
        )

        if (selectionState.toolbarVisible) {
            val toolbarAnchorProvider = remember(selectionState) {
                {
                    when (selectionState.toolbarPinnedEnd) {
                        SelectionEnd.START -> startPosProvider()
                        SelectionEnd.END -> endPosProvider()
                    }
                }
            }

            SelectionToolbar(
                anchorProvider = toolbarAnchorProvider,
                containerWidthPx = selectionState.containerWidthPx,
                containerHeightPx = selectionState.containerHeightPx,
                onCopy = ::doCopy,
                onSelectAll = ::doSelectAll,
            )
        }
    }
}

internal val startHandleShape =
    RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 0,
        bottomEndPercent = 50,
        bottomStartPercent = 50
    )
internal val endHandleShape =
    RoundedCornerShape(
        topStartPercent = 0,
        topEndPercent = 50,
        bottomEndPercent = 50,
        bottomStartPercent = 50
    )

/**
 * A draggable selection handle.
 */
@Composable
internal fun SelectionHandle(
    positionProvider: () -> Offset?,
    isStartVisual: Boolean,
    color: Color,
    selectionState: LazySelectionState,
    listState: LazyListState,
    edgeThresholdPx: Float,
    scrollSpeedPx: Float,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragTo: (rawPositionInContainer: Offset) -> Unit,
) {
    val density = LocalDensity.current
    val handleVisualSizeDp = 28.dp
    val handleTouchSizeDp = 48.dp
    val coroutineScope = rememberCoroutineScope()
    val handleTouchPx = with(density) { handleTouchSizeDp.toPx() }

    val currentPositionProvider by rememberUpdatedState(positionProvider)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragTo by rememberUpdatedState(onDragTo)

    val popupPositionProvider = remember(isStartVisual, handleTouchPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val pos =
                    positionProvider() ?: return IntOffset(-10000, -10000)
                val x = if (isStartVisual) pos.x - handleTouchPx else pos.x
                return IntOffset(
                    x = anchorBounds.left + x.toInt(),
                    y = anchorBounds.top + pos.y.toInt()
                )
            }
        }
    }

    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(clippingEnabled = false)
    ) {
        Box(
            modifier = Modifier
                .size(handleTouchSizeDp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        var isDragging = false
                        var raw = currentPositionProvider() ?: Offset.Zero
                        val pointerPosRef = Ref(raw)

                        val loopJob = coroutineScope.launch {
                            while (isActive) {
                                autoScrollIfNearEdge(
                                    listState,
                                    pointerPosRef.value.y,
                                    selectionState.containerHeightPx,
                                    edgeThresholdPx,
                                    scrollSpeedPx
                                )
                                currentOnDragTo(pointerPosRef.value)
                                delay(16.milliseconds)
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                break
                            }
                            val delta = change.positionChange()
                            change.consume()
                            if (!isDragging) {
                                isDragging = true
                                currentOnDragStart()
                            }
                            raw += delta
                            pointerPosRef.value = raw
                        }

                        loopJob.cancel()
                        if (isDragging) currentOnDragEnd() else currentOnTap()
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(if (isStartVisual) Alignment.TopEnd else Alignment.TopStart)
                    .size(handleVisualSizeDp)
                    .clip(if (isStartVisual) startHandleShape else endHandleShape)
                    .background(color)
            )
        }
    }
}

/**
 * A floating toolbar providing actions like Copy and Select All for the current selection.
 */
@Composable
internal fun SelectionToolbar(
    anchorProvider: () -> Offset?,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
) {
    val density = LocalDensity.current
    val textStyle = LocalTextStyle.current
    val gapPx = with(density) { textStyle.lineHeight.toPx() }
    val handleTouchPx = with(density) { 48.dp.toPx() }

    val anchorInContainer = anchorProvider() ?: return

    val popupPositionProvider =
        remember(containerWidthPx, containerHeightPx, gapPx, handleTouchPx) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val anchor = anchorProvider() ?: return IntOffset.Zero

                    val absoluteAnchorY = anchorBounds.top + anchor.y
                    val fitsAbove = absoluteAnchorY - gapPx - popupContentSize.height >= 0f

                    val absoluteY = if (fitsAbove) {
                        absoluteAnchorY - gapPx - popupContentSize.height
                    } else {
                        absoluteAnchorY + maxOf(gapPx, handleTouchPx)
                    }

                    val clampedAbsoluteY = absoluteY.toInt().coerceIn(
                        0,
                        (windowSize.height - popupContentSize.height).coerceAtLeast(0)
                    )

                    val absoluteAnchorX = anchorBounds.left + anchor.x
                    val absoluteX = absoluteAnchorX - popupContentSize.width / 2f

                    val clampedAbsoluteX = absoluteX.toInt().coerceIn(
                        0,
                        (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                    )

                    return IntOffset(
                        x = clampedAbsoluteX,
                        y = clampedAbsoluteY
                    )
                }
            }
        }

    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(clippingEnabled = false)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                SelectionToolbarActionButton(
                    name = stringResource(android.R.string.copy),
                    onClick = onCopy
                )

                SelectionToolbarActionButton(
                    name = stringResource(android.R.string.selectAll),
                    onClick = onSelectAll
                )
            }
        }
    }
}

@Composable
internal fun SelectionToolbarActionButton(
    modifier: Modifier = Modifier,
    name: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    TextButton(
        modifier = modifier,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(name)
    }
}
