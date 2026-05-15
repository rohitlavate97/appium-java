package com.automation.testrail.tests;

import com.automation.testrail.annotations.TestCase;
import com.automation.testrail.pages.LoginPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.automation.testrail.listener.TestRailListener;

/**
 * Sample test class demonstrating TestRail integration.
 *
 * <ul>
 *   <li>Each {@code @Test} is mapped to a TestRail case via {@code @TestCase}.</li>
 *   <li>Every {@code @Step}-annotated method in the page object is automatically
 *       intercepted by {@link com.automation.testrail.aspect.StepAspect} and logged
 *       into {@link com.automation.testrail.logger.StepLogger}.</li>
 *   <li>On completion, {@link TestRailListener} pushes status + step log to TestRail.</li>
 * </ul>
 */
@Listeners(TestRailListener.class)
public class LoginTest {

    private LoginPage loginPage;

    @BeforeMethod
    public void setUp() {
        // In production: initialize AppiumDriver and pass it to the page
        loginPage = new LoginPage();
    }

    @Test
    @TestCase(caseId = 101, title = "Verify successful login with valid credentials")
    public void testValidLogin() {
        loginPage.launchApp();
        loginPage.enterUsername("testuser");
        loginPage.enterPassword("P@ssw0rd");
        loginPage.clickLogin();
        loginPage.verifyDashboard();
    }

    @Test
    @TestCase(caseId = 102, title = "Verify login fails with invalid password")
    public void testInvalidPassword() {
        loginPage.launchApp();
        loginPage.enterUsername("testuser");
        loginPage.enterPassword("wrong");
        loginPage.clickLogin();
        // In production this step would throw an assertion → TestRail gets FAILED
        loginPage.verifyDashboard();
    }
}
