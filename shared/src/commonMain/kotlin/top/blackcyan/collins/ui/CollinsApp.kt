package top.blackcyan.collins.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.backhandler.BackHandler
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composeunstyled.theme.Theme
import top.blackcyan.collins.SystemBarIconAppearanceEffect
import top.blackcyan.collins.audio.AudioPlayer
import top.blackcyan.collins.domain.Headword
import top.blackcyan.collins.state.CollinsAppUiState
import top.blackcyan.collins.state.CollinsScreen
import top.blackcyan.collins.state.SettingsUiState
import com.composables.ui.theme.ColorScheme
import com.composables.ui.theme.LocalColorScheme

private const val TRANSITION_DURATION = 300
/** Maximum content width before centering kicks in (readability on wide screens). */
private val CONTENT_MAX_WIDTH = 720.dp

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CollinsApp(
    uiState: CollinsAppUiState,
    settingsState: SettingsUiState,
    audioPlayer: AudioPlayer,
    onDictionarySelect: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onHeadwordClick: (Headword) -> Unit,
    onBack: () -> Unit,
    onNearbyEntryClick: (String) -> Unit,
    onEntryReferenceClick: (String) -> Unit,
    onTargetEntryConsumed: () -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsBack: () -> Unit,
    onAboutClick: () -> Unit,
    onAboutBack: () -> Unit,
    onOssLicensesClick: () -> Unit,
    onOssLicensesBack: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Handle system back button — only intercept on sub-screens
    val canGoBack = uiState.screen is CollinsScreen.EntryDetail ||
        uiState.screen is CollinsScreen.Settings ||
        uiState.screen is CollinsScreen.About ||
        uiState.screen is CollinsScreen.OssLicenses
    BackHandler(enabled = canGoBack) {
        when (uiState.screen) {
            is CollinsScreen.EntryDetail -> onBack()
            is CollinsScreen.Settings -> onSettingsBack()
            is CollinsScreen.About -> onAboutBack()
            is CollinsScreen.OssLicenses -> onOssLicensesBack()
            else -> {}
        }
    }

    val darkColorScheme = LocalColorScheme.current == ColorScheme.Dark
    val screen = uiState.screen
    // Splash paints the primary color edge-to-edge; content screens sit on the
    // background color, so bar icons switch polarity with the screen.
    val darkSystemBarIcons = if (screen is CollinsScreen.Splash) darkColorScheme else !darkColorScheme
    SystemBarIconAppearanceEffect(darkSystemBarIcons)

    // The root fills the whole display (including system-bar areas) so no
    // window background ever shows through. System-bar padding is applied only
    // to content screens; the splash stays full-bleed.
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Theme[colors][backgroundColor]),
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                when {
                    initialState is CollinsScreen.Splash && targetState is CollinsScreen.SearchHome -> {
                        fadeIn(tween(TRANSITION_DURATION)) togetherWith
                            fadeOut(tween(TRANSITION_DURATION))
                    }
                    // Entering screens slide in; the exiting screen is hidden
                    // immediately so the two trees never render side-by-side.
                    initialState is CollinsScreen.SearchHome && targetState is CollinsScreen.EntryDetail -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { it } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.EntryDetail && targetState is CollinsScreen.SearchHome -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { -it / 3 } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.SearchHome && targetState is CollinsScreen.Settings -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { it } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.Settings && targetState is CollinsScreen.SearchHome -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { -it / 3 } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.Settings && targetState is CollinsScreen.About -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { it } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.About && targetState is CollinsScreen.Settings -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { -it / 3 } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.About && targetState is CollinsScreen.OssLicenses -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { it } togetherWith
                            ExitTransition.None
                    }
                    initialState is CollinsScreen.OssLicenses && targetState is CollinsScreen.About -> {
                        slideInHorizontally(tween(TRANSITION_DURATION)) { -it / 3 } togetherWith
                            ExitTransition.None
                    }
                    else -> {
                        fadeIn(tween(TRANSITION_DURATION)) togetherWith
                            fadeOut(tween(TRANSITION_DURATION))
                    }
                }
            },
            label = "screenTransition",
        ) { screen ->
            if (screen is CollinsScreen.Splash) {
                SplashScreen(
                    onSplashFinished = onSplashFinished,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Responsive content: on wide screens (desktop) the content
                // area is capped at CONTENT_MAX_WIDTH and centered; on narrow
                // screens (mobile) it fills the full width.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = CONTENT_MAX_WIDTH),
                    ) {
                        when (screen) {
                            is CollinsScreen.SearchHome -> {
                                SearchHomeScreen(
                                    dictionaries = uiState.dictionaries,
                                    selectedDictionaryCode = uiState.selectedDictionaryCode,
                                    dictionariesError = uiState.dictionariesError,
                                    searchState = uiState.search,
                                    onDictionarySelect = onDictionarySelect,
                                    onQueryChange = onQueryChange,
                                    onSearch = onSearch,
                                    onHeadwordClick = onHeadwordClick,
                                    onSettingsClick = onSettingsClick,
                                )
                            }
                            is CollinsScreen.EntryDetail -> {
                                EntryDetailScreen(
                                    entryState = uiState.entry,
                                    audioPlayer = audioPlayer,
                                    onBack = onBack,
                                    onNearbyEntryClick = onNearbyEntryClick,
                                    onEntryReferenceClick = onEntryReferenceClick,
                                    onTargetEntryConsumed = onTargetEntryConsumed,
                                )
                            }
                            is CollinsScreen.Settings -> {
                                SettingsScreen(
                                    settingsState = settingsState,
                                    dictionaries = uiState.dictionaries,
                                    selectedDictionaryCode = uiState.selectedDictionaryCode,
                                    onDictionarySelect = onDictionarySelect,
                                    onSaveApiKey = onSaveApiKey,
                                    onClearApiKey = onClearApiKey,
                                    onBack = onSettingsBack,
                                    onAboutClick = onAboutClick,
                                )
                            }
                            is CollinsScreen.About -> {
                                AboutScreen(
                                    onBack = onAboutBack,
                                    onOpenOssLicenses = onOssLicensesClick,
                                )
                            }
                            is CollinsScreen.OssLicenses -> {
                                OssLicensesScreen(onBack = onOssLicensesBack)
                            }
                            is CollinsScreen.Splash -> error("unreachable")
                        }
                    }
                }
            }
        }
    }
}
