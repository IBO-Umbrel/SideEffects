# Part A companion notes

These explanations use the supplied worksheet and the official Compose documentation. The lecture slides and runnable lecture models were not included; model outputs below are predictions, not claims that those models were run.

1. **Side-effects and data flow.** A side-effect changes something outside a composable's UI calculation: loading data, sending analytics, or registering a listener. Composition can repeat frequently, run in a different order, or be abandoned. An effect is a composable API that schedules work in relation to successful composition. Keep screen data and loading policy in the ViewModel; the UI observes state and sends events, so recomposition does not duplicate requests.

2. **Choose an API.** Suspend work while present: `LaunchedEffect`. Coroutines from callbacks: `rememberCoroutineScope`. Latest value without restarting: `rememberUpdatedState`. Listener registration/cleanup: `DisposableEffect`. Publish to external code: `SideEffect`. External asynchronous source to State: `produceState` (with appropriate collection/subscription and cleanup). Less frequent derived UI updates: `derivedStateOf`. Compose State to Flow: `snapshotFlow`.

3. **Pulse animation.** It starts on entering Composition. Changing 3000 to 1000 cancels the existing coroutine and starts a new one with the new delay. Leaving cancels the loop; `isActive` checks the coroutine's cancellation status. Suspending operations such as delay also cooperate with cancellation. A model message such as `effect(100) cancelled` illustrates cancellation of the old keyed work.

4. **Constant keys.** A constant key means work lives for that call site's composition lifetime. It is appropriate for a one-off splash wait; it is wrong when a changing user identifies the work. Use `LaunchedEffect(userId) { api.sendHello(userId) }` so a user change restarts it.

5. **Callbacks and scopes.** `LaunchedEffect` must be called during composition, so it cannot be called in onClick. `rememberCoroutineScope` returns a CoroutineScope cancelled when its call site leaves. Use `val scope = rememberCoroutineScope()` during composition, then `onClick = { scope.launch { host.showSnackbar("Hi") } }`. Showing a snackbar suspends and therefore needs a coroutine. Appearance-driven work uses LaunchedEffect; event-driven work uses the remembered scope.

6. **Fresh timeout callback.** The launched coroutine otherwise keeps the lambda captured when it began. `rememberUpdatedState` gives it a stable state reference whose value is refreshed with the latest lambda. Keying on that lambda cancels and restarts the wait. A result of captured v1 versus updated v3 demonstrates the distinction. See `LandingScreen` in the implementation.

7. **Listener cleanup.** An owner retaining an observer can retain its captured screen references and keep issuing unwanted callbacks. End DisposableEffect with `onDispose { owner.lifecycle.removeObserver(observer) }`; register the observer before it. Omitting onDispose fails compilation because the block must return DisposableEffectResult. An empty cleanup suggests using LaunchedEffect for suspend work or SideEffect for committed publication instead.

8. **Disposable model prediction.** The four lines are:

   ```text
   A.addObserver(observer)
   A.removeObserver(observer)
   B.addObserver(observer)
   B.removeObserver(observer)
   ```

   Same-key A keeps its registration. Old cleanup precedes new registration; leaving cleans up B. The app's lifecycle task demonstrates this mechanism with a real observer.

9. **Publishing analytics.** SideEffect runs after the initial successful composition and successful recompositions of its scope. A direct body call can execute even if that composition attempt is discarded. In the three attempts: direct/discarded calls analytics; SideEffect/discarded does not; SideEffect/committed does. SideEffect takes no restart keys.

10. **Producing State.** produceState starts a composition-scoped producer coroutine and returns its State. It cancels on exit and restarts on changed keys. Equal assignments are suppressed by the state's equality policy. A function returning a value uses a lowercase initial letter; UI-emitting Unit composables conventionally start uppercase. The image task logs three equal assignments without additional result compositions.

11. **Simplified implementation.** This reproduces the requested suspend-producer behavior, not the complete awaitDispose API:

   ```kotlin
   @Composable
   fun <T> myProduceState(
       initialValue: T,
       key: Any?,
       producer: suspend MutableState<T>.() -> Unit
   ): State<T> {
       val state = remember { mutableStateOf(initialValue) }
       LaunchedEffect(key) { state.producer() }
       return state
   }
   ```

   The remembered state survives key changes; the old producer is cancelled and the replacement starts. Reset to Loading explicitly when appropriate. This simplified version, like the requested example, uses LaunchedEffect to control the producer lifetime.

12. **Scroll threshold.** A direct index read invalidates its scope when the observed index changes. The derived Boolean invalidates consumers only when its result changes. Remember retains that derived-state object. In `[0,0,1,2,3,4,5,3,1,0]`, direct values have nine distinct consecutive runs, including the initial render; threshold values have three runs: false, true, false. Thus the worksheet's 9 and 3 include initial composition; subsequent recompositions alone are 8 and 2.

13. **Full name.** Write `val fullName = "$firstName $lastName"`. The result generally changes whenever either input changes, so a derived-state tracker saves no UI updates. Use derivedStateOf when observed inputs change substantially more often than the result needed by consumers; deriving a value alone is not sufficient justification for its overhead.

14. **Waveforms.** Use derivedStateOf for rapidly changing observable input where the displayed result changes less often, such as a quantized progress bucket. Precomputed amplitudes need no state observer for deriving those amplitudes; a static waveform has no changing input to suppress; manually driven LaunchedEffect/animate updates already provide their chosen update mechanism. A separate threshold derived from animated state can still justify derivedStateOf if its output changes less often.

15. **State to Flow.** snapshotFlow is cold: collection starts observation of snapshot reads, with emissions when the observed result changes. Map turns indexes into `index > 0`; distinctUntilChanged suppresses repeated Booleans; filter keeps true. LaunchedEffect owns and cancels collection. For `0,0,1,2,5,3,0,0,1,4`, consecutive threshold groups are false, true, false, true; two true entries produce two events. Snapshot observation may coalesce rapid changes, so it is not a lossless queue of every state write.

16. **Keys.** Assuming the worksheet's empty-key model means a single lifetime effect: `[]` starts once (zero restarts), captures A, and is stale after tick 3. `[owner]` starts with A then restarts once for B: two starts. `[owner,tick]` starts on all four ticks: three restarts, four starts. Owner-only keys suit owner-dependent work; tick keys are unnecessary if tick is not an identity of that work. Actual Compose LaunchedEffect requires at least one key; Unit expresses a constant lifetime key.

17. **Owner versus callbacks.** Key the lifecycle observer effect on lifecycleOwner. Wrap callbacks in rememberUpdatedState and invoke their latest values inside the observer; they are not registration identities. Missing the owner key leaves the observer on the previous owner. Values that change the identity/lifetime of work are keys; values that should stay fresh without restarting are updated state.

18. **Chat corrections.** Move markOpened out of the body into a user-keyed effect or ViewModel event policy. Keep onClose fresh without resetting the five-second timer. Remove the lifecycle observer in onDispose. Launch send from the remembered scope. A corrected sketch, with application-provided dependencies, is:

   ```kotlin
   @Composable
   fun ChatScreen(userId: String, onClose: () -> Unit) {
       val scope = rememberCoroutineScope()
       val owner = LocalLifecycleOwner.current
       val currentOnClose by rememberUpdatedState(onClose)
       LaunchedEffect(userId) { viewModel.markOpened(userId) }
       LaunchedEffect(userId) { delay(5_000); currentOnClose() }
       DisposableEffect(owner) {
           val observer = LifecycleEventObserver { _, _ -> /* observe */ }
           owner.lifecycle.addObserver(observer)
           onDispose { owner.lifecycle.removeObserver(observer) }
       }
       Button(onClick = { scope.launch { send() } }) { Text("Send") }
   }
   ```

   Here each user starts a new chat timeout. Use Unit for the timeout key if product semantics instead require a single timeout for the entire screen visit. The ViewModel must define whether markOpened is idempotent across later screen visits.

19. **Summary and example uses.**

| API | When it runs | Cleanup | What restarts or updates it | Example in this lab |
| --- | --- | --- | --- | --- |
| LaunchedEffect | On entry | Coroutine cancellation | Changed keys | Splash wait |
| rememberCoroutineScope | Scope remembered during composition; work launched by events | Scope cancellation on exit | A new composition lifetime supplies a new scope | Snackbar tap |
| rememberUpdatedState | Value refreshed during composition | No resource cleanup | New value updates existing State | Timeout callback |
| DisposableEffect | Entry and key changes | onDispose on replacement/exit | Changed keys | Lifecycle observer |
| SideEffect | After successful composition of its scope | No automatic resource cleanup | Each successful execution of that scope | User-type publication |
| produceState | Producer starts on entry | Producer cancellation; awaitDispose for subscriptions | Changed keys | Fake image result |
| derivedStateOf | Calculates when needed, tracks snapshot reads | No external resource cleanup | Observed inputs invalidate calculation; unequal result invalidates consumers | Scroll button threshold |
| snapshotFlow | On collection | Collection cancellation stops observation | Observed changes produce unequal results | Scroll analytics |

Reference: [Android Developers — Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects).
