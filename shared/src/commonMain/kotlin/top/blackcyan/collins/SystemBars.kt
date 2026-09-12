package top.blackcyan.collins

import androidx.compose.runtime.Composable

/**
 * Keeps the system status/navigation bar icon colors readable against the
 * current screen. [darkIcons] requests dark icons (for light backgrounds);
 * `false` requests light icons (for dark backgrounds such as the splash).
 * No-op on non-Android targets.
 */
@Composable
expect fun SystemBarIconAppearanceEffect(darkIcons: Boolean)
