package top.blackcyan.collins.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import top.blackcyan.collins.data.CollinsEntryContentParser
import top.blackcyan.collins.domain.DidYouMeanResult
import top.blackcyan.collins.domain.DictionarySummary
import top.blackcyan.collins.domain.EntryDetail
import top.blackcyan.collins.domain.NearbyEntries
import top.blackcyan.collins.domain.Pronunciation
import top.blackcyan.collins.domain.SearchHit
import top.blackcyan.collins.domain.SearchPage
import top.blackcyan.collins.domain.Topic
import top.blackcyan.collins.repository.CollinsRepository

class CollinsAppStateTest {
    @Test
    fun loadDictionariesSelectsTheFirstDictionaryAndKeepsTheSearchQuery() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())

        state.updateQuery("car")
        state.loadDictionaries()

        assertEquals(listOf("british", "american"), state.uiState.value.dictionaries.map { it.code })
        assertEquals("british", state.uiState.value.selectedDictionaryCode)
        assertEquals("car", state.uiState.value.search.query)
    }

    @Test
    fun exactSearchOpensTheAggregatedHeadwordDetail() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        state.search("  Car  ")

        val ui = state.uiState.value
        assertEquals(CollinsScreen.EntryDetail("british", "Car"), ui.screen)
        assertEquals(false, ui.entry.isLoading)
        assertEquals("Car", ui.entry.headword)
        // car_1 and car_2 are homographs of one headword and load as separate senses.
        assertEquals(listOf("car_1", "car_2"), ui.entry.senses.map { it.detail.entryId })
        // Unparsable fixture HTML falls back to the /pronunciations endpoint,
        // whose lang code is shown uppercased on the button.
        assertEquals(listOf("EN", "EN"), ui.entry.senses.map { it.pronunciations.single().label })
        assertEquals("cars", ui.entry.nearbyEntries?.nearbyFollowingEntries?.single()?.label)
        assertNull(ui.search.headwords)
    }

    @Test
    fun nonExactSearchListsGroupedHeadwords() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        state.search("carp")

        val ui = state.uiState.value
        assertEquals(CollinsScreen.SearchHome, ui.screen)
        assertEquals(listOf("carpet"), ui.search.headwords?.map { it.label })
        assertEquals(listOf("carpet_1"), ui.search.headwords?.single()?.entryIds)
        assertNull(ui.search.emptyMessage)
    }

    @Test
    fun searchWithoutResultsLoadsSuggestionsIntoTheEmptyState() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        state.search("carr")

        assertEquals(null, state.uiState.value.search.headwords)
        assertEquals(listOf("car"), state.uiState.value.search.suggestions)
        assertEquals("No direct results found", state.uiState.value.search.emptyMessage)
    }

    @Test
    fun searchFailureSurfacesAnErrorState() = runTest {
        val state = CollinsAppState(FakeCollinsRepository(throwOnSearch = true))
        state.loadDictionaries()

        state.search("car")

        assertEquals("Search failed", state.uiState.value.search.errorMessage)
        assertEquals(false, state.uiState.value.search.isLoading)
    }

    @Test
    fun selectDictionaryResetsSearchAndClosesDetail() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()
        state.search("car")

        state.selectDictionary("american")

        assertEquals("american", state.uiState.value.selectedDictionaryCode)
        assertEquals(null, state.uiState.value.search.headwords)
        assertEquals(CollinsScreen.SearchHome, state.uiState.value.screen)
        assertEquals(emptyList(), state.uiState.value.entry.senses)
    }

    @Test
    fun openHeadwordWithIdsLoadsSensesPronunciationsAndNearbyEntries() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        state.openHeadword("Car", listOf("car_1", "car_2"))

        assertEquals(CollinsScreen.EntryDetail("british", "Car"), state.uiState.value.screen)
        assertEquals(listOf("car_1", "car_2"), state.uiState.value.entry.senses.map { it.detail.entryId })
        assertEquals("cars", state.uiState.value.entry.nearbyEntries?.nearbyFollowingEntries?.single()?.label)
    }

    @Test
    fun openHeadwordByLabelReResolvesEntryIdsThroughSearch() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        // Nearby navigation carries only a label.
        state.openHeadword("cars")

        val ui = state.uiState.value
        assertEquals(CollinsScreen.EntryDetail("british", "cars"), ui.screen)
        assertEquals(listOf("cars_1"), ui.entry.senses.map { it.detail.entryId })
    }

    @Test
    fun oneMissingHomographNeverHidesTheSensesThatLoaded() = runTest {
        val state = CollinsAppState(FakeCollinsRepository(missingEntryIds = setOf("car_2")))
        state.loadDictionaries()

        state.search("car")

        val senses = state.uiState.value.entry.senses
        assertEquals(listOf("car_1"), senses.map { it.detail.entryId })
        assertNull(state.uiState.value.entry.errorMessage)
    }

    @Test
    fun failingPronunciationsDoNotHideTheSense() = runTest {
        val state = CollinsAppState(FakeCollinsRepository(brokenPronunciationEntryIds = setOf("car_1")))
        state.loadDictionaries()

        state.openHeadword("Car", listOf("car_1", "car_2"))

        val senses = state.uiState.value.entry.senses
        assertEquals(listOf("car_1", "car_2"), senses.map { it.detail.entryId })
        assertEquals(emptyList(), senses.first().pronunciations)
        assertTrue(senses.last().pronunciations.isNotEmpty())
    }

    @Test
    fun htmlAudioIsUsedAndThePronunciationsEndpointIsNotCalledAgain() = runTest {
        val repository = FakeCollinsRepository()
        val state = CollinsAppState(repository)
        state.loadDictionaries()

        state.openHeadword("take", listOf("take_1", "take_2"))

        val senses = state.uiState.value.entry.senses
        assertEquals(2, senses.size)
        val first = senses.first().pronunciations.single()
        assertEquals("UK", first.label)
        assertEquals("https://example.test/take_1.mp3", first.audioUrl)
        assertEquals(0, repository.pronunciationCallsFor("take_1"))
    }

    @Test
    fun openEntryReferenceResolvesTheHomographGroupAndMarksTheScrollTarget() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        state.openEntryReference("take_2")

        val ui = state.uiState.value
        assertEquals(CollinsScreen.EntryDetail("british", "take"), ui.screen)
        assertEquals(listOf("take_1", "take_2"), ui.entry.senses.map { it.detail.entryId })
        assertEquals("take_2", ui.entry.targetEntryId)
        // Nearby is anchored on the targeted entry, not the first homograph.
        assertEquals("take_2", ui.entry.nearbyEntries?.entryId)
    }

    @Test
    fun consumeTargetEntryClearsTheScrollMarker() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()
        state.openEntryReference("take_2")

        state.consumeTargetEntry()

        assertNull(state.uiState.value.entry.targetEntryId)
    }

    @Test
    fun openEntryReferenceToAnUnsearchableIdLoadsTheEntryDirectly() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()

        state.openEntryReference("lone_1")

        val ui = state.uiState.value
        assertEquals(listOf("lone_1"), ui.entry.senses.map { it.detail.entryId })
        assertEquals("lone_1", ui.entry.targetEntryId)
    }

    @Test
    fun closeEntryReturnsToTheSearchHomeScreen() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()
        state.openHeadword("Car", listOf("car_1"))

        state.closeEntry()

        assertEquals(CollinsScreen.SearchHome, state.uiState.value.screen)
        assertEquals(emptyList(), state.uiState.value.entry.senses)
    }

    @Test
    fun crossReferenceNavigationPushesTheCurrentEntryAndBackRestoresIt() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()
        state.openHeadword("Car", listOf("car_1", "car_2"))

        // A cross-reference link drills deeper: the Car page is pushed.
        state.openEntryReference("cars_1")

        var ui = state.uiState.value
        assertEquals(CollinsScreen.EntryDetail("british", "cars"), ui.screen)
        assertEquals(1, ui.entryBackStack.size)
        assertEquals(CollinsScreen.EntryDetail("british", "Car"), ui.entryBackStack.single().screen)

        state.closeEntry()
        ui = state.uiState.value
        assertEquals(CollinsScreen.EntryDetail("british", "Car"), ui.screen)
        assertEquals(listOf("car_1", "car_2"), ui.entry.senses.map { it.detail.entryId })
        assertEquals(emptyList(), ui.entryBackStack)

        state.closeEntry()
        assertEquals(CollinsScreen.SearchHome, state.uiState.value.screen)
    }

    @Test
    fun nearbyNavigationReplacesTheStackTopWithoutPushing() = runTest {
        val state = CollinsAppState(FakeCollinsRepository())
        state.loadDictionaries()
        state.openHeadword("Car", listOf("car_1", "car_2"))

        // Previous/next-entry browsing is sibling navigation: it must not push.
        state.openEntryReference("cars_1", pushToBackStack = false)

        val ui = state.uiState.value
        assertEquals(CollinsScreen.EntryDetail("british", "cars"), ui.screen)
        assertEquals(emptyList(), ui.entryBackStack)

        // Back leaves the app on SearchHome, not on the replaced Car page.
        state.closeEntry()
        assertEquals(CollinsScreen.SearchHome, state.uiState.value.screen)
    }

    @Test
    fun aboutBackReturnsToSettingsAndOssLicensesBackReturnsToAbout() {
        val state = CollinsAppState(FakeCollinsRepository())

        state.navigateToSettings()
        state.navigateToAbout()
        assertEquals(CollinsScreen.About, state.uiState.value.screen)

        state.closeAbout()
        assertEquals(CollinsScreen.Settings, state.uiState.value.screen)

        state.navigateToAbout()
        state.navigateToOssLicenses()
        assertEquals(CollinsScreen.OssLicenses, state.uiState.value.screen)

        state.closeOssLicenses()
        assertEquals(CollinsScreen.About, state.uiState.value.screen)
    }

    private class FakeCollinsRepository(
        private val throwOnSearch: Boolean = false,
        private val missingEntryIds: Set<String> = emptySet(),
        private val brokenPronunciationEntryIds: Set<String> = emptySet(),
    ) : CollinsRepository {
        private val pronunciationCallCounts = mutableMapOf<String, Int>()

        fun pronunciationCallsFor(entryId: String): Int = pronunciationCallCounts[entryId] ?: 0

        override suspend fun dictionaries(): List<DictionarySummary> =
            listOf(
                DictionarySummary("british", "British English", "https://example.test/british"),
                DictionarySummary("american", "American English", "https://example.test/american"),
            )

        override suspend fun search(dictionaryCode: String, query: String, pageSize: Int, pageIndex: Int): SearchPage {
            if (throwOnSearch) error("Search failed")
            val results = when (query) {
                "car" -> listOf(
                    SearchHit("car_1", "Car", "https://example.test/car_1"),
                    SearchHit("car_2", "Car", "https://example.test/car_2"),
                )
                "carp" -> listOf(SearchHit("carpet_1", "carpet", "https://example.test/carpet_1"))
                "cars" -> listOf(SearchHit("cars_1", "cars", "https://example.test/cars_1"))
                "take" -> listOf(
                    SearchHit("take_1", "take1", "https://example.test/take_1"),
                    SearchHit("take_2", "take 2", "https://example.test/take_2"),
                )
                else -> emptyList()
            }
            return SearchPage(
                dictionaryCode = dictionaryCode,
                query = query,
                resultNumber = results.size,
                pageNumber = 1,
                currentPageIndex = 1,
                results = results,
            )
        }

        override suspend fun didYouMean(dictionaryCode: String, query: String, entryNumber: Int): DidYouMeanResult =
            DidYouMeanResult(
                dictionaryCode = dictionaryCode,
                searchTerm = query,
                suggestions = listOf("car"),
            )

        override suspend fun firstMatch(dictionaryCode: String, query: String, format: String): EntryDetail =
            error("First-match lookup is not part of search resolution")

        override suspend fun entry(dictionaryCode: String, entryId: String, format: String): EntryDetail? {
            if (entryId in missingEntryIds) return null
            val headword = entryId.substringBefore('_')
            // take entries carry real Collins HTML with an embedded mp3 source.
            val content = if (headword == "take") {
                """<div class="entry lang_en-gb" id="$entryId"><span class="inline"><h1 class="hwd">take</h1>""" +
                    """<span class="pron">teɪk<audio><source src="https://example.test/$entryId.mp3"/></audio></span>""" +
                    """</span><div class="hom"><span class="gramGrp"><span class="pos">verb</span></span>""" +
                    """<div class="sense"><span class="sensenum">1.</span><span class="def">to grasp</span></div>""" +
                    """</div></div>"""
            } else {
                "<p>Sense of $entryId</p>"
            }
            return EntryDetail(
                dictionaryCode = dictionaryCode,
                format = format,
                entryId = entryId,
                entryLabel = headword,
                entryUrl = "https://example.test/$entryId",
                entryContent = content,
                topics = listOf(Topic("topic-1", "Transport", "https://example.test/topic", null)),
                parsedEntry = CollinsEntryContentParser.parse(content),
            )
        }

        override suspend fun pronunciations(dictionaryCode: String, entryId: String, lang: String?): List<Pronunciation> {
            if (entryId in brokenPronunciationEntryIds) error("Pronunciations unavailable")
            pronunciationCallCounts[entryId] = (pronunciationCallCounts[entryId] ?: 0) + 1
            return listOf(
                Pronunciation(
                    dictionaryCode = dictionaryCode,
                    entryId = entryId,
                    lang = lang ?: "en",
                    url = "https://example.test/$entryId.mp3",
                ),
            )
        }

        override suspend fun nearbyEntries(dictionaryCode: String, entryId: String, entryNumber: Int): NearbyEntries =
            when (entryId) {
                "car_1" -> NearbyEntries(
                    dictionaryCode = dictionaryCode,
                    entryId = entryId,
                    nearbyFollowingEntries = listOf(SearchHit("cars_1", "cars", "https://example.test/cars_1")),
                    nearbyPrecedingEntries = emptyList(),
                )
                else -> NearbyEntries(dictionaryCode, entryId, emptyList(), emptyList())
            }
    }
}
