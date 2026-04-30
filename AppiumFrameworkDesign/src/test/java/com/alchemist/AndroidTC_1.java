package com.alchemist;

import com.alchemist.pageObjects.FormPage;
import com.alchemist.pageObjects.ProductCatalogue;

import io.appium.java_client.android.AndroidDriver;

public class AndroidTC_1 extends BaseTest{
	AndroidDriver driver;
	public void fillForm() throws InterruptedException {
		FormPage formPage = new FormPage(driver);
		formPage.setNameField("Prajakta");
		formPage.setGender("female");
		formPage.setCountrySelection("Argentina");
		ProductCatalogue productCatalogue = formPage.submitForm();
		productCatalogue.addItemToCartByIndex(0);
		productCatalogue.addItemToCartByIndex(0);
		productCatalogue.goToCartPage();
	}
}
