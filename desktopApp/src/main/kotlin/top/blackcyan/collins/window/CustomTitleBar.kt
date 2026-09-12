package top.blackcyan.collins.window

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.X
import com.composables.ui.components.Icon
import com.composables.ui.theme.ColorScheme
import com.composables.ui.theme.LocalColorScheme

private val TitleBarHeight = 40.dp
private val CaptionButtonWidth = 46.dp
private val CaptionButtonHeight = 32.dp
private val CaptionButtonCorner = 6.dp
private val CaptionControlsGap = 2.dp
private val CaptionIconSize = 15.dp

private val CloseHoverColor = Color(0xFFE81123)

/**
 * Custom, flat title bar that replaces the native one. Dragging and
 * double-click-to-maximize are handled by [WindowDraggableArea] / the native
 * window-move API; the caption buttons only display state and invoke callbacks.
 */
@Composable
internal fun WindowScope.CustomTitleBar(
    title: String,
    appIcon: Painter?,
    isMaximized: Boolean,
    onTogglePlacement: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalColorScheme.current == ColorScheme.Dark
    val titleBarColor = if (dark) Color(0xFF0A0A0A) else Color(0xFFF4F4F4)
    val titleTextColor = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val iconColor = if (dark) Color(0xFFBBBBBB) else Color(0xFF555555)
    val neutralHoverColor = if (dark) Color(0x14FFFFFF) else Color(0x0F000000)
    val hairlineColor = if (dark) Color(0x1FFFFFFF) else Color(0x14000000)

    Box(modifier = modifier.fillMaxWidth().height(TitleBarHeight)) {
        Row(
            modifier = Modifier.fillMaxSize().background(titleBarColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App identity + drag area. The double-tap gesture rides on top of
            // the draggable area and only fires on tap, so plain drags still
            // move the window.
            WindowDraggableArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onTogglePlacement() })
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (appIcon != null) {
                        Image(
                            painter = appIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Box(Modifier.width(8.dp))
                    }
                    BasicText(
                        text = title,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleTextColor,
                        ),
                    )
                }
            }

            WindowControls(
                isMaximized = isMaximized,
                iconColor = iconColor,
                neutralHoverColor = neutralHoverColor,
                onMinimize = onMinimize,
                onTogglePlacement = onTogglePlacement,
                onClose = onClose,
            )
        }

        // Hairline separating the title bar from the content.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(hairlineColor),
        )
    }
}

@Composable
private fun WindowControls(
    isMaximized: Boolean,
    iconColor: Color,
    neutralHoverColor: Color,
    onMinimize: () -> Unit,
    onTogglePlacement: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CaptionControlsGap),
    ) {
        CaptionButton(hoverColor = neutralHoverColor, onClick = onMinimize) {
            Icon(
                imageVector = Lucide.Minus,
                contentDescription = "Minimize",
                modifier = Modifier.size(CaptionIconSize),
                tint = iconColor,
            )
        }
        CaptionButton(hoverColor = neutralHoverColor, onClick = onTogglePlacement) {
            Icon(
                // Overlapping squares communicate "restore"; a single square is
                // shown while floating.
                imageVector = if (isMaximized) Lucide.Copy else Lucide.Square,
                contentDescription = if (isMaximized) "Restore" else "Maximize",
                modifier = Modifier.size(CaptionIconSize),
                tint = iconColor,
            )
        }
        CaptionButton(hoverColor = CloseHoverColor, onClick = onClose) { hovered ->
            Icon(
                imageVector = Lucide.X,
                contentDescription = "Close",
                modifier = Modifier.size(CaptionIconSize),
                tint = if (hovered) Color.White else iconColor,
            )
        }
    }
}

@Composable
private fun CaptionButton(
    hoverColor: Color,
    onClick: () -> Unit,
    content: @Composable (hovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(width = CaptionButtonWidth, height = CaptionButtonHeight)
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(CaptionButtonCorner))
            .background(if (isHovered) hoverColor else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        content(isHovered)
    }
}
