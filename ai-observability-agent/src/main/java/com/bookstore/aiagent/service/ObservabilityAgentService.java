package com.bookstore.aiagent.service;

import com.bookstore.aiagent.tools.LokiTools;
import com.bookstore.aiagent.tools.PrometheusTools;
import com.bookstore.aiagent.tools.TempoTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class ObservabilityAgentService {

    private static final Logger logger =
            LoggerFactory.getLogger(ObservabilityAgentService.class);
    private static final String SYSTEM = """
        You are a production Kubernetes observability SRE assistant.
        Your job is to investigate incidents using the supplied read-only tools.
        Do not invent metrics, logs, traces, pod states, causes, or remediation results.
        Prefer evidence from the tools. Correlate metrics, logs, traces and Kubernetes state.
        Explain: (1) symptoms, (2) strongest evidence, (3) probable root cause with confidence,
        (4) affected workload, namespace and time window when known, and
        (5) safe next steps. Never claim that a remediation was executed.
        If evidence is insufficient, explicitly say what additional evidence is needed.
        """;

    private final ChatClient chatClient;

    public ObservabilityAgentService(
            ChatClient.Builder builder,
            PrometheusTools prometheus,
            LokiTools loki,
            TempoTools tempo) {
        logger.info("ObservabilityAgentService  constructor {} ");

        this.chatClient = builder
                .defaultSystem(SYSTEM)
                .defaultTools(prometheus, loki, tempo)
                .build();
    }

    public String diagnose(String question) {
        logger.info("ObservabilityAgentService ObservabilityAgentService {} ",question);
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
