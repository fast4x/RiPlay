package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class DnsOverHttpsType {
    None,
    Google,
    CloudFlare,
    OpenDns,
    AdGuard,
    Custom;

    val type: String?
    get() = when (this) {
        None -> null
        Google -> "google"
        CloudFlare -> "cloudflare"
        OpenDns -> "opendns"
        AdGuard -> "adguard"
        Custom -> "custom"

    }

    val textName: String
        @Composable
        get() = when(this) {
            None -> stringResource(R.string.dns_none)
            Google -> stringResource(R.string.dns_google_public_dns)
            CloudFlare -> stringResource(R.string.dns_cloudflare_public_dns)
            OpenDns -> stringResource(R.string.dns_opendns_public_dns)
            AdGuard -> stringResource(R.string.dns_adguard_public_dns)
            Custom -> stringResource(R.string.dns_custom_dns)
        }
}