package com.bookstore.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TempoTools {
    private final RestClient client;

    public TempoTools(@Value("${agent.tempo.url}") String url) {
        this.client = RestClient.builder().baseUrl(url).build();
    }

    @Tool(description = "Search Tempo for traces using a Tempo search/tag expression. Use service.name and other known tags to correlate slow or failed requests. This is read-only.")
    public String searchTraces(
            @ToolParam(description = "Tempo search query or tag expression supported by the deployed Tempo version") String query,
            @ToolParam(description = "Maximum traces to return, normally 10") Integer limit) {
        int safeLimit = limit == null ? 10 : Math.min(Math.max(limit, 1), 50);
        return client.get()
                .uri(uri -> uri.path("/api/search")
                        .queryParam("q", query)
                        .queryParam("limit", safeLimit)
                        .build())
                .retrieve().body(String.class);
    }
}
