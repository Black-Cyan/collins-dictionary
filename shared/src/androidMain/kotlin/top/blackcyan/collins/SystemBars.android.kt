package top.blackcyan.collins

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
@Suppress("DEPRECATION") // View.SYSTEM_UI_FLAG_* is the only option on API 24-29 (minSdk 24).
actual fun SystemBarIconAppearanceEffect(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                .filterIsInstance<Activity>()
                .firstOrNull() ?: return@SideEffect

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = activity.window.insetsController
                val appearance = buildList {
                    if (darkIcons) {
                        add(android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                        add(android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                    }
                }.fold(0) { acc, flag -> acc or flag }
                val mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                controller?.setSystemBarsAppearance(appearance, mask)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var flags = view.systemUiVisibility
                val lightStatus = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                val lightNav = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    0
                }
                flags = if (darkIcons) {
                    flags or lightStatus or lightNav
                } else {
                    flags and lightStatus.inv() and lightNav.inv()
                }
                view.systemUiVisibility = flags
            }
        }
    }
}
