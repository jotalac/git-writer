import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.compose.uiToolingPreview)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

android {
    namespace = "dev.jotalac"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.jotalac.gitwriter"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        val appVersionNumber = project.findProperty("app.version.number") as? String ?: "1.0.0"
        // 1.2.3 -> 10203: every part gets two digits, so the code grows with each release (1.0.12 comes after 1.0.9)
        val (major, minor, patch) = appVersionNumber.split(".").map { it.toInt() }
        require(minor < 100 && patch < 100) { "minor and patch must be below 100 for the version code: $appVersionNumber" }

        versionCode = major * 10_000 + minor * 100 + patch
        versionName = appVersionNumber
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                rootProject.file("proguard/shared-rules.pro")
            )

            manifestPlaceholders["appName"] = project.findProperty("app.name.display") as? String ?: "GitWriter"
        }

        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "GitWriter (Debug)"
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}