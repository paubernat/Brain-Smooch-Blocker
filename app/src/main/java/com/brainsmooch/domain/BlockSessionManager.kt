package com.brainsmooch.domain

import android.content.Context
import com.brainsmooch.data.BlockRepository
import com.brainsmooch.data.GuardState
import com.brainsmooch.receiver.BlockEndReceiver
import com.brainsmooch.service.BlockerVpnService
import com.brainsmooch.service.MdmManager

object BlockSessionManager {

    suspend fun endSession(context: Context, emergency: Boolean = false) {
        BlockRepository(context).endBlock()
        BlockerVpnService.stop(context)
        GuardState.clearBlockedApps(context)
        MdmManager(context).run {
            if (emergency) emergencyRelease() else deactivateLockdown()
        }
        BlockEndReceiver.cancelAlarm(context)
    }
}
