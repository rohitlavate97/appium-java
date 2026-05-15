package com.automation.testrail.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a TestNG test method to a TestRail case ID.
 *
 * Usage:
 * <pre>
 * {@code @TestCase(caseId = 101, title = "Verify Login")}
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestCase {

    /** TestRail case ID (the numeric part of C101). */
    int caseId();

    /** Descriptive title shown in TestRail run results. */
    String title() default "";
}
