package io.github.javiercanillas.temporal.newrelic.interceptors;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import io.github.javiercanillas.temporal.newrelic.ExceptionUtils;
import io.github.javiercanillas.temporal.newrelic.context.NewRelicDistributedTraceContextPropagator;
import io.temporal.activity.ActivityInfo;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.workflow.Workflow;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * This class extends {@link WorkflowInboundCallsInterceptorBase} and implements {@link WorkflowInboundCallsInterceptor}
 * to enable NewRelic Transaction recording.
 */
@Slf4j
public final class TraceWorkflowInboundCallsInterceptor
    extends WorkflowInboundCallsInterceptorBase {
  
  private final String transactionCategory;

  public TraceWorkflowInboundCallsInterceptor(@NonNull final String transactionCategory, final WorkflowInboundCallsInterceptor next) {
    super(next);
    this.transactionCategory = transactionCategory;
  }

  /**
   * Intercepts a call to the main workflow entry method to start a transaction.
   * <br/><br/>
   * The transaction will be categorized as {@link #transactionCategory} and its name will be <b>{@link ActivityInfo#getWorkflowType()}/execute</b>
   * <br/>
   * Exceptions thrown during this executions will be notify to Newrelic as unexpected errors and will attempt
   * to retrieve additional information from the exception by using {@link ExceptionUtils#retrieveAdditionalData(Exception)}
   * @return result of the workflow execution.
   */
  @Trace(dispatcher = true)
  @Override
  public WorkflowOutput execute(final WorkflowInput input) {
    NewRelicDistributedTraceContextPropagator.acceptDistributedTraceHeaders();
    var info = Workflow.getInfo();
    NewRelic.setTransactionName(this.transactionCategory, info.getWorkflowType() + "/execute");
    try {
      return super.execute(input);
    } catch (Exception e) {
      NewRelic.noticeError(e, ExceptionUtils.retrieveAdditionalData(e), false);
      throw e;
    }
  }

  /**
   * Intercepts a signal delivery action to a workflow execution to start a transaction.
   * <br/><br/>
   * The transaction will be categorized as {@link #transactionCategory} and its name will be
   * <b>{@link ActivityInfo#getWorkflowType()}/signal/{@link SignalInput#getSignalName()}</b>
   * <br/>
   * Exceptions thrown during this executions will be notify to Newrelic as unexpected errors and will attempt
   * to retrieve additional information from the exception by using {@link ExceptionUtils#retrieveAdditionalData(Exception)}
   */
  @Trace(dispatcher = true)
  @Override
  public void handleSignal(final SignalInput input) {
    var info = Workflow.getInfo();
    NewRelic.setTransactionName(
        this.transactionCategory, info.getWorkflowType() + "/signal/" + input.getSignalName());
    try {
      super.handleSignal(input);
    } catch (Exception e) {
      NewRelic.noticeError(e, ExceptionUtils.retrieveAdditionalData(e), false);
      throw e;
    }
  }

  /**
   * Intercepts a query to a workflow to start a transaction.
   * <br/><br/>
   * The transaction will be categorized as {@link #transactionCategory} and its name will be
   * <b>{@link ActivityInfo#getWorkflowType()}/signal/{@link QueryInput#getQueryName()}</b>
   * <br/>
   * Exceptions thrown during this executions will be notify to Newrelic as unexpected errors and will attempt
   * to retrieve additional information from the exception by using {@link ExceptionUtils#retrieveAdditionalData(Exception)}
   * @return result of the workflow execution.
   */
  /** Called when a workflow is queried. */
  @Trace(dispatcher = true)
  @Override
  public QueryOutput handleQuery(final QueryInput input) {
    var info = Workflow.getInfo();
    NewRelic.setTransactionName(
        this.transactionCategory, info.getWorkflowType() + "/signal/" + input.getQueryName());
    try {
      return super.handleQuery(input);
    } catch (Exception e) {
      NewRelic.noticeError(e, ExceptionUtils.retrieveAdditionalData(e), false);
      throw e;
    }
  }
}
