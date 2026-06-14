package com.brainsmooch.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.brainsmooch.R
import com.brainsmooch.data.GuardState

class BlockGuardAccessibilityService : AccessibilityService() {

    private val appLabel by lazy { getString(R.string.app_name) }
    private var lastToastAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!GuardState.isBlockActive(this)) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val cls = event.className?.toString() ?: ""

        // Check for blocked apps
        val blockedApps = GuardState.getBlockedApps(this)
        if (pkg in blockedApps) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            notifyBlocked(isApp = true)
            return
        }

        // Only block bypass screens in hardcore mode
        if (!GuardState.isHardcore(this)) return

        if (isDangerScreen(pkg, cls)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            notifyBlocked(isApp = false)
        }
    }

    private fun isDangerScreen(pkg: String, cls: String): Boolean {
        val lower = cls.lowercase()

        if (pkg.contains("packageinstaller")) {
            return windowMentionsUs()
        }

        if (pkg == "com.android.settings" || pkg.endsWith(".settings")) {
            if (lower.contains("deviceadmin")) return true
            if (lower.contains("accessibilit")) return true
            if (lower.contains("vpn")) return true
            if (lower.contains("appinfo") ||
                lower.contains("installedappdetails") ||
                lower.contains("appdashboard") ||
                cls.contains("SubSettings")
            ) {
                return windowMentionsUs()
            }
        }
        return false
    }

    private fun windowMentionsUs(): Boolean {
        val root = rootInActiveWindow ?: return false
        return nodeContainsText(root, appLabel) || nodeContainsText(root, packageName)
    }

    private fun nodeContainsText(node: AccessibilityNodeInfo?, needle: String): Boolean {
        node ?: return false
        node.text?.toString()?.let { if (it.contains(needle, ignoreCase = true)) return true }
        node.contentDescription?.toString()?.let { if (it.contains(needle, ignoreCase = true)) return true }
        for (i in 0 until node.childCount) {
            if (nodeContainsText(node.getChild(i), needle)) return true
        }
        return false
    }

    private fun notifyBlocked(isApp: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastToastAt < 2000) return
        lastToastAt = now
        val msg = if (isApp) getString(R.string.app_blocked_toast) else getString(R.string.guard_blocked_toast)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {}

    companion object {
        fun isEnabled(context: Context): Boolean {
            val expected = "${context.packageName}/${BlockGuardAccessibilityService::class.java.name}"
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
