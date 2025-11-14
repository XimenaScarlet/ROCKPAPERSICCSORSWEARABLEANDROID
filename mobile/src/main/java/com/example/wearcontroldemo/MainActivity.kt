package com.example.wearcontroldemo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private var phoneChoice: String? = null
    private var watchChoice: String? = null

    // Juego
    private lateinit var btnRock: Button
    private lateinit var btnPaper: Button
    private lateinit var btnScissors: Button
    private lateinit var btnReset: Button
    private lateinit var txtTitle: TextView
    private lateinit var txtStatus: TextView

    // Menú
    private lateinit var rootLayout: LinearLayout
    private lateinit var menuLayout: View
    private lateinit var gameLayout: View
    private lateinit var btnPlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Root y layouts
        rootLayout = findViewById(R.id.rootLayout)
        menuLayout = findViewById(R.id.menuLayout)
        gameLayout = findViewById(R.id.gameLayout)

        // Menú
        btnPlay = findViewById(R.id.btnPlay)

        // Juego
        txtTitle = findViewById(R.id.txtTitle)
        txtStatus = findViewById(R.id.txtStatus)
        btnRock = findViewById(R.id.btnRock)
        btnPaper = findViewById(R.id.btnPaper)
        btnScissors = findViewById(R.id.btnScissors)
        btnReset = findViewById(R.id.btnReset)

        // Menú
        btnPlay.setOnClickListener {
            showGame()
            resetState()
        }

        // Juego
        btnRock.setOnClickListener { pick("ROCK") }
        btnPaper.setOnClickListener { pick("PAPER") }
        btnScissors.setOnClickListener { pick("SCISSORS") }

        btnReset.setOnClickListener {
            resetState()
            showMenu()   // al terminar una ronda regresa al menú
        }

        // Estado inicial
        showMenu()
        resetState()
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    // ---------- Navegación de pantallas ----------

    private fun showMenu() {
        menuLayout.visibility = View.VISIBLE
        gameLayout.visibility = View.GONE
    }

    private fun showGame() {
        menuLayout.visibility = View.GONE
        gameLayout.visibility = View.VISIBLE
    }

    // ---------- Juego y comunicación con el reloj ----------

    private fun pick(choice: String) {
        phoneChoice = choice
        txtStatus.text = "Calculando ganador…"
        sendToWear("RPS:PHONE:$choice")
        recompute()
    }

    // Enviar mensaje al reloj
    private fun sendToWear(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
            for (n in nodes) {
                Wearable.getMessageClient(this@MainActivity)
                    .sendMessage(n.id, "/cmd", text.toByteArray())
                    .await()
            }
        }
    }

    // Mensajes que llegan del reloj
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/cmd") return
        val cmd = String(event.data)

        when {
            cmd.startsWith("RPS:WATCH:") -> {
                val choice = cmd.substringAfter("RPS:WATCH:")
                runOnUiThread {
                    watchChoice = choice
                    txtStatus.text = "Ahora elige en el teléfono…"
                    showGame()
                    setButtonsEnabled(true)
                    recompute()
                }
            }

            cmd == "RPS:RESET" -> {
                runOnUiThread {
                    resetState()
                    showMenu()
                }
            }
        }
    }

    private fun recompute() {
        if (phoneChoice == null || watchChoice == null) {
            btnReset.visibility = View.GONE
            return
        }

        val p = phoneChoice!!
        val w = watchChoice!!

        val winner = when {
            p == w -> "Empate"
            (p == "ROCK" && w == "SCISSORS") ||
                    (p == "PAPER" && w == "ROCK") ||
                    (p == "SCISSORS" && w == "PAPER") -> "Teléfono"
            else -> "Reloj"
        }

        txtStatus.text = "Ganador: $winner"
        btnReset.visibility = View.VISIBLE
        setButtonsEnabled(false)

        val resultMsg = when (winner) {
            "Teléfono" -> "RPS:RESULT:PHONE"
            "Reloj" -> "RPS:RESULT:WATCH"
            else -> "RPS:RESULT:DRAW"
        }
        sendToWear(resultMsg)
    }

    private fun resetState() {
        phoneChoice = null
        watchChoice = null
        txtStatus.text = "Esperando al reloj…"
        setButtonsEnabled(false)
        btnReset.visibility = View.GONE
    }

    private fun setButtonsEnabled(v: Boolean) {
        btnRock.isEnabled = v
        btnPaper.isEnabled = v
        btnScissors.isEnabled = v
        val alpha = if (v) 1.0f else 0.4f
        btnRock.alpha = alpha
        btnPaper.alpha = alpha
        btnScissors.alpha = alpha
    }
}
