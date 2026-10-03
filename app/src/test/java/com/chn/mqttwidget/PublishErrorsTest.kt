package com.chn.mqttwidget

import org.eclipse.paho.client.mqttv3.MqttException
import org.junit.Assert.assertEquals
import org.junit.Test

class PublishErrorsTest {

    @Test
    fun `mqtt reason codes map to readable text`() {
        assertEquals(
            R.string.reason_bad_credentials,
            PublishErrors.reasonText(MqttException.REASON_CODE_FAILED_AUTHENTICATION.toInt()),
        )
        assertEquals(
            R.string.reason_not_authorized,
            PublishErrors.reasonText(MqttException.REASON_CODE_NOT_AUTHORIZED.toInt()),
        )
        assertEquals(
            R.string.reason_broker_unavailable,
            PublishErrors.reasonText(MqttException.REASON_CODE_BROKER_UNAVAILABLE.toInt()),
        )
        assertEquals(
            R.string.reason_protocol_version,
            PublishErrors.reasonText(MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION.toInt()),
        )
        assertEquals(
            R.string.reason_client_id,
            PublishErrors.reasonText(MqttException.REASON_CODE_INVALID_CLIENT_ID.toInt()),
        )
        assertEquals(
            R.string.reason_timeout,
            PublishErrors.reasonText(MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt()),
        )
        assertEquals(
            R.string.reason_connection_lost,
            PublishErrors.reasonText(MqttException.REASON_CODE_CONNECTION_LOST.toInt()),
        )
        assertEquals(
            R.string.reason_tls,
            PublishErrors.reasonText(MqttException.REASON_CODE_SSL_CONFIG_ERROR.toInt()),
        )
        assertEquals(
            R.string.reason_tls,
            PublishErrors.reasonText(MqttException.REASON_CODE_SERVER_CONNECT_ERROR.toInt()),
        )
    }

    @Test
    fun `unknown reason codes fall back to generic text`() {
        assertEquals(
            R.string.reason_unknown,
            PublishErrors.reasonText(MqttException.REASON_CODE_MAX_INFLIGHT.toInt()),
        )
        assertEquals(R.string.reason_unknown, PublishErrors.reasonText(0))
    }
}
