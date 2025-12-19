package com.bbkey2mouse.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.bbkey2mouse.util.Preferences

/**
 * Floating virtual trackpad overlay.
 * - Drag on trackpad to move cursor
 * - Tap for left click
 * - Long press for right click
 * - Optional drag handle to reposition
 */
@SuppressLint("ViewConstructor")
class FloatingControlsOverlay(
    private val context: Context,
    private val onLeftClick: () -> Unit,
    private val onRightClick: () -> Unit,
    private val onToggle: () -> Unit,
    private val onDrag: (Float, Float) -> Unit
) : View(context) {
    
    companion object {
        private const val TAG = "FloatingControls"
        private const val LONG_PRESS_THRESHOLD = 400L
        private const val TAP_THRESHOLD = 200L
        private const val MOVE_THRESHOLD = 10f
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var isAttached = false
    
    // Trackpad settings
    private var trackpadWidth = Preferences.getTrackpadWidth(context)
    private var trackpadHeight = Preferences.getTrackpadHeight(context)
    private var trackpadColor = Preferences.getTrackpadColorValue(context)
    private var trackpadOpacity = Preferences.getTrackpadOpacity(context)
    private var trackpadRounding = Preferences.getTrackpadRounding(context)
    private var showDragHandle = Preferences.getTrackpadShowDrag(context)
    
    // Drag handle dimensions
    private val dragHandleHeight = 30
    private val dragHandlePadding = 8
    
    // Touch tracking
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchStartTime = 0L
    private var isDraggingTrackpad = false
    private var isMovingPosition = false
    private var isTwoFingerDrag = false
    private var wasInTwoFingerMode = false // Block all input until fingers lifted
    private var hasMoved = false
    private var twoFingerDragEnabled = Preferences.getTrackpadTwoFingerDrag(context)
    
    // Two-finger tracking - use raw coordinates for stability
    private var twoFingerLastRawX = 0f
    private var twoFingerLastRawY = 0f
    
    // Long press detection
    private var longPressTriggered = false
    private val longPressRunnable = Runnable {
        if (!hasMoved && !longPressTriggered) {
            longPressTriggered = true
            vibrate(50)
            onRightClick()
        }
    }
    
    // Paints
    private val trackpadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val dragHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Vibrator
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        gravity = Gravity.TOP or Gravity.START
        x = Preferences.getTrackpadX(context)
        y = Preferences.getTrackpadY(context)
    }
    
    init {
        updateDimensions()
    }
    
    private fun updateDimensions() {
        val totalHeight = if (showDragHandle) {
            trackpadHeight + dragHandleHeight + dragHandlePadding
        } else {
            trackpadHeight
        }
        layoutParams.width = trackpadWidth
        layoutParams.height = totalHeight
    }
    
    fun show() {
        if (isAttached) return
        
        try {
            updateSettings()
            windowManager.addView(this, layoutParams)
            isAttached = true
            Log.d(TAG, "Floating trackpad attached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach floating trackpad", e)
        }
    }
    
    fun hide() {
        if (!isAttached) return
        
        try {
            // Save position
            Preferences.setTrackpadX(context, layoutParams.x)
            Preferences.setTrackpadY(context, layoutParams.y)
            
            windowManager.removeView(this)
            isAttached = false
            Log.d(TAG, "Floating trackpad detached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detach floating trackpad", e)
        }
    }
    
    fun updateSettings() {
        trackpadWidth = Preferences.getTrackpadWidth(context)
        trackpadHeight = Preferences.getTrackpadHeight(context)
        trackpadColor = Preferences.getTrackpadColorValue(context)
        trackpadOpacity = Preferences.getTrackpadOpacity(context)
        trackpadRounding = Preferences.getTrackpadRounding(context)
        showDragHandle = Preferences.getTrackpadShowDrag(context)
        twoFingerDragEnabled = Preferences.getTrackpadTwoFingerDrag(context)
        
        layoutParams.x = Preferences.getTrackpadX(context)
        layoutParams.y = Preferences.getTrackpadY(context)
        
        updateDimensions()
        
        if (isAttached) {
            try {
                windowManager.updateViewLayout(this, layoutParams)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update layout", e)
            }
        }
        
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val alpha = (trackpadOpacity * 255 / 100)
        
        // Draw drag handle if enabled
        var trackpadTop = 0f
        if (showDragHandle) {
            trackpadTop = dragHandleHeight.toFloat() + dragHandlePadding
            
            // Drag handle background
            dragHandlePaint.color = Color.argb(alpha, 100, 100, 100)
            val handleRect = RectF(
                trackpadWidth / 4f,
                8f,
                trackpadWidth * 3f / 4f,
                dragHandleHeight.toFloat() - 8f
            )
            canvas.drawRoundRect(handleRect, 6f, 6f, dragHandlePaint)
        }
        
        // Trackpad background
        trackpadPaint.color = Color.argb(
            alpha,
            Color.red(trackpadColor),
            Color.green(trackpadColor),
            Color.blue(trackpadColor)
        )
        
        // Border
        borderPaint.color = Color.argb(
            (alpha * 1.2f).toInt().coerceAtMost(255),
            Color.red(trackpadColor),
            Color.green(trackpadColor),
            Color.blue(trackpadColor)
        )
        
        val trackpadRect = RectF(0f, trackpadTop, trackpadWidth.toFloat(), trackpadTop + trackpadHeight)
        val rounding = trackpadRounding.toFloat()
        
        canvas.drawRoundRect(trackpadRect, rounding, rounding, trackpadPaint)
        canvas.drawRoundRect(trackpadRect, rounding, rounding, borderPaint)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val trackpadTop = if (showDragHandle) dragHandleHeight + dragHandlePadding else 0
        val inDragHandle = showDragHandle && event.y < trackpadTop
        val actionMasked = event.actionMasked
        
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // If we were in two-finger mode, block until all fingers were lifted
                // This is a fresh touch, so reset the flag
                wasInTwoFingerMode = false
                
                touchStartX = event.rawX
                touchStartY = event.rawY
                lastTouchX = event.x
                lastTouchY = event.y
                touchStartTime = System.currentTimeMillis()
                hasMoved = false
                longPressTriggered = false
                isTwoFingerDrag = false
                
                if (inDragHandle) {
                    isMovingPosition = true
                } else {
                    isDraggingTrackpad = true
                    // Schedule long press
                    handler.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD)
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger touched - switch to two-finger drag mode if enabled
                if (twoFingerDragEnabled && event.pointerCount == 2) {
                    // Cancel all single-finger operations permanently for this gesture
                    handler.removeCallbacks(longPressRunnable)
                    isDraggingTrackpad = false
                    isMovingPosition = false
                    isTwoFingerDrag = true
                    wasInTwoFingerMode = true
                    
                    // Store initial raw position for stable dragging
                    twoFingerLastRawX = event.rawX
                    twoFingerLastRawY = event.rawY
                    
                    // Brief vibration feedback
                    vibrate(20)
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // If we ever entered two-finger mode, block all normal operations
                if (wasInTwoFingerMode) {
                    if (isTwoFingerDrag && event.pointerCount >= 2) {
                        // Use raw coordinates for stable two-finger drag
                        val deltaX = event.rawX - twoFingerLastRawX
                        val deltaY = event.rawY - twoFingerLastRawY
                        
                        layoutParams.x = (layoutParams.x + deltaX).toInt()
                        layoutParams.y = (layoutParams.y + deltaY).toInt()
                        
                        twoFingerLastRawX = event.rawX
                        twoFingerLastRawY = event.rawY
                        
                        try {
                            windowManager.updateViewLayout(this, layoutParams)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    // Block all other input while in or after two-finger mode
                    return true
                }
                
                if (isMovingPosition) {
                    // Single-finger drag on handle to reposition
                    layoutParams.x = (layoutParams.x + (event.rawX - touchStartX)).toInt()
                    layoutParams.y = (layoutParams.y + (event.rawY - touchStartY)).toInt()
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    
                    try {
                        windowManager.updateViewLayout(this, layoutParams)
                    } catch (e: Exception) {
                        // Ignore
                    }
                } else if (isDraggingTrackpad) {
                    val deltaX = event.rawX - touchStartX
                    val deltaY = event.rawY - touchStartY
                    val moveDeltaX = event.x - lastTouchX
                    val moveDeltaY = event.y - lastTouchY
                    
                    // Check if moved beyond threshold
                    if (!hasMoved && (kotlin.math.abs(deltaX) > MOVE_THRESHOLD || kotlin.math.abs(deltaY) > MOVE_THRESHOLD)) {
                        hasMoved = true
                        handler.removeCallbacks(longPressRunnable)
                    }
                    
                    if (hasMoved) {
                        onDrag(moveDeltaX, moveDeltaY)
                    }
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                // One finger lifted - stop two-finger drag but keep blocking
                if (isTwoFingerDrag) {
                    isTwoFingerDrag = false
                    // Save position
                    Preferences.setTrackpadX(context, layoutParams.x)
                    Preferences.setTrackpadY(context, layoutParams.y)
                }
                // wasInTwoFingerMode stays true to block input until all fingers lifted
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                
                if (wasInTwoFingerMode) {
                    // Save position after two-finger drag (final save)
                    Preferences.setTrackpadX(context, layoutParams.x)
                    Preferences.setTrackpadY(context, layoutParams.y)
                } else if (isMovingPosition) {
                    // Save position after handle drag
                    Preferences.setTrackpadX(context, layoutParams.x)
                    Preferences.setTrackpadY(context, layoutParams.y)
                } else if (isDraggingTrackpad && !hasMoved && !longPressTriggered) {
                    val touchDuration = System.currentTimeMillis() - touchStartTime
                    if (touchDuration < TAP_THRESHOLD) {
                        // Tap - left click
                        vibrate(30)
                        onLeftClick()
                    }
                }
                
                // Reset all state
                isDraggingTrackpad = false
                isMovingPosition = false
                isTwoFingerDrag = false
                wasInTwoFingerMode = false
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun vibrate(durationMs: Long) {
        if (!Preferences.getVibrateOnClick(context)) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
