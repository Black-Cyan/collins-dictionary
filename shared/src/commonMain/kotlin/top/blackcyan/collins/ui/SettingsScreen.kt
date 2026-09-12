package top.blackcyan.collins.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ChevronsUpDown
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Save
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.components.Toolbar
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import top.blackcyan.collins.about.ProjectInfo
import top.blackcyan.collins.domain.DictionarySummary
import top.blackcyan.collins.openUrl
import top.blackcyan.collins.state.SettingsUiState

@Composable
fun SettingsScreen(
    settingsState: SettingsUiState,
    dictionaries: List<DictionarySummary> = emptyList(),
    selectedDictionaryCode: String? = null,
    onDictionarySelect: (String) -> Unit = {},
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showApiKey by remember { mutableStateOf(false) }

    // The field is local state; settingsState.apiKey holds the last SAVED key,
    // so the diff reliably reflects unsaved edits (pushing every keystroke into
    // the ViewModel made the save button disappear permanently).
    val apiKeyState = rememberTextFieldState(initialText = settingsState.apiKey)

    // External updates to the saved key (clear, or a save performed elsewhere)
    // are mirrored into the field.
    LaunchedEffect(settingsState.apiKey) {
        if (apiKeyState.text.toString() != settingsState.apiKey) {
            apiKeyState.edit {
                replace(0, apiKeyState.text.length, settingsState.apiKey)
            }
        }
    }

    val editedKey = apiKeyState.text.toString()
    val hasUnsavedChanges = editedKey != settingsState.apiKey
    val canSave = editedKey.isNotBlank() && hasUnsavedChanges

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Top bar
        Toolbar(
            modifier = Modifier.fillMaxWidth(),
            title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            leading = {
                IconButton(
                    onClick = onBack,
                    style = ButtonStyle.Ghost,
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // --- Default Dictionary Section ---
            SettingsSection(title = "Default Dictionary") {
                if (dictionaries.isNotEmpty()) {
                    DictionarySetting(
                        dictionaries = dictionaries,
                        selectedDictionaryCode = selectedDictionaryCode,
                        onDictionarySelect = onDictionarySelect,
                    )
                } else {
                    Text(
                        text = "Loading dictionaries...",
                        fontSize = 14.sp,
                        color = Theme[colors][mutedColor],
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            // --- API Configuration Section ---
            SettingsSection(title = "API Configuration") {
                // Status indicator
                ApiKeyStatus(isConfigured = settingsState.isApiKeyConfigured)

                // API Key input
                SettingRow(label = "API Key") {
                    TextField(
                        state = apiKeyState,
                        placeholder = { Text("Enter your Collins API key") },
                        leading = {
                            Icon(
                                imageVector = Lucide.KeyRound,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Theme[colors][mutedColor],
                            )
                        },
                        trailing = {
                            IconButton(
                                onClick = { showApiKey = !showApiKey },
                                style = ButtonStyle.Ghost,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = if (showApiKey) Lucide.EyeOff else Lucide.Eye,
                                    contentDescription = if (showApiKey) "Hide" else "Show",
                                    modifier = Modifier.size(16.dp),
                                    tint = Theme[colors][mutedColor],
                                )
                            }
                        },
                        outputTransformation = if (showApiKey) null else PasswordDots(),
                        modifier = Modifier.fillMaxWidth(),
                        accessibilityLabel = "Collins API Key",
                    )
                }

                // Save — visible whenever the key differs from the saved one.
                Button(
                    onClick = { onSaveApiKey(editedKey.trim()) },
                    style = ButtonStyle.Primary,
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Lucide.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save API Key")
                }

                // Remove API Key — only when the saved key is active.
                AnimatedVisibility(
                    visible = settingsState.isApiKeyConfigured && !hasUnsavedChanges,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Button(
                        onClick = onClearApiKey,
                        style = ButtonStyle.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Remove API Key", color = Theme[colors][destructiveColor])
                    }
                }

                // Messages
                AnimatedVisibility(
                    visible = settingsState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    MessageBanner(
                        message = settingsState.errorMessage ?: "",
                        isError = true,
                    )
                }

                AnimatedVisibility(
                    visible = settingsState.successMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    MessageBanner(
                        message = settingsState.successMessage ?: "",
                        isError = false,
                    )
                }
            }

            // --- Help Section ---
            SettingsSection(title = "How to get an API Key") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HelpStep(
                        number = "1",
                        text = "Visit collinsdictionary.com/api",
                        onClick = { openUrl(ProjectInfo.API_SIGNUP_URL) },
                    )
                    HelpStep(
                        number = "2",
                        text = "Fill in and submit the API access request form",
                    )
                    HelpStep(
                        number = "3",
                        text = "Wait for the confirmation email replying to your request",
                    )
                    HelpStep(
                        number = "4",
                        text = "Use the API key included in the email reply",
                    )
                }
            }

            // --- About ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme[colors][panelColor])
                    .clickable(onClick = onAboutClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Lucide.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Theme[colors][mutedColor],
                )
                Text(
                    "About",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Theme[colors][mutedColor],
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** OutputTransformation that replaces each character with a bullet dot. */
@Composable
private fun PasswordDots(): OutputTransformation {
    return remember {
        object : OutputTransformation {
            override fun TextFieldBuffer.transformOutput() {
                val original = toString()
                replace(0, original.length, "•".repeat(original.length))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Theme[colors][mutedColor],
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Theme[colors][panelColor])
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Theme[colors][mutedColor],
            modifier = Modifier.padding(bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun MessageBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                (if (isError) Theme[colors][destructiveColor] else Theme[colors][primaryColor])
                    .copy(alpha = 0.08f),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isError) Lucide.CircleAlert else Lucide.CircleCheck,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isError) Theme[colors][destructiveColor] else Theme[colors][primaryColor],
        )
        Text(
            text = message,
            color = if (isError) Theme[colors][destructiveColor] else Theme[colors][primaryColor],
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ApiKeyStatus(
    isConfigured: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isConfigured) Theme[colors][primaryColor].copy(alpha = 0.08f)
                else Theme[colors][destructiveColor].copy(alpha = 0.08f),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isConfigured) Lucide.CircleCheck else Lucide.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isConfigured) Theme[colors][primaryColor] else Theme[colors][destructiveColor],
        )
        Text(
            text = if (isConfigured) "API Key is configured" else "API Key is not configured",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isConfigured) Theme[colors][primaryColor] else Theme[colors][destructiveColor],
        )
    }
}

@Composable
private fun DictionarySetting(
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
                style = ButtonStyle.Ghost,
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Default Dictionary",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = selectedDictionary?.name ?: "Not selected",
                            fontSize = 12.sp,
                            color = Theme[colors][mutedColor],
                        )
                    }
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
                        leading = {
                            if (dictionary.code == selectedDictionaryCode) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
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
private fun HelpStep(
    number: String,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Theme[colors][primaryColor].copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme[colors][primaryColor],
            )
        }
        if (onClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClick)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = Theme[colors][primaryColor],
                )
                Icon(
                    imageVector = Lucide.ExternalLink,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Theme[colors][primaryColor],
                )
            }
        } else {
            Text(
                text = text,
                fontSize = 14.sp,
                color = Theme[colors][mutedColor],
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
