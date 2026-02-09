package com.example.meloch.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.meloch.MainActivity
import com.example.meloch.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "meloch_budget_alerts"
        private const val BUDGET_LOW_NOTIFICATION_ID = 1001
        private const val BUDGET_LIMIT_NOTIFICATION_ID = 1002
        private const val BUDGET_RESET_NOTIFICATION_ID = 1003
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Alerts"
            val descriptionText = "Notifications about your budget status"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBudgetAlertNotification(remainingBudget: Double, currencySymbol: String) {
        // Create an intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Budget Alert")
            .setContentText("Your budget is running low! Only $currencySymbol${String.format("%,.2f", remainingBudget)} left.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Make the notification dismissible

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(BUDGET_LOW_NOTIFICATION_ID, builder.build())
        }
    }

    fun showBudgetZeroNotification(currencySymbol: String) {
        // Create an intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Budget Limit Reached")
            .setContentText("You have reached your budget limit for this month!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Make the notification dismissible

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(BUDGET_LIMIT_NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelBudgetAlertNotification() {
        with(NotificationManagerCompat.from(context)) {
            cancel(BUDGET_LOW_NOTIFICATION_ID)
        }
    }

    fun cancelBudgetZeroNotification() {
        with(NotificationManagerCompat.from(context)) {
            cancel(BUDGET_LIMIT_NOTIFICATION_ID)
        }
    }

    fun showBudgetResetNotification(budgetAmount: Double, currencySymbol: String) {
        // Create an intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Budget Reset")
            .setContentText("Your budget has been reset to ${currencySymbol}${String.format("%,.2f", budgetAmount)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Make the notification dismissible

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(BUDGET_RESET_NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelAllBudgetNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancel(BUDGET_LOW_NOTIFICATION_ID)
            cancel(BUDGET_LIMIT_NOTIFICATION_ID)
            cancel(BUDGET_RESET_NOTIFICATION_ID)
        }
    }
}
