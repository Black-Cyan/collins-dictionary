package top.blackcyan.collins.domain

data class DictionarySummary(
    val code: String,
    val name: String,
    val url: String,
)

data class SearchHit(
    val id: String,
    val label: String,
    val url: String,
)

data class SearchPage(
    val dictionaryCode: String,
    val query: String,
    val resultNumber: Int,
    val pageNumber: Int,
    val currentPageIndex: Int,
    val results: List<SearchHit>,
)

data class DidYouMeanResult(
    val dictionaryCode: String,
    val searchTerm: String,
    val suggestions: List<String>,
)

data class Topic(
    val id: String,
    val label: String,
    val url: String,
    val parentId: String?,
)

data class EntryDetail(
    val dictionaryCode: String,
    val format: String,
    val entryId: String,
    val entryLabel: String,
    val entryUrl: String,
    val entryContent: String,
    val topics: List<Topic>,
    /** Structured view of [entryContent]; null when the HTML could not be recognized. */
    val parsedEntry: ParsedEntry? = null,
)

data class Pronunciation(
    val dictionaryCode: String,
    val entryId: String,
    val lang: String,
    val url: String,
)

data class NearbyEntries(
    val dictionaryCode: String,
    val entryId: String,
    val nearbyFollowingEntries: List<SearchHit>,
    val nearbyPrecedingEntries: List<SearchHit>,
)

/**
 * A headword is the word the user recognizes ("car"). One headword maps to one
 * or more Collins [EntryDetail] resources ("car_1", "car_2", ...) when the same
 * spelling covers multiple etymological entries (homographs).
 */
data class Headword(
    val label: String,
    val entryIds: List<String>,
)

/** Outcome of the four-stage search resolution: normalize, exact, search, did-you-mean. */
sealed interface SearchResolution {
    /** The normalized query matched a headword exactly; navigate straight to it. */
    data class ExactHeadword(
        val label: String,
        val entryIds: List<String>,
    ) : SearchResolution

    /** No exact match; one row per distinct headword, in the API's result order. */
    data class Matches(
        val headwords: List<Headword>,
        val totalEntries: Int,
    ) : SearchResolution

    data class DidYouMean(
        val query: String,
        val suggestions: List<String>,
    ) : SearchResolution

    data object NotFound : SearchResolution
}

