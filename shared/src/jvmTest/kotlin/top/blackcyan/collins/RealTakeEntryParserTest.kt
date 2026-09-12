package top.blackcyan.collins

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.blackcyan.collins.data.CollinsEntryContentParser
import top.blackcyan.collins.domain.EntrySection
import top.blackcyan.collins.domain.InlineNode
import top.blackcyan.collins.domain.plainText

/**
 * Full-size smoke test against the real, device-captured `take_1` payload
 * (24 KB, 89 sense nodes, three parts of speech in one entry).
 */
class RealTakeEntryParserTest {

    private val html by lazy {
        val stream = this::class.java.getResourceAsStream("/fixtures/take_1.html")
            ?: error("fixture /fixtures/take_1.html missing")
        stream.bufferedReader().readText()
    }

    @Test
    fun parsesRealTakeEntry() {
        val parsed = CollinsEntryContentParser.parse(html)!!

        assertEquals("take", parsed.headword)
        assertEquals("1", parsed.homographNumber)
        assertEquals("UK", parsed.variant)
        // The third pos ("adjective") belongs to a trailing `re` derived block,
        // not a top-level hom section.
        assertEquals(
            listOf("verb", "noun"),
            parsed.sections.filterIsInstance<EntrySection.Classified>().map { it.partOfSpeech },
        )
        assertEquals("adjective", parsed.derivedTerms.single().partOfSpeech)

        val verb = parsed.sections.filterIsInstance<EntrySection.Classified>().first()
        assertEquals(listOf("takes", "taking", "took", "taken"), verb.inflections)
        assertEquals("(mainly transitive)", verb.grammar?.plainText())

        // Every numbered verb sense from 1. to 76. is present.
        assertEquals((1..76).map { "$it." }, verb.senses.map { it.number })

        // Numbering continues across part-of-speech sections: nested a./b./c.
        // senses live under noun sense 80.
        val noun = parsed.sections.filterIsInstance<EntrySection.Classified>()[1]
        assertEquals((77..84).map { "$it." }, noun.senses.map { it.number })
        val eighty = noun.senses.first { it.number == "80." }
        assertEquals(listOf("a.", "b.", "c."), eighty.children.map { it.number })
        assertEquals(listOf("cinema", "music"), eighty.labels)

        // Idiom rows carry in-app entry targets.
        val idiomTargets = verb.senses.mapNotNull { sense ->
            sense.definition?.filterIsInstance<InlineNode.Link>()?.map { it.targetEntryId }
        }.flatten()
        assertTrue(idiomTargets.contains("take-amiss_1"))

        // No browser-fallback prose survives anywhere in the parsed model.
        fun top.blackcyan.collins.domain.SenseNode.allText(): String =
            listOfNotNull(grammar?.plainText(), definition?.plainText())
                .plus(examples.map { it.plainText() })
                .joinToString(" ") +
                " " + children.joinToString(" ") { it.allText() }

        val modelText = parsed.sections.filterIsInstance<EntrySection.Classified>()
            .flatMap { section -> section.senses.map { it.allText() } }
            .joinToString(" ") +
            parsed.pronunciations.joinToString(" ") { it.ipa.plainText() }
        assertTrue(!modelText.contains("Your browser does not support HTML5 audio"))
    }
}
