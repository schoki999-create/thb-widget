plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.yannick.thbwidget"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.yannick.thbwidget"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    // Fester Schlüssel, damit neue Versionen sich über die alte installieren lassen
    signingConfigs {
        create("fixed") {
            storeFile = file("signing.keystore")
            storePassword = "thbwidget"
            keyAlias = "thbwidget"
            keyPassword = "thbwidget"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
