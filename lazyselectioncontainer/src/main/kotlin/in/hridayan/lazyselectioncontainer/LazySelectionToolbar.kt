package `in`.hridayan.lazyselectioncontainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * A floating toolbar providing actions like Copy and Select All for the current selection.
 */
@Composable
internal fun SelectionToolbar(
    anchorProvider: () -> Pair<Offset, Float>?,
    containerWidthPx: Float,
    containerHeightPx: Float,
    behavior: LazySelectionToolbarBehavior,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
) {
    val density = LocalDensity.current
    val selectableTextStyle = LocalTextStyle.current
    val gapPx = with(density) { selectableTextStyle.lineHeight.toPx() }
    val handleTouchPx = with(density) { 48.dp.toPx() }

    if (anchorProvider() == null) return

    val popupPositionProvider =
        remember(containerWidthPx, containerHeightPx, gapPx, handleTouchPx) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val anchorData = anchorProvider() ?: return IntOffset.Zero
                    val anchor = anchorData.first
                    val lineHeight = anchorData.second

                    val paddingPx = with(density) { 20.dp.toPx() }.toInt()
                    val absoluteAnchorY = anchorBounds.top + anchor.y

                    val absoluteY =
                        if (behavior == LazySelectionToolbarBehavior.FloatingAboveSelection) {
                            val shift = (lineHeight / 2f) + with(density) { 8.dp.toPx() }
                            val fitsAbove =
                                absoluteAnchorY - shift - popupContentSize.height >= paddingPx
                            if (fitsAbove) {
                                absoluteAnchorY - shift - popupContentSize.height
                            } else {
                                absoluteAnchorY + shift
                            }
                        } else {
                            val fitsAbove =
                                absoluteAnchorY - gapPx - popupContentSize.height >= paddingPx
                            if (fitsAbove) {
                                absoluteAnchorY - gapPx - popupContentSize.height
                            } else {
                                absoluteAnchorY + maxOf(gapPx, handleTouchPx)
                            }
                        }

                    val clampedAbsoluteY = absoluteY.toInt().coerceIn(
                        paddingPx,
                        (windowSize.height - popupContentSize.height - paddingPx).coerceAtLeast(
                            paddingPx
                        )
                    )

                    val absoluteAnchorX = anchorBounds.left + anchor.x
                    val absoluteX = absoluteAnchorX - popupContentSize.width / 2f

                    val clampedAbsoluteX = absoluteX.toInt().coerceIn(
                        paddingPx,
                        (windowSize.width - popupContentSize.width - paddingPx).coerceAtLeast(
                            paddingPx
                        )
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
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SelectionToolbarActionButton(
                    name = stringResource(android.R.string.copy),
                    contentColor = contentColor,
                    onClick = onCopy
                )

                SelectionToolbarActionButton(
                    name = stringResource(android.R.string.selectAll),
                    contentColor = contentColor,
                    onClick = onSelectAll
                )
            }
        }
    }
}

@Composable
private fun SelectionToolbarActionButton(
    modifier: Modifier = Modifier,
    name: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    TextButton(
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
    ) {
        Text(text = name)
    }
}