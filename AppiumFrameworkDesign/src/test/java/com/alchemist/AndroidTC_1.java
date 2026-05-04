package com.alchemist;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.alchemist.pageObjects.CartPage;
import com.alchemist.pageObjects.FormPage;
import com.alchemist.pageObjects.ProductCatalogue;
import com.google.common.collect.ImmutableMap;

import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;

public class AndroidTC_1 extends BaseTest {
	AndroidDriver driver;

	@BeforeMethod(alwaysRun = true)
	public void preSetup() {
		// Set screen to home page
		// As we know how to open any page with help of Activity class
		formPage.setActivity();
	}

	@DataProvider
	public Object[][] getData() {
		return new Object[][] { { "rahul shetty", "female", "Argentina" } }; // we can add multiple set of data
	}

	@DataProvider
	public Object[][] getJSONData() throws IOException {
		List<HashMap<String, String>> data = getJsonData(
				System.getProperty("user.dir") + "//src//main//java//com//alchemist//testData//ecommerce.json");
		return new Object[][] {{data.get(0)},{data.get(1)}};
	}
	
//	@DataProvider
//	public Object[][] getJSONData() throws IOException {
//	    List<HashMap<String, String>> data = getJsonData(
//	            System.getProperty("user.dir")
//	                    + "/src/main/java/com/alchemist/testData/ecommerce.json"
//	    );
//	    Object[][] obj = new Object[data.size()][1];
//	    for (int i = 0; i < data.size(); i++) {
//	        obj[i][0] = data.get(i);
//	    }
//	    return obj;
//	}

	@Test(dataProvider = "getJSONData",groups = {"smoke"})
	public void fillForm(HashMap<String,String> input) throws InterruptedException {
		// FormPage formPage = new FormPage(driver); //it is already taken care of in
		// BaseTest, configureApp()
		formPage.setNameField(input.get("name"));
		formPage.setGender(input.get("gender"));
		formPage.setCountrySelection(input.get("country"));
		ProductCatalogue productCatalogue = formPage.submitForm();
		productCatalogue.addItemToCartByIndex(0);
		productCatalogue.addItemToCartByIndex(0);
		CartPage cartPage = productCatalogue.goToCartPage();
		double totalSum = cartPage.getProductsSum();
		double displayFormattedSum = cartPage.getTotalAmountDisplayed();
		Assert.assertEquals(totalSum, displayFormattedSum);
		cartPage.acceptTermsConditions();
		cartPage.submitOrder();
	}
	
	@Test(dataProvider = "getData")
	public void fillFormWithDataProvider(String name, String gender, String country) throws InterruptedException {
		// FormPage formPage = new FormPage(driver); //it is already taken care of in
		// BaseTest, configureApp()
		formPage.setNameField(name);
		formPage.setGender(gender);
		formPage.setCountrySelection(country);
		ProductCatalogue productCatalogue = formPage.submitForm();
		productCatalogue.addItemToCartByIndex(0);
		productCatalogue.addItemToCartByIndex(0);
		CartPage cartPage = productCatalogue.goToCartPage();
		double totalSum = cartPage.getProductsSum();
		double displayFormattedSum = cartPage.getTotalAmountDisplayed();
		Assert.assertEquals(totalSum, displayFormattedSum);
		cartPage.acceptTermsConditions();
		cartPage.submitOrder();
	}

	@Test
	public void fillForm() throws InterruptedException {
		// FormPage formPage = new FormPage(driver); //it is already taken care of in
		// BaseTest, configureApp()
		formPage.setNameField("Prajakta");
		formPage.setGender("female");
		formPage.setCountrySelection("Argentina");
		ProductCatalogue productCatalogue = formPage.submitForm();
		productCatalogue.addItemToCartByIndex(0);
		productCatalogue.addItemToCartByIndex(0);
		CartPage cartPage = productCatalogue.goToCartPage();
		double totalSum = cartPage.getProductsSum();
		double displayFormattedSum = cartPage.getTotalAmountDisplayed();
		Assert.assertEquals(totalSum, displayFormattedSum);
		cartPage.acceptTermsConditions();
		cartPage.submitOrder();
	}
}
