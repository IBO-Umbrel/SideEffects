package com.lessons.sideeffects

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SnackbarScreen() {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var taps by remember { mutableIntStateOf(0) }
    Scaffold(snackbarHost = { SnackbarHost(host) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Tap twice to queue snackbars. Go Back while one is visible, then reopen this lesson.")
            Button(onClick = {
                val tap = ++taps
                // LaunchedEffect is composable and cannot be called from onClick.
                // showSnackbar suspends; this scope and queued requests are cancelled on exit.
                scope.launch {
                    try {
                        labLog("Snackbar", "request $tap")
                        host.showSnackbar("Snackbar $tap", actionLabel = "Dismiss", duration = SnackbarDuration.Long)
                    } finally { labLog("Snackbar", "request $tap finished or cancelled") }
                }
            }) { Text("Show snackbar ($taps taps)") }
            Text("A second tap queues another showSnackbar call. It displays after the first finishes.")
        }
    }
}
