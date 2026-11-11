package com.lessons.sideeffects

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LifecycleScreen() {
    var visible by remember { mutableStateOf(false) }
    var adds by remember { mutableIntStateOf(0) }
    var removes by remember { mutableIntStateOf(0) }
    LessonColumn {
        Text("Enable then disable the logger five times. Adds and removals should both be 5.")
        Row {
            Switch(checked = visible, onCheckedChange = { visible = it })
            Text(if (visible) "Logger attached" else "Logger detached")
        }
        Text("Observers added: $adds / removed: $removes")
        Text("Background and reopen the app while attached to see ON_STOP and ON_START in Logcat.")
        if (visible) LifecycleLogger(
            onAdded = { adds++; labLog("Lifecycle", "totals: added=$adds, removed=$removes") },
            onRemoved = { removes++; labLog("Lifecycle", "totals: added=$adds, removed=$removes") }
        )
        Text("onDispose must be the last expression of DisposableEffect. Removing it produces a type mismatch: DisposableEffectResult is required.")
    }
}

@Composable
fun LifecycleLogger(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onAdded: () -> Unit = {},
    onRemoved: () -> Unit = {}
) {
    val currentOnAdded by rememberUpdatedState(onAdded)
    val currentOnRemoved by rememberUpdatedState(onRemoved)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_STOP) {
                labLog("Lifecycle", event.name)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        labLog("Lifecycle", "addObserver")
        currentOnAdded()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            labLog("Lifecycle", "removeObserver / onDispose")
            currentOnRemoved()
        }
    }
}
