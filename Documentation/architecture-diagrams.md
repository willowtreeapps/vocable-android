# Vocable Android — Architecture Diagrams

Companion to the written baseline in [`CLAUDE.md`](../CLAUDE.md). GitHub renders the Mermaid
blocks below inline. Kept current as of #678 (PID gaze smoothing + position-based tracking).

## User journey

The core loop: a user selects a phrase (by gaze-dwell or touch) and the app speaks it aloud.

```mermaid
flowchart TD
    Launch["App launch"] --> Splash["SplashActivity<br/>(DB seed / migration check)"]
    Splash --> Perms{"Camera permission +<br/>head tracking enabled?"}
    Perms -- yes --> Tracking["Head-tracking cursor active<br/>(gaze + dwell input)"]
    Perms -- no --> Touch["Touch-only input"]
    Tracking --> Presets["Presets screen<br/>(fixed category/phrase grid)"]
    Touch --> Presets
    Presets --> Phrase["Select a phrase"]
    Phrase --> Speak["VocableTextToSpeech speaks it"]
    Speak --> Presets
    Presets --> Keyboard["Keyboard screen<br/>(type a custom phrase)"]
    Keyboard --> Speak
    Presets --> Settings["Settings"]
    Settings --> Sensitivity["Timing & Sensitivity<br/>(dwell time, cursor sensitivity)"]
    Settings --> Voice["Voice selection"]
    Settings --> EditCats["Edit categories & phrases"]
```

## Tech stack

Single `:app` module; one flat Koin module (`di/AppKoinModule.kt`) wires everything.

```mermaid
flowchart TD
    subgraph UI ["UI — 100% Jetpack Compose"]
        NavHost["VocableNavHost<br/>(string routes)"]
        Screens["ui/&lt;feature&gt;/ screens<br/>+ MviScreen"]
        Gaze["GazePointer / GazeButton<br/>GazeClickable (dwell)"]
    end
    subgraph Presentation ["Presentation — MVI"]
        BVM["BaseViewModel<br/>(StateFlow state + Channel events)"]
        VMs["Feature ViewModels"]
    end
    subgraph Domain ["Domain"]
        UseCases["Use cases<br/>(interface + impl pairs)"]
    end
    subgraph Data ["Data"]
        Repos["Repositories"]
        Room["Room DB v7<br/>(stored + preset entities)"]
        Prefs["VocableSharedPreferences"]
    end
    subgraph Core ["Core services"]
        TTS["VocableTextToSpeech"]
        FaceTrack["Face-tracking pipeline<br/>(see diagram below)"]
        GIM["GazeInteractionManager<br/>(gaze-target registry)"]
    end
    Screens --> BVM
    NavHost --> Screens
    Gaze --> GIM
    VMs -.extend.-> BVM
    VMs --> UseCases
    UseCases --> Repos
    Repos --> Room
    VMs --> Prefs
    VMs --> TTS
    FaceTrack --> Gaze
    Koin["Koin DI<br/>(AppKoinModule)"] -. provides .-> VMs
    Koin -. provides .-> UseCases
    Koin -. provides .-> Repos
```

## Gaze-cursor pipeline (#678)

Everything below runs on the **main thread** — sceneview delivers ARCore session updates from a
main-thread Choreographer callback, and the PID tick is the same Choreographer. That confinement
is load-bearing (documented on `FaceTrackingViewModel`).

```mermaid
flowchart TD
    ARCore["ARCore ARSceneView<br/>AugmentedFace @ 30-60fps"] --> Scene["FaceTrackingViewModel.onSceneUpdate"]
    Scene --> Pose["NOSE_TIP pose, camera-relative:<br/>cameraPose.inverse().compose(regionPose)<br/>read .translation — POSITION, not rotation"]
    Pose --> Tracker["HeadPositionTracker<br/>depth-normalize (distance-invariant),<br/>offset vs ~0.7s averaged neutral"]
    Tracker --> Target["latestRawTarget: GazePoint<br/>(fresh instance per sample)"]
    Clock["FrameClock (Choreographer vsync)<br/>self-stops when idle"] --> Tick["PID tick @ display refresh"]
    Target --> Tick
    Tick --> PID["GazePIDFilter — Kotlin port of iOS Pulse<br/>iOS gains 3.307/0.365/0.690, deadband 0.010<br/>+ wake hysteresis, wake confirmation, leaky freeze"]
    PID --> Scale["phone-only y×2 reachability scaling<br/>(after smoothing, on purpose)"]
    Scale --> Flow["adjustedVector StateFlow<br/>(GazePoint equality skips frozen ticks)"]
    Flow --> Pointer["GazePointer composable"]
    Pointer --> Convert["convertCoordSystems<br/>(× sensitivity amplitude — iOS semantics)"]
    Convert --> Hit["intersect: hit-test vs<br/>GazeInteractionManager targets"]
    Hit --> Dwell["GazeClickable dwell (default 1000ms)<br/>selected until TTS finishes"]
    Dwell --> Action["Action fires (e.g. speak phrase)"]
    Loss["Tracking lost >1s or<br/>head tracking re-enabled"] -. reset filter + neutral + target .-> Tracker
```

Key #678 decisions behind this shape (full history:
[`work-log/678-pid-gaze-smoothing.md`](work-log/678-pid-gaze-smoothing.md)):

- **Position, not rotation**: ARCore's RGB-fit orientation estimate bends under yaw (vertical
  swoop); the observed nose position doesn't. Same ARCore API — we read `Pose.translation`
  instead of `Pose.zAxis`.
- **PID over lerp**: a fixed blend fraction can't be both fast and stable; the PID (iOS's exact
  shipped controller) can. Several filter choices look like bugs but are deliberate iOS parity —
  read the work-log before "fixing" them.
- **Sensitivity setting** scales cursor travel (in `convertCoordSystems`), never the smoothing —
  matching iOS's `CursorSensitivity` semantics.
