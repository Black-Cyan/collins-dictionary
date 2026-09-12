package top.blackcyan.collins.repository

import top.blackcyan.collins.data.CollinsApiClient
import top.blackcyan.collins.data.CollinsDidYouMeanResponse
import top.blackcyan.collins.data.CollinsDictionaryDto
import top.blackcyan.collins.data.CollinsEntryContentParser
import top.blackcyan.collins.data.CollinsEntryDto
import top.blackcyan.collins.data.CollinsNearbyEntriesResponse
import top.blackcyan.collins.data.CollinsNearbyEntryDto
import top.blackcyan.collins.data.CollinsPronunciationDto
import top.blackcyan.collins.data.CollinsSearchResponse
import top.blackcyan.collins.domain.DidYouMeanResult
import top.blackcyan.collins.domain.DictionarySummary
import top.blackcyan.collins.domain.EntryDetail
import top.blackcyan.collins.domain.NearbyEntries
import top.blackcyan.collins.domain.Pronunciation
import top.blackcyan.collins.domain.SearchHit
import top.blackcyan.collins.domain.SearchPage
import top.blackcyan.collins.domain.Topic

interface CollinsRepository {
    suspend fun dictionaries(): List<DictionarySummary>
    suspend fun search(dictionaryCode: String, query: String, pageSize: Int = 10, pageIndex: Int = 1): SearchPage
    suspend fun didYouMean(dictionaryCode: String, query: String, entryNumber: Int = 10): DidYouMeanResult
    suspend fun firstMatch(dictionaryCode: String, query: String, format: String = "html"): EntryDetail
    suspend fun entry(dictionaryCode: String, entryId: String, format: String = "html"): EntryDetail?
    suspend fun pronunciations(dictionaryCode: String, entryId: String, lang: String? = null): List<Pronunciation>
    suspend fun nearbyEntries(dictionaryCode: String, entryId: String, entryNumber: Int = 10): NearbyEntries
}

class DefaultCollinsRepository(
    private val apiClient: CollinsApiClient,
) : CollinsRepository {
    override suspend fun dictionaries(): List<DictionarySummary> =
        apiClient.listDictionaries().toDomain()

    override suspend fun search(dictionaryCode: String, query: String, pageSize: Int, pageIndex: Int): SearchPage =
        apiClient.search(dictionaryCode, query, pageSize, pageIndex).toDomain(query)

    override suspend fun didYouMean(dictionaryCode: String, query: String, entryNumber: Int): DidYouMeanResult =
        apiClient.didYouMean(dictionaryCode, query, entryNumber).toDomain()

    override suspend fun firstMatch(dictionaryCode: String, query: String, format: String): EntryDetail =
        apiClient.firstMatch(dictionaryCode, query, format).toDomain()

    override suspend fun entry(dictionaryCode: String, entryId: String, format: String): EntryDetail? =
        apiClient.entry(dictionaryCode, entryId, format)?.toDomain()

    override suspend fun pronunciations(dictionaryCode: String, entryId: String, lang: String?): List<Pronunciation> =
        apiClient.pronunciations(dictionaryCode, entryId, lang).map {
            Pronunciation(
                dictionaryCode = it.dictionaryCode,
                entryId = it.entryId,
                lang = it.lang,
                url = it.pronunciationUrl,
            )
        }

    override suspend fun nearbyEntries(dictionaryCode: String, entryId: String, entryNumber: Int): NearbyEntries =
        apiClient.nearbyEntries(dictionaryCode, entryId, entryNumber).toDomain()
}

private fun List<CollinsDictionaryDto>.toDomain(): List<DictionarySummary> =
    map { it.toDomain() }

private fun CollinsSearchResponse.toDomain(query: String): SearchPage =
    SearchPage(
        dictionaryCode = dictionaryCode,
        query = query,
        resultNumber = resultNumber,
        pageNumber = pageNumber,
        currentPageIndex = currentPageIndex,
        results = results.map { it.toDomain() },
    )

private fun CollinsDidYouMeanResponse.toDomain(): DidYouMeanResult =
    DidYouMeanResult(
        dictionaryCode = dictionaryCode,
        searchTerm = searchTerm,
        suggestions = suggestions,
    )

private fun CollinsEntryDto.toDomain(): EntryDetail =
    EntryDetail(
        dictionaryCode = dictionaryCode,
        format = format,
        entryId = entryId,
        entryLabel = entryLabel,
        entryUrl = entryUrl,
        entryContent = entryContent,
        topics = topics.map { it.toDomain() },
        parsedEntry = runCatching { CollinsEntryContentParser.parse(entryContent) }.getOrNull(),
    )

private fun CollinsNearbyEntriesResponse.toDomain(): NearbyEntries =
    NearbyEntries(
        dictionaryCode = dictionaryCode,
        entryId = entryId,
        nearbyFollowingEntries = nearbyFollowingEntries.map { it.toDomain() },
        nearbyPrecedingEntries = nearbyPrecedingEntries.map { it.toDomain() },
    )

private fun top.blackcyan.collins.data.CollinsDictionaryDto.toDomain(): DictionarySummary =
    DictionarySummary(
        code = dictionaryCode,
        name = dictionaryName,
        url = dictionaryUrl,
    )


private fun top.blackcyan.collins.data.CollinsSearchHitDto.toDomain(): SearchHit =
    SearchHit(
        id = entryId,
        label = entryLabel,
        url = entryUrl,
    )

private fun CollinsNearbyEntryDto.toDomain(): SearchHit =
    SearchHit(
        id = entryId,
        label = entryLabel,
        url = entryUrl,
    )

private fun top.blackcyan.collins.data.CollinsTopicDto.toDomain(): Topic =
    Topic(
        id = topicId,
        label = topicLabel,
        url = topicUrl,
        parentId = topicParentId,
    )
