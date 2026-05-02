package it.fast4x.riplay.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.SecureConfig
import it.fast4x.riplay.utils.appContext

class CastOptionsProvider : OptionsProvider {
//    override fun getCastOptions(appContext: Context): CastOptions {
//        val youTubeReceiverAppId = SecureConfig.getApiKey(appContext().resources.getString(R.string.RiPlay_CHROMECAST_APPLICATION_ID))
//        return CastOptions.Builder()
//            .setReceiverApplicationId(youTubeReceiverAppId)
//            .build()
//    }

    override fun getCastOptions(context: Context): CastOptions {
        val youTubeReceiverAppId = SecureConfig.getApiKey(appContext().resources.getString(R.string.RiPlay_CHROMECAST_APPLICATION_ID))
        return CastOptions.Builder()
            .setReceiverApplicationId(youTubeReceiverAppId)
            .setCastMediaOptions(
                CastMediaOptions.Builder()
                    .setNotificationOptions(
                        NotificationOptions.Builder()
                            .setTargetActivityClassName("it.fast4x.riplay.MainActivity")
                            .build()
                    )
                    .build()
            )
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context) = null
}