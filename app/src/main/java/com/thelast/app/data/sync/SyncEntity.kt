package com.thelast.app.data.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sync_entities", indices = [Index(value = ["cloudId"], unique = true)])
data class SyncEntity(
    @PrimaryKey val key: String,
    val kind: String,
    val localId: Long,
    val cloudId: String,
    val version: Int = 0,
    val dirty: Boolean = true,
    val deleted: Boolean = false,
    val mutationId: String,
    val payload: String,
    val frozenOperation: String? = null,
    val conflict: String? = null,
)

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_entities") suspend fun all(): List<SyncEntity>
    @Query("SELECT * FROM sync_entities WHERE `key` = :key") suspend fun get(key: String): SyncEntity?
    @Query("SELECT * FROM sync_entities WHERE cloudId = :id") suspend fun byCloudId(id: String): SyncEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(entity: SyncEntity)
    @Query("SELECT COUNT(*) FROM sync_entities WHERE dirty = 1 AND conflict IS NULL") fun observePending(): Flow<Int>
    @Query("SELECT COUNT(*) FROM sync_entities WHERE conflict IS NOT NULL") fun observeConflicts(): Flow<Int>
}
