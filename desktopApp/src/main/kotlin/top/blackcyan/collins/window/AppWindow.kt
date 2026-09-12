package top.blackcyan.collins.window

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.composables.ui.theme.ColorScheme
import com.composables.ui.theme.LocalColorScheme
import java.awt.Dimension

private val InitialWindowSize = DpSize(1100.dp, 760.dp)

/** Transparent gap around the floating surface; doubles as the resize grip zone. */
private val FloatingOuterMargin = 12.dp
private val FloatingCornerRadius = 10.dp
private val FloatingBorderWidth = 1.dp
private val FloatingShadowElevation = 8.dp

/**
 * Wider than [FloatingOuterMargin] so the resize strip also covers the visual
 * edge of the rounded surface (the strip itself is measured from the very
 * edge of the transparent window).
 */
private val FloatingResizerThickness = 16.dp

private val WindowMinimumSize = Dimension(640, 460)

/**
 * Entry point: creates the native window and hosts the custom frame.
 *
 * Window geometry state (floating/maximized/minimized, size, position) is
 * owned entirely by [WindowState]; the frame only reads [WindowState.placement]
 * and requests changes through it. The native window manager performs the
 * actual maximize/restore, including restoring the previous bounds.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun AppWindow(
    title: String = "Collins Dictionary",
    icon: Painter? = null,
    content: @Composable () -> Unit,
) = application {
    val windowState = rememberWindowState(size = InitialWindowSize)
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = title,
        icon = icon,
        // Undecorated enables Compose's own edge resize strips (no JNA/native
        // hit-testing). The strip is collapsed while maximized.
        decoration = WindowDecoration.Undecorated(
            resizerThickness = if (isMaximized) 0.dp else FloatingResizerThickness,
        ),
        // Transparent so the rounded corners / outer margin of the floating
        // surface show real desktop behind them.
        transparent = true,
        resizable = true,
    ) {
        LaunchedEffect(window) {
            window.minimumSize = WindowMinimumSize
        }

        CustomWindowFrame(
            title = title,
            appIcon = icon,
            isMaximized = isMaximized,
            onTogglePlacement = {
                windowState.placement =
                    if (windowState.placement == WindowPlacement.Maximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
            },
            onMinimize = { windowState.isMinimized = true },
            onClose = ::exitApplication,
            content = content,
        )
    }
}

/**
 * The custom window frame: a floating surface (rounded corners, outer margin,
 * shadow, hairline border) or an edge-to-edge surface while maximized. All
 * geometry comes from the window manager through [WindowState]; this
 * composable only changes the visuals.
 */
@Composable
private fun WindowScope.CustomWindowFrame(
    title: String,
    appIcon: Painter?,
    isMaximized: Boolean,
    onTogglePlacement: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dark = LocalColorScheme.current == ColorScheme.Dark
    val backgroundColor = if (dark) Color(0xFF0A0A0A) else Color(0xFFF4F4F4)
    val borderColor = if (dark) Color(0x1FFFFFFF) else Color(0x14000000)
    val shape = RoundedCornerShape(if (isMaximized) 0.dp else FloatingCornerRadius)
    val outerPadding = if (isMaximized) 0.dp else FloatingOuterMargin

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(all = outerPadding)
                .shadow(
                    elevation = if (isMaximized) 0.dp else FloatingShadowElevation,
                    shape = shape,
                    clip = false,
                )
                .clip(shape)
                .background(backgroundColor)
                .border(
                    width = if (isMaximized) 0.dp else FloatingBorderWidth,
                    color = borderColor,
                    shape = shape,
                ),
        ) {
            CustomTitleBar(
                title = title,
                appIcon = appIcon,
                isMaximized = isMaximized,
                onTogglePlacement = onTogglePlacement,
                onMinimize = onMinimize,
                onClose = onClose,
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                content()
            }
        }
    }
}
