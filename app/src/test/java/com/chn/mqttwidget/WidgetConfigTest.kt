package com.chn.mqttwidget

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigTest {

    @Test
    fun `defaults are invalid until host and topic set`() {
        assertFalse(WidgetConfig().isValid)
        assertFalse(WidgetConfig(host = "broker.local").isValid)
        assertFalse(WidgetConfig(topic = "home/light").isValid)
        assertTrue(WidgetConfig(host = "broker.local", topic = "home/light").isValid)
    }

    @Test
    fun `server uri uses tcp or ssl scheme`() {
        val plain = WidgetConfig(host = "10.0.0.2", port = 1883)
        assertEquals("tcp://10.0.0.2:1883", plain.serverUri)

        val secure = WidgetConfig(host = "broker.example.com", port = 8883, tls = true)
        assertEquals("ssl://broker.example.com:8883", secure.serverUri)
    }

    @Test
    fun `broker input strips pasted scheme path and port`() {
        val url = WidgetConfig.parseBrokerInput("tcp://broker.example.com:1884", 1883)
        assertEquals("broker.example.com", url.first)
        assertEquals(1884, url.second)

        val path = WidgetConfig.parseBrokerInput("mqtt://broker.local/mqtt", 1883)
        assertEquals("broker.local", path.first)
        assertEquals(1883, path.second)

        val plain = WidgetConfig.parseBrokerInput("10.0.0.2", 8883)
        assertEquals("10.0.0.2", plain.first)
        assertEquals(8883, plain.second)

        val hostPort = WidgetConfig.parseBrokerInput("broker:8883", 1883)
        assertEquals("broker", hostPort.first)
        assertEquals(8883, hostPort.second)

        val ipv6 = WidgetConfig.parseBrokerInput("[fe80::1]:8883", 1883)
        assertEquals("fe80::1", ipv6.first)
        assertEquals(8883, ipv6.second)

        val bareIpv6 = WidgetConfig.parseBrokerInput("fe80::1", 1883)
        assertEquals("fe80::1", bareIpv6.first)
        assertEquals(1883, bareIpv6.second)
    }

    @Test
    fun `json round trip preserves every field`() {
        val config = WidgetConfig(
            icon = "ic_bell",
            label = "Doorbell",
            host = "broker.local",
            port = 8883,
            tls = true,
            username = "user",
            password = "s3cret",
            clientId = "widget-1",
            topic = "home/doorbell",
            qos = 2,
            retain = true,
            payload = "ding",
        )

        val restored = WidgetConfig.fromJson(JSONObject(config.toJson().toString()))
        assertEquals(config, restored)
    }

    @Test
    fun `fromJson tolerates missing fields`() {
        val restored = WidgetConfig.fromJson(JSONObject("""{"host":"h","topic":"t"}"""))
        assertEquals("h", restored.host)
        assertEquals("t", restored.topic)
        assertEquals(1883, restored.port)
        assertEquals("ic_power", restored.icon)
        assertFalse(restored.tls)
        assertEquals(0, restored.qos)
    }

    @Test
    fun `fromJson on garbage returns null via store semantics`() {
        val config = runCatching { WidgetConfig.fromJson(JSONObject("not-json")) }.getOrNull()
        assertNull(config)
    }
}
