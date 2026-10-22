package com.thelast.app

import android.app.Application
import com.thelast.app.data.database.TheLastDatabase
import com.thelast.app.data.datastore.SettingsStore
import com.thelast.app.data.repository.EventRepository
import com.thelast.app.data.sync.CloudSyncManager
import android.net.ConnectivityManager
import android.net.Network

class TheLastApplication : Application() {
    val updates by lazy { com.thelast.app.data.update.AppUpdateManager(this) }
    val cloudSync: CloudSyncManager by lazy { CloudSyncManager(this, database) }
    override fun onCreate() {
        super.onCreate()
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { cloudSync.schedule() }
        })
    }
    val database: TheLastDatabase by lazy {
        TheLastDatabase.getDatabase(this)
    }

    val settingsStore: SettingsStore by lazy {
        SettingsStore(this)
    }

    val eventRepository: EventRepository by lazy {
        EventRepository(
            database = database,
            eventTypeDao = database.eventTypeDao(),
            eventRecordDao = database.eventRecordDao(),
            onChanged = { cloudSync.schedule() },
        )
    }
}
