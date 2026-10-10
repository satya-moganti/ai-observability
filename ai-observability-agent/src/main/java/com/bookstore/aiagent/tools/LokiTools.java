package com.bookstore.aiagent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@Component
public class LokiTools {

    private static final Logger logger =
            LoggerFactory.getLogger(LokiTools.class);

    private final RestClient restClient;
    private final String lokiUrl;

    public LokiTools(
            @Value("${agent.loki.url}") String lokiUrl) {

        this.lokiUrl = lokiUrl;

        this.restClient = RestClient.builder()
                .build();

        logger.info("LokiTools initialized");
        logger.info("Loki URL: {}", lokiUrl);
    }

    /**
     * Query Grafana Loki using LogQL.
     *
     * The AI agent supplies:
     *   1. A valid LogQL query
     *   2. Number of minutes to search
     *
     * Java calculates the Loki timestamps.
     */
    @Tool(description = """
            Query Grafana Loki logs using LogQL.

            IMPORTANT:
            The logql parameter MUST be a valid LogQL query.
            Never pass natural language such as:
            "book-management-app error"

            Valid example:
            {instance=~"default/book-management-app-.*:book-management-app"} |= "ERROR"

            The minutes parameter specifies how far back to search.

            Example:
            logql =
            {instance=~"default/book-management-app-.*:book-management-app"} |= "ERROR"

            minutes = 15

            Use this tool when investigating:
            - application errors
            - exceptions
            - warnings
            - stack traces
            - Kubernetes application failures
            - pod failures

            The application calculates the Loki start and end timestamps.
            Do not provide "now" as a timestamp.
            Return the actual logs returned by Loki.
            """)
    public String queryLogs(String logql, int minutes) {

        logger.info("========== LOKI TOOL ==========");
        logger.info("LogQL: {}", logql);
        logger.info("Minutes: {}", minutes);


        if (logql == null || logql.isBlank()) {
            return "Loki query failed: LogQL query is empty.";
        }

        if (minutes <= 0) {
            return "Loki query failed: minutes must be greater than zero.";
        }

        // Prevent accidentally huge queries
        if (minutes > 1440) {
            logger.warn(
                    "Requested Loki search window {} minutes. Limiting to 1440 minutes.",
                    minutes
            );

            minutes = 1440;
        }

        long end = Instant.now().getEpochSecond();

        long start = Instant.now()
                .minusSeconds(minutes * 60L)
                .getEpochSecond();

        logger.info("Start timestamp: {}", start);
        logger.info("End timestamp:   {}", end);

        URI uri = UriComponentsBuilder
                .fromHttpUrl(lokiUrl)
                .path("/loki/api/v1/query_range")
                .queryParam("query", logql)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("limit", 50)
                .queryParam("direction", "backward")
                .build()

                .toUri();

        logger.info("Loki URI: {}", uri);

        try {

            String response = restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            logger.info("Loki query successful");

            if (response == null || response.isBlank()) {

                logger.warn("Loki returned an empty response");

                return "Loki query completed but returned no data.";
            }

            logger.info("Loki response length: {}", response.length());

            logger.info("========== LOKI TOOL END ==========");

            return response;

        } catch (Exception e) {

            logger.error("========== LOKI TOOL ERROR ==========");
            logger.error("LogQL: {}", logql);
            logger.error("Start: {}", start);
            logger.error("End: {}", end);
            logger.error("URI: {}", uri);
            logger.error("Error: {}", e.getMessage(), e);

            return "Loki query failed: " + e.getMessage();
        }
    }
}