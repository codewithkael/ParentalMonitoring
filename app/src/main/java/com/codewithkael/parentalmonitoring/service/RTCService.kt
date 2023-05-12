package com.codewithkael.parentalmonitoring.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.codewithkael.parentalmonitoring.MainRepository
import com.codewithkael.parentalmonitoring.R
import com.codewithkael.parentalmonitoring.ui.MainActivity
import com.codewithkael.parentalmonitoring.utils.DataModel
import com.codewithkael.parentalmonitoring.utils.DataModelType
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AndroidEntryPoint
class RTCService : Service(), MainRepository.Listener {

    companion object {
        var isServiceRunning = false
    }

    @Inject
    lateinit var mainRepository: MainRepository
    private var surfaceViewRenderer: SurfaceViewRenderer? = null
    private var username: String? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: NotificationCompat.Builder


    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        mainRepository.listener = this
        surfaceViewRenderer = SurfaceViewRenderer(this)
        notificationManager = getSystemService(
            NotificationManager::class.java
        )

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            if (intent.action == "StartIntent") {
                if (!isServiceRunning) {
                    isServiceRunning = true
                    val username = intent.getStringExtra("username")
                    val password = intent.getStringExtra("password")
                    if (username != null && password != null) {
                        this.username = username
                        mainRepository.init(username, password, surfaceViewRenderer)
                    }
                    startServiceWithNotification()

                }

            } else if (intent.action == "StopIntent") {
                stopMyService()
            }
        }
        return START_STICKY
    }

    private fun stopMyService() {
        if (isServiceRunning) {
            isServiceRunning = false
        }
        mainRepository.onDestroy()
        stopForeground(true)
        stopSelf()
        notificationManager.cancelAll()
    }

    private fun startServiceWithNotification() {
        val intent = Intent(this, RTCBroadcastReceiver::class.java).apply {
            action = "action_exit"
            putExtra("type", "exit")
        }

        val pendingIntent =
            PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "chennel1", "forgroundNotif", NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "no sound"
            notificationChannel.setSound(null, null)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)

            notificationManager.createNotificationChannel(notificationChannel)

            val notificationIntent = Intent(applicationContext, MainActivity::class.java)
            notificationIntent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            val notificationPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val icon = BitmapFactory.decodeResource(
                applicationContext.resources, R.mipmap.ic_launcher
            )
            notification = NotificationCompat.Builder(
                this, "chennel1"
            ).setDefaults(0).setSmallIcon(R.mipmap.ic_launcher).setLargeIcon(icon).setSound(null)
                .setVibrate(null).setOnlyAlertOnce(true)
                .addAction(R.id.notifExit, "exit", pendingIntent)
                .setContentIntent(notificationPendingIntent)


            startForeground(1, notification.build())

        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onNewMessage(data: DataModel) {
        when (data.type) {
            DataModelType.SignInSuccess -> {

            }

            DataModelType.UserExists -> {
                stopMyService()
            }
            DataModelType.EndCall -> {
                mainRepository.restartRtcClient()
            }
            else -> {}
        }
    }
}