package com.example.home_chores_automation_app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.home_chores_automation_app.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleReminderWorker()
    }

    /**
     * Schedules a periodic background job that fires every hour.
     * WorkManager ensures it runs even when the app is closed or the phone restarts.
     * KEEP policy means if the job is already scheduled, it won't be re-registered.
     */
    private fun scheduleReminderWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "task_reminder_job",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
