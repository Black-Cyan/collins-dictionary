package top.blackcyan.collins.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.blackcyan.collins.audio.AudioPlayer
import top.blackcyan.collins.audio.createAudioPlayer
import top.blackcyan.collins.domain.Headword
import top.blackcyan.collins.settings.SettingsRepository
import top.blackcyan.collins.state.CollinsAppUiState
import top.blackcyan.collins.state.CollinsAppState
import top.blackcyan.collins.state.CollinsScreen
import top.blackcyan.collins.state.SettingsUiState

class CollinsAppViewModel(
    private val appState: CollinsAppState,
    private val settingsRepository: SettingsRepository,
    val audioPlayer: AudioPlayer = createAudioPlayer(),
) : ViewModel() {
    val uiState: StateFlow<CollinsAppUiState> = appState.uiState

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            appState.loadDictionaries()
        }
        loadSettings()
    }

    override fun onCleared() {
        audioPlayer.stop()
    }

    private fun loadSettings() {
        val apiKey = settingsRepository.getApiKey() ?: ""
        val isConfigured = settingsRepository.hasApiKey()

        _settingsState.update {
            it.copy(
                apiKey = apiKey,
                isApiKeyConfigured = isConfigured,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onDictionarySelect(dictionaryCode: String) {
        appState.selectDictionary(dictionaryCode)
    }

    fun onQueryChange(query: String) {
        appState.updateQuery(query)
    }

    fun onSearch(query: String) {
        viewModelScope.launch {
            appState.search(query)
        }
    }

    fun onHeadwordClick(headword: Headword) {
        viewModelScope.launch {
            appState.openHeadword(headword.label, headword.entryIds)
        }
    }

    fun onBack() {
        appState.closeEntry()
    }

    fun onNearbyEntryClick(entryId: String) {
        viewModelScope.launch {
            appState.openEntryReference(entryId, pushToBackStack = false)
        }
    }

    fun onEntryReferenceClick(entryId: String) {
        viewModelScope.launch {
            appState.openEntryReference(entryId)
        }
    }

    fun onTargetEntryConsumed() {
        appState.consumeTargetEntry()
    }

    fun onSettingsClick() {
        appState.navigateToSettings()
    }

    fun onSettingsBack() {
        appState.closeSettings()
        // Reload dictionaries after settings change
        viewModelScope.launch {
            appState.loadDictionaries()
        }
    }

    fun onAboutClick() {
        appState.navigateToAbout()
    }

    fun onAboutBack() {
        appState.closeAbout()
    }

    fun onOssLicensesClick() {
        appState.navigateToOssLicenses()
    }

    fun onOssLicensesBack() {
        appState.closeOssLicenses()
    }

    fun onSplashFinished() {
        appState.navigateFromSplash()
    }

    fun onSaveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _settingsState.update { it.copy(errorMessage = "API Key cannot be empty") }
            return
        }

        try {
            settingsRepository.saveApiKey(apiKey)
            _settingsState.update {
                it.copy(
                    apiKey = apiKey,
                    isApiKeyConfigured = true,
                    errorMessage = null,
                    successMessage = "API Key saved successfully!",
                )
            }
        } catch (e: Exception) {
            _settingsState.update {
                it.copy(errorMessage = "Failed to save API Key: ${e.message}")
            }
        }
    }

    fun onClearApiKey() {
        try {
            settingsRepository.clearApiKey()
            _settingsState.update {
                it.copy(
                    apiKey = "",
                    isApiKeyConfigured = false,
                    errorMessage = null,
                    successMessage = "API Key cleared successfully!",
                )
            }
        } catch (e: Exception) {
            _settingsState.update {
                it.copy(errorMessage = "Failed to clear API Key: ${e.message}")
            }
        }
    }
}
