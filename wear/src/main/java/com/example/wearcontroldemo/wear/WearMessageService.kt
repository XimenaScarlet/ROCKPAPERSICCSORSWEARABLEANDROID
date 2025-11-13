package com.example.wearcontroldemo.wear

import android.content.Intent
import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/cmd") return
        val cmd = String(event.data ?: ByteArray(0))
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        when {
            cmd == "RPS:RESET" -> {
                startActivity(intent) // MainActivity se reinicia y pone estado inicial
            }
            cmd.startsWith("RPS:PHONE:") -> {
                val choice = cmd.substringAfter("RPS:PHONE:")
                intent.putExtra("from_phone", "PHONE:$choice")
                startActivity(intent)
            }
            else -> {
                Toast.makeText(applicationContext, cmd, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
