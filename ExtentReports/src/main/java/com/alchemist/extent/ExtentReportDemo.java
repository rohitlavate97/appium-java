package com.alchemist.extent;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import io.github.bonigarcia.wdm.WebDriverManager;

public class ExtentReportDemo {
	public ExtentReports extent;
	@BeforeTest
	public void config() {
		//2 classes used to generate the report
		//ExtentReports
		//ExtentSparkReporter -> expects path where your report should be created, this is helper class, which helps to create
		//some configurations
		String path = System.getProperty("user.dir") + "//reports//index.html";
		ExtentSparkReporter reporter = new ExtentSparkReporter(path);
		reporter.config().setReportName("Web Automation Result");
		reporter.config().setDocumentTitle("Test Results");
		//ExtentReports class responsible for drive all reporting execution -> Main Class
		extent = new ExtentReports();
		//attach complete report to this main class
		extent.attachReporter(reporter);
		extent.setSystemInfo("Tester", "Prajakta");
		extent.setSystemInfo("Framework", "Appium + TestNG");
	}
	
	@Test
	public void initialDemo() {
		//How testcase report will be attached to main class ie ExtentReports class variable(declare at global level)
		ExtentTest test = extent.createTest("Initial Demo");  //an object will be created for complete test method
		//Line above will create new test in reporting file, automatically this variable will now keeps on monitoring the result
		//status of this test
//		System.setProperty("webdriver.chrome.driver", "C://Chromedriver.exe");
//		driver = new ChromeDriver();
		WebDriverManager.chromedriver().setup();
		WebDriver driver = new ChromeDriver();
		driver.get("https://rahulshettyacademy.com");
		System.out.println(driver.getTitle());
		test.fail("Result do not match");
		driver.close();
		extent.flush();
		//Above line notifies that, test is done then no more monitoring it, at end of complete execution
	}
}

