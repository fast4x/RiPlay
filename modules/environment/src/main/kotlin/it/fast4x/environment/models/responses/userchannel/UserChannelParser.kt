package it.fast4x.environment.models.responses.userchannel

import kotlinx.serialization.json.Json

val ytJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

fun parseUserChannelProfile(json: String): UserChannelProfilePage {
    val root = ytJson.decodeFromString<UserChannelResponse>(json)

    // --- 1. Estrazione Info Profilo ---
    val headerVm = root.header?.pageHeaderRenderer?.content?.pageHeaderViewModel
    val metaRenderer = root.metadata?.channelMetadataRenderer

    val name = headerVm?.title?.dynamicTextViewModel?.text?.content
        ?: metaRenderer?.title
        ?: "Sconosciuto"

    val avatarUrl = headerVm?.image?.decoratedAvatarViewModel?.avatar?.avatarViewModel?.image?.sources?.lastOrNull()?.url
    val bannerUrl = headerVm?.banner?.imageBannerViewModel?.image?.sources?.lastOrNull()?.url

    // Estraiamo i metadati (Handle, Subscribers, Videos) appiattendo le righe
    val flatMetadata = headerVm?.metadata?.contentMetadataViewModel?.metadataRows
        ?.flatMap { it.metadataParts ?: emptyList() }
        ?.mapNotNull { it.text?.content }
        ?: emptyList()

    // Cerchiamo dinamicamente le stringhe per essere resilienti ai cambiamenti di YouTube
    val handle = flatMetadata.firstOrNull { it.startsWith("@") }
    val subsText = flatMetadata.firstOrNull { it.contains("subscriber", ignoreCase = true) }
    val videosText = flatMetadata.firstOrNull { it.contains("video", ignoreCase = true) }

    // La descrizione nell'header è troncata, preferiamo quella completa dai metadata se presente
    val description = metaRenderer?.description
        ?: headerVm?.description?.descriptionPreviewViewModel?.description?.content
        ?: ""

    val channelId = metaRenderer?.externalId

    // --- 2. Estrazione Sezioni (La logica di prima) ---
    val sections = mutableListOf<UserChannelSection>()
    val selectedTab = root.contents?.twoColumnBrowseResults?.tabs
        ?.firstOrNull { it.tabRenderer?.selected == true }?.tabRenderer

    val sectionList = selectedTab?.content?.sectionListRenderer?.contents ?: emptyList()

    for (sectionWrapper in sectionList) {
        val itemSection = sectionWrapper.itemSectionRenderer?.contents?.firstOrNull() ?: continue

        itemSection.channelVideoPlayerRenderer?.let { featuredRenderer ->
            val title = featuredRenderer.title?.text
            val desc = featuredRenderer.description?.text
            sections.add(UserChannelSection.FeaturedVideo(title, featuredRenderer.videoId, desc))
        }

        itemSection.shelfRenderer?.let { shelf ->
            val shelfTitle = shelf.title?.text ?: ""
            val items = shelf.content?.horizontalListRenderer?.items ?: emptyList()

            val extractedItems = items.mapNotNull { it.lockupViewModel }

            if (extractedItems.isNotEmpty()) {
                val contentType = extractedItems.first().contentType
                when (contentType) {
                    "LOCKUP_CONTENT_TYPE_PLAYLIST" -> {
                        val playlists = extractedItems.map {
                            YTPlaylist(
                                id = it.contentId ?: "",
                                title = it.metadata?.lockupMetadataViewModel?.title?.content ?: "",
                                thumbnailUrl = it.contentImage?.collectionThumbnailViewModel?.primaryThumbnail?.thumbnailViewModel?.image?.sources?.lastOrNull()?.url
                                    ?: it.contentImage?.thumbnailViewModel?.image?.sources?.lastOrNull()?.url
                            )
                        }
                        sections.add(UserChannelSection.PlaylistSection(shelfTitle, playlists))
                    }
                    "LOCKUP_CONTENT_TYPE_VIDEO" -> {
                        val videos = extractedItems.map {
                            YTVideo(
                                id = it.contentId ?: "",
                                title = it.metadata?.lockupMetadataViewModel?.title?.content ?: "",
                                thumbnailUrl = it.contentImage?.thumbnailViewModel?.image?.sources?.lastOrNull()?.url
                            )
                        }
                        sections.add(UserChannelSection.VideoSection(shelfTitle, videos))
                    }
                }
            }
        }

        itemSection.reelShelfRenderer?.let { reelShelf ->
            val shorts = reelShelf.items?.mapNotNull { item ->
                val shortVm = item.shortsLockupViewModel ?: return@mapNotNull null
                YTShort(
                    videoId = shortVm.onTap?.innertubeCommand?.reelWatchEndpoint?.videoId ?: "",
                    title = shortVm.overlayMetadata?.primaryText?.content ?: "",
                    views = shortVm.overlayMetadata?.secondaryText?.content,
                    thumbnailUrl = shortVm.thumbnailViewModel?.image?.sources?.lastOrNull()?.url
                )
            } ?: emptyList()

            if (shorts.isNotEmpty()) {
                sections.add(UserChannelSection.ShortsSection("Shorts", shorts))
            }
        }
    }

    return UserChannelProfilePage(
        name = name,
        handle = handle,
        channelId = channelId,
        avatarUrl = avatarUrl,
        bannerUrl = bannerUrl,
        subscribersText = subsText,
        videosCountText = videosText,
        description = description,
        sections = sections
    )
}