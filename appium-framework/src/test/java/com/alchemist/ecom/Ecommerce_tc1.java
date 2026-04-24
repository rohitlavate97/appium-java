package com.alchemist.ecom;

import com.alchemist.BaseTest;

import io.appium.java_client.AppiumBy;

import org.testng.annotations.Test;
import org.openqa.selenium.By;
import org.testng.Assert;

public class Ecommerce_tc1 extends BaseTest{
	@Test
	public void fillForm() throws InterruptedException {
		driver.findElement(By.id("com.androidsample.generalStore:id/nameField")).sendKeys("Rahul Shetty");
		driver.hideKeyboard();
		driver.findElement(By.xpath("//android.widget.Radiobutton[@text='Female']")).click();
		driver.findElement(By.id("android:id/text1")).click();
		driver.findElement(
			    AppiumBy.androidUIAutomator(
			        "new UiScrollable(new UiSelector().scrollable(true))" +
			        ".scrollIntoView(new UiSelector().text(\"Argentina\"))"
			    )
			);
		driver.findElement(By.xpath("//android.widget.TextView[@text='Argentina']")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnletsshop")).click();
		//Thread.sleep(2000);
		String toastMessage = driver.findElement(By.xpath("(//android.widget.Toast)[1]")).getAttribute("name");
		Assert.assertEquals(toastMessage, "Please Enter your name");
	}
	
	@Test
	public void scrollToProduct() throws InterruptedException {
		driver.findElement(By.id("com.androidsample.generalStore:id/nameField")).sendKeys("Rahul Shetty");
		driver.hideKeyboard();
		driver.findElement(By.xpath("//android.widget.Radiobutton[@text='Female']")).click();
		driver.findElement(By.id("android:id/text1")).click();
		driver.findElement(
			    AppiumBy.androidUIAutomator(
			        "new UiScrollable(new UiSelector().scrollable(true))" +
			        ".scrollIntoView(new UiSelector().text(\"Argentina\"))"
			    )
			);
		int productCount = driver.findElements(By.id("com.android.sample.generalstore:id/productName")).size();
		for(int i=0;i<=productCount;i++) {
			String productName = driver.findElements(By.id("com.android.sample.generalstore:id/productName")).get(i).getText();
			if(productName.equalsIgnoreCase("Jordan 6 Rings")) {
				driver.findElements(By.id("com.androidsample.generalstore:id/productAddCart")).get(i).click();
			}
		}
		driver.findElement(By.id("com.androidsample.generalstore:id/appbar_bt_cart")).click();
	}
}
