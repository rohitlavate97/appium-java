package com.automation.testrail.listener;

import com.automation.testrail.annotations.TestCase;
import com.automation.testrail.logger.StepLogger;
import com.automation.testrail.service.TestRailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG listener that bridges test execution to TestRail.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@code onStart}  – creates a TestRail run with all discovered case IDs.</li>
 *   <li>{@code onTestStart} – clears the step log for the new test.</li>
 *   <li>{@code onTestSuccess / onTestFailure / onTestSkipped} – pushes the result + step log.</li>
 *   <li>{@code onFinish} – closes the TestRail run.</li>
 * </ol>
 *
 * <p>Thread-safe: the listener itself is stateless per-test because step data lives
 * in {@link StepLogger}'s ThreadLocal.  The shared {@code runId} and
 * {@code caseIds} list are safely published via volatile / ConcurrentHashMap.</p>
 */
public class TestRailListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestRailListener.class);

    // TestRail status codes
    private static final int STATUS_PASSED = 1;
    private static final int STATUS_RETEST = 4;
    private static final int STATUS_FAILED = 5;

    private final TestRailService testRailService = new TestRailService();

    /** Set once in onStart, read by every callback. */
    private volatile int runId;

    /** Collects all caseIds discovered in the suite so we can create one run. */
    private final List<Integer> caseIds = Collections.synchronizedList(new ArrayList<>());

    /** Guards against duplicate result pushes when retries are enabled. */
    private final Map<Integer, Boolean> pushedResults = new ConcurrentHashMap<>();

    // ── suite-level hooks ───────────────────────────────────────────

    @Override
    public void onStart(ITestContext context) {
        // Scan all test methods for @TestCase and collect their IDs
        for (var tm : context.getAllTestMethods()) {
            getCaseId(tm.getConstructorOrMethod().getMethod())
                    .ifPresent(caseIds::add);
        }

        if (caseIds.isEmpty()) {
            log.warn("No @TestCase annotations found – TestRail run will NOT be created.");
            return;
        }

        // Check if an existing run ID is provided (via system property or env variable)
        // Priority: System property > Environment variable > config.properties > create new
        int existingRunId = resolveExistingRunId();

        if (existingRunId > 0) {
            this.runId = existingRunId;
            log.info("Using existing TestRail run: runId={}", runId);
        } else {
            String runName = buildRunName(context.getName());
            int[] ids = caseIds.stream().mapToInt(Integer::intValue).toArray();
            this.runId = testRailService.createRun(runName, ids);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        if (runId > 0) {
            // Only close the run if we created it (not if it was pre-existing)
            int existingRunId = resolveExistingRunId();
            if (existingRunId == 0) {
                testRailService.closeRun(runId);
            } else {
                log.info("Skipping closeRun — run {} was pre-existing.", runId);
            }
        }
        pushedResults.clear();
    }

    // ── test-level hooks ────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        StepLogger.clear();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        pushResult(result, STATUS_PASSED);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        pushResult(result, STATUS_FAILED);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        pushResult(result, STATUS_RETEST);
    }

    // ── internal helpers ────────────────────────────────────────────

    private void pushResult(ITestResult result, int statusId) {
        Optional<Integer> caseIdOpt = getCaseId(result.getMethod().getConstructorOrMethod().getMethod());
        if (caseIdOpt.isEmpty() || runId == 0) {
            return;
        }

        int caseId = caseIdOpt.get();

        // Avoid duplicate pushes on retry
        if (pushedResults.putIfAbsent(caseId, Boolean.TRUE) != null && statusId != STATUS_PASSED) {
            return;
        }

        String comment = buildComment(result, statusId);
        testRailService.addResultForCase(runId, caseId, statusId, comment);

        StepLogger.remove();
    }

    private static Optional<Integer> getCaseId(Method method) {
        TestCase tc = method.getAnnotation(TestCase.class);
        return tc != null ? Optional.of(tc.caseId()) : Optional.empty();
    }

    /**
     * Assembles a rich comment that includes step logs and Jenkins context.
     */
    private String buildComment(ITestResult result, int statusId) {
        StringBuilder sb = new StringBuilder();

        // Step log
        sb.append(StepLogger.getStepsAsText()).append("\n\n");

        // Failure trace
        if (statusId == STATUS_FAILED && result.getThrowable() != null) {
            sb.append("**Failure:**\n")
              .append(result.getThrowable().getMessage())
              .append("\n\n");
        }

        // Duration
        long durationMs = result.getEndMillis() - result.getStartMillis();
        sb.append(String.format("Duration: %d ms%n%n", durationMs));

        // Jenkins context (environment variables are available when running under Jenkins)
        appendJenkinsInfo(sb);

        return sb.toString();
    }

    private static void appendJenkinsInfo(StringBuilder sb) {
        String buildNumber = System.getenv("BUILD_NUMBER");
        if (buildNumber == null) {
            return; // not running in Jenkins
        }
        sb.append("Execution Details:\n");
        sb.append(String.format("- Jenkins Job: %s%n",   env("JOB_NAME")));
        sb.append(String.format("- Build Number: %s%n",  buildNumber));
        sb.append(String.format("- Device: %s%n",        env("DEVICE")));
        sb.append(String.format("- Platform: %s%n",      env("PLATFORM")));
        sb.append(String.format("- Environment: %s%n",   env("ENVIRONMENT")));
    }

    private static String env(String key) {
        return Optional.ofNullable(System.getenv(key)).orElse("N/A");
    }

    private String buildRunName(String contextName) {
        String buildNumber = Optional.ofNullable(System.getenv("BUILD_NUMBER")).orElse("local");
        String jobName     = Optional.ofNullable(System.getenv("JOB_NAME")).orElse(contextName);
        return String.format("%s - Build #%s", jobName, buildNumber);
    }

    /**
     * Resolves an existing TestRail run ID from (in priority order):
     * 1. System property: -Dtestrail.runId=123
     * 2. Environment variable: TESTRAIL_RUN_ID=123
     * 3. config.properties: testrail.runId=123
     *
     * Returns 0 if none is set → a new run will be created.
     */
    private static int resolveExistingRunId() {
        // 1. System property (e.g., mvn test -Dtestrail.runId=123)
        String sysProp = System.getProperty("testrail.runId");
        if (sysProp != null && !sysProp.isBlank()) {
            return Integer.parseInt(sysProp.trim());
        }

        // 2. Environment variable (e.g., Jenkins parameter TESTRAIL_RUN_ID)
        String envVar = System.getenv("TESTRAIL_RUN_ID");
        if (envVar != null && !envVar.isBlank()) {
            return Integer.parseInt(envVar.trim());
        }

        // 3. config.properties
        try (InputStream is = TestRailListener.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String val = props.getProperty("testrail.runId", "0");
                return Integer.parseInt(val.trim());
            }
        } catch (Exception e) {
            // ignore — will return 0
        }

        return 0;
    }
}
