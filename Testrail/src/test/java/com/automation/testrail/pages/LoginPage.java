package com.automation.testrail.pages;

import com.automation.testrail.annotations.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample page object for the Login screen.
 *
 * <p>In a real project each method would interact with Appium elements.
 * Here the Appium calls are stubbed out so the integration compiles
 * and runs without a live device.</p>
 */
public class LoginPage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    // In production: private final AppiumDriver driver;
    // public LoginPage(AppiumDriver driver) { this.driver = driver; }

    @Step("Launch App")
    public void launchApp() {
        log.info("Launching application...");
        // driver.launchApp();   ← real Appium call
    }

    @Step("Enter Username")
    public void enterUsername(String username) {
        log.info("Entering username: {}", username);
        // driver.findElement(By.id("username")).sendKeys(username);
    }

    @Step("Enter Password")
    public void enterPassword(String password) {
        log.info("Entering password: ****");
        // driver.findElement(By.id("password")).sendKeys(password);
    }

    @Step("Click Login Button")
    public void clickLogin() {
        log.info("Clicking login button");
        // driver.findElement(By.id("loginBtn")).click();
    }

    @Step("Verify Dashboard Loaded")
    public void verifyDashboard() {
        log.info("Verifying dashboard is displayed");
        // boolean displayed = driver.findElement(By.id("dashboard")).isDisplayed();
        // Assert.assertTrue(displayed, "Dashboard not visible after login");

        // Simulated — uncomment to see FAIL flow:
        // throw new AssertionError("Dashboard not visible after login");
    }
}
