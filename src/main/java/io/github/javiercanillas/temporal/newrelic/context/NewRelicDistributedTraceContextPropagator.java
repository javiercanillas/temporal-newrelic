package io.github.javiercanillas.temporal.newrelic.context;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.DataConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Propagates Distributed Transaction tracing within Temporal workflow & activities. It uses a {@link
 * ThreadLocal} to hold the context map with the information. In order to mitigate the scenario
 * where this information can applied to wrong execution, after the first {@link
 * #getCurrentContext()} the ThreadLocal content value is removed. <br>
 * As far as the use of this class, the context is set once and then get only once.
 * <br/>
 * How to use it:<br/>
 * <pre>
 * {@code
 * var workflowClientOptionsBuilder = WorkflowClientOptions.newBuilder();
 * workflowClientOptionsBuilder.setContextPropagators(
 *      List.of(new NewRelicDistributedTraceContextPropagator()));
 * }
 * </pre>
 * You want to avoid crashing in case of an error, losing NewRelic distributed tracing data, you can compose
 * it with {@link SilentWrapperContextPropagator}.
 */
@Slf4j
public class NewRelicDistributedTraceContextPropagator implements ContextPropagator {

    private static final ThreadLocal<Object> CURRENT_CONTEXT = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NewRelicDistributedTraceContextPropagator.class.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCurrentContext() {
        var obj = CURRENT_CONTEXT.get();
        if (obj == null) {
            try {
                var optTransaction = Optional.ofNullable(NewRelic.getAgent()).map(Agent::getTransaction);
                if (optTransaction.isPresent()) {
                    obj =
                            optTransaction
                                    .map(
                                            t -> {
                                                var headers = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);
                                                t.insertDistributedTraceHeaders(headers);
                                                return headers;
                                            })
                                    .map(ConcurrentHashMapHeaders::getMapCopy)
                                    .orElse(Collections.emptyMap());
                } else {
                    log.trace("No NewRelic transaction exists to get distributed tracing data.");
                    obj = Collections.emptyMap();
                }
            } finally {
                CURRENT_CONTEXT.remove();
            }
        }
        log.trace("getCurrentContext: {}", obj);
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentContext(final Object context) {
        log.trace("setCurrentContext: {}", context);
        @SuppressWarnings("unchecked") final var contextMap = (Map<String, List<String>>) context;
        if (contextMap != null) {
            CURRENT_CONTEXT.set(contextMap);
            acceptDistributedTraceHeaders(contextMap);
        }
    }

    @SuppressWarnings("unchecked")
    public static void acceptDistributedTraceHeaders() {
        Optional.ofNullable((Map<String, List<String>>) CURRENT_CONTEXT.get())
                .ifPresentOrElse(
                        NewRelicDistributedTraceContextPropagator::acceptDistributedTraceHeaders,
                        () -> log.trace("No Distributed trace header to accept"));
    }

    private static void acceptDistributedTraceHeaders(final Map<String, List<String>> contextMap) {
        var headers = ConcurrentHashMapHeaders.buildFromMap(HeaderType.MESSAGE, contextMap);
        Optional.ofNullable(NewRelic.getAgent())
                .map(Agent::getTransaction)
                .ifPresentOrElse(
                        t -> t.acceptDistributedTraceHeaders(TransportType.Other, headers),
                        () -> log.trace("No NewRelic transaction exists to put distributed tracing data."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Payload> serializeContext(final Object context) {
        if (context != null) {
            return Map.of(
                    this.getName(), DataConverter.getDefaultInstance().toPayload(context).orElseThrow());
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object deserializeContext(final Map<String, Payload> context) {

        return Optional.ofNullable(context)
                .map(c -> c.get(this.getName()))
                .map(
                        payload ->
                                DataConverter.getDefaultInstance().fromPayload(payload, Map.class, Map.class))
                .orElse(null);
    }
}
