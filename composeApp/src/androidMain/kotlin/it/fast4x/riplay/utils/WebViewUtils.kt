package it.fast4x.riplay.utils

import android.webkit.CookieManager
import android.webkit.WebStorage
import timber.log.Timber

fun clearWebViewData(){
    // Try delete all data cache and cookies
    runCatching {
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }.onFailure {
        Timber.e("OnlinePlayerCore: onError clearWebkitData failed: ${it.message}")
    }
}