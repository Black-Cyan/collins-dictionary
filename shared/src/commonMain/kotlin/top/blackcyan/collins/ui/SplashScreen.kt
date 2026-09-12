package top.blackcyan.collins.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Icon
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.BookOpen
import com.composables.ui.components.Text
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconScale = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    var showTagline by remember { mutableStateOf(false) }

    // Pulsing glow animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    LaunchedEffect(Unit) {
        // Phase 1: Icon bounces in
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        delay(200)

        // Phase 2: Title fades in with slide
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600),
        )

        delay(150)

        // Phase 3: Subtitle fades in
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500),
        )

        delay(200)

        // Phase 4: Show tagline
        showTagline = true

        // Wait, then navigate
        delay(1400)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme[colors][primaryColor]),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Animated book icon with pulse
            Icon(
                imageVector = Lucide.BookOpen,
                contentDescription = "Collins Dictionary",
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale.value * pulseScale)
                    .graphicsLayer { alpha = pulseAlpha },
                tint = Theme[colors][backgroundColor],
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title with slide-in animation
            Box(
                modifier = Modifier
                    .offset(y = (20 * (1f - titleAlpha.value)).dp)
                    .alpha(titleAlpha.value),
            ) {
                Text(
                    text = "Collins Dictionary",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme[colors][backgroundColor],
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Box(
                modifier = Modifier
                    .offset(y = (12 * (1f - subtitleAlpha.value)).dp)
                    .alpha(subtitleAlpha.value),
            ) {
                Text(
                    text = "English at your fingertips",
                    fontSize = 16.sp,
                    color = Theme[colors][backgroundColor].copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Animated tagline
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(tween(400)) + slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { it / 3 },
                ),
                exit = fadeOut(),
            ) {
                Text(
                    text = "Explore words,\ndiscover meaning",
                    fontSize = 14.sp,
                    color = Theme[colors][backgroundColor].copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}
