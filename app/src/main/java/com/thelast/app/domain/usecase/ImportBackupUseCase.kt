package com.thelast.app.domain.usecase

import com.thelast.app.data.repository.EventRepository
import com.thelast.app.domain.model.BackupImportDraft
import com.thelast.app.domain.model.BackupImportPreview

class ImportBackupUseCase(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(draft: BackupImportDraft): BackupImportPreview {
        repository.importBackup(draft)
        return draft.preview
    }
}
