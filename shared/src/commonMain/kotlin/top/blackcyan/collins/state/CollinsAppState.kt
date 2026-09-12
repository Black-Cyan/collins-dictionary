package top.blackcyan.collins.state

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.coroutineScope
import top.blackcyan.collins.data.CollinsApiException
import top.blackcyan.collins.domain.DictionarySummary
import top.blackcyan.collins.domain.EntryDetail
import top.blackcyan.collins.domain.Headword
import top.blackcyan.collins.domain.NearbyEntries
import top.blackcyan.collins.domain.SearchResolution
import top.blackcyan.collins.domain.SearchResolver
import top.blackcyan.collins.domain.entryIdStem
import top.blackcyan.collins.domain.normalizeQuery
import top.blackcyan.collins.domain.plainText
import top.blackcyan.collins.repository.CollinsRepository

/**
 * HTML-extracted audio paths are relative to the Collins website root
 * (e.g. "/sounds/hwd_mp3/hello.mp3").  The endpoint pronunciation URLs
 * are already absolute.  This resolves both to an absolute URL that the
 * audio player can open.
 */
private const val COLLINS_WEBSITE_ROOT = "https://www.collinsdictionary.com"

private fun resolveAudioUrl(raw: String): String =
    if (raw.startsWith("http://") || raw.startsWith("https://")) raw
    else COLLINS_WEBSITE_ROOT + if (raw.startsWith("/")) raw else "/$raw"

class CollinsAppState(
    private val repository: CollinsRepository,
    private val searchResolver: SearchResolver = SearchResolver(repository),
) {
    private val _uiState = MutableStateFlow(CollinsAppUiState())
    val uiState: StateFlow<CollinsAppUiState> = _uiState.asStateFlow()

    suspend fun loadDictionaries() {
        runCatching {
            repository.dictionaries()
        }.onSuccess { dictionaries ->
            _uiState.update { current ->
                val selectedDictionaryCode = current.selectedDictionaryCode.takeIf { selected ->
                    dictionaries.any { it.code == selected }
                } ?: dictionaries.firstOrNull()?.code

                current.copy(
                    dictionaries = dictionaries,
                    selectedDictionaryCode = selectedDictionaryCode,
                    dictionariesError = null,
                    search = current.search.copy(errorMessage = null),
                )
            }
        }.onFailure { error ->
            _uiState.update { current ->
                current.copy(
                    dictionariesError = error.toUserMessage(),
                    search = current.search.copy(
                        isLoading = false,
                    ),
                )
            }
        }
    }

    fun selectDictionary(dictionaryCode: String) {
        _uiState.update { current ->
            current.copy(
                selectedDictionaryCode = dictionaryCode,
                search = CollinsSearchUiState(),
                entry = CollinsEntryUiState(),
                screen = CollinsScreen.SearchHome,
                entryBackStack = emptyList(),
            )
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                search = current.search.copy(
                    query = query,
                    errorMessage = null,
                ),
            )
        }
    }

    suspend fun search(query: String = uiState.value.search.query) {
        val dictionaryCode = requireSelectedDictionary() ?: return
        if (query.isBlank()) {
            _uiState.update { current ->
                current.copy(
                    search = CollinsSearchUiState(),
                    entry = CollinsEntryUiState(),
                    screen = CollinsScreen.SearchHome,
                    entryBackStack = emptyList(),
                )
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                search = current.search.copy(
                    query = query,
                    isLoading = true,
                    headwords = null,
                    suggestions = emptyList(),
                    emptyMessage = null,
                    errorMessage = null,
                ),
                entry = CollinsEntryUiState(),
                screen = CollinsScreen.SearchHome,
                entryBackStack = emptyList(),
            )
        }

        runCatching {
            searchResolver.resolve(dictionaryCode = dictionaryCode, rawQuery = query)
        }.onSuccess { resolution ->
            when (resolution) {
                is SearchResolution.ExactHeadword -> {
                    _uiState.update { current ->
                        current.copy(
                            search = current.search.copy(
                                isLoading = false,
                                headwords = null,
                                suggestions = emptyList(),
                                emptyMessage = null,
                                errorMessage = null,
                            ),
                        )
                    }
                    openHeadword(resolution.label, resolution.entryIds)
                }

                is SearchResolution.Matches -> {
                    _uiState.update { current ->
                        current.copy(
                            search = current.search.copy(
                                isLoading = false,
                                headwords = resolution.headwords,
                                suggestions = emptyList(),
                                emptyMessage = null,
                                errorMessage = null,
                            ),
                        )
                    }
                }

                is SearchResolution.DidYouMean -> {
                    _uiState.update { current ->
                        current.copy(
                            search = current.search.copy(
                                isLoading = false,
                                headwords = null,
                                suggestions = resolution.suggestions,
                                emptyMessage = "No direct results found",
                                errorMessage = null,
                            ),
                        )
                    }
                }

                SearchResolution.NotFound -> {
                    _uiState.update { current ->
                        current.copy(
                            search = current.search.copy(
                                isLoading = false,
                                headwords = null,
                                suggestions = emptyList(),
                                emptyMessage = "No results found",
                                errorMessage = null,
                            ),
                        )
                    }
                }
            }
        }.onFailure { error ->
            _uiState.update { current ->
                current.copy(
                    search = current.search.copy(
                        isLoading = false,
                        headwords = null,
                        suggestions = emptyList(),
                        emptyMessage = null,
                        errorMessage = error.toUserMessage(),
                    ),
                )
            }
        }
    }

    /**
     * Opens the detail page for a headword. When [entryIds] is known from a
     * prior resolution it is reused; otherwise (e.g. nearby-entry navigation,
     * which only carries an entry id) a fresh /search resolves the homograph
     * group. Every entry loads independently so a failing pronunciation or
     * entry never hides the entries that did load.
     *
     * [targetEntryId] marks the entry block the user asked for inside an
     * aggregated page (cross-reference / nearby click); the detail screen
     * auto-scrolls to it once.
     */
    suspend fun openHeadword(
        headwordLabel: String,
        entryIds: List<String>? = null,
        targetEntryId: String? = null,
        pushToBackStack: Boolean = true,
    ) {
        val dictionaryCode = requireSelectedDictionary() ?: return

        _uiState.update { current ->
            val stack = if (pushToBackStack && current.screen is CollinsScreen.EntryDetail) {
                current.entryBackStack + EntryBackStackEntry(
                    screen = current.screen,
                    entry = current.entry,
                )
            } else current.entryBackStack

            current.copy(
                screen = CollinsScreen.EntryDetail(
                    dictionaryCode = dictionaryCode,
                    headword = headwordLabel,
                ),
                entry = CollinsEntryUiState(
                    headword = headwordLabel,
                    isLoading = true,
                    targetEntryId = targetEntryId,
                ),
                entryBackStack = stack,
            )
        }

        val resolvedIds = entryIds ?: resolveEntryIds(dictionaryCode, headwordLabel)
        if (resolvedIds.isNullOrEmpty()) {
            _uiState.update { current ->
                current.copy(
                    entry = current.entry.copy(
                        isLoading = false,
                        errorMessage = "Entry not found",
                    ),
                )
            }
            return
        }

        loadEntries(dictionaryCode, headwordLabel, resolvedIds, targetEntryId)
    }

    /**
     * Opens a detail page targeted at a concrete Collins entry id (used by
     * in-app cross-reference links and the nearby-entries rows). The id stem is
     * searched once; every homograph in its group loads together and the page
     * scrolls to the requested entry. When the id cannot be resolved through
     * search, the entry is loaded directly; when that also fails the call is a
     * no-op so the link can stay inert text.
     */
    suspend fun openEntryReference(entryId: String, pushToBackStack: Boolean = true) {
        val dictionaryCode = requireSelectedDictionary() ?: return

        val group = runCatching {
            repository.search(
                dictionaryCode = dictionaryCode,
                query = entryIdStem(entryId),
                pageSize = SearchResolver.DEFAULT_PAGE_SIZE,
            )
        }.getOrNull()?.let { page ->
            SearchResolver.groupHeadwords(page.results).firstOrNull { group ->
                group.hits.any { it.id == entryId }
            }
        }

        if (group != null) {
            openHeadword(
                headwordLabel = group.displayLabel,
                entryIds = group.hits.map { it.id },
                targetEntryId = entryId,
                pushToBackStack = pushToBackStack,
            )
            return
        }

        val direct = runCatching {
            repository.entry(dictionaryCode = dictionaryCode, entryId = entryId)
        }.getOrNull() ?: return
        openHeadword(
            headwordLabel = direct.parsedEntry?.headword ?: entryIdStem(entryId),
            entryIds = listOf(entryId),
            targetEntryId = entryId,
            pushToBackStack = pushToBackStack,
        )
    }

    /** Called by the UI after scrolling to the requested entry block. */
    fun consumeTargetEntry() {
        _uiState.update { current ->
            current.copy(entry = current.entry.copy(targetEntryId = null))
        }
    }

    private suspend fun loadEntries(
        dictionaryCode: String,
        headwordLabel: String,
        resolvedIds: List<String>,
        targetEntryId: String?,
    ) {
        var loadError: Throwable? = null
        val senses: List<EntrySense>
        val nearby: NearbyEntries?
        coroutineScope {
            val senseDeferred = resolvedIds.map { entryId ->
                async {
                    val detail = runCatching {
                        repository.entry(dictionaryCode = dictionaryCode, entryId = entryId)
                    }.onFailure { error ->
                        if (loadError == null) loadError = error
                    }.getOrNull() ?: return@async null

                    EntrySense(
                        detail = detail,
                        pronunciations = resolvePronunciations(dictionaryCode, detail),
                    )
                }
            }
            val nearbyDeferred = async {
                // Nearby is anchored on the targeted entry when navigating by id,
                // otherwise on the first homograph.
                val anchor = targetEntryId ?: resolvedIds.first()
                runCatching {
                    repository.nearbyEntries(dictionaryCode = dictionaryCode, entryId = anchor)
                }.getOrNull()
            }
            senses = senseDeferred.awaitAll().filterNotNull()
            nearby = nearbyDeferred.await()
        }

        _uiState.update { current ->
            if (senses.isEmpty()) {
                current.copy(
                    entry = current.entry.copy(
                        isLoading = false,
                        errorMessage = loadError?.toUserMessage() ?: "Entry not found",
                    ),
                )
            } else {
                current.copy(
                    entry = current.entry.copy(
                        isLoading = false,
                        senses = senses,
                        nearbyEntries = nearby,
                        errorMessage = null,
                    ),
                )
            }
        }
    }

    /**
     * Pronunciations come from the parsed HTML first (IPA + regional label +
     * mp3). Only when the HTML contains no audio at all do we fall back to the
     * /pronunciations endpoint; endpoint lang codes are shown uppercased
     * ("uk" -> "UK").
     */
    private suspend fun resolvePronunciations(dictionaryCode: String, detail: EntryDetail): List<SensePronunciation> {
        val parsed = detail.parsedEntry
        if (parsed != null) {
            val htmlPronunciations = parsed.pronunciations.map { pron ->
                SensePronunciation(
                    ipa = pron.ipa.plainText().ifBlank { null },
                    label = pron.label ?: parsed.variant,
                    audioUrl = pron.audioUrl?.let { resolveAudioUrl(it) },
                )
            }
            if (htmlPronunciations.any { it.audioUrl != null }) {
                return htmlPronunciations
            }
            val fallback = runCatching {
                repository.pronunciations(dictionaryCode = dictionaryCode, entryId = detail.entryId)
            }.getOrDefault(emptyList())
            if (fallback.isNotEmpty()) {
                return htmlPronunciations + fallback.map {
                    SensePronunciation(ipa = null, label = it.lang.uppercase(), audioUrl = resolveAudioUrl(it.url))
                }
            }
            return htmlPronunciations
        }

        // Unrecognized HTML: the endpoint is the only audio source we have.
        return runCatching {
            repository.pronunciations(dictionaryCode = dictionaryCode, entryId = detail.entryId)
        }.getOrDefault(emptyList()).map {
            SensePronunciation(ipa = null, label = it.lang.uppercase(), audioUrl = resolveAudioUrl(it.url))
        }
    }

    private suspend fun resolveEntryIds(dictionaryCode: String, headwordLabel: String): List<String>? {
        val page = runCatching {
            repository.search(
                dictionaryCode = dictionaryCode,
                query = headwordLabel,
                pageSize = SearchResolver.DEFAULT_PAGE_SIZE,
            )
        }.getOrNull() ?: return null
        val query = normalizeQuery(headwordLabel)
        return SearchResolver.groupHeadwords(page.results)
            .firstOrNull { it.key == query || normalizeQuery(it.displayLabel) == query }
            ?.hits
            ?.map { it.id }
            ?.takeIf { it.isNotEmpty() }
    }

    fun closeEntry() {
        _uiState.update { current ->
            val stack = current.entryBackStack
            if (stack.isNotEmpty()) {
                val prev = stack.last()
                current.copy(
                    screen = prev.screen,
                    entry = prev.entry,
                    entryBackStack = stack.dropLast(1),
                )
            } else {
                current.copy(
                    screen = CollinsScreen.SearchHome,
                    entry = CollinsEntryUiState(),
                )
            }
        }
    }

    fun navigateToSettings() {
        _uiState.update { current ->
            current.copy(
                screen = CollinsScreen.Settings,
                // Keep entryBackStack so closeSettings() can return to the
                // same entry, but discard it when settings are actually closed
                // to avoid stale navigation state after a dictionary switch.
            )
        }
    }

    fun closeSettings() {
        _uiState.update { current ->
            current.copy(
                screen = CollinsScreen.SearchHome,
                entry = CollinsEntryUiState(),
                entryBackStack = emptyList(),
            )
        }
    }

    /** About is reached from Settings; back always returns to Settings. */
    fun navigateToAbout() {
        _uiState.update { current ->
            current.copy(screen = CollinsScreen.About)
        }
    }

    fun closeAbout() {
        _uiState.update { current ->
            current.copy(screen = CollinsScreen.Settings)
        }
    }

    /** The OSS license list is reached from, and returns to, About. */
    fun navigateToOssLicenses() {
        _uiState.update { current ->
            current.copy(screen = CollinsScreen.OssLicenses)
        }
    }

    fun closeOssLicenses() {
        _uiState.update { current ->
            current.copy(screen = CollinsScreen.About)
        }
    }

    fun navigateFromSplash() {
        _uiState.update { current ->
            current.copy(
                screen = CollinsScreen.SearchHome,
                entryBackStack = emptyList(),
            )
        }
    }

    private fun requireSelectedDictionary(): String? {
        val selectedDictionaryCode = uiState.value.selectedDictionaryCode
        if (selectedDictionaryCode != null) {
            return selectedDictionaryCode
        }

        _uiState.update { current ->
            current.copy(
                search = current.search.copy(
                    isLoading = false,
                    errorMessage = "Choose a dictionary first",
                ),
            )
        }
        return null
    }
}

data class CollinsAppUiState(
    val dictionaries: List<DictionarySummary> = emptyList(),
    val selectedDictionaryCode: String? = null,
    val dictionariesError: String? = null,
    val search: CollinsSearchUiState = CollinsSearchUiState(),
    val entry: CollinsEntryUiState = CollinsEntryUiState(),
    val screen: CollinsScreen = CollinsScreen.Splash,
    val entryBackStack: List<EntryBackStackEntry> = emptyList(),
)

/** Snapshot of one EntryDetail page saved before navigating to another entry. */
data class EntryBackStackEntry(
    val screen: CollinsScreen.EntryDetail,
    val entry: CollinsEntryUiState,
)

data class CollinsSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val headwords: List<Headword>? = null,
    val suggestions: List<String> = emptyList(),
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
)

data class CollinsEntryUiState(
    val headword: String = "",
    val isLoading: Boolean = false,
    val senses: List<EntrySense> = emptyList(),
    val nearbyEntries: NearbyEntries? = null,
    /** Entry block to scroll to once; cleared via consumeTargetEntry(). */
    val targetEntryId: String? = null,
    val errorMessage: String? = null,
)

/** One etymological entry of a headword together with its pronunciations. */
data class EntrySense(
    val detail: EntryDetail,
    val pronunciations: List<SensePronunciation> = emptyList(),
)

/** IPA is null for pronunciations only known from the fallback endpoint. */
data class SensePronunciation(
    val ipa: String?,
    val label: String?,
    val audioUrl: String?,
)

data class SettingsUiState(
    val apiKey: String = "",
    val isApiKeyConfigured: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

sealed interface CollinsScreen {
    data object Splash : CollinsScreen

    data object SearchHome : CollinsScreen

    data class EntryDetail(
        val dictionaryCode: String,
        val headword: String,
    ) : CollinsScreen

    data object Settings : CollinsScreen

    data object About : CollinsScreen

    data object OssLicenses : CollinsScreen
}

private fun Throwable.toUserMessage(): String =
    when (this) {
        is CollinsApiException.HttpFailure -> "Collins API request failed with status ${status.value}"
        is CollinsApiException.DecodingFailure -> "Unable to read Collins API response"
        else -> message ?: "Something went wrong"
    }
