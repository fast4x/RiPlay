package it.fast4x.riplay.extensions.rewind

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.rewind.data.SequentialAnimationContainer
import it.fast4x.riplay.extensions.rewind.slides.AlbumAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.ArtistAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.IntroSlide
import it.fast4x.riplay.extensions.rewind.slides.OutroSlideComposable
import it.fast4x.riplay.extensions.rewind.slides.PlaylistAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.SongAchievementSlide
import it.fast4x.riplay.extensions.rewind.slides.TopArtistSlide


@Composable
fun DynamicRewindSlide(slide: RewindSlide, isPageActive: Boolean) {
    SequentialAnimationContainer {
        when (slide) {
            is RewindSlide.IntroSlide -> IntroSlide(slide, isPageActive)
            is RewindSlide.TopArtist -> TopArtistSlide(slide, isPageActive)
            is RewindSlide.SongAchievement -> SongAchievementSlide(slide, isPageActive)
            is RewindSlide.AlbumAchievement -> AlbumAchievementSlide(slide, isPageActive)
            is RewindSlide.PlaylistAchievement -> PlaylistAchievementSlide(slide, isPageActive)
            is RewindSlide.ArtistAchievement -> ArtistAchievementSlide(slide, isPageActive)
            is RewindSlide.OutroSlide -> OutroSlideComposable(slide, isPageActive)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindScreen(pages: List<RewindSlide>) {
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
