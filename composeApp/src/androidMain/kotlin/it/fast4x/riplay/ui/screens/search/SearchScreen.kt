package it.fast4x.riplay.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.PersistMapCleanup
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.UiType
import it.fast4x.riplay.extensions.preferences.enableVoiceInputKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.styling.favoritesIcon
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.StartVoiceInput
import it.fast4x.riplay.utils.typography

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabChanged) = rememberSaveable {
        mutableStateOf(0)
    }

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    PersistMapCleanup(tagPrefix = "search/")

    val isEnabledVoiceInput by rememberPreference(enableVoiceInputKey, true)

    var startVoiceInput by remember { mutableStateOf(false) }

    if (startVoiceInput) {
        StartVoiceInput(
            onTextRecognized = {
                onTextFieldValueChanged(TextFieldValue(it))
                startVoiceInput = false
            },
            onRecognitionError = {
                startVoiceInput = false
            },
            onListening = {}
        )
    }

    val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
               // .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            if ( UiType.ViMusic.isCurrent() )
                Column (
                    verticalArrangement = Arrangement.Center
                ) {
                   // if (applyFontPadding)
                       Spacer(modifier = Modifier.padding(top = 5.dp))

                    IconButton(
                        onClick = {},
                        icon = R.drawable.search,
                        color = colorPalette().favoritesIcon,
                        modifier = Modifier
                            //.align(Alignment.CenterStart)
                            .size(20.dp)
                    )
                }
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
               // .weight(1f)
                .padding(horizontal = 40.dp)
        ) {
            AnimatedVisibility(
                visible = textFieldValue.text.isEmpty(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                BasicText(
                    text = stringResource(R.string.search), //stringResource(R.string.enter_a_name),
                    maxLines = 1,
                    style = typography().l.secondary,

                )
            }
            innerTextField()
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .padding(horizontal = 10.dp)

        ) {
            Row (
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 5.dp)
                    //.width(80.dp)
            ) {

                if (isEnabledVoiceInput) {
                    IconButton(
                        onClick = { startVoiceInput = true },
                        icon = R.drawable.mic,
                        color = colorPalette().favoritesIcon,
                        modifier = Modifier
                            .size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))
                }

                IconButton(
                    onClick = { onTextFieldValueChanged(TextFieldValue("")) },
                    icon = R.drawable.close,
                    color = colorPalette().favoritesIcon,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }

    ScreenContainer(
        navController,
        tabIndex,
        onTabChanged,
        miniPlayer,
        navBarContent = { item ->
            item(0, stringResource(R.string.online), R.drawable.internet)
            item(1, stringResource(R.string.library), R.drawable.playlist)
            item(2, stringResource(R.string.go_to_link), R.drawable.link)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(currentTabIndex) {
            when (currentTabIndex) {
                0 -> OnlineSearch(
                    navController = navController,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChanged = onTextFieldValueChanged,
                    onSearch = onSearch,
                    decorationBox = decorationBox
                )

                1 -> LocalSongSearch(
                    navController = navController,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChanged = onTextFieldValueChanged,
                    decorationBox = decorationBox,
                    onAction1 = { onTabChanged(0) },
                    onAction2 = { onTabChanged(1) },
                    onAction3 = { onTabChanged(2) },
                    onAction4 = {
                        //onGoToHome()
                        //pop()
                    },
                )

                2 -> GoToLink(
                    navController = navController,
                    textFieldValue = textFieldValue,
                    onTextFieldValueChanged = onTextFieldValueChanged,
                    decorationBox = decorationBox,
                    onAction1 = { onTabChanged(0) },
                    onAction2 = { onTabChanged(1) },
                    onAction3 = { onTabChanged(2) },
                    onAction4 = {
                        //onGoToHome()
                        //pop()
                    },
                )
            }
        }
    }
}
