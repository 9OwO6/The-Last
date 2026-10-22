package com.thelast.app.ui.event

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventTypeEntity
import com.thelast.app.domain.model.EventInsight
import com.thelast.app.domain.model.EventStats
import com.thelast.app.R
import com.thelast.app.ui.component.formatElapsedTime
import com.thelast.app.ui.illustration.eventIllustrationForKey
import com.thelast.app.ui.illustration.statIllustrationForLabel
import com.thelast.app.ui.theme.TheLastMotion
import com.thelast.app.ui.theme.TheLastShape
import com.thelast.app.ui.theme.theLastColors
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class DetailTab(val label: String) {
    Timeline("时间线"),
    Trend("趋势"),
    Calendar("日历"),
}

private val DetailInk: Color @Composable get() = theLastColors().ink
private val DetailSoftInk: Color @Composable get() = theLastColors().muted
private val DetailSurface: Color @Composable get() = theLastColors().background
private val DetailPanel: Color @Composable get() = theLastColors().panel
private val DetailElevated: Color @Composable get() = theLastColors().surface
private val DetailAccent: Color @Composable get() = theLastColors().peach
private val DetailGreen: Color @Composable get() = theLastColors().green
private val DetailBlue: Color @Composable get() = theLastColors().blue
private val DetailYellow: Color @Composable get() = theLastColors().yellow
private val DetailLine: Color @Composable get() = theLastColors().line

@Composable
fun EventDetailRoute(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onEditEvent: () -> Unit,
    onEventDeleted: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editorState by remember { mutableStateOf<RecordEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<EventRecordEntity?>(null) }
    var showDeleteEventDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DetailTab.Timeline) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.messageShown()
    }

    EventDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEditEvent = onEditEvent,
        onDeleteEvent = { showDeleteEventDialog = true },
        onAddRecord = { editorState = viewModel.newRecordEditorState() },
        onEditRecord = { editorState = viewModel.editorStateFor(it) },
        onDeleteRecord = { deleteTarget = it },
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
    )

    editorState?.let { activeEditorState ->
        RecordEditorDialog(
            editorState = activeEditorState,
            onChange = { editorState = it },
            onDismiss = { editorState = null },
            onSave = {
                viewModel.saveRecord(
                    editorState = activeEditorState,
                    onInvalid = { editorState = it },
                    onSaved = { editorState = null },
                )
            },
        )
    }

    deleteTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录？") },
            text = { Text("这条历史记录会从本地数据库删除，删除后不能在应用内恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        viewModel.deleteRecord(record.id)
                    },
                ) {
                    Text(text = "删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (showDeleteEventDialog) {
        val eventName = uiState.eventType?.name ?: "这个事件"
        AlertDialog(
            onDismissRequest = { showDeleteEventDialog = false },
            title = { Text("永久删除“$eventName”？") },
            text = {
                Text("这个事件和它的 ${uiState.records.size} 条记录都会从本机永久删除，无法恢复。若只是暂时不想在首页看到它，可以继续使用归档。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteEventDialog = false
                        viewModel.deleteEvent(onEventDeleted)
                    },
                ) {
                    Text(text = "永久删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteEventDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailScreen(
    uiState: EventDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditEvent: () -> Unit,
    onDeleteEvent: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (EventRecordEntity) -> Unit,
    onDeleteRecord: (EventRecordEntity) -> Unit,
    selectedTab: DetailTab,
    onTabSelected: (DetailTab) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.eventType?.name ?: "事件详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DetailInk,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = DetailInk,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDeleteEvent) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除事件",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    IconButton(onClick = onEditEvent) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = "编辑事件", tint = DetailInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRecord,
                containerColor = DetailAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "补记记录")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DetailSurface)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            EventHeader(
                eventType = uiState.eventType,
                stats = uiState.stats,
                rhythmInsight = uiState.rhythmInsight,
            )
            Spacer(modifier = Modifier.height(16.dp))
            StatsSection(stats = uiState.stats)
            Spacer(modifier = Modifier.height(16.dp))
            DetailTabs(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
            Spacer(modifier = Modifier.height(14.dp))
            when (selectedTab) {
                DetailTab.Timeline -> TimelineSection(
                    records = uiState.records,
                    onAddRecord = onAddRecord,
                    onEditRecord = onEditRecord,
                    onDeleteRecord = onDeleteRecord,
                )

                DetailTab.Trend -> TrendSection(insight = uiState.insight)
                DetailTab.Calendar -> CalendarSection(
                    insight = uiState.insight,
                    records = uiState.records,
                    onEditRecord = onEditRecord,
                )
            }
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun EventHeader(
    eventType: EventTypeEntity?,
    stats: EventStats,
    rhythmInsight: RhythmInsight,
) {
    val elapsed = detailElapsedLabel(stats.latestOccurredAt)
    val eventColor = Color(eventType?.color ?: DefaultEventColor)
    Card(
        colors = CardDefaults.cardColors(containerColor = DetailElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(TheLastShape.Hero),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = TheLastMotion.settle())
            .border(1.dp, DetailLine, RoundedCornerShape(TheLastShape.Hero)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EventRhythmSeal(
                    color = eventColor,
                    icon = eventType?.icon ?: DefaultEventIcon,
                    recordCount = stats.recordCount,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = eventType?.name ?: "加载中",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = eventType?.category?.ifBlank { null } ?: "未设置分类",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetailSoftInk,
                    )
                    Text(
                        text = "上一次印章",
                        style = MaterialTheme.typography.labelLarge,
                        color = eventColor,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(DetailPanel, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    AnimatedContent(
                        targetState = elapsed.value,
                        transitionSpec = {
                            (fadeIn(TheLastMotion.reveal()) + scaleIn(initialScale = 0.9f)) togetherWith
                                (fadeOut(TheLastMotion.reveal(180)) + scaleOut(targetScale = 1.08f))
                        },
                        label = "detail-elapsed",
                    ) { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.displaySmall,
                            color = DetailInk,
                        )
                    }
                    Text(
                        text = elapsed.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = DetailSoftInk,
                    )
                }
                Text(
                    text = stats.latestOccurredAt?.let(::formatRecordTime) ?: "暂无记录",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DetailPanel, RoundedCornerShape(22.dp))
                    .border(1.dp, DetailLine, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = rhythmInsight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailInk,
                )
                Text(
                    text = rhythmInsight.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetailSoftInk,
                )
            }
        }
    }
}

@Composable
private fun EventRhythmSeal(
    color: Color,
    icon: String,
    recordCount: Int,
) {
    val turn by animateFloatAsState(
        targetValue = (recordCount.coerceAtMost(12) * 5f),
        animationSpec = TheLastMotion.gentleSpring(),
        label = "detail-seal-turn",
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer { rotationZ = turn },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color.copy(alpha = 0.13f))
            drawCircle(color = color.copy(alpha = 0.38f), style = Stroke(width = 2.dp.toPx()))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (78f + recordCount.coerceAtMost(12) * 20f),
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Image(
            painter = painterResource(eventIllustrationForKey(icon)),
            contentDescription = null,
            modifier = Modifier.size(65.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun StatsSection(stats: EventStats) {
    val items = listOf(
        Triple("记录次数", stats.recordCount.toString(), DetailGreen),
        Triple("最近一次", stats.latestOccurredAt?.let(::daysAgo) ?: "暂无", DetailYellow),
        Triple("平均间隔", stats.averageIntervalMillis?.let(::formatInterval) ?: "暂无", DetailBlue),
        Triple("最长间隔", stats.longestIntervalMillis?.let(::formatInterval) ?: "暂无", DetailAccent),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "小统计  🌱",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = DetailInk,
        )
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { (label, value, tint) ->
                    StatCard(
                        label = label,
                        value = value,
                        tint = tint,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.border(1.dp, tint.copy(alpha = 0.32f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.16f),
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = DetailSoftInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Image(
                painter = painterResource(statIllustrationForLabel(label)),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTabs(
    selectedTab: DetailTab,
    onTabSelected: (DetailTab) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        DetailTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DetailTab.entries.size,
                ),
            ) {
                Text(tab.label)
            }
        }
    }
}

@Composable
private fun TimelineSection(
    records: List<EventRecordEntity>,
    onAddRecord: () -> Unit,
    onEditRecord: (EventRecordEntity) -> Unit,
    onDeleteRecord: (EventRecordEntity) -> Unit,
) {
    if (records.isEmpty()) {
        EmptyRecordState(onAddRecord = onAddRecord)
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${records.size} 条",
                style = MaterialTheme.typography.bodySmall,
                color = DetailSoftInk,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            records.forEach { record ->
                RecordTimelineCard(
                    record = record,
                    onEdit = { onEditRecord(record) },
                    onDelete = { onDeleteRecord(record) },
                )
            }
        }
    }
}

@Composable
private fun TrendSection(insight: EventInsight) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
                colors = CardDefaults.cardColors(containerColor = DetailElevated),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, DetailLine, RoundedCornerShape(24.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "频率趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailInk,
                    )
                    Text(
                        text = trendSummary(insight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetailSoftInk,
                    )
                    IntervalTrendChart(insight = insight)
                }
        }
        Text(
            text = "小提示：点越低表示两次记录间隔越短，也就是频率更高。",
            style = MaterialTheme.typography.bodySmall,
            color = DetailSoftInk,
        )
    }
}

@Composable
private fun IntervalTrendChart(insight: EventInsight) {
    val points = insight.intervalPoints
    val axisColor = DetailLine
    val lineColor = DetailAccent
    val pointColor = DetailGreen
    val softFill = DetailPanel.copy(alpha = 0.56f)
    var chartVisible by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { chartVisible = true }
    val reveal by animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = TheLastMotion.reveal(680),
        label = "trend-reveal",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DetailSurface),
    ) {
        if (points.size < 2) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "再记录几次，就能看到频率趋势啦",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetailSoftInk,
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = reveal
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
            ) {
                val horizontalPadding = 28.dp.toPx()
                val verticalPadding = 24.dp.toPx()
                val chartWidth = size.width - horizontalPadding * 2
                val chartHeight = size.height - verticalPadding * 2
                val minY = points.minOf { it.intervalMillis }.toFloat()
                val maxY = points.maxOf { it.intervalMillis }.toFloat()
                val range = (maxY - minY).takeIf { it > 0f } ?: 1f

                fun xFor(index: Int): Float {
                    return horizontalPadding + (index - 1) * chartWidth / (points.size - 1).coerceAtLeast(1)
                }

                fun yFor(value: Long): Float {
                    return verticalPadding + chartHeight - ((value - minY) / range) * chartHeight
                }

                drawRoundRect(
                    color = softFill,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                )
                repeat(4) { step ->
                    val y = verticalPadding + chartHeight * step / 3f
                    drawLine(
                        color = axisColor,
                        start = androidx.compose.ui.geometry.Offset(horizontalPadding, y),
                        end = androidx.compose.ui.geometry.Offset(size.width - horizontalPadding, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = xFor(index + 1)
                    val y = yFor(point.intervalMillis)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )

                val firstY = yFor((points.first().intervalMillis + insight.trendSlopeMillis * 0).toLong())
                val lastEstimate = points.first().intervalMillis + insight.trendSlopeMillis * (points.size - 1)
                val lastY = yFor(lastEstimate.toLong())
                drawLine(
                    color = pointColor.copy(alpha = 0.65f),
                    start = androidx.compose.ui.geometry.Offset(xFor(1), firstY),
                    end = androidx.compose.ui.geometry.Offset(xFor(points.size), lastY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = pointColor,
                        radius = 5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            x = xFor(index + 1),
                            y = yFor(point.intervalMillis),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarSection(
    insight: EventInsight,
    records: List<EventRecordEntity>,
    onEditRecord: (EventRecordEntity) -> Unit,
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val firstDay = currentMonth.atDay(1)
    val leadingBlankCount = firstDay.dayOfWeek.value % 7
    val monthDays = (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
    val calendarCells = List(leadingBlankCount) { null } + monthDays
    val selectedRecords = records.filter { record ->
        Instant.ofEpochMilli(record.occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == selectedDate
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
                colors = CardDefaults.cardColors(containerColor = DetailElevated),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, DetailLine, RoundedCornerShape(24.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val targetMonth = currentMonth.minusMonths(1)
                                currentMonth = targetMonth
                                selectedDate = targetMonth.atDay(1)
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上个月")
                        }
                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINA)} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DetailInk,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "本月 ${monthDays.sumOf { insight.recordsByDate[it] ?: 0 }} 条",
                            style = MaterialTheme.typography.bodySmall,
                            color = DetailSoftInk,
                        )
                        IconButton(
                            onClick = {
                                val targetMonth = currentMonth.plusMonths(1)
                                currentMonth = targetMonth
                                selectedDate = targetMonth.atDay(1)
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下个月")
                        }
                    }
                    WeekHeader()
                    calendarCells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                    )
                                } else {
                                    CalendarDayCell(
                                        date = date,
                                        count = insight.recordsByDate[date] ?: 0,
                                        selected = date == selectedDate,
                                        onClick = { selectedDate = date },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            repeat(7 - week.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                )
                            }
                        }
                    }
                }
        }
        SelectedDateRecords(
            date = selectedDate,
            records = selectedRecords,
            onEditRecord = onEditRecord,
        )
    }
}

@Composable
private fun WeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = DetailSoftInk,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = count > 0
    val today = date == LocalDate.now()
    val background = when {
        selected -> DetailAccent.copy(alpha = 0.36f)
        active -> DetailGreen.copy(alpha = 0.24f)
        today -> DetailYellow.copy(alpha = 0.18f)
        else -> DetailSurface
    }
    val border = if (today) DetailYellow else Color.Transparent

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (today) border.copy(alpha = 0.25f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (active || today) FontWeight.SemiBold else FontWeight.Normal,
                color = DetailInk,
            )
        }
        if (active) {
            Text(
                text = "${count}次",
                style = MaterialTheme.typography.labelSmall,
                color = DetailInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedDateRecords(
    date: LocalDate,
    records: List<EventRecordEntity>,
    onEditRecord: (EventRecordEntity) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DetailPanel),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.border(1.dp, DetailLine, RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = DateTimeFormatter.ofPattern("M月d日").format(date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DetailInk,
            )
            if (records.isEmpty()) {
                DetailEmptyPanel(
                    title = "这一天还没有小记录",
                    message = "可以从右下角补记一条过去发生的时间。",
                    imageRes = R.drawable.decor_calendar,
                )
            } else {
                records.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DetailElevated)
                            .clickable { onEditRecord(record) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = DateTimeFormatter.ofPattern("HH:mm").format(
                                Instant.ofEpochMilli(record.occurredAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalTime(),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DetailInk,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = record.note?.ifBlank { null } ?: "轻轻记了一下",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DetailSoftInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecordState(
    onAddRecord: () -> Unit,
) {
    DetailEmptyPanel(
        title = "还没有记录",
        message = "点击右下角可以补记过去发生的时间。",
        imageRes = R.drawable.decor_notebook,
        action = {
            Button(
                onClick = onAddRecord,
                colors = ButtonDefaults.buttonColors(containerColor = DetailAccent, contentColor = Color.White),
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("补记一条")
            }
        },
    )
}

@Composable
private fun DetailEmptyPanel(
    title: String,
    message: String,
    imageRes: Int,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DetailElevated),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DetailLine, RoundedCornerShape(24.dp)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = DetailInk,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DetailSoftInk,
            )
            action?.invoke()
        }
    }
}

private fun trendSummary(insight: EventInsight): String {
    if (insight.intervalPoints.size < 2) return "数据还少，先把生活小事慢慢记起来。"
    return when {
        insight.trendSlopeMillis < -60L * 60L * 1000L -> "最近间隔在变短，说明这件事发生得更频繁。"
        insight.trendSlopeMillis > 60L * 60L * 1000L -> "最近间隔在变长，说明这件事发生得没那么频繁。"
        else -> "最近节奏比较稳定，频率变化不大。"
    }
}

@Composable
private fun RecordTimelineCard(
    record: EventRecordEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DetailPanel.copy(alpha = 0.78f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DetailLine, RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.note?.ifBlank { null } ?: "轻轻记了一下",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatRecordTime(record.occurredAt)} · ${daysAgo(record.occurredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DetailSoftInk,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除记录",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEditorDialog(
    editorState: RecordEditorState,
    onChange: (RecordEditorState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DetailSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = editorState.title.ifBlank { "补记记录" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailInk,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(R.drawable.decor_pencil),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painter = painterResource(R.drawable.decor_star),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            OutlinedTextField(
                value = editorState.occurredAtText,
                onValueChange = { onChange(editorState.copy(occurredAtText = it, error = null)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("发生时间") },
                singleLine = true,
                isError = editorState.error != null,
                supportingText = { Text(editorState.error ?: "格式：yyyy-MM-dd HH:mm") },
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                value = editorState.note,
                onValueChange = { onChange(editorState.copy(note = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注 🌼") },
                placeholder = { Text("写点小记忆...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(18.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Image(
                    painter = painterResource(R.drawable.decor_bear),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DetailElevated, contentColor = DetailInk),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f).border(1.dp, DetailLine, RoundedCornerShape(999.dp)),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = DetailAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存好啦")
                }
            }
        }
    }
}

private fun formatRecordTime(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm").format(dateTime)
}

private fun daysAgo(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, LocalDate.now(zone))
    return when (days) {
        0L -> "今天"
        1L -> "昨天"
        else -> "距今 $days 天"
    }
}

private data class DetailElapsedLabel(
    val value: String,
    val label: String,
)

private fun detailElapsedLabel(timestamp: Long?): DetailElapsedLabel {
    if (timestamp == null) return DetailElapsedLabel("还没有", "等待第一次记录")
    val value = formatElapsedTime(timestamp)
    val label = if (value == "刚刚") "今天已经记住" else "距离上一次"
    return DetailElapsedLabel(value, label)
}

private fun formatInterval(intervalMillis: Long): String {
    val safeMillis = intervalMillis.coerceAtLeast(0)
    val days = safeMillis / (24L * 60L * 60L * 1000L)
    if (days > 0) return "$days 天"
    val hours = safeMillis / (60L * 60L * 1000L)
    if (hours > 0) return "$hours 小时"
    val minutes = safeMillis / (60L * 1000L)
    return if (minutes > 0) "$minutes 分钟" else "刚刚"
}
