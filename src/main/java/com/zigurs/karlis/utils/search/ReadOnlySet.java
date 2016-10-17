package com.zigurs.karlis.utils.search;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Array backed implementation of a set (yes, it is wrong. Very).
 * <p>
 * The rationale is to make memory profile and reads as lightweight as possible
 * even if it comes at the expense of modifying operations.
 * <p>
 * Since it is to be used to contain non-null unique values Set interface seems to be most appropriate.
 * <p>
 * Comes at a cost of being a bit thread unsafe, modifying itself, etc.
 * <p>
 * To be used in QuickSearch and behind write lock _ONLY_!
 *
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class ReadOnlySet<T> extends AbstractSet<T> {

    public static <S> ReadOnlySet<S> empty() {
        //noinspection unchecked
        return new ReadOnlySet<>((S[]) new Object[0]);
    }

    public static <S> ReadOnlySet<S> fromSingle(S item) {
        Objects.requireNonNull(item);

        return new ReadOnlySet<>((S[]) new Object[]{item});
    }

    public static <S> ReadOnlySet<S> addAndCreate(Collection<? extends S> source, S newItem) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(newItem);

        if (source instanceof ReadOnlySet) {
            ReadOnlySet<S> set = (ReadOnlySet<S>) source;

            if (set.contains(newItem)) {
                // TODO - investigate - System.out.println("Repeat add!");
                return set;
            }

            Object[] arr = new Object[set.array.length + 1];
            System.arraycopy(set.array, 0, arr, 0, set.array.length);
            arr[arr.length - 1] = newItem;

            set.array = (S[]) arr;
            return set;
        } else {
            HashSet<S> set = new HashSet<>();
            set.addAll(source);
            set.add(newItem);
            return new ReadOnlySet<>((S[]) set.toArray());
        }
    }

    public static <S> ReadOnlySet<S> removeAndCreate(Collection<? extends S> source, S surplusItem) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(surplusItem);

        if (source instanceof ReadOnlySet) {
            ReadOnlySet<S> set = (ReadOnlySet<S>) source;

            for (int i = 0; i < set.array.length; i++) {
                if (set.array[i].hashCode() == surplusItem.hashCode() && set.array[i].equals(surplusItem)) {
                    System.arraycopy(set.array, i + 1, set.array, i, set.array.length - (i + 1));
                    set.array = Arrays.copyOf(set.array, set.array.length - 1);
                    return set;
                }
            }
            return set; // there was nothing to remove
        } else {
            HashSet<S> set = new HashSet<>();

            set.addAll(source);
            set.remove(surplusItem);

            return new ReadOnlySet<>((S[]) set.toArray());
        }
    }

    public static <S> ReadOnlySet<S> fromCollection(Collection<? extends S> source) {
        Objects.requireNonNull(source);

        HashSet<S> set = new HashSet<>();
        set.addAll(source);
        //noinspection unchecked
        return new ReadOnlySet<>((S[]) set.toArray());
    }

    public static <S> ReadOnlySet<S> fromCollections(Collection<? extends S> source, Collection<? extends S> source2) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(source2);

        HashSet<S> set = new HashSet<>();
        set.addAll(source);
        set.addAll(source2);
        return new ReadOnlySet<>((S[]) set.toArray());
    }

    /*
     * Implementation, as simple as it is.
     */

    private T[] array;

    private ReadOnlySet(T[] array) {
        this.array = array;
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean contains(Object o) {
        Objects.requireNonNull(o);

        for (int i = 0; i < array.length; i++) {
            /* Try to skip more by cheaper hash check first */
            if (array[i].hashCode() == o.hashCode() && array[i].equals(o)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator(array);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);

        for (int i = 0; i < array.length; i++) {
            action.accept(array[i]);
        }
    }

    public ReadOnlySet<T> safeCopy() {
        return new ReadOnlySet<>(Arrays.copyOf(array, array.length));
    }

    private static class ArrayIterator<I> implements Iterator<I> {

        private final I[] innerArray;
        private int index = 0;

        public ArrayIterator(I[] array) {
            innerArray = array;
        }

        @Override
        public boolean hasNext() {
            return index < innerArray.length;
        }

        @Override
        public I next() {
            return innerArray[index++];
        }
    }
}
