package it.fast4x.riplay.enums

/**
 * STEP 3s: enum ALLINEATA ALLA VERSIONE ANDROID
 * (androidApp/.../enums/Locales.kt: STESSA lista di lingue, STESSI tag).
 * Aggiunte Azerbaijani/Basque/Tamil (presenti su Android), Persian rimossa
 * (commentata anche su Android). [nativeName] = esonimo per il selettore
 * lingua della title bar desktop.
 */
enum class Languages(
    val tag: String,
    val nativeName: String
) {
    System("system", "System"),
    Afrikaans("af-ZA", "Afrikaans"),
    Arabic("ar-AE", "العربية"),
    Azerbaijani("az-AZ", "Azərbaycan"),
    Bashkir("ba-RU", "Башҡортса"),
    Basque("eu-ES", "Euskara"),
    Bengali("bn-BD", "বাংলা"),
    Catalan("ca-ES", "Català"),
    ChineseSimplified("zh-CN", "简体中文"),
    ChineseTraditional("zh-TW", "繁體中文"),
    Czech("cs-CZ", "Čeština"),
    Danish("da-DK", "Dansk"),
    Dutch("nl-NL", "Nederlands"),
    English("en-US", "English"),
    Esperanto("eo", "Esperanto"),
    Estonian("et-EE", "Eesti"),
    Filipino("fil-PH", "Filipino"),
    Finnish("fi-FI", "Suomi"),
    French("fr-FR", "Français"),
    Galician("gl-ES", "Galego"),
    German("de-DE", "Deutsch"),
    Greek("el-GR", "Ελληνικά"),
    Hebrew("iw-IL", "עברית"),
    Hindi("hi-IN", "हिन्दी"),
    Hungarian("hu-HU", "Magyar"),
    Italian("it-IT", "Italiano"),
    Indonesian("in-ID", "Bahasa Indonesia"),
    Interlingua("ia", "Interlingua"),
    Irish("ga-IE", "Gaeilge"),
    Japanese("ja-JP", "日本語"),
    Korean("ko-KR", "한국어"),
    Malayalam("ml-IN", "മലയാളം"),
    Norwegian("no-NO", "Norsk"),
    Odia("or-IN", "ଓଡ଼ିଆ"),
    Polish("pl-PL", "Polski"),
    PortugueseBrazilian("pt-BR", "Português (Brasil)"),
    Portuguese("pt-PT", "Português"),
    Romanian("ro-RO", "Română"),
    Russian("ru-RU", "Русский"),
    SerbianCyrillic("sr-RS", "Српски"),
    SerbianLatin("sr-Latn-RS", "Srpski (latinica)"),
    Sinhala("si-LK", "සිංහල"),
    Spanish("es-ES", "Español"),
    Swedish("sv-SE", "Svenska"),
    Tamil("ta-IN", "தமிழ்"),
    Telugu("te-IN", "తెలుగు"),
    Turkish("tr-TR", "Türkçe"),
    Ukrainian("uk-UA", "Українська"),
    Vietnamese("vi-VN", "Tiếng Việt");

    val code: String
        get() = tag.substringBefore("-")

    companion object {
        fun languageFromcode(code: String): Languages? = when (code) {
            "system" -> System
            "af" -> Afrikaans
            "ar" -> Arabic
            "az" -> Azerbaijani
            "ba" -> Bashkir
            "eu" -> Basque
            "bn" -> Bengali
            "ca" -> Catalan
            "zh-CN" -> ChineseSimplified
            "zh-TW" -> ChineseTraditional
            "da" -> Danish
            "nl" -> Dutch
            "en" -> English
            "eo" -> Esperanto
            "et" -> Estonian
            "fil" -> Filipino
            "fi" -> Finnish
            "gl" -> Galician
            "it" -> Italian
            "in" -> Indonesian
            "ga" -> Irish
            "ja" -> Japanese
            "ko" -> Korean
            "cs" -> Czech
            "de" -> German
            "el" -> Greek
            "iw" -> Hebrew
            "he" -> Hebrew
            "hi" -> Hindi
            "hu" -> Hungarian
            "ia" -> Interlingua
            "es" -> Spanish
            "fr" -> French
            "ml" -> Malayalam
            "no" -> Norwegian
            "or" -> Odia
            "pl" -> Polish
            "pt" -> Portuguese
            "pt-BR" -> PortugueseBrazilian
            "ro" -> Romanian
            "ru" -> Russian
            "sr" -> SerbianCyrillic
            "sr-CS" -> SerbianLatin
            "si" -> Sinhala
            "sv" -> Swedish
            "ta" -> Tamil
            "te" -> Telugu
            "tr" -> Turkish
            "uk" -> Ukrainian
            "vi" -> Vietnamese
            else -> null
        }
    }
}

enum class Countries {
    ZZ,
    AR,
    DZ,
    AU,
    AT,
    AZ,
    BH,
    BD,
    BY,
    BE,
    BO,
    BA,
    BR,
    BG,
    KH,
    CA,
    CL,
    HK,
    CO,
    CR,
    HR,
    CY,
    CZ,
    DK,
    DO,
    EC,
    EG,
    SV,
    EE,
    FI,
    FR,
    GE,
    DE,
    GH,
    GR,
    GT,
    HN,
    HU,
    IS,
    IN,
    ID,
    IQ,
    IE,
    IL,
    IT,
    JM,
    JP,
    JO,
    KZ,
    KE,
    KR,
    KW,
    LA,
    LV,
    LB,
    LY,
    LI,
    LT,
    LU,
    MK,
    MY,
    MT,
    MX,
    ME,
    MA,
    NP,
    NL,
    NZ,
    NI,
    NG,
    NO,
    OM,
    PK,
    PA,
    PG,
    PY,
    PE,
    PH,
    PL,
    PT,
    PR,
    QA,
    RO,
    RU,
    SA,
    SN,
    RS,
    SG,
    SK,
    SI,
    ZA,
    ES,
    LK,
    SE,
    CH,
    TW,
    TZ,
    TH,
    TN,
    TR,
    UG,
    UA,
    AE,
    GB,
    US,
    UY,
    VE,
    VN,
    YE,
    ZW;

    val countryName: String
        get() = when (this) {
            ZZ -> "Global"
            AR -> "Argentina"
            DZ -> "Algeria"
            AU -> "Australia"
            AT -> "Austria"
            AZ -> "Azerbaijan"
            BH -> "Bahrain"
            BD -> "Bangladesh"
            BY -> "Belarus"
            BE -> "Belgium"
            BO -> "Bolivia"
            BA -> "Bosnia and Herzegovina"
            BR -> "Brazil"
            BG -> "Bulgaria"
            KH -> "Cambodia"
            CA -> "Canada"
            CL -> "Chile"
            HK -> "Hong Kong"
            CO -> "Colombia"
            CR -> "Costa Rica"
            HR -> "Croatia"
            CY -> "Cyprus"
            CZ -> "Czech Republic"
            DK -> "Denmark"
            DO -> "Dominican Republic"
            EC -> "Ecuador"
            EG -> "Egypt"
            SV -> "El Salvador"
            EE -> "Estonia"
            FI -> "Finland"
            FR -> "France"
            GE -> "Georgia"
            DE -> "Germany"
            GH -> "Ghana"
            GR -> "Greece"
            GT -> "Guatemala"
            HN -> "Honduras"
            HU -> "Hungary"
            IS -> "Iceland"
            IN -> "India"
            ID -> "Indonesia"
            IQ -> "Iraq"
            IE -> "Ireland"
            IL -> "Israel"
            IT -> "Italy"
            JM -> "Jamaica"
            JP -> "Japan"
            JO -> "Jordan"
            KZ -> "Kazakhstan"
            KE -> "Kenya"
            KR -> "South Korea"
            KW -> "Kuwait"
            LA -> "Lao"
            LV -> "Latvia"
            LB -> "Lebanon"
            LY -> "Libya"
            LI -> "Liechtenstein"
            LT -> "Lithuania"
            LU -> "Luxembourg"
            MK -> "Macedonia"
            MY -> "Malaysia"
            MT -> "Malta"
            MX -> "Mexico"
            ME -> "Montenegro"
            MA -> "Morocco"
            NP -> "Nepal"
            NL -> "Netherlands"
            NZ -> "New Zealand"
            NI -> "Nicaragua"
            NG -> "Nigeria"
            NO -> "Norway"
            OM -> "Oman"
            PK -> "Pakistan"
            PA -> "Panama"
            PG -> "Papua New Guinea"
            PY -> "Paraguay"
            PE -> "Peru"
            PH -> "Philippines"
            PL -> "Poland"
            PT -> "Portugal"
            PR -> "Puerto Rico"
            QA -> "Qatar"
            RO -> "Romania"
            RU -> "Russian Federation"
            SA -> "Saudi Arabia"
            SN -> "Senegal"
            RS -> "Serbia"
            SG -> "Singapore"
            SK -> "Slovakia"
            SI -> "Slovenia"
            ZA -> "South Africa"
            ES -> "Spain"
            LK -> "Sri Lanka"
            SE -> "Sweden"
            CH -> "Switzerland"
            TW -> "Taiwan"
            TZ -> "Tanzania"
            TH -> "Thailand"
            TN -> "Tunisia"
            TR -> "Turkey"
            UG -> "Uganda"
            UA -> "Ukraine"
            AE -> "United Arab Emirates"
            GB -> "United Kingdom"
            US -> "United States"
            UY -> "Uruguay"
            VE -> "Venezuela (Bolivarian Republic)"
            VN -> "Vietnam"
            YE -> "Yemen"
            ZW -> "Zimbabwe"
        }
}
