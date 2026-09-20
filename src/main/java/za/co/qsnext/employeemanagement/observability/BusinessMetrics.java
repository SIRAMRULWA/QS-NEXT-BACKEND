package za.co.qsnext.employeemanagement.observability;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Business-event metrics, exported to Prometheus alongside the JVM/HTTP
 * metrics Spring Boot Actuator already provides automatically.
 * <p>
 * Rather than hand-instrumenting a handful of services (and inevitably
 * missing most of the other ~30), this hooks the one choke point every
 * meaningful business event in the system already passes through:
 * {@link za.co.qsnext.employeemanagement.audit.AuditService#log}. Every
 * audited action - login, a leave approval, a payroll run, an AI
 * suggestion, a push notification queued, ... - becomes a Prometheus
 * counter tagged by action and result, with no per-module wiring
 * needed. Action names are a bounded, small set of literal constants
 * throughout the codebase (verified before adding this - the one
 * partially-dynamic action, AI_SUGGESTION_GENERATED:&lt;type&gt;, is
 * built from AiSuggestion's fixed TYPE_* constants, not user input),
 * so this does not risk unbounded label cardinality.
 */
@Component
public class BusinessMetrics {

    private static final String METRIC_NAME = "qsnext.business.events";

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordEvent(String action, String result) {
        meterRegistry.counter(METRIC_NAME, "action", action, "result", result).increment();
    }
}
