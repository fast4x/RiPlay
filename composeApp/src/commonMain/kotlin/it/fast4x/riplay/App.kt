package it.fast4x.riplay

import androidx.compose.runtime.Composable
import database.MusicDatabaseDao

@Composable
fun App(db: MusicDatabaseDao) {
/*
    DesktopTheme {
        DesktopApp()
    }
*/

    /*
    MaterialTheme {
        var relatedPage = remember { mutableStateOf<Innertube.RelatedPage?>(null) }
        LaunchedEffect(Unit) {
            Innertube.relatedPage(
                NextBody(
                    videoId = ("HZnNt9nnEhw")
                )
            )?.onSuccess {
                relatedPage.value = it
            }
        }

        var showContent by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                    Text(stringResource(Res.string.tips))
                    BasicText(text = stringResource(Res.string.restart_app_please))
                    BasicText(text = "Songs: ${relatedPage.value?.songs?.size.toString()}")
                    BasicText(text = "commonDao db song ${db.getAll()}")
                }
            }

        }
    }
     */
}