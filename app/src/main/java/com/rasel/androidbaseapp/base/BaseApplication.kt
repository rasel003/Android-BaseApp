package com.rasel.androidbaseapp.base

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.multidex.MultiDexApplication
import com.rasel.androidbaseapp.BuildConfig
import com.rasel.androidbaseapp.cache.preferences.PreferenceProvider
import com.rasel.androidbaseapp.core.theme.ThemeUtils
import com.rasel.androidbaseapp.ui.MainActivity
import com.rasel.androidbaseapp.util.CHANNEL_ID_RECEIVED_LEAVE_REQUEST
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.rasel.androidbaseapp.R
import javax.inject.Inject


const val DEEPLINK_DOMAIN = "rasel003.github.io"

@HiltAndroidApp
class BaseApplication : MultiDexApplication() {

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var preferencesHelper: PreferenceProvider

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        themeUtils.setNightMode(preferencesHelper.isNightMode)

        // used this logger throughout the app for logging
        /*Logger.addLogAdapter(object : AndroidLogAdapter() {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })*/

        createNotificationChannel()
        createNotificationChannel2()
        showNotification()
    }

    private fun createNotificationChannel() {

        val name = "Channel Leave Request" //getString(R.string.channel_received_leave_request)
        val descriptionText =
            "Channel for Received Leave Request" //getString(R.string.channel_leave_request_description)
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel =
            NotificationChannel(CHANNEL_ID_RECEIVED_LEAVE_REQUEST, name, importance).apply {
                description = descriptionText
            }
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.enableLights(true)
        channel.lightColor = Color.RED
        // channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, )
        channel.enableVibration(true)
        channel.setShowBadge(true)


        // Register the channel with the system
        //   val notificationManager: NotificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)

        val notificationManager = getSystemService(
            NotificationManager::class.java
        ) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotificationChannel2() {
        val channel = NotificationChannel(
            "channel_id",
            "channel_name",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService<NotificationManager>()!!
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification() {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            data = "https://$DEEPLINK_DOMAIN/rasel/dhaka/white-house-guest-house-712313?keyword=White+House+Guest+House&checkInDate=2025-04-28&checkOutDate=2025-04-29&numberOfRooms=1&numberOfAdultTravelers=2&numberOfAdultChildren=0&productType=1&countryCode=BD&latitude=23.80112&longitude=90.41445&searchId=712313&searchType=property".toUri()

        }
        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(activityIntent)
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Launched!")
            .setContentText("Tap to open deep link")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService<NotificationManager>()!!
        notificationManager.notify(1, notification)
    }

}