package it.fast4x.riplay.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.applyIf
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.thumbnailShape
import it.fast4x.riplay.utils.typography


@Composable
fun BlacklistItem(
    modifier: Modifier = Modifier,
    thumbnailContent: @Composable (BoxScope.() -> Unit) = {},
    trailingContent: @Composable (() -> Unit)? = null,
    blacklistedItem: Blacklist,
    enabled: Boolean = true,
    onEnable: () -> Unit = {},
    onRemove: () -> Unit = {},
    onClick: () -> Unit = {}
) {

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var blacklistedEntity: Any? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        when (blacklistedItem.type) {
            BlacklistType.Song.name, BlacklistType.Video.name -> {
                Database.song(blacklistedItem.path).collect { blacklistedEntity = it }
            }
            BlacklistType.Artist.name -> {
                Database.artist(blacklistedItem.path).collect { blacklistedEntity = it }
            }

            BlacklistType.Album.name -> {
                Database.album(blacklistedItem.path).collect { blacklistedEntity = it }
            }

        }

    }

    val thumbnailSizeDp by remember { mutableStateOf(Dimensions.thumbnails.song) }


    ItemContainer(
        alternative = false,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(thumbnailSizeDp)
        ) {
            thumbnailContent()
            when (blacklistedItem.type) {
                BlacklistType.Folder.name, BlacklistType.Playlist.name -> {
                    Image(
                        painter = painterResource(if (blacklistedItem.type == BlacklistType.Folder.name) R.drawable.folder else R.drawable.music_library),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (enabled) colorPalette().text else colorPalette().textDisabled),
                        modifier = Modifier
                            .clip(thumbnailShape())
                            .fillMaxSize(.7f)
                            .align(Alignment.Center)
                    )
                }
                else -> {
                    AsyncImage(
                        model = when (blacklistedEntity) {
                            is Song -> (blacklistedEntity as Song).thumbnailUrl
                            is Album -> (blacklistedEntity as Album).thumbnailUrl
                            is Artist -> (blacklistedEntity as Artist).thumbnailUrl
                            else -> ""
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(thumbnailShape())
                            .fillMaxSize()
                    )
                }
            }
        }

        ItemInfoContainer {
            trailingContent?.let {}
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = cleanPrefix(blacklistedItem.name ?: stringResource(R.string.unknown_title)),
                    style = typography().xs.semiBold.copy(color = if (enabled) colorPalette().text else colorPalette().textDisabled),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                )


                Image(
                    painter = painterResource(if (enabled) R.drawable.eye else R.drawable.eye_off),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (enabled) colorPalette().text else colorPalette().textDisabled),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onEnable()
                        }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(R.drawable.trash),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette().red),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onRemove()
                        }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {

                BasicText(
                    text = blacklistedItem.path,
                    style = typography().xxxs.semiBold.copy(color = if (enabled) colorPalette().text else colorPalette().textDisabled),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .applyIf(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                )
            }
        }
    }
}