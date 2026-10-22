package com.thelast.app.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.thelast.app.data.datastore.ThemeMode
import com.thelast.app.R
import com.thelast.app.ui.component.formatElapsedTime
import com.thelast.app.domain.model.BackupExport
import com.thelast.app.domain.model.BackupExportFormat
import com.thelast.app.domain.model.BackupImportPreview
import com.thelast.app.domain.model.EventSummary
import com.thelast.app.domain.model.HomeCalendarRecord
import com.thelast.app.ui.illustration.StoryBanner
import com.thelast.app.ui.illustration.eventIllustrationForKey
import com.thelast.app.ui.illustration.statIllustrationForLabel
import com.thelast.app.ui.theme.TheLastColors as WarmColors
import com.thelast.app.ui.theme.TheLastMotion
import com.thelast.app.ui.theme.TheLastShape
import com.thelast.app.ui.theme.theLastColors
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class HomeTab(val label: String) {
    Home("首页"),
    Calendar("日历"),
    Stats("统计"),
    Profile("我的"),
}

@Composable
private fun warmColors(): WarmColors = theLastColors()

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onAddEvent: () -> Unit,
    onEventSelected: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        onAddEvent = onAddEvent,
        onQuickRecord = viewModel::quickRecord,
        onEventSelected = onEventSelected,
        onUserMessageShown = viewModel::userMessageShown,
        onUndoRecord = viewModel::undoQuickRecord,
        onPrepareExport = viewModel::prepareExport,
        onExportHandled = viewModel::exportHandled,
        onPrepareImport = viewModel::prepareImport,
        onDismissImportPreview = viewModel::dismissImportPreview,
        onConfirmImport = viewModel::confirmImport,
        onThemeModeChanged = viewModel::setThemeMode,
        onTemplateSelected = viewModel::createEventFromTemplate,
        onVoiceText = viewModel::handleVoiceText,
        onVoiceUnavailable = viewModel::onVoiceUnavailable,
        onDismissVoiceConfirmation = viewModel::dismissVoiceConfirmation,
        onConfirmVoiceAction = viewModel::confirmVoiceAction,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onAddEvent: () -> Unit,
    onQuickRecord: (Long) -> Unit,
    onEventSelected: (Long) -> Unit,
    onUserMessageShown: () -> Unit,
    onUndoRecord: (Long) -> Unit,
    onPrepareExport: (BackupExportFormat) -> Unit,
    onExportHandled: (Boolean) -> Unit,
    onPrepareImport: (String?) -> Unit,
    onDismissImportPreview: () -> Unit,
    onConfirmImport: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onTemplateSelected: (EventTemplate) -> Unit,
    onVoiceText: (String) -> Unit,
    onVoiceUnavailable: (String) -> Unit,
    onDismissVoiceConfirmation: () -> Unit,
    onConfirmVoiceAction: () -> Unit,
) {
    val colors = warmColors()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(HomeTab.Home) }
    var activeExport by remember { mutableStateOf<BackupExport?>(null) }
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        val export = activeExport
        val success = uri != null && export != null && writeExport(context, uri, export)
        activeExport = null
        onExportHandled(success)
    }
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        val export = activeExport
        val success = uri != null && export != null && writeExport(context, uri, export)
        activeExport = null
        onExportHandled(success)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        onPrepareImport(uri?.let { readImportContent(context, it) })
    }
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val matches = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            .orEmpty()
        val spokenText = matches.firstOrNull { it.isNotBlank() }
        if (spokenText == null) {
            onVoiceUnavailable("没有听到内容，请再试一次")
        } else {
            onVoiceText(spokenText)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            runCatching {
                voiceLauncher.launch(buildVoiceRecognitionIntent())
            }.onFailure {
                onVoiceUnavailable("这台设备暂时没有可用的语音识别服务")
            }
        } else {
            onVoiceUnavailable("需要麦克风权限，才能用语音记录")
        }
    }
    val startVoiceInput: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                voiceLauncher.launch(buildVoiceRecognitionIntent())
            }.onFailure {
                onVoiceUnavailable("这台设备暂时没有可用的语音识别服务")
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message.message,
            actionLabel = message.undoRecordId?.let { "撤销" },
            duration = if (message.undoRecordId != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            message.undoRecordId?.let(onUndoRecord)
        }
        onUserMessageShown()
    }
    LaunchedEffect(uiState.pendingExport) {
        val export = uiState.pendingExport ?: return@LaunchedEffect
        activeExport = export
        if (export.mimeType == "application/json") jsonLauncher.launch(export.fileName) else csvLauncher.launch(export.fileName)
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data, containerColor = colors.ink, contentColor = colors.surface,
                    actionColor = colors.yellow, shape = RoundedCornerShape(16.dp),
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.Home && uiState.events.isNotEmpty()) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = onAddEvent, containerColor = colors.green, contentColor = colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("新建小事") },
                )
            }
        },
        bottomBar = {
            WarmBottomBar(colors = colors, selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.Home -> QuickHomePage(
                colors = colors,
                uiState = uiState,
                onAddEvent = onAddEvent,
                onQuickRecord = onQuickRecord,
                onEventSelected = onEventSelected,
                onTemplateSelected = onTemplateSelected,
                onVoiceInput = startVoiceInput,
                onUpdates = { selectedTab = HomeTab.Profile },
                modifier = Modifier.padding(innerPadding),
            )

            HomeTab.Calendar -> CalendarPage(
                colors = colors,
                records = uiState.calendarRecords,
                onEventSelected = onEventSelected,
                modifier = Modifier.padding(innerPadding),
            )

            HomeTab.Stats -> StatsPage(colors = colors, events = uiState.events, modifier = Modifier.padding(innerPadding))
            HomeTab.Profile -> ProfilePage(
                colors = colors,
                uiState = uiState,
                onPrepareExport = onPrepareExport,
                onPrepareImport = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                onThemeModeChanged = onThemeModeChanged,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    uiState.pendingImportPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            onDismiss = onDismissImportPreview,
            onConfirm = onConfirmImport,
        )
    }
    uiState.voiceConfirmation?.let { confirmation ->
        VoiceConfirmationDialog(
            confirmation = confirmation,
            onDismiss = onDismissVoiceConfirmation,
            onConfirm = onConfirmVoiceAction,
        )
    }
}

@Composable
private fun WarmBottomBar(
    colors: WarmColors,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
        HomeTab.entries.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            HomeTab.Home -> Icons.Rounded.Home
                            HomeTab.Calendar -> Icons.Rounded.CalendarMonth
                            HomeTab.Stats -> Icons.Rounded.BarChart
                            HomeTab.Profile -> Icons.Rounded.Person
                        }, contentDescription = null, modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.ink,
                    selectedTextColor = colors.ink,
                    indicatorColor = colors.panel,
                    unselectedIconColor = colors.muted,
                    unselectedTextColor = colors.muted,
                ),
            )
        }
    }
}

@Composable
private fun QuickHomePage(
    colors: WarmColors,
    uiState: HomeUiState,
    onAddEvent: () -> Unit,
    onQuickRecord: (Long) -> Unit,
    onEventSelected: (Long) -> Unit,
    onTemplateSelected: (EventTemplate) -> Unit,
    onVoiceInput: () -> Unit,
    onUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayCount = uiState.calendarRecords.count {
        Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() == today
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 104.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LifeDrawerHeader(colors, todayCount, uiState.rhythmSummary, onVoiceInput)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            UpdateReminder(onOpen = onUpdates)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "收好的小事",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                )
                Text(
                    text = "名字由你决定，我们只替你记住时间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
        if (uiState.events.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyHomeState(
                    colors = colors,
                    templates = uiState.templates,
                    onTemplateSelected = onTemplateSelected,
                    onAddEvent = onAddEvent,
                )
            }
        } else {
            itemsIndexed(
                items = uiState.events,
                key = { _, event -> event.id },
                span = { index, _ ->
                    if (isWideDrawer(index)) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                },
            ) { index, event ->
                LifeDrawerCard(
                    colors = colors,
                    event = event,
                    wide = isWideDrawer(index),
                    isRecording = uiState.recordingEventId == event.id,
                    feedback = uiState.latestRecordFeedback?.takeIf { it.eventTypeId == event.id },
                    onQuickRecord = { onQuickRecord(event.id) },
                    onEventSelected = { onEventSelected(event.id) },
                )
            }
        }
    }
}

@Composable
private fun LifeDrawerHeader(
    colors: WarmColors,
    todayCount: Int,
    summary: String,
    onVoiceInput: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("THE LAST", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.green)
                Text(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA).format(LocalDate.now()),
                    style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            IconButton(onClick = onVoiceInput, modifier = Modifier.background(colors.surface, CircleShape)) {
                Icon(Icons.Rounded.Mic, contentDescription = "语音记录", tint = colors.green)
            }
        }
        StoryBanner("生活里的小小收藏", "我的生活抽屉", "让每件小事，\n都有被记住的位置。",
            R.drawable.story_drawer)
        Row(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("今天 $todayCount 次", style = MaterialTheme.typography.labelLarge, color = colors.green,
                modifier = Modifier.background(colors.green.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(8.dp))
            Text(summary, style = MaterialTheme.typography.bodySmall, color = colors.muted, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LifeDrawerCard(
    colors: WarmColors,
    event: EventSummary,
    wide: Boolean,
    isRecording: Boolean,
    feedback: HomeRecordFeedback?,
    onQuickRecord: () -> Unit,
    onEventSelected: () -> Unit,
) {
    val elapsed = elapsedLabel(event.lastOccurredAt)
    val cardColor by animateColorAsState(
        targetValue = feedback?.let { Color(event.color).copy(alpha = 0.26f) } ?: colors.surface,
        animationSpec = TheLastMotion.reveal(),
        label = "event-card-color",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (feedback != null) 1.018f else 1f,
        animationSpec = TheLastMotion.gentleSpring(),
        label = "drawer-card-scale",
    )
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 10.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (feedback != null) 3.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (wide) Modifier.defaultMinSize(minHeight = 190.dp) else Modifier.defaultMinSize(minHeight = 224.dp))
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .animateContentSize(animationSpec = TheLastMotion.settle())
            .border(1.dp, colors.line.copy(alpha = 0.7f), shape),
        shape = shape,
        onClick = onEventSelected,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor)
                .padding(if (wide) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawerMonogram(colors = colors, event = event, compact = !wide)
                AnimatedVisibility(
                    visible = feedback != null,
                    enter = fadeIn(TheLastMotion.reveal()) + scaleIn(initialScale = 0.7f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Text(
                        text = "已记住",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.ink,
                        modifier = Modifier
                            .border(1.dp, Color(event.color), RoundedCornerShape(TheLastShape.Pill))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
                if (feedback == null) {
                    Text(
                        text = "${event.recordCount} 次 · 查看 →",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        ElapsedDrawerValue(value = elapsed.value, color = colors.ink)
                    }
                    DrawerRecordButton(
                        colors = colors,
                        isRecording = isRecording,
                        onQuickRecord = onQuickRecord,
                    )
                }
            } else {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ElapsedDrawerValue(value = elapsed.value, color = colors.ink)
            }
            Text(
                text = feedback?.message
                    ?: event.lastOccurredAt?.let { "上次 ${formatShortDate(it)}" }
                    ?: "等待第一次记录",
                style = MaterialTheme.typography.bodySmall,
                color = if (feedback != null) colors.ink else colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!wide) {
                DrawerRecordButton(
                    colors = colors,
                    isRecording = isRecording,
                    onQuickRecord = onQuickRecord,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = if (wide) 64.dp else 42.dp, height = 4.dp)
                    .background(colors.ink.copy(alpha = 0.14f), RoundedCornerShape(TheLastShape.Pill)),
            )
        }
    }
}

@Composable
private fun DrawerMonogram(colors: WarmColors, event: EventSummary, compact: Boolean) {
    Box(
        modifier = Modifier
            .size(if (compact) 52.dp else 62.dp)
            .background(Color(event.color).copy(alpha = 0.20f), RoundedCornerShape(if (compact) 14.dp else 17.dp))
            .border(1.dp, Color(event.color).copy(alpha = 0.30f), RoundedCornerShape(if (compact) 14.dp else 17.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(eventIllustrationForKey(event.icon)), null,
            Modifier.size(if (compact) 44.dp else 54.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun ElapsedDrawerValue(value: String, color: Color) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (fadeIn(TheLastMotion.reveal()) + scaleIn(initialScale = 0.92f)) togetherWith
                (fadeOut(TheLastMotion.reveal(180)) + scaleOut(targetScale = 1.06f))
        },
        label = "drawer-elapsed-value",
    ) { elapsed ->
        Text(
            text = elapsed,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun DrawerRecordButton(
    colors: WarmColors,
    isRecording: Boolean,
    onQuickRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onQuickRecord()
        },
        enabled = !isRecording,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.ink,
            contentColor = colors.surface,
            disabledContainerColor = colors.muted.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(TheLastShape.Pill),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier,
    ) {
        Text(if (isRecording) "正在记…" else "记一下")
    }
}

private fun isWideDrawer(index: Int): Boolean = index == 0 || index % 5 == 4

@Composable
private fun EventIllustration(
    colors: WarmColors,
    color: Color,
    icon: String,
    large: Boolean = false,
) {
    val outer = if (large) 82.dp else 44.dp
    val inner = if (large) 62.dp else 30.dp
    Box(
        modifier = Modifier
            .size(outer)
            .background(color.copy(alpha = 0.13f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(inner)
                .background(Color.White.copy(alpha = 0.48f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(eventIllustrationForKey(icon)),
                contentDescription = null,
                modifier = Modifier.size(if (large) 58.dp else 28.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun CalendarPage(
    colors: WarmColors,
    records: List<HomeCalendarRecord>,
    onEventSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    var currentMonth by remember { mutableStateOf(YearMonth.now(zone)) }
    var selectedDate by remember { mutableStateOf(LocalDate.now(zone)) }
    val recordsByDate = remember(records) {
        records.groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 96.dp),
    ) {
        item {
            StoryBanner("日子慢慢，回忆满满", "日历小笺", "翻到那一天，\n看看留下的小事。", R.drawable.story_calendar)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "${currentMonth.monthValue}月",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                    Text(currentMonth.year.toString(), color = colors.muted)
                }
                Row {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1); selectedDate = currentMonth.atDay(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上个月", tint = colors.ink)
                    }
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1); selectedDate = currentMonth.atDay(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下个月", tint = colors.ink)
                    }
                }
            }
        }
        item {
            WarmCalendarGrid(
                colors = colors,
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                recordsByDate = recordsByDate,
                onDateSelected = { selectedDate = it },
            )
        }
        item {
            SelectedDateRecords(
                colors = colors,
                date = selectedDate,
                records = recordsByDate[selectedDate].orEmpty(),
                onEventSelected = onEventSelected,
            )
        }
    }
}

@Composable
private fun WarmCalendarGrid(
    colors: WarmColors,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    recordsByDate: Map<LocalDate, List<HomeCalendarRecord>>,
    onDateSelected: (LocalDate) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(28.dp)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = colors.muted, modifier = Modifier.weight(1f))
                }
            }
            val first = currentMonth.atDay(1)
            val cells = buildList<LocalDate?> {
                repeat(first.dayOfWeek.value - 1) { add(null) }
                (1..currentMonth.lengthOfMonth()).forEach { add(currentMonth.atDay(it)) }
                while (size % 7 != 0) add(null)
            }
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.86f))
                        } else {
                            CalendarDayCell(
                                colors = colors,
                                date = date,
                                selected = date == selectedDate,
                                records = recordsByDate[date].orEmpty(),
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    colors: WarmColors,
    date: LocalDate,
    selected: Boolean,
    records: List<HomeCalendarRecord>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        selected -> colors.peach.copy(alpha = 0.42f)
        date == LocalDate.now() -> colors.green.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    Column(
        modifier = modifier
            .aspectRatio(0.86f)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.ink)
        if (records.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                records.take(4).forEach { record ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(record.eventColor), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDateRecords(
    colors: WarmColors,
    date: LocalDate,
    records: List<HomeCalendarRecord>,
    onEventSelected: (Long) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.panel),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(24.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(date.format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
            if (records.isEmpty()) {
                WarmEmptyState(
                    colors = colors,
                    title = "这一天很安静",
                    message = "还没有记录落在这一天，生活的小节奏会慢慢长出来。",
                    imageRes = R.drawable.story_journal,
                )
            } else {
                records.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface)
                            .clickable { onEventSelected(record.eventTypeId) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EventIllustration(colors = colors, color = Color(record.eventColor), icon = record.eventIcon)
                        Column {
                            Text(record.eventName, fontWeight = FontWeight.SemiBold, color = colors.ink)
                            Text(formatClockTime(record.occurredAt), color = colors.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPage(
    colors: WarmColors,
    events: List<EventSummary>,
    modifier: Modifier = Modifier,
) {
    val recorded = events.count { it.lastOccurredAt != null }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 96.dp),
    ) {
        item {
            StoryBanner("按自己的速度，慢慢生长", "节奏小花园", "不追求连续打卡，\n只看看生活留下了什么。", R.drawable.story_garden)
        }
        if (events.isEmpty()) {
            item {
                WarmEmptyState(
                    colors = colors,
                    title = "还没有统计",
                    message = "添加并记录几次后，这里会长出你的频率和节奏。",
                    imageRes = R.drawable.story_journal,
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.panel),
                shape = RoundedCornerShape(TheLastShape.Card),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.line, RoundedCornerShape(TheLastShape.Card)),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = if (recorded == 0) "节奏正在等待第一笔" else "$recorded 件小事已经有了时间轨迹",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                        )
                        Text(
                            text = if (events.isEmpty()) "先创建一件想记住的小事吧。" else "每一次记录，都会让你的生活轮廓更清楚。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(colors, "事件", events.size.toString(), colors.green, Modifier.weight(1f))
                StatTile(colors, "已记录", recorded.toString(), colors.yellow, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(colors, "待开始", (events.size - recorded).toString(), colors.blue, Modifier.weight(1f))
                StatTile(colors, "本地", "安全", colors.lavender, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(colors: WarmColors, label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.border(1.dp, tint.copy(alpha = 0.34f), RoundedCornerShape(22.dp)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text(label, color = colors.muted)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = colors.ink)
            }
            Image(
                painter = painterResource(statIllustrationForLabel(label)),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun ProfilePage(
    colors: WarmColors,
    uiState: HomeUiState,
    onPrepareExport: (BackupExportFormat) -> Unit,
    onPrepareImport: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 96.dp),
    ) {
        item {
            Text("我的小角落", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
            Text("妥善收好回忆，也照顾自己的偏好。", color = colors.muted)
        }
        item { UpdateCard() }
        item { CloudSyncCard() }
        item {
            ProfileCard(colors = colors, icon = { Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = colors.green) }) {
                Text("导出备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Text("导出为 JSON 或 CSV，方便自己保存。", color = colors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onPrepareExport(BackupExportFormat.Json) }, colors = ButtonDefaults.buttonColors(containerColor = colors.green, contentColor = Color.White), shape = RoundedCornerShape(999.dp)) {
                        Text("JSON")
                    }
                    Button(onClick = { onPrepareExport(BackupExportFormat.Csv) }, colors = ButtonDefaults.buttonColors(containerColor = colors.peach, contentColor = Color.White), shape = RoundedCornerShape(999.dp)) {
                        Text("CSV")
                    }
                }
            }
        }
        item {
            ProfileCard(colors = colors, icon = { Icon(Icons.Rounded.Restore, contentDescription = null, tint = colors.blue) }) {
                Text("恢复备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Text("从 thelast 的 JSON 备份合并恢复，不会删除当前数据。", color = colors.muted)
                Button(
                    onClick = onPrepareImport,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.surface),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text("选择 JSON")
                }
            }
        }
        item {
            ProfileCard(colors = colors, icon = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = colors.lavender) }) {
                Text("主题偏好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Text("选择 thelast 的整体光线。", color = colors.muted)
                ThemeModeSelector(
                    selected = uiState.themeMode,
                    onSelected = onThemeModeChanged,
                )
            }
        }
        item {
            ProfileCard(colors = colors, icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.blue) }) {
                Text("关于 thelast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Text("一个本地优先的小事件间隔记录器，适合轻轻记下生活里的重复瞬间。", color = colors.muted)
            }
        }
        item {
            ProfileCard(colors = colors, icon = { Icon(Icons.Rounded.Shield, contentDescription = null, tint = colors.peach) }) {
                Text("版本 ${com.thelast.app.BuildConfig.VERSION_NAME} · 生活常新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Text("本地优先，离线也能记录。连接 MyJournal 后可同步到其他设备。", color = colors.muted)
            }
        }
    }
}

@Composable
private fun EmptyHomeState(
    colors: WarmColors,
    templates: List<EventTemplate>,
    onTemplateSelected: (EventTemplate) -> Unit,
    onAddEvent: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.line, RoundedCornerShape(28.dp)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.story_journal),
                contentDescription = null,
                modifier = Modifier.size(148.dp),
                contentScale = ContentScale.Fit,
            )
            Text("第一件小事，从这里开始", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.ink)
            Text("先从一个模板开始，也可以创建自己的小节奏。", color = colors.muted)
            templates.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { template ->
                        TemplateButton(
                            colors = colors,
                            template = template,
                            onClick = { onTemplateSelected(template) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Button(onClick = onAddEvent, colors = ButtonDefaults.buttonColors(containerColor = colors.peach, contentColor = Color.White), shape = RoundedCornerShape(999.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("自定义事件")
            }
        }
    }
}

@Composable
private fun TemplateButton(
    colors: WarmColors,
    template: EventTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(template.color).copy(alpha = 0.12f))
            .border(1.dp, Color(template.color).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventIllustration(colors = colors, color = Color(template.color), icon = template.icon)
        Text(template.name, style = MaterialTheme.typography.labelLarge, color = colors.ink)
    }
}

@Composable
private fun ProfileCard(
    colors: WarmColors,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(24.dp)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(colors.panel, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f), content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    val options = listOf(
        ThemeMode.System to "跟随",
        ThemeMode.Warm to "暖光",
        ThemeMode.Night to "夜灯",
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun WarmEmptyState(
    colors: WarmColors,
    title: String,
    message: String,
    imageRes: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(22.dp))
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(84.dp),
            contentScale = ContentScale.Fit,
        )
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: BackupImportPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复这份备份？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("将合并导入 ${preview.eventTypeCount} 个事件、${preview.recordCount} 条记录。")
                Text("当前数据不会被删除或覆盖；重复导入同一份备份会产生重复记录。")
                Text("备份时间：${formatImportTime(preview.exportedAt)}")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认恢复")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun VoiceConfirmationDialog(
    confirmation: VoiceConfirmation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = when (confirmation.action) {
        VoiceConfirmationAction.CreateEvent -> "新建这张卡片？"
        VoiceConfirmationAction.CreateAndRecord -> "新建并记录一次？"
    }
    val message = when (confirmation.action) {
        VoiceConfirmationAction.CreateEvent -> "识别到「${confirmation.rawText}」，将新建「${confirmation.eventName}」卡片。"
        VoiceConfirmationAction.CreateAndRecord -> "没找到「${confirmation.eventName}」卡片。要先新建它，并记录一次现在吗？"
    }
    val confirmLabel = when (confirmation.action) {
        VoiceConfirmationAction.CreateEvent -> "确认新建"
        VoiceConfirmationAction.CreateAndRecord -> "新建并记录"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private fun buildVoiceRecognitionIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "说：记录喝水，或新建卡片加油")
    }
}

private fun readImportContent(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }.getOrNull()
}

private fun writeExport(context: android.content.Context, uri: Uri, export: BackupExport): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(export.content.toByteArray(Charsets.UTF_8)) }
            ?: error("Cannot open output stream")
    }.isSuccess
}

private data class ElapsedLabel(val value: String)

private fun elapsedLabel(timestamp: Long?): ElapsedLabel {
    if (timestamp == null) return ElapsedLabel("还没记")
    return ElapsedLabel(formatElapsedTime(timestamp))
}

private fun formatShortDate(timestamp: Long): String {
    return DateTimeFormatter.ofPattern("M月d日 HH:mm").format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
}

private fun formatClockTime(timestamp: Long): String {
    return DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
}

private fun formatImportTime(timestamp: Long): String {
    return DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm").format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
}
