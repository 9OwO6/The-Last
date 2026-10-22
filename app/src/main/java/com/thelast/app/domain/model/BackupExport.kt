package com.thelast.app.domain.model

data class BackupExport(
    val fileName: String,
    val mimeType: String,
    val content: String,
)

enum class BackupExportFormat {
    Json,
    Csv,
}

data class BackupImportPreview(
    val eventTypeCount: Int,
    val recordCount: Int,
    val exportedAt: Long,
)

data class BackupImportDraft(
    val preview: BackupImportPreview,
    val eventTypes: List<BackupImportEventType>,
    val eventRecords: List<BackupImportRecord>,
)

data class BackupImportEventType(
    val oldId: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val sortOrder: Int,
)

data class BackupImportRecord(
    val oldId: Long,
    val oldEventTypeId: Long,
    val occurredAt: Long,
    val note: String?,
    val value: Double?,
    val valueUnit: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
