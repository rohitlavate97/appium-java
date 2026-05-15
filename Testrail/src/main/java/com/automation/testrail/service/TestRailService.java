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
