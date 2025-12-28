package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class HomeSection {
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