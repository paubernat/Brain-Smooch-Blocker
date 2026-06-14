package com.brainsmooch.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class BrainSmoochDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Brain Smooch needs admin privileges to enforce blocking. Disabling will remove all protections."
    }
}
