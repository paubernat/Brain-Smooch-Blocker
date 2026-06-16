package com.brainsmooch.viewmodel

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brainsmooch.data.AppInfo
import com.brainsmooch.data.BlockRepository
import com.brainsmooch.data.BlockState
import com.brainsmooch.data.GuardState
import com.brainsmooch.domain.BlockSessionManager
import com.brainsmooch.domain.Sfx
import com.brainsmooch.receiver.BlockEndReceiver
import com.brainsmooch.service.AppBlockerManager
import com.brainsmooch.service.BlockGuardAccessibilityService
import com.brainsmooch.service.BlockerVpnService
import com.brainsmooch.service.MdmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BlockTab { DOMAINS, APPS }

/** What to run once VPN consent comes back, since both the pre-smooch and post-smooch paths can trigger it. */
private enum class PendingStart { NONE, REQUEST, AFTER_SMOOCH }

data class UiState(
    val blockState: BlockState = BlockState(),
    val remainingMillis: Long = 0L,
    val currentTab: BlockTab = BlockTab.DOMAINS,
    val domainInput: String = "",
    val domains: List<String> = emptyList(),
    val apps: List<String> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val appSearchQuery: String = "",
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 30,
    val unlimitedMode: Boolean = false,
    val blockPassword: String = "",
    val blockPasswordConfirm: String = "",
    val isDeviceOwner: Boolean = false,
    val isAdminActive: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val isAlwaysOnVpnEnabled: Boolean = false,
    val hardcoreMode: Boolean = false,
    val vpnPrepareIntent: Intent? = null,
    val vpnResumeIntent: Intent? = null,
    val showPanicDialog: Boolean = false,
    val confirmationStep: Int = 0,
    val isStarting: Boolean = false,
    val otherVpnActive: Boolean = false,
    val showVpnConflictDialog: Boolean = false
) {
    val isFullyProtected: Boolean
        get() = isAdminActive && isAccessibilityEnabled && isAlwaysOnVpnEnabled

    val hasDuration: Boolean
        get() = unlimitedMode || days > 0 || hours > 0 || minutes > 0

    val hasContent: Boolean
        get() = domains.isNotEmpty() || apps.isNotEmpty()

    val passwordsMatch: Boolean
        get() = blockPassword == blockPasswordConfirm

    val requiresPassword: Boolean
        get() = unlimitedMode || hardcoreMode || days >= 1

    val needsConfirmation: Boolean
        get() = requiresPassword

    val filteredApps: List<AppInfo>
        get() = if (appSearchQuery.isBlank()) installedApps
                else installedApps.filter {
                    it.label.contains(appSearchQuery, ignoreCase = true) ||
                    it.packageName.contains(appSearchQuery, ignoreCase = true)
                }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BlockRepository(application)
    private val mdmManager = MdmManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        private const val MAX_DAYS = 365
        private const val MAX_HOURS = 23
        private const val MAX_MINUTES = 59
        private const val TEST_MODE = false
    }

    init {
        refreshPermissions()
        loadInstalledApps()

        viewModelScope.launch {
            repository.blockState.collect { blockState ->
                _uiState.update {
                    it.copy(
                        blockState = blockState,
                        remainingMillis = blockState.remainingMillis,
                        // Block is live: clear the "starting" overlay so the active screen shows.
                        isStarting = if (blockState.isActive) false else it.isStarting
                    )
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                val blockState = _uiState.value.blockState
                if (blockState.isActive) {
                    _uiState.update { it.copy(remainingMillis = blockState.remainingMillis) }
                    if (blockState.isExpired) endBlock()
                }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    fun setCurrentTab(tab: BlockTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateDomainInput(input: String) {
        _uiState.update { it.copy(domainInput = input) }
    }

    fun updateAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun addDomain() {
        val domain = _uiState.value.domainInput.trim()
        if (domain.isNotBlank() && domain !in _uiState.value.domains) {
            _uiState.update {
                it.copy(
                    domains = it.domains + domain,
                    domainInput = ""
                )
            }
            Sfx.tick(getApplication())
        }
    }

    fun addDomainToActiveBlock() {
        val domain = _uiState.value.domainInput.trim()
        if (domain.isBlank()) return
        viewModelScope.launch {
            val updated = repository.addDomainToActiveBlock(domain)
            if (!TEST_MODE) {
                BlockerVpnService.updateDomains(getApplication(), updated)
            }
            _uiState.update { it.copy(domainInput = "") }
            Sfx.tick(getApplication())
        }
    }

    fun removeDomain(domain: String) {
        _uiState.update { it.copy(domains = it.domains - domain) }
    }

    fun toggleApp(packageName: String) {
        _uiState.update {
            val newApps = if (packageName in it.apps) {
                it.apps - packageName
            } else {
                it.apps + packageName
            }
            it.copy(apps = newApps)
        }
        Sfx.tick(getApplication())
    }

    fun removeApp(packageName: String) {
        _uiState.update { it.copy(apps = it.apps - packageName) }
    }

    fun addAppToActiveBlock(packageName: String) {
        viewModelScope.launch {
            val updated = repository.addAppToActiveBlock(packageName)
            GuardState.setBlockedApps(getApplication(), updated.toSet())
            Sfx.tick(getApplication())
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppBlockerManager.getInstalledApps(getApplication())
            }
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun updateDays(days: Int) {
        _uiState.update { it.copy(days = days.coerceIn(0, MAX_DAYS)) }
    }

    fun updateHours(hours: Int) {
        _uiState.update { it.copy(hours = hours.coerceIn(0, MAX_HOURS)) }
    }

    fun updateMinutes(minutes: Int) {
        _uiState.update { it.copy(minutes = minutes.coerceIn(0, MAX_MINUTES)) }
    }

    fun setUnlimitedMode(enabled: Boolean) {
        _uiState.update { it.copy(unlimitedMode = enabled) }
    }

    fun updateBlockPassword(password: String) {
        _uiState.update { it.copy(blockPassword = password) }
    }

    fun updateBlockPasswordConfirm(password: String) {
        _uiState.update { it.copy(blockPasswordConfirm = password) }
    }

    private var pendingStart = PendingStart.NONE

    /**
     * Ensures VPN consent before [after] runs. Returns true when the caller may proceed
     * immediately; returns false after stashing [after] and triggering the consent dialog.
     * App-only blocks need no VPN (see [executeStartBlock]), so consent is skipped entirely.
     */
    private fun prepareVpn(after: PendingStart): Boolean {
        if (TEST_MODE) return true
        if (_uiState.value.domains.isEmpty()) return true
        val intent = VpnService.prepare(getApplication())
        if (intent != null) {
            pendingStart = after
            _uiState.update { it.copy(vpnPrepareIntent = intent) }
            return false
        }
        return true
    }

    /** Pre-smooch entry (hardcore/unlimited): runs the confirmation flow once VPN is ready. */
    fun beginRequestStart() {
        if (prepareVpn(PendingStart.REQUEST)) requestStartBlock()
    }

    /** Post-smooch entry (all modes): actually starts the block once VPN is ready. */
    fun beginStartAfterSmooch() {
        _uiState.update { it.copy(isStarting = true) }
        if (prepareVpn(PendingStart.AFTER_SMOOCH)) startBlockAfterSmooch()
    }

    fun onVpnPrepared() {
        _uiState.update { it.copy(vpnPrepareIntent = null) }
    }

    fun onVpnConsentGranted() {
        val action = pendingStart
        pendingStart = PendingStart.NONE
        when (action) {
            PendingStart.REQUEST -> requestStartBlock()
            PendingStart.AFTER_SMOOCH -> startBlockAfterSmooch()
            PendingStart.NONE -> {}
        }
    }

    fun onVpnConsentDenied() {
        pendingStart = PendingStart.NONE
        // Recover from the post-smooch "starting" overlay so we don't hang on a blank gradient,
        // and explain the likely cause: another VPN holding the slot.
        _uiState.update { it.copy(isStarting = false, showVpnConflictDialog = true) }
    }

    fun onVpnResumed() {
        _uiState.update { it.copy(vpnResumeIntent = null) }
    }

    fun resumeVpnAfterReboot() {
        val state = _uiState.value.blockState
        if (!state.isActive || state.isExpired) return
        BlockerVpnService.start(getApplication(), state.blockedDomains, state.endTimeMillis)
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(
                isDeviceOwner = mdmManager.isDeviceOwner,
                isAdminActive = mdmManager.isAdminActive,
                isAccessibilityEnabled = BlockGuardAccessibilityService.isEnabled(getApplication()),
                hasUsageStatsPermission = AppBlockerManager.hasUsageStatsPermission(getApplication()),
                isAlwaysOnVpnEnabled = isAlwaysOnVpnEnabled(),
                hardcoreMode = GuardState.isHardcore(getApplication()),
                otherVpnActive = isOtherVpnActive()
            )
        }
    }

    /**
     * True when some *other* app holds the single VPN slot (Android allows only one active VPN).
     * Website blocking can't start until that VPN is disconnected, so the UI warns the user.
     */
    private fun isOtherVpnActive(): Boolean {
        if (BlockerVpnService.running) return false
        val cm = getApplication<Application>()
            .getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    fun dismissVpnConflictDialog() {
        _uiState.update { it.copy(showVpnConflictDialog = false) }
    }

    private fun isAlwaysOnVpnEnabled(): Boolean {
        // No hay API pública para detectar esto sin permisos especiales
        // Usamos SharedPreferences para trackear si el usuario lo configuró
        return getApplication<Application>()
            .getSharedPreferences("guard_state", android.content.Context.MODE_PRIVATE)
            .getBoolean("always_on_vpn_configured", false)
    }

    fun markAlwaysOnVpnConfigured() {
        getApplication<Application>()
            .getSharedPreferences("guard_state", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("always_on_vpn_configured", true)
            .apply()
        refreshPermissions()
    }

    fun vpnSettingsIntent(): Intent = Intent(android.provider.Settings.ACTION_VPN_SETTINGS)

    fun checkVpnResumeNeeded() {
        if (TEST_MODE) return
        val state = _uiState.value.blockState
        if (!state.isActive || state.isExpired) return

        val intent = VpnService.prepare(getApplication())
        if (intent != null) {
            _uiState.update { it.copy(vpnResumeIntent = intent) }
        } else {
            BlockerVpnService.start(getApplication(), state.blockedDomains, state.endTimeMillis)
        }
    }

    fun setHardcoreMode(enabled: Boolean) {
        GuardState.setHardcore(getApplication(), enabled)
        _uiState.update { it.copy(hardcoreMode = enabled) }
    }

    fun adminIntent(): Intent = mdmManager.getEnableAdminIntent()

    fun usageStatsIntent(): Intent = AppBlockerManager.getUsageStatsSettingsIntent()

    fun requestStartBlock() {
        val state = _uiState.value
        if (!canStart(state)) return

        if (state.needsConfirmation) {
            _uiState.update { it.copy(confirmationStep = 1) }
        }
    }

    fun startBlockAfterSmooch() {
        val state = _uiState.value
        if (!canStart(state)) {
            _uiState.update { it.copy(isStarting = false) }
            return
        }
        executeStartBlock()
    }

    fun confirmStep1() {
        _uiState.update { it.copy(confirmationStep = 2) }
    }

    fun cancelConfirmation() {
        _uiState.update { it.copy(confirmationStep = 0) }
    }

    private fun canStart(state: UiState): Boolean {
        if (!state.hasContent || !state.hasDuration || !state.passwordsMatch) return false
        if (state.requiresPassword && state.blockPassword.isBlank()) return false
        return true
    }

    private fun executeStartBlock() {
        val state = _uiState.value

        val durationMillis = if (state.unlimitedMode) {
            Long.MAX_VALUE / 2
        } else {
            ((state.days * 24L + state.hours) * 60L + state.minutes) * 60_000L
        }
        val endTimeMillis = System.currentTimeMillis() + durationMillis
        val passwordHash = state.blockPassword
            .takeIf { it.isNotBlank() }
            ?.let { BlockRepository.sha256(it) }

        Sfx.trabaja(getApplication())

        // Store blocked apps in GuardState for fast access by AccessibilityService
        GuardState.setBlockedApps(getApplication(), state.apps.toSet())

        viewModelScope.launch {
            repository.startBlock(state.domains, state.apps, durationMillis, passwordHash)
            _uiState.update { it.copy(blockPassword = "", blockPasswordConfirm = "") }

            if (TEST_MODE) {
                return@launch
            }

            if (mdmManager.isDeviceOwner) {
                mdmManager.activateLockdown()
            }

            if (state.domains.isNotEmpty()) {
                BlockerVpnService.start(getApplication(), state.domains, endTimeMillis)
            }
            if (!state.unlimitedMode) {
                BlockEndReceiver.scheduleAlarm(getApplication(), endTimeMillis)
            }
        }
    }

    private fun endBlock() {
        viewModelScope.launch {
            BlockSessionManager.endSession(getApplication())
        }
    }

    fun showReleaseDialog() {
        _uiState.update { it.copy(showPanicDialog = true) }
    }

    fun dismissPanicDialog() {
        _uiState.update { it.copy(showPanicDialog = false) }
    }

    fun tryPanicRelease(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val storedHash = repository.getPanicPasswordHash()
            val ok = storedHash != null && BlockRepository.sha256(password) == storedHash
            if (ok) {
                BlockSessionManager.endSession(getApplication(), emergency = true)
                _uiState.update { it.copy(showPanicDialog = false) }
            }
            onResult(ok)
        }
    }
}
