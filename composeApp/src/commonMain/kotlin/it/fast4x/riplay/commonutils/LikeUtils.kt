package it.fast4x.riplay.commonutils

fun setLikeState(likedAt: Long?): Long? {
    val current =
        when (likedAt) {
            -1L -> null
            null -> System.currentTimeMillis()
            else -> -1L
        }
    //println("mediaItem setLikeState: $current")
    return current
}

fun setDisLikeState(likedAt: Long?): Long? {
    val current =
        when (likedAt) {
            -1L -> null
            null -> -1L
            else -> -1L
        }
    return current
}