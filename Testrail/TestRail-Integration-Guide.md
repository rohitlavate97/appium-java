# TestRail Integration for TestNG + Appium Framework

> Complete implementation guide — maps TestNG tests to TestRail cases, captures steps automatically via AspectJ, pushes results with Jenkins context, and supports parallel execution.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [End-to-End Flow](#2-end-to-end-flow)
3. [Project Structure](#3-project-structure)
4. [Maven Dependencies — pom.xml](#4-maven-dependencies--pomxml)
5. [Configuration — config.properties](#5-configuration--configproperties)
6. [Custom Annotations](#6-custom-annotations)
   - [@TestCase](#61-testcase)
   - [@Step](#62-step)
7. [Step Logger — Thread-Safe Step Storage](#7-step-logger--thread-safe-step-storage)
8. [TestRail Service — REST Assured API Layer](#8-testrail-service--rest-assured-api-layer)
9. [AspectJ Step Aspect — Automatic Step Capture](#9-aspectj-step-aspect--automatic-step-capture)
10. [TestNG Listener — Orchestrates Everything](#10-testng-listener--orchestrates-everything)
11. [Sample Page Object — LoginPage](#11-sample-page-object--loginpage)
12. [Sample Test Class — LoginTest](#12-sample-test-class--logintest)
13. [TestNG Suite XML](#13-testng-suite-xml)
14. [Logback Configuration](#14-logback-configuration)
15. [AspectJ Weaver Configuration — aop.xml](#15-aspectj-weaver-configuration--aopxml)
16. [Jenkins Pipeline Integration](#16-jenkins-pipeline-integration)
17. [How to Add to Your Existing Framework](#17-how-to-add-to-your-existing-framework)
18. [What Gets Posted to TestRail](#18-what-gets-posted-to-testrail)
19. [Thread Safety Explained](#19-thread-safety-explained)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        TEST EXECUTION                           │
│                                                                 │
│   LoginTest                                                     │
│   ├── @TestCase(caseId=101)                                     │
│   │   testValidLogin()                                          │
│   │     ├── loginPage.launchApp()       ← @Step("Launch App")   │
│   │     ├── loginPage.enterUsername()   ← @Step("Enter User")   │
│   │     ├── loginPage.enterPassword()  ← @Step("Enter Pass")   │
│   │     ├── loginPage.clickLogin()     ← @Step("Click Login")  │
│   │     └── loginPage.verifyDashboard()← @Step("Verify Dashboard")│
│   │                                                             │
└───┼─────────────────────────────────────────────────────────────┘
    │                         │
    │  AspectJ intercepts     │  TestNG fires
    │  every @Step call       │  listener events
    ▼                         ▼
┌──────────────┐     ┌──────────────────────┐
│  StepAspect  │     │   TestRailListener   │
│              │     │                      │
│  Logs each   │     │  onStart → createRun │
│  step as     │     │  onSuccess → PASSED  │
│  PASS / FAIL │     │  onFailure → FAILED  │
│              │     │  onSkipped → RETEST  │
│      │       │     │  onFinish → closeRun │
│      ▼       │     │          │           │
│  StepLogger  │────▶│  reads steps +       │
│  (ThreadLocal)│    │  builds comment      │
└──────────────┘     │          │           │
                     │          ▼           │
                     │  TestRailService     │
                     │  (REST Assured)      │
                     │          │           │
                     └──────────┼───────────┘
                                │
                                ▼
                     ┌──────────────────┐
                     │   TestRail API   │
                     │   /api/v2/...    │
                     └──────────────────┘
```

**5 components, 1 responsibility each:**

| Component | Responsibility |
|-----------|---------------|
| `@TestCase` | Maps a `@Test` method to a TestRail case ID |
| `@Step` | Marks a page-object method as a trackable step |
| `StepLogger` | Thread-safe in-memory storage for step results (ThreadLocal) |
| `StepAspect` | AspectJ around-advice that intercepts `@Step` and writes to StepLogger |
| `TestRailListener` | TestNG `ITestListener` that creates runs, pushes results, closes runs |
| `TestRailService` | REST Assured wrapper for TestRail API v2 |

---

## 2. End-to-End Flow

Here is exactly what happens when you run `mvn clean test`:

### Flow A — Dynamic Run (default, no run ID provided)

```
1.  Maven starts → Surefire plugin launches TestNG with -javaagent:aspectjweaver.jar
2.  TestNG reads testng.xml → finds LoginTest with TestRailListener
3.  TestRailListener.onStart() fires:
    a. Scans all @Test methods for @TestCase annotations
    b. Collects case IDs → [101, 102]
    c. resolveExistingRunId() returns 0 (no run ID configured)
    d. Calls TestRailService.createRun("JobName - Build #105", [101, 102])
    e. TestRail returns runId = 42
4.  TestNG starts testValidLogin():
    a. TestRailListener.onTestStart() → StepLogger.clear()
    b. loginPage.launchApp() is called
       → AspectJ StepAspect intercepts the @Step("Launch App") annotation
       → Method executes successfully
       → StepLogger.pass("Launch App") records "[PASS] Launch App (10:32:15.123)"
    c. Same happens for enterUsername, enterPassword, clickLogin, verifyDashboard
    d. All steps pass → StepLogger now holds 5 entries
5.  TestNG fires onTestSuccess():
    a. Listener reads caseId = 101 from @TestCase
    b. Reads all step entries from StepLogger
    c. Builds comment string with step log + Jenkins info
    d. Calls TestRailService.addResultForCase(42, 101, 1, comment)
    e. StepLogger.remove() cleans up ThreadLocal
6.  Repeats for testInvalidPassword (caseId = 102)
7.  TestNG fires TestRailListener.onFinish():
    a. resolveExistingRunId() == 0 → this was a dynamic run
    b. Calls TestRailService.closeRun(42)
    c. Run is locked in TestRail
```

### Flow B — Existing Run (`mvn test -Dtestrail.runId=456` or `TESTRAIL_RUN_ID=456`)

```
1.  Maven starts → same as above
2.  TestNG reads testng.xml → same as above
3.  TestRailListener.onStart() fires:
    a. Scans all @Test methods for @TestCase annotations
    b. Collects case IDs → [101, 102]
    c. resolveExistingRunId() returns 456
    d. runId = 456  (NO createRun API call!)
    e. Logs: "Using existing TestRail run: runId=456"
4-6. Same as Flow A — steps are captured, results pushed to run 456
7.  TestNG fires TestRailListener.onFinish():
    a. resolveExistingRunId() == 456 → pre-existing run
    b. Logs: "Skipping closeRun — run 456 was pre-existing."
    c. Run stays OPEN in TestRail (other jobs can still push to it)
```

**On failure, step 4 changes:**
```
    b-e. Steps pass normally, each logged as [PASS]
    f.   verifyDashboard() throws AssertionError
         → StepAspect catches it → StepLogger.fail("Verify Dashboard Loaded")
         → Re-throws exception to TestNG
    g.   TestNG fires onTestFailure() instead of onTestSuccess()
         → status_id = 5 (FAILED)
         → Comment includes failure message + stack trace
```

---

## 3. Project Structure

```
src/
├── main/java/com/automation/testrail/
│   ├── annotations/
│   │   ├── TestCase.java          ← Annotation: maps @Test → TestRail case ID
│   │   └── Step.java              ← Annotation: marks method as trackable step
│   ├── aspect/
│   │   └── StepAspect.java        ← AspectJ: intercepts @Step, logs PASS/FAIL
│   ├── listener/
│   │   └── TestRailListener.java  ← TestNG ITestListener: creates run, pushes results
│   ├── logger/
│   │   └── StepLogger.java        ← ThreadLocal step storage (parallel-safe)
│   └── service/
│       └── TestRailService.java   ← REST Assured calls to TestRail API v2
│
├── test/java/com/automation/testrail/
│   ├── pages/
│   │   └── LoginPage.java         ← Sample page with @Step methods
│   └── tests/
│       └── LoginTest.java         ← Sample tests with @TestCase annotation
│
└── test/resources/
    ├── config.properties           ← TestRail URL, username, apiKey
    ├── testng.xml                  ← Suite config: parallel="methods"
    ├── logback-test.xml            ← SLF4J logging config
    └── META-INF/
        └── aop.xml                 ← AspectJ load-time weaving config
```

---

## 4. Maven Dependencies — pom.xml

**File:** `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.automation</groupId>
    <artifactId>testrail-integration</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>TestRail Integration</name>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <aspectj.version>1.9.22.1</aspectj.version>
    </properties>

    <dependencies>
        <!-- TestNG -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>7.10.2</version>
        </dependency>

        <!-- Appium -->
        <dependency>
            <groupId>io.appium</groupId>
            <artifactId>java-client</artifactId>
            <version>9.3.0</version>
        </dependency>

        <!-- REST Assured — TestRail API calls -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>5.5.0</version>
        </dependency>

        <!-- AspectJ — automatic @Step capture -->
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <version>${aspectj.version}</version>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>${aspectj.version}</version>
        </dependency>

        <!-- SLF4J + Logback -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.16</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.12</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Surefire with AspectJ weaver agent -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                    <argLine>
                        -javaagent:"${settings.localRepository}/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar"
                    </argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### How pom.xml works:

| Section | Purpose |
|---------|---------|
| `testng 7.10.2` | Test runner — executes `@Test` methods, fires listener events |
| `java-client 9.3.0` | Appium driver library for mobile automation |
| `rest-assured 5.5.0` | HTTP client used by `TestRailService` to call TestRail API |
| `aspectjrt` + `aspectjweaver` | AspectJ runtime + load-time weaving agent |
| `slf4j-api` + `logback-classic` | Structured logging for API calls, step execution, errors |
| `maven-surefire-plugin` | Runs TestNG with `-javaagent:aspectjweaver.jar` so `@Step` interception works |

**Key detail:** The `-javaagent` arg in Surefire is what enables AspectJ load-time weaving. Without it, `StepAspect` won't intercept `@Step` methods.

---

## 5. Configuration — config.properties

**File:** `src/test/resources/config.properties`

```properties
# TestRail Configuration
testrail.url=https://yourcompany.testrail.io
testrail.username=your-email@company.com
testrail.apiKey=your-api-key
testrail.projectId=1
testrail.suiteId=1

# If a run already exists in TestRail, set the run ID here.
# Leave empty or 0 to create a new run dynamically.
testrail.runId=0
```

### How it works:

- `TestRailService` loads this file from the classpath at construction time.
- `testrail.url` — your TestRail instance base URL (no trailing slash).
- `testrail.username` — the email address of the TestRail user.
- `testrail.apiKey` — generated from TestRail → My Settings → API Keys. **Not** the password.
- `testrail.projectId` — numeric ID of the project (visible in the TestRail URL: `/index.php?/projects/overview/1`).
- `testrail.suiteId` — numeric ID of the test suite within that project.
- `testrail.runId` — **set to an existing run ID to push results to a pre-created run.** Set to `0` (default) to create a new run dynamically per execution.

> **Security note:** In production, inject `testrail.apiKey` as a Jenkins credential or environment variable. Never commit real API keys to source control.

---

## 6. Custom Annotations

### 6.1 @TestCase

**File:** `src/main/java/com/automation/testrail/annotations/TestCase.java`

```java
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
```

### How it works:

- `@Retention(RUNTIME)` — the annotation survives compilation and is readable via reflection at runtime. This is required because `TestRailListener` reads it during test execution.
- `@Target(METHOD)` — can only be placed on methods (not classes or fields).
- `caseId` — the numeric TestRail case ID. When you see `C101` in TestRail, use `caseId = 101`.
- `title` — optional human-readable description, useful for logging.

**Usage on a test method:**
```java
@Test
@TestCase(caseId = 101, title = "Verify successful login")
public void testValidLogin() { ... }
```

**How the listener reads it:**
```java
Method method = result.getMethod().getConstructorOrMethod().getMethod();
TestCase tc = method.getAnnotation(TestCase.class);
int caseId = tc.caseId();  // → 101
```

---

### 6.2 @Step

**File:** `src/main/java/com/automation/testrail/annotations/Step.java`

```java
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
```

### How it works:

- Placed on **page object methods**, not on test methods.
- The `value()` is the human-readable step description (e.g., `"Enter Username"`).
- You never call `StepLogger.pass()` manually — `StepAspect` handles that automatically.
- When AspectJ sees a call to an `@Step`-annotated method, it wraps the call in a try/catch. Success → `StepLogger.pass(value)`. Exception → `StepLogger.fail(value)`.

**Usage on a page method:**
```java
@Step("Enter Username")
public void enterUsername(String user) {
    driver.findElement(By.id("username")).sendKeys(user);
}
```

---

## 7. Step Logger — Thread-Safe Step Storage

**File:** `src/main/java/com/automation/testrail/logger/StepLogger.java`

```java
package com.automation.testrail.logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe step logger backed by ThreadLocal.
 * Stores each step's description and pass/fail status for the current test.
 * Safe for parallel="methods" execution.
 */
public final class StepLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Each thread accumulates its own ordered list of step entries. */
    private static final ThreadLocal<List<String>> STEPS = ThreadLocal.withInitial(ArrayList::new);

    private StepLogger() { }

    // ── public API ──────────────────────────────────────────────────

    /** Record a passed step. */
    public static void pass(String description) {
        log("PASS", description);
    }

    /** Record a failed step. */
    public static void fail(String description) {
        log("FAIL", description);
    }

    /** Return an unmodifiable snapshot of all steps collected so far. */
    public static List<String> getSteps() {
        return Collections.unmodifiableList(new ArrayList<>(STEPS.get()));
    }

    /** Build a single String with all steps (used for TestRail comment body). */
    public static String getStepsAsText() {
        List<String> steps = STEPS.get();
        if (steps.isEmpty()) {
            return "No steps recorded.";
        }
        StringBuilder sb = new StringBuilder("Step Execution Log:\n");
        for (int i = 0; i < steps.size(); i++) {
            sb.append(String.format("  %d. %s%n", i + 1, steps.get(i)));
        }
        return sb.toString();
    }

    /** Clear steps — call at the start of each test to avoid leakage. */
    public static void clear() {
        STEPS.get().clear();
    }

    /** Remove ThreadLocal entirely — call in afterMethod or listener cleanup. */
    public static void remove() {
        STEPS.remove();
    }

    // ── internals ───────────────────────────────────────────────────

    private static void log(String status, String description) {
        String timestamp = LocalDateTime.now().format(TS);
        String entry = String.format("[%s] %s  (%s)", status, description, timestamp);
        STEPS.get().add(entry);
    }
}
```

### How it works:

**The core mechanism is `ThreadLocal<List<String>>`.**

```
Thread-1 (testValidLogin):          Thread-2 (testInvalidPassword):
┌─────────────────────────┐         ┌─────────────────────────┐
│ [PASS] Launch App       │         │ [PASS] Launch App       │
│ [PASS] Enter Username   │         │ [PASS] Enter Username   │
│ [PASS] Enter Password   │         │ [PASS] Enter Password   │
│ [PASS] Click Login      │         │ [PASS] Click Login      │
│ [PASS] Verify Dashboard │         │ [FAIL] Verify Dashboard │
└─────────────────────────┘         └─────────────────────────┘
   ↑ completely isolated ↑            ↑ completely isolated ↑
```

- `ThreadLocal` gives each thread its own independent `ArrayList<String>`.
- When TestNG runs tests in parallel (`parallel="methods"`), each test method runs on a separate thread. Their steps never mix.
- `clear()` is called by the listener at `onTestStart()` to reset for a new test.
- `remove()` is called after results are pushed to free memory (prevents ThreadLocal leaks in thread pools).
- `getStepsAsText()` formats all collected steps into a numbered list for the TestRail comment.

**Example output of `getStepsAsText()`:**
```
Step Execution Log:
  1. [PASS] Launch App  (10:32:15.123)
  2. [PASS] Enter Username  (10:32:15.456)
  3. [PASS] Enter Password  (10:32:15.789)
  4. [PASS] Click Login Button  (10:32:16.012)
  5. [PASS] Verify Dashboard Loaded  (10:32:16.345)
```

---

## 8. TestRail Service — REST Assured API Layer

**File:** `src/main/java/com/automation/testrail/service/TestRailService.java`

```java
package com.automation.testrail.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Handles all communication with the TestRail API v2 via REST Assured.
 *
 * <p>Thread-safe: every method builds its own request; no shared mutable state.
 * Configuration is loaded once from {@code config.properties} on the classpath.</p>
 */
public class TestRailService {

    private static final Logger log = LoggerFactory.getLogger(TestRailService.class);

    private final String baseUrl;
    private final String username;
    private final String apiKey;
    private final int projectId;
    private final int suiteId;

    // ── construction ────────────────────────────────────────────────

    public TestRailService() {
        Properties props = loadProperties();
        this.baseUrl   = props.getProperty("testrail.url").replaceAll("/+$", "");
        this.username  = props.getProperty("testrail.username");
        this.apiKey    = props.getProperty("testrail.apiKey");
        this.projectId = Integer.parseInt(props.getProperty("testrail.projectId"));
        this.suiteId   = Integer.parseInt(props.getProperty("testrail.suiteId"));
    }

    // ── public API ──────────────────────────────────────────────────

    /**
     * Creates a new test run in TestRail.
     *
     * @param runName   display name for the run (e.g. "Regression - Build #105")
     * @param caseIds   array of case IDs to include
     * @return          the numeric run ID returned by TestRail
     */
    public int createRun(String runName, int[] caseIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("suite_id", suiteId);
        body.put("name", runName);
        body.put("include_all", false);
        body.put("case_ids", caseIds);

        Response response = post("/add_run/" + projectId, body);
        int runId = response.jsonPath().getInt("id");
        log.info("Created TestRail run '{}' → runId={}", runName, runId);
        return runId;
    }

    /**
     * Pushes a result (pass / fail / retest) for a specific case inside a run.
     *
     * @param runId     the run ID
     * @param caseId    the case ID
     * @param statusId  1=Passed, 4=Retest, 5=Failed
     * @param comment   free-text comment (step logs, Jenkins info, etc.)
     */
    public void addResultForCase(int runId, int caseId, int statusId, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("status_id", statusId);
        body.put("comment", comment);

        Response response = post("/add_result_for_case/" + runId + "/" + caseId, body);
        log.info("Result pushed for caseId={} in runId={} → status={}", caseId, runId, statusId);
    }

    /**
     * Closes (locks) a test run so no further results can be added.
     *
     * @param runId the run ID to close
     */
    public void closeRun(int runId) {
        post("/close_run/" + runId, new HashMap<>());
        log.info("Closed TestRail run {}", runId);
    }

    // ── internal helpers ────────────────────────────────────────────

    /**
     * Sends a POST request to the TestRail API.
     */
    private Response post(String apiPath, Map<String, Object> body) {
        String url = baseUrl + "/index.php?/api/v2" + apiPath;

        log.debug("POST {} → body={}", url, body);

        Response response = RestAssured
                .given()
                    .auth().preemptive().basic(username, apiKey)
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .post(url)
                .then()
                    .extract().response();

        int code = response.getStatusCode();
        if (code >= 400) {
            log.error("TestRail API error {}: {}", code, response.getBody().asString());
            throw new RuntimeException("TestRail API returned HTTP " + code);
        }
        return response;
    }

    /**
     * Loads config.properties from the classpath.
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = TestRailService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties not found on classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
        return props;
    }
}
```

### How it works:

**Three API calls, mapped to TestRail endpoints:**

| Method | TestRail API Endpoint | When Called |
|--------|----------------------|------------|
| `createRun()` | `POST /api/v2/add_run/{projectId}` | `onStart()` — once per suite |
| `addResultForCase()` | `POST /api/v2/add_result_for_case/{runId}/{caseId}` | `onTestSuccess/Failure/Skipped()` — once per test |
| `closeRun()` | `POST /api/v2/close_run/{runId}` | `onFinish()` — once per suite |

**Authentication flow:**
```
REST Assured builds:
  Authorization: Basic base64(username:apiKey)
  Content-Type: application/json
  
  POST https://yourcompany.testrail.io/index.php?/api/v2/add_run/1
  Body: {"suite_id":1, "name":"Regression - Build #105", "include_all":false, "case_ids":[101,102]}
  
  Response: {"id": 42, "name": "Regression - Build #105", ...}
```

**Thread safety:** Each method creates its own local `HashMap` and REST Assured `RequestSpecification`. No instance variables are mutated after construction. The `baseUrl`, `username`, `apiKey`, `projectId`, `suiteId` fields are all `final` — assigned once in the constructor and never changed. This makes the class safe for concurrent use.

**Error handling:** Any HTTP 4xx/5xx response is logged with the response body and throws a `RuntimeException`. This prevents silent failures where TestRail results appear to be pushed but actually aren't.

---

## 9. AspectJ Step Aspect — Automatic Step Capture

**File:** `src/main/java/com/automation/testrail/aspect/StepAspect.java`

```java
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
```

### How it works:

**What is AspectJ doing here?**

AspectJ is an Aspect-Oriented Programming (AOP) framework. It lets you wrap extra behavior around method calls without modifying those methods.

**Step by step:**

1. **`@Pointcut("@annotation(step)")`** — defines a pattern: "match any method that has the `@Step` annotation." The `step` parameter binds the annotation instance so we can read `step.value()`.

2. **`@Around("stepMethod(step)")`** — wraps the matched method. The original method doesn't run until `joinPoint.proceed()` is called.

3. **The interception flow:**

```
Your test calls:       loginPage.enterUsername("testuser")
                              ↓
AspectJ intercepts:    StepAspect.aroundStep() runs
                              ↓
                       Reads step.value() → "Enter Username"
                              ↓
                       Calls joinPoint.proceed() → executes the real enterUsername()
                              ↓
                       If no exception:  StepLogger.pass("Enter Username")
                       If exception:     StepLogger.fail("Enter Username") + re-throws
```

4. **`throw t`** — critical. The exception is re-thrown so TestNG still sees the failure and fires `onTestFailure()`. If we swallowed the exception, TestNG would think the test passed.

**Load-Time Weaving (LTW):**

AspectJ transforms bytecode at class-load time. The JVM flag `-javaagent:aspectjweaver.jar` tells the JVM to run the AspectJ weaver before each class is loaded. The weaver reads `META-INF/aop.xml` to know which aspects to apply and which packages to scan.

---

## 10. TestNG Listener — Orchestrates Everything

**File:** `src/main/java/com/automation/testrail/listener/TestRailListener.java`

```java
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
```

### How it works:

**Two Execution Modes:**

The listener supports two modes — **dynamic run creation** (default) and **existing run** — determined automatically at startup.

| Mode | When Triggered | onStart() | onFinish() |
|------|---------------|-----------|------------|
| **Dynamic** | `testrail.runId` is 0 or unset | Creates a new run via API | Closes the run |
| **Existing Run** | `testrail.runId` > 0 | Skips creation, uses provided ID | Does NOT close the run |

**Existing Run ID resolution priority:**

```
1. System property:       mvn test -Dtestrail.runId=456    ← highest priority
2. Environment variable:  TESTRAIL_RUN_ID=456              ← Jenkins parameter
3. config.properties:     testrail.runId=456               ← lowest priority
```

**Why not close a pre-existing run?**
When you have a run created in TestRail beforehand, you may have multiple Jenkins jobs (e.g., Android suite, iOS suite) pushing results into the *same* run. Closing it from one job would block the others. Leave it open and close it manually or from a final pipeline stage.

---

**Lifecycle mapping to TestRail operations:**

**Mode 1 — Dynamic run (testrail.runId = 0):**
```
TestNG Event              →  TestRail Action
──────────────────────────────────────────────────
onStart(context)          →  createRun("Regression - Build #105", [101, 102])
  │                                returns runId = 42
  ▼
onTestStart(result)       →  StepLogger.clear()
  │
  ▼  (test executes, StepAspect logs steps)
  │
onTestSuccess(result)     →  addResultForCase(42, 101, status=1, comment="...")
  OR
onTestFailure(result)     →  addResultForCase(42, 101, status=5, comment="...")
  OR
onTestSkipped(result)     →  addResultForCase(42, 101, status=4, comment="...")
  │
  ▼
onFinish(context)         →  closeRun(42)
```

**Mode 2 — Existing run (testrail.runId = 456):**
```
TestNG Event              →  TestRail Action
──────────────────────────────────────────────────
onStart(context)          →  runId = 456 (no API call)
  │
  ▼
onTestStart(result)       →  StepLogger.clear()
  │
  ▼  (test executes, StepAspect logs steps)
  │
onTestSuccess(result)     →  addResultForCase(456, 101, status=1, comment="...")
  OR
onTestFailure(result)     →  addResultForCase(456, 101, status=5, comment="...")
  OR
onTestSkipped(result)     →  addResultForCase(456, 101, status=4, comment="...")
  │
  ▼
onFinish(context)         →  (no closeRun — run stays open)
```

**Key design decisions explained:**

1. **`volatile int runId`** — `runId` is written once in `onStart()` (main thread) and read by `onTestSuccess/Failure/Skipped` (worker threads in parallel mode). `volatile` ensures the write is visible to all threads.

2. **`Collections.synchronizedList(new ArrayList<>())` for `caseIds`** — multiple `ITestMethod` entries are scanned in `onStart()`. Although this happens on one thread, the synchronized wrapper provides safety if the listener is ever reused.

3. **`ConcurrentHashMap<Integer, Boolean> pushedResults`** — prevents duplicate result uploads. When TestNG retries a failed test, both the failure and the retry success would trigger `pushResult()`. The `putIfAbsent()` ensures only the first push goes through (unless it's a PASS overriding a previous failure).

4. **`resolveExistingRunId()`** — three-layer resolution (system property → env var → config file) lets you override the run ID at any level without code changes. This enables:
   - Developers passing `-Dtestrail.runId=456` locally
   - Jenkins injecting `TESTRAIL_RUN_ID` as a build parameter
   - A permanent run ID baked into `config.properties` for fixed runs

5. **`onFinish()` conditional close** — when using an existing run, `closeRun()` is skipped. This is critical when multiple suites/jobs report into the same run. The run stays open until you close it manually or from a dedicated pipeline stage.

6. **`buildComment()` output** — assembles the full comment posted to TestRail:
```
Step Execution Log:
  1. [PASS] Launch App  (10:32:15.123)
  2. [PASS] Enter Username  (10:32:15.456)
  3. [PASS] Enter Password  (10:32:15.789)
  4. [PASS] Click Login Button  (10:32:16.012)
  5. [FAIL] Verify Dashboard Loaded  (10:32:16.345)

**Failure:**
Dashboard not visible after login

Duration: 1234 ms

Execution Details:
- Jenkins Job: MobileRegression
- Build Number: 105
- Device: Pixel 8
- Platform: Android 15
- Environment: staging
```

7. **Jenkins info** — `System.getenv("BUILD_NUMBER")` returns `null` when not in Jenkins, so the Jenkins section is simply omitted for local runs.

---

## 11. Sample Page Object — LoginPage

**File:** `src/test/java/com/automation/testrail/pages/LoginPage.java`

```java
package com.automation.testrail.pages;

import com.automation.testrail.annotations.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample page object for the Login screen.
 *
 * <p>In a real project each method would interact with Appium elements.
 * Here the Appium calls are stubbed out so the integration compiles
 * and runs without a live device.</p>
 */
public class LoginPage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    // In production: private final AppiumDriver driver;
    // public LoginPage(AppiumDriver driver) { this.driver = driver; }

    @Step("Launch App")
    public void launchApp() {
        log.info("Launching application...");
        // driver.launchApp();   ← real Appium call
    }

    @Step("Enter Username")
    public void enterUsername(String username) {
        log.info("Entering username: {}", username);
        // driver.findElement(By.id("username")).sendKeys(username);
    }

    @Step("Enter Password")
    public void enterPassword(String password) {
        log.info("Entering password: ****");
        // driver.findElement(By.id("password")).sendKeys(password);
    }

    @Step("Click Login Button")
    public void clickLogin() {
        log.info("Clicking login button");
        // driver.findElement(By.id("loginBtn")).click();
    }

    @Step("Verify Dashboard Loaded")
    public void verifyDashboard() {
        log.info("Verifying dashboard is displayed");
        // boolean displayed = driver.findElement(By.id("dashboard")).isDisplayed();
        // Assert.assertTrue(displayed, "Dashboard not visible after login");

        // Simulated — uncomment to see FAIL flow:
        // throw new AssertionError("Dashboard not visible after login");
    }
}
```

### How it works:

Each method has `@Step("...")` on it. When the test calls `loginPage.enterUsername("testuser")`:

1. **AspectJ intercepts** the call (because `enterUsername` has `@Step`).
2. `StepAspect.aroundStep()` reads `step.value()` → `"Enter Username"`.
3. Calls `joinPoint.proceed()` → the real `enterUsername()` executes.
4. No exception → `StepLogger.pass("Enter Username")`.
5. The step `[PASS] Enter Username (10:32:15.456)` is stored in the `ThreadLocal` list.

**In production** you would replace the log statements with real Appium calls:
```java
@Step("Enter Username")
public void enterUsername(String username) {
    driver.findElement(AppiumBy.id("username")).sendKeys(username);
}
```

---

## 12. Sample Test Class — LoginTest

**File:** `src/test/java/com/automation/testrail/tests/LoginTest.java`

```java
package com.automation.testrail.tests;

import com.automation.testrail.annotations.TestCase;
import com.automation.testrail.pages.LoginPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.automation.testrail.listener.TestRailListener;

/**
 * Sample test class demonstrating TestRail integration.
 *
 * <ul>
 *   <li>Each {@code @Test} is mapped to a TestRail case via {@code @TestCase}.</li>
 *   <li>Every {@code @Step}-annotated method in the page object is automatically
 *       intercepted by {@link com.automation.testrail.aspect.StepAspect} and logged
 *       into {@link com.automation.testrail.logger.StepLogger}.</li>
 *   <li>On completion, {@link TestRailListener} pushes status + step log to TestRail.</li>
 * </ul>
 */
@Listeners(TestRailListener.class)
public class LoginTest {

    private LoginPage loginPage;

    @BeforeMethod
    public void setUp() {
        // In production: initialize AppiumDriver and pass it to the page
        loginPage = new LoginPage();
    }

    @Test
    @TestCase(caseId = 101, title = "Verify successful login with valid credentials")
    public void testValidLogin() {
        loginPage.launchApp();
        loginPage.enterUsername("testuser");
        loginPage.enterPassword("P@ssw0rd");
        loginPage.clickLogin();
        loginPage.verifyDashboard();
    }

    @Test
    @TestCase(caseId = 102, title = "Verify login fails with invalid password")
    public void testInvalidPassword() {
        loginPage.launchApp();
        loginPage.enterUsername("testuser");
        loginPage.enterPassword("wrong");
        loginPage.clickLogin();
        // In production this step would throw an assertion → TestRail gets FAILED
        loginPage.verifyDashboard();
    }
}
```

### How it works:

- **`@Listeners(TestRailListener.class)`** — registers the listener for this class. Alternatively, register it globally in `testng.xml`.
- **`@TestCase(caseId = 101)`** — tells the listener which TestRail case to update when this test finishes.
- **`@BeforeMethod`** — runs before each `@Test`. Creates a fresh `LoginPage` per test (no shared state between tests).
- The test methods just call page-object methods. The `@Step` interception and TestRail pushing happen invisibly.

**What happens when `testValidLogin()` runs:**
```
setUp():
   → new LoginPage()

testValidLogin():
   → loginPage.launchApp()         → StepAspect → [PASS] Launch App
   → loginPage.enterUsername(...)   → StepAspect → [PASS] Enter Username
   → loginPage.enterPassword(...)  → StepAspect → [PASS] Enter Password
   → loginPage.clickLogin()        → StepAspect → [PASS] Click Login Button
   → loginPage.verifyDashboard()   → StepAspect → [PASS] Verify Dashboard Loaded

TestNG fires onTestSuccess():
   → TestRailListener reads caseId=101
   → Builds comment with 5 PASS steps + duration + Jenkins info
   → Calls addResultForCase(runId, 101, status=1, comment)
```

---

## 13. TestNG Suite XML

**File:** `src/test/resources/testng.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="TestRail Integration Suite" parallel="methods" thread-count="4">

    <listeners>
        <listener class-name="com.automation.testrail.listener.TestRailListener"/>
    </listeners>

    <test name="Login Tests">
        <classes>
            <class name="com.automation.testrail.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

### How it works:

- **`parallel="methods"`** — TestNG runs each `@Test` method on a separate thread from a pool of 4 threads. This is why `StepLogger` uses `ThreadLocal`.
- **`thread-count="4"`** — up to 4 tests execute simultaneously.
- **`<listeners>`** — registers `TestRailListener` globally for all tests in the suite. You can remove `@Listeners` from individual test classes if you register here.
- **`<test name="Login Tests">`** — the `name` attribute is used by `buildRunName()` if `JOB_NAME` env var isn't set (i.e., running locally).

---

## 14. Logback Configuration

**File:** `src/test/resources/logback-test.xml`

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>target/logs/testrail-integration.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>target/logs/testrail-integration.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- TestRail API traffic at DEBUG for troubleshooting -->
    <logger name="com.automation.testrail.service" level="DEBUG"/>
    <logger name="com.automation.testrail.aspect"  level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### How it works:

- **Console appender** — prints logs to standard output during test execution. Shows thread name (useful for parallel debugging).
- **File appender** — writes to `target/logs/testrail-integration.log` with daily rotation and 10MB max size.
- **`com.automation.testrail.service` at DEBUG** — logs every REST Assured request URL and body. Useful for debugging API issues.
- **`com.automation.testrail.aspect` at INFO** — logs each step execution without excessive detail.

**Example console output:**
```
10:32:15.100 [TestNG-method-1] INFO  c.a.t.aspect.StepAspect - ▶ Step: Launch App
10:32:15.120 [TestNG-method-1] INFO  c.a.t.pages.LoginPage - Launching application...
10:32:15.121 [TestNG-method-1] INFO  c.a.t.aspect.StepAspect -   ✓ PASS: Launch App
10:32:15.130 [TestNG-method-1] INFO  c.a.t.aspect.StepAspect - ▶ Step: Enter Username
10:32:15.145 [TestNG-method-1] INFO  c.a.t.pages.LoginPage - Entering username: testuser
10:32:15.146 [TestNG-method-1] INFO  c.a.t.aspect.StepAspect -   ✓ PASS: Enter Username
```

---

## 15. AspectJ Weaver Configuration — aop.xml

**File:** `src/test/resources/META-INF/aop.xml`

```xml
<aspectj>
    <weaver options="-verbose -showWeaveInfo">
        <!-- Only weave classes in our project packages -->
        <include within="com.automation.testrail..*"/>
    </weaver>

    <aspects>
        <aspect name="com.automation.testrail.aspect.StepAspect"/>
    </aspects>
</aspectj>
```

### How it works:

- **`<include within="com.automation.testrail..*"/>`** — tells AspectJ to only instrument classes in our packages. Without this, AspectJ would try to weave every class loaded by the JVM (including third-party libraries), which is slow and can cause errors.
- **`<aspect name="...StepAspect"/>`** — registers `StepAspect` as an active aspect.
- **`-verbose -showWeaveInfo`** — prints weaving details to the console. Remove these in production for cleaner output.

**This file must be at exactly `META-INF/aop.xml` on the classpath.** Maven places `src/test/resources/META-INF/aop.xml` into `target/test-classes/META-INF/aop.xml`, which is on the classpath during test execution.

---

## 16. Jenkins Pipeline Integration

### Jenkinsfile Example — Dynamic Run (creates a new run per build)

```groovy
pipeline {
    agent any

    environment {
        DEVICE      = 'Pixel 8'
        PLATFORM    = 'Android 15'
        ENVIRONMENT = 'staging'
    }

    stages {
        stage('Test') {
            steps {
                sh 'mvn clean test'
            }
        }
    }
}
```

### Jenkinsfile Example — Existing Run (push to a pre-created run)

```groovy
pipeline {
    agent any

    environment {
        DEVICE           = 'Pixel 8'
        PLATFORM         = 'Android 15'
        ENVIRONMENT      = 'staging'
        TESTRAIL_RUN_ID  = '456'    // ← pre-created run in TestRail
    }

    stages {
        stage('Test') {
            steps {
                sh 'mvn clean test'
            }
        }
    }
}
```

Or pass it as a Maven system property:
```groovy
stages {
    stage('Test') {
        steps {
            sh 'mvn clean test -Dtestrail.runId=456'
        }
    }
}
```

### Parameterized Jenkins Job (user picks run ID at build time)

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'TESTRAIL_RUN_ID', defaultValue: '0', description: 'TestRail Run ID (0 = create new)')
    }

    environment {
        TESTRAIL_RUN_ID = "${params.TESTRAIL_RUN_ID}"
        DEVICE          = 'Pixel 8'
        PLATFORM        = 'Android 15'
        ENVIRONMENT     = 'staging'
    }

    stages {
        stage('Test') {
            steps {
                sh 'mvn clean test'
            }
        }
    }
}
```

### How it works:

Jenkins automatically sets `BUILD_NUMBER` and `JOB_NAME`. The `environment` block adds `DEVICE`, `PLATFORM`, `ENVIRONMENT`.

`TestRailListener` reads these via `System.getenv()`:

| Variable | Source | Used In |
|----------|--------|---------|
| `BUILD_NUMBER` | Jenkins (automatic) | Run name: `"Regression - Build #105"` |
| `JOB_NAME` | Jenkins (automatic) | Run name prefix |
| `TESTRAIL_RUN_ID` | Jenkinsfile `environment` or parameter | Existing run mode — skips `createRun()` |
| `DEVICE` | Jenkinsfile `environment` | Comment: `"Device: Pixel 8"` |
| `PLATFORM` | Jenkinsfile `environment` | Comment: `"Platform: Android 15"` |
| `ENVIRONMENT` | Jenkinsfile `environment` | Comment: `"Environment: staging"` |

**When running locally (no Jenkins):**
- `BUILD_NUMBER` is `null` → run name becomes `"Login Tests - Build #local"`
- `TESTRAIL_RUN_ID` is `null` → a new run is created dynamically
- Jenkins info section is omitted from comments

**When using an existing run:**
- `TESTRAIL_RUN_ID=456` → run creation is skipped, results go straight to run 456
- `onFinish()` does NOT close the run (you may have other jobs pushing to it)

---

## 17. How to Add to Your Existing Framework

### Step 1: Add Maven Dependencies

Add these to your existing `pom.xml` `<dependencies>`:

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.5.0</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjrt</artifactId>
    <version>1.9.22.1</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.22.1</version>
</dependency>
```

Add the AspectJ weaver agent to Surefire:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
            -javaagent:"${settings.localRepository}/org/aspectj/aspectjweaver/1.9.22.1/aspectjweaver-1.9.22.1.jar"
        </argLine>
    </configuration>
</plugin>
```

### Step 2: Copy the 6 Java Files

Copy into your `src/main/java`:
- `annotations/TestCase.java`
- `annotations/Step.java`
- `logger/StepLogger.java`
- `service/TestRailService.java`
- `aspect/StepAspect.java`
- `listener/TestRailListener.java`

Adjust the package names to match your project.

### Step 3: Add Configuration Files

- `src/test/resources/config.properties` — add your TestRail credentials
- `src/test/resources/META-INF/aop.xml` — update the `within` pattern to your package

### Step 4: Annotate Your Tests

```java
@Test
@TestCase(caseId = 101, title = "Verify Login")
public void testLogin() { ... }
```

### Step 5: Annotate Your Page Methods

```java
@Step("Enter Username")
public void enterUsername(String user) {
    driver.findElement(By.id("username")).sendKeys(user);
}
```

### Step 6: Register the Listener

In `testng.xml`:
```xml
<listeners>
    <listener class-name="com.yourpackage.listener.TestRailListener"/>
</listeners>
```

---

## 18. What Gets Posted to TestRail

### Test Run (created once per suite execution):

```
Name: MobileRegression - Build #105
Suite: [your suite]
Cases: C101, C102
```

### Test Result (posted per test case):

**For a PASSED test:**
```
Status: Passed ✓
Comment:
    Step Execution Log:
      1. [PASS] Launch App  (10:32:15.123)
      2. [PASS] Enter Username  (10:32:15.456)
      3. [PASS] Enter Password  (10:32:15.789)
      4. [PASS] Click Login Button  (10:32:16.012)
      5. [PASS] Verify Dashboard Loaded  (10:32:16.345)

    Duration: 1234 ms

    Execution Details:
    - Jenkins Job: MobileRegression
    - Build Number: 105
    - Device: Pixel 8
    - Platform: Android 15
    - Environment: staging
```

**For a FAILED test:**
```
Status: Failed ✗
Comment:
    Step Execution Log:
      1. [PASS] Launch App  (10:32:17.100)
      2. [PASS] Enter Username  (10:32:17.200)
      3. [PASS] Enter Password  (10:32:17.300)
      4. [PASS] Click Login Button  (10:32:17.400)
      5. [FAIL] Verify Dashboard Loaded  (10:32:17.500)

    **Failure:**
    Dashboard not visible after login

    Duration: 2345 ms

    Execution Details:
    - Jenkins Job: MobileRegression
    - Build Number: 105
    - Device: Pixel 8
    - Platform: Android 15
    - Environment: staging
```

---

## 19. Thread Safety Explained

When `testng.xml` has `parallel="methods" thread-count="4"`, TestNG runs each `@Test` method on a different thread simultaneously.

**Problem:** If steps were stored in a shared `static List`, parallel tests would mix their steps together.

**Solution:** `ThreadLocal` — each thread has its own isolated list.

```
┌─────────────────────────────────────────────────────────────────┐
│                     JVM MEMORY                                  │
│                                                                 │
│  Thread-1 (testValidLogin)     Thread-2 (testInvalidPassword)   │
│  ┌───────────────────────┐     ┌───────────────────────────┐    │
│  │ ThreadLocal List:     │     │ ThreadLocal List:         │    │
│  │ [PASS] Launch App     │     │ [PASS] Launch App         │    │
│  │ [PASS] Enter Username │     │ [PASS] Enter Username     │    │
│  │ [PASS] Enter Password │     │ [PASS] Enter Password     │    │
│  │ [PASS] Click Login    │     │ [PASS] Click Login        │    │
│  │ [PASS] Verify Dash    │     │ [FAIL] Verify Dashboard   │    │
│  └───────────────────────┘     └───────────────────────────┘    │
│         ↓                               ↓                       │
│  onTestSuccess()                onTestFailure()                 │
│  reads THIS thread's steps      reads THIS thread's steps       │
│  → pushes PASSED + 5 steps      → pushes FAILED + 5 steps      │
│  StepLogger.remove()            StepLogger.remove()             │
└─────────────────────────────────────────────────────────────────┘
```

**Concurrency controls used:**

| Field | Type | Why |
|-------|------|-----|
| `StepLogger.STEPS` | `ThreadLocal<List<String>>` | Each thread gets its own list — no cross-talk |
| `TestRailListener.runId` | `volatile int` | Written once in `onStart`, visible to all worker threads |
| `TestRailListener.caseIds` | `synchronizedList` | Safe for concurrent reads during `onStart` scan |
| `TestRailListener.pushedResults` | `ConcurrentHashMap` | Atomic `putIfAbsent` prevents duplicate pushes on retries |
| `TestRailService` fields | `final` | Immutable after construction — inherently thread-safe |
