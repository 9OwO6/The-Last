package com.thelast.app.domain.usecase

import com.thelast.app.data.repository.BackupSnapshot
import com.thelast.app.domain.model.BackupExport
import com.thelast.app.domain.model.BackupExportFormat
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BuildBackupExportUseCase {
    operator fun invoke(
        snapshot: BackupSnapshot,
        format: BackupExportFormat,
    ): BackupExport {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(Instant.ofEpochMilli(snapshot.exportedAt).atZone(ZoneId.systemDefault()))

        return when (format) {
            BackupExportFormat.Json -> BackupExport(
                fileName = "thelast-backup-$stamp.json",
                mimeType = "application/json",
                content = buildJson(snapshot),
            )

            BackupExportFormat.Csv -> BackupExport(
                fileName = "thelast-records-$stamp.csv",
                mimeType = "text/csv",
                content = buildCsv(snapshot),
            )
        }
    }

    private fun buildJson(snapshot: BackupSnapshot): String {
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("exportedAt", snapshot.exportedAt)
            .put(
                "eventTypes",
                JSONArray().apply {
                    snapshot.eventTypes.forEach { eventType ->
                        put(
                            JSONObject()
                                .put("id", eventType.id)
                                .put("name", eventType.name)
                                .put("icon", eventType.icon)
                                .put("color", eventType.color)
                                .put("category", eventType.category ?: JSONObject.NULL)
                                .put("createdAt", eventType.createdAt)
                                .put("updatedAt", eventType.updatedAt)
                                .put("isArchived", eventType.isArchived)
                                .put("sortOrder", eventType.sortOrder),
                        )
                    }
                },
            )
            .put(
                "eventRecords",
                JSONArray().apply {
                    snapshot.eventRecords.forEach { record ->
                        put(
                            JSONObject()
                                .put("id", record.id)
                                .put("eventTypeId", record.eventTypeId)
                                .put("occurredAt", record.occurredAt)
                                .put("note", record.note ?: JSONObject.NULL)
                                .put("value", record.value ?: JSONObject.NULL)
                                .put("valueUnit", record.valueUnit ?: JSONObject.NULL)
                                .put("createdAt", record.createdAt)
                                .put("updatedAt", record.updatedAt),
                        )
                    }
                },
            )

        return root.toString(2)
    }

    private fun buildCsv(snapshot: BackupSnapshot): String {
        val eventTypesById = snapshot.eventTypes.associateBy { it.id }
        val rows = mutableListOf(
            listOf(
                "event_type_id",
                "event_name",
                "event_category",
                "record_id",
                "occurred_at",
                "occurred_at_iso",
                "note",
            ),
        )

        snapshot.eventRecords.forEach { record ->
            val eventType = eventTypesById[record.eventTypeId]
            rows += listOf(
                record.eventTypeId.toString(),
                eventType?.name.orEmpty(),
                eventType?.category.orEmpty(),
                record.id.toString(),
                record.occurredAt.toString(),
                Instant.ofEpochMilli(record.occurredAt).toString(),
                record.note.orEmpty(),
            )
        }

        return rows.joinToString(separator = "\n") { row ->
            row.joinToString(separator = ",") { cell -> escapeCsv(cell) }
        } + "\n"
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
