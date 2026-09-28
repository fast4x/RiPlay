package it.fast4x.environment.models.responses.userchannel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserChannelResponse(
    val contents: Contents? = null,
    val header: Header? = null,       // <-- NUOVO
    val metadata: ChannelMetadata? = null
)


@Serializable
data class Contents(
    @SerialName("twoColumnBrowseResultsRenderer") val twoColumnBrowseResults: TwoColumnBrowseResults? = null
)

@Serializable
data class TwoColumnBrowseResults(
    val tabs: List<TabWrapper>? = null
)

@Serializable
data class TabWrapper(
    @SerialName("tabRenderer") val tabRenderer: TabRenderer? = null
)

@Serializable
data class TabRenderer(
    val title: String? = null,
    val selected: Boolean? = false,
    val content: TabContent? = null
)

@Serializable
data class TabContent(
    @SerialName("sectionListRenderer") val sectionListRenderer: SectionListRenderer? = null
)

@Serializable
data class SectionListRenderer(
    val contents: List<SectionItemWrapper>? = null
)

@Serializable
data class SectionItemWrapper(
    @SerialName("itemSectionRenderer") val itemSectionRenderer: ItemSectionRenderer? = null
)

@Serializable
data class ItemSectionRenderer(
    val contents: List<ItemContentWrapper>? = null
)

// Questo wrapper contiene i vari tipi di sezioni possibili (Shelf per video/playlists, ReelShelf per shorts)
@Serializable
data class ItemContentWrapper(
    @SerialName("shelfRenderer") val shelfRenderer: ShelfRenderer? = null,
    @SerialName("reelShelfRenderer") val reelShelfRenderer: ReelShelfRenderer? = null,
    @SerialName("channelVideoPlayerRenderer") val channelVideoPlayerRenderer: ChannelVideoPlayerRenderer? = null
)

// --- MODELLI PER VIDEO E PLAYLIST (Contenuti in ShelfRenderer) ---
@Serializable
data class ShelfRenderer(
    val title: TitleText? = null,
    val content: ShelfContent? = null
)

@Serializable
data class TitleText(
    val runs: List<RunText>? = null
) {
    val text: String? get() = runs?.joinToString("") { it.text ?: "" }
}

@Serializable
data class RunText(val text: String? = null)

@Serializable
data class ShelfContent(
    @SerialName("horizontalListRenderer") val horizontalListRenderer: HorizontalListRenderer? = null
)

@Serializable
data class HorizontalListRenderer(
    val items: List<HorizontalItemWrapper>? = null
)

@Serializable
data class HorizontalItemWrapper(
    @SerialName("lockupViewModel") val lockupViewModel: LockupViewModel? = null
)

@Serializable
data class LockupViewModel(
    @SerialName("contentId") val contentId: String? = null,
    @SerialName("contentType") val contentType: String? = null,
    val metadata: LockupMetadata? = null,
    @SerialName("contentImage") val contentImage: LockupImage? = null
)

@Serializable
data class LockupMetadata(
    @SerialName("lockupMetadataViewModel") val lockupMetadataViewModel: LockupMetadataViewModel? = null
)

@Serializable
data class LockupMetadataViewModel(
    val title: TitleContent? = null
)

@Serializable
data class TitleContent(val content: String? = null)

@Serializable
data class LockupImage(
    @SerialName("collectionThumbnailViewModel") val collectionThumbnailViewModel: CollectionThumbnailViewModel? = null, // Per playlist
    @SerialName("thumbnailViewModel") val thumbnailViewModel: ThumbnailViewModel? = null // Per video normali
)

@Serializable
data class CollectionThumbnailViewModel(
    @SerialName("primaryThumbnail") val primaryThumbnail: PrimaryThumbnail? = null
)

@Serializable
data class PrimaryThumbnail(
    @SerialName("thumbnailViewModel") val thumbnailViewModel: ThumbnailViewModel? = null
)

@Serializable
data class ThumbnailViewModel(
    val image: ThumbnailImage? = null
)

@Serializable
data class ThumbnailImage(
    val sources: List<ThumbnailSource>? = null
)

@Serializable
data class ThumbnailSource(val url: String? = null)

// --- MODELLI PER GLI SHORTS (Contenuti in ReelShelfRenderer) ---
@Serializable
data class ReelShelfRenderer(
    val items: List<ReelItemWrapper>? = null
)

@Serializable
data class ReelItemWrapper(
    @SerialName("shortsLockupViewModel") val shortsLockupViewModel: ShortsLockupViewModel? = null
)

@Serializable
data class ShortsLockupViewModel(
    @SerialName("entityId") val entityId: String? = null,
    val onTap: ReelOnTap? = null,
    @SerialName("overlayMetadata") val overlayMetadata: ReelOverlayMetadata? = null,
    @SerialName("thumbnailViewModel") val thumbnailViewModel: ThumbnailViewModel? = null
)

@Serializable
data class ReelOnTap(
    @SerialName("innertubeCommand") val innertubeCommand: ReelCommand? = null
)

@Serializable
data class ReelCommand(
    @SerialName("reelWatchEndpoint") val reelWatchEndpoint: ReelEndpoint? = null
)

@Serializable
data class ReelEndpoint(
    @SerialName("videoId") val videoId: String? = null
)

@Serializable
data class ReelOverlayMetadata(
    @SerialName("primaryText") val primaryText: ReelText? = null,
    @SerialName("secondaryText") val secondaryText: ReelText? = null
)

@Serializable
data class ReelText(val content: String? = null)

// --- MODELLO PER IL VIDEO IN PRIMO PIANO (ChannelVideoPlayerRenderer) ---
@Serializable
data class ChannelVideoPlayerRenderer(
    @SerialName("videoId") val videoId: String? = null,
    val title: TitleText? = null,
    val description: TitleText? = null
)

// --- MODELLI PER L'INTESTAZIONE (HEADER) ---
@Serializable
data class Header(
    @SerialName("pageHeaderRenderer") val pageHeaderRenderer: PageHeaderRenderer? = null
)

@Serializable
data class PageHeaderRenderer(
    val content: PageHeaderContent? = null
)

@Serializable
data class PageHeaderContent(
    @SerialName("pageHeaderViewModel") val pageHeaderViewModel: PageHeaderViewModel? = null
)

@Serializable
data class PageHeaderViewModel(
    val title: DynamicTitle? = null,
    val image: AvatarWrapper? = null,
    val metadata: ContentMetadataWrapper? = null,
    val description: DescriptionWrapper? = null,
    val banner: BannerWrapper? = null
)

@Serializable
data class DynamicTitle(val dynamicTextViewModel: DynamicText? = null)
@Serializable
data class DynamicText(val text: SimpleText? = null)
@Serializable
data class SimpleText(val content: String? = null)

@Serializable
data class AvatarWrapper(val decoratedAvatarViewModel: DecoratedAvatar? = null)
@Serializable
data class DecoratedAvatar(val avatar: AvatarVm? = null)
@Serializable
data class AvatarVm(val avatarViewModel: AvatarViewModel? = null)
@Serializable
data class AvatarViewModel(val image: ThumbnailImage? = null) // Riutilizza ThumbnailImage

@Serializable
data class ContentMetadataWrapper(val contentMetadataViewModel: ContentMetadataVm? = null)
@Serializable
data class ContentMetadataVm(val metadataRows: List<MetadataRow>? = null)
@Serializable
data class MetadataRow(val metadataParts: List<MetadataPart>? = null)
@Serializable
data class MetadataPart(val text: SimpleText? = null)

@Serializable
data class DescriptionWrapper(val descriptionPreviewViewModel: DescriptionPreviewVm? = null)
@Serializable
data class DescriptionPreviewVm(val description: SimpleText? = null)

@Serializable
data class BannerWrapper(val imageBannerViewModel: BannerVm? = null)
@Serializable
data class BannerVm(val image: ThumbnailImage? = null)

// --- MODELLI PER I METADATI CANALE ---
@Serializable
data class ChannelMetadata(
    @SerialName("channelMetadataRenderer") val channelMetadataRenderer: ChannelMetadataRenderer? = null
)

@Serializable
data class ChannelMetadataRenderer(
    val title: String? = null,
    val description: String? = null,
    @SerialName("externalId") val externalId: String? = null,
    val keywords: String? = null,
    val channelUrl: String? = null
)