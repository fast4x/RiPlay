package it.fast4x.riplay.enums

enum class ExportType {
    CSV,
    M3U8;

    val ext: String
        get() = when(this) {
            CSV -> ".csv"
            M3U8 -> ".m3u8"
        }

    val mimeExport: String
        get() = when(this) {
            CSV -> "text/csv"
            M3U8 -> "audio/x-mpegurl"
        }

    val mimeImport: Array<String>
        get() = when(this) {
            CSV -> arrayOf("text/csv")
            M3U8 -> arrayOf(
                "audio/x-mpegurl",               // Standard M3U/M3U8
                "application/vnd.apple.mpegurl", // HLS puro
                "audio/mpegurl"                  // Variante senza la 'x'
            )
        }
}