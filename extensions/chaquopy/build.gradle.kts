plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "it.fast4x.chaquopy"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

sourceSets.all {
    java.srcDir("src/$name/kotlin")
}

chaquopy {
    defaultConfig {
        version = "3.13"
        pip {
            // Forza pip a cercare sempre l'ultima versione su PyPI ad ogni sync/build
            options("--upgrade")
            install("yt-dlp")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(projects.composeApp)
}