package io.github.javiercanillas.temporal.newrelic.interceptors;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import io.github.javiercanillas.temporal.newrelic.ExceptionUtils;
import io.github.javiercanillas.temporal.newrelic.context.NewRelicDistributedTraceContextPropagator;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInfo;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * This class extends {@link ActivityInboundCallsInterceptorBase} and implements {@link ActivityInboundCallsInterceptor}
 * to enable NewRelic Transaction recording.
 */
@Slf4j
public final class TraceActivityInboundCallsInterceptor
    extends ActivityInboundCallsInterceptorBase {

  private final String transactionCategory;
  private ActivityInfo activityInfo;

  @Override
  public void init(final ActivityExecutionContext context) {
    this.activityInfo = Objects.requireNonNull(context.getInfo());
    super.init(context);
  }

  public TraceActivityInboundCallsInterceptor(@NonNull final String transactionCategory, final ActivityInboundCallsInterceptor next) {
    super(next);
    this.transactionCategory = transactionCategory;
  }

  /**
   * Intercepts a call to the main activity entry method to start a transaction.
   * <br><br>
   * Transaction is categorized as {@link #transactionCategory} and its name is {@link ActivityInfo#getActivityType()}
   * Exceptions thrown during this executions will be notify to Newrelic as unexpected errors and will attempt
   * to retrieve additional information from the exception by using {@link ExceptionUtils#retrieveAdditionalData(Exception)}
   *
   * @return result of the activity execution.
   */
  @Trace(dispatcher = true)
  @Override
  public ActivityOutput execute(final ActivityInput input) {
    NewRelicDistributedTraceContextPropagator.acceptDistributedTraceHeaders();
    NewRelic.setTransactionName(this.transactionCategory, this.activityInfo.getActivityType());
    try {
      return super.execute(input);
    } catch (Exception e) {
      NewRelic.noticeError(e, ExceptionUtils.retrieveAdditionalData(e), false);
      throw e;
    }
  }
}
