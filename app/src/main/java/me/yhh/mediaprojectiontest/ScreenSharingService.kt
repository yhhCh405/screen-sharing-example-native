/*
 * Copyright (c) 2021 by Ye Htet Hein. All rights reserved.
 */
package me.yhh.mediaprojectiontest

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import me.yhh.mediaprojectiontest.dao.Resolution
import kotlin.properties.Delegates

/**
 * @author Ye Htet Hein
 */
class ScreenSharingService() : Service() {
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var surface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val mediaProjectionCallback = YMediaProjectionCallback()

    companion object {
        private var resolution: Resolution = Resolution.getDefaultResolutionsList.first()
        private var density by Delegates.notNull<Int>()

        private const val NOTIFICATION_CHANNEL_ID = "chaosnewchannel4421"
        private const val NOTIFICATION_CHANNEL = "channel4421"
        private const val NOTIFICATION_ID = 1 // Must greater than 0
        private const val ACTION_RESIZE = "SSS_ACTION_RESIZE"
        private const val ACTION_STOP = "SSS_ACTION_STOP"

        const val MEDIA_PROJECTION_REQ_CODE = 2
        const val ACTION_FILTER = "SSS_ACTION_FILTER"
        const val ACTION_ON_GRANTED_MP = "ACTION_ON_ACTIVITY_RESULT"
        const val ACTION_ON_DENIED_MP = "ACTION_ON_DENIED_MP"
        const val BROADCAST_NEED_TO_REQUEST = "BROADCAST_NEED_TO_REQUEST"

        fun resizeIntent(context: Context, resolution: Resolution, density: Int?): Intent {
            return Intent(context, ScreenSharingService::class.java).setAction(ACTION_RESIZE)
                .putExtra("resolution", resolution)
                .putExtra("density", density ?: density)
        }

        fun onGrantedIntent(
            context: Context,
            resultCode: Int,
            data: Intent,
            density: Int,
            surface: Surface
        ): Intent {
            return Intent(context, ScreenSharingService::class.java).setAction(ACTION_ON_GRANTED_MP)
                .putExtra("resultCode", resultCode)
                .putExtra("data-intent", data).putExtra("density", density)
                .putExtra("surface", surface)
        }

        fun onDeniedIntent(context: Context): Intent =
            Intent(context, ScreenSharingService::class.java).setAction(ACTION_ON_DENIED_MP)


        fun stopIntent(context: Context): Intent =
            Intent(context, ScreenSharingService::class.java).setAction(ACTION_STOP)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        getStartForeground()
    }

    private fun createVirtualDisplay() {
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "Test SS", resolution.width, resolution.height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface!!, null, null
        )
    }

    private fun resizeVirtualDisplay(resolution: Resolution, density: Int?) {
        virtualDisplay?.resize(
            resolution.width, resolution.height,
            density ?: ScreenSharingService.density
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaProjectionManager == null) {
            mediaProjectionManager =
                getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }

        intent?.let { i ->
            when (i.action) {
                ACTION_ON_GRANTED_MP -> {
                    val resultCode = i.getIntExtra("resultCode", 0)
                    val mpIntent = i.getParcelableExtra<Intent>("data-intent")
                    surface = i.getParcelableExtra<Surface>("surface")
                    density = i.getIntExtra("density", 0)
                    mediaProjection = mediaProjectionManager!!.getMediaProjection(
                        resultCode, mpIntent!!
                    )
                    mediaProjection!!.registerCallback(mediaProjectionCallback, null)

                    createVirtualDisplay()
                }
                ACTION_ON_DENIED_MP -> {
                }
                ACTION_STOP -> {
                    if (virtualDisplay != null) {
                        virtualDisplay?.release()
                        virtualDisplay = null
                    } else {

                    }
                }
                ACTION_RESIZE -> {
                    val resolution = i.getParcelableExtra<Resolution>("resolution")
                    val density = i.getIntExtra("density", 0)
                    resizeVirtualDisplay(resolution!!, density)
                    if (mediaProjection != null) {
                        createVirtualDisplay()
                    } else {

                    }
                }
                else -> {
                    if (mediaProjection == null) {
                        val ssintent = Intent(ACTION_FILTER)
                        ssintent.putExtra("type", BROADCAST_NEED_TO_REQUEST)
                        ssintent.putExtra(
                            "data-intent",
                            mediaProjectionManager!!.createScreenCaptureIntent()
                        )
                        LocalBroadcastManager.getInstance(this).sendBroadcast(ssintent)
                    } else if (mediaProjection != null && virtualDisplay == null) {
                        createVirtualDisplay()
                    } else {

                    }
                }
            }

        }
        return START_STICKY
    }


    override fun onDestroy() {
        mediaProjection?.stop()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection = null
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val pendingIntent =
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notiChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager =
                getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notiChannel)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Screen Sharing")
            .setContentText("We are sharing this screen")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setTicker("This is ticker text")
            .setContentIntent(pendingIntent)
            .build()

    }

    private fun getStartForeground() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

    }

    private inner class YMediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            mediaProjection = null
            super.onStop()
        }
    }
}