-keepattributes SourceFile,LineNumberTable

# Paho must not be shrunk/renamed/merged by R8. Its LoggerFactory loads the logger
# implementation via Class.forName(...).newInstance() and casts it to
# org.eclipse.paho.client.mqttv3.logging.Logger. Class merging used to turn that cast
# into an unconditional ClassCastException, after which R8 deleted the whole
# connect/publish path of MqttPublisher as dead code - no MQTT message was ever sent.
-keep class org.eclipse.paho.** { *; }
-keep interface org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**
