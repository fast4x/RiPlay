package it.fast4x.riplay.extensions.fastshare

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.ShareType
import it.fast4x.riplay.enums.UrlType
import it.fast4x.riplay.models.Album
import it.fast4x.riplay.models.Artist
import it.fast4x.riplay.models.Playlist
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.asSong

fun fastShare(
    content: Any,
    typeOfShare: ShareType = ShareType.Classic,
    typeOfUrl: UrlType = UrlType.Youtube
) {

    if (content.toString().isEmpty()) {
        SmartMessage(message = "No content to share!", type = PopupType.Error, context = context())
        return
    }
    when (content) {
        is MediaItem -> {
            when (typeOfShare) {
                ShareType.Classic -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            classicShare(content.asSong.shareYTUrl.toString(), context())
                        }

                        UrlType.YoutubeMusic -> {
                            classicShare(content.asSong.shareYTMUrl.toString(), context())
                        }

                    }
                }

                ShareType.Direct -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            directShare(
                                content.asSong.shareYTUrl.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }

                        UrlType.YoutubeMusic -> {
                            directShare(
                                content.asSong.shareYTMUrl.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }
                    }
                }
            }
        }

        is Playlist -> {
            when (typeOfShare) {
                ShareType.Classic -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            classicShare(
                                if (!content.isPodcast) content.shareYTUrl.toString()
                                else content.shareYTUrlAsPodcast.toString(), context()
                            )
                        }

                        UrlType.YoutubeMusic -> {
                            classicShare(
                                if (!content.isPodcast) content.shareYTMUrl.toString()
                                else content.shareYTMUrlAsPodcast.toString(), context()
                            )
                        }
                    }
                }

                ShareType.Direct -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            directShare(
                                if (!content.isPodcast) content.shareYTUrl.toString()
                                else content.shareYTUrlAsPodcast.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }

                        UrlType.YoutubeMusic -> {
                            directShare(
                                if (!content.isPodcast) content.shareYTMUrl.toString()
                                else content.shareYTMUrlAsPodcast.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }
                    }
                }

            }
        }

        is Album -> {
            when (typeOfShare) {
                ShareType.Classic -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            classicShare(content.shareYTUrl.toString(), context())
                        }

                        UrlType.YoutubeMusic -> {
                            classicShare(content.shareYTMUrl.toString(), context())
                        }
                    }
                }

                ShareType.Direct -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            directShare(
                                content.shareYTUrl.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }

                        UrlType.YoutubeMusic -> {
                            directShare(
                                content.shareYTMUrl.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }
                    }
                }
            }
        }

        is Artist -> {
            when (typeOfShare) {
                ShareType.Classic -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            classicShare(content.shareYTUrl.toString(), context())
                        }

                        UrlType.YoutubeMusic -> {
                            classicShare(content.shareYTMUrl.toString(), context())
                        }
                    }
                }

                ShareType.Direct -> {
                    when (typeOfUrl) {
                        UrlType.Youtube -> {
                            directShare(
                                content.shareYTUrl.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }

                        UrlType.YoutubeMusic -> {
                            directShare(
                                content.shareYTMUrl.toString(),
                                ComponentName(
                                    "com.junkfood.seal",
                                    "com.junkfood.seal.MainActivity"
                                ),
                                context()
                            )
                        }
                    }

                }

            }
        }
    }
}

fun directShare(content: String, componentName: ComponentName, context: Context) {
    if (content.isEmpty() || componentName.packageName.isEmpty()) {
        SmartMessage(message = "No content to share!", type = PopupType.Error, context = context)
        return
    }

    try {
        val intent = Intent().apply {
//            component = ComponentName(
//                "com.junkfood.seal",
//                "com.junkfood.seal.MainActivity"
//            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            component = componentName
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                content
            )
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        SmartMessage(
            "App is not installed! \n${e.localizedMessage}",
            PopupType.Error,
            context = context
        )
    }
}

fun classicShare(content: String, context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            content
        )
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)


}