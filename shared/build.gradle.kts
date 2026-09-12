@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Pin the generated Res class package so it does not depend on the Gradle
// project name.
compose {
    resources {
        packageOfResClass = "top.blackcyan.collins.shared.generated.resources"
    }
}

// Single version source: gradle.properties feeds Android, the desktop
// distributions and this generated constant consumed by the About screen.
val appVersion = providers.gradleProperty("appVersion").get()
val appVersionCode = providers.gradleProperty("appVersionCode").get().toInt()

abstract class GenerateAppVersionTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageDir = outputDirectory.get().asFile.resolve("top/blackcyan/collins")
        packageDir.mkdirs()
        packageDir.resolve("AppVersion.kt").writeText(
            """
            package top.blackcyan.collins

            /** Build-time version constants, generated from gradle.properties. */
            object AppVersion {
                const val NAME = "Collins Dictionary"
                const val VERSION = "${version.get()}"
                const val VERSION_CODE = ${versionCode.get()}
            }
            """.trimIndent(),
        )
    }
}

val generateAppVersion = tasks.register<GenerateAppVersionTask>("generateAppVersion") {
    version.set(appVersion)
    versionCode.set(appVersionCode)
    outputDirectory.set(layout.buildDirectory.dir("generated/kotlin/appVersion"))
}

kotlin {
    jvm()

    android {
        namespace = "top.blackcyan.collins.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.jlayer)
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.composables.ui)
            implementation(libs.composables.icons.lucide)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

dependencies {
    // Coil 3.2.0 was built against Compose 1.8 / skiko 0.9.4. Our
    // Compose 1.11.1 uses skiko 0.144.6 (different versioning scheme).
    // Gradle resolves to 0.144.6, but the Compose plugin's
    // RuntimeLibrariesCompatibilityCheck still flags the requested ≠
    // selected major.minor mismatch.  Stripping the stale dependency
    // from Coil's metadata at resolution time removes it from the
    // compatibility check without affecting the runtime classpath,
    // because Compose itself pulls in skiko 0.144.6 independently.
    components {
        all {
            if (id.group.startsWith("io.coil-kt.coil3")) {
                allVariants {
                    withDependencies {
                        removeAll { it.group == "org.jetbrains.skiko" }
                    }
                }
            }
        }
    }

    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Feed the generated sources into commonMain via the generated-sources
// directory set (carries the task dependency through the Provider).
kotlin.sourceSets.named("commonMain") {
    generatedKotlin.srcDir(generateAppVersion.map { it.outputDirectory })
}
