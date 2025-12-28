package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class ContentType {
    All,
    Official,
    UserGenerated;

    val textName: String
        @Composable
        get() = when( this ) {
            All -> stringResource(R.string.content_type_all)
            Official -> stringResource(R.string.content_type_official)
            UserGenerated -> stringResource(R.string.content_type_user_generated)
        }

    val icon: Int
        @Composable
        get() = when( this ) {
            All -> R.drawable.internet
            Official -> R.drawable.star
            UserGenerated -> R.drawable.person
        }

}