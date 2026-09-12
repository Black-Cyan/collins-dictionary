package top.blackcyan.collins.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ChevronsUpDown
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.X
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.Theme
import top.blackcyan.collins.domain.DictionarySummary
import top.blackcyan.collins.domain.Headword
import top.blackcyan.collins.state.CollinsSearchUiState

@Composable
fun SearchHomeScreen(
    dictionaries: List<DictionarySummary>,
    selectedDictionaryCode: String?,
    dictionariesError: String?,
    searchState: CollinsSearchUiState,
    onDictionarySelect: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onHeadwordClick: (Headword) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val queryState = rememberTextFieldState(initialText = searchState.query)
    val hasResults = searchState.headwords != null || searchState.emptyMessage != null || searchState.errorMessage != null
    val iconAlpha by animateFloatAsState(
        targetValue = if (hasResults) 0f else 1f,
        animationSpec = tween(300),
        label = "iconAlpha",
    )

    // Sync text changes back to ViewModel
    LaunchedEffect(queryState) {
        snapshotFlow { queryState.text.toString() }.collect { text ->
            onQueryChange(text)
        }
    }

    // Sync external query changes to TextFieldState
    LaunchedEffect(searchState.query) {
        if (queryState.text.toString() != searchState.query) {
            queryState.edit {
                replace(0, queryState.text.length, searchState.query)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar with dictionary picker and settings
        TopBar(
            dictionaries = dictionaries,
            selectedDictionaryCode = selectedDictionaryCode,
            onDictionarySelect = onDictionarySelect,
            onSettingsClick = onSettingsClick,
        )

        // Dictionaries error banner
        if (dictionariesError != null) {
            DictionariesErrorBanner(
                message = dictionariesError,
                onSettingsClick = onSettingsClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Main content area
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (hasResults) Alignment.TopCenter else Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .then(if (hasResults) Modifier.fillMaxSize() else Modifier)
                    .padding(horizontal = 24.dp),
            ) {
                // Logo and tagline - fades out when results are shown
                AnimatedVisibility(
                    visible = !hasResults && !searchState.isLoading,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(200)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 40.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.BookOpen,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .alpha(iconAlpha),
                            tint = Theme[colors][mutedColor],
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Look up any word",
                            fontSize = 16.sp,
                            color = Theme[colors][mutedColor],
                        )
                    }
                }

                // Search bar - always visible, centered when no results
                SearchBar(
                    queryState = queryState,
                    isLoading = searchState.isLoading,
                    onSearch = { onSearch(searchState.query) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search button
                Button(
                    onClick = { onSearch(searchState.query) },
                    enabled = searchState.query.isNotBlank() && !searchState.isLoading && selectedDictionaryCode != null,
                    style = ButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    buttonSize = ButtonSize.Large,
                ) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Search")
                }

                // Results area
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        searchState.isLoading -> {
                            LoadingState(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        searchState.errorMessage != null -> {
                            ErrorState(
                                message = searchState.errorMessage,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 32.dp),
                            )
                        }
                        searchState.emptyMessage != null -> {
                            EmptyState(
                                message = searchState.emptyMessage,
                                suggestions = searchState.suggestions,
                                onSuggestionClick = { onSearch(it) },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 32.dp),
                            )
                        }
                        searchState.headwords != null -> {
                            SearchResults(
                                headwords = searchState.headwords,
                                onHeadwordClick = onHeadwordClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    dictionaries: List<DictionarySummary>,
    selectedDictionaryCode: String?,
    onDictionarySelect: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dictionary picker
        DictionaryPicker(
            dictionaries = dictionaries,
            selectedDictionaryCode = selectedDictionaryCode,
            onDictionarySelect = onDictionarySelect,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Settings button
        IconButton(
            onClick = onSettingsClick,
            style = ButtonStyle.Ghost,
        ) {
            Icon(
                imageVector = Lucide.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DictionaryPicker(
    dictionaries: List<DictionarySummary>,
    selectedDictionaryCode: String?,
    onDictionarySelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDictionary = dictionaries.find { it.code == selectedDictionaryCode }

    DropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        alignment = DropdownMenuAlignment.Start,
        modifier = modifier,
        anchor = {
            Button(
                onClick = { expanded = !expanded },
                style = ButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Lucide.BookOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = selectedDictionary?.name ?: "Select dictionary",
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Icon(
                        imageVector = Lucide.ChevronsUpDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Theme[colors][mutedColor],
                    )
                }
            }
        },
        panel = {
            DropdownMenuPanel {
                dictionaries.forEach { dictionary ->
                    DropdownMenuItem(
                        onClick = {
                            onDictionarySelect(dictionary.code)
                            expanded = false
                        },
                    ) {
                        Text(dictionary.name)
                    }
                }
            }
        },
    )
}

@Composable
private fun SearchBar(
    queryState: androidx.compose.foundation.text.input.TextFieldState,
    isLoading: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    TextField(
        state = queryState,
        modifier = modifier,
        accessibilityLabel = "Search",
        placeholder = { Text("Type a word to search...") },
        leading = {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Theme[colors][mutedColor],
            )
        },
        trailing = {
            AnimatedVisibility(
                visible = queryState.text.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    onClick = { queryState.clearText() },
                    modifier = Modifier.size(32.dp),
                    style = ButtonStyle.Ghost,
                    buttonSize = ButtonSize.Small,
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(14.dp),
                        tint = Theme[colors][mutedColor],
                    )
                }
            }
        },
        interactionSource = interactionSource,
    )
}

@Composable
private fun SearchResults(
    headwords: List<Headword>,
    onHeadwordClick: (Headword) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(headwords) { index, headword ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme[colors][panelColor])
                    .clickable { onHeadwordClick(headword) }
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = headword.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Theme[colors][mutedColor],
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        com.composables.ui.components.IndeterminateProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.5f),
        )
        Text(
            text = "Searching...",
            fontSize = 14.sp,
            color = Theme[colors][mutedColor],
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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

@Composable
private fun EmptyState(
    message: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            fontSize = 15.sp,
            color = Theme[colors][mutedColor],
        )
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Did you mean:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            suggestions.forEach { suggestion ->
                Button(
                    onClick = { onSuggestionClick(suggestion) },
                    style = ButtonStyle.Ghost,
                ) {
                    Text(text = suggestion)
                }
            }
        }
    }
}

@Composable
private fun DictionariesErrorBanner(
    message: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Theme[colors][destructiveColor].copy(alpha = 0.08f))
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Lucide.CircleAlert,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Theme[colors][destructiveColor],
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cannot load dictionaries",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Theme[colors][destructiveColor],
                )
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = Theme[colors][destructiveColor].copy(alpha = 0.7f),
                )
            }
            Button(
                onClick = onSettingsClick,
                style = ButtonStyle.Ghost,
                buttonSize = ButtonSize.Small,
            ) {
                Text("Settings")
            }
        }
    }
}
