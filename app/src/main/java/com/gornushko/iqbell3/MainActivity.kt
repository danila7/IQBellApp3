package com.gornushko.iqbell3

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MainActivity : AppCompatActivity() {

    private lateinit var mqttAndroidClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar?)
        connect()
    }

    private fun connect() {
        mqttAndroidClient = MqttAndroidClient(
            applicationContext,
            "tcp://tailor.cloudmqtt.com:18846", "AndroidClient"
        )
        val options = MqttConnectOptions()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1
        options.isCleanSession = false
        options.userName = "ujqjrjyr"
        options.password = "iCmu4k4g5RaB".toCharArray()
        try {
            val token = mqttAndroidClient.connect(options)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.i("Connection", "success ")
                    //connectionStatus = true
                    // Give your callback on connection established here
                    subscribe("i")
                    //subscribe("d")
                    receiveMessages()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    //connectionStatus = false
                    Log.i("Connection", "failure")
                    // Give your callback on connection failure here
                    exception.printStackTrace()
                }
            }
        } catch (e: MqttException) {
            // Give your callback on connection failure here
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String) {
        val qos = 0 // Mention your qos value
        try {
            mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // Give your callback on Subscription here
                    Log.d("mqtt", "subscriptionSucceed")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give your subscription failure callback here
                    Log.d("mqtt", "subscriptionFailed")
                }
            })
        } catch (e: MqttException) {
            // Give your subscription failure callback here
        }
    }

    fun receiveMessages() {
        mqttAndroidClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                //connectionStatus = false
                // Give your callback on failure here
                Log.d("connectionToMQTT", "connection Lost!")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    val data = String(message.payload, charset("US-ASCII"))
                    // data is the desired received message
                    // Give your callback on message received here
                    Log.d("messageReceived", data)
                } catch (e: Exception) {
                    // Give your callback on error here
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // Acknowledgement on delivery complete
            }
        })
    }

    fun publish(topic: String, data: String) {
        val encodedPayload: ByteArray
        try {
            encodedPayload = data.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            message.qos = 1
            message.isRetained = false
            mqttAndroidClient.publish(topic, message)
        } catch (e: Exception) {
            // Give Callback on error here
        } catch (e: MqttException) {
            // Give Callback on error here
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    private fun disconnect() {
        try {
            mqttAndroidClient.disconnect()
        } catch (e: MqttException) {
        }
    }

}
