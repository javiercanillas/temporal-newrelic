package io.github.javiercanillas.temporal.newrelic.metric;

import com.newrelic.api.agent.NewRelic;
import com.uber.m3.util.Duration;
import io.temporal.serviceclient.MetricsTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Map;

import static io.github.javiercanillas.temporal.newrelic.metric.MetricScopeReporter.ALL_NONE;
import static io.github.javiercanillas.temporal.newrelic.metric.MetricScopeReporter.COUNTER_METRIC;
import static io.github.javiercanillas.temporal.newrelic.metric.MetricScopeReporter.GAUGE_METRIC;
import static io.github.javiercanillas.temporal.newrelic.metric.MetricScopeReporter.TIMER_METRIC;

class MetricScopeReporterTest {

  private static final Map<String, String> EMPTY_TAGS = Map.of();
  private static final Map<String, String> FULL_TAGS =
      Map.of(
          MetricsTag.NAMESPACE, "namespace",
          MetricsTag.WORKER_TYPE, "workerType",
          MetricsTag.TASK_QUEUE, "taskQueue",
          MetricsTag.WORKFLOW_TYPE, "workflowType",
          MetricsTag.OPERATION_NAME, "operationName",
          MetricsTag.SIGNAL_NAME, "signalName",
          MetricsTag.ACTIVITY_TYPE, "activityType",
          MetricsTag.QUERY_TYPE, "queryType",
          MetricsTag.EXCEPTION, "exception",
          MetricsTag.STATUS_CODE, "statusCode");

  @SuppressWarnings("linelength")
  private static final String FULL_TAGS_STRING =
      "/namespace/workerType/taskQueue/workflowType/operationName/signalName/activityType/queryType/exception/statusCode/";

  @Test
  void reportCounter() {
    try (var mockedNewRelic = Mockito.mockStatic(NewRelic.class)) {
      MetricScopeReporter.instance().reportCounter("name", EMPTY_TAGS, 1L);
      mockedNewRelic.verify(
          () ->
              NewRelic.incrementCounter(
                  ArgumentMatchers.argThat(s -> s.equals(COUNTER_METRIC + ALL_NONE + "name")),
                  Mockito.eq(1)),
          Mockito.times(1));

      mockedNewRelic.reset();

      MetricScopeReporter.instance().reportCounter("name", FULL_TAGS, 1L);
      mockedNewRelic.verify(
          () ->
              NewRelic.incrementCounter(
                  ArgumentMatchers.argThat(
                      s -> s.equals(COUNTER_METRIC + FULL_TAGS_STRING + "name")),
                  Mockito.eq(1)),
          Mockito.times(1));
    }
  }

  @Test
  void reportGauge() {
    try (var mockedNewRelic = Mockito.mockStatic(NewRelic.class)) {
      MetricScopeReporter.instance().reportGauge("name", EMPTY_TAGS, 1d);
      mockedNewRelic.verify(
          () ->
              NewRelic.recordMetric(
                  ArgumentMatchers.argThat(s -> s.equals(GAUGE_METRIC + ALL_NONE + "name")),
                  Mockito.eq(1f)),
          Mockito.times(1));

      mockedNewRelic.reset();

      MetricScopeReporter.instance().reportGauge("name", FULL_TAGS, 1d);
      mockedNewRelic.verify(
          () ->
              NewRelic.recordMetric(
                  ArgumentMatchers.argThat(s -> s.equals(GAUGE_METRIC + FULL_TAGS_STRING + "name")),
                  Mockito.eq(1f)),
          Mockito.times(1));
    }
  }

  @Test
  void reportTimer() {
    try (var mockedNewRelic = Mockito.mockStatic(NewRelic.class)) {
      MetricScopeReporter.instance().reportTimer("name", EMPTY_TAGS, Duration.ofMillis(1000));
      mockedNewRelic.verify(
          () ->
              NewRelic.recordResponseTimeMetric(
                  ArgumentMatchers.argThat(s -> s.equals(TIMER_METRIC + ALL_NONE + "name")),
                  Mockito.eq(1000L)),
          Mockito.times(1));

      mockedNewRelic.reset();

      MetricScopeReporter.instance().reportTimer("name", FULL_TAGS, Duration.ofMillis(1000));
      mockedNewRelic.verify(
          () ->
              NewRelic.recordResponseTimeMetric(
                  ArgumentMatchers.argThat(s -> s.equals(TIMER_METRIC + FULL_TAGS_STRING + "name")),
                  Mockito.eq(1000L)),
          Mockito.times(1));
    }
  }

  @Test
  void reportHistogramValueSamples() {
    try (var mockedNewRelic = Mockito.mockStatic(NewRelic.class)) {
      MetricScopeReporter.instance()
          .reportHistogramValueSamples("name", EMPTY_TAGS, null, 1d, 10d, 10);
      mockedNewRelic.verifyNoInteractions();
    }
  }

  @Test
  void reportHistogramDurationSamples() {
    try (var mockedNewRelic = Mockito.mockStatic(NewRelic.class)) {
      MetricScopeReporter.instance()
          .reportHistogramDurationSamples(
              "name", EMPTY_TAGS, null, Duration.ofMillis(1), Duration.ofSeconds(1), 10);
      mockedNewRelic.verifyNoInteractions();
    }
  }

  @Test
  void capabilities() {
    Assertions.assertNotNull(MetricScopeReporter.instance().capabilities());
  }

  @Test
  void instance() {
    Assertions.assertNotNull(MetricScopeReporter.instance());
  }

  @Test
  void getScope() {
    Assertions.assertNotNull(MetricScopeReporter.getScope());
  }

  @Test
  void test() {
    try (var mockedNewRelic = Mockito.mockStatic(NewRelic.class)) {
      MetricScopeReporter.instance().close();
      mockedNewRelic.verifyNoInteractions();
    }
  }
}
