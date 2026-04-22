package com.alchemist;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import io.appium.java_client.AppiumBy;

public class SwipeDemo extends BaseTest{
	@Test
	public void swipeTest() {
		driver.findElement(AppiumBy.accessibilityId("Views")).click();
		driver.findElement(AppiumBy.accessibilityId("Gallary")).click();
		WebElement firstImage = driver.findElement(By.xpath("//android.widget.TextView[@text='1.Photos']"));
		firstImage.click();
		Assert.assertTrue(
			    Boolean.parseBoolean(
			        driver.findElement(By.xpath("(//android.widget.ImageView)[1]"))
			              .getAttribute("focusable")
			    )
			);
		//Swipe
		((JavascriptExecutor) driver).executeScript(
			    "mobile: swipeGesture",
			    ImmutableMap.of(
			        "elementId", ((RemoteWebElement) firstImage).getId(),
			        "direction", "left",
			        "percent", 0.75
			    )
			);
		//As focus is changed -> focusable attribute should be false
		Assert.assertFalse(
			    Boolean.parseBoolean(
			        driver.findElement(By.xpath("(//android.widget.ImageView)[1]"))
			              .getAttribute("focusable")
			    )
			);
	}
}
