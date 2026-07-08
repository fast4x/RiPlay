package it.fast4x.riplay.enums

enum class ExportType {
    Csv,
    M38u;

    val ext: String
        get() = when(this) {
            Csv -> ".csv"
            M38u -> ".m38u"
        }

    val mimeExport: String
        get() = when(this) {
            Csv -> "text/csv"
            M38u -> "audio/x-mpegurl"
        }

    val mimeImport: Array<String>
        get() = when(this) {
            Csv -> arrayOf("text/csv")
            M38u -> arrayOf(
                "audio/x-mpegurl",               // Standard M3U/M3U8
                "application/vnd.apple.mpegurl", // HLS puro
                "audio/mpegurl"                  // Variante senza la 'x'
            )
        }
}