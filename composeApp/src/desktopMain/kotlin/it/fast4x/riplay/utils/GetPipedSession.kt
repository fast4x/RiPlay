package it.fast4x.riplay.utils

import io.ktor.http.Url
import it.fast4x.riplay.models.PipedSession

/*
fun getPipedSession(): PipedSession {
    //val context = LocalContext.current
    val pipedSession = PipedSession(
        instanceName = "",
        apiBaseUrl = Url(""),
        token = "",
        username = ""
    )
    TODO("Implement getPipedSession in desktop version")
    /*
    if (context.preferences.getBoolean(isPipedEnabledKey, false)) {
        runCatching {
            with(context.encryptedPreferences) {
                pipedSession.username = getString(pipedUsernameKey, "").toString()
                pipedSession.instanceName = getString(pipedInstanceNameKey, "").toString()
                pipedSession.apiBaseUrl = Url(getString(pipedApiBaseUrlKey, "").toString())
                pipedSession.token = getString(pipedApiTokenKey, "").toString()
            }
        }.onFailure {
            //Timber.e("GetPipedSession get encryptedPreferences error ${it.stackTraceToString()}")
        }
    }

     */

    return pipedSession

}
*/

fun getPipedSession() = PipedSession(
    instanceName = "",
    apiBaseUrl = Url(""),
    token = "",
    username = ""
)