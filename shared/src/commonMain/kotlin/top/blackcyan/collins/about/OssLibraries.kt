package top.blackcyan.collins.about

/**
 * Direct third-party dependencies shipped with the app, with the license the
 * user can open in a browser. The list is maintained manually; bump versions
 * here when dependencies are upgraded.
 */
data class OssLibrary(
    val name: String,
    val projectUrl: String,
    val licenseName: String,
    val licenseUrl: String,
)

val ossLibraries: List<OssLibrary> = listOf(
    OssLibrary(
        name = "Compose Multiplatform",
        projectUrl = "https://github.com/JetBrains/compose-multiplatform",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "Skiko",
        projectUrl = "https://github.com/JetBrains/skiko",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "Kotlin Standard Library",
        projectUrl = "https://kotlinlang.org/",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "kotlinx.coroutines",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "kotlinx.serialization",
        projectUrl = "https://github.com/Kotlin/kotlinx.serialization",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "Ktor",
        projectUrl = "https://github.com/ktorio/ktor",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "OkHttp",
        projectUrl = "https://square.github.io/okhttp/",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "Okio",
        projectUrl = "https://github.com/square/okio",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "Coil",
        projectUrl = "https://github.com/coil-kt/coil",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "AndroidX Lifecycle",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "multiplatform-settings",
        projectUrl = "https://github.com/russhwolf/multiplatform-settings",
        licenseName = "Apache-2.0",
        licenseUrl = "https://opensource.org/licenses/Apache-2.0",
    ),
    OssLibrary(
        name = "Composables UI",
        projectUrl = "https://github.com/composablehorizons/ui",
        licenseName = "MIT",
        licenseUrl = "https://opensource.org/licenses/MIT",
    ),
    OssLibrary(
        name = "Compose Icons (Lucide)",
        projectUrl = "https://github.com/composablehorizons/composeicons",
        licenseName = "MIT",
        licenseUrl = "https://opensource.org/licenses/MIT",
    ),
    // LGPL dependency: its source is available from the public repository and
    // the unmodified library can be replaced by the user.
    OssLibrary(
        name = "JLayer",
        projectUrl = "http://www.javazoom.net/javalayer/javalayer.html",
        licenseName = "LGPL-2.1-or-later",
        licenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html",
    ),
)
