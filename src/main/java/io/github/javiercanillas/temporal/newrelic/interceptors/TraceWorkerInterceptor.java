package io.github.javiercanillas.temporal.newrelic.interceptors;

import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkerInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import lombok.NonNull;

/**
 * This interceptor creates and wraps workflow executions with {@link TraceWorkflowInboundCallsInterceptor}
 * and activity executions with {@link TraceActivityInboundCallsInterceptor}.
 *
 * Example how to use it:
 * <pre>
 * {@code
 *  WorkerFactoryOptions.newBuilder(workerFactoryProperties.build())
 *        .setWorkerInterceptors(new TraceWorkerInterceptor())
 *        .build());
 * }
 * </pre>
 */
public class TraceWorkerInterceptor implements WorkerInterceptor {

  private final String workflowTransactionCategory;
  private final String activityTransactionCategory;

  /**
   * Constructs a {@link TraceWorkerInterceptor} using "Workflow" as transaction category for Workflow transactions and
   * "Activity" for Activity transactions.
   */
  public TraceWorkerInterceptor() {
    this("Workflow", "Activity");
  }

  /**
   * Constructs a {@link TraceWorkerInterceptor} using custom categories for Workflow and Activity transactions.
   * @param workflowTransactionCategory a non-null String
   * @param activityTransactionCategory a non-null String
   */
  public TraceWorkerInterceptor(@NonNull final String workflowTransactionCategory, @NonNull final String activityTransactionCategory) {
    this.workflowTransactionCategory = workflowTransactionCategory;
    this.activityTransactionCategory = activityTransactionCategory;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowInboundCallsInterceptor interceptWorkflow(
      final WorkflowInboundCallsInterceptor next) {
    return new TraceWorkflowInboundCallsInterceptor(this.workflowTransactionCategory, next);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ActivityInboundCallsInterceptor interceptActivity(
      final ActivityInboundCallsInterceptor next) {
    return new TraceActivityInboundCallsInterceptor(this.activityTransactionCategory, next);
  }
}
