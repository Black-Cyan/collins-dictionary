package top.blackcyan.collins.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import top.blackcyan.collins.data.CollinsApiClient
import top.blackcyan.collins.data.CollinsDidYouMeanResponse
import top.blackcyan.collins.data.CollinsDictionaryDto
import top.blackcyan.collins.data.CollinsEntryDto
import top.blackcyan.collins.data.CollinsNearbyEntriesResponse
import top.blackcyan.collins.data.CollinsNearbyEntryDto
import top.blackcyan.collins.data.CollinsPronunciationDto
import top.blackcyan.collins.data.CollinsSearchHitDto
import top.blackcyan.collins.data.CollinsSearchResponse
import top.blackcyan.collins.repository.CollinsRepository
import top.blackcyan.collins.repository.DefaultCollinsRepository

class SearchResolverTest {

    @Test
    fun exactQueryResolvesToAHeadwordContainingEveryHomographEntry() = runTest {
        val resolver = resolverWith(
            searchHits = listOf(
                SearchHit("car_1", "car"),
                SearchHit("car_2", "car"),
                SearchHit("car_3", "car"),
            ),
        )

        val resolution = resolver.resolve("british", "car")

        assertEquals(
            SearchResolution.ExactHeadword(label = "car", entryIds = listOf("car_1", "car_2", "car_3")),
            resolution,
        )
    }

    @Test
    fun queryIsTrimmedLowercasedAndWhitespaceCollapsedBeforeMatching() = runTest {
        val resolver = resolverWith(
            searchHits = listOf(SearchHit("apple_1", "Apple")),
        )

        val resolution = resolver.resolve("british", "  Apple   ")

        assertEquals(
            SearchResolution.ExactHeadword(label = "Apple", entryIds = listOf("apple_1")),
            resolution,
        )
    }

    @Test
    fun nonExactQueryGroupsResultsByHeadwordInFirstSeenOrder() = runTest {
        val resolver = resolverWith(
            searchHits = listOf(
                SearchHit("apple_1", "apple"),
                SearchHit("apple_2", "apple"),
                SearchHit("application_1", "application"),
            ),
            resultNumber = 3,
        )

        val resolution = resolver.resolve("british", "appl")

        assertEquals(
            SearchResolution.Matches(
                headwords = listOf(
                    Headword(label = "apple", entryIds = listOf("apple_1", "apple_2")),
                    Headword(label = "application", entryIds = listOf("application_1")),
                ),
                totalEntries = 3,
            ),
            resolution,
        )
    }

    @Test
    fun numberedHomographLabelsAggregateIntoOneStemHeadword() = runTest {
        // Device-verified: /search labels for the take homographs come back as
        // "take1" / "take 2" while their entry ids are take_1 / take_2.
        val resolver = resolverWith(
            searchHits = listOf(
                SearchHit("take_1", "take1"),
                SearchHit("take_2", "take 2"),
            ),
        )

        val resolution = resolver.resolve("british", "take")

        assertEquals(
            SearchResolution.ExactHeadword(label = "take", entryIds = listOf("take_1", "take_2")),
            resolution,
        )
    }

    @Test
    fun queryCarryingAHomographDigitAlsoResolvesToTheStemHeadword() = runTest {
        val resolver = resolverWith(
            searchHits = listOf(
                SearchHit("take_1", "take1"),
                SearchHit("take_2", "take 2"),
            ),
        )

        val resolution = resolver.resolve("british", "take2")

        assertEquals(
            SearchResolution.ExactHeadword(label = "take", entryIds = listOf("take_1", "take_2")),
            resolution,
        )
    }

    @Test
    fun numberedPhrasesDoNotCollapseIntoTheEntryIdStem() = runTest {
        val resolver = resolverWith(
            searchHits = listOf(
                SearchHit("application_1", "application"),
                SearchHit("application-form_1", "application form"),
            ),
        )

        val resolution = resolver.resolve("british", "application form")

        assertEquals(
            SearchResolution.ExactHeadword(
                label = "application form",
                entryIds = listOf("application-form_1"),
            ),
            resolution,
        )
    }

    @Test
    fun nonExactMatchesShowOneRowPerStem() = runTest {
        val resolver = resolverWith(
            searchHits = listOf(
                SearchHit("take_1", "take1"),
                SearchHit("take_2", "take2"),
                SearchHit("takeaway_1", "takeaway"),
            ),
            resultNumber = 3,
        )

        val resolution = resolver.resolve("british", "tak")

        assertEquals(
            SearchResolution.Matches(
                headwords = listOf(
                    Headword(label = "take", entryIds = listOf("take_1", "take_2")),
                    Headword(label = "takeaway", entryIds = listOf("takeaway_1")),
                ),
                totalEntries = 3,
            ),
            resolution,
        )
    }

    @Test
    fun emptyResultsWithSuggestionsResolvesToDidYouMean() = runTest {
        val resolver = resolverWith(searchHits = emptyList(), suggestions = listOf("apple"))

        val resolution = resolver.resolve("british", "appple")

        assertEquals(
            SearchResolution.DidYouMean(query = "appple", suggestions = listOf("apple")),
            resolution,
        )
    }

    @Test
    fun emptyResultsWithoutSuggestionsResolvesToNotFound() = runTest {
        val resolver = resolverWith(searchHits = emptyList(), suggestions = emptyList())

        val resolution = resolver.resolve("british", "zzzzq")

        assertTrue(resolution is SearchResolution.NotFound)
    }

    private fun resolverWith(
        searchHits: List<SearchHit>,
        resultNumber: Int = searchHits.size,
        suggestions: List<String> = emptyList(),
    ): SearchResolver {
        val apiClient = FakeCollinsApiClient(
            searchHits = searchHits,
            resultNumber = resultNumber,
            suggestions = suggestions,
        )
        return SearchResolver(DefaultCollinsRepository(apiClient))
    }

    private data class SearchHit(val id: String, val label: String)

    private class FakeCollinsApiClient(
        private val searchHits: List<SearchHit>,
        private val resultNumber: Int,
        private val suggestions: List<String>,
    ) : CollinsApiClient {
        override suspend fun listDictionaries(): List<CollinsDictionaryDto> = emptyList()

        override suspend fun search(
            dictionaryCode: String,
            query: String,
            pageSize: Int,
            pageIndex: Int,
        ): CollinsSearchResponse = CollinsSearchResponse(
            dictionaryCode = dictionaryCode,
            resultNumber = resultNumber,
            pageNumber = 1,
            currentPageIndex = 1,
            results = searchHits.map {
                CollinsSearchHitDto(
                    entryId = it.id,
                    entryLabel = it.label,
                    entryUrl = "https://example.test/${it.id}",
                )
            },
        )

        override suspend fun didYouMean(
            dictionaryCode: String,
            query: String,
            entryNumber: Int,
        ): CollinsDidYouMeanResponse = CollinsDidYouMeanResponse(
            dictionaryCode = dictionaryCode,
            searchTerm = query,
            suggestions = suggestions,
        )

        override suspend fun firstMatch(
            dictionaryCode: String,
            query: String,
            format: String,
        ): CollinsEntryDto = error("not used by SearchResolver")

        override suspend fun entry(
            dictionaryCode: String,
            entryId: String,
            format: String,
        ): CollinsEntryDto? = null

        override suspend fun pronunciations(
            dictionaryCode: String,
            entryId: String,
            lang: String?,
        ): List<CollinsPronunciationDto> = emptyList()

        override suspend fun nearbyEntries(
            dictionaryCode: String,
            entryId: String,
            entryNumber: Int,
        ): CollinsNearbyEntriesResponse = CollinsNearbyEntriesResponse(
            dictionaryCode = dictionaryCode,
            entryId = entryId,
            nearbyFollowingEntries = emptyList(),
            nearbyPrecedingEntries = emptyList(),
        )
    }
}
