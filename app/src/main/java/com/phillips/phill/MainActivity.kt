package com.phillips.phill

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phillips.phill.navigation.PhillNavGraph
import com.phillips.phill.sms.SmsSyncManager
import com.phillips.phill.ui.theme.PhillTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var smsSyncManager: SmsSyncManager

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (readGranted || receiveGranted) {
            // Permission granted — bootstrap the Comms tab with device SMS history
            lifecycleScope.launch {
                smsSyncManager.syncAllSmsThreads()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhillTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhillNavGraph()
                }
            }
        }

        // Request SMS permissions at startup
        requestSmsPermissions()
    }

    private fun requestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )

        val anyMissing = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (anyMissing) {
            smsPermissionLauncher.launch(permissions)
        } else {
            // Already granted — sync on every launch to catch messages sent/received
            // outside the app since last open
            lifecycleScope.launch {
                smsSyncManager.syncAllSmsThreads()
            }
        }
    }
}
