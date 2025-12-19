package com.bbkey2mouse.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.bbkey2mouse.service.KeyboardTouchService

/**
 * Invisible overlay that intercepts touch events from the BB Key2 keyboard.
 * 
 * This overlay is positioned over the keyboard area and intercepts touch
 * events before they're processed as scroll gestures by the system.
 * 
 * We use FLAG_NOT_FOCUSABLE so it doesn't steal focus, but we don't use
 * FLAG_NOT_TOUCHABLE so we can intercept touch events.
 */
@SuppressLint("ViewConstructor")
class TouchInterceptorOverlay(
    context: Context,
    private val onTouchEvent: (MotionEvent) -> Boolean
) : View(context) {
    
    companion object {
        private const val TAG = "TouchInterceptor"
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isAttached = false
    
    // The interceptor overlay covers the keyboard area
    // BB Key2 keyboard is roughly 1/4 of the screen height at the bottom
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        format = PixelFormat.TRANSLUCENT
        // KEY: We need to receive touch events but pass them through for screen touches
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        
        // Cover the bottom portion where keyboard touch events originate
        // These dimensions will be adjusted based on screen size
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = 1 // Minimal height - we're intercepting input events, not touches on this view
        y = 0
    }
    
    init {
        // Make completely transparent
        alpha = 0f
        setBackgroundColor(0x00000000)
    }
    
    fun show() {
        if (!isAttached) {
            try {
                windowManager.addView(this, layoutParams)
                isAttached = true
                Log.d(TAG, "Touch interceptor overlay attached")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach touch interceptor", e)
            }
        }
    }
    
    fun hide() {
        if (isAttached) {
            try {
                windowManager.removeView(this)
                isAttached = false
                Log.d(TAG, "Touch interceptor overlay detached")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to detach touch interceptor", e)
            }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Check if this is from the keyboard touchpad
        if (KeyboardTouchService.isKeyboardTouchEvent(event)) {
            return onTouchEvent.invoke(event)
        }
        // Pass through to underlying views
        return false
    }
    
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Generic motion events from touchpads come through here
        if (KeyboardTouchService.isKeyboardTouchEvent(event)) {
            Log.d(TAG, "Intercepted keyboard touch: action=${event.action}, x=${event.x}, y=${event.y}")
            return onTouchEvent.invoke(event)
        }
        return super.onGenericMotionEvent(event)
    }
}

