# Selenium + Java Interview Preparation — Senior SDET / Automation Architect

> **Role:** Principal SDET / Automation Architect (12+ years) | Java 17+ | Selenium 4 | Enterprise Scale
> **Target:** Senior QA Automation Engineer / SDET (6–10 years experience)
> **Companies:** Google, Amazon, Microsoft, Stripe, FinTech, SaaS, Enterprise

---

# SECTION 1 — THEORY INTERVIEW QUESTIONS (50)

---

## Q1: How does Selenium WebDriver communicate with the browser internally?

### Interview Answer
Selenium WebDriver communicates with browsers via the **W3C WebDriver Protocol** — an HTTP-based REST API. Your test code sends JSON commands to a browser driver (ChromeDriver, GeckoDriver), which translates them into browser-native instructions and returns JSON responses.

### Deep Explanation
- **Client (Java test)** → sends HTTP request (POST/GET/DELETE) to **browser driver** (local process)
- **Browser driver** → translates W3C commands to **browser DevTools / CDP / native automation API**
- **Browser** → executes action, returns result
- Each `WebDriver` session has a unique **session ID** maintained across all requests
- ChromeDriver uses **Chrome DevTools Protocol (CDP)** internally since Chrome 75+
- Architecture is **stateful** — the session ID binds the connection

```
[Java Test Code]
      │  HTTP POST /session/{id}/element
      ▼
[ChromeDriver (localhost:9515)]
      │  Chrome DevTools Protocol (CDP)
      ▼
[Chrome Browser Process]
      │  Returns element handle / result
      ▼
[ChromeDriver] → HTTP 200 JSON response → [Java Test]
```

### Java Example
```java
// What happens under the hood when you do this:
WebDriver driver = new ChromeDriver();
driver.get("https://example.com");
WebElement btn = driver.findElement(By.id("submit"));
btn.click();

// Actual HTTP calls made:
// POST /session → creates session, returns sessionId
// POST /session/{id}/url → navigates to URL
// POST /session/{id}/element → finds element, returns elementId
// POST /session/{id}/element/{eid}/click → clicks
```

### Real-world Usage
- Diagnosing **timeout errors** by checking if ChromeDriver crashed or is unreachable
- Understanding **session leaks** in parallel runs — each thread must own its session
- Debugging **stale element** errors by understanding element handle lifecycle

### Common Mistakes
- Assuming Selenium directly controls the browser — it doesn't; the driver process is the bridge
- Forgetting that `driver.quit()` sends `DELETE /session/{id}` — skipping it orphans browser processes
- Running multiple tests on the same driver instance across threads — session IDs are not thread-safe

### Optimization Tip
Use `ChromeDriverService` to reuse a single driver service process across tests rather than spawning a new process per test:
```java
ChromeDriverService service = new ChromeDriverService.Builder()
    .usingAnyFreePort().build();
service.start();
WebDriver driver = new RemoteWebDriver(service.getUrl(), new ChromeOptions());
```

### Debugging Strategy
1. Enable ChromeDriver verbose logging: `--verbose --log-path=chromedriver.log`
2. Inspect HTTP traffic with a proxy (Charles/Fiddler) on port 9515
3. Check `chromedriver.log` — every W3C command is logged with request/response body
4. Match `sessionId` in logs to the failing test thread

### Interview Trap
Interviewer is testing: Do you know the **protocol layer**, or do you just use `driver.click()`? They want to hear "W3C WebDriver Protocol", "ChromeDriver as HTTP server", and "CDP bridge" — not just "Selenium sends commands to the browser."

### Follow-up Questions
1. What changed from JSON Wire Protocol to W3C WebDriver Protocol and why does it matter?
2. How does Selenium 4's BiDi (BiDirectional) communication differ from the standard request/response model?

### Selenium vs Playwright
Playwright communicates via **CDP directly** (no intermediary driver process), making it faster and enabling native event interception. Selenium's driver abstraction provides cross-browser standardization but adds latency.

---

## Q2: What is the W3C WebDriver Protocol and how does it differ from JSON Wire Protocol?

### Interview Answer
W3C WebDriver (W3C spec, standardized 2018) is the **official browser automation standard** enforced by all browsers. It replaced Selenium's proprietary JSON Wire Protocol (JWIRE). Key difference: W3C is standardized at the browser level — ChromeDriver, GeckoDriver, and SafariDriver all conform to the same spec without Selenium-specific extensions.

### Deep Explanation
| Aspect | JSON Wire Protocol | W3C WebDriver |
|---|---|---|
| Standardization | Selenium-proprietary | W3C official spec |
| Error codes | Custom error objects | Standard HTTP status codes + error types |
| Actions API | Limited | Full pointer/keyboard/wheel input chains |
| Timeouts | Single timeout pattern | Structured `script`, `pageLoad`, `implicit` |
| Capabilities | `desiredCapabilities` | `alwaysMatch` / `firstMatch` |
| Browser support | Driver-dependent | Native browser implementation |

- W3C mandates **capabilities negotiation** via `alwaysMatch`/`firstMatch` — the server returns what it can actually provide
- **Actions API** in W3C supports complex input sequences: multi-finger touch, precise mouse trajectories, simultaneous key+mouse
- Error responses are standardized: `InvalidSelectorException`, `NoSuchElementException` now have spec-defined error codes

### Java Example
```java
// OLD — JSON Wire Protocol style (Selenium 3)
DesiredCapabilities caps = new DesiredCapabilities();
caps.setCapability("browserName", "chrome");

// NEW — W3C style (Selenium 4)
ChromeOptions options = new ChromeOptions();
options.addArguments("--headless=new");
options.setCapability("browserVersion", "stable");

// W3C Actions API — multi-step input chain
Actions actions = new Actions(driver);
actions.keyDown(Keys.CONTROL)
       .click(element1)
       .click(element2)
       .keyUp(Keys.CONTROL)
       .perform(); // sent as a single atomic W3C action sequence
```

### Real-world Usage
- Migrating a Selenium 3 → Selenium 4 framework: `DesiredCapabilities` must be replaced with browser-specific `Options` classes
- Cloud grid providers (BrowserStack, Sauce Labs) enforce W3C — legacy JWIRE capabilities are silently ignored, causing sessions to fail

### Common Mistakes
- Mixing `DesiredCapabilities` with `ChromeOptions` in Selenium 4 — causes capability merge conflicts
- Passing vendor-specific capabilities without the `goog:`, `moz:`, `ms:` namespace prefix — they're silently dropped by W3C-compliant drivers

### Optimization Tip
Always use `options.setCapability("goog:loggingPrefs", logPrefs)` (W3C namespace) rather than the legacy key. For BrowserStack/Sauce, use their W3C capability builders to avoid silent failures.

### Debugging Strategy
1. If session creation fails with `SessionNotCreatedException`, dump the raw capabilities JSON sent to the driver
2. Check driver logs for capability negotiation failures
3. Validate capabilities against the W3C spec endpoint: `GET /status`

### Interview Trap
They're testing whether you've actually migrated a Selenium 3 project to Selenium 4 — not just used it. Mention the `DesiredCapabilities` deprecation, W3C namespace requirements, and capability negotiation.

### Follow-up Questions
1. Why do some cloud providers reject tests even with seemingly correct Selenium 4 code?
2. What is BiDi (WebDriver BiDi) and what problem does it solve that W3C HTTP doesn't?

### Selenium vs Playwright
Playwright bypasses W3C entirely — it uses **CDP directly** and its own protocol, making it faster but less portable. W3C compliance is what gives Selenium cross-browser + cross-vendor portability.

---

## Q3: Explain Selenium Grid 4 architecture — all components and their roles.

### Interview Answer
Selenium Grid 4 uses a **distributed microservices architecture** with these components: **Router**, **Distributor**, **Session Map**, **Session Queue**, **Event Bus**, and **Nodes**. Each is independently deployable, unlike Grid 3's monolithic Hub+Node model.

### Deep Explanation
```
                    ┌─────────────┐
    Test Request ──►│   Router    │ ◄─── Entry point, routes to Queue or SessionMap
                    └──────┬──────┘
                           │
              ┌────────────┴──────────────┐
              ▼                           ▼
    ┌──────────────────┐      ┌──────────────────────┐
    │  Session Queue   │      │     Session Map       │
    │ (pending reqs)   │      │ (sessionId → nodeURL) │
    └────────┬─────────┘      └──────────────────────┘
             │
             ▼
    ┌──────────────────┐
    │   Distributor    │ ◄── Knows all available nodes + slots
    └────────┬─────────┘
             │  assigns slot
      ┌──────┴──────┐
      ▼             ▼
   [Node A]      [Node B]   ◄── run browsers, report via Event Bus
```

- **Router**: Receives all incoming requests. New session requests → Session Queue. Existing sessions → Session Map lookup → direct to Node
- **Distributor**: Maintains registry of nodes and available slots. Pulls from Session Queue, assigns to best node
- **Session Map**: Redis-backed (in distributed mode) mapping of `sessionId → nodeURL`
- **Session Queue**: Holds unassigned session creation requests with configurable timeout
- **Event Bus**: Internal pub/sub (default: in-process; external with ZeroMQ for distributed mode)
- **Node**: Runs actual browsers. Reports capabilities and slot availability to Distributor via Event Bus

### Java Example
```java
// Connecting to Grid 4
ChromeOptions options = new ChromeOptions();
options.addArguments("--headless=new");

WebDriver driver = new RemoteWebDriver(
    new URL("http://selenium-grid-host:4444"),
    options
);

// Grid 4 also supports fully distributed deployment via Docker Compose:
// Router, Distributor, SessionMap, SessionQueue each in separate containers
```

### Real-world Usage
- **Kubernetes deployment**: Each Grid component as a separate pod, auto-scaled via HPA
- **Session Map backed by Redis**: Multiple Router replicas can share session state
- **Node auto-scaling**: Kubernetes Job or cloud autoscaler spins up Node pods on demand

### Common Mistakes
- Treating Grid 4 like Grid 3 — trying to configure a "Hub" when the architecture is now Router+Distributor
- Not setting `--session-request-timeout` → session queue fills up silently, tests hang
- Using Docker without `--network host` on Linux → nodes can't reach the router

### Optimization Tip
In high-throughput environments, externalize the Session Map to Redis and run multiple Router instances behind a load balancer. This eliminates the single-point-of-failure in Grid 3's Hub.

### Debugging Strategy
1. Hit `http://grid-host:4444/status` → shows node count, slot availability, session queue depth
2. Grid 4 GraphQL API: `POST /graphql` → query sessions, nodes, queue state
3. Node logs show browser process spawn/death — check for OOM or zombie processes
4. Enable Grid 4 tracing with OpenTelemetry for distributed span tracking

### Interview Trap
Interviewers check if you know **why** Grid 4 changed from Hub+Node. The answer: **scalability and fault isolation**. In Grid 3, Hub failure = grid failure. Grid 4 allows component-level failure without total grid collapse.

### Follow-up Questions
1. How would you implement auto-scaling of Grid 4 nodes in Kubernetes based on session queue depth?
2. What is the role of the Event Bus and what happens if it goes down?

### Selenium vs Playwright
Playwright has no native grid equivalent — you'd use **Playwright Test's `workers`** for parallelism or rely on CI parallelization. For large enterprise grids (500+ concurrent sessions), Selenium Grid is the production standard.

---

## Q4: What are the different wait strategies in Selenium and which is production-grade?

### Interview Answer
Selenium has three wait types: **Implicit Wait** (global DOM polling), **Explicit Wait** (`WebDriverWait` + `ExpectedConditions`), and **Fluent Wait** (custom polling interval + exception ignoring). Production systems use **Fluent Wait or custom explicit waits** — never implicit wait in combination with explicit wait.

### Deep Explanation
**Implicit Wait:**
- Set once on the driver, applies to every `findElement` call globally
- Polls the DOM up to the timeout before throwing `NoSuchElementException`
- **Problem**: Interacts with explicit wait in unpredictable ways — combined timeouts can cause 2x waits

**Explicit Wait (`WebDriverWait`):**
- Waits for a specific condition using `ExpectedConditions`
- Polls every 500ms by default, throws `TimeoutException` if condition not met
- Clean, scoped per-element, composable

**Fluent Wait:**
- Superset of explicit wait — custom polling interval, ignore specific exceptions, custom message
- Production-grade: handles `StaleElementReferenceException` during polling

### Java Example
```java
// NEVER mix implicit + explicit — undefined behavior
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // DON'T combine with explicit

// PRODUCTION: Custom Fluent Wait
public WebElement waitForElement(WebDriver driver, By locator, int timeoutSec) {
    return new FluentWait<>(driver)
        .withTimeout(Duration.ofSeconds(timeoutSec))
        .pollingEvery(Duration.ofMillis(300))
        .ignoring(NoSuchElementException.class)
        .ignoring(StaleElementReferenceException.class)
        .withMessage("Element not found: " + locator)
        .until(d -> d.findElement(locator));
}

// PRODUCTION: Custom ExpectedCondition
public ExpectedCondition<WebElement> elementHasText(By locator, String text) {
    return driver -> {
        WebElement el = driver.findElement(locator);
        return el.getText().contains(text) ? el : null;
    };
}
```

### Real-world Usage
- **SPA applications (React/Angular)**: Elements exist in DOM but aren't interactive — use `elementToBeClickable`
- **API-driven loading**: Use explicit wait on a specific data attribute set by JS when data loads
- **Flaky CI tests**: Almost always caused by missing/inconsistent waits — Fluent Wait fixes 80% of flakiness

### Common Mistakes
- Using `Thread.sleep()` — hardcoded pause, non-deterministic, slows suite by minutes
- Using only `visibilityOfElementLocated` — element can be visible but still not interactable
- Setting global implicit wait with any explicit wait — they don't compose cleanly in all drivers

### Optimization Tip
Build a **Wait Utility Layer** as a separate class with named wait methods. Never scatter `WebDriverWait` instantiation across page objects:
```java
public class WaitUtils {
    private static final int DEFAULT_TIMEOUT = 15;
    private final WebDriver driver;

    public WaitUtils(WebDriver driver) { this.driver = driver; }

    public WebElement clickable(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
            .until(ExpectedConditions.elementToBeClickable(locator));
    }
}
```

### Debugging Strategy
1. `TimeoutException` with no inner cause → element never appeared — check network tab for failed API call
2. `StaleElementReferenceException` during wait → DOM was rebuilt — add `ignoring(StaleElementReferenceException.class)`
3. Element found but click has no effect → use `elementToBeClickable` not `visibilityOf`
4. Capture `System.currentTimeMillis()` delta for slow-element diagnostics

### Interview Trap
They'll ask "what's wrong with implicit wait?" — the real answer: **implicit + explicit wait interaction is browser-driver-specific and can cause total wait time = implicit + explicit**, making timeouts unpredictable.

### Follow-up Questions
1. How do you handle waiting for an element that disappears and reappears (e.g., loading spinner)?
2. How would you implement a wait that retries the entire page action on failure?

### Selenium vs Playwright
Playwright has **auto-waiting** built into every action — `click()`, `fill()`, `check()` all wait for the element to be actionable. This eliminates the manual wait layer entirely.

---

## Q5: How does `StaleElementReferenceException` occur and how do you handle it in production?

### Interview Answer
`StaleElementReferenceException` occurs when the **DOM node a `WebElement` reference points to has been destroyed and recreated**. The element handle is invalidated. This happens with React/Angular re-renders, AJAX updates, or any DOM mutation after `findElement` was called.

### Deep Explanation
- When you call `driver.findElement(By.id("btn"))`, Selenium creates an **element reference** — a handle (UUID) mapping to a live DOM node
- If JavaScript rebuilds that DOM node (React state change, innerHTML reset), the old handle is **orphaned**
- Any subsequent call using that `WebElement` object throws `StaleElementReferenceException`
- The element may still exist visually — but it's a **new DOM node** with a new element ID

```
findElement("btn") → handle: "abc-123" → DOM node #45
  ... React re-renders ...
DOM node #45 is destroyed, new DOM node #47 created for same button
element.click()  → Selenium sends: click on handle "abc-123"
                 → Browser: "abc-123" not found → StaleElementReferenceException
```

### Java Example
```java
// NAIVE — will throw StaleElementReferenceException on re-renders
WebElement btn = driver.findElement(By.id("submit"));
// ... something causes DOM re-render ...
btn.click(); // BOOM

// PRODUCTION APPROACH 1: Re-find on stale
public void clickWithRetry(WebDriver driver, By locator, int maxRetries) {
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try {
            driver.findElement(locator).click();
            return;
        } catch (StaleElementReferenceException e) {
            if (attempt == maxRetries - 1) throw e;
        }
    }
}

// PRODUCTION APPROACH 2: Fluent Wait ignoring stale
new FluentWait<>(driver)
    .withTimeout(Duration.ofSeconds(10))
    .pollingEvery(Duration.ofMillis(200))
    .ignoring(StaleElementReferenceException.class)
    .until(d -> {
        d.findElement(By.id("submit")).click();
        return true;
    });

// PRODUCTION APPROACH 3: LazyElement (self-healing)
public class LazyElement {
    private final WebDriver driver;
    private final By locator;

    public LazyElement(WebDriver driver, By locator) {
        this.driver = driver;
        this.locator = locator;
    }

    public void click() {
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(10))
            .ignoring(StaleElementReferenceException.class)
            .until(d -> { d.findElement(locator).click(); return true; });
    }
}
```

### Real-world Usage
- **React/Angular SPAs**: Every state change can trigger virtual DOM diffing and real DOM replacement
- **Data tables with live refresh**: Row elements go stale when table data updates every 30s
- **Autocomplete dropdowns**: DOM is rebuilt on each keystroke

### Common Mistakes
- Storing `WebElement` objects as instance variables in Page Objects — they go stale between method calls
- Not understanding that `isDisplayed()` also throws `StaleElementReferenceException`
- Using `try/catch StaleElement` without re-finding — the catch block does nothing useful

### Optimization Tip
**Never store `WebElement` references as Page Object fields.** Always call `findElement` at method invocation time:
```java
// BAD
public class LoginPage {
    private WebElement emailField = driver.findElement(By.id("email")); // stale risk
}

// GOOD
public class LoginPage {
    public void enterEmail(String email) {
        driver.findElement(By.id("email")).sendKeys(email); // re-finds every time
    }
}
```

### Debugging Strategy
1. Log the locator in the exception message — know which element went stale
2. Add CDP Network listener to detect in-flight XHR/fetch calls that trigger DOM updates
3. Check browser console for React/Angular re-render logs at time of exception
4. Use `MutationObserver` via `JavascriptExecutor` to detect DOM mutations during test step

### Interview Trap
Interviewer wants to know if you **understand the root cause** (element handle invalidation) vs. just retrying blindly. Mention DOM node lifecycle, virtual DOM reconciliation in SPAs, and **why storing `WebElement` references is an anti-pattern**.

### Follow-up Questions
1. How would you implement a truly self-healing element that falls back to alternative locators?
2. What is the difference between `StaleElementReferenceException` and `ElementNotInteractableException`?

### Selenium vs Playwright
Playwright stores **element locators, not element handles** — its `Locator` API re-queries the DOM on every action, making `StaleElementReferenceException` essentially impossible.

---

## Q6: How do you write production-grade XPath and what are the optimization rules?

### Interview Answer
Production XPath must be **short, attribute-based, and context-anchored** — never positional (`[1]`, `[2]`), never absolute (`/html/body/div[3]/...`), and never text-dependent unless the text is a stable business label. Optimized XPath uses **unique, stable attributes** and avoids full DOM traversal.

### Deep Explanation
XPath evaluation in browsers works via the **XPath engine in the DOM** — it walks the node tree. Performance and fragility both stem from how much of the tree you force it to traverse.

**XPath types:**
- **Absolute**: `/html/body/div[1]/form/input` — breaks on any layout change, never use
- **Relative with position**: `//div[3]/span` — brittle, breaks on reorder
- **Attribute-based**: `//input[@data-testid='email']` — stable, production-grade
- **Axis-based**: `//label[text()='Email']/following-sibling::input` — useful for forms without IDs
- **Compound**: `//div[@class='modal' and @aria-label='Login']` — narrows scope

**Optimization rules:**
1. Always start with `//tag[@attr='value']` — give the engine a tag + attribute to filter immediately
2. Use `@data-testid`, `@id`, `@name` first — guaranteed unique when present
3. Avoid `contains(@class, 'btn')` when class lists are dynamic (CSS modules add hashes)
4. Use `normalize-space()` for text matching — strips leading/trailing whitespace
5. Anchor to a stable parent to reduce traversal scope: `//form[@id='login']//input[@type='password']`

### Java Example
```java
// BAD — absolute, positional, fragile
By bad = By.xpath("/html/body/div[2]/div/form/div[1]/input");

// BAD — class-dependent (CSS modules change classes)
By fragile = By.xpath("//input[@class='input-field--abc123']");

// GOOD — data-testid (agreed with dev team)
By good = By.xpath("//input[@data-testid='email-input']");

// GOOD — axis navigation for label-input association
By axisXpath = By.xpath("//label[normalize-space()='Email Address']/following-sibling::input[1]");

// GOOD — scoped to stable parent
By scoped = By.xpath("//form[@id='login-form']//button[@type='submit']");

// GOOD — dynamic XPath builder (production utility)
public static By byDataTestId(String testId) {
    return By.xpath(String.format("//*[@data-testid='%s']", testId));
}

public static By byLabelText(String labelText) {
    return By.xpath(
        String.format("//label[normalize-space()='%s']/following-sibling::*[1]", labelText)
    );
}
```

### Real-world Usage
- **Agree `data-testid` attributes with dev team** — most scalable pattern; devs add stable hooks, QA uses them
- **Legacy apps without IDs**: axis-navigation (parent/sibling/ancestor) is the only viable strategy
- **Dynamic tables**: `//tr[td[normalize-space()='John Doe']]/td[@data-col='status']` — find row by content, extract specific column

### Common Mistakes
- Using `//div[@class='btn btn-primary active']` — CSS class order and values change with state
- Text-based XPath without `normalize-space()` — fails on whitespace differences between environments
- Over-using `contains()` — `contains(@class, 'active')` matches unintended elements if class 'active' appears in multiple components

### Optimization Tip
Establish a **test ID contract** with the development team: every interactive element gets a `data-testid` attribute. This eliminates XPath complexity entirely for most locators and makes automation resilient to UI redesigns.

### Debugging Strategy
1. Test XPath in Chrome DevTools Console: `$x("//input[@data-testid='email']")` — instant feedback
2. If XPath returns 0 nodes: check if element is inside a frame or Shadow DOM (XPath can't cross these boundaries)
3. If XPath returns multiple nodes: add more attribute constraints or a parent scope
4. Use `driver.findElements(By.xpath(...)).size()` to count matches before asserting uniqueness

### Interview Trap
They want to see if you know the **performance hierarchy**: `@id` > `@data-testid` > `@name` > attribute combination > text contains > positional. Also — can you explain **why** `//` is slower than `./` when you have a parent context? (`//` searches entire subtree from root; `./` searches only from current node.)

### Follow-up Questions
1. When would you choose XPath over CSS selector, and vice versa?
2. How do you write an XPath to select a table row where one cell contains specific text and extract a value from another cell in the same row?

### Selenium vs Playwright
Playwright uses **CSS selectors and its own locator engine** (`getByRole`, `getByLabel`, `getByTestId`) — it avoids XPath entirely in most cases. Playwright's `getByRole` uses ARIA semantics, making tests accessible-aware. Selenium requires manual XPath/CSS discipline.

---

## Q7: CSS Selectors vs XPath — which is faster and when do you use each?

### Interview Answer
**CSS selectors are generally faster** than XPath because browser engines have native CSS selector optimization. Use CSS selectors by default; use XPath only when you need **axis traversal** (parent, sibling, ancestor), **text matching**, or navigating relationships CSS can't express.

### Deep Explanation
**Why CSS is faster:**
- Browsers use CSS for rendering — the CSS selector engine is highly optimized (right-to-left matching)
- XPath requires a separate XPath engine that traverses the full DOM tree
- For simple attribute lookups, CSS is 20–50% faster in large DOMs

**What CSS can't do (XPath's exclusive territory):**
- Navigate to a **parent element**: `//span/parent::div` — CSS has no parent selector (`:has()` is emerging but not universal)
- Match by **text content**: `//button[text()='Submit']` — CSS has no text-content selector
- Navigate to **preceding sibling**: `//input/preceding-sibling::label`
- **Ancestor** traversal: `//td/ancestor::table[@id='data']`

**CSS selector speed hierarchy (fastest to slowest):**
1. `#id` — O(1) hash lookup
2. `[data-testid='value']` — attribute index lookup
3. `tag.class` — tag filter then class check
4. `.class` — class lookup
5. `tag` — all elements of type
6. `*` (universal) — slowest, avoid

### Java Example
```java
// CSS — preferred for attribute-based identification
By cssById = By.cssSelector("#submit-btn");
By cssDataAttr = By.cssSelector("[data-testid='email-input']");
By cssCombo = By.cssSelector("form.login-form input[type='password']");
By cssState = By.cssSelector("button:not([disabled])");
By cssPseudo = By.cssSelector("ul.menu > li:first-child > a");
By cssChild = By.cssSelector("div.card > .card-body > h3");

// XPath — use when CSS is insufficient
// 1. Parent navigation
By xpathParent = By.xpath("//span[@class='error']/parent::div");
// 2. Text content matching
By xpathText = By.xpath("//button[normalize-space()='Place Order']");
// 3. Sibling-based (find input near its label)
By xpathSibling = By.xpath("//label[text()='Password']/following-sibling::input");
// 4. Row with specific cell value
By xpathTableRow = By.xpath("//tr[td[normalize-space()='INV-001']]/td[3]");
```

### Real-world Usage
- **Standard form automation**: CSS selectors exclusively (`[data-testid]`, `input[name='email']`)
- **Table row extraction**: XPath — `//tr[td[text()='Active']]/td[@data-col='amount']`
- **Accessible UI**: CSS `:has()` (Chrome 105+) or XPath parent axis for label-input association
- **State-dependent elements**: CSS `:not([disabled])`, `[aria-expanded='true']`

### Common Mistakes
- Using XPath everywhere out of habit — CSS is cleaner and faster for 80% of cases
- Using CSS descendant combinator (space) when child combinator (`>`) is more precise — performance and accuracy differ
- `[class='btn primary']` — exact class match fails if element has additional classes; use `[class*='btn']` or `.btn`
- Forgetting CSS attribute selectors: `[href$='.pdf']` (ends with), `[href^='https']` (starts with), `[class*='modal']` (contains)

### Optimization Tip
Agree on `[data-testid]` attributes with developers. This makes both CSS and XPath trivially simple and immune to structural changes. For teams without `data-testid`, build a **locator strategy matrix**: use CSS for structure, XPath only for text/relationship navigation.

### Debugging Strategy
1. Test CSS in Chrome DevTools: `$$("input[data-testid='email']")` in Console
2. CSS returns wrong element: inspect computed styles and check specificity conflicts
3. XPath returns element, CSS doesn't: likely needs parent/text traversal — confirm with DevTools XPath: `$x("...")`
4. Use `driver.findElements(locator).size()` to verify uniqueness before use in production

### Interview Trap
Interviewers ask "which is faster?" expecting you to say CSS — but the **real answer** is: "CSS is generally faster for attribute/structure matching; XPath is unavoidable for text and relationship traversal. In practice, with `data-testid`, both are fast enough that the difference is negligible." Nuance > blanket statement.

### Follow-up Questions
1. How would you handle a scenario where neither CSS nor XPath can uniquely identify an element?
2. What is the `:has()` CSS selector and can it replace some XPath use cases in modern browsers?

### Selenium vs Playwright
Playwright's locator engine (`getByRole`, `getByLabel`, `getByText`, `getByTestId`) abstracts away both CSS and XPath. It uses ARIA tree + DOM queries internally — making locator strategy a design concern, not a syntax concern.

---

## Q8: How do you interact with Shadow DOM elements in Selenium?

### Interview Answer
Shadow DOM elements are **encapsulated — Selenium's standard `findElement` cannot cross the shadow boundary**. In Selenium 4, use `getShadowRoot()` on the shadow host element to get a `SearchContext` for the shadow tree. For deep/nested shadows, chain `getShadowRoot()` calls or use `JavascriptExecutor` as fallback.

### Deep Explanation
**Shadow DOM structure:**
```
<div id="host">          ← Shadow Host (regular DOM)
  #shadow-root (open)    ← Shadow Root (boundary)
    <input id="inner">   ← Shadow DOM element (invisible to standard findElement)
```

- `open` mode: JavaScript can access `host.shadowRoot` — Selenium 4 `getShadowRoot()` works
- `closed` mode: `host.shadowRoot` returns `null` — only JS injection workaround exists
- **XPath and CSS cannot cross shadow boundaries** — `driver.findElement(By.cssSelector("#inner"))` returns `NoSuchElementException`
- Selenium 4 introduced `WebElement.getShadowRoot()` returning a `SearchContext` — you can call `findElement` on it

**Nesting:**
Each shadow host has its own shadow root. To traverse nested shadows, you must chain `getShadowRoot()` at each level.

### Java Example
```java
// SELENIUM 4 — Native Shadow DOM support
WebElement shadowHost = driver.findElement(By.cssSelector("#checkout-widget"));
SearchContext shadowRoot = shadowHost.getShadowRoot();
WebElement shadowInput = shadowRoot.findElement(By.cssSelector("input[name='card-number']"));
shadowInput.sendKeys("4111111111111111");

// NESTED SHADOW DOM
WebElement outerHost = driver.findElement(By.cssSelector("payment-form"));
SearchContext outerShadow = outerHost.getShadowRoot();
WebElement innerHost = outerShadow.findElement(By.cssSelector("card-input"));
SearchContext innerShadow = innerHost.getShadowRoot();
WebElement cardField = innerShadow.findElement(By.cssSelector("input.card-number"));

// FALLBACK — JavascriptExecutor (works for both open and closed shadows)
public WebElement findInShadowDOM(WebDriver driver, String hostSelector, String innerSelector) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return (WebElement) js.executeScript(
        "return document.querySelector(arguments[0]).shadowRoot.querySelector(arguments[1])",
        hostSelector, innerSelector
    );
}

// DEEP NESTED via JS (chained)
public WebElement deepShadowQuery(WebDriver driver, String... selectors) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    StringBuilder script = new StringBuilder("return document");
    for (int i = 0; i < selectors.length - 1; i++) {
        script.append(String.format(".querySelector('%s').shadowRoot", selectors[i]));
    }
    script.append(String.format(".querySelector('%s')", selectors[selectors.length - 1]));
    return (WebElement) js.executeScript(script.toString());
}
```

### Real-world Usage
- **Web Components** (Polymer, Lit, FAST): All internal elements are in shadow DOM — banking and enterprise SaaS use Web Components heavily
- **Browser built-in controls**: `<input type='date'>`, `<video>` controls, `<details>` are often shadow DOM in Chrome
- **Salesforce Lightning**: Deeply nested shadow DOMs — requires chained `getShadowRoot()` or JS utility

### Common Mistakes
- Using XPath to find shadow DOM elements — XPath **cannot pierce shadow boundaries**, always returns empty
- Not checking if shadow mode is `open` or `closed` — `closed` mode requires JS injection
- Using `driver.findElement` directly on shadow children — works in some older ChromeDriver versions but is spec non-compliant and breaks
- Forgetting that CSS selectors also can't cross shadow boundaries in `SearchContext` — must use `getShadowRoot()` chain

### Optimization Tip
Build a `ShadowDOMUtil` class with a recursive shadow query utility. For deeply nested components (3+ levels), JS-based `querySelectorAll` with `>>>` (Chrome's shadow-piercing combinator, deprecated) or chained `shadowRoot` traversal is cleaner than API chaining.

```java
public class ShadowDOMUtil {
    private final JavascriptExecutor js;

    public ShadowDOMUtil(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    public WebElement query(String... cssPath) {
        // cssPath: ["outer-host", "inner-host", "target-element"]
        StringBuilder sb = new StringBuilder("return document");
        for (int i = 0; i < cssPath.length - 1; i++) {
            sb.append(".querySelector('").append(cssPath[i]).append("').shadowRoot");
        }
        sb.append(".querySelector('").append(cssPath[cssPath.length - 1]).append("')");
        return (WebElement) js.executeScript(sb.toString());
    }
}
```

### Debugging Strategy
1. In Chrome DevTools: `document.querySelector('#host').shadowRoot` — if `null`, shadow is closed
2. Enable "Show user agent shadow DOM" in DevTools settings to inspect browser-native shadow DOMs
3. If `getShadowRoot()` throws `UnsupportedOperationException`: driver doesn't support Shadow DOM API — update ChromeDriver
4. For closed shadow DOM: network-intercept the Web Component's JS to check if there's an exposed accessor

### Interview Trap
The interviewer wants to know **why** standard `findElement` fails — not just that it does. Explain **encapsulation**, **shadow boundary**, and the difference between `open` and `closed` shadow modes. Mention that XPath cannot pierce shadow roots.

### Follow-up Questions
1. How would you automate a Salesforce Lightning component that has 4 levels of nested Shadow DOM?
2. What is the difference between `open` and `closed` Shadow DOM and how does it affect testability?

### Selenium vs Playwright
Playwright **automatically pierces shadow DOM** — `page.locator('input[name="card"]')` works even if the input is inside a shadow root. This is a major productivity advantage for Web Component-heavy applications. Selenium requires explicit `getShadowRoot()` traversal.

---

## Q9: How do you handle iFrames and nested frames in Selenium?

### Interview Answer
To interact with elements inside an `<iframe>`, you must **switch the WebDriver context** to that frame using `driver.switchTo().frame(...)`. After completing actions inside the frame, switch back to the default context. Forgetting to switch context is the most common iframe automation bug.

### Deep Explanation
- The browser maintains a **browsing context stack** — the default content is the top-level document
- `<iframe>` elements create a **child browsing context** with its own `document` object
- Selenium's `findElement` only searches the **currently active context** — cross-context queries fail silently or throw `NoSuchElementException`
- `switchTo().frame()` moves the active context into the iframe's document
- `switchTo().defaultContent()` returns to the top-level document (skips intermediate frames)
- `switchTo().parentFrame()` moves up one level (useful for nested frames)

**Frame switching methods:**
```
driver.switchTo().frame(int index)         // by DOM position — fragile
driver.switchTo().frame(String nameOrId)   // by name/id attribute — stable
driver.switchTo().frame(WebElement)        // by element reference — most reliable
driver.switchTo().defaultContent()         // back to top document
driver.switchTo().parentFrame()            // up one frame level
```

### Java Example
```java
// SWITCH BY ELEMENT (production-grade — most reliable)
WebElement iframeEl = driver.findElement(By.cssSelector("iframe[title='Payment Frame']"));
driver.switchTo().frame(iframeEl);

// Now interact with elements INSIDE the iframe
driver.findElement(By.cssSelector("input[name='card-number']")).sendKeys("4111111111111111");

// Return to main document
driver.switchTo().defaultContent();

// NESTED FRAMES — must switch level by level
driver.switchTo().frame(driver.findElement(By.id("outer-frame")));
driver.switchTo().frame(driver.findElement(By.id("inner-frame"))); // now inside nested frame
driver.findElement(By.id("nested-element")).click();
driver.switchTo().parentFrame(); // back to outer frame
driver.switchTo().defaultContent(); // back to main document

// PRODUCTION UTILITY — safe frame switch with wait
public void switchToFrame(WebDriver driver, By frameLocator) {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
}

// PRODUCTION UTILITY — execute action in frame, auto-restore context
public <T> T withinFrame(WebDriver driver, By frameLocator, Supplier<T> action) {
    try {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
        return action.get();
    } finally {
        driver.switchTo().defaultContent();
    }
}

// Usage:
String cardValue = withinFrame(driver,
    By.cssSelector("iframe[title='Payment']"),
    () -> driver.findElement(By.id("card-display")).getText()
);
```

### Real-world Usage
- **Payment widgets** (Stripe, Braintree, Adyen): Card input fields are always in iframes for PCI compliance — iframe automation is mandatory
- **Embedded third-party forms** (Zendesk, Salesforce widgets): frames with dynamic names/IDs requiring element-based switching
- **Rich text editors** (TinyMCE, CKEditor 4): Edit area is an iframe with its own `document`
- **Legacy enterprise apps**: Some JSP/JSF apps use framesets with 3–5 nested frames

### Common Mistakes
- Calling `findElement` without switching context first — returns `NoSuchElementException` with no indication of iframe involvement
- Switching to frame by index (`0`, `1`) — breaks when page adds/removes iframes dynamically
- Forgetting `switchTo().defaultContent()` after frame interaction — subsequent `findElement` calls on main page fail
- Not waiting for the iframe to load before switching — `frameToBeAvailableAndSwitchToIt` handles this

### Optimization Tip
Always use `frameToBeAvailableAndSwitchToIt(By)` from `ExpectedConditions` — it waits for the frame to be present AND switches in one atomic operation. Wrap frame interactions in a utility method that guarantees `defaultContent()` in the `finally` block.

### Debugging Strategy
1. `NoSuchElementException` on a visible element → check if it's inside a frame: inspect DOM in DevTools, look for `<iframe>` ancestor
2. `NoSuchFrameException` → frame isn't loaded yet — add `frameToBeAvailableAndSwitchToIt` wait
3. After page navigation inside a frame, the frame context may be reset — use `defaultContent()` and re-switch
4. Log frame switches in test output — `"Switching to frame: " + frameLocator` — invaluable for debugging nested frame issues

### Interview Trap
The interviewer checks if you know that `switchTo().defaultContent()` skips intermediate frames — if you have 3 levels deep and call `defaultContent()`, you return all the way to top, not one level. `parentFrame()` is the correct call for stepping back one level. Many engineers don't know this distinction.

### Follow-up Questions
1. How would you interact with a TinyMCE rich text editor that renders its input area as an iframe?
2. What happens to your WebDriver frame context after a page refresh inside an iframe?

### Selenium vs Playwright
Playwright handles frames via `frameLocator()` — you get a scoped `FrameLocator` and use it directly without switching context. There is no "switch back" required. This is cleaner and less error-prone than Selenium's stateful `switchTo()` model.

---

## Q10: How and when do you use `JavascriptExecutor` in production automation?

### Interview Answer
`JavascriptExecutor` is used when **Selenium's WebDriver API cannot perform the action** — not as a shortcut around standard interactions. Use it for: scrolling, clicking non-interactable elements (with justification), setting values on React-controlled inputs, reading computed properties, and Shadow DOM traversal. Never use it to bypass legitimate wait issues.

### Deep Explanation
`JavascriptExecutor` executes JavaScript directly in the browser's JS engine (same thread as the page's JS). This bypasses the W3C WebDriver action pipeline entirely — meaning:
- No W3C event simulation (no `mousedown`, `mouseup`, `focus` events from JS click)
- No interactability checks (can click hidden, disabled elements — which may not reflect real user behavior)
- Direct DOM/BOM access — can manipulate anything JavaScript can

**Two execution modes:**
- `executeScript(script, args...)` — synchronous, returns immediately when JS finishes
- `executeAsyncScript(script, args...)` — for Promise-based or callback-based JS, accepts a callback as last argument

**Return type mapping:**
| JS return type | Java type |
|---|---|
| `string` | `String` |
| `number` | `Long` or `Double` |
| `boolean` | `Boolean` |
| `element` | `WebElement` |
| `array` | `List<Object>` |
| `null/undefined` | `null` |

### Java Example
```java
JavascriptExecutor js = (JavascriptExecutor) driver;

// 1. SCROLL — most common JS use
js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
js.executeScript("window.scrollTo(0, document.body.scrollHeight);"); // scroll to bottom

// 2. CLICK — only when element is non-interactable due to overlay (document this decision)
js.executeScript("arguments[0].click();", element);

// 3. SET VALUE — for React/Angular controlled inputs where sendKeys doesn't trigger events
js.executeScript("arguments[0].value = arguments[1];", inputElement, "test@example.com");
// BUT: React may not see this — fire the change event too:
js.executeScript(
    "arguments[0].value = arguments[1];" +
    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
    inputElement, "test@example.com"
);

// 4. READ PROPERTIES
String innerText = (String) js.executeScript("return arguments[0].innerText;", element);
Boolean isChecked = (Boolean) js.executeScript("return arguments[0].checked;", checkbox);
Long scrollY = (Long) js.executeScript("return window.scrollY;");

// 5. HIGHLIGHT ELEMENT (debugging utility)
public void highlight(WebDriver driver, WebElement element) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript(
        "arguments[0].style.border='3px solid red'; arguments[0].style.backgroundColor='yellow';",
        element
    );
}

// 6. ASYNC — wait for Promise
String result = (String) ((JavascriptExecutor) driver).executeAsyncScript(
    "var callback = arguments[arguments.length - 1];" +
    "fetch('/api/status').then(r => r.text()).then(callback);");

// 7. PAGE READY STATE CHECK
public void waitForPageLoad(WebDriver driver) {
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(d -> ((JavascriptExecutor) d)
            .executeScript("return document.readyState").equals("complete"));
}
```

### Real-world Usage
- **Date pickers that block keyboard input**: `js.executeScript("arguments[0].value='2026-12-31'", dateInput)`
- **React/Vue controlled inputs**: `sendKeys` doesn't trigger state updates — must dispatch `input`/`change` events
- **Element outside viewport**: `scrollIntoView` before clicking to avoid `ElementClickInterceptedException`
- **Checking page load state**: `document.readyState === 'complete'` before asserting page content
- **Extracting computed styles**: `window.getComputedStyle(el).getPropertyValue('color')`

### Common Mistakes
- Using `js.executeScript("arguments[0].click()")` as a default click — this bypasses event simulation and may not trigger Angular/React handlers
- Not handling JS exceptions — `executeScript` throws `JavascriptException` on JS errors, must be caught
- Returning complex objects from JS that can't be deserialized to Java — stick to primitives, elements, or simple arrays
- Using `executeAsyncScript` without setting `setScriptTimeout` — default is 0ms, will always timeout

### Optimization Tip
Set script timeout explicitly when using async JS:
```java
driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
```
Build a `JSUtils` class that encapsulates all JS operations — keeps Page Objects clean and provides a single place to update JS snippets when browser behavior changes.

### Debugging Strategy
1. Test JS in browser DevTools console before putting it in code
2. `JavascriptException: javascript error: Cannot read property 'X' of null` → element reference is null/stale — re-find before passing to JS
3. `js.click()` works but subsequent assertions fail → JS click didn't trigger framework event handlers — dispatch events manually
4. For async scripts: add `console.log` in the JS body and check browser console during test execution

### Interview Trap
Interviewers check if you know **when NOT to use JavascriptExecutor**: if you're using it to click a normal button because "it's more reliable," that's a red flag — it means you have an underlying wait or interactability issue you're masking. The correct answer: JS execution should be the **last resort** for standard interactions, and the **primary tool** for reading computed properties, scrolling, and framework-specific value setting.

### Follow-up Questions
1. Why does `sendKeys()` work for plain HTML inputs but sometimes fail for React-controlled inputs, and how do you fix it?
2. What is `executeAsyncScript` and how is it different from `executeScript` with a Promise return?

### Selenium vs Playwright
Playwright exposes `page.evaluate()` and `page.evaluateHandle()` for JS execution — conceptually the same as `JavascriptExecutor`. However, Playwright rarely requires JS workarounds because its action methods (`fill`, `click`, `check`) dispatch proper browser events natively, including for React/Vue/Angular inputs.

---

## Q11: How does the Selenium Actions API work and what are its production use cases?

### Interview Answer
The **Actions API** simulates low-level human input — precise mouse movements, drag-and-drop, hover, right-click, key chording, and multi-touch. It builds an **input action chain** that is dispatched as a single atomic W3C Actions command. Use it when standard `click()` / `sendKeys()` cannot replicate the required user interaction.

### Deep Explanation
The W3C Actions API defines three **input source types**:
- **Pointer** (mouse, pen, touch): `pointerMove`, `pointerDown`, `pointerUp`, `pointerCancel`
- **Keyboard**: `keyDown`, `keyUp`
- **Wheel**: `scroll` (Selenium 4.2+)

`Actions.perform()` serializes the entire chain into a single `POST /session/{id}/actions` HTTP request — all actions execute atomically in the browser, simulating real hardware input with proper event ordering (`mousemove` → `mouseenter` → `mouseover` → `mousedown` → `mouseup` → `click`).

Key difference from `element.click()`:
- `element.click()` → single W3C element click command, limited event chain
- `Actions.click(element)` → full pointer event sequence, triggers `mouseenter`, `mouseleave`, `mouseover`, `focus` etc.

### Java Example
```java
Actions actions = new Actions(driver);

// 1. HOVER — trigger CSS :hover, dropdown menus
actions.moveToElement(menuItem).perform();
// wait for dropdown to appear then click
new WebDriverWait(driver, Duration.ofSeconds(5))
    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dropdown-menu")));
driver.findElement(By.cssSelector(".dropdown-menu li:first-child")).click();

// 2. DRAG AND DROP
WebElement source = driver.findElement(By.id("drag-source"));
WebElement target = driver.findElement(By.id("drop-target"));
actions.dragAndDrop(source, target).perform();

// 3. DRAG AND DROP by OFFSET (when target is a canvas or grid)
actions.clickAndHold(source)
       .moveByOffset(200, 100)
       .release()
       .perform();

// 4. RIGHT CLICK (context menu)
actions.contextClick(element).perform();
driver.findElement(By.cssSelector(".context-menu [data-action='copy']")).click();

// 5. DOUBLE CLICK
actions.doubleClick(element).perform();

// 6. KEY CHORDING — Ctrl+A, Ctrl+C
actions.keyDown(Keys.CONTROL)
       .sendKeys("a")
       .keyUp(Keys.CONTROL)
       .perform();

// 7. MOVE TO ELEMENT WITH OFFSET (for canvas/chart interaction)
actions.moveToElement(canvas, 150, 75).click().perform();

// 8. SCROLL INTO VIEW (Selenium 4 Wheel actions)
actions.scrollToElement(element).perform();
actions.scrollByAmount(0, 500).perform(); // scroll page 500px down

// 9. COMPLEX CHAIN — hover, wait, click submenu
actions.moveToElement(navItem)
       .pause(Duration.ofMillis(500)) // let CSS transition complete
       .moveToElement(subMenuItem)
       .click()
       .perform();
```

### Real-world Usage
- **Kanban boards** (Jira, Trello): Drag cards between columns — requires `clickAndHold` → `moveToElement` → `release`
- **Date range pickers**: Click start date, hold shift, click end date — key chording required
- **Resizable panels**: Drag resize handle by offset
- **Canvas-based apps** (charts, diagram editors): Click at specific pixel coordinates
- **Rich text editors**: Select text with `clickAndHold` + `moveToElement` + `release`, then apply format

### Common Mistakes
- Calling `actions.click(element).perform()` but the element is not in the viewport — `moveToElement` first
- Using `dragAndDrop` on HTML5 drag-and-drop implementations — browser's HTML5 DnD API doesn't fire from Actions; requires custom JS event dispatch
- Not calling `.perform()` — the chain is built but never executed
- Forgetting `actions.release()` after `clickAndHold` — browser stays in drag state, corrupting subsequent interactions
- Using `new Actions(driver)` repeatedly in each method — creating fresh `Actions` objects is fine, but don't reuse a partially-built chain

### Optimization Tip
For HTML5 drag-and-drop that doesn't respond to native Actions (React DnD, SortableJS), use a JS simulation utility:
```java
public void html5DragAndDrop(WebDriver driver, WebElement source, WebElement target) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    String script =
        "function simulateDrag(src, tgt) {" +
        "  var ev = new DragEvent('dragstart', {bubbles:true});" +
        "  src.dispatchEvent(ev);" +
        "  tgt.dispatchEvent(new DragEvent('dragover', {bubbles:true}));" +
        "  tgt.dispatchEvent(new DragEvent('drop', {bubbles:true}));" +
        "  src.dispatchEvent(new DragEvent('dragend', {bubbles:true}));" +
        "} simulateDrag(arguments[0], arguments[1]);";
    js.executeScript(script, source, target);
}
```

### Debugging Strategy
1. Hover doesn't show dropdown → element may be outside viewport; add `scrollIntoView` before `moveToElement`
2. Drag-and-drop silently fails → check if it's HTML5 DnD (inspect for `draggable="true"`) vs CSS-based drag
3. `MoveTargetOutOfBoundsException` → target coordinates are outside the browser viewport — resize window or scroll
4. Key chord not working → verify modifier key release — missing `keyUp` leaves modifier active for all subsequent actions
5. Add `pause(Duration.ofMillis(300))` between actions for timing-sensitive UI transitions

### Interview Trap
The interviewer tests whether you know the **difference between HTML5 DnD API and CSS-based drag**. Native Actions work for CSS drag; HTML5 `dragstart`/`dragover`/`drop` events require JS simulation. Getting this wrong means your drag automation will randomly fail in production.

### Follow-up Questions
1. How do you automate a Sortable.js-based list reordering where native Actions DnD doesn't work?
2. How does `pause()` in an Actions chain differ from `Thread.sleep()` in terms of execution model?

### Selenium vs Playwright
Playwright's `dragAndDrop()`, `hover()`, and `keyboard.press()` dispatch full native events including HTML5 DnD events — no JS workaround needed. Playwright also supports `mouse.move()` for precise canvas interactions with sub-pixel coordinates.

---

## Q12: How do you handle multiple browser tabs and windows in Selenium?

### Interview Answer
Selenium identifies browser tabs/windows by **window handles** — opaque string identifiers for each browsing context. Use `driver.getWindowHandles()` to get all handles, `driver.switchTo().window(handle)` to change focus, and `driver.close()` to close the current tab. Each tab has its own handle; the original tab's handle is returned by `driver.getWindowHandle()` at session start.

### Deep Explanation
- Every tab/window opened during a WebDriver session gets a unique **window handle** (e.g., `CDwindow-ABC123`)
- `driver.getWindowHandles()` returns a `Set<String>` — **order is not guaranteed** (Set, not List)
- `driver.getWindowHandle()` returns the **currently focused** window handle
- Opening a new tab doesn't automatically switch focus — you must explicitly `switchTo().window(handle)`
- After closing a tab with `driver.close()`, the driver focus is **undefined** — must switch to another handle
- `driver.quit()` closes all windows and ends the session

**Selenium 4 New Tab/Window API:**
```
driver.switchTo().newWindow(WindowType.TAB)    // opens new tab and auto-switches
driver.switchTo().newWindow(WindowType.WINDOW) // opens new window and auto-switches
```

### Java Example
```java
// STORE original window handle
String originalWindow = driver.getWindowHandle();

// OPEN new tab via link (opens in new tab)
driver.findElement(By.linkText("Open Report")).click();

// WAIT for new tab to appear
new WebDriverWait(driver, Duration.ofSeconds(5))
    .until(d -> d.getWindowHandles().size() > 1);

// SWITCH to new tab
for (String handle : driver.getWindowHandles()) {
    if (!handle.equals(originalWindow)) {
        driver.switchTo().window(handle);
        break;
    }
}

// Interact with new tab
String reportTitle = driver.findElement(By.tagName("h1")).getText();

// CLOSE new tab and return to original
driver.close();
driver.switchTo().window(originalWindow);

// SELENIUM 4 — Open new tab programmatically (auto-switches)
driver.switchTo().newWindow(WindowType.TAB);
driver.get("https://app.example.com/dashboard");
// do work...
driver.close();
driver.switchTo().window(originalWindow);

// PRODUCTION UTILITY — Window Manager
public class WindowUtils {
    private final WebDriver driver;
    private final Deque<String> windowStack = new ArrayDeque<>();

    public WindowUtils(WebDriver driver) {
        this.driver = driver;
        windowStack.push(driver.getWindowHandle());
    }

    public void switchToNewWindow() {
        String current = driver.getWindowHandle();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(d -> d.getWindowHandles().size() > windowStack.size());
        driver.getWindowHandles().stream()
            .filter(h -> !windowStack.contains(h))
            .findFirst()
            .ifPresent(h -> {
                windowStack.push(h);
                driver.switchTo().window(h);
            });
    }

    public void closeAndReturn() {
        driver.close();
        windowStack.pop();
        driver.switchTo().window(windowStack.peek());
    }
}
```

### Real-world Usage
- **OAuth flows**: "Login with Google" opens a popup window — switch to it, complete auth, switch back
- **PDF/report generation**: Click "View Report" opens in new tab — switch, wait for load, extract data, close
- **Multi-step checkout with external payment**: Payment page opens in new tab in some integrations
- **Admin + user validation**: Open two tabs — admin tab and user tab — compare state side-by-side in test

### Common Mistakes
- Iterating `getWindowHandles()` assuming order — `Set` has no guaranteed order; always filter by comparing to known handles
- Forgetting to switch back after closing a tab — WebDriver focus becomes undefined, subsequent actions throw `NoSuchWindowException`
- Not waiting for new window to appear before calling `getWindowHandles()` — race condition returns stale handle set
- Using `driver.quit()` when only wanting to close one tab — `quit()` ends the entire session

### Optimization Tip
Always track the original window handle at test start (`@BeforeEach`). Build a `WindowUtils` class with a stack-based window history — `push` when opening, `pop` when closing. This prevents lost context even in deeply nested tab flows.

### Debugging Strategy
1. `NoSuchWindowException` → window was closed externally or handle reference is stale — rebuild the handle set
2. `switchTo().window(handle)` silently does nothing → handle is already the active window (not an error)
3. Tab opens but `getWindowHandles().size()` doesn't increase → new tab might have opened in the same window (same-origin navigation) — check `target="_blank"` attribute
4. OAuth popup never appears → popup blocked by browser — add `--disable-popup-blocking` to ChromeOptions

### Interview Trap
The interviewer checks if you know that **`getWindowHandles()` returns a `Set` with no ordering guarantee**. A common bug is `(Set).toArray()[1]` to get the "second window" — this is wrong. The correct approach: filter by comparing with the known original handle. Mention this explicitly.

### Follow-up Questions
1. How do you handle a scenario where clicking a button sometimes opens a new tab and sometimes navigates in the same tab (inconsistent behavior)?
2. How do you run assertions in parallel across two tabs opened in the same WebDriver session?

### Selenium vs Playwright
Playwright uses `BrowserContext` and `Page` objects — each page (tab) is an explicit object reference, no handle juggling. `page.waitForPopup()` returns the new `Page` object directly. Tab management is clean, type-safe, and doesn't rely on opaque string handles.

---

## Q13: How do you handle file upload and download in Selenium automation?

### Interview Answer
**File upload**: Use `sendKeys(absoluteFilePath)` on the `<input type="file">` element — no need for OS-level dialogs. **File download**: Configure the browser to download to a known directory without prompts, then assert the file exists/has correct content. Never use `AutoIt` or `Robot` for file dialogs in headless CI environments.

### Deep Explanation
**File Upload mechanisms:**
1. `<input type="file">` — directly send the file path via `sendKeys`. Selenium handles the rest.
2. **Custom upload buttons** (hidden input + styled button): The `<input type="file">` is hidden via CSS. Make it visible via JS before sending keys, or send keys directly to the hidden element.
3. **Drag-and-drop upload zones**: Requires simulating `dragenter`/`dragover`/`drop` events with a `DataTransfer` object via JS — not achievable with Actions API alone.

**File Download mechanisms:**
- **Chrome**: Set `download.default_directory` in ChromeOptions preferences — browser downloads silently to specified path
- **Firefox**: Set `browser.download.dir` in Firefox profile — same silent download behavior
- **Headless Chrome**: Download directory must be explicitly set AND download behavior enabled via CDP

### Java Example
```java
// ============ FILE UPLOAD ============

// STANDARD — input type="file" (works directly)
WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));
fileInput.sendKeys("C:\\TestData\\invoice.pdf");

// HIDDEN INPUT — make visible via JS then send keys
WebElement hiddenInput = driver.findElement(By.cssSelector("input[type='file'][style*='display:none']"));
((JavascriptExecutor) driver).executeScript(
    "arguments[0].style.display = 'block';", hiddenInput);
hiddenInput.sendKeys("C:\\TestData\\invoice.pdf");

// DRAG AND DROP UPLOAD ZONE — JS DataTransfer simulation
public void uploadByDragDrop(WebDriver driver, WebElement dropZone, String filePath) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript(
        "var dt = new DataTransfer();" +
        "var file = new File([''], arguments[1]);" +
        "dt.items.add(file);" +
        "arguments[0].dispatchEvent(new DragEvent('drop', {dataTransfer: dt, bubbles: true}));",
        dropZone, filePath
    );
}

// ============ FILE DOWNLOAD ============

// CHROME — configure download directory
public WebDriver buildDriverWithDownload(String downloadPath) {
    ChromeOptions options = new ChromeOptions();
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("download.default_directory", downloadPath);
    prefs.put("download.prompt_for_download", false);
    prefs.put("download.directory_upgrade", true);
    prefs.put("safebrowsing.enabled", true);
    options.setExperimentalOption("prefs", prefs);
    return new ChromeDriver(options);
}

// HEADLESS CHROME — additional CDP command needed
public void enableHeadlessDownload(WebDriver driver, String downloadPath) {
    ((ChromeDriver) driver).executeCdpCommand(
        "Page.setDownloadBehavior",
        Map.of("behavior", "allow", "downloadPath", downloadPath)
    );
}

// WAIT FOR DOWNLOAD — poll file system
public Path waitForDownload(String downloadDir, String fileNamePattern, int timeoutSec)
        throws InterruptedException {
    Path dir = Paths.get(downloadDir);
    long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);
    while (System.currentTimeMillis() < deadline) {
        try (var stream = Files.list(dir)) {
            Optional<Path> file = stream
                .filter(p -> p.getFileName().toString().matches(fileNamePattern))
                .filter(p -> !p.getFileName().toString().endsWith(".crdownload")) // Chrome partial download
                .findFirst();
            if (file.isPresent()) return file.get();
        }
        Thread.sleep(500);
    }
    throw new RuntimeException("Download not completed within " + timeoutSec + "s");
}

// ASSERT DOWNLOAD CONTENT
public void assertPdfContains(Path file, String expectedText) throws IOException {
    // integrate with Apache PDFBox or iText
    // String content = extractTextFromPdf(file);
    // assertThat(content).contains(expectedText);
}
```

### Real-world Usage
- **Invoice upload**: Upload PDF to expense management system — `sendKeys` on `input[type='file']`
- **Bulk data import**: Upload CSV to admin panel — same pattern, assert success message + row count
- **Report download**: Click "Export to Excel", wait for `.xlsx` in download folder, validate with Apache POI
- **Document verification workflow**: Upload → process → download — full file lifecycle in one test
- **Headless CI pipelines**: Must configure download path and enable CDP download behavior explicitly

### Common Mistakes
- Using `Robot` or `AutoIt` for file dialogs — breaks in headless mode and Linux CI environments entirely
- Sending file path without using `Path.of(...).toAbsolutePath().toString()` — relative paths fail depending on working directory
- Not waiting for `.crdownload` / `.part` (Firefox) temporary files to disappear — asserting on incomplete downloads
- Forgetting to create the download directory before tests — browser silently falls back to default location
- Not cleaning the download folder between tests — leftover files from previous runs cause false positives

### Optimization Tip
Use `@BeforeEach` to create a unique temp download directory per test:
```java
Path downloadDir = Files.createTempDirectory("selenium-download-");
downloadDir.toFile().deleteOnExit();
WebDriver driver = buildDriverWithDownload(downloadDir.toString());
```
This ensures download assertions are clean and parallel tests don't share download directories.

### Debugging Strategy
1. Upload fails with `InvalidArgumentException` → file path is wrong or relative; always use absolute path
2. Upload field accepts path but page shows no file name → input is inside shadow DOM or is a custom component — inspect element tree
3. Download doesn't happen → check if download was blocked by Chrome Safe Browsing — set `safebrowsing.enabled=false` in prefs
4. Download completes but `.crdownload` stays → file is partial; wait logic must check for absence of `.crdownload` extension
5. In headless mode, download never starts → `Page.setDownloadBehavior` CDP command was not called

### Interview Trap
The interviewer checks whether you know **why `Robot`/`AutoIt` is wrong for CI** (headless browsers have no OS window, Robot can't interact with non-existent dialogs). Also — do you know about `.crdownload` partial file handling? This is a common CI flakiness source.

### Follow-up Questions
1. How would you validate the content of a downloaded PDF report without opening it in a browser?
2. How do you handle file upload in a Selenium Grid setup where the Node is on a remote machine?

### Selenium vs Playwright
Playwright provides `page.setInputFiles(selector, filePath)` for uploads — cleaner API. For downloads, `page.waitForDownload()` returns a `Download` object with `path()`, `suggestedFilename()`, and `saveAs()` — no manual filesystem polling needed.

---

## Q14: How do you handle browser alerts, confirms, and prompts in Selenium?

### Interview Answer
Selenium handles native browser dialogs (`alert()`, `confirm()`, `prompt()`) via `driver.switchTo().alert()`. This returns an `Alert` object — use `accept()` to click OK, `dismiss()` to click Cancel, `getText()` to read the message, and `sendKeys()` to enter text in a prompt. Always switch to the alert before interacting with any page element when a dialog is present.

### Deep Explanation
Native browser dialogs are **modal** — they block all page interaction until dismissed. Three types:
```
alert("message")     → information only → accept() or dismiss() (same behavior)
confirm("message")   → OK/Cancel choice → accept() = OK, dismiss() = Cancel
prompt("message")    → text input       → sendKeys() then accept(), or dismiss() to cancel
```

Key behaviors:
- If a dialog is open and you call `findElement`, you get `UnhandledAlertException` (Selenium) or the command queues until the dialog is dismissed
- `switchTo().alert()` throws `NoAlertPresentException` if no dialog is open
- **`UnexpectedAlertBehaviour` capability**: Controls what happens when an alert appears unexpectedly during a command — options: `accept`, `dismiss`, `ignore`, `acceptAndNotify`, `dismissAndNotify`
- Selenium 4: `UnexpectedAlertBehaviour` is set via `ChromeOptions.setUnhandledPromptBehaviour()`

### Java Example
```java
// WAIT FOR ALERT (production — don't assume immediate appearance)
public Alert waitForAlert(WebDriver driver) {
    return new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.alertIsPresent());
}

// SIMPLE ALERT — just an info message
driver.findElement(By.id("delete-btn")).click();
Alert alert = waitForAlert(driver);
String message = alert.getText(); // "Are you sure you want to delete?"
alert.accept(); // click OK

// CONFIRM — accept or dismiss
Alert confirm = waitForAlert(driver);
assertThat(confirm.getText()).isEqualTo("Confirm deletion of 5 records?");
confirm.accept();   // click OK → proceeds with deletion
// OR
confirm.dismiss();  // click Cancel → cancels deletion

// PROMPT — enter text
Alert prompt = waitForAlert(driver);
prompt.sendKeys("John Doe"); // type into prompt input
prompt.accept();             // submit

// UNHANDLED ALERT BEHAVIOUR — configure globally
ChromeOptions options = new ChromeOptions();
options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.DISMISS_AND_NOTIFY);
// DISMISS_AND_NOTIFY: auto-dismisses unexpected alerts and throws exception
// ACCEPT:            auto-accepts (dangerous — may delete data)
// IGNORE:            lets alerts pile up (causes test hangs)

// SAFE ALERT HANDLER — utility for tests that trigger unexpected alerts
public Optional<String> dismissAlertIfPresent(WebDriver driver) {
    try {
        Alert alert = driver.switchTo().alert();
        String text = alert.getText();
        alert.dismiss();
        return Optional.of(text);
    } catch (NoAlertPresentException e) {
        return Optional.empty();
    }
}
```

### Real-world Usage
- **Delete confirmation dialogs**: Click delete → `confirm()` appears → `accept()` → assert record removed
- **Unsaved changes warnings**: Navigate away → `confirm("Leave page? Changes will be lost")` → `dismiss()` to stay
- **Session timeout alerts**: After inactivity, `alert("Your session has expired")` — handle in `@AfterEach` to prevent cascade failures
- **Form validation alerts** (legacy apps): Some older apps use `alert()` for validation messages instead of inline errors

### Common Mistakes
- Calling `findElement` after an alert appears — throws `UnhandledAlertException`; always handle alert first
- Using `waitForAlert` but the alert is triggered asynchronously — add explicit wait, not `Thread.sleep`
- Not switching back from alert context — after `accept()`/`dismiss()` the driver returns to page context automatically (unlike frames/windows)
- Confusing native browser alerts with custom JS modal dialogs (Bootstrap modals, SweetAlert) — these are regular DOM elements, not `Alert` objects
- Setting `ACCEPT` as unhandled behavior globally — can auto-accept destructive dialogs, causing data loss in tests

### Optimization Tip
Wrap alert handling in a `@AfterEach` safety net to prevent alert from cascading to the next test:
```java
@AfterEach
void dismissUnexpectedAlerts() {
    try {
        driver.switchTo().alert().dismiss();
    } catch (NoAlertPresentException ignored) {}
}
```

### Debugging Strategy
1. `UnhandledAlertException` mid-test → a dialog appeared unexpectedly; add `dismissAlertIfPresent()` guard
2. `NoAlertPresentException` when expecting one → alert was dismissed by unhandled prompt behavior setting, or it's a custom DOM modal
3. `alert.getText()` returns empty → rare; try adding a short wait before reading text
4. Alert doesn't appear in headless mode → some legacy apps detect headless and suppress dialogs; run with `--headless=new` and add `--disable-blink-features=AutomationControlled`

### Interview Trap
Interviewers test if you know the difference between **native browser alerts** (`window.alert()`) and **custom modal dialogs** (Bootstrap, SweetAlert, Material Dialog). Native alerts = `switchTo().alert()`. Custom modals = regular `findElement` with locators. Getting this wrong (using `switchTo().alert()` on a Bootstrap modal) is a common junior mistake they're filtering out.

### Follow-up Questions
1. How do you handle an alert that appears during page load before your `findElement` call?
2. What is `UnexpectedAlertBehaviour` and what are the risks of setting it globally to `ACCEPT`?

### Selenium vs Playwright
Playwright handles dialogs via `page.on('dialog', dialog -> dialog.accept())` — an event listener model. This handles alerts that appear asynchronously without polling. Playwright also distinguishes `alert`, `confirm`, `prompt`, and `beforeunload` dialog types in its `Dialog` object.

---

## Q15: How do you manage cookies and browser sessions in Selenium?

### Interview Answer
Selenium's `driver.manage().getCookies()` / `addCookie()` / `deleteCookie()` API gives full control over browser cookies. The primary production use is **login state injection** — add an authenticated session cookie directly instead of going through the login UI, dramatically reducing test suite execution time.

### Deep Explanation
**Cookie lifecycle in Selenium:**
- Cookies are scoped to the **current domain** — you must navigate to the domain before adding/reading cookies
- `addCookie(Cookie)` injects a cookie into the browser session for the current domain
- `getCookies()` returns all cookies for the current domain
- `deleteAllCookies()` clears the session — equivalent to logging out
- `deleteSessionCookies()` via `manage().deleteAllCookies()` is what `driver.quit()` does implicitly

**Session cookie injection strategy:**
1. One-time login via UI → capture session cookie
2. Store cookie (serialize to file or test fixture)
3. All subsequent tests: navigate to domain, inject cookie, bypass login UI

**Cookie object properties:**
```
name, value, domain, path, expiry, isSecure, isHttpOnly, sameSite
```

### Java Example
```java
// ============ COOKIE INJECTION (production login bypass) ============

// STEP 1: Login via UI once, capture session cookie
driver.get("https://app.example.com/login");
driver.findElement(By.id("email")).sendKeys("test@example.com");
driver.findElement(By.id("password")).sendKeys("password");
driver.findElement(By.id("login-btn")).click();
new WebDriverWait(driver, Duration.ofSeconds(10))
    .until(ExpectedConditions.urlContains("/dashboard"));

Cookie sessionCookie = driver.manage().getCookieNamed("session_token");

// STEP 2: Serialize for reuse
String cookieValue = sessionCookie.getValue();

// STEP 3: In each test — inject cookie (skip login UI)
public void injectSessionCookie(WebDriver driver, String domain, String cookieValue) {
    driver.get("https://" + domain); // must navigate to domain first
    driver.manage().addCookie(new Cookie.Builder("session_token", cookieValue)
        .domain(domain)
        .path("/")
        .isSecure(true)
        .isHttpOnly(true)
        .build());
    driver.navigate().refresh(); // apply cookie
}

// ============ FULL COOKIE MANAGEMENT ============

// READ all cookies
Set<Cookie> allCookies = driver.manage().getCookies();
allCookies.forEach(c -> System.out.println(c.getName() + "=" + c.getValue()));

// READ specific cookie
Cookie authCookie = driver.manage().getCookieNamed("auth_token");

// DELETE specific cookie
driver.manage().deleteCookieNamed("tracking_cookie");

// DELETE all (logout simulation)
driver.manage().deleteAllCookies();
driver.navigate().refresh();

// SERIALIZE cookies to file (for cross-test reuse)
public void saveCookies(WebDriver driver, Path file) throws IOException {
    Set<Cookie> cookies = driver.manage().getCookies();
    List<Map<String, String>> serializable = cookies.stream()
        .map(c -> Map.of("name", c.getName(), "value", c.getValue(),
                         "domain", c.getDomain(), "path", c.getPath()))
        .collect(Collectors.toList());
    new ObjectMapper().writeValue(file.toFile(), serializable);
}

// LOAD cookies from file
public void loadCookies(WebDriver driver, Path file) throws IOException {
    List<Map<String, String>> cookies = new ObjectMapper()
        .readValue(file.toFile(), new TypeReference<>(){});
    cookies.forEach(c -> driver.manage().addCookie(
        new Cookie(c.get("name"), c.get("value"), c.get("domain"), c.get("path"), null)));
}

// ============ LOCAL STORAGE & SESSION STORAGE ============
JavascriptExecutor js = (JavascriptExecutor) driver;

// Set localStorage item (JWT tokens, feature flags)
js.executeScript("window.localStorage.setItem(arguments[0], arguments[1]);", "jwt_token", "eyJ...");

// Read localStorage
String token = (String) js.executeScript("return window.localStorage.getItem(arguments[0]);", "jwt_token");

// Clear localStorage (logout for SPA)
js.executeScript("window.localStorage.clear();");
```

### Real-world Usage
- **Login bypass**: Auth cookie injection reduces login step from 3–5 seconds to 50ms — in a 500-test suite, this saves 20–40 minutes
- **Multi-role testing**: Inject different role-specific session cookies to test admin vs user vs guest behavior
- **Cookie consent**: Inject cookie consent cookie to skip GDPR banners in every test
- **JWT in localStorage**: SPAs store auth tokens in localStorage — inject via JS rather than through login UI
- **Session expiry testing**: Delete session cookie and assert redirect to login page

### Common Mistakes
- Adding a cookie before navigating to the domain — Selenium silently fails or throws `InvalidCookieDomainException`
- Not calling `navigate().refresh()` after adding a cookie — the page doesn't reload to use the new cookie
- Sharing cookies across parallel test threads — each `WebDriver` instance has its own cookie store; no sharing needed (and attempting it causes corruption)
- Storing HttpOnly cookies from one environment and using them in another — domain mismatch causes immediate rejection
- Not handling cookie expiry — captured cookies expire; tests randomly fail until someone notices

### Optimization Tip
Implement a **Session Cache** pattern in your framework:
```java
public class SessionCache {
    private static final Map<String, String> cookieCache = new ConcurrentHashMap<>();

    public static String getOrCreate(String role, Supplier<String> loginAction) {
        return cookieCache.computeIfAbsent(role, r -> loginAction.get());
    }
}
```
Login once per role per test suite run. All tests for that role share the cached cookie value. Invalidate on `@AfterAll`.

### Debugging Strategy
1. `InvalidCookieDomainException` → you haven't navigated to the domain before adding the cookie
2. Cookie added but auth doesn't work → check `isSecure` flag — HTTP page won't use secure cookies
3. Cookie visible in `getCookies()` but not in browser DevTools → domain/path mismatch — verify with `driver.manage().getCookieNamed(name).getDomain()`
4. Login bypass works locally but fails in CI → CI uses different domain (e.g., `localhost` vs `staging.example.com`) — domain must match exactly

### Interview Trap
The interviewer checks if you know the **domain-first rule** (must navigate before adding cookies) AND the `isHttpOnly` implication — HttpOnly cookies can't be read by JS (`localStorage` trick won't work), but Selenium's WebDriver API can still read them because it talks to the driver directly, not JS. Knowing this distinction is a senior-level differentiator.

### Follow-up Questions
1. How do you handle authentication for an app that uses JWT stored in `localStorage` instead of a cookie?
2. If you're running 200 parallel tests that all need an authenticated session, how do you avoid 200 login UI flows?

### Selenium vs Playwright
Playwright uses `BrowserContext.storageState()` — captures cookies, localStorage, and sessionStorage as a single JSON snapshot. Tests restore the full auth state in one call: `browser.newContext(storageState: 'auth.json')`. This is more comprehensive than Selenium's cookie-only approach since it covers localStorage-based JWT auth natively.

---

## Q16: How do you parse and interact with dynamic web tables in Selenium?

### Interview Answer
Web table automation requires treating rows and cells as a **searchable data structure**, not positional indices. Use XPath row-filtering (`//tr[td[text()='value']]`) to find rows by cell content, then extract specific columns by header-index mapping. Never hardcode row/column indices — they change with sorting, pagination, and data updates.

### Deep Explanation
**Table DOM structure:**
```html
<table>
  <thead>
    <tr><th>Name</th><th>Status</th><th>Amount</th></tr>
  </thead>
  <tbody>
    <tr><td>Alice</td><td>Active</td><td>$500</td></tr>
    <tr><td>Bob</td><td>Inactive</td><td>$200</td></tr>
  </tbody>
</table>
```

**Key strategies:**
1. **Header-to-index mapping**: Build a `Map<String, Integer>` of column name → index at runtime — immune to column reordering
2. **Row-by-content XPath**: `//tbody/tr[td[normalize-space()='Alice']]` — find row by known cell value
3. **Pagination handling**: Loop through pages, extracting rows until the target is found or all pages exhausted
4. **Dynamic loading (virtual scroll/lazy load)**: Scroll table container to trigger row rendering before reading

### Java Example
```java
// PRODUCTION TABLE PARSER UTILITY
public class TableParser {
    private final WebDriver driver;
    private final By tableLocator;

    public TableParser(WebDriver driver, By tableLocator) {
        this.driver = driver;
        this.tableLocator = tableLocator;
    }

    // Build header → column index map
    public Map<String, Integer> getHeaderMap() {
        List<WebElement> headers = driver.findElement(tableLocator)
            .findElements(By.cssSelector("thead th"));
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i).getText().trim(), i);
        }
        return map;
    }

    // Get all rows as List of Maps (column name → cell value)
    public List<Map<String, String>> getAllRows() {
        Map<String, Integer> headers = getHeaderMap();
        List<WebElement> rows = driver.findElement(tableLocator)
            .findElements(By.cssSelector("tbody tr"));
        return rows.stream().map(row -> {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            Map<String, String> rowData = new LinkedHashMap<>();
            headers.forEach((header, idx) -> {
                if (idx < cells.size()) {
                    rowData.put(header, cells.get(idx).getText().trim());
                }
            });
            return rowData;
        }).collect(Collectors.toList());
    }

    // Find row containing specific cell value
    public Optional<Map<String, String>> findRowByColumnValue(String column, String value) {
        return getAllRows().stream()
            .filter(row -> value.equals(row.get(column)))
            .findFirst();
    }

    // Get cell value by row identifier and target column
    public String getCellValue(String identifierColumn, String identifierValue, String targetColumn) {
        return findRowByColumnValue(identifierColumn, identifierValue)
            .map(row -> row.get(targetColumn))
            .orElseThrow(() -> new NoSuchElementException(
                "Row not found where " + identifierColumn + "=" + identifierValue));
    }

    // Click action button in a specific row
    public void clickActionInRow(String identifierColumn, String identifierValue, String buttonText) {
        String rowXpath = String.format(
            "//tbody/tr[td[normalize-space()='%s']]//button[normalize-space()='%s']",
            identifierValue, buttonText);
        driver.findElement(By.xpath(rowXpath)).click();
    }

    // Paginated table — read all pages
    public List<Map<String, String>> getAllRowsAcrossPages(By nextButtonLocator) {
        List<Map<String, String>> allRows = new ArrayList<>();
        do {
            allRows.addAll(getAllRows());
            List<WebElement> nextBtn = driver.findElements(nextButtonLocator);
            if (nextBtn.isEmpty() || !nextBtn.get(0).isEnabled()) break;
            nextBtn.get(0).click();
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.stalenessOf(
                    driver.findElement(tableLocator).findElements(By.cssSelector("tbody tr")).get(0)));
        } while (true);
        return allRows;
    }
}

// USAGE IN TEST
TableParser table = new TableParser(driver, By.id("transactions-table"));
String status = table.getCellValue("Transaction ID", "TXN-1042", "Status");
assertThat(status).isEqualTo("Completed");

table.clickActionInRow("Invoice #", "INV-2024", "Download");
```

### Real-world Usage
- **FinTech dashboards**: Verify transaction rows by ID, check amount/status columns — header map handles column reordering
- **Admin user tables**: Find user by email, verify role assignment, click "Edit" in that row
- **Pagination + assertion**: Verify a record exists across 10 pages of search results
- **Sort verification**: Read all rows before and after sort, compare against expected order

### Common Mistakes
- Hardcoding `cells.get(2)` for "Status" column — breaks the moment columns are reordered
- Not handling empty rows (`<tr class="no-data">`) — throws `IndexOutOfBoundsException` when mapping headers to empty cells
- Not waiting for table rows to load after pagination click — reading stale rows from previous page
- Using `table.getText()` on the entire table element — returns everything concatenated, unusable

### Optimization Tip
For performance-critical table assertions, use XPath directly instead of loading all rows into Java:
```java
// 5x faster than getAllRows() for single-cell lookup
String amount = driver.findElement(By.xpath(
    "//tbody/tr[td[normalize-space()='TXN-1042']]/td[" +
    (getHeaderMap().get("Amount") + 1) + "]")).getText();
```

### Debugging Strategy
1. `NoSuchElementException` on row — table might be loading dynamically; wait for `tbody tr` count > 0 before parsing
2. Header map returns wrong indices — table has `colspan` headers; handle merged headers separately
3. Cell text is empty but visible in browser — text might be in a child `<span>` or set via CSS `content:` — use `innerText` via JS
4. Pagination doesn't advance — "Next" button is disabled (last page); always check `isEnabled()` before clicking

### Interview Trap
Interviewers assign a live table exercise: "Find the row where Status is 'Failed' and click Retry." They're checking if you hardcode indices or build a flexible query. The correct answer uses **header map + XPath row filter** — not `tr[3]/td[4]`.

### Follow-up Questions
1. How do you handle a virtual/infinite-scroll table where rows are rendered only when visible in the viewport?
2. How do you verify that a table is sorted correctly after clicking a column header?

### Selenium vs Playwright
Playwright can use `page.locator('table').getByRole('row').filter({hasText: 'TXN-1042'})` — its role-based locator engine understands table semantics. Combined with `getByRole('cell')`, it provides readable, accessible table navigation without custom utilities.

---

## Q17: What are Relative Locators in Selenium 4 and when should you use them?

### Interview Answer
**Relative Locators** (formerly "Friendly Locators") in Selenium 4 locate elements based on their **spatial relationship to another element**: `above`, `below`, `toLeftOf`, `toRightOf`, `near`. They use visual position on the rendered page, not DOM structure. Use them when elements lack unique identifiers and live in proximity to a visually identifiable neighbor.

### Deep Explanation
Relative locators work by:
1. Finding the **anchor element** (well-identified neighbor)
2. Calling `driver.findElement(RelativeLocator.with(By.tagName("X")).above(anchor))`
3. Selenium queries **all elements of that tag**, then filters by position using the bounding box (`getBoundingClientRect`)
4. Position comparison uses visual pixel coordinates — `above` means the element's bottom edge is above the anchor's top edge

**Available relations:**
```
.above(WebElement or By)     — element is visually above the anchor
.below(WebElement or By)     — element is visually below
.toLeftOf(WebElement or By)  — element is to the left of the anchor
.toRightOf(WebElement or By) — element is to the right
.near(WebElement or By)      — element is within ~50px of anchor (default distance)
```

**Chaining**: Relations can be chained — `with(By.tag("input")).above(zipField).toRightOf(streetLabel)`

### Java Example
```java
import static org.openqa.selenium.support.locators.RelativeLocator.with;

// SCENARIO: Form without IDs — find input fields by their labels
// HTML: <label>Email</label> <input type="text">

WebElement emailLabel = driver.findElement(By.xpath("//label[text()='Email']"));

// Find the input that is to the RIGHT of the Email label
WebElement emailInput = driver.findElement(
    with(By.tagName("input")).toRightOf(emailLabel));
emailInput.sendKeys("test@example.com");

// Find checkbox ABOVE a "Submit" button
WebElement submitBtn = driver.findElement(By.id("submit"));
WebElement termsCheckbox = driver.findElement(
    with(By.tagName("input")).above(submitBtn));
termsCheckbox.click();

// CHAINED: input that is below "First Name" label AND to the right of row marker
WebElement firstNameInput = driver.findElement(
    with(By.tagName("input"))
        .below(By.xpath("//label[text()='First Name']"))
        .toRightOf(By.cssSelector(".form-row-marker")));

// NEAR: find any button within ~50px of an error banner
WebElement dismissBtn = driver.findElement(
    with(By.tagName("button")).near(By.cssSelector(".error-banner")));
dismissBtn.click();

// PRACTICAL: Handle a form with repeated structure (address form)
WebElement billingSection = driver.findElement(By.id("billing-address"));
WebElement shippingZip = driver.findElement(
    with(By.cssSelector("input[name='zip']")).below(billingSection));
```

### Real-world Usage
- **Legacy forms without IDs**: Labels are stable text; inputs have no unique attributes — relative locators bridge the gap
- **Grid/table-like layouts**: Find "Edit" button in the same row as a label without complex XPath
- **Dynamic forms**: Field order changes based on user type — `above`/`below` relationships are more stable than DOM order
- **Accessibility-driven apps**: Labels and inputs are paired visually; relative locators mirror how a user navigates

### Common Mistakes
- Using relative locators on **deeply nested or absolutely positioned elements** — their visual bounding boxes may not reflect layout intent
- Assuming `above()` finds the nearest element above — it returns the **first match** in the tag list that satisfies the position constraint, not necessarily the closest
- Using on **responsive layouts** — at different viewport sizes, elements reflow and `toLeftOf`/`toRightOf` break
- Not providing a tag filter — `with(By.tagName("input"))` is necessary; without a tag constraint the search set is all DOM elements, causing unpredictable results

### Optimization Tip
Use relative locators as a **last resort** after standard locators fail. They're slower (require bounding box calculations), less precise than explicit locators, and brittle on responsive layouts. Prefer agreeing `data-testid` with developers. When you do use relative locators, always anchor to a highly stable, visible element.

```java
// PREFER THIS when available:
By.cssSelector("[data-testid='email-input']")

// USE RELATIVE LOCATOR when this is the only option:
with(By.tagName("input")).toRightOf(By.xpath("//label[text()='Email']"))
```

### Debugging Strategy
1. Returns wrong element → viewport size matters; run at a fixed window size: `driver.manage().window().setSize(new Dimension(1920, 1080))`
2. Returns `NoSuchElementException` → no element of that tag satisfies the spatial constraint; verify layout in browser DevTools
3. Ambiguous results → chain multiple relations to narrow down: `.above(...).toRightOf(...)`
4. Breaks in headless mode → headless Chrome renders at a smaller default viewport; always set explicit window size for relative locator tests

### Interview Trap
Most candidates say "relative locators find elements by position." The deeper answer: they use **`getBoundingClientRect`** to get pixel positions, then filter elements of the given tag by spatial constraint. The interviewer wants to know you understand the **performance implication** (page-wide DOM scan + bounding box query per element) and that you wouldn't use them as a first choice.

### Follow-up Questions
1. How does `near()` determine "nearness" — what is the default threshold and can it be customized?
2. Why do relative locators fail on elements that are positioned with CSS `position: fixed` or `position: absolute`?

### Selenium vs Playwright
Playwright doesn't have relative locators — it uses `getByRole`, `getByLabel`, `getByText`, and CSS/XPath. Playwright's `getByLabel` natively finds the input associated with a label via ARIA `for` attribute or DOM proximity, which is more semantically correct than visual position.

---

## Q18: How do you handle dynamic elements, dynamic IDs, and dynamic locators in production?

### Interview Answer
Dynamic elements have attributes that change on every page load (e.g., `id="input_12849"`, `class="btn_a3f9"`). Handle them by using **stable, non-generated attributes** — `data-testid`, `name`, `type`, `aria-label`, relative XPath, or partial attribute matching. The root fix is a **test ID contract with developers**.

### Deep Explanation
**Sources of dynamic attributes:**
1. **Framework-generated IDs**: React/Angular generate IDs like `mat-input-23`, `rc-uuid-abc123` — change per render
2. **CSS Modules/CSS-in-JS**: Class names get hashed (`btn_a3k9x`) — change on every build
3. **Server-rendered random IDs**: JSF, Spring MVC sometimes generates `j_idt42`, `form:j_idt18`
4. **Dynamic content**: Table rows, list items where position-based locators shift when data changes

**Locator stability hierarchy (most stable → least):**
```
1. data-testid / data-qa / data-cy (agreed with dev team)
2. name attribute (forms)
3. aria-label / aria-labelledby
4. type + contextual parent (input[type='email'] inside form#login)
5. Visible text (XPath normalize-space)
6. Partial attribute match (XPath contains / CSS *)
7. DOM position / index   ← NEVER use in production
```

### Java Example
```java
// PROBLEM: Dynamic ID — breaks every render
By bad = By.id("j_idt42:j_idt87"); // JSF generated — don't use

// SOLUTION 1: Stable attribute
By stable = By.cssSelector("[data-testid='submit-payment-btn']");

// SOLUTION 2: Partial attribute match (CSS)
// id starts with "email-" — stable prefix even if suffix changes
By partialId = By.cssSelector("[id^='email-']");       // starts with
By partialClass = By.cssSelector("[class*='submit-btn']"); // contains
By partialHref = By.cssSelector("[href$='.pdf']");     // ends with

// SOLUTION 3: XPath with partially stable values
By xpathContains = By.xpath("//*[contains(@id, 'email')]");
By xpathNot = By.xpath("//input[not(contains(@class,'disabled'))]");

// SOLUTION 4: Contextual locator — parent anchor + relative child
By contextual = By.cssSelector("#payment-form input[type='text']:first-of-type");

// SOLUTION 5: Aria attributes (accessibility-driven, very stable)
By aria = By.cssSelector("[aria-label='Card number']");
By ariaRole = By.cssSelector("[role='dialog'] button[aria-label='Close']");

// SOLUTION 6: Self-healing locator utility — try multiple strategies
public class SelfHealingLocator {
    private final List<By> strategies;
    private int successfulStrategyIndex = 0;

    public SelfHealingLocator(By... strategies) {
        this.strategies = List.of(strategies);
    }

    public WebElement find(WebDriver driver) {
        // Try last successful strategy first
        for (int i = successfulStrategyIndex; i < strategies.size(); i++) {
            try {
                WebElement el = driver.findElement(strategies.get(i));
                successfulStrategyIndex = i; // remember which worked
                return el;
            } catch (NoSuchElementException ignored) {}
        }
        // Fall back to all strategies
        for (int i = 0; i < successfulStrategyIndex; i++) {
            try {
                WebElement el = driver.findElement(strategies.get(i));
                successfulStrategyIndex = i;
                return el;
            } catch (NoSuchElementException ignored) {}
        }
        throw new NoSuchElementException("All locator strategies failed for: " + strategies);
    }
}

// USAGE
SelfHealingLocator emailField = new SelfHealingLocator(
    By.cssSelector("[data-testid='email-input']"),    // primary
    By.cssSelector("[aria-label='Email address']"),   // fallback 1
    By.xpath("//input[@type='email']"),               // fallback 2
    By.name("email")                                  // fallback 3
);
emailField.find(driver).sendKeys("test@example.com");
```

### Real-world Usage
- **JSF enterprise apps**: No IDs or dynamic IDs — use `name` attribute or XPath text matching
- **React Material UI**: `id="mui-component-select-123"` → use `[aria-labelledby]` or `[role='combobox']`
- **Angular reactive forms**: `formControlName` attribute is stable — `[formcontrolname='email']`
- **Micro-frontend apps**: Components have namespaced `data-testid` like `[data-testid='checkout::card-number']`
- **A/B test variants**: Elements exist in two variants — self-healing locator tries both selectors

### Common Mistakes
- Using `contains(@id, 'email')` without a uniqueness check — multiple elements may contain 'email' in their ID
- Relying on element text that's subject to i18n (internationalization) — text changes with locale
- Using CSS `:nth-child(3)` for dynamic lists — position shifts when items are added/removed
- Treating `data-reactid` / `__reactFiber` attributes as stable — these are internal React identifiers, change between renders

### Optimization Tip
Establish a **locator governance policy** in your test framework:
1. Linting rule: PR fails if locator uses `contains(@id, ...)` without a `data-testid` fallback
2. Developer checklist: Every user-facing interactive element must have `data-testid` before the PR merges
3. Test health dashboard: Track % of tests using `data-testid` vs fragile locators — treat as a quality metric

### Debugging Strategy
1. Frequent `NoSuchElementException` on deploy days → ID/class changed — switch to `data-testid` or attribute-based
2. Test passes locally, fails in CI → CI may run different locale, build hash, or AB variant — add fallback locators
3. Element found but wrong one → `contains(@class)` matched multiple — add parent scope or more constraints
4. Intermittent failures on same element → element alternates between two states with different attributes — self-healing locator covers both

### Interview Trap
The interviewer is testing **root-cause thinking** — the real answer to "how do you handle dynamic IDs" isn't "use `contains(@id)`." It's: **don't tolerate dynamic IDs in testable code.** The senior answer is: raise it with the dev team, agree on `data-testid` attributes, and use `contains()` only as a temporary bridge. Mention **locator governance as a process, not just a coding trick**.

### Follow-up Questions
1. How would you implement a self-healing locator that logs which fallback strategy was used for monitoring?
2. What is the difference between `data-testid`, `data-qa`, and `data-cy` — are they interchangeable?

### Selenium vs Playwright
Playwright's `getByTestId('submit-btn')` reads from a configurable `testIdAttribute` (default: `data-testid`). Its `getByRole`, `getByLabel`, `getByText` engine uses ARIA semantics, making locators resilient to DOM changes. Playwright was designed with dynamic frameworks (React, Vue, Angular) as first-class targets.

---

## Q19: How do you configure and run Selenium tests in headless mode and what are its limitations?

### Interview Answer
Headless mode runs the browser **without a GUI** — the rendering engine runs but no window is displayed. Use `--headless=new` (Chrome 112+) or `--headless` (Firefox). It's the standard for CI/CD pipelines. Limitations: headless can exhibit different rendering behavior, font metrics differ, some browser features behave differently, and interactive debugging is impossible without screenshots.

### Deep Explanation
**How headless works:**
- Chrome headless uses the same Blink rendering engine — no compositor/GPU pipeline differences in `--headless=new`
- Old headless (`--headless` pre-Chrome 112) was a separate code path with known rendering differences — `--headless=new` unified this
- Firefox headless (`-headless`) uses the same Gecko engine rendering cycle

**`--headless=new` vs old `--headless`:**
| Feature | Old `--headless` | `--headless=new` (Chrome 112+) |
|---|---|---|
| Rendering engine | Separate code path | Same as headed |
| GPU compositing | Disabled | Enabled (software) |
| Extensions | Not supported | Limited support |
| DevTools Protocol | Partial | Full |
| Print-to-PDF | Not reliable | Reliable |

### Java Example
```java
// CHROME HEADLESS — production configuration
public WebDriver buildHeadlessChrome(String downloadPath) {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");          // new headless mode (Chrome 112+)
    options.addArguments("--window-size=1920,1080"); // CRITICAL — set explicit viewport
    options.addArguments("--no-sandbox");            // required in Docker/CI
    options.addArguments("--disable-dev-shm-usage"); // prevent /dev/shm OOM in Docker
    options.addArguments("--disable-gpu");           // Docker containers, no GPU
    options.addArguments("--disable-extensions");
    options.addArguments("--disable-infobars");
    options.addArguments("--remote-debugging-port=9222"); // enable CDP access

    // Disable automation detection
    options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
    options.setExperimentalOption("useAutomationExtension", false);

    // Download config for headless
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("download.default_directory", downloadPath);
    prefs.put("download.prompt_for_download", false);
    options.setExperimentalOption("prefs", prefs);

    ChromeDriver driver = new ChromeDriver(options);

    // Enable downloads in headless (CDP required)
    driver.executeCdpCommand("Page.setDownloadBehavior",
        Map.of("behavior", "allow", "downloadPath", downloadPath));

    return driver;
}

// FIREFOX HEADLESS
public WebDriver buildHeadlessFirefox() {
    FirefoxOptions options = new FirefoxOptions();
    options.addArguments("-headless");
    options.addArguments("--width=1920");
    options.addArguments("--height=1080");

    FirefoxProfile profile = new FirefoxProfile();
    profile.setPreference("browser.download.folderList", 2);
    profile.setPreference("browser.download.dir", "/tmp/downloads");
    profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/pdf");
    options.setProfile(profile);

    return new FirefoxDriver(options);
}

// SCREENSHOT ON FAILURE (critical in headless — your only visual debug tool)
@AfterEach
void screenshotOnFailure(TestInfo testInfo) {
    if (testFailed) { // track with TestWatcher extension
        TakesScreenshot ts = (TakesScreenshot) driver;
        File screenshot = ts.getScreenshotAs(OutputType.FILE);
        Path dest = Paths.get("target/screenshots",
            testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_") + ".png");
        Files.copy(screenshot.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
    }
}
```

### Real-world Usage
- **GitHub Actions / Jenkins pipelines**: No display server — headless is mandatory
- **Docker containers**: Linux containers have no GUI — `--no-sandbox` + `--disable-dev-shm-usage` are required flags
- **Performance**: Headless is 10–15% faster than headed mode — no GPU rendering overhead
- **Parallel execution at scale**: 50+ concurrent headless browsers on a single machine vs 10 headed

### Common Mistakes
- Not setting `--window-size` in headless — default viewport is 800×600, breaking responsive layouts and causing `ElementClickInterceptedException`
- Missing `--no-sandbox` in Docker — Chrome crashes silently without this flag inside containers
- Missing `--disable-dev-shm-usage` — Docker's `/dev/shm` is 64MB by default; Chrome needs more, causing OOM crashes
- Not setting up CDP download behavior in headless — downloads silently fail without `Page.setDownloadBehavior`
- Using old `--headless` flag with Chrome 112+ — behavior has subtle differences from `--headless=new`

### Optimization Tip
For debugging headless failures, implement **automatic screenshot + page source capture on test failure** stored to CI artifacts:
```java
public void captureFailureArtifacts(WebDriver driver, String testName) {
    // Screenshot
    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    Files.write(Paths.get("target/screenshots/" + testName + ".png"), screenshot);

    // Page source
    Files.writeString(Paths.get("target/pagesource/" + testName + ".html"),
        driver.getPageSource());

    // Browser console logs
    LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
    String logContent = logs.getAll().stream()
        .map(e -> e.getLevel() + ": " + e.getMessage())
        .collect(Collectors.joining("\n"));
    Files.writeString(Paths.get("target/consolelogs/" + testName + ".log"), logContent);
}
```

### Debugging Strategy
1. Test passes in headed, fails in headless → likely a viewport issue — set `--window-size=1920,1080`
2. `ElementClickInterceptedException` in headless → element offscreen (viewport too small) or overlay — check screenshot
3. Download not happening → missing `Page.setDownloadBehavior` CDP command in headless
4. Chrome crashes instantly in Docker → missing `--no-sandbox` or `--disable-dev-shm-usage`
5. Anti-bot detection blocking headless → add `--disable-blink-features=AutomationControlled` and remove `enable-automation` switch

### Interview Trap
"Why do some tests pass headed but fail headless?" — The answer is not just "rendering differences." Specifically: **viewport size** (default 800x600 headless vs maximized headed), **font metrics** (headless uses different font rendering affecting element dimensions), **timing** (headless can be faster, exposing race conditions), and **GPU features** (WebGL, canvas fingerprinting may behave differently).

### Follow-up Questions
1. How do you debug a test failure that only occurs in headless mode on CI?
2. What is `--disable-blink-features=AutomationControlled` and why would you need it?

### Selenium vs Playwright
Playwright runs headless by default — you opt INTO headed mode with `headless: false`. Its headless implementation is more reliable and consistent because it was designed headless-first. Playwright also has a `--trace` recording feature that captures screenshots, DOM snapshots, and network logs for every step — replacing the need for custom failure artifact capture.

---

## Q20: How do you handle CAPTCHA in automation testing?

### Interview Answer
**You don't automate real CAPTCHA in production tests.** The correct approach is to **eliminate CAPTCHA from test environments** via environment-specific bypasses: a test environment feature flag, a backdoor API endpoint, or a whitelisted IP. Automated CAPTCHA solving services exist but are inappropriate for internal test environments and violate terms of service.

### Deep Explanation
**Why CAPTCHA exists:** Differentiate humans from bots. Selenium IS a bot — CAPTCHA will correctly identify and block it.

**CAPTCHA types and testability:**
| Type | Automation approach |
|---|---|
| reCAPTCHA v2 (image challenge) | Cannot solve reliably — use bypass |
| reCAPTCHA v3 (score-based) | Can spoof score via API token — test environment bypass |
| hCaptcha | Same as reCAPTCHA v2 — bypass only |
| Text-based CAPTCHA | Solvable via OCR — but this is a security issue, not a testing strategy |
| Math CAPTCHA | Parseable — but indicates bad test environment design |

**Correct engineering approaches:**

1. **Test environment feature flag**: Dev team adds `CAPTCHA_DISABLED=true` env var — CAPTCHA renders but automatically passes
2. **Backdoor token**: Test sends a known token (e.g., `x-test-bypass: true` header) that server-side skips CAPTCHA validation
3. **Google reCAPTCHA test keys**: Google provides dedicated test site keys that always pass:
   - Test site key: `6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI`
   - Always returns a valid token in test environments
4. **Mock/stub at API level**: Use WireMock to intercept CAPTCHA validation API call and return success
5. **2captcha / Anti-captcha services**: Third-party services that use human solvers — acceptable ONLY for testing external sites you don't own, never for internal apps

### Java Example
```java
// APPROACH 1: Verify environment bypass is active before test
@BeforeAll
static void verifyCaptchaBypass() {
    WebDriver tempDriver = new ChromeDriver(new ChromeOptions().addArguments("--headless=new"));
    tempDriver.get(BASE_URL + "/login");
    boolean bypassActive = tempDriver.findElements(
        By.cssSelector("[data-testid='captcha-bypass-indicator']")).size() > 0;
    tempDriver.quit();
    assumeTrue(bypassActive, "CAPTCHA bypass not active — skipping test suite");
}

// APPROACH 2: Inject Google reCAPTCHA TEST keys via JS (test env only)
public void injectRecaptchaTestResponse(WebDriver driver) {
    // Only valid with Google's test site key
    ((JavascriptExecutor) driver).executeScript(
        "document.getElementById('g-recaptcha-response').innerHTML = 'test-response-token';" +
        "document.getElementById('g-recaptcha-response').style.display = 'block';"
    );
}

// APPROACH 3: Mock CAPTCHA validation at network level (WireMock)
// In test setup:
WireMockServer wireMock = new WireMockServer(8089);
wireMock.stubFor(post(urlEqualTo("/recaptcha/api/siteverify"))
    .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{\"success\": true, \"score\": 0.9}")));

// APPROACH 4: API-level bypass header (negotiated with backend team)
ChromeOptions options = new ChromeOptions();
// Use CDP to inject header on all requests
Map<String, Object> headers = Map.of("X-Test-CAPTCHA-Bypass", "true");
((ChromeDriver) driver).executeCdpCommand(
    "Network.setExtraHTTPHeaders", Map.of("headers", headers));

// APPROACH 5: reCAPTCHA v3 — override score check
// Backend checks score threshold; inject JS to mock grecaptcha
((JavascriptExecutor) driver).executeScript(
    "window.grecaptcha = { execute: async () => 'test-token-v3', ready: (cb) => cb() };"
);
```

### Real-world Usage
- **CI/CD pipeline**: All environments above `dev` should have CAPTCHA disabled via feature flag — mandatory for automation
- **External site testing** (not your app): 2captcha/Anti-captcha as last resort with rate limiting
- **reCAPTCHA v3 scoring systems**: Inject Google's test key in staging; backend validates test tokens as always-pass
- **Login page automation**: Most enterprises disable CAPTCHA entirely in QA/staging environments; document it in the test environment setup guide

### Common Mistakes
- Building CAPTCHA automation into the framework — signals the test environment is improperly configured, not that automation is clever
- Using OCR to solve text CAPTCHAs — bypasses your own security controls; creates a real vulnerability
- Hardcoding the bypass JS injection without checking the environment — accidentally running bypass code in production
- Treating CAPTCHA as an automation challenge to solve, not an environment configuration problem
- Using 2captcha for your own app's test environment — unnecessary cost and unreliable latency

### Optimization Tip
The senior solution is to **raise a ticket** to the dev team: test environments must have CAPTCHA disabled or use Google's test keys. Document this as a **test environment contract requirement** in your framework's README. Any test that encounters live CAPTCHA should fail with a clear message: `"CAPTCHA bypass not configured — this is a test environment issue, not a test failure."`

### Debugging Strategy
1. Test always fails at login → inspect if CAPTCHA is present in test environment — check `driver.getPageSource()` for reCAPTCHA widget
2. CAPTCHA appears only on CI → IP-based CAPTCHA triggering — whitelist CI server IPs in the CAPTCHA config
3. Bypass works locally but not in CI → feature flag not set in CI environment variables — check CI configuration
4. reCAPTCHA v3 fails validation → score too low for automated requests — use test keys or mock the `/siteverify` endpoint

### Interview Trap
The interviewer is testing **engineering judgment over cleverness**. The wrong answer: "I use 2captcha to solve it." The right answer: "CAPTCHA should not exist in test environments — I work with the team to add a bypass. If I'm testing an external site I don't own, I'd use a solving service as a last resort." This shows **systems thinking and collaboration**, not just technical workarounds.

### Follow-up Questions
1. How would you test that CAPTCHA is working correctly (i.e., that it actually blocks bots) without automating the CAPTCHA itself?
2. How do you handle a scenario where the client insists CAPTCHA must remain active in the staging environment?

### Selenium vs Playwright
Both Playwright and Selenium face the same CAPTCHA challenge — it's not a framework issue, it's an environment issue. Playwright's CDP-based network interception makes it slightly easier to mock CAPTCHA API responses at the network layer, but the correct solution is the same: **eliminate CAPTCHA from test environments**.

---

## Q21: How do you implement parallel test execution with Selenium and TestNG/JUnit 5?

### Interview Answer
Parallel execution requires **thread-safe WebDriver management** — each thread must own its own `WebDriver` instance. Use `ThreadLocal<WebDriver>` in a `DriverFactory` to isolate driver instances per thread. TestNG supports parallelism via `parallel="methods|classes|tests"` in the suite XML; JUnit 5 uses `@Execution(CONCURRENT)` with `junit.jupiter.execution.parallel.config`.

### Deep Explanation
**The core problem:** `WebDriver` is not thread-safe. If two threads share a driver instance, W3C commands interleave — one thread's `findElement` gets another thread's element. Session IDs are not reentrant.

**Solution: `ThreadLocal<WebDriver>`**
- Each thread gets its own `WebDriver` instance stored in a `ThreadLocal`
- `ThreadLocal.get()` returns the calling thread's driver — zero contention
- `ThreadLocal.remove()` in `@AfterEach` / `@AfterMethod` prevents memory leaks

**Thread-safe driver lifecycle:**
```
Thread-1: DriverFactory.init() → creates ChromeDriver-1 → stored in ThreadLocal[T1]
Thread-2: DriverFactory.init() → creates ChromeDriver-2 → stored in ThreadLocal[T2]
Thread-1: DriverFactory.get()  → returns ChromeDriver-1 (T1's instance)
Thread-2: DriverFactory.get()  → returns ChromeDriver-2 (T2's instance)
```

### Java Example
```java
// THREAD-SAFE DRIVER FACTORY
public class DriverFactory {
    private static final ThreadLocal<WebDriver> driverPool = new ThreadLocal<>();

    public static void initDriver(String browser) {
        WebDriver driver;
        switch (browser.toLowerCase()) {
            case "chrome" -> {
                ChromeOptions opts = new ChromeOptions();
                opts.addArguments("--headless=new", "--window-size=1920,1080",
                                  "--no-sandbox", "--disable-dev-shm-usage");
                driver = new ChromeDriver(opts);
            }
            case "firefox" -> {
                FirefoxOptions opts = new FirefoxOptions();
                opts.addArguments("-headless");
                driver = new FirefoxDriver(opts);
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
        driver.manage().timeouts().implicitlyWait(Duration.ZERO); // always use explicit waits
        driverPool.set(driver);
    }

    public static WebDriver getDriver() {
        WebDriver driver = driverPool.get();
        if (driver == null) throw new IllegalStateException("Driver not initialized for thread: "
            + Thread.currentThread().getName());
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = driverPool.get();
        if (driver != null) {
            driver.quit();
            driverPool.remove(); // CRITICAL — prevent memory leak
        }
    }
}

// BASE TEST CLASS
public class BaseTest {
    @BeforeEach
    void setUp() {
        String browser = System.getProperty("browser", "chrome");
        DriverFactory.initDriver(browser);
    }

    @AfterEach
    void tearDown() {
        DriverFactory.quitDriver();
    }

    protected WebDriver driver() {
        return DriverFactory.getDriver();
    }
}

// JUNIT 5 — Parallel configuration (junit-platform.properties)
// junit.jupiter.execution.parallel.enabled = true
// junit.jupiter.execution.parallel.mode.default = concurrent
// junit.jupiter.execution.parallel.config.strategy = fixed
// junit.jupiter.execution.parallel.config.fixed.parallelism = 4

@Execution(ExecutionMode.CONCURRENT)
class CheckoutTest extends BaseTest {
    @Test void testCardPayment() { /* uses driver() — own thread's WebDriver */ }
    @Test void testPaypalPayment() { /* own WebDriver instance */ }
}

// TESTNG — Suite XML for parallel execution
/*
<suite name="Regression" parallel="methods" thread-count="4">
  <test name="CheckoutTests">
    <classes>
      <class name="com.example.tests.CheckoutTest"/>
    </classes>
  </test>
</suite>
*/

// TESTNG BASE TEST
public class BaseTestNG {
    @BeforeMethod
    public void setUp() {
        DriverFactory.initDriver(System.getProperty("browser", "chrome"));
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
```

### Real-world Usage
- **CI/CD pipeline with 200+ tests**: 4 parallel threads reduces 40-minute suite to 10 minutes
- **Cross-browser matrix**: TestNG `@Parameters({"browser"})` × parallel threads = run Chrome + Firefox + Safari simultaneously
- **Selenium Grid + parallel**: Each parallel thread creates a `RemoteWebDriver` session on the Grid — Grid distributes across nodes
- **Maven Surefire plugin**: `<forkCount>4</forkCount><reuseForks>true</reuseForks>` for class-level parallelism

### Common Mistakes
- Storing `WebDriver` as a static field — all threads share one instance → corrupted sessions
- Using `@BeforeAll` (JUnit 5) to initialize the driver — `@BeforeAll` runs once per class, driver shared → not thread-safe
- Forgetting `ThreadLocal.remove()` in `@AfterEach` → `ThreadLocal` holds strong reference to driver → memory leak over hundreds of tests
- Using `parallel="instances"` in TestNG without understanding it creates new test class instances per method — can cause `@BeforeClass` not running correctly
- Setting implicit wait on the driver — always `Duration.ZERO` with parallel explicit waits to avoid combined timeout issues

### Optimization Tip
For Selenium Grid + parallel, initialize `RemoteWebDriver` lazily — create it only when the first `driver()` call happens in the test, not in `@BeforeEach`. This avoids creating sessions for tests that fail preconditions before any browser interaction:
```java
public static WebDriver getDriver() {
    if (driverPool.get() == null) {
        initDriver(System.getProperty("browser", "chrome")); // lazy init
    }
    return driverPool.get();
}
```

### Debugging Strategy
1. `SessionNotFoundException` in parallel run → two threads used same driver → static driver field, not `ThreadLocal` — verify with thread dump
2. Tests interfere with each other (wrong data, wrong page state) → shared state outside `ThreadLocal` (static page objects, shared test data)
3. Memory grows unboundedly in long parallel runs → `ThreadLocal.remove()` missing in `@AfterEach`
4. Grid shows session count < thread count → Grid node ran out of slots — increase node max sessions
5. Add thread name to test logs: `"[" + Thread.currentThread().getName() + "] " + message` — critical for parallel debugging

### Interview Trap
"Why can't you use `static WebDriver`?" — The answer must include: **Java memory model**, **race conditions on static field access**, and **session ID mixing**. Also: "Why must you call `ThreadLocal.remove()`?" — **Memory leak**: `ThreadLocal` values are stored in the `Thread` object's map; thread pool threads are reused, so old `WebDriver` references persist and accumulate.

### Follow-up Questions
1. How would you implement a listener that captures a screenshot from the correct thread's driver when any test fails in a parallel run?
2. How do you share test data safely across parallel tests without using static fields?

### Selenium vs Playwright
Playwright's `Browser`, `BrowserContext`, and `Page` objects are **already thread-isolated** by design. You pass `Page` to each test method. No `ThreadLocal` pattern needed — Playwright's API is inherently parallel-safe when each test uses its own `Page` / `BrowserContext`.

---

## Q22: How do you prevent and diagnose flaky tests in a Selenium framework?

### Interview Answer
Flaky tests fail intermittently without code changes — the root causes are **timing issues** (missing/wrong waits), **test data conflicts** (shared mutable state), **environment instability** (network, CI resource contention), and **locator fragility** (dynamic attributes). Fixes: proper explicit waits, test data isolation, retry mechanisms with root-cause logging, and flakiness tracking dashboards.

### Deep Explanation
**Flakiness taxonomy:**
| Category | Root Cause | Fix |
|---|---|---|
| Timing | Missing wait / wrong condition | Fluent Wait on correct condition |
| Stale element | DOM rebuilt mid-test | `ignoring(StaleElementReferenceException.class)` |
| Race condition | Async API/JS updates DOM | Wait for network idle or specific DOM state |
| Data conflict | Tests share mutable test data | Test-owned data, unique per test |
| Locator fragility | Dynamic ID/class | `data-testid` or stable attribute |
| Environment | CI resource pressure | Retry with backoff, resource monitoring |
| Order dependency | Test B requires Test A's state | Independent tests, proper setup/teardown |
| Animation | CSS transitions intercept clicks | Wait for animation to complete |

### Java Example
```java
// RETRY MECHANISM — JUnit 5 extension
public class RetryExtension implements TestExecutionExceptionHandler {
    private static final int MAX_RETRIES = 2;
    private final Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        String testId = context.getUniqueId();
        int attempts = retryCount.getOrDefault(testId, 0);
        if (attempts < MAX_RETRIES) {
            retryCount.put(testId, attempts + 1);
            // Log which attempt this is
            System.out.printf("[RETRY] %s — attempt %d/%d — cause: %s%n",
                context.getDisplayName(), attempts + 1, MAX_RETRIES,
                throwable.getClass().getSimpleName());
            throw throwable; // JUnit 5 re-runs on exception in extension
        }
        // Max retries exhausted — record as flaky
        FlakinessDashboard.record(testId, context.getDisplayName(), throwable);
        throw throwable;
    }
}

// TESTNG — built-in retry analyzer
public class RetryAnalyzer implements IRetryAnalyzer {
    private int retryCount = 0;
    private static final int MAX_RETRY = 2;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            System.out.printf("[RETRY] %s attempt %d%n",
                result.getName(), retryCount);
            return true;
        }
        return false;
    }
}

// Use on test:
@Test(retryAnalyzer = RetryAnalyzer.class)
public void testCheckout() { ... }

// WAIT FOR ANIMATION COMPLETE
public void waitForAnimationsComplete(WebDriver driver) {
    new WebDriverWait(driver, Duration.ofSeconds(5))
        .until(d -> (Boolean) ((JavascriptExecutor) d)
            .executeScript(
                "return document.querySelectorAll(':animating').length === 0 && " +
                "document.readyState === 'complete'"));
}

// WAIT FOR NETWORK IDLE (no pending XHR/fetch)
public void waitForNetworkIdle(WebDriver driver) {
    ((JavascriptExecutor) driver).executeScript(
        "window.__pendingRequests = 0;" +
        "var origOpen = XMLHttpRequest.prototype.open;" +
        "XMLHttpRequest.prototype.open = function() {" +
        "  window.__pendingRequests++;" +
        "  this.addEventListener('loadend', () => window.__pendingRequests--);" +
        "  origOpen.apply(this, arguments);" +
        "};");
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(d -> (Long)((JavascriptExecutor) d)
            .executeScript("return window.__pendingRequests") == 0);
}

// FLAKINESS TRACKING — record and report
public class FlakinessDashboard {
    private static final List<FlakyRecord> records = Collections.synchronizedList(new ArrayList<>());

    public static void record(String testId, String testName, Throwable cause) {
        records.add(new FlakyRecord(testId, testName, cause.getClass().getSimpleName(),
            LocalDateTime.now()));
    }

    public static void printReport() {
        System.out.println("=== FLAKY TEST REPORT ===");
        records.forEach(r -> System.out.printf("  [FLAKY] %s — %s at %s%n",
            r.testName(), r.causeType(), r.timestamp()));
    }
}
```

### Real-world Usage
- **FinTech checkout tests**: Payment gateway has 100–800ms response variance — explicit wait on confirmation element, not `Thread.sleep`
- **Admin dashboard with live data**: Table refreshes every 30s — StaleElement retry + wait for table staleness before re-read
- **CI with 8 parallel threads on 4-core machine**: Resource contention causes timing flakiness — increase polling interval, reduce thread count in constrained environments
- **A/B test environments**: Some users see variant B — self-healing locator covers both variants

### Common Mistakes
- Adding retry as the **first** fix for flakiness — retry hides the root cause; diagnose first, retry as safety net only
- Retrying `AssertionError` — flaky assertions indicate wrong test logic or bad test data, not environmental issues; never retry assertions
- Not logging which retry attempt fixed the test — without logs, you'll never find the root cause
- Setting retry to 3+ attempts — a test that needs 3 retries to pass is not a test, it's a prayer
- Using `Thread.sleep(2000)` as an "anti-flakiness" patch — adds 2 seconds permanently; use condition-based waits

### Optimization Tip
Instrument your test framework to **classify retry failures**:
```java
// Retryable exceptions (environmental):
Set<Class<? extends Throwable>> RETRYABLE = Set.of(
    TimeoutException.class,
    StaleElementReferenceException.class,
    NoSuchWindowException.class,
    WebDriverException.class
);

// Never retry:
Set<Class<? extends Throwable>> NOT_RETRYABLE = Set.of(
    AssertionError.class,
    IllegalArgumentException.class,
    NullPointerException.class
);
```
Only retry on environmental/infrastructure failures — never on assertion or logic failures.

### Debugging Strategy
1. Test fails on CI but passes locally → timing or resource contention in CI — add explicit wait on the failing step
2. Fails 1 in 10 runs → `StaleElementReferenceException` — add `ignoring(StaleElementReferenceException.class)` to FluentWait
3. Fails only in parallel runs → shared mutable state — grep for `static` fields in page objects and test data
4. Fails after a deployment → locator changed — switch to `data-testid`
5. Build a **flaky test heatmap**: track failure frequency per test over 30 days — top 10 flaky tests get dedicated fix sprint

### Interview Trap
"How do you fix flaky tests?" — The wrong answer: "I add retry." The right answer: **Diagnose → Classify → Fix root cause → Retry as fallback.** Interviewers want to hear: timing analysis, DOM mutation detection, test data isolation, locator stability, and that retry is a **last resort with logging**, not a first response.

### Follow-up Questions
1. How would you build a flakiness detection system that automatically quarantines tests that fail more than X% of the time?
2. What is the difference between a flaky test and an unreliable test environment, and how do you tell them apart?

### Selenium vs Playwright
Playwright's auto-wait engine eliminates the majority of timing-related flakiness at the framework level. Its `--retries` flag and trace viewer (`npx playwright show-trace`) provide built-in flakiness analysis with step-by-step screenshots and network logs — comparable to months of custom instrumentation in a Selenium framework.

---

## Q23: How do you implement a robust Page Object Model (POM) in production?

### Interview Answer
Production POM separates **page interaction logic** from **test logic**. Each page class encapsulates locators and action methods — tests call high-level methods like `loginPage.loginAs(user)`, never `driver.findElement(...)` directly. Use **lazy element resolution** (find at call time, not at construction), **wait encapsulation** inside page methods, and **fluent builder pattern** for multi-step flows.

### Deep Explanation
**POM layers in a mature framework:**
```
Test Layer          → business-level test methods, assertions
Page Object Layer   → page-specific actions (loginAs, addToCart)
Component Layer     → reusable UI components (Header, DatePicker, Modal)
Base Page Layer     → shared utilities (wait, scroll, highlight)
Driver Layer        → ThreadLocal DriverFactory
```

**Anti-patterns to eliminate:**
- `WebElement` fields initialized at construction → stale references
- `driver.findElement(...)` in test classes → defeats POM purpose
- `Thread.sleep()` inside page methods → fragile, slow
- Page methods returning `void` → prevents fluent chaining
- Fat page objects (500+ lines) → violates single responsibility

### Java Example
```java
// BASE PAGE — shared infrastructure
public abstract class BasePage {
    protected final WebDriver driver;
    protected final WaitUtils wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this); // optional — for @FindBy annotation style
    }

    protected WebElement find(By locator) {
        return wait.visible(locator);
    }

    protected void click(By locator) {
        wait.clickable(locator).click();
    }

    protected void type(By locator, String text) {
        WebElement el = wait.clickable(locator);
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(By locator) {
        return wait.visible(locator).getText().trim();
    }

    protected void scrollTo(By locator) {
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }
}

// LOGIN PAGE — page-specific actions
public class LoginPage extends BasePage {
    // Locators as private constants — not WebElement fields
    private static final By EMAIL    = By.cssSelector("[data-testid='email-input']");
    private static final By PASSWORD = By.cssSelector("[data-testid='password-input']");
    private static final By SUBMIT   = By.cssSelector("[data-testid='login-submit']");
    private static final By ERROR    = By.cssSelector("[data-testid='login-error']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage enterEmail(String email) {
        type(EMAIL, email);
        return this; // fluent — returns self for chaining
    }

    public LoginPage enterPassword(String password) {
        type(PASSWORD, password);
        return this;
    }

    public DashboardPage submit() {
        click(SUBMIT);
        // Return the destination page — test knows where submit leads
        return new DashboardPage(driver);
    }

    // HIGH-LEVEL convenience method for tests
    public DashboardPage loginAs(String email, String password) {
        return enterEmail(email)
            .enterPassword(password)
            .submit();
    }

    public String getErrorMessage() {
        return getText(ERROR);
    }
}

// DASHBOARD PAGE
public class DashboardPage extends BasePage {
    private static final By WELCOME_MSG   = By.cssSelector("[data-testid='welcome-message']");
    private static final By NAV_REPORTS   = By.cssSelector("[data-testid='nav-reports']");

    public DashboardPage(WebDriver driver) {
        super(driver);
        // Verify we're on the right page
        wait.urlContains("/dashboard");
    }

    public String getWelcomeMessage() {
        return getText(WELCOME_MSG);
    }

    public ReportsPage navigateToReports() {
        click(NAV_REPORTS);
        return new ReportsPage(driver);
    }
}

// COMPONENT — reusable across pages
public class DatePickerComponent extends BasePage {
    private final By container;

    public DatePickerComponent(WebDriver driver, By containerLocator) {
        super(driver);
        this.container = containerLocator;
    }

    public void selectDate(LocalDate date) {
        click(container); // open picker
        // navigate to month/year, click day
        By dayLocator = By.xpath(String.format(
            "//div[@data-testid='datepicker']//td[@data-date='%s']",
            date.format(DateTimeFormatter.ISO_DATE)));
        click(dayLocator);
    }
}

// TEST — clean, readable, no driver interaction
class LoginTest extends BaseTest {
    @Test
    void validLoginNavigatesToDashboard() {
        LoginPage loginPage = new LoginPage(driver());
        DashboardPage dashboard = loginPage.loginAs("user@test.com", "password");
        assertThat(dashboard.getWelcomeMessage()).contains("Welcome");
    }

    @Test
    void invalidPasswordShowsError() {
        String error = new LoginPage(driver())
            .enterEmail("user@test.com")
            .enterPassword("wrongpass")
            .submit(); // type error intentional — submit returns DashboardPage but we never navigate
        // Better practice: have a separate submitExpectingError() method
    }
}
```

### Real-world Usage
- **Enterprise checkout flow**: `cartPage.addItem(product).checkout().fillShipping(address).pay(card)` — each method returns next page
- **Admin portal**: `UserManagementPage.findUser(email).editRole(ADMIN).save()` — component-level operations
- **SPA with modals**: `OrderPage.clickCancelOrder()` returns a `ConfirmationModal` component — modal as a first-class component

### Common Mistakes
- Returning `void` from page methods — prevents fluent chaining; return page object or next destination
- `@FindBy` with PageFactory in parallel tests — PageFactory uses `@FindBy` proxies lazily, but the proxy mechanism is not thread-safe without careful handling
- Huge "God" page objects — one class for entire checkout flow → split by page section or step
- Assertions inside page objects — page objects should never assert; return values to test for assertion
- Navigation methods that don't return the destination page — test has to know what page comes next, leaking page knowledge into test layer

### Optimization Tip
Use **page verification in constructor** — each page object verifies the URL or a unique page element in its constructor. This gives immediate, clear error messages when navigation fails:
```java
public DashboardPage(WebDriver driver) {
    super(driver);
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.urlContains("/dashboard"));
    // If this times out, the error message clearly shows "Expected /dashboard"
}
```

### Debugging Strategy
1. `NullPointerException` in page object → PageFactory `@FindBy` element not found → check if element exists, add null check or use `By` locators with explicit wait
2. `StaleElementReferenceException` in page method → element stored as field, not found per-call → convert to `By` constant + `find()` call
3. Wrong page returned from navigation method → page verification in constructor catches it immediately
4. Test is brittle to page redesigns → locators scattered in test methods, not in page object → refactor locators to page object constants

### Interview Trap
"What is the difference between POM and PageFactory?" — PageFactory is an **implementation mechanism** (annotation-based element proxy injection) that can be used inside POM. POM is an **architectural pattern**. PageFactory is optional and has thread-safety caveats in parallel execution. Many senior engineers prefer `By` constants over `@FindBy` for clarity and thread safety.

### Follow-up Questions
1. How would you structure a POM for a Single Page Application where multiple "pages" exist at the same URL (different states)?
2. What is the Component Object Model (COM) and how does it extend POM for reusable UI widgets?

### Selenium vs Playwright
Playwright's `Locator` API encourages a component-like model natively — `page.locator('.checkout-form').locator('input[name="email"]')` scopes the locator to the component. Its `Fixtures` system provides page object injection per test without manual instantiation, similar to Spring DI.

---

## Q24: How does Chrome DevTools Protocol (CDP) integration work in Selenium 4?

### Interview Answer
Selenium 4 exposes **Chrome DevTools Protocol (CDP)** via `ChromeDriver.executeCdpCommand()` and the `DevTools` API. CDP enables capabilities beyond standard WebDriver: **network interception**, **console log capture**, **geolocation mocking**, **performance metrics**, **JavaScript coverage**, and **mobile device emulation**. It's the bridge to browser internals that standard W3C commands can't reach.

### Deep Explanation
**CDP architecture:**
- CDP is a **JSON-based protocol** over WebSocket — Chrome exposes it on `--remote-debugging-port`
- Organized into **domains**: `Network`, `Page`, `Runtime`, `Performance`, `Emulation`, `Log`, `Security`, `Target`
- Each domain has **methods** (commands), **events** (async notifications), and **types**
- Selenium 4's `DevTools` API is a typed Java wrapper around raw CDP — auto-generated from the CDP spec

**Two access methods in Selenium 4:**
1. `driver.executeCdpCommand(method, params)` — raw Map-based, works for any CDP method
2. `((HasDevTools) driver).getDevTools()` → typed API with auto-complete for specific domains

### Java Example
```java
ChromeDriver driver = new ChromeDriver(new ChromeOptions());
DevTools devTools = driver.getDevTools();
devTools.createSession();

// 1. NETWORK INTERCEPTION — mock API responses
devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
devTools.addListener(Network.requestWillBeSent(), request -> {
    System.out.println("Request: " + request.getRequest().getUrl());
});

// BLOCK specific URL pattern
devTools.send(Network.setBlockedURLs(List.of("*analytics*", "*tracking*")));

// MOCK response for API call
devTools.send(Network.setRequestInterception(
    List.of(new RequestPattern(Optional.of("*/api/payments"), Optional.empty(),
        Optional.of(InterceptionStage.RESPONSE)))));
devTools.addListener(Network.requestIntercepted(), intercepted -> {
    devTools.send(Network.continueInterceptedRequest(
        intercepted.getInterceptionId(),
        Optional.empty(),
        Optional.of("401"),  // mock HTTP status
        Optional.of("Unauthorized"),
        Optional.empty(),
        Optional.of("{\"error\":\"Payment failed\"}"),
        Optional.empty(),
        Optional.empty()));
});

// 2. CONSOLE LOG CAPTURE
devTools.send(Log.enable());
devTools.addListener(Log.entryAdded(), entry ->
    System.out.println("[BROWSER-LOG] " + entry.getLevel() + ": " + entry.getText()));

// 3. GEOLOCATION MOCK
devTools.send(Emulation.setGeolocationOverride(
    Optional.of(37.7749),   // latitude (San Francisco)
    Optional.of(-122.4194), // longitude
    Optional.of(1.0)));     // accuracy

// 4. NETWORK CONDITIONS (throttle to 3G)
devTools.send(Network.emulateNetworkConditions(
    false,   // offline
    100,     // latency ms
    500_000, // download bytes/sec (~500KB/s)
    100_000, // upload bytes/sec
    Optional.empty()));

// 5. PERFORMANCE METRICS
devTools.send(Performance.enable(Optional.empty()));
List<Metric> metrics = devTools.send(Performance.getMetrics());
metrics.stream()
    .filter(m -> m.getName().equals("DOMContentLoaded") || m.getName().equals("FirstPaint"))
    .forEach(m -> System.out.println(m.getName() + ": " + m.getValue()));

// 6. DEVICE EMULATION (mobile)
devTools.send(Emulation.setDeviceMetricsOverride(
    375,    // width (iPhone 12)
    812,    // height
    3.0,    // device scale factor
    true,   // mobile
    Optional.empty(), Optional.empty(), Optional.empty(),
    Optional.empty(), Optional.empty(), Optional.empty(),
    Optional.empty(), Optional.empty(), Optional.empty()));

// 7. JAVASCRIPT COVERAGE
devTools.send(Profiler.enable());
devTools.send(Profiler.startPreciseCoverage(Optional.of(true), Optional.of(true), Optional.empty()));
driver.get("https://example.com");
// ... test actions ...
ScriptCoverage coverage = devTools.send(Profiler.takePreciseCoverage());

// 8. RAW CDP COMMAND (executeCdpCommand — simpler for one-off use)
driver.executeCdpCommand("Page.setDownloadBehavior",
    Map.of("behavior", "allow", "downloadPath", "/tmp/downloads"));
```

### Real-world Usage
- **Network mocking**: Test payment failure UI by returning 500 from `/api/payment` without backend changes
- **Performance baseline testing**: Capture `FirstContentfulPaint`, `LargestContentfulPaint`, `TimeToInteractive` via Performance domain
- **Geolocation-dependent features**: Test location-based pricing, region restrictions by mocking lat/long
- **Slow network simulation**: Throttle to 3G to test loading skeletons, timeout handling, progressive loading
- **Auth token injection**: Set Authorization header via `Network.setExtraHTTPHeaders` without login UI
- **Security testing**: Override SSL certificate errors for internal staging environments

### Common Mistakes
- Using CDP in a way that ties tests to Chrome-only — wrap CDP calls behind an interface for portability
- Not closing the DevTools session — leaked DevTools sessions consume WebSocket connections
- Using deprecated CDP methods — CDP evolves with Chrome; check Chrome version compatibility
- Calling `devTools.createSession()` multiple times — creates nested sessions; call once per driver lifecycle
- Forgetting CDP event listeners are async — add listeners BEFORE the action that triggers the event

### Optimization Tip
Wrap CDP operations in a dedicated `CDPUtils` class scoped to the `ChromeDriver` lifecycle:
```java
public class CDPUtils implements AutoCloseable {
    private final DevTools devTools;

    public CDPUtils(ChromeDriver driver) {
        this.devTools = driver.getDevTools();
        this.devTools.createSession();
        this.devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
    }

    public void mockApiResponse(String urlPattern, int statusCode, String body) { ... }
    public void throttleTo3G() { ... }
    public void mockGeolocation(double lat, double lon) { ... }

    @Override
    public void close() {
        devTools.close();
    }
}
// Use in test: try (var cdp = new CDPUtils(chromeDriver)) { cdp.mockApiResponse(...); }
```

### Debugging Strategy
1. `DevToolsException` on CDP command → check Chrome version — some CDP methods change between Chrome versions
2. Network listener not firing → `Network.enable()` must be called before navigation, not after
3. `createSession()` throws → remote debugging port not open — ensure ChromeDriver was started with `--remote-debugging-port` arg
4. CDP works locally, fails on Grid (RemoteWebDriver) → Grid nodes must expose CDP endpoint; use `((Augmenter) new Augmenter()).augment(remoteDriver)` to enable CDP on remote sessions

### Interview Trap
"What can CDP do that standard Selenium can't?" — Network interception, JS coverage, browser console streaming, performance metrics, device emulation, geolocation mocking, security overrides. The key insight: **CDP gives test automation access to the same tools as Chrome DevTools** — the same panel a frontend developer uses to debug. That's the entire value proposition.

### Follow-up Questions
1. What is WebDriver BiDi and how is it designed to replace CDP for cross-browser use cases?
2. How would you use CDP to test that a payment form correctly handles a network timeout?

### Selenium vs Playwright
Playwright uses CDP natively for all Chrome/Edge operations — there's no separate "enable CDP" step. Playwright's `page.route()` for network interception, `page.emulateMedia()`, and `page.coverage` all use CDP internally. Firefox support uses a CDP-compatible protocol layer. Playwright is essentially built on top of what CDP enables.

---

## Q25: How do you implement network request interception and response mocking in Selenium?

### Interview Answer
Network interception in Selenium requires **CDP** (Chrome only via `ChromeDriver`) or a **proxy** (BrowserMob Proxy, WireMock with proxy settings — works cross-browser). Use it to mock API responses, test error states without backend changes, block analytics/tracking calls, and inject headers. For cross-browser or Grid scenarios, BrowserMob Proxy is the production-grade choice.

### Deep Explanation
**Three approaches:**

1. **CDP Network Interception (Chrome only)**: Use `Network.setRequestInterception` or `Fetch.enable` — intercepts at Chrome's network layer before requests leave the browser. Fast, no external process. Chrome-specific.

2. **BrowserMob Proxy**: Java library that starts a local HTTP proxy. Configure the browser to use it. Can intercept, modify, and assert on HTTP requests/responses for **any browser**. Works with RemoteWebDriver on Grid.

3. **WireMock as mock server**: Start WireMock, configure app's API base URL to point to WireMock in test config. Not true interception — requires configurable base URL in the application under test.

**CDP `Fetch` domain vs `Network` domain:**
- `Network.requestIntercepted` — older, deprecated in newer Chrome
- `Fetch.requestPaused` — current standard; intercepts request before sending, allows full modification

### Java Example
```java
// ============ APPROACH 1: CDP Fetch Interception (Chrome) ============
ChromeDriver driver = new ChromeDriver();
DevTools devTools = driver.getDevTools();
devTools.createSession();

devTools.send(Fetch.enable(
    Optional.of(List.of(new RequestPattern(
        Optional.of("*/api/*"),          // URL pattern
        Optional.empty(),
        Optional.of(RequestStage.RESPONSE)))),  // intercept response
    Optional.of(false)));

devTools.addListener(Fetch.requestPaused(), request -> {
    String url = request.getRequest().getUrl();

    if (url.contains("/api/payment")) {
        // MOCK: return 500 error
        devTools.send(Fetch.fulfillRequest(
            request.getRequestId(),
            500,
            Optional.of(List.of(
                new HeaderEntry("Content-Type", "application/json"))),
            Optional.empty(),
            Optional.of(Base64.getEncoder().encodeToString(
                "{\"error\":\"Payment gateway timeout\"}".getBytes())),
            Optional.of("Internal Server Error")));
    } else {
        // PASS THROUGH unchanged
        devTools.send(Fetch.continueRequest(
            request.getRequestId(),
            Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty()));
    }
});

driver.get("https://app.example.com/checkout");

// ============ APPROACH 2: BrowserMob Proxy (Cross-browser) ============
BrowserMobProxyServer proxy = new BrowserMobProxyServer();
proxy.start(0); // random port
int port = proxy.getPort();

// INTERCEPT: add request header to all requests
proxy.addRequestFilter((request, contents, messageInfo) -> {
    request.headers().add("X-Test-Mode", "true");
    return null; // null = pass through
});

// INTERCEPT: mock specific response
proxy.addResponseFilter((response, contents, messageInfo) -> {
    if (messageInfo.getUrl().contains("/api/feature-flags")) {
        contents.setTextContents("{\"newCheckout\":true,\"darkMode\":false}");
    }
});

// Configure Chrome to use the proxy
Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
ChromeOptions options = new ChromeOptions();
options.setProxy(seleniumProxy);
options.setAcceptInsecureCerts(true); // proxy MITMs HTTPS
WebDriver driver2 = new ChromeDriver(options);

// ASSERT on captured requests (HAR)
proxy.enableHar();
driver2.get("https://app.example.com");
Har har = proxy.getHar();
har.getLog().getEntries().stream()
    .filter(e -> e.getRequest().getUrl().contains("/api/"))
    .forEach(e -> System.out.println(e.getRequest().getUrl()
        + " → " + e.getResponse().getStatus()));

proxy.stop();

// ============ APPROACH 3: Block analytics/tracking ============
DevTools devTools2 = ((ChromeDriver)driver).getDevTools();
devTools2.createSession();
devTools2.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
devTools2.send(Network.setBlockedURLs(List.of(
    "*google-analytics.com*",
    "*doubleclick.net*",
    "*facebook.net/signals*",
    "*hotjar.com*"
)));
// All analytics calls now return net::ERR_BLOCKED_BY_CLIENT — cleaner, faster tests
```

### Real-world Usage
- **Testing error UI**: Mock `/api/orders` to return 503 → assert "Service Unavailable" banner appears
- **Feature flag testing**: Mock `/api/feature-flags` to return `{newUI: true}` → test new UI without enabling it in backend
- **Performance testing**: Block all 3rd-party scripts (analytics, chat widgets) for consistent load time measurement
- **Security testing**: Inject auth headers via proxy to test authenticated endpoints
- **CI speed optimization**: Block all non-essential 3rd-party calls — can reduce page load by 2–3 seconds

### Common Mistakes
- Using CDP interception with `RemoteWebDriver` on Grid without augmenting — raw RemoteWebDriver doesn't support CDP; must use `Augmenter`
- Not calling `Fetch.continueRequest` for unmatched URLs — all requests pause and timeout
- BrowserMob Proxy without `setAcceptInsecureCerts(true)` — HTTPS interception fails with certificate errors
- Forgetting CDP listeners are asynchronous — add listener before `driver.get()`, not after
- Not stopping the proxy in `@AfterEach` — proxy process leaks, accumulates over test run

### Optimization Tip
For large test suites, use **selective blocking** to eliminate test noise and speed up execution:
```java
// Block all 3rd-party domains not needed for testing
List<String> blocklist = List.of(
    "*google-analytics.com*", "*gtm.js*", "*hotjar.com*",
    "*intercom.io*", "*segment.io*", "*fullstory.com*"
);
devTools.send(Network.setBlockedURLs(blocklist));
// Reduces page load time by 1–4 seconds per navigation in real apps
```

### Debugging Strategy
1. `Fetch.requestPaused` listener never fires → check URL pattern — patterns are glob-style, not regex
2. Mocked response not received by app → check if app uses `fetch()` vs `XMLHttpRequest` — both are intercepted by `Fetch` domain
3. BrowserMob intercepts HTTP but not HTTPS → add `proxy.setTrustAllServers(true)` and `setAcceptInsecureCerts(true)` on ChromeOptions
4. CDP interception breaks on Selenium Grid → use `new Augmenter().augment(remoteDriver)` to expose CDP on remote sessions
5. Request filter not running → check BrowserMob proxy `addRequestFilter` was called BEFORE driver navigation, not after

### Interview Trap
"Can you intercept network requests in Selenium without using CDP?" — Yes: BrowserMob Proxy. This tests whether you know the **cross-browser limitation** of CDP (Chrome-only) and the **proxy-based alternative**. The senior answer covers both and explains when to use each: CDP for Chrome-specific speed, BrowserMob for cross-browser and Grid scenarios.

### Follow-up Questions
1. How would you use network interception to automatically inject an Authorization header into every API request made by the browser during a test?
2. What is the HAR format and how do you use BrowserMob Proxy's HAR capture to assert on API call sequences?

### Selenium vs Playwright
Playwright's `page.route(urlPattern, handler)` is the cleanest network interception API available — one line to intercept, modify, or abort any request. It works for both Chromium and Firefox. `page.route('**/api/payment', route => route.fulfill({status: 500, body: '...'}))` — no CDP setup, no proxy, no session management.

---

## Q26: How do you implement logging and reporting in a production Selenium framework?

### Interview Answer
Production logging uses **SLF4J + Logback** with structured log output — one log file per test run, test-scoped MDC context, and log level tied to environment. Reporting uses **Allure**, **Extent Reports**, or **TestNG HTML reports** — with screenshots, step logs, environment info, and trend history. Logs and reports are separate concerns: logs are for debugging, reports are for stakeholders.

### Deep Explanation
**Logging layers in a test framework:**
```
Test Layer         → log test intent: "Attempting login as admin@test.com"
Page Object Layer  → log actions: "Clicking submit button [data-testid='submit']"
Wait Layer         → log wait events: "Waiting for element visible: [#dashboard]"
Driver Layer       → log WebDriver events (via EventFiringWebDriver listener)
Framework Layer    → log setup/teardown, retries, screenshot captures
```

**Structured logging with MDC (Mapped Diagnostic Context):**
- MDC lets you attach key-value pairs (test name, thread ID, browser) to every log line automatically
- In parallel runs, MDC scopes logs to the current thread — you can filter all log lines for one test

**Report requirements for enterprise:**
- Pass/fail counts with trend over builds
- Screenshot on failure — embedded in report
- Step-by-step execution log
- Environment metadata (browser, OS, build version, environment URL)
- Retry history (test passed on retry → marked as flaky, not green)

### Java Example
```java
// LOGBACK CONFIGURATION (logback-test.xml)
/*
<configuration>
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>target/logs/test-run.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>target/logs/test-run.%d{yyyy-MM-dd}.log</fileNamePattern>
      <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{testName}] [%X{browser}] %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="INFO"><appender-ref ref="FILE"/></root>
</configuration>
*/

// BASE TEST — MDC setup
public class BaseTest {
    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String browser = System.getProperty("browser", "chrome");
        // Set MDC context — automatically added to every log line from this thread
        MDC.put("testName", testInfo.getDisplayName());
        MDC.put("browser", browser);
        MDC.put("threadId", String.valueOf(Thread.currentThread().getId()));
        DriverFactory.initDriver(browser);
        log.info("TEST START — browser={} env={}", browser,
            System.getProperty("env", "staging"));
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("TEST END — {}", testInfo.getDisplayName());
        DriverFactory.quitDriver();
        MDC.clear(); // CRITICAL — clear MDC after test, prevents leak in thread pools
    }
}

// WEB DRIVER EVENT LISTENER — automatic action logging
public class WebDriverLogger implements WebDriverListener {
    private static final Logger log = LoggerFactory.getLogger(WebDriverLogger.class);

    @Override
    public void beforeFindElement(WebDriver driver, By locator) {
        log.debug("Finding element: {}", locator);
    }

    @Override
    public void afterClick(WebElement element) {
        log.info("Clicked: {}", describeElement(element));
    }

    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        log.info("Typed into: {} — value: [MASKED]", describeElement(element));
    }

    @Override
    public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
        log.error("WebDriver error on {}: {}", method.getName(), e.getCause().getMessage());
    }

    private String describeElement(WebElement el) {
        try {
            return el.getTagName() + "[" + el.getAttribute("data-testid") + "]";
        } catch (Exception ex) {
            return "unknown element";
        }
    }
}

// WRAP DRIVER WITH LISTENER
WebDriver rawDriver = new ChromeDriver(options);
WebDriver driver = new EventFiringDecorator<>(new WebDriverLogger()).decorate(rawDriver);

// ALLURE REPORTING — step logging
@Step("Login as {email}")
public DashboardPage loginAs(String email, String password) {
    Allure.parameter("email", email);
    type(EMAIL, email);
    type(PASSWORD, password);
    click(SUBMIT);
    return new DashboardPage(driver);
}

// ALLURE — screenshot on failure (JUnit 5 extension)
public class AllureScreenshotExtension implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        WebDriver driver = DriverFactory.getDriver();
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment("Screenshot on failure", "image/png",
            new ByteArrayInputStream(screenshot), ".png");
        Allure.addAttachment("Page Source", "text/html",
            new ByteArrayInputStream(driver.getPageSource().getBytes()), ".html");
    }
}

// ENVIRONMENT INFO in Allure report
@BeforeSuite
public void setAllureEnvironment() {
    Properties props = new Properties();
    props.setProperty("Browser", System.getProperty("browser", "chrome"));
    props.setProperty("Environment", System.getProperty("env", "staging"));
    props.setProperty("Base URL", System.getProperty("baseUrl"));
    props.setProperty("Build", System.getProperty("buildNumber", "local"));
    try (var out = new FileOutputStream("target/allure-results/environment.properties")) {
        props.store(out, null);
    }
}
```

### Real-world Usage
- **CI pipeline**: Allure report published as GitHub Actions artifact — PM views pass/fail/trend without reading code
- **Flaky test detection**: Allure history shows a test that passes 9/10 times — dashboards it as "flaky"
- **Parallel debugging**: MDC + structured logs — filter `testName=checkout_payment_test` to see only that test's logs
- **Regulatory compliance (FinTech)**: Test execution logs stored 90 days for audit — Logback rolling appender with retention policy

### Common Mistakes
- Logging passwords or sensitive data — always mask: `log.info("Typed into password field [MASKED]")`
- Not clearing MDC after test — thread pool threads carry stale MDC from previous test into next
- Using `System.out.println` for test output — not captured by log appenders, disappears in CI
- One giant log file for all tests — use test-scoped log files or MDC filtering for parallel diagnostics
- Report shows all tests as "Retried" (all green) instead of "Flaky" — configure Allure to distinguish retry success from first-run success

### Optimization Tip
Use **Allure `@Step` annotations** in page objects for automatic step breakdown in reports:
```java
@Step("Navigate to checkout page")
public CheckoutPage goToCheckout() { ... }

@Step("Enter card number {cardNumber}")
public CheckoutPage enterCardNumber(String cardNumber) { ... }
```
This gives stakeholders a human-readable step log without writing separate test documentation.

### Debugging Strategy
1. `NullPointerException` in `@AfterEach` teardown → `DriverFactory.getDriver()` returns null because setup failed → add null check in teardown
2. Allure report shows empty steps → `@Step` annotations on page objects not processed → check Allure JUnit5 dependency and `aspectjweaver` agent
3. MDC values missing in parallel logs → `MDC.put()` called in wrong thread → ensure MDC setup in `@BeforeEach` (per-thread), not `@BeforeAll`
4. Screenshots not attached to Allure → `testFailed` extension not registered → add `@ExtendWith(AllureScreenshotExtension.class)` on BaseTest

### Interview Trap
"What's the difference between logging and reporting?" — Logs are **machine-readable, developer-focused**, timestamped, verbose. Reports are **human-readable, stakeholder-focused**, summarized, visual. A production framework needs both. Interviewers flag candidates who conflate TestNG HTML report = logging.

### Follow-up Questions
1. How would you integrate your Selenium test reports with a CI/CD tool like Jenkins or GitHub Actions to show trend graphs?
2. How do you handle sensitive data (passwords, API keys) that appears in test logs?

### Selenium vs Playwright
Playwright's built-in `--reporter=html` generates a rich report with screenshots, videos, and network traces per test — zero configuration. Its `test.step()` API provides automatic step breakdown. Matching this in Selenium requires Allure + `@Step` + `EventFiringDecorator` + custom extensions.

---

## Q27: How do you handle exception handling and retry mechanisms in Selenium?

### Interview Answer
Production exception handling separates **expected exceptions** (element not found during normal wait — handled by FluentWait) from **unexpected exceptions** (driver crash, session expired — handled at framework level with retry or abort). Never swallow exceptions silently. Retry only **retryable transient failures** — never assertion failures.

### Deep Explanation
**Selenium exception taxonomy:**

| Exception | Cause | Handling |
|---|---|---|
| `NoSuchElementException` | Element not in DOM | Explicit wait / FluentWait |
| `StaleElementReferenceException` | DOM rebuilt after findElement | FluentWait ignoring + re-find |
| `TimeoutException` | Wait condition not met | Log + screenshot + fail |
| `ElementNotInteractableException` | Element exists but not clickable | Wait for `elementToBeClickable` |
| `ElementClickInterceptedException` | Overlay blocking click | Scroll into view / wait for overlay to disappear |
| `NoSuchWindowException` | Window closed unexpectedly | Rebuild window handle, or abort |
| `SessionNotCreatedException` | Driver init failed | Retry driver init |
| `WebDriverException` | Driver process crashed | Retry test from scratch |
| `InvalidSelectorException` | Bad locator syntax | Code fix — not retryable |
| `JavascriptException` | JS error in `executeScript` | Check JS, not retryable |

### Java Example
```java
// EXCEPTION HIERARCHY — retryable vs non-retryable
public class ExceptionClassifier {
    private static final Set<Class<? extends Throwable>> RETRYABLE = Set.of(
        TimeoutException.class,
        StaleElementReferenceException.class,
        NoSuchWindowException.class,
        org.openqa.selenium.WebDriverException.class
    );

    private static final Set<Class<? extends Throwable>> NOT_RETRYABLE = Set.of(
        AssertionError.class,
        InvalidSelectorException.class,
        JavascriptException.class,
        NullPointerException.class,
        IllegalArgumentException.class
    );

    public static boolean isRetryable(Throwable t) {
        Class<?> clazz = t.getClass();
        if (NOT_RETRYABLE.stream().anyMatch(c -> c.isAssignableFrom(clazz))) return false;
        return RETRYABLE.stream().anyMatch(c -> c.isAssignableFrom(clazz));
    }
}

// RETRY TEMPLATE — generic action retry with backoff
public class RetryTemplate {
    private static final Logger log = LoggerFactory.getLogger(RetryTemplate.class);

    public static <T> T execute(Supplier<T> action, int maxAttempts, Duration backoff) {
        Throwable lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Throwable t) {
                if (!ExceptionClassifier.isRetryable(t)) {
                    log.error("Non-retryable exception: {}", t.getMessage());
                    throw t instanceof RuntimeException ? (RuntimeException) t
                        : new RuntimeException(t);
                }
                lastException = t;
                log.warn("Attempt {}/{} failed: {} — {}",
                    attempt, maxAttempts, t.getClass().getSimpleName(), t.getMessage());
                if (attempt < maxAttempts) {
                    try { Thread.sleep(backoff.toMillis() * attempt); } // exponential backoff
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
                }
            }
        }
        throw new RuntimeException("All " + maxAttempts + " attempts failed", lastException);
    }
}

// USAGE
RetryTemplate.execute(
    () -> driver.findElement(By.cssSelector("[data-testid='confirm-btn']")).click(),
    3, Duration.ofMillis(500));

// GLOBAL EXCEPTION HANDLER — JUnit 5 extension
public class GlobalExceptionHandler implements TestExecutionExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        log.error("TEST FAILED: {} — {}: {}",
            context.getDisplayName(),
            throwable.getClass().getSimpleName(),
            throwable.getMessage());

        // Capture artifacts
        try {
            WebDriver driver = DriverFactory.getDriver();
            captureScreenshot(driver, context.getDisplayName());
            captureConsoleLog(driver, context.getDisplayName());
            capturePageSource(driver, context.getDisplayName());
        } catch (Exception e) {
            log.warn("Failed to capture failure artifacts: {}", e.getMessage());
        }

        throw throwable; // re-throw — don't swallow
    }

    private void captureScreenshot(WebDriver driver, String testName) throws IOException {
        byte[] shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        String fileName = testName.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
        Files.write(Paths.get("target/screenshots", fileName), shot);
    }

    private void captureConsoleLog(WebDriver driver, String testName) {
        try {
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            String content = logs.getAll().stream()
                .filter(e -> e.getLevel().intValue() >= Level.WARNING.intValue())
                .map(e -> e.getLevel() + ": " + e.getMessage())
                .collect(Collectors.joining("\n"));
            Files.writeString(Paths.get("target/consolelogs",
                testName.replaceAll("[^a-zA-Z0-9]", "_") + ".log"), content);
        } catch (Exception ignored) {}
    }
}

// ELEMENT CLICK INTERCEPTED — overlay handling
public void safeClick(WebDriver driver, By locator) {
    try {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(locator))
            .click();
    } catch (ElementClickInterceptedException e) {
        log.warn("Click intercepted on {}, trying JS click", locator);
        WebElement el = driver.findElement(locator);
        // First try: scroll into view
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", el);
        new WebDriverWait(driver, Duration.ofSeconds(3))
            .until(ExpectedConditions.elementToBeClickable(locator))
            .click();
    }
}
```

### Real-world Usage
- **FinTech app under load**: `WebDriverException: chrome not reachable` intermittently in CI — retryable, restart driver session
- **SPAs with lazy-loaded components**: `ElementNotInteractableException` because component not mounted yet — `elementToBeClickable` wait resolves it
- **Payment overlay animations**: `ElementClickInterceptedException` because loading overlay hasn't fully disappeared — wait for overlay to become invisible
- **Grid session timeout**: `SessionNotCreatedException` when Grid queue fills — retry with exponential backoff

### Common Mistakes
- `catch (Exception e) { }` — swallowed exceptions hide real failures; always log and re-throw or fail
- Retrying `AssertionError` — this means the test genuinely failed, not an infrastructure issue
- Using bare `WebDriverException` catch without checking the cause — masks `InvalidSelectorException`, `JavascriptException` etc.
- Not capturing artifacts (screenshot, logs) before re-throwing — artifacts are lost after exception propagates
- Overly broad retry (`maxAttempts=5`) — masks systemic failures; use maximum 2 retries

### Optimization Tip
Build a **custom `click()` method with layered fallbacks** in `BasePage` — handles 95% of click failures automatically:
```java
protected void click(By locator) {
    try {
        wait.clickable(locator).click();
    } catch (ElementClickInterceptedException e) {
        scrollIntoCenter(locator);
        wait.clickable(locator).click();
    } catch (StaleElementReferenceException e) {
        // Re-find and retry once
        driver.findElement(locator).click();
    }
}
```

### Debugging Strategy
1. `TimeoutException` — what was the wait condition? Log the exact condition + current page URL + screenshot
2. `ElementClickInterceptedException` — what's blocking? Take screenshot, inspect overlays
3. `WebDriverException: unknown error: net::ERR_NAME_NOT_RESOLVED` — environment config issue, not Selenium issue — check base URL
4. `SessionNotCreatedException` — check ChromeDriver version matches Chrome version
5. Enable structured exception logging: exception class, message, test name, page URL, timestamp — all in one log line

### Interview Trap
"How do you handle `ElementClickInterceptedException`?" — Most candidates say "use JS click." The senior answer: **diagnose first** — what's intercepting? Usually a loading overlay, cookie banner, or element not scrolled into view. Fix the root cause. Use JS click only as a documented last resort, logged with a warning.

### Follow-up Questions
1. How would you build a framework-level exception handler that distinguishes between test failures, infrastructure failures, and application bugs?
2. What is the difference between `WebDriverException` and `SeleniumException` in the Selenium exception hierarchy?

### Selenium vs Playwright
Playwright has significantly fewer exception types — most timing issues are handled by auto-wait. `PlaywrightException` is the root type with a clear message and last-known state snapshot. Selenium's exception hierarchy is broader and more granular, requiring explicit handling per exception type.

---

## Q28: How do you integrate Selenium tests with CI/CD pipelines (Jenkins, GitHub Actions)?

### Interview Answer
Selenium tests in CI require: **headless browser configuration**, **environment-parameterized test runs**, **parallel execution controls**, **test report publishing**, and **failure notification**. The pipeline pattern: checkout → install dependencies → start test infrastructure (Grid/Docker) → run tests in parallel → publish reports → notify on failure.

### Deep Explanation
**CI/CD integration concerns:**

| Concern | Solution |
|---|---|
| No display server | `--headless=new` Chrome, `-headless` Firefox |
| Docker container | `--no-sandbox`, `--disable-dev-shm-usage` |
| Environment config | JVM system properties (`-Denv=staging -Dbaseurl=...`) |
| Parallel execution | Maven Surefire `forkCount`, TestNG thread-count |
| Test reports | Allure, publish as CI artifact |
| Failure notification | Jenkins email/Slack plugin, GitHub Actions notification |
| Browser version | Pin browser + driver versions in Docker image |
| Test selection | TestNG groups, JUnit tags (`@Tag("smoke")`) |

### Java Example
```yaml
# GITHUB ACTIONS — Complete Selenium CI pipeline
name: Selenium Regression

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # nightly full regression

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        browser: [chrome, firefox]  # cross-browser matrix
      fail-fast: false              # don't cancel other browsers on first failure

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

      - name: Install Chrome
        uses: browser-actions/setup-chrome@latest

      - name: Install Firefox
        uses: browser-actions/setup-firefox@latest
        if: matrix.browser == 'firefox'

      - name: Run Selenium Tests
        run: |
          mvn test \
            -Dbrowser=${{ matrix.browser }} \
            -Denv=staging \
            -DbaseUrl=${{ secrets.STAGING_URL }} \
            -Dthreads=4 \
            -Dgroups=regression \
            -Dsurefire.failIfNoSpecifiedTests=false
        env:
          TEST_USERNAME: ${{ secrets.TEST_USERNAME }}
          TEST_PASSWORD: ${{ secrets.TEST_PASSWORD }}

      - name: Publish Allure Results
        uses: actions/upload-artifact@v4
        if: always()  # publish even on failure
        with:
          name: allure-results-${{ matrix.browser }}
          path: target/allure-results/

      - name: Publish Test Screenshots
        uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: failure-screenshots-${{ matrix.browser }}
          path: target/screenshots/

      - name: Notify Slack on failure
        uses: 8398a7/action-slack@v3
        if: failure()
        with:
          status: failure
          fields: repo,message,commit,author,action,workflow
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

```xml
<!-- MAVEN SUREFIRE — parallel + parameterized -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <parallel>methods</parallel>
        <threadCount>${threads}</threadCount>
        <forkCount>1</forkCount>
        <reuseForks>true</reuseForks>
        <systemPropertyVariables>
            <browser>${browser}</browser>
            <env>${env}</env>
            <baseUrl>${baseUrl}</baseUrl>
        </systemPropertyVariables>
        <groups>${groups}</groups>
        <includes>
            <include>**/*Test.java</include>
        </includes>
        <reportFormat>xml</reportFormat>
    </configuration>
</plugin>
```

```groovy
// JENKINS DECLARATIVE PIPELINE
pipeline {
    agent { docker { image 'selenium/standalone-chrome:latest' } }

    parameters {
        choice(name: 'ENV', choices: ['staging', 'production'], description: 'Target environment')
        string(name: 'THREADS', defaultValue: '4', description: 'Parallel thread count')
        string(name: 'GROUPS', defaultValue: 'regression', description: 'TestNG groups')
    }

    stages {
        stage('Checkout') { steps { checkout scm } }

        stage('Run Tests') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'test-credentials',
                        usernameVariable: 'TEST_USER',
                        passwordVariable: 'TEST_PASS')
                ]) {
                    sh """
                        mvn test \
                          -Dbrowser=chrome \
                          -Denv=${params.ENV} \
                          -Dthreads=${params.THREADS} \
                          -Dgroups=${params.GROUPS} \
                          -DtestUsername=${TEST_USER} \
                          -DtestPassword=${TEST_PASS}
                    """
                }
            }
        }

        stage('Publish Reports') {
            steps {
                allure([
                    includeProperties: true,
                    jdk: '',
                    results: [[path: 'target/allure-results']]
                ])
                junit 'target/surefire-reports/*.xml'
            }
        }
    }

    post {
        failure {
            emailext(
                subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Test run failed.\nBuild: ${env.BUILD_URL}",
                to: 'qa-team@company.com'
            )
        }
        always {
            archiveArtifacts artifacts: 'target/screenshots/**', allowEmptyArchive: true
        }
    }
}
```

### Real-world Usage
- **PR gating**: Smoke suite runs on every PR — 5 min max. Full regression runs nightly.
- **Cross-browser matrix**: GitHub Actions matrix runs Chrome + Firefox in parallel — separate artifacts per browser
- **Environment parameterization**: Same tests run against `dev`, `staging`, `staging-2` by changing `-Denv` — single codebase, multiple environments
- **Secret injection**: Test credentials via GitHub Secrets / Jenkins Credentials — never hardcoded in code

### Common Mistakes
- Running full regression on every PR — PR suite should be smoke tests only (< 5 min); full regression = nightly
- Hardcoding base URLs in test code — use system properties: `-DbaseUrl=https://staging.example.com`
- Missing `if: always()` on report publishing — reports not published when tests fail (when you need them most)
- Not pinning browser version in Docker — `latest` tag causes unpredictable failures when Chrome updates
- Storing test credentials in code or property files committed to git — always use CI secrets

### Optimization Tip
Use **test selection by tag** to create a layered pipeline:
```
PR checks    → @Tag("smoke")     → 10 tests, 2 minutes
Merge to dev → @Tag("sanity")    → 50 tests, 10 minutes  
Nightly      → @Tag("regression")→ 500 tests, 30 minutes (parallel)
Pre-release  → @Tag("full")      → all tests
```
This gives maximum coverage without blocking developer workflow.

### Debugging Strategy
1. Tests pass locally, fail in CI → viewport size, missing flags (`--no-sandbox`), different Chrome version — check CI Chrome version
2. All tests fail in parallel on CI → resource contention — reduce `threads` from 4 to 2 on free-tier CI
3. `SessionNotCreatedException` in CI → ChromeDriver version mismatch — use `WebDriverManager` for auto version management
4. Reports not published → check pipeline step order; report publishing must run `if: always()` or `post { always { ... } }`

### Interview Trap
"How do you prevent test credentials from appearing in CI logs?" — Use CI secrets injection via environment variables, never command-line arguments (args appear in process lists). In Maven, pass as system properties from env vars: `-DtestUser=${TEST_USERNAME}` where `TEST_USERNAME` is a secret environment variable. Log masking middleware in Logback redacts known patterns.

### Follow-up Questions
1. How would you implement a CI pipeline that automatically quarantines flaky tests (moves them to a separate suite) after 3 consecutive failures?
2. How do you manage WebDriver versioning in CI to prevent version mismatch errors when Chrome auto-updates?

### Selenium vs Playwright
Playwright's `npx playwright install` downloads browser binaries at the correct version — zero version mismatch. `@playwright/test` integrates natively with CI via `npx playwright test --reporter=github` which outputs GitHub Actions-compatible annotations directly in the PR diff.

---

## Q29: How do you implement test data management in a Selenium framework?

### Interview Answer
Test data management ensures **test isolation** — each test creates, uses, and cleans up its own data. Strategies: **API-driven setup** (fastest, most reliable), **database seeding** (direct SQL), **factory pattern with fake data**, and **data cleanup via `@AfterEach`**. Never share mutable test data between tests. Never depend on pre-existing data in the environment.

### Deep Explanation
**Test data anti-patterns:**
1. **Shared static data**: `testUser = "john@example.com"` — parallel tests conflict, data mutates between tests
2. **UI-driven setup**: Creating test user via UI in `@BeforeEach` — 10x slower than API, flaky on UI changes
3. **Assuming data exists**: `"Log in as existing admin user"` — fails when environment is fresh or data was cleaned
4. **No cleanup**: Created data accumulates, slows down database queries, causes naming conflicts

**Data strategies by speed:**
```
DB seed (direct SQL)  → 1ms  — fastest, tight DB coupling
API setup             → 50ms — fast, tests same stack as production
UI setup              → 3-5s — slowest, highest maintenance
Test fixtures (JSON)  → 0ms  — read-only reference data only
```

### Java Example
```java
// API-DRIVEN TEST DATA FACTORY
public class TestDataFactory {
    private static final String API_BASE = System.getProperty("apiBaseUrl");
    private final List<Runnable> cleanupTasks = new ArrayList<>();
    private final RestAssured restAssured; // or OkHttp, Apache HttpClient

    // CREATE USER via API
    public UserDto createUser(String role) {
        UserDto user = new UserDto(
            "test-" + UUID.randomUUID() + "@example.com",
            "Test" + System.currentTimeMillis(),
            role
        );
        String userId = given()
            .baseUri(API_BASE)
            .header("Authorization", "Bearer " + getAdminToken())
            .contentType("application/json")
            .body(user)
            .when()
            .post("/api/admin/users")
            .then()
            .statusCode(201)
            .extract().path("id");

        user.setId(userId);
        // Register cleanup
        cleanupTasks.add(() -> deleteUser(userId));
        return user;
    }

    // CREATE ORDER via API
    public OrderDto createOrder(String userId, String status) {
        OrderDto order = OrderDto.builder()
            .userId(userId)
            .status(status)
            .amount(BigDecimal.valueOf(99.99))
            .items(List.of(new OrderItem("SKU-001", 2)))
            .build();

        String orderId = given()
            .baseUri(API_BASE)
            .header("Authorization", "Bearer " + getUserToken(userId))
            .body(order)
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201)
            .extract().path("id");

        order.setId(orderId);
        cleanupTasks.add(() -> deleteOrder(orderId));
        return order;
    }

    // CLEANUP — called in @AfterEach
    public void cleanup() {
        // Run cleanup in reverse order
        ListIterator<Runnable> it = cleanupTasks.listIterator(cleanupTasks.size());
        while (it.hasPrevious()) {
            try {
                it.previous().run();
            } catch (Exception e) {
                log.warn("Cleanup failed: {}", e.getMessage());
            }
        }
        cleanupTasks.clear();
    }
}

// TEST — using data factory
class OrderTest extends BaseTest {
    private TestDataFactory dataFactory;

    @BeforeEach
    void setUpData() {
        dataFactory = new TestDataFactory();
    }

    @AfterEach
    void cleanupData() {
        dataFactory.cleanup();
    }

    @Test
    void adminCanCancelPendingOrder() {
        // Create isolated test data — unique per test run
        UserDto admin = dataFactory.createUser("ADMIN");
        UserDto customer = dataFactory.createUser("CUSTOMER");
        OrderDto order = dataFactory.createOrder(customer.getId(), "PENDING");

        // Login as admin via cookie injection (not UI)
        String adminToken = getAuthToken(admin.getEmail(), admin.getPassword());
        injectSessionCookie(driver(), adminToken);

        // Navigate to order
        driver().get(BASE_URL + "/admin/orders/" + order.getId());

        // Test the UI interaction
        new OrderDetailsPage(driver())
            .clickCancelOrder()
            .confirmCancellation();

        assertThat(new OrderDetailsPage(driver()).getStatus()).isEqualTo("CANCELLED");
        // Cleanup happens in @AfterEach
    }
}

// BUILDER PATTERN for test data
@Builder @Data
public class UserDto {
    private String id;
    @Builder.Default private String email = "test-" + UUID.randomUUID() + "@example.com";
    @Builder.Default private String password = "Test@12345";
    @Builder.Default private String role = "CUSTOMER";
    @Builder.Default private String firstName = "Test";
    @Builder.Default private String lastName = "User";
}

// USAGE with builder
UserDto user = UserDto.builder()
    .role("ADMIN")
    .firstName("John")
    .build(); // email, password auto-generated

// FAKER for realistic test data
Faker faker = new Faker();
UserDto user2 = UserDto.builder()
    .firstName(faker.name().firstName())
    .lastName(faker.name().lastName())
    .email(faker.internet().emailAddress())
    .build();
```

### Real-world Usage
- **FinTech**: Create test account → fund it via API → verify UI shows correct balance → refund and delete in cleanup
- **E-commerce**: API creates product, category, discount — test verifies checkout flow — cleanup deletes all created entities
- **Admin portal**: Seed users with different roles via API — test role-based access with cookie injection per role
- **Multi-tenant SaaS**: Each test creates its own tenant via API — zero data sharing, no cross-test contamination

### Common Mistakes
- Using hardcoded test user `admin@test.com` shared across parallel tests — parallel tests corrupt each other's state
- Not cleaning up created data — database grows unboundedly; UNIQUE constraint violations start failing unrelated tests
- Creating test data via UI in `@BeforeEach` — 50ms API call vs 5s UI flow × 500 tests = 40 minutes wasted
- Using production-like emails (`john.smith@company.com`) for test users — risked being real users; use `test-{uuid}@example.com`
- Silent cleanup failure — cleanup throws exception and stops → subsequent cleanup tasks skipped → data leaks. Always wrap each cleanup in try/catch.

### Optimization Tip
Implement **data caching for immutable reference data**:
```java
public class TestDataCache {
    // Shared read-only data — safe to cache (never mutated)
    private static final Map<String, ProductDto> productCache = new ConcurrentHashMap<>();

    public static ProductDto getOrCreateProduct(String sku) {
        return productCache.computeIfAbsent(sku, k -> TestDataFactory.createProduct(k));
    }
}
// Mutable data (users, orders) → always fresh per test
// Immutable data (products, categories) → cache and reuse
```

### Debugging Strategy
1. Test fails with "User already exists" → cleanup from previous failed test didn't run → add `deleteUserIfExists` in setup, not just cleanup
2. Parallel tests corrupt each other → shared mutable static data → replace with per-test created data via factory
3. API setup returns 401 → admin token expired → implement token refresh or recreate admin token per test suite run
4. Cleanup runs but data still in DB → soft-delete pattern in app — API `DELETE` marks as deleted but data remains visible to other queries — use hard-delete endpoint for test environments

### Interview Trap
"What is the best way to set up test data?" — Most candidates say "create it in `@BeforeEach` via UI." The senior answer: **API-driven setup** with a data factory pattern and guaranteed cleanup. The interviewer is also listening for: "avoid shared mutable data," "unique identifiers per test," and "cleanup even when test fails (finally block / AfterEach always runs)."

### Follow-up Questions
1. How do you handle test data setup when the API doesn't exist yet (API and UI developed simultaneously)?
2. How do you manage test data in a microservices architecture where data spans multiple services?

### Selenium vs Playwright
Playwright's `globalSetup` / `globalTeardown` + `storageState` simplifies auth setup, but test data management is a framework concern regardless of the automation tool used. Both Selenium and Playwright teams face identical test data challenges.

---

## Q30: How do you implement cross-browser testing and manage browser compatibility?

### Interview Answer
Cross-browser testing uses the **same test code** against multiple browsers via browser-specific `Options` classes and a **parameterized `DriverFactory`**. Run via TestNG `@Parameters` or JUnit 5 `@MethodSource` across Chrome, Firefox, and Safari/Edge. On Selenium Grid or cloud providers (BrowserStack/Sauce Labs), the browser is declared in capabilities — the test code is identical.

### Deep Explanation
**Browser compatibility concerns:**
- **CSS rendering differences**: Safari uses WebKit, Chrome/Edge use Blink, Firefox uses Gecko — fonts, box model edge cases
- **JavaScript engine differences**: V8 (Chrome), SpiderMonkey (Firefox), JavaScriptCore (Safari) — rare but real edge cases
- **WebDriver behavior differences**: Some `ExpectedConditions` behave subtly differently across drivers
- **Capability namespacing**: W3C requires `goog:` prefix for Chrome, `moz:` for Firefox, `ms:` for Edge
- **Safari WebDriver (safaridriver)**: Only runs on macOS, no headless, very strict W3C compliance

**Browser-specific quirks to handle:**
```
Chrome: most permissive, best DevTools support, --headless=new
Firefox: strictest event simulation, better JS spec compliance, -headless
Safari: macOS only, no headless, requires "Allow Remote Automation" in developer menu
Edge: Chromium-based, behaves like Chrome, use EdgeDriver/EdgeOptions
```

### Java Example
```java
// DRIVER FACTORY — multi-browser support
public class DriverFactory {
    private static final ThreadLocal<WebDriver> driverPool = new ThreadLocal<>();

    public static void initDriver(String browser, String gridUrl) {
        WebDriver driver;
        if (gridUrl != null && !gridUrl.isEmpty()) {
            driver = createRemoteDriver(browser, gridUrl);
        } else {
            driver = createLocalDriver(browser);
        }
        driver.manage().window().maximize();
        driverPool.set(driver);
    }

    private static WebDriver createLocalDriver(String browser) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> new ChromeDriver(chromeOptions());
            case "firefox" -> new FirefoxDriver(firefoxOptions());
            case "edge" -> new EdgeDriver(edgeOptions());
            default -> throw new IllegalArgumentException("Unsupported: " + browser);
        };
    }

    private static WebDriver createRemoteDriver(String browser, String gridUrl) {
        try {
            MutableCapabilities caps = switch (browser.toLowerCase()) {
                case "chrome"   -> chromeOptions();
                case "firefox"  -> firefoxOptions();
                case "edge"     -> edgeOptions();
                case "safari"   -> safariOptions();
                default -> throw new IllegalArgumentException("Unsupported: " + browser);
            };
            return new RemoteWebDriver(new URL(gridUrl), caps);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Grid URL: " + gridUrl, e);
        }
    }

    private static ChromeOptions chromeOptions() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new", "--window-size=1920,1080",
                          "--no-sandbox", "--disable-dev-shm-usage");
        opts.setCapability("goog:loggingPrefs",
            Map.of(LogType.BROWSER, Level.ALL, LogType.DRIVER, Level.WARNING));
        return opts;
    }

    private static FirefoxOptions firefoxOptions() {
        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("-headless", "--width=1920", "--height=1080");
        opts.setCapability("moz:firefoxOptions",
            Map.of("log", Map.of("level", "warn")));
        return opts;
    }

    private static EdgeOptions edgeOptions() {
        EdgeOptions opts = new EdgeOptions();
        opts.addArguments("--headless=new", "--window-size=1920,1080",
                          "--no-sandbox");
        return opts;
    }

    private static SafariOptions safariOptions() {
        SafariOptions opts = new SafariOptions();
        // Safari: no headless, runs only on macOS
        opts.setCapability("safari:automaticInspection", false);
        return opts;
    }
}

// JUNIT 5 — Parameterized cross-browser test
@ParameterizedTest(name = "Browser: {0}")
@MethodSource("browserProvider")
void loginWorksOnAllBrowsers(String browser) {
    DriverFactory.initDriver(browser, System.getProperty("gridUrl"));
    try {
        DashboardPage dashboard = new LoginPage(DriverFactory.getDriver())
            .loginAs("user@test.com", "password");
        assertThat(dashboard.getWelcomeMessage()).contains("Welcome");
    } finally {
        DriverFactory.quitDriver();
    }
}

static Stream<String> browserProvider() {
    String browsers = System.getProperty("browsers", "chrome,firefox");
    return Arrays.stream(browsers.split(","));
}

// TESTNG — cross-browser via XML parameters
/*
<suite name="CrossBrowser" parallel="tests" thread-count="3">
  <test name="Chrome">
    <parameter name="browser" value="chrome"/>
    <classes><class name="com.example.LoginTest"/></classes>
  </test>
  <test name="Firefox">
    <parameter name="browser" value="firefox"/>
    <classes><class name="com.example.LoginTest"/></classes>
  </test>
  <test name="Edge">
    <parameter name="browser" value="edge"/>
    <classes><class name="com.example.LoginTest"/></classes>
  </test>
</suite>
*/

// BROWSERSTACK — cloud cross-browser
public WebDriver createBrowserStackDriver(String browser, String version, String os) {
    MutableCapabilities caps = new MutableCapabilities();
    HashMap<String, Object> bsOptions = new HashMap<>();
    bsOptions.put("os", os);
    bsOptions.put("osVersion", "11");
    bsOptions.put("browserName", browser);
    bsOptions.put("browserVersion", version);
    bsOptions.put("userName", System.getenv("BROWSERSTACK_USERNAME"));
    bsOptions.put("accessKey", System.getenv("BROWSERSTACK_ACCESS_KEY"));
    bsOptions.put("projectName", "My App Regression");
    bsOptions.put("buildName", "Build-" + System.getProperty("buildNumber"));
    bsOptions.put("sessionName", "CrossBrowser-" + browser + "-" + version);
    caps.setCapability("bstack:options", bsOptions);

    try {
        return new RemoteWebDriver(
            new URL("https://hub.browserstack.com/wd/hub"), caps);
    } catch (MalformedURLException e) {
        throw new RuntimeException(e);
    }
}
```

### Real-world Usage
- **B2C product companies**: Chrome (65% users) + Safari (20%) + Firefox (10%) + Edge (5%) — weighted coverage
- **Enterprise SaaS**: IE11 dropped, but Edge + Chrome critical — often Chrome + Firefox + Edge matrix
- **FinTech with corporate clients**: Corporate clients use Edge on Windows — must include Edge in matrix
- **Cross-browser smoke gate**: Run 20 smoke tests × 3 browsers on merge to main — catch CSS/JS regressions before full regression

### Common Mistakes
- Running all 500 tests across all browsers — cross-browser suite should be a curated subset (50–100 tests covering critical paths)
- Not accounting for Safari's no-headless restriction — Safari tests must run on macOS agents only
- Using `DesiredCapabilities` (Selenium 3 style) for W3C browsers — some capabilities silently ignored
- Treating all browsers as Chrome — Firefox has stricter event simulation; some JS behaviors differ
- Not setting explicit window size for each browser — default viewport differs by browser and OS

### Optimization Tip
Implement **browser-specific wait adjustments**:
```java
public int getDefaultTimeout() {
    String browser = System.getProperty("browser", "chrome");
    return switch (browser) {
        case "safari" -> 20;   // Safari is slower on all DOM operations
        case "firefox" -> 15;
        default -> 10;
    };
}
```
Safari consistently needs longer timeouts due to slower JavaScript execution and DOM operations.

### Debugging Strategy
1. Test passes Chrome, fails Firefox → JS event difference — Firefox requires `fireEvent` on some inputs; check for `change` event dispatch
2. Test passes Chrome, fails Safari → Check: strict W3C compliance issue, CSS layout difference (screenshot), `safaridriver` version
3. All browsers fail on Grid → Grid node running wrong browser version — check node capabilities at `http://grid/status`
4. `WebDriverException: unknown command` on specific browser → capability not supported — check W3C spec for that browser
5. Take comparative screenshots per browser and diff them — `ImageMagick compare` in CI pipeline for visual regression

### Interview Trap
"How do you decide which browsers to test?" — This is a business question, not a technical one. Senior answer: **check analytics** — look at the real user browser distribution from Google Analytics/Mixpanel. Test browsers that cover 95% of your actual user base. Safari on Windows users = 0.1% — don't test it. Edge on enterprise Windows = 30% of corporate clients — always test it. Data-driven browser selection, not "we test all browsers."

### Follow-up Questions
1. How do you handle visual regression across browsers where pixel-perfect layout differences are expected?
2. When would you use a local Selenium Grid vs a cloud provider like BrowserStack for cross-browser testing?

### Selenium vs Playwright
Playwright supports Chromium, Firefox, and WebKit (Safari engine) out of the box — `npx playwright install` downloads all three. `test.use({browserName: 'webkit'})` isolates Safari tests. Playwright's cross-browser model is first-class; Selenium's requires managing separate driver binaries and options per browser.

---

## Q31: How do you manage WebDriver versioning and browser-driver compatibility?

### Interview Answer
Browser-driver version mismatches cause `SessionNotCreatedException` — the most common CI breakage when Chrome auto-updates. Solutions: **WebDriverManager** (automated version resolution), **pinned browser versions in Docker**, or **Selenium Manager** (built into Selenium 4.6+). Never manually manage driver binaries in a production framework.

### Deep Explanation
**The version problem:**
- ChromeDriver must exactly match the Chrome major version: Chrome 124 → ChromeDriver 124.x.x
- Chrome auto-updates silently on most systems, including CI
- GeckoDriver / Firefox are more loosely coupled but still version-sensitive
- EdgeDriver ships with the Edge browser — but version must still match

**Three solutions:**

1. **Selenium Manager** (Selenium 4.6+ built-in): Automatically downloads the correct driver binary for the detected browser version. Zero configuration. Runs at WebDriver instantiation if no driver is on PATH.

2. **WebDriverManager** (Bonigarcia library): Pre-Selenium 4.6 standard. Detects browser version, downloads correct driver, adds to PATH. Still valid for older setups.

3. **Docker with pinned browser image**: `selenium/standalone-chrome:124.0` — pin exact browser + driver version. Gold standard for CI reproducibility.

**Version resolution flow (Selenium Manager):**
```
new ChromeDriver() →
  Selenium Manager checks: Is chromedriver on PATH? →
  No → detect installed Chrome version →
  Download matching chromedriver from Chrome for Testing repo →
  Cache in ~/.cache/selenium/ →
  Use cached driver next run
```

### Java Example
```java
// SELENIUM 4.6+ — Selenium Manager (ZERO configuration needed)
// Just instantiate the driver — Selenium Manager handles everything
WebDriver driver = new ChromeDriver(); // automatically resolves driver version
WebDriver firefox = new FirefoxDriver(); // same for Firefox

// WEBDRIVERMANAGER — explicit setup (Selenium < 4.6 or more control needed)
// pom.xml: io.github.bonigarcia:webdrivermanager:5.8.0
WebDriverManager.chromedriver().setup();
WebDriver driver2 = new ChromeDriver();

// Pin specific version (useful for reproducible CI)
WebDriverManager.chromedriver().driverVersion("124.0.6367.60").setup();

// Detect browser version and resolve
WebDriverManager.chromedriver().browserVersion("124").setup();

// FIREFOX
WebDriverManager.firefoxdriver().setup();
WebDriver firefox2 = new FirefoxDriver();

// EDGE
WebDriverManager.edgedriver().setup();
WebDriver edge = new EdgeDriver();

// DRIVER FACTORY — with version management
public class DriverFactory {
    static {
        // One-time setup — resolves all drivers at class loading time
        // Only needed if not using Selenium 4.6+ Selenium Manager
        if (!isSeleniumManagerAvailable()) {
            WebDriverManager.chromedriver().setup();
            WebDriverManager.firefoxdriver().setup();
            WebDriverManager.edgedriver().setup();
        }
    }

    private static boolean isSeleniumManagerAvailable() {
        // Selenium 4.6+ has manager built in
        String version = SeleniumManager.class.getPackage().getImplementationVersion();
        return version != null;
    }
}

// DOCKER — pinned browser version (docker-compose.yml)
/*
services:
  chrome:
    image: selenium/standalone-chrome:4.20.0-20240425  # pinned exact version
    ports: ["4444:4444"]
    shm_size: 2gb
    environment:
      - SE_NODE_MAX_SESSIONS=4
      - SE_NODE_SESSION_TIMEOUT=300

  firefox:
    image: selenium/standalone-firefox:4.20.0-20240425
    ports: ["4445:4444"]
*/

// CONNECTING TO DOCKER GRID
WebDriver driver3 = new RemoteWebDriver(
    new URL("http://localhost:4444"),
    new ChromeOptions()
);

// GITHUB ACTIONS — pin Chrome version
/*
- name: Install specific Chrome version
  uses: browser-actions/setup-chrome@latest
  with:
    chrome-version: '124'   # pin major version

- name: Verify Chrome version
  run: google-chrome --version
*/

// WEBDRIVERMANAGER — offline cache for air-gapped CI
WebDriverManager.chromedriver()
    .cachePath("/ci-tools/webdriver-cache")  // pre-populated cache
    .avoidExternalConnections()              // don't download — use cache only
    .setup();
```

### Real-world Usage
- **Enterprise CI (air-gapped network)**: WebDriverManager with pre-populated offline cache — no internet access from CI agents
- **Kubernetes pods**: Docker image `selenium/standalone-chrome:124.0` pinned — guaranteed reproducibility across 100 pods
- **Local developer setup**: Selenium Manager auto-resolves — developer never thinks about version; `new ChromeDriver()` just works
- **Version mismatch alert in CI**: `SessionNotCreatedException` with message "This version of ChromeDriver only supports Chrome version 123" — signals version drift

### Common Mistakes
- Committing ChromeDriver binary to git — binaries are OS-specific, large, and stale within weeks
- Using `latest` Docker tag for selenium images — Chrome updates break tests unpredictably
- Not caching WebDriverManager download in CI — re-downloads driver on every CI run (100+ MB, adds 30s to pipeline)
- Calling `WebDriverManager.chromedriver().setup()` inside `@BeforeEach` — called 500 times per suite; call in `@BeforeSuite` or static initializer

### Optimization Tip
Cache WebDriverManager's driver download directory in CI:
```yaml
# GitHub Actions cache
- name: Cache WebDriverManager
  uses: actions/cache@v4
  with:
    path: ~/.cache/selenium
    key: webdriver-${{ runner.os }}-chrome-${{ steps.chrome.outputs.chrome-version }}
```
This converts a 30-second download to a 1-second cache restore per CI run.

### Debugging Strategy
1. `SessionNotCreatedException: This version of ChromeDriver only supports Chrome version X` → version mismatch — use Selenium Manager or WebDriverManager to auto-resolve
2. WebDriverManager downloads wrong version → explicitly set `browserVersion()` or `driverVersion()`
3. Works locally, fails in CI → different Chrome versions — pin Chrome version in CI action or use Docker
4. Air-gapped CI fails to download driver → use `avoidExternalConnections().cachePath()` with pre-populated cache

### Interview Trap
"What do you do when your CI tests suddenly all fail with `SessionNotCreatedException` after a Chrome update?" — the senior answer: implement **Selenium Manager or WebDriverManager for automatic resolution** and **pin Chrome version in Docker** for CI. Also: "how do you prevent this from happening again?" — Docker pinned images + automated dependency bot (Dependabot/Renovate) for image version updates.

### Follow-up Questions
1. How does Selenium Manager differ from WebDriverManager in terms of how it resolves driver versions?
2. How would you set up a completely air-gapped CI environment for Selenium tests with no internet access?

### Selenium vs Playwright
Playwright eliminates this problem entirely — `npx playwright install` downloads the exact browser binaries built and tested against that Playwright version. Browser and driver are a matched pair shipped together. Version mismatch is structurally impossible in Playwright.

---

## Q32: How do you test browser storage — localStorage, sessionStorage, and IndexedDB?

### Interview Answer
Browser storage (localStorage, sessionStorage) is accessible only via **`JavascriptExecutor`** in Selenium — no native WebDriver API exists for it. For IndexedDB, use JS async queries. These are primarily needed for: **verifying SPA state after actions**, **injecting auth tokens**, **clearing stored data for clean test state**, and **testing storage quota limits**.

### Deep Explanation
**Storage types and test relevance:**

| Storage | Persistence | Scope | Test Use Case |
|---|---|---|---|
| `localStorage` | Until cleared | Origin (domain) | JWT tokens, user prefs, feature flags |
| `sessionStorage` | Tab lifetime | Tab + origin | Cart state, form draft, wizard steps |
| `IndexedDB` | Until cleared | Origin | Large data, offline apps, PWA data |
| `Cookies` | Configurable | Domain + path | Session tokens, tracking consent |

**When you need storage testing:**
- SPAs store auth in `localStorage` (JWT) — inject token instead of UI login
- Shopping cart stored in `sessionStorage` — verify items persist on page refresh within same tab
- PWA offline mode uses IndexedDB — verify sync when connection restores
- Feature flag override in `localStorage` — `localStorage.setItem('featureFlags', '{"newUI":true}')` before test

### Java Example
```java
// STORAGE UTILITY — production helper class
public class BrowserStorageUtils {
    private final JavascriptExecutor js;

    public BrowserStorageUtils(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    // ============ LOCAL STORAGE ============
    public void setLocalStorage(String key, String value) {
        js.executeScript("window.localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    public String getLocalStorage(String key) {
        return (String) js.executeScript(
            "return window.localStorage.getItem(arguments[0]);", key);
    }

    public void removeLocalStorage(String key) {
        js.executeScript("window.localStorage.removeItem(arguments[0]);", key);
    }

    public void clearLocalStorage() {
        js.executeScript("window.localStorage.clear();");
    }

    public Map<String, String> getAllLocalStorage() {
        String script =
            "var items = {};" +
            "for (var i = 0; i < localStorage.length; i++) {" +
            "  var key = localStorage.key(i);" +
            "  items[key] = localStorage.getItem(key);" +
            "}" +
            "return items;";
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) js.executeScript(script);
        return result;
    }

    // ============ SESSION STORAGE ============
    public void setSessionStorage(String key, String value) {
        js.executeScript("window.sessionStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    public String getSessionStorage(String key) {
        return (String) js.executeScript(
            "return window.sessionStorage.getItem(arguments[0]);", key);
    }

    public void clearSessionStorage() {
        js.executeScript("window.sessionStorage.clear();");
    }

    // ============ INDEXEDDB ============
    // IndexedDB is async — use executeAsyncScript
    public String getIndexedDbValue(String dbName, String storeName, String key) {
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
        return (String) ((JavascriptExecutor) driver).executeAsyncScript(
            "var callback = arguments[arguments.length - 1];" +
            "var request = indexedDB.open('" + dbName + "');" +
            "request.onsuccess = function(e) {" +
            "  var db = e.target.result;" +
            "  var tx = db.transaction('" + storeName + "', 'readonly');" +
            "  var store = tx.objectStore('" + storeName + "');" +
            "  var getReq = store.get('" + key + "');" +
            "  getReq.onsuccess = function(e) { callback(JSON.stringify(e.target.result)); };" +
            "  getReq.onerror = function() { callback(null); };" +
            "};" +
            "request.onerror = function() { callback(null); };"
        );
    }

    public void clearIndexedDb(String dbName) {
        js.executeScript(
            "indexedDB.deleteDatabase('" + dbName + "');"
        );
    }

    // ============ INJECT JWT (SPA auth bypass) ============
    public void injectJwtToken(String token) {
        setLocalStorage("auth_token", token);
        // Also set in memory if app reads from window object
        js.executeScript("window.__authToken = arguments[0];", token);
        // Dispatch storage event so app reacts (for cross-tab listeners)
        js.executeScript(
            "window.dispatchEvent(new StorageEvent('storage', {" +
            "  key: 'auth_token', newValue: arguments[0], storageArea: localStorage" +
            "}));", token);
    }

    // ============ VERIFY STORAGE AFTER ACTION ============
    // Test that adding to cart persists in sessionStorage
    public void assertSessionStorageContains(String key, String expectedValue) {
        String actual = getSessionStorage(key);
        assertThat(actual)
            .as("sessionStorage[%s]", key)
            .isNotNull()
            .contains(expectedValue);
    }
}

// USAGE IN TEST
class StorageTest extends BaseTest {
    private BrowserStorageUtils storage;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        storage = new BrowserStorageUtils(driver());
        driver().get(BASE_URL); // navigate to domain first
    }

    @Test
    void jwtInjectionBypassesLogin() {
        String jwt = JwtFactory.createTestToken("user@test.com", List.of("CUSTOMER"));
        storage.injectJwtToken(jwt);
        driver().navigate().to(BASE_URL + "/dashboard");
        assertThat(new DashboardPage(driver()).getWelcomeMessage()).contains("Welcome");
    }

    @Test
    void cartItemsPersistedInSessionStorage() {
        new ProductPage(driver()).addToCart("SKU-001");
        String cartJson = storage.getSessionStorage("cart");
        assertThat(cartJson).contains("SKU-001");
    }

    @Test
    void userPreferencesStoredAfterSettings() {
        new SettingsPage(driver()).setTheme("dark");
        assertThat(storage.getLocalStorage("theme")).isEqualTo("dark");
    }

    @AfterEach
    void cleanStorage() {
        storage.clearLocalStorage();
        storage.clearSessionStorage();
    }
}
```

### Real-world Usage
- **SPA auth (React/Angular/Vue)**: Inject JWT into `localStorage` → bypass login UI → immediately test authenticated pages
- **Wizard/stepper forms**: Verify `sessionStorage` contains each step's data as the user progresses
- **PWA offline mode**: Set IndexedDB data, disconnect network (CDP `Network.emulateNetworkConditions`), verify app reads from IndexedDB correctly
- **Feature flags via localStorage**: Override `localStorage.setItem('featureFlags', '{"betaCheckout":true}')` → test beta feature without backend config
- **Clean state**: `clearLocalStorage()` + `clearSessionStorage()` in `@AfterEach` ensures no storage bleed between tests

### Common Mistakes
- Reading `localStorage` before navigating to the domain — returns `null` or throws `SecurityError` (cross-origin)
- Not dispatching the `storage` event after setting a value — SPA may not react to programmatic changes unless the event fires
- Using `executeScript` for IndexedDB — IndexedDB is async; requires `executeAsyncScript` with callback pattern
- Not clearing storage between tests — stored JWT tokens, cart state, or feature flags from one test affect the next

### Optimization Tip
Build a **storage-based login shortcut** for your entire test suite — replaces UI login with a 3-line token injection:
```java
public void loginViaToken(String role) {
    driver().get(BASE_URL); // must be on domain
    String token = TokenCache.getOrCreate(role);
    new BrowserStorageUtils(driver()).injectJwtToken(token);
    driver().get(BASE_URL + "/dashboard"); // navigate to authenticated area
}
```
For 500 tests × 3-second UI login saved = **25 minutes per suite run**.

### Debugging Strategy
1. `localStorage.getItem()` returns `null` in test but app reads it fine → test reads different origin (http vs https, port mismatch) — ensure test navigates to exact same origin as app
2. JWT injection doesn't authenticate → app reads token from `httpOnly` cookie, not `localStorage` — switch to cookie injection instead
3. `StorageEvent` not triggering app listener → app uses `window.addEventListener('storage')` which only fires for changes from OTHER tabs — use direct JS notification instead

### Interview Trap
"Why can't Selenium's WebDriver API read localStorage directly?" — Because `localStorage` is a browser API accessible only within the **same-origin JavaScript context**, not via HTTP commands. The W3C WebDriver spec doesn't include storage APIs. `JavascriptExecutor` bridges this by executing code within the page's JS context. This reveals understanding of **browser security model** and **same-origin policy**.

### Follow-up Questions
1. How do you test a Progressive Web App that uses IndexedDB for offline data sync?
2. What is the storage event and when does it fire — and why does programmatic `setItem` NOT trigger it across tabs?

### Selenium vs Playwright
Playwright provides `browserContext.storageState()` which captures all storage (cookies + localStorage + sessionStorage) as one JSON snapshot. `page.evaluate(() => localStorage.getItem('key'))` is cleaner than Selenium's `executeScript`. For IndexedDB, both use JS similarly.

---

## Q33: How do you handle performance testing considerations in Selenium automation?

### Interview Answer
Selenium is **not a load testing tool** — it tests functional correctness, not throughput or concurrency. However, Selenium can collect **page-level performance metrics** via Chrome DevTools Protocol: navigation timing, resource timing, LCP, FCP, TTI. Use these as **performance regression gates** in your test suite — fail if a critical page loads slower than a threshold.

### Deep Explanation
**What Selenium CAN measure:**
- Navigation Timing API (`performance.timing`) — `domContentLoaded`, `loadEventEnd`, `responseStart`
- Resource Timing API (`performance.getEntriesByType('resource')`) — individual asset load times
- Web Vitals via CDP: LCP (Largest Contentful Paint), FCP (First Contentful Paint), CLS
- Time to interactive via CDP Performance domain
- Memory usage (`window.performance.memory`)

**What Selenium CANNOT do:**
- Concurrent user simulation (use JMeter, Gatling, k6)
- Backend throughput testing
- True end-to-end latency under load

**Performance testing layers:**
```
Load/Stress       → JMeter, Gatling, k6 (concurrent users)
API Performance   → REST Assured + response time assertions
Browser Perf      → Selenium + CDP (single user, real browser metrics)
Visual Perf       → Lighthouse CI (scores, Web Vitals)
```

### Java Example
```java
// PERFORMANCE METRICS COLLECTION via CDP
public class PerformanceCollector {
    private final WebDriver driver;
    private final JavascriptExecutor js;

    public PerformanceCollector(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
    }

    // NAVIGATION TIMING — complete page load metrics
    public NavigationTiming getNavigationTiming() {
        Map<String, Object> timing = (Map<String, Object>) js.executeScript(
            "var t = performance.timing;" +
            "return {" +
            "  dnsLookup:       t.domainLookupEnd - t.domainLookupStart," +
            "  tcpConnect:      t.connectEnd - t.connectStart," +
            "  ttfb:            t.responseStart - t.requestStart," +
            "  download:        t.responseEnd - t.responseStart," +
            "  domInteractive:  t.domInteractive - t.navigationStart," +
            "  domContentLoaded:t.domContentLoadedEventEnd - t.navigationStart," +
            "  pageLoad:        t.loadEventEnd - t.navigationStart" +
            "};"
        );
        return new NavigationTiming(timing);
    }

    // WEB VITALS — LCP, FCP, CLS via PerformanceObserver
    public WebVitals getWebVitals() {
        // Inject PerformanceObserver before navigation
        js.executeScript(
            "window.__webVitals = {};" +
            "new PerformanceObserver((list) => {" +
            "  list.getEntries().forEach(e => {" +
            "    if (e.entryType === 'largest-contentful-paint') window.__webVitals.lcp = e.startTime;" +
            "    if (e.entryType === 'first-contentful-paint') window.__webVitals.fcp = e.startTime;" +
            "  });" +
            "}).observe({type:'largest-contentful-paint', buffered:true});" +
            "new PerformanceObserver((list) => {" +
            "  list.getEntries().forEach(e => window.__webVitals.fcp = e.startTime);" +
            "}).observe({type:'paint', buffered:true});"
        );

        // Wait for LCP to stabilize
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(d -> js.executeScript("return window.__webVitals.lcp != null") != null);

        Map<String, Object> vitals = (Map<String, Object>) js.executeScript(
            "return window.__webVitals;");
        return new WebVitals(vitals);
    }

    // CDP PERFORMANCE METRICS
    public Map<String, Double> getCdpMetrics(DevTools devTools) {
        devTools.send(Performance.enable(Optional.empty()));
        List<Metric> metrics = devTools.send(Performance.getMetrics());
        return metrics.stream().collect(Collectors.toMap(
            Metric::getName, m -> m.getValue().doubleValue()));
    }

    // RESOURCE TIMING — find slow resources
    public List<ResourceTiming> getSlowResources(long thresholdMs) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) js.executeScript(
            "return performance.getEntriesByType('resource')" +
            "  .filter(r => r.duration > arguments[0])" +
            "  .map(r => ({name: r.name, duration: r.duration, type: r.initiatorType}));",
            thresholdMs
        );
        return entries.stream()
            .map(e -> new ResourceTiming(
                (String) e.get("name"),
                ((Number) e.get("duration")).longValue(),
                (String) e.get("type")))
            .collect(Collectors.toList());
    }

    // MEMORY USAGE (Chrome only)
    public MemoryInfo getMemoryUsage() {
        Map<String, Object> memory = (Map<String, Object>) js.executeScript(
            "return performance.memory ? {" +
            "  usedJsHeap: performance.memory.usedJSHeapSize," +
            "  totalJsHeap: performance.memory.totalJSHeapSize," +
            "  heapLimit: performance.memory.jsHeapSizeLimit" +
            "} : null;");
        return memory != null ? new MemoryInfo(memory) : null;
    }
}

// PERFORMANCE ASSERTIONS IN TEST
@Test
void dashboardLoadsWithinPerformanceBudget() {
    PerformanceCollector perf = new PerformanceCollector(driver());
    driver().get(BASE_URL + "/dashboard");

    NavigationTiming timing = perf.getNavigationTiming();

    // Performance budget assertions
    assertThat(timing.getPageLoad())
        .as("Page load time must be under 3 seconds")
        .isLessThan(3000L);
    assertThat(timing.getTtfb())
        .as("Time to first byte must be under 500ms")
        .isLessThan(500L);
    assertThat(timing.getDomContentLoaded())
        .as("DOM ready must be under 1.5 seconds")
        .isLessThan(1500L);

    // No slow resources
    List<ResourceTiming> slowResources = perf.getSlowResources(2000);
    assertThat(slowResources)
        .as("Found resources taking > 2s: " + slowResources)
        .isEmpty();
}

// MEMORY REGRESSION TEST — detect leaks over navigation
@Test
void noMemoryLeakAfterRepeatedNavigation() {
    PerformanceCollector perf = new PerformanceCollector(driver());
    driver().get(BASE_URL + "/reports");

    long baselineMemory = perf.getMemoryUsage().getUsedJsHeap();

    // Navigate back and forth 10 times
    for (int i = 0; i < 10; i++) {
        driver().navigate().to(BASE_URL + "/dashboard");
        driver().navigate().to(BASE_URL + "/reports");
    }

    long finalMemory = perf.getMemoryUsage().getUsedJsHeap();
    long growth = finalMemory - baselineMemory;

    // Memory growth > 50MB after 10 navigations = potential leak
    assertThat(growth)
        .as("Memory grew by " + growth / 1_000_000 + "MB — potential leak")
        .isLessThan(50 * 1_000_000L);
}
```

### Real-world Usage
- **Performance regression gate in CI**: Dashboard must load < 3s — gating metric, fails pipeline if exceeded
- **Memory leak detection**: Run 50-page navigation loop — measure heap growth — flag if > 100MB
- **Third-party script impact**: Compare load time with/without analytics scripts blocked via CDP
- **FinTech trading dashboards**: Measure time until real-time price data renders on screen (custom `PerformanceObserver` mark)
- **API response time baselines**: Capture `responseStart - requestStart` for each API call — regression alert in CI

### Common Mistakes
- Using Selenium for load testing — it can't simulate concurrent users; use JMeter/k6 for that
- Asserting on absolute timing values without environment awareness — CI machines are slower; use relative thresholds or separate perf baselines per environment
- Not injecting `PerformanceObserver` BEFORE navigation — LCP/FCP events fire during load; injecting after navigation misses them
- Measuring wall clock time with `System.currentTimeMillis()` instead of browser `performance.timing` — captures Selenium overhead, not actual browser performance

### Optimization Tip
Integrate **Lighthouse CI** alongside Selenium for comprehensive performance scoring:
```yaml
# GitHub Actions — run Lighthouse after Selenium tests deploy
- name: Lighthouse CI
  uses: treosh/lighthouse-ci-action@v11
  with:
    urls: 'https://staging.example.com/dashboard'
    budgetPath: '.lighthouserc.json'
    uploadArtifacts: true
```
Selenium tests functional correctness; Lighthouse measures performance scores. Both together = complete quality gate.

### Debugging Strategy
1. `performance.timing` all zeros → page wasn't fully loaded when metric was captured — wait for `document.readyState === 'complete'` first
2. LCP value not captured → `PerformanceObserver` was injected after page loaded — inject before navigation
3. TTFB fluctuates wildly in CI → CI network latency; run perf tests at fixed times or use relative change detection (> 20% regression) rather than fixed thresholds
4. Memory values not returning → `performance.memory` is Chrome-only, non-standard — check browser before calling

### Interview Trap
"How do you do performance testing with Selenium?" — The wrong answer: "I use Selenium to simulate 100 concurrent users." The right answer: **Selenium measures single-user browser performance metrics** (navigation timing, Web Vitals). Concurrent load testing is JMeter/k6/Gatling. The interviewer is testing whether you know the **scope and limits** of Selenium as a tool.

### Follow-up Questions
1. What is the difference between TTFB, FCP, LCP, and TTI — and what does each indicate about the user experience?
2. How would you implement a performance budget that automatically fails the CI build if page load time regresses by > 20%?

### Selenium vs Playwright
Playwright has `page.metrics()` and built-in `--trace` recording that captures performance spans. Its `browserContext.tracing.start()` records a full execution trace with screenshots, network, and performance data viewable in Playwright Trace Viewer — more comprehensive than manual CDP metric collection.

---

## Q34: How do you handle thread safety in Selenium — what can and can't be shared across threads?

### Interview Answer
In parallel Selenium execution, **`WebDriver` instances must never be shared** — each thread needs its own instance via `ThreadLocal`. Objects that ARE safe to share: read-only config, stateless utilities, test data factories using thread-safe collections. Objects that are NEVER safe to share: `WebDriver`, `WebElement`, `Actions`, `DevTools` sessions, page objects holding driver references.

### Deep Explanation
**Thread safety taxonomy:**

| Object | Thread-safe? | Reason |
|---|---|---|
| `WebDriver` instance | ❌ No | Session ID not reentrant; HTTP requests interleave |
| `WebElement` | ❌ No | Element handle tied to specific session |
| `Actions` | ❌ No | Stateful chain builder |
| `DevTools` | ❌ No | WebSocket session per driver |
| `By` (locator) | ✅ Yes | Immutable value object |
| `Config` (read-only) | ✅ Yes | No mutation |
| `ThreadLocal<WebDriver>` | ✅ Yes | Isolated per thread |
| `ConcurrentHashMap` | ✅ Yes | Thread-safe collection |
| `AtomicInteger` (counters) | ✅ Yes | Atomic operations |
| Page Objects (with `ThreadLocal` driver) | ✅ Yes | If driver accessed via `ThreadLocal` |

**Java Memory Model concern:**
- `static` fields are shared across all threads — reading a `static WebDriver` causes data races
- `static final` fields are safe (immutable after publication)
- Instance fields are safe only when the instance is not shared across threads

### Java Example
```java
// ❌ WRONG — static WebDriver, shared across all threads
public class BadTest {
    static WebDriver driver = new ChromeDriver(); // RACE CONDITION

    @Test
    void test1() { driver.get("http://page1.com"); }  // Thread-1

    @Test
    void test2() { driver.get("http://page2.com"); }  // Thread-2 — corrupts Thread-1's session
}

// ✅ CORRECT — ThreadLocal per thread
public class BaseTest {
    private static final ThreadLocal<WebDriver> driverPool = new ThreadLocal<>();

    @BeforeEach
    void setUp() {
        driverPool.set(new ChromeDriver(headlessOptions()));
    }

    @AfterEach
    void tearDown() {
        driverPool.get().quit();
        driverPool.remove(); // prevent memory leak
    }

    protected WebDriver driver() {
        return Objects.requireNonNull(driverPool.get(),
            "Driver not initialized for thread: " + Thread.currentThread().getName());
    }
}

// THREAD-SAFE SHARED OBJECTS — OK to share
public class SharedTestInfrastructure {
    // ✅ Immutable config — safe to share
    public static final String BASE_URL = System.getProperty("baseUrl");
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    // ✅ Thread-safe counter — test execution stats
    public static final AtomicInteger testCount = new AtomicInteger(0);
    public static final AtomicInteger failCount = new AtomicInteger(0);

    // ✅ Thread-safe result accumulation
    public static final ConcurrentHashMap<String, String> testResults = new ConcurrentHashMap<>();

    // ✅ Thread-safe session cache (read-mostly)
    private static final ConcurrentHashMap<String, String> authTokenCache
        = new ConcurrentHashMap<>();

    public static String getAuthToken(String role) {
        return authTokenCache.computeIfAbsent(role,
            r -> ApiClient.login(r).getToken()); // atomic create-if-absent
    }
}

// PAGE OBJECTS — thread-safe design
public class LoginPage {
    // ✅ Locators as static final — immutable, safe to share
    private static final By EMAIL    = By.cssSelector("[data-testid='email']");
    private static final By PASSWORD = By.cssSelector("[data-testid='password']");

    // ✅ Driver obtained from ThreadLocal, not stored as field with `= driver`
    private final WebDriver driver; // set in constructor from caller's ThreadLocal

    public LoginPage(WebDriver driver) {
        this.driver = driver; // each Thread passes its OWN driver instance
    }

    public DashboardPage loginAs(String email, String password) {
        // driver.findElement() uses THIS thread's driver session
        driver.findElement(EMAIL).sendKeys(email);
        driver.findElement(PASSWORD).sendKeys(password);
        driver.findElement(By.cssSelector("[data-testid='submit']")).click();
        return new DashboardPage(driver);
    }
}

// DETECTING SHARED-STATE BUGS — thread dump analysis
// In IntelliJ: Run → "Attach to Process" → Threads view
// In CI: kill -3 <pid> → prints Java thread dump
// Look for: multiple threads in the same WebDriver HTTP request

// TESTING FOR THREAD SAFETY — intentional stress test
@Test
@Execution(ExecutionMode.CONCURRENT)
void parallelStressTest() throws InterruptedException {
    // Each parallel invocation gets its own driver via ThreadLocal
    String threadId = Thread.currentThread().getName();
    DriverFactory.initDriver("chrome");
    WebDriver driver = DriverFactory.getDriver();

    // If any thread uses another thread's driver, this will throw
    driver.get(BASE_URL + "/dashboard?thread=" + threadId);
    String pageContent = driver.getPageSource();
    assertThat(pageContent).contains(threadId);

    DriverFactory.quitDriver();
}
```

### Real-world Usage
- **50-thread parallel Grid run**: `ThreadLocal` driver per thread — 50 isolated `RemoteWebDriver` sessions on Grid, zero interference
- **Shared auth token cache**: `ConcurrentHashMap.computeIfAbsent()` for login — all threads for role "ADMIN" share one token, created once
- **Parallel test counter**: `AtomicInteger` tracks pass/fail count across all threads for summary report
- **Test isolation verification**: Teams run `stress tests` with 20 parallel threads on same test — watch for `NoSuchSessionException` which signals accidental session sharing

### Common Mistakes
- `static WebDriver driver` in base class — guaranteed thread corruption in parallel runs
- Storing page object as `static` field — page object holds driver reference → shared driver → corruption
- `PageFactory.initElements(driver, this)` in base class constructor with shared driver — PageFactory proxies use the driver at proxy call time — safe **only if** the driver passed is already thread-specific
- Using `@BeforeAll static void setUp()` for driver init — `@BeforeAll` runs once per class, shared across all test methods in that class running on different threads

### Optimization Tip
Add a **thread-safety validator** to your `DriverFactory` that detects accidental double-initialization:
```java
public static void initDriver(String browser) {
    if (driverPool.get() != null) {
        log.warn("Driver already initialized for thread {} — this may indicate a setup issue",
            Thread.currentThread().getName());
        driverPool.get().quit();
        driverPool.remove();
    }
    driverPool.set(createDriver(browser));
}
```

### Debugging Strategy
1. `Invalid Session ID` or `session deleted` in parallel run → driver was quit by another thread → static field, not ThreadLocal
2. Tests work alone, fail when parallel → shared mutable state — enable thread names in logs, trace which thread owns which session
3. `NullPointerException` in `driver()` method → `ThreadLocal.get()` returns null → driver not initialized for this thread (wrong `@BeforeEach` scope)
4. Thread dump shows two threads in `ChromeDriver.execute()` at same time → definitive proof of shared driver instance

### Interview Trap
"What is a memory leak in the context of `ThreadLocal`?" — The answer: thread pool threads (like those used by TestNG/JUnit parallel executor) are reused after test completion. If `ThreadLocal.remove()` is not called in `@AfterEach`, the old `WebDriver` object remains referenced by the thread's `ThreadLocalMap`, preventing garbage collection. Over 500 tests, this accumulates 500 quit-but-not-GC'd `WebDriver` objects → `OutOfMemoryError`.

### Follow-up Questions
1. What happens to ThreadLocal values when JUnit 5 uses virtual threads (Project Loom) for parallel execution?
2. How do you diagnose a race condition in a Selenium test that appears only under high parallel load?

### Selenium vs Playwright
Playwright's `BrowserContext` is designed as the test isolation unit — each test gets its own context with isolated cookies, storage, and network state. No `ThreadLocal` pattern needed; isolation is enforced by the API. Playwright's architecture makes accidental state sharing structurally much harder.

---

## Q35: How do you handle browser lifecycle management — startup, reuse, and cleanup?

### Interview Answer
Browser lifecycle determines **test isolation vs speed**. Three strategies: **new browser per test** (maximum isolation, slowest), **new context per test with shared browser** (fast, clean state — Playwright model), and **browser pool** (reuse sessions across tests — fastest, risk of state pollution). Production choice: **new driver per test class** (compromise between speed and isolation) with `ThreadLocal` cleanup.

### Deep Explanation
**Lifecycle strategies:**

| Strategy | Session per | Isolation | Speed | Use Case |
|---|---|---|---|---|
| New driver per test method | Method | Maximum | Slowest | Flaky/stateful tests |
| New driver per test class | Class | Good | Medium | Most production suites |
| Shared driver across suite | Suite | Minimal | Fastest | Read-only smoke tests |
| Driver pool | N sessions | Configurable | Fast | High-volume parallel |

**Browser startup cost breakdown:**
- Chrome spawn + initialization: ~1.5–3 seconds
- Navigate to base URL: ~0.5–2 seconds
- Login via UI: ~3–5 seconds
- With cookie injection: ~0.1 seconds

For 500 tests × 3-second startup = **25 minutes** in startup alone if new driver per test. Shared driver per class with cookie login = 25x faster setup overhead.

**What "state" gets shared when reusing a browser:**
- Cookies ← must clear between tests
- localStorage / sessionStorage ← must clear
- Browser cache ← acceptable to share / clear depending on test
- Open tabs/windows ← close all but one
- DOM event listeners ← gone on navigation
- JavaScript globals ← gone on navigation

### Java Example
```java
// STRATEGY 1: New driver per test method (maximum isolation)
public class MaxIsolationTest {
    private WebDriver driver;

    @BeforeEach
    void setUp() { driver = DriverFactory.createDriver("chrome"); }

    @AfterEach
    void tearDown() { if (driver != null) driver.quit(); }
}

// STRATEGY 2: New driver per test class (production standard)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClassScopedTest {
    private WebDriver driver;
    private BrowserStorageUtils storage;

    @BeforeAll
    void startBrowser() {
        driver = DriverFactory.createDriver("chrome");
        storage = new BrowserStorageUtils(driver);
    }

    @BeforeEach
    void resetState() {
        // Clean slate before each test — reuse same browser
        storage.clearLocalStorage();
        storage.clearSessionStorage();
        driver.manage().deleteAllCookies();
        closeExtraTabs(driver);
        driver.navigate().to(BASE_URL);
    }

    @AfterAll
    void stopBrowser() {
        if (driver != null) driver.quit();
    }

    private void closeExtraTabs(WebDriver driver) {
        String original = driver.getWindowHandles().iterator().next();
        driver.getWindowHandles().stream()
            .filter(h -> !h.equals(original))
            .forEach(h -> { driver.switchTo().window(h); driver.close(); });
        driver.switchTo().window(original);
    }
}

// STRATEGY 3: DRIVER POOL — for high-volume parallel execution
public class DriverPool {
    private final BlockingQueue<WebDriver> pool;
    private final int poolSize;
    private final String browser;

    public DriverPool(String browser, int size) {
        this.browser = browser;
        this.poolSize = size;
        this.pool = new ArrayBlockingQueue<>(size);
        // Pre-warm pool
        IntStream.range(0, size).forEach(i ->
            pool.offer(DriverFactory.createDriver(browser)));
    }

    public WebDriver acquire(Duration timeout) throws InterruptedException {
        WebDriver driver = pool.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (driver == null) throw new RuntimeException("No driver available in pool after " + timeout);
        // Verify driver is still alive
        try {
            driver.getCurrentUrl();
        } catch (WebDriverException e) {
            log.warn("Dead driver in pool — creating replacement");
            driver = DriverFactory.createDriver(browser);
        }
        return driver;
    }

    public void release(WebDriver driver) {
        // Clean state before returning to pool
        try {
            driver.manage().deleteAllCookies();
            ((JavascriptExecutor) driver).executeScript(
                "localStorage.clear(); sessionStorage.clear();");
            driver.get("about:blank"); // neutral state
            pool.offer(driver);
        } catch (Exception e) {
            log.warn("Driver cleanup failed — discarding from pool");
            driver.quit();
            pool.offer(DriverFactory.createDriver(browser)); // replace with fresh
        }
    }

    public void shutdown() {
        pool.forEach(driver -> { try { driver.quit(); } catch (Exception ignored) {} });
        pool.clear();
    }
}

// BROWSER MEMORY LEAK PREVENTION
// After long test suites, Chrome can accumulate memory
public void preventMemoryLeak(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    // Clear browser cache via CDP
    if (driver instanceof ChromeDriver cd) {
        try {
            cd.getDevTools().createSession();
            cd.getDevTools().send(Network.clearBrowserCache());
            cd.getDevTools().send(Network.clearBrowserCookies());
        } catch (Exception ignored) {}
    }
    // Force GC (hint — not guaranteed)
    js.executeScript("window.gc && window.gc();");
}

// GRACEFUL SHUTDOWN HOOK — prevent zombie processes on JVM crash
public class DriverFactory {
    private static final List<WebDriver> allDrivers =
        Collections.synchronizedList(new ArrayList<>());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM shutdown — quitting {} drivers", allDrivers.size());
            allDrivers.forEach(d -> { try { d.quit(); } catch (Exception ignored) {} });
        }));
    }

    public static WebDriver createDriver(String browser) {
        WebDriver driver = buildDriver(browser);
        allDrivers.add(driver);
        return driver;
    }

    public static void quitDriver(WebDriver driver) {
        allDrivers.remove(driver);
        try { driver.quit(); } catch (Exception ignored) {}
    }
}
```

### Real-world Usage
- **500-test regression suite**: New driver per class (50 classes × 3s startup = 2.5 min) vs per method (500 × 3s = 25 min) — 10x faster with class scope
- **Flaky test quarantine**: Isolated tests (per method) after proving they contaminate state in other tests
- **Smoke suite (50 tests)**: Shared driver across entire smoke suite — 5s startup, 50 tests, clean cookie wipe between tests
- **Driver pool in CI**: 4 Chrome instances in pool, 20 parallel test threads — 4 browsers serve 20 tests via pool

### Common Mistakes
- Not calling `driver.quit()` at end of suite — orphaned Chrome/ChromeDriver processes accumulate on CI agents
- Reusing driver across tests without clearing cookies — login state bleeds from one test to the next
- Not navigating to `about:blank` before returning to pool — previous page's network requests and timers still active
- Calling `driver.close()` instead of `driver.quit()` at end of session — closes the window but doesn't kill the ChromeDriver process
- Using `@AfterAll` with `@TestInstance(PER_METHOD)` — `@AfterAll` doesn't match expected lifecycle; driver never quits

### Optimization Tip
Implement **intelligent browser reuse** — reset state instead of restarting:
```java
public void resetBrowserState(WebDriver driver, String startUrl) {
    driver.manage().deleteAllCookies();
    ((JavascriptExecutor) driver).executeScript(
        "localStorage.clear(); sessionStorage.clear();" +
        "window.history.pushState({}, '', '/');" // reset history without reload
    );
    driver.get(startUrl);
}
// Cost: ~200ms vs 3000ms for new browser — 15x faster reset
```

### Debugging Strategy
1. Zombie Chrome processes on CI agent → `driver.quit()` not called — add shutdown hook, check `@AfterAll` runs
2. State bleed between tests in same class → cookies/storage not cleared in `@BeforeEach` — add `deleteAllCookies()` + `localStorage.clear()`
3. `driver.quit()` hangs for 30+ seconds → Chrome is waiting for in-flight downloads or network requests — set `pageLoadTimeout` and `scriptTimeout` to cap wait times
4. `NoSuchSessionException` when reusing driver from pool → Chrome was killed externally (OOM killer on CI) — add health-check in pool `acquire()`

### Interview Trap
"When would you use `driver.close()` vs `driver.quit()`?" — `close()` closes the current window/tab, `driver` remains active for other tabs. `quit()` terminates the entire session and the ChromeDriver process. Calling `close()` on the last tab leaves a dead session — subsequent calls throw `NoSuchSessionException`. **Always use `quit()` for cleanup.**

### Follow-up Questions
1. How would you implement a browser pool that automatically replaces dead/crashed driver instances without interrupting the test queue?
2. What is the impact on test reliability of sharing a browser across 50 tests vs creating a new browser per test?

### Selenium vs Playwright
Playwright's `BrowserContext` provides per-test isolation without spawning a new browser. `Browser.newContext()` creates an isolated session in milliseconds (vs 3 seconds for a new Chrome). Each context has its own cookies, storage, and network state. `context.close()` is instant. This model gives Playwright the isolation of per-test browser with the speed of browser reuse.

---

## Q36: How do you implement a robust custom wait library beyond WebDriverWait?

### Interview Answer
`WebDriverWait` covers element-level waits but production suites need **condition-level waits**: waiting for API responses, Angular/React render cycles, network idle, animation completion, and custom application states. A custom wait library wraps `FluentWait` with domain-specific condition builders, integrating with the test framework's assertion DSL.

### Deep Explanation
**Why `WebDriverWait` alone is insufficient:**
- `ExpectedConditions.visibilityOf()` passes as soon as element is in DOM — Angular component may still be rendering data
- No built-in "wait for network idle" — AJAX requests can still be in flight when element appears
- No "wait for React re-render cycle" — React batches state updates; DOM may flicker before settling
- No "wait for animation to complete" — CSS transitions make elements "visible" mid-animation

**Condition hierarchy for robust waits:**
```
1. DOM Condition     → elementIsVisible, elementHasText, elementCount
2. JS Condition      → documentReady, jsVariableEquals, angularReady, reactReady
3. Network Condition → networkIdle, pendingAjaxZero
4. App Condition     → customBusinessCondition (e.g., "cart total updated")
5. Composite         → allOf(networkIdle, elementHasText)
```

### Java Example
```java
// CUSTOM WAIT LIBRARY
public class AppWait {
    private final WebDriver driver;
    private final JavascriptExecutor js;
    private Duration timeout;
    private Duration polling;

    public AppWait(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.timeout = Duration.ofSeconds(15);
        this.polling = Duration.ofMillis(300);
    }

    public AppWait withTimeout(Duration t) { this.timeout = t; return this; }
    public AppWait pollingEvery(Duration p) { this.polling = p; return this; }

    private <T> T waitFor(ExpectedCondition<T> condition, String description) {
        return new FluentWait<>(driver)
            .withTimeout(timeout)
            .pollingEvery(polling)
            .withMessage(() -> "Timed out waiting for: " + description
                + " [url=" + driver.getCurrentUrl() + "]")
            .ignoring(StaleElementReferenceException.class)
            .ignoring(NoSuchElementException.class)
            .until(condition);
    }

    // ── DOM CONDITIONS ─────────────────────────────────────────────
    public WebElement untilVisible(By locator) {
        return waitFor(ExpectedConditions.visibilityOfElementLocated(locator),
            "element visible: " + locator);
    }

    public WebElement untilClickable(By locator) {
        return waitFor(ExpectedConditions.elementToBeClickable(locator),
            "element clickable: " + locator);
    }

    public WebElement untilHasText(By locator, String expectedText) {
        return waitFor(d -> {
            WebElement el = d.findElement(locator);
            String text = el.getText().trim();
            if (!text.contains(expectedText))
                throw new NoSuchElementException(
                    "Expected '" + expectedText + "' but got '" + text + "'");
            return el;
        }, "element has text '" + expectedText + "'");
    }

    public List<WebElement> untilCount(By locator, int count) {
        return waitFor(d -> {
            List<WebElement> els = d.findElements(locator);
            return els.size() == count ? els : null;
        }, "element count == " + count);
    }

    public void untilAbsent(By locator) {
        waitFor(ExpectedConditions.invisibilityOfElementLocated(locator),
            "element absent: " + locator);
    }

    // ── JS / FRAMEWORK CONDITIONS ──────────────────────────────────
    public void untilDocumentReady() {
        waitFor(d -> "complete".equals(js.executeScript("return document.readyState")),
            "document.readyState == complete");
    }

    public void untilAngularReady() {
        waitFor(d -> Boolean.TRUE.equals(js.executeScript(
            "return !window.angular || " +
            "window.getAllAngularTestabilities().every(t => t.isStable())")),
            "Angular stable");
    }

    public void untilReactReady() {
        waitFor(d -> {
            Boolean ready = (Boolean) js.executeScript(
                "var root = document.getElementById('root') || document.body;" +
                "var fiber = root._reactRootContainer || root.__reactFiber$$;" +
                "if (!fiber) return true;" +
                "return typeof fiber.pendingLanes === 'number' ? fiber.pendingLanes === 0 : true;");
            return Boolean.TRUE.equals(ready);
        }, "React render complete");
    }

    // ── NETWORK CONDITIONS ─────────────────────────────────────────
    public void untilNetworkIdle() {
        js.executeScript(
            "if (!window.__pendingRequests) {" +
            "  window.__pendingRequests = 0;" +
            "  var origOpen = XMLHttpRequest.prototype.open;" +
            "  XMLHttpRequest.prototype.open = function() {" +
            "    window.__pendingRequests++;" +
            "    this.addEventListener('loadend', () => window.__pendingRequests--);" +
            "    origOpen.apply(this, arguments);" +
            "  };" +
            "  var origFetch = window.fetch;" +
            "  window.fetch = function() {" +
            "    window.__pendingRequests++;" +
            "    return origFetch.apply(this, arguments)" +
            "      .finally(() => window.__pendingRequests--);" +
            "  };" +
            "}");
        waitFor(d -> {
            Long pending = (Long) js.executeScript("return window.__pendingRequests || 0;");
            return pending == 0;
        }, "network idle (0 pending XHR/fetch)");
    }

    // ── COMPOSITE ──────────────────────────────────────────────────
    public WebElement untilStable(By locator) {
        untilNetworkIdle();
        return untilVisible(locator);
    }

    // ── ANIMATION COMPLETION ───────────────────────────────────────
    public void untilAnimationComplete(By locator) {
        waitFor(d -> {
            WebElement el = d.findElement(locator);
            String state = (String) js.executeScript(
                "var s = window.getComputedStyle(arguments[0]);" +
                "return s.animationPlayState === 'running' || " +
                "       s.transitionDuration !== '0s' ? 'animating' : 'done';", el);
            return "done".equals(state);
        }, "animation complete: " + locator);
    }
}

// USAGE IN PAGE OBJECT
public class SearchPage {
    private static final By SEARCH_INPUT    = By.cssSelector("[data-testid='search']");
    private static final By RESULTS_LIST    = By.cssSelector("[data-testid='result']");
    private static final By LOADING_SPINNER = By.cssSelector(".spinner");

    private final AppWait wait;

    public SearchPage(WebDriver driver) {
        this.wait = new AppWait(driver).withTimeout(Duration.ofSeconds(20));
    }

    public List<WebElement> searchFor(String query) {
        wait.untilClickable(SEARCH_INPUT).sendKeys(query + Keys.ENTER);
        wait.untilAbsent(LOADING_SPINNER);
        wait.untilNetworkIdle();
        return wait.untilCount(RESULTS_LIST, 10);
    }
}
```

### Real-world Usage
- **Angular SPA**: `untilAngularReady()` after every click — Angular's `$http` and `$timeout` tracked; test proceeds only when digest cycle is stable
- **Infinite scroll table**: `untilCount(ROW, 25)` — waits until exactly 25 rows load, not just first batch
- **AJAX form validation**: `untilNetworkIdle()` then `untilHasText(ERROR_MSG, "Email already exists")` — avoids race where error element exists before text is set
- **Animation guard**: Login modal has 300ms CSS transition — `untilAnimationComplete()` before `click()` to prevent mid-animation click failure

### Common Mistakes
- `Thread.sleep(2000)` instead of explicit wait — brittle, slow, masks root cause
- Stacking `untilVisible` + `untilClickable` redundantly — `untilClickable` already implies visible
- Setting `implicitlyWait` alongside `FluentWait` — they interact unpredictably; implicit wait doubles effective poll timeout
- Starting `untilNetworkIdle` after the action — XHR may fire and complete before monitoring begins; inject counter once at page load

### Debugging Strategy
1. Wait times out despite element visible → Angular/React rerender resets element between polls — add framework stability check before DOM assertion
2. `untilNetworkIdle` never returns → WebSocket connections increment the counter permanently — filter event source and WebSocket from request counting
3. `FluentWait` message not showing useful detail → use lambda `withMessage(() -> ...)` capturing current URL and DOM state
4. Intermittent `StaleElementReferenceException` in `untilHasText` → add `.ignoring(StaleElementReferenceException.class)` to the FluentWait chain

### Interview Trap
"What is the difference between `implicitlyWait` and `FluentWait`?" — `implicitlyWait` is a global timeout applied to every `findElement` call, blocking until element appears. `FluentWait` is explicit, per-condition, with configurable polling and custom exception ignoring. **Never mix both** — implicit wait applies on each `findElement` inside FluentWait's poll, multiplying the effective timeout unpredictably.

### Follow-up Questions
1. How do you wait for a React component to finish rendering data fetched from an API?
2. How would you detect and handle an infinite spinner state that never completes?

### Selenium vs Playwright
Playwright has built-in auto-waiting on every action — `click()`, `fill()`, `check()` all wait for the element to be visible, stable, enabled, and not animating before acting. `page.waitForResponse()` and `page.waitForLoadState('networkidle')` handle network waits natively. No custom wait library needed for most cases.

---

## Q37: How do you implement a self-healing locator strategy?

### Interview Answer
Self-healing locators detect when a primary locator fails and **automatically fall back through a ranked list of alternatives**, logging the failure for later repair. The goal is reducing test maintenance burden — when a dev changes a CSS class but keeps `data-testid`, only one locator needs updating rather than hunting through 200 test files.

### Deep Explanation
**Self-healing rationale:**
- UI changes are the #1 cause of locator failures in mature test suites
- A production app changes 10–30 locators per sprint
- Without self-healing: each change requires immediate test maintenance
- With self-healing: test passes using fallback; team is alerted to repair at lower urgency

**Locator stability hierarchy (most stable → least stable):**
```
1. data-testid / data-qa / data-cy  → developer-controlled, stable by contract
2. ARIA role + name                 → accessibility attributes, stable in accessible apps
3. id attribute                     → unique by spec; avoid dynamic IDs like "input_12"
4. name attribute                   → stable for form elements
5. CSS by stable class              → structural/BEM classes (not utility/animation)
6. XPath by text                    → brittle if i18n changes
7. XPath by position                → most fragile; breaks on DOM restructure
```

### Java Example
```java
// SELF-HEALING LOCATOR
public class HealingLocator {
    private final String elementName;
    private final List<By> locators = new ArrayList<>();
    private static final ConcurrentHashMap<String, Integer> healingLog = new ConcurrentHashMap<>();

    public static HealingLocator named(String name) { return new HealingLocator(name); }
    private HealingLocator(String name) { this.elementName = name; }

    public HealingLocator primary(By locator) { locators.add(0, locator); return this; }
    public HealingLocator fallback(By... fallbacks) {
        locators.addAll(Arrays.asList(fallbacks)); return this;
    }

    public WebElement find(WebDriver driver) {
        return find(driver, Duration.ofSeconds(10));
    }

    public WebElement find(WebDriver driver, Duration timeout) {
        List<Exception> failures = new ArrayList<>();
        for (int i = 0; i < locators.size(); i++) {
            By locator = locators.get(i);
            try {
                WebElement el = new WebDriverWait(driver, timeout)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));

                if (i > 0) {
                    String key = elementName + "::" + locator;
                    healingLog.merge(key, 1, Integer::sum);
                    log.warn("[HEALING] '{}' — primary {} failed, healed with fallback #{}: {} [{} times]",
                        elementName, locators.get(0), i, locator, healingLog.get(key));
                    HealingReporter.record(elementName, locators.get(0), locator, driver.getCurrentUrl());
                }
                return el;
            } catch (TimeoutException | NoSuchElementException e) {
                failures.add(e);
            }
        }
        throw new HealingFailedException(
            "All " + locators.size() + " locators failed for '" + elementName + "':\n" +
            IntStream.range(0, locators.size())
                .mapToObj(i -> "  [" + i + "] " + locators.get(i) + ": " + failures.get(i).getMessage())
                .collect(Collectors.joining("\n")));
    }

    public static Map<String, Integer> getHealingReport() {
        return Collections.unmodifiableMap(healingLog);
    }
}

// USAGE IN PAGE OBJECT
public class CheckoutPage {
    private static final HealingLocator PAY_BUTTON = HealingLocator.named("Pay Button")
        .primary(By.cssSelector("[data-testid='checkout-pay-btn']"))
        .fallback(
            By.cssSelector("[aria-label='Complete purchase']"),
            By.id("checkout-submit"),
            By.cssSelector("button.checkout-submit"),
            By.xpath("//button[contains(text(),'Pay')]")
        );

    private static final HealingLocator ORDER_TOTAL = HealingLocator.named("Order Total")
        .primary(By.cssSelector("[data-testid='order-total']"))
        .fallback(
            By.cssSelector(".order-summary__total"),
            By.xpath("//*[contains(@class,'total') and contains(text(),'$')]")
        );

    private final WebDriver driver;
    public CheckoutPage(WebDriver driver) { this.driver = driver; }

    public ConfirmationPage clickPay() {
        PAY_BUTTON.find(driver).click();
        return new ConfirmationPage(driver);
    }

    public String getOrderTotal() {
        return ORDER_TOTAL.find(driver).getText();
    }
}

// CI HEALTH REPORT — published after test run
@AfterSuite
void publishHealingReport() {
    Map<String, Integer> healings = HealingLocator.getHealingReport();
    if (!healings.isEmpty()) {
        log.warn("=== LOCATOR HEALING REPORT ===");
        healings.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> log.warn("  {} healed {} times", e.getKey(), e.getValue()));
        // Alert Slack/Teams + create JIRA tickets for top offenders
        SlackNotifier.sendHealingAlert(healings);
    }
}
```

### Real-world Usage
- **E-commerce suite (800 tests)**: After React component rename, `data-testid` still works — zero test failures vs 80 broken tests without healing
- **Weekly healing report**: CI publishes top-10 most-healed locators as tech debt tickets every Monday
- **Zero-tolerance gate**: Any locator healed > 5 times in a week triggers automated JIRA ticket creation
- **A/B test traffic**: Test runs on variant A (old class) and variant B (new class) — healing log reveals which variant broke the primary locator

### Common Mistakes
- Making fallback locators too similar to primary — if primary fails due to DOM restructure, similar fallbacks fail too; always include ARIA + text-based for diversity
- Not setting a healing alert threshold — silent healing masks growing locator debt until many tests fail simultaneously
- Using healing to cover unstable test infrastructure — healing fixes locator fragility, not network failures or JS errors
- More than 5 fallbacks — makes tests too slow when primary fails on every run

### Debugging Strategy
1. All locators fail despite element visible → element is inside an iframe or Shadow DOM — ensure correct context before healing search
2. Healing report shows same locator healed 50+ times → primary locator is effectively dead — swap fallback to primary, delete old
3. Healing works in CI but not locally → different app version deployed locally — pin test environment

### Interview Trap
"Isn't self-healing just masking poor test maintenance?" — Partially yes. Self-healing is a **reliability tool, not a maintenance substitute**. The key is the **healing report** — every heal is logged, reported, and triaged. Healing buys the team time to fix locators at reasonable pace without emergency halts. Without healing: one dev refactors a component → 80 tests fail in CI → entire team drops work to fix. With healing: 80 tests pass, healing report filed, fixed in next sprint.

### Follow-up Questions
1. How would you implement a self-healing locator that uses visual/DOM similarity scoring instead of a predefined fallback list?
2. How do you prevent developers from relying on healing forever and never repairing the primary locator?

### Selenium vs Playwright
Playwright has no built-in self-healing, but its first-class ARIA locators (`getByRole`, `getByLabel`, `getByText`) are inherently more stable than CSS/XPath — equivalent to always using the most stable tier. Playwright's philosophy: write stable locators from the start rather than heal fragile ones.

---

## Q38: How do you implement API + UI hybrid testing with REST Assured and Selenium?

### Interview Answer
Hybrid testing uses API calls for **test setup and teardown** and Selenium only for **UI interaction validation**. This eliminates redundant UI steps: instead of logging in via UI for every test, POST credentials; instead of creating test data via UI forms, POST to the API. Rule: **only test through the UI what the user actually does through the UI**.

### Deep Explanation
**Why hybrid testing:**
- Creating one user via UI: 5 screens × 3 seconds = 15 seconds
- Creating same user via API POST: 200ms
- For 500 tests each needing a fresh user: 15s × 500 = 125 min (UI) vs 0.2s × 500 = 1.7 min (API)
- APIs also verify backend state directly — asserting a DB record was saved correctly, not just displayed in UI

**Layers:**
```
API ONLY         → Contract tests (REST Assured alone)
API + UI         → API for setup, UI for user journey
UI + API verify  → UI action, API assertion (verify DB state, not just display)
UI ONLY          → Cannot avoid (visual render, CSS, accessibility, animations)
```

### Java Example
```java
// REQUEST SPEC — shared configuration
public class ApiConfig {
    private static final RequestSpecification REQUEST_SPEC =
        new RequestSpecBuilder()
            .setBaseUri(System.getProperty("apiBaseUrl", "https://api.example.com"))
            .setContentType(ContentType.JSON)
            .addHeader("Accept", "application/json")
            .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
            .addFilter(new RequestLoggingFilter(LogDetail.ALL))
            .build();

    public static RequestSpecification spec() { return REQUEST_SPEC; }
}

// API DATA FACTORY
public class UserApiFactory {
    public static TestUser createUser(String role) {
        String email = "test+" + UUID.randomUUID() + "@example.com";
        String body = """
            {"email":"%s","password":"TestPass123!","role":"%s",
             "firstName":"Test","lastName":"User"}
            """.formatted(email, role);

        String userId = given().spec(ApiConfig.spec())
            .header("Authorization", "Bearer " + getAdminToken())
            .body(body)
        .when().post("/admin/users")
        .then().statusCode(201).extract().path("id");

        return new TestUser(userId, email, "TestPass123!", role);
    }

    public static String getAuthToken(TestUser user) {
        return given().spec(ApiConfig.spec())
            .body(Map.of("email", user.email(), "password", user.password()))
        .when().post("/auth/login")
        .then().statusCode(200).extract().path("token");
    }

    public static void deleteUser(String userId) {
        given().spec(ApiConfig.spec())
            .header("Authorization", "Bearer " + getAdminToken())
        .when().delete("/admin/users/{id}", userId)
        .then().statusCode(204);
    }
}

// HYBRID TEST — API setup + UI journey + API backend verification
class OrderFulfillmentTest extends BaseTest {
    private TestUser customer;
    private String orderId;

    @BeforeEach
    void setUp() {
        customer = UserApiFactory.createUser("CUSTOMER");
        ProductApiFactory.createProduct("SKU-001", "Laptop", 1299.99);

        // Inject token — bypass login UI
        String token = UserApiFactory.getAuthToken(customer);
        DriverFactory.initDriver("chrome");
        driver().get(BASE_URL);
        new BrowserStorageUtils(driver()).injectJwtToken(token);
    }

    @Test
    void userCanPlaceOrderAndSeeConfirmation() {
        // UI: user journey only
        ConfirmationPage confirmation = new ProductPage(driver())
            .navigateTo("SKU-001")
            .addToCart()
            .proceedToCheckout()
            .fillShipping(customer.address())
            .payWithCard("4242424242424242")
            .confirm();

        // UI assertion
        assertThat(confirmation.getOrderNumber()).matches("ORD-\\d{6}");
        orderId = confirmation.getOrderNumber();

        // API verification — verify backend state, not just display
        given().spec(ApiConfig.spec())
            .header("Authorization", "Bearer " + UserApiFactory.getAuthToken(customer))
        .when().get("/orders/{id}", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"))
            .body("items[0].sku", equalTo("SKU-001"))
            .body("total", equalTo(1299.99f));
    }

    @AfterEach
    void tearDown() {
        if (orderId != null) OrderApiFactory.cancelOrder(orderId);
        if (customer != null) UserApiFactory.deleteUser(customer.id());
        DriverFactory.quitDriver();
    }
}

// WAIT FOR ASYNC BACKEND JOB VIA API POLL
public String waitForOrderStatus(String orderId, String expectedStatus) {
    return new FluentWait<>(driver())
        .withTimeout(Duration.ofSeconds(30))
        .pollingEvery(Duration.ofSeconds(2))
        .until(d -> {
            String actual = given().spec(ApiConfig.spec())
                .when().get("/orders/{id}", orderId)
                .then().extract().path("status");
            return expectedStatus.equals(actual) ? actual : null;
        });
}
```

### Real-world Usage
- **E-commerce (300 tests)**: API creates user + cart + address in 500ms; UI tests start at payment page — saves 8 minutes per full suite run
- **Multi-tenant SaaS**: API creates tenant + admin + sample data per test; teardown deletes entire tenant — clean isolation, 300ms setup vs 5-minute UI wizard
- **Background job verification**: Place order via UI, then poll `/orders/{id}` API every 2 seconds until `status == SHIPPED` — more reliable than refreshing page and reading UI
- **Negative scenarios**: Use API to put record into a locked/error state impossible to reach via normal UI flow

### Common Mistakes
- Testing API contract through UI — if API returns wrong data that UI displays, the UI test passes; use direct REST Assured assertions for contract testing
- Not cleaning up API-created data — leaked users and orders cause unique constraint failures in subsequent runs
- Using different base URLs for API and UI without config management — test creates user on staging API, UI pointed at prod
- Calling `getAuthToken()` on every API assertion — token is stable; cache it per role to avoid 100+ redundant login calls

### Debugging Strategy
1. UI test passes but backend data wrong → not asserting via API — add post-action API verification step
2. `401 Unauthorized` on API calls in test → admin token expired mid-run — add token refresh logic; reduce TTL in test environment
3. API creates data but UI doesn't show it → cache invalidation gap — call cache-clear API endpoint or navigate with cache-bust param after API setup
4. Teardown fails to delete user → order references user (FK): delete child records first in teardown

### Interview Trap
"Why assert via API when the UI already confirmed the action?" — Because **UI tests verify presentation layer only**. An API bug can corrupt data that the UI displays misleadingly but visually correctly. For example: UI shows "Total: $99" even if the API stored "$9.9" due to a decimal rounding bug — only an API assertion catches this. UI tests verify what the user sees; API tests verify what actually happened in the system.

### Follow-up Questions
1. How do you synchronize UI actions with background asynchronous API operations in a hybrid test?
2. When a feature is tested via hybrid (API setup + UI action + API verify), what categories of bugs can it still miss?

### Selenium vs Playwright
Playwright has `page.request` — a built-in HTTP client sharing the browser context's cookies and auth state. `page.request.get('/api/orders')` sends an authenticated API call with the same session as the browser. No separate REST Assured configuration needed; auth is automatic.

---

## Q39: How do you test accessibility (a11y) with Selenium?

### Interview Answer
Selenium doesn't natively test accessibility — but **axe-core** (by Deque) integrates via JavaScript injection to run WCAG 2.1/2.2 audits in any WebDriver session. Automated a11y testing catches ~30% of WCAG issues (the rest require human judgment). Key violations to gate in CI: missing ARIA labels, insufficient color contrast, missing alt text, keyboard navigation failures, and focus management issues.

### Deep Explanation
**Accessibility testing layers:**
```
Automated (Selenium + axe)  → WCAG rule violations detectable by DOM analysis (~30% coverage)
Manual + Screen Reader       → NVDA/JAWS experience (cannot be automated)
Keyboard Navigation          → Tab order, focus visibility, keyboard traps
Color Contrast               → axe detects, but edge cases need human review
Cognitive Accessibility      → Requires user testing — not automatable
```

**WCAG 2.1 violation categories axe catches automatically:**
- Missing `alt` on `<img>` elements
- Form inputs without `<label>` or `aria-label`
- Color contrast below 4.5:1 (normal text) or 3:1 (large text)
- Missing landmark roles (`<main>`, `<nav>`, `<header>`)
- Interactive elements not keyboard reachable
- Duplicate `id` attributes
- Invalid ARIA roles or required child roles missing

### Java Example
```java
// pom.xml:
// <dependency>
//   <groupId>com.deque.html.axe-core</groupId>
//   <artifactId>selenium</artifactId>
//   <version>4.8.0</version>
// </dependency>

// ACCESSIBILITY AUDITOR UTILITY
public class A11yAuditor {
    private final WebDriver driver;

    public A11yAuditor(WebDriver driver) { this.driver = driver; }

    public AxeResults auditPage() {
        return new AxeBuilder()
            .withTags(List.of("wcag2a", "wcag2aa", "wcag21aa"))
            .analyze(driver);
    }

    public AxeResults auditComponent(By locator) {
        return new AxeBuilder()
            .withTags(List.of("wcag2a", "wcag2aa"))
            .include(List.of(driver.findElement(locator)))
            .analyze(driver);
    }

    public AxeResults auditWithExclusions(List<String> knownIssueSelectors) {
        AxeBuilder builder = new AxeBuilder()
            .withTags(List.of("wcag2a", "wcag2aa", "wcag21aa"));
        knownIssueSelectors.forEach(s -> builder.exclude(List.of(s)));
        return builder.analyze(driver);
    }

    public void assertNoViolations(AxeResults results) {
        List<Rule> violations = results.getViolations();
        if (!violations.isEmpty()) {
            String report = violations.stream()
                .map(v -> String.format(
                    "  [%s] %s (impact: %s)\n    %s\n    Affected:\n%s",
                    v.getId(), v.getDescription(), v.getImpact(), v.getHelpUrl(),
                    v.getNodes().stream()
                        .map(n -> "      • " + n.getTarget() + "\n        " + n.getFailureSummary())
                        .collect(Collectors.joining("\n"))))
                .collect(Collectors.joining("\n\n"));
            fail("A11y violations on " + driver.getCurrentUrl() + ":\n\n" + report);
        }
    }
}

// KEYBOARD NAVIGATION TEST
class KeyboardAccessibilityTest extends BaseTest {
    @Test
    void checkoutFormIsFullyKeyboardAccessible() {
        new CheckoutPage(driver()).navigate();
        WebElement body = driver().findElement(By.tagName("body"));
        List<String> focusOrder = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            body.sendKeys(Keys.TAB);
            WebElement focused = (WebElement) ((JavascriptExecutor) driver())
                .executeScript("return document.activeElement;");
            String testId = focused.getDomAttribute("data-testid");
            if (testId != null) focusOrder.add(testId);
        }

        assertThat(focusOrder).containsSubsequence(
            "shipping-name", "shipping-address", "card-number", "checkout-submit");
    }

    @Test
    void modalCloseableWithEscapeKey() {
        new ProductPage(driver()).openImageGallery();
        By modal = By.cssSelector("[role='dialog']");
        new AppWait(driver()).untilVisible(modal);
        driver().findElement(modal).sendKeys(Keys.ESCAPE);
        new AppWait(driver()).untilAbsent(modal);
    }
}

// A11Y REGRESSION SUITE — all critical pages
@ParameterizedTest
@ValueSource(strings = {"/", "/login", "/dashboard", "/checkout", "/profile"})
void pagePassesWCAG21AA(String path) {
    driver().get(BASE_URL + path);
    A11yAuditor auditor = new A11yAuditor(driver());
    AxeResults results = auditor.auditPage();
    auditor.assertNoViolations(results);
}
```

### Real-world Usage
- **Government/regulated apps**: WCAG 2.1 AA is a legal requirement (ADA, Section 508, EU Web Accessibility Directive) — automated gate prevents deploying non-compliant releases
- **New-feature scoping**: `auditComponent(By.cssSelector("[data-testid='new-feature']"))` — scope to only new code; lets teams adopt incrementally without fixing legacy violations first
- **Violation trending dashboard**: Run axe in parallel with functional tests; track violation count over time per sprint to show improvement/regression trend
- **Exclusion management**: Known third-party widget violations tracked in JIRA, excluded with expiry comments, reviewed monthly

### Common Mistakes
- Treating axe as complete a11y coverage — automated testing catches ~30% of WCAG issues; always combine with manual screen reader testing
- Running axe without specifying WCAG tags — defaults may not match legal standard (WCAG 2.1 AA requires `wcag2a`, `wcag2aa`, `wcag21aa`)
- Excluding violations without tracking — exclusion without a JIRA ticket and owner = permanent suppress
- Ignoring keyboard navigation — axe detects missing labels but not whether focus order is logically meaningful

### Debugging Strategy
1. `color-contrast` violations on correctly styled elements → browser zoom affects contrast calc — run axe at 100% zoom
2. Axe passes but screen reader users report issues → ARIA is technically correct but semantically confusing — requires manual VoiceOver/NVDA audit
3. Violation count increases after dependency update → third-party component library introduced a11y regression — scope axe to exclude library components temporarily
4. `aria-required-children` on custom component → ARIA parent role requires owned child roles not present — add missing child roles or change parent role

### Interview Trap
"Does axe-core testing make your app accessible?" — No. Automated tools catch structural violations (missing labels, measurable contrast ratios). Real accessibility requires: **correct ARIA semantics, logical reading order, meaningful focus management, plain language for cognitive accessibility** — none fully automatable. axe is the baseline catch; manual testing with real assistive technology is the standard.

### Follow-up Questions
1. A screen reader user reports they can't operate your custom dropdown. axe shows no violations. How do you debug this?
2. How do you include a11y testing in CI when the team has 200 existing known violations?

### Selenium vs Playwright
Playwright has `@axe-core/playwright` with `checkA11y()`. Its auto-waiting means a11y audits run on stabilized DOM — avoiding timing issues common in Selenium axe integrations where audits run before React/Angular finishes rendering.

---

## Q40: How do you structure a Selenium framework for a large team (10+ developers)?

### Interview Answer
A large-team framework is governed by **convention over configuration**: page objects follow one pattern, waits use one utility, locators follow one naming scheme. The framework provides scaffolding that makes doing the right thing easy and doing the wrong thing hard. Key concerns: **parallel execution**, **shared infrastructure**, **test data isolation**, **CI integration**, and **onboarding speed** (new developer productive in < 1 day).

### Deep Explanation
**Large-team framework pillars:**

| Pillar | Implementation | Problem Solved |
|---|---|---|
| Driver management | `ThreadLocal` + `DriverFactory` | Parallel safety |
| Page Objects | Base class + component model | Consistency |
| Wait strategy | Single `AppWait` utility | Timing reliability |
| Test data | API factory + cleanup registration | Isolation |
| Reporting | Allure + MDC logging | Debuggability |
| Configuration | Env-based `ConfigReader` | Multi-environment |
| CI/CD | Parameterized pipelines | Grid execution |
| Linting | Custom Checkstyle rules | Anti-pattern enforcement |

**Directory structure (Maven multi-module):**
```
automation-framework/
├── framework-core/       ← DriverFactory, AppWait, BasePage, API clients
├── framework-reporting/  ← Allure, logging, screenshot capture
├── page-objects/         ← all page and component objects
├── test-data/            ← API factories, test data models, builders
└── tests/                ← @Test classes only (thin layer)
```

### Java Example
```java
// BASE TEST — all tests extend this
@ExtendWith({DriverExtension.class, AllureExtension.class, RetryExtension.class})
public abstract class BaseTest {
    protected WebDriver driver()  { return DriverFactory.getDriver(); }
    protected AppWait wait()      { return new AppWait(driver()); }
    protected BrowserStorageUtils storage() { return new BrowserStorageUtils(driver()); }

    protected void loginAs(String role) {
        String token = AuthTokenCache.getOrCreate(role);
        driver().get(BASE_URL);
        storage().injectJwtToken(token);
        driver().get(BASE_URL + "/dashboard");
    }

    protected void navigateTo(String path) { driver().get(BASE_URL + path); }
    protected static final String BASE_URL = ConfigReader.get("baseUrl");
}

// DRIVER EXTENSION — lifecycle + failure handling
public class DriverExtension implements BeforeEachCallback, AfterEachCallback, TestWatcher {
    @Override
    public void beforeEach(ExtensionContext ctx) {
        String browser = ConfigReader.get("browser", "chrome");
        DriverFactory.initDriver(browser);
        MDC.put("testId", ctx.getUniqueId());
        MDC.put("browser", browser);
        MDC.put("thread", Thread.currentThread().getName());
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        DriverFactory.quitDriver();
        MDC.clear();
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        byte[] screenshot = ((TakesScreenshot) DriverFactory.getDriver())
            .getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment("Screenshot", "image/png",
            new ByteArrayInputStream(screenshot), ".png");
        Allure.addAttachment("Page Source", DriverFactory.getDriver().getPageSource());
    }
}

// CONFIG READER — system property → env var → properties file → default
public class ConfigReader {
    private static final Properties props = loadProperties();

    public static String get(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.toUpperCase().replace(".", "_"));
        if (v == null) v = props.getProperty(key);
        if (v == null) v = defaultValue;
        if (v == null) throw new ConfigurationException("Required config '" + key + "' not set");
        return v;
    }
    public static String get(String key) { return get(key, null); }
}

// CHECKSTYLE RULE — prevent static WebDriver
/*
<module name="Regexp">
  <property name="format" value="static\s+WebDriver\s+\w+\s*;"/>
  <property name="illegalPattern" value="true"/>
  <property name="message" value="Static WebDriver fields are not thread-safe.
    Use DriverFactory.getDriver() instead."/>
</module>
*/

// PR TEMPLATE CHECKLIST (enforced)
// New test file MUST:
// 1. Extend BaseTest
// 2. Use DriverFactory.getDriver() — never new ChromeDriver()
// 3. Use AppWait — never Thread.sleep() or implicitlyWait
// 4. Use API factory for test data setup — never create via UI in @BeforeEach
// 5. Include @AfterEach cleanup via API delete
// 6. Follow naming: FeatureNameTest.java / testActionExpectedOutcome()
```

### Real-world Usage
- **10-developer team**: Framework core owned by 2 SDET leads; page objects owned by feature squads; tests written by same devs as production code (shift-left model)
- **Onboarding**: New developer pairs with lead for 1 day, writes first test in < 4 hours by following templates
- **PR review automation**: Checkstyle flags `Thread.sleep`, `new ChromeDriver()`, missing cleanup patterns — CI fails the PR before human review
- **Parallel execution**: 80 tests in 8 parallel threads; `ThreadLocal` ensures zero interference; CI time 15 min vs 2 hours sequential

### Common Mistakes
- No code review standards for tests — test code quality degrades faster than production code (lower scrutiny, no feature owner)
- Monolithic page objects — 2000-line `CheckoutPage` edited by 5 developers simultaneously → constant merge conflicts; use component objects for sub-sections
- Framework changes breaking all tests — no versioning of `framework-core`; treat it as an internal library with semantic versioning and changelog
- No unit tests for the framework itself — `AppWait`, `DriverFactory`, `HealingLocator` need unit tests; bugs discovered before propagating to 800 test files

### Debugging Strategy
1. 20% of suite fails after framework update → regression in `framework-core` — versioned library allows rollback in one line
2. Developer bypasses `BaseTest` → no screenshot on failure, no MDC context, no retry — enforce via Checkstyle parent class requirement
3. Parallel tests intermittently fail locally but pass on Grid → local resource contention (ports, display) — switch to headless for local parallel runs
4. Page object merge conflicts every sprint → refactor to component model — each component is a separate file owned by the team that built the feature

### Interview Trap
"How do you prevent test code from becoming unmaintainable?" — Three mechanisms: **enforced conventions** (Checkstyle + PR templates), **separation of concerns** (framework-core vs page objects vs tests as separate modules), and **treating test code as production code** — code reviews, refactoring time in sprints, pair programming. Root cause of unmaintainable tests is organizational: test code treated as second-class, reviewed less rigorously, refactored never.

### Follow-up Questions
1. How do you handle a frontend refactor that breaks 200 page objects simultaneously?
2. How do you balance framework flexibility (teams need customization) vs consistency (everything must work the same way)?

### Selenium vs Playwright
Playwright's built-in fixtures (`test.describe`, `test.use`, fixture pattern) provide the same structure as a custom `BaseTest` class — baked into the framework. The fixture model enforces test isolation, setup/teardown, and context reuse without team discipline. Playwright's TypeScript support gives compile-time safety that Java Selenium achieves through Checkstyle rules.

---

## Q41: How do you handle test observability — logging, tracing, and distributed test monitoring?

### Interview Answer
Test observability means having enough context to diagnose a failure **without re-running it**. The three pillars: **logs** (structured, with test/thread/browser context via MDC), **screenshots + video** (captured automatically on failure), and **traces** (Selenium event log + network HAR). In distributed CI runs, correlate failures to specific node, browser, thread, and test ID via a correlation ID injected at test start.

### Deep Explanation
**What "observable" means in practice:**
- DevOps CI job fails at 2 AM — no one is watching
- Next morning, engineer opens Allure report → sees screenshot of exact failure state
- Checks structured log → sees MDC fields: `testId=checkout_pay_001`, `thread=worker-3`, `browser=chrome-124`, `node=ci-agent-5`
- Checks HAR → sees network call `POST /payment` returned 503 — root cause found in 60 seconds without re-run

**Observability stack:**
```
Structured Logging   → SLF4J + Logback + MDC (correlation IDs)
Event Decoration     → EventFiringDecorator (log every WebDriver action)
Screenshots          → TakesScreenshot on failure (Allure attachment)
Video Recording      → Selenium Grid 4 video, or FFMPEG on CI agent
Network HAR          → BrowserMob Proxy or CDP Network.enable
Distributed Tracing  → OpenTelemetry spans across test + API calls
Reporting            → Allure + CI artifact publishing
```

### Java Example
```java
// STRUCTURED LOGGING WITH MDC
// Logback pattern: %d{ISO8601} [%X{testId}] [%X{browser}] [%X{thread}] %-5level %msg%n
public class DriverExtension implements BeforeEachCallback, AfterEachCallback, TestWatcher {

    @Override
    public void beforeEach(ExtensionContext ctx) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("testId", ctx.getDisplayName());
        MDC.put("correlationId", correlationId);
        MDC.put("browser", ConfigReader.get("browser", "chrome"));
        MDC.put("thread", Thread.currentThread().getName());
        MDC.put("environment", ConfigReader.get("env", "staging"));
        log.info("=== TEST START: {} [corrId={}] ===", ctx.getDisplayName(), correlationId);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        log.info("=== TEST END: {} ===", ctx.getDisplayName());
        DriverFactory.quitDriver();
        MDC.clear();
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        WebDriver driver = DriverFactory.getDriver();
        log.error("TEST FAILED: {} — {}", ctx.getDisplayName(), cause.getMessage());

        // Screenshot
        if (driver instanceof TakesScreenshot ts) {
            byte[] png = ts.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("Failure Screenshot", "image/png",
                new ByteArrayInputStream(png), ".png");
        }
        // Page source
        Allure.addAttachment("Page Source at Failure", driver.getPageSource());
        // Browser console logs
        LogEntries consoleLogs = driver.manage().logs().get(LogType.BROWSER);
        String consoleOutput = consoleLogs.getAll().stream()
            .map(e -> "[" + e.getLevel() + "] " + e.getMessage())
            .collect(Collectors.joining("\n"));
        Allure.addAttachment("Browser Console Logs", consoleOutput);
        // Current URL
        Allure.addAttachment("URL at Failure", driver.getCurrentUrl());
    }
}

// EVENT FIRING DECORATOR — log every Selenium action
public class ObservableDriverFactory {
    public static WebDriver createObservableDriver(String browser) {
        WebDriver raw = createRawDriver(browser);
        WebDriverListener listener = new WebDriverListener() {
            @Override
            public void beforeClick(WebElement element) {
                log.debug("CLICK → {} [text='{}']",
                    safeGetCss(element), safeGetText(element));
            }
            @Override
            public void afterNavigateTo(String url, WebDriver driver) {
                log.info("NAVIGATE → {}", url);
            }
            @Override
            public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
                log.debug("TYPE → {} [value='{}']",
                    safeGetCss(element), maskIfSensitive(keysToSend));
            }
            @Override
            public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
                log.error("WEBDRIVER ERROR in {}({}): {}",
                    method.getName(), Arrays.toString(args), e.getCause().getMessage());
            }
        };
        return new EventFiringDecorator<>(listener).decorate(raw);
    }
}

// HAR CAPTURE — network traffic for every test
public class HarCaptureExtension implements BeforeEachCallback, AfterEachCallback {
    private BrowserMobProxy proxy;

    @Override
    public void beforeEach(ExtensionContext ctx) {
        proxy = new BrowserMobProxyServer();
        proxy.start(0);
        proxy.newHar(ctx.getDisplayName());
        // Driver must be created with proxy configured in ChromeOptions
        ProxyContext.set(proxy);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        Har har = proxy.getHar();
        // Attach HAR to Allure report
        try {
            String harJson = new ObjectMapper().writeValueAsString(har);
            Allure.addAttachment("Network HAR", "application/json", harJson);
        } catch (Exception e) {
            log.warn("Could not attach HAR: {}", e.getMessage());
        }
        proxy.stop();
    }
}

// ALLURE STEP LOGGING — granular test steps in report
@Step("Login as {role}")
public DashboardPage loginAs(String role) {
    log.info("Logging in as role={}", role);
    driver.findElement(EMAIL_INPUT).sendKeys(credentials.email(role));
    driver.findElement(PASSWORD_INPUT).sendKeys(credentials.password(role));
    driver.findElement(SUBMIT_BTN).click();
    return new DashboardPage(driver);
}

// OPENTELEMETRY SPAN — correlate test with backend traces
public class TraceableTest extends BaseTest {
    private Span testSpan;

    @BeforeEach
    void startTrace(TestInfo info) {
        testSpan = tracer.spanBuilder("selenium.test")
            .setAttribute("test.name", info.getDisplayName())
            .setAttribute("test.browser", ConfigReader.get("browser", "chrome"))
            .setAttribute("test.env", ConfigReader.get("env", "staging"))
            .startSpan();
        try (Scope scope = testSpan.makeCurrent()) {
            MDC.put("traceId", testSpan.getSpanContext().getTraceId());
        }
    }

    @AfterEach
    void endTrace() {
        if (testSpan != null) testSpan.end();
        MDC.remove("traceId");
    }
}
```

### Real-world Usage
- **Nightly regression (500 tests across 10 Grid nodes)**: Every failure has screenshot + console logs + HAR in Allure — DevOps debugs in 2 minutes vs 30-minute re-run
- **Flaky test triage**: MDC `correlationId` links log lines across browser, API, and test layers — identifies that flakiness correlates with specific Grid node having high CPU
- **OpenTelemetry integration (microservices)**: Test span ID injected as request header → backend distributed trace shows exact DB query that timed out during the test
- **Video on demand**: Selenium Grid 4 records MP4 per session — only reviewed on failure (URL embedded in Allure report)

### Common Mistakes
- Logging without MDC — plain log lines unreadable when 20 parallel tests write simultaneously; MDC correlation is mandatory
- Capturing screenshots only at test end — multi-step failures need screenshots at each major step; use `@Step` with Allure screenshot listener
- Not clearing MDC in `afterEach` — MDC leaks to thread pool's next test (wrong context, confusing logs)
- Attaching full page source always — for 500 tests, page sources can total 2GB in report; only attach on failure

### Debugging Strategy
1. Failure shows blank screenshot → driver already quit before screenshot captured → move screenshot to `testFailed` called before `afterEach`
2. Logs from multiple tests interleaved unreadably → missing MDC pattern in logback.xml — add `%X{testId}` and `%X{thread}` to pattern
3. HAR shows no requests → proxy not configured in ChromeOptions → ensure `proxy.asSeleniumProxy()` used in ChromeOptions at driver creation
4. Allure report generated but empty → `allure-results/` directory not in report path; check Maven Surefire `argLine` includes Allure agent

### Interview Trap
"What do you do when a test fails in CI but you can't reproduce it locally?" — The answer demonstrates observability maturity: **first look at the Allure report** (screenshot + console logs + HAR + MDC-correlated log lines). If not enough: **enable CDP tracing** on that test specifically and re-run on CI with video. If still not reproducible: **add `@Step` granularity** to narrow the exact line. Never just run it again and hope — that's a symptom of insufficient observability.

### Follow-up Questions
1. How would you implement distributed tracing that links a Selenium test action to the exact backend database query it triggered?
2. How do you manage Allure report storage and retention in a CI system running 500 tests daily?

### Selenium vs Playwright
Playwright's `--trace on` records a full execution trace (screenshots, network, console, DOM snapshots) viewable in Playwright Trace Viewer — a single file capturing everything an Allure + HAR + MDC stack provides in Selenium. `npx playwright show-trace trace.zip` opens an interactive timeline. Playwright's observability is first-class and zero-config.

---

## Q42: How do you handle test data isolation in parallel test execution?

### Interview Answer
Test data isolation in parallel execution means **each test thread owns exclusively the data it operates on** — no two threads read-modify-write the same record. Strategies: **unique data per test** (UUID-based identifiers), **data partitioning** (pre-created pools assigned per thread), **database transactions with rollback** (for integration-level isolation), and **API-created + API-deleted** lifecycle with cleanup registration.

### Deep Explanation
**Isolation failure modes in parallel:**
- Thread-1 creates user `test@example.com`; Thread-2 tries to create same user → unique constraint violation
- Thread-1 adds item to shared cart; Thread-2 reads cart → sees Thread-1's items, assertion fails
- Thread-1 deletes a shared product; Thread-2 navigates to product page → 404, test fails
- Thread-1 modifies a shared "test admin" account settings; Thread-3 relies on default settings → wrong state

**Isolation patterns:**
```
UUID-based data    → Each test creates data with random ID — no collision possible
DB transaction     → Begin transaction in @BeforeEach, rollback in @AfterEach (no cleanup needed)
Tenant-per-test    → SaaS apps: create tenant per test, delete after — total isolation
Data partitioning  → Pre-created pool of N users, assign 1 per thread
Schema-per-test    → H2/PostgreSQL: create schema per test, drop after (integration tests)
```

### Java Example
```java
// PATTERN 1: UUID-BASED UNIQUE DATA (most common)
public class TestDataFactory {
    private static final ThreadLocal<List<Runnable>> cleanupTasks = ThreadLocal.withInitial(ArrayList::new);

    public static TestUser createUniqueUser(String role) {
        // UUID suffix guarantees no collision across any number of parallel threads
        String uniqueEmail = "test." + role.toLowerCase() + "." +
            UUID.randomUUID().toString().substring(0, 8) + "@testdomain.com";

        TestUser user = UserApiFactory.createUser(uniqueEmail, role);
        // Register cleanup — runs in @AfterEach regardless of pass/fail
        registerCleanup(() -> UserApiFactory.deleteUser(user.id()));
        return user;
    }

    public static TestProduct createUniqueProduct(String category) {
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TestProduct product = ProductApiFactory.createProduct(sku, category, 99.99);
        registerCleanup(() -> ProductApiFactory.deleteProduct(product.id()));
        return product;
    }

    public static void registerCleanup(Runnable cleanup) {
        cleanupTasks.get().add(cleanup);
    }

    // Called in @AfterEach — runs all cleanups for this thread's test
    public static void runCleanups() {
        List<Runnable> tasks = new ArrayList<>(cleanupTasks.get());
        cleanupTasks.get().clear();
        // Run in reverse order (LIFO — delete child before parent)
        Collections.reverse(tasks);
        tasks.forEach(task -> {
            try { task.run(); }
            catch (Exception e) { log.warn("Cleanup failed: {}", e.getMessage()); }
        });
    }
}

// BASE TEST — cleanup integration
public abstract class BaseTest {
    @AfterEach
    void cleanupTestData() {
        TestDataFactory.runCleanups();
    }
}

// PATTERN 2: DATA PARTITIONING — pre-created pool
public class UserPool {
    private static final BlockingQueue<TestUser> adminPool = new LinkedBlockingQueue<>();

    static {
        // Pre-create 20 admin users at suite start
        IntStream.range(0, 20)
            .mapToObj(i -> UserApiFactory.createUser("admin-pool-" + i + "@test.com", "ADMIN"))
            .forEach(adminPool::offer);
    }

    // Acquire admin — blocks if pool is exhausted (waits for another thread to release)
    public static TestUser acquireAdmin() throws InterruptedException {
        return adminPool.poll(30, TimeUnit.SECONDS);
    }

    // Return to pool after test
    public static void releaseAdmin(TestUser user) {
        // Reset admin state before returning
        AdminApiClient.resetToDefaults(user.id());
        adminPool.offer(user);
    }

    @AfterSuite
    public static void deletePool() {
        adminPool.forEach(u -> UserApiFactory.deleteUser(u.id()));
    }
}

// USAGE IN TEST
class AdminSettingsTest extends BaseTest {
    private TestUser admin;

    @BeforeEach
    void setUp() throws InterruptedException {
        admin = UserPool.acquireAdmin(); // guaranteed exclusive access
        DriverFactory.initDriver("chrome");
        loginAs(admin);
    }

    @Test
    void adminCanUpdateEmailSettings() {
        // Safe to modify — no other thread has this admin
        new AdminSettingsPage(driver()).setEmailNotifications(true);
        assertThat(new AdminSettingsPage(driver()).isEmailEnabled()).isTrue();
    }

    @AfterEach
    void releaseAdmin() {
        UserPool.releaseAdmin(admin); // reset + return for next test
        DriverFactory.quitDriver();
    }
}

// PATTERN 3: TENANT-PER-TEST (SaaS apps)
public class TenantFactory {
    public static TestTenant createIsolatedTenant() {
        String tenantId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        TestTenant tenant = TenantApiClient.create(tenantId);
        // Creates admin user + sample data automatically (seeded tenant)
        TestDataFactory.registerCleanup(() -> TenantApiClient.delete(tenantId));
        return tenant;
    }
}

class TenantTest extends BaseTest {
    private TestTenant tenant;

    @BeforeEach
    void setUp() {
        tenant = TenantFactory.createIsolatedTenant();
        // Tenant has its own URL, data, and admin — zero interference with other threads
        driver().get("https://" + tenant.subdomain() + ".example.com");
        loginAs(tenant.adminUser());
    }
    // Tests run in completely isolated tenant — delete tenant in @AfterEach via registered cleanup
}
```

### Real-world Usage
- **UUID-based isolation (e-commerce, 200 parallel tests)**: Every test creates its own user with `test.customer.a3f8b2@testdomain.com` — no collision across all threads, all CI runs, all environments
- **Admin pool (settings tests)**: 5 pre-created admins for 40 settings tests; pool limits to 5 parallel settings tests — prevents shared admin corruption while still running fast
- **Tenant-per-test (SaaS)**: Each of 100 parallel tests gets its own isolated tenant with own database schema — maximum isolation; teardown deletes the entire tenant record set
- **Cleanup registration**: Even when test fails mid-way, registered cleanups run in `@AfterEach` — no orphaned data accumulating in database

### Common Mistakes
- Shared `static TestUser admin` across tests — first test that modifies the admin breaks all subsequent tests in parallel
- Running cleanup only on test success — orphaned data accumulates; always run in `@AfterEach` (not `@AfterEach` conditional on pass)
- Not cleaning up on CI failure — after 100 CI runs with no cleanup, database has 100,000 test records → `OutOfMemoryError` or `TimeoutException` from slow queries
- Using sequential IDs instead of UUIDs — `test-user-1`, `test-user-2`: as soon as two CI pipelines run simultaneously, both create `test-user-1` → conflict

### Debugging Strategy
1. Unique constraint violation in parallel run → two threads creating data with same key — ensure UUID or timestamp suffix on all unique fields
2. Test passes alone, fails in parallel → shared static test data being mutated — audit all `static` non-final fields in test classes
3. Pool `acquireAdmin()` times out after 30s → pool too small for parallelism level — increase pool size or reduce parallel threads for that test class
4. Orphaned data accumulating in DB → cleanup not running after failure — move cleanup to `@AfterEach` (always runs) and add try/catch per cleanup task

### Interview Trap
"What happens when a test fails mid-way through — does the test data get cleaned up?" — The answer tests lifecycle understanding. Cleanup in `@AfterTest` / `@AfterEach` always runs regardless of test result. The key design requirement: **register cleanup immediately when data is created**, not at the end of the test. If the test fails after creating user but before reaching the cleanup registration line at the end, the data leaks. The `TestDataFactory.registerCleanup()` pattern solves this by registering cleanup atomically with creation.

### Follow-up Questions
1. How do you handle test data cleanup when a CI pipeline is forcibly killed mid-execution?
2. How do you test error scenarios that require specific database state (e.g., "user account locked after 5 failed attempts")?

### Selenium vs Playwright
Playwright's `browserContext` approach means browser-level state is automatically isolated per test context. However, **backend test data isolation** is framework-agnostic — UUID-based creation, cleanup registration, and partitioned pools apply identically to both Selenium and Playwright suites.

---

## Q43: How do you implement visual regression testing with Selenium?

### Interview Answer
Visual regression testing detects **unintended UI changes** — pixel-level or perceptual diffs between a baseline screenshot and current state. Selenium captures screenshots; a comparison library (Applitools Eyes, Percy, or AShot) computes the diff. The key challenge is handling **dynamic content** (timestamps, ads, user-specific data) without generating false positives.

### Deep Explanation
**Why visual testing alongside functional testing:**
- Functional tests verify behavior — clicking a button produces the right result
- Visual tests verify appearance — the button is in the right place, the right color, the right size
- A CSS change can break layout without breaking any functional assertion
- Production bugs: button hidden behind overlay, text truncated, mobile layout broken at 375px

**Visual testing approaches:**

| Approach | Tool | Pros | Cons |
|---|---|---|---|
| Pixel-by-pixel diff | AShot, custom | Free, deterministic | Flaky from antialiasing, fonts |
| Perceptual diff | ImageMagick SSIM | Tolerant of rendering noise | Threshold tuning needed |
| AI-powered diff | Applitools Eyes | Smart ignore of dynamic content | Costly, cloud dependency |
| DOM snapshot diff | Percy | Component-level diffing | Requires Percy SDK |

### Java Example
```java
// APPROACH 1: ASHOT — open source screenshot + diff
// pom.xml: ru.yandex.qatools.ashot:ashot:1.5.4

public class VisualTestUtils {
    private static final double DIFF_THRESHOLD_PERCENT = 0.5; // 0.5% pixel diff allowed
    private static final Path BASELINE_DIR = Paths.get("src/test/resources/visual-baselines");
    private static final Path DIFF_DIR = Paths.get("target/visual-diffs");

    // CAPTURE FULL-PAGE SCREENSHOT (handles scrolling)
    public static Screenshot captureFullPage(WebDriver driver) {
        return new AShot()
            .shootingStrategy(ShootingStrategies.viewportPasting(100)) // scroll 100px at a time
            .takeScreenshot(driver);
    }

    // CAPTURE ELEMENT SCREENSHOT
    public static Screenshot captureElement(WebDriver driver, By locator) {
        WebElement element = driver.findElement(locator);
        return new AShot()
            .shootingStrategy(ShootingStrategies.simple())
            .takeScreenshot(driver, element);
    }

    // COMPARE AGAINST BASELINE
    public static void assertMatchesBaseline(Screenshot actual, String testName) throws IOException {
        Path baselinePath = BASELINE_DIR.resolve(testName + ".png");
        Files.createDirectories(DIFF_DIR);

        if (!Files.exists(baselinePath)) {
            // First run — save as baseline
            ImageIO.write(actual.getImage(), "PNG", baselinePath.toFile());
            log.info("Visual baseline saved: {}", baselinePath);
            return;
        }

        // Load baseline
        BufferedImage baselineImage = ImageIO.read(baselinePath.toFile());
        Screenshot baseline = new Screenshot(baselineImage);

        // Compute diff
        ImageDiff diff = new ImageDiffer().makeDiff(baseline, actual);
        double diffPercent = (double) diff.getDiffSize() /
            (actual.getImage().getWidth() * actual.getImage().getHeight()) * 100;

        if (diffPercent > DIFF_THRESHOLD_PERCENT) {
            // Save diff image for review
            Path diffPath = DIFF_DIR.resolve(testName + "-diff.png");
            ImageIO.write(diff.getMarkedImage(), "PNG", diffPath.toFile());
            Allure.addAttachment("Visual Diff", "image/png",
                new FileInputStream(diffPath.toFile()), ".png");
            fail(String.format(
                "Visual regression detected for '%s': %.2f%% pixels differ (threshold: %.2f%%)%n" +
                "Diff saved to: %s", testName, diffPercent, DIFF_THRESHOLD_PERCENT, diffPath));
        }
    }

    // IGNORE DYNAMIC REGIONS — masks over areas with changing content
    public static Screenshot captureWithIgnoredRegions(
            WebDriver driver, List<By> dynamicLocators) {
        List<WebElement> elements = dynamicLocators.stream()
            .map(driver::findElement)
            .collect(Collectors.toList());
        return new AShot()
            .shootingStrategy(ShootingStrategies.viewportPasting(100))
            .addIgnoredElements(new HashSet<>(elements))  // mask these areas
            .takeScreenshot(driver, new HashSet<>(elements));
    }
}

// VISUAL TEST CLASS
class CheckoutVisualTest extends BaseTest {
    private static final By TIMESTAMP_AREA = By.cssSelector("[data-testid='order-timestamp']");
    private static final By AD_BANNER      = By.cssSelector(".ad-banner");

    @Test
    void checkoutPageMatchesBaseline() throws IOException {
        loginAs("CUSTOMER");
        navigateTo("/checkout");
        new AppWait(driver()).untilDocumentReady();

        // Ignore dynamic regions (timestamp, ads)
        Screenshot screenshot = VisualTestUtils.captureWithIgnoredRegions(
            driver(), List.of(TIMESTAMP_AREA, AD_BANNER));
        VisualTestUtils.assertMatchesBaseline(screenshot, "checkout-page");
    }

    @Test
    void mobileLayoutMatchesBaseline() throws IOException {
        // Set mobile viewport
        ((ChromeDriver) driver()).executeCdpCommand(
            "Emulation.setDeviceMetricsOverride", Map.of(
                "width", 375, "height", 812,
                "deviceScaleFactor", 2, "mobile", true));

        navigateTo("/checkout");
        new AppWait(driver()).untilDocumentReady();
        Screenshot screenshot = VisualTestUtils.captureFullPage(driver());
        VisualTestUtils.assertMatchesBaseline(screenshot, "checkout-page-mobile-375");
    }
}

// APPLITOOLS EYES — cloud AI visual testing
// pom.xml: com.applitools:eyes-selenium-java5:5.x
class ApplitoolsVisualTest extends BaseTest {
    private Eyes eyes;

    @BeforeEach
    void initEyes() {
        eyes = new Eyes();
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        eyes.setConfiguration(new Configuration()
            .setBatch(new BatchInfo("Regression Suite - " + LocalDate.now()))
            .setMatchLevel(MatchLevel.LAYOUT) // ignore text/colors, focus on layout
        );
    }

    @Test
    void dashboardLayoutIsConsistent() {
        eyes.open(driver(), "MyApp", "Dashboard Layout Test",
            new RectangleSize(1280, 800));
        navigateTo("/dashboard");
        new AppWait(driver()).untilDocumentReady();
        eyes.checkWindow("Dashboard"); // AI-powered baseline comparison
        eyes.closeAsync();
    }

    @AfterEach
    void closeEyes() {
        eyes.abortIfNotClosed();
    }
}
```

### Real-world Usage
- **Design system validation**: After updating shared component library, visual regression on all 50 pages catches unintended layout shifts before deployment
- **Mobile responsive testing**: Capture screenshots at 375px, 768px, 1024px, 1920px — assert each matches baseline; catches responsive breakpoint regressions
- **Release gate**: Visual tests run on staging before every deployment; diff > 1% on any critical page blocks deployment
- **Ignoring dynamic content**: Clock widgets, advertisement banners, user-specific names — masked from comparison with explicit `addIgnoredElements()`

### Common Mistakes
- Not masking dynamic regions — timestamps, random avatars, "Welcome, John" text change on every run, causing constant false positives
- Pixel-exact comparison on cross-platform CI — font rendering differs between macOS (local) and Linux (CI); use perceptual diff or run visual tests in Docker for consistency
- Running visual tests in parallel — screenshot captures can have timing issues; visual tests run sequentially or with strict wait-for-animation-complete guards
- No process for updating baselines — when intentional design change is deployed, baselines must be updated; lack of process means tests fail silently or are permanently disabled

### Debugging Strategy
1. Constant false positives on visually identical pages → font rendering or antialiasing difference → use AShot with `ImageDiffer.withDiffSizeTrigger()` threshold increase, or switch to perceptual diff
2. Diff shows hundreds of shifted pixels after correct refactor → update baseline using `UPDATE_BASELINE=true` system property that triggers save-instead-of-compare
3. Screenshot dimensions differ on CI vs local → viewport size not fixed → explicitly set `driver.manage().window().setSize(new Dimension(1280, 800))` in `@BeforeEach`
4. Applitools shows diff but layout looks identical → match level `EXACT` is too strict — switch to `LAYOUT` or `CONTENT` match level

### Interview Trap
"How is visual regression testing different from functional testing?" — Functional tests verify **what happens**; visual tests verify **how it looks**. A button can be clickable and produce the correct result (functional pass) while being invisible behind an overlay (visual fail). CSS bugs, layout breaks, z-index issues, and responsive failures are invisible to functional assertions but immediately caught by visual diff.

### Follow-up Questions
1. How would you handle a visual regression test suite when a new theme is rolled out across the entire application?
2. What is the difference between `EXACT`, `LAYOUT`, and `CONTENT` match levels in Applitools Eyes?

### Selenium vs Playwright
Playwright has built-in visual comparison: `expect(page).toHaveScreenshot('baseline.png', {maxDiffPixelRatio: 0.01})`. It handles screenshot storage, comparison, and diff reporting natively. Baselines are committed to git and updated with `--update-snapshots`. No third-party library needed for basic visual testing.

---

## Q44: How do you test Single Page Applications (SPAs) built with React, Angular, or Vue?

### Interview Answer
SPAs introduce unique challenges: **no full page reload between views**, **asynchronous data fetching after navigation**, **client-side routing without HTTP requests**, and **virtual DOM reconciliation causing element staleness**. The key strategy is **never assert immediately after navigation** — always wait for the framework's render cycle to complete before interacting.

### Deep Explanation
**SPA-specific failure modes:**

| Issue | Root Cause | Solution |
|---|---|---|
| `StaleElementReferenceException` after "navigation" | React/Vue re-renders component, invalidating DOM reference | Re-fetch element after each action |
| Element visible but empty | Component mounted before data fetched | Wait for data-loaded indicator or text content |
| URL changed but page still old | React Router / Vue Router is async | Wait for URL + DOM state, not just URL |
| Click has no effect | Event handler attached after initial render | Wait for JS hydration signal |
| Form submits but nothing happens | Controlled input — `sendKeys` doesn't trigger React synthetic event | Use JS `nativeInputValueSetter` or CDP |

**Framework detection:**
```javascript
window.angular   → Angular app
window.React     → React app
window.__VUE__   → Vue 3 app
window.ng        → Angular (modern)
window.Ember     → Ember.js
```

### Java Example
```java
// SPA-AWARE BASE PAGE
public abstract class SpaPage {
    protected final WebDriver driver;
    protected final AppWait wait;
    protected final JavascriptExecutor js;

    protected SpaPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new AppWait(driver);
        this.js = (JavascriptExecutor) driver;
    }

    // Wait for React to finish rendering
    protected void waitForReact() {
        wait.untilDocumentReady();
        // Wait for React root to be stable
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(10))
            .pollingEvery(Duration.ofMillis(100))
            .until(d -> {
                Object pending = js.executeScript(
                    "var r = document.getElementById('root');" +
                    "if (!r) return true;" +
                    "var fiber = r._reactRootContainer || r.__reactFiber$$;" +
                    "if (!fiber) return true;" +
                    "return typeof fiber.pendingLanes === 'number' " +
                    "  ? fiber.pendingLanes === 0 : true;");
                return Boolean.TRUE.equals(pending);
            });
    }

    // Wait for Angular to finish all pending async tasks
    protected void waitForAngular() {
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(15))
            .pollingEvery(Duration.ofMillis(200))
            .until(d -> Boolean.TRUE.equals(js.executeScript(
                "return window.getAllAngularTestabilities ? " +
                "window.getAllAngularTestabilities().every(t => t.isStable()) : true;")));
    }

    // Wait for Vue to finish rendering (Vue 3)
    protected void waitForVue() {
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(10))
            .pollingEvery(Duration.ofMillis(100))
            .until(d -> Boolean.TRUE.equals(js.executeScript(
                "return window.__VUE_APP__ ? " +
                "window.__VUE_APP__._instance.isMounted : true;")));
    }

    // SPA NAVIGATION WAIT — URL changes without HTTP request
    protected void waitForRoute(String expectedUrlFragment) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.urlContains(expectedUrlFragment));
        waitForReact(); // also wait for component to hydrate
    }

    // RE-FETCH ELEMENT — handles stale references after re-render
    protected WebElement freshElement(By locator) {
        return wait.untilVisible(locator); // fresh lookup every time
    }
}

// REACT CONTROLLED INPUT — sendKeys doesn't trigger synthetic events
public class ReactFormHelper {
    private final JavascriptExecutor js;

    public ReactFormHelper(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    // Trigger React synthetic onChange event for controlled inputs
    public void setReactInputValue(WebElement input, String value) {
        // Standard sendKeys
        input.clear();
        input.sendKeys(value);

        // Trigger React's synthetic event system
        js.executeScript(
            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(" +
            "  window.HTMLInputElement.prototype, 'value').set;" +
            "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            input, value);
    }

    // For React Select components (custom dropdowns)
    public void selectReactOption(By selectLocator, String optionText) {
        WebElement select = driver.findElement(selectLocator);
        select.click(); // opens dropdown
        // Options rendered dynamically after open
        new AppWait(driver).untilVisible(By.cssSelector("[class*='option']"));
        driver.findElements(By.cssSelector("[class*='option']")).stream()
            .filter(el -> el.getText().equals(optionText))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Option not found: " + optionText))
            .click();
    }
}

// SPA TEST EXAMPLE — React SPA
class ReactDashboardTest extends BaseTest {

    @Test
    void dashboardLoadsDataAfterNavigation() {
        loginAs("CUSTOMER");
        // React Router navigation — NOT a full page load
        navigateTo("/dashboard");
        SpaPage spaHelper = new SpaPage(driver()) {};
        spaHelper.waitForReact();

        // Wait for API data to load (not just component mount)
        new AppWait(driver())
            .untilVisible(By.cssSelector("[data-testid='dashboard-content']"));
        new AppWait(driver())
            .untilHasText(By.cssSelector("[data-testid='total-orders']"), "Orders: ");

        assertThat(new DashboardPage(driver()).getTotalOrders()).isGreaterThan(0);
    }

    @Test
    void reactControlledFormSubmitsCorrectly() {
        navigateTo("/profile/edit");
        ReactFormHelper reactForm = new ReactFormHelper(driver());

        WebElement nameInput = driver().findElement(By.cssSelector("[data-testid='display-name']"));
        reactForm.setReactInputValue(nameInput, "Updated Name"); // triggers React onChange

        driver().findElement(By.cssSelector("[data-testid='save-btn']")).click();

        new AppWait(driver()).untilHasText(
            By.cssSelector("[data-testid='success-toast']"), "Profile updated");
    }
}
```

### Real-world Usage
- **React SPA (FinTech dashboard)**: `waitForReact()` after every navigation + `untilNetworkIdle()` after every button click — eliminates 90% of timing failures compared to static `sleep(2000)` patterns
- **Angular 17 standalone components**: `waitForAngular()` leverages `ngZone.isStable()` — the most reliable way to know Angular has settled after all Observables, HTTP calls, and change detection cycles complete
- **Vue 3 Composition API**: State updates are async (`nextTick`); wait for specific DOM elements reflecting updated state, not just component mount
- **React controlled inputs**: E-commerce checkout with address autocomplete — `setReactInputValue()` triggers correct React state update, autocomplete fires correctly

### Common Mistakes
- Calling `driver.navigate().to(url)` for SPA routes — some SPAs break on full navigation; use `js.executeScript("window.location.href = arguments[0]", url)` or click the nav link instead
- `driver.findElement()` and caching the result — the element reference becomes stale after React re-renders; always call `findElement` fresh
- Using `Thread.sleep(2000)` for React hydration — hydration time varies; use framework-specific stability wait
- `clear()` + `sendKeys()` on React controlled input — `clear()` doesn't trigger React's synthetic event system; value appears entered but React state is still empty

### Debugging Strategy
1. `StaleElementReferenceException` on every second action → React component re-renders after first action — never cache element references; call `findElement` fresh each time
2. `sendKeys()` types correctly but form shows empty → controlled input not receiving React events — use `setReactInputValue()` with native setter dispatch
3. Test clicks nav link but page doesn't change → Angular router has guard that blocks navigation (auth guard) — check router guard preconditions
4. `waitForAngular()` times out → long-running Observable or timer not part of `ngZone` — identify with Angular DevTools; may need a different stable indicator

### Interview Trap
"Why does `element.sendKeys('value')` sometimes not work in React forms?" — Because React uses **synthetic events** and **controlled components**. When you `sendKeys`, the DOM value changes but React's synthetic `onChange` event is not triggered — React state remains as the previous value, overwriting the DOM input on next render. The fix: use the native `HTMLInputElement.value` setter + dispatch `change` event with `bubbles: true` to trigger React's event delegation layer.

### Follow-up Questions
1. How do you test a Next.js app that uses server-side rendering (SSR) followed by client-side hydration?
2. What is the difference in test strategy for a React app using React Query vs one using Redux for state management?

### Selenium vs Playwright
Playwright has `page.waitForLoadState('networkidle')` and `page.waitForFunction()` built in. Its auto-wait mechanism handles React/Angular hydration better by waiting for "actionability" (visible + stable + enabled) before every interaction. The `locator.fill()` triggers DOM `input` events correctly for React controlled inputs without custom JavaScript.

---

## Q45: How do you implement retry mechanisms and handle flaky tests systematically?

### Interview Answer
Flaky tests are tests that pass and fail non-deterministically on the same code. The correct response is **never accept flakiness** — diagnose root cause, fix it, and track recurrence. Retry is a short-term mitigation, not a solution. A systematic approach: **classify the flakiness cause**, **fix the root cause**, **add targeted waits or resilience**, and **quarantine persistent offenders** to prevent them from blocking CI.

### Deep Explanation
**Flakiness root cause taxonomy:**

| Category | Examples | Fix |
|---|---|---|
| Timing | `NoSuchElementException` on AJAX content | Explicit wait — `untilVisible()` after action |
| State pollution | Previous test left data behind | `@BeforeEach` cleanup + UUID isolation |
| Race condition | Parallel threads sharing driver | `ThreadLocal` driver isolation |
| Test order dependency | Test B relies on Test A's side effect | Make each test self-contained |
| Environment | CI machine overloaded, DNS slow | Investigate infrastructure; don't retry indefinitely |
| Third-party | Ad widget loads slowly | Mock or ignore third-party elements |
| Browser bug | Chrome version-specific rendering | Pin browser version, report upstream |

**Retry strategy:**
- Retry is **only valid** for genuine infrastructure flakiness (network hiccup, Grid node overload)
- Retry that masks real bugs is tech debt — test reports "pass" but code is broken
- Max retry: 2 times (total 3 attempts) — more than that signals the retry is hiding a real issue

### Java Example
```java
// JUNIT 5 RETRY EXTENSION
public class RetryExtension implements TestExecutionExceptionHandler {
    private static final int MAX_RETRIES = 2;
    private static final Map<String, Integer> retryCounts = new ConcurrentHashMap<>();

    @Override
    public void handleTestExecutionException(ExtensionContext ctx, Throwable throwable)
            throws Throwable {
        // Only retry for infrastructure-class exceptions
        if (!isRetryable(throwable)) {
            throw throwable; // AssertionError, config error → fail immediately
        }

        String testId = ctx.getUniqueId();
        int retryCount = retryCounts.getOrDefault(testId, 0);

        if (retryCount < MAX_RETRIES) {
            retryCounts.put(testId, retryCount + 1);
            log.warn("[RETRY {}/{}] {} — caused by: {}",
                retryCount + 1, MAX_RETRIES,
                ctx.getDisplayName(), throwable.getMessage());

            // Quit and recreate driver for clean state
            try { DriverFactory.quitDriver(); } catch (Exception ignored) {}
            DriverFactory.initDriver(ConfigReader.get("browser", "chrome"));

            // Re-invoke the test method via reflection
            Method testMethod = ctx.getRequiredTestMethod();
            Object testInstance = ctx.getRequiredTestInstance();
            try {
                testMethod.invoke(testInstance);
            } catch (InvocationTargetException e) {
                retryCounts.put(testId, MAX_RETRIES); // don't retry again
                throw e.getCause();
            }
        } else {
            retryCounts.remove(testId);
            log.error("[RETRY EXHAUSTED] {} failed after {} attempts",
                ctx.getDisplayName(), MAX_RETRIES + 1);
            throw throwable;
        }
    }

    private boolean isRetryable(Throwable t) {
        return t instanceof WebDriverException ||         // driver crash, session lost
               t instanceof TimeoutException ||           // page load timeout
               t instanceof StaleElementReferenceException || // DOM refresh
               (t instanceof RuntimeException &&
                t.getMessage() != null &&
                t.getMessage().contains("Connection refused")); // Grid node down
        // NOT retryable: AssertionError, NullPointerException, ConfigurationException
    }
}

// TESTNG RETRY ANALYZER
public class TestNgRetryAnalyzer implements IRetryAnalyzer {
    private static final int MAX_RETRIES = 2;
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        String key = result.getMethod().getMethodName();
        int count = counts.getOrDefault(key, 0);
        if (count < MAX_RETRIES) {
            counts.put(key, count + 1);
            log.warn("[RETRY {}/{}] {}", count + 1, MAX_RETRIES, key);
            return true;
        }
        return false;
    }
}

// FLAKINESS TRACKER — detect and report patterns
public class FlakinessTracker {
    private static final ConcurrentHashMap<String, FlakeRecord> flakeMap =
        new ConcurrentHashMap<>();

    public static void recordFlake(String testName, Throwable cause) {
        flakeMap.compute(testName, (k, v) -> {
            FlakeRecord r = v != null ? v : new FlakeRecord(testName);
            r.increment(cause.getClass().getSimpleName(), cause.getMessage());
            return r;
        });
    }

    @AfterSuite
    public static void publishFlakeReport() {
        if (flakeMap.isEmpty()) return;
        log.warn("=== FLAKINESS REPORT ===");
        flakeMap.entrySet().stream()
            .sorted(Comparator.comparingInt(e -> -e.getValue().totalFlakes()))
            .forEach(e -> {
                FlakeRecord r = e.getValue();
                log.warn("  {} — flaked {} times. Top cause: {}",
                    e.getKey(), r.totalFlakes(), r.topCause());
            });
        // Send to Slack + create JIRA tickets for tests that flaked > 3 times
        flakeMap.values().stream()
            .filter(r -> r.totalFlakes() >= 3)
            .forEach(r -> JiraClient.createFlakeTicket(r));
    }
}

// QUARANTINE ANNOTATION — isolate known flaky tests
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Quarantined {
    String jiraTicket();
    String reason();
}

// QUARANTINE EXTENSION — skip quarantined tests in main suite
public class QuarantineExtension implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
        return ctx.getElement()
            .flatMap(el -> Optional.ofNullable(el.getAnnotation(Quarantined.class)))
            .map(q -> {
                boolean runQuarantined = Boolean.getBoolean("run.quarantined");
                if (runQuarantined) return ConditionEvaluationResult.enabled("Quarantine override");
                return ConditionEvaluationResult.disabled(
                    "QUARANTINED: " + q.reason() + " [" + q.jiraTicket() + "]");
            })
            .orElse(ConditionEvaluationResult.enabled("Not quarantined"));
    }
}

// USAGE
@Test
@Quarantined(jiraTicket = "AUTO-234", reason = "Flaky due to third-party ad widget timing")
void paymentPageWithAdBanner() {
    // This test is excluded from main suite until AUTO-234 is fixed
    // Runs separately in quarantine suite via -Drun.quarantined=true
}
```

### Real-world Usage
- **Retry policy**: 2 retries for `WebDriverException` and `TimeoutException` only; `AssertionError` never retries — failing assertion = real bug
- **Flakiness dashboard**: CI publishes JSON report of flake rates per test per week; any test > 5% flake rate gets a mandatory fix sprint
- **Quarantine suite**: 8 known external-dependency flaky tests run nightly in separate `quarantine` pipeline with `-Drun.quarantined=true` — don't block main PRs
- **Root cause fix examples**: Fixed `StaleElementReferenceException` by removing cached element fields; fixed timing flakiness by adding `untilNetworkIdle()` after every AJAX-triggering click

### Common Mistakes
- Setting `MAX_RETRIES = 5` — passing after 5 retries means the underlying test is wrong 80% of the time; retry count > 2 is a smell
- Retrying `AssertionError` — means the application has a bug but the test is pretending it passed; this is the worst possible outcome
- Accepting flakiness as "normal for E2E tests" — flaky tests erode trust in the entire suite; engineers start ignoring CI failures
- No flakiness tracking — without data, the same tests flake for months without prioritization; track flake rate to prioritize fixes

### Debugging Strategy
1. Test flakes with `NoSuchElementException` → element appears sometimes → add `untilVisible()` wait before interaction
2. Flakiness only in parallel → shared state → audit static/shared fields; add UUID to test data
3. Flakiness only on CI, not locally → timing is different on slower CI machine → increase explicit wait timeouts (10s → 20s) on CI via config
4. Cannot reproduce after retry → genuine infrastructure flakiness (Grid node restart, DNS hiccup) → retry is correct mitigation; add infrastructure alerting

### Interview Trap
"How many retries should a test have?" — The principled answer: **the minimum needed to mask genuine infrastructure noise, not the maximum that makes tests appear green**. Two retries is the practical limit. A test retrying more than twice on every run is not "flaky" — it's broken. The right response to > 2 retries needed: quarantine the test, file a bug, and fix it. Using retry as a permanent pass mechanism destroys the value of the test suite.

### Follow-up Questions
1. How do you distinguish between a test that is genuinely flaky (infrastructure issue) vs one that is revealing an actual intermittent application bug?
2. How do you build a culture where flaky tests are fixed promptly rather than rerun and ignored?

### Selenium vs Playwright
Playwright has built-in retry via `retries: 2` in `playwright.config.ts`. It also has `--repeat-each` for deliberate flakiness detection. Playwright's auto-waiting significantly reduces flakiness at the source — fewer timing-based failures mean fewer retries needed. Its trace recording makes the root cause of any flake visible without reproducing it.

---

## Q46: How do you manage Selenium Grid 4 at scale — node configuration, session management, and observability?

### Interview Answer
Selenium Grid 4 is a **distributed microservices system**: Router, Distributor, Session Map, Session Queue, Event Bus, and Nodes. At scale (50+ nodes), you need: **dynamic node registration via Docker/Kubernetes**, **session queue tuning** to prevent capacity starvation, **per-node observability** (CPU, memory, active sessions), and **graceful drain** before node recycling. The Grid's REST API and GraphQL endpoint expose all management surfaces.

### Deep Explanation
**Grid 4 component responsibilities:**

| Component | Role |
|---|---|
| Router | Entry point — routes new session requests and existing session commands |
| Distributor | Assigns new session requests to the most suitable Node |
| Session Map | Stores mapping of session ID → Node URL |
| Session Queue | Buffers new session requests when all Nodes are at capacity |
| Event Bus | Pub/sub backbone — components communicate asynchronously |
| Node | Runs actual browser instances; registers capabilities with Distributor |

**Session lifecycle:**
```
Client → POST /session → Router → Session Queue (if at capacity)
                                 → Distributor (if capacity available)
                                 → Distributor selects Node by capability match
                                 → Node creates session, returns session ID
                                 → Session Map stores {sessionId → nodeUrl}
Subsequent commands → Router → Session Map lookup → direct to Node
```

**Scaling strategies:**
- **Docker Compose**: Fixed set of Node containers — simple, predictable
- **Kubernetes + HPA**: Nodes scale based on Session Queue depth — elastic, cost-efficient
- **Standalone mode**: Single JVM with all components — dev/local only, not production scale

### Java Example
```java
// REMOTE WEBDRIVER — connecting to Grid
public class GridDriverFactory {
    private static final String GRID_URL = System.getProperty("gridUrl",
        "http://selenium-grid:4444");

    public static WebDriver createRemoteDriver(String browser, String version) {
        Capabilities capabilities = switch (browser.toLowerCase()) {
            case "chrome" -> {
                ChromeOptions opts = new ChromeOptions();
                opts.setPlatformName("linux");
                opts.setBrowserVersion(version); // Grid selects matching Node
                opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                yield opts;
            }
            case "firefox" -> {
                FirefoxOptions opts = new FirefoxOptions();
                opts.setPlatformName("linux");
                opts.setBrowserVersion(version);
                yield opts;
            }
            case "edge" -> {
                EdgeOptions opts = new EdgeOptions();
                opts.setPlatformName("linux");
                yield opts;
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };

        try {
            return new RemoteWebDriver(new URL(GRID_URL), capabilities);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Grid URL: " + GRID_URL, e);
        }
    }
}

// GRID MANAGEMENT VIA REST API
public class GridManagerClient {
    private static final String GRID_URL = System.getProperty("gridUrl", "http://selenium-grid:4444");

    // GET GRID STATUS — nodes, sessions, capacity
    public GridStatus getStatus() {
        String response = RestAssured.given()
            .get(GRID_URL + "/status")
            .then().statusCode(200)
            .extract().asString();
        return parseGridStatus(response);
    }

    // GRAPHQL — richer queries
    public int getQueuedSessionCount() {
        String query = """
            { grid { sessionQueueSize } }
            """;
        return RestAssured.given()
            .contentType("application/json")
            .body(Map.of("query", query))
            .post(GRID_URL + "/graphql")
            .then().statusCode(200)
            .extract().path("data.grid.sessionQueueSize");
    }

    public List<NodeInfo> getActiveNodes() {
        String query = """
            {
              nodesInfo {
                nodes {
                  id
                  uri
                  status
                  sessionCount
                  maxSession
                  slotStereotypes { slots { id { id } session { capabilities } } }
                }
              }
            }
            """;
        // parse and return node list
        return RestAssured.given()
            .contentType("application/json")
            .body(Map.of("query", query))
            .post(GRID_URL + "/graphql")
            .then().extract()
            .jsonPath().getList("data.nodesInfo.nodes", NodeInfo.class);
    }

    // DRAIN NODE — graceful shutdown (finish active sessions, no new sessions)
    public void drainNode(String nodeId) {
        RestAssured.given()
            .post(GRID_URL + "/se/grid/node/{nodeId}/drain", nodeId)
            .then().statusCode(200);
        log.info("Node {} draining — waiting for active sessions to complete", nodeId);
    }

    // WAIT FOR GRID CAPACITY — before test suite starts
    public void waitForGridReady(int expectedNodes, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            GridStatus status = getStatus();
            if (status.isReady() && status.getNodeCount() >= expectedNodes) {
                log.info("Grid ready: {} nodes available", status.getNodeCount());
                return;
            }
            log.info("Grid not ready yet: {}/{} nodes. Waiting...", status.getNodeCount(), expectedNodes);
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
        }
        throw new RuntimeException("Grid did not reach " + expectedNodes +
            " nodes within " + timeout);
    }
}

// DOCKER COMPOSE — Grid with auto-scaling Node pool
/*
version: "3.9"
services:
  selenium-hub:
    image: selenium/hub:4.20.0
    ports: ["4444:4444"]
    environment:
      - SE_SESSION_QUEUE_CAPACITY=100
      - SE_SESSION_REQUEST_TIMEOUT=300
      - SE_DRAIN_AFTER_SESSION_COUNT=0

  chrome-node:
    image: selenium/node-chrome:4.20.0
    depends_on: [selenium-hub]
    environment:
      - SE_EVENT_BUS_HOST=selenium-hub
      - SE_EVENT_BUS_PUBLISH_PORT=4442
      - SE_EVENT_BUS_SUBSCRIBE_PORT=4443
      - SE_NODE_MAX_SESSIONS=4
      - SE_NODE_SESSION_TIMEOUT=300
      - SE_DRAIN_AFTER_SESSION_COUNT=50   # recycle node every 50 sessions (memory hygiene)
    shm_size: 2gb
    deploy:
      replicas: 5   # 5 nodes × 4 sessions = 20 concurrent Chrome sessions
*/

// KUBERNETES — elastic node scaling based on queue depth
/*
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: selenium-chrome-node-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: selenium-chrome-node
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: External
    external:
      metric:
        name: selenium_grid_session_queue_size  # custom metric from Grid GraphQL
      target:
        type: Value
        value: "4"   # scale up when queue > 4 waiting sessions
*/

// CI PRE-FLIGHT CHECK — verify Grid before test suite
@BeforeSuite(alwaysRun = true)
void waitForGridReady() {
    new GridManagerClient().waitForGridReady(
        Integer.parseInt(System.getProperty("gridNodes", "5")),
        Duration.ofMinutes(5));
}
```

### Real-world Usage
- **50-node Kubernetes Grid**: HPA scales Chrome nodes from 2 → 20 when CI pipeline triggers 200-test parallel run; scales back to 2 within 10 minutes of completion
- **`SE_DRAIN_AFTER_SESSION_COUNT=50`**: Each Node container auto-recycles after 50 browser sessions — prevents Chrome memory accumulation on long-running nodes
- **Queue capacity monitoring**: CI Slack alert when `sessionQueueSize > 20` for > 2 minutes — signals Grid undersized for current parallelism level
- **Node drain for rolling updates**: Before updating Grid version, drain all nodes gracefully — zero session interruption during maintenance

### Common Mistakes
- Not setting `SE_SESSION_REQUEST_TIMEOUT` — session request can wait indefinitely in queue; set to 300s and fail fast with descriptive error
- Using `latest` Docker tag for Grid images — breaking changes between releases; pin to exact version (`4.20.0-20240425`)
- Forgetting `shm_size: 2gb` for Chrome nodes — Chrome uses `/dev/shm` for shared memory; default 64MB causes tab crashes and random session failures
- Not setting `SE_NODE_MAX_SESSIONS` — defaults to 1; under-utilizes nodes; set to 4–6 per CPU core available

### Debugging Strategy
1. `Could not start a new session` — Grid returns 500 → check `SE_SESSION_QUEUE_CAPACITY` and node availability via `/status` endpoint
2. Session allocated but tests start sequentially instead of parallel → `RemoteWebDriver` creation not in parallel; check test framework parallel config (JUnit5 `junit-platform.properties`, TestNG suite XML)
3. Node shows `UP` in Grid but sessions fail → node container OOM-killed → increase container memory limit and `shm_size`
4. Tests intermittently get `NoSuchSessionException` → Grid session map is not persisting (Hub restarted) → use distributed Session Map backed by Redis for production

### Interview Trap
"What happens if the Selenium Grid Hub goes down mid-test run?" — In Grid 4's fully distributed mode, the Hub is stateless (Session Map is separate). Only new session requests fail. Active sessions continue because commands go **directly to the Node** after initial Session Map lookup. However, if Session Map is in-memory (default), it also goes down — active sessions lose their routing. **Production solution**: use Redis-backed Session Map for Grid Hub fault tolerance.

### Follow-up Questions
1. How would you implement a cost-saving strategy that automatically scales Grid nodes to zero when no tests are running?
2. What is the difference between Selenium Grid 3 (Hub-Node) and Grid 4's architecture? What key problems does Grid 4 solve?

### Selenium vs Playwright
Playwright Grid equivalent is **Playwright Workers** in its own test runner (`--workers=50`) — but for distributed execution across machines, teams use cloud services (BrowserStack, LambdaTest) or custom infrastructure. Playwright doesn't have a built-in distributed Grid — Selenium Grid 4 is still the standard for self-hosted parallel cross-browser testing at scale.

---

## Q47: How do you implement test environment management and configuration across multiple environments?

### Interview Answer
Production automation frameworks run tests across multiple environments (dev, staging, prod, performance) with **zero code changes** — only configuration changes. The pattern: **externalized config** (no hardcoded URLs, credentials, or feature flags in test code), **environment profiles** (Maven profiles or JVM system properties), and **secret management** (CI vault, never committed to git).

### Deep Explanation
**Configuration sources (priority order):**
```
1. JVM System Properties  -Dkey=value         (highest — CI/CLI override)
2. Environment Variables  SELENIUM_BASE_URL    (CI secrets, Docker env)
3. Properties File        config/staging.properties
4. Default values         hardcoded fallback in ConfigReader
```

**What gets configured per environment:**

| Config Key | Dev | Staging | Prod |
|---|---|---|---|
| `baseUrl` | `http://localhost:3000` | `https://staging.example.com` | `https://example.com` |
| `apiBaseUrl` | `http://localhost:8080` | `https://api-staging.example.com` | `https://api.example.com` |
| `gridUrl` | (local driver) | `http://grid-staging:4444` | `http://grid-prod:4444` |
| `defaultTimeout` | `10` | `20` | `30` |
| `browser` | `chrome` | `chrome` | `chrome,firefox,edge` |
| `headless` | `false` | `true` | `true` |

### Java Example
```java
// CONFIG READER — multi-source resolution
public class ConfigReader {
    private static final Properties props = loadPropertiesForEnvironment();

    private static Properties loadPropertiesForEnvironment() {
        String env = System.getProperty("env",
            System.getenv().getOrDefault("TEST_ENV", "staging"));
        Properties p = new Properties();
        // Load base config
        try (InputStream base = ConfigReader.class
                .getResourceAsStream("/config/base.properties")) {
            if (base != null) p.load(base);
        } catch (IOException e) { log.warn("No base.properties found"); }
        // Load env-specific config (overrides base)
        try (InputStream envConfig = ConfigReader.class
                .getResourceAsStream("/config/" + env + ".properties")) {
            if (envConfig != null) p.load(envConfig);
            else log.warn("No config/{}.properties found — using base only", env);
        } catch (IOException e) { /* ignored */ }
        log.info("Config loaded for environment: {}", env);
        return p;
    }

    public static String get(String key, String defaultValue) {
        // Priority: JVM prop → env var → properties file → default
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.toUpperCase().replace(".", "_"));
        if (v == null) v = props.getProperty(key);
        if (v == null) v = defaultValue;
        if (v == null) throw new ConfigurationException(
            "Required config '" + key + "' not set for env=" +
            System.getProperty("env", "staging"));
        return v;
    }

    public static String get(String key) { return get(key, null); }
    public static int getInt(String key, int def) {
        return Integer.parseInt(get(key, String.valueOf(def)));
    }
    public static boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(get(key, String.valueOf(def)));
    }
    public static Duration getDuration(String key, Duration def) {
        return Duration.ofSeconds(getInt(key, (int) def.toSeconds()));
    }
}

// PROPERTIES FILES — src/test/resources/config/
/*
base.properties:
browser=chrome
headless=true
defaultTimeoutSeconds=15
retryCount=2
screenshotOnFailure=true

dev.properties:
baseUrl=http://localhost:3000
apiBaseUrl=http://localhost:8080
headless=false
defaultTimeoutSeconds=10

staging.properties:
baseUrl=https://staging.example.com
apiBaseUrl=https://api-staging.example.com
gridUrl=http://selenium-grid-staging:4444
defaultTimeoutSeconds=20

prod.properties:
baseUrl=https://example.com
apiBaseUrl=https://api.example.com
gridUrl=http://selenium-grid-prod:4444
defaultTimeoutSeconds=30
browser=chrome,firefox
*/

// MAVEN PROFILES — pom.xml
/*
<profiles>
  <profile>
    <id>staging</id>
    <activation><activeByDefault>true</activeByDefault></activation>
    <properties>
      <env>staging</env>
      <gridUrl>http://grid-staging:4444</gridUrl>
    </properties>
  </profile>
  <profile>
    <id>prod</id>
    <properties>
      <env>prod</env>
      <gridUrl>http://grid-prod:4444</gridUrl>
    </properties>
  </profile>
</profiles>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <configuration>
        <systemPropertyVariables>
          <env>${env}</env>
          <gridUrl>${gridUrl}</gridUrl>
        </systemPropertyVariables>
      </configuration>
    </plugin>
  </plugins>
</build>
*/

// SECRET MANAGEMENT — never in properties files
public class SecretProvider {
    // In CI: injected as environment variable from vault
    // In local dev: .env file (gitignored) or system keychain

    public static String getDbPassword() {
        return requireNonNull(System.getenv("TEST_DB_PASSWORD"),
            "TEST_DB_PASSWORD env var not set — required for DB cleanup");
    }

    public static String getAdminApiKey() {
        return requireNonNull(System.getenv("TEST_ADMIN_API_KEY"),
            "TEST_ADMIN_API_KEY env var not set");
    }

    public static String getApplitoolsApiKey() {
        return requireNonNull(System.getenv("APPLITOOLS_API_KEY"),
            "APPLITOOLS_API_KEY env var not set");
    }
}

// GITHUB ACTIONS — multi-environment pipeline
/*
name: E2E Tests
on:
  push:
    branches: [main]
  workflow_dispatch:
    inputs:
      environment:
        type: choice
        options: [staging, prod]
        default: staging

jobs:
  e2e:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment || 'staging' }}
    env:
      TEST_ENV: ${{ github.event.inputs.environment || 'staging' }}
      TEST_ADMIN_API_KEY: ${{ secrets.ADMIN_API_KEY }}
      TEST_DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
    steps:
      - uses: actions/checkout@v4
      - name: Run E2E Tests
        run: mvn test -P${{ env.TEST_ENV }} -Denv=${{ env.TEST_ENV }}
      - name: Upload Allure Report
        uses: actions/upload-artifact@v4
        with:
          name: allure-results-${{ env.TEST_ENV }}-${{ github.run_id }}
          path: target/allure-results/
*/

// FEATURE FLAG OVERRIDES — per environment
public class FeatureFlags {
    public static boolean isEnabled(String flagName) {
        // Check test override first (allows enabling beta features in staging tests)
        String override = ConfigReader.get("feature." + flagName, null);
        if (override != null) return Boolean.parseBoolean(override);
        // Otherwise read from app API
        return FeatureFlagApiClient.isEnabled(flagName);
    }
}

// USAGE
@Test
void newCheckoutFlowWhenFeatureEnabled() {
    assumeTrue(FeatureFlags.isEnabled("new-checkout"),
        "Skipping: new-checkout feature flag not enabled in this environment");
    // test new checkout only when feature flag is on
}
```

### Real-world Usage
- **CI/CD matrix**: GitHub Actions runs staging tests on every PR; prod smoke tests on every deployment; performance tests weekly against prod-like environment
- **Secret rotation**: `TEST_ADMIN_API_KEY` rotated monthly; updating CI environment secret is the only change needed — zero code changes
- **Feature flag testing**: Staging environment has `feature.new-checkout=true` in staging.properties — new checkout tested there before flag is enabled in prod
- **Timeout calibration**: `defaultTimeoutSeconds=30` in prod config (slower network than staging) configured without code change; CI agent passes `-Denv=prod`

### Common Mistakes
- Hardcoding `https://staging.example.com` in test code — every environment change requires a code change; PR, review, and deploy cycle for a URL update
- Committing credentials to git — even in private repos; use `System.getenv()` + CI secrets for all sensitive values
- Using different test logic per environment — `if (env.equals("prod")) skip()` patterns lead to tests that don't actually verify prod behavior; tests should be environment-agnostic
- Not validating required config at startup — fail fast with descriptive `ConfigurationException` on missing keys rather than `NullPointerException` deep in test execution

### Debugging Strategy
1. Tests work locally but wrong URL in CI → `baseUrl` not passed via system property in CI command — check Maven Surefire `systemPropertyVariables` config
2. `ConfigurationException: Required config 'adminApiKey' not set` in CI → secret not added to CI environment → add to GitHub Actions environment secrets
3. Tests pick up wrong environment config → multiple `env` system properties set from different sources — add `log.info("Config loaded for: env={}", get("env"))` at startup to confirm
4. Feature flag test intermittently skips in prod → flag state changes between test runs — pin flag state via API before test, restore in cleanup

### Interview Trap
"How do you test in production without breaking it?" — Key answer: **read-only smoke tests** (no data mutations), **isolated test accounts** (`test+smoke@company.com` separate from real users), **feature flag discipline** (test only stable features), and **rate limiting awareness** (prod has tighter rate limits — use longer timeouts and fewer parallel sessions). Never run full regression in prod; run **targeted smoke tests** that verify critical paths without side effects.

### Follow-up Questions
1. How do you handle test configuration for a blue-green deployment where you need to test both environments before traffic switch?
2. How do you ensure secrets never leak into test logs even when DEBUG logging is enabled?

### Selenium vs Playwright
Playwright's `playwright.config.ts` `use` block and `projects` array provide the same multi-environment/multi-browser configuration in a single file. `process.env.BASE_URL` is the standard pattern. Playwright's TypeScript config is more expressive than Java properties files — but the configuration management principles are identical.

---

## Q48: How do you measure and optimize Selenium test suite execution time?

### Interview Answer
Suite execution time is owned by four factors: **browser startup cost** (3s per instance), **navigation/page load time** (1–5s per page), **wait time** (fixed sleeps add up massively), and **parallelism** (sequential execution is the single biggest time killer). Optimization order: first maximize parallelism, then eliminate `Thread.sleep`, then reduce browser startups via session reuse, then optimize wait timeouts.

### Deep Explanation
**Time audit breakdown (typical 500-test suite, sequential):**

| Cost Source | Per Test | 500 Tests | Optimized |
|---|---|---|---|
| Browser startup | 3s | 25 min | 2.5 min (class-scoped) |
| Login via UI | 5s | 41 min | 1.7 min (API token) |
| `Thread.sleep(2s)` × 3 | 6s | 50 min | 0 (explicit waits) |
| Page loads | 2s avg | 16 min | 16 min (irreducible) |
| Test logic | 3s avg | 25 min | 25 min |
| **Sequential total** | **19s** | **~2.7 hrs** | |
| **After optimization (20 parallel)** | | | **~15 min** |

**Optimization techniques by impact:**
1. **Parallelism** — biggest win; 20× speedup with 20 threads
2. **Eliminate `Thread.sleep`** — replace with explicit waits
3. **API login instead of UI login** — 5s → 0.2s per test requiring auth
4. **Class-scoped driver** — 3s startup amortized over 10 tests per class
5. **Headless mode** — 30–50% faster rendering, no display server overhead
6. **Optimize locators** — CSS > XPath; ID > attribute; avoid leading `//`
7. **Reduce navigation** — start test closer to target page; inject state instead of navigating through it
8. **Grid node scaling** — more nodes = more parallel capacity

### Java Example
```java
// EXECUTION TIME PROFILER — measure cost per phase
public class TestTimingExtension implements BeforeEachCallback, AfterEachCallback {
    private static final ConcurrentHashMap<String, Long> timings = new ConcurrentHashMap<>();
    private long startTime;

    @Override
    public void beforeEach(ExtensionContext ctx) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        long duration = System.currentTimeMillis() - startTime;
        String testName = ctx.getDisplayName();
        timings.put(testName, duration);

        if (duration > 30_000) { // flag tests > 30s
            log.warn("[SLOW TEST] {} took {}s — review for optimization opportunities",
                testName, duration / 1000);
        }
    }

    @AfterSuite
    public static void publishTimingReport() {
        if (timings.isEmpty()) return;
        DoubleSummaryStatistics stats = timings.values().stream()
            .mapToDouble(Long::doubleValue).summaryStatistics();
        log.info("=== TIMING REPORT ===");
        log.info("Total tests: {}", timings.size());
        log.info("Average: {}s", stats.getAverage() / 1000);
        log.info("Max: {}s", stats.getMax() / 1000);
        log.info("Min: {}s", stats.getMin() / 1000);

        // Top 10 slowest tests
        log.info("Top 10 slowest:");
        timings.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> log.info("  {} — {}s", e.getKey(), e.getValue() / 1000));
    }
}

// PARALLEL CONFIG — junit-platform.properties
/*
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=20
*/

// MAVEN SUREFIRE — parallel with thread groups
/*
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkCount>1</forkCount>
    <reuseForks>true</reuseForks>
    <argLine>-Dfile.encoding=UTF-8</argLine>
    <systemPropertyVariables>
      <junit.jupiter.execution.parallel.enabled>true</junit.jupiter.execution.parallel.enabled>
      <junit.jupiter.execution.parallel.config.fixed.parallelism>20</junit.jupiter.execution.parallel.config.fixed.parallelism>
    </systemPropertyVariables>
  </configuration>
</plugin>
*/

// DRIVER STARTUP OPTIMIZATION — minimize Chrome launch flags
public static ChromeOptions fastChromeOptions() {
    ChromeOptions opts = new ChromeOptions();
    opts.addArguments(
        "--headless=new",                    // headless: 30-50% faster
        "--no-sandbox",                      // required in Docker/CI
        "--disable-dev-shm-usage",           // prevents /dev/shm OOM
        "--disable-extensions",              // skip extension loading
        "--disable-gpu",                     // no GPU in headless
        "--disable-background-networking",   // reduce background network activity
        "--disable-default-apps",            // skip default app installs
        "--disable-sync",                    // skip Google sync
        "--no-first-run",                    // skip first-run wizard
        "--disable-backgrounding-occluded-windows",
        "--disable-renderer-backgrounding"
    );
    opts.setPageLoadStrategy(PageLoadStrategy.EAGER); // don't wait for images/fonts
    return opts;
}

// PAGE LOAD STRATEGY — EAGER vs NORMAL vs NONE
/*
NORMAL (default) → waits for load event (all resources)  — slowest
EAGER            → waits for DOMContentLoaded (DOM ready) — good for most SPAs
NONE             → returns immediately after navigation starts — use with explicit waits only
*/

// LOCATOR PERFORMANCE — fastest to slowest
/*
By.id("submit")                          → fastest (single hashtable lookup)
By.cssSelector("#submit")                → fast
By.cssSelector("[data-testid='submit']") → fast
By.cssSelector(".checkout-submit")       → fast
By.xpath("//button[@id='submit']")       → slower (XPath engine)
By.xpath("//button[contains(@class,'submit')]") → slower
By.xpath("//div/form/div[3]/button")     → slowest + brittle
*/

// BATCH API CALLS — avoid N+1 pattern in test setup
// BAD: loop creates 10 users with 10 sequential API calls
List<TestUser> users = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    users.add(UserApiFactory.createUser("CUSTOMER")); // 10 × 200ms = 2s
}

// GOOD: parallel API calls via CompletableFuture
List<TestUser> users2 = IntStream.range(0, 10)
    .mapToObj(i -> CompletableFuture.supplyAsync(
        () -> UserApiFactory.createUser("CUSTOMER")))
    .collect(Collectors.toList())
    .stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList()); // 200ms total (parallel)
```

### Real-world Usage
- **Suite time audit**: Profiling 500-test suite found 40 tests using `Thread.sleep(5000)` × 3 = 10 minutes wasted per run; replacing with explicit waits reduced suite time by 10 minutes
- **Headless switch**: Moving from headed to headless Chrome reduced average test time from 8s to 5.5s — 400 tests × 2.5s = 16 minutes saved per suite run
- **`PageLoadStrategy.EAGER`**: React SPA suite — DOM ready triggers before all lazy-loaded images; 500ms per navigation × 300 navigations = 2.5 minutes saved
- **Parallel API setup**: `@BeforeSuite` creates 50 users — 10 seconds sequential, 0.5 seconds with `CompletableFuture` parallel creation

### Common Mistakes
- Adding parallelism without fixing thread safety first — `ThreadLocal` driver isolation must be in place before increasing `parallelism` beyond 1
- `PageLoadStrategy.NONE` without disciplined explicit waits — test clicks before page renders → `NoSuchElementException` cascade; only use if you add `untilDocumentReady()` to every navigation
- Over-optimizing locators prematurely — `By.id` is 0.001s faster than `By.cssSelector`; focus on architectural optimization (parallelism, startup cost) before micro-optimizing locators
- Not measuring before optimizing — instrument with `TestTimingExtension` first; top 10 slowest tests often have obvious fixes (login via UI, `Thread.sleep`)

### Debugging Strategy
1. Suite time increased after adding tests → likely a new test with UI login loop in `@BeforeEach` — profile with timing extension, enforce API login rule
2. Parallelism increased but suite time didn't decrease → Grid is the bottleneck, not JVM threads — add more Grid nodes
3. Headless tests faster locally but same speed in CI → CI already runs headless Chrome via Xvfb; the flag was already effective — CI bottleneck may be Grid node CPU
4. `EAGER` strategy causes `NoSuchElementException` on image-dependent pages → some tests genuinely need full page load; override strategy per test class

### Interview Trap
"Your 500-test suite takes 3 hours. How do you get it to 30 minutes?" — Systematic answer: **measure first** (timing profiler, identify top 10 slow tests), then **parallelize** (20 threads → 20× throughput), **eliminate `Thread.sleep`**, **replace UI login with API token injection**, **switch to headless**, **use class-scoped driver**. Announce the expected improvement at each step with numbers. This demonstrates engineering rigor over guessing.

### Follow-up Questions
1. How do you decide which tests to run in parallel vs which must run sequentially?
2. How would you implement a test impact analysis that runs only tests affected by specific code changes (rather than the full suite)?

### Selenium vs Playwright
Playwright is natively faster — browser context creation (~50ms) vs WebDriver session (~3s). Its auto-waiting eliminates polling overhead. For large suites, `--workers=20` is equivalent to JUnit5 parallel config. Playwright's built-in parallelism and context isolation remove most of the architectural complexity required for parallel Selenium execution.

---

## Q49: How do you integrate Selenium with microservices architectures — testing across service boundaries?

### Interview Answer
Microservices architectures require testing the **integration of services as the user experiences them**, not individual services in isolation. Selenium tests the browser-level behavior; the critical addition is **contract verification** (Pact), **API-level service boundary assertions**, and **distributed trace correlation** to pinpoint which service caused a UI failure. The test strategy must distinguish between: UI regression, API contract regression, and service integration regression.

### Deep Explanation
**Testing concerns in microservices via UI:**

| What Broke | Root Cause | Detection Method |
|---|---|---|
| UI shows wrong data | Frontend rendering bug | Selenium assertion |
| UI shows wrong data | API returns wrong data | REST Assured assertion post-UI action |
| UI shows error | Downstream service unavailable | CDP network interception + WireMock stub |
| Slow page load | Cascading service latency | CDP performance timing |
| Feature missing | Feature flag service down | Feature flag API check |

**Contract testing (Pact) position:**
```
Unit Tests       → individual service logic
Contract Tests   → API producer/consumer contract (Pact)
Integration Tests→ two real services talking to each other
E2E (Selenium)   → user journey across all services via browser
```

Selenium E2E is the most expensive — runs fewest, covers broadest scope. Contract tests (Pact) are fastest and catch API regressions before E2E even runs.

### Java Example
```java
// MICROSERVICES E2E TEST — with service boundary assertions
class OrderServiceIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Order placement triggers inventory, payment, and notification services")
    void orderPlacementIntegratesAllServices() throws Exception {
        // SETUP: Use API to create clean state across services
        TestUser customer = UserApiFactory.createUser("CUSTOMER");
        String productId = InventoryApiClient.createProduct("LAPTOP-001", 10); // qty=10
        String token = UserApiFactory.getAuthToken(customer);

        // BROWSER: Inject token, navigate to product
        driver().get(BASE_URL);
        new BrowserStorageUtils(driver()).injectJwtToken(token);
        driver().get(BASE_URL + "/products/" + productId);

        // UI ACTION: Place order
        ConfirmationPage confirmation = new ProductPage(driver())
            .addToCart()
            .proceedToCheckout()
            .fillShipping(customer.address())
            .payWithCard("4242424242424242")
            .confirm();

        String orderId = confirmation.getOrderNumber();
        assertThat(orderId).isNotNull();

        // SERVICE BOUNDARY ASSERTIONS — verify each downstream service processed correctly

        // 1. Order Service
        given().spec(ApiConfig.spec()).header("Authorization", "Bearer " + token)
            .when().get("/orders/{id}", orderId)
            .then().statusCode(200)
            .body("status", equalTo("CONFIRMED"))
            .body("items[0].productId", equalTo(productId));

        // 2. Inventory Service — stock decremented
        given().spec(ApiConfig.spec())
            .when().get("/inventory/{id}", productId)
            .then().statusCode(200)
            .body("quantity", equalTo(9)); // was 10, now 9

        // 3. Payment Service — charge created
        String paymentId = given().spec(ApiConfig.spec())
            .header("Authorization", "Bearer " + token)
            .when().get("/payments?orderId=" + orderId)
            .then().statusCode(200)
            .extract().path("[0].id");
        assertThat(paymentId).isNotNull();

        // 4. Notification Service — email queued (async — poll)
        await().atMost(10, SECONDS).pollInterval(1, SECONDS)
            .untilAsserted(() ->
                given().spec(ApiConfig.spec())
                    .when().get("/notifications?userId=" + customer.id() + "&type=ORDER_CONFIRMED")
                    .then().statusCode(200)
                    .body("size()", greaterThan(0)));
    }
}

// WIREMOCK — stub downstream services for isolated UI testing
class CheckoutWithServiceStubsTest extends BaseTest {
    private WireMockServer inventoryService;
    private WireMockServer paymentService;

    @BeforeEach
    void startStubs() {
        // Stub inventory service — tests checkout UI behavior when stock check returns different results
        inventoryService = new WireMockServer(WireMockConfiguration.options().port(8081));
        inventoryService.start();

        // Stub payment service
        paymentService = new WireMockServer(WireMockConfiguration.options().port(8082));
        paymentService.start();
    }

    @Test
    void checkoutShowsOutOfStockWhenInventoryServiceSaysZero() {
        // Stub inventory to return "out of stock"
        inventoryService.stubFor(get(urlPathEqualTo("/inventory/LAPTOP-001"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"quantity\": 0, \"available\": false}")
                .withHeader("Content-Type", "application/json")));

        driver().get(BASE_URL + "/products/LAPTOP-001");
        new AppWait(driver()).untilVisible(By.cssSelector("[data-testid='product-details']"));

        WebElement addToCartBtn = driver().findElement(
            By.cssSelector("[data-testid='add-to-cart']"));
        assertThat(addToCartBtn.isEnabled()).isFalse();
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='stock-indicator']")).getText())
            .contains("Out of Stock");
    }

    @Test
    void checkoutShowsPaymentErrorWhenPaymentServiceFails() {
        paymentService.stubFor(post(urlPathEqualTo("/payments"))
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("{\"error\": \"Payment gateway unavailable\"}")));

        // Complete checkout to payment step
        new CheckoutPage(driver()).fillAndSubmitPayment("4242424242424242");

        assertThat(new AppWait(driver())
            .untilVisible(By.cssSelector("[data-testid='payment-error']")).getText())
            .contains("Payment unavailable");
    }

    @AfterEach
    void stopStubs() {
        inventoryService.stop();
        paymentService.stop();
    }
}

// DISTRIBUTED TRACE CORRELATION
public class DistributedTraceHelper {
    // Inject trace ID into browser → propagated as HTTP header by app code
    public static String injectTraceId(WebDriver driver) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        // App reads this from localStorage and adds as X-Trace-ID header to all API calls
        new BrowserStorageUtils(driver).setLocalStorage("__testTraceId", traceId);
        log.info("Injected trace ID: {} — use to correlate in Jaeger/Zipkin", traceId);
        return traceId;
    }

    public static void logTraceUrl(String traceId) {
        String traceUrl = "http://jaeger:16686/trace/" + traceId;
        log.info("Jaeger trace: {}", traceUrl);
        Allure.addAttachment("Distributed Trace URL", traceUrl);
    }
}
```

### Real-world Usage
- **E-commerce microservices (12 services)**: E2E tests cover 8 critical paths; each path asserts on order, inventory, payment, notification APIs post-action — catches cross-service contract breaks that functional UI assertions miss
- **WireMock for downstream service failure simulation**: Tests verify UI shows graceful degradation when payment gateway returns 503 — impossible to test in real environment without causing production incident
- **Pact contract tests as pre-E2E gate**: Pact tests run in 30 seconds and catch 80% of API contract regressions before expensive E2E suite starts
- **Distributed trace injection**: Test engineer sees failure in checkout; trace ID from Allure report → Jaeger → entire 12-service request chain visible in 30 seconds

### Common Mistakes
- Running all microservice integration through UI — use contract tests (Pact) for service boundaries; UI is for user journey only
- Not stubbing slow/expensive third-party services in E2E — Stripe, SendGrid, Twilio slow down tests and incur costs in test runs; use WireMock stubs
- Asserting only on UI without verifying downstream services — order appears placed on screen but inventory never decremented — you need both UI and API assertions
- Forgetting to clean up cross-service test data — order created in service A references product in service B; cleanup must handle foreign key ordering

### Debugging Strategy
1. E2E test fails but all individual service tests pass → integration point broken (field name mismatch, schema change) → add explicit API assertion at each service boundary after UI action
2. Intermittent payment failure in E2E → real payment sandbox has rate limits → stub payment service in E2E; test payment flows against real sandbox in dedicated payment integration tests
3. Can't tell which service caused UI failure → no distributed trace → implement trace ID injection from test; link Allure report to Jaeger/Zipkin
4. WireMock stub not intercepted by browser → app code uses hardcoded service URL not going through proxy → configure app's service URLs as environment variables pointing to WireMock ports in test config

### Interview Trap
"Should you use Selenium E2E tests to verify microservices contract?" — No. **Contract testing (Pact) is the correct tool for service boundaries**. Pact tests run in milliseconds, isolate each service, and can be run by each team independently. Selenium E2E tests verify user journeys, which happen to cross service boundaries. The distinction: E2E catches "the checkout user journey is broken"; Pact catches "the Order service API contract changed without notifying the UI team." Both are necessary; neither replaces the other.

### Follow-up Questions
1. How do you implement consumer-driven contract testing with Pact alongside Selenium E2E tests?
2. How do you test a feature that spans 5 microservices when 2 of those services are owned by other teams?

### Selenium vs Playwright
Both tools require the same microservices testing strategy — the choice of browser automation tool doesn't change how you handle service boundaries, contract testing, or distributed tracing. Playwright's `page.request` makes API assertions slightly easier. WireMock and Pact integration is framework-agnostic.

---

## Q50: How do you evaluate whether Selenium is the right tool — when to use alternatives?

### Interview Answer
Selenium is the right choice for: **Java/JVM teams**, **cross-browser testing of web apps**, **legacy test infrastructure investment**, **Selenium Grid self-hosted scale**, and **teams with existing Selenium expertise**. Consider alternatives when: **speed is critical** (Playwright/Cypress 3–5× faster setup), **TypeScript/JavaScript is the team language**, **mobile is primary** (Appium), **desktop apps** (WinAppDriver/Appium for desktop), or **pure API testing** (REST Assured, Karate). The choice is architectural — evaluate based on team skill, app type, scale needs, and maintenance burden.

### Deep Explanation
**Decision matrix:**

| Requirement | Best Tool |
|---|---|
| Web E2E, Java team, enterprise | Selenium 4 |
| Web E2E, JS/TS team, modern SPA | Playwright |
| Web E2E, simplicity, no parallel | Cypress |
| Mobile native (iOS/Android) | Appium |
| Desktop app automation | WinAppDriver / Appium Desktop |
| API testing only | REST Assured / Karate / RestSharp |
| Performance/load testing | JMeter / Gatling / k6 |
| Visual regression only | Applitools / Percy standalone |
| Accessibility only | axe-core / Lighthouse |
| AI-powered self-healing | Healenium / Applitools |

**Selenium strengths:**
- Widest browser support (all major browsers including legacy IE via older versions)
- Language support: Java, Python, C#, Ruby, JS, Kotlin — widest polyglot support
- Mature ecosystem: 20+ years of libraries, tutorials, StackOverflow answers
- Grid 4: self-hosted parallel execution at any scale
- Chrome DevTools Protocol integration in Selenium 4
- W3C standard compliance — tests portable across drivers

**Selenium weaknesses vs Playwright:**
- No built-in auto-waiting — requires `WebDriverWait` everywhere
- Slower test execution (3s browser startup vs 50ms Playwright context)
- No built-in network interception in API (CDP only via Chrome)
- No built-in trace recording
- No built-in mobile viewport emulation (requires Chrome options flags)
- No built-in visual comparison
- More boilerplate for parallel execution

### Java Example
```java
// SELENIUM vs PLAYWRIGHT — same test, side by side

// ===== SELENIUM (Java) =====
@Test
void loginAndVerifyDashboard_Selenium() {
    // Driver setup (3s startup)
    ChromeOptions opts = new ChromeOptions();
    opts.addArguments("--headless=new");
    WebDriver driver = new ChromeDriver(opts);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    try {
        driver.get("https://example.com/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[data-testid='email']"))).sendKeys("user@test.com");
        driver.findElement(By.cssSelector("[data-testid='password']"))
            .sendKeys("password");
        driver.findElement(By.cssSelector("[data-testid='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        String welcome = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[data-testid='welcome']"))).getText();
        assertThat(welcome).contains("Welcome");
    } finally {
        driver.quit();
    }
}

// ===== EQUIVALENT PLAYWRIGHT (Java — playwright-java) =====
@Test
void loginAndVerifyDashboard_Playwright() {
    // Context start ~50ms
    try (Playwright playwright = Playwright.create();
         Browser browser = playwright.chromium().launch(
             new BrowserType.LaunchOptions().setHeadless(true));
         BrowserContext context = browser.newContext();
         Page page = context.newPage()) {

        page.navigate("https://example.com/login");
        // Auto-wait: all locator actions wait automatically
        page.getByTestId("email").fill("user@test.com");
        page.getByTestId("password").fill("password");
        page.getByTestId("submit").click();
        page.waitForURL("**/dashboard");
        assertThat(page.getByTestId("welcome")).containsText("Welcome");
    }
}

// MIGRATION DECISION FRAMEWORK
public class MigrationEvaluator {
    /**
     * Evaluate whether to migrate from Selenium to Playwright
     * Score > 6: consider migration
     * Score <= 6: stay with Selenium
     */
    public int calculateMigrationScore(ProjectContext ctx) {
        int score = 0;

        // Strong reasons to migrate
        if (ctx.teamLanguage().equals("TypeScript"))  score += 3;
        if (ctx.suiteRunTime().toHours() > 2)         score += 2; // speed pain
        if (ctx.flakeyTestRate() > 0.10)              score += 2; // > 10% flake
        if (ctx.testCount() < 200)                    score += 1; // small suite = low migration cost

        // Strong reasons to stay
        if (ctx.hasExistingGridInfrastructure())      score -= 2;
        if (ctx.teamExpertise().equals("SELENIUM"))   score -= 2;
        if (ctx.testCount() > 1000)                   score -= 2; // large suite = high migration cost
        if (ctx.needsIESupport())                     score -= 3; // Playwright can't do IE
        if (ctx.hasJavaBackendTeam())                 score -= 1; // Java consistency argument

        return score;
    }
}

// HYBRID APPROACH — run both during migration
// Tag new tests with @Playwright / @Selenium
// Gradually migrate page objects module by module
// Keep Selenium Grid for cross-browser; use Playwright Workers for smoke suite
```

### Real-world Usage
- **Migration case (startup)**: 150-test Selenium suite in Python → migrated to Playwright (TypeScript, matching team stack) — suite time 45 min → 8 min; flake rate 15% → 2%; migration took 6 weeks
- **Stay-with-Selenium case (enterprise bank)**: 2000-test Java Selenium suite, 50-node self-hosted Grid, Java backend team, IE11 requirement for one legacy app → migration cost > 3 years of productivity gain from Playwright
- **Hybrid case (SaaS product)**: Selenium for cross-browser regression (IE11 + Safari + Firefox), Playwright for smoke suite (Chrome only, needs to run in < 5 min for every PR) — both running in CI targeting different triggers
- **Appium for mobile**: Web app has hybrid mobile (WebView), Selenium tests cover desktop web, Appium covers mobile WebView with same page objects reused via `AppiumDriver extends RemoteWebDriver`

### Common Mistakes
- Choosing Playwright purely because it's newer — newer ≠ better for all contexts; team expertise and infrastructure investment matter
- Migrating a 2000-test suite all at once — incremental migration (test class by test class) is lower risk; run both in parallel during transition
- Expecting Playwright to eliminate all test maintenance — locator strategy, test data isolation, and flakiness from application bugs affect both tools equally
- Not evaluating Cypress for developer-first testing — Cypress has `cy.intercept()` for network stubbing and time-travel debugging that makes it excellent for component and integration tests written by frontend developers

### Debugging Strategy
1. Team evaluating Playwright for speed — benchmark with identical 50-test sample in both tools in same CI environment; measure p50 and p95 run time before deciding
2. Forced to use Selenium (IE11 requirement) but want Playwright benefits — run Playwright for all modern browser tests (80% of suite), Selenium for IE-specific tests only
3. Playwright context not available in Java — `playwright-java` is the official Java library; same capabilities as TypeScript but less community content than Java Selenium

### Interview Trap
"Is Playwright better than Selenium?" — The senior answer: **it depends on context**. Playwright wins on: speed (browser context startup), built-in auto-waiting, trace recording, and modern API design. Selenium wins on: Java ecosystem maturity, polyglot support, self-hosted Grid at scale, IE/legacy browser support, and lower migration cost for established teams. The question is not which is "better" — it's which is **better for your specific team, codebase, and requirements**. Always evaluate with data, not hype.

### Follow-up Questions
1. How would you propose and execute a phased migration from Selenium to Playwright for a 500-test suite?
2. How do you maintain test coverage continuity during a framework migration without creating a "big bang" cutover?

### Selenium vs Playwright
This question IS the comparison — both tools have distinct strengths. The mature professional position: know both tools deeply, recommend based on evidence and context, and resist tribal loyalty to either. The ability to articulate trade-offs clearly is what distinguishes a Principal SDET from a senior engineer.

---

# Section 2: Coding / Hands-on Questions (30)

---

## CQ1: Write a complete, production-ready `DriverFactory` with `ThreadLocal`, browser support, and Grid capability

### Problem Statement
Implement a `DriverFactory` class that: supports Chrome, Firefox, and Edge; works locally and on Selenium Grid; is thread-safe for parallel execution; supports headless mode via config; and cleans up properly.

### Solution

```java
public class DriverFactory {
    private static final ThreadLocal<WebDriver> driverPool    = new ThreadLocal<>();
    private static final ThreadLocal<String>    browserPool   = new ThreadLocal<>();
    private static final List<WebDriver>        allDrivers    =
        Collections.synchronizedList(new ArrayList<>());

    // JVM shutdown hook — kills any orphaned drivers if tests crash
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
            allDrivers.forEach(d -> { try { d.quit(); } catch (Exception ignored) {} })));
    }

    public static void initDriver(String browser) {
        if (driverPool.get() != null) {
            log.warn("Driver already initialized for thread {} — quitting previous instance",
                Thread.currentThread().getName());
            quitDriver();
        }
        WebDriver driver = createDriver(browser);
        driverPool.set(driver);
        browserPool.set(browser);
        allDrivers.add(driver);
    }

    public static WebDriver getDriver() {
        WebDriver driver = driverPool.get();
        if (driver == null) throw new IllegalStateException(
            "Driver not initialized for thread: " + Thread.currentThread().getName() +
            " — call DriverFactory.initDriver() in @BeforeEach");
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = driverPool.get();
        if (driver != null) {
            allDrivers.remove(driver);
            try { driver.quit(); } catch (WebDriverException e) {
                log.warn("Error quitting driver: {}", e.getMessage());
            } finally {
                driverPool.remove();
                browserPool.remove();
            }
        }
    }

    private static WebDriver createDriver(String browser) {
        String gridUrl  = ConfigReader.get("gridUrl", null);
        boolean headless = ConfigReader.getBoolean("headless", true);

        return (gridUrl != null)
            ? createRemoteDriver(browser, headless, gridUrl)
            : createLocalDriver(browser, headless);
    }

    private static WebDriver createLocalDriver(String browser, boolean headless) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> {
                ChromeOptions opts = chromeOptions(headless);
                yield new ChromeDriver(opts);
            }
            case "firefox" -> {
                FirefoxOptions opts = firefoxOptions(headless);
                yield new FirefoxDriver(opts);
            }
            case "edge" -> {
                EdgeOptions opts = edgeOptions(headless);
                yield new EdgeDriver(opts);
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
    }

    private static WebDriver createRemoteDriver(String browser, boolean headless, String gridUrl) {
        Capabilities caps = switch (browser.toLowerCase()) {
            case "chrome"  -> chromeOptions(headless);
            case "firefox" -> firefoxOptions(headless);
            case "edge"    -> edgeOptions(headless);
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
        try {
            RemoteWebDriver driver = new RemoteWebDriver(new URL(gridUrl), caps);
            driver.setFileDetector(new LocalFileDetector()); // for file upload on Grid
            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Grid URL: " + gridUrl, e);
        }
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions opts = new ChromeOptions();
        if (headless) opts.addArguments("--headless=new");
        opts.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-extensions",
            "--disable-gpu",
            "--window-size=1920,1080"
        );
        opts.setPageLoadStrategy(PageLoadStrategy.EAGER);
        // Disable password manager popups
        opts.setExperimentalOption("prefs", Map.of(
            "credentials_enable_service", false,
            "profile.password_manager_enabled", false
        ));
        return opts;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions opts = new FirefoxOptions();
        if (headless) opts.addArguments("-headless");
        opts.addPreference("browser.download.folderList", 2);
        opts.addPreference("browser.helperApps.neverAsk.saveToDisk",
            "application/pdf,application/octet-stream");
        return opts;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions opts = new EdgeOptions();
        if (headless) opts.addArguments("--headless=new");
        opts.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        return opts;
    }
}
```

### Key Design Decisions
- `ThreadLocal` — each thread gets its own driver; no sharing, no locking needed
- Shutdown hook — prevents zombie ChromeDriver processes if JVM crashes mid-run
- `allDrivers` list — enables shutdown hook to find all live drivers across all threads
- `quitDriver()` calls `remove()` on `ThreadLocal` — prevents memory leak in thread pools
- `LocalFileDetector` on Remote — file path on local machine needs to be uploaded to Grid node

---

## CQ2: Implement a fluent Page Object for a login page with strong typing and error handling

### Problem Statement
Build a production-quality `LoginPage` class using the Page Object Model: fluent API, explicit waits baked in, meaningful error messages, no `Thread.sleep`, no public `WebElement` fields, and reused `By` locators as private static constants.

### Solution

```java
public class LoginPage {
    // ── LOCATORS — static final; one place to update ─────────────────
    private static final By EMAIL_INPUT   = By.cssSelector("[data-testid='email-input']");
    private static final By PASSWORD_INPUT= By.cssSelector("[data-testid='password-input']");
    private static final By SUBMIT_BUTTON = By.cssSelector("[data-testid='login-submit']");
    private static final By ERROR_BANNER  = By.cssSelector("[data-testid='login-error']");
    private static final By SPINNER       = By.cssSelector("[data-testid='loading-spinner']");
    private static final By FORGOT_LINK   = By.cssSelector("[data-testid='forgot-password']");

    private final WebDriver driver;
    private final AppWait   wait;

    public LoginPage(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.wait   = new AppWait(driver).withTimeout(Duration.ofSeconds(15));
        // Verify we're on the right page
        wait.untilVisible(EMAIL_INPUT);
    }

    // ── FACTORY METHOD — navigate and return page object ─────────────
    public static LoginPage navigateTo(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/login");
        return new LoginPage(driver);
    }

    // ── FLUENT ACTIONS ────────────────────────────────────────────────
    public LoginPage enterEmail(String email) {
        WebElement input = wait.untilClickable(EMAIL_INPUT);
        input.clear();
        input.sendKeys(email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        WebElement input = wait.untilClickable(PASSWORD_INPUT);
        input.clear();
        input.sendKeys(password);
        return this;
    }

    public LoginPage clickSubmit() {
        wait.untilClickable(SUBMIT_BUTTON).click();
        return this;
    }

    // ── COMBINED ACTION — returns next page on success ────────────────
    public DashboardPage loginAs(String email, String password) {
        enterEmail(email)
            .enterPassword(password)
            .clickSubmit();

        // Wait for spinner to disappear (async login call)
        wait.untilAbsent(SPINNER);

        // Determine outcome — error or success
        if (hasError()) {
            throw new LoginFailedException(
                "Login failed for user '" + email + "'. Error: " + getErrorMessage());
        }
        // Wait for redirect to dashboard
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Expected redirect to /dashboard after login as " + email)
            .until(ExpectedConditions.urlContains("/dashboard"));

        return new DashboardPage(driver);
    }

    // Overload — login expecting failure (negative tests)
    public LoginPage loginExpectingFailure(String email, String password) {
        enterEmail(email)
            .enterPassword(password)
            .clickSubmit();
        wait.untilAbsent(SPINNER);
        wait.untilVisible(ERROR_BANNER); // fail here if no error shown
        return this;
    }

    // ── ASSERTIONS / GETTERS ──────────────────────────────────────────
    public boolean hasError() {
        return !driver.findElements(ERROR_BANNER).isEmpty()
            && driver.findElement(ERROR_BANNER).isDisplayed();
    }

    public String getErrorMessage() {
        return wait.untilVisible(ERROR_BANNER).getText().trim();
    }

    public boolean isSubmitEnabled() {
        return driver.findElement(SUBMIT_BUTTON).isEnabled();
    }

    public ForgotPasswordPage clickForgotPassword() {
        wait.untilClickable(FORGOT_LINK).click();
        return new ForgotPasswordPage(driver);
    }
}

// CUSTOM EXCEPTION — rich context for failures
public class LoginFailedException extends RuntimeException {
    public LoginFailedException(String message) { super(message); }
}

// USAGE IN TEST
@Test
void validCredentialsRedirectToDashboard() {
    DashboardPage dashboard = LoginPage.navigateTo(driver(), BASE_URL)
        .loginAs("user@test.com", "correctPassword");
    assertThat(dashboard.getWelcomeMessage()).contains("Welcome");
}

@Test
void invalidPasswordShowsErrorMessage() {
    String error = LoginPage.navigateTo(driver(), BASE_URL)
        .loginExpectingFailure("user@test.com", "wrongPassword")
        .getErrorMessage();
    assertThat(error).isEqualTo("Invalid email or password");
}

@Test
void emptyEmailDisablesSubmitButton() {
    LoginPage page = LoginPage.navigateTo(driver(), BASE_URL)
        .enterPassword("anyPassword");
    assertThat(page.isSubmitEnabled()).isFalse();
}
```

### Key Design Decisions
- Constructor validates page state — `new LoginPage(driver)` fails immediately if not on login page
- `loginAs()` returns `DashboardPage` — strong typing enforces page flow at compile time
- `loginExpectingFailure()` — separate method for negative tests; semantics are explicit
- `By` constants as `private static final` — one place to update when locator changes
- No `Thread.sleep` anywhere — all waits via `AppWait`
- `LoginFailedException` with context — test failure message includes email and error text

---

## CQ3: Write a utility to handle dynamic data tables — extract data, search, sort, and paginate

### Problem Statement
Implement a reusable `DataTable` component class that can: extract all rows as typed objects, find a row by column value, verify sort order, and navigate pagination — all without any `Thread.sleep`.

### Solution

```java
public class DataTable {
    // Configurable locators — passed at construction for reuse across different tables
    private final By tableLocator;
    private final By headerLocator;
    private final By rowLocator;
    private final By paginationNext;
    private final By paginationInfo;

    private final WebDriver driver;
    private final AppWait   wait;

    // Builder for flexible construction
    public static Builder builder(WebDriver driver) { return new Builder(driver); }

    private DataTable(Builder b) {
        this.driver          = b.driver;
        this.tableLocator    = b.tableLocator;
        this.headerLocator   = b.headerLocator;
        this.rowLocator      = b.rowLocator;
        this.paginationNext  = b.paginationNext;
        this.paginationInfo  = b.paginationInfo;
        this.wait            = new AppWait(driver).withTimeout(Duration.ofSeconds(15));
    }

    // ── HEADER MAPPING ────────────────────────────────────────────────
    // Returns {"Name" → 0, "Email" → 1, "Status" → 2, ...}
    private Map<String, Integer> getHeaderIndexMap() {
        wait.untilVisible(tableLocator);
        List<WebElement> headers = driver.findElements(headerLocator);
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i).getText().trim(), i);
        }
        return map;
    }

    // ── EXTRACT ALL ROWS AS MAP LIST ──────────────────────────────────
    public List<Map<String, String>> getAllRows() {
        Map<String, Integer> headerMap = getHeaderIndexMap();
        List<WebElement> rows = driver.findElements(rowLocator);
        return rows.stream().map(row -> {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            Map<String, String> rowData = new LinkedHashMap<>();
            headerMap.forEach((header, idx) -> {
                if (idx < cells.size()) {
                    rowData.put(header, cells.get(idx).getText().trim());
                }
            });
            return rowData;
        }).collect(Collectors.toList());
    }

    // ── FIND ROW BY COLUMN VALUE ──────────────────────────────────────
    public Optional<Map<String, String>> findRowBy(String columnName, String value) {
        return getAllRows().stream()
            .filter(row -> value.equals(row.get(columnName)))
            .findFirst();
    }

    public WebElement findRowElementBy(String columnName, String value) {
        Map<String, Integer> headerMap = getHeaderIndexMap();
        int colIndex = headerMap.getOrDefault(columnName, -1);
        if (colIndex == -1)
            throw new IllegalArgumentException("Column not found: " + columnName);

        return driver.findElements(rowLocator).stream()
            .filter(row -> {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                return colIndex < cells.size() &&
                    cells.get(colIndex).getText().trim().equals(value);
            })
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                "No row with " + columnName + " = '" + value + "'"));
    }

    // ── VERIFY SORT ORDER ─────────────────────────────────────────────
    public boolean isColumnSortedAscending(String columnName) {
        List<String> values = getAllRows().stream()
            .map(row -> row.getOrDefault(columnName, ""))
            .collect(Collectors.toList());
        List<String> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return values.equals(sorted);
    }

    public boolean isColumnSortedDescending(String columnName) {
        List<String> values = getAllRows().stream()
            .map(row -> row.getOrDefault(columnName, ""))
            .collect(Collectors.toList());
        List<String> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.reverseOrder());
        return values.equals(sorted);
    }

    public DataTable clickColumnHeader(String columnName) {
        int colIndex = getHeaderIndexMap().getOrDefault(columnName, -1);
        if (colIndex == -1)
            throw new IllegalArgumentException("Column not found: " + columnName);
        driver.findElements(headerLocator).get(colIndex).click();
        // Wait for sort to apply (spinner or row count stability)
        int rowCount = driver.findElements(rowLocator).size();
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(5))
            .pollingEvery(Duration.ofMillis(200))
            .until(d -> d.findElements(rowLocator).size() == rowCount); // stabilized
        return this;
    }

    // ── PAGINATION ────────────────────────────────────────────────────
    public boolean hasNextPage() {
        List<WebElement> nextBtns = driver.findElements(paginationNext);
        return !nextBtns.isEmpty() && nextBtns.get(0).isEnabled()
            && !nextBtns.get(0).getDomAttribute("disabled").equals("true");
    }

    public DataTable nextPage() {
        if (!hasNextPage())
            throw new IllegalStateException("No next page available");
        wait.untilClickable(paginationNext).click();
        // Wait for rows to refresh
        String currentInfo = driver.findElement(paginationInfo).getText();
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(10))
            .pollingEvery(Duration.ofMillis(300))
            .until(d -> !d.findElement(paginationInfo).getText().equals(currentInfo));
        return this;
    }

    // Collect all rows across all pages
    public List<Map<String, String>> getAllRowsAllPages() {
        List<Map<String, String>> all = new ArrayList<>(getAllRows());
        while (hasNextPage()) {
            nextPage();
            all.addAll(getAllRows());
        }
        return all;
    }

    // ── BUILDER ───────────────────────────────────────────────────────
    public static class Builder {
        private final WebDriver driver;
        private By tableLocator   = By.cssSelector("table");
        private By headerLocator  = By.cssSelector("thead th");
        private By rowLocator     = By.cssSelector("tbody tr");
        private By paginationNext = By.cssSelector("[data-testid='pagination-next']");
        private By paginationInfo = By.cssSelector("[data-testid='pagination-info']");

        Builder(WebDriver driver) { this.driver = driver; }
        public Builder table(By l)     { tableLocator   = l; return this; }
        public Builder headers(By l)   { headerLocator  = l; return this; }
        public Builder rows(By l)      { rowLocator     = l; return this; }
        public Builder nextPage(By l)  { paginationNext = l; return this; }
        public Builder pageInfo(By l)  { paginationInfo = l; return this; }
        public DataTable build()       { return new DataTable(this); }
    }
}

// USAGE IN TEST
@Test
void usersTableShowsCorrectDataAndSupportsPagination() {
    navigateTo("/admin/users");

    DataTable usersTable = DataTable.builder(driver())
        .headers(By.cssSelector("[data-testid='users-table'] thead th"))
        .rows(By.cssSelector("[data-testid='users-table'] tbody tr"))
        .nextPage(By.cssSelector("[data-testid='next-page']"))
        .pageInfo(By.cssSelector("[data-testid='page-info']"))
        .build();

    // Find specific user
    Map<String, String> userRow = usersTable.findRowBy("Email", "admin@example.com")
        .orElseThrow(() -> new AssertionError("User not found in table"));
    assertThat(userRow.get("Status")).isEqualTo("Active");
    assertThat(userRow.get("Role")).isEqualTo("ADMIN");

    // Verify sort
    usersTable.clickColumnHeader("Email");
    assertThat(usersTable.isColumnSortedAscending("Email")).isTrue();

    // All pages
    List<Map<String, String>> allUsers = usersTable.getAllRowsAllPages();
    assertThat(allUsers).hasSizeGreaterThan(20);
}
```

### Key Design Decisions
- Builder pattern — clean construction for tables with different locators across pages
- Header-index map computed once — decouples column name from position; order changes don't break tests
- `getAllRowsAllPages()` — utility covers full dataset without callers managing pagination loop
- No XPath row indexing — row position is fragile; always find by column value
- Sort verification — compares actual list to sorted copy; language-independent

---

## CQ4: Implement a file download verification utility for headless Chrome

### Problem Statement
Write a complete solution for verifying file downloads in headless Chrome — set download directory via CDP, wait for download completion (no `.crdownload` temp file), verify filename and content.

### Solution

```java
public class FileDownloadHelper {
    private final WebDriver  driver;
    private final Path       downloadDir;
    private final DevTools   devTools;

    public FileDownloadHelper(WebDriver driver) {
        this.driver      = driver;
        this.downloadDir = createTempDownloadDir();
        this.devTools    = ((ChromeDriver) driver).getDevTools();
        configureHeadlessDownloads();
    }

    private Path createTempDownloadDir() {
        try {
            return Files.createTempDirectory("selenium-downloads-");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create download directory", e);
        }
    }

    private void configureHeadlessDownloads() {
        devTools.createSession();
        // Allow downloads in headless mode — blocked by default
        devTools.send(Browser.setDownloadBehavior(
            Browser.SetDownloadBehaviorBehavior.ALLOW,
            Optional.empty(),
            Optional.of(downloadDir.toAbsolutePath().toString()),
            Optional.of(true)  // eventsEnabled — get download progress events
        ));
        log.info("Download directory configured: {}", downloadDir);
    }

    // ── WAIT FOR DOWNLOAD COMPLETE ────────────────────────────────────
    public Path waitForDownload(String expectedFilenamePattern, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try {
                // Check for completed file (not .crdownload temp)
                Optional<Path> completed = Files.list(downloadDir)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return !name.endsWith(".crdownload") &&
                               !name.endsWith(".tmp") &&
                               name.matches(expectedFilenamePattern);
                    })
                    .findFirst();
                if (completed.isPresent()) {
                    log.info("Download complete: {}", completed.get());
                    return completed.get();
                }
                Thread.sleep(500);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error waiting for download", e);
            }
        }
        throw new TimeoutException("Download did not complete within " + timeout +
            ". Directory contents: " + listDir());
    }

    // ── VERIFY FILE CONTENT ───────────────────────────────────────────
    public void assertCsvContainsRow(Path csvFile, String... expectedValues) throws IOException {
        List<String> lines = Files.readAllLines(csvFile);
        boolean found = lines.stream()
            .anyMatch(line -> Arrays.stream(expectedValues)
                .allMatch(line::contains));
        if (!found) {
            fail("CSV file " + csvFile.getFileName() + " does not contain row with: " +
                Arrays.toString(expectedValues) + "\nActual rows:\n" +
                lines.stream().limit(10).collect(Collectors.joining("\n")));
        }
    }

    public void assertPdfNotEmpty(Path pdfFile) {
        assertThat(pdfFile.toFile().length())
            .as("PDF file should not be empty")
            .isGreaterThan(1024L); // at least 1KB
    }

    public void assertFileSize(Path file, long minBytes, long maxBytes) throws IOException {
        long size = Files.size(file);
        assertThat(size)
            .as("File size of " + file.getFileName() + " (" + size + " bytes)")
            .isBetween(minBytes, maxBytes);
    }

    // ── CLEANUP ───────────────────────────────────────────────────────
    public void cleanup() {
        try {
            Files.walk(downloadDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Could not clean download dir: {}", e.getMessage());
        }
    }

    private String listDir() {
        try {
            return Files.list(downloadDir)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.joining(", "));
        } catch (IOException e) {
            return "(error listing dir)";
        }
    }
}

// USAGE IN TEST
class ReportDownloadTest extends BaseTest {
    private FileDownloadHelper downloader;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        downloader = new FileDownloadHelper(driver());
        loginAs("ADMIN");
    }

    @Test
    void exportedCsvContainsAllUsers() throws IOException {
        navigateTo("/admin/reports");
        new ReportsPage(driver()).clickExportCsv();

        Path csvFile = downloader.waitForDownload(
            "users-export-.*\\.csv", Duration.ofSeconds(30));

        downloader.assertCsvContainsRow(csvFile,
            "admin@example.com", "ADMIN", "Active");
        downloader.assertFileSize(csvFile, 1024, 5 * 1024 * 1024); // 1KB–5MB
    }

    @Test
    void invoicePdfDownloadsSuccessfully() throws IOException {
        navigateTo("/orders/ORD-001234");
        new OrderPage(driver()).clickDownloadInvoice();

        Path pdf = downloader.waitForDownload(
            "invoice-ORD-001234\\.pdf", Duration.ofSeconds(20));
        downloader.assertPdfNotEmpty(pdf);
    }

    @AfterEach
    void tearDown() {
        downloader.cleanup();
        DriverFactory.quitDriver();
    }
}
```

### Key Design Decisions
- CDP `Browser.setDownloadBehavior` — the only reliable way to configure downloads in headless Chrome; `ChromeOptions` prefs approach is unreliable in headless
- `.crdownload` filter — Chrome writes to `filename.crdownload` while downloading; only consider file complete when that extension is absent
- `createTempDirectory` — unique directory per test run; no cross-test contamination
- Regex pattern matching — handles dynamic filenames with timestamps (`users-export-2024-05-01.csv`)
- Cleanup in `@AfterEach` — temp files never accumulate across runs

---

## CQ5: Write a Shadow DOM utility and use it to interact with web components

### Problem Statement
Implement a utility that navigates into Shadow DOM trees of arbitrary depth, handles both `open` and the workaround for `closed` mode, and exposes a clean API for finding and interacting with shadow elements.

### Solution

```java
public class ShadowDomUtils {
    private final WebDriver        driver;
    private final JavascriptExecutor js;

    public ShadowDomUtils(WebDriver driver) {
        this.driver = driver;
        this.js     = (JavascriptExecutor) driver;
    }

    // ── SELENIUM 4 NATIVE — getShadowRoot() ──────────────────────────
    // Works for open shadow roots; preferred approach
    public SearchContext shadowRoot(By hostLocator) {
        WebElement host = driver.findElement(hostLocator);
        try {
            return host.getShadowRoot();
        } catch (WebDriverException e) {
            throw new RuntimeException(
                "Could not access shadow root of " + hostLocator +
                ". Element may have closed shadow root or no shadow root at all.", e);
        }
    }

    // Chain into nested shadow DOMs
    // Usage: shadowRoot(host1).shadowRoot(host2).findElement(By.cssSelector("button"))
    public SearchContext nestedShadowRoot(By... hostLocators) {
        if (hostLocators.length == 0)
            throw new IllegalArgumentException("At least one host locator required");

        SearchContext ctx = driver;
        for (By hostLocator : hostLocators) {
            WebElement host = ctx.findElement(hostLocator);
            ctx = host.getShadowRoot();
        }
        return ctx;
    }

    // ── JS FALLBACK — for closed shadow roots (workaround) ───────────
    // NOTE: closed shadow roots are intentionally inaccessible from outside.
    // This approach uses devtools internals and may break — document its fragility.
    public WebElement findInClosedShadowRoot(String hostCssSelector, String innerCssSelector) {
        return (WebElement) js.executeScript(
            "return document.querySelector(arguments[0]).shadowRoot" +
            "       .querySelector(arguments[1]);",
            hostCssSelector, innerCssSelector);
    }

    // ── PIERCE CSS — flat tree traversal via JS ───────────────────────
    // Works across any shadow boundary depth
    public WebElement pierceCss(String cssSelector) {
        return (WebElement) js.executeScript(
            "function pierce(root, sel) {" +
            "  var el = root.querySelector(sel);" +
            "  if (el) return el;" +
            "  var nodes = root.querySelectorAll('*');" +
            "  for (var n of nodes) {" +
            "    if (n.shadowRoot) { var r = pierce(n.shadowRoot, sel); if (r) return r; }" +
            "  }" +
            "  return null;" +
            "}" +
            "return pierce(document, arguments[0]);",
            cssSelector);
    }

    // ── FIND ELEMENT WITH WAIT ────────────────────────────────────────
    public WebElement waitForShadowElement(By hostLocator, By innerLocator, Duration timeout) {
        return new FluentWait<>(driver)
            .withTimeout(timeout)
            .pollingEvery(Duration.ofMillis(300))
            .ignoring(NoSuchElementException.class)
            .ignoring(WebDriverException.class)
            .until(d -> {
                SearchContext shadow = shadowRoot(hostLocator);
                return shadow.findElement(innerLocator);
            });
    }

    // ── SEND KEYS INTO SHADOW INPUT ───────────────────────────────────
    public void typeInShadowInput(By hostLocator, By inputLocator, String text) {
        WebElement input = waitForShadowElement(hostLocator, inputLocator, Duration.ofSeconds(10));
        input.clear();
        input.sendKeys(text);
    }

    // ── CLICK IN SHADOW ───────────────────────────────────────────────
    public void clickInShadow(By hostLocator, By buttonLocator) {
        WebElement button = waitForShadowElement(hostLocator, buttonLocator, Duration.ofSeconds(10));
        button.click();
    }

    // ── ASSERT TEXT IN SHADOW ─────────────────────────────────────────
    public String getShadowElementText(By hostLocator, By elementLocator) {
        return waitForShadowElement(hostLocator, elementLocator, Duration.ofSeconds(10))
            .getText().trim();
    }
}

// USAGE IN TEST
class WebComponentTest extends BaseTest {
    @Test
    void customSearchComponentFiltersResults() {
        navigateTo("/components-demo");
        ShadowDomUtils shadow = new ShadowDomUtils(driver());

        // <my-search-widget> hosts a shadow DOM
        By searchHost  = By.cssSelector("my-search-widget");
        By searchInput = By.cssSelector("input[type='search']");
        By filterBtn   = By.cssSelector("button.apply-filter");
        By resultItem  = By.cssSelector(".result-item");

        shadow.typeInShadowInput(searchHost, searchInput, "Selenium");
        shadow.clickInShadow(searchHost, filterBtn);

        // Results are also inside the shadow DOM
        SearchContext shadowCtx = shadow.shadowRoot(searchHost);
        List<WebElement> results = shadowCtx.findElements(resultItem);
        assertThat(results).hasSizeGreaterThan(0);
        assertThat(results.get(0).getText()).containsIgnoringCase("selenium");
    }

    @Test
    void nestedShadowDomInteraction() {
        navigateTo("/nested-components");
        ShadowDomUtils shadow = new ShadowDomUtils(driver());

        // <outer-component> → shadow → <inner-component> → shadow → <button>
        SearchContext innerShadow = shadow.nestedShadowRoot(
            By.cssSelector("outer-component"),
            By.cssSelector("inner-component")
        );

        innerShadow.findElement(By.cssSelector("button[data-testid='confirm']")).click();
        assertThat(shadow.getShadowElementText(
            By.cssSelector("outer-component"),
            By.cssSelector(".status-message")))
            .isEqualTo("Confirmed");
    }
}
```

### Key Design Decisions
- `getShadowRoot()` preferred — Selenium 4 native approach; most reliable for open shadow roots
- `nestedShadowRoot()` varargs — chains through arbitrary shadow depth with one call
- `pierceCss()` JS fallback — last resort for complex cases; documented as fragile
- Closed shadow DOM approach — intentionally limited; closed mode exists to prevent external JS access; documented with caveat
- `waitForShadowElement()` wraps `getShadowRoot()` in `FluentWait` — shadow root itself may not be ready immediately after navigation

---

## CQ6: Write a network request interceptor using CDP to stub API responses

### Problem Statement
Implement a utility that intercepts outgoing network requests in a Chrome browser session and returns mocked responses — without a proxy server. Use Selenium's CDP integration to mock a specific API endpoint response.

### Solution

```java
public class CdpNetworkInterceptor {
    private final ChromeDriver driver;
    private final DevTools     devTools;
    private final Map<String, MockResponse> stubs = new ConcurrentHashMap<>();

    public CdpNetworkInterceptor(WebDriver driver) {
        if (!(driver instanceof ChromeDriver))
            throw new IllegalArgumentException("CDP interceptor requires ChromeDriver");
        this.driver   = (ChromeDriver) driver;
        this.devTools = this.driver.getDevTools();
        this.devTools.createSession();
        setupInterception();
    }

    // ── REGISTER A STUB ───────────────────────────────────────────────
    public CdpNetworkInterceptor stubGet(String urlPattern, int statusCode, String jsonBody) {
        stubs.put(urlPattern, new MockResponse(statusCode, jsonBody, "application/json"));
        return this;
    }

    public CdpNetworkInterceptor stubPost(String urlPattern, int statusCode, String jsonBody) {
        stubs.put(urlPattern, new MockResponse(statusCode, jsonBody, "application/json"));
        return this;
    }

    // ── SETUP CDP FETCH INTERCEPTION ──────────────────────────────────
    private void setupInterception() {
        devTools.send(Fetch.enable(
            Optional.of(List.of(
                new RequestPattern(Optional.of("*"), Optional.empty(),
                    Optional.of(RequestStage.REQUEST))
            )),
            Optional.of(false) // handleAuthRequests
        ));

        devTools.addListener(Fetch.requestPaused(), event -> {
            String url = event.getRequest().getUrl();
            String requestId = event.getRequestId().toString();

            // Find a matching stub
            Optional<Map.Entry<String, MockResponse>> match = stubs.entrySet().stream()
                .filter(e -> url.contains(e.getKey()))
                .findFirst();

            if (match.isPresent()) {
                MockResponse mock = match.get().getValue();
                log.debug("[CDP STUB] Intercepting {} → {}", url, mock.statusCode());
                // Return mocked response
                devTools.send(Fetch.fulfillRequest(
                    new RequestId(requestId),
                    mock.statusCode(),
                    Optional.of(List.of(
                        new HeaderEntry("Content-Type", mock.contentType()),
                        new HeaderEntry("Access-Control-Allow-Origin", "*")
                    )),
                    Optional.empty(),
                    Optional.of(Base64.getEncoder()
                        .encodeToString(mock.body().getBytes(StandardCharsets.UTF_8))),
                    Optional.empty()
                ));
            } else {
                // Pass through — don't intercept
                devTools.send(Fetch.continueRequest(
                    new RequestId(requestId),
                    Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty()
                ));
            }
        });
    }

    // ── CAPTURE ACTUAL REQUESTS ───────────────────────────────────────
    private final List<String> capturedUrls = Collections.synchronizedList(new ArrayList<>());

    public void startCapturing() {
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.addListener(Network.requestWillBeSent(), event ->
            capturedUrls.add(event.getRequest().getUrl()));
    }

    public boolean wasRequestMade(String urlFragment) {
        return capturedUrls.stream().anyMatch(u -> u.contains(urlFragment));
    }

    public List<String> getCapturedUrls() {
        return Collections.unmodifiableList(capturedUrls);
    }

    // ── DISABLE INTERCEPTION ──────────────────────────────────────────
    public void disable() {
        devTools.send(Fetch.disable());
        stubs.clear();
    }

    record MockResponse(int statusCode, String body, String contentType) {}
}

// USAGE IN TEST
class ApiStubTest extends BaseTest {
    private CdpNetworkInterceptor interceptor;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        interceptor = new CdpNetworkInterceptor(driver());
    }

    @Test
    void dashboardShowsNoDataWhenApiReturnsEmptyList() {
        // Stub API to return empty list
        interceptor.stubGet("/api/v1/orders",
            200, "{\"orders\": [], \"total\": 0}");

        loginAs("CUSTOMER");
        navigateTo("/dashboard");

        // UI should handle empty state gracefully
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='empty-state']")).isDisplayed())
            .isTrue();
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='empty-state']")).getText())
            .contains("No orders yet");
    }

    @Test
    void errorPageShownWhenApiReturns503() {
        interceptor.stubGet("/api/v1/products", 503,
            "{\"error\": \"Service temporarily unavailable\"}");

        navigateTo("/products");

        new AppWait(driver()).untilVisible(
            By.cssSelector("[data-testid='error-banner']"));
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='error-banner']")).getText())
            .contains("temporarily unavailable");
    }

    @Test
    void paymentFormSubmitsCorrectPayload() {
        interceptor.startCapturing();
        // Stub payment API to always succeed (avoid real charge)
        interceptor.stubPost("/api/v1/payments",
            200, "{\"id\": \"pay_test_001\", \"status\": \"succeeded\"}");

        navigateTo("/checkout");
        new CheckoutPage(driver())
            .fillCard("4242424242424242", "12/28", "123")
            .submit();

        assertThat(interceptor.wasRequestMade("/api/v1/payments")).isTrue();
        new AppWait(driver()).untilVisible(
            By.cssSelector("[data-testid='payment-success']"));
    }

    @AfterEach
    void tearDown() {
        interceptor.disable();
        DriverFactory.quitDriver();
    }
}
```

### Key Design Decisions
- `Fetch.enable` with `RequestStage.REQUEST` — intercepts before request is sent to network; enables full response replacement
- Pattern matching by `url.contains()` — simple, covers `/api/v1/orders?page=1` when stub registered as `/api/v1/orders`
- Pass-through for non-stubbed requests — only intercept explicitly registered URLs; all other traffic flows normally
- Base64 body encoding — CDP `fulfillRequest` requires body as Base64 string
- `ConcurrentHashMap` for stubs — safe when multiple devTools listener callbacks execute concurrently

---

## CQ7: Implement a complete Actions API chain for drag-and-drop, hover menus, and keyboard shortcuts

### Problem Statement
Write production-quality code for: (1) HTML5 drag-and-drop that works reliably, (2) multi-level hover menu navigation, (3) keyboard shortcut with modifier keys, and (4) right-click context menu interaction.

### Solution

```java
public class ActionsHelper {
    private final WebDriver driver;
    private final AppWait   wait;
    private final JavascriptExecutor js;

    public ActionsHelper(WebDriver driver) {
        this.driver = driver;
        this.wait   = new AppWait(driver);
        this.js     = (JavascriptExecutor) driver;
    }

    // ── DRAG AND DROP — Selenium Actions (HTML4 / native draggable) ───
    public void dragAndDrop(By source, By target) {
        WebElement src = wait.untilVisible(source);
        WebElement tgt = wait.untilVisible(target);
        new Actions(driver)
            .clickAndHold(src)
            .moveToElement(tgt)
            .release(tgt)
            .perform();
    }

    // ── DRAG AND DROP — JS simulation (HTML5 drag events) ────────────
    // Use when Selenium Actions doesn't work (React/Vue DnD libraries)
    public void dragAndDropJs(By source, By target) {
        WebElement src = wait.untilVisible(source);
        WebElement tgt = wait.untilVisible(target);
        js.executeScript(
            "function simulateDrag(src, tgt) {" +
            "  function createEvent(type) {" +
            "    var e = new DragEvent(type, {bubbles:true, cancelable:true});" +
            "    Object.defineProperty(e, 'dataTransfer', {" +
            "      value: { data:{}, setData(k,v){this.data[k]=v;}, " +
            "               getData(k){return this.data[k]||'';} }" +
            "    });" +
            "    return e;" +
            "  }" +
            "  var dt = createEvent('dragstart').dataTransfer;" +
            "  src.dispatchEvent(Object.assign(createEvent('dragstart'),{dataTransfer:dt}));" +
            "  tgt.dispatchEvent(Object.assign(createEvent('dragenter'),{dataTransfer:dt}));" +
            "  tgt.dispatchEvent(Object.assign(createEvent('dragover'), {dataTransfer:dt}));" +
            "  tgt.dispatchEvent(Object.assign(createEvent('drop'),     {dataTransfer:dt}));" +
            "  src.dispatchEvent(Object.assign(createEvent('dragend'),  {dataTransfer:dt}));" +
            "}" +
            "simulateDrag(arguments[0], arguments[1]);",
            src, tgt);
        // Short settle wait for React state to update after DnD
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(2))
            .pollingEvery(Duration.ofMillis(100))
            .until(d -> true); // just allow one tick
    }

    // ── DRAG BY OFFSET ────────────────────────────────────────────────
    public void dragByOffset(By source, int xOffset, int yOffset) {
        WebElement src = wait.untilVisible(source);
        new Actions(driver)
            .clickAndHold(src)
            .moveByOffset(xOffset, yOffset)
            .release()
            .perform();
    }

    // ── HOVER MENU — multi-level ──────────────────────────────────────
    public void navigateHoverMenu(By... menuItems) {
        Actions actions = new Actions(driver);
        for (By locator : menuItems) {
            WebElement item = wait.untilVisible(locator);
            actions.moveToElement(item);
        }
        // Click the last item (the target menu option)
        actions.click().perform();
    }

    // Wait for submenu to appear before hovering next level
    public void navigateHoverMenuWithWaits(List<By> menuLocators) {
        for (int i = 0; i < menuLocators.size(); i++) {
            By locator = menuLocators.get(i);
            WebElement item = wait.untilVisible(locator);
            new Actions(driver).moveToElement(item).perform();
            if (i < menuLocators.size() - 1) {
                // Wait for next submenu to appear
                wait.untilVisible(menuLocators.get(i + 1));
            }
        }
        // Click final item
        driver.findElement(menuLocators.get(menuLocators.size() - 1)).click();
    }

    // ── KEYBOARD SHORTCUTS ────────────────────────────────────────────
    public void pressShortcut(Keys modifier, String key) {
        new Actions(driver)
            .keyDown(modifier)
            .sendKeys(key)
            .keyUp(modifier)
            .perform();
    }

    // Multi-modifier: Ctrl+Shift+S
    public void pressShortcut(Keys modifier1, Keys modifier2, String key) {
        new Actions(driver)
            .keyDown(modifier1)
            .keyDown(modifier2)
            .sendKeys(key)
            .keyUp(modifier2)
            .keyUp(modifier1)
            .perform();
    }

    // Send keys to specific element (for focused shortcut)
    public void pressShortcutOn(By locator, Keys modifier, String key) {
        WebElement el = wait.untilClickable(locator);
        el.click(); // ensure focus
        new Actions(driver)
            .keyDown(el, modifier)
            .sendKeys(el, key)
            .keyUp(el, modifier)
            .perform();
    }

    // ── RIGHT-CLICK CONTEXT MENU ──────────────────────────────────────
    public void rightClick(By locator) {
        WebElement el = wait.untilVisible(locator);
        new Actions(driver).contextClick(el).perform();
    }

    public void rightClickAndSelect(By targetLocator, By menuOptionLocator) {
        rightClick(targetLocator);
        wait.untilVisible(menuOptionLocator).click();
    }

    // ── DOUBLE CLICK ──────────────────────────────────────────────────
    public void doubleClick(By locator) {
        WebElement el = wait.untilVisible(locator);
        new Actions(driver).doubleClick(el).perform();
    }

    // ── SCROLL TO ELEMENT (Actions — Selenium 4.2+) ───────────────────
    public void scrollToElement(By locator) {
        WebElement el = driver.findElement(locator);
        new Actions(driver).scrollToElement(el).perform();
    }
}

// USAGE IN TESTS
class ActionsTest extends BaseTest {
    @Test
    void dragCardToNewColumn() {
        navigateTo("/kanban");
        ActionsHelper actions = new ActionsHelper(driver());
        By todoCard    = By.cssSelector("[data-testid='card-001']");
        By doneColumn  = By.cssSelector("[data-testid='column-done']");

        actions.dragAndDrop(todoCard, doneColumn);

        // Verify card moved — now inside done column
        WebElement doneCol = driver().findElement(doneColumn);
        assertThat(doneCol.findElements(By.cssSelector("[data-testid='card-001']")))
            .hasSize(1);
    }

    @Test
    void ctrlSaveKeyboardShortcutSavesDocument() {
        navigateTo("/editor");
        ActionsHelper actions = new ActionsHelper(driver());

        driver().findElement(By.cssSelector("[data-testid='editor']"))
            .sendKeys("Draft content");
        actions.pressShortcut(Keys.CONTROL, "s");

        new AppWait(driver()).untilHasText(
            By.cssSelector("[data-testid='save-status']"), "Saved");
    }

    @Test
    void hoverMenuNavigatesToReports() {
        ActionsHelper actions = new ActionsHelper(driver());
        actions.navigateHoverMenuWithWaits(List.of(
            By.cssSelector("[data-testid='nav-analytics']"),    // top-level
            By.cssSelector("[data-testid='submenu-reports']"),  // sub-level
            By.cssSelector("[data-testid='menu-monthly']")      // leaf item
        ));
        new AppWait(driver()).untilDocumentReady();
        assertThat(driver().getCurrentUrl()).contains("/reports/monthly");
    }

    @Test
    void rightClickOnFileOpensContextMenu() {
        navigateTo("/file-manager");
        ActionsHelper actions = new ActionsHelper(driver());
        actions.rightClickAndSelect(
            By.cssSelector("[data-testid='file-document.pdf']"),
            By.cssSelector("[data-testid='context-menu-download']")
        );
        // Verify download initiated
        assertThat(new FileDownloadHelper(driver())
            .waitForDownload("document\\.pdf", Duration.ofSeconds(15)))
            .exists();
    }
}
```

### Key Design Decisions
- Both Actions and JS DnD implementations — HTML4 native draggable uses Actions; React/Vue DnD libraries require JS `DragEvent` simulation
- `navigateHoverMenuWithWaits()` — waits for each submenu level to appear before moving to next; avoids race where submenu hasn't rendered
- Modifier key `keyDown`/`keyUp` pairing — always release modifier keys; leaving `keyDown` active corrupts subsequent actions
- `scrollToElement` uses Selenium 4.2+ native Actions API — more reliable than JS `scrollIntoView` which can leave element partially obscured by sticky header

---

## CQ8: Build a complete test retry extension with exponential backoff and failure classification

### Problem Statement
Implement a JUnit 5 extension that retries failed tests with exponential backoff, distinguishes retryable failures (infrastructure) from non-retryable ones (assertion failures), logs retry attempts with context, and records total retry count in the test report.

### Solution

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
    int maxAttempts() default 3;
    long initialDelayMs() default 500;
    double backoffMultiplier() default 2.0;
}

public class RetryExtension implements TestExecutionExceptionHandler,
                                       BeforeTestExecutionCallback,
                                       AfterTestExecutionCallback {

    // Stores retry state per test — keyed by unique test ID
    private static final ConcurrentHashMap<String, RetryState> retryStates =
        new ConcurrentHashMap<>();

    @Override
    public void beforeTestExecution(ExtensionContext ctx) {
        String id = ctx.getUniqueId();
        retryStates.put(id, new RetryState(0, getMaxAttempts(ctx)));
    }

    @Override
    public void handleTestExecutionException(ExtensionContext ctx, Throwable throwable)
            throws Throwable {

        if (!isRetryable(throwable)) {
            log.error("[NO RETRY] {} — not retryable: {}",
                ctx.getDisplayName(), throwable.getClass().getSimpleName());
            throw throwable;
        }

        String id = ctx.getUniqueId();
        RetryState state = retryStates.get(id);

        if (state == null || state.attempt() >= state.maxAttempts() - 1) {
            log.error("[RETRY EXHAUSTED] {} — failed after {} attempts",
                ctx.getDisplayName(), state != null ? state.attempt() + 1 : 1);
            recordRetryToAllure(state != null ? state.attempt() + 1 : 1);
            throw throwable;
        }

        int nextAttempt = state.attempt() + 1;
        retryStates.put(id, new RetryState(nextAttempt, state.maxAttempts()));

        long delayMs = computeDelay(nextAttempt, ctx);
        log.warn("[RETRY {}/{}] {} — cause: {} | waiting {}ms before retry",
            nextAttempt, state.maxAttempts() - 1,
            ctx.getDisplayName(),
            throwable.getMessage(),
            delayMs);

        // Full driver reset between retries
        try { DriverFactory.quitDriver(); } catch (Exception ignored) {}
        Thread.sleep(delayMs);
        DriverFactory.initDriver(ConfigReader.get("browser", "chrome"));

        // Re-invoke test method
        try {
            ctx.getRequiredTestMethod().invoke(ctx.getRequiredTestInstance());
        } catch (InvocationTargetException e) {
            throw e.getCause(); // unwrap — handleTestExecutionException called again
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot re-invoke test method", e);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext ctx) {
        retryStates.remove(ctx.getUniqueId());
    }

    // ── RETRYABLE CLASSIFICATION ──────────────────────────────────────
    private boolean isRetryable(Throwable t) {
        // Infrastructure failures → retry
        if (t instanceof WebDriverException) return true;
        if (t instanceof TimeoutException) return true;
        if (t instanceof StaleElementReferenceException) return true;
        if (t instanceof SessionNotCreatedException) return true;
        if (t instanceof NoSuchSessionException) return true;

        // Application / assertion failures → do NOT retry (they're real bugs)
        if (t instanceof AssertionError) return false;
        if (t instanceof NullPointerException) return false;
        if (t instanceof IllegalArgumentException) return false;
        if (t instanceof LoginFailedException) return false;
        if (t instanceof ConfigurationException) return false;

        // Unknown RuntimeException — check message for clues
        if (t instanceof RuntimeException) {
            String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
            if (msg.contains("connection refused")) return true;
            if (msg.contains("connection reset")) return true;
            if (msg.contains("read timed out")) return true;
            if (msg.contains("unable to connect")) return true;
        }
        return false; // default: don't retry unknown exceptions
    }

    private long computeDelay(int attempt, ExtensionContext ctx) {
        Retryable ann = ctx.getRequiredTestMethod().getAnnotation(Retryable.class);
        long initial    = ann != null ? ann.initialDelayMs()    : 500L;
        double mult     = ann != null ? ann.backoffMultiplier() : 2.0;
        // Exponential with 10% jitter to avoid thundering herd
        long delay = (long) (initial * Math.pow(mult, attempt - 1));
        delay += (long) (delay * 0.1 * Math.random()); // jitter
        return Math.min(delay, 30_000L); // cap at 30s
    }

    private int getMaxAttempts(ExtensionContext ctx) {
        Retryable ann = ctx.getElement()
            .map(e -> ((java.lang.reflect.Method) e).getAnnotation(Retryable.class))
            .orElse(null);
        return ann != null ? ann.maxAttempts() : 3;
    }

    private void recordRetryToAllure(int totalAttempts) {
        if (totalAttempts > 1) {
            Allure.addAttachment("Retry Info",
                "Test was retried " + (totalAttempts - 1) + " time(s) before final result.");
        }
    }

    record RetryState(int attempt, int maxAttempts) {}
}

// REGISTRATION — src/test/resources/META-INF/services/
// org.junit.jupiter.api.extension.Extension
// → com.example.framework.RetryExtension

// USAGE
@ExtendWith(RetryExtension.class)
class PaymentIntegrationTest extends BaseTest {

    // Default retry (3 attempts, 500ms → 1s → 2s backoff)
    @Test
    void paymentGatewayProcessesCharge() {
        // ...
    }

    // Custom retry — more attempts with longer initial delay
    @Test
    @Retryable(maxAttempts = 5, initialDelayMs = 1000, backoffMultiplier = 1.5)
    void slowThirdPartyIntegration() {
        // ...
    }

    // Explicitly non-retryable — override annotation to disable
    @Test
    @Retryable(maxAttempts = 1)
    void assertionOnCriticalBusinessRule() {
        // Failure here is always a real bug — never retry
    }
}
```

### Key Design Decisions
- Retryable classification is strict — `AssertionError` never retries; retrying assertion failures hides real bugs
- Exponential backoff with jitter — prevents all retrying tests hitting the Grid simultaneously (thundering herd)
- Full driver reset between retries — stale browser state is a common cause of flakiness; fresh driver eliminates it
- `@Retryable` annotation — per-test control; some tests genuinely need more attempts (slow third-party APIs)
- `ConcurrentHashMap` keyed by test ID — thread-safe for parallel execution; each test's retry state is isolated

---

## CQ9: Write an Allure reporting integration with custom steps, attachments, and environment info

### Problem Statement
Implement a complete Allure integration: custom `@Step` methods, automatic screenshot on failure, environment properties file generation, test categories, and severity tagging. Include a `TestWatcher` that attaches browser logs on failure.

### Solution

```java
// ALLURE TEST WATCHER EXTENSION
public class AllureExtension implements
        BeforeEachCallback, AfterEachCallback, TestWatcher {

    @Override
    public void beforeEach(ExtensionContext ctx) {
        // Set Allure test metadata programmatically
        String displayName = ctx.getDisplayName();
        Allure.getLifecycle().updateTestCase(tc -> {
            tc.setName(displayName);
            tc.addLabel(ResultsUtils.createLabel("suite",
                ctx.getParent().map(ExtensionContext::getDisplayName).orElse("Unknown")));
            tc.addLabel(ResultsUtils.createLabel("environment",
                ConfigReader.get("env", "staging")));
            tc.addLabel(ResultsUtils.createLabel("browser",
                ConfigReader.get("browser", "chrome")));
        });
    }

    @Override
    public void afterEach(ExtensionContext ctx) { /* cleanup if needed */ }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        WebDriver driver = DriverFactory.getDriver();
        if (driver == null) return;

        // Screenshot
        attachScreenshot(driver, "Screenshot at Failure");

        // Page source
        Allure.addAttachment("Page Source",
            "text/html", driver.getPageSource(), ".html");

        // Current URL
        Allure.addAttachment("URL", driver.getCurrentUrl());

        // Browser console logs (Chrome only)
        attachBrowserLogs(driver);

        // Stack trace formatted nicely
        Allure.addAttachment("Stack Trace",
            ExceptionUtils.getStackTrace(cause));
    }

    public static void attachScreenshot(WebDriver driver, String name) {
        if (driver instanceof TakesScreenshot ts) {
            byte[] png = ts.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png",
                new ByteArrayInputStream(png), ".png");
        }
    }

    private void attachBrowserLogs(WebDriver driver) {
        try {
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            if (!logs.getAll().isEmpty()) {
                String logText = logs.getAll().stream()
                    .filter(e -> e.getLevel().intValue() >= Level.WARNING.intValue())
                    .map(e -> "[" + e.getLevel() + "] " + e.getMessage())
                    .collect(Collectors.joining("\n"));
                if (!logText.isBlank())
                    Allure.addAttachment("Browser Console Errors/Warnings", logText);
            }
        } catch (Exception e) {
            log.debug("Could not capture browser logs: {}", e.getMessage());
        }
    }
}

// ALLURE STEP HELPERS — use in page objects and tests
public class AllureSteps {

    @Step("Navigate to {url}")
    public static void navigateTo(WebDriver driver, String url) {
        driver.get(url);
    }

    @Step("Login as {email}")
    public static DashboardPage loginAs(WebDriver driver, String email, String password) {
        return LoginPage.navigateTo(driver, ConfigReader.get("baseUrl"))
            .loginAs(email, password);
    }

    @Step("Take screenshot: {name}")
    public static void screenshot(WebDriver driver, String name) {
        AllureExtension.attachScreenshot(driver, name);
    }

    @Step("Assert element '{locatorDescription}' has text '{expectedText}'")
    public static void assertText(WebDriver driver,
                                  By locator, String locatorDescription,
                                  String expectedText) {
        String actual = new AppWait(driver).untilVisible(locator).getText().trim();
        assertThat(actual).as("Text of " + locatorDescription).isEqualTo(expectedText);
    }
}

// SEVERITY + CATEGORY ANNOTATION USAGE
@Epic("Checkout")
@Feature("Payment")
@Story("Credit Card Payment")
@Severity(SeverityLevel.CRITICAL)
@Owner("payments-team")
@TmsLink("TC-1042")
@Issue("PAY-501")
class PaymentTest extends BaseTest {

    @Test
    @DisplayName("Valid Visa card completes checkout successfully")
    void visaCheckout() {
        // steps auto-appear in Allure timeline
        AllureSteps.loginAs(driver(), "customer@test.com", "pass");
        navigateTo("/checkout");
        AllureSteps.screenshot(driver(), "Before payment");
        new CheckoutPage(driver()).payWithCard("4242424242424242");
        AllureSteps.assertText(driver(),
            By.cssSelector("[data-testid='confirmation-msg']"),
            "confirmation message",
            "Payment successful");
    }
}

// ALLURE ENVIRONMENT PROPERTIES — generated at suite start
public class AllureEnvironmentWriter {
    @BeforeSuite
    public static void writeEnvironmentProperties() {
        Properties env = new Properties();
        env.setProperty("Browser", ConfigReader.get("browser", "chrome"));
        env.setProperty("Environment", ConfigReader.get("env", "staging"));
        env.setProperty("Base URL", ConfigReader.get("baseUrl"));
        env.setProperty("Grid URL", ConfigReader.get("gridUrl", "local"));
        env.setProperty("Selenium Version",
            RemoteWebDriver.class.getPackage().getImplementationVersion());
        env.setProperty("Java Version", System.getProperty("java.version"));
        env.setProperty("OS", System.getProperty("os.name"));
        env.setProperty("Test Run ID", System.getProperty("BUILD_NUMBER", "local"));

        Path allureResultsDir = Paths.get("target/allure-results");
        try {
            Files.createDirectories(allureResultsDir);
            try (OutputStream out = Files.newOutputStream(
                    allureResultsDir.resolve("environment.properties"))) {
                env.store(out, "Test Environment Configuration");
            }
        } catch (IOException e) {
            log.warn("Could not write allure environment.properties: {}", e.getMessage());
        }
    }
}

// ALLURE CATEGORIES — target/allure-results/categories.json
/*
[
  {
    "name": "Infrastructure failures",
    "messageRegex": ".*WebDriverException.*|.*Connection refused.*",
    "matchedStatuses": ["broken"]
  },
  {
    "name": "Assertion failures",
    "messageRegex": ".*AssertionError.*|.*expected.*but was.*",
    "matchedStatuses": ["failed"]
  },
  {
    "name": "Timeout failures",
    "messageRegex": ".*TimeoutException.*|.*Timed out.*",
    "matchedStatuses": ["broken"]
  }
]
*/
```

### Key Design Decisions
- `TestWatcher.testFailed()` — only captures screenshot/logs on failure; not on every test (avoids 500 × 2MB = 1GB report)
- Browser log filter for `WARNING` and above — avoids noise from debug-level console messages
- `@Step` on page object methods — Allure timeline shows steps even when test passes; valuable for review
- `environment.properties` written in `@BeforeSuite` — appears in Allure report's environment tab for every run
- `categories.json` — separates infrastructure failures (broken) from code failures (failed) in Allure's category view

---

## CQ10: Implement a TestNG parallel suite with data providers, groups, and cross-test dependencies

### Problem Statement
Build a complete TestNG configuration with: parallel execution across test methods, a `@DataProvider` that reads from JSON, group-based execution (`smoke`, `regression`), a dependency chain (`loginTest` must pass before `checkoutTest`), and `IRetryAnalyzer` integration.

### Solution

```java
// TESTNG XML SUITE — testng-regression.xml
/*
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Regression Suite" parallel="methods" thread-count="10"
       data-provider-thread-count="5" verbose="1">

  <listeners>
    <listener class-name="com.example.framework.RetryListener"/>
    <listener class-name="com.example.framework.AllureTestNgListener"/>
    <listener class-name="com.example.framework.DriverManagementListener"/>
  </listeners>

  <test name="Smoke Tests">
    <groups>
      <run><include name="smoke"/></run>
    </groups>
    <classes>
      <class name="com.example.tests.LoginTest"/>
      <class name="com.example.tests.HomePageTest"/>
      <class name="com.example.tests.CheckoutTest"/>
    </classes>
  </test>

  <test name="Regression Tests">
    <groups>
      <run><include name="regression"/></run>
    </groups>
    <packages>
      <package name="com.example.tests"/>
    </packages>
  </test>
</suite>
*/

// RETRY LISTENER — registers IRetryAnalyzer on all tests
public class RetryListener implements IAnnotationTransformer {
    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass, Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(TestNgRetryAnalyzer.class);
        }
    }
}

// RETRY ANALYZER
public class TestNgRetryAnalyzer implements IRetryAnalyzer {
    private static final int MAX_RETRIES = 2;
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        if (!isRetryable(result.getThrowable())) return false;
        String key = result.getMethod().getQualifiedName();
        int count  = counts.getOrDefault(key, 0);
        if (count < MAX_RETRIES) {
            counts.put(key, count + 1);
            log.warn("[TestNG RETRY {}/{}] {}", count + 1, MAX_RETRIES, key);
            return true;
        }
        return false;
    }

    private boolean isRetryable(Throwable t) {
        if (t == null) return false;
        return t instanceof WebDriverException
            || t instanceof TimeoutException
            || (t.getMessage() != null && t.getMessage().contains("Connection refused"));
    }
}

// DRIVER MANAGEMENT LISTENER — TestNG lifecycle
public class DriverManagementListener implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
        String browser = ConfigReader.get("browser", "chrome");
        DriverFactory.initDriver(browser);
        MDC.put("testMethod", result.getMethod().getMethodName());
        MDC.put("thread", Thread.currentThread().getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) { cleanup(); }

    @Override
    public void onTestFailure(ITestResult result) {
        // Screenshot + source on failure
        WebDriver driver = DriverFactory.getDriver();
        AllureExtension.attachScreenshot(driver, "Failure Screenshot");
        cleanup();
    }

    @Override
    public void onTestSkipped(ITestResult result) { cleanup(); }

    private void cleanup() {
        DriverFactory.quitDriver();
        MDC.clear();
    }
}

// JSON DATA PROVIDER
public class JsonDataProvider {
    @DataProvider(name = "checkoutScenarios", parallel = true)
    public static Object[][] checkoutScenarios() throws IOException {
        // src/test/resources/data/checkout-scenarios.json
        InputStream is = JsonDataProvider.class.getResourceAsStream(
            "/data/checkout-scenarios.json");
        List<Map<String, Object>> scenarios =
            new ObjectMapper().readValue(is, new TypeReference<>() {});

        return scenarios.stream()
            .map(s -> new Object[]{
                s.get("scenarioName"),
                s.get("cardNumber"),
                s.get("expectedResult"),
                s.get("expectedMessage")
            })
            .toArray(Object[][]::new);
    }

    @DataProvider(name = "loginCredentials")
    public static Object[][] loginCredentials() {
        return new Object[][] {
            {"user@test.com",         "ValidPass1!",     "CUSTOMER", true},
            {"admin@test.com",        "AdminPass1!",     "ADMIN",    true},
            {"user@test.com",         "wrongpassword",   null,       false},
            {"nonexistent@test.com",  "somepassword",    null,       false},
            {"",                      "somepassword",    null,       false},
        };
    }
}

// TEST CLASS WITH GROUPS, DEPENDENCIES, DATA PROVIDERS
@Listeners({RetryListener.class, DriverManagementListener.class})
public class CheckoutTest {
    private static String authToken; // shared across thread — only used for read-only setup

    @BeforeClass(alwaysRun = true)
    public static void setupAuthToken() {
        // Pre-create auth token once for the class
        TestUser user = UserApiFactory.createUser("CUSTOMER");
        authToken = UserApiFactory.getAuthToken(user);
    }

    @Test(groups = {"smoke", "regression"},
          description = "Login with valid credentials succeeds")
    public void loginTest() {
        driver().get(BASE_URL + "/login");
        new LoginPage(driver()).loginAs("customer@test.com", "ValidPass123!");
        assertThat(driver().getCurrentUrl()).contains("/dashboard");
    }

    @Test(groups = {"regression"},
          dependsOnMethods = {"loginTest"},  // only runs if loginTest passes
          description = "Add item to cart after login")
    public void addToCartTest() {
        // Inject token — fast auth bypass
        new BrowserStorageUtils(driver()).injectJwtToken(authToken);
        navigateTo("/products/LAPTOP-001");
        new ProductPage(driver()).addToCart();
        assertThat(new CartPage(driver()).getItemCount()).isEqualTo(1);
    }

    @Test(groups = {"regression"},
          dependsOnMethods = {"addToCartTest"},
          dataProvider = "checkoutScenarios",
          dataProviderClass = JsonDataProvider.class,
          description = "Checkout with various card scenarios")
    public void checkoutTest(String scenarioName, String cardNumber,
                              String expectedResult, String expectedMessage) {
        log.info("Running checkout scenario: {}", scenarioName);
        new BrowserStorageUtils(driver()).injectJwtToken(authToken);
        navigateTo("/checkout");

        CheckoutPage checkout = new CheckoutPage(driver());
        checkout.fillCard(cardNumber).submit();

        if ("success".equals(expectedResult)) {
            new AppWait(driver()).untilHasText(
                By.cssSelector("[data-testid='result']"), expectedMessage);
        } else {
            new AppWait(driver()).untilHasText(
                By.cssSelector("[data-testid='error']"), expectedMessage);
        }
    }

    @Test(groups = {"smoke"},
          description = "Smoke: homepage loads within 3 seconds")
    public void homepagePerfTest() {
        long start = System.currentTimeMillis();
        driver().get(BASE_URL);
        new AppWait(driver()).untilDocumentReady();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).as("Homepage load time").isLessThan(3000L);
    }

    private WebDriver driver() { return DriverFactory.getDriver(); }
    private void navigateTo(String path) {
        driver().get(ConfigReader.get("baseUrl") + path);
    }
}

// DATA FILE — src/test/resources/data/checkout-scenarios.json
/*
[
  {"scenarioName":"Valid Visa",   "cardNumber":"4242424242424242",
   "expectedResult":"success",   "expectedMessage":"Payment successful"},
  {"scenarioName":"Declined card","cardNumber":"4000000000000002",
   "expectedResult":"error",     "expectedMessage":"Your card was declined"},
  {"scenarioName":"Insufficient", "cardNumber":"4000000000009995",
   "expectedResult":"error",     "expectedMessage":"Insufficient funds"},
  {"scenarioName":"Expired card", "cardNumber":"4000000000000069",
   "expectedResult":"error",     "expectedMessage":"Your card has expired"}
]
*/
```

### Key Design Decisions
- `IAnnotationTransformer` (RetryListener) — automatically applies `IRetryAnalyzer` to ALL tests without requiring each test class to declare it
- `dependsOnMethods` — hard dependency: if `loginTest` fails, `addToCartTest` and `checkoutTest` are skipped (not failed) — reports accurately distinguish "dependency failed" from "test itself failed"
- `parallel = true` on `@DataProvider` — each data row runs in a separate thread — multiplies parallelism (10 threads × 4 data rows = 40 simultaneous browser sessions)
- `data-provider-thread-count=5` in suite XML — limits DataProvider parallelism independently from method parallelism
- JSON data provider — externalizes test data from code; non-engineers can add scenarios without touching Java

---

## CQ11: Write a complete custom `ExpectedCondition` for waiting on a React/Angular state

### Problem Statement
Create three `ExpectedCondition` implementations: (1) waits until an element's attribute equals a value, (2) waits until AJAX request count reaches zero, (3) waits until a React component prop/state has a specific value (via JS).

### Solution

```java
public class CustomExpectedConditions {

    // ── 1. ATTRIBUTE EQUALS ───────────────────────────────────────────
    public static ExpectedCondition<WebElement> attributeEquals(
            By locator, String attribute, String expectedValue) {
        return new ExpectedCondition<>() {
            @Override
            public WebElement apply(WebDriver driver) {
                try {
                    WebElement el = driver.findElement(locator);
                    String actual = el.getDomAttribute(attribute);
                    return expectedValue.equals(actual) ? el : null;
                } catch (StaleElementReferenceException | NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return String.format(
                    "attribute '%s' of element %s to equal '%s'",
                    attribute, locator, expectedValue);
            }
        };
    }

    // ── 2. ATTRIBUTE CONTAINS ─────────────────────────────────────────
    public static ExpectedCondition<WebElement> attributeContains(
            By locator, String attribute, String substring) {
        return new ExpectedCondition<>() {
            @Override
            public WebElement apply(WebDriver driver) {
                try {
                    WebElement el = driver.findElement(locator);
                    String actual = el.getDomAttribute(attribute);
                    return (actual != null && actual.contains(substring)) ? el : null;
                } catch (StaleElementReferenceException | NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return String.format("attribute '%s' of %s to contain '%s'",
                    attribute, locator, substring);
            }
        };
    }

    // ── 3. AJAX REQUESTS COMPLETE ─────────────────────────────────────
    public static ExpectedCondition<Boolean> ajaxRequestsComplete() {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(WebDriver driver) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                // Works for jQuery apps
                Object jqueryActive = js.executeScript(
                    "return (typeof jQuery !== 'undefined') ? jQuery.active : 0;");
                if (jqueryActive instanceof Long && (Long) jqueryActive > 0) return null;

                // Works for apps using our counter injection (AppWait.untilNetworkIdle)
                Object pendingCount = js.executeScript(
                    "return window.__pendingRequests || 0;");
                if (pendingCount instanceof Long && (Long) pendingCount > 0) return null;

                return Boolean.TRUE;
            }

            @Override
            public String toString() { return "all AJAX requests to complete"; }
        };
    }

    // ── 4. ELEMENT COUNT BETWEEN MIN AND MAX ──────────────────────────
    public static ExpectedCondition<List<WebElement>> elementCountBetween(
            By locator, int minCount, int maxCount) {
        return new ExpectedCondition<>() {
            @Override
            public List<WebElement> apply(WebDriver driver) {
                List<WebElement> els = driver.findElements(locator);
                return (els.size() >= minCount && els.size() <= maxCount) ? els : null;
            }

            @Override
            public String toString() {
                return String.format("element count of %s to be between %d and %d",
                    locator, minCount, maxCount);
            }
        };
    }

    // ── 5. REACT COMPONENT STATE ──────────────────────────────────────
    public static ExpectedCondition<Boolean> reactComponentState(
            By componentLocator, String stateKey, Object expectedValue) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(WebDriver driver) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                try {
                    WebElement el = driver.findElement(componentLocator);
                    Object actual = js.executeScript(
                        "var el = arguments[0];" +
                        "var key = Object.keys(el).find(k => k.startsWith('__reactFiber'));" +
                        "if (!key) return null;" +
                        "var fiber = el[key];" +
                        "var state = fiber.memoizedState;" +
                        "if (!state) return null;" +
                        "return state.memoizedState !== undefined " +
                        "       ? state.memoizedState[arguments[1]] " +
                        "       : state[arguments[1]];",
                        el, stateKey);
                    return Objects.equals(actual != null ? actual.toString() : null,
                        expectedValue != null ? expectedValue.toString() : null);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return String.format("React state '%s' of %s to equal '%s'",
                    stateKey, componentLocator, expectedValue);
            }
        };
    }

    // ── 6. URL REGEX MATCH ────────────────────────────────────────────
    public static ExpectedCondition<Boolean> urlMatches(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return pattern.matcher(driver.getCurrentUrl()).find() ? Boolean.TRUE : null;
            }

            @Override
            public String toString() {
                return "URL to match regex: " + regex;
            }
        };
    }

    // ── 7. PAGE TITLE MATCHES ─────────────────────────────────────────
    public static ExpectedCondition<Boolean> titleMatches(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return pattern.matcher(driver.getTitle()).find() ? Boolean.TRUE : null;
            }

            @Override
            public String toString() { return "page title to match: " + regex; }
        };
    }
}

// USAGE IN TESTS
class CustomConditionTest extends BaseTest {
    @Test
    void tabBecomesActiveAfterClick() {
        navigateTo("/dashboard");
        By analyticsTab = By.cssSelector("[data-testid='tab-analytics']");

        driver().findElement(analyticsTab).click();

        // Wait for aria-selected="true" on the clicked tab
        new WebDriverWait(driver(), Duration.ofSeconds(10))
            .until(CustomExpectedConditions.attributeEquals(
                analyticsTab, "aria-selected", "true"));
    }

    @Test
    void searchResultsLoadAfterAjax() {
        navigateTo("/search");
        driver().findElement(By.cssSelector("[data-testid='search']"))
            .sendKeys("laptop" + Keys.ENTER);

        WebDriverWait wait = new WebDriverWait(driver(), Duration.ofSeconds(15));
        // Wait for all AJAX to complete
        wait.withMessage("AJAX requests did not complete")
            .until(CustomExpectedConditions.ajaxRequestsComplete());
        // Then wait for results between 1 and 50
        List<WebElement> results = wait
            .until(CustomExpectedConditions.elementCountBetween(
                By.cssSelector("[data-testid='result']"), 1, 50));
        assertThat(results).isNotEmpty();
    }

    @Test
    void urlContainsOrderIdAfterCheckout() {
        // After checkout, URL should be /orders/ORD-\d{6}
        new WebDriverWait(driver(), Duration.ofSeconds(15))
            .until(CustomExpectedConditions.urlMatches("/orders/ORD-\\d{6}"));
    }
}
```

### Key Design Decisions
- `toString()` override on every `ExpectedCondition` — appears in `TimeoutException` message; makes failures self-describing without reading source code
- `return null` (not `false`) for "not yet" — `ExpectedCondition` returning `null` means "retry"; returning `false` means "stop, condition is false"
- `getDomAttribute()` not `getAttribute()` — `getAttribute()` in Selenium 4 falls back to property if attribute absent; `getDomAttribute()` is precise
- React fiber state access — uses React internal fiber key; fragile by design, documented; only use when no better DOM indicator exists
- jQuery + custom counter check in `ajaxRequestsComplete` — covers both jQuery AJAX and modern `fetch`/XHR apps

---

## CQ12: Implement a parallel test execution setup for JUnit 5 with resource locking

### Problem Statement
Configure JUnit 5 for 20-thread parallel execution with: `@ResourceLock` for tests that must not run concurrently, `@Isolated` for tests requiring exclusive resource access, and a custom `ParallelExecutionCondition` that disables parallelism for specific test classes.

### Solution

```java
// junit-platform.properties
/*
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=20
junit.jupiter.execution.parallel.config.fixed.max-pool-size=25
*/

// RESOURCE LOCK CONSTANTS
public class Resources {
    public static final String EMAIL_SERVICE  = "email-service";
    public static final String PAYMENT_CONFIG = "payment-config";
    public static final String ADMIN_USER     = "admin-user";
    public static final String REPORT_QUEUE   = "report-queue";
    public static final String DATABASE       = "database";
}

// TESTS USING RESOURCE LOCKS
class EmailNotificationTest extends BaseTest {

    // READ lock — multiple tests can run concurrently while reading email config
    @Test
    @ResourceLock(value = Resources.EMAIL_SERVICE, mode = ResourceAccessMode.READ)
    void orderConfirmationEmailIsSent() {
        // read-only: verifies email is sent using current config
        UserApiFactory.createUser("CUSTOMER");
        // ... place order ...
        assertThat(EmailInbox.waitForEmail("order.confirmation@example.com",
            Duration.ofSeconds(30))).isNotNull();
    }

    // READ_WRITE lock — exclusive access while modifying email config
    @Test
    @ResourceLock(value = Resources.EMAIL_SERVICE, mode = ResourceAccessMode.READ_WRITE)
    void emailsDisabledWhenConfigTurnedOff() {
        EmailConfigApi.disable(); // modifies shared email config
        try {
            // place order — no email should be sent
            UserApiFactory.createUser("CUSTOMER");
            // ... place order ...
            assertThat(EmailInbox.hasEmail("order.confirmation@example.com",
                Duration.ofSeconds(5))).isFalse();
        } finally {
            EmailConfigApi.enable(); // restore for other tests
        }
    }
}

// ISOLATED TEST — runs alone, no other tests concurrently
@Isolated("Modifies global payment gateway configuration")
class PaymentGatewayConfigTest extends BaseTest {

    @Test
    void sandboxModeProcessesTestCards() {
        // This test flips the payment gateway to sandbox mode globally
        // @Isolated ensures no other test is running simultaneously
        PaymentConfigApi.setSandboxMode(true);
        try {
            new CheckoutPage(driver()).payWithCard("4242424242424242");
            assertThat(new ConfirmationPage(driver()).getPaymentId())
                .startsWith("sandbox_");
        } finally {
            PaymentConfigApi.setSandboxMode(false);
        }
    }
}

// MULTIPLE RESOURCE LOCKS — test needs both locks simultaneously
class AdminReportTest extends BaseTest {

    @Test
    @ResourceLock(value = Resources.ADMIN_USER,     mode = ResourceAccessMode.READ)
    @ResourceLock(value = Resources.REPORT_QUEUE,   mode = ResourceAccessMode.READ_WRITE)
    void adminCanGenerateMonthlyReport() {
        loginAs("ADMIN");
        new AdminPage(driver()).triggerMonthlyReport();
        // Waits for report queue to process
        assertThat(new AppWait(driver())
            .untilVisible(By.cssSelector("[data-testid='report-ready']"))
            .getText()).contains("Report generated");
    }
}

// CUSTOM PARALLEL CONDITION — disable parallelism per-class annotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RunSequentially {
    String reason() default "";
}

public class SequentialExecutionCondition implements ParallelExecutionCondition {
    private static final ExecutionMode CONCURRENT = ExecutionMode.CONCURRENT;
    private static final ExecutionMode SEQUENTIAL  = ExecutionMode.SAME_THREAD;

    @Override
    public ExecutionMode getExecutionMode(ExtensionContext ctx) {
        // Check class annotation
        boolean isSequential = ctx.getTestClass()
            .map(c -> c.isAnnotationPresent(RunSequentially.class))
            .orElse(false);

        if (isSequential) {
            String reason = ctx.getTestClass()
                .map(c -> c.getAnnotation(RunSequentially.class).reason())
                .orElse("unknown");
            log.debug("Running {} sequentially: {}", ctx.getDisplayName(), reason);
            return SEQUENTIAL;
        }
        return CONCURRENT;
    }
}

// USAGE
@RunSequentially(reason = "Tests share a single bank account balance state")
class BankBalanceTest extends BaseTest {
    // These tests all run on the same thread — no concurrent state changes
    @Test void depositIncreasesBalance() { /* ... */ }
    @Test void withdrawalDecreasesBalance() { /* ... */ }
    @Test void overdraftIsRejected() { /* ... */ }
}

// THREAD POOL CONFIGURATION — fine-grained for different test categories
/*
# Separate pools for different test types (via Maven Surefire groups)
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <executions>
    <execution>
      <id>smoke-tests</id>
      <goals><goal>test</goal></goals>
      <configuration>
        <groups>smoke</groups>
        <systemPropertyVariables>
          <junit.jupiter.execution.parallel.config.fixed.parallelism>5</junit.jupiter.execution.parallel.config.fixed.parallelism>
        </systemPropertyVariables>
      </configuration>
    </execution>
    <execution>
      <id>regression-tests</id>
      <goals><goal>test</goal></goals>
      <configuration>
        <groups>regression</groups>
        <systemPropertyVariables>
          <junit.jupiter.execution.parallel.config.fixed.parallelism>20</junit.jupiter.execution.parallel.config.fixed.parallelism>
        </systemPropertyVariables>
      </configuration>
    </execution>
  </executions>
</plugin>
*/
```

### Key Design Decisions
- `READ` vs `READ_WRITE` locks — multiple tests can hold `READ` simultaneously; `READ_WRITE` requires exclusive access; models reader-writer concurrency correctly
- `@Isolated` for global config changes — cleaner than `@ResourceLock` when a test requires the entire environment to itself
- `@RunSequentially` custom annotation — class-level sequencing without listing every method with `@ResourceLock`; documents the reason
- `ParallelExecutionCondition` SPI — plugged into JUnit 5's extension mechanism; no framework changes needed

---

## CQ13: Write a CDP-based performance metrics collector with budget assertions

### Problem Statement
Build a `PerformanceMetricsCollector` that uses CDP to collect Core Web Vitals (LCP, FCP, CLS), navigation timing, memory usage, and custom user timing marks. Include assertion methods that fail if metrics exceed budget thresholds.

### Solution

```java
public class PerformanceMetricsCollector {
    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final DevTools devTools;

    // Performance budgets (configurable)
    private long maxPageLoadMs   = 3000;
    private long maxTtfbMs       = 500;
    private long maxFcpMs        = 1800;
    private long maxLcpMs        = 2500;
    private double maxClsScore   = 0.1;

    public PerformanceMetricsCollector(WebDriver driver) {
        this.driver    = driver;
        this.js        = (JavascriptExecutor) driver;
        this.devTools  = ((ChromeDriver) driver).getDevTools();
        this.devTools.createSession();
        injectWebVitalsObserver();
        enableCdpPerformance();
    }

    // Configure budgets fluently
    public PerformanceMetricsCollector maxPageLoad(long ms)  { maxPageLoadMs = ms; return this; }
    public PerformanceMetricsCollector maxTtfb(long ms)      { maxTtfbMs = ms; return this; }
    public PerformanceMetricsCollector maxFcp(long ms)       { maxFcpMs = ms; return this; }
    public PerformanceMetricsCollector maxLcp(long ms)       { maxLcpMs = ms; return this; }
    public PerformanceMetricsCollector maxCls(double score)  { maxClsScore = score; return this; }

    // ── INJECT WEB VITALS OBSERVER ────────────────────────────────────
    // Must be injected before navigation to capture all events
    private void injectWebVitalsObserver() {
        js.executeScript(
            "window.__perfMetrics = {lcp: null, fcp: null, cls: 0, clsEntries: []};" +
            "try {" +
            "  new PerformanceObserver(list => {" +
            "    list.getEntries().forEach(e => { window.__perfMetrics.lcp = e.startTime; });" +
            "  }).observe({type:'largest-contentful-paint', buffered:true});" +
            "  new PerformanceObserver(list => {" +
            "    list.getEntries().forEach(e => {" +
            "      if (e.name === 'first-contentful-paint')" +
            "        window.__perfMetrics.fcp = e.startTime;" +
            "    });" +
            "  }).observe({type:'paint', buffered:true});" +
            "  new PerformanceObserver(list => {" +
            "    list.getEntries().forEach(e => {" +
            "      if (!e.hadRecentInput) {" +
            "        window.__perfMetrics.cls += e.value;" +
            "        window.__perfMetrics.clsEntries.push(e.value);" +
            "      }" +
            "    });" +
            "  }).observe({type:'layout-shift', buffered:true});" +
            "} catch(err) { console.warn('PerfObserver setup failed:', err); }"
        );
    }

    private void enableCdpPerformance() {
        devTools.send(Performance.enable(Optional.empty()));
    }

    // ── NAVIGATION TIMING ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public NavigationTiming getNavigationTiming() {
        Map<String, Object> raw = (Map<String, Object>) js.executeScript(
            "var t = performance.getEntriesByType('navigation')[0] || performance.timing;" +
            "var base = t.fetchStart || t.navigationStart || 0;" +
            "return {" +
            "  ttfb:             (t.responseStart - (t.requestStart || t.fetchStart)) || 0," +
            "  domContentLoaded: (t.domContentLoadedEventEnd - base) || 0," +
            "  pageLoad:         (t.loadEventEnd - base) || 0," +
            "  dns:              (t.domainLookupEnd - t.domainLookupStart) || 0," +
            "  tcp:              (t.connectEnd - t.connectStart) || 0," +
            "  download:         (t.responseEnd - t.responseStart) || 0" +
            "};"
        );
        return new NavigationTiming(raw);
    }

    // ── WEB VITALS ────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public WebVitals getWebVitals() {
        // Wait for LCP to stabilize (fires up to 2.5s after load)
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(5))
            .pollingEvery(Duration.ofMillis(200))
            .until(d -> js.executeScript("return window.__perfMetrics.lcp !== null") != null);

        Map<String, Object> raw = (Map<String, Object>) js.executeScript(
            "return window.__perfMetrics;");
        return new WebVitals(raw);
    }

    // ── CDP METRICS ───────────────────────────────────────────────────
    public Map<String, Double> getCdpMetrics() {
        List<Metric> metrics = devTools.send(Performance.getMetrics());
        return metrics.stream()
            .collect(Collectors.toMap(
                Metric::getName,
                m -> m.getValue().doubleValue()));
    }

    // ── MEMORY ───────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public MemoryInfo getMemoryUsage() {
        Map<String, Object> raw = (Map<String, Object>) js.executeScript(
            "return performance.memory ? {" +
            "  usedJsHeapMb: performance.memory.usedJSHeapSize / 1048576," +
            "  totalJsHeapMb: performance.memory.totalJSHeapSize / 1048576" +
            "} : null;");
        return raw != null ? new MemoryInfo(raw) : null;
    }

    // ── CUSTOM USER TIMING ────────────────────────────────────────────
    public long getUserTimingMark(String markName) {
        Object result = js.executeScript(
            "var entries = performance.getEntriesByName(arguments[0], 'mark');" +
            "return entries.length > 0 ? entries[entries.length-1].startTime : null;",
            markName);
        if (result == null) throw new NoSuchElementException("Mark not found: " + markName);
        return ((Number) result).longValue();
    }

    // ── BUDGET ASSERTIONS ─────────────────────────────────────────────
    public void assertWithinBudget() {
        NavigationTiming nav = getNavigationTiming();
        WebVitals vitals      = getWebVitals();
        List<String> violations = new ArrayList<>();

        if (nav.pageLoad() > maxPageLoadMs)
            violations.add(String.format("PageLoad: %dms > budget %dms",
                nav.pageLoad(), maxPageLoadMs));
        if (nav.ttfb() > maxTtfbMs)
            violations.add(String.format("TTFB: %dms > budget %dms",
                nav.ttfb(), maxTtfbMs));
        if (vitals.fcp() != null && vitals.fcp() > maxFcpMs)
            violations.add(String.format("FCP: %.0fms > budget %dms",
                vitals.fcp(), maxFcpMs));
        if (vitals.lcp() != null && vitals.lcp() > maxLcpMs)
            violations.add(String.format("LCP: %.0fms > budget %dms",
                vitals.lcp(), maxLcpMs));
        if (vitals.cls() > maxClsScore)
            violations.add(String.format("CLS: %.4f > budget %.2f",
                vitals.cls(), maxClsScore));

        if (!violations.isEmpty()) {
            fail("Performance budget violations on " + driver.getCurrentUrl() + ":\n  - " +
                String.join("\n  - ", violations));
        }
    }

    // ── DATA CLASSES ──────────────────────────────────────────────────
    public record NavigationTiming(long ttfb, long domContentLoaded,
                                   long pageLoad, long dns, long tcp, long download) {
        @SuppressWarnings("unchecked")
        NavigationTiming(Map<String, Object> raw) {
            this(
                toLong(raw.get("ttfb")),
                toLong(raw.get("domContentLoaded")),
                toLong(raw.get("pageLoad")),
                toLong(raw.get("dns")),
                toLong(raw.get("tcp")),
                toLong(raw.get("download"))
            );
        }
        static long toLong(Object v) {
            return v instanceof Number n ? n.longValue() : 0L;
        }
    }

    public record WebVitals(Double lcp, Double fcp, double cls) {
        @SuppressWarnings("unchecked")
        WebVitals(Map<String, Object> raw) {
            this(
                raw.get("lcp") instanceof Number n ? n.doubleValue() : null,
                raw.get("fcp") instanceof Number n ? n.doubleValue() : null,
                raw.get("cls") instanceof Number n ? n.doubleValue() : 0.0
            );
        }
    }

    public record MemoryInfo(double usedJsHeapMb, double totalJsHeapMb) {
        @SuppressWarnings("unchecked")
        MemoryInfo(Map<String, Object> raw) {
            this(
                ((Number) raw.get("usedJsHeapMb")).doubleValue(),
                ((Number) raw.get("totalJsHeapMb")).doubleValue()
            );
        }
    }
}

// USAGE IN PERFORMANCE GATE TESTS
class PerformanceBudgetTest extends BaseTest {

    @Test
    void dashboardMeetsPerformanceBudget() {
        PerformanceMetricsCollector perf = new PerformanceMetricsCollector(driver())
            .maxPageLoad(2500).maxTtfb(400).maxLcp(2000).maxCls(0.05);

        loginAs("CUSTOMER");
        perf.injectWebVitalsObserver(); // re-inject after login navigation
        navigateTo("/dashboard");
        new AppWait(driver()).untilDocumentReady();

        perf.assertWithinBudget();

        // Also log metrics regardless of pass/fail
        NavigationTiming nav = perf.getNavigationTiming();
        Allure.addAttachment("Performance Metrics",
            "PageLoad: " + nav.pageLoad() + "ms\n" +
            "TTFB: " + nav.ttfb() + "ms\n" +
            "LCP: " + perf.getWebVitals().lcp() + "ms");
    }
}
```

### Key Design Decisions
- `injectWebVitalsObserver()` before navigation — LCP/FCP are fired during page load; observer must be registered first
- `PerformanceObserver` in `buffered:true` mode — captures entries that fired before observer was registered
- CLS accumulates across layout shift entries — not a single event; must sum all `layout-shift` entries without `hadRecentInput`
- Budget violations collected as list — fail with all violations at once, not just the first
- Fluent budget configuration — `maxPageLoad(2500).maxLcp(2000)` is readable and overridable per page type

---

## CQ14: Write a RestAssured integration to validate API contracts within Selenium tests

### Problem Statement
Build a reusable `ApiClient` using REST Assured with: request/response specification, JSON schema validation, authentication token management, and response time assertion — all usable from within Selenium test classes for hybrid UI+API testing.

### Solution

```java
// API CLIENT BASE — reusable across all API calls
public class ApiClient {
    private static final String BASE_URI = ConfigReader.get("apiBaseUrl");

    // Shared request spec — built once, reused everywhere
    private static final RequestSpecification BASE_SPEC =
        new RequestSpecBuilder()
            .setBaseUri(BASE_URI)
            .setContentType(ContentType.JSON)
            .addHeader("Accept", "application/json")
            .addHeader("X-Client", "selenium-test")
            .setRelaxedHTTPSValidation() // for self-signed certs on staging
            .addFilter(new RequestLoggingFilter(LogDetail.URI))
            .addFilter(new ResponseLoggingFilter(LogDetail.STATUS))
            .build();

    // Response spec — common assertions applied to every response
    private static final ResponseSpecification SUCCESS_SPEC =
        new ResponseSpecBuilder()
            .expectStatusCode(anyOf(is(200), is(201)))
            .expectContentType(ContentType.JSON)
            .expectResponseTime(Matchers.lessThan(3000L)) // 3s SLA
            .build();

    public static RequestSpecification authenticated(String token) {
        return new RequestSpecBuilder()
            .addRequestSpecification(BASE_SPEC)
            .addHeader("Authorization", "Bearer " + token)
            .build();
    }

    public static RequestSpecification asAdmin() {
        return authenticated(AuthTokenCache.getOrCreate("ADMIN"));
    }
}

// TYPED API CLIENTS — one per service/domain
public class OrderApiClient {

    // CREATE ORDER
    public static OrderResponse createOrder(String token, CreateOrderRequest request) {
        return given()
            .spec(ApiClient.authenticated(token))
            .body(request)
        .when()
            .post("/v1/orders")
        .then()
            .spec(ApiClient.SUCCESS_SPEC)
            .time(Matchers.lessThan(2000L)) // tighter SLA for write ops
            .extract()
            .as(OrderResponse.class);
    }

    // GET ORDER — with JSON Schema validation
    public static OrderResponse getOrder(String token, String orderId) {
        return given()
            .spec(ApiClient.authenticated(token))
        .when()
            .get("/v1/orders/{id}", orderId)
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/order-response-schema.json"))
            .extract()
            .as(OrderResponse.class);
    }

    // LIST ORDERS — with HAL pagination
    public static PagedResponse<OrderResponse> listOrders(
            String token, int page, int size) {
        return given()
            .spec(ApiClient.authenticated(token))
            .queryParam("page", page)
            .queryParam("size", size)
        .when()
            .get("/v1/orders")
        .then()
            .statusCode(200)
            .body("content.size()", greaterThanOrEqualTo(0))
            .body("pageable.pageNumber", equalTo(page))
            .extract()
            .as(new TypeRef<PagedResponse<OrderResponse>>() {});
    }

    // CANCEL ORDER — verify state transition
    public static void cancelOrder(String token, String orderId) {
        given()
            .spec(ApiClient.authenticated(token))
            .body(Map.of("reason", "Test cleanup"))
        .when()
            .post("/v1/orders/{id}/cancel", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"));
    }

    // ASSERT ORDER STATUS (with retry for async processing)
    public static void assertOrderStatus(String token, String orderId, String expectedStatus) {
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() ->
                given()
                    .spec(ApiClient.authenticated(token))
                .when()
                    .get("/v1/orders/{id}", orderId)
                .then()
                    .statusCode(200)
                    .body("status", equalTo(expectedStatus))
            );
    }
}

// JSON SCHEMA — src/test/resources/schemas/order-response-schema.json
/*
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type": "object",
  "required": ["id", "status", "total", "items", "createdAt"],
  "properties": {
    "id":        {"type": "string", "pattern": "^ORD-\\d{6}$"},
    "status":    {"type": "string", "enum": ["PENDING","CONFIRMED","SHIPPED","CANCELLED"]},
    "total":     {"type": "number", "minimum": 0},
    "items":     {"type": "array", "minItems": 1,
                  "items": {"type":"object",
                            "required":["sku","quantity","price"]}},
    "createdAt": {"type": "string", "format": "date-time"}
  },
  "additionalProperties": false
}
*/

// HYBRID TEST — UI action + API contract verification
class OrderApiContractTest extends BaseTest {
    private TestUser customer;
    private String authToken;

    @BeforeEach
    void setUp() {
        customer  = UserApiFactory.createUser("CUSTOMER");
        authToken = UserApiFactory.getAuthToken(customer);
        DriverFactory.initDriver("chrome");
    }

    @Test
    void placeOrderViaUiAndVerifyApiContract() {
        // Setup via API
        ProductApiClient.ensureProductExists("LAPTOP-001", 1299.99);

        // UI: Place order (user journey)
        driver().get(BASE_URL);
        new BrowserStorageUtils(driver()).injectJwtToken(authToken);
        navigateTo("/products/LAPTOP-001");
        String orderId = new ProductPage(driver())
            .addToCart()
            .checkout()
            .getOrderId();

        // API: Verify contract — response shape, field values, schema
        OrderResponse order = OrderApiClient.getOrder(authToken, orderId);
        assertThat(order.id()).matches("ORD-\\d{6}");
        assertThat(order.status()).isEqualTo("CONFIRMED");
        assertThat(order.total()).isEqualByComparingTo(BigDecimal.valueOf(1299.99));
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).sku()).isEqualTo("LAPTOP-001");
    }

    @Test
    void apiResponseTimeIsWithinSla() {
        // Direct API performance test (no UI)
        for (int i = 0; i < 5; i++) {
            given()
                .spec(ApiClient.authenticated(authToken))
            .when()
                .get("/v1/orders")
            .then()
                .statusCode(200)
                .time(Matchers.lessThan(1000L)); // 1s SLA for list
        }
    }

    @AfterEach
    void tearDown() {
        TestDataFactory.runCleanups();
        DriverFactory.quitDriver();
    }
}
```

### Key Design Decisions
- Shared `RequestSpecification` — DRY; all clients inherit base URI, content type, logging, HTTPS relaxation
- `ResponseSpecification` with response time — API SLA enforced in every test without explicit timing assertion
- JSON Schema in classpath — contract lives in `src/test/resources/schemas/`; versioned, reviewable, reusable
- `Awaitility` for async status polling — cleaner than `FluentWait` for pure API polling (no `WebDriver` needed)
- Typed response extraction — `extract().as(OrderResponse.class)` uses Jackson for deserialization; compile-time safety

---

## CQ15: Write a complete test for iframe interaction, nested frames, and cross-origin iframe handling

### Problem Statement
Implement utilities and tests for: switching to named and indexed iframes, interacting within frame context, handling nested iframes, returning to default content, and dealing with cross-origin iframes (where `getShadowRoot` and direct JS access are blocked).

### Solution

```java
public class IframeUtils {
    private final WebDriver driver;
    private final AppWait   wait;

    public IframeUtils(WebDriver driver) {
        this.driver = driver;
        this.wait   = new AppWait(driver);
    }

    // ── SWITCH BY LOCATOR ─────────────────────────────────────────────
    public IframeUtils switchTo(By frameLocator) {
        WebElement frame = wait.untilVisible(frameLocator);
        driver.switchTo().frame(frame);
        return this;
    }

    // ── SWITCH BY NAME OR ID ──────────────────────────────────────────
    public IframeUtils switchTo(String nameOrId) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
        return this;
    }

    // ── SWITCH BY INDEX ───────────────────────────────────────────────
    public IframeUtils switchTo(int index) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
        return this;
    }

    // ── RETURN TO MAIN PAGE ───────────────────────────────────────────
    public IframeUtils exitFrame() {
        driver.switchTo().defaultContent();
        return this;
    }

    // ── EXIT ONE LEVEL (parent frame) ─────────────────────────────────
    public IframeUtils exitToParentFrame() {
        driver.switchTo().parentFrame();
        return this;
    }

    // ── FIND ELEMENT IN FRAME ─────────────────────────────────────────
    public WebElement findInFrame(By frameLocator, By elementLocator) {
        switchTo(frameLocator);
        try {
            return wait.untilVisible(elementLocator);
        } catch (TimeoutException e) {
            driver.switchTo().defaultContent(); // always exit on failure
            throw new TimeoutException(
                "Element " + elementLocator + " not found in frame " + frameLocator, e);
        }
    }

    // ── PERFORM ACTION IN FRAME THEN EXIT ─────────────────────────────
    public <T> T withinFrame(By frameLocator, java.util.function.Function<WebDriver, T> action) {
        switchTo(frameLocator);
        try {
            return action.apply(driver);
        } finally {
            driver.switchTo().defaultContent(); // ALWAYS exit, even on exception
        }
    }

    // ── NESTED FRAME NAVIGATION ───────────────────────────────────────
    public IframeUtils switchToNested(By... frameLocators) {
        driver.switchTo().defaultContent(); // start from top
        for (By locator : frameLocators) {
            switchTo(locator);
        }
        return this;
    }

    // ── COUNT IFRAMES ON PAGE ─────────────────────────────────────────
    public int countFrames() {
        return driver.findElements(By.tagName("iframe")).size() +
               driver.findElements(By.tagName("frame")).size();
    }

    // ── CROSS-ORIGIN IFRAME — limited access ──────────────────────────
    // Cross-origin iframes block: JS execution, element interaction
    // What IS possible: verify iframe exists, verify src attribute, check dimensions
    public String getIframeSrc(By frameLocator) {
        // Must be in default context to read iframe attributes
        return driver.findElement(frameLocator).getDomAttribute("src");
    }

    public boolean iframeIsLoaded(By frameLocator) {
        // Switch, look for any expected element, switch back
        try {
            switchTo(frameLocator);
            // For same-origin: check document.readyState
            String readyState = (String) ((JavascriptExecutor) driver)
                .executeScript("return document.readyState;");
            return "complete".equals(readyState);
        } catch (WebDriverException e) {
            // Cross-origin: switching is allowed but JS may fail
            return false;
        } finally {
            driver.switchTo().defaultContent();
        }
    }
}

// USAGE IN TESTS
class IframeTest extends BaseTest {

    @Test
    void reCaptchaCheckboxInIframe() {
        navigateTo("/contact");
        IframeUtils iframes = new IframeUtils(driver());

        // reCAPTCHA is in an iframe with title="reCAPTCHA"
        iframes.switchTo(By.cssSelector("iframe[title='reCAPTCHA']"));

        // In test environments, use test keys that auto-solve
        By checkbox = By.cssSelector(".recaptcha-checkbox-border");
        wait().untilClickable(checkbox).click();

        // Verify checkmark appears
        By checkmark = By.cssSelector(".recaptcha-checkbox-checked");
        wait().untilVisible(checkmark);

        // Return to page and submit form
        iframes.exitFrame();
        driver().findElement(By.cssSelector("[data-testid='submit']")).click();
    }

    @Test
    void paymentFormInCrossOriginIframe() {
        navigateTo("/checkout");
        IframeUtils iframes = new IframeUtils(driver());

        // Stripe card element is cross-origin iframe
        By stripeIframe = By.cssSelector("iframe[name^='__privateStripeFrame']");

        // Verify iframe loaded (allowed even cross-origin)
        assertThat(iframes.iframeIsLoaded(stripeIframe)).isTrue();
        String src = iframes.getIframeSrc(stripeIframe);
        assertThat(src).contains("stripe.com");

        // Interact via keyboard (Selenium switches to frame, sendKeys works)
        iframes.withinFrame(stripeIframe, d -> {
            d.findElement(By.cssSelector("[name='cardnumber']"))
                .sendKeys("4242424242424242");
            d.findElement(By.cssSelector("[name='exp-date']"))
                .sendKeys("12/28");
            d.findElement(By.cssSelector("[name='cvc']"))
                .sendKeys("123");
            return null;
        });

        // Back in main page context
        driver().findElement(By.cssSelector("[data-testid='pay-button']")).click();
    }

    @Test
    void nestedFrameInteraction() {
        navigateTo("/legacy-portal");
        IframeUtils iframes = new IframeUtils(driver());

        // Navigate: outer-frame → middle-frame → inner content
        iframes.switchToNested(
            By.cssSelector("frame[name='outerFrame']"),
            By.cssSelector("frame[name='middleFrame']")
        );

        driver().findElement(By.cssSelector("[name='username']")).sendKeys("user");
        driver().findElement(By.cssSelector("[name='password']")).sendKeys("pass");
        driver().findElement(By.cssSelector("[type='submit']")).click();

        // Return to default to check result
        iframes.exitFrame();
        assertThat(driver().getTitle()).contains("Portal - Logged In");
    }

    @Test
    void withinFrameHelperEnsuresExit() {
        navigateTo("/embedded-editor");
        IframeUtils iframes = new IframeUtils(driver());

        // Even if action throws, we always exit frame
        String content = iframes.withinFrame(
            By.cssSelector("iframe#editor"),
            d -> d.findElement(By.cssSelector(".editor-content")).getText()
        );
        assertThat(content).isNotBlank();

        // Confirm we're back in main context
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='page-title']")).isDisplayed())
            .isTrue();
    }
}
```

### Key Design Decisions
- `withinFrame()` with `finally` — ALWAYS exits frame even if action throws; prevents all subsequent `findElement` calls from failing in wrong context
- `switchToNested()` starts with `defaultContent()` — idempotent; safe to call even if already at top level
- `frameToBeAvailableAndSwitchToIt` for name-based switch — handles timing; frame may not be in DOM yet
- Cross-origin documentation — explicitly notes what is/isn't possible; prevents engineers from spending hours on impossible JS injection
- Stripe pattern — real-world cross-origin iframe example; `sendKeys` into cross-origin frame works even when JS doesn't

---

## CQ16: Implement a multi-window and browser tab management utility

### Problem Statement
Write a `WindowManager` utility that handles opening new tabs, switching between windows by title or URL, closing specific windows, and managing popup windows triggered by UI actions — all thread-safe for parallel execution.

### Solution

```java
public class WindowManager {
    private final WebDriver driver;
    private final AppWait   wait;

    public WindowManager(WebDriver driver) {
        this.driver = driver;
        this.wait   = new AppWait(driver);
    }

    // ── OPEN NEW TAB (blank) ──────────────────────────────────────────
    public String openNewTab() {
        String current = driver.getWindowHandle();
        ((JavascriptExecutor) driver).executeScript("window.open('about:blank','_blank');");
        String newHandle = waitForNewWindow(current);
        driver.switchTo().window(newHandle);
        return newHandle;
    }

    // ── OPEN URL IN NEW TAB ───────────────────────────────────────────
    public String openUrlInNewTab(String url) {
        String handle = openNewTab();
        driver.get(url);
        return handle;
    }

    // ── SWITCH BY HANDLE ──────────────────────────────────────────────
    public WindowManager switchTo(String handle) {
        driver.switchTo().window(handle);
        return this;
    }

    // ── SWITCH BY TITLE (partial match) ──────────────────────────────
    public WindowManager switchToWindowWithTitle(String titleFragment) {
        String original = driver.getWindowHandle();
        boolean found = new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(d -> {
                for (String handle : d.getWindowHandles()) {
                    d.switchTo().window(handle);
                    if (d.getTitle().contains(titleFragment)) return true;
                }
                return false;
            });
        if (!found) {
            driver.switchTo().window(original);
            throw new NoSuchWindowException(
                "No window with title containing: " + titleFragment);
        }
        return this;
    }

    // ── SWITCH BY URL (partial match) ─────────────────────────────────
    public WindowManager switchToWindowWithUrl(String urlFragment) {
        String original = driver.getWindowHandle();
        boolean found = new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(d -> {
                for (String handle : d.getWindowHandles()) {
                    d.switchTo().window(handle);
                    if (d.getCurrentUrl().contains(urlFragment)) return true;
                }
                return false;
            });
        if (!found) {
            driver.switchTo().window(original);
            throw new NoSuchWindowException(
                "No window with URL containing: " + urlFragment);
        }
        return this;
    }

    // ── HANDLE POPUP TRIGGERED BY UI ACTION ──────────────────────────
    public String clickAndCapturePopup(By triggerLocator) {
        Set<String> before = driver.getWindowHandles();
        driver.findElement(triggerLocator).click();
        String popupHandle = waitForNewWindow(before);
        driver.switchTo().window(popupHandle);
        return popupHandle;
    }

    // ── CLOSE CURRENT WINDOW AND RETURN TO PREVIOUS ───────────────────
    public void closeCurrentAndReturn(String returnHandle) {
        driver.close();
        driver.switchTo().window(returnHandle);
    }

    // ── CLOSE ALL WINDOWS EXCEPT ONE ─────────────────────────────────
    public void closeAllExcept(String keepHandle) {
        for (String handle : new HashSet<>(driver.getWindowHandles())) {
            if (!handle.equals(keepHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(keepHandle);
    }

    // ── CLOSE ALL POPUP WINDOWS ───────────────────────────────────────
    public void closeAllPopups(String mainHandle) {
        closeAllExcept(mainHandle);
    }

    // ── GET WINDOW COUNT ─────────────────────────────────────────────
    public int getWindowCount() {
        return driver.getWindowHandles().size();
    }

    // ── WAIT FOR WINDOW COUNT TO REACH N ─────────────────────────────
    public void waitForWindowCount(int expectedCount) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
            .withMessage("Expected " + expectedCount + " windows, but count didn't stabilize")
            .until(ExpectedConditions.numberOfWindowsToBe(expectedCount));
    }

    // ── PRIVATE: WAIT FOR NEW WINDOW HANDLE ──────────────────────────
    private String waitForNewWindow(String existingHandle) {
        return waitForNewWindow(Set.of(existingHandle));
    }

    private String waitForNewWindow(Set<String> existingHandles) {
        return new WebDriverWait(driver, Duration.ofSeconds(15))
            .withMessage("New window did not appear within 15s")
            .until(d -> {
                Set<String> all = d.getWindowHandles();
                return all.stream()
                    .filter(h -> !existingHandles.contains(h))
                    .findFirst()
                    .orElse(null);
            });
    }
}

// USAGE IN TESTS
class WindowManagementTest extends BaseTest {

    @Test
    void termsLinkOpensInNewTab() {
        navigateTo("/register");
        String mainHandle = driver().getWindowHandle();
        WindowManager windows = new WindowManager(driver());

        // Terms link opens new tab
        String termsHandle = windows.clickAndCapturePopup(
            By.cssSelector("[data-testid='terms-link']"));

        // Verify correct page loaded in new tab
        new AppWait(driver()).untilDocumentReady();
        assertThat(driver().getTitle()).contains("Terms of Service");
        assertThat(driver().getCurrentUrl()).contains("/terms");

        // Close tab, return to registration
        windows.closeCurrentAndReturn(mainHandle);
        assertThat(driver().getCurrentUrl()).contains("/register");
    }

    @Test
    void shareButtonOpensPopupWindow() {
        navigateTo("/products/LAPTOP-001");
        WindowManager windows = new WindowManager(driver());
        String mainHandle = driver().getWindowHandle();

        windows.clickAndCapturePopup(By.cssSelector("[data-testid='share-btn']"));
        windows.waitForWindowCount(2);

        // Interact in popup
        new AppWait(driver()).untilVisible(
            By.cssSelector("[data-testid='share-url']"));
        String shareUrl = driver()
            .findElement(By.cssSelector("[data-testid='share-url']"))
            .getAttribute("value");
        assertThat(shareUrl).contains("/products/LAPTOP-001");

        windows.closeAllPopups(mainHandle);
        assertThat(windows.getWindowCount()).isEqualTo(1);
    }

    @Test
    void compareProductsInMultipleTabs() {
        WindowManager windows = new WindowManager(driver());
        String tab1 = driver().getWindowHandle();

        navigateTo("/products/LAPTOP-001");
        String price1 = driver()
            .findElement(By.cssSelector("[data-testid='price']")).getText();

        String tab2 = windows.openUrlInNewTab(BASE_URL + "/products/LAPTOP-002");
        String price2 = driver()
            .findElement(By.cssSelector("[data-testid='price']")).getText();

        // Compare side by side
        assertThat(Double.parseDouble(price1.replace("$", "")))
            .isNotEqualTo(Double.parseDouble(price2.replace("$", "")));

        windows.closeAllExcept(tab1);
    }
}
```

### Key Design Decisions
- `waitForNewWindow(Set<String>)` overload — passes snapshot of handles taken BEFORE the action; immune to race conditions where popup opens and closes instantly
- `clickAndCapturePopup` — atomically captures popup handle; avoids race between `click()` and iterating `getWindowHandles()`
- `closeAllExcept` copies handles into `HashSet` — avoids `ConcurrentModificationException` on `driver.getWindowHandles()` while iterating and closing
- `switchToWindowWithTitle` restores original handle on failure — caller's context is never corrupted by a failed switch

---

## CQ17: Build an alert and modal handler for browser dialogs and custom JS modals

### Problem Statement
Implement a `DialogHandler` that manages browser alerts/confirms/prompts (native), custom JavaScript-rendered modals, and Bootstrap modals — including waiting for them to appear, interacting, and verifying dismissal.

### Solution

```java
public class DialogHandler {
    private final WebDriver driver;
    private final AppWait   wait;
    private final JavascriptExecutor js;

    public DialogHandler(WebDriver driver) {
        this.driver = driver;
        this.wait   = new AppWait(driver);
        this.js     = (JavascriptExecutor) driver;
    }

    // ── NATIVE BROWSER ALERT ──────────────────────────────────────────
    public String acceptAlert() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.accept();
        return text;
    }

    public String dismissAlert() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.dismiss();
        return text;
    }

    public String getAlertText() {
        return waitForAlert().getText();
    }

    public void answerPrompt(String answer) {
        Alert alert = waitForAlert();
        alert.sendKeys(answer);
        alert.accept();
    }

    public boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    private Alert waitForAlert() {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Browser alert did not appear within 10s")
            .until(ExpectedConditions.alertIsPresent());
    }

    // ── INTERCEPT ALERTS VIA JS (auto-dismiss for background tests) ───
    // Overrides window.alert/confirm/prompt before navigation
    public void interceptAlerts(String confirmAnswer) {
        js.executeScript(
            "window.__alertMessages = [];" +
            "window.alert   = function(msg) { window.__alertMessages.push({type:'alert',   msg:msg}); };" +
            "window.confirm = function(msg) { window.__alertMessages.push({type:'confirm', msg:msg}); " +
            "                                 return " + confirmAnswer + "; };" +
            "window.prompt  = function(msg) { window.__alertMessages.push({type:'prompt',  msg:msg}); " +
            "                                 return null; };"
        );
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getInterceptedAlerts() {
        return (List<Map<String, String>>) js.executeScript(
            "return window.__alertMessages || [];");
    }

    // ── BOOTSTRAP / CUSTOM CSS MODAL ─────────────────────────────────
    public WebElement waitForModal(By modalLocator) {
        // Bootstrap modals animate in; wait for 'show' class + visible
        return new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Modal did not appear: " + modalLocator)
            .until(d -> {
                try {
                    WebElement modal = d.findElement(modalLocator);
                    String classes = modal.getDomAttribute("class");
                    boolean isBootstrapShown =
                        classes != null && classes.contains("show");
                    return (isBootstrapShown || modal.isDisplayed()) ? modal : null;
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    return null;
                }
            });
    }

    public void clickInModal(By modalLocator, By buttonLocator) {
        waitForModal(modalLocator);
        driver.findElement(modalLocator)
              .findElement(buttonLocator)
              .click();
    }

    public String getModalText(By modalLocator) {
        return waitForModal(modalLocator).getText();
    }

    public void confirmModal(By modalLocator, By confirmButtonLocator) {
        clickInModal(modalLocator, confirmButtonLocator);
        waitForModalToClose(modalLocator);
    }

    public void dismissModal(By modalLocator, By cancelButtonLocator) {
        clickInModal(modalLocator, cancelButtonLocator);
        waitForModalToClose(modalLocator);
    }

    // Dismiss via Escape key
    public void dismissModalWithEscape(By modalLocator) {
        waitForModal(modalLocator);
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        waitForModalToClose(modalLocator);
    }

    public void waitForModalToClose(By modalLocator) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Modal did not close: " + modalLocator)
            .until(d -> {
                try {
                    WebElement modal = d.findElement(modalLocator);
                    String classes = modal.getDomAttribute("class");
                    // Bootstrap: 'show' class removed when modal closes
                    if (classes != null && classes.contains("show")) return null;
                    return !modal.isDisplayed() ? true : null;
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    return true; // element gone = definitely closed
                }
            });
    }

    // ── TOAST / SNACKBAR NOTIFICATIONS ───────────────────────────────
    public String captureToastMessage(By toastLocator) {
        WebElement toast = wait.untilVisible(toastLocator);
        String message = toast.getText();
        // Wait for toast to disappear before returning
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.invisibilityOf(toast));
        return message;
    }
}

// USAGE IN TESTS
class DialogHandlerTest extends BaseTest {
    private DialogHandler dialogs;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        dialogs = new DialogHandler(driver());
    }

    @Test
    void deleteConfirmationAlertAcceptRemovesItem() {
        navigateTo("/cart");
        new CartPage(driver()).addItem("LAPTOP-001");

        // Click delete — triggers browser confirm dialog
        driver().findElement(By.cssSelector("[data-testid='delete-item']")).click();

        String alertText = dialogs.acceptAlert();
        assertThat(alertText).contains("Are you sure");

        // Item should be gone
        assertThat(driver().findElements(
            By.cssSelector("[data-testid='cart-item']"))).isEmpty();
    }

    @Test
    void deleteConfirmationDismissedKeepsItem() {
        navigateTo("/cart");
        new CartPage(driver()).addItem("LAPTOP-001");

        driver().findElement(By.cssSelector("[data-testid='delete-item']")).click();
        dialogs.dismissAlert();

        assertThat(driver().findElements(
            By.cssSelector("[data-testid='cart-item']"))).hasSize(1);
    }

    @Test
    void bootstrapDeleteModalConfirms() {
        navigateTo("/admin/products");
        By deleteModal  = By.cssSelector("#deleteProductModal");
        By confirmBtn   = By.cssSelector("[data-testid='confirm-delete']");

        driver().findElement(By.cssSelector("[data-testid='delete-LAPTOP-001']")).click();

        assertThat(dialogs.getModalText(deleteModal))
            .contains("This action cannot be undone");

        dialogs.confirmModal(deleteModal, confirmBtn);

        // Product removed from list
        assertThat(driver().findElements(
            By.cssSelector("[data-testid='product-LAPTOP-001']"))).isEmpty();
    }

    @Test
    void toastNotificationAppearsAfterSave() {
        navigateTo("/profile");
        driver().findElement(By.cssSelector("[data-testid='display-name']"))
            .clear();
        driver().findElement(By.cssSelector("[data-testid='display-name']"))
            .sendKeys("New Name");
        driver().findElement(By.cssSelector("[data-testid='save-profile']")).click();

        String toast = dialogs.captureToastMessage(
            By.cssSelector("[data-testid='toast-success']"));
        assertThat(toast).isEqualTo("Profile saved successfully");
    }

    @Test
    void backgroundAlertInterceptedByJs() {
        // Some pages fire confirm() during window.onbeforeunload
        dialogs.interceptAlerts("true"); // auto-accept all confirms
        navigateTo("/editor");
        driver().findElement(By.cssSelector("[data-testid='editor']"))
            .sendKeys("unsaved work");
        navigateTo("/dashboard"); // triggers beforeunload

        List<Map<String, String>> intercepted = dialogs.getInterceptedAlerts();
        assertThat(intercepted).anyMatch(a -> "confirm".equals(a.get("type")));
    }
}
```

### Key Design Decisions
- `waitForModal` checks Bootstrap `show` class AND `isDisplayed()` — Bootstrap adds `show` before transition completes; checking both covers animation race
- `waitForModalToClose` treats `NoSuchElementException` as closed — modal removed from DOM is unambiguously closed
- `interceptAlerts` overrides `window.alert/confirm/prompt` — prevents `UnhandledAlertException` in headless tests that do background navigation
- `captureToastMessage` waits for toast to disappear — caller gets text AND knows the toast lifecycle is complete

---

## CQ18: Write a browser storage utility for cookies, localStorage, and sessionStorage

### Problem Statement
Implement `BrowserStorageUtils` that manages cookies (add, delete, get all), injects JWT tokens into `localStorage` for fast test auth bypass, clears storage between tests, and snapshots/restores session state for complex test setups.

### Solution

```java
public class BrowserStorageUtils {
    private final WebDriver driver;
    private final JavascriptExecutor js;

    public BrowserStorageUtils(WebDriver driver) {
        this.driver = driver;
        this.js     = (JavascriptExecutor) driver;
    }

    // ── COOKIES ───────────────────────────────────────────────────────
    public void addCookie(String name, String value) {
        driver.manage().addCookie(new Cookie(name, value));
    }

    public void addCookie(Cookie cookie) {
        driver.manage().addCookie(cookie);
    }

    // Secure, HttpOnly, SameSite cookie for auth
    public void addAuthCookie(String name, String value, String domain) {
        Cookie cookie = new Cookie.Builder(name, value)
            .domain(domain)
            .path("/")
            .isSecure(true)
            .isHttpOnly(true)
            .build();
        driver.manage().addCookie(cookie);
    }

    public String getCookieValue(String name) {
        Cookie cookie = driver.manage().getCookieNamed(name);
        if (cookie == null) throw new NoSuchCookieException(name);
        return cookie.getValue();
    }

    public boolean hasCookie(String name) {
        return driver.manage().getCookieNamed(name) != null;
    }

    public void deleteCookie(String name) {
        driver.manage().deleteCookieNamed(name);
    }

    public void deleteAllCookies() {
        driver.manage().deleteAllCookies();
    }

    public Set<Cookie> getAllCookies() {
        return driver.manage().getCookies();
    }

    // Save full cookie jar, restore later
    public Set<Cookie> snapshotCookies() {
        return new HashSet<>(driver.manage().getCookies());
    }

    public void restoreCookies(Set<Cookie> snapshot) {
        driver.manage().deleteAllCookies();
        snapshot.forEach(driver.manage()::addCookie);
    }

    // ── LOCALSTORAGE ──────────────────────────────────────────────────
    public void setLocalStorage(String key, String value) {
        js.executeScript("localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    public String getLocalStorage(String key) {
        return (String) js.executeScript(
            "return localStorage.getItem(arguments[0]);", key);
    }

    public void removeLocalStorage(String key) {
        js.executeScript("localStorage.removeItem(arguments[0]);", key);
    }

    public void clearLocalStorage() {
        js.executeScript("localStorage.clear();");
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getAllLocalStorage() {
        return (Map<String, String>) js.executeScript(
            "var s = {};" +
            "for (var i = 0; i < localStorage.length; i++) {" +
            "  var k = localStorage.key(i);" +
            "  s[k] = localStorage.getItem(k);" +
            "}" +
            "return s;"
        );
    }

    // ── SESSIONSTORAGE ────────────────────────────────────────────────
    public void setSessionStorage(String key, String value) {
        js.executeScript("sessionStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    public String getSessionStorage(String key) {
        return (String) js.executeScript(
            "return sessionStorage.getItem(arguments[0]);", key);
    }

    public void clearSessionStorage() {
        js.executeScript("sessionStorage.clear();");
    }

    // ── AUTH BYPASS PATTERNS ──────────────────────────────────────────
    // JWT in localStorage (React/Vue SPA pattern)
    public void injectJwtToken(String token) {
        setLocalStorage("authToken", token);
        setLocalStorage("tokenType", "Bearer");
    }

    // Cookie-based auth (server-rendered apps)
    public void injectSessionCookie(String sessionId, String domain) {
        addAuthCookie("SESSION", sessionId, domain);
        driver.navigate().refresh(); // trigger cookie re-read
    }

    // Full auth state snapshot for complex apps
    public void injectAuthState(AuthState state) {
        setLocalStorage("authToken", state.accessToken());
        setLocalStorage("refreshToken", state.refreshToken());
        setLocalStorage("userId", state.userId());
        setLocalStorage("userRole", state.role());
        if (state.expiresAt() != null) {
            setLocalStorage("tokenExpiry", String.valueOf(state.expiresAt()));
        }
    }

    // ── CLEAR ALL ─────────────────────────────────────────────────────
    public void clearAll() {
        clearLocalStorage();
        clearSessionStorage();
        deleteAllCookies();
    }

    // ── SNAPSHOT / RESTORE FULL STORAGE STATE ─────────────────────────
    public StorageSnapshot snapshot() {
        return new StorageSnapshot(
            snapshotCookies(),
            getAllLocalStorage(),
            (Map<String, String>) js.executeScript(
                "var s = {};" +
                "for (var i = 0; i < sessionStorage.length; i++) {" +
                "  var k = sessionStorage.key(i);" +
                "  s[k] = sessionStorage.getItem(k);" +
                "}" +
                "return s;")
        );
    }

    public void restore(StorageSnapshot snap) {
        restoreCookies(snap.cookies());
        clearLocalStorage();
        snap.localStorage().forEach(this::setLocalStorage);
        clearSessionStorage();
        snap.sessionStorage().forEach(this::setSessionStorage);
    }

    public record StorageSnapshot(
        Set<Cookie> cookies,
        Map<String, String> localStorage,
        Map<String, String> sessionStorage) {}

    public record AuthState(
        String accessToken, String refreshToken,
        String userId, String role, Long expiresAt) {}
}

// USAGE IN TESTS
class BrowserStorageTest extends BaseTest {

    @Test
    void loginBypassViaJwtInjectsAuthenticated() {
        // Create test user via API, get token
        TestUser user     = UserApiFactory.createUser("CUSTOMER");
        String authToken  = UserApiFactory.getAuthToken(user);

        // Navigate to base URL (must be on same domain before setting storage)
        driver().get(BASE_URL);
        BrowserStorageUtils storage = new BrowserStorageUtils(driver());
        storage.injectJwtToken(authToken);

        // Navigate directly to protected page — no login form interaction
        driver().get(BASE_URL + "/dashboard");
        new AppWait(driver()).untilDocumentReady();

        // Should be authenticated — no redirect to login
        assertThat(driver().getCurrentUrl()).doesNotContain("/login");
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='user-menu']")).isDisplayed()).isTrue();
    }

    @Test
    void storageIsClearedBetweenTests() {
        BrowserStorageUtils storage = new BrowserStorageUtils(driver());
        storage.setLocalStorage("testKey", "testValue");
        assertThat(storage.getLocalStorage("testKey")).isEqualTo("testValue");

        storage.clearAll();

        assertThat(storage.getLocalStorage("testKey")).isNull();
        assertThat(storage.hasCookie("SESSION")).isFalse();
    }

    @Test
    void cartStateRestoredFromSnapshot() {
        BrowserStorageUtils storage = new BrowserStorageUtils(driver());
        driver().get(BASE_URL);
        storage.injectJwtToken(UserApiFactory.getAuthToken(
            UserApiFactory.createUser("CUSTOMER")));

        // Add items to cart (state stored in localStorage)
        navigateTo("/products/LAPTOP-001");
        new ProductPage(driver()).addToCart();

        StorageSnapshot snap = storage.snapshot();

        // Simulate new browser — clear and restore
        storage.clearAll();
        driver().navigate().refresh();
        assertThat(driver().findElements(
            By.cssSelector("[data-testid='cart-badge']"))).isEmpty();

        storage.restore(snap);
        driver().navigate().refresh();
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='cart-badge']")).getText()).isEqualTo("1");
    }
}
```

### Key Design Decisions
- Must navigate to domain before setting cookies/localStorage — browser enforces same-origin; `driver.get(BASE_URL)` before any storage injection is mandatory
- `snapshot()` / `restore()` — enables pre-built test state re-use across multiple tests without repeating UI login flows
- `injectAuthState` covers full token payload — some SPAs read `refreshToken`, `expiresAt`, and `role` from storage; setting only `authToken` causes redirect loops
- `Cookie.Builder` for auth cookies — sets `isHttpOnly` and `isSecure` matching production cookie flags; mismatches cause auth failures on HTTPS staging

---

## CQ19: Implement a visual regression testing utility using AShot or pixel comparison

### Problem Statement
Build a `VisualTestHelper` that captures page/element screenshots, compares them against stored baselines using pixel difference analysis, generates diff images, and handles dynamic content exclusion regions — with configurable tolerance thresholds.

### Solution

```java
public class VisualTestHelper {
    private static final Path BASELINE_DIR = Paths.get("src/test/resources/visual-baselines");
    private static final Path ACTUAL_DIR   = Paths.get("target/visual-actual");
    private static final Path DIFF_DIR     = Paths.get("target/visual-diff");

    private final WebDriver driver;
    private final TakesScreenshot screenshotter;

    // Default tolerance: 0% pixel difference allowed
    private double tolerancePercent = 0.0;
    private final List<Rectangle> exclusionZones = new ArrayList<>();

    public VisualTestHelper(WebDriver driver) {
        this.driver        = driver;
        this.screenshotter = (TakesScreenshot) driver;
        createDirectories();
    }

    public VisualTestHelper withTolerance(double percentDifference) {
        this.tolerancePercent = percentDifference;
        return this;
    }

    // Exclude dynamic regions (timestamps, ads, user avatars)
    public VisualTestHelper exclude(By locator) {
        try {
            WebElement el = driver.findElement(locator);
            exclusionZones.add(new Rectangle(
                el.getLocation().x, el.getLocation().y,
                el.getSize().height, el.getSize().width));
        } catch (NoSuchElementException e) {
            log.warn("Exclusion zone element not found: {}", locator);
        }
        return this;
    }

    public VisualTestHelper exclude(int x, int y, int width, int height) {
        exclusionZones.add(new Rectangle(x, y, height, width));
        return this;
    }

    // ── CAPTURE FULL PAGE SCREENSHOT ──────────────────────────────────
    public BufferedImage captureFullPage() throws IOException {
        byte[] bytes = screenshotter.getScreenshotAs(OutputType.BYTES);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    // ── CAPTURE ELEMENT SCREENSHOT ────────────────────────────────────
    public BufferedImage captureElement(By locator) throws IOException {
        WebElement el    = new AppWait(driver).untilVisible(locator);
        byte[] bytes     = ((TakesScreenshot) el).getScreenshotAs(OutputType.BYTES);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    // ── ASSERT MATCHES BASELINE ───────────────────────────────────────
    public void assertMatchesBaseline(String baselineName) {
        assertMatchesBaseline(baselineName, (By) null);
    }

    public void assertMatchesBaseline(String baselineName, By elementLocator) {
        try {
            BufferedImage actual = (elementLocator != null)
                ? captureElement(elementLocator)
                : captureFullPage();

            applyExclusionZones(actual);

            Path baselinePath = BASELINE_DIR.resolve(baselineName + ".png");

            if (!Files.exists(baselinePath)) {
                // First run: save as baseline
                saveImage(actual, baselinePath);
                log.info("[VISUAL] Baseline created: {}", baselineName);
                Allure.addAttachment("Visual Baseline Created — " + baselineName,
                    "image/png", toInputStream(actual), ".png");
                return; // First run always passes
            }

            BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            VisualDiff diff        = compareImages(baseline, actual);

            // Save actual screenshot always
            Path actualPath = ACTUAL_DIR.resolve(baselineName + ".png");
            saveImage(actual, actualPath);

            if (diff.differencePercent() > tolerancePercent) {
                // Save diff image
                Path diffPath = DIFF_DIR.resolve(baselineName + "-diff.png");
                saveImage(diff.diffImage(), diffPath);

                // Attach all three to Allure
                Allure.addAttachment("Baseline: " + baselineName,
                    "image/png", Files.newInputStream(baselinePath), ".png");
                Allure.addAttachment("Actual: " + baselineName,
                    "image/png", toInputStream(actual), ".png");
                Allure.addAttachment("Diff: " + baselineName,
                    "image/png", toInputStream(diff.diffImage()), ".png");

                fail(String.format(
                    "Visual regression detected for '%s': %.2f%% pixels differ (tolerance: %.2f%%)\n" +
                    "Diff image: %s",
                    baselineName, diff.differencePercent(), tolerancePercent, diffPath));
            }
        } catch (IOException e) {
            throw new RuntimeException("Visual comparison failed for: " + baselineName, e);
        }
    }

    // ── PIXEL COMPARISON ──────────────────────────────────────────────
    private VisualDiff compareImages(BufferedImage baseline, BufferedImage actual) {
        // Resize actual to baseline dimensions if they differ (viewport size changes)
        if (baseline.getWidth() != actual.getWidth() ||
                baseline.getHeight() != actual.getHeight()) {
            actual = resize(actual, baseline.getWidth(), baseline.getHeight());
        }

        int width       = baseline.getWidth();
        int height      = baseline.getHeight();
        int totalPixels = width * height;
        int diffPixels  = 0;

        BufferedImage diffImage = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int b = baseline.getRGB(x, y);
                int a = actual.getRGB(x, y);
                if (b != a) {
                    diffPixels++;
                    diffImage.setRGB(x, y, 0xFF0000); // red for diff pixels
                } else {
                    // Dimmed original for context
                    int dimmed = ((b & 0xFEFEFE) >> 1) | 0x808080;
                    diffImage.setRGB(x, y, dimmed);
                }
            }
        }

        double percent = (diffPixels * 100.0) / totalPixels;
        return new VisualDiff(diffImage, percent, diffPixels, totalPixels);
    }

    private void applyExclusionZones(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GRAY);
        for (Rectangle zone : exclusionZones) {
            g.fillRect(zone.x, zone.y, zone.width, zone.height);
        }
        g.dispose();
    }

    private BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, src.getType());
        out.createGraphics().drawImage(src.getScaledInstance(w, h,
            java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return out;
    }

    private void saveImage(BufferedImage img, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ImageIO.write(img, "PNG", path.toFile());
    }

    private InputStream toInputStream(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private void createDirectories() {
        try {
            Files.createDirectories(BASELINE_DIR);
            Files.createDirectories(ACTUAL_DIR);
            Files.createDirectories(DIFF_DIR);
        } catch (IOException e) {
            log.warn("Could not create visual testing directories", e);
        }
    }

    // Update baseline (run with flag: -DupdateVisualBaselines=true)
    public void updateBaseline(String baselineName) throws IOException {
        BufferedImage actual = captureFullPage();
        applyExclusionZones(actual);
        saveImage(actual, BASELINE_DIR.resolve(baselineName + ".png"));
        log.info("[VISUAL] Baseline updated: {}", baselineName);
    }

    record VisualDiff(BufferedImage diffImage, double differencePercent,
                      int diffPixels, int totalPixels) {}
}

// USAGE IN TESTS
@Tag("visual")
class VisualRegressionTest extends BaseTest {

    @Test
    void checkoutPageMatchesBaseline() {
        loginAs("CUSTOMER");
        navigateTo("/checkout");
        new AppWait(driver()).untilDocumentReady();

        new VisualTestHelper(driver())
            .withTolerance(0.5)                                     // 0.5% tolerance
            .exclude(By.cssSelector("[data-testid='order-date']"))  // dynamic: today's date
            .exclude(By.cssSelector("[data-testid='user-avatar']")) // dynamic: user-specific
            .assertMatchesBaseline("checkout-page");
    }

    @Test
    void productCardComponentMatchesBaseline() {
        navigateTo("/products");
        new VisualTestHelper(driver())
            .withTolerance(0.2)
            .exclude(By.cssSelector("[data-testid='stock-count']")) // live inventory
            .assertMatchesBaseline("product-card",
                By.cssSelector("[data-testid='product-LAPTOP-001']"));
    }
}
```

### Key Design Decisions
- First run creates baseline instead of failing — enables baseline generation via CI without a separate "update" build step
- `applyExclusionZones` paints gray over dynamic regions before comparison — dynamic content (timestamps, prices) masked identically in both baseline and actual
- Red diff pixels with grayed background — immediately shows WHERE regression occurred (red spots) with surrounding context (grayed original)
- Tolerance percent — small rendering differences across OS/GPU are expected; 0.5% covers anti-aliasing differences without hiding real regressions
- `-DupdateVisualBaselines` pattern — documented update mechanism; engineers don't commit wrong baselines accidentally

---

## CQ20: Write a self-healing locator strategy that falls back through multiple selector types

### Problem Statement
Implement a `SelfHealingElement` wrapper that tries a primary locator, then falls back through a priority list of alternative selectors, logs which locator was used, updates a registry when primary fails, and can be trained from test failures.

### Solution

```java
public class SelfHealingLocator {
    private final WebDriver          driver;
    private final AppWait            wait;
    private final LocatorRegistry    registry;

    public SelfHealingLocator(WebDriver driver) {
        this.driver   = driver;
        this.wait     = new AppWait(driver);
        this.registry = LocatorRegistry.getInstance();
    }

    // PRIMARY FIND — tries primary, then fallbacks
    public WebElement find(String elementId, By primary, By... fallbacks) {
        // Check if registry has a healed locator for this element
        By healed = registry.getHealed(elementId);
        if (healed != null) {
            WebElement el = tryLocator(healed, Duration.ofSeconds(5));
            if (el != null) {
                log.debug("[SELF-HEAL] Using healed locator for '{}': {}", elementId, healed);
                return el;
            } else {
                registry.clearHealed(elementId); // healed locator also broken — clear it
            }
        }

        // Try primary
        WebElement el = tryLocator(primary, Duration.ofSeconds(10));
        if (el != null) return el;

        log.warn("[SELF-HEAL] Primary locator failed for '{}': {}", elementId, primary);

        // Try fallbacks in order
        for (By fallback : fallbacks) {
            el = tryLocator(fallback, Duration.ofSeconds(5));
            if (el != null) {
                log.warn("[SELF-HEAL] Healed '{}' using fallback: {}", elementId, fallback);
                registry.recordHealed(elementId, fallback); // save for next run
                Allure.addAttachment("Self-Heal Event",
                    String.format("Element '%s' healed\nPrimary: %s\nUsed: %s",
                        elementId, primary, fallback));
                return el;
            }
        }

        // All failed — generate attributes-based fallback as last resort
        el = tryAttributeBasedSearch(primary);
        if (el != null) {
            log.warn("[SELF-HEAL] Last-resort attribute heal succeeded for '{}'", elementId);
            return el;
        }

        throw new NoSuchElementException(
            "Self-healing exhausted all locators for '" + elementId + "'. " +
            "Primary: " + primary + ". Fallbacks: " + Arrays.toString(fallbacks));
    }

    // CLICK — self-healing wrapper
    public void click(String elementId, By primary, By... fallbacks) {
        find(elementId, primary, fallbacks).click();
    }

    // TYPE — self-healing wrapper
    public void type(String elementId, String text, By primary, By... fallbacks) {
        WebElement el = find(elementId, primary, fallbacks);
        el.clear();
        el.sendKeys(text);
    }

    // ── ATTRIBUTE-BASED SEARCH — last resort ──────────────────────────
    // Extracts attributes from the primary By and searches by them
    private WebElement tryAttributeBasedSearch(By primary) {
        String locatorString = primary.toString();
        // Extract CSS class from ID locator
        if (locatorString.contains("data-testid=")) {
            String testid = locatorString.replaceAll(".*data-testid=['\"]?([^'\"\\]]+).*", "$1");
            if (!testid.equals(locatorString)) {
                // Try XPath text content match as very last resort
                return tryLocator(
                    By.xpath("//*[@data-testid='" + testid + "']"),
                    Duration.ofSeconds(3));
            }
        }
        return null;
    }

    private WebElement tryLocator(By locator, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout)
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            return null;
        }
    }
}

// LOCATOR REGISTRY — persists healed locators to file between runs
public class LocatorRegistry {
    private static LocatorRegistry instance;
    private final Path registryPath = Paths.get("target/healed-locators.json");
    private final Map<String, String> healedLocators = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private LocatorRegistry() { loadFromDisk(); }

    public static synchronized LocatorRegistry getInstance() {
        if (instance == null) instance = new LocatorRegistry();
        return instance;
    }

    public void recordHealed(String elementId, By locator) {
        healedLocators.put(elementId, locator.toString());
        saveToDisk();
    }

    public By getHealed(String elementId) {
        String serialized = healedLocators.get(elementId);
        if (serialized == null) return null;
        return deserializeLocator(serialized);
    }

    public void clearHealed(String elementId) {
        healedLocators.remove(elementId);
        saveToDisk();
    }

    public Map<String, String> getHealingReport() {
        return Collections.unmodifiableMap(healedLocators);
    }

    // Generates a healing summary for CI notification
    public String getSummary() {
        if (healedLocators.isEmpty()) return "No self-healing events this run.";
        StringBuilder sb = new StringBuilder("SELF-HEALING EVENTS:\n");
        healedLocators.forEach((id, loc) ->
            sb.append("  - ").append(id).append(" → ").append(loc).append("\n"));
        return sb.toString();
    }

    private void loadFromDisk() {
        if (!Files.exists(registryPath)) return;
        try {
            Map<String, String> saved = mapper.readValue(registryPath.toFile(),
                new TypeReference<>() {});
            healedLocators.putAll(saved);
            log.info("[SELF-HEAL] Loaded {} healed locators from registry", saved.size());
        } catch (IOException e) {
            log.warn("Could not load healed locator registry: {}", e.getMessage());
        }
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(registryPath.getParent());
            mapper.writeValue(registryPath.toFile(), healedLocators);
        } catch (IOException e) {
            log.warn("Could not save healed locator registry: {}", e.getMessage());
        }
    }

    private By deserializeLocator(String serialized) {
        // Selenium's By.toString() format: "By.cssSelector: .foo"
        if (serialized.startsWith("By.cssSelector: "))
            return By.cssSelector(serialized.substring("By.cssSelector: ".length()));
        if (serialized.startsWith("By.id: "))
            return By.id(serialized.substring("By.id: ".length()));
        if (serialized.startsWith("By.xpath: "))
            return By.xpath(serialized.substring("By.xpath: ".length()));
        if (serialized.startsWith("By.name: "))
            return By.name(serialized.substring("By.name: ".length()));
        return null;
    }
}

// USAGE IN PAGE OBJECTS
public class LoginPage {
    private final SelfHealingLocator heal;

    // Define element with primary + ordered fallbacks
    private static final String EMAIL_FIELD = "login-email";
    private static final By EMAIL_PRIMARY   = By.cssSelector("[data-testid='email-input']");
    private static final By EMAIL_FALLBACK1 = By.id("email");
    private static final By EMAIL_FALLBACK2 = By.name("email");
    private static final By EMAIL_FALLBACK3 = By.xpath("//input[@type='email']");

    private static final String PASS_FIELD  = "login-password";
    private static final By PASS_PRIMARY    = By.cssSelector("[data-testid='password-input']");
    private static final By PASS_FALLBACK1  = By.id("password");
    private static final By PASS_FALLBACK2  = By.name("password");

    private static final String SUBMIT_BTN  = "login-submit";
    private static final By SUBMIT_PRIMARY  = By.cssSelector("[data-testid='login-submit']");
    private static final By SUBMIT_FALLBACK1= By.cssSelector("button[type='submit']");
    private static final By SUBMIT_FALLBACK2= By.xpath("//button[contains(text(),'Log in')]");

    public LoginPage(WebDriver driver) {
        this.heal = new SelfHealingLocator(driver);
    }

    public DashboardPage loginAs(String email, String password) {
        heal.type(EMAIL_FIELD,  email,    EMAIL_PRIMARY,  EMAIL_FALLBACK1,  EMAIL_FALLBACK2,  EMAIL_FALLBACK3);
        heal.type(PASS_FIELD,   password, PASS_PRIMARY,   PASS_FALLBACK1,   PASS_FALLBACK2);
        heal.click(SUBMIT_BTN,            SUBMIT_PRIMARY, SUBMIT_FALLBACK1, SUBMIT_FALLBACK2);
        return new DashboardPage(heal.getDriver());
    }
}
```

### Key Design Decisions
- Healed locator checked first on subsequent runs — avoids re-failure with known-broken primary; runs faster
- Locator registry persisted to `target/` — not committed to VCS; resets when `mvn clean` runs; prevents stale heals accumulating
- Last-resort `data-testid` XPath extraction — catches cases where CSS selector syntax changed but attribute value didn't
- `getSummary()` for CI notifications — team is alerted to healing events without reading logs; motivates fixing broken locators before they become permanent

---

## CQ21: Create a complete test data factory with API creation and automatic cleanup

### Problem Statement
Implement a `TestDataFactory` that creates test entities via REST API (not UI), registers them for automatic cleanup after each test, supports complex dependency graphs (user → order → invoice), and provides thread-safe isolation for parallel tests.

### Solution

```java
public class TestDataFactory {
    // ThreadLocal cleanup stack — each parallel test thread has its own
    private static final ThreadLocal<Deque<Runnable>> cleanupStack =
        ThreadLocal.withInitial(ArrayDeque::new);

    private final String authToken;

    public TestDataFactory(String adminToken) {
        this.authToken = adminToken;
    }

    // ── USER FACTORY ──────────────────────────────────────────────────
    public TestUser createUser(UserRole role) {
        return createUser(role, UserBuilder.defaults(role));
    }

    public TestUser createUser(UserRole role, UserBuilder builder) {
        Map<String, Object> payload = builder.build();
        TestUser user = given()
            .spec(ApiClient.authenticated(authToken))
            .body(payload)
        .when()
            .post("/v1/admin/users")
        .then()
            .statusCode(201)
            .extract()
            .as(TestUser.class);

        // Register cleanup
        register(() -> deleteUser(user.id()));
        log.debug("[TestData] Created user: {} ({})", user.email(), role);
        return user;
    }

    public String getAuthToken(TestUser user) {
        return given()
            .body(Map.of("email", user.email(), "password", user.password()))
            .contentType(ContentType.JSON)
        .when()
            .post(ConfigReader.get("apiBaseUrl") + "/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("accessToken");
    }

    // ── PRODUCT FACTORY ───────────────────────────────────────────────
    public TestProduct createProduct(String sku, BigDecimal price) {
        TestProduct product = given()
            .spec(ApiClient.authenticated(authToken))
            .body(Map.of(
                "sku",      sku,
                "name",     "Test Product " + sku,
                "price",    price,
                "stock",    100,
                "category", "TEST"
            ))
        .when()
            .post("/v1/admin/products")
        .then()
            .statusCode(201)
            .extract()
            .as(TestProduct.class);

        register(() -> deleteProduct(product.id()));
        return product;
    }

    // ── ORDER FACTORY ─────────────────────────────────────────────────
    public TestOrder createOrder(TestUser user, TestProduct... products) {
        String userToken = getAuthToken(user);
        List<Map<String, Object>> items = Arrays.stream(products)
            .map(p -> Map.<String, Object>of("sku", p.sku(), "quantity", 1))
            .toList();

        TestOrder order = given()
            .spec(ApiClient.authenticated(userToken))
            .body(Map.of("items", items))
        .when()
            .post("/v1/orders")
        .then()
            .statusCode(201)
            .extract()
            .as(TestOrder.class);

        register(() -> cancelOrder(order.id(), adminToken()));
        return order;
    }

    // ── COMPLEX GRAPH: User + Product + Order ─────────────────────────
    public OrderScenario createOrderScenario() {
        TestUser    customer = createUser(UserRole.CUSTOMER);
        TestProduct product  = createProduct("TEST-SKU-" + randomSuffix(), BigDecimal.TEN);
        TestOrder   order    = createOrder(customer, product);
        return new OrderScenario(customer, product, order);
    }

    public record OrderScenario(TestUser customer, TestProduct product, TestOrder order) {}

    // ── CLEANUP REGISTRATION ──────────────────────────────────────────
    public static void register(Runnable cleanup) {
        cleanupStack.get().push(cleanup); // LIFO: last created → first deleted
    }

    // Call in @AfterEach — runs all cleanups in reverse order
    public static void runCleanups() {
        Deque<Runnable> stack = cleanupStack.get();
        List<Throwable> failures = new ArrayList<>();
        while (!stack.isEmpty()) {
            Runnable cleanup = stack.pop();
            try {
                cleanup.run();
            } catch (Exception e) {
                failures.add(e); // collect, don't stop
                log.error("[TestData] Cleanup failed: {}", e.getMessage());
            }
        }
        cleanupStack.remove(); // prevent memory leak in thread pool

        if (!failures.isEmpty()) {
            log.warn("[TestData] {} cleanup(s) failed — test data may remain", failures.size());
        }
    }

    // ── DELETE OPERATIONS ─────────────────────────────────────────────
    private void deleteUser(String userId) {
        given().spec(ApiClient.authenticated(authToken))
            .when().delete("/v1/admin/users/{id}", userId)
            .then().statusCode(anyOf(is(200), is(204), is(404)));
    }

    private void deleteProduct(String productId) {
        given().spec(ApiClient.authenticated(authToken))
            .when().delete("/v1/admin/products/{id}", productId)
            .then().statusCode(anyOf(is(200), is(204), is(404)));
    }

    private void cancelOrder(String orderId, String token) {
        given().spec(ApiClient.authenticated(token))
            .body(Map.of("reason", "test-cleanup"))
            .when().post("/v1/orders/{id}/cancel", orderId)
            .then().statusCode(anyOf(is(200), is(404), is(422)));
    }

    private String adminToken() {
        return AuthTokenCache.getOrCreate("ADMIN");
    }

    private String randomSuffix() {
        return String.valueOf(System.nanoTime()).substring(8);
    }
}

// USER BUILDER
public class UserBuilder {
    private String email;
    private String password    = "TestPass123!";
    private String firstName   = "Test";
    private String lastName    = "User";
    private String role;
    private boolean verified   = true;
    private boolean active     = true;

    public static UserBuilder defaults(UserRole role) {
        return new UserBuilder()
            .role(role.name())
            .email("test+" + UUID.randomUUID().toString().substring(0, 8)
                + "@example.com");
    }

    public UserBuilder email(String email)       { this.email = email; return this; }
    public UserBuilder password(String pass)     { this.password = pass; return this; }
    public UserBuilder firstName(String name)    { this.firstName = name; return this; }
    public UserBuilder lastName(String name)     { this.lastName = name; return this; }
    public UserBuilder role(String role)         { this.role = role; return this; }
    public UserBuilder unverified()              { this.verified = false; return this; }
    public UserBuilder inactive()                { this.active = false; return this; }

    public Map<String, Object> build() {
        return Map.of(
            "email",     email,
            "password",  password,
            "firstName", firstName,
            "lastName",  lastName,
            "role",      role,
            "verified",  verified,
            "active",    active
        );
    }
}

// AUTH TOKEN CACHE — reuse admin token across tests
public class AuthTokenCache {
    private static final Map<String, String>  tokens     = new ConcurrentHashMap<>();
    private static final Map<String, Instant> expiries   = new ConcurrentHashMap<>();

    public static String getOrCreate(String role) {
        String cached = tokens.get(role);
        Instant expiry = expiries.get(role);
        if (cached != null && expiry != null && Instant.now().isBefore(expiry)) {
            return cached;
        }
        String token = createToken(role);
        tokens.put(role, token);
        expiries.put(role, Instant.now().plus(Duration.ofMinutes(50)));
        return token;
    }

    private static String createToken(String role) {
        String email    = ConfigReader.get("test." + role.toLowerCase() + ".email");
        String password = ConfigReader.get("test." + role.toLowerCase() + ".password");
        return given()
            .body(Map.of("email", email, "password", password))
            .contentType(ContentType.JSON)
        .when()
            .post(ConfigReader.get("apiBaseUrl") + "/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("accessToken");
    }
}

// USAGE IN TESTS
class OrderWorkflowTest extends BaseTest {
    private TestDataFactory factory;
    private TestUser customer;
    private String authToken;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        factory   = new TestDataFactory(AuthTokenCache.getOrCreate("ADMIN"));
        customer  = factory.createUser(UserRole.CUSTOMER);
        authToken = factory.getAuthToken(customer);
    }

    @Test
    void customerCanViewOrderHistory() {
        TestProduct product = factory.createProduct("LAPTOP-001", BigDecimal.valueOf(1299.99));
        TestOrder order     = factory.createOrder(customer, product);

        driver().get(BASE_URL);
        new BrowserStorageUtils(driver()).injectJwtToken(authToken);
        navigateTo("/orders/" + order.id());

        assertThat(driver().findElement(
            By.cssSelector("[data-testid='order-id']")).getText())
            .isEqualTo(order.id());
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='order-status']")).getText())
            .isEqualTo("CONFIRMED");
    }

    @AfterEach
    void tearDown() {
        TestDataFactory.runCleanups();
        DriverFactory.quitDriver();
    }
}
```

### Key Design Decisions
- `ThreadLocal<Deque<Runnable>>` — each parallel thread has its own cleanup stack; prevents test A cleaning up test B's data
- LIFO cleanup order — last created entity is deleted first; respects foreign key constraints (delete order before user, not after)
- Cleanup accepts 404 — entity may already be gone (test itself deleted it); `anyOf(200, 204, 404)` prevents cleanup failures from masking test results
- Collect-then-report pattern — all cleanups run even if one fails; failure logged but doesn't throw (test result is already recorded)
- `AuthTokenCache` with expiry — admin token reused for 50 minutes; avoids one auth call per test in 200-test suites

---

## CQ22: Implement a configuration reader supporting multiple environments and property override chains

### Problem Statement
Build a `ConfigReader` that loads environment-specific properties (`staging.properties`, `prod.properties`), supports system property overrides, environment variable overrides, encrypts sensitive values with Jasypt, and provides typed getters with defaults.

### Solution

```java
public class ConfigReader {
    private static ConfigReader instance;
    private final Properties properties = new Properties();

    // Override chain (highest to lowest priority):
    // 1. System properties (-Dkey=value)
    // 2. Environment variables (CI_BROWSER=chrome)
    // 3. Environment-specific file (staging.properties)
    // 4. Base file (base.properties)
    private ConfigReader() {
        loadBase();
        loadEnvironmentSpecific();
        // System properties and env vars resolved at get-time (not loaded here)
    }

    public static synchronized ConfigReader getInstance() {
        if (instance == null) instance = new ConfigReader();
        return instance;
    }

    // Static convenience methods
    public static String get(String key) {
        return getInstance().resolve(key, null);
    }

    public static String get(String key, String defaultValue) {
        return getInstance().resolve(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        return val != null ? Integer.parseInt(val.trim()) : defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String val = get(key);
        return val != null ? Long.parseLong(val.trim()) : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        return val != null ? Boolean.parseBoolean(val.trim()) : defaultValue;
    }

    public static Duration getDuration(String key, Duration defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        // Format: "10s", "500ms", "2m"
        if (val.endsWith("ms")) return Duration.ofMillis(Long.parseLong(val.replace("ms", "")));
        if (val.endsWith("s"))  return Duration.ofSeconds(Long.parseLong(val.replace("s", "")));
        if (val.endsWith("m"))  return Duration.ofMinutes(Long.parseLong(val.replace("m", "")));
        return Duration.ofSeconds(Long.parseLong(val));
    }

    public static List<String> getList(String key) {
        String val = get(key);
        if (val == null || val.isBlank()) return List.of();
        return Arrays.stream(val.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    // ── RESOLVE with priority chain ──────────────────────────────────
    private String resolve(String key, String defaultValue) {
        // 1. System property (highest priority)
        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;

        // 2. Environment variable (key with dots → underscores, uppercase)
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envVar = System.getenv(envKey);
        if (envVar != null) return envVar;

        // 3. Loaded properties (env-specific overrides base)
        String prop = properties.getProperty(key);
        if (prop != null) return decrypt(prop);

        // 4. Default
        return defaultValue;
    }

    // ── LOAD PROPERTIES ───────────────────────────────────────────────
    private void loadBase() {
        loadFromClasspath("config/base.properties");
    }

    private void loadEnvironmentSpecific() {
        String env = System.getProperty("env",
            System.getenv("TEST_ENV") != null ? System.getenv("TEST_ENV") : "staging");
        loadFromClasspath("config/" + env + ".properties");
    }

    private void loadFromClasspath(String path) {
        try (InputStream is =
                ConfigReader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.debug("Config file not found (non-fatal): {}", path);
                return;
            }
            Properties p = new Properties();
            p.load(is);
            // Later files override earlier (env-specific overrides base)
            properties.putAll(p);
            log.debug("Loaded config: {} ({} properties)", path, p.size());
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load config: " + path, e);
        }
    }

    // ── SIMPLE ENCRYPTION (Base64 obfuscation for non-prod secrets) ───
    // For production: use Jasypt or AWS Secrets Manager
    private String decrypt(String value) {
        if (value != null && value.startsWith("ENC(") && value.endsWith(")")) {
            String encoded = value.substring(4, value.length() - 1);
            return new String(Base64.getDecoder().decode(encoded),
                StandardCharsets.UTF_8);
        }
        return value;
    }

    // ── REQUIRE (fail fast if missing) ───────────────────────────────
    public static String require(String key) {
        String val = get(key);
        if (val == null || val.isBlank())
            throw new ConfigurationException(
                "Required config key '" + key + "' is missing. " +
                "Set via: -D" + key + "=value or environment variable " +
                key.toUpperCase().replace('.', '_'));
        return val;
    }

    // ── PRINT CONFIG SUMMARY (mask secrets) ──────────────────────────
    public static void printSummary() {
        Set<String> sensitiveKeys = Set.of("password", "token", "secret", "key", "credential");
        log.info("=== Configuration Summary ===");
        log.info("Environment: {}", get("env", "staging"));
        log.info("Base URL:    {}", get("baseUrl", "not set"));
        log.info("Browser:     {}", get("browser", "chrome"));
        log.info("Grid URL:    {}", get("gridUrl", "local"));
        properties.forEach((k, v) -> {
            String key = k.toString().toLowerCase();
            boolean sensitive = sensitiveKeys.stream().anyMatch(key::contains);
            log.debug("  {} = {}", k, sensitive ? "****" : v);
        });
    }
}

// CONFIG FILES — src/test/resources/config/
// base.properties:
/*
browser=chrome
env=staging
defaultTimeoutSeconds=15
fluentPollingMs=300
screenshotsOnFailure=true
*/

// staging.properties:
/*
baseUrl=https://staging.example.com
apiBaseUrl=https://api-staging.example.com
gridUrl=http://selenium-grid-staging:4444/wd/hub
test.admin.email=admin@staging-test.com
test.admin.password=ENC(c3RhZ2luZ0FkbWluUGFzcw==)
*/

// USAGE
public class AppWait {
    private static final Duration DEFAULT_TIMEOUT = ConfigReader.getDuration(
        "defaultTimeout", Duration.ofSeconds(15));
    private static final Duration POLLING = ConfigReader.getDuration(
        "fluentPolling", Duration.ofMillis(300));

    public AppWait(WebDriver driver) {
        this(driver, DEFAULT_TIMEOUT);
    }
    // ...
}

// Maven exec: mvn test -Denv=prod -Dbrowser=firefox -DgridUrl=http://grid:4444
```

### Key Design Decisions
- Resolution chain (system → env var → file → default) — CI can override any property without modifying files; `-D` flags take precedence over everything
- `ENC(...)` pattern — distinguishes encrypted values from plain text; safe to commit obfuscated staging credentials (not production)
- `require()` throws with actionable message — tells engineer exactly which `-D` flag or env var to set
- `getDuration` with unit suffixes (`10s`, `500ms`) — human-readable timeout values in config files; avoids magic numbers
- `printSummary` masks sensitive keys — safe to log config on CI without leaking credentials

---

## CQ23: Write a Selenium Grid 4 multi-node test execution setup with dynamic capabilities

### Problem Statement
Implement a `RemoteDriverFactory` that connects to Selenium Grid 4, sets browser-specific capabilities dynamically (Chrome, Firefox, Edge, Safari on macOS), handles Grid authentication, implements node health checking, and falls back to local execution when Grid is unavailable.

### Solution

```java
public class RemoteDriverFactory {
    private static final String GRID_URL = ConfigReader.get("gridUrl");
    private static final int    NODE_HEALTH_TIMEOUT_S = 10;

    public static WebDriver createDriver(String browserName, String browserVersion) {
        if (isGridAvailable()) {
            return createRemoteDriver(browserName, browserVersion);
        } else {
            log.warn("[Grid] Grid unavailable at {} — falling back to local", GRID_URL);
            return LocalDriverFactory.createDriver(browserName);
        }
    }

    // ── REMOTE DRIVER CREATION ────────────────────────────────────────
    private static WebDriver createRemoteDriver(String browser, String version) {
        AbstractDriverOptions<?> options = buildOptions(browser, version);
        URL gridUrl = parseUrl(GRID_URL);

        try {
            RemoteWebDriver driver = new RemoteWebDriver(gridUrl, options);
            driver.setFileDetector(new LocalFileDetector()); // enable file upload from client
            log.info("[Grid] Session created: {} @ {} → session {}",
                browser, GRID_URL, driver.getSessionId());
            return driver;
        } catch (SessionNotCreatedException e) {
            log.error("[Grid] Session creation failed: {}", e.getMessage());
            if (e.getMessage().contains("No node supports") ||
                    e.getMessage().contains("no matching capabilities")) {
                throw new ConfigurationException(
                    "No Grid node supports browser='" + browser +
                    "' version='" + version + "'. Check node registration.", e);
            }
            throw e;
        }
    }

    // ── BUILD OPTIONS PER BROWSER ─────────────────────────────────────
    private static AbstractDriverOptions<?> buildOptions(String browser, String version) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> buildChromeOptions(version);
            case "firefox" -> buildFirefoxOptions(version);
            case "edge" -> buildEdgeOptions(version);
            case "safari" -> buildSafariOptions(version);
            default -> throw new ConfigurationException("Unsupported browser: " + browser);
        };
    }

    private static ChromeOptions buildChromeOptions(String version) {
        ChromeOptions options = new ChromeOptions();
        // Selenium 4 Grid — use capability instead of desiredCapabilities
        if (version != null && !version.isBlank())
            options.setBrowserVersion(version);

        options.addArguments(
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-infobars"
        );

        // Allure environment tag for this session
        options.setCapability("selenoid:options", Map.of(
            "enableVNC",    true,
            "enableVideo",  ConfigReader.getBoolean("grid.videoRecording", false),
            "screenResolution", "1920x1080x24",
            "name",         System.getProperty("surefire.test.class", "unknown")
        ));

        // Platform — for cross-OS testing on Grid
        String platform = ConfigReader.get("grid.platform", "");
        if (!platform.isBlank())
            options.setPlatformName(platform);

        return options;
    }

    private static FirefoxOptions buildFirefoxOptions(String version) {
        FirefoxOptions options = new FirefoxOptions();
        if (version != null && !version.isBlank())
            options.setBrowserVersion(version);
        options.addArguments("-width=1920", "-height=1080");
        options.setCapability("moz:firefoxOptions",
            Map.of("args", List.of("--width=1920", "--height=1080")));
        return options;
    }

    private static EdgeOptions buildEdgeOptions(String version) {
        EdgeOptions options = new EdgeOptions();
        if (version != null && !version.isBlank())
            options.setBrowserVersion(version);
        options.addArguments(
            "--disable-gpu", "--no-sandbox",
            "--disable-dev-shm-usage", "--window-size=1920,1080");
        return options;
    }

    private static SafariOptions buildSafariOptions(String version) {
        SafariOptions options = new SafariOptions();
        // Safari requires macOS node; platform enforced
        options.setPlatformName("macOS 13");
        if (version != null && !version.isBlank())
            options.setBrowserVersion(version);
        return options;
    }

    // ── GRID HEALTH CHECK ─────────────────────────────────────────────
    public static boolean isGridAvailable() {
        if (GRID_URL == null || GRID_URL.isBlank() || "local".equalsIgnoreCase(GRID_URL))
            return false;
        try {
            URL statusUrl = new URL(GRID_URL.replace("/wd/hub", "") + "/status");
            HttpURLConnection conn = (HttpURLConnection) statusUrl.openConnection();
            conn.setConnectTimeout(NODE_HEALTH_TIMEOUT_S * 1000);
            conn.setReadTimeout(NODE_HEALTH_TIMEOUT_S * 1000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (IOException e) {
            log.debug("[Grid] Health check failed: {}", e.getMessage());
            return false;
        }
    }

    public static GridStatus getGridStatus() {
        try {
            String statusJson = given()
                .baseUri(GRID_URL.replace("/wd/hub", ""))
                .get("/status")
                .then().statusCode(200)
                .extract().asString();
            // Parse node count from Grid 4 /status response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(statusJson);
            int nodeCount = root.path("value").path("nodes").size();
            boolean ready = root.path("value").path("ready").asBoolean();
            return new GridStatus(ready, nodeCount);
        } catch (Exception e) {
            return new GridStatus(false, 0);
        }
    }

    // ── GRID AUTH (Selenium Grid 4 with --username/--password) ────────
    public static WebDriver createAuthenticatedDriver(String browser) {
        String user  = ConfigReader.require("grid.username");
        String pass  = ConfigReader.require("grid.password");
        String urlWithAuth = GRID_URL
            .replace("://", "://" + user + ":" + pass + "@");
        try {
            AbstractDriverOptions<?> options = buildOptions(browser, null);
            return new RemoteWebDriver(new URL(urlWithAuth), options);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Grid URL: " + urlWithAuth, e);
        }
    }

    private static URL parseUrl(String url) {
        try { return new URL(url); }
        catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Grid URL: " + url, e);
        }
    }

    public record GridStatus(boolean ready, int nodeCount) {}
}

// INTEGRATION WITH DRIVER FACTORY ─────────────────────────────────────
// In DriverFactory.initDriver():
public static void initDriver(String browser) {
    String gridUrl = ConfigReader.get("gridUrl", "local");
    WebDriver driver;
    if (!"local".equalsIgnoreCase(gridUrl)) {
        driver = RemoteDriverFactory.createDriver(browser,
            ConfigReader.get("browserVersion", ""));
    } else {
        driver = LocalDriverFactory.createDriver(browser);
    }
    driverThreadLocal.set(driver);
    applyTimeouts(driver);
}

// USAGE — parametrized cross-browser test suite
@ParameterizedTest
@CsvSource({
    "chrome,  stable",
    "firefox, latest",
    "edge,    latest"
})
void loginWorksOnAllBrowsers(String browser, String version) {
    WebDriver driver = RemoteDriverFactory.createDriver(browser, version);
    try {
        driver.get(BASE_URL + "/login");
        new LoginPage(driver).loginAs("user@test.com", "pass");
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    } finally {
        driver.quit();
    }
}
```

### Key Design Decisions
- Health check before session request — instant failure with clear message instead of 30s timeout waiting for grid to respond
- `LocalFileDetector` on remote driver — required for file upload tests; without it, `sendKeys(filePath)` silently fails on Grid nodes
- `selenoid:options` capability — supports Selenoid (popular Docker-based Grid) VNC and video recording without extra config
- `try/finally driver.quit()` in parametrized tests — parametrized tests don't use `@AfterEach`; explicit cleanup prevents session leaks
- Grid auth via URL — Selenium Grid 4 `--username/--password` flag uses HTTP Basic Auth embedded in URL

---

## CQ24: Build a test suite orchestrator that optimizes execution order based on historical failure data

### Problem Statement
Implement a `TestOrchestrator` that reads historical test results (from a JSON store updated each run), sorts tests to run failing tests first (fail-fast), skips quarantined tests, assigns test classes to execution buckets for balanced load distribution, and generates an optimized test execution plan.

### Solution

```java
public class TestOrchestrator {
    private static final Path HISTORY_FILE = Paths.get("target/test-history.json");
    private final ObjectMapper mapper = new ObjectMapper();

    // ── HISTORICAL DATA ───────────────────────────────────────────────
    public record TestResult(
        String  testId,         // "className#methodName"
        boolean passed,
        long    durationMs,
        int     consecutivePassCount,
        int     totalRuns,
        boolean quarantined,
        String  lastFailReason
    ) {}

    @SuppressWarnings("unchecked")
    public Map<String, TestResult> loadHistory() {
        if (!Files.exists(HISTORY_FILE)) return new HashMap<>();
        try {
            return mapper.readValue(HISTORY_FILE.toFile(),
                new TypeReference<Map<String, TestResult>>() {});
        } catch (IOException e) {
            log.warn("Could not load test history: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public void recordResult(String testId, boolean passed, long durationMs, String failReason) {
        Map<String, TestResult> history = loadHistory();
        TestResult prev = history.get(testId);
        int consecutive = (passed && prev != null) ? prev.consecutivePassCount() + 1 : 0;
        int totalRuns   = (prev != null) ? prev.totalRuns() + 1 : 1;
        boolean quarantined = prev != null && prev.quarantined() &&
            consecutive < 5; // unquarantine after 5 consecutive passes

        history.put(testId, new TestResult(
            testId, passed, durationMs, consecutive,
            totalRuns, quarantined,
            passed ? null : failReason));
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            mapper.writeValue(HISTORY_FILE.toFile(), history);
        } catch (IOException e) {
            log.warn("Could not save test history: {}", e.getMessage());
        }
    }

    public void quarantine(String testId) {
        Map<String, TestResult> history = loadHistory();
        TestResult prev = history.getOrDefault(testId,
            new TestResult(testId, false, 0, 0, 0, false, null));
        history.put(testId, new TestResult(
            testId, prev.passed(), prev.durationMs(), prev.consecutivePassCount(),
            prev.totalRuns(), true, prev.lastFailReason()));
        try { mapper.writeValue(HISTORY_FILE.toFile(), history); }
        catch (IOException e) { log.warn("Quarantine save failed", e); }
    }

    // ── SORTED EXECUTION PLAN ─────────────────────────────────────────
    // Returns test IDs sorted: failing first, then by average duration (shortest first)
    public List<String> getSortedExecutionPlan(List<String> allTestIds) {
        Map<String, TestResult> history = loadHistory();

        return allTestIds.stream()
            .filter(id -> {
                TestResult r = history.get(id);
                return r == null || !r.quarantined(); // skip quarantined
            })
            .sorted(Comparator
                // Failed tests (or unknown) first — fail fast
                .<String, Integer>comparing(id -> {
                    TestResult r = history.get(id);
                    if (r == null) return 1;      // unknown → run second
                    return r.passed() ? 2 : 0;   // failed → run first
                })
                // Within "failed" group: most recently failing first
                // (approximated by lowest consecutivePassCount)
                .thenComparingInt(id -> {
                    TestResult r = history.get(id);
                    return r == null ? 0 : r.consecutivePassCount();
                })
                // Within "passed" group: fastest first (minimizes time to feedback)
                .thenComparingLong(id -> {
                    TestResult r = history.get(id);
                    return r == null ? Long.MAX_VALUE : r.durationMs();
                })
            )
            .toList();
    }

    // ── BUCKET DISTRIBUTION for parallel workers ──────────────────────
    // Distributes tests across N buckets trying to equalize total estimated duration
    public List<List<String>> distributeIntoBuckets(List<String> testIds, int bucketCount) {
        Map<String, TestResult> history = loadHistory();

        List<List<String>> buckets    = new ArrayList<>();
        List<Long>         bucketTime = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            buckets.add(new ArrayList<>());
            bucketTime.add(0L);
        }

        // Sort by estimated duration descending — LPT (Longest Processing Time) algorithm
        List<String> sorted = testIds.stream()
            .sorted(Comparator.comparingLong((String id) -> {
                TestResult r = history.get(id);
                return r == null ? 30_000L : r.durationMs(); // 30s default estimate
            }).reversed())
            .toList();

        for (String testId : sorted) {
            // Assign to bucket with lowest current estimated total time
            int minBucket = IntStream.range(0, bucketCount)
                .boxed()
                .min(Comparator.comparingLong(bucketTime::get))
                .orElse(0);
            buckets.get(minBucket).add(testId);
            long duration = Optional.ofNullable(history.get(testId))
                .map(TestResult::durationMs).orElse(30_000L);
            bucketTime.set(minBucket, bucketTime.get(minBucket) + duration);
        }

        log.info("[Orchestrator] {} tests distributed across {} buckets. " +
            "Estimated times: {}", testIds.size(), bucketCount,
            bucketTime.stream().map(t -> t/1000 + "s").toList());
        return buckets;
    }

    // ── JUNIT 5 EXECUTION CONDITION — skip quarantined tests ──────────
    public static class QuarantineCondition implements ExecutionCondition {
        private static final TestOrchestrator orchestrator = new TestOrchestrator();

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
            if (ctx.getTestMethod().isEmpty()) return ConditionEvaluationResult.enabled("class");
            String testId = ctx.getRequiredTestClass().getName() +
                "#" + ctx.getRequiredTestMethod().getName();
            Map<String, TestResult> history = orchestrator.loadHistory();
            TestResult result = history.get(testId);
            if (result != null && result.quarantined()) {
                return ConditionEvaluationResult.disabled(
                    "QUARANTINED: " + testId +
                    " | Last failure: " + result.lastFailReason() +
                    " | Consecutive passes needed to restore: " +
                    (5 - result.consecutivePassCount()));
            }
            return ConditionEvaluationResult.enabled("Not quarantined");
        }
    }

    // ── AFTER-RUN REPORT ──────────────────────────────────────────────
    public void printSummary() {
        Map<String, TestResult> history = loadHistory();
        long quarantined  = history.values().stream().filter(TestResult::quarantined).count();
        long recentlyFail = history.values().stream()
            .filter(r -> !r.passed() && !r.quarantined()).count();
        long stable       = history.values().stream()
            .filter(r -> r.consecutivePassCount() >= 10).count();

        log.info("=== Test Execution Health ===");
        log.info("Total tracked tests: {}", history.size());
        log.info("Quarantined: {}", quarantined);
        log.info("Recently failing (not quarantined): {}", recentlyFail);
        log.info("Stable (10+ consecutive passes): {}", stable);
        if (recentlyFail > 0) {
            log.warn("Flaky tests needing attention:");
            history.values().stream()
                .filter(r -> !r.passed() && !r.quarantined())
                .forEach(r -> log.warn("  - {} | {}", r.testId(), r.lastFailReason()));
        }
    }
}

// REGISTRATION IN JUNIT 5 BASE CLASS
@ExtendWith(TestOrchestrator.QuarantineCondition.class)
public abstract class BaseTest {
    protected static final TestOrchestrator orchestrator = new TestOrchestrator();

    @AfterEach
    void recordExecutionResult(TestInfo info, TestReporter reporter) {
        // Recorded by AllureExtension.testFailed / testSucceeded
    }
}
```

### Key Design Decisions
- Fail-fast ordering — failing tests run first; CI gives earliest possible signal on broken builds without waiting for all 500 tests
- LPT (Longest Processing Time) bucketing — provably near-optimal for minimizing makespan; avoids naive round-robin which creates uneven buckets
- Quarantine threshold: 5 consecutive passes — single flaky test can't permanently quarantine itself; must prove stability
- `ExecutionCondition` for quarantine — JUnit 5 native; quarantined tests show as "disabled" not "skipped"; clearly visible in report
- History in `target/` — ephemeral per `mvn clean`; CI should persist via artifact upload/download between pipeline runs

---

## CQ25: Write a complete Selenium 4 BiDi (WebDriver BiDi protocol) event listener

### Problem Statement
Implement a `BiDiEventListener` using Selenium 4's WebDriver BiDi API to: listen to `console.log` events, capture JavaScript exceptions, monitor network requests/responses with full headers, intercept `beforeunload` events, and collect all events for post-test analysis.

### Solution

```java
public class BiDiEventListener implements AutoCloseable {
    private final WebDriver driver;
    private final BiDi biDi;

    private final List<ConsoleEntry>  consoleEntries  = Collections.synchronizedList(new ArrayList<>());
    private final List<JsException>   jsExceptions    = Collections.synchronizedList(new ArrayList<>());
    private final List<NetworkEvent>  networkEvents   = Collections.synchronizedList(new ArrayList<>());
    private final List<String>        navigationEvents= Collections.synchronizedList(new ArrayList<>());

    public BiDiEventListener(WebDriver driver) {
        if (!(driver instanceof HasBiDi bidiDriver)) {
            throw new IllegalArgumentException(
                "Driver does not support BiDi. Use Chrome or Firefox with BiDi enabled.");
        }
        this.driver = driver;
        this.biDi   = bidiDriver.getBiDi();
        registerListeners();
    }

    // ── BiDi Chrome option ────────────────────────────────────────────
    public static ChromeOptions biDiEnabledChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.setCapability("webSocketUrl", true); // Enable BiDi
        options.addArguments("--window-size=1920,1080", "--no-sandbox");
        return options;
    }

    // ── REGISTER ALL LISTENERS ────────────────────────────────────────
    private void registerListeners() {
        registerConsoleListener();
        registerJsExceptionListener();
        registerNetworkListener();
        registerNavigationListener();
    }

    // ── CONSOLE LOG LISTENER ──────────────────────────────────────────
    private void registerConsoleListener() {
        biDi.addBrowsingContextLifecycleHandler(
            BrowsingContextInfo.class, ctx -> {});

        // Use LogInspector for console and JS exceptions
        try (LogInspector inspector = new LogInspector(driver)) {
            inspector.onConsoleEntry(entry -> {
                ConsoleEntry ce = new ConsoleEntry(
                    entry.getType().toString(),
                    entry.getText(),
                    entry.getTimestamp(),
                    entry.getLevel().toString()
                );
                consoleEntries.add(ce);
                if ("error".equalsIgnoreCase(entry.getLevel().toString())) {
                    log.debug("[BiDi CONSOLE ERROR] {}", entry.getText());
                }
            });
        } catch (Exception e) {
            log.warn("BiDi console listener setup failed: {}", e.getMessage());
        }
    }

    // ── JS EXCEPTION LISTENER ─────────────────────────────────────────
    private void registerJsExceptionListener() {
        try (LogInspector inspector = new LogInspector(driver)) {
            inspector.onJavaScriptException(ex -> {
                JsException jse = new JsException(
                    ex.getMessage(),
                    ex.getDescription(),
                    Instant.now().toEpochMilli()
                );
                jsExceptions.add(jse);
                log.warn("[BiDi JS EXCEPTION] {} | {}", ex.getMessage(), ex.getDescription());
            });
        } catch (Exception e) {
            log.warn("BiDi JS exception listener failed: {}", e.getMessage());
        }
    }

    // ── NETWORK LISTENER ──────────────────────────────────────────────
    private void registerNetworkListener() {
        try {
            Network network = new Network(driver);
            // Request started
            network.addRequestHandler(request -> {
                networkEvents.add(new NetworkEvent(
                    "REQUEST",
                    request.getRequest().getUrl(),
                    request.getRequest().getMethod(),
                    null, // no status for request
                    Instant.now().toEpochMilli()
                ));
            });

            // Response completed
            network.addResponseCompletedHandler(response -> {
                networkEvents.add(new NetworkEvent(
                    "RESPONSE",
                    response.getRequest().getUrl(),
                    response.getRequest().getMethod(),
                    response.getResponse().getStatus(),
                    Instant.now().toEpochMilli()
                ));
                // Alert on 4xx/5xx
                int status = response.getResponse().getStatus();
                if (status >= 400) {
                    log.warn("[BiDi NETWORK] {} {} → {}",
                        response.getRequest().getMethod(),
                        response.getRequest().getUrl(),
                        status);
                }
            });
        } catch (Exception e) {
            log.warn("BiDi network listener failed: {}", e.getMessage());
        }
    }

    // ── NAVIGATION LISTENER ───────────────────────────────────────────
    private void registerNavigationListener() {
        try {
            BrowsingContext ctx = new BrowsingContext(driver, driver.getWindowHandle());
            ctx.onNavigationStarted(nav ->
                navigationEvents.add("NAV_START: " + nav.getUrl()));
            ctx.onLoad(nav ->
                navigationEvents.add("NAV_COMPLETE: " + nav.getUrl()));
            ctx.onDomContentLoaded(nav ->
                navigationEvents.add("DOM_READY: " + nav.getUrl()));
        } catch (Exception e) {
            log.warn("BiDi navigation listener failed: {}", e.getMessage());
        }
    }

    // ── ACCESSORS ─────────────────────────────────────────────────────
    public List<ConsoleEntry> getConsoleEntries() {
        return Collections.unmodifiableList(consoleEntries);
    }

    public List<ConsoleEntry> getConsoleErrors() {
        return consoleEntries.stream()
            .filter(e -> "error".equalsIgnoreCase(e.level()))
            .toList();
    }

    public List<JsException> getJsExceptions() {
        return Collections.unmodifiableList(jsExceptions);
    }

    public boolean hasJsExceptions() { return !jsExceptions.isEmpty(); }
    public boolean hasConsoleErrors() { return !getConsoleErrors().isEmpty(); }

    public List<NetworkEvent> getFailedRequests() {
        return networkEvents.stream()
            .filter(e -> "RESPONSE".equals(e.type()) &&
                e.status() != null && e.status() >= 400)
            .toList();
    }

    public List<NetworkEvent> getNetworkEvents(String urlFragment) {
        return networkEvents.stream()
            .filter(e -> e.url().contains(urlFragment))
            .toList();
    }

    // ── ASSERT HELPERS ────────────────────────────────────────────────
    public void assertNoJsExceptions() {
        if (hasJsExceptions()) {
            String summary = jsExceptions.stream()
                .map(e -> e.message() + ": " + e.description())
                .collect(Collectors.joining("\n  "));
            fail("JavaScript exceptions occurred during test:\n  " + summary);
        }
    }

    public void assertNoConsoleErrors() {
        List<ConsoleEntry> errors = getConsoleErrors();
        if (!errors.isEmpty()) {
            String summary = errors.stream()
                .map(ConsoleEntry::text)
                .collect(Collectors.joining("\n  "));
            fail("Console errors occurred during test:\n  " + summary);
        }
    }

    public void assertNoFailedRequests() {
        List<NetworkEvent> failed = getFailedRequests();
        if (!failed.isEmpty()) {
            String summary = failed.stream()
                .map(e -> e.status() + " " + e.method() + " " + e.url())
                .collect(Collectors.joining("\n  "));
            fail("Failed network requests during test:\n  " + summary);
        }
    }

    // ── ALLURE ATTACHMENT ─────────────────────────────────────────────
    public void attachEventsToAllure() {
        if (!consoleEntries.isEmpty())
            Allure.addAttachment("Console Entries",
                consoleEntries.stream().map(Object::toString)
                    .collect(Collectors.joining("\n")));
        if (!jsExceptions.isEmpty())
            Allure.addAttachment("JS Exceptions",
                jsExceptions.stream().map(Object::toString)
                    .collect(Collectors.joining("\n")));
        if (!networkEvents.isEmpty())
            Allure.addAttachment("Network Events",
                networkEvents.stream()
                    .filter(e -> e.status() != null && e.status() >= 400)
                    .map(Object::toString)
                    .collect(Collectors.joining("\n")));
    }

    @Override
    public void close() {
        try { biDi.close(); } catch (Exception e) { /* ignored */ }
    }

    // ── DATA RECORDS ──────────────────────────────────────────────────
    public record ConsoleEntry(String type, String text, long timestamp, String level) {}
    public record JsException(String message, String description, long timestamp) {}
    public record NetworkEvent(String type, String url, String method,
                               Integer status, long timestamp) {}
}

// USAGE IN TESTS
class BiDiIntegrationTest extends BaseTest {

    @Test
    void checkoutPageHasNoJsErrors() {
        // BiDi requires Chrome with webSocketUrl capability
        ChromeOptions opts = BiDiEventListener.biDiEnabledChromeOptions();
        WebDriver bidiDriver = new ChromeDriver(opts);

        try (BiDiEventListener events = new BiDiEventListener(bidiDriver)) {
            bidiDriver.get(BASE_URL + "/checkout");
            new LoginPage(bidiDriver).loginAs("user@test.com", "pass");
            new CheckoutPage(bidiDriver).fillCard("4242424242424242").submit();
            new AppWait(bidiDriver).untilVisible(
                By.cssSelector("[data-testid='confirmation']"));

            events.assertNoJsExceptions();
            events.assertNoConsoleErrors();
            events.assertNoFailedRequests();
            events.attachEventsToAllure();
        } finally {
            bidiDriver.quit();
        }
    }

    @Test
    void paymentApiCallSucceeds() {
        ChromeOptions opts = BiDiEventListener.biDiEnabledChromeOptions();
        WebDriver bidiDriver = new ChromeDriver(opts);
        try (BiDiEventListener events = new BiDiEventListener(bidiDriver)) {
            bidiDriver.get(BASE_URL + "/checkout");
            new CheckoutPage(bidiDriver).fillCard("4242424242424242").submit();

            List<NetworkEvent> paymentCalls =
                events.getNetworkEvents("/api/v1/payments");
            assertThat(paymentCalls).anyMatch(
                e -> "RESPONSE".equals(e.type()) && e.status() == 200);
        } finally {
            bidiDriver.quit();
        }
    }
}
```

### Key Design Decisions
- `webSocketUrl: true` capability — required for BiDi in Selenium 4; without it `HasBiDi` cast fails
- `Collections.synchronizedList` — BiDi event callbacks fire on separate threads; non-synchronized list causes `ConcurrentModificationException`
- `try-with-resources` on `BiDiEventListener` — `AutoCloseable` ensures BiDi session is properly closed even on test failure
- `assertNoJsExceptions` as explicit assertion — test author opts in to JS-exception checking; not automatic (some pages have intentional warnings)
- Network events filtered for 4xx/5xx only in `attachEventsToAllure` — full network log is too noisy; failures are signal

---

## CQ26: Write a complete Page Object base class with fluent navigation, scroll utilities, and component composition

### Problem Statement
Implement a `BasePage` that all page objects extend — providing common element interaction utilities, scroll-into-view before interaction, JS-based click fallback, component composition via typed sub-objects, navigation helpers, and URL assertion methods.

### Solution

```java
public abstract class BasePage {
    protected final WebDriver driver;
    protected final AppWait   wait;
    protected final JavascriptExecutor js;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new AppWait(driver);
        this.js     = (JavascriptExecutor) driver;
        verifyOnCorrectPage();
    }

    // Subclasses override to assert correct URL/title on construction
    protected void verifyOnCorrectPage() { /* no-op by default */ }

    // ── ELEMENT INTERACTION ───────────────────────────────────────────
    protected WebElement find(By locator) {
        return wait.untilVisible(locator);
    }

    protected List<WebElement> findAll(By locator) {
        return wait.untilAllVisible(locator);
    }

    protected void click(By locator) {
        WebElement el = wait.untilClickable(locator);
        scrollIntoView(el);
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            // Overlay covering element — try JS click as fallback
            log.debug("[BasePage] Native click intercepted on {}; using JS click", locator);
            jsClick(el);
        }
    }

    protected void type(By locator, String text) {
        WebElement el = wait.untilClickable(locator);
        scrollIntoView(el);
        el.clear();
        el.sendKeys(text);
    }

    protected void typeSlowly(By locator, String text, long delayMs) {
        WebElement el = wait.untilClickable(locator);
        scrollIntoView(el);
        el.clear();
        for (char c : text.toCharArray()) {
            el.sendKeys(String.valueOf(c));
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    protected void selectByText(By locator, String visibleText) {
        new Select(wait.untilVisible(locator)).selectByVisibleText(visibleText);
    }

    protected void selectByValue(By locator, String value) {
        new Select(wait.untilVisible(locator)).selectByValue(value);
    }

    protected String getText(By locator) {
        return find(locator).getText().trim();
    }

    protected String getAttribute(By locator, String attribute) {
        return find(locator).getDomAttribute(attribute);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    protected boolean isEnabled(By locator) {
        try { return driver.findElement(locator).isEnabled(); }
        catch (NoSuchElementException e) { return false; }
    }

    // ── SCROLL ────────────────────────────────────────────────────────
    protected void scrollIntoView(WebElement el) {
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    protected void scrollIntoView(By locator) {
        scrollIntoView(driver.findElement(locator));
    }

    protected void scrollToTop() {
        js.executeScript("window.scrollTo(0, 0);");
    }

    protected void scrollToBottom() {
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    protected void scrollBy(int x, int y) {
        js.executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    // ── JS UTILITIES ──────────────────────────────────────────────────
    protected void jsClick(WebElement el) {
        js.executeScript("arguments[0].click();", el);
    }

    protected void jsClick(By locator) {
        jsClick(driver.findElement(locator));
    }

    protected void highlight(By locator) {
        WebElement el = driver.findElement(locator);
        js.executeScript(
            "arguments[0].style.border='3px solid red'; " +
            "arguments[0].style.backgroundColor='yellow';", el);
    }

    protected Object executeScript(String script, Object... args) {
        return js.executeScript(script, args);
    }

    // ── NAVIGATION ────────────────────────────────────────────────────
    protected void navigateTo(String path) {
        String baseUrl = ConfigReader.get("baseUrl");
        driver.get(baseUrl + path);
        wait.untilDocumentReady();
    }

    protected void refresh() {
        driver.navigate().refresh();
        wait.untilDocumentReady();
    }

    protected void goBack() {
        driver.navigate().back();
        wait.untilDocumentReady();
    }

    // ── URL / TITLE ASSERTIONS ────────────────────────────────────────
    protected void assertUrlContains(String fragment) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("URL does not contain: " + fragment)
            .until(ExpectedConditions.urlContains(fragment));
    }

    protected void assertTitle(String expectedTitle) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Title did not become: " + expectedTitle)
            .until(ExpectedConditions.titleIs(expectedTitle));
    }

    protected String currentUrl() { return driver.getCurrentUrl(); }
    protected String pageTitle()  { return driver.getTitle(); }

    // ── COMPONENT COMPOSITION ─────────────────────────────────────────
    // Typed sub-components scoped to a container element
    protected <T extends BaseComponent> T component(Class<T> type, By containerLocator) {
        WebElement container = wait.untilVisible(containerLocator);
        try {
            return type.getConstructor(WebDriver.class, WebElement.class)
                .newInstance(driver, container);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create component: " + type.getSimpleName(), e);
        }
    }

    // ── WAIT HELPERS ──────────────────────────────────────────────────
    protected WebElement waitForVisible(By locator) {
        return wait.untilVisible(locator);
    }

    protected void waitForInvisible(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForText(By locator, String text) {
        wait.untilHasText(locator, text);
    }

    protected void waitForDocumentReady() {
        wait.untilDocumentReady();
    }
}

// BASE COMPONENT — for reusable UI sections (nav, table, card)
public abstract class BaseComponent {
    protected final WebDriver   driver;
    protected final WebElement  root;   // scoped to this component's container
    protected final AppWait     wait;

    protected BaseComponent(WebDriver driver, WebElement root) {
        this.driver = driver;
        this.root   = root;
        this.wait   = new AppWait(driver);
    }

    protected WebElement find(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(d -> {
                try { return root.findElement(locator); }
                catch (StaleElementReferenceException e) { return null; }
            });
    }

    protected List<WebElement> findAll(By locator) {
        return root.findElements(locator);
    }

    protected String getText(By locator) { return find(locator).getText().trim(); }
}

// EXAMPLE: NavigationBar component
public class NavigationBar extends BaseComponent {
    private static final By USER_MENU   = By.cssSelector("[data-testid='user-menu']");
    private static final By CART_BADGE  = By.cssSelector("[data-testid='cart-badge']");
    private static final By SEARCH_BOX  = By.cssSelector("[data-testid='nav-search']");

    public NavigationBar(WebDriver driver, WebElement root) {
        super(driver, root);
    }

    public int getCartCount() {
        try {
            return Integer.parseInt(getText(CART_BADGE));
        } catch (NoSuchElementException e) { return 0; }
    }

    public String getLoggedInUser() { return getText(USER_MENU); }

    public void search(String query) {
        find(SEARCH_BOX).sendKeys(query + Keys.ENTER);
    }
}

// EXAMPLE: Full page object using BasePage
public class ProductPage extends BasePage {
    private static final By TITLE        = By.cssSelector("[data-testid='product-title']");
    private static final By PRICE        = By.cssSelector("[data-testid='product-price']");
    private static final By ADD_TO_CART  = By.cssSelector("[data-testid='add-to-cart']");
    private static final By NAV_CONTAINER= By.cssSelector("[data-testid='nav']");

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected void verifyOnCorrectPage() {
        assertUrlContains("/products/");
    }

    public String getTitle()    { return getText(TITLE); }
    public String getPrice()    { return getText(PRICE); }

    public CartPage addToCart() {
        click(ADD_TO_CART);
        waitForText(By.cssSelector("[data-testid='toast']"), "Added to cart");
        return new CartPage(driver);
    }

    public NavigationBar getNav() {
        return component(NavigationBar.class, NAV_CONTAINER);
    }
}
```

### Key Design Decisions
- `scrollIntoView` before every click — sticky headers or lazy-loaded content may obscure elements; centering the element prevents `ElementClickInterceptedException`
- `ElementClickInterceptedException` fallback to JS click — overlays (cookie banners, loading spinners) are common; JS click bypasses overlay without waiting
- `verifyOnCorrectPage()` in constructor — assertions fire immediately if page object is constructed for the wrong page; fails with clear error rather than obscure `NoSuchElementException`
- Scoped `BaseComponent.find()` — `root.findElement()` not `driver.findElement()`; eliminates accidental cross-component element matches

---

## CQ27: Implement a JDBC database verification utility for UI-to-database assertion

### Problem Statement
Write a `DatabaseHelper` that executes SQL queries against the test database to verify that UI actions persist correctly — including record counts, field value assertions, transaction rollback for test isolation, and connection pooling via HikariCP.

### Solution

```java
public class DatabaseHelper implements AutoCloseable {
    private static HikariDataSource dataSource;
    private Connection connection;
    private boolean inTransaction = false;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ConfigReader.require("db.url"));
        config.setUsername(ConfigReader.require("db.username"));
        config.setPassword(ConfigReader.require("db.password"));
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(600_000);
        config.setPoolName("test-db-pool");
        dataSource = new HikariDataSource(config);
    }

    public DatabaseHelper() {
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain DB connection", e);
        }
    }

    // ── QUERY METHODS ─────────────────────────────────────────────────
    public <T> T queryForObject(String sql, Class<T> type, Object... params) {
        try (PreparedStatement ps = prepareStatement(sql, params);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return castResultSetValue(rs, 1, type);
            return null;
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Query failed: " + sql, e);
        }
    }

    public Map<String, Object> queryForRow(String sql, Object... params) {
        try (PreparedStatement ps = prepareStatement(sql, params);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
            }
            return row;
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Query failed: " + sql, e);
        }
    }

    public List<Map<String, Object>> queryForList(String sql, Object... params) {
        try (PreparedStatement ps = prepareStatement(sql, params);
             ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                }
                results.add(row);
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Query failed: " + sql, e);
        }
    }

    public int queryForCount(String sql, Object... params) {
        Integer count = queryForObject(sql, Integer.class, params);
        return count != null ? count : 0;
    }

    public int execute(String sql, Object... params) {
        try (PreparedStatement ps = prepareStatement(sql, params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Execute failed: " + sql, e);
        }
    }

    // ── TRANSACTION HELPERS ───────────────────────────────────────────
    public void beginTransaction() {
        try {
            connection.setAutoCommit(false);
            inTransaction = true;
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Cannot start transaction", e);
        }
    }

    public void commit() {
        try {
            connection.commit();
            connection.setAutoCommit(true);
            inTransaction = false;
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Commit failed", e);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
            inTransaction = false;
        } catch (SQLException e) {
            throw new DatabaseAssertionException("Rollback failed", e);
        }
    }

    // ── ASSERTION METHODS ─────────────────────────────────────────────
    public void assertRowExists(String table, String whereClause, Object... params) {
        int count = queryForCount(
            "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, params);
        assertThat(count)
            .as("Expected at least 1 row in %s WHERE %s", table, whereClause)
            .isGreaterThan(0);
    }

    public void assertRowCount(String table, String whereClause,
                                int expectedCount, Object... params) {
        int actual = queryForCount(
            "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, params);
        assertThat(actual)
            .as("Row count in %s WHERE %s", table, whereClause)
            .isEqualTo(expectedCount);
    }

    public void assertFieldValue(String table, String field,
                                  Object expectedValue,
                                  String whereClause, Object... params) {
        String sql = "SELECT " + field + " FROM " + table + " WHERE " + whereClause;
        Object actual = queryForObject(sql, Object.class, params);
        assertThat(actual)
            .as("Field '%s' in %s WHERE %s", field, table, whereClause)
            .isEqualTo(expectedValue);
    }

    public void assertNoRow(String table, String whereClause, Object... params) {
        int count = queryForCount(
            "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, params);
        assertThat(count)
            .as("Expected no rows in %s WHERE %s but found %d", table, whereClause, count)
            .isEqualTo(0);
    }

    // ── WAIT FOR DB STATE (async writes) ──────────────────────────────
    public void waitForRow(String table, String whereClause,
                            Duration timeout, Object... params) {
        Awaitility.await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(500))
            .withFailMessage("Row never appeared in %s WHERE %s within %s",
                table, whereClause, timeout)
            .until(() -> queryForCount(
                "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, params) > 0);
    }

    // ── CLEANUP HELPERS ───────────────────────────────────────────────
    public void deleteWhere(String table, String whereClause, Object... params) {
        execute("DELETE FROM " + table + " WHERE " + whereClause, params);
    }

    public void truncateTable(String table) {
        execute("TRUNCATE TABLE " + table);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────
    private PreparedStatement prepareStatement(String sql, Object[] params)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }

    @SuppressWarnings("unchecked")
    private <T> T castResultSetValue(ResultSet rs, int col, Class<T> type)
            throws SQLException {
        Object val = rs.getObject(col);
        if (val == null) return null;
        if (type == Integer.class) return (T) ((Number) val).intValue() == (int)(Object)((Number)val).intValue()
            ? type.cast(((Number)val).intValue()) : type.cast(val);
        if (type == Long.class)    return type.cast(((Number) val).longValue());
        if (type == String.class)  return type.cast(val.toString());
        return type.cast(val);
    }

    @Override
    public void close() {
        try {
            if (inTransaction) rollback();
            if (!connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.warn("Error closing DB connection", e);
        }
    }

    public static void shutdownPool() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    public static class DatabaseAssertionException extends RuntimeException {
        public DatabaseAssertionException(String message, Throwable cause) {
            super(message, cause);
        }
        public DatabaseAssertionException(String message) { super(message); }
    }
}

// USAGE IN TESTS
class OrderPersistenceTest extends BaseTest {
    private DatabaseHelper db;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        db = new DatabaseHelper();
    }

    @Test
    void placedOrderPersistsToDatabase() {
        TestUser user = factory.createUser(UserRole.CUSTOMER);
        new BrowserStorageUtils(driver()).injectJwtToken(factory.getAuthToken(user));
        navigateTo("/products/LAPTOP-001");
        String orderId = new ProductPage(driver())
            .addToCart()
            .checkout()
            .getOrderId();

        // DB verification — UI action created expected record
        db.assertRowExists("orders", "order_ref = ?", orderId);
        db.assertFieldValue("orders", "status", "CONFIRMED",
            "order_ref = ?", orderId);
        db.assertFieldValue("orders", "user_id", user.id(),
            "order_ref = ?", orderId);
        db.assertRowCount("order_items", "order_ref = ?", 1, orderId);
    }

    @Test
    void cancelledOrderUpdatesDbStatus() {
        // Async cancel — use waitForRow
        TestOrder order = factory.createOrder(factory.createUser(UserRole.CUSTOMER));
        navigateTo("/orders/" + order.id());
        new OrderPage(driver()).cancel("Changed mind");

        db.waitForRow("orders", "order_ref = ? AND status = 'CANCELLED'",
            Duration.ofSeconds(10), order.id());
        db.assertFieldValue("orders", "cancellation_reason", "Changed mind",
            "order_ref = ?", order.id());
    }

    @AfterEach
    void tearDown() {
        db.close();
        TestDataFactory.runCleanups();
        DriverFactory.quitDriver();
    }
}
```

### Key Design Decisions
- HikariCP static pool — one pool for entire test suite; parallel tests share connections without reconnecting per test
- `PreparedStatement` with `setObject` — parameterized queries prevent SQL injection, even in test code (test DBs are commonly shared staging environments)
- `waitForRow` with Awaitility — async event processing (Kafka consumers, background jobs) means DB writes lag UI actions; polling beats `Thread.sleep`
- `rollback()` in `close()` — if test fails mid-transaction, connection returns cleanly to pool; no dangling transactions

---

## CQ28: Write an accessibility testing utility using axe-core JavaScript injection

### Problem Statement
Implement an `AccessibilityHelper` that injects `axe-core` via JavaScript, runs accessibility audits against WCAG 2.1 AA rules, filters by violation impact level, generates tagged reports for Allure, and can assert zero violations for a specific page or component.

### Solution

```java
public class AccessibilityHelper {
    private static final String AXE_SCRIPT_PATH = "/axe/axe.min.js";
    private static String axeScript; // loaded once, reused

    private final WebDriver driver;
    private final JavascriptExecutor js;

    public AccessibilityHelper(WebDriver driver) {
        this.driver = driver;
        this.js     = (JavascriptExecutor) driver;
        injectAxe();
    }

    // ── INJECT AXE-CORE ───────────────────────────────────────────────
    // axe.min.js placed in src/test/resources/axe/
    private void injectAxe() {
        if (axeScript == null) {
            try (InputStream is =
                    getClass().getResourceAsStream(AXE_SCRIPT_PATH)) {
                if (is == null)
                    throw new ConfigurationException(
                        "axe.min.js not found at " + AXE_SCRIPT_PATH +
                        ". Download from: https://github.com/dequelabs/axe-core/releases");
                axeScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ConfigurationException("Cannot load axe-core script", e);
            }
        }
        js.executeScript(axeScript);
    }

    // ── RUN AUDIT — full page ─────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public AxeResults runFullPageAudit() {
        return runAudit(null, List.of("wcag2a", "wcag2aa", "wcag21aa", "best-practice"));
    }

    // ── RUN AUDIT — scoped to element ─────────────────────────────────
    @SuppressWarnings("unchecked")
    public AxeResults runComponentAudit(By componentLocator) {
        WebElement component = new AppWait(driver).untilVisible(componentLocator);
        return runAuditOnElement(component,
            List.of("wcag2a", "wcag2aa", "wcag21aa"));
    }

    @SuppressWarnings("unchecked")
    private AxeResults runAudit(String cssSelector, List<String> tags) {
        String script =
            "var callback = arguments[arguments.length - 1];" +
            "var options = { runOnly: { type: 'tag', values: " +
                new ObjectMapper().valueToTree(tags) + " } };" +
            (cssSelector != null ?
                "axe.run('" + cssSelector + "', options, function(err, results) {" :
                "axe.run(options, function(err, results) {") +
            "  if (err) callback({error: err.message});" +
            "  else callback(results);" +
            "});";
        Object raw = ((JavascriptExecutor) driver).executeAsyncScript(script);
        return parseResults((Map<String, Object>) raw);
    }

    @SuppressWarnings("unchecked")
    private AxeResults runAuditOnElement(WebElement element, List<String> tags) {
        String script =
            "var el = arguments[0];" +
            "var callback = arguments[arguments.length - 1];" +
            "var options = { runOnly: { type: 'tag', values: arguments[1] } };" +
            "axe.run(el, options, function(err, results) {" +
            "  if (err) callback({error: err.message});" +
            "  else callback(results);" +
            "});";
        Object raw = ((JavascriptExecutor) driver)
            .executeAsyncScript(script, element, tags);
        return parseResults((Map<String, Object>) raw);
    }

    // ── ASSERTION HELPERS ─────────────────────────────────────────────
    public void assertNoViolations() {
        assertNoViolations(Impact.MINOR);
    }

    public void assertNoViolations(Impact minimumImpact) {
        AxeResults results = runFullPageAudit();
        attachToAllure(results);

        List<Violation> relevant = results.violations().stream()
            .filter(v -> v.impact().ordinal() >= minimumImpact.ordinal())
            .toList();

        if (!relevant.isEmpty()) {
            String message = relevant.stream()
                .map(v -> String.format("[%s] %s\n  Help: %s\n  Elements: %s",
                    v.impact().name(),
                    v.description(),
                    v.helpUrl(),
                    v.nodes().stream()
                        .map(n -> "    " + n.target())
                        .collect(Collectors.joining("\n"))))
                .collect(Collectors.joining("\n\n"));
            fail("Accessibility violations found on " + driver.getCurrentUrl() +
                " (minimum impact: " + minimumImpact + "):\n\n" + message);
        }
    }

    public void assertNoViolationsForComponent(By locator) {
        AxeResults results = runComponentAudit(locator);
        attachToAllure(results, "Component: " + locator);
        List<Violation> violations = results.violations();
        if (!violations.isEmpty()) {
            fail("Accessibility violations in component " + locator + ":\n" +
                violations.stream()
                    .map(v -> "[" + v.impact() + "] " + v.description())
                    .collect(Collectors.joining("\n")));
        }
    }

    // ── ALLURE ATTACHMENT ─────────────────────────────────────────────
    private void attachToAllure(AxeResults results) {
        attachToAllure(results, "Accessibility Report");
    }

    private void attachToAllure(AxeResults results, String name) {
        String report = buildTextReport(results);
        Allure.addAttachment(name, "text/plain", report, ".txt");
        if (!results.violations().isEmpty()) {
            Allure.addAttachment(name + " — Violations only",
                results.violations().stream()
                    .map(v -> "[" + v.impact() + "] " + v.id() + ": " + v.description())
                    .collect(Collectors.joining("\n")));
        }
    }

    private String buildTextReport(AxeResults results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Axe-Core Accessibility Report\n");
        sb.append("URL: ").append(driver.getCurrentUrl()).append("\n");
        sb.append("Violations: ").append(results.violations().size()).append("\n");
        sb.append("Passes:     ").append(results.passes().size()).append("\n\n");
        results.violations().forEach(v -> {
            sb.append("VIOLATION [").append(v.impact()).append("] ")
              .append(v.id()).append(": ").append(v.description()).append("\n");
            sb.append("  Help: ").append(v.helpUrl()).append("\n");
            v.nodes().forEach(n ->
                sb.append("  Element: ").append(n.target()).append("\n")
                  .append("  Fix any: ").append(n.failureSummary()).append("\n"));
            sb.append("\n");
        });
        return sb.toString();
    }

    // ── PARSE RESULTS ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private AxeResults parseResults(Map<String, Object> raw) {
        if (raw.containsKey("error"))
            throw new RuntimeException("axe-core error: " + raw.get("error"));

        List<Violation> violations = parseViolationList(
            (List<Map<String, Object>>) raw.getOrDefault("violations", List.of()));
        List<Violation> passes = parseViolationList(
            (List<Map<String, Object>>) raw.getOrDefault("passes", List.of()));
        return new AxeResults(violations, passes);
    }

    @SuppressWarnings("unchecked")
    private List<Violation> parseViolationList(List<Map<String, Object>> items) {
        return items.stream().map(item -> {
            List<Map<String, Object>> nodeRaw =
                (List<Map<String, Object>>) item.getOrDefault("nodes", List.of());
            List<ViolationNode> nodes = nodeRaw.stream()
                .map(n -> new ViolationNode(
                    String.valueOf(n.getOrDefault("target", "")),
                    String.valueOf(n.getOrDefault("failureSummary", ""))))
                .toList();
            return new Violation(
                String.valueOf(item.get("id")),
                String.valueOf(item.get("description")),
                Impact.fromString(String.valueOf(item.getOrDefault("impact", "minor"))),
                String.valueOf(item.getOrDefault("helpUrl", "")),
                nodes);
        }).toList();
    }

    public enum Impact {
        MINOR, MODERATE, SERIOUS, CRITICAL;
        public static Impact fromString(String s) {
            try { return valueOf(s.toUpperCase()); } catch (Exception e) { return MINOR; }
        }
    }

    public record Violation(String id, String description, Impact impact,
                             String helpUrl, List<ViolationNode> nodes) {}
    public record ViolationNode(String target, String failureSummary) {}
    public record AxeResults(List<Violation> violations, List<Violation> passes) {}
}

// USAGE IN TESTS
@Tag("accessibility")
class AccessibilityTest extends BaseTest {

    @Test
    void checkoutPageMeetsWcag21AA() {
        loginAs("CUSTOMER");
        navigateTo("/checkout");
        new AppWait(driver()).untilDocumentReady();

        new AccessibilityHelper(driver())
            .assertNoViolations(AccessibilityHelper.Impact.SERIOUS);
    }

    @Test
    void loginFormIsAccessible() {
        navigateTo("/login");
        new AccessibilityHelper(driver())
            .assertNoViolationsForComponent(
                By.cssSelector("[data-testid='login-form']"));
    }
}
```

### Key Design Decisions
- `axe.min.js` loaded from classpath once — reading a 200KB file once vs. per test saves significant suite time; `static` field with lazy load
- `executeAsyncScript` for axe — axe is asynchronous; `executeScript` would return immediately before results are ready
- `Impact.MINOR` as default assertion threshold — catches serious/critical first; `MINOR` violations (low-priority cosmetics) can be triaged separately
- WCAG tags (`wcag2a`, `wcag2aa`, `wcag21aa`) explicit in options — tags control which rules run; avoids test noise from experimental rules

---

## CQ29: Write a cross-browser screenshot comparison utility with viewport management

### Problem Statement
Implement a `CrossBrowserScreenshotCollector` that captures page screenshots across multiple browsers in parallel, generates an HTML comparison report showing screenshots side by side, and highlights visual differences between browsers.

### Solution

```java
public class CrossBrowserScreenshotCollector {
    private static final Path OUTPUT_DIR = Paths.get("target/cross-browser-report");
    private final List<String> browsers;
    private final ExecutorService pool;

    public CrossBrowserScreenshotCollector(String... browsers) {
        this.browsers = List.of(browsers);
        this.pool     = Executors.newFixedThreadPool(browsers.length);
    }

    public record BrowserScreenshot(String browser, String pageName,
                                     String screenshotBase64, long captureMs) {}

    // ── CAPTURE ACROSS BROWSERS IN PARALLEL ──────────────────────────
    public List<BrowserScreenshot> capturePageAcrossBrowsers(
            String pageName, String url, Dimension viewport) {

        List<Future<BrowserScreenshot>> futures = browsers.stream()
            .map(browser -> pool.submit(() ->
                captureInBrowser(browser, pageName, url, viewport)))
            .toList();

        return futures.stream().map(f -> {
            try { return f.get(120, TimeUnit.SECONDS); }
            catch (Exception e) {
                log.error("Screenshot capture failed for browser", e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .toList();
    }

    private BrowserScreenshot captureInBrowser(
            String browser, String pageName, String url, Dimension viewport) {
        WebDriver d = null;
        try {
            d = RemoteDriverFactory.createDriver(browser, "latest");
            d.manage().window().setSize(viewport);
            d.get(url);
            new AppWait(d).untilDocumentReady();
            // Allow animations to settle
            Thread.sleep(1000);
            byte[] png = ((TakesScreenshot) d)
                .getScreenshotAs(OutputType.BYTES);
            String base64 = Base64.getEncoder().encodeToString(png);
            return new BrowserScreenshot(browser, pageName, base64,
                System.currentTimeMillis());
        } catch (Exception e) {
            log.error("[CrossBrowser] Failed on {}: {}", browser, e.getMessage());
            return null;
        } finally {
            if (d != null) d.quit();
        }
    }

    // ── GENERATE HTML REPORT ──────────────────────────────────────────
    public Path generateHtmlReport(List<BrowserScreenshot> screenshots,
                                    String reportTitle) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Path reportPath = OUTPUT_DIR.resolve("index.html");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>")
            .append("<title>").append(reportTitle).append("</title>")
            .append("<style>")
            .append("body{font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px}")
            .append(".grid{display:flex;gap:20px;flex-wrap:wrap}")
            .append(".card{background:white;border-radius:8px;padding:16px;")
            .append("      box-shadow:0 2px 4px rgba(0,0,0,.1);flex:1;min-width:300px}")
            .append(".card h3{margin:0 0 8px;color:#333;font-size:14px}")
            .append(".card img{width:100%;border:1px solid #ddd;border-radius:4px}")
            .append(".meta{font-size:12px;color:#888;margin-top:8px}")
            .append("</style></head><body>")
            .append("<h1>").append(reportTitle).append("</h1>")
            .append("<div class='grid'>");

        for (BrowserScreenshot ss : screenshots) {
            html.append("<div class='card'>")
                .append("<h3>").append(ss.browser().toUpperCase()).append("</h3>")
                .append("<img src='data:image/png;base64,")
                .append(ss.screenshotBase64()).append("' alt='")
                .append(ss.browser()).append("'>")
                .append("<div class='meta'>Page: ").append(ss.pageName()).append("</div>")
                .append("</div>");
        }

        html.append("</div></body></html>");
        Files.writeString(reportPath, html.toString(), StandardCharsets.UTF_8);
        Allure.addAttachment("Cross-Browser Report", "text/html",
            html.toString(), ".html");
        log.info("[CrossBrowser] Report generated: {}", reportPath.toAbsolutePath());
        return reportPath;
    }

    public void shutdown() { pool.shutdownNow(); }
}

// USAGE
class CrossBrowserTest {

    @Test
    void homepageLooksConsistentAcrossBrowsers() throws IOException {
        CrossBrowserScreenshotCollector collector =
            new CrossBrowserScreenshotCollector("chrome", "firefox", "edge");

        List<CrossBrowserScreenshotCollector.BrowserScreenshot> shots =
            collector.capturePageAcrossBrowsers(
                "homepage",
                ConfigReader.get("baseUrl"),
                new Dimension(1920, 1080));

        assertThat(shots).hasSize(3);
        collector.generateHtmlReport(shots, "Homepage Cross-Browser Comparison");
        collector.shutdown();
    }
}
```

### Key Design Decisions
- Parallel capture via `ExecutorService` — three browsers captured simultaneously; reduces total capture time from `3 × T` to `~T`
- `Thread.sleep(1000)` after `untilDocumentReady()` — CSS animations and lazy image loads continue after `DOMContentLoaded`; brief settle avoids capturing mid-transition states
- Base64-embedded images in HTML — single self-contained HTML file; no external image files to manage or lose
- `d.quit()` in `finally` — browser session always closed even on capture failure; prevents session leak on Grid

---

## CQ30: Write a full end-to-end test demonstrating all framework components working together

### Problem Statement
Implement a complete production-grade E2E test for a critical user journey (guest → register → browse → purchase → order confirmation) that uses: Page Object Model, API test data setup, JWT auth bypass, custom waits, Allure reporting, DB verification, visual baseline check, and automatic cleanup.

### Solution

```java
/**
 * E2E: User Registration → Product Browse → Add to Cart → Checkout → Confirmation
 * Demonstrates all framework components in a single production-quality test.
 */
@Epic("Critical User Journeys")
@Feature("Purchase Flow")
@Story("First-time Customer Purchase")
@Severity(SeverityLevel.BLOCKER)
@Owner("platform-qa")
@TmsLink("TC-0001")
@ExtendWith({RetryExtension.class, AllureExtension.class})
class PurchaseJourneyE2eTest extends BaseTest {

    private TestDataFactory factory;
    private DatabaseHelper  db;

    @BeforeEach
    @Step("Setup: Initialize driver, factory, and DB helper")
    void setUp() {
        DriverFactory.initDriver(ConfigReader.get("browser", "chrome"));
        factory = new TestDataFactory(AuthTokenCache.getOrCreate("ADMIN"));
        db      = new DatabaseHelper();
    }

    @Test
    @Retryable(maxAttempts = 2)
    @DisplayName("First-time customer completes full purchase journey")
    void firstTimeCustomerCompletesPurchase() {
        // ── STEP 1: Create test data via API ──────────────────────────
        AllureSteps.step("Create test user and product via API", () -> {
            TestUser customer = factory.createUser(UserRole.CUSTOMER,
                UserBuilder.defaults(UserRole.CUSTOMER)
                    .firstName("Jake").lastName("Tester"));
            TestDataContext.set("customer", customer);

            TestProduct laptop = factory.createProduct(
                "LAPTOP-E2E-001", BigDecimal.valueOf(1299.99));
            TestDataContext.set("product", laptop);
        });

        TestUser    customer = TestDataContext.get("customer");
        TestProduct laptop   = TestDataContext.get("product");
        String      token    = factory.getAuthToken(customer);

        // ── STEP 2: Auth bypass — inject JWT directly ─────────────────
        AllureSteps.step("Inject JWT and navigate to homepage", () -> {
            driver().get(ConfigReader.get("baseUrl"));
            new BrowserStorageUtils(driver()).injectJwtToken(token);
        });

        // ── STEP 3: Browse and add to cart ───────────────────────────
        AllureSteps.step("Browse product and add to cart", () -> {
            navigateTo("/products/" + laptop.sku());
            ProductPage product = new ProductPage(driver());

            assertThat(product.getTitle()).isNotBlank();
            assertThat(product.getPrice()).contains("1,299.99");

            // Visual baseline check for product page
            new VisualTestHelper(driver())
                .withTolerance(0.5)
                .exclude(By.cssSelector("[data-testid='stock-count']"))
                .assertMatchesBaseline("product-page-" + laptop.sku());

            product.addToCart();
        });

        // ── STEP 4: Cart verification ─────────────────────────────────
        AllureSteps.step("Verify cart contents", () -> {
            CartPage cart = new CartPage(driver());
            assertThat(cart.getItemCount()).isEqualTo(1);
            assertThat(cart.getItem(0).getName()).containsIgnoringCase("laptop");
            assertThat(cart.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1299.99));
        });

        // ── STEP 5: Checkout ──────────────────────────────────────────
        AllureSteps.step("Complete checkout with test card", () -> {
            CheckoutPage checkout = new CartPage(driver()).proceedToCheckout();
            checkout
                .fillShipping("123 Test Street", "Test City", "12345")
                .fillCard("4242424242424242", "12/28", "123")
                .submit();
        });

        // ── STEP 6: Confirmation page ─────────────────────────────────
        String orderId = AllureSteps.stepWithReturn("Verify confirmation page", () -> {
            ConfirmationPage confirmation = new ConfirmationPage(driver());
            confirmation.assertUrlContains("/confirmation");
            assertThat(confirmation.getConfirmationMessage())
                .isEqualTo("Your order has been placed!");
            assertThat(confirmation.getOrderId()).matches("ORD-\\d{6}");
            return confirmation.getOrderId();
        });
        TestDataContext.set("orderId", orderId);

        // ── STEP 7: API verification ──────────────────────────────────
        AllureSteps.step("Verify order via API", () -> {
            OrderApiClient.assertOrderStatus(token, orderId, "CONFIRMED");
            OrderResponse apiOrder = OrderApiClient.getOrder(token, orderId);
            assertThat(apiOrder.total())
                .isEqualByComparingTo(BigDecimal.valueOf(1299.99));
            assertThat(apiOrder.items()).hasSize(1);
            assertThat(apiOrder.items().get(0).sku()).isEqualTo(laptop.sku());
        });

        // ── STEP 8: Database verification ────────────────────────────
        AllureSteps.step("Verify order persisted to database", () -> {
            db.assertRowExists("orders", "order_ref = ? AND status = 'CONFIRMED'", orderId);
            db.assertFieldValue("orders", "user_id", customer.id(),
                "order_ref = ?", orderId);
            db.assertRowCount("order_items", "order_ref = ?", 1, orderId);
            db.assertFieldValue("order_items", "sku", laptop.sku(),
                "order_ref = ?", orderId);
        });

        // ── STEP 9: Email notification check ─────────────────────────
        AllureSteps.step("Verify order confirmation email was sent", () -> {
            // Awaitility — email delivery is async
            await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var email = EmailInbox.getEmail(
                        customer.email(), "Order Confirmed");
                    assertThat(email).isNotNull();
                    assertThat(email.body()).contains(orderId);
                });
        });

        // ── STEP 10: Accessibility spot-check on confirmation page ────
        AllureSteps.step("Accessibility check on confirmation page", () -> {
            new AccessibilityHelper(driver())
                .assertNoViolations(AccessibilityHelper.Impact.SERIOUS);
        });
    }

    @AfterEach
    @Step("Teardown: cleanup test data and release resources")
    void tearDown() {
        // Always attach current state for debugging
        if (DriverFactory.getDriver() != null) {
            AllureExtension.attachScreenshot(driver(), "Final State");
            Allure.addAttachment("Final URL", driver().getCurrentUrl());
        }

        // Cancel created order (cleanup)
        String orderId = TestDataContext.get("orderId");
        if (orderId != null) {
            try {
                OrderApiClient.cancelOrder(
                    AuthTokenCache.getOrCreate("ADMIN"), orderId);
            } catch (Exception e) {
                log.warn("Order cleanup failed: {}", e.getMessage());
            }
        }

        db.close();
        TestDataFactory.runCleanups();
        DriverFactory.quitDriver();
        TestDataContext.clear();
    }
}

// TEST DATA CONTEXT — thread-safe per-test storage for sharing data between steps
public class TestDataContext {
    private static final ThreadLocal<Map<String, Object>> context =
        ThreadLocal.withInitial(HashMap::new);

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) context.get().get(key);
    }

    public static void set(String key, Object value) {
        context.get().put(key, value);
    }

    public static void clear() {
        context.get().clear();
        context.remove();
    }
}

// ALLURE STEP HELPERS — lambda-based steps for return values
public class AllureSteps {
    public static void step(String name, Runnable action) {
        Allure.step(name, action::run);
    }

    @SuppressWarnings("unchecked")
    public static <T> T stepWithReturn(String name, java.util.concurrent.Callable<T> action) {
        final Object[] result = new Object[1];
        Allure.step(name, () -> result[0] = action.call());
        return (T) result[0];
    }
}
```

### Key Design Decisions
- `TestDataContext` ThreadLocal — shares data between `@Step` lambdas without making fields nullable; each parallel test thread has isolated context
- API setup, not UI setup — test begins with already-authenticated user and existing product; removes 3 fragile UI setup steps from the critical path
- JWT injection after `driver.get(BASE_URL)` — must be on the target domain before setting localStorage (same-origin policy)
- 10-step Allure structure — granular `@Step` blocks appear as named timeline entries; failed step is immediately identifiable without reading stack trace
- DB + API + Email triple verification — UI assertion proves rendering; API assertion proves backend; DB assertion proves persistence; email proves event publishing
- `TestDataContext.clear()` in `@AfterEach` — ThreadLocal cleared explicitly; prevents memory leak in thread pools

---

# Section 3 — Architecture Questions

## AQ1: Design a Selenium framework architecture for a 500+ test organization with 15 teams

### Question
You are joining a company with 15 product teams, 200 engineers, and 500+ Selenium tests currently in a single monorepo with no shared abstractions — each team has copy-pasted page objects, duplicated wait logic, and tests intermittently fail due to shared test data. Design the target architecture.

### Answer

**Core Problem Analysis:**
The root causes are: no shared abstractions (duplication), no test data isolation (shared state causing flakiness), and no governance (15 teams moving independently).

**Target Architecture — Layered Monorepo with Bounded Contexts:**

```
automation-platform/
├── framework-core/          ← Published internal library (v-semver)
│   ├── driver/              ← DriverFactory, RemoteDriverFactory
│   ├── wait/                ← AppWait, CustomExpectedConditions
│   ├── base/                ← BasePage, BaseComponent, BaseTest
│   ├── reporting/           ← AllureExtension, RetryExtension
│   ├── data/                ← TestDataFactory, ConfigReader
│   └── utils/               ← BrowserStorageUtils, ActionsHelper
│
├── domain-components/       ← Shared page objects for common UI
│   ├── auth/                ← LoginPage, RegisterPage (used by ALL teams)
│   ├── checkout/            ← CheckoutPage (owned by Payments team)
│   └── navigation/          ← NavigationBar, Breadcrumbs
│
└── team-tests/
    ├── payments-team/       ← Tests owned by payments team
    ├── catalog-team/        ← Tests owned by catalog team
    └── ...                  ← 13 more team modules
```

**Key Architectural Decisions:**

**1. `framework-core` as internal Maven artifact**
Teams declare `<dependency>framework-core:1.4.2</dependency>` — not copy-paste. Breaking changes require a major version bump and migration guide. Teams can freeze at a stable version.

**2. Test Data Isolation via `TestDataFactory`**
Each test creates its own entities via API with ThreadLocal cleanup. No shared staging users (`testuser1`, `testuser2` that everyone modifies). Eliminates the entire class of data-collision flakiness.

**3. Team Ownership Model**
Domain components are owned by the team responsible for that feature area. `CheckoutPage` is owned by Payments team; they review PRs that change it. Ownership enforced via `CODEOWNERS` file.

**4. Standardized Execution Contract**
All tests must implement `BaseTest` interface contract: no `Thread.sleep`, mandatory `@AfterEach` cleanup, no hardcoded credentials. Enforced via ArchUnit rules:
```java
@Test
void noThreadSleepInTests() {
    noClasses().that().resideInAPackage("..tests..")
        .should().callMethod(Thread.class, "sleep", long.class)
        .check(new ClassFileImporter().importPackages("com.example"));
}
```

**5. Parallel Execution at Team Level**
Each team module runs in its own JVM (`maven-surefire-plugin` module-level fork). Prevents interference between teams. `framework-core` thread safety guarantees apply within each module.

**6. Observability Layer**
Allure results from all teams published to a single Allure TestOps server. Teams see their own tests. Platform team sees all tests. Trend lines identify teams with rising flakiness.

**7. Framework Release Cadence**
- `framework-core` releases weekly (or on-demand with semantic versioning)
- Automated compatibility matrix runs all 15 team test suites against each candidate release
- Teams have 2 weeks to migrate before old version EOLs

**Metrics to Track:**
- Flakiness rate per team (target: <2%)
- Suite duration (target: <15 min for smoke, <45 min for full regression)
- Framework adoption rate (% of tests using `framework-core` abstractions)
- Mean time to detect a failure (alert fired within 5 minutes of CI failure)

---

## AQ2: Explain Selenium Grid 4 architecture and how you would scale it for 1000 concurrent test sessions

### Question
Describe the internal components of Selenium Grid 4, how they differ from Grid 3, and design a production Grid deployment that supports 1000 concurrent browser sessions with high availability, auto-scaling, and session observability.

### Answer

**Selenium Grid 4 Components:**

| Component | Role |
|---|---|
| **Router** | Entry point; routes new session requests and commands to correct Node/Distributor |
| **Distributor** | Tracks node availability; assigns sessions to nodes using slot algorithm |
| **Session Map** | Stores sessionId → node mapping; persisted in Redis for HA |
| **Node** | Runs browser processes; reports available slots to Distributor |
| **Event Bus** | Internal message bus (default: in-process; can be Kafka/NATS for distributed) |
| **Observability** | Built-in OpenTelemetry; exports traces and metrics |

**Grid 3 vs Grid 4 Differences:**
- Grid 3: Hub/Node binary; no microservice separation; W3C not fully supported; no CDP passthrough
- Grid 4: Fully W3C; CDP tunneled to Node; microservice architecture; native BiDi support; GraphQL status API; Docker/K8s native

**1000-Concurrent-Session Production Architecture:**

```
                         ┌─────────────────────────┐
                         │   Load Balancer (L4)     │
                         │   (AWS ALB / NGINX)       │
                         └────────────┬────────────┘
                                      │
              ┌───────────────────────┴──────────────────────┐
              │              Router Cluster (×3)              │
              │         (3 pods, stateless, HPA)              │
              └────────┬───────────────────────┬─────────────┘
                       │                       │
              ┌────────▼────────┐   ┌──────────▼────────┐
              │   Distributor   │   │   Session Map      │
              │   (×2, leader   │   │   (Redis Cluster   │
              │   election)     │   │    3 primaries)    │
              └────────┬────────┘   └───────────────────-┘
                       │
        ┌──────────────┴───────────────────────────┐
        │           Node Pool (Kubernetes)          │
        │                                           │
        │  ┌──────────┐  ┌──────────┐  ┌─────────┐│
        │  │Node Pod  │  │Node Pod  │  │Node Pod ││
        │  │Chrome ×4 │  │Firefox×4 │  │Edge ×4  ││
        │  │(250 pods)│  │(100 pods)│  │(50 pods)││
        │  └──────────┘  └──────────┘  └─────────┘│
        │                                           │
        │  [HPA scales pods based on pending queue] │
        └───────────────────────────────────────────┘
```

**Scaling Mechanisms:**

**1. Node Pod Auto-scaling (HPA)**
```yaml
# Kubernetes HPA — scale Node pods on pending session queue depth
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: selenium-chrome-nodes
spec:
  minReplicas: 10
  maxReplicas: 300
  metrics:
  - type: External
    external:
      metric:
        name: selenium_grid_pending_sessions
      target:
        type: AverageValue
        averageValue: "2"  # scale when avg pending sessions per pod > 2
```

**2. Node Configuration (4 browsers per pod)**
```bash
# Node startup — 4 Chrome slots per container
java -jar selenium-server.jar node \
  --max-sessions 4 \
  --override-max-sessions true \
  --session-timeout 300 \
  --hub http://router-service:4444
```

**3. Redis Session Map — HA**
Session map in Redis Cluster: no single point of failure. If a Router pod dies, new Router reads session→node mapping from Redis and routes correctly.

**4. Observability Stack**
- Grid's OpenTelemetry → Jaeger (trace: session lifecycle, node assignment time)
- Grid metrics → Prometheus → Grafana dashboards: active sessions, queue depth, node utilization per browser, session creation latency
- Alert: `pending_sessions > 50 for 2 minutes` → trigger emergency scale-out

**5. Node Image Strategy**
Custom Docker images with pre-warmed browser caches, pre-installed extensions:
```dockerfile
FROM selenium/node-chrome:4.20
RUN google-chrome --no-sandbox --dump-dom about:blank  # warm binary
COPY test-certs/ /usr/local/share/ca-certificates/
RUN update-ca-certificates
```

**6. Session Timeout + Orphan Cleanup**
`--session-timeout 300` — sessions unused for 5 minutes auto-terminated. Prevents test crashes from leaking Grid slots.

**Capacity Math:**
- 1000 concurrent = 250 Chrome pods × 4 slots = 1000 Chrome
- Average session duration: 3 minutes
- With 10-minute suite: need `1000 × 10/3 ≈ 3333` total sessions → throughput target
- Node startup time: ~15 seconds — pre-warm minimum 50 pods at all times

---

## AQ3: How would you design a test framework that supports Selenium, Playwright, and REST API tests in the same codebase?

### Question
Your organization wants to run Selenium, Playwright, and REST Assured tests from a single framework. Design the abstraction layer, project structure, execution model, and CI pipeline to support all three with shared utilities and single test reports.

### Answer

**Unification Strategy — Adapter Pattern:**

The key insight: avoid a lowest-common-denominator API. Instead, create a thin adapter interface that each tool implements, and allow tests to use tool-specific APIs when needed.

**Core Abstraction:**
```java
// Driver-agnostic interface — minimal contract
public interface TestDriver {
    void get(String url);
    WebElement find(By locator);    // Selenium By — common vocabulary
    void quit();
    String getCurrentUrl();
    boolean isDisplayed(By locator);
    void click(By locator);
    void type(By locator, String text);
}

// Selenium implementation
public class SeleniumDriver implements TestDriver {
    private final WebDriver driver;
    public SeleniumDriver(WebDriver driver) { this.driver = driver; }
    @Override public void get(String url) { driver.get(url); }
    @Override public WebElement find(By locator) {
        return new AppWait(driver).untilVisible(locator); }
    // ...
}

// Playwright implementation (wraps Playwright Page)
public class PlaywrightDriver implements TestDriver {
    private final Page page;
    public PlaywrightDriver(Page page) { this.page = page; }
    @Override public void get(String url) { page.navigate(url); }
    @Override public WebElement find(By locator) {
        // Wrap Playwright Locator in a WebElement adapter
        return new PlaywrightElementAdapter(page.locator(toPlaywrightSelector(locator)));
    }
    // ...
}
```

**Project Structure:**
```
unified-framework/
├── core/                  ← Shared: ConfigReader, TestDataFactory, Allure
├── selenium-driver/       ← SeleniumDriver, DriverFactory
├── playwright-driver/     ← PlaywrightDriver, PlaywrightFactory
├── api-client/            ← REST Assured base, Jackson models
│
└── tests/
    ├── selenium-tests/    ← Tests requiring Selenium (CDP, Grid, visual)
    ├── playwright-tests/  ← Tests requiring Playwright (network mock, trace)
    ├── api-tests/         ← Pure API contract tests (no UI)
    └── hybrid-tests/      ← API setup + UI verification (tool-agnostic)
```

**Test Selection by Capability:**
```java
// Annotate tests with required capability
@RequiredCapability(Capability.CDP_INTERCEPTION)   // → Selenium only
@RequiredCapability(Capability.NETWORK_MOCK)        // → Playwright preferred
@RequiredCapability(Capability.CROSS_BROWSER_GRID)  // → Selenium Grid

// JUnit 5 condition — skip if driver doesn't have capability
public class CapabilityCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
        String driverType = ConfigReader.get("driverType", "selenium");
        RequiredCapability req = ctx.getRequiredTestMethod()
            .getAnnotation(RequiredCapability.class);
        if (req == null) return ConditionEvaluationResult.enabled("No constraint");
        boolean supported = CapabilityRegistry.isSupported(driverType, req.value());
        return supported
            ? ConditionEvaluationResult.enabled(driverType + " supports " + req.value())
            : ConditionEvaluationResult.disabled(driverType + " does NOT support " + req.value());
    }
}
```

**Unified Reporting:**
All three tool types emit Allure annotations and write to `target/allure-results`. CI merges results into a single report:
```yaml
# CI pipeline (GitHub Actions)
jobs:
  selenium-tests:
    runs-on: ubuntu-latest
    steps:
      - run: mvn test -pl selenium-tests -Dallure.results.directory=allure-results/selenium
  playwright-tests:
    runs-on: ubuntu-latest
    steps:
      - run: mvn test -pl playwright-tests -Dallure.results.directory=allure-results/playwright
  api-tests:
    runs-on: ubuntu-latest
    steps:
      - run: mvn test -pl api-tests -Dallure.results.directory=allure-results/api
  report:
    needs: [selenium-tests, playwright-tests, api-tests]
    steps:
      - run: allure generate allure-results/* --clean -o allure-report
      - uses: actions/upload-artifact@v3
        with: { path: allure-report }
```

**Decision Matrix — When to Use Which Tool:**

| Scenario | Tool |
|---|---|
| Cross-browser regression | Selenium Grid 4 |
| CDP network interception | Selenium 4 / Playwright (both support) |
| Fast smoke tests (single browser) | Playwright (faster startup) |
| Visual regression | Selenium (AShot maturity) |
| API contract tests | REST Assured |
| Trace recording + debugging | Playwright (superior tooling) |
| Legacy IE/Safari testing | Selenium |

---

## AQ4: How do you implement a completely flake-free test suite at scale?

### Question
Your suite runs 800 tests in CI with a 12% flakiness rate — 96 tests fail intermittently. Walk through your systematic approach to identify root causes, fix them, and build processes to prevent new flaky tests from being introduced.

### Answer

**Phase 1 — Measure Before You Fix**

You can't fix what you can't measure. First, build a flakiness dashboard:
```java
// FlakinessTracker — run in CI, records pass/fail per test
// After 10 runs: identify tests that pass AND fail
// Flakiness rate = runs_failed / total_runs * 100
```

Run suite 10× in CI with fixed data (use TestDataFactory, not shared staging). Generate a heat map: tests with >2% flakiness rate are candidates. The 12% suite rate likely comes from 15–20 root-cause classes.

**Phase 2 — Root Cause Taxonomy**

From industry experience, 800-test suite flakiness breaks down roughly as:

| Root Cause | % of Flaky Tests | Fix |
|---|---|---|
| Timing / missing waits | 35% | Replace `Thread.sleep` with `FluentWait` |
| Shared test data | 25% | TestDataFactory per-test isolation |
| Stale element references | 15% | Retry wrapper, re-find on StaleElement |
| Browser/driver version mismatch | 10% | Selenium Manager / pinned versions |
| Environment-specific (CSS render) | 8% | Visual tolerance threshold |
| Test ordering dependencies | 7% | `@TestMethodOrder` or state isolation |

**Phase 3 — Systematic Fixes**

**Timing fixes:**
```java
// BEFORE — flaky
Thread.sleep(2000);
driver.findElement(By.id("result")).click();

// AFTER — reliable
new AppWait(driver).untilClickable(By.id("result")).click();
```

**Data isolation:**
```java
// BEFORE — shared staging user (other tests modify this user's state)
driver.findElement(email).sendKeys("testuser@example.com");

// AFTER — isolated user created fresh per test
TestUser user = factory.createUser(UserRole.CUSTOMER);
new BrowserStorageUtils(driver()).injectJwtToken(factory.getAuthToken(user));
```

**StaleElement elimination:**
```java
// Re-find strategy in AppWait — already built into wait utilities
// Ensures every element access re-finds from DOM
```

**Phase 4 — Quarantine, Don't Delete**

Never delete a flaky test — it may be catching a real bug. Quarantine it:
```java
@Quarantine(reason = "Flaky for 3 sprints — TC-4421",
            since = "2026-01-15",
            owner = "checkout-team")
```
Quarantined tests run in a nightly "flaky test rehab" pipeline, not in the PR gate.

**Phase 5 — Prevention (Shift Left)**

New test PR requirements:
1. Must use `BaseTest` — no raw `WebDriver`
2. Must use `TestDataFactory` — no shared credentials
3. No `Thread.sleep` (enforced by ArchUnit in CI)
4. New test must pass 5× consecutively in the PR pipeline before merge
5. Flakiness budget per team: each team owns their flakiness rate, reported weekly

**Phase 6 — Metrics Targets**

| Metric | Current | Target (Month 1) | Target (Quarter) |
|---|---|---|---|
| Suite flakiness rate | 12% | 5% | <2% |
| Quarantined tests | 0 | 20 | 5 (most fixed) |
| Mean time to green | 65 min | 45 min | <20 min |
| Retry rate | unknown | tracked | <1% |

---

## AQ5: Design a CI/CD pipeline that provides fast feedback for a team deploying to production 10× per day

### Question
Design a complete CI/CD pipeline with Selenium tests for a team deploying 10 times per day. Define which tests run at each stage, how long each stage can take, how failures are handled, and how to avoid blocking deployments on flaky tests.

### Answer

**Pipeline Stages:**

```
Commit → [Stage 1: Build + Unit] → [Stage 2: Smoke] → [Stage 3: PR Regression]
                                                              ↓
                                            Merge → [Stage 4: Pre-prod Integration]
                                                              ↓
                                            Deploy → [Stage 5: Production Smoke]
```

**Stage 1: Build + Unit Tests (< 3 min)**
- Unit tests (JUnit 5, no browser)
- Static analysis (SonarQube gate)
- Dependency vulnerability scan
- **Failure: blocks immediately**

**Stage 2: Smoke Suite (< 8 min)**
- 20–30 critical path tests: login, search, add-to-cart, checkout happy path
- Runs in parallel: 8 workers × 4 tests = ~2 min execution, 8 min total with startup
- Zero tolerance: 1 failure blocks the PR
- **Failure: blocks PR merge**

**Stage 3: PR Full Regression (< 20 min, non-blocking on PR)**
- 300 regression tests: full suite minus known-slow tests
- Runs async — doesn't block PR merge for fast teams
- Results posted as PR comment with Allure link
- **Failure: creates JIRA ticket, alerts test owner, does NOT block deploy**

**Stage 4: Pre-prod Integration (< 15 min, blocks deploy)**
- Runs against pre-prod environment after merge, before production deploy
- Full smoke + affected-area tests (determined by changed files)
- API contract tests
- Performance budget checks
- **Failure: blocks production deploy, sends Slack alert to on-call**

**Stage 5: Production Smoke (< 5 min, post-deploy)**
- 10 critical paths against production
- Uses production-safe test accounts (non-destructive: read-only + test payment gateway)
- **Failure: triggers automatic rollback via Spinnaker/Argo CD**

**Fast Feedback Optimizations:**
1. **Test Impact Analysis** — only run tests affected by changed files (Stage 3); reduces from 300 to ~60 tests per PR
2. **Fail-fast ordering** — historically failing tests run first (TestOrchestrator)
3. **Parallel execution** — 20 Grid nodes for Stage 4
4. **JWT auth bypass** — eliminates UI login from every test; saves 30 seconds per test in suites of 300 = 150 minutes saved

**Flaky Test Handling in Pipeline:**
```yaml
# Stage 2 Smoke — zero tolerance (blocking)
retry-count: 0
quarantine-enabled: true  # quarantined tests skipped automatically

# Stage 3 Regression — non-blocking, 1 retry allowed
retry-count: 1
report-flaky-tests: true
block-on-failure: false
```

**Total per-deploy pipeline time: 3 + 8 + 15 = 26 minutes** (from commit to production). With 10 deploys/day, comfortable for 2-hour release cadence.

---

## AQ6: How would you architect test data management for a suite of 500+ tests running in parallel?

### Question
Describe a complete test data management strategy for 500+ Selenium tests running 20 at a time in parallel: data creation, isolation, reuse patterns, cleanup, and handling of dependent data (users that own orders that have invoices).

### Answer

**Core Principle: Test Data Ownership**

Each test thread owns its data for the duration of the test. No test reads data created by another test. No global mutable state.

**Strategy 1: API-First Data Creation (preferred)**
```java
// TestDataFactory — creates via REST API, registers ThreadLocal cleanup
TestUser     user    = factory.createUser(UserRole.CUSTOMER);
TestProduct  product = factory.createProduct("SKU-001", BigDecimal.TEN);
TestOrder    order   = factory.createOrder(user, product);
// cleanup registered: delete in reverse order (LIFO): order→product→user
```

**Strategy 2: Database Seeds for Read-Only Reference Data**
Some data is expensive to create and not mutated by tests (product catalog, pricing tiers, country codes). This lives in a shared seed database populated once at test run start:
```java
@BeforeSuite
void seedReferenceData() {
    DatabaseHelper db = new DatabaseHelper();
    // Idempotent: only insert if not exists
    db.execute("INSERT INTO product_categories (id, name) VALUES (1, 'Electronics') " +
               "ON CONFLICT (id) DO NOTHING");
}
```

**Strategy 3: Builder Pattern for Variants**
```java
TestUser premiumUser = UserBuilder.defaults(UserRole.CUSTOMER)
    .withSubscription("PREMIUM")
    .withAddress("123 Main St", "US")
    .buildVia(factory);
```

**Cleanup Architecture:**
```
Test Start
    │
    ▼
factory.createUser()     ─── registers cleanup: deleteUser(id) → stack
factory.createProduct()  ─── registers cleanup: deleteProduct(id) → stack
factory.createOrder()    ─── registers cleanup: cancelOrder(id) → stack
    │
    ▼ (test runs)
    │
    ▼
TestDataFactory.runCleanups()    ← @AfterEach
    pop: cancelOrder  (newest → first)
    pop: deleteProduct
    pop: deleteUser   (oldest → last)
    │
    ▼
cleanup accepts HTTP 404 — entity may already be gone (test deleted it)
```

**Strategy 4: Long-lived Test User Pool for Slow-Creating Resources**
Some resources take 5+ seconds to create (user with KYC verification, subscription setup). Pre-create a pool of 50 such users before the suite starts. Tests check out a user from the pool and return it after test (pool pattern):
```java
public class TestUserPool {
    private final BlockingQueue<TestUser> available;

    public TestUser checkout(UserRole role) throws InterruptedException {
        TestUser user = available.poll(10, TimeUnit.SECONDS);
        if (user == null) throw new IllegalStateException("No users available in pool");
        return user;
    }

    public void returnToPool(TestUser user) {
        resetUser(user); // API call: clear cart, reset address
        available.offer(user);
    }
}
```

**Strategy 5: Unique Identifiers Prevent Collisions**
```java
// Every generated entity gets UUID suffix — no name collisions across parallel threads
String email = "test+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
String sku   = "TEST-SKU-" + System.nanoTime();
```

**Handling Dependent Data:**
Cascade rules: when an order is cancelled, its line items remain but are marked inactive. When a user is deleted, orders soft-delete. Test cleanup accounts for this:
```java
// Cleanup in correct dependency order:
// 1. Cancel order (removes active state)
// 2. Delete invoice (FK constraint: invoice→order)
// 3. Archive/delete order
// 4. Delete product (if test-created)
// 5. Delete user (cascade soft-deletes remaining)
```

**Isolation Guarantee:**
20 parallel tests × average 10 entities each = 200 entities in flight at any time. Unique email/SKU suffixes ensure zero cross-test contamination. ThreadLocal cleanup stack ensures each thread cleans exactly its own data.

---

## AQ7: Design an alerting and observability system for a Selenium test suite in production CI

### Question
Describe how to build observability into your Selenium framework: metrics collection, alerting on failure patterns, distributed tracing through browser sessions, and dashboards for both engineers and management.

### Answer

**Observability Pillars for Test Infrastructure:**

**1. Metrics (What is happening?)**

Key metrics to collect:

| Metric | Collection Method | Alert Threshold |
|---|---|---|
| Test pass rate (per suite) | Allure → custom exporter → Prometheus | < 95% |
| Flakiness rate per test | TestOrchestrator history | > 5% over 7 days |
| Suite duration (p50, p95) | Maven Surefire timing | p95 > SLA |
| Grid session queue depth | Grid `/status` API scrape | > 20 pending |
| Session creation latency | Grid OpenTelemetry | p99 > 10s |
| Browser crash rate | WebDriverException count | > 2% of sessions |

```java
// Prometheus metrics pushed from TestExecutionListener
public class MetricsListener implements ITestListener {
    private static final Counter testTotal = Counter.build()
        .name("selenium_tests_total")
        .labelNames("suite", "browser", "result")
        .register();
    private static final Histogram testDuration = Histogram.build()
        .name("selenium_test_duration_seconds")
        .labelNames("suite", "class")
        .buckets(5, 10, 30, 60, 120, 300)
        .register();

    @Override
    public void onTestSuccess(ITestResult result) {
        testTotal.labels(getSuite(), getBrowser(), "passed").inc();
        testDuration.labels(getSuite(), result.getTestClass().getName())
            .observe(result.getEndMillis() - result.getStartMillis() / 1000.0);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        testTotal.labels(getSuite(), getBrowser(), "failed").inc();
    }
}
```

**2. Logging (Why did it happen?)**

Structured logging with MDC (Mapped Diagnostic Context):
```java
// In DriverFactory.initDriver():
MDC.put("testMethod", getCurrentTestName());
MDC.put("browser",    browserName);
MDC.put("thread",     Thread.currentThread().getName());
MDC.put("sessionId",  ((RemoteWebDriver)driver).getSessionId().toString());
// Every log line now carries these fields → queryable in Kibana/CloudWatch
```

Log format (JSON for Kibana/Splunk):
```json
{"level":"ERROR","testMethod":"checkoutTest","browser":"chrome",
 "sessionId":"abc123","message":"TimeoutException: visible button not found",
 "locator":"By.cssSelector: [data-testid='pay-button']",
 "url":"https://staging.example.com/checkout","timestamp":"2026-04-30T14:22:00Z"}
```

**3. Distributed Tracing (Where in the stack did it fail?)**

Grid 4 natively emits OpenTelemetry spans. Extend into test code:
```java
// Span per test method → child spans for page actions
Span testSpan = tracer.spanBuilder(testName).startSpan();
try (Scope scope = testSpan.makeCurrent()) {
    // Each page object action creates a child span
    Span loginSpan = tracer.spanBuilder("LoginPage.loginAs").startSpan();
    // ...
}
// Trace view in Jaeger: test failed in LoginPage.loginAs → API call to /auth → 500 error
```

**4. Dashboards**

Engineer Dashboard (Grafana):
- Real-time: active sessions, pass/fail rate last 30 min, queue depth
- Test-level: flakiness trend per test, slowest 20 tests, error type distribution

Management Dashboard (Allure TestOps):
- Pass rate trend (weekly), coverage by feature area, time to first failure

**5. Alerting Rules (PagerDuty/Slack)**
```yaml
# Prometheus Alertmanager rules
- alert: HighTestFlakiness
  expr: rate(selenium_tests_total{result="retried"}[1h]) / rate(selenium_tests_total[1h]) > 0.05
  for: 10m
  labels: { severity: warning }
  annotations:
    summary: "Test flakiness > 5% in last hour — investigate grid/environment stability"

- alert: SuiteBlockedOnGrid
  expr: selenium_grid_pending_sessions > 30
  for: 5m
  annotations:
    summary: "Grid queue > 30 — test suite may be stalled"

- alert: CriticalSuiteFailing
  expr: selenium_smoke_suite_pass_rate < 0.8
  for: 2m
  annotations:
    summary: "Smoke suite pass rate < 80% — deploy blocked"
    runbook: "https://wiki/runbooks/selenium-smoke-failure"
```

**6. Session Video Recording for Failures**
Selenoid (Docker Grid) records video per session. On failure, CI attaches video URL to the Allure report and Jira ticket. Engineers watch the 2-minute video to understand failure without reproducing locally.

---

## AQ8: How do you implement a test framework that supports mobile web testing in addition to desktop browsers?

### Question
Extend your existing Selenium framework to support mobile web testing (Chrome on Android, Safari on iOS) via Appium, while sharing page objects, utilities, and reporting infrastructure with desktop tests. Address capability management, viewport handling, and touch actions.

### Answer

**Architecture: Unified Driver Abstraction with Platform Branching**

The key design decision: page objects should not change for mobile. Layout changes, but functionality doesn't. Use the same `LoginPage`, `CheckoutPage` etc — but with a different driver underneath.

**Capability Management:**
```java
public class MobileDriverFactory {
    public static WebDriver createAndroidDriver(String deviceName) {
        UiAutomator2Options options = new UiAutomator2Options()
            .setDeviceName(deviceName)
            .setPlatformVersion("14")
            .setBrowserName("Chrome")
            .setChromedriverExecutable("/usr/local/bin/chromedriver")
            .setNewCommandTimeout(Duration.ofSeconds(90))
            // Mobile-specific
            .setNativeWebTap(true)
            .setAutoGrantPermissions(true)
            .setCapability("appium:chromeOptions", Map.of(
                "args", List.of("--disable-translate")
            ));

        try {
            return new RemoteWebDriver(new URL(
                ConfigReader.get("appiumUrl", "http://localhost:4723")), options);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Appium URL", e);
        }
    }

    public static WebDriver createIosDriver(String deviceName) {
        XCUITestOptions options = new XCUITestOptions()
            .setDeviceName(deviceName)
            .setPlatformVersion("17")
            .setBrowserName("Safari")
            .setNewCommandTimeout(Duration.ofSeconds(90))
            .setWdaLocalPort(8100)
            .setIncludeSafariInWebviews(true);

        try {
            return new RemoteWebDriver(new URL(
                ConfigReader.get("appiumUrl", "http://localhost:4723")), options);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Appium URL", e);
        }
    }
}
```

**Touch Actions Abstraction:**
```java
public class TouchHelper {
    private final WebDriver driver;

    // Selenium 4 W3C touch — works via PointerInput (no Appium dependency)
    public void tap(By locator) {
        WebElement el = new AppWait(driver).untilVisible(locator);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Point center = getCenter(el);
        Sequence tap = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO,
                PointerInput.Origin.viewport(), center.x, center.y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(new Pause(finger, Duration.ofMillis(50)))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        ((RemoteWebDriver) driver).perform(List.of(tap));
    }

    public void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int startX = size.width / 2;
        int startY = (int)(size.height * 0.8);
        int endY   = (int)(size.height * 0.2);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO,
                PointerInput.Origin.viewport(), startX, startY))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(Duration.ofMillis(600),
                PointerInput.Origin.viewport(), startX, endY))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        ((RemoteWebDriver) driver).perform(List.of(swipe));
    }

    public void pinchToZoom(double scale) {
        // Two-finger gesture via two PointerInput sequences
        // scale > 1 → zoom in; scale < 1 → zoom out
        // (implementation uses two concurrent Sequence objects)
    }

    private Point getCenter(WebElement el) {
        Point loc = el.getLocation();
        Dimension size = el.getSize();
        return new Point(loc.x + size.width / 2, loc.y + size.height / 2);
    }
}
```

**Viewport-Aware Page Objects:**
```java
public class LoginPage extends BasePage {
    // Locators same — responsive design uses same data-testid on mobile and desktop
    private static final By EMAIL    = By.cssSelector("[data-testid='email-input']");
    private static final By PASSWORD = By.cssSelector("[data-testid='password-input']");
    private static final By SUBMIT   = By.cssSelector("[data-testid='login-submit']");

    // Mobile: hamburger menu must be opened first to access nav
    private static final By HAMBURGER = By.cssSelector("[data-testid='menu-toggle']");

    public LoginPage(WebDriver driver) { super(driver); }

    public DashboardPage loginAs(String email, String password) {
        // Mobile-specific: ensure login form visible (may be behind nav)
        if (isMobile() && isDisplayed(HAMBURGER)) {
            click(HAMBURGER);
        }
        type(EMAIL, email);
        type(PASSWORD, password);
        click(SUBMIT);
        return new DashboardPage(driver);
    }

    private boolean isMobile() {
        Dimension size = driver.manage().window().getSize();
        return size.width < 768;
    }
}
```

**Test Configuration for Mobile:**
```properties
# mobile-android.properties
platform=android
appiumUrl=http://appium-server:4723
deviceName=Pixel_7_API_34
browser=chrome
baseUrl=https://m.staging.example.com
```

**Shared Reporting:**
Mobile and desktop tests write to the same Allure results directory. Allure tags tell them apart:
```java
// In BaseTest for mobile
Allure.label("platform", "mobile");
Allure.label("device",   ConfigReader.get("deviceName", "unknown"));
```

**BrowserStack / Sauce Labs for Real Devices:**
```java
// Replace local Appium URL with cloud provider
// BrowserStack capabilities extend UiAutomator2Options:
options.setCapability("bstack:options", Map.of(
    "deviceName", "Samsung Galaxy S23",
    "osVersion",  "13.0",
    "sessionName", testName,
    "buildName",  System.getProperty("BUILD_NUMBER", "local")
));
```

---

## AQ9: Walk through your approach to maintaining a 2000-test Selenium suite over 3 years

### Question
You inherit a 3-year-old, 2000-test Selenium suite: Selenium 3.x, Java 8, JUnit 4, outdated page objects, no CDP, some tests using `Thread.sleep`. Design a migration plan to modernize without breaking CI.

### Answer

**Phase 0: Before Touching Anything — Understand the Landscape (Week 1)**

Run the suite 5× and measure:
- How many tests pass consistently?
- How many are flaky (pass and fail)?
- How many always fail (broken tests)?
- What is the runtime breakdown (p50, p99 per test)?

This creates a baseline. Every migration decision must not make these numbers worse.

**Phase 1: Infrastructure Upgrade Without Code Changes (Weeks 2–4)**

Upgrade without touching test code:

```xml
<!-- Step 1: Selenium 3 → Selenium 4 (backward compatible API) -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.20.0</version>
</dependency>
<!-- selenium-support, selenium-chrome-driver automatically updated -->
```

Most Selenium 3 code runs unchanged on Selenium 4. Key deprecations to address:
- `DesiredCapabilities` → `ChromeOptions` (log warnings, fix incrementally)
- `driver.findElement(By.id())` still works unchanged
- `driver.manage().timeouts()` API changed — fix as encountered

**Phase 2: Java 8 → Java 17 (Weeks 5–8)**

Change POM, fix compilation errors:
```xml
<java.version>17</java.version>
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>
```

Common Java 8→17 issues in test code:
- `sun.misc.BASE64Encoder` → `java.util.Base64` (use ArchUnit to detect)
- `new Integer(5)` → `Integer.valueOf(5)` (deprecation)
- Module path issues with reflection — add `--add-opens` in Surefire config

**Phase 3: JUnit 4 → JUnit 5 (Weeks 9–12)**

Use the JUnit Vintage Engine for gradual migration:
```xml
<!-- Run old JUnit 4 tests unchanged -->
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
</dependency>
```

Migrate test classes team by team: `@Before` → `@BeforeEach`, `@Test(expected=)` → `assertThrows`, `@Rule` → Extensions. PR per class, CI gate ensures no regressions.

**Phase 4: Remove Thread.sleep (Weeks 13–16)**

ArchUnit scan finds all `Thread.sleep` calls:
```java
@Test
void noThreadSleepInTests() {
    noClasses().should()
        .callMethod(Thread.class, "sleep", long.class)
        .check(new ClassFileImporter().importPackages("com.example.tests"));
}
```

Automated codemod using OpenRewrite:
```xml
<recipe>org.openrewrite.java.RemoveMethodInvocations</recipe>
<!-- Then manual review: replace each Thread.sleep with AppWait -->
```

**Phase 5: Introduce framework-core as shared library (Weeks 17–20)**

Factor out `DriverFactory`, `AppWait`, `BasePage`, `AllureExtension` into `framework-core:1.0.0`. Migrate one test class at a time to extend the new base.

**Phase 6: CDP Adoption for Flaky Tests (Weeks 21–24)**

Identify tests using network mocking via proxy (BrowserMob) — replace with CDP `Fetch.enable`. Faster, no proxy port conflicts in parallel execution.

**Migration Guardrails:**
1. All changes behind `main` branch PRs — CI gate runs full suite
2. No migration PR accepted if flakiness rate increases
3. Migration progress tracked: `{tests migrated} / 2000` displayed on team dashboard
4. Each phase has a rollback plan — Java 8 and Java 17 branches coexist for 4 weeks
5. Deprecation warnings treated as errors from Phase 3 onward

**Timeline: 6 months to full modernization** for 2000 tests with a team of 2 dedicated engineers. Key success factor: never break CI, prove improvement at each phase.

---

## AQ10: How do you handle test environment management for a microservices architecture with 20 services?

### Question
Your application has 20 microservices. Selenium tests need a running environment. Describe strategies for environment management: full environment, service virtualization, consumer-driven contract testing, and when to use each.

### Answer

**The Core Problem:**
20 microservices × 3 instances × 2 environments = 60 running processes for a full environment. This is expensive, slow to provision, and brittle (any service bug breaks all E2E tests).

**Strategy Selection Matrix:**

| Test Type | Services Needed | Approach | Speed |
|---|---|---|---|
| Unit (logic only) | None | Mock everything | <1s |
| Component (1 service UI) | 1 + stubs | WireMock stubs | <5s |
| Integration (flow across 2–3 services) | 2–3 real | Docker Compose subset | <2 min |
| E2E Critical Path | All 20 | Full staging environment | Slow — gate before prod |
| E2E Regression | All 20 | Full staging environment | Nightly only |

**Approach 1: Service Virtualization with WireMock for Selenium Tests**

For Selenium E2E tests that primarily test the UI/frontend, stub the backend:
```java
// WireMockServer started in @BeforeSuite, proxies matching requests
WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig()
    .port(8080));
wireMock.start();

wireMock.stubFor(get(urlPathEqualTo("/api/v1/products"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBodyFile("stubs/products-list.json")));

// Selenium tests run against frontend pointing to WireMock backend
// Fast, no real services needed, no network flakiness
```

**Approach 2: Docker Compose Subset for Integration Tests**

Tests needing real service behavior use a Docker Compose profile:
```yaml
# docker-compose.integration.yml — only services needed for checkout tests
services:
  frontend:       { image: "app/frontend:${VERSION}", ports: ["3000:3000"] }
  checkout-api:   { image: "app/checkout:${VERSION}", ports: ["8081:8080"] }
  payment-api:    { image: "app/payment:${VERSION}", ports: ["8082:8080"] }
  user-api:       { image: "app/user:${VERSION}", ports: ["8083:8080"] }
  postgres:       { image: "postgres:15", environment: {...} }
  # Order service, catalog service, etc. → stubbed by WireMock
```

CI pipeline:
```bash
docker-compose -f docker-compose.integration.yml up -d
mvn test -Dgroups=integration -DbaseUrl=http://localhost:3000
docker-compose -f docker-compose.integration.yml down
```

**Approach 3: Consumer-Driven Contract Testing (Pact) Prevents E2E Over-Testing**

The biggest mistake: using Selenium E2E to test API contracts. Use Pact instead:
```java
// Consumer (frontend) defines contract:
@Pact(consumer = "frontend", provider = "checkout-api")
public RequestResponsePact createProductsPact(PactDslWithProvider builder) {
    return builder
        .given("products exist")
        .uponReceiving("GET /api/v1/products")
        .path("/api/v1/products")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(new PactDslJsonArray()
            .object().stringType("id").numberType("price").closeObject())
        .toPact();
}
```

Pact tests run in CI in < 30 seconds. Selenium tests can then assume the API works and focus purely on UI behavior.

**Approach 4: Shared Staging vs. Personal/Feature Environments**

| Model | Cost | Speed | Isolation |
|---|---|---|---|
| Shared staging (1 environment) | Low | N/A (always on) | None — parallel deploys conflict |
| Feature environments (per PR) | High | ~3 min provision | Perfect |
| Canary environments (30% traffic) | Free | N/A | Partial |

For 20 services, feature environments via Kubernetes namespaces are practical:
```yaml
# Helm chart: values-feature-env.yaml
global:
  namespace: "pr-${PR_NUMBER}"
  tag: "${COMMIT_SHA}"
# All 20 services deployed in namespace
# DNS: checkout.pr-1234.internal
# Destroyed when PR merges
```

**Recommended Stack for 20-Service Architecture:**
1. Unit + Component tests: WireMock stubs (no real services)
2. Contract tests: Pact (fast, catches API regressions before E2E)
3. Integration tests: Docker Compose subset (5 services max per test scenario)
4. E2E Smoke: Feature environment via Kubernetes + Helm (5 critical paths, < 10 min)
5. E2E Full Regression: Shared staging, nightly only (not in PR gate)

The Selenium suite in CI only tests what Selenium is uniquely good at: the UI itself, not the backend API contracts.

---

## AQ11: How do you design a framework for testing internationalization (i18n) and localization (l10n)?

### Question
Your application supports 12 locales (en-US, fr-FR, de-DE, ja-JP, ar-SA, zh-CN, etc.). Design a framework strategy for testing that all locales render correctly, date/number/currency formats are correct, RTL layouts work, and character encoding handles non-Latin scripts.

### Answer

**Scope of i18n/l10n Testing:**

| Layer | What to Test | Tool |
|---|---|---|
| Translation completeness | No missing keys, no placeholder text (`{{key}}`) | Static scan + API |
| Formatting (date, number, currency) | `€1.299,99` vs `$1,299.99` | Selenium assertion |
| RTL layout (Arabic, Hebrew) | Elements don't overlap, text-align correct | Visual regression |
| Character encoding | Japanese/Chinese characters display, not garbled | Selenium getText + regex |
| Locale-specific business rules | Tax calculation differs by region | API + DB |
| SEO (hreflang, canonical) | HTML meta tags present | Selenium + source parse |

**Framework Design:**

**1. Locale Configuration in ConfigReader**
```java
// locale.properties (per-locale override files)
// en-US.properties, fr-FR.properties, ja-JP.properties, ar-SA.properties

public class LocaleConfig {
    private final Locale locale;
    private final Properties strings;

    public LocaleConfig(String localeCode) {
        this.locale  = Locale.forLanguageTag(localeCode);
        this.strings = loadStrings(localeCode);
    }

    public String t(String key) {
        String val = strings.getProperty(key);
        if (val == null) throw new MissingTranslationException(key, locale);
        return val;
    }

    public String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(locale).format(amount);
    }

    public String formatDate(LocalDate date) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(locale).format(date);
    }

    public boolean isRtl() {
        return Set.of("ar", "he", "fa", "ur")
            .contains(locale.getLanguage());
    }
}
```

**2. Locale-Aware Browser Launch**
```java
public WebDriver createLocaleDriver(String localeCode) {
    ChromeOptions options = new ChromeOptions();
    // Accept-Language header — tells app which locale to serve
    options.addArguments("--lang=" + localeCode);
    // Timezone (affects date display)
    String tz = LocaleTimezoneMap.get(localeCode); // "Europe/Paris" for fr-FR
    options.addArguments("--tz=" + tz);
    // For RTL: ensure OS-level RTL support
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("intl.accept_languages", localeCode);
    options.setExperimentalOption("prefs", prefs);
    return new ChromeDriver(options);
}
```

**3. Locale-Driven Assertions in Page Objects**
```java
public class PricingPage extends BasePage {
    private final LocaleConfig locale;

    public PricingPage(WebDriver driver, LocaleConfig locale) {
        super(driver);
        this.locale = locale;
    }

    public void assertProductPrice(String sku, BigDecimal expectedAmount) {
        String displayedPrice = getText(
            By.cssSelector("[data-testid='price-" + sku + "']"));
        // Assert the FORMAT is locale-correct, not just the number
        String expectedFormatted = locale.formatCurrency(expectedAmount);
        assertThat(displayedPrice)
            .as("Price format for locale " + locale.locale())
            .isEqualTo(expectedFormatted);
    }

    public void assertNoMissingTranslations() {
        // Detect untranslated placeholder keys in the DOM
        String bodyText = driver.findElement(By.tagName("body")).getText();
        // Common patterns: {{key}}, [MISSING: key], translation_key_name
        assertThat(bodyText)
            .doesNotContain("{{")
            .doesNotContain("[MISSING")
            .doesNotContainPattern("^[a-z]+\\.[a-z_]+\\.[a-z_]+$"); // raw key pattern
    }

    public void assertRtlLayout() {
        if (!locale.isRtl()) return;
        // Body dir attribute must be "rtl"
        String dir = driver.findElement(By.tagName("html"))
            .getDomAttribute("dir");
        assertThat(dir).isEqualTo("rtl");
        // Key elements must have correct text-direction
        String textAlign = (String) ((JavascriptExecutor) driver)
            .executeScript(
                "return window.getComputedStyle(arguments[0]).direction;",
                driver.findElement(By.cssSelector("[data-testid='main-content']")));
        assertThat(textAlign).isEqualTo("rtl");
    }
}
```

**4. Parametrized Locale Test**
```java
@ParameterizedTest(name = "Locale: {0}")
@ValueSource(strings = {"en-US", "fr-FR", "de-DE", "ja-JP", "ar-SA", "zh-CN"})
void productPriceDisplaysInCorrectLocaleFormat(String localeCode) {
    LocaleConfig locale = new LocaleConfig(localeCode);
    WebDriver localeDriver = createLocaleDriver(localeCode);
    try {
        localeDriver.get(BASE_URL + "?locale=" + localeCode);
        PricingPage page = new PricingPage(localeDriver, locale);
        page.assertProductPrice("LAPTOP-001", BigDecimal.valueOf(1299.99));
        page.assertNoMissingTranslations();
        if (locale.isRtl()) page.assertRtlLayout();
    } finally {
        localeDriver.quit();
    }
}
```

**5. Visual Regression for RTL**
```java
new VisualTestHelper(driver)
    .withTolerance(1.0) // slightly higher tolerance for font rendering differences
    .assertMatchesBaseline("checkout-" + localeCode);
```

**6. Character Encoding Assertions**
```java
public void assertJapaneseCharactersRendered() {
    String productName = getText(By.cssSelector("[data-testid='product-name']"));
    // Verify contains at least one CJK character (not question marks or squares)
    boolean hasCjk = productName.chars()
        .anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA);
    assertThat(hasCjk).as("Japanese product name should contain CJK characters").isTrue();
}
```

**Key Architectural Decisions:**
- `Accept-Language` header via Chrome arg — tells the SPA which locale bundle to load without a separate URL per locale
- `LocaleConfig.t(key)` in assertions — test code references the same translation key as the app; if translation changes, test updates automatically
- Visual baseline per locale — captures RTL layout, font differences, and symbol ordering in one assertion
- `--lang` and `--tz` both set — date display depends on both locale AND timezone; missing timezone causes subtle date-off-by-one failures for near-midnight tests

---

## AQ12: How do you test a real-time application (WebSocket, Server-Sent Events, live dashboards)?

### Question
Your application has a real-time trading dashboard that receives price updates via WebSocket at 100ms intervals, a live order status feed via Server-Sent Events, and a notifications panel updated without page refresh. Design a testing strategy.

### Answer

**Challenge:** Selenium has no native WebSocket or SSE support. Tests must interact with the DOM while asynchronous updates arrive continuously.

**Strategy 1: Assert on DOM State After Event**

For WebSocket/SSE, you don't test the protocol — you test the DOM result. When a price update arrives, the DOM updates. Assert the DOM:
```java
public class TradingDashboardPage extends BasePage {
    private static final By BTC_PRICE = By.cssSelector("[data-testid='price-BTC']");
    private static final By PRICE_FLASH= By.cssSelector("[data-testid='price-BTC'].flash-green");

    // Wait for price to update (non-zero, non-stale)
    public BigDecimal waitForPriceUpdate(BigDecimal stalePrice) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
            .withMessage("Price did not update from stale value " + stalePrice)
            .until(d -> {
                try {
                    String text = d.findElement(BTC_PRICE).getText()
                        .replaceAll("[^0-9.]", "");
                    if (text.isBlank()) return null;
                    BigDecimal current = new BigDecimal(text);
                    return current.compareTo(stalePrice) != 0 ? current : null;
                } catch (StaleElementReferenceException e) { return null; }
            });
    }

    // Assert flash animation appeared (price changed visually)
    public void assertPriceFlashed() {
        // CSS class briefly added on price update
        new WebDriverWait(driver, Duration.ofSeconds(5))
            .until(ExpectedConditions.presenceOfElementLocated(PRICE_FLASH));
    }
}
```

**Strategy 2: Inject WebSocket Message via JavaScript**

Bypass network entirely — inject the event directly into the app's WebSocket handler:
```java
public void injectWebSocketMessage(String jsonPayload) {
    // Get the app's WebSocket object from the global scope
    js.executeScript(
        "var ws = window.__websocket || window.tradingSocket;" +
        "if (ws && ws.onmessage) {" +
        "  var evt = new MessageEvent('message', {data: arguments[0]});" +
        "  ws.onmessage(evt);" +
        "} else {" +
        "  console.warn('No WebSocket found on window');" +
        "}",
        jsonPayload
    );
}

// Usage: trigger specific price scenario
page.injectWebSocketMessage("{\"type\":\"PRICE_UPDATE\",\"symbol\":\"BTC\",\"price\":65000.00}");
page.assertPriceDisplayed("BTC", BigDecimal.valueOf(65000.00));
```

**Strategy 3: CDP Network Interception for SSE**

Intercept the SSE endpoint and inject custom event data:
```java
public void interceptSseAndInject(String endpoint, String eventData) {
    DevTools devTools = ((ChromeDriver) driver).getDevTools();
    devTools.createSession();
    devTools.send(Fetch.enable(
        Optional.of(List.of(new RequestPattern(
            Optional.of("*" + endpoint + "*"),
            Optional.empty(),
            Optional.of(RequestStage.RESPONSE)))),
        Optional.of(false)));

    devTools.addListener(Fetch.requestPaused(), event -> {
        if (event.getRequest().getUrl().contains(endpoint)) {
            // Return controlled SSE stream
            String body = "data: " + eventData + "\n\n";
            devTools.send(Fetch.fulfillRequest(
                event.getRequestId(),
                200,
                Optional.of(List.of(
                    new HeaderEntry("Content-Type", "text/event-stream"),
                    new HeaderEntry("Cache-Control", "no-cache"))),
                Optional.empty(),
                Optional.of(Base64.getEncoder().encodeToString(
                    body.getBytes(StandardCharsets.UTF_8))),
                Optional.empty()));
        } else {
            devTools.send(Fetch.continueRequest(event.getRequestId(),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()));
        }
    });
}
```

**Strategy 4: Capture WebSocket Messages via JavaScript Monkey-Patch**

Intercept ALL WebSocket messages received for test assertions:
```java
public void startCapturingWebSocketMessages() {
    js.executeScript(
        "window.__wsMessages = [];" +
        "var OriginalWebSocket = window.WebSocket;" +
        "window.WebSocket = function(url, protocols) {" +
        "  var ws = new OriginalWebSocket(url, protocols);" +
        "  var origOnMessage = ws.onmessage;" +
        "  ws.addEventListener('message', function(evt) {" +
        "    window.__wsMessages.push({data: evt.data, ts: Date.now()});" +
        "    if (window.__wsMessages.length > 100)" +
        "      window.__wsMessages.shift(); // ring buffer" +
        "  });" +
        "  window.__websocket = ws;" +
        "  return ws;" +
        "};"
    );
}

@SuppressWarnings("unchecked")
public List<String> getCapturedMessages() {
    List<Map<String, Object>> raw = (List<Map<String, Object>>)
        js.executeScript("return window.__wsMessages || [];");
    return raw.stream().map(m -> m.get("data").toString()).toList();
}
```

**Test Structure:**
```java
class TradingDashboardTest extends BaseTest {
    @Test
    void priceUpdatesReflectInDashboardWithin500ms() {
        loginAs("TRADER");
        navigateTo("/dashboard");
        TradingDashboardPage dashboard = new TradingDashboardPage(driver());

        BigDecimal initialPrice = dashboard.getCurrentPrice("BTC");
        assertThat(initialPrice).isPositive();

        // Inject price update directly
        dashboard.injectWebSocketMessage(
            "{\"type\":\"PRICE\",\"symbol\":\"BTC\",\"bid\":65432.10,\"ask\":65433.50}");

        // Assert UI updated
        BigDecimal updatedPrice = dashboard.waitForPriceUpdate(initialPrice);
        assertThat(updatedPrice).isEqualByComparingTo(BigDecimal.valueOf(65432.10));
        dashboard.assertPriceFlashed();
    }

    @Test
    void orderStatusUpdateArrivesViaSSE() {
        TestOrder order = factory.createOrder(
            factory.createUser(UserRole.TRADER));
        navigateTo("/orders/" + order.id());

        // SSE will push status update — wait for DOM change
        new AppWait(driver()).waitForCondition(
            CustomExpectedConditions.attributeEquals(
                By.cssSelector("[data-testid='order-status']"),
                "data-status", "FILLED"),
            Duration.ofSeconds(30)
        );
    }
}
```

**Key Decisions:**
- Test DOM result, not the protocol — verifying `WebSocket.send()` was called is a unit test concern; Selenium tests the user-visible outcome
- JS injection for deterministic scenarios — test specific amounts/states without waiting for live market data
- Ring buffer for captured messages — prevents O(N) memory growth in long-running tests
- 500ms assertion timeout for price updates — tighter than default 15s; real-time apps must update within SLA

---

## AQ13: How do you ensure test suite security? What vulnerabilities can your test framework introduce?

### Question
As a Principal SDET, security is part of your remit. What security risks exist in a Selenium test framework, and how do you mitigate them?

### Answer

**Risk 1: Credentials in Code / Version Control**

*Vulnerability:* Hardcoded passwords in test files committed to GitHub → credential leak.

*Mitigation:*
```java
// BAD
driver.findElement(By.id("password")).sendKeys("Admin123!");

// GOOD — environment variable or secret manager
String password = ConfigReader.require("test.admin.password");
// In CI: set as GitHub Secret, injected as env var
// In config: ENC(base64...) — obfuscated, not plaintext
```

ArchUnit enforcement:
```java
@Test
void noHardcodedPasswordsInCode() {
    // Detect obvious patterns
    GreppingCatchClause check = noClasses().should()
        .containAnyMembersThat(member ->
            member.toString().matches(".*[Pp]assword.*=.*\"[^\"]{6,}\".*"));
    // Additionally: GitLeaks in CI pre-commit hook
}
```

**Risk 2: SQL Injection in Test Utilities**

*Vulnerability:* `DatabaseHelper.execute("DELETE FROM orders WHERE id='" + orderId + "'")` — if `orderId` comes from UI, it could be a crafted SQL string.

*Mitigation:* Always parameterized queries. Already enforced in the `DatabaseHelper` implementation (uses `PreparedStatement` with `setObject`):
```java
// ALWAYS — PreparedStatement
ps.setObject(1, orderId); // safe
// NEVER
"WHERE id = '" + orderId + "'" // injection-vulnerable
```

**Risk 3: Test Framework Attacking Production**

*Vulnerability:* `ConfigReader` selects environment based on `-Denv=prod`. An accidental prod run with destructive tests (DELETE, POST /admin/reset) could corrupt production data.

*Mitigation:*
```java
public class EnvironmentGuard {
    @BeforeSuite
    public static void assertNotProduction() {
        String env = ConfigReader.get("env", "staging");
        String baseUrl = ConfigReader.require("baseUrl");

        if ("prod".equalsIgnoreCase(env) ||
                baseUrl.contains("production.example.com") ||
                baseUrl.contains("app.example.com")) {
            throw new ConfigurationException(
                "SAFETY GUARD: Refusing to run destructive tests against production. " +
                "Use -Denv=staging or -Denv=prod-readonly explicitly.");
        }
    }
}
```

**Risk 4: Sensitive Data in Screenshots / Allure Reports**

*Vulnerability:* Allure screenshots capture credit card numbers, PII, tokens on screen. Reports committed to artifact storage accessible by build engineers.

*Mitigation:*
```java
public class SensitiveDataMasker {
    private static final List<By> MASK_BEFORE_SCREENSHOT = List.of(
        By.cssSelector("[data-testid='card-number']"),
        By.cssSelector("[data-testid='cvv']"),
        By.cssSelector("[data-testid='ssn']"),
        By.name("cardNumber")
    );

    public static void maskSensitiveFields(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (By locator : MASK_BEFORE_SCREENSHOT) {
            driver.findElements(locator).forEach(el ->
                js.executeScript(
                    "arguments[0].value = '****'; " +
                    "arguments[0].textContent = '****';", el));
        }
    }
}

// In AllureExtension.testFailed():
SensitiveDataMasker.maskSensitiveFields(driver);
attachScreenshot(driver, "Screenshot at Failure"); // now safe
```

**Risk 5: Test Users with Excessive Privileges**

*Vulnerability:* Test admin user has production-level permissions. If compromised, attacker has full production access using test credentials.

*Mitigation:*
- Test users exist only in non-production environments
- Test admin has `TEST_ADMIN` role with limited scope (can create/delete test data, cannot access financial reports, cannot export customer data)
- Test credentials rotate every 30 days via HashiCorp Vault integration
- All test API calls logged with `X-Test-Run-ID` header for audit trail

**Risk 6: CDP Enabling Browsing Out of Test Scope**

*Vulnerability:* `CdpNetworkInterceptor` can intercept any URL. If test code is injected with malicious input, it could intercept real payment calls.

*Mitigation:*
- Restrict CDP to test-specific domains: assert `GRID_URL` is not production
- `stubs.put()` only accepts URLs from an allowlist in non-production environments
- CDP sessions locked to test session duration — closed in `@AfterEach`

**Risk 7: Dependency Vulnerabilities**

*Mitigation:*
```xml
<!-- OWASP Dependency Check in Maven build -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.0</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <!-- Block build if any dependency has CVSS score ≥ 7 -->
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

Run weekly and on every dependency bump. Test frameworks (`selenium-java`, `rest-assured`, `allure-java`) have historically had vulnerabilities.

**Security Checklist for Framework PRs:**
- [ ] No hardcoded credentials
- [ ] No `PreparedStatement` bypass (parameterized queries only)
- [ ] No production URL references
- [ ] Sensitive fields masked before screenshot
- [ ] New dependencies scanned by OWASP check
- [ ] CDP/network interceptors scoped to non-prod domains

---

## AQ14: How do you handle test suite performance — what makes Selenium tests slow and how do you fix it?

### Question
Your 500-test Selenium suite takes 90 minutes. Your target is 15 minutes. Walk through a systematic approach to identify bottlenecks and reduce execution time by 80%.

### Answer

**Step 1: Profile Before Optimizing**

Never guess at bottlenecks. Instrument first:
```java
// TestExecutionTimer — records actual wall-clock time per test
public class TestTimingListener implements ITestListener {
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    @Override
    public void onTestStart(ITestResult result) {
        startTimes.put(result.getMethod().getQualifiedName(),
            System.currentTimeMillis());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        recordTime(result, "PASS");
    }

    private void recordTime(ITestResult result, String status) {
        String name  = result.getMethod().getQualifiedName();
        long   start = startTimes.getOrDefault(name, 0L);
        long   ms    = System.currentTimeMillis() - start;
        log.info("[TIMING] {} | {} | {}ms", status, name, ms);
        // Export to CSV for analysis
    }
}
```

After one profiling run, you'll typically find the **Pareto pattern**: 20% of tests consume 80% of time.

**Common Root Causes and Fixes:**

**Root Cause 1: Sequential execution (biggest win)**

*Typical saving: 70–80% of total time*

If suite runs sequentially (1 thread), 500 tests × average 10s = 83 minutes. Switch to 20-thread parallel:
```xml
<!-- JUnit 5 -->
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.config.fixed.parallelism=20
<!-- Theoretical: 500 tests × 10s / 20 threads = ~4 min execution -->
<!-- With 5 min overhead: ~9 min total -->
```

*Prerequisite:* Tests must be stateless (TestDataFactory isolation). If tests share state, parallelism causes failures — fix isolation first.

**Root Cause 2: UI Login in Every Test**

*Typical saving: 20–30 seconds per test × 500 = 2.5–4 hours total*

```java
// BAD — 30s login via UI per test
driver.get(BASE_URL + "/login");
new LoginPage(driver).loginAs("user@test.com", "pass");

// GOOD — 0.1s JWT injection per test
driver.get(BASE_URL);
new BrowserStorageUtils(driver).injectJwtToken(token);
driver.navigate().to(BASE_URL + "/dashboard");
```

**Root Cause 3: Large `Thread.sleep` Calls**

*Typical saving: 2–10 seconds per occurrence*

Scan with ArchUnit, replace with `FluentWait`. In a 500-test suite, 50 tests with `Thread.sleep(5000)` = 250 seconds = 4+ minutes wasted.

**Root Cause 4: Over-wide Default Timeout**

```java
// BAD — 30s default wait, most elements appear in <1s
new WebDriverWait(driver, Duration.ofSeconds(30)).until(...);

// GOOD — tuned defaults, longer timeout only where needed
// AppWait default: 10s (covers 99% of elements)
// Critical path timeouts: 30s (payment processing, file upload)
// Fast assertions: 3s (element visible after JS-triggered DOM change)
```

**Root Cause 5: Browser Startup Overhead**

Each test creates a new driver: `ChromeDriver()` = ~2–3 seconds.
500 tests × 2.5s = 21 minutes just starting browsers.

*Fix: Driver reuse within a test class (class-scoped driver):*
```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // JUnit 5
public class ProductSearchTest extends BaseTest {
    // Driver created once for all methods in this class
    @BeforeAll void setUpDriver() { DriverFactory.initDriver("chrome"); }
    @AfterAll  void tearDown()    { DriverFactory.quitDriver(); }

    @BeforeEach void resetState() {
        // Clear storage instead of creating new driver
        new BrowserStorageUtils(driver()).clearAll();
        driver().get(BASE_URL);
    }
}
// 20 tests in class → 1 browser start instead of 20 → saves 47.5 seconds
```

**Root Cause 6: Redundant Navigation**

```java
// BAD — navigates to /login every test (full page load each time)
@BeforeEach void setUp() { driver.get(BASE_URL + "/login"); }

// GOOD — stay on the page from previous test, only re-navigate if URL changed
@BeforeEach void setUp() {
    if (!driver.getCurrentUrl().contains("/dashboard")) {
        new BrowserStorageUtils(driver()).injectJwtToken(token);
        driver.get(BASE_URL + "/dashboard");
    }
}
```

**Root Cause 7: Unnecessary Screenshots**

```java
// BAD — screenshot on every test (even passing ones)
@AfterEach void screenshot() { takeScreenshot(); } // 500 × 0.5s = 250s

// GOOD — only on failure
@Override public void testFailed(ExtensionContext ctx, Throwable cause) {
    takeScreenshot(); // Only runs on failure
}
```

**Root Cause 8: Sequential Data Setup via UI**

```java
// BAD — adds product to cart via UI in @BeforeEach (5 page loads per test)
// GOOD — API call: 100ms
TestOrder order = factory.createOrder(customer, product); // ~200ms total
```

**Optimization Roadmap (target: 90 min → 15 min):**

| Fix | Estimated Saving |
|---|---|
| 20-thread parallelism | 75 min → 15 min (75% reduction) |
| JWT auth bypass | Additional 8 min saved |
| Remove Thread.sleep | Additional 4 min saved |
| Driver reuse (class-scoped) | Additional 5 min saved |
| API data setup | Additional 3 min saved |
| **Total** | **~90 min → <15 min** |

The parallelism change alone achieves the target. All other changes are refinements that also improve reliability.

---

## AQ15: How do you approach testing authentication and authorization in a Selenium framework?

### Question
Your application has SSO (SAML/OAuth2), role-based access control (5 roles), multi-factor authentication, and session timeout behavior. Design a testing strategy for all of these.

### Answer

**Authentication Testing Strategy:**

**1. SSO (SAML/OAuth2) — Don't Test the Protocol, Test the Result**

Testing that SAML assertions parse correctly is a unit test for the identity provider. Selenium's job is to test that after SSO, the user lands on the correct page with the correct identity.
```java
// For staging: configure IdP to accept test assertions (Okta/Auth0 sandbox)
// Bypass SSO with pre-created JWT (fastest path for most tests)
public void loginWithSso(String email) {
    // Option A: Direct token (fastest, most reliable)
    String token = AuthApiClient.getTokenViaCreds(email, TEST_PASSWORD);
    new BrowserStorageUtils(driver).injectJwtToken(token);

    // Option B: Full SSO flow (slower, used for SSO-specific tests only)
    driver.get(BASE_URL + "/auth/saml/login");
    // Redirect to test IdP
    new IdpPage(driver).loginAs(email, TEST_PASSWORD);
    // Redirect back to app
    new AppWait(driver).untilDocumentReady();
    assertThat(driver.getCurrentUrl()).contains("/dashboard");
}
```

**2. RBAC (5 Roles) — Matrix Testing**

Every protected page/action × every role = authorization matrix:
```java
@ParameterizedTest
@MethodSource("provideAuthorizationScenarios")
void authorizationMatrix(String role, String url, int expectedStatus,
                          boolean expectContent) {
    TestUser user  = factory.createUser(UserRole.valueOf(role));
    String   token = factory.getAuthToken(user);
    new BrowserStorageUtils(driver()).injectJwtToken(token);
    driver().get(BASE_URL + url);
    new AppWait(driver()).untilDocumentReady();

    if (expectContent) {
        assertThat(driver().getCurrentUrl()).doesNotContain("/403");
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='page-content']")).isDisplayed()).isTrue();
    } else {
        // Should redirect to 403 or login
        assertThat(driver().getCurrentUrl())
            .matches(".*/403.*|.*/login.*|.*/unauthorized.*");
    }
}

static Stream<Arguments> provideAuthorizationScenarios() {
    return Stream.of(
        // Page                  Role         Accessible?
        Arguments.of("/admin",  "CUSTOMER",   false),
        Arguments.of("/admin",  "ADMIN",       true),
        Arguments.of("/reports","ANALYST",     true),
        Arguments.of("/reports","CUSTOMER",   false),
        Arguments.of("/orders", "CUSTOMER",    true),
        Arguments.of("/orders", "ANONYMOUS",  false)
        // ... 50+ combinations
    );
}
```

**3. MFA Testing**

```java
public class MfaHelper {
    // For testing: use TOTP with known secret seed
    // Test IdP configured with fixed TOTP secret per test user
    public String generateTotp(String base32Secret) {
        // TOTP: 6-digit code from HMAC-SHA1(secret, timestep)
        byte[] key = BASE32.decode(base32Secret);
        long   timeStep = Instant.now().getEpochSecond() / 30;
        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
        byte[] hash = hmacSha1(key, data);
        int offset  = hash[hash.length - 1] & 0x0F;
        int code    = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset+1] & 0xFF) << 16)
                    | ((hash[offset+2] & 0xFF) << 8)
                    |  (hash[offset+3] & 0xFF);
        return String.format("%06d", code % 1_000_000);
    }
}

@Test
void mfaLoginSucceedsWithValidTotp() {
    navigateTo("/login");
    new LoginPage(driver())
        .enterCredentials("mfa-user@test.com", TEST_PASSWORD)
        .submitCredentials();

    // MFA challenge page
    String totp = new MfaHelper()
        .generateTotp(ConfigReader.require("test.mfa.secret"));
    new MfaPage(driver()).enterCode(totp).submit();

    assertThat(driver().getCurrentUrl()).contains("/dashboard");
}

@Test
void mfaLoginFailsWithExpiredCode() {
    // Use a code from 2 time-steps ago (expired)
    // Test that error message is shown
}
```

**4. Session Timeout Testing**

```java
@Test
void sessionExpiresAfterInactivity() {
    loginAs("CUSTOMER");
    navigateTo("/dashboard");

    // Fast-forward time by manipulating localStorage token expiry
    // (rather than waiting real 15 minutes)
    new BrowserStorageUtils(driver()).setLocalStorage(
        "tokenExpiry",
        String.valueOf(Instant.now().minus(Duration.ofMinutes(1)).toEpochMilli())
    );

    // Trigger any API call (e.g., navigate to another protected page)
    navigateTo("/orders");

    // App should detect expired token and redirect
    new AppWait(driver()).untilUrlContains("/login");
    assertThat(driver().findElement(
        By.cssSelector("[data-testid='session-expired-msg']")).isDisplayed())
        .isTrue();
}

@Test
void concurrentSessionPrevented() {
    String token1 = factory.getAuthToken(customer);
    String token2 = factory.getAuthToken(customer); // second login — invalidates token1

    WebDriver browser1 = DriverFactory.createDriver("chrome");
    WebDriver browser2 = DriverFactory.createDriver("chrome");
    try {
        new BrowserStorageUtils(browser1).injectJwtToken(token1);
        new BrowserStorageUtils(browser2).injectJwtToken(token2);

        browser1.get(BASE_URL + "/orders"); // token1 now invalid
        // App should redirect browser1 to login (token invalidated)
        new AppWait(browser1).untilUrlContains("/login");
    } finally {
        browser1.quit();
        browser2.quit();
    }
}
```

**Key Design Decisions:**
- JWT manipulation for session expiry — avoids actual waiting; test executes in 3 seconds not 15 minutes
- TOTP with known seed — deterministic MFA code generation; no dependency on external authenticator app
- Authorization matrix as `@MethodSource` — maintainable table in one method; adding a new role means adding rows, not new test methods
- Separate SSO flow tests from rest of suite — SSO tests run nightly (slow); all other tests use JWT bypass (fast)

---

## AQ16: How do you integrate Selenium tests with a feature flag system?

### Question
Your application uses feature flags (LaunchDarkly/Unleash) to control which features are visible. How do you write Selenium tests that work both when a flag is ON and when it is OFF, and how do you test new features behind flags before they are GA?

### Answer

**Core Problem:**
When a feature is behind a flag, the UI element simply doesn't exist when the flag is off. Writing a test that always expects the element will fail on flag-off deployments.

**Strategy 1: Feature Flag Abstraction in Tests**

```java
public class FeatureFlagClient {
    private final String baseUrl;
    private final String apiKey;

    public boolean isEnabled(String flagKey) {
        return given()
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + apiKey)
        .when()
            .get("/api/flags/{key}", flagKey)
        .then()
            .statusCode(200)
            .extract()
            .path("enabled");
    }

    // Force flag state for a specific test user (test-only API in Unleash/LaunchDarkly)
    public void setFlagForUser(String flagKey, boolean enabled, String userId) {
        given()
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + apiKey)
            .body(Map.of("userId", userId, "enabled", enabled))
        .when()
            .post("/api/flags/{key}/override", flagKey)
        .then()
            .statusCode(200);
        // Register cleanup: remove override after test
        TestDataFactory.register(() -> removeFlagOverride(flagKey, userId));
    }

    private void removeFlagOverride(String flagKey, String userId) {
        given()
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + apiKey)
            .body(Map.of("userId", userId))
        .when()
            .delete("/api/flags/{key}/override", flagKey)
        .then()
            .statusCode(anyOf(is(200), is(204)));
    }
}
```

**Strategy 2: Conditional Tests Based on Flag State**

```java
public class FeatureFlagCondition implements ExecutionCondition {
    private static final FeatureFlagClient flags = new FeatureFlagClient();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
        RequiresFlag annotation = ctx.getElement()
            .map(e -> e.getAnnotation(RequiresFlag.class))
            .orElse(null);
        if (annotation == null) return ConditionEvaluationResult.enabled("No flag required");

        boolean enabled = flags.isEnabled(annotation.value());
        if (annotation.mustBeEnabled() && !enabled)
            return ConditionEvaluationResult.disabled(
                "Skipped: flag '" + annotation.value() + "' is OFF in this environment");
        if (!annotation.mustBeEnabled() && enabled)
            return ConditionEvaluationResult.disabled(
                "Skipped: flag '" + annotation.value() + "' is ON; this test targets flag-OFF behavior");

        return ConditionEvaluationResult.enabled("Flag state matches requirement");
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFlag {
    String value();                 // flag key
    boolean mustBeEnabled() default true;
}

// USAGE
@ExtendWith(FeatureFlagCondition.class)
class CheckoutTest extends BaseTest {

    @Test
    @RequiresFlag("checkout-v2")  // only runs when flag is ON
    void newCheckoutFlowHasExpressOption() {
        navigateTo("/checkout");
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='express-checkout']")).isDisplayed())
            .isTrue();
    }

    @Test
    @RequiresFlag(value = "checkout-v2", mustBeEnabled = false)  // only when flag is OFF
    void legacyCheckoutFlowIsDefault() {
        navigateTo("/checkout");
        assertThat(driver().findElements(
            By.cssSelector("[data-testid='express-checkout']"))).isEmpty();
    }
}
```

**Strategy 3: Force Flag State Per Test**

```java
@Test
void newDashboardWidgetDisplaysCorrectData() {
    TestUser user = factory.createUser(UserRole.CUSTOMER);
    // Force flag ON for only this test user
    featureFlags.setFlagForUser("dashboard-v2-widget", true, user.id());

    new BrowserStorageUtils(driver()).injectJwtToken(factory.getAuthToken(user));
    navigateTo("/dashboard");

    // Widget only visible with flag ON
    assertThat(new AppWait(driver())
        .untilVisible(By.cssSelector("[data-testid='analytics-widget']"))
        .isDisplayed()).isTrue();
}
```

**Strategy 4: Page Object Handles Flag Variants**

```java
public class CheckoutPage extends BasePage {
    private final boolean isV2;

    public CheckoutPage(WebDriver driver) {
        super(driver);
        // Detect which version is active by checking DOM
        this.isV2 = !driver.findElements(
            By.cssSelector("[data-testid='express-checkout']")).isEmpty();
    }

    public CheckoutPage fillCard(String cardNumber) {
        if (isV2) {
            // New checkout: single combined field
            type(By.cssSelector("[data-testid='card-field']"), cardNumber);
        } else {
            // Legacy: separate fields
            type(By.cssSelector("[data-testid='card-number']"), cardNumber);
        }
        return this;
    }

    public boolean isExpressCheckoutAvailable() { return isV2; }
}
```

**Key Decisions:**
- Flag state checked at test JVM startup, not per test — avoids 500 API calls; flags cached for the run
- Per-user overrides with cleanup — flag change is isolated to the test user; no cross-test contamination
- `@RequiresFlag` annotation — explicitly documents which feature flag gates the behavior under test; visible in code review

---

## AQ17: How do you build a self-service test infrastructure where developers can run the full Selenium suite locally without special setup?

### Question
Today it takes engineers 2 hours to set up the Selenium test environment locally. Design a developer experience where `mvn test` "just works" on a clean machine with no prerequisites beyond Java and Maven.

### Answer

**Goal: Zero-friction local test execution**

`git clone → mvn test` should work. No WebDriver installation, no browser version matching, no environment variables for test accounts.

**Component 1: Selenium Manager (Zero Driver Setup)**

Selenium 4.6+ includes Selenium Manager — automatically downloads the correct ChromeDriver/GeckoDriver for the installed browser. Zero configuration needed:
```java
// This "just works" — Selenium Manager handles driver download
WebDriver driver = new ChromeDriver();
// No WebDriverManager dependency needed in Selenium 4.6+
```

For older Selenium or additional browsers, WebDriverManager provides the same:
```xml
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>
```

**Component 2: Dockerized Test Environment via Testcontainers**

Pull the application stack automatically on `mvn test`:
```java
@BeforeSuite
public static void startTestEnvironment() {
    if (!"true".equals(System.getProperty("useExternalEnv"))) {
        // Automatically start the application stack in Docker
        DockerComposeContainer<?> compose = new DockerComposeContainer<>(
            new File("src/test/resources/docker/docker-compose.test.yml"))
            .withExposedService("frontend", 3000,
                Wait.forHttp("/health").withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("api", 8080,
                Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("postgres", 5432,
                Wait.forListeningPort());
        compose.start();

        // Override ConfigReader with dynamic ports
        System.setProperty("baseUrl",
            "http://localhost:" + compose.getServicePort("frontend", 3000));
        System.setProperty("apiBaseUrl",
            "http://localhost:" + compose.getServicePort("api", 8080));
        System.setProperty("db.url",
            "jdbc:postgresql://localhost:" +
            compose.getServicePort("postgres", 5432) + "/testdb");

        // Register shutdown
        TestDataFactory.register(compose::stop);
    }
}
```

**Component 3: Test Account Auto-Provisioning**

On first run, if test accounts don't exist, create them:
```java
@BeforeSuite(dependsOnMethods = "startTestEnvironment")
public static void provisionTestAccounts() {
    TestDataFactory factory = new TestDataFactory(getBootstrapToken());
    // Idempotent: create only if missing
    AuthTokenCache.provisionIfMissing("ADMIN",
        "test-admin@local.test", "LocalTestAdmin1!");
    AuthTokenCache.provisionIfMissing("CUSTOMER",
        "test-customer@local.test", "LocalTestCustomer1!");
    AuthTokenCache.provisionIfMissing("ANALYST",
        "test-analyst@local.test", "LocalTestAnalyst1!");
    // Tokens cached for the run
}
```

**Component 4: Headless by Default Locally**

```java
private static ChromeOptions localChrome() {
    ChromeOptions opts = new ChromeOptions();
    // Headless unless developer explicitly wants UI (-DheadlessEnabled=false)
    boolean headless = !"false".equals(System.getProperty("headlessEnabled", "true"));
    if (headless) {
        opts.addArguments("--headless=new", "--window-size=1920,1080");
    }
    return opts;
}
```

**Component 5: Developer Makefile for Common Commands**
```makefile
# Makefile — in project root
test-smoke:
    mvn test -Dgroups=smoke -Denv=local -DbrowserHeadless=true

test-regression:
    mvn test -Dgroups=regression -Denv=local -Dparallel=5

test-single:
    mvn test -Dtest=$(CLASS)#$(METHOD) -Denv=local

test-debug:
    mvn test -Dtest=$(CLASS)#$(METHOD) -DheadlessEnabled=false -Denv=local
    # Opens browser visibly for debugging
```

**Component 6: Environment Detection**
```java
public class EnvironmentDetector {
    public static boolean isLocalDeveloper() {
        // Developer machine: no BUILD_NUMBER (CI env), no KUBERNETES_SERVICE_HOST
        return System.getenv("BUILD_NUMBER") == null
            && System.getenv("KUBERNETES_SERVICE_HOST") == null
            && System.getenv("CI") == null;
    }

    public static boolean isCi() {
        return System.getenv("CI") != null
            || System.getenv("BUILD_NUMBER") != null;
    }
}

// In DriverFactory:
if (EnvironmentDetector.isLocalDeveloper()) {
    // Local: headless Chrome, Testcontainers stack
} else {
    // CI: Grid URL, environment from -Denv flag
}
```

**Developer Experience Summary:**
```bash
# New developer workflow — zero setup required:
git clone https://github.com/company/app.git
cd app
mvn test -Dgroups=smoke  # Automatically starts Docker, downloads drivers, runs tests
# Total: 8 minutes from clone to first test result
```

**Prerequisites reduced to:**
- Java 17+ (`sdk install java`)
- Maven 3.9+ (included in project via `mvn wrapper`)
- Docker Desktop
- `git clone`

---

## AQ18: How do you design test reporting that serves multiple audiences (engineers, QA leads, management)?

### Question
Design a reporting architecture that gives: engineers detailed failure context (screenshot, stack trace, video), QA leads trend data (flakiness rate, coverage gaps), and management a one-page health dashboard. All from the same test run.

### Answer

**Three-Tier Reporting Architecture:**

```
Test Run
    │
    ▼
Allure Results (raw JSON)
    │
    ├──► Allure HTML Report (Engineer view)
    ├──► Allure TestOps (QA Lead trend view)
    └──► Prometheus + Grafana (Management dashboard)
```

**Tier 1: Engineer Report (Allure HTML)**

What engineers need per failing test:
- Screenshot at failure moment
- Full stack trace
- Page source HTML
- Browser console errors
- Network requests that returned 4xx/5xx
- Test steps timeline (which step failed)
- Video recording link (Selenoid)
- Retry history (was this a flaky failure or deterministic?)

Implementation already covered in CQ9 (`AllureExtension`). Additional context:
```java
@Override
public void testFailed(ExtensionContext ctx, Throwable cause) {
    WebDriver driver = DriverFactory.getDriver();

    // All failure artifacts in one block
    AllureExtension.attachScreenshot(driver, "Screenshot at Failure");
    Allure.addAttachment("Page Source", "text/html", driver.getPageSource(), ".html");
    Allure.addAttachment("Current URL", driver.getCurrentUrl());

    // Selenoid video link
    String sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
    Allure.addLink("Session Video",
        ConfigReader.get("selenoidUrl") + "/video/" + sessionId + ".mp4");

    // Retry context from TestOrchestrator
    String testId = ctx.getRequiredTestClass().getName() + "#" +
        ctx.getRequiredTestMethod().getName();
    TestResult history = new TestOrchestrator().loadHistory().get(testId);
    if (history != null) {
        Allure.addAttachment("Test History",
            "Total runs: " + history.totalRuns() + "\n" +
            "Consecutive passes: " + history.consecutivePassCount() + "\n" +
            "Quarantined: " + history.quarantined());
    }
}
```

**Tier 2: QA Lead Dashboard (Allure TestOps or Custom)**

QA leads need trend data, not individual test details. In Allure TestOps this is built-in. For custom:
```java
// TestSummaryExporter — writes JSON after each suite run
public class TestSummaryExporter {
    public void export(SuiteResult result) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId",          System.getProperty("BUILD_NUMBER", "local"));
        summary.put("timestamp",      Instant.now().toString());
        summary.put("browser",        ConfigReader.get("browser", "chrome"));
        summary.put("environment",    ConfigReader.get("env", "staging"));
        summary.put("totalTests",     result.total());
        summary.put("passed",         result.passed());
        summary.put("failed",         result.failed());
        summary.put("skipped",        result.skipped());
        summary.put("passRate",       result.passRate());
        summary.put("flakinesRate",   result.flakinessRate());
        summary.put("durationSeconds",result.durationSeconds());
        summary.put("failedTests",    result.failedTestNames());

        // POST to internal metrics API
        given().body(summary).contentType(ContentType.JSON)
            .post(ConfigReader.get("metricsApiUrl") + "/test-runs");
    }
}
```

QA lead pipeline (Allure TestOps / custom Grafana dashboard):
- Pass rate trend (14-day rolling)
- Flakiest tests by name (>2% retry rate)
- Coverage by feature area (epic/feature Allure labels)
- New failures introduced in this sprint
- Mean time to recovery per failed test

**Tier 3: Management Dashboard (Grafana / PowerBI)**

Management needs maximum 5 numbers:

| Metric | Target | Alert |
|---|---|---|
| Smoke gate pass rate | >99% | <95% |
| Regression pass rate (7-day avg) | >98% | <90% |
| Mean time to detect failure | <10 min | >30 min |
| Suite duration (p95) | <20 min | >45 min |
| Production defect escape rate | 0 (caught by tests) | >0 |

Grafana dashboard fed by Prometheus:
```promql
# Pass rate — last 24 hours
sum(selenium_tests_total{result="passed"}) 
  / sum(selenium_tests_total) * 100

# Flakiness rate
sum(selenium_tests_total{result="retried"})
  / sum(selenium_tests_total) * 100

# Suite duration trend
histogram_quantile(0.95, 
  sum by (suite, le) (selenium_suite_duration_seconds_bucket{suite="smoke"}))
```

**Key Architectural Decision:**
The reporting pyramid follows information density:
- Engineers: maximum detail (every test, every artifact)
- QA leads: aggregate trends (team-level, weekly)
- Management: 5 KPIs on one screen (no raw test data)

The same Allure result files feed all tiers — no separate reporting runs.

---

## AQ19: What is your strategy for testing Progressive Web Apps (PWA) and offline capabilities?

### Question
Your team has built a PWA with a service worker that caches resources for offline use. Users can add items to a cart offline and sync when connectivity is restored. Describe how you test this with Selenium.

### Answer

**PWA-Specific Testing Challenges:**
- Service worker caching — app works after initial load even with no network
- Background sync — queued actions must fire when connection restores
- Push notifications — browser permission dialog; not standard DOM
- App installation (Add to Homescreen) — browser-native, not Selenium-controlled
- Offline behavior — requires simulating network disconnection

**Strategy 1: Simulate Network Offline/Online via CDP**

```java
public class NetworkConditionSimulator {
    private final DevTools devTools;

    public NetworkConditionSimulator(WebDriver driver) {
        devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
    }

    public void goOffline() {
        devTools.send(Network.emulateNetworkConditions(
            true,    // offline
            0,       // latencyMs
            0,       // downloadThroughput
            0,       // uploadThroughput
            Optional.of(ConnectionType.NONE)
        ));
        log.info("[Network] Simulated OFFLINE");
    }

    public void goOnline() {
        devTools.send(Network.emulateNetworkConditions(
            false,   // offline = false
            20,      // 20ms latency (realistic)
            -1,      // unlimited download
            -1,      // unlimited upload
            Optional.of(ConnectionType.WIFI)
        ));
        log.info("[Network] Simulated ONLINE");
    }

    public void throttleSlowConnection() {
        // 3G-equivalent: 40kbps down, 20kbps up, 300ms latency
        devTools.send(Network.emulateNetworkConditions(
            false, 300, 40_000, 20_000,
            Optional.of(ConnectionType.CELLULAR3G)));
    }

    public void stopEmulation() {
        devTools.send(Network.disable());
    }
}
```

**Strategy 2: Test Service Worker Cache**

```java
public class ServiceWorkerHelper {
    private final JavascriptExecutor js;

    public ServiceWorkerHelper(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    // Wait for service worker to be installed and activated
    public void waitForServiceWorkerActive() {
        new WebDriverWait((WebDriver) js, Duration.ofSeconds(30))
            .until(d -> {
                Object state = js.executeScript(
                    "return navigator.serviceWorker.controller ? " +
                    "  navigator.serviceWorker.controller.state : null;");
                return "activated".equals(state);
            });
    }

    // Check if specific URL is cached
    public boolean isCached(String url) {
        Object result = js.executeAsyncScript(
            "var url = arguments[0];" +
            "var cb = arguments[arguments.length-1];" +
            "caches.match(url).then(r => cb(r != null));",
            url);
        return Boolean.TRUE.equals(result);
    }

    // Clear all service worker caches (pre-test setup)
    public void clearCaches() {
        js.executeAsyncScript(
            "var cb = arguments[arguments.length-1];" +
            "caches.keys().then(keys => " +
            "  Promise.all(keys.map(k => caches.delete(k)))" +
            "  .then(() => cb(true)));");
    }

    // Force service worker update
    public void forceUpdate() {
        js.executeAsyncScript(
            "var cb = arguments[arguments.length-1];" +
            "navigator.serviceWorker.ready.then(reg => " +
            "  reg.update().then(() => cb(true)));");
    }
}
```

**Strategy 3: Full Offline Scenario Test**

```java
class PwaOfflineTest extends BaseTest {
    private NetworkConditionSimulator network;
    private ServiceWorkerHelper swHelper;

    @BeforeEach
    void setUp() {
        DriverFactory.initDriver("chrome");
        network  = new NetworkConditionSimulator(driver());
        swHelper = new ServiceWorkerHelper(driver());
    }

    @Test
    void cartItemsQueuedOfflineAndSyncOnReconnect() {
        // ── Online: Load app and wait for SW to activate ──────────────
        loginAs("CUSTOMER");
        navigateTo("/products/LAPTOP-001");
        swHelper.waitForServiceWorkerActive();

        // ── Simulate offline ──────────────────────────────────────────
        network.goOffline();

        // App should show offline indicator
        new AppWait(driver()).untilVisible(
            By.cssSelector("[data-testid='offline-banner']"));

        // Add to cart while offline
        new ProductPage(driver()).addToCart();

        // Verify queued in background sync
        Object syncCount = ((JavascriptExecutor) driver()).executeAsyncScript(
            "var cb = arguments[arguments.length-1];" +
            "navigator.serviceWorker.ready.then(reg => {" +
            "  if (reg.sync) reg.sync.getTags().then(tags => cb(tags.length));" +
            "  else cb(0);" +
            "});");
        assertThat(((Number) syncCount).intValue()).isGreaterThan(0);

        // ── Restore connectivity ──────────────────────────────────────
        network.goOnline();

        // Offline banner disappears
        new WebDriverWait(driver(), Duration.ofSeconds(15))
            .until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("[data-testid='offline-banner']")));

        // Cart item should now be synced
        new AppWait(driver()).untilVisible(
            By.cssSelector("[data-testid='cart-badge']"));
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='cart-badge']")).getText())
            .isEqualTo("1");

        // Verify API received the queued cart item
        assertThat(CartApiClient.getCartItems(authToken)).hasSize(1);
    }

    @Test
    void cachedPagesLoadWhileOffline() {
        loginAs("CUSTOMER");
        navigateTo("/products");       // loads products list — gets cached
        navigateTo("/products/LAPTOP-001"); // loads product — gets cached
        swHelper.waitForServiceWorkerActive();

        network.goOffline();

        // Navigate to a cached page — should load from cache
        navigateTo("/products/LAPTOP-001");
        new AppWait(driver()).untilDocumentReady();
        assertThat(driver().findElement(
            By.cssSelector("[data-testid='product-title']")).isDisplayed())
            .isTrue();

        // Navigate to un-cached page — should show offline fallback page
        navigateTo("/reports");
        new AppWait(driver()).untilVisible(
            By.cssSelector("[data-testid='offline-fallback']"));
    }

    @AfterEach
    void tearDown() {
        network.stopEmulation();
        DriverFactory.quitDriver();
    }
}
```

**Strategy 4: Push Notification Testing**

Push notifications require browser permission — grant it programmatically:
```java
// Grant notification permission before test
Map<String, Object> params = new HashMap<>();
params.put("origin", BASE_URL);
params.put("permission", "notifications");
params.put("setting", "granted");
((ChromeDriver) driver).executeCdpCommand(
    "Browser.setPermission", params);

// Then test that notification is received (via JS observation)
js.executeScript(
    "window.__notifications = [];" +
    "var OrigNotification = window.Notification;" +
    "window.Notification = function(title, opts) {" +
    "  window.__notifications.push({title, opts});" +
    "  return new OrigNotification(title, opts);" +
    "};"
);
```

**Key Design Decisions:**
- CDP `Network.emulateNetworkConditions` for offline simulation — deterministic, instant, no VPN or proxy required
- Test Background Sync via `navigator.serviceWorker.ready.sync.getTags()` — verifies queue mechanism before reconnection rather than purely trusting UI state
- `clearCaches()` in `@BeforeEach` — ensures each test starts from a known cache state; prevents test ordering dependencies

---

## AQ20: What are the key differences between Selenium and Playwright for enterprise test automation, and how do you decide which to use?

### Question
As a Principal SDET evaluating tool choices for a large organization, compare Selenium 4 and Playwright comprehensively: architecture, performance, browser support, debugging, parallel execution, CI integration, community/support, and the realistic scenarios where each is the right choice.

### Answer

**Architecture Comparison:**

| Dimension | Selenium 4 | Playwright |
|---|---|---|
| Protocol | W3C WebDriver (HTTP + CDP) | CDP/DevTools (direct, non-HTTP) |
| Language support | Java, Python, JS, C#, Ruby | JS/TS, Python, Java, C# |
| Browser communication | HTTP requests to WebDriver server | WebSocket (persistent, low latency) |
| Browser support | Chrome, Firefox, Edge, Safari, IE | Chrome, Firefox, Edge, WebKit |
| Grid/distributed | Selenium Grid 4 (mature, cloud-ready) | Playwright Grid (less mature) |
| Mobile testing | Appium + Selenium (separate) | Limited (no native Appium equivalent) |

**Performance:**

Playwright is measurably faster for single-browser test execution:
- Browser startup: Playwright ~400ms vs Selenium ~1.5–2.5s
- Element finding: WebSocket vs HTTP round-trip — Playwright ~30% faster
- Auto-waiting: Playwright built-in vs Selenium explicit `WebDriverWait`

However: with 20-thread parallel Selenium on Grid, wall-clock time is comparable because parallelism compensates for per-test overhead.

**Developer Experience:**

Playwright advantages:
- `page.waitForSelector()` built-in auto-wait — no explicit `WebDriverWait`
- `page.route()` — request interception without CDP boilerplate
- `page.addLocatorHandler()` — dismiss overlays automatically
- Playwright Inspector — interactive debugger with step-through
- Playwright Trace Viewer — zip file: screenshot per action, network log, console
- `codegen` tool — record browser interactions and generate test code

Selenium advantages:
- Mature Java ecosystem (12+ years of libraries, StackOverflow answers)
- WebDriverManager, Selenium Manager — driver management solved
- Allure integration depth for Java
- `EventFiringDecorator` for cross-cutting concerns

**Browser Support Reality:**

Safari production testing requires Selenium (WebKit ≠ Safari). If real Safari on macOS is required:
- Selenium + SafariDriver on a macOS node = real Safari
- Playwright WebKit = Webkit engine, NOT Apple's Safari with extensions/OS integration

**Debugging Capability:**

| Scenario | Selenium | Playwright |
|---|---|---|
| Slow-motion replay | `driver.manage().timeouts()` | `slowMo` option |
| Step-through debugging | IDE debugger | Playwright Inspector |
| Video on failure | Selenoid (external) | Built-in `recordVideo` |
| Trace analysis | Allure + manual | Playwright Trace Viewer (built-in) |
| Network inspection | CDP (manual code) | `page.route()` (1 line) |

Playwright wins significantly on debugging UX.

**When to Choose Selenium:**

1. **Cross-browser at scale (Grid 4)** — 1000 concurrent sessions, CI/CD pipeline, multiple teams
2. **Java-first organization** — existing Selenium expertise; `framework-core` investment
3. **Real Safari testing** — SafariDriver on macOS is the only real Safari
4. **Legacy test suite migration** — Selenium vintage, incremental modernization
5. **Mobile web** — Appium + Selenium; no Playwright equivalent
6. **Compliance/regulated industry** — Selenium WebDriver is a W3C standard; auditors recognize it

**When to Choose Playwright:**

1. **New greenfield project with JS/TS teams** — codegen, trace viewer, auto-wait out of the box
2. **Developer-driven testing** — component test + E2E in same stack
3. **Complex network scenarios** — `page.route()` is simpler than CDP code
4. **Single-browser fast feedback** — quick smoke suite, developer laptop
5. **Modern SPAs (React/Vue/Next.js)** — Playwright's auto-waiting handles SPA rendering better
6. **Visual regression with Playwright screenshots** — `expect(page).toHaveScreenshot()` built-in

**Recommended Hybrid Strategy for Large Organizations:**

```
Use Playwright for:
  - Developer component/integration tests (fast feedback, DX)
  - New feature tests written by feature teams
  - Network interception / contract scenarios
  - Test generation via codegen

Use Selenium for:
  - Cross-browser Grid runs (Chrome + Firefox + Edge + Safari at scale)
  - Mobile web via Appium
  - Legacy test suite (maintained, don't rewrite what works)
  - Regulated scenarios requiring W3C standard compliance

Both feed → unified Allure TestOps report
Both use → shared TestDataFactory, ConfigReader, API clients
```

**Key Hiring/Team Implication:**

For Principal SDET level: knowing both deeply is expected. Being able to choose the right tool for a given scenario — and articulate the trade-offs to stakeholders — is the architectural skill being evaluated. The wrong answer is "always Selenium" or "always Playwright." The right answer is demonstrating the decision framework.

---

# Summary

This document covers 100 interview questions for a **Principal SDET / Automation Architect (Selenium + Java)** role:

## Section 1 — Theory Questions (Q1–Q50)
Core Selenium concepts, wait strategies, Grid architecture, parallel execution, CDP, POM, test observability, SPA testing, flaky test management, accessibility, performance, and framework design at scale.

## Section 2 — Coding Questions (CQ1–CQ30)
Production-quality implementations of: DriverFactory, Page Objects, data table components, CDP utilities (file download, network interception, performance metrics, BiDi), Actions API, retry logic, Allure integration, TestNG parallel suites, custom ExpectedConditions, JUnit 5 parallelism with resource locks, REST Assured hybrid tests, iframe handling, window management, dialogs, browser storage, visual regression, self-healing locators, test data factories, config management, Grid 4 remote driver factory, test orchestrator, cross-browser capture, BasePage composition, database verification, accessibility with axe-core, and a full E2E test demonstrating every component.

## Section 3 — Architecture Questions (AQ1–AQ20)
Enterprise framework design for 500+ tests across 15 teams, Grid 4 at 1000 sessions, unified Selenium+Playwright+API framework, flakiness elimination, CI/CD for 10x daily deploys, parallel data management, observability stack, mobile web extension, legacy suite migration, microservices environment strategy, i18n/l10n testing, real-time WebSocket/SSE testing, framework security (OWASP), performance optimization (90 min → 15 min), authentication/authorization, feature flags, developer self-service environment, multi-audience reporting, PWA offline testing, and Selenium vs Playwright decision framework.

---

*End of Selenium + Java Interview Preparation Guide*


