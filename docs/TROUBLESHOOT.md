# Troubleshooting Guide --- AI Observability Stack

> **Project:** `ai-observability`\
> **Environment:** Windows + PowerShell + Docker Desktop / Docker
> Compose\
> **Components:** Spring Boot `book-management-app`, OpenTelemetry Java
> Agent, OpenTelemetry Collector, Prometheus, Loki, Tempo, Grafana, and
> `ai-observability-agent` using Spring AI + Ollama.
>
> This guide consolidates the issues encountered so far and adds a
> repeatable end-to-end verification checklist. Commands assume the
> Compose file is under `C:\Satya\AI\ai-observability\docker`. If your
> Compose filename or service names differ, adjust the commands
> accordingly.

------------------------------------------------------------------------

## 1. Start here: confirm the correct directory and Compose file

A previous error was:

``` text
no configuration file provided: not found
```

This means Docker Compose did not find a Compose file in the current
directory. It does **not** mean the target container or URL failed.

``` powershell
cd C:\Satya\AI\ai-observability\docker
Get-ChildItem -Name *compose*.y*ml
docker compose config --services
docker compose ps
```

If the Compose file is in another directory, either `cd` to that
directory or pass the file explicitly:

``` powershell
docker compose -f .\docker-compose.yml ps
```

Use the exact filename shown by `Get-ChildItem`. Run Compose commands
from the folder containing that file unless you use `-f`.

### Baseline status

``` powershell
docker compose ps -a
docker compose logs --tail=100
```

Confirm that the expected services exist and are running. A container
being `Up` does not, by itself, prove that its application endpoint or
telemetry pipeline is healthy.

------------------------------------------------------------------------

## 2. Application and component map

  --------------------------------------------------------------------------------------------------------------
  Component                  Typical address from       Address to use from another Compose    Main
                             Windows                    container                              responsibility
  -------------------------- -------------------------- -------------------------------------- -----------------
  `book-management-app`      `http://localhost:8080`    `http://book-management-app:8080`      Spring Boot
                                                                                               sample
                                                                                               application

  OpenTelemetry Collector    `localhost:4317`,          `otel-collector:4317` (gRPC),          Receives and
                             `localhost:4318` if        `otel-collector:4318` (HTTP/protobuf)  routes telemetry
                             published                                                         

  Collector Prometheus       Often `localhost:8889` if  Collector's configured metrics         Exposes collected
  exporter                   published                  endpoint                               metrics for
                                                                                               scraping

  Prometheus                 `http://localhost:9090`    `http://prometheus:9090`               Metrics storage
                                                                                               and queries

  Loki                       `http://localhost:3100`    `http://loki:3100`                     Log storage and
                                                                                               queries

  Tempo                      `http://localhost:3200`    `http://tempo:3200`                    Trace storage and
                                                                                               queries

  Grafana                    `http://localhost:3000`    `http://grafana:3000`                  Explore and
                                                                                               dashboards

  `ai-observability-agent`   `http://localhost:8085`    `http://ai-observability-agent:8085`   AI-assisted
                                                                                               diagnosis

  Ollama on Windows host     `http://localhost:11434`   `http://host.docker.internal:11434`    Local LLM
                                                                                               inference
  --------------------------------------------------------------------------------------------------------------

These are the configured names used in this project; confirm actual
service names and port mappings with `docker compose config` and
`docker compose ps`.

**Important networking rule:** `localhost` inside a container points to
that same container. For Compose-to-Compose communication use the
service name; for a container reaching a service on the Windows host,
use `host.docker.internal`.

------------------------------------------------------------------------

## 3. First-response diagnostics

Run these before changing configuration:

``` powershell
docker compose config
docker compose ps -a
docker compose logs --tail=150 book-management-app
docker compose logs --tail=150 otel-collector
docker compose logs --tail=150 prometheus
docker compose logs --tail=150 loki
docker compose logs --tail=150 tempo
docker compose logs --tail=150 grafana
docker compose logs --tail=150 ai-observability-agent
```

If a service is optional or not enabled in the current Compose profile,
Compose may report that it cannot find that service. Check the
configured profiles and services:

``` powershell
docker compose config --services
docker compose config
```

For a targeted time window:

``` powershell
docker compose logs --since=10m otel-collector
```

Do not paste secrets, API keys, passwords, or tokens into issue reports.

------------------------------------------------------------------------

## 4. Docker Compose file and bind-mount problems

### Symptom: `no configuration file provided: not found`

**Likely cause:** Running `docker compose` from a directory without the
Compose YAML file.

**Check:**

``` powershell
Get-Location
Get-ChildItem -Force
Get-ChildItem -Name *compose*.y*ml
```

Then change to the correct folder or use
`docker compose -f <path-to-compose-file> ...`.

### Symptom: a config file is reported as a directory, or the container cannot read it

A bind mount such as `./grafana/datasources.yaml` may accidentally refer
to a directory created by an earlier command, rather than a YAML file.

**Check the host path:**

``` powershell
Test-Path .\grafana\datasources.yaml -PathType Leaf
Get-Item .\grafana\datasources.yaml -ErrorAction SilentlyContinue |
    Format-List FullName,Mode,Length,Attributes
Get-ChildItem .\grafana
```

Repeat the checks with the exact path used by each mount, especially:

-   `otel/otel-collector-config.yaml`
-   `grafana/datasources.yaml`
-   Prometheus configuration
-   Loki configuration
-   Tempo configuration

Inspect the resolved Compose configuration and confirm each left-hand
mount path exists and is the expected file or directory:

``` powershell
docker compose config
```

Create or edit a real file if a file mount is expected. Do not blindly
delete a path until you have verified whether it contains configuration
or data.

------------------------------------------------------------------------

## 5. `book-management-app`: application health and OpenTelemetry Java Agent

### Verify the application first

``` powershell
curl.exe -i http://localhost:8080/actuator/health
curl.exe -i http://localhost:8080/actuator/info
curl.exe -i http://localhost:8080/actuator/prometheus
```

If these fail, inspect the application logs before troubleshooting
Grafana or the AI agent:

``` powershell
docker compose logs --tail=200 book-management-app
```

Check that: - The application starts successfully and listens on port
`8080`. - The container port and host port mapping match the Compose
configuration. - Actuator endpoints are exposed as intended. - The H2
datasource and JPA initialization complete without errors. - The Java
Agent JAR path in the container exists and is readable. - The
application's `OTEL_*` environment variables match the intended
Collector receiver.

### Recommended OTLP environment for an app container

For the app and Collector on the same Compose network, the endpoint
should use the **Collector service name**, not `localhost`:

``` yaml
environment:
  OTEL_SERVICE_NAME: book-management-app
  OTEL_RESOURCE_ATTRIBUTES: service.name=book-management-app,deployment.environment=development
  OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4318
  OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
  OTEL_TRACES_EXPORTER: otlp
  OTEL_METRICS_EXPORTER: otlp
```

Only enable the logs exporter after the application is actually
configured to emit OTLP logs:

``` yaml
  OTEL_LOGS_EXPORTER: otlp
```

The exact settings must be merged into the existing app service, not
pasted as a duplicate `environment:` block. If your Collector uses only
gRPC, use the matching gRPC endpoint/protocol instead.

### Symptom: Java Agent cannot connect to `localhost:4318`

An error such as:

``` text
Failed to connect to localhost/[::1]:4318
```

means the app is trying to send telemetry to port `4318` inside its own
container.

**Fix:** Set `OTEL_EXPORTER_OTLP_ENDPOINT` to
`http://otel-collector:4318` for HTTP/protobuf when the Collector
service is named `otel-collector` and both containers share a Compose
network.

Check the relevant values:

``` powershell
docker compose exec book-management-app sh -c "env | sort | grep '^OTEL_'"
docker compose logs --since=10m otel-collector
```

If `sh`, `grep`, or another utility is absent in the app image, inspect
the Compose YAML and use the container logs instead.

### Confirm OTLP ports

The Collector commonly listens on `4317` for OTLP/gRPC and `4318` for
OTLP/HTTP. Confirm the actual receiver configuration and container
ports:

``` powershell
docker compose ps otel-collector
docker compose logs --tail=150 otel-collector
```

If testing from Windows, a published host port can be checked with:

``` powershell
Test-NetConnection localhost -Port 4318
```

This only checks host-to-published-port connectivity. It does not prove
that the app is configured to use the Collector or that the Collector
exports successfully.

------------------------------------------------------------------------

## 6. OpenTelemetry Collector: receivers, processors, exporters, pipelines

The Collector is the central telemetry routing point. Validate all four
stages:

1.  **Receivers** accept telemetry from the app.
2.  **Processors** process/batch telemetry.
3.  **Exporters** send telemetry to the backend.
4.  **Service pipelines** connect receivers, processors, and exporters
    for each signal.

Inspect the real configuration file and logs:

``` powershell
Get-Content .\otel\otel-collector-config.yaml
docker compose logs --tail=250 otel-collector
docker compose restart otel-collector
docker compose logs --since=2m otel-collector
```

After editing a mounted configuration file, recreate the Collector if
necessary:

``` powershell
docker compose up -d --force-recreate otel-collector
docker compose logs --tail=200 otel-collector
```

### Checklist for the Collector configuration

-   [ ] OTLP receiver is configured for the protocol(s) used by the app.
-   [ ] HTTP receiver listens on `0.0.0.0:4318` if app telemetry uses
    HTTP/protobuf.
-   [ ] gRPC receiver listens on `0.0.0.0:4317` if app telemetry uses
    gRPC.
-   [ ] A **traces** pipeline is defined and exports to Tempo.
-   [ ] A **metrics** pipeline is defined and exposes or exports metrics
    as intended.
-   [ ] A **logs** pipeline is defined if OTLP logs are expected.
-   [ ] Every component referenced by a pipeline is defined under the
    appropriate configuration section.
-   [ ] Exporter endpoints use Compose service DNS names, not
    container-local `localhost`.
-   [ ] The Collector startup logs contain no configuration parsing or
    component initialization errors.

### Signal flow to validate

``` text
Spring Boot + Java Agent
        |
        | OTLP (HTTP 4318 or gRPC 4317)
        v
OpenTelemetry Collector
        |                  |                    |
        v                  v                    v
      Tempo            Prometheus             Loki
     (traces)            (metrics)             (logs)
        \                  |                    /
         \                 |                   /
                       Grafana
```

This is the intended logical flow. The exact exporter configuration
determines whether each arrow is active.

### Symptom: telemetry arrives but does not appear in a backend

Collector debug logs showing `ResourceSpans`, `ResourceMetrics`, or
`ResourceLogs` prove the Collector received that signal. They do **not**
prove that its exporter successfully delivered it to the backend. Look
for exporter errors, retry messages, and backend readiness.

------------------------------------------------------------------------

## 7. Traces and Tempo

Previously, the Java Agent attempted to export spans to
`localhost:4318`, and Tempo returned a connection-refused error on its
query endpoint (`:3200`) during another test.

### Check the trace path

1.  Confirm the app's OTLP endpoint targets `otel-collector`, not
    `localhost`.
2.  Confirm the Collector traces pipeline exports to the Tempo **OTLP
    ingestion** endpoint, usually `tempo:4317` for gRPC or `tempo:4318`
    for HTTP, depending on the exporter.
3.  Confirm the Tempo service and container are running.
4.  Confirm Tempo's query API is reachable on port `3200`.
5.  Generate a new request to the app, then search for the new trace in
    Grafana Explore using Tempo.

``` powershell
docker compose ps tempo otel-collector book-management-app
docker compose logs --tail=200 tempo
docker compose logs --since=5m otel-collector
curl.exe -i http://localhost:3200/ready
curl.exe -i http://localhost:3200/api/echo
```

The readiness path and query API behavior can vary with Tempo
configuration/version. A `connection refused` error means the TCP
connection was not accepted at that address/port at that time; inspect
Tempo startup logs, listening ports, and service configuration before
changing Grafana.

**Do not confuse ports:** `4317`/`4318` are commonly OTLP ingestion
ports; `3200` is Tempo's query/API port. Sending OTLP data to `3200` is
not the same as querying Tempo.

------------------------------------------------------------------------

## 8. Metrics and Prometheus

### Confirm the app exposes metrics

``` powershell
curl.exe -i http://localhost:8080/actuator/prometheus
```

### Confirm Prometheus is reachable

Open `http://localhost:9090`, or run:

``` powershell
curl.exe -i http://localhost:9090/-/ready
```

In Prometheus, inspect **Status → Targets** and confirm the expected
scrape target is `UP`. Check the scrape target address and path: a
target scraped from inside the Prometheus container should use the
appropriate Compose DNS name, not the Windows host's `localhost`.

If Prometheus scrapes the Collector's Prometheus exporter, confirm
that: - The Collector metrics exporter is configured and enabled. - The
configured exporter endpoint/port matches the Prometheus scrape
target. - The metrics pipeline includes the intended exporter. -
Prometheus is configured to scrape that endpoint.

### Collector metric type conflicts

A Collector log showing a metric type conflict or duplicate metric from
JVM, HTTP, or Hikari instrumentation may affect some metric exports
without necessarily stopping traces. Identify the exact metric name,
receiver, exporter, and pipeline in the error before applying a filter
or changing instrumentation. Do not disable all metrics merely because
one metric fails.

------------------------------------------------------------------------

## 9. Logs and Loki: the most important distinction

A manual Loki ingestion test succeeded previously: the log
`Test log sent directly to Loki` appeared in Grafana Explore when
queried with:

``` logql
{job="manual-test"}
```

That established that **Loki ingestion and Grafana querying worked for
the manual test**. It did not establish that `book-management-app` logs
were reaching Loki.

### Why app logs may be missing

Application logs appearing in `docker compose logs` or console output do
**not** automatically mean they are exported to Loki. An OTLP Collector
does not automatically ingest every container's stdout. The application
needs a configured log export path, or the Collector needs a separate
container-log collection path.

Common choices are:

-   **OTLP application logs:** configure a supported logging
    appender/bridge or SDK so app logs are emitted as OTLP logs; the
    Collector needs an OTLP receiver, a logs pipeline, and a
    Loki-compatible exporter.
-   **Container stdout/stderr collection:** configure a supported
    container log receiver/agent and appropriate Docker log
    access/mounts. This is a separate path from OTLP.
-   **Direct Loki push:** useful for testing Loki itself, but not a
    substitute for application log collection.

### Check in this order

1.  Do app logs appear in `docker compose logs book-management-app`?
2.  Is the app actually emitting logs through the selected OTLP or
    container-log path?
3.  Does the Collector have a logs receiver and a
    `service.pipelines.logs` pipeline?
4.  Do Collector logs show received `ResourceLogs`?
5.  Does the Collector's logs exporter successfully deliver to Loki?
6.  Can Grafana Explore query Loki for the app's actual labels?

For Loki 3.x native OTLP ingestion, an OTLP HTTP exporter is commonly
configured to use Loki's OTLP endpoint (often `http://loki:3100/otlp`).
Validate the endpoint against your Loki version and exporter
configuration. Do not assume the logs pipeline exists just because the
traces and metrics pipelines work.

### Query in Grafana Explore

Start by exploring the available Loki labels. Then try a query matching
the labels actually present, for example:

``` logql
{service_name="book-management-app"}
```

The exact label mapping depends on Loki's OTLP configuration and
version. If this returns nothing, inspect available labels and recent
Collector logs instead of assuming `service_name` is the correct label.

The `manual-test` stream is only a control test; do not use it as proof
that app logs are flowing.

------------------------------------------------------------------------

## 10. Grafana and datasource provisioning

Grafana is the visualization/query interface; it does not create missing
telemetry. Confirm each backend has data independently before debugging
a dashboard.

### Datasource provisioning

A provisioning file such as `grafana/datasources.yaml` should be a real
YAML file, mounted at the path expected by Grafana's provisioning
configuration. An example is:

``` yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true

  - name: Loki
    uid: loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: true

  - name: Tempo
    uid: tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    editable: true
```

These URLs are correct only if those are the actual Compose service
names and the services share the Compose network.

### Checks

``` powershell
docker compose logs --tail=200 grafana
Test-Path .\grafana\datasources.yaml -PathType Leaf
```

Then open `http://localhost:3000` and verify each datasource under
**Connections / Data sources** (menu labels may vary by Grafana
version). Use each datasource's **Save & test** action.

-   Prometheus: run a known metric query.
-   Loki: query the manual test stream, then query app logs.
-   Tempo: generate a new app request and search for its trace.

A datasource can be healthy while having no matching telemetry. Test
connectivity and data availability separately.

------------------------------------------------------------------------

## 11. AI diagnosis agent and Ollama

The AI agent is a separate service from the observability backends.
Troubleshoot the HTTP endpoint, the agent's Spring configuration, and
its model connection as separate layers.

### A. Confirm Ollama runs on Windows

``` powershell
curl.exe -i http://localhost:11434/api/tags
```

The response should list locally available models. The configured model
in `application.yml` is:

``` yaml
spring:
  ai:
    model:
      chat: ollama
    ollama:
      base-url: http://host.docker.internal:11434
      chat:
        model: qwen2.5:7b
        options:
          temperature: 0.2
```

If `qwen2.5:7b` is not listed:

``` powershell
ollama pull qwen2.5:7b
```

Inside the agent container, `localhost:11434` means the agent container
itself. If Ollama runs on Windows, use `host.docker.internal:11434`. If
Ollama instead runs as a Compose service, use its Compose service name,
for example `http://ollama:11434`, and ensure that service exists.

### B. Confirm the agent starts

If the agent is behind a Compose profile, activate the appropriate
profile when starting it. Check the actual profile in
`docker compose config`.

``` powershell
docker compose ps -a
docker compose logs --tail=250 ai-observability-agent
```

After changing the agent's source code, rebuild the image if the
Dockerfile packages a JAR into the image:

``` powershell
docker compose up -d --build --force-recreate ai-observability-agent
```

A no-cache rebuild will not fix a runtime configuration or missing
Spring bean unless the underlying cause is changed.

### C. Known issue: missing `KubernetesClient` bean

An earlier startup failure reported that a `KubernetesTools` component
required a `KubernetesClient` bean that was not available.

This is an application dependency/configuration problem, not an Ollama
network problem. If the agent is running in local Docker Compose and
Kubernetes diagnostics are not configured, either: - make the Kubernetes
integration conditional/optional for the Compose profile, or - configure
and provide the required Kubernetes client bean and credentials for the
environment where it is expected to run.

Do not assume a Kubernetes client is available merely because Docker
Desktop has Kubernetes enabled. Inspect the actual Spring configuration,
bean conditions, and startup exception before changing code.

### D. Known issue: `400 Bad Request` from `/api/agent/diagnose`

A request previously returned:

``` json
{
  "status": 400,
  "error": "Bad Request",
  "path": "/api/agent/diagnose"
}
```

This response does not, by itself, identify the cause. It may be caused
by JSON deserialization, request DTO validation, a missing/incorrect
field, or explicit controller validation. A separate Ollama connection
failure can occur later during model invocation, but it does not
automatically explain the 400.

Use a PowerShell-native JSON body to avoid shell quoting problems:

``` powershell
$body = @{
    question = "Check metrics, logs and traces for book-management-app. Identify the most likely cause of slow or failing requests."
} | ConvertTo-Json -Depth 5 -Compress

Invoke-RestMethod `
    -Uri "http://localhost:8085/api/agent/diagnose" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

If it still returns 400: 1. Read the agent logs at the same timestamp.
2. Inspect the controller mapped to `/api/agent/diagnose` and its
`@RequestBody` DTO. 3. Verify the exact required JSON fields and
validation annotations. 4. Check whether the endpoint expects a
different request schema. 5. Use Swagger/OpenAPI if enabled, for example
`http://localhost:8085/swagger-ui/index.html` or
`http://localhost:8085/v3/api-docs`.

Do not diagnose the 400 from the HTTP status alone.

### E. Verify agent access to observability backends

The agent configuration uses defaults similar to:

``` yaml
agent:
  prometheus:
    url: ${PROMETHEUS_URL:http://prometheus:9090}
  loki:
    url: ${LOKI_URL:http://loki:3100}
  tempo:
    url: ${TEMPO_URL:http://tempo:3200}
```

These are appropriate for Compose only if the service names and network
match. Confirm the agent can reach the backends, and ensure the agent's
implementation uses the configured properties. A URL in YAML has no
effect if the code is not binding to that property.

Also, Kubernetes events will not be available in a plain Docker Compose
environment unless the agent has a configured Kubernetes client, valid
credentials, and access to a Kubernetes API. Do not interpret missing
Kubernetes events as evidence that no application problem exists.

------------------------------------------------------------------------

## 12. End-to-end smoke test

Run the following sequence after the stack is up. It checks the path
from application health through telemetry storage and AI diagnosis.

### Step 1 --- Confirm containers

``` powershell
cd C:\Satya\AI\ai-observability\docker
docker compose ps
```

### Step 2 --- Confirm the application and its metrics endpoint

``` powershell
curl.exe -i http://localhost:8080/actuator/health
curl.exe -i http://localhost:8080/actuator/prometheus
```

### Step 3 --- Generate traffic

Use a valid endpoint in your application, for example the actual
book-management API endpoint. Do not assume a particular CRUD path
without checking the controller mappings. Generate several requests so
there are fresh spans and metrics to inspect.

### Step 4 --- Check Collector ingestion/export

``` powershell
docker compose logs --since=5m otel-collector
```

Look for received traces/metrics/logs and any exporter errors. Confirm
each signal independently.

### Step 5 --- Check each backend

-   Prometheus: `http://localhost:9090` → check **Status → Targets**,
    then run a metric query.
-   Loki: `http://localhost:3100/ready`, then query in Grafana Explore.
-   Tempo: `http://localhost:3200/ready`, then search for a newly
    generated trace.
-   Grafana: `http://localhost:3000` → test datasources and query each
    backend.

### Step 6 --- Check the AI model and agent

``` powershell
curl.exe -i http://localhost:11434/api/tags
docker compose logs --tail=150 ai-observability-agent
```

Then submit the diagnosis request with the PowerShell `ConvertTo-Json`
command from section 11.

------------------------------------------------------------------------

## 13. Quick symptom-to-check reference

  ---------------------------------------------------------------------------------------------
  Symptom                            First place to check          Likely area
  ---------------------------------- ----------------------------- ----------------------------
  `no configuration file provided`   `Get-Location`, Compose       Wrong working directory or
                                     filename                      missing `-f`

  Config file mount error / file     `Test-Path`, `Get-Item`,      Invalid bind-mount source
  behaves like directory             `docker compose config`       path/type

  App health endpoint fails          App logs and `8080` mapping   Application startup, port,
                                                                   DB, actuator

  Agent reports connection to        App `OTEL_*` settings         Wrong OTLP destination
  `localhost:4318` failed                                          inside container

  Collector receives spans but Tempo Collector trace               Trace exporter, endpoint,
  has no trace                       pipeline/exporter; Tempo logs backend readiness

  Tempo `:3200` connection refused   Tempo logs, ports, readiness  Tempo not listening/ready or
                                                                   wrong address

  Prometheus target is `DOWN`        Prometheus **Status →         Wrong scrape URL/path,
                                     Targets**                     network, exporter

  Manual Loki log appears but app    App log export path +         App logs are not being
  logs do not                        Collector logs pipeline       collected/exported

  Grafana datasource test fails      Grafana logs and service DNS  Wrong URL, service/network,
                                                                   backend readiness

  Agent cannot call                  `spring.ai.ollama.base-url`   Container is pointing to
  `localhost:11434`                                                itself instead of Windows
                                                                   host

  Model not found                    `GET /api/tags`               Configured model not
                                                                   installed or model name
                                                                   mismatch

  Agent fails at startup requiring   Spring bean/configuration     Kubernetes integration not
  `KubernetesClient`                 logs                          configured/conditional

  `/api/agent/diagnose` returns 400  Controller, DTO, validation   Invalid request shape or
                                     error logs                    validation

  Agent returns diagnosis without    Kubernetes client and API     Kubernetes integration
  Kubernetes events                  access                        unavailable in Compose

  One or more metrics fail to export Exact Collector metric error  Metric
                                                                   conflict/type/temporality;
                                                                   investigate the named metric
  ---------------------------------------------------------------------------------------------

------------------------------------------------------------------------

## 14. Configuration change workflow

For each issue, use this sequence to avoid introducing multiple
unrelated changes:

1.  **Capture the exact error and timestamp.**

2.  **Identify the failing layer:** app, Collector receiver, Collector
    exporter, backend, Grafana datasource, or AI agent.

3.  **Inspect current configuration before editing it.**

4.  **Change one thing at a time.**

5.  **Validate YAML and Compose expansion:**

    ``` powershell
    docker compose config
    ```

6.  **Recreate only the affected service:**

    ``` powershell
    docker compose up -d --force-recreate <service-name>
    ```

    Add `--build` if code or the image build inputs changed.

7.  **Check the new logs:**

    ``` powershell
    docker compose logs --since=2m <service-name>
    ```

8.  **Repeat the specific test that failed.**

9.  **Confirm downstream data in the backend, not only that the
    container is running.**

10. **Record the final working configuration and the reason for the
    change.**

Avoid repeatedly rebuilding every service for a configuration-only
change. Avoid `docker compose down -v` unless you intentionally want to
remove named volumes and understand the data-loss consequences.

------------------------------------------------------------------------

## 15. Final health checklist

Use this checklist whenever the complete application is expected to
work.

-   [ ] Compose file is found and `docker compose config` completes
    successfully.
-   [ ] All expected containers are running and restart counts are
    stable.
-   [ ] `book-management-app` starts and `/actuator/health` returns a
    healthy response.
-   [ ] The app's Java Agent is loaded and its OTLP endpoint targets the
    Collector service.
-   [ ] Collector receiver ports and protocols match the app's exporter
    settings.
-   [ ] Collector traces pipeline exports successfully to Tempo.
-   [ ] Collector metrics pipeline and Prometheus scrape target are
    healthy.
-   [ ] App logs have a real collection/export path; they are not
    assumed to flow from stdout automatically.
-   [ ] Collector logs pipeline and Loki exporter are configured if OTLP
    logs are intended.
-   [ ] Grafana Prometheus, Loki, and Tempo datasources pass their
    connectivity checks.
-   [ ] A fresh app request appears as a trace in Tempo.
-   [ ] Relevant application metrics appear in Prometheus.
-   [ ] App logs---not just the manual test stream---are queryable in
    Loki.
-   [ ] Ollama's tags endpoint is reachable from Windows and the
    configured model exists.
-   [ ] The agent starts without missing-bean errors.
-   [ ] The diagnosis endpoint request matches the controller's expected
    DTO.
-   [ ] The agent's backend URLs point to reachable Compose services.
-   [ ] Kubernetes-related diagnostics are treated as unavailable unless
    Kubernetes access is actually configured.

**Operating principle:** verify the signal at each hop---source →
Collector receiver → Collector exporter → backend → Grafana/agent query.
This quickly isolates whether a failure is caused by generation,
transport, ingestion, storage, or querying.
