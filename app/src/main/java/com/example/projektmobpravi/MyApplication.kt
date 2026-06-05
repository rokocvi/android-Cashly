package com.example.projektmobpravi

import android.app.Application
import com.example.projektmobpravi.util.BudgetNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var notificationHelper: BudgetNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()
    }
}