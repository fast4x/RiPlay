package it.fast4x.riplay.extensions.chromecast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import it.fast4x.riplay.R

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val riPlayChromecastApplicationID = context.resources.getString(R.string.RiPlay_CHROMECAST_APPLICATION_ID)
        return CastOptions.Builder()
            .setReceiverApplicationId(riPlayChromecastApplicationID)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}