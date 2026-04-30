package com.alchemist;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.alchemist.pageObjects.FormPage;
import com.google.common.collect.ImmutableMap;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

public class BaseTest {

	public AndroidDriver driver;
	public AppiumDriverLocalService service;
	public FormPage formPage;

	@BeforeClass
	public void configureApp() throws MalformedURLException {

		service = new AppiumServiceBuilder().withIPAddress("127.0.0.1").usingPort(4723).build();

		service.start();

		UiAutomator2Options options = new UiAutomator2Options();
		options.setChromedriverExecutable("//users//rahulshetty//documents//chromedriver");
		options.setDeviceName("RohitPhone");
		options.setApp("//users//rohit//Appium//src//test//java//resources//ApiDemos-debug.apk"); // Set app location
																									// here
		AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		formPage = new FormPage(driver);
	}

	@AfterClass
	public void tearDown() {
		driver.quit();
		service.stop();
	}
}
