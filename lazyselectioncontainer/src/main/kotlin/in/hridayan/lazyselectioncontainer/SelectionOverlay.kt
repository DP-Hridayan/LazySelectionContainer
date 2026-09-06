package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

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
    toolbar: LazySelectionToolbar,
    onCopy: ((success: Boolean) -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
) {
    if (!selectionState.handlesVisible) return

    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val hapticsConfig = LocalLazySelectionHaptics.current ?: LazySelectionDefaults.haptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val edgeThresholdPx = with(density) { autoScrollEdgeThreshold.toPx() }
    val scrollSpeedPx = with(density) { autoScrollSpeed.toPx() }

    val dragAnchor = remember { Ref<Pair<Int, Int>?>(null) }

    val performSelectionHaptic = {
        if (hapticsConfig.enabled) {
            hapticsConfig.hapticFeedbackType?.let {
                haptic.performHapticFeedback(it)
            }
        }
    }

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
        onSelectAll?.invoke()
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
        SelectionDragHandle(
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

                    val newSel = LineSelection(
                        startLine = result.first,
                        startOffset = result.second,
                        endLine = anchor.first,
                        endOffset = anchor.second,
                    )
                    if (selectionState.selection != newSel) {
                        performSelectionHaptic()
                        selectionState.selection = newSel
                    }
                } else if (result != null) {
                    updateSelectionForVisualHandle(
                        selectionState,
                        isStartVisual = true,
                        line = result.first,
                        offset = result.second,
                        onSelectionChanged = performSelectionHaptic
                    )
                }
            },
        )

        SelectionDragHandle(
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

                    val newSel = LineSelection(
                        startLine = anchor.first,
                        startOffset = anchor.second,
                        endLine = result.first,
                        endOffset = result.second,
                    )

                    if (selectionState.selection != newSel) {
                        performSelectionHaptic()
                        selectionState.selection = newSel
                    }
                } else if (result != null) {
                    updateSelectionForVisualHandle(
                        selectionState,
                        isStartVisual = false,
                        line = result.first,
                        offset = result.second,
                        onSelectionChanged = performSelectionHaptic
                    )
                }
            },
        )

        if (selectionState.toolbarVisible) {
            val toolbarAnchorProvider = remember(selectionState, toolbar.behavior) {
                {
                    selectionState.lineInfoVersion
                    when (toolbar.behavior) {
                        LazySelectionToolbarBehavior.PinnedToDragHandle -> {
                            val offset = when (selectionState.toolbarPinnedEnd) {
                                SelectionEnd.START -> startPosProvider()
                                SelectionEnd.END -> endPosProvider()
                            }
                            offset?.let { Pair(it, 0f) }
                        }

                        LazySelectionToolbarBehavior.FloatingAboveSelection -> {
                            computeToolbarAnchor(selectionState)
                        }
                    }
                }
            }

            SelectionToolbar(
                anchorProvider = toolbarAnchorProvider,
                containerWidthPx = selectionState.containerWidthPx,
                containerHeightPx = selectionState.containerHeightPx,
                shape = toolbar.shape,
                containerColor = toolbar.containerColor,
                contentColor = toolbar.contentColor,
                behavior = toolbar.behavior,
                onCopy = ::doCopy,
                onSelectAll = ::doSelectAll,
            )
        }
    }
}