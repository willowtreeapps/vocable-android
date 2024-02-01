CROWDIN CRASH COURSE
Link to CrowdIn GitHub integration for this
project: https://crowdin.com/project/vocable-android/apps/system/github

CrowdIn is a crowd-sourced translation tool for software projects. Vocable uses a CrowdIn file-based
integration with GitHub to make a PR whenever a new translation is added to CrowdIn. Read the docs:
https://support.crowdin.com/github-integration/

CrowdIn should automatically raise a PR to the `main` branch whenever translations are updated. The
sync schedule is currently set to 1 hour, or you have the option to hit the "Sync Now" button on the
integration page (linked at the top of this file). This PR should involve only string resource
files.

POTENTIAL PITFALLS

* It is strongly recommended that you do NOT alter the folder structure displayed on the "Sources"
  tab of the CrowdIn project. This structure was automatically generated when the integration was
  set up. If the structure of the project no longer matches this structure, or CrowdIn is otherwise
  not working, you may need to spin up
  a new GitHub integration.
* Only the person who created an integration can edit the same integration, and only one integration
  can be active on a single GitHub repo.
* To create a new integration, you must have admin privileges on the GitHub repo - you can check
  this through CrowdIn by navigating to the Integrations tab and clicking "Add Repository". The
  drop-down will populate after a little while - leave the tab open, it DOES take some time to
  retrieve your GitHub profile. If you have the correct privileges, you'll
  see `willowtreeapps/vocable-android` in the repos list, grayed out since you can't create a second
  integration for the same repo.
* BEFORE DELETING AN INTEGRATION, download the latest translations from CrowdIn so that they can be
  uploaded to the new integration:
    * Go to the Translations tab on the CrowdIn
      project (https://crowdin.com/project/vocable-android/translations).
    * Open the `Target File Bundles` accordion. There should be a saved bundle here with the
      following settings:
        * Bundle name: Translations
        * Source files path: **/strings.xml
        * Target file format: Android XML
        * Include project source language box checked
        * Resulting file pattern: values-%android_code%/strings.xml
    * The Preview pane to the right will allow you to make sure the structure matches your intent
    * Click Save to exit the bundles modal
    * Click "Download All"
    * Navigate to the Integrations tab and delete the active integration
    * Click "Add Integration" to set up a new one
        * Choose GitHub
        * Source files path: enter the path to the strings file you wish to translate. Defaults
          to `app/src/main/res/values/strings.xml`
        * Translated files destination path: enter the path where the translated strings should be
          placed. Defaults to `/app/src/main/res/values-%android_code%/%original_file_name%`
            * Begins with forward slash - apparently this is crucial so that CrowdIn doesn't create
              a separate top-level directory
            * `%android_code/$original_file_name%` should be the end of the path: Android needs
              these in order to match up the translated strings to the system locale
            * Choose the branch you wish to sync with - this will be `main` as long as we are
              sticking with trunk-based development on this project
            * OPTIONAL: check "One-time translation import after the branch is connected" - if you
              do this, make sure to copy the downloaded translations into your project _before_
              creating this integration (see below). If you do this step correctly, you can skip the
              step to sync translations to CrowdIn.
    * Copy the translations you downloaded before into the correct folders in your project and make
      a PR to make this change live on the branch that is synced with CrowdIn
    * Once the "good" translations are merged in, return to the CrowdIn integrations page. Click
      the dropdown next to the "Sync Now" button and choose "Sync Translations to CrowdIn"

Once the new integration is set up, it should behave as described above.

CROWDIN CONFIGURATION FILE
A CrowdIn integration should create a config file, `crowdin.yml`, at the root of the project. It
should be set up as follows:
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

