package com.bookstore.aiagent.api;

import com.bookstore.aiagent.service.ObservabilityAgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private static final Logger logger =
            LoggerFactory.getLogger(AgentController.class);
    private final ObservabilityAgentService agent;

    public AgentController(ObservabilityAgentService agent) {
        this.agent = agent;
    }

    @PostMapping(value = "/diagnose", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String diagnose(@Valid @RequestBody DiagnoseRequest request) {
        logger.info("AgentController diagnose  request :{} ",request.question());
        return agent.diagnose(request.question());
    }

    public record DiagnoseRequest(@NotBlank String question) {}
}
