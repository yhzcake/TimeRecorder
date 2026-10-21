package com.timerecorder.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class TimeRecorderWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        when (intent.action) {
            ACTION_MARK -> addMark(context, intent.getStringExtra(EXTRA_NOTE).orEmpty())
            ACTION_NEW -> saveAndStartNext(context)
            ACTION_OPEN -> {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        if (intent.action in setOf(ACTION_MARK, ACTION_NEW, ACTION_OPEN)) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) intArrayOf(widgetId)
            else manager.getAppWidgetIds(ComponentName(context, TimeRecorderWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        private const val ACTION_MARK = "com.timerecorder.app.widget.MARK"
        private const val ACTION_NEW = "com.timerecorder.app.widget.NEW"
        private const val ACTION_OPEN = "com.timerecorder.app.widget.OPEN"
        private const val EXTRA_NOTE = "note"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TimeRecorderWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val store = WorkspaceStore(context)
            val active = store.readDraft() ?: store.readCases().maxByOrNull { it.created }
            val views = RemoteViews(context.packageName, R.layout.time_recorder_widget)
            views.setTextViewText(R.id.widget_case_title, active?.title ?: "时间记录")
            val entries = active?.marks.orEmpty().takeLast(4).asReversed().joinToString("\n") { mark ->
                "${formatTime(mark.at)}  ${mark.note}"
            }.ifBlank { "暂无时间点" }
            views.setTextViewText(R.id.widget_entries, entries)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent(context, id, ACTION_OPEN))
            views.setOnClickPendingIntent(R.id.widget_case_title, pendingIntent(context, id, ACTION_OPEN))
            views.setOnClickPendingIntent(R.id.widget_new_case, pendingIntent(context, id, ACTION_NEW))
            listOf(
                R.id.widget_note_flight to "飞行",
                R.id.widget_note_start to "开始",
                R.id.widget_note_end to "结束",
                R.id.widget_note_issue to "问题"
            ).forEach { (viewId, note) ->
                val intent = Intent(context, TimeRecorderWidgetProvider::class.java)
                    .setAction(ACTION_MARK)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(EXTRA_NOTE, note)
                views.setOnClickPendingIntent(viewId, PendingIntent.getBroadcast(context, id * 100 + viewId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            val newIntent = Intent(context, TimeRecorderWidgetProvider::class.java)
                .setAction(ACTION_NEW)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            views.setOnClickPendingIntent(R.id.widget_new_case, PendingIntent.getBroadcast(context, id * 100 + R.id.widget_new_case, newIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, views)
        }

        private fun pendingIntent(context: Context, id: Int, action: String): PendingIntent {
            val intent = if (action == ACTION_OPEN) Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else Intent(context, TimeRecorderWidgetProvider::class.java).setAction(action)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            return if (action == ACTION_OPEN) PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            else PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun addMark(context: Context, note: String) {
            val store = WorkspaceStore(context)
            val now = System.currentTimeMillis()
            val draft = store.readDraft() ?: UseCase(UUID.randomUUID().toString(), defaultTitle(now), now, emptyList())
            store.writeDraft(draft.copy(marks = draft.marks + Mark(UUID.randomUUID().toString(), now, note)))
        }

        private fun saveAndStartNext(context: Context) {
            val store = WorkspaceStore(context)
            val now = System.currentTimeMillis()
            val active = store.readDraft()
            if (active != null) {
                val cases = store.readCases().toMutableList()
                val index = cases.indexOfFirst { it.id == active.id }
                if (index < 0) cases.add(active) else cases[index] = active
                store.writeCases(cases)
            }
            store.writeDraft(UseCase(UUID.randomUUID().toString(), defaultTitle(now), now, emptyList()))
        }

        private fun defaultTitle(epoch: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))

        private fun formatTime(epoch: Long): String = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))
    }
}
