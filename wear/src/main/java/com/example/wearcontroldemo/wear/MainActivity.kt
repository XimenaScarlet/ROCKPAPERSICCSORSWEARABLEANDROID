package com.example.wearcontroldemo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

private enum class Screen { MENU, GAME, SETTINGS, THEME }

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private val statusState = mutableStateOf("")
    private val showResetState = mutableStateOf(false)
    private val buttonsEnabledState = mutableStateOf(true)
    private val currentScreenState = mutableStateOf(Screen.MENU)
    private val backgroundColorState = mutableStateOf(Color(0xFF121212))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val status by statusState
                val showReset by showResetState
                val buttonsEnabled by buttonsEnabledState
                val currentScreen by currentScreenState
                val bgColor by backgroundColorState

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    when (currentScreen) {

                        Screen.MENU -> MenuScreen(
                            onPlay = {
                                resetAll()
                                currentScreenState.value = Screen.GAME
                            },
                            onSettings = { currentScreenState.value = Screen.SETTINGS }
                        )

                        Screen.GAME -> GameScreen(
                            status = status,
                            showReset = showReset,
                            buttonsEnabled = buttonsEnabled,
                            onChoose = { choose(it) },
                            onPlayAgain = { resetAll() },
                            onBack = { currentScreenState.value = Screen.MENU }
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            onTema = { currentScreenState.value = Screen.THEME },
                            onBack = { currentScreenState.value = Screen.MENU }
                        )

                        Screen.THEME -> ThemeScreen(
                            currentColor = bgColor,
                            onColorSelected = { backgroundColorState.value = it },
                            onBack = { currentScreenState.value = Screen.SETTINGS }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------
    // MENÚ PRINCIPAL
    // -------------------------------------------------------------------

    @Composable
    private fun MenuScreen(onPlay: () -> Unit, onSettings: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Piedra · Papel · Tijera", color = Color.White)

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                IconButtonCircle(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Jugar")
                }
                IconButtonCircle(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Configuración")
                }
            }
        }
    }

    @Composable
    private fun IconButtonCircle(onClick: () -> Unit, icon: @Composable () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFEAEAEA),
                contentColor = Color.Black
            )
        ) {
            icon()
        }
    }

    // -------------------------------------------------------------------
    // GAME SCREEN
    // -------------------------------------------------------------------

    @Composable
    private fun GameScreen(
        status: String,
        showReset: Boolean,
        buttonsEnabled: Boolean,
        onChoose: (String) -> Unit,
        onPlayAgain: () -> Unit,
        onBack: () -> Unit
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Piedra · Papel · Tijera", color = Color.White)

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Solo mostramos los botones de jugada mientras no haya ganador
            if (!showReset) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RpsButton(buttonsEnabled, { onChoose("ROCK") }) {
                        Icon(Icons.Default.SportsMma, contentDescription = "Piedra")
                    }
                    RpsButton(buttonsEnabled, { onChoose("PAPER") }) {
                        Icon(Icons.Default.Description, contentDescription = "Papel")
                    }
                    RpsButton(buttonsEnabled, { onChoose("SCISSORS") }) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Tijera")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Mensaje de estado
            if (status == "Enviado...") {
                Text(
                    status,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (status.isNotEmpty()) {
                Text(status, color = Color.White)
            }

            // Cuando hay ganador: solo botón "Jugar de nuevo" con ícono de play
            if (showReset) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onPlayAgain) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Jugar de nuevo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jugar de nuevo")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón Atrás SOLO cuando NO hay ganador
            if (!showReset) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEAEAEA),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
            }
        }
    }

    @Composable
    private fun RpsButton(
        enabled: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(52.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFEAEAEA),
                contentColor = Color.Black
            )
        ) { icon() }
    }

    // -------------------------------------------------------------------
    // SETTINGS
    // -------------------------------------------------------------------

    @Composable
    private fun SettingsScreen(onTema: () -> Unit, onBack: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Configuración", color = Color.White)

            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = onTema) { Text("Tema") }

            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = onBack) { Text("Volver") }
        }
    }

    // -------------------------------------------------------------------
    // THEME SCREEN (scroll infinito)
    // -------------------------------------------------------------------

    @Composable
    private fun ThemeScreen(
        currentColor: Color,
        onColorSelected: (Color) -> Unit,
        onBack: () -> Unit
    ) {
        val baseColors = listOf(
            Color(0xFF121212),
            Color(0xFF512DA8),
            Color(0xFF0D47A1),
            Color(0xFF1B5E20),
            Color(0xFFB71C1C),
            Color(0xFFF57F17)
        )

        val infiniteList = remember { List(20) { baseColors }.flatten() }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = 60)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Tema", color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(infiniteList.size) { i ->
                    val c = infiniteList[i]

                    Button(
                        onClick = { onColorSelected(c) },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = c)
                    ) {
                        if (c == currentColor) Text("✓")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onBack) { Text("Volver") }
        }
    }

    // -------------------------------------------------------------------
    // COMUNICACIÓN WATCH → PHONE
    // -------------------------------------------------------------------

    private fun send(msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
            for (n in nodes) {
                Wearable.getMessageClient(this@MainActivity)
                    .sendMessage(n.id, "/cmd", msg.toByteArray()).await()
            }
        }
    }

    private fun choose(c: String) {
        if (!buttonsEnabledState.value) return

        buttonsEnabledState.value = false
        statusState.value = "Enviado..."
        showResetState.value = false
        send("RPS:WATCH:$c")
    }

    private fun resetAll() {
        buttonsEnabledState.value = true
        statusState.value = ""
        showResetState.value = false
        send("RPS:RESET")
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/cmd") return
        val cmd = String(event.data)

        runOnUiThread {
            when (cmd) {
                "RPS:RESULT:PHONE" -> {
                    statusState.value = "Ganador: Teléfono"
                    showResetState.value = true
                }
                "RPS:RESULT:WATCH" -> {
                    statusState.value = "Ganador: Reloj"
                    showResetState.value = true
                }
                "RPS:RESULT:DRAW" -> {
                    statusState.value = "Ganador: Empate"
                    showResetState.value = true
                }
            }
        }
    }
}

