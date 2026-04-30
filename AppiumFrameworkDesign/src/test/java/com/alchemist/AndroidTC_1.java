package com.alchemist;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.alchemist.pageObjects.CartPage;
import com.alchemist.pageObjects.FormPage;
import com.alchemist.pageObjects.ProductCatalogue;

import io.appium.java_client.android.AndroidDriver;

public class AndroidTC_1 extends BaseTest{
	AndroidDriver driver;
	@Test
	public void fillForm() throws InterruptedException {
		//FormPage formPage = new FormPage(driver);  //it is already taken care of in BaseTest, configureApp()
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
