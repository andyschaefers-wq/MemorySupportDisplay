package com.otheruncle.memorydisplay.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.otheruncle.memorydisplay.MainActivity
import com.otheruncle.memorydisplay.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service to handle incoming push notifications
 */
@AndroidEntryPoint
class MemoryDisplayMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenManager: PushTokenManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"
        
        // Notification channels
        const val CHANNEL_DEFAULT = "memory_display_default"
        const val CHANNEL_REMINDERS = "memory_display_reminders"
        const val CHANNEL_CARD_UPDATES = "memory_display_card_updates"
        
        // Notification types (from backend)
        const val TYPE_CARD_EDITED = "card_edited"
        const val TYPE_EVENT_REMINDER = "event_reminder"
        const val TYPE_DRIVER_REMINDER = "driver_reminder"
        const val TYPE_CALENDAR_REVIEW = "calendar_review"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        
        serviceScope.launch {
            pushTokenManager.onNewToken(token)
        }
    }

    /**
     * Called when a message is received from FCM
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification - Title: ${notification.title}, Body: ${notification.body}")
            showNotification(
                title = notification.title ?: "Memory Display",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
    }

    /**
     * Handle data-only messages (no notification payload)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        val title = data["title"] ?: "Memory Display"
        val body = data["body"] ?: ""
        
        showNotification(title, body, data)
    }

    /**
     * Display a notification to the user
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Determine which channel to use based on notification type
        val type = data["type"]
        val channelId = when (type) {
            TYPE_EVENT_REMINDER, TYPE_DRIVER_REMINDER -> CHANNEL_REMINDERS
            TYPE_CARD_EDITED -> CHANNEL_CARD_UPDATES
            else -> CHANNEL_DEFAULT
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Pass notification data to activity
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        // For longer messages, use big text style
        if (body.length > 50) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
        }

        // Show the notification with a unique ID
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Create notification channels (required for Android O+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications from Memory Display"
            }

            // Reminders channel (higher priority)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming appointments and events"
                enableVibration(true)
            }

            // Card updates channel
            val cardUpdatesChannel = NotificationChannel(
                CHANNEL_CARD_UPDATES,
                "Card Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when your cards are edited"
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, remindersChannel, cardUpdatesChannel)
            )
        }
    }
}
