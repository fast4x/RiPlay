package vlcj

import player.PlayerController
import player.component.ComponentRenderer
import java.awt.Canvas

class VlcjComponentController constructor(
    private val controller: VlcjController = VlcjController(),
) : ComponentRenderer, PlayerController by controller {

    override val component by lazy { Canvas() }

    private val surface by lazy { controller.factory.videoSurfaces().newVideoSurface(component) }

    override fun load(url: String) {
        runCatching {
            controller.load(url)
            controller.player?.videoSurface()?.set(surface)
        }.onFailure {
            it.printStackTrace()

        }
    }
}