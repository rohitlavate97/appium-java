package com.alchemist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

public abstract class AppiumUtils {
	/*
	 * AppiumDriver driver;
	 * 
	 * public AppiumUtils(AppiumDriver driver) { this.driver = driver; }
	 */
	public AppiumDriverLocalService service;
	public AppiumDriverLocalService startAppiumServer(String ipAddress, int port) {
		//service = new AppiumServiceBuilder().withIPAddress("127.0.0.1").usingPort(4723).build();
		service = new AppiumServiceBuilder().withIPAddress(ipAddress).usingPort(port).build();
		service.start();
		return service;
	}

	public Double getFormattedAmount(String amount) {
		Double price = Double.parseDouble(amount.substring(1));
		return price;
	}

	public void waitForElementToAppear(WebElement ele, AppiumDriver driver) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.attributeContains(ele, "text", "Cart"));
	}

	public List<HashMap<String, String>> getJsonData(String jsonFilePath) throws IOException {

	    String jsonContent = FileUtils.readFileToString(
	            new File(jsonFilePath),
	            StandardCharsets.UTF_8
	    );
	    ObjectMapper mapper = new ObjectMapper();
	    List<HashMap<String, String>> data =
	            mapper.readValue(
	                    jsonContent,
	                    new TypeReference<List<HashMap<String, String>>>() {}
	            );
	    return data;
	}
	
	public String getScreenshotPath(String testCaseName, AppiumDriver driver) throws IOException {
	    File src = driver.getScreenshotAs(OutputType.FILE);
	    String dest = System.getProperty("user.dir")
	            + "/reports/"
	            + testCaseName
	            + ".png";
	    FileUtils.copyFile(src, new File(dest));
	    return dest;   //So that, it can be attached in index.html file
	}
}
