package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import it.fast4x.riplay.R

enum class ContentType {
    All,
    Official,
    UserGenerated;

    val textName: String
        @Composable
        get() = when( this ) {
            All -> "All"
            Official -> "Official"
            UserGenerated -> "User Generated"
        }

    val icon: Int
        @Composable
        get() = when( this ) {
            All -> R.drawable.internet
            Official -> R.drawable.star
            UserGenerated -> R.drawable.person
        }

}