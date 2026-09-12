# Project Context

Glossary of the Collins dictionary domain as this app understands it. Terms only — implementation decisions live in `docs/adr/`.

## Collins resources

- **Dictionary** — A Collins dictionary collection, e.g. the English dictionary code `english`. Selected before searching.
- **Entry** — One dictionary resource, identified by an entry id such as `car_1`. It carries a label (`car`), HTML content, topics and pronunciations.
- **Homograph** — One of several entries that share the same spelling (`car_1`, `car_2`, …). Collins models them as separate entries; the user sees them as one word.
- **Headword** — The word a user recognizes and types ("car"). A headword aggregates **one or more** entries sharing a label after normalization. It is the unit shown as a search-result row and the unit whose detail page opens.
- **Sense section** — The detail-page presentation of one aggregated entry of a headword, shown in Collins' entry order (`car_1`, `car_2`, …). Note: Collins' HTML content inside an entry contains linguistic senses; the app does not split those — this section is the whole entry.
- **Pronunciation** — A per-entry audio resource (with a language code), never shared between entries.
- **Topic** — A categorization label attached to an entry, itself a Collins resource (e.g. a subject area).
- **Nearby entries** — The entries immediately before/after a given entry in dictionary order. They anchor on the headword's **primary (first) entry**.

## Search resolution

The pipeline that turns a raw query into a screen outcome:

1. **Normalization** — Trim edges, collapse internal whitespace runs, lowercase. Deliberately conservative; no apostrophe/hyphen/accent folding.
2. **Exact Match** — A result whose normalized label equals the normalized query. All entries sharing that label aggregate into one headword and open its detail page directly.
3. **Search / Best Matches** — No exact match: search results are grouped one row per distinct headword label, in the API's first-seen order.
4. **Did You Mean** — No results at all, but Collins returns spelling suggestions: show them.
5. **Not Found** — No results and no suggestions.

- **Best Match** — Collins' `/search/first` "best guess" behaviour (e.g. `appl` can return `apple`). The app deliberately does **not** use it: it would silently replace the user's query, so exactness is decided from `/search` labels instead.
- **Primary entry** — The first entry id of a headword (`headword_1` in practice). Used as the anchor for nearby entries.

## Navigation

- Detail pages are addressed by **headword label**, not by entry id: nearby-entry clicks carry only a label, so opening a detail page without a known id list re-runs search and re-resolves the matching entry ids.
