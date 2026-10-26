package it.fast4x.riplay.player.kcef

import java.awt.Canvas
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.concurrent.thread

/**
 * STEP 3d: pannello OSPITE del player KCEF a livello SWING.
 *
 * Perche' esiste: il browser CEF in rendering "windowed" e' un componente
 * AWT NATIVO che sta SEMPRE sopra qualunque contenuto Compose (e' la stessa
 * ragione per cui badge e banner erano coperti). La v21 tentava di far
 * vedere la cover Compose nascondendo il componente AWT del browser
 * (isVisible=false): oltre a lasciare scoperto il contenitore Swing di
 * interop (pannello BIANCO visto in prova), il browser nascosto non faceva
 * partire la pagina/la riproduzione (il tasto play non aveva nulla da
 * avviare). La cover va quindi disegnata DENTRO il livello Swing:
 *
 *  - questo pannello (sfondo NERO, opaco) contiene il browser SOTTO e una
 *    [CoverCanvas] SOPRA (z-order esplicito: index 0 = sopra);
 *  - il browser NON viene MAI nascosto durante la riproduzione: la pagina
 *    player carica, l'autoplay parte e play/pausa funzionano come nello
 *    step2r validato. L'audio continua sotto la cover, esattamente come
 *    Android (UnifiedPlayer mostra Image(coverPainter) e il player suona);
 *  - la canvas disegna: la COVER del brano (adattata, Fit) quando c'e' un
 *    brano audio; il SEGNAPOSTO RiPlay quando non c'e' nessun brano (niente
 *    piu' pannello bianco in home); il messaggio di ERRORE YouTube quando
 *    il player segnala un codice (il vecchio banner Compose era comunque
 *    coperto dal browser nativo);
 *  - la canvas e' CLICCABILE e apre il player espandibile.
 *
 * Solo quando l'overlay espanso e' aperto l'INTERO livello Swing viene
 * nascosto (l'audio a quel punto e' gia' avviato e prosegue, come la
 * riproduzione in background).
 */
internal class KcefPlayerHostPanel : JPanel(null) {

    /** Canvas con cover/segnaposto/errore: sta SOPRA il browser (z-order 0). */
    val coverCanvas = CoverCanvas()

    private var browserComponent: java.awt.Component? = null

    init {
        isOpaque = true
        background = Color.BLACK
        add(coverCanvas)
        coverCanvas.isVisible = false
    }

    /**
     * Inserisce il browser SOTTO la canvas della cover. Z-order AWT: il
     * componente con indice MINORE e' disegnato SOPRA; la cover va quindi
     * forzata all'indice 0 e il browser all'indice 1.
     */
    fun setBrowser(component: java.awt.Component) {
        removeAll()
        browserComponent = component
        add(component)
        add(coverCanvas)
        setComponentZOrder(coverCanvas, 0)
        setComponentZOrder(component, 1)
        revalidate()
        repaint()
    }

    /** Layout manuale (null layout): entrambi i figli riempiono il pannello. */
    override fun doLayout() {
        val w = width
        val h = height
        coverCanvas.setBounds(0, 0, w, h)
        browserComponent?.setBounds(0, 0, w, h)
    }

    /** Cover visibile con [url]; url null = segnaposto RiPlay. */
    fun showCover(visible: Boolean, url: String?) = coverCanvas.showCover(visible, url)

    /** Messaggio di errore YouTube disegnato sulla canvas (al posto del banner). */
    fun setErrorText(text: String?) = coverCanvas.setErrorText(text)

    /** Callback di click sulla cover (apre il player espandibile). */
    fun setOnCoverClick(callback: (() -> Unit)?) {
        coverCanvas.onClick = callback
    }
}

/**
 * Canvas AWT (heavyweight: solo cosi' sta sopra il browser nativo) che
 * disegna copertina/segnaposto/errore. L'immagine viene scaricata in un
 * thread dedicato (ImageIO) e ridisegnata al termine; il dimensionamento
 * e' Fit centrato come la copertina del player Android.
 */
internal class CoverCanvas : Canvas() {

    @Volatile private var currentUrl: String? = null
    @Volatile private var loadedImage: Image? = null
    @Volatile private var errorText: String? = null

    /** Click sulla cover (inizializzato dall'update di SwingPanel). */
    @Volatile var onClick: (() -> Unit)? = null

    init {
        background = Color.BLACK
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onClick?.invoke()
            }
        })
    }

    fun showCover(visible: Boolean, url: String?) {
        if (isVisible != visible) {
            isVisible = visible
            repaint()
        }
        if (!visible) return
        if (url != currentUrl) {
            currentUrl = url
            loadedImage = null
            repaint()
            if (url != null) loadAsync(url)
        }
    }

    fun setErrorText(text: String?) {
        if (errorText == text) return
        errorText = text
        repaint()
    }

    private fun loadAsync(url: String) {
        thread(name = "riplay-cover-loader", isDaemon = true) {
            val img = runCatching { javax.imageio.ImageIO.read(java.net.URL(url)) }.getOrNull()
            if (currentUrl == url) {
                loadedImage = img
                repaint()
            }
        }
    }

    /** Niente clearRect di default (sfarfallio): la paint copre tutto comunque. */
    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        g.color = Color.BLACK
        g.fillRect(0, 0, w, h)

        // 1) errore YouTube: priorita' massima (al posto del vecchio banner)
        val err = errorText
        if (!err.isNullOrBlank()) {
            (g as? Graphics2D)?.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )
            g.color = Color(0xFFFFC107.toInt())
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
            val fm = g.fontMetrics
            val lines = err.chunked(44)
            val lineStep = fm.height + 2
            var y = h / 2 - (lines.size * lineStep) / 2 + fm.ascent
            for (line in lines) {
                g.drawString(line, (w - fm.stringWidth(line)) / 2, y)
                y += lineStep
            }
            return
        }

        // 2) copertina del brano, adattata centrata (ContentScale.Fit)
        val img = loadedImage
        if (img != null) {
            val iw = img.getWidth(null)
            val ih = img.getHeight(null)
            if (iw > 0 && ih > 0) {
                // STEP 3n: interpolazione bilineare (scala piu' morbida)
                (g as? Graphics2D)?.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
                val scale = minOf(w.toDouble() / iw, h.toDouble() / ih)
                val dw = (iw * scale).toInt().coerceAtLeast(1)
                val dh = (ih * scale).toInt().coerceAtLeast(1)
                g.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null)
                return
            }
        }

        // 3) segnaposto (avvio senza brano o cover non disponibile):
        //    triangolo play verde + nome app (nessun pannello bianco)
        (g as? Graphics2D)?.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        val cx = w / 2
        val cy = h / 2 - 16
        val size = (minOf(w, h) * 0.14f).toInt().coerceIn(28, 64)
        g.color = Color(0xFF3D9142.toInt())
        g.fillPolygon(
            intArrayOf(cx - size / 2, cx - size / 2, cx + size / 2),
            intArrayOf(cy - size / 2, cy + size / 2, cy),
            3
        )
        g.font = Font(Font.SANS_SERIF, Font.BOLD, (size * 0.55f).toInt().coerceIn(14, 26))
        g.color = Color(0xFFB9B9B9.toInt())
        val label = "RiPlay"
        g.drawString(label, (w - g.fontMetrics.stringWidth(label)) / 2, cy + size / 2 + g.fontMetrics.height)
    }
}
