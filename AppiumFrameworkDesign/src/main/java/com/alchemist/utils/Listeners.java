package com.alchemist.utils;

import java.io.IOException;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.alchemist.AppiumUtils;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import io.appium.java_client.AppiumDriver;

public class Listeners extends AppiumUtils implements ITestListener {

	ExtentReports extent = ExtentReportNG.getReporterObject();
	ExtentTest test;
	AppiumDriver driver;

	@Override
	public void onTestStart(ITestResult result) {
		test = extent.createTest(result.getMethod().getMethodName());
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		// test.pass("Test Passed");
		test.log(Status.PASS, "Test Passed");
	}

	@Override
	public void onTestFailure(ITestResult result) {
		// result holds the information about the test method
		test.fail(result.getThrowable());
		try {
			driver = (AppiumDriver) result.getTestClass().getRealClass().getField("driver").get(result.getInstance());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
			test.addScreenCaptureFromPath(getScreenshotPath(result.getMethod().getMethodName(), driver),
					result.getMethod().getMethodName());   //first argument is path, second is title
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onFinish(ITestContext context) {
		extent.flush();
	}
}