package io.github.javiercanillas.temporal.newrelic.interceptors;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class TraceWorkflowInboundCallsInterceptorTest {

  private static final String CATEGORY = "category";
  @Mock private WorkflowInboundCallsInterceptor next;
  @Mock private WorkflowInfo info;

  @Test
  void execute_ok() {
    var expectedOutput = Mockito.mock(WorkflowInboundCallsInterceptor.WorkflowOutput.class);
    var input = Mockito.mock(WorkflowInboundCallsInterceptor.WorkflowInput.class);
    Mockito.doReturn(expectedOutput).when(next).execute(input);
    var interceptor = new TraceWorkflowInboundCallsInterceptor(CATEGORY, next);
    final Supplier<WorkflowInboundCallsInterceptor.WorkflowOutput> supplier =
        () -> interceptor.execute(input);
    var output = doWithWorkflowMocks(Workflow::getInfo, info, supplier);
    Assertions.assertEquals(expectedOutput, output);
  }

  @Test
  void execute_fail() {
    var expectedEx = new RuntimeException("expected!");
    var input = Mockito.mock(WorkflowInboundCallsInterceptor.WorkflowInput.class);
    Mockito.doThrow(expectedEx).when(next).execute(input);
    var interceptor = new TraceWorkflowInboundCallsInterceptor(CATEGORY, next);
    final Supplier<WorkflowInboundCallsInterceptor.WorkflowOutput> supplier =
        () -> interceptor.execute(input);
    Assertions.assertEquals(
        expectedEx,
        Assertions.assertThrows(
            RuntimeException.class, () -> doWithWorkflowMocks(Workflow::getInfo, info, supplier)));
  }

  @Test
  void handleSignal_ok() {
    var input = Mockito.mock(WorkflowInboundCallsInterceptor.SignalInput.class);
    var interceptor = new TraceWorkflowInboundCallsInterceptor(CATEGORY, next);
    final Runnable runnable = () -> interceptor.handleSignal(input);
    doWithWorkflowMocks(Workflow::getInfo, info, runnable);
    Mockito.verify(next, Mockito.times(1)).handleSignal(input);
  }

  @Test
  void handleSignal_fail() {
    var expectedEx = new RuntimeException("expected!");
    var input = Mockito.mock(WorkflowInboundCallsInterceptor.SignalInput.class);
    Mockito.doThrow(expectedEx).when(next).handleSignal(input);
    var interceptor = new TraceWorkflowInboundCallsInterceptor(CATEGORY, next);
    final Runnable runnable = () -> interceptor.handleSignal(input);
    Assertions.assertEquals(
        expectedEx,
        Assertions.assertThrows(
            RuntimeException.class, () -> doWithWorkflowMocks(Workflow::getInfo, info, runnable)));
    Mockito.verify(next, Mockito.times(1)).handleSignal(input);
  }

  @Test
  void handleQuery_ok() {
    var expectedOutput = Mockito.mock(WorkflowInboundCallsInterceptor.QueryOutput.class);
    var input = Mockito.mock(WorkflowInboundCallsInterceptor.QueryInput.class);
    Mockito.doReturn(expectedOutput).when(next).handleQuery(input);
    var interceptor = new TraceWorkflowInboundCallsInterceptor(CATEGORY, next);
    final Supplier<WorkflowInboundCallsInterceptor.QueryOutput> supplier =
        () -> interceptor.handleQuery(input);
    var output = doWithWorkflowMocks(Workflow::getInfo, info, supplier);
    Assertions.assertEquals(expectedOutput, output);
  }

  @Test
  void handleQuery_fail() {
    var expectedEx = new RuntimeException("expected!");
    var input = Mockito.mock(WorkflowInboundCallsInterceptor.QueryInput.class);
    Mockito.doThrow(expectedEx).when(next).handleQuery(input);
    var interceptor = new TraceWorkflowInboundCallsInterceptor(CATEGORY, next);
    final Supplier<WorkflowInboundCallsInterceptor.QueryOutput> supplier =
        () -> interceptor.handleQuery(input);
    Assertions.assertEquals(
        expectedEx,
        Assertions.assertThrows(
            RuntimeException.class, () -> doWithWorkflowMocks(Workflow::getInfo, info, supplier)));
  }

  private void doWithWorkflowMocks(
      final MockedStatic.Verification verification,
      final Object rtnObject,
      final Runnable executionBlock) {
    try (MockedStatic<Workflow> workflowMockedStatic = Mockito.mockStatic(Workflow.class)) {
      workflowMockedStatic.when(verification).thenReturn(rtnObject);
      executionBlock.run();
    }
  }

  private <T> T doWithWorkflowMocks(
      final MockedStatic.Verification verification,
      final Object rtnObject,
      final Supplier<T> executionBlock) {
    try (MockedStatic<Workflow> workflowMockedStatic = Mockito.mockStatic(Workflow.class)) {
      workflowMockedStatic.when(verification).thenReturn(rtnObject);
      return executionBlock.get();
    }
  }
}
