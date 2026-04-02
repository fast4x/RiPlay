package it.fast4x.riplay.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val youTubeReceiverAppId = "CC1AD845"
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
//        return CastOptions.Builder()
//            .setReceiverApplicationId("CC1AD845") // YouTube Receiver ID
//            .build()
    }
    override fun getAdditionalSessionProviders(context: Context) = null
}