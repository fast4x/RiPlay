package it.fast4x.riplay.extensions.experimental.cast.miracast

import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class MiracastViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRouter = MediaRouter.getInstance(application)
    val routeSelector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
        .build()

    // Stato: Route correntemente selezionata
    private val _selectedRoute = MutableStateFlow<MediaRouter.RouteInfo?>(null)
    val selectedRoute: StateFlow<MediaRouter.RouteInfo?> = _selectedRoute

    private var currentPresentation: CastPresentation? = null
    private val displayManager = application.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val callback = object : MediaRouter.Callback() {
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
            Log.d("Miracast", "Selected: ${route.name}")
            _selectedRoute.value = route
            updatePresentation(route)
        }

        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
            Log.d("Miracast", "Unselected")
            _selectedRoute.value = null
            updatePresentation(null)
        }

        override fun onRoutePresentationDisplayChanged(
            router: MediaRouter,
            route: MediaRouter.RouteInfo
        ) {
            updatePresentation(route)
        }
    }

    init {
        // Inizializza il callback
        mediaRouter.addCallback(
            routeSelector,
            callback,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
        )
    }

    private fun updatePresentation(route: MediaRouter.RouteInfo?) {
        // Chiudi la vecchia presentation se la route cambia o rimane
        if (currentPresentation != null && currentPresentation?.display != route?.presentationDisplay) {
            currentPresentation?.dismiss()
            currentPresentation = null
        }

        // Apri nuova presentation se la route ha un display valido
        route?.presentationDisplay?.let {
            currentPresentation = CastPresentation(getApplication(), it)
            currentPresentation?.show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRouter.removeCallback(callback)
        currentPresentation?.dismiss()
    }
}