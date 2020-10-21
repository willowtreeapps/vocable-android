# Head/Face tracking

We use ARCore to do face tracking, and we check if the device supports it or not when the app opens (you can see this in BaseActivity). If the device does not support the app… we show a toast that says, “Sceneform requires OpenGL ES 3.0 later”, which is not great. We should allow users to use the app, but not have head tracking.

### FaceTrackFragment & FaceTrackingViewModel
The two key things that I think defines face tracking for our application are:
We flip the z axis in the augmentedFace’s RegionPose (we use the tip of your nose), that way it is pointing at the screen, rather than away from it. The augmentedFace is the arSceneView’s session’s trackables. Basically, it’s the user’s face that the device can see.
We use Vector3.lerp and give it the parameters of the old vector, the new vector, and a sensitivity. We learned that the sensitivity needed to be very small (0.05F - 0.15F) in order for the interpolation to work properly. (the pointer would not go across the screen smoothly when the sensitivity was large)

We also check if the user is using a phone (or not a tablet), and we allow the user more space to move around in the y axis if they are.

### BaseActivity
We need to define when the PointerView enters and exits a view. That’s where updatePointer and findIntersectingView come in. If the PointerView’s x and y are within a view, it will trigger onPointerEnter, else if it’s not intersecting a view it will trigger onPointerExit.

### findIntersectingView
This function will check if the currentView and the PointerView are in the same location.

Since these are fundamental pieces of this application (using head tracking) it lives in the BaseActivity.

### VocableButton
This is a PointerListener, so it needs an onPointerEnter and onPointerExit. We make it so that every button will say the text on the button, and then it will perform the action that you give it (within the buttonJob) in onPointerEnter. In onPointerExit it will cancel the buttonJob, and the action that was within it.