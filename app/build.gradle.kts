import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "net.blazarforce.folio"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.blazarforce.blazarreader"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.5"
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    // "direct" = BlazarSwap / GitHub sideload build (in-app banner links to the newer APK).
    // "play"   = Google Play build: updates come only from Play, so the external APK banner is compiled out
    //            (Device and Network Abuse policy: no self-updating outside Play).
    flavorDimensions += "distribution"
    productFlavors {
        create("direct") {
            dimension = "distribution"
            isDefault = true
            buildConfigField("boolean", "ENABLE_EXTERNAL_UPDATE_BANNER", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_EXTERNAL_UPDATE_BANNER", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.maxHeapSize = "2g"
                // Forward screenshot/testdata paths into the test JVM (workers may not inherit every env var).
                System.getenv("FOLIO_SHOTS")?.let { shots -> it.environment("FOLIO_SHOTS", shots) }
                System.getenv("FOLIO_TESTDATA")?.let { data -> it.environment("FOLIO_TESTDATA", data) }
                System.getenv("FOLIO_PLAY_SHOTS")?.let { shots -> it.environment("FOLIO_PLAY_SHOTS", shots) }
            }
        }
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation(bom)
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.43.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.43.0")
    // Merges ComponentActivity into the debug manifest for Robolectric Compose tests.
    // Must stay debug-only so it is not packaged in the release AAB.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Release unit tests use the release manifest, which correctly omits the test-only
// ComponentActivity. Compose createComposeRule() then fails. The same suites are
// run on the debug variant, which is the intended Robolectric setup.
tasks.matching { it.name.startsWith("test") && it.name.endsWith("ReleaseUnitTest") }.configureEach { enabled = false }

