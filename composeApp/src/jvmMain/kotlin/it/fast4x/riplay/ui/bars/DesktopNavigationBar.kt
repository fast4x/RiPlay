package it.fast4x.riplay.ui.bars

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.ui.components.RiPlayAccent
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.album
import riplay.composeapp.generated.resources.artists
import riplay.composeapp.generated.resources.home
import riplay.composeapp.generated.resources.library
import riplay.composeapp.generated.resources.musical_notes
// STEP 3j: icona della nuova sezione RICERCA
import riplay.composeapp.generated.resources.search

/**
 * STEP 3b: navigazione desktop in stile Android.
 *
 * Riproduce la barra in basso dell'app Android (HorizontalNavigationBar +
 * HomeScreen navBarContent), stessa lista e stesso ordine:
 *   Item(0, "home",      R.drawable.home)
 *   Item(1, "songs",     R.drawable.musical_notes)
 *   Item(2, "artists",   R.drawable.music_artist)
 *   Item(3, "albums",    R.drawable.music_album)
 *   Item(4, "playlists", R.drawable.music_library)
 * con le icone corrispondenti gia' presenti in commonMain
 * (musical_notes / artists / album / library) + la nuova icona home.
 */
enum class DesktopSection(val label: String) {
    HOME("Home"),
    // STEP 3j: ricerca YouTube Music (come lato Android)
    SEARCH("Search"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    PLAYLISTS("Playlists")
}

@Composable
fun DesktopNavigationBar(
    currentSection: DesktopSection,
    onSectionSelected: (DesktopSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        HorizontalDivider(
            color = Color.DarkGray,
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.6f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Color.Black),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (section in DesktopSection.values()) {

                val icon = when (section) {
                    DesktopSection.HOME -> Res.drawable.home
                    DesktopSection.SEARCH -> Res.drawable.search
                    DesktopSection.SONGS -> Res.drawable.musical_notes
                    DesktopSection.ARTISTS -> Res.drawable.artists
                    DesktopSection.ALBUMS -> Res.drawable.album
                    DesktopSection.PLAYLISTS -> Res.drawable.library
                }

                // STEP 3h (migliorie visive): la voce attiva ha una PILL di
                // sfondo verde tenue + tinta accento piena (prima solo il
                // colore dell'icona cambiava); voci inattive bianco attenuato,
                // identico ad Android (text vs textDisabled).
                val active = section == currentSection
                val tint = if (active)
                    RiPlayAccent
                else
                    Color.White.copy(alpha = 0.4f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) RiPlayAccent.copy(alpha = 0.13f) else Color.Transparent)
                        .clickable { onSectionSelected(section) }
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    Image(
                        painter = painterResource(icon),
                        colorFilter = ColorFilter.tint(tint),
                        contentDescription = section.label,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = section.label,
                        color = tint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
