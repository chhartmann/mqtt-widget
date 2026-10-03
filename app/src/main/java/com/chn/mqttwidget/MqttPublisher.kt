package com.chn.mqttwidget

import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

object MqttPublisher {

    private const val TAG = "MqttPublisher"

    fun publish(config: WidgetConfig) {
        require(config.isValid) { "Invalid config: host and topic are required" }

        val clientId = config.clientId.ifBlank {
            "mqttwidget-${UUID.randomUUID().toString().take(8)}"
        }

        val client = MqttClient(config.serverUri, clientId, MemoryPersistence())
        try {
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = false
                isCleanSession = true
                connectionTimeout = 10
                if (config.username.isNotBlank()) {
                    userName = config.username
                    if (config.password.isNotEmpty()) {
                        password = config.password.toCharArray()
                    }
                }
            }

            Log.d(TAG, "Connecting to ${config.serverUri}")
            client.connect(options)

            val message = MqttMessage(config.payload.toByteArray(Charsets.UTF_8)).apply {
                qos = config.qos
                isRetained = config.retain
            }
            Log.d(
                TAG,
                "Publishing ${message.payload.size} bytes to '${config.topic}' " +
                    "(qos=${config.qos}, retained=${config.retain})",
            )
            client.publish(config.topic, message)
            Log.d(TAG, "Publish acknowledged by the MQTT client")
        } catch (e: MqttException) {
            Log.e(
                TAG,
                "MQTT operation failed (reason=${e.reasonCode}, cause=${e.cause?.message})",
                e,
            )
            throw e
        } finally {
            runCatching {
                if (client.isConnected) client.disconnect()
            }
            runCatching { client.close() }
        }
    }
}
