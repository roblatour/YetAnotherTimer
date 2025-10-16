import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
// Load external signing properties (SIGNING_FILE env var or -PsigningFile=path) before android configuration.
// Note: -PsigningFile is a Gradle project property, not a Java system property.
val signingFilePath = (project.findProperty("signingFile") as String?) ?: System.getenv("SIGNING_FILE")
if (!signingFilePath.isNullOrBlank()) {
    val sf = file(signingFilePath)
    if (sf.exists()) {
        val props = Properties()
        sf.inputStream().use { props.load(it) }
        for (name in props.stringPropertyNames()) {
            if (project.findProperty(name) == null) {
                project.extensions.extraProperties.set(name, props.getProperty(name))
            }
        }
        println("Loaded signing properties from $signingFilePath")
    } else {
        println("WARNING: SIGNING_FILE specified but file not found: $signingFilePath")
    }
}

android {
    namespace = "com.example.yetanothertimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.roblatour.yetanothertimer"
        minSdk = 24
    targetSdk = 35
        versionCode = (project.findProperty("VERSION_CODE") as String).toInt()
        versionName = project.findProperty("VERSION_NAME") as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Only create release config if required properties are present
        val hasSigning = listOf(
            "RELEASE_STORE_FILE",
            "RELEASE_STORE_PASSWORD",
            "RELEASE_KEY_ALIAS"
        ).all { project.findProperty(it) != null }
        if (hasSigning) {
            create("release") {
                val storePath = project.findProperty("RELEASE_STORE_FILE") as String
                val storePass = project.findProperty("RELEASE_STORE_PASSWORD") as String
                val alias = project.findProperty("RELEASE_KEY_ALIAS") as String
                val keyPass = (project.findProperty("RELEASE_KEY_PASSWORD") as String?) ?: storePass

                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            // Resource shrinking pairs with code shrinking (R8) to reduce size
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Attach signing config if defined
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // Simple helper task to print version (invoked via: gradlew printAppVersion)
    tasks.register("printAppVersion") {
        group = "help"
        description = "Prints the current app versionCode and versionName"
        doLast {
            println("App Version -> code=" + (project.findProperty("VERSION_CODE")) + ", name=" + (project.findProperty("VERSION_NAME")))
        }
    }

    // Prints SHA-1 / SHA-256 / MD5 fingerprints of the upload keystore (for APIs / Play Console)
    tasks.register("printUploadKeyFingerprints") {
        group = "help"
        description = "Prints certificate fingerprints of the configured upload keystore"
        doLast {
            val required = listOf(
                "RELEASE_STORE_FILE",
                "RELEASE_STORE_PASSWORD",
                "RELEASE_KEY_ALIAS"
            )
            if (!required.all { project.findProperty(it) != null }) {
                println("Missing properties. Provide RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS (and RELEASE_KEY_PASSWORD if different).")
                return@doLast
            }
            val storeFile = project.findProperty("RELEASE_STORE_FILE") as String
            val storePass = project.findProperty("RELEASE_STORE_PASSWORD") as String
            val alias = project.findProperty("RELEASE_KEY_ALIAS") as String
            val keyPass = (project.findProperty("RELEASE_KEY_PASSWORD") as String?) ?: storePass

            try {
                project.exec {
                    commandLine(
                        "keytool",
                        "-list", "-v",
                        "-keystore", storeFile,
                        "-alias", alias,
                        "-storepass", storePass,
                        "-keypass", keyPass
                    )
                }
            } catch (e: Exception) {
                println("Failed to execute keytool: ${e.message}")
                println("Ensure JDK 'keytool' is on PATH and the keystore path is correct.")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all"
        )
    }

    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Compose Material icons (for Icons.Filled.Settings)
    implementation("androidx.compose.material:material-icons-extended")
    // Material Components for Android (XML theme: Theme.Material3.*)
    implementation("com.google.android.material:material:1.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Locale management
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
