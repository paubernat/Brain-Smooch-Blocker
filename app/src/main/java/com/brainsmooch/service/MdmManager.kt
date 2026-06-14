package com.brainsmooch.service

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.brainsmooch.R
import com.brainsmooch.receiver.BrainSmoochDeviceAdminReceiver

class MdmManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName =
        ComponentName(context, BrainSmoochDeviceAdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponent)

    /**
     * Intent that opens the system dialog to activate Device Admin. Works on a
     * normal personal phone (unlike Device Owner). While the admin is active the
     * app cannot be uninstalled without first deactivating it in Settings — and
     * the accessibility guard blocks that screen during a block.
     */
    fun getEnableAdminIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.admin_explanation)
            )
        }

    fun activateLockdown() {
        if (!isDeviceOwner) return

        // Prevent uninstalling the app
        devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)

        // Prevent VPN configuration changes
        devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_VPN)

        // Prevent date/time changes to cheat the timer
        devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_DATE_TIME)

        // Prevent USB debugging to bypass restrictions
        devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)

        // Prevent safe mode boot
        devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)

        // Prevent factory reset
        devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
    }

    fun deactivateLockdown() {
        if (!isDeviceOwner) return

        devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, false)
        devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_VPN)
        devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_DATE_TIME)
        devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
        devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
        devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
    }

    fun emergencyRelease() {
        deactivateLockdown()
    }
}
