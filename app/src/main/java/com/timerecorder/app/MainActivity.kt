package com.timerecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val PageBackground = Color(0xFFF7F8F5)
private val PrimaryGreen = Color(0xFF356859)
private val Ink = Color(0xFF1F2A25)
private val MutedInk = Color(0xFF718078)

data class TimeEntry(val title: String, val duration: String, val time: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { TimeRecorderWorkspace() }
    }
}

@Composable
fun TimeRecorderApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
            TimeRecorderScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRecorderScreen() {
    var isRunning by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf("产品设计") }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    val entries = remember {
        mutableStateListOf(
            TimeEntry("产品设计", "01:24:18", "今天 09:10"),
            TimeEntry("阅读与学习", "00:48:32", "昨天 20:15"),
            TimeEntry("团队会议", "00:32:05", "昨天 14:00")
        )
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("时间记录", fontWeight = FontWeight.Bold, color = Ink)
                        Text("星期三，10月21日", fontSize = 12.sp, color = MutedInk)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Add, contentDescription = "新建任务", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item { DailySummary() }
            item {
                TimerCard(
                    selectedTask = selectedTask,
                    elapsedSeconds = elapsedSeconds,
                    isRunning = isRunning,
                    onSelectTask = { selectedTask = it },
                    onToggle = { isRunning = !isRunning },
                    onStop = {
                        if (elapsedSeconds > 0) {
                            entries.add(0, TimeEntry(selectedTask, formatDuration(elapsedSeconds), "刚刚"))
                        }
                        elapsedSeconds = 0
                        isRunning = false
                    }
                )
            }
            item {
                Text("最近记录", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            items(entries) { entry -> EntryRow(entry) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DailySummary() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("今日专注", color = Color(0xFFCFE6D7), fontSize = 14.sp)
                Text("03:42:16", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("比昨日多 28 分钟", color = Color(0xFFCFE6D7), fontSize = 13.sp)
            }
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFFB8DEC5),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun TimerCard(
    selectedTask: String,
    elapsedSeconds: Long,
    isRunning: Boolean,
    onSelectTask: (String) -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    val tasks = listOf("产品设计", "阅读与学习", "团队会议")
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    OutlinedButton(
                        onClick = { onSelectTask(task) },
                        modifier = Modifier.weight(1f),
                        contentPadding = ButtonDefaults.ContentPadding,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedTask == task) Color(0xFFE4F1E8) else Color.Transparent,
                            contentColor = if (selectedTask == task) PrimaryGreen else MutedInk
                        )
                    ) { Text(task, fontSize = 11.sp, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(formatDuration(elapsedSeconds), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (isRunning) "正在记录 · $selectedTask" else "准备记录 · $selectedTask", color = MutedInk, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(64.dp)
                        .background(PrimaryGreen, CircleShape)
                ) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isRunning) "暂停" else "开始", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onStop, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = "停止", tint = MutedInk, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: TimeEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(entry.time, color = MutedInk, fontSize = 13.sp)
        }
        Text(entry.duration, color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
}

@Preview(showBackground = true)
@Composable
private fun TimeRecorderPreview() {
    TimeRecorderApp()
}
