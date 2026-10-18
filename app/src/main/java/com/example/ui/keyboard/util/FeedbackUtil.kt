package com.example.ui.keyboard.util

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

object FeedbackUtil {

    fun performKeyPressFeedback(
        context: Context,
        view: View?,
        soundEnabled: Boolean,
        hapticEnabled: Boolean
    ) {
        // Explicitly ensure view does not play unwanted sound effects when disabled
        view?.isSoundEffectsEnabled = soundEnabled

        if (soundEnabled) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f)
            } catch (_: Exception) {
            }
        }

        if (hapticEnabled) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(12)
                    }
                } else if (view != null) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } catch (_: Exception) {
            }
        }
    }
}
