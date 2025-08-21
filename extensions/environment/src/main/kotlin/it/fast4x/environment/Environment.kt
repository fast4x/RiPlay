package it.fast4x.environment

import com.zionhuang.innertube.pages.LibraryContinuationPage
import com.zionhuang.innertube.pages.LibraryPage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import it.fast4x.environment.models.AccountInfo
import it.fast4x.environment.models.AccountMenuResponse
import it.fast4x.environment.models.BrowseResponse
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.Context.Client
import it.fast4x.environment.models.Context.Companion.DefaultWeb
import it.fast4x.environment.models.Context.Companion.DefaultWeb2WithLocale
import it.fast4x.environment.models.GridRenderer
import it.fast4x.environment.models.MusicNavigationButtonRenderer
import it.fast4x.environment.models.MusicShelfRenderer
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.NextResponse
import it.fast4x.environment.models.ReturnYouTubeDislikeResponse
import it.fast4x.environment.models.Runs
import it.fast4x.environment.models.Thumbnail
import it.fast4x.environment.models.VideoOrSongInfo
import it.fast4x.environment.models.bodies.AccountMenuBody
import it.fast4x.environment.models.bodies.Action
import it.fast4x.environment.models.bodies.BrowseBody
import it.fast4x.environment.models.bodies.CreatePlaylistBody
import it.fast4x.environment.models.bodies.EditPlaylistBody
import it.fast4x.environment.models.bodies.LikeBody
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.models.bodies.PlaylistDeleteBody
import it.fast4x.environment.models.bodies.SubscribeBody
import it.fast4x.environment.utils.EnvironmentLocale
import it.fast4x.environment.utils.EnvironmentPreferences
import it.fast4x.environment.utils.ProxyPreferences
import it.fast4x.environment.utils.getProxy
import it.fast4x.environment.utils.parseCookieString
import it.fast4x.environment.utils.sha1
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetAddress
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.TimeUnit

const val YT_PLAYLIST_SHARE_BASEURL = "https://www.youtube.com/playlist?list="
const val YTM_PLAYLIST_SHARE_BASEURL = "https://music.youtube.com/playlist?list="
const val YT_VIDEOORSONG_SHARE_BASEURL = "https://www.youtube.com/watch?v="
const val YTM_VIDEOORSONG_SHARE_BASEURL = "https://music.youtube.com/watch?v="
const val YT_ARTIST_SHARE_BASEURL = "https://www.youtube.com/channel/"
const val YTM_ARTIST_SHARE_BASEURL = "https://music.youtube.com/channel/"
const val YT_ALBUM_SHARE_BASEURL = "https://www.youtube.com/browse/"
const val YTM_ALBUM_SHARE_BASEURL = "https://music.youtube.com/browse/"

object Environment {

    val _7ZoUy0mkCP = EnvironmentPreferences.preference?.p37 ?: ""
    val _uMYwa66ycM = EnvironmentPreferences.preference?.p38 ?: ""
    val _3djbhqyLpE = EnvironmentPreferences.preference?.p1 ?: ""
    val _NXIvG4ve8N = EnvironmentPreferences.preference?.p8 ?: ""
    val _QPWiB5riY1 = EnvironmentPreferences.preference?.p11 ?: ""
    val _QPmE9fYezr = EnvironmentPreferences.preference?.p7 ?: ""
    val _Uwjb1AiI8t = EnvironmentPreferences.preference?.p13 ?: ""
    val _qkHMinedvm = EnvironmentPreferences.preference?.p3 ?: ""
    val _lvsJfaKiys = EnvironmentPreferences.preference?.p9 ?: ""
    val _YUxeqOcD7P = EnvironmentPreferences.preference?.p12 ?: ""
    val _Pb7oepZC3P = EnvironmentPreferences.preference?.p4 ?: ""
    val _EWGT63Xrf0 = EnvironmentPreferences.preference?.p14 ?: ""
    val _eR3hChvLRR = EnvironmentPreferences.preference?.p10 ?: ""
    val _wI7xC0jvaR = EnvironmentPreferences.preference?.p2 ?: ""
    val _1enRpaV4ei = EnvironmentPreferences.preference?.p6 ?: ""
    val _XsHo8IdebO = EnvironmentPreferences.preference?.p36 ?: ""
    val _1Vv31MecRl = EnvironmentPreferences.preference?.p0 ?: ""


    private fun buildClient() = HttpClient(OkHttp) {

        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }

        install(HttpCache)



        engine {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )



            if (this@Environment.dnsToUse != null) {
               // Used in memory cache insted of this, it seems that actually there is a bug in file cache with ktor
               // val dnsCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
               // val bootstrapClient = OkHttpClient.Builder().cache(dnsCache).build()
                val bootstrapClient = OkHttpClient.Builder().build()
                val googleDns = DnsOverHttps.Builder().client(bootstrapClient)
                    .url("https://dns.google/dns-query".toHttpUrl())
                    .bootstrapDnsHosts(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4")).build()
                val cloudflareDns = DnsOverHttps.Builder().client(bootstrapClient)
                    .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                    .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1")).build()
                val openDns = DnsOverHttps.Builder().client(bootstrapClient)
                    .url("https://doh.opendns.com/dns-query".toHttpUrl())
                    .bootstrapDnsHosts(InetAddress.getByName("208.67.222.222"), InetAddress.getByName("208.67.220.220")).build()
                val adGuardDns = DnsOverHttps.Builder().client(bootstrapClient)
                    .url("https://unfiltered.adguard-dns.com/dns-query".toHttpUrl()).build()
                val customDns = this@Environment.customDnsToUse?.let {
                    DnsOverHttps.Builder().client(bootstrapClient)
                        .url(it.toHttpUrl()).build()
                } ?: googleDns
                val dns: DnsOverHttps = when (this@Environment.dnsToUse) {
                    "google" -> googleDns
                    "cloudflare" -> cloudflareDns
                    "opendns" -> openDns
                    "adguard" -> adGuardDns
                    "custom" -> customDns
                    else -> googleDns
                }

                val clientWithDns = bootstrapClient.newBuilder().dns(dns).build()
                preconfigured = clientWithDns
            }

            config {
                followRedirects(true)
                followSslRedirects(true)
                retryOnConnectionFailure(true)
                pingInterval(1, TimeUnit.SECONDS)
            }

        }

        ProxyPreferences.preference?.let {
            engine {
                proxy = getProxy(it)
            }
        }

        defaultRequest {
            url(scheme = "https", host = _1Vv31MecRl) {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                parameters.append("prettyPrint", "false")
            }
        }
    }

    var client = buildClient()

    var dnsToUse: String? = null
        set(value) {
            field = value
            client.close()
            client = buildClient()
        }
    var customDnsToUse: String? = null
        set(value) {
            field = value
            client.close()
            client = buildClient()
        }

    var proxy: Proxy? = null
        set(value) {
            field = value
            client.close()
            client = buildClient()
        }

    var locale = EnvironmentLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
        //gl = LocalePreferences.preference?.gl ?: "US",
        //hl = LocalePreferences.preference?.hl ?: "en"
    )

    var visitorData: String = _uMYwa66ycM
    var dataSyncId: String? = null

    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    internal const val musicResponsiveListItemRendererMask = "musicResponsiveListItemRenderer(flexColumns,fixedColumns,thumbnail,navigationEndpoint,badges)"
    internal const val musicTwoRowItemRendererMask = "musicTwoRowItemRenderer(thumbnailRenderer,title,subtitle,navigationEndpoint)"
    const val playlistPanelVideoRendererMask = "playlistPanelVideoRenderer(title,navigationEndpoint,longBylineText,shortBylineText,thumbnail,lengthText)"

    internal fun HttpRequestBuilder.mask(value: String = "*") =
        header("X-Goog-FieldMask", value)


    @Serializable
    data class Info<T : NavigationEndpoint.Endpoint>(
        val name: String?,
        val endpoint: T?
    ) {
        @Suppress("UNCHECKED_CAST")
        constructor(run: Runs.Run) : this(
            name = run.text,
            endpoint = run.navigationEndpoint?.endpoint as T?
        )
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val Song = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val Video = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val Album = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val Artist = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val CommunityPlaylist = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FeaturedPlaylist = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val Podcast = SearchFilter("EgWKAQJQAWoIEBAQERADEBU%3D")
        }
    }

    @Serializable
    sealed class Item {
        abstract val thumbnail: Thumbnail?
        abstract val key: String
        abstract val title: String?
    }

    @Serializable
    data class SongItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val album: Info<NavigationEndpoint.Endpoint.Browse>?,
        val durationText: String?,
        override val thumbnail: Thumbnail?,
        val explicit: Boolean = false,
        val setVideoId: String? = null,
        val isAudioOnly: Boolean = true
    ) : Item() {
        override val key get() = info?.endpoint?.videoId ?: ""
        override val title get() = info?.name

        val isOfficialMusicVideo: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_OMV"

        val isUserGeneratedContent: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_UGC"

        val isOfficialUploadByArtistContent: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_ATV"

        val shareYTUrl: String
            get() = "${YT_VIDEOORSONG_SHARE_BASEURL}${key}"

        val shareYTMUrl: String?
            get() = "${YTM_VIDEOORSONG_SHARE_BASEURL}${key}"

        companion object
    }

    @Serializable
    data class VideoItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val viewsText: String?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.videoId ?: ""
        override val title get() = info?.name

        val isOfficialMusicVideo: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_OMV"

        val isUserGeneratedContent: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_UGC"

        val isOfficialUploadByArtistContent: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_ATV"

        val shareYTUrl: String
            get() = "${YT_VIDEOORSONG_SHARE_BASEURL}${key}"

        val shareYTMUrl: String?
            get() = "${YTM_VIDEOORSONG_SHARE_BASEURL}${key}"

        companion object
    }

    @Serializable
    data class AlbumItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        val playlistId: String? = null,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.browseId ?: ""
        override val title get() = info?.name

        val shareYTUrl: String
            get() = "${YT_ALBUM_SHARE_BASEURL}${playlistId}"

        val shareYTMUrl: String?
            get() = "${YTM_ALBUM_SHARE_BASEURL}${playlistId}"

        companion object
    }

    @Serializable
    data class ArtistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val subscribersCountText: String?,
        val channelId: String? = null,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.browseId ?: ""
        override val title get() = info?.name

        val shareYTUrl: String
            get() = "${YT_ARTIST_SHARE_BASEURL}${channelId}"

        val shareYTMUrl: String?
            get() = "${YTM_ARTIST_SHARE_BASEURL}${channelId}"

        companion object
    }

    @Serializable
    data class PlaylistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val channel: Info<NavigationEndpoint.Endpoint.Browse>?,
        val songCount: Int?,
        val isEditable: Boolean?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.browseId ?: ""
        override val title get() = info?.name

        val shareYTUrl: String
            get() = "${YT_PLAYLIST_SHARE_BASEURL}${key}"

        val shareYTMUrl: String?
            get() = "${YTM_PLAYLIST_SHARE_BASEURL}${key}"

        companion object
    }

    data class ArtistInfoPage(
        val name: String?,
        val description: String?,
        val subscriberCountText: String?,
        val thumbnail: Thumbnail?,
        val shuffleEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val radioEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val songs: List<SongItem>?,
        val songsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val albums: List<AlbumItem>?,
        val albumsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val singles: List<AlbumItem>?,
        val singlesEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val playlists: List<PlaylistItem>?,
    )

    data class PlaylistOrAlbumPage(
        val title: String?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        val thumbnail: Thumbnail?,
        val url: String?,
        val songsPage: ItemsPage<SongItem>?,
        val otherVersions: List<AlbumItem>?,
        val description: String?,
        val otherInfo: String?
    )

    data class NextPage(
        val itemsPage: ItemsPage<SongItem>?,
        val playlistId: String?,
        val params: String? = null,
        val playlistSetVideoId: String? = null
    )

    @Serializable
    data class RelatedPage(
        val songs: List<SongItem>? = null,
        val playlists: List<PlaylistItem>? = null,
        val albums: List<AlbumItem>? = null,
        val artists: List<ArtistItem>? = null,
    )
    data class RelatedSongs(
        val songs: List<SongItem>? = null
    )

    @Serializable
    data class DiscoverPage(
        val newReleaseAlbums: List<AlbumItem>,
        val moods: List<Mood.Item>
    )

    data class DiscoverPageAlbums(
        val newReleaseAlbums: List<AlbumItem>

    )

    @Serializable
    data class Mood(
        val title: String,
        val items: List<Item>
    ) {
        @Serializable
        data class Item(
            val title: String,
            val stripeColor: Long,
            val endpoint: NavigationEndpoint.Endpoint.Browse
        )
    }

    data class ItemsPage<T : Item>(
        var items: List<T>?,
        val continuation: String?
    )

    @Serializable
    data class ChartsPage(
        val playlists: List<PlaylistItem>? = null,
        val artists: List<ArtistItem>? = null,
        val videos: List<VideoItem>? = null,
        val songs: List<SongItem>? = null,
        val trending: List<SongItem>? = null
    )

    data class Podcast(
        val title: String,
        val author: String?,
        val authorThumbnail: String?,
        val thumbnail: List<Thumbnail>,
        val description: String?,
        val listEpisode: List<EpisodeItem>,
    ) {
        data class EpisodeItem(
            val title: String,
            val author: String?,
            val description: String?,
            val thumbnail: List<Thumbnail>,
            val createdDay: String?,
            val durationString: String?,
            val videoId: String
        )
    }

    data class SearchSuggestions(
        val queries: List<String>,
        val recommendedSong: SongItem?,
        val recommendedAlbum: AlbumItem?,
        val recommendedArtist: ArtistItem?,
        val recommendedPlaylist: PlaylistItem?,
        val recommendedVideo: VideoItem?,
    )

    fun MusicNavigationButtonRenderer.toMood(): Mood.Item? {
        return Mood.Item(
            title = buttonText.runs.firstOrNull()?.text ?: return null,
            stripeColor = solid?.leftStripeColor ?: return null,
            endpoint = clickCommand.browseEndpoint ?: return null
        )
    }

    fun List<Thumbnail>.getBestQuality() =
        maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }


    suspend fun accountInfo(): Result<AccountInfo?> = runCatching {

        accountMenu()
            .body<AccountMenuResponse>()
            .actions?.get(0)?.openPopupAction?.popup?.multiPageMenuRenderer
            ?.header?.activeAccountHeaderRenderer
            ?.toAccountInfo()
    }.onFailure {
        println("Error YoutubeLogin accountInfo(): ${it.stackTraceToString()}")
    }

    suspend fun accountInfoList(): Result<List<AccountInfo?>?> = runCatching {
        accountMenu()
            .body<AccountMenuResponse>()
            .actions?.get(0)?.openPopupAction?.popup?.multiPageMenuRenderer
            ?.header?.activeAccountHeaderRenderer
            ?.toAccountInfoList()
    }

    suspend fun accountMenu(): HttpResponse {
        val response =
            client.post(_qkHMinedvm) {
                setLogin(setLogin = true)
                setBody(AccountMenuBody())
            }

        return response
    }

    suspend fun getSwJsData() = client.get("https://$_1Vv31MecRl/sw.js_data")

    suspend fun getInitialVisitorData(): Result<String> = runCatching {
        Json.parseToJsonElement(getSwJsData().bodyAsText().substring(5))
            .jsonArray[0]
            .jsonArray[2]
            .jsonArray.first { (it as? JsonPrimitive)?.content?.startsWith(_7ZoUy0mkCP) == true }
            .jsonPrimitive.content
    }.onFailure {
        println("Environment Error in getInitialVisitorData(): ${it.stackTraceToString()}")
    }

    fun HttpRequestBuilder.setLogin(clientType: Client = DefaultWeb.client, setLogin: Boolean = false) {

        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", "${clientType.xClientName ?: 1}")
            append("X-YouTube-Client-Version", clientType.clientVersion)
            append("X-Origin", _XsHo8IdebO)
            if (clientType.referer != null) {
                append("Referer", clientType.referer)
            }
            if (setLogin && clientType.loginSupported) {
                cookie?.let { cookieData ->

                    cookieMap = parseCookieString(cookieData)
//                    append("X-Goog-Authuser", "0")
//                    append("X-Goog-Visitor-Id", visitorData ?: "")
                    append("Cookie", cookieData)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidCookie = cookieMap["SAPISID"]
                    val sapisidHash = sha1("$currentTime $sapisidCookie $_XsHo8IdebO")
                    println("HttpRequestBuilder.setLogin currentTime ${currentTime}")
                    println("HttpRequestBuilder.setLogin sapisidCookie ${sapisidCookie}")
                    println("HttpRequestBuilder.setLogin sapisidHash ${sapisidHash}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
                }
            }
        }
        clientType.userAgent?.let { userAgent(it) }
        parameter("prettyPrint", false)

    }

    /*******************************************
     * NEW CODE
     */

    suspend fun createPlaylist(
        ytClient: Client,
        title: String,
    ) = client.post(_lvsJfaKiys) {
        setLogin(ytClient, true)
        setBody(
            CreatePlaylistBody(
                context = Context.DefaultWebWithLocale,
                title = title
            )
        )
    }

    suspend fun deletePlaylist(
        ytClient: Client,
        playlistId: String,
    ) = client.post(_YUxeqOcD7P) {
        println("deleting $playlistId")
        setLogin(ytClient, setLogin = true)
        setBody(
            PlaylistDeleteBody(
                context = Context.DefaultWebWithLocale,
                playlistId = playlistId
            )
        )
    }

    suspend fun renamePlaylist(
        ytClient: Client,
        playlistId: String,
        name: String,
    ) = client.post(_Pb7oepZC3P) {
        setLogin(ytClient, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = Context.DefaultWebWithLocale,
                playlistId = playlistId,
                actions = listOf(
                    Action.RenamePlaylistAction(
                        playlistName = name
                    )
                )
            )
        )
    }

    suspend fun addToPlaylist(
        ytClient: Client,
        playlistId: String,
        videoId: String,
    ) = addToPlaylist(ytClient, playlistId, listOf(videoId))

    suspend fun addToPlaylist(
        ytClient: Client,
        playlistId: String,
        videoIds: List<String>,
    ) = client.post(_Pb7oepZC3P) {
        setLogin(ytClient, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = Context.DefaultWebWithLocale,
                playlistId = playlistId.removePrefix("VL"),
                actions = videoIds.map{ Action.AddVideoAction(addedVideoId = it)}
            )
        )
    }

    suspend fun removeFromPlaylist(
        ytClient: Client,
        playlistId: String,
        videoId: String,
        setVideoId: String? = null,
    ) = removeFromPlaylist(ytClient, playlistId, videoId, listOf(setVideoId))

    suspend fun removeFromPlaylist(
        ytClient: Client,
        playlistId: String,
        videoId: String,
        setVideoIds: List<String?>,
    ) = client.post(_Pb7oepZC3P) {
        setLogin(ytClient, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = Context.DefaultWebWithLocale,
                playlistId = playlistId.removePrefix("VL"),
                actions = setVideoIds.map {
                    Action.RemoveVideoAction(
                        removedVideoId = videoId,
                        setVideoId = it,
                    )
                }
            )
        )
    }

    suspend fun addPlaylistToPlaylist(
        ytClient: Client,
        playlistId: String,
        addPlaylistId: String,
    ) = client.post(_Pb7oepZC3P) {
        setLogin(ytClient, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = Context.DefaultWebWithLocale,
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                )
            )
        )
    }

    suspend fun subscribeChannel(
        channelId: String,
    ) = client.post(_EWGT63Xrf0) {
        setLogin(setLogin = true)
        setBody(
            SubscribeBody(
                context = DefaultWeb,
                channelIds = listOf(channelId)
            )
        )
    }

    suspend fun unsubscribeChannel(
        channelId: String,
    ) = client.post(_eR3hChvLRR) {
        setLogin(setLogin = true)
        setBody(
            SubscribeBody(
                context = DefaultWeb,
                channelIds = listOf(channelId)
            )
        )
    }


    suspend fun likePlaylistOrAlbum(
        playlistId: String,
    ) = client.post(_wI7xC0jvaR) {
        setLogin(setLogin = true)
        setBody(
            LikeBody(
                context = DefaultWeb,
                target = LikeBody.Target.PlaylistTarget(playlistId = playlistId)
            )
        )
    }

    suspend fun removelikePlaylistOrAlbum(
        playlistId: String,
    ) = client.post(_1enRpaV4ei) {
        setLogin(setLogin = true)
        setBody(
            LikeBody(
                context = DefaultWeb,
                target = LikeBody.Target.PlaylistTarget(playlistId = playlistId)
            )
        )
    }

    suspend fun likeVideoOrSong(
        videoId: String,
    ) = client.post(_wI7xC0jvaR) {
        setLogin(setLogin = true)
        setBody(
            LikeBody(
                context = DefaultWeb,
                target = LikeBody.Target.VideoTarget(videoId = videoId)
            )
        )
    }

    suspend fun removelikeVideoOrSong(
        videoId: String,
    ) = client.post(_1enRpaV4ei) {
        setLogin(setLogin = true)
        setBody(
            LikeBody(
                context = DefaultWeb,
                target = LikeBody.Target.VideoTarget(videoId = videoId)
            )
        )
    }



    suspend fun browse(
        ytClient: Client = DefaultWeb.client,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = client.post(_3djbhqyLpE) {
        setLogin(ytClient, true)
        setBody(
            BrowseBody(
                context = Context.DefaultWebWithLocale,
                browseId = browseId,
                params = params,
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
        if (continuation != null) {
            parameter("type", "next")
        }
    }

    suspend fun customBrowse(
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = true,
    ) = runCatching {
        browse(DefaultWeb.client, browseId, params, continuation, setLogin).body<BrowseResponse>()
    }

    suspend fun next(
        context: Context = DefaultWeb2WithLocale,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = client.post(_NXIvG4ve8N) {
        setLogin(context.client, false)
        setBody(
            NextBody(
                context = context,
                videoId = videoId,
                playlistId = playlistId,
                playlistSetVideoId = playlistSetVideoId,
                index = index,
                params = params,
                continuation = continuation,
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
        if (continuation != null) {
            parameter("type", "next")
        }
    }


    suspend fun library(browseId: String, tabIndex: Int = 0) = runCatching {
        val response = browse(
            browseId = browseId,
            setLogin = true
        ).body<BrowseResponse>()

        val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs

        val contents = if (tabs != null && tabs.size >= tabIndex) {
            tabs[tabIndex].tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
        }
        else {
            null
        }

        when {
            contents?.gridRenderer != null -> {
                contents.gridRenderer.items
                    ?.mapNotNull (GridRenderer.Item::musicTwoRowItemRenderer)
                    ?.mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }?.let {
                        LibraryPage(
                            items = it,
                            continuation = contents.gridRenderer.continuations?.firstOrNull()?.nextContinuationData?.continuation
                        )
                    }
            }

            else -> {
                LibraryPage(
                    items = contents?.musicShelfRenderer?.contents!!
                        .mapNotNull (MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                    continuation = contents.musicShelfRenderer.continuations?.firstOrNull()?.
                    nextContinuationData?.continuation
                )
            }
        }
    }

    suspend fun libraryContinuation(continuation: String) = runCatching {
        val response = browse(
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        val contents = response.continuationContents

        when {
            contents?.gridContinuation != null -> {
                contents.gridContinuation.items
                    ?.mapNotNull (GridRenderer.Item::musicTwoRowItemRenderer)
                    ?.mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }?.let {
                        LibraryContinuationPage(
                            items = it,
                            continuation = contents.gridContinuation.continuations?.firstOrNull()?.nextContinuationData?.continuation
                        )
                    }
            }

            else -> {
                LibraryContinuationPage(
                    items = contents?.musicShelfContinuation?.contents!!
                        .mapNotNull (MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                    continuation = contents.musicShelfContinuation.continuations?.firstOrNull()?.
                    nextContinuationData?.continuation
                )
            }
        }
    }

    suspend fun returnYouTubeDislike(videoId: String) =
        client.get("https://returnyoutubedislikeapi.com/Votes?videoId=$videoId") {
            contentType(ContentType.Application.Json)
        }

    suspend fun getVideoOrSongInfo(videoId: String): Result<VideoOrSongInfo> =
        runCatching {
            val response = next(context = DefaultWeb2WithLocale, videoId, null, null, null, null, null).body<NextResponse>()
            val videoSecondary =
                response.contents?.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer
            val videoPrimary =
                response.contents?.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer
            val returnYouTubeDislikeResponse =
                returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()
            return@runCatching VideoOrSongInfo(
                videoId = videoId,
                title = videoPrimary
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                author = videoSecondary
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                authorId =
                videoSecondary
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.navigationEndpoint
                    ?.browseEndpoint
                    ?.browseId,
                authorThumbnail =
                videoSecondary
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.thumbnail
                    ?.thumbnails
                    ?.find {
                        it.height == 48
                    }?.url
                    ?.replace("s48", "s960"),
                description = videoSecondary?.attributedDescription?.content,
                subscribers =
                videoSecondary
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.subscriberCountText
                    ?.simpleText?.split(" ")?.firstOrNull(),
                uploadDate = videoPrimary?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )

        }

}
