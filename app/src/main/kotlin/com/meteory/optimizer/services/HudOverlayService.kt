package com.meteory.optimizer.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.meteory.optimizer.MeteoryApplication
import com.meteory.optimizer.R
import com.meteory.optimizer.ui.components.HudContent
import com.meteory.optimizer.ui.theme.MeteoryTheme
import com.meteory.optimizer.utils.SystemInfo
import kotlinx.coroutines.*

class HudOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var hudView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var fpsValue   by mutableStateOf("--")
    private var cpuValue   by mutableStateOf("0%")
    private var ramValue   by mutableStateOf("--")
    private var tempValue  by mutableStateOf("--°C")
    private var pingValue  by mutableStateOf("--ms")
    private var opacity    by mutableStateOf(0.85f)
    private var isVisible  by mutableStateOf(true)

    private var lastX = 40
    private var lastY = 100

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        createHudView()
        startMetricsCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_OPACITY -> {
                opacity = intent.getFloatExtra(EXTRA_OPACITY, 0.85f)
            }
            ACTION_TOGGLE_VISIBILITY -> {
                isVisible = !isVisible
                hudView?.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        hudView?.let { windowManager.removeView(it) }
        hudView = null
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, MeteoryApplication.CHANNEL_HUD)
            .setContentTitle(getString(R.string.notification_hud_title))
            .setContentText(getString(R.string.notification_hud_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun createHudView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastX; y = lastY
        }

        val owner = createLifecycleOwner()
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )
            setContent {
                MeteoryTheme {
                    HudContent(
                        fps       = fpsValue,
                        cpu       = cpuValue,
                        ram       = ramValue,
                        temp      = tempValue,
                        ping      = pingValue,
                        opacity   = opacity,
                        visible   = isVisible,
                        onToggle  = { isVisible = !isVisible }
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(owner)
        composeView.setViewTreeSavedStateRegistryOwner(owner)

        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(composeView, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(composeView, params)
        hudView = composeView
    }

    private fun startMetricsCollection() {
        scope.launch {
            while (isActive) {
                cpuValue  = "${SystemInfo.getCpuUsagePercent()}%"
                val ram   = SystemInfo.getRamInfo(this@HudOverlayService)
                ramValue  = "${ram.availableMb}MB libre"
                val temp  = SystemInfo.getCpuTemperature()
                tempValue = "${"%.1f".format(temp)}°C"
                delay(1000)
            }
        }
    }

    private fun createLifecycleOwner(): LifecycleOwner {
        val registry = LifecycleRegistry(object : LifecycleOwner {
            override val lifecycle: Lifecycle get() = LifecycleRegistry(this)
        })
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return object : LifecycleOwner, SavedStateRegistryOwner {
            private val savedStateController = SavedStateRegistryController.create(this)
            override val lifecycle: Lifecycle = registry
            override val savedStateRegistry: SavedStateRegistry =
                savedStateController.savedStateRegistry
            init { savedStateController.performAttach() }
        }
    }

    companion object {
        private const val NOTIF_ID = 1001
        const val ACTION_SET_OPACITY       = "ACTION_SET_OPACITY"
        const val ACTION_TOGGLE_VISIBILITY = "ACTION_TOGGLE_VISIBILITY"
        const val ACTION_STOP              = "ACTION_STOP"
        const val EXTRA_OPACITY            = "EXTRA_OPACITY"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, HudOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, HudOverlayService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }
}
