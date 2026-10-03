package com.chn.mqttwidget

import org.json.JSONObject

data class WidgetConfig(
    val icon: String = IconCatalog.DEFAULT_ICON,
    val label: String = "",
    val host: String = "",
    val port: Int = 1883,
    val tls: Boolean = false,
    val username: String = "",
    val password: String = "",
    val clientId: String = "",
    val topic: String = "",
    val qos: Int = 0,
    val retain: Boolean = false,
    val payload: String = "",
) {
    val isValid: Boolean
        get() = host.isNotBlank() && topic.isNotBlank()

    val serverUri: String
        get() = "${if (tls) "ssl" else "tcp"}://$host:$port"

    fun toJson(): JSONObject = JSONObject().apply {
        put("icon", icon)
        put("label", label)
        put("host", host)
        put("port", port)
        put("tls", tls)
        put("username", username)
        put("password", password)
        put("clientId", clientId)
        put("topic", topic)
        put("qos", qos)
        put("retain", retain)
        put("payload", payload)
    }

    companion object {
        private val SCHEME_PREFIX = Regex("^(?i)(tcp|ssl|mqtt|mqtts|ws|wss)://")

        /**
         * Normalizes what users typically paste into the host field:
         * "broker", "broker:8883", "tcp://broker:8883", "mqtt://broker/mqtt".
         * Returns the bare host and the port found in the input (or fallbackPort).
         */
        fun parseBrokerInput(raw: String, fallbackPort: Int): Pair<String, Int> {
            var value = raw.trim().replaceFirst(SCHEME_PREFIX, "")
            value = value.substringBefore('/')

            if (value.startsWith("[")) { // IPv6 literal: [fe80::1]:8883
                val end = value.indexOf(']')
                val host = if (end == -1) "" else value.substring(1, end)
                val rest = if (end == -1) "" else value.substring(end + 1)
                return host to (rest.removePrefix(":").toIntOrNull() ?: fallbackPort)
            }

            // "host:port", but only when unambiguous (bare IPv6 has several colons)
            if (value.count { it == ':' } == 1) {
                val idx = value.indexOf(':')
                val port = value.substring(idx + 1).toIntOrNull()
                if (port != null) return value.substring(0, idx) to port
            }
            return value to fallbackPort
        }

        fun fromJson(json: JSONObject): WidgetConfig = WidgetConfig(
            icon = json.optString("icon", IconCatalog.DEFAULT_ICON),
            label = json.optString("label", ""),
            host = json.optString("host", ""),
            port = json.optInt("port", 1883),
            tls = json.optBoolean("tls", false),
            username = json.optString("username", ""),
            password = json.optString("password", ""),
            clientId = json.optString("clientId", ""),
            topic = json.optString("topic", ""),
            qos = json.optInt("qos", 0),
            retain = json.optBoolean("retain", false),
            payload = json.optString("payload", ""),
        )
    }
}

