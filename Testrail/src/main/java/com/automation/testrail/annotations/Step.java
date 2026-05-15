package com.automation.testrail.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a test step. Captured automatically by AspectJ.
 *
 * Usage:
 * <pre>
 * {@code @Step("Enter Username")}
 * public void enterUsername(String user) { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Step {

    /** Step description that appears in TestRail and logs. */
    String value();
}
