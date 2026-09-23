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

/** Writes the per-target cinterop .def, substituting the machine-specific lib path. */
abstract class GenerateGit2Def : DefaultTask() {

    @get:InputFile
    abstract val template: RegularFileProperty

    @get:Input
    abstract val libgit2LibPath: Property<String>

    @get:OutputFile
    abstract val outputDef: RegularFileProperty

    @TaskAction
    fun generate() {
        val text = template.get().asFile.readText()
            .replace("\${libgit2LibPath}", libgit2LibPath.get())
        val out = outputDef.get().asFile
        out.parentFile.mkdirs()
        out.writeText(text)
    }
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

        // Static libgit2 + headers built by tools/build-libgit2.sh, one slice per target.
        val sliceDir = rootProject.layout.projectDirectory
            .dir("third_party/libgit2/slices/${iosTarget.name}")

        // libgit2 must be EMBEDDED in the cinterop klib: a static framework is
        // linked relocatably, so linkerOpts alone leaves _git_* undefined.
        val targetName = iosTarget.name
        val generateDef = tasks.register<GenerateGit2Def>("generateGit2Def${targetName.replaceFirstChar(Char::uppercase)}") {
            template.set(layout.projectDirectory.file("src/nativeInterop/cinterop/git2.def.tpl"))
            libgit2LibPath.set(sliceDir.dir("lib").asFile.absolutePath)
            // One file per target, so each cinterop task has an unambiguous input.
            outputDef.set(layout.buildDirectory.file("cinterop/$targetName/git2.def"))
        }

        val cinteropTask = iosTarget.compilations.getByName("main").cinterops.create("git2")
        cinteropTask.definitionFile.set(generateDef.flatMap { it.outputDef })
        cinteropTask.includeDirs(sliceDir.dir("include"))
        cinteropTask.compilerOpts("-DGIT_DEPRECATE_HARD=0")

        // definitionFile is only a path; make the ordering explicit so the
        // generator always runs first.
        tasks.matching { it.name == "cinteropGit2${targetName.replaceFirstChar(Char::uppercase)}" }
            .configureEach { dependsOn(generateDef) }

        // libgit2 is built with USE_HTTPS=SecureTransport and a bundled zlib, so
        // Security and CoreFoundation are its only external dependencies.
        iosTarget.binaries.all {
            linkerOpts(
                "-L${sliceDir.dir("lib").asFile.absolutePath}",
                "-lgit2",
                "-framework", "Security",
                "-framework", "CoreFoundation"
            )
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
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jgit.latest)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }

        //shared platform for android and jvm (both running on JVM)
        val jvmAndAndroidMain = create("jvmAndAndroidMain") {
            dependsOn(commonMain.get())
            dependencies {

            }
        }

        //make android and jvm depend on the shared platform
        androidMain.get().dependsOn(jvmAndAndroidMain)
        jvmMain.get().dependsOn(jvmAndAndroidMain)

        iosMain.get().dependsOn(commonMain.get())

        // connect hardware targets
        getByName("iosArm64Main").dependsOn(iosMain.get())
        getByName("iosSimulatorArm64Main").dependsOn(iosMain.get())
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