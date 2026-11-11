package com.lessons.sideeffects

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lessons.sideeffects.ui.theme.SideEffectsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SideEffectsTheme { SideEffectsLab() } }
    }
}

private val lessons = listOf(
    "Splash callbacks" to "LaunchedEffect / rememberUpdatedState",
    "Snackbar button" to "rememberCoroutineScope",
    "Lifecycle logger" to "DisposableEffect",
    "Analytics properties" to "SideEffect",
    "Scroll laboratory" to "derivedStateOf / snapshotFlow",
    "Image loading" to "produceState"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideEffectsLab() {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(selected != null) { selected = null }
    Scaffold(topBar = {
        TopAppBar(title = { Text(selected?.let { lessons[it].first } ?: "Side Effects Lab") },
            navigationIcon = {
                if (selected != null) TextButton(onClick = { selected = null }) { Text("Back") }
            })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> SplashScreen()
                1 -> SnackbarScreen()
                2 -> LifecycleScreen()
                3 -> AnalyticsScreen()
                4 -> ScrollScreen()
                5 -> ImageLoadingScreen()
                else -> LessonColumn {
                    Text("Explore all eight Compose effect APIs", style = MaterialTheme.typography.headlineSmall)
                    Text("Open a lesson, try its controls, and filter Logcat by SideEffectsLab.")
                    lessons.forEachIndexed { index, lesson ->
                        Card(onClick = { selected = index }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text("${index + 1}. ${lesson.first}", style = MaterialTheme.typography.titleMedium)
                                Text(lesson.second)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LessonColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

// Only CounterText observes this state; updates do not invalidate the measured parent.
internal class CommitCounter { var count by mutableIntStateOf(0) }

@Composable
internal fun CounterText(counter: CommitCounter, label: String = "Committed compositions") {
    Text("$label: ${counter.count}", style = MaterialTheme.typography.bodySmall)
}
