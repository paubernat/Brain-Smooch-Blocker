package com.brainsmooch.domain

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.brainsmooch.R

/** One-shot UI sound effects with a small haptic tick. */
object Sfx {

    /** Plays when a domain/app is added - just haptic, no sound. */
    fun tick(context: Context) {
        vibrate(context)
    }

    /** Plays when the smooch button is pressed. */
    fun smooch(context: Context) = play(context, R.raw.smooch)

    /** Plays when the block begins. */
    fun trabaja(context: Context) = play(context, R.raw.trabaja)

    private fun play(context: Context, resId: Int) {
        MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
