package com.lessons.sideeffects

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

sealed interface ImageResult {
    data object Loading : ImageResult
    data class Success(val url: String) : ImageResult
    data class Error(val message: String) : ImageResult
}

class FakeImageRepository {
    suspend fun load(url: String): ImageResult {
        delay(500)
        return if (url.endsWith(".png")) ImageResult.Success(url)
        else ImageResult.Error("URL must end with .png")
    }
}

@Composable
fun loadNetworkImage(url: String, repository: FakeImageRepository, reloadKey: Int = 0): State<ImageResult> =
    produceState<ImageResult>(initialValue = ImageResult.Loading, url, repository, reloadKey) {
        try {
            value = ImageResult.Loading
            labLog("Image", "producer started: $url")
            val result = repository.load(url)
            value = result
            // Delays make these separate writes observable; equal results still conflate.
            repeat(3) {
                delay(500)
                value = result
                labLog("Image", "equal assignment ${it + 1}: $result")
            }
        } catch (cancelled: CancellationException) {
            labLog("Image", "producer cancelled: $url")
            throw cancelled
        } finally {
            labLog("Image", "producer finally: $url")
        }
    }

@Composable
fun ImageLoadingScreen() {
    val repository = remember { FakeImageRepository() }
    var input by remember { mutableStateOf("avatar.png") }
    var url by remember { mutableStateOf("avatar.png") }
    var request by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    LessonColumn {
        Text("Fake source: 500 ms delay. No internet or image download is used.")
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Image URL") }, singleLine = true)
        Button(onClick = { url = input; request++ }) { Text("Load URL") }
        Button(onClick = { input = "invalid.jpg"; url = input; request++ }) { Text("Try an error") }
        Button(onClick = { visible = !visible }) { Text(if (visible) "Hide loader / cancel" else "Show loader") }
        if (visible) {
            ImageResultPanel(url, repository, request)
        }
        Text("After success or error, the producer assigns the same result three times at 500 ms intervals. The result panel's count stays unchanged. Hide it during the first 2 seconds to observe cancellation and finally in Logcat.")
    }
}

@Composable
private fun ImageResultPanel(url: String, repository: FakeImageRepository, request: Int) {
    val result by loadNetworkImage(url, repository, request)
    val commits = remember { CommitCounter() }
    SideEffect {
        commits.count++
        labLog("Image", "result compositions=${commits.count}: $result")
    }
    when (val current = result) {
        ImageResult.Loading -> { CircularProgressIndicator(); Text("Loading $url") }
        is ImageResult.Success -> Text("Success: ${current.url}")
        is ImageResult.Error -> Text("Error: ${current.message}", color = MaterialTheme.colorScheme.error)
    }
    CounterText(commits, "Result compositions")
}
