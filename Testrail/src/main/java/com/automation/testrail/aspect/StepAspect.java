package com.automation.testrail.aspect;

import com.automation.testrail.annotations.Step;
import com.automation.testrail.logger.StepLogger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AspectJ aspect that intercepts every method annotated with {@link Step}.
 *
 * <p>Works with load-time weaving (LTW) via the {@code -javaagent:aspectjweaver.jar}
 * flag configured in the Maven Surefire plugin.</p>
 *
 * <p>On success the step is recorded as PASS; on exception it is recorded as FAIL
 * and the exception is re-thrown so TestNG still marks the test as failed.</p>
 */
@Aspect
public class StepAspect {

    private static final Logger log = LoggerFactory.getLogger(StepAspect.class);

    @Pointcut("@annotation(step)")
    public void stepMethod(Step step) { }

    @Around("stepMethod(step)")
    public Object aroundStep(ProceedingJoinPoint joinPoint, Step step) throws Throwable {
        String description = step.value();
        log.info("▶ Step: {}", description);

        try {
            Object result = joinPoint.proceed();
            StepLogger.pass(description);
            log.info("  ✓ PASS: {}", description);
            return result;

        } catch (Throwable t) {
            StepLogger.fail(description);
            log.error("  ✗ FAIL: {} → {}", description, t.getMessage());
            throw t;   // let TestNG handle the failure
        }
    }
}
