package com.brainsmooch.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.app.NotificationCompat
import com.brainsmooch.MainActivity
import com.brainsmooch.R
import com.brainsmooch.data.BlockRepository
import com.brainsmooch.data.GuardState
import com.brainsmooch.domain.BlockSessionManager
import com.brainsmooch.service.BlockerVpnService
import com.brainsmooch.service.MdmManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "brain_smooch_alert"
        private const val NOTIFICATION_ID = 2
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON"
            )
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = BlockRepository(context)
                val state = repository.getBlockStateOnce()

                if (state.isActive && !state.isExpired) {
                    GuardState.setBlockActive(context, true)
                    GuardState.setBlockedApps(context, state.blockedApps.toSet())

                    val mdmManager = MdmManager(context)
                    mdmManager.activateLockdown()

                    val vpnIntent = VpnService.prepare(context)
                    if (vpnIntent == null) {
                        BlockerVpnService.start(
                            context,
                            state.blockedDomains,
                            state.endTimeMillis
                        )
                    } else {
                        showVpnRequiredNotification(context)
                    }

                    BlockEndReceiver.scheduleAlarm(context, state.endTimeMillis)
                } else if (state.isExpired) {
                    BlockSessionManager.endSession(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showVpnRequiredNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alert_channel_description)
        }
        notificationManager.createNotificationChannel(channel)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.vpn_reauth_title))
            .setContentText(context.getString(R.string.vpn_reauth_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
