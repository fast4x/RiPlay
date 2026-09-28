package it.fast4x.androidyoutubeplayer.cast.io.youtube

import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.framework.SessionManager
import it.fast4x.androidyoutubeplayer.cast.io.infrastructure.ChromecastCommunicationChannel
import it.fast4x.androidyoutubeplayer.cast.utils.JSONUtils

/**
 * Communication channel used to exchange messages with the YouTube Chromecast receiver.
 */
internal class ChromecastYouTubeIOChannel(
  private val sessionManager: SessionManager
) : ChromecastCommunicationChannel {
  override val namespace get() = "urn:x-cast:com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.communication"

  override val observers = HashSet<ChromecastCommunicationChannel.ChromecastChannelObserver>()

  override fun sendMessage(message: String) {
    try {
      sessionManager.currentCastSession?.sendMessage(namespace, message)
//                    .setResultCallback {
//                        if(it.isSuccess)
//                            Log.d(this.javaClass.simpleName, "message sent")
//                        else
//                            Log.e(this.javaClass.simpleName, "failed, can't send message")
//                    }

    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }

  override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
    val parsedMessage = JSONUtils.parseMessageFromReceiverJson(message)
    observers.forEach { it.onMessageReceived(parsedMessage) }
  }
}