package it.fast4x.riplay.ui.screens.player.common

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.valentinilk.shimmer.shimmer
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.lyrics
import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import it.fast4x.lrclib.models.Track
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.Languages
import it.fast4x.riplay.enums.LyricsAlignment
import it.fast4x.riplay.enums.LyricsBackground
import it.fast4x.riplay.enums.LyricsColor
import it.fast4x.riplay.enums.LyricsFontSize
import it.fast4x.riplay.enums.LyricsHighlight
import it.fast4x.riplay.enums.LyricsOutline
import it.fast4x.riplay.enums.PlayerBackgroundColors
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.Romanization
import it.fast4x.riplay.data.models.Lyrics
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.TextPlaceholder
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.ui.styling.DefaultDarkColorPalette
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.PureBlackColorPalette
import it.fast4x.riplay.ui.styling.onOverlayShimmer
import it.fast4x.riplay.ui.styling.center
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.extensions.preferences.colorPaletteModeKey
import it.fast4x.riplay.extensions.preferences.expandedplayerKey
import it.fast4x.riplay.extensions.preferences.isShowingSynchronizedLyricsKey
import it.fast4x.riplay.utils.languageDestination
import it.fast4x.riplay.utils.languageDestinationName
import it.fast4x.riplay.extensions.preferences.lyricsAlignmentKey
import it.fast4x.riplay.extensions.preferences.lyricsBackgroundKey
import it.fast4x.riplay.extensions.preferences.lyricsColorKey
import it.fast4x.riplay.extensions.preferences.lyricsFontSizeKey
import it.fast4x.riplay.extensions.preferences.lyricsHighlightKey
import it.fast4x.riplay.extensions.preferences.lyricsOutlineKey
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.extensions.preferences.otherLanguageAppKey
import it.fast4x.riplay.extensions.preferences.playerBackgroundColorsKey
import it.fast4x.riplay.extensions.preferences.playerEnableLyricsPopupMessageKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.romanizationKey
import it.fast4x.riplay.extensions.preferences.showBackgroundLyricsKey
import it.fast4x.riplay.extensions.preferences.showSecondLineKey
import it.fast4x.riplay.extensions.preferences.showlyricsthumbnailKey
import it.fast4x.riplay.utils.copyTextToClipboard
import it.fast4x.riplay.utils.verticalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.ColorPaletteName
import it.fast4x.riplay.extensions.lyricshelper.models.LyricLine
import it.fast4x.riplay.extensions.lyricshelper.parsers.SyncLRCLyricsKaraokeParser
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography
import it.fast4x.riplay.ui.components.themed.LyricsSizeDialog
import it.fast4x.riplay.extensions.preferences.colorPaletteNameKey
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.extensions.preferences.effectRotationKey
import it.fast4x.riplay.extensions.preferences.isShowingSynchronizedWordByWordLyricsKey
import it.fast4x.riplay.extensions.preferences.jumpPreviousKey
import it.fast4x.riplay.extensions.preferences.landscapeControlsKey
import it.fast4x.riplay.extensions.preferences.lyricsSizeAnimateKey
import it.fast4x.riplay.extensions.preferences.lyricsSizeKey
import it.fast4x.riplay.extensions.preferences.lyricsSizeLKey
import it.fast4x.riplay.ui.styling.ColorPalette
import it.fast4x.riplay.utils.SynchronizedLyricsLines
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.toLyricLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import kotlin.Float.Companion.POSITIVE_INFINITY
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.extensions.lyricshelper.providers.syncLRCfetchLyrics
import it.fast4x.riplay.extensions.lyricshelper.models.SyncLRCType
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.utils.CustomHttpClient
import it.fast4x.riplay.utils.PlayerViewModel
import it.fast4x.riplay.utils.PlayerViewModelFactory
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.getRoundnessShape


val RainbowColors = listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
val RainbowColorsdark = listOf(Color.Black.copy(0.35f).compositeOver(Color.Red), Color.Black.copy(0.35f).compositeOver(Color.Magenta), Color.Black.copy(0.35f).compositeOver(Color.Blue), Color.Black.copy(0.35f).compositeOver(Color.Cyan), Color.Black.copy(0.35f).compositeOver(Color.Green), Color.Black.copy(0.35f).compositeOver(Color.Yellow), Color.Black.copy(0.35f).compositeOver(Color.Red))
val RainbowColors2 = listOf(Color.Red.copy(0.3f), Color.Magenta.copy(0.3f), Color.Blue.copy(0.3f), Color.Cyan.copy(0.3f), Color.Green.copy(0.3f), Color.Yellow.copy(0.3f), Color.Red.copy(0.3f))


@OptIn(ExperimentalSerializationApi::class)
@UnstableApi
@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    //positionProvider: () -> Long = { 0L },
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier,
    clickLyricsText: Boolean,
    trailingContent: (@Composable () -> Unit)? = null,
    isLandscape: Boolean,
) {
    if (!isDisplayed) return

    val context = LocalContext.current
    val menuState = LocalGlobalSheetState.current
    val currentView = LocalView.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    val factory = remember(binder) {
        PlayerViewModelFactory(binder)
    }
    val playerViewModel: PlayerViewModel = viewModel(factory = factory)
    val positionAndDuration by playerViewModel.positionAndDuration.collectAsStateWithLifecycle()
    val positionProvider = { positionAndDuration.first }
    //Timber.d("LyricsNew positionAndDuration ${positionAndDuration.first}")

    var showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, false)
    var isShowingSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, false)
    var isShowingSynchronizedWordByWordLyrics by rememberPreference(isShowingSynchronizedWordByWordLyricsKey, false)

    val currentLyrics by Database.lyrics(mediaId).collectAsState(initial = null)

    var invalidLrc by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
    var isPicking by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }

    var lyricsColor by rememberPreference(lyricsColorKey, LyricsColor.Thememode)
    var lyricsOutline by rememberPreference(lyricsOutlineKey, LyricsOutline.None)
    val playerBackgroundColors by rememberPreference(playerBackgroundColorsKey, PlayerBackgroundColors.BlurredCoverColor)
    var lyricsFontSize by rememberPreference(lyricsFontSizeKey, LyricsFontSize.Medium)

    val thumbnailSize = Dimensions.thumbnails.player.song
    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)

    var isEditing by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
    var showPlaceholder by remember { mutableStateOf(false) }

    val lyricsText = when {
        isShowingSynchronizedLyrics && !isShowingSynchronizedWordByWordLyrics -> currentLyrics?.synced ?: ""
        isShowingSynchronizedWordByWordLyrics && isShowingSynchronizedLyrics -> currentLyrics?.lrcSynced ?: ""
        else -> currentLyrics?.fixed ?: ""
    }

    var textTranslated by remember { mutableStateOf("") }
    var isError by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
    var isErrorSync by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }

    var showLanguagesList by remember { mutableStateOf(false) }
    var translateEnabled by remember { mutableStateOf(false) }

    var romanization by rememberPreference(romanizationKey, Romanization.Off)
    var showSecondLine by rememberPreference(showSecondLineKey, false)
    var otherLanguageApp by rememberPreference(otherLanguageAppKey, Languages.English)
    var lyricsBackground by rememberPreference(lyricsBackgroundKey, LyricsBackground.Black)


    if (showLanguagesList) {
        translateEnabled = false
        menuState.display {
            Menu {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TitleSection(title = stringResource(R.string.languages))
                }
                MenuEntry(icon = R.drawable.translate, text = stringResource(R.string.do_not_translate), onClick = {
                    menuState.hide(); showLanguagesList = false; translateEnabled = false
                })
                MenuEntry(icon = R.drawable.translate, text = stringResource(R.string._default), secondaryText = languageDestinationName(otherLanguageApp), onClick = {
                    menuState.hide(); showLanguagesList = false; translateEnabled = true
                })
                Languages.entries.forEach {
                    if (it != Languages.System) MenuEntry(icon = R.drawable.translate, text = languageDestinationName(it), onClick = {
                        menuState.hide(); otherLanguageApp = it; showLanguagesList = false; translateEnabled = true
                    })
                }
            }
        }
    }

    val languageDestination = languageDestination(otherLanguageApp)

    val translator =  Translator(CustomHttpClient.okHttpClient)

    var copyToClipboard by remember { mutableStateOf(false) }
    if (copyToClipboard) { lyricsText?.let { copyTextToClipboard(it, context) }; copyToClipboard = false }

    var copyTranslatedToClipboard by remember { mutableStateOf(false) }
    if (copyTranslatedToClipboard) { textTranslated.let { copyTextToClipboard(it, context) }; copyTranslatedToClipboard = false }

    var fontSize by rememberPreference(lyricsFontSizeKey, LyricsFontSize.Medium)
    val showBackgroundLyrics by rememberPreference(showBackgroundLyricsKey, false)
    val playerEnableLyricsPopupMessage by rememberPreference(playerEnableLyricsPopupMessageKey, true)
    var expandedplayer by rememberPreference(expandedplayerKey, false)

    var checkedLyricsLrc by remember(mediaId) { mutableStateOf(false) }
    var checkedLyricsKugou by remember(mediaId) { mutableStateOf(false) }
    var checkedLyricsInnertube by remember(mediaId) { mutableStateOf(false) }
    var checkedLyricsSyncLrc by remember(mediaId) { mutableStateOf(false) }
    var checkLyrics by remember { mutableStateOf(false) }
    var lyricsHighlight by rememberPreference(lyricsHighlightKey, LyricsHighlight.None)
    var lyricsAlignment by rememberPreference(lyricsAlignmentKey, LyricsAlignment.Center)
    var lyricsSizeAnimate by rememberPreference(lyricsSizeAnimateKey, false)

    val mediaMetadata = mediaMetadataProvider()
    var artistName by rememberSaveable { mutableStateOf(mediaMetadata.artist?.toString().orEmpty()) }
    var title by rememberSaveable { mutableStateOf(cleanPrefix(mediaMetadata.title?.toString().orEmpty())) }

    var lyricsSize by rememberPreference(lyricsSizeKey, 20f)
    var lyricsSizeL by rememberPreference(lyricsSizeLKey, 20f)
    val customSize = if (isLandscape) lyricsSizeL else lyricsSize
    var showLyricsSizeDialog by rememberSaveable { mutableStateOf(false) }

    val lightTheme = colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))
    val effectRotationEnabled by rememberPreference(effectRotationKey, true)
    var landscapeControls by rememberPreference(landscapeControlsKey, true)
    var jumpPrevious by rememberPreference(jumpPreviousKey, "3")
    var isRotated by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (isRotated) 360F else 0f, animationSpec = tween(200), label = "")
    val colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)

    if (showLyricsSizeDialog) {
        LyricsSizeDialog(onDismiss = { showLyricsSizeDialog = false }, sizeValue = { lyricsSize = it }, sizeValueL = { lyricsSizeL = it })
    }

    LaunchedEffect(mediaMetadata.title, mediaMetadata.artist) {
        artistName = mediaMetadata.artist?.toString().orEmpty()
        title = cleanPrefix(mediaMetadata.title?.toString().orEmpty())
    }


    @Composable
    fun translateLyrics(output: MutableState<String>, textToTranslate: String, isSync: Boolean, destinationLanguage: Language = Language.AUTO) {
        LaunchedEffect(showSecondLine, romanization, textToTranslate, destinationLanguage){
            var destLanguage = destinationLanguage
            val result = withContext(Dispatchers.IO) {
                try {
                    val helperTranslation = translator.translate(textToTranslate, Language.CHINESE_TRADITIONAL, Language.AUTO)
                    if (destinationLanguage == Language.AUTO) {
                        destLanguage = if (helperTranslation.translatedText == textToTranslate) Language.CHINESE_TRADITIONAL else helperTranslation.sourceLanguage
                    }
                    val mainTranslation = translator.translate(textToTranslate, destLanguage, Language.AUTO)
                    val outputText = if (textToTranslate.isEmpty()) ""
                    else if (!showSecondLine || (mainTranslation.sourceText == mainTranslation.translatedText)) {
                        if (romanization == Romanization.Off) if (translateEnabled) mainTranslation.translatedText else textToTranslate
                        else if (romanization == Romanization.Original) if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                        else if (romanization == Romanization.Translated) mainTranslation.translatedPronunciation ?: mainTranslation.translatedText
                        else if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                    } else {
                        if (romanization == Romanization.Off) textToTranslate + "\\n[${mainTranslation.translatedText}]"
                        else if (romanization == Romanization.Original) {
                            if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation
                            else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText + "\\n[${mainTranslation.translatedText}]"
                        } else if (romanization == Romanization.Translated) textToTranslate + "\\n[${mainTranslation.translatedPronunciation ?: mainTranslation.translatedText}]"
                        else if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation
                        else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText + "\\n[${mainTranslation.translatedPronunciation ?: mainTranslation.translatedText}]"
                    }
                    outputText?.replace("\\r", "\r")?.replace("\\n", "\n")
                } catch (e: Exception) { 
                    Timber.e("Lyrics translation error ${e.stackTraceToString()}")
                    showPlaceholder = false
                    output.value = appContext().resources.getString(R.string.an_error_has_occurred_while_fetching_the_lyrics)
                }
            }
            val translatedText = if (result.toString() == "kotlin.Unit") "" else result.toString()
            showPlaceholder = false
            output.value = translatedText
            textTranslated = translatedText
        }
    }

    LaunchedEffect(mediaId, isShowingSynchronizedLyrics, isShowingSynchronizedWordByWordLyrics, checkLyrics) {
        fetchLyricsIfNeeded(
            mediaId = mediaId,
            artistName = artistName,
            title = title,
            currentLyrics = currentLyrics,
            isShowingSynchronizedWordByWordLyrics = isShowingSynchronizedWordByWordLyrics,
            isShowingSynchronizedLyrics = isShowingSynchronizedLyrics,
            mediaMetadata = mediaMetadata,
            durationProvider = durationProvider,
            playerEnableLyricsPopupMessage = playerEnableLyricsPopupMessage,
            context = context,
            onCheckedLrc = { checkedLyricsLrc = it },
            onCheckedKugou = { checkedLyricsKugou = it },
            onCheckedInnertube = { checkedLyricsInnertube = it },
            onCheckedSyncLrc = { checkedLyricsSyncLrc = it },
            onError = {
                isError = it
                Timber.e("Lyrics fetchLyricsIfNeeded onError $it")
            }
        )

    }


     // todo  To improve actually not stable
    if (isShowingSynchronizedLyrics && lyricsText.isNotEmpty()) {
        val mutState = remember { mutableStateOf("") }
        if (translateEnabled)
            translateLyrics(mutState, lyricsText, true, languageDestination)
    } else if (!isShowingSynchronizedLyrics && lyricsText.isNotEmpty()) {
        val mutState = remember { mutableStateOf("") }
        if (translateEnabled)
            translateLyrics(mutState, lyricsText, false, languageDestination)
    }


    if (isEditing) {
        InputTextDialog(
            onDismiss = { isEditing = false },
            setValueRequireNotNull = false,
            title = stringResource(R.string.enter_the_lyrics),
            value = lyricsText ?: "",
            placeholder = stringResource(R.string.enter_the_lyrics),
            setValue = {
                Database.asyncTransaction {
                    ensureSongInserted()
                    upsert(
                        Lyrics(
                            songId = mediaId,
                            fixed = if (isShowingSynchronizedLyrics) currentLyrics?.fixed else it,
                            synced = if (isShowingSynchronizedLyrics) it else currentLyrics?.synced,
                        )
                    )
                }
            }
        )
    }


    @Composable
    fun SelectLyricFromTrack(tracks: List<Track>, lyrics: Lyrics?) {
        menuState.display {
            Menu {
                MenuEntry(icon = R.drawable.chevron_back, text = stringResource(R.string.cancel), onClick = { menuState.hide() })
                Row {
                    TextField(value = title, onValueChange = { title = it }, singleLine = true, colors = TextFieldDefaults.textFieldColors(textColor = colorPalette().text, unfocusedIndicatorColor = colorPalette().text), modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .weight(1f))
                    TextField(value = artistName, onValueChange = { artistName = it }, singleLine = true, colors = TextFieldDefaults.textFieldColors(textColor = colorPalette().text, unfocusedIndicatorColor = colorPalette().text), modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .weight(1f))
                    IconButton(icon = R.drawable.search, color = Color.Black, onClick = { isPicking = false; menuState.hide(); isPicking = true }, modifier = Modifier
                        .background(shape = RoundedCornerShape(4.dp), color = Color.White)
                        .padding(4.dp)
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                        .weight(0.2f))
                }
                tracks.forEach {
                    MenuEntry(icon = R.drawable.text, text = "${it.artistName} - ${it.trackName}", secondaryText = "${stringResource(R.string.sort_duration)} ${it.duration.seconds.toComponents { m, s, _ -> "$m:${s.toString().padStart(2, '0')}" }} ${stringResource(R.string.id)} ${it.id}", onClick = {
                        menuState.hide()
                        Database.asyncTransaction { upsert(Lyrics(songId = mediaId, fixed = lyrics?.fixed, synced = it.syncedLyrics.orEmpty())) }
                    })
                }
                MenuEntry(icon = R.drawable.chevron_back, text = stringResource(R.string.cancel), onClick = { menuState.hide() })
            }
        }
        isPicking = false
    }


    if (isPicking && isShowingSynchronizedLyrics) {
        var loading by remember { mutableStateOf(true) }
        val tracks = remember { mutableStateListOf<Track>() }
        var error by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            runCatching {
                LrcLib.lyrics(artist = artistName, title = title)?.onSuccess {
                    if (it.isNotEmpty() && playerEnableLyricsPopupMessage) coroutineScope.launch { SmartMessage(context.resources.getString(R.string.info_lyrics_tracks_found_on_s).format("LrcLib.net"), type = PopupType.Success, context = context) }
                    else if (playerEnableLyricsPopupMessage) coroutineScope.launch { SmartMessage(context.resources.getString(R.string.info_lyrics_tracks_not_found_on_s).format("LrcLib.net"), type = PopupType.Error, durationLong = true, context = context) }
                    if (it.isEmpty()) {
                        menuState.display { /* Menu ricerca */ }
                        isPicking = false
                    }
                    tracks.clear(); tracks.addAll(it); loading = false; error = false
                }?.onFailure {
                    if (playerEnableLyricsPopupMessage) coroutineScope.launch { SmartMessage(context.resources.getString(R.string.an_error_has_occurred_while_fetching_the_lyrics).format("LrcLib.net"), type = PopupType.Error, durationLong = true, context = context) }
                    loading = false; error = true
                }
            }.onFailure { Timber.e("Lyrics get error 1 ${it.stackTraceToString()}") }
        }
        if (loading) DefaultDialog(onDismiss = { isPicking = false }) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
        if (tracks.isNotEmpty()) SelectLyricFromTrack(tracks = tracks, lyrics = currentLyrics)
    }

    if (isShowingSynchronizedLyrics) {
        DisposableEffect(Unit) {
            currentView.keepScreenOn = true
            onDispose { currentView.keepScreenOn = false }
        }
    }


    if (lyricsText.isEmpty() && !checkedLyricsLrc && !checkedLyricsKugou && !checkedLyricsInnertube && !checkedLyricsSyncLrc)
        checkLyrics = !checkLyrics


    AnimatedVisibility(visible = isDisplayed, enter = fadeIn(), exit = fadeOut()) {
        Box(contentAlignment = Alignment.Center, modifier = modifier
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .fillMaxSize()
            .background(if (!showlyricsthumbnail) Color.Transparent else Color.Black.copy(0.8f))
            .clip(thumbnailShape())) {

            // Error Banner
            AnimatedVisibility(visible = (isError && lyricsText.isEmpty()) || (invalidLrc && isShowingSynchronizedLyrics), enter = slideInVertically { -it }, exit = slideOutVertically { -it }, modifier = Modifier.align(Alignment.TopCenter)) {
                BasicText(text = stringResource(R.string.an_error_has_occurred_while_fetching_the_lyrics), style = typography().xs.center.medium.color(PureBlackColorPalette.text), modifier = Modifier
                    .background(
                        if (!showlyricsthumbnail) Color.Transparent else Color.Black.copy(
                            0.4f
                        )
                    )
                    .padding(8.dp)
                    .fillMaxWidth())
            }

            if (lyricsText.isNotEmpty()) {
                if (isShowingSynchronizedLyrics) {
                    val density = LocalDensity.current

                    val synchronizedLyrics = remember(isShowingSynchronizedLyrics, isShowingSynchronizedWordByWordLyrics, lyricsText) {
                        val sentences = if (!isShowingSynchronizedWordByWordLyrics) LrcLib.Lyrics(lyricsText).sentences.toLyricLine()
                        else SyncLRCLyricsKaraokeParser
                            .parse(currentLyrics?.lrcSynced ?: "", isOnline = binder?.player?.currentMediaItem?.isLocal == true)
                        invalidLrc = false
                        SynchronizedLyricsLines(sentences) { positionProvider() }
                    }

                    val lazyListState = rememberLazyListState()


                    LaunchedEffect(synchronizedLyrics, density) {
                        val centerOffset = with(density) { (-thumbnailSize.div(if (!showlyricsthumbnail && !isLandscape) if (trailingContent == null) 2 else 1 else if (trailingContent == null) 3 else 2)).roundToPx() }
                        lazyListState.animateScrollToItem(index = synchronizedLyrics.index + 1, scrollOffset = centerOffset)
                        while (isActive) {
                            delay(50)
                            if (!synchronizedLyrics.update()) continue
                            lazyListState.animateScrollToItem(index = synchronizedLyrics.index + 1, scrollOffset = centerOffset)
                        }
                    }

                    var modifierBG = Modifier.verticalFadingEdge()
                    if (showBackgroundLyrics && showlyricsthumbnail) modifierBG = modifierBG.background(colorPalette().accent)

                    LazyColumn(state = lazyListState, userScrollEnabled = true, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = modifierBG.background(if (isDisplayed && !showlyricsthumbnail) if (lyricsBackground == LyricsBackground.Black) Color.Black.copy(0.6f) else if (lyricsBackground == LyricsBackground.White) Color.White.copy(0.4f) else Color.Transparent else Color.Transparent)) {
                        item(key = "header", contentType = 0) { Spacer(modifier = Modifier.height(thumbnailSize)) }
                        itemsIndexed(items = synchronizedLyrics.sentences) { index, sentence ->
                            val isActive = index == synchronizedLyrics.index
                            val progressInsideLine = if (isActive) (positionProvider() - sentence.timeMs) else -1L

//                            if (isActive) {
//                                Timber.d("DEBUG_TIME Posizione Player: ${positionProvider()} | Inizio Riga: ${sentence.timeMs} | Risultato: ${progressInsideLine}")
//                            }

                            var translatedText by remember { mutableStateOf("") }
                            val trimmedSentence = sentence.text.trim()
                            if (showSecondLine || translateEnabled || romanization != Romanization.Off) {
                                val mutState = remember { mutableStateOf("") }
                                // todo  To improve actually not stable
                                if (translateEnabled)
                                    translateLyrics(mutState, trimmedSentence, true, languageDestination)
                                translatedText = mutState.value
                            } else {
                                translatedText = trimmedSentence
                            }

                            val infiniteTransition = rememberInfiniteTransition()
                            val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")

                            val styleParams = getLyricsStyleParams(
                                index = index,
                                activeIndex = synchronizedLyrics.index,
                                showThumbnail = showlyricsthumbnail,
                                lightTheme = lightTheme,
                                lyricsColor = lyricsColor,
                                lyricsOutline = lyricsOutline,
                                lyricsAlignment = lyricsAlignment,
                                lyricsHighlight = lyricsHighlight,
                                lyricsSizeAnimate = lyricsSizeAnimate,
                                colorPaletteMode = colorPaletteMode,
                                colorPalette = colorPalette(),
                                fontSize = fontSize,
                                customSize = customSize,
                                offset = offset
                            )


                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (!showlyricsthumbnail) {

                                    if (styleParams.useOutline) {
                                        val style = getOutlineTextStyle(styleParams, customSize)
                                        val rowModifier = getRowModifier(
                                            params = styleParams,
                                            isActive = isActive,
                                            index = index,
                                            activeIndex = synchronizedLyrics.index,
                                            alignment = lyricsAlignment,
                                            sizeAnimate = lyricsSizeAnimate,
                                            clickLyricsText = clickLyricsText,
                                            binder = binder,
                                            sentence = sentence,
                                            positionProvider = positionProvider,
                                            onDismiss = onDismiss,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp),
                                            applyHighlight = true,
                                            highlightColor = styleParams.highlightColor
                                        ).align(getAlignment(lyricsAlignment))

                                        if (isShowingSynchronizedWordByWordLyrics) LyricRow(sentence, isActive, progressInsideLine, style, rowModifier) else BasicText(text = translatedText, style = style, modifier = rowModifier)
                                    }

                                    else if (styleParams.useRainbow) {
                                        val style = getRainbowTextStyle(
                                            params = styleParams,
                                            offset = offset,
                                            isActive = isActive
                                        )
                                        val rowModifier = getRowModifier(
                                            params = styleParams,
                                            isActive = isActive,
                                            index = index,
                                            activeIndex = synchronizedLyrics.index,
                                            alignment = lyricsAlignment,
                                            sizeAnimate = lyricsSizeAnimate,
                                            clickLyricsText = clickLyricsText,
                                            binder = binder,
                                            sentence = sentence,
                                            positionProvider = positionProvider,
                                            onDismiss = onDismiss,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp),
                                            applyHighlight = true,
                                            highlightColor = styleParams.highlightColor
                                        ).align(getAlignment(lyricsAlignment))

                                        if (isShowingSynchronizedWordByWordLyrics) LyricRow(sentence, isActive, progressInsideLine, style, rowModifier) else BasicText(text = translatedText, style = style, modifier = rowModifier)
                                    }

                                    else {
                                        val style = getStandardTextStyle(styleParams, isActive)
                                        val rowModifier = getRowModifier(
                                            params = styleParams,
                                            isActive = isActive,
                                            index = index,
                                            activeIndex = synchronizedLyrics.index,
                                            alignment = lyricsAlignment,
                                            sizeAnimate = lyricsSizeAnimate,
                                            clickLyricsText = clickLyricsText,
                                            binder = binder,
                                            sentence = sentence,
                                            positionProvider = positionProvider,
                                            onDismiss = onDismiss,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp),
                                            applyHighlight = true,
                                            highlightColor = styleParams.highlightColor
                                        ).align(getAlignment(lyricsAlignment))

                                        if (isShowingSynchronizedWordByWordLyrics) LyricRow(sentence, isActive, progressInsideLine, style, rowModifier) else BasicText(text = translatedText, style = style, modifier = rowModifier)
                                    }
                                }

                                else {
                                    val style = getThumbnailTextStyle(styleParams, isActive)
                                    val rowModifier = Modifier
                                        .padding(vertical = 4.dp, horizontal = 32.dp)
                                        .align(getAlignment(lyricsAlignment))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = if (clickLyricsText) ripple(true) else null,
                                            onClick = {
                                                if (clickLyricsText) seekToLyric(
                                                    binder,
                                                    sentence
                                                ) else onDismiss()
                                            })
                                    if (isShowingSynchronizedWordByWordLyrics) LyricRow(sentence, isActive, progressInsideLine, style, rowModifier) else BasicText(text = translatedText, style = style, modifier = rowModifier)
                                }
                            }
                        }
                        item(key = "footer", contentType = 0) { Spacer(modifier = Modifier.height(thumbnailSize)) }
                    }
                } else {

                    var translatedText by remember { mutableStateOf("") }
                    if (showSecondLine || translateEnabled || romanization != Romanization.Off) {
                        val mutState = remember { mutableStateOf("") }
                        // todo  To improve actually not stable
                        if (translateEnabled)
                            translateLyrics(mutState, lyricsText, false, languageDestination)
                        translatedText = mutState.value
                    } else { translatedText = lyricsText }

                    Column(modifier = Modifier
                        .verticalFadingEdge()
                        .background(
                            if (isDisplayed && !showlyricsthumbnail) if (lyricsBackground == LyricsBackground.Black) Color.Black.copy(
                                0.4f
                            ) else if (lyricsBackground == LyricsBackground.White) Color.White.copy(
                                0.4f
                            ) else Color.Transparent else Color.Transparent
                        )) {
                        Box(modifier = Modifier
                            .verticalFadingEdge()
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(vertical = size / 4, horizontal = 32.dp), contentAlignment = Alignment.Center) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")

                            val styleParams = getLyricsStyleParams(0, 0, showlyricsthumbnail, lightTheme, lyricsColor, lyricsOutline, lyricsAlignment, lyricsHighlight, lyricsSizeAnimate, colorPaletteMode, colorPalette(), fontSize, customSize, offset)

                            if (!showlyricsthumbnail) {
                                if (styleParams.useOutline) {
                                    BasicText(text = translatedText, style = getFixedOutlineTextStyle(styleParams), modifier = Modifier.align(getAlignment(lyricsAlignment)))
                                } else if (styleParams.useRainbow) {
                                    BasicText(text = translatedText, style = getFixedRainbowTextStyle(styleParams, offset), modifier = Modifier.align(getAlignment(lyricsAlignment)))
                                } else if (styleParams.useStandard) {
                                    BasicText(text = translatedText, style = getFixedStandardTextStyle(styleParams), modifier = Modifier.align(getAlignment(lyricsAlignment)))
                                }
                            } else {
                                BasicText(text = translatedText, style = getThumbnailTextStyle(styleParams, isActive = true), modifier = Modifier.align(getAlignment(lyricsAlignment)))
                            }
                        }
                    }
                }
            }

            // Placeholder
            if ((lyricsText.isEmpty() && !isError) || showPlaceholder) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.shimmer()) { repeat(4) { TextPlaceholder(color = colorPalette().onOverlayShimmer, modifier = Modifier.alpha(1f - it * 0.1f)) } }
            }


            if (trailingContent != null) {
                Box(modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.4f)) { trailingContent() }
            }

            //Controls
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(if (trailingContent == null) 0.30f else 0.22f)
            ) {
                if (isLandscape && !showlyricsthumbnail)
                    IconButton(
                        icon = R.drawable.chevron_back,
                        color = colorPalette().accent,
                        enabled = true,
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .align(Alignment.BottomStart)
                            .size(30.dp)
                    )

                if (showlyricsthumbnail)
                    IconButton(
                        icon = R.drawable.text,
                        color = DefaultDarkColorPalette.text,
                        enabled = true,
                        onClick = {
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.light),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Light
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.medium),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Medium
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.heavy),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Heavy
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.large),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Large
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                    )
            }
            if (!showlyricsthumbnail && isDisplayed && isLandscape && landscapeControls) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (lightTheme) Color.White.copy(0.5f) else Color.Black.copy(
                                        0.5f
                                    )
                                ),
                                startY = 0f,
                                endY = POSITIVE_INFINITY
                            ),
                        )
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ){
                    Image(
                        painter = painterResource(R.drawable.play_skip_back),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    if (jumpPrevious == "") jumpPrevious = "0"
                                    if (binder?.player?.hasPreviousMediaItem() == false || (jumpPrevious != "0" && (binder?.player?.currentPosition
                                            ?: 0) > jumpPrevious.toInt() * 1000)
                                    ) {
                                        if (binder?.player?.currentMediaItem?.isLocal == true) {
                                            binder?.player?.seekTo(0)
                                        } else {
                                            binder?.onlinePlayer?.seekTo(0f)
                                        }
                                    } else binder?.player?.playPrevious()
                                    if (effectRotationEnabled) isRotated = !isRotated
                                }
                            )
                            .rotate(rotationAngle)
                            .padding(horizontal = 15.dp)
                            .size(30.dp)

                    )
                    Box {
                        Box(modifier = Modifier
                            .align(Alignment.Center)
                            .size(45.dp)
                            .background(colorPalette().accent, RoundedCornerShape(15.dp))
                        ){}
                        Image(
                            painter = painterResource(if (binder?.player?.isPlaying == true) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (colorPaletteName == ColorPaletteName.PureBlack) Color.Black else colorPalette().text),
                            modifier = Modifier
                                .clickable(
                                    indication = ripple(bounded = false),
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (binder?.player?.isPlaying == true) {
                                            binder.callPause({})
                                        } else {
                                            binder?.player?.play()
                                        }
                                    },
                                )
                                .align(Alignment.Center)
                                .rotate(rotationAngle)
                                .padding(horizontal = 15.dp)
                                .size(36.dp)

                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.play_skip_forward),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    binder?.player?.playNext()
                                    if (effectRotationEnabled) isRotated = !isRotated
                                }
                            )
                            .rotate(rotationAngle)
                            .padding(horizontal = 15.dp)
                            .size(30.dp)

                    )
                }

            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.3f)
                    .padding(PaddingValues(start = 15.dp, bottom = 10.dp))
                    .background(colorPalette().accent.copy(alpha = 0.4f), getRoundnessShape())
            ) {
                Text(
                    text = when {
                        isShowingSynchronizedLyrics && isShowingSynchronizedWordByWordLyrics -> stringResource(R.string.lyrics_word_by_word)
                        isShowingSynchronizedLyrics && !isShowingSynchronizedWordByWordLyrics -> stringResource(R.string.lyrics_line_by_line)
                        else -> stringResource(R.string.lyrics_fulltext)
                    },
                    style = typography().xxs.medium.color(colorPalette().text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .height(24.dp)
                        .padding(top = 5.dp)
                        .align(Alignment.Center)
                        .clickable {
                            when {
                                isShowingSynchronizedLyrics && isShowingSynchronizedWordByWordLyrics -> {
                                    isShowingSynchronizedWordByWordLyrics = false
                                }

                                isShowingSynchronizedLyrics && !isShowingSynchronizedWordByWordLyrics -> {
                                    isShowingSynchronizedLyrics = false
                                }

                                else -> {
                                    isShowingSynchronizedLyrics = true
                                    isShowingSynchronizedWordByWordLyrics = true
                                }
                            }
                        }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.2f)
            ) {


                if (showlyricsthumbnail)
                    IconButton(
                        icon = R.drawable.translate,
                        color = if (translateEnabled) colorPalette().text else colorPalette().textDisabled,
                        enabled = true,
                        onClick = {
                            translateEnabled = !translateEnabled
                            if (translateEnabled) showLanguagesList = true
                        },
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .align(Alignment.BottomStart)
                            .size(24.dp)
                    )


                Image(
                    painter = painterResource(R.drawable.ellipsis_vertical),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(DefaultDarkColorPalette.text),
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .clickable(
                            indication = ripple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                menuState.display {
                                    Menu {
                                        if (isLandscape && !showlyricsthumbnail) {
                                            MenuEntry(
                                                icon = if (landscapeControls) R.drawable.checkmark else R.drawable.play,
                                                text = stringResource(R.string.toggle_controls_landscape),
                                                enabled = true,
                                                onClick = {
                                                    menuState.hide()
                                                    landscapeControls = !landscapeControls
                                                }
                                            )
                                        }
                                        MenuEntry(
                                            icon = R.drawable.text,
                                            enabled = true,
                                            text = stringResource(R.string.lyricsalignment),
                                            onClick = {
                                                menuState.display {
                                                    Menu {
                                                        MenuEntry(
                                                            icon = R.drawable.arrow_left,
                                                            text = stringResource(R.string.direction_left),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                lyricsAlignment =
                                                                    LyricsAlignment.Left
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = R.drawable.arrow_down,
                                                            text = stringResource(R.string.center),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                lyricsAlignment =
                                                                    LyricsAlignment.Center
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = R.drawable.arrow_right,
                                                            text = stringResource(R.string.direction_right),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                lyricsAlignment =
                                                                    LyricsAlignment.Right
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.text,
                                                enabled = true,
                                                text = stringResource(R.string.lyrics_size),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.light),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Light
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.medium),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Medium
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.heavy),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Heavy
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.large),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Large
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.custom),
                                                                secondaryText = stringResource(R.string.lyricsSizeSecondary),
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Custom
                                                                },
                                                                onLongClick = {
                                                                    showLyricsSizeDialog =
                                                                        !showLyricsSizeDialog
                                                                },
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.droplet,
                                                enabled = true,
                                                text = stringResource(R.string.lyricscolor),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.theme),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.Thememode
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.Black
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.accent),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor = LyricsColor.Accent
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.fluidrainbow),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.FluidRainbow
                                                                }
                                                            )
                                                            /*MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.fluidtheme),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor = LyricsColor.FluidTheme
                                                                }
                                                            )*/
                                                        }
                                                    }
                                                }
                                            )
                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.horizontal_bold_line,
                                                enabled = true,
                                                text = stringResource(R.string.lyricsoutline),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.close,
                                                                text = stringResource(R.string.none),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.None
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_bold_line,
                                                                text = stringResource(R.string.theme),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.Thememode
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_bold_line,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_bold_line,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.Black
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.fluidrainbow),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.Rainbow
                                                                }
                                                            )
                                                            if (isShowingSynchronizedLyrics) {
                                                                MenuEntry(
                                                                    icon = R.drawable.droplet,
                                                                    text = stringResource(R.string.glow),
                                                                    secondaryText = "",
                                                                    onClick = {
                                                                        menuState.hide()
                                                                        lyricsOutline =
                                                                            LyricsOutline.Glow
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            )

                                        //if (!showlyricsthumbnail)
                                        MenuEntry(
                                            icon = R.drawable.translate,
                                            text = stringResource(
                                                R.string.translate_to,
                                                otherLanguageApp
                                            ),
                                            enabled = true,
                                            onClick = {
                                                menuState.hide()
                                                translateEnabled = true
                                            }
                                        )
                                        MenuEntry(
                                            icon = R.drawable.translate,
                                            text = stringResource(R.string.translate_to_other_language),
                                            enabled = true,
                                            onClick = {
                                                menuState.hide()
                                                showLanguagesList = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = if (romanization == Romanization.Original || romanization == Romanization.Translated || romanization == Romanization.Both) R.drawable.checkmark else R.drawable.text,
                                            enabled = true,
                                            text = stringResource(R.string.toggle_romanization),
                                            onClick = {
                                                menuState.display {
                                                    Menu {
                                                        MenuEntry(
                                                            icon = if (romanization == Romanization.Off) R.drawable.checkmark else R.drawable.text,
                                                            text = stringResource(R.string.turn_off),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                romanization =
                                                                    Romanization.Off
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = if (romanization == Romanization.Original || (romanization == Romanization.Both && !showSecondLine)) R.drawable.checkmark else R.drawable.text,
                                                            text = stringResource(R.string.original_lyrics),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                romanization =
                                                                    Romanization.Original
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = if (romanization == Romanization.Translated) R.drawable.checkmark else R.drawable.text,
                                                            text = stringResource(R.string.translated_lyrics),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                romanization =
                                                                    Romanization.Translated
                                                            }
                                                        )
                                                        if (showSecondLine) {
                                                            MenuEntry(
                                                                icon = if (romanization == Romanization.Both) R.drawable.checkmark else R.drawable.text,
                                                                text = stringResource(R.string.both),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    romanization =
                                                                        Romanization.Both
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                        MenuEntry(
                                            icon = if (showSecondLine) R.drawable.checkmark else R.drawable.close,
                                            text = stringResource(R.string.showsecondline),
                                            enabled = true,
                                            onClick = {
                                                menuState.hide()
                                                showSecondLine = !showSecondLine
                                            }
                                        )

                                        if (!showlyricsthumbnail && isShowingSynchronizedLyrics) {
                                            MenuEntry(
                                                icon = if (lyricsSizeAnimate) R.drawable.checkmark else R.drawable.close,
                                                text = stringResource(R.string.lyricsanimate),
                                                enabled = true,
                                                onClick = {
                                                    menuState.hide()
                                                    lyricsSizeAnimate = !lyricsSizeAnimate
                                                }
                                            )
                                        }

                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.horizontal_bold_line_rounded,
                                                enabled = true,
                                                text = stringResource(R.string.highlight),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_straight_line,
                                                                text = stringResource(R.string.none),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsHighlight =
                                                                        LyricsHighlight.None
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_straight_line,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsHighlight =
                                                                        LyricsHighlight.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_straight_line,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsHighlight =
                                                                        LyricsHighlight.Black
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            )

                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.droplet,
                                                enabled = true,
                                                text = stringResource(R.string.lyricsbackground),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.none),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsBackground =
                                                                        LyricsBackground.None
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsBackground =
                                                                        LyricsBackground.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsBackground =
                                                                        LyricsBackground.Black
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            )

                                        MenuEntry(
                                            icon = R.drawable.song_lyrics,
                                            text = stringResource(R.string.lyrics_type),
                                            onClick = {
                                                menuState.display {
                                                    Menu {
                                                        MenuEntry(
                                                            icon = R.drawable.text,
                                                            text = stringResource(R.string.unsynchronized_lyrics),
                                                            secondaryText = "",
                                                            trailingContent = {
                                                                if (!isShowingSynchronizedLyrics && !isShowingSynchronizedWordByWordLyrics)
                                                                    Image(
                                                                        painter = painterResource(R.drawable.checkmark),
                                                                        contentDescription = null,
                                                                        colorFilter = ColorFilter.tint(
                                                                            colorPalette().text
                                                                        )
                                                                    )
                                                            },
                                                            onClick = {
                                                                isShowingSynchronizedLyrics = false
                                                                isShowingSynchronizedWordByWordLyrics =
                                                                    false
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = R.drawable.time,
                                                            text = stringResource(R.string.synchronized_lyrics),
                                                            secondaryText = stringResource(
                                                                R.string.provided_by
                                                            ) + " kugou.com and LrcLib.net",
                                                            trailingContent = {
                                                                if (isShowingSynchronizedLyrics && !isShowingSynchronizedWordByWordLyrics)
                                                                    Image(
                                                                        painter = painterResource(R.drawable.checkmark),
                                                                        contentDescription = null,
                                                                        colorFilter = ColorFilter.tint(
                                                                            colorPalette().text
                                                                        )
                                                                    )
                                                            },
                                                            onClick = {
                                                                isShowingSynchronizedLyrics = true
                                                                isShowingSynchronizedWordByWordLyrics =
                                                                    false
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = R.drawable.time,
                                                            text = stringResource(R.string.synchronized_karaoke_lyrics),
                                                            secondaryText = stringResource(
                                                                R.string.provided_by
                                                            ) + " SyncLRC.tharuk.pro",
                                                            trailingContent = {
                                                                if (isShowingSynchronizedWordByWordLyrics)
                                                                    Image(
                                                                        painter = painterResource(R.drawable.checkmark),
                                                                        contentDescription = null,
                                                                        colorFilter = ColorFilter.tint(
                                                                            colorPalette().text
                                                                        )
                                                                    )
                                                            },
                                                            onClick = {
                                                                isShowingSynchronizedWordByWordLyrics =
                                                                    true
                                                                isShowingSynchronizedLyrics = true
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.title_edit,
                                            text = stringResource(R.string.edit_lyrics),
                                            onClick = {
                                                menuState.hide()
                                                isEditing = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.copy,
                                            text = stringResource(R.string.copy_lyrics),
                                            onClick = {
                                                menuState.hide()
                                                copyToClipboard = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.copy,
                                            text = stringResource(R.string.copy_translated_lyrics),
                                            onClick = {
                                                menuState.hide()
                                                copyTranslatedToClipboard = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.search,
                                            text = stringResource(R.string.search_lyrics_online),
                                            onClick = {
                                                menuState.hide()
                                                val mediaMetadata =
                                                    binder?.player?.currentMediaItem?.mediaMetadata
                                                        ?: return@MenuEntry

                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                            putExtra(
                                                                SearchManager.QUERY,
                                                                "${cleanPrefix(mediaMetadata.title.toString())} ${mediaMetadata.artist} lyrics"
                                                            )
                                                        }
                                                    )
                                                } catch (e: ActivityNotFoundException) {
                                                    SmartMessage(
                                                        context.resources.getString(R.string.info_not_find_app_browse_internet),
                                                        type = PopupType.Warning, context = context
                                                    )
                                                }
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.sync,
                                            text = stringResource(R.string.fetch_lyrics_again),
                                            enabled = lyricsText != "",
                                            onClick = {
                                                menuState.hide()
//                                                Database.asyncTransaction {
//                                                    upsert(
//                                                        Lyrics(
//                                                            songId = mediaId,
//                                                            fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else null,
//                                                            synced = if (isShowingSynchronizedLyrics) null else lyrics?.synced,
//                                                        )
//                                                    )
//                                                }
                                            }
                                        )

                                        if (isShowingSynchronizedLyrics) {
                                            MenuEntry(
                                                icon = R.drawable.sync,
                                                text = stringResource(R.string.pick_from) + " LrcLib.net",
                                                onClick = {
                                                    menuState.hide()
                                                    isPicking = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        .padding(all = 8.dp)
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}


@Composable
private fun getLyricsStyleParams(
    index: Int,
    activeIndex: Int,
    showThumbnail: Boolean,
    lightTheme: Boolean,
    lyricsColor: LyricsColor,
    lyricsOutline: LyricsOutline,
    lyricsAlignment: LyricsAlignment,
    lyricsHighlight: LyricsHighlight,
    lyricsSizeAnimate: Boolean,
    colorPaletteMode: ColorPaletteMode,
    colorPalette: ColorPalette,
    fontSize: LyricsFontSize,
    customSize: Float,
    offset: Float
): LyricsStyleParams {

    val isActive = index == activeIndex
    val highlightColor = when (lyricsHighlight) {
        LyricsHighlight.White -> Color.White.copy(0.5f)
        LyricsHighlight.Black -> Color.Black.copy(0.5f)
        else -> Color.Transparent
    }

    val useOutline = !showThumbnail && (lyricsOutline == LyricsOutline.White || lyricsOutline == LyricsOutline.Black || lyricsOutline == LyricsOutline.Thememode || lyricsOutline == LyricsOutline.Rainbow)
    val useRainbow = !showThumbnail && (lyricsColor == LyricsColor.FluidRainbow)
    val useStandard = !showThumbnail && !useOutline && !useRainbow && (lyricsColor == LyricsColor.Thememode || lyricsColor == LyricsColor.White || lyricsColor == LyricsColor.Black || lyricsColor == LyricsColor.Accent)

    return LyricsStyleParams(
        isActive, showThumbnail, lightTheme, lyricsColor, lyricsOutline, lyricsAlignment, lyricsHighlight, lyricsSizeAnimate,
        colorPaletteMode, colorPalette, fontSize, highlightColor, offset, customSize, useOutline, useRainbow, useStandard
    )
}

data class LyricsStyleParams(
    val isActive: Boolean,
    val showThumbnail: Boolean,
    val lightTheme: Boolean,
    val lyricsColor: LyricsColor,
    val lyricsOutline: LyricsOutline,
    val lyricsAlignment: LyricsAlignment,
    val lyricsHighlight: LyricsHighlight,
    val lyricsSizeAnimate: Boolean,
    val colorPaletteMode: ColorPaletteMode,
    val colorPalette: ColorPalette,
    val fontSize: LyricsFontSize,
    val highlightColor: Color,
    val offset: Float,
    val customSize: Float,
    val useOutline: Boolean,
    val useRainbow: Boolean,
    val useStandard: Boolean
)


@Composable
private fun getThumbnailTextStyle(params: LyricsStyleParams, isActive: Boolean): TextStyle {
    val color = if (params.isActive) PureBlackColorPalette.text else PureBlackColorPalette.textDisabled
    return TextStyle(color = color, fontWeight = FontWeight.Medium, fontSize = getFontSize(params.fontSize, params.customSize))
}


fun getOutlineTextStyle(params: LyricsStyleParams, customSize: Float): TextStyle {
    val color = when (params.lyricsOutline) {
        LyricsOutline.White -> Color.White
        LyricsOutline.Black -> Color.Black
        LyricsOutline.Thememode -> if (params.colorPaletteMode == ColorPaletteMode.Light) Color.White else Color.Black
        else -> Color.Transparent
    }
    val width = getStrokeWidth(params.fontSize, params.lyricsOutline, params.isActive, customSize)
    return TextStyle(color = color, fontSize = getFontSize(params.fontSize, customSize), drawStyle = Stroke(width = width, join = StrokeJoin.Round))
}


@Composable
private fun getRainbowBrush(params: LyricsStyleParams, isActive: Boolean): ShaderBrush {
    val colors = if (params.lightTheme) RainbowColors else RainbowColorsdark
    val offset = params.offset
    return remember(offset) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val wo = size.width * offset; val ho = size.height * offset
                return LinearGradientShader(colors = if (params.isActive) colors else RainbowColors2, from = Offset(wo, ho), to = Offset(wo + size.width, ho + size.height), tileMode = TileMode.Mirror)
            }
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
private suspend fun fetchLyricsIfNeeded(
    mediaId: String, artistName: String, title: String, currentLyrics: Lyrics?,
    isShowingSynchronizedWordByWordLyrics: Boolean, isShowingSynchronizedLyrics: Boolean,
    mediaMetadata: MediaMetadata, durationProvider: () -> Long, playerEnableLyricsPopupMessage: Boolean,
    context: Context,
    onCheckedLrc: (Boolean) -> Unit,
    onCheckedKugou: (Boolean) -> Unit,
    onCheckedInnertube: (Boolean) -> Unit,
    onCheckedSyncLrc: (Boolean) -> Unit,
    onError: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {

        if (isShowingSynchronizedWordByWordLyrics && currentLyrics?.lrcSynced == null) {
            launch(Dispatchers.Main) {
                syncLRCfetchLyrics(
                    context = context,
                    artist = artistName,
                    title = title,
                    onSuccess = { syncLRClyrics ->
                        Timber.d("fetchLyricsIfNeeded success $syncLRClyrics")
                        if (syncLRClyrics.type != SyncLRCType.KARAOKE)
                            SmartMessage(
                                "Lyrics Karaoke not available",
                                type = PopupType.Warning,
                                context = context
                            )

                        val lyricsToUpdate = when (syncLRClyrics.type) {
                            SyncLRCType.KARAOKE -> Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = currentLyrics?.synced, lrcSynced = syncLRClyrics.lyrics)
                            SyncLRCType.SYNCED -> Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = syncLRClyrics.lyrics, lrcSynced = currentLyrics?.lrcSynced)
                            SyncLRCType.PLAIN -> Lyrics(songId = mediaId, fixed = syncLRClyrics.lyrics, synced = currentLyrics?.synced, lrcSynced = currentLyrics?.lrcSynced)
                            else -> null
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            Timber.d("fetchLyricsIfNeeded upsert lyricsToUpdate $lyricsToUpdate")
                            lyricsToUpdate?.let { Database.upsert(it) }
                        }
                        onError(false)
                        onCheckedSyncLrc(true)
                    },
                    onError = {
                        Timber.d("fetchLyricsIfNeeded error $it")
                        onError(true)
                    }
                )

            }
        } else if (isShowingSynchronizedLyrics && !isShowingSynchronizedWordByWordLyrics && currentLyrics?.synced == null) {
            var duration = withContext(Dispatchers.Main) { durationProvider() }
            while (duration == C.TIME_UNSET) {
                delay(100); duration = withContext(Dispatchers.Main) { durationProvider() }
            }

            runCatching {
                LrcLib.lyrics(artist = artistName, title = title, duration = duration.milliseconds, album = mediaMetadata.albumTitle?.toString())?.onSuccess {
                    if ((it?.text?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true) && playerEnableLyricsPopupMessage)
                        SmartMessage(context.resources.getString(R.string.info_lyrics_found_on_s).format("LrcLib.net"), type = PopupType.Success, context = context)
                    else if (playerEnableLyricsPopupMessage)
                        SmartMessage(context.resources.getString(R.string.info_lyrics_not_found_on_s).format("LrcLib.net"), type = PopupType.Error, durationLong = true, context = context)

                    onError(false)
                    Database.upsert(Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = it?.text.orEmpty()))
                    onCheckedLrc(true)
                }?.onFailure {
                    if (playerEnableLyricsPopupMessage)
                        SmartMessage(context.resources.getString(R.string.info_lyrics_not_found_on_s_try_on_s).format("LrcLib.net", "KuGou.com"), type = PopupType.Error, durationLong = true, context = context)
                    onCheckedLrc(true)

                    runCatching {
                        KuGou.lyrics(artist = mediaMetadata.artist?.toString() ?: "", title = cleanPrefix(mediaMetadata.title?.toString() ?: ""), duration = duration / 1000)?.onSuccess {
                            if ((it?.value?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true) && playerEnableLyricsPopupMessage)
                                SmartMessage(context.resources.getString(R.string.info_lyrics_found_on_s).format("KuGou.com"), type = PopupType.Success, context = context)
                            else if (playerEnableLyricsPopupMessage)
                                SmartMessage(context.resources.getString(R.string.info_lyrics_not_found_on_s).format("KuGou.com"), type = PopupType.Error, durationLong = true, context = context)

                            onError(false)
                            Database.upsert(Lyrics(songId = mediaId, fixed = currentLyrics?.fixed, synced = it?.value.orEmpty()))
                            onCheckedKugou(true)
                        }?.onFailure {
                            if (playerEnableLyricsPopupMessage)
                                SmartMessage(context.resources.getString(R.string.info_lyrics_not_found_on_s).format("KuGou.com"), type = PopupType.Error, durationLong = true, context = context)
                            onError(true)
                        }
                    }.onFailure { Timber.e("Lyrics Kugou error ${it.stackTraceToString()}") }
                }
            }.onFailure { Timber.e("Lyrics get error ${it.stackTraceToString()}") }

        } else if (!isShowingSynchronizedLyrics && currentLyrics?.fixed == null) {
            onError(false)
            runCatching {
                Environment.lyrics(NextBody(videoId = mediaId))?.onSuccess { fixedLyrics ->
                    Database.upsert(Lyrics(songId = mediaId, fixed = fixedLyrics ?: "", synced = currentLyrics?.synced))
                }?.onFailure { onError(true) }
            }.onFailure { Timber.e("Lyrics Innertube error ${it.stackTraceToString()}") }
            onCheckedInnertube(true)
        }
    }
}

fun getFontSize(fontSize: LyricsFontSize, customSize: Float): TextUnit {
    return when (fontSize) {
        LyricsFontSize.Light -> 18.sp  // T ipico Medium/Large, adatta se necessario
        LyricsFontSize.Medium -> 22.sp
        LyricsFontSize.Heavy -> 26.sp
        LyricsFontSize.Large -> 32.sp
        LyricsFontSize.Custom -> customSize.sp
    }
}


fun getStrokeWidth(
    fontSize: LyricsFontSize,
    lyricsOutline: LyricsOutline,
    isActive: Boolean,
    customSize: Float
): Float {
    return when (fontSize) {
        LyricsFontSize.Large -> if (lyricsOutline == LyricsOutline.White) 6.0f
        else if (lyricsOutline == LyricsOutline.Black) 10.0f
        else 0f
        LyricsFontSize.Heavy -> if (lyricsOutline == LyricsOutline.White) 3f
        else if (lyricsOutline == LyricsOutline.Black) 7f
        else 0f
        LyricsFontSize.Medium -> if (lyricsOutline == LyricsOutline.White) 2f
        else if (lyricsOutline == LyricsOutline.Black) 6f
        else 0f
        LyricsFontSize.Light -> if (lyricsOutline == LyricsOutline.White) 1.3f
        else if (lyricsOutline == LyricsOutline.Black) 5.3f
        else 0f
        LyricsFontSize.Custom -> if (lyricsOutline == LyricsOutline.White) (customSize / 5.6f)
        else if (lyricsOutline == LyricsOutline.Black) (customSize / 3.4f)
        else 0f
    }
}

/**
 * Restituisce l'allineamento generico per il Modifier.align().
 * Nota: Modifier.align usa Alignment (es. Alignment.Center), non Alignment.Horizontal.
 */
fun getAlignment(lyricsAlignment: LyricsAlignment): Alignment {
    return when (lyricsAlignment) {
        LyricsAlignment.Left -> Alignment.CenterStart
        LyricsAlignment.Right -> Alignment.CenterEnd
        LyricsAlignment.Center -> Alignment.Center
    }
}

/**
 * Esegue il seek del player al timestamp della riga cliccata.
 */
@androidx.annotation.OptIn(UnstableApi::class)
fun seekToLyric(binder: PlayerService.Binder?, sentence: LyricLine) {
    val positionMs = sentence.timeMs
    if (binder?.player?.currentMediaItem?.isLocal == true) {
        Timber.d("Seeking local player to $positionMs ms")
        binder?.player?.seekTo(positionMs)
    } else {
        val positionSeconds = positionMs / 1000f
        Timber.d("Seeking online player to $positionSeconds s")
        binder?.onlinePlayer?.seekTo(positionSeconds)
    }
}


fun getRainbowBrush(lightTheme: Boolean, offset: Float, isActive: Boolean): ShaderBrush {
    val colors = if (lightTheme) RainbowColors else RainbowColorsdark
    val colorsDimmed = RainbowColors2

    val targetColors = if (isActive) colors else colorsDimmed

    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val widthOffset = size.width * offset
            val heightOffset = size.height * offset
            return LinearGradientShader(
                colors = targetColors,
                from = Offset(widthOffset, heightOffset),
                to = Offset(widthOffset + size.width, heightOffset + size.height),
                tileMode = TileMode.Mirror
            )
        }
    }
}


fun getFixedOutlineTextStyle(params: LyricsStyleParams): TextStyle {
    val color = when (params.lyricsOutline) {
        LyricsOutline.White -> Color.White
        LyricsOutline.Black -> Color.Black
        LyricsOutline.Thememode -> if (params.colorPaletteMode == ColorPaletteMode.Light) Color.White else Color.Black
        else -> Color.Transparent
    }

    val width = getStrokeWidth(params.fontSize, params.lyricsOutline, true, params.customSize)

    return TextStyle(
        textAlign = when(params.lyricsAlignment) {
            LyricsAlignment.Left -> TextAlign.Start
            LyricsAlignment.Right -> TextAlign.End
            else -> TextAlign.Center
        },
        color = color,
        fontSize = getFontSize(params.fontSize, params.customSize),
        drawStyle = Stroke(width = width, join = StrokeJoin.Round)
    )
}


fun getFixedStandardTextStyle(params: LyricsStyleParams): TextStyle {
    val color = when (params.lyricsColor) {
        LyricsColor.White -> Color.White
        LyricsColor.Black -> Color.Black
        LyricsColor.Thememode -> params.colorPalette.text
        LyricsColor.Accent -> params.colorPalette.accent
        else -> params.colorPalette.text
    }
    return TextStyle(
        fontWeight = FontWeight.Medium,
        color = color,
        fontSize = getFontSize(params.fontSize, params.customSize),
        textAlign = when(params.lyricsAlignment) {
            LyricsAlignment.Left -> TextAlign.Start
            LyricsAlignment.Right -> TextAlign.End
            else -> TextAlign.Center
        }
    )
}


fun getStandardTextStyle(params: LyricsStyleParams, isActive: Boolean): TextStyle {
    val color = when (params.lyricsColor) {
        LyricsColor.White -> Color.White
        LyricsColor.Black -> Color.Black
        LyricsColor.Thememode -> params.colorPalette.text
        LyricsColor.Accent -> params.colorPalette.accent
        else -> params.colorPalette.text
    }
    return TextStyle(
        fontWeight = FontWeight.Medium,
        color = color,
        fontSize = getFontSize(params.fontSize, params.customSize),
        textAlign = when(params.lyricsAlignment) {
            LyricsAlignment.Left -> TextAlign.Start
            LyricsAlignment.Right -> TextAlign.End
            else -> TextAlign.Center
        }
    )
}


fun getRainbowTextStyle(params: LyricsStyleParams, offset: Float, isActive: Boolean): TextStyle {
    return TextStyle(
        brush = getRainbowBrush(params.lightTheme, offset, isActive),
        fontSize = getFontSize(params.fontSize, params.customSize),
        fontWeight = FontWeight.Medium,
        drawStyle = Stroke(
            width = getStrokeWidth(params.fontSize, params.lyricsOutline, isActive, params.customSize),
            join = StrokeJoin.Round
        )
    )
}


/**
 * Costruisce il Modifier per una riga di testo sincronizzata.
 * Gestisce padding, allineamento, animazioni, click e highlight.
 */
@Composable
@UnstableApi
fun getRowModifier(
    params: LyricsStyleParams,
    isActive: Boolean,
    index: Int,
    activeIndex: Int,
    alignment: LyricsAlignment,
    sizeAnimate: Boolean,
    clickLyricsText: Boolean,
    binder: PlayerService.Binder?,
    sentence: LyricLine,
    positionProvider: () -> Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    applyHighlight: Boolean = false,
    highlightColor: Color = Color.Transparent
): Modifier {
    return modifier
        .padding(vertical = 4.dp, horizontal = 32.dp)
        .applyIf(sizeAnimate) {
            graphicsLayer {
                transformOrigin = when (alignment) {
                    LyricsAlignment.Center -> TransformOrigin(0.5f, 0.5f)
                    LyricsAlignment.Left -> TransformOrigin(0f, 0.5f)
                    LyricsAlignment.Right -> TransformOrigin(1f, 0.5f)
                }
                scaleY = if (isActive) 1.05f else 0.85f
                scaleX = if (isActive) 1.05f else 0.85f
            }
        }
        .graphicsLayer { alpha = if (isActive) 1f else 0.6f }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = if (clickLyricsText) ripple(true) else null,
            onClick = {
                if (clickLyricsText) {
                    seekToLyric(binder, sentence)
                } else {
                    onDismiss()
                }
            }
        )

        .applyIf(applyHighlight && isActive) {
            background(highlightColor, RoundedCornerShape(6.dp))
            fillMaxWidth()
        }
}

fun getFixedRainbowTextStyle(params: LyricsStyleParams, offset: Float): TextStyle {
    return TextStyle(
        brush = getRainbowBrush(params.lightTheme, offset, true),
        fontSize = getFontSize(params.fontSize, 20f),
        fontWeight = FontWeight.Medium,
        textAlign = when(params.lyricsAlignment) {
            LyricsAlignment.Left -> TextAlign.Start
            LyricsAlignment.Right -> TextAlign.End
            else -> TextAlign.Center
        },
        drawStyle = Stroke(
            width = getStrokeWidth(params.fontSize, params.lyricsOutline, true, 20f),
            join = StrokeJoin.Round
        )
    )
}



@Composable
fun LyricRow(
    line: LyricLine,
    isActive: Boolean,
    progressMs: Long,
    style1: TextStyle,
    modifier: Modifier
) {

//    if (isActive)
//        Timber.d("Lyrics LyricRow isActive: $isActive  line $line")

    val isLineStarted = progressMs >= 0

    val annotatedString = buildAnnotatedString {
        line.words.forEach { word ->

            val wordEndTime = word.startTimeInTheLineMs + word.durationMs

            val isWordPassed = isLineStarted && progressMs >= wordEndTime

            val isWordActive = isLineStarted &&
                    (progressMs >= word.startTimeInTheLineMs) &&
                    !isWordPassed

            val style = when {
                isWordActive -> SpanStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                isWordPassed -> SpanStyle(
                    color = Color.Gray.copy(alpha = 0.6f),
                    fontSize = 24.sp
                )

                isActive -> SpanStyle(
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 24.sp
                )

                else -> SpanStyle(
                    color = Color.DarkGray,
                    fontSize = 20.sp
                )
            }

            withStyle(style) {
                append(word.text)
                append(" ")
            }
        }
    }

    Text(
        text = annotatedString,
        style = style1,
        modifier = modifier
    )
}


/*@Composable
fun SelectLyricFromTrack(
    tracks: List<Track>,
    mediaId: String,
    lyrics: Lyrics?
) {
    val menuState = LocalMenuState.current

    menuState.display {
        Menu {
            MenuEntry(
                icon = R.drawable.chevron_back,
                text = stringResource(R.string.cancel),
                onClick = { menuState.hide() }
            )
            tracks.forEach {
                MenuEntry(
                    icon = R.drawable.text,
                    text = "${it.artistName} - ${it.trackName}",
                    secondaryText = "(${stringResource(R.string.sort_duration)} ${
                        it.duration.seconds.toComponents { minutes, seconds, _ ->
                            "$minutes:${seconds.toString().padStart(2, '0')}"
                        }
                    } ${stringResource(R.string.id)} ${it.id}) ",
                    onClick = {
                        menuState.hide()
                        Database.asyncTransaction {
                            upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = lyrics?.fixed,
                                    synced = it.syncedLyrics.orEmpty()
                                )
                            )
                        }
                    }
                )
            }
            MenuEntry(
                icon = R.drawable.chevron_back,
                text = stringResource(R.string.cancel),
                onClick = { menuState.hide() }
            )
        }
    }
}*/