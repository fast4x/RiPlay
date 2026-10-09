package it.fast4x.riplay.extensions.appforegroundtracker

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import timber.log.Timber

object AppForegroundTracker {
    @Volatile var isAppInForeground: Boolean = false
        private set

    fun init(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    isAppInForeground = true
                    Timber.d("APP-STATE: app in FOREGROUND")
                }
                override fun onStop(owner: LifecycleOwner) {
                    isAppInForeground = false
                    Timber.d("APP-STATE: app in BACKGROUND")
                }
            }
        )
    }
}