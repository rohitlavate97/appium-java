package com.alchemist.pageObjects;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import com.alchemist.utils.AndroidActions;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class CartPage extends AndroidActions{
	AndroidDriver driver;
	
	public CartPage(AndroidDriver driver) {
		super(driver);
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/productprice")
    public List<WebElement> productList;
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/totalAmountLbl")
    public WebElement totalAmount;
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/termsButton")
	public WebElement terms;
	
	@AndroidFindBy(id="android:id/button1")
	public WebElement acceptButton;
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/btnProceed")
	public WebElement proceed;
	
	@AndroidFindBy(className="android.widget.checkbox")
	public WebElement checkBox;
	
	public List<WebElement> getProductList(){
		return productList;
	}
	
	public double getProductsSum() {
		int count = productList.size();
		double totalSum = 0;
		for(int i=0;i<count;i++) {
			String amountString = productList.get(i).getText();
			Double price = getFormattedAmount(amountString);
			totalSum += price;
		}
		return totalSum;
	}
	
	public Double getTotalAmountDisplayed() {
		return getFormattedAmount(totalAmount.getText());
	}
	
	public void acceptTermsConditions() {
		longPressAction(terms);
		acceptButton.click();
	}
	
	public Double getFormattedAmount(String amount) {
		Double price = Double.parseDouble(amount.substring(1));
		return price;
	}
	
	public void submitOrder() {
		checkBox.click();
		proceed.click();
	}
}
