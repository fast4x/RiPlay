package it.fast4x.riplay.ui.components.themed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.extensions.preferences.menuStyleKey
import it.fast4x.riplay.extensions.preferences.rememberPreference

@Composable
fun QueueItemMenu(
    modifier: Modifier = Modifier,
    navController: NavController,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val menuStyle by rememberPreference(
        menuStyleKey,
        MenuStyle.List
    )
    if (menuStyle == MenuStyle.Grid) {
        QueueItemMenuGrid(
            modifier = modifier,
            navController = navController,
            onDismiss = onDismiss,
            onEdit = onEdit,
            onRemove = onRemove,
        )
    } else {
        QueueItemMenuList(
            modifier = modifier,
            navController = navController,
            onDismiss = onDismiss,
            onEdit = onEdit,
            onRemove = onRemove,
        )

    }
}

@Composable
fun QueueItemMenuList(
    modifier: Modifier = Modifier,
    navController: NavController,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    Menu {
        MenuEntry(
            icon = R.drawable.title_edit,
            text = stringResource(R.string.update),
            onClick = {
                onEdit()
                onDismiss()
            }
        )
        MenuEntry(
            icon = R.drawable.close,
            text = stringResource(R.string.delete),
            onClick = {
                onRemove()
                onDismiss()
            }
        )

    }

}

@Composable
fun QueueItemMenuGrid(
    modifier: Modifier = Modifier,
    navController: NavController,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val colorPalette = colorPalette()

    GridMenu {
        GridMenuItem(
            icon = R.drawable.title_edit,
            title = R.string.update,
            colorIcon = colorPalette.text,
            colorText = colorPalette.text,
            onClick = {
                onEdit()
                onDismiss()
            }
        )
        GridMenuItem(
            icon = R.drawable.close,
            title = R.string.delete,
            colorIcon = colorPalette.text,
            colorText = colorPalette.text,
            onClick = {
                onRemove()
                onDismiss()
            }
        )
    }

}