package com.bbkey2mouse.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.bbkey2mouse.util.Preferences

@SuppressLint("ViewConstructor")
class CursorOverlay(context: Context) : View(context) {
    
    companion object {
        private const val TAG = "CursorOverlay"
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var isAttached = false
    
    // Cursor position (center of screen initially)
    private var cursorX = 0f
    private var cursorY = 0f
    
    // Screen dimensions
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Cursor properties
    private var cursorSize = Preferences.getPointerSize(context)
    private var cursorImage = Preferences.getPointerImage(context)
    
    // Custom cursor bitmap
    private var customCursorBitmap: Bitmap? = null
    
    // Paints
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
    }
    
    // Cheese colors 🧀
    private val cheeseYellow = Color.parseColor("#FFD700")
    private val cheeseOrange = Color.parseColor("#FFA500")
    private val cheeseHole = Color.parseColor("#CC9900")
    
    // Auto-hide and dimming
    private var isVisible = true
    private var isDimmed = false
    private val dimRunnable = Runnable { dimCursor() }
    private val hideRunnable = Runnable { hideCursor() }
    private var autoHideEnabled = Preferences.getAutoHideCursor(context)
    private val dimDelay = 3000L // 3 seconds to dim
    private val hideDelay = 5000L // 5 seconds to fully hide (after dim)
    
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = Gravity.TOP or Gravity.START
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
    }
    
    fun show() {
        if (!isAttached) {
            val metrics = context.resources.displayMetrics
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            cursorX = screenWidth / 2f
            cursorY = screenHeight / 2f
            
            try {
                windowManager.addView(this, layoutParams)
                isAttached = true
                isVisible = true
                alpha = 1f
                scheduleAutoHide()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun hide() {
        if (isAttached) {
            try {
                handler.removeCallbacks(dimRunnable)
                handler.removeCallbacks(hideRunnable)
                windowManager.removeView(this)
                isAttached = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun moveCursor(deltaX: Float, deltaY: Float) {
        val speed = Preferences.getPointerSpeed(context)
        cursorX = (cursorX + deltaX * speed).coerceIn(0f, screenWidth.toFloat())
        cursorY = (cursorY + deltaY * speed).coerceIn(0f, screenHeight.toFloat())
        
        // Restore visibility if hidden or dimmed
        if (!isVisible || isDimmed) {
            fadeIn()
        }
        
        scheduleAutoHide()
        invalidate()
    }
    
    fun getCursorPosition(): Pair<Float, Float> {
        return Pair(cursorX, cursorY)
    }
    
    fun updateSettings() {
        cursorSize = Preferences.getPointerSize(context)
        cursorImage = Preferences.getPointerImage(context)
        autoHideEnabled = Preferences.getAutoHideCursor(context)
        loadCustomCursor()
        invalidate()
    }
    
    private fun loadCustomCursor() {
        val uriString = Preferences.getCustomPointerUri(context)
        if (uriString != null && cursorImage == Preferences.POINTER_CUSTOM) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    if (originalBitmap != null) {
                        // Scale bitmap to cursor size
                        customCursorBitmap = Bitmap.createScaledBitmap(
                            originalBitmap, 
                            cursorSize, 
                            cursorSize, 
                            true
                        )
                        if (originalBitmap != customCursorBitmap) {
                            originalBitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load custom cursor", e)
                customCursorBitmap = null
            }
        } else {
            customCursorBitmap?.recycle()
            customCursorBitmap = null
        }
    }
    
    private fun scheduleAutoHide() {
        handler.removeCallbacks(dimRunnable)
        handler.removeCallbacks(hideRunnable)
        
        // Always schedule dimming after inactivity
        handler.postDelayed(dimRunnable, dimDelay)
    }
    
    private fun dimCursor() {
        isDimmed = true
        animate()
            .alpha(0.3f)
            .setDuration(300)
            .withEndAction {
                // If auto-hide is enabled, schedule complete hide after dimming
                if (autoHideEnabled) {
                    handler.postDelayed(hideRunnable, hideDelay - dimDelay)
                }
            }
            .start()
    }
    
    private fun hideCursor() {
        animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction { isVisible = false }
            .start()
    }
    
    private fun fadeIn() {
        isVisible = true
        isDimmed = false
        animate()
            .alpha(1f)
            .setDuration(150)
            .start()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        when (cursorImage) {
            Preferences.POINTER_DEFAULT -> drawArrowCursor(canvas)
            Preferences.POINTER_CHEESE -> drawCheeseCursor(canvas)
            Preferences.POINTER_DOT -> drawDotCursor(canvas)
            Preferences.POINTER_HAND -> drawHandCursor(canvas)
            Preferences.POINTER_CUSTOM -> drawCustomCursor(canvas)
        }
    }
    
    private fun drawCustomCursor(canvas: Canvas) {
        val bitmap = customCursorBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.drawBitmap(bitmap, cursorX, cursorY, null)
        } else {
            // Fallback to arrow if custom bitmap not available
            drawArrowCursor(canvas)
        }
    }
    
    private fun drawArrowCursor(canvas: Canvas) {
        val path = Path()
        val size = cursorSize.toFloat()
        
        // Classic arrow cursor shape
        path.moveTo(cursorX, cursorY)
        path.lineTo(cursorX, cursorY + size * 0.85f)
        path.lineTo(cursorX + size * 0.25f, cursorY + size * 0.65f)
        path.lineTo(cursorX + size * 0.45f, cursorY + size)
        path.lineTo(cursorX + size * 0.55f, cursorY + size * 0.92f)
        path.lineTo(cursorX + size * 0.35f, cursorY + size * 0.58f)
        path.lineTo(cursorX + size * 0.6f, cursorY + size * 0.58f)
        path.close()
        
        // White fill
        cursorPaint.color = Color.WHITE
        canvas.drawPath(path, cursorPaint)
        
        // Black outline
        canvas.drawPath(path, outlinePaint)
    }
    
    private fun drawCheeseCursor(canvas: Canvas) {
        val size = cursorSize.toFloat()
        val centerX = cursorX + size / 2
        val centerY = cursorY + size / 2
        
        // Cheese wedge shape 🧀
        val path = Path()
        path.moveTo(cursorX, cursorY + size * 0.8f)
        path.lineTo(cursorX + size, cursorY + size * 0.8f)
        path.lineTo(cursorX + size * 0.5f, cursorY)
        path.close()
        
        // Main cheese color
        cursorPaint.color = cheeseYellow
        canvas.drawPath(path, cursorPaint)
        
        // Cheese holes
        cursorPaint.color = cheeseHole
        canvas.drawCircle(cursorX + size * 0.3f, cursorY + size * 0.55f, size * 0.08f, cursorPaint)
        canvas.drawCircle(cursorX + size * 0.55f, cursorY + size * 0.65f, size * 0.06f, cursorPaint)
        canvas.drawCircle(cursorX + size * 0.7f, cursorY + size * 0.5f, size * 0.07f, cursorPaint)
        
        // Orange edge for depth
        outlinePaint.color = cheeseOrange
        outlinePaint.strokeWidth = 3f
        canvas.drawPath(path, outlinePaint)
        outlinePaint.color = Color.BLACK
        outlinePaint.strokeWidth = 2f
    }
    
    private fun drawDotCursor(canvas: Canvas) {
        val radius = cursorSize / 2f
        
        // Cyan dot with glow effect
        cursorPaint.color = Color.parseColor("#00D9FF")
        canvas.drawCircle(cursorX + radius, cursorY + radius, radius, cursorPaint)
        
        // Inner white highlight
        cursorPaint.color = Color.WHITE
        canvas.drawCircle(cursorX + radius - radius * 0.2f, cursorY + radius - radius * 0.2f, radius * 0.3f, cursorPaint)
        
        // Outer ring
        outlinePaint.color = Color.parseColor("#00A8C6")
        outlinePaint.strokeWidth = 3f
        canvas.drawCircle(cursorX + radius, cursorY + radius, radius, outlinePaint)
        outlinePaint.color = Color.BLACK
        outlinePaint.strokeWidth = 2f
    }
    
    private fun drawHandCursor(canvas: Canvas) {
        val size = cursorSize.toFloat()
        
        // Pointing hand
        cursorPaint.color = Color.WHITE
        
        // Palm
        canvas.drawRoundRect(
            cursorX + size * 0.15f, cursorY + size * 0.4f,
            cursorX + size * 0.7f, cursorY + size * 0.9f,
            size * 0.1f, size * 0.1f, cursorPaint
        )
        
        // Pointing finger (index)
        canvas.drawRoundRect(
            cursorX + size * 0.25f, cursorY,
            cursorX + size * 0.45f, cursorY + size * 0.5f,
            size * 0.08f, size * 0.08f, cursorPaint
        )
        
        // Other fingers (curled)
        canvas.drawCircle(cursorX + size * 0.55f, cursorY + size * 0.35f, size * 0.1f, cursorPaint)
        canvas.drawCircle(cursorX + size * 0.65f, cursorY + size * 0.45f, size * 0.08f, cursorPaint)
        
        // Thumb
        canvas.drawRoundRect(
            cursorX, cursorY + size * 0.5f,
            cursorX + size * 0.25f, cursorY + size * 0.7f,
            size * 0.06f, size * 0.06f, cursorPaint
        )
        
        // Outline
        outlinePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(
            cursorX + size * 0.25f, cursorY,
            cursorX + size * 0.45f, cursorY + size * 0.5f,
            size * 0.08f, size * 0.08f, outlinePaint
        )
        outlinePaint.strokeWidth = 2f
    }
}

