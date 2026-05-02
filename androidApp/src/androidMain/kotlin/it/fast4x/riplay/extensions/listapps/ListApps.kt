package it.fast4x.riplay.extensions.listapps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import it.fast4x.riplay.data.models.ExternalApp

fun listApps(
    context: Context,
    includeSystemApps: Boolean = false
): List<DeviceApp> {

    // searching main activities labeled to be launchers of the apps
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

    if (!includeSystemApps) {
        val resolvedInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            pm.queryIntentActivities(mainIntent, 0)
        }



        return resolvedInfos.map { info ->
            val app = DeviceApp(
                packageName = info.activityInfo.packageName,
                activityName = info.activityInfo.name,
                //iconDrawable = info.loadIcon(pm),
                appName = info.loadLabel(pm).toString(),
                isSystemApp = false,
            )
            app
        }
    } else {
        val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            pm.getInstalledApplications(0)
        }

        return appInfos.map { appInfo ->
            val app = DeviceApp(
                packageName = appInfo.packageName,
                activityName = appInfo.name,
                //iconDrawable = appInfo.loadIcon(pm),
                appName = appInfo.loadLabel(pm).toString(),
                isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            )
            app
        }
    }
}

data class DeviceApp (
    val packageName: String,
    val activityName: String,
    val appName: String,
    val isSystemApp: Boolean,
)

fun DeviceApp.toExternalApp(): ExternalApp =
    ExternalApp(
        packageName = this.packageName,
        activityName = this.activityName,
        appName = this.appName,
        isSystemApp = this.isSystemApp
    )

fun ExternalApp.toDeviceApp(): DeviceApp =
    DeviceApp(
        packageName = this.packageName,
        activityName = this.activityName,
        appName = this.appName.toString(),
        isSystemApp = this.isSystemApp
    )

val DeviceApp.componentName: ComponentName
    get() = ComponentName(packageName, activityName)