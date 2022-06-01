package io.github.javiercanillas.temporal.newrelic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

class ExceptionUtilsTest {

  @Test
  void testArguments() {
    Assertions.assertThrows(
        NullPointerException.class, () -> ExceptionUtils.retrieveAdditionalData(null));
  }

  private static Stream<Arguments> testArgs() {
    return Stream.of(
        Arguments.of(
            new RuntimeException() {
              public String getDescription() {
                return "hello";
              }

              public void setEmptyDescription() {}

              public void setDescription(final String value) {}
            },
            Map.of("getDescription", "hello")),
        Arguments.of(
            new RuntimeException() {
              public String getDescription() {
                return "hello";
              }

              public void getVoid() {}
            },
            Map.of("getDescription", "hello")),
        Arguments.of(
            new RuntimeException() {
              public String getDescription() {
                throw new RuntimeException("ops!");
              }
            },
            Collections.emptyMap()),
        Arguments.of(
            new RuntimeException("exception message") {
              public String getDescription() {
                return "hello";
              }
            },
            Map.of("getDescription", "hello")),
        Arguments.of(
            new RuntimeException(
                "exception message", new RuntimeException("exception cause message")) {
              public String getDescription() {
                return "hello";
              }
            },
            Map.of("getDescription", "hello")),
        Arguments.of(
            new RuntimeException() {
              public String getDescription() {
                return "hello";
              }

              public Integer getInteger() {
                return null;
              }
            },
            Map.of("getDescription", "hello")));
  }

  @MethodSource("testArgs")
  @ParameterizedTest
  void test(final Exception ex, final Map<String, Object> expectedMap) {
    Assertions.assertEquals(expectedMap, ExceptionUtils.retrieveAdditionalData(ex));
  }
}
