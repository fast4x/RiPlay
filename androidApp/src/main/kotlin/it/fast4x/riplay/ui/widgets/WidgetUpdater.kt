package it.fast4x.riplay.ui.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import it.fast4x.riplay.extensions.appsettings.models.AppSettings
import it.fast4x.riplay.enums.WidgetAccentColor
import it.fast4x.riplay.enums.WidgetBackground
import it.fast4x.riplay.enums.WidgetBadgePosition
import it.fast4x.riplay.enums.WidgetTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pipeline centralizzata di aggiornamento dei widget RiPlay.
 *
 * Fino a oggi ogni evento del player riscriveva due DataStore separati
 * (uno per widget) con la stessa copertina in Base64, senza alcun
 * filtro: cambiando stato, brano e posizione nello stesso istante
 * l'app eseguiva fino a 6-8 riscritture consecutive.
 *
 * Con questa pipeline:
 *  1. il PlayerService pubblica solo uno [PlayerWidgetSnapshot] ([push]);
 *  2. gli aggiornamenti vengono attenuati con un debounce di 300ms;
 *  3. le riscritture identiche vengono eliminate ([WidgetData.hasSameVisualContent]);
 *  4. un solo canale scrive lo stato di tutti i widget aggiunti alla home;
 *  5. la posizione del brano viaggia su un canale separato a bassa priorità
 *     ([pushPosition]) e viene scritta solo nel widget grande, solo se
 *     in riproduzione e solo se è davvero aggiunto alla home;
 *  6. un cambio di impostazioni (tema, pulsanti, copertina) ripubblica
 *     subito lo stato, senza attendere il prossimo evento del player;
 *  7. a schermo spento la pipeline è congelata ([setScreenInteractive]):
 *     nessuna scrittura in background, lo stato arrivato nel frattempo viene
 *     allineato appena lo schermo si riaccende (correzione 2.1.1).
 */
@OptIn(FlowPreview::class)
object WidgetUpdater {

    private const val DEBOUNCE_MS = 300L

    private val snapshotFlow = MutableStateFlow<PlayerWidgetSnapshot?>(null)
    private val positionFlow = MutableStateFlow<Pair<Long, Long>?>(null)

    private var appContext: Context? = null
    private var attachedScope: CoroutineScope? = null
    private var settingsFlow: StateFlow<AppSettings>? = null

    private var lastPublished: WidgetData? = null
    private var lastPublishedPosition: Pair<Long, Long>? = null

    /**
     * Correzione 2.1.1 "schermo spento": a schermo spento i widget sono invisibili,
     * ma la pipeline continuava a lavorare in background (binder verso il launcher,
     * scrittura datastore, ricomposizione Glance) a ogni evento e ogni secondo per la
     * posizione: lavoro sprecato, consumi extra e superficie di errore proprio
     * mentre Android valuta Doze/standby. Con la pipeline congelata nessuna
     * scrittura widget avviene in background; gli eventi arrivati nel frattempo
     * vengono allineati appena lo schermo si riaccende.
     * Il PlayerService aggiorna questo flag dal screenReceiver (SCREEN_ON/SCREEN_OFF).
     */
    private val screenInteractive = AtomicBoolean(true)

    /** Ultimo stato arrivato mentre lo schermo era spento, da pubblicare alla riaccensione. */
    private var pendingWhileScreenOff: WidgetData? = null

    fun isScreenInteractive(): Boolean = screenInteractive.get()

    /**
     * Congela ([interactive] = false) o riattiva ([interactive] = true) la pipeline.
     * Alla riattivazione pubblica immediatamente l'ultimo stato noto (se era stato
     * silenziato) e azzera la deduplica della posizione, così il prossimo tick del
     * ticker riscrive il valore vero senza attendere un nuovo evento.
     */
    fun setScreenInteractive(interactive: Boolean) {
        if (screenInteractive.getAndSet(interactive) == interactive) return
        if (!interactive) {
            Timber.d("WidgetUpdater: pipeline congelata (schermo spento)")
            return
        }
        Timber.d("WidgetUpdater: pipeline riattivata (schermo acceso), allineo i widget")
        val scope = attachedScope ?: return
        val pending = pendingWhileScreenOff
        pendingWhileScreenOff = null
        if (pending != null) {
            // dedup disattivata per questa pubblicazione: lo stato memorizzato deve
            // raggiungere i widget anche se identico all'ultimo pubblicato a schermo acceso
            lastPublished = null
            scope.launch {
                runCatching { publish(pending) }
                    .onFailure { e ->
                        if (e is CancellationException) {
                            Timber.d("WidgetUpdater: allineamento post-schermo-spento annullato")
                        } else {
                            Timber.e(e, "WidgetUpdater: allineamento post-schermo-spento fallito")
                        }
                    }
            }
        }
        // la posizione riparte dal tick successivo del ticker (entro 1 secondo)
        lastPublishedPosition = null
    }

    /** Tutti i widget pubblicabili: chi aggiunge un nuovo widget lo inserisce qui. */
    private val targets: List<GlanceAppWidget>
        get() = listOf(
            PlayerVerticalWidget.instance,
            PlayerHorizontalWidget.instance,
            PlayerCompactWidget.instance,
            PlayerMiniWidget.instance,
            PlayerLargeWidget.instance,
        )

    /**
     * Collega la pipeline allo scope del PlayerService e alle impostazioni.
     * Idempotente: se già collegata allo stesso scope non fa nulla
     * (uno scope nuovo, ad esempio dopo la ricreazione del servizio, riavvia i collector).
     */
    fun attach(context: Context, scope: CoroutineScope, settings: StateFlow<AppSettings>) {
        if (attachedScope === scope) return
        appContext = context.applicationContext
        attachedScope = scope
        settingsFlow = settings

        // Percorso principale: snapshot -> debounce -> pubblicazione
        scope.launch {
            snapshotFlow
                .debounce(DEBOUNCE_MS)
                .collectLatest { snapshot ->
                    if (snapshot != null) {
                        runCatching { publish(buildWidgetData(snapshot, settings.value)) }
                            .onFailure { e ->
                                if (e is CancellationException) {
                                    // Normale con collectLatest: un aggiornamento più
                                    // recente ha sostituito quello in corso.
                                    Timber.d("WidgetUpdater: pubblicazione annullata (sostituita da un aggiornamento più recente)")
                                } else {
                                    Timber.e(e, "WidgetUpdater: pubblicazione stato fallita (collector resta vivo)")
                                }
                            }
                    }
                }
        }

        // Cambio impostazioni (tema, pulsanti, copertina): ripubblica subito
        scope.launch {
            settings.collect { currentSettings ->
                val snapshot = snapshotFlow.value ?: return@collect
                val data = buildWidgetData(snapshot, currentSettings)
                if (!data.hasSameVisualContent(lastPublished)) {
                    runCatching { publish(data) }
                        .onFailure { e ->
                            if (e is CancellationException) {
                                Timber.d("WidgetUpdater: ripubblicazione annullata (sostituita da un aggiornamento più recente)")
                            } else {
                                Timber.e(e, "WidgetUpdater: ripubblicazione per cambio impostazioni fallita")
                            }
                        }
                }
            }
        }

        // Canale posizione: solo widget grande, scrittura leggera
        scope.launch {
            positionFlow.collect { position ->
                if (position != null) {
                    runCatching { publishPosition(position.first, position.second) }
                        .onFailure { e ->
                            if (e is CancellationException) {
                                Timber.d("WidgetUpdater: aggiornamento posizione annullato")
                            } else {
                                Timber.e(e, "WidgetUpdater: aggiornamento posizione fallito (collector resta vivo)")
                            }
                        }
                }
            }
        }

        Timber.d("WidgetUpdater: pipeline collegata allo scope del PlayerService")
    }

    /** Pubblica un nuovo snapshot del player (il debounce si occupa del resto). */
    fun push(snapshot: PlayerWidgetSnapshot) {
        snapshotFlow.value = snapshot
    }

    /**
     * Pubblica la posizione corrente del brano. Chiamato dal PlayerService
     * circa una volta al secondo, solo mentre il brano è in riproduzione.
     */
    fun pushPosition(positionMs: Long, durationMs: Long) {
        positionFlow.value = positionMs to durationMs
    }

    /**
     * Ripubblica immediatamente l'ultimo snapshot (ignorando la deduplica).
     * Usato dalla sezione Impostazioni > Widgets ("Aggiorna i widget adesso").
     */
    fun forceRefresh() {
        val scope = attachedScope ?: return
        val snapshot = snapshotFlow.value ?: return
        lastPublished = null
        lastPublishedPosition = null
        scope.launch {
            publish(buildWidgetData(snapshot, settingsFlow?.value ?: return@launch))
        }
    }

    private fun buildWidgetData(snapshot: PlayerWidgetSnapshot, settings: AppSettings): WidgetData = WidgetData(
        snapshot = snapshot,
        themeName = when (settings.widgetTheme) {
            WidgetTheme.System -> WidgetThemeNames.SYSTEM
            WidgetTheme.Light -> WidgetThemeNames.LIGHT
            WidgetTheme.Dark -> WidgetThemeNames.DARK
            WidgetTheme.PureBlack -> WidgetThemeNames.PURE_BLACK
        },
        accentName = when (settings.widgetAccentColor) {
            WidgetAccentColor.Default -> WidgetAccentNames.DEFAULT
            WidgetAccentColor.Orange -> WidgetAccentNames.ORANGE
            WidgetAccentColor.Red -> WidgetAccentNames.RED
            WidgetAccentColor.Pink -> WidgetAccentNames.PINK
            WidgetAccentColor.Purple -> WidgetAccentNames.PURPLE
            WidgetAccentColor.Blue -> WidgetAccentNames.BLUE
            WidgetAccentColor.Teal -> WidgetAccentNames.TEAL
            WidgetAccentColor.Green -> WidgetAccentNames.GREEN
        },
        backgroundName = when (settings.widgetBackground) {
            WidgetBackground.Solid -> WidgetBackgroundNames.SOLID
            WidgetBackground.SemiTransparent -> WidgetBackgroundNames.SEMI_TRANSPARENT
            WidgetBackground.Transparent -> WidgetBackgroundNames.TRANSPARENT
        },
        badgePositionName = when (settings.widgetBadgePosition) {
            WidgetBadgePosition.TopStart -> WidgetBadgeNames.TOP_START
            WidgetBadgePosition.TopEnd -> WidgetBadgeNames.TOP_END
            WidgetBadgePosition.BottomStart -> WidgetBadgeNames.BOTTOM_START
            WidgetBadgePosition.BottomEnd -> WidgetBadgeNames.BOTTOM_END
        },
        showArtwork = settings.widgetShowArtwork,
        showLikeButton = settings.widgetShowLikeButton,
        showShuffleButton = settings.widgetShowShuffleButton,
        showProgress = settings.widgetShowProgress,
    )

    /** Scrive lo stato completo e ridisegna tutte le istanze aggiunte dei widget. */
    private suspend fun publish(data: WidgetData) {
        val context = appContext ?: return
        if (data.hasSameVisualContent(lastPublished)) return

        // Schermo spento: nessuna scrittura. Lo stato più recente resta in attesa
        // (e la dedup resta coerente) per l'allineamento alla riaccensione.
        if (!screenInteractive.get()) {
            pendingWhileScreenOff = data
            lastPublished = data
            Timber.d("WidgetUpdater: stato memorizzato mentre lo schermo è spento (title=${data.snapshot.title}, isPlaying=${data.snapshot.isPlaying})")
            return
        }

        withContext(Dispatchers.IO) {
            val manager = GlanceAppWidgetManager(context)
            for (widget in targets) {
                // getGlanceIds dentro il runCatching: un errore transitorio del
                // launcher non deve terminare il collector di pubblicazione.
                runCatching {
                    val ids = manager.getGlanceIds(widget::class.java)
                    for (id in ids) {
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                            prefs.toMutablePreferences().apply { data.applyTo(this) }
                        }
                        widget.update(context, id)
                    }
                }.onFailure { e ->
                    // Un annullamento qui è normale e atteso: collectLatest sostituisce
                    // una pubblicazione ancora in corso con quella più recente.
                    // Non è un errore: si rilancia per uscire subito dal ciclo
                    // (i widget già scritti restano validi, il nuovo giro li rifà tutti).
                    if (e is CancellationException) throw e
                    Timber.e(e, "WidgetUpdater: pubblicazione fallita per ${widget::class.simpleName}")
                }
            }
        }

        lastPublished = data
        Timber.d("WidgetUpdater: stato pubblicato (title=${data.snapshot.title}, isPlaying=${data.snapshot.isPlaying})")
    }

    /** Scrive solo posizione/durata, solo nel widget grande, senza ridisegnare gli altri. */
    private suspend fun publishPosition(positionMs: Long, durationMs: Long) {
        val context = appContext ?: return
        val position = positionMs to durationMs
        if (position == lastPublishedPosition) return

        // Schermo spento: aggiorniamo solo la dedup, la scrittura vera avverrà
        // al primo tick utile dopo la riaccensione (lastPublishedPosition viene
        // azzerato da setScreenInteractive).
        if (!screenInteractive.get()) {
            lastPublishedPosition = position
            return
        }

        withContext(Dispatchers.IO) {
            // Tutto dentro il runCatching: se il widget grande non è aggiunto
            // (nessun id) o il launcher dà errore, il collector posizione resta vivo
            // e riproverà al tick successivo.
            runCatching {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(PlayerLargeWidget::class.java)
                for (id in ids) {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[WidgetKeys.positionMs] = positionMs
                            this[WidgetKeys.durationMs] = durationMs
                        }
                    }
                    PlayerLargeWidget.instance.update(context, id)
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Timber.e(e, "WidgetUpdater: aggiornamento posizione fallito")
            }
        }

        lastPublishedPosition = position
    }
}
