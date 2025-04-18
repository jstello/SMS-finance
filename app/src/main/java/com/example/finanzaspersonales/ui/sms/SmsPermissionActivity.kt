package com.example.finanzaspersonales.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.finanzaspersonales.R

/**
 * Activity for testing SMS permissions and functionality
 * This is a temporary activity for Phase 1 implementation
 */
class SmsPermissionActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "SmsPermissionActivity"
    }
    
    private lateinit var statusTextView: TextView
    private lateinit var requestPermissionButton: Button
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsPermissionGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val receiveSmsPermissionGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        
        updatePermissionStatus(smsPermissionGranted && receiveSmsPermissionGranted)
        
        if (smsPermissionGranted && receiveSmsPermissionGranted) {
            Toast.makeText(this, "SMS permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS permissions denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_permission)
        
        statusTextView = findViewById(R.id.status_text)
        requestPermissionButton = findViewById(R.id.request_permission_button)
        
        requestPermissionButton.setOnClickListener {
            requestSmsPermissions()
        }
        
        // Check initial permission status
        updatePermissionStatus(hasSmsPermissions())
    }
    
    /**
     * Request SMS permissions if not already granted
     */
    private fun requestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        
        if (!hasSmsPermissions()) {
            Log.d(TAG, "Requesting SMS permissions")
            requestPermissionLauncher.launch(permissions)
        } else {
            Toast.makeText(this, "SMS permissions already granted!", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check if SMS permissions are granted
     */
    private fun hasSmsPermissions(): Boolean {
        val readSmsPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        val receiveSmsPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        return readSmsPermission && receiveSmsPermission
    }
    
    /**
     * Update the UI to show current permission status
     */
    private fun updatePermissionStatus(isGranted: Boolean) {
        statusTextView.text = if (isGranted) {
            "SMS permissions are granted. The app will detect financial transaction SMS messages."
        } else {
            "SMS permissions are NOT granted. Please grant permissions to detect SMS messages."
        }
    }
} 