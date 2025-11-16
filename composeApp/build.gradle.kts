import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    jvm("desktop")



    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)

            implementation(libs.coil.network.okhttp)
            runtimeOnly(libs.kotlinx.coroutines.swing)

            /*
            // Uncomment only for build jvm desktop version
            // Comment before build android version
            configurations.commonMainApi {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
            }
            */

        }

        androidMain.dependencies {
            implementation(libs.navigation)
            implementation(libs.media3.session)
            implementation(libs.media3.ui)
            implementation(libs.kotlin.coroutines.guava)
            implementation(libs.kotlin.concurrent.futures)
            implementation(libs.androidx.webkit)
            implementation(libs.room.backup)
            implementation(libs.workmanager)
            implementation(libs.accompanist)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(projects.environment)
            implementation(projects.kugou)
            implementation(projects.lrclib)
            implementation(projects.audiotaginfo)


            implementation(libs.room)
            implementation(libs.room.runtime)
            implementation(libs.room.sqlite.bundled)

            implementation(libs.mediaplayer.kmp)

            implementation(libs.navigation.kmp)

            //coil3 mp
            implementation(libs.coil.compose.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.mp)

            implementation(libs.translator)
            implementation(libs.reorderable)

            implementation(libs.fastscroller)
            implementation(libs.fastscroller.material3)
            implementation(libs.fastscroller.indicator)

        }
    }
}

android {

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    fun Project.propertyOrEmpty(name: String): String {
        val property = findProperty(name) as String?
        return property ?: ""
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileSdk = 36

    defaultConfig {
        applicationId = "it.fast4x.riplay"
        minSdk = 21
        targetSdk = 36
        versionCode = 42
        versionName = "0.0.$versionCode-alpha"

        multiDexEnabled = true

        // INIT ENVIRONMENT
        resValue(
            "string",
            "env_CrQ0JjAXgv",
            propertyOrEmpty("CrQ0JjAXgv")
        )
        resValue(
            "string",
            "env_hNpBzzAn7i",
            propertyOrEmpty("hNpBzzAn7i")
        )
        resValue(
            "string",
            "env_lEi9YM74OL",
            propertyOrEmpty("lEi9YM74OL")
        )
        resValue(
            "string",
            "env_C0ZR993zmk",
            propertyOrEmpty("C0ZR993zmk")
        )
        resValue(
            "string",
            "env_w3TFBFL74Y",
            propertyOrEmpty("w3TFBFL74Y")
        )
        resValue(
            "string",
            "env_mcchaHCWyK",
            propertyOrEmpty("mcchaHCWyK")
        )
        resValue(
            "string",
            "env_L2u4JNdp7L",
            propertyOrEmpty("L2u4JNdp7L")
        )
        resValue(
            "string",
            "env_sqDlfmV4Mt",
            propertyOrEmpty("sqDlfmV4Mt")
        )
        resValue(
            "string",
            "env_WpLlatkrVv",
            propertyOrEmpty("WpLlatkrVv")
        )
        resValue(
            "string",
            "env_1zNshDpFoh",
            propertyOrEmpty("1zNshDpFoh")
        )
        resValue(
            "string",
            "env_mPVWVuCxJz",
            propertyOrEmpty("mPVWVuCxJz")
        )
        resValue(
            "string",
            "env_auDsjnylCZ",
            propertyOrEmpty("auDsjnylCZ")
        )
        resValue(
            "string",
            "env_AW52cvJIJx",
            propertyOrEmpty("AW52cvJIJx")
        )
        resValue(
            "string",
            "env_0RGAyC1Zqu",
            propertyOrEmpty("0RGAyC1Zqu")
        )
        resValue(
            "string",
            "env_4Fdmu9Jkax",
            propertyOrEmpty("4Fdmu9Jkax")
        )
        resValue(
            "string",
            "env_kuSdQLhP8I",
            propertyOrEmpty("kuSdQLhP8I")
        )
        resValue(
            "string",
            "env_QrgDKwvam1",
            propertyOrEmpty("QrgDKwvam1")
        )
        resValue(
            "string",
            "env_wLwNESpPtV",
            propertyOrEmpty("wLwNESpPtV")
        )
        resValue(
            "string",
            "env_JJUQaehRFg",
            propertyOrEmpty("JJUQaehRFg")
        )
        resValue(
            "string",
            "env_i7WX2bHV6R",
            propertyOrEmpty("i7WX2bHV6R")
        )
        resValue(
            "string",
            "env_XpiuASubrV",
            propertyOrEmpty("XpiuASubrV")
        )
        resValue(
            "string",
            "env_lOlIIVw38L",
            propertyOrEmpty("lOlIIVw38L")
        )
        resValue(
            "string",
            "env_mtcR0FhFEl",
            propertyOrEmpty("mtcR0FhFEl")
        )
        resValue(
            "string",
            "env_DTihHAFaBR",
            propertyOrEmpty("DTihHAFaBR")
        )
        resValue(
            "string",
            "env_a4AcHS8CSg",
            propertyOrEmpty("a4AcHS8CSg")
        )
        resValue(
            "string",
            "env_krdLqpYLxM",
            propertyOrEmpty("krdLqpYLxM")
        )
        resValue(
            "string",
            "env_ye6KGLZL7n",
            propertyOrEmpty("ye6KGLZL7n")
        )
        resValue(
            "string",
            "env_ec09m20YH5",
            propertyOrEmpty("ec09m20YH5")
        )
        resValue(
            "string",
            "env_LDRlbOvbF1",
            propertyOrEmpty("LDRlbOvbF1")
        )
        resValue(
            "string",
            "env_EEqX0yizf2",
            propertyOrEmpty("EEqX0yizf2")
        )
        resValue(
            "string",
            "env_i3BRhLrV1v",
            propertyOrEmpty("i3BRhLrV1v")
        )
        resValue(
            "string",
            "env_MApdyHLMyJ",
            propertyOrEmpty("MApdyHLMyJ")
        )
        resValue(
            "string",
            "env_hizI7yLjL4",
            propertyOrEmpty("hizI7yLjL4")
        )
        resValue(
            "string",
            "env_rLoZP7BF4c",
            propertyOrEmpty("rLoZP7BF4c")
        )
        resValue(
            "string",
            "env_nza34sU88C",
            propertyOrEmpty("nza34sU88C")
        )
        resValue(
            "string",
            "env_dwbUvjWUl3",
            propertyOrEmpty("dwbUvjWUl3")
        )
        resValue(
            "string",
            "env_fqqhBZd0cf",
            propertyOrEmpty("fqqhBZd0cf")
        )
        resValue(
            "string",
            "env_9sZKrkMg8p",
            propertyOrEmpty("9sZKrkMg8p")
        )
        resValue(
            "string",
            "env_aQpNCVOe2i",
            propertyOrEmpty("aQpNCVOe2i")
        )
        resValue(
            "string",
            "env_XNl2TKXLlB",
            propertyOrEmpty("XNl2TKXLlB")
        )
        resValue(
            "string",
            "env_yNjbjspY8v",
            propertyOrEmpty("yNjbjspY8v")
        )
        resValue(
            "string",
            "env_eZueG672lt",
            propertyOrEmpty("eZueG672lt")
        )
        resValue(
            "string",
            "env_WkUFhXtC3G",
            propertyOrEmpty("WkUFhXtC3G")
        )
        resValue(
            "string",
            "env_z4Xe47r8Vs",
            propertyOrEmpty("z4Xe47r8Vs")
        )
        // INIT ENVIRONMENT

        //INIT CHROMECAST RECEIVER
        resValue(
            "string",
            "RiPlay_CHROMECAST_APPLICATION_ID",
            propertyOrEmpty("RiPlay_CHROMECAST_APPLICATION_ID")
        )
        resValue(
            "string",
            "Ayp_CHROMECAST_APPLICATION_ID",
            propertyOrEmpty("Ayp_CHROMECAST_APPLICATION_ID")
        )
        //INIT CHROMECAST RECEIVER

        //INIT AudioTagInfo Api
        resValue(
            "string",
            "AudioTagInfo_API_KEY",
            propertyOrEmpty("AudioTagInfo_API_KEY")
        )
        //INIT AudioTagInfo Api

    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {

            // Enables building multiple APKs per ABI.
            isEnable = false

            // By default all ABIs are included, so use reset() and include to specify that you only
            // want APKs for x86 and x86_64.

            // Resets the list of ABIs for Gradle to create APKs for to none.
            reset()

            // Specifies a list of ABIs for Gradle to create APKs for.
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")

            // Specifies that you don't want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }

    namespace = "it.fast4x.riplay"

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "RiPlay-Debug"
        }

        release {
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "RiPlay"
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("full") {
            isDefault = true
            dimension = "version"
        }
    }
    productFlavors {
        create("accrescent") {
            dimension = "version"
            manifestPlaceholders["appName"] = "RiPlay-Acc"
        }
    }
    productFlavors {
        create("fdroid") {
            dimension = "version"
            //manifestPlaceholders["appName"] = "RiPlay"
        }
    }

    tasks.withType<KotlinCompile> {
        if (name.substringAfter("compile").lowercase().startsWith("fdroid")) {
            exclude("**/extensions/chromecast/**")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "RiPlay-${output.baseName}-${variant.versionName}.apk"
                //val outputFileName = "riplay-${variant.baseName}.apk"
                output.outputFileName = outputFileName
            }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }



    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

//    composeOptions {
//        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
//    }

    androidResources {
        generateLocaleConfig = true
    }

}



java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {

        mainClass = "MainKt"


        //conveyor
        version = "0.0.1"
        group = "riplay"
/*

        nativeDistributions {
            vendor = "fast4x RiPlay"
            description = "Desktop music player"
        }
        */

        //jpackage
        nativeDistributions {
            //conveyor
            vendor = "RiPlay.DesktopApp"
            description = "RiPlay Desktop Video Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "RiPLay.DesktopApp"
            packageVersion = "0.0.1"

            /*
            val iconsRoot = project.file("desktop-icons")
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
            }
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }

             */
        }

    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    listOf(
        "kspAndroid",
         "ksp",
        //"kspIosSimulatorArm64",
        //"kspIosX64",
        //"kspIosArm64"
    ).forEach {
        add(it, libs.room.compiler)
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation(projects.composePersist)
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ripple)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.coil)
    implementation(libs.palette)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.appcompat)
    implementation(libs.appcompat.resources)
    implementation(libs.support)
    implementation(libs.media)
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.compose.ui.graphics.android)
    implementation(libs.constraintlayout)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.animation)
    implementation(libs.kotlin.csv)
    implementation(libs.monetcompat)
    implementation(libs.androidmaterial)
    implementation(libs.timber)
    implementation(libs.crypto)
    implementation(libs.logging.interceptor)
    implementation(libs.math3)
    implementation(libs.toasty)
    implementation(libs.haze)
    implementation(libs.androidyoutubeplayer)
    implementation(libs.androidyoutubeplayer.custom.ui)
    "fullImplementation"(libs.androidyoutubeplayer.chromecast.sender)
    "accrescentImplementation"(libs.androidyoutubeplayer.chromecast.sender)
    implementation(libs.androidx.mediarouter)
    implementation(libs.glance.widgets)
    implementation(libs.kizzy.rpc)
    implementation(libs.gson)
    implementation(libs.hypnoticcanvas)
    implementation(libs.hypnoticcanvas.shaders)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.multidex)


    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(projects.environment)
    implementation(projects.kugou)
    implementation(projects.lrclib)

    coreLibraryDesugaring(libs.desugaring)

}
