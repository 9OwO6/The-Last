package com.thelast.app.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thelast.app.ui.illustration.eventIllustrationForKey
import com.thelast.app.ui.theme.TheLastShape
import com.thelast.app.ui.theme.theLastColors

@Composable
fun EventFormRoute(
    viewModel: EventFormViewModel,
    onClose: () -> Unit,
    onArchived: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onClose()
    }
    LaunchedEffect(uiState.isArchived) {
        if (uiState.isArchived) onArchived()
    }

    EventFormScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onIconSelected = viewModel::onIconSelected,
        onColorSelected = viewModel::onColorSelected,
        onSave = viewModel::save,
        onArchive = viewModel::archive,
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFormScreen(
    uiState: EventFormUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onColorSelected: (Long) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onClose: () -> Unit,
) {
    var showArchiveDialog by remember { mutableStateOf(false) }
    val colors = theLastColors()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = if (uiState.isEditMode) "编辑生活节奏" else "新建生活节奏")
                        Text(
                            text = "给下一次相遇留一个位置",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = uiState.name.isNotBlank() && !uiState.isLoading,
                    ) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.ink,
                    navigationIconContentColor = colors.ink,
                    actionIconContentColor = colors.ink,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            EventPreviewCard(uiState = uiState)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("事件名称") },
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = {
                    Text(text = uiState.nameError ?: "例如：加油、剪头发、洗车")
                },
                shape = RoundedCornerShape(TheLastShape.SmallCard),
            )

            OutlinedTextField(
                value = uiState.category,
                onValueChange = onCategoryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类") },
                singleLine = true,
                supportingText = {
                    Text(text = "可选，例如：车辆、生活、家务")
                },
                shape = RoundedCornerShape(TheLastShape.SmallCard),
            )

            PresetIconSection(
                selectedIcon = uiState.selectedIcon,
                onIconSelected = onIconSelected,
            )

            PresetColorSection(
                selectedColor = uiState.selectedColor,
                onColorSelected = onColorSelected,
            )

            Button(
                onClick = onSave,
                enabled = uiState.name.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.surface),
                shape = RoundedCornerShape(TheLastShape.Pill),
            ) {
                Text(text = if (uiState.isEditMode) "保存这个节奏" else "开始记录这个节奏")
            }

            if (uiState.isEditMode) {
                TextButton(
                    onClick = { showArchiveDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "归档这个事件",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("归档事件？") },
            text = { Text("归档后首页不再显示这个事件，已有记录会保留在本地数据库中。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveDialog = false
                        onArchive()
                    },
                ) {
                    Text(text = "归档")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
private fun EventPreviewCard(uiState: EventFormUiState) {
    val colors = theLastColors()
    val eventColor = Color(uiState.selectedColor)
    Card(
        colors = CardDefaults.cardColors(containerColor = eventColor.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(TheLastShape.Hero),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, eventColor.copy(alpha = 0.34f), RoundedCornerShape(TheLastShape.Hero)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(colors.surface.copy(alpha = 0.72f), CircleShape)
                    .border(1.dp, eventColor.copy(alpha = 0.38f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(eventIllustrationForKey(uiState.selectedIcon)),
                    contentDescription = null,
                    modifier = Modifier.size(62.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("卡片预览", style = MaterialTheme.typography.labelLarge, color = eventColor)
                Text(
                    text = uiState.name.ifBlank { "一件想记住的小事" },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                )
                Text(
                    text = uiState.category.ifBlank { "生活节奏" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                Text("等待第一次记录", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetIconSection(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "图标",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EventIconOptions.forEach { option ->
                FilterChip(
                    selected = selectedIcon == option.key,
                    onClick = { onIconSelected(option.key) },
                    label = { Text(option.label) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(eventIllustrationForKey(option.key)),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    shape = RoundedCornerShape(TheLastShape.SmallCard),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetColorSection(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "颜色",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EventColorOptions.forEach { option ->
                ColorChoice(
                    option = option,
                    selected = selectedColor == option.value,
                    onClick = { onColorSelected(option.value) },
                )
            }
        }
    }
}

@Composable
private fun ColorChoice(
    option: EventColorOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(option.value), CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = option.label)
    }
}
