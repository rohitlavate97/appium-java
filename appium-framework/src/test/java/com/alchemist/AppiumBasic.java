package com.alchemist;

import java.net.MalformedURLException;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.appium.java_client.AppiumBy;

public class AppiumBasic extends BaseTest{
	@Test
	public void wifiSettingName() throws MalformedURLException {
		driver.findElement(AppiumBy.accessibilityId("Preference")).click();
		//tagName[@attribute='value']
		driver.findElement(By.xpath("//android.widget.TextView[@content-desc='3.Preference dependencies']")).click();
		driver.findElement(By.id("android:id/checkbox")).click();
		driver.findElement(By.xpath("(//android.widget.Relative-Layout)[2]")).click();
		String alertTitle = driver.findElement(By.id("android:id/alertTitile")).getText();
		Assert.assertEquals(alertTitle, "Wifi settings");
		driver.findElement(By.id("android:id/edit")).sendKeys("Rahul Wifi");
		//Click on Ok
		//driver.findElements(By.className("android.widget.Button")).get(1).click();
		driver.findElements(AppiumBy.className("android.widget.Button")).get(1).click();
		
	}
}
