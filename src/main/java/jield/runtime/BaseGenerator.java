package jield.runtime;

import static jield.runtime.Bounce.cont;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Generator class that can produce a {@link Stream} of values consisting of the return values of some
 * generator states. This class must be utilized through composition instead of inheritance.
 *
 * <p>
 * Internally uses trampolining to enable CPS and prevent stack overflow.
 * </p>
 * @param <E> The return type of the generator.
 */
public class BaseGenerator<E> implements Iterator<E>, Iterable<E> {
    private static final Thunk END_STATE = null;

    private E current;

    private Thunk<Bounce<E>> continuation;

    /**
     * Creates a new instance that will start at the specified state.
     * @param start the method that should be executed first when producing generated values
     * @param <E> The return type of the generator.
     * @return a new generator instance
     */
    public static <E> BaseGenerator<E> startingAt(GeneratorState<E> start) {
        return new BaseGenerator<>(Objects.requireNonNull(start));
    }

    private BaseGenerator(GeneratorState<E> start) {
        this.continuation = () -> start.apply(this::endCont);

        /*
         * Step the generator first. Actually we are one step forward than the backed stream to ensure the return value
         * of hasNext().
         *
         * If there are no more backing methods, then the continuation is set to null and the hasNext() method will
         * return false.
         */
        this.current = stepGenerator();
    }

    @Override
    public final Iterator<E> iterator() {
        return this;
    }

    @Override
    public final boolean hasNext() {
        return continuation != END_STATE;
    }

    @Override
    public final E next() {
        final E last = current;

        current = stepGenerator();

        return last;
    }

    /**
     * Returns a stream backed by the generator methods.
     * @return a stream of generated values
     */
    public final Stream<E> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    private Bounce<E> endCont(GeneratorState<E> k) {
        return cont(null);
    }

    /**
     * A trampoline that prevents stack overflow. Takes a starting method and then continues execution until
     * either a value is returned or there is no continuation.
     * @param start the next method the trampoline should execute
     * @return a {@code Bounce} object containing the produced value (if any) and the continuation where the execution
     * should proceed next time
     */
    private Bounce<E> trampoline(Thunk<Bounce<E>> start) {
        Bounce<E> next = cont(start);

        while (next.getContinuation().isPresent()) {
            if (next.getValue().isPresent()) {
                return next;
            }

            next = next.getContinuation().get().evaluate();
        }

        /*
         * No need to create a new one each time, better be cached.
         */
        return cont(null);
    }

    /**
     * Steps the generator and returns the next generated value. If there is no next value, {@code null} is returned
     * but the current continuation is set to {@code END_STATE}, thus the iterator and the stream backed by this class
     * is going to be stopped.
     * @return the next generated value
     */
    @SuppressWarnings("unchecked")
    private E stepGenerator() {
        Bounce<E> pauseState = trampoline(continuation);

        /*
         * Assigning the END_STATE is unchecked, but perfectly safe because it is null and conforms all types.
         */
        this.continuation = pauseState.getContinuation().orElse(END_STATE);

        return pauseState.getValue().orElse(null);
    }
}