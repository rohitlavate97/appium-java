package com.alchemist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;

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

public class BaseTest extends AppiumUtils {

	public AndroidDriver driver;
	public AppiumDriverLocalService service;
	public FormPage formPage;

	@BeforeClass(alwaysRun = true)
	public void configureApp() throws IOException {
		Properties prop = new Properties();
		FileInputStream fis = new FileInputStream(
				System.getProperty("user.dir") + "//src//main//java//com//alchemist//resources//data.json");
		prop.load(fis);
 		//String ipAddress = prop.getProperty("ipAddress");
		String ipAddress = System.getProperty("ipAddress") != null ? System.getProperty("ipAddress")
				: prop.getProperty("ipAddress");
		//String port = prop.getProperty("port");
		String port = System.getProperty("port") != null ? System.getProperty("port")
				: prop.getProperty("port");
		service = startAppiumServer(ipAddress, Integer.parseInt(port));
		UiAutomator2Options options = new UiAutomator2Options();
		options.setChromedriverExecutable("//users//rahulshetty//documents//chromedriver");
		options.setDeviceName("RohitPhone");
		options.setApp("//users//rohit//Appium//src//test//java//resources//ApiDemos-debug.apk"); // Set app location
																									// here
		// AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),
		// options);
		AndroidDriver driver = new AndroidDriver(service.getUrl(), options);
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		formPage = new FormPage(driver);
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() {
		driver.quit();
		service.stop();
	}
}
