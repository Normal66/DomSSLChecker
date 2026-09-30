package com.domsslchecker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.domsslchecker.ui.DomSslCheckerAppRoot
import com.domsslchecker.ui.DomainsViewModel
import com.domsslchecker.ui.SettingsViewModel
import com.domsslchecker.ui.theme.DomSSLCheckerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DomainsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            DomSSLCheckerTheme {
                DomSslCheckerAppRoot(vm = viewModel, settingsVm = settingsViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    DomSSLCheckerTheme {
        // Preview placeholder; ViewModel not constructed in preview.
        Text("DomSSLChecker", style = MaterialTheme.typography.headlineMedium)
    }
}

