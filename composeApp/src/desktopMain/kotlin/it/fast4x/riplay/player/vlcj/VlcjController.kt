package vlcj

import com.sun.jna.NativeLibrary.addSearchPath
import exception.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import player.PlayerController
import player.PlayerState
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.nio.file.Paths
import kotlin.io.path.pathString

class VlcjController : PlayerController {

    init {
        //println("desktop VlcjController init ${System.setProperty("jna.library.path", System.getProperty("user.dir"))}")
        addSearchPath(
            RuntimeUtil.getLibVlcLibraryName(),
            Paths.get(System.getProperty("user.dir"), "lib", "libvlc.dll").pathString
        )
        NativeDiscovery().discover()
    }

    internal val factory by lazy { MediaPlayerFactory() }

    internal var player: EmbeddedMediaPlayer? = null
        private set

    private val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            catch { mediaPlayer.audio().setVolume(50) }
            _state.update { it.copy(duration = mediaPlayer.status().length()) }
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = true) }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun muted(mediaPlayer: MediaPlayer, muted: Boolean) {
            _state.update { it.copy(isMuted = muted) }
        }

        override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
            _state.update { it.copy(volume = volume) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            _state.update { it.copy(timestamp = newTime) }
        }
    }

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun load(url: String) = catch {
        player = factory.mediaPlayers()?.newEmbeddedMediaPlayer()?.apply {
            events()?.addMediaPlayerEventListener(stateListener)
            media()?.prepare(url)
        }
    }

    override fun play() = catch {
        player?.controls()?.play()
    }

    override fun pause() = catch {
        player?.controls()?.setPause(true)
    }

    override fun stop() = catch {
        player?.controls()?.stop()
    }

    override fun dispose() = catch {
        player?.run {
            controls().stop()
            events().removeMediaPlayerEventListener(stateListener)
            release()
        }
    }

    override fun seekTo(timestamp: Long) = catch {
        player?.controls()?.setTime(timestamp)
    }

    override fun setVolume(value: Float) = catch {
        player?.audio()?.setVolume((value * 100).toInt().coerceIn(0..100))
    }

    override fun toggleSound() = catch {
        player?.audio()?.mute()
    }
}