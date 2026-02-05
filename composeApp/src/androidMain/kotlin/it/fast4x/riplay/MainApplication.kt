package it.fast4x.riplay

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.multidex.MultiDexApplication
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import it.fast4x.riplay.data.DatabaseInitializer
import it.fast4x.riplay.enums.CoilDiskCacheMaxSize
import it.fast4x.riplay.utils.CaptureCrash
import it.fast4x.riplay.utils.FileLoggingTree
import it.fast4x.riplay.extensions.preferences.coilCustomDiskCacheKey
import it.fast4x.riplay.extensions.preferences.coilDiskCacheMaxSizeKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.logDebugEnabledKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.usePlaceholderInImageLoaderKey
import it.fast4x.riplay.commonutils.initializeEnvironment
import it.fast4x.riplay.service.PlayerService
import timber.log.Timber
import java.io.File

class MainApplication : MultiDexApplication(), ImageLoaderFactory {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val receiver = ComponentName(this, PlayerService::class.java)
        val pm = packageManager

        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        Dependencies.init(this)

        /***** CRASH LOG ALWAYS ENABLED *****/
        val dir = filesDir.resolve("logs").also {
            if (it.exists()) return@also
            it.mkdir()
        }
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash(dir.absolutePath))
        /***** CRASH LOG ALWAYS ENABLED *****/

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(logDebugEnabledKey, false)
        if (logEnabled) {
            Timber.plant(FileLoggingTree(File(dir, "RiPlay_log.txt")))
            Timber.d("Log enabled at ${dir.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/
    }

    override fun newImageLoader(): ImageLoader {
        val coilCustomDiskCache = preferences.getInt(coilCustomDiskCacheKey, 128) * 1000 * 1000L
        val coilDiskCacheMaxSize = preferences.getEnum(coilDiskCacheMaxSizeKey,CoilDiskCacheMaxSize.`128MB`)
        val usePlaceholder = preferences.getBoolean(usePlaceholderInImageLoaderKey, true)
        val coilCacheSize = when (coilDiskCacheMaxSize) {
            CoilDiskCacheMaxSize.Custom -> coilCustomDiskCache
            else -> coilDiskCacheMaxSize.bytes
        }


        return ImageLoader.Builder(this)
            .crossfade(true)
            //.allowHardware(if (isAtLeastAndroid8) true else false)
            //.bitmapConfig(if (isAtLeastAndroid8) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888)
            //.networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .error(R.drawable.noimage)
            .fallback(R.drawable.noimage)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizePercent(0.1)
                    .build()
            )
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(
                DiskCache.Builder()
                    .directory(filesDir.resolve("coil"))
                    .maxSizeBytes(
                        if (coilCacheSize == 0L) CoilDiskCacheMaxSize.`128MB`.bytes
                        else coilCacheSize
                    )
                    .build()
            )
            .apply {
                if (usePlaceholder) {
                    placeholder(R.drawable.loader)
                }
            }
            .build()
    }

}

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
        DatabaseInitializer()
        initializeEnvironment() // multiplatform initialization
    }
}