package com.bbkey2mouse

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bbkey2mouse.databinding.ActivitySettingsBinding
import com.bbkey2mouse.service.MouseAccessibilityService
import com.bbkey2mouse.util.Preferences

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    // Image picker launchers
    private val customPointerPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Preferences.setCustomPointerUri(this, it.toString())
            // Auto-select custom pointer type
            Preferences.setPointerImage(this, Preferences.POINTER_CUSTOM)
            updatePointerImageDisplay()
            updateCustomPointerStatus()
            notifySettingsChanged()
        }
    }
    
    private val customTrackpadPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Preferences.setCustomTrackpadUri(this, it.toString())
            updateCustomTrackpadStatus()
            notifySettingsChanged()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        setupUI()
        loadSettings()
        updateVisibility()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun setupUI() {
        // Input Mode Selection
        binding.inputModeCard.setOnClickListener {
            showInputModeDialog()
        }
        
        // Pointer Speed Slider
        binding.speedSlider.apply {
            max = 90
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val speed = 0.5f + (progress * 0.05f)
                    binding.speedValue.text = String.format("%.1fx", speed)
                    if (fromUser) {
                        Preferences.setPointerSpeed(this@SettingsActivity, speed)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Pointer Size Slider
        binding.sizeSlider.apply {
            max = 112
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = 16 + progress
                    binding.sizeValue.text = "${size}px"
                    if (fromUser) {
                        Preferences.setPointerSize(this@SettingsActivity, size)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Pointer Image Selection
        binding.pointerImageCard.setOnClickListener {
            showPointerImageDialog()
        }
        
        // Custom Pointer Image
        binding.customPointerBtn.setOnClickListener {
            customPointerPicker.launch(arrayOf("image/*"))
        }
        binding.customPointerClearBtn.setOnClickListener {
            Preferences.setCustomPointerUri(this, null)
            // Reset to default if currently using custom
            if (Preferences.getPointerImage(this) == Preferences.POINTER_CUSTOM) {
                Preferences.setPointerImage(this, Preferences.POINTER_DEFAULT)
                updatePointerImageDisplay()
            }
            updateCustomPointerStatus()
            notifySettingsChanged()
        }
        
        // Virtual Trackpad Settings
        setupTrackpadSettings()
        
        // Keyboard Settings (for when keyboard mode is selected)
        setupKeyboardSettings()
        
        // Behavior switches
        binding.vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setVibrateOnClick(this, isChecked)
        }
        
        binding.autoHideSwitch.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setAutoHideCursor(this, isChecked)
            notifySettingsChanged()
        }
    }
    
    private fun setupTrackpadSettings() {
        // Trackpad Color
        binding.trackpadColorCard.setOnClickListener {
            showTrackpadColorDialog()
        }
        
        // Trackpad Rounding
        binding.trackpadRoundingSlider.apply {
            max = 100 // 0-100 pixels
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    binding.trackpadRoundingValue.text = "${progress}px"
                    if (fromUser) {
                        Preferences.setTrackpadRounding(this@SettingsActivity, progress)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Trackpad X Position
        binding.trackpadXSlider.apply {
            max = 1000 // Will be adjusted based on screen
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    binding.trackpadXValue.text = "${progress}px"
                    if (fromUser) {
                        Preferences.setTrackpadX(this@SettingsActivity, progress)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Trackpad Y Position
        binding.trackpadYSlider.apply {
            max = 2000 // Will be adjusted based on screen
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    binding.trackpadYValue.text = "${progress}px"
                    if (fromUser) {
                        Preferences.setTrackpadY(this@SettingsActivity, progress)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Trackpad Opacity
        binding.trackpadOpacitySlider.apply {
            max = 80 // 20-100%
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val opacity = 20 + progress
                    binding.trackpadOpacityValue.text = "$opacity%"
                    if (fromUser) {
                        Preferences.setTrackpadOpacity(this@SettingsActivity, opacity)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Trackpad Width
        binding.trackpadWidthSlider.apply {
            max = 350 // 50-400
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val width = 50 + progress
                    binding.trackpadWidthValue.text = "${width}px"
                    if (fromUser) {
                        Preferences.setTrackpadWidth(this@SettingsActivity, width)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Trackpad Height
        binding.trackpadHeightSlider.apply {
            max = 350 // 50-400
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val height = 50 + progress
                    binding.trackpadHeightValue.text = "${height}px"
                    if (fromUser) {
                        Preferences.setTrackpadHeight(this@SettingsActivity, height)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Show Drag Handle
        binding.trackpadDragSwitch.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setTrackpadShowDrag(this, isChecked)
            notifySettingsChanged()
        }
        
        // Two-Finger Drag
        binding.trackpadTwoFingerDragSwitch.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setTrackpadTwoFingerDrag(this, isChecked)
            notifySettingsChanged()
        }
        
        // Custom Trackpad Image
        binding.customTrackpadBtn.setOnClickListener {
            customTrackpadPicker.launch(arrayOf("image/*"))
        }
        binding.customTrackpadClearBtn.setOnClickListener {
            Preferences.setCustomTrackpadUri(this, null)
            Preferences.setTrackpadStandaloneImage(this, false)
            updateCustomTrackpadStatus()
            updateStandaloneImageVisibility()
            notifySettingsChanged()
        }
        
        // Standalone Image Toggle
        binding.standaloneImageSwitch.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setTrackpadStandaloneImage(this, isChecked)
            updateStandaloneImageVisibility()
            notifySettingsChanged()
        }
        
        // Image Size Slider
        binding.trackpadImageSizeSlider.apply {
            max = 450 // 50-500
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = 50 + progress
                    binding.trackpadImageSizeValue.text = "${size}px"
                    if (fromUser) {
                        Preferences.setTrackpadImageSize(this@SettingsActivity, size)
                        notifySettingsChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }
    
    private fun setupKeyboardSettings() {
        // These remain for keyboard mode
        binding.toggleKeyCard.setOnClickListener {
            showKeySelectionDialog("Toggle Mouse Mode Key") { keyCode ->
                Preferences.setToggleKeycode(this, keyCode)
                binding.toggleKeyValue.text = Preferences.getKeyName(keyCode)
            }
        }
        
        binding.clickKeyCard.setOnClickListener {
            showKeySelectionDialog("Left Click Key") { keyCode ->
                Preferences.setClickKeycode(this, keyCode)
                binding.clickKeyValue.text = Preferences.getKeyName(keyCode)
            }
        }
        
        binding.rightClickKeyCard.setOnClickListener {
            showKeySelectionDialog("Right Click Key") { keyCode ->
                Preferences.setRightClickKeycode(this, keyCode)
                binding.rightClickKeyValue.text = Preferences.getKeyName(keyCode)
            }
        }
        
        binding.scrollModeKeyCard.setOnClickListener {
            showKeySelectionDialog("Scroll Mode Key") { keyCode ->
                Preferences.setScrollModeKeycode(this, keyCode)
                binding.scrollModeKeyValue.text = Preferences.getKeyName(keyCode)
            }
        }
    }
    
    private fun loadSettings() {
        // Input Mode
        updateInputModeDisplay()
        
        // Pointer Speed
        val speed = Preferences.getPointerSpeed(this)
        val speedProgress = ((speed - 0.5f) / 0.05f).toInt()
        binding.speedSlider.progress = speedProgress
        binding.speedValue.text = String.format("%.1fx", speed)
        
        // Pointer Size
        val size = Preferences.getPointerSize(this)
        binding.sizeSlider.progress = size - 16
        binding.sizeValue.text = "${size}px"
        
        // Pointer Image
        updatePointerImageDisplay()
        
        // Trackpad settings
        updateTrackpadColorDisplay()
        
        val opacity = Preferences.getTrackpadOpacity(this)
        binding.trackpadOpacitySlider.progress = opacity - 20
        binding.trackpadOpacityValue.text = "$opacity%"
        
        val rounding = Preferences.getTrackpadRounding(this)
        binding.trackpadRoundingSlider.progress = rounding
        binding.trackpadRoundingValue.text = "${rounding}px"
        
        val width = Preferences.getTrackpadWidth(this)
        binding.trackpadWidthSlider.progress = width - 50
        binding.trackpadWidthValue.text = "${width}px"
        
        val height = Preferences.getTrackpadHeight(this)
        binding.trackpadHeightSlider.progress = height - 50
        binding.trackpadHeightValue.text = "${height}px"
        
        val posX = Preferences.getTrackpadX(this)
        binding.trackpadXSlider.progress = posX
        binding.trackpadXValue.text = "${posX}px"
        
        val posY = Preferences.getTrackpadY(this)
        binding.trackpadYSlider.progress = posY
        binding.trackpadYValue.text = "${posY}px"
        
        binding.trackpadDragSwitch.isChecked = Preferences.getTrackpadShowDrag(this)
        binding.trackpadTwoFingerDragSwitch.isChecked = Preferences.getTrackpadTwoFingerDrag(this)
        
        // Custom images
        updateCustomPointerStatus()
        updateCustomTrackpadStatus()
        
        // Standalone image settings
        binding.standaloneImageSwitch.isChecked = Preferences.getTrackpadStandaloneImage(this)
        val imageSize = Preferences.getTrackpadImageSize(this)
        binding.trackpadImageSizeSlider.progress = imageSize - 50
        binding.trackpadImageSizeValue.text = "${imageSize}px"
        updateStandaloneImageVisibility()
        
        // Key bindings
        binding.toggleKeyValue.text = Preferences.getKeyName(Preferences.getToggleKeycode(this))
        binding.clickKeyValue.text = Preferences.getKeyName(Preferences.getClickKeycode(this))
        binding.rightClickKeyValue.text = Preferences.getKeyName(Preferences.getRightClickKeycode(this))
        binding.scrollModeKeyValue.text = Preferences.getKeyName(Preferences.getScrollModeKeycode(this))
        
        // Behavior
        binding.vibrateSwitch.isChecked = Preferences.getVibrateOnClick(this)
        binding.autoHideSwitch.isChecked = Preferences.getAutoHideCursor(this)
    }
    
    private fun updateVisibility() {
        val isVirtual = Preferences.isVirtualMode(this)
        
        // Show/hide sections based on mode
        binding.virtualTrackpadSection.visibility = if (isVirtual) View.VISIBLE else View.GONE
        binding.keyboardSection.visibility = if (!isVirtual) View.VISIBLE else View.GONE
    }
    
    private fun updateInputModeDisplay() {
        val mode = Preferences.getInputMode(this)
        binding.inputModeValue.text = when (mode) {
            Preferences.INPUT_MODE_VIRTUAL -> getString(R.string.input_mode_virtual)
            Preferences.INPUT_MODE_KEYBOARD -> getString(R.string.input_mode_keyboard)
            else -> getString(R.string.input_mode_virtual)
        }
    }
    
    private fun updatePointerImageDisplay() {
        val imageName = when (Preferences.getPointerImage(this)) {
            Preferences.POINTER_DEFAULT -> getString(R.string.pointer_default)
            Preferences.POINTER_CHEESE -> getString(R.string.pointer_cheese)
            Preferences.POINTER_DOT -> getString(R.string.pointer_dot)
            Preferences.POINTER_HAND -> getString(R.string.pointer_hand)
            Preferences.POINTER_CUSTOM -> getString(R.string.pointer_custom)
            else -> getString(R.string.pointer_default)
        }
        binding.pointerImageValue.text = imageName
    }
    
    private fun updateCustomPointerStatus() {
        val uri = Preferences.getCustomPointerUri(this)
        if (uri != null) {
            binding.customPointerStatus.text = getString(R.string.image_set)
            binding.customPointerClearBtn.visibility = View.VISIBLE
        } else {
            binding.customPointerStatus.text = getString(R.string.no_image)
            binding.customPointerClearBtn.visibility = View.GONE
        }
    }
    
    private fun updateCustomTrackpadStatus() {
        val uri = Preferences.getCustomTrackpadUri(this)
        if (uri != null) {
            binding.customTrackpadStatus.text = getString(R.string.image_set)
            binding.customTrackpadClearBtn.visibility = View.VISIBLE
            binding.standaloneImageSection.visibility = View.VISIBLE
        } else {
            binding.customTrackpadStatus.text = getString(R.string.no_image)
            binding.customTrackpadClearBtn.visibility = View.GONE
            binding.standaloneImageSection.visibility = View.GONE
        }
    }
    
    private fun updateStandaloneImageVisibility() {
        val hasImage = Preferences.getCustomTrackpadUri(this) != null
        val isStandalone = Preferences.getTrackpadStandaloneImage(this)
        
        if (hasImage && isStandalone) {
            // Standalone mode: hide width/height, show image size
            binding.trackpadSizeSection.visibility = View.GONE
            binding.imageSizeSection.visibility = View.VISIBLE
        } else {
            // Mask mode or no image: show width/height, hide image size
            binding.trackpadSizeSection.visibility = View.VISIBLE
            binding.imageSizeSection.visibility = View.GONE
        }
    }
    
    private fun updateTrackpadColorDisplay() {
        val colorType = Preferences.getTrackpadColor(this)
        val colorName = when (colorType) {
            Preferences.COLOR_CYAN -> getString(R.string.color_cyan)
            Preferences.COLOR_PURPLE -> getString(R.string.color_purple)
            Preferences.COLOR_GREEN -> getString(R.string.color_green)
            Preferences.COLOR_ORANGE -> getString(R.string.color_orange)
            Preferences.COLOR_WHITE -> getString(R.string.color_white)
            Preferences.COLOR_DARK -> getString(R.string.color_dark)
            Preferences.COLOR_TRANSPARENT -> getString(R.string.color_transparent)
            Preferences.COLOR_CUSTOM -> Preferences.getCustomTrackpadColorHex(this)
            else -> getString(R.string.color_cyan)
        }
        binding.trackpadColorValue.text = colorName
        
        // Set color preview (checkerboard for transparent)
        val colorValue = Preferences.getTrackpadColorValue(this)
        if (colorType == Preferences.COLOR_TRANSPARENT) {
            binding.trackpadColorPreview.setBackgroundColor(Color.LTGRAY)
        } else {
            binding.trackpadColorPreview.setBackgroundColor(colorValue)
        }
    }
    
    private fun showInputModeDialog() {
        val options = arrayOf(
            getString(R.string.input_mode_virtual),
            getString(R.string.input_mode_keyboard)
        )
        
        val currentSelection = Preferences.getInputMode(this)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.input_mode))
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                Preferences.setInputMode(this, which)
                updateInputModeDisplay()
                updateVisibility()
                notifySettingsChanged()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPointerImageDialog() {
        val hasCustomImage = Preferences.getCustomPointerUri(this) != null
        
        val options = if (hasCustomImage) {
            arrayOf(
                getString(R.string.pointer_default),
                getString(R.string.pointer_cheese),
                getString(R.string.pointer_dot),
                getString(R.string.pointer_hand),
                getString(R.string.pointer_custom)
            )
        } else {
            arrayOf(
                getString(R.string.pointer_default),
                getString(R.string.pointer_cheese),
                getString(R.string.pointer_dot),
                getString(R.string.pointer_hand)
            )
        }
        
        val currentSelection = Preferences.getPointerImage(this).let {
            if (!hasCustomImage && it == Preferences.POINTER_CUSTOM) 0 else it
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pointer_image))
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                Preferences.setPointerImage(this, which)
                updatePointerImageDisplay()
                notifySettingsChanged()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTrackpadColorDialog() {
        val options = arrayOf(
            getString(R.string.color_cyan),
            getString(R.string.color_purple),
            getString(R.string.color_green),
            getString(R.string.color_orange),
            getString(R.string.color_white),
            getString(R.string.color_dark),
            getString(R.string.color_transparent),
            getString(R.string.color_custom)
        )
        
        val currentSelection = Preferences.getTrackpadColor(this)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.trackpad_color))
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                if (which == Preferences.COLOR_CUSTOM) {
                    dialog.dismiss()
                    showCustomColorDialog()
                } else {
                    Preferences.setTrackpadColor(this, which)
                    updateTrackpadColorDisplay()
                    notifySettingsChanged()
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCustomColorDialog() {
        val currentHex = Preferences.getCustomTrackpadColorHex(this)
        
        val editText = android.widget.EditText(this).apply {
            setText(currentHex)
            hint = "#RRGGBB"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            filters = arrayOf(android.text.InputFilter.LengthFilter(7))
        }
        
        val container = android.widget.FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(editText)
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.custom_color))
            .setMessage(getString(R.string.enter_hex_color))
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                var hex = editText.text.toString().trim()
                if (!hex.startsWith("#")) hex = "#$hex"
                
                try {
                    // Validate color
                    android.graphics.Color.parseColor(hex)
                    Preferences.setCustomTrackpadColor(this, hex)
                    Preferences.setTrackpadColor(this, Preferences.COLOR_CUSTOM)
                    updateTrackpadColorDisplay()
                    notifySettingsChanged()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Invalid color format", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showKeySelectionDialog(title: String, onKeySelected: (Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_key_capture, null)
        val messageText = dialogView.findViewById<TextView>(R.id.captureMessage)
        var capturedKey: Int? = null
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                capturedKey?.let { onKeySelected(it) }
            }
            .create()
        
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode != android.view.KeyEvent.KEYCODE_BACK) {
                capturedKey = keyCode
                messageText.text = "Captured: ${Preferences.getKeyName(keyCode)}\n\nPress Save to confirm"
                true
            } else {
                false
            }
        }
        
        dialog.show()
    }
    
    private fun notifySettingsChanged() {
        sendBroadcast(Intent(MouseAccessibilityService.ACTION_UPDATE_SETTINGS).apply {
            setPackage(packageName)
        })
    }
}
