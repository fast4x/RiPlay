package it.fast4x.riplay.ui.screens.player.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext

@Composable
fun PlayerScreen() {

    val codaDiEsempio = listOf(
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            titolo = "SoundHelix Song 1",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            titolo = "SoundHelix Song 2",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            titolo = "SoundHelix Song 3",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            titolo = "SoundHelix Song 4",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            titolo = "SoundHelix Song 5",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            titolo = "SoundHelix Song 6",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            titolo = "SoundHelix Song 7",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            titolo = "SoundHelix Song 8",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            titolo = "SoundHelix Song 9",
            artista = "SoundHelix"
        ),
        MediaItemGenericoImpl(
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            titolo = "SoundHelix Song 10",
            artista = "SoundHelix"
        )
    )

    val factory = PlayerCodaViewModelFactory(codaDiEsempio)

    val playerViewModel: PlayerCodaViewModel = viewModel(factory = factory)

    val exoPlayerWrapper = ExoPlayerWrapper(appContext(), playerViewModel)
    playerViewModel.associaController(exoPlayerWrapper)

    PlayerCodaScreen(playerViewModel = playerViewModel)
}

data class MediaItemGenericoImpl(
    override val uri: String,
    override val titolo: String?,
    override val artista: String?
) : MediaItemGenerico