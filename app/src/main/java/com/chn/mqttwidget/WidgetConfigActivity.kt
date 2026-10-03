package com.chn.mqttwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedIcon = "ic_power"
    private var existedBefore = false

    private lateinit var iconGrid: GridLayout
    private lateinit var labelInput: EditText
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var tlsInput: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var userInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var clientIdInput: EditText
    private lateinit var topicInput: EditText
    private lateinit var qosInput: AutoCompleteTextView
    private lateinit var retainInput: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var payloadInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        bindViews()
        existedBefore = WidgetConfigStore.load(this, appWidgetId) != null
        populateFrom(WidgetConfigStore.load(this, appWidgetId))
        buildIconGrid()
        setupQos()

        // Publish results are reported via notification (+ toast), which needs
        // POST_NOTIFICATIONS on Android 13+. Without it the system also drops
        // background toasts, and the user would never see why a publish failed.
        NotificationHelper.requestPermission(this)

        findViewById<Button>(R.id.btn_save).setOnClickListener { save() }
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { cancel() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cancel()
        })
    }

    private fun bindViews() {
        iconGrid = findViewById(R.id.icon_grid)
        labelInput = findViewById(R.id.input_label)
        hostInput = findViewById(R.id.input_host)
        portInput = findViewById(R.id.input_port)
        tlsInput = findViewById(R.id.input_tls)
        userInput = findViewById(R.id.input_user)
        passwordInput = findViewById(R.id.input_password)
        clientIdInput = findViewById(R.id.input_client_id)
        topicInput = findViewById(R.id.input_topic)
        qosInput = findViewById(R.id.input_qos)
        retainInput = findViewById(R.id.input_retain)
        payloadInput = findViewById(R.id.input_payload)
    }

    private fun populateFrom(config: WidgetConfig?) {
        val cfg = config ?: WidgetConfig()
        selectedIcon = cfg.icon
        labelInput.setText(cfg.label)
        hostInput.setText(cfg.host)
        portInput.setText(cfg.port.toString())
        tlsInput.isChecked = cfg.tls
        userInput.setText(cfg.username)
        passwordInput.setText(cfg.password)
        clientIdInput.setText(cfg.clientId)
        topicInput.setText(cfg.topic)
        qosInput.setText(cfg.qos.toString(), false)
        retainInput.isChecked = cfg.retain
        payloadInput.setText(cfg.payload)
    }

    private fun buildIconGrid() {
        iconGrid.removeAllViews()
        val size = resources.getDimensionPixelSize(R.dimen.icon_choice_size)
        val margin = resources.getDimensionPixelSize(R.dimen.icon_choice_margin)

        for ((name, resId) in MqttWidgetProvider.ICONS) {
            val image = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(margin, margin, margin, margin)
                setImageResource(resId)
                contentDescription = name
                background = ContextCompat.getDrawable(
                    this@WidgetConfigActivity,
                    if (name == selectedIcon) R.drawable.icon_selected_bg else R.drawable.icon_bg,
                )
                setOnClickListener {
                    selectedIcon = name
                    buildIconGrid()
                }
            }
            iconGrid.addView(image)
        }
    }

    private fun setupQos() {
        val adapter = ArrayAdapter(this, R.layout.list_item, listOf("0", "1", "2"))
        qosInput.setAdapter(adapter)
    }

    private fun save() {
        val rawHost = hostInput.text.toString().trim()
        val defaultPort = portInput.text.toString().toIntOrNull() ?: 1883
        val (host, port) = WidgetConfig.parseBrokerInput(rawHost, defaultPort)
        val topic = topicInput.text.toString().trim()

        if (rawHost.isEmpty() || host.isEmpty()) {
            hostInput.error = getString(R.string.error_host_required)
            return
        }
        if (topic.isEmpty()) {
            topicInput.error = getString(R.string.error_topic_required)
            return
        }

        val config = WidgetConfig(
            icon = selectedIcon,
            label = labelInput.text.toString().trim(),
            host = host,
            port = port.coerceIn(1, 65535),
            tls = tlsInput.isChecked,
            username = userInput.text.toString().trim(),
            password = passwordInput.text.toString(),
            clientId = clientIdInput.text.toString().trim(),
            topic = topic,
            qos = qosInput.text.toString().toIntOrNull() ?: 0,
            retain = retainInput.isChecked,
            payload = payloadInput.text.toString(),
        )

        WidgetConfigStore.save(this, appWidgetId, config)

        val manager = AppWidgetManager.getInstance(this)
        MqttWidgetProvider.updateWidget(this, manager, appWidgetId)

        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        if (!existedBefore) {
            WidgetConfigStore.delete(this, appWidgetId)
        }
        finish()
    }
}
