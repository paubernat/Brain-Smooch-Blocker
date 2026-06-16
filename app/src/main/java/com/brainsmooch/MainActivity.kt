package com.brainsmooch

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.brainsmooch.ui.screens.MainScreen
import com.brainsmooch.ui.theme.BrainSmoochTheme
import com.brainsmooch.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onVpnPrepared()
        if (result.resultCode == RESULT_OK) {
            viewModel.onVpnConsentGranted()
        } else {
            viewModel.onVpnConsentDenied()
        }
    }

    private val vpnResumeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onVpnResumed()
        if (result.resultCode == RESULT_OK) {
            viewModel.resumeVpnAfterReboot()
        }
    }

    private val adminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }

    private val alwaysOnVpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.markAlwaysOnVpnConfigured()
    }

    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }

    private val vpnSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User may have disconnected the conflicting VPN — re-check so the warning clears.
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // Launch each consent dialog exactly once per intent (keyed), instead of on every
            // recomposition — launching from the composition body can fire multiple times.
            uiState.vpnPrepareIntent?.let { intent ->
                LaunchedEffect(intent) { vpnPermissionLauncher.launch(intent) }
            }

            uiState.vpnResumeIntent?.let { intent ->
                LaunchedEffect(intent) { vpnResumeLauncher.launch(intent) }
            }

            BrainSmoochTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        uiState = uiState,
                        onTabChange = viewModel::setCurrentTab,
                        onDomainInputChange = viewModel::updateDomainInput,
                        onAddDomain = viewModel::addDomain,
                        onAddDomainToActiveBlock = viewModel::addDomainToActiveBlock,
                        onRemoveDomain = viewModel::removeDomain,
                        onAppSearchChange = viewModel::updateAppSearchQuery,
                        onToggleApp = viewModel::toggleApp,
                        onRemoveApp = viewModel::removeApp,
                        onDaysChange = viewModel::updateDays,
                        onHoursChange = viewModel::updateHours,
                        onMinutesChange = viewModel::updateMinutes,
                        onBlockPasswordChange = viewModel::updateBlockPassword,
                        onBlockPasswordConfirmChange = viewModel::updateBlockPasswordConfirm,
                        onStartBlock = viewModel::beginRequestStart,
                        onStartBlockAfterSmooch = viewModel::beginStartAfterSmooch,
                        onConfirmStep1 = viewModel::confirmStep1,
                        onCancelConfirmation = viewModel::cancelConfirmation,
                        onUnlimitedModeChange = viewModel::setUnlimitedMode,
                        onHardcoreModeChange = viewModel::setHardcoreMode,
                        onEnableAdmin = { adminLauncher.launch(viewModel.adminIntent()) },
                        onEnableAccessibility = {
                            accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onEnableAlwaysOnVpn = {
                            alwaysOnVpnLauncher.launch(viewModel.vpnSettingsIntent())
                        },
                        onEnableUsageStats = {
                            usageStatsLauncher.launch(viewModel.usageStatsIntent())
                        },
                        onReleaseBlock = viewModel::showReleaseDialog,
                        onPanicPasswordSubmit = viewModel::tryPanicRelease,
                        onPanicDismiss = viewModel::dismissPanicDialog,
                        onOpenVpnSettings = {
                            vpnSettingsLauncher.launch(viewModel.vpnSettingsIntent())
                        },
                        onDismissVpnConflict = viewModel::dismissVpnConflictDialog
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.checkVpnResumeNeeded()
    }
}
