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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.ScrollText
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.Toolbar
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.painterResource
import top.blackcyan.collins.AppVersion
import top.blackcyan.collins.about.ProjectInfo
import top.blackcyan.collins.about.contributors
import top.blackcyan.collins.domain.Contributor
import top.blackcyan.collins.shared.generated.resources.Res
import top.blackcyan.collins.shared.generated.resources.collins
import top.blackcyan.collins.openUrl

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Toolbar(
            modifier = Modifier.fillMaxWidth(),
            title = { Text("About", fontWeight = FontWeight.SemiBold) },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Identity ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Theme[colors][panelColor]),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.collins),
                        contentDescription = "${AppVersion.NAME} icon",
                        modifier = Modifier.size(72.dp),
                    )
                }
                Text(AppVersion.NAME, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Version ${AppVersion.VERSION}",
                    fontSize = 13.sp,
                    color = Theme[colors][mutedColor],
                )
            }

            // ── Links ──────────────────────────────────────────────────────
            AboutSection(title = "Links") {
                AboutLinkRow(
                    icon = Lucide.Github,
                    title = "Repository",
                    subtitle = "github.com/${ProjectInfo.REPOSITORY_OWNER}/${ProjectInfo.REPOSITORY_NAME}",
                    onClick = { openUrl(ProjectInfo.REPOSITORY_URL) },
                )
                AboutLinkRow(
                    icon = Lucide.Scale,
                    title = "License",
                    subtitle = "GPL-3.0-or-later",
                    onClick = { openUrl(ProjectInfo.LICENSE_URL) },
                )
                AboutLinkRow(
                    icon = Lucide.ScrollText,
                    title = "Open-source licenses",
                    onClick = onOpenOssLicenses,
                )
            }

            // ── Contributors ───────────────────────────────────────────────
            Text(
                text = "Contributors",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme[colors][mutedColor],
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ContributorsGrid()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContributorsGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            contributors.forEach { contributor ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Theme[colors][mutedColor].copy(alpha = 0.15f))
                        .clickable { openUrl(contributor.profileUrl) },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = contributor.avatarUrl,
                        contentDescription = "${contributor.login}'s GitHub profile",
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AboutLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Theme[colors][mutedColor],
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Theme[colors][mutedColor])
            }
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Theme[colors][mutedColor],
        )
    }
}
