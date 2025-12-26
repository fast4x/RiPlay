package it.fast4x.riplay.extensions.rewind

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.rewind.data.SequentialAnimationContainer
import it.fast4x.riplay.extensions.rewind.data.getRewindSlides
import it.fast4x.riplay.extensions.rewind.slides.AlbumAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.AnnualListenerSlide
import it.fast4x.riplay.extensions.rewind.slides.ArtistAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.IntermediateSlide
import it.fast4x.riplay.extensions.rewind.slides.IntroSlide
import it.fast4x.riplay.extensions.rewind.slides.OutroSlideComposable
import it.fast4x.riplay.extensions.rewind.slides.PlaylistAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.SongAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.TopAlbumsSlide
import it.fast4x.riplay.extensions.rewind.slides.TopArtistsSlide
import it.fast4x.riplay.extensions.rewind.slides.TopPlaylistsSlide
import it.fast4x.riplay.extensions.rewind.slides.TopSongsSlide
import it.fast4x.riplay.extensions.rewind.utils.getRewindYears
import it.fast4x.riplay.extensions.rewind.utils.shadersList
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.RewindItem
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import java.util.Calendar
import kotlin.random.Random


@Composable
fun DynamicRewindSlide(slide: RewindSlide, isPageActive: Boolean) {
    SequentialAnimationContainer(year = slide.year) {
        //VisualBitmapCreator(modifier = Modifier.fillMaxSize()) {
            when (slide) {
                is RewindSlide.IntroSlide -> IntroSlide(slide, isPageActive)
                is RewindSlide.TopSongs -> TopSongsSlide(slide, isPageActive)
                is RewindSlide.TopAlbums -> TopAlbumsSlide(slide, isPageActive)
                is RewindSlide.TopArtists -> TopArtistsSlide(slide, isPageActive)
                is RewindSlide.TopPlaylists -> TopPlaylistsSlide(slide, isPageActive)
                is RewindSlide.SongAchievement -> SongAchievementSlide(slide, isPageActive)
                is RewindSlide.AlbumAchievement -> AlbumAchievementSlide(slide, isPageActive)
                is RewindSlide.PlaylistAchievement -> PlaylistAchievementSlide(slide, isPageActive)
                is RewindSlide.ArtistAchievement -> ArtistAchievementSlide(slide, isPageActive)
                is RewindSlide.OutroSlide -> OutroSlideComposable(slide, isPageActive)
                is RewindSlide.Intermediate -> IntermediateSlide(slide, isPageActive)
                is RewindSlide.AnnualListener -> AnnualListenerSlide(slide, isPageActive)
            }
        //}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindScreen(year: Int? = null) {

    val pages = getRewindSlides(year)

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize()) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val isPageActive = pagerState.currentPage == pageIndex
            DynamicRewindSlide(
                slide = pages[pageIndex],
                isPageActive = isPageActive
            )
        }


        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}


@Composable
fun HomepageRewind(
    showIfEndOfYear: Boolean = false,
    navController: NavController,
    playlistThumbnailSizeDp: Dp,
    endPaddingValues: PaddingValues,
    disableScrollingText: Boolean
) {
    if (showIfEndOfYear && Calendar.getInstance().get(Calendar.MONTH) < 11) return

    getRewindYears().let { years ->
        if (years.isEmpty()) return@let

        Title(
            title = if (showIfEndOfYear) stringResource(R.string.rw_watch_your_rewind) else stringResource(R.string.rewinds),
            onClick = { navController.navigate(NavRoutes.rewind.name) },
        )

        LazyRow(contentPadding = endPaddingValues) {
            items(
                items = years,
                key = { it }
            ) { year ->
                RewindItem(
                    name = year.toString(),
                    showName = false,
                    thumbnailSizeDp = playlistThumbnailSizeDp,
                    thumbnailContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shaderBackground(
                                    shadersList()[Random.nextInt(
                                        0,
                                        shadersList().size - 1
                                    )]
                                )
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
        }

    }
}