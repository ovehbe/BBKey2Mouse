package com.bbkey2mouse

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Activity to create a home screen shortcut for toggling mouse mode.
 * This handles the CREATE_SHORTCUT intent properly.
 */
class CreateShortcutActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create the shortcut intent with flags to prevent activity from showing
        val shortcutIntent = Intent(this, ToggleMouseActivity::class.java).apply {
            action = ToggleMouseActivity.ACTION_TOGGLE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use ShortcutManager for Android O+
            val shortcut = ShortcutInfoCompat.Builder(this, "toggle_mouse_shortcut")
                .setShortLabel(getString(R.string.toggle_mouse_shortcut))
                .setLongLabel(getString(R.string.toggle_mouse_shortcut))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_cursor))
                .setIntent(shortcutIntent)
                .build()
            
            val pinnedShortcutCallbackIntent = ShortcutManagerCompat.createShortcutResultIntent(this, shortcut)
            setResult(RESULT_OK, pinnedShortcutCallbackIntent)
        } else {
            // Legacy shortcut creation
            @Suppress("DEPRECATION")
            val resultIntent = Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.toggle_mouse_shortcut))
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this@CreateShortcutActivity, R.drawable.ic_shortcut_cursor)
                )
            }
            setResult(RESULT_OK, resultIntent)
        }
        
        finish()
    }
}

