package io.github.javiercanillas.temporal.newrelic.interceptors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TraceWorkerInterceptorTest {

  @Test
  void interceptWorkflow() {
    Assertions.assertNotNull(new TraceWorkerInterceptor().interceptWorkflow(null));
  }

  @Test
  void interceptActivity() {
    Assertions.assertNotNull(new TraceWorkerInterceptor().interceptActivity(null));
  }
}
