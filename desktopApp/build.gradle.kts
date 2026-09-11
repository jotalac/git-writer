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
//            targetFormats(
//                TargetFormat.Dmg,
//                TargetFormat.Pkg,
//                TargetFormat.Msi,
//                TargetFormat.Exe,
//                TargetFormat.Deb,
//                TargetFormat.Rpm,
//                TargetFormat.AppImage
//            )
            packageName = "git-writer"
            packageVersion = "0.0.2"
            description = "Git-backed note-taking app"

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
                targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
                iconFile.set(project.file("launcher_icons/icon.png"))
            }

            windows {
                targetFormats(TargetFormat.Msi, TargetFormat.Exe)
                iconFile.set(project.file("launcher_icons/icon.ico"))
            }

            macOS {
                targetFormats(TargetFormat.Dmg, TargetFormat.Pkg)
                iconFile.set(project.file("launcher_icons/icon.icns"))
                // jpackage (macOS) rejects app versions whose major component is 0, while the
                // project version is still 0.x — so the mac bundle gets its own valid version.
                packageVersion = "1.0.2"
                bundleID = "dev.jotalac.gitwriter"
            }
        }

    }
}