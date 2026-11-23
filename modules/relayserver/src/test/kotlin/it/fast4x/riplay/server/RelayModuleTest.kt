package it.fast4x.riplay.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit test del modulo relayserver (puri JVM, senza rete): logica di
 * whitelist/coda, URI servibili, Range RFC 7233, M3U, MIME e token.
 * Le verifiche E2E HTTP (bind, Range su socket, redirect) sono state
 * eseguite durante la migrazione; qui si copre la logica deterministica
 * che non richiede porte/socket, cosi' `gradle :relayserver:test` resta
 * affidabile su qualunque macchina.
 */
class RelayModuleTest {

    // ─────────────────── RelayRegistry ───────────────────

    @Test
    fun `register crea id stabile dalla uri`() {
        val registry = RelayRegistry()
        val track = registry.register(RelayRegistry.Candidate(uri = "/musica/brano.mp3", title = "Brano"))
        assertEquals(RelayUris.streamId("/musica/brano.mp3"), track.id)
        assertEquals(1, registry.size())
        assertTrue(registry.track(track.id) != null)
    }

    @Test
    fun `syncQueue aggiorna ordine e non svuota la whitelist`() {
        val registry = RelayRegistry()
        val primo = registry.register(RelayRegistry.Candidate(uri = "/a.mp3", title = "A"))
        // la whitelist non viene svuotata dalla syncQueue
        registry.syncQueue(
            listOf(
                RelayRegistry.Candidate(uri = "/b.mp3", title = "B"),
                RelayRegistry.Candidate(uri = "/c.mp3", title = "C"),
            )
        )
        assertEquals(3, registry.size())
        assertTrue(registry.track(primo.id) != null)
        // ordine coda = nuovo
        assertEquals(listOf("/b.mp3", "/c.mp3"), registry.queue().map { it.uri })
    }

    @Test
    fun `evict rispetta maxTracks`() {
        val registry = RelayRegistry()
        registry.maxTracks = 2
        registry.register(RelayRegistry.Candidate(uri = "/1.mp3"))
        registry.register(RelayRegistry.Candidate(uri = "/2.mp3"))
        registry.register(RelayRegistry.Candidate(uri = "/3.mp3"))
        assertEquals(2, registry.size())
        // il piu' vecchio (per inserimento) e' stato rimosso
        assertNull(registry.track(RelayUris.streamId("/1.mp3")))
    }

    // ─────────────────── RelayUris ───────────────────

    @Test
    fun `isDirect riconosce solo http e https`() {
        assertTrue(RelayUris.isDirect("http://host/x.mp3"))
        assertTrue(RelayUris.isDirect("https://host/x.mp3"))
        assertFalse(RelayUris.isDirect("file:///x.mp3"))
        assertFalse(RelayUris.isDirect("/percorso/x.mp3"))
        assertFalse(RelayUris.isDirect("videoid"))
    }

    @Test
    fun `isLocal riconosce file content e percorsi desktop`() {
        assertTrue(RelayUris.isLocal("file:///storage/music/x.mp3"))
        assertTrue(RelayUris.isLocal("content://media/external/audio/media/1"))
        assertTrue(RelayUris.isLocal("/home/user/musica/x.mp3"))       // POSIX
        assertTrue(RelayUris.isLocal("C:\\Musica\\x.mp3"))             // Windows backslash
        assertTrue(RelayUris.isLocal("C:/Musica/x.mp3"))               // Windows slash
        assertFalse(RelayUris.isLocal("https://cdn/x.mp3"))            // diretto, non locale
        assertFalse(RelayUris.isLocal("videoid123"))                   // videoId (Fase 3)
        assertFalse(RelayUris.isLocal(""))                             // vuoto
    }

    @Test
    fun `streamId e' stabile e deterministico`() {
        assertEquals(RelayUris.streamId("/x.mp3"), RelayUris.streamId("/x.mp3"))
        assertEquals(20, RelayUris.streamId("/x.mp3").length)
        assertFalse(RelayUris.streamId("/x.mp3") == RelayUris.streamId("/y.mp3"))
    }

    // ─────────────────── RelayM3u ───────────────────

    @Test
    fun `m3u con link relay per locali e url originale per dirette`() {
        val base = "http://192.168.1.10:8765/relay/token"
        val locale = RelayTrack(id = "idLocale", uri = "/musica/brano.mp3", title = "Brano", artist = "Artista")
        val diretta = RelayTrack(id = "idDiretta", uri = "https://stream/radio", title = "Radio")
        val fase3 = RelayTrack(id = "idVideo", uri = "videoid", title = "YouTube")
        val m3u = RelayM3u.build(playlistName = "RiPlay", tracks = listOf(locale, diretta, fase3)) { track ->
            when {
                RelayUris.isDirect(track.uri) -> track.uri
                RelayUris.isLocal(track.uri) -> RelayUris.streamUrl(base, track)
                else -> null
            }
        }
        assertTrue(m3u.startsWith("#EXTM3U"))
        assertTrue(m3u.contains("#PLAYLIST:RiPlay"))
        assertTrue(m3u.contains("#EXTINF:-1,Artista - Brano"))
        assertTrue(m3u.contains("$base/s/idLocale"))
        assertTrue(m3u.contains("https://stream/radio"))
        assertFalse(m3u.contains("videoid"))
    }

    // ─────────────────── HttpRanges ───────────────────

    @Test
    fun `range start-end`() {
        assertEquals(HttpRange(0, 99, 1000), HttpRanges.parse("bytes=0-99", 1000))
        assertEquals(HttpRange(100, 199, 1000), HttpRanges.parse("bytes=100-199", 1000))
    }

    @Test
    fun `range aperto a destra arriva a fine risorsa`() {
        assertEquals(HttpRange(500, 999, 1000), HttpRanges.parse("bytes=500-", 1000))
    }

    @Test
    fun `range suffix ultimi N byte`() {
        assertEquals(HttpRange(800, 999, 1000), HttpRanges.parse("bytes=-200", 1000))
        assertEquals(HttpRange(0, 999, 1000), HttpRanges.parse("bytes=-5000", 1000))
    }

    @Test
    fun `range invalido o oltre fine ritorna null`() {
        assertNull(HttpRanges.parse(null, 1000))
        assertNull(HttpRanges.parse("", 1000))
        assertNull(HttpRanges.parse("bytes=", 1000))
        assertNull(HttpRanges.parse("bytes=abc-", 1000))
        assertNull(HttpRanges.parse("bytes=2000-", 1000))   // oltre fine: 416 lato endpoint
        assertNull(HttpRanges.parse("bytes=0-", 0))         // risorsa vuota
        assertNull(HttpRanges.parse("bytes=5-2", 1000))     // end < start
    }

    // ─────────────────── RelayMime ───────────────────

    @Test
    fun `mime dalla tabella interna anche senza percorso`() {
        // CORREZIONE della migrazione: nome file "nudo" (senza slash) deve
        // comunque produrre l'estensione (la versione Android lo perdeva)
        assertEquals("audio/mpeg", RelayMime.fromFileName("canzone.mp3"))
        assertEquals("audio/flac", RelayMime.fromFileName("/musica/brano.flac"))
        assertEquals("audio/ogg", RelayMime.fromFileName("C:\\musica\\traccia.ogg"))
        assertEquals("audio/mp4", RelayMime.fromFileName("s.m4a"))
        assertEquals("audio/x-ms-wma", RelayMime.fromFileName("s.wma"))
    }

    @Test
    fun `mime null per nomi senza estensione o vuoti`() {
        assertNull(RelayMime.fromFileName(null))
        assertNull(RelayMime.fromFileName(""))
        assertNull(RelayMime.fromFileName("noperiodo"))
        assertNull(RelayMime.fromFileName("."))
    }

    @Test
    fun `extensionResolver di piattaforma ha precedenza sulla tabella`() {
        val original = RelayMime.extensionResolver
        try {
            RelayMime.extensionResolver = { ext -> if (ext == "custom") "audio/custom" else null }
            assertEquals("audio/custom", RelayMime.fromFileName("a.custom"))
            // resolver null per l'estensione -> cade nella tabella interna
            assertEquals("audio/mpeg", RelayMime.fromFileName("a.mp3"))
        } finally {
            RelayMime.extensionResolver = original
        }
    }

    // ─────────────────── Token e RelayLog ───────────────────

    @Test
    fun `token store in memoria (comportamento loadOrCreate)`() {
        val store = InMemoryTokenStore()
        val server1 = RelayServer(RelayRegistry(), nullRelaySource(), store, preferredPort = 1)
        // il server non avviato genera comunque il token al costruttore
        assertTrue(server1.token.isNotBlank())
        assertEquals(server1.token, store.saved)
        // una seconda istanza con lo stesso store riusa il token (link M3U stabili)
        val server2 = RelayServer(RelayRegistry(), nullRelaySource(), store, preferredPort = 1)
        assertEquals(server1.token, server2.token)
    }

    @Test
    fun `logging non lancia con sink di default`() {
        RelayLog.i("Test", "messaggio %s", "formattato")
        RelayLog.w(Throwable("boom"), "Test", "warning con errore")
        RelayLog.e("Test", "errore")
    }
}

/**
 * Sorgente "mai servibile" (probe/open sempre null): la risposta del server
 * e' sempre 404, utile per istanze di test senza file reali.
 */
private fun nullRelaySource(): RelayStreamSource =
    object : RelayStreamSource {
        override fun probe(track: RelayTrack): RelayMeta? = null
        override fun open(track: RelayTrack, range: HttpRange?): java.io.InputStream? = null
    }

private class InMemoryTokenStore : RelayTokenStore {
    var saved: String? = null
    override fun load(): String? = saved
    override fun save(token: String) { saved = token }
}
