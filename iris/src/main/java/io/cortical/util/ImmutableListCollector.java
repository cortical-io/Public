package io.cortical.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * A {@link Collector} capable of returning an immutable List
 * using clean, simplified {@link Stream} semantics.
 * 
 * @author Johan Haleby
 * @author cogmission
 */
public class ImmutableListCollector {
    
    /**
     * Collector method where the type of the backing list may be specified.
     * 
     * A reduction operation that accumulates input elements into an <em>immutable</em> 
     * result container of the type indicated by the passed {@link Supplier}, optionally 
     * transforming the accumulated result into a final representation after all input elements 
     * have been processed. 
     * <p>
     * Reduction operations can be performed either sequentially or in parallel.
     * </p>
     * <p>
     * <b>NOTE: </b> This method allows the ability to specify the backing list type.
     * <p>
     * <b>USAGE:</b>
     * <pre>
     *  List<Integer> original = ...
     *  List<Integer> immutable = original.stream().collect(ImmutableListCollector.toImmutableList(LinkedList::new)); // <-- notice the "LinkedList::new"
     * </pre>
     * 
     * 
     * @param <T> the type of input elements to the reduction operation
     * @param <A> the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @param collectionFactory
     * @return an immutable List
     */
    public static <T, A extends List<T>> Collector<T, A, List<T>> toImmutableList(Supplier<A> collectionFactory) {
        return Collector.of(collectionFactory, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableList);
    }
 
    /**
     * A reduction operation that accumulates input elements into an <em>immutable</em> 
     * result container.
     * <p>
     * Reduction operations can be performed either sequentially or in parallel.
     * <p>
     * <b>USAGE:</b>
     * <pre>
     *  List<Integer> original = ...
     *  List<Integer> immutable = original.stream().collect(ImmutableListCollector.toImmutableList());
     * </pre>
     * 
     * @param <T> the type of input elements to the reduction operation
     * @param <A> the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @return  an immutable List
     */
    public static <T> Collector<T, List<T>, List<T>> toImmutableList() {
        return toImmutableList(ArrayList::new);
    }
}
