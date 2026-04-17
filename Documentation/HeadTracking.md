# Head/Face tracking

Vocable’s head tracking implementation has been updated from the old View/Fragment-based approach to a Compose-based gaze interaction system.

## Current approach

### High-level flow
- `FaceTrackingManager` checks whether head tracking is available on the device.
- If ARCore or the required OpenGL support is unavailable, head tracking is disabled and the rest of the app remains usable.
- `FaceTrackingScreen` hosts a minimal `ARScene` using the front camera and feeds face updates into `FaceTrackingViewModel`.
- `FaceTrackingViewModel` converts ARCore face data into a smoothed screen position.
- `GazePointer` draws the pointer in Compose and performs hit-testing against registered Compose gaze targets.
- `GazeInteractionManager` stores gaze targets and exposes dwell progress for pointer UI.

## FaceTrackingManager

`FaceTrackingManager` is responsible for availability checks and enabling or disabling the pointer UI.

- Checks ARCore support.
- Checks for OpenGL ES 3.0+ support.
- Observes the head-tracking permission/enabled state.
- Disables head tracking cleanly when unsupported instead of blocking app usage.

This replaces the older behavior where unsupported devices surfaced a poor error path tied to the legacy implementation.

## FaceTrackingScreen

`FaceTrackingScreen` is the Compose entry point for head tracking.

- Renders a lightweight `ARScene` configured for front camera face tracking.
- Pulls `AugmentedFace` trackables from the AR session.
- Passes scene updates to `FaceTrackingViewModel.onSceneUpdate(...)`.
- Shows the `GazePointer` overlay.
- Shows an error banner when a face is not detected for a short period while tracking is enabled.

## FaceTrackingViewModel

`FaceTrackingViewModel` contains the face-to-pointer mapping logic.

### Face data processing
- Uses `AugmentedFace.RegionType.NOSE_TIP`.
- Reads the pose `zAxis` from the nose tip region.
- Flips the z-axis so the vector points toward the screen.
- Expands y-axis movement on phones to give users more vertical range.
- Smooths movement with `Vector3.lerp(...)` using the configured sensitivity.

### Pointer and hover behavior
- Exposes the adjusted face vector as state.
- Converts the smoothed vector into screen coordinates with `convertCoordSystems(...)`.
- Finds the active gaze target by checking which registered target contains the pointer location.
- Handles hover transitions by calling the previous target’s `onExit` and the new target’s `onEnter`.
- Sends accessibility speech events for targets that provide an accessibility label.

## GazePointer

`GazePointer` is the Compose UI overlay for gaze interaction.

- Observes the adjusted vector from `FaceTrackingViewModel`.
- Converts it into pointer coordinates within the current Compose layout.
- Translates those coordinates into window coordinates for hit-testing.
- Draws the pointer indicator.
- Renders dwell progress as an arc around the pointer.

## GazeInteractionManager

`GazeInteractionManager` is the shared registry for Compose gaze targets.

- Registers and unregisters `ComposeGazeTarget` instances.
- Provides the current list of active gaze targets for hit-testing.
- Publishes dwell progress so the pointer can visualize selection timing.

A `ComposeGazeTarget` contains:
- bounds
- `onEnter`
- `onExit`
- optional accessibility label

## Buttons and dwell interaction

Interactive Compose components participate in head tracking by registering gaze targets rather than relying on legacy pointer callbacks in `BaseActivity`.

For example, button components can:
- start dwell behavior on `onEnter`
- cancel dwell behavior on `onExit`
- trigger their action when dwell completes
- provide accessibility text for spoken feedback

`VocableButton` now focuses on visual state in Compose, while gaze/dwell behavior is handled by the gaze interaction system.

## Removed legacy approach

The following older concepts are no longer the active architecture and should not be used as the reference implementation:
- `BaseActivity`-driven pointer intersection logic
- `findIntersectingView` for View-based hit-testing
- `FaceTrackFragment` as the core tracking entry point
- View-based `PointerListener` behavior as the primary interaction model
- legacy Sceneform/fragment-driven documentation

The current implementation is Compose-first, with ARCore face tracking feeding a gaze target registry and pointer overlay.