package it.fast4x.riplay.extensions.games.pacman.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

data class EnemyMovementModel (
    val redEnemyMovement: MutableState<Offset> = mutableStateOf(Offset(0F, 0F)),
    val orangeEnemyMovement: MutableState<Offset> = mutableStateOf(Offset(0F, 0F))
)