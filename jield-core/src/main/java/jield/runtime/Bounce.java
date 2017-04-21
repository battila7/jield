package jield.runtime;

import java.util.Optional;

/**
 * Class that encapsulates a return value and a continuation for the purpose of trampolining. Instances of this object
 * must be returned from generator states to enable trampolining. The continuation is going to be the next method the
 * trampoline executes and the value is going to be the return value of the generator. If no continuation is returned
 * then the generator stops.
 * @param <T> The return type of the enclosing generator.
 */
public final class Bounce<T> {
    /*
     * The continuation the trampoline should execute next.
     */
    private final Optional<Thunk<Bounce<T>>> continuation;

    /*
     * Must use Optional<T> because {@code null} can be a desirable generated value sometimes.
     */
    private final Optional<T> value;

    /**
     * Creates a new instance with the specified continuation and value. Note that {@code null} can be
     * used as a value.
     * @param continuation the continuation where the execution should continue
     * @param value the next return value of the generator
     * @param <T> The return type of the enclosing generator.
     * @return
     */
    public static <T> Bounce<T> cont(Thunk<Bounce<T>> continuation, T value) {
        return new Bounce<>(continuation, value);
    }

    /**
     * Creates a new instance with the specified continuation and no return value
     * @param continuation the continuation where the execution should continue
     * @param <T> The return type of the enclosing generator.
     * @return
     */
    public static <T> Bounce<T> cont(Thunk<Bounce<T>> continuation) {
        return new Bounce<>(continuation, null);
    }

    private Bounce(Thunk<Bounce<T>> continuation, T value) {
        this.continuation = Optional.ofNullable(continuation);

        this.value = Optional.ofNullable(value);
    }

    public Optional<Thunk<Bounce<T>>> getContinuation() {
        return continuation;
    }

    public Optional<T> getValue() {
        return value;
    }
}