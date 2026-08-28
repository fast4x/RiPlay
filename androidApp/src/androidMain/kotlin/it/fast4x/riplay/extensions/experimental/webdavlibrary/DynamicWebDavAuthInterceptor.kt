package it.fast4x.riplay.extensions.experimental.webdavlibrary

import okhttp3.Interceptor
import okhttp3.Response

class DynamicWebDavAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // Chiediamo al provider se questo URL ha bisogno di autenticazione
        val authHeader = WebDavCredentialsProvider.getAuthHeaderForUrl(url)

        val newRequest = if (authHeader != null) {
            request.newBuilder()
                .header("Authorization", authHeader)
                .build()
        } else {
            request // Richiesta normale, prosegue senza auth
        }

        return chain.proceed(newRequest)
    }
}