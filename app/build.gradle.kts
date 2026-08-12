plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val githubClientSecret = providers
    .environmentVariable("REPOGLANCE_GITHUB_CLIENT_SECRET")
    .orElse("")

android {
    namespace = "co.saari.repoglance"
    compileSdk = 35

    defaultConfig {
        applicationId = "co.saari.repoglance"
        minSdk = 31
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-auth-live"

        buildConfigField("String", "GITHUB_APP_CLIENT_ID", "Iv23livzEuAb2HMCbcUq".asBuildConfigString())
        buildConfigField("String", "GITHUB_APP_CLIENT_SECRET", githubClientSecret.get().asBuildConfigString())
        buildConfigField(
            "String",
            "GITHUB_CALLBACK_URL",
            "https://repoglance.ztoned.com/oauth/callback".asBuildConfigString(),
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.json.jvm)
}
