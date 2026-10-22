package com.thelast.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thelast.app.data.sync.SyncDao
import com.thelast.app.data.sync.SyncEntity
import org.json.JSONObject
import java.util.UUID
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventTypeEntity

@Database(
    entities = [
        EventTypeEntity::class,
        EventRecordEntity::class,
        SyncEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TheLastDatabase : RoomDatabase() {
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventRecordDao(): EventRecordDao
    abstract fun syncDao(): SyncDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_entities (`key` TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, localId INTEGER NOT NULL, cloudId TEXT NOT NULL, version INTEGER NOT NULL, dirty INTEGER NOT NULL, deleted INTEGER NOT NULL, mutationId TEXT NOT NULL, payload TEXT NOT NULL, frozenOperation TEXT, conflict TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_entities_cloudId ON sync_entities(cloudId)")
                val parentIds = mutableMapOf<Long, String>()
                db.query("SELECT * FROM event_types").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                        val cloudId = UUID.randomUUID().toString(); parentIds[id] = cloudId
                        val data = JSONObject()
                        listOf("name", "icon", "category").forEach { key ->
                            val col = cursor.getColumnIndexOrThrow(key); data.put(key, if (cursor.isNull(col)) JSONObject.NULL else cursor.getString(col))
                        }
                        listOf("color", "sortOrder", "createdAt", "updatedAt").forEach { key -> data.put(key, cursor.getLong(cursor.getColumnIndexOrThrow(key))) }
                        data.put("isArchived", cursor.getInt(cursor.getColumnIndexOrThrow("isArchived")) != 0)
                        db.execSQL("INSERT INTO sync_entities VALUES (?,?,?,?,0,1,0,?,?,NULL,NULL)", arrayOf("type:$id", "type", id, cloudId, UUID.randomUUID().toString(), data.toString()))
                    }
                }
                db.query("SELECT * FROM event_records").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                        val parent = parentIds[cursor.getLong(cursor.getColumnIndexOrThrow("eventTypeId"))] ?: error("Missing event type")
                        val data = JSONObject().put("eventTypeId", parent)
                        listOf("note", "valueUnit").forEach { key -> val col = cursor.getColumnIndexOrThrow(key); data.put(key, if (cursor.isNull(col)) JSONObject.NULL else cursor.getString(col)) }
                        val valueCol = cursor.getColumnIndexOrThrow("value"); data.put("value", if (cursor.isNull(valueCol)) JSONObject.NULL else cursor.getDouble(valueCol))
                        listOf("occurredAt", "createdAt", "updatedAt").forEach { key -> data.put(key, cursor.getLong(cursor.getColumnIndexOrThrow(key))) }
                        db.execSQL("INSERT INTO sync_entities VALUES (?,?,?,?,0,1,0,?,?,NULL,NULL)", arrayOf("record:$id", "record", id, UUID.randomUUID().toString(), UUID.randomUUID().toString(), data.toString()))
                    }
                }
            }
        }
        @Volatile
        private var instance: TheLastDatabase? = null

        fun getDatabase(context: Context): TheLastDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TheLastDatabase::class.java,
                    "thelast.db",
                ).addMigrations(MIGRATION_1_2).build().also {
                    instance = it
                }
            }
        }
    }
}
