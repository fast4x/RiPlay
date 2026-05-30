plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    namespace = "it.fast4x.androidyoutubeplayer.cast"

    sourceSets {
        all {
            java.directories.add("src/$name/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}


dependencies {
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.media3.cast)
    implementation(project(":ayp"))
}