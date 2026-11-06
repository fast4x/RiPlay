package it.fast4x.riplay.ui.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.enums.ValidationType
import it.fast4x.riplay.ui.components.ScreenContainer
import it.fast4x.riplay.ui.components.themed.DialogColorPicker
import it.fast4x.riplay.ui.components.themed.IDialog
import it.fast4x.riplay.ui.components.themed.InputTextDialog
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.SelectorWebDavAccountDialog
import it.fast4x.riplay.ui.components.themed.Slider
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.components.themed.StringListDialog
import it.fast4x.riplay.ui.components.themed.Switch
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.components.themed.ValueSelectorDialog
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SettingsScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {

    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabChanged) = rememberSaveable {
        mutableStateOf(0)
    }

    val transitionEffect = appSettings.transitionEffect

    val showMiniPlayerInSettings = appSettings.showMiniPlayerInSettings

    ScreenContainer(
        navController,
        tabIndex,
        onTabChanged,
        if (showMiniPlayerInSettings) miniPlayer else null,
        transitionEffect = transitionEffect,
        navBarContent = { item ->
            item(0, stringResource(R.string.tab_general), R.drawable.app_icon, true)
            item(1, stringResource(R.string.ui_tab), R.drawable.ui, true)
            item(2, stringResource(R.string.player_appearance), R.drawable.color_palette, true)
            item(3, stringResource(R.string.tab_widgets), R.drawable.grid_view, true)
            item(4, if (!isYtLoggedIn()) stringResource(R.string.home, true)
            else stringResource(R.string.home), if (!isYtLoggedIn()) R.drawable.sparkles
            else R.drawable.home, true)
            item(5, stringResource(R.string.tab_data), R.drawable.server, true)
            item(6, stringResource(R.string.tab_accounts), R.drawable.person, true)
            item(7, stringResource(R.string.tab_miscellaneous), R.drawable.equalizer, true)
            item(8, stringResource(R.string.about), R.drawable.information, true)

        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(currentTabIndex) {
            when (currentTabIndex) {
                0 -> GeneralSettings(navController = navController)
                1 -> UiSettings(navController = navController)
                2 -> PlayerAppearanceSettings(navController = navController)
                3 -> WidgetsSettings(navController = navController)
                4 -> HomeSettings(navController = navController)
                5 -> DataSettings(navController = navController)
                6 -> AccountsSettings(navController = navController)
                7 -> MiscSettings(navController = navController)
                8 -> About(navController = navController)

            }
        }
    }
}

/*
 * Ricerca nelle impostazioni
 * -------------------------------------------------------------------------
 * Il filtro si applica a titoli e descrizioni di ogni voce e ai gruppi che
 * le contengono, senza duplicare la logica di confronto nei punti di
 * chiamata. Non sono @Composable: si usano anche a livello di LazyListScope
 * per nascondere i gruppi senza risultati.
 */

fun Search.matchesAny(@StringRes vararg stringIds: Int): Boolean {
    if (input.isBlank()) return true
    return stringIds.any { context.getString(it).contains(input, ignoreCase = true) }
}

fun Search.matches(text: String): Boolean {
    if (input.isBlank()) return true
    return text.contains(input, ignoreCase = true)
}

/*
 * Anatomia unificata di una voce di impostazione
 * -------------------------------------------------------------------------
 *  [ icona ]  Titolo voce                          Valore selezionato  ›
 *             descrizione di supporto
 *
 *  - SwitchSettingEntry  -> switch a destra, nessun chevron
 *  - *SelectorEntry      -> valore corrente in accent + chevron a destra
 *  - SliderSettingsEntry -> valore a destra + slider inline
 *  - ButtonBar/Text/Color-> azione diretta, icona a sinistra
 * La vecchia "pillola" con chevron in basso è stata rimossa: il valore
 * vive sulla riga, come nelle liste di preferenze moderne.
 */

@Composable
fun SettingsEntry(
    modifier: Modifier = Modifier,
    title: String,
    titleSecondary: String? = null,
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    icon: Int? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    valueText: String? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isEnabled) 1f else 0.5f)
    ) {
        if (showDivider) SettingsGroupDivider()

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {

            if (leadingContent != null) {
                leadingContent()
            } else if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(colorPalette().textSecondary),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .weight(1f)
            ) {
                BasicText(
                    text = title,
                    style = typography().s.semiBold.copy(
                        color = if (isEnabled) colorPalette().text else colorPalette().textDisabled
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                )

                if (!titleSecondary.isNullOrBlank()) {
                    BasicText(
                        text = titleSecondary,
                        style = typography().xxs.secondary.copy(color = colorPalette().textSecondary),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (text.isNotBlank()) {
                    BasicText(
                        text = text,
                        style = typography().xxs.secondary.copy(color = colorPalette().textSecondary),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!valueText.isNullOrBlank()) {
                BasicText(
                    text = valueText,
                    style = typography().xs.semiBold.copy(color = colorPalette().accent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 140.dp)
                )
            }

            if (showChevron) {
                Image(
                    painter = painterResource(R.drawable.chevron_forward),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(colorPalette().textSecondary),
                )
            }

            trailingContent?.invoke()

        }
    }
}

/*
 * Separatore interno a una card di gruppo.
 * Allineato al testo delle voci (dopo icona + gap), come negli
 * elenchi di preferenze inset.
 */
@Composable
fun SettingsGroupDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = 50.dp, end = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(colorPalette().text.copy(alpha = 0.07f))
    )
}

/*
 * Contenitore card per liste non-Lazy (About, Data, Accounts...):
 * stessa superficie delle card di gruppo delle tab LazyColumn.
 */
@Composable
fun SettingsCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
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

@Composable
inline fun <reified T : Enum<T>> EnumValueSelectorSettingsEntry(
    title: String,
    titleSecondary: String? = null,
    text: String? = null,
    selectedValue: T,
    noinline onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    noinline valueText: @Composable (T) -> String  = { it.name },
    noinline trailingContent: (@Composable () -> Unit) = {},
    icon: Int? = null,
) {
    ValueSelectorSettingsEntry(
        title = title,
        titleSecondary = titleSecondary,
        text = text,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        onValueSelected = onValueSelected,
        modifier = modifier,
        isEnabled = isEnabled,
        valueText = valueText,
        trailingContent = trailingContent,
        icon = icon,
    )
}

@Composable
fun ValueWebDavAccountSelectorSettingsEntry(
    modifier: Modifier = Modifier,
    title: String,
    titleSecondary: String? = null,
    text: String? = null,
    values: List<WebDavAccount>,
    onValueSelected: (WebDavAccount) -> Unit,
    isEnabled: Boolean = true,
    trailingContent: (@Composable () -> Unit) = {},
    onNewAccount: () -> Unit = {},
    onDismiss: () -> Unit = {},
    icon: Int? = null,
) {
    var isShowingDialog by remember {
        mutableStateOf(false)
    }
    var accountSelected by remember { mutableStateOf(values.firstOrNull()) }

    if (isShowingDialog) {
        SelectorWebDavAccountDialog(
            onDismiss = {
                isShowingDialog = false
                onDismiss()
            },
            title = title,
            values = values,
            onValueSelected = {
                onValueSelected(it)
                accountSelected = it
            },
            onNewAccount = onNewAccount
        )
    }

    SettingsEntry(
        title = title,
        titleSecondary = titleSecondary,
        text = text.orEmpty(),
        modifier = modifier,
        isEnabled = isEnabled,
        onClick = { isShowingDialog = true },
        trailingContent = trailingContent,
        icon = icon,
        valueText = accountSelected?.name,
        showChevron = true,
    )
}

@Composable
fun <T> ValueSelectorSettingsEntry(
    title: String,
    titleSecondary: String? = null,
    text: String? = null,
    selectedValue: T,
    values: List<T>,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    valueText: @Composable (T) -> String = { it?.toString() ?: "---" },
    trailingContent: (@Composable () -> Unit) = {},
    icon: Int? = null,
) {
    var isShowingDialog by remember {
        mutableStateOf(false)
    }

    if (isShowingDialog) {
        ValueSelectorDialog(
            onDismiss = { isShowingDialog = false },
            title = title,
            selectedValue = selectedValue,
            values = values,
            onValueSelected = onValueSelected,
            valueText = valueText
        )
    }

    SettingsEntry(
        title = title,
        titleSecondary = titleSecondary,
        text = text.orEmpty(),
        modifier = modifier,
        isEnabled = isEnabled,
        onClick = { isShowingDialog = true },
        trailingContent = trailingContent,
        icon = icon,
        valueText = valueText(selectedValue),
        showChevron = true,
    )
}

@Composable
fun SwitchSettingEntry(
    title: String,
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    icon: Int? = null,
) {
    SettingsEntry(
        title = title,
        titleSecondary = text.ifBlank { null },
        text = "",
        isEnabled = isEnabled,
        onClick = { onCheckedChange(!isChecked) },
        trailingContent = { Switch(isChecked = isChecked) },
        icon = icon,
        modifier = modifier,
    )
}

/*
 * Titolo, descrizione e contenuto di un gruppo di impostazioni.
 * Le voci passate in [content] vengono disposte dentro una card
 * arrotondata (stile inset-grouped) con divisori sottili inseribili
 * tramite [SettingsGroupDivider].
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    description: String? = null,
    important: Boolean = false,
    color: Color = colorPalette().accent,
    uppercase: Boolean = true,
    visibleWhen: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visibleWhen) return

    Column(modifier = modifier) {
        if (title != null) {
            SettingsEntryGroupText(title = title, color = color, uppercase = uppercase)
        }
        description?.let { description ->
            SettingsDescription(
                text = description,
                important = important
            )
        }
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
        SettingsGroupSpacer()
    }
}

@Composable
fun SettingsEntryGroup(
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(all = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                content()
            }

        }
    }
}

@Composable
fun SettingsTopDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        style = typography().xs.secondary,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsDescription(
    text: String,
    modifier: Modifier = Modifier,
    important: Boolean = false,
) {
    BasicText(
        text = text,
        style = if (important) typography().xxs.semiBold.color(colorPalette().red)
        else typography().xxs.secondary,
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp)
            .padding(bottom = 8.dp)
    )
}

@Composable
fun ImportantSettingsDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        style = typography().xxs.semiBold.color(colorPalette().red),
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsEntryGroupText(
    title: String,
    color: Color = colorPalette().accent,
    uppercase: Boolean = true,
    modifier: Modifier = Modifier,
    icon: Int? = null,
) {
    if (icon == null) {
        Title(title = title)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette().accent),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(18.dp)
            )
            Title(
                title = title,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SettingsGroupSpacer(
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .height(24.dp)
    )
}

@Composable
fun TextDialogSettingEntry(
    title: String,
    text: String,
    currentText: String,
    onTextSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    validationType: ValidationType = ValidationType.None,
    icon: Int? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        InputTextDialog(
            onDismiss = { showDialog = false },
            title = title,
            value = currentText,
            placeholder = title,
            setValue = {
                onTextSave(it)
            },
            validationType = validationType,
            setValueRequireNotNull = validationType != ValidationType.None

        )
    }
    SettingsEntry(
        title = title,
        titleSecondary = text.ifBlank { null },
        text = "",
        isEnabled = isEnabled,
        onClick = { showDialog = true },
        icon = icon,
        valueText = currentText.takeIf { it.isNotBlank() },
        showChevron = true,
        modifier = modifier,
    )
}

@Composable
fun ColorSettingEntry(
    title: String,
    text: String,
    color: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    icon: Int? = null,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsEntry(
        title = title,
        titleSecondary = text.ifBlank { null },
        text = "",
        isEnabled = isEnabled,
        onClick = { showColorPicker = true },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .border(BorderStroke(1.dp, colorPalette().textSecondary))
            )
        },
        icon = icon,
        showChevron = false,
        modifier = modifier
    )

    if (showColorPicker)
        DialogColorPicker(onDismiss = { showColorPicker = false }, color = color) {
            onColorSelected(it)
            showColorPicker = false
            SmartMessage(context.resources.getString(R.string.info_color_s_applied).format(title), context = context)
        }

}

@Composable
fun ButtonBarSettingEntry(
    title: String,
    text: String,
    icon: Int,
    iconSize: Dp = 22.dp,
    iconColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    SettingsEntry(
        title = title,
        titleSecondary = text.ifBlank { null },
        text = "",
        isEnabled = isEnabled,
        onClick = onClick,
        icon = icon,
        showChevron = false,
        modifier = modifier,
    )

}

@Composable
fun ButtonBarSettingEntry(
    title: String,
    text: String,
    icon: ImageVector,
    iconSize: Dp = 22.dp,
    iconColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    SettingsEntry(
        title = title,
        titleSecondary = text.ifBlank { null },
        text = "",
        isEnabled = isEnabled,
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = iconColor ?: colorPalette().textSecondary
            )
        },
        showChevron = false,
        modifier = modifier,
    )
}

@Composable
fun SliderSettingsEntry(
    title: String,
    text: String,
    state: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onSlide: (Float) -> Unit = { },
    onSlideComplete: () -> Unit = { },
    toDisplay: @Composable (Float) -> String = { it.toString() },
    steps: Int = 0,
    isEnabled: Boolean = true,
    usePadding: Boolean = true,
    icon: Int? = null,
) = Column(modifier = modifier) {

    val manualEnterDialog = object: IDialog {

        var valueFloat: Float by remember( state ) { mutableFloatStateOf( state ) }

        override val dialogTitle: String
            @Composable
            get() = stringResource( R.string.enter_the_value )

        override var isActive: Boolean by rememberSaveable { mutableStateOf(false) }
        override var value: String by remember( valueFloat ) {
            mutableStateOf( "%.1f".format( valueFloat ).replace(",", ".") )
        }

        override fun onSet( newValue: String ) {
            this.valueFloat = newValue.toFloatOrNull() ?: return
            onSlide( this.valueFloat )
            onSlideComplete()

            onDismiss()
        }
    }
    manualEnterDialog.Render()

    SettingsEntry(
        title = title,
        text = text,
        onClick = manualEnterDialog::onShortClick,
        isEnabled = isEnabled,
        icon = icon,
        valueText = toDisplay(state),
        showChevron = false,
    )

    Slider(
        state = state,
        setState = { value: Float ->
            manualEnterDialog.valueFloat = value
            onSlide(value)
        },
        onSlideComplete = onSlideComplete,
        range = range,
        steps = steps,
        modifier = Modifier
            .height(36.dp)
            .alpha(if (isEnabled) 1f else 0.5f)
            .let {
                if (usePadding)
                    it.padding(start = if (icon != null) 50.dp else 16.dp, end = 16.dp)
                else it
            }
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    )
}
