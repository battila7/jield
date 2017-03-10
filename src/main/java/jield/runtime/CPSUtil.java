package jield.runtime;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Utility class providing helper method for the CPS transformation.
 */
public final class CPSUtil {
    private CPSUtil() {
    /*
     * Cannot be instantiated.
     */
    }

    public static Iterator<Byte> iterator(byte[] array) {
        return new ArrayIterator<>(array);
    }

    public static Iterator<Short> iterator(short[] array) {
        return new ArrayIterator<>(array);
    }

    public static Iterator<Integer> iterator(int[] array) {
        return IntStream.of(array).iterator();
    }

    public static Iterator<Long> iterator(long[] array) {
        return LongStream.of(array).iterator();
    }

    public static Iterator<Float> iterator(float[] array) {
        return new ArrayIterator<>(array);
    }

    public static Iterator<Double> iterator(double[] array) {
        return DoubleStream.of(array).iterator();
    }

    public static Iterator<Character> iterator(char[] array) {
        return new ArrayIterator<>(array);
    }

    public static Iterator<Boolean> iterator(boolean[] array) {
        return new ArrayIterator<>(array);
    }

    public static <T> Iterator<T> iterator(T[] array) {
        return Arrays.asList(array).iterator();
    }

    public static <T> Iterator<T> iterator(Collection<T> collection) {
        return collection.iterator();
    }

    private static class ArrayIterator<T> implements Iterator<T> {
        private final Object array;

        private int index;

        private ArrayIterator(Object array) {
            this.array = array;

            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < Array.getLength(array);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            return (T) Array.get(array, index++);
        }
    }
}

