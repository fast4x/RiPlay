package it.fast4x.riplay.enums

enum class Languages {
    System,
    Afrikaans,
    Arabic,
    Azerbaijani,
    Bashkir,
    Basque,
    Bengali,
    Catalan,
    Danish,
    English,
    Esperanto,
    Estonian,
    ChineseSimplified,
    ChineseTraditional,
    Czech,
    Dutch,
    Filipino,
    Finnish,
    French,
    Galician,
    German,
    Greek,
    Hebrew,
    Hindi,
    Hungarian,
    Italian,
    Indonesian,
    Interlingua,
    Irish,
    Japanese,
    Korean,
    Malayalam,
    Norwegian,
    Odia,
    //Persian,
    Polish,
    PortugueseBrazilian,
    Portuguese,
    Romanian,
    //RomanianEmo,
    Russian,
    SerbianCyrillic,
    SerbianLatin,
    Sinhala,
    Spanish,
    Swedish,
    Tamil,
    Telugu,
    Turkish,
    Ukrainian,
    Vietnamese;

    val tag: String
        get() = when (this) {
            System -> "system"
            Afrikaans -> "af-ZA"
            Azerbaijani -> "az-AZ"
            Arabic -> "ar-AE" // Emirati Arabi Uniti o usa "ar-SA" per Arabia Saudita
            Bashkir -> "ba-RU"
            Basque -> "eu-ES"
            Bengali -> "bn-BD"
            Catalan -> "ca-ES"
            ChineseSimplified -> "zh-CN"
            ChineseTraditional -> "zh-TW"
            Danish -> "da-DK"
            Dutch -> "nl-NL"
            English -> "en-US"
            Esperanto -> "eo"    // L'Esperanto non ha una regione/paese associato, country rimarrà vuoto
            Estonian -> "et-EE"
            Filipino -> "fil-PH"
            Finnish -> "fi-FI"
            Galician -> "gl-ES"
            Italian -> "it-IT"
            Indonesian -> "in-ID" // Android usa internamente "in" anziché "id"
            Irish -> "ga-IE"
            Japanese -> "ja-JP"
            Korean -> "ko-KR"
            Czech -> "cs-CZ"
            German -> "de-DE"
            Greek -> "el-GR"
            Hebrew -> "iw-IL"    // Android usa internamente "iw" anziché "he"
            Hindi -> "hi-IN"
            Hungarian -> "hu-HU"
            Interlingua -> "ia"   // Lingua artificiale internazionale, non ha una regione
            Spanish -> "es-ES"
            French -> "fr-FR"
            Malayalam -> "ml-IN"
            Norwegian -> "no-NO"
            Odia -> "or-IN"
            Polish -> "pl-PL"
            Portuguese -> "pt-PT"
            PortugueseBrazilian -> "pt-BR"
            Romanian -> "ro-RO"
            Russian -> "ru-RU"
            SerbianCyrillic -> "sr-RS" // Codice ISO corretto per la Serbia
            SerbianLatin -> "sr-Latn-RS" // Corretto formato BCP-47 per il Serbo in caratteri Latini
            Sinhala -> "si-LK"
            Swedish -> "sv-SE"
            Tamil -> "ta-IN"
            Telugu -> "te-IN"
            Turkish -> "tr-TR"
            Ukrainian -> "uk-UA"
            Vietnamese -> "vi-VN"
        }

    val code: String
        get() = tag.substringBefore("-")


    companion object {
        fun languageFromcode(code: String): Languages? = when (code) {
            "system" -> System
            "af" -> Afrikaans
            "ar" -> Arabic
            "ba" -> Bashkir
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
            "iw" -> Hebrew //Hebrew -> "he"
            "hi" -> Hindi
            "hu" -> Hungarian
            "ia" -> Interlingua
            "es" -> Spanish
            "fr" -> French
            "ml" -> Malayalam
            "no" -> Norwegian
            "or" -> Odia
            //"fa" -> Persian
            "pl" -> Polish
            "pt" -> Portuguese
            "pt-BR" -> PortugueseBrazilian
            "ro" -> Romanian
            //"ro-RO" -> RomanianEmo
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
    XX, // Funge da Paese di sistema, se selezionato il country selezionato sarà quello di sistema
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

    val code: String
        get() = this.name

    val countryName: String
        get() = when (this) {
            XX -> "System"
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
            EE -> "Es->nia"
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
            PR -> "Puer-> Rico"
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