package jield.runtime;

import java.util.function.Function;

/**
 * Represents a state of the generator that takes another state (a continuation) and produces a {@link Bounce} object
 * to enable trampolining.
 * @param <T> the return type of the containing generator
 */
@FunctionalInterface
public interface GeneratorState<T> extends Function<GeneratorState<T>, Bounce<T>> {
  /*
   * Empty, just an alias.
   */
}