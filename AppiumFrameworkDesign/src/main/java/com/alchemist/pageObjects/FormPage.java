package com.alchemist.pageObjects;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import com.alchemist.utils.AndroidActions;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class FormPage extends AndroidActions{
	AndroidDriver driver;
	
	public FormPage(AndroidDriver driver) {
		super(driver);
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	@AndroidFindBy(id="com.androidsample.generalstore:id/nameField")
    private WebElement nameField;
	
	@AndroidFindBy(xpath="//android.widget.Radiobutton[@text='Female']")
    private WebElement femaleOption;
	
	@AndroidFindBy(xpath="//android.widget.Radiobutton[@text='Male']")
    private WebElement maleOption;
	
	public void setNameField(String name) {
		nameField.sendKeys(name);
		driver.hideKeyboard();
	}
	
	public void setGender(String gender) {
		if(gender.contains("female")) femaleOption.click();
		else maleOption.click();	
	}
}
