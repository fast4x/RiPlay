package it.fast4x.riplay.extensions.chromecast.utils

import java.util.Random


object VideoIdsProvider {
    private val videoIds =
        arrayOf<String>("6JYIGclVQdw", "LvetJ9U_tVY", "S0Q4gqBUs7c", "zOa-rSM4nms")
    private val liveVideoIds = arrayOf<String>("hHW1oY26kxQ")
    private val random = Random()

    val nextVideoId: String
        get() = videoIds[random.nextInt(videoIds.size)]

    val nextLiveVideoId: String
        get() = liveVideoIds[random.nextInt(liveVideoIds.size)]
}