package com.meteory.optimizer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meteory.optimizer.services.MonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MonitorService.start(context)
        }
    }
}
