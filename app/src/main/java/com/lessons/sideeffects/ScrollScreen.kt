package com.lessons.sideeffects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ScrollScreen() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var events by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it > 0 }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                Analytics.sendScrolledPastFirstItemEvent()
                events++
            }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Both variants observe the same list. Slowly scroll across several item boundaries, then return to item 0.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScrollIndicator(listState, true, Modifier.weight(1f)) {
                scope.launch { listState.animateScrollToItem(0) }
            }
            ScrollIndicator(listState, false, Modifier.weight(1f)) {
                scope.launch { listState.animateScrollToItem(0) }
            }
        }
        Text("Threshold analytics events: $events")
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until 100).toList(), key = { it }) { index ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text("Item $index", Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ScrollIndicator(listState: LazyListState, derived: Boolean, modifier: Modifier, onTop: () -> Unit) {
    val commits = remember { CommitCounter() }
    val threshold by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val visible = if (derived) threshold else listState.firstVisibleItemIndex > 0
    SideEffect {
        commits.count++
        labLog("Scroll", "${if (derived) "derived" else "direct"} compositions=${commits.count}, showButton=$visible")
    }
    OutlinedCard(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(if (derived) "derivedStateOf" else "Direct read", style = MaterialTheme.typography.titleSmall)
            CounterText(commits)
            // Reserve space so appearing buttons don't move the list's viewport.
            Box(Modifier.height(56.dp)) {
                if (visible) TextButton(onClick = onTop) { Text("Scroll to top") }
            }
        }
    }
}
