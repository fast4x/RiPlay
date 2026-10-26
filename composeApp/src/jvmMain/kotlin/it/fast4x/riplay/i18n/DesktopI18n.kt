package it.fast4x.riplay.i18n

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import it.fast4x.riplay.enums.Languages
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * STEP 3t - LOCALIZZAZIONE DESKTOP da file XML formato ANDROID.
 *
 * Dalla v38 le stringhe UI desktop NON sono piu' in tabelle Kotlin: vivono in
 *   composeApp/src/jvmMain/resources/i18n/values/strings.xml            (INGLESE = fonte)
 *   composeApp/src/jvmMain/resources/i18n/values-<codice>/strings.xml   (47 traduzioni)
 * con la STESSA convenzione "values" dell'app Android (values-it, values-pt-rBR,
 * values-zh-rCN, values-in, values-iw, values-sr, values-sr-rCS, values-fil...):
 * i file sono cosi' gestibili dai servizi di traduzione (Crowdin/Lokalise/
 * Transifex) tramite il crowdin.yml alla radice del progetto. Vedere
 * resources/i18n/README.md.
 *
 * COME FUNZIONA:
 *  - ogni lingua = tabella inglese + override del file della lingua (le chiavi
 *    mancanti ricadono SEMPRE sull'inglese; ultima spiaggia la chiave grezza:
 *    non ci sono mai testi vuoti);
 *  - all'AVVIO (DesktopI18n.initialize, chiamata da main.kt) si legge
 *    ~/.riplay/app.properties (ui.language = nome dell'enum o "system"):
 *    se la scelta e' System (DEFAULT) o assente/corrotta, si rileva la
 *    lingua DEL SISTEMA (Locale.getDefault) e si mappa sull'enum
 *    (pt-BR, zh-CN/zh-TW, sr managed; match sul solo codice lingua;
 *    fallback inglese);
 *  - il SELETTORE nella title bar cambia lingua A RUNTIME:
 *    DesktopI18n.language e' uno snapshot state di Compose, quindi ogni
 *    Text passato per ds() si ricompone da solo; la scelta viene
 *    persistita subito nel file delle preferenze.
 *
 * ds() NON richiede @Composable: dentro una composizione la lettura dello
 * state si iscrive alla ricomposizione, fuori (es. funzioni helper) restituisce
 * solo il valore corrente. I segnaposto restano nel formato {nome}:
 * ds("page.albums", "n" to 12) -> "Albums (12)".
 */
object DesktopI18n {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("app.properties")
    private const val PREF_KEY = "ui.language"

    /** Lingua attiva (state Compose: la UI si ricompone al cambio). */
    val languageState: MutableState<Languages> = mutableStateOf(Languages.English)

    var language: Languages
        get() = languageState.value
        private set(value) { languageState.value = value }

    /** Chiamare UNA volta all'avvio (main.kt) PRIMA della prima UI. */
    fun initialize() {
        val saved = readSavedLanguage()
        language = when (saved) {
            null, Languages.System -> detectSystemLanguage()
            else -> saved
        }
    }

    /**
     * Scelta persistita: null = assente/corrotta (-> rileva il sistema),
     * Languages.System = "segui il sistema" ad ogni avvio.
     */
    private fun readSavedLanguage(): Languages? = try {
        val p = java.util.Properties()
        if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
        val raw = p.getProperty(PREF_KEY)?.trim().orEmpty()
        when {
            raw.isEmpty() -> null
            raw.equals("system", ignoreCase = true) -> Languages.System
            else -> Languages.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    } catch (t: Throwable) {
        null
    }

    /** Persiste la scelta (nome enum o "system"). */
    fun saveLanguageChoice(choice: Languages) {
        try {
            val p = java.util.Properties()
            if (prefsFile.isFile) prefsFile.inputStream().use { p.load(it) }
            p.setProperty(PREF_KEY, choice.name)
            prefsFile.parentFile?.mkdirs()
            prefsFile.outputStream().use { p.store(it, "RiPlay desktop preferences") }
        } catch (t: Throwable) {
            // la persistenza non deve mai rompere l'app: il cambio lingua
            // resta attivo per la sessione corrente
        }
    }

    /** Cambia lingua A RUNTIME (selettore title bar): state + persistenza. */
    fun setLanguageChoice(choice: Languages) {
        language = choice
        saveLanguageChoice(choice)
    }

    /**
     * Lingua DEL SISTEMA -> enum. Regole:
     *  - prima il match dei casi PARTICOLARI (pt-BR, zh, sr con script);
     *  - poi il solo codice lingua sull'enum;
     *  - fallback English (richiesta utente: l'inglese e' il default).
     */
    fun detectSystemLanguage(): Languages {
        val locale = Locale.getDefault()
        val tag = locale.toLanguageTag().replace('_', '-')
        val lang = locale.language.lowercase(Locale.ROOT)
        val country = locale.country.uppercase(Locale.ROOT)

        // match tag completo (con distinguo regionali significativi)
        when {
            lang == "pt" && country == "BR" -> return Languages.PortugueseBrazilian
            lang == "zh" -> return if (country == "TW" || country == "HK" || country == "MO")
                Languages.ChineseTraditional else Languages.ChineseSimplified
            lang == "sr" -> return if (tag.contains("latn", ignoreCase = true))
                Languages.SerbianLatin else Languages.SerbianCyrillic
        }

        Languages.entries.firstOrNull {
            it != Languages.System && it.code.equals(lang, ignoreCase = true)
        }?.let { return it }

        // alias particolari (Java usa "in" per l'indonesiano e "iw" per l'ebraico)
        return when (lang) {
            "in", "id" -> Languages.Indonesian
            "iw", "he" -> Languages.Hebrew
            "no", "nb", "nn" -> Languages.Norwegian
            "fil", "tl" -> Languages.Filipino
            else -> Languages.English
        }
    }

    /** Cache tabelle caricate (XML piccoli, 48 lingue: la prima richiesta paga il costo). */
    private val cache = ConcurrentHashMap<Languages, Map<String, String>>()

    /**
     * Tabella della lingua [l]: inglese (values/) + override della lingua
     * (values-<codice>/). Fallback e cache inclusi: mai mappe parziali al chiamante.
     */
    fun tableFor(l: Languages): Map<String, String> = when (l) {
        Languages.System -> englishTable()
        else -> cache.getOrPut(l) { mergeTable(l) }
    }

    private fun englishTable(): Map<String, String> =
        cache.getOrPut(Languages.English) { parseAndroidStringsXml(I18N_BASE) }

    private fun mergeTable(l: Languages): Map<String, String> {
        val base = englishTable()
        if (l == Languages.English) return base
        val folder = androidResFolder(l)
        val override = parseAndroidStringsXml("$I18N_DIR/values-$folder/strings.xml")
        return if (override.isEmpty()) base else base + override
    }

    /**
     * Codice cartella stile Android per ogni lingua (STESSA convenzione di
     * androidApp/src/main/res: zh-rCN, pt-rBR, iw, in, sr, sr-rCS, fil...).
     * L'inglese usa la cartella fonte senza suffisso (values/).
     */
    private fun androidResFolder(l: Languages): String = when (l) {
        Languages.ChineseSimplified -> "zh-rCN"
        Languages.ChineseTraditional -> "zh-rTW"
        Languages.PortugueseBrazilian -> "pt-rBR"
        Languages.Hebrew -> "iw"
        Languages.Indonesian -> "in"
        Languages.SerbianCyrillic -> "sr"
        Languages.SerbianLatin -> "sr-rCS"
        else -> l.code
    }

    private fun parseAndroidStringsXml(path: String): Map<String, String> = try {
        val stream = DesktopI18n::class.java.getResourceAsStream(path)
        if (stream == null) emptyMap()
        else stream.use { input ->
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            // hardening XXE: i file possono arrivare da servizi di traduzione esterni
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true) } catch (_: Throwable) {}
            try { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (_: Throwable) {}
            try { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Throwable) {}
            try { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) } catch (_: Throwable) {}
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false
            val doc = factory.newDocumentBuilder().parse(input)
            val out = HashMap<String, String>(128)
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as? org.w3c.dom.Element ?: continue
                val name = el.getAttribute("name").trim()
                if (name.isNotEmpty()) out[name] = androidUnescape(el.textContent)
            }
            out
        }
    } catch (t: Throwable) {
        // un file di lingua rotto non deve MAI rompere l'app: ricade sull'inglese
        emptyMap()
    }

    /**
     * Unescape delle convenzioni testuali Android dopo il parse XML:
     * \n, \t, \\, \' e \" tornano ai caratteri reali.
     */
    private fun androidUnescape(s: String): String {
        if (!s.contains('\\')) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '\'' -> { sb.append('\''); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    else -> { sb.append(c); i += 1 }
                }
            } else {
                sb.append(c); i += 1
            }
        }
        return sb.toString()
    }

    private const val I18N_DIR = "/i18n"
    private const val I18N_BASE = "/i18n/values/strings.xml"
}

/**
 * Stringa localizzata della UI desktop. Esempi:
 *   ds("action.add_to_queue")
 *   ds("page.albums", "n" to albums.size)
 * La lettura di DesktopI18n.language dentro una composizione rende la UI
 * reattiva al cambio lingua del selettore.
 */
fun ds(key: String, vararg args: Pair<String, Any?>): String {
    val lang = DesktopI18n.language
    var s = DesktopI18n.tableFor(lang)[key] ?: DesktopI18n.tableFor(Languages.English)[key] ?: key
    for ((k, v) in args) {
        if (v != null) s = s.replace("{$k}", v.toString())
    }
    return s
}
