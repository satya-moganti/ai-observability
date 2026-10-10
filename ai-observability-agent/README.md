# AI Observability Agent

A read-only Spring Boot 3 / Spring AI agent for the `ai-observability` project.

## What it does

The agent accepts an incident question and uses model tool-calling to investigate:

- Prometheus: metrics and PromQL
- Loki: application/container logs and LogQL
- Tempo: distributed traces
- Kubernetes API: pods, pod logs and events

It correlates the evidence and returns a diagnosis with confidence and safe next steps.

## Project integration

The repository already runs Java 17 + Spring Boot 3 and the Grafana stack. The agent therefore runs as a separate deployment rather than modifying `book-management-app`.

## Build

For Manual Build:

```bash
cd ai-observability-agent
mvn clean package
java -jar target/ai-observability-agent.jar
```

Then:

```bash
    curl -X POST http://localhost:8085/api/agent/diagnose   -H "Content-Type: application/json"   -d '{"question":"Why are requests to book-management-app failing or becoming slow? Check metrics, logs, traces and Kubernetes events and give me the most likely root cause."}'
```

## Important

The Tempo search endpoint can vary with Tempo configuration/version. If `/api/search?q=...` is not enabled by the installed chart, adapt `TempoTools.searchTraces()` to the TraceQL/search endpoint exposed by the deployed Tempo version.
