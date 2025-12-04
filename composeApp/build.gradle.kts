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
}


fun Project.propertyOrEmpty(name: String): String {
    return (findProperty(name) as? String) ?: ""
}
val generatedSrcDir = layout.buildDirectory.dir("generated/kotlin/config").get()

val generateEnvironmentConfig by tasks.registering {
    group = "build"
    description = "Genera un file Kotlin con le configurazioni di ambiente."

    val environmentPropertyNames = listOf(
        "CrQ0JjAXgv", "hNpBzzAn7i", "lEi9YM74OL", "C0ZR993zmk", "w3TFBFL74Y", "mcchaHCWyK",
        "L2u4JNdp7L", "sqDlfmV4Mt", "WpLlatkrVv", "1zNshDpFoh", "mPVWVuCxJz", "auDsjnylCZ",
        "AW52cvJIJx", "0RGAyC1Zqu", "4Fdmu9Jkax", "kuSdQLhP8I", "QrgDKwvam1", "wLwNESpPtV",
        "JJUQaehRFg", "i7WX2bHV6R", "XpiuASubrV", "lOlIIVw38L", "mtcR0FhFEl", "DTihHAFaBR",
        "a4AcHS8CSg", "krdLqpYLxM", "ye6KGLZL7n", "ec09m20YH5", "LDRlbOvbF1", "EEqX0yizf2",
        "i3BRhLrV1v", "MApdyHLMyJ", "hizI7yLjL4", "rLoZP7BF4c", "nza34sU88C", "dwbUvjWUl3",
        "fqqhBZd0cf", "9sZKrkMg8p", "aQpNCVOe2i", "XNl2TKXLlB", "yNjbjspY8v", "eZueG672lt",
        "WkUFhXtC3G", "z4Xe47r8Vs", "RiPlay_CHROMECAST_APPLICATION_ID", "Ayp_CHROMECAST_APPLICATION_ID",
        "AudioTagInfo_API_KEY"
    )
    inputs.properties(environmentPropertyNames.associateWith { propertyOrEmpty(it) })

    val outputFile = generatedSrcDir.file("it/fast4x/riplay/config/EnvironmentConfig.kt")
    outputs.file(outputFile)

    doLast {
        val file = outputFile.asFile
        file.parentFile.mkdirs()

        val props = inputs.properties

        file.writeText(
            """
            // GENERATED FILE - DO NOT MODIFY
            package it.fast4x.riplay.config

            object EnvironmentConfig {
                const val env_CrQ0JjAXgv = "${props["CrQ0JjAXgv"]}"
                const val env_hNpBzzAn7i = "${props["hNpBzzAn7i"]}"
                const val env_lEi9YM74OL = "${props["lEi9YM74OL"]}"
                const val env_C0ZR993zmk = "${props["C0ZR993zmk"]}"
                const val env_w3TFBFL74Y = "${props["w3TFBFL74Y"]}"
                const val env_mcchaHCWyK = "${props["mcchaHCWyK"]}"
                const val env_L2u4JNdp7L = "${props["L2u4JNdp7L"]}"
                const val env_sqDlfmV4Mt = "${props["sqDlfmV4Mt"]}"
                const val env_WpLlatkrVv = "${props["WpLlatkrVv"]}"
                const val env_1zNshDpFoh = "${props["1zNshDpFoh"]}"
                const val env_mPVWVuCxJz = "${props["mPVWVuCxJz"]}"
                const val env_auDsjnylCZ = "${props["auDsjnylCZ"]}"
                const val env_AW52cvJIJx = "${props["AW52cvJIJx"]}"
                const val env_0RGAyC1Zqu = "${props["0RGAyC1Zqu"]}"
                const val env_4Fdmu9Jkax = "${props["4Fdmu9Jkax"]}"
                const val env_kuSdQLhP8I = "${props["kuSdQLhP8I"]}"
                const val env_QrgDKwvam1 = "${props["QrgDKwvam1"]}"
                const val env_wLwNESpPtV = "${props["wLwNESpPtV"]}"
                const val env_JJUQaehRFg = "${props["JJUQaehRFg"]}"
                const val env_i7WX2bHV6R = "${props["i7WX2bHV6R"]}"
                const val env_XpiuASubrV = "${props["XpiuASubrV"]}"
                const val env_lOlIIVw38L = "${props["lOlIIVw38L"]}"
                const val env_mtcR0FhFEl = "${props["mtcR0FhFEl"]}"
                const val env_DTihHAFaBR = "${props["DTihHAFaBR"]}"
                const val env_a4AcHS8CSg = "${props["a4AcHS8CSg"]}"
                const val env_krdLqpYLxM = "${props["krdLqpYLxM"]}"
                const val env_ye6KGLZL7n = "${props["ye6KGLZL7n"]}"
                const val env_ec09m20YH5 = "${props["ec09m20YH5"]}"
                const val env_LDRlbOvbF1 = "${props["LDRlbOvbF1"]}"
                const val env_EEqX0yizf2 = "${props["EEqX0yizf2"]}"
                const val env_i3BRhLrV1v = "${props["i3BRhLrV1v"]}"
                const val env_MApdyHLMyJ = "${props["MApdyHLMyJ"]}"
                const val env_hizI7yLjL4 = "${props["hizI7yLjL4"]}"
                const val env_rLoZP7BF4c = "${props["rLoZP7BF4c"]}"
                const val env_nza34sU88C = "${props["nza34sU88C"]}"
                const val env_dwbUvjWUl3 = "${props["dwbUvjWUl3"]}"
                const val env_fqqhBZd0cf = "${props["fqqhBZd0cf"]}"
                const val env_9sZKrkMg8p = "${props["9sZKrkMg8p"]}"
                const val env_aQpNCVOe2i = "${props["aQpNCVOe2i"]}"
                const val env_XNl2TKXLlB = "${props["XNl2TKXLlB"]}"
                const val env_yNjbjspY8v = "${props["yNjbjspY8v"]}"
                const val env_eZueG672lt = "${props["eZueG672lt"]}"
                const val env_WkUFhXtC3G = "${props["WkUFhXtC3G"]}"
                const val env_z4Xe47r8Vs = "${props["z4Xe47r8Vs"]}"
                
                const val RiPlay_CHROMECAST_APPLICATION_ID = "${props["RiPlay_CHROMECAST_APPLICATION_ID"]}"
                const val Ayp_CHROMECAST_APPLICATION_ID = "${props["Ayp_CHROMECAST_APPLICATION_ID"]}"
                const val AudioTagInfo_API_KEY = "${props["AudioTagInfo_API_KEY"]}"
            }
            """.trimIndent()
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateEnvironmentConfig)
}

/*
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
 */

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

        val commonMain by getting {
            kotlin.srcDir(generatedSrcDir)

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(project(":environment"))
                implementation(project(":kugou"))
                implementation(project(":lrclib"))
                implementation(project(":audiotaginfo"))

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


                implementation(
                    fileTree(
                        mapOf(
                            "dir" to "libs",
                            "include" to listOf("*.aar", "*.jar")
                        )
                    )
                )
            }
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)

            val fxSuffix = "win"
            implementation("org.openjfx:javafx-base:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-graphics:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-controls:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-swing:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-web:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-media:21.0.5:${fxSuffix}")

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
            //implementation(libs.room.backup)
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
            implementation(libs.androidyoutubeplayer.chromecast.sender)
            implementation(libs.androidx.mediarouter)
            implementation(libs.glance.widgets)
            implementation(libs.kizzy.rpc)
            implementation(libs.gson)
            implementation(libs.hypnoticcanvas)
            implementation(libs.hypnoticcanvas.shaders)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.multidex)
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

    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "it.fast4x.riplay"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        val versionMajor = libs.versions.releaseVersionMajor.get().toInt()
        val versionMinor = libs.versions.releaseVersionMinor.get().toInt()
        versionCode = libs.versions.releaseVersionCode.get().toInt()
        val releaseSuffix = libs.versions.releaseVersionSuffix.get()
        versionName = "$versionMajor.$versionMinor.$versionCode$releaseSuffix"

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

    packaging {
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
            multiDexKeepProguard = File("multidex-config.txt")
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

        version = "0.0.1"
        group = "riplay"

        nativeDistributions {
            vendor = "RiPlay.DesktopApp"
            description = "RiPlay Desktop Music Player"

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

    add("kspAndroid", libs.room.compiler)
    add("ksp", libs.room.compiler)

    coreLibraryDesugaring(libs.desugaring)
}


