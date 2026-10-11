package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class HomePageSection {
    Home,
    ForYou,
    Other;

    val textName: String
        @Composable
        get() = when(this) {
            Home -> stringResource(R.string.home_section_home)
            ForYou -> stringResource(R.string.home_section_for_you)
            Other -> stringResource(R.string.home_section_other)
        }
}

enum class HomePageMode {
    Online,
    Mixed;
    //Local;

    val textName: String
    @Composable
    get() = when(this) {
        Online -> stringResource(R.string.homepage_online)
        Mixed -> stringResource(R.string.homepage_mixed)
        //Local -> stringResource(R.string.homepage_local)
    }

    val summaryText: String
    @Composable
    get() = when(this) {
        Online -> stringResource(R.string.homepage_online_summary)
        Mixed -> stringResource(R.string.homepage_mixed_summary)
        //Local -> stringResource(R.string.homepage_local_summary)
    }

    val descriptionText: String
        @Composable
        get() = when(this) {
            Online -> stringResource(R.string.homepage_online_description)
            Mixed -> stringResource(R.string.homepage_mixed_description)
            //Local -> stringResource(R.string.homepage_local_description)
        }

}

