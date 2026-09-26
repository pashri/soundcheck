import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Local signing config, absent on any machine but the author's and never committed.
 *
 * Debug builds and unit tests don't need it. Unlike the author's other apps, packaging a
 * release without it fails (see `checkReleaseSigning`) instead of quietly building an unsigned
 * APK.
 */
val keystoreFile: File = rootProject.file("keystore.properties")

val keystoreProperties: Properties = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

/**
 * The keystore's path, with a leading `~/` meaning the home folder; a relative path is read
 * from the project root, where `keystore.properties` lives.
 */
val releaseStoreFile: String? = keystoreProperties.getProperty("storeFile")?.let { path ->
    if (path.startsWith("~/")) System.getProperty("user.home") + path.substring(1) else path
}?.let { rootProject.file(it).path }

/**
 * Signs the release build with this Mac's debug key instead, for trying the minified build
 * on the emulator. Such an APK never leaves the Mac.
 */
val signReleaseWithDebugKey: Boolean =
    providers.gradleProperty("soundcheck.signWithDebugKey").orNull == "true"

/** What keeps the release from being signed, by name; never a value from the file. */
val releaseSigningProblems: List<String> = if (!keystoreFile.exists()) {
    listOf("keystore.properties is missing from the project root")
} else {
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .filter { keystoreProperties.getProperty(it).isNullOrBlank() }
        .map { "keystore.properties has no $it" } +
        listOfNotNull(
            "the keystore that storeFile names doesn't exist"
                .takeIf { releaseStoreFile?.let { !File(it).exists() } == true },
        )
}

android {
    namespace = "org.pashri.soundcheck"
    compileSdk = 35
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "org.pashri.soundcheck"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        resValue(type = "string", name = "app_name", value = "Soundcheck")
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        if (releaseSigningProblems.isEmpty()) {
            create("release") {
                storeFile = File(checkNotNull(releaseStoreFile))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue(type = "string", name = "app_name", value = "Soundcheck debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (signReleaseWithDebugKey) {
                signingConfigs.getByName("debug")
            } else {
                signingConfigs.findByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        prefab = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

/** Stops a release build that has no signing key, before anything is packaged or signed. */
val checkReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails a release build that has no signing key configured."
    val problems = releaseSigningProblems
    val debugKey = signReleaseWithDebugKey
    doFirst {
        if (!debugKey && problems.isNotEmpty()) {
            throw GradleException(
                "The release build needs its signing key (see the README, " +
                    "\"Release build\"):\n  " + problems.joinToString(separator = "\n  "),
            )
        }
    }
}

// Hung on packaging, not preReleaseBuild, so release compilation and `./gradlew test` never
// need the key.
tasks.matching { it.name in setOf("packageRelease", "bundleRelease") }.configureEach {
    dependsOn(checkReleaseSigning)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.oboe)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
