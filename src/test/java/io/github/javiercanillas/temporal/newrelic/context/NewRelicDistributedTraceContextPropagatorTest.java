package io.github.javiercanillas.temporal.newrelic.context;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransportType;
import io.github.javiercanillas.temporal.newrelic.context.NewRelicDistributedTraceContextPropagator;
import io.temporal.api.common.v1.Payload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class NewRelicDistributedTraceContextPropagatorTest {

  private final NewRelicDistributedTraceContextPropagator propagator =
      new NewRelicDistributedTraceContextPropagator();

  @Mock private Transaction transaction;
  @Mock private Agent agent;

  @Test
  void getName() {
    Assertions.assertEquals(
        NewRelicDistributedTraceContextPropagator.class.getName(), propagator.getName());
  }

  @Test
  void getCurrentContext_withTransaction() {
    Mockito.doReturn(transaction).when(agent).getTransaction();
    try (MockedStatic<NewRelic> newRelicMockedStatic = Mockito.mockStatic(NewRelic.class)) {
      newRelicMockedStatic.when(NewRelic::getAgent).thenReturn(agent);
      Assertions.assertNotNull(propagator.getCurrentContext());
    }
    Mockito.verify(transaction, Mockito.times(1)).insertDistributedTraceHeaders(Mockito.any());
  }

  @Test
  void getCurrentContext_withoutTransaction() {
    try (MockedStatic<NewRelic> newRelicMockedStatic = Mockito.mockStatic(NewRelic.class)) {
      newRelicMockedStatic.when(NewRelic::getAgent).thenReturn(agent);
      Assertions.assertNotNull(propagator.getCurrentContext());
    }
    Mockito.verify(transaction, Mockito.never()).insertDistributedTraceHeaders(Mockito.any());
  }

  @Test
  void setCurrentContext_withTransaction() {
    Mockito.doReturn(transaction).when(agent).getTransaction();
    try (MockedStatic<NewRelic> newRelicMockedStatic = Mockito.mockStatic(NewRelic.class)) {
      newRelicMockedStatic.when(NewRelic::getAgent).thenReturn(agent);
      propagator.setCurrentContext(Map.of("key1", List.of("value1", "value2")));
    }
    Mockito.verify(transaction, Mockito.times(1))
        .acceptDistributedTraceHeaders(Mockito.eq(TransportType.Other), Mockito.any());
  }

  @Test
  void setCurrentContext_withoutTransaction() {
    try (MockedStatic<NewRelic> newRelicMockedStatic = Mockito.mockStatic(NewRelic.class)) {
      newRelicMockedStatic.when(NewRelic::getAgent).thenReturn(agent);
      propagator.setCurrentContext(Map.of("key1", List.of("value1", "value2")));
    }
    Mockito.verify(transaction, Mockito.never())
        .acceptDistributedTraceHeaders(Mockito.any(), Mockito.any());
  }

  @Test
  void serializeContext() {
    Assertions.assertEquals(Collections.emptyMap(), propagator.serializeContext(null));
  }

  @Test
  void deserializeContext() {
    Assertions.assertNull(propagator.deserializeContext(null));
  }

  @Test
  void serializeAndDeserializeContext() throws InvalidProtocolBufferException {
    var map = Map.of("key", List.of("value1", "value2"));
    var result = propagator.serializeContext(map);
    Assertions.assertNotNull(result);
    Assertions.assertEquals(1, result.size());
    Assertions.assertNotNull(result.entrySet().iterator().next().getValue());

    // lets add another value here just to mess around
    var newMap =
        Map.ofEntries(
            result.entrySet().iterator().next(),
            Map.entry(
                "anotherKey",
                Payload.newBuilder().putMetadata("metadataKey", ByteString.EMPTY).build()));

    @SuppressWarnings("unchecked")
    var copiedMap = (Map<String, List<String>>) propagator.deserializeContext(newMap);
    Assertions.assertNotNull(copiedMap);
    Assertions.assertEquals(map.size(), copiedMap.size());
    Assertions.assertEquals(map.get("key"), copiedMap.get("key"));
    Assertions.assertEquals(map.get("key").size(), copiedMap.get("key").size());
    Assertions.assertEquals(map.get("key").get(0), copiedMap.get("key").get(0));
    Assertions.assertEquals(map.get("key").get(1), copiedMap.get("key").get(1));
  }
}
