package it.fast4x.environment.models.responses

import it.fast4x.environment.models.AccountInfo
import it.fast4x.environment.models.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class AccountSwitcherEndpointResponse(
    val code: String?,
    val data: SwitcherData?,
)

@Serializable
data class SwitcherData(
    val responseContext: ResponseContext?,
    val selectText: TextRuns?,
    val actions: List<SwitcherAction?>?,
)

@Serializable
data class ResponseContext(
    val serviceTrackingParams: List<ServiceTrackingParam?>?,
    val responseId: String?,
)

@Serializable
data class ServiceTrackingParam(
    val service: String?,
    val params: List<TrackingParam?>?,
)

@Serializable
data class TrackingParam(
    val key: String?,
    val value: String?,
)

@Serializable
data class SwitcherAction(
    val getMultiPageMenuAction: GetMultiPageMenuAction?,
)

@Serializable
data class GetMultiPageMenuAction(
    val menu: Menu?,
)

@Serializable
data class Menu(
    val multiPageMenuRenderer: MultiPageMenuRenderer?,
)

@Serializable
data class MultiPageMenuRenderer(
    val header: MenuHeader?,
    val sections: List<MenuSection?>?,
    val footer: MenuFooter?,
    val style: String?,
)

@Serializable
data class MenuHeader(
    val simpleMenuHeaderRenderer: SimpleMenuHeaderRenderer?,
)

@Serializable
data class SimpleMenuHeaderRenderer(
    val backButton: BackButton?,
    val title: TextRuns?,
)

@Serializable
data class BackButton(
    val buttonRenderer: ButtonRenderer?,
)

@Serializable
data class ButtonRenderer(
    val style: String?,
    val size: String?,
    val isDisabled: Boolean?,
    val icon: IconType?,
    val accessibility: AccessibilityLabel?,
    val accessibilityData: AccessibilityData?,
)

@Serializable
data class AccessibilityData(
    val accessibilityData: AccessibilityLabel?,
)

@Serializable
data class AccessibilityLabel(
    val label: String?,
)

@Serializable
data class MenuSection(
    val accountSectionListRenderer: AccountSectionListRenderer?,
)

@Serializable
data class AccountSectionListRenderer(
    val contents: List<AccountSectionContent?>?,
    val header: AccountSectionHeader?,
)

@Serializable
data class AccountSectionContent(
    val accountItemSectionRenderer: AccountItemSectionRenderer?,
)

@Serializable
data class AccountItemSectionRenderer(
    val contents: List<AccountItemContent?>?,
)

@Serializable
data class AccountItemContent(
    val accountItem: AccountItem?,
)

@Serializable
data class AccountItem(
    val accountName: TextRuns?,
    val accountPhoto: PhotoThumbnails?,
    val isSelected: Boolean?,
    val isDisabled: Boolean?,
    val hasChannel: Boolean?,
    val serviceEndpoint: AccountServiceEndpoint?,
    val accountByline: TextRuns?,
    val channelHandle: TextRuns?,
    val accountLogDirectiveInts: List<Int?>?,
) {
    fun toAccountInfo(email: String): AccountInfo? {
        return AccountInfo(
            name = accountName?.runs?.firstOrNull()?.text ?: return null,
            email = email,
            pageId = serviceEndpoint?.selectActiveIdentityEndpoint?.supportedTokens
                ?.firstOrNull { it?.pageIdToken?.pageId != null }
                ?.pageIdToken?.pageId,
            channelHandle = channelHandle?.runs?.firstOrNull()?.text,
            thumbnailUrl = accountPhoto?.thumbnails?.firstOrNull()?.url?.substringBefore("=")
        )
    }
}

@Serializable
data class PhotoThumbnails(
    val thumbnails: List<Thumbnail?>?,
)

@Serializable
data class AccountServiceEndpoint(
    val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint?,
)

@Serializable
data class SelectActiveIdentityEndpoint(
    val supportedTokens: List<SupportedToken?>?,
)

@Serializable
data class SupportedToken(
    val accountStateToken: AccountStateToken?,
    val offlineCacheKeyToken: OfflineCacheKeyToken?,
    val accountSigninToken: AccountSigninToken?,
    val datasyncIdToken: DatasyncIdToken?,
    val pageIdToken: PageIdToken?,
)

@Serializable
data class AccountStateToken(
    val hasChannel: Boolean?,
    val isMerged: Boolean?,
    val obfuscatedGaiaId: String?,
)

@Serializable
data class OfflineCacheKeyToken(
    val clientCacheKey: String?,
)

@Serializable
data class AccountSigninToken(
    val signinUrl: String?,
)

@Serializable
data class DatasyncIdToken(
    val datasyncIdToken: String?,
)

@Serializable
data class PageIdToken(
    val pageId: String?,
)

@Serializable
data class AccountSectionHeader(
    val googleAccountHeaderRenderer: GoogleAccountHeaderRenderer?,
)

@Serializable
data class GoogleAccountHeaderRenderer(
    val name: TextRuns?,
    val email: TextRuns?,
)

@Serializable
data class MenuFooter(
    val multiPageMenuSectionRenderer: MenuFooterSectionRenderer?,
)

@Serializable
data class MenuFooterSectionRenderer(
    val items: List<FooterItem?>?,
)

@Serializable
data class FooterItem(
    val compactLinkRenderer: CompactLinkRenderer?,
)

@Serializable
data class CompactLinkRenderer(
    val icon: IconType?,
    val title: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?,
    val style: String?,
)

@Serializable
data class NavigationEndpoint(
    val urlEndpoint: UrlEndpoint?,
    val signOutEndpoint: SignOutEndpoint?,
)

@Serializable
data class UrlEndpoint(
    val url: String?,
)

@Serializable
data class SignOutEndpoint(
    val hack: Boolean?,
)

// Classi utilizzate più volte nel JSON per evitare duplicati e ridurre i nomi
@Serializable
data class TextRuns(
    val runs: List<TextRun?>?,
)

@Serializable
data class TextRun(
    val text: String?,
)

@Serializable
data class IconType(
    val iconType: String?,
)


fun AccountSwitcherEndpointResponse.toListAccountInfo(): List<AccountInfo> {
    if (this.code == "SUCCESS" && this.data != null) {
        val list = mutableListOf<AccountInfo>()

        // Estraiamo l'email dall'header, come da struttura JSON
        val accountEmail = this.data.actions
            ?.firstOrNull()
            ?.getMultiPageMenuAction
            ?.menu
            ?.multiPageMenuRenderer
            ?.sections
            ?.firstOrNull()
            ?.accountSectionListRenderer
            ?.header
            ?.googleAccountHeaderRenderer
            ?.email
            ?.runs
            ?.firstOrNull()
            ?.text ?: ""

        this.data.actions
            ?.firstOrNull()
            ?.getMultiPageMenuAction
            ?.menu
            ?.multiPageMenuRenderer
            ?.sections
            ?.firstOrNull()
            ?.accountSectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.accountItemSectionRenderer
            ?.contents
            ?.forEach { content ->
                content?.accountItem?.let { accountItem ->
                    accountItem.toAccountInfo(email = accountEmail)?.let {
                        list.add(it)
                    }
                }
            }
        return list
    } else {
        return emptyList()
    }
}

/**
 * Payload da inviare in POST a /youtubei/v1/account/select_active_identity
 */
@Serializable
data class SwitchAccountPayload(
    val pageIdToken: PageIdToken? = null,
    val accountStateToken: AccountStateToken? = null,
    val offlineCacheKeyToken: OfflineCacheKeyToken? = null,
    val datasyncIdToken: DatasyncIdToken? = null
)

/**
 * Estrae il payload per lo switch a partire dall'indice dell'account nella lista.
 * L'indice 0 corrisponde al primo profilo, 1 al secondo, ecc.
 */
fun AccountSwitcherEndpointResponse.toSwitchAccountPayload(accountIndex: Int): SwitchAccountPayload? {
    if (this.code != "SUCCESS" || this.data == null) return null

    // Naviga l'albero per trovare l'account item corretto in base all'indice
    val accountItem = this.data.actions
        ?.firstOrNull()
        ?.getMultiPageMenuAction
        ?.menu
        ?.multiPageMenuRenderer
        ?.sections
        ?.firstOrNull()
        ?.accountSectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.accountItemSectionRenderer
        ?.contents
        ?.getOrNull(accountIndex)
        ?.accountItem
        ?: return null

    // Prende la lista dei token supportati per questo account
    val supportedTokens = accountItem.serviceEndpoint
        ?.selectActiveIdentityEndpoint
        ?.supportedTokens
        ?: return null

    // Mappa i token della risposta nel payload di richiesta
    return SwitchAccountPayload(
        pageIdToken = supportedTokens.firstOrNull { it?.pageIdToken != null }?.pageIdToken,
        accountStateToken = supportedTokens.firstOrNull { it?.accountStateToken != null }?.accountStateToken,
        offlineCacheKeyToken = supportedTokens.firstOrNull { it?.offlineCacheKeyToken != null }?.offlineCacheKeyToken,
        datasyncIdToken = supportedTokens.firstOrNull { it?.datasyncIdToken != null }?.datasyncIdToken
    )
}

@Serializable
data class CachedAccountProfile(
    val name: String?,
    val email: String?,
    val channelHandle: String?,
    val thumbnailUrl: String?,
    val isSelected: Boolean?,
    val pageId: String?,
    val authUser: String?,
    val signinUrl: String?
)

fun AccountSwitcherEndpointResponse.toCachedProfiles(): List<CachedAccountProfile> {
    if (this.code != "SUCCESS" || this.data == null) return emptyList()

    val accountEmail = this.data.actions
        ?.firstOrNull()
        ?.getMultiPageMenuAction
        ?.menu
        ?.multiPageMenuRenderer
        ?.sections
        ?.firstOrNull()
        ?.accountSectionListRenderer
        ?.header
        ?.googleAccountHeaderRenderer
        ?.email
        ?.runs
        ?.firstOrNull()
        ?.text ?: ""

    return this.data.actions
        ?.firstOrNull()
        ?.getMultiPageMenuAction
        ?.menu
        ?.multiPageMenuRenderer
        ?.sections
        ?.firstOrNull()
        ?.accountSectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.accountItemSectionRenderer
        ?.contents
        ?.mapNotNull { content ->
            val item = content?.accountItem ?: return@mapNotNull null

            val supportedTokens = item.serviceEndpoint
                ?.selectActiveIdentityEndpoint
                ?.supportedTokens ?: emptyList()

            // Estrae il PageId
            val pageId = supportedTokens.firstOrNull { it?.pageIdToken != null }
                ?.pageIdToken?.pageId

            val signinUrl = supportedTokens.firstOrNull { it?.accountSigninToken != null }
                ?.accountSigninToken?.signinUrl

            // Estrae l'authUser dall'URL del signin (es. "authuser=0" -> 0)
            val authUser = signinUrl
                ?.substringAfter("authuser=")
                ?.substringBefore("&")

            CachedAccountProfile(
                name = item.accountName?.runs?.firstOrNull()?.text,
                email = accountEmail,
                channelHandle = item.channelHandle?.runs?.firstOrNull()?.text,
                thumbnailUrl = item.accountPhoto?.thumbnails?.firstOrNull()?.url?.substringBefore("="),
                isSelected = item.isSelected,
                pageId = pageId,
                authUser = authUser,
                signinUrl = signinUrl
            )
        } ?: emptyList()
}