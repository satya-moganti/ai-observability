# Book Management System with OpenTelemetry Observability

A Spring Boot application for managing book details with full CRUD operations, integrated with OpenTelemetry for complete observability. This application supports Java 8 to Java 21 and can be built with both Maven and Gradle.

## Technology Stack

- **Framework:** Spring Boot 2.7.18
- **Language:** Java (8-21 compatible)
- **Database:** H2 (in-memory)
- **Build Tools:** Maven & Gradle
- **ORM:** Spring Data JPA
- **Validation:** Java Bean Validation
- **Observability:** OpenTelemetry (OTEL)
- **Metrics:** Micrometer + Prometheus
- **Logging:** Loki
- **Tracing:** Tempo
- **Visualization:** Grafana
- **Containerization:** Docker & Docker Compose


# 🚀 Deployment Options

### Option 1: Docker Deployment (Recommended - Full Observability Stack)

This option runs the entire stack: Application + OpenTelemetry Collector + Prometheus + Loki + Tempo + Grafana

```cmd
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f book-management-app

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

**Access URLs (Docker):**
- Application: http://localhost:8080
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- OTEL Collector: http://localhost:4318 (HTTP), http://localhost:4317 (gRPC)
- H2 Console: http://localhost:8080/h2-console

### Manual Deployment WITHOUT OpenTelemetry

Simple local development without observability features.

#### Using Maven

```cmd
# Build the project (automatically downloads OTEL agent)
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target\book-management-1.0.0.jar
```

#### Using Gradle

```cmd
# Build the project (automatically downloads OTEL agent)
gradle build

# Run the application
gradle bootRun

# Or run the JAR directly
java -jar build\libs\book-management-1.0.0.jar
```

#### Using Maven Wrapper (Windows)

```cmd
# Build and run
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

#### Using Gradle Wrapper (Windows)

```cmd
# Build and run
gradlew.bat build
gradlew.bat bootRun
```

## 📊 Observability Stack Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                     │
│         (with OpenTelemetry Java Agent)                      │
│                                                              │
│  • Auto-instrumentation of HTTP requests                     │
│  • Database queries tracing                                  │
│  • JVM metrics collection                                    │
│  • Application logs with trace context                       │
└──────────────┬──────────────────────────────────────────────┘
               │ OTLP (HTTP/gRPC)
               ▼
┌─────────────────────────────────────────────────────────────┐
│              OpenTelemetry Collector                         │
│                                                              │
│  • Receives traces, metrics, and logs                        │
│  • Processes and batches telemetry data                      │
│  • Routes to appropriate backends                            │
└──────┬───────────────┬────────────────┬─────────────────────┘
       │               │                │
       │ Traces        │ Metrics        │ Logs
       ▼               ▼                ▼
┌───────────┐   ┌─────────────┐   ┌──────────┐
│   Tempo   │   │ Prometheus  │   │   Loki   │
│           │   │             │   │          │
│ Trace     │   │ Metrics     │   │ Logs     │
│ Storage   │   │ Storage     │   │ Storage  │
└───────────┘   └─────────────┘   └──────────┘
       │               │                │
       └───────────────┴────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │    Grafana      │
              │                 │
              │  Unified        │
              │  Observability  │
              │  Dashboard      │
              └─────────────────┘
```

## API Endpoints

The application runs on `http://localhost:8080`

### Book Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/books` | Create a new book |
| GET | `/api/books` | Get all books |
| GET | `/api/books/{id}` | Get book by ID |
| PUT | `/api/books/{id}` | Update book by ID |
| DELETE | `/api/books/{id}` | Delete book by ID |
| GET | `/api/books/search?title={title}` | Search books by title |
| GET | `/api/books/author/{author}` | Get books by author |
| GET | `/api/books/category/{category}` | Get books by category |
| GET | `/api/books/isbn/{isbn}` | Get book by ISBN |

### Sample Request Body (POST/PUT)

```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0132350884",
  "description": "A Handbook of Agile Software Craftsmanship",
  "price": 45.99,
  "publicationYear": 2008,
  "availableQuantity": 100,
  "publisher": "Prentice Hall",
  "category": "Software Engineering"
}
```

### Sample Response

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0132350884",
  "description": "A Handbook of Agile Software Craftsmanship",
  "price": 45.99,
  "publicationYear": 2008,
  "availableQuantity": 100,
  "publisher": "Prentice Hall",
  "category": "Software Engineering"
}
```

## 🔍 What Gets Monitored



### Traces (via Tempo)
- HTTP request/response spans
- Database query execution
- Service-to-service calls
- Error tracking and stack traces
- Request latency breakdown

### Metrics (via Prometheus)
- HTTP request rates and durations
- JVM metrics (memory, GC, threads)
- Database connection pool stats
- Custom application metrics
- System resource utilization

### Logs (via Loki)
- Application logs with trace correlation
- Error and exception logs
- Request/response logging
- Database query logs
- Performance logs


#### Creating Dashboards:

**1. Application Metrics Dashboard:**
- Click "+" → "Dashboard" → "Add visualization"
- Select "Prometheus" datasource
- Use queries like:
  - `rate(http_server_requests_seconds_count[5m])` - Request rate
  - `http_server_requests_seconds_bucket` - Request duration histogram
  - `jvm_memory_used_bytes` - JVM memory usage
  - `jvm_gc_pause_seconds_sum` - GC pause time

**2. Logs Dashboard:**
- Click "Explore" → Select "Loki"
- Use queries like:
  - `{service_name="book-management-app"}` - All logs
  - `{service_name="book-management-app"} |= "ERROR"` - Error logs
  - `{service_name="book-management-app"} |= "BookController"` - Controller logs

**3. Traces Dashboard:**
- Click "Explore" → Select "Tempo"
- Search traces by:
  - Service name: `book-management-app`
  - Operation name
  - Duration
  - Tags

### Prometheus (Metrics)
- **URL:** http://localhost:9090
- Query examples:
  - `up` - Check service availability
  - `http_server_requests_seconds_count` - Total HTTP requests
  - `rate(http_server_requests_seconds_count[1m])` - Request rate per minute
  - `jvm_threads_live_threads` - Active threads


## 🔗 References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [OpenTelemetry Java Agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Grafana Loki Documentation](https://grafana.com/docs/loki/)
- [Grafana Tempo Documentation](https://grafana.com/docs/tempo/)
- [OTEL Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 📄 License

This project is open source and available for educational purposes.

---

**Built with ❤️ for demonstrating OpenTelemetry observability in Spring Boot applications**

---



