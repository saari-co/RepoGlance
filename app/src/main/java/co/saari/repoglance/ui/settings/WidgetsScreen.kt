package co.saari.repoglance.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import co.saari.repoglance.ui.theme.LabelRole
import co.saari.repoglance.ui.theme.LabelText
import co.saari.repoglance.ui.theme.LocalControlShape
import co.saari.repoglance.ui.theme.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val WIDGETS_TEST_TAG = "repoglance:widgets"
internal const val WIDGETS_BACK_TEST_TAG = "repoglance:widgets-back"
internal const val WIDGETS_ADD_TEST_TAG = "repoglance:widgets-add"
internal const val WIDGETS_PLACED_TEST_TAG = "repoglance:widgets-placed"
internal const val WIDGETS_PLACED_HEAD = "On your home screen"

@Composable
fun WidgetsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(WidgetsUiState(pinSupported = true, placed = null, sample = false)) }
    LifecycleResumeEffect(Unit) {
        val job = scope.launch { state = withContext(Dispatchers.IO) { loadWidgetsUiState(context) } }
        onPauseOrDispose { job.cancel() }
    }
    BackHandler(onBack = onBack)
    WidgetsContent(
        state = state,
        onBack = onBack,
        onAdd = { kind -> WidgetPinning.request(context, kind) },
        onOpenSetup = { id -> context.startActivity(WidgetPinning.setupIntent(context, id)) },
        modifier = modifier,
    )
}

@Composable
fun WidgetsContent(
    state: WidgetsUiState,
    onBack: () -> Unit,
    onAdd: (WidgetKind) -> Unit,
    onOpenSetup: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = LocalWidgetsLook.current
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(WIDGETS_TEST_TAG),
        topBar = { SettingsTopBar("Widgets", WIDGETS_BACK_TEST_TAG, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            if (state.sample) item(key = "sample-note") { WidgetsNote(WIDGETS_SAMPLE_NOTE) }
            when (look) {
                WidgetsLook.CARDS -> cardsLook(state, onAdd, onOpenSetup)
                WidgetsLook.LIST -> listLook(state, onAdd, onOpenSetup)
                WidgetsLook.PREVIEWS -> previewsLook(state, onAdd, onOpenSetup)
                WidgetsLook.PLACED_FIRST -> placedFirstLook(state, onAdd, onOpenSetup)
                WidgetsLook.GROUPED -> groupedLook(state, onAdd, onOpenSetup)
            }
        }
    }
}

private fun LazyListScope.cardsLook(
    state: WidgetsUiState,
    onAdd: (WidgetKind) -> Unit,
    onOpenSetup: (Int) -> Unit,
) {
    if (!state.pinSupported) item(key = "how-to") { WidgetsNote(WIDGETS_HOW_TO) }
    WidgetKind.entries.forEach { kind ->
        item(key = "add-${kind.name}") {
            TonalCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(kind.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        kind.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.pinSupported) {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AddButton(kind, onAdd)
                        }
                    }
                }
            }
        }
    }
    item(key = "placed-head") { SettingsSectionHead(WIDGETS_PLACED_HEAD) }
    placedRows(state.placed, onOpenSetup, withIcons = false)
}

private fun LazyListScope.listLook(
    state: WidgetsUiState,
    onAdd: (WidgetKind) -> Unit,
    onOpenSetup: (Int) -> Unit,
) {
    item(key = "add-head") { SettingsSectionHead("Add a widget") }
    if (!state.pinSupported) {
        item(key = "how-to") {
            SettingsRow(headline = "Add from your home screen", supporting = WIDGETS_HOW_TO, icon = Icons.Outlined.Info)
        }
    }
    WidgetKind.entries.forEach { kind ->
        item(key = "add-${kind.name}") {
            ListItem(
                headlineContent = { Text(kind.title) },
                supportingContent = { Text(kind.description) },
                leadingContent = { Icon(kind.icon(), contentDescription = null) },
                trailingContent = if (state.pinSupported) {
                    {
                        IconButton(onClick = { onAdd(kind) }, modifier = Modifier.testTag(kind.testTag)) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add ${kind.title}")
                        }
                    }
                } else {
                    null
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = if (state.pinSupported) Modifier.clickable { onAdd(kind) } else Modifier,
            )
        }
    }
    item(key = "placed-head") { SettingsSectionHead(WIDGETS_PLACED_HEAD) }
    placedRows(state.placed, onOpenSetup, withIcons = true)
}

private fun LazyListScope.previewsLook(
    state: WidgetsUiState,
    onAdd: (WidgetKind) -> Unit,
    onOpenSetup: (Int) -> Unit,
) {
    if (!state.pinSupported) item(key = "how-to") { WidgetsNote(WIDGETS_HOW_TO) }
    item(key = "previews") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WidgetKind.entries.forEach { kind ->
                TonalCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(112.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            WidgetSketch(kind)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(kind.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            kind.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.pinSupported) {
                            Spacer(Modifier.height(12.dp))
                            AddButton(kind, onAdd, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
    item(key = "placed-head") { SettingsSectionHead(WIDGETS_PLACED_HEAD) }
    placedRows(state.placed, onOpenSetup, withIcons = true)
}

private fun LazyListScope.placedFirstLook(
    state: WidgetsUiState,
    onAdd: (WidgetKind) -> Unit,
    onOpenSetup: (Int) -> Unit,
) {
    item(key = "placed-head") { SettingsSectionHead(WIDGETS_PLACED_HEAD) }
    placedRows(state.placed, onOpenSetup, withIcons = true)
    item(key = "add") {
        if (state.pinSupported) {
            var choosing by remember { mutableStateOf(false) }
            PrimaryButton(
                onClick = { choosing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag(WIDGETS_ADD_TEST_TAG),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                LabelText("Add a widget", LabelRole.BUTTON)
            }
            if (choosing) {
                AddWidgetDialog(
                    onDismiss = { choosing = false },
                    onChoose = { kind ->
                        choosing = false
                        onAdd(kind)
                    },
                )
            }
        } else {
            WidgetsNote(WIDGETS_HOW_TO)
        }
    }
}

private fun LazyListScope.groupedLook(
    state: WidgetsUiState,
    onAdd: (WidgetKind) -> Unit,
    onOpenSetup: (Int) -> Unit,
) {
    if (!state.pinSupported) item(key = "how-to") { WidgetsNote(WIDGETS_HOW_TO) }
    WidgetKind.entries.forEach { kind ->
        item(key = "group-${kind.name}") {
            TonalCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kind.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                kind.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.pinSupported) {
                            Spacer(Modifier.width(12.dp))
                            AddButton(kind, onAdd)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    val ofKind = state.placed?.filter { it.kind == kind }
                    when {
                        ofKind == null -> GroupLine("Checking your home screen…")
                        ofKind.isEmpty() -> GroupLine("None on your home screen yet")
                        else -> ofKind.forEach { PlacedRow(it, onOpenSetup, withIcon = false, onCard = true) }
                    }
                }
            }
        }
    }
    if (state.placed?.isNotEmpty() == true) item(key = "remove-hint") { WidgetsNote(WIDGETS_REMOVE_HINT) }
}

private fun LazyListScope.placedRows(placed: List<PlacedWidget>?, onOpenSetup: (Int) -> Unit, withIcons: Boolean) {
    when {
        placed == null -> item(key = "placed-loading") { WidgetsNote("Checking your home screen…") }
        placed.isEmpty() -> item(key = "placed-empty") { WidgetsNote(WIDGETS_EMPTY_TEXT) }
        else -> {
            placed.forEach { widget ->
                item(key = "placed-${widget.appWidgetId}") { PlacedRow(widget, onOpenSetup, withIcons) }
            }
            item(key = "remove-hint") { WidgetsNote(WIDGETS_REMOVE_HINT) }
        }
    }
}

@Composable
private fun PlacedRow(widget: PlacedWidget, onOpenSetup: (Int) -> Unit, withIcon: Boolean, onCard: Boolean = false) {
    val opensSetup = widget is PlacedWidget.Repository
    ListItem(
        headlineContent = { Text(widget.headline()) },
        supportingContent = { Text(widget.supporting()) },
        leadingContent = if (withIcon) {
            { Icon(widget.kind.icon(), contentDescription = null) }
        } else {
            null
        },
        trailingContent = if (opensSetup) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Change this widget") }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (onCard) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.background
            },
        ),
        modifier = Modifier
            .testTag("$WIDGETS_PLACED_TEST_TAG-${widget.appWidgetId}")
            .then(if (opensSetup) Modifier.clickable { onOpenSetup(widget.appWidgetId) } else Modifier),
    )
}

@Composable
private fun AddButton(kind: WidgetKind, onAdd: (WidgetKind) -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(onClick = { onAdd(kind) }, modifier = modifier.testTag(kind.testTag)) {
        LabelText("Add", LabelRole.BUTTON)
    }
}

@Composable
private fun AddWidgetDialog(onDismiss: () -> Unit, onChoose: (WidgetKind) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a widget") },
        text = {
            Column {
                WidgetKind.entries.forEach { kind ->
                    ListItem(
                        headlineContent = { Text(kind.title) },
                        supportingContent = { Text(kind.description) },
                        leadingContent = { Icon(kind.icon(), contentDescription = null) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier.clickable { onChoose(kind) }.testTag(kind.testTag),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { LabelText("Cancel", LabelRole.BUTTON) } },
    )
}

@Composable
private fun TonalCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = LocalControlShape.current.card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun WidgetsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun GroupLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun WidgetSketch(kind: WidgetKind) {
    val frame = MaterialTheme.colorScheme.surfaceContainerHighest
    val bar = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    when (kind) {
        WidgetKind.REPOSITORY -> Column(
            modifier = Modifier
                .width(112.dp)
                .height(60.dp)
                .background(frame, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SketchBar(56.dp, accent)
            SketchLedger(bar)
            SketchLedger(bar)
        }
        WidgetKind.PINNED_REPOS -> Column(
            modifier = Modifier
                .width(112.dp)
                .height(100.dp)
                .background(frame, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SketchBar(48.dp, accent)
            repeat(3) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SketchBar(64.dp, bar)
                    SketchBar(40.dp, bar)
                }
            }
        }
    }
}

@Composable
private fun SketchBar(width: Dp, color: Color) {
    Box(modifier = Modifier.width(width).height(5.dp).background(color, RoundedCornerShape(3.dp)))
}

@Composable
private fun SketchLedger(color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SketchBar(40.dp, color)
        Box(modifier = Modifier.size(10.dp, 5.dp).background(color, RoundedCornerShape(3.dp)))
    }
}

private fun WidgetKind.icon(): ImageVector = when (this) {
    WidgetKind.REPOSITORY -> Icons.Outlined.Widgets
    WidgetKind.PINNED_REPOS -> Icons.Outlined.PushPin
}
