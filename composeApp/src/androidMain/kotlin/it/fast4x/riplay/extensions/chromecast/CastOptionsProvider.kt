package it.fast4x.riplay.extensions.chromecast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

const val RiPlay_APPLICATION_ID = "87961733"
const val PierFrancesco_APPLICATION_ID = "C5CBE8CA"

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {

        return CastOptions.Builder()
            .setReceiverApplicationId(RiPlay_APPLICATION_ID)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}