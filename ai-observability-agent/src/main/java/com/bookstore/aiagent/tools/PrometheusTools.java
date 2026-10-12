
package com.bookstore.aiagent.tools;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import com.bookstore.aiagent.constants.AgentConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PrometheusTools {

    private static final Logger logger =
            LoggerFactory.getLogger(PrometheusTools.class);

    private static final int MAX_RANGE_MINUTES = 10080;

    private static final Pattern STEP_PATTERN =
            Pattern.compile("^\\d+(ms|s|m|h|d|w|y)$");

    private final RestClient restClient;
    private final String prometheusUrl;
    private final ObjectMapper objectMapper;

    public PrometheusTools(
            @Value("${agent.prometheus.url}") String prometheusUrl,
            ObjectMapper objectMapper) {

        if (prometheusUrl == null || prometheusUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Property agent.prometheus.url must not be empty");
        }

        this.prometheusUrl = prometheusUrl.replaceAll("/+$", "");
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;

        logger.info("PrometheusTools initialized");
        logger.info("Prometheus URL: {}", this.prometheusUrl);
    }

    /**
     * Executes an instant PromQL query.
     * Suitable for current values and current application status.
     */
    @Tool(description = AgentConstants.PROMETHEUS_DESCRIPTION)
    public String queryPrometheus(String query) {

        logger.info("========== PROMETHEUS INSTANT QUERY START ==========");

        if (query == null || query.isBlank()) {
            return "Prometheus query failed: PromQL query is empty.";
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(prometheusUrl)
                    .path("/api/v1/query")
                    .queryParam("query", query.trim())
                    .build()
                    .encode()
                    .toUri();

            logger.info("PromQL: {}", query);
            logger.debug("Prometheus request URI: {}", uri);

            String response = executeRequest(uri);

            logger.info("Prometheus instant query completed");
            return response;

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Prometheus HTTP error: status={}, body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());

            return "Prometheus HTTP error: status="
                    + ex.getStatusCode()
                    + ", response="
                    + ex.getResponseBodyAsString();

        } catch (Exception ex) {
            logger.error("Prometheus instant query failed", ex);
            return "Prometheus query failed: " + ex.getMessage();

        } finally {
            logger.info("========== PROMETHEUS INSTANT QUERY END ==========");
        }
    }

    /**
     * Executes a range PromQL query over the previous N minutes.
     * Suitable for trends, request rates, CPU usage, and error counts.
     */
    @Tool(description = AgentConstants.PROMETHEUS_RANGE_DESCRIPTION)
    public String queryPrometheusRange(
            String query,
            int minutesBack,
            String step) {

        logger.info("========== PROMETHEUS RANGE QUERY START ==========");

        if (query == null || query.isBlank()) {
            return "Prometheus range query failed: PromQL query is empty.";
        }

        if (minutesBack < 1 || minutesBack > MAX_RANGE_MINUTES) {
            return "Invalid minutesBack. Use a value between 1 and "
                    + MAX_RANGE_MINUTES + ".";
        }

        if (step == null || !STEP_PATTERN.matcher(step).matches()) {
            return "Invalid step. Use a duration such as 15s, 1m, or 5m.";
        }

        try {
            Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant start = end.minus(minutesBack, ChronoUnit.MINUTES);

            URI uri = UriComponentsBuilder
                    .fromUriString(prometheusUrl)
                    .path("/api/v1/query_range")
                    .queryParam("query", query.trim())
                    .queryParam("start", start.toString())
                    .queryParam("end", end.toString())
                    .queryParam("step", step)
                    .build()
                    .encode()
                    .toUri();

            logger.info("PromQL: {}", query);
            logger.info("Range start: {}", start);
            logger.info("Range end: {}", end);
            logger.info("Range minutes: {}", minutesBack);
            logger.info("Step: {}", step);
            logger.debug("Prometheus request URI: {}", uri);

            String response = executeRequest(uri);

            logger.info("Prometheus range query completed");
            return response;

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Prometheus range HTTP error: status={}, body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());

            return "Prometheus range query HTTP error: status="
                    + ex.getStatusCode()
                    + ", response="
                    + ex.getResponseBodyAsString();

        } catch (Exception ex) {
            logger.error("Prometheus range query failed", ex);
            return "Prometheus range query failed: " + ex.getMessage();

        } finally {
            logger.info("========== PROMETHEUS RANGE QUERY END ==========");
        }
    }

    /**
     * Executes the request and checks Prometheus's JSON-level status.
     * Prometheus can return HTTP 200 with status=error.
     */
    private String executeRequest(URI uri) throws Exception {

        String response = restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            return "Prometheus returned an empty response.";
        }

        JsonNode root = objectMapper.readTree(response);

        if ("error".equals(root.path("status").asText())) {
            String errorType = root.path("errorType").asText("unknown");
            String error = root.path("error").asText("No error details supplied");

            logger.warn(
                    "Prometheus API error: type={}, message={}",
                    errorType,
                    error);

            return "Prometheus API error: type="
                    + errorType
                    + ", message="
                    + error;
        }

        logger.debug("Prometheus response length: {}", response.length());

        // Return the complete response, including all matching series.
        return response;
    }
}
