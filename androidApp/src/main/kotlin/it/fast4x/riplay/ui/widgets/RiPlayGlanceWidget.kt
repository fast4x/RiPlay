package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import it.fast4x.riplay.R

/**
 * Palette di colori risolta per il rendering di un widget.
 *
 * Vengono usati colori espliciti (e non i colori dinamici di GlanceTheme)
 * per avere un aspetto prevedibile e riconoscibile su ogni launcher e
 * per supportare il tema forzato Chiaro/Scuro/Nero puro scelto nelle impostazioni.
 * L'accento predefinito riprende il colore di default dell'app (#DE6433) ma può
 * essere sostituito da uno dei valori di [it.fast4x.riplay.enums.WidgetAccentColor].
 */
data class WidgetPalette(
    val isDark: Boolean,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val progressTrack: Color,
    val accent: Color = WidgetAccentColors.DEFAULT,
) {
    companion object {
        val DARK = WidgetPalette(
            isDark = true,
            background = Color(0xF7141414),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xB3FFFFFF),
            progressTrack = Color(0x33FFFFFF),
        )
        val LIGHT = WidgetPalette(
            isDark = false,
            background = Color(0xF7FCFCFC),
            textPrimary = Color(0xFF1B1B1B),
            textSecondary = Color(0x991B1B1B),
            progressTrack = Color(0x221B1B1B),
        )

        /** Palette nero assoluto per display OLED: massima economizzazione energetica. */
        val PURE_BLACK = WidgetPalette(
            isDark = true,
            background = Color(0xF7000000),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xB3FFFFFF),
            progressTrack = Color(0x33FFFFFF),
        )
    }

    /** Restituisce la stessa palette con un colore d'accento personalizzato. */
    fun withAccent(accent: Color) = copy(accent = accent)

    /** Restituisce la stessa palette con l'opacità dello sfondo ridefinita (0..255). */
    fun withBackgroundAlpha(alpha: Int) = copy(
        background = background.copy(alpha = alpha.coerceIn(0, 255) / 255f)
    )
}

/**
 * Colori d'accento disponibili per i widget, corrispondenti ai valori di
 * [it.fast4x.riplay.enums.WidgetAccentColor]. La risoluzione parte dal nome
 * salvato nel datastore ([WidgetData.accentName]).
 */
object WidgetAccentColors {
    val DEFAULT = Color(0xFFDE6433)
    val ORANGE = Color(0xFFFF9800)
    val RED = Color(0xFFF44336)
    val PINK = Color(0xFFE91E63)
    val PURPLE = Color(0xFF9C27B0)
    val BLUE = Color(0xFF2196F3)
    val TEAL = Color(0xFF009688)
    val GREEN = Color(0xFF4CAF50)

    fun byName(name: String): Color = when (name) {
        WidgetAccentNames.ORANGE -> ORANGE
        WidgetAccentNames.RED -> RED
        WidgetAccentNames.PINK -> PINK
        WidgetAccentNames.PURPLE -> PURPLE
        WidgetAccentNames.BLUE -> BLUE
        WidgetAccentNames.TEAL -> TEAL
        WidgetAccentNames.GREEN -> GREEN
        else -> DEFAULT
    }
}

/** Opacità dello sfondo (0..255) corrispondente a [it.fast4x.riplay.enums.WidgetBackground]. */
object WidgetBackgroundAlphas {
    const val SOLID = 0xF7
    const val SEMI_TRANSPARENT = 0x99
    const val TRANSPARENT = 0x00

    fun byName(name: String): Int = when (name) {
        WidgetBackgroundNames.SEMI_TRANSPARENT -> SEMI_TRANSPARENT
        WidgetBackgroundNames.TRANSPARENT -> TRANSPARENT
        else -> SOLID
    }
}

/** Icone monocromatiche dedicate ai widget, in variante chiara e scura. */
object WidgetIcons {
    fun play(dark: Boolean) = if (dark) R.drawable.widget_ic_play else R.drawable.widget_ic_play_dark
    fun pause(dark: Boolean) = if (dark) R.drawable.widget_ic_pause else R.drawable.widget_ic_pause_dark
    fun next(dark: Boolean) = if (dark) R.drawable.widget_ic_next else R.drawable.widget_ic_next_dark
    fun previous(dark: Boolean) = if (dark) R.drawable.widget_ic_previous else R.drawable.widget_ic_previous_dark

    /**
     * Shuffle: variante attenuata quando la riproduzione casuale è disattivata
     * (due icone separate invece del modifier alpha, per compatibilità con ogni versione di Glance).
     */
    fun shuffle(dark: Boolean, enabled: Boolean) = when {
        dark && enabled -> R.drawable.widget_ic_shuffle
        dark && !enabled -> R.drawable.widget_ic_shuffle_dim
        !dark && enabled -> R.drawable.widget_ic_shuffle_dark
        else -> R.drawable.widget_ic_shuffle_dim_dark
    }

    /** Il cuore pieno usa il colore d'accento scelto dall'utente, valido su entrambi i temi. */
    fun heart(likedState: Int, dark: Boolean, accentName: String = WidgetAccentNames.DEFAULT) =
        when (likedState) {
            LikeState.LIKED -> heartFilled(accentName)
            else -> if (dark) R.drawable.widget_ic_heart else R.drawable.widget_ic_heart_dark
        }

    /**
     * Variante piena del cuore nel colore d'accento. Glance 1.0 non supporta
     * il colorFilter sulle immagini (arrivato in 1.1), quindi ogni accento ha
     * un drawable dedicato generato a partire dal cuore arancione originale.
     */
    private fun heartFilled(accentName: String) = when (accentName) {
        WidgetAccentNames.ORANGE -> R.drawable.widget_ic_heart_filled_orange
        WidgetAccentNames.RED -> R.drawable.widget_ic_heart_filled_red
        WidgetAccentNames.PINK -> R.drawable.widget_ic_heart_filled_pink
        WidgetAccentNames.PURPLE -> R.drawable.widget_ic_heart_filled_purple
        WidgetAccentNames.BLUE -> R.drawable.widget_ic_heart_filled_blue
        WidgetAccentNames.TEAL -> R.drawable.widget_ic_heart_filled_teal
        WidgetAccentNames.GREEN -> R.drawable.widget_ic_heart_filled_green
        else -> R.drawable.widget_ic_heart_filled
    }
}

/**
 * Risolve la palette completa in base alle preferenze utente contenute in [data]:
 * - tema: Dark / Light / PureBlack fissi, System in base alla modalità notte;
 * - accento: colore di barra di avanzamento e cuore pieno;
 * - sfondo: opacità piena, ridotta o nulla.
 */
fun resolveWidgetPalette(context: Context, data: WidgetData): WidgetPalette {
    val base = when (data.themeName) {
        WidgetThemeNames.DARK -> WidgetPalette.DARK
        WidgetThemeNames.LIGHT -> WidgetPalette.LIGHT
        WidgetThemeNames.PURE_BLACK -> WidgetPalette.PURE_BLACK
        else -> if (isSystemInDarkMode(context)) WidgetPalette.DARK else WidgetPalette.LIGHT
    }
    return base
        .withAccent(WidgetAccentColors.byName(data.accentName))
        .withBackgroundAlpha(WidgetBackgroundAlphas.byName(data.backgroundName))
}

private fun isSystemInDarkMode(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

/** Decodifica la copertina dalla cache su disco; fallback sull'icona dell'app. */
fun decodeWidgetCover(path: String?): ImageProvider {
    if (path.isNullOrEmpty()) return ImageProvider(R.drawable.app_icon)
    val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    return if (bitmap != null) ImageProvider(bitmap) else ImageProvider(R.drawable.app_icon)
}

/**
 * Sfondo traslucido scuro condiviso dai badge sovrapposti (logo e coda):
 * resta leggibile su qualunque copertina, wallpaper o palette.
 */
val WidgetBadgeBackground = Color(0xB3000000)

/** Altezza della barra di avanzamento dei widget. */
private val ProgressHeight = 4.dp

/**
 * Livelli di bisezione del riempimento della barra: con 8 livelli la precisione
 * è 1/256 (~0.4%), impercettibile su una barra alta 4 dp.
 */
private const val ProgressFillDepth = 8

/**
 * Classe base di tutti i widget RiPlay.
 *
 * Si occupa di:
 *  - leggere lo stato condiviso dal datastore ([toWidgetData]);
 *  - risolvere la palette in base al tema scelto nelle impostazioni
 *    (Sistema, Chiaro o Scuro);
 *  - decodificare la copertina dalla cache su disco ([WidgetArtworkCache]);
 *  - fornire i componenti grafici condivisi (testi, pulsantiera, barra di avanzamento,
 *    badge logo e badge coda).
 *
 * Le sottoclassi definiscono solo il layout ([Content]).
 */
abstract class RiPlayGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val data = prefs.toWidgetData()
            val palette = resolveWidgetPalette(context, data)
            // Con "mostra copertina" disattivata nei widget compare l'icona dell'app
            val cover = if (data.showArtwork) {
                decodeWidgetCover(data.snapshot.artworkPath)
            } else {
                ImageProvider(R.drawable.app_icon)
            }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(palette.background)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Content(palette = palette, data = data, cover = cover)
                }
            }
        }
    }

    /** Layout specifico del widget. */
    @Composable
    protected abstract fun Content(
        palette: WidgetPalette,
        data: WidgetData,
        cover: ImageProvider,
    )

    // ------------------------------------------------------------------
    // Componenti condivisi
    // ------------------------------------------------------------------

    /** Titolo e artista del brano corrente. */
    @Composable
    protected fun MediaText(
        palette: WidgetPalette,
        data: WidgetData,
        titleSize: TextUnit = 14.sp,
        artistSize: TextUnit = 12.sp,
        centerAlign: Boolean = false,
    ) {
        val titleStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            color = ColorProvider(palette.textPrimary),
            fontSize = titleSize
        )
        val artistStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            color = ColorProvider(palette.textSecondary),
            fontSize = artistSize
        )
        Column(horizontalAlignment = if (centerAlign) Alignment.CenterHorizontally else Alignment.Start) {
            Text(text = data.snapshot.title, style = titleStyle, maxLines = 1)
            Text(text = data.snapshot.artist, style = artistStyle, maxLines = 1)
        }
    }

    /**
     * Pulsantiera di controllo: shuffle (opzionale), previous, play/pause,
     * next e like (opzionale). Il pulsante play/pause è determinato dallo
     * stato pubblicato: dopo il tocco il PlayerService aggiorna subito il widget.
     */
    @Composable
    protected fun ControlRow(
        palette: WidgetPalette,
        data: WidgetData,
        iconSize: Dp = 22.dp,
        playIconSize: Dp = 30.dp,
        spacing: Dp = 14.dp,
    ) {
        val dark = palette.isDark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (data.showShuffleButton) {
                Image(
                    provider = ImageProvider(WidgetIcons.shuffle(dark, data.snapshot.shuffleEnabled)),
                    contentDescription = "shuffle",
                    modifier = GlanceModifier
                        .size(iconSize)
                        .clickable(actionRunCallback<ToggleShuffleAction>())
                )
                Spacer(modifier = GlanceModifier.width(spacing))
            }

            Image(
                provider = ImageProvider(WidgetIcons.previous(dark)),
                contentDescription = "previous",
                modifier = GlanceModifier
                    .size(iconSize)
                    .clickable(actionRunCallback<PreviousAction>())
            )

            Spacer(modifier = GlanceModifier.width(spacing))

            Image(
                provider = ImageProvider(
                    if (data.snapshot.isPlaying) WidgetIcons.pause(dark) else WidgetIcons.play(dark)
                ),
                contentDescription = "play/pause",
                modifier = GlanceModifier
                    .size(playIconSize)
                    .clickable(
                        if (data.snapshot.isPlaying) actionRunCallback<PauseAction>()
                        else actionRunCallback<PlayAction>()
                    )
            )

            Spacer(modifier = GlanceModifier.width(spacing))

            Image(
                provider = ImageProvider(WidgetIcons.next(dark)),
                contentDescription = "next",
                modifier = GlanceModifier
                    .size(iconSize)
                    .clickable(actionRunCallback<NextAction>())
            )

            if (data.showLikeButton) {
                Spacer(modifier = GlanceModifier.width(spacing))
                Image(
                    provider = ImageProvider(
                        WidgetIcons.heart(data.snapshot.likedState, dark, data.accentName)
                    ),
                    contentDescription = "like",
                    modifier = GlanceModifier
                        .size(iconSize)
                        .clickable(actionRunCallback<ToggleLikeAction>())
                )
            }
        }
    }

    /**
     * Barra di avanzamento con i tempi formattati.
     *
     * Correzione 2.2.3: la parte riempita non usa più una larghezza assoluta in dp
     * derivata da LocalSize (Glance 1.0 non supporta fillMaxWidth(fraction) né
     * LinearProgressIndicator, arrivati in 1.1). Su diversi launcher LocalSize
     * riporta una larghezza minore di quella reale del widget (es. il minWidth
     * dell'XML invece della dimensione corrente) e la barra si fermava al 50-70%:
     * il riempimento è ora costruito per bisezione con i soli pesi ([ProgressFill]),
     * quindi risulta esattamente frazione della larghezza REALE della traccia su
     * qualunque launcher, orientamento o ridimensionamento del widget.
     */
    @Composable
    protected fun ProgressSection(
        palette: WidgetPalette,
        data: WidgetData,
    ) {
        val duration = data.snapshot.durationMs
        val fraction = if (duration > 0) {
            (data.snapshot.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            // Traccia: un solo box a tutta larghezza reale, angoli arrotondati;
            // il riempimento è disegnato sopra dal componente a pesi, che occupa
            // esattamente la frazione richiesta di QUESTA larghezza.
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(ProgressHeight)
                    .cornerRadius(2.dp)
                    .background(palette.progressTrack)
            ) {
                if (fraction > 0f) {
                    ProgressFill(fraction = fraction, palette = palette, depth = 0)
                }
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = formatWidgetTime(data.snapshot.positionMs),
                    style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 11.sp),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = formatWidgetTime(duration),
                    style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }
    }

    /**
     * Riempimento frazionario della traccia senza alcuna larghezza in dp.
     *
     * Il riempimento è SEMPRE contiguo dal bordo sinistro: a ogni livello lo
     * spazio disponibile è diviso in due metà uguali con defaultWeight e:
     *  - frazione >= 0.5  -> la metà sinistra si riempie tutta, e nella metà
     *     destra si prosegue con la frazione riscalata ((f-0.5)*2);
     *  - frazione < 0.5   -> la metà destra resta vuota (traccia visibile) e
     *     la prosecuzione avviene DENTRO la metà sinistra, con frazione f*2:
     *     è questa ricorsione a sinistra che tiene il riempimento ancorato
     *     al bordo sinistro (la versione 2.2.3 scendeva nella metà destra e
     *     il riempimento partiva da destra, a frammenti).
     *
     * Con [ProgressFillDepth] livelli il risultato copre la frazione richiesta
     * a meno di 1/256 (~0.4%), ed è sempre relativo alla larghezza effettiva
     * del contenitore: immune dai valori inaccurati di LocalSize riportati
     * da alcuni launcher.
     */
    @Composable
    private fun ProgressFill(
        fraction: Float,
        palette: WidgetPalette,
        depth: Int,
    ) {
        val leftFilled = fraction >= 0.5f
        val rest = if (leftFilled) (fraction - 0.5f) * 2f else fraction * 2f
        val lastLevel = depth >= ProgressFillDepth - 1

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // Metà sinistra: piena se il bit corrente è 1, altrimenti contiene
            // la prosecuzione del riempimento (che è ancora dentro questa metà).
            Box(modifier = GlanceModifier.defaultWeight().height(ProgressHeight)) {
                when {
                    leftFilled ->
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(palette.accent)
                        ) {}
                    !lastLevel && rest > 0f -> ProgressFill(rest, palette, depth + 1)
                    lastLevel && rest >= 0.5f ->
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(palette.accent)
                        ) {}
                    // ultimo livello con rest < 0.5: resta visibile la traccia
                }
            }
            // Metà destra: si riempie solo se la sinistra è piena (rest = resto).
            Box(modifier = GlanceModifier.defaultWeight().height(ProgressHeight)) {
                when {
                    leftFilled && !lastLevel && rest > 0f ->
                        ProgressFill(rest, palette, depth + 1)
                    leftFilled && lastLevel && rest >= 0.5f ->
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(palette.accent)
                        ) {}
                    // altrimenti vuota: sotto si vede il colore della traccia
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Badge condivisi: logo RiPlay e brani in coda
    // ------------------------------------------------------------------

    /** Vero se il widget sta mostrando la copertina reale e non l'icona di fallback. */
    protected fun WidgetData.hasRealCover(): Boolean =
        showArtwork && !snapshot.artworkPath.isNullOrEmpty()

    /**
     * Etichetta della coda nel formato "n/N", mostrata solo quando in coda
     * c'è più di un brano e l'indice è noto.
     */
    protected fun queueLabel(data: WidgetData): String? {
        val size = data.snapshot.queueSize
        val index = data.snapshot.queueIndex
        if (size <= 1 || index < 0) return null
        return "${index + 1}/$size"
    }

    /**
     * Chip scuro con l'etichetta della coda ("n/N"). Lo sfondo traslucido
     * condiviso lo mantiene leggibile sia sulla copertina sia sui temi
     * chiaro e scuro, quindi è usabile inline e sovrapposto.
     */
    @Composable
    protected fun QueueChip(
        label: String,
        textSize: TextUnit = 9.sp,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = textSize,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier
                .background(WidgetBadgeBackground)
                .cornerRadius(5.dp)
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }

    /**
     * Piccolo badge con il logo RiPlay su sfondo traslucido scuro.
     * È la firma dell'app nell'angolo di ogni widget: sulla copertina dei
     * widget con copertina, sugli angoli o in coda ai controlli per quelli
     * senza copertina. Non viene mostrato quando la copertina stessa è
     * l'icona dell'app (fallback senza player o copertina nascosta),
     * per non duplicare il logo.
     */
    @Composable
    protected fun LogoChip(size: Dp = 16.dp) {
        Box(
            modifier = GlanceModifier
                .size(size)
                .cornerRadius(size / 3)
                .background(WidgetBadgeBackground)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.app_icon),
                contentDescription = "logo RiPlay",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }

    // ------------------------------------------------------------------
    // Posizione dei badge negli angoli
    // ------------------------------------------------------------------

    /**
     * Allineamento del badge coda in base alla posizione scelta nelle
     * impostazioni ([it.fast4x.riplay.enums.WidgetBadgePosition]).
     *
     * Nota su Glance 1.0: il Box non offre uno scope con align per i figli,
     * quindi gli angoli si realizzano con Box a tutta area e
     * contentAlignment, che è l'unica API supportata da tutte le versioni.
     */
    protected fun badgeAlignment(data: WidgetData): Alignment = when (data.badgePositionName) {
        WidgetBadgeNames.TOP_START -> Alignment.TopStart
        WidgetBadgeNames.TOP_END -> Alignment.TopEnd
        WidgetBadgeNames.BOTTOM_START -> Alignment.BottomStart
        else -> Alignment.BottomEnd
    }

    /** Il logo RiPlay occupa sempre l'angolo diagonale opposto al badge della coda. */
    protected fun logoAlignment(data: WidgetData): Alignment = when (data.badgePositionName) {
        WidgetBadgeNames.TOP_START -> Alignment.BottomEnd
        WidgetBadgeNames.TOP_END -> Alignment.BottomStart
        WidgetBadgeNames.BOTTOM_START -> Alignment.TopEnd
        else -> Alignment.TopStart
    }

    /**
     * Overlay trasparente a tutta area con il logo RiPlay e il badge dei
     * brani in coda negli angoli configurati. Va chiamato DENTRO il Box che
     * contiene la copertina (o, nei widget senza copertina, dentro il Box
     * radice): i due Box interni riempiono tutta l'area disponibile e
     * allineano i rispettivi chip con [contentAlignment], che in Glance 1.0
     * è il solo modo supportato per ancorare un figlio a un angolo.
     *
     * [showLogo] di default nasconde il logo quando la copertina mostrata è
     * l'icona dell'app di fallback, per non duplicarla.
     */
    @Composable
    protected fun CornerBadges(
        data: WidgetData,
        cornerPadding: Dp = 6.dp,
        logoSize: Dp = 16.dp,
        badgeTextSize: TextUnit = 9.sp,
        showLogo: Boolean = data.hasRealCover(),
    ) {
        if (showLogo) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(cornerPadding),
                contentAlignment = logoAlignment(data)
            ) {
                LogoChip(size = logoSize)
            }
        }

        val queue = queueLabel(data)
        if (queue != null) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(cornerPadding),
                contentAlignment = badgeAlignment(data)
            ) {
                QueueChip(label = queue, textSize = badgeTextSize)
            }
        }
    }
}
