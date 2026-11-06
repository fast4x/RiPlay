package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.ui.screens.settings.SettingsEntryGroupText
import it.fast4x.riplay.ui.screens.settings.SettingsGroupSpacer


/*
 * Item "legacy": una voce in una sua card arrotondata.
 * Mantenuto per retrocompatibilita', il nuovo layout delle impostazioni
 * usa settingsGroupHeader + settingsGroupCard.
 */
fun LazyListScope.settingsItem(
    isHeader: Boolean = false,
    content: @Composable () -> Unit
) {
    if (isHeader)
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorPalette().background0)
            ) {
                content()
            }
        }
    else
        item {
            Column( modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .background(colorPalette().background1, shape = getRoundnessShape())
                .fillMaxWidth()

            ) {
                content()
            }
        }
}

fun LazyListScope.settingsSearchBarItem(
    content: @Composable (ColumnScope.() -> Unit)
) {
    item {
        Column (
            modifier = Modifier
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

/*
 * Intestazione di tab: titolo grande con icona, senza card.
 */
fun LazyListScope.settingsHeaderItem(
    content: @Composable LazyItemScope.() -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

/*
 * Titolo di gruppo "sticky": resta visibile durante lo scroll della tab
 * e indica a quale card appartengono le voci sottostanti.
 * [icon] (opzionale) mostra una piccola icona accanto al titolo.
 */
fun LazyListScope.settingsGroupHeader(
    title: String,
    icon: Int? = null
) {
    stickyHeader {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = title, icon = icon)
        }
    }
}

/*
 * Card di gruppo (stile inset-grouped): tutte le voci passate in content
 * condividono una singola superficie arrotondata; usare SettingsGroupDivider
 * tra le voci per separare con una linea sottile.
 */
fun LazyListScope.settingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .background(
                    colorPalette().background2,
                    shape = getRoundnessShape()
                )
                .border(
                    1.dp,
                    colorPalette().text.copy(alpha = 0.07f),
                    getRoundnessShape()
                )
        ) {
            content()
        }
    }
}

/*
 * Card di gruppo con titolo, piu' compatta di settingsGroupHeader+Card:
 * il titolo scorre insieme alla card (non sticky).
 */
fun LazyListScope.settingsGroupCardWithTitle(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    item {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsEntryGroupText(title = title)
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .background(
                        colorPalette().background2,
                        shape = getRoundnessShape()
                    )
                    .border(
                        1.dp,
                        colorPalette().text.copy(alpha = 0.07f),
                        getRoundnessShape()
                    )
            ) {
                content()
            }
        }
    }
}
