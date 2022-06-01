package io.github.javiercanillas.temporal.newrelic;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility class to manage Exceptions, specially when tracing them. */
@Slf4j
public final class ExceptionUtils {
  private static final Set<String> SKIPPED_METHODS =
      streamOfGetters(Throwable.class).map(Method::getName).collect(Collectors.toSet());

  /* None should create an instance of this */
  private ExceptionUtils() {}

  private static Stream<Method> streamOfGetters(final Class<?> clazz) {
    return Arrays.stream(clazz.getMethods())
        // getters do not have parameters
        .filter(m -> m.getParameters() == null || m.getParameters().length == 0)
        // if method starts with "get" or "is"
        .filter(m -> m.getName().startsWith("is") || m.getName().startsWith("get"));
  }

  /**
   * execute all getters (including is*) methods of the given exception (including those inherited
   * by its superclass) and returns a map containing each method name and its value. All declared
   * methods on {@link Throwable} are skipped to avoid returning a huge map full of stack-traces.
   * <b>Note: Be aware that this method <u>executes</u> those methods, so there shouldn't be logic
   * on them</b>
   *
   * @param exception a non-null exception instance
   * @return a map containing method name as key, and the value returned after method execution
   */
  public static Map<String, Object> retrieveAdditionalData(@NonNull final Exception exception) {
    try {
      final var methods = new HashMap<String, Method>();
      Class<?> clazz = exception.getClass();
      do {
        streamOfGetters(clazz)
            // if already set (because it was overridden) lets keep the fresh one
            .filter(m -> !methods.containsKey(m.getName()))
            // avoid skipped methods
            .filter(m -> !SKIPPED_METHODS.contains(m.getName()))
            .forEach(m -> methods.put(m.getName(), m));
        clazz = clazz.getSuperclass();
      } while (clazz != null && clazz != Object.class);

      return methods.entrySet().stream()
          .map(m -> Map.entry(m.getKey(), silentInvoke(m.getValue(), exception)))
          .filter(entry -> entry.getValue().isPresent())
          .map(entry -> Map.entry(entry.getKey(), entry.getValue().orElse(null)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } catch (RuntimeException e) {
      log.debug("Couldn't extract additional data from {}", exception, e);
      return Collections.emptyMap();
    }
  }

  private static Optional<Object> silentInvoke(
      @NonNull final Method method, @NonNull final Object target) {
    try {
      return Optional.ofNullable(method.invoke(target));
    } catch (InvocationTargetException | IllegalAccessException e) {
      log.debug("Method invocation failure for {} over {}", method.getName(), target, e);
      return Optional.empty();
    }
  }
}
