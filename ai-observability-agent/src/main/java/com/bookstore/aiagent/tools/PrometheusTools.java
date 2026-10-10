
package com.bookstore.aiagent.tools;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PrometheusTools {

    private static final Logger logger =
            LoggerFactory.getLogger(PrometheusTools.class);

    private final RestClient restClient;
    private final String prometheusUrl;

    public PrometheusTools(
            @Value("${agent.prometheus.url}") String prometheusUrl) {

        this.prometheusUrl = prometheusUrl;

        this.restClient = RestClient.builder().build();

        logger.info("PrometheusTools initialized");
        logger.info("Prometheus URL: {}", prometheusUrl);
    }

    @Tool(description = """
            Query Prometheus metrics using PromQL.

            Pass only a valid PromQL expression, never natural language.

            Examples:
            up{job="book-management-app"}

            http_server_requests_seconds_count{
                job="book-management-app",
                status=~"5.."
            }

            Use this tool to investigate application availability,
            HTTP request metrics, error rates, JVM metrics,
            application performance, and service health.

            Return the actual response returned by Prometheus.
            """)
    public String queryPrometheus(String query) {

        logger.info("========== PROMETHEUS TOOL ==========");

        if (query == null || query.isBlank()) {
            return "Prometheus query failed: PromQL query is empty.";
        }

        logger.info("PromQL: {}", query);

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(prometheusUrl)
                    .pathSegment("api", "v1", "query")
                    .queryParam("query", query)
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            logger.info("Prometheus URI: {}", uri);

            String response = restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                logger.warn("Prometheus returned an empty response");
                return "Prometheus query completed but returned no data.";
            }

            logger.info("Prometheus query successful");
            logger.info("Prometheus response length: {}", response.length());
            logger.info("========== PROMETHEUS TOOL END ==========");

            return response;

        } catch (Exception e) {
            logger.error("Prometheus query failed. PromQL: {}", query, e);

            return "Prometheus query failed: " + e.getMessage();
        }
    }
}
