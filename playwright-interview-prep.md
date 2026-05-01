# Playwright + JavaScript/TypeScript Interview Preparation Guide

> Target: Senior QA Automation Engineer / SDET (6–10 years experience)  
> Focus: UI Automation · API Automation · Enterprise Frameworks · FinTech / SaaS / Product Companies

---

# SECTION 1 — THEORY INTERVIEW QUESTIONS (50)

---

## Q1: What is Playwright's architecture and how does it communicate with browsers?

**Answer:**
- Playwright uses the **Chrome DevTools Protocol (CDP)** for Chromium, and its own protocol wrappers for Firefox and WebKit.
- It communicates with browsers over **WebSocket connections**, sending JSON-based protocol messages.
- Each browser instance runs as a separate OS process, giving Playwright true process-level isolation.
- The Node.js (or Python/Java/.NET) client acts as the orchestrator; the browser is the remote execution engine.

**Explanation:**
- Unlike Selenium's HTTP-based WebDriver protocol, Playwright's persistent WebSocket connection removes per-command HTTP overhead, making it significantly faster.
- Playwright ships its own patched browser binaries to ensure consistent behavior across versions.
- This architecture enables features like network interception, CDP event access, and service worker control that are impossible (or unreliable) with WebDriver.

**JavaScript / TypeScript Example:**
```typescript
import { chromium, Browser, BrowserContext, Page } from '@playwright/test';

async function launchWithCDPAccess(): Promise<void> {
  const browser: Browser = await chromium.launch({ headless: true });
  const context: BrowserContext = await browser.newContext();
  const page: Page = await context.newPage();

  // Direct CDP session access
  const client = await context.newCDPSession(page);
  await client.send('Network.enable');
  client.on('Network.requestWillBeSent', (event) => {
    console.log('Request:', event.request.url);
  });

  await page.goto('https://example.com');
  await browser.close();
}
```

**Real-world Usage:**
- Used in enterprise frameworks to intercept and log all outgoing API calls during UI tests for correlation.
- CDP access is used in FinTech apps to intercept WebSocket trading messages during E2E tests.

**Common Mistakes:**
- Assuming Playwright uses WebDriver under the hood (it does not).
- Forgetting that Firefox and WebKit use their own protocol bridges — CDPSession is Chromium-only.
- Not closing CDPSession/context/browser — causes resource leaks in long test runs.

**Optimization Tip:**
- Reuse browser instances across tests using `globalSetup`; only create new contexts per test (not new browsers) to reduce startup latency by ~80%.

**Debugging Strategy:**
- Enable `DEBUG=pw:protocol` env variable to inspect raw WebSocket protocol messages.
- Use `--trace on` in `playwright.config.ts` to record full protocol trace for failure analysis.

**Tricky Follow-up Questions:**
1. *Why does Playwright perform faster than Selenium even for the same operations?*
2. *What are the limitations of using CDP directly, and when would you avoid it?*

**Compare — Playwright vs Selenium vs Cypress:**
| | Playwright | Selenium | Cypress |
|---|---|---|---|
| Protocol | CDP + custom | WebDriver (HTTP) | CDP (Chromium only) |
| Browser support | Chromium, Firefox, WebKit | All | Chromium-based only |
| Speed | Fastest | Slowest | Fast |
| Network control | Full | Limited | Limited |

---

## Q2: What is a BrowserContext and why is it critical for test isolation?

**Answer:**
- A `BrowserContext` is an isolated browser session — equivalent to an **incognito window** with its own cookies, localStorage, sessionStorage, and cache.
- Multiple contexts can run **in parallel within the same browser process**, enabling high-throughput parallel execution without spawning multiple browsers.
- Each Playwright test gets its own context by default via the built-in `test` fixture.

**Explanation:**
- Contexts provide true isolation without OS-level process overhead.
- They prevent test data bleed — a login session from Test A cannot leak into Test B.
- In enterprise frameworks, contexts are used to simulate multiple concurrent users against the same app in a single test run.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, BrowserContext, Page } from '@playwright/test';

// Fixture: create isolated context per test
test.beforeEach(async ({ context }) => {
  // context is automatically isolated per test by Playwright Test runner
});

// Simulating two concurrent users in one test
test('concurrent user isolation', async ({ browser }) => {
  const adminContext: BrowserContext = await browser.newContext();
  const userContext: BrowserContext = await browser.newContext();

  const adminPage: Page = await adminContext.newPage();
  const userPage: Page = await userContext.newPage();

  await adminPage.goto('/admin/dashboard');
  await userPage.goto('/user/dashboard');

  // Both run concurrently without session bleed
  await Promise.all([
    expect(adminPage.locator('h1')).toHaveText('Admin Dashboard'),
    expect(userPage.locator('h1')).toHaveText('User Dashboard'),
  ]);

  await adminContext.close();
  await userContext.close();
});
```

**Real-world Usage:**
- FinTech platforms use dual-context tests to validate that a broker's actions are reflected in the client portal simultaneously.
- SaaS multi-tenant tests use separate contexts per tenant to verify data isolation.

**Common Mistakes:**
- Creating a new `Browser` per test instead of a new `BrowserContext` — 10x slower and wastes resources.
- Not closing contexts after use — causes memory leaks in long-running CI pipelines.
- Sharing context between tests using module-level variables — breaks isolation.

**Optimization Tip:**
- Use `storageState` on a context to inject a pre-authenticated session, eliminating UI login steps.
  ```typescript
  const context = await browser.newContext({ storageState: 'auth/admin.json' });
  ```

**Debugging Strategy:**
- If tests intermittently share state, verify no context is shared across test files. Run with `--workers=1` to isolate the issue.
- Check `playwright-report/trace/` for context lifecycle events.

**Tricky Follow-up Questions:**
1. *How does Playwright's context isolation differ from Cypress's lack of true multi-tab/multi-user support?*
2. *What happens to network interception rules when you create a new page inside an existing context?*

**Compare:**
- Selenium has no equivalent native isolation primitive — you must manage cookies/sessions manually.
- Cypress cannot natively run multiple browser sessions in a single test.

---

## Q3: Explain Playwright's auto-waiting mechanism. How does it work internally?

**Answer:**
- Playwright **automatically waits** for elements to satisfy a set of **actionability conditions** before performing any action (click, fill, check, etc.).
- Actionability checks include: visible, stable, enabled, editable, and receives events.
- This eliminates the need for manual `waitForSelector`, `sleep`, or `waitForTimeout` calls in most scenarios.

**Explanation:**
- Internally, Playwright polls the DOM at the browser level using injected JavaScript.
- For each action, it runs a series of checks in a tight loop until all conditions pass or the timeout expires.
- The timeout is configurable globally (`actionTimeout`) or per-action.
- Auto-waiting is why Playwright tests are inherently more reliable than Selenium tests using explicit sleeps.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('auto-wait demonstration', async ({ page }) => {
  await page.goto('/checkout');

  // Playwright waits for button to be: visible + enabled + stable + receives events
  await page.getByRole('button', { name: 'Place Order' }).click();

  // Auto-waits for the confirmation message to appear
  await expect(page.getByText('Order confirmed')).toBeVisible();
});

// Overriding timeout for a slow element
test('custom timeout', async ({ page }) => {
  await page.goto('/reports');
  
  await page.getByRole('button', { name: 'Generate Report' }).click();
  
  // Give report generation 30 seconds
  await expect(page.getByTestId('report-table')).toBeVisible({ timeout: 30_000 });
});
```

**Real-world Usage:**
- In enterprise apps with heavy SSR or lazy-loaded components, auto-waiting prevents flaky failures caused by elements rendering after initial DOM load.
- FinTech dashboards with real-time data streams benefit from auto-waiting on data-populated elements.

**Common Mistakes:**
- Adding `page.waitForTimeout(2000)` when auto-waiting would handle it — creates slow, brittle tests.
- Using `waitForSelector` unnecessarily — the Locator API with auto-wait is the modern approach.
- Setting global `actionTimeout: 0` (disabling timeout) — dangerous in CI as tests hang indefinitely.

**Optimization Tip:**
- Set a tight global `actionTimeout` (e.g., 10s) in `playwright.config.ts` and override only where genuinely needed. This surfaces slow elements early.
  ```typescript
  use: { actionTimeout: 10_000 }
  ```

**Debugging Strategy:**
- When auto-wait times out, the error shows the last element state. Use `--debug` mode to pause and inspect.
- Enable `page.on('console', ...)` to catch JS errors that may prevent elements from becoming actionable.

**Tricky Follow-up Questions:**
1. *What is the difference between `actionTimeout`, `navigationTimeout`, and `expect` timeout? Which overrides which?*
2. *Can auto-waiting cause a test to pass when it should fail? Give an example.*

**Compare:**
- Selenium requires explicit waits (`WebDriverWait`, `ExpectedConditions`) — verbose and error-prone.
- Cypress has a retry-ability mechanism on assertions but lacks full actionability checks (e.g., no stability check).

---

## Q4: What is the Locator API and how does it differ from `page.$()` / `ElementHandle`?

**Answer:**
- A `Locator` is a **lazy, re-queried reference** to a DOM element. It does not resolve to an element until an action is performed on it.
- `ElementHandle` (from `page.$()`) captures a live reference to a specific DOM node at a point in time — if the DOM re-renders, the handle becomes stale.
- Locators are the **preferred modern API**; `ElementHandle` is considered legacy.

**Explanation:**
- Because Locators re-query on every action, they naturally handle dynamic DOM updates (e.g., React re-renders, list updates) without stale element exceptions.
- Locators also carry auto-waiting semantics built in, whereas ElementHandle actions may not retry.
- Strict mode (enforced by default on Locators) throws if a locator matches multiple elements, preventing silent ambiguity bugs.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, Locator } from '@playwright/test';

test('locator vs elementhandle', async ({ page }) => {
  await page.goto('/products');

  // ✅ CORRECT: Locator — lazy, re-queried, strict
  const addToCartBtn: Locator = page.getByRole('button', { name: 'Add to Cart' });
  await addToCartBtn.click();

  // ❌ LEGACY: ElementHandle — stale if DOM updates
  // const btn = await page.$('button[data-action="add-cart"]');
  // await btn?.click(); // may be stale after React re-render

  // Locator with filtering
  const productCard: Locator = page
    .getByTestId('product-card')
    .filter({ hasText: 'MacBook Pro' });

  await expect(productCard).toBeVisible();
  await productCard.getByRole('button', { name: 'Add to Cart' }).click();
});
```

**Real-world Usage:**
- In React/Vue SPAs with frequent re-renders, Locators prevent the `StaleElementReferenceException` equivalent that plagues Selenium-based frameworks.
- Component-level Locators are used in design system test libraries to encapsulate element queries.

**Common Mistakes:**
- Using `page.$()` in new test code — it's deprecated behavior.
- Storing a Locator and reusing it after a full page navigation — the underlying frame may have changed.
- Not using `filter()` on lists — leads to strict mode violations when multiple matches exist.

**Optimization Tip:**
- Build a page object layer using Locator properties (not methods) so they're evaluated lazily:
  ```typescript
  class LoginPage {
    readonly emailInput: Locator;
    constructor(private page: Page) {
      this.emailInput = page.getByLabel('Email');
    }
  }
  ```

**Debugging Strategy:**
- Use `await locator.count()` to debug how many elements a locator matches before acting.
- Use `playwright codegen` to auto-generate locators from live interaction.

**Tricky Follow-up Questions:**
1. *What does Playwright's strict mode mean for Locators, and how do you intentionally target the nth element?*
2. *How do Locators behave differently inside frames vs the main frame?*

**Compare:**
- Selenium's `WebElement` is equivalent to `ElementHandle` — stale element exceptions are a chronic issue.
- Cypress chains commands on subjects but lacks the explicit re-query guarantee Playwright's Locators provide.

---

## Q5: What is Playwright's strict mode and when does it throw errors?

**Answer:**
- Strict mode means Playwright **throws an error** if a Locator resolves to **more than one element** when performing a single-element action (click, fill, etc.).
- It prevents silent bugs where a test acts on the wrong element because the selector matched multiple nodes.
- Strict mode is **on by default** for all Locator-based actions in Playwright Test.

**Explanation:**
- Before strict mode, frameworks like Selenium silently acted on the first matched element, masking selector ambiguity bugs.
- In modern SPAs, generic selectors like `button.primary` may match multiple buttons — strict mode forces precision.
- You can bypass strict mode intentionally using `.first()`, `.last()`, `.nth(n)`, or `.filter()`.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('strict mode handling', async ({ page }) => {
  await page.goto('/cart');

  // ❌ Throws if multiple "Remove" buttons exist
  // await page.getByRole('button', { name: 'Remove' }).click();

  // ✅ Explicit nth selection
  await page.getByRole('button', { name: 'Remove' }).first().click();

  // ✅ Filter to a specific context
  await page
    .getByTestId('cart-item')
    .filter({ hasText: 'iPhone 15' })
    .getByRole('button', { name: 'Remove' })
    .click();

  // ✅ Check count before acting
  const removeButtons = page.getByRole('button', { name: 'Remove' });
  const count = await removeButtons.count();
  console.log(`Found ${count} remove buttons`);
  await removeButtons.nth(0).click();
});
```

**Real-world Usage:**
- In e-commerce cart tests, multiple "Remove" and "Edit" buttons exist per row — strict mode forces testers to scope locators to their row context.
- In data tables in FinTech dashboards, strict mode prevents accidental interaction with the wrong row's action button.

**Common Mistakes:**
- Using `.first()` as a lazy fix without investigating why multiple elements match — hides a real selector problem.
- Writing overly broad CSS selectors like `page.locator('button')` — always violates strict mode.
- Disabling strict mode globally via configuration — defeats its purpose entirely.

**Optimization Tip:**
- Use `data-testid` attributes on interactive elements in the application during development — prevents strict mode violations and makes selectors resilient to DOM restructuring.

**Debugging Strategy:**
- When a strict mode error occurs, use `await page.locator('your-selector').count()` to see how many elements match, then add `.filter()` or scope to a parent element.

**Tricky Follow-up Questions:**
1. *How would you handle a legitimate case where you need to verify all matching elements without disabling strict mode?*
2. *Does strict mode apply to `page.waitForSelector()`? Why or why not?*

---

## Q6: Compare XPath vs Playwright's built-in Locators. When should you still use XPath?

**Answer:**
- Playwright provides semantic locators (`getByRole`, `getByLabel`, `getByText`, `getByTestId`, `getByPlaceholder`, `getByAltText`, `getByTitle`) that align with **how users and assistive technologies perceive the UI**.
- XPath is a low-level DOM traversal language — powerful but brittle, slow, and tied to HTML structure.
- Prefer semantic locators; fall back to XPath only when no semantic locator can uniquely identify an element.

**Explanation:**
- Semantic locators are resilient to HTML restructuring — if a `<button>` moves in the DOM but retains its accessible role and name, the locator still works.
- XPath breaks when HTML structure changes (e.g., an added wrapper `<div>`) — a common source of maintenance burden.
- XPath is still useful for: traversing from child to parent, selecting elements by position in complex tables, attribute combinations unavailable in CSS.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('locator strategy comparison', async ({ page }) => {
  await page.goto('/login');

  // ✅ Best: semantic role + name (ARIA-based)
  await page.getByRole('textbox', { name: 'Email' }).fill('user@example.com');

  // ✅ Good: test ID (stable, developer-maintained)
  await page.getByTestId('password-input').fill('SecurePass123!');

  // ✅ Acceptable: label association
  await page.getByLabel('Password').fill('SecurePass123!');

  // ⚠️ Fragile CSS — breaks on class rename
  // await page.locator('.login-form input[type="password"]').fill(...)

  // ⚠️ XPath: use only for parent traversal or complex cases
  // Navigate from a cell value to its row's action button
  await page
    .locator('//tr[td[normalize-space()="John Doe"]]//button[@data-action="edit"]')
    .click();

  // ✅ Modern alternative to the XPath above:
  await page
    .getByRole('row', { name: 'John Doe' })
    .getByRole('button', { name: 'Edit' })
    .click();
});
```

**Real-world Usage:**
- Legacy enterprise apps with no `data-testid` attributes force use of XPath — wrap them in Page Object methods to centralize maintenance.
- Accessibility-focused teams mandate `getByRole` to validate ARIA compliance alongside functionality.

**Common Mistakes:**
- Using absolute XPath (`/html/body/div[3]/form/...`) — breaks on any structural change.
- Over-relying on CSS class selectors — classes are styling concerns and change frequently.
- Using `getByText` for buttons that change label based on state — use `getByRole` with exact name instead.

**Optimization Tip:**
- Establish a selector priority policy in your framework:
  1. `getByTestId` (most stable)
  2. `getByRole` + name (ARIA-aligned)
  3. `getByLabel` (form elements)
  4. CSS with `data-*` attributes
  5. XPath (last resort)

**Debugging Strategy:**
- Use Playwright Inspector (`PWDEBUG=1 npx playwright test`) to highlight elements matched by locators in the browser.
- Use `npx playwright codegen <url>` to get Playwright's recommended locator for any element.

**Tricky Follow-up Questions:**
1. *How does `getByRole` determine what role an element has — is it purely HTML tag-based?*
2. *What is `aria-label` vs `aria-labelledby`, and how do they affect `getByRole`'s name matching?*

**Compare:**
| | Playwright Semantic Locators | XPath | CSS Selectors |
|---|---|---|---|
| Resilience | High | Low | Medium |
| Readability | High | Low | Medium |
| ARIA alignment | Yes | No | No |
| Parent traversal | Limited | Yes | No |
| Performance | Fast | Slowest | Fast |

---

## Q7: How does Playwright handle iFrames and nested frames?

**Answer:**
- Playwright provides the `frameLocator()` API to scope all locator queries to a specific iframe's document.
- For legacy `ElementHandle`-based access, `page.frame(name)` and `page.frames()` are available but not preferred.
- Nested frames require chaining `frameLocator()` calls.

**Explanation:**
- iFrames create separate browsing contexts with their own DOM. Without explicit frame scoping, locators only search the top-level document.
- `frameLocator()` returns a `FrameLocator` object — all subsequent `.locator()` / `getByRole()` calls are evaluated within that iframe.
- Cross-origin iframes are fully supported, unlike Cypress which cannot interact with cross-origin iframes.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('iframe interaction', async ({ page }) => {
  await page.goto('/payment');

  // Single iframe by CSS selector
  const stripeFrame = page.frameLocator('iframe[name="stripe-card"]');
  await stripeFrame.getByLabel('Card number').fill('4242 4242 4242 4242');
  await stripeFrame.getByLabel('Expiry').fill('12/30');
  await stripeFrame.getByLabel('CVC').fill('123');

  // Nested iframe
  const outerFrame = page.frameLocator('#outer-frame');
  const innerFrame = outerFrame.frameLocator('#inner-frame');
  await innerFrame.getByRole('button', { name: 'Submit' }).click();

  // Locating by content inside frame
  const docFrame = page.frameLocator('iframe').filter({ 
    has: page.locator('[title="Document Viewer"]') 
  });
  await expect(docFrame.getByText('Contract Terms')).toBeVisible();
});
```

**Real-world Usage:**
- Payment forms (Stripe, PayPal, Adyen) embed card input fields in sandboxed iframes — mandatory knowledge for FinTech automation.
- Third-party analytics dashboards, chat widgets (Intercom, Zendesk), and embedded reports (Power BI) use iframes extensively.
- KYC document upload flows often render document previews in iframes.

**Common Mistakes:**
- Attempting to locate an element inside an iframe without `frameLocator()` — locator returns no match silently.
- Using `page.frame({ url: '...' })` for cross-origin iframes without proper CSP consideration.
- Not waiting for the iframe to load before interacting — use `frameLocator().locator('...')` which auto-waits.

**Optimization Tip:**
- Encapsulate frame interactions in dedicated utility classes:
  ```typescript
  class StripeFrameHelper {
    private frame: FrameLocator;
    constructor(page: Page) {
      this.frame = page.frameLocator('iframe[name="stripe-card"]');
    }
    async fillCardDetails(number: string, expiry: string, cvc: string) {
      await this.frame.getByLabel('Card number').fill(number);
      await this.frame.getByLabel('Expiry').fill(expiry);
      await this.frame.getByLabel('CVC').fill(cvc);
    }
  }
  ```

**Debugging Strategy:**
- List all frames on the page: `page.frames().forEach(f => console.log(f.url(), f.name()))`.
- If a `frameLocator` never resolves, verify the iframe `src` has fully loaded using `page.waitForLoadState('networkidle')`.

**Tricky Follow-up Questions:**
1. *How do you interact with a dynamically injected iframe that doesn't have a stable `name` or `id`?*
2. *What is the difference between `frameLocator()` and `page.frame()`? Which should you always prefer and why?*

**Compare:**
- Selenium: `driver.switchTo().frame()` — must explicitly switch context and switch back, error-prone in parallel tests.
- Cypress: Cannot interact with cross-origin iframes natively — a well-known limitation.
- Playwright: No context switching required; scoped locators handle it cleanly.

---

## Q8: How does Playwright handle multiple tabs and new pages?

**Answer:**
- Playwright handles new tabs/pages using the `context.waitForEvent('page')` pattern, which resolves when a new `Page` is created within the same `BrowserContext`.
- New tabs share the browser context (cookies, localStorage) with the originating page.
- You can also programmatically open new pages with `context.newPage()`.

**Explanation:**
- In browser automation, any `target="_blank"` link or `window.open()` call creates a new page in the same context.
- Playwright makes this deterministic by letting you await the `'page'` event before the triggering action completes.
- New page objects require their own `waitForLoadState` to ensure content has rendered before interaction.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, Page } from '@playwright/test';

test('handle new tab on link click', async ({ page, context }) => {
  await page.goto('/dashboard');

  // Wait for new tab to open when clicking "Open Report"
  const [newPage] = await Promise.all([
    context.waitForEvent('page'),
    page.getByRole('link', { name: 'Open Report' }).click(),
  ]);

  await newPage.waitForLoadState('domcontentloaded');
  await expect(newPage).toHaveURL(/\/reports\//);
  await expect(newPage.getByRole('heading', { name: 'Q4 Report' })).toBeVisible();
  await newPage.close();
});

test('open new tab programmatically', async ({ context }) => {
  const page1: Page = await context.newPage();
  const page2: Page = await context.newPage();

  await page1.goto('/login');
  await page2.goto('/public/pricing');

  // Work across both tabs
  await expect(page1.getByTestId('login-form')).toBeVisible();
  await expect(page2.getByTestId('pricing-table')).toBeVisible();
});
```

**Real-world Usage:**
- PDF/invoice downloads in new tabs are tested in FinTech and SaaS billing systems.
- OAuth flows that open a new window for login are handled using the multi-page pattern.
- Verifying that admin actions in one tab are reflected in another tab (real-time updates, WebSockets).

**Common Mistakes:**
- Not using `Promise.all()` to simultaneously wait for the page event and trigger the action — the new tab may open and close before you start listening.
- Forgetting `waitForLoadState` on the new page — leads to acting on an empty/loading page.
- Accessing `page.context().pages()` instead of awaiting the `'page'` event — race condition.

**Optimization Tip:**
- Create a reusable utility:
  ```typescript
  async function openNewTab(page: Page, action: () => Promise<void>): Promise<Page> {
    const [newPage] = await Promise.all([
      page.context().waitForEvent('page'),
      action(),
    ]);
    await newPage.waitForLoadState('domcontentloaded');
    return newPage;
  }
  ```

**Debugging Strategy:**
- Log all open pages: `context.pages().map(p => console.log(p.url()))` at the point of failure.
- If the new tab closes before you interact, increase the `waitForEvent` timeout.

**Tricky Follow-up Questions:**
1. *Does a new page created via `window.open()` share cookies with the opener page? What about `localStorage`?*
2. *How do you handle a scenario where a click may or may not open a new tab depending on application state?*

**Compare:**
- Selenium: Requires `driver.switchTo().window(handle)` — must iterate over window handles, fragile with multiple tabs.
- Cypress: Cannot natively handle multiple tabs or windows — requires workarounds (`cy.stub(window, 'open')`).
- Playwright: First-class multi-page API within same context.

---

## Q9: How does Playwright handle browser dialogs (alert, confirm, prompt)?

**Answer:**
- Playwright handles JavaScript dialogs (`alert`, `confirm`, `prompt`, `beforeunload`) using the `page.on('dialog', handler)` event listener.
- The handler receives a `Dialog` object with methods: `.accept()`, `.dismiss()`, `.message()`, `.defaultValue()`.
- If no handler is registered, **Playwright auto-dismisses** dialogs by default.

**Explanation:**
- Unlike Selenium which blocks test execution on a dialog, Playwright's event-driven approach handles dialogs asynchronously.
- You must register the `dialog` listener **before** the action that triggers it, not after.
- The `beforeunload` dialog requires `page.on('dialog')` registered before navigation or close.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('handle alert dialog', async ({ page }) => {
  await page.goto('/settings');

  // Register handler BEFORE triggering action
  page.once('dialog', async (dialog) => {
    expect(dialog.message()).toBe('Are you sure you want to delete this account?');
    await dialog.accept();
  });

  await page.getByRole('button', { name: 'Delete Account' }).click();
  await expect(page.getByText('Account deleted')).toBeVisible();
});

test('handle confirm - dismiss', async ({ page }) => {
  await page.goto('/data');

  page.once('dialog', async (dialog) => {
    expect(dialog.type()).toBe('confirm');
    await dialog.dismiss(); // Cancel deletion
  });

  await page.getByRole('button', { name: 'Clear All Data' }).click();
  await expect(page.getByTestId('data-table')).toBeVisible(); // Data still present
});

test('handle prompt dialog', async ({ page }) => {
  await page.goto('/rename');

  page.once('dialog', async (dialog) => {
    expect(dialog.type()).toBe('prompt');
    await dialog.accept('New Report Name'); // Enter value
  });

  await page.getByRole('button', { name: 'Rename' }).click();
  await expect(page.getByText('New Report Name')).toBeVisible();
});
```

**Real-world Usage:**
- CRM and admin tools use `confirm` dialogs for destructive actions (delete, archive, bulk update).
- Legacy enterprise apps built with jQuery or vanilla JS heavily use native browser dialogs.
- `beforeunload` dialog testing ensures users are warned before leaving unsaved forms.

**Common Mistakes:**
- Registering the `dialog` handler after triggering the action — handler never fires, dialog auto-dismisses.
- Using `page.on('dialog', ...)` (persistent) when only one dialog is expected — use `page.once()` to prevent stale handlers.
- Not resetting the handler between tests — a stale handler from a previous test handles the next test's dialog.

**Optimization Tip:**
- For repeated dialog acceptance in a test suite, create a helper:
  ```typescript
  function autoAcceptDialogs(page: Page, expectedMessage?: string): void {
    page.on('dialog', async (dialog) => {
      if (expectedMessage) expect(dialog.message()).toContain(expectedMessage);
      await dialog.accept();
    });
  }
  ```

**Debugging Strategy:**
- Add `console.log(dialog.type(), dialog.message())` in the handler to confirm which dialog type fires.
- If dialog is auto-dismissed unexpectedly, verify no previous test left a `dismiss` handler registered on the context.

**Tricky Follow-up Questions:**
1. *What happens if you don't handle a `beforeunload` dialog and call `page.close()`?*
2. *Can you test a scenario where the user clicks Cancel on a confirm dialog and verifies the data is unchanged, all in one test?*

---

## Q10: How do you handle file uploads and downloads in Playwright?

**Answer:**
- **File Upload**: Use `page.setInputFiles(locator, filePath)` or `locator.setInputFiles(filePath)` to directly set files on `<input type="file">`.
- **File Download**: Use `page.waitForEvent('download')` combined with the triggering action, then save or read the file.
- Both APIs support single files, multiple files, and empty file sets (to clear).

**Explanation:**
- Playwright bypasses the OS file picker dialog entirely — it directly injects the file into the input element via the protocol.
- For download verification, the `Download` object provides `suggestedFilename()`, `path()` (after save), and `stream()` for content inspection.
- Large downloads can be streamed without saving to disk, useful for validating content in memory.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

// --- FILE UPLOAD ---
test('upload single file', async ({ page }) => {
  await page.goto('/documents/upload');

  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles(path.join(__dirname, 'fixtures/test-document.pdf'));

  await page.getByRole('button', { name: 'Upload' }).click();
  await expect(page.getByText('Upload successful')).toBeVisible();
});

test('upload multiple files', async ({ page }) => {
  await page.goto('/gallery/upload');
  await page.locator('input[type="file"]').setInputFiles([
    path.join(__dirname, 'fixtures/image1.png'),
    path.join(__dirname, 'fixtures/image2.png'),
  ]);
  await page.getByRole('button', { name: 'Submit' }).click();
});

// --- FILE DOWNLOAD ---
test('download and verify CSV report', async ({ page }) => {
  await page.goto('/reports');

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Export CSV' }).click(),
  ]);

  expect(download.suggestedFilename()).toMatch(/report.*\.csv/);

  const savePath = path.join('downloads', download.suggestedFilename());
  await download.saveAs(savePath);

  const content = fs.readFileSync(savePath, 'utf-8');
  expect(content).toContain('Transaction ID');
  expect(content).toContain('Amount');
});
```

**Real-world Usage:**
- Document upload is critical in KYC flows (passport, ID, proof of address), loan applications, and HR onboarding.
- CSV/PDF report download verification is a standard test case in FinTech reporting modules.
- Bulk import testing (user CSV upload, product catalog upload) in SaaS admin panels.

**Common Mistakes:**
- Clicking a custom-styled "Upload" button instead of directly targeting the hidden `<input type="file">` — use `setInputFiles` on the input, not the decorative button.
- Not using `Promise.all()` for download events — missing the download if it fires before the listener is registered.
- Assuming `download.path()` is immediately available — it returns `null` until `saveAs()` or `path()` await resolves.

**Optimization Tip:**
- Keep fixture files small and deterministic. Store reusable upload fixtures in a `fixtures/` folder.
- For download content validation, use streaming instead of saving to disk to avoid filesystem flakiness in parallel runs:
  ```typescript
  const stream = await download.createReadStream();
  ```

**Debugging Strategy:**
- Log `download.failure()` to capture download errors (e.g., network abort, server error).
- Use `page.route('**/download/**', route => route.fulfill({ body: testContent }))` to mock downloads in unit-level tests.

**Tricky Follow-up Questions:**
1. *How would you test a drag-and-drop file upload component where there is no `<input type="file">` element?*
2. *How do you verify that a PDF download contains specific text content without using a third-party library?*

---

## Q11: How do you manage authentication state and session reuse in Playwright?

**Answer:**
- Playwright can serialize a browser context's authentication state (cookies + localStorage) to a JSON file using `context.storageState({ path })`.
- This state file is loaded into new contexts using `browser.newContext({ storageState: 'path/to/state.json' })` or via `playwright.config.ts`.
- This pattern eliminates UI login in every test, reducing test runtime significantly.

**Explanation:**
- The storage state includes all cookies (session tokens, CSRF tokens) and localStorage (JWT tokens, user preferences).
- A `globalSetup` script performs login once per test run and saves the state; all tests reuse it.
- For multi-role systems, maintain separate state files (e.g., `admin.json`, `user.json`, `readonly.json`).

**JavaScript / TypeScript Example:**
```typescript
// global-setup.ts
import { chromium, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig): Promise<void> {
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto(`${config.projects[0].use.baseURL}/login`);
  await page.getByLabel('Email').fill(process.env.ADMIN_EMAIL!);
  await page.getByLabel('Password').fill(process.env.ADMIN_PASSWORD!);
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL('/dashboard');

  // Save authenticated state
  await context.storageState({ path: 'auth/admin.json' });
  await browser.close();
}

export default globalSetup;
```

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  globalSetup: './global-setup',
  use: {
    storageState: 'auth/admin.json', // Default for all tests
    baseURL: 'https://staging.app.com',
  },
  projects: [
    { name: 'admin', use: { storageState: 'auth/admin.json' } },
    { name: 'readonly-user', use: { storageState: 'auth/readonly.json' } },
  ],
});
```

```typescript
// test using pre-authenticated state
import { test, expect } from '@playwright/test';

test('admin can access settings', async ({ page }) => {
  // No login step needed — session loaded from admin.json
  await page.goto('/admin/settings');
  await expect(page.getByRole('heading', { name: 'System Settings' })).toBeVisible();
});

// Test requiring fresh (unauthenticated) context
test('redirect to login when unauthenticated', async ({ browser }) => {
  const context = await browser.newContext(); // No storageState
  const page = await context.newPage();
  await page.goto('/dashboard');
  await expect(page).toHaveURL('/login');
  await context.close();
});
```

**Real-world Usage:**
- Enterprise test suites with 500+ tests save significant CI time by performing login once per role per run.
- FinTech tests with MFA can use API-based token injection into storage state, bypassing the MFA UI.
- Multi-role authorization tests (admin, manager, viewer) use project-scoped storage states.

**Common Mistakes:**
- Committing `auth/*.json` files to version control — they contain session tokens (security risk). Add to `.gitignore`.
- Not rebuilding storage state when the authentication flow changes — stale tokens cause all tests to fail.
- Using the same storage state for parallel workers writing to it — race condition.

**Optimization Tip:**
- Inject API tokens directly into storage state without UI login for backend-issued JWTs:
  ```typescript
  await context.addCookies([{ name: 'session', value: apiToken, domain: '.app.com', path: '/' }]);
  await context.storageState({ path: 'auth/api-user.json' });
  ```

**Debugging Strategy:**
- If tests fail with auth errors, regenerate the storage state manually and inspect the JSON file to verify cookies/localStorage match expected values.
- Use `page.evaluate(() => localStorage.getItem('token'))` to verify the token is present in the browser.

**Tricky Follow-up Questions:**
1. *How do you handle SSO/SAML login flows in Playwright where the authentication happens on a third-party domain?*
2. *What happens to storageState when a session expires mid-test run? How do you handle token refresh?*

---

## Q12: How does Playwright's network interception work? Explain `route.fulfill`, `route.continue`, and `route.abort`.

**Answer:**
- `page.route(pattern, handler)` intercepts all requests matching a URL pattern (string, glob, or regex).
- `route.fulfill()` — responds with a mocked response without hitting the real server.
- `route.continue()` — passes the request through, optionally modifying headers/body.
- `route.abort()` — cancels the request, simulating network failure.

**Explanation:**
- Network interception is implemented at the browser protocol level — it's faster and more reliable than proxy-based approaches.
- Routes are evaluated in registration order; first match wins.
- You can intercept XHR, Fetch, WebSocket, and static asset requests.
- Route handlers can be async — useful for validating request payloads from UI actions.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('mock API response (route.fulfill)', async ({ page }) => {
  await page.route('**/api/users', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, name: 'Alice', role: 'admin' },
        { id: 2, name: 'Bob', role: 'user' },
      ]),
    });
  });

  await page.goto('/users');
  await expect(page.getByText('Alice')).toBeVisible();
  await expect(page.getByText('Bob')).toBeVisible();
});

test('modify request headers (route.continue)', async ({ page }) => {
  await page.route('**/api/**', async (route) => {
    await route.continue({
      headers: {
        ...route.request().headers(),
        'X-Test-Run': 'playwright',
        'Authorization': `Bearer ${process.env.API_TOKEN}`,
      },
    });
  });

  await page.goto('/data');
});

test('simulate network failure (route.abort)', async ({ page }) => {
  await page.route('**/api/payments', (route) => route.abort('connectionrefused'));

  await page.goto('/checkout');
  await page.getByRole('button', { name: 'Pay Now' }).click();
  await expect(page.getByText('Payment service unavailable')).toBeVisible();
});

test('validate request payload from UI action', async ({ page }) => {
  let capturedPayload: Record<string, unknown> = {};

  await page.route('**/api/orders', async (route) => {
    capturedPayload = JSON.parse(route.request().postData() ?? '{}');
    await route.continue();
  });

  await page.goto('/checkout');
  await page.getByRole('button', { name: 'Place Order' }).click();
  await page.waitForResponse('**/api/orders');

  expect(capturedPayload.items).toHaveLength(2);
  expect(capturedPayload.currency).toBe('USD');
});
```

**Real-world Usage:**
- Mocking backend APIs that are unstable in staging environments to create deterministic UI tests.
- Testing error states (500, 403, 429) without engineering backend logic to produce them.
- FinTech: Simulating payment gateway timeouts and verifying UI error handling.
- Intercepting analytics calls to prevent test data contamination of production metrics.

**Common Mistakes:**
- Not calling `route.fulfill()`, `route.continue()`, or `route.abort()` in the handler — request hangs indefinitely, causing test timeout.
- Using string literal URL patterns instead of glob patterns — e.g., `'/api/users'` won't match cross-origin requests.
- Forgetting to call `page.unroute()` between tests — stale routes contaminate subsequent tests.

**Optimization Tip:**
- Create a route registry/factory in your framework:
  ```typescript
  async function mockApiEndpoint<T>(page: Page, url: string, data: T, status = 200): Promise<void> {
    await page.route(url, route => route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(data),
    }));
  }
  ```

**Debugging Strategy:**
- Use `page.on('request', req => console.log(req.url()))` to log all requests and confirm your route pattern is matching.
- Check `route.request().resourceType()` to filter by type (xhr, fetch, document, etc.).

**Tricky Follow-up Questions:**
1. *How do you intercept and modify a WebSocket connection in Playwright?*
2. *If you register two `page.route()` handlers for the same URL, which one executes? How do you chain them?*

---

## Q13: How do you perform API testing using Playwright's `APIRequestContext`?

**Answer:**
- `APIRequestContext` provides HTTP client capabilities (GET, POST, PUT, DELETE, PATCH) directly within Playwright tests, without launching a browser.
- It is accessible via the `request` fixture in `@playwright/test`, or created standalone with `playwright.request.newContext()`.
- It automatically inherits the browser context's cookies and storage state, enabling seamless UI + API hybrid tests.

**Explanation:**
- Unlike Axios or Supertest, `APIRequestContext` is context-aware — it can use the same authentication cookies as the browser page.
- API responses are `APIResponse` objects with `.json()`, `.text()`, `.status()`, `.headers()`, and `.ok()` methods.
- It supports full request customization: headers, body, form data, multipart, query params, and timeout.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, APIRequestContext } from '@playwright/test';

// --- Standalone API test (no browser) ---
test('GET /api/users returns list', async ({ request }) => {
  const response = await request.get('/api/users', {
    headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
    params: { page: 1, limit: 10 },
  });

  expect(response.status()).toBe(200);
  const users = await response.json();
  expect(users).toHaveProperty('data');
  expect(users.data).toHaveLength(10);
});

test('POST /api/orders creates order and validates schema', async ({ request }) => {
  const payload = {
    customerId: 'cust-001',
    items: [{ productId: 'prod-123', quantity: 2 }],
    currency: 'USD',
  };

  const response = await request.post('/api/orders', { data: payload });
  expect(response.ok()).toBeTruthy();

  const order = await response.json();
  expect(order).toMatchObject({
    id: expect.stringMatching(/^ord-/),
    status: 'pending',
    currency: 'USD',
  });
});

// --- Hybrid UI + API test ---
test('create user via API, verify in UI', async ({ page, request }) => {
  // Setup: create user via API
  const createResp = await request.post('/api/users', {
    data: { name: 'Test User', email: 'test@example.com', role: 'viewer' },
    headers: { Authorization: `Bearer ${process.env.ADMIN_TOKEN}` },
  });
  const { id } = await createResp.json();

  // Action: verify user appears in admin UI
  await page.goto('/admin/users');
  await expect(page.getByText('test@example.com')).toBeVisible();

  // Teardown: delete via API
  await request.delete(`/api/users/${id}`);
});
```

**Real-world Usage:**
- API contract testing within the same test suite as UI tests — validates backend contracts haven't changed.
- Test data setup and teardown via API (faster and more reliable than UI setup).
- FinTech: Creating trade orders via API and verifying them in the portfolio UI.

**Common Mistakes:**
- Not handling non-2xx responses — `response.ok()` returns false; always assert status explicitly.
- Reusing the `request` fixture across tests without resetting cookies — causes state bleed.
- Not setting `baseURL` in config, leading to relative URL failures for API requests.

**Optimization Tip:**
- Create a typed API client wrapper:
  ```typescript
  class OrdersApiClient {
    constructor(private request: APIRequestContext, private baseUrl: string) {}
    
    async create(order: CreateOrderDto): Promise<OrderResponse> {
      const res = await this.request.post(`${this.baseUrl}/api/orders`, { data: order });
      expect(res.ok()).toBeTruthy();
      return res.json();
    }

    async delete(id: string): Promise<void> {
      const res = await this.request.delete(`${this.baseUrl}/api/orders/${id}`);
      expect(res.status()).toBe(204);
    }
  }
  ```

**Debugging Strategy:**
- Log full response details on failure:
  ```typescript
  if (!response.ok()) {
    console.error('API Error:', response.status(), await response.text());
  }
  ```
- Use Playwright's trace viewer to inspect API requests made during a test run.

**Tricky Follow-up Questions:**
1. *How does `APIRequestContext` share cookies with a browser `Page` in the same test? When does this break?*
2. *How would you implement retry logic for flaky API endpoints within a Playwright test?*

---

## Q14: How do you implement and use fixtures in Playwright Test?

**Answer:**
- Fixtures are **dependency-injected values or objects** provided to tests via the `test` function signature.
- Playwright Test has built-in fixtures (`page`, `browser`, `context`, `request`, `browserName`) and allows defining **custom fixtures** using `test.extend()`.
- Fixtures support setup/teardown via generator functions (`yield`), scoping (`test` or `worker`), and options.

**Explanation:**
- Fixtures replace `beforeEach`/`afterEach` hooks with a composable, reusable dependency injection pattern.
- Worker-scoped fixtures run once per worker process, ideal for expensive operations like browser launch or API authentication.
- Test-scoped fixtures run once per test, providing fresh isolated state.
- Fixtures can depend on other fixtures, forming a dependency graph.

**JavaScript / TypeScript Example:**
```typescript
import { test as base, expect, Page } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';
import { ApiClient } from './utils/ApiClient';

// Define custom fixture types
type CustomFixtures = {
  authenticatedPage: Page;
  loginPage: LoginPage;
  apiClient: ApiClient;
  testUser: { id: string; email: string; token: string };
};

// Extend base test with custom fixtures
export const test = base.extend<CustomFixtures>({
  // Test-scoped: creates a test user via API, yields, then cleans up
  testUser: async ({ request }, use) => {
    const res = await request.post('/api/test/users', {
      data: { name: 'Playwright Test User', role: 'user' },
      headers: { Authorization: `Bearer ${process.env.ADMIN_TOKEN}` },
    });
    const user = await res.json();

    await use(user); // ← test runs here

    // Teardown: delete user after test
    await request.delete(`/api/test/users/${user.id}`);
  },

  // Test-scoped: authenticated page using worker-level auth
  authenticatedPage: async ({ page, testUser }, use) => {
    await page.addInitScript((token: string) => {
      localStorage.setItem('auth_token', token);
    }, testUser.token);
    await use(page);
  },

  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  apiClient: async ({ request }, use) => {
    await use(new ApiClient(request, process.env.BASE_URL!));
  },
});

export { expect };
```

```typescript
// test file using custom fixtures
import { test, expect } from './fixtures';

test('authenticated user sees dashboard', async ({ authenticatedPage, testUser }) => {
  await authenticatedPage.goto('/dashboard');
  await expect(authenticatedPage.getByText(testUser.email)).toBeVisible();
});
```

**Real-world Usage:**
- Enterprise frameworks use fixtures to inject page objects, API clients, test data factories, and reporting helpers.
- Worker-scoped database fixtures create/restore database snapshots once per parallel worker.
- FinTech: A `tradeOrderFixture` creates a simulated trade order and tears it down after each test.

**Common Mistakes:**
- Using `beforeEach` when a fixture would be more reusable and composable.
- Not calling `use(value)` in a fixture — test hangs indefinitely waiting for the fixture to yield.
- Defining worker-scoped fixtures for state that must be isolated per test — causes bleed.

**Optimization Tip:**
- Compose fixtures rather than monolith setup functions. A `dashboardPage` fixture can depend on `authenticatedPage` which depends on `testUser`:
  ```
  testUser → authenticatedPage → dashboardPage
  ```
  Each layer is independently reusable.

**Debugging Strategy:**
- Add `console.log('[fixture] setup')` and `console.log('[fixture] teardown')` to trace fixture lifecycle in CI logs.
- If teardown doesn't run, verify no unhandled exception before `await use(value)`.

**Tricky Follow-up Questions:**
1. *What is the difference between a worker-scoped fixture and a test-scoped fixture, and what problems arise if you use the wrong scope?*
2. *How do you pass configuration options into a fixture from the test or from `playwright.config.ts`?*

---

## Q15: How do you configure and manage parallel execution in Playwright?

**Answer:**
- Playwright runs tests in parallel using **worker processes** — each worker is an independent Node.js process with its own browser instance.
- Parallelism is configured via `workers` in `playwright.config.ts` — defaults to half the CPU cores.
- Tests within a single file run **serially by default**; files are distributed across workers in parallel.
- Use `test.describe.configure({ mode: 'parallel' })` to parallelize tests within a single file.

**Explanation:**
- Worker isolation ensures no shared memory between parallel tests — each worker has its own browser context.
- Slow test suites can be sped up dramatically: 100 tests × 5 workers = ~5x faster (minus overhead).
- Shared resources (databases, external APIs) require careful design — parallel tests must not conflict on shared IDs.
- `fullyParallel: true` in config makes all tests run in parallel regardless of file structure.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  fullyParallel: true,
  workers: process.env.CI ? 4 : 2,   // 4 in CI, 2 locally
  retries: process.env.CI ? 2 : 0,    // Retry only in CI
  
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    storageState: 'auth/user.json',
    trace: 'on-first-retry',           // Capture trace on first retry
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
    { name: 'firefox', use: { browserName: 'firefox' } },
  ],
});
```

```typescript
// Parallelizing tests within a file
import { test, expect } from '@playwright/test';

test.describe.configure({ mode: 'parallel' });

test.describe('User Profile', () => {
  test('view profile', async ({ page }) => { /* ... */ });
  test('edit profile', async ({ page }) => { /* ... */ });
  test('delete avatar', async ({ page }) => { /* ... */ });
});

// Forcing serial execution for tests with shared state
test.describe.configure({ mode: 'serial' });

test.describe('Checkout Flow (serial)', () => {
  test('add item to cart', async ({ page }) => { /* ... */ });
  test('apply coupon', async ({ page }) => { /* ... */ });
  test('complete payment', async ({ page }) => { /* ... */ });
});
```

```typescript
// Generating unique test data to avoid conflicts in parallel runs
import { test } from '@playwright/test';

test('create order', async ({ page }, testInfo) => {
  // Use testInfo.workerIndex or testInfo.parallelIndex for unique IDs
  const orderId = `test-order-${testInfo.workerIndex}-${Date.now()}`;
  await page.goto(`/orders/new?ref=${orderId}`);
  // ...
});
```

**Real-world Usage:**
- CI pipelines sharding tests across multiple machines: `--shard=1/4`, `--shard=2/4`, etc.
- Enterprise suites use separate `projects` for smoke tests (2 workers) and regression (8 workers).
- Database-backed tests use worker-scoped fixtures to create isolated database schemas per worker.

**Common Mistakes:**
- Using hardcoded test data IDs in parallel tests — causes conflicts and race conditions.
- Setting `workers` too high for the test environment's memory/CPU — degrades performance.
- Marking database-mutating tests as parallel without isolation — leads to data corruption.
- Not using `test.describe.configure({ mode: 'serial' })` for inherently sequential flows.

**Optimization Tip:**
- Use test sharding in CI for very large suites:
  ```bash
  # Run on 3 separate CI agents
  npx playwright test --shard=1/3
  npx playwright test --shard=2/3
  npx playwright test --shard=3/3
  ```
- Merge reports from shards using `npx playwright merge-reports`.

**Debugging Strategy:**
- Run with `--workers=1` to reproduce parallel-specific failures in serial mode.
- Use `testInfo.workerIndex` in logs to trace which worker produced a failure.
- Enable `reporter: [['list'], ['html']]` to see per-worker test distribution.

**Tricky Follow-up Questions:**
1. *How does Playwright decide which tests go to which worker? Can you control test-to-worker assignment?*
2. *What is the impact of `fullyParallel: true` on tests that share a `storageState` file — is it safe?*

---

*— End of Q1–Q15 —*

---

## Q16: How do you manage cookies, localStorage, and sessionStorage in Playwright?

**Answer:**
- **Cookies**: managed via `context.cookies()`, `context.addCookies()`, `context.clearCookies()`.
- **localStorage / sessionStorage**: accessed via `page.evaluate()` since they are browser-side JS APIs.
- Both can be serialized to/from the `storageState` JSON format for session reuse across tests.

**Explanation:**
- Cookies are scoped to the `BrowserContext` — adding a cookie affects all pages in that context.
- localStorage is scoped to the origin (`domain + protocol + port`) and persists within the context.
- sessionStorage is scoped to the tab (page) and is cleared when the page is closed.
- Direct manipulation without UI login is the fastest way to seed authentication state.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, BrowserContext } from '@playwright/test';

test('cookie management', async ({ context, page }) => {
  // Add a cookie
  await context.addCookies([{
    name: 'session_token',
    value: 'abc123xyz',
    domain: 'example.com',
    path: '/',
    httpOnly: true,
    secure: true,
    sameSite: 'Strict',
    expires: Math.floor(Date.now() / 1000) + 3600, // 1 hour
  }]);

  await page.goto('/dashboard');
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

  // Read cookies
  const cookies = await context.cookies('https://example.com');
  const sessionCookie = cookies.find(c => c.name === 'session_token');
  expect(sessionCookie?.value).toBe('abc123xyz');

  // Clear all cookies
  await context.clearCookies();
});

test('localStorage manipulation', async ({ page }) => {
  await page.goto('/');

  // Set localStorage before page logic runs using addInitScript
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'Bearer eyJhbGci...');
    localStorage.setItem('user_role', 'admin');
    localStorage.setItem('theme', 'dark');
  });

  await page.reload();
  await expect(page.getByTestId('theme-wrapper')).toHaveClass(/dark/);

  // Read localStorage mid-test
  const token = await page.evaluate(() => localStorage.getItem('auth_token'));
  expect(token).toContain('Bearer');

  // Clear specific key
  await page.evaluate(() => localStorage.removeItem('theme'));
});

test('sessionStorage manipulation', async ({ page }) => {
  await page.goto('/checkout');

  // Inject cart state into sessionStorage
  await page.evaluate(() => {
    sessionStorage.setItem('cart', JSON.stringify([
      { id: 'prod-1', qty: 2, price: 99.99 },
    ]));
  });

  await page.reload();
  await expect(page.getByTestId('cart-count')).toHaveText('2');
});
```

**Real-world Usage:**
- Injecting JWT tokens into localStorage to bypass UI login in tests targeting token-auth SPAs.
- Seeding feature flags stored in localStorage to test premium features without backend config.
- Clearing cookies between tests that verify session expiry behavior.
- FinTech: Injecting trading session state into sessionStorage to test mid-session scenarios.

**Common Mistakes:**
- Using `page.evaluate(() => localStorage.setItem(...))` after `page.goto()` — the script runs after the app may have already read (or overwritten) localStorage. Use `addInitScript` instead.
- Adding cookies without specifying `domain` — cookie is silently dropped.
- Forgetting `httpOnly: true` cookies cannot be read via `document.cookie` — only via `context.cookies()`.

**Optimization Tip:**
- For JWT-auth SPAs, skip UI login entirely by injecting the token via `addInitScript`:
  ```typescript
  await page.addInitScript((token: string) => {
    localStorage.setItem('access_token', token);
  }, await generateTestToken());
  ```

**Debugging Strategy:**
- Use `page.evaluate(() => JSON.stringify(localStorage))` to dump the full localStorage at any point.
- Compare `context.cookies()` before and after login to confirm expected cookies were set.

**Tricky Follow-up Questions:**
1. *Why does `addInitScript` run before page scripts, and why is that critical for localStorage injection?*
2. *How do you test a feature that behaves differently based on the absence of a specific cookie?*

---

## Q17: How do you validate network requests and responses in Playwright?

**Answer:**
- Use `page.waitForRequest()` / `page.waitForResponse()` to await specific network events.
- Use `page.on('request', ...)` / `page.on('response', ...)` for continuous event-based monitoring.
- Combine with `route()` interception to capture and assert request payloads.
- `response.json()` / `response.text()` / `response.status()` expose full response details.

**Explanation:**
- Network validation bridges the gap between UI actions and backend correctness — verifying that a button click triggers the right API call with the right payload.
- `waitForResponse()` accepts URL string, glob, regex, or a predicate function for flexible matching.
- `page.waitForRequest()` resolves when a matching request is *sent*; `waitForResponse()` resolves when the *response* arrives.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, Request, Response } from '@playwright/test';

test('validate API call from UI action', async ({ page }) => {
  await page.goto('/checkout');

  // Wait for both the action and the response concurrently
  const [response] = await Promise.all([
    page.waitForResponse(
      (res) => res.url().includes('/api/orders') && res.request().method() === 'POST'
    ),
    page.getByRole('button', { name: 'Place Order' }).click(),
  ]);

  expect(response.status()).toBe(201);
  const body = await response.json();
  expect(body).toMatchObject({
    status: 'pending',
    currency: 'USD',
  });
  expect(body.id).toMatch(/^ord-/);
});

test('validate request payload from UI', async ({ page }) => {
  const requests: Request[] = [];

  page.on('request', (req) => {
    if (req.url().includes('/api/analytics')) {
      requests.push(req);
    }
  });

  await page.goto('/products/123');
  await page.getByRole('button', { name: 'Add to Cart' }).click();

  // Allow event loop to process
  await page.waitForTimeout(500);

  const analyticsCall = requests.find(r => r.method() === 'POST');
  expect(analyticsCall).toBeDefined();

  const payload = JSON.parse(analyticsCall!.postData() ?? '{}');
  expect(payload.event).toBe('add_to_cart');
  expect(payload.productId).toBe('123');
});

test('assert no unexpected API errors', async ({ page }) => {
  const failedRequests: Response[] = [];

  page.on('response', (res) => {
    if (res.status() >= 400 && !res.url().includes('/api/health')) {
      failedRequests.push(res);
    }
  });

  await page.goto('/dashboard');
  await page.getByRole('link', { name: 'Reports' }).click();
  await page.waitForLoadState('networkidle');

  expect(failedRequests).toHaveLength(0);
});
```

**Real-world Usage:**
- FinTech: Validating that clicking "Transfer Funds" sends the exact amount, currency, and account ID in the API payload.
- SaaS: Verifying analytics/telemetry events fire correctly from UI interactions.
- Regression testing: Ensuring a UI refactor doesn't change the API contract (unintentional field removal).
- Asserting that error UI is shown when the API returns 4xx/5xx responses.

**Common Mistakes:**
- Not using `Promise.all()` — if `waitForResponse` is called after the action, the response may have already arrived.
- Matching on partial URL without accounting for query parameters — use regex: `/\/api\/orders(\?.*)?$/`.
- Forgetting that `response.json()` throws if the response body is not valid JSON — use `response.text()` for safety first.
- Asserting request payloads from `page.on('request')` before the request has been sent — timing issue.

**Optimization Tip:**
- Create a reusable request capture utility:
  ```typescript
  async function captureRequest(page: Page, urlPattern: string | RegExp, action: () => Promise<void>) {
    const [response] = await Promise.all([
      page.waitForResponse(urlPattern),
      action(),
    ]);
    return { status: response.status(), body: await response.json() };
  }
  ```

**Debugging Strategy:**
- Use `page.on('requestfailed', req => console.log('FAILED:', req.url(), req.failure()?.errorText))` to detect silently failing requests.
- Enable HAR recording to capture all network traffic for post-test analysis.

**Tricky Follow-up Questions:**
1. *What is the difference between `page.waitForResponse()` using a string vs a predicate function?*
2. *How do you validate a series of sequential API calls triggered by a single UI action?*

---

## Q18: What is HAR recording in Playwright and how do you use it for replay and testing?

**Answer:**
- **HAR (HTTP Archive)** is a JSON format that captures all network requests/responses during a browser session.
- Playwright can record a HAR file via `context.routeFromHAR()` or `page.routeFromHAR()` and replay it in tests — effectively mocking all network traffic from a recorded session.
- HAR is also used for performance analysis and API contract documentation.

**Explanation:**
- HAR replay creates a fully offline test environment — no real backend needed.
- Playwright matches incoming requests against the HAR archive and returns the recorded responses.
- Fallback options control behavior when a request has no match: `'abort'`, `'continue'`, or `'fallback'`.
- HAR recording is ideal for testing against third-party APIs (payment gateways, map APIs) without hitting their servers in CI.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

// --- RECORDING a HAR ---
test('record HAR for payment flow', async ({ page, context }) => {
  // Record all requests to the HAR file
  await context.routeFromHAR('hars/payment-flow.har', { update: true });

  await page.goto('/checkout');
  await page.getByLabel('Card Number').fill('4242424242424242');
  await page.getByRole('button', { name: 'Pay' }).click();
  await expect(page.getByText('Payment successful')).toBeVisible();
  // HAR is saved when context closes
});

// --- REPLAYING a HAR ---
test('replay HAR — payment flow offline', async ({ page, context }) => {
  await context.routeFromHAR('hars/payment-flow.har', {
    update: false,
    notFound: 'abort', // Abort any request not in the HAR
  });

  await page.goto('/checkout');
  await page.getByLabel('Card Number').fill('4242424242424242');
  await page.getByRole('button', { name: 'Pay' }).click();
  await expect(page.getByText('Payment successful')).toBeVisible();
});

// --- Selective HAR replay with live fallback ---
test('replay only external API, pass everything else through', async ({ page, context }) => {
  await context.routeFromHAR('hars/third-party-apis.har', {
    url: '**/api.stripe.com/**',  // Only mock Stripe calls
    notFound: 'fallback',         // All other requests hit the real server
  });

  await page.goto('/checkout');
  // Internal APIs hit real staging; Stripe calls are mocked from HAR
});
```

**Real-world Usage:**
- Mocking Stripe, Twilio, Google Maps, and other third-party APIs in CI without API keys or rate limits.
- Capturing production network traffic via HAR and using it as a baseline for regression tests.
- Performance testing: analyzing request timing data captured in HAR to detect regressions.
- Offline-capable test suites that work without network access (air-gapped CI environments).

**Common Mistakes:**
- Using `update: true` in CI — HAR is overwritten every run, breaking the reproducibility guarantee.
- Not versioning HAR files in source control — tests work locally but fail in CI (no HAR file present).
- Recording a HAR with environment-specific URLs (e.g., `staging.internal.com`) — it won't replay correctly against `localhost` in CI. Use URL remapping.

**Optimization Tip:**
- Store HAR files in a `hars/` directory committed to source control. Add a separate `record` test project in Playwright config to refresh them:
  ```typescript
  projects: [
    { name: 'record-hars', use: { /* update: true */ } },
    { name: 'ci-tests', use: { /* update: false */ } },
  ]
  ```

**Debugging Strategy:**
- Inspect the HAR file (it's JSON) to verify that the target requests/responses are recorded correctly.
- If replay fails, check for URL mismatches — the recorded URL must match what the test generates exactly.

**Tricky Follow-up Questions:**
1. *What are the security risks of storing HAR files in source control, and how do you mitigate them?*
2. *How would you update a stale HAR file for a specific endpoint without re-recording the entire session?*

---

## Q19: How do you use Playwright's Trace Viewer for debugging failures?

**Answer:**
- Playwright Trace is a recording of a full test run that captures: DOM snapshots, screenshots, network traffic, console logs, and action timeline.
- Configured via `trace: 'on' | 'off' | 'on-first-retry' | 'retain-on-failure'` in `playwright.config.ts` or per-context with `context.tracing.start()`.
- Viewed with `npx playwright show-trace trace.zip` or uploaded to `trace.playwright.dev`.

**Explanation:**
- Trace Viewer provides a **time-travel debugger** — you can scrub through each action and see the exact DOM state at that moment.
- Network tab shows all requests/responses with timing, making it easy to correlate UI failures with API errors.
- Action log shows locators used, their resolution, and auto-wait timings.
- Essential for debugging flaky test failures in CI where you can't run `--debug` interactively.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts — trace configuration
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    trace: 'on-first-retry',       // Record trace only when test retries (CI-efficient)
    screenshot: 'only-on-failure', // Capture screenshot on failure
    video: 'retain-on-failure',    // Keep video only for failed tests
  },
});
```

```typescript
// Manual trace control for specific tests
import { test } from '@playwright/test';

test('complex checkout flow', async ({ page, context }) => {
  await context.tracing.start({
    screenshots: true,   // Capture DOM screenshots at each action
    snapshots: true,     // Capture DOM snapshots for hover/inspect
    sources: true,       // Include source code in trace
  });

  try {
    await page.goto('/checkout');
    await page.getByRole('button', { name: 'Place Order' }).click();
    // ... test steps
  } finally {
    await context.tracing.stop({ path: `traces/checkout-${Date.now()}.zip` });
  }
});

// Chunked tracing — save intermediate trace without stopping
test('long multi-step test', async ({ page, context }) => {
  await context.tracing.start({ screenshots: true, snapshots: true });

  await page.goto('/step1');
  // Save partial trace after each major step
  await context.tracing.startChunk({ title: 'Step 1 - Navigation' });
  await page.getByRole('button', { name: 'Next' }).click();
  await context.tracing.stopChunk({ path: 'traces/step1.zip' });

  await context.tracing.startChunk({ title: 'Step 2 - Form Fill' });
  await page.getByLabel('Name').fill('Test User');
  await context.tracing.stopChunk({ path: 'traces/step2.zip' });
});
```

**Real-world Usage:**
- CI pipelines attach trace `.zip` files as build artifacts — engineers download and open them locally to debug failures without re-running.
- FinTech teams use trace viewer to correlate the exact UI state when an API returned a 422 error.
- QA teams share trace files in bug reports as a replacement for verbose screenshots + logs.
- Trace `sources: true` shows which line of test code triggered each action.

**Common Mistakes:**
- Using `trace: 'on'` in CI — records every test run, consuming gigabytes of storage and slowing CI significantly. Prefer `'on-first-retry'` or `'retain-on-failure'`.
- Not archiving trace files as CI artifacts — traces are deleted with the CI runner after each build.
- Forgetting `snapshots: true` — without snapshots, DOM inspection in Trace Viewer is unavailable.

**Optimization Tip:**
- In CI, use `--reporter=blob` + `npx playwright merge-reports` to consolidate traces from sharded runs into a single report:
  ```bash
  npx playwright merge-reports --reporter html ./blob-reports
  ```

**Debugging Strategy:**
- Open the trace in `trace.playwright.dev` (browser-based, no install needed) — share a URL with teammates.
- The "Before" and "After" DOM diff for each action shows exactly what changed, making invisible element bugs immediately obvious.
- Check the "Network" tab in the trace to find if a slow API call (>2s) caused the auto-wait timeout.

**Tricky Follow-up Questions:**
1. *What is the difference between a trace screenshot and a trace snapshot? When is each useful?*
2. *How would you automatically attach the trace file to a bug tracking system (e.g., Jira) when a CI test fails?*

---

## Q20: How do you configure screenshots, video recording, and logging in Playwright?

**Answer:**
- **Screenshots**: `screenshot: 'on' | 'off' | 'only-on-failure'` — or `page.screenshot()` manually.
- **Video**: `video: 'on' | 'off' | 'retain-on-failure' | 'on-first-retry'` — saved per test context.
- **Logs**: `page.on('console', ...)` for browser console, `page.on('pageerror', ...)` for uncaught exceptions, plus Playwright's built-in reporters.

**Explanation:**
- These three together form the **observability triad** for test failures — screenshot shows the visual state, video shows the sequence of events, and logs show the runtime errors.
- All are file-based artifacts saved under `test-results/` by default.
- Custom reporters extend observability: JUnit XML for CI, HTML for developers, JSON for dashboards.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  outputDir: 'test-results/',
  reporter: [
    ['list'],                                   // Console output
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'results/junit.xml' }], // For CI integration
    ['json', { outputFile: 'results/results.json' }],
  ],
  use: {
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',
    launchOptions: {
      slowMo: process.env.SLOW_MO ? parseInt(process.env.SLOW_MO) : 0,
    },
  },
});
```

```typescript
// Inline screenshot and logging in tests
import { test, expect } from '@playwright/test';

test('checkout with observability', async ({ page }, testInfo) => {
  // Capture browser console logs
  const consoleLogs: string[] = [];
  page.on('console', (msg) => consoleLogs.push(`[${msg.type()}] ${msg.text()}`));

  // Capture uncaught JS errors
  page.on('pageerror', (err) => {
    console.error('Page error:', err.message);
    testInfo.annotations.push({ type: 'bug', description: err.message });
  });

  await page.goto('/checkout');

  // Manual screenshot at a specific point
  await page.screenshot({ path: `screenshots/checkout-initial.png`, fullPage: true });

  await page.getByRole('button', { name: 'Place Order' }).click();

  // Attach custom data to test report
  await testInfo.attach('checkout-screenshot', {
    body: await page.screenshot(),
    contentType: 'image/png',
  });

  await testInfo.attach('console-logs', {
    body: consoleLogs.join('\n'),
    contentType: 'text/plain',
  });

  await expect(page.getByText('Order confirmed')).toBeVisible();
});
```

```typescript
// Custom logger utility
class TestLogger {
  private logs: Array<{ type: string; message: string; timestamp: string }> = [];

  attachToPage(page: Page): void {
    page.on('console', msg => this.logs.push({
      type: msg.type(),
      message: msg.text(),
      timestamp: new Date().toISOString(),
    }));
    page.on('pageerror', err => this.logs.push({
      type: 'pageerror',
      message: err.message,
      timestamp: new Date().toISOString(),
    }));
  }

  async attachToReport(testInfo: TestInfo): Promise<void> {
    await testInfo.attach('browser-logs', {
      body: JSON.stringify(this.logs, null, 2),
      contentType: 'application/json',
    });
  }
}
```

**Real-world Usage:**
- Enterprise CI pipelines attach HTML reports, JUnit XML (for Jenkins/GitHub Actions), and screenshots as artifacts.
- FinTech QA teams mandate `video: 'retain-on-failure'` — videos serve as audit evidence for failed compliance tests.
- SaaS: Console error logs are attached to Jira tickets automatically via a custom reporter.
- On-call teams use the HTML report's screenshot + video to triage production-impacting failures without re-running.

**Common Mistakes:**
- Using `video: 'on'` in full regression runs — generates gigabytes of video for thousands of passing tests.
- Not attaching artifacts to `testInfo` — files exist on disk but are not linked in the HTML report.
- Ignoring `page.on('pageerror', ...)` — uncaught JS exceptions are a common source of silent test failures where the page looks fine visually.

**Optimization Tip:**
- Use `testInfo.status` to conditionally capture screenshots only on failure within a test:
  ```typescript
  test.afterEach(async ({ page }, testInfo) => {
    if (testInfo.status !== 'passed') {
      await testInfo.attach('failure-screenshot', {
        body: await page.screenshot({ fullPage: true }),
        contentType: 'image/png',
      });
    }
  });
  ```

**Debugging Strategy:**
- If a test passes locally but fails in CI, compare HTML reports: screenshots reveal environment-specific rendering differences.
- Browser console logs (`pageerror`, `warning`) often reveal the root cause of a test failure faster than DOM inspection.

**Tricky Follow-up Questions:**
1. *How do you customize the HTML report to include your own metadata (e.g., environment name, build number, Jira ticket ID)?*
2. *What is the difference between `testInfo.attach()` and saving a file to `testInfo.outputDir`?*

---

## Q21: How do you implement retry strategy and handle flaky tests in Playwright?

**Answer:**
- Playwright supports **automatic test retries** via `retries` in `playwright.config.ts` or `test.describe`/`test` level.
- On retry, Playwright creates a fresh browser context — it does not reuse the failed state.
- `testInfo.retry` gives the current retry index; `testInfo.status` gives `'failed'`, `'passed'`, `'timedOut'`, `'skipped'`.
- Flaky tests are identified via the `--reporter=html` report and addressed through root cause analysis, not just retries.

**Explanation:**
- Retries are a **safety net**, not a fix. Over-relying on retries masks genuine reliability problems.
- Best practice: `retries: 0` locally (surface failures immediately), `retries: 2` in CI (tolerate environment flakiness).
- `test.fixme()` quarantines a known-flaky test while a fix is in progress.
- `test.slow()` triples the timeout for genuinely slow tests — prevents false flakiness.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts
export default defineConfig({
  retries: process.env.CI ? 2 : 0,
  use: {
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
    trace: 'on-first-retry',      // Record trace only on retry
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
});
```

```typescript
import { test, expect } from '@playwright/test';

// Retry a specific describe block
test.describe('Payment Integration', () => {
  test.describe.configure({ retries: 3 }); // Override for external API tests

  test('process payment', async ({ page }) => { /* ... */ });
});

// Mark a test as flaky while investigating
test.fixme('order history pagination - flaky on Firefox', async ({ page }) => {
  // Will be skipped and reported as 'fixme'
});

// Slow test — triples timeout
test.slow('generate large financial report', async ({ page }) => {
  await page.goto('/reports/annual');
  await expect(page.getByTestId('report-table')).toBeVisible();
  // Timeout is tripled: default 30s → 90s
});

// Retry-aware test logic
test('resilient payment flow', async ({ page }, testInfo) => {
  if (testInfo.retry > 0) {
    console.log(`Retrying test (attempt ${testInfo.retry + 1})`);
    // Clean up state from previous failed attempt
    await page.context().clearCookies();
  }

  await page.goto('/checkout');
  await page.getByRole('button', { name: 'Pay Now' }).click();
  await expect(page.getByText('Payment confirmed')).toBeVisible();
});

// Custom retry utility for flaky API calls within tests
async function retryRequest<T>(
  fn: () => Promise<T>,
  maxRetries = 3,
  delayMs = 1000
): Promise<T> {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await fn();
    } catch (err) {
      if (attempt === maxRetries - 1) throw err;
      await new Promise(resolve => setTimeout(resolve, delayMs * (attempt + 1)));
    }
  }
  throw new Error('Unreachable');
}
```

**Real-world Usage:**
- FinTech CI pipelines with `retries: 2` tolerate third-party payment gateway intermittency.
- Enterprise frameworks track retry rates per test over time — tests with >10% retry rate are quarantined for refactoring.
- `test.fixme()` is used in sprint planning to mark tests broken by a known bug — prevents blocking the CI pipeline.

**Common Mistakes:**
- Setting `retries: 5` as a global default — hides real failures and makes CI run 5x slower for genuinely broken tests.
- Not enabling `trace: 'on-first-retry'` — losing debug info on the first failure that triggered the retry.
- Using `waitForTimeout` to "fix" flakiness — the underlying timing issue is still there, just less likely to surface.
- Not resetting state between retries — second retry fails due to partial state from the first attempt.

**Optimization Tip:**
- Build a **flakiness dashboard**: track which tests retry most frequently using the JSON reporter output. Tests with `>5%` retry rate get prioritized for refactoring.

**Debugging Strategy:**
- Use `PWDEBUG=console` to get verbose Playwright logs without opening the inspector UI.
- Run a suspected flaky test 20 times in a loop: `npx playwright test --repeat-each=20` to make intermittent failures reproducible.

**Tricky Follow-up Questions:**
1. *Is a test that always passes on retry considered flaky? What metrics would you use to define "flaky"?*
2. *How do you prevent a retry from cascading and causing a previously unrelated test to fail due to shared resource state?*

---

## Q22: What is the difference between headless and headed execution in Playwright? When do you use each?

**Answer:**
- **Headless**: Browser runs without a visible UI window. Faster, uses less memory, ideal for CI.
- **Headed**: Browser renders a visible window. Required for debugging, screen-dependent behavior, and extension-based testing.
- Configured via `headless: false` in `use.launchOptions` or via `--headed` CLI flag.

**Explanation:**
- Headless is not the same as "no rendering" — the browser still fully renders the page in an off-screen buffer.
- Some web features behave differently in headless mode: focus events, certain WebGL operations, browser extensions, and clipboard access.
- Playwright's headless mode uses proper browser headless (not the legacy Chrome `--headless=old`) for accurate rendering.
- A test that passes headed but fails headless usually involves focus handling, scroll-dependent visibility, or geolocation permissions.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    headless: !process.env.HEADED,  // Headed locally when env var is set
    launchOptions: {
      slowMo: process.env.SLOW_MO ? 100 : 0, // Slow motion for debugging
      devtools: process.env.DEVTOOLS === 'true', // Open DevTools
    },
  },
});
```

```typescript
// Run specific test in headed mode
// npx playwright test --headed --project=chromium

// Programmatically control launch options
test('extension test requires headed', async ({ browser }) => {
  const context = await browser.newContext({
    // Extensions don't work in headless mode
  });
  // ...
});

// Geolocation — requires headed for some browser/OS combinations
test('location-based feature', async ({ page, context }) => {
  await context.grantPermissions(['geolocation']);
  await context.setGeolocation({ latitude: 40.7128, longitude: -74.0060 });
  await page.goto('/nearby-stores');
  await expect(page.getByText('New York')).toBeVisible();
});
```

**Real-world Usage:**
- CI always runs headless (Jenkins, GitHub Actions, GitLab) — no display server available.
- Local debugging: `HEADED=1 npx playwright test --debug` to watch the test execute step by step.
- Visual regression tests sometimes require headed mode to ensure font rendering matches screenshots.

**Common Mistakes:**
- Assuming headless and headed are always equivalent — clipboard, focus events, and some browser extensions differ.
- Running headed in CI without a virtual display (Xvfb on Linux) — browser crashes.
- Using `--devtools` in CI — opens DevTools UI which may block test execution.

**Optimization Tip:**
- Use environment variables to switch between modes without changing config files:
  ```bash
  HEADED=1 SLOW_MO=1 npx playwright test tests/checkout.spec.ts --project=chromium
  ```

**Debugging Strategy:**
- Add `slowMo: 500` to see each action execute with a 500ms delay — makes it easy to spot where a test goes wrong visually.
- Use `page.pause()` to pause execution in headed+debug mode and use Playwright Inspector to step through.

**Tricky Follow-up Questions:**
1. *What changed between Chrome's legacy `--headless` mode and the new headless mode, and why does Playwright use the new one?*
2. *How would you run visual regression tests in a way that ensures pixel-perfect consistency between local and CI environments?*

---

## Q23: How do you implement cross-browser testing in Playwright?

**Answer:**
- Playwright natively supports **Chromium, Firefox, and WebKit (Safari)** — no additional drivers or setup needed.
- Cross-browser configuration is done via `projects` in `playwright.config.ts`, each specifying `browserName`.
- Tests run across all projects unless filtered with `--project=chromium`.

**Explanation:**
- Playwright ships its own browser binaries, ensuring version consistency regardless of the host machine.
- Each project is independent — a test can pass on Chromium and fail on WebKit, making browser-specific bugs immediately identifiable.
- Project inheritance allows sharing `use` configuration while overriding browser-specific settings.
- Mobile browser emulation uses the same engine as desktop but with device profiles.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts — full cross-browser setup
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  fullyParallel: true,
  retries: 2,

  projects: [
    // Desktop browsers
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },

    // Mobile browsers
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 7'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 15 Pro'] },
    },

    // Branded browsers
    {
      name: 'edge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' },
    },
    {
      name: 'chrome',
      use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    },
  ],
});
```

```typescript
// Skip test for specific browsers
import { test, expect } from '@playwright/test';

test('WebGL visualization', async ({ page, browserName }) => {
  test.skip(browserName === 'firefox', 'WebGL feature not supported on Firefox yet');
  
  await page.goto('/charts/3d');
  await expect(page.getByTestId('webgl-canvas')).toBeVisible();
});

// Browser-specific assertion
test('CSS grid rendering', async ({ page, browserName }) => {
  await page.goto('/layout');
  
  const expectedGap = browserName === 'webkit' ? '16px' : '20px';
  await expect(page.getByTestId('grid')).toHaveCSS('gap', expectedGap);
});

// Run only on specific browsers
test.describe('Safari-only tests', () => {
  test.skip(({ browserName }) => browserName !== 'webkit');
  
  test('iOS scroll behavior', async ({ page }) => { /* ... */ });
});
```

**Real-world Usage:**
- Banks and FinTech platforms mandate Safari (WebKit) testing — macOS/iOS users are a major demographic.
- SaaS dashboards run smoke tests on all 3 browsers + 2 mobile devices during release pipelines.
- Enterprise: Chromium for fast iteration, Firefox + WebKit only in nightly/release builds to reduce CI time.

**Common Mistakes:**
- Assuming Chromium-passing tests pass everywhere — CSS rendering, font metrics, touch events, and specific Web APIs differ.
- Not using `devices['Desktop Chrome']` spread — missing viewport, user agent, and device scale factor.
- Running the full 5-browser matrix on every commit — use browser-filtered CI stages (smoke on all, regression on Chromium only).

**Optimization Tip:**
- Use a tiered strategy:
  - **PR builds**: Chromium only (fast feedback)
  - **Merge to main**: Chromium + Firefox + WebKit
  - **Release builds**: All browsers + mobile

**Debugging Strategy:**
- When a test fails on WebKit only, run with `--project=webkit --headed` to observe the rendering difference.
- Check Playwright's known browser limitations page for Safari-specific issues (indexedDB, clipboard, camera).

**Tricky Follow-up Questions:**
1. *What are three real differences between Chromium and WebKit that commonly cause test failures, and how do you handle them?*
2. *How do Playwright's bundled browser versions relate to the actual Safari or Chrome version used by real users?*

---

## Q24: How does Playwright support mobile emulation?

**Answer:**
- Playwright emulates mobile devices using device descriptors from `playwright.devices` — containing viewport size, user agent, device pixel ratio, touch support, and orientation.
- Mobile emulation is configured per-project or per-context using `devices['iPhone 15 Pro']` etc.
- It does not run a real mobile OS — it emulates mobile characteristics in a desktop browser engine.

**Explanation:**
- Device emulation affects: viewport dimensions, DPR (device pixel ratio), `navigator.userAgent`, touch event support, pointer type (`coarse` vs `fine`), and `window.screen` dimensions.
- Touch gestures are simulated using `page.touchscreen.tap()` or locator-level tap actions.
- Network throttling and CPU throttling simulate mobile network conditions.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect, devices } from '@playwright/test';

// Config-level mobile emulation
// playwright.config.ts
projects: [
  { name: 'iPhone', use: { ...devices['iPhone 15 Pro'] } },
  { name: 'Pixel', use: { ...devices['Pixel 7'] } },
  { name: 'iPad', use: { ...devices['iPad Pro 11'] } },
]

// Per-context custom emulation
test('custom mobile viewport', async ({ browser }) => {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 ...)',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
  });
  const page = await context.newPage();
  await page.goto('/mobile-menu');

  // Mobile-specific hamburger menu
  await page.getByRole('button', { name: 'Menu' }).tap();
  await expect(page.getByRole('navigation')).toBeVisible();
});

// Network throttling for mobile conditions
test('mobile on 3G network', async ({ browser }) => {
  const context = await browser.newContext({
    ...devices['Pixel 7'],
  });
  const page = await context.newPage();

  // Enable CDP network throttling
  const client = await context.newCDPSession(page);
  await client.send('Network.emulateNetworkConditions', {
    offline: false,
    downloadThroughput: 375 * 1024 / 8,  // 375 Kbps (3G)
    uploadThroughput: 375 * 1024 / 8,
    latency: 100,
  });

  const startTime = Date.now();
  await page.goto('/');
  const loadTime = Date.now() - startTime;
  expect(loadTime).toBeLessThan(10_000); // Page loads within 10s on 3G
});

// Touch interaction
test('swipe carousel on mobile', async ({ page }) => {
  await page.goto('/products');
  
  const carousel = page.getByTestId('product-carousel');
  const box = await carousel.boundingBox();
  if (!box) throw new Error('Carousel not found');

  // Simulate swipe left
  await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
});
```

**Real-world Usage:**
- E-commerce: Mobile checkout flow testing (responsive layout, touch-friendly buttons, virtual keyboard).
- FinTech: Mobile banking app testing (e.g., PWA) — ensuring charts are readable, modals scroll correctly on small viewports.
- SaaS: Verifying responsive design breakpoints and mobile-specific navigation patterns.

**Common Mistakes:**
- Assuming mobile emulation tests the native mobile browser — it uses the desktop engine with mobile characteristics; Safari on iOS is different from WebKit with iPhone emulation.
- Not setting `isMobile: true` — omitting this skips touch event dispatch and media query targeting.
- Using pixel-hardcoded assertions for element sizes that differ across device DPRs.

**Optimization Tip:**
- Use a `mobile` test project that runs only responsiveness-critical tests — avoid running the full 500-test suite against every mobile device profile.

**Debugging Strategy:**
- Run `--headed` with a mobile device profile to see the exact viewport experience.
- Check `page.evaluate(() => window.navigator.userAgent)` and `page.viewportSize()` to confirm the emulation is active.

**Tricky Follow-up Questions:**
1. *What is Device Pixel Ratio (DPR) and how does a high DPR affect Playwright screenshot comparisons in visual regression tests?*
2. *How would you test a feature that shows a native app banner ("Open in App") only on real iOS, and not in emulation?*

---

## Q25: How do you implement accessibility testing in Playwright?

**Answer:**
- Playwright integrates with **`@axe-core/playwright`** (the axe accessibility rules engine) to run automated WCAG compliance checks.
- Built-in accessible locators (`getByRole`, `getByLabel`, `getByAltText`) validate ARIA semantics implicitly during test execution.
- `page.accessibility.snapshot()` captures the accessibility tree for assertions.

**Explanation:**
- Automated accessibility testing catches ~30-40% of WCAG violations (axe-core's estimate). Manual testing is required for the rest.
- Axe-core runs JavaScript analysis against the live DOM and returns violations categorized by WCAG level (A, AA, AAA) and impact (critical, serious, moderate, minor).
- Combining `getByRole` locators + axe-core provides both functional and structural accessibility validation.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.describe('Accessibility Tests', () => {
  test('login page has no critical violations', async ({ page }) => {
    await page.goto('/login');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();

    // Assert no violations
    expect(results.violations).toHaveLength(0);
  });

  test('dashboard accessibility with exclusions', async ({ page }) => {
    await page.goto('/dashboard');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2aa'])
      .exclude('#third-party-widget') // Known third-party violation
      .disableRules(['color-contrast'])  // Handle separately
      .analyze();

    const criticalViolations = results.violations.filter(v => v.impact === 'critical');
    expect(criticalViolations).toHaveLength(0);
  });

  test('dynamic content remains accessible', async ({ page }) => {
    await page.goto('/notifications');

    // Check before state
    const beforeResults = await new AxeBuilder({ page }).analyze();

    // Trigger dynamic content
    await page.getByRole('button', { name: 'Load More' }).click();
    await page.waitForLoadState('networkidle');

    // Check after state — new content must also be accessible
    const afterResults = await new AxeBuilder({ page }).analyze();
    expect(afterResults.violations).toHaveLength(beforeResults.violations.length);
  });
});

// Accessibility tree snapshot
test('form has correct structure', async ({ page }) => {
  await page.goto('/contact');

  const snapshot = await page.accessibility.snapshot();
  expect(snapshot?.children).toContainEqual(
    expect.objectContaining({
      role: 'form',
      name: 'Contact Us',
    })
  );
});
```

**Real-world Usage:**
- Government and public sector SaaS products mandate WCAG 2.1 AA compliance — axe-core assertions are part of every PR pipeline.
- FinTech: WCAG compliance for screen-reader users is both a legal requirement (ADA, EAA) and a business requirement.
- Accessibility violations are reported to Jira automatically using a custom Playwright reporter.

**Common Mistakes:**
- Treating axe-core passing as "fully accessible" — it only detects automatable rules (~35% of WCAG).
- Running axe-core on every test — slow and redundant. Run dedicated a11y tests in a separate project.
- Not excluding known third-party widget violations — causes the entire suite to fail on issues you don't own.

**Optimization Tip:**
- Run accessibility checks as a separate project in CI that only triggers on main branch merges:
  ```typescript
  projects: [
    { name: 'a11y', testMatch: '**/*.a11y.spec.ts' }
  ]
  ```

**Debugging Strategy:**
- Print violation details with `violations.forEach(v => console.log(v.id, v.description, v.nodes.map(n => n.html)))` to see exactly which DOM nodes are failing.

**Tricky Follow-up Questions:**
1. *How do you test keyboard navigation (Tab order, focus trapping in modals) that axe-core cannot detect?*
2. *What is the difference between WCAG 2.1 AA and WCAG 2.2, and which level should enterprise products target in 2025?*

---

## Q26: How do you implement visual regression testing in Playwright?

**Answer:**
- Playwright has built-in visual comparison via `expect(page).toHaveScreenshot()` and `expect(element).toHaveScreenshot()`.
- Screenshots are compared pixel-by-pixel against **baseline images** stored in `__screenshots__/` directories.
- On first run, baselines are created. Subsequent runs diff against the baseline and fail if differences exceed the configured threshold.

**Explanation:**
- Visual regression catches CSS regressions, layout shifts, and rendering changes that functional tests miss.
- `toHaveScreenshot()` supports thresholds: `maxDiffPixels`, `maxDiffPixelRatio`, and `threshold` (per-pixel color difference).
- Animated content, dynamic dates, and ads must be masked to prevent false positives.
- Baselines are environment-specific — screenshots from macOS @ 2x DPR differ from Linux CI @ 1x DPR.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('homepage visual regression', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');

  // Full page screenshot
  await expect(page).toHaveScreenshot('homepage.png', {
    maxDiffPixelRatio: 0.02, // Allow 2% pixel difference
    fullPage: true,
  });
});

test('component-level visual regression', async ({ page }) => {
  await page.goto('/components/button-showcase');

  const buttonGroup = page.getByTestId('primary-buttons');
  await expect(buttonGroup).toHaveScreenshot('primary-buttons.png', {
    maxDiffPixels: 100,
  });
});

test('visual regression with dynamic content masked', async ({ page }) => {
  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');

  await expect(page).toHaveScreenshot('dashboard.png', {
    mask: [
      page.getByTestId('live-ticker'),    // Mask real-time price ticker
      page.getByTestId('timestamp'),       // Mask timestamps
      page.locator('.ad-banner'),          // Mask ads
      page.getByTestId('user-avatar'),     // Mask user-specific content
    ],
    maxDiffPixelRatio: 0.01,
  });
});

// Update baselines: npx playwright test --update-snapshots
```

```typescript
// playwright.config.ts — visual regression configuration
export default defineConfig({
  expect: {
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.02,
      threshold: 0.2,        // Per-pixel color diff threshold (0–1)
      animations: 'disabled', // Disable CSS animations for stable screenshots
    },
  },
  snapshotPathTemplate: '{testDir}/__screenshots__/{projectName}/{testFilePath}/{arg}{ext}',
});
```

**Real-world Usage:**
- Design system teams run visual regression on every component PR to catch unintentional style changes.
- FinTech: Trading chart and portfolio visualization screenshots are diffed to detect rendering regressions in chart libraries.
- SaaS: Login page / marketing landing page visual tests prevent accidental brand guideline violations.

**Common Mistakes:**
- Committing Linux CI baselines and running comparison on macOS (or vice versa) — font rendering differences cause false failures.
- Not masking dynamic content (timestamps, live data, user avatars) — tests are always "failing" due to content changes.
- Setting `maxDiffPixelRatio: 0` — any sub-pixel antialiasing difference fails the test.

**Optimization Tip:**
- Run visual regression tests in a **dedicated Docker container** in CI to ensure consistent rendering (exact same OS, font packages, display DPR) as the baseline.

**Debugging Strategy:**
- Failed visual tests generate a diff image in `test-results/` — the diff highlights changed pixels in red. Open the HTML report to view side-by-side baseline vs actual vs diff.
- Use `--update-snapshots` to update baselines after an intentional visual change.

**Tricky Follow-up Questions:**
1. *How do you manage baseline screenshots across teams where developers use different OS/DPR displays?*
2. *What is the difference between Playwright's built-in `toHaveScreenshot` and using a third-party tool like Percy or Chromatic?*

---

## Q27: How do you interact with Shadow DOM elements in Playwright?

**Answer:**
- Playwright **automatically pierces Shadow DOM** — CSS selectors and most locator methods work across shadow boundaries without any special syntax.
- `page.locator('css-selector')` traverses shadow roots natively.
- This is a major advantage over Selenium where shadow DOM requires JavaScript execution (`executeScript`) to access shadow roots.

**Explanation:**
- Web Components encapsulate their DOM inside a shadow root — standard `querySelector` stops at shadow boundaries.
- Playwright's engine-level CSS matching crosses shadow roots automatically.
- `>>` (deep combinator) in older Playwright versions is deprecated — the native CSS selector now handles this.
- Some older frameworks (Polymer, older LitElement) use closed shadow roots — these are inaccessible even to Playwright.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('interact with web component shadow DOM', async ({ page }) => {
  await page.goto('/components');

  // Playwright auto-pierces shadow DOM — no special syntax needed
  const input = page.locator('my-input-component input[type="text"]');
  await input.fill('Hello Shadow DOM');

  // Click button inside a shadow root
  await page.locator('my-button-component button').click();

  // Nested shadow root
  await page.locator('app-shell nav-drawer drawer-item a').click();

  // Using getByRole across shadow boundary
  await page.getByRole('textbox', { name: 'Search' }).fill('products');
});

test('shadow DOM with data-testid', async ({ page }) => {
  await page.goto('/app');

  // data-testid works inside shadow DOM
  const submitBtn = page.getByTestId('submit-button');
  await expect(submitBtn).toBeEnabled();
  await submitBtn.click();
});

// When you need to access the shadow root explicitly via JS
test('read shadow DOM property directly', async ({ page }) => {
  await page.goto('/web-components');

  const value = await page.evaluate(() => {
    const host = document.querySelector('my-custom-input') as Element & { shadowRoot: ShadowRoot };
    const input = host.shadowRoot?.querySelector('input');
    return input?.value;
  });

  expect(value).toBe('expected-value');
});

test('detect closed shadow root limitation', async ({ page }) => {
  const isClosed = await page.evaluate(() => {
    const host = document.querySelector('closed-shadow-host') as Element;
    return (host as any).shadowRoot === null;
  });
  
  if (isClosed) {
    console.warn('Closed shadow root detected — element not accessible via Playwright locators');
  }
});
```

**Real-world Usage:**
- Enterprise Design System tests: Angular Material, Ionic, Shoelace, and custom web components all use shadow DOM.
- SAP Fiori and Salesforce Lightning Experience are heavily component-based with shadow DOM.
- FinTech trading terminals often built on LitElement/StencilJS web components.

**Common Mistakes:**
- Using the deprecated `>>` deep combinator syntax — no longer needed and may break in future versions.
- Attempting to locate shadow DOM elements by passing `{ strict: false }` — shadow DOM piercing is always on.
- Expecting `getByText` to work inside a closed shadow root — only open shadow roots are accessible.

**Optimization Tip:**
- Advocate for `data-testid` attributes on web component host elements and inside shadow roots during design phase — this makes selectors stable regardless of internal HTML restructuring.

**Debugging Strategy:**
- In Chrome DevTools, check "Show user agent shadow DOM" to inspect shadow roots. If an element isn't accessible, check if it uses `attachShadow({ mode: 'closed' })`.

**Tricky Follow-up Questions:**
1. *What is the difference between "open" and "closed" shadow roots, and what are your options when you encounter a closed shadow root?*
2. *How do Playwright's CSS selectors traverse multiple nested shadow roots, and is there a depth limit?*

---

## Q28: How do you implement contract testing concepts in Playwright API tests?

**Answer:**
- **Contract testing** verifies that an API response conforms to an agreed schema/contract between consumer (frontend) and provider (backend).
- In Playwright, contract testing is implemented using schema validation libraries (`zod`, `ajv`, `joi`) within `APIRequestContext` tests.
- Consumer-driven contracts define what fields the UI expects; provider tests verify the backend delivers them.

**Explanation:**
- Full contract testing platforms (Pact) are the industry standard, but schema validation in Playwright API tests provides lightweight consumer-side contract validation.
- Schema assertions ensure the backend hasn't removed/renamed a field the UI depends on — catching breaking changes before they reach E2E tests.
- Playwright's `expect().toMatchObject()` provides partial contract matching.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';
import { z } from 'zod';

// Define schemas (consumer contract definitions)
const UserSchema = z.object({
  id: z.string().regex(/^usr-/),
  email: z.string().email(),
  name: z.string().min(1),
  role: z.enum(['admin', 'user', 'readonly']),
  createdAt: z.string().datetime(),
  profile: z.object({
    avatar: z.string().url().nullable(),
    timezone: z.string(),
  }),
});

const PaginatedUsersSchema = z.object({
  data: z.array(UserSchema),
  meta: z.object({
    total: z.number().int().nonnegative(),
    page: z.number().int().positive(),
    limit: z.number().int().positive(),
  }),
});

type User = z.infer<typeof UserSchema>;

test.describe('User API Contract Tests', () => {
  test('GET /api/users conforms to consumer contract', async ({ request }) => {
    const response = await request.get('/api/users', {
      params: { page: 1, limit: 10 },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();

    // Schema validation — throws detailed error if contract violated
    const parsed = PaginatedUsersSchema.safeParse(body);
    expect(parsed.success, `Contract violation: ${JSON.stringify(parsed.error?.flatten())}`).toBe(true);
  });

  test('POST /api/users returns created user matching contract', async ({ request }) => {
    const payload = { name: 'Test User', email: `test-${Date.now()}@example.com`, role: 'user' };

    const response = await request.post('/api/users', { data: payload });
    expect(response.status()).toBe(201);

    const user = await response.json();
    const parsed = UserSchema.safeParse(user);
    expect(parsed.success, `Response failed contract: ${JSON.stringify(parsed.error?.flatten())}`).toBe(true);

    // Cleanup
    await request.delete(`/api/users/${user.id}`);
  });

  test('backwards compatibility — legacy fields still present', async ({ request }) => {
    const response = await request.get('/api/users/usr-001');
    const user = await response.json();

    // Partial contract — assert only consumed fields exist
    expect(user).toMatchObject({
      id: expect.stringMatching(/^usr-/),
      email: expect.stringContaining('@'),
      name: expect.any(String),
    });
  });
});
```

**Real-world Usage:**
- FinTech: The trading API team and UI team define a formal `OrderSchema` — both validate against it in their respective tests.
- SaaS microservices: Consumer-driven contract tests run in CI to detect breaking API changes before deployment.
- Teams transitioning to Pact start with Playwright schema validation as a stepping stone.

**Common Mistakes:**
- Only using `toMatchObject` without schema validation — doesn't catch incorrect types (number where string expected) or missing optional fields that the UI renders.
- Skipping contract tests because E2E tests "cover it" — E2E tests use the full stack; contract tests isolate the interface.
- Not versioning schemas — breaking a schema silently passes old tests.

**Optimization Tip:**
- Export schemas as a shared npm package or TypeScript module so both frontend tests and backend unit tests import the same source of truth.

**Debugging Strategy:**
- Zod's `safeParse` returns a structured error: `error.flatten()` shows exactly which field failed and why — far more useful than a generic assertion failure.

**Tricky Follow-up Questions:**
1. *What is the difference between consumer-driven contract testing (Pact) and provider schema validation? When would you choose Pact over Playwright-based schema tests?*
2. *How do you handle optional API fields that may or may not be present depending on user role or feature flag?*

---

## Q29: How do you handle token-based authentication (JWT / OAuth 2.0) in Playwright tests?

**Answer:**
- For JWT: Inject tokens directly into `localStorage`, `sessionStorage`, or cookies using `addInitScript` or `context.addCookies()` — skipping UI login entirely.
- For OAuth 2.0: Use API-based token acquisition (client credentials flow) and inject the resulting token into the browser context.
- For MFA/SSO: Use a dedicated test account with MFA disabled, or acquire tokens directly from the auth server via API.

**Explanation:**
- UI-based OAuth flows are slow, fragile, and may be blocked by bot detection.
- Playwright supports acquiring tokens programmatically via `APIRequestContext` before the browser context is created.
- `storageState` serializes injected auth tokens so they persist across tests without re-injection.
- PKCE flows can be automated but require simulating the redirect URI handling.

**JavaScript / TypeScript Example:**
```typescript
// global-setup.ts — acquire OAuth token and store in state
import { chromium, FullConfig, request } from '@playwright/test';

async function globalSetup(config: FullConfig): Promise<void> {
  // Acquire JWT via client credentials (machine-to-machine)
  const apiContext = await request.newContext();
  const tokenResponse = await apiContext.post('https://auth.example.com/oauth/token', {
    form: {
      grant_type: 'client_credentials',
      client_id: process.env.CLIENT_ID!,
      client_secret: process.env.CLIENT_SECRET!,
      scope: 'read:users write:orders',
    },
  });

  const { access_token } = await tokenResponse.json();

  // Inject token via browser context
  const browser = await chromium.launch();
  const context = await browser.newContext({
    baseURL: config.projects[0].use.baseURL as string,
  });

  const page = await context.newPage();
  await page.goto('/');

  // Inject into localStorage (for JWT SPA apps)
  await page.evaluate((token: string) => {
    localStorage.setItem('access_token', token);
    localStorage.setItem('token_type', 'Bearer');
  }, access_token);

  await context.storageState({ path: 'auth/oauth-user.json' });
  await browser.close();
  await apiContext.dispose();
}

export default globalSetup;
```

```typescript
// Tests using injected OAuth token
import { test, expect } from '@playwright/test';

// playwright.config.ts sets storageState: 'auth/oauth-user.json'
test('authenticated API request with token from storage state', async ({ page, request }) => {
  // UI is pre-authenticated
  await page.goto('/dashboard');
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

  // Read token from browser context for API calls
  const token = await page.evaluate(() => localStorage.getItem('access_token'));
  
  const apiResponse = await request.get('/api/orders', {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(apiResponse.ok()).toBeTruthy();
});

// Testing token expiry behavior
test('expired token redirects to login', async ({ context, page }) => {
  // Inject expired JWT
  await context.addCookies([{
    name: 'session',
    value: 'expired.jwt.token',
    domain: 'example.com',
    path: '/',
  }]);

  await page.goto('/dashboard');
  await expect(page).toHaveURL('/login');
  await expect(page.getByText('Session expired')).toBeVisible();
});

// Testing role-based access with different tokens
test('viewer role cannot access admin panel', async ({ browser }) => {
  const context = await browser.newContext({ storageState: 'auth/viewer.json' });
  const page = await context.newPage();

  await page.goto('/admin');
  await expect(page).toHaveURL('/forbidden');
  await context.close();
});
```

**Real-world Usage:**
- FinTech microservices use client credentials OAuth — Playwright acquires tokens in `globalSetup` and distributes via `storageState`.
- B2B SaaS platforms with SSO (Okta, Auth0, Azure AD) — Playwright acquires tokens via ROPC (Resource Owner Password Credentials) flow for test accounts.
- Testing token refresh: Playwright intercepts the `/oauth/token` refresh call and validates it fires before expiry.

**Common Mistakes:**
- UI-based OAuth login in every test — 5-10 seconds per test, multiplied across hundreds of tests.
- Hardcoding tokens in test files — tokens expire and security risk. Use environment variables + `globalSetup`.
- Not handling token refresh — long test runs fail mid-run when tokens expire.
- Storing `client_secret` in test files — major security vulnerability. Use CI secrets manager.

**Optimization Tip:**
- Check token expiry before each test run and refresh if needed:
  ```typescript
  function isTokenExpired(jwt: string): boolean {
    const payload = JSON.parse(Buffer.from(jwt.split('.')[1], 'base64').toString());
    return payload.exp * 1000 < Date.now() + 60_000; // Refresh 1 min before expiry
  }
  ```

**Debugging Strategy:**
- Decode JWT token with `jwt-decode` and log claims to verify correct scope/role is present.
- Use `page.on('request', ...)` to verify `Authorization: Bearer <token>` header is present on API calls.

**Tricky Follow-up Questions:**
1. *How do you test an OAuth PKCE flow where the app needs to handle the authorization code redirect?*
2. *How would you test that a refresh token is correctly used to acquire a new access token without requiring the user to re-login?*

---

## Q30: How do you integrate Playwright tests into a CI/CD pipeline (GitHub Actions / Jenkins)?

**Answer:**
- Playwright provides official GitHub Actions support via `actions/setup-node` + `npx playwright install --with-deps`.
- Tests run in headless mode on Linux CI agents without additional configuration.
- HTML reports, traces, and screenshots are published as CI artifacts for failure analysis.
- Sharding distributes tests across parallel CI jobs for large suites.

**Explanation:**
- Linux CI agents require browser dependencies — `--with-deps` installs OS-level libraries (fonts, codecs, sandboxes).
- `npx playwright install chromium` speeds up CI by only installing the browser needed.
- Docker-based execution ensures reproducible environments across all CI agents.
- Environment secrets (credentials, tokens) are injected via CI environment variables.

**JavaScript / TypeScript Example:**
```yaml
# .github/workflows/playwright.yml
name: Playwright Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    timeout-minutes: 30
    runs-on: ubuntu-latest
    
    strategy:
      fail-fast: false
      matrix:
        shard: [1, 2, 3, 4]  # 4 parallel shards
    
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Install Playwright browsers
        run: npx playwright install chromium --with-deps

      - name: Run Playwright tests (shard ${{ matrix.shard }}/4)
        run: npx playwright test --shard=${{ matrix.shard }}/4
        env:
          BASE_URL: ${{ secrets.STAGING_URL }}
          ADMIN_EMAIL: ${{ secrets.ADMIN_EMAIL }}
          ADMIN_PASSWORD: ${{ secrets.ADMIN_PASSWORD }}
          CLIENT_ID: ${{ secrets.OAUTH_CLIENT_ID }}
          CLIENT_SECRET: ${{ secrets.OAUTH_CLIENT_SECRET }}
          CI: true

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-results-${{ matrix.shard }}
          path: |
            playwright-report/
            test-results/
          retention-days: 30

  merge-reports:
    needs: test
    if: always()
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - name: Download all shard reports
        uses: actions/download-artifact@v4
        with:
          path: all-blob-reports
          pattern: playwright-results-*
      - name: Merge and generate HTML report
        run: npx playwright merge-reports --reporter html ./all-blob-reports/**/playwright-report
      - name: Publish merged HTML report
        uses: actions/upload-artifact@v4
        with:
          name: playwright-final-report
          path: playwright-report/
```

```typescript
// playwright.config.ts — CI-aware configuration
export default defineConfig({
  workers: process.env.CI ? 2 : undefined,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI
    ? [['blob'], ['github'], ['junit', { outputFile: 'results/junit.xml' }]]
    : [['html', { open: 'on-failure' }]],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
});
```

**Real-world Usage:**
- Enterprise: 4-shard GitHub Actions matrix reduces 800-test suite runtime from 40m to 12m.
- FinTech: Jenkins pipeline with Playwright in Docker ensures exact same environment across all agents.
- SaaS: Playwright tests gate deployments — push to production only if tests pass on staging.
- Nightly cross-browser runs trigger on schedule: `on: schedule: cron: '0 2 * * *'`.

**Common Mistakes:**
- Not running `npx playwright install --with-deps` — browsers install without required OS libraries, causing cryptic startup failures.
- Hardcoding `BASE_URL` in config instead of using environment variables — tests always run against production.
- Not uploading artifacts with `if: always()` — on failure, CI deletes the job workspace including reports.
- Running all browsers on every PR — use Chromium-only for PRs, full matrix for main/release.

**Optimization Tip:**
- Use `blob` reporter in sharded runs and `merge-reports` in a post-job step — the merged HTML report contains all shards in a single navigable report.

**Debugging Strategy:**
- Add `npx playwright show-report` as a final step in local development after a failed run.
- For CI failures, download the artifact ZIP and run `npx playwright show-report playwright-report/` locally.

**Tricky Follow-up Questions:**
1. *How do you prevent Playwright tests from running against a production environment if someone accidentally sets `BASE_URL=https://production.app.com` in CI?*
2. *What is the `github` reporter and what GitHub-specific features does it enable?*

---

*— End of Q16–Q30 —*

---

## Q31: How do you run Playwright tests inside Docker containers?

**Answer:**
- Use Playwright's official Docker image (`mcr.microsoft.com/playwright`) which includes all browsers and system dependencies pre-installed.
- Mount the project directory as a volume, run tests with `npx playwright test`, and copy artifacts out.
- Docker ensures identical execution environments across all CI agents and developer machines.

**Explanation:**
- The biggest source of CI inconsistency is the host OS — Docker eliminates this by pinning the OS, browser versions, and font packages.
- Playwright's official images are tagged by version (`v1.44.0-jammy`) ensuring reproducibility.
- Running in Docker enables testing in CI systems without browser dependencies installed (bare cloud VMs).
- Multi-stage Docker builds separate the test runner image from the application image.

**JavaScript / TypeScript Example:**
```dockerfile
# Dockerfile.playwright
FROM mcr.microsoft.com/playwright:v1.44.0-jammy

WORKDIR /app

# Copy project files
COPY package*.json ./
RUN npm ci

COPY . .

# Default command runs all tests
CMD ["npx", "playwright", "test", "--reporter=list"]
```

```yaml
# docker-compose.playwright.yml
version: '3.8'
services:
  playwright:
    build:
      context: .
      dockerfile: Dockerfile.playwright
    environment:
      - BASE_URL=http://app:3000
      - CI=true
      - ADMIN_EMAIL=${ADMIN_EMAIL}
      - ADMIN_PASSWORD=${ADMIN_PASSWORD}
    volumes:
      - ./test-results:/app/test-results
      - ./playwright-report:/app/playwright-report
    depends_on:
      app:
        condition: service_healthy
    networks:
      - test-network

  app:
    image: my-app:latest
    ports:
      - "3000:3000"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 5s
      timeout: 3s
      retries: 10
    networks:
      - test-network

networks:
  test-network:
```

```bash
# Run tests in Docker
docker-compose -f docker-compose.playwright.yml up --exit-code-from playwright

# Run specific test file
docker run --rm \
  -v $(pwd)/test-results:/app/test-results \
  -e BASE_URL=http://host.docker.internal:3000 \
  my-playwright-image \
  npx playwright test tests/checkout.spec.ts
```

```typescript
// playwright.config.ts — Docker-aware config
export default defineConfig({
  webServer: process.env.CI ? undefined : {
    // Only start dev server locally; Docker uses service in compose
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
  },
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
  },
});
```

**Real-world Usage:**
- FinTech teams run Playwright in Docker to ensure font rendering matches baselines for visual regression.
- Enterprise: Docker Compose spins up app + database + Playwright as a self-contained test environment.
- Kubernetes-based CI systems (ArgoCD, Tekton) run Playwright pods with ephemeral storage for test artifacts.

**Common Mistakes:**
- Using `--ipc=host` — should be `--shm-size=2gb` instead; browser shared memory defaults (64MB) cause crashes on complex pages.
- Not pinning the Playwright Docker image version — `latest` tag changes break reproducibility.
- Running as root in Docker — some browser sandbox features are disabled; use `--no-sandbox` only as last resort.

**Optimization Tip:**
- Pre-pull the Playwright Docker image in CI before the test step to reduce pipeline time:
  ```yaml
  - run: docker pull mcr.microsoft.com/playwright:v1.44.0-jammy
  ```

**Debugging Strategy:**
- Add `--headed` with VNC access for debugging in Docker:
  ```dockerfile
  RUN apt-get install -y x11vnc xvfb
  ```
- Mount traces directory as a volume and inspect locally after a failed Docker run.

**Tricky Follow-up Questions:**
1. *What is the `--shm-size` flag in Docker and why does Playwright need it increased from the default?*
2. *How would you run Playwright tests in a Kubernetes Job with artifact collection to S3 on failure?*

---

## Q32: How do you run Playwright tests on cloud browser platforms (BrowserStack / Sauce Labs)?

**Answer:**
- Connect to cloud platforms using the **CDP endpoint** they expose via `chromium.connectOverCDP(wsEndpoint)` or via `connect` in `playwright.config.ts`.
- BrowserStack and Sauce Labs provide a WebSocket URL for each cloud browser session.
- Alternatively, use platform-specific Playwright integrations: `@browserstack/playwright` or native CDP connect.

**Explanation:**
- Cloud platforms provide real browsers on real OS/device combinations — critical for Safari on macOS and real iOS/Android.
- Cloud sessions are billed per minute — efficient test design and parallel limits matter.
- Session names, tags, and status (pass/fail) are pushed to the cloud dashboard via capabilities or API calls.
- `connectOverCDP` reuses the standard Playwright API — no new syntax required.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts — BrowserStack integration
import { defineConfig } from '@playwright/test';
import { BrowserStackCapabilities } from './utils/browserstack';

const isCloudRun = process.env.CLOUD === 'browserstack';

export default defineConfig({
  workers: isCloudRun ? 5 : 2,

  use: {
    connectOptions: isCloudRun ? {
      wsEndpoint: `wss://cdp.browserstack.com/playwright?caps=${
        encodeURIComponent(JSON.stringify({
          browser: 'chrome',
          browser_version: 'latest',
          os: 'Windows',
          os_version: '11',
          'browserstack.username': process.env.BS_USERNAME,
          'browserstack.accessKey': process.env.BS_ACCESS_KEY,
          'browserstack.networkLogs': true,
          'browserstack.consoleLogs': 'info',
        }))
      }`,
    } : undefined,
    baseURL: process.env.BASE_URL,
  },
});
```

```typescript
// Dynamic cloud session per test with status reporting
import { test as base } from '@playwright/test';

export const test = base.extend({
  page: async ({ page }, use, testInfo) => {
    await use(page);

    // Mark session as passed/failed in BrowserStack dashboard
    if (process.env.CLOUD === 'browserstack') {
      const status = testInfo.status === 'passed' ? 'passed' : 'failed';
      const reason = testInfo.error?.message ?? '';
      await page.evaluate(
        ([s, r]) => { (window as any).browserstack_executor = { action: 'setSessionStatus', arguments: { status: s, reason: r } }; },
        [status, reason]
      );
    }
  }
});
```

```typescript
// Direct CDP connect (works for any CDP-compatible cloud)
import { chromium } from '@playwright/test';

async function runOnCloud(): Promise<void> {
  const browser = await chromium.connectOverCDP(
    `wss://hub.browserstack.com/playwright?caps=${encodeURIComponent(JSON.stringify({
      'browserstack.username': process.env.BS_USERNAME,
      'browserstack.accessKey': process.env.BS_ACCESS_KEY,
      browser: 'safari',
      os: 'OS X',
      os_version: 'Sonoma',
    }))}`
  );
  const page = await browser.newPage();
  await page.goto('https://staging.app.com');
  await browser.close();
}
```

**Real-world Usage:**
- FinTech: Real Safari on macOS testing for banking portals — WebKit emulation is not sufficient for compliance.
- SaaS: Cross-browser matrix (12 combinations) runs on BrowserStack nightly; local runs use Chromium only.
- Enterprise: Sauce Labs provides video recordings of every cloud session — attached to CI artifacts.

**Common Mistakes:**
- Running the full regression suite on cloud simultaneously — cloud parallelism has session limits and cost implications.
- Not setting session name/build in capabilities — cloud dashboard becomes unnavigable with unnamed sessions.
- Forgetting to mark session status — failed tests show as "Unknown" in the cloud dashboard.

**Optimization Tip:**
- Use cloud only for cross-browser smoke tests (20-30 critical paths). Run the main regression locally/in Docker on Chromium.
  ```bash
  # Smoke suite on cloud
  npx playwright test --grep @smoke --project=cloud-safari-macos
  ```

**Debugging Strategy:**
- Use `browserstack.networkLogs: true` and `consoleLogs: 'errors'` to capture network and console data in cloud sessions.
- Download session videos from the BrowserStack Automate dashboard when a test fails on cloud but passes locally.

**Tricky Follow-up Questions:**
1. *What are the limitations of `connectOverCDP` compared to a local browser launch, and which Playwright features don't work over CDP?*
2. *How do you handle session timeouts on cloud platforms for tests that take longer than the platform's default idle timeout?*

---

## Q33: Explain the Page Object Model (POM) pattern in Playwright. How do you implement it correctly?

**Answer:**
- **Page Object Model** encapsulates all interactions with a page/component into a class, separating UI interaction logic from test logic.
- Each Page Object represents a page or reusable component — exposing **methods** (actions) and **assertions**, not raw locators.
- Tests become readable business-language sequences: `loginPage.login(email, password)` instead of raw `fill`, `click` chains.

**Explanation:**
- POM reduces duplication — selector changes are fixed in one place, not 50 tests.
- Methods should represent **business actions**, not UI steps: `placeOrder()` not `clickSubmitButton()`.
- Avoid exposing `Locator` properties publicly — this leaks implementation detail and breaks encapsulation.
- Page Objects should be **stateless** — they operate on the `Page` instance passed via constructor.

**JavaScript / TypeScript Example:**
```typescript
// pages/LoginPage.ts
import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
  private readonly emailInput: Locator;
  private readonly passwordInput: Locator;
  private readonly loginButton: Locator;
  private readonly errorMessage: Locator;

  constructor(private page: Page) {
    this.emailInput = page.getByLabel('Email address');
    this.passwordInput = page.getByLabel('Password');
    this.loginButton = page.getByRole('button', { name: 'Sign in' });
    this.errorMessage = page.getByRole('alert');
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
  }

  async login(email: string, password: string): Promise<void> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async expectLoginError(message: string): Promise<void> {
    await expect(this.errorMessage).toBeVisible();
    await expect(this.errorMessage).toContainText(message);
  }

  async expectRedirectedToDashboard(): Promise<void> {
    await expect(this.page).toHaveURL('/dashboard');
  }
}
```

```typescript
// pages/CheckoutPage.ts
import { Page, Locator, expect } from '@playwright/test';

export class CheckoutPage {
  constructor(private page: Page) {}

  private get orderSummary(): Locator {
    return this.page.getByTestId('order-summary');
  }

  private get placeOrderButton(): Locator {
    return this.page.getByRole('button', { name: 'Place Order' });
  }

  async fillShippingAddress(address: ShippingAddress): Promise<void> {
    await this.page.getByLabel('Street').fill(address.street);
    await this.page.getByLabel('City').fill(address.city);
    await this.page.getByLabel('ZIP').fill(address.zip);
    await this.page.getByLabel('Country').selectOption(address.country);
  }

  async placeOrder(): Promise<string> {
    await this.placeOrderButton.click();
    await expect(this.page.getByText('Order confirmed')).toBeVisible();
    return await this.page.getByTestId('order-id').innerText();
  }

  async expectTotal(amount: string): Promise<void> {
    await expect(this.orderSummary.getByTestId('total-amount')).toHaveText(amount);
  }
}

interface ShippingAddress {
  street: string; city: string; zip: string; country: string;
}
```

```typescript
// tests/checkout.spec.ts — clean test using POMs
import { test, expect } from '../fixtures';
import { LoginPage } from '../pages/LoginPage';
import { CheckoutPage } from '../pages/CheckoutPage';

test('complete checkout flow', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const checkoutPage = new CheckoutPage(page);

  await loginPage.goto();
  await loginPage.login('user@example.com', 'Password123!');
  await loginPage.expectRedirectedToDashboard();

  await page.goto('/checkout');
  await checkoutPage.fillShippingAddress({
    street: '123 Main St', city: 'New York', zip: '10001', country: 'US'
  });
  const orderId = await checkoutPage.placeOrder();

  expect(orderId).toMatch(/^ord-/);
});
```

**Real-world Usage:**
- All enterprise Playwright frameworks use POM as the foundational pattern — ensures 100+ developers can work in the same codebase.
- FinTech: Separate page objects per module (Trading, Portfolio, Accounts, Reports) owned by respective feature teams.

**Common Mistakes:**
- Exposing `Locator` properties as public — callers bypass the POM and write raw locator code in tests.
- Creating one giant POM per page with 50+ methods — split into focused component objects.
- Returning `Page` from action methods to chain — creates fragile fluent API that breaks on navigation.
- Putting assertions in Page Objects — makes them harder to reuse and harder to compose.

**Optimization Tip:**
- Use getter properties (not methods) for locators — evaluated lazily and concise:
  ```typescript
  private get submitButton() { return this.page.getByRole('button', { name: 'Submit' }); }
  ```

**Debugging Strategy:**
- Add `await this.page.screenshot()` to page object methods during debugging — remove before committing.

**Tricky Follow-up Questions:**
1. *What are the downsides of POM at scale, and how does the Screenplay pattern address them?*
2. *How do you handle page objects for single-page applications where "pages" are more like views with shared navigation?*

---

## Q34: What is the Component Object Model and when do you use it over POM?

**Answer:**
- **Component Object Model (COM)** models reusable UI components (modal, data table, dropdown, date picker) as independent objects — rather than per-page objects.
- Used when the same component appears on multiple pages (e.g., a shared navigation bar, a confirmation modal, a product card).
- Components are scoped to a **root `Locator`** passed at construction — not to the full `Page`.

**Explanation:**
- POM maps 1:1 to pages — when a component is reused, duplicating it in each page object introduces maintenance debt.
- COM composes: a `CheckoutPage` contains a `PaymentForm` component, an `AddressForm` component, and an `OrderSummary` component.
- Component objects accept a `Locator` (their root) instead of a `Page` — enabling scoped queries within their container.
- This aligns with how modern frontend is structured (React, Angular, Vue components).

**JavaScript / TypeScript Example:**
```typescript
// components/DataTable.ts
import { Locator, expect } from '@playwright/test';

export class DataTable {
  constructor(private root: Locator) {}

  row(rowText: string): Locator {
    return this.root.getByRole('row').filter({ hasText: rowText });
  }

  async getColumnValue(rowText: string, columnHeader: string): Promise<string> {
    const row = this.row(rowText);
    const headerRow = this.root.getByRole('row').first();
    const headers = await headerRow.getByRole('columnheader').allInnerTexts();
    const colIndex = headers.indexOf(columnHeader);
    if (colIndex === -1) throw new Error(`Column "${columnHeader}" not found`);
    return await row.getByRole('cell').nth(colIndex).innerText();
  }

  async clickRowAction(rowText: string, action: string): Promise<void> {
    await this.row(rowText).getByRole('button', { name: action }).click();
  }

  async expectRowCount(count: number): Promise<void> {
    // Exclude header row
    await expect(this.root.getByRole('row')).toHaveCount(count + 1);
  }

  async sortBy(columnHeader: string): Promise<void> {
    await this.root
      .getByRole('columnheader', { name: columnHeader })
      .click();
  }
}
```

```typescript
// components/ConfirmationModal.ts
import { Locator, Page, expect } from '@playwright/test';

export class ConfirmationModal {
  private root: Locator;

  constructor(page: Page) {
    this.root = page.getByRole('dialog');
  }

  async expectMessage(text: string): Promise<void> {
    await expect(this.root.getByRole('paragraph')).toContainText(text);
  }

  async confirm(): Promise<void> {
    await this.root.getByRole('button', { name: 'Confirm' }).click();
    await expect(this.root).not.toBeVisible();
  }

  async cancel(): Promise<void> {
    await this.root.getByRole('button', { name: 'Cancel' }).click();
  }
}
```

```typescript
// pages/UsersPage.ts — composing components
import { Page } from '@playwright/test';
import { DataTable } from '../components/DataTable';
import { ConfirmationModal } from '../components/ConfirmationModal';

export class UsersPage {
  readonly table: DataTable;
  readonly confirmationModal: ConfirmationModal;

  constructor(private page: Page) {
    this.table = new DataTable(page.getByTestId('users-table'));
    this.confirmationModal = new ConfirmationModal(page);
  }

  async deleteUser(userName: string): Promise<void> {
    await this.table.clickRowAction(userName, 'Delete');
    await this.confirmationModal.expectMessage(`Delete ${userName}?`);
    await this.confirmationModal.confirm();
  }
}
```

**Real-world Usage:**
- Enterprise applications with a shared design system (shared data tables, modals, sidebars, dropdowns) benefit most from COM.
- FinTech: A `TradingTicket` component appears on 5 different pages — a single component object maintains it.
- SaaS admin panels: Pagination, filter bars, and bulk action toolbars are reused across Orders, Users, Products pages.

**Common Mistakes:**
- Passing `Page` to component objects instead of a scoped `Locator` — defeats component isolation.
- Creating a component object for something that appears only once — overhead without benefit; use POM method instead.
- Making component objects aware of their parent page — creates tight coupling.

**Optimization Tip:**
- Build a component library catalog (mirroring the frontend component library) — every UI component has a corresponding test component object, maintained in lockstep.

**Tricky Follow-up Questions:**
1. *How do you handle a component whose root element changes dynamically (e.g., a modal that is re-mounted on each open)?*
2. *How do you test a component that has multiple instances on the same page (e.g., three data tables)?*

---

## Q35: How do you manage test data in a large-scale Playwright framework?

**Answer:**
- **Three strategies**: static fixtures (JSON/TS files), dynamic API-created data (per-test), and database seeding (per-suite).
- Use API-created data for test isolation — each test creates its own data via the API, uses it, and deletes it in teardown.
- Never rely on pre-existing database state — tests must be able to run in any order against a clean environment.

**Explanation:**
- Static fixtures are fine for read-only data (product catalog, country list) but cause conflicts in parallel tests when they're mutable.
- API-created data is the gold standard: deterministic, parallel-safe, self-cleaning.
- For heavy setup (100+ records), use a factory pattern with batch creation APIs.
- Environment-specifc data (staging vs prod-like) is managed via config-driven data sets.

**JavaScript / TypeScript Example:**
```typescript
// utils/TestDataFactory.ts
import { APIRequestContext } from '@playwright/test';

interface CreateUserOptions {
  role?: 'admin' | 'user' | 'readonly';
  verified?: boolean;
  plan?: 'free' | 'pro' | 'enterprise';
}

interface TestUser {
  id: string;
  email: string;
  name: string;
  role: string;
  token: string;
}

export class TestDataFactory {
  private createdUserIds: string[] = [];

  constructor(
    private request: APIRequestContext,
    private adminToken: string
  ) {}

  async createUser(options: CreateUserOptions = {}): Promise<TestUser> {
    const timestamp = Date.now();
    const payload = {
      name: `Test User ${timestamp}`,
      email: `test-${timestamp}@playwright.test`,
      role: options.role ?? 'user',
      verified: options.verified ?? true,
      plan: options.plan ?? 'pro',
    };

    const res = await this.request.post('/api/users', {
      data: payload,
      headers: { Authorization: `Bearer ${this.adminToken}` },
    });

    if (!res.ok()) throw new Error(`Failed to create user: ${await res.text()}`);
    const user = await res.json();
    this.createdUserIds.push(user.id);
    return user;
  }

  async createOrder(userId: string, items: OrderItem[]): Promise<TestOrder> {
    const res = await this.request.post('/api/orders', {
      data: { userId, items, currency: 'USD' },
      headers: { Authorization: `Bearer ${this.adminToken}` },
    });
    const order = await res.json();
    return order;
  }

  async cleanup(): Promise<void> {
    // Delete all created users (cascades to their data)
    await Promise.all(
      this.createdUserIds.map(id =>
        this.request.delete(`/api/users/${id}`, {
          headers: { Authorization: `Bearer ${this.adminToken}` },
        })
      )
    );
    this.createdUserIds = [];
  }
}

interface OrderItem { productId: string; quantity: number; }
interface TestOrder { id: string; status: string; total: number; }
```

```typescript
// fixtures/index.ts — factory as fixture
import { test as base } from '@playwright/test';
import { TestDataFactory } from '../utils/TestDataFactory';

export const test = base.extend<{ factory: TestDataFactory }>({
  factory: async ({ request }, use) => {
    const factory = new TestDataFactory(request, process.env.ADMIN_TOKEN!);
    await use(factory);
    await factory.cleanup(); // Auto-cleanup after each test
  },
});
```

```typescript
// test using factory
import { test, expect } from '../fixtures';

test('admin can deactivate user', async ({ page, factory }) => {
  const user = await factory.createUser({ role: 'user', plan: 'pro' });

  await page.goto(`/admin/users/${user.id}`);
  await page.getByRole('button', { name: 'Deactivate' }).click();
  await expect(page.getByTestId('user-status')).toHaveText('Inactive');
});
```

```typescript
// Static fixtures for reference data
// fixtures/data/products.ts
export const testProducts = {
  basic: { id: 'prod-001', name: 'Basic Plan', price: 9.99 },
  pro:   { id: 'prod-002', name: 'Pro Plan',   price: 29.99 },
} as const;
```

**Real-world Usage:**
- FinTech: `TestDataFactory` creates portfolios, positions, and accounts via API — tests run in parallel without data conflicts.
- SaaS: A `TenantFactory` creates isolated tenant workspaces per test — full isolation at the tenant level.
- Enterprise: Database seeding via migrations for expensive setup (1000-row tables) done once at worker scope.

**Common Mistakes:**
- Using hardcoded IDs (e.g., `userId: 'test-user-1'`) in parallel tests — race conditions guaranteed.
- Not cleaning up test data — staging database grows unbounded with orphaned records.
- Creating test data via UI instead of API — 10-20x slower, adds fragility.

**Optimization Tip:**
- Tag test data records: `email: 'test-${timestamp}@playwright.test'` — a cleanup job can delete all `@playwright.test` records nightly.

**Tricky Follow-up Questions:**
1. *How do you handle test data cleanup when a test fails halfway through, leaving partially created data?*
2. *What is a "builder pattern" for test data and how does it improve readability over plain factory functions?*

---

## Q36: How do you manage environment configuration and secrets in Playwright?

**Answer:**
- Use `playwright.config.ts` `use.baseURL` driven by environment variables for URL configuration.
- Secrets (passwords, tokens, API keys) must **never** be hardcoded — use `.env` files locally (via `dotenv`) and CI secret managers in pipelines.
- Use typed configuration objects to centralize and validate env var access.

**Explanation:**
- Environment parity (dev / staging / production-like) is a core CI/CD principle — tests must be portable across environments.
- `dotenv` loads `.env` files in local development; CI systems inject secrets as environment variables.
- A `Config` utility validates required env vars at startup — failing fast with a clear error if a required secret is missing.
- `.env` files must be in `.gitignore` — a single leaked credential invalidates the entire security posture.

**JavaScript / TypeScript Example:**
```typescript
// config/environment.ts — typed, validated config
interface EnvironmentConfig {
  baseURL: string;
  apiURL: string;
  adminEmail: string;
  adminPassword: string;
  adminToken: string;
  environment: 'local' | 'staging' | 'production';
}

function requireEnv(key: string): string {
  const value = process.env[key];
  if (!value) throw new Error(`Required environment variable "${key}" is not set`);
  return value;
}

export const config: EnvironmentConfig = {
  baseURL:       process.env.BASE_URL     ?? 'http://localhost:3000',
  apiURL:        process.env.API_URL      ?? 'http://localhost:3001',
  adminEmail:    requireEnv('ADMIN_EMAIL'),
  adminPassword: requireEnv('ADMIN_PASSWORD'),
  adminToken:    requireEnv('ADMIN_TOKEN'),
  environment:   (process.env.ENV ?? 'local') as EnvironmentConfig['environment'],
};

// Guard against accidental production test runs
if (config.environment === 'production' && !process.env.ALLOW_PRODUCTION_TESTS) {
  throw new Error('Tests blocked: set ALLOW_PRODUCTION_TESTS=1 to explicitly run against production');
}
```

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import * as dotenv from 'dotenv';

// Load .env file for local development
dotenv.config({ path: `.env.${process.env.ENV ?? 'local'}` });

export default defineConfig({
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
  },
  projects: [
    { name: 'local',   testMatch: '**/*.spec.ts' },
    { name: 'staging', testMatch: '**/*.spec.ts' },
  ],
});
```

```bash
# .env.local (NOT committed to git)
BASE_URL=http://localhost:3000
ADMIN_EMAIL=admin@local.dev
ADMIN_PASSWORD=LocalPassword123!
ADMIN_TOKEN=dev-token-abc123

# .env.staging (NOT committed to git)
BASE_URL=https://staging.app.com
ADMIN_EMAIL=admin@staging.app.com
ADMIN_TOKEN=staging-token-xyz
```

```bash
# GitHub Actions — secrets injected by CI
env:
  ADMIN_EMAIL: ${{ secrets.STAGING_ADMIN_EMAIL }}
  ADMIN_PASSWORD: ${{ secrets.STAGING_ADMIN_PASSWORD }}
  ADMIN_TOKEN: ${{ secrets.STAGING_API_TOKEN }}
  BASE_URL: ${{ secrets.STAGING_BASE_URL }}
```

**Real-world Usage:**
- FinTech: `ENV=staging npx playwright test` runs tests against staging; the `requireEnv` guard prevents accidental production runs.
- Enterprise: Vault (HashiCorp) or AWS Secrets Manager injects secrets via CI — no credentials in any config file.
- Teams use `.env.example` (committed) with placeholder values to document required variables.

**Common Mistakes:**
- Committing `.env` files with real credentials — a severe security incident.
- `console.log(process.env.ADMIN_PASSWORD)` in test files — credentials leaked in CI logs. Use `***` masking.
- Not validating required env vars at startup — tests fail with cryptic `undefined is not a string` errors 10 minutes into a run.

**Optimization Tip:**
- Use a `.env.example` file with all required variables documented (with fake values) — developers clone and rename to `.env.local`.

**Tricky Follow-up Questions:**
1. *How do you prevent secrets from appearing in Playwright trace files or HAR recordings?*
2. *What is the difference between build-time and runtime environment variables in a test framework context?*

---

## Q37: How do you build a custom Playwright reporter?

**Answer:**
- Implement the `Reporter` interface from `@playwright/test/reporter` — override lifecycle hooks: `onBegin`, `onTestEnd`, `onEnd`, etc.
- Register in `playwright.config.ts` under `reporter`.
- Custom reporters enable: Slack notifications, Jira ticket creation, custom dashboards, Testrail integration, and metric collection.

**Explanation:**
- Built-in reporters (list, html, json, junit) cover standard needs; custom reporters handle business-specific requirements.
- Reporters receive `TestResult` objects with full test metadata: status, duration, errors, attachments, retry count, and annotations.
- Reporters can be async — useful for API calls to external systems.
- Multiple reporters run simultaneously — combine built-in with custom.

**JavaScript / TypeScript Example:**
```typescript
// reporters/SlackReporter.ts
import type {
  Reporter, FullConfig, Suite, TestCase,
  TestResult, FullResult
} from '@playwright/test/reporter';

interface SlackMessage {
  channel: string;
  text: string;
  blocks?: object[];
}

export default class SlackReporter implements Reporter {
  private passed = 0;
  private failed = 0;
  private skipped = 0;
  private failures: Array<{ title: string; error: string }> = [];
  private startTime = 0;

  onBegin(config: FullConfig, suite: Suite): void {
    this.startTime = Date.now();
    console.log(`[Slack Reporter] Starting ${suite.allTests().length} tests`);
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    switch (result.status) {
      case 'passed':   this.passed++;  break;
      case 'failed':
      case 'timedOut':
        this.failed++;
        this.failures.push({
          title: test.titlePath().join(' > '),
          error: result.error?.message?.slice(0, 200) ?? 'Unknown error',
        });
        break;
      case 'skipped':  this.skipped++; break;
    }
  }

  async onEnd(result: FullResult): Promise<void> {
    if (!process.env.SLACK_WEBHOOK_URL) return;

    const duration = ((Date.now() - this.startTime) / 1000).toFixed(1);
    const emoji = result.status === 'passed' ? '✅' : '❌';
    const total = this.passed + this.failed + this.skipped;

    const message: SlackMessage = {
      channel: '#ci-test-results',
      text: `${emoji} Playwright Tests: ${result.status.toUpperCase()}`,
      blocks: [
        {
          type: 'section',
          text: {
            type: 'mrkdwn',
            text: `*${emoji} Playwright Test Results*\n` +
              `Environment: \`${process.env.ENV ?? 'local'}\`\n` +
              `Duration: ${duration}s | Total: ${total}\n` +
              `✅ Passed: ${this.passed} | ❌ Failed: ${this.failed} | ⏭️ Skipped: ${this.skipped}`,
          },
        },
        ...(this.failures.length > 0 ? [{
          type: 'section',
          text: {
            type: 'mrkdwn',
            text: `*Failed Tests:*\n${this.failures.slice(0, 5).map(f => `• ${f.title}\n  \`${f.error}\``).join('\n')}`,
          },
        }] : []),
      ],
    };

    await fetch(process.env.SLACK_WEBHOOK_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(message),
    });
  }
}
```

```typescript
// playwright.config.ts — registering reporters
export default defineConfig({
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'results/junit.xml' }],
    ['./reporters/SlackReporter.ts'],  // Custom reporter
  ],
});
```

**Real-world Usage:**
- Enterprise: Custom reporters push results to TestRail for test case management, linking runs to requirements.
- FinTech: A `JiraReporter` auto-creates bug tickets for failed tests with trace attachment links.
- SaaS: A `MetricsReporter` publishes test duration, pass rate, and flakiness metrics to Datadog/Grafana.

**Common Mistakes:**
- Throwing unhandled errors in reporter hooks — crashes the entire test run.
- Making synchronous network calls in `onTestEnd` — blocks test execution. Buffer and flush in `onEnd`.
- Not handling the `onEnd` result for early exit cases (e.g., interrupted run) — some hooks may not fire.

**Optimization Tip:**
- Buffer all events during the run and make a single API call in `onEnd` — avoids throttling from Slack/Jira with per-test API calls.

**Tricky Follow-up Questions:**
1. *How do you attach a trace file URL (from a cloud storage bucket) to each failed test in a custom reporter?*
2. *What is the difference between a `Reporter` and a `globalTeardown` for post-run reporting tasks?*

---

## Q38: How do you test WebSocket connections in Playwright?

**Answer:**
- Playwright supports WebSocket interception via `page.routeWebSocket(url, handler)` (Playwright v1.48+).
- For older versions, use `page.on('websocket', ws => ...)` to observe WebSocket frames sent/received.
- Mock WebSocket messages with `ws.send()` in the route handler to control the data stream in tests.

**Explanation:**
- WebSocket testing is critical for real-time applications: trading platforms, live dashboards, chat apps, notifications.
- `routeWebSocket` intercepts the upgrade request and provides a mock WebSocket server in the test.
- Observing real WebSocket traffic (without mocking) uses the `websocket` page event to capture frames.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

// --- OBSERVE real WebSocket traffic ---
test('observe WebSocket price updates', async ({ page }) => {
  const wsMessages: string[] = [];

  page.on('websocket', (ws) => {
    console.log('WebSocket opened:', ws.url());

    ws.on('framesent', frame => {
      wsMessages.push(`SENT: ${frame.payload}`);
    });

    ws.on('framereceived', frame => {
      wsMessages.push(`RECV: ${frame.payload}`);
    });

    ws.on('close', () => console.log('WebSocket closed'));
  });

  await page.goto('/trading/dashboard');
  // Wait for price updates to arrive
  await page.waitForTimeout(2000);

  expect(wsMessages.some(m => m.includes('RECV'))).toBeTruthy();
  console.log('Messages:', wsMessages);
});

// --- MOCK WebSocket with routeWebSocket ---
test('mock real-time price feed via WebSocket', async ({ page }) => {
  await page.routeWebSocket('wss://market-data.app.com/prices', (ws) => {
    // Intercept connection and send mock data
    ws.onopen = () => {
      // Send initial snapshot
      ws.send(JSON.stringify({ type: 'snapshot', data: { AAPL: 182.50, GOOG: 175.30 } }));

      // Simulate real-time updates
      let interval = setInterval(() => {
        ws.send(JSON.stringify({
          type: 'update',
          data: { AAPL: (182 + Math.random() * 2).toFixed(2) }
        }));
      }, 500);

      ws.onclose = () => clearInterval(interval);
    };
  });

  await page.goto('/trading/dashboard');

  // Verify UI renders the mocked prices
  await expect(page.getByTestId('price-AAPL')).toContainText('182');
  await expect(page.getByTestId('ticker-status')).toHaveText('Live');
});

// --- TEST WebSocket reconnection ---
test('UI reconnects after WebSocket disconnect', async ({ page }) => {
  let wsInstance: any;

  await page.routeWebSocket('wss://app.com/live', (ws) => {
    wsInstance = ws;
    ws.onopen = () => ws.send(JSON.stringify({ type: 'connected' }));
  });

  await page.goto('/live-dashboard');
  await expect(page.getByTestId('connection-status')).toHaveText('Connected');

  // Simulate server-side disconnect
  wsInstance.close();

  await expect(page.getByTestId('connection-status')).toHaveText('Reconnecting...');
  await expect(page.getByTestId('connection-status')).toHaveText('Connected', { timeout: 10_000 });
});
```

**Real-world Usage:**
- FinTech trading platforms: Mocking market data WebSocket feeds to test UI rendering of price movements, order book updates.
- SaaS: Testing live collaboration features (shared editing, cursor sync) by injecting WebSocket messages.
- Notification systems: Verifying toast notifications appear when a WebSocket event arrives.

**Common Mistakes:**
- Forgetting that WebSocket frames are strings or binary — parse JSON before asserting.
- Not cleaning up mock WebSocket intervals in `ws.onclose` — memory leaks in parallel tests.
- Asserting on WebSocket messages before the connection is established — race condition.

**Optimization Tip:**
- Create a `WebSocketMockServer` fixture that pre-populates common message sequences — reuse across tests that need the same data stream.

**Tricky Follow-up Questions:**
1. *How would you test a WebSocket-based feature in a browser that falls back to HTTP long-polling when WebSocket is unavailable?*
2. *What is the difference between `framesent` and `framereceived` events, and which corresponds to server-to-client messages?*

---

## Q39: How do you handle multipart/form-data requests and file uploads via the API in Playwright?

**Answer:**
- Use `request.post()` with the `multipart` option in `APIRequestContext` to send multipart/form-data requests.
- For file uploads: read the file as a `Buffer` and pass it in the `multipart` object with `name`, `mimeType`, and `buffer`.
- This is the API-layer equivalent of UI file input testing — essential for testing backend file processing directly.

**Explanation:**
- Multipart requests combine file data with text form fields in a single HTTP request.
- `APIRequestContext.multipart` handles: file buffers, text fields, multiple files, and custom MIME types.
- Testing file upload via API (bypassing UI) is faster and more focused for backend validation.
- Use alongside UI upload tests for full coverage — API tests validate backend processing, UI tests validate the upload UX.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test('upload document via API (multipart)', async ({ request }) => {
  const fileBuffer = fs.readFileSync(path.join(__dirname, 'fixtures/contract.pdf'));

  const response = await request.post('/api/documents/upload', {
    headers: {
      Authorization: `Bearer ${process.env.API_TOKEN}`,
    },
    multipart: {
      file: {
        name: 'contract.pdf',
        mimeType: 'application/pdf',
        buffer: fileBuffer,
      },
      documentType: 'contract',
      userId: 'usr-001',
      metadata: JSON.stringify({ category: 'legal', year: 2024 }),
    },
  });

  expect(response.status()).toBe(201);
  const result = await response.json();
  expect(result).toMatchObject({
    id: expect.stringMatching(/^doc-/),
    fileName: 'contract.pdf',
    status: 'processing',
  });
});

test('upload multiple files in single API request', async ({ request }) => {
  const files = ['image1.png', 'image2.png'].map(name => ({
    name,
    mimeType: 'image/png',
    buffer: fs.readFileSync(path.join(__dirname, `fixtures/${name}`)),
  }));

  // Send each file as a separate multipart field with the same key
  const response = await request.post('/api/gallery/upload', {
    multipart: {
      'files[0]': files[0],
      'files[1]': files[1],
      albumId: 'album-123',
    },
  });

  expect(response.ok()).toBeTruthy();
  const { uploaded } = await response.json();
  expect(uploaded).toHaveLength(2);
});

test('validate file type rejection', async ({ request }) => {
  const maliciousBuffer = Buffer.from('<?php echo shell_exec($_GET["cmd"]); ?>');

  const response = await request.post('/api/documents/upload', {
    multipart: {
      file: {
        name: 'script.php',
        mimeType: 'application/x-httpd-php',
        buffer: maliciousBuffer,
      },
      documentType: 'kyc',
    },
  });

  expect(response.status()).toBe(422);
  const error = await response.json();
  expect(error.code).toBe('INVALID_FILE_TYPE');
});
```

**Real-world Usage:**
- KYC document processing APIs: Testing that uploaded IDs are correctly routed to verification queues.
- FinTech: Trade confirmation PDF uploads, contract signing workflows.
- SaaS: User avatar upload, product image upload, bulk CSV import — all testable via multipart API calls.

**Common Mistakes:**
- Setting `Content-Type: multipart/form-data` manually — `APIRequestContext` sets boundary automatically; overriding it breaks the request.
- Using a file path string instead of a `Buffer` in the multipart field — API context requires the buffer.
- Not validating the rejection case — security tests for file type validation are mandatory.

**Optimization Tip:**
- Create a small test fixture file factory that generates common test files in memory (no disk I/O):
  ```typescript
  function createTestPDF(content = 'Test PDF content'): Buffer {
    // Minimal valid PDF header
    return Buffer.from(`%PDF-1.4\n1 0 obj\n<</Type /Catalog>>\n%%EOF`);
  }
  ```

**Tricky Follow-up Questions:**
1. *How does the server-side multipart boundary work, and what happens if you send an incorrect Content-Type header manually?*
2. *How would you test virus scan integration — where a file upload triggers async scanning and the UI should show a "Scanning..." state?*

---

## Q40: How do you implement test tagging, filtering, and categorization in Playwright?

**Answer:**
- Use `@tag` annotations in test titles with `test.describe`/`test` combined with `--grep` CLI filter.
- Use `test.describe` grouping and `testInfo.annotations` for structured metadata.
- Playwright v1.42+ supports `tag` option in `test()` and `test.describe()` natively.

**Explanation:**
- Tagging enables selective test execution: `@smoke`, `@regression`, `@critical`, `@slow`, `@flaky`, `@wip`.
- CI pipelines use tags to define test tiers: smoke on every PR, regression on merge, performance weekly.
- Tags in test titles are the simplest approach; native `tag` option from v1.42 provides structured metadata.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

// Method 1: Tags in test title (Playwright v1.42+ native tag option preferred)
test('login with valid credentials @smoke @critical', async ({ page }) => {
  // ...
});

// Method 2: Native tag option (v1.42+)
test('checkout flow', {
  tag: ['@smoke', '@regression', '@payment'],
}, async ({ page }) => {
  // ...
});

// Tagging at describe level
test.describe('User Management', {
  tag: ['@admin', '@regression'],
}, () => {
  test('create user', async ({ page }) => { /* inherits @admin @regression */ });
  test('delete user', async ({ page }) => { /* inherits @admin @regression */ });
  test('bulk import @slow', async ({ page }) => { /* @admin @regression @slow */ });
});

// Test annotations for structured metadata
test('payment processing', async ({ page }, testInfo) => {
  testInfo.annotations.push(
    { type: 'jira', description: 'PAY-1234' },
    { type: 'owner', description: 'payments-team' },
    { type: 'priority', description: 'P0' },
  );
  // ...
});

// Conditional skip based on environment
test('production-only smoke', {
  tag: ['@smoke'],
}, async ({ page }) => {
  test.skip(process.env.ENV !== 'production', 'Only runs in production');
  // ...
});
```

```bash
# CLI filtering examples
npx playwright test --grep "@smoke"                  # Run only smoke tests
npx playwright test --grep "@smoke|@critical"        # Smoke OR critical
npx playwright test --grep-invert "@slow|@wip"       # Exclude slow and WIP
npx playwright test --grep "@regression" --project=chromium  # Regression on Chromium

# Combined with sharding
npx playwright test --grep "@regression" --shard=1/4
```

```typescript
// playwright.config.ts — define tag-based projects
export default defineConfig({
  projects: [
    {
      name: 'smoke',
      grep: /@smoke/,
      use: { retries: 0 },
    },
    {
      name: 'regression',
      grep: /@regression/,
      grepInvert: /@slow/,
      retries: 2,
    },
    {
      name: 'full',
      grepInvert: /@wip/,
    },
  ],
});
```

**Real-world Usage:**
- Enterprise: PR pipelines run `@smoke` (50 tests, 3 min); nightly runs `@regression` (500 tests, 25 min).
- FinTech: `@p0` tags run on every deployment; `@p1` run nightly; `@p2` weekly.
- QA: `@flaky` tag quarantines known flaky tests while fixes are in progress — excluded from main suite, monitored separately.

**Common Mistakes:**
- Using `test.skip()` inside the test body for tag-based skipping — use `grep`/`grepInvert` in config instead.
- Overusing tags — a test tagged `@smoke @regression @p0 @critical @checkout @payment` is unmaintainable.
- Not standardizing the tag vocabulary — each team uses different conventions (`@P0` vs `@p0` vs `@priority-0`).

**Optimization Tip:**
- Define a `tags` constant enum in the framework to enforce vocabulary:
  ```typescript
  export const Tags = {
    SMOKE: '@smoke',
    REGRESSION: '@regression',
    SLOW: '@slow',
    WIP: '@wip',
    P0: '@p0',
  } as const;
  ```

**Tricky Follow-up Questions:**
1. *How do you enforce that every test must have at least one priority tag (`@p0`, `@p1`, `@p2`) using a custom reporter or linting rule?*
2. *How does Playwright's native `tag` option (v1.42+) differ in behavior from `@tag` in test titles?*

---

## Q41: What are Playwright's built-in debugging tools and how do you use them effectively?

**Answer:**
- **Playwright Inspector** (`PWDEBUG=1`): Step-through GUI debugger with action log, element picker, and selector explorer.
- **`page.pause()`**: Pauses test execution at a specific line to inspect browser state.
- **VS Code extension**: Breakpoint-based debugging directly in the editor.
- **`--debug` flag**: Combines headed mode + Playwright Inspector for a single failing test.

**Explanation:**
- `PWDEBUG=1` opens a separate Inspector window alongside the browser — you can step through, re-run, and pick selectors interactively.
- `page.pause()` is a targeted pause — more useful than `PWDEBUG=1` when you only care about a specific step.
- The VS Code Playwright extension enables standard breakpoints with full test context.
- `slowMo` makes automation visible in headed mode — useful for recording demos and walkthroughs.

**JavaScript / TypeScript Example:**
```bash
# Debug all tests in Inspector
PWDEBUG=1 npx playwright test

# Debug a specific test
npx playwright test tests/checkout.spec.ts --debug

# Debug on specific browser
npx playwright test --debug --project=webkit

# Headed + slow motion
npx playwright test --headed --project=chromium
```

```typescript
import { test, expect } from '@playwright/test';

// Targeted pause in test
test('debug login flow', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('test@example.com');

  await page.pause(); // ← Execution pauses here; Inspector opens for step-through

  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Login' }).click();
});

// Conditional pause in CI-safe way (only pauses locally)
test('checkout with conditional debug', async ({ page }) => {
  await page.goto('/checkout');

  if (process.env.DEBUG_PAUSE) {
    await page.pause();
  }

  await page.getByRole('button', { name: 'Place Order' }).click();
});

// Using Playwright codegen to generate locators
// npx playwright codegen https://example.com
// npx playwright codegen --save-storage auth.json https://example.com (with auth)
```

```typescript
// playwright.config.ts — VS Code launch settings for debugging
// .vscode/launch.json
{
  "configurations": [
    {
      "name": "Debug Playwright Tests",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/node_modules/.bin/playwright",
      "args": ["test", "--debug", "${relativeFile}"],
      "env": { "PWDEBUG": "1" },
      "console": "integratedTerminal"
    }
  ]
}
```

**Real-world Usage:**
- Developers use `npx playwright codegen` to generate locators for new page objects without manually inspecting the DOM.
- QA uses `page.pause()` during test development to validate selectors interactively before committing.
- `PWDEBUG=console` (outputs to stdout, no GUI) is used in environments where opening a browser window isn't possible (remote SSH).

**Common Mistakes:**
- Leaving `page.pause()` in committed test code — blocks CI indefinitely (CI has no user to click "Resume").
- Using `await page.waitForTimeout(5000)` as a "debugging pause" — remove before commit.
- Not using `PWDEBUG=console` in headless-only environments — `PWDEBUG=1` requires a display server.

**Optimization Tip:**
- Map `PAUSE` env variable to `page.pause()` calls during development, guarded by `process.env.PAUSE !== 'true'`. This prevents accidental commits from hanging CI.

**Tricky Follow-up Questions:**
1. *What is the difference between `npx playwright test --debug` and `PWDEBUG=1 npx playwright test`?*
2. *How do you use Playwright's source maps to debug TypeScript test code directly (without compiled JS)?*

---

## Q42: How does Playwright's `webServer` configuration work, and when should you use it?

**Answer:**
- `webServer` in `playwright.config.ts` starts a local development server before tests run and stops it after.
- It waits until the server is ready (via `url` health check) before launching tests.
- Supports `reuseExistingServer: true` to skip restarting if the server is already running (critical for local developer experience).

**Explanation:**
- `webServer` eliminates the need to manually start dev servers before running tests — tests become a single command.
- Multiple `webServer` entries start multiple services (API server + frontend) in parallel.
- In CI, the app is typically already deployed (staging) — use `reuseExistingServer: !process.env.CI` to conditionally start the server.
- `stdout` and `stderr` from the server process are available for debugging startup failures.

**JavaScript / TypeScript Example:**
```typescript
// playwright.config.ts — single server
export default defineConfig({
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000, // Wait up to 2 min for server to start
    stdout: 'pipe',
    stderr: 'pipe',
    env: {
      NODE_ENV: 'test',
      DATABASE_URL: process.env.TEST_DATABASE_URL!,
    },
  },
  use: {
    baseURL: 'http://localhost:3000',
  },
});
```

```typescript
// Multiple servers — API + Frontend
export default defineConfig({
  webServer: [
    {
      command: 'npm run start:api',
      url: 'http://localhost:3001/health',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
    {
      command: 'npm run start:frontend',
      url: 'http://localhost:3000',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
  use: {
    baseURL: 'http://localhost:3000',
  },
});
```

```typescript
// Conditional: CI uses deployed staging, local starts dev server
export default defineConfig({
  webServer: process.env.CI ? undefined : {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
  },
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
  },
});
```

**Real-world Usage:**
- Monorepo projects use `webServer` to start the relevant service without requiring developers to remember startup commands.
- Component-level tests against a Storybook server: `command: 'npm run storybook', url: 'http://localhost:6006'`.
- Microservices: Each e2e test project starts only the services it needs.

**Common Mistakes:**
- Not setting `reuseExistingServer: true` locally — kills and restarts server for every `npx playwright test` run, wasting 30-60 seconds.
- Setting `timeout` too low for slow-starting Java/Spring boot backends — tests start before the server is ready.
- Not passing test-specific env vars (`NODE_ENV: 'test'`) — the server may use production database.

**Tricky Follow-up Questions:**
1. *How does Playwright determine the server is "ready"? What happens if the URL returns a non-2xx status?*
2. *How would you configure `webServer` for a server that doesn't expose an HTTP health endpoint?*

---

## Q43: How do you implement the Screenplay pattern in Playwright and when is it better than POM?

**Answer:**
- The **Screenplay pattern** models tests as: **Actors** who have **Abilities** (interact with devices) and perform **Tasks** (business workflows) using **Questions** (assertions) and **Interactions** (atomic UI actions).
- It is better than POM when: multiple actors interact in one test, tests need to be role-aware, or the codebase scales beyond what POM can maintain cleanly.
- POM is simpler at small scale; Screenplay scales better in complex enterprise contexts.

**Explanation:**
- POM's weakness: page objects grow large and mix high-level flows with low-level interactions.
- Screenplay decomposes: `Task` = "Place Order" (high-level) → composed of `Interactions`: click button, fill form.
- **Actors** carry context (who is performing), enabling multi-actor tests naturally.
- Screenplay is the pattern behind Serenity/BDD and aligns with SOLID principles.

**JavaScript / TypeScript Example:**
```typescript
// screenplay/abilities/BrowseTheWeb.ts
import { Page } from '@playwright/test';

export class BrowseTheWeb {
  constructor(public readonly page: Page) {}

  static using(page: Page): BrowseTheWeb {
    return new BrowseTheWeb(page);
  }
}
```

```typescript
// screenplay/Actor.ts
export class Actor {
  private abilities = new Map<string, unknown>();

  constructor(public readonly name: string) {}

  static named(name: string): Actor {
    return new Actor(name);
  }

  whoCan(...abilities: object[]): this {
    abilities.forEach(a => this.abilities.set(a.constructor.name, a));
    return this;
  }

  abilityTo<T>(abilityClass: new (...args: unknown[]) => T): T {
    const ability = this.abilities.get(abilityClass.name) as T;
    if (!ability) throw new Error(`${this.name} does not have ability: ${abilityClass.name}`);
    return ability;
  }

  async attemptsTo(...tasks: Array<{ performAs(actor: Actor): Promise<void> }>): Promise<void> {
    for (const task of tasks) {
      await task.performAs(this);
    }
  }
}
```

```typescript
// screenplay/tasks/Login.ts
import { Actor } from '../Actor';
import { BrowseTheWeb } from '../abilities/BrowseTheWeb';

export class Login {
  constructor(private email: string, private password: string) {}

  static as(email: string, password: string): Login {
    return new Login(email, password);
  }

  async performAs(actor: Actor): Promise<void> {
    const { page } = actor.abilityTo(BrowseTheWeb);
    await page.goto('/login');
    await page.getByLabel('Email').fill(this.email);
    await page.getByLabel('Password').fill(this.password);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.waitForURL('/dashboard');
  }
}
```

```typescript
// screenplay test — multi-actor scenario
import { test, expect } from '@playwright/test';
import { Actor } from '../screenplay/Actor';
import { BrowseTheWeb } from '../screenplay/abilities/BrowseTheWeb';
import { Login } from '../screenplay/tasks/Login';

test('admin approves user registration', async ({ browser }) => {
  const adminContext = await browser.newContext({ storageState: 'auth/admin.json' });
  const userContext = await browser.newContext();

  const admin = Actor.named('Admin').whoCan(BrowseTheWeb.using(await adminContext.newPage()));
  const newUser = Actor.named('NewUser').whoCan(BrowseTheWeb.using(await userContext.newPage()));

  await newUser.attemptsTo(Login.as('newuser@test.com', 'Pass123!'));
  await admin.attemptsTo(/* ApproveRegistration.for('newuser@test.com') */);

  await adminContext.close();
  await userContext.close();
});
```

**Real-world Usage:**
- Enterprise compliance testing where an auditor, submitter, and approver all interact in the same test scenario.
- FinTech: A broker places a trade; a compliance officer reviews it; an admin confirms settlement — three actors, one test.

**Common Mistakes:**
- Implementing Screenplay for a simple CRUD app — POM is sufficient and far less overhead.
- Mixing POM and Screenplay in the same framework — creates confusion.
- Making Tasks too granular — `ClickButton` is an Interaction, not a Task. Tasks are business-level: `SubmitKYCDocuments`.

**Tricky Follow-up Questions:**
1. *What are "Questions" in the Screenplay pattern and how do they differ from assertions in POM?*
2. *When would you choose Screenplay over POM in a new greenfield project?*

---

## Q44: How do you handle dynamic content, infinite scroll, and pagination in Playwright?

**Answer:**
- **Dynamic content**: Use `page.waitForResponse()` or `expect(locator).toBeVisible()` with custom timeout — never `waitForTimeout`.
- **Infinite scroll**: Trigger scroll programmatically and wait for new content to load using `waitForResponse` or element count change.
- **Pagination**: Navigate pages sequentially asserting content per page, or validate all pages via API and spot-check UI.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

// Dynamic content loading
test('wait for dynamically loaded content', async ({ page }) => {
  await page.goto('/feed');

  // Wait for the API response that populates the list
  await page.waitForResponse(res =>
    res.url().includes('/api/feed') && res.status() === 200
  );

  await expect(page.getByTestId('feed-item')).toHaveCount(10);
});

// Infinite scroll — scroll until all items load
test('infinite scroll loads all products', async ({ page }) => {
  await page.goto('/products');
  let previousCount = 0;

  while (true) {
    const items = page.getByTestId('product-card');
    const currentCount = await items.count();

    if (currentCount === previousCount) break; // No new items loaded
    previousCount = currentCount;

    // Scroll to the last item to trigger next batch
    await items.last().scrollIntoViewIfNeeded();

    // Wait for new items or "end of results" indicator
    await Promise.race([
      page.waitForResponse(res => res.url().includes('/api/products')),
      page.getByTestId('end-of-results').waitFor({ timeout: 3000 }).catch(() => {}),
    ]);

    if (await page.getByTestId('end-of-results').isVisible()) break;
  }

  console.log(`Loaded ${previousCount} products total`);
  expect(previousCount).toBeGreaterThan(0);
});

// Pagination — traverse all pages
test('validate all pages of order history', async ({ page }) => {
  await page.goto('/orders');

  let pageNumber = 1;
  const allOrderIds: string[] = [];

  while (true) {
    // Collect order IDs from current page
    const ids = await page.getByTestId('order-id').allInnerTexts();
    allOrderIds.push(...ids);

    const nextButton = page.getByRole('button', { name: 'Next page' });
    if (!(await nextButton.isEnabled())) break;

    await nextButton.click();
    await page.waitForResponse(res => res.url().includes('/api/orders'));
    pageNumber++;
  }

  console.log(`Traversed ${pageNumber} pages, found ${allOrderIds.length} orders`);
  expect(allOrderIds).toHaveLength(new Set(allOrderIds).size); // No duplicates
});
```

**Real-world Usage:**
- FinTech: Transaction history with pagination — validate sorting, filtering, and all-page totals match the API aggregate.
- E-commerce: Infinite scroll product listings — assert specific products appear before/after filter changes.
- SaaS: Activity feed with live updates — assert new items appear at top when a background action occurs.

**Common Mistakes:**
- Using `waitForTimeout` to wait for dynamic content — brittle and slow. Use network response or element presence.
- Not handling the "no more results" state in infinite scroll — test loops forever.
- Paginating in tests when the API returns all data — use API assertions for data completeness, UI tests for display logic only.

**Tricky Follow-up Questions:**
1. *How do you test a "load more" button that sometimes shows a spinner for 3-5 seconds before new content appears?*
2. *How would you verify that an infinite scroll list de-duplicates items when the same record appears across two API pages?*

---

## Q45: How do you measure and assert on performance metrics in Playwright?

**Answer:**
- Use `page.evaluate()` to access the **Performance API** (`performance.getEntriesByType`, `performance.timing`, `PerformanceObserver`).
- Use CDP's `Performance` domain via `context.newCDPSession()` for Chromium-level metrics (JS heap, layout count, DOM size).
- `page.waitForLoadState('networkidle')` combined with timing captures load performance baselines.

**Explanation:**
- Playwright is not a dedicated performance testing tool (use k6, Lighthouse, or WebPageTest for that) but can assert basic performance budgets as part of E2E tests.
- Performance assertions act as **budgets** — a test fails if Time to Interactive exceeds 5 seconds.
- CDP metrics include: `Timestamp`, `Documents`, `Frames`, `JSEventListeners`, `Nodes`, `LayoutCount`, `RecalcStyleCount`, `LayoutDuration`, `RecalcStyleDuration`, `ScriptDuration`, `TaskDuration`, `JSHeapUsedSize`.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

// Web Vitals measurement
test('homepage meets performance budget', async ({ page }) => {
  await page.goto('/');

  // Measure Core Web Vitals via Performance API
  const metrics = await page.evaluate(() => {
    return new Promise<Record<string, number>>((resolve) => {
      const results: Record<string, number> = {};

      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.entryType === 'largest-contentful-paint') {
            results['lcp'] = entry.startTime;
          }
          if (entry.entryType === 'first-input') {
            results['fid'] = (entry as PerformanceEventTiming).processingStart - entry.startTime;
          }
        }
      });

      observer.observe({ entryTypes: ['largest-contentful-paint', 'first-input'] });

      // Also capture navigation timing
      const [nav] = performance.getEntriesByType('navigation') as PerformanceNavigationTiming[];
      results['ttfb'] = nav.responseStart - nav.requestStart;
      results['domInteractive'] = nav.domInteractive;
      results['loadComplete'] = nav.loadEventEnd;

      setTimeout(() => {
        observer.disconnect();
        resolve(results);
      }, 3000);
    });
  });

  console.log('Performance Metrics:', metrics);
  expect(metrics['ttfb']).toBeLessThan(500);         // TTFB < 500ms
  expect(metrics['domInteractive']).toBeLessThan(2000); // DOM Interactive < 2s
});

// CDP-based metrics (Chromium only)
test('dashboard CDP performance metrics', async ({ page, context }) => {
  const client = await context.newCDPSession(page);
  await client.send('Performance.enable');

  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');

  const { metrics } = await client.send('Performance.getMetrics');
  const metricMap = Object.fromEntries(metrics.map(m => [m.name, m.value]));

  console.log('CDP Metrics:', metricMap);

  // Assert performance budgets
  expect(metricMap['JSHeapUsedSize']).toBeLessThan(50 * 1024 * 1024); // < 50MB JS heap
  expect(metricMap['LayoutCount']).toBeLessThan(50);  // < 50 layout recalculations
  expect(metricMap['TaskDuration']).toBeLessThan(2);  // < 2s total task duration
});

// API response time assertion
test('critical API responds within SLA', async ({ page }) => {
  const responseTimes: number[] = [];

  page.on('response', res => {
    if (res.url().includes('/api/dashboard')) {
      const timing = res.request().timing();
      responseTimes.push(timing.responseEnd - timing.requestStart);
    }
  });

  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');

  expect(responseTimes.length).toBeGreaterThan(0);
  const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;
  expect(avgResponseTime).toBeLessThan(1000); // APIs respond within 1s on average
});
```

**Real-world Usage:**
- FinTech: Asserting that portfolio page loads within 3 seconds with 1000+ positions — a contractual SLA requirement.
- SaaS: CI performance budget gates prevent merging if LCP regresses by >20%.
- Enterprise: Dashboard load time is tracked per-build in Grafana using custom reporter metric export.

**Common Mistakes:**
- Using Playwright as a load/stress testing tool — it's single-user; use k6 or Gatling for concurrency.
- Asserting hard millisecond values without accounting for CI machine variability — use generous budgets (2x local baseline).
- Not warming up the server before measuring — first request is always slow due to JIT/cold cache.

**Tricky Follow-up Questions:**
1. *What is the difference between TTFB, FCP, LCP, and TTI, and which one best represents perceived user experience for a SPA?*
2. *How would you integrate Playwright performance assertions with Lighthouse CI for a comprehensive performance gate?*

---

## Q46: How do you test service workers and PWA features in Playwright?

**Answer:**
- Playwright supports Service Worker interception via `context.route()` which applies to all requests, including those from service workers.
- `page.waitForEvent('worker')` resolves when a new service worker is registered on the page.
- Use `context.serviceWorkers()` to access existing service workers and `worker.evaluate()` to execute code within the service worker context.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('PWA works offline with service worker cache', async ({ page, context }) => {
  // Navigate online first to prime the service worker cache
  await page.goto('/');
  await page.waitForLoadState('networkidle');

  // Wait for service worker to activate
  await page.waitForFunction(() => navigator.serviceWorker.controller !== null);

  // Simulate offline
  await context.setOffline(true);

  // Reload — should serve from cache
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await expect(page.getByTestId('offline-indicator')).toBeVisible();

  // Restore online
  await context.setOffline(false);
});

test('service worker intercepts and caches API responses', async ({ page, context }) => {
  // Listen for service worker registration
  const [worker] = await Promise.all([
    page.waitForEvent('worker'),
    page.goto('/'),
  ]);

  expect(worker.url()).toContain('service-worker.js');

  // Verify SW is active
  const swState = await page.evaluate(() =>
    navigator.serviceWorker.controller?.state
  );
  expect(swState).toBe('activated');

  // Check SW cache via evaluate in SW context
  const cachedUrls = await worker.evaluate(async () => {
    const cache = await caches.open('api-cache-v1');
    const keys = await cache.keys();
    return keys.map(req => req.url);
  });

  expect(cachedUrls.some(url => url.includes('/api/dashboard'))).toBeTruthy();
});

test('push notification permission flow', async ({ page, context }) => {
  // Grant notification permission
  await context.grantPermissions(['notifications']);

  await page.goto('/settings/notifications');
  await page.getByRole('button', { name: 'Enable Notifications' }).click();

  // Verify browser permission was requested
  const permissionState = await page.evaluate(() =>
    navigator.permissions.query({ name: 'notifications' as PermissionName })
      .then(p => p.state)
  );

  expect(permissionState).toBe('granted');
  await expect(page.getByTestId('notifications-enabled')).toBeVisible();
});
```

**Real-world Usage:**
- PWA e-commerce apps: Testing offline product browsing and cart persistence using service worker cache.
- FinTech: PWA mobile apps where market data is cached by SW and shown during brief network interruptions.
- SaaS: Verifying background sync — form submissions queued offline are sent when connectivity resumes.

**Common Mistakes:**
- Not waiting for the service worker to activate (`activated` state) before testing offline scenarios — premature offline testing fails because SW isn't controlling the page yet.
- Using `context.setOffline(true)` without verifying the SW has cached required resources first.
- Forgetting to grant `notifications` permission via `context.grantPermissions` — permission prompts block test execution.

**Tricky Follow-up Questions:**
1. *How do you test background sync in a service worker — queuing a form submission offline and verifying it's sent when back online?*
2. *How would you clear the service worker cache between tests to ensure test isolation?*

---

## Q47: How do you handle geolocation and browser permissions in Playwright?

**Answer:**
- Use `context.grantPermissions(['geolocation', 'notifications', 'clipboard-read'])` to pre-grant permissions — avoiding browser permission dialogs.
- Use `context.setGeolocation({ latitude, longitude, accuracy })` to mock location.
- Permissions and geolocation are context-level — all pages in the context inherit them.

**JavaScript / TypeScript Example:**
```typescript
import { test, expect } from '@playwright/test';

test('location-based restaurant finder', async ({ browser }) => {
  const context = await browser.newContext({
    geolocation: { latitude: 40.7128, longitude: -74.0060 }, // New York
    permissions: ['geolocation'],
  });
  const page = await context.newPage();

  await page.goto('/restaurants/nearby');
  await expect(page.getByText('New York')).toBeVisible();
  await expect(page.getByTestId('restaurant-list')).toBeVisible();

  // Change location mid-test
  await context.setGeolocation({ latitude: 51.5074, longitude: -0.1278 }); // London
  await page.getByRole('button', { name: 'Refresh Location' }).click();
  await expect(page.getByText('London')).toBeVisible();

  await context.close();
});

test('clipboard copy functionality', async ({ page, context }) => {
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  await page.goto('/dashboard');

  // Click "Copy API Key" button
  await page.getByRole('button', { name: 'Copy API Key' }).click();

  // Read clipboard content
  const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
  expect(clipboardText).toMatch(/^pk_[a-zA-Z0-9]{32}$/);
});

test('camera/microphone permission for video call feature', async ({ browser }) => {
  const context = await browser.newContext({
    permissions: ['camera', 'microphone'],
  });
  const page = await context.newPage();

  await page.goto('/video-call/join');
  await expect(page.getByTestId('video-preview')).toBeVisible();
  await expect(page.getByTestId('camera-blocked-warning')).not.toBeVisible();

  await context.close();
});
```

**Real-world Usage:**
- Ride-sharing and delivery apps: Location-based dispatch, route display, driver tracking.
- FinTech: Branch/ATM locators using geolocation.
- Collaboration tools: Copy-to-clipboard for invite links, API keys, one-time codes.
- Video conferencing: Pre-granting media permissions for remote interview platform testing.

**Common Mistakes:**
- Not pre-granting permissions in context — browser shows native permission dialog, which Playwright auto-dismisses (denies), causing test failure.
- Setting geolocation after `page.goto()` when the app reads location on load — set it in `newContext()` options or before navigation.
- Using geolocation permissions without `isMobile: true` for apps that only request location on mobile.

**Tricky Follow-up Questions:**
1. *How do you test the "location denied" flow — where the user refuses location permission?*
2. *How do you test an app that degrades gracefully when `navigator.geolocation` is unavailable?*

---

## Q48: How do you implement a robust Page Object Factory pattern in Playwright?

**Answer:**
- A **Page Object Factory** centralizes instantiation of all page objects — tests request pages by name rather than importing and `new`-ing them directly.
- It ensures a single `Page` instance is shared, manages page initialization (navigation, waitForLoad), and supports lazy instantiation.
- Implemented as a fixture that provides the factory to all tests.

**JavaScript / TypeScript Example:**
```typescript
// utils/PageFactory.ts
import { Page } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { CheckoutPage } from '../pages/CheckoutPage';
import { AdminPage } from '../pages/AdminPage';

type PageMap = {
  login: LoginPage;
  dashboard: DashboardPage;
  checkout: CheckoutPage;
  admin: AdminPage;
};

export class PageFactory {
  private cache = new Map<string, unknown>();

  constructor(private page: Page) {}

  get<K extends keyof PageMap>(name: K): PageMap[K] {
    if (!this.cache.has(name)) {
      this.cache.set(name, this.create(name));
    }
    return this.cache.get(name) as PageMap[K];
  }

  private create<K extends keyof PageMap>(name: K): PageMap[K] {
    const constructors: Record<keyof PageMap, new (page: Page) => PageMap[keyof PageMap]> = {
      login:     LoginPage,
      dashboard: DashboardPage,
      checkout:  CheckoutPage,
      admin:     AdminPage,
    };
    return new constructors[name](this.page) as PageMap[K];
  }
}
```

```typescript
// fixtures/index.ts
import { test as base } from '@playwright/test';
import { PageFactory } from '../utils/PageFactory';

export const test = base.extend<{ pages: PageFactory }>({
  pages: async ({ page }, use) => {
    await use(new PageFactory(page));
  },
});
```

```typescript
// Clean test using factory
import { test, expect } from '../fixtures';

test('checkout flow using page factory', async ({ pages }) => {
  const login = pages.get('login');
  const checkout = pages.get('checkout');

  await login.goto();
  await login.login('user@example.com', 'Pass123!');

  await checkout.fillShippingAddress({ street: '123 Main', city: 'NYC', zip: '10001', country: 'US' });
  const orderId = await checkout.placeOrder();
  expect(orderId).toMatch(/^ord-/);
});
```

**Real-world Usage:**
- Large teams prevent inconsistent page object instantiation patterns — the factory is the only way to get a page object.
- TypeScript generics on the factory provide full autocomplete and type safety.
- Lazy caching prevents multiple instances of the same page object from being created in a single test.

**Tricky Follow-up Questions:**
1. *How does the factory pattern interact with page object methods that navigate to a new page — do you need a new factory instance?*
2. *What are the trade-offs between a centralized factory and decentralized direct instantiation in test files?*

---

## Q49: What are the most common anti-patterns in Playwright frameworks and how do you avoid them?

**Answer:**
The most critical Playwright anti-patterns with their solutions:

**1. Hardcoded Waits**
```typescript
// ❌ Anti-pattern
await page.waitForTimeout(3000);

// ✅ Solution
await expect(page.getByTestId('result')).toBeVisible();
await page.waitForResponse('**/api/data');
```

**2. Fragile Selectors**
```typescript
// ❌ Anti-pattern
await page.locator('div.sc-bXqXMF.gHXJVX > button:nth-child(2)').click();

// ✅ Solution
await page.getByRole('button', { name: 'Submit Order' }).click();
await page.getByTestId('submit-order-btn').click();
```

**3. Shared Mutable State Between Tests**
```typescript
// ❌ Anti-pattern — module-level browser shared across tests
let page: Page;
beforeAll(async () => { page = await browser.newPage(); });

// ✅ Solution — fixture-per-test isolation
test('...', async ({ page }) => { /* fresh page per test */ });
```

**4. UI-Only Test Setup**
```typescript
// ❌ Anti-pattern — create test data via UI
test('delete user', async ({ page }) => {
  await page.goto('/register');
  await page.getByLabel('Name').fill('Test');
  // ... 10 steps to create user via UI
  await page.goto('/admin/users');
  await page.getByText('Test').click();
  await page.getByRole('button', { name: 'Delete' }).click();
});

// ✅ Solution — API setup, UI verification
test('delete user', async ({ page, request }) => {
  const { id } = await (await request.post('/api/users', { data: testUser })).json();
  await page.goto(`/admin/users/${id}`);
  await page.getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByText('User deleted')).toBeVisible();
});
```

**5. Testing Implementation Details**
```typescript
// ❌ Anti-pattern — asserting CSS classes, internal IDs
await expect(page.locator('.btn-primary.active')).toBeVisible();

// ✅ Solution — assert user-visible behavior
await expect(page.getByRole('button', { name: 'Confirm' })).toBeEnabled();
```

**6. Giant Test Files**
```typescript
// ❌ Anti-pattern — 1000-line spec file with 50 tests
// tests/everything.spec.ts

// ✅ Solution — domain-organized, small spec files
// tests/auth/login.spec.ts
// tests/checkout/payment.spec.ts
// tests/admin/user-management.spec.ts
```

**7. Missing Teardown**
```typescript
// ❌ Anti-pattern — creates data, never cleans up
test('create order', async ({ request }) => {
  await request.post('/api/orders', { data: orderData });
  // No cleanup → staging DB grows unbounded
});

// ✅ Solution — fixture-based teardown
factory: async ({ request }, use) => {
  const factory = new TestDataFactory(request);
  await use(factory);
  await factory.cleanup(); // Always runs, even on failure
},
```

**8. Ignoring Flaky Tests**
```typescript
// ❌ Anti-pattern — set retries: 5 and ignore root cause
// ✅ Solution — quarantine with test.fixme(), investigate, fix
test.fixme('payment flow - flaky on FF #JIRA-1234', async () => { ... });
```

**Real-world Usage:**
- Anti-pattern audits are run quarterly in enterprise frameworks; violations are tracked in tech debt backlog.
- Linting rules (custom ESLint plugins) automatically flag `waitForTimeout`, CSS class selectors, and missing `data-testid`.

**Tricky Follow-up Questions:**
1. *How would you introduce a linting rule that enforces `data-testid` over CSS class selectors in a team of 50 engineers?*
2. *What is "test coupling" and how does over-reliance on `test.describe` execution order create it?*

---

## Q50: How do you design a scalable, maintainable enterprise Playwright framework from scratch?

**Answer:**
A production-grade enterprise framework has these layers:

**Folder Structure:**
```
playwright-framework/
├── config/
│   ├── playwright.config.ts       # Main config
│   └── environment.ts             # Typed env vars
├── fixtures/
│   └── index.ts                   # All custom fixtures exported
├── pages/                         # Page objects
│   ├── LoginPage.ts
│   └── CheckoutPage.ts
├── components/                    # Component objects
│   ├── DataTable.ts
│   └── Modal.ts
├── api/                           # API client layer
│   ├── ApiClient.ts
│   ├── UsersApi.ts
│   └── OrdersApi.ts
├── utils/
│   ├── TestDataFactory.ts         # Data creation/cleanup
│   ├── PageFactory.ts             # Page object factory
│   └── RetryUtils.ts              # Custom retry helpers
├── reporters/
│   ├── SlackReporter.ts
│   └── JiraReporter.ts
├── auth/
│   ├── admin.json                 # .gitignored storage states
│   └── user.json
├── hars/                          # HAR files for replay mocking
├── fixtures/data/                 # Static test data
│   └── testUsers.ts
├── tests/
│   ├── auth/
│   ├── checkout/
│   ├── admin/
│   └── api/
└── global-setup.ts                # One-time auth + misc setup
```

**Architecture Principles:**
```typescript
// 1. Centralized config typed and validated at startup
// 2. All page objects via factory — no direct imports in tests
// 3. All test data via API factory — no UI setup
// 4. Auth via storageState — no UI login per test
// 5. Custom fixtures compose all dependencies
// 6. Parallel-safe by design — no shared mutable state
// 7. Tags drive CI pipelines: @smoke / @regression / @p0
// 8. Reporters push to Slack + Jira on failure
// 9. Docker container for visual regression baselines consistency
// 10. HAR files for third-party API mocking
```

```typescript
// The "master" fixture — everything a test needs
export const test = base.extend<{
  pages: PageFactory;
  api: ApiClient;
  factory: TestDataFactory;
  adminPage: Page;
  logger: TestLogger;
}>({
  factory: async ({ request }, use) => {
    const f = new TestDataFactory(request, process.env.ADMIN_TOKEN!);
    await use(f);
    await f.cleanup();
  },
  api: async ({ request }, use) => {
    await use(new ApiClient(request, config.apiURL));
  },
  pages: async ({ page }, use) => {
    await use(new PageFactory(page));
  },
  logger: async ({ page }, use, testInfo) => {
    const logger = new TestLogger();
    logger.attachToPage(page);
    await use(logger);
    if (testInfo.status !== 'passed') {
      await logger.attachToReport(testInfo);
    }
  },
});
```

**CI/CD Pipeline Design:**
```
PR Opened → @smoke (Chromium, 3 min) → Merge allowed
Merge to main → @regression (Chromium, 15 min) → Deploy to staging
Staging deploy → @smoke all browsers (10 min) → Deploy to production
Nightly → Full regression + visual regression + accessibility
Weekly → Cross-browser matrix + performance budget
```

**Real-world Usage:**
- FinTech platforms with 5000+ tests across 8 projects, 4 environments, 3 browsers, and 20 parallel workers.
- SaaS: Monorepo with 15 apps; each app has its own page objects but shares the core fixture/factory/API layer.

**Tricky Follow-up Questions:**
1. *How do you onboard a new engineer to a complex Playwright framework in under a day?*
2. *How do you measure and improve "test health" (reliability, speed, coverage) across a large test suite over time?*

---

*— End of Q31–Q50 — Section 1 Complete —*

> **Next:** Section 2 — Coding / Hands-On Questions (30 coding challenges)

---

---

# SECTION 2 — CODING / HANDS-ON QUESTIONS (30)

---

## C1: Build a reusable Login Helper with session caching

**Problem:** Every test in your suite requires authentication. The current approach performs UI login before every test, adding 5–8 seconds per test. With 400 tests, this wastes ~45 minutes per CI run.

**Difficulty:** Medium

**Task:** Implement a `LoginHelper` that:
1. Performs UI login once and caches the storage state to disk
2. Reuses the cached state for all subsequent tests
3. Supports multiple user roles (admin, user, readonly)
4. Automatically refreshes the cache if expired or missing

**Constraints:**
- Must be thread-safe for parallel workers
- Must not hardcode credentials
- Cache files must not be committed to source control

**Solution:**
```typescript
// utils/LoginHelper.ts
import { Browser, BrowserContext, Page, chromium } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export type UserRole = 'admin' | 'user' | 'readonly';

interface RoleCredentials {
  email: string;
  password: string;
}

const ROLE_CREDENTIALS: Record<UserRole, RoleCredentials> = {
  admin:    { email: process.env.ADMIN_EMAIL!,    password: process.env.ADMIN_PASSWORD! },
  user:     { email: process.env.USER_EMAIL!,     password: process.env.USER_PASSWORD! },
  readonly: { email: process.env.READONLY_EMAIL!, password: process.env.READONLY_PASSWORD! },
};

const AUTH_DIR = path.join(process.cwd(), 'auth');
const CACHE_TTL_MS = 4 * 60 * 60 * 1000; // 4 hours

function getStorageStatePath(role: UserRole): string {
  return path.join(AUTH_DIR, `${role}.json`);
}

function isCacheValid(filePath: string): boolean {
  if (!fs.existsSync(filePath)) return false;
  const stats = fs.statSync(filePath);
  return (Date.now() - stats.mtimeMs) < CACHE_TTL_MS;
}

export async function ensureAuthenticated(role: UserRole, baseURL: string): Promise<void> {
  const statePath = getStorageStatePath(role);
  if (isCacheValid(statePath)) return; // Cache hit — nothing to do

  if (!fs.existsSync(AUTH_DIR)) fs.mkdirSync(AUTH_DIR, { recursive: true });

  const browser = await chromium.launch();
  try {
    const context: BrowserContext = await browser.newContext({ baseURL });
    const page: Page = await context.newPage();
    const { email, password } = ROLE_CREDENTIALS[role];

    await page.goto('/login');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.waitForURL('/dashboard', { timeout: 15_000 });

    await context.storageState({ path: statePath });
  } finally {
    await browser.close();
  }
}
```

```typescript
// global-setup.ts — pre-warm all role caches before tests run
import { FullConfig } from '@playwright/test';
import { ensureAuthenticated, UserRole } from './utils/LoginHelper';

export default async function globalSetup(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0].use.baseURL as string;
  const roles: UserRole[] = ['admin', 'user', 'readonly'];

  // Parallel auth for all roles
  await Promise.all(roles.map(role => ensureAuthenticated(role, baseURL)));
  console.log('[Setup] All auth states cached');
}
```

```typescript
// fixtures/index.ts — role-specific authenticated pages
import { test as base, BrowserContext } from '@playwright/test';
import { UserRole } from '../utils/LoginHelper';

type AuthFixtures = { asAdmin: BrowserContext; asUser: BrowserContext; asReadonly: BrowserContext; };

export const test = base.extend<AuthFixtures>({
  asAdmin: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: 'auth/admin.json' });
    await use(ctx);
    await ctx.close();
  },
  asUser: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: 'auth/user.json' });
    await use(ctx);
    await ctx.close();
  },
  asReadonly: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: 'auth/readonly.json' });
    await use(ctx);
    await ctx.close();
  },
});
```

```typescript
// Test using the helper
import { test, expect } from '../fixtures';

test('admin sees delete button, readonly does not', async ({ asAdmin, asReadonly }) => {
  const adminPage = await asAdmin.newPage();
  const readonlyPage = await asReadonly.newPage();

  await adminPage.goto('/users');
  await readonlyPage.goto('/users');

  await expect(adminPage.getByRole('button', { name: 'Delete' })).toBeVisible();
  await expect(readonlyPage.getByRole('button', { name: 'Delete' })).not.toBeVisible();
});
```

**Alternative Approach:** Use API token injection instead of UI login for JWT-based apps — eliminates browser overhead entirely.

**Common Mistakes:**
- Using `workerStoragePath` without locking — parallel workers overwrite each other's cache file.
- Not checking cache staleness — expired tokens cause all tests to fail mid-run.

---

## C2: Implement a Dynamic Locator Utility

**Problem:** Your app renders data tables with dynamic row content. Tests need to locate action buttons in a specific row by row data, not by row index.

**Difficulty:** Medium

**Task:** Build a `TableHelper` class that:
1. Locates a row by any cell value
2. Returns the value of any column in that row
3. Clicks an action button within a specific row
4. Supports sorting and verifies post-sort order

**Solution:**
```typescript
// components/TableHelper.ts
import { Locator, Page, expect } from '@playwright/test';

export class TableHelper {
  private tableLocator: Locator;

  constructor(private page: Page, tableSelector: string) {
    this.tableLocator = page.locator(tableSelector);
  }

  /** Get all column headers */
  private async getHeaders(): Promise<string[]> {
    return this.tableLocator
      .getByRole('columnheader')
      .allInnerTexts();
  }

  /** Get the 0-based index of a column by its header text */
  private async getColumnIndex(header: string): Promise<number> {
    const headers = await this.getHeaders();
    const idx = headers.findIndex(h => h.trim() === header);
    if (idx === -1) throw new Error(`Column "${header}" not found. Available: ${headers.join(', ')}`);
    return idx;
  }

  /** Find a row that contains a specific text value */
  row(cellText: string): Locator {
    return this.tableLocator
      .getByRole('row')
      .filter({ hasText: cellText });
  }

  /** Get the value of a specific column in the row matching rowText */
  async getCellValue(rowText: string, columnHeader: string): Promise<string> {
    const colIdx = await this.getColumnIndex(columnHeader);
    return this.row(rowText)
      .getByRole('cell')
      .nth(colIdx)
      .innerText();
  }

  /** Click an action button within a specific row */
  async clickRowAction(rowText: string, actionName: string): Promise<void> {
    await this.row(rowText)
      .getByRole('button', { name: actionName })
      .click();
  }

  /** Get all values in a specific column */
  async getColumnValues(columnHeader: string): Promise<string[]> {
    const colIdx = await this.getColumnIndex(columnHeader);
    const rows = this.tableLocator.getByRole('row').filter({ hasNot: this.tableLocator.getByRole('columnheader') });
    const cells = rows.getByRole('cell').nth(colIdx);
    return cells.allInnerTexts();
  }

  /** Sort by column and verify direction */
  async sortBy(columnHeader: string, direction: 'asc' | 'desc'): Promise<void> {
    const header = this.tableLocator.getByRole('columnheader', { name: columnHeader });
    await header.click();

    // Wait for re-render
    await this.page.waitForLoadState('domcontentloaded');

    const values = await this.getColumnValues(columnHeader);
    const sorted = [...values].sort((a, b) =>
      direction === 'asc' ? a.localeCompare(b) : b.localeCompare(a)
    );
    expect(values).toEqual(sorted);
  }

  /** Assert total visible row count */
  async expectRowCount(count: number): Promise<void> {
    // Subtract 1 for header row
    await expect(this.tableLocator.getByRole('row')).toHaveCount(count + 1);
  }
}
```

```typescript
// Usage in tests
import { test, expect } from '@playwright/test';
import { TableHelper } from '../components/TableHelper';

test('manage users table', async ({ page }) => {
  await page.goto('/admin/users');

  const usersTable = new TableHelper(page, '[data-testid="users-table"]');

  // Verify row count
  await usersTable.expectRowCount(10);

  // Get specific cell value
  const role = await usersTable.getCellValue('alice@example.com', 'Role');
  expect(role).toBe('Admin');

  // Sort and verify
  await usersTable.sortBy('Name', 'asc');

  // Action in a row
  await usersTable.clickRowAction('bob@example.com', 'Edit');
  await expect(page.getByRole('dialog', { name: 'Edit User' })).toBeVisible();
});
```

**Alternative Approach:** For tables with virtualized rows (only rendering visible rows), use `page.evaluate()` to scroll and collect all data.

---

## C3: Build a Generic API Request Validator

**Problem:** Your test suite makes dozens of API calls. Error messages are cryptic (`Expected 200, got 422`) and debugging requires reading raw response bodies.

**Difficulty:** Medium

**Task:** Build an `ApiClient` wrapper that:
1. Adds auth headers automatically
2. Validates status codes and throws descriptive errors
3. Returns typed response bodies
4. Logs request/response for failed calls
5. Supports retry for transient errors

**Solution:**
```typescript
// api/ApiClient.ts
import { APIRequestContext, APIResponse } from '@playwright/test';

interface RequestOptions {
  params?: Record<string, string | number | boolean>;
  data?: unknown;
  headers?: Record<string, string>;
  retries?: number;
}

interface ApiError extends Error {
  status: number;
  url: string;
  responseBody: string;
}

export class ApiClient {
  constructor(
    private request: APIRequestContext,
    private baseURL: string,
    private authToken: string
  ) {}

  private buildHeaders(extra?: Record<string, string>): Record<string, string> {
    return {
      Authorization: `Bearer ${this.authToken}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...extra,
    };
  }

  private createError(method: string, url: string, res: APIResponse, body: string): ApiError {
    const err = new Error(
      `[${method}] ${url} → ${res.status()}\nResponse: ${body.slice(0, 500)}`
    ) as ApiError;
    err.status = res.status();
    err.url = url;
    err.responseBody = body;
    return err;
  }

  private async executeWithRetry(
    fn: () => Promise<APIResponse>,
    retries: number,
    method: string,
    url: string
  ): Promise<APIResponse> {
    for (let attempt = 0; attempt <= retries; attempt++) {
      const res = await fn();
      if (res.status() < 500 || attempt === retries) return res;
      console.warn(`[ApiClient] Retry ${attempt + 1}/${retries} for ${method} ${url} (${res.status()})`);
      await new Promise(r => setTimeout(r, 1000 * (attempt + 1))); // Exponential backoff
    }
    throw new Error('Unreachable');
  }

  async get<T>(path: string, options?: RequestOptions): Promise<T> {
    const url = `${this.baseURL}${path}`;
    const res = await this.executeWithRetry(
      () => this.request.get(url, {
        headers: this.buildHeaders(options?.headers),
        params: options?.params as Record<string, string>,
      }),
      options?.retries ?? 0,
      'GET', url
    );

    if (!res.ok()) {
      throw this.createError('GET', url, res, await res.text());
    }
    return res.json() as Promise<T>;
  }

  async post<T>(path: string, body: unknown, options?: RequestOptions): Promise<T> {
    const url = `${this.baseURL}${path}`;
    const res = await this.executeWithRetry(
      () => this.request.post(url, {
        data: body,
        headers: this.buildHeaders(options?.headers),
      }),
      options?.retries ?? 0,
      'POST', url
    );

    if (res.status() !== 201 && res.status() !== 200) {
      throw this.createError('POST', url, res, await res.text());
    }
    return res.json() as Promise<T>;
  }

  async delete(path: string, options?: RequestOptions): Promise<void> {
    const url = `${this.baseURL}${path}`;
    const res = await this.request.delete(url, {
      headers: this.buildHeaders(options?.headers),
    });
    if (res.status() !== 204 && res.status() !== 200) {
      throw this.createError('DELETE', url, res, await res.text());
    }
  }

  async patch<T>(path: string, body: unknown): Promise<T> {
    const url = `${this.baseURL}${path}`;
    const res = await this.request.patch(url, {
      data: body,
      headers: this.buildHeaders(),
    });
    if (!res.ok()) throw this.createError('PATCH', url, res, await res.text());
    return res.json() as Promise<T>;
  }
}
```

```typescript
// Typed domain-specific API on top of ApiClient
// api/UsersApi.ts
import { ApiClient } from './ApiClient';

interface User { id: string; email: string; name: string; role: string; }
interface CreateUserDto { name: string; email: string; role: string; }

export class UsersApi {
  constructor(private client: ApiClient) {}

  async getAll(page = 1, limit = 20): Promise<{ data: User[]; total: number }> {
    return this.client.get('/api/users', { params: { page, limit } });
  }

  async getById(id: string): Promise<User> {
    return this.client.get(`/api/users/${id}`);
  }

  async create(dto: CreateUserDto): Promise<User> {
    return this.client.post('/api/users', dto);
  }

  async delete(id: string): Promise<void> {
    return this.client.delete(`/api/users/${id}`);
  }
}
```

**Alternative Approach:** Use `got` or `axios` as an HTTP client alongside Playwright's `APIRequestContext` — trade-off is losing the shared cookie/session context.

---

## C4: Implement Network Request Capture and Assertion Utility

**Problem:** You need to verify that a UI action sends the correct API request with exact payload — a critical need for FinTech transaction flows.

**Difficulty:** Medium

**Task:** Build a `NetworkCapture` utility that:
1. Captures all requests matching a pattern during a UI interaction
2. Asserts request method, headers, and body
3. Asserts response status and body
4. Works with async/race-safe Promise patterns

**Solution:**
```typescript
// utils/NetworkCapture.ts
import { Page, Request, Response } from '@playwright/test';

interface CapturedExchange {
  request: {
    url: string;
    method: string;
    headers: Record<string, string>;
    body: unknown;
  };
  response: {
    status: number;
    headers: Record<string, string>;
    body: unknown;
  };
}

export class NetworkCapture {
  constructor(private page: Page) {}

  /**
   * Capture the first matching request/response pair during an action.
   * Uses Promise.all to avoid race conditions.
   */
  async capture(
    urlPattern: string | RegExp,
    action: () => Promise<void>,
    options: { method?: string; timeout?: number } = {}
  ): Promise<CapturedExchange> {
    const [response] = await Promise.all([
      this.page.waitForResponse(
        (res) => {
          const matchesUrl = typeof urlPattern === 'string'
            ? res.url().includes(urlPattern)
            : urlPattern.test(res.url());
          const matchesMethod = options.method
            ? res.request().method() === options.method.toUpperCase()
            : true;
          return matchesUrl && matchesMethod;
        },
        { timeout: options.timeout ?? 10_000 }
      ),
      action(),
    ]);

    const req = response.request();
    let requestBody: unknown = null;
    try {
      requestBody = JSON.parse(req.postData() ?? 'null');
    } catch { requestBody = req.postData(); }

    let responseBody: unknown = null;
    try {
      responseBody = await response.json();
    } catch { responseBody = await response.text(); }

    return {
      request: {
        url: req.url(),
        method: req.method(),
        headers: await req.allHeaders(),
        body: requestBody,
      },
      response: {
        status: response.status(),
        headers: response.headers(),
        body: responseBody,
      },
    };
  }

  /**
   * Capture ALL matching requests during an action (e.g., analytics batch calls).
   */
  async captureAll(
    urlPattern: string | RegExp,
    action: () => Promise<void>,
    waitAfterMs = 500
  ): Promise<CapturedExchange[]> {
    const exchanges: CapturedExchange[] = [];

    const handler = async (res: Response) => {
      const matchesUrl = typeof urlPattern === 'string'
        ? res.url().includes(urlPattern)
        : urlPattern.test(res.url());
      if (!matchesUrl) return;

      let body: unknown = null;
      try { body = await res.json(); } catch { body = await res.text(); }

      exchanges.push({
        request: {
          url: res.request().url(),
          method: res.request().method(),
          headers: await res.request().allHeaders(),
          body: (() => { try { return JSON.parse(res.request().postData() ?? 'null'); } catch { return null; } })(),
        },
        response: { status: res.status(), headers: res.headers(), body },
      });
    };

    this.page.on('response', handler);
    await action();
    await this.page.waitForTimeout(waitAfterMs); // Allow in-flight requests to complete
    this.page.off('response', handler);

    return exchanges;
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { NetworkCapture } from '../utils/NetworkCapture';

test('place order sends correct payload', async ({ page }) => {
  await page.goto('/checkout');
  const capture = new NetworkCapture(page);

  const exchange = await capture.capture(
    '/api/orders',
    async () => page.getByRole('button', { name: 'Place Order' }).click(),
    { method: 'POST' }
  );

  expect(exchange.request.method).toBe('POST');
  expect(exchange.request.headers['authorization']).toMatch(/^Bearer /);
  expect(exchange.request.body).toMatchObject({
    currency: 'USD',
    items: expect.arrayContaining([
      expect.objectContaining({ quantity: expect.any(Number) }),
    ]),
  });
  expect(exchange.response.status).toBe(201);
  expect((exchange.response.body as any).id).toMatch(/^ord-/);
});
```

---

## C5: Build a Retry Utility for Flaky Async Operations

**Problem:** Your test environment has a flaky message queue — an action on the UI triggers a background job that updates a record; the update takes 100ms–3s depending on load.

**Difficulty:** Medium

**Task:** Build a `waitUntil` utility that polls a condition function until it passes or a timeout is reached, with configurable interval and descriptive error messages.

**Solution:**
```typescript
// utils/waitUntil.ts
interface WaitUntilOptions {
  timeout?: number;   // Max wait time in ms (default: 10000)
  interval?: number;  // Poll interval in ms (default: 500)
  message?: string;   // Error message on timeout
}

/**
 * Polls `condition` every `interval` ms until it returns true or timeout is reached.
 * Useful for eventually-consistent state assertions.
 */
export async function waitUntil(
  condition: () => Promise<boolean> | boolean,
  options: WaitUntilOptions = {}
): Promise<void> {
  const { timeout = 10_000, interval = 500, message = 'Condition not met within timeout' } = options;
  const startTime = Date.now();

  while (true) {
    try {
      const result = await condition();
      if (result) return;
    } catch {
      // Condition threw — treat as not met yet
    }

    if (Date.now() - startTime >= timeout) {
      throw new Error(`${message} (waited ${timeout}ms, polled every ${interval}ms)`);
    }

    await new Promise(resolve => setTimeout(resolve, interval));
  }
}

/**
 * Retries an async function up to `maxAttempts` times.
 * Throws the last error if all attempts fail.
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  options: { maxAttempts?: number; delayMs?: number; shouldRetry?: (err: Error) => boolean } = {}
): Promise<T> {
  const { maxAttempts = 3, delayMs = 1000, shouldRetry = () => true } = options;

  let lastError: Error = new Error('Unknown error');
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (err) {
      lastError = err as Error;
      if (attempt === maxAttempts || !shouldRetry(lastError)) throw lastError;
      console.warn(`[Retry] Attempt ${attempt}/${maxAttempts} failed: ${lastError.message}`);
      await new Promise(r => setTimeout(r, delayMs * attempt)); // Exponential backoff
    }
  }
  throw lastError;
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { waitUntil, withRetry } from '../utils/waitUntil';

test('background job completes and updates UI', async ({ page, request }) => {
  await page.goto('/reports');
  await page.getByRole('button', { name: 'Generate Report' }).click();

  // Wait until the API confirms the job is complete (eventually consistent)
  await waitUntil(
    async () => {
      const res = await request.get('/api/reports/latest');
      const report = await res.json();
      return report.status === 'completed';
    },
    { timeout: 30_000, interval: 1000, message: 'Report generation did not complete' }
  );

  await expect(page.getByTestId('report-download-btn')).toBeEnabled();
});

test('flaky external API with retry', async ({ request }) => {
  const result = await withRetry(
    async () => {
      const res = await request.post('/api/external/notify', { data: { userId: 'usr-001' } });
      if (res.status() === 503) throw new Error('Service temporarily unavailable');
      return res.json();
    },
    {
      maxAttempts: 3,
      delayMs: 2000,
      shouldRetry: (err) => err.message.includes('temporarily unavailable'),
    }
  );
  expect(result.sent).toBe(true);
});
```

**Alternative Approach:** Use Playwright's built-in `expect.poll()` for condition polling with automatic retries and better error messages:
```typescript
await expect.poll(
  async () => {
    const res = await request.get('/api/reports/latest');
    return (await res.json()).status;
  },
  { message: 'Report did not complete', timeout: 30_000, intervals: [1000, 2000] }
).toBe('completed');
```

---

## C6: Implement a Page Navigation and URL Assertion Utility

**Problem:** Your app has complex URL patterns with query params, hash fragments, and dynamic segments. Tests need reliable URL assertion without brittle string matching.

**Difficulty:** Medium

**Task:** Build a `NavigationHelper` that handles URL assertions, navigation history validation, and back/forward navigation with state verification.

**Solution:**
```typescript
// utils/NavigationHelper.ts
import { Page, expect } from '@playwright/test';

interface UrlExpectation {
  pathname?: string | RegExp;
  params?: Record<string, string>;
  hash?: string;
}

export class NavigationHelper {
  constructor(private page: Page) {}

  /** Assert URL shape without hardcoding full URL */
  async expectUrl(expectation: UrlExpectation): Promise<void> {
    const url = new URL(this.page.url());

    if (expectation.pathname) {
      if (typeof expectation.pathname === 'string') {
        expect(url.pathname).toBe(expectation.pathname);
      } else {
        expect(url.pathname).toMatch(expectation.pathname);
      }
    }

    if (expectation.params) {
      for (const [key, value] of Object.entries(expectation.params)) {
        expect(url.searchParams.get(key)).toBe(value);
      }
    }

    if (expectation.hash) {
      expect(url.hash).toBe(`#${expectation.hash}`);
    }
  }

  /** Navigate and wait for both URL change and load completion */
  async goto(path: string, waitForState: 'load' | 'networkidle' | 'domcontentloaded' = 'load'): Promise<void> {
    await this.page.goto(path);
    await this.page.waitForLoadState(waitForState);
  }

  /** Assert navigation happens after an action */
  async expectNavigationAfter(
    action: () => Promise<void>,
    expectedUrlPattern: string | RegExp
  ): Promise<void> {
    await Promise.all([
      this.page.waitForURL(expectedUrlPattern),
      action(),
    ]);
  }

  /** Go back and assert previous URL */
  async goBackAndExpect(expectedUrlPattern: string | RegExp): Promise<void> {
    await this.page.goBack();
    await expect(this.page).toHaveURL(expectedUrlPattern);
  }

  /** Assert that an action does NOT navigate (stays on same page) */
  async expectNoNavigation(action: () => Promise<void>, timeout = 2000): Promise<void> {
    const currentUrl = this.page.url();
    await action();
    await this.page.waitForTimeout(timeout);
    expect(this.page.url()).toBe(currentUrl);
  }

  /** Get current URL parts */
  getURLParts(): { pathname: string; params: URLSearchParams; hash: string } {
    const url = new URL(this.page.url());
    return { pathname: url.pathname, params: url.searchParams, hash: url.hash };
  }
}
```

```typescript
// Usage
test('checkout navigation flow', async ({ page }) => {
  const nav = new NavigationHelper(page);

  await nav.goto('/products');

  await nav.expectNavigationAfter(
    () => page.getByRole('link', { name: 'MacBook Pro' }).click(),
    /\/products\/\d+/
  );

  await nav.expectUrl({
    pathname: /\/products\/\d+/,
    params: {},
  });

  await page.getByRole('button', { name: 'Add to Cart' }).click();
  await nav.expectNoNavigation(
    () => page.getByRole('button', { name: 'Continue Shopping' }).click()
  );

  await nav.goto('/checkout');
  await nav.expectUrl({ pathname: '/checkout' });

  await page.getByRole('button', { name: 'Place Order' }).click();
  await nav.expectUrl({ pathname: /\/orders\/ord-/ });
});
```

---

## C7: Build a Multi-Tab Utility for OAuth and New Window Flows

**Problem:** Your app's "Share" feature opens a new tab with a shareable link. The OAuth login redirects through an external provider in a popup. Tests must handle both scenarios reliably.

**Difficulty:** Hard

**Task:** Build a `TabManager` utility that:
1. Captures new tabs spawned by actions
2. Waits for them to reach a stable state
3. Switches focus between tabs
4. Closes tabs and resumes the original

**Solution:**
```typescript
// utils/TabManager.ts
import { BrowserContext, Page } from '@playwright/test';

export class TabManager {
  private pages: Page[] = [];

  constructor(private context: BrowserContext) {
    // Track all pages in the context
    this.pages = context.pages();
    context.on('page', (page) => this.pages.push(page));
  }

  /**
   * Perform an action that opens a new tab, capture and return it.
   */
  async openNewTab(
    action: () => Promise<void>,
    options: { waitForURL?: string | RegExp; loadState?: 'load' | 'networkidle' } = {}
  ): Promise<Page> {
    const [newPage] = await Promise.all([
      this.context.waitForEvent('page'),
      action(),
    ]);

    if (options.waitForURL) {
      await newPage.waitForURL(options.waitForURL, { timeout: 15_000 });
    } else {
      await newPage.waitForLoadState(options.loadState ?? 'domcontentloaded');
    }

    return newPage;
  }

  /** Get the current focused page */
  getCurrentPage(): Page {
    return this.pages[this.pages.length - 1];
  }

  /** Switch focus to the tab at a given index */
  getTabAt(index: number): Page {
    if (index >= this.pages.length) {
      throw new Error(`Tab index ${index} out of range (${this.pages.length} open tabs)`);
    }
    return this.pages[index];
  }

  /** Close a tab and return focus to the first tab */
  async closeTab(page: Page): Promise<Page> {
    await page.close();
    this.pages = this.context.pages();
    return this.pages[0];
  }

  /** Close all tabs except the first */
  async closeAllExceptFirst(): Promise<void> {
    const extras = this.context.pages().slice(1);
    await Promise.all(extras.map(p => p.close()));
    this.pages = this.context.pages();
  }

  /** Wait for a tab with a specific URL pattern to open (e.g., OAuth popup) */
  async waitForTabWithUrl(urlPattern: string | RegExp, timeoutMs = 10_000): Promise<Page> {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
      const match = this.context.pages().find(p =>
        typeof urlPattern === 'string'
          ? p.url().includes(urlPattern)
          : urlPattern.test(p.url())
      );
      if (match) return match;
      await new Promise(r => setTimeout(r, 200));
    }
    throw new Error(`No tab with URL matching ${urlPattern} opened within ${timeoutMs}ms`);
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { TabManager } from '../utils/TabManager';

test('share creates new tab with correct URL', async ({ page, context }) => {
  const tabs = new TabManager(context);
  await page.goto('/reports/annual-2024');

  const shareTab = await tabs.openNewTab(
    () => page.getByRole('button', { name: 'Share' }).click(),
    { waitForURL: /\/shared\// }
  );

  expect(shareTab.url()).toMatch(/\/shared\/[a-zA-Z0-9]+/);
  await expect(shareTab.getByRole('heading', { name: 'Annual Report 2024' })).toBeVisible();

  await tabs.closeTab(shareTab);
  await expect(page.getByRole('heading', { name: 'Annual Report 2024' })).toBeVisible();
});

test('OAuth popup login flow', async ({ page, context }) => {
  const tabs = new TabManager(context);
  await page.goto('/login');

  const oauthTab = await tabs.openNewTab(
    () => page.getByRole('button', { name: 'Continue with Google' }).click(),
    { waitForURL: /accounts\.google\.com/ }
  );

  // Interact with the OAuth page (mocked in tests)
  await oauthTab.getByLabel('Email').fill('test@example.com');
  await oauthTab.getByRole('button', { name: 'Next' }).click();

  // OAuth tab closes after successful login
  await oauthTab.waitForEvent('close', { timeout: 10_000 });

  // Main page should now be authenticated
  await page.waitForURL('/dashboard');
  await expect(page.getByText('test@example.com')).toBeVisible();
});
```

---

## C8: Implement a Visual Regression Helper with Dynamic Masking

**Problem:** Your dashboard has live pricing data, timestamps, and user avatars that change constantly, making visual regression tests always fail.

**Difficulty:** Medium

**Task:** Build a `VisualHelper` that:
1. Takes full-page screenshots with configurable masking
2. Stabilizes animations before capture
3. Manages snapshot naming by test name and viewport
4. Provides diff threshold configuration

**Solution:**
```typescript
// utils/VisualHelper.ts
import { Page, Locator, TestInfo, expect } from '@playwright/test';

interface ScreenshotOptions {
  masks?: Locator[];
  fullPage?: boolean;
  threshold?: number;
  maxDiffPixelRatio?: number;
  waitForNetworkIdle?: boolean;
  stabilizeAnimations?: boolean;
}

export class VisualHelper {
  constructor(private page: Page, private testInfo: TestInfo) {}

  /** Disable all CSS animations and transitions for stable screenshots */
  private async stabilize(): Promise<void> {
    await this.page.addStyleTag({
      content: `
        *, *::before, *::after {
          animation-duration: 0s !important;
          animation-delay: 0s !important;
          transition-duration: 0s !important;
          transition-delay: 0s !important;
        }
      `,
    });
  }

  /** Take a screenshot and compare against baseline */
  async assertMatchesSnapshot(snapshotName: string, options: ScreenshotOptions = {}): Promise<void> {
    const {
      masks = [],
      fullPage = true,
      threshold = 0.2,
      maxDiffPixelRatio = 0.02,
      waitForNetworkIdle = true,
      stabilizeAnimations = true,
    } = options;

    if (waitForNetworkIdle) {
      await this.page.waitForLoadState('networkidle');
    }

    if (stabilizeAnimations) {
      await this.stabilize();
    }

    // Auto-mask common dynamic elements if not explicitly specified
    const defaultMasks: Locator[] = [
      this.page.locator('[data-testid="timestamp"]'),
      this.page.locator('[data-testid="live-price"]'),
      this.page.locator('[data-testid="user-avatar"]'),
      this.page.locator('.skeleton-loader'),
      ...masks,
    ];

    // Filter to only existing elements (avoid errors for non-present masks)
    const activeMasks: Locator[] = [];
    for (const mask of defaultMasks) {
      if ((await mask.count()) > 0) activeMasks.push(mask);
    }

    const viewport = this.page.viewportSize();
    const fullName = `${snapshotName}-${viewport?.width}x${viewport?.height}.png`;

    await expect(this.page).toHaveScreenshot(fullName, {
      fullPage,
      mask: activeMasks,
      threshold,
      maxDiffPixelRatio,
      animations: 'disabled',
    });
  }

  /** Capture a specific element (component-level regression) */
  async assertElementMatchesSnapshot(
    element: Locator,
    snapshotName: string,
    options: Pick<ScreenshotOptions, 'masks' | 'threshold'> = {}
  ): Promise<void> {
    await expect(element).toHaveScreenshot(`${snapshotName}.png`, {
      mask: options.masks ?? [],
      threshold: options.threshold ?? 0.1,
    });
  }
}
```

```typescript
// Usage
import { test } from '@playwright/test';
import { VisualHelper } from '../utils/VisualHelper';

test('dashboard visual regression', async ({ page }, testInfo) => {
  await page.goto('/dashboard');
  const visual = new VisualHelper(page, testInfo);

  await visual.assertMatchesSnapshot('dashboard-overview', {
    masks: [
      page.getByTestId('portfolio-chart'), // Chart has animated entry
      page.locator('.notification-badge'),
    ],
    maxDiffPixelRatio: 0.01,
  });
});

test('button component variants', async ({ page }, testInfo) => {
  await page.goto('/components/buttons');
  const visual = new VisualHelper(page, testInfo);

  await visual.assertElementMatchesSnapshot(
    page.getByTestId('button-showcase'),
    'button-variants',
    { threshold: 0.05 }
  );
});
```

---

## C9: Build an Authentication State Manager for Multi-Role Tests

**Problem:** A test scenario requires both an admin and a standard user to interact with the same feature simultaneously, each in their own browser context.

**Difficulty:** Hard

**Task:** Build an `AuthStateManager` that provisions multiple authenticated browser contexts and manages their lifecycle within a single test.

**Solution:**
```typescript
// utils/AuthStateManager.ts
import { Browser, BrowserContext, Page } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

export type Role = 'admin' | 'manager' | 'user' | 'readonly';

interface RoleSession {
  context: BrowserContext;
  page: Page;
  role: Role;
}

export class AuthStateManager {
  private sessions = new Map<Role, RoleSession>();

  constructor(private browser: Browser) {}

  async createSession(role: Role): Promise<RoleSession> {
    const statePath = path.join(process.cwd(), 'auth', `${role}.json`);
    if (!fs.existsSync(statePath)) {
      throw new Error(
        `Auth state for role "${role}" not found at ${statePath}. ` +
        `Run global-setup to generate it.`
      );
    }

    const context = await this.browser.newContext({
      storageState: statePath,
      baseURL: process.env.BASE_URL,
    });
    const page = await context.newPage();
    const session: RoleSession = { context, page, role };
    this.sessions.set(role, session);
    return session;
  }

  getSession(role: Role): RoleSession {
    const session = this.sessions.get(role);
    if (!session) throw new Error(`Session for role "${role}" not created. Call createSession() first.`);
    return session;
  }

  getPage(role: Role): Page {
    return this.getSession(role).page;
  }

  /** Create sessions for multiple roles in parallel */
  async createSessions(roles: Role[]): Promise<Map<Role, RoleSession>> {
    await Promise.all(roles.map(r => this.createSession(r)));
    return this.sessions;
  }

  /** Close all sessions */
  async closeAll(): Promise<void> {
    await Promise.all(
      Array.from(this.sessions.values()).map(s => s.context.close())
    );
    this.sessions.clear();
  }
}
```

```typescript
// Usage: multi-role test
import { test, expect } from '@playwright/test';
import { AuthStateManager } from '../utils/AuthStateManager';

test('manager approves, user sees approval notification', async ({ browser }) => {
  const auth = new AuthStateManager(browser);

  try {
    await auth.createSessions(['manager', 'user']);

    const managerPage = auth.getPage('manager');
    const userPage = auth.getPage('user');

    // User submits a request
    await userPage.goto('/requests/new');
    await userPage.getByLabel('Title').fill('Budget Increase Request');
    await userPage.getByRole('button', { name: 'Submit for Approval' }).click();
    const requestId = await userPage.getByTestId('request-id').innerText();

    // Manager approves it
    await managerPage.goto(`/approvals/${requestId}`);
    await managerPage.getByRole('button', { name: 'Approve' }).click();
    await expect(managerPage.getByText('Request approved')).toBeVisible();

    // User sees the notification
    await userPage.reload();
    await expect(userPage.getByTestId('notification')).toContainText('approved');
  } finally {
    await auth.closeAll();
  }
});
```

---

## C10: Implement a Frame Handling Utility for Payment Forms

**Problem:** Your checkout flow embeds a Stripe payment iframe and a 3DS authentication iframe. Both must be interacted with in sequence.

**Difficulty:** Hard

**Task:** Build a `FrameHelper` that:
1. Waits for specific iframes to load by URL pattern or title
2. Provides scoped interaction methods
3. Handles the 3DS redirect iframe chain

**Solution:**
```typescript
// utils/FrameHelper.ts
import { Page, FrameLocator, Locator } from '@playwright/test';

export class FrameHelper {
  constructor(private page: Page) {}

  /** Get a FrameLocator scoped to a URL pattern */
  byUrl(urlPattern: string): FrameLocator {
    return this.page.frameLocator(`iframe[src*="${urlPattern}"]`);
  }

  /** Get a FrameLocator by iframe name attribute */
  byName(name: string): FrameLocator {
    return this.page.frameLocator(`iframe[name="${name}"]`);
  }

  /** Get a FrameLocator by title attribute */
  byTitle(title: string): FrameLocator {
    return this.page.frameLocator(`iframe[title="${title}"]`);
  }

  /** Wait for an iframe to appear and return its FrameLocator */
  async waitForFrame(
    selector: string,
    contentSelector: string,
    timeoutMs = 10_000
  ): Promise<FrameLocator> {
    const frame = this.page.frameLocator(selector);
    await frame.locator(contentSelector).waitFor({ timeout: timeoutMs });
    return frame;
  }

  /** Fill Stripe card details */
  async fillStripeCard(details: {
    number: string;
    expiry: string;
    cvc: string;
    name?: string;
  }): Promise<void> {
    const stripe = this.byUrl('js.stripe.com');
    await stripe.getByLabel('Card number').fill(details.number);
    await stripe.getByLabel('Expiration').fill(details.expiry);
    await stripe.getByLabel('CVC').fill(details.cvc);
    if (details.name) {
      await stripe.getByLabel('Name on card').fill(details.name);
    }
  }

  /** Handle 3DS authentication challenge */
  async handle3DSChallenge(action: '3ds-challenge' | 'complete'): Promise<void> {
    // Wait for 3DS iframe to appear
    const challengeFrame = await this.waitForFrame(
      'iframe[name="__privateStripeFrame"]',
      '[data-testid="3ds-frame"]',
      15_000
    );

    if (action === '3ds-challenge') {
      // The outer 3DS frame contains a nested frame
      const innerFrame = challengeFrame.frameLocator('iframe');
      await innerFrame.getByRole('button', { name: 'Complete authentication' }).click();
    }
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { FrameHelper } from '../utils/FrameHelper';

test('complete 3DS payment flow', async ({ page }) => {
  const frames = new FrameHelper(page);
  await page.goto('/checkout');

  // Fill cart...
  await page.getByRole('button', { name: 'Proceed to Payment' }).click();

  // Fill Stripe card form in iframe
  await frames.fillStripeCard({
    number: '4000002500003155', // 3DS required card
    expiry: '12/30',
    cvc: '123',
    name: 'Test User',
  });

  await page.getByRole('button', { name: 'Pay $99.99' }).click();

  // Handle 3DS challenge
  await frames.handle3DSChallenge('3ds-challenge');

  await expect(page).toHaveURL(/\/orders\/confirmation/);
  await expect(page.getByTestId('payment-status')).toHaveText('Payment successful');
});
```

---

## C11: Build a Download Verification Utility

**Problem:** Your reporting module generates PDF, CSV, and Excel reports. Tests must verify downloads contain correct content and proper metadata.

**Difficulty:** Medium

**Task:** Build a `DownloadHelper` that captures downloads, saves them, and provides content verification methods.

**Solution:**
```typescript
// utils/DownloadHelper.ts
import { Page, Download } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export class DownloadHelper {
  private downloadsDir: string;

  constructor(private page: Page, downloadsDir = 'test-downloads') {
    this.downloadsDir = path.join(process.cwd(), downloadsDir);
    if (!fs.existsSync(this.downloadsDir)) {
      fs.mkdirSync(this.downloadsDir, { recursive: true });
    }
  }

  /** Trigger an action that causes a download and return the saved file path */
  async captureDownload(action: () => Promise<void>): Promise<{ path: string; filename: string; download: Download }> {
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      action(),
    ]);

    const filename = download.suggestedFilename();
    const savePath = path.join(this.downloadsDir, `${Date.now()}-${filename}`);
    await download.saveAs(savePath);

    if (download.failure()) {
      throw new Error(`Download failed: ${download.failure()}`);
    }

    return { path: savePath, filename, download };
  }

  /** Assert CSV file contains specific rows and headers */
  assertCsvContains(filePath: string, options: { headers?: string[]; rows?: string[][] }): void {
    const content = fs.readFileSync(filePath, 'utf-8');
    const lines = content.trim().split('\n').map(l => l.split(',').map(c => c.trim().replace(/"/g, '')));

    if (options.headers) {
      for (const header of options.headers) {
        if (!lines[0].includes(header)) {
          throw new Error(`CSV header "${header}" not found. Headers: ${lines[0].join(', ')}`);
        }
      }
    }

    if (options.rows) {
      for (const expectedRow of options.rows) {
        const found = lines.some(line => expectedRow.every(val => line.includes(val)));
        if (!found) throw new Error(`CSV row ${JSON.stringify(expectedRow)} not found`);
      }
    }
  }

  /** Assert PDF is valid (has PDF header) */
  assertIsPdf(filePath: string): void {
    const buffer = fs.readFileSync(filePath);
    const header = buffer.slice(0, 5).toString('ascii');
    if (header !== '%PDF-') throw new Error(`File is not a valid PDF. Header: ${header}`);
  }

  /** Assert file size is within bounds */
  assertFileSize(filePath: string, minBytes: number, maxBytes: number): void {
    const size = fs.statSync(filePath).size;
    if (size < minBytes || size > maxBytes) {
      throw new Error(`File size ${size} bytes is outside expected range [${minBytes}, ${maxBytes}]`);
    }
  }

  /** Clean up downloaded files */
  cleanup(): void {
    if (fs.existsSync(this.downloadsDir)) {
      fs.rmSync(this.downloadsDir, { recursive: true });
    }
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { DownloadHelper } from '../utils/DownloadHelper';

test('export transactions CSV', async ({ page }, testInfo) => {
  const downloader = new DownloadHelper(page, testInfo.outputDir);
  await page.goto('/transactions');

  const { path: filePath, filename } = await downloader.captureDownload(
    () => page.getByRole('button', { name: 'Export CSV' }).click()
  );

  expect(filename).toMatch(/transactions.*\.csv/);

  downloader.assertCsvContains(filePath, {
    headers: ['Transaction ID', 'Date', 'Amount', 'Status'],
    rows: [['TXN-001', 'Completed'], ['TXN-002', 'Pending']],
  });
  downloader.assertFileSize(filePath, 100, 5 * 1024 * 1024); // 100B–5MB
});

test('download invoice PDF', async ({ page }, testInfo) => {
  const downloader = new DownloadHelper(page, testInfo.outputDir);
  await page.goto('/invoices/inv-001');

  const { path: filePath } = await downloader.captureDownload(
    () => page.getByRole('button', { name: 'Download Invoice' }).click()
  );

  downloader.assertIsPdf(filePath);
  downloader.assertFileSize(filePath, 1024, 10 * 1024 * 1024);
});
```

---

## C12: Implement a Custom Test Fixture for Database State

**Problem:** Some tests require a specific database state that takes too long to create via UI or API. A direct database seed/cleanup approach is needed.

**Difficulty:** Hard

**Task:** Build a `DatabaseFixture` (using a hypothetical DB client) that seeds test data before a test and resets it after, with worker-level isolation.

**Solution:**
```typescript
// fixtures/DatabaseFixture.ts
import { test as base, WorkerInfo } from '@playwright/test';

// Hypothetical database client interface
interface DbClient {
  query<T>(sql: string, params?: unknown[]): Promise<T[]>;
  execute(sql: string, params?: unknown[]): Promise<void>;
  close(): Promise<void>;
}

// In practice, use pg, mysql2, mongodb, etc.
async function createDbClient(workerIndex: number): Promise<DbClient> {
  // Each worker gets its own DB schema for isolation
  const schema = `test_worker_${workerIndex}`;
  // ... connect to DB
  return {
    async query<T>(sql: string, params?: unknown[]): Promise<T[]> {
      // Execute query against schema
      return [] as T[];
    },
    async execute(sql: string, params?: unknown[]): Promise<void> {},
    async close(): Promise<void> {},
  };
}

interface DatabaseFixtures {
  db: DbClient;
  seedUsers: (count: number) => Promise<Array<{ id: string; email: string }>>;
  truncateTable: (table: string) => Promise<void>;
}

export const test = base.extend<DatabaseFixtures, { dbConnection: DbClient }>({
  // Worker-scoped: one DB connection per worker (not per test)
  dbConnection: [async ({}, use, workerInfo: WorkerInfo) => {
    const db = await createDbClient(workerInfo.workerIndex);
    await use(db);
    await db.close();
  }, { scope: 'worker' }],

  // Test-scoped: access to the worker DB connection
  db: async ({ dbConnection }, use) => {
    await use(dbConnection);
  },

  // Helper: seed N users and clean up after test
  seedUsers: async ({ db }, use) => {
    const createdIds: string[] = [];

    const seeder = async (count: number) => {
      const users: Array<{ id: string; email: string }> = [];
      for (let i = 0; i < count; i++) {
        const email = `seed-user-${Date.now()}-${i}@test.com`;
        await db.execute(
          'INSERT INTO users (id, email, name, role) VALUES (uuid_generate_v4(), $1, $2, $3)',
          [email, `Seed User ${i}`, 'user']
        );
        const [created] = await db.query<{ id: string; email: string }>(
          'SELECT id, email FROM users WHERE email = $1', [email]
        );
        users.push(created);
        createdIds.push(created.id);
      }
      return users;
    };

    await use(seeder);

    // Cleanup: remove all seeded users after test
    if (createdIds.length > 0) {
      const placeholders = createdIds.map((_, i) => `$${i + 1}`).join(', ');
      await db.execute(`DELETE FROM users WHERE id IN (${placeholders})`, createdIds);
    }
  },

  truncateTable: async ({ db }, use) => {
    const truncater = async (table: string) => {
      await db.execute(`TRUNCATE TABLE ${table} CASCADE`);
    };
    await use(truncater);
  },
});
```

```typescript
// Usage
import { test, expect } from '../fixtures/DatabaseFixture';

test('user list shows all 25 seeded users', async ({ page, seedUsers }) => {
  const users = await seedUsers(25);

  await page.goto('/admin/users');
  await expect(page.getByTestId('user-count')).toHaveText('25');

  // Spot check first and last seeded user appear
  await expect(page.getByText(users[0].email)).toBeVisible();
});
```

---

## C13: Create a Reusable Pagination Traversal Utility

**Problem:** Your admin table has 500 records across 25 pages. Tests need to traverse all pages to find a specific record or collect all data.

**Difficulty:** Medium

**Solution:**
```typescript
// utils/PaginationHelper.ts
import { Page, Locator } from '@playwright/test';

interface PaginationConfig {
  nextButtonSelector: string;
  itemsSelector: string;
  pageIndicatorSelector?: string;
}

export class PaginationHelper {
  constructor(private page: Page, private config: PaginationConfig) {}

  /** Collect all items text across all pages */
  async collectAllItems(): Promise<string[]> {
    const allItems: string[] = [];

    while (true) {
      const items = await this.page.locator(this.config.itemsSelector).allInnerTexts();
      allItems.push(...items);

      const next = this.page.locator(this.config.nextButtonSelector);
      const isDisabled = await next.isDisabled().catch(() => true);
      const isHidden = !(await next.isVisible().catch(() => false));

      if (isDisabled || isHidden) break;
      await next.click();
      await this.page.waitForLoadState('networkidle');
    }

    return allItems;
  }

  /** Find the first page containing an item with the given text */
  async findItemOnPage(searchText: string): Promise<{ pageNumber: number; found: boolean }> {
    let pageNumber = 1;

    while (true) {
      const items = await this.page.locator(this.config.itemsSelector).allInnerTexts();
      if (items.some(t => t.includes(searchText))) {
        return { pageNumber, found: true };
      }

      const next = this.page.locator(this.config.nextButtonSelector);
      if (await next.isDisabled().catch(() => true)) break;

      await next.click();
      await this.page.waitForLoadState('networkidle');
      pageNumber++;
    }

    return { pageNumber: -1, found: false };
  }

  /** Jump directly to a page number */
  async goToPage(pageNum: number): Promise<void> {
    if (!this.config.pageIndicatorSelector) throw new Error('pageIndicatorSelector required');
    await this.page.locator(this.config.pageIndicatorSelector).fill(String(pageNum));
    await this.page.keyboard.press('Enter');
    await this.page.waitForLoadState('networkidle');
  }
}
```

```typescript
// Usage
test('find specific transaction across all pages', async ({ page }) => {
  await page.goto('/admin/transactions');

  const pagination = new PaginationHelper(page, {
    nextButtonSelector: '[data-testid="pagination-next"]',
    itemsSelector: '[data-testid="transaction-row"]',
    pageIndicatorSelector: '[data-testid="page-input"]',
  });

  const { found, pageNumber } = await pagination.findItemOnPage('TXN-SPECIAL-001');
  expect(found).toBe(true);
  console.log(`Found on page ${pageNumber}`);
});
```

---

## C14: Build a Custom Soft Assertion Utility

**Problem:** In a reporting page test, you want to check 15 metrics simultaneously without the test stopping at the first failure. You need a "collect all failures, report at end" approach.

**Difficulty:** Medium

**Solution:**
```typescript
// utils/SoftAssert.ts
import { expect, Locator } from '@playwright/test';

type AssertionFn = () => Promise<void> | void;

export class SoftAssert {
  private failures: Array<{ name: string; error: string }> = [];

  /** Run an assertion without throwing; collect failure instead */
  async check(name: string, assertion: AssertionFn): Promise<void> {
    try {
      await assertion();
    } catch (err) {
      this.failures.push({
        name,
        error: (err as Error).message.split('\n')[0],
      });
    }
  }

  /** Assert all collected failures at once */
  assertAll(): void {
    if (this.failures.length === 0) return;

    const report = this.failures
      .map((f, i) => `  ${i + 1}. [${f.name}] ${f.error}`)
      .join('\n');

    throw new Error(
      `${this.failures.length} soft assertion(s) failed:\n${report}`
    );
  }

  get failureCount(): number {
    return this.failures.length;
  }
}
```

```typescript
// Usage
import { test } from '@playwright/test';
import { SoftAssert } from '../utils/SoftAssert';

test('dashboard metrics are all correct', async ({ page }) => {
  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');

  const soft = new SoftAssert();

  await soft.check('Total Revenue', async () =>
    expect(page.getByTestId('total-revenue')).toHaveText('$1,234,567')
  );
  await soft.check('Active Users', async () =>
    expect(page.getByTestId('active-users')).toHaveText('4,892')
  );
  await soft.check('Pending Orders', async () =>
    expect(page.getByTestId('pending-orders')).toHaveText('23')
  );
  await soft.check('Conversion Rate', async () =>
    expect(page.getByTestId('conversion-rate')).toHaveText('3.4%')
  );
  await soft.check('Error Rate badge hidden', async () =>
    expect(page.getByTestId('error-badge')).not.toBeVisible()
  );

  // Throws once with ALL failures listed
  soft.assertAll();
});
```

---

## C15: Implement a GraphQL API Testing Utility

**Problem:** Your backend uses GraphQL. Tests need to send queries/mutations, validate response shapes, and handle GraphQL-specific errors (errors in a 200 response body).

**Difficulty:** Hard

**Solution:**
```typescript
// api/GraphQLClient.ts
import { APIRequestContext, expect } from '@playwright/test';

interface GraphQLResponse<T = unknown> {
  data?: T;
  errors?: Array<{ message: string; extensions?: { code: string } }>;
}

export class GraphQLClient {
  constructor(
    private request: APIRequestContext,
    private endpoint: string,
    private token: string
  ) {}

  async query<T>(
    query: string,
    variables?: Record<string, unknown>
  ): Promise<T> {
    const res = await this.request.post(this.endpoint, {
      data: { query, variables },
      headers: {
        Authorization: `Bearer ${this.token}`,
        'Content-Type': 'application/json',
      },
    });

    expect(res.status(), `GraphQL HTTP error on query`).toBe(200);
    const body: GraphQLResponse<T> = await res.json();

    if (body.errors?.length) {
      throw new Error(
        `GraphQL errors:\n${body.errors.map(e => `  - ${e.message} (${e.extensions?.code})`).join('\n')}`
      );
    }

    if (body.data === undefined) {
      throw new Error('GraphQL response has no data field');
    }

    return body.data;
  }

  async mutation<T>(
    mutation: string,
    variables?: Record<string, unknown>
  ): Promise<T> {
    return this.query<T>(mutation, variables);
  }

  /** Assert a query returns a specific GraphQL error code */
  async expectError(
    query: string,
    variables: Record<string, unknown>,
    expectedCode: string
  ): Promise<void> {
    const res = await this.request.post(this.endpoint, {
      data: { query, variables },
      headers: { Authorization: `Bearer ${this.token}` },
    });

    const body: GraphQLResponse = await res.json();
    expect(body.errors).toBeDefined();
    expect(body.errors!.some(e => e.extensions?.code === expectedCode)).toBe(true);
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { GraphQLClient } from '../api/GraphQLClient';

test('GraphQL: create and fetch order', async ({ request }) => {
  const gql = new GraphQLClient(request, '/api/graphql', process.env.API_TOKEN!);

  // Mutation: create order
  const { createOrder } = await gql.mutation<{ createOrder: { id: string; status: string } }>(`
    mutation CreateOrder($input: CreateOrderInput!) {
      createOrder(input: $input) {
        id
        status
        total
      }
    }
  `, {
    input: { customerId: 'cust-001', items: [{ productId: 'prod-1', quantity: 2 }] }
  });

  expect(createOrder.id).toMatch(/^ord-/);
  expect(createOrder.status).toBe('pending');

  // Query: fetch it back
  const { order } = await gql.query<{ order: { id: string; status: string } }>(`
    query GetOrder($id: ID!) {
      order(id: $id) { id status }
    }
  `, { id: createOrder.id });

  expect(order.status).toBe('pending');

  // Assert unauthorized access returns correct error code
  await gql.expectError(
    `mutation DeleteOrder($id: ID!) { deleteOrder(id: $id) }`,
    { id: createOrder.id },
    'FORBIDDEN'
  );
});
```

---

*— End of Section 2, C1–C15 —*

> **Next:** Section 2 C16–C30 covering: Custom reporter with Jira integration, Accessibility automation helper, Performance budget enforcer, API contract validation with Zod, Parallel data seeding fixture, Request/response HAR mock builder, Dynamic form filler, Screenshot diff reporter, Environment-aware config loader, Token refresh interceptor, Drag-and-drop utility, WebSocket mock server fixture, Bulk operation test helper, A/B testing variant controller, and End-to-end order lifecycle helper.

---

## C16: Build a Custom Playwright Reporter with Slack Notifications

**Problem:** Your CI pipeline runs 600 tests. The team wants a Slack message on failure with a summary of failed tests, links to trace files, and a pass/fail ratio — without polling CI dashboards.

**Difficulty:** Hard

**Task:** Implement a custom `SlackReporter` class that:
1. Implements Playwright's `Reporter` interface
2. Collects failures during the run
3. Posts a formatted summary to Slack via webhook on `onEnd`
4. Attaches trace file URLs when available

**Solution:**
```typescript
// reporters/SlackReporter.ts
import {
  Reporter, FullConfig, Suite, TestCase,
  TestResult, FullResult
} from '@playwright/test/reporter';
import * as https from 'https';
import * as url from 'url';

interface FailedTest {
  title: string;
  file: string;
  error: string;
  duration: number;
  traceUrl?: string;
}

export default class SlackReporter implements Reporter {
  private failures: FailedTest[] = [];
  private passed = 0;
  private skipped = 0;
  private startTime = 0;
  private webhookUrl: string;
  private traceBaseUrl: string;

  constructor(options: { webhookUrl?: string; traceBaseUrl?: string } = {}) {
    this.webhookUrl = options.webhookUrl ?? process.env.SLACK_WEBHOOK_URL ?? '';
    this.traceBaseUrl = options.traceBaseUrl ?? process.env.TRACE_BASE_URL ?? '';
  }

  onBegin(_config: FullConfig, _suite: Suite): void {
    this.startTime = Date.now();
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    if (result.status === 'passed' || result.status === 'timedOut' && result.retry > 0) {
      this.passed++;
      return;
    }
    if (result.status === 'skipped') { this.skipped++; return; }
    if (result.status === 'failed' || result.status === 'timedOut') {
      const attachment = result.attachments.find(a => a.name === 'trace');
      this.failures.push({
        title: test.titlePath().slice(1).join(' > '),
        file: test.location.file.replace(process.cwd(), ''),
        error: result.error?.message?.split('\n')[0] ?? 'Unknown error',
        duration: result.duration,
        traceUrl: attachment?.path
          ? `${this.traceBaseUrl}/${attachment.path.split('/').slice(-2).join('/')}`
          : undefined,
      });
    }
  }

  async onEnd(result: FullResult): Promise<void> {
    if (!this.webhookUrl) return;

    const total = this.passed + this.failures.length + this.skipped;
    const duration = ((Date.now() - this.startTime) / 1000).toFixed(1);
    const statusEmoji = result.status === 'passed' ? ':white_check_mark:' : ':x:';
    const passRate = total > 0 ? ((this.passed / total) * 100).toFixed(1) : '0';

    const failureBlocks = this.failures.slice(0, 10).map(f => ({
      type: 'section',
      text: {
        type: 'mrkdwn',
        text: [
          `*:red_circle: ${f.title}*`,
          `File: \`${f.file}\``,
          `Error: \`${f.error.slice(0, 150)}\``,
          f.traceUrl ? `<${f.traceUrl}|View Trace>` : '',
        ].filter(Boolean).join('\n'),
      },
    }));

    const payload = {
      blocks: [
        {
          type: 'header',
          text: {
            type: 'plain_text',
            text: `${statusEmoji} Playwright Test Results`,
          },
        },
        {
          type: 'section',
          fields: [
            { type: 'mrkdwn', text: `*Status:* ${result.status.toUpperCase()}` },
            { type: 'mrkdwn', text: `*Duration:* ${duration}s` },
            { type: 'mrkdwn', text: `*Passed:* ${this.passed}/${total} (${passRate}%)` },
            { type: 'mrkdwn', text: `*Failed:* ${this.failures.length}` },
            { type: 'mrkdwn', text: `*Skipped:* ${this.skipped}` },
          ],
        },
        { type: 'divider' },
        ...failureBlocks,
        this.failures.length > 10
          ? {
              type: 'section',
              text: { type: 'mrkdwn', text: `_...and ${this.failures.length - 10} more failures_` },
            }
          : null,
      ].filter(Boolean),
    };

    await this.postToSlack(JSON.stringify(payload));
  }

  private postToSlack(body: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const parsed = url.parse(this.webhookUrl);
      const req = https.request(
        { hostname: parsed.hostname, path: parsed.path, method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) } },
        (res) => {
          if (res.statusCode !== 200) reject(new Error(`Slack responded with ${res.statusCode}`));
          else resolve();
        }
      );
      req.on('error', reject);
      req.write(body);
      req.end();
    });
  }

  printsToStdio(): boolean { return false; }
}
```

```typescript
// playwright.config.ts — register the reporter
import { defineConfig } from '@playwright/test';

export default defineConfig({
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['./reporters/SlackReporter', {
      webhookUrl: process.env.SLACK_WEBHOOK_URL,
      traceBaseUrl: 'https://ci.example.com/traces',
    }],
  ],
});
```

**Alternative Approach:** Use `@playwright/test`'s built-in JSON reporter output and post-process it in a separate script after `playwright test` exits — simpler but loses the streaming per-test data.

---

## C17: Build an Accessibility Automation Helper with axe-core

**Problem:** Your QA charter requires WCAG 2.1 AA compliance checks on every page. Tests must fail on violations, categorize by impact level, and suppress known accepted violations.

**Difficulty:** Medium

**Task:** Build an `AccessibilityHelper` that:
1. Runs axe-core analysis on specific regions or full page
2. Filters by impact level (critical, serious, moderate, minor)
3. Maintains a known-violations exclusion list
4. Generates readable failure messages

**Solution:**
```typescript
// utils/AccessibilityHelper.ts
import { Page, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

type ImpactLevel = 'critical' | 'serious' | 'moderate' | 'minor';

interface AccessibilityOptions {
  include?: string[];           // CSS selectors to include
  exclude?: string[];           // CSS selectors to exclude
  minImpact?: ImpactLevel;      // Minimum impact to fail on
  tags?: string[];              // WCAG tags: 'wcag2a', 'wcag2aa', 'wcag21aa'
  disableRules?: string[];      // axe rule IDs to suppress
}

const IMPACT_ORDER: ImpactLevel[] = ['minor', 'moderate', 'serious', 'critical'];

export class AccessibilityHelper {
  constructor(private page: Page) {}

  async analyze(options: AccessibilityOptions = {}): Promise<void> {
    const {
      include,
      exclude = [],
      minImpact = 'serious',
      tags = ['wcag2a', 'wcag2aa', 'wcag21aa'],
      disableRules = [],
    } = options;

    let builder = new AxeBuilder({ page: this.page })
      .withTags(tags)
      .disableRules(disableRules);

    if (include?.length) builder = builder.include(include);
    if (exclude?.length) builder = builder.exclude(exclude);

    const { violations } = await builder.analyze();

    const minIdx = IMPACT_ORDER.indexOf(minImpact);
    const relevant = violations.filter(v =>
      IMPACT_ORDER.indexOf(v.impact as ImpactLevel) >= minIdx
    );

    if (relevant.length === 0) return;

    const report = relevant.map(v => [
      `[${v.impact?.toUpperCase()}] ${v.id}: ${v.description}`,
      `  Help: ${v.helpUrl}`,
      ...v.nodes.slice(0, 3).map(n =>
        `  Node: ${n.html.slice(0, 120)}\n  Fix: ${n.failureSummary?.split('\n')[0]}`
      ),
    ].join('\n')).join('\n\n');

    throw new Error(
      `${relevant.length} accessibility violation(s) found:\n\n${report}`
    );
  }

  /** Check a specific component region */
  async analyzeRegion(selector: string, options?: AccessibilityOptions): Promise<void> {
    return this.analyze({ ...options, include: [selector] });
  }

  /** Assert zero violations and attach results to test report */
  async assertNoViolations(options?: AccessibilityOptions): Promise<void> {
    return this.analyze(options);
  }
}
```

```typescript
// Usage
import { test } from '@playwright/test';
import { AccessibilityHelper } from '../utils/AccessibilityHelper';

test.describe('Accessibility: Dashboard', () => {
  test('no critical/serious violations on load', async ({ page }) => {
    await page.goto('/dashboard');
    const a11y = new AccessibilityHelper(page);

    await a11y.assertNoViolations({
      minImpact: 'serious',
      tags: ['wcag21aa'],
      disableRules: ['color-contrast'], // Accepted deviation — using brand palette
    });
  });

  test('data table region is accessible', async ({ page }) => {
    await page.goto('/reports');
    const a11y = new AccessibilityHelper(page);

    await a11y.analyzeRegion('[data-testid="reports-table"]', {
      minImpact: 'moderate',
    });
  });
});
```

---

## C18: Implement a Performance Budget Enforcer

**Problem:** Your SLA requires the dashboard page to load under 3 seconds on a 4G connection. You need automated performance gating in the test suite.

**Difficulty:** Hard

**Task:** Build a `PerformanceBudget` utility that:
1. Captures Core Web Vitals (LCP, FCP, CLS, TBT)
2. Measures time-to-interactive under network throttling
3. Asserts values against defined budgets
4. Outputs metrics as test attachments

**Solution:**
```typescript
// utils/PerformanceBudget.ts
import { Page, TestInfo } from '@playwright/test';

interface WebVitals {
  fcp: number;   // First Contentful Paint (ms)
  lcp: number;   // Largest Contentful Paint (ms)
  cls: number;   // Cumulative Layout Shift (score)
  tbt: number;   // Total Blocking Time (ms)
  ttfb: number;  // Time to First Byte (ms)
  domInteractive: number;
  domComplete: number;
}

interface PerformanceBudgets {
  fcp?: number;
  lcp?: number;
  cls?: number;
  tbt?: number;
  ttfb?: number;
}

export class PerformanceBudget {
  constructor(private page: Page, private testInfo?: TestInfo) {}

  /** Apply CDP network throttling (simulates 4G) */
  async throttleNetwork(preset: 'Fast3G' | 'Slow3G' | '4G' = '4G'): Promise<void> {
    const presets = {
      '4G':     { downloadThroughput: 4 * 1024 * 1024 / 8, uploadThroughput: 3 * 1024 * 1024 / 8, latency: 20 },
      'Fast3G': { downloadThroughput: 1.5 * 1024 * 1024 / 8, uploadThroughput: 750 * 1024 / 8, latency: 40 },
      'Slow3G': { downloadThroughput: 500 * 1024 / 8, uploadThroughput: 500 * 1024 / 8, latency: 400 },
    };

    const cdp = await this.page.context().newCDPSession(this.page);
    await cdp.send('Network.emulateNetworkConditions', {
      offline: false,
      ...presets[preset],
    });
  }

  /** Collect Core Web Vitals via Performance Observer */
  async collectVitals(): Promise<WebVitals> {
    const vitals = await this.page.evaluate((): Promise<WebVitals> => {
      return new Promise((resolve) => {
        const result: Partial<WebVitals> = {};

        // Navigation timing
        const nav = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        if (nav) {
          result.ttfb = nav.responseStart - nav.requestStart;
          result.domInteractive = nav.domInteractive;
          result.domComplete = nav.domComplete;
        }

        let lcp = 0;
        let cls = 0;
        let tbt = 0;

        new PerformanceObserver(list => {
          for (const entry of list.getEntries()) {
            lcp = entry.startTime;
          }
        }).observe({ type: 'largest-contentful-paint', buffered: true });

        new PerformanceObserver(list => {
          for (const entry of list.getEntries()) {
            cls += (entry as any).value;
          }
        }).observe({ type: 'layout-shift', buffered: true });

        new PerformanceObserver(list => {
          for (const entry of list.getEntries()) {
            const blockingTime = entry.duration - 50;
            if (blockingTime > 0) tbt += blockingTime;
          }
        }).observe({ type: 'longtask', buffered: true });

        const fcp = performance.getEntriesByName('first-contentful-paint')[0]?.startTime ?? 0;

        setTimeout(() => {
          resolve({ fcp, lcp, cls, tbt, ttfb: result.ttfb ?? 0,
            domInteractive: result.domInteractive ?? 0, domComplete: result.domComplete ?? 0 });
        }, 3000);
      });
    });

    if (this.testInfo) {
      await this.testInfo.attach('performance-metrics', {
        body: JSON.stringify(vitals, null, 2),
        contentType: 'application/json',
      });
    }

    return vitals;
  }

  /** Assert vitals meet the defined budget */
  async assertBudget(vitals: WebVitals, budgets: PerformanceBudgets): Promise<void> {
    const violations: string[] = [];

    const check = (metric: keyof PerformanceBudgets, actual: number, unit: string) => {
      const budget = budgets[metric];
      if (budget !== undefined && actual > budget) {
        violations.push(`${metric.toUpperCase()}: ${actual.toFixed(2)}${unit} > budget ${budget}${unit}`);
      }
    };

    check('fcp', vitals.fcp, 'ms');
    check('lcp', vitals.lcp, 'ms');
    check('cls', vitals.cls, '');
    check('tbt', vitals.tbt, 'ms');
    check('ttfb', vitals.ttfb, 'ms');

    if (violations.length > 0) {
      throw new Error(`Performance budget exceeded:\n${violations.map(v => `  - ${v}`).join('\n')}`);
    }
  }
}
```

```typescript
// Usage
import { test } from '@playwright/test';
import { PerformanceBudget } from '../utils/PerformanceBudget';

test('dashboard meets performance budget on 4G', async ({ page }, testInfo) => {
  const perf = new PerformanceBudget(page, testInfo);

  await perf.throttleNetwork('4G');
  await page.goto('/dashboard');

  const vitals = await perf.collectVitals();

  await perf.assertBudget(vitals, {
    fcp:  1800,  // ms
    lcp:  2500,  // ms
    cls:  0.1,   // score
    tbt:  300,   // ms
    ttfb: 600,   // ms
  });
});
```

---

## C19: API Contract Validation with Zod Schema

**Problem:** Your frontend has 30 API integrations. A backend team change broke 3 response shapes without updating docs. You need automated contract tests that catch schema drift.

**Difficulty:** Medium

**Task:** Build a contract testing utility using Zod that validates API responses match expected schemas, with clear field-level error messages.

**Solution:**
```typescript
// api/ContractValidator.ts
import { APIRequestContext } from '@playwright/test';
import { z, ZodSchema, ZodError } from 'zod';

interface ContractTestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  headers?: Record<string, string>;
  expectedStatus?: number;
}

export class ContractValidator {
  constructor(
    private request: APIRequestContext,
    private baseUrl: string,
    private token: string
  ) {}

  async validate<T>(
    path: string,
    schema: ZodSchema<T>,
    options: ContractTestOptions = {}
  ): Promise<T> {
    const { method = 'GET', body, headers, expectedStatus = 200 } = options;
    const url = `${this.baseUrl}${path}`;
    const authHeaders = { Authorization: `Bearer ${this.token}`, ...headers };

    let res;
    switch (method) {
      case 'POST':   res = await this.request.post(url, { data: body, headers: authHeaders }); break;
      case 'PUT':    res = await this.request.put(url, { data: body, headers: authHeaders }); break;
      case 'PATCH':  res = await this.request.patch(url, { data: body, headers: authHeaders }); break;
      case 'DELETE': res = await this.request.delete(url, { headers: authHeaders }); break;
      default:       res = await this.request.get(url, { headers: authHeaders });
    }

    if (res.status() !== expectedStatus) {
      throw new Error(`${method} ${path}: expected status ${expectedStatus}, got ${res.status()}\n${await res.text()}`);
    }

    let data: unknown;
    try {
      data = await res.json();
    } catch {
      throw new Error(`${method} ${path}: response is not valid JSON`);
    }

    try {
      return schema.parse(data);
    } catch (err) {
      if (err instanceof ZodError) {
        const issues = err.issues.map(i =>
          `  - [${i.path.join('.')}] ${i.message} (expected: ${i.code})`
        ).join('\n');
        throw new Error(`Contract violation for ${method} ${path}:\n${issues}`);
      }
      throw err;
    }
  }
}

// --- Domain Schemas ---
// schemas/api.ts
export const UserSchema = z.object({
  id:        z.string().regex(/^usr-[a-z0-9]+/),
  email:     z.string().email(),
  name:      z.string().min(1),
  role:      z.enum(['admin', 'user', 'readonly']),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
});

export const PaginatedUsersSchema = z.object({
  data:  z.array(UserSchema),
  total: z.number().int().nonnegative(),
  page:  z.number().int().positive(),
  limit: z.number().int().positive(),
});

export const OrderSchema = z.object({
  id:       z.string().regex(/^ord-/),
  status:   z.enum(['pending', 'confirmed', 'shipped', 'delivered', 'cancelled']),
  total:    z.number().positive(),
  currency: z.literal('USD'),
  items:    z.array(z.object({
    productId: z.string(),
    quantity:  z.number().int().positive(),
    price:     z.number().positive(),
  })).min(1),
});
```

```typescript
// Usage
import { test } from '@playwright/test';
import { ContractValidator } from '../api/ContractValidator';
import { PaginatedUsersSchema, OrderSchema } from '../schemas/api';

test.describe('API Contract Tests', () => {
  test('GET /users matches schema', async ({ request }) => {
    const validator = new ContractValidator(
      request, process.env.BASE_URL!, process.env.API_TOKEN!
    );

    await validator.validate('/api/users', PaginatedUsersSchema, {
      method: 'GET',
    });
  });

  test('POST /orders returns valid order schema', async ({ request }) => {
    const validator = new ContractValidator(
      request, process.env.BASE_URL!, process.env.API_TOKEN!
    );

    await validator.validate('/api/orders', OrderSchema, {
      method: 'POST',
      expectedStatus: 201,
      body: {
        customerId: 'cust-001',
        items: [{ productId: 'prod-1', quantity: 1 }],
      },
    });
  });
});
```

---

## C20: Build a Token Refresh Interceptor

**Problem:** Your JWT tokens expire after 15 minutes. Long-running tests (or tests that start after a delay) fail with 401s mid-test. You need transparent token refresh.

**Difficulty:** Hard

**Task:** Intercept all outgoing requests, detect 401 responses, refresh the token via API, and retry the original request automatically.

**Solution:**
```typescript
// utils/TokenRefreshInterceptor.ts
import { BrowserContext, Route, Request } from '@playwright/test';

interface TokenStore {
  accessToken: string;
  refreshToken: string;
}

export class TokenRefreshInterceptor {
  private isRefreshing = false;
  private refreshQueue: Array<() => void> = [];

  constructor(
    private context: BrowserContext,
    private tokenStore: TokenStore,
    private apiBase: string,
    private refreshEndpoint = '/api/auth/refresh'
  ) {}

  async install(): Promise<void> {
    await this.context.route('**/api/**', async (route: Route) => {
      await this.handleRoute(route);
    });
  }

  private async handleRoute(route: Route): Promise<void> {
    const request = route.request();

    // Skip auth endpoints to avoid infinite loops
    if (request.url().includes(this.refreshEndpoint) ||
        request.url().includes('/api/auth/login')) {
      await route.continue();
      return;
    }

    // Inject current access token
    await route.continue({
      headers: {
        ...await request.allHeaders(),
        Authorization: `Bearer ${this.tokenStore.accessToken}`,
      },
    });
  }

  /**
   * Use this wrapper around actions that may trigger 401s.
   * Intercepts 401 responses at the page level and refreshes.
   */
  async withAutoRefresh(action: () => Promise<void>): Promise<void> {
    // Monitor responses for 401
    const responseHandler = async (response: { status(): number; url(): string }) => {
      if (response.status() !== 401) return;
      if (response.url().includes(this.refreshEndpoint)) return;
      await this.doRefresh();
    };

    this.context.on('response' as any, responseHandler);
    try {
      await action();
    } finally {
      this.context.off('response' as any, responseHandler);
    }
  }

  private async doRefresh(): Promise<void> {
    if (this.isRefreshing) {
      // Queue callers while a refresh is in-flight
      return new Promise(resolve => this.refreshQueue.push(resolve));
    }

    this.isRefreshing = true;
    try {
      const apiContext = await this.context.request;
      const res = await apiContext.post(`${this.apiBase}${this.refreshEndpoint}`, {
        data: { refreshToken: this.tokenStore.refreshToken },
      });

      if (!res.ok()) throw new Error(`Token refresh failed: ${res.status()}`);

      const { accessToken, refreshToken } = await res.json();
      this.tokenStore.accessToken = accessToken;
      this.tokenStore.refreshToken = refreshToken;

      // Flush queued callers
      this.refreshQueue.forEach(r => r());
      this.refreshQueue = [];
    } finally {
      this.isRefreshing = false;
    }
  }
}
```

```typescript
// Usage in fixture
import { test as base, BrowserContext } from '@playwright/test';
import { TokenRefreshInterceptor } from '../utils/TokenRefreshInterceptor';

export const test = base.extend<{ authContext: BrowserContext }>({
  authContext: async ({ browser }, use) => {
    const tokenStore = {
      accessToken:  process.env.ACCESS_TOKEN!,
      refreshToken: process.env.REFRESH_TOKEN!,
    };
    const context = await browser.newContext();
    const interceptor = new TokenRefreshInterceptor(
      context, tokenStore, process.env.BASE_URL!
    );
    await interceptor.install();
    await use(context);
    await context.close();
  },
});
```

---

## C21: Implement a Drag-and-Drop Utility

**Problem:** Your Kanban board uses `@dnd-kit` which doesn't support native HTML5 drag events. Standard `page.dragAndDrop()` doesn't work; it requires mouse event simulation.

**Difficulty:** Hard

**Task:** Build a `DragHelper` that supports both HTML5 drag APIs and pointer-event–based drag (for custom DnD libraries).

**Solution:**
```typescript
// utils/DragHelper.ts
import { Page, Locator } from '@playwright/test';

export class DragHelper {
  constructor(private page: Page) {}

  /** Standard HTML5 drag-and-drop via Playwright's built-in */
  async dragNative(source: Locator, target: Locator): Promise<void> {
    await source.dragTo(target);
  }

  /**
   * Pointer-event–based drag for custom DnD libraries (dnd-kit, react-beautiful-dnd etc.)
   * Simulates mousedown → mousemove (incremental) → mouseup
   */
  async dragPointer(source: Locator, target: Locator): Promise<void> {
    const sourceBBox = await source.boundingBox();
    const targetBBox = await target.boundingBox();

    if (!sourceBBox || !targetBBox) {
      throw new Error('Could not get bounding box for drag source or target');
    }

    const startX = sourceBBox.x + sourceBBox.width / 2;
    const startY = sourceBBox.y + sourceBBox.height / 2;
    const endX   = targetBBox.x + targetBBox.width / 2;
    const endY   = targetBBox.y + targetBBox.height / 2;

    await this.page.mouse.move(startX, startY);
    await this.page.mouse.down();

    // Incremental move to trigger drag events in sequence
    const steps = 20;
    for (let i = 1; i <= steps; i++) {
      const x = startX + (endX - startX) * (i / steps);
      const y = startY + (endY - startY) * (i / steps);
      await this.page.mouse.move(x, y, { steps: 1 });
      await this.page.waitForTimeout(10); // Small delay for DnD lib to react
    }

    await this.page.mouse.up();
  }

  /**
   * Drag by keyboard (for accessible drag implementations).
   * Space to pick up, arrow keys to move, Space/Enter to drop.
   */
  async dragByKeyboard(
    item: Locator,
    direction: 'up' | 'down' | 'left' | 'right',
    steps = 1
  ): Promise<void> {
    await item.focus();
    await this.page.keyboard.press('Space'); // Lift

    const key = {
      up: 'ArrowUp', down: 'ArrowDown',
      left: 'ArrowLeft', right: 'ArrowRight',
    }[direction];

    for (let i = 0; i < steps; i++) {
      await this.page.keyboard.press(key);
      await this.page.waitForTimeout(50);
    }

    await this.page.keyboard.press('Space'); // Drop
  }

  /** Reorder a list item by dragging it to a target index */
  async reorderListItem(
    listSelector: string,
    fromIndex: number,
    toIndex: number
  ): Promise<void> {
    const items = this.page.locator(`${listSelector} [draggable="true"]`);
    const source = items.nth(fromIndex);
    const target = items.nth(toIndex);
    await this.dragPointer(source, target);
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { DragHelper } from '../utils/DragHelper';

test('kanban: move card from Todo to In Progress', async ({ page }) => {
  await page.goto('/board');
  const drag = new DragHelper(page);

  const todoCard = page
    .getByTestId('column-todo')
    .getByText('Design Login Page');

  const inProgressColumn = page.getByTestId('column-in-progress');

  await drag.dragPointer(todoCard, inProgressColumn);

  await expect(
    page.getByTestId('column-in-progress').getByText('Design Login Page')
  ).toBeVisible();
  await expect(
    page.getByTestId('column-todo').getByText('Design Login Page')
  ).not.toBeVisible();
});
```

---

## C22: Build a WebSocket Mock Server Fixture

**Problem:** Your real-time dashboard subscribes to a WebSocket for live price ticks. Tests must be deterministic, so they need a controllable mock WS server.

**Difficulty:** Hard

**Task:** Build a test fixture that starts a lightweight WebSocket server, allows tests to push messages on demand, and verifies what clients send.

**Solution:**
```typescript
// fixtures/WebSocketServer.ts
import { test as base } from '@playwright/test';
import { WebSocketServer as WSSLib, WebSocket } from 'ws';
import * as http from 'http';
import * as net from 'net';

interface WsMessage {
  type: string;
  data: unknown;
}

export interface MockWebSocketServer {
  url: string;
  broadcast: (message: WsMessage) => void;
  sendToAll: (raw: string) => void;
  receivedMessages: () => WsMessage[];
  waitForMessage: (predicate: (msg: WsMessage) => boolean, timeoutMs?: number) => Promise<WsMessage>;
  close: () => Promise<void>;
}

async function getAvailablePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.listen(0, () => {
      const port = (server.address() as net.AddressInfo).port;
      server.close(() => resolve(port));
    });
    server.on('error', reject);
  });
}

async function createMockWsServer(): Promise<MockWebSocketServer> {
  const port = await getAvailablePort();
  const server = http.createServer();
  const wss = new WSSLib({ server });
  const clients = new Set<WebSocket>();
  const received: WsMessage[] = [];

  wss.on('connection', (ws: WebSocket) => {
    clients.add(ws);
    ws.on('message', (data: Buffer) => {
      try {
        received.push(JSON.parse(data.toString()));
      } catch {}
    });
    ws.on('close', () => clients.delete(ws));
  });

  await new Promise<void>(resolve => server.listen(port, resolve));

  return {
    url: `ws://localhost:${port}`,
    broadcast(message: WsMessage) {
      const payload = JSON.stringify(message);
      clients.forEach(ws => {
        if (ws.readyState === WebSocket.OPEN) ws.send(payload);
      });
    },
    sendToAll(raw: string) {
      clients.forEach(ws => { if (ws.readyState === WebSocket.OPEN) ws.send(raw); });
    },
    receivedMessages: () => [...received],
    waitForMessage(predicate, timeoutMs = 5000) {
      return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('WS message timeout')), timeoutMs);
        const check = setInterval(() => {
          const match = received.find(predicate);
          if (match) { clearInterval(check); clearTimeout(timeout); resolve(match); }
        }, 50);
      });
    },
    close() {
      return new Promise<void>(resolve => {
        wss.close(() => server.close(() => resolve()));
      });
    },
  };
}

type WsFixtures = { wsServer: MockWebSocketServer };

export const test = base.extend<WsFixtures>({
  wsServer: async ({}, use) => {
    const server = await createMockWsServer();
    await use(server);
    await server.close();
  },
});
```

```typescript
// Usage
import { test, expect } from '../fixtures/WebSocketServer';

test('dashboard updates on live price tick', async ({ page, wsServer }) => {
  // Route the app's WS connection to our mock server
  await page.addInitScript((wsUrl) => {
    (window as any).__MOCK_WS_URL__ = wsUrl;
  }, wsServer.url);

  await page.goto('/dashboard');

  // Push a price update
  wsServer.broadcast({ type: 'price_update', data: { symbol: 'AAPL', price: 189.45 } });

  await expect(
    page.getByTestId('price-AAPL')
  ).toHaveText('$189.45', { timeout: 3000 });

  // Verify the client sent a subscription message
  const subMsg = await wsServer.waitForMessage(
    m => m.type === 'subscribe' && (m.data as any).symbol === 'AAPL'
  );
  expect(subMsg).toBeDefined();
});
```

---

## C23: Build a Dynamic Form Filler Utility

**Problem:** Your app has 15 different form types (registration, payment, onboarding, profile). Maintaining separate fill logic for each creates substantial duplication.

**Difficulty:** Medium

**Task:** Build a generic `FormFiller` that fills any form from a data map keyed by label text or `data-testid`, handles selects, checkboxes, radio buttons, and date pickers.

**Solution:**
```typescript
// utils/FormFiller.ts
import { Page, Locator } from '@playwright/test';

type FieldValue = string | boolean | string[];

interface FormData {
  [labelOrTestId: string]: FieldValue;
}

export class FormFiller {
  constructor(private page: Page, private scope?: Locator) {

  }

  private get root(): Page | Locator {
    return this.scope ?? this.page;
  }

  async fill(data: FormData): Promise<void> {
    for (const [key, value] of Object.entries(data)) {
      await this.fillField(key, value);
    }
  }

  private async fillField(key: string, value: FieldValue): Promise<void> {
    // Try data-testid first, then aria label, then placeholder
    const locator = await this.resolveField(key);
    const tagName = await locator.evaluate(el => el.tagName.toLowerCase());
    const type = await locator.getAttribute('type') ?? '';
    const role = await locator.getAttribute('role') ?? '';

    if (tagName === 'select') {
      await locator.selectOption(value as string);
    } else if (type === 'checkbox' || role === 'checkbox') {
      const checked = await locator.isChecked();
      if (checked !== (value as boolean)) await locator.click();
    } else if (type === 'radio') {
      if (value === true || value === 'true') await locator.check();
    } else if (role === 'combobox' || role === 'listbox') {
      await locator.click();
      await this.page.getByRole('option', { name: value as string }).click();
    } else if (Array.isArray(value)) {
      // Multi-select or multi-checkbox
      for (const v of value) {
        await this.page.getByLabel(v).check();
      }
    } else {
      await locator.clear();
      await locator.fill(value as string);
    }
  }

  private async resolveField(key: string): Promise<Locator> {
    // Priority: data-testid → label → placeholder → name
    const byTestId = this.root.locator(`[data-testid="${key}"]`);
    if (await byTestId.count() > 0) return byTestId.first();

    const byLabel = this.root.getByLabel(key);
    if (await byLabel.count() > 0) return byLabel.first();

    const byPlaceholder = this.root.getByPlaceholder(key);
    if (await byPlaceholder.count() > 0) return byPlaceholder.first();

    const byName = this.root.locator(`[name="${key}"]`);
    if (await byName.count() > 0) return byName.first();

    throw new Error(`FormFiller: could not resolve field "${key}"`);
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { FormFiller } from '../utils/FormFiller';

test('complete onboarding form', async ({ page }) => {
  await page.goto('/onboarding');
  const filler = new FormFiller(page);

  await filler.fill({
    'First Name':    'Alice',
    'Last Name':     'Chen',
    'Email':         'alice@example.com',
    'Country':       'United States',          // <select>
    'Date of Birth': '1990-05-15',
    'newsletter':    true,                     // checkbox by testid
    'Role':          'Engineering Manager',    // combobox
    'Skills':        ['TypeScript', 'Python'], // multi-checkbox
  });

  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page.getByText('Welcome, Alice!')).toBeVisible();
});
```

---

## C24: Implement an Environment-Aware Configuration Loader

**Problem:** Your test suite must run against dev, staging, and production with different base URLs, credentials, feature flags, and timeouts. Hardcoding environment checks throughout tests creates unmaintainable code.

**Difficulty:** Medium

**Solution:**
```typescript
// config/EnvironmentConfig.ts
import * as path from 'path';
import * as fs from 'fs';

type Environment = 'dev' | 'staging' | 'production';

interface FeatureFlags {
  newCheckoutFlow: boolean;
  betaDashboard: boolean;
  mfaRequired: boolean;
}

interface EnvironmentConfig {
  env: Environment;
  baseUrl: string;
  apiUrl: string;
  timeout: number;
  retries: number;
  credentials: {
    adminEmail: string;
    adminPassword: string;
  };
  featureFlags: FeatureFlags;
  services: {
    stripePublicKey: string;
    analyticsEndpoint: string;
  };
}

type DeepPartial<T> = { [K in keyof T]?: T[K] extends object ? DeepPartial<T[K]> : T[K] };

const DEFAULTS: EnvironmentConfig = {
  env:     'dev',
  baseUrl: 'http://localhost:3000',
  apiUrl:  'http://localhost:3000/api',
  timeout: 30_000,
  retries: 2,
  credentials: {
    adminEmail:    'admin@dev.example.com',
    adminPassword: 'dev-password-123',
  },
  featureFlags: {
    newCheckoutFlow: false,
    betaDashboard:   false,
    mfaRequired:     false,
  },
  services: {
    stripePublicKey:    'pk_test_xxx',
    analyticsEndpoint:  'http://localhost:4000/analytics',
  },
};

const ENV_OVERRIDES: Record<Environment, DeepPartial<EnvironmentConfig>> = {
  dev: {},
  staging: {
    baseUrl: 'https://staging.example.com',
    apiUrl:  'https://staging.example.com/api',
    timeout: 45_000,
    retries: 1,
    featureFlags: { newCheckoutFlow: true },
    services: { stripePublicKey: 'pk_test_staging_xxx' },
  },
  production: {
    baseUrl: 'https://app.example.com',
    apiUrl:  'https://app.example.com/api',
    timeout: 60_000,
    retries: 0,
    featureFlags: { newCheckoutFlow: true, betaDashboard: true, mfaRequired: true },
    services: { stripePublicKey: 'pk_live_xxx' },
  },
};

function deepMerge<T>(base: T, override: DeepPartial<T>): T {
  const result = { ...base };
  for (const key of Object.keys(override) as Array<keyof T>) {
    const val = override[key as keyof DeepPartial<T>];
    if (val && typeof val === 'object' && !Array.isArray(val)) {
      result[key] = deepMerge(base[key] as object, val as object) as T[keyof T];
    } else if (val !== undefined) {
      result[key] = val as T[keyof T];
    }
  }
  return result;
}

function resolveEnv(): Environment {
  const raw = (process.env.TEST_ENV ?? 'dev').toLowerCase();
  if (['dev', 'staging', 'production'].includes(raw)) return raw as Environment;
  throw new Error(`Unknown TEST_ENV "${raw}". Valid: dev, staging, production`);
}

let _config: EnvironmentConfig | null = null;

export function getConfig(): EnvironmentConfig {
  if (_config) return _config;

  const env = resolveEnv();
  const merged = deepMerge(DEFAULTS, ENV_OVERRIDES[env]);

  // Allow per-field env var overrides for CI flexibility
  merged.env = env;
  if (process.env.BASE_URL) merged.baseUrl = process.env.BASE_URL;
  if (process.env.API_URL)  merged.apiUrl  = process.env.API_URL;
  if (process.env.ADMIN_EMAIL)    merged.credentials.adminEmail    = process.env.ADMIN_EMAIL;
  if (process.env.ADMIN_PASSWORD) merged.credentials.adminPassword = process.env.ADMIN_PASSWORD;

  _config = merged;
  return _config;
}
```

```typescript
// Usage in playwright.config.ts
import { defineConfig } from '@playwright/test';
import { getConfig } from './config/EnvironmentConfig';

const cfg = getConfig();

export default defineConfig({
  use: {
    baseURL:  cfg.baseUrl,
    timeout:  cfg.timeout,
  },
  retries: cfg.retries,
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
  ],
});
```

```typescript
// Usage in tests
import { test, expect } from '@playwright/test';
import { getConfig } from '../config/EnvironmentConfig';

test('MFA prompt shown on production', async ({ page }) => {
  const cfg = getConfig();

  await page.goto('/login');
  await page.getByLabel('Email').fill(cfg.credentials.adminEmail);
  await page.getByLabel('Password').fill(cfg.credentials.adminPassword);
  await page.getByRole('button', { name: 'Sign in' }).click();

  if (cfg.featureFlags.mfaRequired) {
    await expect(page.getByTestId('mfa-prompt')).toBeVisible();
  } else {
    await expect(page).toHaveURL('/dashboard');
  }
});
```

---

## C25: End-to-End Order Lifecycle Test Helper

**Problem:** Your most critical E2E test verifies the full order lifecycle: browse → cart → checkout → payment → confirmation → email receipt → admin fulfillment. This test is 120 lines and hard to maintain.

**Difficulty:** Hard

**Task:** Refactor using a step-builder pattern that makes the test read like a specification while keeping each step independently reusable and debuggable.

**Solution:**
```typescript
// flows/OrderLifecycleFlow.ts
import { Page, APIRequestContext, expect } from '@playwright/test';

interface Product { id: string; name: string; price: number; }
interface OrderResult { orderId: string; confirmationNumber: string; total: number; }

export class OrderLifecycleFlow {
  private orderId: string = '';
  private cartTotal = 0;

  constructor(
    private customerPage: Page,
    private adminPage: Page,
    private request: APIRequestContext
  ) {}

  async browseAndAddToCart(products: Product[]): Promise<this> {
    await this.customerPage.goto('/products');

    for (const product of products) {
      await this.customerPage.getByRole('link', { name: product.name }).click();
      await this.customerPage.getByRole('button', { name: 'Add to Cart' }).click();
      await this.customerPage.getByRole('button', { name: 'Continue Shopping' }).click();
      this.cartTotal += product.price;
    }

    await expect(
      this.customerPage.getByTestId('cart-count')
    ).toHaveText(String(products.length));

    return this;
  }

  async proceedToCheckout(shippingDetails: {
    name: string; address: string; city: string; zip: string;
  }): Promise<this> {
    await this.customerPage.getByRole('link', { name: 'Cart' }).click();
    await this.customerPage.getByRole('button', { name: 'Proceed to Checkout' }).click();

    await this.customerPage.getByLabel('Full Name').fill(shippingDetails.name);
    await this.customerPage.getByLabel('Address').fill(shippingDetails.address);
    await this.customerPage.getByLabel('City').fill(shippingDetails.city);
    await this.customerPage.getByLabel('ZIP Code').fill(shippingDetails.zip);
    await this.customerPage.getByRole('button', { name: 'Continue to Payment' }).click();

    return this;
  }

  async completePayment(card: { number: string; expiry: string; cvc: string }): Promise<this> {
    const stripeFrame = this.customerPage.frameLocator('iframe[src*="stripe.com"]');
    await stripeFrame.getByLabel('Card number').fill(card.number);
    await stripeFrame.getByLabel('Expiration').fill(card.expiry);
    await stripeFrame.getByLabel('CVC').fill(card.cvc);
    await this.customerPage.getByRole('button', { name: /Place Order/ }).click();

    return this;
  }

  async verifyConfirmation(): Promise<OrderResult> {
    await expect(this.customerPage).toHaveURL(/\/orders\/ord-/, { timeout: 20_000 });

    const orderId = await this.customerPage.getByTestId('order-id').innerText();
    const confirmationNumber = await this.customerPage.getByTestId('confirmation-number').innerText();
    const totalText = await this.customerPage.getByTestId('order-total').innerText();
    const total = parseFloat(totalText.replace(/[^0-9.]/g, ''));

    expect(total).toBeCloseTo(this.cartTotal * 1.1, 1); // with tax
    this.orderId = orderId;

    return { orderId, confirmationNumber, total };
  }

  async verifyEmailReceipt(emailAddress: string): Promise<this> {
    // Poll email test API (e.g., Mailhog, Mailpit)
    const maxAttempts = 10;
    for (let i = 0; i < maxAttempts; i++) {
      const res = await this.request.get(
        `/api/test/emails?to=${emailAddress}&subject=${encodeURIComponent('Order Confirmation')}`
      );
      const { emails } = await res.json();
      if (emails.length > 0) {
        expect(emails[0].body).toContain(this.orderId);
        return this;
      }
      await new Promise(r => setTimeout(r, 2000));
    }
    throw new Error('Order confirmation email not received within 20s');
  }

  async adminFulfillsOrder(): Promise<this> {
    await this.adminPage.goto(`/admin/orders/${this.orderId}`);
    await expect(this.adminPage.getByTestId('order-status')).toHaveText('Pending');

    await this.adminPage.getByRole('button', { name: 'Mark as Shipped' }).click();
    await this.adminPage.getByLabel('Tracking Number').fill('1Z999AA10123456784');
    await this.adminPage.getByRole('button', { name: 'Confirm Shipment' }).click();

    await expect(this.adminPage.getByTestId('order-status')).toHaveText('Shipped', { timeout: 5000 });
    return this;
  }

  async customerSeesShipmentUpdate(): Promise<this> {
    await this.customerPage.goto(`/orders/${this.orderId}`);
    await expect(this.customerPage.getByTestId('order-status')).toHaveText('Shipped');
    await expect(this.customerPage.getByTestId('tracking-number')).toHaveText('1Z999AA10123456784');
    return this;
  }
}
```

```typescript
// Usage — reads like a specification
import { test } from '@playwright/test';
import { AuthStateManager } from '../utils/AuthStateManager';
import { OrderLifecycleFlow } from '../flows/OrderLifecycleFlow';

test('full order lifecycle: customer to admin', async ({ browser, request }) => {
  const auth = new AuthStateManager(browser);
  try {
    await auth.createSessions(['user', 'admin']);

    const flow = new OrderLifecycleFlow(
      auth.getPage('user'),
      auth.getPage('admin'),
      request
    );

    const { orderId } = await flow
      .browseAndAddToCart([
        { id: 'prod-1', name: 'MacBook Pro 14"', price: 1999 },
        { id: 'prod-2', name: 'USB-C Hub', price: 49 },
      ])
      .then(f => f.proceedToCheckout({
        name: 'Alice Chen', address: '123 Market St',
        city: 'San Francisco', zip: '94102',
      }))
      .then(f => f.completePayment({ number: '4242424242424242', expiry: '12/30', cvc: '123' }))
      .then(f => f.verifyConfirmation());

    await flow
      .verifyEmailReceipt('alice@example.com')
      .then(f => f.adminFulfillsOrder())
      .then(f => f.customerSeesShipmentUpdate());

    console.log(`✓ Order ${orderId} lifecycle complete`);
  } finally {
    await auth.closeAll();
  }
});
```

**Alternative Approach:** Use `test.step()` to wrap each phase — provides better trace visibility and step-level retries:
```typescript
await test.step('Browse and add to cart', () => flow.browseAndAddToCart(products));
await test.step('Complete payment', () => flow.completePayment(card));
```

---

*— End of Section 2, C16–C25 —*

> **Remaining in Section 2 (C26–C30):** Custom HAR mock builder, Bulk operation helper, A/B variant controller, Parallel data-seeding fixture, and Screenshot diff reporter — then Section 3 begins.

---

## C26: Build a HAR-Based Network Mock Builder

**Problem:** Your integration tests hit a paid third-party API (currency exchange, geolocation, credit scoring). Tests are slow, flaky, and expensive. You need to record real responses once and replay them deterministically.

**Difficulty:** Hard

**Task:** Build a `HarMockBuilder` that:
1. Records a real session to HAR
2. Replays stored HAR responses by URL pattern in subsequent tests
3. Allows selective override of specific responses

**Solution:**
```typescript
// utils/HarMockBuilder.ts
import { BrowserContext, Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

interface HarEntry {
  request: { url: string; method: string };
  response: {
    status: number;
    headers: Array<{ name: string; value: string }>;
    content: { text?: string; mimeType: string };
  };
}

interface HarFile {
  log: { entries: HarEntry[] };
}

type ResponseOverride = {
  url: string | RegExp;
  status?: number;
  body?: unknown;
  headers?: Record<string, string>;
};

export class HarMockBuilder {
  private overrides: ResponseOverride[] = [];

  constructor(
    private context: BrowserContext,
    private harDir: string = path.join(process.cwd(), 'fixtures', 'har')
  ) {
    if (!fs.existsSync(this.harDir)) fs.mkdirSync(this.harDir, { recursive: true });
  }

  /** Record mode: capture real network traffic to a HAR file */
  async record(harName: string, action: (page: Page) => Promise<void>): Promise<void> {
    const harPath = path.join(this.harDir, `${harName}.har`);
    await this.context.routeFromHAR(harPath, { update: true });
    const page = await this.context.newPage();
    await action(page);
    await page.close();
    console.log(`[HarMock] Recorded to ${harPath}`);
  }

  /** Replay mode: serve responses from HAR file */
  async replay(harName: string): Promise<void> {
    const harPath = path.join(this.harDir, `${harName}.har`);
    if (!fs.existsSync(harPath)) {
      throw new Error(`HAR file not found: ${harPath}. Run in record mode first.`);
    }

    await this.context.routeFromHAR(harPath, {
      update: false,
      notFound: 'fallthrough', // Pass through non-HAR routes
    });

    // Apply selective overrides on top of HAR
    for (const override of this.overrides) {
      await this.context.route(override.url, (route) => {
        route.fulfill({
          status: override.status ?? 200,
          contentType: 'application/json',
          headers: override.headers,
          body: JSON.stringify(override.body),
        });
      });
    }
  }

  /** Override a specific URL's response on top of HAR replay */
  addOverride(override: ResponseOverride): this {
    this.overrides.push(override);
    return this;
  }

  /** Parse a HAR file and list all captured API endpoints */
  listEndpoints(harName: string): Array<{ method: string; url: string; status: number }> {
    const harPath = path.join(this.harDir, `${harName}.har`);
    const har: HarFile = JSON.parse(fs.readFileSync(harPath, 'utf-8'));
    return har.log.entries.map(e => ({
      method: e.request.method,
      url:    e.request.url,
      status: e.response.status,
    }));
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { HarMockBuilder } from '../utils/HarMockBuilder';

// Record once (run manually or in a dedicated record job):
// TEST_RECORD=true npx playwright test har-record.spec.ts
test.skip(process.env.TEST_RECORD !== 'true', 'Record mode only');
test('record: currency conversion flow', async ({ context, page }) => {
  const har = new HarMockBuilder(context);
  await har.record('currency-exchange', async (p) => {
    await p.goto('/convert?from=USD&to=EUR&amount=100');
    await p.waitForResponse(/api\/exchange-rate/);
  });
});

// Replay in every CI run:
test('currency conversion uses HAR mock', async ({ context, page }) => {
  const har = new HarMockBuilder(context);

  // Override one endpoint to test an edge case
  har.addOverride({
    url: /api\/exchange-rate/,
    body: { rate: 0.92, from: 'USD', to: 'EUR' },
  });

  await har.replay('currency-exchange');
  await page.goto('/convert?from=USD&to=EUR&amount=100');

  await expect(page.getByTestId('converted-amount')).toHaveText('92.00 EUR');
});
```

---

## C27: Parallel Data-Seeding Fixture with Worker Isolation

**Problem:** With 8 parallel workers, all tests against `/admin/users` share the same data. Tests intermittently fail because one worker's cleanup deletes another worker's test data.

**Difficulty:** Hard

**Task:** Build a fixture that gives each worker its own isolated data set via a unique prefix, and auto-cleans only that worker's data after the suite.

**Solution:**
```typescript
// fixtures/IsolatedDataFixture.ts
import { test as base, WorkerInfo } from '@playwright/test';

interface SeedRecord {
  id: string;
  type: 'user' | 'order' | 'product';
}

interface DataSeed {
  prefix: string;
  createUser: (overrides?: Record<string, unknown>) => Promise<{ id: string; email: string }>;
  createOrder: (userId: string, overrides?: Record<string, unknown>) => Promise<{ id: string }>;
  cleanup: () => Promise<void>;
}

type DataFixtures = { seed: DataSeed };

export const test = base.extend<DataFixtures, { workerPrefix: string }>({
  // Worker-scoped prefix — stable per worker for the whole run
  workerPrefix: [async ({}, use, workerInfo: WorkerInfo) => {
    const prefix = `w${workerInfo.workerIndex}-${Date.now()}`;
    await use(prefix);
  }, { scope: 'worker' }],

  // Test-scoped: creates data with the worker prefix and tracks for cleanup
  seed: async ({ workerPrefix, request }, use) => {
    const created: SeedRecord[] = [];

    const seed: DataSeed = {
      prefix: workerPrefix,

      async createUser(overrides = {}) {
        const email = `${workerPrefix}-user-${Date.now()}@test.com`;
        const res = await request.post('/api/test/users', {
          data: { name: `Test User ${workerPrefix}`, email, role: 'user', ...overrides },
          headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
        });
        const user = await res.json();
        created.push({ id: user.id, type: 'user' });
        return user;
      },

      async createOrder(userId, overrides = {}) {
        const res = await request.post('/api/test/orders', {
          data: { userId, items: [{ productId: 'prod-test', quantity: 1 }], ...overrides },
          headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
        });
        const order = await res.json();
        created.push({ id: order.id, type: 'order' });
        return order;
      },

      async cleanup() {
        // Delete in reverse order to respect FK constraints
        const reversed = [...created].reverse();
        await Promise.allSettled(
          reversed.map(r =>
            request.delete(`/api/test/${r.type}s/${r.id}`, {
              headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
            })
          )
        );
        created.length = 0;
      },
    };

    await use(seed);
    await seed.cleanup(); // Auto-cleanup after every test
  },
});
```

```typescript
// Usage
import { test, expect } from '../fixtures/IsolatedDataFixture';

test('admin can deactivate a user', async ({ page, seed }) => {
  const user = await seed.createUser({ role: 'user' });

  await page.goto(`/admin/users/${user.id}`);
  await page.getByRole('button', { name: 'Deactivate' }).click();

  await expect(page.getByTestId('user-status')).toHaveText('Inactive');
  // seed.cleanup() runs automatically — only deletes THIS worker's user
});

test('users list shows seeded users', async ({ page, seed }) => {
  // These users have the worker prefix — won't collide with other workers
  await seed.createUser({ name: 'Alice Test' });
  await seed.createUser({ name: 'Bob Test' });

  await page.goto('/admin/users');
  await page.getByPlaceholder('Search').fill(seed.prefix);

  await expect(page.getByRole('row').filter({ hasText: 'Alice Test' })).toBeVisible();
  await expect(page.getByRole('row').filter({ hasText: 'Bob Test' })).toBeVisible();
});
```

---

## C28: Build an A/B Test Variant Controller

**Problem:** Your app runs A/B tests via a feature flag service. During testing you need deterministic control over which variant a test session sees, independent of the flag service's assignment logic.

**Difficulty:** Medium

**Task:** Build a `VariantController` that:
1. Intercepts the feature flag API and returns a specified variant
2. Injects variant overrides via `localStorage` / `cookies` without hitting the network
3. Validates which variant is rendered by observing the DOM

**Solution:**
```typescript
// utils/VariantController.ts
import { BrowserContext, Page } from '@playwright/test';

type Variant = 'control' | 'treatment' | string;

interface FlagOverride {
  flag: string;
  variant: Variant;
}

export class VariantController {
  private overrides: FlagOverride[] = [];

  constructor(private context: BrowserContext) {}

  /** Force specific flag variants for this context */
  setVariant(flag: string, variant: Variant): this {
    this.overrides.push({ flag, variant });
    return this;
  }

  /**
   * Install overrides via two mechanisms:
   * 1. Intercept the feature flag HTTP endpoint
   * 2. Set localStorage overrides before page load (for client-side SDKs)
   */
  async install(): Promise<void> {
    // Intercept LaunchDarkly / Split / Optimizely style evaluation endpoint
    await this.context.route('**/api/feature-flags**', async (route) => {
      const response = await route.fetch();
      const flags = await response.json();

      // Merge our overrides into the real flag response
      for (const { flag, variant } of this.overrides) {
        if (flag in flags) {
          flags[flag] = { ...flags[flag], variant, enabled: true };
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(flags),
      });
    });

    // Also inject via addInitScript for client-side SDK overrides
    await this.context.addInitScript((overrides: FlagOverride[]) => {
      const stored: Record<string, Variant> = {};
      for (const { flag, variant } of overrides) {
        stored[flag] = variant;
      }
      // Many SDKs check localStorage for dev overrides
      localStorage.setItem('ff_overrides', JSON.stringify(stored));
      // Set cookie-based overrides too
      for (const { flag, variant } of overrides) {
        document.cookie = `ff_${flag}=${variant}; path=/`;
      }
    }, this.overrides);
  }

  /** Verify which variant is actually rendered */
  async assertVariantRendered(page: Page, flag: string, variant: Variant): Promise<void> {
    const rendered = await page.getAttribute(`[data-feature-flag="${flag}"]`, 'data-variant');
    if (rendered !== variant) {
      throw new Error(
        `Flag "${flag}": expected variant "${variant}", got "${rendered ?? 'not found'}"`
      );
    }
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { VariantController } from '../utils/VariantController';

test.describe('A/B: New Checkout Flow', () => {
  test('treatment: shows single-page checkout', async ({ context, page }) => {
    const variants = new VariantController(context);
    variants
      .setVariant('new_checkout_flow', 'treatment')
      .setVariant('express_payment', 'enabled');
    await variants.install();

    await page.goto('/cart');
    await page.getByRole('button', { name: 'Checkout' }).click();

    // Treatment renders single-page layout
    await expect(page.getByTestId('single-page-checkout')).toBeVisible();
    await expect(page.getByTestId('multi-step-checkout')).not.toBeVisible();

    await variants.assertVariantRendered(page, 'new_checkout_flow', 'treatment');
  });

  test('control: shows multi-step checkout', async ({ context, page }) => {
    const variants = new VariantController(context);
    variants.setVariant('new_checkout_flow', 'control');
    await variants.install();

    await page.goto('/cart');
    await page.getByRole('button', { name: 'Checkout' }).click();

    await expect(page.getByTestId('multi-step-checkout')).toBeVisible();
    await expect(page.getByTestId('single-page-checkout')).not.toBeVisible();
  });
});
```

---

## C29: Bulk Operation Test Helper

**Problem:** Your admin panel has bulk operations: select 50 rows, click "Bulk Delete", confirm a modal, and verify all 50 records are removed. Testing this manually is impractical; doing it in UI is slow.

**Difficulty:** Medium

**Task:** Build a `BulkOperationHelper` that seeds records via API, selects them using the UI's "Select All" or individual checkboxes, and verifies bulk action outcomes.

**Solution:**
```typescript
// utils/BulkOperationHelper.ts
import { Page, APIRequestContext, expect } from '@playwright/test';

interface BulkOperationConfig {
  tableSelector:       string;
  selectAllSelector:   string;
  rowCheckboxSelector: string;
  bulkActionSelector:  string;
  confirmSelector?:    string;
  rowCountSelector?:   string;
}

export class BulkOperationHelper {
  constructor(
    private page: Page,
    private request: APIRequestContext,
    private config: BulkOperationConfig
  ) {}

  /** Select all visible rows via the "Select All" header checkbox */
  async selectAll(): Promise<number> {
    const selectAll = this.page.locator(this.config.selectAllSelector);
    await selectAll.check();

    const checked = await this.page.locator(this.config.rowCheckboxSelector + ':checked').count();
    return checked;
  }

  /** Select specific rows by their text content */
  async selectRowsMatching(texts: string[]): Promise<void> {
    for (const text of texts) {
      const row = this.page
        .locator(this.config.tableSelector)
        .getByRole('row')
        .filter({ hasText: text });

      await row.locator('input[type="checkbox"]').check();
    }
  }

  /** Select a specific count of rows (e.g., first 10) */
  async selectFirstN(n: number): Promise<void> {
    const checkboxes = this.page.locator(this.config.rowCheckboxSelector);
    const count = await checkboxes.count();
    const toSelect = Math.min(n, count);

    for (let i = 0; i < toSelect; i++) {
      await checkboxes.nth(i).check();
    }
  }

  /** Execute a bulk action from the bulk-actions dropdown/button */
  async executeBulkAction(actionName: string, confirm = true): Promise<void> {
    await this.page.locator(this.config.bulkActionSelector).click();
    await this.page.getByRole('menuitem', { name: actionName }).click();

    if (confirm && this.config.confirmSelector) {
      await this.page.locator(this.config.confirmSelector).click();
    } else if (confirm) {
      // Handle browser confirm dialog
      this.page.once('dialog', d => d.accept());
    }

    // Wait for table to update
    await this.page.waitForLoadState('networkidle');
  }

  /** Assert the visible row count in the table */
  async assertRowCount(expected: number): Promise<void> {
    if (this.config.rowCountSelector) {
      await expect(this.page.locator(this.config.rowCountSelector))
        .toHaveText(new RegExp(`${expected}`));
    } else {
      // Count data rows (exclude header)
      const rows = this.page
        .locator(this.config.tableSelector)
        .getByRole('row')
        .filter({ hasNot: this.page.locator('th') });
      await expect(rows).toHaveCount(expected);
    }
  }

  /** Seed N records via API and return their IDs */
  async seedRecords(
    endpoint: string,
    count: number,
    template: (i: number) => Record<string, unknown>,
    token: string
  ): Promise<string[]> {
    const ids: string[] = [];

    // Batch in groups of 10 for speed
    const batchSize = 10;
    for (let batch = 0; batch < Math.ceil(count / batchSize); batch++) {
      const batchPromises = Array.from(
        { length: Math.min(batchSize, count - batch * batchSize) },
        (_, i) => this.request.post(endpoint, {
          data: template(batch * batchSize + i),
          headers: { Authorization: `Bearer ${token}` },
        })
      );
      const responses = await Promise.all(batchPromises);
      for (const res of responses) {
        const record = await res.json();
        ids.push(record.id);
      }
    }

    return ids;
  }
}
```

```typescript
// Usage
import { test, expect } from '@playwright/test';
import { BulkOperationHelper } from '../utils/BulkOperationHelper';

test('bulk delete 25 users from admin panel', async ({ page, request }) => {
  const bulk = new BulkOperationHelper(page, request, {
    tableSelector:       '[data-testid="users-table"]',
    selectAllSelector:   '[data-testid="select-all-checkbox"]',
    rowCheckboxSelector: '[data-testid="row-checkbox"]',
    bulkActionSelector:  '[data-testid="bulk-actions-btn"]',
    confirmSelector:     '[data-testid="confirm-delete-btn"]',
    rowCountSelector:    '[data-testid="row-count"]',
  });

  // Seed 25 users via API
  const ids = await bulk.seedRecords(
    '/api/test/users', 25,
    (i) => ({ name: `Bulk Test ${i}`, email: `bulk-${i}-${Date.now()}@test.com`, role: 'user' }),
    process.env.SEED_TOKEN!
  );

  await page.goto('/admin/users?filter=bulk-');

  const selected = await bulk.selectAll();
  expect(selected).toBe(25);

  await bulk.executeBulkAction('Delete Selected', true);
  await bulk.assertRowCount(0);

  // Verify via API too
  for (const id of ids.slice(0, 3)) {
    const res = await request.get(`/api/users/${id}`);
    expect(res.status()).toBe(404);
  }
});
```

---

## C30: Screenshot Diff Reporter with Baseline Management

**Problem:** Visual regression fails are hard to review. The team needs a self-contained HTML report showing side-by-side diffs with approve/reject buttons that update the baseline.

**Difficulty:** Hard

**Task:** Build a custom reporter that generates a standalone HTML visual diff review page from `toHaveScreenshot` failures.

**Solution:**
```typescript
// reporters/VisualDiffReporter.ts
import type {
  Reporter, TestCase, TestResult, FullResult
} from '@playwright/test/reporter';
import * as fs from 'fs';
import * as path from 'path';

interface DiffEntry {
  testName: string;
  file: string;
  expectedPath: string;
  actualPath: string;
  diffPath: string;
}

export default class VisualDiffReporter implements Reporter {
  private diffs: DiffEntry[] = [];
  private outputDir: string;

  constructor(options: { outputDir?: string } = {}) {
    this.outputDir = options.outputDir ?? path.join(process.cwd(), 'visual-diff-report');
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    if (result.status !== 'failed') return;

    for (const attachment of result.attachments) {
      if (attachment.name === 'screenshot-diff' || attachment.name?.includes('-diff.png')) {
        const expected = result.attachments.find(a => a.name?.includes('-expected.png'));
        const actual   = result.attachments.find(a => a.name?.includes('-actual.png'));
        const diff     = attachment;

        if (expected?.path && actual?.path && diff?.path) {
          this.diffs.push({
            testName: test.titlePath().slice(1).join(' > '),
            file:     test.location.file.replace(process.cwd(), ''),
            expectedPath: expected.path,
            actualPath:   actual.path,
            diffPath:     diff.path,
          });
        }
      }
    }
  }

  onEnd(_result: FullResult): void {
    if (this.diffs.length === 0) return;
    if (!fs.existsSync(this.outputDir)) fs.mkdirSync(this.outputDir, { recursive: true });

    const imagesDir = path.join(this.outputDir, 'images');
    fs.mkdirSync(imagesDir, { recursive: true });

    // Copy images and encode as base64 for self-contained report
    const entries = this.diffs.map((d, i) => {
      const toBase64 = (p: string) => {
        if (!fs.existsSync(p)) return '';
        return `data:image/png;base64,${fs.readFileSync(p).toString('base64')}`;
      };

      return {
        ...d,
        id: `diff-${i}`,
        expectedB64: toBase64(d.expectedPath),
        actualB64:   toBase64(d.actualPath),
        diffB64:     toBase64(d.diffPath),
      };
    });

    const html = this.generateHtml(entries);
    const reportPath = path.join(this.outputDir, 'index.html');
    fs.writeFileSync(reportPath, html);
    console.log(`\n[VisualDiff] Report: ${reportPath} (${this.diffs.length} diff(s))\n`);
  }

  private generateHtml(entries: ReturnType<typeof this.onEnd extends () => void ? never : any>[]): string {
    const cards = (entries as any[]).map(e => `
      <div class="card" id="${e.id}">
        <h2>${e.testName}</h2>
        <small>${e.file}</small>
        <div class="images">
          <figure><figcaption>Expected (Baseline)</figcaption><img src="${e.expectedB64}" /></figure>
          <figure><figcaption>Actual (Run)</figcaption><img src="${e.actualB64}" /></figure>
          <figure><figcaption>Diff</figcaption><img src="${e.diffB64}" /></figure>
        </div>
        <div class="actions">
          <button class="approve" onclick="approve('${e.id}', '${e.actualPath.replace(/\\/g, '\\\\')}', '${e.expectedPath.replace(/\\/g, '\\\\')}')">
            ✓ Approve (update baseline)
          </button>
          <button class="reject" onclick="reject('${e.id}')">✗ Reject</button>
        </div>
      </div>
    `).join('');

    return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Visual Diff Report — ${new Date().toLocaleString()}</title>
<style>
  body { font-family: system-ui, sans-serif; margin: 0; padding: 20px; background: #0f0f0f; color: #eee; }
  h1 { color: #f8f8f8; }
  .summary { background: #1a1a2e; padding: 12px 20px; border-radius: 8px; margin-bottom: 24px; }
  .card { background: #1e1e1e; border-radius: 10px; padding: 20px; margin-bottom: 28px; border: 1px solid #333; }
  .card h2 { margin: 0 0 4px; font-size: 1rem; color: #ff6b6b; }
  .card small { color: #888; font-size: 0.75rem; }
  .images { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin: 16px 0; }
  figure { margin: 0; }
  figcaption { font-size: 0.75rem; color: #aaa; margin-bottom: 6px; }
  img { width: 100%; border-radius: 6px; border: 1px solid #444; }
  .actions { display: flex; gap: 12px; }
  .approve { background: #27ae60; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
  .reject  { background: #c0392b; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
  .approved { opacity: 0.4; border-color: #27ae60; }
  .rejected { opacity: 0.4; border-color: #c0392b; }
</style>
</head>
<body>
<h1>Visual Regression Diff Report</h1>
<div class="summary">${(entries as any[]).length} visual difference(s) found on ${new Date().toLocaleString()}</div>
${cards}
<script>
function approve(id, actualPath, expectedPath) {
  // In a real tool: POST to a local server endpoint that copies actualPath → expectedPath
  // Here we just mark the card visually
  document.getElementById(id).classList.add('approved');
  alert('Baseline update queued. Re-run: playwright test --update-snapshots for: ' + expectedPath);
}
function reject(id) {
  document.getElementById(id).classList.add('rejected');
}
</script>
</body>
</html>`;
  }

  printsToStdio(): boolean { return false; }
}
```

```typescript
// playwright.config.ts
export default defineConfig({
  reporter: [
    ['list'],
    ['./reporters/VisualDiffReporter', { outputDir: 'visual-diff-report' }],
  ],
});
```

**Alternative Approach:** Use `@playwright/test`'s built-in HTML reporter which already shows screenshot diffs — the custom reporter above adds approve/reject workflow integration for teams who want one-click baseline updates.

---

*— End of Section 2, C26–C30 — Section 2 Complete —*

> **Next:** Section 3 — Framework / Architecture Questions (20 questions)

---

---

# SECTION 3 — FRAMEWORK & ARCHITECTURE QUESTIONS (20)

---

## A1: How would you design a Playwright framework from scratch for a 50-engineer QA team at a FinTech company?

**Answer:** A production-grade framework for 50 engineers requires layered architecture with clear separation of concerns, enforced conventions, and self-service tooling so engineers can onboard without tribal knowledge.

**Core Layers:**
```
playwright-framework/
├── config/
│   ├── playwright.config.ts       # Environment-aware base config
│   ├── environments/              # dev.ts, staging.ts, production.ts
│   └── feature-flags.ts           # Flag overrides per env
├── fixtures/
│   ├── index.ts                   # Re-exports all composed fixtures
│   ├── auth.fixture.ts            # Role-based auth contexts
│   ├── database.fixture.ts        # Worker-scoped DB seed/cleanup
│   ├── api.fixture.ts             # Pre-configured ApiClient per role
│   └── page-objects.fixture.ts    # POM instances injected per test
├── pages/                         # Page Object Model classes
│   ├── base.page.ts               # BasePage with common methods
│   ├── auth/
│   ├── dashboard/
│   └── admin/
├── api/                           # API client layer
│   ├── ApiClient.ts               # Base HTTP wrapper
│   ├── endpoints/                 # UsersApi, OrdersApi, etc.
│   └── schemas/                   # Zod contracts
├── utils/                         # Reusable utilities
│   ├── NetworkCapture.ts
│   ├── waitUntil.ts
│   ├── TableHelper.ts
│   └── ...
├── reporters/                     # Custom reporters
├── global-setup.ts                # Auth warm-up, DB migrations
├── global-teardown.ts             # Cleanup, report publish
└── tests/
    ├── smoke/                     # @smoke tagged, run on every deploy
    ├── regression/                # Full suite
    ├── api-contracts/             # Contract tests
    └── performance/               # Budget tests
```

**Key Architectural Decisions:**

| Decision | Choice | Rationale |
|---|---|---|
| Fixture composition | Extend `base.extend<>()` chain | Type-safe DI, auto cleanup |
| Auth strategy | Storage state + global-setup warm-up | 5s/test saving × 600 tests = 50 min CI reduction |
| Parallelism | `workers: 50%` in CI, `--shard` across runners | Balances speed vs resource cost |
| Data strategy | API seeding + worker-scoped prefix | Eliminates cross-worker interference |
| Locator policy | Semantic locators only (role, label, testid) | Resilient to CSS/structure changes |
| Reporting | List + HTML + Slack (CI) | Traceability at each level |
| Config | Single `getConfig()` + env var overrides | One source of truth for all environments |

**TypeScript Configuration:**
```typescript
// tsconfig.json — strict mode enforced
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "target": "ES2022",
    "moduleResolution": "bundler",
    "paths": { "@fixtures": ["./fixtures/index.ts"], "@pages/*": ["./pages/*"] }
  }
}
```

**Onboarding Self-Service:**
- `npm run test:new` scaffolds fixture + POM stubs for a new feature
- ESLint plugin `eslint-plugin-playwright` enforces no `page.locator('div > span')`, no hardcoded waits
- Pre-commit hook runs type-check + lint on changed test files only

**Tricky Follow-ups:**
1. *How do you prevent engineers from bypassing the fixture layer and calling `page.goto` directly without auth?* — ESLint custom rule that flags `page.goto` outside of page objects.
2. *How do you handle when a fixture teardown fails silently?* — Wrap all teardown in try/catch/log; use `testInfo.errors` to surface fixture teardown failures.

---

## A2: How do you implement test sharding and optimal CI distribution for a 600-test Playwright suite?

**Answer:** Playwright's native sharding splits the test files across N machines. The key challenge is uneven shard load — slow tests cluster on one shard while others finish early.

**Basic Sharding (GitHub Actions Matrix):**
```yaml
# .github/workflows/playwright.yml
jobs:
  test:
    strategy:
      fail-fast: false
      matrix:
        shard: [1, 2, 3, 4, 5, 6, 7, 8]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }
      - run: npm ci
      - run: npx playwright install chromium --with-deps
      - run: npx playwright test --shard=${{ matrix.shard }}/8
        env:
          BASE_URL: ${{ secrets.STAGING_URL }}
          CI: true
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report-shard-${{ matrix.shard }}
          path: playwright-report/
          retention-days: 7
```

**Merging Shard Reports:**
```yaml
  merge-reports:
    needs: test
    if: always()
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - uses: actions/download-artifact@v4
        with: { path: all-blob-reports, pattern: playwright-report-shard-* }
      - run: npx playwright merge-reports --reporter html ./all-blob-reports
      - uses: actions/upload-artifact@v4
        with: { name: html-report, path: playwright-report/ }
```

**Optimizing Shard Balance with `--fully-parallel`:**
```typescript
// playwright.config.ts
export default defineConfig({
  fullyParallel: true,      // Shards tests WITHIN files, not just by file
  workers: process.env.CI ? 4 : '50%',
  retries: process.env.CI ? 1 : 0,

  use: {
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
});
```

**Dynamic Shard Count Based on Suite Size:**
```typescript
// scripts/compute-shards.ts — run before matrix is determined
import { execSync } from 'child_process';

const testCount = parseInt(
  execSync('npx playwright test --list --reporter=json | jq ".suites | length"').toString().trim()
);
const testsPerShard = 75;
const shards = Math.ceil(testCount / testsPerShard);
console.log(shards); // Used by GH Actions: matrix.shard count
```

**Tricky Follow-ups:**
1. *A single test file has 40 tests and takes 8 minutes — it bottlenecks one shard. How do you fix it?* — Split the file into 4 files of 10, or use `test.describe.configure({ mode: 'parallel' })` within the file so `fullyParallel: true` can distribute them.
2. *How do you retry only failed tests without re-running the full suite?* — Use `--last-failed` flag: `npx playwright test --last-failed` reads `.playwright-last-run.json` to replay only failures.

---

## A3: Explain how Playwright fixtures compose and how you design a clean fixture hierarchy

**Answer:** Playwright's `test.extend()` creates fixture chains via TypeScript generics. Each level adds capabilities while preserving type safety for all downstream consumers.

**Three-tier fixture hierarchy:**

```typescript
// fixtures/1-base.fixture.ts — Foundation: environment + raw browser context
import { test as playwrightTest, expect } from '@playwright/test';
import { getConfig } from '../config/EnvironmentConfig';

type BaseFixtures = { config: ReturnType<typeof getConfig> };

export const baseTest = playwrightTest.extend<BaseFixtures>({
  config: async ({}, use) => {
    await use(getConfig());
  },
});
export { expect };
```

```typescript
// fixtures/2-auth.fixture.ts — Auth layer: role-based contexts
import { baseTest } from './1-base.fixture';
import { BrowserContext } from '@playwright/test';

type AuthFixtures = {
  adminContext:    BrowserContext;
  userContext:     BrowserContext;
  readonlyContext: BrowserContext;
};

export const authTest = baseTest.extend<AuthFixtures>({
  adminContext: async ({ browser, config }, use) => {
    const ctx = await browser.newContext({
      storageState: `auth/admin.json`,
      baseURL: config.baseUrl,
    });
    await use(ctx);
    await ctx.close();
  },
  userContext: async ({ browser, config }, use) => {
    const ctx = await browser.newContext({
      storageState: `auth/user.json`,
      baseURL: config.baseUrl,
    });
    await use(ctx);
    await ctx.close();
  },
  readonlyContext: async ({ browser, config }, use) => {
    const ctx = await browser.newContext({ storageState: 'auth/readonly.json' });
    await use(ctx);
    await ctx.close();
  },
});
```

```typescript
// fixtures/3-pages.fixture.ts — POM layer: pre-constructed page objects
import { authTest } from './2-auth.fixture';
import { DashboardPage } from '../pages/DashboardPage';
import { AdminPage } from '../pages/AdminPage';
import { Page } from '@playwright/test';

type PageFixtures = {
  dashboardPage: DashboardPage;
  adminPage: AdminPage;
  adminCtxPage: Page;
};

export const test = authTest.extend<PageFixtures>({
  adminCtxPage: async ({ adminContext }, use) => {
    const page = await adminContext.newPage();
    await use(page);
    await page.close();
  },

  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },

  adminPage: async ({ adminCtxPage }, use) => {
    await use(new AdminPage(adminCtxPage));
  },
});

export { expect } from '@playwright/test';
```

```typescript
// fixtures/index.ts — single export point for all tests
export { test, expect } from './3-pages.fixture';
```

```typescript
// Any test — imports only from fixtures/index.ts
import { test, expect } from '../fixtures';

test('admin approves user', async ({ adminPage, dashboardPage, config }) => {
  // All fixtures available with full type inference
  expect(config.env).toBeDefined();
  await adminPage.navigate();
});
```

**Fixture Scopes Decision Matrix:**

| Scope | When to Use | Example |
|---|---|---|
| `test` (default) | Per-test data, pages, contexts | `seedUsers`, `page`, `dashboardPage` |
| `worker` | Shared read-heavy setup across tests in a worker | DB connection, read-only auth state |
| `project` | Not a Playwright concept — use `globalSetup` instead | Auth state files written to disk |

**Tricky Follow-ups:**
1. *What happens if a fixture's `use()` is never called?* — Playwright throws a timeout error. Always ensure `use()` is called even in error paths.
2. *Can two fixtures depend on each other circularly?* — No, circular deps cause a startup error. Design fixtures as a strict DAG.

---

## A4: How do you implement zero-flakiness strategy in a large Playwright suite?

**Answer:** Flakiness has root causes — the strategy systematically eliminates each category.

**Category 1: Async Timing (most common)**
```typescript
// ❌ Flaky: hardcoded waits
await page.waitForTimeout(2000);
await page.click('button');

// ✅ Reliable: wait for the condition that proves readiness
await page.getByRole('button', { name: 'Submit' }).click();
// Playwright auto-waits for: visible, enabled, not-obscured, stable, receives-events

// ✅ For eventually-consistent state:
await expect.poll(
  () => request.get('/api/status').then(r => r.json()).then(j => j.status),
  { timeout: 30_000, intervals: [1000, 2000, 5000] }
).toBe('complete');
```

**Category 2: Test Isolation (test data)**
```typescript
// ❌ Flaky: tests share data
test('delete user alice', ...); // Alice may not exist if other test ran first

// ✅ Each test creates its own data
test('delete user', async ({ seed }) => {
  const user = await seed.createUser(); // Worker-prefixed, isolated
  // ... test ...
  // seed.cleanup() runs automatically
});
```

**Category 3: Race Conditions in Network**
```typescript
// ❌ Flaky: click then assert (response may not have arrived)
await page.getByRole('button', { name: 'Save' }).click();
await expect(page.getByText('Saved!')).toBeVisible();

// ✅ Assert the network round-trip AND the UI state
const [response] = await Promise.all([
  page.waitForResponse(r => r.url().includes('/api/save') && r.status() === 200),
  page.getByRole('button', { name: 'Save' }).click(),
]);
await expect(page.getByText('Saved!')).toBeVisible();
```

**Category 4: Shared Browser State**
```typescript
// ✅ Each test gets a fresh BrowserContext — cookies, localStorage, IndexedDB are isolated
// This is Playwright's default behaviour with `page` fixture
```

**Flakiness Detection and Quarantine Pipeline:**
```yaml
# CI: periodic flakiness detection job
- run: npx playwright test --repeat-each=5 --reporter=json > flaky-report.json
- run: node scripts/analyze-flakiness.js flaky-report.json
  # Flags tests with >1 failure in 5 runs, posts to Slack
```

```typescript
// scripts/analyze-flakiness.ts
import report from './flaky-report.json';
const flaky = report.suites
  .flatMap(s => s.specs)
  .filter(spec => spec.tests.some(t => t.results.some(r => r.status === 'failed')));

if (flaky.length > 0) {
  console.error(`Flaky tests detected:\n${flaky.map(f => `  - ${f.title}`).join('\n')}`);
  process.exit(1);
}
```

**Playwright Config for Maximum Stability:**
```typescript
export default defineConfig({
  retries:    process.env.CI ? 2 : 0,
  timeout:    45_000,
  expect:     { timeout: 10_000 },
  fullyParallel: true,
  use: {
    actionTimeout:    15_000,
    navigationTimeout: 30_000,
    trace: 'on-first-retry',
  },
});
```

**Tricky Follow-ups:**
1. *A test passes 49/50 times but fails 1/50. How do you debug it?* — Run `--repeat-each=20` with `--trace=on` and examine the trace of the one failure for animation frames, network timing differences, or state leakage.
2. *Page Object methods don't have timeouts — how do you propagate test-level timeouts into them?* — Pass `testInfo.timeout` into the POM constructor and use it as the default for `waitFor` calls.

---

## A5: Design the auth strategy for a suite that tests 5 user roles against 200 features

**Answer:**

**Auth Architecture:**
```
global-setup.ts
  └── for each role (admin, manager, user, readonly, service)
        └── UI login once → storageState saved to auth/{role}.json
        └── Also save API token to auth/{role}-token.json

tests/
  └── Every test imports { test } from fixtures/
  └── fixture auto-selects storageState based on declared role need
  └── Tests that need no auth use { page } directly
```

**Storage State Strategy:**
```typescript
// global-setup.ts
import { chromium, FullConfig } from '@playwright/test';

const ROLES = ['admin', 'manager', 'user', 'readonly', 'service'] as const;

export default async function globalSetup(config: FullConfig) {
  const browser = await chromium.launch();
  const baseURL = config.projects[0].use.baseURL as string;

  await Promise.all(ROLES.map(async (role) => {
    const statePath = `auth/${role}.json`;
    // Skip if recently cached (< 4h)
    if (isCacheValid(statePath)) return;

    const ctx = await browser.newContext({ baseURL });
    const page = await ctx.newPage();

    await page.goto('/login');
    await page.getByLabel('Email').fill(process.env[`${role.toUpperCase()}_EMAIL`]!);
    await page.getByLabel('Password').fill(process.env[`${role.toUpperCase()}_PASSWORD`]!);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.waitForURL('/dashboard');

    // Capture both browser state AND API token
    await ctx.storageState({ path: statePath });
    const token = await page.evaluate(() => localStorage.getItem('apiToken'));
    require('fs').writeFileSync(`auth/${role}-token.json`, JSON.stringify({ token }));

    await ctx.close();
  }));

  await browser.close();
}
```

**Permission Matrix Test Pattern:**
```typescript
// tests/permissions/feature-access.spec.ts
import { test, expect } from '../fixtures';

const FEATURE_MATRIX = [
  { url: '/admin/users',  canAccess: ['admin'],                  cannotAccess: ['user', 'readonly'] },
  { url: '/reports',      canAccess: ['admin', 'manager', 'user'], cannotAccess: ['readonly'] },
  { url: '/billing',      canAccess: ['admin'],                  cannotAccess: ['manager', 'user', 'readonly'] },
] as const;

for (const { url, canAccess, cannotAccess } of FEATURE_MATRIX) {
  for (const role of canAccess) {
    test(`${role} can access ${url}`, async ({ browser }) => {
      const ctx = await browser.newContext({ storageState: `auth/${role}.json` });
      const page = await ctx.newPage();
      await page.goto(url);
      await expect(page).not.toHaveURL(/\/403|\/unauthorized/);
      await ctx.close();
    });
  }

  for (const role of cannotAccess) {
    test(`${role} cannot access ${url}`, async ({ browser }) => {
      const ctx = await browser.newContext({ storageState: `auth/${role}.json` });
      const page = await ctx.newPage();
      await page.goto(url);
      await expect(page).toHaveURL(/\/403|\/unauthorized|\/login/);
      await ctx.close();
    });
  }
}
```

**Tricky Follow-ups:**
1. *What if a role's auth state expires mid-test-run?* — Detect 401 responses via `page.on('response')` in a worker-level fixture, trigger re-auth when detected, and refresh the `.json` file atomically.
2. *How do you test that permission changes take effect immediately without restart?* — After changing the role in the admin UI (as admin), reload the affected user's page and assert the new permission state is reflected within one navigation.

---

## A6: How do you implement the Page Object Model at enterprise scale?

**Answer:** At enterprise scale, POM needs strict conventions, type safety, inter-page navigation contracts, and a clear boundary between "what the page can do" vs "test assertions."

**Base Page Pattern:**
```typescript
// pages/BasePage.ts
import { Page, Locator, expect } from '@playwright/test';

export abstract class BasePage {
  // Each subclass declares its own URL
  abstract readonly url: string;

  // Navigation
  protected abstract isLoaded(): Promise<boolean>;

  constructor(protected page: Page) {}

  async navigate(): Promise<void> {
    await this.page.goto(this.url);
    await this.waitUntilLoaded();
  }

  async waitUntilLoaded(timeout = 15_000): Promise<void> {
    await expect
      .poll(() => this.isLoaded(), { timeout, message: `${this.constructor.name} did not load` })
      .toBe(true);
  }

  // Typed navigation that returns the destination POM
  async goto<T extends BasePage>(PageClass: new (page: Page) => T): Promise<T> {
    const target = new PageClass(this.page);
    await target.navigate();
    return target;
  }

  // Shared: close any open modal
  async closeModal(): Promise<void> {
    const closeBtn = this.page.getByRole('button', { name: 'Close' });
    if (await closeBtn.isVisible()) await closeBtn.click();
  }
}
```

```typescript
// pages/DashboardPage.ts
import { BasePage } from './BasePage';
import { Locator, expect } from '@playwright/test';

export class DashboardPage extends BasePage {
  readonly url = '/dashboard';

  // Locators as getters — evaluated lazily, never stale
  get revenueWidget(): Locator { return this.page.getByTestId('revenue-widget'); }
  get userCountWidget(): Locator { return this.page.getByTestId('user-count-widget'); }
  get navMenu(): Locator { return this.page.getByRole('navigation'); }

  protected async isLoaded(): Promise<boolean> {
    return this.revenueWidget.isVisible().catch(() => false);
  }

  async getRevenue(): Promise<number> {
    const text = await this.revenueWidget.getByTestId('value').innerText();
    return parseFloat(text.replace(/[^0-9.]/g, ''));
  }

  async navigateToSection(section: 'Users' | 'Orders' | 'Reports' | 'Settings'): Promise<void> {
    await this.navMenu.getByRole('link', { name: section }).click();
  }

  async assertMetrics(expected: { revenue?: number; users?: number }): Promise<void> {
    if (expected.revenue !== undefined) {
      await expect(this.revenueWidget.getByTestId('value')).toHaveText(
        new RegExp(expected.revenue.toLocaleString())
      );
    }
  }
}
```

**Inter-Page Navigation with Type Safety:**
```typescript
// pages/LoginPage.ts
import { BasePage } from './BasePage';
import { DashboardPage } from './DashboardPage';

export class LoginPage extends BasePage {
  readonly url = '/login';
  protected async isLoaded() { return this.page.getByLabel('Email').isVisible(); }

  async loginAs(email: string, password: string): Promise<DashboardPage> {
    await this.page.getByLabel('Email').fill(email);
    await this.page.getByLabel('Password').fill(password);
    await this.page.getByRole('button', { name: 'Sign in' }).click();

    const dashboard = new DashboardPage(this.page);
    await dashboard.waitUntilLoaded();
    return dashboard; // Strongly typed return
  }
}
```

**Tricky Follow-ups:**
1. *Should Page Objects contain assertions?* — Only self-validation assertions (e.g., `isLoaded()`). Business assertions belong in tests. This keeps POMs reusable across positive and negative test cases.
2. *How do you handle pages with infinite scroll or lazy-loaded sections?* — Add explicit scroll+wait methods to the POM (`async scrollToLoadAll()`) that encapsulate the scroll-and-wait logic so tests remain declarative.

---

## A7: How do you architect Playwright for microservices — testing across 12 services simultaneously?

**Answer:** Microservices testing requires a fan-out API layer, service-level contract tests, and integration tests that orchestrate multiple services without relying on full UI for setup.

**Architecture:**
```
tests/
├── service-contracts/      # Zod-based per-service contract tests
│   ├── users-service.spec.ts
│   ├── orders-service.spec.ts
│   └── payments-service.spec.ts
├── integration/            # Multi-service E2E flows
│   ├── checkout-flow.spec.ts    # UI + 3 API services
│   └── notification-flow.spec.ts
└── smoke/                  # One test per service verifying health
```

**Service Registry Pattern:**
```typescript
// config/services.ts
interface ServiceConfig { baseUrl: string; healthPath: string; }

export const SERVICES: Record<string, ServiceConfig> = {
  users:        { baseUrl: process.env.USERS_SVC_URL!,    healthPath: '/health' },
  orders:       { baseUrl: process.env.ORDERS_SVC_URL!,   healthPath: '/health' },
  payments:     { baseUrl: process.env.PAYMENTS_SVC_URL!, healthPath: '/health' },
  notifications:{ baseUrl: process.env.NOTIF_SVC_URL!,    healthPath: '/ping' },
  gateway:      { baseUrl: process.env.GATEWAY_URL!,      healthPath: '/health' },
};

// global-setup.ts: validate all services are reachable before tests run
export async function verifyServicesHealth(request: APIRequestContext) {
  const results = await Promise.allSettled(
    Object.entries(SERVICES).map(async ([name, svc]) => {
      const res = await request.get(`${svc.baseUrl}${svc.healthPath}`, { timeout: 5000 });
      if (!res.ok()) throw new Error(`${name}: unhealthy (${res.status()})`);
      return name;
    })
  );

  const failed = results.filter(r => r.status === 'rejected') as PromiseRejectedResult[];
  if (failed.length > 0) {
    throw new Error(`Services not healthy:\n${failed.map(f => `  - ${f.reason}`).join('\n')}`);
  }
}
```

**Cross-Service Transaction Test:**
```typescript
test('checkout creates records in 3 services', async ({ request }) => {
  const usersApi    = new ApiClient(request, SERVICES.users.baseUrl, token);
  const ordersApi   = new ApiClient(request, SERVICES.orders.baseUrl, token);
  const paymentsApi = new ApiClient(request, SERVICES.payments.baseUrl, token);

  // 1. Create user in users-service
  const user = await usersApi.post<User>('/users', { email: 'test@e2e.com', role: 'user' });

  // 2. Place order via gateway (fan-out to orders + payments)
  const order = await ordersApi.post<Order>('/orders', {
    userId: user.id,
    items: [{ productId: 'prod-1', quantity: 1 }],
  });

  // 3. Assert payment record created in payments-service
  const payment = await paymentsApi.get<Payment>(`/payments?orderId=${order.id}`);
  expect(payment.status).toBe('captured');
  expect(payment.amount).toBe(order.total);
});
```

**Tricky Follow-ups:**
1. *Service B depends on an event from service A via a message queue. How do you test this without a real queue?* — Use Docker Compose with a real local queue (RabbitMQ, Kafka) in CI, or intercept the outbound Kafka publish and mock the consumer response via `page.route`.
2. *How do you decide which tests belong in service-level contract tests vs E2E integration tests?* — Contract tests: verify the API shape never breaks (fast, run on every PR). Integration tests: verify business flows work end-to-end (slower, run nightly or on merge to main).

---

## A8: How do you implement test data management at scale?

**Answer:** At scale, test data management has three layers:

**Layer 1 — Test Data Factories:**
```typescript
// factories/UserFactory.ts
import { faker } from '@faker-js/faker';

interface UserOverrides {
  email?: string;
  name?: string;
  role?: 'admin' | 'user' | 'readonly';
  verified?: boolean;
}

export class UserFactory {
  static build(overrides: UserOverrides = {}) {
    return {
      email:     overrides.email    ?? faker.internet.email(),
      name:      overrides.name     ?? faker.person.fullName(),
      role:      overrides.role     ?? 'user',
      verified:  overrides.verified ?? true,
      createdAt: new Date().toISOString(),
    };
  }

  static buildList(count: number, overrides: UserOverrides = {}) {
    return Array.from({ length: count }, () => this.build(overrides));
  }

  // Trait pattern for common scenarios
  static asAdmin(overrides: UserOverrides = {}) { return this.build({ ...overrides, role: 'admin' }); }
  static asUnverified(overrides: UserOverrides = {}) { return this.build({ ...overrides, verified: false }); }
}
```

**Layer 2 — API-Level Seeding (fast):**
```typescript
// factories/ApiSeeder.ts
export class ApiSeeder {
  constructor(private request: APIRequestContext, private token: string) {}

  async seedUser(overrides?: UserOverrides): Promise<User> {
    const data = UserFactory.build(overrides);
    const res = await this.request.post('/api/test/users', {
      data,
      headers: { Authorization: `Bearer ${this.token}` },
    });
    return res.json();
  }

  async seedOrderWithItems(userId: string, itemCount = 3): Promise<Order> {
    const items = Array.from({ length: itemCount }, () => ({
      productId: `prod-${faker.string.alphanumeric(6)}`,
      quantity:  faker.number.int({ min: 1, max: 5 }),
    }));
    const res = await this.request.post('/api/test/orders', {
      data: { userId, items },
      headers: { Authorization: `Bearer ${this.token}` },
    });
    return res.json();
  }
}
```

**Layer 3 — Environmental Data Strategy:**

| Environment | Strategy | Why |
|---|---|---|
| Dev/local | Direct DB seed scripts | Fast, full control |
| CI/staging | API seeding via test endpoints | No DB access, service-level fidelity |
| Production | Read-only assertions only | Never write to prod |

**Data Cleanup Strategy:**
- `beforeEach`: Assert clean state via API (not UI)
- `afterEach` fixture: Delete by test-specific prefix/tag
- Weekly cron: Sweep `test_*` prefixed records older than 7 days

**Tricky Follow-ups:**
1. *How do you test negative scenarios (e.g., user with expired subscription) without polluting the DB?* — Use `page.route()` to mock the API response for subscription status — no DB mutation needed.
2. *Production data has edge cases your factories don't. How do you capture them?* — Run your test suite in "shadow mode" against sanitized production data snapshots (PII removed) in a staging DB weekly.

---

## A9: How do you design CI/CD gate quality metrics for a Playwright suite?

**Answer:** Quality gates prevent regressions from reaching production by enforcing measurable thresholds automatically.

**Gate 1 — Pass Rate Threshold:**
```typescript
// scripts/quality-gate.ts
import * as fs from 'fs';

interface PlaywrightReport {
  stats: { expected: number; failed: number; skipped: number; flaky: number };
}

const report: PlaywrightReport = JSON.parse(
  fs.readFileSync('playwright-report/report.json', 'utf-8')
);

const { expected, failed, flaky } = report.stats;
const total = expected + failed;
const passRate = (expected / total) * 100;

const GATES = {
  minPassRate:      98,   // %
  maxFlaky:         5,    // count
  maxNewFailures:   0,    // compared to main branch baseline
};

const violations: string[] = [];

if (passRate < GATES.minPassRate)
  violations.push(`Pass rate ${passRate.toFixed(1)}% < ${GATES.minPassRate}% threshold`);

if (flaky > GATES.maxFlaky)
  violations.push(`${flaky} flaky tests > ${GATES.maxFlaky} max allowed`);

if (violations.length > 0) {
  console.error(`\n❌ Quality gate failed:\n${violations.map(v => `  - ${v}`).join('\n')}\n`);
  process.exit(1);
}

console.log(`✅ Quality gates passed (pass rate: ${passRate.toFixed(1)}%)`);
```

**Gate 2 — Coverage by Feature Tag:**
```yaml
# Enforce smoke tests ALWAYS pass (block deploy)
- run: npx playwright test --grep @smoke
  name: Smoke Gate

# Regression suite gates on merge to main
- run: npx playwright test
  name: Regression Gate
  if: github.ref == 'refs/heads/main'
```

**Gate 3 — Performance Budget Gate:**
```typescript
// In CI — fail the pipeline if Core Web Vitals regress
npx playwright test tests/performance/ --reporter=json | \
  node scripts/check-performance-budget.js
```

**Tricky Follow-ups:**
1. *A gate fails in CI but the test passes locally. What's the most likely cause?* — Environment inconsistency: different `BASE_URL`, missing environment variable, or race condition exposed by CI's slower machines. Add `--trace=on` in CI to capture the exact failure point.
2. *How do you prevent a single new test from tanking the pass rate gate?* — Apply the gate only to tests tagged `@stable`; newly added tests get `@experimental` tag and are excluded from the gate for 2 weeks.

---

## A10: How do you handle test suite maintainability as the app evolves?

**Answer:** Maintainability requires treating tests as first-class code with the same refactoring disciplines as production code.

**Locator Resilience Strategy:**
```typescript
// ❌ Brittle — breaks on any DOM restructure
page.locator('div.container > ul > li:nth-child(3) > button');

// ✅ Resilient — tied to semantics, not structure
page.getByRole('button', { name: 'Delete Account' });
page.getByTestId('delete-account-btn');  // When semantic locator isn't unique
```

**Automated Stale Test Detection:**
```typescript
// scripts/detect-dead-tests.ts
// Find tests that reference data-testid selectors no longer in the codebase
import { execSync } from 'child_process';
import * as fs from 'fs';
import * as glob from 'glob';

const testFiles = glob.sync('tests/**/*.spec.ts');
const testids = new Set<string>();

for (const file of testFiles) {
  const content = fs.readFileSync(file, 'utf-8');
  const matches = content.matchAll(/getByTestId\(['"]([^'"]+)['"]\)/g);
  for (const match of matches) testids.add(match[1]);
}

// Search for testids in source code
const missing: string[] = [];
for (const id of testids) {
  const result = execSync(`grep -r "data-testid=\\"${id}\\"" src/ --include="*.tsx" -l`, { encoding: 'utf-8' }).trim();
  if (!result) missing.push(id);
}

if (missing.length > 0) {
  console.warn(`Potentially stale test IDs:\n${missing.map(id => `  - ${id}`).join('\n')}`);
}
```

**Dependency Graph for Impact Analysis:**
```typescript
// When a POM changes, identify all tests that use it
// scripts/impact-analysis.ts
// Uses TypeScript compiler API to find all imports of a changed POM file
// Output: set of test files to prioritize for validation
```

**Tricky Follow-ups:**
1. *The design team renames 30 components, breaking 200 tests overnight. What preventive measure would have avoided this?* — A POM layer with `data-testid` conventions means component renames don't affect tests. The missing piece was not using `data-testid` — add an ESLint rule requiring `data-testid` on all interactive elements.
2. *How do you sunset (delete) obsolete tests without losing coverage?* — Use coverage instrumentation (`istanbul`/`c8`) to map which test exercises which code paths. Tests covering zero unique code paths are deletion candidates.

---

*— End of Section 3, A1–A10 —*

> **Next:** Section 3 A11–A20 covering: Test pyramid strategy for microservices, blue-green deployment testing, multi-region testing architecture, observability integration, component vs E2E boundaries, disaster recovery testing, accessibility compliance framework, security testing integration, performance regression pipeline, and framework migration strategy.

---

## A11: How do you structure the test pyramid for a microservices-based FinTech platform?

**Answer:** The classic 70/20/10 pyramid needs recalibration for microservices. The "integration" layer becomes the most valuable tier because service contracts are where most bugs surface.

**Recommended Distribution:**

```
                    ┌─────────────┐
                    │  E2E (5%)   │  ~30 critical user journeys
                    │  UI + API   │  Run: pre-deploy, nightly
                    └──────┬──────┘
               ┌───────────┴───────────┐
               │  Integration (35%)    │  ~200 multi-service flows
               │  API + Contract       │  Run: on merge to main
               └───────────┬───────────┘
          ┌─────────────────┴────────────────┐
          │         Unit (60%)               │  ~1200 isolated functions
          │   Business logic, transformers   │  Run: on every commit
          └──────────────────────────────────┘
```

**Per-Layer Playwright Responsibilities:**

**Contract Layer (most Playwright API work):**
```typescript
// tests/contracts/payments-service.spec.ts
// These run in CI on every PR to the payments service
test.describe('@contract payments-service', () => {
  test('POST /payments/capture returns PaymentSchema', async ({ request }) => {
    const validator = new ContractValidator(request, SERVICES.payments.baseUrl, token);
    await validator.validate('/payments/capture', PaymentSchema, {
      method: 'POST',
      body: { orderId: 'ord-001', amount: 9999, currency: 'USD' },
      expectedStatus: 200,
    });
  });

  // Consumer-driven contract: the frontend declares what shape it needs
  test('GET /payments/:id has all fields frontend consumes', async ({ request }) => {
    // Zod schema is owned by the frontend team, validated against backend response
    const res = await request.get(`${SERVICES.payments.baseUrl}/payments/pay-001`);
    FrontendPaymentSchema.parse(await res.json()); // throws if backend drops a field
  });
});
```

**Integration Layer (multi-service orchestration):**
```typescript
// tests/integration/payment-flow.spec.ts
// Tests the flow: orders-svc → payments-svc → notifications-svc
test('@integration order payment triggers notification', async ({ request }) => {
  const ordersApi   = new ApiClient(request, SERVICES.orders.baseUrl, token);
  const notifApi    = new ApiClient(request, SERVICES.notifications.baseUrl, token);

  const order = await ordersApi.post('/orders', { userId: 'usr-001', items: [] });
  await ordersApi.post(`/orders/${order.id}/pay`, { method: 'card' });

  // Poll notifications service (eventual consistency)
  await expect.poll(
    () => notifApi.get(`/notifications?userId=usr-001&type=payment_success`),
    { timeout: 10_000 }
  ).toMatchObject({ data: expect.arrayContaining([expect.objectContaining({ orderId: order.id })]) });
});
```

**E2E Layer (full UI journeys, minimal count):**
```typescript
// tests/e2e/critical-paths.spec.ts
// Only the 30 paths a user MUST be able to complete for the business to function
const CRITICAL_JOURNEYS = [
  'User can register, verify email, and place first order',
  'User can initiate a wire transfer with 2FA',
  'Admin can freeze and unfreeze a user account',
  // ...
];
```

**Tricky Follow-ups:**
1. *Why not put everything in E2E tests if they give the highest confidence?* — E2E tests are 10–50× slower, have higher flakiness, and don't pinpoint which service broke. A failing contract test tells you exactly which API changed; a failing E2E tells you "something is wrong somewhere."
2. *How do you prevent the test pyramid from inverting over time (too many E2E, too few unit tests)?* — Enforce with a CI gate: if `E2E_count / TOTAL_count > 0.15`, fail the build with a message directing the author to write a contract or unit test instead.

---

## A12: How do you test blue-green and canary deployments?

**Answer:** Blue-green and canary introduce routing complexity — traffic splits mean your tests may hit either version. The strategy is to target specific versions deterministically.

**Blue-Green Testing Strategy:**

```typescript
// config/DeploymentConfig.ts
export type DeploymentSlot = 'blue' | 'green' | 'active';

export function getDeploymentUrl(slot: DeploymentSlot): string {
  const map: Record<DeploymentSlot, string> = {
    blue:   process.env.BLUE_URL   ?? 'https://blue.example.com',
    green:  process.env.GREEN_URL  ?? 'https://green.example.com',
    active: process.env.BASE_URL   ?? 'https://app.example.com',
  };
  return map[slot];
}
```

```typescript
// tests/deployment/smoke-new-slot.spec.ts
// Run against the INACTIVE slot before traffic switch
const newSlot = (process.env.ACTIVE_SLOT === 'blue') ? 'green' : 'blue';

test.use({ baseURL: getDeploymentUrl(newSlot) });

test.describe(`@smoke Smoke: ${newSlot} slot pre-switch`, () => {
  test('login works', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(process.env.SMOKE_EMAIL!);
    await page.getByLabel('Password').fill(process.env.SMOKE_PASSWORD!);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL('/dashboard');
  });

  test('checkout API is healthy', async ({ request }) => {
    const res = await request.get('/api/health');
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.services.checkout).toBe('healthy');
  });
});
```

**Canary Validation — Comparing Canary vs Stable:**
```typescript
// tests/canary/response-parity.spec.ts
// Run the same API calls against both canary and stable; compare responses
test('canary /api/products returns same shape as stable', async ({ request }) => {
  const stableRes  = await request.get(`${process.env.STABLE_URL}/api/products`);
  const canaryRes  = await request.get(`${process.env.CANARY_URL}/api/products`);

  const stableBody = await stableRes.json();
  const canaryBody = await canaryRes.json();

  // Shape must match; values may differ (prices can update)
  expect(Object.keys(canaryBody)).toEqual(Object.keys(stableBody));
  expect(canaryBody.data[0]).toMatchObject({
    id:    expect.any(String),
    name:  expect.any(String),
    price: expect.any(Number),
  });
});
```

**Automated Rollback Trigger:**
```yaml
# .github/workflows/canary-gate.yml
- name: Run canary smoke tests
  run: npx playwright test tests/deployment/smoke-new-slot.spec.ts
  env:
    BASE_URL: ${{ env.CANARY_URL }}
  id: canary_smoke

- name: Rollback canary on failure
  if: failure() && steps.canary_smoke.conclusion == 'failure'
  run: |
    echo "Canary smoke failed — triggering rollback"
    curl -X POST ${{ secrets.DEPLOYMENT_WEBHOOK }}/rollback \
      -H "Authorization: Bearer ${{ secrets.DEPLOY_TOKEN }}"
```

**Tricky Follow-ups:**
1. *How do you test that the traffic split itself is working (10% to canary)?* — Send 1000 API requests from a load script; count how many hit the canary version by checking a `X-Served-By` response header or a version endpoint. Assert the count is within ±3% of 10%.
2. *During a canary rollout, a test flakily hits either version. How do you make tests deterministic?* — Add a `Cookie: canary=true` or `X-Target-Version: canary` header to force consistent routing to one slot during test execution.

---

## A13: How do you architect multi-region testing?

**Answer:** Multi-region testing verifies that geographic distribution (CDN, data residency, latency) works correctly and that region-specific compliance rules are enforced.

**Multi-Region Test Configuration:**
```typescript
// playwright.config.ts — projects per region
import { defineConfig } from '@playwright/test';

const REGIONS = [
  { name: 'us-east',    baseURL: 'https://us-east.app.com',  locale: 'en-US', timezone: 'America/New_York' },
  { name: 'eu-west',    baseURL: 'https://eu-west.app.com',  locale: 'en-GB', timezone: 'Europe/London' },
  { name: 'ap-south',   baseURL: 'https://ap-south.app.com', locale: 'en-IN', timezone: 'Asia/Kolkata' },
];

export default defineConfig({
  projects: REGIONS.map(region => ({
    name: region.name,
    use: {
      baseURL:  region.baseURL,
      locale:   region.locale,
      timezoneId: region.timezone,
      extraHTTPHeaders: {
        'X-Test-Region': region.name,
      },
    },
    testMatch: ['tests/smoke/**', 'tests/regional/**'],
  })),
});
```

**Region-Specific Compliance Tests:**
```typescript
// tests/regional/gdpr.spec.ts
// These run only on EU projects
test.skip(
  !process.env.PLAYWRIGHT_PROJECT?.includes('eu'),
  'GDPR tests only run in EU region'
);

test('cookie consent banner appears on first visit (GDPR)', async ({ page }) => {
  // Clear cookies to simulate first visit
  await page.context().clearCookies();
  await page.goto('/');
  await expect(page.getByTestId('cookie-consent-banner')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Accept All' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Reject Non-Essential' })).toBeVisible();
});

test('user data export available (GDPR Article 20)', async ({ page }) => {
  await page.goto('/settings/privacy');
  await expect(page.getByRole('button', { name: 'Download My Data' })).toBeVisible();
});

test('right to erasure available (GDPR Article 17)', async ({ page }) => {
  await page.goto('/settings/account');
  await expect(page.getByRole('button', { name: 'Delete My Account' })).toBeVisible();
});
```

**Latency Assertions Per Region:**
```typescript
// tests/regional/latency.spec.ts
test('dashboard loads within SLA for region', async ({ page }) => {
  const start = Date.now();
  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');
  const loadTime = Date.now() - start;

  const SLA_MS: Record<string, number> = {
    'us-east': 2000,
    'eu-west': 2500,
    'ap-south': 3500, // Higher latency acceptable for APAC
  };

  const region = process.env.PLAYWRIGHT_PROJECT ?? 'us-east';
  expect(loadTime).toBeLessThan(SLA_MS[region] ?? 3000);
});
```

**Tricky Follow-ups:**
1. *How do you test region-specific data residency (EU data must not leave EU)?* — Intercept all outgoing API requests using `page.route('**')` and assert no request URLs point to non-EU infrastructure (e.g., no `us-east-1.amazonaws.com` hostnames appear).
2. *Time-of-day business rules differ by region (market open/close). How do you test them deterministically?* — Use `timezoneId` in the Playwright project config to fix the browser's timezone, and mock the system time via `page.clock.setFixedTime()` to test specific market hours.

---

## A14: How do you integrate Playwright with observability tools?

**Answer:** Observability integration makes test failures self-explanatory by correlating test events with traces, logs, and metrics in tools like Datadog, Jaeger, or OpenTelemetry.

**Correlation ID Injection:**
```typescript
// fixtures/observability.fixture.ts
import { test as base } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';

type ObsFixtures = { correlationId: string };

export const test = base.extend<ObsFixtures>({
  correlationId: async ({ page }, use, testInfo) => {
    // Generate a unique ID per test for log correlation
    const correlationId = `e2e-${testInfo.testId.slice(0, 8)}-${uuidv4().slice(0, 8)}`;

    // Inject into every outgoing request
    await page.route('**/api/**', async (route) => {
      await route.continue({
        headers: {
          ...await route.request().allHeaders(),
          'X-Correlation-ID': correlationId,
          'X-Test-Name': testInfo.title,
        },
      });
    });

    await use(correlationId);
  },
});
```

**OpenTelemetry Span per Test:**
```typescript
// reporters/OtelReporter.ts
import type { Reporter, TestCase, TestResult } from '@playwright/test/reporter';
import { trace, context, SpanStatusCode } from '@opentelemetry/api';

const tracer = trace.getTracer('playwright-e2e');

export default class OtelReporter implements Reporter {
  private spans = new Map<string, ReturnType<typeof tracer.startSpan>>();

  onTestBegin(test: TestCase): void {
    const span = tracer.startSpan(test.titlePath().join(' > '), {
      attributes: {
        'test.file': test.location.file,
        'test.line': test.location.line,
      },
    });
    this.spans.set(test.id, span);
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const span = this.spans.get(test.id);
    if (!span) return;

    span.setAttributes({
      'test.status':   result.status,
      'test.duration': result.duration,
      'test.retries':  result.retry,
    });

    if (result.status === 'failed') {
      span.setStatus({ code: SpanStatusCode.ERROR, message: result.error?.message?.slice(0, 200) });
    }

    span.end();
    this.spans.delete(test.id);
  }
  printsToStdio(): boolean { return false; }
}
```

**Datadog RUM + Test Correlation:**
```typescript
// In global-setup: tag Datadog RUM sessions with test run ID
await page.addInitScript((runId) => {
  (window as any).DD_RUM?.addRumGlobalContext('test_run_id', runId);
  (window as any).DD_RUM?.addRumGlobalContext('environment', 'e2e');
}, process.env.GITHUB_RUN_ID ?? 'local');
```

**Tricky Follow-ups:**
1. *A test passes but a downstream service logged errors during it. How do you surface that?* — In the test teardown fixture, query the log aggregation API (e.g., Datadog Logs API) for `correlationId`-tagged errors during the test window. If found, attach them to `testInfo` and fail or warn.
2. *How do you avoid polluting production dashboards with e2e test traffic?* — Inject `X-Test-Run: true` header in all test requests; add a Datadog facet filter to exclude this header. Also tag RUM sessions with `env:e2e` which is pre-excluded from production dashboards.

---

## A15: Where is the boundary between component tests and E2E tests?

**Answer:** The boundary is determined by the cost-of-truth ratio — how much real system involvement is needed to get meaningful confidence.

**Decision Framework:**

| Test Type | When to Use | Tool | Speed |
|---|---|---|---|
| Unit | Pure functions, business logic, transformers | Jest/Vitest | ~1ms |
| Component | UI component behavior in isolation | Playwright CT / Storybook | ~100ms |
| API/Contract | Service interface correctness | Playwright API | ~200ms |
| Integration | Multi-service flow correctness | Playwright API | ~2s |
| E2E | Critical user journeys with real browser + all services | Playwright UI | ~15s |

**Playwright Component Testing:**
```typescript
// component-tests/CheckoutButton.spec.tsx
import { test, expect } from '@playwright/experimental-ct-react';
import { CheckoutButton } from './CheckoutButton';

test('shows loading state while processing', async ({ mount }) => {
  const component = await mount(
    <CheckoutButton onCheckout={() => new Promise(r => setTimeout(r, 1000))} />
  );

  await component.getByRole('button').click();
  await expect(component.getByRole('progressbar')).toBeVisible();
  await expect(component.getByRole('button')).toBeDisabled();
});

test('shows error state when checkout fails', async ({ mount }) => {
  const component = await mount(
    <CheckoutButton onCheckout={() => Promise.reject(new Error('Payment declined'))} />
  );

  await component.getByRole('button').click();
  await expect(component.getByRole('alert')).toContainText('Payment declined');
});
```

**Boundary Rule:** Move from component to E2E when you need:
- Real authentication context
- Network responses with side effects (DB writes)
- Multi-service orchestration
- Browser navigation (routing)

**What should NOT be E2E:**
```typescript
// ❌ This is a component test disguised as E2E — too expensive
test('button is disabled when form is invalid', async ({ page }) => {
  await page.goto('/register');
  await expect(page.getByRole('button', { name: 'Submit' })).toBeDisabled();
});

// ✅ Move to component test (10× faster, same confidence)
test('submit button disabled with empty form', async ({ mount }) => {
  const c = await mount(<RegisterForm />);
  await expect(c.getByRole('button', { name: 'Submit' })).toBeDisabled();
});
```

**Tricky Follow-ups:**
1. *The same bug can be caught by a unit test, a component test, and an E2E test. Which do you write?* — Write the unit test (fastest feedback, lowest cost). Only escalate to component if the bug is in rendering/interaction; only to E2E if it requires a real service flow.
2. *Playwright Component Testing doesn't support all frameworks. What do you do for Vue/Svelte?* — Playwright CT supports React, Vue, Svelte, and Solid. For unsupported frameworks, use Vitest browser mode or Cypress Component Testing as alternatives.

---

## A16: How do you design a security testing integration within the Playwright suite?

**Answer:** Security tests in Playwright cover authentication boundary enforcement, authorization (IDOR/BOLA), XSS injection vectors, and CSRF protection — running automatically in CI without requiring a separate DAST pipeline for every deploy.

**Authentication Boundary Tests:**
```typescript
// tests/security/auth-boundaries.spec.ts
const PROTECTED_ROUTES = [
  '/admin/users', '/admin/billing', '/settings/api-keys',
  '/api/users', '/api/orders', '/api/payments/config',
];

test.describe('@security Unauthenticated access denied', () => {
  test.use({ storageState: { cookies: [], origins: [] } }); // No auth

  for (const route of PROTECTED_ROUTES) {
    test(`${route} redirects unauthenticated users`, async ({ page, request }) => {
      if (route.startsWith('/api/')) {
        const res = await request.get(`${process.env.BASE_URL}${route}`);
        expect([401, 403]).toContain(res.status());
      } else {
        await page.goto(route);
        await expect(page).toHaveURL(/\/login|\/403/);
      }
    });
  }
});
```

**IDOR (Insecure Direct Object Reference) Tests:**
```typescript
test('@security user cannot access another user\'s order', async ({ request }) => {
  // Seed two users via API
  const userAToken = await getTokenForRole('user-a');
  const userBToken = await getTokenForRole('user-b');

  // User A creates an order
  const res = await request.post('/api/orders', {
    data: { items: [{ productId: 'prod-1', quantity: 1 }] },
    headers: { Authorization: `Bearer ${userAToken}` },
  });
  const { id: orderIdA } = await res.json();

  // User B attempts to access User A's order
  const idor = await request.get(`/api/orders/${orderIdA}`, {
    headers: { Authorization: `Bearer ${userBToken}` },
  });
  expect([403, 404]).toContain(idor.status()); // Must NOT be 200
});
```

**XSS Input Sanitization Tests:**
```typescript
test('@security XSS payload in profile name is not executed', async ({ page }) => {
  const xssPayload = '<img src=x onerror="window.__xss_triggered=true">';

  // Inject via API (skip UI form validation)
  await request.patch('/api/users/current', {
    data: { name: xssPayload },
    headers: { Authorization: `Bearer ${token}` },
  });

  await page.goto('/profile');

  // Assert the script was NOT executed
  const xssTriggered = await page.evaluate(() => (window as any).__xss_triggered === true);
  expect(xssTriggered).toBe(false);

  // Assert the text is rendered as plain text, not as HTML
  await expect(page.getByTestId('user-name')).toHaveText(xssPayload);
});
```

**Security Headers Validation:**
```typescript
test('@security response has required security headers', async ({ request }) => {
  const res = await request.get('/');
  const headers = res.headers();

  expect(headers['content-security-policy']).toBeDefined();
  expect(headers['x-frame-options']).toBe('DENY');
  expect(headers['x-content-type-options']).toBe('nosniff');
  expect(headers['strict-transport-security']).toMatch(/max-age=\d+/);
  expect(headers['referrer-policy']).toBeDefined();
});
```

**Tricky Follow-ups:**
1. *Should Playwright replace a dedicated DAST tool like OWASP ZAP or Burp Suite?* — No. Playwright security tests cover known paths (authentication, authorization, injection). ZAP/Burp do fuzzing and unknown-path discovery. Use both: Playwright for fast per-PR checks, ZAP in a weekly scheduled scan.
2. *How do you prevent security tests from creating real vulnerabilities during testing?* — Use dedicated test user accounts with no access to real financial data; run on an isolated environment; ensure `SEED_TOKEN` used to bypass auth has no production access and rotates every 24 hours.

---

## A17: How do you build a performance regression detection pipeline?

**Answer:** Performance regression detection requires a baseline, a measurement, and a statistically meaningful comparison — not just point-in-time assertions.

**Baseline Storage:**
```typescript
// scripts/capture-baseline.ts
// Run on the main branch after every successful deploy; store in Redis/S3
import { chromium } from '@playwright/test';
import * as fs from 'fs';

const PAGES_TO_MEASURE = [
  { name: 'dashboard',  path: '/dashboard' },
  { name: 'products',   path: '/products' },
  { name: 'checkout',   path: '/checkout' },
];

async function captureBaseline() {
  const browser = await chromium.launch();
  const results: Record<string, { lcp: number; fcp: number; tbt: number }> = {};

  for (const { name, path } of PAGES_TO_MEASURE) {
    // Take 3 measurements and use median
    const measurements: number[] = [];
    for (let i = 0; i < 3; i++) {
      const page = await browser.newPage();
      await page.goto(path);
      const lcp = await page.evaluate(() =>
        new Promise<number>(resolve => {
          new PerformanceObserver(list => resolve(list.getEntries().at(-1)?.startTime ?? 0))
            .observe({ type: 'largest-contentful-paint', buffered: true });
          setTimeout(() => resolve(0), 3000);
        })
      );
      measurements.push(lcp);
      await page.close();
    }

    const sorted = [...measurements].sort((a, b) => a - b);
    results[name] = {
      lcp: sorted[Math.floor(sorted.length / 2)], // median
      fcp: 0, // similar collection
      tbt: 0,
    };
  }

  await browser.close();
  fs.writeFileSync('perf-baseline.json', JSON.stringify(results, null, 2));
  console.log('Baseline captured:', results);
}

captureBaseline();
```

**Regression Detection in PR Tests:**
```typescript
// tests/performance/regression.spec.ts
import * as fs from 'fs';

const REGRESSION_THRESHOLD = 0.15; // 15% degradation triggers failure
const baseline = JSON.parse(fs.readFileSync('perf-baseline.json', 'utf-8'));

for (const [pageName, baselineMetrics] of Object.entries(baseline) as [string, any][]) {
  test(`@performance ${pageName} LCP within 15% of baseline`, async ({ page }, testInfo) => {
    await page.goto(`/${pageName}`);

    const lcp = await page.evaluate(() =>
      new Promise<number>(resolve => {
        new PerformanceObserver(list => resolve(list.getEntries().at(-1)?.startTime ?? 0))
          .observe({ type: 'largest-contentful-paint', buffered: true });
        setTimeout(() => resolve(0), 5000);
      })
    );

    await testInfo.attach('lcp-measurement', {
      body: JSON.stringify({ page: pageName, baseline: baselineMetrics.lcp, actual: lcp }),
      contentType: 'application/json',
    });

    const regression = (lcp - baselineMetrics.lcp) / baselineMetrics.lcp;
    expect(regression, `LCP regressed by ${(regression * 100).toFixed(1)}%`).toBeLessThan(REGRESSION_THRESHOLD);
  });
}
```

**Trend Visualization (CI Artifact):**
```yaml
# Post perf results to a time-series database for dashboard
- name: Post performance metrics
  if: github.ref == 'refs/heads/main'
  run: node scripts/post-metrics-to-datadog.js perf-results.json
  env:
    DD_API_KEY: ${{ secrets.DATADOG_API_KEY }}
    GITHUB_SHA: ${{ github.sha }}
```

**Tricky Follow-ups:**
1. *LCP varies by ±200ms due to CI machine variance. How do you avoid false positives?* — Use median of 5 runs (not single measurement), and set the regression threshold at 20% rather than 5%. Alternatively, compare P95 against the rolling 7-day P95 baseline to filter machine variance.
2. *The dashboard team ships a legitimate 400ms LCP improvement. How does the baseline update?* — Use an auto-update script that runs on main-branch deploys: if performance *improves* by >10%, automatically update the baseline file and commit it back via a bot PR.

---

## A18: How do you migrate an existing Selenium/Cypress suite to Playwright?

**Answer:** Migration is a gradual parallel-run strategy — never a big-bang rewrite. The goal is to build confidence in Playwright while reducing Selenium/Cypress in phases.

**Phase 1 — Audit and Prioritize (Week 1–2):**
```typescript
// scripts/audit-test-suite.ts
// Categorize tests by migration difficulty

type TestCategory = 'simple' | 'moderate' | 'complex';

interface TestAudit {
  file: string;
  category: TestCategory;
  blockers: string[];
}

function categorize(content: string): { category: TestCategory; blockers: string[] } {
  const blockers: string[] = [];

  if (content.includes('executeScript'))    blockers.push('Uses executeScript (→ page.evaluate)');
  if (content.includes('Actions().'))      blockers.push('Uses Selenium Actions (→ page.mouse)');
  if (content.includes('cy.intercept'))    blockers.push('Uses cy.intercept (→ page.route)');
  if (content.includes('iframe'))          blockers.push('iframe interaction needed');
  if (content.includes('file://'))         blockers.push('Uses file:// protocol');

  const category: TestCategory =
    blockers.length === 0   ? 'simple' :
    blockers.length <= 2    ? 'moderate' :
    'complex';

  return { category, blockers };
}
```

**Phase 2 — Run Both in Parallel (Month 1–3):**
```yaml
# CI: run both suites, compare results
jobs:
  selenium:  { runs-on: ubuntu-latest, steps: [{ run: mvn test }] }
  playwright:{ runs-on: ubuntu-latest, steps: [{ run: npx playwright test }] }

  compare:
    needs: [selenium, playwright]
    runs-on: ubuntu-latest
    steps:
      - run: node scripts/compare-results.js selenium-results.xml playwright-results.json
        # Fails if Playwright has MORE failures than Selenium baseline
```

**Phase 3 — Migration Mapping (Selenium → Playwright):**
```typescript
// Selenium → Playwright equivalence reference
const MIGRATION_MAP = {
  // Locators
  'By.id("x")':              'page.locator("#x")          → page.getByTestId("x")',
  'By.cssSelector(".x")':    'page.locator(".x")          → prefer semantic',
  'By.xpath("//button")':    'page.locator("//button")    → page.getByRole("button")',

  // Waits
  'Thread.sleep(2000)':      'REMOVE                      → Playwright auto-waits',
  'WebDriverWait(15)':       'REMOVE                      → built-in actionTimeout',
  'wait.until(visible)':     'REMOVE                      → auto-wait on locator',

  // Actions
  'driver.get(url)':         'await page.goto(url)',
  'element.click()':         'await locator.click()',
  'element.sendKeys("x")':   'await locator.fill("x")',
  'driver.switchTo().frame': 'page.frameLocator(selector)',

  // Assertions
  'assertEquals(text, el)':  'await expect(locator).toHaveText(text)',
  'assertTrue(el.displayed)':'await expect(locator).toBeVisible()',
};
```

**Phase 4 — Decommission (Month 4+):**
```typescript
// Track migration progress automatically
// scripts/track-migration.ts
const seleniumCount = parseInt(execSync('find . -name "*.java" | wc -l').toString().trim());
const playwrightCount = parseInt(execSync('find . -name "*.spec.ts" | wc -l').toString().trim());
const progress = (playwrightCount / (seleniumCount + playwrightCount) * 100).toFixed(0);
console.log(`Migration: ${progress}% complete (${playwrightCount} PW / ${seleniumCount} Selenium)`);
```

**Tricky Follow-ups:**
1. *Cypress custom commands are deeply embedded in 400 tests. How do you migrate them?* — Map each `cy.customCommand()` to a Playwright fixture or utility function. Create a `cy-compat.ts` shim that re-exports them under the same name as Playwright-native calls, so test files need minimal changes.
2. *Engineers resist migration because they know Cypress well. How do you handle the change?* — Run a Playwright workshop; show side-by-side that common patterns are less code in Playwright (auto-waiting, multi-tab, network mocking). Let experienced engineers write the base fixtures — give them ownership of the framework.

---

## A19: How do you implement accessibility compliance testing at CI scale?

**Answer:** Accessibility testing at CI scale requires four layers: automated static analysis, dynamic runtime checks, manual audit triggers, and regression prevention.

**Layer 1 — Per-Page Automated Checks (fast, runs every PR):**
```typescript
// tests/accessibility/wcag-compliance.spec.ts
import AxeBuilder from '@axe-core/playwright';

const PAGES_TO_CHECK = [
  { path: '/login',     name: 'Login', role: null },
  { path: '/dashboard', name: 'Dashboard', role: 'user' },
  { path: '/checkout',  name: 'Checkout',  role: 'user' },
  { path: '/admin/users', name: 'Admin Users', role: 'admin' },
];

for (const { path, name, role } of PAGES_TO_CHECK) {
  test(`@a11y WCAG 2.1 AA: ${name}`, async ({ browser }) => {
    const storageState = role ? `auth/${role}.json` : undefined;
    const ctx = await browser.newContext({ storageState });
    const page = await ctx.newPage();

    await page.goto(path);
    await page.waitForLoadState('networkidle');

    const { violations } = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .disableRules(['color-contrast']) // Tracked separately in design system
      .analyze();

    const serious = violations.filter(v =>
      ['critical', 'serious'].includes(v.impact ?? '')
    );

    if (serious.length > 0) {
      const report = serious.map(v =>
        `[${v.impact}] ${v.id}: ${v.description}\n${v.nodes.slice(0, 2).map(n => `  ${n.html.slice(0, 100)}`).join('\n')}`
      ).join('\n\n');
      throw new Error(`${serious.length} accessibility violation(s) on ${name}:\n\n${report}`);
    }

    await ctx.close();
  });
}
```

**Layer 2 — Component-Level Accessibility (with Playwright CT):**
```typescript
// component-tests/FormField.a11y.spec.tsx
test('form field has proper label association', async ({ mount }) => {
  const component = await mount(<FormField label="Email Address" type="email" />);

  // Every input must be associated with its label
  const input = component.getByRole('textbox', { name: 'Email Address' });
  await expect(input).toBeVisible();

  // Must be keyboard-navigable
  await component.locator('body').press('Tab');
  await expect(input).toBeFocused();
});
```

**Layer 3 — Regression Lock (prevent regressions, allow improvements):**
```typescript
// scripts/a11y-gate.ts
// Compare current violation count against baseline
const baseline = JSON.parse(fs.readFileSync('a11y-baseline.json', 'utf-8'));
const current  = JSON.parse(fs.readFileSync('a11y-results.json', 'utf-8'));

const newViolations = current.violations.filter(
  (v: any) => !baseline.violations.find((b: any) => b.id === v.id && b.page === v.page)
);

if (newViolations.length > 0) {
  console.error(`${newViolations.length} NEW accessibility violations introduced:\n${
    newViolations.map((v: any) => `  - [${v.impact}] ${v.id} on ${v.page}`).join('\n')
  }`);
  process.exit(1);
}
console.log('No new accessibility regressions.');
```

**Layer 4 — Keyboard Navigation Smoke Test:**
```typescript
test('@a11y keyboard navigation: complete checkout without mouse', async ({ page }) => {
  await page.goto('/checkout');

  // Navigate entirely by keyboard
  await page.keyboard.press('Tab'); // First Name
  await page.keyboard.type('Alice');
  await page.keyboard.press('Tab'); // Last Name
  await page.keyboard.type('Chen');
  await page.keyboard.press('Tab'); // Email
  await page.keyboard.type('alice@example.com');

  // Tab to submit and press Enter
  let tabCount = 0;
  while (tabCount < 20) {
    const focused = await page.evaluate(() => document.activeElement?.textContent ?? '');
    if (focused.includes('Place Order')) break;
    await page.keyboard.press('Tab');
    tabCount++;
  }
  await page.keyboard.press('Enter');

  await expect(page).toHaveURL(/\/orders\/confirmation/, { timeout: 15_000 });
});
```

**Tricky Follow-ups:**
1. *axe-core reports 0 violations but a screen reader user still can't use the feature. Why?* — axe-core detects ~57% of WCAG issues automatically. Issues like poor reading order, illogical focus management, and confusing live region announcements require manual screen reader testing (NVDA + Firefox, VoiceOver + Safari).
2. *Color contrast violations exist because the design team committed to the brand palette. How do you handle this?* — Document the accepted deviation in an `a11y-exceptions.json` file, reference it in the axe `disableRules` call, and require design lead sign-off to update the exception list. Track exceptions in a Jira epic as tech debt.

---

## A20: How do you design the overall QA strategy for continuous deployment (50 deploys/day)?

**Answer:** At 50 deploys/day, traditional "gate everything" fails — you need a risk-tiered pipeline that runs proportionally to the risk of each change.

**Risk-Tiered Pipeline:**

```
┌─────────────────────────────────────────────────────┐
│ Every commit (< 2 min)                               │
│  • Type check + lint                                 │
│  • Unit tests (Jest)                                 │
│  • API health check (1 request per service)          │
└──────────────────────┬──────────────────────────────┘
                       │ Pass
┌──────────────────────▼──────────────────────────────┐
│ Pre-deploy smoke (< 5 min)                           │
│  • 30 @smoke Playwright tests (login, checkout, API) │
│  • Run on new slot BEFORE traffic switch             │
└──────────────────────┬──────────────────────────────┘
                       │ Pass → switch traffic
┌──────────────────────▼──────────────────────────────┐
│ Post-deploy canary validation (< 10 min)             │
│  • Contract tests vs live canary                     │
│  • 5-minute error rate monitoring (Datadog alert)    │
│  • Auto-rollback if error rate > 0.1%                │
└──────────────────────┬──────────────────────────────┘
                       │ Pass
┌──────────────────────▼──────────────────────────────┐
│ Async full regression (< 30 min, non-blocking)       │
│  • All 600 Playwright tests (sharded × 8)            │
│  • Failure → alert team + block NEXT deploy          │
│  • Does NOT roll back current deploy                 │
└─────────────────────────────────────────────────────┘
```

**Risk Scoring for Test Selection:**
```typescript
// scripts/select-tests-by-risk.ts
// Use git diff to score which test files to run
import { execSync } from 'child_process';

const changedFiles = execSync('git diff --name-only HEAD~1 HEAD')
  .toString().trim().split('\n');

const riskScore = changedFiles.reduce((score, file) => {
  if (file.includes('payment'))   return score + 10; // High risk
  if (file.includes('auth'))      return score + 8;
  if (file.includes('api/'))      return score + 5;
  if (file.includes('components'))return score + 3;
  if (file.includes('styles'))    return score + 1;
  return score + 2;
}, 0);

const testScope =
  riskScore >= 20 ? 'all' :
  riskScore >= 10 ? 'integration' :
  'smoke';

console.log(`Risk score: ${riskScore} → running: ${testScope}`);
execSync(`npx playwright test --grep @${testScope}`, { stdio: 'inherit' });
```

**Playwright Config for Continuous Deployment:**
```typescript
// playwright.config.ts
export default defineConfig({
  // Never retry in smoke — fail fast, rollback fast
  retries: process.env.SUITE === 'smoke' ? 0 : 1,

  // Short timeouts in smoke — smoke tests MUST be fast
  timeout: process.env.SUITE === 'smoke'
    ? 20_000   // 20s per test
    : 45_000,  // 45s in full regression

  use: {
    // Smoke: no trace (speed); regression: trace on retry (debuggability)
    trace: process.env.SUITE === 'smoke' ? 'off' : 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  // Smoke: sequential to reduce resource burst on new slot
  // Regression: parallel for speed
  workers: process.env.SUITE === 'smoke' ? 2 : '50%',
});
```

**Tricky Follow-ups:**
1. *A smoke test itself is flaky and causes false rollbacks. How do you handle this?* — Smoke tests must be held to a higher reliability standard than regression tests. They run zero retries — if they flake, they get fixed immediately (P0 task). Maintain a dashboard tracking smoke test flakiness rate; any test flaking >1% in 30 days is automatically quarantined.
2. *How do you distinguish "this deploy broke it" vs "it was already broken before this deploy"?* — Always run the smoke suite against the CURRENT stable slot AND the new slot simultaneously before switching traffic. If both fail, it was pre-existing; if only the new slot fails, the deploy introduced it.

---

*— End of Section 3, A11–A20 — Section 3 Complete —*

> **Next:** Section 4 — UI + API Integration Scenarios (20 scenarios)

---

---

# SECTION 4 — UI + API INTEGRATION SCENARIOS (20)

---

## S1: User Registration → Email Verification → First Login

**Scenario:** A new user registers via the UI, receives a verification email, clicks the link, and completes their first authenticated session.

**Difficulty:** Hard

**Challenge:** The verification token exists only in the email — tests must retrieve it without a real inbox.

**Solution:**
```typescript
import { test, expect } from '@playwright/test';

test('full registration and email verification flow', async ({ page, request }) => {
  const email = `reg-${Date.now()}@test.example.com`;

  // Step 1: Register via UI
  await page.goto('/register');
  await page.getByLabel('Full Name').fill('Alice Chen');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill('SecurePass123!');
  await page.getByLabel('Confirm Password').fill('SecurePass123!');
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page.getByTestId('verify-email-banner')).toContainText(
    'Check your inbox to verify your email'
  );

  // Step 2: Retrieve verification token via Mailpit test API
  // (Mailpit runs as a local mail catcher in CI)
  let verificationUrl = '';
  await expect.poll(async () => {
    const mailRes = await request.get(
      `http://localhost:8025/api/v1/search?query=${encodeURIComponent(email)}`
    );
    const { messages } = await mailRes.json();
    if (!messages?.length) return false;
    const body: string = messages[0].Text ?? messages[0].HTML ?? '';
    const match = body.match(/https?:\/\/[^\s"]+verify[^\s"]+/);
    if (match) { verificationUrl = match[0]; return true; }
    return false;
  }, { timeout: 15_000, intervals: [1000, 2000], message: 'Verification email not received' }).toBe(true);

  // Step 3: Visit verification link
  await page.goto(verificationUrl);
  await expect(page.getByText('Email verified!')).toBeVisible();

  // Step 4: Log in with verified credentials
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill('SecurePass123!');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL('/dashboard');
  await expect(page.getByTestId('welcome-message')).toContainText('Alice Chen');

  // Step 5: Verify via API that the account is fully active
  const profileRes = await request.get('/api/users/current', {
    headers: { Authorization: `Bearer ${await page.evaluate(() => localStorage.getItem('apiToken'))}` },
  });
  const profile = await profileRes.json();
  expect(profile.verified).toBe(true);
  expect(profile.status).toBe('active');
});
```

**Key Techniques:**
- `expect.poll()` for eventually-delivered email
- Mailpit/Mailhog API to extract real tokens without mocking
- Full loop: UI → backend → email → UI → API assertion
- Cleanup: register with a unique timestamped email so no teardown is needed

**Common Mistakes:**
- Mocking the email delivery — this skips the actual token generation/storage path
- Using `page.waitForTimeout(5000)` instead of polling the mail API

---

## S2: Shopping Cart Persistence Across Sessions

**Scenario:** A user adds items to a cart, closes the browser, reopens it, and verifies the cart is still populated — testing both UI persistence and the cart API state.

**Difficulty:** Medium

**Solution:**
```typescript
test('cart persists across browser sessions', async ({ browser, request }) => {
  // Session 1: Add items
  const context1 = await browser.newContext({ storageState: 'auth/user.json' });
  const page1 = await context1.newPage();

  await page1.goto('/products');
  await page1.getByText('MacBook Pro 14"').click();
  await page1.getByRole('button', { name: 'Add to Cart' }).click();
  await page1.getByText('USB-C Hub').click();
  await page1.getByRole('button', { name: 'Add to Cart' }).click();

  const cartCount = await page1.getByTestId('cart-count').innerText();
  expect(cartCount).toBe('2');

  // Verify cart saved via API
  const token = await page1.evaluate(() => localStorage.getItem('apiToken'));
  await context1.close(); // "Close the browser"

  // Step 2: Verify API state persisted
  const apiRes = await request.get('/api/cart', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const cart = await apiRes.json();
  expect(cart.items).toHaveLength(2);
  expect(cart.items.map((i: any) => i.name)).toContain('MacBook Pro 14"');

  // Session 2: Reopen browser — cart should still be there
  const context2 = await browser.newContext({ storageState: 'auth/user.json' });
  const page2 = await context2.newPage();

  await page2.goto('/cart');

  await expect(page2.getByTestId('cart-item')).toHaveCount(2);
  await expect(page2.getByText('MacBook Pro 14"')).toBeVisible();
  await expect(page2.getByText('USB-C Hub')).toBeVisible();

  // Total price matches API
  const subtotal = await page2.getByTestId('cart-subtotal').innerText();
  const uiTotal = parseFloat(subtotal.replace(/[^0-9.]/g, ''));
  expect(uiTotal).toBeCloseTo(cart.total, 0);

  await context2.close();
});
```

---

## S3: Concurrent Cart Modification (Race Condition Test)

**Scenario:** A user has the same account open in two tabs. Both tabs add different items simultaneously. Verify the final cart contains both items and the quantity is correct — no lost updates.

**Difficulty:** Hard

**Solution:**
```typescript
test('concurrent cart updates from two tabs do not lose data', async ({ context }) => {
  // Both tabs share the same auth context (same user session)
  const tab1 = await context.newPage();
  const tab2 = await context.newPage();

  await tab1.goto('/products/prod-001'); // MacBook
  await tab2.goto('/products/prod-002'); // iPad

  // Add from both tabs simultaneously
  await Promise.all([
    tab1.getByRole('button', { name: 'Add to Cart' }).click(),
    tab2.getByRole('button', { name: 'Add to Cart' }).click(),
  ]);

  // Wait for both confirmations
  await Promise.all([
    expect(tab1.getByTestId('cart-toast')).toBeVisible(),
    expect(tab2.getByTestId('cart-toast')).toBeVisible(),
  ]);

  // Navigate either tab to cart; verify both items present
  await tab1.goto('/cart');
  await tab1.waitForLoadState('networkidle');

  const items = await tab1.getByTestId('cart-item').allInnerTexts();
  expect(items.length).toBeGreaterThanOrEqual(2);

  const itemNames = items.join(' ');
  expect(itemNames).toContain('MacBook');
  expect(itemNames).toContain('iPad');

  // Verify via API — both items must exist, no quantity collision
  const token = await tab1.evaluate(() => localStorage.getItem('apiToken'));
  const apiCart = await context.request.get('/api/cart', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const cart = await apiCart.json();
  const productIds = cart.items.map((i: any) => i.productId);
  expect(productIds).toContain('prod-001');
  expect(productIds).toContain('prod-002');
});
```

---

## S4: Password Reset Full Flow

**Scenario:** User requests a password reset, receives email with token, resets password, verifies old password rejected and new password works, and API tokens are invalidated.

**Difficulty:** Hard

**Solution:**
```typescript
test('password reset invalidates old sessions and enables new login', async ({ page, browser, request }) => {
  const email = 'resettest@example.com';
  const oldPassword = 'OldPass123!';
  const newPassword = 'NewSecurePass456!';

  // Pre-condition: user exists and has a valid token
  const loginRes = await request.post('/api/auth/login', {
    data: { email, password: oldPassword },
  });
  const { accessToken: oldToken } = await loginRes.json();

  // Step 1: Request password reset via UI
  await page.goto('/forgot-password');
  await page.getByLabel('Email').fill(email);
  await page.getByRole('button', { name: 'Send Reset Link' }).click();
  await expect(page.getByText('Check your email')).toBeVisible();

  // Step 2: Get reset token from Mailpit
  let resetUrl = '';
  await expect.poll(async () => {
    const res = await request.get(
      `http://localhost:8025/api/v1/search?query=subject:Password+Reset+to:${email}`
    );
    const { messages } = await res.json();
    const body = messages?.[0]?.Text ?? '';
    const match = body.match(/https?:\/\/[^\s"]+reset[^\s"]+/);
    if (match) { resetUrl = match[0]; return true; }
    return false;
  }, { timeout: 15_000 }).toBe(true);

  // Step 3: Set new password
  await page.goto(resetUrl);
  await page.getByLabel('New Password').fill(newPassword);
  await page.getByLabel('Confirm New Password').fill(newPassword);
  await page.getByRole('button', { name: 'Reset Password' }).click();
  await expect(page.getByText('Password updated successfully')).toBeVisible();

  // Step 4: Old token must be revoked
  const oldTokenCheck = await request.get('/api/users/current', {
    headers: { Authorization: `Bearer ${oldToken}` },
  });
  expect(oldTokenCheck.status()).toBe(401);

  // Step 5: Old password rejected
  const oldLoginAttempt = await request.post('/api/auth/login', {
    data: { email, password: oldPassword },
  });
  expect(oldLoginAttempt.status()).toBe(401);

  // Step 6: New password works
  const newLoginContext = await browser.newContext();
  const newPage = await newLoginContext.newPage();
  await newPage.goto('/login');
  await newPage.getByLabel('Email').fill(email);
  await newPage.getByLabel('Password').fill(newPassword);
  await newPage.getByRole('button', { name: 'Sign in' }).click();
  await expect(newPage).toHaveURL('/dashboard');
  await newLoginContext.close();
});
```

---

## S5: Real-Time Notification via WebSocket + UI Verification

**Scenario:** Admin sends a broadcast notification. All logged-in users receive it in real time via WebSocket. Test that UI shows the notification within 3 seconds without page refresh.

**Difficulty:** Hard

**Solution:**
```typescript
test('admin broadcast notification appears in real time for all users', async ({ browser }) => {
  // Create two sessions: admin and user
  const adminCtx = await browser.newContext({ storageState: 'auth/admin.json' });
  const userCtx  = await browser.newContext({ storageState: 'auth/user.json' });

  const adminPage = await adminCtx.newPage();
  const userPage  = await userCtx.newPage();

  // User navigates to dashboard (WS connection established)
  await userPage.goto('/dashboard');
  await userPage.waitForLoadState('networkidle');

  // Verify WS connection is open
  const wsConnected = await userPage.evaluate(() =>
    (window as any).__wsStatus === 'connected'
  );
  expect(wsConnected).toBe(true);

  // Admin sends broadcast via UI
  await adminPage.goto('/admin/notifications');
  await adminPage.getByLabel('Message').fill('System maintenance at 11 PM tonight');
  await adminPage.getByRole('button', { name: 'Send to All Users' }).click();
  await expect(adminPage.getByText('Notification sent')).toBeVisible();

  // Assert user receives notification within 3s without refresh
  await expect(
    userPage.getByTestId('notification-banner'),
    'Notification should appear without page refresh'
  ).toBeVisible({ timeout: 3000 });

  await expect(userPage.getByTestId('notification-banner'))
    .toContainText('System maintenance at 11 PM tonight');

  // Verify notification count badge incremented
  await expect(userPage.getByTestId('notification-count')).toHaveText('1');

  // Verify via API that notification is marked as delivered
  const token = await userPage.evaluate(() => localStorage.getItem('apiToken'));
  const notifRes = await userCtx.request.get('/api/notifications', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const { notifications } = await notifRes.json();
  expect(notifications[0].message).toBe('System maintenance at 11 PM tonight');
  expect(notifications[0].delivered).toBe(true);

  await adminCtx.close();
  await userCtx.close();
});
```

---

## S6: File Upload → Processing → Download Round-Trip

**Scenario:** User uploads a CSV file of contacts, the system processes it asynchronously, and the user downloads the processed/cleaned result file.

**Difficulty:** Medium

**Solution:**
```typescript
import * as fs from 'fs';
import * as path from 'path';

test('CSV upload processed and result downloadable', async ({ page, request }) => {
  // Prepare test CSV
  const csvContent = [
    'name,email,phone',
    'Alice Chen,alice@example.com,+14155551234',
    'Bob Smith,INVALID_EMAIL,555-1234',
    'Carol Jones,carol@example.com,+16505559876',
  ].join('\n');

  const csvPath = path.join(process.cwd(), 'test-fixtures', 'contacts-upload.csv');
  fs.writeFileSync(csvPath, csvContent);

  // Step 1: Upload via UI
  await page.goto('/contacts/import');
  await page.getByLabel('Upload CSV').setInputFiles(csvPath);
  await page.getByRole('button', { name: 'Import Contacts' }).click();

  // Step 2: Poll for processing completion
  await expect(page.getByTestId('import-status')).toHaveText('Processing...', { timeout: 5000 });

  await expect.poll(
    () => page.getByTestId('import-status').innerText(),
    { timeout: 30_000, intervals: [1000, 2000] }
  ).toBe('Complete');

  // Step 3: Verify summary
  await expect(page.getByTestId('import-added')).toHaveText('2');      // Valid rows
  await expect(page.getByTestId('import-rejected')).toHaveText('1');   // Invalid email

  // Step 4: Download results report
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Download Results' }).click(),
  ]);

  const resultPath = path.join(process.cwd(), 'test-downloads', download.suggestedFilename());
  await download.saveAs(resultPath);

  // Step 5: Assert CSV content
  const result = fs.readFileSync(resultPath, 'utf-8');
  const lines = result.split('\n').filter(Boolean);

  expect(lines[0]).toContain('status'); // Has status column in output
  expect(result).toContain('alice@example.com');
  expect(result).toContain('carol@example.com');
  expect(result).toContain('INVALID_EMAIL'); // Present with "rejected" status
  expect(result).toContain('rejected');

  // Step 6: Verify contacts appear in the contacts list via API
  const token = await page.evaluate(() => localStorage.getItem('apiToken'));
  const contactsRes = await request.get('/api/contacts?email=alice@example.com', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const { contacts } = await contactsRes.json();
  expect(contacts).toHaveLength(1);
  expect(contacts[0].name).toBe('Alice Chen');

  fs.unlinkSync(csvPath);
});
```

---

## S7: Stripe Checkout → Webhook → Order Status Update

**Scenario:** User completes a Stripe checkout. Stripe fires a `payment_intent.succeeded` webhook. The backend updates order status. Test verifies the status change propagates to the UI without refresh.

**Difficulty:** Hard

**Solution:**
```typescript
test('stripe payment triggers webhook and updates order status', async ({ page, request }) => {
  await page.goto('/checkout');

  // Fill cart (pre-seeded via API for speed)
  const token = await page.evaluate(() => localStorage.getItem('apiToken'));
  await request.post('/api/cart/items', {
    data: { productId: 'prod-001', quantity: 1 },
    headers: { Authorization: `Bearer ${token}` },
  });
  await page.reload();

  // Fill shipping
  await page.getByLabel('Full Name').fill('Test User');
  await page.getByLabel('Address').fill('123 Test St');
  await page.getByRole('button', { name: 'Continue to Payment' }).click();

  // Fill Stripe test card (4242... = immediate success)
  const stripeFrame = page.frameLocator('iframe[src*="stripe.com"]');
  await stripeFrame.getByLabel('Card number').fill('4242424242424242');
  await stripeFrame.getByLabel('Expiration').fill('12/30');
  await stripeFrame.getByLabel('CVC').fill('123');

  // Capture the order ID before payment
  await page.getByRole('button', { name: /Pay/ }).click();

  // Wait for redirect to confirmation page
  await expect(page).toHaveURL(/\/orders\/ord-/, { timeout: 20_000 });
  const orderId = page.url().split('/orders/')[1];

  // Initial status: pending or processing
  await expect(page.getByTestId('order-status')).toHaveText(/pending|processing/i);

  // The webhook fires asynchronously — stripe-cli forwards it locally in dev
  // In CI: use Stripe's test webhook simulation
  await request.post('/api/test/stripe/simulate-webhook', {
    data: {
      type: 'payment_intent.succeeded',
      orderId,
    },
    headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
  });

  // Status should update on the page without reload (via WS or polling)
  await expect(
    page.getByTestId('order-status'),
    'Order status should update to confirmed after webhook'
  ).toHaveText('Confirmed', { timeout: 10_000 });

  // API verification
  const orderRes = await request.get(`/api/orders/${orderId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const order = await orderRes.json();
  expect(order.status).toBe('confirmed');
  expect(order.paymentStatus).toBe('captured');
});
```

---

## S8: Role Permission Change Takes Effect Immediately

**Scenario:** Admin demotes a user from `manager` to `readonly`. The user's current session should immediately lose manager-level capabilities without requiring re-login.

**Difficulty:** Hard

**Solution:**
```typescript
test('permission downgrade takes effect in active session', async ({ browser, request }) => {
  // Seed a manager user
  const seedRes = await request.post('/api/test/users', {
    data: { email: `mgr-${Date.now()}@test.com`, role: 'manager', name: 'Test Manager' },
    headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
  });
  const { id: userId, email } = await seedRes.json();

  const adminCtx   = await browser.newContext({ storageState: 'auth/admin.json' });
  const managerCtx = await browser.newContext();

  try {
    // Manager logs in
    const managerPage = await managerCtx.newPage();
    await managerPage.goto('/login');
    await managerPage.getByLabel('Email').fill(email);
    await managerPage.getByLabel('Password').fill('TestPass123!');
    await managerPage.getByRole('button', { name: 'Sign in' }).click();
    await expect(managerPage).toHaveURL('/dashboard');

    // Verify manager sees "Approve" button (manager capability)
    await managerPage.goto('/approvals');
    await expect(managerPage.getByRole('button', { name: 'Approve' })).toBeVisible();

    // Admin demotes manager to readonly in parallel
    const adminPage = await adminCtx.newPage();
    await adminPage.goto(`/admin/users/${userId}`);
    await adminPage.getByLabel('Role').selectOption('readonly');
    await adminPage.getByRole('button', { name: 'Save Changes' }).click();
    await expect(adminPage.getByText('User updated')).toBeVisible();

    // Manager's active session: navigate and verify capabilities removed
    await managerPage.reload(); // Simulates next navigation

    await expect(
      managerPage.getByRole('button', { name: 'Approve' }),
      'Approve button should be gone after role downgrade'
    ).not.toBeVisible({ timeout: 5000 });

    // API call with the manager's token should return 403
    const mgrToken = await managerPage.evaluate(() => localStorage.getItem('apiToken'));
    const approveAttempt = await request.post(`/api/approvals/approve-001`, {
      headers: { Authorization: `Bearer ${mgrToken}` },
    });
    expect(approveAttempt.status()).toBe(403);

  } finally {
    await adminCtx.close();
    await managerCtx.close();
    await request.delete(`/api/test/users/${userId}`, {
      headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
    });
  }
});
```

---

## S9: Search with Debounce + API Validation

**Scenario:** The search box debounces user input (500ms) before sending an API request. Test that: (1) only one API request fires per search, (2) results displayed match the API response, (3) empty results show correct UI state.

**Difficulty:** Medium

**Solution:**
```typescript
test('search debounce fires single API request and displays correct results', async ({ page }) => {
  await page.goto('/products');

  // Capture all search API requests made during typing
  const searchRequests: string[] = [];
  page.on('request', (req) => {
    if (req.url().includes('/api/search')) {
      searchRequests.push(req.url());
    }
  });

  // Type quickly — only the final term should trigger the API call
  const searchBox = page.getByRole('searchbox', { name: 'Search products' });
  await searchBox.click();
  await searchBox.type('mac', { delay: 50 }); // Fast typing, within debounce window

  // Wait for the debounce to fire and response to arrive
  const [response] = await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/search?q=mac')),
    page.waitForTimeout(600), // Just past the 500ms debounce
  ]);

  // Exactly ONE API request should have been made (debounce worked)
  expect(searchRequests.filter(u => u.includes('q=mac'))).toHaveLength(1);

  const apiResults = await response.json();

  // UI results must match API results exactly
  const uiResults = await page.getByTestId('product-card').allInnerTexts();
  expect(uiResults).toHaveLength(apiResults.data.length);

  for (const product of apiResults.data) {
    await expect(page.getByText(product.name)).toBeVisible();
  }
});

test('search shows empty state when no results found', async ({ page }) => {
  await page.goto('/products');

  const searchBox = page.getByRole('searchbox', { name: 'Search products' });
  await searchBox.fill('zzz-no-product-exists-xyz');

  await page.waitForResponse(r => r.url().includes('/api/search'));

  await expect(page.getByTestId('empty-search-state')).toBeVisible();
  await expect(page.getByText('No products found')).toBeVisible();
  await expect(page.getByTestId('product-card')).toHaveCount(0);
});
```

---

## S10: Optimistic UI Update + Rollback on API Failure

**Scenario:** User clicks "Like" on a post. The UI immediately shows the like count incremented (optimistic update). The API call fails. UI must roll back to the original count and show an error.

**Difficulty:** Hard

**Solution:**
```typescript
test('optimistic like update rolls back on API failure', async ({ page }) => {
  await page.goto('/feed');

  const likeBtn   = page.getByTestId('post-001-like-btn');
  const likeCount = page.getByTestId('post-001-like-count');

  const originalCount = parseInt(await likeCount.innerText());

  // Intercept the like API and force it to fail
  await page.route('**/api/posts/post-001/like', (route) => {
    route.fulfill({ status: 500, body: JSON.stringify({ error: 'Internal server error' }) });
  });

  await likeBtn.click();

  // Optimistic update: count should momentarily increment
  // (This verifies the UI did attempt the optimistic update before rollback)
  // Depending on the app's implementation speed, we check rollback:

  // After API failure resolves, count must be back to original
  await expect(likeCount).toHaveText(String(originalCount), { timeout: 3000 });

  // Error message must appear
  await expect(page.getByTestId('error-toast')).toContainText(
    /failed|try again|error/i,
    { timeout: 3000 }
  );

  // The like button must be in un-liked state (not stuck in liked state)
  await expect(likeBtn).not.toHaveAttribute('aria-pressed', 'true');

  // Remove the route override for cleanup
  await page.unroute('**/api/posts/post-001/like');

  // Now allow the real call — subsequent click must work
  await likeBtn.click();
  await expect(likeCount).toHaveText(String(originalCount + 1));
});
```

---

*— End of Section 4, S1–S10 —*

> **Next:** Section 4 S11–S20 (final batch) — covering: Pagination with API consistency, Multi-step wizard with back-navigation state, Infinite scroll + item insertion, Two-factor authentication flow, Bulk export with progress tracking, Admin impersonation session, Audit log verification, Rate-limiting UI feedback, Data table sort/filter/export parity, and Session timeout + re-authentication.

---

## S11: Paginated List — UI/API Consistency Across All Pages

**Scenario:** An admin table shows 500 users across 25 pages. Verify that the UI renders exactly what the API returns on every page, the total count is consistent, and navigating to page 15 directly produces the same data as paginating to it step by step.

**Difficulty:** Medium

**Solution:**
```typescript
test('paginated users table is consistent with API on all pages', async ({ page, request }) => {
  const token = await getAdminToken(request);

  await page.goto('/admin/users?page=1');

  // Step 1: Verify total count matches API
  const totalRes = await request.get('/api/users?page=1&limit=20', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const apiPage1 = await totalRes.json();

  await expect(page.getByTestId('total-count')).toHaveText(
    new RegExp(apiPage1.total.toLocaleString())
  );
  await expect(page.getByRole('row').filter({ hasNot: page.locator('th') })).toHaveCount(
    apiPage1.data.length
  );

  // Step 2: Verify page 1 UI rows match API rows (spot-check first 3)
  const uiEmails = await page
    .getByRole('row')
    .filter({ hasNot: page.locator('th') })
    .getByRole('cell')
    .nth(1) // Email column
    .allInnerTexts();

  for (let i = 0; i < Math.min(3, apiPage1.data.length); i++) {
    expect(uiEmails[i]).toBe(apiPage1.data[i].email);
  }

  // Step 3: Jump directly to page 15 via URL and verify matches API
  const apiPage15Res = await request.get('/api/users?page=15&limit=20', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const apiPage15 = await apiPage15Res.json();

  await page.goto('/admin/users?page=15');
  await page.waitForLoadState('networkidle');

  const p15Emails = await page
    .getByRole('row')
    .filter({ hasNot: page.locator('th') })
    .getByRole('cell')
    .nth(1)
    .allInnerTexts();

  expect(p15Emails[0]).toBe(apiPage15.data[0].email);
  expect(p15Emails.at(-1)).toBe(apiPage15.data.at(-1)?.email);

  // Step 4: Navigate to last page and verify "Next" is disabled
  const lastPage = Math.ceil(apiPage1.total / 20);
  await page.goto(`/admin/users?page=${lastPage}`);
  await expect(page.getByTestId('pagination-next')).toBeDisabled();
  await expect(page.getByTestId('pagination-prev')).toBeEnabled();
});
```

---

## S12: Multi-Step Wizard — State Preserved on Back Navigation

**Scenario:** A 5-step onboarding wizard. User fills steps 1–3, goes back to step 2, changes a value, goes forward. Verify: step 3 data reflects the change from step 2, no data is lost, and the final submission API payload is correct.

**Difficulty:** Medium

**Solution:**
```typescript
test('wizard preserves and updates state across back/forward navigation', async ({ page, request }) => {
  await page.goto('/onboarding');

  // Step 1: Company Info
  await page.getByLabel('Company Name').fill('Acme Corp');
  await page.getByLabel('Industry').selectOption('Technology');
  await page.getByRole('button', { name: 'Next' }).click();

  // Step 2: Team Size
  await page.getByLabel('Team Size').selectOption('51-200');
  await page.getByRole('button', { name: 'Next' }).click();

  // Step 3: Use Case (auto-populated based on team size choice)
  await expect(page.getByTestId('recommended-plan')).toHaveText('Business');
  await page.getByLabel('Primary Use Case').selectOption('Sales CRM');

  // Go back to Step 2 and change team size
  await page.getByRole('button', { name: 'Back' }).click();

  // Step 2 state must be preserved
  await expect(page.getByLabel('Team Size')).toHaveValue('51-200');

  // Change team size
  await page.getByLabel('Team Size').selectOption('201-1000');
  await page.getByRole('button', { name: 'Next' }).click();

  // Step 3: recommended plan should now reflect the new team size
  await expect(page.getByTestId('recommended-plan')).toHaveText('Enterprise');

  // Step 3 data (use case) should still be filled
  await expect(page.getByLabel('Primary Use Case')).toHaveValue('Sales CRM');

  // Step 4: Billing
  await page.getByLabel('Billing Email').fill('billing@acme.com');
  await page.getByRole('button', { name: 'Next' }).click();

  // Step 5: Review + Submit
  await expect(page.getByTestId('review-company')).toHaveText('Acme Corp');
  await expect(page.getByTestId('review-team-size')).toHaveText('201-1000');
  await expect(page.getByTestId('review-plan')).toHaveText('Enterprise');

  // Capture the API submission payload
  const [response] = await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/onboarding') && r.request().method() === 'POST'),
    page.getByRole('button', { name: 'Complete Setup' }).click(),
  ]);

  const payload = JSON.parse(response.request().postData() ?? '{}');
  expect(payload).toMatchObject({
    companyName: 'Acme Corp',
    teamSize:    '201-1000',
    plan:        'enterprise',
    useCase:     'Sales CRM',
    billingEmail: 'billing@acme.com',
  });

  await expect(page).toHaveURL('/dashboard?welcome=true');
});
```

---

## S13: Infinite Scroll with API Page-Loading Verification

**Scenario:** A social feed uses infinite scroll. New posts load when the user reaches the bottom. Verify: correct API pages load in sequence, no duplicate posts appear, and a newly-created post from another user appears correctly.

**Difficulty:** Hard

**Solution:**
```typescript
test('infinite scroll loads correct API pages with no duplicates', async ({ page, request }) => {
  await page.goto('/feed');
  await page.waitForLoadState('networkidle');

  const requestedPages: number[] = [];
  page.on('request', req => {
    const match = req.url().match(/\/api\/feed\?.*page=(\d+)/);
    if (match) requestedPages.push(parseInt(match[1]));
  });

  const allPostIds = new Set<string>();

  // Collect initial posts
  const initialIds = await page.getByTestId('feed-post').evaluateAll(
    (els: Element[]) => els.map(el => el.getAttribute('data-post-id') ?? '')
  );
  initialIds.forEach(id => allPostIds.add(id));

  // Scroll 3 times to load more
  for (let i = 0; i < 3; i++) {
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForResponse(r => r.url().includes('/api/feed'), { timeout: 8000 });
    await page.waitForLoadState('networkidle');

    const currentIds = await page.getByTestId('feed-post').evaluateAll(
      (els: Element[]) => els.map(el => el.getAttribute('data-post-id') ?? '')
    );

    // Check for duplicates
    const newIds = currentIds.filter(id => !allPostIds.has(id));
    const duplicates = currentIds.filter(id => allPostIds.has(id) && id !== '');
    expect(duplicates.length, `${duplicates.length} duplicate post(s) found after scroll ${i + 1}`).toBe(0);

    newIds.forEach(id => allPostIds.add(id));
  }

  // Verify pages loaded in sequence (1, 2, 3, 4)
  expect(requestedPages).toEqual(expect.arrayContaining([1, 2, 3, 4]));
  expect(new Set(requestedPages).size).toBe(requestedPages.length); // No page loaded twice

  // Inject a new post via API and verify it appears at the top after refresh
  const token = await page.evaluate(() => localStorage.getItem('apiToken'));
  await request.post('/api/posts', {
    data: { content: 'New post injected during scroll test' },
    headers: { Authorization: `Bearer ${token}` },
  });

  // Pull-to-refresh or click "New posts available"
  await page.evaluate(() => window.scrollTo(0, 0));
  const refreshBtn = page.getByTestId('new-posts-banner');
  if (await refreshBtn.isVisible()) {
    await refreshBtn.click();
  }

  await expect(page.getByTestId('feed-post').first())
    .toContainText('New post injected during scroll test', { timeout: 5000 });
});
```

---

## S14: Two-Factor Authentication (TOTP) Full Flow

**Scenario:** User enables TOTP-based 2FA, gets backup codes, logs out, logs back in with TOTP code, and verifies backup code works when TOTP is not available.

**Difficulty:** Hard

**Solution:**
```typescript
import * as OTPAuth from 'otpauth'; // npm install otpauth

test('enable TOTP 2FA and authenticate with it', async ({ page, request }) => {
  await page.goto('/settings/security');

  // Step 1: Enable 2FA
  await page.getByRole('button', { name: 'Enable Two-Factor Authentication' }).click();

  // Step 2: Read the TOTP secret from the QR code alt-text or plain-text field
  const totpSecret = await page.getByTestId('totp-secret').innerText();
  expect(totpSecret).toMatch(/^[A-Z2-7]{32}$/); // Base32 secret

  // Step 3: Generate a valid TOTP code using the secret
  const totp = new OTPAuth.TOTP({
    issuer:    'TestApp',
    label:     'test@example.com',
    algorithm: 'SHA1',
    digits:    6,
    period:    30,
    secret:    OTPAuth.Secret.fromBase32(totpSecret),
  });
  const code = totp.generate();

  // Step 4: Verify the code to complete 2FA setup
  await page.getByLabel('Enter the 6-digit code').fill(code);
  await page.getByRole('button', { name: 'Verify and Enable' }).click();
  await expect(page.getByText('Two-factor authentication enabled')).toBeVisible();

  // Step 5: Save backup codes
  const backupCodes = await page.getByTestId('backup-code').allInnerTexts();
  expect(backupCodes).toHaveLength(10);

  // Step 6: Log out
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL('/login');

  // Step 7: Log in — should stop at 2FA challenge
  await page.getByLabel('Email').fill('test@example.com');
  await page.getByLabel('Password').fill('TestPass123!');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByTestId('totp-challenge')).toBeVisible();

  // Step 8: Enter fresh TOTP code (regenerate in case 30s boundary crossed)
  const freshCode = totp.generate();
  await page.getByLabel('Authentication code').fill(freshCode);
  await page.getByRole('button', { name: 'Verify' }).click();
  await expect(page).toHaveURL('/dashboard');

  // Step 9: Test backup code works
  await page.getByRole('button', { name: 'Sign out' }).click();
  await page.getByLabel('Email').fill('test@example.com');
  await page.getByLabel('Password').fill('TestPass123!');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await page.getByRole('link', { name: 'Use a backup code' }).click();
  await page.getByLabel('Backup code').fill(backupCodes[0]);
  await page.getByRole('button', { name: 'Verify' }).click();
  await expect(page).toHaveURL('/dashboard');

  // Step 10: Used backup code must not work again (one-time use)
  await page.getByRole('button', { name: 'Sign out' }).click();
  await page.getByLabel('Email').fill('test@example.com');
  await page.getByLabel('Password').fill('TestPass123!');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByRole('link', { name: 'Use a backup code' }).click();
  await page.getByLabel('Backup code').fill(backupCodes[0]); // Same code
  await page.getByRole('button', { name: 'Verify' }).click();
  await expect(page.getByText('Invalid or already used backup code')).toBeVisible();
});
```

---

## S15: Bulk Export with Progress Tracking

**Scenario:** Admin triggers an export of 10,000 records. A progress bar updates in real time. On completion, the file is available for download. Test the full lifecycle without timing out.

**Difficulty:** Medium

**Solution:**
```typescript
test('bulk export tracks progress and completes with downloadable file', async ({ page, request }) => {
  await page.goto('/admin/exports');

  // Trigger export
  await page.getByRole('button', { name: 'Export All Transactions' }).click();
  await page.getByLabel('Format').selectOption('CSV');
  await page.getByLabel('Date Range').selectOption('last-90-days');
  await page.getByRole('button', { name: 'Start Export' }).click();

  const exportId = await page.getByTestId('export-job-id').innerText();
  expect(exportId).toMatch(/^export-/);

  // Step 2: Progress bar should appear immediately
  await expect(page.getByRole('progressbar')).toBeVisible();

  // Step 3: Progress must advance (not stuck at 0%)
  const initialProgress = await page
    .getByRole('progressbar')
    .getAttribute('aria-valuenow');
  expect(parseInt(initialProgress ?? '0')).toBeGreaterThanOrEqual(0);

  // Step 4: Poll for completion — max 2 minutes for 10k records
  await expect.poll(
    async () => {
      const res = await request.get(`/api/exports/${exportId}`, {
        headers: { Authorization: `Bearer ${process.env.API_TOKEN}` },
      });
      const job = await res.json();
      return job.status;
    },
    {
      timeout:   120_000,
      intervals: [2000, 5000, 10_000],
      message:   'Export job did not complete within 2 minutes',
    }
  ).toBe('completed');

  // Step 5: Progress bar should be at 100%
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  await expect(page.getByText('Export complete')).toBeVisible();

  // Step 6: Download the file
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Download Export' }).click(),
  ]);

  expect(download.suggestedFilename()).toMatch(/transactions.*\.csv/);

  const failureReason = download.failure();
  expect(failureReason).toBeNull();

  // Step 7: Verify file size is reasonable (10k rows at ~200 bytes each ≈ 2MB)
  const savePath = `test-downloads/${download.suggestedFilename()}`;
  await download.saveAs(savePath);
  const { size } = require('fs').statSync(savePath);
  expect(size).toBeGreaterThan(100_000);  // At least 100KB
  expect(size).toBeLessThan(50_000_000); // Under 50MB
});
```

---

## S16: Admin Impersonation Session

**Scenario:** Admin can "impersonate" a user to see the app from their perspective. Impersonation session must: show the user's data, restrict admin-only actions, and end cleanly returning the admin to their own session.

**Difficulty:** Hard

**Solution:**
```typescript
test('admin impersonation shows user view and ends cleanly', async ({ page, request }) => {
  // Seed a user with specific data
  const seedRes = await request.post('/api/test/users', {
    data: { name: 'Target User', email: `target-${Date.now()}@test.com`, role: 'user' },
    headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
  });
  const { id: targetUserId, email: targetEmail } = await seedRes.json();

  // Admin login
  const context = await page.context().browser()!.newContext({ storageState: 'auth/admin.json' });
  const adminPage = await context.newPage();

  await adminPage.goto(`/admin/users/${targetUserId}`);

  // Start impersonation
  await adminPage.getByRole('button', { name: 'Impersonate User' }).click();
  await adminPage.getByRole('button', { name: 'Confirm Impersonation' }).click();

  // Impersonation banner must be visible at all times
  await expect(adminPage.getByTestId('impersonation-banner')).toContainText(
    `Viewing as ${targetEmail}`
  );

  // App shows the impersonated user's data
  await adminPage.goto('/dashboard');
  await expect(adminPage.getByTestId('user-greeting')).toContainText('Target User');

  // Admin-only sections must be hidden during impersonation
  await adminPage.goto('/admin/users');
  await expect(adminPage).toHaveURL(/\/403|\/dashboard/, { timeout: 5000 });

  // Impersonated user's orders are shown (not admin's orders)
  await adminPage.goto('/orders');
  const token = await adminPage.evaluate(() => localStorage.getItem('apiToken'));
  const ordersRes = await request.get('/api/orders', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const orders = await ordersRes.json();
  for (const order of orders.data ?? []) {
    expect(order.userId).toBe(targetUserId);
  }

  // End impersonation
  await adminPage.getByRole('button', { name: 'End Impersonation' }).click();
  await expect(adminPage.getByTestId('impersonation-banner')).not.toBeVisible();

  // Admin should be back in their own session
  await adminPage.goto('/admin/users');
  await expect(adminPage).toHaveURL('/admin/users');
  await expect(adminPage.getByTestId('user-greeting')).not.toContainText('Target User');

  // Cleanup
  await context.close();
  await request.delete(`/api/test/users/${targetUserId}`, {
    headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
  });
});
```

---

## S17: Audit Log Verification for Sensitive Operations

**Scenario:** Every sensitive action (user delete, role change, export, login, failed login) must create an immutable audit log entry. Test that the UI action correlates exactly with the API audit log.

**Difficulty:** Medium

**Solution:**
```typescript
test('sensitive actions create correct audit log entries', async ({ page, request }) => {
  const adminToken = process.env.ADMIN_API_TOKEN!;

  // Record audit log baseline timestamp
  const before = new Date().toISOString();

  // --- Action 1: Admin changes a user's role ---
  await page.goto('/admin/users/usr-001');
  await page.getByLabel('Role').selectOption('readonly');
  await page.getByRole('button', { name: 'Save Changes' }).click();
  await expect(page.getByText('User updated')).toBeVisible();

  // --- Action 2: Admin deletes a test user ---
  await page.goto('/admin/users/usr-test-deleteme');
  await page.getByRole('button', { name: 'Delete User' }).click();
  await page.getByRole('button', { name: 'Confirm Delete' }).click();
  await expect(page.getByText('User deleted')).toBeVisible();

  // Fetch audit logs for the period
  const auditRes = await request.get(`/api/audit-logs?after=${before}&limit=50`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  expect(auditRes.status()).toBe(200);
  const { logs } = await auditRes.json();

  // Verify role-change entry
  const roleChangeLog = logs.find((l: any) =>
    l.action === 'user.role_changed' && l.targetId === 'usr-001'
  );
  expect(roleChangeLog).toBeDefined();
  expect(roleChangeLog.changes).toMatchObject({
    role: { from: expect.any(String), to: 'readonly' },
  });
  expect(roleChangeLog.actorId).toMatch(/^usr-admin/);
  expect(roleChangeLog.ipAddress).toMatch(/^\d+\.\d+\.\d+\.\d+$/);

  // Verify delete entry
  const deleteLog = logs.find((l: any) =>
    l.action === 'user.deleted' && l.targetId === 'usr-test-deleteme'
  );
  expect(deleteLog).toBeDefined();
  expect(deleteLog.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);

  // Verify audit logs are immutable (attempting to delete one returns 405)
  const deleteAuditAttempt = await request.delete(`/api/audit-logs/${roleChangeLog.id}`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  expect(deleteAuditAttempt.status()).toBe(405);
});
```

---

## S18: Rate Limiting — UI Feedback and Retry-After Behavior

**Scenario:** The login endpoint has a rate limit of 5 attempts per minute. After the 5th failed attempt the user sees a lockout message with a countdown. After the Retry-After period, login becomes possible again.

**Difficulty:** Medium

**Solution:**
```typescript
test('rate limit shows lockout UI and respects Retry-After header', async ({ page, request }) => {
  const testEmail = `ratelimit-${Date.now()}@test.com`;

  // Pre-create the user but use wrong password for rate limiting test
  await request.post('/api/test/users', {
    data: { email: testEmail, name: 'Rate Test', role: 'user' },
    headers: { Authorization: `Bearer ${process.env.SEED_TOKEN}` },
  });

  await page.goto('/login');

  // Attempt 4 failed logins — UI should show warning after 3rd
  for (let attempt = 1; attempt <= 4; attempt++) {
    await page.getByLabel('Email').fill(testEmail);
    await page.getByLabel('Password').fill('WrongPassword!');
    await page.getByRole('button', { name: 'Sign in' }).click();

    if (attempt >= 3) {
      await expect(page.getByTestId('rate-limit-warning')).toContainText(
        new RegExp(`${5 - attempt} attempt(s)? remaining`)
      );
    }
  }

  // 5th attempt — triggers lockout
  await page.getByLabel('Email').fill(testEmail);
  await page.getByLabel('Password').fill('WrongPassword!');
  await page.getByRole('button', { name: 'Sign in' }).click();

  // Lockout message must appear
  await expect(page.getByTestId('lockout-message')).toBeVisible();
  await expect(page.getByTestId('lockout-message')).toContainText(/too many attempts/i);
  await expect(page.getByTestId('retry-countdown')).toBeVisible();

  // Sign-in form must be disabled
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeDisabled();

  // Verify API also returns 429 with Retry-After header
  const apiAttempt = await request.post('/api/auth/login', {
    data: { email: testEmail, password: 'AnyPassword' },
  });
  expect(apiAttempt.status()).toBe(429);
  expect(apiAttempt.headers()['retry-after']).toBeDefined();
  const retryAfterSecs = parseInt(apiAttempt.headers()['retry-after']);
  expect(retryAfterSecs).toBeGreaterThan(0);
  expect(retryAfterSecs).toBeLessThanOrEqual(60);

  // Fast-forward time: mock the clock to skip the lockout window
  await page.clock.fastForward(retryAfterSecs * 1000 + 1000);

  // Form should be re-enabled after the lockout expires
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeEnabled({ timeout: 5000 });

  // Correct password now works
  await page.getByLabel('Password').fill('CorrectPassword123!');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL('/dashboard');
});
```

---

## S19: Data Table Sort/Filter/Export Parity

**Scenario:** Users can filter a transactions table by status, sort by amount, and export the filtered+sorted result. The exported CSV must contain exactly the same records and order as the visible UI table.

**Difficulty:** Medium

**Solution:**
```typescript
test('filtered and sorted table export matches visible UI rows', async ({ page, request }) => {
  await page.goto('/transactions');

  // Step 1: Apply filter — status = "Failed"
  await page.getByRole('combobox', { name: 'Status' }).selectOption('failed');
  await page.waitForResponse(r => r.url().includes('/api/transactions'));

  // Step 2: Sort by Amount descending
  await page.getByRole('columnheader', { name: 'Amount' }).click(); // asc
  await page.getByRole('columnheader', { name: 'Amount' }).click(); // desc
  await page.waitForResponse(r => r.url().includes('/api/transactions'));
  await page.waitForLoadState('networkidle');

  // Step 3: Collect all visible rows from UI
  const uiRows = await page
    .getByRole('row')
    .filter({ hasNot: page.locator('th') })
    .evaluateAll((rows: Element[]) =>
      rows.map(row => {
        const cells = row.querySelectorAll('td');
        return {
          id:     cells[0]?.textContent?.trim() ?? '',
          amount: cells[2]?.textContent?.trim() ?? '',
          status: cells[3]?.textContent?.trim() ?? '',
        };
      })
    );

  expect(uiRows.every(r => r.status.toLowerCase() === 'failed')).toBe(true);

  // Verify descending sort (amounts must be decreasing)
  const amounts = uiRows.map(r => parseFloat(r.amount.replace(/[^0-9.]/g, '')));
  for (let i = 1; i < amounts.length; i++) {
    expect(amounts[i]).toBeLessThanOrEqual(amounts[i - 1]);
  }

  // Step 4: Export the filtered+sorted view
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Export CSV' }).click(),
  ]);
  const csvPath = `test-downloads/${download.suggestedFilename()}`;
  await download.saveAs(csvPath);

  // Step 5: Parse CSV and verify matches UI rows
  const csvContent = require('fs').readFileSync(csvPath, 'utf-8');
  const csvLines = csvContent.trim().split('\n').slice(1); // Remove header
  const csvRows = csvLines.map((line: string) => {
    const cols = line.split(',').map((c: string) => c.replace(/"/g, '').trim());
    return { id: cols[0], amount: cols[2], status: cols[3] };
  });

  expect(csvRows.length).toBe(uiRows.length);

  // First 5 rows must match exactly (same order)
  for (let i = 0; i < Math.min(5, uiRows.length); i++) {
    expect(csvRows[i].id).toBe(uiRows[i].id);
    expect(csvRows[i].status.toLowerCase()).toBe('failed');
  }

  // Step 6: Cross-check with API using the same filter/sort params
  const apiRes = await request.get(
    '/api/transactions?status=failed&sort=amount&order=desc&limit=100',
    { headers: { Authorization: `Bearer ${process.env.API_TOKEN}` } }
  );
  const apiData = await apiRes.json();

  expect(apiData.data.length).toBe(uiRows.length);
  expect(apiData.data[0].id).toBe(uiRows[0].id);
});
```

---

## S20: Session Timeout + Re-Authentication Without Data Loss

**Scenario:** A user is filling a long form. Their session expires mid-way. When they submit, they see a re-auth prompt. After re-authenticating, the form data must still be present and submission must succeed.

**Difficulty:** Hard

**Solution:**
```typescript
test('session expiry triggers re-auth without losing form data', async ({ page }) => {
  await page.goto('/reports/new');

  // Fill a complex form (partially)
  await page.getByLabel('Report Title').fill('Q4 Financial Analysis');
  await page.getByLabel('Description').fill('Detailed analysis of Q4 revenue streams...');
  await page.getByLabel('Date Range').fill('2024-10-01 to 2024-12-31');
  await page.getByLabel('Include Charts').check();
  await page.getByLabel('Department').selectOption('Finance');

  // Simulate session expiry: invalidate the auth token artificially
  // Intercept next API call to return 401
  let isExpired = true;
  await page.route('**/api/reports', async (route) => {
    if (isExpired && route.request().method() === 'POST') {
      isExpired = false; // Only fail once
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Session expired', code: 'SESSION_EXPIRED' }),
      });
    } else {
      await route.continue();
    }
  });

  // Submit the form — should trigger re-auth modal
  await page.getByRole('button', { name: 'Generate Report' }).click();

  // Re-auth dialog must appear
  await expect(page.getByRole('dialog', { name: /session expired|re-authenticate/i })).toBeVisible({
    timeout: 5000,
  });

  // Form data behind the dialog must still be there
  // (verify without closing dialog — form is in background)
  const titleValue = await page.getByLabel('Report Title').inputValue();
  expect(titleValue).toBe('Q4 Financial Analysis');

  // Re-authenticate
  await page.getByLabel('Password').fill('UserSecurePass123!');
  await page.getByRole('button', { name: 'Re-authenticate' }).click();

  // Dialog closes
  await expect(page.getByRole('dialog', { name: /session expired/i })).not.toBeVisible({ timeout: 5000 });

  // Form data must still be intact
  await expect(page.getByLabel('Report Title')).toHaveValue('Q4 Financial Analysis');
  await expect(page.getByLabel('Description')).toHaveValue('Detailed analysis of Q4 revenue streams...');
  await expect(page.getByLabel('Include Charts')).toBeChecked();
  await expect(page.getByLabel('Department')).toHaveValue('Finance');

  // Original submission auto-retries and succeeds
  await expect(page).toHaveURL(/\/reports\/rpt-/, { timeout: 15_000 });
  await expect(page.getByText('Report generation started')).toBeVisible();
});
```

---

*— End of Section 4, S11–S20 — Section 4 Complete —*

---

---

# COMPLETE GUIDE SUMMARY

This guide covers **120 questions and scenarios** across four sections:

| Section | Count | Focus |
|---|---|---|
| Section 1 — Theory | 50 | Playwright internals, APIs, patterns, CI/CD |
| Section 2 — Coding | 30 | Production-grade utility classes and helpers |
| Section 3 — Architecture | 20 | Framework design, strategy, enterprise patterns |
| Section 4 — UI+API Scenarios | 20 | Realistic end-to-end integration test challenges |

**Target role:** Senior SDET / QA Automation Engineer (6–10 years) at FinTech, SaaS, product companies.

*— End of Guide —*
