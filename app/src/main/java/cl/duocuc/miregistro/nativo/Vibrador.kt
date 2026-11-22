package cl.duocuc.miregistro.nativo

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Vibrador(private val context: Context) {

    private fun obtener(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }

    private fun vibrar(milisegundos: Long) {
        try {
            val vibrador = obtener() ?: return
            if (!vibrador.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(milisegundos)
            }
        } catch (e: Exception) {
        }
    }

    fun exito() {
        vibrar(120)
    }

    fun error() {
        vibrar(350)
    }
}
