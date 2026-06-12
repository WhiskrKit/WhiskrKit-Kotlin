plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.maven.publish)
}

val sdkVersion = "0.1.0"

android {
    namespace = "eu.whiskrkit"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Plain-JVM tests hit android.util.Log via WhiskrLog; no-op it.
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

roborazzi {
    // Golden images live in the repo, not in build/.
    outputDir.set(file("src/test/snapshots"))
}

dependencies {
    implementation(platform(libs.compose.bom))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.lifecycle.process)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.konsist)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.espresso.core)
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(groupId = "eu.whiskrkit", artifactId = "whiskrkit-android", version = sdkVersion)

    pom {
        name.set("WhiskrKit for Android")
        description.set("In-app survey and feedback SDK for Android, built with Jetpack Compose.")
        inceptionYear.set("2026")
        // TODO: replace with the real repo URL once the GitHub repo exists.
        url.set("https://github.com/whiskrkit/whiskrkit-kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("dennisvermeulen")
                name.set("Dennis Vermeulen")
            }
        }
        scm {
            url.set("https://github.com/whiskrkit/whiskrkit-android")
            connection.set("scm:git:git://github.com/whiskrkit/whiskrkit-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/whiskrkit/whiskrkit-android.git")
        }
    }

    // Signing only when CI provides the key (publishing requires it; local builds don't).
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
