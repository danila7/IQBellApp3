package com.gornushko.iqbell3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.util.Log.d
import android.util.Log.e
import androidx.core.app.NotificationCompat
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


@ExperimentalUnsignedTypes
class IQService : Service() {

    private var pi: PendingIntent? = null
    private lateinit var mqttAndroidClient: MqttAndroidClient
    private val options = MqttConnectOptions()
    private var startInfo: ByteArray? = null
    private var startData: ByteArray? = null
    private var startConnection = false

    companion object {
        private const val NOTIFICATIONS_CHANNEL = "new_channel"
        const val PENDING_INTENT = "pint"
        const val ACTION = "action"
        const val START = 1
        const val CONNECTING = 5
        const val NO_INTERNET = 12
        const val CONNECTED = 8
        const val STOP_SERVICE = 404
        const val NEW_PENDING_INTENT = 301
        const val SEND_DATA = 22
        const val INFO = "inf"
        const val DATA = "dt"
        const val NEW_INFO = 51
        const val NEW_DATA = 47
        const val TAG = "MqttClient"
        const val TOPIC = "tp"
        const val ERROR = 321
        const val OK = 123
    }

    override fun onCreate() {
        super.onCreate()
        val loginData = getSharedPreferences(ConnectActivity.LOGIN_DATA, Context.MODE_PRIVATE)
        mqttAndroidClient = MqttAndroidClient(
            this, loginData.getString(
                ConnectActivity.URL_N_PORT,
                ""
            ), MqttClient.generateClientId()
        )
        mqttAndroidClient.registerResources(this)
        options.apply {
            mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1
            isCleanSession = false
            userName = loginData.getString(ConnectActivity.USERNAME, "")
            password = loginData.getString(ConnectActivity.PASSWORD, "")!!.toCharArray()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent!!.getIntExtra(ACTION, START)) {
            START -> {
                pi = intent.getParcelableExtra(PENDING_INTENT)
                connect()
                updateNotification(getString(R.string.connecting), true)
            }
            NEW_PENDING_INTENT -> {
                pi = intent.getParcelableExtra(PENDING_INTENT)
            }
            STOP_SERVICE -> {
                disconnect()
                stopForeground(true)
                stopSelf()
            }
            SEND_DATA -> {
                sendData(intent.getByteArrayExtra(DATA)!!, intent.getStringExtra(TOPIC)!!)
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(text: String, progress: Boolean) {
        createNotificationsChannel()
        val builder = NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL)
            .setSmallIcon(R.drawable.ic_bell_outline)
            .setContentTitle(getText(R.string.iq_status))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null, AudioManager.STREAM_RING)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        if (progress) builder.setProgress(0, 0, true)
        val notification = builder.build()
        startForeground(1, notification)
    }

    private fun createNotificationsChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATIONS_CHANNEL, "Connection status",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "IQBell Status"
        channel.enableVibration(false)
        channel.enableLights(false)
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun sendData(
        data: ByteArray,
        topic: String
    ) { //an universal function for receiving/sending data
        d(TAG, "sending ${data.size} bytes of data")
        try {
            val message = MqttMessage(data)
            message.qos = 0
            message.isRetained = false
            mqttAndroidClient.publish(topic, message)
            pi?.send(OK)
        } catch (e: Exception) {
            pi?.send(ERROR)
            // Give Callback on error here
        } catch (e: MqttException) {
            pi?.send(ERROR)
            // Give Callback on error here
        }
    }

    private fun connect() {
        try {
            e(TAG, "trying to connect...")
            val token = mqttAndroidClient.connect(options)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    d("Connection", "success ")
                    pi?.send(CONNECTING)
                    startConnection = true
                    subscribe("d")
                    subscribe("i")
                    receiveMessages()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    e("Connection", "failure")
                    pi?.send(NO_INTERNET)
                    updateNotification(getString(R.string.no_connection), true)
                }
            }
        } catch (e: MqttException) {
            // Give your callback on connection failure here
            e(TAG, "MQTT Exception!")
        }
    }

    private fun subscribe(topic: String) {
        val qos = 0 // Mention your qos value
        try {
            mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // Give your callback on Subscription here
                    d(TAG, "subscription succeed")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give your subscription failure callback here
                    e(TAG, "subscription failed")
                }
            })
        } catch (e: MqttException) {
            // Give your subscription failure callback here
        }
    }


    private fun receiveMessages() {
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable) {
                e(TAG, "connection lost")
                pi?.send(NO_INTERNET)
                updateNotification(getString(R.string.no_connection), true)

            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                d(TAG, "connection complete callback!!!")
                pi?.send(CONNECTING)
                updateNotification(getString(R.string.loading), true)
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    val data = message.payload
                    if (topic == "i" && startInfo == null) {
                        startInfo = data
                    }
                    if (topic == "d" && startData == null) {
                        startData = data
                    }
                    if (startData != null && startInfo != null && startConnection) {
                        pi?.send(
                            applicationContext,
                            CONNECTED,
                            Intent().putExtra(INFO, startInfo).putExtra(DATA, startData)
                        )
                        updateNotification(getString(R.string.connected), false)
                        startConnection = false
                    } else when (topic) {
                        "i" -> pi?.send(applicationContext, NEW_INFO, Intent().putExtra(DATA, data))
                        "d" -> pi?.send(applicationContext, NEW_DATA, Intent().putExtra(DATA, data))
                    }
                } catch (e: Exception) {
                    // Give your callback on error here
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })
    }


    private fun disconnect() {
        d(TAG, "disconnecting...")
        mqttAndroidClient.unregisterResources()
        mqttAndroidClient.close()
    }
}