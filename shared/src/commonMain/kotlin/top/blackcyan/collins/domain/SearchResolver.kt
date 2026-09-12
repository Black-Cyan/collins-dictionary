package top.blackcyan.collins.domain

import top.blackcyan.collins.repository.CollinsRepository

private val WHITESPACE_RUN = Regex("\\s+")
private val ENTRY_ID_HOMOGRAPH_SUFFIX = Regex("_\\d+$")
private val TRAILING_HOMOGRAPH_DIGITS = Regex("\\s*\\d+$")

/**
 * Normalization applied before every lookup: trim edges, collapse inner
 * whitespace runs, lowercase. Deliberately conservative — apostrophe, hyphen
 * and accent folding must wait until Collins' own data rules are verified.
 */
fun normalizeQuery(query: String): String =
    query.trim().lowercase().replace(WHITESPACE_RUN, " ")

/** Entry id without its homograph suffix: `take_1` -> `take`. */
fun entryIdStem(entryId: String): String = entryId.replace(ENTRY_ID_HOMOGRAPH_SUFFIX, "")

/**
 * Resolves a raw user query against Collins using the wiki-style pipeline:
 *
 * normalize -> exact headword match within /search results -> grouped result
 * list -> did-you-mean -> not found.
 *
 * Exactness is decided from the headword labels of a single /search response
 * rather than from /search/first, whose "best match" semantics can return
 * "apple" for "appl". All entries sharing the matched headword are aggregated.
 *
 * Collins sometimes spells homograph labels with a digit ("take1", "take 2")
 * while the entry ids carry the conventional `_N` suffix (take_1, take_2).
 * When stripping the trailing digit from a label yields the entry id stem,
 * the structural id wins and both rows collapse onto one headword ("take").
 * Phrase labels are safe: "application form" cannot equal id stem
 * "application-form".
 */
class SearchResolver(
    private val repository: CollinsRepository,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    suspend fun resolve(dictionaryCode: String, rawQuery: String): SearchResolution {
        val query = normalizeQuery(rawQuery)
        val page = repository.search(
            dictionaryCode = dictionaryCode,
            query = query,
            pageSize = pageSize,
        )

        val groups = groupHeadwords(page.results)
        val byKey = groups.associateBy { it.key }

        val exact = byKey[query]
            ?: byKey[query.replace(TRAILING_HOMOGRAPH_DIGITS, "")]?.takeIf { it.collapsedFromDigits }
        if (exact != null) {
            return SearchResolution.ExactHeadword(
                label = exact.displayLabel,
                entryIds = exact.hits.map { it.id },
            )
        }

        if (groups.isNotEmpty()) {
            return SearchResolution.Matches(
                headwords = groups.map { group ->
                    Headword(label = group.displayLabel, entryIds = group.hits.map { it.id })
                },
                totalEntries = page.resultNumber,
            )
        }

        val suggestions = runCatching {
            repository.didYouMean(dictionaryCode = dictionaryCode, query = query).suggestions
        }.getOrDefault(emptyList())

        return if (suggestions.isEmpty()) {
            SearchResolution.NotFound
        } else {
            SearchResolution.DidYouMean(query = query, suggestions = suggestions)
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20

        internal fun groupHeadwords(hits: List<SearchHit>): List<HeadwordGroup> {
            // LinkedHashMap keeps first-seen order for the grouped result list.
            val groups = LinkedHashMap<String, HeadwordGroup>()
            hits.forEach { hit ->
                val labelKey = normalizeQuery(hit.label)
                val idStem = entryIdStem(hit.id)
                val labelWithoutDigits = labelKey.replace(TRAILING_HOMOGRAPH_DIGITS, "")
                val collapsed = labelWithoutDigits == idStem && labelKey != idStem
                val key = if (labelWithoutDigits == idStem) idStem else labelKey
                val existing = groups[key]
                groups[key] = if (existing == null) {
                    HeadwordGroup(
                        key = key,
                        hits = mutableListOf(hit),
                        collapsedFromDigits = collapsed,
                    )
                } else {
                    existing.copy(
                        hits = existing.hits + hit,
                        collapsedFromDigits = existing.collapsedFromDigits || collapsed,
                    )
                }
            }
            return groups.values.map { group ->
                // Prefer a label that already spells the stem without a digit;
                // otherwise strip the digit run from the first collins label.
                val displayLabel = group.hits.firstOrNull { normalizeQuery(it.label) == group.key }?.label
                    ?: group.hits.first().label.replace(Regex("\\s*\\d+$"), "")
                group.copy(displayLabel = displayLabel)
            }
        }
    }

    internal data class HeadwordGroup(
        val key: String,
        val hits: List<SearchHit>,
        val collapsedFromDigits: Boolean,
        val displayLabel: String = hits.first().label,
    )
}
