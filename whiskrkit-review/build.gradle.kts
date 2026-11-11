plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// Released together with :whiskrkit, always with the same version.
val sdkVersion = providers.gradleProperty("whiskrkitVersion").get()

android {
    namespace = "eu.whiskrkit.review"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    explicitApi()
    // Same consumer constraints as :whiskrkit (see its build file).
    coreLibrariesVersion = "2.1.20"
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

dependencies {
    // PlayReviewRequester implements eu.whiskrkit.ReviewRequester.
    api(project(":whiskrkit"))
    implementation(libs.play.review.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(groupId = "eu.whiskrkit", artifactId = "whiskrkit-android-review", version = sdkVersion)

    pom {
        name.set("WhiskrKit for Android: Google Play review")
        description.set(
            "Optional add-on for WhiskrKit for Android that shows the Google Play in-app review " +
                "after a high star rating.",
        )
        inceptionYear.set("2026")
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
