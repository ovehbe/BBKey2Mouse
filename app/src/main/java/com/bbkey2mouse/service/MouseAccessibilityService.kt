package com.bbkey2mouse.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.bbkey2mouse.overlay.CursorOverlay
import com.bbkey2mouse.overlay.FloatingControlsOverlay
import com.bbkey2mouse.overlay.TouchInterceptorOverlay
import com.bbkey2mouse.util.Preferences

class MouseAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "MouseAccessibilityService"
        
        const val ACTION_TOGGLE_MOUSE = "com.bbkey2mouse.ACTION_TOGGLE_MOUSE"
        const val ACTION_ENABLE_MOUSE = "com.bbkey2mouse.ACTION_ENABLE_MOUSE"
        const val ACTION_DISABLE_MOUSE = "com.bbkey2mouse.ACTION_DISABLE_MOUSE"
        const val ACTION_UPDATE_SETTINGS = "com.bbkey2mouse.ACTION_UPDATE_SETTINGS"
        const val ACTION_QUERY_STATE = "com.bbkey2mouse.ACTION_QUERY_STATE"
        const val ACTION_STATE_CHANGED = "com.bbkey2mouse.ACTION_STATE_CHANGED"
        const val EXTRA_MOUSE_ENABLED = "mouse_enabled"
        
        private var instance: MouseAccessibilityService? = null
        
        fun isRunning(): Boolean = instance != null
        
        fun isMouseModeActive(): Boolean = instance?.isMouseModeEnabled ?: false
    }
    
    private var cursorOverlay: CursorOverlay? = null
    private var touchInterceptor: TouchInterceptorOverlay? = null
    private var floatingControls: FloatingControlsOverlay? = null
    private var isMouseModeEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private var inputInterceptor: InputEventInterceptor? = null
    
    // Key tracking for long press detection
    private var toggleKeyDownTime = 0L
    private var isToggleKeyDown = false
    private val longPressThreshold = 500L // 500ms for long press
    
    // Scroll mode (hold key to scroll instead of move cursor)
    private var isScrollModeActive = false
    
    // Track last touch position for delta calculation
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isTouching = false
    
    // Vibrator
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_TOGGLE_MOUSE -> toggleMouseMode()
                ACTION_ENABLE_MOUSE -> if (!isMouseModeEnabled) enableMouseMode().also { 
                    Preferences.setMouseModeEnabled(this@MouseAccessibilityService, true)
                    broadcastState() 
                }
                ACTION_DISABLE_MOUSE -> if (isMouseModeEnabled) disableMouseMode().also {
                    Preferences.setMouseModeEnabled(this@MouseAccessibilityService, false)
                    broadcastState()
                }
                ACTION_UPDATE_SETTINGS -> {
                    cursorOverlay?.updateSettings()
                    floatingControls?.updateSettings()
                }
                ACTION_QUERY_STATE -> broadcastState()
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        Log.d(TAG, "Accessibility service connected")
        
        // Programmatically configure the service for key event filtering
        val info = serviceInfo
        info.flags = info.flags or 
            android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
            android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
        
        Log.d(TAG, "Service flags set: ${info.flags}, canRetrieveWindowContent=${info.flags and android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS != 0}")
        Log.d(TAG, "Key event filtering enabled: ${info.flags and android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS != 0}")
        
        // Initialize input interceptor for keyboard touch events
        inputInterceptor = InputEventInterceptor(this) { event ->
            onKeyboardTouch(event)
        }
        inputInterceptor?.start()
        
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_TOGGLE_MOUSE)
            addAction(ACTION_ENABLE_MOUSE)
            addAction(ACTION_DISABLE_MOUSE)
            addAction(ACTION_UPDATE_SETTINGS)
            addAction(ACTION_QUERY_STATE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        
        // Restore previous state
        if (Preferences.isMouseModeEnabled(this)) {
            enableMouseMode()
        }
        
        broadcastState()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
        instance = null
        inputInterceptor?.stop()
        inputInterceptor = null
        disableMouseMode()
        touchInterceptor = null
        floatingControls = null
        cursorOverlay = null
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
    
    /**
     * Handle generic motion events from the keyboard touchpad.
     * This is the key method for intercepting BB Key2 keyboard touch gestures.
     */
    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Check if this is from the keyboard touchpad and mouse mode is enabled
        val interceptor = inputInterceptor ?: return false
        return interceptor.processMotionEvent(event)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Log accessibility events to understand what we receive
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || 
                it.eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_START ||
                it.eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_END) {
                Log.d(TAG, "onAccessibilityEvent: type=${it.eventType}, source=${it.source}")
            }
        }
    }
    
    /**
     * Override to intercept gestures when touch exploration is enabled
     */
    override fun onGesture(gestureId: Int): Boolean {
        Log.d(TAG, "onGesture: gestureId=$gestureId, mouseMode=$isMouseModeEnabled")
        return super.onGesture(gestureId)
    }
    
    override fun onInterrupt() {
        // Service interrupted
    }
    
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Log IMMEDIATELY to verify this method is being called
        Log.i(TAG, ">>> onKeyEvent CALLED! keyCode=${event.keyCode}, action=${event.action}")
        
        val keyCode = event.keyCode
        val action = event.action
        
        // Log ALL key events for debugging
        Log.d(TAG, "onKeyEvent: keyCode=$keyCode (${KeyEvent.keyCodeToString(keyCode)}), action=$action, device=${event.device?.name}")
        
        // Get configured keycodes
        val toggleKey = Preferences.getToggleKeycode(this)
        val clickKey = Preferences.getClickKeycode(this)
        val rightClickKey = Preferences.getRightClickKeycode(this)
        val scrollModeKey = Preferences.getScrollModeKeycode(this)
        
        // Handle toggle key (long press)
        if (keyCode == toggleKey) {
            Log.d(TAG, "Toggle key detected! action=$action, isToggleKeyDown=$isToggleKeyDown")
            when (action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isToggleKeyDown) {
                        isToggleKeyDown = true
                        toggleKeyDownTime = System.currentTimeMillis()
                        Log.d(TAG, "Toggle key DOWN - scheduling long press check")
                        
                        // Schedule long press check
                        handler.postDelayed({
                            Log.d(TAG, "Long press check: isToggleKeyDown=$isToggleKeyDown, elapsed=${System.currentTimeMillis() - toggleKeyDownTime}")
                            if (isToggleKeyDown && 
                                System.currentTimeMillis() - toggleKeyDownTime >= longPressThreshold) {
                                Log.d(TAG, "Long press confirmed! Toggling mouse mode")
                                toggleMouseMode()
                                vibrate(100)
                            }
                        }, longPressThreshold)
                    }
                    return isMouseModeEnabled // Consume if mouse mode active
                }
                KeyEvent.ACTION_UP -> {
                    val wasLongPress = System.currentTimeMillis() - toggleKeyDownTime >= longPressThreshold
                    Log.d(TAG, "Toggle key UP - wasLongPress=$wasLongPress")
                    isToggleKeyDown = false
                    handler.removeCallbacksAndMessages(null)
                    
                    // If it was a long press, we already toggled. If short press and mouse mode, 
                    // let it pass through
                    return wasLongPress || isMouseModeEnabled
                }
            }
        }
        
        // Only handle other keys if mouse mode is enabled
        if (!isMouseModeEnabled) {
            return super.onKeyEvent(event)
        }
        
        // Handle click key
        if (keyCode == clickKey) {
            if (action == KeyEvent.ACTION_DOWN) {
                performClick()
                return true
            }
            return true
        }
        
        // Handle right click key
        if (keyCode == rightClickKey) {
            if (action == KeyEvent.ACTION_DOWN) {
                performLongPress()
                return true
            }
            return true
        }
        
        // Handle scroll mode key
        if (keyCode == scrollModeKey) {
            isScrollModeActive = action == KeyEvent.ACTION_DOWN
            return true
        }
        
        return super.onKeyEvent(event)
    }
    
    /**
     * Called from TouchInterceptor when keyboard touch events are detected.
     * The BB Key2 keyboard touch surface generates generic motion events that
     * we intercept and translate to cursor movement.
     */
    fun onKeyboardTouch(event: MotionEvent): Boolean {
        if (!isMouseModeEnabled) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isTouching = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTouching) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y
                    
                    if (isScrollModeActive) {
                        // TODO: Implement scroll gesture
                        performScroll(deltaX, deltaY)
                    } else {
                        cursorOverlay?.moveCursor(deltaX, deltaY)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                return true
            }
        }
        return false
    }
    
    private fun toggleMouseMode() {
        if (isMouseModeEnabled) {
            disableMouseMode()
        } else {
            enableMouseMode()
        }
        Preferences.setMouseModeEnabled(this, isMouseModeEnabled)
        broadcastState()
    }
    
    private fun enableMouseMode() {
        Log.d(TAG, "Enabling mouse mode")
        
        // Create and show cursor overlay
        if (cursorOverlay == null) {
            cursorOverlay = CursorOverlay(this)
        }
        cursorOverlay?.show()
        
        // Create and show floating controls (since key events don't work on BB Key2)
        if (floatingControls == null) {
            floatingControls = FloatingControlsOverlay(
                context = this,
                onLeftClick = { performClick() },
                onRightClick = { performLongPress() },
                onToggle = { toggleMouseMode() },
                onDrag = { dx, dy -> cursorOverlay?.moveCursor(dx, dy) }
            )
        }
        floatingControls?.show()
        
        // Create and show touch interceptor (for keyboard touchpad if it works)
        if (touchInterceptor == null) {
            touchInterceptor = TouchInterceptorOverlay(this) { event ->
                onKeyboardTouch(event)
            }
        }
        touchInterceptor?.show()
        
        isMouseModeEnabled = true
        vibrate(50) // Feedback that mouse mode is enabled
    }
    
    private fun disableMouseMode() {
        Log.d(TAG, "Disabling mouse mode")
        cursorOverlay?.hide()
        floatingControls?.hide()
        touchInterceptor?.hide()
        isMouseModeEnabled = false
        vibrate(30) // Feedback that mouse mode is disabled
    }
    
    private fun performClick() {
        val (x, y) = cursorOverlay?.getCursorPosition() ?: return
        
        val path = Path()
        path.moveTo(x, y)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (Preferences.getVibrateOnClick(this@MouseAccessibilityService)) {
                    vibrate(30)
                }
            }
        }, null)
    }
    
    private fun performLongPress() {
        val (x, y) = cursorOverlay?.getCursorPosition() ?: return
        
        val path = Path()
        path.moveTo(x, y)
        
        // Long press gesture (500ms)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 550))
            .build()
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (Preferences.getVibrateOnClick(this@MouseAccessibilityService)) {
                    vibrate(50)
                }
            }
        }, null)
    }
    
    private fun performScroll(deltaX: Float, deltaY: Float) {
        val (x, y) = cursorOverlay?.getCursorPosition() ?: return
        
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x - deltaX * 3, y - deltaY * 3)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        dispatchGesture(gesture, null, null)
    }
    
    private fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
    
    private fun broadcastState() {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_MOUSE_ENABLED, isMouseModeEnabled)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}

