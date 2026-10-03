package com.chn.mqttwidget

import android.content.Context
import androidx.annotation.StringRes
import org.eclipse.paho.client.mqttv3.MqttException

/**
 * Turns a publish failure into text a user can act on.
 *
 * Paho reports connect failures as a bare reason code ("MQTT reason 5"), which
 * is meaningless in a notification; the codes are mapped to readable text here.
 */
object PublishErrors {

    @StringRes
    fun reasonText(reasonCode: Int): Int = when (reasonCode) {
        MqttException.REASON_CODE_FAILED_AUTHENTICATION.toInt() -> R.string.reason_bad_credentials
        MqttException.REASON_CODE_NOT_AUTHORIZED.toInt() -> R.string.reason_not_authorized
        MqttException.REASON_CODE_BROKER_UNAVAILABLE.toInt() -> R.string.reason_broker_unavailable
        MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION.toInt() -> R.string.reason_protocol_version
        MqttException.REASON_CODE_INVALID_CLIENT_ID.toInt() -> R.string.reason_client_id
        MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt() -> R.string.reason_timeout
        MqttException.REASON_CODE_CONNECTION_LOST.toInt() -> R.string.reason_connection_lost
        MqttException.REASON_CODE_SSL_CONFIG_ERROR.toInt() -> R.string.reason_tls
        MqttException.REASON_CODE_SERVER_CONNECT_ERROR.toInt() -> R.string.reason_tls
        else -> R.string.reason_unknown
    }

    fun describe(error: Throwable, context: Context): String = when (error) {
        is MqttException -> buildString {
            append(context.getString(reasonText(error.reasonCode)))
            append(" (reason ")
            append(error.reasonCode)
            append(')')
            error.cause?.message?.takeIf { it.isNotBlank() }?.let {
                append(": ")
                append(it)
            }
        }
        else -> error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
    }
}
