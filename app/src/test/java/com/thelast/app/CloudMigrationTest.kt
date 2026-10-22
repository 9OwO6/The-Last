package com.thelast.app

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.thelast.app.data.database.TheLastDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CloudMigrationTest {
    @Test fun versionOneUpgradePreservesRecordsAndAssignsStableIds() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("migration-test.db")
        val schema = JSONObject(javaClass.classLoader!!.getResourceAsStream("com.thelast.app.data.database.TheLastDatabase/1.json")!!.bufferedReader().readText()).getJSONObject("database")
        val path = context.getDatabasePath("migration-test.db").also { it.parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { raw ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))
            raw.execSQL("INSERT INTO event_types VALUES (7,'使用手机 B','history',4283328337,NULL,1000,2000,0,0)")
            raw.execSQL("INSERT INTO event_records VALUES (11,7,3000,'旧记录',3.5,'次',3000,4000)")
            raw.version = 1
        }
        fun open() = Room.databaseBuilder(context, TheLastDatabase::class.java, "migration-test.db")
            .allowMainThreadQueries().addMigrations(TheLastDatabase.MIGRATION_1_2).build()
        val db = open()
        assertEquals("旧记录", db.eventRecordDao().getRecord(11)!!.note)
        assertEquals(3.5, db.eventRecordDao().getRecord(11)!!.value!!, 0.0)
        val typeId = db.syncDao().get("type:7")!!.cloudId
        val recordId = db.syncDao().get("record:11")!!.cloudId
        assertEquals(typeId, JSONObject(db.syncDao().get("record:11")!!.payload).getString("eventTypeId"))
        assertEquals(2, db.syncDao().all().size)
        db.close()
        val reopened = open()
        assertEquals(typeId, reopened.syncDao().get("type:7")!!.cloudId)
        assertEquals(recordId, reopened.syncDao().get("record:11")!!.cloudId)
        reopened.close(); context.deleteDatabase("migration-test.db")
        Unit
    }
}
