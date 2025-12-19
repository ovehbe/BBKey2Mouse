package com.bbkey2mouse.service

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent

/**
 * Utility class to identify and handle BB Key2 keyboard touch events.
 * 
 * The BlackBerry Key2's keyboard touch surface is recognized as a touchpad input device.
 * When enabled, it generates MotionEvents that we need to intercept before they're
 * processed as scroll gestures by the system.
 * 
 * Device characteristics:
 * - Sources include SOURCE_TOUCHPAD
 * - Usually has "keyboard" or "bb" in the device name
 * - Supports multi-touch (though we mainly use single touch for cursor)
 */
object KeyboardTouchService {
    
    private const val TAG = "KeyboardTouchService"
    
    // Common device name patterns for BB Key2 keyboard touchpad
    private val BB_KEYBOARD_PATTERNS = listOf(
        "bbq", // BBQ is the BB Key2 keyboard codename
        "blackberry",
        "keyboard touchpad",
        "keypad",
        "priv" // BB Priv also has touch keyboard
    )
    
    /**
     * Check if a MotionEvent comes from the BB Key2 keyboard touch surface.
     */
    fun isKeyboardTouchEvent(event: MotionEvent): Boolean {
        val device = event.device ?: return false
        return isKeyboardTouchDevice(device)
    }
    
    /**
     * Check if an InputDevice is the BB Key2 keyboard touch surface.
     */
    fun isKeyboardTouchDevice(device: InputDevice): Boolean {
        // Check if it's a touchpad source
        val sources = device.sources
        val isTouchpad = (sources and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD
        
        if (!isTouchpad) return false
        
        // Check device name for BB patterns
        val deviceName = device.name.lowercase()
        return BB_KEYBOARD_PATTERNS.any { pattern -> 
            deviceName.contains(pattern) 
        } || isLikelyBBKeyboard(device)
    }
    
    /**
     * Heuristic check for BB keyboard characteristics.
     */
    private fun isLikelyBBKeyboard(device: InputDevice): Boolean {
        // BB Key2 keyboard has specific motion ranges
        val xRange = device.getMotionRange(MotionEvent.AXIS_X)
        val yRange = device.getMotionRange(MotionEvent.AXIS_Y)
        
        if (xRange == null || yRange == null) return false
        
        // The keyboard touchpad has a specific aspect ratio (wide and short)
        val width = xRange.max - xRange.min
        val height = yRange.max - yRange.min
        
        // Keyboard is roughly 3:1 aspect ratio touchpad
        val aspectRatio = if (height > 0) width / height else 0f
        return aspectRatio > 2.0f && aspectRatio < 5.0f
    }
    
    /**
     * Get list of all keyboard touch devices.
     */
    fun getKeyboardTouchDevices(context: Context): List<InputDevice> {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val deviceIds = inputManager.inputDeviceIds
        
        return deviceIds.toList().mapNotNull { id ->
            inputManager.getInputDevice(id)?.takeIf { isKeyboardTouchDevice(it) }
        }
    }
    
    /**
     * Check if any keyboard touch device is connected.
     */
    fun hasKeyboardTouch(context: Context): Boolean {
        return getKeyboardTouchDevices(context).isNotEmpty()
    }
}

