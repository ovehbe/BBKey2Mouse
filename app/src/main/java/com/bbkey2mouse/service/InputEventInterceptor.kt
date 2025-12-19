package com.bbkey2mouse.service

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import java.lang.reflect.Method

/**
 * Intercepts input events from the BB Key2 keyboard touchpad.
 * 
 * This uses the InputManager's InputEventListener interface to receive
 * all input events before they're dispatched to apps.
 * 
 * Note: This requires the app to have proper permissions and may need
 * the accessibility service to function properly.
 */
class InputEventInterceptor(
    private val context: Context,
    private val onTouchEvent: (MotionEvent) -> Boolean
) {
    
    companion object {
        private const val TAG = "InputEventInterceptor"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    
    // Track keyboard device IDs
    private val keyboardDeviceIds = mutableSetOf<Int>()
    
    // Device listener for hot-plug events
    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            checkAndAddDevice(deviceId)
        }
        
        override fun onInputDeviceRemoved(deviceId: Int) {
            keyboardDeviceIds.remove(deviceId)
        }
        
        override fun onInputDeviceChanged(deviceId: Int) {
            checkAndAddDevice(deviceId)
        }
    }
    
    fun start() {
        // Find existing keyboard touch devices
        inputManager.inputDeviceIds.forEach { deviceId ->
            checkAndAddDevice(deviceId)
        }
        
        // Listen for device changes
        inputManager.registerInputDeviceListener(deviceListener, handler)
    }
    
    fun stop() {
        inputManager.unregisterInputDeviceListener(deviceListener)
        keyboardDeviceIds.clear()
    }
    
    private fun checkAndAddDevice(deviceId: Int) {
        val device = inputManager.getInputDevice(deviceId) ?: return
        if (KeyboardTouchService.isKeyboardTouchDevice(device)) {
            keyboardDeviceIds.add(deviceId)
        }
    }
    
    /**
     * Check if a motion event is from the keyboard touchpad
     */
    fun isKeyboardTouchEvent(event: MotionEvent): Boolean {
        return keyboardDeviceIds.contains(event.deviceId) ||
               KeyboardTouchService.isKeyboardTouchEvent(event)
    }
    
    /**
     * Process a generic motion event.
     * Returns true if the event was consumed (mouse mode active).
     */
    fun processMotionEvent(event: MotionEvent): Boolean {
        if (!isKeyboardTouchEvent(event)) {
            return false
        }
        return onTouchEvent(event)
    }
    
    /**
     * Get all connected keyboard touch devices
     */
    fun getConnectedDevices(): List<InputDevice> {
        return keyboardDeviceIds.mapNotNull { inputManager.getInputDevice(it) }
    }
}

