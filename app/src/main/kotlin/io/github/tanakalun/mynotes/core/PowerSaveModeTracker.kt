package io.github.tanakalun.mynotes.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PowerSaveModeTracker {

    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode.asStateFlow()

    private var receiver: BroadcastReceiver? = null

    fun init(context: Context) {
        if (receiver != null) return
        val ctx = context.applicationContext
        val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isPowerSaveMode.value = powerManager.isPowerSaveMode

        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                _isPowerSaveMode.value = powerManager.isPowerSaveMode
            }
        }
        receiver = r
        ContextCompat.registerReceiver(
            ctx,
            r,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}