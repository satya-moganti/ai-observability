package com.bookstore.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for OpenTelemetry instrumentation and MDC logging.
 * Provides reusable methods to reduce boilerplate code for tracing and logging.
 */
public class TelemetryUtils {

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("book-management-service", "1.0.0");

    /**
     * Executes a business operation with automatic span creation, MDC management, and exception handling.
     *
     * @param spanName   Name of the span (e.g., "BookService.createBook")
     * @param operation  Lambda containing the business logic
     * @param attributes Map of attributes to add to both span and MDC
     * @param logger     SLF4J logger for logging
     * @param <T>        Return type of the operation
     * @return Result of the operation
     */
    public static <T> T executeWithTelemetry(
            String spanName,
            Supplier<T> operation,
            Map<String, Object> attributes,
            Logger logger) {

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Add all attributes to span and MDC
            if (attributes != null) {
                addAttributesToSpanAndMDC(span, attributes);
            }

            logger.debug("Starting operation: {}", spanName);

            // Execute the operation
            T result = operation.get();

            span.setStatus(StatusCode.OK);
            logger.debug("Completed operation: {}", spanName);

            return result;

        } catch (Exception e) {
            handleException(span, e, logger);
            throw e;
        } finally {
            span.end();
            // Clear all MDC attributes
            if (attributes != null) {
                attributes.keySet().forEach(MDC::remove);
            }
        }
    }

    /**
     * Executes a void operation with automatic span creation and MDC management.
     *
     * @param spanName   Name of the span
     * @param operation  Runnable containing the business logic
     * @param attributes Map of attributes to add to both span and MDC
     * @param logger     SLF4J logger for logging
     */
    public static void executeVoidWithTelemetry(
            String spanName,
            Runnable operation,
            Map<String, Object> attributes,
            Logger logger) {

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            if (attributes != null) {
                addAttributesToSpanAndMDC(span, attributes);
            }

            logger.debug("Starting operation: {}", spanName);
            operation.run();

            span.setStatus(StatusCode.OK);
            logger.debug("Completed operation: {}", spanName);

        } catch (Exception e) {
            handleException(span, e, logger);
            throw e;
        } finally {
            span.end();
            if (attributes != null) {
                attributes.keySet().forEach(MDC::remove);
            }
        }
    }

    /**
     * Adds attributes to both OpenTelemetry span and SLF4J MDC.
     *
     * @param span       The current span
     * @param attributes Map of attribute key-value pairs
     */
    private static void addAttributesToSpanAndMDC(Span span, Map<String, Object> attributes) {
        attributes.forEach((key, value) -> {
            if (value != null) {
                // Add to span
                if (value instanceof String) {
                    span.setAttribute(key, (String) value);
                } else if (value instanceof Long) {
                    span.setAttribute(key, (Long) value);
                } else if (value instanceof Integer) {
                    span.setAttribute(key, ((Integer) value).longValue());
                } else if (value instanceof Double) {
                    span.setAttribute(key, (Double) value);
                } else if (value instanceof Boolean) {
                    span.setAttribute(key, (Boolean) value);
                } else {
                    span.setAttribute(key, value.toString());
                }

                // Add to MDC (always as string)
                MDC.put(key, value.toString());
            }
        });
    }

    /**
     * Handles exceptions by recording them in the span and logging.
     *
     * @param span   The current span
     * @param e      The exception
     * @param logger SLF4J logger
     */
    private static void handleException(Span span, Exception e, Logger logger) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
        span.setAttribute("error", true);
        span.setAttribute("error.type", e.getClass().getSimpleName());
        logger.error("Operation failed with error: {}", e.getMessage(), e);
    }

    /**
     * Adds a custom event to the current span.
     *
     * @param eventName Name of the event
     */
    public static void addSpanEvent(String eventName) {
        Span.current().addEvent(eventName);
    }

    /**
     * Adds a custom event with attributes to the current span.
     *
     * @param eventName  Name of the event
     * @param attributes Event attributes
     */
    public static void addSpanEvent(String eventName, Map<String, Object> attributes) {
        io.opentelemetry.api.common.AttributesBuilder builder = Attributes.builder();
        attributes.forEach((key, value) -> {
            if (value instanceof String) {
                builder.put(AttributeKey.stringKey(key), (String) value);
            } else if (value instanceof Long) {
                builder.put(AttributeKey.longKey(key), (Long) value);
            } else if (value instanceof Integer) {
                builder.put(AttributeKey.longKey(key), ((Integer) value).longValue());
            } else if (value instanceof Double) {
                builder.put(AttributeKey.doubleKey(key), (Double) value);
            } else if (value instanceof Boolean) {
                builder.put(AttributeKey.booleanKey(key), (Boolean) value);
            }
        });
        Span.current().addEvent(eventName, builder.build());
    }

    /**
     * Adds an attribute to the current span only.
     *
     * @param key   Attribute key
     * @param value Attribute value
     */
    public static void addSpanAttribute(String key, Object value) {
        Span current = Span.current();
        if (value instanceof String) {
            current.setAttribute(key, (String) value);
        } else if (value instanceof Long) {
            current.setAttribute(key, (Long) value);
        } else if (value instanceof Integer) {
            current.setAttribute(key, ((Integer) value).longValue());
        } else if (value instanceof Double) {
            current.setAttribute(key, (Double) value);
        } else if (value instanceof Boolean) {
            current.setAttribute(key, (Boolean) value);
        } else if (value != null) {
            current.setAttribute(key, value.toString());
        }
    }

    /**
     * Adds multiple attributes to the current span.
     *
     * @param attributes Map of attributes
     */
    public static void addSpanAttributes(Map<String, Object> attributes) {
        attributes.forEach(TelemetryUtils::addSpanAttribute);
    }

    /**
     * Creates a builder for fluent span creation with telemetry.
     *
     * @param spanName Name of the span
     * @return TelemetryBuilder instance
     */
    public static TelemetryBuilder builder(String spanName) {
        return new TelemetryBuilder(spanName);
    }

    /**
     * Fluent builder for telemetry operations.
     */
    public static class TelemetryBuilder {
        private final String spanName;
        private Map<String, Object> attributes;
        private Logger logger;

        private TelemetryBuilder(String spanName) {
            this.spanName = spanName;
        }

        public TelemetryBuilder withAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public TelemetryBuilder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public <T> T execute(Supplier<T> operation) {
            return executeWithTelemetry(spanName, operation, attributes, logger);
        }

        public void executeVoid(Runnable operation) {
            executeVoidWithTelemetry(spanName, operation, attributes, logger);
        }
    }
}
