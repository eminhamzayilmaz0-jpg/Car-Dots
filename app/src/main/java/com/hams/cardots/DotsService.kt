package com.hams.cardots

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.view.Display
import android.view.Surface
import android.view.WindowManager

class DotsService : Service(), SensorEventListener {

    companion object {
        const val ACTION_STOP = "com.hams.cardots.STOP"
        private const val CHANNEL = "dots"
        @Volatile var running = false
        var listener: (() -> Unit)? = null
    }

    private lateinit var wm: WindowManager
    private lateinit var sm: SensorManager
    private var view: DotsView? = null
    private var useLinear = false
    private val grav = FloatArray(2)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (view != null) return START_NOT_STICKY

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        startAsForeground()

        try {
            val v = DotsView(this)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            if (Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            wm.addView(v, lp)
            view = v
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        val lin = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val sensor = lin ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        useLinear = lin != null
        if (sensor != null) sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)

        running = true
        listener?.invoke()
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Noktalar", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, DotsService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_dots)
            .setContentTitle("Noktalar açık")
            .setContentIntent(openPi)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_dots), "Durdur", stopPi
                ).build()
            )
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun rotation(): Int =
        (getSystemService(DISPLAY_SERVICE) as DisplayManager)
            .getDisplay(Display.DEFAULT_DISPLAY).rotation

    override fun onSensorChanged(e: SensorEvent) {
        var x = e.values[0]
        var y = e.values[1]
        if (!useLinear) {
            // yerçekimini süz
            grav[0] = 0.9f * grav[0] + 0.1f * x
            grav[1] = 0.9f * grav[1] + 0.1f * y
            x -= grav[0]
            y -= grav[1]
        }
        val sx: Float
        val sy: Float
        when (rotation()) {
            Surface.ROTATION_90 -> { sx = y; sy = -x }
            Surface.ROTATION_180 -> { sx = -x; sy = -y }
            Surface.ROTATION_270 -> { sx = -y; sy = x }
            else -> { sx = x; sy = y }
        }
        val gain = getSharedPreferences("cardots", MODE_PRIVATE).getInt("gain", 50)
        view?.setAccel(sx, sy, 3f + gain * 0.12f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        if (::sm.isInitialized) sm.unregisterListener(this)
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
        running = false
        listener?.invoke()
        super.onDestroy()
    }
}
