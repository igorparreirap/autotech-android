package com.autotech.android

import android.app.Application
import android.content.pm.PackageManager
import androidx.room.InvalidationTracker
import com.autotech.android.work.AlertScheduler
import com.autotech.android.work.ReminderNotifier
import com.autotech.android.data.local.AutoTechDatabase
import com.google.android.libraries.places.api.Places

class AutoTechApplication : Application() {

    val database: AutoTechDatabase by lazy { AutoTechDatabase.build(this) }

    lateinit var graph: AppGraph
        private set

    private val sessionPrefs by lazy { getSharedPreferences("autotech_session", MODE_PRIVATE) }

    var currentUserId: Long? = null
        private set

    fun restoreSession() {
        val id = sessionPrefs.getLong("userId", -1L)
        currentUserId = if (id >= 0) id else null
    }

    fun setSession(userId: Long?) {
        currentUserId = userId
        sessionPrefs.edit().apply {
            if (userId == null) remove("userId") else putLong("userId", userId)
            apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        graph = AppGraph(this)
        initPlaces()
        initReminders()
    }

    /**
     * RF-06: canal de notificações, verificação periódica (avisos por tempo, com o app fechado)
     * e verificação imediata sempre que odômetro, manutenções, ciclos ou abastecimentos mudam
     * (avisos por quilometragem).
     */
    private fun initReminders() {
        ReminderNotifier.createChannel(this)
        AlertScheduler.enqueuePeriodic(this)
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(
                arrayOf("vehicles", "maintenance_records", "maintenance_cycles", "fuel_records")
            ) {
                override fun onInvalidated(tables: Set<String>) {
                    AlertScheduler.checkNow(this@AutoTechApplication)
                }
            }
        )
    }

    private fun initPlaces() {
        if (Places.isInitialized()) return
        val apiKey = packageManager
            .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
        Places.initializeWithNewPlacesApiEnabled(this, apiKey)
    }

    companion object {
        lateinit var instance: AutoTechApplication
            private set
    }
}
