# Side Effects Lab

Android / Kotlin / Jetpack Compose implementation of all six hands-on tasks from the seminar. Open the project in Android Studio, let Gradle sync, and run the app on an API 24+ device. The home screen opens each task separately; Back removes the active lesson from the Composition. No network permission or backend is needed.

## Lessons and API choices

| Task | APIs | Reason |
| --- | --- | --- |
| 1. Splash callbacks | `LaunchedEffect`, `rememberUpdatedState` | One two-second wait per entry, with a fresh callback without restarting the timer. A deliberately captured variant demonstrates stale values. |
| 2. Snackbar | `rememberCoroutineScope` | Button events launch suspending snackbar work in a scope cancelled on leaving the screen. |
| 3. Lifecycle logger | `DisposableEffect(lifecycleOwner)` | Attach a listener and remove precisely that listener on exit or owner change. Updated callbacks do not restart registration. |
| 4. Analytics | `SideEffect` | Publish the user type to an ordinary external object only after successful composition. |
| 5. Scroll | `derivedStateOf`, `snapshotFlow` | Recompose the derived indicator only when the threshold changes; turn list state into a distinct stream of threshold entries. |
| 6. Fake image loader | `produceState` | Expose a cancellable suspend result as Loading, Success, or Error state, with URL/reload keys. |

## Experiments

1. Open Splash callbacks. The captured callback reports v0; the updated callback reports the latest committed version at timeout (usually v3 or v4 depending on scheduling). Restart to repeat. Enable the callback-key switch: the captured timer keeps restarting because a new lambda arrives every 500 ms, faster than the two-second timeout.
2. Tap Show snackbar twice: the second request waits for the first. Go Back while visible and reopen: no pending snackbar survives because the scope and host belonged to the old screen.
3. The lifecycle switch starts off. Enable and disable five complete times: adds = removals = 5. Leave it enabled and background/reopen the app to see ON_STOP/ON_START. The final `onDispose` is mandatory; intentionally uncompilable examples are kept out of the runnable app.
4. Change the user type, then press Recompose with same user type. Both actions increase the SideEffect count. The unrelated state forces a successful recomposition without changing the user property. A discarded composition attempt can run direct body calls, while SideEffect runs only after a commit.
5. Slowly scroll across multiple item boundaries. Both indicators watch one shared 100-item list, making the comparison use identical input. The direct scope recomposes on each observed index change; the derived scope changes at the threshold. Each transition from index 0 to a positive index emits one event; returning to 0 re-arms it. Fast scrolling may coalesce intermediate states. Counts include the initial composition and any other actual invalidation; they are observations, not fixed expected totals.
6. Load a `.png` URL for Success and another extension for Error. Change URLs to restart the producer and see Loading. Three delayed assignments of the equal result leave the result composition count unchanged. Hide the loader or leave within two seconds to cancel a running producer; Logcat records cancellation and `finally`. If the producer has already finished, its `finally` has already run. Fake loading does not render a downloaded bitmap.

Counters are incremented in SideEffect and observed only by a dedicated child composable. Reading the counter in the measured scope would create a feedback loop and invalidate the experiment.

## Logcat evidence

Filter Android Studio Logcat with `tag:SideEffectsLab`, or capture actual device output:

```powershell
adb logcat -v time -s SideEffectsLab:D '*:S' > logcat.txt
```

Exercise tasks 1, 3, 4, and 5, stop capture, and include the file in your submission. Expected message shapes below are examples, **not captured runtime evidence**:

```text
[Splash] captured -> v0
[Splash] updatedState -> v3
[Lifecycle] addObserver
[Lifecycle] ON_START
[Lifecycle] removeObserver / onDispose
[Analytics] SideEffect #2: userType=Member, unrelated=0
[Scroll] direct compositions=5, showButton=true
[Scroll] derived compositions=2, showButton=true
[Scroll] scrolled_past_first_item
```

## Validation and submission

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Repository tests cover PNG success, other-extension errors, and cancellation during loading. Interactive experiments require a device or emulator. `THEORY.md` contains companion Part A explanations; the lecture slides themselves were not supplied, so exact lecture-model execution is not claimed.

Verified on November 11, 2026: debug APK assembly, Android lint, and all four unit tests passed (three repository tests plus the existing arithmetic test). The build used Android Studio's bundled JDK 25 via `JAVA_HOME`. No device or emulator was connected, so interactive behavior and actual Logcat captures remain unverified. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Lint reported 0 errors and 12 warnings: the intentionally frequent direct list-state read required by Task 5, existing unused template colors and a redundant manifest label, plus dependency update notices. The direct-read warning is part of the comparison experiment.

The provided folder did not contain Git metadata or a remote. No repository, commits, or GitHub publication was created. The seminar requests a commit per task and a GitHub link; handle version history and publishing separately, and attach genuine Logcat evidence after running the experiments.

Reference: [Android Developers — Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects).
