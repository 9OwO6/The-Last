package com.thelast.app.domain.usecase

import com.thelast.app.domain.model.BackupImportDraft
import com.thelast.app.domain.model.BackupImportEventType
import com.thelast.app.domain.model.BackupImportPreview
import com.thelast.app.domain.model.BackupImportRecord
import org.json.JSONObject

class ParseBackupImportUseCase {
    operator fun invoke(content: String): BackupImportDraft {
        if (content.isBlank()) {
            throw IllegalArgumentException("备份文件是空的")
        }

        val root = runCatching { JSONObject(content) }
            .getOrElse { throw IllegalArgumentException("没有读懂这个备份文件") }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != 1) {
            throw IllegalArgumentException("这个备份版本暂时不支持")
        }

        val eventTypesJson = root.optJSONArray("eventTypes")
            ?: throw IllegalArgumentException("备份里没有事件列表")
        val eventRecordsJson = root.optJSONArray("eventRecords")
            ?: throw IllegalArgumentException("备份里没有记录列表")
        val exportedAt = root.optLong("exportedAt", 0L).takeIf { it > 0L } ?: System.currentTimeMillis()

        val eventTypes = buildList {
            for (index in 0 until eventTypesJson.length()) {
                val item = eventTypesJson.optJSONObject(index)
                    ?: throw IllegalArgumentException("事件数据格式不完整")
                val id = item.optLong("id", 0L)
                val name = item.optString("name").trim()
                if (id <= 0L || name.isBlank()) {
                    throw IllegalArgumentException("事件数据格式不完整")
                }
                add(
                    BackupImportEventType(
                        oldId = id,
                        name = name,
                        icon = item.optString("icon", "history"),
                        color = item.optLong("color", 0xFFFFA987),
                        category = item.nullableString("category"),
                        createdAt = item.optLong("createdAt", exportedAt),
                        updatedAt = item.optLong("updatedAt", exportedAt),
                        isArchived = item.optBoolean("isArchived", false),
                        sortOrder = item.optInt("sortOrder", 0),
                    ),
                )
            }
        }

        val eventIds = eventTypes.map { it.oldId }.toSet()
        val records = buildList {
            for (index in 0 until eventRecordsJson.length()) {
                val item = eventRecordsJson.optJSONObject(index)
                    ?: throw IllegalArgumentException("记录数据格式不完整")
                val eventTypeId = item.optLong("eventTypeId", 0L)
                if (eventTypeId !in eventIds) {
                    throw IllegalArgumentException("备份记录指向了不存在的事件")
                }
                add(
                    BackupImportRecord(
                        oldId = item.optLong("id", 0L),
                        oldEventTypeId = eventTypeId,
                        occurredAt = item.optLong("occurredAt", exportedAt),
                        note = item.nullableString("note"),
                        value = item.nullableDouble("value"),
                        valueUnit = item.nullableString("valueUnit"),
                        createdAt = item.optLong("createdAt", exportedAt),
                        updatedAt = item.optLong("updatedAt", exportedAt),
                    ),
                )
            }
        }

        return BackupImportDraft(
            preview = BackupImportPreview(
                eventTypeCount = eventTypes.size,
                recordCount = records.size,
                exportedAt = exportedAt,
            ),
            eventTypes = eventTypes,
            eventRecords = records,
        )
    }
}

private fun JSONObject.nullableString(name: String): String? {
    return if (isNull(name)) null else optString(name).trim().ifBlank { null }
}

private fun JSONObject.nullableDouble(name: String): Double? {
    return if (isNull(name)) null else optDouble(name)
}
