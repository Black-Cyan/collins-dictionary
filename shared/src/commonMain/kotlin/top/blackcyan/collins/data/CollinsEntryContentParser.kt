package top.blackcyan.collins.data

import top.blackcyan.collins.domain.EntrySection
import top.blackcyan.collins.domain.DerivedTerm
import top.blackcyan.collins.domain.InlineNode
import top.blackcyan.collins.domain.InlineSpanStyle
import top.blackcyan.collins.domain.InlineText
import top.blackcyan.collins.domain.ParsedEntry
import top.blackcyan.collins.domain.ParsedPronunciation
import top.blackcyan.collins.domain.RichText
import top.blackcyan.collins.domain.SenseNode
import top.blackcyan.collins.domain.plainText

/**
 * Tolerant parser for the small, machine-generated HTML vocabulary Collins
 * puts in `entryContent`. It recognizes the classes verified against live
 * responses (hwd/homnum, pron, hom, gramGrp, pos, subc, colloc, lbl, sense,
 * sensenum, def, cit/quote, xr/re, etym, orth, formTypeInfl, hi/bold, audio);
 * anything unrecognized is flattened into text. A section that fails to map
 * becomes [EntrySection.Unparsed]; parse() itself returns null only when no
 * `.entry` node exists at all.
 */
object CollinsEntryContentParser {

    fun parse(html: String): ParsedEntry? {
        val root = MiniHtmlParser.parse(html)
        val entry = root.firstDescendant { it.classes.contains("entry") } ?: return null
        val variant = entry.classes.firstOrNull { it.startsWith("lang_") }
            ?.removePrefix("lang_")
            ?.let { langToken ->
                when {
                    langToken.startsWith("en-gb") -> "UK"
                    langToken.startsWith("en-us") -> "US"
                    else -> langToken.replace('-', ' ').uppercase()
                }
            }

        val headerInline = entry.children.firstOrNull { it.tag == "span" && it.classes.contains("inline") }
        val hwd = headerInline?.firstDescendant { it.tag == "h1" && it.classes.contains("hwd") }
        val homographNumber = hwd?.firstDescendant { it.classes.contains("homnum") }?.textContent()?.trim()

        val pronunciations = buildList {
            headerInline?.children?.filter { it.tag == "span" && it.classes.contains("pron") }
                ?.forEach { add(it.toPronunciation()) }
        }.filter { it.ipa.plainText().isNotEmpty() || it.audioUrl != null }

        val sections = mutableListOf<EntrySection>()
        val derivedTerms = mutableListOf<DerivedTerm>()
        var etymology: RichText? = null

        for (child in entry.children) {
            when {
                child.tag == "div" && child.classes.contains("hom") -> {
                    runCatching { child.toSection() }
                        .getOrElse { EntrySection.Unparsed(child.textContent().cleanSpacing()) }
                        .let { sections.add(it) }
                }
                child.tag == "div" && child.classes.contains("etym") -> {
                    child.toRichText(dropWrapper = true).takeIf { it.plainText().isNotEmpty() }?.let { etymology = it }
                }
                child.tag == "span" && child.classes.contains("re") -> {
                    child.toDerivedTerm()?.let(derivedTerms::add)
                }
            }
        }

        val headword = (hwd?.let { hw ->
            // The homnum digit is a child span; text of the hwd minus it.
            buildRichText(hw) { node -> !node.classes.contains("homnum") }.plainText()
        } ?: "").trim()

        return ParsedEntry(
            headword = headword,
            homographNumber = homographNumber,
            variant = variant,
            pronunciations = pronunciations,
            sections = sections,
            derivedTerms = derivedTerms,
            etymology = etymology,
        )
    }

    private fun HtmlNode.toSection(): EntrySection.Classified {
        val pos = firstDescendant { it.tag == "span" && it.classes.contains("pos") }?.textContent()?.trim()

        // Inflections: formTypeInfl wrappers (takes/taking/…) and bare inline
        // orth groups directly inside the hom (-plies/-plying/-plied).
        val inflections = buildList {
            descendants { it.tag == "span" && it.classes.contains("orth") }
                .filter { orth ->
                    val parent = orth.parent
                    parent != null &&
                        (parent.classes.contains("formTypeInfl") ||
                            (parent.tag == "span" && parent.classes.contains("inline") && parent.parent === this@toSection))
                }
                .forEach { add(it.textContent().cleanSpacing()) }
        }.filter { it.isNotBlank() }.distinct()

        val senses = children.filter { it.tag == "div" && it.classes.contains("sense") }
            .mapNotNull { it.toSense() }

        // A hom-level gramGrp carrying subc/colloc (but no pos) is a section
        // usage note such as "(mainly transitive)".
        val sectionGrammar = children.firstOrNull {
            it.tag == "span" && it.classes.contains("gramGrp") &&
                it.firstDescendant { n -> n.classes.contains("subc") || n.classes.contains("colloc") } != null
        }?.toRichText(dropWrapper = true)?.takeIf { it.plainText().isNotEmpty() }

        // Register labels placed directly under the hom (before the senses).
        val sectionLabels = children.filter { it.tag == "span" && it.classes.contains("lbl") }
            .map { it.textContent().cleanSpacing().trim().removePrefix(",").trim() }
            .filter { it.isNotEmpty() }

        return EntrySection.Classified(
            partOfSpeech = pos,
            inflections = inflections,
            grammar = sectionGrammar,
            labels = sectionLabels,
            senses = senses,
        )
    }

    private fun HtmlNode.toSense(): SenseNode? {
        val number = children.firstOrNull { it.tag == "span" && it.classes.contains("sensenum") }
            ?.textContent()?.trim()?.takeIf { it.isNotEmpty() }

        val labels = children.filter { it.tag == "span" && it.classes.contains("lbl") }
            .map { it.textContent().cleanSpacing().trim().removePrefix(",").trim() }
            .filter { it.isNotEmpty() }

        val grammar = children.firstOrNull { it.tag == "span" && it.classes.contains("gramGrp") }
            ?.takeIf { it.firstDescendant { n -> n.classes.contains("subc") || n.classes.contains("colloc") } != null }
            ?.toRichText(dropWrapper = true)
            ?.takeIf { it.plainText().isNotEmpty() }

        val examples = children.filter { it.tag == "span" && it.classes.contains("cit") }
            .mapNotNull { cit ->
                cit.firstDescendant { it.tag == "span" && it.classes.contains("quote") }
                    ?.toRichText(dropWrapper = true)
                    ?.withoutLeadingArrows()
            }
            .filter { it.plainText().isNotEmpty() }

        val crossReference = children.firstOrNull {
            (it.tag == "span" && (it.classes.contains("re") || it.classes.contains("xr")))
        }?.toRichText(dropWrapper = true)?.takeIf { it.plainText().isNotEmpty() }

        val definition = children.firstOrNull { it.tag == "span" && it.classes.contains("def") }
            ?.toRichText(dropWrapper = true)
            ?.takeIf { it.plainText().isNotEmpty() }
            ?: crossReference

        val nested = children.filter { it.tag == "div" && it.classes.contains("sense") }
            .mapNotNull { it.toSense() }

        if (number == null && grammar == null && definition == null && nested.isEmpty()) return null
        return SenseNode(
            number = number,
            labels = labels,
            grammar = grammar,
            definition = definition,
            examples = examples,
            children = nested,
        )
    }

    private fun HtmlNode.toPronunciation(): ParsedPronunciation {
        val label = children.firstOrNull { it.tag == "span" && it.classes.contains("lbl") }
            ?.textContent()?.cleanSpacing()?.trim()
        val audioUrl = descendants { it.tag == "source" }
            .firstNotNullOfOrNull { it.attr("src") }
        val ipa = toRichText { node ->
            // Drop the clickable gif and the HTML5-audio fallback entirely; the
            // regional lbl is captured separately so it never mixes into IPA.
            val byPlayback = node.tag == "a" && node.classes.contains("playback") ||
                generateSequence(node.parent) { it.parent }.any { it.tag == "a" && it.classes.contains("playback") }
            !byPlayback && node.tag != "audio" && node.tag != "source" && node.tag != "img" &&
                !node.classes.contains("lbl")
        }
        return ParsedPronunciation(ipa = ipa.trimLeadingPunctuation(), label = label, audioUrl = audioUrl)
    }

    private fun RichText.trimLeadingPunctuation(): RichText {
        val first = firstOrNull() as? InlineNode.Text ?: return this
        val cleaned = first.inline.text.replace(Regex("^[\\s,、]+"), "")
        if (cleaned == first.inline.text) return this
        return toMutableList().also { it[0] = InlineNode.Text(first.inline.copy(text = cleaned)) }
    }

    private fun HtmlNode.toDerivedTerm(): DerivedTerm? {
        val pos = firstDescendant { it.tag == "span" && it.classes.contains("pos") }?.textContent()?.trim()
        // The nested hom only carries the part of speech; the term itself is
        // everything else in the re block.
        val heading = toRichText { node ->
            val insideHom = generateSequence(node.parent) { it.parent }
                .any { it.tag == "div" && it.classes.contains("hom") }
            !insideHom && node.tag != "br"
        }.let { text ->
            text.map { node ->
                if (node is InlineNode.Text) node.copy(inline = node.inline.copy(text = node.inline.text.replace("&gt;", ">"))) else node
            }
        }.takeIf { it.plainText().isNotEmpty() } ?: return null
        return DerivedTerm(heading = heading, partOfSpeech = pos)
    }

    // ---- Inline rendering ---------------------------------------------------

    private fun HtmlNode.toRichText(
        dropWrapper: Boolean = false,
        includeNode: (HtmlNode) -> Boolean = { true },
    ): RichText {
        val startNodes = if (dropWrapper) children else listOf(this)
        return buildRichText(startNodes, includeNode)
    }

    private fun buildRichText(
        roots: List<HtmlNode>,
        includeNode: (HtmlNode) -> Boolean,
    ): RichText {
        val out = mutableListOf<InlineNode>()
        val boldOpen = ArrayDeque<Boolean>()
        val italicOpen = ArrayDeque<Boolean>()
        val supOpen = ArrayDeque<Boolean>()

        fun currentStyle(): InlineSpanStyle = when {
            boldOpen.lastOrNull() == true -> InlineSpanStyle.BOLD
            italicOpen.lastOrNull() == true -> InlineSpanStyle.ITALIC
            else -> InlineSpanStyle.PLAIN
        }

        fun emitText(raw: String) {
            if (raw.isEmpty()) return
            val text = InlineText(
                text = raw,
                style = currentStyle(),
                superscript = supOpen.any { it },
            )
            val last = out.lastOrNull()
            if (last is InlineNode.Text &&
                last.inline.style == text.style &&
                last.inline.superscript == text.superscript
            ) {
                out[out.lastIndex] = InlineNode.Text(last.inline.copy(text = last.inline.text + raw))
            } else {
                out.add(InlineNode.Text(text))
            }
        }

        fun emitLink(node: HtmlNode) {
            val target = node.attr("data-topic")?.takeIf { it.isNotBlank() }
            val parts = mutableListOf<InlineText>()
            fun collect(nodes: List<HtmlNode>, bold: Boolean, italic: Boolean, sup: Boolean) {
                for (child in nodes) {
                    if (child.tag == "img" || child.tag == "br") continue
                    val childBold = bold || child.isBold()
                    val childItalic = italic || child.isItalic()
                    val childSup = sup || child.tag == "sup"
                    if (child.isText) {
                        child.text?.let { raw ->
                            parts.add(
                                InlineText(
                                    raw,
                                    if (childBold) InlineSpanStyle.BOLD else if (childItalic) InlineSpanStyle.ITALIC else InlineSpanStyle.PLAIN,
                                    childSup,
                                ),
                            )
                        }
                    }
                    collect(child.children, childBold, childItalic, childSup)
                }
            }
            collect(node.children, boldOpen.any { it }, italicOpen.any { it }, supOpen.any { it })
            val merged = parts.mergeAdjacentText()
            if (merged.isNotEmpty()) out.add(InlineNode.Link(merged, target))
        }

        fun walk(nodes: List<HtmlNode>) {
            for (node in nodes) {
                if (!includeNode(node)) continue
                when {
                    node.isText -> emitText(node.text ?: "")
                    node.tag == "img" || node.tag == "br" || node.tag == "audio" || node.tag == "source" -> Unit
                    node.tag == "a" -> {
                        if (node.attr("data-topic") != null) emitLink(node)
                        // playback / href="#" anchors are non-content
                    }
                    else -> {
                        boldOpen.addLast(node.isBold())
                        italicOpen.addLast(node.isItalic())
                        supOpen.addLast(node.tag == "sup")
                        walk(node.children)
                        supOpen.removeLast()
                        italicOpen.removeLast()
                        boldOpen.removeLast()
                    }
                }
            }
        }

        walk(roots)
        return out.normalized()
    }

    private fun buildRichText(root: HtmlNode, includeNode: (HtmlNode) -> Boolean): RichText =
        buildRichText(listOf(root), includeNode)

    private fun HtmlNode.isBold(): Boolean =
        tag == "b" || classes.contains("bold")

    private fun HtmlNode.isItalic(): Boolean =
        tag == "em" || tag == "i" || classes.contains("hi") || classes.contains("ital")

    private fun MutableList<InlineText>.mergeAdjacentText(): List<InlineText> {
        val result = mutableListOf<InlineText>()
        for (part in this) {
            val last = result.lastOrNull()
            if (last != null && last.style == part.style && last.superscript == part.superscript) {
                result[result.lastIndex] = last.copy(text = last.text + part.text)
            } else {
                result.add(part)
            }
        }
        return result
    }

    /** Whitespace/entity cleanup and merges same-style neighbours across links. */
    private fun RichText.normalized(): RichText {
        val cleaned = map { node ->
            when (node) {
                is InlineNode.Text -> InlineNode.Text(node.inline.copy(text = node.inline.text.cleanSpacing()))
                is InlineNode.Link -> node.copy(
                    parts = node.parts.map { it.copy(text = it.text.cleanSpacing()) }
                        .filter { it.text.isNotEmpty() },
                )
            }
        }.filter {
            when (it) {
                is InlineNode.Text -> it.inline.text.isNotEmpty()
                is InlineNode.Link -> it.parts.isNotEmpty()
            }
        }
        return cleaned
    }

    private fun RichText.withoutLeadingArrows(): RichText = map { node ->
        when (node) {
            is InlineNode.Text -> InlineNode.Text(node.inline.copy(text = node.inline.text.replace(Regex("^\\s*[⇒>]\\s*"), "")))
            is InlineNode.Link -> node
        }
    }.filter {
        when (it) {
            is InlineNode.Text -> it.inline.text.isNotEmpty()
            is InlineNode.Link -> true
        }
    }

    private fun String.cleanSpacing(): String =
        replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,.;:!?)])"), "$1")
            .replace(Regex("([(])\\s+"), "$1")
}

// ---- Minimal tolerant HTML tree ----------------------------------------------

internal data class HtmlNode(
    val tag: String,
    val attributes: Map<String, String>,
    val children: MutableList<HtmlNode> = mutableListOf(),
    var parent: HtmlNode? = null,
    val isText: Boolean = false,
    var text: String? = null,
) {
    val classes: List<String>
        get() = attributes["class"]?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()

    fun attr(name: String): String? = attributes[name]

    fun firstDescendant(predicate: (HtmlNode) -> Boolean): HtmlNode? {
        for (child in children) {
            if (predicate(child)) return child
            child.firstDescendant(predicate)?.let { return it }
        }
        return null
    }

    fun descendants(predicate: (HtmlNode) -> Boolean): List<HtmlNode> {
        val result = mutableListOf<HtmlNode>()
        fun walk(nodes: List<HtmlNode>) {
            for (node in nodes) {
                if (predicate(node)) result.add(node)
                walk(node.children)
            }
        }
        walk(children)
        return result
    }

    fun textContent(): String = buildString {
        if (isText) append(text)
        children.forEach { append(it.textContent()) }
    }
}

internal object MiniHtmlParser {
    private val VOID_TAGS = setOf("br", "img", "source", "hr", "input", "meta", "link", "col")
    private val TAG =
        Regex("""(?is)<!--.*?-->|<!\[CDATA\[.*?]]>|<!DOCTYPE[^>]*>|</?([a-z0-9]+)((?:\s+[^\s"'>/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+))?)*)\s*/?>""")
    private val ATTR = Regex("""([^\s"'>/=]+)(?:\s*=\s*("([^"]*)"|'([^']*)'|([^\s>]+)))?""")

    fun parse(html: String): HtmlNode {
        val root = HtmlNode("#root", emptyMap())
        val stack = ArrayDeque<HtmlNode>().apply { addLast(root) }

        var cursor = 0
        for (match in TAG.findAll(html)) {
            if (match.range.first > cursor) {
                appendText(root, stack, html.substring(cursor, match.range.first))
            }
            cursor = match.range.last + 1
            val token = match.value
            when {
                token.startsWith("<!") -> Unit
                token.startsWith("</") -> {
                    val name = Regex("(?i)</([a-z0-9]+)").find(token)!!.groupValues[1].lowercase()
                    // Pop back to the nearest open match; tolerate imbalance.
                    val index = stack.indexOfLast { it.tag == name }
                    if (index > 0) {
                        while (stack.size > index) stack.removeLast()
                    }
                }
                else -> {
                    val name = Regex("(?i)^<([a-z0-9]+)").find(token)!!.groupValues[1].lowercase()
                    val attrs = parseAttributes(match.groupValues[2])
                    val node = HtmlNode(name, attrs)
                    node.parent = stack.last()
                    stack.last().children.add(node)
                    val selfClosing = token.endsWith("/>")
                    if (!VOID_TAGS.contains(name) && !selfClosing) {
                        stack.addLast(node)
                    }
                }
            }
        }
        if (cursor < html.length) appendText(root, stack, html.substring(cursor))
        return root
    }

    private fun appendText(root: HtmlNode, stack: ArrayDeque<HtmlNode>, raw: String) {
        if (raw.isEmpty()) return
        val decoded = decodeEntities(raw)
        if (decoded.isEmpty()) return
        val textNode = HtmlNode("#text", emptyMap(), isText = true, text = decoded)
        textNode.parent = stack.last()
        stack.last().children.add(textNode)
    }

    private fun parseAttributes(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        ATTR.findAll(raw).forEach { m ->
            val name = m.groupValues[1].lowercase()
            val value = m.groupValues[3].ifEmpty { m.groupValues[4].ifEmpty { m.groupValues[5] } }
            map[name] = decodeEntities(value)
        }
        return map
    }

    private fun decodeEntities(input: String): String {
        if (!input.contains('&')) return input
        var out = input
        namedEntities.forEach { (entity, replacement) -> out = out.replace(entity, replacement) }
        out = out.replace(Regex("&#(\\d+);")) { mr ->
            mr.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: mr.value
        }
        out = out.replace(Regex("&#x([0-9a-fA-F]+);")) { mr ->
            mr.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: mr.value
        }
        return out
    }

    private val namedEntities = mapOf(
        "&nbsp;" to " ",
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&#39;" to "'",
    )
}
