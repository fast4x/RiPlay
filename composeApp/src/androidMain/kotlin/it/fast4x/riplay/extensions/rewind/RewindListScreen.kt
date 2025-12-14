package it.fast4x.riplay.extensions.rewind

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.mikepenz.hypnoticcanvas.shaderBackground
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.Environment
import it.fast4x.environment.requests.discoverPage
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.navigationBarPositionKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showSearchTabKey
import it.fast4x.riplay.extensions.rewind.utils.getRewindYears
import it.fast4x.riplay.extensions.rewind.utils.shadersList
import it.fast4x.riplay.ui.components.themed.LoaderScreen
import it.fast4x.riplay.ui.items.RewindItem
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.LazyListContainer
import kotlin.random.Random

@ExperimentalTextApi
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RewindListScreen(
    navController: NavController
) {

    val thumbnailSizeDp = Dimensions.thumbnails.album + 24.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    val navigationBarPosition by rememberPreference(
        navigationBarPositionKey,
        NavigationBarPosition.Bottom
    )

    val showSearchTab by rememberPreference(showSearchTabKey, false)

    val lazyGridState = rememberLazyGridState()

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)


    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
            //.padding(WindowInsets.navigationBars.asPaddingValues().calculateTopPadding())
    ) {

        /***************/
        getRewindYears(10).let { years ->
            LazyListContainer(
                state = lazyGridState,
            ) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(Dimensions.thumbnails.album + 24.dp),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier = Modifier
                        .background(colorPalette().background0)
                    //.fillMaxSize()
                ) {
                    item(
                        key = "header",
                        contentType = 0,
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        HeaderWithIcon(
                            title = "Rewinds",
                            iconId = R.drawable.stat_year,
                            enabled = true,
                            showIcon = !showSearchTab,
                            modifier = Modifier,
                            onClick = {}
                        )

                    }

                    items(
                        items = years,
                        key = { it }
                    ) { year ->
                        RewindItem(
                            name = year.toString(),
                            showName = false,
                            thumbnailSizeDp = thumbnailSizeDp,
                            thumbnailContent = {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                        .shaderBackground(shadersList()[Random.nextInt(0, shadersList().size-1)])
                                ) {
                                    Text(
                                        text = year.toString(),
                                        color = colorPalette().text,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 60.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            },
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null
                                )
                                .fillMaxSize()
                                .clickable(
                                    onClick = { navController.navigate(route = "${NavRoutes.rewind.name}/${year}") }
                                ),
                            disableScrollingText = disableScrollingText,
                            alternative = true,
                        )
                    }
                    item(
                        key = "footer",
                        contentType = 0,
                    ) {
                        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                    }
                }
            }

        }
        /***************/


    }

}
