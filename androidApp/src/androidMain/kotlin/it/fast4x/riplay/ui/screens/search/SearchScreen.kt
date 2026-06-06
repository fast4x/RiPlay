package it.fast4x.riplay.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.PersistMapCleanup
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.SearchQuery
import it.fast4x.riplay.enums.ContentType
import it.fast4x.riplay.enums.TransitionEffect
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.PreferenceKey.ENABLE_VOICE_INPUT
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.TRANSITION_EFFECT
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.StartVoiceInput
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography
import kotlinx.serialization.ExperimentalSerializationApi

@UnstableApi
@OptIn(ExperimentalSerializationApi::class, ExperimentalMaterialApi::class, ExperimentalTextApi::class,
    ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val transitionEffect by rememberPreference(TRANSITION_EFFECT.key, TransitionEffect.SlideHorizontal)
    val saveableStateHolder = rememberSaveableStateHolder()

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    PersistMapCleanup(tagPrefix = "search/")

    var submittedQuery by rememberSaveable { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current


    val isEnabledVoiceInput by rememberPreference(ENABLE_VOICE_INPUT.key, true)
    var startVoiceInput by remember { mutableStateOf(false) }

    if (startVoiceInput) {
        StartVoiceInput(
            onTextRecognized = {
                onTextFieldValueChanged(TextFieldValue(it, selection = TextRange(it.length)))
                submittedQuery = it
                startVoiceInput = false
            },
            onRecognitionError = { startVoiceInput = false },
            onListening = {}
        )
    }

    var filterContentType by remember { mutableStateOf(ContentType.All) }

    val isSearchActive = submittedQuery.isNotEmpty()

    val (baseTabIndex, onBaseTabChanged) = rememberSaveable { mutableStateOf(0) }
    val (resultTabIndex, onResultTabChanged) = rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.length > 3) {
            if (!context.preferences.getBoolean(PreferenceKey.PAUSE_SEARCH_HISTORY.key, false)) {
                Database.asyncTransaction {
                    insert(SearchQuery(query = textFieldValue.text))
                }
            }
        }
    }

    val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(all = 10.dp)
                .fillMaxWidth()
        ) {


            IconButton(
                onClick = {},
                icon = R.drawable.search,
                color = colorPalette().text,
                modifier = Modifier.size(20.dp)
            )


            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.padding(horizontal = 10.dp).weight(1f)
            ) {
                if (textFieldValue.text.isEmpty())
                    BasicText(
                        text = stringResource(R.string.search),
                        maxLines = 1,
                        style = typography().l.secondary
                    )

                innerTextField()
            }
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 5.dp)
                ) {
                    if (isEnabledVoiceInput) {
                        IconButton(
                            onClick = { startVoiceInput = true },
                            icon = R.drawable.mic,
                            color = colorPalette().text,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onTextFieldValueChanged(TextFieldValue(""))
                                submittedQuery = ""
                                focusRequester.requestFocus()
                            },
                            icon = R.drawable.close,
                            color = colorPalette().text,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

        ScreenContainer(
            navController = navController,
            tabIndex = if (isSearchActive) resultTabIndex else baseTabIndex,
            onTabChanged = { newIndex ->
                if (isSearchActive) onResultTabChanged(newIndex) else onBaseTabChanged(newIndex)
            },
            miniPlayer = miniPlayer,
            transitionEffect = transitionEffect,
            navBarContent = { item ->
                if (isSearchActive) {
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes, true)
                    item(1, stringResource(R.string.albums), R.drawable.music_album, true)
                    item(2, stringResource(R.string.artists), R.drawable.music_artist, true)
                    item(3, stringResource(R.string.videos), R.drawable.video, true)
                    item(4, stringResource(R.string.playlists), R.drawable.playlist, true)
                    item(5, stringResource(R.string.featured), R.drawable.featured_playlist, true)
                    item(6, stringResource(R.string.podcasts), R.drawable.podcast, true)
                } else {
                    item(0, stringResource(R.string.online), R.drawable.internet, true)
                    item(1, stringResource(R.string.library), R.drawable.playlist, true)
                    item(2, stringResource(R.string.go_to_link), R.drawable.link, true)
                }
            }
        ) { currentTabIndex ->

            Column(modifier = Modifier.fillMaxSize()) {
                if (currentTabIndex < 2)
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newTextFieldValue ->
                            onTextFieldValueChanged(newTextFieldValue)

                            if (newTextFieldValue.text.isEmpty()) {
                                submittedQuery = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .background(
                                colorPalette().background4,
                                shape = getRoundnessShape()
                            )
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (!it.hasFocus) {
                                    keyboardController?.hide()
                                }
                            },
                        singleLine = true,
                        textStyle = typography().l.medium.align(TextAlign.Start),
                        cursorBrush = SolidColor(colorPalette().text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (textFieldValue.text.isNotEmpty()) {
                                    submittedQuery = textFieldValue.text
                                    keyboardController?.hide()

                                    if (!context.preferences.getBoolean(PreferenceKey.PAUSE_SEARCH_HISTORY.key, false)) {
                                        Database.asyncTransaction {
                                            insert(SearchQuery(query = textFieldValue.text))
                                        }
                                    }
                                }
                            }
                        ),
                        decorationBox = decorationBox
                    )

                Box(modifier = Modifier.weight(1f)) {
                    saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                        if (isSearchActive) {
                            SearchResultsContent(
                                query = submittedQuery,
                                tabIndex = currentTabIndex,
                                filterContentType = filterContentType,
                                onFilterChanged = { filterContentType = it },
                                navController = navController,
                                onSaveHistory = {
                                    if (!context.preferences.getBoolean(
                                            PreferenceKey.PAUSE_SEARCH_HISTORY.key,
                                            false
                                        )
                                    ) {
                                        Database.asyncTransaction { insert(SearchQuery(query = textFieldValue.text)) }
                                    }
                                },
                                focusRequester = focusRequester,
                                keyboardController = keyboardController
                            )
                        } else {
                            when (currentTabIndex) {
                                0 -> OnlineSearch(
                                    navController = navController,
                                    textFieldValue = textFieldValue,
                                    onTextFieldValueChanged = onTextFieldValueChanged,
                                    onSearch = { query ->
                                        submittedQuery = query
                                        keyboardController?.hide()
                                    },
                                    decorationBox = decorationBox
                                )

                                1 -> LocalSongSearch(
                                    navController = navController,
                                    textFieldValue = textFieldValue,
                                    onTextFieldValueChanged = onTextFieldValueChanged,
                                    decorationBox = decorationBox,
                                    onAction1 = { onBaseTabChanged(0) },
                                    onAction2 = { onBaseTabChanged(1) },
                                    onAction3 = { onBaseTabChanged(2) },
                                    onAction4 = {}
                                )

                                2 -> GoToLink(
                                    navController = navController,
                                    textFieldValue = textFieldValue,
                                    onTextFieldValueChanged = onTextFieldValueChanged,
                                    decorationBox = decorationBox,
                                    onAction1 = { onBaseTabChanged(0) },
                                    onAction2 = { onBaseTabChanged(1) },
                                    onAction3 = { onBaseTabChanged(2) },
                                    onAction4 = {}
                                )
                            }
                        }
                    }
                }
        }
    }
}