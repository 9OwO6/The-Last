package com.thelast.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thelast.app.BuildConfig
import com.thelast.app.R
import com.thelast.app.TheLastApplication

@Composable
fun UpdateCard() {
    val manager = (LocalContext.current.applicationContext as TheLastApplication).updates
    val state by manager.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var source by remember(state.repository) { mutableStateOf(state.repository) }
    var error by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf(false) }
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = com.thelast.app.ui.theme.theLastColors().panel)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(painterResource(R.drawable.story_delivery), null, Modifier.size(92.dp))
                Column(Modifier.weight(1f)) {
                    Text("让小日常，常用常新", style = MaterialTheme.typography.titleMedium)
                    Text("当前版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            if (state.progress != null) LinearProgressIndicator(progress = { state.progress ?: 0f }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("自动检查更新")
                    Text("打开软件时检查，每 6 小时至多一次", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.automatic, onCheckedChange = manager::setAutomatic)
            }
            if (state.repository.isNotEmpty()) Text(state.repository, style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (state.repository.isEmpty()) editing = true else manager.check(true) }, enabled = !state.busy) {
                    Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.repository.isEmpty()) "连接发布源" else "检查更新")
                }
                TextButton(onClick = { editing = true }, enabled = !state.busy) { Text("设置来源") }
            }
            if (editing) {
                HorizontalDivider()
                Text("GitHub 发布源", style = MaterialTheme.typography.titleSmall)
                Text("填写你信任的公开仓库。只读取正式 Release，无需账号或令牌。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = source, onValueChange = { source = it; error = null },
                    label = { Text("owner/repository") }, singleLine = true, isError = error != null,
                    modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row {
                    TextButton(onClick = { error = manager.configure(source); if (error == null) editing = false }) { Text("保存并检查") }
                    TextButton(onClick = { editing = false }) { Text("取消") }
                }
            }
            state.release?.let { release ->
                Button(onClick = { details = !details }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (details) "收起更新说明" else "查看新版本 ${release.version}")
                }
                if (details) {
                    HorizontalDivider()
                    Text(release.notes.ifBlank { "这个版本没有提供更新说明。" },
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()))
                    Text("安装包 ${release.size / 1024 / 1024} MB · 验证完整性与签名后安装", style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !state.busy, onClick = { if (state.ready) manager.install() else manager.download() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.ready) "安装新版本" else if (state.busy) "下载中…" else "下载更新")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateReminder(onOpen: () -> Unit) {
    val manager = (LocalContext.current.applicationContext as TheLastApplication).updates
    val state by manager.state.collectAsState()
    state.release?.let { release ->
        Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.SystemUpdate, null)
                Column(Modifier.weight(1f)) {
                    Text("新版本 ${release.version} 已到达", style = MaterialTheme.typography.titleSmall)
                    Text("点击查看更新说明与安装", style = MaterialTheme.typography.bodySmall)
                }
                Text("查看 →")
            }
        }
    }
}
