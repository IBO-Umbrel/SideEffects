package com.lessons.sideeffects

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SplashScreen() {
    var run by remember { mutableIntStateOf(0) }
    var restartOnCallback by remember { mutableStateOf(false) }
    LessonColumn {
        Text("A new callback is created every 500 ms. Both timers wait 2 seconds.")
        Row {
            Switch(checked = restartOnCallback, onCheckedChange = { restartOnCallback = it; run++ })
            Text("Use callback as captured timer key", Modifier.padding(8.dp))
        }
        Button(onClick = { run++ }) { Text("Restart experiment") }
        key(run) { SplashExperiment(restartOnCallback) }
        Text("With the callback key enabled, each new lambda cancels and restarts the wait. Turn it off to finish.")
    }
}

@Composable
private fun SplashExperiment(restartOnCallback: Boolean) {
    var version by remember { mutableIntStateOf(0) }
    var capturedResult by remember { mutableStateOf("Waiting...") }
    var updatedResult by remember { mutableStateOf("Waiting...") }
    LaunchedEffect(Unit) { while (isActive) { delay(500); version++ } }
    val callbackVersion = version // Capture a value, not the mutable State delegate.
    Text("Current callback: v$version")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                Text("Captured callback", style = MaterialTheme.typography.titleSmall)
                CapturedLandingScreen(restartOnCallback) {
                    capturedResult = "v$callbackVersion"
                    labLog("Splash", "captured -> v$callbackVersion")
                }
                Text(capturedResult)
            }
        }
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                Text("Updated callback", style = MaterialTheme.typography.titleSmall)
                LandingScreen {
                    updatedResult = "v$callbackVersion"
                    labLog("Splash", "updatedState -> v$callbackVersion")
                }
                Text(updatedResult)
            }
        }
    }
}

@Composable
fun LandingScreen(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) { delay(2_000); currentOnTimeout() }
}

@Composable
private fun CapturedLandingScreen(restartOnCallback: Boolean, onTimeout: () -> Unit) {
    // A changing lambda key restarts the 2-second delay every 500 ms, so it never finishes.
    LaunchedEffect(if (restartOnCallback) onTimeout else Unit) { delay(2_000); onTimeout() }
}
