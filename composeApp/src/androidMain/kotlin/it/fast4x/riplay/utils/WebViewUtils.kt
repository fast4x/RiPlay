package it.fast4x.riplay.utils

import android.webkit.CookieManager
import android.webkit.WebStorage
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