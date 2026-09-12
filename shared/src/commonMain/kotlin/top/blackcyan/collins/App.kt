package top.blackcyan.collins

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.composables.ui.theme.ComposablesTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import top.blackcyan.collins.data.CollinsApiConfig
import top.blackcyan.collins.data.CollinsDefaultBaseUrl
import top.blackcyan.collins.data.HttpCollinsApiClient
import top.blackcyan.collins.repository.DefaultCollinsRepository
import top.blackcyan.collins.settings.SettingsRepository
import top.blackcyan.collins.ui.CollinsApp
import top.blackcyan.collins.ui.CollinsAppViewModel
import top.blackcyan.collins.state.CollinsAppState

@Composable
fun App(
    modifier: Modifier = Modifier,
) {
    val settingsRepository = remember { SettingsRepository() }
    val httpClient = remember { HttpClient(OkHttp) }

    // Avatar images are loaded through the shared Ktor/OkHttp client.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = httpClient))
            }
            .build()
    }

    // Build config reactively from current settings. The API base URL is fixed;
    // only the access key is user-configurable.
    val currentApiKey = remember {
        settingsRepository.getApiKey() ?: getCollinsAccessKey()
    }
    var apiKey by remember { mutableStateOf(currentApiKey) }

    val apiConfig = remember(apiKey) {
        CollinsApiConfig(accessKey = apiKey, baseUrl = CollinsDefaultBaseUrl)
    }
    val apiClient = remember(apiConfig) {
        HttpCollinsApiClient(httpClient, apiConfig)
    }
    val repository = remember(apiClient) {
        DefaultCollinsRepository(apiClient)
    }
    val appState = remember(repository) {
        CollinsAppState(repository)
    }
    val viewModel = remember(appState) {
        CollinsAppViewModel(appState, settingsRepository)
    }

    val uiState by viewModel.uiState.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()

    ComposablesTheme {
        CollinsApp(
            uiState = uiState,
            settingsState = settingsState,
            audioPlayer = viewModel.audioPlayer,
            onDictionarySelect = viewModel::onDictionarySelect,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            onHeadwordClick = viewModel::onHeadwordClick,
            onBack = viewModel::onBack,
            onNearbyEntryClick = viewModel::onNearbyEntryClick,
            onEntryReferenceClick = viewModel::onEntryReferenceClick,
            onTargetEntryConsumed = viewModel::onTargetEntryConsumed,
            onSettingsClick = viewModel::onSettingsClick,
            onSettingsBack = {
                // Read the latest saved key BEFORE closing settings screen
                apiKey = settingsRepository.getApiKey() ?: ""
                viewModel.onSettingsBack()
            },
            onAboutClick = viewModel::onAboutClick,
            onAboutBack = viewModel::onAboutBack,
            onOssLicensesClick = viewModel::onOssLicensesClick,
            onOssLicensesBack = viewModel::onOssLicensesBack,
            onSaveApiKey = viewModel::onSaveApiKey,
            onClearApiKey = viewModel::onClearApiKey,
            onSplashFinished = viewModel::onSplashFinished,
            modifier = modifier,
        )
    }
}
