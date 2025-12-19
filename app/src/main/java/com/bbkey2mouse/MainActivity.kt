package com.bbkey2mouse

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bbkey2mouse.databinding.ActivityMainBinding
import com.bbkey2mouse.service.KeyboardTouchService
import com.bbkey2mouse.service.MouseAccessibilityService
import com.bbkey2mouse.util.Preferences

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var isMouseModeEnabled = false
    
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == MouseAccessibilityService.ACTION_STATE_CHANGED) {
                isMouseModeEnabled = intent.getBooleanExtra(
                    MouseAccessibilityService.EXTRA_MOUSE_ENABLED, 
                    false
                )
                updateUI()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Register state receiver
        val filter = IntentFilter(MouseAccessibilityService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
        
        // Query current state
        sendBroadcast(Intent(MouseAccessibilityService.ACTION_QUERY_STATE).apply {
            setPackage(packageName)
        })
        
        checkPermissions()
        updateUI()
    }
    
    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(stateReceiver)
        } catch (e: Exception) {
            // Not registered
        }
    }
    
    private fun setupUI() {
        // Toggle button
        binding.toggleButton.setOnClickListener {
            sendBroadcast(Intent(MouseAccessibilityService.ACTION_TOGGLE_MOUSE).apply {
                setPackage(packageName)
            })
        }
        
        // Settings button
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Enable accessibility button
        binding.enableAccessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }
        
        // Overlay permission button
        binding.enableOverlayButton.setOnClickListener {
            openOverlaySettings()
        }
        
        // Check for BB keyboard
        val hasKeyboard = KeyboardTouchService.hasKeyboardTouch(this)
        binding.keyboardStatus.text = if (hasKeyboard) {
            "✓ BlackBerry keyboard detected"
        } else {
            "⚠ BlackBerry keyboard not detected\nMake sure you're using a BB Key2"
        }
        binding.keyboardStatus.setTextColor(
            ContextCompat.getColor(this, if (hasKeyboard) R.color.status_active else R.color.status_warning)
        )
    }
    
    private fun checkPermissions() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)
        
        // Update accessibility section
        binding.accessibilityCard.visibility = if (accessibilityEnabled) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
        
        // Update overlay section
        binding.overlayCard.visibility = if (overlayEnabled) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
        
        // Show main controls only if all permissions granted
        val allPermissionsGranted = accessibilityEnabled && overlayEnabled
        binding.mainControls.visibility = if (allPermissionsGranted) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
    
    private fun updateUI() {
        val toggleKey = Preferences.getKeyName(Preferences.getToggleKeycode(this))
        
        if (isMouseModeEnabled) {
            binding.statusText.text = getString(R.string.mouse_mode_enabled)
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
            binding.toggleButton.text = "Disable Mouse Mode"
            binding.statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_active))
            binding.instructionText.text = "Swipe on keyboard to move cursor\nLong press $toggleKey to disable"
        } else {
            binding.statusText.text = getString(R.string.mouse_mode_disabled)
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
            binding.toggleButton.text = "Enable Mouse Mode"
            binding.statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.on_surface_dim))
            binding.instructionText.text = "Long press $toggleKey to toggle mouse mode"
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        return enabledServices.any { 
            it.resolveInfo.serviceInfo.packageName == packageName 
        }
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
    
    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }
}

