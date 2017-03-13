package jield.runtime;

/**
 * Represents a computation that awaits evaluation.
 * @param <T> the return type of the computation
 */
@FunctionalInterface
public interface Thunk<T> {
    /**
     * Evaluates the awaiting computation.
     * @return the result of the computation
     */
    T evaluate();
}
