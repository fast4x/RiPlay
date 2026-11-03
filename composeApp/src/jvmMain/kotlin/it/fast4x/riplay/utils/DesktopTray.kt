package it.fast4x.riplay.utils

import it.fast4x.riplay.i18n.ds
import java.awt.AWTEvent
import java.awt.Color
import java.awt.EventQueue
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.AWTEventListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.JDialog
import javax.swing.JPanel

/*
 *
 * Implementazione java.awt.SystemTray (nativa, nessuna dipendenza extra):
 * le azioni arrivano sul thread AWT-EDT; le scritte di stato Compose sono
 * consentite da qualunque thread. I comandi player passano da
 * [DesktopPlaybackHub] (registrato da ThreeColumnsApp all'avvio): quindi
 * funzionano anche a finestra nascosta. Se il sistema non ha tray
 * (SystemTray.isSupported() false) la classe resta inerte e la chiusura
 * della finestra torna a uscire dall'app (main.kt).
 */
object DesktopTray {

    /** TRUE se il sistema offre un'area di notifica (verifica sicura). */
    val isSupported: Boolean
        get() = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    /** TRUE se l'icona e' davvero stata aggiunta (main.kt: decide se la
     *  chiusura della finestra nasconde o esce). */
    var running: Boolean = false
        private set

    /** Callback registrate da main.kt (composizione). */
    @Volatile var showWindow: (() -> Unit)? = null
    @Volatile var requestExit: (() -> Unit)? = null

    private var trayIcon: TrayIcon? = null
    private var menuWindow: TrayMenuWindow? = null

    /** Istante dell'ultimo click destro GESTITO (antirimbalzo pressed/
     *  released, vedi [onTrayRightClick]). */
    private var lastRightClickActMs = 0L

    private var menuHiddenByFocusLossAtMs = 0L

    /** Voce del menu (action = comando al click). */
    private class MenuEntry(val label: String, val action: (() -> Unit)?)

    /** Segnaposto separatore nella lista delle voci. */
    private object MenuSeparator

    private val MENU_SEPARATOR = MenuSeparator

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "riplay-tray").apply { isDaemon = true }
    }

    fun start() {
        if (running) return
        if (!isSupported) {
            println("[RiPlay-Tray] SystemTray non supportato su questo sistema: tray disattivato")
            return
        }
        runCatching {
            val tray = SystemTray.getSystemTray()

            val icon = TrayIcon(appIconImage(), "RiPlay")
            icon.isImageAutoSize = true

            icon.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    // DOPPIO CLICK = riapri la finestra (singolo click:
                    // niente, per non rubare il gesto al menu)
                    if (e.clickCount >= 2 && e.button == MouseEvent.BUTTON1) {
                        showWindow?.invoke()
                    }
                }

                override fun mousePressed(e: MouseEvent) = onTrayRightClick(e)

                override fun mouseReleased(e: MouseEvent) = onTrayRightClick(e)
            })

            tray.add(icon)
            trayIcon = icon
            running = true
            println("[RiPlay-Tray] icona nel tray attiva (doppio click = mostra la finestra, " +
                    "click destro = menu)")

            // aggiorna etichetta play/pausa + tooltip (brano in riproduzione)
            scheduler.scheduleWithFixedDelay({
                runCatching { refreshDynamicLabel() }.getOrNull()
            }, 2L, 2L, TimeUnit.SECONDS)
        }.onFailure {
            println("[RiPlay-Tray] tray non disponibile: ${it.message}")
        }
    }

    /** Riporta la finestra in primo piano DOPO che la composizione l'ha
     *  resa di nuovo visibile (300ms: un frame per il passaggio di stato). */
    fun bringToFrontLater() {
        scheduler.schedule({
            EventQueue.invokeLater {
                runCatching {
                    // la principale e' l'ultima creata: portandole tutte in
                    // fronte in ordine, l'app finisce sopra (la splash, se
                    // ancora viva, e' sempre alwaysOnTop e non disturba)
                    java.awt.Window.getWindows().forEach { w ->
                        if (w.isVisible || w.isShowing) w.toFront()
                    }
                }
            }
        }, 300L, TimeUnit.MILLISECONDS)
    }


    private fun onTrayRightClick(e: MouseEvent) {
        if (e.button != MouseEvent.BUTTON3) return
        val now = System.currentTimeMillis()
        if (now - lastRightClickActMs < 300) {
            println("[RiPlay-Tray] click destro: secondo evento dello stesso click, ignoro")
            return
        }
        lastRightClickActMs = now
        println("[RiPlay-Tray] click destro ricevuto a (${e.xOnScreen},${e.yOnScreen}) " +
                "(${if (e.id == MouseEvent.MOUSE_PRESSED) "pressed" else "released"})")
        val w = menuWindow
        if (w != null && w.isVisible) {
            println("[RiPlay-Tray] menu: click destro mentre aperto -> chiudo (toggle)")
            w.dismiss()
        } else if (System.currentTimeMillis() - menuHiddenByFocusLossAtMs < 350) {
            menuHiddenByFocusLossAtMs = 0L
            println("[RiPlay-Tray] menu: chiuso dal click corrente (focus alla taskbar), non riapro")
        } else {
            runCatching { openMenuAt(e.xOnScreen, e.yOnScreen) }
                .onFailure { println("[RiPlay-Tray] ERRORE aprendo il menu: $it") }
        }
    }

    /** Costruisce le voci con lo stato CORRENTE del player e apre il menu
     *  disegnato su misura nel punto dell'icona del tray (si e' gia' sull'
     *  AWT-EDT: gli eventi del TrayIcon arrivano lì). */
    private fun openMenuAt(eventX: Int, eventY: Int) {
        val pointer = runCatching { MouseInfo.getPointerInfo() }.getOrNull()
        val px = pointer?.location?.x ?: eventX
        val py = pointer?.location?.y ?: eventY
        if (px != eventX || py != eventY) {
            println("[RiPlay-Tray] spazio coordinate diverso: evento=($eventX,$eventY), " +
                    "puntatore=($px,$py) -> uso il puntatore (coerente con setLocation)")
        }
        val playing =
            runCatching { DesktopPlaybackHub.activeController?.state?.value?.isPlaying == true }
                .getOrDefault(false)
        val entries: List<Any> = listOf(
            MenuEntry(ds("tray.show")) { showWindow?.invoke() },
            MENU_SEPARATOR,
            MenuEntry(ds("player.previous")) {
                runCatching { DesktopPlaybackHub.onPrevious?.invoke() }
            },
            MenuEntry(if (playing) ds("player.pause") else ds("player.play")) {
                runCatching {
                    val c = DesktopPlaybackHub.activeController ?: return@runCatching
                    if (c.state.value.isPlaying) c.pause() else c.play()
                }
            },
            MenuEntry(ds("player.next")) {
                runCatching { DesktopPlaybackHub.onNext?.invoke() }
            },
            MENU_SEPARATOR,
            MenuEntry(ds("tray.quit")) { requestExit?.invoke() },
        )
        val win = menuWindow ?: TrayMenuWindow().also { menuWindow = it }
        win.openAt(entries, px, py, pointer?.device)
    }

    /** Aggiorna il tooltip con il titolo del brano corrente.
     *  Eseguito sull'EDT: le proprieta' AWT non vanno toccate da thread
     *  esterni. (Il menu ricostruisce le voci ad ogni apertura, quindi non
     *  serve piu' aggiornare l'etichetta play/pausa a tempo.) */
    private fun refreshDynamicLabel() {
        val title = runCatching { DesktopPlaybackHub.currentSongTitle() }.getOrNull()
        EventQueue.invokeLater {
            runCatching {
                trayIcon?.toolTip = if (title.isNullOrBlank()) "RiPlay"
                                    else "RiPlay — $title"
            }
        }
    }


    private var cachedAppIcon: BufferedImage? = null

    private fun appIconImage(): BufferedImage {
        cachedAppIcon?.let { return it }
        val decoded = runCatching {
            val png = Base64.getMimeDecoder().decode(APP_ICON_PNG_B64.trim())
            ImageIO.read(ByteArrayInputStream(png))
        }.getOrNull()
        return (decoded ?: drawFallbackIcon()).also { cachedAppIcon = it }
    }

    private class TrayMenuWindow : JDialog() {

        /** Riga disegnata: voce (indice in [entries]) o separatore. */
        private class Row(val entryIdx: Int, val separator: Boolean,
                          val y: Int, val height: Int)

        private var entries: List<Any> = emptyList()
        private var rows: List<Row> = emptyList()
        private var hoverRow = -1

        /** STEP 10k: istante dell'ultima azione su una voce: antirimbalzo
         *  pressione/rilascio per far agire lo stesso click fisico una
         *  volta sola (vedi [RowPanel] e [clickOutsideRows]). */
        private var lastRowActMs = 0L

        /** STEP 10k: i pannelli delle voci (componenti Swing reali). */
        private val rowPanels = ArrayList<RowPanel>()

        /** Istante di apertura (per la grazia focus, vedi windowLostFocus). */
        private var openedAtMs = 0L

        /** STEP 10j: timer dei tentativi ripetuti di presa del focus (il
         *  primo requestFocusInWindow puo' perdere la gara con
         *  l'attivazione della taskbar scatenata dal click sull'icona). */
        private var focusRetryTimer: javax.swing.Timer? = null

        /** Ascoltatore globale attivo solo a menu aperto ([installGrab]). */
        private var outsideGrab: AWTEventListener? = null

        private val bg = Color(0xF9, 0xF9, 0xF9)
        private val hoverBg = Color(0xEA, 0xEA, 0xEA)
        private val fg = Color(0x1B, 0x1B, 0x1B)
        private val borderCol = Color(0xD9, 0xD9, 0xD9)
        private val font = Font("Segoe UI", Font.PLAIN, 12)

        init {
            isUndecorated = true
            focusableWindowState = true
            isAlwaysOnTop = true          // mai sotto la taskbar o l'app
            background = bg

            val cp0 = contentPane as javax.swing.JComponent
            cp0.background = bg
            cp0.isOpaque = true
            cp0.border = javax.swing.border.LineBorder(borderCol, 1)
            cp0.layout = null
            addWindowFocusListener(object : WindowAdapter() {
                override fun windowGainedFocus(e: WindowEvent) {
                    println("[RiPlay-Tray] menu: focus ottenuto")
                }

                override fun windowLostFocus(e: WindowEvent) {
                    if (!isVisible) return
                    val dt = System.currentTimeMillis() - openedAtMs
                    if (dt < 200) {
                        println("[RiPlay-Tray] menu: focus perso dopo ${dt}ms " +
                                "dall'apertura (gara di attivazione, ignoro e riprovo)")
                        return
                    }
                    println("[RiPlay-Tray] menu: chiuso (focus andato a un'altra finestra " +
                            "dopo ${dt}ms)")
                    hideNow(byFocusLoss = true)
                }
            })

            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) = clickOutsideRows(e)

                override fun mouseReleased(e: MouseEvent) = clickOutsideRows(e)

                override fun mouseExited(e: MouseEvent) = setHover(-1)
            })
            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    setHover(rows.indexOfFirst {
                        !it.separator && e.y >= it.y && e.y < it.y + it.height
                    })
                }
            })
            // ESC = chiudi
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ESCAPE) {
                        println("[RiPlay-Tray] menu: chiuso con ESC")
                        hideNow()
                    }
                }
            })
        }

        /** Chiude il menu (usato anche dal toggle del click destro). */
        fun dismiss() = hideNow()

        private fun installGrab() {
            if (outsideGrab != null) return
            val l = AWTEventListener { ev ->
                if (ev is KeyEvent && ev.id == KeyEvent.KEY_PRESSED &&
                    ev.keyCode == KeyEvent.VK_ESCAPE && isVisible) {
                    println("[RiPlay-Tray] menu: chiuso con ESC (grab globale)")
                    hideNow()
                    return@AWTEventListener
                }
                if (ev is MouseEvent && ev.id == MouseEvent.MOUSE_PRESSED && isVisible) {
                    // la sorgente puo' non essere un Component (es. gli
                    // eventi del TrayIcon, che arrivano pure qui): non e'
                    // un click su una finestra dell'app, ignora
                    val src = ev.source as? java.awt.Component
                        ?: return@AWTEventListener
                    val win = javax.swing.SwingUtilities.getWindowAncestor(src)
                    if (win != null && win !== this) {
                        println("[RiPlay-Tray] menu: click su un'altra finestra dell'app -> chiudo")
                        hideNow()
                    }
                }
            }
            outsideGrab = l
            Toolkit.getDefaultToolkit().addAWTEventListener(
                l, AWTEvent.MOUSE_EVENT_MASK or AWTEvent.KEY_EVENT_MASK)
        }

        private fun removeGrab() {
            val l = outsideGrab ?: return
            outsideGrab = null
            runCatching { Toolkit.getDefaultToolkit().removeAWTEventListener(l) }
        }

        /** Costruisce righe e dimensioni, posiziona sopra il punto del tray
         *  (dentro l'area utile dello schermo, senza coprire la taskbar) e
         *  mostra la finestra prendendone il focus. */
        fun openAt(newEntries: List<Any>, screenX: Int, screenY: Int,
                   pointerDevice: GraphicsDevice?) {
            entries = newEntries
            hoverRow = -1

            val fm = getFontMetrics(font)
            val rowH = fm.height + 16          // la spaziatura che mancava
            val sepH = 9
            val padX = 14
            val textW = entries.filterIsInstance<MenuEntry>()
                .maxOfOrNull { fm.stringWidth(it.label) } ?: 0
            val w = textW + padX * 2 + 12
            var h = 8
            val built = ArrayList<Row>()
            entries.forEachIndexed { i, e ->
                if (e is MenuSeparator) {
                    built.add(Row(-1, true, h, sepH)); h += sepH
                } else {
                    built.add(Row(i, false, h, rowH)); h += rowH
                }
            }
            h += 8
            rows = built
            setSize(w, h)

            lastRowActMs = 0L
            val cp = contentPane as javax.swing.JComponent
            cp.removeAll()
            rowPanels.clear()
            built.forEachIndexed { idx, row ->
                val p = RowPanel(idx, row)
                p.setBounds(1, row.y, w - 2, row.height)
                cp.add(p)
                rowPanels.add(p)
            }
            cp.revalidate()
            cp.repaint()

            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()

            val screen = pointerDevice?.defaultConfiguration
                ?: ge.screenDevices.map { it.defaultConfiguration }
                    .firstOrNull { it.bounds.contains(screenX, screenY) }
                ?: ge.defaultScreenDevice.defaultConfiguration
            val b = screen.bounds
            val ins = Toolkit.getDefaultToolkit().getScreenInsets(screen)
            val nx = (screenX - w / 2).coerceIn(b.x + ins.left, b.x + b.width - ins.right - w)
            val above = screenY - h - 6        // il tray e' in basso: apri sopra
            val bottom = b.y + b.height - ins.bottom - h
            var ny = if (above >= b.y + ins.top) above
                     else (screenY + 6).coerceAtMost(bottom)

            ny = ny.coerceIn(b.y + ins.top, bottom.coerceAtLeast(b.y + ins.top))
            setLocation(nx, ny)

            openedAtMs = System.currentTimeMillis()
            installGrab()
            isVisible = true
            toFront()
            requestFocusInWindow()
            startFocusRetries()
            repaint()
            println("[RiPlay-Tray] menu: apro a ($nx,$ny) ${w}x${h}px " +
                    "(riga $rowH px, metriche font ${fm.height} px, schermo " +
                    "${b.x},${b.y} ${b.width}x${b.height}, dpi " +
                    "${Toolkit.getDefaultToolkit().screenResolution})")
        }


        private fun startFocusRetries() {
            focusRetryTimer?.stop()
            var attempt = 0
            val t = javax.swing.Timer(80, null)
            t.isRepeats = true
            t.addActionListener {
                attempt++
                if (!isVisible || isFocused || attempt > 5) {
                    if (attempt > 5) {
                        println("[RiPlay-Tray] menu: focus non ottenuto dopo $attempt " +
                                "tentativi (chiudibile comunque: tray, voci, ESC)")
                    }
                    t.stop()
                    return@addActionListener
                }
                toFront()
                val ok = requestFocusInWindow()
                println("[RiPlay-Tray] menu: tentativo focus #$attempt (richiesta accettata: $ok)")
            }
            t.start()
            focusRetryTimer = t
        }

        private fun hideNow(byFocusLoss: Boolean = false) {
            focusRetryTimer?.stop()
            removeGrab()
            if (isVisible) {
                isVisible = false
                hoverRow = -1
                if (byFocusLoss) {
                    DesktopTray.menuHiddenByFocusLossAtMs = System.currentTimeMillis()
                }
            }
        }

        private fun setHover(idx: Int) {
            if (idx == hoverRow) return
            val old = hoverRow
            hoverRow = idx

            listOf(old, idx).filter { it in rowPanels.indices }
                .forEach { rowPanels[it].repaint() }
        }


        private fun clickOutsideRows(e: MouseEvent) {
            val row = rows.firstOrNull { e.y >= it.y && e.y < it.y + it.height }
            val entry = row?.takeIf { !it.separator }
                ?.let { entries[it.entryIdx] as? MenuEntry }
            if (entry != null) activateEntry(entry) else hideNow()
        }


        private fun activateEntry(entry: MenuEntry) {
            val now = System.currentTimeMillis()
            if (now - lastRowActMs < 300) {
                println("[RiPlay-Tray] menu: secondo evento dello stesso click sulla voce, ignoro")
                return
            }
            lastRowActMs = now
            println("[RiPlay-Tray] menu: scelta la voce \"${entry.label}\"")
            runCatching { entry.action?.invoke() }
                .onFailure { println("[RiPlay-Tray] menu: ERRORE nell'azione della voce: $it") }
            hideNow()
        }


        private inner class RowPanel(val rowIdx: Int, val row: Row) : JPanel() {

            init {
                isOpaque = true
                background = bg
                if (!row.separator) {
                    addMouseListener(object : MouseAdapter() {
                        override fun mousePressed(e: MouseEvent) = activate()

                        override fun mouseReleased(e: MouseEvent) = activate()

                        override fun mouseEntered(e: MouseEvent) = setHover(rowIdx)

                        override fun mouseExited(e: MouseEvent) {
                            if (hoverRow == rowIdx) setHover(-1)
                        }
                    })
                }
            }

            /** Esegue la voce (solo righe non separatore: i separatori non
             *  hanno listener). */
            private fun activate() {
                val entry = entries.getOrNull(row.entryIdx) as? MenuEntry
                             ?: return
                activateEntry(entry)
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    if (row.separator) {
                        g2.color = bg
                        g2.fillRect(0, 0, width, height)
                        g2.color = borderCol
                        g2.fillRect(9, height / 2, width - 18, 1)
                    } else {
                        g2.color = bg
                        g2.fillRect(0, 0, width, height)
                        if (rowIdx == hoverRow) {
                            g2.color = hoverBg
                            g2.fillRect(3, 0, width - 6, height)
                        }
                        val label = (entries[row.entryIdx] as MenuEntry).label
                        g2.font = font
                        val fm = g2.getFontMetrics(font)
                        g2.color = fg
                        g2.drawString(label, 13,
                                      (height - fm.height) / 2 + fm.ascent)
                    }
                } finally {
                    g2.dispose()
                }
            }
        }
    }

    /** Disegno di riserva (cerchio accent + triangolo play), usato solo se
     *  il PNG incorporato non fosse decodificabile. */
    private fun drawFallbackIcon(): BufferedImage {
        val s = 64
        val img = BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        // cerchio accent (RiPlayAccent 0xFF30D158, leggermente scurito per
        // restare leggibile su taskbar chiare)
        g.color = Color(0x2A, 0xB8, 0x4D)
        g.fillOval(0, 0, s - 1, s - 1)
        // bordo interno piu' scuro
        g.color = Color(0x22, 0x8F, 0x3E)
        g.drawOval(1, 1, s - 3, s - 3)
        // triangolo play bianco
        g.color = Color.WHITE
        val tri = Polygon()
        tri.addPoint((s * 0.40).toInt(), (s * 0.28).toInt())
        tri.addPoint((s * 0.40).toInt(), (s * 0.72).toInt())
        tri.addPoint((s * 0.75).toInt(), (s * 0.50).toInt())
        g.fillPolygon(tri)
        g.dispose()
        return img
    }

    /**
     * Il vero logo RiPlay: PNG 128x128 incorporato in base64, estratto
     * dalla voce 128x128 di desktop-icons/icon-windows.ico (generato con
     * scripts/step10_extract_tray_icon.py). Stringa raw multi-riga: il
     * MimeDecoder ignora gli a-capo.
     */
    private const val APP_ICON_PNG_B64 = """
iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAoAklEQVR4nO19eZgdVZn++51zquou3Z1EJYjojOIokqCQpBPCIt0RFxxcxhnvTTo4w2+QCSoKOoySpBOqC9Ik
kcEZNyBxG52BpO8dRUafER013SwCoUMikqigiKigEUjSy723qs453++PurfpQLbuvp10h7zPkye9VNetOuc7374QJhcoV8iJncfNoJ7WDgMiHv7Li+9e21jJxq+1hl4P0CkQ
9Fq29jUAHU/ASwFMASAP/BFcBtOzAD/NjD9A4FcE8QiAn7PVj3bNXfl7EPb63JZNvgKA1m7YIAhsXd94nEFH+gEOBT77orsbomdBoIf//ML71rzSOHY2GGcSMIcJJwM4QbqO
I5QAM8DWgo0FGwbbg+8NEYGkAEkCCQESBLYME2mwMXtIyMdA9BAR7oOi+2IM7iieGkTDn3UHZlIROft8Ap2ImLgEwEw5FMXwhfR9Xzz2d5nZ2vI72dq3w2KWk/WyQklYY2Ei
DRsbMLMlMAMEZhCBCSCADvF9mZmJGAwQJd8QgUgKkq6CcCRABD0Ygtn+mkH3gO0d0truW5pXPFW7TY4Lcga2c0ATlytMOALwfV90t+592hdv/fTplvj9sPa9RHSqk03Bag1T
icGWDQAGmJghiAgYr/fihKkQobahSroK0nPAzIgHw11C0I/Z4r9d4d3x9Vmf2F17px0zZ1Ixnzfj8lxjwIQhAN/3xY6OmVSkZJHeu8mfmpmafh8RLmLD5zqNKTJhsukgaAAE
hjjkUz1OYMASYMFMpKRUaRcAoEvRH0D4Fln++q1zlm2pXZ/jgqy940TAkSeAGquvLsrfP7zmL7TGP4FxkZNxX2WNhS5FAKCTEw5xZB/4AGBmIOEOwlFSZVzEAyEgcQcJ+uKt
b7zqu8l1oFyxICYCRziSBEA5Lgxt/OLNq0+CKy8H8UUq7U7V5Qg20gYJSz+I5j4BwcwgMgCUk/XAzLCxuZfAn7nltKX/DSQKIwAcSR3hiBBArpCTxXzRAMCiezuPp7T6ZwAf
cjJuUzxQAVvWzCypKtAnPZgNg8jJuIIEwUb6Hm24s2v20u8BVbGAvH2+eXk4cHgXmEE5JKe+xffVCe9Nf0QILFMZ7+XxQAVsrAHoiMv18QIzLJjhZD0BACY2t7PRV2+c0/4Q
sPfBOFw4bAs9/OUW9na+WTrqepV2z9ClCDY2GoA8Wjf++WBmCxDcxpQwYVxm5hv6nyiv/u57gtLh5gaHZcFbNvmqZ0Gg3/b9K7MvO+6l15KUHxdKki6FhgFx1LD6kYLZQAjp
NqURD4Y/s7G9vGvusm4g0Q8Oh24wvgvPTLliXhTzRdN2/3VnUkrerDLem6LdJQaxBWjyKXf1BoMZbJy0q6yxbC2vfXTzY1dvuXR9XDs44/nx40YAeyl6D66+Uii5Wghy4nKk
iUiN1+dOVjDDEpi8qVmKBsN7bGgu7jpj+SPjTQTjQgC1hz7/Pr9pWirzJSfr5qM9Za46VSeuHT8BwMzabUgpE+lnbRxfsrF5xW25QkEWc+OjF9SdAGqbn7v7mpOdBregMt6b
or6yBr94lLwxg9kIR0mhJEwYrdgwZ3knmAkdHYQ6RxvruiG1zc/3di5QrtMllDguHgyPsfxRgBmWCHCnZETcX/qq/tXWJcV80dRbOawbAdQ2f+H9q3Iq5f4Xg92qJ++Yojda
MBgE407JqHig/D29u5IvLggG6ukvqAsBDG3+A50fVCn3yzbWsMbaY/K+PmBm7U3JqLgU3qNL5fcUzwqerRcnGDMB1DZ/0QOrPqSy6Zt0ObJsLJGor21fZYm1F6Yk2J/E6wFi
MBMAgaPUp8DM2m3KKF0Ke8uRPf/bZyx/ph5EMKbFGpL596+6xG1Mf8lUImONradjh5F4zaRMO5CuAjODtQVbOyx7R4AtQ5cjsDaGQTSho4ajBDNrrymtdDnePLir9I7bFwS7
fd8XY0lDG7VyNrT5916zyM16XzKVyHA9N5/ZgEi6TRmpyxFMGP/MhPouNvgpBD8uYEqWoUiKlxHTyQyaD+BspykzzUYxTCU2OMo4AhGpsK+svSmZeWlrbs/95BPn4/sIwUyj
TT8b1eLUkhpy9137Fifr3cHaSqst6nbqmI2TTUkdxRpMt0jm9f8566p76SAveWHvqhMM6G9JisucbOqUeDAEa3PUKaLMrL2pWRX2lW7rmrP8b6v7YYGR+wlGTAA1DXThvdec
IrPePbCYamJdHwdPNRfPa0yLuBxuYkOf2jjnqt7ar2vZt9P/PJOBIoAckAN2dm+n1lbYmjzM/eSGtEzHHyNBy5yUMzXsq1gA9SPQCQAGx6mpWaeye/BzXc3tV4zWYzgyAqg6
I97biqb01PS9ylVv0KWwPieMwRBglXaFrsSdG2cvWwGMMLGSmVq6O2RtIRZvXn0SPLqGpLwQAEw51gDLo0gsaLcprSq7By8rzFtx42iIYEQLMWTu9Xbe7jVl3hPuKdXLycMk
yMiUo+LB6GNdc5d/AeyLXHEHjcreZVBLtz9ECIu2XHe+cFSnSjmzjyqxwMwkhRVKQkfxgq7m9rtG6iM4ZAIY2vzNnctT07Kdld2D9fPwMRu3KSPD3YNXd52x4to5vUucLXPW
67H6vodn4+YKvuu8Pn05QMtV2p0W9pdtYkZObrHADKs8JYw2vzews4uzlj/td3TQoVoGh0QAQ3L/vs43y4zbzcZYNlYe6t8fEMzGaUjLeLD8vxub2y9oYV/1IDD1DHwMPxW5
+659jZv2Agj6eyJAlyIDmuTWAieiIOwrfadrbvt7RpJ5fHDqZ6YZuRl88c/XNgpXfI0IwmorUJ/NZ5JC6Eo0wLG4DAxqBeoe9apuPrVs8lVx/srf3HLap/5Bh/E7rLVb3KkZ
SUoSmI94hu6oQVBRf1l7UzLvzm9e9ZEi5U2OC4ck4g5KADkURUCBHeyPr3ca06815dDUjW0SGachRdbodV1nLnu8pduX45gFwz0LAu37vsgVCrKrefkP4l+UzopL4ZVEeNZt
ykgGLDMmbBXPAcEs48HQSk99+gNbrvurIuVtLev4QDjgBblCwkoW3t95npPxLo32lAyEqJ/yRCTjgUoIuDeBmVpbx3/xgyCwiU6Qk8V8EG04felnTLk8R1eibyhPCSfjCjCb
ao7/5AERsTasPDcbW74JAO8ozjwol97/BQzy4dPj3XArjd5WlXLeoMuxrZ/SlDh74sFw08bm5W8Zq0tzlKCWTc9ZC4sfXP1WCFqtMl7zJLYWtNuYVpW+wYsKc1d842D6wH43
M4eCCCiw5azzidSUzBtMKaof6wcAEJMUYMKPAFB36xHRxvcSC7fOXvbDX+rHztKDlX8G0TNuU0ZygkkjFhgQOoytkHLN4odWT5vRsZ05CZTtE/tcdN/3RRE5+w9bbzhROmpp
NBBa1N9cIqsN2GIrAJ7eOvOIsdzhYmFL8/r41lnL/i0cjObEYfQfKuWQk/EmjVggQNhQW68xfYIN7fIgCGwexf3u3T5/sWPmTAIRh3HZd7JeExtjx8FMkrocMyT9AQBmYPsR
X9zh1sK3zl752w1vuuofdRS/1cT6AXfKZLIWWEb9FUtKXraw99rXFpHbr0L4gh/67ItiLm8XbllzinSdi6L+ikW9a/MYTERgtlojLtX13mPH3tbCnPYfPbmrfFZcjj5OhKfd
pozERBcLRMTWWifrpWHFShDtVyF8AQHsKO4gEBhaL1dp103i8eOXzClZ1efkc32fcbhY6FkQ6A2nXfVZE8WzdSX+qvQmhViQUX/FSle1tfV2vqGY37dZuNcPfPZFMV+0uc3X
nCxclY8Gx+H01xm5Qk76vi9A4FwhJw/F9h0JhouFrnkrf3fraZ/6IEfxW6zW97tTMlJMXLFAYLYq47qW8Unsxyzca7F2YCYBYEHycifrubDje/rHCt/3RTFfNEEQWJ83qWK+
aAIKbK5QkDiA5jsKcM+CQIOZcoWCvLW5fVP8y5POjkqVyzGxxYKM+isspGhbvOXavyzm8+b5B+S5YI7viyLlzQe2XT9dm+jCeDBkMEtMVBd51W+w6P7rzpcZ58pHH9p8Ytu2
tdvBfP2GWfnNwDh04yDiIlB1IuUNgM9feJ9/W0zUIV3ng6TERKt3JDBrpzGdjvaUlwBo7+6GAJ5zuA1RQ0vVDo/j+ANuY2YKG2smaoAkVyhIBIFduKXzPJl1vycc8VaATnHS
7vtJ0D1tW1d/tq3Xf1mR8gbMNJ5i4Zb5we83nHbVJToyC0xs7nObMlI6E0osCF2OAPBFuYf9hoSTPcfVhxamp7XDtGzyFZgvtpEG80QOkxaT/wxfJZRA3FeJTKxt1FcyNjbK
yaYuh8psXbRt7cUg4oACm+PxFQtdc5d160d6z4lL4ccY9Gd3yoQRC8JG2jiN6RNV6L4LAFq6/SG9TgDVE0XExzeps1TamVlfl2/dMZQkQkK83MaamUhRkgAqAXDYV9ZEeKWT
cr7Stm3tpnzvmvlFyhsQ8aFGyQ79aYiHYgsLi2bDrKVf0KV4VlyKviRcNWQt8JG2FhhsLf0/ABgec0k2OVf9xooLpecANGkiYvtyUBEBykSa4/6yUa5qVQ7dvXjb2s+/7x5/
+riKBU4SZ4pnrfjDhtOvWqK1bbGxvjcRC4r4SIkFIhGXQpBAa+6+a18TUGB9P3l/AQZVlb8sgAsSeTFhT/9e4APY/rXWjvFgxdjYSJVxP5rOprcu+umafxpHsYAhscAFWZiz
7M7X3VY+Jx6MLgPwJ68qFoDDTggEZuM2pj0p5HsBoBZ7ETkUBABEOjxHZbwTTWQsJgkBHBKq0bywr6xB9Ao35a5v27a2J/9A59njKhYoEQtBR8AbZl91ox0ozdalcL1wFam0
Jw+/WGCy2oIJfwMAPd3VdnY7u7cnrTVZvFs4cngXzKMKNbEQ9ZWNdNW50lV3Lt665sZF93Yen4gFUK6QqyshFPNFAwK3bPLVxnOCJ289femlJtbnGq3vGRILwLh2ABkCkzCV
CESYt/Bn174KQWB99oXoWRBUtX+cZ0JNE1v7HxtqYkEPVgxHhlTW+zCl1da2rWsuBSUbNt5ioau5/a4Nb1r65rgcfgTgP3pNaQUGj7vZSCA21jiNqTQi2QoA3d0QAgBPn+K+
npR4vQnjSZ8le0ggkiBQ2FfWBJzgZLybF29de2db75pzamKhVoRSx88cEgsAsOH0pTeV9+jZphzeLBwJlU2Nv1ggYgKBLL8NAKa3zuSqJogz3QZP4HCxowkCApSJNUd9JSM8
eQ4purNt65qb3te76oSaw2Q8xcJtLSueuuW0pR9mzeeaKL7LbUqPq1hghjBRDAbOXNK7zilS3ggAIIuzx+MDJwOeEwuhsbGBk/U+lJZqa9uD130EHYnPYbzFwobmpXdvOH3p
uXEp+hADTyVigZltfbkBEYSJNIQUr+nH068FAOGzLxg0y8YT3fs3zhgmFkB0vJNNf7HtfWvvWtjb+ebDIhaYacOspeu4rGfpcniT9BySniK2dfYk2iQXk9nOAgDxi4fViQCf
ZCKTjFR4kWO4WJCuPEs66s7FD61Zt+hu/xU9CwINSkLQ9fzMRCwkBLbxzPY/3Xra0o/oSvTXIHrSyVbzDuoFIiZBAMRsABAI5SlSqSY2lo918UrwnBMptDYyrFLeEsqmty7e
uvoy/+okBD2eYiHJPVjxPdO351wT6V+qjCfrxgmYyRoLJpwKJJ7AU4WrAByd9v9YQJQ0ro72lAwRTVeZ1BcefV/67rbNnS3jKRZ6FgR6Tu8Sp+uczl/rgfLbTWx+L1OOqEfR
ChORjQ3AeG3uYd8VRDgl8aZP1MymCQAiOWQtOOpM8lT34m1rvpTbuurEWk1BvcXClub1cQv7qvjm4AkbRQtBiIUkxhg3ijjJxgbjBCdUJwgAr7MmmXhSn0c/OjEkFkqhtZFm
lfEuccjZumjb6o/Vik/rnYnUQ1VOMG/lT0wp/FenISUx1vBy4hBioUTWsPgLAeCVbCySyVrHcDAkYoEo2lMyYD7OTac+p97QfHe+t3NBMb+XWKjLem6Zs0777Ityk7s66i//
Qbhqr4ye0YAZLFxFAF4lwDiejcVEzf6ZsCCSVpvEWpByvuM6P2776ZqvLNx87auqYoHrIhaIuLsb4n9Ouaoflm9WGZcwRgIgAgshABavEiA0sD0m/0eFYWLBhDE7Ke9i6ToP
tj24+oqWTb56TiyMjRtUEziIyW6I+ypRLfFl9HdMOmNA2Je/eB0/dcSQWOgrGWa8zGlI/fuJL8ncs6h39VsSsTA2xS2gwDIzuppX/toau02lHALGogtQokowv+wYAdQTRJK1
4WhPyQgl50lP/WjRltVfzj1xQ9r3fTEWTtDa3SEBgITYLByJpFnq6MBc7bLKNPUYAdQbw8VCJTLp45o+KHeGNwRBYHPF3NjXm/BIHZ4SyQxUNBwjgHFC0jeRKHym3wC8OPcT
/yVJ3uDYrC1j+Wk2Fhij1cYAIOAdI4DxBVlmYpByXccDAL+jY1QbVyufF0T1CRUnAkQdI4DxAoMBjrwpGUHgB25pXvHHsXRBGUrdIzSCqD6OWx5Ds+hjOACq4+C8powXD4SP
Go3LhjYsGNutreW/JCkw2ubQQ0jIKT7GAeoJZmaGdZrSkpQYjAbKqwcfG5xXnN++AxjbjOBaMQcRn87GYqy6BCXB//IxDlAvMDMJgkp7wpSjojXwu+Ys+zlQhyGQDAoosO+5
e20jrJ5vwhgYa+p+EgAcOMYB6gEGQ0ompdiUKx+69fSl+a45S39ezbamsfY+bOn2JRiUTenz3MbUdBONrWUPEXNVj9ilqoOJjsUBxgAG2EkpEQ+Gl3TNbf+Kv8lXSNrX10Vj
n/7nmQwCm838UYmhMTljuicRgYl2KhB2kRAvYWsZx1LCRo6k5EpG/eXbuua2f2VJ7zonaL5Uo04JFrUeB/nNq9/qZtR58UBoacy9CxNXMAFPCTD/SSiReIaOYTQQJoxZSVoN
Ztr12LRRTe7YJ5hpBrZz7okb0kLh88z1uS8DxIZhrf2dIMJvSQrwGHzLL1YwYGXKJRPpR6JZf/VgtVS8brl7Ld0dMqDAyp2VdW7We4OpRHUp20/Sw2MDJZ4QFniEBFXlyjGM
BMTMwpEA4ZdJendhjGHaBDlO+jX0LAj0oi1rPu02Zv4+6q/oek1mEVICjD1C8RNKMHawZTBPkK42RxKJQmzBXHW0MHGibh/w1AmiChhUa1wyWvi+L3Z0zKQi5c1Fm/xUNDX1
BSfjfTAaqGjUyWnHAJOSxNr83v5i29OKgIdNJWZ6MVsCST2eJSmk9BxJUiRWkmWYSKNaMv/CusnqNWxxAghc5Nzo2H91xE2wINAIgLbNnS2RKz/jpL3ZUV/JoI6zlwnM0pHQ
ZX60mC8aIbP0C7b8dLXf3YtODLBlS0qSOyUj2dpyPBhujgfKXVF/5RvxYOUHVps/OFn3uTbye/0xCRPGAPGb2nr/9WUAAH9knUcSdp/0G1q8efVJbQ+t/Rp5qlsqWdv8uvcu
IEEA0U8BQP3njOXPLOy97hHhquOs1hO+MWRdwWychpQ0UfyMLoU3sBQbupqXPT78kgsf+WyTHiydRxD/4jSlz4r7KxbMCYsgEGtj3KbMlGj34GIQfW5J7zq1Pjh4zp7PvuhA
BxORyW3yG5ypqY9DiCsdz50a9ZXYgHjs5t6+XpmF1QZCYAtQlSvE6JWOPFu/mCyB2uaH5i5diS8qzl/5m+TnoFyxIJBLGlgHdEUfgNvA/O22rWtWqpQbmCg2bFiAQCAiXY5Y
uM7y3P1+YX3zpX9c0rvEWd+8Pt7Xx9bkfEB5EyDAogfXtpFgX2VSJ8cDFdRO/biU6TFYSCGiwbCidPgQMKRY8N1s7BXVEaR1/9yJBma2TjYl43J074CtnP/d+UFpSe8S54Tv
nGACCmwRw5pLMiiHgpiBDg5mB9cseqCzXzWkPqMHQ4OEWwoTaes0eMcrpL55wV1L37W+ec0un33R3Q1Ri+Pv7N5O01tnckB5gwBo23LdmSTFNdJTb7XaIOwraUoac44bB2aA
lecQl6Mdt8wP/gDmRLlgtvdHA5UKSZFiw0e3a5iZhZJkQ72LtV743XlBqWWTr9Y3B/s8sSBwEUkLmSW965z1zZf+26IHOs9wG9MLo/5ycloFiXggtG5D6qwpNO2uhVs6rwio
/UfYR/r2wq2rXy2JlgF0ifSUiAcqtWHXarwPHxGscKRACXcB4BZ0KMWJ/fe7hb2dP1Wee4YuhxaYdGNSDh1Exsl6KtpV+kzXGSt/d8jTNgl8Aj9pwEy8bc3SuBy9m4RIs00O
DAkS0UDFqrQ7U5Lzw7YHV29iY+8A0y8YNhZS/QUTvxmMd6us1xT1lWFjXWP3hwXMEDY2IOAHAIBuQHRUs01BuEM4CmPJNp3wYDAJUnF/uWxk9I2RDqoKKLC5Yl50zVr2uI3t
D1TWIxCGxAUJEroSWxvFLFPuAndKZq1q8G53GlL/6zS4N7tZ70ICNUV7SsnfHM55RMwsHSniUuWZkgp/AiTdYcWOPycyisDf1aWQwUevFcAAS88BMz9cbA6eAGjESRo7j5tB
1fjpj5I6+71B1SGUulQxUV9Z61JodCkyUX9FR/0VzcbwERlERWRk2mFBYtPts4LduUJOgohFMZ/INzP79VtNrHeolEOTdnbeQUAEm6RTiccBIFcsjNivPr01Cc2C7a+tTjrW
7ufTJBIlW1b/KQDqiJXgMRMYZKz4JlAlZFRdnC3dvixS3hDom8JTOFp7BQJIvHfGagBUa5E7IlTdvVLJGHzgbqUTBsxMjpTxQPkZleLvA0BPa2CAKgHU5KAx3BUPhhpHqzOI
mWAZkHQ8AJ7RMfJBVTuP204AyDCfQEqAJkMcncg4GQ9g+s6tb1q2K2kOXisNQDVZkX1RnN++w8b6LifrYQL1u68nhAk1YPnUC+/zm4IgsCMt16ra9QxL84lo7Nm5hwOcdAcj
5v94/q+eGxjRXf1ainVHbak4EVmtjduQmm6k+3Zg7975B0U1QeNdvX4GZN+jy3VIzhx/GJV2SZejbfrx190NZqpOOwEwfGDEgsCAQTb0/ifuLz8uPWfMjQgmJghsLDNhea1l
+qGmWC/Zsl4FFNi0UR/yGjOvtFFsMNEJgBnClQQhbirm86alZvZXMfzhuaXbl8Wzriwz+GaZHnsjggkKqcuR9Rozs375Lu/angWBbunuOPC0see8gHHu/lWnOyk3iEvhZOiq
boWrRNhXfkrFmY1gUE9rx16ifa84c09rhwEH5G4WX472lD8plHiJ1XYyvOjIQCTjgbJRWXf5wi2r+7rmLFvbg6TR087jZtBw/z2Q9OpZj0vjRfevahYp59tE1GC0sZMghcaq
jKfs7vKNt8y/oi/xeu5dW7h3okG1v81/Llj+zKLNnTeqbHZl1Wt1dBEAALaQuhRZN+utadu6utmQCgqnf/LhfV2b610zRQj8E0l0EFFWh3oij9SpwQopZNRXfoaocjMY1NoB
2/O8i16QadLaDdvDTLSl43NRH31YKPHSo5MLAABENFCxbmP6/ShF72l7cM3/WW27heBfGaZIEr3CEs8TxOc72dSr4sEKtJkUmw8wrMqmVLRn8N83zA2eznFBBsELR+i9gACC
ILC5jplyQ3PwdFvv6k+rbOrT0a5Bi324PY8GEJGI+8sGQrhOxr2ApLiAjYVkgASBBEGXoyROn8wDHNp8BmzND8BM1RSRI+8YYsBKV8qwr/SkJfl5TnoA7lOf22euWRE56/u+
eAqVL/b18aUy5Z6kwwk9SWxsIJKwzPFAxVbt+mQTq8mh1UGQz2nPiY9EKM8RSbsWwBoLE2qAWVcHbh4xQiBmVmlX2Eqpo3jG0j15PkliPwM0972hRLyjYyatbw5KDP6UcAVN
Co/XWJBk9+ztv69+P5QVnLh+rdOQktJzyET6N/FguCkerPxQV+KfMXPsNqUVSUlHbl4gG5X1ZNhX6jXzXvdVn31RxP5rFfZ7omt57l1z2r8V9le+5zSm5UTzDtIYu2+NCJy0
VnKynojL8XfYxG8x0Z9nbmxe/paNze1v65qz7DQYnBqXo4AF+pyMJ+re6v0QnpKIwNoyrL2iSHmzoziTDtSl7IDpxjNy2xkMUg+Ij+py9BApmWZteMzsjUDMDCIhnTBOj+hv
qwx6Se86p4+fzh4WxsRgSMFCCjKV8LKNs5ffWPtVzX8QENku4BEAHQvvvabLgjY4We+0eDC0JOjwiE5rrTOtQYbPDnyx64yVP8kVCnK4129fOOCDBRTYHAri1nnLHjORWeY0
pARA9eECzMbJesI68mQwU3f3oekXPnwCg/bEz54IIV5pD8ecA4JVKVeYSnTZhtnLb2zZ5KtaA8iAAlvNKSDf98WS3nVO15lX/zzu028zsf7V4QqvM8PKtCujvvJj2SZnmc++
KOYOXqdw0EUvUt7kuCALZ7R/Ptw18H9OU0rVTRQQwODcSAIq3d0QILBw+K/dxlSKrTXjqnlzMmEjHqx8p+uMlTct6V3n9LQGZh8NIDkIAru++dJ4Tu86p3hu+59txVwMgIQY
d1HFtc+wbC/56ilX9Ses/+DrekinbgaqosBTF8fl6GnhqLH3rqckkVK57t/k7l91es+CQB+s977Pvmjthn3X//gZJrrSVOJqz9NxhbCxZrBdM1T9exDdY0vzpXHLJl91zW+/
S1fiW8Zbf2Jm407JSF2KVxea2zclbWoPzPprOCQCSHLhCuKW05b+3kb6YuFKEpLGWgZNbC1Ikqtc9fXc/101pWdBoJf0rnN8HtZVkxPW2rLJVwEFNggCmz0xdaOT9U7SlZgx
jqYpA1Z6Dukw+s0UMf2BkVT/trbCgpmQ9pbFA5UBGq/KK8vGbUyrcHe5x/72dStzXJC1ZI9DwYhOTy2DduHmVdempjWsCPcMxgA5I37oYWDL1m1ICR3p+1AO/2HDmVc/ur9r
c5v8Bjkl9Tkn4/1jPFCp5eWPH5iN05CW0UDl+11zl58/0l4/teYOC+9ftdJ7ScM10a5BA1G/fEBmWOkpwcb8UZdNc/GsFU/67I+oJc1I2Se1bPJlz4JAL9xy3be8xvT7wj0l
TWMtXmQ2KuNJE+vdxPR5HetCejD61dcXBJUlveucXXrnK5WS74QUlzsZ9+Sov2wOS+o6s3Ea0zLuK9++cW77+3LFgjhU1lr9e/LRQU9teUWqD888LF31ah1qrodDjS2zdKSF
FKz7w/MKZ6+8s0ZwI7nPyOUnM/kdHbQjh4yKUj0q7c6uulLHtCHMbIWSwsl6iPoqFmyfANGzALLM+Eu3KZWykYapxPUvmNz/UxmV8WRcCu/tam4/q5oNPCI2XtuUxb2rc7LB
K8TVYpKxPRYYIqlvCHcNXlw4c+XXDrm+4XkYOSUSMTqA4qnBQCWqvFeH8W9VxpOwY1NyiEiwNhz1lTXAQnrOq1XanS0952QhKRX3lY2uaHt4c+kpSSEDzfzAtuunAxhx9W9t
LuCtzcuK0Z5ST3Xsy9gUQoJxm9Iq2lO6eiybD4wywhdQYHOFgrxtfvB7DqMLrOE/y5RThxcjQuKcYh1pG5ciqyuxTcrVSB72WES1+tdrSjfpMF4EAre0juYZkvRjqeSVNtaG
hABGq0ATYm9qVkW7S5/tOmPFtWPZ/OR2Y8BQB6t7grlOY/r7AKaZ8HCy6MMCKxxJbMxTpOjU172xsqda1j0yUVD1yi3cfO2XvKkNl0S7S6NRCLU3NaPCPeX1G+csuzTHBVlE
/qBm6YEwphNVpLxp2eSrwtn+A2ZQv5OZn60LJ5hYEDbS1mnMvMJEpj2gwOZRHPG6zdi+ncFMNuOtjAbCXeSqQzcLk6v23vxCQRaRG9PmA3VI8qg5cDaeufz+uC98OzOeUhlP
MvPRM4mcSER9ZSsc9dGFW645pUh5O9KBUEEQ2JbuDlk89VN/RGw6nYx3SEm3nATzjTslo8I9pc9unLPs0iE3bx1S0usiU2tEUHzz1VtMWbfayDziNqbVUUQEBGtZea7Hhq4H
wBhFWVFPa2B89sVx0xq+EPaVfiHTrgCwX27JDEuCoBpSMtwz6G+cs/zjuUJOBgi4XvUIdVOqehYEOscF2XXG8kdsRZ+rK/GdqalZBUAfFb2HqomkbkP6goWbV72zmB9qCzeC
e4B3FGfS519/RQjgkyTFfhv/M7NRrhLCUSbqL1/S1dx+TY4LspgvjpntD0ddteqaybPxzPY/6UdKbwv7y19zm9IKRDhyCRL1BIEtA0Jcn3vYd2vh8pHcoUY4Xc3Lvxv3V+5w
Gl4YJ2Bm7TamJROetKXwHV1z27/SsslXVSdPXQ9T3c2qYr5ofPZFMR9EG2cvuzjqK39CeEqrlCuOApEgTTkyXlN6pii5Hw4osCOqLBpCUmFKjvwXE8YRhCAkKScWgPWmZpUO
4+5Kf3jWxvkrfzxWU+9AGMcwatJsqZjPm7bea88h1/uySjknR33lxKqfvPmFlhwJGN4lhZzxX7cNPO0jUfJGcpOhOMHmzs9607KXh7sGQpV2PYBgY/3pJ2+/o70n6NGjce+O
BOO3CQQu5hMzcUPzyrsHnxmcHw9GX1EZV0pPCWZMVm4gODbWafBeGkdRgCCwO2bOHPFBmtGxnX3fF24DXRP1lXemj5visbWPmih+54bZy67q6eg2vu+L8dx84DC1h69N1waA
xVs6/xaOc71KOSdFfeUkw3byOY4YIBautDCYe+usT20b/o6HippzKL951Ue8hvTcSmnnx4vNa/dUWX7d5f2+cPhSl5kpVyyKYj5vFt+1ehoayIegy6TrqGigbAF6YSvWiYxq
n8Gov/Ljrnnt542GAJL77B1gOpQ8vnrisOeuD5dpFz543VwmcY3w1PlgICm4nESEwGycxpQM+8rvL8xb8c1Ry2sG+fApQEfd7PtDxRHrV5NDcUi+tW1b/W4CLZOecyZbRlyK
koqbpArniFfa7A/MsCrlkAnjx8x0740zXtUXHolNHAuO6OI+l1KdaNBt21b/HZH4OElxjlAS8WAIMGsGxMFath8xWDbutKwMdw22d81rv268tfZ6Y0KcrufLvbZta94BxofB
eKfT4LmmEqNakWsnHDEwMynJIOoXOj7lluYVT415TNxhxIQggBqeH95cuGXNKVLShWw5J131euFK6EoME8ZMRAYAgSGOUEEmJxMlmBnQ3tSsF+4pfds+tvX9QOIQOwLPNGJM
KAKoIVfIyRm5GVw7Ref/72e9aa8ot8Lw3zHhrdJRr5Gego0NTKhhjbWUTPqoTfiod5UuVwtFbfVrApGUroL0FJgBXQrL1th7bRzlimcFz1avmfC6wIQkgBpqHbeHu0FzT9yQ
Vrv1PMT2HQxewIw3uhkvKxwJthYmNrCxARubRNPoufJtAAABtJ+eQEzE4FrNYbUGjVmQEERKQroSpCTAjHgwBDMeEwL3gvmHiHHnrfOWPXY41qWemNAEMIRhPfyfr2Dltq46
UTDeBIhmMGYR08mAPZGEmCJdBZLVwl7LSVCy9v/zzyYlvQCIAAhKBitahjUWNtIhGE8DeAyCfkaCelmKLUa8+hfFU/PR8OesZ6TucGByEMBwVIlh53Hbqac1eH55FlrYVy9/
QJ0AIV4thHiNNfYkIjoRzC9n4GVE1AAgDcBlVDkBkyFChZlLRLQbwJ8AftJa+h2k/bUrvd9wxX3ilvlX9D3/cXJckDu7t1NrMil0Uih+w/H/AY2LfH9ZyZC1AAAAAElFTkSu
QmCC
"""
}
