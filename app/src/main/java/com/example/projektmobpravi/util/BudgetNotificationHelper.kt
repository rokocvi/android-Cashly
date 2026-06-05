package com.example.projektmobpravi.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "budget_alerts"
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Budget obavijesti",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Upozorenja kad se priblizis ili prekoracis budget"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun notifyNearLimit(category: String, percent: Int, limit: Double) {
        sendNotification(
            id = category.hashCode() and 0x7FFFFFFF,
            title = "Blizu budgeta — $category",
            text = "Iskoristio si $percent% od €%.2f".format(limit)
        )
    }

    fun notifyOverLimit(category: String, spent: Double, limit: Double) {
        sendNotification(
            id = (category.hashCode() and 0x7FFFFFFF) + 10_000,
            title = "Prekoracen budget — $category",
            text = "Potrosio si €%.2f od €%.2f".format(spent, limit)
        )
    }

    private fun sendNotification(id: Int, title: String, text: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
