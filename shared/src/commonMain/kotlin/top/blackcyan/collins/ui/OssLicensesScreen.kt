package top.blackcyan.collins.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scale
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.Toolbar
import com.composables.ui.components.ButtonStyle
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import top.blackcyan.collins.about.OssLibrary
import top.blackcyan.collins.about.ossLibraries
import top.blackcyan.collins.openUrl

@Composable
fun OssLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Toolbar(
            modifier = Modifier.fillMaxWidth(),
            title = { Text("Open-source licenses", fontWeight = FontWeight.SemiBold) },
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ossLibraries) { library ->
                LicenseRow(library)
            }
        }
    }
}

@Composable
private fun LicenseRow(library: OssLibrary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Theme[colors][panelColor])
            .clickable { openUrl(library.projectUrl) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.Scale,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Theme[colors][mutedColor],
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(library.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            // The license tag has its own, more specific click target.
            Text(
                library.licenseName,
                fontSize = 12.sp,
                color = Theme[colors][primaryColor],
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { openUrl(library.licenseUrl) }
                    .padding(top = 4.dp),
            )
        }
    }
}
