package it.fast4x.riplay.utils

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

fun clearWebViewData(){
    Timber.e("OnlinePlayerCore: clearWebkitData called")
    // Try delete all data cache and cookies
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }.onFailure {
            Timber.e("OnlinePlayerCore: onError clearWebkitData failed: ${it.message}")
        }
    }

}

data class WebViewInfo(
    val provider: String? = null,
    val version: String? = null,
    val code: Long? = null,
    val isWebViewAvailable: Boolean = false,
    val error: String? = null
)

fun getWebViewInfo(context: android.content.Context): WebViewInfo {
    return try {
        if (isAtLeastAndroid8 ) {
            val packageInfo = WebViewCompat.getCurrentWebViewPackage(context)

            if (packageInfo != null) {
                return WebViewInfo(
                    provider = packageInfo.packageName,
                    version = packageInfo.versionName,
                    code = if(isAtLeastAndroid9) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
                    isWebViewAvailable = true
                )
            } else {
                return WebViewInfo(isWebViewAvailable = false)
            }
        } else {
            return WebViewInfo(isWebViewAvailable = false)
        }
    } catch (e: Exception) {
        WebViewInfo(isWebViewAvailable = false, error = e.message)
    }
}