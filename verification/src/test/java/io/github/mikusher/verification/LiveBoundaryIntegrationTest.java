package io.github.mikusher.verification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.observability.Slf4jEgressLogger;
import io.github.mikusher.satellite.egress.opentelemetry.OpenTelemetrySpanAdapter;
import io.github.mikusher.satellite.egress.policy.EgressPolicyEngine;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressRules;
import io.github.mikusher.satellite.egress.policy.EgressSink;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Real in-process boundaries: Logback events, SDK-exported spans and loopback HTTP.
 * Never sends data outside the CI runner and never uses production credentials.
 */
public class LiveBoundaryIntegrationTest {

    private static final Key<String> ORDER_ID = Key.string("order.id")
            .classifiedAs(DataClassification.PUBLIC);
    private static final Key<String> EMAIL = Key.string("customer.email")
            .classifiedAs(DataClassification.CONFIDENTIAL)
            .category(DataCategory.PERSONAL_DATA);
    private static final Key<String> TOKEN = Key.string("payment.token")
            .classifiedAs(DataClassification.RESTRICTED)
            .category(DataCategory.CREDENTIAL);

    private static EgressEnvelope payment() {
        return EgressEnvelope.builder()
                .put(ORDER_ID, "ORDER-9001")
                .put(EMAIL, "alice@example.com")
                .put(TOKEN, "tok_private")
                .build();
    }

    @Test
    public void logbackBackendReceivesOnlyApprovedFields() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("satellite.release.validation");
        ListAppender<ILoggingEvent> capture = new ListAppender<ILoggingEvent>();
        capture.start();
        Level originalLevel = logger.getLevel();
        boolean originalAdditive = logger.isAdditive();
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(capture);
        try {
            Slf4jEgressLogger.secure(logger).info("payment\ncompleted", payment(), "payment-log");

            assertEquals(1, capture.list.size());
            String line = capture.list.get(0).getFormattedMessage();
            assertTrue(line.contains("ORDER-9001"));
            assertTrue(line.contains("[REDACTED]"));
            assertTrue(line.contains("payment\\ncompleted"));
            assertFalse(line.contains("alice@example.com"));
            assertFalse(line.contains("tok_private"));
            assertFalse(line.contains("payment\ncompleted"));
        } finally {
            logger.detachAppender(capture);
            logger.setLevel(originalLevel);
            logger.setAdditive(originalAdditive);
            capture.stop();
        }
    }

    @Test
    public void otelSdkExporterReceivesOnlyApprovedSpanAttributes() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        try {
            OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(provider)
                    .build();
            Span span = sdk.getTracer("satellite.release.validation")
                    .spanBuilder("payment").startSpan();
            OpenTelemetrySpanAdapter.secure()
                    .applyToSpan(span, payment(), "payment-trace");
            span.end();

            assertEquals(1, exporter.getFinishedSpanItems().size());
            SpanData exported = exporter.getFinishedSpanItems().get(0);
            assertEquals("ORDER-9001",
                    exported.getAttributes().get(AttributeKey.stringKey("order.id")));
            assertEquals("[REDACTED]",
                    exported.getAttributes().get(AttributeKey.stringKey("customer.email")));
            assertFalse(exported.getAttributes().asMap()
                    .containsKey(AttributeKey.stringKey("payment.token")));
        } finally {
            provider.close();
        }
    }

    @Test
    public void loopbackHttpReceivesOnlyPurposeAuthorizedPayload() throws Exception {
        EgressPolicyEngine policy = EgressPolicyEngine.builder()
                .add(EgressRules.allow(EMAIL, EgressSink.NETWORK,
                        "payment-provider", "PAYMENT_EMAIL_REQUIRED"))
                .add(EgressRules.allow(TOKEN, EgressSink.NETWORK,
                        "payment-provider", "PAYMENT_TOKEN_REQUIRED"))
                .withSecureDefaults().build();
        EgressProcessor processor = new EgressProcessor(policy);

        AtomicReference<String> receivedForProvider = new AtomicReference<String>();
        AtomicReference<String> receivedForAnalytics = new AtomicReference<String>();
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/provider", exchange -> {
            try {
                receivedForProvider.set(new String(
                        exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] reply = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, reply.length);
                try (java.io.OutputStream output = exchange.getResponseBody()) {
                    output.write(reply);
                }
            } finally {
                exchange.close();
            }
        });
        server.createContext("/analytics", exchange -> {
            try {
                receivedForAnalytics.set(new String(
                        exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] reply = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, reply.length);
                try (java.io.OutputStream output = exchange.getResponseBody()) {
                    output.write(reply);
                }
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            EgressReport providerOutput = processor.process(
                    payment(), EgressSink.NETWORK, "payment-provider");
            post(base + "/provider", providerOutput);

            EgressReport analyticsOutput = processor.process(
                    payment(), EgressSink.NETWORK, "analytics");
            post(base + "/analytics", analyticsOutput);

            String providerBody = receivedForProvider.get();
            String analyticsBody = receivedForAnalytics.get();
            assertTrue(providerBody.contains("ORDER-9001"));
            assertTrue(providerBody.contains("alice@example.com"));
            assertTrue(providerBody.contains("tok_private"));
            assertTrue(analyticsBody.contains("ORDER-9001"));
            assertFalse(analyticsBody.contains("alice@example.com"));
            assertFalse(analyticsBody.contains("tok_private"));
        } finally {
            server.stop(0);
        }
    }

    private static void post(String uri, EgressReport approved) throws Exception {
        // Satellite is a library, not an HTTP interceptor. Only the policy-approved
        // representation is serialized. Do not serialize the raw envelope here.
        String body = new ObjectMapper().writeValueAsString(approved.getOutput());
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(java.time.Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}
