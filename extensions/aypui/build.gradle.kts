plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    namespace = "it.fast4x.androidyoutubeplayer.core.customui"

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
    implementation(project(":ayp"))
}