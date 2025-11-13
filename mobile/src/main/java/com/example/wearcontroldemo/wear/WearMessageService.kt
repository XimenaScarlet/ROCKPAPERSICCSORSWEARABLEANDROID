package com.example.wearcontroldemo.wear

import android.content.Intent
import android.widget.Toast
import com.example.wearcontroldemo.MainActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/cmd") return
        val cmd = String(event.data ?: ByteArray(0))

        when {
            cmd == "RPS:RESET" -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("from_wear", "RESET")
                }
                startActivity(intent)
            }
            cmd.startsWith("RPS:WATCH:") -> {
                val choice = cmd.substringAfter("RPS:WATCH:")
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("from_wear", "WATCH:$choice")
                }
                startActivity(intent)
            }
            else -> {
                Toast.makeText(applicationContext, cmd, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
