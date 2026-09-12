package top.blackcyan.collins

import kotlin.test.Test
import kotlin.test.assertEquals
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
import top.blackcyan.collins.repository.DefaultCollinsRepository

class SharedLogicDesktopTest {
    @Test
    fun repositoryMapsTheSharedCollinsModelsOnJvm() {
        runTest {
            val repository = DefaultCollinsRepository(FakeCollinsApiClient())

            assertEquals(
                listOf("british"),
                repository.dictionaries().map { it.code },
            )
            assertEquals(
                "car",
                repository.search("british", "car").results.single().label,
            )
            assertEquals(
                "carr",
                repository.didYouMean("british", "carr").searchTerm,
            )
            assertEquals(
                "car",
                repository.firstMatch("british", "car").entryId,
            )
            assertEquals(
                "html",
                repository.entry("british", "car")?.format,
            )
            assertEquals(
                "en",
                repository.pronunciations("british", "car").single().lang,
            )
            assertEquals(
                "cars",
                repository.nearbyEntries("british", "car").nearbyFollowingEntries.single().label,
            )
        }
    }

    private class FakeCollinsApiClient : CollinsApiClient {
        override suspend fun listDictionaries(): List<CollinsDictionaryDto> =
            listOf(
                CollinsDictionaryDto(
                    dictionaryCode = "british",
                    dictionaryName = "British English",
                    dictionaryUrl = "https://example.test/british",
                ),
            )

        override suspend fun search(dictionaryCode: String, query: String, pageSize: Int, pageIndex: Int): CollinsSearchResponse =
            CollinsSearchResponse(
                dictionaryCode = dictionaryCode,
                resultNumber = 1,
                pageNumber = 1,
                currentPageIndex = 1,
                results = listOf(
                    CollinsSearchHitDto(
                        entryId = query,
                        entryLabel = query,
                        entryUrl = "https://example.test/$query",
                    ),
                ),
            )

        override suspend fun didYouMean(dictionaryCode: String, query: String, entryNumber: Int): CollinsDidYouMeanResponse =
            CollinsDidYouMeanResponse(
                dictionaryCode = dictionaryCode,
                searchTerm = query,
                suggestions = emptyList(),
            )

        override suspend fun firstMatch(dictionaryCode: String, query: String, format: String): CollinsEntryDto =
            entry(dictionaryCode, query, format) ?: error("Entry $query not found")

        override suspend fun entry(dictionaryCode: String, entryId: String, format: String): CollinsEntryDto? =
            CollinsEntryDto(
                dictionaryCode = dictionaryCode,
                format = format,
                entryContent = "<p>$entryId</p>",
                entryId = entryId,
                entryLabel = entryId,
                entryUrl = "https://example.test/$entryId",
                topics = emptyList(),
            )

        override suspend fun pronunciations(dictionaryCode: String, entryId: String, lang: String?): List<CollinsPronunciationDto> =
            listOf(
                CollinsPronunciationDto(
                    dictionaryCode = dictionaryCode,
                    entryId = entryId,
                    lang = lang ?: "en",
                    pronunciationUrl = "https://example.test/$entryId.mp3",
                ),
            )

        override suspend fun nearbyEntries(dictionaryCode: String, entryId: String, entryNumber: Int): CollinsNearbyEntriesResponse =
            CollinsNearbyEntriesResponse(
                dictionaryCode = dictionaryCode,
                entryId = entryId,
                nearbyFollowingEntries = listOf(
                    CollinsNearbyEntryDto(
                        entryId = "cars",
                        entryLabel = "cars",
                        entryUrl = "https://example.test/cars",
                    ),
                ),
                nearbyPrecedingEntries = emptyList(),
            )
    }
}
