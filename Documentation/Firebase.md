# Firebase in Vocable

### Usage
Vocable is connected to Firebase for the purpose of using Crashlytics and, at some point in the future, Firebase Analytics.
It is currently set up to be able to handle both, but is only using Crashlytics at the moment.  The link to the Firebase project is [here](https://console.firebase.google.com/u/0/project/vocable-fcb07/overview).

### Structure
The Firebase project is split into three apps - Vocable iOS, Vocable Android, and Vocable Android Debug.  For Android, we use the debug
app for debug builds, and the regular app for release/production builds, so that we don't get crash data mixed up in Crashlytics.

### Analytics
We are not currently tracking any analytics events, but the project should be set up to handle that, whenever we decide to add analytics events.
At some point, it would be very beneficial to add analytics and/or event tracking, so that we can get a better feel for how users are using certain features,
but that isn't something that we've implemented yet, beyond setting up the framework for it.