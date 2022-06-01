package io.github.javiercanillas.temporal.newrelic.context;

import io.github.javiercanillas.temporal.newrelic.context.SilentWrapperContextPropagator;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SilentWrapperContextPropagatorTest {

  @Mock private Object object;
  @Mock private Map<String, Payload> map;
  @Mock private ContextPropagator wrappedPropagator;
  @Spy private RuntimeException exception;

  private SilentWrapperContextPropagator propagator;

  @BeforeEach
  void setup() {
    this.propagator = SilentWrapperContextPropagator.wrap(wrappedPropagator);
  }

  @Test
  void wrap() {
    Assertions.assertThrows(
        NullPointerException.class, () -> SilentWrapperContextPropagator.wrap(null));
  }

  @Test
  void getName() {
    Mockito.doReturn("name").when(wrappedPropagator).getName();
    Assertions.assertEquals("name", propagator.getName());
  }

  @Test
  void serializeContext() {
    Mockito.doReturn(map).when(wrappedPropagator).serializeContext(object);
    Assertions.assertEquals(map, propagator.serializeContext(object));

    Mockito.doThrow(exception).when(wrappedPropagator).serializeContext(object);
    Assertions.assertEquals(Collections.emptyMap(), propagator.serializeContext(object));
  }

  @Test
  void deserializeContext() {
    Mockito.doReturn(object).when(wrappedPropagator).deserializeContext(map);
    Assertions.assertEquals(object, propagator.deserializeContext(map));

    Mockito.doThrow(exception).when(wrappedPropagator).deserializeContext(map);
    Assertions.assertNull(propagator.deserializeContext(map));
  }

  @Test
  void getCurrentContext() {
    Mockito.doReturn(object).when(wrappedPropagator).getCurrentContext();
    Assertions.assertEquals(object, propagator.getCurrentContext());

    Mockito.doThrow(exception).when(wrappedPropagator).getCurrentContext();
    Assertions.assertNotNull(propagator.getCurrentContext());
    Mockito.verify(wrappedPropagator, Mockito.times(2)).getCurrentContext();
  }

  @Test
  void setCurrentContext() {
    propagator.setCurrentContext(object);
    Mockito.verify(wrappedPropagator, Mockito.times(1)).setCurrentContext(object);

    Mockito.doThrow(exception).when(wrappedPropagator).setCurrentContext(object);
    propagator.setCurrentContext(object);

    Mockito.verify(wrappedPropagator, Mockito.times(2)).setCurrentContext(object);
  }
}
