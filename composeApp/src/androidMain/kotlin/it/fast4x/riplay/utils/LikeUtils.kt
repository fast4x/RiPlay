package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.IconLikeType
import it.fast4x.riplay.extensions.preferences.iconLikeTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun getLikeState(mediaId: String): Int {
    var likedAt by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(mediaId) {
        Database.likedAt(mediaId).distinctUntilChanged().collect { likedAt = it }
    }

    return when (likedAt) {
        -1L -> getDislikedIcon()
        null -> getUnlikedIcon()
        else -> getLikedIcon()
    }

}

fun setLikeState(likedAt: Long?): Long? {
    val current =
     when (likedAt) {
        -1L -> null
        null -> System.currentTimeMillis()
        else -> -1L
    }
    //println("mediaItem setLikeState: $current")
    return current
}

fun setDisLikeState(likedAt: Long?): Long? {
    val current =
        when (likedAt) {
            -1L -> null
            null -> -1L
            else -> -1L
        }
    return current
}

@Composable
fun getLikedIcon(): Int {
    val iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)

    return when (iconLikeType) {
        IconLikeType.Essential -> R.drawable.heart
        IconLikeType.Gift -> R.drawable.heart_gift
        IconLikeType.Apple -> R.drawable.heart_apple
        IconLikeType.Brilliant -> R.drawable.heart_brilliant
        IconLikeType.Shape -> R.drawable.heart_shape
        IconLikeType.Breaked -> R.drawable.heart_breaked_no
        IconLikeType.Striped -> R.drawable.heart_striped
    }
}

@Composable
fun getUnlikedIcon(): Int {
    val iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)

    return when (iconLikeType) {
        IconLikeType.Essential -> R.drawable.heart_outline
        IconLikeType.Gift -> R.drawable.heart_gift_outline
        IconLikeType.Apple -> R.drawable.heart_apple_outline
        IconLikeType.Brilliant -> R.drawable.heart_brilliant_outline
        IconLikeType.Shape -> R.drawable.heart_shape_outline
        IconLikeType.Breaked -> R.drawable.heart_breaked_yes
        IconLikeType.Striped -> R.drawable.heart_striped_outline
    }
}

@Composable
fun getDislikedIcon(): Int {
    return R.drawable.heart_dislike
}