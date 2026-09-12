package top.blackcyan.collins

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import top.blackcyan.collins.window.AppWindow

fun main() {
    val appIcon = runCatching {
        object {}::class.java.getResourceAsStream("/images/collins.png")
            ?.let { BitmapPainter(loadImageBitmap(it)) }
    }.getOrNull()

    AppWindow(icon = appIcon) {
        App()
    }
}
