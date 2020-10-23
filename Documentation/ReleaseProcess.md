# Release Process

The release process for this project is a pretty standard one for Android projects.  We just use incremental build numbers,
so you'll need to increment the build number and then generate an .aab file.  The signing keys are available in LastPass - if you need help finding them, ping Cameron Greene on Slack.
The password for the key and keystore are the same, and the alias is vocable-release.

Once the .aab is generated, you can upload it to the Play Console.  We have been uploading to the internal test track first, and making sure a couple of people
are able to update their apps successfully within WillowTree before promoting it to Production.  You can also do one last smoke test
on the release build while it's in internal test, if you'd like.

Once the release is promoted to Production, keep an eye on Crashlytics for a couple of days to make sure there aren't any major issues that come up.
