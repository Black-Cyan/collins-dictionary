# 0001 — Aggregate homographs into one headword, navigate by label

Date: 2026-09-11

## Context

Collins models the same spelling with multiple etymological entries as separate resources: `car_1`, `car_2`, … each with its own id, content, topics, and pronunciations. Search results list them as separate rows. Users think of "car" as one word.

Collins also offers `/search/first` ("best match"), which returns a single entry it guesses the user wants — it can return `apple` for the query `appl`, silently substituting a different word.

Nearby-entry navigation (`/entries/{id}/nearby`) returns entries carrying labels and ids, and the detail screen must be reachable from those rows as well.

## Decision

- Introduce a **Headword** domain entity: a label plus the ordered list of entry ids sharing that normalized label. Homographs aggregate into one headword; the detail page renders every entry as a numbered sense section.
- Search resolution (`SearchResolver`) makes **one** `/search` call (page size 20), groups hits by normalized label, and declares an Exact Match when a grouped label equals the normalized query. `/search/first` is not used.
- Detail pages are addressed by headword **label**. When entry ids are known from the resolution they are passed along; label-only navigation (nearby entries) re-runs `/search` and filters by normalized label to resolve ids.
- Nearby entries anchor on the primary (first) entry of the headword.
- Each sense section and the nearby block fail independently: one unreadable entry or pronunciation set never hides the sections that loaded.

## Consequences

- One word = one result row and one detail page; homographs are navigable via numbered quick-jump chips.
- Exactness is deterministic from labels; no query substitution.
- Cost: opening a detail page via nearby navigation costs one extra `/search` request before entry loads. Acceptable given it is a click-through action; cached resolution could be added later.
- Exact-match coverage is limited to the first 20 search hits. Collins treats homograph numbers by ordinal, so same-label homographs sort together; revisit pagination only if evidence shows labels split across pages.
