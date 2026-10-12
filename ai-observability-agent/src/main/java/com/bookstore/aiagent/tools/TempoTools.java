
package com.bookstore.aiagent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import com.bookstore.aiagent.constants.AgentConstants;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TempoTools {

    private static final Logger logger =
            LoggerFactory.getLogger(TempoTools.class);

    private static final int MAX_TRACE_LIMIT = 100;

    // Standard Tempo trace IDs are 32 hexadecimal characters.
    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{32}$");

    private final RestClient restClient;
    private final String tempoUrl;

    public TempoTools(
            @Value("${agent.tempo.url}") String tempoUrl) {

        if (tempoUrl == null || tempoUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Property agent.tempo.url must not be empty"
            );
        }

        this.tempoUrl = tempoUrl.replaceAll("/+$", "");

        this.restClient = RestClient.builder()
                .build();

        logger.info("TempoTools initialized");
        logger.info("Tempo URL: {}", this.tempoUrl);
    }

    /**
     * Search traces using TraceQL.
     *
     * Endpoint:
     * GET /api/search?q={TraceQL}&limit={limit}
     */

    @Tool(description = AgentConstants.TEMPO_DESCRIPTION)
    public String searchTraces(String serviceName, int limit) {

        logger.info("========== TEMPO SEARCH START ==========");

        try {
            if (serviceName == null || serviceName.isBlank()) {
                return "Error: serviceName must not be empty.";
            }

            int effectiveLimit = Math.max(1, Math.min(limit, MAX_TRACE_LIMIT));

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(tempoUrl)
                    .path("/api/search")
                    .queryParam("tags", "service.name=" + serviceName.trim())
                    .queryParam("limit", effectiveLimit)
                    .build()
                    .encode()
                    .toUri();

            logger.info("Tempo search URL: {}", uri);
            logger.info("Service name: {}", serviceName);
            logger.info("Requested limit: {}", effectiveLimit);

            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                logger.warn("Tempo returned an empty search response.");
                return "{\"traces\":[]}";
            }

            logger.info("Tempo search completed successfully.");
            logger.debug("Tempo response: {}", response);

            // Return the complete JSON response; do not select only
            // the first trace or truncate the traces array.
            return response;

        } catch (org.springframework.web.client.RestClientResponseException ex) {
            logger.error(
                    "Tempo search failed: status={}, body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );

            return "Tempo search failed with HTTP status "
                    + ex.getStatusCode()
                    + ". Check the service name and Tempo response.";

        } catch (Exception ex) {
            logger.error("Unexpected error during Tempo search.", ex);
            return "Tempo search failed: " + ex.getMessage();

        } finally {
            logger.info("========== TEMPO SEARCH END ==========");
        }
    }


    /**
     * Retrieve complete trace data by trace ID.
     *
     * Endpoint:
     * GET /api/traces/{traceID}
     *
     * This endpoint returns the trace data available in Tempo,
     * including spans and their recorded attributes and events.
     */
    @Tool(description = AgentConstants.TEMPO_TRACE_DETAILS_DESCRIPTION)
    public String getTraceDetails(String traceId) {

        logger.info("========== TEMPO TRACE DETAILS ==========");
        logger.info("Requested trace ID: {}", traceId);

        if (traceId == null || traceId.isBlank()) {
            return "Tempo trace retrieval failed: trace ID is empty.";
        }

        String normalizedTraceId = traceId.trim()
                .toLowerCase(Locale.ROOT);

        if (!TRACE_ID_PATTERN.matcher(normalizedTraceId).matches()) {
            return "Tempo trace retrieval failed: invalid trace ID. "
                    + "Expected 32 hexadecimal characters.";
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(tempoUrl)
                    .pathSegment("api", "traces", normalizedTraceId)
                    .build()
                    .encode()
                    .toUri();

            logger.info("Tempo trace endpoint: {}", uri);

            String response = restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return "Tempo returned an empty trace response for "
                        + normalizedTraceId
                        + ". Verify the trace ID, retention period, "
                        + "and Tempo configuration.";
            }

            logger.info(
                    "Trace retrieval succeeded. Trace ID: {}, "
                            + "response length: {}",
                    normalizedTraceId,
                    response.length()
            );

            return response;

        } catch (RestClientResponseException e) {
            logger.error(
                    "Tempo trace HTTP error: traceId={}, status={}, body={}",
                    normalizedTraceId,
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            return "Tempo trace retrieval failed for trace ID "
                    + normalizedTraceId
                    + " with HTTP status "
                    + e.getStatusCode().value()
                    + ". Verify the trace ID, endpoint, and retention.";

        } catch (Exception e) {
            logger.error(
                    "Tempo trace retrieval failed for {}",
                    normalizedTraceId,
                    e
            );

            return "Tempo trace retrieval failed: "
                    + e.getClass().getSimpleName()
                    + ". Check application logs for details.";
        } finally {
            logger.info("========== TEMPO TRACE DETAILS END ==========");
        }
    }
}
