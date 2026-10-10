# Docker Compose Commands --- AI Observability Application

This guide covers common Docker Compose commands for building, starting,
stopping, inspecting, and troubleshooting the AI Observability
application.

> Run these commands from the directory containing your `compose.yaml`
> or `docker-compose.yml` file. In this project, that may be
> `C:\Satya\AI\ai-observability\docker`. Adjust the directory and
> service names if your Compose file differs.

## 1. Open the project directory

Check the Compose configuration:

``` powershell
docker compose config
```

List services defined in the Compose file:

``` powershell
docker compose config --services
```

## 2. Build images

Build or rebuild images without starting containers:

``` powershell
docker compose build
```

Build without using the build cache:

``` powershell
docker compose build --no-cache
```

Build and start/recreate services:

``` powershell
docker compose up --build -d
```

Build and start only the agent service (replace the name if your Compose
service differs):

``` powershell
docker compose up --build -d ai-observability-agent
```

## 3. Start and stop containers

Start services in the background:

``` powershell
docker compose up -d
```

Start and show live logs in the current terminal:

``` powershell
docker compose up
```

Stop containers without removing them:

``` powershell
docker compose stop
```

Stop only the agent:

``` powershell
docker compose stop ai-observability-agent
```

Start previously stopped services:

``` powershell
docker compose start
```

Restart all services:

``` powershell
docker compose restart
```

Restart only the agent:

``` powershell
docker compose restart ai-observability-agent
```

Stop and remove Compose containers and networks:

``` powershell
docker compose down
```

Stop and remove containers, networks, and Compose-managed anonymous
volumes:

``` powershell
docker compose down --volumes
```

**Caution:** `docker compose down --volumes` can delete data stored in
Compose-managed volumes. Do not use it if you need to preserve
Prometheus, Loki, Grafana, or other persistent data.

Remove images built by Compose as well as containers and networks:

``` powershell
docker compose down --rmi local
```

## 4. Check status and logs

List running Compose services:

``` powershell
docker compose ps
```

Include stopped containers:

``` powershell
docker compose ps --all
```

Follow logs from all services:

``` powershell
docker compose logs -f
```

Show the latest 100 lines:

``` powershell
docker compose logs --tail=100
```

Follow only the agent logs:

``` powershell
docker compose logs -f --tail=100 ai-observability-agent
```

Show logs for Prometheus, Loki, or Ollama (only if these service names
exist in your Compose file):

``` powershell
docker compose logs --tail=100 prometheus
docker compose logs --tail=100 loki
docker compose logs --tail=100 ollama
```

Exit live log viewing with `Ctrl+C`. This stops log streaming, not the
containers.

## 5. Execute commands inside a container

Open a shell inside the agent container (if the image contains `sh`):

``` powershell
docker compose exec ai-observability-agent sh
```

Check service environment/configuration without opening a shell:

``` powershell
docker compose exec ai-observability-agent printenv
```

The exact service name must match `docker compose config --services`.

## 6. Validate container networking and ports

Show port mappings:

``` powershell
docker compose port ai-observability-agent 8085
```

Inspect the agent container:

``` powershell
docker compose ps ai-observability-agent
docker inspect $(docker compose ps -q ai-observability-agent)
```

From Windows, test whether port 8085 is reachable:

``` powershell
Test-NetConnection localhost -Port 8085
```

If the agent uses port 8085 inside the container, ensure the Compose
file publishes it, for example `8085:8085`. The actual mapping may
differ.

## 7. Rebuild after Java code changes

``` powershell
docker compose down
docker compose build --no-cache ai-observability-agent
docker compose up -d
docker compose logs -f --tail=100 ai-observability-agent
```

Usually, `docker compose up --build -d ai-observability-agent` is
sufficient; use `--no-cache` only when you need a clean image rebuild.

## 8. Clean up unused Docker resources

Show Docker disk usage:

``` powershell
docker system df
```

Remove unused build cache:

``` powershell
docker builder prune
```

Remove unused images, networks, and stopped containers:

``` powershell
docker system prune
```

Review Docker's confirmation prompt carefully. Avoid
`docker system prune --volumes` unless you understand which unused
volumes could be removed.

## 9. Quick command reference

  ------------------------------------------------------------------------------------------------
  Task                                Command
  ----------------------------------- ------------------------------------------------------------
  Validate Compose file               `docker compose config`

  List services                       `docker compose config --services`

  Build images                        `docker compose build`

  Build and start                     `docker compose up --build -d`

  Start                               `docker compose up -d`

  Live logs                           `docker compose logs -f`

  Agent logs                          `docker compose logs -f --tail=100 ai-observability-agent`

  Status                              `docker compose ps`

  Stop                                `docker compose stop`

  Restart                             `docker compose restart`

  Stop and remove containers/networks `docker compose down`

  Rebuild agent cleanly               `docker compose build --no-cache ai-observability-agent`
  ------------------------------------------------------------------------------------------------

## Troubleshooting checklist

1.  Run `docker compose config` and fix any configuration errors.
2.  Run `docker compose config --services` to confirm actual service
    names.
3.  Run `docker compose ps --all` to check for exited or unhealthy
    containers.
4.  Review `docker compose logs --tail=200 <service-name>`.
5.  Confirm the required ports are published and not occupied by another
    process.
6.  Confirm services use Compose service DNS names (for example,
    `http://prometheus:9090`) when communicating inside the Compose
    network. Use `localhost` from Windows only for ports published to
    the host.
