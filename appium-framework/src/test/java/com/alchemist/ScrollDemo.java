package com.alchemist;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import io.appium.java_client.AppiumBy;

public class ScrollDemo extends BaseTest {

    @Test
    public void scrollTest() {

        // Step 1: Open Views
        driver.findElement(AppiumBy.accessibilityId("Views")).click();

        // Step 2: Scroll to WebView and click
        driver.findElement(
            AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))" +
                ".scrollIntoView(new UiSelector().text(\"WebView\"))"
            )
        ).click();

        // Step 3: Scroll using gesture (dynamic scrolling)
        boolean canScrollMore = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "mobile: scrollGesture",
            ImmutableMap.of(
                "left", 100,
                "top", 100,
                "width", 800,
                "height", 1200,
                "direction", "down",
                "percent", 0.75
            )
        );

        System.out.println("Can scroll more: " + canScrollMore);
    }
}

