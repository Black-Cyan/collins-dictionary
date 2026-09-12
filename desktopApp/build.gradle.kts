import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// The desktop runtime must be JetBrains Runtime: Compose's WindowDraggableArea
// only uses the native interactive window move (_NET_WM_MOVERESIZE on X11,
// enabling edge-tiling/maximize, drag-restore and WM move animations) on JBR;
// plain OpenJDK falls back to programmatic setLocation() which KWin treats as
// non-interactive. JBR is also what Compose bundles in native distributions.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
}

// The Compose plugin's `run` task hard-codes its executable to
// application.javaHome (which defaults to the JVM running the Gradle daemon)
// and ignores the Java toolchain, so point it at the JBR launcher explicitly.
val jbrLauncher = javaToolchains.launcherFor(java.toolchain)

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.composables.ui)
    implementation(libs.composables.icons.lucide)
}

compose.desktop {
    application {
        mainClass = "top.blackcyan.collins.MainKt"

        // Same JBR for `run` (see jbrLauncher above); packaging itself still
        // downloads/bundles a matching JetBrains Runtime for distribution.
        javaHome = jbrLauncher.get().metadata.installationPath.asFile.absolutePath

        // Per-pixel transparent windows (the floating rounded surface) render
        // nothing under Skiko's default GL backend on XWayland. Software
        // rendering makes the window visible; independent of the JBR move API.
        jvmArgs("-Dskiko.renderApi=SOFTWARE")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "collins-dictionary"
            packageVersion = providers.gradleProperty("appVersion").get()
            // jpackage needs a PNG on Linux (deb/rpm/app-image); .ico/.icns
            // would be required for the Windows/macOS platform blocks.
            linux {
                iconFile.set(file("src/main/resources/images/collins.png"))
            }
        }
    }
}