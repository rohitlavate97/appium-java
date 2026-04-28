package com.alchemist.ecom;

import com.alchemist.BaseTest;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;

import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

public class Ecommerce_tc1 extends BaseTest {
	@Test
	public void fillForm() throws InterruptedException {
		driver.findElement(By.id("com.androidsample.generalStore:id/nameField")).sendKeys("Rahul Shetty");
		driver.hideKeyboard();
		driver.findElement(By.xpath("//android.widget.Radiobutton[@text='Female']")).click();
		driver.findElement(By.id("android:id/text1")).click();
		driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
				+ ".scrollIntoView(new UiSelector().text(\"Argentina\"))"));
		driver.findElement(By.xpath("//android.widget.TextView[@text='Argentina']")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnletsshop")).click();
		// Thread.sleep(2000);
		String toastMessage = driver.findElement(By.xpath("(//android.widget.Toast)[1]")).getAttribute("name");
		Assert.assertEquals(toastMessage, "Please Enter your name");
	}

	@Test
	public void scrollToProduct() throws InterruptedException {
		driver.findElement(By.id("com.androidsample.generalStore:id/nameField")).sendKeys("Rahul Shetty");
		driver.hideKeyboard();
		driver.findElement(By.xpath("//android.widget.Radiobutton[@text='Female']")).click();
		driver.findElement(By.id("android:id/text1")).click();
		driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
				+ ".scrollIntoView(new UiSelector().text(\"Argentina\"))"));
		driver.findElement(By.xpath("//android.widget.TextView[@text='Argentina']")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnLetsshop")).click();
		driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
				+ ".scrollIntoView(new UiSelector().text(\"Jordan 6 Rings\"))"));
		int productCount = driver.findElements(By.id("com.android.sample.generalstore:id/productName")).size();
		for (int i = 0; i <= productCount; i++) {
			String productName = driver.findElements(By.id("com.android.sample.generalstore:id/productName")).get(i)
					.getText();
			if (productName.equalsIgnoreCase("Jordan 6 Rings")) {
				driver.findElements(By.id("com.androidsample.generalstore:id/productAddCart")).get(i).click();
			}
		}
		driver.findElement(By.id("com.androidsample.generalstore:id/appbar_bt_cart")).click();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.attributeContains(By.id("com.androidsample.generalstore:id/toolbar_title"),
				"text", "cart"));
		String lastPageProduct = driver.findElement(By.id("com.androidsample.generalstore:id/productName")).getText();
		Assert.assertEquals(lastPageProduct, "Jordan 6 Rings");
	}

	public void addPrice() throws InterruptedException {
		driver.findElement(By.id("com.androidsample.generalStore:id/nameField")).sendKeys("Rahul Shetty");
		driver.hideKeyboard();
		driver.findElement(By.xpath("//android.widget.Radiobutton[@text='Female']")).click();
		driver.findElement(By.id("android:id/text1")).click();
		driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
				+ ".scrollIntoView(new UiSelector().text(\"Argentina\"))"));
		driver.findElement(By.xpath("//android.widget.TextView[@text='Argentina']")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnLetsshop")).click();
		// Once code below execute, it changes to ADDED TO CART --> Now again we give
		// index 1 ie get(1).click()
		// but we have only 1 element now, so script might fail
		driver.findElements(By.xpath("//android.widget.Textview[@text='ADD TO CART']")).get(0).click();
		// or We can apply index in driver.findElement()
		driver.findElements(By.xpath("//android.widget.Textview[@text='ADD TO CART']")).get(0).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/appbar_btn_cart")).click();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.attributeContains(By.id("com.androidsample.generalstore:id/toolbar_title"),
				"text", "cart"));
		//Thread.sleep(2000);
		List<WebElement> productPrices = driver.findElements(By.id("com.androidsample.generalstore:id/productPrice"));
		int count = productPrices.size();
		double sum = 0;
		for(int i =0;i<count;i++) {
		 String amountString = productPrices.get(i).getText();
		 Double price = Double.parseDouble(amountString.substring(1));
		 sum = sum + price;
		}
		String displaySum = driver.findElement(By.id("com.androidsample.generalstore:id/totalAmountLbl")).getText();
		Double displayFormattedSum = getFormattedAmount(displaySum);
		Assert.assertEquals(sum, displayFormattedSum);
		
		WebElement ele = driver.findElement(By.id("com.androidsample.generalstore:id/termsButton"));
		longPressAction(ele);
		driver.findElement(By.id("android:id/button1")).click();
		driver.findElement(AppiumBy.className("android.widget.Checkbox")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnProceed")).click();
		Thread.sleep(2000);
	}
	
	public void switchToWebView() throws InterruptedException {
		driver.findElement(By.id("com.androidsample.generalStore:id/nameField")).sendKeys("Rahul Shetty");
		driver.hideKeyboard();
		driver.findElement(By.xpath("//android.widget.Radiobutton[@text='Female']")).click();
		driver.findElement(By.id("android:id/text1")).click();
		driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
				+ ".scrollIntoView(new UiSelector().text(\"Argentina\"))"));
		driver.findElement(By.xpath("//android.widget.TextView[@text='Argentina']")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnLetsshop")).click();
		driver.findElements(By.xpath("//android.widget.Textview[@text='ADD TO CART']")).get(0).click();
		driver.findElements(By.xpath("//android.widget.Textview[@text='ADD TO CART']")).get(0).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/appbar_btn_cart")).click();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.attributeContains(By.id("com.androidsample.generalstore:id/toolbar_title"),
				"text", "cart"));
		Thread.sleep(2000);
		List<WebElement> productPrices = driver.findElements(By.id("com.androidsample.generalstore:id/productPrice"));
		int count = productPrices.size();
		double sum = 0;
		for(int i =0;i<count;i++) {
		 String amountString = productPrices.get(i).getText();
		 Double price = Double.parseDouble(amountString.substring(1));
		 sum = sum + price;
		}
		String displaySum = driver.findElement(By.id("com.androidsample.generalstore:id/totalAmountLbl")).getText();
		Double displayFormattedSum = getFormattedAmount(displaySum);
		Assert.assertEquals(sum, displayFormattedSum);
		
		WebElement ele = driver.findElement(By.id("com.androidsample.generalstore:id/termsButton"));
		longPressAction(ele);
		driver.findElement(By.id("android:id/button1")).click();
		driver.findElement(AppiumBy.className("android.widget.Checkbox")).click();
		driver.findElement(By.id("com.androidsample.generalstore:id/btnProceed")).click();
		Thread.sleep(6000);  //As it takes time to load browser
		//Get me all context handles currently active in app, context name eg: 'webview'(can be different
		//depends on developer who writes the code
		Set<String> contexts = driver.getContextHandles();
		for(String contextName:contexts) {
			System.out.println(contextName);
		}
		driver.context("WEBVIEW_com.androidsample.generalstore");
		//Now from here we can write selenium code, as we are at browser level now
		driver.findElement(By.name("q")).sendKeys("Rahul Shetty");
		driver.findElement(By.name("q")).sendKeys(Keys.ENTER);
		driver.pressKey(new KeyEvent(AndroidKey.BACK));
		//Now switch back to context
		driver.context("NATIVE_APP");
	}
}
