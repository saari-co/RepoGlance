plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// --- Version derivation ------------------------------------------------
//
// Single source of truth for the app version is the git tag; no version
// number is hand-committed here. See docs/RELEASING.md for the full cut
// procedure.
//
// versionName is the tag with its leading `v` stripped, e.g. `0.3.0` or
// `0.3.0-beta.1`.
//
// versionCode = MAJOR*1_000_000 + MINOR*10_000 + PATCH*100 + preCode
//   preCode = 99 for a stable release, N for a `-beta.N` prerelease.
//   Requires: MINOR < 100, PATCH < 100, beta N in 1..98 (build fails with a
//   clear message otherwise). This keeps betas sorted below their stable
//   and versionCode always climbing. Verified examples:
//     v0.3.0-beta.1 -> 0*1_000_000 + 3*10_000 + 0*100 +  1 = 30001
//     v0.3.0        -> 0*1_000_000 + 3*10_000 + 0*100 + 99 = 30099
//     v0.3.1        -> 0*1_000_000 + 3*10_000 + 1*100 + 99 = 30199
//     v0.4.0        -> 0*1_000_000 + 4*10_000 + 0*100 + 99 = 40099
//
// Local builds off a non-tag commit must not fail: when no tag is resolvable
// this falls back to versionName "0.0.0-dev", versionCode 1.
data class RepoGlanceVersion(val versionName: String, val versionCode: Int)

fun resolveReleaseTag(): String? {
    val refName = System.getenv("GITHUB_REF_NAME")
    if (refName != null && refName.startsWith("v")) {
        return refName
    }
    return try {
        val process = ProcessBuilder("git", "describe", "--tags", "--exact-match")
            .redirectErrorStream(false)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode == 0 && output.startsWith("v")) output else null
    } catch (e: Exception) {
        null
    }
}

fun resolveRepoGlanceVersion(): RepoGlanceVersion {
    val devFallback = RepoGlanceVersion(versionName = "0.0.0-dev", versionCode = 1)
    val tag = resolveReleaseTag() ?: return devFallback

    val stablePattern = Regex("""^v(\d+)\.(\d+)\.(\d+)$""")
    val betaPattern = Regex("""^v(\d+)\.(\d+)\.(\d+)-beta\.(\d+)$""")
    val stableMatch = stablePattern.matchEntire(tag)
    val betaMatch = betaPattern.matchEntire(tag)
    val match = stableMatch ?: betaMatch
        ?: error(
            "Release tag '$tag' does not match the required vMAJOR.MINOR.PATCH " +
                "or vMAJOR.MINOR.PATCH-beta.N format. See docs/RELEASING.md."
        )

    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch = match.groupValues[3].toInt()
    val betaN = if (match === betaMatch) match.groupValues[4].toInt() else null

    if (minor >= 100) {
        error("Release tag '$tag': MINOR must be < 100, got $minor.")
    }
    if (patch >= 100) {
        error("Release tag '$tag': PATCH must be < 100, got $patch.")
    }
    if (betaN != null && betaN !in 1..98) {
        error("Release tag '$tag': beta N must be in 1..98, got $betaN.")
    }

    val preCode = betaN ?: 99
    val versionName = tag.removePrefix("v")
    val versionCode = major * 1_000_000 + minor * 10_000 + patch * 100 + preCode
    return RepoGlanceVersion(versionName = versionName, versionCode = versionCode)
}

val repoGlanceVersion = resolveRepoGlanceVersion()

// --- Release signing (conditional) --------------------------------------
//
// CI decodes secret ANDROID_KEYSTORE_B64 to a temp file and points
// ANDROID_KEYSTORE_PATH at it before invoking Gradle, alongside
// ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD. When
// that file isn't present (secret not yet provisioned, or a local dev
// build), the release build type is left unsigned rather than failing.
val releaseKeystoreFile = System.getenv("ANDROID_KEYSTORE_PATH")
    ?.let { File(it) }
    ?.takeIf { it.isFile }

android {
    namespace = "co.saari.repoglance"
    compileSdk = 35

    defaultConfig {
        applicationId = "co.saari.repoglance"
        minSdk = 31
        targetSdk = 35
        versionCode = repoGlanceVersion.versionCode
        versionName = repoGlanceVersion.versionName

        buildConfigField("String", "GITHUB_APP_CLIENT_ID", "Iv23livzEuAb2HMCbcUq".asBuildConfigString())
    }

    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

// Diagnostic task for release.yml and local verification:
// ./gradlew -q printVersion
tasks.register("printVersion") {
    doLast {
        println("versionName=${repoGlanceVersion.versionName}")
        println("versionCode=${repoGlanceVersion.versionCode}")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
}
