package it.fast4x.riplay.extensions.audiotag

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import it.fast4x.audiotaginfo.models.Track
import it.fast4x.riplay.cleanString
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.audiotag.models.AudioTagInfoErrors
import it.fast4x.riplay.extensions.audiotag.models.UiState
import it.fast4x.riplay.ui.components.themed.DialogTextButton
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioTagger(viewModel: AudioTagViewModel, navController: NavController) {
    val uiState = viewModel.uiState.collectAsState().value
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(key1 = true) {
        if (!recordAudioPermission.status.isGranted) {
            recordAudioPermission.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            is UiState.Idle -> {
                if (recordAudioPermission.status.isGranted) {
                    Text("Tap to identify a song", fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = colorPalette().text,
                        textAlign = TextAlign.Center
                    )
                    DialogTextButton(
                        text = "Start Listening",
                        onClick = { viewModel.identifySong() },
                        primary = true
                    )
                } else {
                    Text("Microphone permission is required.", color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    DialogTextButton(
                        text = "Grant Permission",
                        onClick = { recordAudioPermission.launchPermissionRequest() },
                        primary = true
                    )
                }
            }
            is UiState.Recording -> {
                Text("Listening...", style = typography().xl, color = colorPalette().text)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            is UiState.Loading -> {
                Text("Identifying...", style = typography().xl, color = colorPalette().text)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            is UiState.Success -> {
                SongInfoCard(state.tracks, navController)
                Spacer(modifier = Modifier.height(24.dp))
                DialogTextButton(
                    text = "Identify another song",
                    onClick = { viewModel.identifySong() },
                    primary = true
                )
            }
            is UiState.Error -> {
                Text("Error", style = typography().xl, color = colorPalette().red)
                Text(AudioTagInfoErrors.getAudioTagInfoError(state.message).textName, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp), color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                DialogTextButton(
                    text = "Try again",
                    onClick = { viewModel.identifySong() },
                    primary = true
                )
            }
            is UiState.Response -> {
                Text("Info", style = typography().xl, color = colorPalette().text)
                Spacer(modifier = Modifier.height(16.dp))
                Text(state.message, fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                DialogTextButton(
                    text = "Identify another song",
                    onClick = { viewModel.identifySong() },
                    primary = true
                )
            }
        }
    }
}

@Composable
fun SongInfoCard(tracks: List<Track>?, navController: NavController) {
    tracks?.forEach { track ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("${NavRoutes.searchResults.name}/${cleanString(track.title)} ${cleanString(track.artist)}") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorPalette().background1),
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.title,
                    fontSize = typography().l.fontSize,
                    fontWeight = typography().xl.fontWeight,
                    textAlign = TextAlign.Center,
                    color = colorPalette().text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = track.artist,
                    fontSize = typography().l.fontSize,
                    color = colorPalette().text
                )
                track.album.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        fontSize = typography().m.fontSize,
                        color = colorPalette().text
                    )
                }
                track.year.let {
                    if (it != 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it.toString(),
                            fontSize = typography().m.fontSize,
                            color = colorPalette().text
                        )
                    }
                }
            }
        }
    }
}