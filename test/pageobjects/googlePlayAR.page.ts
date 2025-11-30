import { $ } from '@wdio/globals'
import Page from './page.js'

class GooglePlayARPage extends Page {
  // Attempt to click the 'Continue' button on the AR permission popup
  public async clickContinue(): Promise<boolean> {
    const selectors = [
      '~Continue',
      '//*[@text="Continue"]',
      '//*[contains(@text, "Continue")]',
      'android=new UiSelector().text("Continue")'
    ]
    for (const sel of selectors) {
      try {
        const el = await $(sel)
        if (await el.isExisting()) {
          await el.click()
          return true
        }
      } catch (e) {
        // ignore and continue
      }
    }
    return false
  }

  // Dismiss the Google Play sign-in / consent screen by pressing back button
  public async dismissGooglePlaySign(): Promise<boolean> {
    try {
      // Press the back button (Android keycode 4)
      await driver.back()
      await driver.pause(500) // Brief pause to allow screen transition
      return true
    } catch (e) {
      console.warn('Failed to press back button:', e)
      return false
    }
  }

  // Return the main header element if present
  public async getMainTextElement() {
    const selectors = [
      '//*[contains(@text, "Select something below to speak")]',
      '//*[@text="Select something below to speak"]'
    ]
    for (const sel of selectors) {
      try {
        const el = await $(sel)
        if (await el.isExisting()) return el
      } catch (e) {
        // ignore
      }
    }
    return null
  }

  // Tap the arrow / next button to move to the next category
  public async tapNextArrow(): Promise<boolean> {
    const selectors = [
      '~Next',
      '//*[@content-desc="Next"]',
      '//*[contains(@resource-id, "next")]',
      '//*[contains(@content-desc, "next")]',
      '(//android.widget.TextView[contains(@text, "Category")]/following-sibling::*)[1]',
      '(//android.widget.TextView[contains(@text, "Select")]/following-sibling::*)[1]'
    ]
    for (const sel of selectors) {
      try {
        const el = await $(sel)
        if (await el.isExisting()) {
          await el.click()
          return true
        }
      } catch (e) {
        // ignore
      }
    }

    // coordinate fallback
    try {
      const { width, height } = await driver.getWindowSize()
      await driver.touchPerform([
        { action: 'press', options: { x: Math.floor(width * 0.85), y: Math.floor(height * 0.35) } },
        { action: 'release' }
      ])
      return true
    } catch (e) {
      return false
    }
  }

  // Handle system runtime permission dialogs (audio / camera). Attempts several common
  // selectors and clicks 'Allow while using the app' / 'Allow' where present.
  public async allowSystemPermissions(): Promise<void> {
    const allowSelectors = [
      '//*[@text="Allow while using the app"]',
      '//*[@text="Allow only while using the app"]',
      '//*[@text="Allow"]',
      '~Allow',
      '//*[@resource-id="com.android.packageinstaller:id/permission_allow_button"]',
      '//*[@resource-id="com.android.permissioncontroller:id/permission_allow_foreground_only_button"]'
    ]

    // Some devices show multiple permission prompts (camera then mic etc.). Try up to 3 times.
    for (let attempt = 0; attempt < 3; attempt++) {
      let clicked = false
      for (const sel of allowSelectors) {
        try {
          const el = await $(sel)
          if (await el.isExisting()) {
            await el.click()
            clicked = true
            // small pause to allow next dialog to appear
            await driver.pause(600)
            break
          }
        } catch (e) {
          // ignore selector errors
        }
      }
      if (!clicked) break
    }
  }

  // Check if Basic Needs category is visible
  public async isBasicNeedsPresent(): Promise<boolean> {
    const selectors = ['//*[contains(@text, "Basic Needs")]', '//*[@text="Basic Needs"]']
    for (const sel of selectors) {
      try {
        const el = await $(sel)
        if (await el.isExisting()) return true
      } catch (e) {
        // ignore
      }
    }
    return false
  }
}

export default new GooglePlayARPage()
