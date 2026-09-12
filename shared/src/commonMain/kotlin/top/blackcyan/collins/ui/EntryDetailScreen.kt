package top.blackcyan.collins.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon
import com.composables.ui.components.IndeterminateProgressIndicator
import com.composables.ui.components.Text
import com.composables.ui.components.Toolbar
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Square
import com.composeunstyled.theme.Theme
import kotlinx.coroutines.launch
import top.blackcyan.collins.audio.AudioPlayer
import top.blackcyan.collins.audio.createAudioPlayer
import top.blackcyan.collins.domain.EntrySection
import top.blackcyan.collins.domain.InlineNode
import top.blackcyan.collins.domain.InlineSpanStyle
import top.blackcyan.collins.domain.InlineText
import top.blackcyan.collins.domain.RichText
import top.blackcyan.collins.domain.plainText
import top.blackcyan.collins.state.CollinsEntryUiState
import top.blackcyan.collins.state.EntrySense
import top.blackcyan.collins.state.SensePronunciation

@Composable
fun EntryDetailScreen(
    entryState: CollinsEntryUiState,
    audioPlayer: AudioPlayer,
    onBack: () -> Unit,
    onNearbyEntryClick: (String) -> Unit,
    onEntryReferenceClick: (String) -> Unit,
    onTargetEntryConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stop playback when the screen is dismissed.
    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.stop() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Toolbar(
            modifier = Modifier.fillMaxWidth(),
            title = { Text(entryState.headword.ifBlank { "Entry Detail" }, fontWeight = FontWeight.SemiBold) },
            leading = {
                IconButton(onClick = onBack, style = ButtonStyle.Ghost) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                entryState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        IndeterminateProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                        Text(
                            text = "Loading entry…",
                            fontSize = 14.sp,
                            color = Theme[colors][mutedColor],
                        )
                    }
                }

                entryState.errorMessage != null -> {
                    ErrorState(message = entryState.errorMessage, modifier = Modifier.align(Alignment.Center))
                }

                entryState.senses.isNotEmpty() -> {
                    HeadwordContent(
                        entryState = entryState,
                        audioPlayer = audioPlayer,
                        onNearbyEntryClick = onNearbyEntryClick,
                        onEntryReferenceClick = onEntryReferenceClick,
                        onTargetEntryConsumed = onTargetEntryConsumed,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main content
// ---------------------------------------------------------------------------

@Composable
private fun HeadwordContent(
    entryState: CollinsEntryUiState,
    audioPlayer: AudioPlayer,
    onNearbyEntryClick: (String) -> Unit,
    onEntryReferenceClick: (String) -> Unit,
    onTargetEntryConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // entryId -> vertical pixel offset inside the scrolling content.
    val entryOffsets = remember { mutableMapOf<String, Int>() }
    // Parent (scrollable Column) coordinates, used to compute child offsets.
    val parentCoords = remember { mutableStateOf<LayoutCoordinates?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .onGloballyPositioned { parentCoords.value = it }
            .padding(16.dp),
    ) {
        // Entry number chips — only when two or more homographs are loaded.
        if (entryState.senses.size > 1) {
            EntryJumpChips(
                count = entryState.senses.size,
                onJump = { index ->
                    val target = entryState.senses.getOrNull(index) ?: return@EntryJumpChips
                    val offsetPx = entryOffsets[target.detail.entryId] ?: return@EntryJumpChips
                    scope.launch {
                        val gap = with(density) { 16.dp.roundToPx() }
                        scrollState.animateScrollTo((offsetPx - gap).coerceAtLeast(0))
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        entryState.senses.forEachIndexed { index, sense ->
            EntryBlock(
                sense = sense,
                audioPlayer = audioPlayer,
                onEntryReferenceClick = onEntryReferenceClick,
                parentCoords = parentCoords,
                onMeasured = { offset -> entryOffsets[sense.detail.entryId] = offset },
            )
            if (index < entryState.senses.lastIndex) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalSeparator()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Nearby — thin muted row.
        val nearby = entryState.nearbyEntries
        if (nearby != null &&
            (nearby.nearbyPrecedingEntries.isNotEmpty() || nearby.nearbyFollowingEntries.isNotEmpty())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalSeparator()
            Spacer(modifier = Modifier.height(12.dp))

            val preceding = nearby.nearbyPrecedingEntries.lastOrNull()
            val following = nearby.nearbyFollowingEntries.firstOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (preceding != null) {
                    Text(
                        text = "← ${preceding.label}",
                        fontSize = 13.sp,
                        color = Theme[colors][mutedColor],
                        modifier = Modifier.clickable { onNearbyEntryClick(preceding.id) },
                    )
                } else {
                    Spacer(Modifier)
                }
                if (following != null) {
                    Text(
                        text = "${following.label} →",
                        fontSize = 13.sp,
                        color = Theme[colors][mutedColor],
                        modifier = Modifier.clickable { onNearbyEntryClick(following.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Auto-scroll to the target entry once.
    val targetId = entryState.targetEntryId
    if (targetId != null) {
        LaunchedEffect(targetId) {
            val offsetPx = entryOffsets[targetId]
            if (offsetPx != null) {
                val gap = with(density) { 16.dp.roundToPx() }
                scrollState.animateScrollTo((offsetPx - gap).coerceAtLeast(0))
                onTargetEntryConsumed()
            }
            // If not yet measured, the onMeasured lambda records the offset; on
            // the next recomposition the target will resolve. For simplicity we
            // do not retry indefinitely; a second tap on the link is the fallback.
        }
    }
}

// ---------------------------------------------------------------------------
// Entry number chips
// ---------------------------------------------------------------------------

@Composable
private fun EntryJumpChips(count: Int, onJump: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            Button(
                onClick = { onJump(index) },
                style = ButtonStyle.Outlined,
                buttonSize = ButtonSize.Small,
            ) {
                Text((index + 1).toString())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// One homograph entry block
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryBlock(
    sense: EntrySense,
    audioPlayer: AudioPlayer,
    onEntryReferenceClick: (String) -> Unit,
    parentCoords: State<LayoutCoordinates?>,
    onMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                parentCoords.value?.let { parent ->
                    onMeasured(parent.localPositionOf(coords, Offset.Zero).y.toInt())
                }
            },
    ) {
        val parsed = sense.detail.parsedEntry

        // Pronunciations — IPA + play/stop.
        val pronunciations = sense.pronunciations
        if (pronunciations.isNotEmpty()) {
            pronunciations.forEach { pron ->
                PronunciationRow(
                    pron = pron,
                    audioPlayer = audioPlayer,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val sections = parsed?.sections
        if (sections != null) {
            sections.forEachIndexed { idx, section ->
                if (idx > 0) Spacer(modifier = Modifier.height(12.dp))
                SectionBlock(
                    section = section,
                    onEntryReferenceClick = onEntryReferenceClick,
                )
            }
        } else {
            // Unrecognized HTML: plain-text fallback, preserving the existing
            // regex-stripped content until the parser catches up.
            val stripped = sense.detail.entryContent.replace(Regex("<[^>]*>"), "").trim()
            if (stripped.isNotBlank()) {
                Text(stripped, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }

            // Etymology
        if (parsed?.etymology != null) {
            Spacer(modifier = Modifier.height(12.dp))
            val linkColor = Theme[colors][primaryColor]
            AnnotatedClickableText(
                text = buildAnnotatedFromRichText(
                    parsed.etymology,
                    defaultStyle = SpanStyle(
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Theme[colors][mutedColor],
                    ),
                    linkColor = linkColor,
                ),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                onEntryReferenceClick = onEntryReferenceClick,
            )
        }

        // Derived terms
        if (parsed?.derivedTerms != null && parsed.derivedTerms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalSeparator()
            Spacer(modifier = Modifier.height(8.dp))
            parsed.derivedTerms.forEach { term ->
                val rowText = buildAnnotatedString {
                    appendInlineParts(term.heading)
                    if (term.partOfSpeech != null) {
                        append("  ")
                        withStyle(SpanStyle(color = Theme[colors][mutedColor], fontSize = 12.sp)) {
                            append(term.partOfSpeech!!)
                        }
                    }
                }
                Text(rowText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }

        // Topics (when only available from the raw DTO and no parsed entry)
        if (parsed == null && sense.detail.topics.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sense.detail.topics.forEach { topic ->
                    Text(
                        text = topic.label,
                        fontSize = 11.sp,
                        color = Theme[colors][mutedColor],
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pronunciation row
// ---------------------------------------------------------------------------

@Composable
private fun PronunciationRow(
    pron: SensePronunciation,
    audioPlayer: AudioPlayer,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!pron.ipa.isNullOrBlank()) {
            Text(
                text = "/${pron.ipa}/",
                fontSize = 15.sp,
                color = Theme[colors][mutedColor],
            )
        }

        val url = pron.audioUrl
        if (url != null) {
            val isPlaying = audioPlayer.activeUrl.collectAsState().value == url
            Button(
                onClick = { audioPlayer.playOrToggle(url) },
                style = ButtonStyle.Outlined,
                buttonSize = ButtonSize.Small,
            ) {
                if (isPlaying) {
                    Icon(Lucide.Square, contentDescription = "Stop", modifier = Modifier.size(12.dp))
                } else {
                    Icon(Lucide.Play, contentDescription = "Play", modifier = Modifier.size(12.dp))
                }
                if (!pron.label.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(pron.label!!.uppercase())
                }
            }
        } else if (!pron.label.isNullOrBlank()) {
            // No audio available; just show the label.
            Text(
                text = pron.label!!.uppercase(),
                fontSize = 13.sp,
                color = Theme[colors][mutedColor],
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section block (POS / inflections / grammar / senses)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionBlock(
    section: EntrySection,
    onEntryReferenceClick: (String) -> Unit,
) {
    when (section) {
        is EntrySection.Classified -> {
            // POS chip + inflections on one row.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (section.partOfSpeech != null) {
                    SurfaceChip(label = section.partOfSpeech!!)
                }
                if (section.inflections.isNotEmpty()) {
                    Text(
                        text = section.inflections.joinToString(", "),
                        fontSize = 13.sp,
                        color = Theme[colors][mutedColor],
                    )
                }
            }

            // Section-level grammar or labels
            if (section.grammar != null || section.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val fragments = buildList {
                    section.labels.forEach { add(it) }
                    if (section.grammar != null) add(section.grammar!!.plainText())
                }
                BasicText(
                    text = fragments.joinToString(" · "),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Theme[colors][mutedColor],
                    ),
                )
            }

            // Senses
            if (section.senses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SenseNodes(
                    senses = section.senses,
                    onEntryReferenceClick = onEntryReferenceClick,
                )
            }
        }

        is EntrySection.Unparsed -> {
            Text(
                text = section.text,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sense nodes (recursive)
// ---------------------------------------------------------------------------

@Composable
private fun SenseNodes(
    senses: List<top.blackcyan.collins.domain.SenseNode>,
    onEntryReferenceClick: (String) -> Unit,
    indentDp: Int = 0,
) {
    Column(modifier = if (indentDp > 0) Modifier.padding(start = indentDp.dp) else Modifier) {
        senses.forEachIndexed { idx, sense ->
            SenseRow(
                sense = sense,
                index = idx,
                onEntryReferenceClick = onEntryReferenceClick,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SenseRow(
    sense: top.blackcyan.collins.domain.SenseNode,
    index: Int,
    onEntryReferenceClick: (String) -> Unit,
) {
    val numText = sense.number ?: ""
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        // Number, labels, definition on one row.
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Right-aligned number column.
            if (numText.isNotBlank()) {
                Text(
                    text = numText,
                    fontSize = 15.sp,
                    color = Theme[colors][mutedColor],
                    modifier = Modifier.width(28.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Labels as outlined chips.
                if (sense.labels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        sense.labels.forEach { label ->
                            SurfaceChip(label = label, small = true)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Grammar prefix + definition.
                if (sense.grammar != null || sense.definition != null) {
                    val linkColor = Theme[colors][primaryColor]
                    val annotated = buildAnnotatedString {
                        if (sense.grammar != null) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Theme[colors][mutedColor])) {
                                append(sense.grammar!!.plainText())
                                append(" ")
                            }
                        }
                        if (sense.definition != null) {
                            appendInlineParts(sense.definition!!, onEntryReferenceClick, linkColor)
                        }
                    }
                    AnnotatedClickableText(
                        text = annotated,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        onEntryReferenceClick = onEntryReferenceClick,
                    )
                }
            }
        }

        // Examples — indented under the definition, prefixed with ⇒.
        sense.examples.forEach { example ->
            Row(modifier = Modifier.padding(start = if (numText.isNotBlank()) 36.dp else 0.dp)) {
                val linkColor = Theme[colors][primaryColor]
                AnnotatedClickableText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Theme[colors][mutedColor])) {
                            append("⇒ ")
                        }
                        appendInlineParts(example, onEntryReferenceClick, linkColor)
                    },
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = Theme[colors][mutedColor],
                        lineHeight = 20.sp,
                    ),
                    onEntryReferenceClick = onEntryReferenceClick,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Nested children (a./b./c.).
        if (sense.children.isNotEmpty()) {
            SenseNodes(
                senses = sense.children,
                onEntryReferenceClick = onEntryReferenceClick,
                indentDp = 20,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// RichText → AnnotatedString
// ---------------------------------------------------------------------------

private fun AnnotatedString.Builder.appendInlineParts(
    parts: RichText,
    onEntryReferenceClick: (String) -> Unit = {},
    linkColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    parts.forEach { node ->
        when (node) {
            is InlineNode.Text -> appendStyled(node.inline)
            is InlineNode.Link -> {
                if (node.parts.isEmpty()) return@forEach
                if (node.targetEntryId != null) {
                    pushStringAnnotation(tag = "entryRef", annotation = node.targetEntryId!!)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        node.parts.forEach { appendStyled(it) }
                    }
                    pop()
                } else {
                    node.parts.forEach { appendStyled(it) }
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyled(text: InlineText) {
    val style = SpanStyle(
        fontWeight = when (text.style) {
            InlineSpanStyle.BOLD -> FontWeight.Bold
            else -> null
        },
        fontStyle = when (text.style) {
            InlineSpanStyle.ITALIC -> FontStyle.Italic
            else -> null
        },
    ).let { base ->
        if (text.superscript) {
            base.copy(
                fontSize = 10.sp,
                baselineShift = BaselineShift.Superscript,
            )
        } else {
            base
        }
    }
    withStyle(style) { append(text.text) }
}

/** Builds an [AnnotatedString] from [RichText] with a custom [defaultStyle]. */
@Composable
private fun buildAnnotatedFromRichText(
    parts: RichText,
    defaultStyle: SpanStyle,
    linkColor: androidx.compose.ui.graphics.Color = Theme[colors][primaryColor],
) = buildAnnotatedString {
    withStyle(defaultStyle) { appendInlineParts(parts, linkColor = linkColor) }
}

// ---------------------------------------------------------------------------
// Small surface chip (outlined, rounded)
// ---------------------------------------------------------------------------

@Composable
private fun SurfaceChip(label: String, small: Boolean = false) {
    val fontSize = if (small) 11.sp else 13.sp
    val padH = if (small) 6.dp else 8.dp
    val padV = if (small) 2.dp else 4.dp
    Text(
        text = label,
        fontSize = fontSize,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Theme[colors][primaryColor].copy(alpha = 0.08f))
            .padding(horizontal = padH, vertical = padV),
    )
}

// ---------------------------------------------------------------------------
// Icon button wrapper (Ghost)
// ---------------------------------------------------------------------------

@Composable
private fun IconButton(
    onClick: () -> Unit,
    style: ButtonStyle = ButtonStyle.Ghost,
    content: @Composable () -> Unit,
) {
    Button(onClick = onClick, style = style) { content() }
}

// ---------------------------------------------------------------------------
// Clickable annotated text — wraps ClickableText to handle entryRef
// annotation taps. Call for any RichText that may contain cross-ref links.
// ---------------------------------------------------------------------------

@Composable
private fun AnnotatedClickableText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    onEntryReferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableText(
        text = text,
        style = style,
        onClick = { offset ->
            text.getStringAnnotations(tag = "entryRef", start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation -> onEntryReferenceClick(annotation.item) }
        },
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// Error state
// ---------------------------------------------------------------------------

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Theme[colors][destructiveColor].copy(alpha = 0.08f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Theme[colors][destructiveColor],
        )
        Text(
            text = message,
            color = Theme[colors][destructiveColor],
            fontSize = 14.sp,
        )
    }
}
