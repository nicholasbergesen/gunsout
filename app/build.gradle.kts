import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
}

fun readBuildSecret(name: String): String {
    val fromEnv = System.getenv(name)?.takeIf { it.isNotBlank() }
    if (fromEnv != null) return fromEnv
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        val props = Properties().apply { localProps.inputStream().use { load(it) } }
        return (props.getProperty(name) ?: "").trim()
    }
    return ""
}

val googleWebClientId: String = readBuildSecret("GOOGLE_WEB_CLIENT_ID")

// Fail any APK assembly that has no Google Web Client ID configured. Sign-in is mandatory for
// every screen behind AuthGate, so an APK without a client ID cannot do anything useful: the
// runtime would still surface SignInResult.ConfigurationError, but a shipped or sideloaded build
// in that shape is broken on purpose. Hooking into the `assemble*` tasks (rather than the script
// top level) means IDE syncs, `clean`, and unit-test runs still work while the client ID is
// being set up, and only the actual APK assembly is blocked.
gradle.projectsEvaluated {
    tasks.matching { it.name.startsWith("assemble") }.configureEach {
        doFirst {
            require(googleWebClientId.isNotBlank()) {
                "GOOGLE_WEB_CLIENT_ID is empty. Set it in local.properties or as the " +
                    "GOOGLE_WEB_CLIENT_ID env var / CI secret. See README \"Google Sign-In setup\" " +
                    "for details."
            }
        }
    }
}

// Version is driven by env vars in CI so each pushed APK installs cleanly over the previous one.
// Locally these fall through to the defaults; the values still produce a valid APK.
val ciVersionCode: Int = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val ciVersionName: String = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.1.0"

// Stable debug keystore checked in at app/gunsout-debug.keystore. When present, every build
// (CI or local) signs with the same certificate so sideloaded APKs update in place instead of
// being rejected with INSTALL_FAILED_UPDATE_INCOMPATIBLE. When absent, AGP falls back to the
// default ~/.android/debug.keystore behaviour, which keeps a first-time `./gradlew assembleDebug`
// working before the user has generated the shared keystore.
val sharedDebugKeystore = rootProject.file("app/gunsout-debug.keystore")

android {
    namespace = "com.nicholasbergesen.gunsout"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nicholasbergesen.gunsout"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode
        versionName = ciVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"" + googleWebClientId.replace("\"", "\\\"") + "\""
        )
    }

    signingConfigs {
        getByName("debug") {
            if (sharedDebugKeystore.exists()) {
                storeFile = sharedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

extensions.configure<KotlinAndroidProjectExtension>("kotlin") {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Tell Room to export schema JSON files into app/schemas so migrations can be authored against a
// checked-in baseline.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.register<JacocoReport>("debugUnitTestCoverage") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/debugUnitTestCoverage/debugUnitTestCoverage.xml"))
        html.required.set(true)
        csv.required.set(false)
    }

    val generatedClassExclusions = listOf(
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/R.class",
        "**/R$*.class",
        "**/ComposableSingletons*.*",
        "**/*\$\$inlined\$*.*",
        "**/*_Factory*.*",
        "**/*_MembersInjector.*",
        "**/*_Impl*.*",
        "**/*_GeneratedInjector.*",
        "**/*_ComponentTreeDeps.*",
        "**/*_Provide*Factory.*",
        "**/_com_*.*",
        "**/Dagger*.*",
        "**/Hilt_*.*",
        "**/*Hilt*.*"
    )

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) {
            exclude(generatedClassExclusions)
        }
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}

// One-time helper: generate the shared debug keystore that every build (local and CI) signs with.
// Run `./gradlew generateDebugKeystore` once, commit `app/gunsout-debug.keystore`, and from then
// on every published APK installs as an in-place update over previous CI builds.
tasks.register<Exec>("generateDebugKeystore") {
    group = "gunsout"
    description = "Creates the checked-in debug keystore used for sideload-friendly signing."
    val keystoreFile = sharedDebugKeystore
    outputs.file(keystoreFile)
    onlyIf {
        val exists = keystoreFile.exists()
        if (exists) {
            logger.lifecycle("Debug keystore already exists at ${keystoreFile.absolutePath}. Skipping.")
        }
        !exists
    }
    doFirst { keystoreFile.parentFile.mkdirs() }
    commandLine(
        "keytool", "-genkeypair", "-v",
        "-keystore", keystoreFile.absolutePath,
        "-storetype", "PKCS12",
        "-storepass", "android",
        "-keypass", "android",
        "-alias", "androiddebugkey",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "36500",
        "-dname", "CN=Android Debug,O=Android,C=US"
    )
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.mockwebserver)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
