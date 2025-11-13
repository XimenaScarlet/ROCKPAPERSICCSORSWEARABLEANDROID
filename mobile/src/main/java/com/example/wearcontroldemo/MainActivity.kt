package com.example.wearcontroldemo

import android.graphics.Color
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

    // Menú / Config
    private lateinit var rootLayout: LinearLayout
    private lateinit var menuLayout: View
    private lateinit var gameLayout: View
    private lateinit var settingsLayout: View

    private lateinit var btnPlay: Button
    private lateinit var btnSettings: Button
    private lateinit var btnTheme: Button
    private lateinit var btnBackSettings: Button

    private val themeColors = listOf(
        Color.parseColor("#303030"),
        Color.parseColor("#4A148C"),
        Color.parseColor("#0D47A1"),
        Color.parseColor("#1B5E20"),
        Color.parseColor("#B71C1C"),
        Color.parseColor("#F57F17")
    )
    private var themeIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Root para tema
        rootLayout = findViewById(R.id.rootLayout)

        // Layouts
        menuLayout = findViewById(R.id.menuLayout)
        gameLayout = findViewById(R.id.gameLayout)
        settingsLayout = findViewById(R.id.settingsLayout)

        // Menú
        btnPlay = findViewById(R.id.btnPlay)
        btnSettings = findViewById(R.id.btnSettings)

        // Configuración
        btnTheme = findViewById(R.id.btnTheme)
        btnBackSettings = findViewById(R.id.btnBackSettings)

        // Juego
        txtTitle = findViewById(R.id.txtTitle)
        txtStatus = findViewById(R.id.txtStatus)
        btnRock = findViewById(R.id.btnRock)
        btnPaper = findViewById(R.id.btnPaper)
        btnScissors = findViewById(R.id.btnScissors)
        btnReset = findViewById(R.id.btnReset)

        // Listeners menú
        btnPlay.setOnClickListener {
            showGame()
            resetState()
        }
        btnSettings.setOnClickListener {
            showSettings()
        }

        // Listeners configuración
        btnTheme.setOnClickListener {
            changeTheme()
        }
        btnBackSettings.setOnClickListener {
            showMenu()
        }

        // Listeners juego
        btnRock.setOnClickListener { pick("ROCK") }
        btnPaper.setOnClickListener { pick("PAPER") }
        btnScissors.setOnClickListener { pick("SCISSORS") }

        btnReset.setOnClickListener {
            resetState()
        }

        // Estado inicial: menú y primer tema
        applyTheme()
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
        settingsLayout.visibility = View.GONE
    }

    private fun showGame() {
        menuLayout.visibility = View.GONE
        gameLayout.visibility = View.VISIBLE
        settingsLayout.visibility = View.GONE
    }

    private fun showSettings() {
        menuLayout.visibility = View.GONE
        gameLayout.visibility = View.GONE
        settingsLayout.visibility = View.VISIBLE
    }

    // ---------- Tema ----------

    private fun changeTheme() {
        themeIndex = (themeIndex + 1) % themeColors.size
        applyTheme()
    }

    private fun applyTheme() {
        rootLayout.setBackgroundColor(themeColors[themeIndex])
    }

    // ---------- Juego y comunicación con el reloj ----------

    private fun pick(choice: String) {
        phoneChoice = choice
        txtStatus.text = "Calculando ganador…"
        // Se envía la jugada al reloj (por si se quiere usar allá)
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
                    showGame()              // por si estás en el menú / config
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
