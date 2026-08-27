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
}

compose.desktop {
    application {
        mainClass = "dev.jotalac.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.AppImage
            )
            packageName = "git-writer"
            packageVersion = "0.0.1"
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
        }

    }
}