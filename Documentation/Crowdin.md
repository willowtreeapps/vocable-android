CROWDIN CRASH COURSE
Dashboard: https://crowdin.com/project/vocable-android

CrowdIn is a crowd-sourced translation tool for software projects. Vocable uses a CrowdIn file-based
integration with GitHub to make a PR whenever a new translation is added to CrowdIn. Read the docs:
https://support.crowdin.com/github-integration/

In order to make changes to the CrowdIn project, you'll need to be added as a manager on CrowdIn.
The list of current managers is shown to the right of the dashboard - there's a "Contact" link, but
it's probably best to just find the manager(s) by name in Slack. Or, reach out on the #vocable
Slack channel, and someone should be able to help you!

CrowdIn will automatically raise a PR to the `main` branch whenever translations are updated. The
sync schedule is currently set to 1 hour, or you have the option to hit the "Sync Now" button on the
integration page (linked at the top of this file). This PR should involve only string resource
files.

POTENTIAL PITFALLS

* At one point, we saw multiple trees appear under the "Sources" tab. It's unclear as to exactly
  what the problem was, but while this was the case, CrowdIn was also pushing non-translated
  "translations" to the repo. We fixed this by doing the following:
    * Downloading and manually copying the latest translations into the repo
    * Deleting all the sources and integrations on CrowdIn
    * Re-adding the GitHub integration on CrowdIn and allowing it to pull the latest translations
      from the repo
* Only the person who created an integration can edit the same integration, and only one integration
  can be active on a single GitHub repo. I.e. if something isn't working with the current
  integration, you will either need to get its creator to fix it, or delete it and make a new one of
  your own.

CROWDIN CONFIGURATION FILE
A CrowdIn integration should automatically create a config file, `crowdin.yml`, at the root of the
project. It should be set up as follows:

```
files:
#    path to strings that should be translated
- source: app/src/main/res/values/strings.xml
#    path to translated files
    translation: /app/src/main/res/values-%android_code%/%original_file_name%
#    whether attributes (xml tags) should be translated. 0 - no, 1 - yes
    translate_attributes: 0
#    whether long translations should be split into multiple string resources. 0 - no, 1 - yes
    content_segmentation: 0
```

