package com.alchemist;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.testng.annotations.Test;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

public class AppiumBasics {
	
	@Test
	public void appiumTest() throws MalformedURLException {
		/*
		 * Start and stop appium server programmatically using AppiumServiceBuilder
		 * Create object of AppiumServiceBuilder
		 * And tell where your main.js file(responsible for invoking appium server)
		 * Give path below with use of File Class object
		 * For windows --> C:/Users/rahul/AppData/Roaming/npm/node-modules/appium/build/lib/main.js
		 * For mac --> /user/local/lib/node-modules/appium/build/lib/main.js
		 * Also you have to tell which IP Address and Port you have to start 
		 */
		AppiumDriverLocalService service = new AppiumServiceBuilder().
				withAppiumJS(new File("location where main.js file present"))
				.withIPAddress("127.0.0.1").usingPort(4723)
				.build();
		service.start();
		//What kind of driver you are using -> create object of the driver
		//AndroidDriver driver = new AndroidDriver(); -> Expects 2 arguments
		// 1. AppiumDriverLocalService, 2. Capabilities
		//AppiumDriverLocalService -> ie Url of appium server after starting it -> wrapped in URL class
		//Capabilities -> In what OS you want to execute-> android,ios etc, Version number, device name, info about your app
		//capabilities, to make environment ready
		//UiAutomatorOptions -> This class will be used for capabilities
		UiAutomator2Options options = new UiAutomator2Options();
		options.setDeviceName("RohitPhone");
		options.setApp("//users//rohit//Appium//src//test//java//resources//ApiDemos-debug.apk");   //Set app location here
		AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),options);
		driver.quit();
		//Actual automation code from here
	}

}
