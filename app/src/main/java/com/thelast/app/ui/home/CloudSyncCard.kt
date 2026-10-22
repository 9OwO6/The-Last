package com.thelast.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thelast.app.TheLastApplication
import com.thelast.app.ui.theme.theLastColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CloudSyncCard() {
    val context = LocalContext.current
    val app = context.applicationContext as TheLastApplication
    val manager = app.cloudSync
    val state by manager.state.collectAsState()
    val pending by app.database.syncDao().observePending().collectAsState(initial = 0)
    val conflicts by app.database.syncDao().observeConflicts().collectAsState(initial = 0)
    val colors = theLastColors()
    val scope = rememberCoroutineScope()
    var server by remember(state.server) { mutableStateOf(state.server.ifEmpty { "https://my-journal-amber.vercel.app" }) }
    var confirmCloud by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            exportMessage = runCatching {
                val content = manager.exportSyncBackup()
                withContext(Dispatchers.IO) { checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(content.toByteArray(Charsets.UTF_8)) } }
                "同步草稿备份已导出"
            }.getOrElse { "导出失败，请重试" }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("云端同步 · MyJournal", style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(state.message, color = colors.muted)
            Text("$pending 项待同步 · $conflicts 项冲突", style = MaterialTheme.typography.bodySmall, color = colors.muted)
            if (state.connected) {
                Text(state.server, style = MaterialTheme.typography.bodySmall, color = colors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !state.busy, onClick = { scope.launch { manager.syncNow() } }) { Text("立即同步") }
                    TextButton(onClick = manager::disconnect) { Text("断开本机") }
                }
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${state.server}/mobile/thelast"))) }) { Text("打开网页记录页") }
            } else {
                Text("连接后，本机现有记录会上传到你的私人空间。其他设备可用网页记录。首次连接前建议导出 JSON 备份。", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text("MyJournal 网站地址（HTTPS）") },
                    placeholder = { Text("https://你的网站") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !state.busy && server.isNotBlank(), onClick = { manager.startPair(server) }) { Text(if (state.pairingCode.isEmpty()) "连接我的 MyJournal" else "重新生成连接码") }
                if (state.pairingCode.isNotEmpty()) {
                    Text("连接码：${state.pairingCode}", style = MaterialTheme.typography.titleLarge, color = colors.ink)
                    Text("浏览器登录后核对连接码并确认授权，再回到这里检查连接。", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${state.server}/thelast?pair=${state.pairingCode}"))) }) { Text("浏览器授权") }
                        TextButton(onClick = manager::checkPair) { Text("检查连接") }
                    }
                }
            }
            if (conflicts > 0) {
                Text("两端修改了同一条数据，或事项已在其他设备删除。先导出本机草稿，再决定使用云端版本。", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { export.launch("thelast-sync-drafts-${System.currentTimeMillis()}.json") }) { Text("导出含冲突的完整同步草稿") }
                TextButton(enabled = !state.busy, onClick = { confirmCloud = true }) { Text("使用云端版本") }
            }
            if (exportMessage.isNotEmpty()) Text(exportMessage, color = colors.muted)
        }
    }
    if (confirmCloud) AlertDialog(onDismissRequest = { confirmCloud = false }, title = { Text("使用云端版本？") },
        text = { Text("这会放弃冲突的本机修改。建议先导出同步草稿，其他未同步记录仍会保留。") },
        confirmButton = { TextButton(onClick = { confirmCloud = false; manager.useCloudVersion() }) { Text("使用云端") } },
        dismissButton = { TextButton(onClick = { confirmCloud = false }) { Text("取消") } })
}
