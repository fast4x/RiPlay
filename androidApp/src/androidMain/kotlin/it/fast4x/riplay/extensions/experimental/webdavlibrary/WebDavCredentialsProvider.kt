package it.fast4x.riplay.extensions.experimental.webdavlibrary

import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.utils.CryptoManager
import okhttp3.Credentials

// Un singleton o un oggetto in RAM per mantenere in memoria
// le credenziali decriptate per l'uso ad alte prestazioni
object WebDavCredentialsProvider {
    // Mappa: "https://mionas.com/remote.php/dav/files/user/" -> "Basic dXNlcjpwd2Q="
    private val credentialsMap = mutableMapOf<String, String>()

    fun updateCredentials(accounts: List<WebDavAccount>) {
        credentialsMap.clear()
        accounts.forEach { account ->
            // Decripta la password (possiamo farlo qui se questa lista viene aggiornata raramente)
            val rawPassword = CryptoManager.decrypt(account.encryptedPassword)
            val authHeader = Credentials.basic(account.username, rawPassword)
            // Usiamo il baseUrl come chiave
            credentialsMap[account.baseUrl.trimEnd('/')] = authHeader
        }
    }

    fun getAuthHeaderForUrl(url: String): String? {
        // Cerca se l'URL della richiesta inizia con uno dei baseUrl configurati
        for ((baseUrl, authHeader) in credentialsMap) {
            if (url.startsWith(baseUrl)) {
                return authHeader
            }
        }
        return null // Non è un URL WebDAV
    }
}

