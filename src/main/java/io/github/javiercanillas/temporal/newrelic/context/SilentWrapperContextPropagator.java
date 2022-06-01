package io.github.javiercanillas.temporal.newrelic.context;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * This class implements {@link ContextPropagator} wrapping an instance of {@link ContextPropagator}
 * in order to provider a secure execution, avoid bubbling errors on this stack, but logging them
 * using the same wrapped class.
 * How to use it:
 * <pre>
 * {@code
 *     var silentContextPropagator = SilentWrapperContextPropagator.wrap(...);
 * }
 * </pre>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SilentWrapperContextPropagator implements ContextPropagator {

  private final ContextPropagator wrappedPropagator;
  private final Logger log;

  public static SilentWrapperContextPropagator wrap(
      @NonNull final ContextPropagator contextPropagator) {
    return new SilentWrapperContextPropagator(
        contextPropagator, LoggerFactory.getLogger(contextPropagator.getClass()));
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return wrappedPropagator.getName();
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Payload> serializeContext(final Object context) {
    try {
      return wrappedPropagator.serializeContext(context);
    } catch (Exception e) {
      log.warn("Error while serializing context", e);
      return Collections.emptyMap();
    }
  }

  /** {@inheritDoc} */
  @Override
  public Object deserializeContext(final Map<String, Payload> header) {
    try {
      return wrappedPropagator.deserializeContext(header);
    } catch (Exception e) {
      log.warn("Error while deserializing context", e);
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public Object getCurrentContext() {
    try {
      return wrappedPropagator.getCurrentContext();
    } catch (Exception e) {
      log.warn("Error while getting current context", e);
      return Collections.emptyMap();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setCurrentContext(final Object context) {
    try {
      wrappedPropagator.setCurrentContext(context);
    } catch (Exception e) {
      log.warn("Error while setting current context", e);
    }
  }
}
