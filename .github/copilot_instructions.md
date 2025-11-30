# Test Case Writing Using Appium MCP Server

Use this document as a guide when writing test cases using the Appium MCP server.

## Key Development Patterns
Here are some key development patterns to follow when writing your test cases:

1. **Async/Await Syntax**: Always use async/await for asynchronous operations to ensure proper flow control.
```javascript
async function exampleFunction() {
  const result = await someAsyncOperation();
  console.log(result);
}
``` 

2. **Error Handling**: Use try/catch blocks to handle potential errors during test execution.
```javascript
try {
  await someOperation();
} catch (error) {
  console.error("An error occurred:", error);
}
```   

3. **Page Object Model**: Organize your code by creating separate classes for different pages or components.
```javascript
class FormScreen {
  // Selectors
  get formsTab() {
    return $("~Forms");
  }

  get textInput() {
    return $("~text-input");
  }

  get inputTextResult() {
    return $("~input-text-result");
  }

  get switchElement() {
    return $("~switch");
  }

  get switchText() {
    return $("~switch-text");
  }

  get dropdown() {
    return $("~Dropdown");
  }

  get activeButton() {
    return $("~button-Active");
  }

  // Methods for interactions
  async navigateToFormsTab() {
    await this.formsTab.click();
    await browser.pause(1000);
  }

  async enterText(text: string) {
    await this.textInput.setValue(text);
    await browser.pause(500);
  }

  async getInputResultText() {
    return await this.inputTextResult.getText();
  }

  async toggleSwitch() {
    await this.switchElement.click();
    await browser.pause(500);
  }

  async getSwitchText() {
    return await this.switchText.getText();
  }

  async selectDropdownOption(optionText: string) {
    await this.dropdown.click();
    await browser.pause(500);

    const dropdownOption = await browser.$(
      `//android.widget.CheckedTextView[@text="${optionText}"]`
    );
    await dropdownOption.click();
    await browser.pause(500);
  }

  async clickActiveButton() {
    await this.activeButton.click();
    await browser.pause(1000);
  }

  async clickOkButton() {
    const okButton = await browser.$('//android.widget.Button[@text="OK"]');
    await okButton.click();
    await browser.pause(500);
  }
}

export default new FormScreen();
```   

4. **Mocha Framework**: Structure your tests using Mocha's describe, beforeAll, afterAll, and it blocks.
```javascript
import FormScreen from "../../screen-objects/form.js";

describe("Forms E2E Test", () => {
  it("should complete the forms flow successfully", async () => {
    // Step 1: Click on the "Forms" tab
    await FormScreen.navigateToFormsTab();

    // Step 2: Enter text "ABCDEF" into the input field
    await FormScreen.enterText("ABCDEF");

    // Verify the text was entered
    const resultText = await FormScreen.getInputResultText();
    expect(resultText).toBe("ABCDEF");

    // Step 3: Click to turn the switch ON
    await FormScreen.toggleSwitch();

    // Verify switch text changed
    const switchStatusText = await FormScreen.getSwitchText();
    expect(switchStatusText).toBe("Click to turn the switch OFF");

    // Step 4: Select "Appium is awesome" from the dropdown
    await FormScreen.selectDropdownOption("Appium is awesome");

    // Step 5: Click on the "Active" button
    await FormScreen.clickActiveButton();

    // Step 6: Click on OK in the displayed pop-up
    await FormScreen.clickOkButton();

    console.log("✅ All test steps completed successfully!");
  });
});
```

5. **Assertions**: Use Mocha's expect() function to include necessary assertions in your tests.
```javascript
expect(value).toBe(expectedValue);
expect(array).toContain(item);
expect(object).toHaveProperty("key", "value");
```

6. **Comments**: Include comments in your code to explain the purpose where necessary.

7. **File Naming**: When done, save the test case with a descriptive name, e.g., `e2e.test.js` in the tests directory.

8. **Running Tests**: Execute the test file and iterate until the test passes.

# References:
https://github.com/gavrix/appium-mcp


