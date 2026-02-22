package com.monstertimer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.monstertimer.app.data.UsageStats
import com.monstertimer.app.service.ShortsAccessibilityService
import java.io.File
import java.io.FileOutputStream
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var timerSlider: Slider
    private lateinit var timerValueText: TextView
    private lateinit var pinEditText: TextInputEditText
    private lateinit var monsterRecyclerView: RecyclerView
    private lateinit var addMonsterButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var accessibilityButton: MaterialButton
    private lateinit var overlayButton: MaterialButton
    private lateinit var statsText: TextView
    private lateinit var timerTypeRadioGroup: RadioGroup
    private lateinit var sliderRadioButton: RadioButton
    private lateinit var manualRadioButton: RadioButton
    private lateinit var manualTimerInputLayout: TextInputLayout
    private lateinit var manualTimerEditText: TextInputEditText

    private val monsterAdapter = MonsterGalleryAdapter()
    private var currentSettings = AppSettings()
    
    private val timerPresets = listOf(5, 10, 15, 20, 30)

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { addMonsterFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadSettings()
        setupUI()

        val settings = AppSettings.load(this)
        if (!settings.eulaAccepted) {
            showEulaDialog()
        } else {
            checkPermissions()
        }
    }

    private fun showEulaDialog() {
        val scrollView = ScrollView(this)
        val eulaTextView = TextView(this).apply {
            text = getString(R.string.eula_text)
            setPadding(48, 32, 48, 32)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
        }
        scrollView.addView(eulaTextView)

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.eula_title)
            .setView(scrollView)
            .setCancelable(false)
            .setPositiveButton("I Accept") { _, _ ->
                val s = AppSettings.load(this)
                val newSettings = s.copy(eulaAccepted = true)
                AppSettings.save(this, newSettings)
                currentSettings = newSettings
                checkPermissions()
            }
            .setNegativeButton("Decline") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateStatsDisplay()
    }

    private fun initViews() {
        // Stats
        statsText = findViewById(R.id.statsText)
        
        // Permissions
        statusText = findViewById(R.id.statusText)
        accessibilityButton = findViewById(R.id.accessibilityButton)
        overlayButton = findViewById(R.id.overlayButton)
        
        // Timer
        timerSlider = findViewById(R.id.timerSlider)
        timerValueText = findViewById(R.id.timerValueText)
        timerTypeRadioGroup = findViewById(R.id.timerTypeRadioGroup)
        sliderRadioButton = findViewById(R.id.sliderRadioButton)
        manualRadioButton = findViewById(R.id.manualRadioButton)
        manualTimerInputLayout = findViewById(R.id.manualTimerInputLayout)
        manualTimerEditText = findViewById(R.id.manualTimerEditText)
        
        // PIN
        pinEditText = findViewById(R.id.pinEditText)
        
        // Monster gallery
        monsterRecyclerView = findViewById(R.id.monsterRecyclerView)
        addMonsterButton = findViewById(R.id.addMonsterButton)
        
        // Save
        saveButton = findViewById(R.id.saveButton)
        
        // Setup RecyclerView
        monsterRecyclerView.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )
        monsterRecyclerView.adapter = monsterAdapter
    }

    private fun setupUI() {
        // Timer type listener
        timerTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.sliderRadioButton) {
                timerSlider.visibility = View.VISIBLE
                manualTimerInputLayout.visibility = View.GONE
                val minutes = timerPresets[timerSlider.value.toInt()]
                timerValueText.text = "$minutes minutes"
            } else {
                timerSlider.visibility = View.GONE
                manualTimerInputLayout.visibility = View.VISIBLE
                val minutes = manualTimerEditText.text.toString().toIntOrNull() ?: 0
                timerValueText.text = "$minutes minutes"
            }
        }

        // Timer slider listener
        timerSlider.addOnChangeListener { _, value, _ ->
            val minutes = timerPresets[value.toInt()]
            timerValueText.text = "$minutes minutes"
        }

        manualTimerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (manualRadioButton.isChecked) {
                    val minutes = s.toString().toIntOrNull() ?: 0
                    timerValueText.text = "$minutes minutes"
                }
            }
        })

        // Monster adapter callback
        monsterAdapter.onRemoveClick = { path ->
            val newPaths = currentSettings.monsterPaths.toMutableList()
            newPaths.remove(path)
            currentSettings = currentSettings.copy(monsterPaths = newPaths)
            monsterAdapter.updateData(newPaths)
        }
        
        // Button click listeners
        accessibilityButton.setOnClickListener { openAccessibilitySettings() }
        overlayButton.setOnClickListener { requestOverlayPermission() }
        addMonsterButton.setOnClickListener { pickMonsterMedia() }
        saveButton.setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        currentSettings = AppSettings.load(this)

        if (currentSettings.isManualTimer) {
            manualRadioButton.isChecked = true
            manualTimerEditText.setText(currentSettings.timerMinutes.toString())
            timerSlider.visibility = View.GONE
            manualTimerInputLayout.visibility = View.VISIBLE
        } else {
            sliderRadioButton.isChecked = true
            val timerIndex = timerPresets.indexOf(currentSettings.timerMinutes)
                .takeIf { it >= 0 } ?: 1
            timerSlider.value = timerIndex.toFloat()
            timerSlider.visibility = View.VISIBLE
            manualTimerInputLayout.visibility = View.GONE
        }

        timerValueText.text = "${currentSettings.timerMinutes} minutes"
        
        pinEditText.setText(currentSettings.parentPin)
        monsterAdapter.updateData(currentSettings.monsterPaths)
    }

    private fun saveSettings() {
        val accessibilityEnabled = ShortsAccessibilityService.isServiceRunning
        val overlayEnabled = Settings.canDrawOverlays(this)

        if (!accessibilityEnabled || !overlayEnabled) {
            Toast.makeText(this, "Please enable Accessibility and Overlay permissions first!", Toast.LENGTH_LONG).show()
            return
        }

        val pin = pinEditText.text.toString()
        if (pin.length != 4) {
            Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        val isManual = manualRadioButton.isChecked
        val timerMinutes = if (isManual) {
            manualTimerEditText.text.toString().toIntOrNull() ?: currentSettings.timerMinutes
        } else {
            timerPresets[timerSlider.value.toInt()]
        }

        currentSettings = currentSettings.copy(
            timerMinutes = timerMinutes,
            isManualTimer = isManual,
            parentPin = pin
        )
        
        AppSettings.save(this, currentSettings)
        
        Toast.makeText(this, "✓ Settings saved!", Toast.LENGTH_SHORT).show()
        
        // Reset parent bypass flag
        ShortsAccessibilityService.isParentBypassed = false
    }

    private fun updateStatsDisplay() {
        val todayStats = UsageStats.getToday(this)
        val weeklyStats = UsageStats.getWeeklyStats(this)
        
        val weeklyTotal = weeklyStats.sumOf { it.totalSecondsWatched }
        val weeklyHours = weeklyTotal / 3600
        val weeklyMinutes = (weeklyTotal % 3600) / 60
        
        val statsBuilder = StringBuilder()
        statsBuilder.append("📺 Today: ${todayStats.formattedWatchTime}\n")
        statsBuilder.append("📅 This week: ${weeklyHours}h ${weeklyMinutes}m\n")
        statsBuilder.append("👹 Monsters shown today: ${todayStats.timesMonsterShown}")
        
        if (todayStats.timesStoppedEarly > 0) {
            statsBuilder.append("\n⭐ Good stops today: ${todayStats.timesStoppedEarly}")
        }
        
        statsText.text = statsBuilder.toString()
    }

    private fun pickMonsterMedia() {
        if (currentSettings.monsterPaths.size >= 5) {
            Toast.makeText(this, "Maximum 5 monsters allowed", Toast.LENGTH_SHORT).show()
            return
        }
        pickMedia.launch("*/*")
    }

    private fun addMonsterFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val fileName = "monster_${System.currentTimeMillis()}.${getExtension(uri)}"
            val file = File(filesDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            val newPaths = currentSettings.monsterPaths.toMutableList()
            newPaths.add(file.absolutePath)
            currentSettings = currentSettings.copy(monsterPaths = newPaths)
            monsterAdapter.updateData(newPaths)
            
            Toast.makeText(this, "Monster added!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to add monster: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri) ?: return "jpg"
        return when {
            mimeType.contains("png") -> "png"
            mimeType.contains("video") -> "mp4"
            mimeType.contains("webm") -> "webm"
            else -> "jpg"
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { }.launch(needed.toTypedArray())
            }
        }
    }

    private fun updatePermissionStatus() {
        val accessibilityEnabled = ShortsAccessibilityService.isServiceRunning
        val overlayEnabled = Settings.canDrawOverlays(this)

        val status = StringBuilder()
        status.append("• Accessibility: ${if (accessibilityEnabled) "✓ Enabled" else "✗ Disabled"}\n")
        status.append("• Overlay: ${if (overlayEnabled) "✓ Enabled" else "✗ Disabled"}")
        
        statusText.text = status.toString()
        
        accessibilityButton.visibility = if (accessibilityEnabled) View.GONE else View.VISIBLE
        overlayButton.visibility = if (overlayEnabled) View.GONE else View.VISIBLE
        
        if (accessibilityEnabled && overlayEnabled) {
            statusText.setTextColor(ContextCompat.getColor(this, R.color.success))
        } else {
            statusText.setTextColor(ContextCompat.getColor(this, R.color.error))
        }
    }

    private fun openAccessibilitySettings() {
        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Enable Accessibility Service")
            .setMessage("Please find 'Monster Timer' in the list and enable it.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    // RecyclerView Adapter for Monster Gallery
    inner class MonsterGalleryAdapter : RecyclerView.Adapter<MonsterGalleryAdapter.ViewHolder>() {
        
        private var paths: List<String> = emptyList()
        var onRemoveClick: ((String) -> Unit)? = null

        fun updateData(newPaths: List<String>) {
            paths = newPaths
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_monster, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = paths[position]
            Glide.with(holder.imageView).load(File(path)).into(holder.imageView)
            holder.deleteButton.setOnClickListener { onRemoveClick?.invoke(path) }
        }

        override fun getItemCount() = paths.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ShapeableImageView = itemView.findViewById(R.id.monsterImage)
            val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        }
    }
}
