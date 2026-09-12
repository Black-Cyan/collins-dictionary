package top.blackcyan.collins.settings

import com.russhwolf.settings.Settings

class SettingsRepository(
    private val settings: Settings = Settings(),
) {
    companion object {
        private const val KEY_API_KEY = "collins_api_key"
    }

    fun getApiKey(): String? =
        settings.getStringOrNull(KEY_API_KEY)

    fun saveApiKey(apiKey: String) {
        settings.putString(KEY_API_KEY, apiKey)
    }

    fun clearApiKey() {
        settings.remove(KEY_API_KEY)
    }

    fun hasApiKey(): Boolean =
        !getApiKey().isNullOrBlank()
}
