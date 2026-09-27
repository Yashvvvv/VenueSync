plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.venuesync.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.venuesync.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-m0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Base URLs live here (not hardcoded in code) so debug/release can diverge later.
        buildConfigField("String", "API_BASE_URL", "\"https://venuesync-backend.onrender.com/api/v1\"")
        // Auth0 "VenueSync-App" (Native, public client — the id ships in every APK, it is not a secret).
        buildConfigField("String", "OIDC_AUTHORITY", "\"https://dev-gtxw4nl5kf3t4pxd.us.auth0.com\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"0veL3NInWpoVPwGk4BqmZML1yAik8mvI\"")
        // Must equal the backend's AUTH0_AUDIENCE. Without it Auth0 issues a token the API rejects.
        buildConfigField("String", "OIDC_AUDIENCE", "\"https://api.venuesync.app\"")
        buildConfigField("String", "OIDC_REDIRECT_URI", "\"venuesync://oauth2redirect\"")
        // AppAuth's own manifest registers the redirect catcher activity with this scheme.
        manifestPlaceholders["appAuthRedirectScheme"] = "venuesync"
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.appauth)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
