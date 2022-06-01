package io.github.javiercanillas.temporal.newrelic.interceptors;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInfo;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraceActivityInboundCallsInterceptorTest {

  private static final String CATEGORY = "category";
  @Mock private ActivityInboundCallsInterceptor next;
  @Mock private ActivityExecutionContext context;
  @Mock private ActivityInfo info;

  @BeforeEach
  void setup() {
    Mockito.doReturn(info).when(context).getInfo();
  }

  @Test
  void init() {
    new TraceActivityInboundCallsInterceptor(CATEGORY, next).init(context);
    Mockito.verify(context, Mockito.times(1)).getInfo();
    Mockito.verify(next, Mockito.times(1)).init(context);
  }

  @Test
  void execute_ok() {
    var input = Mockito.mock(ActivityInboundCallsInterceptor.ActivityInput.class);
    var expectedOutput = Mockito.mock(ActivityInboundCallsInterceptor.ActivityOutput.class);
    Mockito.doReturn(expectedOutput).when(next).execute(input);
    final var traceActivityInboundCallsInterceptor = new TraceActivityInboundCallsInterceptor(CATEGORY, next);
    traceActivityInboundCallsInterceptor.init(context);
    Assertions.assertEquals(expectedOutput, traceActivityInboundCallsInterceptor.execute(input));
  }

  @Test
  void execute_exception() {
    var input = Mockito.mock(ActivityInboundCallsInterceptor.ActivityInput.class);
    var ex = new RuntimeException("Expected!");
    Mockito.doThrow(ex).when(next).execute(input);
    final var traceActivityInboundCallsInterceptor = new TraceActivityInboundCallsInterceptor(CATEGORY, next);
    traceActivityInboundCallsInterceptor.init(context);
    Assertions.assertEquals(
        ex,
        Assertions.assertThrows(
            RuntimeException.class, () -> traceActivityInboundCallsInterceptor.execute(input)));
  }
}
