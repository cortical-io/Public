package io.cortical.iris.view.input.expression;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;

class FilteredListCollector implements Collector<Bubble, FilteredBubbleList.Builder, FilteredBubbleList> {
    
    public Supplier<FilteredBubbleList.Builder> supplier() {
        return FilteredBubbleList.Builder::new;
    }
    
    @Override
    public BiConsumer<FilteredBubbleList.Builder, Bubble> accumulator() {
        return (builder, b) -> builder.add(b);
    }

    @Override
    public BinaryOperator<FilteredBubbleList.Builder> combiner() {
        return (left, right) -> {
            left.addAll(right.build());
            return left;
        };
    }

    @Override
    public Function<FilteredBubbleList.Builder, FilteredBubbleList> finisher() {
        return FilteredBubbleList.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
    
    public static <E extends FilteredBubbleList> Collector<Bubble, ?, FilteredBubbleList> toFilteredList() {
        return new FilteredListCollector();
    }
}