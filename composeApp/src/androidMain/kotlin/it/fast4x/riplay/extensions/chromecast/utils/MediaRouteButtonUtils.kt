package it.fast4x.riplay.extensions.chromecast.utils

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.chromecast.ChromeCastActivity.MediaRouteButtonContainer
import timber.log.Timber

object MediaRouteButtonUtils {
    fun initMediaRouteButton(context: Context): MediaRouteButton {
        val mediaRouteButton = MediaRouteButton(context)
        CastButtonFactory.setUpMediaRouteButton(context, mediaRouteButton)

        return mediaRouteButton
    }

    fun addMediaRouteButtonToPlayerUi(
        mediaRouteButton: MediaRouteButton, tintColor: Int,
        disabledContainer: MediaRouteButtonContainer?, activatedContainer: MediaRouteButtonContainer
    ) {
        setMediaRouterButtonTint(mediaRouteButton, tintColor)

        disabledContainer?.removeMediaRouteButton(mediaRouteButton)

        if (mediaRouteButton.parent != null) {
            return
        }

        activatedContainer.addMediaRouteButton(mediaRouteButton)
    }

    private fun setMediaRouterButtonTint(mediaRouterButton: MediaRouteButton, color: Int) {
        val castContext: ContextThemeWrapper =
            ContextThemeWrapper(mediaRouterButton.context, androidx.mediarouter.R.style.Theme_MediaRouter)
        val styledAttributes = castContext.obtainStyledAttributes(
            null,
            androidx.mediarouter.R.styleable.MediaRouteButton,
            androidx.mediarouter.R.attr.mediaRouteButtonStyle,
            0
        )
        val drawable =
            styledAttributes.getDrawable(androidx.mediarouter.R.styleable.MediaRouteButton_externalRouteEnabledDrawable)

        if (drawable == null) {
            Timber.e("MediaRouteButtonUtils can't apply tint to MediaRouteButton")
            return
        }

        styledAttributes.recycle()
        DrawableCompat.setTint(
            drawable,
            ContextCompat.getColor(mediaRouterButton.context, color)
        )

        mediaRouterButton.setRemoteIndicatorDrawable(drawable)
    }
}
