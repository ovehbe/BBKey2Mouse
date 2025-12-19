package com.bbkey2mouse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.bbkey2mouse.service.MouseAccessibilityService

/**
 * Transparent activity that toggles mouse mode and immediately finishes.
 * Can be triggered via:
 * - App shortcut
 * - Intent from other apps (Tasker, MacroDroid, etc.)
 * - Widget
 * 
 * Intent action: com.bbkey2mouse.TOGGLE_MOUSE
 */
class ToggleMouseActivity : Activity() {
    
    companion object {
        const val ACTION_TOGGLE = "com.bbkey2mouse.TOGGLE_MOUSE"
        const val ACTION_ENABLE = "com.bbkey2mouse.ENABLE_MOUSE"
        const val ACTION_DISABLE = "com.bbkey2mouse.DISABLE_MOUSE"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        when (intent?.action) {
            ACTION_ENABLE -> {
                sendBroadcast(Intent(MouseAccessibilityService.ACTION_ENABLE_MOUSE).apply {
                    setPackage(packageName)
                })
            }
            ACTION_DISABLE -> {
                sendBroadcast(Intent(MouseAccessibilityService.ACTION_DISABLE_MOUSE).apply {
                    setPackage(packageName)
                })
            }
            else -> {
                // Default: toggle
                sendBroadcast(Intent(MouseAccessibilityService.ACTION_TOGGLE_MOUSE).apply {
                    setPackage(packageName)
                })
            }
        }
        
        finish()
    }
}

