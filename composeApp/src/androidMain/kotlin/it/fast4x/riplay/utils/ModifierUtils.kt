package it.fast4x.riplay.utils

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import it.fast4x.riplay.extensions.preferences.disablePlayerHorizontalSwipeKey
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference

@Composable
fun Modifier.detectGestures(
    detectPlayerGestures: Boolean = false,
    onSwipeToLeft: () -> Unit? = {},
    onSwipeToRight: () -> Unit? = {},
    onTap: () -> Unit? = {},
    onDoubleTap: () -> Unit? = {},
    onPress: () -> Unit? = {},
    onLongPress: () -> Unit? = {},
): Modifier {
    val disablePlayerHorizontalSwipe by rememberObservedPreference(disablePlayerHorizontalSwipeKey, false)
    var deltaX by remember { mutableStateOf(0f) }
    return this
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    deltaX = dragAmount
                },
                onDragStart = {
                },
                onDragEnd = {
                    if (!disablePlayerHorizontalSwipe && detectPlayerGestures) {
                        if (deltaX > 5) {
                            onSwipeToRight() // Previous
                        } else if (deltaX < -5) {
                            onSwipeToLeft() // Next
                        }
                    } else {
                        if (deltaX > 5) {
                            onSwipeToRight() // Previous
                        } else if (deltaX < -5) {
                            onSwipeToLeft() // Next
                        }
                    }

                }

            )
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    onTap()
                },
                onDoubleTap = {
                    onDoubleTap()
                },
                onPress = {
                    onPress()
                },
                onLongPress = {
                    onLongPress()
                }
            )
        }
}