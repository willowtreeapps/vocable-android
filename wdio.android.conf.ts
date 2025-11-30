import type { Config } from '@wdio/types';

export const config: Config = {
  ...baseConfig,

  // Override capabilities for Android
  capabilities: [
    {
      // capabilities for local Appium native app tests on Android Emulator
      platformName: "Android",
      "appium:deviceName": "Android GoogleAPI Emulator",
      "appium:platformVersion": "12.0",
      "appium:automationName": "UiAutomator2",
      "appium:app":
        "/Users/albertacosta/Documents/GitHub/vocable-android/app/build/outputs/apk/debug/app-debug.apk",
      "appium:appPackage": "com.willowtree.vocable",
      "appium:appActivity": "com.willowtree.vocable.splash.SplashActivity",
      "appium:noReset": false,
    },
  ],
};
