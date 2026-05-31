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
            install("yt-dlp")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(projects.composeApp)
}