import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
    //alias(libs.plugins.chaquopy)
}

kotlin {

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    dependencies {
        implementation(projects.composeApp)
        implementation(libs.navigation)
        implementation(libs.media3.session)
        implementation(libs.kotlin.coroutines.guava)
        implementation(libs.kotlin.concurrent.futures)
        implementation(libs.androidx.webkit)
        implementation(libs.workmanager)
        implementation(libs.accompanist)

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
        //implementation(libs.androidyoutubeplayer) // replaced by project ayp
        //implementation(libs.androidyoutubeplayer.custom.ui) // replaced by project aypui
        implementation(projects.ayp)
        implementation(projects.aypui)
        implementation(libs.glance.widgets)
        implementation(libs.kizzy.rpc)
        implementation(libs.gson)
        implementation(libs.hypnoticcanvas)
        implementation(libs.hypnoticcanvas.shaders)
        //implementation(libs.multidex)
        implementation(libs.jsoup)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.okhttp)
        implementation(libs.ktor.client.websockets)

        implementation(projects.environment)
        implementation(projects.kugou)
        implementation(projects.lrclib)
        implementation(projects.audiotaginfo)
        implementation(projects.lastfm)
        implementation(projects.simpmusiclyrics)

        implementation(libs.room.ktx)
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

        implementation (libs.jaudiotagger)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.okhttp)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.client.encoding)
        implementation(libs.ktor.client.serialization)
        implementation(libs.ktor.serialization.json)
    }
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
extensions.configure<ApplicationExtension> {

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
        resValues = true
    }

    compileSdk = 37

    defaultConfig {
        applicationId = "it.fast4x.riplay"
        targetSdk = 37

        versionCode = 84
        versionName = "0.7.83"


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

        // INIT APIKEYS
        resValue("string", "AudioTagInfo_API_KEY", propertyOrEmpty("AudioTagInfo_API_KEY"))
        resValue("string", "RiPlay_LASTFM_API_KEY", propertyOrEmpty("RiPlay_LASTFM_API_KEY"))
        resValue("string", "RiPlay_LASTFM_SECRET", propertyOrEmpty("RiPlay_LASTFM_SECRET"))
        resValue("string", "RiPlay_DISCORD_APPLICATION_ID", propertyOrEmpty("RiPlay_DISCORD_APPLICATION_ID"))


//        ndk {
//            abiFilters += listOf("arm64-v8a", "x86_64")
//        }

    }

//    chaquopy {
//        defaultConfig {
//            version = "3.13"
//            pip {
//                install("yt-dlp")
//            }
//        }
//    }

    packaging {
        jniLibs.keepDebugSymbols.add("**/*.so")
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        abi {
            isEnable = false
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    namespace = "it.fast4x.riplay"

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE")
                ?: localProperties.getProperty("SIGNING_STORE_FILE")
            val storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: localProperties.getProperty("SIGNING_STORE_PASSWORD")
            val keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: localProperties.getProperty("SIGNING_KEY_ALIAS")
            val keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: localProperties.getProperty("SIGNING_KEY_PASSWORD")

            if (storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                logger.warn("⚠️  Signing config not complete - APK not signed")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "RiPlay-Debug"
        }

        release {
            vcsInfo.include = true
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "RiPlay"
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            //multiDexKeepProguard = File("multidex-config.pro")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("full") {
            minSdk = 24
            isDefault = true
            dimension = "version"
            //buildConfigField("String", "BUILD_VARIANT", "\"full\"")
            resValue("string", "RiPlay_CHROMECAST_APPLICATION_ID", propertyOrEmpty("RiPlay_CHROMECAST_APPLICATION_ID"))
            /*
            resValue("string", "AudioTagInfo_API_KEY", propertyOrEmpty("AudioTagInfo_API_KEY"))
            resValue("string", "RiPlay_LASTFM_API_KEY", propertyOrEmpty("RiPlay_LASTFM_API_KEY"))
            resValue("string", "RiPlay_LASTFM_SECRET", propertyOrEmpty("RiPlay_LASTFM_SECRET"))
            resValue("string", "RiPlay_DISCORD_APPLICATION_ID", propertyOrEmpty("RiPlay_DISCORD_APPLICATION_ID"))
            */
        }
    }

    productFlavors {
        create("foss") {
            minSdk = 23
            dimension = "version"
            //manifestPlaceholders["appName"] = "RiPlay"
            //buildConfigField("String", "BUILD_VARIANT", "\"foss\"")
            resValue("string", "RiPlay_CHROMECAST_APPLICATION_ID", "\"\"")
        }
    }

    sourceSets {
        getByName("full") {
            manifest.srcFile("src/androidFull/AndroidManifest.xml")
            kotlin.directories.add("src/androidFull/kotlin")
        }
        getByName("foss") {
            manifest.srcFile("src/androidFoss/AndroidManifest.xml")
            kotlin.directories.add("src/androidFoss/kotlin")
        }
        all {
            kotlin.directories.add("src/$name/kotlin")
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    output.outputFileName = "RiPlay-${output.baseName}-${variant.outputs.first().versionName.orNull}.apk"
                }
            }
        }
    }

//    sourceSets.all {
//        kotlin.srcDir("src/$name/kotlin")
//    }



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

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    "fullImplementation"(libs.media3.ui)
    "fullImplementation"(libs.media3.cast)
    "fullImplementation"(projects.aypcast)
    "fullImplementation"(projects.chaquopy)

    add("kspAndroid", libs.room.compiler)
//    add("kspDesktop", libs.room.compiler)

    coreLibraryDesugaring(libs.desugaring)
}