package com.bbkey2mouse.util

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.core.content.edit

object Preferences {
    private const val PREFS_NAME = "bbkey2mouse_prefs"
    
    // Keys
    private const val KEY_POINTER_SPEED = "pointer_speed"
    private const val KEY_POINTER_SIZE = "pointer_size"
    private const val KEY_POINTER_IMAGE = "pointer_image"
    private const val KEY_TOGGLE_KEYCODE = "toggle_keycode"
    private const val KEY_CLICK_KEYCODE = "click_keycode"
    private const val KEY_RIGHT_CLICK_KEYCODE = "right_click_keycode"
    private const val KEY_SCROLL_MODE_KEYCODE = "scroll_mode_keycode"
    private const val KEY_VIBRATE_ON_CLICK = "vibrate_on_click"
    private const val KEY_AUTO_HIDE_CURSOR = "auto_hide_cursor"
    private const val KEY_MOUSE_MODE_ENABLED = "mouse_mode_enabled"
    
    // Input mode keys
    private const val KEY_INPUT_MODE = "input_mode"
    
    // Virtual trackpad keys
    private const val KEY_TRACKPAD_COLOR = "trackpad_color"
    private const val KEY_TRACKPAD_OPACITY = "trackpad_opacity"
    private const val KEY_TRACKPAD_SHAPE = "trackpad_shape"
    private const val KEY_TRACKPAD_WIDTH = "trackpad_width"
    private const val KEY_TRACKPAD_HEIGHT = "trackpad_height"
    private const val KEY_TRACKPAD_X = "trackpad_x"
    private const val KEY_TRACKPAD_Y = "trackpad_y"
    private const val KEY_TRACKPAD_SHOW_DRAG = "trackpad_show_drag"
    private const val KEY_TRACKPAD_ROUNDING = "trackpad_rounding"
    private const val KEY_TRACKPAD_TWO_FINGER_DRAG = "trackpad_two_finger_drag"
    
    // Pointer image options
    const val POINTER_DEFAULT = 0
    const val POINTER_CHEESE = 1
    const val POINTER_DOT = 2
    const val POINTER_HAND = 3
    
    // Input mode options
    const val INPUT_MODE_VIRTUAL = 0
    const val INPUT_MODE_KEYBOARD = 1
    
    // Trackpad shape options
    const val SHAPE_SQUARE = 0
    const val SHAPE_ROUNDED = 1
    const val SHAPE_CIRCLE = 2
    
    // Trackpad color options
    const val COLOR_CYAN = 0
    const val COLOR_PURPLE = 1
    const val COLOR_GREEN = 2
    const val COLOR_ORANGE = 3
    const val COLOR_WHITE = 4
    const val COLOR_DARK = 5
    
    // Defaults
    private const val DEFAULT_POINTER_SPEED = 1.5f
    private const val DEFAULT_POINTER_SIZE = 48
    private const val DEFAULT_POINTER_IMAGE = POINTER_DEFAULT
    private const val DEFAULT_TOGGLE_KEYCODE = KeyEvent.KEYCODE_SPACE  // Long press space
    private const val DEFAULT_CLICK_KEYCODE = KeyEvent.KEYCODE_ENTER   // Enter to click
    private const val DEFAULT_RIGHT_CLICK_KEYCODE = KeyEvent.KEYCODE_DEL  // Backspace for right click
    private const val DEFAULT_SCROLL_MODE_KEYCODE = KeyEvent.KEYCODE_SHIFT_LEFT  // Hold shift for scroll
    private const val DEFAULT_VIBRATE_ON_CLICK = true
    private const val DEFAULT_AUTO_HIDE_CURSOR = true
    private const val DEFAULT_INPUT_MODE = INPUT_MODE_VIRTUAL
    private const val DEFAULT_TRACKPAD_COLOR = COLOR_CYAN
    private const val DEFAULT_TRACKPAD_OPACITY = 80 // 80%
    private const val DEFAULT_TRACKPAD_SHAPE = SHAPE_ROUNDED
    private const val DEFAULT_TRACKPAD_WIDTH = 180
    private const val DEFAULT_TRACKPAD_HEIGHT = 120
    private const val DEFAULT_TRACKPAD_X = 50
    private const val DEFAULT_TRACKPAD_Y = 400
    private const val DEFAULT_TRACKPAD_SHOW_DRAG = false
    private const val DEFAULT_TRACKPAD_ROUNDING = 16 // pixels
    private const val DEFAULT_TRACKPAD_TWO_FINGER_DRAG = true
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Pointer Speed (0.5 - 5.0)
    fun getPointerSpeed(context: Context): Float {
        return getPrefs(context).getFloat(KEY_POINTER_SPEED, DEFAULT_POINTER_SPEED)
    }
    
    fun setPointerSpeed(context: Context, speed: Float) {
        getPrefs(context).edit { putFloat(KEY_POINTER_SPEED, speed.coerceIn(0.5f, 5.0f)) }
    }
    
    // Pointer Size (16 - 128 pixels)
    fun getPointerSize(context: Context): Int {
        return getPrefs(context).getInt(KEY_POINTER_SIZE, DEFAULT_POINTER_SIZE)
    }
    
    fun setPointerSize(context: Context, size: Int) {
        getPrefs(context).edit { putInt(KEY_POINTER_SIZE, size.coerceIn(16, 128)) }
    }
    
    // Pointer Image
    fun getPointerImage(context: Context): Int {
        return getPrefs(context).getInt(KEY_POINTER_IMAGE, DEFAULT_POINTER_IMAGE)
    }
    
    fun setPointerImage(context: Context, image: Int) {
        getPrefs(context).edit { putInt(KEY_POINTER_IMAGE, image) }
    }
    
    // Toggle Key
    fun getToggleKeycode(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOGGLE_KEYCODE, DEFAULT_TOGGLE_KEYCODE)
    }
    
    fun setToggleKeycode(context: Context, keycode: Int) {
        getPrefs(context).edit { putInt(KEY_TOGGLE_KEYCODE, keycode) }
    }
    
    // Click Key
    fun getClickKeycode(context: Context): Int {
        return getPrefs(context).getInt(KEY_CLICK_KEYCODE, DEFAULT_CLICK_KEYCODE)
    }
    
    fun setClickKeycode(context: Context, keycode: Int) {
        getPrefs(context).edit { putInt(KEY_CLICK_KEYCODE, keycode) }
    }
    
    // Right Click Key
    fun getRightClickKeycode(context: Context): Int {
        return getPrefs(context).getInt(KEY_RIGHT_CLICK_KEYCODE, DEFAULT_RIGHT_CLICK_KEYCODE)
    }
    
    fun setRightClickKeycode(context: Context, keycode: Int) {
        getPrefs(context).edit { putInt(KEY_RIGHT_CLICK_KEYCODE, keycode) }
    }
    
    // Scroll Mode Key
    fun getScrollModeKeycode(context: Context): Int {
        return getPrefs(context).getInt(KEY_SCROLL_MODE_KEYCODE, DEFAULT_SCROLL_MODE_KEYCODE)
    }
    
    fun setScrollModeKeycode(context: Context, keycode: Int) {
        getPrefs(context).edit { putInt(KEY_SCROLL_MODE_KEYCODE, keycode) }
    }
    
    // Vibrate on Click
    fun getVibrateOnClick(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATE_ON_CLICK, DEFAULT_VIBRATE_ON_CLICK)
    }
    
    fun setVibrateOnClick(context: Context, vibrate: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_VIBRATE_ON_CLICK, vibrate) }
    }
    
    // Auto Hide Cursor
    fun getAutoHideCursor(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_HIDE_CURSOR, DEFAULT_AUTO_HIDE_CURSOR)
    }
    
    fun setAutoHideCursor(context: Context, autoHide: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_AUTO_HIDE_CURSOR, autoHide) }
    }
    
    // Mouse Mode State
    fun isMouseModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MOUSE_MODE_ENABLED, false)
    }
    
    fun setMouseModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_MOUSE_MODE_ENABLED, enabled) }
    }
    
    // Input Mode
    fun getInputMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_INPUT_MODE, DEFAULT_INPUT_MODE)
    }
    
    fun setInputMode(context: Context, mode: Int) {
        getPrefs(context).edit { putInt(KEY_INPUT_MODE, mode) }
    }
    
    fun isVirtualMode(context: Context): Boolean {
        return getInputMode(context) == INPUT_MODE_VIRTUAL
    }
    
    // Trackpad Color
    fun getTrackpadColor(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_COLOR, DEFAULT_TRACKPAD_COLOR)
    }
    
    fun setTrackpadColor(context: Context, color: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_COLOR, color) }
    }
    
    fun getTrackpadColorValue(context: Context): Int {
        return when (getTrackpadColor(context)) {
            COLOR_CYAN -> 0xFF00D9FF.toInt()
            COLOR_PURPLE -> 0xFF9C27B0.toInt()
            COLOR_GREEN -> 0xFF4CAF50.toInt()
            COLOR_ORANGE -> 0xFFFF9800.toInt()
            COLOR_WHITE -> 0xFFFFFFFF.toInt()
            COLOR_DARK -> 0xFF2A2A3E.toInt()
            else -> 0xFF00D9FF.toInt()
        }
    }
    
    // Trackpad Opacity (0-100)
    fun getTrackpadOpacity(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_OPACITY, DEFAULT_TRACKPAD_OPACITY)
    }
    
    fun setTrackpadOpacity(context: Context, opacity: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_OPACITY, opacity.coerceIn(20, 100)) }
    }
    
    // Trackpad Shape
    fun getTrackpadShape(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_SHAPE, DEFAULT_TRACKPAD_SHAPE)
    }
    
    fun setTrackpadShape(context: Context, shape: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_SHAPE, shape) }
    }
    
    // Trackpad Width (50-400)
    fun getTrackpadWidth(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_WIDTH, DEFAULT_TRACKPAD_WIDTH)
    }
    
    fun setTrackpadWidth(context: Context, width: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_WIDTH, width.coerceIn(50, 400)) }
    }
    
    // Trackpad Height (50-400)
    fun getTrackpadHeight(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_HEIGHT, DEFAULT_TRACKPAD_HEIGHT)
    }
    
    fun setTrackpadHeight(context: Context, height: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_HEIGHT, height.coerceIn(50, 400)) }
    }
    
    // Trackpad Position X
    fun getTrackpadX(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_X, DEFAULT_TRACKPAD_X)
    }
    
    fun setTrackpadX(context: Context, x: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_X, x) }
    }
    
    // Trackpad Position Y
    fun getTrackpadY(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_Y, DEFAULT_TRACKPAD_Y)
    }
    
    fun setTrackpadY(context: Context, y: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_Y, y) }
    }
    
    // Show Drag Handle
    fun getTrackpadShowDrag(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TRACKPAD_SHOW_DRAG, DEFAULT_TRACKPAD_SHOW_DRAG)
    }
    
    fun setTrackpadShowDrag(context: Context, show: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_TRACKPAD_SHOW_DRAG, show) }
    }
    
    // Trackpad Rounding (0-100 pixels)
    fun getTrackpadRounding(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRACKPAD_ROUNDING, DEFAULT_TRACKPAD_ROUNDING)
    }
    
    fun setTrackpadRounding(context: Context, rounding: Int) {
        getPrefs(context).edit { putInt(KEY_TRACKPAD_ROUNDING, rounding.coerceIn(0, 100)) }
    }
    
    // Two-Finger Drag to Reposition
    fun getTrackpadTwoFingerDrag(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TRACKPAD_TWO_FINGER_DRAG, DEFAULT_TRACKPAD_TWO_FINGER_DRAG)
    }
    
    fun setTrackpadTwoFingerDrag(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_TRACKPAD_TWO_FINGER_DRAG, enabled) }
    }
    
    // Helper to get key name
    fun getKeyName(keycode: Int): String {
        return when (keycode) {
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_ENTER -> "Enter"
            KeyEvent.KEYCODE_DEL -> "Backspace"
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift"
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "Alt"
            KeyEvent.KEYCODE_SYM -> "Sym"
            KeyEvent.KEYCODE_AT -> "@"
            KeyEvent.KEYCODE_PERIOD -> "."
            KeyEvent.KEYCODE_COMMA -> ","
            else -> KeyEvent.keyCodeToString(keycode).removePrefix("KEYCODE_")
        }
    }
    
    // Register listener for preference changes
    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        getPrefs(context).registerOnSharedPreferenceChangeListener(listener)
    }
    
    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        getPrefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}

