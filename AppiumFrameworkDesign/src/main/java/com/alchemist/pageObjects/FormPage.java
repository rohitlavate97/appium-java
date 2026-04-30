package com.alchemist.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import com.alchemist.utils.AndroidActions;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class FormPage extends AndroidActions{
	AndroidDriver driver;
	
	public FormPage(AndroidDriver driver) {
		super(driver); //as we have inherited from parent class, parent class also asking driver, so giving info from child class
		//above line will call parent class constructor sending driver, this will fall into AndroidDriver driver instance,
		//and this.driver will be activated
		//driver is born into BaseTest -> It is coming here from test file
		//BaseTest sending to testcase -> from here it is going to pageObject -> Then to parent class AndroidDriver
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/nameField")
    private WebElement nameField;
	
	@AndroidFindBy(xpath="//android.widget.Radiobutton[@text='Female']")
    private WebElement femaleOption;
	
	@AndroidFindBy(xpath="//android.widget.Radiobutton[@text='Male']")
    private WebElement maleOption;
	
	@AndroidFindBy(id="android:id/text1")
    private WebElement countrySelection;
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/btn-LetsShop")
    private WebElement shopButton;
	
	public void setNameField(String name) {
		nameField.sendKeys(name);
		driver.hideKeyboard();
	}
	
	public void setGender(String gender) {
		if(gender.contains("female")) femaleOption.click();
		else maleOption.click();	
	}
	
	public void setCountrySelection(String countryName) {
		countrySelection.click();
		scrollToText(countryName);
		driver.findElement(By.xpath("//android.widget.Textview[@text='"+countryName+"']")).click();
	}
	
//	public void submitForm() {
//		shopButton.click();
//	}
	
	//As we know we are going to product page , so we will create object of new page as it landed on that page
	public ProductCatalogue submitForm() {
		shopButton.click();
		return new ProductCatalogue(driver);
	}
}
