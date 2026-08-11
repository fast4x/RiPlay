package it.fast4x.environment.models.responses.userchannel

data class UserChannelProfilePage(
    val name: String,
    val handle: String?,
    val channelId: String?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscribersText: String?,
    val videosCountText: String?,
    val description: String?,
    val sections: List<UserChannelSection>
)

sealed class UserChannelSection {
    abstract val title: String?

    data class FeaturedVideo(
        override val title: String?,
        val videoId: String?,
        val description: String?
    ) : UserChannelSection()

    data class PlaylistSection(
        override val title: String?,
        val playlists: List<YTPlaylist>
    ) : UserChannelSection()

    data class VideoSection(
        override val title: String?,
        val videos: List<YTVideo>
    ) : UserChannelSection()

    data class ShortsSection(
        override val title: String?,
        val shorts: List<YTShort>
    ) : UserChannelSection()
}

data class YTPlaylist(
    val id: String,
    val title: String,
    val thumbnailUrl: String?
)

data class YTVideo(
    val id: String,
    val title: String,
    val thumbnailUrl: String?
)

data class YTShort(
    val videoId: String,
    val title: String,
    val views: String?,
    val thumbnailUrl: String?
)