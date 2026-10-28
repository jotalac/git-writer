import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildKonfig)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }

        // libgit2 for the git sync, built by tools/build-libgit2.sh into a folder per target - what to bind
        // and how to link it is in src/nativeInterop/cinterop/git2.def
        val libgit2 = rootProject.layout.projectDirectory.dir("third_party/libgit2/slices/${iosTarget.name}")
        val git2 = iosTarget.compilations.getByName("main").cinterops.create("git2") {
            includeDirs(libgit2.dir("include"))
            extraOpts("-libraryPath", libgit2.dir("lib").asFile.absolutePath)
        }
        tasks.named(git2.interopProcessingTaskName) {
            val headers = libgit2.dir("include").asFile
            doFirst {
                check(headers.isDirectory) { "libgit2 is not built ($headers is missing) - run tools/build-libgit2.sh" }
            }
        }
    }

    jvm()


    android {
        namespace = "dev.jotalac.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jgit.v5)
            implementation(libs.connectivity.device)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jgit.latest)
            implementation(libs.nucleus.spell.check)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.connectivity.device)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.markdown)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.network)
            implementation(libs.coil.svg)
            implementation(libs.ktor.client.core)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.io)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)


            implementation(libs.multiplatform.markdown.renderer)
            implementation(libs.multiplatform.markdown.renderer.m3)
            implementation(libs.multiplatform.markdown.renderer.coil3)
            implementation(libs.multiplatform.markdown.renderer.code)

            implementation(libs.latex.base) // Basic logging
            implementation(libs.latex.renderer) // Rendering logic
            implementation(libs.latex.parser) // Parsing logic

            implementation(libs.connectivity.core)

            // encrypted notebooks: AES-GCM and PBKDF2 from the platform (JDK, Apple CommonCrypto + CryptoKit)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        jvmTest.dependencies {
            // Skia's native library, for tests that render Compose offscreen (the PDF export)
            implementation(compose.desktop.currentOs)
        }

        //shared platform for android and jvm (both running on JVM)
        val jvmAndAndroidMain = create("jvmAndAndroidMain") {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly(libs.jgit.v5)
            }
        }

        //make android and jvm depend on the shared platform
        androidMain.get().dependsOn(jvmAndAndroidMain)
        jvmMain.get().dependsOn(jvmAndAndroidMain)

        iosMain.get().dependsOn(commonMain.get())
        iosTest.get().dependsOn(commonTest.get())

        // connect hardware targets
        getByName("iosArm64Main").dependsOn(iosMain.get())
        getByName("iosSimulatorArm64Main").dependsOn(iosMain.get())
        getByName("iosArm64Test").dependsOn(iosTest.get())
        getByName("iosSimulatorArm64Test").dependsOn(iosTest.get())
    }
}

compose {
    resources {
        publicResClass = true
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

// create the build config to display the version in code
buildkonfig {
    packageName = "git_writer.shared"

    defaultConfigs {
        val appVersionNumber = project.findProperty("app.version.number") as? String ?: "1.0.0"

        buildConfigField(FieldSpec.Type.STRING, "APP_VERSION", appVersionNumber)
    }

}