package com.bookstore.aiagent.service;

import com.bookstore.aiagent.tools.LokiTools;
import com.bookstore.aiagent.tools.PrometheusTools;
import com.bookstore.aiagent.tools.TempoTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import com.bookstore.aiagent.constants.AgentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class ObservabilityAgentService {

    private static final Logger logger =
            LoggerFactory.getLogger(ObservabilityAgentService.class);

    private final ChatClient chatClient;

    public ObservabilityAgentService(
            ChatClient.Builder builder,
            PrometheusTools prometheus,
            LokiTools loki,
            TempoTools tempo) {
        logger.info("ObservabilityAgentService  constructor {} ");

        this.chatClient = builder
                .defaultSystem(AgentConstants.SYSTEM)
                .defaultTools(prometheus, loki, tempo)
                .build();
    }

    public String diagnose(String question) {
        logger.info("ObservabilityAgentService ObservabilityAgentService {} ",question);
                String response = chatClient.prompt()
                .system(AgentConstants.SYSTEM_PROMPT)
                .user(question)
                .call()
                .content();

                return response;
    }
}
