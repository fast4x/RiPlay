package it.fast4x.riplay.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import kotlin.coroutines.resumeWithException


suspend fun Call.executeAsync(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response) { cause, _, _ ->
                    call.cancel()
                }
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    })
}



fun <T> Flow<T>.collect(scope: CoroutineScope, action: suspend (value: T) -> Unit) {
    scope.launch {
        collect(action)
    }
}

fun <T> Flow<T>.collectLatest(scope: CoroutineScope, action: suspend (value: T) -> Unit) {
    scope.launch {
        collectLatest(action)
    }
}

val SilentHandler = CoroutineExceptionHandler { _, _ -> }