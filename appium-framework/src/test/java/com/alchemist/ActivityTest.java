package com.alchemist;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.Activity;

public class ActivityTest extends BaseTest{
	@Test
	public void activityLaunch() {
		Activity activity = new Activity("io.appium.android.apis",
				"io.appium.android.apis.preference.PreferenceDependencies");
		//driver.startActivity(activity); 
		//n Appium 2, startActivity is executed using mobile commands instead of deprecated
	    ((JavascriptExecutor) driver).executeScript(
	            "mobile: startActivity",
	            ImmutableMap.of(
	                "appPackage", "io.appium.android.apis",
	                "appActivity", "io.appium.android.apis.preference.PreferenceDependencies"
	            )
	        );
	    driver.findElement(By.id("android:id/checkbox")).click();
		driver.findElement(By.xpath("(//android.widget.Relative-Layout)[2]")).click();
		String alertTitle = driver.findElement(By.id("android:id/alertTitile")).getText();
		Assert.assertEquals(alertTitle, "Wifi settings");
		driver.findElement(By.id("android:id/edit")).sendKeys("Rahul Wifi");
		driver.findElements(AppiumBy.className("android.widget.Button")).get(1).click();
	}
}
