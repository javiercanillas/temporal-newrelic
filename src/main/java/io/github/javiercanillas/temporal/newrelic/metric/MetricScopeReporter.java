package io.github.javiercanillas.temporal.newrelic.metric;

import com.newrelic.api.agent.NewRelic;
import com.uber.m3.tally.Buckets;
import com.uber.m3.tally.Capabilities;
import com.uber.m3.tally.CapableOf;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import com.uber.m3.util.Duration;
import io.temporal.serviceclient.MetricsTag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link StatsReporter} that sends data to log as TRACE entries and also reports
 * it to NewRelic in the form of custom metrics with a base name:
 *
 * <ul>
 *   <li>Custom/temporalio</li>
 * </ul>
 *
 * Timers will follow with the {@code timer} on the metric name, Also de Gauges will follow with
 * {@code gauge} and finally the Counters will have {@code counter}. <br>
 * After that, all tags will be placed in the following specific order:
 *
 * <ul>
 *   <li>namespace
 *   <li>worker_type
 *   <li>task_queue
 *   <li>workflow_type
 *   <li>operation_name
 *   <li>signal_name
 *   <li>activity_type
 *   <li>query_type
 *   <li>exception
 *   <li>status_code
 * </ul>
 *
 * And finally, the metric name will be appended. <br>
 * <br>
 * <b>Histograms are not well supported on newrelic, so they will only be reported on logs.</b> <br>
 * <br>
 * For example:
 *
 * <pre>{@code
 * MetricScopeReporter.instance().reportCounter("name", Map.of(), 1L);
 * }</pre>
 *
 * Will generate the following metric:
 *
 * <pre>
 *     Custom/temporalio/timer/none/none/none/none/none/none/none/none/none/none/name
 * </pre>
 *
 * On the other hand:
 *
 * <pre>{@code
 * MetricScopeReporter.instance().reportGauge("name",
 *    Map.of(MetricsTag.TASK_QUEUE, "taskQueue"), 1d);
 * }</pre>
 *
 * Will generate the following metric:
 *
 * <pre>
 *     Custom/temporalio/timer/none/none/taskQueue/none/none/none/none/none/none/none/name
 * </pre>
 */
@Slf4j
public final class MetricScopeReporter implements StatsReporter {

  protected static final String METRIC_BASE = "Custom/temporalio/";
  protected static final String TIMER_METRIC = METRIC_BASE + "timer";
  protected static final String GAUGE_METRIC = METRIC_BASE + "gauge";
  protected static final String COUNTER_METRIC = METRIC_BASE + "counter";
  protected static final String ALL_NONE = "/none/none/none/none/none/none/none/none/none/none/";
  protected static final String NONE = "none";

  private static final MetricScopeReporter INSTANCE = new MetricScopeReporter();

  private MetricScopeReporter() {
    // to avoid instantiation
  }

  @Override
  public void reportCounter(final String name, final Map<String, String> tags, final long value) {
    NewRelic.incrementCounter(COUNTER_METRIC + buildPathFromTags(tags) + name, (int) value);
    log.trace("[Counter {}: {} | tags: {}]", name, value, tags);
  }

  @Override
  public void reportGauge(final String name, final Map<String, String> tags, final double value) {
    NewRelic.recordMetric(GAUGE_METRIC + buildPathFromTags(tags) + name, (float) value);
    log.trace("[Gauge: {}: {} | tags: {}]", name, value, tags);
  }

  @Override
  public void reportTimer(
      final String name, final Map<String, String> tags, final Duration interval) {
    NewRelic.recordResponseTimeMetric(
        TIMER_METRIC + buildPathFromTags(tags) + name, interval.toMillis());
    log.trace("[Timer: {}: {}ms | tags: {}]", name, interval.toMillis(), tags);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void reportHistogramValueSamples(
      final String name,
      final Map<String, String> tags,
      final Buckets buckets,
      final double bucketLowerBound,
      final double bucketUpperBound,
      final long samples) {
    log.trace(
        "[Histogram: {}: { lower:{}, upper:{}, samples: {}} | tags: {}]",
        name,
        bucketLowerBound,
        bucketUpperBound,
        samples,
        tags);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void reportHistogramDurationSamples(
      final String name,
      final Map<String, String> tags,
      final Buckets buckets,
      final Duration bucketLowerBound,
      final Duration bucketUpperBound,
      final long samples) {
    log.trace(
        "[Histogram (ms): {}: { lower: {}, upper: {}, samples: {}} | tags: {}]",
        name,
        bucketLowerBound.toMillis(),
        bucketUpperBound.toMillis(),
        samples,
        tags);
  }

  @Override
  public Capabilities capabilities() {
    return CapableOf.REPORTING;
  }

  @Override
  public void flush() {
    // DO NOTHING
  }

  @Override
  public void close() {
    // DO NOTHING
  }

  public static MetricScopeReporter instance() {
    return INSTANCE;
  }

  public static Scope getScope() {
    return new RootScopeBuilder().reporter(instance()).reportEvery(Duration.ofMillis(1));
  }

  private String buildPathFromTags(final Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return ALL_NONE;
    } else {
      return List.of(
              tags.getOrDefault(MetricsTag.NAMESPACE, NONE),
              tags.getOrDefault(MetricsTag.WORKER_TYPE, NONE),
              tags.getOrDefault(MetricsTag.TASK_QUEUE, NONE),
              tags.getOrDefault(MetricsTag.WORKFLOW_TYPE, NONE),
              tags.getOrDefault(MetricsTag.OPERATION_NAME, NONE),
              tags.getOrDefault(MetricsTag.SIGNAL_NAME, NONE),
              tags.getOrDefault(MetricsTag.ACTIVITY_TYPE, NONE),
              tags.getOrDefault(MetricsTag.QUERY_TYPE, NONE),
              tags.getOrDefault(MetricsTag.EXCEPTION, NONE),
              tags.getOrDefault(MetricsTag.STATUS_CODE, NONE))
          .stream()
          .collect(Collectors.joining("/", "/", "/"));
    }
  }
}
