package me.yhh.mediaprojectiontest

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import me.yhh.mediaprojectiontest.dao.Resolution
import kotlin.properties.Delegates

class MediaProjectionActivity : Activity() {
    private lateinit var surfaceView: SurfaceView
    private var surface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var density by Delegates.notNull<Int>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            i?.let {
                when (i.getStringExtra("type")) {
                    ScreenSharingService.BROADCAST_NEED_TO_REQUEST -> {
                        val i = i.getParcelableExtra<Intent>("data-intent")
                        i?.let {
                            startActivityForResult(
                                i,
                                ScreenSharingService.MEDIA_PROJECTION_REQ_CODE
                            )
                        }
                    }
                    else -> {
                    }
                }
            }
        }

    }

    companion object {
        fun getIntent(c: Context): Intent {
            return Intent(c, MediaProjectionActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_projection)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        density = metrics.densityDpi
        surfaceView = findViewById(R.id.surfaceView)
        surface = surfaceView.holder.surface
        surfaceView.holder.addCallback(surfaceCallbacks)
        surfaceView.layoutParams.width =
            Resolution.getDefaultResolutionsList.first().width// metrics.widthPixels
        surfaceView.layoutParams.height =
            Resolution.getDefaultResolutionsList.first().height//metrics.heightPixels
        startService(Intent(this, ScreenSharingService::class.java))
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(ScreenSharingService.ACTION_FILTER))

        startService(Intent(this, ScreenSharingService::class.java))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        surfaceView.holder.removeCallback(surfaceCallbacks)
        surface?.release()
        surface = null
        stopSharingScreen()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ScreenSharingService.MEDIA_PROJECTION_REQ_CODE -> {
                if (resultCode == RESULT_OK) {
                    startService(
                        ScreenSharingService.onGrantedIntent(
                            this,
                            resultCode,
                            data!!,
                            density,
                            surface!!
                        )
                    )
                } else {
                    startService(ScreenSharingService.onDeniedIntent(this))
                }
            }
        }
    }

    fun stopSharingScreen() {
        startService(ScreenSharingService.stopIntent(this))
    }


    private val surfaceCallbacks = object : SurfaceHolder.Callback {
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            startService(
                ScreenSharingService.resizeIntent(
                    this@MediaProjectionActivity,
                    Resolution(width, height),
                    density
                )
            )
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            surface = holder.surface
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            stopSharingScreen()
        }
    }
}