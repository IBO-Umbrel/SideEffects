package com.lessons.sideeffects

import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.*

internal fun labLog(task: String, message: String) { Log.d("SideEffectsLab", "[$task] $message") }

// Ordinary properties represent a library outside Compose.
object Analytics {
    var userType: String = "Guest"
    fun sendScrolledPastFirstItemEvent() { labLog("Scroll", "scrolled_past_first_item") }
}

@Composable
fun AnalyticsScreen() {
    var userType by remember { mutableStateOf("Guest") }
    var unrelated by remember { mutableIntStateOf(0) }
    val commits = remember { CommitCounter() }
    val publication = userType to unrelated
    SideEffect {
        Analytics.userType = publication.first
        commits.count++
        labLog("Analytics", "SideEffect #${commits.count}: userType=${Analytics.userType}, unrelated=${publication.second}")
    }
    LessonColumn {
        Text("Compose user type: $userType")
        Button(onClick = { userType = if (userType == "Guest") "Member" else "Guest" }) { Text("Change user type") }
        Button(onClick = { unrelated++ }) { Text("Recompose with same user type ($unrelated)") }
        CounterText(commits, "SideEffect executions")
        Text("SideEffect runs after each successful composition of this scope, even when only the unrelated counter changed.")
        Text("In the attempt model, a direct library call can run during a discarded attempt; SideEffect publishes only after a successful commit.")
    }
}
