package jield.runtime;

import static jield.runtime.Bounce.cont;

/**
 * Utility class providing helper method for the CPS transformation.
 */
public final class CPSUtil {
    private CPSUtil() {
    /*
     * Cannot be instantiated.
     */
    }

    /**
     * Returns the continuation with which the execution should continue based on the evaluation of the specified
     * condition.
     * @param cond the condition to be evaluated
     * @param then the continuation to be returned if the condition evaluates to {@code true}
     * @param otherwise the continuation to be returned if the condition evaluates to {@code false}
     * @param k the continuation to be passed to the returned continuation
     * @param <T> the return type of the enclosing generator
     * @return a {@code Bounce} instance with no return value but only the next continuation
     */
    public static <T> Bounce<T> conditional(Thunk<Boolean> cond, GeneratorState<T> then, GeneratorState<T> otherwise,
                                            GeneratorState<T> k) {
        if (cond.evaluate()) {
            return cont(() -> then.apply(k));
        } else {
            return cont(() -> otherwise.apply(k));
        }
    }
}

