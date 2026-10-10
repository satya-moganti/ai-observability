# AI Observability Agent --- Testing Guide

This document describes ways to test the Spring Boot AI Observability
Agent from Windows Command Prompt, PowerShell, and `curl`.

The agent is expected to use: - **Prometheus** for metrics queries -
**Loki** for log queries - **Ollama** for local model inference, if
configured - **Spring Boot** as the agent API

The application has been observed listening on port `8085`, and a
candidate diagnosis route has been used:

``` text
POST http://localhost:8085/api/agent/diagnose
```

**Important:** The exact route, request DTO, JSON field names, and
required fields must match `AgentController.java` and its request model.
The example JSON below is only a starting point. A previous request
using `{"query":"..."}` returned HTTP 400, so verify the expected body
before treating that request as valid.

## 1. Start the application

From the directory containing the Compose file:

``` powershell
docker compose up --build -d
docker compose ps
docker compose logs -f --tail=100 ai-observability-agent
```

Open a second terminal to run tests. To stop watching logs, press
`Ctrl+C`; the application remains running.

## 2. Test basic connectivity

### PowerShell

``` powershell
Test-NetConnection localhost -Port 8085
```

A successful TCP test should report `TcpTestSucceeded : True`.

Check the root URL (a 404 can be normal if no root endpoint exists):

``` powershell
Invoke-WebRequest -Uri "http://localhost:8085/" -Method GET
```

### CMD

``` cmd
curl.exe -i http://localhost:8085/
```

### curl

``` bash
curl -i http://localhost:8085/
```

A `404 Not Found` at `/` does not necessarily mean the application is
down. Use a configured health endpoint for a meaningful health check.

## 3. Test Spring Boot Actuator health

If `health` is enabled and exposed in the application configuration:

### PowerShell

``` powershell
Invoke-RestMethod -Uri "http://localhost:8085/actuator/health" -Method GET
```

### CMD

``` cmd
curl.exe -i http://localhost:8085/actuator/health
```

### curl

``` bash
curl -i http://localhost:8085/actuator/health
```

Expected when available: HTTP `200` and a JSON health response. If this
returns `404`, verify `spring-boot-starter-actuator`, the management
endpoint exposure settings, and any custom management port/path.

## 4. Test the diagnosis endpoint

First inspect `AgentController.java` to confirm: - The `@RequestMapping`
and `@PostMapping` path - The request DTO or method parameter type -
Required JSON properties and validation constraints - Whether
authentication is required

The following is an example only, assuming the route and body field are
correct.

### PowerShell --- JSON body

``` powershell
$body = @{
    question = "Check the health of book-management-app. Analyze Prometheus metrics and Loki error logs."
} | ConvertTo-Json -Compress

Write-Host "JSON being sent: $body"

Invoke-RestMethod `
    -Uri "http://localhost:8085/api/agent/diagnose" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

### CMD --- `curl.exe`

``` cmd
curl.exe -i -X POST "http://localhost:8085/api/agent/diagnose" -H "Content-Type: application/json" -d "{\"query\":\"Check the health of book-management-app. Analyze Prometheus metrics and Loki error logs.\"}"
```

### curl --- Linux, macOS, or Git Bash

``` bash
curl -i -X POST "http://localhost:8085/api/agent/diagnose" \
  -H "Content-Type: application/json" \
  -d '{"query":"Check the health of book-management-app. Analyze Prometheus metrics and Loki error logs."}'
```

Do not repeatedly change the payload at random if the response is 400.
Compare it with the actual request DTO and inspect the response body and
application logs.

## 5. Test Prometheus directly

If Prometheus publishes host port `9090`, test its readiness endpoint.

### PowerShell

``` powershell
Invoke-WebRequest -Uri "http://localhost:9090/-/ready" -Method GET
```

Test a simple PromQL query:

``` powershell
$query = 'up'
$url = "http://localhost:9090/api/v1/query?query=$([uri]::EscapeDataString($query))"
Invoke-RestMethod -Uri $url -Method GET
```

Test the application HTTP 5xx metric (adjust labels/metric name to match
your Prometheus data):

``` powershell
$query = 'http_server_requests_seconds_count{job="book-management-app",status=~"5.."}'
$url = "http://localhost:9090/api/v1/query?query=$([uri]::EscapeDataString($query))"
Invoke-RestMethod -Uri $url -Method GET
```

### CMD

``` cmd
curl.exe -i "http://localhost:9090/-/ready"
curl.exe -G "http://localhost:9090/api/v1/query" --data-urlencode "query=up"
```

### curl

``` bash
curl -i http://localhost:9090/-/ready
curl -G http://localhost:9090/api/v1/query --data-urlencode 'query=up'
```

A valid API response typically includes `"status":"success"`. An empty
result vector can mean the query is valid but no matching series
currently exist.

From inside the agent container, use the Compose service name, typically
`http://prometheus:9090`, rather than `localhost:9090`.

## 6. Test Loki directly

If Loki publishes host port `3100`, check readiness:

### PowerShell

``` powershell
Invoke-WebRequest -Uri "http://localhost:3100/ready" -Method GET
```

Query recent logs. Replace the selector with labels actually present in
your Loki streams:

``` powershell
$query = '{service_name="book-management-app"}'
$url = "http://localhost:3100/loki/api/v1/query_range?query=$([uri]::EscapeDataString($query))&limit=20"
Invoke-RestMethod -Uri $url -Method GET
```

### CMD

``` cmd
curl.exe -i http://localhost:3100/ready
curl.exe -G "http://localhost:3100/loki/api/v1/query_range" --data-urlencode "query={service_name=\"book-management-app\"}" --data-urlencode "limit=20"
```

### curl

``` bash
curl -i http://localhost:3100/ready
curl -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={service_name="book-management-app"}' \
  --data-urlencode 'limit=20'
```

If Loki returns no streams, inspect the labels that actually exist
before changing the selector. A successful HTTP response with no
matching streams is different from a Loki connectivity failure.

## 7. Test Ollama directly

Only run these commands if Ollama is running and its API is reachable
from the Windows host on port `11434`.

### PowerShell

``` powershell
Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -Method GET
```

Generate a small response (replace `llama3.2` with a model installed
locally):

``` powershell
$body = @{
    model = "llama3.2"
    prompt = "Reply with the word OK."
    stream = $false
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:11434/api/generate" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

### CMD

``` cmd
curl.exe -i http://localhost:11434/api/tags
curl.exe -X POST http://localhost:11434/api/generate -H "Content-Type: application/json" -d "{\"model\":\"llama3.2\",\"prompt\":\"Reply with the word OK.\",\"stream\":false}"
```

### curl

``` bash
curl -i http://localhost:11434/api/tags
curl -X POST http://localhost:11434/api/generate \
  -H 'Content-Type: application/json' \
  -d '{"model":"llama3.2","prompt":"Reply with the word OK.","stream":false}'
```

If Ollama runs in a different container, use the correct Compose service
hostname from the agent container. The model name must already be
installed.

## 8. Check service logs and container status

``` powershell
docker compose ps --all
docker compose logs --tail=200 ai-observability-agent
docker compose logs --tail=100 prometheus
docker compose logs --tail=100 loki
docker compose logs --tail=100 ollama
```

The last three commands apply only if those are the service names in the
Compose file.

Follow logs while sending a request:

``` powershell
docker compose logs -f --tail=100 ai-observability-agent
```

In another terminal, send the request. If no new logs appear, verify the
request is reaching the correct published port and route, and check the
controller's logging configuration.

## 9. Common HTTP results

  -----------------------------------------------------------------------
  Result                              Meaning / next check
  ----------------------------------- -----------------------------------
  `200 OK`                            Request succeeded; inspect the
                                      response content.

  `400 Bad Request`                   Body shape, JSON parsing, or
                                      validation problem; inspect
                                      response body and controller DTO.

  `401 Unauthorized`                  Authentication is required or
                                      credentials are missing/invalid.

  `403 Forbidden`                     Request reached a security rule but
                                      access was denied.

  `404 Not Found`                     Route/path may be wrong, or the
                                      resource does not exist.

  `405 Method Not Allowed`            Path may exist but does not support
                                      the HTTP method used.

  `500 Internal Server Error`         Application-side exception; inspect
                                      agent logs and stack trace.

  Connection refused / timeout        Container, port mapping, app
                                      startup, or network issue.
  -----------------------------------------------------------------------

## 10. Suggested end-to-end test sequence

1.  `docker compose config` --- validate configuration.
2.  `docker compose ps` --- confirm expected containers are running.
3.  `Test-NetConnection localhost -Port 8085` --- confirm host
    connectivity to the agent.
4.  Call `/actuator/health` if it is exposed.
5.  Query Prometheus with `up`.
6.  Query Loki with a selector based on real labels.
7.  Test Ollama's `/api/tags` if it is part of the setup.
8.  Call the agent endpoint using the exact DTO expected by
    `AgentController.java`.
9.  Check the agent logs and verify the response includes the expected
    diagnosis or a useful tool error.

## Important note about the current HTTP 400

The earlier example request with `{"query":"..."}` returned HTTP 400.
This guide cannot determine the correct request JSON without the
controller/request DTO. Use the PowerShell error-body snippet above and
inspect `AgentController.java` before assuming `query` is the correct
property.
