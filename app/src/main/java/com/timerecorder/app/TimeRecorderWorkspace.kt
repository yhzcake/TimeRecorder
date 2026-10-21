package com.timerecorder.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private val AppBg = Color(0xFFF7F8FA)
private val AppInk = Color(0xFF1A1C1E)
private val AppMuted = Color(0xFF626970)
private val AppGreen = Color(0xFF356859)
private val DefaultTypes = listOf(
    NoteType("飞行", 0xFFE3F2FD, 0xFF1565C0),
    NoteType("开始", 0xFFE8F5E9, 0xFF2E7D32),
    NoteType("结束", 0xFFFFF3E0, 0xFFEF6C00),
    NoteType("问题", 0xFFFCE4EC, 0xFFC2185B)
)

data class NoteType(val name: String, val bg: Long, val fg: Long)
data class Mark(val id: String, val at: Long, val note: String)
data class UseCase(val id: String, val title: String, val created: Long, val marks: List<Mark>)
private enum class Screen { HOME, EDIT, DETAIL }

class WorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("time_recorder_data", Context.MODE_PRIVATE)

    fun readCases(): List<UseCase> = runCatching {
        val array = JSONArray(prefs.getString("cases", "[]"))
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            UseCase(item.getString("id"), item.getString("title"), item.getLong("created"), item.getJSONArray("marks").toMarks())
        }
    }.getOrDefault(emptyList())

    fun writeCases(cases: List<UseCase>) {
        val array = JSONArray()
        cases.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title).put("created", item.created).put("marks", item.marks.toJson()))
        }
        prefs.edit().putString("cases", array.toString()).apply()
    }

    fun readDraft(): UseCase? = runCatching {
        val item = JSONObject(prefs.getString("draft", "{}"))
        if (!item.has("id")) null else UseCase(item.getString("id"), item.getString("title"), item.getLong("created"), item.getJSONArray("marks").toMarks())
    }.getOrNull()

    fun writeDraft(draft: UseCase?) {
        if (draft == null) prefs.edit().remove("draft").apply()
        else prefs.edit().putString("draft", JSONObject().put("id", draft.id).put("title", draft.title).put("created", draft.created).put("marks", draft.marks.toJson()).toString()).apply()
    }

    fun readTypes(): List<NoteType> = runCatching {
        val array = JSONArray(prefs.getString("types", ""))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            NoteType(obj.getString("name"), obj.getLong("bg"), obj.getLong("fg"))
        }
    }.getOrDefault(DefaultTypes)

    fun writeTypes(types: List<NoteType>) {
        val array = JSONArray()
        types.forEach { array.put(JSONObject().put("name", it.name).put("bg", it.bg).put("fg", it.fg)) }
        prefs.edit().putString("types", array.toString()).apply()
    }
}

private fun List<Mark>.toJson(): JSONArray = JSONArray().also { array ->
    forEach { array.put(JSONObject().put("id", it.id).put("at", it.at).put("note", it.note)) }
}

private fun JSONArray.toMarks(): List<Mark> = (0 until length()).map { i ->
    val obj = getJSONObject(i)
    Mark(obj.getString("id"), obj.getLong("at"), obj.getString("note"))
}

@Composable
fun TimeRecorderWorkspace() {
    val context = LocalContext.current
    val store = remember { WorkspaceStore(context) }
    val cases = remember { mutableStateListOf<UseCase>().apply { addAll(store.readCases()) } }
    val noteTypes = remember { mutableStateListOf<NoteType>().apply { addAll(store.readTypes()) } }
    var draft by remember { mutableStateOf(store.readDraft()) }
    var screen by remember { mutableStateOf(if (draft != null) Screen.EDIT else Screen.HOME) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var titleDialog by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var typeManager by remember { mutableStateOf(false) }
    var customTypeDialog by remember { mutableStateOf(false) }
    var deleteCaseId by remember { mutableStateOf<String?>(null) }
    var pendingExportId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun persistCases() = store.writeCases(cases.toList())
    fun persistDraft() = store.writeDraft(draft)
    fun goBack() {
        if (screen == Screen.EDIT) confirmDiscard = true
        else {
            screen = Screen.HOME
            selectedId = null
        }
    }

    BackHandler(screen != Screen.HOME) { goBack() }
    LaunchedEffect(draft, cases.size, noteTypes.size) {
        persistDraft()
        persistCases()
        store.writeTypes(noteTypes.toList())
    }
    LaunchedEffect(exportMessage) {
        exportMessage?.let { snackbarHostState.showSnackbar(it); exportMessage = null }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val target = cases.firstOrNull { it.id == pendingExportId }
        pendingExportId = null
        if (uri == null || target == null) {
            if (uri == null) exportMessage = "已取消导出"
        } else {
            val result = runCatching {
                val stream = context.contentResolver.openOutputStream(uri) ?: error("无法打开导出文件")
                stream.use {
                    OutputStreamWriter(it, Charsets.UTF_8).use { writer ->
                        writer.write("标题,创建时间,时间点,备注\r\n")
                        target.marks.forEach { mark ->
                            writer.write(listOf(target.title, formatDate(target.created), formatTime(mark.at), mark.note).joinToString(",") { csvCell(it) })
                            writer.write("\r\n")
                        }
                    }
                }
            }
            exportMessage = if (result.isSuccess) "导出成功" else "导出失败：${result.exceptionOrNull()?.message ?: "写入文件失败"}"
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    cases = cases.filter { it.title.contains(query, ignoreCase = true) },
                    query = query,
                    onQuery = { query = it },
                    onCreate = { titleDialog = true },
                    onOpen = { selectedId = it.id; screen = Screen.DETAIL },
                    onDeleteSelected = { id -> deleteCaseId = id }
                )
                Screen.EDIT -> draft?.let { current ->
                    EditScreen(
                        draft = current,
                        types = noteTypes,
                        onBack = ::goBack,
                        onSave = {
                            val index = cases.indexOfFirst { it.id == current.id }
                            if (index < 0) cases.add(current) else cases[index] = current
                            persistCases()
                            draft = null
                            persistDraft()
                            screen = Screen.HOME
                        },
                        onAddMark = { note -> draft = current.copy(marks = current.marks + Mark(UUID.randomUUID().toString(), System.currentTimeMillis(), note)); persistDraft() },
                        onUpdateMarks = { marks -> draft = current.copy(marks = marks); persistDraft() },
                        onManageTypes = { typeManager = true },
                        onAddType = { customTypeDialog = true }
                    )
                }
                Screen.DETAIL -> cases.firstOrNull { it.id == selectedId }?.let { item ->
                    DetailScreen(item, noteTypes, onBack = ::goBack, onExport = {
                        pendingExportId = item.id
                        exportLauncher.launch("${safeFileName(item.title)}_${fileDate(item.created)}.csv")
                    }, onUpdate = { marks ->
                        val index = cases.indexOfFirst { it.id == item.id }
                        if (index >= 0) cases[index] = item.copy(marks = marks)
                        persistCases()
                    })
                }
            }
            SnackbarHost(snackbarHostState, Modifier.fillMaxWidth())
        }
    }

    if (titleDialog) {
        TextInputDialog("新建用例", "用例标题", "确定", onDismiss = { titleDialog = false }) { title ->
            val created = System.currentTimeMillis()
            draft = UseCase(UUID.randomUUID().toString(), title, created, emptyList())
            persistDraft()
            screen = Screen.EDIT
            titleDialog = false
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃本次编辑？") },
            text = { Text("尚未保存的用例和时间点将被丢弃。") },
            confirmButton = { TextButton(onClick = { draft = null; persistDraft(); screen = Screen.HOME; confirmDiscard = false }) { Text("放弃") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") } }
        )
    }
    if (typeManager) {
        TypeManagerDialog(noteTypes, onDismiss = { typeManager = false }, onReorder = { reordered ->
            noteTypes.clear()
            noteTypes.addAll(reordered)
            store.writeTypes(noteTypes.toList())
        }, onAdd = { typeManager = false; customTypeDialog = true }, onDelete = { name ->
            noteTypes.removeAll { it.name == name }
            store.writeTypes(noteTypes.toList())
        })
    }
    if (customTypeDialog) {
        TextInputDialog("新增时间备注", "备注名称", "添加", onDismiss = { customTypeDialog = false }, validate = { name -> noteTypes.none { it.name.trim().equals(name.trim(), ignoreCase = true) } }) { name ->
            val palette = listOf(0xFFEDE7F6L to 0xFF5E35B1L, 0xFFE0F2F1L to 0xFF00796BL, 0xFFFFF9C4L to 0xFF827717L, 0xFFECEFF1L to 0xFF455A64L)
            val colors = palette[noteTypes.size % palette.size]
            noteTypes.add(NoteType(name, colors.first, colors.second))
            store.writeTypes(noteTypes.toList())
            customTypeDialog = false
            typeManager = screen == Screen.EDIT
        }
    }
    deleteCaseId?.let { id ->
        val item = cases.firstOrNull { it.id == id }
        if (item != null) AlertDialog(
            onDismissRequest = { deleteCaseId = null },
            title = { Text("删除用例？") },
            text = { Text("确定删除“${item.title}”及其所有时间点吗？") },
            confirmButton = { TextButton(onClick = { cases.removeAll { it.id == id }; persistCases(); deleteCaseId = null; selectedId = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteCaseId = null }) { Text("取消") } }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
private fun HomeScreen(cases: List<UseCase>, query: String, onQuery: (String) -> Unit, onCreate: () -> Unit, onOpen: (UseCase) -> Unit, onDeleteSelected: (String) -> Unit) {
    var contextMenuId by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = AppBg,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("搜索用例") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate, containerColor = AppGreen, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "新建用例")
            }
        }
    ) { padding ->
        if (cases.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (query.isBlank()) "还没有用例" else "没有匹配的用例", color = AppMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(cases, key = { _, item -> item.id }) { _, item ->
                    var expanded by remember(item.id) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().height(72.dp).combinedClickable(onClick = { onOpen(item) }, onLongClick = { contextMenuId = item.id; expanded = true }),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title, Modifier.weight(1f), color = AppInk, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(formatDate(item.created), color = AppMuted, fontSize = 12.sp)
                            DropdownMenu(expanded = expanded && contextMenuId == item.id, onDismissRequest = { expanded = false; contextMenuId = null }) {
                                DropdownMenuItem(text = { Text("删除") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { expanded = false; onDeleteSelected(item.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreen(draft: UseCase, types: List<NoteType>, onBack: () -> Unit, onSave: () -> Unit, onAddMark: (String) -> Unit, onUpdateMarks: (List<Mark>) -> Unit, onManageTypes: () -> Unit, onAddType: () -> Unit) {
    Scaffold(
        containerColor = AppBg,
        topBar = { TopAppBar(title = { Text(draft.title, maxLines = 1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, actions = { TextButton(onClick = onSave) { Text("保存") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg)) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(AppBg).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { type ->
                            Button(onClick = { onAddMark(type.name) }, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(type.bg), contentColor = Color(type.fg)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                                Text(type.name, maxLines = 1, fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = onAddType, modifier = Modifier.size(48.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.Add, "新增备注") }
                    }
                    TextButton(onClick = onManageTypes) { Icon(Icons.Default.SwapVert, "管理备注"); Spacer(Modifier.width(2.dp)); Text("管理") }
                }
            }
        }
    ) { padding -> MarkList(draft.marks, types, Modifier.fillMaxSize().padding(padding), onUpdateMarks) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(item: UseCase, types: List<NoteType>, onBack: () -> Unit, onExport: () -> Unit, onUpdate: (List<Mark>) -> Unit) {
    Scaffold(
        containerColor = AppBg,
        topBar = { TopAppBar(title = { Text(item.title, maxLines = 1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, actions = { IconButton(onClick = onExport) { Icon(Icons.Default.Download, "导出") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg)) }
    ) { padding -> MarkList(item.marks, types, Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), onUpdate) }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MarkList(marks: List<Mark>, types: List<NoteType>, modifier: Modifier, onUpdate: (List<Mark>) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var editMark by remember { mutableStateOf<Mark?>(null) }
    var deleteMark by remember { mutableStateOf<Mark?>(null) }
    val listState = rememberLazyListState()
    var previousMarkCount by remember { mutableStateOf(marks.size) }
    LaunchedEffect(marks.size) {
        if (marks.size > previousMarkCount && marks.isNotEmpty()) listState.animateScrollToItem(marks.lastIndex)
        previousMarkCount = marks.size
    }
    LazyColumn(modifier.padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(marks, key = { _, mark -> mark.id }) { index, mark ->
            val type = types.firstOrNull { it.name == mark.note } ?: NoteType(mark.note, 0xFFECEFF1, 0xFF455A64)
            Row(
                Modifier.fillMaxWidth().height(68.dp).combinedClickable(onClick = {}, onLongClick = { selected = mark.id; menuExpanded = true }).background(Color(type.bg), RoundedCornerShape(12.dp)).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTime(mark.at), Modifier.weight(0.42f), color = Color(type.fg), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(mark.note, Modifier.weight(0.58f), color = Color(type.fg), fontSize = 16.sp)
                DropdownMenu(expanded = menuExpanded && selected == mark.id, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("改变备注") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menuExpanded = false; editMark = mark })
                    DropdownMenuItem(text = { Text("上移") }, onClick = { menuExpanded = false; if (index > 0) onUpdate(marks.toMutableList().apply { add(index - 1, removeAt(index)) }) })
                    DropdownMenuItem(text = { Text("下移") }, onClick = { menuExpanded = false; if (index < marks.lastIndex) onUpdate(marks.toMutableList().apply { add(index + 1, removeAt(index)) }) })
                    DropdownMenuItem(text = { Text("删除") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuExpanded = false; deleteMark = mark })
                }
            }
        }
    }
    editMark?.let { mark ->
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { editMark = null }, title = { Text("改变备注") }, text = {
            Column {
                TextButton(onClick = { expanded = true }) { Text(mark.note) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    types.forEach { type -> DropdownMenuItem(text = { Text(type.name) }, onClick = { onUpdate(marks.map { if (it.id == mark.id) it.copy(note = type.name) else it }); expanded = false; editMark = null }) }
                }
            }
        }, confirmButton = { TextButton(onClick = { editMark = null }) { Text("完成") } })
    }
    deleteMark?.let { mark ->
        AlertDialog(onDismissRequest = { deleteMark = null }, title = { Text("删除时间点？") }, confirmButton = { TextButton(onClick = { onUpdate(marks.filterNot { it.id == mark.id }); deleteMark = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleteMark = null }) { Text("取消") } })
    }
}

@Composable
private fun TextInputDialog(title: String, label: String, confirm: String, onDismiss: () -> Unit, validate: (String) -> Boolean = { true }, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) },
        confirmButton = { TextButton(enabled = value.isNotBlank() && validate(value.trim()), onClick = { onConfirm(value.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TypeManagerDialog(types: List<NoteType>, onDismiss: () -> Unit, onReorder: (List<NoteType>) -> Unit, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    var localTypes by remember { mutableStateOf(types.toList()) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val currentTypes by rememberUpdatedState(localTypes)
    val rowHeightPx = with(LocalDensity.current) { 52.dp.toPx() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备注顺序") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(localTypes, key = { it.name }) { type ->
                    val index = localTypes.indexOf(type)
                    val isDragging = draggingIndex == index
                    val dragSteps = if (isDragging) (dragOffset / rowHeightPx).roundToInt() else 0
                    val rowOffset = if (isDragging) dragOffset - dragSteps * rowHeightPx else 0f
                    Column(Modifier.animateItem()) {
                        if (isDragging) Divider(color = AppGreen, thickness = 3.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(if (isDragging) Color(0xFFE8F2EC) else Color.Transparent, RoundedCornerShape(10.dp))
                                .offset { IntOffset(0, rowOffset.roundToInt()) }
                                .zIndex(if (isDragging) 1f else 0f)
                                .pointerInput(type) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingIndex = currentTypes.indexOf(type); dragStartIndex = currentTypes.indexOf(type); dragOffset = 0f },
                                        onDrag = { _, amount ->
                                            dragOffset += amount.y
                                            val current = currentTypes.indexOf(type)
                                            val start = dragStartIndex ?: current
                                            val target = (start + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, currentTypes.lastIndex)
                                            if (target != current) {
                                                val reordered = currentTypes.toMutableList()
                                                val moved = reordered.removeAt(current)
                                                reordered.add(target, moved)
                                                localTypes = reordered
                                                draggingIndex = target
                                                onReorder(reordered)
                                            }
                                        },
                                        onDragEnd = { draggingIndex = null; dragStartIndex = null; dragOffset = 0f },
                                        onDragCancel = { draggingIndex = null; dragStartIndex = null; dragOffset = 0f }
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.name, Modifier.weight(1f), textAlign = TextAlign.Start)
                            if (DefaultTypes.none { it.name == type.name }) IconButton(onClick = { localTypes = localTypes.filterNot { it.name == type.name }; onDelete(type.name) }) { Icon(Icons.Default.Delete, "删除备注") }
                            else Spacer(Modifier.size(48.dp))
                            IconButton(onClick = { }) { Icon(Icons.Default.DragHandle, "拖动排序", tint = AppMuted) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text("添加备注") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

private fun formatTime(epoch: Long): String = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault()).format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))
private fun formatDate(epoch: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()).format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))
private fun fileDate(epoch: Long): String = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.getDefault()).format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))
private fun safeFileName(value: String): String = value.replace(Regex("[\\\\/:*?\"<>|]"), "_")
private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
