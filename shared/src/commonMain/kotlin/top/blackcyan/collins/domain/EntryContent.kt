package top.blackcyan.collins.domain

/**
 * Structured view of a Collins entry's HTML content, produced by
 * `CollinsEntryContentParser`. Only the class vocabulary observed in real
 * Collins responses is modelled; anything unknown is flattened into text.
 */
data class ParsedEntry(
    /** Headword text without the homograph number. */
    val headword: String,
    /** Raw homograph digit ("2") shown inside the h1, when Collins sets one. */
    val homographNumber: String?,
    /** Dictionary accent of the entry: "UK" / "US", or null when undeclared. */
    val variant: String?,
    val pronunciations: List<ParsedPronunciation>,
    /** Part-of-speech sections in document order (an entry may have several). */
    val sections: List<EntrySection>,
    /** `> derived` blocks appended after the sections (e.g. takable). */
    val derivedTerms: List<DerivedTerm>,
    val etymology: RichText?,
)

data class ParsedPronunciation(
    val ipa: RichText,
    /** Regional/language label attached to this pron block (e.g. "French"); null = entry's primary accent. */
    val label: String?,
    val audioUrl: String?,
)

sealed interface EntrySection {
    data class Classified(
        val partOfSpeech: String?,
        /** Inflected forms ("takes", "taking", "took", "taken"; "-plies"…). */
        val inflections: List<String>,
        /** Section-level usage note, e.g. "(mainly transitive)". */
        val grammar: RichText?,
        /** Register/domain labels scoped to the whole section (e.g. "fencing"). */
        val labels: List<String>,
        val senses: List<SenseNode>,
    ) : EntrySection

    /** Section whose recognized structure failed to parse; text is still shown. */
    data class Unparsed(val text: String) : EntrySection
}

data class SenseNode(
    /** Numbering text straight from Collins ("47.", "a.", "b."); null when unnumbered. */
    val number: String?,
    /** Register/domain labels ("informal", "logic", "New Zealand"). */
    val labels: List<String>,
    /** Syntactic usage ("(transitive; often foll by for)"). */
    val grammar: RichText?,
    /** Definition text; may hold cross-reference links. */
    val definition: RichText?,
    val examples: List<RichText>,
    val children: List<SenseNode>,
)

data class DerivedTerm(
    val heading: RichText,
    val partOfSpeech: String?,
)

// ---- Inline rich text -------------------------------------------------------

enum class InlineSpanStyle { PLAIN, ITALIC, BOLD }

data class InlineText(
    val text: String,
    val style: InlineSpanStyle = InlineSpanStyle.PLAIN,
    val superscript: Boolean = false,
)

sealed interface InlineNode {
    data class Text(val inline: InlineText) : InlineNode

    /** Anchor carrying a Collins entry id (the `data-topic` attribute), navigable in-app. */
    data class Link(
        val parts: List<InlineText>,
        val targetEntryId: String?,
    ) : InlineNode
}

typealias RichText = List<InlineNode>

/** Plain concatenation of a rich text run, ignoring styles and links. */
fun RichText.plainText(): String = buildString {
    this@plainText.forEach { node ->
        when (node) {
            is InlineNode.Text -> append(node.inline.text)
            is InlineNode.Link -> node.parts.forEach { append(it.text) }
        }
    }
}.trim()
