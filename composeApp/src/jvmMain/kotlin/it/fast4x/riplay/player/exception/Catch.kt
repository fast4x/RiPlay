package exception

fun catch(body: () -> Unit) = runCatching { body() }.onFailure { println(it.localizedMessage) }.getOrNull() ?: Unit