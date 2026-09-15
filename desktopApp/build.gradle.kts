import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    implementation(libs.compose.components.resources)

    // Native window handle access + DWM calls, used to theme the Windows title bar.
    implementation(libs.jna)
}

compose.desktop {
    application {
        mainClass = "dev.jotalac.MainKt"

        nativeDistributions {
            packageName = project.findProperty("app.name.display") as? String ?: "GitWriter"
            packageVersion = project.findProperty("app.version.number") as? String ?: "1.0.0"
            description = "Git-backed note-taking app"

            targetFormats = buildSet {
                add(TargetFormat.Deb)
                add(TargetFormat.Rpm)
                add(TargetFormat.Msi)
                add(TargetFormat.Exe)
                add(TargetFormat.Dmg)
                add(TargetFormat.Pkg)
            }

            modules(
                "jdk.unsupported",
                "java.sql",
                "java.naming",
                "java.desktop",
                "java.management",
                "java.security.jgss"
            )

            buildTypes.release.proguard {
                // disable ProGuard to prevent it from stripping background libraries - later configure it properly
                isEnabled.set(false)
            }

            linux {
                iconFile.set(project.file("launcher_icons/icon.png"))
            }

            windows {
                iconFile.set(project.file("launcher_icons/icon.ico"))
            }

            macOS {
                iconFile.set(project.file("launcher_icons/icon.icns"))
                bundleID = "dev.jotalac.gitwriter"
                // macos crashes when the version is bellow 1.0.0 - later in the stable release it will use the gobal versino nubmer
            }
        }

    }
}