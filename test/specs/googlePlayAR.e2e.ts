import googlePlayAR from '../pageobjects/googlePlayAR.page.js'

describe('Google Play AR flow (pageobject)', () => {
  it('starts app, handles AR prompts, navigates to Basic Needs', async () => {
    // App is launched by the WDIO runner using capabilities in `capabilities.json`.

    // 0) Handle system permission dialogs (audio / camera)
    await googlePlayAR.allowSystemPermissions()

    // 1) Click 'Continue' on Google Play Services for AR popup
    await googlePlayAR.clickContinue()

    // 2) Dismiss/close Google Play sign-in screen
    await googlePlayAR.dismissGooglePlaySign()

    // 3) Verify the main app text is displayed
    let main = await googlePlayAR.getMainTextElement()
    if (!main) {
      await driver.pause(1000)
      main = await googlePlayAR.getMainTextElement()
    }
    if (!main) throw new Error('Main text "Select something below to speak" not found')

    // 4) Tap the arrow to the right of the category label to navigate to "Basic Needs"
    await googlePlayAR.tapNextArrow()

    // Optionally assert we've navigated to "Basic Needs" category
    const isBasic = await googlePlayAR.isBasicNeedsPresent()
    if (!isBasic) console.warn('Could not verify "Basic Needs" text; confirm selector and UI state')

    // End of test — WDIO will terminate the session. Optionally pause for observation.
    await driver.pause(500)
  })
})
