package top.blackcyan.collins.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val CollinsAcceptHeaderValue: String = "application/json"
const val CollinsAccessKeyHeader: String = "accessKey"
const val CollinsDefaultBaseUrl: String = "https://api.collinsdictionary.com"
const val CollinsApiVersionPrefix: String = "/api/v1"

data class CollinsApiConfig(
    val accessKey: String,
    val baseUrl: String = CollinsDefaultBaseUrl,
)

@Serializable
data class CollinsDictionaryDto(
    val dictionaryCode: String,
    val dictionaryName: String,
    val dictionaryUrl: String,
)

@Serializable
data class CollinsSearchHitDto(
    val entryId: String,
    val entryLabel: String,
    val entryUrl: String,
)

@Serializable
data class CollinsSearchResponse(
    val dictionaryCode: String,
    val resultNumber: Int,
    val pageNumber: Int,
    val currentPageIndex: Int,
    val results: List<CollinsSearchHitDto> = emptyList(),
)

@Serializable
data class CollinsDidYouMeanResponse(
    val dictionaryCode: String,
    val searchTerm: String,
    // Device-verified wire shape: suggestions is a bare JSON array of strings.
    val suggestions: List<String> = emptyList(),
)

@Serializable
data class CollinsTopicDto(
    val topicId: String,
    val topicLabel: String,
    val topicUrl: String,
    val topicParentId: String? = null,
)

@Serializable
data class CollinsEntryDto(
    val dictionaryCode: String,
    val format: String,
    val entryContent: String,
    val entryId: String,
    val entryLabel: String,
    val entryUrl: String,
    val topics: List<CollinsTopicDto> = emptyList(),
)

@Serializable
data class CollinsPronunciationDto(
    val dictionaryCode: String,
    val entryId: String,
    val lang: String,
    @SerialName("pronunciationUrl") val pronunciationUrl: String,
)

@Serializable
data class CollinsNearbyEntryDto(
    val entryId: String,
    val entryLabel: String,
    val entryUrl: String,
)

@Serializable
data class CollinsNearbyEntriesResponse(
    val dictionaryCode: String,
    val entryId: String,
    val nearbyFollowingEntries: List<CollinsNearbyEntryDto> = emptyList(),
    val nearbyPrecedingEntries: List<CollinsNearbyEntryDto> = emptyList(),
)

val CollinsJson: kotlinx.serialization.json.Json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}
