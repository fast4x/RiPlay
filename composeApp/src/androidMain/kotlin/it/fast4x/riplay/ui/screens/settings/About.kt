package it.fast4x.riplay.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.extensions.contributors.ShowDevelopers
import it.fast4x.riplay.extensions.contributors.ShowTranslators
import it.fast4x.riplay.extensions.contributors.countDevelopers
import it.fast4x.riplay.extensions.contributors.countTranslators
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.getVersionName
import it.fast4x.riplay.utils.secondary
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.typography


@ExperimentalAnimationApi
@Composable
fun About() {
    val uriHandler = LocalUriHandler.current
    //val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
            .verticalScroll(rememberScrollState())
            /*
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )

             */
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.about),
            iconId = R.drawable.information,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
        ) {
            BasicText(
                text = "RiPlay v${getVersionName()} by fast4x",
                style = typography().s.secondary,

                )
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.social))

        SettingsEntry(
            online = false,
            offline = false,
            title = stringResource(R.string.social_telegram),
            text = stringResource(R.string.social_telegram_info),
            onClick = {
                uriHandler.openUri("https://t.me/riplay_app")
            }
        )

        SettingsEntry(
            online = false,
            offline = false,
            title = stringResource(R.string.social_reddit),
            text = stringResource(R.string.social_reddit_info),
            onClick = {
                uriHandler.openUri("https://www.reddit.com/r/RiMusicApp/")
            }
        )

//        SettingsEntry(
//            title = stringResource(R.string.social_discord),
//            text = stringResource(R.string.social_discord_info),
//            onClick = {
//                uriHandler.openUri("https://discord.gg/h7eS2PcGeW")
//            }
//        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "Official Project Page")

        SettingsEntry(
            online = false,
            offline = false,
            title = stringResource(R.string.social_github),
            text = stringResource(R.string.view_the_source_code),
            onClick = {
                uriHandler.openUri("https://github.com/fast4x/RiPlay")
            }
        )



        SettingsGroupSpacer()

        //todo enable issues
//        SettingsEntryGroupText(title = stringResource(R.string.troubleshooting))
//
//        SettingsEntry(
//            title = stringResource(R.string.report_an_issue),
//            text = stringResource(R.string.you_will_be_redirected_to_github),
//            onClick = {
//                uriHandler.openUri("https://github.com/fast4x/RiPlay/issues/new?assignees=&labels=bug&template=bug_report.yaml")
//            }
//        )
//
//
//        SettingsEntry(
//            title = stringResource(R.string.request_a_feature_or_suggest_an_idea),
//            text = stringResource(R.string.you_will_be_redirected_to_github),
//            onClick = {
//                uriHandler.openUri("https://github.com/fast4x/RiPlay/issues/new?assignees=&labels=feature_request&template=feature_request.yaml")
//            }
//        )

//        SettingsGroupSpacer()
//
//        Title(
//            title = stringResource(R.string.contributors)
//        )
//
//        SettingsEntryGroupText(title = "${ countTranslators() } " + stringResource(R.string.translators))
//        SettingsDescription(text = stringResource(R.string.in_alphabetical_order))
//        ShowTranslators()
//
//        SettingsGroupSpacer()
//
//        SettingsEntryGroupText(title = "${ countDevelopers() } " + "Developers / Designers")
//        SettingsDescription(text = stringResource(R.string.in_alphabetical_order))
//        ShowDevelopers()

        SettingsGroupSpacer(
            modifier = Modifier.height(Dimensions.bottomSpacer)
        )
    }
}
