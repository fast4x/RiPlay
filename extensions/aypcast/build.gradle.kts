plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    namespace = "it.fast4x.androidyoutubeplayer.cast"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

sourceSets.all {
    java.srcDir("src/$name/java")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.media3.cast)
    implementation(project(":ayp"))
}