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
import androidx.compose.ui.graphics.luminance
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

    // Detecta si un color es oscuro
    private fun Color.isDark() = this.luminance() < 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val status by statusState
            val showReset by showResetState
            val buttonsEnabled by buttonsEnabledState
            val currentScreen by currentScreenState
            val bgColor by backgroundColorState

            val textColor = if (bgColor.isDark()) Color.White else Color.Black
            val iconColor = textColor

            MaterialTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    when (currentScreen) {

                        Screen.MENU -> MenuScreen(
                            textColor,
                            iconColor,
                            onPlay = {
                                resetAll()
                                currentScreenState.value = Screen.GAME
                            },
                            onSettings = { currentScreenState.value = Screen.SETTINGS }
                        )

                        Screen.GAME -> GameScreen(
                            status,
                            showReset,
                            buttonsEnabled,
                            textColor,
                            iconColor,
                            onChoose = { choose(it) },
                            onPlayAgain = {
                                resetAll()
                                currentScreenState.value = Screen.GAME
                            },
                            onBack = { currentScreenState.value = Screen.MENU }
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            textColor,
                            iconColor,
                            onTema = { currentScreenState.value = Screen.THEME },
                            onBack = { currentScreenState.value = Screen.MENU }
                        )

                        Screen.THEME -> ThemeScreen(
                            currentColor = bgColor,
                            textColor = textColor,
                            iconColor = iconColor,
                            onColorSelected = { backgroundColorState.value = it },
                            onBack = { currentScreenState.value = Screen.SETTINGS }
                        )
                    }
                }
            }
        }
    }

    // ---------------- MENÚ ----------------

    @Composable
    private fun MenuScreen(
        textColor: Color,
        iconColor: Color,
        onPlay: () -> Unit,
        onSettings: () -> Unit
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Piedra · Papel · Tijera", color = textColor)

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButtonCircle(onPlay, iconColor) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Jugar", tint = iconColor)
                }
                IconButtonCircle(onSettings, iconColor) {
                    Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = iconColor)
                }
            }
        }
    }

    @Composable
    private fun IconButtonCircle(
        onClick: () -> Unit,
        iconColor: Color,
        icon: @Composable () -> Unit
    ) {
        Button(
            onClick,
            Modifier.size(52.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFEAEAEA),
                contentColor = iconColor
            )
        ) { icon() }
    }

    // ---------------- GAME ----------------

    @Composable
    private fun GameScreen(
        status: String,
        showReset: Boolean,
        buttonsEnabled: Boolean,
        textColor: Color,
        iconColor: Color,
        onChoose: (String) -> Unit,
        onPlayAgain: () -> Unit,
        onBack: () -> Unit
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Piedra · Papel · Tijera", color = textColor)

            Spacer(Modifier.height(12.dp))

            // ⭐ Botones SOLO si NO hay ganador
            if (!showReset) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RpsButton(buttonsEnabled, { onChoose("ROCK") }, iconColor) {
                        Icon(Icons.Default.SportsMma, contentDescription = "Piedra", tint = iconColor)
                    }
                    RpsButton(buttonsEnabled, { onChoose("PAPER") }, iconColor) {
                        Icon(Icons.Default.Description, contentDescription = "Papel", tint = iconColor)
                    }
                    RpsButton(buttonsEnabled, { onChoose("SCISSORS") }, iconColor) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Tijera", tint = iconColor)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (status.isNotEmpty())
                Text(status, color = textColor)

            Spacer(Modifier.height(10.dp))

            // ⭐ ESTADO DE GANADOR → Solo mostrar PLAY
            if (showReset) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.size(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEAEAEA),
                        contentColor = iconColor
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = iconColor)
                }
            }

            // ⭐ IMPORTANTE: NO hay botón de regresar en ganador
            if (!showReset) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEAEAEA),
                        contentColor = iconColor
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = iconColor)
                }
            }
        }
    }

    @Composable
    private fun RpsButton(
        enabled: Boolean,
        onClick: () -> Unit,
        iconColor: Color,
        icon: @Composable () -> Unit
    ) {
        Button(
            onClick,
            enabled = enabled,
            modifier = Modifier.size(52.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFEAEAEA),
                contentColor = iconColor
            )
        ) { icon() }
    }

    // ---------------- SETTINGS ----------------

    @Composable
    private fun SettingsScreen(
        textColor: Color,
        iconColor: Color,
        onTema: () -> Unit,
        onBack: () -> Unit
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Configuración", color = textColor)

            Spacer(Modifier.height(14.dp))

            Button(onClick = onTema) {
                Text("Tema", color = textColor)
            }

            Spacer(Modifier.height(14.dp))

            // Flecha (sí está en ajustes)
            Button(
                onClick = onBack,
                modifier = Modifier.size(46.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFEAEAEA),
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = iconColor)
            }
        }
    }

    // ---------------- THEME SCREEN ----------------

    @Composable
    private fun ThemeScreen(
        currentColor: Color,
        textColor: Color,
        iconColor: Color,
        onColorSelected: (Color) -> Unit,
        onBack: () -> Unit
    ) {
        val baseColors = listOf(
            Color(0xFF121212),
            Color(0xFFF1F1F1),
            Color(0xFF80D0FF),
            Color(0xFF03DAC5),
            Color(0xFF2196F3),
            Color(0xFFF44336)
        )

        val infiniteList = remember { List(20) { baseColors }.flatten() }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = 50)

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Tema", color = textColor)

            Spacer(Modifier.height(12.dp))

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(infiniteList.size) { i ->
                    val c = infiniteList[i]
                    Button(
                        onClick = { onColorSelected(c) },
                        modifier = Modifier.size(42.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = c)
                    ) {
                        if (c == currentColor) Text("✓", color = textColor)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.size(46.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFEAEAEA),
                    contentColor = iconColor
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = iconColor)
            }
        }
    }

    // ---------------- COMUNICACIÓN ----------------

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

        when (val cmd = String(event.data)) {
            "RPS:RESULT:PHONE" -> {
                runOnUiThread {
                    statusState.value = "Ganador: Teléfono"
                    showResetState.value = true
                }
            }
            "RPS:RESULT:WATCH" -> {
                runOnUiThread {
                    statusState.value = "Ganador: Reloj"
                    showResetState.value = true
                }
            }
            "RPS:RESULT:DRAW" -> {
                runOnUiThread {
                    statusState.value = "Empate"
                    showResetState.value = true
                }
            }
        }
    }
}
