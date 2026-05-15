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
