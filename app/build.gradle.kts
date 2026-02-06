import java.util.Properties

val envPropertiesFile = rootProject.file("env.properties")
val envProperties = Properties().apply {
    if (envPropertiesFile.exists()) {
        envPropertiesFile.inputStream().use { load(it) }
    }
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrainsKotlinKsp)
    alias(libs.plugins.hiltPlugin)
}



android {
    namespace = "com.kiosktouchscreendpr.cosmic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kiosktouchscreendpr.cosmic"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_PASSWORD", "\"${envProperties["APP_PASSWORD"]}\"")
        buildConfigField("String", "WS_URL", "\"${envProperties["WS_URL"]}\"")
        buildConfigField("String", "WEBVIEW_BASEURL", "\"${envProperties["WEBVIEW_BASEURL"]}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            // Using debug keystore for simplicity - in production use proper keystore
            val debugKeystorePath = "${System.getProperty("user.home")}/.android/debug.keystore"
            storeFile = file(debugKeystorePath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            // Enable both v1 and v2 signing for better compatibility
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    /* =================> external libs <================= */
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.koin)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material.icons.core)
    implementation (libs.hilt.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    ksp(libs.dagger.compiler)
    ksp(libs.hilt.compiler)
    implementation (libs.androidx.hilt.navigation.compose)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    /* =================> H.264 & Media Codec <================= */
    implementation("androidx.media3:media3-common:1.1.1")
    implementation("androidx.media3:media3-exoplayer:1.1.1")
    
    /* =================> Network Monitoring & Utils <================= */
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("org.json:json:20230227")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}