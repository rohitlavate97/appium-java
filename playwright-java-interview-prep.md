# Playwright Java Interview Preparation Guide
### Target: Senior SDET / Automation Architect (6+ Years Experience)
> Java 17+ | Playwright Java Latest | Enterprise-Grade | SOLID Principles

---

# SECTION 1 — THEORY INTERVIEW QUESTIONS (50)

---

## Q1: What is the Playwright Java architecture? How does it differ from Selenium WebDriver?

**Answer:**
- Playwright Java is a browser automation library that communicates with browsers via the **Chrome DevTools Protocol (CDP)** or browser-native protocols (Firefox, WebKit).
- It uses a single binary (`playwright-driver`) that manages browser lifecycle via an **inter-process communication (IPC)** channel.
- Unlike Selenium, Playwright does NOT use WebDriver JSON Wire Protocol — it communicates directly with the browser engine, enabling far more reliable automation.

**Explanation:**
- Playwright's architecture: `Test Code → Playwright Java Client → IPC → Playwright Node Server → Browser`
- The Node server manages browser processes and translates Java API calls to browser-native instructions.
- Auto-waiting is baked into every action — no `Thread.sleep()` or explicit `WebDriverWait` needed.
- Browsers (Chromium, Firefox, WebKit) are bundled and versioned with Playwright to ensure compatibility.

**Java Example:**
```java
import com.microsoft.playwright.*;

public class PlaywrightArchitectureDemo {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://example.com");
            System.out.println("Title: " + page.title());
            // Auto cleanup via try-with-resources
        }
    }
}
```

**Real-world Usage:**
- In a FinTech platform, Playwright Java's direct CDP communication is used to intercept API calls during UI flows (e.g., capturing JWT tokens during login) — impossible with standard Selenium.

**Common Mistakes:**
- Not closing `Playwright`, `Browser`, `BrowserContext` — causes resource/memory leaks in CI pipelines.
- Mixing Playwright and Selenium concepts (e.g., expecting Playwright to need explicit waits).

**Optimization Tip:**
- Use `try-with-resources` for all Playwright objects. In parallel test suites, create one `Playwright` instance per thread to avoid thread-safety issues.

**Debugging Strategy:**
- Set `PLAYWRIGHT_JAVA_SRC` env var and enable debug logging: `DEBUG=pw:api` to trace all API calls.
- Use `page.pause()` to open Inspector mid-test for interactive debugging.

**Tricky Follow-up Questions:**
1. If Playwright uses a Node server under the hood, how does it handle Java threading in parallel execution?
2. What happens when the Playwright Node server process crashes mid-test — how do you recover gracefully?

**Compare — Playwright vs Selenium:**
| | Playwright Java | Selenium WebDriver |
|---|---|---|
| Protocol | CDP / Browser-native | WebDriver (HTTP JSON Wire) |
| Auto-wait | Built-in | Explicit / Fluent Wait needed |
| Browser bundling | Yes (versioned) | No (driver management separate) |
| Network intercept | Native | Limited (via proxy) |
| Speed | Faster | Slower (HTTP round trips) |

---

## Q2: Explain the Browser, BrowserContext, and Page lifecycle in Playwright Java.

**Answer:**
- `Playwright` → top-level factory, manages browser types.
- `Browser` → represents a browser instance (Chromium, Firefox, WebKit).
- `BrowserContext` → an isolated browser session (like an incognito window). Each context has its own cookies, storage, and auth state.
- `Page` → a single tab within a context.

**Explanation:**
- `BrowserContext` is the key isolation unit. Multiple contexts in the same browser share the browser process but have **completely isolated** storage.
- This is critical for parallel test execution — each test gets its own `BrowserContext`, avoiding session bleed between tests.
- Pages within the same context **share** cookies and storage state.

**Java Example:**
```java
public class LifecycleDemo {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            // One browser — multiple isolated contexts
            Browser browser = playwright.chromium().launch();

            // Context 1 — User A session
            BrowserContext contextA = browser.newContext();
            Page pageA = contextA.newPage();
            pageA.navigate("https://app.example.com/login");

            // Context 2 — User B session (completely isolated)
            BrowserContext contextB = browser.newContext();
            Page pageB = contextB.newPage();
            pageB.navigate("https://app.example.com/login");

            // Both run independently — no session bleed
            contextA.close();
            contextB.close();
        }
    }
}
```

**Real-world Usage:**
- In an enterprise SaaS platform, parallel test execution uses one `BrowserContext` per test class. Authentication state is pre-saved via `storageState` and injected per context — eliminating repeated login flows.

**Common Mistakes:**
- Creating a new `Browser` per test (expensive). Correct pattern: reuse `Browser`, create new `BrowserContext` per test.
- Forgetting `context.close()` leading to browser zombie processes in Docker containers.

**Optimization Tip:**
- Pre-authenticate once, save `storageState` to a JSON file, and inject it into each `BrowserContext` — reduces test suite time by 30–40%.

```java
// Save auth state
context.storageState(new BrowserContext.StorageStateOptions()
    .setPath(Paths.get("auth/user-state.json")));

// Reuse in other tests
BrowserContext authenticatedContext = browser.newContext(
    new Browser.NewContextOptions()
        .setStorageStatePath(Paths.get("auth/user-state.json")));
```

**Debugging Strategy:**
- Call `context.tracing().start(...)` at context creation and `context.tracing().stop(...)` on failure — produces a Trace Viewer file with full network + DOM snapshots.

**Tricky Follow-up Questions:**
1. Two pages in the same `BrowserContext` — if Page A sets a cookie, does Page B see it immediately?
2. How would you implement a "clean state" pattern for tests that must not share any browser state, even across browser restarts?

**Compare — Playwright vs Selenium:**
- Selenium has no concept of `BrowserContext` — each WebDriver instance is a full browser. Playwright's context model enables cheaper isolation at scale.

---

## Q3: How does Playwright's auto-waiting mechanism work? What exactly does it wait for?

**Answer:**
- Playwright automatically waits for elements to be in an **actionable state** before performing any action.
- Actionable state means: visible, stable (no animation), enabled, editable (for input), and not obscured.

**Explanation:**
- For every action (`click`, `fill`, `check`, etc.), Playwright internally runs an **actionability check** loop.
- Default timeout: **30 seconds** (configurable).
- Checks performed before `click()`:
  1. Element is attached to DOM
  2. Element is visible
  3. Element is stable (no CSS animations)
  4. Element is enabled
  5. Element receives events (not covered by overlay)

**Java Example:**
```java
public class AutoWaitDemo {
    public void demonstrateAutoWait(Page page) {
        // Playwright waits automatically — no explicit wait needed
        page.locator("#submit-btn").click(); // waits until button is actionable

        // For navigation after click
        page.locator("#submit-btn").click();
        page.waitForURL("**/dashboard"); // wait for URL change after navigation

        // Custom timeout per action
        page.locator("#lazy-element").click(
            new Locator.ClickOptions().setTimeout(60_000)
        );
    }
}
```

**Real-world Usage:**
- In a FinTech payment portal, buttons are disabled while validation runs. Playwright's auto-wait naturally handles this — tests don't need custom waits for the validation spinner to complete.

**Common Mistakes:**
- Adding `page.waitForTimeout(2000)` (sleep) — anti-pattern. Playwright's auto-wait handles timing.
- Not understanding that `locator.isVisible()` does NOT wait — it's a snapshot check. Use `locator.waitFor()` explicitly if needed.

**Optimization Tip:**
- Set global timeout in config rather than per-action:
```java
page.setDefaultTimeout(45_000); // 45 seconds global
page.setDefaultNavigationTimeout(60_000); // navigation timeout
```

**Debugging Strategy:**
- When auto-wait times out, the error message shows which actionability check failed (e.g., "Element is not visible"). Use `page.pause()` or Trace Viewer to inspect the DOM state at failure time.

**Tricky Follow-up Questions:**
1. `locator.click()` waited but still clicked the wrong element — how can that happen and how do you fix it?
2. How does Playwright handle an element that becomes visible but is immediately covered by a tooltip overlay?

**Compare — Playwright vs Selenium:**
- Selenium requires explicit `WebDriverWait` + `ExpectedConditions`. Playwright's automatic actionability checking eliminates the most common source of flaky Selenium tests.

---

## Q4: What is Playwright's smart locator strategy? How do you choose the right locator?

**Answer:**
- Playwright recommends **user-facing, resilient locators** over brittle CSS/XPath: `getByRole()`, `getByText()`, `getByLabel()`, `getByPlaceholder()`, `getByTestId()`.
- These locators mirror how real users interact with the UI, making tests less sensitive to DOM structure changes.

**Explanation:**
- Locator priority (best to worst):
  1. `getByRole()` — ARIA roles, matches accessibility tree
  2. `getByLabel()` — form inputs by label text
  3. `getByPlaceholder()` — inputs by placeholder
  4. `getByText()` — elements by visible text
  5. `getByTestId()` — `data-testid` attributes (agreed with dev team)
  6. CSS selectors — when above aren't applicable
  7. XPath — last resort

**Java Example:**
```java
public class SmartLocatorDemo {

    public void loginWithSmartLocators(Page page) {
        // Prefer role + accessible name
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Email"))
            .fill("user@company.com");

        page.getByLabel("Password").fill("SecurePass123");

        // Button by role and name
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In"))
            .click();
    }

    public void useTestId(Page page) {
        // When role locators aren't sufficient
        page.getByTestId("dashboard-widget-revenue").isVisible();
    }
}
```

**Real-world Usage:**
- In an enterprise SaaS product, the dev team adds `data-testid` attributes for automation-critical elements. QA team uses `getByTestId()` for stable automation and `getByRole()` for accessibility validation simultaneously.

**Common Mistakes:**
- Using `locator("div > span:nth-child(3)")` — fragile, breaks on DOM restructure.
- Using `getByText()` for dynamic text that changes per environment (e.g., prices, dates).

**Optimization Tip:**
- Establish a **locator strategy contract** with developers: agree on `data-testid` naming convention for all interactive elements. This creates a stable automation layer independent of UI redesigns.

**Debugging Strategy:**
- Use Playwright's `codegen` tool to auto-generate locator suggestions: `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="codegen https://yourapp.com"`

**Tricky Follow-up Questions:**
1. `getByRole(AriaRole.BUTTON, name("Submit"))` matches two buttons on the page — Playwright throws a strict mode error. How do you resolve this?
2. A third-party component doesn't expose ARIA roles. What's your fallback strategy?

**Compare — Playwright vs Selenium:**
- Selenium has no built-in smart locator strategy — teams default to XPath/CSS. Playwright's role-based locators align automation with accessibility, improving both test resilience and a11y coverage.

---

## Q5: What is Playwright Strict Mode? When is it triggered and how do you handle it?

**Answer:**
- Strict mode means Playwright throws an error if a locator matches **more than one element**.
- It ensures deterministic tests — you must always target exactly one element.
- Triggered on any action (`click`, `fill`, etc.) or assertion when multiple elements match the locator.

**Explanation:**
- Strict mode is **on by default** for all locator actions in Playwright.
- It prevents silent errors like clicking the wrong button when two match.
- `locator.all()` is the explicit opt-out — returns all matching elements as a list.

**Java Example:**
```java
public class StrictModeDemo {

    public void handleStrictMode(Page page) {
        // This throws if multiple ".submit-btn" exist
        // page.locator(".submit-btn").click(); // ❌ STRICT MODE ERROR

        // Fix 1: Narrow the locator
        page.locator("form#checkout .submit-btn").click(); // ✅

        // Fix 2: Use nth()
        page.locator(".submit-btn").nth(0).click(); // ✅ first match

        // Fix 3: Filter by text
        page.locator(".submit-btn").filter(
            new Locator.FilterOptions().setHasText("Place Order")
        ).click(); // ✅

        // Intentionally get all matches
        List<Locator> allButtons = page.locator(".card-item").all();
        for (Locator btn : allButtons) {
            System.out.println(btn.textContent());
        }
    }
}
```

**Real-world Usage:**
- In a product catalog page with repeating "Add to Cart" buttons, strict mode forces the team to write precise locators scoped to a specific product card — preventing accidental multi-match bugs.

**Common Mistakes:**
- Using `.first()` blindly without understanding which element you're actually targeting.
- Suppressing strict mode with `.nth(0)` without understanding root cause.

**Optimization Tip:**
- When you encounter a strict mode error, use Playwright Inspector (`page.pause()`) to visually identify all matching elements and refine the locator rather than blindly using `.nth()`.

**Debugging Strategy:**
- The error message: `"strict mode violation: locator resolved to X elements"` — run with `DEBUG=pw:api` to see full resolution details. Then use `page.locator(...).count()` to verify expected match count.

**Tricky Follow-up Questions:**
1. In a dynamic list where items are added/removed, how do you ensure your locator always targets the correct element deterministically?
2. Can you disable strict mode globally? Should you ever do this in a production test suite?

---

## Q6: How do you handle dynamic locators in Playwright Java?

**Answer:**
- Dynamic locators handle elements whose attributes change at runtime (dynamic IDs, data-driven text, generated classnames).
- Strategies: parameterized locators, CSS attribute selectors, regex text matching, `filter()`, `locator.and()`.

**Explanation:**
- Playwright supports **string interpolation in selectors** and **regex-based text matching**.
- `filter()` enables composing stable locators from multiple conditions.
- `locator.and()` chains two locators that must both match the same element.

**Java Example:**
```java
public class DynamicLocatorDemo {

    // Parameterized locator — row by dynamic user ID
    public Locator getUserRow(Page page, String userId) {
        return page.locator(String.format("tr[data-user-id='%s']", userId));
    }

    // Regex text match — partial dynamic text
    public void handleDynamicText(Page page) {
        // Matches "Order #12345", "Order #99999", etc.
        page.getByText(Pattern.compile("Order #\\d+")).click();
    }

    // Filter by nested element text
    public void selectProductCard(Page page, String productName) {
        page.locator(".product-card")
            .filter(new Locator.FilterOptions().setHasText(productName))
            .locator(".add-to-cart-btn")
            .click();
    }

    // Locator AND — element matching multiple conditions
    public void clickActiveAdminButton(Page page) {
        Locator active = page.locator("[data-state='active']");
        Locator adminBtn = page.locator("[data-role='admin-action']");
        active.and(adminBtn).click();
    }
}
```

**Real-world Usage:**
- In a financial dashboard with server-generated row IDs, dynamic locators parameterized by account number are used to target specific rows without relying on fragile positional XPath like `//tr[3]`.

**Common Mistakes:**
- Building XPath like `//td[contains(text(), 'dynamic-value')]` — over-reliance on XPath for dynamic content.
- Not escaping special characters in parameterized selectors (SQL-injection-style locator corruption).

**Optimization Tip:**
- Build a `LocatorFactory` utility class that encapsulates dynamic locator patterns as reusable methods — reduces duplication across Page Objects.

**Debugging Strategy:**
- Use `page.locator(selector).count()` to verify match count before acting. Log selector strings when tests fail to aid in reproduction.

**Tricky Follow-up Questions:**
1. A row has a dynamically generated `id` like `row-a3f92b` — how do you locate it reliably without knowing the ID in advance?
2. How do you handle locators for elements inside a dynamically loaded micro-frontend iframe that changes src URLs?

---

## Q7: When should you use CSS selectors vs XPath in Playwright Java?

**Answer:**
- **Prefer CSS** for most cases — faster, more readable, supported natively by browsers.
- **Use XPath** only when CSS cannot express the requirement (e.g., selecting a parent by child text, axis navigation).
- Playwright-native locators (`getByRole`, `getByText`) should always be considered first.

**Explanation:**
- CSS selectors: `div.card > button.submit`, `input[type='email']`, `[data-testid='login']`
- XPath: `//label[text()='Email']/following-sibling::input`, `//button[contains(@class,'submit') and not(@disabled)]`
- Playwright evaluates both through the browser engine — CSS is generally faster.

**Java Example:**
```java
public class CssXpathDemo {

    public void cssExamples(Page page) {
        // Attribute selector
        page.locator("input[name='email']").fill("test@example.com");

        // Child combinator
        page.locator("form.login-form > button[type='submit']").click();

        // Nth-child
        page.locator("table tbody tr:nth-child(2) td:first-child").textContent();

        // Data attribute
        page.locator("[data-testid='confirm-modal'] button.primary").click();
    }

    public void xpathExamples(Page page) {
        // Select input AFTER a label — CSS can't traverse backwards
        page.locator("xpath=//label[text()='Account Number']/following-sibling::input")
            .fill("123456789");

        // Parent selection (CSS can't go up the tree)
        page.locator("xpath=//span[text()='Error']/ancestor::div[@class='alert']")
            .isVisible();
    }
}
```

**Real-world Usage:**
- In a complex insurance claim form, label-to-input relationship locators use XPath axis navigation when `getByLabel()` fails due to non-standard HTML structure. All other locators use CSS for performance.

**Common Mistakes:**
- Writing `xpath=//div/div/div/span` — deeply nested XPath is extremely fragile.
- Using XPath text functions when `getByText()` is simpler and more readable.
- Mixing `$x()` JS console XPath syntax with Java — they are different.

**Optimization Tip:**
- Run a locator audit in your framework: flag any XPath longer than 3 axes as a technical debt item. Replace with data-testid or CSS equivalent.

**Debugging Strategy:**
- Validate selectors in the browser console: `document.querySelector('your-css')` or `$x('your-xpath')` before using in code.

**Tricky Follow-up Questions:**
1. CSS `:has()` selector — is it supported in Playwright? How does it compare to Playwright's `filter()` method?
2. You have a table with no IDs or data attributes — how do you locate a specific cell without brittle positional XPath?

---

## Q8: How do text locators work in Playwright Java? What are the nuances?

**Answer:**
- `getByText()` finds elements by their **visible text content**.
- Supports exact string, substring, and regex matching.
- Matches the deepest element containing the text (not outer containers).

**Explanation:**
- By default, `getByText("Sign In")` does a **substring, case-insensitive** match.
- Exact match: `getByText("Sign In", new Page.GetByTextOptions().setExact(true))`
- Regex match: `getByText(Pattern.compile("Order.*Confirmed"))`
- Text matching normalizes whitespace by default.

**Java Example:**
```java
public class TextLocatorDemo {

    public void textLocatorVariants(Page page) {
        // Substring match (default)
        page.getByText("Welcome").isVisible();

        // Exact match
        page.getByText("Welcome back, John",
            new Page.GetByTextOptions().setExact(true)).isVisible();

        // Regex match for dynamic content
        page.getByText(Pattern.compile("Invoice #\\d{6}")).click();

        // Scoped text locator — within a specific container
        Locator modal = page.locator(".confirmation-modal");
        modal.getByText("Confirm").click(); // only "Confirm" inside modal

        // Filter locator using text
        page.locator(".notification-item")
            .filter(new Locator.FilterOptions().setHasText("Payment Failed"))
            .locator("button.dismiss")
            .click();
    }
}
```

**Real-world Usage:**
- In an enterprise notification center, `filter().setHasText()` is used to locate and dismiss specific notification types (e.g., "Payment Failed") without position-based locators.

**Common Mistakes:**
- Using `getByText()` on container elements (e.g., a `<div>` containing many text nodes) — matches the container, not the intended child.
- Forgetting that whitespace in the page HTML (e.g., `\n  Sign In  \n`) is normalized, but extra words are not.
- Using `getByText()` for buttons — `getByRole(BUTTON, name("..."))` is more precise.

**Optimization Tip:**
- Always scope `getByText()` within a specific container locator to avoid accidental matches in headers, footers, or sidebars.

**Debugging Strategy:**
- `locator.count()` to verify how many elements match. Use `locator.all()` to iterate and print each element's text to identify the correct one.

**Tricky Follow-up Questions:**
1. `getByText("Submit")` matches a visible button AND a hidden tooltip. How does Playwright resolve this?
2. The text "Order Confirmed" exists in both the page title and a confirmation badge — how do you target only the badge?

---

## Q9: How do role locators work in Playwright Java? Why are they preferred?

**Answer:**
- `getByRole()` locates elements by their **ARIA role** and optionally by accessible name.
- Preferred because they reflect how assistive technologies (screen readers) see the page — tests verify both functionality AND accessibility.

**Explanation:**
- ARIA roles include: `BUTTON`, `TEXTBOX`, `CHECKBOX`, `LINK`, `HEADING`, `DIALOG`, `LISTITEM`, `COMBOBOX`, etc.
- Accessible name is derived from: `aria-label`, `aria-labelledby`, element text, or `title` attribute.
- `getByRole()` only matches **visible, included** elements by default (respects `aria-hidden`).

**Java Example:**
```java
public class RoleLocatorDemo {

    public void roleLocatorExamples(Page page) {
        // Button by role + name
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
            .setName("Submit Payment")).click();

        // Textbox by role + name (label association)
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions()
            .setName("Card Number")).fill("4111111111111111");

        // Checkbox
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions()
            .setName("Accept Terms and Conditions")).check();

        // Heading — verify page title after navigation
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
            .setName("Dashboard").setLevel(1))).isVisible();

        // Dialog — scoped locator inside modal
        Locator dialog = page.getByRole(AriaRole.DIALOG);
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
            .setName("Confirm")).click();

        // Include hidden elements (opt-in)
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
            .setName("Hidden Action").setIncludeHidden(true));
    }
}
```

**Real-world Usage:**
- In a FinTech application undergoing accessibility audit, the QA team uses `getByRole()` locators to simultaneously validate automation flows AND ensure ARIA roles are correctly implemented — reducing separate accessibility testing effort.

**Common Mistakes:**
- Not using the `setName()` option when multiple elements share a role — triggers strict mode violations.
- Confusing HTML element roles (e.g., `<div role="button">`) vs semantic elements (`<button>`) — both work with `getByRole()`.

**Optimization Tip:**
- Pair `getByRole()` with Playwright's `expect(locator).toHaveAccessibleName()` assertion to create tests that double as accessibility validation.

**Debugging Strategy:**
- Open browser DevTools → Accessibility tab → inspect the accessibility tree to find the correct role and name. Or use `npx playwright codegen` to auto-suggest role locators.

**Tricky Follow-up Questions:**
1. A `<div>` is styled to look like a button but has no `role="button"` attribute — does `getByRole(BUTTON)` find it?
2. After a React re-render, the ARIA name changes dynamically — how do you write a resilient locator for this?

---

## Q10: How do you handle Shadow DOM in Playwright Java?

**Answer:**
- Playwright **automatically pierces Shadow DOM** — no special syntax needed.
- CSS selectors and locators work transparently across shadow boundaries.
- This is a major advantage over Selenium, which requires JavaScript execution to access shadow roots.

**Explanation:**
- Shadow DOM creates encapsulated DOM trees attached to custom web components.
- Playwright's locator engine resolves through shadow roots automatically.
- `>>>` (deep combinator) is supported for explicit shadow DOM traversal in CSS.

**Java Example:**
```java
public class ShadowDomDemo {

    public void shadowDomHandling(Page page) {
        // Playwright auto-pierces shadow DOM — just use normal locators
        page.navigate("https://app-with-web-components.com");

        // This works even if #user-input is inside a shadow root
        page.locator("#user-input").fill("John Doe");

        // Explicit shadow DOM pierce using >>> combinator
        page.locator("my-custom-form >>> input[name='email']").fill("test@example.com");

        // Inside shadow root via getByRole
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
            .setName("Submit")).click();

        // Nested shadow DOM (multiple levels)
        page.locator("app-root >>> user-profile >>> button.edit").click();
    }

    public void verifyCustomComponentState(Page page) {
        // Verify text inside a web component's shadow DOM
        assertThat(page.locator("order-status >>> .status-label"))
            .hasText("Confirmed");
    }
}
```

**Real-world Usage:**
- In an enterprise portal built with Lit/StencilJS web components, all UI elements live in shadow DOM. Playwright's automatic shadow piercing means QA writes standard locators — no JavaScript gymnastics required.

**Common Mistakes:**
- Trying to use `document.querySelector()` style JS injection (Selenium habit) — unnecessary in Playwright.
- Assuming `>>>` is always needed — plain locators work most of the time.

**Optimization Tip:**
- Create a component-specific Page Object per web component (Component Object Model) to encapsulate shadow DOM locators and expose clean business-level methods.

**Debugging Strategy:**
- In browser DevTools → Elements panel, expand `#shadow-root` nodes to understand the DOM structure. Playwright Inspector also visualizes shadow DOM elements.

**Tricky Follow-up Questions:**
1. A closed shadow root (`{mode: 'closed'}`) — can Playwright access it? How?
2. If a web component emits custom events instead of standard DOM events, how do you interact with it in Playwright?

---

## Q11: How do you handle iFrames in Playwright Java?

**Answer:**
- Use `page.frameLocator()` to create a scoped locator targeting elements inside an iframe.
- `frameLocator()` returns a `FrameLocator` — all subsequent locator calls are scoped within that frame.
- Playwright handles frame attachment/detachment automatically.

**Explanation:**
- Unlike Selenium's `driver.switchTo().frame()`, Playwright doesn't require switching — `frameLocator()` creates a static reference.
- Nested iframes are supported via chained `frameLocator()` calls.
- Cross-origin iframes are handled transparently.

**Java Example:**
```java
public class IframeDemo {

    public void handleIframe(Page page) {
        // Locate by CSS selector
        FrameLocator frame = page.frameLocator("#payment-iframe");
        frame.locator("input[name='cardNumber']").fill("4111111111111111");
        frame.locator("input[name='cvv']").fill("123");
        frame.locator("button[type='submit']").click();
    }

    public void handleDynamicIframe(Page page) {
        // Locate frame by src URL pattern
        FrameLocator frame = page.frameLocator("iframe[src*='payment-gateway.com']");
        frame.getByLabel("Card Number").fill("4111111111111111");
        frame.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions()
            .setName("Pay Now")).click();
    }

    public void handleNestedIframe(Page page) {
        // Nested iframes
        FrameLocator outerFrame = page.frameLocator("#outer-frame");
        FrameLocator innerFrame = outerFrame.frameLocator("#inner-frame");
        innerFrame.locator(".confirmation-code").textContent();
    }

    public void handleAllFrames(Page page) {
        // Get all frames
        for (Frame frame : page.frames()) {
            System.out.println("Frame URL: " + frame.url());
        }
    }
}
```

**Real-world Usage:**
- In a FinTech checkout page, the payment form lives in a Stripe iframe. `frameLocator("iframe[src*='stripe.com']")` is used to fill card details without switching context.

**Common Mistakes:**
- Using `page.frame("frameName")` and calling Playwright locator methods on it — `Frame` API has different method signatures than `FrameLocator`.
- Forgetting that iframes on different origins can have restrictions — but Playwright handles most cross-origin scenarios.

**Optimization Tip:**
- Encapsulate iframe interactions in a dedicated utility class (e.g., `PaymentFrameComponent`) that returns scoped `FrameLocator` actions — keeps Page Objects clean.

**Debugging Strategy:**
- Use `page.frames()` to list all frames and verify the frame is attached. If `frameLocator()` times out, the iframe may not have loaded — check network tab for frame src loading.

**Tricky Follow-up Questions:**
1. The payment iframe is loaded lazily (triggered by scrolling) — how do you ensure the frame is available before interacting with it?
2. How do you validate text/state inside an iframe without executing JavaScript?

---

## Q12: How do you handle multiple tabs/windows in Playwright Java?

**Answer:**
- Use `context.waitForPage()` or `page.waitForPopup()` to capture newly opened tabs/windows.
- New pages open within the same `BrowserContext`, so they share session/cookies.
- After capture, interact with the new page as a normal `Page` object.

**Explanation:**
- When a click opens a new tab (`target="_blank"`), Playwright emits a `page` event on the `BrowserContext`.
- `context.waitForPage()` captures the next new page synchronously with the triggering action.
- Both pages remain available simultaneously — you can switch between them freely.

**Java Example:**
```java
public class MultiTabDemo {

    public void handleNewTab(Page page, BrowserContext context) {
        // Capture new tab opened by clicking a link
        Page newPage = context.waitForPage(() -> {
            page.locator("a[target='_blank']").click();
        });

        newPage.waitForLoadState(); // Wait for new tab to fully load
        System.out.println("New Tab URL: " + newPage.url());

        // Validate content in new tab
        assertThat(newPage.locator("h1")).hasText("Report Details");

        // Close new tab and return to original
        newPage.close();
        page.bringToFront(); // bring original tab to focus
    }

    public void handlePopupWindow(Page page) {
        // Popup via window.open()
        Page popup = page.waitForPopup(() -> {
            page.locator("#open-popup-btn").click();
        });

        popup.waitForLoadState();
        popup.getByLabel("Search").fill("query");
        popup.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
            .setName("Search")).click();

        // Pass data back — interact with both pages
        String result = popup.locator(".result-title").textContent();
        popup.close();

        page.locator("#search-result").fill(result);
    }
}
```

**Real-world Usage:**
- In an enterprise document management system, clicking "Preview" opens a new tab with a PDF viewer. `context.waitForPage()` captures the tab, validates the document URL pattern, and closes it before proceeding.

**Common Mistakes:**
- Calling `click()` and then `context.waitForPage()` sequentially — the new page may open and be missed before `waitForPage()` starts listening. Always wrap the triggering action inside `waitForPage()`.
- Assuming new pages in a new `BrowserContext` — they are within the SAME context as the triggering page.

**Optimization Tip:**
- Build a `TabManager` utility that maintains a stack of open pages and provides `switchTo(index)`, `closeCurrentTab()`, and `getCurrentPage()` methods for clean multi-tab test flows.

**Debugging Strategy:**
- Use `context.pages()` to list all currently open pages and their URLs. If `waitForPage()` times out, confirm the click actually triggers a new tab (vs AJAX navigation in same tab).

**Tricky Follow-up Questions:**
1. A link opens a new tab in Chromium but navigates in-page in WebKit — how do you write cross-browser compatible multi-tab tests?
2. If the new tab performs a redirect chain before settling, how do you reliably detect the final URL?

---

## Q13: How do you handle dialogs (alert, confirm, prompt) in Playwright Java?

**Answer:**
- Register a `dialog` event handler on the `Page` before triggering the action that causes the dialog.
- The handler receives a `Dialog` object — call `dialog.accept()` or `dialog.dismiss()`.
- Unhandled dialogs are auto-dismissed by Playwright.

**Explanation:**
- Dialog types: `alert`, `confirm`, `prompt`, `beforeunload`.
- `Dialog.accept(promptText)` — for prompts, passes text input.
- Event registration must happen **before** the dialog is triggered.
- Playwright processes dialogs asynchronously — event listener fires when dialog appears.

**Java Example:**
```java
public class DialogHandlingDemo {

    public void handleAlert(Page page) {
        // Register handler before triggering
        page.onDialog(dialog -> {
            assertThat(dialog.message()).contains("Are you sure");
            dialog.accept();
        });
        page.locator("#delete-btn").click();
    }

    public void handleConfirmDismiss(Page page) {
        page.onDialog(dialog -> {
            System.out.println("Dialog type: " + dialog.type()); // "confirm"
            dialog.dismiss(); // Click Cancel
        });
        page.locator("#archive-btn").click();
    }

    public void handlePrompt(Page page) {
        page.onDialog(dialog -> {
            // Supply text to prompt input
            dialog.accept("Reason for rejection: Invalid documents");
        });
        page.locator("#reject-btn").click();
    }

    public void handleBeforeUnload(Page page) {
        // Accept the "Leave page?" dialog when navigating away
        page.onDialog(dialog -> dialog.accept());
        page.navigate("https://other-page.com"); // triggers beforeunload
    }

    public void captureDialogMessage(Page page) {
        // Capture dialog text for assertion
        List<String> messages = new ArrayList<>();
        page.onDialog(dialog -> {
            messages.add(dialog.message());
            dialog.accept();
        });

        page.locator("#submit-form").click();
        assertThat(messages).containsExactly("Form submitted successfully!");
    }
}
```

**Real-world Usage:**
- In a claims processing application, deleting a claim triggers a `confirm` dialog. Tests validate the dialog message text before accepting — ensuring the correct warning is shown for each deletion type.

**Common Mistakes:**
- Registering the dialog handler after triggering the action — by then, the dialog may already be shown and auto-dismissed.
- Not removing one-time handlers — if the handler persists across test methods in a shared Page instance, it will fire on unintended dialogs.

**Optimization Tip:**
- Create a `DialogCapture` utility that registers a one-time handler using `page.onceDialog()` and returns the captured `Dialog` for assertions — keeps test code clean.

**Debugging Strategy:**
- If a dialog is unexpected and your test hangs, check browser console logs — JavaScript errors often trigger `alert()` in older apps. Use `page.onConsoleMessage()` to capture JS errors.

**Tricky Follow-up Questions:**
1. A `beforeunload` dialog appears during browser/tab close in CI — tests hang waiting for the dialog. How do you prevent this in headless mode?
2. A page uses a custom modal (not native browser dialog) for confirmations — does `onDialog` handle it?

---

## Q14: How do you handle file uploads in Playwright Java?

**Answer:**
- Use `locator.setInputFiles()` for `<input type="file">` elements.
- For drag-and-drop upload zones or custom upload widgets, use `page.dispatchEvent()` with `FileChooser`.
- Supports single file, multiple files, and buffer-based uploads (in-memory files).

**Explanation:**
- `setInputFiles()` directly sets files on a file input — no OS dialog interaction needed.
- `page.waitForFileChooser()` captures file chooser dialogs triggered by button clicks.
- Supports `Path` (local files), byte arrays (generated test data), and clearing selections.

**Java Example:**
```java
public class FileUploadDemo {

    public void uploadSingleFile(Page page) {
        // Direct input file upload
        page.locator("input[type='file']")
            .setInputFiles(Paths.get("src/test/resources/sample-document.pdf"));
    }

    public void uploadMultipleFiles(Page page) {
        page.locator("input[type='file'][multiple]").setInputFiles(new Path[]{
            Paths.get("src/test/resources/invoice-1.pdf"),
            Paths.get("src/test/resources/invoice-2.pdf"),
            Paths.get("src/test/resources/invoice-3.pdf")
        });
    }

    public void uploadInMemoryFile(Page page) {
        // Upload dynamically generated file content (no disk I/O)
        page.locator("input[type='file']").setInputFiles(
            new FilePayload("generated-report.csv",
                "text/csv",
                "AccountID,Amount,Date\n001,5000,2024-01-15".getBytes())
        );
    }

    public void handleFileChooserDialog(Page page) {
        // For custom upload buttons that open OS file dialog
        FileChooser fileChooser = page.waitForFileChooser(() -> {
            page.locator("#upload-document-btn").click();
        });
        fileChooser.setFiles(Paths.get("src/test/resources/claim-form.pdf"));
    }

    public void clearFileInput(Page page) {
        // Clear previously selected files
        page.locator("input[type='file']").setInputFiles(new Path[0]);
    }
}
```

**Real-world Usage:**
- In an insurance claims system, documents (PDF/JPG) are uploaded as part of claim submission. Tests upload in-memory files with controlled content to validate file type restrictions, size limits, and virus scan responses without needing physical test files.

**Common Mistakes:**
- Using browser `click()` on a file input and expecting an OS dialog to appear — doesn't work in headless mode. Always use `setInputFiles()` or `waitForFileChooser()`.
- Not waiting for upload completion before proceeding — check for a success indicator or network request.

**Optimization Tip:**
- Use `FilePayload` (in-memory) for test data that can be generated programmatically. This avoids managing test fixture files and enables dynamic content (e.g., unique file names per test run).

**Debugging Strategy:**
- After `setInputFiles()`, verify the upload was accepted by checking: file name displayed in UI, upload progress bar completion, or a POST request to the upload endpoint (via network listener).

**Tricky Follow-up Questions:**
1. The upload endpoint returns a `403 Forbidden` for files over 5MB — how do you generate a large in-memory test file to validate this error handling?
2. A drag-and-drop upload zone doesn't use `<input type="file">` — it listens for `dragover`/`drop` events. How do you simulate this in Playwright?

---

## Q15: How do you handle file downloads in Playwright Java?

**Answer:**
- Use `page.waitForDownload()` to capture the download triggered by a user action.
- Playwright saves the file to a temp location — use `download.path()` to get the local path.
- Validate file name, size, MIME type, and content programmatically.

**Explanation:**
- Downloads are triggered by `<a download>`, server-set `Content-Disposition: attachment`, or programmatic `window.location` navigation.
- `waitForDownload()` must wrap the triggering action to avoid missing the event.
- Downloads are saved to a temp location by default; use `download.saveAs()` to persist.

**Java Example:**
```java
public class FileDownloadDemo {

    public void downloadAndValidateFile(Page page) throws IOException {
        // Capture download triggered by button click
        Download download = page.waitForDownload(() -> {
            page.locator("#export-report-btn").click();
        });

        // Validate file name
        assertThat(download.suggestedFilename()).matches("report-\\d{8}\\.csv");

        // Save to a specific location
        Path savedPath = Paths.get("src/test/resources/downloads/report.csv");
        download.saveAs(savedPath);

        // Validate file content
        String content = Files.readString(savedPath);
        assertThat(content).contains("AccountID,Amount,Date");

        // Validate file size
        assertThat(Files.size(savedPath)).isGreaterThan(0L);
    }

    public void downloadWithContextConfig(Browser browser) {
        // Enable downloads explicitly (important in some contexts)
        BrowserContext context = browser.newContext(
            new Browser.NewContextOptions()
                .setAcceptDownloads(true)
        );
        Page page = context.newPage();
        page.navigate("https://reports.example.com");

        Download download = page.waitForDownload(() -> {
            page.locator("button[data-action='download-csv']").click();
        });

        assertThat(download.failure()).isNull(); // Verify no download error
        System.out.println("Downloaded: " + download.suggestedFilename());
    }

    public void validatePdfDownload(Page page) throws IOException {
        Download download = page.waitForDownload(() -> {
            page.locator("#download-invoice").click();
        });

        Path filePath = download.path();
        byte[] pdfBytes = Files.readAllBytes(filePath);

        // Validate PDF magic bytes
        assertThat(new String(Arrays.copyOf(pdfBytes, 4))).isEqualTo("%PDF");
    }
}
```

**Real-world Usage:**
- In an enterprise reporting platform, "Export to CSV" is tested end-to-end: trigger download, validate file name format, parse CSV headers, and verify row count matches the UI-displayed record count.

**Common Mistakes:**
- Not setting `setAcceptDownloads(true)` in context options — downloads may be blocked silently.
- Calling `download.path()` before the download completes — it blocks until complete, but not handling the null case when download fails.
- Not cleaning up downloaded files between test runs in CI — disk fills up over time.

**Optimization Tip:**
- Add download cleanup to `@AfterEach` / test teardown. Create a `DownloadValidator` utility that encapsulates file type detection, size checks, and content parsing — reusable across download scenarios.

**Debugging Strategy:**
- Check `download.failure()` — returns an error string if the download failed. Also monitor network requests to the download endpoint using `page.routeFromHAR()` or request listeners to verify the server response.

**Tricky Follow-up Questions:**
1. A "Download" link opens the file in a new browser tab instead of triggering a download — how do you handle this in Playwright?
2. In a CI/CD pipeline with Docker, downloaded files land in a container path — how do you extract and verify them in the pipeline logs?

---

*— End of Q1–Q15 | Section 1: Theory Questions —*

---

## Q16: How do you reuse authentication state in Playwright Java?

**Answer:**
- Save the authenticated `BrowserContext` state (cookies + localStorage + sessionStorage) to a JSON file using `context.storageState()`.
- Inject it into new contexts via `Browser.NewContextOptions().setStorageStatePath()`.
- This eliminates repeated login flows across tests — a major performance win.

**Explanation:**
- Auth state includes: cookies, `localStorage`, `sessionStorage`.
- Login once in a global setup, save state, reuse across the entire test suite.
- Each test context loads the pre-authenticated state — tests start directly on secured pages.

**Java Example:**
```java
public class AuthStateManager {

    private static final Path AUTH_STATE_PATH = Paths.get("src/test/resources/auth/user-state.json");

    // Run once — global setup
    public static void saveAuthState(Browser browser) {
        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        page.navigate("https://app.example.com/login");
        page.getByLabel("Email").fill("admin@company.com");
        page.getByLabel("Password").fill(System.getenv("TEST_PASSWORD")); // No hardcoded secrets
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        page.waitForURL("**/dashboard");

        // Save auth state to file
        context.storageState(new BrowserContext.StorageStateOptions()
            .setPath(AUTH_STATE_PATH));
        context.close();
    }

    // Reuse in each test
    public static BrowserContext createAuthenticatedContext(Browser browser) {
        return browser.newContext(
            new Browser.NewContextOptions()
                .setStorageStatePath(AUTH_STATE_PATH)
        );
    }
}

// In test
@Test
public void testDashboard() {
    BrowserContext context = AuthStateManager.createAuthenticatedContext(browser);
    Page page = context.newPage();
    page.navigate("https://app.example.com/dashboard");
    // Already authenticated — no login needed
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
        .setName("Dashboard"))).isVisible();
}
```

**Real-world Usage:**
- In a SaaS platform with 200+ tests, login takes 3 seconds each. Without auth state reuse: 200 × 3s = 10 min overhead. With reuse: login runs once globally — test suite time reduced by 10 minutes.

**Common Mistakes:**
- Hardcoding credentials in test code — use environment variables or a secrets vault.
- Reusing a single auth state file across users — create separate state files per user role (`admin-state.json`, `viewer-state.json`, `manager-state.json`).
- Not refreshing the auth state when tokens expire — add TTL checks in global setup.

**Optimization Tip:**
- Create role-based auth state files as part of a one-time global setup phase. Store them in a CI-accessible location and cache them between pipeline runs (until token expiry).

**Debugging Strategy:**
- If auth state is rejected (redirected back to login), inspect the JSON file — check cookie expiry fields. Re-run global setup to refresh. Add a `page.url()` assertion after navigation to detect silent auth failures.

**Tricky Follow-up Questions:**
1. The app uses short-lived JWTs (15-minute expiry) — auth state becomes invalid mid-suite. How do you handle token refresh without re-running full login?
2. How do you manage auth state for a multi-tenant application where each tenant has a different SSO provider?

---

## Q17: How do you manage cookies and storage state in Playwright Java?

**Answer:**
- `context.cookies()` — get all cookies; `context.addCookies()` — set cookies; `context.clearCookies()` — clear all.
- `page.evaluate()` — access `localStorage` / `sessionStorage` directly.
- `context.storageState()` — export full state snapshot (cookies + storage).

**Explanation:**
- Cookies are scoped to the `BrowserContext` — not the `Page`.
- `localStorage` and `sessionStorage` are scoped to origins within Pages.
- Full state can be exported/imported as JSON for auth reuse or test state seeding.

**Java Example:**
```java
public class CookieStorageDemo {

    public void cookieOperations(BrowserContext context) {
        // Add cookies (e.g., bypass cookie consent banner)
        context.addCookies(Arrays.asList(
            new Cookie("cookie_consent", "accepted")
                .setDomain(".example.com")
                .setPath("/"),
            new Cookie("session_id", "abc123xyz")
                .setDomain("app.example.com")
                .setPath("/")
                .setHttpOnly(true)
                .setSecure(true)
        ));

        // Read cookies
        List<Cookie> cookies = context.cookies();
        cookies.forEach(c -> System.out.println(c.name + "=" + c.value));

        // Filter cookies for a specific URL
        List<Cookie> sessionCookies = context.cookies(List.of("https://app.example.com"));

        // Clear all cookies
        context.clearCookies();
    }

    public void localStorageOperations(Page page) {
        // Set localStorage value
        page.evaluate("localStorage.setItem('theme', 'dark')");

        // Read localStorage value
        String theme = (String) page.evaluate("localStorage.getItem('theme')");

        // Clear localStorage
        page.evaluate("localStorage.clear()");

        // Seed feature flags for test
        page.evaluate("window.localStorage.setItem('featureFlags', JSON.stringify({newUI: true}))");
        page.reload(); // Apply seeded flags
    }

    public void exportImportState(BrowserContext context) {
        // Export current state
        String stateJson = context.storageState();
        System.out.println("State: " + stateJson);

        // Export to file
        context.storageState(new BrowserContext.StorageStateOptions()
            .setPath(Paths.get("state-snapshot.json")));
    }
}
```

**Real-world Usage:**
- In an e-commerce platform, cookie consent banners and GDPR dialogs are bypassed by pre-setting consent cookies at the context level — tests focus on business flows, not banner dismissal.

**Common Mistakes:**
- Setting cookies on `Page` instead of `BrowserContext` — there is no `page.addCookies()`. Cookies belong to the context.
- Not setting the correct `domain` on cookies — cookies without a matching domain are silently ignored.

**Optimization Tip:**
- Create a `CookieSeeder` utility that reads cookie configurations from a JSON fixture file and applies them to a context before tests — enables environment-specific cookie configurations (dev/staging/prod).

**Debugging Strategy:**
- `context.cookies()` before an action to inspect current state. In browser DevTools → Application → Cookies to visually verify. If a cookie isn't being applied, check `secure` flag requirements (HTTPS only).

**Tricky Follow-up Questions:**
1. A cookie has `SameSite=Strict` — when does this cause issues in automated tests, and how do you handle cross-origin redirects?
2. How do you verify that sensitive cookies (session tokens) have `HttpOnly` and `Secure` flags set — as a security test?

---

## Q18: How does network interception work in Playwright Java?

**Answer:**
- Use `page.route()` to intercept, modify, block, or mock network requests.
- The route handler receives a `Route` object — call `route.fulfill()`, `route.abort()`, or `route.continue_()`.
- Supports URL patterns, glob patterns, and `java.util.regex.Pattern`.

**Explanation:**
- `route.fulfill()` — return a mock response (status code, headers, body).
- `route.abort()` — simulate network failure (timeout, connection refused).
- `route.continue_()` — pass the request through with optional modifications (headers, body).
- Routes are evaluated in reverse registration order — last registered takes priority.

**Java Example:**
```java
public class NetworkInterceptionDemo {

    public void mockApiResponse(Page page) {
        // Intercept API call and return mock data
        page.route("**/api/v1/accounts/**", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("""
                    {
                        "accountId": "ACC-001",
                        "balance": 50000.00,
                        "currency": "USD",
                        "status": "ACTIVE"
                    }
                    """)
            );
        });

        page.navigate("https://app.example.com/accounts/ACC-001");
        assertThat(page.locator(".account-balance")).hasText("$50,000.00");
    }

    public void simulateNetworkFailure(Page page) {
        // Test error handling when API is down
        page.route("**/api/v1/payments", route ->
            route.abort("connectionrefused")
        );

        page.locator("#pay-btn").click();
        assertThat(page.locator(".error-banner"))
            .hasText("Payment service unavailable. Please try again.");
    }

    public void modifyRequestHeaders(Page page) {
        // Inject auth header into all API calls
        page.route("**/api/**", route -> {
            Map<String, String> headers = new HashMap<>(route.request().headers());
            headers.put("X-Test-Mode", "true");
            headers.put("X-Tenant-ID", "tenant-123");
            route.continue_(new Route.ContinueOptions().setHeaders(headers));
        });
    }

    public void captureRequests(Page page) {
        List<String> capturedUrls = new ArrayList<>();
        page.onRequest(request -> {
            if (request.url().contains("/api/")) {
                capturedUrls.add(request.url());
            }
        });
        page.onResponse(response -> {
            if (response.url().contains("/api/") && response.status() != 200) {
                System.err.println("API error: " + response.status() + " " + response.url());
            }
        });
    }
}
```

**Real-world Usage:**
- In a FinTech app, third-party payment gateway calls are intercepted and mocked in test environments. This eliminates test charges on real payment providers and enables testing of payment failure scenarios (insufficient funds, card declined) on demand.

**Common Mistakes:**
- Forgetting to call `route.fulfill()`, `route.abort()`, or `route.continue_()` — the request hangs indefinitely.
- Using `page.route()` after the page has already made the request — routes must be registered before navigation.

**Optimization Tip:**
- Build a `MockApiRegistry` that loads mock definitions from JSON fixture files. This externalizes mock data from test code and enables reuse across multiple test classes without code duplication.

**Debugging Strategy:**
- Add `System.out.println(route.request().url())` inside route handlers during development. Use `page.onRequest()` + `page.onResponse()` listeners to audit all network traffic during test execution.

**Tricky Follow-up Questions:**
1. You route `**/api/**` to a mock, but one specific endpoint should still hit the real server — how do you implement this selective passthrough?
2. A request is intercepted and modified, but the server still returns a 400 because of a missing required header — how do you debug the actual request being sent?

---

## Q19: How do you mock APIs using Playwright Java? What patterns work best?

**Answer:**
- Use `page.route()` with `route.fulfill()` for inline mocks.
- Load mock responses from JSON fixture files for maintainability.
- Use `context.routeFromHAR()` for recording and replaying real API interactions.

**Explanation:**
- Three mocking strategies:
  1. **Inline mocks** — hardcoded response in `route.fulfill()` (quick, good for simple cases)
  2. **Fixture-based mocks** — response loaded from JSON files (maintainable, reusable)
  3. **HAR-based mocks** — recorded real responses replayed (`routeFromHAR`) (most realistic)
- `route.fulfill()` supports: status, headers, body (string/bytes), `contentType`, `path` (file).

**Java Example:**
```java
public class ApiMockingDemo {

    // Strategy 1: Inline mock
    public void inlineMock(Page page) {
        page.route("**/api/v1/portfolio/summary", route ->
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("{\"totalValue\": 125000, \"holdings\": 12}")
            )
        );
    }

    // Strategy 2: Fixture file mock
    public void fixtureMock(Page page) {
        page.route("**/api/v1/transactions*", route ->
            route.fulfill(new Route.FulfillOptions()
                .setPath(Paths.get("src/test/resources/mocks/transactions-response.json"))
            )
        );
    }

    // Strategy 3: HAR replay
    public void harBasedMock(BrowserContext context) throws IOException {
        // Record once: context.routeFromHAR(path, options with update=true)
        // Replay in tests:
        context.routeFromHAR(
            Paths.get("src/test/resources/har/portfolio-flows.har"),
            new BrowserContext.RouteFromHAROptions()
                .setNotFound(HarNotFound.FALLBACK) // real network for unmatched requests
        );
    }

    // Strategy 4: Conditional mock — specific scenario simulation
    public void conditionalMock(Page page) {
        page.route("**/api/v1/payment", route -> {
            String body = route.request().postData();
            if (body != null && body.contains("\"amount\":999999")) {
                // Simulate declined payment for large amount
                route.fulfill(new Route.FulfillOptions()
                    .setStatus(402)
                    .setContentType("application/json")
                    .setBody("{\"error\": \"INSUFFICIENT_FUNDS\", \"code\": \"PAY_001\"}")
                );
            } else {
                route.continue_(); // Let real requests through
            }
        });
    }
}
```

**Real-world Usage:**
- In an enterprise trading platform, market data API calls (real-time prices) are replaced with fixture mocks in QA/staging environments. HAR recording captures a single live session, and tests replay it consistently — eliminating flakiness from live price fluctuations.

**Common Mistakes:**
- Building mocks that are too perfect — real APIs return rate limit errors, partial data, pagination. Mocks should reflect realistic scenarios.
- Not versioning mock fixture files alongside the API contract — mocks become stale when APIs change.

**Optimization Tip:**
- Adopt **Contract-Driven Mocking**: derive mock responses from OpenAPI/Swagger specs. Use a tool like WireMock alongside Playwright to manage complex mock scenarios in a centralized mock server.

**Debugging Strategy:**
- Log intercepted requests and the mock response being returned. Compare mock response structure with what the UI expects — mismatched JSON keys are a common source of mock-related test failures.

**Tricky Follow-up Questions:**
1. HAR file was recorded in staging — it contains real bearer tokens that expire. How do you make HAR-based mocks work after token expiry?
2. `routeFromHAR()` serves a cached response, but the test needs to verify the UI handles a 500 error for a specific call — how do you override a single entry in a HAR file?

---

## Q20: How do you validate requests and responses in Playwright Java?

**Answer:**
- Use `page.onRequest()` and `page.onResponse()` event listeners to capture and validate network traffic.
- Use `route.request()` inside a route handler to inspect request details before fulfilling.
- Use `response.json()` to parse and assert response body.

**Explanation:**
- `Request` object: `url()`, `method()`, `headers()`, `postData()`, `postDataJSON()`, `resourceType()`.
- `Response` object: `url()`, `status()`, `headers()`, `body()`, `json()`, `ok()`.
- `page.waitForResponse()` — waits for a specific response matching a URL pattern, returns the `Response`.

**Java Example:**
```java
public class RequestResponseValidationDemo {

    public void validatePostRequest(Page page) {
        // Capture and validate a POST request payload
        page.onRequest(request -> {
            if (request.url().contains("/api/v1/orders") && "POST".equals(request.method())) {
                Map<String, Object> body = (Map<String, Object>) request.postDataJSON();
                assertThat(body.get("customerId")).isNotNull();
                assertThat((Double) body.get("amount")).isGreaterThan(0);
            }
        });
    }

    public void waitForAndValidateResponse(Page page) {
        // Trigger action and wait for specific API response
        Response response = page.waitForResponse(
            resp -> resp.url().contains("/api/v1/orders") && resp.status() == 201,
            () -> page.locator("#place-order-btn").click()
        );

        assertThat(response.status()).isEqualTo(201);
        Map<String, Object> body = (Map<String, Object>) response.json();
        assertThat(body.get("orderId")).isNotNull();
        assertThat(body.get("status")).isEqualTo("CONFIRMED");
    }

    public void validateRequestHeaders(Page page) {
        page.onRequest(request -> {
            if (request.url().contains("/api/")) {
                // Validate auth header present on all API calls
                assertThat(request.headers()).containsKey("authorization");
                assertThat(request.headers().get("authorization"))
                    .startsWith("Bearer ");
                // Validate required headers
                assertThat(request.headers()).containsKey("x-correlation-id");
            }
        });
    }

    public void captureAllApiCalls(Page page) {
        List<Map<String, Object>> apiLog = new ArrayList<>();
        page.onResponse(response -> {
            if (response.url().contains("/api/")) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("url", response.url());
                entry.put("status", response.status());
                entry.put("timing", response.request().timing());
                apiLog.add(entry);
            }
        });

        page.navigate("https://app.example.com/dashboard");

        // Verify no API errors occurred during page load
        apiLog.stream()
            .filter(e -> (int) e.get("status") >= 400)
            .forEach(e -> fail("API error: " + e.get("url") + " -> " + e.get("status")));
    }
}
```

**Real-world Usage:**
- In a regulated FinTech application, every API call must include `X-Correlation-ID` and `X-Audit-User` headers for compliance. Tests validate these headers are present on every request — combining UI flow testing with API compliance validation.

**Common Mistakes:**
- Calling `response.body()` or `response.json()` inside `onResponse()` listener — this can block if called synchronously. Use `page.waitForResponse()` instead for response body access.
- Not filtering requests by URL pattern — `onRequest` fires for ALL resources (images, CSS, fonts) — adds noise and slows validation.

**Optimization Tip:**
- Build a `NetworkAuditHelper` that auto-validates API contract conformance (status codes, mandatory headers, response structure) on every test run — creates a passive API regression layer alongside UI tests.

**Debugging Strategy:**
- Use `page.request().fetch()` to make direct API calls from within the browser context and inspect responses independently of UI interactions — useful for isolating API vs UI failures.

**Tricky Follow-up Questions:**
1. A response body is gzip-compressed — `response.body()` returns bytes. How do you decompress and parse it in Java?
2. You want to validate that a specific API call is NOT made when a feature flag is disabled — how do you assert the absence of a request?

---

## Q21: How do you use APIRequestContext in Playwright Java for REST API automation?

**Answer:**
- `APIRequestContext` is Playwright's built-in HTTP client for REST API testing.
- Obtained via `playwright.request().newContext()` or `page.request()` (shares browser session cookies).
- Supports GET, POST, PUT, PATCH, DELETE with full header, body, and auth management.

**Explanation:**
- `playwright.request().newContext()` — standalone HTTP client, independent of browser.
- `page.request()` — shares cookies/auth with the browser session (great for UI+API hybrid tests).
- Automatically follows redirects, handles cookies, supports multipart forms, and JSON bodies.

**Java Example:**
```java
public class ApiRequestContextDemo {

    public void standaloneApiTest(Playwright playwright) {
        APIRequestContext request = playwright.request().newContext(
            new APIRequest.NewContextOptions()
                .setBaseURL("https://api.example.com")
                .setExtraHTTPHeaders(Map.of(
                    "Authorization", "Bearer " + System.getenv("API_TOKEN"),
                    "Content-Type", "application/json"
                ))
        );

        // GET request
        APIResponse response = request.get("/v1/accounts/ACC-001");
        assertThat(response).isOK(); // status 2xx
        Map<String, Object> account = (Map<String, Object>) response.json();
        assertThat(account.get("status")).isEqualTo("ACTIVE");

        // POST request with JSON body
        APIResponse createResponse = request.post("/v1/orders",
            RequestOptions.create()
                .setData(Map.of(
                    "customerId", "CUST-123",
                    "items", List.of(Map.of("sku", "SKU-001", "quantity", 2)),
                    "currency", "USD"
                ))
        );
        assertThat(createResponse.status()).isEqualTo(201);

        request.dispose();
    }

    public void hybridUiApiTest(Page page) {
        // Shared session — API calls use same cookies as browser
        APIRequestContext apiContext = page.request();

        // Create data via API
        APIResponse createResp = apiContext.post("https://api.example.com/v1/customers",
            RequestOptions.create().setData(Map.of("name", "Test Corp", "tier", "PREMIUM"))
        );
        String customerId = (String) ((Map<String, Object>) createResp.json()).get("id");

        // Validate in UI
        page.navigate("https://app.example.com/customers/" + customerId);
        assertThat(page.locator(".customer-tier-badge")).hasText("PREMIUM");

        // Cleanup via API
        apiContext.delete("https://api.example.com/v1/customers/" + customerId);
    }
}
```

**Real-world Usage:**
- In an enterprise SaaS test framework, UI tests use `page.request()` for test data setup and teardown (CRUD via API) while verifying the result in the browser — faster and more reliable than creating data through the UI.

**Common Mistakes:**
- Not calling `request.dispose()` for standalone contexts — causes connection pool leaks in long test suites.
- Using `playwright.request().newContext()` when you need session cookies — use `page.request()` instead.
- Ignoring SSL certificate errors in test environments without explicit configuration.

**Optimization Tip:**
- Create a typed `ApiClient` wrapper around `APIRequestContext` with methods like `createOrder()`, `deleteCustomer()`, `getAccount()` — exposes a clean business API to tests, hiding HTTP concerns.

**Debugging Strategy:**
- Enable request logging: `APIRequest.NewContextOptions().setExtraHTTPHeaders()` to add debug headers. Check `response.headers()` for rate limit info (`X-RateLimit-Remaining`). Use `response.text()` (not `.json()`) when response parsing fails — log raw body.

**Tricky Follow-up Questions:**
1. `page.request()` shares cookies — but your API requires a different auth token format than the browser session. How do you handle this cleanly?
2. The API uses mutual TLS (mTLS) — how do you configure `APIRequestContext` with a client certificate?

---

## Q22: How do you handle OAuth2 / JWT authentication in Playwright Java?

**Answer:**
- Obtain tokens directly via `APIRequestContext` (bypassing UI login) and inject into browser context.
- For OAuth2 flows: complete the token exchange via direct HTTP, inject the resulting token as a cookie or `localStorage` value.
- Never hardcode tokens — use environment variables or a secrets manager.

**Explanation:**
- Three approaches:
  1. **Direct token API** — POST to `/oauth/token` with credentials, inject token.
  2. **Auth state file** — login via UI once, save state, reuse (Q16 pattern).
  3. **Mock auth** — intercept auth endpoints and return pre-built tokens (for non-prod).
- JWTs are typically stored in `localStorage` (`access_token`) or as `HttpOnly` cookies.

**Java Example:**
```java
public class OAuthJwtDemo {

    // Approach 1: Direct token acquisition
    public String acquireToken(Playwright playwright) {
        APIRequestContext request = playwright.request().newContext(
            new APIRequest.NewContextOptions().setBaseURL("https://auth.example.com")
        );

        APIResponse tokenResponse = request.post("/oauth/token",
            RequestOptions.create().setData(Map.of(
                "grant_type", "client_credentials",
                "client_id", System.getenv("CLIENT_ID"),
                "client_secret", System.getenv("CLIENT_SECRET"),
                "scope", "read:accounts write:orders"
            ))
        );

        assertThat(tokenResponse).isOK();
        Map<String, Object> tokenBody = (Map<String, Object>) tokenResponse.json();
        request.dispose();
        return (String) tokenBody.get("access_token");
    }

    // Inject token into browser context
    public BrowserContext createAuthenticatedContext(Browser browser, String jwtToken) {
        BrowserContext context = browser.newContext(
            new Browser.NewContextOptions().setBaseURL("https://app.example.com")
        );
        Page page = context.newPage();
        page.navigate("https://app.example.com"); // must navigate first to set storage
        page.evaluate(String.format("localStorage.setItem('access_token', '%s')", jwtToken));
        return context;
    }

    // Approach 2: Cookie-based JWT injection
    public void injectJwtAsCookie(BrowserContext context, String jwtToken) {
        context.addCookies(List.of(
            new Cookie("auth_token", jwtToken)
                .setDomain(".example.com")
                .setPath("/")
                .setSecure(true)
                .setHttpOnly(true)
        ));
    }

    // Validate JWT claims in API response
    public void validateJwtClaims(Page page) {
        APIResponse profileResp = page.request().get("https://api.example.com/v1/me");
        assertThat(profileResp).isOK();
        Map<String, Object> profile = (Map<String, Object>) profileResp.json();
        assertThat(profile.get("role")).isEqualTo("ADMIN");
        assertThat(profile.get("tenantId")).isEqualTo("TENANT-001");
    }

    // Token refresh handling
    public void handleTokenRefresh(Page page) {
        page.route("**/api/**", route -> {
            APIResponse response = route.fetch();
            if (response.status() == 401) {
                // Token expired — re-acquire and retry
                String newToken = refreshToken();
                page.evaluate("localStorage.setItem('access_token', '" + newToken + "')");
                route.continue_();
            } else {
                route.fulfill(new Route.FulfillOptions().setResponse(response));
            }
        });
    }

    private String refreshToken() {
        // Token refresh logic
        return System.getenv("REFRESH_TOKEN");
    }
}
```

**Real-world Usage:**
- In a multi-tenant SaaS platform, tests acquire separate JWT tokens (via `client_credentials` grant) for each tenant. Tokens are cached per tenant and refreshed when expired — tests run across 10 tenants in parallel with no shared state.

**Common Mistakes:**
- Logging JWT tokens in test output — tokens are like passwords. Mask them in CI logs.
- Not checking token expiry before injecting — an expired token causes sporadic 401s mid-suite.
- Using `password` grant in tests — prefer `client_credentials` or a dedicated test identity provider.

**Optimization Tip:**
- Implement a `TokenCache` (singleton per test session) with TTL-aware caching. Tokens valid for >5 minutes are reused; otherwise refreshed. Thread-safe with `ConcurrentHashMap`.

**Debugging Strategy:**
- Decode JWT at `jwt.io` or via Java Base64 to inspect claims (`exp`, `scope`, `sub`). If API returns 403 (vs 401), the token is valid but missing required scopes — check the `scope` in the token request.

**Tricky Follow-up Questions:**
1. The OAuth provider uses PKCE — how does that change your token acquisition approach in an automated test context?
2. Tokens are single-use (replay prevention) — each test run needs a fresh token. How do you avoid token acquisition becoming a bottleneck in parallel execution?

---

## Q23: How do you handle parallel execution in Playwright Java? What are the thread-safety considerations?

**Answer:**
- Playwright Java is **not thread-safe** — each `Playwright` and `Browser` instance must be used by one thread only.
- For parallel execution: create one `Playwright` instance per thread using `ThreadLocal`.
- `BrowserContext` provides test isolation within the same thread.

**Explanation:**
- Playwright's Java client uses a single IPC channel per instance — concurrent access causes race conditions.
- `ThreadLocal<Playwright>` ensures each thread has its own `Playwright` + `Browser` lifecycle.
- JUnit 5 + `@TestMethodOrder` + `@Execution(ExecutionMode.CONCURRENT)` enables parallel test methods.
- Maven Surefire / Failsafe plugins can run test classes in parallel with separate JVM forks.

**Java Example:**
```java
// Thread-safe Playwright manager
public class PlaywrightThreadLocalManager {

    private static final ThreadLocal<Playwright> PLAYWRIGHT_TL = ThreadLocal.withInitial(
        Playwright::create
    );
    private static final ThreadLocal<Browser> BROWSER_TL = ThreadLocal.withInitial(() ->
        PLAYWRIGHT_TL.get().chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        )
    );

    public static Browser getBrowser() {
        return BROWSER_TL.get();
    }

    public static void closeAll() {
        BROWSER_TL.get().close();
        PLAYWRIGHT_TL.get().close();
        BROWSER_TL.remove();
        PLAYWRIGHT_TL.remove();
    }
}

// JUnit 5 base test class
@Execution(ExecutionMode.CONCURRENT)
public abstract class BaseTest {

    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    public void setUp() {
        context = PlaywrightThreadLocalManager.getBrowser().newContext();
        page = context.newPage();
    }

    @AfterEach
    public void tearDown() {
        context.close(); // Closes page too
    }

    @AfterAll
    public static void globalTearDown() {
        PlaywrightThreadLocalManager.closeAll();
    }
}
```

```xml
<!-- Maven Surefire parallel config -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <forkCount>4</forkCount>
        <reuseForks>true</reuseForks>
        <properties>
            <configurationParameters>
                junit.jupiter.execution.parallel.enabled=true
                junit.jupiter.execution.parallel.mode.default=concurrent
                junit.jupiter.execution.parallel.config.strategy=fixed
                junit.jupiter.execution.parallel.config.fixed.parallelism=4
            </configurationParameters>
        </properties>
    </configuration>
</plugin>
```

**Real-world Usage:**
- In a CI pipeline running 500 tests, parallel execution with 4 threads reduces suite time from 45 minutes to ~12 minutes. Each thread maintains its own `Playwright` + `Browser` instance; tests use independent `BrowserContext` per test.

**Common Mistakes:**
- Sharing a single `Playwright` or `Browser` instance across threads — causes `ConcurrentModificationException` or port conflicts.
- Using static `Page` objects — pages are not thread-safe. Always create per-test.
- Not cleaning up `ThreadLocal` values — causes memory leaks in thread-pool-based executors.

**Optimization Tip:**
- Use `BrowserContext` with pre-loaded auth state per thread to eliminate login overhead in parallel runs. Combine with `forkCount=1C` (one fork per CPU core) in Surefire for optimal resource use.

**Debugging Strategy:**
- When parallel tests fail with strange errors (wrong page content, mixed assertions), it's usually shared state. Add thread name to test logs: `Thread.currentThread().getName()`. Use Allure Reports to correlate failures by thread.

**Tricky Follow-up Questions:**
1. In Docker with 2 CPUs, running 8 parallel threads — how do you prevent resource exhaustion (Playwright processes, memory)?
2. Two parallel tests both create an `account` with the same `email` — causing unique constraint violations. How do you generate thread-unique test data?

---

## Q24: How do you implement retry strategies for flaky tests in Playwright Java?

**Answer:**
- Configure retries at the runner level (JUnit 5 `@RepeatedTest`, Surefire `rerunFailingTestsCount`).
- Implement custom retry logic at the action level using a `RetryUtils` wrapper.
- Use Playwright's built-in `waitFor` mechanisms before declaring retry needed.

**Explanation:**
- Flaky tests usually come from: timing issues, network variability, dynamic content, or environment instability.
- First: eliminate root cause (better locators, proper waits).
- If non-deterministic external causes remain: implement selective retries with exponential backoff.

**Java Example:**
```java
// Custom retry wrapper for Playwright actions
public class RetryUtils {

    public static <T> T retry(int maxAttempts, long delayMs, Supplier<T> action) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (PlaywrightException | AssertionError e) {
                lastException = e;
                System.err.printf("Attempt %d/%d failed: %s%n", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("All " + maxAttempts + " attempts failed", lastException);
    }

    // Retry with page refresh on failure
    public static void retryWithRefresh(Page page, int maxRetries, Runnable action) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                action.run();
                return;
            } catch (PlaywrightException | AssertionError e) {
                if (i < maxRetries - 1) {
                    page.reload();
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                } else {
                    throw e;
                }
            }
        }
    }
}

// JUnit 5 — retry at test method level
@ExtendWith(RetryExtension.class)
public class FlakyTestExample extends BaseTest {

    @Test
    @RetryOnFailure(maxRetries = 3)
    public void testDashboardLoad() {
        page.navigate("https://app.example.com/dashboard");
        RetryUtils.retry(3, 1000, () -> {
            assertThat(page.locator(".widget-loaded")).hasCount(5);
            return null;
        });
    }
}
```

```xml
<!-- Maven Surefire retry config -->
<configuration>
    <rerunFailingTestsCount>2</rerunFailingTestsCount>
</configuration>
```

**Real-world Usage:**
- In a stock trading platform, real-time price updates cause occasional widget loading delays beyond standard timeouts. The team uses 2 retries at the Surefire level with Playwright's `NETWORKIDLE` wait state — reducing flaky failures from 8% to <0.5%.

**Common Mistakes:**
- Retrying every test failure blindly — masks real bugs. Only retry for known non-deterministic failures (network timeouts, animation races).
- Not logging retry attempts — makes it impossible to distinguish "flaky but passed" from "stable pass" in reports.
- Using `Thread.sleep()` as retry delay — prefer exponential backoff with jitter.

**Optimization Tip:**
- Tag tests with `@Flaky` and track flakiness metrics in your CI dashboard. Set a flakiness threshold (e.g., >5% flake rate triggers a mandatory fix). Use Allure's retry history feature to visualize flakiness trends.

**Debugging Strategy:**
- Add screenshot + trace capture on every retry attempt. Compare screenshots from attempt 1 and attempt 2 — often reveals a timing-dependent UI state change. If failing only in CI (not local), suspect environment-specific issues: CPU throttling, slower containers, DNS resolution.

**Tricky Follow-up Questions:**
1. A test passes on retry but the underlying feature is broken intermittently — how do you distinguish "infrastructure flakiness" from "real intermittent bug"?
2. Retrying a test that creates records (e.g., submitting an order) — each retry creates duplicate data. How do you implement idempotent retries?

---

## Q25: How do you perform headless vs headed execution in Playwright Java?

**Answer:**
- Headless: `setHeadless(true)` (default) — no visible browser window, faster, CI-friendly.
- Headed: `setHeadless(false)` — visible browser, essential for local debugging and visual test development.
- Configurable via environment variable for seamless local/CI switching.

**Explanation:**
- Headless mode: browser renders off-screen. Faster, lower memory, works in environments without display (Docker, Linux CI).
- Headed mode: visible browser window. Required for: local development/debugging, `page.pause()`, recording sessions.
- Playwright's headless mode is **not the same as the old headless Chrome** — it's a full browser implementation, not a stripped-down version.

**Java Example:**
```java
public class HeadlessExecutionConfig {

    // Environment-driven headless/headed mode
    public static Browser createBrowser(Playwright playwright) {
        boolean headless = Boolean.parseBoolean(
            System.getProperty("headless", System.getenv().getOrDefault("HEADLESS", "true"))
        );

        return playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(headless ? 0 : 50) // Slow down headed mode for visibility
                .setArgs(List.of(
                    "--no-sandbox",            // Required in Docker
                    "--disable-dev-shm-usage", // Docker shared memory fix
                    "--disable-gpu"            // CI-friendly
                ))
        );
    }

    // Different browsers
    public static Browser createFirefoxBrowser(Playwright playwright) {
        return playwright.firefox().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    public static Browser createWebKitBrowser(Playwright playwright) {
        return playwright.webkit().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    // Remote browser (e.g., Selenium Grid / BrowserStack via CDP)
    public static Browser connectRemoteBrowser(Playwright playwright) {
        return playwright.chromium().connectOverCDP(
            "ws://remote-browser-host:9222"
        );
    }
}
```

**Real-world Usage:**
- In a CI/CD pipeline (Jenkins/GitHub Actions), `HEADLESS=true` is set by default. Developers run locally with `mvn test -Dheadless=false` for visual debugging. Nightly regression runs in headless mode with 4 parallel threads on a Linux Docker container.

**Common Mistakes:**
- Running headed mode in Docker — fails with "cannot open display" error. Always use headless in Docker or configure Xvfb virtual display.
- Assuming headless and headed tests always produce identical behavior — some CSS animations, hover effects, and focus behaviors differ between modes.

**Optimization Tip:**
- Use `setSlowMo()` only in headed debug sessions. In CI headless mode, `slowMo=0` is critical for performance. Add `--disable-extensions` and `--disable-popup-blocking` to Chrome args in CI for stability.

**Debugging Strategy:**
- When a test fails only in headless mode: disable headless locally to visually reproduce. Common causes: element off-screen (viewport size differs), focus-dependent interactions failing, or CSS `visibility: hidden` vs `display: none` differences.

**Tricky Follow-up Questions:**
1. A test requires hovering over an element to reveal a dropdown — in headless mode, the hover doesn't expose it. How do you debug and fix this?
2. Playwright's new "headless=new" mode (headless shell) — how does it differ from the traditional headless mode, and why might you choose it?

---

*— End of Q16–Q30 | Section 1: Theory Questions —*

---

## Q31: How do you implement cross-browser testing in Playwright Java?

**Answer:**
- Playwright supports **Chromium, Firefox, and WebKit** (Safari engine) natively — no driver management needed.
- Parameterize browser type via a factory method or JUnit 5 `@ParameterizedTest` with `@EnumSource`.
- Use a `BrowserFactory` pattern to decouple browser creation from test logic.

**Explanation:**
- Chromium covers Chrome + Edge; WebKit covers Safari on macOS/iOS; Firefox is bundled and versioned.
- Playwright's bundled browsers are tested against the framework version — no compatibility matrix to manage.
- Cross-browser tests run the same test code against all three engines — revealing browser-specific rendering or JS behavior differences.

**Java Example:**
```java
// BrowserFactory pattern
public class BrowserFactory {

    public enum BrowserType { CHROMIUM, FIREFOX, WEBKIT }

    public static Browser create(Playwright playwright, BrowserType type) {
        BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions().setHeadless(true);
        return switch (type) {
            case CHROMIUM -> playwright.chromium().launch(opts);
            case FIREFOX  -> playwright.firefox().launch(opts);
            case WEBKIT   -> playwright.webkit().launch(opts);
        };
    }
}

// JUnit 5 parameterized cross-browser test
@ParameterizedTest(name = "Browser: {0}")
@EnumSource(BrowserFactory.BrowserType.class)
public void testLoginAcrossBrowsers(BrowserFactory.BrowserType browserType) {
    try (Playwright playwright = Playwright.create()) {
        Browser browser = BrowserFactory.create(playwright, browserType);
        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        page.navigate("https://app.example.com/login");
        page.getByLabel("Email").fill("user@example.com");
        page.getByLabel("Password").fill(System.getenv("TEST_PASSWORD"));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        page.waitForURL("**/dashboard");

        assertThat(page.getByRole(AriaRole.HEADING,
            new Page.GetByRoleOptions().setName("Dashboard"))).isVisible();
        context.close();
        browser.close();
    }
}
```

**Real-world Usage:**
- In an enterprise SaaS product, the QA team runs smoke tests on all three browsers in the nightly pipeline. Full regression runs on Chromium only (fastest). Cross-browser suite executes on Chromium + Firefox; WebKit focuses on Safari-specific payment flow rendering.

**Common Mistakes:**
- Ignoring WebKit — it mimics Safari which has different CSS rendering, input masking behavior, and JS engine quirks from Chromium.
- Using `System.setProperty("browser", "firefox")` and switch-casing in base class — creates messy inheritance. Prefer factory + parameterization.

**Optimization Tip:**
- Separate cross-browser suite from main regression. Tag cross-browser tests with `@Tag("cross-browser")` and run them in a dedicated pipeline stage. Run Chromium-only tests in all PR builds for speed.

**Debugging Strategy:**
- When a test fails on Firefox/WebKit but passes on Chromium: run that specific browser in headed mode locally. Check for: CSS layout shifts, `focus()` behavior differences, Date/number formatting differences, and browser-specific event handling.

**Tricky Follow-up Questions:**
1. Playwright's WebKit is not the same as Safari on macOS — what are the known differences, and when would you still need real Safari testing?
2. A countdown timer animation behaves differently on Firefox causing assertion timing issues — how do you write browser-agnostic timing assertions?

---

## Q32: How do you implement mobile emulation in Playwright Java?

**Answer:**
- Use `Browser.NewContextOptions` with `setViewportSize()`, `setUserAgent()`, `setDeviceScaleFactor()`, `setIsMobile()`, and `setHasTouch()`.
- Use `playwright.devices()` map to load pre-defined device profiles (iPhone, Pixel, Galaxy, etc.).
- Combine with geolocation and locale for realistic mobile simulation.

**Explanation:**
- `isMobile(true)` — tells the browser to render in mobile mode (meta viewport, touch events).
- `hasTouch(true)` — enables touch event simulation (tap, swipe, pinch).
- `deviceScaleFactor` — simulates Retina/high-DPI displays.
- `playwright.devices()` provides 40+ pre-configured device profiles.

**Java Example:**
```java
public class MobileEmulationDemo {

    public void emulateIPhone(Playwright playwright, Browser browser) {
        // Use pre-defined device profile
        DeviceDescriptor iPhone = playwright.devices().get("iPhone 14");

        BrowserContext context = browser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(iPhone.viewport().width(), iPhone.viewport().height())
                .setUserAgent(iPhone.userAgent())
                .setDeviceScaleFactor(iPhone.deviceScaleFactor())
                .setIsMobile(iPhone.isMobile())
                .setHasTouch(iPhone.hasTouch())
        );

        Page page = context.newPage();
        page.navigate("https://app.example.com");

        // Tap instead of click on mobile
        page.locator(".hamburger-menu").tap();
        assertThat(page.locator(".mobile-nav")).isVisible();
    }

    public void emulateCustomDevice(Browser browser) {
        BrowserContext context = browser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(390, 844)       // iPhone 14 dimensions
                .setDeviceScaleFactor(3)
                .setIsMobile(true)
                .setHasTouch(true)
                .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)")
                .setGeolocation(new Geolocation(37.7749, -122.4194)) // San Francisco
                .setPermissions(List.of("geolocation"))
                .setLocale("en-US")
        );

        Page page = context.newPage();
        page.navigate("https://app.example.com/nearby");
        assertThat(page.locator(".location-banner")).containsText("San Francisco");
    }

    public void swipeGesture(Page page) {
        // Simulate swipe using touch events
        page.locator(".carousel").dispatchEvent("touchstart",
            Map.of("touches", List.of(Map.of("clientX", 300, "clientY", 400))));
        page.locator(".carousel").dispatchEvent("touchmove",
            Map.of("touches", List.of(Map.of("clientX", 100, "clientY", 400))));
        page.locator(".carousel").dispatchEvent("touchend", Map.of());
    }
}
```

**Real-world Usage:**
- In a retail app, mobile checkout flow is tested using iPhone 14 and Pixel 7 emulation. Responsive breakpoints, hamburger menu, touch-based carousels, and geolocation-based store locator are all validated in the same Playwright Java suite without needing real devices.

**Common Mistakes:**
- Using `setViewportSize()` alone without `setIsMobile(true)` — the app renders at narrow width but doesn't switch to mobile layout (meta viewport not respected).
- Not setting `setHasTouch(true)` — tap events may not fire correctly, causing click fallback behavior.

**Optimization Tip:**
- Create a `DeviceProfileFactory` that loads device configurations from a YAML/JSON config file. Enables QA to add/update device profiles without touching test code.

**Debugging Strategy:**
- Use `page.evaluate("window.innerWidth + 'x' + window.innerHeight")` to verify viewport is set correctly. Check `navigator.userAgent` and `navigator.maxTouchPoints` values in evaluated JS to confirm mobile mode.

**Tricky Follow-up Questions:**
1. Mobile emulation uses Chromium — but Safari on iOS has different rendering for `position: sticky` and `100vh`. How do you catch Safari-specific mobile bugs without real devices?
2. A PWA (Progressive Web App) shows an "Install App" prompt on real mobile but not in emulation — why, and how do you test the install flow?

---

## Q33: What is Playwright's visual testing approach? How do you implement screenshot-based assertions?

**Answer:**
- Playwright's `expect(page).toHaveScreenshot()` captures a screenshot and compares it pixel-by-pixel against a stored baseline.
- First run creates baselines; subsequent runs compare and fail if pixel diff exceeds threshold.
- Supports full-page, element-level, and masked region screenshots.

**Explanation:**
- Visual regression testing catches UI regressions (layout shifts, color changes, missing elements) that functional assertions miss.
- Playwright stores baselines alongside tests. Update baselines with `--update-snapshots` flag.
- Configurable: `maxDiffPixels`, `maxDiffPixelRatio`, `threshold` (color sensitivity).

**Java Example:**
```java
public class VisualTestingDemo {

    @Test
    public void testDashboardVisualRegression(Page page) {
        page.navigate("https://app.example.com/dashboard");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Full page screenshot comparison
        assertThat(page).toHaveScreenshot("dashboard-full.png",
            new PageAssertions.ToHaveScreenshotOptions()
                .setFullPage(true)
                .setMaxDiffPixelRatio(0.02) // allow 2% pixel difference
        );
    }

    @Test
    public void testComponentVisual(Page page) {
        page.navigate("https://app.example.com/portfolio");

        // Element-level snapshot — only compare the chart widget
        assertThat(page.locator(".portfolio-chart")).toHaveScreenshot("portfolio-chart.png",
            new LocatorAssertions.ToHaveScreenshotOptions()
                .setThreshold(0.1) // color sensitivity 0-1
        );
    }

    @Test
    public void testWithMaskedDynamicRegions(Page page) {
        page.navigate("https://app.example.com/orders");

        // Mask dynamic content (timestamps, IDs) to prevent false failures
        assertThat(page).toHaveScreenshot("orders-page.png",
            new PageAssertions.ToHaveScreenshotOptions()
                .setMask(List.of(
                    page.locator(".order-timestamp"),
                    page.locator(".order-id"),
                    page.locator(".live-price-ticker")
                ))
        );
    }

    // Manual screenshot capture
    public void captureScreenshot(Page page, String testName) {
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("screenshots/" + testName + ".png"))
            .setFullPage(true)
            .setType(ScreenshotType.PNG)
        );
    }
}
```

**Real-world Usage:**
- In an enterprise reporting platform, chart and table components undergo visual regression testing. Dynamic data (timestamps, live prices) is masked. Baselines are stored in Git alongside tests — PRs that change UI components trigger visual diff reports in CI for designer review.

**Common Mistakes:**
- Not masking dynamic regions (dates, IDs, prices) — screenshots fail on every run due to data changes, not real visual regressions.
- Running visual tests across OS/font rendering differences (Windows vs Linux) — baselines must be generated in the same environment they're compared in (use Docker for consistency).

**Optimization Tip:**
- Run visual tests only on `main` branch merges (not every PR) to reduce CI cost. Use a dedicated baseline branch. Consider Applitools or Percy for AI-powered visual comparison that handles minor rendering differences intelligently.

**Debugging Strategy:**
- When visual tests fail, Playwright generates a diff image highlighting changed pixels. Check: was it a real regression (layout broke) or a false positive (font rendering, anti-aliasing)? Adjust `threshold` and `maxDiffPixelRatio` to suppress known rendering noise.

**Tricky Follow-up Questions:**
1. Visual baselines differ between macOS (developer) and Linux (CI Docker) due to font rendering — how do you standardize baselines across environments?
2. How do you implement visual testing for dark mode vs light mode in the same test suite?

---

## Q34: How do you implement accessibility testing with Playwright Java?

**Answer:**
- Use `page.accessibility().snapshot()` to capture the accessibility tree as a JSON structure.
- Integrate **Axe-core** (via `com.deque.html.axe-core/selenium` or custom JS injection) for WCAG compliance scanning.
- Use `getByRole()` locators as both test selectors AND accessibility validators.

**Explanation:**
- Accessibility testing verifies: ARIA roles, labels, focus management, keyboard navigation, contrast ratios, and semantic HTML.
- Playwright's built-in `accessibility.snapshot()` exposes the browser's accessibility tree.
- Axe-core integration provides automated WCAG 2.1 AA/AAA rule scanning.

**Java Example:**
```java
public class AccessibilityTestingDemo {

    @Test
    public void testAccessibilityTree(Page page) {
        page.navigate("https://app.example.com/login");

        // Capture accessibility tree
        AccessibilityNode snapshot = page.accessibility().snapshot();
        System.out.println("A11y Tree: " + snapshot);

        // Verify specific accessible names
        assertThat(page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions()
            .setName("Email address"))).isVisible();
        assertThat(page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions()
            .setName("Password"))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
            .setName("Sign In"))).isEnabled();
    }

    @Test
    public void testAxeCoreIntegration(Page page) {
        page.navigate("https://app.example.com/dashboard");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Inject and run axe-core
        page.addScriptTag(new Page.AddScriptTagOptions()
            .setPath(Paths.get("src/test/resources/axe.min.js")));

        Map<String, Object> axeResults = (Map<String, Object>) page.evaluate("""
            async () => {
                const results = await axe.run();
                return {
                    violations: results.violations,
                    passes: results.passes.length,
                    incomplete: results.incomplete.length
                };
            }
        """);

        List<Map<String, Object>> violations = (List<Map<String, Object>>) axeResults.get("violations");
        if (!violations.isEmpty()) {
            StringBuilder report = new StringBuilder("Accessibility violations:\n");
            violations.forEach(v -> report.append("- ").append(v.get("id"))
                .append(": ").append(v.get("description")).append("\n"));
            fail(report.toString());
        }
    }

    @Test
    public void testKeyboardNavigation(Page page) {
        page.navigate("https://app.example.com/form");

        // Tab through form fields
        page.keyboard().press("Tab");
        assertThat(page.locator(":focus")).hasAttribute("name", "firstName");

        page.keyboard().press("Tab");
        assertThat(page.locator(":focus")).hasAttribute("name", "lastName");

        // Enter submits form when button is focused
        page.keyboard().press("Tab"); page.keyboard().press("Tab");
        page.keyboard().press("Enter");
        assertThat(page.locator(".success-message")).isVisible();
    }
}
```

**Real-world Usage:**
- A government-sector enterprise application must meet WCAG 2.1 AA compliance. Playwright + Axe-core is integrated into the CI pipeline — accessibility violations in new features block PRs from merging. The QA team uses `getByRole()` locators for all tests, doubling as ARIA role validation.

**Common Mistakes:**
- Treating accessibility testing as a separate, manual activity — integrating basic axe-core scanning into the CI pipeline catches regressions automatically.
- Only testing visual accessibility (color contrast) — functional accessibility (keyboard navigation, focus management, screen reader labels) is equally critical.

**Optimization Tip:**
- Scope Axe scans to specific components rather than full pages to reduce scan time:
  ```java
  page.evaluate("axe.run(document.querySelector('.payment-form'))");
  ```
  Run full-page scans nightly; component scans on every PR.

**Debugging Strategy:**
- Axe violation objects include `nodes[].html` showing the exact DOM element failing. Use `page.accessibility().snapshot()` to see the full accessibility tree structure and identify missing labels or incorrect roles.

**Tricky Follow-up Questions:**
1. Axe-core reports a "color contrast" violation, but the designer says the brand colors are intentional — how do you handle accessibility rule exceptions in your test suite?
2. A modal dialog traps focus correctly when open, but after closing, focus returns to the wrong element — how do you write an automated test for focus restoration behavior?

---

## Q35: How do you implement logging in a Playwright Java framework?

**Answer:**
- Use **SLF4J + Logback** for structured test logging.
- Supplement with Playwright's `page.onConsoleMessage()` to capture browser console logs.
- Log request/response data via `page.onRequest()` / `page.onResponse()` for API audit trails.

**Explanation:**
- Framework-level logging: test lifecycle (start, pass, fail), action steps, and assertion outcomes.
- Browser-level logging: console errors, warnings, JS exceptions.
- Network-level logging: API calls, response codes, latencies.
- Combine all into a unified, correlatable log per test run (using correlation IDs).

**Java Example:**
```java
// Logback configuration (logback-test.xml)
public class LoggingSetup {
    private static final Logger log = LoggerFactory.getLogger(LoggingSetup.class);

    public void setupBrowserLogging(Page page) {
        // Capture browser console messages
        page.onConsoleMessage(msg -> {
            switch (msg.type()) {
                case "error"   -> log.error("[BROWSER CONSOLE] {}", msg.text());
                case "warning" -> log.warn("[BROWSER CONSOLE] {}", msg.text());
                default        -> log.debug("[BROWSER CONSOLE] {}", msg.text());
            }
        });

        // Capture uncaught JS exceptions
        page.onPageError(error -> log.error("[PAGE ERROR] {}", error));

        // Capture network activity
        page.onRequest(request ->
            log.debug("[REQUEST] {} {}", request.method(), request.url())
        );
        page.onResponse(response -> {
            if (response.status() >= 400) {
                log.warn("[RESPONSE] {} {} {}", response.status(),
                    response.request().method(), response.url());
            }
        });
    }

    public void logTestStep(String step) {
        log.info("[STEP] {}", step);
    }

    public void logTestResult(String testName, boolean passed) {
        if (passed) {
            log.info("[PASS] Test: {}", testName);
        } else {
            log.error("[FAIL] Test: {}", testName);
        }
    }
}
```

```xml
<!-- logback-test.xml -->
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>target/logs/test-run.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>target/logs/test-%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
        <encoder>
            <pattern>%d{ISO8601} [%-5level] [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Real-world Usage:**
- In an enterprise test framework, every test logs a `correlation-id` (UUID) that ties together: test log entries, browser console errors, and API calls in that test. When a failure occurs, engineers filter logs by `correlation-id` to reconstruct the complete flow.

**Common Mistakes:**
- Using `System.out.println()` — no log levels, no timestamps, unstructured. Always use SLF4J.
- Logging sensitive data (tokens, PII) — scrub credentials from logs before writing.
- Over-logging at INFO level — floods log files. Use DEBUG for request details, INFO for test steps, ERROR for failures.

**Optimization Tip:**
- Use MDC (Mapped Diagnostic Context) in SLF4J to attach test name and thread ID automatically to every log line:
```java
MDC.put("testName", testName);
MDC.put("threadId", String.valueOf(Thread.currentThread().getId()));
```

**Debugging Strategy:**
- Filter logs by test name and timestamp range to isolate a specific failure. Cross-reference `[PAGE ERROR]` entries with `[FAIL]` — JavaScript errors often cause cascading UI failures that are hard to spot without console logging.

**Tricky Follow-up Questions:**
1. Parallel tests write to the same log file — log lines are interleaved and unreadable. How do you implement per-test log files in a parallel execution environment?
2. A flaky test passes locally (where you can see browser console) but fails in CI. How do you capture and surface browser console errors in the CI test report?

---

## Q36: How do you integrate Allure Reporting with Playwright Java?

**Answer:**
- Add `allure-junit5` dependency and `AspectJ` agent to Maven/Gradle.
- Use `@Step`, `@Attachment`, `@Issue`, `@Severity` annotations in test code.
- Attach screenshots, traces, and logs to Allure report on test failure.

**Explanation:**
- Allure generates rich HTML reports with: test timeline, steps, attachments, history trends, and failure analysis.
- `@Step` annotations create a visual step-by-step breakdown of each test.
- Screenshots taken on failure are attached to the Allure report — reviewable without re-running tests.

**Java Example:**
```java
@Epic("Payment Module")
@Feature("Order Checkout")
public class OrderCheckoutTest extends BaseTest {

    @Test
    @Story("Successful order placement")
    @Severity(SeverityLevel.CRITICAL)
    @Issue("JIRA-1234")
    @Description("Validates end-to-end order placement with payment confirmation")
    public void testSuccessfulOrderPlacement() {
        navigateToCheckout();
        fillOrderDetails("John Doe", "john@example.com");
        completePayment("4111111111111111", "123", "12/26");
        verifyOrderConfirmation();
    }

    @Step("Navigate to checkout page")
    private void navigateToCheckout() {
        page.navigate("https://app.example.com/checkout");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Step("Fill order details: {name}, {email}")
    private void fillOrderDetails(String name, String email) {
        page.getByLabel("Full Name").fill(name);
        page.getByLabel("Email").fill(email);
    }

    @Step("Complete payment with card ending {cardNumber}")
    private void completePayment(String cardNumber, String cvv, String expiry) {
        FrameLocator frame = page.frameLocator("#payment-iframe");
        frame.getByLabel("Card Number").fill(cardNumber);
        frame.getByLabel("CVV").fill(cvv);
        frame.getByLabel("Expiry").fill(expiry);
        frame.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions()
            .setName("Pay Now")).click();
    }

    @Step("Verify order confirmation")
    private void verifyOrderConfirmation() {
        page.waitForURL("**/order-confirmed/**");
        assertThat(page.locator(".confirmation-header"))
            .hasText(Pattern.compile("Order #\\d+ Confirmed"));
        attachScreenshot("Order Confirmation");
    }

    @Attachment(value = "{name}", type = "image/png")
    private byte[] attachScreenshot(String name) {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }
}
```

```xml
<!-- pom.xml dependencies -->
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit5</artifactId>
    <version>2.25.0</version>
</dependency>
<!-- Maven Surefire with AspectJ agent -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-javaagent:${settings.localRepository}/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar</argLine>
    </configuration>
</plugin>
```

**Real-world Usage:**
- In a FinTech test suite, Allure reports are published to an internal dashboard after every CI run. Product managers review the Epic/Feature/Story hierarchy to track test coverage. Failures attach screenshots + Playwright trace files — QA engineers diagnose failures without re-running locally.

**Common Mistakes:**
- Forgetting the AspectJ weaver agent — `@Step` and `@Attachment` annotations silently don't work.
- Attaching screenshots only on failure but not on test steps — makes reports less informative for stakeholders.
- Not configuring `allure-results` directory consistently — Allure history fails when results move between runs.

**Optimization Tip:**
- Attach Playwright trace `.zip` files to Allure reports on failure:
```java
@Attachment(value = "Playwright Trace", type = "application/zip")
private byte[] attachTrace() throws IOException {
    Path tracePath = Paths.get("target/traces/" + testName + ".zip");
    context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
    return Files.readAllBytes(tracePath);
}
```

**Debugging Strategy:**
- `allure serve target/allure-results` — opens interactive report locally. If steps don't appear in Allure, verify AspectJ weaver is on the classpath and the `allure.properties` file points to the correct results directory.

**Tricky Follow-up Questions:**
1. In parallel execution, multiple tests write to `allure-results` simultaneously — how do you prevent report data corruption?
2. Allure history trend shows a spike in failures on Friday deploys — how do you configure Allure to link failures to CI build numbers and Git commit hashes?

---

## Q37: How do you use Playwright's Trace Viewer for debugging?

**Answer:**
- Enable tracing on `BrowserContext` with `context.tracing().start()`.
- Stop and save trace with `context.tracing().stop(path)`.
- Open with `playwright show-trace trace.zip` — provides full DOM snapshots, network, console logs, and action timeline.

**Explanation:**
- Trace Viewer provides a time-travel debugger: replay any moment of the test execution.
- Captures: every Playwright action, DOM state before/after each action, network requests/responses, console messages, and screenshots.
- Invaluable for debugging CI failures that can't be reproduced locally.

**Java Example:**
```java
public class TraceViewerDemo {

    // Enable tracing in test setup
    @BeforeEach
    public void startTracing() {
        context.tracing().start(new Tracing.StartOptions()
            .setScreenshots(true)  // capture screenshots at each action
            .setSnapshots(true)    // capture DOM snapshots
            .setSources(true)      // embed source code (Playwright version dependent)
        );
    }

    // Save trace on test failure
    @AfterEach
    public void stopTracing(TestInfo testInfo) {
        String testName = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_");
        Path tracePath = Paths.get("target/traces/" + testName + ".zip");

        if (testInfo.getExecutionException().isPresent()) {
            // Only save trace when test fails
            context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
            System.out.println("Trace saved: " + tracePath.toAbsolutePath());
        } else {
            context.tracing().stop(); // stop without saving on pass
        }
    }

    // Chunked tracing for long-running tests
    public void tracingWithChunks(BrowserContext context) {
        context.tracing().start(new Tracing.StartOptions().setSnapshots(true));
        // ... first phase of test ...
        context.tracing().startChunk(); // start new chunk without losing previous
        // ... second phase ...
        context.tracing().stopChunk(new Tracing.StopChunkOptions()
            .setPath(Paths.get("target/traces/phase2.zip")));
    }
}
```

**Real-world Usage:**
- In a CI pipeline, Playwright traces are archived as build artifacts on every failed test. Engineers click the trace artifact download in Jenkins/GitHub Actions, open it with `playwright show-trace`, and instantly see the exact DOM state, API call, and action that caused the failure — without re-running the test.

**Common Mistakes:**
- Saving traces for every test (including passes) — wastes disk space in CI. Save only on failure.
- Not enabling `setSnapshots(true)` — without DOM snapshots, the trace shows actions but not the page state, making it far less useful.

**Optimization Tip:**
- Configure Playwright CLI in CI to open traces (for local use): add `playwright show-trace` as a post-build step that opens automatically if failures are detected, saving time during local CI reproduction.

**Debugging Strategy:**
- In Trace Viewer: use the **Actions** panel to click any step — the DOM snapshot updates to show that exact moment. Check the **Network** tab for failed API calls. Use **Console** tab to see JS errors. The timeline view shows screenshot progression visually.

**Tricky Follow-up Questions:**
1. A trace file is 500MB because the test navigated 30 pages — how do you reduce trace file size while retaining debug value?
2. The trace shows the correct locator was resolved, but the element wasn't clicked — what does that indicate about the actionability check at that moment?

---

## Q38: How do you capture screenshots and video recordings in Playwright Java?

**Answer:**
- **Screenshots**: `page.screenshot()` — inline, on-demand, full-page, element-level, or clip region.
- **Video**: Configure `BrowserContext` with `setRecordVideoDir()` — records a video of every page in the context.
- Attach both to test reports (Allure/Extent) on failure for visual debugging.

**Explanation:**
- Screenshots are synchronous snapshots — instant capture.
- Video recording is async — runs continuously during the test, finalized when the page/context closes.
- Video + Trace provides the most complete debug evidence chain for CI failures.

**Java Example:**
```java
public class ScreenshotVideoDemo {

    // Screenshot on failure
    @AfterEach
    public void captureOnFailure(TestInfo testInfo) {
        if (testInfo.getExecutionException().isPresent()) {
            String name = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_");
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/FAIL_" + name + ".png"))
                .setFullPage(true)
            );
        }
    }

    // Element-level screenshot
    public void captureElement(Page page) {
        page.locator(".error-summary").screenshot(new Locator.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/error-summary.png"))
        );
    }

    // Clipped screenshot (specific region)
    public void captureClip(Page page) {
        page.screenshot(new Page.ScreenshotOptions()
            .setClip(new Clip(0, 0, 800, 400)) // x, y, width, height
            .setPath(Paths.get("target/screenshots/header-region.png"))
        );
    }

    // Enable video recording in context setup
    public BrowserContext createContextWithVideo(Browser browser) {
        return browser.newContext(
            new Browser.NewContextOptions()
                .setRecordVideoDir(Paths.get("target/videos/"))
                .setRecordVideoSize(new RecordVideoSize(1280, 720))
        );
    }

    // Retrieve video path after test
    public void saveVideo(Page page, String testName) {
        // Video is finalized when page is closed
        page.close();
        Path videoPath = page.video().path();
        Path dest = Paths.get("target/videos/" + testName + ".webm");
        try {
            Files.move(videoPath, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save video", e);
        }
    }
}
```

**Real-world Usage:**
- In a critical payment flow test suite, video recording is enabled only for test failures (context-level) and for nightly full regression runs. Product owners review failure videos to understand the exact user experience during a bug — faster than reading logs or asking QA to reproduce.

**Common Mistakes:**
- Calling `page.video().path()` before `page.close()` — video is not finalized until the page closes. The path exists but the file is incomplete.
- Recording video for every test in large suites — storage and performance overhead. Enable video selectively (on failure or on critical paths only).

**Optimization Tip:**
- Use `context.tracing()` for debugging and `video` for stakeholder demos/storytelling. Trace is more informative for engineers; video is more accessible for product/business stakeholders.

**Debugging Strategy:**
- If video is blank/frozen, check: `setRecordVideoDir()` directory exists and is writable, video codec support in the Docker image (`ffmpeg`), and that the page was actually navigated (blank page produces blank video).

**Tricky Follow-up Questions:**
1. Video files are `.webm` format — how do you convert them to `.mp4` for stakeholders who can't play webm in Windows Media Player?
2. In parallel tests, all videos land in the same directory with auto-generated names — how do you reliably rename and map them to specific test names?

---

## Q39: How do you integrate Playwright Java with CI/CD pipelines (Jenkins/GitHub Actions)?

**Answer:**
- Playwright Java runs headlessly in CI with standard Maven/Gradle commands.
- Install Playwright browsers in the pipeline with `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"`.
- Archive test reports, traces, screenshots, and videos as build artifacts.

**Explanation:**
- CI requires: JDK, Maven/Gradle, Playwright browser binaries, and display (for headed — Xvfb on Linux).
- Browser installation with `--with-deps` installs OS-level browser dependencies (libglib, libnss, etc.) required in minimal Docker/CI environments.
- Environment variables manage test credentials and environment configuration.

**Java Example (GitHub Actions workflow):**
```yaml
# .github/workflows/playwright-tests.yml
name: Playwright Java Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Install Playwright Browsers
        run: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"

      - name: Run Tests
        env:
          TEST_PASSWORD: ${{ secrets.TEST_PASSWORD }}
          CLIENT_ID: ${{ secrets.CLIENT_ID }}
          CLIENT_SECRET: ${{ secrets.CLIENT_SECRET }}
          HEADLESS: "true"
          BASE_URL: "https://staging.example.com"
        run: mvn test -Dsurefire.failIfNoSpecifiedTests=false

      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-reports
          path: |
            target/allure-results/
            target/screenshots/
            target/traces/
            target/videos/
          retention-days: 14

      - name: Generate Allure Report
        if: always()
        run: mvn allure:report

      - name: Upload Allure Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-report
          path: target/site/allure-maven-plugin/
```

**Real-world Usage:**
- In an enterprise FinTech pipeline, tests run in GitHub Actions on every PR (smoke suite: 50 tests, ~5 min) and nightly (full regression: 500 tests, parallel, ~20 min). Allure reports are published to GitHub Pages. Failed run traces are attached as artifacts — reviewable by the team without CI access.

**Common Mistakes:**
- Not running `playwright install --with-deps` in CI — browsers fail to launch due to missing OS libraries.
- Embedding credentials directly in `workflow.yml` — always use repository secrets (`${{ secrets.SECRET_NAME }}`).
- Not using `if: always()` on artifact upload steps — artifacts aren't uploaded when tests fail (because the step is skipped).

**Optimization Tip:**
- Cache Playwright browser binaries between CI runs using `actions/cache` keyed to the Playwright version — saves 1-2 minutes per run on browser download.

**Debugging Strategy:**
- Add `PLAYWRIGHT_JAVA_SRC=1` and `DEBUG=pw:api` environment variables in CI to enable verbose Playwright logging. Check CI runner resource limits — low CPU/memory causes timing issues in headless tests that don't occur locally.

**Tricky Follow-up Questions:**
1. Tests pass locally but consistently fail in GitHub Actions with "Timeout" errors — what are the top 5 causes to investigate first?
2. You need to run Playwright tests against a service that only exists within the CI network (internal staging) — how do you configure networking in GitHub Actions?

---

## Q40: How do you run Playwright Java tests in Docker containers?

**Answer:**
- Use the official `mcr.microsoft.com/playwright/java` Docker image — comes with Playwright + all browsers pre-installed.
- Mount test source or run from a built JAR. Pass environment variables for config.
- Use Docker Compose for multi-container setups (app + tests together).

**Explanation:**
- Docker eliminates "works on my machine" — tests run in a controlled, reproducible environment.
- The official image includes: JDK 17, Maven/Gradle, Playwright browsers, and all system dependencies.
- In headless mode, no display server is needed. For headed debugging, use Xvfb or VNC.

**Java Example:**
```dockerfile
# Dockerfile for tests
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy

WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build (skip tests during image build)
RUN mvn clean compile -q

# Run tests with environment variables
CMD ["mvn", "test", "-Dsurefire.failIfNoSpecifiedTests=false"]
```

```yaml
# docker-compose.yml — app + tests
version: '3.8'
services:
  app:
    image: my-app:latest
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 5s
      retries: 10

  playwright-tests:
    build: .
    depends_on:
      app:
        condition: service_healthy
    environment:
      BASE_URL: "http://app:8080"
      HEADLESS: "true"
      TEST_PASSWORD: "${TEST_PASSWORD}"
    volumes:
      - ./target/allure-results:/app/target/allure-results
      - ./target/screenshots:/app/target/screenshots
      - ./target/traces:/app/target/traces
```

```bash
# Run tests in Docker
docker-compose up --abort-on-container-exit --exit-code-from playwright-tests
```

**Real-world Usage:**
- In an enterprise microservices platform, integration tests spin up the full application stack (app server + DB + mock services) via Docker Compose. Playwright tests run in a separate container connected to the same Docker network — tests run against the real application running in containers, exactly as in production.

**Common Mistakes:**
- Not setting `--no-sandbox` Chrome flag in Docker — Chromium requires sandbox disabled in containerized environments (no root namespace).
- Using a regular Ubuntu base image instead of the official Playwright image — missing browser dependencies causes hard-to-diagnose launch failures.

**Optimization Tip:**
- Build a custom Docker image that layer-caches Maven dependencies separate from test code. Changes to test code only rebuild the test layer — not the dependency layer. Reduces Docker build time from 3 minutes to 30 seconds.

**Debugging Strategy:**
- Add `--cap-add=SYS_PTRACE` to Docker run flags for better error messages from Chromium. Mount `/tmp` and check for Playwright debug dumps. Run with `docker run -it` (interactive) to get a shell and manually run `playwright test` for interactive debugging.

**Tricky Follow-up Questions:**
1. In a Docker container without a display, `page.pause()` hangs the test — how do you set up remote debugging from your IDE into a containerized Playwright test?
2. The official Playwright Docker image is 2GB — how do you build a minimal custom image for faster CI pulls?

---

## Q41: How do you run Playwright Java tests on cloud execution platforms (BrowserStack, LambdaTest, Sauce Labs)?

**Answer:**
- Connect Playwright to cloud browsers via `playwright.chromium().connect()` using the platform's WebSocket endpoint.
- Pass capabilities (OS, browser version, device) as Playwright launch options or via query parameters in the WS URL.
- Use the CDP endpoint provided by the cloud platform for real browser execution.

**Java Example:**
```java
public class CloudExecutionDemo {

    // BrowserStack connection
    public Browser connectToBrowserStack(Playwright playwright) {
        String capabilities = Base64.getEncoder().encodeToString(
            "{\"browser\": \"chrome\", \"browser_version\": \"latest\", \"os\": \"Windows\", \"os_version\": \"11\", \"name\": \"Playwright Test\", \"build\": \"CI-Build-123\"}"
            .getBytes()
        );

        String wsEndpoint = String.format(
            "wss://cdp.browserstack.com/playwright?caps=%s&browserstack.username=%s&browserstack.access_key=%s",
            capabilities,
            System.getenv("BS_USERNAME"),
            System.getenv("BS_ACCESS_KEY")
        );

        return playwright.chromium().connectOverCDP(wsEndpoint);
    }

    // LambdaTest connection
    public Browser connectToLambdaTest(Playwright playwright) {
        String capabilities = URLEncoder.encode(
            "{\"browserName\": \"Chrome\", \"browserVersion\": \"latest\", \"LT:Options\": {\"platform\": \"Windows 11\", \"build\": \"Playwright-LT\", \"name\": \"Order Test\", \"user\": \"" + System.getenv("LT_USERNAME") + "\", \"accessKey\": \"" + System.getenv("LT_ACCESS_KEY") + "\"}}",
            StandardCharsets.UTF_8
        );

        String wsEndpoint = "wss://cdp.lambdatest.com/playwright?capabilities=" + capabilities;
        return playwright.chromium().connectOverCDP(wsEndpoint);
    }

    @Test
    public void testOnCloud() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = connectToBrowserStack(playwright);
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://app.example.com");
            assertThat(page.locator("h1")).isVisible();
            browser.close();
        }
    }
}
```

**Real-world Usage:**
- An FinTech application must be tested on Windows 11 Chrome, macOS Safari (WebKit), and Android Chrome (real device). Cloud platforms provide the infrastructure — Playwright orchestrates the test logic uniformly across all targets.

**Common Mistakes:**
- Not URL-encoding capability JSON in the WebSocket endpoint — causes connection failures.
- Forgetting to close the browser after cloud tests — leaves zombie sessions consuming cloud credits.

**Optimization Tip:**
- Use cloud execution only for cross-OS/cross-device coverage. Run core regression suite locally/Docker for speed. Use cloud for nightly cross-browser matrix (10 OS+browser combinations in parallel) via platform's parallel execution feature.

**Tricky Follow-up Questions:**
1. Cloud sessions have a timeout (typically 10 min) — how do you handle long-running tests that approach the timeout limit?
2. How do you debug a failure that only happens on a cloud-based real Android device but not in local Chromium emulation?

---

## Q42: How do you implement environment configuration management in Playwright Java?

**Answer:**
- Use a `ConfigManager` singleton that reads from: system properties → environment variables → properties/YAML files (in priority order).
- Never hardcode URLs, credentials, or environment-specific values in test code.
- Support multiple environments: dev, staging, prod, local.

**Java Example:**
```java
public class ConfigManager {

    private static ConfigManager instance;
    private final Properties properties;

    private ConfigManager() {
        properties = new Properties();
        // Load base config
        try (InputStream is = getClass().getResourceAsStream("/config/base.properties")) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load base config", e);
        }

        // Override with environment-specific config
        String env = System.getProperty("env", System.getenv().getOrDefault("TEST_ENV", "staging"));
        try (InputStream envIs = getClass().getResourceAsStream("/config/" + env + ".properties")) {
            if (envIs != null) properties.load(envIs);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load env config for: " + env, e);
        }
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    public String getBaseUrl() {
        return System.getProperty("baseUrl",
            System.getenv().getOrDefault("BASE_URL", properties.getProperty("base.url")));
    }

    public String getApiBaseUrl() {
        return properties.getProperty("api.base.url");
    }

    // Secrets: NEVER in config files — always from env vars
    public String getTestPassword() {
        String pwd = System.getenv("TEST_PASSWORD");
        if (pwd == null || pwd.isBlank()) throw new RuntimeException("TEST_PASSWORD env var not set");
        return pwd;
    }

    public int getDefaultTimeout() {
        return Integer.parseInt(properties.getProperty("default.timeout.ms", "30000"));
    }
}
```

**Real-world Usage:**
- A SaaS application runs tests in 3 environments (dev/staging/prod-readonly). The `ConfigManager` selects the correct base URL and API endpoints based on the `TEST_ENV` variable set in each CI pipeline. Credentials are always fetched from environment variables — never stored in property files.

**Common Mistakes:**
- Storing passwords, API keys, or tokens in `*.properties` files committed to Git — critical security vulnerability.
- Singleton not being thread-safe in parallel execution — use `synchronized` or `volatile` double-check locking.

**Optimization Tip:**
- Integrate with a secrets manager (AWS Secrets Manager, HashiCorp Vault) and fetch secrets at test startup — eliminates environment variables for credentials entirely, providing rotation and audit trail.

**Tricky Follow-up Questions:**
1. Your config file accidentally gets committed with a staging password — what immediate remediation steps do you take, and how do you prevent recurrence?
2. How do you manage configuration for 10 different tenant environments in a multi-tenant SaaS test suite?

---

## Q43: How do you handle test data management in Playwright Java enterprise tests?

**Answer:**
- **API-driven setup/teardown**: create test data via REST APIs before each test, delete after.
- **Database seeding**: use a `TestDataSeeder` that inserts directly via JDBC for complex data dependencies.
- **Fixture files**: JSON/YAML test data files loaded per test scenario.
- **Test data factories**: builder pattern to generate configurable test entities.

**Java Example:**
```java
// Test data factory
public class CustomerFactory {

    public static Customer.Builder aCustomer() {
        return new Customer.Builder()
            .withName("Test Customer " + UUID.randomUUID().toString().substring(0, 8))
            .withEmail("test+" + System.currentTimeMillis() + "@test.example.com")
            .withTier("STANDARD")
            .withCurrency("USD");
    }

    public static Customer.Builder aPremiumCustomer() {
        return aCustomer().withTier("PREMIUM").withCreditLimit(100_000);
    }
}

// API-driven test data lifecycle
public class TestDataManager {

    private final APIRequestContext apiContext;
    private final List<String> createdEntities = new ArrayList<>();

    public TestDataManager(APIRequestContext apiContext) {
        this.apiContext = apiContext;
    }

    public String createCustomer(Customer customer) {
        APIResponse response = apiContext.post("/v1/customers",
            RequestOptions.create().setData(customer));
        assertThat(response.status()).isEqualTo(201);
        String customerId = (String) ((Map<String, Object>) response.json()).get("id");
        createdEntities.add("/v1/customers/" + customerId);
        return customerId;
    }

    public void cleanupAll() {
        // Reverse order to handle dependencies
        Collections.reverse(createdEntities);
        createdEntities.forEach(path -> apiContext.delete("https://api.example.com" + path));
        createdEntities.clear();
    }
}

// Usage in test
@Test
public void testCustomerDashboard() {
    String customerId = dataManager.createCustomer(
        CustomerFactory.aPremiumCustomer()
            .withName("Acme Corp")
            .build()
    );

    page.navigate(config.getBaseUrl() + "/customers/" + customerId);
    assertThat(page.locator(".tier-badge")).hasText("PREMIUM");
}

@AfterEach
public void cleanup() {
    dataManager.cleanupAll(); // always runs, even on test failure
}
```

**Real-world Usage:**
- In a multi-tenant SaaS platform, each test creates isolated tenant + user + data via REST API at test start, runs the UI/API scenario, and deletes all created entities in `@AfterEach`. Tests never share data — completely independent, parallelizable, and repeatable.

**Common Mistakes:**
- Creating test data via UI (slow, fragile) when API/DB methods are available.
- Not cleaning up test data — staging DBs accumulate thousands of orphaned test records, causing performance degradation and capacity issues.
- Using shared static test accounts — parallel tests corrupt each other's state.

**Optimization Tip:**
- Use unique prefixes/tags for all test-created data (e.g., `TEST_AUTO_` prefix) as a safety net — a cleanup job can purge these even if teardown fails.

**Tricky Follow-up Questions:**
1. Test data cleanup fails mid-suite due to an API outage — orphaned records pile up in staging. How do you implement a resilient cleanup strategy?
2. You need to test a complex workflow that requires 3 days of historical transaction data — how do you seed this efficiently without manually creating 3 days of records?

---

## Q44: What are the common anti-patterns in Playwright Java frameworks and how do you avoid them?

**Answer:**
- **Thread.sleep()** — replace with Playwright's built-in waits.
- **Brittle XPath** — replace with role/text/testid locators.
- **Shared Page objects** — create fresh context/page per test.
- **Hardcoded credentials** — use env vars / secrets manager.
- **God Page Objects** — split into focused component objects.

**Explanation:**
- Anti-patterns kill test suite maintenance: they create flaky tests, security holes, and unmaintainable code.
- Recognizing them during code reviews is as important as avoiding them during initial development.

**Java Example:**
```java
// ❌ ANTI-PATTERNS — DO NOT USE

// 1. Thread.sleep — flaky, slow
page.locator(".submit-btn").click();
Thread.sleep(3000); // ❌ never

// 2. Hardcoded credentials
page.getByLabel("Password").fill("Admin@123"); // ❌ security risk

// 3. Brittle XPath
page.locator("xpath=//div[3]/table/tbody/tr[2]/td[4]/button").click(); // ❌ fragile

// 4. Shared static page across tests
public static Page page = browser.newPage(); // ❌ not thread-safe, shared state

// 5. Logic in Page Objects
public void loginAndVerifyDashboard() { // ❌ assertion in PO = wrong layer
    fillLoginForm();
    clickSignIn();
    assertThat(page.locator(".dashboard")).isVisible(); // belongs in test, not PO
}

// ✅ CORRECT PATTERNS

// 1. Auto-wait / explicit wait
page.locator(".submit-btn").click(); // auto-wait built-in
page.locator(".success-message").waitFor(); // explicit wait if needed

// 2. Secrets from env
page.getByLabel("Password").fill(System.getenv("TEST_PASSWORD")); // ✅

// 3. Role/testid locators
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click(); // ✅

// 4. Per-test context/page
@BeforeEach
void setUp() {
    context = browser.newContext(); // fresh per test
    page = context.newPage();
}

// 5. Page Objects return locators, tests do assertions
// LoginPage.java — only navigation and actions
public void login(String email, String password) {
    page.getByLabel("Email").fill(email);
    page.getByLabel("Password").fill(password);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
}
// Test class — assertions live here
```

**Real-world Usage:**
- During a framework audit of a legacy Selenium-migrated suite, the team found: 200+ `Thread.sleep()` calls, hardcoded passwords in 15 files, and 3000-line "God" Page Objects. Refactoring to Playwright patterns reduced flakiness from 15% to <1% and cut suite time by 40%.

**Common Mistakes:**
- Migrating from Selenium 1:1 without adopting Playwright's auto-wait — re-introduces all the same flakiness.
- Creating Page Objects that extend each other in deep hierarchies — violates composition over inheritance.

**Optimization Tip:**
- Add automated anti-pattern detection to CI: use PMD/Checkstyle rules to flag `Thread.sleep()`, hardcoded string patterns matching password formats, and detect static `Page` fields.

**Tricky Follow-up Questions:**
1. A senior developer argues that `Thread.sleep(500)` is "safe" for a specific animation — how do you address this in a code review without being dismissive?
2. A Page Object has grown to 800 lines. How do you refactor it without breaking 200 existing tests?

---

## Q45: How does Playwright Java handle environment variables and secrets securely?

**Answer:**
- Read secrets exclusively from environment variables (`System.getenv()`) — never from config files committed to Git.
- Validate required secrets at framework startup — fail fast with clear error messages.
- Integrate with secrets managers (AWS SSM, HashiCorp Vault, Azure Key Vault) for enterprise scenarios.
- Mask secrets in all logs and reports.

**Java Example:**
```java
public class SecretsManager {

    // Validate required secrets at startup
    public static void validateRequiredSecrets() {
        List<String> required = List.of(
            "TEST_PASSWORD", "CLIENT_ID", "CLIENT_SECRET", "API_TOKEN"
        );
        List<String> missing = required.stream()
            .filter(key -> System.getenv(key) == null || System.getenv(key).isBlank())
            .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            throw new RuntimeException(
                "Missing required environment variables: " + String.join(", ", missing)
            );
        }
    }

    public static String getSecret(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Required secret not set: " + key);
        }
        return value;
    }

    // Mask secret in string (for logging)
    public static String maskSecret(String value) {
        if (value == null || value.length() < 8) return "****";
        return value.substring(0, 4) + "*".repeat(value.length() - 4);
    }
}
```

**Real-world Usage:**
- In a regulated FinTech environment, test credentials are stored in AWS Secrets Manager. A pre-test setup step fetches secrets and sets them as environment variables within the CI job — secrets never touch disk or appear in logs.

**Common Mistakes:**
- `System.getenv()` returning `null` silently used as a locator/URL value — no error until mid-test. Always validate at startup.
- Printing tokens in debug logs: `log.debug("Using token: " + token)` — use masked version.

**Tricky Follow-up Questions:**
1. A developer accidentally commits a `.env` file with real credentials — what's your incident response and how do you prevent recurrence with tooling?
2. In a multi-cloud CI setup (GitHub Actions + Jenkins), how do you maintain a single source of truth for test credentials across both platforms?

---

## Q46: How do you implement the Page Object Model (POM) correctly in Playwright Java?

**Answer:**
- Page Objects encapsulate **locators and actions** for a specific page/component — not assertions.
- Use constructor injection to receive the `Page` instance.
- Return `this` or the next Page Object from action methods (fluent API / chaining).
- Separate components into their own Component Objects.

**Java Example:**
```java
// Base Page
public abstract class BasePage {
    protected final Page page;
    protected final ConfigManager config;

    protected BasePage(Page page) {
        this.page = page;
        this.config = ConfigManager.getInstance();
    }

    protected void waitForPageLoad() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }
}

// Login Page Object
public class LoginPage extends BasePage {

    private final Locator emailField = page.getByLabel("Email");
    private final Locator passwordField = page.getByLabel("Password");
    private final Locator signInButton = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Sign In"));
    private final Locator errorMessage = page.locator(".auth-error");

    public LoginPage(Page page) {
        super(page);
    }

    public LoginPage navigate() {
        page.navigate(config.getBaseUrl() + "/login");
        waitForPageLoad();
        return this;
    }

    public DashboardPage loginAs(String email, String password) {
        emailField.fill(email);
        passwordField.fill(password);
        signInButton.click();
        page.waitForURL("**/dashboard");
        return new DashboardPage(page); // return next page
    }

    public LoginPage loginWithInvalidCredentials(String email, String password) {
        emailField.fill(email);
        passwordField.fill(password);
        signInButton.click();
        return this; // stay on login page
    }

    public Locator getErrorMessage() {
        return errorMessage;
    }
}

// Usage in test — clean, readable
@Test
public void testSuccessfulLogin() {
    DashboardPage dashboard = new LoginPage(page)
        .navigate()
        .loginAs("admin@example.com", SecretsManager.getSecret("TEST_PASSWORD"));

    assertThat(dashboard.getWelcomeHeader()).containsText("Welcome");
}

@Test
public void testInvalidCredentials() {
    LoginPage loginPage = new LoginPage(page)
        .navigate()
        .loginWithInvalidCredentials("bad@example.com", "wrongpass");

    assertThat(loginPage.getErrorMessage()).hasText("Invalid email or password");
}
```

**Real-world Usage:**
- In a SaaS platform with 60+ pages, each page has a corresponding Page Object. Component Objects (NavBar, DataTable, Modal) are reused across multiple Page Objects. Tests read like business scenarios — no locator details visible in test classes.

**Common Mistakes:**
- Defining locators as `String` constants in PO and calling `page.locator(SELECTOR)` repeatedly — locators should be `Locator` fields initialized in constructor.
- Page Objects performing assertions — violates single responsibility.
- Using `static` Page Objects shared across tests.

**Tricky Follow-up Questions:**
1. A Page Object's locator becomes stale after a SPA (React) re-renders the component — how does Playwright's lazy locator evaluation handle this vs Selenium's `StaleElementReferenceException`?
2. Your POM has 60 Page Objects and there's a site-wide navbar change — how do you update all Page Objects efficiently using the Component Object Model?

---

## Q47: What design patterns are used in enterprise Playwright Java frameworks?

**Answer:**
- **Factory** — `BrowserFactory`, `PageFactory` for object creation.
- **Builder** — test data construction (`CustomerBuilder`).
- **Strategy** — swappable auth, reporting, or browser strategies.
- **Decorator** — wrapping `Page` with logging, retry, screenshot.
- **Singleton** — `ConfigManager`, `TokenCache`.
- **Template Method** — `BaseTest` defines test lifecycle hooks.

**Java Example:**
```java
// Factory Pattern — Browser creation
public class BrowserFactory {
    public static Browser create(Playwright playwright, String type) {
        return switch (type.toLowerCase()) {
            case "chromium" -> playwright.chromium().launch(defaultOpts());
            case "firefox"  -> playwright.firefox().launch(defaultOpts());
            case "webkit"   -> playwright.webkit().launch(defaultOpts());
            default -> throw new IllegalArgumentException("Unknown browser: " + type);
        };
    }
    private static BrowserType.LaunchOptions defaultOpts() {
        return new BrowserType.LaunchOptions().setHeadless(true);
    }
}

// Builder Pattern — Test data
public class OrderBuilder {
    private String customerId;
    private String sku = "DEFAULT-SKU";
    private int quantity = 1;
    private String currency = "USD";

    public OrderBuilder forCustomer(String customerId) {
        this.customerId = customerId; return this;
    }
    public OrderBuilder withSku(String sku) {
        this.sku = sku; return this;
    }
    public OrderBuilder withQuantity(int quantity) {
        this.quantity = quantity; return this;
    }
    public Map<String, Object> build() {
        return Map.of("customerId", customerId, "sku", sku,
                      "quantity", quantity, "currency", currency);
    }
}

// Strategy Pattern — Reporting
public interface ReportingStrategy {
    void onTestStart(String testName);
    void onTestPass(String testName);
    void onTestFail(String testName, Throwable cause, byte[] screenshot);
}

public class AllureReportingStrategy implements ReportingStrategy { /* ... */ }
public class ExtentReportingStrategy implements ReportingStrategy { /* ... */ }

// Decorator Pattern — Page with logging
public class LoggingPage {
    private final Page page;
    private static final Logger log = LoggerFactory.getLogger(LoggingPage.class);

    public LoggingPage(Page page) { this.page = page; }

    public void click(Locator locator) {
        log.info("[ACTION] Clicking: {}", locator);
        locator.click();
    }
    public void fill(Locator locator, String value) {
        log.info("[ACTION] Filling: {} with value: {}", locator, maskIfSensitive(value));
        locator.fill(value);
    }
}
```

**Real-world Usage:**
- In a financial enterprise framework, the `Strategy` pattern swaps reporting between Allure (for QA) and a lightweight CSV reporter (for performance benchmarking runs). The `Decorator` pattern wraps every Page action with timing metrics sent to Grafana dashboards.

**Common Mistakes:**
- Over-engineering — adding 5 design patterns to a 10-test suite. Apply patterns when they solve real problems, not for CV-driven development.
- Implementing Factory pattern but still calling `new LoginPage()` directly in tests — defeats the purpose.

**Tricky Follow-up Questions:**
1. You need to support both TestNG and JUnit 5 in the same framework — which design pattern helps you implement this without duplicating test lifecycle code?
2. The Observer pattern — how would you use it in a Playwright framework to implement event-driven reporting without coupling the test code to specific reporters?

---

## Q48: How do you measure and optimize test performance in a Playwright Java suite?

**Answer:**
- Measure: total suite time, per-test time (JUnit 5 `@Timeout`), browser startup time, and API response times.
- Optimize: parallel execution, auth state reuse, API-driven data setup, selective tracing, and resource blocking.
- Profile: Maven Surefire timing reports, Allure timeline view, browser network waterfall.

**Java Example:**
```java
public class PerformanceOptimizations {

    // 1. Block unnecessary resources (images, fonts, stylesheets) for speed
    public void blockNonEssentialResources(Page page) {
        page.route("**/*.{png,jpg,gif,svg,woff,woff2,css}", route -> {
            String resourceType = route.request().resourceType();
            if (List.of("image", "font", "stylesheet").contains(resourceType)) {
                route.abort(); // block for faster page loads
            } else {
                route.continue_();
            }
        });
    }

    // 2. Wait for DOMCONTENTLOADED instead of LOAD for faster navigation
    public void fastNavigation(Page page, String url) {
        page.navigate(url, new Page.NavigateOptions()
            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)); // faster than LOAD
    }

    // 3. Measure action timing
    public <T> T measureAction(String actionName, Supplier<T> action) {
        long start = System.currentTimeMillis();
        T result = action.get();
        long duration = System.currentTimeMillis() - start;
        log.info("[PERF] {} took {}ms", actionName, duration);
        return result;
    }

    // 4. Per-test timeout enforcement
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testWithTimeout() {
        // Test must complete within 60 seconds or fails
    }

    // 5. Network idle vs load state selection
    public void waitForAppReady(Page page) {
        // Use NETWORKIDLE only when truly needed (SPAs with lazy loading)
        // For most pages, DOMCONTENTLOADED or LOAD is sufficient and faster
        page.waitForLoadState(LoadState.LOAD);
    }
}
```

**Real-world Usage:**
- A 500-test suite initially took 75 minutes. Optimizations applied: parallel execution (4 threads) → 20 min; auth state reuse → -10 min; resource blocking for non-critical tests → -5 min; API data setup instead of UI → -8 min. Final: ~18 minutes (76% reduction).

**Common Mistakes:**
- Using `NETWORKIDLE` universally — it waits for ALL network requests to stop (500ms of silence). For pages with polling/WebSockets, it never fires. Use `LOAD` or `DOMCONTENTLOADED` as default.
- Running all tests serially when parallelization is safe — leaving significant time on the table.

**Tricky Follow-up Questions:**
1. Resource blocking speeds up tests but causes some assertions to fail (CSS-dependent visibility checks) — how do you selectively block resources only for tests that don't need them?
2. Test suite performance degrades over time as more tests are added — how do you implement and enforce a "test budget" to maintain suite time SLAs?

---

## Q49: How do you handle test environment configuration for multi-environment deployments?

**Answer:**
- Use a layered config hierarchy: `base.properties` → `{env}.properties` → system properties → env vars.
- Support environments: `local`, `dev`, `staging`, `prod` (read-only smoke tests).
- Use `@Tag` to label environment-specific tests and run only appropriate tests per environment.

**Java Example:**
```java
// src/test/resources/config/base.properties
// default.timeout.ms=30000
// retry.count=2

// src/test/resources/config/staging.properties
// base.url=https://staging.example.com
// api.base.url=https://api-staging.example.com
// feature.new_ui.enabled=true

// src/test/resources/config/prod.properties
// base.url=https://app.example.com
// api.base.url=https://api.example.com
// feature.new_ui.enabled=false

// In tests — environment-conditional behavior
@Test
@EnabledIfEnvironmentVariable(named = "TEST_ENV", matches = "staging|dev")
public void testNewUiFeature() {
    // Only runs on staging/dev where new UI is enabled
    if (config.isFeatureEnabled("new_ui")) {
        page.navigate(config.getBaseUrl() + "/new-dashboard");
        assertThat(page.locator(".new-ui-header")).isVisible();
    }
}

// Tag-based environment filtering
@Test
@Tag("smoke")
@Tag("all-envs")
public void testCriticalHealthCheck() { /* ... */ }

@Test
@Tag("regression")
@Tag("staging-only")
public void testDataMigrationValidation() { /* ... */ }
```

**Real-world Usage:**
- In an enterprise deployment pipeline: dev → staging → production. Smoke tests (`@Tag("smoke")`) run against every environment post-deploy. Full regression (`@Tag("regression")`) runs only on staging. Production has a readonly smoke suite that validates live endpoints without creating test data.

**Common Mistakes:**
- Running destructive tests (create/delete data) against production — always guard with environment checks.
- Hard-coding environment URLs in test annotations or class-level fields.

**Tricky Follow-up Questions:**
1. A smoke test passes in staging but fails in production due to a feature flag difference — how do you make your tests environment-aware for feature flags?
2. How do you run the same test against 5 environments in parallel in a single pipeline run?

---

## Q50: What are the key differences between Playwright Java and Playwright TypeScript/JavaScript, and when do you choose Java?

**Answer:**
- **Java** — preferred in enterprises with existing Java/Selenium infrastructure, strong typing, mature build tools (Maven/Gradle), JVM ecosystem (Allure, TestNG, JUnit 5).
- **TypeScript** — preferred for Node.js teams, front-end developers, and when using Playwright Test runner (built-in parallelism, fixtures, reporters).
- Both share the same browser engine and most API surface — core concepts are identical.

**Explanation:**
- Playwright TypeScript has the first-class `@playwright/test` runner with fixtures, built-in parallelism, and native reporters. Java relies on JUnit 5/TestNG for the same.
- Java's strong typing prevents many runtime errors. TypeScript has type inference and is lighter-weight.
- In Java enterprises, the QA team may already have JUnit + Maven expertise — Java Playwright is a natural fit.

**Java Example:**
```java
// Java — explicit types, constructor injection, class-based POM
public class LoginPage {
    private final Page page;
    private final Locator emailField;

    public LoginPage(Page page) {
        this.page = page;
        this.emailField = page.getByLabel("Email");
    }

    public DashboardPage login(String email, String password) {
        emailField.fill(email);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        return new DashboardPage(page);
    }
}
```

```typescript
// TypeScript equivalent — more concise
class LoginPage {
    constructor(private page: Page) {}

    async login(email: string, password: string): Promise<DashboardPage> {
        await this.page.getByLabel('Email').fill(email);
        await this.page.getByLabel('Password').fill(password);
        await this.page.getByRole('button', { name: 'Sign In' }).click();
        return new DashboardPage(this.page);
    }
}
```

**Key Differences:**
| | Playwright Java | Playwright TypeScript |
|---|---|---|
| Async model | Synchronous API | `async/await` |
| Test runner | JUnit 5 / TestNG | `@playwright/test` |
| Fixtures | `@BeforeEach` | Built-in fixture system |
| Parallelism | ThreadLocal + Surefire | Built-in, config-driven |
| Reporting | Allure, Extent | Built-in HTML, Allure |
| Type safety | Strong (compile-time) | Good (with TypeScript) |
| Team fit | Java/backend teams | JS/frontend teams |

**Real-world Usage:**
- A bank's QA team migrated from Selenium Java to Playwright Java — reusing Maven infrastructure, JUnit 5 expertise, and Allure reporting. A startup's frontend team chose Playwright TypeScript for its `@playwright/test` runner and native component testing support.

**Common Mistakes:**
- Choosing language based on personal preference rather than team context and ecosystem fit.
- Assuming Playwright TypeScript features (like fixtures) don't exist in Java — they do, just implemented differently with JUnit 5 extensions.

**Tricky Follow-up Questions:**
1. Playwright TypeScript's `test.use()` fixture system provides inheritable context configuration — how do you replicate this pattern in Java using JUnit 5 extensions and custom annotations?
2. Your team has 3 Java engineers and 2 JavaScript engineers — how do you decide on the Playwright stack and manage the inevitable knowledge split?

---

*— End of Q31–Q50 | Section 1: Theory Questions Complete (50/50) —*

---

# SECTION 2 — CODING / HANDS-ON QUESTIONS (30)

> Next batch: **Coding Q1–Q15** — Browser Factory, Page Abstraction, Login Utility, Token Manager, API Wrapper, Dynamic Table Parser, Retry Wrapper, and more.

---

## Coding Q1: Build a Thread-Safe Browser Factory

**Problem:** Enterprise test suite needs to support Chromium, Firefox, and WebKit with headless/headed toggling, slow motion, and per-thread isolation.

**Difficulty:** Medium

**Task:** Implement a `BrowserFactory` that creates browser instances based on config, is thread-safe for parallel execution, and cleans up properly.

**Constraints:**
- Must use `ThreadLocal` for parallel safety
- Browser type configurable via system property
- Headless mode toggled via env variable
- Must release all resources on shutdown

**Solution:**
```java
public class BrowserFactory {

    private static final ThreadLocal<Playwright> playwrightTL = new ThreadLocal<>();
    private static final ThreadLocal<Browser> browserTL = new ThreadLocal<>();

    public enum BrowserEngine { CHROMIUM, FIREFOX, WEBKIT }

    public static Browser getBrowser() {
        if (browserTL.get() == null) {
            Playwright playwright = Playwright.create();
            playwrightTL.set(playwright);

            BrowserEngine engine = resolveEngine();
            boolean headless = isHeadless();
            int slowMo = Integer.parseInt(System.getProperty("slowMo", "0"));

            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage"));

            Browser browser = switch (engine) {
                case CHROMIUM -> playwright.chromium().launch(opts);
                case FIREFOX  -> playwright.firefox().launch(opts);
                case WEBKIT   -> playwright.webkit().launch(opts);
            };
            browserTL.set(browser);
        }
        return browserTL.get();
    }

    private static BrowserEngine resolveEngine() {
        String val = System.getProperty("browser",
            System.getenv().getOrDefault("BROWSER", "chromium")).toUpperCase();
        try {
            return BrowserEngine.valueOf(val);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported browser: " + val);
        }
    }

    private static boolean isHeadless() {
        return Boolean.parseBoolean(
            System.getProperty("headless",
                System.getenv().getOrDefault("HEADLESS", "true")));
    }

    public static void closeAll() {
        try {
            if (browserTL.get() != null) {
                browserTL.get().close();
                browserTL.remove();
            }
        } finally {
            if (playwrightTL.get() != null) {
                playwrightTL.get().close();
                playwrightTL.remove();
            }
        }
    }
}
```

**Alternative Approach:**
- Use a `BrowserPool` (fixed-size queue) instead of `ThreadLocal` when you want to limit total browser processes in resource-constrained environments.

**Usage:**
```bash
mvn test -Dbrowser=firefox -Dheadless=false -DslowMo=100
```

---

## Coding Q2: Build a Generic Base Page with Context Management

**Problem:** Every Page Object in a 60-page enterprise suite needs consistent: timeout config, load state waiting, screenshot on failure, and logging.

**Difficulty:** Medium

**Task:** Create a `BasePage` that all Page Objects extend — handling common concerns without duplicating code.

**Constraints:**
- Configurable default timeout
- `waitForLoad()` with configurable state
- `takeScreenshot()` with meaningful names
- Common navigation helpers

**Solution:**
```java
public abstract class BasePage {

    protected final Page page;
    protected final ConfigManager config;
    protected static final Logger log = LoggerFactory.getLogger(BasePage.class);

    protected BasePage(Page page) {
        this.page = page;
        this.config = ConfigManager.getInstance();
        page.setDefaultTimeout(config.getDefaultTimeout());
        page.setDefaultNavigationTimeout(config.getNavigationTimeout());
        setupConsoleLogging();
    }

    // Subclasses define their URL path
    protected abstract String getPath();

    public <T extends BasePage> T navigate() {
        String url = config.getBaseUrl() + getPath();
        log.info("[NAV] Navigating to: {}", url);
        page.navigate(url);
        waitForLoad();
        @SuppressWarnings("unchecked")
        T self = (T) this;
        return self;
    }

    protected void waitForLoad() {
        waitForLoad(LoadState.DOMCONTENTLOADED);
    }

    protected void waitForLoad(LoadState state) {
        page.waitForLoadState(state);
    }

    public byte[] takeScreenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    public byte[] takeScreenshot(String elementSelector) {
        return page.locator(elementSelector).screenshot();
    }

    public String getPageTitle() {
        return page.title();
    }

    public String getCurrentUrl() {
        return page.url();
    }

    protected void waitForUrl(String urlPattern) {
        page.waitForURL(urlPattern);
    }

    protected void scrollToElement(Locator locator) {
        locator.scrollIntoViewIfNeeded();
    }

    protected boolean isElementVisible(Locator locator) {
        return locator.isVisible();
    }

    private void setupConsoleLogging() {
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                log.error("[BROWSER] {}", msg.text());
            }
        });
        page.onPageError(err -> log.error("[PAGE ERROR] {}", err));
    }
}

// Concrete Page Object
public class DashboardPage extends BasePage {

    private final Locator welcomeHeader = page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setLevel(1));
    private final Locator portfolioWidget = page.locator("[data-testid='portfolio-widget']");
    private final Locator notificationBell = page.locator("[data-testid='notification-bell']");

    public DashboardPage(Page page) {
        super(page);
    }

    @Override
    protected String getPath() { return "/dashboard"; }

    public Locator getWelcomeHeader() { return welcomeHeader; }
    public Locator getPortfolioWidget() { return portfolioWidget; }

    public NotificationPanel openNotifications() {
        notificationBell.click();
        return new NotificationPanel(page);
    }
}
```

---

## Coding Q3: Build a Reusable Login Utility with Multi-Role Support

**Problem:** 500-test suite needs login for 5 different user roles (admin, manager, viewer, auditor, superuser). Login UI flow is slow — needs to be optimized.

**Difficulty:** Medium

**Task:** Build a `LoginManager` that: logs in per role once, caches auth state per role, injects state into BrowserContexts for all subsequent tests.

**Constraints:**
- State files stored in `target/auth/`
- State refreshed if older than 30 minutes
- Thread-safe for parallel execution
- Never log credentials

**Solution:**
```java
public class LoginManager {

    private static final Path AUTH_DIR = Paths.get("target/auth");
    private static final long STATE_TTL_MINUTES = 30;
    private static final Map<UserRole, Object> locks = new ConcurrentHashMap<>();

    public enum UserRole {
        ADMIN, MANAGER, VIEWER, AUDITOR, SUPERUSER;

        public Path statePath() {
            return AUTH_DIR.resolve(name().toLowerCase() + "-state.json");
        }
    }

    static {
        try { Files.createDirectories(AUTH_DIR); }
        catch (IOException e) { throw new RuntimeException(e); }
        for (UserRole role : UserRole.values()) {
            locks.put(role, new Object());
        }
    }

    public static BrowserContext createContextForRole(Browser browser, UserRole role) {
        ensureAuthState(browser, role);
        return browser.newContext(new Browser.NewContextOptions()
            .setStorageStatePath(role.statePath()));
    }

    private static void ensureAuthState(Browser browser, UserRole role) {
        synchronized (locks.get(role)) {
            Path statePath = role.statePath();
            if (isStateValid(statePath)) return;

            log.info("[AUTH] Generating auth state for role: {}", role);
            BrowserContext ctx = browser.newContext();
            Page page = ctx.newPage();

            try {
                performLogin(page, role);
                ctx.storageState(new BrowserContext.StorageStateOptions().setPath(statePath));
            } finally {
                ctx.close();
            }
        }
    }

    private static boolean isStateValid(Path statePath) {
        try {
            if (!Files.exists(statePath)) return false;
            FileTime lastModified = Files.getLastModifiedTime(statePath);
            long ageMinutes = Duration.between(
                lastModified.toInstant(), Instant.now()).toMinutes();
            return ageMinutes < STATE_TTL_MINUTES;
        } catch (IOException e) {
            return false;
        }
    }

    private static void performLogin(Page page, UserRole role) {
        page.navigate(ConfigManager.getInstance().getBaseUrl() + "/login");
        page.getByLabel("Email").fill(getEmail(role));
        page.getByLabel("Password").fill(getPassword(role));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        page.waitForURL("**/dashboard");
    }

    private static String getEmail(UserRole role) {
        return System.getenv(role.name() + "_EMAIL");
    }

    private static String getPassword(UserRole role) {
        return System.getenv(role.name() + "_PASSWORD");
    }

    private static final Logger log = LoggerFactory.getLogger(LoginManager.class);
}

// Usage in test
@Test
public void testAdminCanDeleteUser() {
    BrowserContext ctx = LoginManager.createContextForRole(browser, UserRole.ADMIN);
    Page page = ctx.newPage();
    new UserManagementPage(page).navigate().deleteUser("user@example.com");
    // ...
    ctx.close();
}
```

---

## Coding Q4: Build a JWT Token Manager with Caching and Auto-Refresh

**Problem:** API tests need JWT tokens for 3 different OAuth clients. Tokens expire in 15 minutes. Acquiring a token takes 400ms — must not be called on every test.

**Difficulty:** Hard

**Task:** Implement a thread-safe `TokenManager` that caches tokens per client, auto-refreshes before expiry, and handles concurrent access.

**Constraints:**
- Token refresh window: 60 seconds before expiry
- Thread-safe for parallel execution
- Credentials from environment variables only
- Support PKCE and client credentials grant types

**Solution:**
```java
public class TokenManager {

    private record TokenEntry(String accessToken, Instant expiresAt) {
        boolean isExpiringSoon() {
            return Instant.now().isAfter(expiresAt.minusSeconds(60));
        }
    }

    public enum OAuthClient {
        TRADING_SERVICE("TRADING_CLIENT_ID", "TRADING_CLIENT_SECRET", "read:portfolio write:orders"),
        REPORTING_SERVICE("REPORT_CLIENT_ID", "REPORT_CLIENT_SECRET", "read:reports"),
        ADMIN_SERVICE("ADMIN_CLIENT_ID", "ADMIN_CLIENT_SECRET", "admin:all");

        final String clientIdEnv, clientSecretEnv, scope;

        OAuthClient(String clientIdEnv, String clientSecretEnv, String scope) {
            this.clientIdEnv = clientIdEnv;
            this.clientSecretEnv = clientSecretEnv;
            this.scope = scope;
        }
    }

    private static final ConcurrentHashMap<OAuthClient, TokenEntry> cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<OAuthClient, Object> clientLocks = new ConcurrentHashMap<>();

    static {
        for (OAuthClient c : OAuthClient.values()) clientLocks.put(c, new Object());
    }

    public static String getToken(OAuthClient client) {
        TokenEntry entry = cache.get(client);
        if (entry != null && !entry.isExpiringSoon()) {
            return entry.accessToken();
        }
        return refreshToken(client);
    }

    private static String refreshToken(OAuthClient client) {
        synchronized (clientLocks.get(client)) {
            // Double-check after acquiring lock
            TokenEntry entry = cache.get(client);
            if (entry != null && !entry.isExpiringSoon()) return entry.accessToken();

            try (Playwright playwright = Playwright.create()) {
                APIRequestContext request = playwright.request().newContext(
                    new APIRequest.NewContextOptions()
                        .setBaseURL(ConfigManager.getInstance().getAuthBaseUrl())
                );

                APIResponse response = request.post("/oauth/token",
                    RequestOptions.create().setData(Map.of(
                        "grant_type", "client_credentials",
                        "client_id", System.getenv(client.clientIdEnv),
                        "client_secret", System.getenv(client.clientSecretEnv),
                        "scope", client.scope
                    ))
                );

                if (!response.ok()) {
                    throw new RuntimeException("Token acquisition failed: " + response.status()
                        + " for client: " + client);
                }

                Map<String, Object> body = (Map<String, Object>) response.json();
                String token = (String) body.get("access_token");
                int expiresIn = (int) body.get("expires_in");
                Instant expiresAt = Instant.now().plusSeconds(expiresIn);

                cache.put(client, new TokenEntry(token, expiresAt));
                request.dispose();
                return token;
            }
        }
    }

    public static void invalidate(OAuthClient client) {
        cache.remove(client);
    }

    public static void invalidateAll() {
        cache.clear();
    }
}

// Usage
APIRequestContext apiCtx = page.request();
String token = TokenManager.getToken(OAuthClient.TRADING_SERVICE);
APIResponse resp = apiCtx.get("https://api.example.com/v1/portfolio",
    RequestOptions.create().setHeader("Authorization", "Bearer " + token));
```

---

## Coding Q5: Build a Typed REST API Client Wrapper

**Problem:** Tests need to call 20+ REST endpoints. Raw `APIRequestContext` calls are verbose and not type-safe. Error handling is inconsistent across tests.

**Difficulty:** Medium

**Task:** Build an `ApiClient` wrapper around `APIRequestContext` with: typed responses, centralized error handling, auth header injection, and retry logic.

**Constraints:**
- All responses deserialized via Jackson `ObjectMapper`
- Auto-inject Bearer token from `TokenManager`
- Retry on 429 (rate limit) with `Retry-After` header
- Throw typed `ApiException` on non-2xx

**Solution:**
```java
public class ApiClient {

    private final APIRequestContext context;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    public ApiClient(Playwright playwright) {
        this.context = playwright.request().newContext(
            new APIRequest.NewContextOptions()
                .setBaseURL(ConfigManager.getInstance().getApiBaseUrl())
                .setExtraHTTPHeaders(Map.of("Content-Type", "application/json"))
        );
    }

    public <T> T get(String path, Class<T> responseType) {
        return execute("GET", path, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return execute("POST", path, body, responseType);
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        return execute("PUT", path, body, responseType);
    }

    public void delete(String path) {
        execute("DELETE", path, null, Void.class);
    }

    private <T> T execute(String method, String path, Object body, Class<T> type) {
        return retryOnRateLimit(() -> {
            String token = TokenManager.getToken(OAuthClient.TRADING_SERVICE);
            RequestOptions opts = RequestOptions.create()
                .setHeader("Authorization", "Bearer " + token);
            if (body != null) opts.setData(serialize(body));

            log.debug("[API] {} {}", method, path);
            APIResponse response = switch (method) {
                case "GET"    -> context.get(path, opts);
                case "POST"   -> context.post(path, opts);
                case "PUT"    -> context.put(path, opts);
                case "DELETE" -> context.delete(path, opts);
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            };

            if (!response.ok()) {
                throw new ApiException(method, path, response.status(), response.text());
            }

            return type == Void.class ? null : deserialize(response.text(), type);
        });
    }

    private <T> T retryOnRateLimit(Supplier<T> action) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return action.get();
            } catch (ApiException e) {
                if (e.getStatus() == 429 && attempt < 2) {
                    int retryAfter = e.getRetryAfterSeconds().orElse(2);
                    log.warn("[API] Rate limited. Waiting {}s before retry.", retryAfter);
                    sleep(retryAfter * 1000L);
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }

    private Map<String, Object> serialize(Object body) {
        return mapper.convertValue(body, new TypeReference<>() {});
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize response: " + json, e);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void dispose() { context.dispose(); }
}

// Typed exception
public class ApiException extends RuntimeException {
    private final int status;
    private final String path;

    public ApiException(String method, String path, int status, String body) {
        super(String.format("[%d] %s %s: %s", status, method, path, body));
        this.status = status;
        this.path = path;
    }

    public int getStatus() { return status; }

    public Optional<Integer> getRetryAfterSeconds() {
        // Parse Retry-After if available
        return Optional.empty();
    }
}

// Usage in test
ApiClient api = new ApiClient(playwright);
OrderResponse order = api.post("/v1/orders",
    new OrderRequest("CUST-001", "SKU-101", 5), OrderResponse.class);
assertThat(order.getStatus()).isEqualTo("CONFIRMED");
api.delete("/v1/orders/" + order.getId());
```

---

## Coding Q6: Build a Dynamic Table Parser Utility

**Problem:** Enterprise apps have data tables with sorting, filtering, and pagination. Tests need to: find rows by column value, extract entire tables, validate sort order, and handle empty states.

**Difficulty:** Hard

**Task:** Build a `TableUtils` class that wraps table interactions and works generically across different table implementations.

**Constraints:**
- Works with standard `<table>` and custom grid components
- Column lookup by header name (not position)
- Handles pagination automatically
- Returns typed row data as `List<Map<String, String>>`

**Solution:**
```java
public class TableUtils {

    private final Locator tableRoot;
    private final Page page;

    public TableUtils(Page page, Locator tableRoot) {
        this.page = page;
        this.tableRoot = tableRoot;
    }

    // Get all column headers
    public List<String> getHeaders() {
        return tableRoot.locator("thead th").allTextContents()
            .stream().map(String::trim).collect(Collectors.toList());
    }

    // Get row count (excluding header)
    public int getRowCount() {
        return tableRoot.locator("tbody tr").count();
    }

    // Get all rows as list of maps (column name → cell value)
    public List<Map<String, String>> getAllRows() {
        List<String> headers = getHeaders();
        List<Map<String, String>> rows = new ArrayList<>();

        List<Locator> rowLocators = tableRoot.locator("tbody tr").all();
        for (Locator row : rowLocators) {
            List<String> cells = row.locator("td").allTextContents()
                .stream().map(String::trim).collect(Collectors.toList());
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(headers.size(), cells.size()); i++) {
                rowMap.put(headers.get(i), cells.get(i));
            }
            rows.add(rowMap);
        }
        return rows;
    }

    // Find first row matching a column value
    public Optional<Map<String, String>> findRowBy(String columnName, String value) {
        return getAllRows().stream()
            .filter(row -> value.equals(row.get(columnName)))
            .findFirst();
    }

    // Get a specific cell value by row index and column name
    public String getCellValue(int rowIndex, String columnName) {
        List<String> headers = getHeaders();
        int colIndex = headers.indexOf(columnName);
        if (colIndex == -1) throw new RuntimeException("Column not found: " + columnName);

        return tableRoot.locator("tbody tr").nth(rowIndex)
            .locator("td").nth(colIndex).textContent().trim();
    }

    // Click action button in a specific row
    public void clickRowAction(String columnName, String rowValue, String actionLabel) {
        List<String> headers = getHeaders();
        List<Locator> rows = tableRoot.locator("tbody tr").all();

        int colIndex = headers.indexOf(columnName);
        for (Locator row : rows) {
            String cellText = row.locator("td").nth(colIndex).textContent().trim();
            if (rowValue.equals(cellText)) {
                row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
                    .setName(actionLabel)).click();
                return;
            }
        }
        throw new RuntimeException("Row with " + columnName + "=" + rowValue + " not found");
    }

    // Validate column is sorted
    public boolean isColumnSorted(String columnName, boolean ascending) {
        List<String> values = getAllRows().stream()
            .map(row -> row.getOrDefault(columnName, ""))
            .collect(Collectors.toList());

        List<String> sorted = new ArrayList<>(values);
        if (ascending) Collections.sort(sorted);
        else sorted.sort(Collections.reverseOrder());
        return values.equals(sorted);
    }

    // Get all rows across all pages
    public List<Map<String, String>> getAllRowsWithPagination(Locator nextButton) {
        List<Map<String, String>> allRows = new ArrayList<>();
        do {
            allRows.addAll(getAllRows());
            if (!nextButton.isEnabled()) break;
            nextButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } while (true);
        return allRows;
    }

    // Verify empty state message
    public boolean isEmptyState(String emptyMessage) {
        Locator emptyLocator = tableRoot.locator(".empty-state, [data-testid='empty-table']");
        return emptyLocator.isVisible() && emptyLocator.textContent().contains(emptyMessage);
    }
}

// Usage in test
TableUtils ordersTable = new TableUtils(page, page.locator("[data-testid='orders-table']"));
Map<String, String> row = ordersTable.findRowBy("Order ID", "ORD-00123")
    .orElseThrow(() -> new AssertionError("Order not found in table"));
assertThat(row.get("Status")).isEqualTo("CONFIRMED");
assertThat(Double.parseDouble(row.get("Amount").replace("$", "").replace(",", "")))
    .isGreaterThan(0);
ordersTable.clickRowAction("Order ID", "ORD-00123", "View Details");
```

---

## Coding Q7: Build a Pagination Handler

**Problem:** A product catalog has 100+ pages of results. Tests need to navigate, find items, and extract all data — handling both number-based and load-more pagination.

**Difficulty:** Medium

**Task:** Build a `PaginationHandler` that supports click-through pagination and infinite scroll / load-more patterns.

**Constraints:**
- Max page limit to prevent infinite loops
- Pluggable item extractor (lambda)
- Works with both button-next and page-number pagination

**Solution:**
```java
public class PaginationHandler {

    private final Page page;
    private static final int MAX_PAGES = 200;
    private static final Logger log = LoggerFactory.getLogger(PaginationHandler.class);

    public PaginationHandler(Page page) { this.page = page; }

    // Click-through pagination — collect items from all pages
    public <T> List<T> collectAll(Locator nextButton, Locator itemsLocator,
                                   Function<Locator, T> extractor) {
        List<T> results = new ArrayList<>();
        int pageNum = 1;

        while (pageNum <= MAX_PAGES) {
            log.info("[PAGINATION] Collecting from page {}", pageNum);
            List<Locator> items = itemsLocator.all();
            items.forEach(item -> results.add(extractor.apply(item)));

            if (!nextButton.isEnabled() || !nextButton.isVisible()) break;
            nextButton.click();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            pageNum++;
        }

        if (pageNum > MAX_PAGES) log.warn("Reached max page limit: {}", MAX_PAGES);
        log.info("[PAGINATION] Collected {} items across {} pages", results.size(), pageNum);
        return results;
    }

    // Find first page containing item matching predicate
    public <T> Optional<T> findFirst(Locator nextButton, Locator itemsLocator,
                                      Function<Locator, T> extractor, Predicate<T> predicate) {
        int pageNum = 1;
        while (pageNum <= MAX_PAGES) {
            List<Locator> items = itemsLocator.all();
            for (Locator item : items) {
                T value = extractor.apply(item);
                if (predicate.test(value)) return Optional.of(value);
            }
            if (!nextButton.isEnabled() || !nextButton.isVisible()) break;
            nextButton.click();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            pageNum++;
        }
        return Optional.empty();
    }

    // Load-more / infinite scroll pagination
    public <T> List<T> collectAllWithLoadMore(Locator loadMoreButton, Locator itemsLocator,
                                               Function<Locator, T> extractor) {
        int previousCount = 0;
        int maxIterations = MAX_PAGES;

        while (maxIterations-- > 0) {
            int currentCount = itemsLocator.count();
            if (currentCount == previousCount) break; // No new items loaded
            previousCount = currentCount;

            if (!loadMoreButton.isVisible() || !loadMoreButton.isEnabled()) break;
            loadMoreButton.click();
            page.waitForFunction(
                "count => document.querySelectorAll('.product-item').length > count",
                currentCount
            );
        }

        List<T> results = new ArrayList<>();
        itemsLocator.all().forEach(item -> results.add(extractor.apply(item)));
        return results;
    }

    // Navigate to specific page number
    public void goToPage(int targetPage, Locator pageNumberInput) {
        pageNumberInput.fill(String.valueOf(targetPage));
        pageNumberInput.press("Enter");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }
}

// Usage
PaginationHandler pager = new PaginationHandler(page);
List<String> allOrderIds = pager.collectAll(
    page.locator("[data-testid='next-page']"),
    page.locator(".order-row"),
    row -> row.locator(".order-id").textContent().trim()
);
assertThat(allOrderIds).hasSize(greaterThan(50));
assertThat(allOrderIds).contains("ORD-00001");
```

---

## Coding Q8: Build an Action-Level Retry Wrapper

**Problem:** In a stock trading app, certain UI elements flicker during live data updates. `StaleElementException` equivalents and transient failures need retrying at the action level — not the whole test.

**Difficulty:** Medium

**Task:** Build a `Retry` utility that retries any `Runnable`/`Supplier` with configurable attempts, delay, backoff strategy, and specific exception filtering.

**Constraints:**
- Exponential backoff with jitter
- Filter by exception type (don't retry assertion failures)
- Log every retry attempt
- Return value support (Supplier)

**Solution:**
```java
public class Retry {

    private static final Logger log = LoggerFactory.getLogger(Retry.class);

    public static void execute(int maxAttempts, long initialDelayMs, Runnable action) {
        execute(maxAttempts, initialDelayMs, () -> { action.run(); return null; });
    }

    public static <T> T execute(int maxAttempts, long initialDelayMs, Supplier<T> action) {
        return execute(maxAttempts, initialDelayMs, List.of(PlaywrightException.class), action);
    }

    @SafeVarargs
    public static <T> T execute(int maxAttempts, long initialDelayMs,
                                 List<Class<? extends Exception>> retryOn,
                                 Supplier<T> action) {
        Exception lastEx = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                boolean shouldRetry = retryOn.stream().anyMatch(c -> c.isInstance(e));

                if (!shouldRetry) throw e; // Don't retry assertion errors, NPE, etc.

                lastEx = e;
                if (attempt < maxAttempts) {
                    long delay = calculateBackoff(initialDelayMs, attempt);
                    log.warn("[RETRY] Attempt {}/{} failed ({}). Retrying in {}ms.",
                        attempt, maxAttempts, e.getMessage(), delay);
                    sleep(delay);
                }
            }
        }
        throw new RuntimeException("All " + maxAttempts + " attempts failed", lastEx);
    }

    // Exponential backoff with jitter: delay * 2^(attempt-1) + random(0, 100)
    private static long calculateBackoff(long initialDelayMs, int attempt) {
        long exponential = initialDelayMs * (1L << (attempt - 1));
        long jitter = (long) (Math.random() * 100);
        return Math.min(exponential + jitter, 10_000); // cap at 10s
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // Preset retry configurations
    public static class Presets {
        public static <T> T flaky(Supplier<T> action) {
            return execute(3, 500, action);
        }

        public static <T> T networkBound(Supplier<T> action) {
            return execute(5, 1000, action);
        }

        public static void uiAction(Runnable action) {
            execute(3, 300, action);
        }
    }
}

// Usage
// Retry a specific flaky action
String confirmationCode = Retry.execute(3, 500, () ->
    page.locator(".confirmation-code").textContent()
);

// Retry with preset
Retry.Presets.uiAction(() ->
    page.locator("#submit-btn").click()
);

// Retry page-level element wait (network-bound)
APIResponse resp = Retry.Presets.networkBound(() ->
    api.get("/v1/orders/" + orderId, OrderResponse.class).getStatus().equals("CONFIRMED")
        ? api.get("/v1/orders/" + orderId, OrderResponse.class)
        : (APIResponse)(Object) (() -> { throw new PlaywrightException("Not ready"); }).get()
);
```

---

## Coding Q9: Build a File Download Validator

**Problem:** Enterprise app exports CSV, PDF, and Excel reports. Tests must validate: correct file name, MIME type, size constraints, content structure, and line counts.

**Difficulty:** Medium

**Task:** Build a `DownloadValidator` that captures downloads, validates file properties, and parses content.

**Constraints:**
- Validate PDF magic bytes, CSV headers, Excel structure
- Configurable size min/max thresholds
- Clean up downloaded files after validation
- Works with multiple simultaneous downloads

**Solution:**
```java
public class DownloadValidator {

    private final Path downloadDir;

    public DownloadValidator() {
        this.downloadDir = Paths.get("target/downloads");
        try { Files.createDirectories(downloadDir); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    // Capture and validate download
    public DownloadResult capture(Page page, Runnable trigger) {
        Download download = page.waitForDownload(trigger::run);
        assertThat(download.failure()).as("Download should not fail").isNull();
        return new DownloadResult(download, downloadDir);
    }

    public static class DownloadResult {
        private final Download download;
        private final Path savedPath;

        public DownloadResult(Download download, Path dir) {
            this.download = download;
            String name = download.suggestedFilename();
            this.savedPath = dir.resolve(name);
            download.saveAs(savedPath);
        }

        public DownloadResult assertFileName(String pattern) {
            assertThat(download.suggestedFilename()).matches(pattern);
            return this;
        }

        public DownloadResult assertFileSizeBetween(long minBytes, long maxBytes) {
            try {
                long size = Files.size(savedPath);
                assertThat(size).isBetween(minBytes, maxBytes);
            } catch (IOException e) { throw new RuntimeException(e); }
            return this;
        }

        public DownloadResult assertIsPdf() {
            try {
                byte[] bytes = Files.readAllBytes(savedPath);
                assertThat(new String(Arrays.copyOf(bytes, 4)))
                    .as("File should start with PDF magic bytes").isEqualTo("%PDF");
            } catch (IOException e) { throw new RuntimeException(e); }
            return this;
        }

        public DownloadResult assertCsvHeaders(String... expectedHeaders) {
            try (BufferedReader reader = Files.newBufferedReader(savedPath)) {
                String headerLine = reader.readLine();
                assertThat(headerLine).isNotNull();
                List<String> actual = Arrays.asList(headerLine.split(","));
                assertThat(actual).containsExactly(expectedHeaders);
            } catch (IOException e) { throw new RuntimeException(e); }
            return this;
        }

        public DownloadResult assertCsvRowCount(int expectedRows) {
            try (Stream<String> lines = Files.lines(savedPath)) {
                long count = lines.count() - 1; // subtract header
                assertThat(count).isEqualTo(expectedRows);
            } catch (IOException e) { throw new RuntimeException(e); }
            return this;
        }

        public List<Map<String, String>> parseCsv() {
            List<Map<String, String>> rows = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(savedPath)) {
                String[] headers = reader.readLine().split(",");
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] cells = line.split(",");
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        row.put(headers[i].trim(), i < cells.length ? cells[i].trim() : "");
                    }
                    rows.add(row);
                }
            } catch (IOException e) { throw new RuntimeException(e); }
            return rows;
        }

        public void cleanup() {
            try { Files.deleteIfExists(savedPath); }
            catch (IOException e) { /* log warning */ }
        }
    }
}

// Usage in test
DownloadValidator validator = new DownloadValidator();
DownloadValidator.DownloadResult result = validator.capture(page,
    () -> page.locator("#export-orders-csv").click()
);

result
    .assertFileName("orders-\\d{8}\\.csv")
    .assertFileSizeBetween(1024, 10 * 1024 * 1024) // 1KB to 10MB
    .assertCsvHeaders("Order ID", "Customer", "Amount", "Status", "Date")
    .assertCsvRowCount(25);

List<Map<String, String>> rows = result.parseCsv();
assertThat(rows).allMatch(row -> List.of("CONFIRMED", "PENDING", "CANCELLED")
    .contains(row.get("Status")));
result.cleanup();
```

---

## Coding Q10: Build a File Upload Utility with Validation

**Problem:** Insurance claim portal accepts PDF/JPG/PNG documents up to 10MB. Tests need to: upload valid files, test file type restrictions, test size limits, and verify upload success.

**Difficulty:** Medium

**Task:** Build an `UploadUtils` class supporting direct input uploads, drag-drop zones, and multiple file uploads with content generation.

**Solution:**
```java
public class UploadUtils {

    // Generate in-memory PDF file of specified size
    public static FilePayload generatePdf(String filename, int approximateSizeKb) {
        StringBuilder content = new StringBuilder("%PDF-1.4\n");
        // Pad to approximate size
        String padding = "% " + "X".repeat(512) + "\n";
        while (content.length() < approximateSizeKb * 1024) {
            content.append(padding);
        }
        content.append("%%EOF");
        return new FilePayload(filename, "application/pdf", content.toString().getBytes());
    }

    // Generate CSV content
    public static FilePayload generateCsv(String filename, int rows) {
        StringBuilder csv = new StringBuilder("ID,Name,Amount,Date\n");
        for (int i = 1; i <= rows; i++) {
            csv.append(String.format("%d,Test Entity %d,%d.00,2024-01-%02d\n",
                i, i, i * 100, (i % 28) + 1));
        }
        return new FilePayload(filename, "text/csv", csv.toString().getBytes());
    }

    // Upload via input[type=file]
    public static void uploadViaInput(Page page, Locator fileInput, FilePayload... files) {
        fileInput.setInputFiles(files);
    }

    // Upload via file chooser (button triggers OS dialog)
    public static void uploadViaChooser(Page page, Locator triggerButton, Path... filePaths) {
        FileChooser chooser = page.waitForFileChooser(triggerButton::click);
        chooser.setFiles(filePaths);
    }

    // Upload via drag and drop zone
    public static void uploadViaDragDrop(Page page, Locator dropZone, FilePayload file) {
        // Simulate DataTransfer with file
        page.evaluate("""
            (args) => {
                const [selector, name, mimeType, base64Content] = args;
                const zone = document.querySelector(selector);
                const dt = new DataTransfer();
                const blob = new Blob([atob(base64Content)], {type: mimeType});
                const f = new File([blob], name, {type: mimeType});
                dt.items.add(f);
                zone.dispatchEvent(new DragEvent('dragover', {dataTransfer: dt, bubbles: true}));
                zone.dispatchEvent(new DragEvent('drop', {dataTransfer: dt, bubbles: true}));
            }
            """,
            List.of(getSelector(dropZone), file.name(), file.mimeType(),
                Base64.getEncoder().encodeToString(file.buffer()))
        );
    }

    // Wait for upload to complete
    public static void waitForUploadSuccess(Page page, Locator successIndicator) {
        successIndicator.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(30_000));
    }

    // Validate file type restriction
    public static void assertFileTypeRejected(Page page, Locator errorMessage, String filename) {
        assertThat(errorMessage).isVisible();
        assertThat(errorMessage).containsText(
            Pattern.compile("(invalid|unsupported|not allowed|file type)", Pattern.CASE_INSENSITIVE));
    }

    private static String getSelector(Locator locator) {
        // Extract selector string for JS evaluation
        return locator.toString().replaceAll("Locator@.*?\\[", "").replace("]", "");
    }
}

// Usage in test
@Test
public void testValidPdfUpload() {
    page.navigate(config.getBaseUrl() + "/claims/new");
    FilePayload pdf = UploadUtils.generatePdf("claim-document.pdf", 512); // 512KB
    UploadUtils.uploadViaInput(page, page.locator("input[type='file']"), pdf);
    UploadUtils.waitForUploadSuccess(page, page.locator(".upload-success-badge"));
    assertThat(page.locator(".uploaded-file-name")).hasText("claim-document.pdf");
}

@Test
public void testFileSizeRejection() {
    FilePayload oversized = UploadUtils.generatePdf("huge-file.pdf", 15_000); // 15MB > 10MB limit
    page.locator("input[type='file']").setInputFiles(oversized);
    assertThat(page.locator(".upload-error")).containsText("File size exceeds 10MB limit");
}
```

---

## Coding Q11: Build a Frame Utility for Complex iFrame Interactions

**Problem:** A banking portal embeds payment forms, document viewers, and third-party widgets in iframes — some nested, some cross-origin, some lazy-loaded.

**Difficulty:** Hard

**Task:** Build a `FrameUtils` class that handles: frame lookup by multiple strategies, nested frames, and frame state validation.

**Solution:**
```java
public class FrameUtils {

    private final Page page;

    public FrameUtils(Page page) { this.page = page; }

    // Get frame by CSS selector
    public FrameLocator bySelector(String selector) {
        return page.frameLocator(selector);
    }

    // Get frame by src URL pattern
    public FrameLocator bySrcPattern(String urlPattern) {
        return page.frameLocator("iframe[src*='" + urlPattern + "']");
    }

    // Get frame by title/name attribute
    public FrameLocator byTitle(String title) {
        return page.frameLocator("iframe[title='" + title + "']");
    }

    // Wait for lazy-loaded frame to appear
    public FrameLocator waitForFrame(String selector, int timeoutMs) {
        page.locator(selector).waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED)
            .setTimeout(timeoutMs));
        return page.frameLocator(selector);
    }

    // Nested frame — chain frameLocator calls
    public FrameLocator nested(String outerSelector, String innerSelector) {
        return page.frameLocator(outerSelector).frameLocator(innerSelector);
    }

    // Get Frame object by URL (for direct frame API access)
    public Frame getFrameByUrl(String urlFragment) {
        return page.frames().stream()
            .filter(f -> f.url().contains(urlFragment))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Frame not found with URL: " + urlFragment));
    }

    // Check if frame is loaded and accessible
    public boolean isFrameReady(String selector) {
        try {
            Locator frame = page.locator(selector);
            if (!frame.isVisible()) return false;
            // Try accessing content inside frame
            page.frameLocator(selector).locator("body").waitFor(
                new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    // Execute action in frame with retry (for lazy-loading frames)
    public <T> T executeInFrame(String frameSelector, Function<FrameLocator, T> action) {
        return Retry.execute(3, 1000, () -> {
            FrameLocator frame = page.frameLocator(frameSelector);
            return action.apply(frame);
        });
    }

    // Stripe payment frame example
    public void fillStripeCardDetails(String cardNumber, String expiry, String cvc) {
        FrameLocator cardFrame = bySrcPattern("stripe.com");
        cardFrame.locator("[name='cardnumber']").fill(cardNumber);

        FrameLocator expiryFrame = bySrcPattern("stripe.com/elements/frame/expiry");
        expiryFrame.locator("[name='exp-date']").fill(expiry);

        FrameLocator cvcFrame = bySrcPattern("stripe.com/elements/frame/cvc");
        cvcFrame.locator("[name='cvc']").fill(cvc);
    }
}

// Usage
FrameUtils frames = new FrameUtils(page);

// Payment form in Stripe iframe
frames.fillStripeCardDetails("4111111111111111", "12/26", "123");

// Lazy-loaded document viewer
FrameLocator docViewer = frames.waitForFrame("#document-viewer-iframe", 15_000);
assertThat(docViewer.locator(".page-count")).containsText("5 pages");

// Nested widget
FrameLocator deepWidget = frames.nested("#outer-widget", "#inner-chart");
assertThat(deepWidget.locator(".chart-title")).hasText("Revenue Trend");
```

---

## Coding Q12: Build a Multi-Tab Handler

**Problem:** Reports module opens drill-down data in new tabs. Dashboard links launch in new windows. Tests must navigate between tabs, pass data, and validate across multiple pages.

**Difficulty:** Medium

**Task:** Build a `TabManager` that tracks open pages, switches between them, and handles cleanup.

**Solution:**
```java
public class TabManager {

    private final BrowserContext context;
    private final Deque<Page> pageStack = new ArrayDeque<>();

    public TabManager(BrowserContext context, Page initialPage) {
        this.context = context;
        this.pageStack.push(initialPage);
    }

    public Page currentPage() {
        return pageStack.peek();
    }

    // Open new tab and switch to it
    public Page openNewTab(String url) {
        Page newPage = context.newPage();
        newPage.navigate(url);
        newPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
        pageStack.push(newPage);
        return newPage;
    }

    // Capture tab opened by click (target=_blank)
    public Page captureNewTab(Runnable triggerAction) {
        Page newPage = context.waitForPage(triggerAction::run);
        newPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
        pageStack.push(newPage);
        return newPage;
    }

    // Capture popup window
    public Page capturePopup(Runnable triggerAction) {
        Page popup = currentPage().waitForPopup(triggerAction::run);
        popup.waitForLoadState(LoadState.DOMCONTENTLOADED);
        pageStack.push(popup);
        return popup;
    }

    // Switch to tab by URL pattern
    public Page switchToTabByUrl(String urlPattern) {
        Page target = context.pages().stream()
            .filter(p -> p.url().matches(urlPattern.replace("*", ".*")))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No tab matching: " + urlPattern));
        target.bringToFront();
        pageStack.remove(target);
        pageStack.push(target);
        return target;
    }

    // Switch to tab by title
    public Page switchToTabByTitle(String title) {
        Page target = context.pages().stream()
            .filter(p -> p.title().contains(title))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No tab with title: " + title));
        target.bringToFront();
        pageStack.remove(target);
        pageStack.push(target);
        return target;
    }

    // Close current tab and return to previous
    public Page closeCurrentTab() {
        Page current = pageStack.pop();
        current.close();
        Page previous = pageStack.peek();
        if (previous != null) previous.bringToFront();
        return previous;
    }

    // Get all open tab URLs
    public List<String> getOpenTabUrls() {
        return context.pages().stream().map(Page::url).collect(Collectors.toList());
    }

    // Close all tabs except the first
    public void closeAllExceptFirst() {
        while (pageStack.size() > 1) {
            closeCurrentTab();
        }
    }
}

// Usage
TabManager tabs = new TabManager(context, page);

// Trigger new tab from report link
Page reportTab = tabs.captureNewTab(
    () -> page.locator("a[data-action='open-report']").click()
);
assertThat(reportTab.locator("h1")).hasText("Q4 2024 Report");

// Extract data from new tab
String reportId = reportTab.locator(".report-id").textContent();

// Switch back to original
tabs.closeCurrentTab();
// Verify data returned to original page
page.locator("#selected-report-id").fill(reportId);
```

---

## Coding Q13: Build a Smart Locator Utility

**Problem:** Tests break frequently due to DOM changes. Need a resilient locator strategy that tries multiple selectors in priority order and uses the first stable one.

**Difficulty:** Medium

**Task:** Build a `SmartLocator` that tries locators in priority order: testId → role → label → text → CSS. Falls back gracefully and logs which strategy succeeded.

**Solution:**
```java
public class SmartLocator {

    private final Page page;
    private static final Logger log = LoggerFactory.getLogger(SmartLocator.class);

    public SmartLocator(Page page) { this.page = page; }

    // Try multiple locator strategies in order
    public Locator find(LocatorSpec... specs) {
        for (LocatorSpec spec : specs) {
            try {
                Locator candidate = spec.resolve(page);
                if (candidate.count() == 1) {
                    log.debug("[LOCATOR] Resolved via: {}", spec.description());
                    return candidate;
                }
            } catch (PlaywrightException e) {
                log.debug("[LOCATOR] Strategy failed ({}): {}", spec.description(), e.getMessage());
            }
        }
        throw new RuntimeException("No locator strategy resolved to exactly one element. Tried: " +
            Arrays.stream(specs).map(LocatorSpec::description).collect(Collectors.joining(", ")));
    }

    // Fluent builder for locator candidates
    public static LocatorSpec byTestId(String testId) {
        return new LocatorSpec(
            p -> p.getByTestId(testId),
            "testId=" + testId
        );
    }

    public static LocatorSpec byRole(AriaRole role, String name) {
        return new LocatorSpec(
            p -> p.getByRole(role, new Page.GetByRoleOptions().setName(name)),
            "role=" + role + ", name=" + name
        );
    }

    public static LocatorSpec byLabel(String label) {
        return new LocatorSpec(
            p -> p.getByLabel(label),
            "label=" + label
        );
    }

    public static LocatorSpec byText(String text) {
        return new LocatorSpec(
            p -> p.getByText(text, new Page.GetByTextOptions().setExact(true)),
            "text=" + text
        );
    }

    public static LocatorSpec byCss(String selector) {
        return new LocatorSpec(
            p -> p.locator(selector),
            "css=" + selector
        );
    }

    public record LocatorSpec(Function<Page, Locator> resolver, String description) {
        Locator resolve(Page page) { return resolver.apply(page); }
    }
}

// Usage
SmartLocator smart = new SmartLocator(page);

Locator submitBtn = smart.find(
    SmartLocator.byTestId("submit-payment-btn"),           // 1st priority
    SmartLocator.byRole(AriaRole.BUTTON, "Submit Payment"),// 2nd
    SmartLocator.byText("Submit Payment"),                 // 3rd
    SmartLocator.byCss("form.payment-form button[type='submit']") // fallback
);
submitBtn.click();
```

---

## Coding Q14: Build a Config Reader with Environment Override

**Problem:** Framework needs to support multiple environments (dev/staging/prod), with YAML-based config, system property overrides, and typed config access.

**Difficulty:** Medium

**Task:** Build a `ConfigReader` using SnakeYAML that loads environment-specific YAML, merges with base config, and validates required fields.

**Solution:**
```java
public class ConfigReader {

    private final Map<String, Object> config;
    private static ConfigReader instance;

    private ConfigReader() {
        Yaml yaml = new Yaml();
        Map<String, Object> base = loadYaml("config/base.yml");
        String env = System.getProperty("env",
            System.getenv().getOrDefault("TEST_ENV", "staging"));
        Map<String, Object> envConfig = loadYaml("config/" + env + ".yml");

        // Deep merge env config over base
        this.config = deepMerge(base, envConfig);
        validateRequired();
    }

    public static synchronized ConfigReader getInstance() {
        if (instance == null) instance = new ConfigReader();
        return instance;
    }

    public String getBaseUrl() {
        return getOverridable("base.url", "BASE_URL");
    }

    public String getApiBaseUrl() {
        return getOverridable("api.base.url", "API_BASE_URL");
    }

    public int getDefaultTimeout() {
        return Integer.parseInt(getString("timeouts.default", "30000"));
    }

    public int getNavigationTimeout() {
        return Integer.parseInt(getString("timeouts.navigation", "60000"));
    }

    public boolean isFeatureEnabled(String featureName) {
        return Boolean.parseBoolean(getString("features." + featureName, "false"));
    }

    private String getOverridable(String configKey, String envVar) {
        String envValue = System.getenv(envVar);
        return (envValue != null && !envValue.isBlank()) ? envValue : getString(configKey, null);
    }

    @SuppressWarnings("unchecked")
    private String getString(String dotPath, String defaultValue) {
        String[] parts = dotPath.split("\\.");
        Object current = config;
        for (String part : parts) {
            if (!(current instanceof Map)) return defaultValue;
            current = ((Map<String, Object>) current).get(part);
        }
        return current != null ? current.toString() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return new HashMap<>();
            return new Yaml().load(is);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config: " + resourcePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new HashMap<>(base);
        override.forEach((key, value) -> {
            if (value instanceof Map && result.get(key) instanceof Map) {
                result.put(key, deepMerge(
                    (Map<String, Object>) result.get(key),
                    (Map<String, Object>) value));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private void validateRequired() {
        List<String> required = List.of("base.url", "api.base.url");
        List<String> missing = required.stream()
            .filter(key -> getString(key, null) == null)
            .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new RuntimeException("Missing required config keys: " + missing);
        }
    }
}
```

```yaml
# src/test/resources/config/base.yml
timeouts:
  default: 30000
  navigation: 60000
features:
  new_ui: false
  dark_mode: false

# src/test/resources/config/staging.yml
base:
  url: https://staging.example.com
api:
  base:
    url: https://api-staging.example.com
features:
  new_ui: true
```

---

## Coding Q15: Build a Reporting Utility with Screenshot and Trace Attachment

**Problem:** Test failures in CI are hard to diagnose. Need a unified reporting utility that: captures screenshots, saves Playwright traces, attaches to Allure, and logs structured failure summaries.

**Difficulty:** Medium

**Task:** Build a `TestReporter` JUnit 5 extension that automatically handles failure evidence collection and Allure attachment.

**Solution:**
```java
@ExtendWith(TestReporter.class)
public class TestReporterExtension
    implements BeforeEachCallback, AfterEachCallback, TestWatcher {

    private static final ThreadLocal<BrowserContext> contextRef = new ThreadLocal<>();
    private static final ThreadLocal<Page> pageRef = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(TestReporterExtension.class);

    public static void register(BrowserContext context, Page page) {
        contextRef.set(context);
        pageRef.set(page);
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        // Start tracing
        BrowserContext context = contextRef.get();
        if (context != null) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true));
        }
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        String testName = ctx.getDisplayName().replaceAll("[^a-zA-Z0-9_]", "_");
        Page page = pageRef.get();
        BrowserContext context = contextRef.get();

        if (page != null) {
            attachScreenshot(testName, page);
            log.error("[FAIL] Test: {} | URL: {} | Error: {}",
                testName, page.url(), cause.getMessage());
        }

        if (context != null) {
            attachTrace(testName, context);
        }

        // Log structured failure summary
        Allure.addAttachment("Failure Summary", "text/plain",
            buildFailureSummary(ctx, cause, page));
    }

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        BrowserContext context = contextRef.get();
        if (context != null) {
            context.tracing().stop(); // stop without saving
        }
    }

    @Attachment(value = "Screenshot on Failure", type = "image/png")
    private byte[] attachScreenshot(String testName, Page page) {
        try {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (PlaywrightException e) {
            log.warn("Screenshot capture failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    @Attachment(value = "Playwright Trace", type = "application/zip")
    private byte[] attachTrace(String testName, BrowserContext context) {
        try {
            Path tracePath = Paths.get("target/traces/" + testName + ".zip");
            Files.createDirectories(tracePath.getParent());
            context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
            return Files.readAllBytes(tracePath);
        } catch (IOException e) {
            log.warn("Trace capture failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    private String buildFailureSummary(ExtensionContext ctx, Throwable cause, Page page) {
        return String.format("""
            Test: %s
            Class: %s
            Timestamp: %s
            URL: %s
            Error: %s
            """,
            ctx.getDisplayName(),
            ctx.getTestClass().map(Class::getSimpleName).orElse("Unknown"),
            Instant.now(),
            page != null ? page.url() : "N/A",
            cause.getMessage()
        );
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        contextRef.remove();
        pageRef.remove();
    }
}

// Usage in test base class
@ExtendWith(TestReporterExtension.class)
public abstract class BaseTest {

    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    public void setUp() {
        context = BrowserFactory.getBrowser().newContext();
        page = context.newPage();
        TestReporterExtension.register(context, page); // register for reporter
    }

    @AfterEach
    public void tearDown() {
        context.close();
    }
}
```

---

*— End of Coding Q1–Q15 | Section 2 —*

> Next batch: **Coding Q16–Q30** — Parallel-safe execution layer, API contract validator, Dynamic form filler, Multi-step wizard navigator, and more.

---

## Coding Q16: Build a Parallel-Safe Test Execution Layer

**Problem:** 500 tests need to run in parallel across 4 threads. Each test must have its own isolated browser context, auth state, and test data — zero shared state between threads.

**Difficulty:** Hard

**Task:** Build a `TestExecutionContext` that manages the full lifecycle (Playwright → Browser → Context → Page) per thread, with auth state injection per user role.

**Constraints:**
- One `Playwright` and `Browser` per thread (ThreadLocal)
- One `BrowserContext` per test (new each `@BeforeEach`)
- Auth state loaded from pre-generated files
- Clean teardown even on test failure

**Solution:**
```java
public class TestExecutionContext {

    // Thread-local Playwright and Browser — created once per thread
    private static final ThreadLocal<Playwright> TL_PLAYWRIGHT =
        ThreadLocal.withInitial(Playwright::create);

    private static final ThreadLocal<Browser> TL_BROWSER =
        ThreadLocal.withInitial(() -> TL_PLAYWRIGHT.get().chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(Boolean.parseBoolean(
                    System.getenv().getOrDefault("HEADLESS", "true")))
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage"))
        ));

    // Per-test context and page (set in @BeforeEach, cleared in @AfterEach)
    private static final ThreadLocal<BrowserContext> TL_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> TL_PAGE = new ThreadLocal<>();

    public static Browser getBrowser() {
        return TL_BROWSER.get();
    }

    public static BrowserContext getContext() {
        return TL_CONTEXT.get();
    }

    public static Page getPage() {
        return TL_PAGE.get();
    }

    // Create fresh context per test — optionally injecting auth state
    public static Page initTest(LoginManager.UserRole role) {
        Browser.NewContextOptions opts = new Browser.NewContextOptions()
            .setViewportSize(1920, 1080)
            .setLocale("en-US")
            .setTimezoneId("America/New_York");

        if (role != null && LoginManager.hasValidState(role)) {
            opts.setStorageStatePath(role.statePath());
        }

        BrowserContext context = TL_BROWSER.get().newContext(opts);

        // Start tracing per test
        context.tracing().start(new Tracing.StartOptions()
            .setScreenshots(true).setSnapshots(true));

        Page page = context.newPage();
        TL_CONTEXT.set(context);
        TL_PAGE.set(page);
        TestReporterExtension.register(context, page);
        return page;
    }

    // Teardown — always runs, saves trace on failure
    public static void teardownTest(boolean failed, String testName) {
        BrowserContext context = TL_CONTEXT.get();
        Page page = TL_PAGE.get();
        try {
            if (context != null) {
                if (failed) {
                    Path tracePath = Paths.get("target/traces/" + testName + ".zip");
                    Files.createDirectories(tracePath.getParent());
                    context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
                } else {
                    context.tracing().stop();
                }
                context.close();
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(TestExecutionContext.class)
                .warn("Teardown failed: {}", e.getMessage());
        } finally {
            TL_CONTEXT.remove();
            TL_PAGE.remove();
        }
    }

    // Shutdown entire thread's Playwright/Browser — call from @AfterAll
    public static void shutdownThread() {
        try {
            if (TL_BROWSER.get() != null) TL_BROWSER.get().close();
        } finally {
            TL_BROWSER.remove();
            if (TL_PLAYWRIGHT.get() != null) TL_PLAYWRIGHT.get().close();
            TL_PLAYWRIGHT.remove();
        }
    }
}

// Clean base test using the execution context
@Execution(ExecutionMode.CONCURRENT)
public abstract class ParallelBaseTest {

    protected Page page;
    protected BrowserContext context;
    private boolean testFailed = false;

    protected LoginManager.UserRole getRole() { return null; } // override per suite

    @BeforeEach
    void initTest() {
        page = TestExecutionContext.initTest(getRole());
        context = TestExecutionContext.getContext();
    }

    @AfterEach
    void teardown(TestInfo info) {
        String name = info.getDisplayName().replaceAll("[^a-zA-Z0-9_]", "_")
            + "_" + Thread.currentThread().getId();
        TestExecutionContext.teardownTest(testFailed, name);
    }

    // JUnit 5 lifecycle hook to detect failure
    @RegisterExtension
    final TestWatcher watcher = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext ctx, Throwable cause) {
            testFailed = true;
        }
    };

    @AfterAll
    static void shutdownBrowser() {
        TestExecutionContext.shutdownThread();
    }
}
```

---

## Coding Q17: Build an API Contract Validator

**Problem:** Frontend and backend teams change APIs without coordinating. Tests silently pass with wrong response structures. Need automated contract validation integrated into the test framework.

**Difficulty:** Hard

**Task:** Build an `ApiContractValidator` that validates API responses against JSON Schema definitions loaded from OpenAPI/YAML spec files.

**Constraints:**
- Load JSON Schema from OpenAPI spec or standalone schema files
- Validate response structure, field types, and required fields
- Collect ALL violations (not fail-fast)
- Produce human-readable violation report

**Solution:**
```java
public class ApiContractValidator {

    private final Map<String, Map<String, Object>> schemas = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    // Load schema definitions from a JSON Schema file
    public ApiContractValidator loadSchema(String schemaName, String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Schema not found: " + resourcePath);
            schemas.put(schemaName, mapper.readValue(is, new TypeReference<>() {}));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schema: " + schemaName, e);
        }
        return this;
    }

    // Validate an API response body against a named schema
    public ValidationResult validate(String schemaName, String responseBody) {
        Map<String, Object> schema = schemas.get(schemaName);
        if (schema == null) throw new RuntimeException("Unknown schema: " + schemaName);

        try {
            Map<String, Object> actual = mapper.readValue(responseBody, new TypeReference<>() {});
            List<String> violations = new ArrayList<>();
            validateNode(actual, schema, "$", violations);
            return new ValidationResult(schemaName, violations);
        } catch (JsonProcessingException e) {
            return new ValidationResult(schemaName,
                List.of("Response is not valid JSON: " + e.getMessage()));
        }
    }

    // Validate Playwright APIResponse directly
    public ValidationResult validate(String schemaName, APIResponse response) {
        assertThat(response).isOK();
        return validate(schemaName, response.text());
    }

    @SuppressWarnings("unchecked")
    private void validateNode(Object actual, Map<String, Object> schema,
                               String path, List<String> violations) {
        String type = (String) schema.getOrDefault("type", "object");

        // Type check
        if (!matchesType(actual, type)) {
            violations.add(String.format("'%s': expected type '%s', got '%s'",
                path, type, getTypeName(actual)));
            return;
        }

        if ("object".equals(type) && actual instanceof Map) {
            Map<String, Object> actualMap = (Map<String, Object>) actual;
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            List<String> required = (List<String>) schema.getOrDefault("required", List.of());

            // Check required fields
            for (String field : required) {
                if (!actualMap.containsKey(field)) {
                    violations.add(String.format("'%s.%s': required field missing", path, field));
                }
            }

            // Validate each property
            if (properties != null) {
                for (Map.Entry<String, Object> prop : properties.entrySet()) {
                    String key = prop.getKey();
                    Object childSchema = prop.getValue();
                    if (actualMap.containsKey(key) && childSchema instanceof Map) {
                        validateNode(actualMap.get(key), (Map<String, Object>) childSchema,
                            path + "." + key, violations);
                    }
                }
            }
        } else if ("array".equals(type) && actual instanceof List) {
            List<Object> actualList = (List<Object>) actual;
            Map<String, Object> itemSchema = (Map<String, Object>) schema.get("items");
            if (itemSchema != null) {
                for (int i = 0; i < actualList.size(); i++) {
                    validateNode(actualList.get(i), itemSchema,
                        path + "[" + i + "]", violations);
                }
            }
        }
    }

    private boolean matchesType(Object value, String type) {
        return switch (type) {
            case "string"  -> value instanceof String;
            case "number"  -> value instanceof Number;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "boolean" -> value instanceof Boolean;
            case "array"   -> value instanceof List;
            case "object"  -> value instanceof Map;
            case "null"    -> value == null;
            default -> true;
        };
    }

    private String getTypeName(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Integer || value instanceof Long) return "integer";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List) return "array";
        if (value instanceof Map) return "object";
        return value.getClass().getSimpleName();
    }

    public record ValidationResult(String schemaName, List<String> violations) {
        public boolean isValid() { return violations.isEmpty(); }

        public void assertValid() {
            if (!isValid()) {
                throw new AssertionError(String.format(
                    "API contract violations for schema '%s':\n%s",
                    schemaName,
                    violations.stream().map(v -> "  - " + v).collect(Collectors.joining("\n"))
                ));
            }
        }
    }
}

// Usage
ApiContractValidator validator = new ApiContractValidator()
    .loadSchema("order", "schemas/order-response.json")
    .loadSchema("customer", "schemas/customer-response.json");

APIResponse response = api.get("/v1/orders/ORD-001");
validator.validate("order", response).assertValid();
```

```json
// src/test/resources/schemas/order-response.json
{
  "type": "object",
  "required": ["orderId", "status", "amount", "customerId"],
  "properties": {
    "orderId":    { "type": "string" },
    "status":     { "type": "string" },
    "amount":     { "type": "number" },
    "customerId": { "type": "string" },
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["sku", "quantity"],
        "properties": {
          "sku":      { "type": "string" },
          "quantity": { "type": "integer" }
        }
      }
    }
  }
}
```

---

## Coding Q18: Build a Dynamic Form Filler

**Problem:** A multi-section form has 30+ fields — text inputs, dropdowns, date pickers, checkboxes, radio buttons, and file uploads — varying by user role and scenario.

**Difficulty:** Medium

**Task:** Build a `FormFiller` that accepts a `Map<String, Object>` of field name→value pairs and fills the form intelligently based on field type detection.

**Constraints:**
- Auto-detect field type (input, select, checkbox, radio, date picker)
- Support nested section fields with `sectionName.fieldName` syntax
- Skip null values (partial form fill)
- Return filled fields list for assertion

**Solution:**
```java
public class FormFiller {

    private final Page page;
    private static final Logger log = LoggerFactory.getLogger(FormFiller.class);

    public FormFiller(Page page) { this.page = page; }

    // Fill entire form from a map
    public List<String> fill(Map<String, Object> fieldValues) {
        List<String> filled = new ArrayList<>();
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            if (entry.getValue() == null) continue;
            try {
                fillField(entry.getKey(), entry.getValue());
                filled.add(entry.getKey());
            } catch (Exception e) {
                log.warn("[FORM] Failed to fill '{}': {}", entry.getKey(), e.getMessage());
                throw new RuntimeException("Failed to fill field: " + entry.getKey(), e);
            }
        }
        return filled;
    }

    private void fillField(String fieldName, Object value) {
        Locator field = resolveField(fieldName);

        // Detect field type and fill accordingly
        String tagName = (String) field.evaluate("el => el.tagName.toLowerCase()");
        String inputType = (String) field.evaluate(
            "el => el.getAttribute('type') || ''");
        String role = (String) field.evaluate(
            "el => el.getAttribute('role') || ''");

        log.debug("[FORM] Filling '{}' (tag={}, type={}) with '{}'",
            fieldName, tagName, inputType, value);

        switch (tagName) {
            case "select" -> fillSelect(field, value.toString());
            case "input" -> {
                switch (inputType.toLowerCase()) {
                    case "checkbox" -> fillCheckbox(field, value);
                    case "radio"    -> fillRadio(fieldName, value.toString());
                    case "file"     -> fillFile(field, value);
                    case "date"     -> fillDate(field, value.toString());
                    default         -> fillText(field, value.toString());
                }
            }
            case "textarea" -> fillText(field, value.toString());
            default -> {
                if ("combobox".equals(role) || "listbox".equals(role)) {
                    fillComboBox(field, value.toString());
                } else {
                    fillText(field, value.toString());
                }
            }
        }
    }

    private Locator resolveField(String fieldName) {
        // Try: label → testid → name attr → placeholder
        if (page.getByLabel(fieldName).count() == 1) return page.getByLabel(fieldName);
        if (page.getByTestId(fieldName).count() == 1) return page.getByTestId(fieldName);
        if (page.locator("[name='" + fieldName + "']").count() == 1)
            return page.locator("[name='" + fieldName + "']");
        if (page.getByPlaceholder(fieldName).count() == 1)
            return page.getByPlaceholder(fieldName);
        throw new RuntimeException("Cannot resolve field: " + fieldName);
    }

    private void fillText(Locator field, String value) {
        field.clear();
        field.fill(value);
    }

    private void fillSelect(Locator field, String value) {
        field.selectOption(value);
    }

    private void fillCheckbox(Locator field, Object value) {
        boolean shouldCheck = value instanceof Boolean ? (Boolean) value
            : Boolean.parseBoolean(value.toString());
        if (shouldCheck) field.check(); else field.uncheck();
    }

    private void fillRadio(String groupName, String value) {
        page.locator(String.format("input[type='radio'][name='%s'][value='%s']",
            groupName, value)).check();
    }

    private void fillDate(Locator field, String value) {
        // ISO format: YYYY-MM-DD
        field.fill(value);
    }

    private void fillFile(Locator field, Object value) {
        if (value instanceof Path) field.setInputFiles((Path) value);
        else if (value instanceof FilePayload) field.setInputFiles((FilePayload) value);
        else field.setInputFiles(Paths.get(value.toString()));
    }

    private void fillComboBox(Locator field, String value) {
        field.click();
        field.fill(value);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(value)).click();
    }
}

// Usage in test
FormFiller filler = new FormFiller(page);
page.navigate(config.getBaseUrl() + "/customers/new");

List<String> filled = filler.fill(Map.of(
    "Company Name",   "Acme Corp",
    "Email",          "billing@acme.com",
    "Industry",       "Technology",      // dropdown
    "tier",           "PREMIUM",         // radio
    "acceptTerms",    true,              // checkbox
    "annualRevenue",  "5000000"
));
assertThat(filled).hasSize(6);
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Customer")).click();
```

---

## Coding Q19: Build a Multi-Step Wizard Navigator

**Problem:** An enterprise onboarding wizard has 6 steps with conditional branching (step 3 skipped for certain user types), validation on each step, and a progress indicator.

**Difficulty:** Hard

**Task:** Build a `WizardNavigator` that models wizard steps as typed objects, validates progress state, and handles conditional step branching.

**Solution:**
```java
public class WizardNavigator {

    private final Page page;
    private final List<WizardStep> steps;
    private int currentStepIndex = 0;

    public WizardNavigator(Page page, List<WizardStep> steps) {
        this.page = page;
        this.steps = steps;
    }

    @FunctionalInterface
    public interface StepAction {
        void execute(Page page);
    }

    public record WizardStep(
        String name,
        StepAction action,
        Predicate<Page> skipCondition,  // null = never skip
        String completionIndicator      // locator for step completed signal
    ) {
        public boolean shouldSkip(Page page) {
            return skipCondition != null && skipCondition.test(page);
        }
    }

    // Execute all steps in order, skipping conditional ones
    public void executeAll() {
        for (WizardStep step : steps) {
            if (step.shouldSkip(page)) {
                LoggerFactory.getLogger(WizardNavigator.class)
                    .info("[WIZARD] Skipping step: {}", step.name());
                continue;
            }
            executeStep(step);
        }
    }

    // Execute a single step
    private void executeStep(WizardStep step) {
        LoggerFactory.getLogger(WizardNavigator.class)
            .info("[WIZARD] Executing step: {}", step.name());

        // Verify we're on the right step
        assertThat(page.locator(".wizard-step--active"))
            .containsText(step.name());

        // Execute step actions (form fill, selection, etc.)
        step.action().execute(page);

        // Click Next / Submit
        Locator nextButton = page.locator("[data-wizard-action='next'], [data-wizard-action='submit']");
        nextButton.click();

        // Wait for completion signal
        if (step.completionIndicator() != null) {
            page.locator(step.completionIndicator()).waitFor();
        }
        currentStepIndex++;
    }

    // Navigate to a specific step by clicking breadcrumb
    public void goToStep(String stepName) {
        page.locator(".wizard-breadcrumb").getByText(stepName).click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    // Get current step name from UI
    public String getCurrentStepName() {
        return page.locator(".wizard-step--active .step-title").textContent().trim();
    }

    // Get progress percentage
    public int getProgressPercentage() {
        String style = page.locator(".wizard-progress-bar").getAttribute("style");
        // Parse "width: 66%" → 66
        return Integer.parseInt(style.replaceAll(".*width:\\s*(\\d+)%.*", "$1"));
    }

    // Validate step indicator
    public void assertStepCompleted(String stepName) {
        assertThat(page.locator(".wizard-step[data-step='" + stepName + "']"))
            .hasClass(Pattern.compile(".*completed.*"));
    }

    // Builder for common step types
    public static WizardStep formStep(String name, Map<String, Object> formData) {
        return new WizardStep(name,
            p -> new FormFiller(p).fill(formData),
            null,
            ".step-form--valid"
        );
    }

    public static WizardStep conditionalStep(String name, StepAction action,
                                              String skipIfLocatorVisible) {
        return new WizardStep(name, action,
            p -> p.locator(skipIfLocatorVisible).isVisible(),
            null
        );
    }
}

// Usage
WizardNavigator wizard = new WizardNavigator(page, List.of(
    WizardNavigator.formStep("Basic Info", Map.of(
        "Company Name", "Acme Corp", "Industry", "Technology")),

    WizardNavigator.formStep("Contact Details", Map.of(
        "Primary Email", "admin@acme.com", "Phone", "+1-555-0100")),

    // Step 3 skipped for Enterprise tier (pre-assigned account manager)
    WizardNavigator.conditionalStep("Account Manager", p ->
        p.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions()
            .setName("Account Manager")).selectOption("Sarah Johnson"),
        "[data-tier='ENTERPRISE']"
    ),

    WizardNavigator.formStep("Billing", Map.of(
        "Billing Address", "123 Main St", "City", "New York")),

    new WizardNavigator.WizardStep("Review & Submit",
        p -> p.getByRole(AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName("I confirm all details are correct")).check(),
        null, ".onboarding-complete"
    )
));

page.navigate(config.getBaseUrl() + "/onboarding");
wizard.executeAll();
assertThat(page.locator(".success-message")).hasText("Onboarding complete!");
```

---

## Coding Q20: Build a Network Audit Helper

**Problem:** In a regulated FinTech app, every outbound API call must include `X-Correlation-ID`, `X-Audit-User`, and a valid `Authorization` header. Failures to include these violate compliance rules.

**Difficulty:** Medium

**Task:** Build a `NetworkAuditHelper` that passively monitors all API calls during a test and produces a compliance report.

**Solution:**
```java
public class NetworkAuditHelper {

    private final Page page;
    private final List<AuditEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private final List<String> requiredHeaders;
    private final String apiUrlPattern;

    public record AuditEntry(
        String method, String url, int status,
        Map<String, String> requestHeaders,
        long durationMs, boolean compliant, List<String> violations
    ) {}

    public NetworkAuditHelper(Page page, String apiUrlPattern, String... requiredHeaders) {
        this.page = page;
        this.apiUrlPattern = apiUrlPattern;
        this.requiredHeaders = List.of(requiredHeaders);
        attachListeners();
    }

    private void attachListeners() {
        Map<String, Long> startTimes = new ConcurrentHashMap<>();

        page.onRequest(request -> {
            if (request.url().matches(apiUrlPattern.replace("*", ".*"))) {
                startTimes.put(request.url() + request.method(), System.currentTimeMillis());
            }
        });

        page.onResponse(response -> {
            String key = response.url() + response.request().method();
            if (!response.url().matches(apiUrlPattern.replace("*", ".*"))) return;

            long duration = System.currentTimeMillis() -
                startTimes.getOrDefault(key, System.currentTimeMillis());

            Map<String, String> headers = response.request().headers();
            List<String> violations = new ArrayList<>();

            for (String required : requiredHeaders) {
                if (!headers.containsKey(required.toLowerCase())) {
                    violations.add("Missing required header: " + required);
                }
            }

            // Validate Authorization format
            String auth = headers.get("authorization");
            if (auth != null && !auth.startsWith("Bearer ")) {
                violations.add("Authorization header must use Bearer scheme");
            }

            entries.add(new AuditEntry(
                response.request().method(), response.url(),
                response.status(), headers, duration,
                violations.isEmpty(), violations
            ));
        });
    }

    // Get all non-compliant calls
    public List<AuditEntry> getViolations() {
        return entries.stream().filter(e -> !e.compliant()).collect(Collectors.toList());
    }

    // Assert no compliance violations
    public void assertFullCompliance() {
        List<AuditEntry> violations = getViolations();
        if (!violations.isEmpty()) {
            StringBuilder report = new StringBuilder("API compliance violations:\n");
            violations.forEach(e -> {
                report.append(String.format("  [%s] %s %s\n", e.status(), e.method(), e.url()));
                e.violations().forEach(v -> report.append("    → ").append(v).append("\n"));
            });
            fail(report.toString());
        }
    }

    // Performance audit — flag calls slower than threshold
    public List<AuditEntry> getSlowCalls(long thresholdMs) {
        return entries.stream()
            .filter(e -> e.durationMs() > thresholdMs)
            .sorted(Comparator.comparingLong(AuditEntry::durationMs).reversed())
            .collect(Collectors.toList());
    }

    // Summary report
    public String getSummary() {
        long total = entries.size();
        long errors = entries.stream().filter(e -> e.status() >= 400).count();
        long violations = getViolations().size();
        return String.format("API Audit: %d calls | %d errors | %d compliance violations",
            total, errors, violations);
    }
}

// Usage
NetworkAuditHelper audit = new NetworkAuditHelper(page,
    "**/api/**",
    "Authorization", "X-Correlation-ID", "X-Audit-User"
);

// Run the test flow
new LoginPage(page).navigate().loginAs("admin@example.com", password);
new DashboardPage(page).navigate().openReports();
new ReportsPage(page).exportToCsv();

// Assert compliance after all actions
System.out.println(audit.getSummary());
audit.assertFullCompliance();

// Performance check
List<NetworkAuditHelper.AuditEntry> slowCalls = audit.getSlowCalls(2000);
assertThat(slowCalls).as("API calls over 2s SLA").isEmpty();
```

---

## Coding Q21: Build a Visual Regression Comparison Helper

**Problem:** Dashboard has 12 widgets. Only 3 are stable for visual regression — others have live data. Need targeted visual comparison with per-widget thresholds and dynamic masking.

**Difficulty:** Medium

**Task:** Build a `VisualComparisonHelper` that manages baseline screenshots, applies dynamic masks, and produces diff reports.

**Solution:**
```java
public class VisualComparisonHelper {

    private final Page page;
    private static final Path BASELINE_DIR = Paths.get("src/test/resources/visual-baselines");
    private static final Path DIFF_DIR = Paths.get("target/visual-diffs");

    public record ComparisonConfig(
        String name,
        Locator element,
        List<Locator> masks,
        double maxDiffRatio  // 0.0 to 1.0
    ) {}

    public VisualComparisonHelper(Page page) {
        this.page = page;
        try {
            Files.createDirectories(BASELINE_DIR);
            Files.createDirectories(DIFF_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Compare element screenshot against baseline
    public void assertMatchesBaseline(ComparisonConfig config) {
        assertThat(config.element()).toHaveScreenshot(
            BASELINE_DIR.resolve(config.name() + ".png").toString(),
            new LocatorAssertions.ToHaveScreenshotOptions()
                .setMaxDiffPixelRatio(config.maxDiffRatio())
                .setMask(config.masks())
        );
    }

    // Compare page screenshot against baseline
    public void assertPageMatchesBaseline(String name, List<Locator> masks, double maxDiffRatio) {
        assertThat(page).toHaveScreenshot(
            BASELINE_DIR.resolve(name + ".png").toString(),
            new PageAssertions.ToHaveScreenshotOptions()
                .setFullPage(true)
                .setMaxDiffPixelRatio(maxDiffRatio)
                .setMask(masks)
        );
    }

    // Update baseline for a specific component
    public void updateBaseline(ComparisonConfig config) {
        Path baselinePath = BASELINE_DIR.resolve(config.name() + ".png");
        byte[] screenshot = config.element().screenshot();
        try {
            Files.write(baselinePath, screenshot);
            LoggerFactory.getLogger(VisualComparisonHelper.class)
                .info("[VISUAL] Baseline updated: {}", baselinePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update baseline: " + config.name(), e);
        }
    }

    // Factory methods for common dashboard widget configs
    public static ComparisonConfig widgetConfig(String name, Page page,
                                                  String widgetTestId, double threshold) {
        return new ComparisonConfig(
            name,
            page.getByTestId(widgetTestId),
            List.of(
                page.locator(".live-data"),
                page.locator(".timestamp"),
                page.locator(".last-updated")
            ),
            threshold
        );
    }
}

// Usage
VisualComparisonHelper visual = new VisualComparisonHelper(page);
page.navigate(config.getBaseUrl() + "/dashboard");
page.waitForLoadState(LoadState.NETWORKIDLE);

// Compare stable widgets with tight threshold
visual.assertMatchesBaseline(
    VisualComparisonHelper.widgetConfig("revenue-chart", page, "revenue-chart-widget", 0.01));

visual.assertMatchesBaseline(
    VisualComparisonHelper.widgetConfig("account-summary", page, "account-summary-widget", 0.02));

// Full page with broader tolerance and more masks
visual.assertPageMatchesBaseline("dashboard-full", List.of(
    page.locator(".live-ticker"),
    page.locator(".user-greeting"),
    page.locator(".notification-count")
), 0.03);
```

---

## Coding Q22: Build a Request Interceptor with Scenario Simulation

**Problem:** Payment service has 6 failure modes: timeout, 500, 402 insufficient funds, 409 duplicate, 422 validation error, 503 service down. Tests need to simulate all 6 without test environment changes.

**Difficulty:** Hard

**Task:** Build a `ScenarioSimulator` that registers named failure scenarios as route handlers, enabling test code to activate specific scenarios declaratively.

**Solution:**
```java
public class ScenarioSimulator {

    private final Page page;
    private final Map<String, RouteHandler> scenarios = new LinkedHashMap<>();

    @FunctionalInterface
    interface RouteHandler {
        void handle(Route route);
    }

    public ScenarioSimulator(Page page) {
        this.page = page;
        registerDefaultScenarios();
    }

    private void registerDefaultScenarios() {
        // Payment failure scenarios
        addScenario("payment.timeout", route ->
            route.abort("timedout")
        );

        addScenario("payment.server_error", route ->
            route.fulfill(new Route.FulfillOptions()
                .setStatus(500)
                .setContentType("application/json")
                .setBody("{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Unexpected failure\"}")
            )
        );

        addScenario("payment.insufficient_funds", route ->
            route.fulfill(new Route.FulfillOptions()
                .setStatus(402)
                .setContentType("application/json")
                .setBody("{\"error\":\"INSUFFICIENT_FUNDS\",\"code\":\"PAY_001\",\"availableBalance\":250.00}")
            )
        );

        addScenario("payment.duplicate", route ->
            route.fulfill(new Route.FulfillOptions()
                .setStatus(409)
                .setContentType("application/json")
                .setBody("{\"error\":\"DUPLICATE_TRANSACTION\",\"existingTxnId\":\"TXN-99999\"}")
            )
        );

        addScenario("payment.validation_error", route ->
            route.fulfill(new Route.FulfillOptions()
                .setStatus(422)
                .setContentType("application/json")
                .setBody("{\"error\":\"VALIDATION_FAILED\",\"fields\":[{\"field\":\"amount\",\"message\":\"Must be positive\"}]}")
            )
        );

        addScenario("payment.service_down", route ->
            route.fulfill(new Route.FulfillOptions()
                .setStatus(503)
                .setHeader("Retry-After", "30")
                .setContentType("application/json")
                .setBody("{\"error\":\"SERVICE_UNAVAILABLE\"}")
            )
        );
    }

    public ScenarioSimulator addScenario(String name, RouteHandler handler) {
        scenarios.put(name, handler);
        return this;
    }

    // Activate a scenario for a specific URL pattern
    public void activate(String scenarioName, String urlPattern) {
        RouteHandler handler = scenarios.get(scenarioName);
        if (handler == null) throw new RuntimeException("Unknown scenario: " + scenarioName);

        page.route(urlPattern, handler::handle);
        LoggerFactory.getLogger(ScenarioSimulator.class)
            .info("[SCENARIO] Activated '{}' for '{}'", scenarioName, urlPattern);
    }

    // Remove all active route overrides
    public void reset() {
        page.unrouteAll();
    }

    // Activate scenario only for the next N requests
    public void activateOnce(String scenarioName, String urlPattern) {
        RouteHandler handler = scenarios.get(scenarioName);
        if (handler == null) throw new RuntimeException("Unknown scenario: " + scenarioName);

        page.routeOnce(urlPattern, handler::handle);
    }
}

// Usage
ScenarioSimulator simulator = new ScenarioSimulator(page);

// Test insufficient funds scenario
simulator.activate("payment.insufficient_funds", "**/api/v1/payments");
page.locator("#pay-now-btn").click();
assertThat(page.locator(".payment-error"))
    .hasText(Pattern.compile(".*insufficient funds.*", Pattern.CASE_INSENSITIVE));
assertThat(page.locator(".available-balance")).isVisible();
simulator.reset();

// Test service down scenario
simulator.activate("payment.service_down", "**/api/v1/payments");
page.locator("#pay-now-btn").click();
assertThat(page.locator(".retry-banner")).isVisible();
assertThat(page.locator("#pay-now-btn")).isDisabled(); // Retry-After respected
simulator.reset();
```

---

## Coding Q23: Build a Cursor-Based Pagination Handler

**Problem:** Financial transactions API uses cursor-based pagination (not page numbers). Must collect all records across pages using `next_cursor` tokens, with a record limit cap.

**Difficulty:** Hard

**Task:** Build a `CursorPaginator` that handles cursor-based API pagination, collecting all records up to a configurable limit.

**Solution:**
```java
public class CursorPaginator {

    private final APIRequestContext apiContext;
    private static final Logger log = LoggerFactory.getLogger(CursorPaginator.class);
    private static final int DEFAULT_MAX_RECORDS = 10_000;

    public CursorPaginator(APIRequestContext apiContext) {
        this.apiContext = apiContext;
    }

    // Collect all records from a cursor-paginated endpoint
    public <T> List<T> collectAll(String endpoint, Class<T> itemType,
                                   String itemsKey, String cursorKey) {
        return collectAll(endpoint, itemType, itemsKey, cursorKey,
            DEFAULT_MAX_RECORDS, Collections.emptyMap());
    }

    public <T> List<T> collectAll(String endpoint, Class<T> itemType,
                                   String itemsKey, String cursorKey,
                                   int maxRecords, Map<String, String> baseParams) {
        List<T> allItems = new ArrayList<>();
        String cursor = null;
        int page = 0;
        ObjectMapper mapper = new ObjectMapper();

        do {
            Map<String, String> params = new HashMap<>(baseParams);
            if (cursor != null) params.put("cursor", cursor);

            String url = buildUrl(endpoint, params);
            log.info("[CURSOR] Page {} | {}", ++page, url);

            APIResponse response = apiContext.get(url);
            if (!response.ok()) {
                throw new RuntimeException("Pagination failed at page " + page +
                    ": " + response.status());
            }

            try {
                Map<String, Object> body = mapper.readValue(response.text(),
                    new TypeReference<>() {});

                @SuppressWarnings("unchecked")
                List<Object> rawItems = (List<Object>) body.get(itemsKey);
                if (rawItems == null || rawItems.isEmpty()) break;

                for (Object rawItem : rawItems) {
                    allItems.add(mapper.convertValue(rawItem, itemType));
                    if (allItems.size() >= maxRecords) {
                        log.warn("[CURSOR] Reached max record limit: {}", maxRecords);
                        return allItems;
                    }
                }

                // Extract next cursor
                cursor = (String) body.get(cursorKey);
                log.debug("[CURSOR] Next cursor: {}", cursor != null ? "present" : "null (last page)");

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse page " + page, e);
            }

        } while (cursor != null);

        log.info("[CURSOR] Collected {} records across {} pages", allItems.size(), page);
        return allItems;
    }

    private String buildUrl(String endpoint, Map<String, String> params) {
        if (params.isEmpty()) return endpoint;
        String query = params.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                      URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        return endpoint + (endpoint.contains("?") ? "&" : "?") + query;
    }
}

// Usage
CursorPaginator paginator = new CursorPaginator(page.request());

List<Transaction> transactions = paginator.collectAll(
    "https://api.example.com/v1/transactions",
    Transaction.class,
    "data",        // JSON key containing the items array
    "next_cursor", // JSON key containing the next cursor
    5000,          // max records
    Map.of("account_id", "ACC-001", "from_date", "2024-01-01", "limit", "100")
);

assertThat(transactions).hasSizeGreaterThan(0);
assertThat(transactions).allMatch(t -> t.getAccountId().equals("ACC-001"));
assertThat(transactions.stream().mapToDouble(Transaction::getAmount).sum())
    .isGreaterThan(0);
```

---

## Coding Q24: Build a Component Object Model (COM) for Reusable UI Components

**Problem:** A `DataTable` component appears on 15 different pages with identical functionality (sort, filter, paginate, export). Duplicating locators across 15 Page Objects creates maintenance debt.

**Difficulty:** Medium

**Task:** Build a `DataTableComponent` Component Object that encapsulates all DataTable functionality and is composable into any Page Object via constructor injection.

**Solution:**
```java
// Base component interface
public interface UIComponent {
    boolean isVisible();
    void waitForVisible();
}

// DataTable Component Object
public class DataTableComponent implements UIComponent {

    private final Locator root;
    private final Page page;

    public DataTableComponent(Page page, Locator root) {
        this.page = page;
        this.root = root;
    }

    // Convenience factory — by testid
    public static DataTableComponent byTestId(Page page, String testId) {
        return new DataTableComponent(page, page.getByTestId(testId));
    }

    @Override
    public boolean isVisible() { return root.isVisible(); }

    @Override
    public void waitForVisible() {
        root.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public int getRowCount() {
        return root.locator("tbody tr").count();
    }

    public List<String> getHeaders() {
        return root.locator("thead th").allTextContents()
            .stream().map(String::trim).collect(Collectors.toList());
    }

    public void sortBy(String columnName, boolean ascending) {
        Locator header = root.locator("thead th").filter(
            new Locator.FilterOptions().setHasText(columnName));
        header.click(); // First click = ascending
        if (!ascending) header.click(); // Second = descending
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void filterBy(String columnName, String value) {
        root.locator(".column-filter[data-column='" + columnName + "']").fill(value);
        page.waitForResponse(resp -> resp.url().contains("/api/") && resp.status() == 200);
    }

    public void clearFilters() {
        root.locator("[data-action='clear-filters']").click();
    }

    public DataTableComponent.Row findRow(String columnName, String value) {
        List<String> headers = getHeaders();
        int colIdx = headers.indexOf(columnName);
        if (colIdx == -1) throw new RuntimeException("Column not found: " + columnName);

        List<Locator> rows = root.locator("tbody tr").all();
        for (Locator row : rows) {
            if (value.equals(row.locator("td").nth(colIdx).textContent().trim())) {
                return new Row(row, headers);
            }
        }
        throw new RuntimeException("Row not found where " + columnName + "='" + value + "'");
    }

    public void export(String format) {
        root.locator("[data-action='export']").click();
        page.locator("[data-export-format='" + format.toLowerCase() + "']").click();
    }

    // Inner Row class for fluent row interactions
    public static class Row {
        private final Locator rowLocator;
        private final List<String> headers;

        public Row(Locator rowLocator, List<String> headers) {
            this.rowLocator = rowLocator;
            this.headers = headers;
        }

        public String getCellValue(String columnName) {
            int idx = headers.indexOf(columnName);
            if (idx == -1) throw new RuntimeException("Column not found: " + columnName);
            return rowLocator.locator("td").nth(idx).textContent().trim();
        }

        public void clickAction(String actionLabel) {
            rowLocator.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
                .setName(actionLabel)).click();
        }

        public Locator getLocator() { return rowLocator; }
    }
}

// Page Object composing multiple components
public class OrdersPage extends BasePage {

    private final DataTableComponent ordersTable;
    private final DataTableComponent auditLogTable;
    private final Locator exportButton;

    public OrdersPage(Page page) {
        super(page);
        this.ordersTable = DataTableComponent.byTestId(page, "orders-table");
        this.auditLogTable = DataTableComponent.byTestId(page, "audit-log-table");
        this.exportButton = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Export"));
    }

    @Override
    protected String getPath() { return "/orders"; }

    public DataTableComponent getOrdersTable() { return ordersTable; }
    public DataTableComponent getAuditLogTable() { return auditLogTable; }
}

// Usage — clean, composable
OrdersPage ordersPage = new OrdersPage(page).navigate();
ordersPage.getOrdersTable().sortBy("Date", false); // sort descending
ordersPage.getOrdersTable().filterBy("Status", "CONFIRMED");

DataTableComponent.Row order = ordersPage.getOrdersTable()
    .findRow("Order ID", "ORD-00123");
assertThat(order.getCellValue("Status")).isEqualTo("CONFIRMED");
assertThat(Double.parseDouble(order.getCellValue("Amount").replace("$", "")))
    .isGreaterThan(100.0);
order.clickAction("View Details");
```

---

## Coding Q25: Build an End-to-End Scenario Runner

**Problem:** Business scenario "Customer Places Order and Receives Confirmation" spans: API setup → UI checkout → API validation → Email verification → PDF download. Needs clean orchestration, step logging, and rollback on failure.

**Difficulty:** Hard

**Task:** Build a `ScenarioRunner` that executes named steps with lifecycle hooks, step-level screenshots, API + UI interleaving, and automatic cleanup.

**Solution:**
```java
public class ScenarioRunner {

    private final Page page;
    private final ApiClient api;
    private final Map<String, Object> context = new LinkedHashMap<>();
    private final List<Runnable> cleanupTasks = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(ScenarioRunner.class);

    public record Step(String name, CheckedRunnable action) {}

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }

    public ScenarioRunner(Page page, ApiClient api) {
        this.page = page;
        this.api = api;
    }

    // Store value in scenario context (shared across steps)
    public ScenarioRunner put(String key, Object value) {
        context.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) context.get(key);
    }

    // Register cleanup task (runs in reverse order on completion or failure)
    public ScenarioRunner onCleanup(Runnable task) {
        cleanupTasks.add(task);
        return this;
    }

    // Execute a sequence of steps
    public void run(Step... steps) {
        try {
            for (Step step : steps) {
                executeStep(step);
            }
        } catch (Exception e) {
            log.error("[SCENARIO] Scenario failed at step. Running cleanup.");
            throw new RuntimeException("Scenario execution failed", e);
        } finally {
            runCleanup();
        }
    }

    private void executeStep(Step step) {
        log.info("[STEP] ▶ {}", step.name());
        long start = System.currentTimeMillis();
        try {
            step.action().run();
            long duration = System.currentTimeMillis() - start;
            log.info("[STEP] ✓ {} ({}ms)", step.name(), duration);
            Allure.step(step.name());
        } catch (Exception e) {
            captureFailureEvidence(step.name());
            throw new RuntimeException("[STEP FAILED] " + step.name() + ": " + e.getMessage(), e);
        }
    }

    private void captureFailureEvidence(String stepName) {
        try {
            byte[] screenshot = page.screenshot();
            Allure.addAttachment("Failure at: " + stepName, "image/png",
                new ByteArrayInputStream(screenshot), "png");
        } catch (Exception e) {
            log.warn("Could not capture failure screenshot: {}", e.getMessage());
        }
    }

    private void runCleanup() {
        List<Runnable> reversed = new ArrayList<>(cleanupTasks);
        Collections.reverse(reversed);
        for (Runnable task : reversed) {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("[CLEANUP] Task failed: {}", e.getMessage());
            }
        }
    }
}

// Usage — full E2E scenario
@Test
@Story("Customer Places Order and Receives Confirmation Email")
public void testOrderPlacementEndToEnd() {
    ScenarioRunner scenario = new ScenarioRunner(page, api);

    scenario.run(
        // Step 1: Create customer via API
        new ScenarioRunner.Step("Create test customer", () -> {
            String customerId = api.post("/v1/customers",
                CustomerFactory.aPremiumCustomer().withName("E2E Test Corp").build(),
                Map.class).get("id").toString();
            scenario.put("customerId", customerId);
            scenario.onCleanup(() -> api.delete("/v1/customers/" + customerId));
        }),

        // Step 2: Navigate to product catalog in UI
        new ScenarioRunner.Step("Navigate to product catalog", () -> {
            page.navigate(config.getBaseUrl() + "/catalog");
            assertThat(page.locator("h1")).hasText("Product Catalog");
        }),

        // Step 3: Add product to cart via UI
        new ScenarioRunner.Step("Add product to cart", () -> {
            page.getByTestId("product-SKU-PREMIUM-001").locator(".add-to-cart").click();
            assertThat(page.locator(".cart-count")).hasText("1");
        }),

        // Step 4: Checkout with pre-created customer
        new ScenarioRunner.Step("Complete checkout", () -> {
            page.navigate(config.getBaseUrl() + "/checkout");
            page.getByLabel("Customer ID").fill(scenario.get("customerId"));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName("Place Order")).click();
            page.waitForURL("**/order-confirmed/**");
            String orderId = page.locator(".order-id").textContent();
            scenario.put("orderId", orderId);
            scenario.onCleanup(() -> api.delete("/v1/orders/" + orderId));
        }),

        // Step 5: Validate order in API
        new ScenarioRunner.Step("Validate order via API", () -> {
            String orderId = scenario.get("orderId");
            Map<String, Object> order = api.get("/v1/orders/" + orderId, Map.class);
            assertThat(order.get("status")).isEqualTo("CONFIRMED");
            assertThat(order.get("customerId")).isEqualTo(scenario.get("customerId"));
        }),

        // Step 6: Download and validate order PDF
        new ScenarioRunner.Step("Download order confirmation PDF", () -> {
            DownloadValidator dv = new DownloadValidator();
            dv.capture(page, () -> page.locator("[data-action='download-pdf']").click())
              .assertFileName("order-.*\\.pdf")
              .assertIsPdf()
              .assertFileSizeBetween(10_000, 5_000_000);
        })
    );
}
```

---

## Coding Q26: Build a Multipart / Form-Data Request Utility

**Problem:** Document management API accepts multipart uploads — a JSON metadata part and a binary file part in a single `multipart/form-data` request. Playwright's `APIRequestContext` must send both.

**Difficulty:** Medium

**Task:** Build a `MultipartRequestBuilder` that constructs and sends multipart API requests with mixed JSON + binary content.

**Solution:**
```java
public class MultipartRequestBuilder {

    private final APIRequestContext context;
    private final Map<String, Object> formData = new LinkedHashMap<>();

    public MultipartRequestBuilder(APIRequestContext context) {
        this.context = context;
    }

    public MultipartRequestBuilder addField(String name, String value) {
        formData.put(name, value);
        return this;
    }

    public MultipartRequestBuilder addJsonField(String name, Object jsonObject) {
        try {
            String json = new ObjectMapper().writeValueAsString(jsonObject);
            formData.put(name, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON field: " + name, e);
        }
        return this;
    }

    public MultipartRequestBuilder addFile(String fieldName, Path filePath, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            formData.put(fieldName, new FilePayload(
                filePath.getFileName().toString(), mimeType, bytes));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file: " + filePath, e);
        }
        return this;
    }

    public MultipartRequestBuilder addFile(String fieldName, String filename,
                                            String mimeType, byte[] content) {
        formData.put(fieldName, new FilePayload(filename, mimeType, content));
        return this;
    }

    public APIResponse post(String url) {
        return context.post(url, RequestOptions.create().setMultipart(formData));
    }

    public APIResponse put(String url) {
        return context.put(url, RequestOptions.create().setMultipart(formData));
    }
}

// Usage
MultipartRequestBuilder builder = new MultipartRequestBuilder(page.request());

Map<String, Object> metadata = Map.of(
    "documentType", "INVOICE",
    "claimId", "CLM-00123",
    "description", "Q4 2024 Invoice",
    "tags", List.of("finance", "invoice", "q4")
);

APIResponse response = builder
    .addJsonField("metadata", metadata)
    .addFile("document", Paths.get("src/test/resources/invoice.pdf"), "application/pdf")
    .addFile("thumbnail", "thumb.jpg", "image/jpeg",
        UploadUtils.generatePdf("thumb", 10).buffer())
    .addField("uploadedBy", "test-automation")
    .post("https://api.example.com/v1/documents");

assertThat(response.status()).isEqualTo(201);
Map<String, Object> result = (Map<String, Object>) response.json();
assertThat(result.get("documentId")).isNotNull();
assertThat(result.get("status")).isEqualTo("PROCESSING");
```

---

## Coding Q27: Build a WebSocket Message Validator

**Problem:** A trading platform uses WebSockets to push real-time price updates to the browser. Tests need to verify the application correctly processes incoming WS messages and updates the UI.

**Difficulty:** Hard

**Task:** Build a `WebSocketMonitor` that captures all WebSocket messages, validates message structure, and waits for specific messages by predicate.

**Solution:**
```java
public class WebSocketMonitor {

    private final List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());
    private final List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, WebSocket> activeConnections = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebSocketMonitor.class);

    public WebSocketMonitor(Page page) {
        page.onWebSocket(ws -> {
            log.info("[WS] Connected: {}", ws.url());
            activeConnections.put(ws.url(), ws);

            ws.onFrameReceived(frame -> {
                String data = frame.text();
                receivedMessages.add(data);
                log.debug("[WS] Received: {}", data);
            });

            ws.onFrameSent(frame -> {
                String data = frame.text();
                sentMessages.add(data);
                log.debug("[WS] Sent: {}", data);
            });

            ws.onClose(wsInstance -> {
                log.info("[WS] Closed: {}", wsInstance.url());
                activeConnections.remove(wsInstance.url());
            });
        });
    }

    // Wait for a message matching a predicate (with timeout)
    public String waitForMessage(Predicate<String> predicate, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            synchronized (receivedMessages) {
                Optional<String> match = receivedMessages.stream()
                    .filter(predicate).findFirst();
                if (match.isPresent()) return match.get();
            }
            try { Thread.sleep(100); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        throw new RuntimeException("WebSocket message not received within " + timeoutMs + "ms");
    }

    // Wait for JSON message containing specific key-value
    public Map<String, Object> waitForJsonMessage(String key, Object expectedValue, int timeoutMs) {
        ObjectMapper mapper = new ObjectMapper();
        String raw = waitForMessage(msg -> {
            try {
                Map<String, Object> parsed = mapper.readValue(msg, new TypeReference<>() {});
                Object actual = parsed.get(key);
                return expectedValue.equals(actual);
            } catch (Exception e) { return false; }
        }, timeoutMs);

        try {
            return mapper.readValue(raw, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse matched WS message", e);
        }
    }

    public List<String> getAllReceived() { return Collections.unmodifiableList(receivedMessages); }
    public List<String> getAllSent() { return Collections.unmodifiableList(sentMessages); }
    public void clear() { receivedMessages.clear(); sentMessages.clear(); }

    public int getConnectionCount() { return activeConnections.size(); }

    // Assert a message was (or was not) received
    public void assertMessageReceived(Predicate<String> predicate) {
        assertThat(receivedMessages.stream().anyMatch(predicate))
            .as("Expected WebSocket message was not received").isTrue();
    }

    public void assertNoErrorMessages() {
        List<String> errors = receivedMessages.stream()
            .filter(m -> m.contains("\"type\":\"error\"") || m.contains("\"error\":"))
            .collect(Collectors.toList());
        assertThat(errors).as("Unexpected WS error messages: " + errors).isEmpty();
    }
}

// Usage
WebSocketMonitor wsMonitor = new WebSocketMonitor(page);
page.navigate(config.getBaseUrl() + "/trading/AAPL");
page.waitForLoadState(LoadState.NETWORKIDLE);

// Wait for price update for AAPL
Map<String, Object> priceMsg = wsMonitor.waitForJsonMessage("symbol", "AAPL", 10_000);
assertThat(priceMsg.get("price")).isNotNull();
assertThat((Double) priceMsg.get("price")).isGreaterThan(0.0);

// Verify UI reflects the WS update
Double wsPrice = (Double) priceMsg.get("price");
String uiPrice = page.locator("[data-symbol='AAPL'] .live-price").textContent()
    .replaceAll("[^0-9.]", "");
assertThat(Double.parseDouble(uiPrice)).isCloseTo(wsPrice, Offset.offset(0.01));

wsMonitor.assertNoErrorMessages();
```

---

## Coding Q28: Build a Session State Validator

**Problem:** After login, the application must set specific localStorage keys, cookies with correct flags, and sessionStorage settings. Security tests must validate these are correctly configured.

**Difficulty:** Medium

**Task:** Build a `SessionStateValidator` that validates browser storage and cookie security posture after authentication.

**Solution:**
```java
public class SessionStateValidator {

    private final Page page;
    private final BrowserContext context;

    public SessionStateValidator(Page page, BrowserContext context) {
        this.page = page;
        this.context = context;
    }

    // Validate localStorage
    public Map<String, String> getLocalStorage() {
        @SuppressWarnings("unchecked")
        Map<String, String> storage = (Map<String, String>) page.evaluate("""
            () => {
                const result = {};
                for (let i = 0; i < localStorage.length; i++) {
                    const key = localStorage.key(i);
                    result[key] = localStorage.getItem(key);
                }
                return result;
            }
        """);
        return storage;
    }

    public SessionStateValidator assertLocalStorageContains(String key) {
        assertThat(getLocalStorage()).containsKey(key);
        return this;
    }

    public SessionStateValidator assertLocalStorageKeyAbsent(String key) {
        assertThat(getLocalStorage()).doesNotContainKey(key);
        return this;
    }

    // Validate cookies
    public Optional<Cookie> getCookie(String name) {
        return context.cookies().stream()
            .filter(c -> name.equals(c.name))
            .findFirst();
    }

    public SessionStateValidator assertCookieExists(String name) {
        assertThat(getCookie(name)).as("Cookie '" + name + "' should exist").isPresent();
        return this;
    }

    public SessionStateValidator assertCookieIsHttpOnly(String name) {
        Cookie cookie = getCookie(name)
            .orElseThrow(() -> new AssertionError("Cookie not found: " + name));
        assertThat(cookie.httpOnly).as("Cookie '" + name + "' should be HttpOnly").isTrue();
        return this;
    }

    public SessionStateValidator assertCookieIsSecure(String name) {
        Cookie cookie = getCookie(name)
            .orElseThrow(() -> new AssertionError("Cookie not found: " + name));
        assertThat(cookie.secure).as("Cookie '" + name + "' should be Secure").isTrue();
        return this;
    }

    public SessionStateValidator assertCookieSameSite(String name, String sameSite) {
        Cookie cookie = getCookie(name)
            .orElseThrow(() -> new AssertionError("Cookie not found: " + name));
        assertThat(cookie.sameSite.name()).isEqualToIgnoringCase(sameSite);
        return this;
    }

    // Validate no sensitive data in localStorage
    public SessionStateValidator assertNoPlaintextPasswords() {
        Map<String, String> storage = getLocalStorage();
        storage.forEach((key, value) -> {
            assertThat(value.toLowerCase())
                .as("localStorage key '" + key + "' should not contain plaintext password")
                .doesNotContainPattern(Pattern.compile("password|passwd|pwd", Pattern.CASE_INSENSITIVE));
        });
        return this;
    }

    // Run full security audit
    public void assertSecurityPosture(String sessionCookieName) {
        assertCookieExists(sessionCookieName)
        .assertCookieIsHttpOnly(sessionCookieName)
        .assertCookieIsSecure(sessionCookieName)
        .assertCookieSameSite(sessionCookieName, "STRICT")
        .assertNoPlaintextPasswords()
        .assertLocalStorageKeyAbsent("password")
        .assertLocalStorageKeyAbsent("rawToken");
    }
}

// Usage — security validation after login
new LoginPage(page).navigate().loginAs("user@example.com", password);
SessionStateValidator validator = new SessionStateValidator(page, context);

// Full security posture check
validator.assertSecurityPosture("session_id");

// Specific assertions
validator
    .assertLocalStorageContains("user_preferences")
    .assertLocalStorageContains("feature_flags")
    .assertCookieExists("csrf_token");
```

---

## Coding Q29: Build a Performance Timing Collector

**Problem:** Non-functional requirement: key user journeys must complete within defined SLAs. "Login to Dashboard" < 3s, "Place Order" < 5s. Need automated performance measurement in tests.

**Difficulty:** Medium

**Task:** Build a `PerformanceCollector` that measures Navigation Timing API metrics and custom action durations, comparing against SLA thresholds.

**Solution:**
```java
public class PerformanceCollector {

    private final Page page;
    private final Map<String, Long> actionTimings = new LinkedHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(PerformanceCollector.class);

    public PerformanceCollector(Page page) { this.page = page; }

    // Measure browser Navigation Timing for current page
    @SuppressWarnings("unchecked")
    public NavigationMetrics getNavigationMetrics() {
        Map<String, Object> timing = (Map<String, Object>) page.evaluate("""
            () => {
                const t = performance.getEntriesByType('navigation')[0];
                return {
                    domContentLoaded: Math.round(t.domContentLoadedEventEnd - t.startTime),
                    load: Math.round(t.loadEventEnd - t.startTime),
                    firstByte: Math.round(t.responseStart - t.requestStart),
                    domInteractive: Math.round(t.domInteractive - t.startTime),
                    transferSize: t.transferSize
                };
            }
        """);
        return new NavigationMetrics(
            ((Number) timing.get("domContentLoaded")).longValue(),
            ((Number) timing.get("load")).longValue(),
            ((Number) timing.get("firstByte")).longValue(),
            ((Number) timing.get("domInteractive")).longValue(),
            ((Number) timing.get("transferSize")).longValue()
        );
    }

    // Measure a custom action duration
    public <T> T measure(String actionName, Supplier<T> action) {
        long start = System.currentTimeMillis();
        T result = action.get();
        long duration = System.currentTimeMillis() - start;
        actionTimings.put(actionName, duration);
        log.info("[PERF] '{}' took {}ms", actionName, duration);
        return result;
    }

    public void measure(String actionName, Runnable action) {
        measure(actionName, () -> { action.run(); return null; });
    }

    // Assert action was within SLA
    public void assertWithinSla(String actionName, long slaMs) {
        Long actual = actionTimings.get(actionName);
        assertThat(actual).as("'" + actionName + "' SLA: " + slaMs + "ms, actual: " + actual + "ms")
            .isLessThanOrEqualTo(slaMs);
    }

    // Assert all actions within SLA from a map
    public void assertAllWithinSla(Map<String, Long> slaDefs) {
        slaDefs.forEach(this::assertWithinSla);
    }

    public Map<String, Long> getAllTimings() { return Collections.unmodifiableMap(actionTimings); }

    public record NavigationMetrics(
        long domContentLoadedMs, long loadMs, long firstByteMs,
        long domInteractiveMs, long transferSizeBytes
    ) {
        public void assertWithinSla(long maxLoadMs) {
            assertThat(loadMs).as("Page load SLA exceeded: " + loadMs + "ms > " + maxLoadMs + "ms")
                .isLessThanOrEqualTo(maxLoadMs);
        }
    }
}

// Usage
PerformanceCollector perf = new PerformanceCollector(page);

// Measure login flow
perf.measure("Login to Dashboard", () -> {
    new LoginPage(page).navigate()
        .loginAs("user@example.com", password);
    page.waitForURL("**/dashboard");
});

// Measure order placement
perf.measure("Add to Cart", () ->
    page.getByTestId("product-001").locator(".add-to-cart").click());

perf.measure("Checkout and Confirm", () -> {
    page.navigate(config.getBaseUrl() + "/checkout");
    new CheckoutPage(page).completeOrder(paymentDetails);
    page.waitForURL("**/order-confirmed/**");
});

// Check navigation metrics
perf.getNavigationMetrics().assertWithinSla(2000); // page load < 2s

// Assert all SLAs
perf.assertAllWithinSla(Map.of(
    "Login to Dashboard",     3000L,  // < 3s
    "Add to Cart",             500L,  // < 0.5s
    "Checkout and Confirm",   5000L   // < 5s
));

// Log performance report
perf.getAllTimings().forEach((action, ms) ->
    System.out.printf("%-40s %5dms%n", action, ms));
```

---

## Coding Q30: Build an End-to-End API + UI Data Consistency Validator

**Problem:** After creating an order via UI, the same data must appear consistently in: the UI order details page, the REST API response, the audit log API, and a PDF confirmation download.

**Difficulty:** Hard

**Task:** Build a `DataConsistencyValidator` that fetches the same entity from multiple sources and cross-validates all fields match.

**Solution:**
```java
public class DataConsistencyValidator {

    private final Page page;
    private final ApiClient api;
    private static final Logger log = LoggerFactory.getLogger(DataConsistencyValidator.class);

    public record DataSource(String name, Supplier<Map<String, Object>> fetcher) {}

    public DataConsistencyValidator(Page page, ApiClient api) {
        this.page = page;
        this.api = api;
    }

    // Compare a set of fields across multiple data sources
    public void assertConsistent(List<DataSource> sources, List<String> fieldsToCompare) {
        Map<String, Map<String, Object>> allData = new LinkedHashMap<>();

        for (DataSource source : sources) {
            log.info("[CONSISTENCY] Fetching from: {}", source.name());
            Map<String, Object> data = source.fetcher().get();
            allData.put(source.name(), data);
        }

        List<String> violations = new ArrayList<>();
        String referenceSourceName = sources.get(0).name();
        Map<String, Object> referenceData = allData.get(referenceSourceName);

        for (String field : fieldsToCompare) {
            Object referenceValue = referenceData.get(field);
            for (Map.Entry<String, Map<String, Object>> entry : allData.entrySet()) {
                if (entry.getKey().equals(referenceSourceName)) continue;
                Object actualValue = entry.getValue().get(field);
                if (!Objects.equals(referenceValue, actualValue)) {
                    violations.add(String.format(
                        "Field '%s': %s='%s' vs %s='%s'",
                        field, referenceSourceName, referenceValue,
                        entry.getKey(), actualValue
                    ));
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("Data consistency violations:\n" +
                violations.stream().map(v -> "  - " + v).collect(Collectors.joining("\n")));
        }
        log.info("[CONSISTENCY] All {} fields consistent across {} sources",
            fieldsToCompare.size(), sources.size());
    }

    // Extract order data from UI page
    public Map<String, Object> extractFromUi(String orderId) {
        page.navigate(config.getBaseUrl() + "/orders/" + orderId);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        return Map.of(
            "orderId",    page.locator("[data-field='order-id']").textContent().trim(),
            "status",     page.locator("[data-field='status']").textContent().trim(),
            "amount",     page.locator("[data-field='amount']").textContent()
                .replaceAll("[^0-9.]", "").trim(),
            "customerId", page.locator("[data-field='customer-id']").textContent().trim()
        );
    }

    // Extract order data from REST API
    public Map<String, Object> extractFromApi(String orderId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> order = api.get("/v1/orders/" + orderId, Map.class);
        return Map.of(
            "orderId",    order.get("id").toString(),
            "status",     order.get("status").toString(),
            "amount",     order.get("amount").toString(),
            "customerId", order.get("customerId").toString()
        );
    }

    private ConfigManager config = ConfigManager.getInstance();
}

// Usage
@Test
@Story("Order data is consistent across all views")
public void testOrderDataConsistency() {
    // Create order via API
    String orderId = createTestOrder();

    DataConsistencyValidator validator = new DataConsistencyValidator(page, api);

    validator.assertConsistent(
        List.of(
            new DataConsistencyValidator.DataSource("UI Order Page",
                () -> validator.extractFromUi(orderId)),

            new DataConsistencyValidator.DataSource("REST API",
                () -> validator.extractFromApi(orderId)),

            new DataConsistencyValidator.DataSource("Audit Log API",
                () -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> latest = api.get(
                        "/v1/audit-log?entityId=" + orderId + "&limit=1", Map.class);
                    return Map.of(
                        "orderId",    latest.get("entityId").toString(),
                        "status",     latest.get("newStatus").toString(),
                        "customerId", latest.get("actorId").toString(),
                        "amount",     latest.getOrDefault("metadata.amount", "").toString()
                    );
                }
            )
        ),
        List.of("orderId", "status", "customerId") // fields to cross-validate
    );
}
```

---

*— End of Coding Q16–Q30 | Section 2: Coding Questions Complete (30/30) —*

---

# SECTION 3 — FRAMEWORK / ARCHITECTURE / REAL-WORLD DESIGN QUESTIONS (20)

---

## Architecture Q1: Design an Enterprise-Grade Playwright Java Framework from Scratch

**Question:** You are the lead SDET architect at a FinTech company. The QA team is 12 engineers, 3 product lines, 800+ tests, 5 target environments. Design the complete framework architecture.

**Answer:**

**Non-Functional Requirements (NFRs) to address first:**
- Parallel execution: 500+ tests in < 15 minutes
- Zero shared mutable state between tests
- Environment-agnostic (dev/staging/uat/perf/prod-ro)
- Pluggable browser strategy (local → Docker → cloud)
- Allure reporting with screenshots, traces, and API logs
- Auth state pre-warmed (login not repeated each test)
- Works in GitHub Actions CI and developer laptops equally

**Package Structure:**
```
automation-framework/
├── pom.xml                        # Parent POM — all dependency/plugin management
├── framework-core/                # Shared module: no test deps, only infra
│   └── src/main/java/
│       └── com/company/framework/
│           ├── browser/           # BrowserFactory, BrowserContextFactory
│           ├── config/            # ConfigManager, EnvironmentConfig, SecretResolver
│           ├── auth/              # LoginManager, TokenManager, AuthStateCache
│           ├── api/               # ApiClient, MultipartBuilder, ContractValidator
│           ├── reporting/         # AllureHelper, TraceManager, ScreenshotAttacher
│           ├── utils/             # RetryUtil, WaitUtil, DateUtil, FileUtil
│           └── base/              # BasePage, BaseTest, TestExecutionContext
├── tests-ui/                      # UI test module
│   └── src/test/java/
│       └── com/company/tests/
│           ├── pages/             # Page Objects per product line
│           │   ├── common/        # Shared components (NavBar, DataTable, Modal)
│           │   ├── orders/        # Orders page objects
│           │   ├── customers/     # Customer page objects
│           │   └── payments/      # Payment page objects
│           ├── scenarios/         # Business scenario runners
│           └── suites/            # Test classes per feature area
├── tests-api/                     # Pure API test module
│   └── src/test/java/
│       └── com/company/apitests/
├── tests-integration/             # E2E UI+API combined tests
└── test-data/                     # TestDataFactory, builders, resource files
    └── src/main/java/
        └── com/company/testdata/
            ├── builders/          # Fluent builders per domain entity
            ├── factories/         # TestDataFactory, random generators
            └── cleaners/          # Post-test cleanup strategies
```

**Layer Diagram:**
```
┌─────────────────────────────────────────────────────┐
│                   Test Classes (JUnit 5)             │
│           @Epic / @Feature / @Story labels           │
├─────────────────────────────────────────────────────┤
│              Page Objects / Component Objects        │
│        BasePage → ProductPageObjects → Steps         │
├─────────────────────────────────────────────────────┤
│           Framework Core (TestExecutionContext)      │
│   BrowserFactory | AuthManager | ApiClient | Config  │
├─────────────────────────────────────────────────────┤
│              Playwright Java Runtime                 │
│         Browser Process (Chromium/FF/WebKit)         │
└─────────────────────────────────────────────────────┘
```

**Key design decisions:**
| Decision | Choice | Reason |
|---|---|---|
| Thread isolation | ThreadLocal Browser+Context | Zero locking, JUnit parallel safe |
| Auth state | Pre-warmed JSON files | Skip login overhead in each test |
| Config | YAML per env + env var override | Portable for CI and local |
| Reporting | Allure + GitHub Actions artifact upload | Rich history with trace attachments |
| Page factory | Manual constructor (no `initElements`) | Explicit, IDE-navigable, no reflection |
| Assertions | AssertJ + Playwright `assertThat` | Fluent, readable, soft assertion support |

---

## Architecture Q2: Maven Multi-Module Setup for Large-Scale Playwright Projects

**Question:** How do you structure the Maven build for a framework with shared core, UI tests, API tests, and integration tests? Show the complete POM hierarchy.

**Answer:**

**Root `pom.xml` (Parent):**
```xml
<project>
  <groupId>com.company</groupId>
  <artifactId>automation-parent</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>

  <modules>
    <module>framework-core</module>
    <module>test-data</module>
    <module>tests-ui</module>
    <module>tests-api</module>
    <module>tests-integration</module>
  </modules>

  <properties>
    <java.version>17</java.version>
    <playwright.version>1.44.0</playwright.version>
    <junit.version>5.10.2</junit.version>
    <allure.version>2.27.0</allure.version>
    <maven.surefire.version>3.2.5</maven.surefire.version>
    <parallel.thread.count>4</parallel.thread.count>
    <headless>true</headless>
    <env>staging</env>
  </properties>

  <dependencyManagement>
    <dependencies>
      <!-- Playwright -->
      <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>${playwright.version}</version>
      </dependency>
      <!-- JUnit 5 BOM -->
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <!-- Allure -->
      <dependency>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-junit5</artifactId>
        <version>${allure.version}</version>
      </dependency>
      <!-- Jackson -->
      <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.0</version>
      </dependency>
      <!-- SnakeYAML -->
      <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
      </dependency>
      <!-- AssertJ -->
      <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.26.0</version>
      </dependency>
      <!-- SLF4J + Logback -->
      <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.5.6</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>${maven.surefire.version}</version>
          <configuration>
            <parallel>methods</parallel>
            <useUnlimitedThreads>false</useUnlimitedThreads>
            <threadCount>${parallel.thread.count}</threadCount>
            <forkCount>1</forkCount>
            <systemPropertyVariables>
              <headless>${headless}</headless>
              <env>${env}</env>
            </systemPropertyVariables>
            <argLine>
              -javaagent:${settings.localRepository}/org/aspectj/aspectjweaver/1.9.22/aspectjweaver-1.9.22.jar
            </argLine>
          </configuration>
        </plugin>
        <plugin>
          <groupId>io.qameta.allure</groupId>
          <artifactId>allure-maven</artifactId>
          <version>2.12.0</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>

  <!-- Profiles for selective test execution -->
  <profiles>
    <profile>
      <id>smoke</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <groups>smoke</groups>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
    <profile>
      <id>regression</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <groups>regression</groups>
              <threadCount>8</threadCount>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
    <profile>
      <id>cross-browser</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <systemPropertyVariables>
                <browsers>chromium,firefox,webkit</browsers>
              </systemPropertyVariables>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
</project>
```

**Child module `tests-ui/pom.xml`:**
```xml
<parent>
  <groupId>com.company</groupId>
  <artifactId>automation-parent</artifactId>
  <version>1.0.0</version>
</parent>
<artifactId>tests-ui</artifactId>

<dependencies>
  <dependency>
    <groupId>com.company</groupId>
    <artifactId>framework-core</artifactId>
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>com.company</groupId>
    <artifactId>test-data</artifactId>
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <scope>test</scope>
  </dependency>
  <!-- ... junit5, allure, assertj from parent dependencyManagement -->
</dependencies>
```

**Running subsets:**
```bash
# Smoke tests on staging (4 threads)
mvn test -pl tests-ui -P smoke -Denv=staging

# Full regression on UAT (8 threads, CI)
mvn test -pl tests-ui,tests-api -P regression -Denv=uat -Dheadless=true

# Cross-browser on specific feature
mvn test -pl tests-ui -P cross-browser -Dgroups=payments -Denv=staging

# Single module, skip others
mvn test -pl tests-api -Denv=dev -DthreadCount=2
```

---

## Architecture Q3: Browser Lifecycle Management Strategy

**Question:** Explain the three levels of browser resource scoping in Playwright Java — what should be shared vs. isolated, and why? How does this impact parallel execution?

**Answer:**

**Three Levels:**

| Resource | Scope | Cost | Isolation |
|---|---|---|---|
| `Playwright` | Per thread (ThreadLocal) | High (JVM process) | Required per thread |
| `Browser` | Per thread (ThreadLocal) | Medium (browser process) | Per thread for performance |
| `BrowserContext` | Per test (`@BeforeEach`) | Low (incognito-like) | Full auth/cookie/storage isolation |
| `Page` | Per test action | Negligible | Tabs within context |

**Why `BrowserContext` is the key isolation unit:**
- Each `BrowserContext` is a full incognito profile: separate cookies, localStorage, sessionStorage, auth state
- Two tests sharing a `BrowserContext` = flaky cross-contamination (auth bleed, cookie conflicts)
- Creating a new context per test costs ~10ms vs. ~2000ms for a new browser

**Why `Browser` is shared per thread (not per test):**
- Browser process launch is expensive (~1–2s, forks OS process)
- Browsers are thread-safe for context creation
- Sharing browser across tests in same thread = ~10x throughput gain

**Why `Playwright` is per-thread (not static singleton):**
- `Playwright.create()` is NOT thread-safe
- CDP connection and DevTools protocol handling is per-thread
- Must not share across JUnit parallel threads

**Diagram:**
```
JUnit Worker Thread 1                JUnit Worker Thread 2
├── Playwright (ThreadLocal)         ├── Playwright (ThreadLocal)
│   └── Browser (ThreadLocal)        │   └── Browser (ThreadLocal)
│       ├── Context (Test A)          │       ├── Context (Test C)
│       │   └── Page                  │       │   └── Page
│       └── Context (Test B)          │       └── Context (Test D)
│           └── Page                  │           └── Page
```

**Lifecycle events:**
```java
// @BeforeAll (once per thread)       → Playwright.create(), browser.launch()
// @BeforeEach (each test)            → browser.newContext(), context.newPage()
// @AfterEach (each test)             → context.close() [saves trace on failure]
// @AfterAll (thread shutdown hook)   → browser.close(), playwright.close()
```

**Anti-patterns to avoid:**
- `static Browser browser` — race condition in parallel execution
- `Playwright.create()` in `@BeforeEach` — 1–2s overhead per test, ~1000 tests = 30 min wasted
- Not closing context after test — memory leak, eventual OOM in long runs
- Sharing context across tests — auth state bleed causes intermittent failures

---

## Architecture Q4: Page Object Model Design Principles at Scale

**Question:** Your POM has grown to 80 Page Objects. Half have duplicated locators (navigation bar appears in 60 files). You are tasked with refactoring the POM. What principles do you apply?

**Answer:**

**Problem Analysis:**
- Locator duplication = single UI change breaks N files
- Large page object classes = violation of Single Responsibility Principle
- Inheritance chains > 3 levels = fragile base class problem

**Solution: Component Object Model + Composition over Inheritance**

**Principle 1 — Shared components extracted to `Component Objects`:**
```java
// Bad: NavBar locators duplicated in 60 Page Objects
public class OrdersPage {
    private final Locator navBarLogo = page.locator(".nav-logo");
    private final Locator navBarUser = page.locator(".nav-user");
    // ...
}

// Good: NavBarComponent used by composition
public class OrdersPage extends BasePage {
    private final NavBarComponent navBar;
    private final DataTableComponent ordersTable;

    public OrdersPage(Page page) {
        super(page);
        this.navBar = new NavBarComponent(page);
        this.ordersTable = DataTableComponent.byTestId(page, "orders-table");
    }
}
```

**Principle 2 — BasePage handles only lifecycle, not business logic:**
```java
public abstract class BasePage {
    protected final Page page;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected abstract String getPath();

    public <T extends BasePage> T navigate() {
        page.navigate(ConfigManager.getInstance().getBaseUrl() + getPath());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        waitForPageReady();
        return cast(this);
    }

    protected void waitForPageReady() {} // override per page

    @SuppressWarnings("unchecked")
    private <T> T cast(Object o) { return (T) o; }
}
```

**Principle 3 — Page Objects return `this` or target Page Object (fluent/chained navigation):**
```java
public class LoginPage extends BasePage {
    public DashboardPage loginAs(String email, String password) {
        page.getByLabel("Email").fill(email);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        return new DashboardPage(page);
    }
}
// Test reads like a user story:
new LoginPage(page).navigate()
    .loginAs("admin@example.com", secret)
    .openOrders()
    .filterByStatus("CONFIRMED");
```

**Principle 4 — Locator Strategy hierarchy (resilience order):**
1. `getByTestId()` — most stable, change-proof
2. `getByRole()` + `setName()` — semantic, accessibility-friendly
3. `getByLabel()` — form fields
4. `getByText()` — buttons/links with visible text
5. CSS/XPath — last resort for legacy selectors

**Principle 5 — Page Objects own data extraction, not test classes:**
```java
// Bad: test extracts data using raw locators
String status = page.locator(".order-status-badge").textContent();

// Good: Page Object exposes typed methods
OrderStatus status = ordersPage.getOrderStatus("ORD-001");
```

**Principle 6 — No waits in test classes; all waits hidden in Page Objects:**
```java
// Bad (in test):
page.waitForSelector(".spinner", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));

// Good (in Page Object):
public OrdersPage waitForTableLoaded() {
    page.locator(".spinner").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    ordersTable.waitForVisible();
    return this;
}
```

---

## Architecture Q5: API Client Layer Architecture

**Question:** Design an API client layer that supports: typed responses, authentication header injection, request logging, retry on transient failure, environment-aware base URL, and both REST and multipart requests.

**Answer:**

**Layer Architecture:**
```
Test Code
    ↓ calls typed domain methods
DomainApiClient  (e.g., OrdersApiClient, CustomersApiClient)
    ↓ delegates to
ApiClient        (typed generic: post/get/put/delete with retry + logging)
    ↓ builds requests via
RequestBuilder   (auth header injection, base URL, JSON serialization)
    ↓ executes via
APIRequestContext (Playwright — manages connection pool, TLS, cookies)
    ↓
HTTP Response    → assertOK() → deserialize → return typed result
```

**Core `ApiClient`:**
```java
public class ApiClient {
    private final APIRequestContext context;
    private final String baseUrl;
    private final Supplier<String> tokenSupplier;
    private final RetryUtil retry;
    private final ObjectMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    public ApiClient(Playwright playwright, String baseUrl, Supplier<String> tokenSupplier) {
        this.baseUrl = baseUrl;
        this.tokenSupplier = tokenSupplier;
        this.retry = new RetryUtil(3, Duration.ofMillis(500));
        this.mapper = new ObjectMapper();
        this.context = playwright.request().newContext(
            new APIRequest.NewContextOptions()
                .setBaseURL(baseUrl)
                .setExtraHTTPHeaders(Map.of(
                    "Content-Type", "application/json",
                    "Accept", "application/json"
                ))
        );
    }

    public <T> T get(String path, Class<T> responseType) {
        return retry.execute(() -> {
            APIResponse response = context.get(path, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + tokenSupplier.get()));
            return parseResponse(response, responseType, "GET", path);
        });
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return retry.execute(() -> {
            APIResponse response = context.post(path, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + tokenSupplier.get())
                .setData(serialize(body)));
            return parseResponse(response, responseType, "POST", path);
        });
    }

    public void delete(String path) {
        retry.execute(() -> {
            APIResponse response = context.delete(path, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + tokenSupplier.get()));
            if (!response.ok()) {
                throw new RuntimeException("DELETE " + path + " failed: " + response.status());
            }
            return null;
        });
    }

    private <T> T parseResponse(APIResponse response, Class<T> type,
                                 String method, String path) {
        log.info("[API] {} {} → {}", method, path, response.status());
        if (!response.ok()) {
            throw new ApiException(method, path, response.status(), response.text());
        }
        try {
            return mapper.readValue(response.text(), type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize response from " + path, e);
        }
    }

    private String serialize(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
}

// Typed domain client
public class OrdersApiClient {
    private final ApiClient api;

    public OrdersApiClient(ApiClient api) { this.api = api; }

    public Order createOrder(CreateOrderRequest request) {
        return api.post("/v1/orders", request, Order.class);
    }

    public Order getOrder(String orderId) {
        return api.get("/v1/orders/" + orderId, Order.class);
    }

    public void cancelOrder(String orderId) {
        api.delete("/v1/orders/" + orderId);
    }

    public List<Order> listOrders(String customerId, String status) {
        return api.get("/v1/orders?customerId=" + customerId + "&status=" + status,
            OrderListResponse.class).getOrders();
    }
}
```

---

## Architecture Q6: Auth State Management Strategy for 500+ Tests

**Question:** Running 500 tests each requiring login would add 500 × 3s = 25 minutes of overhead. Design an auth state management strategy that eliminates login overhead while keeping test isolation.

**Answer:**

**Strategy: Pre-warm auth state files once, reuse per test via `storageState`**

**Phase 1 — Pre-warm before test run (JUnit `@BeforeAll` in a dedicated setup suite or CI pre-step):**
```java
@Tag("auth-setup")
public class AuthStateSetup {

    private static final Path AUTH_DIR = Paths.get("target/auth-states");

    @BeforeAll
    static void createAuthDir() throws IOException {
        Files.createDirectories(AUTH_DIR);
    }

    @Test
    void warmAdminAuthState() {
        captureAuthState("ADMIN", "admin@example.com", System.getenv("ADMIN_PASSWORD"));
    }

    @Test
    void warmAnalystAuthState() {
        captureAuthState("ANALYST", "analyst@example.com", System.getenv("ANALYST_PASSWORD"));
    }

    @Test
    void warmReadOnlyAuthState() {
        captureAuthState("READ_ONLY", "readonly@example.com", System.getenv("RO_PASSWORD"));
    }

    private void captureAuthState(String role, String email, String password) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(ConfigManager.getInstance().getBaseUrl() + "/login");
            page.getByLabel("Email").fill(email);
            page.getByLabel("Password").fill(password);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
            page.waitForURL("**/dashboard");

            Path statePath = AUTH_DIR.resolve(role + ".json");
            context.storageState(new BrowserContext.StorageStateOptions().setPath(statePath));

            LoggerFactory.getLogger(AuthStateSetup.class)
                .info("[AUTH] Captured state for role {}: {}", role, statePath);
            context.close();
            browser.close();
        }
    }
}
```

**Phase 2 — Test base class injects auth state into new context:**
```java
public enum UserRole {
    ADMIN("target/auth-states/ADMIN.json"),
    ANALYST("target/auth-states/ANALYST.json"),
    READ_ONLY("target/auth-states/READ_ONLY.json");

    private final String statePath;
    UserRole(String statePath) { this.statePath = statePath; }
    public Path statePath() { return Paths.get(statePath); }

    public boolean isValid() {
        return Files.exists(statePath()) &&
               Instant.now().isBefore(lastModified().plus(Duration.ofHours(8)));
    }

    private Instant lastModified() {
        try { return Files.getLastModifiedTime(statePath()).toInstant(); }
        catch (IOException e) { return Instant.EPOCH; }
    }
}

// Test — no login, context starts pre-authenticated
@Execution(ExecutionMode.CONCURRENT)
public class OrdersTest extends ParallelBaseTest {

    @Override
    protected UserRole getRole() { return UserRole.ANALYST; }

    @Test
    public void testOrderSorting() {
        // page is already logged in as ANALYST — no login step needed
        new OrdersPage(page).navigate().sortBy("Date", false);
    }
}
```

**Savings calculation:**
- Without auth pre-warming: 500 tests × 3s login = **25 min overhead**
- With pre-warming (3 roles × 1 login each): **9 seconds** of login work, saved **~25 min**

**Auth state expiry handling:**
```java
// In AuthStateCache — auto re-warm if state is stale
public static boolean hasValidState(UserRole role) {
    return Files.exists(role.statePath()) && role.isValid();
}
// CI pipeline: always run AuthStateSetup before test suites
// Local dev: state files cached until 8h expiry or manual delete
```

---

## Architecture Q7: CI/CD Pipeline Integration

**Question:** Design the complete GitHub Actions CI/CD pipeline for a Playwright Java framework — including parallelization, artifact upload, Allure report publishing, environment targeting, and failure notifications.

**Answer:**

```yaml
# .github/workflows/regression.yml
name: Playwright Java Regression Suite

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Nightly regression at 2 AM UTC
  workflow_dispatch:
    inputs:
      env:
        description: 'Target environment'
        default: 'staging'
        required: true
        type: choice
        options: [dev, staging, uat, perf]
      profile:
        description: 'Test profile'
        default: 'smoke'
        type: choice
        options: [smoke, regression, payments, orders]

env:
  JAVA_VERSION: '17'
  PLAYWRIGHT_VERSION: '1.44.0'

jobs:
  # Job 1: Pre-warm auth states (sequential, shared artifact)
  auth-setup:
    runs-on: ubuntu-latest
    container:
      image: mcr.microsoft.com/playwright/java:v1.44.0-jammy
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven

      - name: Warm auth states
        env:
          ADMIN_PASSWORD: ${{ secrets.ADMIN_PASSWORD }}
          ANALYST_PASSWORD: ${{ secrets.ANALYST_PASSWORD }}
          RO_PASSWORD: ${{ secrets.RO_PASSWORD }}
          TEST_ENV: ${{ github.event.inputs.env || 'staging' }}
        run: mvn test -pl tests-ui -Dgroups=auth-setup -Denv=$TEST_ENV -Dheadless=true

      - name: Upload auth state artifacts
        uses: actions/upload-artifact@v4
        with:
          name: auth-states
          path: target/auth-states/
          retention-days: 1

  # Job 2–5: Parallel test shards (4 parallel jobs)
  test-shard:
    needs: auth-setup
    runs-on: ubuntu-latest
    container:
      image: mcr.microsoft.com/playwright/java:v1.44.0-jammy
    strategy:
      fail-fast: false   # Don't cancel other shards on first failure
      matrix:
        shard: [1, 2, 3, 4]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven

      - name: Download auth states
        uses: actions/download-artifact@v4
        with:
          name: auth-states
          path: target/auth-states/

      - name: Run test shard ${{ matrix.shard }}
        env:
          TEST_ENV: ${{ github.event.inputs.env || 'staging' }}
          SHARD_INDEX: ${{ matrix.shard }}
          SHARD_TOTAL: 4
        run: |
          mvn test -pl tests-ui,tests-api \
            -P ${{ github.event.inputs.profile || 'smoke' }} \
            -Denv=$TEST_ENV \
            -Dheadless=true \
            -DshardIndex=$SHARD_INDEX \
            -DshardTotal=$SHARD_TOTAL \
            -DthreadCount=4

      - name: Upload Allure results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-results-shard-${{ matrix.shard }}
          path: target/allure-results/
          retention-days: 7

      - name: Upload trace artifacts on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: traces-shard-${{ matrix.shard }}
          path: target/traces/
          retention-days: 3

  # Job 6: Merge and publish Allure report
  allure-report:
    needs: test-shard
    if: always()
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Download all shard results
        uses: actions/download-artifact@v4
        with:
          pattern: allure-results-shard-*
          merge-multiple: true
          path: target/allure-results/

      - name: Generate Allure report
        uses: simple-elf/allure-report-action@master
        with:
          allure_results: target/allure-results
          allure_history: allure-history
          keep_reports: 30

      - name: Publish to GitHub Pages
        if: github.ref == 'refs/heads/main'
        uses: peaceiris/actions-gh-pages@v4
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_branch: gh-pages
          publish_dir: allure-history

      - name: Notify on failure
        if: failure()
        uses: slackapi/slack-github-action@v1.26.0
        with:
          payload: |
            {"text": ":x: Regression failed on `${{ github.event.inputs.env || 'staging' }}`\nBranch: `${{ github.ref_name }}`\nReport: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"}
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Architecture Q8: Docker Containerization Strategy

**Question:** The QA team runs tests on 5 different Mac/Windows/Linux machines. Tests pass locally but fail in CI due to browser version differences. Redesign the test execution environment using Docker.

**Answer:**

**Root cause:** Browser binaries + OS-level fonts + GPU rendering differ per machine. Solution: standardize on the official Playwright Docker image.

**`Dockerfile` for the framework:**
```dockerfile
# Use official Playwright Java image (includes all browsers + dependencies)
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy

WORKDIR /automation

# Copy Maven wrapper and POM files first (layer cache optimization)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY framework-core/pom.xml framework-core/
COPY tests-ui/pom.xml tests-ui/
COPY tests-api/pom.xml tests-api/
COPY test-data/pom.xml test-data/

# Download dependencies (cached unless POM changes)
RUN ./mvnw dependency:go-offline -q

# Copy source
COPY . .

# Build framework-core (without running tests)
RUN ./mvnw install -pl framework-core,test-data -DskipTests -q

# Default command: run smoke suite against staging
CMD ["./mvnw", "test", "-pl", "tests-ui", "-P", "smoke", \
     "-Denv=staging", "-Dheadless=true", "-DthreadCount=4"]
```

**`docker-compose.yml` for local development:**
```yaml
version: '3.9'
services:
  playwright-tests:
    build: .
    environment:
      - ENV=${ENV:-staging}
      - HEADLESS=true
      - THREAD_COUNT=${THREAD_COUNT:-4}
      - ADMIN_PASSWORD=${ADMIN_PASSWORD}
      - ANALYST_PASSWORD=${ANALYST_PASSWORD}
    volumes:
      - ./target:/automation/target   # Mount target/ so reports survive container exit
      - ./src/test/resources:/automation/src/test/resources:ro
    command: >
      ./mvnw test -pl tests-ui
      -P ${PROFILE:-smoke}
      -Denv=${ENV:-staging}
      -Dheadless=true
      -DthreadCount=${THREAD_COUNT:-4}
    shm_size: '2gb'  # Required: Chrome crashes without enough /dev/shm
    security_opt:
      - seccomp:unconfined  # Required for Chrome sandbox in Docker
```

**Running locally with Docker:**
```bash
# Smoke tests
docker-compose run playwright-tests

# Regression on UAT with 8 threads
ENV=uat PROFILE=regression THREAD_COUNT=8 docker-compose run playwright-tests

# View Allure report (mount target/ and serve)
docker run -p 8080:8080 \
  -v $(pwd)/target/allure-results:/allure-results \
  frankescobar/allure-docker-service
```

**Key Docker flags explained:**
- `shm_size: 2gb` — Chrome uses `/dev/shm` for GPU/rendering; default 64MB causes crashes
- `seccomp:unconfined` — Chrome sandbox requires `clone` syscall blocked by default Docker profile
- `--no-sandbox` flag — alternative to `seccomp:unconfined` for some environments

---

## Architecture Q9: Test Data Management Strategy

**Question:** You have 800 tests that each need isolated test data. Shared test data causes intermittent failures when tests run in parallel. Design a test data management strategy that guarantees isolation.

**Answer:**

**Four strategies (choose by context):**

**Strategy 1 — API-First Data Creation (preferred for most tests):**
```java
// Each test creates its own data via API, stores cleanup reference
@BeforeEach
void createTestData() {
    this.customer = CustomerFactory.aPremiumCustomer()
        .withName("Test-" + UUID.randomUUID().toString().substring(0, 8))
        .build();
    this.customerId = customersApi.create(customer).getId();
}

@AfterEach
void cleanup() {
    if (customerId != null) customersApi.delete(customerId);
}
```

**Strategy 2 — Builder Pattern for complex domain objects:**
```java
// Fluent, readable, default-safe builders
public class CustomerBuilder {
    private String name = "Default Test Customer";
    private String tier = "STANDARD";
    private String email = "test+" + RandomStringUtils.randomAlphanumeric(8) + "@example.com";
    private boolean active = true;

    public CustomerBuilder withName(String name) { this.name = name; return this; }
    public CustomerBuilder withTier(String tier) { this.tier = tier; return this; }
    public CustomerBuilder withEmail(String email) { this.email = email; return this; }

    public CreateCustomerRequest build() {
        return new CreateCustomerRequest(name, email, tier, active);
    }
}

public class CustomerFactory {
    public static CustomerBuilder aPremiumCustomer() {
        return new CustomerBuilder().withTier("PREMIUM");
    }
    public static CustomerBuilder aStandardCustomer() {
        return new CustomerBuilder().withTier("STANDARD");
    }
}
```

**Strategy 3 — Test data namespacing (prefix with thread/run ID):**
```java
// All data created by a test run is namespaced — easy bulk cleanup
public class TestDataNamespace {
    private static final String RUN_ID = System.getenv().getOrDefault(
        "BUILD_ID", "local-" + LocalDate.now());

    public static String name(String base) {
        return "[AUTO-" + RUN_ID + "] " + base;
    }
    // e.g., "[AUTO-build-1234] Premium Customer" — easily grep/delete post-run
}
```

**Strategy 4 — Read-only reference data (seeded once, never mutated by tests):**
```java
// In test config — reference data that tests can READ but never WRITE
public class ReferenceData {
    public static final String PRODUCT_SKU_BASIC = "PROD-001";
    public static final String PRODUCT_SKU_PREMIUM = "PROD-002";
    public static final String COUNTRY_CODE_US = "US";
    // These exist in all environments, seeded by DB migration
}
```

**Cleanup strategy — centralized tracker:**
```java
public class CleanupRegistry implements AutoCloseable {
    private final Deque<Runnable> tasks = new ArrayDeque<>();

    public void register(Runnable cleanup) { tasks.push(cleanup); }

    @Override
    public void close() {
        while (!tasks.isEmpty()) {
            try { tasks.pop().run(); }
            catch (Exception e) {
                LoggerFactory.getLogger(CleanupRegistry.class)
                    .warn("Cleanup failed: {}", e.getMessage());
            }
        }
    }
}

// Usage in BaseTest:
@AfterEach
void cleanupTestData() {
    if (cleanupRegistry != null) cleanupRegistry.close();
}
```

---

## Architecture Q10: Retry and Flakiness Management Strategy

**Question:** Across 800 tests, 40 have intermittent failures (5–10% flake rate) from animation delays, network latency, and async UI updates. How do you reduce flake without masking real bugs?

**Answer:**

**Rule 1 — Fix root cause before adding retry:**
| Flake Pattern | Root Cause | Proper Fix |
|---|---|---|
| `Element not found` | Animation/skeleton loader | `waitFor({state: 'visible'})` before action |
| `strict mode violation` | Duplicate locators | Use more specific locator (role + name) |
| `Timeout exceeded` | Slow API response | `page.waitForResponse()` not `waitForTimeout()` |
| `Element detached` | Page re-renders between locate and click | Re-locate on use (Playwright does this automatically) |

**Rule 2 — Use `waitForResponse` over arbitrary sleeps:**
```java
// Bad — race condition
page.locator("#submit").click();
page.waitForTimeout(2000);
assertThat(page.locator(".success")).isVisible();

// Good — deterministic wait
page.waitForResponse(
    resp -> resp.url().contains("/api/orders") && resp.status() == 201,
    () -> page.locator("#submit").click()
);
assertThat(page.locator(".success")).isVisible();
```

**Rule 3 — Targeted retry only for known transient operations:**
```java
// Retry ONLY for specific known-transient operations, not globally
public class RetryableActions {
    public static void clickWithRetry(Locator locator, int maxAttempts) {
        RetryUtil.execute(maxAttempts, Duration.ofMillis(300), () -> {
            locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
            locator.click();
        });
    }
}
```

**Rule 4 — JUnit 5 `@RepeatedTest` and retry extension for flaky tests (quarantine):**
```java
// Annotate known-flaky tests for tracking, NOT to hide failures permanently
@Tag("flaky")
@RepeatedTest(value = 3, failureThreshold = 1)
public void testRealtimeDashboardUpdate() {
    // Known flaky due to WebSocket timing — tracked in Jira SDET-123
}
```

**Rule 5 — Allure flakiness tracking:**
```java
@Step("Retry wrapper: {stepName}")
public static <T> T withRetry(String stepName, int maxAttempts, Supplier<T> action) {
    for (int i = 1; i <= maxAttempts; i++) {
        try {
            return action.get();
        } catch (Exception e) {
            Allure.addAttachment("Retry " + i + " failed", e.getMessage());
            if (i == maxAttempts) throw e;
        }
    }
    throw new RuntimeException("Unreachable");
}
```

**Flake reduction target:** < 1% flake rate. Tests above 2% flake rate are quarantined (tagged `@Disabled @Tag("quarantine")`) and tracked in Jira until root-cause fixed.

---

## Architecture Q11: Cross-Browser and Mobile Testing Strategy

**Question:** Product requirement: support Chrome, Firefox, Safari, iOS Safari, and Android Chrome. You have 800 tests. Running all 800 on 5 browsers = 4000 runs. How do you design a practical cross-browser strategy?

**Answer:**

**Tiered approach — not all tests need all browsers:**

| Tier | Browser Coverage | Test Count | Run Frequency |
|---|---|---|---|
| Tier 1 — Smoke | Chromium only | 50 critical paths | Every PR, every deploy |
| Tier 2 — Regression | Chromium + Firefox | 800 functional | Nightly |
| Tier 3 — Cross-browser | All 3 (+ mobile emulation) | 200 layout/interaction | Weekly |
| Tier 4 — Device lab | Real iOS/Android via BrowserStack | 50 critical mobile paths | On-demand before releases |

**Parameterized browser factory:**
```java
public class BrowserProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
        String[] browsers = System.getProperty("browsers", "chromium").split(",");
        return Arrays.stream(browsers).map(Arguments::of);
    }
}

@ParameterizedTest
@ArgumentsSource(BrowserProvider.class)
@Tag("cross-browser")
public void testCheckoutFlow(String browserName) {
    BrowserType browserType = switch (browserName.trim()) {
        case "firefox" -> playwright.firefox();
        case "webkit" -> playwright.webkit();
        default -> playwright.chromium();
    };
    Browser browser = browserType.launch(new BrowserType.LaunchOptions().setHeadless(true));
    // test runs against specified browser
}
```

**Mobile emulation for responsive tests:**
```java
Browser.NewContextOptions mobileOptions = new Browser.NewContextOptions()
    .setIsMobile(true)
    .setHasTouch(true)
    .setViewportSize(390, 844)  // iPhone 14 Pro
    .setDeviceScaleFactor(3)
    .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 ...)");

BrowserContext mobileContext = browser.newContext(mobileOptions);
```

**Maven profile execution:**
```bash
# Tier 2: Nightly regression on Chromium + Firefox
mvn test -P regression -Dbrowsers=chromium,firefox

# Tier 3: Weekly cross-browser
mvn test -P cross-browser -Dbrowsers=chromium,firefox,webkit -DthreadCount=6
```

---

## Architecture Q12: Cloud Testing Strategy with BrowserStack

**Question:** Your company needs compliance-verified browser testing on real devices (iOS Safari, Samsung Galaxy, real Chrome versions). Design the BrowserStack integration.

**Answer:**

**Connection via CDP (Chrome DevTools Protocol):**
```java
public class CloudBrowserFactory {

    private static final String BS_URL = "wss://cdp.browserstack.com/playwright?caps=";
    private static final String USERNAME = System.getenv("BROWSERSTACK_USERNAME");
    private static final String ACCESS_KEY = System.getenv("BROWSERSTACK_ACCESS_KEY");

    public static Browser connectToCloud(CloudCapabilities caps) {
        try {
            Map<String, Object> capabilities = Map.of(
                "browser", caps.browser(),
                "browser_version", caps.browserVersion(),
                "os", caps.os(),
                "os_version", caps.osVersion(),
                "name", caps.testName(),
                "build", System.getenv().getOrDefault("BUILD_ID", "local"),
                "project", "FinTech Automation",
                "browserstack.username", USERNAME,
                "browserstack.accessKey", ACCESS_KEY,
                "browserstack.networkLogs", "true",
                "browserstack.consoleLogs", "verbose"
            );

            String capsJson = URLEncoder.encode(
                new ObjectMapper().writeValueAsString(capabilities),
                StandardCharsets.UTF_8);

            return Playwright.create().chromium()
                .connect(BS_URL + capsJson);

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to BrowserStack: " + e.getMessage(), e);
        }
    }

    // Mark test pass/fail in BrowserStack dashboard via API
    public static void markTestResult(Page page, boolean passed, String reason) {
        String script = String.format(
            "_ => browserstack_executor: {\"action\": \"setSessionStatus\", " +
            "\"arguments\": {\"status\": \"%s\", \"reason\": \"%s\"}}",
            passed ? "passed" : "failed", reason.replace("\"", "'"));
        page.evaluate(script);
    }

    public record CloudCapabilities(
        String browser, String browserVersion,
        String os, String osVersion, String testName
    ) {}
}
```

**Usage in test with result marking:**
```java
@AfterEach
void markCloudResult(TestInfo info) {
    if (isCloudRun()) {
        CloudBrowserFactory.markTestResult(page, !testFailed,
            testFailed ? "Test failed: " + failureReason : "Test passed");
    }
}
```

---

## Architecture Q13: Reporting Strategy — Allure + Structured Logging

**Question:** QA manager needs: feature-level pass rates, slowest tests, failure trends, links to screenshots and traces from CI artifacts. Design the reporting strategy.

**Answer:**

**Allure annotation hierarchy:**
```java
@Epic("Order Management")          // Business epic — top-level grouping
@Feature("Order Creation")         // Feature within epic
@Story("Customer places online order")  // User story
@Severity(SeverityLevel.CRITICAL)  // Impact if failing
@Owner("sdet-team-orders")         // Ownership for failure notifications
@Tag("regression")                 // Suite inclusion tag
@TmsLink("JIRA-1234")             // Link to requirements
@Issue("BUG-5678")                 // Link to known bug if any
public class OrderCreationTest extends ParallelBaseTest { ... }
```

**Step-level annotation for granular failure location:**
```java
@Step("Navigate to checkout page")
public CheckoutPage openCheckout() { ... }

@Step("Fill payment details: card ending {last4}")
public CheckoutPage fillPaymentDetails(String last4, ...) { ... }

@Step("Click Place Order and wait for confirmation")
public OrderConfirmationPage placeOrder() { ... }
```

**Attachment helpers for CI artifacts:**
```java
public class AllureAttachments {

    @Attachment(value = "Screenshot", type = "image/png")
    public static byte[] captureScreenshot(Page page) {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    @Attachment(value = "API Response", type = "application/json")
    public static String attachApiResponse(String body) { return body; }

    @Attachment(value = "Page HTML", type = "text/html")
    public static String capturePageSource(Page page) { return page.content(); }

    public static void attachTraceLink(Path tracePath) {
        Allure.addAttachment("Playwright Trace",
            "To view: playwright show-trace " + tracePath.toString());
    }
}
```

**GitHub Actions — Upload Allure as PR comment:**
```yaml
- name: Comment Allure report link on PR
  if: github.event_name == 'pull_request'
  uses: mshick/add-pr-comment@v2
  with:
    message: |
      ## Test Results
      [View Allure Report](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }})
      Traces available in CI artifacts for 3 days.
```

---

## Architecture Q14: Environment Configuration Management

**Question:** The framework must run against 5 environments with different base URLs, credentials, feature flags, and database connections. Some values are secrets (passwords, API keys). Design the config management strategy.

**Answer:**

**YAML-Per-Environment + Secret Override Pattern:**

**`src/test/resources/config/staging.yml`:**
```yaml
environment: staging
baseUrl: https://staging.example.com
apiBaseUrl: https://api.staging.example.com
features:
  paymentV2: true
  newCheckout: false
timeouts:
  pageLoad: 30000
  element: 10000
  api: 15000
  download: 60000
users:
  admin:
    email: admin@staging.example.com
    # password loaded from env var ADMIN_PASSWORD — never in YAML
  analyst:
    email: analyst@staging.example.com
browsers:
  default: chromium
  headless: true
```

**`ConfigManager.java`:**
```java
@SuppressWarnings("unchecked")
public class ConfigManager {

    private static volatile ConfigManager INSTANCE;
    private final Map<String, Object> config;

    private ConfigManager() {
        String env = System.getProperty("env", System.getenv().getOrDefault("ENV", "staging"));
        String resourcePath = "config/" + env + ".yml";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Config not found: " + resourcePath);
            Yaml yaml = new Yaml();
            this.config = yaml.load(is);
            LoggerFactory.getLogger(ConfigManager.class).info("[CONFIG] Loaded: {}", resourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + resourcePath, e);
        }
    }

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ConfigManager.class) {
                if (INSTANCE == null) INSTANCE = new ConfigManager();
            }
        }
        return INSTANCE;
    }

    public String getBaseUrl() { return (String) config.get("baseUrl"); }
    public String getApiBaseUrl() { return (String) config.get("apiBaseUrl"); }

    public int getTimeout(String key) {
        return (int) ((Map<?, ?>) config.get("timeouts")).get(key);
    }

    public boolean isFeatureEnabled(String featureName) {
        return (boolean) ((Map<?, ?>) config.get("features")).getOrDefault(featureName, false);
    }

    // Secrets always come from environment variables — never from YAML/files
    public String getSecret(String secretName) {
        String value = System.getenv(secretName);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Required secret not set: " + secretName +
                ". Set the environment variable before running.");
        }
        return value;
    }

    public String getUserEmail(String role) {
        Map<?, ?> users = (Map<?, ?>) config.get("users");
        Map<?, ?> user = (Map<?, ?>) users.get(role.toLowerCase());
        return (String) user.get("email");
    }
}
```

**Secret resolution rules:**
1. Passwords, API keys, tokens → Environment variables only (never in YAML, git, or logs)
2. URLs, timeouts, feature flags → YAML per environment (safe to commit)
3. Test user emails → YAML (safe; no PII in email format)
4. CI secrets → GitHub Actions secrets, injected as `env:` block
5. Local dev → `.env` file loaded by IDE run config (`.env` in `.gitignore`)

---

## Architecture Q15: Design Patterns in Test Automation — Applied Examples

**Question:** Name and demonstrate 5 design patterns applied specifically to Playwright Java test automation.

**Answer:**

**1. Factory Pattern — Browser creation:**
```java
// Decouples test code from browser instantiation details
public class BrowserFactory {
    public static Browser create(BrowserType.LaunchOptions opts) {
        return switch (System.getProperty("browser", "chromium")) {
            case "firefox" -> playwright.firefox().launch(opts);
            case "webkit"  -> playwright.webkit().launch(opts);
            default        -> playwright.chromium().launch(opts);
        };
    }
}
```

**2. Builder Pattern — Test data:**
```java
// Complex object construction without telescoping constructors
Order order = OrderBuilder.anOrder()
    .forCustomer("CUST-001").withProduct("SKU-001", 2)
    .withTier("PREMIUM").withPriority(true).build();
```

**3. Template Method Pattern — Base test lifecycle:**
```java
// Algorithm skeleton in base class; steps overridden in subclasses
public abstract class ParallelBaseTest {
    @BeforeEach final void setUp() { page = initPage(); customSetup(); }     // template
    @AfterEach  final void tearDown() { captureEvidence(); customTeardown(); }
    protected void customSetup() {}    // hook — override in subclass
    protected void customTeardown() {} // hook — override in subclass
}
```

**4. Strategy Pattern — Locator resolution:**
```java
// Swap locator strategy at runtime
public interface LocatorStrategy { Locator locate(Page page, String identifier); }
public class ByTestId implements LocatorStrategy { ... }
public class ByRole implements LocatorStrategy { ... }
public class ByLabel implements LocatorStrategy { ... }

// SmartLocator tries strategies in order
```

**5. Decorator Pattern — Allure-aware step logging:**
```java
// Wraps existing action with Allure step annotation at call site
public static <T> T step(String name, Supplier<T> action) {
    return Allure.step(name, action::get);
}
// Usage:
step("Click place order", () -> checkoutPage.placeOrder());
```

**6. Singleton Pattern — ConfigManager, TokenManager:**
```java
// Single instance with double-checked locking for thread safety
private static volatile ConfigManager INSTANCE;
public static ConfigManager getInstance() {
    if (INSTANCE == null) {
        synchronized (ConfigManager.class) {
            if (INSTANCE == null) INSTANCE = new ConfigManager();
        }
    }
    return INSTANCE;
}
```

---

## Architecture Q16: Handling Test Execution at Scale — Sharding Strategy

**Question:** You have 800 tests and 4 CI runners available. How do you distribute tests efficiently to maximize parallelism and minimize total run time?

**Answer:**

**Two-Level Parallelism:**
- Level 1 (Outer) — GitHub Actions matrix: 4 parallel jobs (shards)
- Level 2 (Inner) — JUnit parallel execution: 4 threads per shard = 16 concurrent tests

**Total concurrency: 4 shards × 4 threads = 16 simultaneous browser contexts**

**JUnit 5 sharding via custom `TestShard` condition:**
```java
public class ShardCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        int shardIndex = Integer.parseInt(System.getProperty("shardIndex", "1"));
        int shardTotal = Integer.parseInt(System.getProperty("shardTotal", "1"));

        if (shardTotal == 1) return ConditionEvaluationResult.enabled("No sharding");

        // Deterministic hash-based assignment — same test always goes to same shard
        String testId = context.getRequiredTestClass().getName() +
            "#" + context.getDisplayName();
        int assignedShard = (Math.abs(testId.hashCode()) % shardTotal) + 1;

        return assignedShard == shardIndex
            ? ConditionEvaluationResult.enabled("Shard " + shardIndex)
            : ConditionEvaluationResult.disabled("Assigned to shard " + assignedShard);
    }
}

// Register globally in META-INF/services/org.junit.jupiter.api.extension.Extension
// or via @ExtendWith in BaseTest
```

**`junit-platform.properties` for inner parallelism:**
```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4
junit.jupiter.execution.parallel.config.fixed.max-pool-size=8
```

**GitHub Actions matrix strategy:**
```yaml
strategy:
  fail-fast: false
  matrix:
    shard: [1, 2, 3, 4]
steps:
  - run: mvn test -DshardIndex=${{ matrix.shard }} -DshardTotal=4 -DthreadCount=4
```

**Execution time calculation:**
- 800 tests × 8s avg = 6400s = ~107 min sequential
- 4 shards × 4 threads × 8s avg = 6400 / 16 = 400s = **~7 min total**

---

## Architecture Q17: Security Testing Integration in the Framework

**Question:** The security team requires automated OWASP checks as part of the CI pipeline. How do you integrate security testing into your Playwright Java framework without rebuilding from scratch?

**Answer:**

**Layer 1 — Header security assertions (in every authenticated test):**
```java
public class SecurityHeadersValidator {
    public static void assertSecurityHeaders(APIResponse response) {
        Map<String, String> headers = response.headers();
        assertThat(headers).containsKey("strict-transport-security")
            .as("HSTS header missing — MITM risk");
        assertThat(headers).containsKey("x-content-type-options")
            .as("X-Content-Type-Options missing — MIME sniffing risk");
        assertThat(headers).containsKey("x-frame-options")
            .as("X-Frame-Options missing — clickjacking risk");
        assertThat(headers.get("x-frame-options"))
            .isIn("DENY", "SAMEORIGIN");
        assertThat(headers).doesNotContainKey("x-powered-by")
            .as("X-Powered-By exposes server type");
        assertThat(headers).doesNotContainKey("server")
            .as("Server header exposes version info");
    }
}
```

**Layer 2 — Input validation boundary tests:**
```java
@ParameterizedTest
@ValueSource(strings = {
    "<script>alert(1)</script>",
    "'; DROP TABLE orders; --",
    "../../../../etc/passwd",
    "${7*7}",                        // SSTI
    "\u0000",                        // Null byte
    "A".repeat(10001)               // Oversized input
})
@Tag("security")
public void testInputValidation(String maliciousInput) {
    page.getByLabel("Company Name").fill(maliciousInput);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
    assertThat(page.locator(".error-message")).isVisible();
    assertThat(page.locator(".error-message").textContent())
        .doesNotContain(maliciousInput); // Reflected XSS check
    assertThat(page.locator("body").textContent())
        .doesNotContain("<script>"); // Script not rendered
}
```

**Layer 3 — OWASP ZAP passive scan integration (API level):**
```yaml
# In CI — run ZAP passive scan alongside test suite
- name: ZAP API Scan
  uses: zaproxy/action-api-scan@v0.7.0
  with:
    target: ${{ env.API_BASE_URL }}/openapi.json
    rules_file_name: .zap/rules.tsv
    fail_action: true
```

**Layer 4 — Auth boundary tests:**
```java
@Test
@Tag("security")
public void testHorizontalPrivilegeEscalation() {
    // User A creates an order
    String orderIdA = createOrderAs(UserRole.ANALYST, "user-a@example.com");
    // User B (different tenant) tries to access User A's order
    APIResponse response = apiAs(UserRole.ANALYST, "user-b@example.com")
        .get("/v1/orders/" + orderIdA);
    assertThat(response.status()).isIn(403, 404); // Must not return 200
}
```

---

## Architecture Q18: Framework Versioning and Governance

**Question:** Your framework is used by 12 engineers across 3 product lines. How do you manage versioning, breaking changes, and knowledge sharing so the framework evolves without breaking existing tests?

**Answer:**

**Semantic versioning of `framework-core`:**
```
1.x.y — MINOR: new utility classes, backward-compatible
2.x.y — MAJOR: breaking API changes (require team migration)
```

**Deprecation policy:**
```java
// Never delete — deprecate with migration note for 2 major versions
@Deprecated(since = "2.0.0", forRemoval = true)
// @see SmartLocator#resolve(Page, String) as replacement
public Locator findElement(String cssSelector) { ... }
```

**Architecture Decision Records (ADRs) in repo:**
```
docs/adr/
├── 001-threadlocal-browser-lifecycle.md
├── 002-auth-state-prewarming.md
├── 003-allure-over-extent-reports.md
└── 004-component-object-model.md
```

**PR checklist for framework changes:**
```markdown
## Framework Change Checklist
- [ ] Backward compatible OR marked @Deprecated with migration path
- [ ] Unit tests added for framework-core changes
- [ ] ADR updated or created for architectural decisions
- [ ] CHANGELOG.md updated
- [ ] Notified #qa-framework Slack channel
- [ ] Example usage added to docs/examples/
```

**`CHANGELOG.md` maintained per release:**
```markdown
## [2.1.0] - 2024-Q3
### Added
- DataConsistencyValidator — cross-source field validation
- CursorPaginator — cursor-based API pagination

### Deprecated
- BasePage.findByCss() — use SmartLocator.resolve() instead

### Fixed
- TokenManager: race condition on concurrent token refresh
```

---

## Architecture Q19: Handling Async UI Patterns Without Hard Waits

**Question:** Your app uses optimistic UI updates, eventual consistency, WebSocket pushes, and skeleton loaders. Across 400 tests, the QA team added 200 `waitForTimeout(2000)` calls. Tests still flake. Fix this systematically.

**Answer:**

**Root cause inventory:**
| Pattern | Wrong fix | Correct fix |
|---|---|---|
| Skeleton loader | `waitForTimeout(2000)` | `page.locator(".skeleton").waitFor(HIDDEN)` |
| Optimistic update then server confirm | Sleep after click | `page.waitForResponse(url, action)` |
| WebSocket push updates UI | Sleep | `WebSocketMonitor.waitForMessage(predicate)` |
| Table data load | Sleep | `page.locator("tbody tr").first().waitFor()` |
| Toast notification | Sleep | `page.locator(".toast").waitFor(VISIBLE)` |
| Modal open animation | Sleep | `page.locator(".modal .modal-body").waitFor(VISIBLE)` |

**Framework-level enforcement — custom wait utilities:**
```java
public class WaitUtils {

    // Wait for skeleton loaders to disappear
    public static void waitForSkeletonGone(Page page) {
        page.locator(".skeleton, .loading-placeholder, [data-loading='true']")
            .first()
            .waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(15_000));
    }

    // Wait for a network response matching predicate, triggered by an action
    public static APIResponse waitForApiResponse(Page page,
                                                   Predicate<Response> matcher,
                                                   Runnable trigger) {
        return page.waitForResponse(matcher::test, trigger::run);
    }

    // Wait for a specific element count (async data table population)
    public static void waitForMinRowCount(Page page, Locator rows, int minCount) {
        page.waitForFunction(
            "([selector, count]) => document.querySelectorAll(selector).length >= count",
            List.of(rows.toString(), minCount),
            new Page.WaitForFunctionOptions().setTimeout(15_000)
        );
    }

    // Detect and fail immediately if any explicit sleep crept into test code
    // (enforced via ArchUnit in CI)
}
```

**ArchUnit rule to ban `waitForTimeout` in test code:**
```java
@ArchTest
static final ArchRule NO_EXPLICIT_WAITS = noClasses()
    .that().resideInAPackage("..tests..")
    .should().callMethod(Page.class, "waitForTimeout", double.class)
    .because("Use WaitUtils or Playwright's built-in waitForResponse/waitForSelector instead");
```

---

## Architecture Q20: Framework Health Metrics and Continuous Improvement

**Question:** How do you measure the health of a test automation framework over time? What metrics do you track, and how do you use them to drive improvements?

**Answer:**

**Tier 1 — Execution Health Metrics (from CI runs):**
| Metric | Target | Alert if |
|---|---|---|
| Pass rate | > 98% | < 95% |
| Flake rate (test passes on retry) | < 1% | > 3% |
| Run time — smoke suite | < 5 min | > 10 min |
| Run time — full regression | < 15 min | > 30 min |
| Auth state warm-up time | < 30s | > 2 min |

**Tier 2 — Framework Quality Metrics (from code analysis):**
```java
// ArchUnit rules enforced in CI — violations = build failure
@ArchTest
static final ArchRule NO_THREAD_SLEEP = noClasses()
    .that().resideInAPackage("..tests..")
    .should().callMethod(Thread.class, "sleep", long.class);

@ArchTest
static final ArchRule PAGE_OBJECTS_DONT_IMPORT_JUNIT =
    noClasses().that().resideInAPackage("..pages..")
    .should().dependOnClassesThat().resideInAPackage("org.junit..");

@ArchTest
static final ArchRule TESTS_EXTEND_BASE_TEST =
    classes().that().resideInAPackage("..suites..")
    .and().areAnnotatedWith(Test.class)
    .should().beAssignableTo(ParallelBaseTest.class);
```

**Tier 3 — Coverage Metrics (linked to requirements):**
```java
// In Allure — @TmsLink tags map to test management system
// Coverage report: % of user stories with automated tests
// Target: 100% of P0/P1 stories have >= 1 automated test
@TmsLink("STORY-1234")
@TmsLink("STORY-5678")
@Test
public void testOrderCreation() { ... }
```

**Tier 4 — Maintenance Metrics (from git history):**
- Tests modified per sprint (high churn = fragile locators)
- Flaky test list age (old flaky tests = not being fixed)
- TODOs/fixme count in framework code (technical debt)
- Time to add a new Page Object (developer productivity)

**Monthly framework review checklist:**
```
□ Review Allure trend report — identify consistently failing tests
□ Review flaky test registry — close or fix tests > 30 days old
□ Run ArchUnit in report-only mode — count rule violations
□ Check Playwright version — update if behind by > 2 minor versions
□ Review parallel execution efficiency — actual vs theoretical throughput
□ Survey engineers — friction points in framework adoption
□ Update CHANGELOG.md with improvements made
```

---

*— End of Architecture Q1–Q20 | Section 3 Complete (20/20) —*

---

# SECTION 4 — UI + API SCENARIO-BASED QUESTIONS (20)

> **UI+API Scenarios Q1–Q20** — Customer onboarding, Order placement, Payment lifecycle, Claims processing, Portfolio creation, Approval workflows, Audit log verification, Bulk operations, Role-based access, and Notification validation.

---

## Scenario Q1: New Customer Onboarding — End-to-End

**Business Context:** A new B2B customer signs up via the portal. The onboarding wizard captures company details, contact info, selects a subscription tier, uploads a signed contract, and triggers an automated Welcome email.

**What to test (UI + API cross-validation):**
- Wizard advances correctly through all 5 steps
- Required field validation fires on each step
- Uploaded PDF contract is stored and retrievable via API
- Customer record is created with correct tier in API
- Welcome email is queued (validated via email-events API)
- Audit log records the creation event

**Test:**
```java
@Test
@Epic("Customer Management")
@Feature("Customer Onboarding")
@Story("B2B customer completes onboarding wizard")
@Severity(SeverityLevel.BLOCKER)
public void testNewB2BCustomerOnboarding() {
    // ── SETUP ────────────────────────────────────────────
    String companyName = TestDataNamespace.name("Acme Corp");
    String email = "billing+" + UUID.randomUUID().toString().substring(0, 6) + "@acme-test.com";

    // ── STEP 1: Navigate and fill Basic Info ─────────────
    page.navigate(config.getBaseUrl() + "/onboarding");
    assertThat(page.locator("h1")).hasText("New Customer Onboarding");

    new FormFiller(page).fill(Map.of(
        "Company Name",  companyName,
        "Industry",      "Technology",
        "Company Size",  "201-500"
    ));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

    // ── STEP 2: Contact Details ───────────────────────────
    assertThat(page.locator(".wizard-step--active")).containsText("Contact Details");
    new FormFiller(page).fill(Map.of(
        "Primary Email",   email,
        "Phone",           "+1-555-010-0202",
        "Billing Address", "123 Enterprise Blvd, New York, NY 10001"
    ));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

    // ── STEP 3: Subscription Tier ─────────────────────────
    assertThat(page.locator(".wizard-step--active")).containsText("Subscription");
    page.getByTestId("tier-PREMIUM").click();
    assertThat(page.getByTestId("tier-PREMIUM")).hasClass(Pattern.compile(".*selected.*"));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

    // ── STEP 4: Upload Signed Contract ────────────────────
    assertThat(page.locator(".wizard-step--active")).containsText("Contract");
    new UploadUtils(page).uploadFile(
        page.locator("input[type='file'][accept='.pdf']"),
        Paths.get("src/test/resources/fixtures/sample-contract.pdf")
    );
    assertThat(page.locator(".upload-success")).isVisible();
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

    // ── STEP 5: Review & Submit ────────────────────────────
    assertThat(page.locator(".review-section")).containsText(companyName);
    assertThat(page.locator(".review-section")).containsText("PREMIUM");
    assertThat(page.locator(".review-section")).containsText(email);

    page.getByRole(AriaRole.CHECKBOX,
        new Page.GetByRoleOptions().setName("I confirm all details are correct")).check();

    APIResponse createResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/customers") && r.status() == 201,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Complete Onboarding")).click()
    );

    // ── UI: Confirmation screen ────────────────────────────
    page.waitForURL("**/onboarding/complete");
    assertThat(page.locator(".success-heading")).hasText("Welcome to the platform!");
    String customerId = page.locator("[data-field='customer-id']").textContent().trim();
    assertThat(customerId).matches("CUST-\\d+");

    // ── API: Customer created with correct data ────────────
    Map<String, Object> customer = api.get("/v1/customers/" + customerId, Map.class);
    assertThat(customer.get("name")).isEqualTo(companyName);
    assertThat(customer.get("tier")).isEqualTo("PREMIUM");
    assertThat(customer.get("status")).isEqualTo("ACTIVE");
    assertThat(customer.get("primaryEmail")).isEqualTo(email);

    // ── API: Contract document stored ────────────────────
    Map<String, Object> docs = api.get("/v1/customers/" + customerId + "/documents", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> docList = (List<Map<String, Object>>) docs.get("documents");
    assertThat(docList).hasSize(1);
    assertThat(docList.get(0).get("type")).isEqualTo("SIGNED_CONTRACT");
    assertThat(docList.get(0).get("status")).isEqualTo("STORED");

    // ── API: Welcome email event queued ───────────────────
    Map<String, Object> emailEvents = api.get(
        "/v1/email-events?customerId=" + customerId + "&type=WELCOME", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> events = (List<Map<String, Object>>) emailEvents.get("events");
    assertThat(events).hasSize(1);
    assertThat(events.get(0).get("status")).isIn("QUEUED", "SENT");

    // ── API: Audit log entry created ──────────────────────
    Map<String, Object> auditLog = api.get(
        "/v1/audit-log?entityId=" + customerId + "&action=CUSTOMER_CREATED", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> entries = (List<Map<String, Object>>) auditLog.get("entries");
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).get("actorType")).isEqualTo("USER");

    // ── CLEANUP ───────────────────────────────────────────
    cleanup.register(() -> api.delete("/v1/customers/" + customerId));
}
```

---

## Scenario Q2: Order Placement with Inventory Validation

**Business Context:** A procurement officer places an order for 3 products. One product has limited stock (2 units available, 5 requested). The system must warn before checkout, block over-ordering, and create a partial order.

**What to test:**
- Product catalog shows real-time stock from API
- Adding quantity > stock shows inline warning
- Checkout blocked until quantity is corrected
- Partial order created for available stock
- Order confirmation shows partial fulfillment
- Inventory API reflects reduced stock atomically

**Test:**
```java
@Test
@Feature("Order Management")
@Story("Partial order when stock insufficient")
@Severity(SeverityLevel.CRITICAL)
public void testOrderWithInsufficientInventory() {
    // ── SETUP: Create products via API ────────────────────
    String sku1 = "SKU-FULL-" + UUID.randomUUID().toString().substring(0, 6);
    String sku2 = "SKU-LOW-" + UUID.randomUUID().toString().substring(0, 6);

    api.post("/v1/inventory", Map.of("sku", sku1, "stock", 100, "name", "Full Stock Widget"), Map.class);
    api.post("/v1/inventory", Map.of("sku", sku2, "stock", 2, "name", "Low Stock Gadget"), Map.class);
    cleanup.register(() -> api.delete("/v1/inventory/" + sku1));
    cleanup.register(() -> api.delete("/v1/inventory/" + sku2));

    // ── UI: Add products to cart ──────────────────────────
    page.navigate(config.getBaseUrl() + "/catalog");
    page.getByTestId("product-" + sku1).locator("[data-action='add-to-cart']").click();
    page.getByTestId("product-" + sku2).locator("[data-action='add-to-cart']").click();

    // ── UI: Navigate to cart ──────────────────────────────
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cart (2)")).click();

    // Set quantity 5 for low-stock item
    page.getByTestId("cart-item-" + sku2).locator(".qty-input").fill("5");
    page.getByTestId("cart-item-" + sku2).locator(".qty-input").press("Tab");

    // ── UI: Inline stock warning appears ──────────────────
    assertThat(page.getByTestId("cart-item-" + sku2).locator(".stock-warning"))
        .isVisible()
        .containsText("Only 2 available");

    // ── UI: Checkout button is disabled ───────────────────
    assertThat(page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Proceed to Checkout"))).isDisabled();

    // ── UI: Correct quantity to available stock ───────────
    page.getByTestId("cart-item-" + sku2).locator(".qty-input").fill("2");
    page.getByTestId("cart-item-" + sku2).locator(".qty-input").press("Tab");
    assertThat(page.getByTestId("cart-item-" + sku2).locator(".stock-warning")).not().isVisible();
    assertThat(page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Proceed to Checkout"))).isEnabled();

    // ── UI: Complete checkout ─────────────────────────────
    APIResponse orderResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/orders") && r.request().method().equals("POST"),
        () -> {
            page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Proceed to Checkout")).click();
            page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Confirm Order")).click();
        }
    );
    assertThat(orderResponse.status()).isEqualTo(201);
    page.waitForURL("**/orders/confirmed/**");

    String orderId = page.locator("[data-field='order-id']").textContent().trim();

    // ── API: Order created with correct line items ─────────
    Map<String, Object> order = api.get("/v1/orders/" + orderId, Map.class);
    assertThat(order.get("status")).isEqualTo("CONFIRMED");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
    Map<String, Object> lowStockItem = items.stream()
        .filter(i -> sku2.equals(i.get("sku"))).findFirst().orElseThrow();
    assertThat(lowStockItem.get("quantity")).isEqualTo(2);

    // ── API: Inventory decremented atomically ──────────────
    Map<String, Object> inventory = api.get("/v1/inventory/" + sku2, Map.class);
    assertThat(inventory.get("stock")).isEqualTo(0);
    assertThat(inventory.get("status")).isEqualTo("OUT_OF_STOCK");

    cleanup.register(() -> api.delete("/v1/orders/" + orderId));
}
```

---

## Scenario Q3: Payment Processing — Success, Decline, and Retry

**Business Context:** A FinTech checkout flow supports credit card and ACH payments. Cards can be declined (insufficient funds, expired, fraud hold). Users must retry with a different card without losing their cart.

**What to test:**
- Successful card payment completes order
- Declined card shows specific decline reason
- Cart persists after decline — no data loss
- Retry with second card succeeds
- Payment events recorded in API with correct statuses
- Idempotency — submitting the same form twice doesn't charge twice

**Test:**
```java
@Test
@Feature("Payment Processing")
@Story("Card declined then successful retry")
@Severity(SeverityLevel.BLOCKER)
public void testPaymentDeclineAndRetry() {
    // ── SETUP: Create order in PENDING_PAYMENT state ──────
    String orderId = api.post("/v1/orders", Map.of(
        "customerId", testCustomerId,
        "items", List.of(Map.of("sku", "PROD-001", "quantity", 1)),
        "status", "PENDING_PAYMENT"
    ), Map.class).get("id").toString();
    cleanup.register(() -> api.delete("/v1/orders/" + orderId));

    page.navigate(config.getBaseUrl() + "/orders/" + orderId + "/payment");
    assertThat(page.locator("h1")).containsText("Payment");

    // ── ATTEMPT 1: Declined card (test card number) ───────
    new FormFiller(page).fill(Map.of(
        "Card Number", "4000000000000002",   // Stripe test: always declined
        "Expiry",      "12/28",
        "CVV",         "123",
        "Name on Card","Test User"
    ));

    APIResponse declineResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/payments") && r.status() == 402,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Pay Now")).click()
    );
    Map<String, Object> declineBody = (Map<String, Object>) declineResponse.json();
    assertThat(declineBody.get("code")).isEqualTo("INSUFFICIENT_FUNDS");

    // ── UI: Decline reason shown, form preserved ──────────
    assertThat(page.locator(".payment-error")).containsText("Insufficient funds");
    assertThat(page.locator(".payment-error [data-action='use-different-card']")).isVisible();
    assertThat(page.locator("[name='cardNumber']")).isVisible(); // form still present

    // ── ATTEMPT 2: Valid card ─────────────────────────────
    page.getByTestId("card-number-input").clear();
    new FormFiller(page).fill(Map.of(
        "Card Number", "4242424242424242",   // Stripe test: success
        "Expiry",      "12/28",
        "CVV",         "123"
    ));

    APIResponse successResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/payments") && r.status() == 201,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Pay Now")).click()
    );
    page.waitForURL("**/payment/confirmed");
    assertThat(page.locator(".confirmation-heading")).containsText("Payment Successful");

    // ── API: Order updated to PAID ────────────────────────
    Map<String, Object> order = api.get("/v1/orders/" + orderId, Map.class);
    assertThat(order.get("status")).isEqualTo("PAID");

    // ── API: Both payment attempts recorded ───────────────
    Map<String, Object> payments = api.get("/v1/payments?orderId=" + orderId, Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> attempts = (List<Map<String, Object>>) payments.get("attempts");
    assertThat(attempts).hasSize(2);
    assertThat(attempts.get(0).get("status")).isEqualTo("DECLINED");
    assertThat(attempts.get(0).get("declineCode")).isEqualTo("INSUFFICIENT_FUNDS");
    assertThat(attempts.get(1).get("status")).isEqualTo("CAPTURED");

    // ── IDEMPOTENCY: Re-submitting same confirmation → no double charge ──
    String paymentId = attempts.get(1).get("id").toString();
    APIResponse idempotentPost = api.post(
        "/v1/payments/" + paymentId + "/capture",
        Map.of("orderId", orderId),
        APIResponse.class
    );
    // Should return existing captured payment, not create a new one
    assertThat(idempotentPost.status()).isEqualTo(200);
    Map<String, Object> recheckPayments = api.get("/v1/payments?orderId=" + orderId, Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> recheckedAttempts =
        (List<Map<String, Object>>) recheckPayments.get("attempts");
    assertThat(recheckedAttempts.stream()
        .filter(a -> "CAPTURED".equals(a.get("status"))).count()).isEqualTo(1);
}
```

---

## Scenario Q4: Role-Based Access Control Verification

**Business Context:** The platform has 4 roles: ADMIN, MANAGER, ANALYST, READ_ONLY. Each role has specific UI element visibility and API endpoint permissions. A test suite must verify all RBAC rules.

**What to test:**
- Each role sees only their permitted navigation items
- ANALYST cannot access admin settings page (UI redirect)
- READ_ONLY cannot call mutating API endpoints (403)
- ADMIN can impersonate another user
- UI elements (edit/delete buttons) hidden for restricted roles
- API enforces permissions independent of UI state

**Test:**
```java
@ParameterizedTest
@EnumSource(UserRole.class)
@Feature("Security")
@Story("Role-based access control")
public void testRolePermissions(UserRole role) {
    // Inject auth state for the specific role
    BrowserContext context = TestExecutionContext.getBrowser().newContext(
        new Browser.NewContextOptions()
            .setStorageStatePath(role.statePath())
    );
    Page rolePage = context.newPage();

    try {
        rolePage.navigate(config.getBaseUrl() + "/dashboard");
        assertThat(rolePage.locator(".nav-item")).containsText("Dashboard");

        // Role-specific UI assertions
        switch (role) {
            case ADMIN -> {
                assertThat(rolePage.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("Admin Settings"))).isVisible();
                assertThat(rolePage.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("User Management"))).isVisible();
            }
            case MANAGER -> {
                assertThat(rolePage.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("Reports"))).isVisible();
                assertThat(rolePage.locator("[data-nav='admin-settings']")).not().isVisible();
            }
            case ANALYST -> {
                assertThat(rolePage.locator("[data-action='delete']")).not().isVisible();
                assertThat(rolePage.locator("[data-action='export']")).isVisible();
            }
            case READ_ONLY -> {
                assertThat(rolePage.locator("[data-action='edit']")).not().isVisible();
                assertThat(rolePage.locator("[data-action='delete']")).not().isVisible();
                assertThat(rolePage.locator("[data-action='create']")).not().isVisible();
            }
        }

        // Test direct URL access to restricted pages
        if (role != UserRole.ADMIN) {
            rolePage.navigate(config.getBaseUrl() + "/admin/settings");
            assertThat(rolePage.locator(".access-denied, [data-page='403']")).isVisible();
            assertThat(rolePage.url()).doesNotContain("/admin/settings");
        }

        // API enforcement — independent of UI
        APIRequestContext apiContext = rolePage.request();
        APIResponse mutateResponse = apiContext.post(
            config.getApiBaseUrl() + "/v1/users",
            RequestOptions.create().setData("{\"name\":\"Hacker\"}")
        );

        int expectedStatus = (role == UserRole.ADMIN || role == UserRole.MANAGER)
            ? 201 : 403;
        assertThat(mutateResponse.status()).isEqualTo(expectedStatus);

    } finally {
        context.close();
    }
}
```

---

## Scenario Q5: Insurance Claims — Multi-Stage Workflow

**Business Context:** An insurance claim goes through: SUBMITTED → UNDER_REVIEW → DOCUMENTS_REQUESTED → DOCUMENTS_RECEIVED → APPROVED/REJECTED. Each transition has specific actor constraints and notification triggers.

**What to test:**
- Claimant submits claim with required documents
- Adjuster can request additional documents
- System sends notification to claimant on document request
- Claimant uploads additional document
- Adjuster approves and approval is reflected in all views
- Rejected claim shows reason and appeal options

**Test:**
```java
@Test
@Epic("Claims Management")
@Feature("Claim Lifecycle")
@Story("Claim submitted, documents requested, then approved")
@Severity(SeverityLevel.BLOCKER)
public void testClaimFullApprovalWorkflow() {
    // ── SETUP: Create policy via API ──────────────────────
    String policyId = api.post("/v1/policies", Map.of(
        "customerId", testCustomerId,
        "type", "COMPREHENSIVE",
        "premium", 1200.00
    ), Map.class).get("id").toString();
    cleanup.register(() -> api.delete("/v1/policies/" + policyId));

    // ── ACT AS CLAIMANT: Submit claim via UI ──────────────
    switchToRole(UserRole.ANALYST);  // claimant role
    page.navigate(config.getBaseUrl() + "/claims/new");

    new FormFiller(page).fill(Map.of(
        "Policy ID",          policyId,
        "Claim Type",         "Property Damage",
        "Incident Date",      "2024-11-15",
        "Description",        "Vehicle damaged in parking lot collision",
        "Estimated Amount",   "4500"
    ));

    new UploadUtils(page).uploadFile(
        page.locator("[data-field='incident-report']"),
        Paths.get("src/test/resources/fixtures/incident-report.pdf")
    );

    APIResponse submitResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/claims") && r.status() == 201,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Submit Claim")).click()
    );
    String claimId = ((Map<?, ?>) submitResponse.json()).get("id").toString();
    cleanup.register(() -> api.delete("/v1/claims/" + claimId));

    // UI: Confirmation page
    assertThat(page.locator(".claim-status-badge")).hasText("SUBMITTED");
    assertThat(page.locator(".claim-id")).containsText(claimId);

    // ── API: Claim in SUBMITTED state ─────────────────────
    Map<String, Object> claim = api.get("/v1/claims/" + claimId, Map.class);
    assertThat(claim.get("status")).isEqualTo("SUBMITTED");

    // ── ACT AS ADJUSTER: Request additional documents ──────
    switchToRole(UserRole.ADMIN);  // adjuster role
    page.navigate(config.getBaseUrl() + "/adjuster/claims/" + claimId);

    page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Request Documents")).click();

    page.getByLabel("Required Documents").fill("Police report, Repair estimate from certified shop");
    page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Send Request")).click();

    assertThat(page.locator(".claim-status-badge")).hasText("DOCUMENTS_REQUESTED");

    // ── API: Status updated + notification queued ─────────
    Map<String, Object> updatedClaim = api.get("/v1/claims/" + claimId, Map.class);
    assertThat(updatedClaim.get("status")).isEqualTo("DOCUMENTS_REQUESTED");

    Map<String, Object> notifications = api.get(
        "/v1/notifications?claimId=" + claimId + "&type=DOCUMENTS_REQUESTED", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> notifs = (List<Map<String, Object>>) notifications.get("items");
    assertThat(notifs).hasSize(1);
    assertThat(notifs.get(0).get("recipientEmail")).isEqualTo(claimantEmail);

    // ── ACT AS CLAIMANT: Upload requested documents ────────
    switchToRole(UserRole.ANALYST);
    page.navigate(config.getBaseUrl() + "/claims/" + claimId + "/documents");

    new UploadUtils(page).uploadFile(
        page.locator("[data-field='police-report']"),
        Paths.get("src/test/resources/fixtures/police-report.pdf")
    );
    new UploadUtils(page).uploadFile(
        page.locator("[data-field='repair-estimate']"),
        Paths.get("src/test/resources/fixtures/repair-estimate.pdf")
    );
    page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Submit Documents")).click();
    assertThat(page.locator(".claim-status-badge")).hasText("DOCUMENTS_RECEIVED");

    // ── ACT AS ADJUSTER: Approve claim ────────────────────
    switchToRole(UserRole.ADMIN);
    page.navigate(config.getBaseUrl() + "/adjuster/claims/" + claimId);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Approve Claim")).click();
    page.getByLabel("Approved Amount").fill("4200");
    page.getByLabel("Adjuster Notes").fill("Valid claim, repair estimate verified.");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm Approval")).click();

    assertThat(page.locator(".claim-status-badge")).hasText("APPROVED");
    assertThat(page.locator("[data-field='approved-amount']")).containsText("4,200");

    // ── API: Final state validation ────────────────────────
    Map<String, Object> approvedClaim = api.get("/v1/claims/" + claimId, Map.class);
    assertThat(approvedClaim.get("status")).isEqualTo("APPROVED");
    assertThat(approvedClaim.get("approvedAmount")).isEqualTo(4200.0);
    assertThat(approvedClaim.get("adjusterNotes")).isNotNull();

    // ── API: Payout scheduled ─────────────────────────────
    Map<String, Object> payout = api.get("/v1/payouts?claimId=" + claimId, Map.class);
    assertThat(payout.get("status")).isIn("SCHEDULED", "PROCESSING");
    assertThat(payout.get("amount")).isEqualTo(4200.0);
}
```

---

## Scenario Q6: Fund Creation and NAV Calculation (Asset Management)

**Business Context:** A portfolio manager creates a new mutual fund, adds 5 securities to the portfolio, and verifies the Net Asset Value (NAV) is correctly calculated and displayed on the fund dashboard.

**What to test:**
- Fund created via UI with correct attributes
- Securities added with correct weights
- Weights must sum to 100% (validation)
- NAV calculated correctly: sum(price × units) / total_shares
- Fund performance chart renders with no JS errors
- API returns consistent NAV with UI

**Test:**
```java
@Test
@Epic("Asset Management")
@Feature("Fund Operations")
@Story("Portfolio manager creates fund and verifies NAV")
@Severity(SeverityLevel.CRITICAL)
public void testFundCreationAndNavCalculation() {
    // ── SETUP: Seed securities via API ────────────────────
    String secId1 = createSecurity("AAPL", 182.50, 500);   // $91,250
    String secId2 = createSecurity("MSFT", 415.20, 200);   // $83,040
    double expectedNav = (91250.0 + 83040.0) / 1000;       // 1000 fund shares = $174.29

    cleanup.register(() -> api.delete("/v1/securities/" + secId1));
    cleanup.register(() -> api.delete("/v1/securities/" + secId2));

    // ── UI: Create new fund ───────────────────────────────
    page.navigate(config.getBaseUrl() + "/funds/new");

    new FormFiller(page).fill(Map.of(
        "Fund Name",    TestDataNamespace.name("Growth Fund"),
        "Fund Type",    "EQUITY",
        "Total Shares", "1000",
        "Base Currency","USD",
        "Inception Date","2024-01-01"
    ));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Fund")).click();
    page.waitForURL("**/funds/*/portfolio");

    String fundId = page.url().replaceAll(".*/funds/([^/]+)/.*", "$1");
    cleanup.register(() -> api.delete("/v1/funds/" + fundId));

    // ── UI: Add securities to portfolio ───────────────────
    addSecurityToFund(secId1, 500, 55.0);  // 55% weight
    addSecurityToFund(secId2, 200, 45.0);  // 45% weight

    // ── UI: Validate weight total ─────────────────────────
    assertThat(page.locator("[data-field='total-weight']")).hasText("100.00%");
    assertThat(page.locator(".weight-warning")).not().isVisible();

    // ── UI: NAV calculated and displayed ──────────────────
    page.navigate(config.getBaseUrl() + "/funds/" + fundId);
    page.waitForLoadState(LoadState.NETWORKIDLE);
    WaitUtils.waitForSkeletonGone(page);

    String navText = page.locator("[data-field='nav']").textContent()
        .replaceAll("[^0-9.]", "");
    double actualUiNav = Double.parseDouble(navText);
    assertThat(actualUiNav).isCloseTo(expectedNav, Offset.offset(0.01));

    // ── UI: Performance chart renders without errors ───────
    NetworkAuditHelper audit = new NetworkAuditHelper(page, "**/api/**", "Authorization");
    page.locator("[data-widget='performance-chart']").waitFor();
    assertThat(page.locator("[data-widget='performance-chart'] canvas")).isVisible();
    audit.assertFullCompliance();

    // ── API: NAV consistent with UI ───────────────────────
    Map<String, Object> fundData = api.get("/v1/funds/" + fundId, Map.class);
    double apiNav = ((Number) fundData.get("nav")).doubleValue();
    assertThat(apiNav).isCloseTo(expectedNav, Offset.offset(0.01));
    assertThat(apiNav).isCloseTo(actualUiNav, Offset.offset(0.01));

    // ── API: Securities in portfolio ──────────────────────
    Map<String, Object> portfolio = api.get("/v1/funds/" + fundId + "/portfolio", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> holdings = (List<Map<String, Object>>) portfolio.get("holdings");
    assertThat(holdings).hasSize(2);
}

private void addSecurityToFund(String secId, int units, double weight) {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add Security")).click();
    page.getByLabel("Security").fill(secId);
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(secId)).click();
    page.getByLabel("Units").fill(String.valueOf(units));
    page.getByLabel("Weight (%)").fill(String.valueOf(weight));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to Portfolio")).click();
    assertThat(page.locator("[data-security='" + secId + "']")).isVisible();
}
```

---

## Scenario Q7: Approval Chain Workflow

**Business Context:** Purchase orders above $50,000 require 2-level approval: Manager (L1) then VP (L2). Tests must verify approval escalation, rejection with reason, and that the PO cannot be modified while under review.

**What to test:**
- PO > $50k triggers dual-approval workflow
- L1 approver receives notification and approves
- L2 approver receives notification after L1 approval
- PO is read-only while PENDING_APPROVAL
- L2 rejection returns PO to DRAFT with reason
- Full approval path transitions to APPROVED and creates vendor payment record

**Test:**
```java
@Test
@Feature("Purchase Orders")
@Story("High-value PO requires 2-level approval")
@Severity(SeverityLevel.CRITICAL)
public void testDualApprovalWorkflow() {
    // ── SETUP: Create PO as Analyst ───────────────────────
    switchToRole(UserRole.ANALYST);
    page.navigate(config.getBaseUrl() + "/purchase-orders/new");

    new FormFiller(page).fill(Map.of(
        "Vendor",         "Acme Supplies Co.",
        "Description",    "Q1 2025 Infrastructure Purchase",
        "Amount",         "75000",
        "Requested Date", "2025-02-01"
    ));

    APIResponse poResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/purchase-orders") && r.status() == 201,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Submit for Approval")).click()
    );
    String poId = ((Map<?, ?>) poResponse.json()).get("id").toString();
    cleanup.register(() -> api.delete("/v1/purchase-orders/" + poId));

    assertThat(page.locator(".po-status-badge")).hasText("PENDING_L1_APPROVAL");

    // ── UI: PO is read-only while under review ─────────────
    page.navigate(config.getBaseUrl() + "/purchase-orders/" + poId);
    assertThat(page.locator("[data-action='edit-po']")).not().isVisible();
    assertThat(page.locator("[data-action='delete-po']")).not().isVisible();
    assertThat(page.locator(".readonly-banner")).containsText("Under review");

    // ── API: L1 approver notification sent ────────────────
    Map<String, Object> l1Notif = awaitNotification(poId, "L1_APPROVAL_REQUIRED", 5000);
    assertThat(l1Notif.get("recipientRole")).isEqualTo("MANAGER");

    // ── ACT AS L1 APPROVER (MANAGER) ─────────────────────
    switchToRole(UserRole.MANAGER);
    page.navigate(config.getBaseUrl() + "/approvals/pending");
    assertThat(page.locator("[data-po-id='" + poId + "']")).isVisible();

    page.locator("[data-po-id='" + poId + "'] [data-action='review']").click();
    assertThat(page.locator("[data-field='amount']")).containsText("75,000");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Approve")).click();
    page.getByLabel("Approval Notes").fill("Budget approved for Q1 infrastructure.");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm Approval")).click();

    assertThat(page.locator(".success-toast")).containsText("L1 approval recorded");

    // ── API: Transitioned to PENDING_L2_APPROVAL ──────────
    Map<String, Object> poAfterL1 = api.get("/v1/purchase-orders/" + poId, Map.class);
    assertThat(poAfterL1.get("status")).isEqualTo("PENDING_L2_APPROVAL");
    assertThat(poAfterL1.get("l1ApprovedBy")).isNotNull();

    // ── ACT AS L2 APPROVER (VP) ───────────────────────────
    switchToRole(UserRole.ADMIN);  // VP role uses ADMIN in test env
    page.navigate(config.getBaseUrl() + "/approvals/pending");
    page.locator("[data-po-id='" + poId + "'] [data-action='review']").click();

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Approve")).click();
    page.getByLabel("VP Approval Notes").fill("Approved. Proceed with procurement.");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Final Approve")).click();
    page.waitForURL("**/approvals/confirmed");

    // ── API: Fully approved + vendor payment created ───────
    Map<String, Object> approvedPo = api.get("/v1/purchase-orders/" + poId, Map.class);
    assertThat(approvedPo.get("status")).isEqualTo("APPROVED");
    assertThat(approvedPo.get("l1ApprovedBy")).isNotNull();
    assertThat(approvedPo.get("l2ApprovedBy")).isNotNull();

    Map<String, Object> vendorPayment = api.get(
        "/v1/vendor-payments?poId=" + poId, Map.class);
    assertThat(vendorPayment.get("status")).isIn("SCHEDULED", "PENDING");
    assertThat(vendorPayment.get("amount")).isEqualTo(75000.0);
}

private Map<String, Object> awaitNotification(String entityId, String type, int timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
        Map<String, Object> response = api.get(
            "/v1/notifications?entityId=" + entityId + "&type=" + type, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        if (items != null && !items.isEmpty()) return items.get(0);
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    throw new AssertionFailedError("Notification not found: type=" + type + " entityId=" + entityId);
}
```

---

## Scenario Q8: Bulk Operations with Progress Tracking

**Business Context:** An admin selects 200 customer accounts for a bulk tier upgrade from STANDARD to PREMIUM. The operation runs async — progress shown via a live progress bar. Tests must validate batch processing and rollback on partial failure.

**What to test:**
- Select-all checkbox selects 200 records
- Bulk operation modal shows correct count
- Progress bar advances in real time
- API returns final status with success/failure counts
- Failed records are listed with reasons
- Partial failure rolls back successful records (transactional)

**Test:**
```java
@Test
@Feature("Bulk Operations")
@Story("Bulk tier upgrade with progress tracking")
@Severity(SeverityLevel.CRITICAL)
public void testBulkTierUpgrade() {
    // ── SETUP: Create 10 STANDARD customers via API ───────
    List<String> customerIds = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        String id = api.post("/v1/customers", Map.of(
            "name", TestDataNamespace.name("Bulk-Customer-" + i),
            "tier", "STANDARD",
            "email", "bulk-test-" + i + "-" + UUID.randomUUID().toString().substring(0, 4) + "@example.com"
        ), Map.class).get("id").toString();
        customerIds.add(id);
        cleanup.register(() -> api.delete("/v1/customers/" + id));
    }

    // ── UI: Navigate to customer list ─────────────────────
    page.navigate(config.getBaseUrl() + "/customers?tier=STANDARD");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    WaitUtils.waitForSkeletonGone(page);

    // ── UI: Select all visible records ────────────────────
    page.locator("[data-action='select-all']").click();
    int selectedCount = Integer.parseInt(
        page.locator(".selected-count").textContent().trim());
    assertThat(selectedCount).isGreaterThanOrEqualTo(10);

    // ── UI: Open bulk action menu ─────────────────────────
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Bulk Actions")).click();
    page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Upgrade Tier")).click();

    // ── UI: Bulk operation modal ──────────────────────────
    Locator modal = page.locator("[data-modal='bulk-upgrade']");
    assertThat(modal).isVisible();
    assertThat(modal.locator(".operation-count")).containsText(String.valueOf(selectedCount));

    page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("New Tier"))
        .selectOption("PREMIUM");

    // ── UI: Start bulk operation and watch progress ────────
    APIResponse batchResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/batch-operations") && r.status() == 202,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Start Upgrade")).click()
    );
    String batchId = ((Map<?, ?>) batchResponse.json()).get("batchId").toString();

    // ── UI: Progress bar increases ────────────────────────
    PerformanceCollector perf = new PerformanceCollector(page);
    Locator progressBar = page.locator("[data-batch-id='" + batchId + "'] .progress-fill");
    progressBar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

    // Poll until 100% complete (max 60s)
    page.waitForFunction(
        "selector => { const el = document.querySelector(selector); " +
        "return el && el.getAttribute('aria-valuenow') === '100'; }",
        List.of("[data-batch-id='" + batchId + "'] .progress-fill"),
        new Page.WaitForFunctionOptions().setTimeout(60_000)
    );
    assertThat(page.locator("[data-batch-id='" + batchId + "'] .batch-status"))
        .hasText(Pattern.compile("Complete|Finished", Pattern.CASE_INSENSITIVE));

    // ── API: Batch operation results ──────────────────────
    Map<String, Object> batchResult = api.get("/v1/batch-operations/" + batchId, Map.class);
    assertThat(batchResult.get("status")).isEqualTo("COMPLETED");
    assertThat((int) batchResult.get("successCount")).isGreaterThanOrEqualTo(10);
    assertThat((int) batchResult.get("failureCount")).isEqualTo(0);

    // ── API: All created customers now PREMIUM ─────────────
    for (String customerId : customerIds) {
        Map<String, Object> customer = api.get("/v1/customers/" + customerId, Map.class);
        assertThat(customer.get("tier")).isEqualTo("PREMIUM");
    }
}
```

---

## Scenario Q9: Search, Filter, Sort, and Export

**Business Context:** A compliance officer needs to export all CONFIRMED orders between two dates for a specific customer, sorted by amount descending, as a CSV file. The export must match the filtered on-screen data exactly.

**What to test:**
- Date range filter returns only records within range
- Customer filter narrows results correctly
- Sort by amount descending is applied
- CSV export downloads correctly
- CSV row count, headers, and data match UI grid exactly
- Large exports (1000+ rows) complete within SLA

**Test:**
```java
@Test
@Feature("Reporting")
@Story("Compliance officer exports filtered orders as CSV")
@Severity(SeverityLevel.NORMAL)
public void testFilteredOrderExport() {
    // ── SETUP: Create known orders via API ─────────────────
    String fromDate = "2024-10-01";
    String toDate   = "2024-12-31";
    String custId   = createTestCustomer("Export Test Corp");
    cleanup.register(() -> api.delete("/v1/customers/" + custId));

    List<String> orderIds = new ArrayList<>();
    double[] amounts = {1500.00, 3200.00, 800.00, 5100.00};
    for (double amount : amounts) {
        String orderId = api.post("/v1/orders", Map.of(
            "customerId", custId, "amount", amount,
            "status", "CONFIRMED", "orderDate", "2024-11-15"
        ), Map.class).get("id").toString();
        orderIds.add(orderId);
        cleanup.register(() -> api.delete("/v1/orders/" + orderId));
    }

    // ── UI: Apply filters ─────────────────────────────────
    page.navigate(config.getBaseUrl() + "/orders");
    new FormFiller(page).fill(Map.of(
        "From Date",  fromDate,
        "To Date",    toDate,
        "Customer ID",custId,
        "Status",     "CONFIRMED"
    ));
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply Filters")).click();
    page.waitForResponse(r -> r.url().contains("/api/v1/orders") && r.status() == 200);
    WaitUtils.waitForSkeletonGone(page);

    // ── UI: All 4 orders visible ───────────────────────────
    assertThat(page.locator("tbody tr")).hasCount(4);

    // ── UI: Sort by amount descending ─────────────────────
    page.locator("th[data-column='amount']").click(); // ascending
    page.locator("th[data-column='amount']").click(); // descending
    page.waitForResponse(r -> r.url().contains("/api/v1/orders") && r.status() == 200);

    // Verify order: 5100, 3200, 1500, 800
    List<Locator> rows = page.locator("tbody tr").all();
    double[] sortedAmounts = {5100.00, 3200.00, 1500.00, 800.00};
    for (int i = 0; i < rows.size(); i++) {
        String amountText = rows.get(i).locator("[data-column='amount']")
            .textContent().replaceAll("[^0-9.]", "");
        assertThat(Double.parseDouble(amountText))
            .isCloseTo(sortedAmounts[i], Offset.offset(0.01));
    }

    // ── UI: Export to CSV ─────────────────────────────────
    DownloadValidator download = new DownloadValidator();
    download.capture(page, () ->
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Export CSV")).click()
    );
    download.assertFileName("orders-export.*\\.csv");

    // ── CSV: Validate content matches UI ──────────────────
    List<String[]> csvRows = parseCsv(download.getPath());
    assertThat(csvRows.get(0)).containsExactly(
        "Order ID", "Customer", "Amount", "Status", "Order Date");
    assertThat(csvRows).hasSize(5); // 1 header + 4 data rows

    // CSV sorted same as UI
    for (int i = 1; i <= 4; i++) {
        double csvAmount = Double.parseDouble(csvRows.get(i)[2].replaceAll("[^0-9.]", ""));
        assertThat(csvAmount).isCloseTo(sortedAmounts[i - 1], Offset.offset(0.01));
    }
}

private List<String[]> parseCsv(Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
        return reader.lines().map(l -> l.split(",")).collect(Collectors.toList());
    }
}
```

---

## Scenario Q10: Subscription Lifecycle — Upgrade, Downgrade, Cancellation

**Business Context:** A SaaS subscription can be upgraded, downgraded, or cancelled with pro-rata billing adjustments. Cancellation adds a 30-day grace period before access is revoked.

**What to test:**
- Upgrade: correct pro-rata charge calculated and shown
- Downgrade: effective at next billing cycle, not immediate
- Cancellation: grace period set, access maintained
- Post-grace: access revoked via API status
- Reinstatement within grace period restores access

**Test:**
```java
@Test
@Feature("Subscription Management")
@Story("Subscription upgrade, then cancellation with grace period")
@Severity(SeverityLevel.CRITICAL)
public void testSubscriptionUpgradeAndCancellation() {
    // ── SETUP: Active BASIC subscription ──────────────────
    String subId = api.post("/v1/subscriptions", Map.of(
        "customerId", testCustomerId,
        "plan", "BASIC",
        "billingCycle", "MONTHLY",
        "startDate", LocalDate.now().minusDays(15).toString()
    ), Map.class).get("id").toString();
    cleanup.register(() -> api.delete("/v1/subscriptions/" + subId));

    // ── UI: View subscription details ─────────────────────
    page.navigate(config.getBaseUrl() + "/subscriptions/" + subId);
    assertThat(page.locator("[data-field='plan']")).hasText("BASIC");
    assertThat(page.locator("[data-field='status']")).hasText("ACTIVE");

    // ── UI: Upgrade to PROFESSIONAL ───────────────────────
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upgrade Plan")).click();
    page.getByTestId("plan-PROFESSIONAL").click();
    assertThat(page.locator(".pro-rata-preview")).isVisible();
    assertThat(page.locator(".pro-rata-preview")).containsText("Prorated charge");

    APIResponse upgradeResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/subscriptions/" + subId + "/upgrade"),
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Confirm Upgrade")).click()
    );
    assertThat(upgradeResponse.status()).isEqualTo(200);

    assertThat(page.locator("[data-field='plan']")).hasText("PROFESSIONAL");
    assertThat(page.locator(".success-toast")).containsText("Plan upgraded");

    // ── API: Subscription updated + pro-rata invoice ───────
    Map<String, Object> sub = api.get("/v1/subscriptions/" + subId, Map.class);
    assertThat(sub.get("plan")).isEqualTo("PROFESSIONAL");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> invoices = (List<Map<String, Object>>)
        api.get("/v1/invoices?subscriptionId=" + subId, Map.class).get("invoices");
    assertThat(invoices.stream().anyMatch(i -> "PRORATION".equals(i.get("type")))).isTrue();

    // ── UI: Cancel subscription ───────────────────────────
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel Subscription")).click();
    assertThat(page.locator(".cancellation-modal")).isVisible();
    assertThat(page.locator(".grace-period-notice")).containsText("30-day grace period");

    page.getByLabel("Reason").selectOption("SWITCHING_PROVIDERS");
    page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Confirm Cancellation")).click();

    assertThat(page.locator("[data-field='status']")).hasText("CANCELLATION_PENDING");
    assertThat(page.locator("[data-field='access-until']")).isVisible();

    // ── API: Grace period correctly set ───────────────────
    Map<String, Object> cancelledSub = api.get("/v1/subscriptions/" + subId, Map.class);
    assertThat(cancelledSub.get("status")).isEqualTo("CANCELLATION_PENDING");

    String accessUntil = cancelledSub.get("accessUntil").toString();
    LocalDate accessUntilDate = LocalDate.parse(accessUntil.substring(0, 10));
    assertThat(accessUntilDate).isBetween(
        LocalDate.now().plusDays(29),
        LocalDate.now().plusDays(31)
    );

    // ── UI: Access still active within grace period ────────
    assertThat(page.locator("[data-feature='premium-dashboard']")).isVisible();

    // ── API: Reinstate within grace period ────────────────
    APIResponse reinstateResponse = api.post(
        "/v1/subscriptions/" + subId + "/reinstate", Map.of(), Map.class
    );
    Map<String, Object> reinstatedSub = api.get("/v1/subscriptions/" + subId, Map.class);
    assertThat(reinstatedSub.get("status")).isEqualTo("ACTIVE");
    assertThat(reinstatedSub.get("accessUntil")).isNull();
}
```

---

## Scenario Q11: Real-Time Dashboard Data Accuracy

**Business Context:** An executive KPI dashboard shows live metrics: total revenue today, active orders, SLA breach count, and top 5 customers by spend — all updating via WebSocket every 30 seconds.

**What to test:**
- Dashboard loads without JS errors
- All widgets render within 3 seconds
- KPI values match API-computed totals
- WebSocket connection established and pushes updates
- Updated values reflected on screen within 5 seconds of push

**Test:**
```java
@Test
@Feature("Dashboard")
@Story("Executive dashboard KPIs match API data")
@Severity(SeverityLevel.CRITICAL)
public void testDashboardKpiAccuracy() {
    // ── SETUP: Capture expected values from API ─────────
    Map<String, Object> apiMetrics = api.get("/v1/analytics/today", Map.class);
    double expectedRevenue   = ((Number) apiMetrics.get("totalRevenue")).doubleValue();
    int    expectedActiveOrders = ((Number) apiMetrics.get("activeOrders")).intValue();
    int    expectedSlaBreach = ((Number) apiMetrics.get("slaBreach")).intValue();

    // ── SETUP: Attach WebSocket monitor ───────────────────
    WebSocketMonitor wsMonitor = new WebSocketMonitor(page);

    // ── UI: Navigate to dashboard ────────────────────────
    PerformanceCollector perf = new PerformanceCollector(page);
    perf.measure("Dashboard load", () -> {
        page.navigate(config.getBaseUrl() + "/dashboard/executive");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        WaitUtils.waitForSkeletonGone(page);
    });

    // ── UI: All widgets visible within 3s ────────────────
    perf.assertWithinSla("Dashboard load", 3000);
    assertThat(page.getByTestId("widget-revenue")).isVisible();
    assertThat(page.getByTestId("widget-active-orders")).isVisible();
    assertThat(page.getByTestId("widget-sla-breach")).isVisible();
    assertThat(page.getByTestId("widget-top-customers")).isVisible();

    // ── UI vs API: Revenue KPI ────────────────────────────
    String revenueText = page.getByTestId("widget-revenue")
        .locator(".kpi-value").textContent().replaceAll("[^0-9.]", "");
    assertThat(Double.parseDouble(revenueText))
        .isCloseTo(expectedRevenue, Offset.offset(expectedRevenue * 0.01)); // 1% tolerance

    // ── UI vs API: Active orders ───────────────────────────
    String activeOrdersText = page.getByTestId("widget-active-orders")
        .locator(".kpi-value").textContent().trim();
    assertThat(Integer.parseInt(activeOrdersText)).isEqualTo(expectedActiveOrders);

    // ── WebSocket: Connection established ─────────────────
    assertThat(wsMonitor.getConnectionCount()).isGreaterThanOrEqualTo(1);
    wsMonitor.assertNoErrorMessages();

    // ── WebSocket: Trigger update via API and verify UI ───
    // Create an order to change active order count
    String newOrderId = api.post("/v1/orders", Map.of(
        "customerId", testCustomerId, "amount", 999.99,
        "status", "ACTIVE", "items", List.of(Map.of("sku", "TEST-001", "quantity", 1))
    ), Map.class).get("id").toString();
    cleanup.register(() -> api.delete("/v1/orders/" + newOrderId));

    // Wait for WS push confirming metrics refreshed
    wsMonitor.waitForJsonMessage("type", "METRICS_UPDATED", 35_000);

    // UI active orders should increment
    String updatedOrdersText = page.getByTestId("widget-active-orders")
        .locator(".kpi-value").textContent().trim();
    assertThat(Integer.parseInt(updatedOrdersText)).isEqualTo(expectedActiveOrders + 1);

    // ── No JS console errors throughout ───────────────────
    // (captured via NetworkAuditHelper or page console listener)
}
```

---

## Scenario Q12: Concurrent Access and Optimistic Locking

**Business Context:** Two users attempt to edit the same customer record simultaneously. The system uses optimistic locking (`version` field). The second save must fail with a conflict error and prompt the user to reload.

**What to test:**
- First save succeeds, version increments
- Second save with stale version returns 409 Conflict
- UI shows "Record updated by another user — please refresh"
- After refresh, user sees the latest version
- No data from the losing save is persisted

**Test:**
```java
@Test
@Feature("Concurrency")
@Story("Optimistic locking prevents stale data overwrites")
@Severity(SeverityLevel.CRITICAL)
public void testOptimisticLockingConflict() {
    // ── SETUP: Create customer with version=1 ─────────────
    Map<String, Object> customer = api.post("/v1/customers", Map.of(
        "name", TestDataNamespace.name("Lock Test Corp"),
        "email", "locktest@example.com", "tier", "STANDARD"
    ), Map.class);
    String custId = customer.get("id").toString();
    int originalVersion = ((Number) customer.get("version")).intValue();
    cleanup.register(() -> api.delete("/v1/customers/" + custId));

    // ── USER A: Open edit form (holds version=1) ───────────
    page.navigate(config.getBaseUrl() + "/customers/" + custId + "/edit");
    assertThat(page.locator("[name='name']")).hasValue(TestDataNamespace.name("Lock Test Corp"));
    String formVersion = page.locator("[name='version']").inputValue();
    assertThat(formVersion).isEqualTo(String.valueOf(originalVersion));

    // ── USER B: Update via API (increments to version=2) ───
    api.put("/v1/customers/" + custId, Map.of(
        "name", TestDataNamespace.name("Lock Test Corp — B Updated"),
        "tier", "PREMIUM",
        "version", originalVersion  // current version
    ), Map.class);

    // Verify version incremented
    Map<String, Object> updatedByB = api.get("/v1/customers/" + custId, Map.class);
    assertThat(((Number) updatedByB.get("version")).intValue()).isEqualTo(originalVersion + 1);
    assertThat(updatedByB.get("tier")).isEqualTo("PREMIUM");

    // ── USER A: Attempts to save with stale version=1 ──────
    page.locator("[name='name']").fill(TestDataNamespace.name("Lock Test Corp — A Override"));
    page.locator("[name='tier']").selectOption("ENTERPRISE");

    APIResponse conflictResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/customers/" + custId),
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Save Changes")).click()
    );
    assertThat(conflictResponse.status()).isEqualTo(409);

    // ── UI: Conflict message shown ─────────────────────────
    assertThat(page.locator(".conflict-error")).isVisible()
        .containsText("updated by another user");
    assertThat(page.locator("[data-action='reload-latest']")).isVisible();

    // ── UI: Reload loads latest version (User B's changes) ─
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reload Latest")).click();
    assertThat(page.locator("[name='version']").inputValue())
        .isEqualTo(String.valueOf(originalVersion + 1));
    assertThat(page.locator("[name='tier']").inputValue()).isEqualTo("PREMIUM"); // User B's value

    // ── API: User A's data was NOT persisted ───────────────
    Map<String, Object> finalState = api.get("/v1/customers/" + custId, Map.class);
    assertThat(finalState.get("name").toString()).doesNotContain("A Override");
    assertThat(finalState.get("tier")).isEqualTo("PREMIUM"); // User B's change preserved
}
```

---

## Scenario Q13: API Rate Limiting Behaviour

**Business Context:** The public API enforces rate limits: 100 requests/minute per API key. Exceeding this returns 429 with a `Retry-After` header. The UI must gracefully handle 429 and poll with backoff.

**What to test:**
- UI displays a "rate limited" message on 429
- `Retry-After` header is respected (no immediate retry)
- After Retry-After window, request retries automatically
- Rate limit counter resets after 60 seconds
- API key identifies the calling client in error response

**Test:**
```java
@Test
@Feature("API Resilience")
@Story("UI handles rate limiting gracefully")
@Severity(SeverityLevel.NORMAL)
public void testRateLimitHandling() {
    ScenarioSimulator simulator = new ScenarioSimulator(page);

    // ── Simulate 429 rate limit on data load ──────────────
    simulator.addScenario("rate.limit", route ->
        route.fulfill(new Route.FulfillOptions()
            .setStatus(429)
            .setHeader("Retry-After", "5")
            .setHeader("X-RateLimit-Limit", "100")
            .setHeader("X-RateLimit-Remaining", "0")
            .setHeader("X-RateLimit-Reset", String.valueOf(
                System.currentTimeMillis() / 1000 + 5))
            .setContentType("application/json")
            .setBody("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"retryAfter\":5}")
        )
    );
    simulator.activate("rate.limit", "**/api/v1/orders*");

    // ── UI: Trigger an order list load ────────────────────
    page.navigate(config.getBaseUrl() + "/orders");

    // ── UI: Rate limit banner shown ───────────────────────
    assertThat(page.locator(".rate-limit-banner")).isVisible();
    assertThat(page.locator(".rate-limit-banner")).containsText("Too many requests");
    assertThat(page.locator(".retry-countdown")).isVisible();

    // ── UI: No immediate retry (spinner absent) ───────────
    page.waitForTimeout(1000);
    assertThat(page.locator(".loading-spinner")).not().isVisible();

    // ── Remove rate limit after simulated window ──────────
    simulator.reset();

    // ── UI: Auto-retry fires and loads data ───────────────
    // Wait for auto-retry (Retry-After=5s + buffer)
    page.waitForResponse(
        r -> r.url().contains("/api/v1/orders") && r.status() == 200,
        new Page.WaitForResponseOptions().setTimeout(10_000)
    );

    assertThat(page.locator(".rate-limit-banner")).not().isVisible();
    assertThat(page.locator("tbody tr")).hasCountGreaterThan(0);
    assertThat(page.locator(".loading-spinner")).not().isVisible();
}
```

---

## Scenario Q14: Audit Log Integrity Verification

**Business Context:** For SOX compliance, every mutation to financial records must be captured in an immutable audit log with: who made the change, what changed (before/after), when, and from which IP.

**What to test:**
- Create → audit entry with correct actor and values
- Update → audit entry shows before/after state
- Delete → audit entry records deletion with final state snapshot
- Audit log entries are immutable (cannot be deleted via API)
- Audit log pagination returns complete history in chronological order

**Test:**
```java
@Test
@Feature("Compliance")
@Story("Audit log captures full CRUD lifecycle")
@Severity(SeverityLevel.BLOCKER)
public void testAuditLogIntegrity() {
    // ── CREATE ────────────────────────────────────────────
    Map<String, Object> newCustomer = api.post("/v1/customers", Map.of(
        "name", TestDataNamespace.name("Audit Corp"),
        "tier", "STANDARD",
        "email", "audit-test@example.com"
    ), Map.class);
    String custId = newCustomer.get("id").toString();
    cleanup.register(() -> api.delete("/v1/customers/" + custId));

    // Audit: create entry exists
    List<Map<String, Object>> createEntries = getAuditEntries(custId, "CUSTOMER_CREATED");
    assertThat(createEntries).hasSize(1);
    Map<String, Object> createEntry = createEntries.get(0);
    assertThat(createEntry.get("actorEmail")).isNotNull();
    assertThat(createEntry.get("newValue")).isNotNull();
    assertThat(createEntry.get("previousValue")).isNull(); // no previous on create
    assertThat(createEntry.get("ipAddress")).matches("(\\d{1,3}\\.){3}\\d{1,3}");
    assertThat(createEntry.get("timestamp")).isNotNull();

    // ── UPDATE ────────────────────────────────────────────
    api.put("/v1/customers/" + custId, Map.of(
        "name", TestDataNamespace.name("Audit Corp — Updated"),
        "tier", "PREMIUM"
    ), Map.class);

    List<Map<String, Object>> updateEntries = getAuditEntries(custId, "CUSTOMER_UPDATED");
    assertThat(updateEntries).hasSize(1);
    Map<String, Object> upd = updateEntries.get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> prev = (Map<String, Object>) upd.get("previousValue");
    @SuppressWarnings("unchecked")
    Map<String, Object> next = (Map<String, Object>) upd.get("newValue");
    assertThat(prev.get("tier")).isEqualTo("STANDARD");
    assertThat(next.get("tier")).isEqualTo("PREMIUM");

    // ── DELETE ────────────────────────────────────────────
    api.delete("/v1/customers/" + custId);

    List<Map<String, Object>> deleteEntries = getAuditEntries(custId, "CUSTOMER_DELETED");
    assertThat(deleteEntries).hasSize(1);
    assertThat(deleteEntries.get(0).get("previousValue")).isNotNull(); // snapshot preserved
    assertThat(deleteEntries.get(0).get("newValue")).isNull();

    // ── IMMUTABILITY: Cannot delete audit entries ──────────
    String entryId = createEntry.get("id").toString();
    APIResponse deleteAuditResponse = page.request().delete(
        config.getApiBaseUrl() + "/v1/audit-log/" + entryId
    );
    assertThat(deleteAuditResponse.status()).isEqualTo(405); // Method Not Allowed

    // ── COMPLETENESS: All 3 entries in chronological order ─
    CursorPaginator paginator = new CursorPaginator(page.request());
    List<Map<String, Object>> allEntries = paginator.collectAll(
        config.getApiBaseUrl() + "/v1/audit-log?entityId=" + custId,
        (Class<Map<String, Object>>) (Class<?>) Map.class,
        "entries", "next_cursor"
    );
    assertThat(allEntries).hasSize(3);
    assertThat(allEntries.get(0).get("action")).isEqualTo("CUSTOMER_CREATED");
    assertThat(allEntries.get(1).get("action")).isEqualTo("CUSTOMER_UPDATED");
    assertThat(allEntries.get(2).get("action")).isEqualTo("CUSTOMER_DELETED");
}

private List<Map<String, Object>> getAuditEntries(String entityId, String action) {
    Map<String, Object> result = api.get(
        "/v1/audit-log?entityId=" + entityId + "&action=" + action, Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
    return entries != null ? entries : Collections.emptyList();
}
```

---

## Scenario Q15: Multi-Tab and Multi-Window Workflow

**Business Context:** A broker opens a customer profile in a main tab, opens the transaction history in a new tab, modifies the customer profile in the first tab, and expects the second tab to reflect the update on refresh.

**What to test:**
- Customer profile opens in main tab
- "Open in new tab" creates a second tab
- Focus management between tabs
- Data updated in tab 1 is consistent in tab 2 after refresh
- Closing tab 2 returns focus to tab 1

**Test:**
```java
@Test
@Feature("Multi-Tab Workflow")
@Story("Changes in tab 1 reflected in tab 2 after refresh")
@Severity(SeverityLevel.NORMAL)
public void testMultiTabDataConsistency() {
    // ── SETUP ─────────────────────────────────────────────
    String custId = createTestCustomer("Multi-Tab Corp");
    cleanup.register(() -> api.delete("/v1/customers/" + custId));

    // ── TAB 1: Open customer profile ──────────────────────
    page.navigate(config.getBaseUrl() + "/customers/" + custId);
    assertThat(page.locator("h1")).containsText("Multi-Tab Corp");

    // ── Open transaction history in new tab ───────────────
    TabManager tabManager = new TabManager(context);
    Page tab2 = tabManager.openInNewTab(page,
        page.locator("[data-action='view-transactions']"));

    assertThat(tabManager.getTabCount()).isEqualTo(2);
    assertThat(tab2.url()).contains("/customers/" + custId + "/transactions");
    assertThat(tab2.locator(".customer-name-header")).containsText("Multi-Tab Corp");

    // ── TAB 1: Update customer name ───────────────────────
    tabManager.switchToTab(0); // back to tab 1
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit")).click();
    page.getByLabel("Company Name").fill("Multi-Tab Corp — RENAMED");
    page.waitForResponse(
        r -> r.url().contains("/api/v1/customers/" + custId) && r.status() == 200,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Save")).click()
    );
    assertThat(page.locator("h1")).containsText("RENAMED");

    // ── TAB 2: Before refresh — old name still shown ───────
    tabManager.switchToTab(1);
    assertThat(tab2.locator(".customer-name-header")).containsText("Multi-Tab Corp");
    assertThat(tab2.locator(".customer-name-header")).not().containsText("RENAMED");

    // ── TAB 2: After refresh — shows updated name ──────────
    tab2.reload();
    tab2.waitForLoadState(LoadState.DOMCONTENTLOADED);
    assertThat(tab2.locator(".customer-name-header")).containsText("Multi-Tab Corp — RENAMED");

    // ── Close tab 2 and verify focus returns ──────────────
    tabManager.closeTab(tab2);
    assertThat(tabManager.getTabCount()).isEqualTo(1);
    assertThat(page.locator("h1")).containsText("RENAMED"); // tab 1 still intact
}
```

---

## Scenario Q16: Email Notification Verification via API

**Business Context:** Transactional emails (order confirmation, password reset, payment receipt) are sent asynchronously via a mail service. Tests must verify the correct email was sent with the correct content and within SLA.

**What to test:**
- Email queued immediately after triggering action
- Email delivered within 30 seconds
- Subject line, recipient, and body content are correct
- Attachments (PDF invoice) included for payment emails
- Password reset link contains valid token with 24h expiry

**Test:**
```java
@Test
@Feature("Notifications")
@Story("Order confirmation email sent with correct content")
@Severity(SeverityLevel.CRITICAL)
public void testOrderConfirmationEmail() {
    String recipientEmail = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

    // ── SETUP: Create customer with controlled email ───────
    String custId = api.post("/v1/customers", Map.of(
        "name", "Email Test Corp", "tier", "STANDARD", "email", recipientEmail
    ), Map.class).get("id").toString();
    cleanup.register(() -> api.delete("/v1/customers/" + custId));

    // ── Place order via UI to trigger confirmation email ───
    page.navigate(config.getBaseUrl() + "/orders/new");
    new FormFiller(page).fill(Map.of("Customer ID", custId, "Product SKU", "PROD-001"));

    APIResponse orderResp = page.waitForResponse(
        r -> r.url().contains("/api/v1/orders") && r.status() == 201,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Place Order")).click()
    );
    String orderId = ((Map<?, ?>) orderResp.json()).get("id").toString();
    cleanup.register(() -> api.delete("/v1/orders/" + orderId));

    // ── API: Poll email service for delivery (max 30s) ────
    Map<String, Object> email = awaitEmailDelivery(recipientEmail, "ORDER_CONFIRMATION", 30_000);
    assertThat(email.get("status")).isEqualTo("DELIVERED");

    // ── Validate email content ────────────────────────────
    assertThat(email.get("to")).isEqualTo(recipientEmail);
    assertThat(email.get("subject").toString()).contains(orderId).contains("Order Confirmed");

    @SuppressWarnings("unchecked")
    Map<String, Object> emailBody = (Map<String, Object>) email.get("body");
    assertThat(emailBody.get("html").toString()).contains(orderId);
    assertThat(emailBody.get("html").toString()).contains("PROD-001");
    assertThat(emailBody.get("html").toString()).doesNotContain("{{"); // no template vars

    // ── Validate PDF invoice attachment ───────────────────
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> attachments = (List<Map<String, Object>>) email.get("attachments");
    assertThat(attachments).hasSize(1);
    assertThat(attachments.get(0).get("filename").toString()).matches("invoice-.*\\.pdf");
    assertThat(attachments.get(0).get("contentType")).isEqualTo("application/pdf");
    int attachmentSize = ((Number) attachments.get(0).get("sizeBytes")).intValue();
    assertThat(attachmentSize).isBetween(5000, 2_000_000);
}

private Map<String, Object> awaitEmailDelivery(String email, String type, int timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
        Map<String, Object> result = api.get(
            "/v1/email-events?recipient=" + email + "&type=" + type, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
        if (events != null && !events.isEmpty() &&
            "DELIVERED".equals(events.get(0).get("status"))) {
            return events.get(0);
        }
        try { Thread.sleep(1000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    throw new AssertionFailedError("Email not delivered within " + timeoutMs + "ms: " + email);
}
```

---

## Scenario Q17: File Processing Pipeline Validation

**Business Context:** A bulk data import accepts a CSV of 500 customer records. The file is uploaded, processed asynchronously, and results made available via a job status API. Invalid rows are rejected with reasons.

**What to test:**
- CSV with valid and invalid rows is uploaded
- Job status transitions: QUEUED → PROCESSING → COMPLETED
- Successful rows are importable and accessible via API
- Invalid rows appear in an error report
- Error report is downloadable as CSV
- Duplicate emails are detected and flagged

**Test:**
```java
@Test
@Feature("Data Import")
@Story("CSV bulk import with mixed valid and invalid rows")
@Severity(SeverityLevel.CRITICAL)
public void testBulkCustomerImport() {
    // ── SETUP: Generate test CSV ───────────────────────────
    Path csvPath = generateTestCsv(8, 2); // 8 valid + 2 invalid rows

    // ── UI: Upload CSV ────────────────────────────────────
    page.navigate(config.getBaseUrl() + "/imports/customers");
    page.locator("input[type='file']").setInputFiles(csvPath);
    assertThat(page.locator(".file-preview")).containsText("10 rows detected");

    APIResponse uploadResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/import-jobs") && r.status() == 202,
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Start Import")).click()
    );
    String jobId = ((Map<?, ?>) uploadResponse.json()).get("jobId").toString();

    // ── UI: Status transitions visible ────────────────────
    assertThat(page.locator("[data-job-id='" + jobId + "'] .job-status"))
        .hasText(Pattern.compile("QUEUED|PROCESSING"));

    // ── API: Poll until complete (max 60s) ────────────────
    Map<String, Object> jobResult = awaitJobCompletion(jobId, 60_000);

    assertThat(jobResult.get("status")).isEqualTo("COMPLETED");
    assertThat(jobResult.get("successCount")).isEqualTo(8);
    assertThat(jobResult.get("failureCount")).isEqualTo(2);

    // ── UI: Results displayed ─────────────────────────────
    page.reload();
    assertThat(page.locator("[data-job-id='" + jobId + "'] .success-count")).hasText("8");
    assertThat(page.locator("[data-job-id='" + jobId + "'] .failure-count")).hasText("2");

    // ── Download error report ─────────────────────────────
    DownloadValidator download = new DownloadValidator();
    download.capture(page, () ->
        page.locator("[data-job-id='" + jobId + "'] [data-action='download-errors']").click()
    );
    download.assertFileName("import-errors-" + jobId + "\\.csv");

    // ── API: Valid customers imported ─────────────────────
    Map<String, Object> searchResult = api.get(
        "/v1/customers?importJobId=" + jobId + "&status=ACTIVE", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> imported = (List<Map<String, Object>>) searchResult.get("customers");
    assertThat(imported).hasSize(8);
    imported.forEach(c -> cleanup.register(() -> api.delete("/v1/customers/" + c.get("id"))));
}

private Map<String, Object> awaitJobCompletion(String jobId, int timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
        Map<String, Object> job = api.get("/v1/import-jobs/" + jobId, Map.class);
        if ("COMPLETED".equals(job.get("status")) || "FAILED".equals(job.get("status"))) {
            return job;
        }
        try { Thread.sleep(2000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    throw new AssertionFailedError("Import job did not complete in " + timeoutMs + "ms");
}
```

---

## Scenario Q18: Accessibility Compliance Verification

**Business Context:** The product must comply with WCAG 2.1 AA. Key user journeys (login, checkout, dashboard) must be keyboard-navigable, screen reader compatible, and pass automated accessibility checks.

**What to test:**
- Axe-core accessibility scan on all key pages (zero critical violations)
- Complete login form using keyboard only (no mouse)
- All form inputs have associated labels
- Error messages are announced via `aria-live`
- Focus order follows logical DOM order
- Color contrast passes WCAG AA (via axe)

**Test:**
```java
@Test
@Tag("accessibility")
@Feature("Accessibility")
@Story("Login page passes WCAG 2.1 AA compliance")
@Severity(SeverityLevel.CRITICAL)
public void testLoginPageAccessibility() {
    page.navigate(config.getBaseUrl() + "/login");
    page.waitForLoadState(LoadState.DOMCONTENTLOADED);

    // ── Keyboard-only login flow ───────────────────────────
    // Tab to email field
    page.keyboard().press("Tab");
    assertThat(page.locator(":focus")).hasAttribute("name", "email");

    page.keyboard().type("admin@example.com");
    page.keyboard().press("Tab");
    assertThat(page.locator(":focus")).hasAttribute("type", "password");

    page.keyboard().type(config.getSecret("ADMIN_PASSWORD"));
    page.keyboard().press("Tab");
    assertThat(page.locator(":focus")).hasAttribute("type", "submit");
    page.keyboard().press("Enter");
    page.waitForURL("**/dashboard");

    // ── Axe accessibility scan ────────────────────────────
    // Inject axe-core and run analysis
    page.navigate(config.getBaseUrl() + "/login");
    page.addScriptTag(new Page.AddScriptTagOptions()
        .setUrl("https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.9.0/axe.min.js"));

    @SuppressWarnings("unchecked")
    Map<String, Object> axeResult = (Map<String, Object>) page.evaluate("""
        async () => {
            const result = await axe.run(document, {
                runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa'] }
            });
            return {
                violations: result.violations.map(v => ({
                    id: v.id, impact: v.impact, description: v.description,
                    nodes: v.nodes.length
                })),
                passes: result.passes.length,
                incomplete: result.incomplete.length
            };
        }
    """);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> violations = (List<Map<String, Object>>) axeResult.get("violations");

    // Filter: only critical and serious violations fail the test
    List<Map<String, Object>> blockers = violations.stream()
        .filter(v -> "critical".equals(v.get("impact")) || "serious".equals(v.get("impact")))
        .collect(Collectors.toList());

    if (!blockers.isEmpty()) {
        String report = blockers.stream()
            .map(v -> String.format("  [%s] %s (%d nodes): %s",
                v.get("impact"), v.get("id"), v.get("nodes"), v.get("description")))
            .collect(Collectors.joining("\n"));
        Allure.addAttachment("Accessibility Violations", report);
        fail("WCAG 2.1 AA violations found:\n" + report);
    }

    int passCount = ((Number) axeResult.get("passes")).intValue();
    assertThat(passCount).isGreaterThan(0);
    Allure.addAttachment("Axe Summary",
        String.format("Passes: %d | Violations: %d | Incomplete: %d",
            passCount, violations.size(), ((Number) axeResult.get("incomplete")).intValue()));
}
```

---

## Scenario Q19: Performance Benchmark Across Key Journeys

**Business Context:** SLA requirements mandate: login < 3s, order list load < 2s, checkout page < 4s, PDF download < 10s. These must be measured and asserted automatically in CI on every regression run.

**What to test:**
- Navigation Timing API for page load times
- Custom timing for specific user actions
- All timings compared against defined SLA map
- Slow test fails and attaches timing report to Allure
- Trends tracked over successive CI runs

**Test:**
```java
@Test
@Tag("performance")
@Feature("Performance")
@Story("Key user journeys meet SLA requirements")
@Severity(SeverityLevel.CRITICAL)
public void testKeyJourneySlas() {
    PerformanceCollector perf = new PerformanceCollector(page);

    // ── Journey 1: Login ───────────────────────────────────
    perf.measure("Login to Dashboard", () -> {
        page.navigate(config.getBaseUrl() + "/login");
        page.getByLabel("Email").fill("admin@example.com");
        page.getByLabel("Password").fill(config.getSecret("ADMIN_PASSWORD"));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        page.waitForURL("**/dashboard");
        WaitUtils.waitForSkeletonGone(page);
    });

    // ── Journey 2: Order list load ─────────────────────────
    perf.measure("Order List Load", () -> {
        page.navigate(config.getBaseUrl() + "/orders");
        page.waitForResponse(r -> r.url().contains("/api/v1/orders") && r.status() == 200);
        WaitUtils.waitForSkeletonGone(page);
        assertThat(page.locator("tbody tr")).hasCountGreaterThan(0);
    });

    // ── Journey 3: Checkout page render ───────────────────
    perf.measure("Checkout Page Render", () -> {
        page.navigate(config.getBaseUrl() + "/checkout");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.locator(".checkout-form")).isVisible();
    });

    // ── Journey 4: PDF download ───────────────────────────
    String orderId = getFirstAvailableOrderId();
    perf.measure("Order PDF Download", () -> {
        page.navigate(config.getBaseUrl() + "/orders/" + orderId);
        Download download = page.waitForDownload(
            () -> page.locator("[data-action='download-pdf']").click()
        );
        // Wait for download to complete
        Path path = download.path();
        assertThat(Files.size(path)).isGreaterThan(1000);
    });

    // ── SLA Assertions ────────────────────────────────────
    Map<String, Long> slas = Map.of(
        "Login to Dashboard",    3000L,
        "Order List Load",       2000L,
        "Checkout Page Render",  4000L,
        "Order PDF Download",   10_000L
    );
    perf.assertAllWithinSla(slas);

    // ── Attach timing report to Allure ────────────────────
    String report = perf.getAllTimings().entrySet().stream()
        .map(e -> String.format("%-40s %5dms  SLA: %5dms  %s",
            e.getKey(), e.getValue(), slas.get(e.getKey()),
            e.getValue() <= slas.get(e.getKey()) ? "PASS" : "FAIL"))
        .collect(Collectors.joining("\n"));
    Allure.addAttachment("Performance Report", report);
}
```

---

## Scenario Q20: Complete FinTech Regression Scenario — Trade Order Lifecycle

**Business Context:** A retail broker places a stock trade order: validate instrument, set limit price, confirm risk checks, route to exchange, receive fill confirmation, and verify portfolio position updated. Tests span UI → API → order management system.

**What to test:**
- Instrument search and selection via UI
- Limit order form validation (price, quantity, account balance check)
- Risk check passes (position limit not exceeded)
- Order routing to exchange confirmed via API
- Fill confirmation received and position updated
- Trade confirmation PDF downloadable
- Order visible in trades history with correct status

**Test:**
```java
@Test
@Epic("Trading Platform")
@Feature("Order Management")
@Story("Broker places limit buy order — full lifecycle")
@Severity(SeverityLevel.BLOCKER)
public void testTradeLimitOrderLifecycle() {
    String accountId = "ACC-BROKER-001";
    String symbol = "AAPL";
    int quantity = 10;
    double limitPrice = 182.50;

    // ── SETUP: Verify account balance via API ──────────────
    Map<String, Object> account = api.get("/v1/accounts/" + accountId, Map.class);
    double availableCash = ((Number) account.get("cashBalance")).doubleValue();
    double requiredCapital = quantity * limitPrice * 1.05; // 5% margin buffer
    assertThat(availableCash).isGreaterThan(requiredCapital);

    // ── UI: Search and select instrument ──────────────────
    page.navigate(config.getBaseUrl() + "/trading");
    page.getByPlaceholder("Search instruments...").fill(symbol);
    page.waitForResponse(r -> r.url().contains("/api/v1/instruments") && r.status() == 200);
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("AAPL — Apple Inc."))
        .click();

    assertThat(page.locator("[data-field='instrument-name']")).containsText("Apple Inc.");
    assertThat(page.locator("[data-field='last-price']")).isVisible();

    // ── UI: Set order parameters ───────────────────────────
    page.locator("[data-field='order-type']").selectOption("LIMIT");
    page.getByLabel("Quantity").fill(String.valueOf(quantity));
    page.getByLabel("Limit Price").fill(String.valueOf(limitPrice));
    page.locator("[data-field='order-direction']").getByText("BUY").click();

    // ── UI: Order summary preview ─────────────────────────
    assertThat(page.locator("[data-field='total-value-preview']"))
        .containsText(String.valueOf(quantity * limitPrice));

    // ── UI: Risk check indicator ───────────────────────────
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Check Risk")).click();
    page.waitForResponse(r -> r.url().contains("/api/v1/risk-checks") && r.status() == 200);
    assertThat(page.locator(".risk-check-badge")).hasText("PASSED");
    assertThat(page.locator(".risk-check-badge")).hasClass(Pattern.compile(".*success.*"));

    // ── UI: Place order ───────────────────────────────────
    APIResponse orderResponse = page.waitForResponse(
        r -> r.url().contains("/api/v1/trade-orders") && r.status() == 201,
        () -> {
            page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Place Order")).click();
            page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Confirm")).click();
        }
    );
    Map<String, Object> orderBody = (Map<String, Object>) orderResponse.json();
    String orderId = orderBody.get("orderId").toString();
    assertThat(orderBody.get("status")).isEqualTo("PENDING");
    cleanup.register(() -> api.delete("/v1/trade-orders/" + orderId));

    // ── UI: Order confirmation page ────────────────────────
    page.waitForURL("**/trades/confirmed/**");
    assertThat(page.locator("[data-field='order-id']")).containsText(orderId);
    assertThat(page.locator("[data-field='status']")).hasText("PENDING");

    // ── API: Simulate exchange fill (test environment) ────
    api.post("/v1/trade-orders/" + orderId + "/simulate-fill", Map.of(
        "fillPrice", 181.95, "fillQuantity", quantity, "exchangeRefId", "EXCH-999-TEST"
    ), Map.class);

    // ── API: Order status = FILLED ─────────────────────────
    // Poll with retry — fill processing is async
    Map<String, Object> filledOrder = RetryUtil.execute(10, Duration.ofMillis(500), () -> {
        Map<String, Object> o = api.get("/v1/trade-orders/" + orderId, Map.class);
        if (!"FILLED".equals(o.get("status"))) throw new RuntimeException("Not filled yet");
        return o;
    });
    assertThat(filledOrder.get("fillPrice")).isNotNull();
    assertThat(filledOrder.get("exchangeRefId")).isEqualTo("EXCH-999-TEST");

    // ── UI: Status updated to FILLED ──────────────────────
    page.reload();
    assertThat(page.locator("[data-field='status']")).hasText("FILLED");
    assertThat(page.locator("[data-field='fill-price']")).isVisible();

    // ── API: Portfolio position updated ───────────────────
    Map<String, Object> positions = api.get(
        "/v1/accounts/" + accountId + "/positions?symbol=" + symbol, Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> posList = (List<Map<String, Object>>) positions.get("positions");
    Map<String, Object> applePosition = posList.stream()
        .filter(p -> symbol.equals(p.get("symbol"))).findFirst().orElseThrow();
    int positionQty = ((Number) applePosition.get("quantity")).intValue();
    assertThat(positionQty).isGreaterThanOrEqualTo(quantity); // may have existing position

    // ── API: Account cash balance reduced ─────────────────
    Map<String, Object> updatedAccount = api.get("/v1/accounts/" + accountId, Map.class);
    double newBalance = ((Number) updatedAccount.get("cashBalance")).doubleValue();
    double filledValue = ((Number) filledOrder.get("fillPrice")).doubleValue() * quantity;
    assertThat(newBalance).isCloseTo(availableCash - filledValue, Offset.offset(1.0));

    // ── UI: Download trade confirmation PDF ───────────────
    DownloadValidator download = new DownloadValidator();
    download.capture(page, () ->
        page.locator("[data-action='download-confirmation']").click()
    );
    download.assertFileName("trade-confirmation-" + orderId + "\\.pdf");
    download.assertIsPdf();
    download.assertFileSizeBetween(5_000, 1_000_000);

    // ── UI: Trade visible in trade history ────────────────
    page.navigate(config.getBaseUrl() + "/trading/history");
    DataTableComponent tradesTable = DataTableComponent.byTestId(page, "trades-history-table");
    DataTableComponent.Row tradeRow = tradesTable.findRow("Order ID", orderId);
    assertThat(tradeRow.getCellValue("Status")).isEqualTo("FILLED");
    assertThat(tradeRow.getCellValue("Symbol")).isEqualTo(symbol);
    assertThat(tradeRow.getCellValue("Quantity")).isEqualTo(String.valueOf(quantity));

    // ── Audit log: Trade lifecycle recorded ───────────────
    List<Map<String, Object>> auditEntries = getAuditEntries(orderId, "TRADE_PLACED");
    assertThat(auditEntries).hasSize(1);
    List<Map<String, Object>> fillEntries = getAuditEntries(orderId, "TRADE_FILLED");
    assertThat(fillEntries).hasSize(1);
}
```

---

*— End of UI+API Scenarios Q1–Q20 | Section 4 Complete (20/20) —*

---

# GUIDE COMPLETE

| Section | Questions | Status |
|---|---|---|
| Section 1 — Theory | 50 | ✅ Complete |
| Section 2 — Coding / Hands-On | 30 | ✅ Complete |
| Section 3 — Architecture & Design | 20 | ✅ Complete |
| Section 4 — UI + API Scenarios | 20 | ✅ Complete |
| **Total** | **120** | **✅ All Done** |

---
*Playwright Java Interview Preparation Guide — Senior SDET / Automation Architect Level*
