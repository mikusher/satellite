package io.github.mikusher.satellite.egress.observability;

import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.policy.EgressContext;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressSink;
import io.github.mikusher.satellite.egress.policy.PrivacyViolation;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * SLF4J adapter that only logs policy-processed EgressEnvelope values.
 */
public final class Slf4jEgressLogger {
    private final Logger logger;
    private final EgressProcessor processor;
    private final PrivacyViolationListener violationListener;

    public Slf4jEgressLogger(Logger logger, EgressProcessor processor) {
        this(logger, processor, PrivacyViolationListener.noop());
    }

    public Slf4jEgressLogger(Logger logger,
                            EgressProcessor processor,
                            PrivacyViolationListener violationListener) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.processor = Objects.requireNonNull(processor, "processor");
        this.violationListener = Objects.requireNonNull(violationListener, "violationListener");
    }

    public SafeLogEvent prepare(String message, EgressEnvelope data, String purpose) {
        EgressReport report = processor.process(
                Objects.requireNonNull(data, "data"),
                EgressContext.of(EgressSink.LOG, purpose));

        for (PrivacyViolation violation : report.getViolations()) {
            violationListener.onViolation(violation);
        }

        return new SafeLogEvent(SafeLogEncoder.encode(message, report.getOutput()), report);
    }

    public void trace(String message, EgressEnvelope data, String purpose) {
        if (logger.isTraceEnabled()) {
            logger.trace(prepare(message, data, purpose).getLine());
        }
    }

    public void debug(String message, EgressEnvelope data, String purpose) {
        if (logger.isDebugEnabled()) {
            logger.debug(prepare(message, data, purpose).getLine());
        }
    }

    public void info(String message, EgressEnvelope data, String purpose) {
        if (logger.isInfoEnabled()) {
            logger.info(prepare(message, data, purpose).getLine());
        }
    }

    public void warn(String message, EgressEnvelope data, String purpose) {
        if (logger.isWarnEnabled()) {
            logger.warn(prepare(message, data, purpose).getLine());
        }
    }

    public void error(String message, EgressEnvelope data, String purpose) {
        if (logger.isErrorEnabled()) {
            logger.error(prepare(message, data, purpose).getLine());
        }
    }

}
