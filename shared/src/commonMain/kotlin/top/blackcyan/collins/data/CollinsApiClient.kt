package top.blackcyan.collins.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface CollinsApiClient {
    suspend fun listDictionaries(): List<CollinsDictionaryDto>
    suspend fun search(dictionaryCode: String, query: String, pageSize: Int = 10, pageIndex: Int = 1): CollinsSearchResponse
    suspend fun didYouMean(dictionaryCode: String, query: String, entryNumber: Int = 10): CollinsDidYouMeanResponse
    suspend fun firstMatch(dictionaryCode: String, query: String, format: String = "html"): CollinsEntryDto
    suspend fun entry(dictionaryCode: String, entryId: String, format: String = "html"): CollinsEntryDto?
    suspend fun pronunciations(dictionaryCode: String, entryId: String, lang: String? = null): List<CollinsPronunciationDto>
    suspend fun nearbyEntries(dictionaryCode: String, entryId: String, entryNumber: Int = 10): CollinsNearbyEntriesResponse
}

class HttpCollinsApiClient(
    private val httpClient: HttpClient,
    private val config: CollinsApiConfig,
    private val json: Json = CollinsJson,
) : CollinsApiClient {
    override suspend fun listDictionaries(): List<CollinsDictionaryDto> =
        get("$CollinsApiVersionPrefix/dictionaries")

    override suspend fun search(dictionaryCode: String, query: String, pageSize: Int, pageIndex: Int): CollinsSearchResponse =
        get("$CollinsApiVersionPrefix/dictionaries/$dictionaryCode/search/") {
            parameters.append("q", query)
            parameters.append("pagesize", pageSize.toString())
            parameters.append("pageindex", pageIndex.toString())
        }

    override suspend fun didYouMean(dictionaryCode: String, query: String, entryNumber: Int): CollinsDidYouMeanResponse =
        get("$CollinsApiVersionPrefix/dictionaries/$dictionaryCode/search/didyoumean/") {
            parameters.append("q", query)
            parameters.append("entrynumber", entryNumber.toString())
        }

    override suspend fun firstMatch(dictionaryCode: String, query: String, format: String): CollinsEntryDto =
        get("$CollinsApiVersionPrefix/dictionaries/$dictionaryCode/search/first/") {
            parameters.append("q", query)
            parameters.append("format", format)
        }

    override suspend fun entry(dictionaryCode: String, entryId: String, format: String): CollinsEntryDto? {
        // The API returns an empty JSON object ("{}") when the entry id is unknown.
        val payload: kotlinx.serialization.json.JsonObject =
            get("$CollinsApiVersionPrefix/dictionaries/$dictionaryCode/entries/$entryId") {
                parameters.append("format", format)
            }
        return try {
            payload.takeIf { it.isNotEmpty() }?.let {
                json.decodeFromJsonElement(CollinsEntryDto.serializer(), it)
            }
        } catch (exception: SerializationException) {
            throw CollinsApiException.DecodingFailure(payload.toString(), exception)
        }
    }

    override suspend fun pronunciations(dictionaryCode: String, entryId: String, lang: String?): List<CollinsPronunciationDto> =
        get("$CollinsApiVersionPrefix/dictionaries/$dictionaryCode/entries/$entryId/pronunciations") {
            lang?.let { parameters.append("lang", it) }
        }

    override suspend fun nearbyEntries(dictionaryCode: String, entryId: String, entryNumber: Int): CollinsNearbyEntriesResponse =
        get("$CollinsApiVersionPrefix/dictionaries/$dictionaryCode/entries/$entryId/nearbyentries") {
            parameters.append("entrynumber", entryNumber.toString())
        }

    private suspend inline fun <reified T> get(
        path: String,
        crossinline block: io.ktor.http.URLBuilder.() -> Unit = {},
    ): T {
        val response = httpClient.get(config.baseUrl.withoutTrailingSlash() + path) {
            header(HttpHeaders.Accept, CollinsAcceptHeaderValue)
            header(CollinsAccessKeyHeader, config.accessKey)
            url {
                block()
            }
        }

        val body = response.bodyAsText()

        if (response.status.value !in 200..299) {
            throw CollinsApiException.HttpFailure(response.status, body)
        }

        return try {
            json.decodeFromString<T>(body)
        } catch (exception: SerializationException) {
            throw CollinsApiException.DecodingFailure(body, exception)
        }
    }
}

private fun String.withoutTrailingSlash(): String =
    if (endsWith('/')) dropLast(1) else this

sealed class CollinsApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class HttpFailure(
        val status: HttpStatusCode,
        val responseBody: String,
    ) : CollinsApiException("Collins API request failed with status ${status.value}")

    class DecodingFailure(
        val responseBody: String,
        cause: Throwable,
    ) : CollinsApiException("Unable to decode Collins API response", cause)
}
