package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * A draggable selection handle.
 */
@Composable
internal fun SelectionDragHandle(
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

private val startHandleShape =
    RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 0,
        bottomEndPercent = 50,
        bottomStartPercent = 50
    )
private val endHandleShape =
    RoundedCornerShape(
        topStartPercent = 0,
        topEndPercent = 50,
        bottomEndPercent = 50,
        bottomStartPercent = 50
    )