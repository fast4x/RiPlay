package it.fast4x.riplay.models

import io.ktor.http.Url
import it.fast4x.piped.models.authenticatedWith

data class PipedSession(
    var instanceName: String,
    var apiBaseUrl: Url,
    var token: String,
    var username: String
) {

    fun toApiSession() = apiBaseUrl authenticatedWith token

    fun serialize() = "$instanceName|$apiBaseUrl|$token|$username"
    companion object {
        fun deserialize(serialized: String) = serialized.split("|").let {
            PipedSession(it[0], Url(it[1]), it[2], it[3])
        }
    }
}

val String.toPipedSession: PipedSession
    get() = this.split("|").let {
        PipedSession(it[0], Url(it[1]), it[2], it[3])
    }

