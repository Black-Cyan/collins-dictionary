package top.blackcyan.collins

import java.awt.Desktop
import java.net.URI

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getCollinsAccessKey(): String =
    System.getenv("COLLINS_ACCESS_KEY") ?: ""

actual fun openUrl(url: String) {
    val uri = URI(url)
    // AWT Desktop is unavailable on some minimal/headless sessions; xdg-open
    // (XDG_ACTIVATION handles focus stealing) is the reliable fallback.
    val browsed = runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri)
            true
        } else {
            false
        }
    }.getOrDefault(false)
    if (!browsed) {
        ProcessBuilder("xdg-open", url).start()
    }
}
