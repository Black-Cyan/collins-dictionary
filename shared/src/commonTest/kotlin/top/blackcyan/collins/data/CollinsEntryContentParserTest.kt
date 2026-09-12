package top.blackcyan.collins.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.blackcyan.collins.domain.EntrySection
import top.blackcyan.collins.domain.InlineNode
import top.blackcyan.collins.domain.plainText

class CollinsEntryContentParserTest {

    @Test
    fun applicationParsesNumberedSensesQuotesLabelsAndCrossReferences() {
        val parsed = CollinsEntryContentParser.parse(APPLICATION_HTML)!!

        assertEquals("application", parsed.headword)
        assertNull(parsed.homographNumber)
        assertEquals("UK", parsed.variant)

        val audio = parsed.pronunciations.single()
        assertEquals(
            "https://api.collinsdictionary.com/media/sounds/sounds/0/022/02225/02225.mp3",
            audio.audioUrl,
        )
        assertEquals("ˌæplɪˈkeɪʃən", audio.ipa.plainText())
        assertNull(audio.label)

        val section = parsed.sections.single() as EntrySection.Classified
        assertEquals("noun", section.partOfSpeech)
        assertEquals(8, section.senses.size)
        assertEquals(listOf("1.", "2.", "3.", "4.", "5.", "6.", "7.", "8."), section.senses.map { it.number })

        val second = section.senses[1]
        assertEquals("relevance or value", second.definition?.plainText())
        assertEquals("the practical applications of space technology", second.examples.single().plainText())

        val seventh = section.senses[6]
        assertEquals(listOf("logic", "mathematics"), seventh.labels)
        assertEquals("the process of determining the value of a function for a given argument", seventh.definition?.plainText())

        // Sense 8 is an xr cross-reference with two in-app entry links.
        val eighth = section.senses[7]
        val links = eighth.definition?.filterIsInstance<InlineNode.Link>().orEmpty()
        assertEquals(listOf("application-program_1", "applications-package_1"), links.map { it.targetEntryId })
        assertTrue(eighth.definition!!.plainText().startsWith("→ short for"))
    }

    @Test
    fun audioFallbackTextAndSpeakerGifNeverAppearInContent() {
        val parsed = CollinsEntryContentParser.parse(APPLICATION_HTML)!!
        val allText = buildString {
            parsed.sections.forEach { section ->
                append(section.toString())
            }
            append(parsed.pronunciations.toString())
        }
        assertTrue(!allText.contains("Your browser does not support HTML5 audio"))
        assertTrue(!allText.contains("redspeaker"))
    }

    @Test
    fun appelEntryHasPrimaryAndRegionalPronunciationsAndEtymology() {
        val parsed = CollinsEntryContentParser.parse(APPEL_1_HTML)!!

        assertEquals("appel", parsed.headword)
        assertEquals(2, parsed.pronunciations.size)
        assertEquals(null, parsed.pronunciations[0].label)
        assertEquals("əˈpɛl", parsed.pronunciations[0].ipa.plainText())
        assertEquals("French", parsed.pronunciations[1].label)
        assertEquals("apɛl", parsed.pronunciations[1].ipa.plainText())
        assertNull(parsed.pronunciations[1].audioUrl)

        val section = parsed.sections.single() as EntrySection.Classified
        assertEquals("noun", section.partOfSpeech)
        assertEquals(listOf("fencing"), section.labels)
        assertEquals(2, section.senses.size)
        assertEquals("from French: challenge", parsed.etymology?.plainText()?.trim('[', ']'))
    }

    @Test
    fun unnumberedSenseIsKeptWithoutANumber() {
        val parsed = CollinsEntryContentParser.parse(APPEL_2_HTML)!!
        val section = parsed.sections.single() as EntrySection.Classified
        val sense = section.senses.single()
        assertNull(sense.number)
        assertTrue(sense.definition!!.plainText().contains("Karel"))
    }

    @Test
    fun applyParsesInflectionsAndNestedSyntacticGrammar() {
        val parsed = CollinsEntryContentParser.parse(APPLY_HTML)!!
        val verb = parsed.sections.first() as EntrySection.Classified
        assertEquals("verb", verb.partOfSpeech)
        assertEquals(listOf("-plies", "-plying", "-plied"), verb.inflections)
        val fourth = verb.senses.first { it.number == "4." }
        assertEquals("(intransitive; often foll by for)", fourth.grammar?.plainText())

        // Top-level derived term with its own (empty) hom part of speech.
        val derived = parsed.derivedTerms.single()
        assertEquals("noun", derived.partOfSpeech)
        assertTrue(derived.heading.plainText().contains("applier"))
    }

    @Test
    fun takeParsesHomnumSectionGrammarNestedSensesAndIdiomLinks() {
        val parsed = CollinsEntryContentParser.parse(TAKE_FRAGMENTS_HTML)!!

        assertEquals("take", parsed.headword)
        assertEquals("2", parsed.homographNumber)
        assertEquals("UK", parsed.variant)

        val verb = parsed.sections[0] as EntrySection.Classified
        assertEquals("verb", verb.partOfSpeech)
        assertEquals(listOf("takes", "taking", "took", "taken"), verb.inflections)
        assertEquals("(mainly transitive)", verb.grammar?.plainText())

        val eightieth = verb.senses.first { it.number == "80." }
        assertEquals(listOf("cinema", "music"), eightieth.labels)
        assertEquals(listOf("a.", "b.", "c."), eightieth.children.map { it.number })

        val idiom = verb.senses.first { it.number == "65." }
        val link = idiom.definition?.filterIsInstance<InlineNode.Link>()?.single()
        assertEquals("take-amiss_1", link?.targetEntryId)
        assertEquals("take amiss", link?.parts?.let { parts -> parts.joinToString("") { it.text } })
        assertTrue(idiom.definition!!.plainText().startsWith("See"))

        val noun = parsed.sections[1] as EntrySection.Classified
        assertEquals("noun", noun.partOfSpeech)
    }

    @Test
    fun junkHtmlReturnsNullRatherThanThrowing() {
        assertNull(CollinsEntryContentParser.parse("not html at all"))
        assertNull(CollinsEntryContentParser.parse("<div><span>nothing recognizable</span></div>"))
    }

    private companion object {
        val APPLICATION_HTML = """
<div class="entry_container"><div class="entry lang_en-gb" id="application_1"><span class="inline"><h1 class="hwd">application</h1><span> (</span><span class="pron" type="">ˌæplɪˈkeɪʃən<a href="#" class="playback"><img src="https://api.collinsdictionary.com/external/images/redspeaker.gif?version=branch 1.0" alt="Pronunciation for application" class="sound" title="Pronunciation for application" style="cursor: pointer"/></a><audio type="pronunciation" title="application"><source type="audio/mpeg" src="https://api.collinsdictionary.com/media/sounds/sounds/0/022/02225/02225.mp3"/>Your browser does not support HTML5 audio.</audio></span><span>)</span></span><div class="hom" id="application_1.1"><span class="gramGrp"><span>   </span><span class="pos">noun</span></span><div class="sense"><span class="sensenum">     1. </span><span class="def">the act of applying to a particular purpose or use</span></div><div class="sense"><span class="sensenum">     2. </span><span class="def">relevance or value</span><span class="cit" id="application_1.2"><span class="quote"><span> ⇒ </span>the practical applications of space technology</span></span></div><div class="sense"><span class="sensenum">     3. </span><span class="def">the act of asking for something</span><span class="cit" id="application_1.3"><span class="quote"><span> ⇒ </span>an application for leave</span></span></div><div class="sense"><span class="sensenum">     4. </span><span class="def">a verbal or written request, as for a job, etc</span><span class="cit" id="application_1.4"><span class="quote"><span> ⇒ </span>he filed his application</span></span></div><div class="sense"><span class="sensenum">     5. </span><span class="def">diligent effort or concentration</span><span class="cit" id="application_1.5"><span class="quote"><span> ⇒ </span>a job requiring application</span></span></div><div class="sense"><span class="sensenum">     6. </span><span class="def">something, such as a healing agent or lotion, that is applied, esp to the skin</span></div><div class="sense"><span class="sensenum">     7. </span><span class="lbl">logic</span><span class="lbl"><span>, </span>mathematics</span><span> </span><span class="def">the process of determining the value of a function for a given argument</span></div><div class="sense"><span class="sensenum">     8. </span><span class="xr"><span> → </span><span class="lbl">short for</span><span> </span><a data-resource="english" data-topic="application-program_1" href="">application program</a><span class="bold">, </span><a data-resource="english" data-topic="applications-package_1" href="">applications package</a></span></div></div></div></div>
"""

        val APPEL_1_HTML = """
<div class="entry_container"><div class="entry lang_en-gb" id="appel_1"><span class="inline"><h1 class="hwd">appel</h1><span> (</span><span class="pron" type="">əˈpɛl<a href="#" class="playback"><img src="https://api.collinsdictionary.com/external/images/redspeaker.gif" alt="Pronunciation for appel" class="sound"/><audio type="pronunciation" title="appel"><source type="audio/mpeg" src="https://api.collinsdictionary.com/media/sounds/sounds/e/en_/en_gb/en_gb_appel.mp3"/>Your browser does not support HTML5 audio.</audio></span><span class="pron" type=""><span>, </span><span class="lbl">French</span><span> </span>apɛl</span><span>)</span></span><div class="hom" id="appel_1.1"><span class="gramGrp"><span>   </span><span class="pos">noun</span></span><span class="lbl"><span> </span>fencing</span><div class="sense"><span class="sensenum">     1. </span><span class="def">a stamp of the foot, used to warn of one&apos;s intent to attack</span></div><div class="sense"><span class="sensenum">     2. </span><span class="def">a sharp blow with the blade made to procure an opening</span></div></div><div class="etym"><span>   [</span>from French: challenge<span>]</span></div></div></div>
"""

        val APPEL_2_HTML = """
<div class="entry_container"><div class="entry lang_en-gb" id="appel_2"><span class="inline"><h1 class="hwd">Appel</h1><span> (</span><span class="pron" type=""><span class="lbl">Dutch</span><span> </span>ˈɑpəl</span><span>)</span></span><div class="hom" id="appel_2.1"><span class="gramGrp"><span>   </span><span class="pos">noun</span></span><div class="sense"><span>     </span><span> </span><span class="def"><em class="hi">Karel</em> (<span class="pron" type="">ˈkaːrəl</span>). 1921–2006, Dutch abstract expressionist painter</span></div></div></div></div>
"""

        val APPLY_HTML = """
<div class="entry_container"><div class="entry lang_en-gb" id="apply_1"><span class="inline"><h1 class="hwd">apply</h1><span> (</span><span class="pron" type="">əˈplaɪ<a href="#" class="playback"><img src="https://api.collinsdictionary.com/external/images/redspeaker.gif" class="sound"/><audio type="pronunciation" title="apply"><source type="audio/mpeg" src="https://api.collinsdictionary.com/media/sounds/sounds/0/022/02233/02233.mp3"/>Your browser does not support HTML5 audio.</audio></span><span>)</span></span><div class="hom" id="apply_1.1"><span class="gramGrp"><span>   </span><span class="pos">verb</span></span><span class="inline"><span> </span><span class="orth">-plies</span><span>, </span><span class="orth">-plying</span><em class="span">, </em><span class="orth">-plied</span></span><div class="sense"><span class="sensenum">     1. </span><span class="gramGrp"><span class="subc"><span>(</span>transitive<span>)</span></span></span><span> </span><span class="def">to put to practical use; utilize; employ</span></div><div class="sense"><span class="sensenum">     4. </span><span class="gramGrp"><span>(</span><span class="gramGrp"><span class="subc">intransitive</span></span><span class="lbl"><span>; </span>often foll by</span><span> </span><span class="colloc">for</span><span>)</span></span><span> </span><span class="def">to put in an application or request</span></div></div><div class="etym"><span>   [</span>C14: from Old French <em class="hi">aplier,</em> from Latin <em class="hi">applicāre</em> to attach to<span>]</span></div><span class="re" id="apply_1.3"><span> <br/>&gt; </span><span class="inline"><span class="orth">applier</span><span> (</span><span>apˈplier</span><span>)</span></span><div class="hom" id="apply_1.4"><span class="gramGrp"><span> </span><span class="pos">noun</span></span></div></span></div></div>
"""

        val TAKE_FRAGMENTS_HTML = """
<div class="entry_container"><div class="entry lang_en-gb" id="take_1"><span class="inline"><h1 class="hwd">take<span class="homnum">2</span></h1><span> (</span><span class="pron" type="">ˈtɑːkɪ<a href="#" class="playback"><img src="https://api.collinsdictionary.com/external/images/redspeaker.gif" class="sound"/><audio type="pronunciation" title="take2"><source type="audio/mpeg" src="https://api.collinsdictionary.com/media/sounds/sounds/e/en_/en_gb/en_gb_take_1.mp3"/>Your browser does not support HTML5 audio.</audio></span><span>)</span></span><div class="hom" id="take_1.1"><span class="gramGrp"><span>   </span><span class="pos">verb</span></span><div class="formTypeInfl"><span> </span><span class="orth">takes</span><span>, </span><span class="orth">taking</span><span>, </span><span class="orth">took</span><em class="span">, </em><span class="orth">taken</span></div><span class="gramGrp"><span class="subc"><span> </span><span>(</span>mainly transitive<span>)</span></span></span><div class="sense"><span class="sensenum">     65. </span><span class="re"><span class="xr"> See <a data-resource="english" data-topic="take-amiss_1" href="">take amiss</a></span></span></div><div class="sense"><span class="sensenum">     80. </span><span class="lbl">cinema</span><span class="lbl"><span>, </span>music</span><div class="sense"><span class="sensenum">   a. </span><span class="def">one of a series of recordings from which the best will be selected for release</span></div><div class="sense"><span class="sensenum">   b. </span><span class="def">the process of taking one such recording</span></div><div class="sense"><span class="sensenum">   c. </span><span class="def">a scene or part of a scene photographed without interruption</span></div></div></div><div class="hom" id="take_1.53"><span class="gramGrp"><span>   </span><span class="pos">noun</span></span><div class="sense"><span class="sensenum">     77. </span><span class="def">the act of taking</span></div></div><div class="etym"><span> [</span>Old English tacan<span>]</span></div></div></div>
"""
    }
}
